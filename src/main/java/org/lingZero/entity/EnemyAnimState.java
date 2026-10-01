package org.lingZero.entity;

/**
 * 敌人的表现状态，用于驱动 GeckoLib 的循环动画。
 * 一次性攻击动画由 triggerAnim("main", "attack") 触发，不占用该状态。
 */
public enum EnemyAnimState {
    MOVE,
    BLOCKED,
    ATTACK;

    private static final EnemyAnimState[] VALUES = values();

    public static EnemyAnimState byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : MOVE;
    }
}
