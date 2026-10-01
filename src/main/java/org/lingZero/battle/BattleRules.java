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

    public static boolean isVictorious(LevelState state, int remaining, int alive, int gateHp) {
        return state == LevelState.RUNNING && remaining <= 0 && alive <= 0 && gateHp > 0;
    }

    public static int applyGateDamage(int gateHp) {
        return Math.max(0, gateHp - 1);
    }
}
