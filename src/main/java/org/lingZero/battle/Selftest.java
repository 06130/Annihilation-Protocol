package org.lingZero.battle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * 纯逻辑自测，由 /ap selftest 触发。
 * 这里的失败原因面向开发者诊断，故直接使用中文字面量（见 AGENTS.md 豁免说明）。
 */
public final class Selftest {
    private Selftest() {
    }

    public record Result(int passed, int failed, List<String> failures) {
    }

    public static Result run() {
        List<String> failures = new ArrayList<>();
        int passed = 0;

        if (!BattleRules.isDefeated(0)) {
            failures.add("蓝门 0 血应判失败");
        } else {
            passed++;
        }
        if (BattleRules.isDefeated(1)) {
            failures.add("蓝门 1 血不应判失败");
        } else {
            passed++;
        }
        if (!BattleRules.isVictorious(LevelState.RUNNING, 0, 0, 1)) {
            failures.add("RUNNING + 无剩余 + 无存活 + 蓝门存活 应判胜利");
        } else {
            passed++;
        }
        if (BattleRules.isVictorious(LevelState.IDLE, 0, 0, 1)) {
            failures.add("非 RUNNING 不应判胜利");
        } else {
            passed++;
        }
        if (BattleRules.isVictorious(LevelState.RUNNING, 0, 0, 0)) {
            failures.add("蓝门归零不应判胜利");
        } else {
            passed++;
        }
        if (BattleRules.isVictorious(LevelState.RUNNING, 1, 1, 3)) {
            failures.add("仍有未结算敌人时不应判胜利");
        } else {
            passed++;
        }
        if (BattleRules.applyGateDamage(3) != 2) {
            failures.add("蓝门扣血应为 3 -> 2");
        } else {
            passed++;
        }
        if (BattleRules.applyGateDamage(0) != 0) {
            failures.add("蓝门血量不应为负");
        } else {
            passed++;
        }
        if (LevelState.RUNNING.canStart()) {
            failures.add("RUNNING 状态不应允许再次 start");
        } else {
            passed++;
        }
        if (!LevelState.IDLE.canStart() || !LevelState.DEFEAT.canStart()) {
            failures.add("IDLE/DEFEAT 状态应允许 start");
        } else {
            passed++;
        }

        LevelDefinition definition = new LevelDefinition();
        definition.setSpawnPos(new BlockPos(0, 0, 0));
        definition.setGatePos(new BlockPos(10, 0, 0));
        definition.setArea(new BlockPos(0, 0, 0), new BlockPos(10, 4, 10));
        definition.addPathPoint(new BlockPos(5, 0, 0));

        if (definition.missingSetup().size() != 0) {
            failures.add("完整配置不应报缺失: " + definition.missingSetup());
        } else {
            passed++;
        }
        if (!definition.isOnPath(new BlockPos(2, 0, 0))) {
            failures.add("路径 A 点应被判定在路径上");
        } else {
            passed++;
        }
        if (definition.isOnPath(new BlockPos(2, 0, 4))) {
            failures.add("偏离路径 4 格的方块不应判定在路径上");
        } else {
            passed++;
        }
        if (definition.isOnPath(new BlockPos(2, 0, 0)) && definition.areaContains(new BlockPos(2, 0, 0))) {
            passed++;
        } else {
            failures.add("路径上的方块应在区域内但仍被 isOnPath 命中（部署应被拒绝）");
        }
        if (definition.areaContains(new BlockPos(2, 0, 4)) && !definition.isOnPath(new BlockPos(2, 0, 4))) {
            passed++;
        } else {
            failures.add("区域内且不在路径上的方块应可部署");
        }
        if (definition.areaContains(new BlockPos(20, 0, 0))) {
            failures.add("区域外的方块不应在区域内");
        } else {
            passed++;
        }

        LevelDefinition empty = new LevelDefinition();
        if (empty.missingSetup().size() != 3) {
            failures.add("空配置应报 3 项缺失: " + empty.missingSetup());
        } else {
            passed++;
        }

        LevelDefinition roundTrip = LevelDefinition.load(definition.save());
        if (!roundTrip.path().equals(definition.path()) || !roundTrip.isOnPath(new BlockPos(5, 0, 0))) {
            failures.add("配置存取往返不一致");
        } else {
            passed++;
        }

        passed += testSpawnQueue(failures);

        return new Result(passed, failures.size(), failures);
    }

    /** 生成队列直接决定"是否还有敌人没出场"，是胜利判定的前置条件，必须覆盖。 */
    private static int testSpawnQueue(List<String> failures) {
        int passed = 0;

        SpawnQueue queue = new SpawnQueue();
        if (!queue.isEmpty()) {
            failures.add("空队列应 isEmpty");
        } else {
            passed++;
        }
        queue.plan(3, 5);
        if (queue.isEmpty()) {
            failures.add("规划 3 个敌人后队列不应为空");
        } else {
            passed++;
        }
        int ticks = 0;
        int spawned = 0;
        while (!queue.isEmpty() && ticks < 200) {
            spawned += queue.tickPop();
            ticks++;
        }
        if (spawned != 3) {
            failures.add("规划 3 个敌人应生成 3 个，实际 " + spawned);
        } else {
            passed++;
        }
        if (ticks != 11) {
            failures.add("间隔 5 tick 时 3 个敌人应在 11 tick 内出场，实际 " + ticks);
        } else {
            passed++;
        }
        if (!queue.isEmpty()) {
            failures.add("生成完毕后队列应排空");
        } else {
            passed++;
        }

        SpawnQueue intervalQueue = new SpawnQueue();
        intervalQueue.plan(3, 5);
        int firstFiveTicks = 0;
        for (int i = 0; i < 5; i++) {
            firstFiveTicks += intervalQueue.tickPop();
        }
        if (firstFiveTicks != 1) {
            failures.add("首个敌人应立即出场，前 5 tick 应只生成 1 个，实际 " + firstFiveTicks);
        } else {
            passed++;
        }

        SpawnQueue appended = new SpawnQueue();
        appended.append(2);
        if (appended.tickPop() != 1 || appended.isEmpty()) {
            failures.add("append(2) 应立即出队 1 个且队列仍非空");
        } else {
            passed++;
        }

        return passed;
    }
}
