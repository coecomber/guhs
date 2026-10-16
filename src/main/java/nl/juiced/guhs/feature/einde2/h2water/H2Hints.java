package nl.juiced.guhs.feature.einde2.h2water;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 fix round 1, stage 2 (fix2-h2, playtest 1: "ietsjes meer hints, een duidelijkere aansluiting, en hoe ga je verder na
 * doodgaan"): what chapter 2 adds to the story's hints and continuity, on kern's {@link StapHint} API.
 * <ul>
 *   <li><b>A hint for every step 1-17</b> ({@link #registreer}): where the next part starts (a marker of the Waterstam, or a
 *       spot of the Geestenwereld), a chat line with the direction and the landmark, a soft column of light there while the
 *       step is on, and where "Verder met het verhaal" puts the player ({@code terug}). Where the place is not in view, a
 *       ~3 s look scene ({@code einde2_h2_water_kijk_<naam>}, one caption) shows it when the step starts.</li>
 *   <li><b>A look scene when you come near</b> ({@link #seconde}): a target that was too far for the look scene when its step
 *       began (the harbour, the oasis, the Golfkansel...) gets its look scene once, the first time the player comes within
 *       {@value #NABIJ} blocks.</li>
 *   <li><b>The powers</b>: when a step teaches one of the four powers, a line names it and the real key of the wheel.</li>
 *   <li><b>In the Geestenwereld</b> ({@link #geestRichting}): every few seconds the direction to Koh's cave or the
 *       Hemelvijver over the hotbar (they lie 200-300 blocks from where the soul arrives; the path lanterns lead too).</li>
 *   <li><b>Titles</b> ({@link #gehaald}): a big title and a sound when a training part, the sled, the gondola, the escape,
 *       Koh's test or the cage is done.</li>
 * </ul>
 * Texts: tools/features/einde2_h2_water_tekst.py (HINTS {@code gui.guhs.einde2_h2_water.hint.*}, KIJK the look scenes
 * {@code scene.guhs.einde2_h2_water_kijk_*}, TITELS {@code gui.guhs.einde2_h2_water.titel.*}); English in c93.
 */
public final class H2Hints {
    static final String P = "gui.guhs.einde2_h2_water.";
    static final String T = P + "hint.";
    /** The look scenes, one per hint that has one (each with its own caption): {@code einde2_h2_water_kijk_<naam>}. */
    static final String[] KIJK = {"pakguh", "zweep", "golf", "slee", "gondel", "yue", "haven", "ontsnap", "oase", "mediteer", "vis_halen",
            "vis_terug", "golf_kansel", "storm"};
    /** A target too far away for the look scene at its step's start gets it once, when the player comes this near. */
    static final double NABIJ = 56;
    /** The resume spot beside the pond of the oasis (template; the dev command's "yue_maan_kant"). */
    static final int[] OEVER = {111, 43, 26};

    private static final Map<String, Cutscene> SCENES = new ConcurrentHashMap<>();
    /** Per player: the step whose look scene was shown (or will be shown by StapHint at the step's start). */
    private static final Map<UUID, Integer> GEKEKEN = new ConcurrentHashMap<>();
    /** Per player: the last game time a Geestenwereld direction was shown over the hotbar. */
    private static final Map<UUID, Long> GEEST_BALK = new ConcurrentHashMap<>();

    private H2Hints() {
    }

    static Cutscene kijk(String naam) {
        return SCENES.computeIfAbsent(naam, n -> StapHint.kijkScene("einde2_h2_water_kijk_" + n, true));
    }

    /** A template spot of the Waterstam (its guaranteed copy). */
    static StapHint.Doel punt(int x, int y, int z) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            BlockPos o = oorsprong(level);
            return o == null ? null : o.offset(x, y, z);
        });
    }

    /** A marker of the Geestenwereld. */
    static StapHint.Doel geest(String marker) {
        return StapHint.plek(Geestenwereld.DIM, (p, level) -> Geestenwereld.wereld(marker));
    }

    @Nullable
    private static BlockPos oorsprong(ServerLevel level) {
        E2H2Water.Oord t = E2H2Water.TEST;
        if (t != null && t.level() == level) {
            return t.oorsprong();
        }
        return Plekken.bekend(level, E2H2Water.STRUCTUUR) ? Plekken.oorsprong(level, E2H2Water.STRUCTUUR) : null;
    }

    /** A marker of the Waterstam (also the copy of the tests). */
    static StapHint.Doel marker(String plek) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            BlockPos o = oorsprong(level);
            Plekken.Plek m = Plekken.plek(E2H2Water.STRUCTUUR, plek);
            return o == null || m == null ? null : o.offset(m.pos());
        });
    }

    /** From E2H2WaterFeature.register (common code: the look scenes must be known on both sides). */
    static void registreer() {
        String lijn = E2Verhaal.WATER;
        StapHint.Doel oever = punt(OEVER[0], OEVER[1], OEVER[2]);
        // 1: after the arrival scene, Pakguh on the training square (through the gate, up the grand stair)
        StapHint.naar(marker("pakguh")).tekst(T + "pakguh").kijk(kijk("pakguh")).bijStart().baken().zet(lijn, E2H2Water.PAKGUH);
        // 2: after Pakguh's scene and wink 2: the five targets on the east rim of the square; "Verder" a few blocks west of them
        StapHint.naar(marker("zweep_doel_3")).tekst(T + "zweep").kijk(kijk("zweep")).bijStart().baken().terug(punt(125, 37, 84))
                .zet(lijn, E2H2Water.ZWEEP);
        // 3: the wave pool east of the square (the jetty)
        StapHint.naar(marker("ijsbrug_a")).tekst(T + "golf").kijk(kijk("golf")).bijStart().baken().zet(lijn, E2H2Water.GOLFOEFENING);
        // 4: the islet, right in front of the jetty (no look scene: the player stands at the pool)
        StapHint.naar(marker("ijsbrug_b")).tekst(T + "ijsbrug").zonderKijk().bijStart().baken().terug(marker("ijsbrug_a"))
                .zet(lijn, E2H2Water.IJSBRUG);
        // 5: the Glijkansel on the wall west of the gate
        StapHint.naar(marker("slee_start")).tekst(T + "slee").kijk(kijk("slee")).bijStart().baken().zet(lijn, E2H2Water.SLEE);
        // 6: the gondola in the basin at the foot of the grand stair
        StapHint.naar(marker("gondel_start")).tekst(T + "gondel").kijk(kijk("gondel")).bijStart().baken().zet(lijn, E2H2Water.GONDEL);
        // 7: the palace's great door (Yue-guh is in the throne hall behind it)
        StapHint.naar(marker("paleis_poort")).tekst(T + "yue").kijk(kijk("yue")).bijStart().baken().terug(punt(112, 45, 66))
                .zet(lijn, E2H2Water.YUE);
        // 8: the harbour (the capture happens there by itself); "Verder" at the camp, just outside the harbour
        StapHint.naar(marker("sluip_start")).tekst(T + "haven").kijk(kijk("haven")).bijStart().baken().terug(marker("rustpunt_1"))
                .zet(lijn, E2H2Water.HAVEN);
        // 9: out of the brig back to the harbour; "Verder" puts the player back in the brig corridor
        StapHint.naar(marker("sluip_start")).tekst(T + "ontsnap").kijk(kijk("ontsnap")).bijStart().baken()
                .terug(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
                    BlockPos o = oorsprong(level);
                    Vec3 cel = o == null ? null : Sluipen.cel(new E2H2Water.Oord(level, o));
                    return cel == null ? null : BlockPos.containing(cel);
                })).zet(lijn, E2H2Water.ONTSNAP);
        // 10: the little door in the ice behind the palace
        StapHint.naar(marker("oase_poort")).tekst(T + "oase").kijk(kijk("oase")).bijStart().baken().terug(marker("rustpunt_4"))
                .zet(lijn, E2H2Water.MAANVIS);
        // 11: after the scene of the theft, the pond (sit anywhere in the oasis)
        StapHint.naar(marker("yue_maan")).tekst(T + "mediteer").kijk(kijk("mediteer")).bijStart().baken().terug(oever)
                .zet(lijn, E2H2Water.MEDITEER);
        // 12, 13: in the Geestenwereld (shown by Geestreis right after the arrival / Koh's test); "Verder" = beside the pond,
        //         where meditating again takes the soul straight back in
        StapHint.naar(geest("grot_ingang")).tekst(T + "koh").zonderKijk().baken().terug(oever).zet(lijn, E2H2Water.KOH);
        StapHint.naar(geest("plek_hemelvijver")).tekst(T + "hemelvijver").zonderKijk().bijStart().baken().terug(oever)
                .zet(lijn, E2H2Water.HEMELVIJVER);
        // 14: back in the body: to the harbour, the start of the sneak (the cage sparkles on the ship)
        StapHint.naar(marker("rustpunt_2")).tekst(T + "vis_halen").kijk(kijk("vis_halen")).bijStart(60).baken().zet(lijn, E2H2Water.VIS_HALEN);
        // 15: the fish back to the pond
        StapHint.naar(marker("yue_maan")).tekst(T + "vis_terug").kijk(kijk("vis_terug")).bijStart().baken().terug(marker("rustpunt_4"))
                .zet(lijn, E2H2Water.VIS_TERUG);
        // 16: the Golfkansel above the gate; "Verder" on the wall walk beside it (on the spot itself the wave would start at once)
        StapHint.naar(marker("golf_plek")).tekst(T + "golf_kansel").kijk(kijk("golf_kansel")).bijStart().baken().terug(marker("rustpunt_5"))
                .zet(lijn, E2H2Water.GOLF);
        // 17: the light in the storm (shown by E2H2Water.storm right after the scene of Zuka)
        StapHint.naar(marker("sneeuwstorm_vind")).tekst(T + "storm").kijk(kijk("storm")).baken().terug(marker("sneeuwstorm_start"))
                .zet(lijn, E2H2Water.STORM);

        E2H2WaterFeature.LIJN.opStap(H2Hints::opStap);
    }

    // =====================================================================================================================
    // the step listener: the look scenes that StapHint plays itself, the powers
    // =====================================================================================================================

    static void opStap(ServerPlayer p, int oud, int nieuw) {
        StapHint.Hint h = StapHint.hint(E2Verhaal.WATER, nieuw);
        BlockPos doel = h == null ? null : h.doel().blok(p);
        boolean speeltZelf = h != null && h.bijStartAan() && h.kijk() != null && doel != null && p.level().dimension() == h.doel().dim()
                && p.position().distanceTo(Vec3.atCenterOf(doel)) <= StapHint.KIJK_BEREIK;
        if (speeltZelf || h == null || h.kijk() == null) {
            GEKEKEN.put(p.getUUID(), nieuw);
        } else {
            GEKEKEN.remove(p.getUUID());
        }
        String kracht = switch (nieuw) {
            case E2H2Water.ZWEEP -> E2H2WaterKrachten.ZWEEP;
            case E2H2Water.GOLFOEFENING -> E2H2WaterKrachten.GOLF;
            case E2H2Water.IJSBRUG -> E2H2WaterKrachten.IJSBRUG;
            case E2H2Water.GONDEL -> E2H2WaterKrachten.STROOM;
            default -> null;
        };
        if (kracht != null) {
            // after the step's hint (its look scene and its chat line)
            naScene(p, 40 + StapHint.KIJK_DUUR, s -> {
                if (E2H2Water.lijn().stap(s) == nieuw) {
                    leer(s, kracht);
                }
            });
        }
    }

    /** "New power: X! Hold [key] for your power wheel and pick it; then tap [key] to use it." */
    static void leer(ServerPlayer p, String kracht) {
        Component toets = Component.keybind("key.guhs.einde2_kracht").withStyle(ChatFormatting.YELLOW);
        p.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable("quest.guhs.einde2_h2_water.kracht.uitleg",
                        Component.translatable(P + "kracht." + kracht).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), toets)
                        .withStyle(ChatFormatting.AQUA)));
    }

    /** Runs a job for the player in so many ticks, or later when they are watching a scene then (at most ~30 s on). */
    static void naScene(ServerPlayer p, int ticks, Consumer<ServerPlayer> wat) {
        naScene(p, ticks, wat, 30);
    }

    private static void naScene(ServerPlayer p, int ticks, Consumer<ServerPlayer> wat, int nog) {
        StapHint.later(p, ticks, s -> {
            if (Cutscenes.bezig(s) && nog > 0) {
                naScene(s, 20, wat, nog - 1);
            } else {
                wat.accept(s);
            }
        });
    }

    // =====================================================================================================================
    // once a second inside the Waterstam: the look scene of a far target when the player comes near
    // =====================================================================================================================

    static void seconde(ServerPlayer p, int stap) {
        Integer gekeken = GEKEKEN.get(p.getUUID());
        if (gekeken != null && gekeken == stap) {
            return;
        }
        StapHint.Hint h = StapHint.hint(E2Verhaal.WATER, stap);
        Cutscene scene = h == null ? null : h.kijk();
        if (scene == null || p.level().dimension() != h.doel().dim() || Cutscenes.bezig(p) || p.getVehicle() != null) {
            return;
        }
        BlockPos doel = h.doel().blok(p);
        if (doel == null || p.position().distanceTo(Vec3.atCenterOf(doel)) > NABIJ) {
            return;
        }
        GEKEKEN.put(p.getUUID(), stap);
        StapHint.speelKijk(p, scene, doel, null);
    }

    // =====================================================================================================================
    // the Geestenwereld: the direction over the hotbar
    // =====================================================================================================================

    /** Every few seconds while the player walks to Koh's cave or the Hemelvijver: "➜ Koh's grot: in het noordwesten, ver weg". */
    static void geestRichting(ServerPlayer p, String marker, String naamKey) {
        BlockPos b = Geestenwereld.wereld(marker);
        if (b == null || Cutscenes.bezig(p)) {
            return;
        }
        long nu = p.level().getGameTime();
        Long vorig = GEEST_BALK.get(p.getUUID());
        if (vorig != null && nu - vorig < 120 && nu >= vorig) {
            return;
        }
        if (b.distToCenterSqr(p.position()) < 20 * 20) {
            return;
        }
        GEEST_BALK.put(p.getUUID(), nu);
        p.sendOverlayMessage(Component.translatable(P + "hint.geest_balk", Component.translatable(naamKey), Richting.van(p.position(), Vec3.atCenterOf(b)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // a part done: a title and a sound
    // =====================================================================================================================

    /** A big title ({@code gui.guhs.einde2_h2_water.titel.<naam>} + {@code .onder}) and a cheerful sound. */
    static void gehaald(ServerPlayer p, String naam) {
        if (p.connection != null) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 14));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(P + "titel." + naam + ".onder").withStyle(ChatFormatting.AQUA)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(P + "titel." + naam).withStyle(ChatFormatting.GOLD)));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    /** (logout, wipe) forget what this class keeps for the player. */
    static void vergeet(UUID speler) {
        GEKEKEN.remove(speler);
        GEEST_BALK.remove(speler);
    }
}
