package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * einde2 fix round 1, stage 2 (fix2-h3, playtest 1: "ietsjes meer hints, een iets duidelijkere aansluiting, en duidelijk hoe
 * je verder kunt gaan na doodgaan"): what chapter 3 adds to the story's hints and continuity.
 * <ul>
 *   <li>a {@link StapHint} for every step after the arrival ({@value E2H3Aarde#LOKET} .. {@value E2H3Aarde#TOPH}): the
 *       target (a marker of the city), a chat line with the direction and a landmark, a light column, a ~3 s look scene
 *       with one caption where the place is outdoors (inside the tea house only the line and the column), and a resume
 *       spot for "Verder met het verhaal". Step {@value E2H3Aarde#BLINDDOEK} follows the lesson: Toph-guh in the ring,
 *       then (flag {@link E2H3Aarde#V_BAAN}) the opening of her course, with a look scene of its own ({@link #baan});</li>
 *   <li>the way in between: when a step starts, a team member says why we go there next ({@link #TEAM_LIJNEN}, only a
 *       member who is in the player's {@link E2Team}), and then the hint shows;</li>
 *   <li>far targets: a look scene can only play within {@link StapHint#KIJK_BEREIK} blocks, and most steps of the city
 *       start further away. So when the player was far from the target during the step and then comes within
 *       {@link #NABIJ} blocks, the hint (with its look scene) shows once more, once per step;</li>
 *   <li>under the ground, where a compass direction is no help: a trail of small lights along the corridor ahead of you
 *       (the base under Meer Laogai to Appa's bars, the catacombs down to the crystal chamber) and the direction in the
 *       action bar;</li>
 *   <li>the team at the campfire matches the story: Katara-guh is not at the camp while the Dai Li hold her (step
 *       {@value E2H3Aarde#VAL}; {@link E2Team#AFWEZIG}).</li>
 * </ul>
 * Texts: tools/features/einde2_h3_aarde_tekst.py (HINTS {@code gui.guhs.einde2_h3_aarde.hint.*}, KIJK the look scenes
 * {@code scene.guhs.einde2_h3_aarde_kijk_*}, TEAM {@code gui.guhs.einde2_h3_aarde.team.*}, TITELS).
 */
public final class H3Hints {
    static final String T = "gui.guhs.einde2_h3_aarde.hint.", G = "gui.guhs.einde2_h3_aarde.";
    /** The look scenes, one per hint with its own caption: {@code einde2_h3_aarde_kijk_<naam>}. */
    static final String[] KIJK = {"loket", "aardrommel", "baan", "monorail", "theehuis", "joo_dee", "kamp", "koning", "laogai", "lu_ten", "val", "toph"};
    /** A target this far away (blocks) during its step counts as "far"; coming within {@link #NABIJ} shows the hint again. */
    static final double VER = 90, NABIJ = 48, OP_DOEL = 12;
    /** Ticks after a step started before the team member speaks, and from that line to the hint. */
    static final int TEAM_WACHT = 20, NA_TEAM = 50;

    private static final Map<String, Cutscene> SCENES = new ConcurrentHashMap<>();
    /** The player was far from the target of this step (step number). */
    private static final Map<UUID, Integer> WAS_VER = new ConcurrentHashMap<>();

    /** A team line at the start of a step: the lang key's name and who says it. */
    record TeamLijn(String key, E2Team.Lid wie) {
    }

    static final Map<Integer, TeamLijn> TEAM_LIJNEN = new ConcurrentHashMap<>();

    private H3Hints() {
    }

    private static Cutscene kijk(String naam) {
        return SCENES.computeIfAbsent(naam, n -> StapHint.kijkScene("einde2_h3_aarde_kijk_" + n, true));
    }

    private static void team(String key, String wie, int stap) {
        TEAM_LIJNEN.put(stap, new TeamLijn(key, E2Team.Lid.van(wie)));
    }

    // =====================================================================================================================
    // targets in the city
    // =====================================================================================================================

    /** A marker of the copy of Ba Sing Guh in the Guheinde (the guaranteed copy, or a test's own spot). */
    static StapHint.Doel m(String marker) {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(level);
            return stad == null ? null : stad.pos(marker);
        });
    }

    /** The middle of the north side of a zone (where you walk in from the avenue: the tea house's terrace). */
    @Nullable
    static BlockPos noordRand(E2H3Aarde.Stad stad, String zone) {
        Plekken.Plek z = stad.plek(zone);
        if (z == null || z.tot() == null) {
            return null;
        }
        BlockPos a = z.pos(), b = z.tot();
        return stad.o().offset((a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    /** The opening in the west wall of Toph's course (the wall has a gap of three at its middle, facing the arena). */
    @Nullable
    static BlockPos baanIngang(E2H3Aarde.Stad stad) {
        Plekken.Plek z = stad.plek("blinddoek_baan");
        if (z == null) {
            return null;
        }
        BlockPos a = z.pos();
        return stad.o().offset(a.getX() - 1, a.getY(), a.getZ() + 5);
    }

    /** Step 3: Toph-guh in the ring, and once she sent you there, the opening of her course. */
    static StapHint.Doel tophOfBaan() {
        return StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(level);
            if (stad == null) {
                return null;
            }
            return E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN) ? baanIngang(stad) : stad.pos("toph");
        });
    }

    // =====================================================================================================================
    // registration (common code: the look scenes must be known on both sides)
    // =====================================================================================================================

    /** From E2H3AardeFeature.register. */
    static void registreer() {
        for (String naam : KIJK) {
            kijk(naam);
        }
        String lijn = E2H3Aarde.LIJN.id();
        // 1: after the arrival scene, the desk in the gate
        StapHint.naar(m("loket")).tekst(T + "loket").kijk(kijk("loket")).bijStart().baken().zet(lijn, E2H3Aarde.LOKET);
        // 2: after the loket scene, the arena (its entrance, where the Aardrommel scene stands you)
        StapHint.naar(m("rustpunt_2")).tekst(T + "aardrommel").kijk(kijk("aardrommel")).baken().zet(lijn, E2H3Aarde.AARDROMMEL);
        // 3: Toph-guh in the ring, then her course (the step's own nu text follows the flag: nu.3 / nu.3_baan)
        StapHint.naar(tophOfBaan()).bijStart().baken().zet(lijn, E2H3Aarde.BLINDDOEK);
        // 4: the car on the outer wall; "Verder" puts you on the platform next to it
        StapHint.naar(m("monorail_wagen")).tekst(T + "monorail").kijk(kijk("monorail")).baken().terug(m("station_buiten"))
                .zet(lijn, E2H3Aarde.MONORAIL);
        // 5: after the ride (and the Sjokkel wink), the tea house's terrace below station Middenring
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(level);
            return stad == null ? null : noordRand(stad, "theehuis");
        })).tekst(T + "theehuis").kijk(kijk("theehuis")).baken().zet(lijn, E2H3Aarde.THEEHUIS);
        // 6, 7: inside the tea house (no look scene: the camera would only see the roof); "Verder" puts you in the tea room
        StapHint.naar(m("theeketel")).tekst(T + "thee").zonderKijk().bijStart(50).baken().terug(m("zuka_ober")).zet(lijn, E2H3Aarde.THEE);
        StapHint.naar(m("miroh")).tekst(T + "pai_sho").zonderKijk().baken().terug(m("zuka_ober")).zet(lijn, E2H3Aarde.PAI_SHO);
        // 8 .. 14
        StapHint.naar(m("joo_dee")).tekst(T + "joo_dee").kijk(kijk("joo_dee")).baken().zet(lijn, E2H3Aarde.JOO_DEE);
        StapHint.naar(m("appa_kamp")).tekst(T + "kamp").kijk(kijk("kamp")).baken().zet(lijn, E2H3Aarde.KAMP);
        StapHint.naar(m("aardekoning")).tekst(T + "koning").kijk(kijk("koning")).baken().zet(lijn, E2H3Aarde.KONING);
        StapHint.naar(m("basis_ingang")).tekst(T + "laogai").kijk(kijk("laogai")).baken().zet(lijn, E2H3Aarde.LAOGAI);
        StapHint.naar(m("lu_ten_boom")).tekst(T + "lu_ten").kijk(kijk("lu_ten")).baken().zet(lijn, E2H3Aarde.LU_TEN);
        StapHint.naar(m("catacomben_ingang")).tekst(T + "val").kijk(kijk("val")).baken().zet(lijn, E2H3Aarde.VAL);
        StapHint.naar(m("team_toph")).tekst(T + "toph").kijk(kijk("toph")).bijStart(40).baken().zet(lijn, E2H3Aarde.TOPH);

        // the way in between: a team member says why we go there next, then the hint (steps without bijStart above)
        team("aardrommel", "sokka", E2H3Aarde.AARDROMMEL);
        team("monorail", "sokka", E2H3Aarde.MONORAIL);
        team("theehuis", "aang", E2H3Aarde.THEEHUIS);
        team("pai_sho", "aang", E2H3Aarde.PAI_SHO);
        team("joo_dee", "katara", E2H3Aarde.JOO_DEE);
        team("kamp", "sokka", E2H3Aarde.KAMP);
        team("koning", "sokka", E2H3Aarde.KONING);
        team("laogai", "katara", E2H3Aarde.LAOGAI);
        team("lu_ten", "aang", E2H3Aarde.LU_TEN);
        team("val", "sokka", E2H3Aarde.VAL);
        E2H3Aarde.LIJN.opStap((p, oud, nieuw) -> {
            if (TEAM_LIJNEN.containsKey(nieuw)) {
                StapHint.later(p, TEAM_WACHT, s -> aansluiting(s, nieuw, 0));
            }
        });

        // Katara-guh is not at the campfire while the Dai Li hold her
        E2Team.AFWEZIG.add((p, structuur, lid) -> lid == E2Team.Lid.KATARA && E2H3Aarde.STRUCTUUR.equals(structuur)
                && E2H3Aarde.stap(p) == E2H3Aarde.VAL);

        NeoForge.EVENT_BUS.addListener(H3Hints::opTick);
    }

    /** The team line of a step (when the player is free: not in a scene or a talk), then the step's hint. */
    static void aansluiting(ServerPlayer p, int stap, int poging) {
        if (E2H3Aarde.stap(p) != stap) {
            return;
        }
        if ((Cutscenes.bezig(p) || Praat.bezig(p)) && poging < 60) {
            StapHint.later(p, 10, s -> aansluiting(s, stap, poging + 1));
            return;
        }
        TeamLijn t = TEAM_LIJNEN.get(stap);
        if (t != null && t.wie() != null && E2Team.leden(p).contains(t.wie())) {
            zeg(p, t.wie(), Component.translatable(G + "team." + t.key()));
            StapHint.later(p, NA_TEAM, s -> {
                if (E2H3Aarde.stap(s) == stap) {
                    StapHint.toon(s, E2H3Aarde.LIJN.id());
                }
            });
        } else {
            StapHint.toon(p, E2H3Aarde.LIJN.id());
        }
    }

    /** "&lt;Sokka-guh&gt; text" in the chat (a member of the team; they walk with you, so their voice is near). */
    static void zeg(ServerPlayer p, E2Team.Lid wie, Component tekst) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc." + wie.basis().id())).append("> ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(tekst.copy().withStyle(ChatFormatting.WHITE)));
    }

    /** Toph told you to shut your eyes (flag {@link E2H3Aarde#V_BAAN}): a look at her course, then the line. */
    static void baan(ServerPlayer p) {
        StapHint.later(p, 10, s -> {
            E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(s.level());
            BlockPos ingang = stad == null ? null : baanIngang(stad);
            if (ingang == null || E2H3Aarde.stap(s) != E2H3Aarde.BLINDDOEK) {
                return;
            }
            java.util.function.Consumer<ServerPlayer> regel = s2 -> s2.sendSystemMessage(Component.literal("➜ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(T + "baan", Richting.van(s2.getEyePosition(), Vec3.atCenterOf(ingang))).withStyle(ChatFormatting.YELLOW)));
            if (!StapHint.speelKijk(s, kijk("baan"), ingang, regel)) {
                regel.accept(s);
            }
        });
    }

    // =====================================================================================================================
    // every 10 ticks: far targets, the trails under the ground
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + 7) % 10 != 0 || p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad == null || !stad.doos(H3Verloop.BEREIK).contains(p.position()) || !H3Verloop.open.test(p)) {
            return;
        }
        tik(p, stad);
    }

    /** (also from the game tests) what this player is shown now. */
    static void tik(ServerPlayer p, E2H3Aarde.Stad stad) {
        int stap = E2H3Aarde.stap(p);
        if (stap >= E2H3Aarde.STAPPEN) {
            WAS_VER.remove(p.getUUID());
            return;
        }
        nabij(p, stap);
        if (stap == E2H3Aarde.LAOGAI && stad.in(p, "basis", 1) && !H3Monorail.rijdt(p)) {
            List<Vec3> pad = new ArrayList<>();
            voeg(pad, stad.voet("rustpunt_3"));
            pad.addAll(stad.pad("sluip_route"));
            voeg(pad, stad.voet("appa_poort"));
            spoor(p, pad, stad.voet("appa_poort"), "pad.appa");
        } else if (stap == E2H3Aarde.VAL && stad.in(p, "catacomben", 0) && !stad.in(p, "kristalkamer", 0)) {
            List<Vec3> pad = new ArrayList<>();
            for (int[] t : CATACOMBEN) {
                pad.add(Vec3.atBottomCenterOf(stad.o().offset(t[0], t[1], t[2])));
            }
            spoor(p, pad, stad.voet("bliksem_speler"), "pad.kristal");
        }
    }

    /**
     * The corridor of the catacombs from the foot of the old gate's stair (rustpunt_4) down to the crystal chamber, as the
     * build carves it (tools/features/einde2_bouw_aarde_onder.py catacomben(): east, south five down, west five down, north
     * three down, east into the chamber), template coordinates of the floor.
     */
    static final int[][] CATACOMBEN = {{163, 30, 104}, {176, 30, 104}, {176, 25, 109}, {176, 25, 149}, {171, 20, 149}, {128, 20, 149},
            {128, 17, 146}, {128, 17, 128}, {137, 17, 128}, {149, 17, 127}};

    private static void voeg(List<Vec3> pad, @Nullable Vec3 v) {
        if (v != null) {
            pad.add(v);
        }
    }

    /** Far from this step's target, and now near again: its hint once more (with the look scene), once per step. */
    private static void nabij(ServerPlayer p, int stap) {
        StapHint.Hint h = StapHint.hint(E2H3Aarde.LIJN.id(), stap);
        BlockPos doel = h == null ? null : h.doel().blok(p);
        if (doel == null) {
            return;
        }
        double d = p.position().distanceTo(Vec3.atCenterOf(doel));
        Integer ver = WAS_VER.get(p.getUUID());
        if (d > VER) {
            WAS_VER.put(p.getUUID(), stap);
        } else if (ver != null && ver == stap && d < NABIJ && d > OP_DOEL && H3Verloop.vrij(p)) {
            WAS_VER.remove(p.getUUID());
            if (E2H3Aarde.LIJN.eenmalig(p, "hint_nabij_" + stap)) {
                StapHint.toon(p, E2H3Aarde.LIJN.id());
            }
        }
    }

    private static final DustParticleOptions SPOOR = new DustParticleOptions(0x9CF09C, 1.0f);

    /**
     * Small green lights on the floor of the corridor ahead of the player (2 to 10 blocks along the path, from the point of
     * the path nearest to them), and every four seconds the direction to the goal in the action bar.
     */
    static void spoor(ServerPlayer p, List<Vec3> pad, @Nullable Vec3 doel, String balk) {
        if (pad.size() < 2) {
            return;
        }
        double[] tot = new double[pad.size()];
        for (int i = 1; i < pad.size(); i++) {
            tot[i] = tot[i - 1] + pad.get(i).distanceTo(pad.get(i - 1));
        }
        double best = Double.MAX_VALUE, s = 0;
        Vec3 hier = p.position();
        for (int i = 1; i < pad.size(); i++) {
            Vec3 a = pad.get(i - 1), b = pad.get(i), ab = b.subtract(a);
            double len2 = Math.max(1e-6, ab.lengthSqr());
            double f = Math.max(0, Math.min(1, hier.subtract(a).dot(ab) / len2));
            double dd = a.add(ab.scale(f)).distanceToSqr(hier);
            if (dd < best) {
                best = dd;
                s = tot[i - 1] + f * Math.sqrt(len2);
            }
        }
        for (double v = 2; v <= 10; v += 2) {
            Vec3 punt = op(pad, tot, s + v);
            if (punt != null) {
                Schijn.deeltjes(p, SPOOR, punt.add(0, 0.35, 0), 1, 0.05, 0);
            }
        }
        if (doel != null && p.tickCount % 80 < 10) {
            p.sendOverlayMessage(Component.translatable(G + balk, Richting.van(p.position(), doel)).withStyle(ChatFormatting.GREEN));
        }
    }

    @Nullable
    private static Vec3 op(List<Vec3> pad, double[] tot, double afstand) {
        if (afstand > tot[tot.length - 1]) {
            return null;
        }
        for (int i = 1; i < pad.size(); i++) {
            if (afstand <= tot[i]) {
                double f = (afstand - tot[i - 1]) / Math.max(1e-6, tot[i] - tot[i - 1]);
                return pad.get(i - 1).lerp(pad.get(i), f);
            }
        }
        return null;
    }

    // =====================================================================================================================
    // small helpers for the games
    // =====================================================================================================================

    /** A big title (and an optional subtitle) for this player only: the start and the end of a game. */
    static void titel(ServerPlayer p, Component titel, @Nullable Component onder, int blijf) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(4, blijf, 8));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(onder == null ? Component.empty() : onder));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(titel));
    }

    /** A title with a sound: something was done. */
    static void gelukt(ServerPlayer p, String titel, @Nullable Component onder, SoundEvent geluid) {
        titel(p, Component.translatable(G + titel).withStyle(ChatFormatting.GOLD), onder, 60);
        Schijn.geluid(p, geluid, p.blockPosition(), 0.9f, 1.1f);
    }

    /** (dev, tests) forgets what this class remembers about the player. */
    static void vergeet(ServerPlayer p) {
        WAS_VER.remove(p.getUUID());
    }

    /** (tests) the team line of a step: "wie/key", or null. */
    @Nullable
    static String teamLijn(int stap) {
        TeamLijn t = TEAM_LIJNEN.get(stap);
        return t == null || t.wie() == null ? null : t.wie().id() + "/" + t.key();
    }
}
