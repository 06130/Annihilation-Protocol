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

    /**
     * 取得维度会话；**首次调用有副作用**：载入持久化数据并执行 BattleSession#onLoaded
     * （丢弃不属于本会话的己方残留实体、把 RUNNING 降级为 IDLE）。之后只是返回缓存。
     */
    public static BattleSession ensureSession(ServerLevel level) {
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
