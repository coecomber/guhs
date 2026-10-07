package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.biomeguhs.Biomeguhs;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * Game tests of the einde2 skeleton: the structure type guhs:leegte_bouw on the test build {@code einde2_test_bouw}
 * (tools/features/einde2.py: 3 x 2 tiles, in no structure set), {@link Plekken} (sidecar to world and back, for a
 * leegte_bouw and for a jigsaw piece, the guaranteed copy, the overlays), the deep band (a tile that empties its box and
 * keeps its template's shapes, the terrain-free box, the pair that stands next to each other, the void of ten seeds),
 * protection, discovery, the hooks and the wiring (pages, compass, stand-in entities). The Guheinde's generator comes from
 * {@link Einde2Test}.
 */
public class E2KernGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_kern";
    private static final String BOUW = "einde2_test_bouw";
    private static final long SEED = 21400050L;

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    /** The type: where the build lands, its tiles, its rim, no start near 0,0, and its pieces survive saving. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2KernLeegteBouw(GameTestHelper helper) {
        Einde2Test.Wereld w = Einde2Test.wereld(helper, SEED);
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), BOUW);
        helper.assertTrue(bouw != null, "einde2_test_bouw is a guhs:leegte_bouw");
        helper.assertTrue(Einde2Test.start(w, BOUW, new ChunkPos(3, -2)) == null, "never near the main island (min_distance)");
        ChunkPos chunk = new ChunkPos(100, -90);
        StructureStart start = Einde2Test.start(w, BOUW, chunk);
        helper.assertTrue(start != null, "it starts far from 0,0, whatever the terrain");
        BlockPos o = bouw.oorsprong(chunk);
        helper.assertTrue(o.getX() == chunk.getMiddleBlockX() - bouw.anchor().getX() && o.getY() == bouw.y()
                && o.getZ() == chunk.getMiddleBlockZ() - bouw.anchor().getZ(), "the anchor lands on the middle of the start chunk: " + o);
        helper.assertTrue(Math.floorMod(o.getX(), 16) == 0 && Math.floorMod(o.getZ(), 16) == 0, "tiles line up with chunks: " + o);
        BoundingBox doos = bouw.doos(chunk);
        helper.assertTrue(doos.getXSpan() == 80 && doos.getYSpan() == 12 && doos.getZSpan() == 48, "the box is the build: " + doos);
        helper.assertTrue(doos.minY() >= Einde2Ids.HOOG_MIN && doos.maxY() <= Einde2Ids.HOOG_MAX, "in the high band: " + doos);
        int tiles = 0, randen = 0;
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox b = piece.getBoundingBox();
            if (piece instanceof LeegteBouwStructure.Stuk) {
                tiles++;
                int i = (b.minX() - o.getX()) / 32, j = (b.minZ() - o.getZ()) / 32;
                helper.assertTrue(b.minX() == o.getX() + i * 32 && b.minZ() == o.getZ() + j * 32 && b.minY() == o.getY()
                        && b.getXSpan() == (i == 2 ? 16 : 32) && b.getZSpan() == (j == 1 ? 16 : 32), "tile " + i + "," + j + " never turned: " + b);
            } else if (piece instanceof LeegteBouwStructure.Rand) {
                randen++;
                helper.assertTrue(b.equals(doos.inflatedBy(48, 0, 48)), "the rim: the box grown by rand in x and z only: " + b);
            }
        }
        helper.assertTrue(tiles == 6 && randen == 1 && start.getPieces().get(0) instanceof LeegteBouwStructure.Stuk, "3 x 2 tiles and one rim: " + tiles + ", " + randen);
        helper.assertTrue(bouw.keepClear() == Math.max(Math.max(bouw.anchor().getX(), 79 - bouw.anchor().getX()),
                Math.max(bouw.anchor().getZ(), 47 - bouw.anchor().getZ())) + 1 + bouw.rand(), "keep_clear = reach + 1 + rand: " + bouw.keepClear());
        // saved and loaded again: the same pieces
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        CompoundTag tag = start.createTag(context, chunk);
        StructureStart terug = StructureStart.loadStaticStart(context, tag, SEED);
        helper.assertTrue(terug != null && terug.getPieces().size() == start.getPieces().size(), "the pieces load again");
        for (int k = 0; k < start.getPieces().size(); k++) {
            helper.assertTrue(terug.getPieces().get(k).getClass() == start.getPieces().get(k).getClass()
                    && terug.getPieces().get(k).getBoundingBox().equals(start.getPieces().get(k).getBoundingBox()), "piece " + k + " is the same after loading");
        }
        // a marker of the sidecar -> the world, by a plain shift
        Plekken.Plek guh = Plekken.plek(BOUW, "proefguh");
        helper.assertTrue(guh != null && o.offset(guh.pos()).equals(Plekken.wereld(start, guh)), "a marker is origin + its pos");
        helper.succeed();
    }

    /** The sidecar: every kind of marker and the overlay come back as they were recorded. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernPlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(BOUW);
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().equals(new BlockPos(80, 12, 48)), "the sidecar of the test build: " + blad.grootte());
        Plekken.Plek guh = Plekken.plek(BOUW, "proefguh");
        helper.assertTrue(guh != null && guh.soort().equals(Plekken.NPC) && guh.pos().equals(new BlockPos(40, 3, 20)) && guh.yaw() == 90f
                && "PROEFGUH".equals(guh.kind()) && guh.wie().equals("een proefguh"), "an npc spot: " + guh);
        Plekken.Plek zone = Plekken.plek(BOUW, "aankomst");
        helper.assertTrue(zone != null && zone.soort().equals(Plekken.ZONE) && zone.pos().equals(new BlockPos(2, 3, 2))
                && new BlockPos(12, 6, 12).equals(zone.tot()), "a zone: " + zone);
        Plekken.Plek pad = Plekken.plek(BOUW, "route");
        helper.assertTrue(pad != null && pad.soort().equals(Plekken.PAD) && pad.punten().size() == 3 && pad.punten().get(2).equals(new BlockPos(72, 3, 40))
                && pad.pos().equals(pad.punten().get(0)), "a pad: " + pad);
        Plekken.Plek ring = Plekken.plek(BOUW, "ring_01");
        helper.assertTrue(ring != null && ring.soort().equals(Plekken.RING) && ring.as() == Direction.Axis.X && ring.straal() == 4, "a ring: " + ring);
        helper.assertTrue(Plekken.plek(BOUW, "uitzicht") != null && Plekken.plek(BOUW, "bestaat_niet") == null && Plekken.van("bestaat_niet").isEmpty(),
                "unknown markers and structures are simply not there");
        Plekken.Laag laag = blad.lagen().get("proef");
        helper.assertTrue(laag != null && laag.template().equals(Guhs.id(BOUW + "/laag_proef")) && laag.pos().equals(new BlockPos(50, 3, 10))
                && laag.grootte().equals(new BlockPos(3, 3, 3)), "the overlay: " + laag);
        // the overlay itself: placed and taken away again
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Plekken.laag(level, laag, at, false), "the overlay is placed");
        helper.assertTrue(level.getBlockState(at.offset(1, 1, 1)).getBlock() == BuiltInRegistries.BLOCK.getValue(Guhs.id("guh_kristal_lamp"))
                && !level.getBlockState(at).isAir(), "its blocks are there");
        helper.assertTrue(Plekken.laag(level, laag, at, true), "the overlay is taken away");
        helper.assertTrue(level.getBlockState(at.offset(1, 1, 1)).isAir() && level.getBlockState(at).isAir(), "its blocks are air again");
        helper.succeed();
    }

    /** A jigsaw piece: a marker local to the piece lands where the piece is (the Terugpoort's anchor: on the start). */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 1200)
    public static void einde2KernJigsawPlek(GameTestHelper helper) {
        Einde2Test.Wereld w = Einde2Test.wereld(helper, SEED);
        StructureSet set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("guheinde_terugpoort"));
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement, "the Terugpoort's set");
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
        StructureStart start = null;
        ChunkPos chunk = null;
        int s = spread.spacing(), geprobeerd = 0;
        for (int rx = 3; rx < 12 && start == null; rx++) {
            for (int rz = -6; rz < 6 && start == null; rz++) {
                chunk = spread.getPotentialStructureChunk(SEED, rx * s, rz * s);
                geprobeerd++;
                start = Einde2Test.start(w, "guheinde_terugpoort", chunk);
            }
        }
        helper.assertTrue(start != null, "a Terugpoort starts somewhere on the outer islands (" + geprobeerd + " spots tried)");
        PoolElementStructurePiece stuk = (PoolElementStructurePiece) start.getPieces().get(0);
        // (tools/features/guheinde_bouw.py: the anchor guheinde_terugpoort_midden sits at 5, 4, 5 of the template)
        Plekken.Plek anker = new Plekken.Plek("anker", Plekken.PROP, new BlockPos(5, 4, 5), 0f, null, List.of(), "guheinde_terugpoort", null, null, 0, null, "");
        BlockPos at = Plekken.wereld(start, anker);
        helper.assertTrue(at != null && stuk.getBoundingBox().isInside(at), "the marker lies in its piece: " + at + " in " + stuk.getBoundingBox());
        helper.assertTrue(at.getX() == chunk.getMinBlockX() && at.getZ() == chunk.getMinBlockZ(), "the anchor is where the structure starts (rotation "
                + stuk.getRotation() + "): " + at + " vs " + chunk);
        helper.assertTrue(Plekken.alle(start, anker).equals(List.of(at)), "one such piece, one spot");
        Plekken.Plek elders = new Plekken.Plek("elders", Plekken.PROP, BlockPos.ZERO, 0f, null, List.of(), "einde2_vesting/bestaat_niet", null, null, 0, null, "");
        helper.assertTrue(Plekken.wereld(start, elders) == null && Plekken.alle(start, elders).isEmpty(), "a piece this copy doesn't have: no spot");
        helper.succeed();
    }

    /**
     * The guaranteed copy of a big place, in a Guheinde that is one outer biome everywhere (the dimension itself keeps its one
     * biome until the biomes slice): the stand-in Leegtehaven and Leeuwschildpad-eiland are found in their rings, really
     * start there, and {@link Plekken} gives the same spot.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2KernGegarandeerd(GameTestHelper helper) {
        Einde2Test.Wereld w = Einde2Test.wereld(helper, SEED, Einde2Ids.GATENKAASHEUVELS);
        helper.assertTrue(Einde2Test.biome(w, 2000, 2000).is(Einde2Ids.BUITENEILANDEN), "the stand-in biomes are in guhs:einde2/buiteneilanden");
        for (String id : List.of(Einde2Ids.LEEGTEHAVEN, Einde2Ids.LEEUWSCHILDPAD_EILAND)) {
            List<String> fouten = Einde2Test.controleer(w, id);
            helper.assertTrue(fouten.isEmpty(), id + ": " + fouten);
            ChunkPos chunk = Einde2Test.gegarandeerd(w, id);
            LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), id);
            helper.assertTrue(chunk != null && chunk.equals(Plekken.startChunk(helper.getLevel().registryAccess(), w.state(), w.seed(), id)),
                    id + ": Plekken gives the ring position");
            StructureSet set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(id + "_gegarandeerd"));
            helper.assertTrue(set.placement() instanceof GegarandeerdPlacement g && g.minAfstand() >= 1200 && g.maxAfstand() <= 2800, id + ": its ring");
            helper.assertTrue(bouw.doos(chunk).minY() >= Einde2Ids.HOOG_MIN, id + ": in the high band");
        }
        // a biome-bound place has no spot in a world without its biome (and says so instead of landing somewhere else)
        helper.assertTrue(Einde2Test.gegarandeerd(w, Einde2Ids.OMASHGUH) == null, "Omashguh only in the Kristalkaasklippen");
        helper.assertTrue(!Einde2Test.controleer(w, Einde2Ids.OMASHGUH).isEmpty(), "and the check reports it");
        helper.succeed();
    }

    /**
     * A tile of the deep band: it empties its own box before it places its template (rock of a relaxed spot never sticks
     * into town) and leaves everything outside it alone, and its blocks keep the states of the template (a ladder that hangs
     * on nothing stays, where a plain placement lets it pop off): nothing breaks at a tile seam.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernRuimeTegel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var tegel = Guhs.id("einde2_test_tegel");
        helper.assertTrue(level.getStructureManager().get(tegel).isPresent(), "the test tile exists");
        for (int x = 0; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 1; z <= 3; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        LeegteBouwStructure.Stuk stuk = new LeegteBouwStructure.Stuk(level.getStructureManager(), tegel, at, true);
        BoundingBox box = stuk.getBoundingBox();
        helper.assertTrue(box.equals(BoundingBox.fromCorners(at, at.offset(2, 2, 2))), "the tile's box is its template: " + box);
        // saved and loaded again: it still empties its box
        CompoundTag tag = stuk.createTag(StructurePieceSerializationContext.fromLevel(level));
        helper.assertTrue(new LeegteBouwStructure.Stuk(level.getStructureManager(), tag).ruim()
                && !new LeegteBouwStructure.Stuk(level.getStructureManager(), tegel, at, false).ruim(), "ruim survives saving");
        stuk.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, ChunkPos.containing(at), at);
        var lamp = BuiltInRegistries.BLOCK.getValue(Guhs.id("guh_kristal_lamp"));
        int lucht = 0;
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(p);
            if (p.equals(at)) {
                helper.assertTrue(state.is(lamp), "the lamp of the template");
            } else if (p.equals(at.offset(1, 1, 1))) {
                helper.assertTrue(state.is(Blocks.LADDER), "the ladder keeps hanging (the template's shape is kept): " + state);
            } else {
                helper.assertTrue(state.isAir(), "everything else in the box is air: " + state + " at " + p);
                lucht++;
            }
        }
        helper.assertTrue(lucht == 25, "25 cells of rock emptied");
        for (int y = 0; y <= 2; y++) {
            helper.assertTrue(level.getBlockState(at.offset(-1, y, 1)).is(Blocks.STONE), "rock outside the box stays");
        }
        // the same template put down the plain way: the ladder looks at its neighbours and falls off
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(2, 2, 2))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        level.getStructureManager().get(tegel).orElseThrow().placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), 2);
        helper.assertTrue(level.getBlockState(at).is(lamp) && level.getBlockState(at.offset(1, 1, 1)).isAir(),
                "without known shapes the ladder is gone: " + level.getBlockState(at.offset(1, 1, 1)));
        // a tile of the high band leaves the world as it is
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(2, 2, 2))) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        }
        new LeegteBouwStructure.Stuk(level.getStructureManager(), tegel, at, false).postProcess(level, level.structureManager(),
                level.getChunkSource().getGenerator(), level.getRandom(), box, ChunkPos.containing(at), at);
        helper.assertTrue(level.getBlockState(at).is(lamp) && level.getBlockState(at.offset(2, 2, 2)).is(Blocks.STONE), "no ruim: unset cells stay");
        helper.succeed();
    }

    /**
     * The Sponsguh pair in a Guheinde of one outer biome: the stand-in Bikinibodem has its guaranteed copy in the deep band,
     * the stand-in Schelpenstad stands exactly at its offset from it and nowhere else ({@code bij}), and {@link Plekken}
     * answers for both.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2KernSponsPaar(GameTestHelper helper) {
        Einde2Test.Wereld w = Einde2Test.wereld(helper, SEED, Einde2Ids.GATENKAASHEUVELS);
        var access = helper.getLevel().registryAccess();
        for (String id : Einde2Ids.SPONS_STRUCTUREN) {
            List<String> fouten = Einde2Test.controleer(w, id);
            helper.assertTrue(fouten.isEmpty(), id + ": " + fouten);
        }
        LeegteBouwStructure bikini = Plekken.leegteBouw(access, Einde2Ids.BIKINIBODEM), schelp = Plekken.leegteBouw(access, Einde2Ids.SCHELPENSTAD);
        helper.assertTrue(bikini.ruim() && schelp.ruim() && bikini.bij().isEmpty() && schelp.bij().isPresent()
                && schelp.bij().get().structuur().equals(Guhs.id(Einde2Ids.BIKINIBODEM)), "both empty their box; Schelpenstad stands next to Bikinibodem");
        ChunkPos b = Einde2Test.gegarandeerd(w, Einde2Ids.BIKINIBODEM), s = Einde2Test.gegarandeerd(w, Einde2Ids.SCHELPENSTAD);
        StructureSet ring = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("bikinibodem_gegarandeerd"));
        helper.assertTrue(ring != null && ring.placement() instanceof GegarandeerdPlacement g && g.minAfstand() == 1600 && g.maxAfstand() == 3000,
                "Bikinibodem's ring");
        helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("schelpenstad_gegarandeerd")) == null
                && access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("schelpenstad")).placement() instanceof RandomSpreadStructurePlacement r
                && r.spacing() == 1 && r.separation() == 0, "Schelpenstad's one set makes every chunk a candidate");
        helper.assertTrue(schelp.oorsprong(s).equals(bikini.oorsprong(b).offset(224, 0, 64)), "224 east and 64 south of Bikinibodem's origin: "
                + schelp.oorsprong(s) + " vs " + bikini.oorsprong(b));
        helper.assertTrue(s.equals(schelp.bijChunk(bikini, b)) && bikini.bijChunk(schelp, s) == null, "bijChunk is the pure function behind it");
        helper.assertTrue(Math.hypot(s.getMiddleBlockX(), s.getMiddleBlockZ()) > Einde2Ids.STRAAL_MIDDEN + 256, "still far outside the middle");
        StructureStart start = Einde2Test.start(w, Einde2Ids.SCHELPENSTAD, s);
        helper.assertTrue(start != null && start.getPieces().stream().allMatch(p -> p instanceof LeegteBouwStructure.Stuk t && t.ruim()),
                "it starts there, with tiles that empty their box and no rim");
        helper.assertTrue(Einde2Test.start(w, Einde2Ids.SCHELPENSTAD, new ChunkPos(s.x() + 3, s.z() - 2)) == null
                && Einde2Test.start(w, Einde2Ids.SCHELPENSTAD, b) == null, "and in no other chunk");
        BoundingBox doos = schelp.doos(s);
        helper.assertTrue(doos.minY() == Einde2Ids.LAAG_MIN && doos.maxY() <= Einde2Ids.LAAG_MAX, "in the deep band: " + doos);
        helper.succeed();
    }

    /**
     * The terrain-free box ({@code leeg}): a build that asks for one only starts where no sampled column holds terrain. And
     * what the Guheinde really offers the Sponsguh pair: the outer islands are about 43 % land in small pieces, so the box of
     * 480 x 224 the pair asks for has no spot without any terrain; for ten seeds the search still finds its spot in the ring,
     * in a round that lets terrain stand in 5 % or 15 % of the columns (the tiles empty their box: {@code ruim}).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void einde2KernLeeg(GameTestHelper helper) throws java.io.IOException {
        ServerLevel level = helper.getLevel();
        Einde2Test.Wereld w = Einde2Test.wereld(helper, SEED, Einde2Ids.GATENKAASHEUVELS);
        // the stand-in Bikinibodem's structure with a terrain-free box of its own (a small one: such a spot is easy to find)
        JsonObject json;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("worldgen/structure/bikinibodem.json")).orElseThrow().openAsReader()) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        helper.assertTrue(!json.has("leeg") && json.get("ruim").getAsBoolean(), "the stand-in asks for no terrain-free box (it always finds its spot)");
        JsonArray leeg = new JsonArray();
        leeg.add(96);
        leeg.add(64);
        json.add("leeg", leeg);
        Structure structure = Structure.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), json).getOrThrow();
        LeegteBouwStructure bouw = (LeegteBouwStructure) structure;
        helper.assertTrue(bouw.leegKolommen() == 4 * 3 && bouw.leegMag(1) == 0, "96 x 64: columns 0, 32, 64, 95 by 0, 32, 63; none may hold terrain");
        helper.assertTrue(LeegteBouwStructure.kolommen(480, 224) == 16 * 8 && LeegteBouwStructure.leegMag(128, 2) == 6 && LeegteBouwStructure.leegMag(128, 3) == 19,
                "480 x 224: 16 x 8 columns; the relaxed rounds let terrain stand in 5 % and 15 % of them");
        BiPredicate<ChunkPos, Boolean> klopt = (chunk, moet) -> {
            StructureStart start = structure.generate(Holder.direct(structure), level.dimension(), level.registryAccess(), w.generator(),
                    w.generator().getBiomeSource(), w.random(), level.getStructureManager(), w.seed(), chunk, 0, w.hoogte(), structure.biomes()::contains);
            return start.isValid() == moet;
        };
        int vrij = 0, vol = 0;
        for (int k = 0; k < 400 && (vrij < 3 || vol < 6); k++) {
            ChunkPos chunk = new ChunkPos(100 + (k % 20) * 9, -150 + (k / 20) * 9);
            int staat = bouw.terrein(w.generator(), w.random(), w.hoogte(), bouw.oorsprong(chunk), Integer.MAX_VALUE);
            if (staat == 0 ? vrij++ < 3 : vol++ < 6) {
                helper.assertTrue(klopt.test(chunk, staat == 0), "terrain in " + staat + " columns at " + chunk + ": it starts only when there is none");
            }
        }
        helper.assertTrue(vrij >= 3 && vol >= 6, "the sample holds both void and islands: " + vrij + " free, " + vol + " not");
        // ten seeds, the pair's own box and ring: a spot is always found, and the log says in which round
        List<Einde2Test.Wereld> werelden = new java.util.ArrayList<>();
        for (long seed : Einde2Test.LEEG_SEEDS) {
            werelden.add(Einde2Test.wereld(helper, seed, Einde2Ids.GATENKAASHEUVELS));
        }
        List<Einde2Test.Leegte> gevonden = werelden.parallelStream().map(wereld -> Einde2Test.zoekLeeg(wereld, 1600, 3000, 480, 224)).toList();
        StringBuilder log = new StringBuilder();
        for (int k = 0; k < gevonden.size(); k++) {
            Einde2Test.Leegte l = gevonden.get(k);
            log.append(Einde2Test.LEEG_SEEDS[k]).append(": round x").append(l.factor()).append(" after ").append(l.pogingen()).append(" spots, terrain in ")
                    .append(l.terrein()).append(" of ").append(l.kolommen()).append("; ");
        }
        com.mojang.logging.LogUtils.getLogger().info("einde2: a box of 480 x 224 in the ring 1600-3000, per seed: {}", log);
        for (Einde2Test.Leegte l : gevonden) {
            helper.assertTrue(l.factor() > 0 && l.terrein() <= LeegteBouwStructure.leegMag(l.kolommen(), l.factor()), "every seed has its spot: " + log);
        }
        // a box that fits the land: 256 x 160 has a spot without any terrain
        Einde2Test.Leegte klein = Einde2Test.zoekLeeg(w, 1600, 3000, 256, 160);
        helper.assertTrue(klein.factor() == 1 && klein.terrein() == 0, "a box of 256 x 160 finds a spot of pure void: " + klein);
        helper.succeed();
    }

    /** Protection on a test box: no breaking or building, creative may, a registered exception may, outside is free. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernBescherming(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.END_STONE);
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(4, 3, 4)));
        Einde2Bescherming.TEST_AREAS.add(box);
        boolean[] mag = {false};
        Einde2Bescherming.uitzondering(Einde2Bescherming.TEST, (p, at) -> mag[0] && at.equals(pos));
        ServerPlayer player = speler(helper);
        try {
            ServerLevel level = helper.getLevel();
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(Einde2Bescherming.beschermd(level, pos) && Einde2Bescherming.TEST.equals(Einde2Bescherming.structuurBij(level, pos)),
                    "the box is protected");
            helper.assertTrue(!Einde2Bescherming.beschermd(level, pos.offset(20, 0, 0)), "outside is free");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled(), "no breaking");
            var place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos.above()), Blocks.END_STONE.defaultBlockState(), player);
            helper.assertTrue(NeoForge.EVENT_BUS.post(place).isCanceled(), "no building");
            mag[0] = true;
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled(), "an exception lets this player change this block");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos.above(), state, player)).isCanceled(), "but no other block");
            mag[0] = false;
            player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled(), "creative players may change it");
        } finally {
            Einde2Bescherming.TEST_AREAS.remove(box);
            weg(helper, player);
        }
        helper.succeed();
    }

    /** Discovery is per player and remembered; the hooks do nothing until a later phase fills them. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernOntdektEnHaken(GameTestHelper helper) {
        ServerPlayer a = speler(helper), b = speler(helper);
        try {
            helper.assertTrue(Einde2Ontdekt.ontdekt(a).isEmpty() && !Einde2Ontdekt.heeft(a, Einde2Ids.LEEGTEHAVEN), "nothing found yet");
            helper.assertTrue(Einde2Ontdekt.ontdek(a, Einde2Ids.LEEGTEHAVEN) && !Einde2Ontdekt.ontdek(a, Einde2Ids.LEEGTEHAVEN), "new once");
            helper.assertTrue(Einde2Ontdekt.ontdek(a, Einde2Ids.OMASHGUH), "a second place");
            helper.assertTrue(List.copyOf(Einde2Ontdekt.ontdekt(a)).equals(List.of(Einde2Ids.LEEGTEHAVEN, Einde2Ids.OMASHGUH)), "in the order found");
            helper.assertTrue(Einde2Ontdekt.ontdekt(b).isEmpty(), "per player");
            var adv = helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/einde2_gevonden_leegtehaven"));
            helper.assertTrue(adv != null && a.getAdvancements().getOrStartProgress(adv).isDone() && !b.getAdvancements().getOrStartProgress(adv).isDone(),
                    "the hidden advancement, only for the finder");
            for (String id : Einde2Ids.LEEGTE_STRUCTUREN) {
                helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/einde2_gevonden_" + id)) != null, "advancement of " + id);
            }
            // a place of the Superkompas is marked visited there too; a story structure is in no tab
            helper.assertTrue(Einde2Ontdekt.inKompas(Einde2Ids.LEEGTEHAVEN) && SpelGroepen.structuurBezocht(a, Einde2Ids.LEEGTEHAVEN)
                    && !SpelGroepen.structuurBezocht(b, Einde2Ids.LEEGTEHAVEN), "the compass knows the visit, per player");
            helper.assertTrue(Einde2Ontdekt.ontdek(a, Einde2Ids.BA_SING_GUH) && !Einde2Ontdekt.inKompas(Einde2Ids.BA_SING_GUH)
                    && !SpelGroepen.structuurBezocht(a, Einde2Ids.BA_SING_GUH), "a story structure is found, but it is no compass place");
            // the hooks: no-ops by default
            ServerLevel level = helper.getLevel();
            helper.assertTrue(Einde2Haken.portaalGeweigerd(level, a) == null, "no portal lock");
            BiFunction<ServerLevel, Entity, Component> slot = (l, e) -> e == a ? Component.literal("njeg") : null;
            Einde2Haken.PORTAAL_SLOTEN.add(slot);
            try {
                helper.assertTrue(Einde2Haken.portaalGeweigerd(level, a) != null && Einde2Haken.portaalGeweigerd(level, b) == null, "a lock refuses who it names");
            } finally {
                Einde2Haken.PORTAAL_SLOTEN.remove(slot);
            }
            helper.assertTrue(!Einde2Haken.ELEMENT_GELEERD.test(a, Einde2Ids.Element.LUCHT) && !Einde2Haken.WERKPLAATS_OPEN.test(a)
                    && !Einde2Haken.VERBORGEN.test(a, Einde2Ids.BA_SING_GUH), "nobody learnt an element, the workshop is shut, nothing is hidden");
            // the lock is a list of separate conditions behind one question
            helper.assertTrue(Einde2Haken.magNaarGuheinde(level, a) && Einde2Haken.portaalRedenen(level, a).isEmpty(), "no lock: everybody may go");
            BiFunction<ServerLevel, Entity, Component> een = (l, e) -> e == a ? Component.literal("een") : null;
            BiFunction<ServerLevel, Entity, Component> twee = (l, e) -> Component.literal("twee");
            Einde2Haken.PORTAAL_SLOTEN.add(een);
            Einde2Haken.PORTAAL_SLOTEN.add(twee);
            try {
                helper.assertTrue(!Einde2Haken.magNaarGuheinde(level, a) && !Einde2Haken.magNaarGuheinde(level, b), "one lock is enough to refuse");
                helper.assertTrue(Einde2Haken.portaalRedenen(level, a).stream().map(Component::getString).toList().equals(List.of("een", "twee"))
                        && Einde2Haken.portaalRedenen(level, b).stream().map(Component::getString).toList().equals(List.of("twee"))
                        && Einde2Haken.portaalGeweigerd(level, a).getString().equals("een"), "every reason, in the order the locks were added");
            } finally {
                Einde2Haken.PORTAAL_SLOTEN.remove(een);
                Einde2Haken.PORTAAL_SLOTEN.remove(twee);
            }
            // the stories: one group, fixed ids, nobody has finished one; the recipe is secret; nothing hangs on the fight yet
            helper.assertTrue(Einde2Haken.VERHAAL_GROEP.equals("guheinde") && Einde2Haken.GROTE_VERHALEN.size() == 6 && Einde2Haken.KLEINE_VERHALEN.size() == 14
                    && Einde2Haken.GROTE_VERHALEN.stream().noneMatch(Einde2Haken.KLEINE_VERHALEN::contains), "six big stories, fourteen small ones");
            helper.assertTrue(Einde2Haken.groteVerhalenKlaar(a) == 0 && !Einde2Haken.KNABBELBURGER_RECEPT.test(a) && Einde2Haken.GEVECHT_BEGINT.isEmpty(),
                    "no story finished, no recipe, no entrance scene");
            var oud = Einde2Haken.VERHAAL_KLAAR;
            Einde2Haken.VERHAAL_KLAAR = (p, id) -> p == a && (id.startsWith("guhvatar_") || id.equals("omashguh"));
            try {
                helper.assertTrue(Einde2Haken.groteVerhalenKlaar(a) == 4 && Einde2Haken.groteVerhalenKlaar(b) == 0, "the counter of big stories, per player");
            } finally {
                Einde2Haken.VERHAAL_KLAAR = oud;
            }
        } finally {
            weg(helper, a);
            weg(helper, b);
        }
        helper.succeed();
    }

    /** The wiring every slice builds on: pages, compass entries, stand-in entities, ids, the biome table. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2KernSkelet(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        // the six guhs are real variants, the thirteen creatures are pages; all nineteen are named in NIEUW_EINDE2
        helper.assertTrue(GuhDex.NIEUW_EINDE2.size() == 19, "nineteen new pages");
        for (String naam : GuhDex.NIEUW_EINDE2) {
            GuhVariant v = GuhVariant.valueOf(naam);
            helper.assertTrue(GuhDex.ENTRIES.contains(v), naam + " is in the Guhdex");
            helper.assertTrue(GuhDex.TELLEND.contains(v) == (v != GuhVariant.LEEGTEWALVISGUH && v != GuhVariant.SPONSGUH && v != GuhVariant.GERRIT),
                    naam + ": counts, unless it is a bonus page");
            if (v.isCharacter()) {
                helper.assertTrue(GuhDex.isCreaturePage(v) && BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(v.id())), naam + ": a creature page with its entity");
            } else {
                helper.assertTrue(GuhDex.TAMEABLE.contains(v) && GuhVariant.VARIANT_BONES.contains(v.id()) && v.bones.contains(v.id()),
                        naam + ": a tameable variant with its own bones");
            }
        }
        helper.assertTrue(GuhVariant.KORSTGUH.ordinal() == GuhVariant.PINGUH.ordinal() + 1 && GuhVariant.KOORGUH.ordinal() == GuhVariant.REISGUH.ordinal() - 1,
                "the six guhs sit between PINGUH and REISGUH");
        helper.assertTrue(GuhVariant.values()[GuhVariant.values().length - 1] == GuhVariant.GERRIT, "the creature pages come last");
        // the Superkompas tab "einde": the vesting, the eight side places and Bikinibodem, each an existing structure; the story
        // structures and Schelpenstad are in no tab
        var einde = SuperkompasItem.CATEGORIES.stream().filter(c -> c.id().equals("einde")).findFirst().orElseThrow();
        var structures = access.lookupOrThrow(Registries.STRUCTURE);
        helper.assertTrue(einde.structures().contains(Einde2Ids.MIKA_VESTING), "the Mika-vesting is in the compass");
        for (String id : Einde2Ids.ZIJ_STRUCTUREN) {
            helper.assertTrue(einde.structures().contains(id), id + " is in the compass");
            helper.assertTrue(structures.getValue(Guhs.id(id)) instanceof LeegteBouwStructure, id + " is a guhs:leegte_bouw");
            helper.assertTrue(structures.get(Guhs.id(id)).orElseThrow().is(Einde2Ids.ZIJ), id + " is in guhs:einde2/zij");
            helper.assertTrue(Plekken.plek(id, "aankomst") != null, id + " has a sidecar");
        }
        for (String id : Einde2Ids.VERHAAL_STRUCTUREN) {
            helper.assertTrue(SuperkompasItem.CATEGORIES.stream().noneMatch(c -> c.structures().contains(id)), id + " is in no compass tab");
        }
        helper.assertTrue(einde.structures().contains(Einde2Ids.BIKINIBODEM) && !Einde2Ontdekt.inKompas(Einde2Ids.SCHELPENSTAD),
                "Bikinibodem is in the compass, Schelpenstad is not (the road leads there)");
        for (String id : Einde2Ids.SPONS_STRUCTUREN) {
            helper.assertTrue(structures.getValue(Guhs.id(id)) instanceof LeegteBouwStructure, id + " is a guhs:leegte_bouw");
            helper.assertTrue(structures.get(Guhs.id(id)).orElseThrow().is(Einde2Ids.SPONSGUH), id + " is in guhs:einde2/sponsguh");
            helper.assertTrue(Plekken.plek(id, "aankomst") != null, id + " has a sidecar");
        }
        helper.assertTrue(Einde2Ids.LEEGTE_STRUCTUREN.size() == 14 && Einde2Ids.LEEGTE_STRUCTUREN.containsAll(Einde2Ids.SPONS_STRUCTUREN), "fourteen big places");
        // the stand-in blocks and items the builds of the Sponsguh story place
        for (String id : List.of("karen", "krokante_grill", "besteltafel", "vruchthuis_wand", "vruchthuis_dak", "vruchthuis_raam", "vruchthuis_deur",
                "bellenblaas", "spooklantaarn", "zeepbel", "bubbelstroom", "bloemwolk")) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Guhs.id(id)) && BuiltInRegistries.ITEM.containsKey(Guhs.id(id)), "block and item " + id);
        }
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id("knabbelburger")) && BuiltInRegistries.ITEM.containsKey(Guhs.id("kwallenjam")), "the food");
        helper.assertTrue(Einde2Ids.ELEMENT_VAN.keySet().stream().allMatch(Einde2Ids.ZIJ_STRUCTUREN::contains), "the element places are side places");
        // the biomes and their tags
        var biomes = access.lookupOrThrow(Registries.BIOME);
        for (var key : Einde2Ids.BUITEN) {
            var biome = biomes.get(key).orElse(null);
            helper.assertTrue(biome != null && biome.is(Einde2Ids.BUITENEILANDEN) && biome.is(Einde2Ids.GEWOON) != biome.is(Einde2Ids.ZELDZAAM),
                    key.identifier() + ": an outer biome, common or rare");
            helper.assertTrue(Biomeguhs.variantVoor(key) != null && Biomeguhs.variantVoor(key).id().endsWith("guh"), key.identifier() + " has its guh");
        }
        helper.assertTrue(!biomes.get(Einde2Ids.GUHEINDE).orElseThrow().is(Einde2Ids.BUITENEILANDEN) && Biomeguhs.variantVoor(Einde2Ids.GUHEINDE) == null,
                "the main island is no outer biome and has no guh of its own");
        for (Einde2Ids.Element e : Einde2Ids.Element.values()) {
            helper.assertTrue(biomes.get(e.biome()).orElseThrow().is(net.minecraft.tags.TagKey.create(Registries.BIOME,
                    Guhs.id("einde2/" + e.name().toLowerCase(Locale.ROOT)))), e + ": its biome tag");
        }
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(Guhs.id("kaaskorst")).defaultBlockState().is(Einde2Ids.EILANDGROND),
                "kaaskorst is island ground");
        helper.succeed();
    }
}
