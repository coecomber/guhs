package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 phase B, slice h2_water: chapter 2 of the Guhvatar story, "De Noordelijke Waterstam" (DESIGN_EINDE2 5, CONTRACT_E2_B
 * 7.3). Everything is per player, in main's questline {@link E2H2WaterFeature#LIJN} ({@code guhvatar_water}); this class
 * is the state machine of its eighteen steps. Who says what: {@link E2H2WaterRol}; the powers of the training:
 * {@link E2H2WaterKrachten}; the two sneak parts on Zhao's ship: {@link Sluipen}; the trip into the Geestenwereld (Koh, the
 * Hemelvijver): {@link Geestreis}; the extra cast and the actors of one player (the sled, the gondola, Zuka in the storm):
 * {@link Toneel}; the scenes: {@link E2H2WaterScenes}.
 * <ol start="0">
 *   <li>{@link #AANKOMST}: the narrator card, then the arrival scene, as soon as the player is inside the Waterstam;</li>
 *   <li>{@link #PAKGUH}: talk to Meester Pakguh: he only teaches guhs with a moustache, Katara-guh challenges him (scene,
 *       then wink 2: Baltoguh);</li>
 *   <li>{@link #ZWEEP}, {@link #GOLFOEFENING}, {@link #IJSBRUG}: the training with the powers waterzweep (the five targets),
 *       watergolf (three waves in the pool) and ijsbrug (freeze the gap to the islet and walk over);</li>
 *   <li>{@link #SLEE}: ride an otter-pinguinguh down the sled run from the wall; {@link #GONDEL}: push the gondola with
 *       waterstroom past the three stops of the canals;</li>
 *   <li>{@link #YUE}: Yue-guh in the palace (Sokka-guh tries to impress her); she warns of the fleet;</li>
 *   <li>{@link #HAVEN}: on the harbour Zhao's soldiers catch you; {@link #ONTSNAP}: in the brig the Blue Spirit lets you out
 *       (scene), sneak off the ship (caught: back to the last rest point);</li>
 *   <li>{@link #MAANVIS}: Zhao steals the moon fish from the oasis (scene): the world turns grey for the player;
 *       {@link #MEDITEER}: sit (sneak) by the pond: the trip with Aang-guh (scene, the wrong dimension) into the
 *       Geestenwereld, flag {@value E2Verhaal#V_GEEST};</li>
 *   <li>{@link #KOH}: walk to Koh: show no emotion; {@link #HEMELVIJVER}: the Hemelvijver and La's wrath, back to the body;</li>
 *   <li>{@link #VIS_HALEN}: sneak onto the ship to the cage; {@link #VIS_TERUG}: the fish back to the oasis: Yue-guh becomes
 *       the moon (scene), the colours come back;</li>
 *   <li>{@link #GOLF}: on the Golfkansel the great wave washes the fleet away (scene); {@link #STORM}: Zuka drags you into
 *       the blizzard on the snow plain (scene), find the light: Katara-guh and Appa find you (scene); flag
 *       {@value E2Verhaal#V_GELEERD}; done: Katara-guh and Sokka-guh join, the rewards, Appa flies on to Ba Sing Guh.</li>
 * </ol>
 */
public final class E2H2Water {
    public static final String STRUCTUUR = Einde2Ids.NOORDELIJKE_WATERSTAM;
    /** The narrator card of the chapter (and the picture behind the replays of its scenes). */
    public static final String KAART = "einde2_h2_water";
    /** The kind of rest point of the two sneak parts ({@link Rustpunten}). */
    public static final String RUST = "einde2_h2_water";

    /** The steps of {@link E2H2WaterFeature#LIJN}. */
    public static final int AANKOMST = 0, PAKGUH = 1, ZWEEP = 2, GOLFOEFENING = 3, IJSBRUG = 4, SLEE = 5, GONDEL = 6, YUE = 7, HAVEN = 8,
            ONTSNAP = 9, MAANVIS = 10, MEDITEER = 11, KOH = 12, HEMELVIJVER = 13, VIS_HALEN = 14, VIS_TERUG = 15, GOLF = 16, STORM = 17,
            STAPPEN = 18;
    /** The names of the steps (the dev command {@code /guhs einde2 h2 ga <naam>}). */
    public static final List<String> STAPNAMEN = List.of("aankomst", "pakguh", "zweep", "golfoefening", "ijsbrug", "slee", "gondel", "yue", "haven",
            "ontsnap", "maanvis", "mediteer", "koh", "hemelvijver", "vis_halen", "vis_terug", "golf", "storm", "klaar");

    /** Flags, counters and once-only marks of the line (guhs_guhvatar_water_v_... / _t_... / _e_...). */
    static final String V_DOEL = "doel_", V_BRUG = "brug", V_HALTE = "halte_", V_VIS = "vis", V_VLOOT_WEG = "vloot_weg", T_GOLVEN = "golven",
            E_MASKER = "masker", E_BELONING = "beloning";
    /** How many whip targets, waves and gondola stops the training wants. */
    public static final int DOELEN = 5, GOLVEN = 3, HALTES = 3;
    /** How long (ticks) the player sits still by the pond before the trip into the Geestenwereld begins. */
    public static final int MEDITEER_TICKS = 60;

    /** The client state this chapter sends (payload {@link E2H2WaterPayloads.Toestand}). */
    public static final int GEWOON = 0, GRIJS = 1, STORMACHTIG = 2;

    private E2H2Water() {
    }

    static Verhaallijn lijn() {
        return E2H2WaterFeature.LIJN;
    }

    // =====================================================================================================================
    // where: the guaranteed copy in the Guheinde (tests: a spot of the test level)
    // =====================================================================================================================

    /** A copy of the Waterstam in a level: template 0,0,0 is {@code oorsprong}. */
    public record Oord(ServerLevel level, BlockPos oorsprong) {
        /** The world position of a marker (null: no such marker in the sidecar). */
        @Nullable
        public BlockPos wereld(String plek) {
            Plekken.Plek p = Plekken.plek(STRUCTUUR, plek);
            return p == null ? null : oorsprong.offset(p.pos());
        }

        /** The middle of a marker's block at its floor. */
        @Nullable
        public Vec3 midden(String plek) {
            BlockPos b = wereld(plek);
            return b == null ? null : Vec3.atBottomCenterOf(b);
        }

        /** The points of a pad marker in the world (empty: none). */
        public List<BlockPos> punten(String plek) {
            Plekken.Plek p = Plekken.plek(STRUCTUUR, plek);
            return p == null ? List.of() : p.punten().stream().map(oorsprong::offset).toList();
        }

        /** Is this spot inside the zone marker, grown by {@code rand} blocks on every side? */
        public boolean in(Vec3 pos, String zone, double rand) {
            Plekken.Plek p = Plekken.plek(STRUCTUUR, zone);
            if (p == null) {
                return false;
            }
            BlockPos a = oorsprong.offset(p.pos()), b = oorsprong.offset(p.tot() == null ? p.pos() : p.tot());
            return pos.x >= Math.min(a.getX(), b.getX()) - rand && pos.x < Math.max(a.getX(), b.getX()) + 1 + rand
                    && pos.y >= Math.min(a.getY(), b.getY()) - rand && pos.y < Math.max(a.getY(), b.getY()) + 1 + rand
                    && pos.z >= Math.min(a.getZ(), b.getZ()) - rand && pos.z < Math.max(a.getZ(), b.getZ()) + 1 + rand;
        }

        /** The whole build. */
        public BoundingBox doos() {
            var g = Plekken.blad(STRUCTUUR).grootte();
            return new BoundingBox(oorsprong.getX(), oorsprong.getY(), oorsprong.getZ(), oorsprong.getX() + g.getX() - 1,
                    oorsprong.getY() + g.getY() - 1, oorsprong.getZ() + g.getZ() - 1);
        }

        /** Within {@code afstand} blocks of a marker (horizontally and vertically)? */
        public boolean bij(Vec3 pos, String plek, double afstand) {
            Vec3 m = midden(plek);
            return m != null && m.distanceToSqr(pos) <= afstand * afstand;
        }
    }

    /** (tests) the copy the chapter plays in instead of the guaranteed one in the Guheinde. */
    static volatile Oord TEST;

    /** The copy of the Waterstam (null: no Guheinde, or the spot is not known yet). */
    @Nullable
    public static Oord oord(MinecraftServer server) {
        Oord t = TEST;
        if (t != null) {
            return t;
        }
        ServerLevel guheinde = server.getLevel(GuheindeFeature.GUHEINDE);
        BlockPos o = guheinde == null ? null : Plekken.oorsprong(guheinde, STRUCTUUR);
        return o == null ? null : new Oord(guheinde, o);
    }

    /** The copy, when the player is in its level (null: elsewhere). */
    @Nullable
    static Oord oordVan(ServerPlayer p) {
        Oord o = oord(p.level().getServer());
        return o != null && o.level() == p.level() ? o : null;
    }

    /** Is the player inside the Waterstam (its box, a few blocks round it)? */
    public static boolean binnen(ServerPlayer p) {
        Oord o = oordVan(p);
        return o != null && o.doos().inflatedBy(4).isInside(p.blockPosition());
    }

    // =====================================================================================================================
    // the per-player state that is not saved (the story itself is all in the questline)
    // =====================================================================================================================

    static final class Speler {
        boolean binnen;
        int hurk;
        @Nullable
        Vec3 hurkPlek;
        int toestand = -1;
        long brugTot;
        int stormGeluid;
    }

    private static final Map<UUID, Speler> SPELERS = new ConcurrentHashMap<>();

    static Speler speler(ServerPlayer p) {
        return SPELERS.computeIfAbsent(p.getUUID(), u -> new Speler());
    }

    static void vergeet(UUID uuid) {
        SPELERS.remove(uuid);
    }

    // =====================================================================================================================
    // talking (chat lines of a character, the narrator, hints)
    // =====================================================================================================================

    /** "&lt;Name&gt; text" in this player's chat, spoken by a character of the cast (the name of its kind). */
    static void zeg(ServerPlayer p, GuhNpcEntity.Kind wie, String key, Object... args) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc." + wie.id())).append("> ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE)));
    }

    /** "&lt;Name&gt; text" with a name key of its own (Koh, the Blue Spirit, the narrator's voices). */
    static void zegAls(ServerPlayer p, String naamKey, String key, Object... args) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable(naamKey)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE)));
    }

    /** A line of the narrator in gold. */
    static void verteller(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD));
    }

    /** "Volgende stap: ..." */
    static void hint(ServerPlayer p, String key) {
        GuhQuests.hint(p, key);
    }

    /** A small line over the hotbar. */
    static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA));
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** Is it this player's turn at this step (the chapter open for them, and exactly at it)? */
    static boolean aanZet(ServerPlayer p, int stap) {
        Verhaallijn l = lijn();
        return l.aanDeBeurt(p) && l.stap(p) == stap;
    }

    /** Plays a scene of this chapter at a marker of the copy (false: no copy / marker, or the player watches something). */
    static boolean speel(ServerPlayer p, Cutscene scene, String anker, @Nullable java.util.function.Consumer<ServerPlayer> daarna) {
        Oord o = oord(p.level().getServer());
        BlockPos a = o == null ? null : o.wereld(anker);
        if (a == null) {
            return false;
        }
        return Cutscenes.speel(p, scene, a, net.minecraft.world.level.block.Rotation.NONE, daarna);
    }

    /** Once a second for every player (the feature's tick listener; the tests call it themselves). */
    static void seconde(ServerPlayer p) {
        Verhaallijn lijn = lijn();
        if (p.isSpectator() || !lijn.aanDeBeurt(p)) {
            stuurToestand(p, GEWOON);
            return;
        }
        int stap = lijn.stap(p);
        if (Geestenwereld.in(p)) {
            stuurToestand(p, GEWOON);
            if (!lijn.klaar(p)) {
                Geestreis.seconde(p, stap);
            }
            return;
        }
        Oord o = oordVan(p);
        boolean binnen = o != null && o.doos().inflatedBy(4).isInside(p.blockPosition());
        Speler s = speler(p);
        if (binnen != s.binnen) {
            s.binnen = binnen;
            if (binnen) {
                E2Team.zetInStructuur(p, STRUCTUUR);   // (the team of this player at the camp, seen by them and their guests)
            } else {
                E2Team.weg(p, STRUCTUUR);
            }
        }
        stuurToestand(p, binnen ? gewensteToestand(p, o, stap) : GEWOON);
        if (!binnen || lijn.klaar(p)) {
            if (!binnen) {
                Toneel.weg(p);
            }
            return;
        }
        Toneel.seconde(p, o, stap);
        switch (stap) {
            case AANKOMST -> {
                if (!E2Reizen.landtNog(p)) {           // (merge_fix1: first Appa lands on his spot, then the card and the scene)
                    aankomst(p);
                }
            }
            case PAKGUH -> wijs(p, o, "pakguh");
            case ZWEEP -> E2H2WaterKrachten.toonDoelen(p, o);
            case GOLFOEFENING -> wijs(p, o, "golf_bassin");
            case IJSBRUG -> ijsbrugKlaar(p, o);
            case SLEE -> Toneel.slee(p, o);
            case GONDEL -> Toneel.gondel(p, o);
            case YUE -> wijs(p, o, "yue");
            case HAVEN -> {
                if (o.in(p.position(), "haven", 0) || o.in(p.position(), "zhao_schip", 0)) {
                    gevangen(p, o);
                }
            }
            case ONTSNAP, VIS_HALEN -> Sluipen.seconde(p, o, stap);
            case MAANVIS -> {
                if (o.in(p.position(), "geestenoase", 0)) {
                    maanvisGestolen(p);
                }
            }
            case MEDITEER, KOH, HEMELVIJVER -> mediteer(p, o, s, stap);
            case VIS_TERUG -> {
                if (o.bij(p.position(), "yue_maan", 8) && lijn.vlag(p, V_VIS)) {
                    yueMaan(p);
                }
            }
            case GOLF -> {
                wijs(p, o, "golf_plek");
                if (o.bij(p.position(), "golf_plek", 3.5)) {
                    golf(p);
                }
            }
            case STORM -> storm(p, o, s);
            default -> {
            }
        }
        if (stap >= ZWEEP && stap <= GONDEL && o.in(p.position(), "trainingsplein", 0)) {
            knipoogSjokkel(p, o);
        }
    }

    /** A little sparkle over the spot the player has to go to now (only for them). */
    static void wijs(ServerPlayer p, Oord o, String plek) {
        Vec3 m = o.midden(plek);
        Plekken.Plek pl = Plekken.plek(STRUCTUUR, plek);
        if (pl != null && Plekken.ZONE.equals(pl.soort()) && pl.tot() != null) {
            BlockPos a = o.oorsprong().offset(pl.pos()), b = o.oorsprong().offset(pl.tot());
            m = new Vec3((a.getX() + b.getX()) / 2.0 + 0.5, Math.min(a.getY(), b.getY()) + 0.5, (a.getZ() + b.getZ()) / 2.0 + 0.5);
        }
        if (m != null && m.distanceToSqr(p.position()) < 48 * 48) {
            Schijn.deeltjes(p, ParticleTypes.GLOW, m.add(0, 2.4, 0), 4, 0.25, 0.02);
        }
    }

    // --- 0: the arrival ------------------------------------------------------------------------------------------------------

    /** Inside the Waterstam with the chapter not begun: the narrator card, then the arrival scene; step 0 is done. */
    static boolean aankomst(ServerPlayer p) {
        if (!aanZet(p, AANKOMST)) {
            return false;
        }
        if (!Verteller.gezien(p, KAART)) {
            return Verteller.toon(p, KAART, speler -> lijn().begin(speler));
        }
        return speel(p, E2H2WaterScenes.AANKOMST, "aankomst_speler", E2H2Water::naAankomst) || naZonderScene(p, E2H2Water::naAankomst);
    }

    /** Where the scene cannot play (no marker in this world), the story still goes on. */
    private static boolean naZonderScene(ServerPlayer p, java.util.function.Consumer<ServerPlayer> daarna) {
        if (oord(p.level().getServer()) != null) {
            return false;   // (the player is watching something else: the next second tries again)
        }
        daarna.accept(p);
        return true;
    }

    static void naAankomst(ServerPlayer p) {
        if (lijn().verder(p, AANKOMST)) {
            verteller(p, "quest.guhs.einde2_h2_water.aankomst.klaar");
            hint(p, "quest.guhs.einde2_h2_water.hint.pakguh");
        }
    }

    // --- 1: Pakguh --------------------------------------------------------------------------------------------------------------

    /** A talk with Meester Pakguh at his step: the scene of the moustache and Katara-guh's challenge, then wink 2. */
    static boolean pakguh(ServerPlayer p) {
        if (!aanZet(p, PAKGUH)) {
            return false;
        }
        boolean gestart = speel(p, E2H2WaterScenes.PAKGUH, "pakguh", speler -> {
            if (!Knipogen.na(speler, E2H2WaterScenes.KNIPOOG_BALTO, "knipoog_balto_camera", E2H2Water::naPakguh)) {
                naPakguh(speler);
            }
        });
        return gestart || naZonderScene(p, E2H2Water::naPakguh);
    }

    static void naPakguh(ServerPlayer p) {
        if (lijn().verder(p, PAKGUH)) {
            zeg(p, GuhNpcEntity.Kind.PAKGUH, "quest.guhs.einde2_h2_water.pakguh.zweep");
            hint(p, "quest.guhs.einde2_h2_water.hint.zweep");
            balk(p, "quest.guhs.einde2_h2_water.kracht.uitleg");
        }
    }

    // --- 2, 3, 4: the training ------------------------------------------------------------------------------------------------

    /** A whip target was hit (E2H2WaterKrachten): counted once per target; all five: on to the waves. */
    static void doelGeraakt(ServerPlayer p, int nr) {
        Verhaallijn l = lijn();
        if (!aanZet(p, ZWEEP) || l.vlag(p, V_DOEL + nr)) {
            return;
        }
        l.vlag(p, V_DOEL + nr, true);
        int n = doelenGeraakt(p);
        balk(p, "quest.guhs.einde2_h2_water.zweep.teller", n, DOELEN);
        if (n >= DOELEN && l.verder(p, ZWEEP)) {
            zeg(p, GuhNpcEntity.Kind.PAKGUH, "quest.guhs.einde2_h2_water.zweep.klaar");
            hint(p, "quest.guhs.einde2_h2_water.hint.golf");
        }
    }

    static int doelenGeraakt(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= DOELEN; i++) {
            n += lijn().vlag(p, V_DOEL + i) ? 1 : 0;
        }
        return n;
    }

    /** A wave in the pool (E2H2WaterKrachten): three and the ice bridge is next. */
    static void golfGemaakt(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (!aanZet(p, GOLFOEFENING)) {
            return;
        }
        int n = Math.min(GOLVEN, l.teller(p, T_GOLVEN) + 1);
        l.teller(p, T_GOLVEN, n);
        balk(p, "quest.guhs.einde2_h2_water.golf.teller", n, GOLVEN);
        if (n >= GOLVEN && l.verder(p, GOLFOEFENING)) {
            zeg(p, GuhNpcEntity.Kind.KATARA_GUH, "quest.guhs.einde2_h2_water.golf.klaar");
            hint(p, "quest.guhs.einde2_h2_water.hint.ijsbrug");
        }
    }

    /** The ice bridge stands for this player (E2H2WaterKrachten): for a minute, shown to them alone. */
    static void brugGemaakt(ServerPlayer p) {
        if (!aanZet(p, IJSBRUG)) {
            return;
        }
        lijn().vlag(p, V_BRUG, true);
        speler(p).brugTot = p.level().getGameTime() + 20 * 60;
        Schijn.straks(p);
        balk(p, "quest.guhs.einde2_h2_water.ijsbrug.staat");
    }

    /** Does this player see their ice bridge now? */
    static boolean brugStaat(ServerPlayer p) {
        return lijn().stap(p) == IJSBRUG && lijn().vlag(p, V_BRUG) && p.level().getGameTime() < speler(p).brugTot;
    }

    /** Over the bridge, on the islet: the training is done. */
    static void ijsbrugKlaar(ServerPlayer p, Oord o) {
        BlockPos b = o.wereld("ijsbrug_b");
        if (b == null || !lijn().vlag(p, V_BRUG) || Vec3.atBottomCenterOf(b.above()).distanceToSqr(p.position()) > 2.2 * 2.2) {
            if (!brugStaat(p)) {
                wijs(p, o, "ijsbrug_a");
            }
            return;
        }
        if (lijn().verder(p, IJSBRUG)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
            zeg(p, GuhNpcEntity.Kind.PAKGUH, "quest.guhs.einde2_h2_water.ijsbrug.klaar");
            zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.slee.sokka");
            hint(p, "quest.guhs.einde2_h2_water.hint.slee");
        }
    }

    // --- 5, 6: the sled and the gondola ---------------------------------------------------------------------------------------

    /** Down the sled run on an otter: at the finish (Toneel.slee). */
    static void sleeKlaar(ServerPlayer p) {
        if (lijn().verder(p, SLEE)) {
            zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.slee.klaar");
            zeg(p, GuhNpcEntity.Kind.KATARA_GUH, "quest.guhs.einde2_h2_water.gondel.katara");
            hint(p, "quest.guhs.einde2_h2_water.hint.gondel");
        }
    }

    /** A gondola stop passed (Toneel.gondel): all three and Yue-guh is next. */
    static void halte(ServerPlayer p, int nr) {
        Verhaallijn l = lijn();
        if (!aanZet(p, GONDEL) || l.vlag(p, V_HALTE + nr)) {
            return;
        }
        l.vlag(p, V_HALTE + nr, true);
        int n = 0;
        for (int i = 1; i <= HALTES; i++) {
            n += l.vlag(p, V_HALTE + i) ? 1 : 0;
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.6f);
        balk(p, "quest.guhs.einde2_h2_water.gondel.teller", n, HALTES);
        if (n >= HALTES && l.verder(p, GONDEL)) {
            zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.gondel.klaar");
            hint(p, "quest.guhs.einde2_h2_water.hint.yue");
        }
    }

    // --- 7: Yue-guh -------------------------------------------------------------------------------------------------------------

    /** The talk in the palace (E2H2WaterRol): Yue-guh, Sokka-guh trying very hard, the warning; then to the harbour. */
    static boolean yue(ServerPlayer p) {
        if (!aanZet(p, YUE)) {
            return false;
        }
        zeg(p, GuhNpcEntity.Kind.YUE_GUH, "quest.guhs.einde2_h2_water.yue.1");
        zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.yue.2");
        zeg(p, GuhNpcEntity.Kind.YUE_GUH, "quest.guhs.einde2_h2_water.yue.3");
        zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.yue.4");
        zeg(p, GuhNpcEntity.Kind.YUE_GUH, "quest.guhs.einde2_h2_water.yue.5");
        zeg(p, GuhNpcEntity.Kind.ARNOOK_GUH, "quest.guhs.einde2_h2_water.yue.6");
        lijn().verder(p, YUE);
        Schijn.straks(p);   // (the fleet is there now)
        hint(p, "quest.guhs.einde2_h2_water.hint.haven");
        return true;
    }

    // --- 8, 9: caught on the harbour, the Blue Spirit, the escape ---------------------------------------------------------------

    /** On the harbour at their step: caught, into the brig of Zhao's ship, and the Blue Spirit comes. */
    static boolean gevangen(ServerPlayer p, Oord o) {
        if (!aanZet(p, HAVEN) || Cutscenes.bezig(p)) {
            return false;
        }
        Vec3 cel = Sluipen.cel(o);
        if (cel == null) {
            return false;
        }
        verteller(p, "quest.guhs.einde2_h2_water.haven.net");
        Duwtje.terug(p, o.level().dimension(), cel, 0f);
        Rustpunten.zet(p, RUST, o.level().dimension(), cel, 0f);
        return speel(p, E2H2WaterScenes.BLAUWE_GEEST, "maanvis_kooi", E2H2Water::naBlauweGeest) || naZonderScene(p, E2H2Water::naBlauweGeest);
    }

    static void naBlauweGeest(ServerPlayer p) {
        if (lijn().verder(p, HAVEN)) {
            hint(p, "quest.guhs.einde2_h2_water.hint.ontsnap");
            balk(p, "quest.guhs.einde2_h2_water.sluip.uitleg");
        }
    }

    /** Off the ship, back on the harbour (Sluipen): the escape is done. A little mask for your guh lies on the quay. */
    static void ontsnapt(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (!l.verder(p, ONTSNAP)) {
            return;
        }
        Rustpunten.wis(p, RUST);
        verteller(p, "quest.guhs.einde2_h2_water.ontsnap.klaar");
        if (l.eenmalig(p, E_MASKER)) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.BLAUWE_GEEST_MASKER)));
            verteller(p, "quest.guhs.einde2_h2_water.ontsnap.masker");
        }
        hint(p, "quest.guhs.einde2_h2_water.hint.oase");
    }

    // --- 10, 11: the moon fish, the meditation ----------------------------------------------------------------------------------

    /** In the oasis at their step: Zhao takes the moon fish (scene); the world turns grey for this player. */
    static boolean maanvisGestolen(ServerPlayer p) {
        if (!aanZet(p, MAANVIS)) {
            return false;
        }
        return speel(p, E2H2WaterScenes.MAANVIS, "yue_maan", E2H2Water::naMaanvis) || naZonderScene(p, E2H2Water::naMaanvis);
    }

    static void naMaanvis(ServerPlayer p) {
        if (lijn().verder(p, MAANVIS)) {
            stuurToestand(p, GRIJS);
            zeg(p, GuhNpcEntity.Kind.AANG_GUH, "quest.guhs.einde2_h2_water.maanvis.aang");
            hint(p, "quest.guhs.einde2_h2_water.hint.mediteer");
        }
    }

    /**
     * Sitting still (sneaking) by the pond. At {@link #MEDITEER}: the trip into the Geestenwereld with its scene; at
     * {@link #KOH} / {@link #HEMELVIJVER} (the soul went back to the body too early): straight in again.
     */
    static void mediteer(ServerPlayer p, Oord o, Speler s, int stap) {
        if (!o.in(p.position(), "geestenoase", 0)) {
            s.hurk = 0;
            return;
        }
        Vec3 vorige = s.hurkPlek;
        s.hurkPlek = p.position();
        boolean bewogen = vorige != null && vorige.distanceToSqr(p.position()) > 0.3 * 0.3;
        if (!p.isShiftKeyDown() || bewogen) {
            if (s.hurk > 0) {
                balk(p, "quest.guhs.einde2_h2_water.mediteer.weg");
            }
            s.hurk = 0;
            if (stap == MEDITEER) {
                wijs(p, o, "yue_maan");
            }
            return;
        }
        s.hurk += 20;
        Schijn.deeltjes(p, ParticleTypes.END_ROD, p.position().add(0, 1.0, 0), 6, 0.6, 0.01);
        if (s.hurk < MEDITEER_TICKS) {
            balk(p, "quest.guhs.einde2_h2_water.mediteer.bezig", (MEDITEER_TICKS - s.hurk) / 20);
            return;
        }
        s.hurk = 0;
        if (stap == MEDITEER) {
            Geestreis.vertrek(p);
        } else {
            Geestreis.terugIn(p);
        }
    }

    // --- 14, 15: the fish back, Yue-guh becomes the moon ----------------------------------------------------------------------

    /** The cage reached (Sluipen): the player carries the moon fish. */
    static void visGepakt(ServerPlayer p) {
        Verhaallijn l = lijn();
        if (!aanZet(p, VIS_HALEN)) {
            return;
        }
        l.vlag(p, V_VIS, true);
        if (l.verder(p, VIS_HALEN)) {
            Rustpunten.wis(p, RUST);
            verteller(p, "quest.guhs.einde2_h2_water.vis.gepakt");
            zeg(p, GuhNpcEntity.Kind.KATARA_GUH, "quest.guhs.einde2_h2_water.vis.katara");
            hint(p, "quest.guhs.einde2_h2_water.hint.vis_terug");
        }
    }

    /** With the fish at the pond: Yue-guh gives the moon spirit her own light (scene); the colours come back. */
    static boolean yueMaan(ServerPlayer p) {
        if (!aanZet(p, VIS_TERUG)) {
            return false;
        }
        E2Audio.moment(p, "yue_maan");
        boolean gestart = speel(p, E2H2WaterScenes.YUE_MAAN, "yue_maan", E2H2Water::naYueMaan);
        if (!gestart) {
            E2Audio.stopMoment(p, 20);
            return naZonderScene(p, E2H2Water::naYueMaan);
        }
        return true;
    }

    static void naYueMaan(ServerPlayer p) {
        E2Audio.stopMoment(p, 40);
        Verhaallijn l = lijn();
        if (l.verder(p, VIS_TERUG)) {
            l.vlag(p, V_VIS, false);
            stuurToestand(p, GEWOON);
            verteller(p, "quest.guhs.einde2_h2_water.yue_maan.klaar");
            hint(p, "quest.guhs.einde2_h2_water.hint.golf_kansel");
        }
    }

    // --- 16: the great wave --------------------------------------------------------------------------------------------------

    /** On the Golfkansel at their step: the wave (scene) washes the fleet away, gently. */
    static boolean golf(ServerPlayer p) {
        if (!aanZet(p, GOLF)) {
            return false;
        }
        E2Audio.moment(p, "golf");
        boolean gestart = speel(p, E2H2WaterScenes.GOLF, "golf_plek", E2H2Water::naGolf);
        if (gestart) {
            lijn().vlag(p, V_VLOOT_WEG, true);   // (the fleet of the overlay goes; the scene's own ships take its place)
            Schijn.straks(p);
            return true;
        }
        E2Audio.stopMoment(p, 20);
        return naZonderScene(p, E2H2Water::naGolf);
    }

    static void naGolf(ServerPlayer p) {
        E2Audio.stopMoment(p, 40);
        if (!lijn().verder(p, GOLF)) {
            return;
        }
        Oord o = oord(p.level().getServer());
        Vec3 start = o == null ? null : o.midden("sneeuwstorm_start");
        if (o == null || start == null) {
            // (a world whose Waterstam has no snow plain: the storm is skipped)
            klaar(p);
            return;
        }
        verteller(p, "quest.guhs.einde2_h2_water.storm.gegrepen");
        Duwtje.terug(p, o.level().dimension(), start, 90f);
    }

    // --- 17: the blizzard ------------------------------------------------------------------------------------------------------

    /** On the snow plain: the scene of Zuka, then find the light through the storm; Katara-guh and Appa find you. */
    static void storm(ServerPlayer p, Oord o, Speler s) {
        Vec3 start = o.midden("sneeuwstorm_start");
        Vec3 vind = o.midden("sneeuwstorm_vind");
        if (start == null || vind == null) {
            klaar(p);
            return;
        }
        if (!o.in(p.position(), "sneeuwvlakte", 6)) {
            if (Cutscenes.bezig(p)) {
                return;
            }
            // the storm does not let go: back in the snow, where Zuka keeps you
            verteller(p, "quest.guhs.einde2_h2_water.storm.terug");
            Duwtje.terug(p, o.level().dimension(), start, 90f);
            return;
        }
        if (!Cutscenes.gezien(p, E2H2WaterScenes.STORM.id())) {
            speel(p, E2H2WaterScenes.STORM, "sneeuwstorm_start", speler -> hint(speler, "quest.guhs.einde2_h2_water.hint.storm"));
            return;
        }
        // the light of Katara-guh's lantern and Appa's call show the way
        Schijn.deeltjes(p, ParticleTypes.FLAME, vind.add(0, 2.2, 0), 6, 0.4, 0.01);
        Schijn.deeltjes(p, ParticleTypes.END_ROD, vind.add(0, 3.0, 0), 4, 0.6, 0.02);
        if (++s.stormGeluid % 4 == 0) {
            Schijn.geluid(p, nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.BRUL.get(), BlockPos.containing(vind), 1.6f, 0.9f);
        }
        if (vind.distanceToSqr(p.position()) <= 4.0 * 4.0) {
            speel(p, E2H2WaterScenes.TEAM, "sneeuwstorm_vind", E2H2Water::klaar);
        }
    }

    /** The chapter is done: water bending learnt, Katara-guh and Sokka-guh join, the rewards, Appa flies on. */
    static void klaar(ServerPlayer p) {
        Verhaallijn l = lijn();
        l.vlag(p, E2Verhaal.V_GELEERD, true);
        if (!l.zet(p, STAPPEN)) {
            return;
        }
        stuurToestand(p, GEWOON);
        Toneel.weg(p);
        if (l.eenmalig(p, E_BELONING)) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.WATERSTAM_PARKA)));
            Minigames.give(p, new ItemStack(E2H2WaterFeature.BOEMERANG.get()));
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            if (l.eenmalig(p, E_MASKER)) {
                Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.BLAUWE_GEEST_MASKER)));
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
        verteller(p, "quest.guhs.einde2_h2_water.klaar.1");
        zeg(p, GuhNpcEntity.Kind.SOKKA_GUH, "quest.guhs.einde2_h2_water.klaar.boemerang");
        verteller(p, "quest.guhs.einde2_h2_water.klaar.2");
        // on to Ba Sing Guh: a real flight on Appa (reizen); without a known spot the whistle brings you later
        if (!E2Reizen.vlieg(p, Einde2Ids.BA_SING_GUH, E2Reizen.Vlucht.H2_H3, null)) {
            verteller(p, "quest.guhs.einde2_h2_water.klaar.geen_vlucht");
        }
    }

    // --- the winks ---------------------------------------------------------------------------------------------------------------

    /** Wink 1 goes on: Sjokkel creeps along the palace terrace behind the training square (once, while training). */
    static void knipoogSjokkel(ServerPlayer p, Oord o) {
        if (!Cutscenes.gezien(p, E2H2WaterScenes.KNIPOOG_SJOKKEL.id())) {
            Knipogen.toon(p, E2H2WaterScenes.KNIPOOG_SJOKKEL, "knipoog_sjokkel_camera", null);
        }
    }

    // =====================================================================================================================
    // the client state (grey world, blizzard)
    // =====================================================================================================================

    /** What the client should show now inside the Waterstam: grey after the theft of the fish, the blizzard on the plain. */
    static int gewensteToestand(ServerPlayer p, Oord o, int stap) {
        if (stap >= MEDITEER && stap <= VIS_TERUG) {
            return GRIJS;
        }
        if (stap == STORM && o.in(p.position(), "sneeuwvlakte", 10)) {
            return STORMACHTIG;
        }
        return GEWOON;
    }

    /** Sends the state when it changed for this player. */
    static void stuurToestand(ServerPlayer p, int toestand) {
        Speler s = speler(p);
        if (s.toestand != toestand) {
            s.toestand = toestand;
            E2H2WaterPayloads.stuur(p, new E2H2WaterPayloads.Toestand(toestand));
        }
    }

    /** (logout, dimension change) the client forgot it: send again next time. */
    static void toestandVergeten(ServerPlayer p) {
        speler(p).toestand = -1;
    }

    // =====================================================================================================================
    // dev and tests
    // =====================================================================================================================

    /** Forgets everything this player did in the chapter (dev command, tests). */
    public static void wis(ServerPlayer p) {
        lijn().wis(p);
        Verteller.vergeet(p, KAART);
        for (Cutscene s : E2H2WaterScenes.alle()) {
            Cutscenes.vergeet(p, s.id());
        }
        Rustpunten.wis(p, RUST);
        stuurToestand(p, GEWOON);
        Toneel.weg(p);
        Geestreis.vergeet(p);
        Schijn.straks(p);
    }
}
