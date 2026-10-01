package org.lingZero.entity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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
 * 干员：固定站位，不移动，只攻击自己被阻挡的敌人。阻挡关系由关卡会话维护。
 */
public class OperatorEntity extends Mob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_DEF_ID = SynchedEntityData.defineId(OperatorEntity.class, EntityDataSerializers.STRING);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private static final String KEY_DEF_ID = "DefId";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<EnemyEntity> blockedEnemies = Collections.newSetFromMap(new IdentityHashMap<>());
    private int attackCooldown;
    @Nullable
    private OperatorDefinition definitionCache;

    public OperatorEntity(EntityType<? extends OperatorEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setDefId(Definitions.OPERATOR_GUARD.id());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEF_ID, Definitions.OPERATOR_GUARD.id());
    }

    public String getDefId() {
        return this.entityData.get(DATA_DEF_ID);
    }

    public void setDefId(String defId) {
        this.entityData.set(DATA_DEF_ID, Definitions.operator(defId).map(OperatorDefinition::id).orElse(Definitions.OPERATOR_GUARD.id()));
        this.definitionCache = null;
    }

    public OperatorDefinition definition() {
        if (this.definitionCache == null) {
            this.definitionCache = Definitions.operator(this.getDefId()).orElse(Definitions.OPERATOR_GUARD);
        }
        return this.definitionCache;
    }

    /** 由关卡会话在生成后调用，按定义修正血量。 */
    public void applyDefinition() {
        OperatorDefinition definition = this.definition();
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(definition.maxHp());
        }
        this.setHealth(definition.maxHp());
        this.setYBodyRot(this.getYRot());
        this.yHeadRot = this.getYRot();
    }

    public float getAttackDamage() {
        return this.definition().attackDamage();
    }

    public Set<EnemyEntity> blockedEnemies() {
        return this.blockedEnemies;
    }

    public int remainingCapacity() {
        int used = 0;
        for (EnemyEntity enemy : this.blockedEnemies) {
            used += enemy.getBlockCost();
        }
        return this.definition().blockCapacity() - used;
    }

    public boolean addBlocked(EnemyEntity enemy) {
        return this.blockedEnemies.add(enemy);
    }

    public void removeBlocked(EnemyEntity enemy) {
        this.blockedEnemies.remove(enemy);
    }

    public void clearBlocked() {
        this.blockedEnemies.clear();
    }

    public boolean readyToAttack() {
        return this.attackCooldown <= 0;
    }

    public void afterAttack() {
        this.attackCooldown = this.definition().attackIntervalTicks();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.getEntity() instanceof Player || super.isInvulnerableTo(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEY_DEF_ID, this.getDefId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(KEY_DEF_ID)) {
            this.setDefId(tag.getString(KEY_DEF_ID));
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
            state.getController().setAnimation(IDLE_ANIM);
            return PlayState.CONTINUE;
        }).triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
