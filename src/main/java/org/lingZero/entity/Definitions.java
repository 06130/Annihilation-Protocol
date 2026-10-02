package org.lingZero.entity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.Config;

/**
 * 类型定义表，客户端与服务端共用，命令补全与网络请求都从这里取。
 */
public final class Definitions {
    public static final EnemyDefinition ENEMY_CRAWLER = new EnemyDefinition("enemy_crawler", 20.0F, 2.0F, 0.03, 1, 40);
    public static final OperatorDefinition OPERATOR_GUARD = new OperatorDefinition("operator_guard", 40.0F, 5.0F, 1, 30);

    private static final Map<String, EnemyDefinition> ENEMIES = Map.of(ENEMY_CRAWLER.id(), ENEMY_CRAWLER);
    private static final Map<String, OperatorDefinition> OPERATORS = Map.of(OPERATOR_GUARD.id(), OPERATOR_GUARD);

    private Definitions() {
    }

    public static Optional<EnemyDefinition> enemy(String id) {
        return Optional.ofNullable(ENEMIES.get(id));
    }

    public static EnemyDefinition enemyOrFallback(String id) {
        EnemyDefinition definition = ENEMIES.get(id);
        if (definition == null) {
            if (Config.debugLogging) {
                AnnihilationProtocolMod.LOGGER.warn("[AP] 未知敌人类型 {}，回退为 {}", id, ENEMY_CRAWLER.id());
            }
            return ENEMY_CRAWLER;
        }
        return definition;
    }

    public static Optional<OperatorDefinition> operator(String id) {
        return Optional.ofNullable(OPERATORS.get(id));
    }

    public static Collection<String> operatorIds() {
        return OPERATORS.keySet();
    }
}
