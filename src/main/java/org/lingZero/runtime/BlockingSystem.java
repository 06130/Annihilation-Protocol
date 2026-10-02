package org.lingZero.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;

/**
 * 阻挡系统：双向绑定（敌人 - 干员），每 N tick 由会话驱动一次，避免每 tick 全遍历。
 */
public final class BlockingSystem {
    public static final double BLOCK_RADIUS = 0.8;
    public static final double RELEASE_RADIUS = BLOCK_RADIUS * 2.0;

    private BlockingSystem() {
    }

    public static void pass(BattleSession session) {
        for (EnemyEntity enemy : prioritized(session)) {
            OperatorEntity blocker = enemy.getBlocker();
            if (blocker != null) {
                if (shouldRelease(blocker, enemy)) {
                    release(enemy);
                }
                continue;
            }
            OperatorEntity candidate = findBlocker(session.level(), enemy);
            if (candidate != null) {
                occupy(candidate, enemy);
            }
        }
        for (OperatorEntity operator : session.loadedOperators()) {
            operator.blockedEnemies().removeIf(enemy -> !enemy.isAlive() || enemy.isRemoved() || enemy.getBlocker() != operator);
        }
    }

    /**
     * 抢占同一干员时按"离蓝门更近者优先"处理，避免结果依赖 UUID 哈希顺序（跨存档/重启会变）。
     */
    private static List<EnemyEntity> prioritized(BattleSession session) {
        List<EnemyEntity> enemies = new ArrayList<>(session.loadedEnemies());
        BlockPos gate = session.definition().gatePos();
        if (gate != null) {
            double gateX = gate.getX() + 0.5;
            double gateZ = gate.getZ() + 0.5;
            enemies.sort(Comparator.comparingDouble(
                            (EnemyEntity enemy) -> enemy.distanceToSqr(gateX, enemy.getY(), gateZ))
                    .thenComparing(enemy -> enemy.getUUID().toString()));
        }
        return enemies;
    }

    private static boolean shouldRelease(OperatorEntity blocker, EnemyEntity enemy) {
        return !blocker.isAlive() || blocker.isRemoved() || blocker.distanceToSqr(enemy) > RELEASE_RADIUS * RELEASE_RADIUS;
    }

    @Nullable
    private static OperatorEntity findBlocker(ServerLevel level, EnemyEntity enemy) {
        List<OperatorEntity> candidates = level.getEntitiesOfClass(OperatorEntity.class,
                enemy.getBoundingBox().inflate(BLOCK_RADIUS), EntitySelector.NO_SPECTATORS);
        OperatorEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (OperatorEntity operator : candidates) {
            if (!operator.isAlive() || operator.isRemoved()) {
                continue;
            }
            if (operator.remainingCapacity() < enemy.getBlockCost()) {
                continue;
            }
            double distance = operator.distanceToSqr(enemy);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = operator;
            }
        }
        return best;
    }

    public static boolean occupy(OperatorEntity operator, EnemyEntity enemy) {
        if (!operator.isAlive() || operator.isRemoved() || enemy.getBlocker() != null) {
            return false;
        }
        if (operator.remainingCapacity() < enemy.getBlockCost()) {
            return false;
        }
        operator.addBlocked(enemy);
        enemy.setBlocker(operator);
        return true;
    }

    public static void release(EnemyEntity enemy) {
        OperatorEntity blocker = enemy.getBlocker();
        if (blocker != null) {
            blocker.removeBlocked(enemy);
        }
        enemy.setBlocker(null);
    }

    /** 清空会话内全部阻挡关系（例如重置或开始新一轮时）。 */
    public static void releaseAll(BattleSession session) {
        for (OperatorEntity operator : session.loadedOperators()) {
            releaseOperator(operator);
        }
        for (EnemyEntity enemy : session.loadedEnemies()) {
            release(enemy);
        }
    }

    public static void releaseOperator(OperatorEntity operator) {
        for (EnemyEntity enemy : List.copyOf(operator.blockedEnemies())) {
            if (enemy.getBlocker() == operator) {
                enemy.setBlocker(null);
            }
        }
        operator.clearBlocked();
    }
}
