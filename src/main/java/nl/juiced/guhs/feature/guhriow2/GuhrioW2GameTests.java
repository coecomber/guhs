package nl.juiced.guhs.feature.guhriow2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of world 2 of Super Guhrio (batch "guhriow2"; run with {@code -Pgt=GuhrioW2GameTests}). The server side only
 * (mock players): the egg gate, the hatching (with its cutscene), the warp pipe, the vadsmunten of the cellars, and what
 * the generators wrote (the two levels with their rooms, this world's pieces in the castle's tiles).
 * <p>
 * Like the engine's tests they build a little lane in an empty room (guhriow2_test_kamer: ground along z = 3, you walk at
 * y = 2, the start block at (2, 2, 3) facing east) and call {@link GuhrioSpel#tick} / {@link GuhrioSpel#actie} themselves.
 */
public class GuhrioW2GameTests {
    private static final String BATCH = "guhriow2", KAMER = "guhriow2_test_kamer", LEVEL = "guhriow2_test", DOEL = "guhriow2_test_doel";
    private static final BlockPos START = new BlockPos(2, 2, 3), START_DOEL = new BlockPos(2, 2, 5), POORT = new BlockPos(12, 2, 1);
    private static final int Z = 3, TEST_KANAAL = 12;

    static {
        // the test level: one lane; its flagpole would put you at the gate of the "hall" (the spot the warp pipe searches round)
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "W-2", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), new BlockPos(10, 0, -2), new BlockPos(-1, 0, 0), null));
        // where the test's warp pipe leads: a level that only opens after the test level
        GuhrioLevel.zet(new GuhrioLevel(DOEL, "W-3", List.of(new GuhrioLevel.BaanDef("doel", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), null, null, LEVEL));
        GuhrioW2.WARP.put(TEST_KANAAL, DOEL);
    }

    private static BlockPos bouw(GameTestHelper helper) {
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioW2Feature.KELDERGROND.get());
        }
        return startblok(helper, START, LEVEL);
    }

    private static BlockPos startblok(GameTestHelper helper, BlockPos pos, String level) {
        helper.setBlock(pos, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(pos);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(abs)).zetLevel(level);
        return abs;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double y) {
        Vec3 v = helper.absoluteVec(new Vec3(x, y, Z + 0.5));
        p.snapTo(v.x, v.y, v.z);
    }

    /** World 2's own tick for a mock player (it runs every fifth tick of the player's own count). */
    private static void tik(ServerPlayer p) {
        p.tickCount = 0;
        tik(p);
    }

    private static boolean dicht(ServerLevel level, BlockPos abs, ServerPlayer p) {
        return !level.getBlockState(abs).getCollisionShape(level, abs, CollisionContext.of(p)).isEmpty();
    }

    /** The gate with the lock, the egg before it, the hatching spot, the nest and Guhshi behind it. */
    private static void nestgang(GameTestHelper helper) {
        BlockState rood = GuhrioFeature.SCHAKELBLOK.get().defaultBlockState().setValue(GuhrioStukken.KANAAL, 7).setValue(GuhrioStukken.SchakelBlok.AAN, false);
        helper.setBlock(new BlockPos(6, 2, Z), GuhrioFeature.GUHSHI_EI.get());
        helper.setBlock(new BlockPos(10, 2, Z), rood);
        helper.setBlock(new BlockPos(10, 3, Z), rood);
        helper.setBlock(new BlockPos(10, 4, Z), GuhrioW2Feature.EISLOT.get());
        helper.setBlock(new BlockPos(12, 2, Z), GuhrioW2Feature.BROEDPLEK.get());
        helper.setBlock(new BlockPos(12, 3, Z), GuhrioW2Feature.BROEDPLEK.get());
        helper.setBlock(new BlockPos(15, 2, Z), GuhrioW2Feature.NEST.get());
        helper.setBlock(new BlockPos(15, 3, Z), GuhrioFeature.GUHSHI_PLEK.get());
    }

    // =====================================================================================================================

    /** The egg gate: red and solid until you have the egg, then open for you alone; open from the start the next time. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow2Eierslot(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        nestgang(helper);
        ServerLevel level = helper.getLevel();
        BlockPos hek = helper.absolutePos(new BlockPos(10, 2, Z)), slot = helper.absolutePos(new BlockPos(10, 4, Z));
        ServerPlayer p = speler(helper), q = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs) && GuhrioSpel.start(q, startAbs), "both players are in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(dicht(level, hek, p) && dicht(level, hek, q) && !GuhrioSpel.kanaal(p, 7), "no egg: the gate is shut");
        // at the lock without the egg: it says what it wants (once in a while), and stays shut
        zet(helper, p, 8.5, 2);
        GuhrioSpel.tick(p);
        tik(p);
        helper.assertTrue(s.rust.containsKey(slot) && dicht(level, hek, p), "the lock asked for the egg");
        // the egg: the lock opens, for the finder alone
        zet(helper, p, 6.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioKasteel.heeftEi(p) && !GuhrioKasteel.heeftEi(q), "the egg is found");
        tik(p);
        tik(q);
        helper.assertTrue(GuhrioSpel.kanaal(p, 7) && !dicht(level, hek, p), "with the egg the gate is open");
        helper.assertTrue(!GuhrioSpel.kanaal(q, 7) && dicht(level, hek, q), "not for the other player");
        helper.assertTrue(dicht(level, hek.above(), q) && !dicht(level, hek.above(), p), "the whole gate");
        // the next run: open from the start
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "a second run");
        helper.assertTrue(GuhrioSpel.kanaal(p, 7) && !dicht(level, hek, p), "who comes in with the egg finds the gate open");
        weg(helper, p, q);
        helper.succeed();
    }

    /** Hatching: with the egg, the hatching spot plays the scene once; then Guhshi waits on his nest and carries you. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhriow2Uitbroeden(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        nestgang(helper);
        BlockPos plek = helper.absolutePos(new BlockPos(15, 3, Z)), nest = helper.absolutePos(new BlockPos(15, 2, Z));
        ServerPlayer p = speler(helper), q = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs) && GuhrioSpel.start(q, startAbs), "both players are in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(s.staat(plek) == 1 && !GuhrioW2.uitgebroed(p), "no Guhshi on the nest before he hatched");
        helper.assertTrue(nest.equals(GuhrioW2.nest(s, startAbs)), "the level's nest is found");
        // without the egg the hatching spot is nothing
        zet(helper, q, 12.5, 2);
        GuhrioSpel.tick(q);
        helper.assertTrue(!Cutscenes.bezig(q) && !GuhrioW2.uitgebroed(q), "no egg, no hatching");
        // with the egg: the scene, and the level waits
        helper.assertTrue(GuhrioKasteel.geefEi(p), "the egg");
        zet(helper, p, 12.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(Cutscenes.bezig(p) && !GuhrioW2.uitgebroed(p), "the hatching scene plays");
        int ticks = s.ticks;
        // (a mock player has no tick of its own: this is the tick the story engine, the level and world 2 all hang on)
        helper.onEachTick(() -> {
            if (!p.isRemoved()) {
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            }
        });
        // (a mock player "watches" for two ticks)
        helper.runAfterDelay(12, () -> {
            {
                helper.assertFalse(Cutscenes.bezig(p), "the scene is over");
                helper.assertTrue(GuhrioW2.uitgebroed(p) && Cutscenes.gezien(p, GuhrioW2.SCENE.id()), "Guhshi hatched, the scene is seen");
                helper.assertTrue(GidsFeature.heeft(p, "guhrio/guhrio_w2_uitgebroed") && GidsFeature.heeft(p, "quest/guhrio_w2_uitgebroed"), "both advancements");
                helper.assertTrue(GuhrioSpel.sessie(p) == s && s.staat(plek) == 0 && !s.guhshi, "he waits on his nest; the level went on (" + ticks + ")");
                helper.assertTrue(!GuhrioW2.uitgebroed(q) && GuhrioSpel.sessie(q).staat(plek) == 1, "not for the other player");
                // onto the nest: he carries you
                zet(helper, p, 15.5, 3);
                GuhrioSpel.tick(p);
                helper.assertTrue(s.guhshi && s.staat(plek) == 1, "Guhshi carries the player");
                // the next run: he is there from the start, and the hatching spot is quiet
                GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
                helper.assertTrue(GuhrioSpel.start(p, startAbs), "a second run");
                GuhrioSpel.Sessie s2 = GuhrioSpel.sessie(p);
                helper.assertTrue(s2.staat(plek) == 0, "he waits on his nest from the start");
                zet(helper, p, 12.5, 2);
                GuhrioSpel.tick(p);
                helper.assertFalse(Cutscenes.bezig(p), "no second hatching");
                weg(helper, p, q);
                helper.succeed();
            }
        });
    }

    /** The scene's frame turns with the lane; the scene is registered with its lines. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow2Scene(GameTestHelper helper) {
        helper.assertTrue(Cutscene.van("guhriow2_uit") == GuhrioW2.SCENE && GuhrioW2.SCENE.duur() == 280 && "guhrio".equals(GuhrioW2.SCENE.lijn()),
                "the hatching scene is registered on the Guhrio questline");
        helper.assertTrue(GuhrioW2.SCENE.heeftSpeler() && GuhrioW2.SCENE.acteur("guhshi") != null && GuhrioW2.SCENE.acteur("ei") != null
                && GuhrioW2.SCENE.zinnen().size() == 6, "its cast and its six lines");
        BlockPos anker = new BlockPos(100, 10, 200);
        Vec3 verder = new Vec3(3.5, 0, 0.5), camera = new Vec3(0.5, 0, 4.5);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Rotation draai = GuhrioW2.draai(d);
            Vec3 v = Cutscene.wereld(anker, draai, verder), c = Cutscene.wereld(anker, draai, camera);
            Direction rechts = d.getClockWise();
            helper.assertTrue(v.distanceTo(new Vec3(100.5 + 3 * d.getStepX(), 10, 200.5 + 3 * d.getStepZ())) < 1e-6,
                    "+x of the scene is further along a lane that runs " + d + ": " + v);
            helper.assertTrue(c.distanceTo(new Vec3(100.5 + 4 * rechts.getStepX(), 10, 200.5 + 4 * rechts.getStepZ())) < 1e-6,
                    "+z of the scene is the camera's side of a lane that runs " + d + ": " + c);
        }
        // Guhshi is out of sight until the dark, then on the nest
        helper.assertTrue(GuhrioW2.SCENE.plek("guhshi", 100).y < -20 && GuhrioW2.SCENE.plek("guhshi", 140).distanceTo(new Vec3(0.5, 1, 0.5)) < 1e-6
                && GuhrioW2.SCENE.plek("ei", 140).y < -20 && GuhrioW2.SCENE.zwartOp(127) == 1f, "the egg makes room for Guhshi in the dark");
        helper.succeed();
    }

    /** A warp pipe: the level it leads to is open for you for good, and you stand at its start; this level is not finished. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow2Warppijp(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        ServerLevel level = helper.getLevel();
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, 5), GuhrioW2Feature.KELDERGROND.get());
        }
        BlockPos doelAbs = startblok(helper, START_DOEL, DOEL);
        // the "hall": the gate of the other level, where this level's flagpole would put you
        helper.setBlock(POORT, GuhrioFeature.POORT.get().defaultBlockState().setValue(GuhrioBlocks.PoortBlok.FACING, Direction.EAST));
        GuhrioBlocks.StartBlockEntity poort = (GuhrioBlocks.StartBlockEntity) level.getBlockEntity(helper.absolutePos(POORT));
        poort.zetLevel(DOEL);
        poort.zetNaar(START_DOEL.subtract(POORT));
        BlockState pijp = GuhrioW2Feature.WARPPIJP.get().defaultBlockState();
        BlockPos warp = new BlockPos(8, 2, Z), nergens = new BlockPos(14, 2, Z);
        helper.setBlock(warp, pijp.setValue(GuhrioBlocks.PijpBlok.KANAAL, TEST_KANAAL));
        helper.setBlock(nergens, pijp.setValue(GuhrioBlocks.PijpBlok.KANAAL, 3));
        ServerPlayer p = speler(helper), q = speler(helper);
        GuhrioLevel doel = GuhrioLevel.vind(level.getServer(), DOEL);
        helper.assertTrue(doel != null && !GuhrioKasteel.magIn(p, doel) && !GuhrioSpel.start(p, doelAbs), "the other level is shut: its level before it is not done");
        helper.assertTrue(GuhrioSpel.start(p, startAbs) && GuhrioSpel.start(q, startAbs), "both players are in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(doelAbs.equals(GuhrioW2.zoekStart(level, s, DOEL)) && GuhrioW2.zoekStart(level, s, "bestaat_niet") == null,
                "the other level's start block is found through its gate");
        // not from a distance, and not through a pipe that leads nowhere
        zet(helper, p, 5.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(warp), 0);
        zet(helper, p, 14.5, 3);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(nergens), 0);
        helper.assertTrue(GuhrioSpel.sessie(p) == s && !GuhrioW2.warpGevonden(p) && !GuhrioKasteel.magIn(p, doel), "nothing happened");
        // standing on it: warp
        zet(helper, p, 8.5, 3);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(warp), 0);
        GuhrioSpel.Sessie nu = GuhrioSpel.sessie(p);
        helper.assertTrue(nu != null && nu != s && DOEL.equals(nu.level().level().id()), "the player is in the other level");
        helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(doelAbs)) < 0.01, "at its start: " + p.position());
        helper.assertTrue(GuhrioKasteel.magIn(p, doel) && GuhrioW2.warpGevonden(p) && !GuhrioKasteel.gehaald(p, LEVEL), "open for good; the level left is not finished");
        helper.assertTrue(GidsFeature.heeft(p, "guhrio/guhrio_w2_warp") && GidsFeature.heeft(p, "quest/guhrio_w2_warp"), "both advancements");
        helper.assertTrue(!GuhrioKasteel.magIn(q, doel) && !GuhrioW2.warpGevonden(q) && GuhrioSpel.sessie(q).level().level().id().equals(LEVEL), "not for the other player");
        // walking out and in again: the gate lets the player in now
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(GuhrioSpel.start(p, doelAbs), "the other level stays open");
        weg(helper, p, q);
        helper.succeed();
    }

    /** The real warp pipes lead to levels of the castle that are not the one they stand in. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow2WarpDoelen(GameTestHelper helper) {
        for (int kanaal : new int[]{13, 14, 15}) {
            String doel = GuhrioW2.WARP.get(kanaal);
            helper.assertTrue(doel != null && GuhrioKasteel.LEVELS.contains(doel) && !GuhrioW2.LEVEL_2.equals(doel), "warp pipe " + kanaal + " leads to " + doel);
            helper.assertTrue(GuhrioLevel.bestand(helper.getLevel().getServer(), doel) != null, "the level " + doel + " exists");
        }
        helper.succeed();
    }

    /** All six big vadsmunten of the cellars: the hidden advancement of the quest book, once. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow2Keldervads(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "in");
        tik(p);
        helper.assertTrue(!GuhrioW2.alleVadsmunten(p) && !GidsFeature.heeft(p, "quest/guhrio_w2_vads"), "nothing yet");
        CompoundTag vads = GuhrioSpel.spaar(p).getCompoundOrEmpty("Vads");
        vads.putInt(GuhrioW2.LEVEL_1, 7);
        vads.putInt(GuhrioW2.LEVEL_2, 3);
        GuhrioSpel.spaar(p).put("Vads", vads);
        tik(p);
        helper.assertTrue(!GuhrioW2.alleVadsmunten(p) && !GuhQuests.saved(p).getBooleanOr(GuhrioW2.VADS, false), "five of six is not all");
        vads.putInt(GuhrioW2.LEVEL_2, 7);
        GuhrioSpel.spaar(p).put("Vads", vads);
        tik(p);
        helper.assertTrue(GuhrioW2.alleVadsmunten(p) && GuhQuests.saved(p).getBooleanOr(GuhrioW2.VADS, false) && GidsFeature.heeft(p, "quest/guhrio_w2_vads"),
                "all six: the advancement");
        weg(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // what the generators wrote
    // =====================================================================================================================

    /** The two levels are this world's own (not the engine's practice lanes): their lanes, their order, their rooms. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow2Levels(GameTestHelper helper) {
        GuhrioLevel een = GuhrioLevel.bestand(helper.getLevel().getServer(), GuhrioW2.LEVEL_1), twee = GuhrioLevel.bestand(helper.getLevel().getServer(), GuhrioW2.LEVEL_2);
        helper.assertTrue(een != null && twee != null, "both level files exist");
        helper.assertTrue("2-1".equals(een.wereld()) && "kasteel_1_2".equals(een.na()) && "2-2".equals(twee.wereld()) && GuhrioW2.LEVEL_1.equals(twee.na()),
                "their names and their order");
        helper.assertTrue(een.banen().stream().map(GuhrioLevel.BaanDef::id).toList().equals(List.of("hoofd", "munten")), "2-1: the main lane and the coin cellar: "
                + een.banen().stream().map(GuhrioLevel.BaanDef::id).toList());
        helper.assertTrue(twee.banen().stream().map(GuhrioLevel.BaanDef::id).toList().equals(List.of("hoofd", "ei", "warp")), "2-2: the main lane, the egg room and "
                + "the warp room: " + twee.banen().stream().map(GuhrioLevel.BaanDef::id).toList());
        for (GuhrioLevel l : List.of(een, twee)) {
            GuhrioLevel.BaanDef hoofd = l.banen().get(0);
            helper.assertTrue(hoofd.boven() - hoofd.onder() == 11 && hoofd.punten().get(1).getX() - hoofd.punten().get(0).getX() == 95,
                    l.id() + ": the main lane is 96 long and twelve rows high");
            for (GuhrioLevel.BaanDef kamer : l.banen().subList(1, l.banen().size())) {
                helper.assertTrue(kamer.onder() == hoofd.boven() + 1 && kamer.boven() - kamer.onder() == 7 && kamer.hoogte() == 4,
                        l.id() + ": the room " + kamer.id() + " lies above the ceiling, eight rows high");
            }
            helper.assertTrue(l.uitgang() != null && l.ingang() != null, l.id() + ": the hall is its exit and its way out");
        }
        helper.succeed();
    }

    /** The castle's tiles hold this world's pieces: three warp pipes (one per target), one lock, one nest, the hatching spot. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow2KasteelStukken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<Integer> kanalen = new HashSet<>();
        int[] aantal = new int[7];
        Block[] blokken = {GuhrioW2Feature.WARPPIJP.get(), GuhrioW2Feature.EISLOT.get(), GuhrioW2Feature.NEST.get(), GuhrioW2Feature.BROEDPLEK.get(),
                GuhrioFeature.GUHSHI_EI.get(), GuhrioW2Feature.KELDERGROND.get(), GuhrioW2Feature.WARPBUIS.get()};
        int tegels = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                StructureTemplate tegel = level.getStructureManager().get(Guhs.id(GuhrioKasteel.STRUCTUUR + "/stuk_" + i + "_" + j)).orElse(null);
                if (tegel == null) {
                    continue;
                }
                tegels++;
                for (int k = 0; k < blokken.length; k++) {
                    List<StructureTemplate.StructureBlockInfo> gevonden = tegel.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), blokken[k]);
                    aantal[k] += gevonden.size();
                    if (k == 0) {
                        gevonden.forEach(info -> kanalen.add(info.state().getValue(GuhrioBlocks.PijpBlok.KANAAL)));
                    }
                    if (k == 1) {
                        gevonden.forEach(info -> helper.assertTrue(info.state().getValue(GuhrioStukken.KANAAL) == 7, "the lock's channel is 7"));
                    }
                }
            }
        }
        helper.assertTrue(tegels == 16, "the castle has 16 tiles: " + tegels);
        helper.assertTrue(aantal[0] == 3 && kanalen.equals(Set.of(13, 14, 15)), "three warp pipes, one per target: " + aantal[0] + " " + kanalen);
        helper.assertTrue(aantal[6] == 3, "each on its body: " + aantal[6]);
        helper.assertTrue(aantal[1] == 1 && aantal[2] == 1 && aantal[3] == 4, "one lock, one nest, a hatching spot four rows high: " + aantal[1] + " "
                + aantal[2] + " " + aantal[3]);
        helper.assertTrue(aantal[4] >= 1, "Guhshi's egg lies in the cellars: " + aantal[4]);
        helper.assertTrue(aantal[5] > 500, "the cellars stand on their own ground: " + aantal[5]);
        helper.succeed();
    }
}
