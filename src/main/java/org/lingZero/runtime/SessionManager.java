package org.lingZero.runtime;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class SessionManager {
    private static final Map<ServerLevel, BattleSession> SESSIONS = new HashMap<>();

    private SessionManager() {
    }

    public static BattleSession getOrCreate(ServerLevel level) {
        BattleSession session = SESSIONS.get(level);
        if (session == null) {
            LevelRuntimeData data = level.getDataStorage().computeIfAbsent(LevelRuntimeData.factory(), LevelRuntimeData.FILE_ID);
            session = new BattleSession(level, data);
            SESSIONS.put(level, session);
            session.onLoaded();
        }
        return session;
    }

    @Nullable
    public static BattleSession get(ServerLevel level) {
        return SESSIONS.get(level);
    }

    public static void unload(ServerLevel level) {
        SESSIONS.remove(level);
    }

    public static void tickAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            BattleSession session = SESSIONS.get(level);
            if (session != null) {
                session.tick();
            }
        }
    }
}
