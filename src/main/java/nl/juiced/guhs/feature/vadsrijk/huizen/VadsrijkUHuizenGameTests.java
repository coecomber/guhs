package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Deur;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-HUIZEN: the housing without its dimension (the test server has no datapack dimensions): ward geometry, the templates (built in
 * the test level, every connecting block joined, the door facing the street), permissions, persistence, placing/breaking, no dupes
 * when a house is cleared, a new ward when one is full, rested xp. Run: {@code runGameTestServer -Pgt=VadsrijkUHuizen}.
 */
public class VadsrijkUHuizenGameTests {
    private static final String BATCH = "vadsrijk_u_huizen";

    /** A plot of our own right next to the test (ward 0 numbers, but at the test's x/z). */
    private static Kavel testKavel(GameTestHelper helper, int dx, Direction straat) {
        BlockPos b = helper.absolutePos(BlockPos.ZERO);
        return new Kavel(0, 0, b.getX() + dx, b.getZ() + 4, straat);
    }

    private static void wis(ServerLevel l, Kavel k) {
        for (int u = 0; u < WijkPlan.KAVEL; u++) {
            for (int v = 0; v < WijkPlan.KAVEL; v++) {
                for (int y = WijkPlan.GROND - 1; y <= WijkPlan.VLOER + Ontwerp.LEEG_HOOGTE; y++) {
                    l.setBlock(k.wereld(u, y, v), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
    }

    private static Huis huis(UUID eigenaar) {
        Huis h = new Huis();
        h.eigenaar = eigenaar;
        h.naam = "Eigenaar";
        return h;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kavelsPassen(GameTestHelper helper) {
        for (int w : new int[] {0, 3}) {
            List<Kavel> l = WijkPlan.kavels(w);
            helper.assertTrue(l.size() == WijkPlan.KAVELS, "24 plots");
            for (int i = 0; i < l.size(); i++) {
                Kavel a = l.get(i);
                helper.assertTrue(WijkPlan.binnenWijk(a.minX(), a.minZ()) && WijkPlan.binnenWijk(a.maxX(), a.maxZ()), "plot " + i + " inside the ward");
                helper.assertTrue(WijkPlan.kavelOp(a.midden().x, a.midden().z) != null && WijkPlan.kavelOp(a.midden().x, a.midden().z).nr() == i, "plot " + i + " found back");
                helper.assertTrue(WijkPlan.kavelOp(a.voorDeur().x, a.voorDeur().z) == null, "the spot in front of plot " + i + " is street");
                int[] uv = a.lokaal(a.wereld(5, 64, 13));
                helper.assertTrue(uv[0] == 5 && uv[1] == 13, "frame round trip " + i);
                BlockPos straat = a.wereld(WijkPlan.POORT_U, 63, -3);   // (3 blocks out: the street itself)
                double sx = straat.getX() - w * WijkPlan.AFSTAND, sz = straat.getZ();
                helper.assertTrue(Math.min(Math.abs(sx), Math.abs(sz)) <= 2, "plot " + i + " faces its street: " + sx + "," + sz);
                for (int j = i + 1; j < l.size(); j++) {
                    Kavel b = l.get(j);
                    boolean over = a.minX() <= b.maxX() && b.minX() <= a.maxX() && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
                    helper.assertTrue(!over, "plots " + i + " and " + j + " overlap");
                }
            }
        }
        helper.assertTrue(WijkPlan.kavelOp(0.5, 0.5) == null, "the square is no plot");
        helper.assertTrue(WijkPlan.inPoort(0.5, 64, -9.5) && !WijkPlan.inPoort(0.5, 64, -5.5), "the gate is the arch, not the arrival spot");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void huizenBouwenVerbonden(GameTestHelper helper) {
        ServerLevel l = helper.getLevel();
        HuisSoort[] soorten = HuisSoort.values();
        Direction[] straten = {Direction.NORTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < soorten.length; i++) {
            Kavel k = testKavel(helper, 30 + i * 24, straten[i]);
            Ontwerp.Uitkomst uit = Ontwerp.huis(l, k, soorten[i], i);
            BlockState deur = l.getBlockState(uit.deur);
            helper.assertTrue(deur.getBlock() instanceof DoorBlock && deur.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && l.getBlockState(uit.deur.above()).getBlock() instanceof DoorBlock, soorten[i] + ": a two-block door");
            helper.assertTrue(deur.getValue(DoorBlock.FACING) == straten[i], soorten[i] + ": the door faces the street " + deur.getValue(DoorBlock.FACING));
            int hekken = 0, ruiten = 0;
            for (BlockPos p : uit.blokken) {
                BlockState s = l.getBlockState(p);
                if (s.getBlock() instanceof FenceBlock && p.getY() == WijkPlan.VLOER) {
                    hekken++;
                    boolean los = !s.getValue(FenceBlock.NORTH) && !s.getValue(FenceBlock.EAST) && !s.getValue(FenceBlock.SOUTH) && !s.getValue(FenceBlock.WEST);
                    helper.assertTrue(!los, soorten[i] + ": a lone fence post at " + p);
                }
                if (s.getBlock() instanceof IronBarsBlock) {   // (glass panes)
                    ruiten++;
                    boolean los = !s.getValue(IronBarsBlock.NORTH) && !s.getValue(IronBarsBlock.EAST) && !s.getValue(IronBarsBlock.SOUTH) && !s.getValue(IronBarsBlock.WEST);
                    helper.assertTrue(!los, soorten[i] + ": a lone window pane at " + p);
                }
            }
            helper.assertTrue(hekken >= 70 && ruiten >= 6, soorten[i] + ": fence and windows there (" + hekken + ", " + ruiten + ")");
            helper.assertTrue(uit.trofeeen.size() >= 2 && !Ontwerp.trofeePlekken(k, soorten[i]).isEmpty(), soorten[i] + ": a trophy shelf");
            BlockPos post = uit.post;
            helper.assertTrue(l.getBlockState(post).is(Blocks.BARREL) && post.equals(k.wereld(WijkPlan.POST_U, WijkPlan.VLOER + 1, WijkPlan.POST_V)), "the mailbox");
            // walkable: the path to the door is flat, the door cell + head room are free when open
            for (int v = 0; v < soorten[i].v0(); v++) {
                BlockPos g = k.wereld(WijkPlan.POORT_U, WijkPlan.GROND, v);
                helper.assertTrue(l.getBlockState(g).is(Blocks.DIRT_PATH) || v == 0, soorten[i] + ": path at v " + v);
                if (v > 0) {
                    helper.assertTrue(l.getBlockState(g.above()).getCollisionShape(l, g.above()).isEmpty()
                            && l.getBlockState(g.above(2)).getCollisionShape(l, g.above(2)).isEmpty(), soorten[i] + ": free path at v " + v);
                }
            }
            // the inside: lit and with a floor
            BlockPos binnen = k.wereld(soorten[i].u0() + 2, WijkPlan.VLOER, soorten[i].v0() + 2);
            helper.assertTrue(!l.getBlockState(binnen.below()).isAir() && l.getBlockState(binnen).isAir(), soorten[i] + ": a floor inside");
            helper.assertTrue(Ontwerp.inHuis(k, soorten[i], binnen.getX() + 0.5, binnen.getY(), binnen.getZ() + 0.5), soorten[i] + ": inside the house box");
            wis(l, k);
        }
        // the free plot: the sign by the street
        Kavel k = testKavel(helper, 30, Direction.SOUTH);
        Ontwerp.Uitkomst uit = Ontwerp.teKoop(l, k);
        helper.assertTrue(l.getBlockState(uit.bord).is(Blocks.OAK_SIGN), "the te koop sign");
        wis(l, k);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void rechten(GameTestHelper helper) {
        UUID baas = UUID.randomUUID(), vriend = UUID.randomUUID(), vreemd = UUID.randomUUID();
        Huis h = huis(baas);
        h.vrienden.put(vriend, "Vriend");
        h.deur = Deur.VRIENDEN;
        helper.assertTrue(Rechten.rol(h, baas) == Rechten.Rol.EIGENAAR && Rechten.rol(h, vriend) == Rechten.Rol.VRIEND && Rechten.rol(h, vreemd) == Rechten.Rol.BEZOEKER, "roles");
        helper.assertTrue(Rechten.magBinnen(h, baas) && Rechten.magBinnen(h, vriend) && !Rechten.magBinnen(h, vreemd), "door: friends");
        h.deur = Deur.OPEN;
        helper.assertTrue(Rechten.magBinnen(h, vreemd), "door: open for everybody");
        h.deur = Deur.SLOT;
        helper.assertTrue(Rechten.magBinnen(h, baas) && !Rechten.magBinnen(h, vriend) && !Rechten.magBinnen(h, vreemd), "door: locked = only the owner");
        helper.assertTrue(Rechten.magInrichten(h, baas) && !Rechten.magInrichten(h, vriend) && !Rechten.magInrichten(h, vreemd), "decorating: owner only");
        h.vriendenInrichten = true;
        helper.assertTrue(Rechten.magInrichten(h, vriend) && !Rechten.magInrichten(h, vreemd), "decorating: friends allowed");
        // placing / breaking
        Kavel k = WijkPlan.kavel(0, 4);
        BlockPos binnen = k.wereld(10, WijkPlan.VLOER, 15), hek = k.wereld(0, WijkPlan.VLOER, 5), hoog = k.wereld(10, WijkPlan.VLOER + WijkPlan.HOOGTE + 1, 15);
        BlockState wol = Blocks.PINK_WOOL.defaultBlockState();
        helper.assertTrue(Rechten.plaatsen(h, k, binnen, baas, wol) == null, "owner places inside");
        helper.assertTrue(Rechten.plaatsen(h, k, binnen, vreemd, wol) != null, "a visitor can't");
        helper.assertTrue(Rechten.plaatsen(h, k, hek, baas, wol) != null, "not on the fence line");
        helper.assertTrue(Rechten.plaatsen(h, k, hoog, baas, wol) != null, "not too high");
        helper.assertTrue(Rechten.plaatsen(h, k, binnen, baas, Blocks.CHEST.defaultBlockState()) != null, "no containers");
        helper.assertTrue(Rechten.plaatsen(h, k, binnen, baas, Blocks.SAND.defaultBlockState()) != null, "no falling blocks");
        helper.assertTrue(Rechten.plaatsen(h, k, binnen, baas, Blocks.TNT.defaultBlockState()) != null, "no tnt");
        helper.assertTrue(Rechten.plaatsen(null, k, binnen, baas, wol) != null, "nobody's plot");
        helper.assertTrue(Rechten.breken(h, binnen, baas) != null, "the house itself never breaks");
        h.geplaatst.add(binnen.asLong());
        helper.assertTrue(Rechten.breken(h, binnen, baas) == null && Rechten.breken(h, binnen, vreemd) != null, "a placed block: the owner may");
        for (int i = h.geplaatst.size(); i < h.soort.maxDeco; i++) {
            h.geplaatst.add(binnen.above(1 + i % 10).east(i / 10).asLong());
        }
        helper.assertTrue(Rechten.plaatsen(h, k, binnen.west(), baas, wol) != null, "the deco limit");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void opslaanEnLaden(GameTestHelper helper) {
        HuizenData d = new HuizenData();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        d.wijken = 1;
        helper.assertTrue(d.claim(7, a, "Anna", HuisSoort.VILLA, 2, 25000), "claim");
        helper.assertTrue(!d.claim(8, a, "Anna", HuisSoort.HUIS, 0, 0), "one house per account");
        helper.assertTrue(!d.claim(7, b, "Bert", HuisSoort.HUIS, 0, 0), "a taken plot");
        Huis h = d.huis(7);
        h.vrienden.put(b, "Bert");
        h.deur = Deur.SLOT;
        h.vriendenInrichten = true;
        h.geplaatst.add(BlockPos.asLong(1, 64, 2));
        h.trofeeen.add("baaskill:guhs:vr_baas_koning_klodder");
        h.rijdier = "pluimguh";
        h.huisdieren.add("klodderreus");
        d.terug.put(a, new CompoundTag());
        d.thuisUit.put(a, 1234L);
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:pink_wool");
        item.putInt("count", 1);
        d.bewaarVoorPost(a, item);
        HuizenData t = HuizenData.load(d.save());
        Huis g = t.huis(7);
        helper.assertTrue(g != null && t.huisVan(a) == g && t.sleutelVan(a) == 7, "the house comes back");
        helper.assertTrue(g.soort == HuisSoort.VILLA && g.variant == 2 && g.deur == Deur.SLOT && g.vriendenInrichten && g.betaald == 25000, "its settings");
        helper.assertTrue("Bert".equals(g.vrienden.get(b)) && g.geplaatst.contains(BlockPos.asLong(1, 64, 2)), "friends + placed blocks");
        helper.assertTrue(g.trofeeen.contains("baaskill:guhs:vr_baas_koning_klodder") && g.rijdier.equals("pluimguh") && g.huisdieren.contains("klodderreus"), "trophies, mount, pets");
        helper.assertTrue(t.thuisUit.get(a) == 1234L && t.terug.containsKey(a) && t.postvak.get(a).size() == 1 && t.wijken == 1, "rest, way back, post box");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void nieuweWijkAlsVol(GameTestHelper helper) {
        HuizenData d = new HuizenData();
        helper.assertTrue(d.eersteVrije() == -1 && d.wijkMetPlek() == 0, "no ward yet: ward 0 gets built");
        d.wijken = 1;
        for (int i = 0; i < WijkPlan.KAVELS; i++) {
            helper.assertTrue(d.eersteVrije() == i, "the first free plot is " + i);
            d.claim(i, UUID.randomUUID(), "x" + i, HuisSoort.HUISJE, 0, 0);
        }
        helper.assertTrue(d.eersteVrije() == -1 && d.wijkMetPlek() == 1 && d.bezet(0) == WijkPlan.KAVELS, "full: the next ward");
        d.wijken = 2;
        helper.assertTrue(d.eersteVrije() == WijkPlan.KAVELS, "ward 2 plot 1");
        d.geefOp(5);
        helper.assertTrue(d.eersteVrije() == 5, "a sold plot is free again");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void opruimenZonderDupes(GameTestHelper helper) {
        ServerLevel l = helper.getLevel();
        Kavel k = testKavel(helper, 30, Direction.NORTH);
        Ontwerp.huis(l, k, HuisSoort.HUISJE, 0);
        HuizenData d = new HuizenData();
        UUID baas = UUID.randomUUID();
        Huis h = huis(baas);
        BlockPos wol = k.wereld(15, WijkPlan.VLOER, 3), deur = k.wereld(3, WijkPlan.VLOER, 3);
        l.setBlock(wol, Blocks.PINK_WOOL.defaultBlockState(), 3);
        BlockState d0 = Blocks.OAK_DOOR.defaultBlockState();
        l.setBlock(deur, d0.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        l.setBlock(deur.above(), d0.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        h.geplaatst.add(wol.asLong());
        h.geplaatst.add(deur.asLong());
        h.geplaatst.add(deur.above().asLong());
        h.geplaatst.add(k.wereld(WijkPlan.POORT_U, WijkPlan.VLOER + 1, 2).asLong());   // (one that was already broken: air above the path)
        int n = Kwartier.ruimOp(l, d, h);
        helper.assertTrue(n == 2 && d.postvak.get(baas).size() == 2, "wool + one door in the post box, not two: " + n);
        helper.assertTrue(l.getBlockState(wol).isAir() && l.getBlockState(deur).isAir() && l.getBlockState(deur.above()).isAir(), "taken out of the world");
        helper.assertTrue(h.geplaatst.isEmpty(), "nothing left to remember");
        helper.assertTrue(Kwartier.ruimOp(l, d, h) == 0 && d.postvak.get(baas).size() == 2, "clearing twice gives nothing extra");
        wis(l, k);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void rustXpThuis(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).extra("huizen").putLong("rust", 0);
        long erbij = RustXp.geef(p, 10);
        helper.assertTrue(erbij > 0 && RustXp.pot(p) == erbij, "10 hours home: a rested pool " + erbij);
        long max = RustXp.max(p);
        RustXp.geef(p, 10_000);
        helper.assertTrue(RustXp.pot(p) == max, "capped at 1.5 levels");
        long voor = Spelers.data(p).generiekXp, pot = RustXp.pot(p);
        Spelers.geefXp(p, 10, "test", null);
        long kreeg = Spelers.data(p).generiekXp - voor;
        helper.assertTrue(kreeg >= 20 && RustXp.pot(p) <= pot - 10, "rested: double xp, taken from the pool (" + kreeg + ")");
        Spelers.data(p).extra("huizen").putLong("rust", 0);
        voor = Spelers.data(p).generiekXp;
        Spelers.geefXp(p, 10, "test", null);
        helper.assertTrue(Spelers.data(p).generiekXp - voor < 20, "not rested: normal xp");
        helper.succeed();
    }
}
