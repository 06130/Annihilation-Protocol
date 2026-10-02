package org.lingZero.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.fml.loading.FMLEnvironment;
import org.lingZero.client.ClientDeployRequest;
import org.lingZero.entity.Definitions;

/**
 * 部署凭证：客户端只发送请求，实际生成干员由服务端在校验后执行。
 */
public class OperatorDeployerItem extends Item {
    public OperatorDeployerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (FMLEnvironment.dist.isClient() && context.getLevel().isClientSide()) {
            // 与方块放置同理：实际部署到被点击面的相邻格，避免干员半个身子埋进地里
            ClientDeployRequest.send(context.getClickedPos().relative(context.getClickedFace()), Definitions.OPERATOR_GUARD.id());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.annihilation_protocol.operator_deployer.tip"));
    }
}
