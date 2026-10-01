package org.lingZero.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.lingZero.entity.Definitions;
import org.lingZero.network.APClient;

/**
 * 部署凭证：客户端只发送请求，实际生成干员由服务端在校验后执行。
 */
public class OperatorDeployerItem extends Item {
    public OperatorDeployerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            APClient.sendDeployRequest(context.getClickedPos(), Definitions.OPERATOR_GUARD.id());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.annihilation_protocol.operator_deployer.tip"));
    }
}
