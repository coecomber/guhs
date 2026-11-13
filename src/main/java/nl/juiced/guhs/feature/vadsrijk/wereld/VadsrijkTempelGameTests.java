package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Werver;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.GuhCompassItem;

/**
 * y-tempel: the fixed ordetempel. The spot finder only takes natural, dry, unprotected ground; placing registers it once (a
 * second place is refused), the Superkompas points at it, its portal waits for Wimpel and then wakes, and undo puts the old
 * ground back. Far away in the test world, on a plate of grass built here. Run: {@code -Pgt=VadsrijkTempel}.
 */
public class VadsrijkTempelGameTests {
    private static final String B = "vadsrijk_tempel";

    /** A flat plate of grass on dirt around {@code m} (ground = m.y: grass at m.y - 1), half {@code h}, chunks loaded. */
    private static void plaat(ServerLevel l, BlockPos m, int h) {
        for (int cx = (m.getX() - h - 3) >> 4; cx <= (m.getX() + h + 3) >> 4; cx++) {
            for (int cz = (m.getZ() - h - 3) >> 4; cz <= (m.getZ() + h + 3) >> 4; cz++) {
                l.getChunk(cx, cz);
            }
        }
        for (int x = -h - 3; x <= h + 3; x++) {
            for (int z = -h - 3; z <= h + 3; z++) {
                for (int y = -7; y <= -1; y++) {
                    l.setBlock(m.offset(x, y, z), y == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2 | 16);
                }
            }
        }
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void plekzoekerKeurt(GameTestHelper helper) {
        ServerLevel l = helper.getLevel();
        BlockPos m = new BlockPos(48016, 101, 48016);
        int h = 6;
        plaat(l, m, h);
        l.setBlock(m.offset(2, 0, 2), Blocks.SHORT_GRASS.defaultBlockState(), 2);
        String r = VasteTempel.keur(l, m, h);
        helper.assertTrue(r == null, "natural flat grass must be fine: " + r);
        // a player's plank
        l.setBlock(m.offset(3, 0, -2), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        r = VasteTempel.keur(l, m, h);
        helper.assertTrue(r != null && r.contains("bouwsel"), "a plank must be refused: " + r);
        l.setBlock(m.offset(3, 0, -2), Blocks.AIR.defaultBlockState(), 2);
        // a chest (a block entity)
        l.setBlock(m.offset(-3, 0, 1), Blocks.CHEST.defaultBlockState(), 2);
        r = VasteTempel.keur(l, m, h);
        helper.assertTrue(r != null && r.contains("bouwsel"), "a chest must be refused: " + r);
        l.setBlock(m.offset(-3, 0, 1), Blocks.AIR.defaultBlockState(), 2);
        // water
        l.setBlock(m.offset(1, -1, 1), Blocks.WATER.defaultBlockState(), 2);
        r = VasteTempel.keur(l, m, h);
        helper.assertTrue(r != null && r.contains("water"), "water must be refused: " + r);
        l.setBlock(m.offset(1, -1, 1), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        // a protected area
        Bescherming.zetDoos(l, "vr_tempel_test", new BoundingBox(m.getX() - 2, m.getY() - 3, m.getZ() - 2, m.getX() + 2, m.getY() + 3, m.getZ() + 2));
        try {
            r = VasteTempel.keur(l, m, h);
            helper.assertTrue(r != null && r.contains("beschermd"), "a protected area must be refused: " + r);
        } finally {
            Bescherming.wisDozen(l, "vr_tempel_test");
        }
        helper.assertTrue(VasteTempel.keur(l, m, h) == null, "fine again");
        // not flat
        for (int y = 0; y < 8; y++) {
            l.setBlock(m.offset(-4, y, -4), Blocks.STONE.defaultBlockState(), 2);
        }
        r = VasteTempel.keur(l, m, h);
        helper.assertTrue(r != null && r.contains("vlak"), "a 8 high pillar is not flat: " + r);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 400)
    public static void plaatsKompasPortaalOngedaan(GameTestHelper helper) {
        ServerLevel l = helper.getLevel();
        if (VasteTempel.get(l.getServer()).geregistreerd()) {
            VasteTempel.ongedaan(l.getServer());
        }
        BlockPos m = new BlockPos(52016, 101, 52016);
        plaat(l, m, VasteTempel.HALF + 2);
        String f = VasteTempel.plaats(l, m, 90, false);
        helper.assertTrue(f == null, "placing failed: " + f);
        VasteTempel v = VasteTempel.get(l.getServer());
        try {
            helper.assertTrue(v.geregistreerd() && m.equals(v.midden()), "registered");
            BlockPos poort = v.portaal();
            helper.assertTrue(poort != null && l.getBlockState(poort).getBlock() instanceof PortaalBlok, "a portal at " + poort);
            BlockState slaapt = l.getBlockState(poort);
            helper.assertTrue(!slaapt.getValue(PortaalBlok.WAKKER), "the portal sleeps, like in a worldgen temple");
            // a second place is refused
            String twee = VasteTempel.plaats(l, m.offset(200, 0, 0), 0, false);
            helper.assertTrue(twee != null && twee.contains("al een vaste tempel"), "second place: " + twee);
            // the Superkompas entry "Het Vadsrijk"
            helper.assertTrue(nl.juiced.guhs.item.SuperkompasItem.allowed("vr_w_portaaltempel"), "the Superkompas has Het Vadsrijk");
            BlockPos k = GuhCompassItem.findCenter(l, ResourceKey.create(Registries.STRUCTURE, Guhs.id("vr_w_portaaltempel")), m.offset(-300, 0, 120));
            helper.assertTrue(poort.equals(k), "the compass points at the fixed temple: " + k);
            // the portal: first Wimpel, then it wakes
            ServerPlayer p = GuhMockPlayer.of(helper);
            var d = Spelers.data(p);
            d.gezien.remove(Werver.GESPROKEN);
            d.gezien.remove(Portalen.ONTWAAKT);
            d.gezien.remove(Portalen.BINNEN_GEWEEST);
            helper.assertTrue(VasteTempel.in(l, poort), "the portal is in the fixed temple");
            helper.assertFalse(Werver.magDoor(p, l, poort, true), "even awake, the temple's portal wants the talk first");
            d.gezien.add(Werver.GESPROKEN);
            Portalen.aanraking(p, poort, slaapt);
            helper.assertTrue(l.getBlockState(poort).getValue(PortaalBlok.WAKKER), "the portal woke up");
            helper.assertTrue(d.gezien.contains(Portalen.ONTWAAKT), "woken by this player");
            d.gezien.remove(Werver.GESPROKEN);
            d.gezien.remove(Portalen.ONTWAAKT);
            // undo
            String o = VasteTempel.ongedaan(l.getServer());
            helper.assertTrue(o == null, "undo: " + o);
            helper.assertTrue(!VasteTempel.get(l.getServer()).geregistreerd() && VasteTempel.wijs(l) == null, "forgotten");
            helper.assertTrue(l.getBlockState(poort).isAir(), "the portal is gone");
            helper.assertTrue(l.getBlockState(m.below()).is(Blocks.GRASS_BLOCK) && l.getBlockState(m.offset(20, -3, -20)).is(Blocks.DIRT),
                    "the old ground is back");
        } finally {
            if (VasteTempel.get(l.getServer()).geregistreerd()) {
                VasteTempel.ongedaan(l.getServer());
            }
        }
        helper.succeed();
    }
}
