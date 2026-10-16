package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 fix round 1, stage 2 (fix2-geestzij, playtest 1: "ietsjes meer hints ... een iets duidelijkere aansluiting"): the
 * step hints of the seven lines of the Geestenwereld ({@link StapHint}). Every step whose spot is not obvious points at
 * it (a chat line with the direction and a landmark, a soft column of light), and where the spot is new a 3 s look scene
 * shows it first. Texts: {@code gui.guhs.einde2_geest_spel.hint.*} and {@code scene.guhs.einde2_geest_spel_kijk_*}
 * (tools/features/einde2_geest_spel_tekst.py HINTS / KIJK).
 * <ul>
 *   <li>"Verder met het verhaal" (the kaasbol) never puts a player INTO the Geestenwereld: the way in is meditation, and a
 *       teleport there would skip the body that stays behind. So every hint here has a resume spot that is never known
 *       ({@link #NIET_TERUG}): the kaasbol then says to look in the Guhdex.</li>
 *   <li>A line's step 0 has no step start ({@code begin} tells no listener), so the White Lotus gate shows its hint itself
 *       ({@link #tick}): the text when the line begins, the look scene once when the player first comes near the
 *       puzzle.</li>
 *   <li>The trials (step {@value GeestLijnen#L_TEGELS}) stay one step while five tiles are earned: after each tile
 *       {@link #volgendeProef} shows the wing of the next master with his own caption.</li>
 * </ul>
 */
public final class GeestHints {
    static final String T = "gui.guhs.einde2_geest_spel.hint.";
    static final String K = "einde2_geest_spel_kijk_";
    /** The resume spot of a Geestenwereld hint: never known (no "Verder" teleport into the spirit world). */
    static final StapHint.Doel NIET_TERUG = StapHint.plek(Geestenwereld.DIM, (p, level) -> null);

    static Cutscene kPuzzel, kHof, kLeden, kVoetstuk, kArchief, kHeiBai, kEikel, kKoh, kChakra, kPad, kPadOpen;
    static final Map<String, Cutscene> VLEUGEL = new HashMap<>();

    private GeestHints() {
    }

    /** A marker of the Geestenwereld as a hint target (a zone: the middle of its floor). */
    static StapHint.Doel op(String marker) {
        return op(p -> marker);
    }

    /** A marker worked out per player (null: nothing to point at now). */
    static StapHint.Doel op(Function<ServerPlayer, String> marker) {
        return StapHint.plek(Geestenwereld.DIM, (p, level) -> blok(marker.apply(p)));
    }

    /** The block of a marker; a zone gives the middle of its floor. Null: no such marker. */
    @Nullable
    static BlockPos blok(@Nullable String marker) {
        if (marker == null) {
            return null;
        }
        AABB d = GeestPlek.doos(marker);
        if (d != null) {
            return BlockPos.containing((d.minX + d.maxX) / 2, d.minY, (d.minZ + d.maxZ) / 2);
        }
        return GeestPlek.pos(marker);
    }

    /** From E2GeestSpelFeature.register (common code: the look scenes must be known on both sides), after the lines. */
    static void registreer() {
        kPuzzel = StapHint.kijkScene(K + "puzzel", true);
        kHof = StapHint.kijkScene(K + "hof", true);
        kLeden = StapHint.kijkScene(K + "leden", true);
        kVoetstuk = StapHint.kijkScene(K + "voetstukken", true);
        kArchief = StapHint.kijkScene(K + "archief", true);
        kHeiBai = StapHint.kijkScene(K + "hei_bai", true);
        kEikel = StapHint.kijkScene(K + "eikel", true);
        kKoh = StapHint.kijkScene(K + "koh", true);
        kChakra = StapHint.kijkScene(K + "chakra", true);
        kPad = StapHint.kijkScene(K + "pad", true);
        kPadOpen = StapHint.kijkScene(K + "pad_open", true);
        for (String m : GeestLijnen.MEESTERS) {
            VLEUGEL.put(m, StapHint.kijkScene(K + "vleugel_" + m, true));
        }

        String wl = GeestLijnen.WITTE_LOTUS;
        StapHint.naar(op("tegel_5")).tekst(T + "lotus.poort").kijk(kPuzzel).baken().terug(NIET_TERUG).zet(wl, GeestLijnen.L_POORT);
        StapHint.naar(op("lotus_hof")).tekst(T + "lotus.hof").kijk(kHof).bijStart().baken().terug(NIET_TERUG).zet(wl, GeestLijnen.L_HOF);
        // (the trials: no look scene of its own here; volgendeProef shows the master's wing with his caption)
        StapHint.naar(op(GeestLijnen::volgendeVleugel)).tekst(T + "lotus.proeven").zonderKijk().baken().terug(NIET_TERUG)
                .zet(wl, GeestLijnen.L_TEGELS);
        StapHint.naar(op(GeestHints::volgendLid)).tekst(T + "lotus.leden").kijk(kLeden).bijStart(60).baken().terug(NIET_TERUG)
                .zet(wl, GeestLijnen.L_LEDEN);
        StapHint.naar(op("stenen_lotus")).tekst(T + "lotus.voetstukken").kijk(kVoetstuk).bijStart(60).baken().terug(NIET_TERUG)
                .zet(wl, GeestLijnen.L_VOETSTUKKEN);
        StapHint.naar(op("archief_ingang")).tekst(T + "lotus.archief").kijk(kArchief).bijStart().baken().terug(NIET_TERUG)
                .zet(wl, GeestLijnen.L_ARCHIEF);

        // the lost lanterns: the next lantern, or the tree while one follows you (no look scene: they are all over the world)
        int[] lantaarns = new int[GeestLijnen.LANTAARNS_N];
        for (int i = 0; i < lantaarns.length; i++) {
            lantaarns[i] = i;
        }
        StapHint.naar(op(p -> Lantaarns.volgers(p).isEmpty() ? Lantaarns.volgende(p) : "lantaarnboom")).tekst(T + "lantaarn")
                .zonderKijk().bijStart(50).baken().terug(NIET_TERUG).zet(GeestLijnen.LANTAARNS, lantaarns);

        StapHint.naar(op("hei_bai")).tekst(T + "hei_bai").kijk(kHeiBai).baken().terug(NIET_TERUG).zet(GeestLijnen.HEI_BAI, GeestLijnen.H_ONTMOET);
        StapHint.naar(op("eikel_plek")).tekst(T + "eikel").kijk(kEikel).bijStart(40).baken().terug(NIET_TERUG)
                .zet(GeestLijnen.HEI_BAI, GeestLijnen.H_EIKEL);

        // Koh: no hint by itself between the riddles (the player stands still before him: a look scene would only disturb)
        StapHint.naar(op("koh_raadsel")).tekst(T + "koh").kijk(kKoh).baken().terug(NIET_TERUG).zet(GeestLijnen.KOH, 0, 1, 2);

        for (int i = 0; i < GeestLijnen.CHAKRAS_N; i++) {
            StapHint.Hint h = StapHint.naar(op("chakra_" + (i + 1))).tekst(T + "chakra." + (i + 1)).kijk(kChakra).baken().terug(NIET_TERUG);
            if (i > 0) {
                h.bijStart(40);
            }
            h.zet(GeestLijnen.CHAKRA, i);
        }

        StapHint.naar(op("pad_8_anker")).tekst(T + "wan.pad").kijk(kPad).bijStart(60).baken().terug(NIET_TERUG).zet(GeestLijnen.WAN, GeestLijnen.W_PAD);
        StapHint.naar(op("pad_8_anker")).tekst(T + "wan.eiland").kijk(kPadOpen).bijStart(60).baken().terug(NIET_TERUG)
                .zet(GeestLijnen.WAN, GeestLijnen.W_EILAND);

        // the court passed: the first trial's wing (the step's own hint has no look scene, this one has the master's)
        GeestLijnen.witteLotus().opStap((p, oud, nieuw) -> {
            if (nieuw == GeestLijnen.L_TEGELS) {
                StapHint.later(p, 140, GeestHints::volgendeProef);
            }
        });
    }

    /** The first member this player has not heard yet, else the founder. */
    @Nullable
    static String volgendLid(ServerPlayer p) {
        for (String lid : WitteLotus.LEDEN) {
            if (!GeestLijnen.witteLotus().vlag(p, "lid_" + lid)) {
                return lid;
            }
        }
        return WitteLotus.STICHTER;
    }

    /**
     * The next trial: its master's wing in a look scene with his caption, then a line with the master's name and the
     * direction. Nothing when every master's tile is earned (or the player left).
     */
    static void volgendeProef(ServerPlayer p) {
        Verhaallijn l = GeestLijnen.witteLotus();
        if (l.stap(p) != GeestLijnen.L_TEGELS || !Geestenwereld.in(p)) {
            return;
        }
        String meester = null;
        for (String m : GeestLijnen.MEESTERS) {
            if (!GeestLijnen.heeftTegel(p, m)) {
                meester = m;
                break;
            }
        }
        if (meester == null) {
            return;
        }
        BlockPos at = blok("echo_" + meester);
        if (at == null) {
            return;
        }
        String m = meester;
        java.util.function.Consumer<ServerPlayer> regel = s -> {
            Component naam = Component.translatable(T + "meester." + m);
            Component richting = Richting.van(s.getEyePosition(), Vec3.atCenterOf(at));
            s.sendSystemMessage(Component.literal("➜ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(T + "lotus.volgende." + m, naam, richting).withStyle(ChatFormatting.YELLOW)));
            s.sendOverlayMessage(Component.translatable(StapHint.P + "balk", richting).withStyle(ChatFormatting.GOLD));
        };
        Cutscene kijk = VLEUGEL.get(meester);
        if (kijk == null || Cutscenes.bezig(p) || !StapHint.speelKijk(p, kijk, at, regel)) {
            regel.accept(p);
        }
    }

    // =====================================================================================================================
    // step 0 of the White Lotus
    // =====================================================================================================================

    /** The White Lotus line just began for p (it begins by itself once the order is open): its gate hint, as text. */
    static void lotusBegonnen(ServerPlayer p) {
        StapHint.later(p, 60, s -> StapHint.toon(s, GeestLijnen.WITTE_LOTUS, false));
    }

    /** Every 10 ticks in the Geestenwereld: the look scene of the gate puzzle once, the first time p comes near it. */
    static void tick(ServerPlayer p) {
        Verhaallijn l = GeestLijnen.witteLotus();
        if (l.stap(p) != GeestLijnen.L_POORT || !GeestToegang.zoneOpen(p, "lotus_ruines")) {
            return;
        }
        Vec3 puzzel = GeestPlek.midden("tegel_5");
        if (puzzel != null && p.position().closerThan(puzzel, 36) && l.eenmalig(p, "kijk_poort")) {
            StapHint.toon(p, GeestLijnen.WITTE_LOTUS, true);
        }
    }

    /** (tests) the hints of this slice: line/step. */
    static List<String> alle() {
        return StapHint.alle().stream().filter(s -> s.startsWith("geest_")).sorted().toList();
    }
}
