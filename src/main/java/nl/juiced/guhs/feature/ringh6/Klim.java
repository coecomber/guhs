package nl.juiced.guhs.feature.ringh6;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ringknipoog.Knipogen;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h6): the climb of the Frituurberg, per player (DESIGN_130 4, chapter 6). The steps of {@link RingH6Feature#LIJN}:
 * <pre>
 * 0  to the mountain        the narrator card when the chapter opens; reaching the base camp
 * 1  the Kronkelpad         walk up to the Eerste Richel, coals fall ({@link Kolen}); open cage 1
 * 2  the rope               two hooks up the west face (the Elfentouw); Smikagol grabs at the ring; open cage 2
 * 3  heavier                the narrow road, the last hook; open cage 3
 * 4  Sam-guh carries you    a click on Sam-guh: "ik kan de ring niet dragen, maar wel jou"; arriving at the Frituurspleet
 * 5  the Frituurspleet      walking onto the balcony: the finale ({@link Finale})
 * 6  the feast              at home in the Gouw ({@link Thuis})
 * </pre>
 * Rules that hold everywhere on the mountain, for everybody: nothing hurts (falls, fire and the frituur are caught: a long
 * fall or a dip in the frituur puts you back at your last Rustvuurtje), and only the player who is exactly at a step solves
 * it ({@link Ring#aanZet}): a friend who walks along opens no cage for somebody else.
 * <p>
 * The ring gets heavier with every block you climb ({@link Ring#zetZwaarte}): light at the camp, as heavy as it gets on the
 * Derde Richel. Smikagol (the guide of chapter 5) interferes on the steps 2 and 3: now and then he sneaks up and grabs at the
 * ring: a warning line, a second to step away, else a nudge (a shove, never more). A flash of the Lichtflesje sends him
 * cowering for a good while.
 */
public final class Klim {
    /** The narrator card of the chapter (tools/features/ring_h6.py). */
    public static final String KAART = "ring_h6";
    /** The ring's weight at the camp, and how much it grows up to the Derde Richel. */
    public static final double ZWAAR_VOET = 0.2, ZWAAR_KLIM = 0.8;
    /** A fall of more than this many blocks on the mountain puts a climber back at their rest point. */
    public static final double VAL = 7.5;
    /** Smikagol grabs every GRAAI_RUST (+ up to as much again) ticks, warns GRAAI_TEL ticks before, reaches GRAAI_BEREIK blocks. */
    public static final int GRAAI_RUST = 500, GRAAI_TEL = 40, VERBLIND = 1200;
    public static final double GRAAI_BEREIK = 3.2, GRAAI_DUW = 0.6;
    public static final String FRITUUR = "ringh6_frituur";

    /** What Smikagol is up to with one player. */
    private record Graai(long volgende, long grijpOp, long blindTot) {
    }

    private static final Map<UUID, Graai> GRAAIEN = new ConcurrentHashMap<>();
    /** How far each climber who is in the air right now has fallen (the most their fall distance has been). */
    private static final Map<UUID, Double> VALT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RIT = new ConcurrentHashMap<>();

    private static Verhaallijn lijn() {
        return RingH6Feature.LIJN;
    }

    /** Is the story in this level (the Barbecuether; in tests: wherever)? */
    static boolean opDeBergWereld(ServerLevel level) {
        return Ring.OVERAL || level.dimension() == BarbecuetherFeature.BARBECUETHER;
    }

    // =====================================================================================================================
    // once a second
    // =====================================================================================================================

    /** (once a second, every player) the chapter's upkeep for this player. */
    static void seconde(ServerPlayer p) {
        Verhaallijn lijn = lijn();
        ServerLevel level = p.level();
        Berg.Kopie berg = opDeBergWereld(level) ? Berg.zoek(p) : null;
        if (berg == null) {
            VALT.remove(p.getUUID());
        }
        if (lijn.klaar(p) || !lijn.aanDeBeurt(p) || p.isSpectator() || !p.isAlive()) {
            return;
        }
        int stap = lijn.stap(p);
        if (stap >= 6) {
            Thuis.seconde(p);
            return;
        }
        if (!opDeBergWereld(level)) {
            return;
        }
        // the chapter opens: the narrator card with the map (once)
        if (!Verteller.gezien(p, KAART)) {
            if (Duwtje.mag(p)) {
                lijn.begin(p);
                Verteller.toon(p, KAART, null);
            }
            return;
        }
        if (stap == 5 && lijn.vlag(p, Finale.GEFRITUURD)) {
            Finale.vlucht(p, berg);                           // (the ring is fried, the flight home did not happen yet)
            return;
        }
        if (berg == null) {
            naderen(p, stap);
            return;
        }
        zwaarte(p, berg, stap);
        switch (stap) {
            case 0 -> {
                if (bij(p, berg, "kamp_vuur", 9)) {
                    aankomst(p, berg);
                }
            }
            case 1, 2, 3 -> {
                Kolen.seconde(p, berg, stap);
                if (stap >= 2) {
                    smikagol(p, berg);
                }
                hints(p, berg, stap);
            }
            case 4 -> {
                Kolen.seconde(p, berg, stap);
                rit(p, berg);
                if (bij(p, berg, "spleet", 3.5) && !p.isPassenger()) {
                    boven(p);
                } else if (!p.isPassenger() && p.tickCount % 600 < 20) {
                    GuhQuests.hint(p, "quest.guhs.ringh6.sam.hint");
                }
            }
            case 5 -> {
                if (bij(p, berg, "rand", 3.2)) {
                    Finale.frituur(p, berg);
                } else if (p.tickCount % 600 < 20) {
                    GuhQuests.hint(p, "quest.guhs.ringh6.rand.hint");
                }
            }
            default -> {
            }
        }
    }

    private static boolean bij(ServerPlayer p, Berg.Kopie berg, String plek, double afstand) {
        Vec3 daar = berg.midden(plek);
        return Math.abs(daar.y - p.getY()) <= 3.0 && daar.distanceToSqr(p.position()) <= afstand * afstand;
    }

    /** The ring's weight: it grows with the height (a carried or roped player feels nothing: ring-kern). */
    private static void zwaarte(ServerPlayer p, Berg.Kopie berg, int stap) {
        if (!Ring.heeft(p)) {
            return;
        }
        double f = stap == 0 ? ZWAAR_VOET * 0.75 : stap >= 4 ? 1.0 : ZWAAR_VOET + ZWAAR_KLIM * berg.hoogte(p.getY());
        Ring.zetZwaarte(p, f, 45);
        if (f >= 0.55 && stap < 4 && lijn().eenmalig(p, "zwaar_half")) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.zwaar.half").withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * On the way to the mountain (not on it yet): the ring is only a little heavy, a little more with every step nearer.
     * Without this ring-kern's own rule would make it as heavy as it gets at the foot, and there would be nothing left for
     * the climb to add ("slower and slower").
     */
    private static void naderen(ServerPlayer p, int stap) {
        if (stap > 4 || !Ring.heeft(p)) {
            return;
        }
        double dichtst = Double.MAX_VALUE;
        for (Sluiers.Zone berg : Sluiers.zones(p.level(), Berg.STRUCTUUR)) {
            double dx = Math.max(Math.max(berg.x0() - p.getX(), 0), p.getX() - (berg.x1() + 1));
            double dz = Math.max(Math.max(berg.z0() - p.getZ(), 0), p.getZ() - (berg.z1() + 1));
            dichtst = Math.min(dichtst, Math.sqrt(dx * dx + dz * dz));
        }
        if (dichtst < Ring.ZWAAR_AFSTAND) {
            Ring.zetZwaarte(p, ZWAAR_VOET * 0.75 * (1 - dichtst / Ring.ZWAAR_AFSTAND), 45);
        }
    }

    /** Step 0 -> 1: the base camp. Sam-guh and Smikagol say their piece; from here it goes up. */
    static void aankomst(ServerPlayer p, Berg.Kopie berg) {
        if (!lijn().verder(p, 0)) {
            return;
        }
        Ring.behaald(p, "ring_h6_berg");
        Sam.roep(p);
        if (Sam.van(p) != null) {
            GuhQuests.say(p, Sam.van(p), "quest.guhs.ringh6.kamp.sam");
        }
        SmikagolEntity smikagol = Smikagol.roep(p, null);
        if (smikagol != null) {
            GuhQuests.say(p, smikagol, "quest.guhs.ringh6.kamp.smikagol");
        }
        GuhQuests.hint(p, "quest.guhs.ringh6.kamp.hint");
    }

    /** Now and then: what to do here (the rope, when the road ends). */
    private static void hints(ServerPlayer p, Berg.Kopie berg, int stap) {
        if (stap >= 2 && !Gaven.heeft(p, RingFeature.ELFENTOUW.get())) {
            // (a story set by command, or a rope thrown away: Sam-guh had a spare one in his pack)
            if (Gaven.geefAlsKwijt(p, RingFeature.ELFENTOUW.get())) {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.touw.geen").withStyle(ChatFormatting.GOLD));
            }
        }
        String start = stap == 2 ? (p.getY() < berg.wereld("boven_1").getY() - 1 ? "start_1" : "start_2") : stap == 3 ? "start_3" : null;
        if (start != null && bij(p, berg, start, 4) && p.tickCount % 300 < 20) {
            GuhQuests.hint(p, "quest.guhs.ringh6.touw.hint");
            if (lijn().eenmalig(p, "touw_sam") && Sam.van(p) != null) {
                GuhQuests.say(p, Sam.van(p), "quest.guhs.ringh6.touw.sam");
            }
        }
    }

    // =====================================================================================================================
    // the cages
    // =====================================================================================================================

    /** A click on the lock of cage {@code nr}. */
    static void slot(ServerPlayer p, BlockPos pos, int nr) {
        Verhaallijn lijn = lijn();
        ServerLevel level = p.level();
        if (lijn.klaar(p) || (lijn.aanDeBeurt(p) && lijn.stap(p) > nr)) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.slot.al_open").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!lijn.aanDeBeurt(p)) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.slot.niet_van_jou").withStyle(ChatFormatting.GRAY));
            return;
        }
        Berg.Kopie berg = Berg.zoek(p);
        if (lijn.stap(p) == 0 && nr == 1 && berg != null) {
            aankomst(p, berg);                                // (somebody who walked round the camp)
        }
        if (!Ring.aanZet(p, lijn, nr)) {
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 0.7f);
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.slot.eerst").withStyle(ChatFormatting.YELLOW));
            return;
        }
        level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.3f);
        level.sendParticles(p, ParticleTypes.CRIT, false, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.1);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.slot.open").withStyle(ChatFormatting.GREEN));
        // their own Rookguhje wriggles out and flies off; the caged one is gone for them the moment the step moves on
        Vec3 kooi = berg != null ? berg.midden("kooi_" + nr) : Vec3.atBottomCenterOf(pos.below());
        GekooideRookguhEntity vrij = GekooideRookguhEntity.vrij(p, kooi, nr);
        lijn.verder(p, nr);
        if (vrij != null) {
            level.playSound(null, vrij, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.0f, 1.5f);
            GuhQuests.say(p, vrij, "quest.guhs.ringh6.rookguh.vrij." + nr);
        }
        GuhEntity sam = Sam.van(p);
        if (nr == 3) {
            Ring.behaald(p, "ring_h6_rookguhs");
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.zwaar.vol").withStyle(ChatFormatting.GOLD));
            if (sam == null) {
                Sam.roep(p);
                sam = Sam.van(p);
            }
            if (sam != null) {
                Sam.kom(p);
                GuhQuests.say(p, sam, "quest.guhs.ringh6.sam.draag");
            }
            GuhQuests.hint(p, "quest.guhs.ringh6.sam.hint");
        } else if (sam != null) {
            GuhQuests.say(p, sam, "quest.guhs.ringh6.rookguh.sam");
        }
    }

    // =====================================================================================================================
    // Smikagol interferes
    // =====================================================================================================================

    private static void smikagol(ServerPlayer p, Berg.Kopie berg) {
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        SmikagolEntity s = Smikagol.van(p);
        if (s == null) {
            s = Smikagol.roep(p, null);
        }
        Graai g = GRAAIEN.get(p.getUUID());
        if (g == null) {
            GRAAIEN.put(p.getUUID(), new Graai(nu + GRAAI_RUST / 2 + p.getRandom().nextInt(GRAAI_RUST), 0, 0));
            return;
        }
        if (s == null || nu < g.blindTot() || g.grijpOp() > 0) {
            return;                                           // (a grab that is under way is finished by tik)
        }
        if (nu < g.volgende() || !Ring.heeft(p) || !Kolen.opDeFlank(p, berg) || !Duwtje.mag(p) || !p.onGround()) {
            return;
        }
        // he sneaks up and says what he is about to do: a second or two to step away, or to flash him
        if (s.distanceToSqr(p) > 36) {
            Smikagol.kom(p);
        }
        Smikagol.zeg(p, "quest.guhs.ringh6.smikagol.loer." + p.getRandom().nextInt(3));
        level.playSound(null, s, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 1.0f, 1.8f);
        GRAAIEN.put(p.getUUID(), new Graai(nu + GRAAI_RUST + p.getRandom().nextInt(GRAAI_RUST), nu + GRAAI_TEL, 0));
    }

    /** (every tick) the grab itself, GRAAI_TEL ticks after the warning. */
    private static void graaiTik(ServerPlayer p) {
        Graai g = GRAAIEN.get(p.getUUID());
        if (g == null || g.grijpOp() == 0 || p.level().getGameTime() < g.grijpOp()) {
            return;
        }
        GRAAIEN.put(p.getUUID(), new Graai(g.volgende(), 0, g.blindTot()));
        SmikagolEntity s = Smikagol.van(p);
        if (s == null || p.level().getGameTime() < g.blindTot() || !Ring.heeft(p)) {
            return;
        }
        Smikagol.grijp(p);
        if (s.distanceToSqr(p) <= GRAAI_BEREIK * GRAAI_BEREIK && Duwtje.mag(p) && !p.isPassenger() && !Gaven.klimt(p)) {
            Duwtje.duw(p, p.position().subtract(s.position()), GRAAI_DUW);
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.smikagol.duw").withStyle(ChatFormatting.GOLD));
            if (lijn().eenmalig(p, "smikagol_sam") && Sam.van(p) != null) {
                GuhQuests.say(p, Sam.van(p), "quest.guhs.ringh6.smikagol.sam");
            }
        } else {
            Smikagol.zeg(p, "quest.guhs.ringh6.smikagol.mis");
        }
    }

    /** (Gaven.BIJ_LICHT) a flash of the Lichtflesje near his own Smikagol on the mountain: he keeps his paws to himself. */
    static void licht(ServerPlayer p, Vec3 plek) {
        SmikagolEntity s = Smikagol.van(p);
        if (s == null || Berg.van(p) == null || !Ring.aanZet(p, lijn(), lijn().stap(p)) || lijn().stap(p) < 1 || lijn().stap(p) > 5
                || s.position().distanceToSqr(plek) > Gaven.FLITS_STRAAL * Gaven.FLITS_STRAAL) {
            return;
        }
        long nu = p.level().getGameTime();
        Graai g = GRAAIEN.get(p.getUUID());
        boolean loerde = g != null && g.grijpOp() > 0;
        GRAAIEN.put(p.getUUID(), new Graai(nu + VERBLIND + GRAAI_RUST, 0, nu + VERBLIND));
        Smikagol.zeg(p, "quest.guhs.ringh6.smikagol.verblind");
        p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, s.getX(), s.getY() + 0.5, s.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
        if (loerde || lijn().stap(p) >= 2) {
            Ring.behaald(p, "ring_h6_flits");
        }
    }

    /** Is Smikagol blinded for this player right now? */
    public static boolean verblind(ServerPlayer p) {
        Graai g = GRAAIEN.get(p.getUUID());
        return g != null && p.level().getGameTime() < g.blindTot();
    }

    // =====================================================================================================================
    // Sam-guh carries
    // =====================================================================================================================

    /**
     * (Sam.BIJ_KLIK) a click on the player's own Sam-guh on the mountain. Step 4: he carries (the famous line is ring-kern's
     * quest.guhs.ring.sam.draag). On the other steps the heavy ring must not send him off towards "the next goal" (the
     * middle of the mountain): he says why he can't carry now.
     */
    static InteractionResult samKlik(GuhEntity sam, ServerPlayer p, InteractionHand hand) {
        if (!Sam.isSam(sam) || Sam.van(p) != sam) {
            return InteractionResult.PASS;
        }
        Berg.Kopie berg = Berg.van(p);
        Verhaallijn lijn = lijn();
        if (berg == null || lijn.klaar(p) || !lijn.aanDeBeurt(p) || (Ring.kreeg(p) && !Ring.heeft(p)) || p.getVehicle() == sam) {
            return InteractionResult.PASS;                    // (not here, or ring-kern's own answer: the ring back, getting off)
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        int stap = lijn.stap(p);
        if (stap == 4) {
            // (ring-knipogen: the first time Boris the goose has something to say first; Sam-guh carries right after it)
            return Knipogen.boris(p, berg.wereld("richel_3"), berg.draai(), q -> draag(q, berg)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (Ring.zwaarte(p) < Sam.DRAAG_VANAF) {
            return InteractionResult.PASS;                    // (light enough: his usual small talk)
        }
        sam.playSound(ModSounds.GUH_AMBIENT.get(), 1f, sam.getVoicePitch());
        GuhQuests.say(p, sam, stap == 5 ? "quest.guhs.ringh6.sam.boven" : stap >= 2 ? "quest.guhs.ringh6.touw.sam" : "quest.guhs.ringh6.sam.nog_niet");
        return InteractionResult.SUCCESS;
    }

    /** Sam-guh takes his player on his back and walks the last road, through the cleft, to the balcony. */
    static boolean draag(ServerPlayer p, Berg.Kopie berg) {
        List<Vec3> route = berg.route("sam");
        if (route.isEmpty()) {
            return false;
        }
        // from the point of the route nearest to where they stand now (somebody may have walked a part)
        int dichtst = 0;
        for (int i = 1; i < route.size(); i++) {
            if (route.get(i).distanceToSqr(p.position()) < route.get(dichtst).distanceToSqr(p.position())) {
                dichtst = i;
            }
        }
        List<Vec3> rest = new ArrayList<>(route.subList(Math.min(dichtst + 1, route.size() - 1), route.size()));
        if (!Sam.draag(p, rest, Klim::boven)) {
            return false;
        }
        RIT.put(p.getUUID(), p.level().getGameTime());
        return true;
    }

    /**
     * (once a second, step 4) Sam-guh on his way with his player: when he makes no headway for a while (a mob's path finder
     * on a narrow mountain road), he takes a little run-up: a hop to the next point of his route.
     */
    private static void rit(ServerPlayer p, Berg.Kopie berg) {
        GuhEntity sam = Sam.van(p);
        if (sam == null || p.getVehicle() != sam) {
            RIT.remove(p.getUUID());
            return;
        }
        List<Vec3> route = berg.route("sam");
        int dichtst = 0;
        for (int i = 1; i < route.size(); i++) {
            if (route.get(i).distanceToSqr(sam.position()) < route.get(dichtst).distanceToSqr(sam.position())) {
                dichtst = i;
            }
        }
        long nu = p.level().getGameTime();
        String key = "guhs_ringh6_rit";
        int vorige = sam.getPersistentData().getIntOr(key, -1);
        if (dichtst != vorige) {
            sam.getPersistentData().putInt(key, dichtst);
            RIT.put(p.getUUID(), nu);
            return;
        }
        if (nu - RIT.getOrDefault(p.getUUID(), nu) >= 120 && dichtst + 1 < route.size()) {
            Vec3 naar = route.get(dichtst + 1);
            sam.teleportTo(naar.x, naar.y, naar.z);
            sam.getNavigation().stop();
            p.level().sendParticles(p, ParticleTypes.POOF, false, false, naar.x, naar.y + 0.4, naar.z, 6, 0.3, 0.2, 0.3, 0.01);
            RIT.put(p.getUUID(), nu);
        }
    }

    /** Step 4 -> 5: at the Frituurspleet (carried there, or walked the hard way). */
    static void boven(ServerPlayer p) {
        if (!lijn().verder(p, 4)) {
            return;
        }
        RIT.remove(p.getUUID());
        if (p.isPassenger() && Sam.isSam(p.getVehicle())) {
            p.stopRiding();
        }
        if (Sam.van(p) != null) {
            GuhQuests.say(p, Sam.van(p), "quest.guhs.ringh6.sam.boven");
        }
        GuhQuests.hint(p, "quest.guhs.ringh6.rand.hint");
    }

    // =====================================================================================================================
    // every tick: nothing on the mountain hurts
    // =====================================================================================================================

    /** (every tick, a player on a Frituurberg) no fire, the frituur and a long fall put back at the rest point, Smikagol's grab. */
    static void tik(ServerPlayer p) {
        Berg.Kopie berg = Berg.van(p);
        if (berg == null) {
            return;
        }
        if (p.getRemainingFireTicks() > 0) {
            p.clearFire();
        }
        if (p.isSpectator() || p.isCreative()) {
            return;
        }
        graaiTik(p);
        if (p.isInFluidType(BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get())) {
            red(p, berg, FRITUUR);
            return;
        }
        // a long fall (shoved off a ledge, a missed jump): nothing broken, back at the rest point
        if (p.onGround() || p.isPassenger() || Gaven.klimt(p)) {
            Double viel = VALT.remove(p.getUUID());
            if (viel != null && p.onGround() && viel > VAL && Ring.aanZet(p, lijn(), lijn().stap(p))) {
                red(p, berg, Ring.GEVALLEN);
            }
        } else if (p.fallDistance > 0) {
            VALT.merge(p.getUUID(), (double) p.fallDistance, Math::max);
        }
    }

    /** Back at the rest point (when it is on this mountain; else at the base camp), unharmed, with a line about why. */
    static void red(ServerPlayer p, Berg.Kopie berg, String reden) {
        if (!Duwtje.mag(p)) {
            return;
        }
        p.clearFire();
        p.fallDistance = 0;
        VALT.remove(p.getUUID());
        Rustpunten.Punt punt = Ring.rustpunt(p);
        if (Ring.opReis(p) && punt != null && punt.dim() == p.level().dimension() && berg.binnen(punt.plek(), 4)) {
            Ring.terugNaarRustpunt(p, reden, null);
            return;
        }
        // (a visitor, or a climber without a rest point here)
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Duwtje.terug(p, p.level().dimension(), berg.midden("kamp"), p.getYRot());
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.terug." + reden).withStyle(ChatFormatting.LIGHT_PURPLE));
        Sam.kom(p);
        Smikagol.kom(p);
    }

    static void vergeet(UUID speler) {
        GRAAIEN.remove(speler);
        VALT.remove(speler);
        RIT.remove(speler);
    }

    static void wisAlles() {
        GRAAIEN.clear();
        VALT.clear();
        RIT.clear();
    }

    /** (tests) Smikagol grabs at the next look / the grab lands on the next tick. */
    static void zetGraaiNu(ServerPlayer p, boolean meteen) {
        long nu = p.level().getGameTime();
        GRAAIEN.put(p.getUUID(), new Graai(nu, meteen ? nu : 0, 0));
    }

    private Klim() {
    }
}
