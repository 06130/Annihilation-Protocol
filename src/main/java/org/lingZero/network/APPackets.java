package org.lingZero.network;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.Config;
import org.lingZero.battle.DeployResult;
import org.lingZero.battle.LevelState;
import org.lingZero.client.ClientBattleState;
import org.lingZero.runtime.BattleSession;
import org.lingZero.runtime.DeployService;
import org.lingZero.runtime.SessionManager;
import org.lingZero.runtime.Snapshot;
import org.lingZero.runtime.SnapshotSink;

/**
 * LDLib2 RPC 端点：客户端只发部署请求，服务端只广播标量状态。
 * 参数类型必须是 LDLib2 的 IDirectAccessor 支持的类型（标量、枚举、BlockPos 等）。
 */
public final class APPackets implements SnapshotSink {
    public static final APPackets INSTANCE = new APPackets();

    private APPackets() {
    }

    @RPCPacket(APChannels.BATTLE_SNAPSHOT)
    public static void onBattleSnapshot(RPCSender sender, LevelState state, int gateHp, int gateMaxHp, int remaining, int alive) {
        if (FMLEnvironment.dist.isClient()) {
            ClientBattleState.apply(state, gateHp, gateMaxHp, remaining, alive);
        }
    }

    @RPCPacket(APChannels.DEPLOY_REQUEST)
    public static void onDeployRequest(RPCSender sender, BlockPos pos, String operatorId) {
        ServerPlayer player = sender.asPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        DeployResult result = DeployService.deploy(player, level, pos, operatorId);
        if (Config.debugLogging) {
            AnnihilationProtocolMod.LOGGER.info("[AP] 部署请求 {} @ {} -> {}", operatorId, pos, result);
        }
        sendDeployResult(player, result, pos);
        BattleSession session = SessionManager.get(level);
        if (session != null) {
            session.sendSnapshotTo(player);
        }
    }

    @RPCPacket(APChannels.DEPLOY_RESULT)
    public static void onDeployResult(RPCSender sender, DeployResult result, BlockPos pos) {
        if (FMLEnvironment.dist.isClient()) {
            ClientBattleState.onDeployResult(result, pos);
        }
    }

    @Override
    public void broadcast(Snapshot snapshot) {
        RPCPacketDistributor.rpcToAllPlayers(APChannels.BATTLE_SNAPSHOT,
                snapshot.state(), snapshot.gateHp(), snapshot.gateMaxHp(), snapshot.remaining(), snapshot.alive());
    }

    @Override
    public void sendTo(ServerPlayer player, Snapshot snapshot) {
        RPCPacketDistributor.rpcToPlayer(player, APChannels.BATTLE_SNAPSHOT,
                snapshot.state(), snapshot.gateHp(), snapshot.gateMaxHp(), snapshot.remaining(), snapshot.alive());
    }

    public static void sendDeployResult(ServerPlayer player, DeployResult result, BlockPos pos) {
        RPCPacketDistributor.rpcToPlayer(player, APChannels.DEPLOY_RESULT, result, pos);
    }
}
