package nl.juiced.guhs.feature.einde2.vesting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice vesting: the Guhvleugels in the cabin of a vetschip are there for every player. The frame of a modular
 * vesting's ship is fixed and carries {@link #TAG} (tools/features/einde2_vesting_bouw.py): nobody takes the wings out of
 * it, a click (or a tap) hands the player a pair of their own, once per frame, so once per vesting. The ships of the
 * vestingen from before einde2 keep their plain frame: first come, first served, as it was.
 */
public final class VestingVleugels {
    /** Entity tag of the item frame with the Guhvleugels on a vetschip of a modular vesting. */
    public static final String TAG = "guhs_einde2_vesting_vleugels";
    /** Player data (GuhQuests.saved): the frames (their UUIDs) this player took a pair of wings from. */
    public static final String GEPAKT = "guhs_einde2_vesting_vleugels";

    /** A right-click on the frame: the click itself comes first (cancelled here, so nothing else sees it). */
    static void onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof ItemFrame frame && frame.entityTags().contains(TAG)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            klik(event, frame);
        }
    }

    /** The same for an interaction that did not come from a click on a spot of the frame (it only fires this one). */
    static void onInteractGewoon(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ItemFrame frame && frame.entityTags().contains(TAG)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            klik(event, frame);
        }
    }

    private static void klik(PlayerInteractEvent event, ItemFrame frame) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {
            pak(player, frame);
        }
    }

    static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ItemFrame frame) || !frame.entityTags().contains(TAG)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            pak(player, frame);
        }
    }

    /** Did this player take the wings of this frame already? */
    public static boolean heeft(ServerPlayer player, ItemFrame frame) {
        return GuhQuests.saved(player).getCompoundOrEmpty(GEPAKT).contains(frame.getUUID().toString());
    }

    /** This player's own pair of wings from this frame (false: they had it already; the frame keeps showing its pair). */
    public static boolean pak(ServerPlayer player, ItemFrame frame) {
        if (heeft(player, frame)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vesting.vleugels.al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        CompoundTag saved = GuhQuests.saved(player);
        CompoundTag gepakt = saved.getCompoundOrEmpty(GEPAKT);
        gepakt.putBoolean(frame.getUUID().toString(), true);
        saved.put(GEPAKT, gepakt);
        ItemStack vleugels = frame.getItem().is(GuheindeFeature.GUHVLEUGELS.get()) ? frame.getItem().copyWithCount(1) : new ItemStack(GuheindeFeature.GUHVLEUGELS.get());
        Minigames.give(player, vleugels);
        ServerLevel level = player.level();
        level.playSound(null, frame.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, frame.getX(), frame.getY(), frame.getZ(), 8, 0.3, 0.3, 0.3, 0);
        player.sendSystemMessage(Component.translatable("gui.guhs.einde2_vesting.vleugels.gepakt").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private VestingVleugels() {
    }
}
