package org.lingZero.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * 近战结算：干员只攻击自己阻挡的敌人，敌人只攻击阻挡自己的干员。全部服务端权威。
 */
public final class CombatSystem {
    private CombatSystem() {
    }

    public static void tick(BattleSession session) {
        for (OperatorEntity operator : session.loadedOperators()) {
            if (!operator.readyToAttack()) {
                continue;
            }
            for (EnemyEntity enemy : operator.blockedEnemies()) {
                if (!enemy.isAlive() || enemy.isRemoved()) {
                    continue;
                }
                strike(operator, enemy, operator.getAttackDamage());
                operator.afterAttack();
                break;
            }
        }
        for (EnemyEntity enemy : session.loadedEnemies()) {
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
        if (attacker instanceof GeoEntity geo) {
            geo.triggerAnim("main", "attack");
        }
    }
}
