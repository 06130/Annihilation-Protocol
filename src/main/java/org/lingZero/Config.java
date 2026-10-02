package org.lingZero;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 关卡与战斗的可调参数。数值在配置文件加载后才会写入静态字段，因此都带默认值。
 */
@EventBusSubscriber(modid = AnnihilationProtocolMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue GATE_MAX_HP = BUILDER
            .comment("蓝门最大生命值")
            .defineInRange("gateMaxHp", 3, 1, 1000);
    private static final ModConfigSpec.IntValue DEFAULT_ENEMY_COUNT = BUILDER
            .comment("/ap start 未指定数量时生成的敌人总数")
            .defineInRange("defaultEnemyCount", 5, 1, 500);
    private static final ModConfigSpec.IntValue SPAWN_INTERVAL_TICKS = BUILDER
            .comment("生成队列默认间隔（tick）")
            .defineInRange("spawnIntervalTicks", 40, 1, 12000);
    private static final ModConfigSpec.IntValue MAX_OPERATORS = BUILDER
            .comment("同时存在的干员上限")
            .defineInRange("maxOperators", 8, 1, 64);
    private static final ModConfigSpec.IntValue DEPLOY_COOLDOWN_TICKS = BUILDER
            .comment("同一玩家的部署冷却（tick）")
            .defineInRange("deployCooldownTicks", 40, 0, 12000);
    private static final ModConfigSpec.IntValue BLOCK_CHECK_INTERVAL_TICKS = BUILDER
            .comment("阻挡检测间隔（tick）")
            .defineInRange("blockCheckIntervalTicks", 5, 1, 100);
    private static final ModConfigSpec.DoubleValue MAX_DEPLOY_DISTANCE = BUILDER
            .comment("玩家部署时与目标格的最大距离（格）")
            .defineInRange("maxDeployDistance", 8.0, 1.0, 64.0);
    private static final ModConfigSpec.BooleanValue SHOW_HP_NAME_TAG = BUILDER
            .comment("是否用名字牌显示干员/敌人血量")
            .define("showHpNameTag", true);
    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("输出关卡与网络调试日志")
            .define("debugLogging", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int gateMaxHp = 3;
    public static int defaultEnemyCount = 5;
    public static int spawnIntervalTicks = 40;
    public static int maxOperators = 8;
    public static int deployCooldownTicks = 40;
    public static int blockCheckIntervalTicks = 5;
    public static double maxDeployDistance = 8.0;
    public static boolean showHpNameTag = true;
    public static boolean debugLogging = false;

    private Config() {
    }

    @SubscribeEvent
    static void onLoading(ModConfigEvent.Loading event) {
        apply();
    }

    @SubscribeEvent
    static void onReloading(ModConfigEvent.Reloading event) {
        apply();
    }

    private static void apply() {
        gateMaxHp = GATE_MAX_HP.get();
        defaultEnemyCount = DEFAULT_ENEMY_COUNT.get();
        spawnIntervalTicks = SPAWN_INTERVAL_TICKS.get();
        maxOperators = MAX_OPERATORS.get();
        deployCooldownTicks = DEPLOY_COOLDOWN_TICKS.get();
        blockCheckIntervalTicks = BLOCK_CHECK_INTERVAL_TICKS.get();
        maxDeployDistance = MAX_DEPLOY_DISTANCE.get();
        showHpNameTag = SHOW_HP_NAME_TAG.get();
        debugLogging = DEBUG_LOGGING.get();
    }
}
