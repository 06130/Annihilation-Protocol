package org.lingZero.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.Config;
import org.lingZero.battle.DeployResult;
import org.lingZero.battle.LevelDefinition;
import org.lingZero.battle.LevelState;
import org.lingZero.battle.Selftest;
import org.lingZero.entity.Definitions;
import org.lingZero.runtime.BattleSession;
import org.lingZero.runtime.DeployService;
import org.lingZero.runtime.SessionManager;

/**
 * /ap 命令树：关卡配置、开始、重置、生成、部署与自测。
 */
@EventBusSubscriber(modid = AnnihilationProtocolMod.MODID)
public final class AnnihilationCommand {
    private AnnihilationCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(build("ap"));
        dispatcher.register(build("annihilation"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(AnnihilationCommand::status))
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx, Config.defaultEnemyCount, Config.spawnIntervalTicks))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "count"), Config.spawnIntervalTicks))
                                .then(Commands.argument("interval", IntegerArgumentType.integer(1, 12000))
                                        .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "count"),
                                                IntegerArgumentType.getInteger(ctx, "interval"))))))
                .then(Commands.literal("reset")
                        .executes(AnnihilationCommand::reset))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("deploy")
                        .then(Commands.argument("operator", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String id : Definitions.operatorIds()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> deployHere(ctx, StringArgumentType.getString(ctx, "operator")))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> deployAt(ctx, StringArgumentType.getString(ctx, "operator"),
                                                BlockPosArgument.getBlockPos(ctx, "pos"))))))
                .then(Commands.literal("config")
                        .then(Commands.literal("setspawn")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(AnnihilationCommand::setSpawn)))
                        .then(Commands.literal("setgate")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(AnnihilationCommand::setGate)))
                        .then(Commands.literal("setarea")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .executes(AnnihilationCommand::setArea)))))
                .then(Commands.literal("path")
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(AnnihilationCommand::addPathPoint)))
                        .then(Commands.literal("clear")
                                .executes(AnnihilationCommand::clearPath))
                        .then(Commands.literal("list")
                                .executes(AnnihilationCommand::listPath)))
                .then(Commands.literal("selftest")
                        .executes(AnnihilationCommand::selftest));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BattleSession session = SessionManager.getOrCreate(source.getLevel());
        LevelDefinition definition = session.definition();
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.status",
                Component.translatable(stateKey(session.state())),
                session.gateHp(), session.gateMaxHp(), session.remaining(), session.alive(), session.operatorCount()), false);
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.status.setup",
                describe(definition.spawnPos()), describe(definition.gatePos()),
                definition.missingSetup().isEmpty() ? "OK" : String.join(",", definition.missingSetup()),
                definition.path().size()), false);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int count, int interval) {
        CommandSourceStack source = ctx.getSource();
        BattleSession session = SessionManager.getOrCreate(source.getLevel());
        if (!session.state().canStart()) {
            source.sendFailure(Component.translatable("msg.annihilation_protocol.error.already_running"));
            return 0;
        }
        List<String> missing = session.definition().missingSetup();
        if (!missing.isEmpty()) {
            source.sendFailure(Component.translatable("msg.annihilation_protocol.error.not_configured", String.join(", ", missing)));
            return 0;
        }
        session.start(count, interval);
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.started", count, interval), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BattleSession session = SessionManager.getOrCreate(source.getLevel());
        session.reset();
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.reset"), true);
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, int count) {
        CommandSourceStack source = ctx.getSource();
        BattleSession session = SessionManager.getOrCreate(source.getLevel());
        if (session.state() != LevelState.RUNNING) {
            source.sendFailure(Component.translatable("msg.annihilation_protocol.error.not_running"));
            return 0;
        }
        if (!session.definition().isConfigured()) {
            source.sendFailure(Component.translatable("msg.annihilation_protocol.error.not_configured",
                    String.join(", ", session.definition().missingSetup())));
            return 0;
        }
        session.spawnNow(count);
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.spawned", count), true);
        return 1;
    }

    private static int deployHere(CommandContext<CommandSourceStack> ctx, String operatorId) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        BlockPos pos = player != null ? BlockPos.containing(player.position()) : BlockPos.containing(source.getPosition());
        return deployAt(ctx, operatorId, pos);
    }

    private static int deployAt(CommandContext<CommandSourceStack> ctx, String operatorId, BlockPos pos) {
        CommandSourceStack source = ctx.getSource();
        DeployResult result = DeployService.deploy(source.getPlayer(), source.getLevel(), pos, operatorId);
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.deploy." + result.name()), false);
        return result == DeployResult.SUCCESS ? 1 : 0;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        session.definition().setSpawnPos(pos);
        session.markDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.configured.spawn", describe(pos)), true);
        return 1;
    }

    private static int setGate(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        session.definition().setGatePos(pos);
        session.markDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.configured.gate", describe(pos)), true);
        return 1;
    }

    private static int setArea(CommandContext<CommandSourceStack> ctx) {
        BlockPos from = BlockPosArgument.getBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getBlockPos(ctx, "to");
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        session.definition().setArea(from, to);
        session.markDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.configured.area", describe(from), describe(to)), true);
        return 1;
    }

    private static int addPathPoint(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        session.definition().addPathPoint(pos);
        session.markDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.configured.path_added", describe(pos),
                session.definition().path().size()), true);
        return 1;
    }

    private static int clearPath(CommandContext<CommandSourceStack> ctx) {
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        session.definition().clearPath();
        session.markDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.configured.path_cleared"), true);
        return 1;
    }

    private static int listPath(CommandContext<CommandSourceStack> ctx) {
        BattleSession session = SessionManager.getOrCreate(ctx.getSource().getLevel());
        List<BlockPos> path = session.definition().path();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(describe(path.get(i)));
        }
        String text = builder.length() == 0 ? "-" : builder.toString();
        ctx.getSource().sendSuccess(() -> Component.translatable("msg.annihilation_protocol.path.list", path.size(), text), false);
        return 1;
    }

    private static int selftest(CommandContext<CommandSourceStack> ctx) {
        Selftest.Result result = Selftest.run();
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("msg.annihilation_protocol.selftest.result",
                result.passed(), result.passed() + result.failed()), false);
        for (String failure : result.failures()) {
            source.sendFailure(Component.literal(failure));
        }
        return result.failed() == 0 ? 1 : 0;
    }

    private static String stateKey(LevelState state) {
        return "msg.annihilation_protocol.state." + state.name();
    }

    private static String describe(BlockPos pos) {
        return pos == null ? "-" : pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
