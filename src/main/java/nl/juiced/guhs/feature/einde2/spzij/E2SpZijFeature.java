package nl.juiced.guhs.feature.einde2.spzij;

import java.util.function.Consumer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.10), slice sp_zij: the side lines of Bikinibodem and Octo's house.
 * <ul>
 *   <li>{@link SpZijLijnen}: the three lines (group {@code sponsguh}), open once the player works in town;</li>
 *   <li>{@link Boomkoepel} (Sandy-guh: karate, rocket parts, the rocket), {@link Guhlander} (the ghost ship, the scary story,
 *       the lanterns), {@link Helden} (Zeemeerman and Mosselguh: the belt, the invisible boatmobile), {@link OctoHuis}
 *       (atmosphere);</li>
 *   <li>{@link SpZijScenes} (five scenes), {@link SpZijHints} (a hint for every step), {@link SpZijCommando} (dev).</li>
 * </ul>
 * Rewards: the luchthelm and the heldencape (guh clothes of the Sponsguh set; their source here is {@value #KLEDINGBRON}) and
 * the spooklantaarn with its recipe. Texts: tools/features/einde2_sp_zij.py; English: tools/lang/en/c119_e2_sp_zij.json.
 */
public final class E2SpZijFeature {
    public static final String KLEDINGBRON = "einde2_sp_zij";

    private E2SpZijFeature() {
    }

    public static void register(IEventBus modBus) {
        SpZijLijnen.lijnen();
        SpZijScenes.register();
        SpZijHints.registreer();
        KledingBronnen.bron(GuhClothes.SPONSGUH_LUCHTHELM, KLEDINGBRON);
        KledingBronnen.bron(GuhClothes.SPONSGUH_HELDENCAPE, KLEDINGBRON);
        SponsguhWezen.BIJ_KLIK.add(E2SpZijFeature::klik);
        Bewoners.BIJ_MAKEN.add(Boomkoepel::markeer);
        for (Verhaallijn l : SpZijLijnen.lijnen()) {
            l.opStap((p, oud, nieuw) -> Zicht.kijkOpnieuw(p));
        }
        Praat.luister(Guhlander.DANK, (p, spreker, optie) -> {
            if (optie == -1) {
                Guhlander.omlaag(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(E2SpZijFeature::opTick);
        NeoForge.EVENT_BUS.addListener(Guhlander::serverTick);
        NeoForge.EVENT_BUS.addListener(Guhlander::opVal);
        NeoForge.EVENT_BUS.addListener(E2SpZijFeature::opUitloggen);
        NeoForge.EVENT_BUS.addListener(SpZijEigen::opJoin);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, PlayerInteractEvent.RightClickBlock.class, OctoHuis::opKlik);
        NeoForge.EVENT_BUS.addListener(SpZijCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /** {@code SponsguhWezen.BIJ_KLIK}: Sandy-guh, the ghost captain, Zeemeerman and Mosselguh (CONTRACT_E2_C 4.3); nobody else. */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        return Boomkoepel.klik(w, p) || Guhlander.klik(w, p) || Helden.klik(w, p);
    }

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        ServerLevel level = p.level();
        long nu = SpZij.nu(p);
        if ((nu + p.getId()) % 100 == 17 && !Cutscenes.bezig(p) && SpZijLijnen.beginWatOpenIs(p)) {
            SpZij.merk(p, SpZij.P + "open");
        }
        if (!SpZij.inStad(p)) {
            return;
        }
        if (SpZijLijnen.SANDY_LIJN.stap(p) == SpZijLijnen.S_KARATE) {
            Boomkoepel.karateTik(p, level, nu);
        }
        if ((nu + p.getId()) % 5 != 0) {
            return;
        }
        Boomkoepel.tick(p, level, nu);
        Guhlander.tick(p, level, nu);
        Helden.tick(p, level, nu);
        OctoHuis.tick(p, level, nu);
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            SpZijEigen.vergeet(p);
            Boomkoepel.vergeet(p.getUUID());
            Guhlander.vergeet(p.getUUID());
            Helden.vergeet(p.getUUID());
            OctoHuis.vergeet(p.getUUID());
        }
    }
}
