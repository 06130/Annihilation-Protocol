package org.lingZero.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lingZero.battle.DeployResult;
import org.lingZero.battle.LevelState;

/**
 * 客户端只读缓存：服务端广播的唯一落点。
 * 后续版本加 HUD 时，只需从这里读取，不必改动服务端。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBattleState {
    private static LevelState state = LevelState.IDLE;
    private static int gateHp;
    private static int gateMaxHp;
    private static int remaining;
    private static int alive;
    private static boolean received;

    private ClientBattleState() {
    }

    public static void apply(LevelState newState, int newGateHp, int newGateMaxHp, int newRemaining, int newAlive) {
        boolean stateChanged = !received || newState != state;
        boolean gateDamaged = received && newGateHp < gateHp;
        state = newState;
        gateHp = newGateHp;
        gateMaxHp = newGateMaxHp;
        remaining = newRemaining;
        alive = newAlive;
        received = true;
        if (stateChanged) {
            message(Component.translatable("msg.annihilation_protocol.level_state",
                    Component.translatable("msg.annihilation_protocol.state." + newState.name())));
        } else if (gateDamaged) {
            message(Component.translatable("msg.annihilation_protocol.gate_hp", newGateHp, newGateMaxHp));
        }
    }

    public static void onDeployResult(DeployResult result, BlockPos pos) {
        message(Component.translatable("msg.annihilation_protocol.deploy." + result.name()));
    }

    private static void message(Component component) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(component, true);
        }
    }

    public static LevelState state() {
        return state;
    }

    public static int gateHp() {
        return gateHp;
    }

    public static int gateMaxHp() {
        return gateMaxHp;
    }

    public static int remaining() {
        return remaining;
    }

    public static int alive() {
        return alive;
    }

    public static boolean hasData() {
        return received;
    }
}
