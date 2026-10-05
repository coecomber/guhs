package nl.juiced.guhs.feature.bleekwoud;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LecternBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.Terugkeer;

/** The lectern of het Houthakkershutje (a diary for everybody) and name tags on heart creatures (refused). */
public final class BleekwoudEvents {
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND
                || player.level().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        if (isDagboekLessenaar(player.level(), event.getPos())) {
            Dagboek.geef(player);
        }
    }

    /** The lectern in a Houthakkershutje? */
    public static boolean isDagboekLessenaar(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LecternBlock && !Terugkeer.stukken(level, BleekwoudFeature.HUTJE, pos, null).isEmpty();
    }

    /** A name tag would make a heart's creature "yours" and keep it: it belongs to its heart, so the tag is refused. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof KraakWezen w && w.hart() != null && event.getItemStack().is(Items.NAME_TAG)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.bleekwoud.geen_naam").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private BleekwoudEvents() {
    }
}
