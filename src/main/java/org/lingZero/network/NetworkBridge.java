package org.lingZero.network;

import org.lingZero.runtime.SnapshotBroadcaster;

/**
 * 把运行时与 LDLib2 网络层接起来，运行时本身不认识任何网络库类型。
 */
public final class NetworkBridge {
    private NetworkBridge() {
    }

    public static void init() {
        SnapshotBroadcaster.setSink(APPackets.INSTANCE);
    }
}
