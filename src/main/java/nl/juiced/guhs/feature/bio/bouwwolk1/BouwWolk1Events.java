package nl.juiced.guhs.feature.bio.bouwwolk1;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature;

/**
 * The game-bus side of slice bouw-wolk1. Everything here reacts to something a player does; nothing runs per tick.
 * <ul>
 *   <li>the wolkenhoeder's lesson goes on as the player shears a wolkenschaapje, makes a cloud block, builds a cloud
 *       stair, and sits on a cloud bench or lies on a cloud bed ({@link Hoeder});</li>
 *   <li>the fold's herd: no lead on a herd schaapje, no lammetje from one ({@link Kudde});</li>
 *   <li>the telescope at the ruin: sterrenstof once a night ({@link Sterrenkijker});</li>
 *   <li>the haven balloon: no sneaking out of the basket in flight; logging out in flight puts you back on the jetty
 *       ({@link HavenBallonEntity}, {@link Ballonvaarder}).</li>
 * </ul>
 */
public final class BouwWolk1Events {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer speler) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Entity doel = event.getTarget();
        ItemStack hand = event.getItemStack();
        if (Kudde.isKudde(doel) && hand.is(Items.LEAD)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.wolkenhoeder_hut.kudde_blijft").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (isScheren(speler, doel, hand)) {
            Hoeder.geschoren(speler);
        }
    }

    /** Is this click going to shear a wolkenschaapje (asked before the click does it)? */
    static boolean isScheren(ServerPlayer speler, Entity doel, ItemStack hand) {
        return hand.is(Items.SHEARS) && Kudde.soort().filter(t -> t == doel.getType()).isPresent()
                && doel instanceof IShearable schaap && schaap.isShearable(speler, hand, doel.level(), doel.blockPosition());
    }

    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (Kudde.isKudde(event.getParentA()) || Kudde.isKudde(event.getParentB())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer speler) {
            Hoeder.gemaakt(speler, event.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer speler && !event.isCanceled() && Hoeder.isWolk(event.getPlacedBlock())) {
            Hoeder.geplaatst(speler, event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer speler) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (Hoeder.isRustplek(state)) {
            Hoeder.gerust(speler);
        } else if (state.is(SterrenwachtFeature.TELESCOOP.get())) {
            Sterrenkijker.kijk(speler, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof HavenBallonEntity ballon
                && ballon.onderweg() && !ballon.magUit && event.getEntityMounting() instanceof ServerPlayer speler && speler.isAlive()
                && !speler.isRemoved() && speler.isShiftKeyDown()) {
            event.setCanceled(true);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.luchtballon_haven.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer speler) {
            Ballonvaarder.uitloggen(speler);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        BouwWolk1Commando.registreer(event);
    }

    private BouwWolk1Events() {
    }
}
