package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Laying out a Guh-parkour route by clicking, in two ways that share the rules of {@link Routes#klik}:
 * <ul>
 *   <li>with the <b>Startpaaltje in your hand</b>: right-click toys and obstacles one by one (1, 2, 3 ...) up to the
 *       Finishpaaltje; the item remembers the list and the post takes it over when you place it;</li>
 *   <li>on a <b>placed</b> Startpaaltje: sneak + right-click it (or the button in its screen) and click more pieces; click
 *       the post again when you are done. A Scorebord clicked this way is linked to that post.</li>
 * </ul>
 * A second click on a piece takes it out. While you lay out, a pink dotted line shows the route (to you only).
 */
public final class Uitzetten {
    /** Laying out on a placed post: which one, and until when (game time). */
    private record Modus(ResourceKey<Level> dim, BlockPos paal, long tot) {
    }

    private static final Map<UUID, Modus> MODUS = new ConcurrentHashMap<>();
    private static final int DUUR = 20 * 60 * 5;

    private static net.minecraft.network.chat.MutableComponent roze(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    // --- on a placed post --------------------------------------------------------------------------------------------------------

    /** Is this player laying out the route of the post at pos? */
    public static boolean bezig(ServerPlayer p, BlockPos pos) {
        Modus m = MODUS.get(p.getUUID());
        return m != null && m.dim() == p.level().dimension() && m.paal().equals(pos);
    }

    public static boolean bezig(ServerPlayer p) {
        return MODUS.containsKey(p.getUUID());
    }

    public static boolean start(ServerPlayer p, StartpaalBlockEntity paal) {
        if (!paal.magBewerken(p)) {
            p.sendOverlayMessage(roze("gui.guhs.guhparkour.niet_van_jou", paal.eigenaarNaam()));
            return false;
        }
        MODUS.put(p.getUUID(), new Modus(p.level().dimension(), paal.getBlockPos().immutable(), p.level().getGameTime() + DUUR));
        p.sendSystemMessage(roze("gui.guhs.guhparkour.uitzet.modus_aan"));
        return true;
    }

    public static void stop(ServerPlayer p, boolean melden) {
        Modus m = MODUS.remove(p.getUUID());
        if (m != null && melden) {
            int n = p.level().getBlockEntity(m.paal()) instanceof StartpaalBlockEntity paal ? paal.stukken().size() : 0;
            p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.modus_uit", n));
        }
    }

    @Nullable
    private static StartpaalBlockEntity paalVan(ServerPlayer p) {
        Modus m = MODUS.get(p.getUUID());
        if (m == null) {
            return null;
        }
        if (m.dim() != p.level().dimension() || p.level().getGameTime() > m.tot() || !p.blockPosition().closerThan(m.paal(), Routes.BEREIK + 24)
                || !(p.level().getBlockEntity(m.paal()) instanceof StartpaalBlockEntity paal)) {
            stop(p, true);
            return null;
        }
        return paal;
    }

    // --- the clicks ----------------------------------------------------------------------------------------------------------------

    /** What a click made of a list says to the player (and a little sound). */
    private static void meld(ServerPlayer p, Routes.Uitkomst u, List<BlockPos> stukken, BlockPos stuk, Component naam) {
        switch (u) {
            case ERBIJ -> p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.erbij", Routes.nummer(stukken, stuk), naam));
            case FINISH -> p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.finish", Math.max(0, stukken.size() - 1)).withStyle(ChatFormatting.GOLD));
            case ERUIT -> p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.eruit", naam));
            case VOL -> p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.vol", Routes.MAX_STUKKEN));
            case TE_VER -> p.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.te_ver", Routes.BEREIK));
            default -> {
            }
        }
        if (u == Routes.Uitkomst.ERBIJ || u == Routes.Uitkomst.FINISH || u == Routes.Uitkomst.ERUIT) {
            float toon = u == Routes.Uitkomst.ERUIT ? 0.7f : u == Routes.Uitkomst.FINISH ? 1.6f : 0.9f + 0.04f * stukken.size();
            p.level().playSound(null, stuk, ParkourSlice.KLIK.get(), SoundSource.PLAYERS, 0.7f, toon);
        }
    }

    /** A click on a piece with the Startpaaltje in the hand: the item's own list. */
    public static Routes.Uitkomst klikMetItem(ServerPlayer p, ItemStack stack, BlockPos stuk) {
        List<BlockPos> stukken = StartpaalBlock.route(stack);
        Component naam = Routes.naam(p.level(), stuk);
        Routes.Uitkomst u = Routes.klik(p.level(), stukken, stukken.isEmpty() ? null : stukken.get(0), stuk);
        if (u == Routes.Uitkomst.ERBIJ || u == Routes.Uitkomst.FINISH || u == Routes.Uitkomst.ERUIT) {
            StartpaalBlock.zetRoute(stack, stukken);
        }
        meld(p, u, stukken, stuk, naam);
        return u;
    }

    /** A click on a piece while laying out on a placed post. */
    public static Routes.Uitkomst klikInModus(ServerPlayer p, StartpaalBlockEntity paal, BlockPos stuk) {
        Component naam = Routes.naam(p.level(), stuk);
        Routes.Uitkomst u = paal.klik(stuk);
        meld(p, u, paal.stukken(), stuk, naam);
        return u;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onKlik(PlayerInteractEvent.RightClickBlock event) {
        Player speler = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || speler.isSpectator()) {
            return;
        }
        Level level = event.getLevel();
        ItemStack hand = speler.getMainHandItem();
        boolean metItem = hand.is(ParkourSlice.STARTPAALTJE_ITEM.get()) && !speler.isShiftKeyDown();
        BlockPos stuk = Routes.stuk(level, event.getPos());
        if (metItem && stuk != null) {
            // (both sides: or the client would place the post against the toy)
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (speler instanceof ServerPlayer sp) {
                klikMetItem(sp, hand, stuk);
            }
            return;
        }
        if (!(speler instanceof ServerPlayer sp) || !bezig(sp)) {
            return;
        }
        StartpaalBlockEntity paal = paalVan(sp);
        if (paal == null) {
            return;
        }
        if (stuk != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            klikInModus(sp, paal, stuk);
        } else if (level.getBlockEntity(event.getPos()) instanceof ScorebordBlockEntity bord) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!bord.getBlockPos().closerThan(paal.getBlockPos(), Routes.BEREIK + 0.5)) {
                sp.sendOverlayMessage(roze("gui.guhs.guhparkour.uitzet.te_ver", Routes.BEREIK));
            } else {
                bord.koppel(paal.getBlockPos());
                bord.zet(paal.rijen());
                sp.sendOverlayMessage(roze("gui.guhs.guhparkour.bord.gekoppeld"));
                level.playSound(null, bord.getBlockPos(), ParkourSlice.KLIK.get(), SoundSource.PLAYERS, 0.7f, 1.3f);
            }
        }
    }

    /** The dotted line: twice a second for whoever is laying out (on a post, or with a post that remembers pieces in the hand). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0 || !(p.level() instanceof ServerLevel)) {
            return;
        }
        if (bezig(p)) {
            StartpaalBlockEntity paal = paalVan(p);
            if (paal != null) {
                Routes.toon(p, paal.getBlockPos(), paal.stukken());
            }
            return;
        }
        ItemStack hand = p.getMainHandItem();
        if (hand.is(ParkourSlice.STARTPAALTJE_ITEM.get())) {
            List<BlockPos> stukken = StartpaalBlock.route(hand);
            if (!stukken.isEmpty()) {
                Routes.toon(p, null, stukken);
            }
        }
    }

    @SubscribeEvent
    public static void onUit(PlayerEvent.PlayerLoggedOutEvent event) {
        MODUS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onGestopt(ServerStoppedEvent event) {
        MODUS.clear();
    }

    private Uitzetten() {
    }
}
