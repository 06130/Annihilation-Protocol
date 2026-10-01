package org.lingZero.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import org.lingZero.entity.EnemyEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 资产路径由实体注册名决定：geo/entity/enemy_crawler.geo.json 等。
 */
public class EnemyRenderer extends GeoEntityRenderer<EnemyEntity> {
    public EnemyRenderer(EntityRendererProvider.Context context, EntityType<? extends EnemyEntity> entityType) {
        super(context, entityType);
    }
}
