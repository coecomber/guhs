package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 phase B, slice geest_spel: the seven questlines of the Geestenwereld (CONTRACT_E2_B 2.1). All in the group
 * {@value E2Verhaal#GROEP}, none with a {@code na}: what opens them is the zone access of {@link GeestToegang}. Texts:
 * tools/features/einde2_geest_spel.py ({@code gui.guhs.verhalen.geest_*}).
 * <ul>
 *   <li>{@link #WITTE_LOTUS}: the gate, the court, the five trials (a tile each), the old members (the White Lotus tile),
 *       the six pedestals, the Oude Reigerguh and the archive;</li>
 *   <li>{@link #LANTAARNS}: eight lost lantern spirits home to the Lantaarnboom (the step = how many are home);</li>
 *   <li>{@link #HEI_BAI}: the angry forest spirit and the knabbelkoor acorn;</li>
 *   <li>{@link #KOH}: three riddles of Koh without showing a feeling (after chapter 2);</li>
 *   <li>{@link #CHAKRA}: seven meditations, one per zone, with Guru Pathik-guh at the first;</li>
 *   <li>{@link #VERSCHIJNINGEN}: four rare sightings (the step = how many seen);</li>
 *   <li>{@link #WAN}: the hooded figure in the distance, the path to the eighth island, the origin.</li>
 * </ul>
 */
public final class GeestLijnen {
    public static final String WITTE_LOTUS = "geest_witte_lotus", LANTAARNS = "geest_lantaarns", HEI_BAI = "geest_hei_bai", KOH = "geest_koh",
            CHAKRA = "geest_chakra", VERSCHIJNINGEN = "geest_verschijningen", WAN = "geest_wan";
    public static final List<String> ALLE = List.of(WITTE_LOTUS, LANTAARNS, HEI_BAI, KOH, CHAKRA, VERSCHIJNINGEN, WAN);

    /** The steps of the White Lotus. */
    public static final int L_POORT = 0, L_HOF = 1, L_TEGELS = 2, L_LEDEN = 3, L_VOETSTUKKEN = 4, L_ARCHIEF = 5, L_KLAAR = 6;
    /** The steps of Hei Bai-guh. */
    public static final int H_ONTMOET = 0, H_EIKEL = 1, H_KLAAR = 2;
    /** The steps of Wan-guh. */
    public static final int W_GLIMP = 0, W_PAD = 1, W_EILAND = 2, W_KLAAR = 3;
    /** How many lanterns, riddles, chakras and sightings. */
    public static final int LANTAARNS_N = 8, RAADSELS_N = 3, CHAKRAS_N = 7, VERSCHIJNINGEN_N = 4, GLIMPEN_N = 3;
    /** The five masters of the trials, in wing order (and their tiles), then the White Lotus tile. */
    public static final List<String> MEESTERS = List.of("bumi", "pakguh", "piandao", "jeong_jeong", "miroh");
    public static final List<String> TEGELS = List.of("bumi", "pakguh", "piandao", "jeong_jeong", "miroh", "witte_lotus");
    /** The four rare sightings (flags {@code v_<naam>} of {@link #VERSCHIJNINGEN}). */
    public static final List<String> ZELDZAAM = List.of("reiger", "vosje", "koh", "maan");

    private static Verhaallijn witteLotus, lantaarns, heiBai, koh, chakra, verschijningen, wan;

    private GeestLijnen() {
    }

    @Nullable
    public static Verhaallijn lijn(String id) {
        return switch (id) {
            case WITTE_LOTUS -> witteLotus;
            case LANTAARNS -> lantaarns;
            case HEI_BAI -> heiBai;
            case KOH -> koh;
            case CHAKRA -> chakra;
            case VERSCHIJNINGEN -> verschijningen;
            case WAN -> wan;
            default -> null;
        };
    }

    public static Verhaallijn witteLotus() {
        return witteLotus;
    }

    public static Verhaallijn lantaarns() {
        return lantaarns;
    }

    public static Verhaallijn heiBai() {
        return heiBai;
    }

    public static Verhaallijn koh() {
        return koh;
    }

    public static Verhaallijn chakra() {
        return chakra;
    }

    public static Verhaallijn verschijningen() {
        return verschijningen;
    }

    public static Verhaallijn wan() {
        return wan;
    }

    // =====================================================================================================================
    // register
    // =====================================================================================================================

    static void register() {
        witteLotus = Verhaallijn.maak(WITTE_LOTUS, E2Verhaal.GROEP).stappen(L_KLAAR).icoon("guhs:pai_sho_tegel_witte_lotus").doelregel(true)
                .nodig(GeestLijnen::nodigLotus)
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:pai_sho_tegel_witte_lotus", alleTegels(p))))
                .doel((p, stap) -> doel(switch (stap) {
                    case L_POORT -> "poort";
                    case L_HOF -> "lotus_hof";
                    case L_TEGELS -> volgendeVleugel(p);
                    case L_LEDEN -> "lid_stichter";
                    case L_VOETSTUKKEN -> "stenen_lotus";
                    default -> "archief_ingang";
                }, "gui.guhs.einde2_geest_spel.doel.lotus"))
                .registreer();
        lantaarns = Verhaallijn.maak(LANTAARNS, E2Verhaal.GROEP).stappen(LANTAARNS_N).icoon("guhs:geestlantaarn").doelregel(true)
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:libelkonijnguh_spawn_egg", "gui.guhs.einde2_geest_spel.beloning.libelkonijnguh",
                        lantaarns.klaar(p))))
                .doel((p, stap) -> doel(Lantaarns.volgende(p), "gui.guhs.einde2_geest_spel.doel.lantaarn"))
                .registreer();
        heiBai = Verhaallijn.maak(HEI_BAI, E2Verhaal.GROEP).stappen(H_KLAAR).icoon("guhs:einde2_geest_spel_knabbelkoor_eikel").doelregel(true)
                .nodig((p, stap) -> stap == H_EIKEL ? List.of(Verhaallijn.nodig("guhs:einde2_geest_spel_knabbelkoor_eikel",
                        nl.juiced.guhs.quest.GuhQuests.count(p, E2GeestSpelFeature.EIKEL.get()), 1)) : List.of())
                .doel((p, stap) -> doel(stap == H_ONTMOET ? "hei_bai" : "eikel_plek", "gui.guhs.einde2_geest_spel.doel.hei_bai"))
                .registreer();
        koh = Verhaallijn.maak(KOH, E2Verhaal.GROEP).stappen(RAADSELS_N).icoon("guhs:einde2_geest_spel_koh_masker").doelregel(true)
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_geest_spel_koh_masker", koh.klaar(p))))
                .doel((p, stap) -> doel("koh_raadsel", "gui.guhs.einde2_geest_spel.doel.koh"))
                .registreer();
        chakra = Verhaallijn.maak(CHAKRA, E2Verhaal.GROEP).stappen(CHAKRAS_N).icoon("minecraft:amethyst_shard").doelregel(true)
                .beloningen(p -> List.of(Verhaallijn.beloning("minecraft:glow_ink_sac", "gui.guhs.einde2_geest_spel.beloning.gloed", chakra.klaar(p))))
                .doel((p, stap) -> doel("chakra_" + (stap + 1), "gui.guhs.einde2_geest_spel.doel.chakra"))
                .registreer();
        verschijningen = Verhaallijn.maak(VERSCHIJNINGEN, E2Verhaal.GROEP).stappen(VERSCHIJNINGEN_N).icoon("guhs:einde2_geest_spel_veer")
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_geest_spel_veer", verschijningen.vlag(p, "reiger")),
                        Verhaallijn.beloning("guhs:einde2_geest_spel_gouden_rol", verschijningen.vlag(p, "vosje")),
                        Verhaallijn.beloning("guhs:einde2_geest_spel_koh_schub", verschijningen.vlag(p, "koh")),
                        Verhaallijn.beloning("guhs:einde2_geest_spel_maandruppel", verschijningen.vlag(p, "maan"))))
                .registreer();
        wan = Verhaallijn.maak(WAN, E2Verhaal.GROEP).stappen(W_KLAAR).icoon("guhs:einde2_geest_spel_eerste_knabbel").doelregel(true)
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_geest_spel_eerste_knabbel", wan.klaar(p))))
                .doel((p, stap) -> stap == W_GLIMP ? null : doel(stap == W_PAD ? "pad_8_anker" : "wan_onthulling", "gui.guhs.einde2_geest_spel.doel.wan"))
                .registreer();
    }

    @Nullable
    private static Doel doel(@Nullable String marker, String tekst) {
        BlockPos at = marker == null ? null : GeestPlek.pos(marker);
        return at == null ? null : Doel.plek(Geestenwereld.DIM, at, Component.translatable(tekst));
    }

    // =====================================================================================================================
    // the White Lotus: tiles
    // =====================================================================================================================

    /** Has this player earned this tile (flag {@code v_tegel_<meester>})? */
    public static boolean heeftTegel(ServerPlayer p, String meester) {
        return witteLotus.vlag(p, "tegel_" + meester);
    }

    /** The five masters' tiles earned. */
    public static int meesterTegels(ServerPlayer p) {
        int n = 0;
        for (String m : MEESTERS) {
            if (heeftTegel(p, m)) {
                n++;
            }
        }
        return n;
    }

    /** All six tiles (the five and the White Lotus tile)? */
    public static boolean alleTegels(ServerPlayer p) {
        return meesterTegels(p) == MEESTERS.size() && heeftTegel(p, "witte_lotus");
    }

    /** The wing of the first master whose tile is still missing (null: none). */
    @Nullable
    static String volgendeVleugel(ServerPlayer p) {
        for (String m : MEESTERS) {
            if (!heeftTegel(p, m)) {
                return "echo_" + m;
            }
        }
        return null;
    }

    private static List<VerhaalStand.Nodig> nodigLotus(ServerPlayer p, int stap) {
        if (stap != L_TEGELS && stap != L_VOETSTUKKEN) {
            return List.of();
        }
        List<VerhaalStand.Nodig> uit = new ArrayList<>();
        for (String m : stap == L_TEGELS ? MEESTERS : TEGELS) {
            boolean ok = stap == L_TEGELS ? heeftTegel(p, m) : witteLotus.vlag(p, "voetstuk_" + m);
            uit.add(Verhaallijn.nodig("guhs:pai_sho_tegel_" + m, ok ? 1 : 0, 1));
        }
        return uit;
    }

    // =====================================================================================================================
    // the chakras and the eighth island
    // =====================================================================================================================

    /** All seven chakras opened? */
    public static boolean alleChakras(ServerPlayer p) {
        return chakra.klaar(p);
    }

    /** May this player see the path to the eighth island: the whole story done, or all tiles and all chakras (DESIGN 5)? */
    public static boolean pad8(ServerPlayer p) {
        return E2Verhaal.finaleKlaar(p) || (alleTegels(p) && alleChakras(p));
    }
}
