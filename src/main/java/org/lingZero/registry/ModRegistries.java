package org.lingZero.registry;

import net.neoforged.bus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreation);
    }
}
