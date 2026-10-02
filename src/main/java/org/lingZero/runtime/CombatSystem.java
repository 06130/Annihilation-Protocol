package org.lingZero.runtime;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.lingZero.battle.LevelState;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;

/**
 * 近战结算：干员只攻击自己阻挡的敌人，敌人只攻击阻挡自己的干员。全部服务端权威。
 * 本包不接触任何渲染/动画库：攻击演出由实体自己的 afterAttack() 触发。
 */
public final class CombatSystem {
    private CombatSystem() {
    }

    public static void tick(BattleSession session) {
        if (session.state() != LevelState.RUNNING) {
            return;
        }
        for (OperatorEntity operator : session.loadedOperators()) {
            if (!operator.readyToAttack()) {
                continue;
            }
            // 复制一份：strike() 可能触发死亡回调，回调会改写 blockedEnemies
            for (EnemyEntity enemy : List.copyOf(operator.blockedEnemies())) {
                if (!enemy.isAlive() || enemy.isRemoved()) {
                    continue;
                }
                strike(operator, enemy, operator.getAttackDamage());
                operator.afterAttack();
                break;
            }
            if (session.state() != LevelState.RUNNING) {
                return;
            }
        }
        for (EnemyEntity enemy : session.loadedEnemies()) {
            if (session.state() != LevelState.RUNNING) {
                return;
            }
            OperatorEntity blocker = enemy.getBlocker();
            if (blocker == null || !blocker.isAlive() || blocker.isRemoved()) {
                continue;
            }
            if (!enemy.readyToAttack()) {
                continue;
            }
            strike(enemy, blocker, enemy.getAttackDamage());
            enemy.afterAttack();
        }
    }

    private static void strike(LivingEntity attacker, LivingEntity target, float damage) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        target.hurt(level.damageSources().mobAttack(attacker), damage);
    }
}
