package org.lingZero.entity;

/**
 * 干员类型定义。MVP 只有一个条目。
 */
public record OperatorDefinition(String id, float maxHp, float attackDamage, int blockCapacity, int attackIntervalTicks) {
}
