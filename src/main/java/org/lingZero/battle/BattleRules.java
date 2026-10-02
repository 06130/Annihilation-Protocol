package org.lingZero.battle;

/**
 * 纯规则判定，不依赖世界与实体，便于自测。
 */
public final class BattleRules {
    private BattleRules() {
    }

    public static boolean isDefeated(int gateHp) {
        return gateHp <= 0;
    }

    /**
     * 胜利判定。
     * 调用方必须保证 remaining 表示"本局尚未结算的敌人总数"，**包含仍在生成队列里、尚未出场的敌人**；
     * 若只在敌人真正生成时才累加 remaining，带间隔的生成队列会在中途被提前判胜。
     */
    public static boolean isVictorious(LevelState state, int remaining, int alive, int gateHp) {
        return state == LevelState.RUNNING && remaining <= 0 && alive <= 0 && gateHp > 0;
    }

    public static int applyGateDamage(int gateHp) {
        return Math.max(0, gateHp - 1);
    }
}
