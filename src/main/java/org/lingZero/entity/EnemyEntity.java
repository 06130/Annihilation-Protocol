package org.lingZero.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lingZero.Config;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 敌人：沿固定路径点移动，被阻挡时停下，不接受任何原版 AI 或寻路控制。
 */
public class EnemyEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_DEF_ID = SynchedEntityData.defineId(EnemyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIM_STATE = SynchedEntityData.defineId(EnemyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLOCKER_ID = SynchedEntityData.defineId(EnemyEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation MOVE_ANIM = RawAnimation.begin().thenLoop("move");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private static final String KEY_DEF_ID = "DefId";
    private static final String KEY_WAYPOINT = "Waypoint";
    private static final String KEY_GATE = "Gate";
    private static final String KEY_PATH = "Path";

    public static final double ARRIVE_RADIUS = 0.25;
    private static final double MAX_Y_STEP = 0.25;
    private static final int ATTACK_ANIM_TICKS = 10;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final List<BlockPos> path = new ArrayList<>();
    @Nullable
    private BlockPos gatePos;
    private int waypointIndex;
    private int attackCooldown;
    private int attackAnimTicks;
    @Nullable
    private OperatorEntity blocker;
    @Nullable
    private EnemyDefinition definitionCache;

    public EnemyEntity(EntityType<? extends EnemyEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setDefId(Definitions.ENEMY_CRAWLER.id());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEF_ID, Definitions.ENEMY_CRAWLER.id());
        builder.define(DATA_ANIM_STATE, EnemyAnimState.MOVE.ordinal());
        builder.define(DATA_BLOCKER_ID, -1);
    }

    public String getDefId() {
        return this.entityData.get(DATA_DEF_ID);
    }

    public void setDefId(String defId) {
        this.entityData.set(DATA_DEF_ID, Definitions.enemyOrFallback(defId).id());
        this.definitionCache = null;
    }

    public EnemyDefinition definition() {
        if (this.definitionCache == null) {
            this.definitionCache = Definitions.enemyOrFallback(this.getDefId());
        }
        return this.definitionCache;
    }

    public EnemyAnimState getAnimState() {
        return EnemyAnimState.byOrdinal(this.entityData.get(DATA_ANIM_STATE));
    }

    private void setAnimState(EnemyAnimState state) {
        this.entityData.set(DATA_ANIM_STATE, state.ordinal());
    }

    public int getBlockCost() {
        return this.definition().blockCost();
    }

    public float getAttackDamage() {
        return this.definition().attackDamage();
    }

    public int getBlockerId() {
        return this.entityData.get(DATA_BLOCKER_ID);
    }

    @Nullable
    public OperatorEntity getBlocker() {
        return this.blocker;
    }

    public void setBlocker(@Nullable OperatorEntity blocker) {
        this.blocker = blocker;
        this.entityData.set(DATA_BLOCKER_ID, blocker == null ? -1 : blocker.getId());
    }

    /** 由关卡会话在生成后调用，注入路径与终点。 */
    public void configure(List<BlockPos> path, BlockPos gatePos) {
        this.path.clear();
        this.path.addAll(path);
        this.gatePos = gatePos.immutable();
        this.waypointIndex = 0;
        EnemyDefinition definition = this.definition();
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(definition.maxHp());
        }
        this.setHealth(definition.maxHp());
    }

    public boolean readyToAttack() {
        return this.attackCooldown <= 0;
    }

    public void afterAttack() {
        this.attackCooldown = this.definition().attackIntervalTicks();
        this.attackAnimTicks = ATTACK_ANIM_TICKS;
    }

    private void updateHpTag() {
        if (!Config.showHpNameTag) {
            this.setCustomNameVisible(false);
            return;
        }
        this.setCustomName(Component.literal("HP " + (int) Math.ceil(this.getHealth()) + "/" + (int) this.definition().maxHp()));
        this.setCustomNameVisible(true);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        if (!this.level().isClientSide) {
            this.updateHpTag();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.serverTick();
        }
    }

    private void serverTick() {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.attackAnimTicks > 0) {
            this.attackAnimTicks--;
        }

        OperatorEntity currentBlocker = this.blocker;
        if (currentBlocker != null && currentBlocker.isAlive() && !currentBlocker.isRemoved()) {
            this.faceTowards(currentBlocker.getX(), currentBlocker.getZ());
            this.setAnimState(this.attackAnimTicks > 0 ? EnemyAnimState.ATTACK : EnemyAnimState.BLOCKED);
            return;
        }

        this.setAnimState(this.attackAnimTicks > 0 ? EnemyAnimState.ATTACK : EnemyAnimState.MOVE);
        this.moveAlongPath();
    }

    private void moveAlongPath() {
        BlockPos target = this.waypointIndex < this.path.size() ? this.path.get(this.waypointIndex) : this.gatePos;
        if (target == null) {
            return;
        }
        double dx = target.getX() + 0.5 - this.getX();
        double dz = target.getZ() + 0.5 - this.getZ();
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        if (distXZ < ARRIVE_RADIUS) {
            if (this.waypointIndex < this.path.size()) {
                this.waypointIndex++;
            }
            return;
        }
        double step = Math.min(this.definition().moveSpeed(), distXZ);
        double dy = Mth.clamp(target.getY() - this.getY(), -MAX_Y_STEP, MAX_Y_STEP);
        this.move(MoverType.SELF, new Vec3(dx / distXZ * step, dy, dz / distXZ * step));
        this.setDeltaMovement(Vec3.ZERO);
        this.faceTowards(target.getX() + 0.5, target.getZ() + 0.5);
    }

    private void faceTowards(double x, double z) {
        double dx = x - this.getX();
        double dz = z - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYHeadRot(yaw);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_DEF_ID, this.getDefId());
        tag.putInt(KEY_WAYPOINT, this.waypointIndex);
        if (this.gatePos != null) {
            tag.put(KEY_GATE, new IntArrayTag(new int[]{this.gatePos.getX(), this.gatePos.getY(), this.gatePos.getZ()}));
        }
        ListTag pathTag = new ListTag();
        for (BlockPos pos : this.path) {
            pathTag.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put(KEY_PATH, pathTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY_DEF_ID)) {
            this.setDefId(tag.getString(KEY_DEF_ID));
        }
        this.waypointIndex = tag.getInt(KEY_WAYPOINT);
        if (tag.get(KEY_GATE) instanceof IntArrayTag gate) {
            int[] values = gate.getAsIntArray();
            this.gatePos = new BlockPos(values[0], values[1], values[2]);
        }
        this.path.clear();
        ListTag pathTag = tag.getList(KEY_PATH, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < pathTag.size(); i++) {
            if (pathTag.get(i) instanceof IntArrayTag point) {
                int[] values = point.getAsIntArray();
                this.path.add(new BlockPos(values[0], values[1], values[2]));
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
    }

    @Override
    public void pushEntities() {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            state.getController().setAnimation(this.getAnimState() == EnemyAnimState.MOVE ? MOVE_ANIM : IDLE_ANIM);
            return PlayState.CONTINUE;
        }).triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
