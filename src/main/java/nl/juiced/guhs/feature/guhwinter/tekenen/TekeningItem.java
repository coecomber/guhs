package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * "Tekening van &lt;maker&gt;": a drawing you can hang on a wall (always 2x2, like a painting: {@link TekeningEntity}). The
 * tooltip shows a little preview of the real drawing ({@link Voorbeeld}, drawn by the client).
 */
public class TekeningItem extends Item {
    /** The tooltip preview: which drawing. */
    public record Voorbeeld(java.util.UUID id) implements TooltipComponent {
    }

    public TekeningItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        TekeningRef ref = stack.get(TekenenFeature.TEKENING_REF.get());
        return ref == null ? super.getName(stack) : Component.translatable("item.guhs.guhwinter_tekening.van", ref.maker());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        TekeningRef ref = stack.get(TekenenFeature.TEKENING_REF.get());
        Direction kant = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(kant);
        Player speler = context.getPlayer();
        if (ref == null || kant.getAxis().isVertical() || (speler != null && !speler.mayUseItemAt(pos, kant, stack))) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        TekeningEntity e = new TekeningEntity(level, pos, kant, ref);
        if (!e.survives()) {
            if (speler != null && !level.isClientSide()) {
                speler.sendOverlayMessage(Component.translatable("guhwinter.tekenen.past_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            e.playPlacementSound();
            level.gameEvent(speler, GameEvent.ENTITY_PLACE, e.position());
            level.addFreshEntity(e);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        TekeningRef ref = stack.get(TekenenFeature.TEKENING_REF.get());
        return ref == null ? Optional.empty() : Optional.of(new Voorbeeld(ref.id()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        TekeningRef ref = stack.get(TekenenFeature.TEKENING_REF.get());
        builder.accept(Component.translatable(ref == null ? "item.guhs.guhwinter_tekening.leeg" : "item.guhs.guhwinter_tekening.lore")
                .withStyle(ChatFormatting.GRAY));
    }
}
