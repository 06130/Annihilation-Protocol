package org.lingZero.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.entity.Definitions;
import org.lingZero.entity.EnemyEntity;
import org.lingZero.entity.OperatorEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AnnihilationProtocolMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EnemyEntity>> ENEMY_CRAWLER = ENTITIES.register("enemy_crawler",
            () -> EntityType.Builder.of(EnemyEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("enemy_crawler"));

    public static final DeferredHolder<EntityType<?>, EntityType<OperatorEntity>> OPERATOR_GUARD = ENTITIES.register("operator_guard",
            () -> EntityType.Builder.of(OperatorEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("operator_guard"));

    private ModEntities() {
    }

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ENEMY_CRAWLER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Definitions.ENEMY_CRAWLER.maxHp())
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .build());
        event.put(OPERATOR_GUARD.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Definitions.OPERATOR_GUARD.maxHp())
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .build());
    }
}
