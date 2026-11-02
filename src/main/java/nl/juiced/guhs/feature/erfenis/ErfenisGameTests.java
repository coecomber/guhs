package nl.juiced.guhs.feature.erfenis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.gametest.GuhTest;

/** De Erfenis (POC): the rules of a base, the molen, saving, the spiral of cells, and stamping/moving one building. */
public class ErfenisGameTests {
    private static final String BATCH = "erfenis";
    private static final long T0 = 1_000_000L;

    private static Base start() {
        return Base.nieuw(UUID.randomUUID(), "Testguh", 0, 0, T0);
    }

    /** Overlap, out of bounds, limits and locks are refused; a free spot is fine and costs Bouwspul. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void plaatsRegels(GameTestHelper helper) {
        Base b = start();
        helper.assertTrue(b.stadhuisLevel() == 1 && b.aantal(GebouwType.MOLEN) == 1, "start: stadhuis 1 and a molen");
        helper.assertTrue(b.kanPlaatsen(GebouwType.MUUR, 31, 31) != null, "overlap with the stadhuis is refused");
        helper.assertTrue("erfenis.fout.buiten".equals(b.kanPlaatsen(GebouwType.MUUR, 64, 3).key()), "out of the plot is refused");
        helper.assertTrue("erfenis.fout.buiten".equals(b.kanPlaatsen(GebouwType.BOOMPJE, 63, 10).key()), "a 2x2 over the edge is refused");
        helper.assertTrue("erfenis.fout.slot".equals(b.kanPlaatsen(GebouwType.MOLEN, 5, 5).key()), "a second molen waits for stadhuis 2");
        helper.assertTrue("erfenis.fout.slot".equals(b.kanPlaatsen(GebouwType.SCHUURTJE, 5, 5).key()), "the schuurtje waits for stadhuis 2");
        int voor = b.bouwspul;
        helper.assertTrue(b.plaats(GebouwType.MUUR, 5, 5, 0, T0, -1) == null, "a wall on a free spot");
        helper.assertTrue(b.bouwspul == voor - GebouwType.MUUR.kosten(1), "the wall cost Bouwspul");
        helper.assertTrue(b.op(5, 5) != null && b.op(5, 5).level == 1 && b.bouw == null, "a wall stands at once, no builder");
        helper.assertTrue(b.kanVerplaatsen(b.op(5, 5).id, 31, 31) != null, "moving onto the stadhuis is refused");
        helper.assertTrue(b.verplaats(b.op(5, 5).id, 6, 5) == null && b.op(6, 5) != null && b.op(5, 5) == null, "moving to a free spot");
        helper.succeed();
    }

    /** One builder: an upgrade blocks the next; it finishes at its time; the stadhuis level unlocks more. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bouwerEnLevels(GameTestHelper helper) {
        Base b = start();
        b.bouwspul = 10_000;
        Gebouw sh = b.stadhuis();
        helper.assertTrue(b.verbeter(sh.id, T0, -1) == null, "the stadhuis upgrade starts");
        helper.assertTrue(b.bouwspul == 10_000 - GebouwType.STADHUIS.kosten(2), "it cost Bouwspul");
        helper.assertTrue("erfenis.fout.bouwer".equals(b.kanVerbeteren(sh.id).key()), "the builder is busy (one job at a time)");
        helper.assertTrue(b.afronden(T0 + 1000) == null && sh.level == 1, "not done too early");
        long eind = T0 + GebouwType.STADHUIS.tijdSec(2) * 1000L;
        helper.assertTrue(b.afronden(eind) == sh && sh.level == 2 && b.bouw == null, "done at its time: level 2");
        helper.assertTrue(b.kanKopen(GebouwType.MOLEN) == null, "stadhuis 2 allows a second molen");
        helper.assertTrue(b.plaats(GebouwType.MOLEN, 5, 5, 0, eind, -1) == null, "a new molen is placed");
        Gebouw nieuw = b.op(5, 5);
        helper.assertTrue(nieuw.level == 0 && b.inAanbouw(nieuw), "a new molen is first built by the builder");
        helper.assertTrue(b.afronden(eind + 10_000) == nieuw && nieuw.level == 1, "and then stands at level 1");
        helper.assertTrue(b.kanVerbeteren(nieuw.id) == null, "molen level 2 is allowed at stadhuis 2");
        b.verbeter(nieuw.id, eind, 0);
        b.afronden(eind);
        helper.assertTrue("erfenis.fout.slot".equals(b.kanVerbeteren(nieuw.id).key()), "molen level 3 waits for stadhuis 4");
        helper.succeed();
    }

    /** The molen fills per real minute up to its own cap; collecting stops at the storage cap and keeps the rest. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void molenOphalen(GameTestHelper helper) {
        Base b = start();
        Gebouw m = b.op(24, 32);
        m.voorraad = 0;
        m.bijgewerkt = T0;
        helper.assertTrue(Math.abs(m.voorraadOp(T0 + 60_000) - GebouwType.productiePerMinuut(1)) < 1e-6, "one minute = one minute of Bouwspul");
        helper.assertTrue(m.voorraadOp(T0 + 3_600_000) == GebouwType.molenOpslag(1), "it stops when full");
        b.bouwspul = b.opslag() - 5;
        Object r = b.ophalen(m.id, T0 + 3_600_000);
        helper.assertTrue(Integer.valueOf(5).equals(r), "only what fits in the storage");
        helper.assertTrue(b.bouwspul == b.opslag() && m.voorraad >= GebouwType.molenOpslag(1) - 5 - 1e-6, "the rest stays in the molen");
        helper.assertTrue("erfenis.fout.vol".equals(b.kanOphalen(m.id, T0 + 3_600_000).key()), "full storage: nothing more");
        helper.succeed();
    }

    /** Saving and loading keep everything; the cell spiral never gives a cell twice; an NPC base keeps to its limits. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslaanEnCellen(GameTestHelper helper) {
        Base b = start();
        b.bouwspul = 999;
        b.verbeter(b.stadhuis().id, T0, -1);
        Base terug = Base.load(b.save());
        helper.assertTrue(terug != null && terug.id.equals(b.id) && terug.gebouwen.size() == b.gebouwen.size() && terug.bouwspul == b.bouwspul
                && terug.bouw != null && terug.bouw.eind() == b.bouw.eind() && terug.stadhuisLevel() == 1, "save/load round trip");
        Set<Long> gezien = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            int[] c = Plot.spiraal(i);
            helper.assertTrue(gezien.add(Plot.sleutel(c[0], c[1])), "spiral cell " + i + " given twice");
            helper.assertTrue(Math.abs(c[0]) <= 10 && Math.abs(c[1]) <= 10, "spiral stays compact");
        }
        for (int th = 1; th <= GebouwType.MAX_STADHUIS; th++) {
            Base npc = Base.npc(UUID.randomUUID(), "Knabbeldorp", 3, 4, th, 42L + th, T0);
            helper.assertTrue(npc.stadhuisLevel() == th, "npc stadhuis level");
            for (GebouwType t : GebouwType.values()) {
                helper.assertTrue(npc.aantal(t) <= Math.max(1, t.maxAantal(th)), "npc " + t.id + " over its limit at " + th);
            }
            for (Gebouw g : npc.gebouwen) {
                helper.assertTrue(npc.binnen(g.x, g.z, g.type.maat) && npc.vrij(g.x, g.z, g.type.maat, g.id), "npc building overlaps or sticks out");
            }
        }
        helper.succeed();
    }

    /** Stamping a stadhuis puts its level's material in the world; moving clears the old spot; an upgrade shows scaffolding. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void stempelEnVerplaats(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(1, 0, 1)).offset(-30, 0, -30);   // (so the stadhuis at 30,30 lands in the test area)
        Base b = start();
        Gebouw sh = b.stadhuis();
        Stempel.gebouw(level, o, b, sh);
        BlockPos hoek = o.offset(sh.x, 1, sh.z);
        helper.assertTrue(level.getBlockState(hoek).is(Blocks.OAK_LOG), "level 1: an oak corner");
        helper.assertTrue(level.getBlockState(o.offset(sh.x + 1, 1, sh.z)).is(Blocks.OAK_PLANKS), "level 1: oak walls");
        sh.level = 3;
        Stempel.gebouw(level, o, b, sh);
        helper.assertTrue(level.getBlockState(o.offset(sh.x + 1, 1, sh.z)).is(Blocks.IRON_BLOCK), "level 3: iron walls");
        b.bouwspul = 10_000;
        b.verbeter(sh.id, T0, -1);
        Stempel.gebouw(level, o, b, sh);
        helper.assertTrue(level.getBlockState(hoek).is(Blocks.SCAFFOLDING), "an upgrade shows scaffolding");
        b.afronden(Long.MAX_VALUE);
        int ox = sh.x, oz = sh.z;
        sh.x = ox - 3;
        Stempel.wis(level, o, ox, oz, sh.type.maat);
        Stempel.gebouw(level, o, b, sh);
        helper.assertTrue(level.getBlockState(o.offset(ox + 4, 1, oz)).isAir(), "the old spot is empty again");
        helper.assertTrue(level.getBlockState(o.offset(ox + 4, 0, oz)).is(Blocks.GRASS_BLOCK), "with grass under it");
        helper.assertTrue(level.getBlockState(o.offset(sh.x + 1, 1, sh.z)).is(Blocks.GOLD_BLOCK), "level 4 gold at the new spot");
        helper.succeed();
    }

    /** Phase 2: an NPC base in a GIVEN cell; the player spiral skips it; growing replaces the buildings in place. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcBaseOpEigenCel(GameTestHelper helper) {
        ErfenisData d = new ErfenisData();
        UUID dorp = UUID.randomUUID();
        java.util.List<Base.Plan> klein = java.util.List.of(new Base.Plan(GebouwType.STADHUIS, 1, 30, 30, 0), new Base.Plan(GebouwType.MOLEN, 1, 10, 10, 0));
        Base n = d.npcBase(dorp, "Testdorp", 1, 0, klein, T0);
        helper.assertTrue(n != null && n.npc && n.cx == 1 && n.cz == 0 && n.gebouwen.size() == 2, "the NPC base stands in its own cell");
        helper.assertTrue(n.bouwspul <= n.opslag(), "its Bouwspul fits its storage");
        Base a = d.vanSpeler(UUID.randomUUID(), "A", T0), b = d.vanSpeler(UUID.randomUUID(), "B", T0);
        helper.assertTrue(a.cx == 0 && a.cz == 0, "the first player gets (0,0)");
        helper.assertFalse(b.cx == 1 && b.cz == 0, "the second player skips the NPC cell");
        helper.assertTrue(d.npcBase(UUID.randomUUID(), "Indringer", 0, 0, klein, T0) == null, "a taken cell is refused");
        helper.assertTrue(d.npcBase(a.id, "Kaper", 0, 0, klein, T0) == null, "a player's base is never replaced");
        n.moetStempelen = false;
        java.util.List<Base.Plan> groot = new java.util.ArrayList<>(klein);
        groot.add(new Base.Plan(GebouwType.BOOMPJE, 1, 40, 40, 0));
        groot.add(new Base.Plan(GebouwType.MOLEN, 1, 31, 31, 0));   // (overlaps the stadhuis: skipped)
        Base n2 = d.npcBase(dorp, "Testdorp", 1, 0, groot, T0 + 5);
        helper.assertTrue(n2 == n && n.gebouwen.size() == 3 && n.moetStempelen, "the same object, new buildings, stamp again");
        helper.assertTrue(d.bezetteCellen().contains(Plot.sleutel(1, 0)) && d.bezetteCellen().size() == 3, "three cells taken");
        helper.succeed();
    }
}
