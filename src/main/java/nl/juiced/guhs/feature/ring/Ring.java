package nl.juiced.guhs.feature.ring;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ringh1.RingH1Feature;
import nl.juiced.guhs.feature.ringh2.RingH2Feature;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;
import nl.juiced.guhs.feature.ringh4.RingH4Feature;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.ringh6.RingH6Feature;
import nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Doelen;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-kern): "In de ban van de Knabbelring" - the one place the seven chapter slices (ring-h1 .. ring-h6,
 * ring-sausuman) ask about the story as a whole, the ring and the trip. Everything is per player. Manual with examples:
 * guhs_work130/reports/slice_ring-kern.md.
 * <ul>
 *   <li><b>The story</b>: {@link #lijn(int)} (the questline of a chapter: {@code RingH<n>Feature.LIJN}, which the chapter slice
 *       fills in place), {@link #hoofdstuk}, {@link #opReis}, {@link #klaar}, {@link #aanZet} (the "walk along with a friend"
 *       rule: only the player who is exactly at a step solves its puzzle) and {@link #sluier} (the spoiler wall + protection
 *       of a chapter structure: open for whoever reached that chapter).</li>
 *   <li><b>The ring</b>: {@link #geef} / {@link #neem} / {@link #heeft}, wearing it ({@link #om}, {@link #doeOm}: invisible to
 *       Mika's, but the Eye sees you and the Nine come: {@link Negen}), its weight near the mountain ({@link #zetZwaarte},
 *       {@link #zwaarte}).</li>
 *   <li><b>The trip</b>: the last rest point ({@link #rustpunt}, {@link #terugNaarRustpunt}: where the Eye and the Nine put
 *       a player back), where to go ({@link #doel}, {@link #vertelDoel}: what Sam-guh and Guhdalf say).</li>
 * </ul>
 */
public final class Ring {
    /** The group of the questlines (the heading of the Guhdex tab Verhalen) and the id of the travel map. */
    public static final String GROEP = "knabbelring", REISKAART = "knabbelring";
    /** The kind of rest point ({@code Rustpunten}) of this story. */
    public static final String RUST = "ring";
    /** The structures of the chapters 1..6 (index hoofdstuk - 1) and of the extra stop. */
    public static final List<String> STRUCTUREN = List.of("knabbelgouw", "guhvendel", "knabbelmoria", "guhladriel_boomstad", "zwarte_roosterpoort", "frituurberg");
    public static final String SAUSUMAN = "sausuman_toren";
    /** The extra stop counts as "chapter 7" in {@link #lijn(int)} and {@link #sluier}. */
    public static final int SAUSUMAN_NR = 7;
    /** Why a player was put back on their rest point (the message quest.guhs.ring.terug.&lt;reden&gt;). */
    public static final String NEGEN = "negen", OOG = "oog", GEVALLEN = "gevallen";

    static final String OM_TEKST = "quest.guhs.ring.om", AF_TEKST = "quest.guhs.ring.af";
    private static final String GEGEVEN = "guhs_ring_gegeven";
    private static final Identifier ZWAAR = Guhs.id("ring_zwaar");
    /** How much slower the heaviest ring makes its bearer (x the walking speed). */
    public static final double ZWAAR_MAX = 0.6;
    /** The ring gets heavier from this many blocks of the Frituurberg on (its sluier box), heaviest at its foot. */
    public static final int ZWAAR_AFSTAND = 140;

    /** Wearing the ring: player -> the game time they put it on. */
    private static final Map<UUID, Long> OM = new ConcurrentHashMap<>();

    private record Zwaarte(double factor, long tot) {
    }

    /** A weight a chapter set by hand: player -> factor until a game time. */
    private static final Map<UUID, Zwaarte> ZWAARTES = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // the story
    // =====================================================================================================================

    /** The questline of chapter 1..6, or of the Toren van Sausuman ({@link #SAUSUMAN_NR}). */
    public static Verhaallijn lijn(int hoofdstuk) {
        return switch (hoofdstuk) {
            case 1 -> RingH1Feature.LIJN;
            case 2 -> RingH2Feature.LIJN;
            case 3 -> RingH3Feature.LIJN;
            case 4 -> RingH4Feature.LIJN;
            case 5 -> RingH5Feature.LIJN;
            case 6 -> RingH6Feature.LIJN;
            case SAUSUMAN_NR -> RingSausumanFeature.LIJN;
            default -> throw new IllegalArgumentException("chapter " + hoofdstuk);
        };
    }

    /** The six chapters in order (without the extra stop). */
    public static List<Verhaallijn> lijnen() {
        return List.of(lijn(1), lijn(2), lijn(3), lijn(4), lijn(5), lijn(6));
    }

    /** Did this player start the story (talked to Guhdalf, or is somewhere in chapter 1 or later)? */
    public static boolean begonnen(ServerPlayer p) {
        Verhaallijn h1 = lijn(1);
        return h1.begonnen(p) || h1.stap(p) > 0;
    }

    /** The whole story is done (chapter 6: the feast in the Gouw). */
    public static boolean klaar(ServerPlayer p) {
        return lijn(6).klaar(p);
    }

    /** On the trip: started and not done. Sam-guh walks along, the ring whispers, the Nine come when it is worn. */
    public static boolean opReis(ServerPlayer p) {
        return begonnen(p) && !klaar(p);
    }

    /** The chapter this player is in: 0 = not started, 1..6, 7 = the story is done. */
    public static int hoofdstuk(ServerPlayer p) {
        if (!begonnen(p)) {
            return 0;
        }
        for (int n = 1; n <= 6; n++) {
            if (!lijn(n).klaar(p)) {
                return n;
            }
        }
        return 7;
    }

    /** Did this player reach that chapter (the one before is done)? 7 = the Toren van Sausuman (after chapter 4). */
    public static boolean bereikt(ServerPlayer p, int hoofdstuk) {
        if (hoofdstuk == SAUSUMAN_NR) {
            return lijn(4).klaar(p);
        }
        return hoofdstuk <= 1 || lijn(hoofdstuk - 1).klaar(p);
    }

    /**
     * The "walk along with a friend" rule. Is this player exactly at this step of this questline? Only then a puzzle, a
     * lever, a scene or a gift of that step reacts to them. A friend who is done (or further, or not that far) may walk
     * along through every structure their own story has reached, but solves nothing for somebody else.
     */
    public static boolean aanZet(ServerPlayer p, Verhaallijn lijn, int stap) {
        return lijn.stap(p) == stap && lijn.aanDeBeurt(p);
    }

    /**
     * Guhdalfs sluier around a chapter structure, in ONE call for every chapter the same way: hidden (smoke, Superkompas,
     * live map, no creature or boss bar leaking out) and protected (one box: the pieces + rand) for everybody, open for a
     * player once their story reached that chapter (the chapter before is done) and for ever after. hoofdstuk 1..6, or
     * {@link #SAUSUMAN_NR}. From the chapter's Feature.register; python: {@code verhaal_motor.sluier(h, structuur)}.
     */
    public static void sluier(String structuur, int rand, int hoofdstuk) {
        Sluiers.registreer(structuur, rand, p -> bereikt(p, hoofdstuk));
    }

    // =====================================================================================================================
    // the ring
    // =====================================================================================================================

    /** Does this player carry the Knabbelring (anywhere in their pockets)? */
    public static boolean heeft(Player p) {
        return p.getInventory().contains(s -> s.is(RingFeature.KNABBELRING.get()));
    }

    /**
     * Gives the ring (chapter 1, at the farewell party). A player has one ring: when they still carry it nothing happens;
     * when they lost it (and the story still needs it) they get it again. Returns true when a ring was given.
     */
    public static boolean geef(ServerPlayer p) {
        if (heeft(p)) {
            return false;
        }
        GuhQuests.saved(p).putBoolean(GEGEVEN, true);
        Minigames.give(p, new ItemStack(RingFeature.KNABBELRING.get()));
        behaald(p, "ring_gekregen");
        return true;
    }

    /** Was the ring given to this player (so Sam-guh hands it back when it got lost on the trip)? */
    public static boolean kreeg(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(GEGEVEN, false);
    }

    /** The ring is gone for good (Smikagol grabs it, it is fried and shared): out of the pockets, off the finger. */
    public static void neem(ServerPlayer p) {
        doeOm(p, false);
        GuhQuests.saved(p).remove(GEGEVEN);
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(RingFeature.KNABBELRING.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (p.containerMenu.getCarried().is(RingFeature.KNABBELRING.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    /** Is this player wearing the ring right now? (Server; not saved: it is off after a login.) */
    public static boolean om(Player p) {
        return OM.containsKey(p.getUUID());
    }

    /** For how many ticks this player has been wearing the ring (0: not). */
    public static int omTicks(ServerPlayer p) {
        Long sinds = OM.get(p.getUUID());
        return sinds == null ? 0 : (int) Math.max(1, p.level().getGameTime() - sinds);
    }

    /** Puts the ring on or takes it off (only a player who carries it can wear it). Returns whether it is on afterwards. */
    public static boolean doeOm(ServerPlayer p, boolean aan) {
        boolean was = om(p);
        if (aan && heeft(p)) {
            if (!was) {
                OM.put(p.getUUID(), p.level().getGameTime());
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
                p.level().sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.0, p.getZ(), 16, 0.3, 0.5, 0.3, 0.01);
                p.sendOverlayMessage(Component.translatable(OM_TEKST).withStyle(ChatFormatting.GOLD));
                behaald(p, "ring_omgedaan");
            }
            return true;
        }
        if (was) {
            OM.remove(p.getUUID());
            p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.3f);
            p.sendOverlayMessage(Component.translatable(AF_TEKST).withStyle(ChatFormatting.GRAY));
        }
        return false;
    }

    /** Mika's look straight through a player who wears the ring (targets, chapter guards: ask this). */
    public static boolean onzichtbaarVoorMikas(Player p) {
        return om(p);
    }

    /**
     * The Eye of Sausron sees a player who wears the ring, wherever they hide; without the ring it looks past a player who
     * plays rock under the Elfenmanteltje ({@link Gaven#isRots}). The Eye itself (ring-h5) adds its own line of sight.
     */
    public static boolean oogZiet(ServerPlayer p) {
        return om(p) || !Gaven.isRots(p);
    }

    // --- its weight ----------------------------------------------------------------------------------------------------------

    /**
     * A chapter sets how heavy the ring is for this player (0 = not, 1 = as heavy as it gets) for the next {@code ticks}
     * ticks; call it again to keep it (the climb of chapter 6). Without this the ring gets heavier by itself near the
     * Frituurberg while chapter 6 is the player's chapter.
     */
    public static void zetZwaarte(ServerPlayer p, double factor, int ticks) {
        ZWAARTES.put(p.getUUID(), new Zwaarte(Math.max(0, Math.min(1, factor)), p.level().getGameTime() + ticks));
    }

    /** How heavy the ring is for this player right now: 0..1 (0 when they don't carry it). */
    public static double zwaarte(ServerPlayer p) {
        if (!heeft(p)) {
            return 0;
        }
        Zwaarte z = ZWAARTES.get(p.getUUID());
        if (z != null) {
            if (z.tot() >= p.level().getGameTime()) {
                return z.factor();
            }
            ZWAARTES.remove(p.getUUID());
        }
        if (hoofdstuk(p) != 6 || p.level().dimension() != BarbecuetherFeature.BARBECUETHER) {
            return 0;
        }
        double best = Double.MAX_VALUE;
        for (Sluiers.Zone berg : Sluiers.zones(p.level(), STRUCTUREN.get(5))) {
            double dx = Math.max(Math.max(berg.x0() - p.getX(), 0), p.getX() - (berg.x1() + 1));
            double dz = Math.max(Math.max(berg.z0() - p.getZ(), 0), p.getZ() - (berg.z1() + 1));
            best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
        }
        return best >= ZWAAR_AFSTAND ? 0 : 0.25 + 0.75 * (1 - best / ZWAAR_AFSTAND);
    }

    /** (every second) the ring's weight as a slower walk; a player who is carried (rides) feels nothing. */
    static void pasZwaarteToe(ServerPlayer p) {
        AttributeInstance snelheid = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (snelheid == null) {
            return;
        }
        double z = p.isPassenger() ? 0 : zwaarte(p);
        AttributeModifier nu = snelheid.getModifier(ZWAAR);
        double wil = -ZWAAR_MAX * z;
        if (z <= 0.01) {
            if (nu != null) {
                snelheid.removeModifier(ZWAAR);
            }
        } else if (nu == null || Math.abs(nu.amount() - wil) > 0.02) {
            snelheid.removeModifier(ZWAAR);
            snelheid.addTransientModifier(new AttributeModifier(ZWAAR, wil, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // =====================================================================================================================
    // the trip
    // =====================================================================================================================

    /** This is the player's rest point from now on (a Rustvuurtje does it by itself; a chapter may set its own spots). */
    public static void rustpunt(ServerPlayer p, Vec3 plek, float yaw) {
        Rustpunten.zet(p, RUST, p.level().dimension(), plek, yaw);
    }

    /** The player's last rest point (null: none yet). */
    @Nullable
    public static Rustpunten.Punt rustpunt(ServerPlayer p) {
        return Rustpunten.van(p, RUST);
    }

    /**
     * Seen or caught (the Eye, the Nine, a fall in the climb): poof, back at the last rest point, with the message
     * quest.guhs.ring.terug.&lt;reden&gt; ({@link #NEGEN}, {@link #OOG}, {@link #GEVALLEN} or a chapter's own reden with its
     * own text). Never damage, nothing is lost; the ring comes off; the hunt of the Nine is over; Sam-guh comes along.
     * Without a rest point yet the player gets a shove away from {@code van} instead. Returns false when the player
     * may not be moved now (a cutscene, a talking screen: {@code Duwtje.mag}).
     */
    public static boolean terugNaarRustpunt(ServerPlayer p, String reden, @Nullable Vec3 van) {
        if (!Duwtje.mag(p)) {
            return false;
        }
        doeOm(p, false);
        Negen.einde(p);
        Gaven.stopRots(p);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        if (!Rustpunten.terug(p, RUST)) {
            Vec3 weg = van == null ? Vec3.directionFromRotation(0, p.getYRot()).scale(-1) : p.position().subtract(van);
            Duwtje.duw(p, weg, 1.4);
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.terug." + reden).withStyle(ChatFormatting.LIGHT_PURPLE));
        Sam.kom(p);
        Smikagol.kom(p);
        return true;
    }

    /** The dimension of a chapter structure: the Knabbelgouw stands in the Guhmensie, everything else in the Barbecuether. */
    public static ResourceKey<Level> dimensie(String structuur) {
        return structuur.equals(STRUCTUREN.get(0)) ? ModDimensions.GUHMENSION : BarbecuetherFeature.BARBECUETHER;
    }

    /** A goal for a questline's {@code .doel(...)}: this structure (its name on the compass is structure.guhs.&lt;id&gt;). */
    public static Doel doel(String structuur) {
        return Doel.structuur(dimensie(structuur), structuur, Component.translatable("structure.guhs." + structuur));
    }

    /** The goal of chapter n: its structure. */
    public static Doel doel(int hoofdstuk) {
        return doel(hoofdstuk == SAUSUMAN_NR ? SAUSUMAN : STRUCTUREN.get(hoofdstuk - 1));
    }

    /** The questline of this story the player is busy with (the first chapter that isn't done), or null. */
    @Nullable
    public static Verhaallijn bezigMet(ServerPlayer p) {
        int n = hoofdstuk(p);
        return n >= 1 && n <= 6 ? lijn(n) : null;
    }

    /**
     * What Sam-guh and Guhdalf say when you ask them: what to do now (the questline's own "nu" text) and which way to walk
     * (the direction and the distance to the next goal; "through the portal first" when it is in the other dimension).
     */
    public static void vertelDoel(ServerPlayer p, Entity spreker) {
        Verhaallijn lijn = bezigMet(p);
        if (lijn == null) {
            GuhQuests.say(p, spreker, klaar(p) ? "quest.guhs.ring.doel.klaar" : "quest.guhs.ring.doel.niet_begonnen");
            return;
        }
        GuhQuests.say(p, spreker, "gui.guhs.verhalen." + lijn.id() + ".nu." + lijn.sleutel(p));
        Doel doel = Doelen.van(p, lijn);
        if (doel == null) {
            return;
        }
        if (doel.dim() != p.level().dimension()) {
            GuhQuests.say(p, spreker, "quest.guhs.ring.doel.portaal");
            return;
        }
        BlockPos daar = Doelen.zoek(p, doel);
        if (daar == null) {
            return;
        }
        double dx = daar.getX() + 0.5 - p.getX(), dz = daar.getZ() + 0.5 - p.getZ();
        int afstand = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        if (afstand <= 12) {
            GuhQuests.say(p, spreker, "quest.guhs.ring.doel.hier", doel.tekst());
        } else {
            GuhQuests.say(p, spreker, "quest.guhs.ring.doel.richting", doel.tekst(), Component.translatable("quest.guhs.ring.windstreek." + windstreek(dx, dz)),
                    afstand);
        }
    }

    /** n, no, o, zo, z, zw, w, nw for a direction on the map (north = -z). */
    public static String windstreek(double dx, double dz) {
        String[] namen = {"z", "zw", "w", "nw", "n", "no", "o", "zo"};
        double graden = Math.toDegrees(Math.atan2(-dx, dz));   // 0 = south (+z), 90 = west
        return namen[(int) Math.floorMod(Math.round(graden / 45.0), 8)];
    }

    /**
     * A milestone of the story for this player: the hidden advancement quest/&lt;naam&gt; (what the FTB quests look at) and,
     * when there is one, its visible twin knabbelring/&lt;naam&gt; in the advancement tab of the story.
     */
    public static void behaald(ServerPlayer p, String naam) {
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, naam);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "knabbelring/" + naam);
    }

    // =====================================================================================================================
    // housekeeping
    // =====================================================================================================================

    /** (logout, death, another dimension) the ring is off. */
    static void vergeet(UUID speler) {
        OM.remove(speler);
        ZWAARTES.remove(speler);
    }

    static void wisAlles() {
        OM.clear();
        ZWAARTES.clear();
    }

    /** (dev / tests) forget the whole story of this player: every chapter, the ring, the rest point, the rewards. */
    public static void wis(ServerPlayer p) {
        for (Verhaallijn l : Verhaallijnen.vanGroep(GROEP)) {
            l.wis(p);
        }
        neem(p);
        Rustpunten.wis(p, RUST);
        CompoundTag saved = GuhQuests.saved(p);
        for (String key : List.copyOf(saved.keySet())) {
            if (key.startsWith("guhs_ring_")) {
                saved.remove(key);
            }
        }
        Sam.stuurWeg(p);
        Smikagol.stuurWeg(p);
        Negen.einde(p);
    }

    /** Is this level the Guhmensie or the Barbecuether (where the story plays)? */
    static boolean verhaalWereld(ServerLevel level) {
        return level.dimension() == ModDimensions.GUHMENSION || level.dimension() == BarbecuetherFeature.BARBECUETHER;
    }

    private Ring() {
    }
}
