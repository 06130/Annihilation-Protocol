package org.lingZero.runtime;

import net.minecraft.server.level.ServerPlayer;

/**
 * 网络层注入点：运行时只依赖该接口，不依赖任何网络库类型。
 */
public interface SnapshotSink {
    SnapshotSink NOOP = new SnapshotSink() {
        @Override
        public void broadcast(Snapshot snapshot) {
        }

        @Override
        public void sendTo(ServerPlayer player, Snapshot snapshot) {
        }
    };

    void broadcast(Snapshot snapshot);

    void sendTo(ServerPlayer player, Snapshot snapshot);
}
