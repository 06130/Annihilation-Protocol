package org.lingZero.runtime;

import net.minecraft.server.level.ServerPlayer;

public final class SnapshotBroadcaster {
    private static volatile SnapshotSink sink = SnapshotSink.NOOP;

    private SnapshotBroadcaster() {
    }

    public static void setSink(SnapshotSink newSink) {
        sink = newSink;
    }

    public static void broadcast(Snapshot snapshot) {
        sink.broadcast(snapshot);
    }

    public static void sendTo(ServerPlayer player, Snapshot snapshot) {
        sink.sendTo(player, snapshot);
    }
}
