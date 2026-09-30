package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import nl.juiced.guhs.quest.Reisguh;
import nl.juiced.guhs.world.ModDimensions;

/** Blow it on a spot in the Guhmension and a Reisguh sits down there: a waypoint of your own. */
public class ReisguhFluitjeItem extends Item {
    public ReisguhFluitjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() != ModDimensions.GUHMENSION) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("quest.guhs.reis.only_guhmension").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.FAIL;
        }
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isEmptyBlock(at) || !level.isEmptyBlock(at.above())) {
            return InteractionResult.FAIL;
        }
        String name = player == null ? "Reisguh"
                : Component.translatable("quest.guhs.reis.own_name", player.getName().getString()).getString();
        Reisguh.place(level, at, player == null ? 0 : player.getYRot() + 180, name);
        level.playSound(null, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.5f);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.reisguh_fluitje.lore").withStyle(ChatFormatting.GRAY));
    }
}
