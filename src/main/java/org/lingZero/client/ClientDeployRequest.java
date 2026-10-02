package org.lingZero.client;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lingZero.network.APChannels;

/**
 * 客户端部署请求的唯一出口：只发请求，实体一律由服务端生成。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientDeployRequest {
    private ClientDeployRequest() {
    }

    public static void send(BlockPos pos, String operatorId) {
        RPCPacketDistributor.rpcToServer(APChannels.DEPLOY_REQUEST, pos, operatorId);
    }
}
