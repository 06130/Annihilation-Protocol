package org.lingZero.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.Config;
import org.lingZero.battle.DeployResult;
import org.lingZero.battle.LevelDefinition;
import org.lingZero.entity.Definitions;
import org.lingZero.entity.OperatorDefinition;
import org.lingZero.entity.OperatorEntity;
import org.lingZero.registry.ModEntities;

/**
 * 部署校验与生成。所有入口（命令、物品网络请求）都必须经过这里。
 */
public final class DeployService {
    private DeployService() {
    }

    /**
     * @param player 玩家发起时用于距离与冷却校验；控制台或脚本部署时可为 null
     */
    public static DeployResult deploy(@Nullable ServerPlayer player, ServerLevel level, BlockPos pos, String operatorId) {
        if (player != null && player.level() != level) {
            return DeployResult.WRONG_DIMENSION;
        }
        BattleSession session = SessionManager.get(level);
        if (session == null) {
            return DeployResult.NO_SESSION;
        }
        if (session.state().isFinished()) {
            return DeployResult.INVALID_STATE;
        }
        LevelDefinition definition = session.definition();
        if (!definition.isConfigured()) {
            return DeployResult.NOT_CONFIGURED;
        }
        double maxDistance = Config.maxDeployDistance;
        if (player != null && player.distanceToSqr(Vec3.atCenterOf(pos)) > maxDistance * maxDistance) {
            return DeployResult.TOO_FAR;
        }
        if (!definition.areaContains(pos)) {
            return DeployResult.OUT_OF_AREA;
        }
        if (!level.getEntitiesOfClass(OperatorEntity.class, new AABB(pos)).isEmpty()) {
            return DeployResult.OCCUPIED;
        }
        if (definition.isOnPath(pos)) {
            return DeployResult.ON_PATH;
        }
        if (session.operatorCount() >= Config.maxOperators) {
            return DeployResult.LIMIT_REACHED;
        }
        if (player != null && session.isOnDeployCooldown(player)) {
            return DeployResult.COOLDOWN;
        }
        OperatorDefinition operatorDefinition = Definitions.operator(operatorId).orElse(null);
        if (operatorDefinition == null) {
            return DeployResult.INVALID_TYPE;
        }
        OperatorEntity operator = ModEntities.OPERATOR_GUARD.get().create(level);
        if (operator == null) {
            AnnihilationProtocolMod.LOGGER.error("[AP] 干员实体创建失败：{}", operatorDefinition.id());
            return DeployResult.SERVER_ERROR;
        }
        operator.setDefId(operatorDefinition.id());
        float yRot = player != null ? player.getYRot() + 180.0F : 0.0F;
        operator.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0.0F);
        operator.applyDefinition();
        if (!level.addFreshEntity(operator)) {
            AnnihilationProtocolMod.LOGGER.error("[AP] 干员实体加入世界失败：{} @ {}", operatorDefinition.id(), pos);
            return DeployResult.SERVER_ERROR;
        }
        session.trackOperator(operator);
        if (player != null) {
            session.recordDeploy(player);
        }
        session.markDirty();
        return DeployResult.SUCCESS;
    }
}
