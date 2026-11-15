package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
            if (g.id().equals("ambachtseiland")) {
                continue;   // 1.10.4: the craft islet hangs on Knuffelkamp by its bridge; the camp's portal (~90 blocks) serves it
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

    // ---- 1.10.4: stamp patches (WereldStempel) ------------------------------------------------------------------------------------

    private static WereldStempel.Stempel stempel(ServerLevel l, List<BlockPos> plekken, List<BlockState> oud, List<BlockState> nieuw,
                                                 java.util.Map<Long, net.minecraft.nbt.CompoundTag> nbt, WereldStempel.Beleid b) {
        List<BlockState> palet = new ArrayList<>();
        int[] pos = new int[plekken.size() * 3], o = new int[plekken.size()], n = new int[plekken.size()];
        for (int i = 0; i < plekken.size(); i++) {
            pos[3 * i] = plekken.get(i).getX();
            pos[3 * i + 1] = plekken.get(i).getY();
            pos[3 * i + 2] = plekken.get(i).getZ();
            if (!palet.contains(oud.get(i))) {
                palet.add(oud.get(i));
            }
            if (!palet.contains(nieuw.get(i))) {
                palet.add(nieuw.get(i));
            }
            o[i] = palet.indexOf(oud.get(i));
            n[i] = palet.indexOf(nieuw.get(i));
        }
        return new WereldStempel.Stempel("test", l.dimension(), "test", b, null, List.copyOf(palet), pos, o, n, nbt);
    }

    /**
     * A small stamp: dry run counts, --echt places over the generated block / air / water but never over a player block or a block
     * entity, writes the backup first, loads a sign text, never generates a chunk (a cell in an unknown chunk is reported), runs
     * in steps (2 cells per tick) and herstel undoes it.
     */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void stempelProefEchtHerstel(GameTestHelper helper) throws Exception {
        ServerLevel l = helper.getLevel();
        BlockPos m = new BlockPos(57016, 120, 57016);
        l.getChunk(m);
        for (BlockPos b : BlockPos.betweenClosed(m.offset(-1, -1, -1), m.offset(7, 2, 1))) {
            l.setBlock(b, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        WereldStempel.Stempel echtPad = WereldStempel.laad("kamp_pad").orElseThrow();
        var bordNbt = echtPad.nbt().values().iterator().next();
        BlockState bord = Blocks.CHERRY_SIGN.defaultBlockState();
        BlockState lucht = Blocks.AIR.defaultBlockState(), water = Blocks.WATER.defaultBlockState(), steen = Blocks.STONE.defaultBlockState();
        BlockPos a = m, w = m.offset(1, 0, 0), speler = m.offset(2, 0, 0), sign = m.offset(3, 0, 0), kist = m.offset(4, 0, 0), goed = m.offset(5, 0, 0);
        BlockPos ver = new BlockPos(9_000_016, 120, 9_000_016);       // a chunk that was never generated
        l.setBlock(w, water, 2 | 16);
        l.setBlock(speler, Blocks.OAK_PLANKS.defaultBlockState(), 2 | 16);
        l.setBlock(kist, Blocks.CHEST.defaultBlockState(), 2 | 16);
        l.setBlock(goed, steen, 2 | 16);
        WereldStempel.Stempel s = stempel(l, List.of(a, w, speler, sign, kist, goed, ver), List.of(lucht, water, lucht, lucht, lucht, lucht, lucht),
                List.of(steen, Blocks.CALCITE.defaultBlockState(), steen, bord, steen, steen, steen), java.util.Map.of(sign.asLong(), bordNbt),
                WereldStempel.Beleid.ZEE);
        helper.assertTrue(!l.hasChunk(ver.getX() >> 4, ver.getZ() >> 4), "the far chunk is not loaded");

        WereldStempel.Uitslag proef = WereldStempel.toepassen(l, s, false);
        helper.assertTrue(proef.fout() == null && proef.backup() == null, "dry run: " + proef.fout());
        helper.assertTrue(proef.plaatsen() == 3 && proef.alGoed() == 1 && proef.bezet() == 2 && proef.nietGegenereerd() == 1 && proef.chunksNietGegenereerd() == 1,
                "dry run place/ok/occupied/not generated = " + proef.plaatsen() + "/" + proef.alGoed() + "/" + proef.bezet() + "/" + proef.nietGegenereerd()
                        + " (want 3/1/2/1)");
        helper.assertTrue(l.getBlockState(a).isAir() && l.getBlockState(w).is(Blocks.WATER), "the dry run changed nothing");

        // --echt in steps of 2 cells per tick
        WereldStempel.Klus k = new WereldStempel.Klus(l, s, true, null, 2);
        int stappen = 0;
        while (!k.stap()) {
            stappen++;
        }
        WereldStempel.Uitslag echt = k.uitslag();
        try {
            helper.assertTrue(stappen >= 2, "it ran in steps: " + stappen);
            helper.assertTrue(echt.fout() == null && echt.backup() != null && echt.plaatsen() == 3, "echt: " + echt.fout() + " " + echt.plaatsen());
            helper.assertTrue(Files.isRegularFile(WereldPatch.map(l.getServer()).resolve(echt.backup())), "backup file " + echt.backup());
            helper.assertTrue(l.getBlockState(a).is(Blocks.STONE) && l.getBlockState(w).is(Blocks.CALCITE) && l.getBlockState(sign).is(Blocks.CHERRY_SIGN),
                    "placed over air, water and air");
            helper.assertTrue(l.getBlockState(speler).is(Blocks.OAK_PLANKS), "the player planks stay");
            helper.assertTrue(l.getBlockState(kist).is(Blocks.CHEST) && l.getBlockEntity(kist) != null, "the chest (a block entity) stays");
            helper.assertTrue(l.getBlockEntity(sign) instanceof net.minecraft.world.level.block.entity.SignBlockEntity be && be.isWaxed()
                    && !be.getFrontText().getMessage(1, false).getString().isEmpty(), "the sign got its text");
            helper.assertTrue(!l.hasChunk(ver.getX() >> 4, ver.getZ() >> 4), "no chunk was generated");
            // herstel: back to air / water
            WereldPatch.Herstel h = WereldPatch.herstel(l.getServer(), echt.backup());
            helper.assertTrue(h.fout() == null && h.teruggezet() == 3 && h.overgeslagen() == 0, "herstel " + h.fout() + " " + h.teruggezet() + "/" + h.overgeslagen());
            helper.assertTrue(l.getBlockState(a).isAir() && l.getBlockState(w).is(Blocks.WATER) && l.getBlockState(sign).isAir(), "the old blocks are back");
            helper.assertTrue(l.getBlockState(goed).is(Blocks.STONE) && l.getBlockState(kist).is(Blocks.CHEST), "untouched cells stay");
        } finally {
            if (echt.backup() != null) {
                Files.deleteIfExists(WereldPatch.map(l.getServer()).resolve(echt.backup()));
            }
        }
        helper.succeed();
    }

    /** The two shipped stamps load, sit where they belong and never replace a block entity. */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 100)
    public static void ambachtStempelsLaden(GameTestHelper helper) {
        WereldStempel.Stempel eiland = WereldStempel.laad("ambachtseiland").orElse(null);
        WereldStempel.Stempel pad = WereldStempel.laad("kamp_pad").orElse(null);
        helper.assertTrue(eiland != null && pad != null, "both stamps load");
        helper.assertTrue(WereldPatch.laad("ambachtseiland").isEmpty() && WereldPatch.NAMEN.containsAll(WereldPatch.STEMPELS), "stamps are registered, not boxes");
        helper.assertTrue(eiland.dim().equals(Vadsrijk.DIM) && pad.dim().equals(Vadsrijk.DIM), "in the Vadsrijk");
        helper.assertTrue(eiland.cellen() > 50_000 && eiland.cellen() > WereldPatch.MAX_CELLEN, "the islet is a big stamp: " + eiland.cellen());
        helper.assertTrue(eiland.beleid() == WereldStempel.Beleid.ZEE && pad.beleid() == WereldStempel.Beleid.KAMP_PAD, "policies");
        for (int i = 0; i < eiland.cellen(); i++) {
            BlockPos b = eiland.plek(i);
            helper.assertTrue(b.getX() >= -704 && b.getX() <= -476 && b.getZ() >= 940 && b.getZ() <= 1110, "islet cell outside its box: " + b.toShortString());
            helper.assertTrue(!eiland.oud(i).hasBlockEntity(), "the islet replaces no block entity: " + eiland.oud(i));
        }
        int borden = 0;
        for (int i = 0; i < pad.cellen(); i++) {
            BlockPos b = pad.plek(i);
            helper.assertTrue(b.getX() > -476 && b.getX() <= -380 && Math.abs(b.getZ() - 1024) <= 12, "path cell outside its corridor: " + b.toShortString());
            helper.assertTrue(!pad.oud(i).hasBlockEntity(), "the path replaces no block entity: " + pad.oud(i));
            if (pad.nieuw(i).is(BlockTags.STANDING_SIGNS)) {
                borden++;
                helper.assertTrue(pad.nbt().containsKey(b.asLong()), "a sign with text at " + b.toShortString());
            }
        }
        helper.assertTrue(borden == 2, "two direction signs: " + borden);
        helper.succeed();
    }

    /**
     * The real kamp_pad stamp, moved into the test world over its generated states: a chest and a barrel put on the path and planks
     * where a tree trunk stood stay; everything else becomes the path; herstel brings the trees back.
     */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 300)
    public static void kampPadRaaktGeenBlokEntiteiten(GameTestHelper helper) throws Exception {
        ServerLevel l = helper.getLevel();
        WereldStempel.Stempel echtPad = WereldStempel.laad("kamp_pad").orElseThrow();
        BlockPos lo = new BlockPos(-476, 80, 1010);
        BlockPos d = new BlockPos(58016, 40, 58016).subtract(lo);
        WereldStempel.Stempel s = echtPad.verschoven(d, l.dimension());
        for (int i = 0; i < s.cellen(); i++) {
            l.getChunk(s.plek(i));
            l.setBlock(s.plek(i), s.oud(i), 2 | 16);
        }
        // a chest on a path cell, a barrel on an edge cell, and planks in a trunk cell
        int kistI = -1, tonI = -1, plankI = -1;
        for (int i = 0; i < s.cellen(); i++) {
            if (kistI < 0 && s.nieuw(i).is(Blocks.DIRT_PATH)) {
                kistI = i;
            } else if (tonI < 0 && s.nieuw(i).is(Blocks.STONE_BRICKS)) {
                tonI = i;
            } else if (plankI < 0 && s.oud(i).is(BlockTags.LOGS)) {
                plankI = i;
            }
        }
        helper.assertTrue(kistI >= 0 && tonI >= 0 && plankI >= 0, "path, edge and trunk cells exist");
        l.setBlock(s.plek(kistI), Blocks.CHEST.defaultBlockState(), 2 | 16);
        l.setBlock(s.plek(tonI), Blocks.BARREL.defaultBlockState(), 2 | 16);
        l.setBlock(s.plek(plankI), Blocks.OAK_PLANKS.defaultBlockState(), 2 | 16);
        WereldStempel.Uitslag u = WereldStempel.toepassen(l, s, true);
        try {
            helper.assertTrue(u.fout() == null && u.bezet() == 3 && u.plaatsen() == s.cellen() - 3 && u.nietGegenereerd() == 0,
                    "all but 3 placed: " + u.fout() + " " + u.plaatsen() + "/" + s.cellen() + ", bezet " + u.bezet());
            helper.assertTrue(l.getBlockState(s.plek(kistI)).is(Blocks.CHEST) && l.getBlockEntity(s.plek(kistI)) != null, "the chest stays");
            helper.assertTrue(l.getBlockState(s.plek(tonI)).is(Blocks.BARREL) && l.getBlockEntity(s.plek(tonI)) != null, "the barrel stays");
            helper.assertTrue(l.getBlockState(s.plek(plankI)).is(Blocks.OAK_PLANKS), "the planks stay");
            int fout = 0;
            for (int i = 0; i < s.cellen(); i++) {
                if (i != kistI && i != tonI && i != plankI && l.getBlockState(s.plek(i)) != s.nieuw(i)) {
                    fout++;
                }
            }
            helper.assertTrue(fout == 0, fout + " cells are not the path");
            WereldPatch.Herstel h = WereldPatch.herstel(l.getServer(), u.backup());
            helper.assertTrue(h.fout() == null && h.teruggezet() == s.cellen() - 3, "herstel " + h.fout() + " " + h.teruggezet());
            helper.assertTrue(l.getBlockState(s.plek(kistI)).is(Blocks.CHEST), "the chest is still there after herstel");
        } finally {
            if (u.backup() != null) {
                Files.deleteIfExists(WereldPatch.map(l.getServer()).resolve(u.backup()));
            }
        }
        helper.succeed();
    }
}
