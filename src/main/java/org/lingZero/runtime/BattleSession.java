package org.lingZero.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.Config;
import org.lingZero.battle.BattleRules;
import org.lingZero.battle.LevelDefinition;
import org.lingZero.battle.LevelState;
import org.lingZero.battle.SpawnEntry;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;
import org.lingZero.registry.ModEntities;

/**
 * 关卡会话：服务端唯一权威。持有实体追踪表，驱动生成队列、阻挡、战斗与胜负判定。
 */
public class BattleSession {
    private static final int SNAPSHOT_INTERVAL_TICKS = 5;
    private static final double GATE_ARRIVAL_RADIUS = 0.75;
    /** 载入后持续清理残留实体的窗口：实体随区块陆续载入，一次遍历清不干净。 */
    private static final int STALE_CLEANUP_TICKS = 200;

    private final ServerLevel level;
    private final LevelRuntimeData data;
    private final Map<UUID, BlockPos> trackedEnemies = new HashMap<>();
    private final Map<UUID, BlockPos> trackedOperators = new HashMap<>();
    private final Map<UUID, Long> lastDeployTicks = new HashMap<>();
    private final List<PendingSpawn> pendingSpawns = new ArrayList<>();
    private final List<EnemyEntity> loadedEnemies = new ArrayList<>();
    private final List<OperatorEntity> loadedOperators = new ArrayList<>();
    private int spawnInterval = 40;
    private int staleCleanupTicks;
    private int blockCheckCooldown;
    private int snapshotCooldown;
    @Nullable
    private Snapshot lastSent;

    public BattleSession(ServerLevel level, LevelRuntimeData data) {
        this.level = level;
        this.data = data;
    }

    private static final class PendingSpawn {
        private int delay;
        private int count;

        private PendingSpawn(int delay, int count) {
            this.delay = delay;
            this.count = count;
        }
    }

    public ServerLevel level() {
        return level;
    }

    public LevelRuntimeData data() {
        return data;
    }

    public LevelDefinition definition() {
        return data.definition();
    }

    public LevelState state() {
        return data.state();
    }

    public int gateHp() {
        return data.gateHp();
    }

    public int gateMaxHp() {
        return data.gateMaxHp();
    }

    public int remaining() {
        return data.remaining();
    }

    public int alive() {
        return data.alive();
    }

    public int operatorCount() {
        return trackedOperators.size();
    }

    public List<EnemyEntity> loadedEnemies() {
        return loadedEnemies;
    }

    public List<OperatorEntity> loadedOperators() {
        return loadedOperators;
    }

    public void markDirty() {
        data.setDirty();
    }

    /** 世界载入时清理上一局的残留实体与计数，RUNNING 一律降级为 IDLE。 */
    public void onLoaded() {
        boolean wasRunning = data.state() == LevelState.RUNNING;
        staleCleanupTicks = STALE_CLEANUP_TICKS;
        discardStaleEntities();
        trackedEnemies.clear();
        trackedOperators.clear();
        pendingSpawns.clear();
        lastDeployTicks.clear();
        loadedEnemies.clear();
        loadedOperators.clear();
        data.setRemaining(0);
        data.setAlive(0);
        if (data.gateMaxHp() <= 0) {
            data.setGateMaxHp(Config.gateMaxHp);
            data.setGateHp(Config.gateMaxHp);
        }
        if (wasRunning) {
            data.setState(LevelState.IDLE);
            if (Config.debugLogging) {
                AnnihilationProtocolMod.LOGGER.info("[AP] 上一局仍在 RUNNING，已降级为 IDLE 并清理残留实体");
            }
        }
        data.setDirty();
        lastSent = null;
    }

    /** 开始新一轮：清掉残留敌人，保留已部署的干员（允许开局前布置）。 */
    public void start(int count, int interval) {
        discardAllEnemies();
        BlockingSystem.releaseAll(this);
        trackedEnemies.clear();
        loadedEnemies.clear();
        this.spawnInterval = Math.max(1, interval);
        this.pendingSpawns.clear();
        List<SpawnEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int delay = i == 0 ? 0 : this.spawnInterval;
            entries.add(new SpawnEntry(delay, 1));
            this.pendingSpawns.add(new PendingSpawn(delay, 1));
        }
        data.definition().setSpawnQueue(entries);
        data.setGateMaxHp(Config.gateMaxHp);
        data.setGateHp(Config.gateMaxHp);
        data.setRemaining(0);
        data.setAlive(0);
        data.setState(LevelState.RUNNING);
        data.setDirty();
        flushSnapshot(true);
    }

    /** /ap spawn：立即生成，仍走同一条生成队列，便于统一计数。 */
    public void spawnNow(int count) {
        pendingSpawns.add(new PendingSpawn(0, Math.max(1, count)));
    }

    public void reset() {
        discardOwnEntities();
        trackedEnemies.clear();
        trackedOperators.clear();
        pendingSpawns.clear();
        lastDeployTicks.clear();
        loadedEnemies.clear();
        loadedOperators.clear();
        data.setState(LevelState.IDLE);
        data.setGateMaxHp(Config.gateMaxHp);
        data.setGateHp(Config.gateMaxHp);
        data.setRemaining(0);
        data.setAlive(0);
        data.setDirty();
        flushSnapshot(true);
    }

    public void tick() {
        if (snapshotCooldown > 0) {
            snapshotCooldown--;
        }
        if (blockCheckCooldown > 0) {
            blockCheckCooldown--;
        }
        if (staleCleanupTicks > 0) {
            staleCleanupTicks--;
            discardStaleEntities();
        }
        refreshLoadedEntities();
        if (data.state() != LevelState.RUNNING) {
            flushSnapshot(false);
            return;
        }
        processSpawnQueue();
        checkGateArrivals();
        if (blockCheckCooldown <= 0) {
            blockCheckCooldown = Math.max(1, Config.blockCheckIntervalTicks);
            BlockingSystem.pass(this);
        }
        CombatSystem.tick(this);
        refreshLoadedEntities();
        checkVictory();
        flushSnapshot(false);
    }

    private void refreshLoadedEntities() {
        loadedEnemies.clear();
        loadedOperators.clear();

        Iterator<Map.Entry<UUID, BlockPos>> enemyIterator = trackedEnemies.entrySet().iterator();
        while (enemyIterator.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = enemyIterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof EnemyEntity enemy && !enemy.isRemoved()) {
                entry.setValue(enemy.blockPosition());
                loadedEnemies.add(enemy);
            } else if (level.isLoaded(entry.getValue())) {
                onEnemyLost(entry.getKey());
                enemyIterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, BlockPos>> operatorIterator = trackedOperators.entrySet().iterator();
        while (operatorIterator.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = operatorIterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof OperatorEntity operator && !operator.isRemoved()) {
                entry.setValue(operator.blockPosition());
                loadedOperators.add(operator);
            } else if (level.isLoaded(entry.getValue())) {
                operatorIterator.remove();
            }
        }
    }

    private void processSpawnQueue() {
        while (!pendingSpawns.isEmpty()) {
            PendingSpawn head = pendingSpawns.get(0);
            if (head.delay > 0) {
                head.delay--;
                return;
            }
            if (head.count <= 0) {
                pendingSpawns.remove(0);
                continue;
            }
            spawnEnemy();
            head.count--;
            if (head.count > 0) {
                head.delay = spawnInterval;
            } else {
                pendingSpawns.remove(0);
            }
            return;
        }
    }

    private void spawnEnemy() {
        LevelDefinition definition = data.definition();
        BlockPos spawnPos = definition.spawnPos();
        BlockPos gatePos = definition.gatePos();
        if (spawnPos == null || gatePos == null) {
            return;
        }
        EnemyEntity enemy = ModEntities.ENEMY_CRAWLER.get().create(level);
        if (enemy == null) {
            return;
        }
        enemy.configure(definition.path(), gatePos);
        enemy.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        if (!level.addFreshEntity(enemy)) {
            AnnihilationProtocolMod.LOGGER.warn("[AP] 敌人生成失败于 {}", spawnPos);
            return;
        }
        trackedEnemies.put(enemy.getUUID(), spawnPos);
        data.setRemaining(data.remaining() + 1);
        data.setAlive(data.alive() + 1);
        data.setDirty();
    }

    private void checkGateArrivals() {
        BlockPos gate = data.definition().gatePos();
        if (gate == null) {
            return;
        }
        double gateX = gate.getX() + 0.5;
        double gateZ = gate.getZ() + 0.5;
        for (EnemyEntity enemy : List.copyOf(loadedEnemies)) {
            if (!enemy.isAlive() || enemy.isRemoved()) {
                continue;
            }
            if (Math.hypot(enemy.getX() - gateX, enemy.getZ() - gateZ) < GATE_ARRIVAL_RADIUS) {
                onEnemyReachedGate(enemy);
            }
        }
    }

    public void trackOperator(OperatorEntity operator) {
        trackedOperators.put(operator.getUUID(), operator.blockPosition());
    }

    public void onEnemyKilled(EnemyEntity enemy) {
        if (trackedEnemies.remove(enemy.getUUID()) == null) {
            return;
        }
        BlockingSystem.release(enemy);
        data.setAlive(data.alive() - 1);
        data.setRemaining(data.remaining() - 1);
        data.setDirty();
        checkVictory();
        flushSnapshot(false);
    }

    public void onEnemyReachedGate(EnemyEntity enemy) {
        if (trackedEnemies.remove(enemy.getUUID()) == null) {
            return;
        }
        BlockingSystem.release(enemy);
        data.setGateHp(BattleRules.applyGateDamage(data.gateHp()));
        data.setAlive(data.alive() - 1);
        data.setRemaining(data.remaining() - 1);
        enemy.discard();
        data.setDirty();
        if (BattleRules.isDefeated(data.gateHp())) {
            data.setState(LevelState.DEFEAT);
            flushSnapshot(true);
            return;
        }
        checkVictory();
        flushSnapshot(false);
    }

    public void onEnemyUnloaded(EnemyEntity enemy) {
        BlockingSystem.release(enemy);
    }

    public void onOperatorUnloaded(OperatorEntity operator) {
        BlockingSystem.releaseOperator(operator);
    }

    public void onOperatorRemoved(OperatorEntity operator) {
        BlockingSystem.releaseOperator(operator);
        trackedOperators.remove(operator.getUUID());
    }

    private void onEnemyLost(UUID uuid) {
        data.setAlive(data.alive() - 1);
        data.setRemaining(data.remaining() - 1);
        data.setDirty();
        if (Config.debugLogging) {
            AnnihilationProtocolMod.LOGGER.info("[AP] 追踪中的敌人 {} 已不存在，按丢失处理", uuid);
        }
        checkVictory();
    }

    private void checkVictory() {
        if (BattleRules.isVictorious(data.state(), data.remaining(), data.alive(), data.gateHp())) {
            data.setState(LevelState.VICTORY);
            flushSnapshot(true);
        }
    }

    /** 丢弃不属于本会话追踪表的己方实体（重启后的残留）。 */
    private void discardStaleEntities() {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EnemyEntity enemy) {
                if (!trackedEnemies.containsKey(enemy.getUUID())) {
                    enemy.discard();
                }
            } else if (entity instanceof OperatorEntity operator) {
                if (!trackedOperators.containsKey(operator.getUUID())) {
                    operator.discard();
                }
            }
        }
    }

    private void discardOwnEntities() {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EnemyEntity || entity instanceof OperatorEntity) {
                entity.discard();
            }
        }
    }

    private void discardAllEnemies() {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof EnemyEntity) {
                entity.discard();
            }
        }
    }

    public boolean isOnDeployCooldown(ServerPlayer player) {
        Long last = lastDeployTicks.get(player.getUUID());
        return last != null && level.getGameTime() - last < Config.deployCooldownTicks;
    }

    public void recordDeploy(ServerPlayer player) {
        lastDeployTicks.put(player.getUUID(), level.getGameTime());
    }

    public Snapshot snapshot() {
        return new Snapshot(data.state(), data.gateHp(), data.gateMaxHp(), data.remaining(), data.alive());
    }

    public void sendSnapshotTo(ServerPlayer player) {
        SnapshotBroadcaster.sendTo(player, snapshot());
    }

    private void flushSnapshot(boolean force) {
        Snapshot current = snapshot();
        if (!force && current.equals(lastSent)) {
            return;
        }
        if (!force && snapshotCooldown > 0) {
            return;
        }
        lastSent = current;
        snapshotCooldown = SNAPSHOT_INTERVAL_TICKS;
        SnapshotBroadcaster.broadcast(current);
    }
}
