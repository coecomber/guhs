package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d1_start, step {@value SpVerhaal#R_BRIEF} (CONTRACT_E2_C 7.3): the bubble letter. A player whose
 * part 1 is at step 0 (the Guhvatar story done: the gate is open for them themselves) gets, in the Guheinde, after
 * {@value #WACHT} ticks in which they watch no scene and ride nothing (so never in the middle of the Guhvatar credits), a soap
 * bubble with a letter in it: it appears a few blocks ahead, drifts up to them and keeps floating in front of them until they
 * click it (right or left), for {@value #VOLGT} ticks; then it drifts off and comes back {@value #TERUG} ticks later. It is theirs alone ({@link D1Tijdelijk}: a display of the zeepbel block, the letter, a name
 * over it and a clickable box) and comes back on any later visit until popped. The pop plays the scene {@code brief}, then
 * the line goes on to step 1 (fly to Bikinibodem).
 */
public final class Bubbelbrief {
    /** Ticks of quiet in the Guheinde before the bubble comes (after a scene, a landing, a login). */
    static final int WACHT = 200;
    /** How far ahead (blocks) it appears, and how far in front of the eyes it stops. */
    static final double START = 9, STOP = 2.4;
    /** Blocks per tick it drifts. */
    static final double SNELHEID = 0.16;
    /** How long (ticks) a bubble keeps following; then it drifts off and comes back after {@value #TERUG} ticks (a reminder, never a pest). */
    static final int VOLGT = 20 * 75, TERUG = 20 * 180;

    private record Bel(Display.ItemDisplay bel, Display.ItemDisplay brief, Display.TextDisplay naam, Interaction knop, long sinds) {
        boolean weg() {
            return bel.isRemoved() || knop.isRemoved();
        }

        void ruimOp() {
            D1Tijdelijk.weg(bel);
            D1Tijdelijk.weg(brief);
            D1Tijdelijk.weg(naam);
            D1Tijdelijk.weg(knop);
        }
    }

    private static final Map<UUID, Bel> BELLEN = new ConcurrentHashMap<>();
    /** Player -> since when (server tick) they may get the bubble without a break. */
    private static final Map<UUID, Long> RUSTIG = new ConcurrentHashMap<>();

    private Bubbelbrief() {
    }

    /** May the bubble come to this player now (step 0, their own gate, in the Guheinde, watching and riding nothing)? */
    static boolean mag(ServerPlayer p) {
        return SpVerhaal.magBeginnen(p) && E2SpD1Start.stap(p) == SpVerhaal.R_BRIEF && GuheindeFeature.isGuheinde(p.level())
                && p.isAlive() && !p.isSpectator() && !Cutscenes.bezig(p) && p.getVehicle() == null;
    }

    /** Every few ticks for a player (E2SpD1StartFeature): the bubble comes, drifts, or goes. */
    static void tick(ServerPlayer p) {
        long nu = p.level().getServer().getTickCount();
        if (!mag(p)) {
            RUSTIG.remove(p.getUUID());
            weg(p);
            return;
        }
        long sinds = RUSTIG.computeIfAbsent(p.getUUID(), u -> nu);
        Bel b = BELLEN.get(p.getUUID());
        if (b != null && (b.weg() || b.bel.level() != p.level())) {
            weg(p);
            b = null;
        }
        if (b != null && nu - b.sinds > VOLGT) {
            weg(p);                                     // (it drifts off: back in a while, also when the player is busy elsewhere)
            RUSTIG.put(p.getUUID(), nu + TERUG - WACHT);
            p.sendOverlayMessage(E2SpD1Start.tekst("brief.weg").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (b == null) {
            if (nu - sinds < WACHT) {
                return;
            }
            b = maak(p);
            if (b == null) {
                return;
            }
            p.sendOverlayMessage(E2SpD1Start.tekst("brief.komt").withStyle(ChatFormatting.AQUA));
            if (E2SpD1Start.lijn().eenmalig(p, "sp_d1_start_brief_melding")) {
                E2SpD1Start.zie(p, "brief.melding");
            }
            p.level().playSound(null, p.blockPosition(), E2SponsguhFeature.BELLEN.get(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        }
        drijf(p, b, nu);
    }

    /** The spot in front of the player where the bubble wants to be (eye height, a little to the right). */
    static Vec3 doel(ServerPlayer p, double afstand) {
        Vec3 kijk = Vec3.directionFromRotation(0f, p.getYRot());
        Vec3 rechts = new Vec3(-kijk.z, 0, kijk.x);
        return p.getEyePosition().add(kijk.scale(afstand)).add(rechts.scale(0.35)).add(0, -0.15, 0);
    }

    @Nullable
    private static Bel maak(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 plek = doel(p, START).add(0, 1.5, 0);
        Display.ItemDisplay bel = D1Tijdelijk.item(level, plek, "guhs:zeepbel", 0.85f, false, "fixed");
        Display.ItemDisplay brief = D1Tijdelijk.item(level, plek.add(0, 0.05, 0), "minecraft:paper", 0.42f, false, "vertical");
        Display.TextDisplay naam = D1Tijdelijk.tekst(level, plek.add(0, 0.75, 0), E2SpD1Start.tekst("brief.naam").withStyle(ChatFormatting.AQUA),
                0.6f, 0x00000000, 200);
        Interaction knop = D1Tijdelijk.knop(level, plek.add(0, -0.5, 0), 1.1f, 1.1f);
        if (bel == null || brief == null || naam == null || knop == null) {
            return null;
        }
        Bel b = new Bel(D1Tijdelijk.zet(level, bel, p), D1Tijdelijk.zet(level, brief, p), D1Tijdelijk.zet(level, naam, p), D1Tijdelijk.zet(level, knop, p),
                level.getServer().getTickCount());
        BELLEN.put(p.getUUID(), b);
        return b;
    }

    /** Drifts the bubble towards its spot in front of the player, bobbing; far away (a jump, a flight) it comes closer at once. */
    private static void drijf(ServerPlayer p, Bel b, long nu) {
        Vec3 hier = b.bel.position();
        Vec3 doel = doel(p, STOP).add(0, Math.sin(nu * 0.12) * 0.12, 0);
        Vec3 weg = doel.subtract(hier);
        double d = weg.length();
        Vec3 nieuw;
        if (d > 24) {
            nieuw = doel(p, START);
        } else if (d > 0.05) {
            nieuw = hier.add(weg.scale(Math.min(1.0, SNELHEID * 5 / d)));     // (5 ticks per call)
        } else {
            nieuw = doel;
        }
        b.bel.setPos(nieuw);
        b.brief.setPos(nieuw.add(0, 0.05, 0));
        b.naam.setPos(nieuw.add(0, 0.75, 0));
        b.knop.setPos(nieuw.add(0, -0.5, 0));
        if (nu % 10 < 5) {
            E2SpD1Start.ver(p, ParticleTypes.BUBBLE_POP, nieuw, 2, 0.35, 0.35, 0.35, 0.01);
            E2SpD1Start.ver(p, ParticleTypes.END_ROD, nieuw, 1, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /** Is this entity part of this player's bubble? */
    static boolean isVan(ServerPlayer p, Entity e) {
        Bel b = BELLEN.get(p.getUUID());
        return b != null && (e == b.knop || e == b.bel || e == b.brief || e == b.naam);
    }

    /** A click on the bubble: pop. True when it was this player's bubble. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!isVan(p, e)) {
            return false;
        }
        pop(p);
        return true;
    }

    /** Pops the bubble (a click, the dev command): the scene brief, then step 1. */
    static void pop(ServerPlayer p) {
        if (E2SpD1Start.stap(p) != SpVerhaal.R_BRIEF || Cutscenes.bezig(p)) {
            return;
        }
        Bel b = BELLEN.get(p.getUUID());
        Vec3 plek = b != null ? b.bel.position() : doel(p, STOP);
        weg(p);
        ServerLevel level = p.level();
        level.playSound(null, BlockPos.containing(plek), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1.0f, 1.0f);
        E2SpD1Start.ver(p, ParticleTypes.BUBBLE_POP, plek, 30, 0.4, 0.4, 0.4, 0.05);
        E2SpD1Start.lijn().begin(p);
        if (!Cutscenes.speel(p, D1Scenes.BRIEF, p.blockPosition(), Rotation.NONE, Bubbelbrief::gelezen)) {
            gelezen(p);
        }
    }

    /** The letter is read: step 1 (only ever from step 0). */
    static void gelezen(ServerPlayer p) {
        if (E2SpD1Start.lijn().verder(p, SpVerhaal.R_BRIEF)) {
            RUSTIG.remove(p.getUUID());
        }
    }

    /** Takes this player's bubble away (it comes back while step 0 is on). */
    static void weg(ServerPlayer p) {
        Bel b = BELLEN.remove(p.getUUID());
        if (b != null) {
            b.ruimOp();
        }
    }

    /** (dev, tests) the bubble right now at the player, wherever they are. False: it could not be made. */
    static boolean zetNu(ServerPlayer p) {
        weg(p);
        return maak(p) != null;
    }

    /** (dev) the bubble now, without the wait. */
    static void nu(ServerPlayer p) {
        RUSTIG.put(p.getUUID(), (long) p.level().getServer().getTickCount() - WACHT);
    }

    /** (tests) does this player have a bubble now? */
    static boolean heeft(ServerPlayer p) {
        Bel b = BELLEN.get(p.getUUID());
        return b != null && !b.weg();
    }

    /** (tests) the click box of the player's bubble (null: none). */
    @Nullable
    static Interaction knop(ServerPlayer p) {
        Bel b = BELLEN.get(p.getUUID());
        return b == null ? null : b.knop;
    }

    /** (logout) forgets the player. */
    static void vergeet(UUID speler) {
        Bel b = BELLEN.remove(speler);
        if (b != null) {
            b.ruimOp();
        }
        RUSTIG.remove(speler);
    }
}
