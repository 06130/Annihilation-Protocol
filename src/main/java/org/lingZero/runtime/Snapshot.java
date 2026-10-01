package org.lingZero.runtime;

import org.lingZero.battle.LevelState;

/**
 * 广播给客户端的关卡快照，只含标量，便于 LDLib2 RPC 直接序列化。
 */
public record Snapshot(LevelState state, int gateHp, int gateMaxHp, int remaining, int alive) {
}
