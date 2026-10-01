package org.lingZero.entity;

/**
 * 敌人类型定义。MVP 只有一个条目，后续扩展只需在此登记。
 */
public record EnemyDefinition(String id, float maxHp, float attackDamage, double moveSpeed, int blockCost, int attackIntervalTicks) {
}
