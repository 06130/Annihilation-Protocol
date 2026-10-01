package org.lingZero;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.lingZero.network.NetworkBridge;
import org.lingZero.registry.ModRegistries;
import org.slf4j.Logger;

@Mod(AnnihilationProtocolMod.MODID)
public class AnnihilationProtocolMod {
    public static final String MODID = "annihilation_protocol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnnihilationProtocolMod(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NetworkBridge.init();
        LOGGER.info("[AP] Annihilation Protocol 初始化完成");
    }
}
