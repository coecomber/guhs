package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.biomeguhs.Biomeguhs;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * Game tests of the einde2 skeleton: the structure type guhs:leegte_bouw on the test build {@code einde2_test_bouw}
 * (tools/features/einde2.py: 3 x 2 tiles, in no structure set), {@link Plekken} (sidecar to world and back, for a
 * leegte_bouw and for a jigsaw piece, the guaranteed copy, the overlays), protection, discovery, the hooks and the
 * wiring (pages, compass, stand-in entities). The Guheinde's generator comes from {@link Einde2Test}.
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
            for (String id : Einde2Ids.VERHAAL_STRUCTUREN) {
                helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/einde2_gevonden_" + id)) != null, "advancement of " + id);
            }
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
        // the six guhs are real variants, the eleven creatures are pages; all seventeen are named in NIEUW_EINDE2
        helper.assertTrue(GuhDex.NIEUW_EINDE2.size() == 17, "seventeen new pages");
        for (String naam : GuhDex.NIEUW_EINDE2) {
            GuhVariant v = GuhVariant.valueOf(naam);
            helper.assertTrue(GuhDex.ENTRIES.contains(v), naam + " is in the Guhdex");
            helper.assertTrue(GuhDex.TELLEND.contains(v) == (v != GuhVariant.LEEGTEWALVISGUH), naam + ": counts, unless it is the bonus page");
            if (v.isCharacter()) {
                helper.assertTrue(GuhDex.isCreaturePage(v) && BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(v.id())), naam + ": a creature page with its entity");
            } else {
                helper.assertTrue(GuhDex.TAMEABLE.contains(v) && GuhVariant.VARIANT_BONES.contains(v.id()) && v.bones.contains(v.id()),
                        naam + ": a tameable variant with its own bones");
            }
        }
        helper.assertTrue(GuhVariant.KORSTGUH.ordinal() == GuhVariant.PINGUH.ordinal() + 1 && GuhVariant.KOORGUH.ordinal() == GuhVariant.REISGUH.ordinal() - 1,
                "the six guhs sit between PINGUH and REISGUH");
        helper.assertTrue(GuhVariant.values()[GuhVariant.values().length - 1] == GuhVariant.MOMO_GUH, "the creature pages come last");
        // the Superkompas tab "einde": the vesting and the eight side places, each an existing structure; the story structures are in no tab
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
