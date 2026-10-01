package org.lingZero.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.client.render.EnemyRenderer;
import org.lingZero.client.render.OperatorRenderer;
import org.lingZero.registry.ModEntities;

@EventBusSubscriber(modid = AnnihilationProtocolMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENEMY_CRAWLER.get(),
                context -> new EnemyRenderer(context, ModEntities.ENEMY_CRAWLER.get()));
        event.registerEntityRenderer(ModEntities.OPERATOR_GUARD.get(),
                context -> new OperatorRenderer(context, ModEntities.OPERATOR_GUARD.get()));
    }
}
