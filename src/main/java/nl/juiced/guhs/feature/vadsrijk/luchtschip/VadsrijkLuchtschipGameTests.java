package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-LUCHTSCHIP: the timetable, the paths, the data of all lines, and a real (short) test line with a ship, a passenger, the
 * locked-in flight and the landing. Run: {@code runGameTestServer -Pgt=VadsrijkLuchtschip}.
 */
public class VadsrijkLuchtschipGameTests {
    private static final String BATCH = "vadsrijk_luchtschip";

    /** Every line: 60-90 s, both directions start and end at the docks, smooth (no jumps), never below the sea; departures every 3 min. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dienstregeling(GameTestHelper helper) {
        helper.assertTrue(Lijnen.ALLE.stream().filter(l -> !l.test()).count() == 6, "six lines: 0, A, B, C, D, E");   // U-Z6VERHAAL: + line E (zone6.verhaal.Z6Lijn)
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            if (l.test()) {
                continue;
            }
            int d = Dienstregeling.duur(l);
            helper.assertTrue(d >= Lijnen.MIN_DUUR && d <= Lijnen.MAX_DUUR, l.letter() + ": flight " + d + " ticks");
            helper.assertTrue(l.a().id().startsWith("haven_" + l.a().zone() + "_") && l.a().id().endsWith("_vertrek")
                    && l.b().id().equals("haven_" + l.b().zone() + "_" + l.a().zone() + "_vertrek"), l.letter() + ": POI ids " + l.a().id() + " / " + l.b().id());
            for (boolean heen : new boolean[] {true, false}) {
                Dienstregeling.Pad pad = Dienstregeling.pad(l, heen);
                Vec3 van = heen ? l.a().dok() : l.b().dok(), naar = heen ? l.b().dok() : l.a().dok();
                helper.assertTrue(pad.op(0).distanceTo(van) < 0.5 && pad.op(1).distanceTo(naar) < 0.5, l.letter() + ": path ends at the docks");
                helper.assertTrue(pad.lengte() > 300, l.letter() + ": a real journey (" + pad.lengte() + ")");
            }
            // sweep two periods: speed, height, and exactly one departure per period from each harbour
            Vec3 vorig = null;
            int vertrekA = 0;
            for (int t = 0; t < 2 * l.periode(); t++) {
                Dienstregeling.Stand s = Dienstregeling.stand(l, 0, l.fase() + t);
                if (vorig != null) {
                    helper.assertTrue(s.pos().distanceTo(vorig) < 0.75, l.letter() + ": jump of " + s.pos().distanceTo(vorig) + " at " + t);
                }
                helper.assertTrue(s.pos().y > 60, l.letter() + ": too low at " + t + ": " + s.pos());
                vorig = s.pos();
                if (Dienstregeling.totVertrek(l, l.fase() + t) == 0) {
                    vertrekA++;
                }
                Dienstregeling.Stand s1 = Dienstregeling.stand(l, 1, l.fase() + t);
                helper.assertTrue(s.fase() != s1.fase(), l.letter() + ": both ships in the same phase at " + t);
            }
            helper.assertTrue(vertrekA == 2, l.letter() + ": departures in two periods: " + vertrekA);
            helper.assertTrue(Dienstregeling.stand(l, 0, l.fase()).fase() == Dienstregeling.Fase.VLIEGT_AB
                    && Dienstregeling.stand(l, 1, l.fase()).fase() == Dienstregeling.Fase.VLIEGT_BA, l.letter() + ": both leave at the same moment");
            helper.assertTrue(Dienstregeling.schipAan(l, l.a(), l.fase() - 10) == 0 && Dienstregeling.schipAan(l, l.b(), l.fase() - 10) == 1,
                    l.letter() + ": a ship waits at each pier before the departure");
        }
        helper.assertTrue(Luchtschepen.klok(20 * 83).equals("1:23") && Luchtschepen.klok(0).equals("0:00"), "m:ss");
        helper.succeed();
    }

    /** A test line in the test area: board, depart (locked in, no damage, no sneaking off), land on the other pier. */
    // (own batch: the ship flies outside the tiny test area, and neighbouring tests clear the entities in their area)
    @GuhTest(template = "empty", batch = BATCH + "_vlucht", timeoutTicks = 400)
    public static void vlucht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x, 2, z), Blocks.STONE.defaultBlockState(), 3);
                sl.setBlock(o.offset(x + 30, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t1_t2_vertrek", o.above(3), 270f, "t1");
        Lijnen.Haven b = new Lijnen.Haven("haven_t2_t1_vertrek", o.offset(30, 3, 0), 90f, "t2");
        int periode = 200, duur = 60;
        int fase = (int) ((sl.getServer().getTickCount() + 30) % (2 * periode));   // (mod 2P: ship 0 is the one at a)
        Lijnen.Lijn l = new Lijnen.Lijn("test_" + sl.getServer().getTickCount(), "T", a, b, List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)),
                fase, duur, periode, true);
        Lijnen.ALLE.add(l);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(a.plek().getX() + 0.5, a.plek().getY(), a.plek().getZ() + 0.5);
        int k = Dienstregeling.schipAan(l, a, Tijd.nu(sl, l));
        helper.assertTrue(k == 0, "ship 0 waits at a: " + k);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null, "ship spawned");
        helper.assertTrue(Luchtschepen.instappen(p, e), "boarded");
        helper.assertTrue(p.getVehicle() == e, "on board");
        long start = sl.getServer().getTickCount();
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(e.vliegt(), "flying: " + e.stand() + " t=" + Tijd.nu(sl, l) + " fase=" + l.fase());
            helper.assertTrue(p.getVehicle() == e, "passenger still on board: vehicle " + p.getVehicle() + ", removed " + e.isRemoved()
                    + ", alive " + p.isAlive() + ", pos " + p.position() + ", ship " + e.position());
            helper.assertTrue(p.getPersistentData().getStringOr(Luchtschepen.TAG_NAAR, "").equals(b.id()), "destination remembered (ship ticks " + e.tickCount + ", entity-ticking " + sl.isPositionEntityTicking(e.blockPosition()) + ")");
            p.setShiftKeyDown(true);
            p.stopRiding();
            p.setShiftKeyDown(false);
            helper.assertTrue(p.getVehicle() == e, "no sneaking off in flight");
            float hp = p.getHealth();
            p.hurtServer(sl, sl.damageSources().generic(), 4f);
            helper.assertTrue(p.getHealth() == hp, "no damage on board");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sl.getServer().getTickCount() - start > 60, "not yet");
            helper.assertTrue(p.getVehicle() == null, "landed: off the ship");
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(b.plek())) < 4, "on the pier of b: " + p.position());
            helper.assertTrue(p.getPersistentData().getStringOr(Luchtschepen.TAG_NAAR, "").isEmpty(), "destination cleared");
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
        });
    }

    /** Logout in flight: you are put on the destination pier (and the ship goes on without you). */
    @GuhTest(template = "empty", batch = BATCH + "_uit", timeoutTicks = 200)
    public static void uitloggenInVlucht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                sl.setBlock(o.offset(x + 30, 2, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        Lijnen.Haven a = new Lijnen.Haven("haven_t3_t4_vertrek", o.above(3), 270f, "t3");
        Lijnen.Haven b = new Lijnen.Haven("haven_t4_t3_vertrek", o.offset(30, 3, 0), 90f, "t4");
        int fase = (int) ((sl.getServer().getTickCount() + 10) % 800);
        Lijnen.Lijn l = new Lijnen.Lijn("test_uit_" + sl.getServer().getTickCount(), "U", a, b, List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)),
                fase, 200, 400, true);
        Lijnen.ALLE.add(l);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null && Luchtschepen.instappen(p, e), "boarded");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(e.vliegt(), "in flight");
            Luchtschepen.opUitloggen(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            helper.assertTrue(p.getVehicle() == null, "off the ship");
            helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(b.plek())) < 4, "put on the destination pier: " + p.position());
            helper.assertTrue(e.isAlive(), "the ship flies on");
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
            helper.succeed();
        });
    }

    /** The luxury travel map: the first flight to a place is free, then it costs the price (or is refused). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void luxeKaartje(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        String plek = "testplek_" + helper.getLevel().getGameTime();
        helper.assertTrue(Luchtschepen.luxe(p, plek, false), "first time free");
        helper.assertTrue(!Luchtschepen.luxe(p, plek, false), "second time: no money, no flight");
        helper.assertTrue(Luchtschepen.luxe(p, plek, true), "tests fly free");
        helper.succeed();
    }

    /** The view distance of the dimension: configured 20 by default, never below the server's own. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zichtafstand(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int basis = server.getPlayerList().getViewDistance();
        helper.assertTrue(Zicht.STANDAARD == 20, "default 20");
        int z = Zicht.effectief(server);
        helper.assertTrue(z >= basis && z <= 32, "effective " + z + " (server " + basis + ")");
        helper.assertTrue(Zicht.ingesteld() <= 0 || z == Math.max(basis, Zicht.ingesteld()), "uses the configured value");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------ U-LUCHTSCHIP

    /** A short test line lying at its first pier for a while (period 400, flight 200, departure in {@code over} ticks). */
    private static Lijnen.Lijn testLijn(GameTestHelper helper, String naam, int over) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 1, 1));
        Lijnen.Haven a = new Lijnen.Haven("haven_" + naam + "a_" + naam + "b_vertrek", o.above(3), 270f, naam + "a");
        Lijnen.Haven b = new Lijnen.Haven("haven_" + naam + "b_" + naam + "a_vertrek", o.offset(30, 3, 0), 90f, naam + "b");
        int fase = (int) ((sl.getServer().getTickCount() + over) % 800);
        Lijnen.Lijn l = new Lijnen.Lijn("test_" + naam + "_" + sl.getServer().getTickCount(), "K", a, b,
                List.of(new Vec3(o.getX() + 15.5, o.getY() + 8, o.getZ() + 0.5)), fase, 200, 400, true);
        Lijnen.ALLE.add(l);
        return l;
    }

    /**
     * The whole ship can be clicked: click boxes over the hull, the cabin and the balloon (moved with the ship, registered with the
     * level so a click packet finds them); a right-click on the balloon and a left-click on the hull both board; on board a click
     * is no longer swallowed.
     */
    @GuhTest(template = "empty", batch = BATCH + "_klik", timeoutTicks = 100)
    public static void helemaalKlikbaar(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Lijnen.Lijn l = testLijn(helper, "kl", 150);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        helper.assertTrue(e != null && e.isMultipartEntity() && e.delen().length == LuchtschipEntity.DELEN.length, "a multipart ship");
        e.volg();
        helper.assertTrue(!e.vliegt(), "lies at the pier");
        float r = -e.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        // points on the model (ship frame): bow deck, stern hull underside, cabin roof, balloon middle, balloon nose, tail, ears, balloon side
        double[][] punten = {{0, 0.5, 7.5}, {1.5, -2.5, -7}, {0, 3.5, -6.5}, {3, 13, 0}, {0, 13, 11.5}, {0, 13, -12}, {2, 18.5, 6.75}, {-3, 9, 8}};
        for (double[] pt : punten) {
            Vec3 w = e.position().add(new Vec3(pt[0], pt[1], pt[2]).yRot(r));
            boolean raak = false;
            for (SchipDeel d : e.delen()) {
                if (d.getBoundingBox().contains(w) && d.isPickable() && sl.getEntityOrPart(d.getId()) == d) {
                    raak = true;
                }
            }
            helper.assertTrue(raak, "a click box at ship point " + java.util.Arrays.toString(pt));
        }
        // the parts never collide, are never hit by arrows and never hurt
        SchipDeel ballon = e.delen()[7];
        helper.assertTrue(!ballon.canBeCollidedWith(null) && !ballon.canBeHitByProjectile() && !ballon.hurtServer(sl, sl.damageSources().generic(), 5f),
                "click boxes only");
        // right-click on the balloon boards
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(l.a().plek().getX() + 0.5, l.a().plek().getY(), l.a().plek().getZ() + 0.5);
        ballon.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO);
        helper.assertTrue(p.getVehicle() == e, "right-click on the balloon: on board");
        helper.assertTrue(ballon.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO) == net.minecraft.world.InteractionResult.PASS,
                "on board a click on the ship passes (eating, items)");
        // left-click on the hull boards a second player (and is no hit)
        ServerPlayer q = GuhMockPlayer.of(helper);
        q.setGameMode(GameType.SURVIVAL);
        q.snapTo(l.a().plek().getX() + 0.5, l.a().plek().getY(), l.a().plek().getZ() + 0.5);
        var aanval = new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(q, e.delen()[1]);
        Luchtschepen.opAanval(aanval);
        helper.assertTrue(aanval.isCanceled() && q.getVehicle() == e, "left-click on the hull: on board, no hit");
        helper.assertTrue(e.dekPlek(p).distanceTo(e.dekPlek(q)) >= 1, "two passengers start on different places");
        Luchtschepen.ruimOp(sl, l);
        Lijnen.ALLE.remove(l);
        helper.succeed();
    }

    /**
     * Walking on the deck: a step from the client moves you over the deck (the server places you there, in the ship frame, turned
     * with the ship); the railing holds you; a too-big step is cut short; in flight you still walk and still cannot get off.
     */
    @GuhTest(template = "empty", batch = BATCH + "_dek", timeoutTicks = 200)
    public static void dekLopen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Lijnen.Lijn l = testLijn(helper, "dk", 30);
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, 0);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(l.a().plek().getX() + 0.5, l.a().plek().getY(), l.a().plek().getZ() + 0.5);
        helper.assertTrue(e != null && Luchtschepen.instappen(p, e), "boarded");
        Vec3 start = e.dekPlek(p);
        // small steps forward: accepted
        Vec3 plek = start;
        for (int i = 0; i < 5; i++) {
            plek = plek.add(0, 0, 0.2);
            LuchtschipDek.stap(p, new LuchtschipDek.Stap(e.getId(), (float) plek.x, (float) plek.z));
        }
        helper.assertTrue(e.dekPlek(p).distanceTo(plek) < 1e-3, "walked 1 block forward: " + e.dekPlek(p) + " vs " + plek);
        e.positionRider(p);
        Vec3 verwacht = e.position().add(e.dekPlek(p).yRot(-e.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD));
        helper.assertTrue(Math.abs(p.getX() - verwacht.x) < 0.01 && Math.abs(p.getZ() - verwacht.z) < 0.01 && Math.abs(p.getY() - e.getY()) < 0.01,
                "the server puts you there, on the deck: " + p.position() + " vs " + verwacht);
        // the railing: a place outside the deck is clamped
        Vec3 rand = LuchtschipEntity.klem(10, -20);
        helper.assertTrue(rand.x == LuchtschipEntity.DEK_X && rand.z == LuchtschipEntity.DEK_ACHTER, "clamped to the deck");
        // a big jump (cheat / lag) is cut short
        Vec3 voor = e.dekPlek(p);
        LuchtschipDek.stap(p, new LuchtschipDek.Stap(e.getId(), (float) voor.x, (float) (voor.z - 6)));
        helper.assertTrue(e.dekPlek(p).distanceTo(voor) < 5, "no teleport over the deck: " + e.dekPlek(p));
        // a step for another ship is ignored
        Vec3 nu = e.dekPlek(p);
        LuchtschipDek.stap(p, new LuchtschipDek.Stap(e.getId() + 999, 0f, 0f));
        helper.assertTrue(e.dekPlek(p).equals(nu), "a step for another ship is ignored");
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(e.vliegt(), "flying");
            Vec3 a = e.dekPlek(p);
            Vec3 b = LuchtschipEntity.klem(a.x + 0.2, a.z);
            LuchtschipDek.stap(p, new LuchtschipDek.Stap(e.getId(), (float) b.x, (float) b.z));
            helper.assertTrue(e.dekPlek(p).distanceTo(b) < 1e-3, "walking in flight");
            p.setShiftKeyDown(true);
            p.stopRiding();
            p.setShiftKeyDown(false);
            helper.assertTrue(p.getVehicle() == e, "still no getting off in flight");
            helper.assertTrue(p.entityTags().contains(LuchtschipEntity.TAG_VRIJ), "barrier exemption while walking");
            Luchtschepen.ruimOp(sl, l);
            Lijnen.ALLE.remove(l);
            helper.succeed();
        });
    }

    /**
     * A closed line explains itself: a new player (first quest) gets "follow the main quest first: after X you can fly here" with
     * the right quest per line, a short reason for the action bar, the board says who may fly; the way back and creative stay open.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dichtUitleg(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig.TEST = 6;   // (1.10.2: the story reasons, not the server gate's "binnenkort")
        try {
            dichtUitlegAlles(helper, p);
        } finally {
            nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig.TEST = null;
        }
        helper.succeed();
    }

    private static void dichtUitlegAlles(GameTestHelper helper, ServerPlayer p) {
        String[][] verwacht = {{"kamp_z1", "hq05_klodderreus"}, {"z1_z2", "hq12_schaduw"}, {"z2_z3", "z2_hq12_hartsplinter"},
                {"z3_z4", "z3_hq14_vahoegnir"}, {"z4_z5", "z4_hq07_opstijgpier"}};
        for (String[] v : verwacht) {
            Lijnen.Lijn l = Lijnen.vanId(v[0]);
            helper.assertTrue(l != null, "line " + v[0]);
            String slot = Poorten.dicht(p, l, true);
            helper.assertTrue(slot != null, v[0] + ": closed for a new player");
            helper.assertTrue(Poorten.dicht(p, l, false) == null, v[0] + ": the way back is open");
            String uitleg = teksten(Poorten.uitleg(p, l, slot));
            helper.assertTrue(uitleg.contains("vadsrijk.luchtschip.dicht.verhaal") && uitleg.contains(v[1]), v[0] + ": names the quest " + v[1] + ": " + uitleg);
            helper.assertTrue(teksten(Poorten.kort(p, l)).contains("vadsrijk.luchtschip.kort.verhaal"), v[0] + ": short reason");
            helper.assertTrue(teksten(Luchtschepen.bordTekst(helper.getLevel(), l, l.a()).get(3)).contains("vadsrijk.luchtschip.bord.voor"),
                    v[0] + ": the board says who may fly");
            helper.assertTrue(teksten(Luchtschepen.bordTekst(helper.getLevel(), l, l.b()).get(3)).contains("vadsrijk.luchtschip.bord.terug"),
                    v[0] + ": the board on the other side: back is always open");
        }
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(Poorten.dicht(p, Lijnen.vanId("z1_z2"), true) == null, "creative: open");
    }

    /** All translation keys, literal texts and arguments of a component, joined (to test texts without a language). */
    private static String teksten(net.minecraft.network.chat.Component c) {
        StringBuilder b = new StringBuilder();
        verzamel(c, b);
        return b.toString();
    }

    private static void verzamel(Object o, StringBuilder b) {
        if (o instanceof net.minecraft.network.chat.Component c) {
            if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                b.append(t.getKey()).append(' ');
                for (Object a : t.getArgs()) {
                    verzamel(a, b);
                }
            } else if (c.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents pt) {
                b.append(pt.text()).append(' ');
            }
            for (net.minecraft.network.chat.Component s : c.getSiblings()) {
                verzamel(s, b);
            }
        } else if (o != null) {
            b.append(o).append(' ');
        }
    }
}
