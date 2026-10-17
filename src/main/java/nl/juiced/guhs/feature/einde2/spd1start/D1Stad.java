package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.Leegtemist;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh", slice sp_d1_start, steps 1-3 (CONTRACT_E2_C 2.2) and the clicks on the cast while this slice's steps are on
 * (4.3: before part 1 step 3, and during its own steps 3 and 4).
 * <ul>
 *   <li>{@link SpVerhaal#R_REIS}: landing on the island above the dome (Appa set the rider down: {@code E2Reizen.landtNog}
 *       false), or walking in any other way: the narrator card {@value E2SpD1Start#KAART}, then the scene {@code aankomst}
 *       (on the island only), then step 2;</li>
 *   <li>{@link SpVerhaal#R_BUBBEL}: standing on the plein at the foot of the bubbelstroom: scene {@code welkom}, then step 3;</li>
 *   <li>{@link SpVerhaal#R_SOLLICITATIE}: a click on Meneer Krabguh in Bikinibodem: scene {@code sollicitatie}, the Krokante
 *       Knabbel cap, then step 4. Inside the restaurant a column of light stands over his spot.</li>
 * </ul>
 * Small talk: every character of the town answers a click with a line of its own that fits the moment (looking for the job,
 * or baking), in the chat. The side-line characters (Sandy-guh, Zeemeerman, Mosselguh, the ghost captain, Octo at home) are
 * sp_zij's from step 3 on: this slice leaves them alone then.
 */
final class D1Stad {
    /** How close (blocks, flat) to aankomst_speler counts as "on the island", and to bubbel_onder as "at the foot of the stream". */
    static final double EILAND = 10, VOET = 7;
    /** The characters whose own small talk this slice has (the rest keeps its phase-A line). */
    static final Set<Cast> PRAAT = Set.of(Cast.SPONSGUH, Cast.PATGUH, Cast.GERRIT, Cast.MENEER_KRABGUH, Cast.PAREL_GUH, Cast.OCTO_MIKA,
            Cast.OCTOLIAM, Cast.PLANKMIKA, Cast.JUF_PUFGUH, Cast.BIKINIBODEM_GUH, Cast.SANDY_GUH);
    /** The ones with a line of their own while you bake. */
    static final Set<Cast> PRAAT_BAKKEN = Set.of(Cast.SPONSGUH, Cast.PATGUH, Cast.MENEER_KRABGUH, Cast.PAREL_GUH, Cast.OCTO_MIKA);
    /** sp_zij's characters (CONTRACT_E2_C 4.3). */
    static final Set<Cast> ZIJ = Set.of(Cast.SANDY_GUH, Cast.ZEEMEERMAN_GUH, Cast.MOSSELGUH, Cast.SPOOKKAPITEIN);
    /** How many lines a Bikinibodemguh has. */
    static final int GUH_REGELS = 3;

    /** Player -> when (server tick) a scene of this class was last started for them (no double start). */
    private static final Map<UUID, Long> GESTART = new ConcurrentHashMap<>();

    private D1Stad() {
    }

    private static boolean pas(ServerPlayer p) {
        long nu = p.level().getServer().getTickCount();
        Long t = GESTART.get(p.getUUID());
        if (t != null && nu - t < 60) {
            return false;
        }
        GESTART.put(p.getUUID(), nu);
        return true;
    }

    // =====================================================================================================================
    // the step ticks (every few ticks, for a player in Bikinibodem)
    // =====================================================================================================================

    static void tickReis(ServerPlayer p) {
        if (Cutscenes.bezig(p) || E2Reizen.landtNog(p) || p.getVehicle() != null) {
            return;
        }
        ServerLevel g = p.level();
        Vec3 eiland = E2SpD1Start.midden(g, "aankomst_speler");
        if (eiland == null) {
            return;
        }
        double dx = p.getX() - eiland.x, dz = p.getZ() - eiland.z, dy = p.getY() - eiland.y;
        if (dx * dx + dz * dz <= EILAND * EILAND && dy > -3 && dy < 8) {
            aangekomen(p, BlockPos.containing(eiland));
        } else if (Leegtemist.inKoepel(g, p.blockPosition())) {
            aangekomen(p, null);            // (walked in through the gate: the card, then straight on to the stream's step)
        }
    }

    /** Arrived (step 1): the narrator card, then the scene on the island (anchor null: no scene), then step 2. */
    static void aangekomen(ServerPlayer p, @javax.annotation.Nullable BlockPos eiland) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_REIS || Cutscenes.bezig(p) || !pas(p)) {
            return;
        }
        if (!Verteller.toon(p, E2SpD1Start.KAART, a -> aankomstScene(a, eiland))) {
            aankomstScene(p, eiland);
        }
    }

    private static void aankomstScene(ServerPlayer p, @javax.annotation.Nullable BlockPos eiland) {
        if (eiland == null || !Cutscenes.speel(p, D1Scenes.AANKOMST, eiland, Rotation.NONE, s -> E2SpD1Start.lijn().verder(s, SpVerhaal.R_REIS))) {
            E2SpD1Start.lijn().verder(p, SpVerhaal.R_REIS);
        }
    }

    static void tickBubbel(ServerPlayer p) {
        if (Cutscenes.bezig(p) || !p.onGround()) {
            return;
        }
        ServerLevel g = p.level();
        Vec3 voet = E2SpD1Start.midden(g, "bubbel_onder");
        AABB plein = E2SpD1Start.zone(g, "plein");
        if (voet == null) {
            return;
        }
        double dx = p.getX() - voet.x, dz = p.getZ() - voet.z, dy = p.getY() - voet.y;
        boolean bijVoet = dx * dx + dz * dz <= VOET * VOET && Math.abs(dy) < 2.5;
        if (bijVoet || plein != null && plein.contains(p.position()) && Math.abs(dy) < 2.5) {
            welkom(p, BlockPos.containing(voet));
        }
    }

    /** At the foot of the stream (step 2): the scene, then step 3. */
    static void welkom(ServerPlayer p, BlockPos voet) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_BUBBEL || Cutscenes.bezig(p) || !pas(p)) {
            return;
        }
        if (!Cutscenes.speel(p, D1Scenes.WELKOM, voet, Rotation.NONE, s -> E2SpD1Start.lijn().verder(s, SpVerhaal.R_BUBBEL))) {
            E2SpD1Start.lijn().verder(p, SpVerhaal.R_BUBBEL);
        }
    }

    /** Step 3: a column of light over Krabguh's spot inside the restaurant, and a nudge in the office. */
    static void tickSollicitatie(ServerPlayer p) {
        ServerLevel g = p.level();
        Vec3 krabguh = E2SpD1Start.midden(g, "krabguh");
        AABB zaak = E2SpD1Start.zone(g, "krokante_knabbel");
        if (krabguh == null || zaak == null || !zaak.inflate(1).contains(p.position()) || Cutscenes.bezig(p)) {
            return;
        }
        long nu = g.getServer().getTickCount();
        if (nu % 10 < 5) {
            E2SpD1Start.ver(p, ParticleTypes.END_ROD, krabguh.add(0, 2.4, 0), 3, 0.05, 0.6, 0.05, 0.0);
        }
        if (p.position().distanceTo(krabguh) < 6 && nu % 100 < 5) {
            p.sendOverlayMessage(E2SpD1Start.tekst("sollicitatie.klik").withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================================================================
    // clicks on the cast (SponsguhWezen.BIJ_KLIK)
    // =====================================================================================================================

    /** The click listener: true = this slice answered. */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        if (!SpVerhaal.open(p) || !SpVerhaal.actief(p, SpVerhaal.RECEPT)) {
            return false;
        }
        int s = E2SpD1Start.stap(p);
        if (s > SpVerhaal.R_BAKKEN) {
            return false;
        }
        Cast c = w.cast();
        if (s >= SpVerhaal.R_SOLLICITATIE && (ZIJ.contains(c) || c == Cast.OCTO_MIKA && thuis(w))) {
            return false;                                   // (sp_zij's from step 3 on)
        }
        if (c == Cast.MENEER_KRABGUH && s == SpVerhaal.R_SOLLICITATIE && E2SpD1Start.inBikinibodem(p)) {
            w.getLookControl().setLookAt(p);
            sollicitatie(p);
            return true;
        }
        String key = praat(c, s, w);
        if (key == null) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        w.zetAnimatie("praat");
        GuhQuests.say(p, w, key);
        return true;
    }

    /** Is this the resident Octo of his own house (not the one at the till)? */
    static boolean thuis(SponsguhWezen w) {
        return w.getPersistentData().getStringOr(Bewoners.TAG, "").contains("bikinibodem_octo@");
    }

    /** The lang key of a character's small talk at this step (null: none of this slice). */
    @javax.annotation.Nullable
    static String praat(Cast c, int stap, SponsguhWezen w) {
        if (!PRAAT.contains(c)) {
            return null;
        }
        String groep = stap == SpVerhaal.R_BAKKEN && PRAAT_BAKKEN.contains(c) ? "bakken" : "voor";
        String key = E2SpD1Start.P + "praat." + groep + "." + c.id();
        if (c == Cast.BIKINIBODEM_GUH) {
            key += "." + Math.floorMod(w.getUUID().hashCode(), GUH_REGELS);
        }
        return key;
    }

    /** Step 3: the interview (the scene at Krabguh's spot), then the cap and step 4. */
    static void sollicitatie(ServerPlayer p) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_SOLLICITATIE || Cutscenes.bezig(p) || !pas(p)) {
            return;
        }
        BlockPos anker = E2SpD1Start.plek(p.level().getServer() == null ? null : E2SpD1Start.guheinde(p.level().getServer()), "krabguh");
        if (anker == null || !Cutscenes.speel(p, D1Scenes.SOLLICITATIE, anker, Rotation.NONE, D1Stad::aangenomen)) {
            aangenomen(p);
        }
    }

    /** Hired: the cap (once), then step 4. */
    static void aangenomen(ServerPlayer p) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_SOLLICITATIE) {
            return;
        }
        E2SpD1Start.geefPetje(p);
        E2SpD1Start.lijn().verder(p, SpVerhaal.R_SOLLICITATIE);
    }

    static void vergeet(UUID speler) {
        GESTART.remove(speler);
    }
}
