package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A knabbelkristal to put on a knabbelsokkel: four of them on the four sokkels around the terugportaal on the
 * Knabbelberg call Opper-Mika back for another fight (see {@link GuheindeGevecht#tryRespawn}).
 */
public class KnabbelkristalItem extends Item {
    public KnabbelkristalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(GuheindeFeature.KNABBELSOKKEL.get())) {
            return InteractionResult.FAIL;
        }
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above)) {
            return InteractionResult.FAIL;
        }
        double x = above.getX(), y = above.getY(), z = above.getZ();
        if (!level.getEntities(null, new AABB(x, y, z, x + 1, y + 2, z + 1)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            KnabbelkristalEntity crystal = new KnabbelkristalEntity(server, x + 0.5, y, z + 0.5);
            crystal.setShowBottom(false);
            server.addFreshEntity(crystal);
            server.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, above);
            GuheindeGevecht fight = GuheindeGevecht.of(server);
            if (fight != null) {
                fight.tryRespawn(context.getPlayer());
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.knabbelkristal.lore").withStyle(ChatFormatting.GRAY));
    }
}
