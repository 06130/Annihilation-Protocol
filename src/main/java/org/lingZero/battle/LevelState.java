package org.lingZero.battle;

/**
 * 关卡状态机。四个状态之间的迁移：
 * IDLE ->(start)-> RUNNING ->(蓝门归零)-> DEFEAT ->(reset)-> IDLE
 * RUNNING ->(剩余与存活均为 0 且蓝门存活)-> VICTORY ->(reset)-> IDLE
 */
public enum LevelState {
    IDLE,
    RUNNING,
    VICTORY,
    DEFEAT;

    public boolean isFinished() {
        return this == VICTORY || this == DEFEAT;
    }

    public boolean canStart() {
        return this != RUNNING;
    }
}
