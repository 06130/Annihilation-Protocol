package org.lingZero.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;

/**
 * 服务端事件入口：驱动会话 tick，并把实体生命周期事件转成会话回调。
 */
@EventBusSubscriber(modid = AnnihilationProtocolMod.MODID)
public final class SessionEvents {
    private SessionEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SessionManager.tickAll(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SessionManager.ensureSession(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SessionManager.unload(level);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        BattleSession session = SessionManager.get(level);
        if (session == null) {
            return;
        }
        if (event.getEntity() instanceof EnemyEntity enemy) {
            session.onEnemyKilled(enemy);
        } else if (event.getEntity() instanceof OperatorEntity operator) {
            session.onOperatorRemoved(operator);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BattleSession session = SessionManager.get(level);
        if (session == null) {
            return;
        }
        if (event.getEntity() instanceof EnemyEntity enemy) {
            session.onEnemyUnloaded(enemy);
        } else if (event.getEntity() instanceof OperatorEntity operator) {
            session.onOperatorUnloaded(operator);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendSnapshot(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendSnapshot(event.getEntity());
    }

    private static void sendSnapshot(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel level) {
            BattleSession session = SessionManager.get(level);
            if (session != null) {
                session.sendSnapshotTo(serverPlayer);
            }
        }
    }
}
