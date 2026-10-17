package nl.juiced.guhs.feature.einde2.geestspel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: two focused tests of its state machines (CONTRACT_E2_B 8).
 * <ul>
 *   <li>{@link #einde2GeestSpelToegang}: the zone rule of 2.3 as a table, this slice's own areas (the archive after the
 *       reiger, the eighth island after all tiles and chakras), and the guest rule (a guest has the zones of their guide
 *       only while both are inside, and nothing of their own changes);</li>
 *   <li>{@link #einde2GeestSpelLotus}: the trials and tiles (Bumi-guh's wrong and right answers, Oom Miroh's cards and teapots,
 *       a parry inside and outside the beat), the lanterns counting home, and the five scenes with their lengths.</li>
 * </ul>
 */
public class E2GeestSpelGameTests {
    private static final String BATCH = "einde2_geest_spel";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void wis(ServerPlayer p) {
        for (String l : GeestLijnen.ALLE) {
            GeestLijnen.lijn(l).wis(p);
        }
        for (String k : java.util.List.copyOf(GuhQuests.saved(p).keySet())) {
            if (k.startsWith(GeestSpel.DATA)) {
                GuhQuests.saved(p).remove(k);
            }
        }
        GeestSpel.weg(p);
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2GeestSpelToegang(GameTestHelper helper) {
        // the table of 2.3: (zone, geest, lotus, finale, eigen)
        helper.assertTrue(!GeestToegang.regel("meditatiecirkel", false, false, false, false) && GeestToegang.regel("koh_grot", true, false, false, false)
                && GeestToegang.regel("hemelvijver", true, false, false, false) && !GeestToegang.regel("lotus_ruines", true, false, false, false)
                && !GeestToegang.regel("gefluisterwoud", true, false, false, false), "flag geest: the arrival, Koh, the Hemelvijver; not the lotus, not the wood");
        helper.assertTrue(GeestToegang.regel("lotus_ruines", false, true, false, false) && GeestToegang.regel("top_voorgangers", false, true, false, false)
                && GeestToegang.regel("fang_pad", false, true, false, false) && GeestToegang.regel("koh_grot", false, true, false, false)
                && !GeestToegang.regel("theepaviljoen", false, true, false, false), "flag witte_lotus: the lotus, the Top, Fang's path (and the first zones)");
        helper.assertTrue(GeestToegang.regel("gefluisterwoud", false, false, true, false) && GeestToegang.regel("theepaviljoen", false, false, true, false)
                && !GeestToegang.regel(GeestToegang.EILAND_8, true, true, true, false) && !GeestToegang.regel(GeestToegang.ARCHIEF, true, true, true, false)
                && GeestToegang.regel(GeestToegang.ARCHIEF, false, false, false, true), "the finale: every zone; the archive and the island only by their own rule");
        helper.assertTrue(Geestenwereld.LAGEN.containsAll(java.util.List.of(GeestLagen.WOUD, GeestLagen.BOOM, GeestLagen.PAD_8)),
                "the three overlays are named as the build names them");
        ServerPlayer gids = speler(helper), gast = speler(helper);
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        Geestenwereld.TestGebied gebied = new Geestenwereld.TestGebied(helper.getLevel().dimension(), new AABB(o).inflate(40), Vec3.atBottomCenterOf(o), -64);
        try {
            wis(gids);
            wis(gast);
            // a player's own areas: the archive after the reiger, the path after all tiles and chakras
            helper.assertTrue(!GeestToegang.zoneOpen(gids, GeestToegang.ARCHIEF) && !GeestLijnen.pad8(gids), "a new player: neither");
            GeestLijnen.witteLotus().zet(gids, GeestLijnen.L_ARCHIEF);
            helper.assertTrue(GeestToegang.zoneOpen(gids, GeestToegang.ARCHIEF), "after the reiger: the archive");
            for (String m : GeestLijnen.TEGELS) {
                GeestLijnen.witteLotus().vlag(gids, "tegel_" + m, true);
            }
            helper.assertTrue(GeestLijnen.alleTegels(gids) && !GeestLijnen.pad8(gids), "all tiles, no chakras: no path yet");
            GeestLijnen.chakra().zet(gids, GeestLijnen.CHAKRAS_N);
            helper.assertTrue(GeestLijnen.pad8(gids) && GeestToegang.zoneOpen(gids, GeestToegang.EILAND_8) && GeestLagen.ziet(gids, GeestLagen.PAD_8),
                    "all tiles and chakras: the path to the eighth island");
            // the guest rule: only while both are inside
            Geestenwereld.testGebied(gebied);
            gids.teleportTo(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5);
            gast.teleportTo(o.getX() + 2.5, o.getY() + 1, o.getZ() + 0.5);
            helper.assertTrue(Geestenwereld.in(gids) && Geestenwereld.in(gast) && !GeestToegang.zoneOpen(gast, GeestToegang.ARCHIEF), "the guest alone: closed");
            GeestToegang.zetGast(gast, gids);
            helper.assertTrue(GeestToegang.gids(gast) == gids && GeestToegang.zoneOpen(gast, GeestToegang.ARCHIEF)
                    && GeestToegang.zoneOpen(gast, GeestToegang.EILAND_8) && GeestToegang.gastenBij(gids).contains(gast)
                    && GeestLijnen.witteLotus().stap(gast) == 0 && !GeestLijnen.pad8(gast), "a guest: the guide's zones, nothing of their own");
            Geestenwereld.testGebiedWeg(gebied);
            helper.assertTrue(GeestToegang.gids(gast) == null && !GeestToegang.zoneOpen(gast, GeestToegang.ARCHIEF), "outside: a guest no more");
        } finally {
            Geestenwereld.testGebiedWeg(gebied);
            GeestToegang.zetGast(gast, null);
            wis(gids);
            wis(gast);
            helper.getLevel().removePlayerImmediately(gids, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(gast, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2GeestSpelLotus(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            wis(p);
            GeestLijnen.witteLotus().zet(p, GeestLijnen.L_TEGELS);
            // Bumi-guh: the obvious answer turns you back, the mad one on; the third room gives the tile
            Proeven.testBumi(p, 1);
            Proeven.testBumiAntwoord(p, 1);
            helper.assertTrue(Proeven.bumiKamer(p) == 1 && !GeestLijnen.heeftTegel(p, "bumi"), "Bumi: a wrong answer keeps you in the room");
            for (int kamer = 1; kamer <= 3; kamer++) {
                Proeven.testBumiAntwoord(p, Proeven.BUMI_GOED[kamer - 1]);
            }
            helper.assertTrue(Proeven.bumiKamer(p) == null && GeestLijnen.heeftTegel(p, "bumi") && GuhQuests.count(p, E2GeestSpelFeature.tegel("bumi")) == 1,
                    "Bumi: three right answers, the tile");
            // Oom Miroh: nonsense keeps the round, the real ending or the twist go to the tea; the wrong pot is only a face
            for (int gast = 0; gast < 3; gast++) {
                int onzin = 0, goed = 0;
                for (int i = 0; i < 3; i++) {
                    if (Proeven.KAART[gast][i] == 2) {
                        onzin = i + 1;
                    } else {
                        goed = i + 1;
                    }
                }
                Proeven.testMiroh(p, onzin);
                Proeven.testThee(p, Proeven.THEE[gast]);
                helper.assertTrue(!GeestLijnen.heeftTegel(p, "miroh"), "Miroh: nonsense, so no tea yet (guest " + gast + ")");
                Proeven.testMiroh(p, goed);
                Proeven.testThee(p, Proeven.THEE[gast] % 3 + 1);
                Proeven.testThee(p, Proeven.THEE[gast]);
            }
            helper.assertTrue(GeestLijnen.heeftTegel(p, "miroh") && GeestLijnen.meesterTegels(p) == 2, "Miroh: three guests served, the tile");
            // Piandao-guh: a click on the blow is a parry, a click between blows is off the beat
            long nu = helper.getLevel().getGameTime();
            int beat = Proeven.beatTicks();
            Proeven.testDuel(p, nu - (long) Proeven.SLAGEN[0] * beat - 2);
            Proeven.pareer(p);
            Proeven.Duel d = Proeven.duel(p);
            helper.assertTrue(beat == 15 && d != null && d.gepareerd() == 1 && d.uitDeMaat == 0, "Piandao: a parry two ticks late counts");
            Proeven.testDuel(p, nu - (long) Proeven.SLAGEN[0] * beat - beat / 2 - Proeven.VENSTER);
            Proeven.pareer(p);
            d = Proeven.duel(p);
            helper.assertTrue(d != null && d.gepareerd() == 0 && d.uitDeMaat == 1, "Piandao: a click between blows is off the beat");
            Proeven.stop(p);
            // the tiles of the other three and the White Lotus: the line moves on by itself only where its tick says so
            for (String m : new String[] {"pakguh", "piandao", "jeong_jeong"}) {
                Proeven.tegel(p, m);
            }
            helper.assertTrue(GeestLijnen.meesterTegels(p) == 5 && !GeestLijnen.alleTegels(p), "five master tiles, not the sixth");
            // the lanterns: each one home is a step; all eight finish the line
            for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
                Lantaarns.testVolg(p, n);
                Lantaarns.testBreng(p, n);
            }
            helper.assertTrue(GeestLijnen.lantaarns().klaar(p) && Lantaarns.volgers(p).isEmpty() && GeestLagen.ziet(p, GeestLagen.BOOM)
                    && !GeestLagen.ziet(p, GeestLagen.WOUD), "eight lanterns home: the line done, the tree lit, the wood not (yet)");
            // the scenes: registered, with the lengths of CONTRACT_E2_B 5 (fix4-lotus: + Pakguh's explanation, 15-25 s)
            double[][] lengte = {{45, 75}, {45, 75}, {15, 25}, {10, 15}, {10, 15}, {15, 25}};
            for (int i = 0; i < GeestScenes.ALLE.size(); i++) {
                String id = GeestScenes.ALLE.get(i);
                Cutscene s = Cutscene.van(id);
                double sec = GeestScenes.seconden(id);
                helper.assertTrue(s != null && s.lijn() != null && sec >= lengte[i][0] && sec <= lengte[i][1], id + ": " + sec + " s");
            }
        } finally {
            wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /**
     * fix round 1 (fix2-geestzij): every step of the Geestenwereld lines and of the side lines that needs a hint has one, its
     * look scene is the short 3.2 s kind, no Geestenwereld hint has a resume spot (no "Verder" teleport into the spirit
     * world), and Jeong Jeong-guh's too-fast flame only puts out the newest candle.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2Fix2GeestZijHints(GameTestHelper helper) {
        java.util.Map<String, int[]> nodig = new java.util.LinkedHashMap<>();
        nodig.put(GeestLijnen.WITTE_LOTUS, new int[]{0, 1, 2, 3, 4, 5});
        nodig.put(GeestLijnen.LANTAARNS, new int[]{0, 1, 2, 3, 4, 5, 6, 7});
        nodig.put(GeestLijnen.HEI_BAI, new int[]{0, 1});
        nodig.put(GeestLijnen.KOH, new int[]{0, 1, 2});
        nodig.put(GeestLijnen.CHAKRA, new int[]{0, 1, 2, 3, 4, 5, 6});
        nodig.put(GeestLijnen.WAN, new int[]{1, 2});
        nodig.put("omashguh", new int[]{0, 1, 2, 3, 4});
        nodig.put("westelijke_luchttempel", new int[]{0, 1, 2, 3});
        nodig.put("vuurtempel_roku", new int[]{0, 1, 2, 3});
        nodig.put("kyoshi_dorp", new int[]{0, 1, 2, 3});
        nodig.put("bibliotheek_wan_shi_guh", new int[]{0, 1, 2, 3});
        nodig.put("leeuwschildpad", new int[]{0, 1, 2});
        nodig.put("mika_kok", new int[]{0, 2, 3, 4});
        nodig.put("vetbootje", new int[]{1, 2});
        ServerPlayer p = speler(helper);
        try {
            for (var e : nodig.entrySet()) {
                for (int stap : e.getValue()) {
                    var h = nl.juiced.guhs.feature.einde2.verhaal.StapHint.hint(e.getKey(), stap);
                    helper.assertTrue(h != null && h.tekst() != null, "a hint with a text for " + e.getKey() + "/" + stap);
                    Cutscene k = h.kijk();
                    helper.assertTrue(k == null || k.duur() == nl.juiced.guhs.feature.einde2.verhaal.StapHint.KIJK_DUUR,
                            "a short look scene for " + e.getKey() + "/" + stap);
                    if (e.getKey().startsWith("geest_")) {
                        helper.assertTrue(h.doel().dim() == Geestenwereld.DIM && h.terugDoel().blok(p) == null,
                                "a Geestenwereld hint without a resume spot: " + e.getKey() + "/" + stap);
                    }
                }
            }
            helper.assertTrue(nl.juiced.guhs.feature.einde2.verhaal.StapHint.hint(GeestLijnen.VERSCHIJNINGEN, 0) == null,
                    "the rare sightings (random) have no hint");
            // Jeong Jeong-guh: two calm candles, then one too fast: only the newest goes out
            Proeven.testKaars(p, 1);
            Proeven.testKalm(p);
            Proeven.testKaars(p, 2);
            Proeven.testKaars(p, 3);
            helper.assertTrue(Proeven.kaarsenAan(p).equals(java.util.Set.of(1)), "too fast: one step back, not all out: " + Proeven.kaarsenAan(p));
            helper.assertTrue(Proeven.NODIG == 8 && Proeven.VENSTER == 6 && Proeven.WILD == 40, "the forgiving duel and candle numbers");
        } finally {
            wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /**
     * fix4-lotus (playtest 3): the two blockers in the trials and the mirrors made understandable.
     * <ul>
     *   <li>a talking-screen page whose speaker stands far away (Bumi-guh's echo, seen from room 2 and 3; Pakguh's echo,
     *       seen from the necklace) is sent without that speaker: the screen closed itself past 12 blocks;</li>
     *   <li>Pakguh's mirrors: a mirror the light has not reached does not turn, a right one stays right (one click broke
     *       the chain before), clicks that do not help count, and after the hint Pakguh turns the mirror himself; clicking
     *       the glinting mirror solves it.</li>
     * </ul>
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2Fix4LotusProeven(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        net.minecraft.world.entity.decoration.ArmorStand ver = null, bij = null;
        try {
            wis(p);
            Vec3 o = p.position();
            ver = new net.minecraft.world.entity.decoration.ArmorStand(helper.getLevel(), o.x + 20, o.y, o.z);
            bij = new net.minecraft.world.entity.decoration.ArmorStand(helper.getLevel(), o.x + 3, o.y, o.z);
            helper.getLevel().addFreshEntity(ver);
            helper.getLevel().addFreshEntity(bij);
            var uit = GeestSpel.dichtbij(p, java.util.List.of(GeestSpel.regel(ver, "entity.guhs.guh_npc.bumi_guh_echo", "a"),
                    GeestSpel.regel(bij, "entity.guhs.guh_npc.bumi_guh_echo", "b")));
            helper.assertTrue(uit.get(0).spreker() == null && !uit.get(0).naamKey().isEmpty(), "a far speaker: its page without it, with its name");
            helper.assertTrue(uit.get(1).spreker() == bij, "a near speaker stays on its page");
            // Pakguh's mirrors
            java.util.List<String> ids = Proeven.spiegelIds();
            helper.assertTrue(ids.size() >= 3, "the mirrors of the build: " + ids);
            Proeven.testPakguh(p);
            int beurt = Proeven.bereik(Proeven.testStanden(p), ids);
            helper.assertTrue(beurt == 0, "at the start no mirror is right (the first one has to be turned)");
            int[] voor = Proeven.testStanden(p);
            Proeven.testSpiegel(p, 3);
            helper.assertTrue(java.util.Arrays.equals(voor, Proeven.testStanden(p)) && Proeven.testMis(p) == 1,
                    "a mirror the light has not reached does not turn, and counts as a miss");
            for (int k = 0; k < 4 && Proeven.bereik(Proeven.testStanden(p), ids) == 0; k++) {
                Proeven.testSpiegel(p, 1);
            }
            helper.assertTrue(Proeven.bereik(Proeven.testStanden(p), ids) >= 1, "clicking the glinting mirror turns it right");
            voor = Proeven.testStanden(p);
            Proeven.testSpiegel(p, 1);
            helper.assertTrue(java.util.Arrays.equals(voor, Proeven.testStanden(p)), "a right mirror stays right");
            int vooraf = Proeven.bereik(Proeven.testStanden(p), ids);
            for (int k = 0; k < Proeven.HINT_NA + Proeven.HELP_NA + 1 && Proeven.bereik(Proeven.testStanden(p), ids) == vooraf; k++) {
                Proeven.testSpiegel(p, Math.min(ids.size(), vooraf + 2));
            }
            helper.assertTrue(Proeven.bereik(Proeven.testStanden(p), ids) > vooraf, "after the hint, Pakguh turns the glinting mirror himself");
            for (int k = 0; k < 4 * ids.size() && Proeven.bereik(Proeven.testStanden(p), ids) < ids.size(); k++) {
                Proeven.testSpiegel(p, Proeven.bereik(Proeven.testStanden(p), ids) + 1);
            }
            helper.assertTrue(Proeven.bereik(Proeven.testStanden(p), ids) == ids.size(), "only the glinting mirror, again and again: solved");
        } finally {
            if (ver != null) {
                ver.discard();
            }
            if (bij != null) {
                bij.discard();
            }
            Proeven.stop(p);
            wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
