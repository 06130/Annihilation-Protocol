package org.lingZero.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SnapshotBroadcaster {
    private static volatile SnapshotSink sink = SnapshotSink.NOOP;

    private SnapshotBroadcaster() {
    }

    public static void setSink(SnapshotSink newSink) {
        sink = newSink;
    }

    public static void broadcast(ServerLevel level, Snapshot snapshot) {
        sink.broadcast(level, snapshot);
    }

    public static void sendTo(ServerPlayer player, Snapshot snapshot) {
        sink.sendTo(player, snapshot);
    }
}
