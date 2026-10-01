package org.lingZero.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lingZero.AnnihilationProtocolMod;
import org.lingZero.item.OperatorDeployerItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnnihilationProtocolMod.MODID);

    public static final DeferredItem<OperatorDeployerItem> OPERATOR_DEPLOYER =
            ITEMS.registerItem("operator_deployer", OperatorDeployerItem::new, new Item.Properties().stacksTo(1));

    private ModItems() {
    }
}
