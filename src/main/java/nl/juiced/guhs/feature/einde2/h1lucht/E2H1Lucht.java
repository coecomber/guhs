package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 phase B (CONTRACT_E2_B 2.1, 7.2), slice h1_lucht: chapter 1 of the Guhvatar story, the questline
 * {@value E2Verhaal#LUCHT} ({@link #LIJN}, ten steps, done at {@value #STAPPEN}), and what other code calls of this slice:
 * {@link #koningguh} (the Koningguh's story, from {@code GuheindeEvents.talkToKoning}).
 * <ol start="0">
 *   <li>{@link #KAASBOL}: open the glowing kaasbol at the Knabbelberg ({@link Kaasbol}): scenes kaasbol + intro, Appa, the flight;</li>
 *   <li>{@link #VLUCHT}: the flight to the Zuidelijke Luchttempel; arriving there: the narrator card + scene aankomst ({@link Tempel});</li>
 *   <li>{@link #HAL}: Roku-guh or Kyoshi-guh in the Hal van de Guhvatars: scene hal;</li>
 *   <li>{@link #LUCHTBAL}: Guhatso's luchtbal match against Aang-guh ({@link Luchtbal});</li>
 *   <li>{@link #SCOOTER}: the luchtscooter-baan against the clock, wink 1 at the start ({@link Training});</li>
 *   <li>{@link #ZWEVEN}: glide from the great tower through the rings ({@link Training});</li>
 *   <li>{@link #MOMO}: the chase after the lemurguhtje through the temple, scene momo, Momo-guh joins ({@link MomoJacht});</li>
 *   <li>{@link #STAL}: Appa's saddle and three apples ({@link Tempel});</li>
 *   <li>{@link #DEUR}: the Werktuigguh's closed door ("nog niet af, njeg");</li>
 *   <li>{@link #ZUKA}: Zuka's ship rams the pier (scene zuka_steiger), the escape to Appa ({@link Ontsnapping}), scene geleerd,
 *       flag {@value E2Verhaal#V_GELEERD}, the line done, the flight to the Noordelijke Waterstam.</li>
 * </ol>
 * Texts: tools/features/einde2_h1_lucht_tekst.py (chunk c92_e2_h1_lucht).
 */
public final class E2H1Lucht {
    /** The structure of the chapter. */
    public static final String STRUCTUUR = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
    /** The steps (tools/features/einde2_h1_lucht.py checks this number). */
    public static final int STAPPEN = 10;
    public static final int KAASBOL = 0, VLUCHT = 1, HAL = 2, LUCHTBAL = 3, SCOOTER = 4, ZWEVEN = 5, MOMO = 6, STAL = 7, DEUR = 8, ZUKA = 9;
    /** Flags of the line (besides E2Verhaal.V_GELEERD): the escape is on; Zuka's ship lies at the pier; the saddle is whole. */
    public static final String V_VLUCHT = "vlucht", V_SCHIP = "schip", V_ZADEL = "zadel";
    /** Counters: how far the saddle is mended, how many apples Appa ate. */
    public static final String T_ZADEL = "zadel", T_APPELS = "appels";
    /** The lang prefixes of the slice. */
    public static final String P = "gui.guhs.einde2_h1_lucht.", Q = "quest.guhs.einde2_h1_lucht.";
    /** The narrator card of the chapter and the travel map of the whole story. */
    public static final String KAART = "einde2_h1_lucht", REISKAART = "guhvatar";

    /** The questline. */
    public static final Verhaallijn LIJN = Verhaallijn.maak(E2Verhaal.LUCHT, E2Verhaal.GROEP).stappen(STAPPEN).icoon("guhs:luchtscooter")
            .doelregel(true).sleutel(E2H1Lucht::sleutel).extraSleutels("9_vlucht").nodig(E2H1Lucht::nodig)
            .beloningen(E2H1Lucht::beloningen).doel(E2H1Lucht::doel).registreer();

    private E2H1Lucht() {
    }

    private static String sleutel(ServerPlayer p, int stap) {
        return stap == ZUKA && LIJN.vlag(p, V_VLUCHT) ? "9_vlucht" : String.valueOf(stap);
    }

    private static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        if (stap == STAL) {
            return List.of(Verhaallijn.nodig("minecraft:apple", P + "nodig.appels", LIJN.teller(p, T_APPELS), 3));
        }
        return List.of();
    }

    private static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        boolean klaar = LIJN.klaar(p);
        List<VerhaalStand.Beloning> uit = new ArrayList<>();
        uit.add(Verhaallijn.beloning(BuiltInRegistries.ITEM.getKey(ModItems.clothingItem(GuhClothes.GUHVATAR_LUCHTNOMADE)).toString(), klaar));
        uit.add(Verhaallijn.beloning("guhs:luchtscooter", klaar));
        uit.add(Verhaallijn.beloning("minecraft:feather", P + "beloning.luchtsturen", klaar || LIJN.vlag(p, E2Verhaal.V_GELEERD)));
        return uit;
    }

    @Nullable
    private static Doel doel(ServerPlayer p, int stap) {
        if (stap == KAASBOL) {
            ServerLevel g = guheinde(p.level().getServer());
            BlockPos bol = g == null ? null : Kaasbol.plek(g);
            return bol == null ? null : Doel.plek(GuheindeFeature.GUHEINDE, bol, Component.translatable(P + "doel.kaasbol"));
        }
        return Doel.structuur(GuheindeFeature.GUHEINDE, STRUCTUUR, Component.translatable(P + "doel.tempel"));
    }

    // =====================================================================================================================
    // what other code calls
    // =====================================================================================================================

    /**
     * Called by {@code GuheindeEvents.talkToKoning} at the end of the Koningguh's story (the first talk): one more line, where
     * the grillspiespoeder of the new Oog van Vadsig comes from (his story itself already names the recipe and the portal rule).
     */
    public static void koningguh(ServerPlayer player, GuhEntity koning) {
        GuhQuests.say(player, koning, Q + "koning.oog");
    }

    // =====================================================================================================================
    // places (the guaranteed copy of the temple)
    // =====================================================================================================================

    @Nullable
    public static ServerLevel guheinde(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(GuheindeFeature.GUHEINDE);
    }

    /** The world position of a marker of the temple (null: the spot is not worked out yet, or no such marker). */
    @Nullable
    public static BlockPos plek(ServerLevel guheinde, String id) {
        return Plekken.bekend(guheinde, STRUCTUUR) ? Plekken.wereld(guheinde, STRUCTUUR, id) : null;
    }

    /** The middle of a marker's block, on its floor. */
    @Nullable
    public static Vec3 midden(ServerLevel guheinde, String id) {
        BlockPos b = plek(guheinde, id);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** A zone marker of the temple in world coordinates (both corners included). */
    @Nullable
    public static AABB zone(ServerLevel guheinde, String id) {
        Plekken.Plek z = Plekken.plek(STRUCTUUR, id);
        BlockPos o = Plekken.bekend(guheinde, STRUCTUUR) ? Plekken.oorsprong(guheinde, STRUCTUUR) : null;
        if (z == null || o == null) {
            return null;
        }
        BlockPos tot = z.tot() == null ? z.pos() : z.tot();
        return new AABB(Vec3.atLowerCornerOf(o.offset(z.pos())), Vec3.atLowerCornerOf(o.offset(tot)).add(1, 1, 1));
    }

    /** The points of a pad marker of the temple in world coordinates (middle of each block, on its floor). */
    public static List<Vec3> pad(ServerLevel guheinde, String id) {
        Plekken.Plek z = Plekken.plek(STRUCTUUR, id);
        BlockPos o = Plekken.bekend(guheinde, STRUCTUUR) ? Plekken.oorsprong(guheinde, STRUCTUUR) : null;
        if (z == null || o == null) {
            return List.of();
        }
        List<Vec3> uit = new ArrayList<>();
        for (BlockPos b : z.punten()) {
            uit.add(Vec3.atBottomCenterOf(o.offset(b)));
        }
        return uit;
    }

    /** Is the player in the Guheinde and inside the temple's build? */
    public static boolean inTempel(ServerPlayer p) {
        return inTempel(p, p.blockPosition());
    }

    public static boolean inTempel(ServerPlayer p, BlockPos pos) {
        if (!GuheindeFeature.isGuheinde(p.level()) || !Plekken.bekend(p.level(), STRUCTUUR)) {
            return false;
        }
        BoundingBox doos = Plekken.doos(p.level(), STRUCTUUR);
        return doos != null && doos.inflatedBy(2).isInside(pos);
    }

    /** Puts the player at a marker of the temple (dev, a failed course), looking that way. */
    public static boolean zetNaar(ServerPlayer p, String id, float yaw) {
        return zetNaar(p, id, 0, 0, 0, yaw);
    }

    /** The same, this many blocks off the marker. */
    public static boolean zetNaar(ServerPlayer p, String id, int dx, int dy, int dz, float yaw) {
        ServerLevel g = guheinde(p.level().getServer());
        Vec3 naar = g == null ? null : midden(g, id);
        if (naar == null) {
            return false;
        }
        naar = naar.add(dx, dy, dz);
        p.teleportTo(g, naar.x, naar.y + 0.05, naar.z, Set.of(), yaw, 0f, true);
        p.resetFallDistance();
        return true;
    }
}
