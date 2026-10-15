package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.techbron.AangebrandeMika;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Het Guhpad (DESIGN_VERHALENPAD A): the stories open the worlds. The rules, each behind ONE static method:
 * <ul>
 *   <li>{@link #magKnabbelring}: Guhdalf only starts the Knabbelring for a player who finished every big story of the
 *       Guhmensie (the gate of the story, {@code ring.Ring.magBeginnen}, asks it; his refusal lists what is missing:
 *       {@link #guhdalfWeigert});</li>
 *   <li>{@link #magBarbecuether}: the grill portal Guhmensie -> Guhbarbecuether: those stories AND chapter 1 of the
 *       Knabbelring (the lock is the first of {@code GrillPortalBlock.SLOTEN}: {@link #grillSlot}; the gate of the portal,
 *       {@code ring.Ring.magDoorPortaal}, asks the stories too, so both say the same);</li>
 *   <li>{@link #magGuheinde}: the portal in the Knabbelkelder: the Knabbelring and Super Guhrio finished and the
 *       Aangebrande Mika beaten ({@code GuheindePortaalBlock} asks {@link #guheindeGeweigerd} / {@link #guheindeSlot}).
 *       <b>The Guheinde update extends this one method</b> (or {@link #eisen} for {@link Wereld#GUHEINDE}).</li>
 * </ul>
 * The locks hold for everybody, also for who was further before the update. They only ever say no to a player who wants to
 * go IN: a lock is asked with the dimension the portal stands in, and a portal that stands in the locked world itself (the
 * way out, a Knabbelpoort) is never refused. Nobody is moved, so a player who is inside may stay until they leave, and
 * nothing of anybody's story progress is touched. Spectators and non-players pass.
 * <p>
 * What a world asks is a list of {@link Eis}en ({@link #eisen}); the messages, the path map of the Guhdex and the lock
 * quests of the FTB book all show that same list. Texts: tools/features/guhpad.py.
 */
public final class Guhpad {
    /** One thing a world asks of a player: its lang key, the item that draws it, and whether this player has done it. */
    public record Eis(String sleutel, String icoon, boolean voldaan) {
        public MutableComponent naam() {
            return Component.translatable(sleutel);
        }
    }

    /** Chapter 1 of the Knabbelring (the grill portal) and the Aangebrande Mika (the Guheinde): asked besides the stories. */
    public static final String EIS_KNABBELFEEST = "gui.guhs.guhpad.eis.knabbelfeest", EIS_MIKA = "gui.guhs.guhpad.eis.aangebrande_mika";
    /** The real Guheinde does not exist yet: this is never done. */
    public static final String EIS_ECHT = "gui.guhs.guhpad.eis.echt";
    /** The messages: Guhdalf's refusal, the grill portal, the portal of the Knabbelkelder (each with the list as %s). */
    public static final String GUHDALF_NEE = "quest.guhs.guhpad.guhdalf.nee", GRILL_DICHT = "quest.guhs.guhpad.grillportaal",
            GUHEINDE_DICHT = "quest.guhs.guhpad.guheindeportaal";
    /** How long (ticks) a refused player isn't told again at the portal of the Knabbelkelder (one message per attempt). */
    public static final int BERICHT_TICKS = 100;
    private static final Map<UUID, Long> GEWEIGERD_OP = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // what each world asks
    // =====================================================================================================================

    /**
     * Everything this world asks before a player may go in, done or not, in the order it is shown:
     * the Guhmensie nothing; the Guhbarbecuether every big story of the Guhmensie and Guhdalf's party (chapter 1 of the
     * Knabbelring); the Guheinde every big story of the Guhbarbecuether and the Aangebrande Mika; the real Guheinde every
     * big story there is, and itself (it does not exist yet).
     */
    public static List<Eis> eisen(ServerPlayer p, Wereld wereld) {
        List<Eis> out = new ArrayList<>();
        switch (wereld) {
            case GUHMENSIE -> {
            }
            case BARBECUETHER -> {
                verhalen(p, Wereld.GUHMENSIE, out);
                out.add(new Eis(EIS_KNABBELFEEST, "minecraft:firework_rocket", Ring.lijn(1).klaar(p)));
            }
            case GUHEINDE -> {
                verhalen(p, Wereld.BARBECUETHER, out);
                out.add(new Eis(EIS_MIKA, "guhs:gloeister", AangebrandeMika.verslagen(p)));
            }
            case ECHT -> {
                for (Wereld w : Wereld.values()) {
                    verhalen(p, w, out);
                }
                out.add(new Eis(EIS_ECHT, "minecraft:ender_eye", false));
            }
        }
        return out;
    }

    private static void verhalen(ServerPlayer p, Wereld wereld, List<Eis> out) {
        for (GrootVerhaal v : GroteVerhalen.van(wereld)) {
            out.add(new Eis(v.naamSleutel(), v.icoon(), v.klaar(p)));
        }
    }

    /** What this player still misses for this world (empty: it is open for them). */
    public static List<Eis> ontbreekt(ServerPlayer p, Wereld wereld) {
        return eisen(p, wereld).stream().filter(e -> !e.voldaan()).toList();
    }

    /** May this player go into this world? */
    public static boolean open(ServerPlayer p, Wereld wereld) {
        return ontbreekt(p, wereld).isEmpty();
    }

    /** The big stories of the Guhmensie this player has not finished yet (what Guhdalf wants first). */
    public static List<Eis> ontbreektVoorKnabbelring(ServerPlayer p) {
        List<Eis> out = new ArrayList<>();
        verhalen(p, Wereld.GUHMENSIE, out);
        return out.stream().filter(e -> !e.voldaan()).toList();
    }

    // =====================================================================================================================
    // the three locks: ONE static method each
    // =====================================================================================================================

    /** May Guhdalf start the Knabbelring for this player: every big story of the Guhmensie is finished? */
    public static boolean magKnabbelring(ServerPlayer p) {
        return ontbreektVoorKnabbelring(p).isEmpty();
    }

    /** May this player take a grill portal from the Guhmensie into the Guhbarbecuether? */
    public static boolean magBarbecuether(ServerPlayer p) {
        return open(p, Wereld.BARBECUETHER);
    }

    /**
     * May this player take the portal of the Knabbelkelder into the Guheinde? The Knabbelring and Super Guhrio finished and
     * the Aangebrande Mika beaten ({@code AangebrandeMika.verslagen}). The Guheinde update extends THIS method.
     */
    public static boolean magGuheinde(ServerPlayer p) {
        return open(p, Wereld.GUHEINDE);
    }

    // =====================================================================================================================
    // saying no, with the list of what is missing
    // =====================================================================================================================

    /** "A, B en C" (gold names; the comma and the "en" are lang keys). */
    public static MutableComponent lijst(List<Eis> eisen) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < eisen.size(); i++) {
            if (i > 0) {
                out.append(Component.translatable(i == eisen.size() - 1 ? "gui.guhs.guhpad.lijst.en" : "gui.guhs.guhpad.lijst.komma"));
            }
            out.append(eisen.get(i).naam().withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    /**
     * Guhdalf, to a player he cannot start the story for yet: he lists the stories of the Guhmensie that are missing.
     * False: nothing is missing (he said nothing).
     */
    public static boolean guhdalfWeigert(GuhNpcEntity guhdalf, ServerPlayer p) {
        List<Eis> mist = ontbreektVoorKnabbelring(p);
        if (mist.isEmpty()) {
            return false;
        }
        GuhQuests.say(p, guhdalf, GUHDALF_NEE, lijst(mist));
        return true;
    }

    /**
     * Why this entity may not go from {@code van} to {@code naar} (null: it may). Only a player who wants to go INTO the
     * Guhbarbecuether or the Guheinde from somewhere else is ever refused: the way out, a trip inside one world, spectators
     * and everything that is not a player always pass.
     */
    @Nullable
    public static Component slot(ResourceKey<Level> van, ResourceKey<Level> naar, Entity entity) {
        if (van == naar || !(entity instanceof ServerPlayer p) || p.isSpectator()) {
            return null;
        }
        Wereld doel = Wereld.van(naar);
        if (doel == Wereld.BARBECUETHER) {
            List<Eis> verhalen = ontbreektVoorKnabbelring(p);
            if (!verhalen.isEmpty()) {
                return Component.translatable(GRILL_DICHT, lijst(verhalen));
            }
            // (every story is there: what is left is Guhdalf's own party, with the Knabbelring's own words)
            return magBarbecuether(p) ? null : Component.translatable(RingFeature.PORTAAL_DICHT);
        }
        if (doel == Wereld.GUHEINDE && !magGuheinde(p)) {
            return Component.translatable(GUHEINDE_DICHT, lijst(ontbreekt(p, Wereld.GUHEINDE)));
        }
        return null;
    }

    /** The lock of the grill portal ({@code GrillPortalBlock.SLOTEN}: only ever asked in the Guhmensie, on the way there). */
    @Nullable
    public static Component grillSlot(ServerLevel level, Entity entity) {
        return slot(nl.juiced.guhs.world.ModDimensions.GUHMENSION, Wereld.BARBECUETHER.dimensie(), entity);
    }

    /** Why this entity may not take the Guheinde portal that stands in this level (null: it may; always null in the Guheinde itself). */
    @Nullable
    public static Component guheindeSlot(ServerLevel level, Entity entity) {
        return guheindeSlot(level.dimension(), entity);
    }

    /** The same with the dimension the portal stands in passed in (the game tests have no Guhmensie and no Guheinde). */
    @Nullable
    public static Component guheindeSlot(ResourceKey<Level> dim, Entity entity) {
        return slot(dim, Wereld.GUHEINDE.dimensie(), entity);
    }

    /** Refused at the portal of the Knabbelkelder (with the message, once per attempt)? */
    public static boolean guheindeGeweigerd(ServerLevel level, Entity entity) {
        return geweigerd(level, entity, guheindeSlot(level, entity));
    }

    /** Says {@code nee} to a player (at most once per {@link #BERICHT_TICKS} while they keep standing in the portal). */
    static boolean geweigerd(ServerLevel level, Entity entity, @Nullable Component nee) {
        if (nee == null) {
            return false;
        }
        if (entity instanceof ServerPlayer p) {
            long nu = level.getServer().getTickCount();
            Long vorige = GEWEIGERD_OP.get(p.getUUID());
            if (vorige == null || nu - vorige >= BERICHT_TICKS || nu < vorige) {
                p.sendSystemMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            GEWEIGERD_OP.put(p.getUUID(), nu);   // (still standing in the portal: the same attempt)
        }
        return true;
    }

    static void vergeet(UUID speler) {
        GEWEIGERD_OP.remove(speler);
    }

    private Guhpad() {
    }
}
