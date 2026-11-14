package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3: the one-time world patches ({@link WereldPatch}, {@code /guhs vadsrijk patch}). A dry run only counts, --echt places only
 * into air / replaceable blocks and writes a backup first, herstel puts the old blocks back (not over a later change). And the
 * shipped "spiegelzaal" patch: 22 boxes / 98 cells, all in the outer wall ring of the Spiegelzaal. The return portals: every safe town
 * has one inside its area, and a portal patch builds a whole, awake portal on a plaza floor (herstel takes it away again).
 * Run: {@code -Pgt=VadsrijkPatch}.
 */
public class VadsrijkPatchGameTests {
    private static final String B = "vadsrijk_patch";

    private static WereldPatch.Patch testPatch(ServerLevel l, BlockPos m) {
        String dim = l.dimension().identifier().toString();
        String json = "{\"dimensie\":\"" + dim + "\",\"beschrijving\":\"test\",\"dozen\":["
                + "{\"naam\":\"muur\",\"min\":[" + m.getX() + "," + m.getY() + "," + m.getZ() + "],\"max\":[" + (m.getX() + 2) + "," + (m.getY() + 2) + ","
                + m.getZ() + "],\"blok\":\"minecraft:calcite\"},"
                + "{\"naam\":\"voet\",\"min\":[" + m.getX() + "," + (m.getY() - 1) + "," + (m.getZ() + 2) + "],\"max\":[" + (m.getX() + 2) + "," + (m.getY() - 1)
                + "," + (m.getZ() + 2) + "],\"blok\":\"minecraft:tuff_bricks\"}]}";
        return WereldPatch.lees("test", JsonParser.parseString(json).getAsJsonObject());
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void proefEchtHerstel(GameTestHelper helper) throws Exception {
        ServerLevel l = helper.getLevel();
        BlockPos m = new BlockPos(56016, 120, 56016);
        l.getChunk(m);
        for (BlockPos b : BlockPos.betweenClosed(m.offset(-1, -2, -1), m.offset(3, 3, 3))) {
            l.setBlock(b, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        // the "muur" box (3 x 3 = 9 cells): a player's plank, a short grass, one already calcite; the "voet" row (3): one stone
        BlockPos plank = m.offset(1, 1, 0), gras = m.offset(0, 0, 0), alGoed = m.offset(2, 2, 0), steen = m.offset(1, -1, 2);
        l.setBlock(m.offset(0, -1, 0), Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);   // ground for the grass (it must survive)
        l.setBlock(plank, Blocks.OAK_PLANKS.defaultBlockState(), 2 | 16);
        l.setBlock(gras, Blocks.SHORT_GRASS.defaultBlockState(), 2 | 16);
        l.setBlock(alGoed, Blocks.CALCITE.defaultBlockState(), 2 | 16);
        l.setBlock(steen, Blocks.STONE.defaultBlockState(), 2 | 16);
        WereldPatch.Patch p = testPatch(l, m);
        helper.assertTrue(p.dozen().size() == 2 && p.cellen() == 12, "12 cells in 2 boxes: " + p.cellen());

        // 1. dry run: counts only
        WereldPatch.Uitslag proef = WereldPatch.toepassen(l, p, false);
        helper.assertTrue(proef.fout() == null && proef.backup() == null, "dry run without backup: " + proef.fout());
        helper.assertTrue(proef.plaatsen() == 9 && proef.alGoed() == 1 && proef.bezet() == 2,
                "dry run counts place/already/occupied = " + proef.plaatsen() + "/" + proef.alGoed() + "/" + proef.bezet() + " (want 9/1/2)");
        helper.assertTrue(proef.tellingen().get(0).plaatsen() == 7 && proef.tellingen().get(1).plaatsen() == 2, "per box 7 + 2");
        helper.assertTrue(l.getBlockState(m.offset(1, 0, 0)).isAir() && l.getBlockState(gras).is(Blocks.SHORT_GRASS), "the dry run changed nothing");

        // 2. --echt: backup first, only air/replaceable cells
        WereldPatch.Uitslag echt = WereldPatch.toepassen(l, p, true);
        try {
            helper.assertTrue(echt.fout() == null && echt.backup() != null && echt.plaatsen() == 9, "echt: " + echt.fout() + " " + echt.plaatsen());
            helper.assertTrue(Files.isRegularFile(WereldPatch.map(l.getServer()).resolve(echt.backup())), "backup file " + echt.backup());
            helper.assertTrue(WereldPatch.backups(l.getServer()).contains(echt.backup()), "listed for herstel");
            helper.assertTrue(l.getBlockState(m.offset(1, 0, 0)).is(Blocks.CALCITE) && l.getBlockState(gras).is(Blocks.CALCITE)
                    && l.getBlockState(m.offset(0, -1, 2)).is(Blocks.TUFF_BRICKS), "placed into air and over the grass");
            helper.assertTrue(l.getBlockState(plank).is(Blocks.OAK_PLANKS) && l.getBlockState(steen).is(Blocks.STONE), "the plank and the stone stay");
            // a second run has nothing left to do
            WereldPatch.Uitslag nog = WereldPatch.toepassen(l, p, false);
            helper.assertTrue(nog.plaatsen() == 0 && nog.alGoed() == 10 && nog.bezet() == 2, "after: " + nog.plaatsen() + "/" + nog.alGoed() + "/" + nog.bezet());

            // 3. herstel: back to air/grass, except a cell somebody changed after the patch
            BlockPos later = m.offset(2, 0, 0);
            l.setBlock(later, Blocks.GLASS.defaultBlockState(), 2 | 16);
            WereldPatch.Herstel h = WereldPatch.herstel(l.getServer(), echt.backup());
            helper.assertTrue(h.fout() == null && h.teruggezet() == 8 && h.overgeslagen() == 1, "herstel " + h.fout() + " " + h.teruggezet() + "/" + h.overgeslagen());
            helper.assertTrue(l.getBlockState(m.offset(1, 0, 0)).isAir() && l.getBlockState(gras).is(Blocks.SHORT_GRASS)
                    && l.getBlockState(m.offset(0, -1, 2)).isAir(), "the old blocks are back");
            helper.assertTrue(l.getBlockState(later).is(Blocks.GLASS), "the later change stays");
            helper.assertTrue(l.getBlockState(plank).is(Blocks.OAK_PLANKS) && l.getBlockState(steen).is(Blocks.STONE) && l.getBlockState(alGoed).is(Blocks.CALCITE),
                    "untouched cells stay");
            // a bad file name is refused (no paths outside world/data)
            helper.assertTrue(WereldPatch.herstel(l.getServer(), "../level.dat").fout() != null, "path refused");
        } finally {
            if (echt.backup() != null) {
                Files.deleteIfExists(WereldPatch.map(l.getServer()).resolve(echt.backup()));
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 100)
    public static void spiegelzaalPatchInDeMuur(GameTestHelper helper) {
        WereldPatch.Patch p = WereldPatch.laad("spiegelzaal").orElse(null);
        helper.assertTrue(p != null, "the spiegelzaal patch loads");
        helper.assertTrue(p.dim().equals(Vadsrijk.DIM), "in the Vadsrijk: " + p.dim());
        helper.assertTrue(p.dozen().size() == 22 && p.cellen() == 98, "22 boxes / 98 cells: " + p.dozen().size() + " / " + p.cellen());
        BlockPos hal = Z2WereldPlekken.plek("arena_spiegelzucht");
        int hr = 22;   // bouw2.spiegelburcht: PL.SPIEGELZAAL radius; the outer wall reaches hr + 3
        for (WereldPatch.Doos d : p.dozen()) {
            helper.assertTrue(d.blok().is(Blocks.CALCITE) || d.blok().is(Blocks.TUFF_BRICKS), "the hall's own wall blocks: " + d.naam());
            for (BlockPos b : BlockPos.betweenClosed(d.min(), d.max())) {
                double r = Math.hypot(b.getX() - hal.getX(), b.getZ() - hal.getZ());
                helper.assertTrue(r > hr + 1.5 && r <= hr + 3, d.naam() + " " + b.toShortString() + " is not in the wall ring (r " + r + ")");
                helper.assertTrue(b.getY() >= hal.getY() && b.getY() <= hal.getY() + 6, "niche height: " + b.toShortString());
                helper.assertTrue(b.getY() <= hal.getY() + 2 ? d.blok().is(Blocks.TUFF_BRICKS) : d.blok().is(Blocks.CALCITE), "wall course at " + b.toShortString());
            }
        }
        helper.succeed();
    }

    /** Every safe area (kern stad = true) has a return portal POI inside it; a district inside another safe area counts with it. */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 100)
    public static void elkeVeiligeZoneHeeftEenPortaal(GameTestHelper helper) {
        List<BlockPos> portalen = new ArrayList<>(TerugportaalPlekken.ALLE.values());
        WereldPlekken.ALLE.forEach((id, pos) -> {
            if (id.startsWith("terugportaal_")) {
                portalen.add(pos);
            }
        });
        helper.assertTrue(TerugportaalPlekken.PORTALEN.size() == WereldPatch.PORTALEN.size(),
                "one patch per portal in terugportalen.txt: " + TerugportaalPlekken.PORTALEN.size() + " / " + WereldPatch.PORTALEN.size());
        List<Gebieden.Gebied> steden = Gebieden.alle().stream().filter(Gebieden.Gebied::stad).toList();
        helper.assertTrue(steden.size() >= 10, "the safe areas are registered: " + steden.size());
        List<String> zonder = new ArrayList<>();
        for (Gebieden.Gebied g : steden) {
            if (g.id().equals("bondenvallei")) {
                continue;   // the guild valley: stamped at runtime, its own gate back to Hoogvads
            }
            boolean deel = steden.stream().anyMatch(o -> o != g && o.minX() <= g.minX() && o.maxX() >= g.maxX() && o.minZ() <= g.minZ() && o.maxZ() >= g.maxZ());
            if (deel) {
                continue;   // a district (z5_marktlaan in z5_hoogvads): the town's portal serves it
            }
            if (portalen.stream().noneMatch(p -> p.getX() >= g.minX() && p.getX() <= g.maxX() && p.getZ() >= g.minZ() && p.getZ() <= g.maxZ())) {
                zonder.add(g.id());
            }
        }
        helper.assertTrue(zonder.isEmpty(), "safe areas without a return portal: " + zonder);
        // every portal patch: its POI = the txt's, inside its own area, and it ships 36 awake portal blocks
        for (String naam : WereldPatch.PORTALEN) {
            WereldPatch.Patch p = WereldPatch.laad(naam).orElse(null);
            helper.assertTrue(p != null && p.poi() != null && p.gebied() != null, naam + " loads with a POI and an area");
            helper.assertTrue(p.poi().equals(TerugportaalPlekken.plek(p.poiId())), naam + ": POI " + p.poiId() + " matches terugportalen.txt");
            Gebieden.Gebied g = Gebieden.van(p.gebied());
            helper.assertTrue(g != null && p.poi().getX() >= g.minX() && p.poi().getX() <= g.maxX() && p.poi().getZ() >= g.minZ() && p.poi().getZ() <= g.maxZ(),
                    naam + ": POI " + p.poi().toShortString() + " inside " + p.gebied());
            int poort = p.dozen().stream().filter(d -> d.blok().getBlock() instanceof PortaalBlok && d.blok().getValue(PortaalBlok.WAKKER))
                    .mapToInt(WereldPatch.Doos::cellen).sum();
            helper.assertTrue(poort == 36, naam + ": 36 awake portal blocks, not " + poort);
        }
        helper.succeed();
    }

    /** A portal patch (Kandijkom's), moved onto a plaza floor in the test world: a whole, awake portal in a closed frame; herstel undoes it. */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void portaalPatchBouwtEenWerkendPortaal(GameTestHelper helper) throws Exception {
        ServerLevel l = helper.getLevel();
        WereldPatch.Patch echtP = WereldPatch.laad("portaal_kandijkom").orElseThrow();
        BlockPos lo = echtP.dozen().stream().map(WereldPatch.Doos::min).reduce(BlockPos::min).orElseThrow();
        BlockPos doel = new BlockPos(60016, 120, 60016);
        BlockPos d = doel.subtract(lo);
        List<WereldPatch.Doos> dozen = new ArrayList<>();
        for (WereldPatch.Doos x : echtP.dozen()) {
            dozen.add(new WereldPatch.Doos(x.naam(), x.min().offset(d), x.max().offset(d), x.blok(), x.vervang()));
        }
        WereldPatch.Patch p = new WereldPatch.Patch("test_portaal", l.dimension(), "test", List.copyOf(dozen), null, null, null);
        BlockPos hi = dozen.stream().map(WereldPatch.Doos::max).reduce(BlockPos::max).orElseThrow();
        l.getChunk(doel);
        l.getChunk(hi);
        BlockState vloer = WereldPatch.staat("guhs:parelmoer_tegels");
        for (BlockPos b : BlockPos.betweenClosed(doel.offset(-2, 0, -2), new BlockPos(hi.getX() + 2, hi.getY() + 1, hi.getZ() + 2))) {
            l.setBlock(b, b.getY() == doel.getY() ? vloer : Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        WereldPatch.Uitslag u = WereldPatch.toepassen(l, p, true);
        helper.assertTrue(u.fout() == null && u.bezet() == 0 && u.plaatsen() == p.cellen(), "every cell placed: " + u.fout() + " " + u.plaatsen() + "/" + p.cellen());
        helper.runAfterDelay(5, () -> {
            try {
                // the portal blocks: awake, and every in-plane neighbour is portal or frame (a closed frame)
                List<BlockPos> poort = new ArrayList<>();
                for (WereldPatch.Doos x : dozen) {
                    if (x.blok().getBlock() instanceof PortaalBlok) {
                        BlockPos.betweenClosed(x.min(), x.max()).forEach(b -> poort.add(b.immutable()));
                    }
                }
                helper.assertTrue(poort.size() == 36, "36 portal cells");
                Direction.Axis as = l.getBlockState(poort.get(0)).getValue(PortaalBlok.AXIS);
                for (BlockPos b : poort) {
                    BlockState s = l.getBlockState(b);
                    helper.assertTrue(s.getBlock() instanceof PortaalBlok && s.getValue(PortaalBlok.WAKKER) && s.getValue(PortaalBlok.AXIS) == as,
                            "awake portal at " + b.toShortString() + ": " + s);
                    for (Direction r : Direction.values()) {
                        if (r.getAxis() != Direction.Axis.Y && r.getAxis() != as) {
                            continue;
                        }
                        BlockState n = l.getBlockState(b.relative(r));
                        helper.assertTrue(n.getBlock() instanceof PortaalBlok || n.is(WereldRegistry.POORTSTEEN.get()),
                                "closed frame next to " + b.toShortString() + " (" + r + "): " + n);
                    }
                }
                // herstel: the plaza floor and the air come back
                WereldPatch.Herstel h = WereldPatch.herstel(l.getServer(), u.backup());
                helper.assertTrue(h.fout() == null && h.teruggezet() == p.cellen() && h.overgeslagen() == 0,
                        "herstel " + h.fout() + " " + h.teruggezet() + "/" + h.overgeslagen());
                helper.assertTrue(poort.stream().allMatch(b -> l.getBlockState(b).isAir()), "the portal is gone");
                helper.assertTrue(l.getBlockState(doel).is(vloer.getBlock()), "the plaza floor is back");
                helper.succeed();
            } finally {
                try {
                    Files.deleteIfExists(WereldPatch.map(l.getServer()).resolve(u.backup()));
                } catch (Exception e) {
                    // (a leftover test backup is harmless)
                }
            }
        });
    }
}
