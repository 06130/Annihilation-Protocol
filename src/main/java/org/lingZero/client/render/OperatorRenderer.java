package org.lingZero.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import org.lingZero.entity.OperatorEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OperatorRenderer extends GeoEntityRenderer<OperatorEntity> {
    public OperatorRenderer(EntityRendererProvider.Context context, EntityType<? extends OperatorEntity> entityType) {
        super(context, entityType);
    }
}
