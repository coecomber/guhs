package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import net.minecraft.network.chat.Component;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.3), slice sp_d1_start: part 1 steps 0-4 (the bubble letter, the trip to Bikinibodem, the
 * bubbelstroom and the welcome, the job interview, baking Knabbelburgers) and the narrator card of part 1. Wired in
 * feature/einde2/Einde2 with the marker {@code <einde2_sp_d1_start/>}. What it registers (common code, both sides):
 * <ul>
 *   <li>its five steps of {@link SpVerhaal#RECEPT_LIJN} ({@code SpStappen.echt}), their compass targets, the variant
 *       {@code 4_b} (the grill game on), their {@code ga} starts and a {@link StapHint} for every step;</li>
 *   <li>the six scenes and two look scenes ({@link D1Scenes}), the narrator card;</li>
 *   <li>the source of the Krokante Knabbel cap ({@value E2SpD1Start#KLEDINGBRON});</li>
 *   <li>the step tick ({@link #tick}), the clicks on its own display entities, the cast's small talk
 *       ({@code SponsguhWezen.BIJ_KLIK}), the dev commands ({@link D1Commando}).</li>
 * </ul>
 * No registries, payloads or client code of its own: everything this slice shows are vanilla display entities.
 */
public final class E2SpD1StartFeature {
    private E2SpD1StartFeature() {
    }

    public static void register(IEventBus modBus) {
        String lijn = SpVerhaal.RECEPT;
        for (int s = SpVerhaal.R_BRIEF; s <= SpVerhaal.R_BAKKEN; s++) {
            SpStappen.echt(lijn, s);
        }
        D1Scenes.registreer();
        KledingBronnen.bron(GuhClothes.SPONSGUH_KNABBELPETJE, E2SpD1Start.KLEDINGBRON);
        SpStappen.sleutel(lijn, SpVerhaal.R_BAKKEN, (p, s) -> Bakken.bezig(p) ? s + "_b" : String.valueOf(s));
        SpStappen.doel(lijn, SpVerhaal.R_BRIEF, (p, s) -> null);
        SpStappen.doel(lijn, SpVerhaal.R_BUBBEL, E2SpD1StartFeature::doelBubbel);
        SpStappen.doel(lijn, SpVerhaal.R_SOLLICITATIE, (p, s) -> doel(p, s, "krabguh"));
        SpStappen.doel(lijn, SpVerhaal.R_BAKKEN, (p, s) -> doel(p, s, "grill_speler"));
        // the dev command "ga" (after it put the player at the step's default place)
        SpStappen.ga(lijn, SpVerhaal.R_BRIEF, p -> {
            Bubbelbrief.nu(p);
            if (GuheindeFeature.isGuheinde(p.level())) {
                Bubbelbrief.zetNu(p);
            }
        });
        SpStappen.ga(lijn, SpVerhaal.R_BUBBEL, p -> E2SpD1Start.zetNaar(p, "bubbel_boven", 0f));
        SpStappen.ga(lijn, SpVerhaal.R_SOLLICITATIE, p -> E2SpD1Start.zetNaar(p, "krabguh", 180f));
        SpStappen.ga(lijn, SpVerhaal.R_BAKKEN, p -> {
            Bakken.stop(p);
            E2SpD1Start.lijn().teller(p, E2SpD1Start.T_BESTELD, 0);
            Cutscenes.vergeet(p, D1Scenes.BAKKEN_UITLEG.id());
            E2SpD1Start.zetNaar(p, "grill_speler", 180f);
        });
        hints();
        // a step that ended (also by a dev command) takes its things along
        SpVerhaal.RECEPT_LIJN.opStap((p, oud, nieuw) -> {
            if (oud == SpVerhaal.R_BRIEF) {
                Bubbelbrief.weg(p);
            }
            if (oud <= SpVerhaal.R_BAKKEN && nieuw > SpVerhaal.R_BAKKEN) {
                Bakken.stop(p);
            }
        });
        SponsguhWezen.BIJ_KLIK.add(D1Stad::klik);
        NeoForge.EVENT_BUS.addListener(E2SpD1StartFeature::opTick);
        NeoForge.EVENT_BUS.addListener(E2SpD1StartFeature::opUitloggen);
        NeoForge.EVENT_BUS.addListener(D1Tijdelijk::opJoin);
        NeoForge.EVENT_BUS.addListener(E2SpD1StartFeature::opInteract);
        NeoForge.EVENT_BUS.addListener(E2SpD1StartFeature::opInteractSpecifiek);
        NeoForge.EVENT_BUS.addListener(E2SpD1StartFeature::opAanval);
        NeoForge.EVENT_BUS.addListener(D1Commando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    // =====================================================================================================================
    // targets and hints (CONTRACT_E2_C 4.4: every step says what and where; "Verder" brings you back)
    // =====================================================================================================================

    @Nullable
    private static Doel doel(ServerPlayer p, int stap, String marker) {
        ServerLevel g = E2SpD1Start.guheinde(p.level().getServer());
        BlockPos b = E2SpD1Start.plek(g, marker);
        return b == null ? null : Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + SpVerhaal.RECEPT + ".waar." + stap));
    }

    /** Step 2: the top of the stream while the player is up on the island, its foot once they are in the town. */
    @Nullable
    private static Doel doelBubbel(ServerPlayer p, int stap) {
        ServerLevel g = E2SpD1Start.guheinde(p.level().getServer());
        BlockPos boven = E2SpD1Start.plek(g, "bubbel_boven"), onder = E2SpD1Start.plek(g, "bubbel_onder");
        if (boven == null || onder == null) {
            return null;
        }
        BlockPos b = p.getY() > onder.getY() + 40 || !GuheindeFeature.isGuheinde(p.level()) ? boven : onder;
        return Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + SpVerhaal.RECEPT + ".waar." + stap));
    }

    private static void hints() {
        String lijn = SpVerhaal.RECEPT, bb = E2SpD1Start.BB, h = E2SpD1Start.P + "hint.";
        // 0: the bubble finds you in the Guheinde; outside it "Verder" brings you to the kaasbol at the Knabbelberg
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> p.level() == level ? p.blockPosition() : E2Hervat.KAASBOL.apply(level)))
                .tekst(h + "brief").zonderKijk().zet(lijn, SpVerhaal.R_BRIEF);
        // 1: the island above the dome (far away: no look scene; Appa's list or the compass)
        StapHint.naar(bb, "aankomst_speler").tekst(h + "reis").zonderKijk().bijStart().baken().zet(lijn, SpVerhaal.R_REIS);
        // 2: the top of the stream, right next to where you land
        StapHint.naar(bb, "bubbel_boven").tekst(h + "bubbel").kijk(D1Scenes.KIJK_BUBBEL).bijStart().baken().zet(lijn, SpVerhaal.R_BUBBEL);
        // 3: the door of De Krokante Knabbel on the Hoofdstraat (inside, the light stands over Krabguh: D1Stad)
        StapHint.naar(bb, "d1s_knabbel_deur").tekst(h + "knabbel").kijk(D1Scenes.KIJK_KNABBEL).bijStart().baken().zet(lijn, SpVerhaal.R_SOLLICITATIE);
        // 4: the grill (the kitchen door is in the back wall of the hall, on the left); "Verder" puts you behind it
        StapHint.naar(bb, "grill_speler").tekst(h + "grill").zonderKijk().bijStart().baken().zet(lijn, SpVerhaal.R_BAKKEN);
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || p.level().getServer() == null) {
            return;
        }
        if ((p.level().getServer().getTickCount() + p.getId()) % 5 != 0) {
            return;
        }
        tick(p);
    }

    /** Every 5 ticks per player: the step of this slice that is on does its part; a finished step's things go. */
    static void tick(ServerPlayer p) {
        boolean aan = SpVerhaal.actief(p, SpVerhaal.RECEPT);
        int s = aan ? E2SpD1Start.stap(p) : -1;
        if (s != SpVerhaal.R_BRIEF) {
            Bubbelbrief.weg(p);
        }
        if (s != SpVerhaal.R_BAKKEN) {
            Bakken.stop(p);
        }
        if (s < 0 || s > SpVerhaal.R_BAKKEN || !SpVerhaal.open(p)) {
            return;
        }
        boolean daar = E2SpD1Start.inBikinibodem(p);
        switch (s) {
            case SpVerhaal.R_BRIEF -> Bubbelbrief.tick(p);
            case SpVerhaal.R_REIS -> {
                if (daar) {
                    D1Stad.tickReis(p);
                }
            }
            case SpVerhaal.R_BUBBEL -> {
                if (daar) {
                    D1Stad.tickBubbel(p);
                }
            }
            case SpVerhaal.R_SOLLICITATIE -> {
                if (daar) {
                    D1Stad.tickSollicitatie(p);
                }
            }
            case SpVerhaal.R_BAKKEN -> {
                if (daar || Bakken.spel(p) != null) {
                    Bakken.tick(p);
                }
            }
            default -> {
            }
        }
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        java.util.UUID u = event.getEntity().getUUID();
        Bubbelbrief.vergeet(u);
        Bakken.vergeet(u);
        D1Stad.vergeet(u);
    }

    /** A click on one of this slice's own entities (the bubble, a crate): true when it was taken. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!D1Tijdelijk.is(e)) {
            return false;
        }
        return Bubbelbrief.klik(p, e) || Bakken.klik(p, e);
    }

    private static void opInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer p && D1Tijdelijk.is(event.getTarget())) {
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                klik(p, event.getTarget());
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void opInteractSpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer p && D1Tijdelijk.is(event.getTarget())) {
            if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                klik(p, event.getTarget());
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void opAanval(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && D1Tijdelijk.is(event.getTarget())) {
            klik(p, event.getTarget());
            event.setCanceled(true);
        }
    }
}
