package nl.juiced.guhs.feature.piep;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A pieppiepmuisje you picked up (sneak + right-click your own muisje, or "Oppakken" in its menu): the whole muisje (name,
 * owner, health, its menu settings) rides along in the item ({@link PiepDierItem}). Use it on a block: it hops down there.
 * Use it in the air: it climbs onto your shoulder ({@link Schouder}).
 */
public class MuisjeItem extends PiepDierItem {
    public MuisjeItem(Properties properties) {
        super(() -> PiepFeature.PIEPPIEPMUISJE.get(), properties);
    }

    /** Picks the muisje up into the player's pockets (owner only). */
    public static boolean pakOp(PieppiepmuisjeEntity muis, ServerPlayer player) {
        return PiepDierItem.pakOp(muis, player);
    }

    /** The item holding this muisje. */
    public static ItemStack vanMuis(PieppiepmuisjeEntity muis) {
        return PiepDierItem.van(muis, PiepFeature.PIEPPIEPMUISJE_ITEM.get());
    }

    /** A fresh muisje from an item (not yet in the world); an empty item gives a new, wild one. */
    @Nullable
    public static PieppiepmuisjeEntity naarMuis(ItemStack stack, Level level) {
        return PiepDierItem.naar(stack, level, PiepFeature.PIEPPIEPMUISJE.get());
    }

    /** Puts a muisje from an item into the world at this spot. */
    @Nullable
    public static PieppiepmuisjeEntity zetNeer(ItemStack stack, ServerLevel level, Vec3 at, float yaw) {
        return PiepDierItem.zetNeer(stack, level, at, yaw, PiepFeature.PIEPPIEPMUISJE.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (Schouder.heeft(sp)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.schouder_vol").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        PieppiepmuisjeEntity muis = naarMuis(stack, level);
        if (muis == null) {
            return InteractionResult.FAIL;
        }
        Schouder.zet(sp, muis);
        stack.shrink(1);
        return InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }
}
