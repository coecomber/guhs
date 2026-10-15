package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice h3_aarde: chapter 3 of the Guhvatar story, <b>Ba Sing Guh</b> (DESIGN 5 "Hoofdstuk 3", CONTRACT_E2_B
 * 7.4). The questline {@link #LIJN} ({@code guhvatar_aarde}, fifteen steps, per player, after {@code guhvatar_water}):
 * <ol start="0">
 *   <li>{@link #AANKOMST} arrive at the landing lobe: the narrator card and the scene {@code aankomst};</li>
 *   <li>{@link #LOKET} the strict loketguh: three questions, a stamp, the gate opens (scene {@code loket});</li>
 *   <li>{@link #AARDROMMEL} the Aardrommel: Toph-guh, De Blinde Bandiet, against De Kei-guh (scene);</li>
 *   <li>{@link #BLINDDOEK} Toph's lesson: earth bending with your eyes shut ({@link H3Blinddoek}); the flag {@code geleerd};</li>
 *   <li>{@link #MONORAIL} push the monorail car with earth bending to the middle ring ({@link H3Monorail}); wink 1, Sjokkel;</li>
 *   <li>{@link #THEEHUIS} De Jasmijn Draak: Oom Miroh, and Zuka as a grumpy waiter (scene);</li>
 *   <li>{@link #THEE} make tea for Miroh ({@link H3Thee}): the theeset;</li>
 *   <li>{@link #PAI_SHO} a game of Pai Sho with Miroh ({@link PaiSho}): the Pai Sho table;</li>
 *   <li>{@link #JOO_DEE} "Er is geen knabbeltekort in Ba Sing Guh": Joo Dee-guh, and from then on the Dai Li follow you;</li>
 *   <li>{@link #KAMP} back to the camp: Appa-guh is gone (scene "Appa's verloren dagen", {@code Appa.houdWeg(p, true)});</li>
 *   <li>{@link #KONING} the Aardekoning-guh, Long Feng-Mika whispering, Bosco sniffing at your Appa whistle: Meer Laogai;</li>
 *   <li>{@link #LAOGAI} sneak through the base under the lake ({@link H3Sluipen}); Long Feng unmasked, the reunion with Appa;</li>
 *   <li>{@link #LU_TEN} Miroh is not in his tea house: the hill with the tree (scene {@code lu_ten}, his lullaby);</li>
 *   <li>{@link #VAL} the crystal catacombs: the fall of Ba Sing Guh (scene {@code val});</li>
 *   <li>{@link #TOPH} at the camp: Toph-guh joins the team; the line is done and Appa flies on to the Fire Nation.</li>
 * </ol>
 * The cabbage man's stall breaks three times on the way ({@link H3Kool}), wink 3 (Mewtwo-guh) waits in the base after the
 * reunion ({@link H3Knipogen}). Nothing here costs anything: being seen in the base puts you back at the rest point, a
 * lost Pai Sho game is just a game, a tea that went wrong is just another try.
 * <p>
 * Texts: tools/features/einde2_h3_aarde.py (+ _tekst); places: the sidecar of {@code ba_sing_guh} (the build, plus the
 * hill and the scene cameras of tools/features/einde2_bouw_aarde_verhaal.py).
 */
public final class E2H3Aarde {
    public static final String STRUCTUUR = Einde2Ids.BA_SING_GUH;
    /** The narrator card of the chapter. */
    public static final String KAART = "einde2_h3_aarde";
    public static final int KAART_REGELS = 5;

    public static final int AANKOMST = 0, LOKET = 1, AARDROMMEL = 2, BLINDDOEK = 3, MONORAIL = 4, THEEHUIS = 5, THEE = 6, PAI_SHO = 7,
            JOO_DEE = 8, KAMP = 9, KONING = 10, LAOGAI = 11, LU_TEN = 12, VAL = 13, TOPH = 14, STAPPEN = 15;
    /** The step names (dev command, tests, the report); index = step, the last one = done. */
    public static final List<String> STAP_NAMEN = List.of("aankomst", "loket", "aardrommel", "blinddoek", "monorail", "theehuis", "thee", "pai_sho",
            "joo_dee", "kamp", "koning", "laogai", "lu_ten", "val", "toph", "klaar");

    /** The flag of the blindfold lesson (Toph told you to shut your eyes): the step text then says where the course is. */
    public static final String V_BAAN = "baan";
    /** The flag of a stall that broke for this player ("kool_1" .. "kool_3"). */
    public static final String V_KOOL = "kool_";

    /** The clothes of the chapter (GuhClothes, between the markers einde2_h3_aarde): the Aarderijk hat and the Blinde Bandiet belt. */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.AARDERIJK_HOED, GuhClothes.BLINDE_BANDIET_RIEM);
    public static final String KLEDINGBRON = "einde2_h3_aarde";

    /** The questline (texts: gui.guhs.verhalen.guhvatar_aarde.*). */
    public static final Verhaallijn LIJN = Verhaallijn.maak(E2Verhaal.AARDE, E2Verhaal.GROEP).stappen(STAPPEN).na(E2Verhaal.WATER)
            .icoon("guhs:einde2_bouw_aarde_aardemunt").doelregel(true)
            .sleutel((p, stap) -> stap == BLINDDOEK && E2H3Aarde.lijn().vlag(p, V_BAAN) ? "3_baan" : String.valueOf(stap))
            .extraSleutels("3_baan")
            .doel(E2H3Aarde::doel)
            .beloningen(p -> List.of(
                    Verhaallijn.beloning("guhs:theeset", E2H3Aarde.lijn().stap(p) > THEE),
                    Verhaallijn.beloning("guhs:pai_sho_tafel", E2H3Aarde.lijn().stap(p) > PAI_SHO),
                    Verhaallijn.beloning("guhs:" + GuhClothes.AARDERIJK_HOED.id(), KledingUnlocks.heeft(p, GuhClothes.AARDERIJK_HOED)),
                    Verhaallijn.beloning("guhs:" + GuhClothes.BLINDE_BANDIET_RIEM.id(), KledingUnlocks.heeft(p, GuhClothes.BLINDE_BANDIET_RIEM)),
                    Verhaallijn.beloning("guhs:einde2_h3_aarde_koolkraam", "gui.guhs.verhalen.guhvatar_aarde.beloning.koolkraam",
                            E2H3Aarde.lijn().vlag(p, "koolkraam"))))
            .registreer();

    /**
     * Where the guaranteed copy of Ba Sing Guh stands (template 0, 0, 0 in the world), for this level; null when not known
     * (yet). (tests) a game test puts its own spot here.
     */
    static Function<ServerLevel, BlockPos> oorsprong = level -> level.dimension() == GuheindeFeature.GUHEINDE ? Plekken.oorsprong(level, STRUCTUUR) : null;

    private E2H3Aarde() {
    }

    public static Verhaallijn lijn() {
        return LIJN;
    }

    public static int stap(ServerPlayer p) {
        return LIJN.stap(p);
    }

    // =====================================================================================================================
    // places: a marker of the sidecar in the world
    // =====================================================================================================================

    /** The Guheinde of this server (null in a world without it: the game tests). */
    @Nullable
    static ServerLevel guheinde(ServerPlayer p) {
        return p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
    }

    /** One copy of the city in one level (the guaranteed copy, or a test's spot). */
    public record Stad(ServerLevel level, BlockPos o) {
        @Nullable
        public static Stad van(@Nullable ServerLevel level) {
            BlockPos o = level == null ? null : oorsprong.apply(level);
            return o == null ? null : new Stad(level, o);
        }

        @Nullable
        public Plekken.Plek plek(String id) {
            return Plekken.plek(STRUCTUUR, id);
        }

        /** The world block of a marker (its pos); null when the sidecar has no such marker. */
        @Nullable
        public BlockPos pos(String id) {
            Plekken.Plek p = plek(id);
            return p == null ? null : o.offset(p.pos());
        }

        /** The middle of a marker's block, on its floor (where you stand). */
        @Nullable
        public Vec3 voet(String id) {
            BlockPos b = pos(id);
            return b == null ? null : Vec3.atBottomCenterOf(b);
        }

        /** The box of a zone marker in the world, grown by marge (null: no such zone). */
        @Nullable
        public AABB zone(String id, double marge) {
            Plekken.Plek p = plek(id);
            if (p == null) {
                return null;
            }
            BlockPos a = o.offset(p.pos()), b = o.offset(p.tot() == null ? p.pos() : p.tot());
            return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                    Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1).inflate(marge);
        }

        /** Is the player inside this zone (grown by marge)? */
        public boolean in(ServerPlayer p, String zone, double marge) {
            AABB box = zone(zone, marge);
            return box != null && p.level() == level && box.contains(p.position());
        }

        /** Is the player within r blocks of this marker's spot? */
        public boolean bij(ServerPlayer p, String id, double r) {
            Vec3 v = voet(id);
            return v != null && p.level() == level && p.position().distanceToSqr(v) <= r * r;
        }

        /** The points of a pad marker in the world (the middle of each block's floor). */
        public List<Vec3> pad(String id) {
            Plekken.Plek p = plek(id);
            return p == null ? List.of() : p.punten().stream().map(q -> Vec3.atBottomCenterOf(o.offset(q))).toList();
        }

        /** The whole build (its box), grown by marge. */
        public AABB doos(double marge) {
            Plekken.Blad blad = Plekken.blad(STRUCTUUR);
            return new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + blad.grootte().getX(), o.getY() + blad.grootte().getY(),
                    o.getZ() + blad.grootte().getZ()).inflate(marge);
        }
    }

    // =====================================================================================================================
    // the objective: where to go for this step
    // =====================================================================================================================

    /** The marker the Superkompas points at for a step (null: the city as a whole). */
    @Nullable
    static String doelPlek(int stap) {
        return switch (stap) {
            case AANKOMST -> "aankomst_speler";
            case LOKET -> "loket";
            case AARDROMMEL -> "rustpunt_2";
            case BLINDDOEK -> "toph";
            case MONORAIL -> "monorail_wagen";
            case THEEHUIS -> "theehuis";
            case THEE -> "theeketel";
            case PAI_SHO -> "pai_sho";
            case JOO_DEE -> "joo_dee";
            case KAMP -> "appa_kamp";
            case KONING -> "aardekoning";
            case LAOGAI -> "basis_ingang";
            case LU_TEN -> "lu_ten_boom";
            case VAL -> "catacomben_ingang";
            case TOPH -> "team_toph";
            default -> null;
        };
    }

    private static Doel doel(ServerPlayer p, int stap) {
        Component naam = Component.translatable("gui.guhs.verhalen.guhvatar_aarde.doel." + Math.min(stap, STAPPEN - 1));
        Stad stad = Stad.van(guheinde(p));
        String plek = doelPlek(stap);
        BlockPos pos = stad == null || plek == null ? null : stad.pos(plek);
        if (pos != null && stad.plek(plek).soort().equals(Plekken.ZONE)) {
            AABB box = stad.zone(plek, 0);
            pos = BlockPos.containing(box.getCenter().x, box.minY, box.getCenter().z);
        }
        return pos != null ? Doel.plek(GuheindeFeature.GUHEINDE, pos, naam) : Doel.structuur(GuheindeFeature.GUHEINDE, STRUCTUUR, naam);
    }

    // =====================================================================================================================
    // small helpers of the chapter
    // =====================================================================================================================

    /** "&lt;Name&gt; text" in the chat of this player only (a story line; the key lives in quest.guhs.einde2_h3_aarde.*). */
    static void zeg(ServerPlayer p, Entity spreker, String key, Object... args) {
        GuhQuests.say(p, spreker, "quest.guhs.einde2_h3_aarde." + key, args);
    }

    /** A line of the narrator in the chat (italic grey). */
    static void verteller(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h3_aarde." + key, args).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** "Volgende stap: ..." in gold. */
    static void volgende(ServerPlayer p, int stap) {
        GuhQuests.hint(p, "gui.guhs.verhalen.guhvatar_aarde.kort." + Math.min(stap, STAPPEN - 1));
    }

    /** A step that moved on: the cast that only exists for some steps appears or goes for this player right away. */
    static void naStap(ServerPlayer p, int oud, int nieuw) {
        Zicht.kijkOpnieuw(p);
        if (nieuw < STAPPEN) {
            volgende(p, nieuw);
        }
    }
}
