package org.lingZero.network;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class APClient {
    private APClient() {
    }

    public static void sendDeployRequest(BlockPos pos, String operatorId) {
        RPCPacketDistributor.rpcToServer(APChannels.DEPLOY_REQUEST, pos, operatorId);
    }
}
