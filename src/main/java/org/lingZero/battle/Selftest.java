package org.lingZero.battle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * 纯逻辑自测，由 /ap selftest 触发。
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
        if (definition.isDeployable(new BlockPos(2, 0, 0))) {
            failures.add("路径上的方块不可部署");
        } else {
            passed++;
        }
        if (!definition.isDeployable(new BlockPos(2, 0, 4))) {
            failures.add("区域内且不在路径上的方块应可部署");
        } else {
            passed++;
        }
        if (definition.isDeployable(new BlockPos(20, 0, 0))) {
            failures.add("区域外的方块不可部署");
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

        return new Result(passed, failures.size(), failures);
    }
}
