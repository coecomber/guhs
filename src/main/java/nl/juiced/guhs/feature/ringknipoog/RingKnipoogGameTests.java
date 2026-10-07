package nl.juiced.guhs.feature.ringknipoog;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.landdiertjes.ShuckleEntity;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.ringh2.Guhvendel;
import nl.juiced.guhs.feature.ringh2.RingH2Feature;
import nl.juiced.guhs.feature.ringh3.Mijn;
import nl.juiced.guhs.feature.ringh3.MijnEvents;
import nl.juiced.guhs.feature.ringh3.MijnProef;
import nl.juiced.guhs.feature.ringh3.Plekken;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;
import nl.juiced.guhs.feature.ringh4.RingH4Feature;
import nl.juiced.guhs.feature.ringh4.Spiegel;
import nl.juiced.guhs.feature.ringh6.Berg;
import nl.juiced.guhs.feature.ringh6.RingH6Feature;
import nl.juiced.guhs.feature.ringh6.Thuis;
import nl.juiced.guhs.feature.ringsausuman.Bakkerij;
import nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.taal.NlTekst;

/**
 * bbq2 (ring-knipogen): the winks, server side. A mock player "watches" a scene for two ticks and gets no wink by itself
 * ({@link Knipogen#OOK_ZONDER_SCHERM}): every test here switches that on and off again, and has a batch of its own (the
 * switch and the hand-made copies of the chapters' buildings are shared by the whole server). Mock players are not ticked
 * by the server: the tests post their tick event themselves (that is what ends a scene a mock player watches).
 * <ul>
 *   <li>the rule: once per player, the scene's own {@code daarna} exactly once and after the wink, a wink that was cut off;</li>
 *   <li>the hooks, each through what a real player does: the council bell (ring-h2), the mirror (ring-h4), the feast at
 *       home (ring-h6), a click on Sam-guh on the mountain (ring-h6), the narrator card at the gate of the mine (ring-h3),
 *       Sausuman's hall, the ?-block of level 1-1, and Sjokkel on the bridge head.</li>
 * </ul>
 */
public final class RingKnipoogGameTests {
    private static final String KAMER = "ringknipoog_test_kamer", BATCH = "ringknipoog";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        Ring.OVERAL = true;
        Knipogen.OOK_ZONDER_SCHERM = true;
        return p;
    }

    /** The chapters before {@code hoofdstuk} are done for this player; that chapter is at {@code stap}. */
    private static void opStap(ServerPlayer p, int hoofdstuk, int stap) {
        Ring.lijn(1).begin(p);
        for (int n = 1; n < hoofdstuk; n++) {
            Ring.lijn(n).zet(p, Ring.lijn(n).stappen());
        }
        Ring.lijn(hoofdstuk).begin(p);
        Ring.lijn(hoofdstuk).zet(p, stap);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Knipogen.wis(p);
            Mijn.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        Knipogen.OOK_ZONDER_SCHERM = false;
    }

    /** What the server does for a real player every tick (it ends a scene a mock player watches, and runs the winks' own look). */
    private static void tik(ServerPlayer p) {
        if (!p.isRemoved()) {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        }
    }

    /** The tick event of a tick on which ring-h6 does its once-a-second look for this player (and this package does not). */
    private static void secondeH6(ServerPlayer p) {
        p.tickCount = Math.floorMod(7 - p.getId(), 20);
        tik(p);
    }

    /** A tick count on which nobody's once-a-second look runs for this player. */
    private static void stil(ServerPlayer p) {
        p.tickCount = Math.floorMod(1 - p.getId(), 40);
    }

    /** A copy of {@code guhs:<structuur>} that counts as standing here: its template's (0, 0, 0) at {@code nul}, unturned. */
    private static StructureStart kopie(ServerLevel level, String structuur, BlockPos nul, BoundingBox doos) {
        Structure structure = Kopieen.structuur(level, structuur);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + structuur).apply(StructureTemplatePool.Projection.RIGID);
        PoolElementStructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, nul, 0, Rotation.NONE, doos,
                LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(nul), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    private static BoundingBox kamer(GameTestHelper helper) {
        return BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(24, 8, 24)));
    }

    // =====================================================================================================================
    // the rule
    // =====================================================================================================================

    /**
     * Once per player. A scene with a wink after it: the scene, then the wink, then the scene's own daarna, exactly once;
     * the second time the same player gets the scene and its daarna and no wink; another player gets their own. A wink that
     * was cut off (a logout) leaves the story where it was, and the next time only the wink is still owed. A wink before
     * something: that something follows once the player may be touched again. A player whose game cannot show a scene
     * gets none, and the story goes on all the same.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_regel", timeoutTicks = 400)
    public static void ringknipoogEenKeerPerSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 4, 4), q = speler(helper, 8, 4), r = speler(helper, 12, 4), s = speler(helper, 16, 4), mock = speler(helper, 20, 4);
        Cutscene scene = Cutscene.van("ringh2_raad"), knipoog = Knipogen.BALTOGUH;
        BlockPos anker = helper.absolutePos(new BlockPos(12, 2, 12));
        int[] daarna = new int[5];
        helper.assertTrue(Knipogen.alle().size() == 7 && Knipogen.alle().stream().allMatch(k -> Cutscene.van(k.id()) == k), "seven winks, all registered");
        for (Cutscene k : Knipogen.alle()) {
            helper.assertTrue(k.duur() >= 100 && k.duur() <= Knipogen.MAX_TICKS, k.id() + " takes five to ten seconds: " + k.duur());
            helper.assertTrue(k.lijn() != null && k.heeftSpeler() && k.cameraOp(0) != null, k.id() + " belongs to a questline, has the player in it and a camera");
            helper.assertTrue(k.zwartOp(k.duur()) > 0.9f || k == Knipogen.KISTJE, k.id() + " ends in black (the one in a level cuts back to it)");
        }
        helper.assertTrue(!Knipogen.gezien(p, knipoog) && Knipogen.gestart(p) == 0, "nobody saw a wink yet");
        helper.assertTrue(Knipogen.speel(p, scene, knipoog, anker, Rotation.NONE, x -> daarna[0]++) && Cutscenes.bezig(p), "the scene plays");
        helper.assertTrue(!Knipogen.speel(p, scene, knipoog, anker, Rotation.NONE, x -> daarna[0] += 10), "one thing at a time, as Cutscenes.speel");
        // a wink before something, for a player who is watching something else: that something at once
        helper.assertTrue(!Knipogen.eerst(p, Knipogen.BORIS, anker, Rotation.NONE, x -> false) && Knipogen.gestart(p) == 0, "busy: no wink, the story's own answer");
        helper.onEachTick(() -> {
            for (ServerPlayer x : List.of(p, q, r, s, mock)) {
                stil(x);
                tik(x);
            }
        });
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Cutscenes.gezien(p, scene.id()) && Cutscenes.bezig(p) && Knipogen.gestart(p) == 1 && daarna[0] == 0 && !Knipogen.gezien(p, knipoog),
                    "the scene is over, the wink plays, the story waits (seen " + Cutscenes.gezien(p, scene.id()) + ", started " + Knipogen.gestart(p) + ")");
            helper.assertTrue(knipoog.id().equals(GuhQuests.saved(p).getStringOr(Knipogen.OPEN, "")), "the wink is owed");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, knipoog) && daarna[0] == 1, "the wink is over: daarna, once (" + daarna[0] + ")");
            helper.assertTrue(!GuhQuests.saved(p).contains(Knipogen.OPEN), "nothing is owed any more");
            // the same player again: the scene and its daarna, no wink
            helper.assertTrue(Knipogen.speel(p, scene, knipoog, anker, Rotation.NONE, x -> daarna[0]++), "the scene again");
            // another player: their own wink; a third one logs out in the middle of theirs
            helper.assertTrue(Knipogen.speel(q, scene, knipoog, anker, Rotation.NONE, x -> daarna[1]++), "another player");
            helper.assertTrue(Knipogen.speel(r, scene, knipoog, anker, Rotation.NONE, x -> daarna[2]++), "a third player");
        });
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && daarna[0] == 2 && Knipogen.gestart(p) == 1, "the second time: no wink, daarna at once (" + daarna[0] + ")");
            helper.assertTrue(Cutscenes.bezig(q) && Knipogen.gestart(q) == 1 && daarna[1] == 0, "the other player's wink plays");
            helper.assertTrue(Cutscenes.bezig(r) && Knipogen.gestart(r) == 1, "the third player's too");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(r));
            helper.assertTrue(!Cutscenes.bezig(r) && daarna[2] == 0 && !Knipogen.gezien(r, knipoog) && Cutscenes.gezien(r, scene.id()),
                    "cut off: the long scene was seen, the wink was not, the story did not move");
            helper.assertTrue(knipoog.id().equals(GuhQuests.saved(r).getStringOr(Knipogen.OPEN, "")), "the wink is still owed");
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!Cutscenes.bezig(q) && Knipogen.gezien(q, knipoog) && daarna[1] == 1, "the other player saw theirs");
            // the third player comes back: only the wink plays before the story goes on
            helper.assertTrue(Knipogen.speel(r, scene, knipoog, anker, Rotation.NONE, x -> daarna[2]++) && Knipogen.gestart(r) == 1, "the wink that is owed");
            // a wink BEFORE something
            helper.assertTrue(Knipogen.eerst(s, Knipogen.BORIS, anker, Rotation.NONE, x -> {
                daarna[3]++;
                return true;
            }) && Cutscenes.bezig(s) && daarna[3] == 0, "Boris first");
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!Cutscenes.bezig(r) && Knipogen.gezien(r, knipoog) && daarna[2] == 1, "two ticks later (not four): the wink alone, then daarna (" + daarna[2] + ")");
            helper.assertTrue(!Cutscenes.bezig(s) && Knipogen.gezien(s, Knipogen.BORIS) && daarna[3] == 0 && !Duwtje.mag(s), "Boris is done, the player is still looked after");
        });
        // a player whose game cannot show a scene (a mock player as the chapters' own tests have them): no wink, the story goes on
        helper.runAfterDelay(18, () -> {
            Knipogen.OOK_ZONDER_SCHERM = false;
            helper.assertTrue(!Knipogen.kanZien(mock) && Knipogen.speel(mock, scene, knipoog, anker, Rotation.NONE, x -> daarna[4]++), "the scene plays for a mock player");
            helper.assertTrue(!Knipogen.toon(mock, knipoog, anker, Rotation.NONE, null), "no wink for a game that cannot show it");
            helper.assertTrue(Knipogen.eerst(mock, Knipogen.BORIS, anker, Rotation.NONE, x -> true) && Knipogen.gestart(mock) == 0, "the story's own answer at once");
        });
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(!Cutscenes.bezig(mock) && daarna[4] == 1 && !Knipogen.gezien(mock, knipoog) && Knipogen.gestart(mock) == 0, "the mock: scene and daarna only");
            Knipogen.OOK_ZONDER_SCHERM = true;
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(Duwtje.mag(s) && daarna[3] == 1, "what came after Boris, once the player may be touched again: " + daarna[3]);
            helper.assertTrue(Knipogen.eerst(s, Knipogen.BORIS, anker, Rotation.NONE, x -> {
                daarna[3]++;
                return false;
            }) == false && daarna[3] == 2 && Knipogen.gestart(s) == 1, "the second time: no Boris, the answer of what comes after");
            weg(helper, p, q, r, s, mock);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the hooks in the chapters
    // =====================================================================================================================

    /**
     * Wink 1: the council bell of Guhvendel. The council scene, Baltoguh, then the step; a second player who rings later
     * gets the same; a player who saw the wink before only gets the council.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_raad", timeoutTicks = 300)
    public static void ringknipoogRaadVanGuhrond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos tafel = helper.absolutePos(new BlockPos(14, 2, 12));
        kopie(level, Guhvendel.STRUCTUUR, tafel.subtract(Guhvendel.KRING), kamer(helper));
        BlockPos bel = tafel.offset(Guhvendel.BEL.subtract(Guhvendel.KRING));
        level.setBlockAndUpdate(bel.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        level.setBlockAndUpdate(bel, Blocks.BELL.defaultBlockState());
        ServerPlayer p = speler(helper, 8, 12), later = speler(helper, 8, 14), oud = speler(helper, 8, 16);
        Verhaallijn lijn = RingH2Feature.LIJN;
        for (ServerPlayer x : List.of(p, later, oud)) {
            opStap(x, 2, Guhvendel.RAADSBEL);
        }
        // (this one saw Baltoguh in an earlier life)
        GuhQuests.saved(oud).put("guhs_scene_" + Knipogen.BALTOGUH.id(), gezienTag());
        helper.assertTrue(Knipogen.gezien(oud, Knipogen.BALTOGUH), "(a wink is seen when the engine says its scene was)");
        Guhvendel.Oord o = Guhvendel.oord(level, p.blockPosition());
        helper.assertTrue(o != null && o.anker().equals(tafel) && o.bel().equals(bel), "the copy of Guhvendel is found");
        luid(p, bel);
        luid(oud, bel);
        helper.assertTrue(Cutscenes.bezig(p) && Cutscenes.bezig(oud) && lijn.stap(p) == Guhvendel.RAADSBEL, "the bell: the council plays");
        helper.onEachTick(() -> {
            for (ServerPlayer x : List.of(p, later, oud)) {
                stil(x);
                tik(x);
            }
        });
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Cutscenes.gezien(p, "ringh2_raad") && Cutscenes.bezig(p) && Knipogen.gestart(p) == 1 && lijn.stap(p) == Guhvendel.RAADSBEL,
                    "after the council Baltoguh walks in; the step waits (started " + Knipogen.gestart(p) + ", step " + lijn.stap(p) + ")");
            helper.assertTrue(!Cutscenes.bezig(oud) && lijn.stap(oud) == Guhvendel.MELDEN && Knipogen.gestart(oud) == 0, "who saw him before: the council, then the step");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.BALTOGUH) && lijn.stap(p) == Guhvendel.MELDEN, "Baltoguh is gone again: step 4 (" + lijn.stap(p) + ")");
            CompoundTag waar = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.BALTOGUH.id());
            helper.assertTrue(BlockPos.of(waar.getLongOr("Pos", 0L)).equals(tafel) && waar.getIntOr("Draai", -1) == Rotation.NONE.ordinal(),
                    "he was played in the council's own frame: the stone table");
            // a player who comes later
            luid(later, bel);
            helper.assertTrue(Cutscenes.bezig(later), "a later player's council");
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!Cutscenes.bezig(later) && Knipogen.gezien(later, Knipogen.BALTOGUH) && Knipogen.gestart(later) == 1 && lijn.stap(later) == Guhvendel.MELDEN,
                    "the later player saw him too");
            level.setBlockAndUpdate(bel, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(bel.below(), Blocks.AIR.defaultBlockState());
            Kopieen.testWissen(level);
            weg(helper, p, later, oud);
            helper.succeed();
        });
    }

    private static CompoundTag gezienTag() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Gezien", true);
        return t;
    }

    /** A right click on the council bell, as the game posts it. */
    private static void luid(ServerPlayer p, BlockPos bel) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bel), Direction.UP, bel, false);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, bel, hit));
    }

    /**
     * Wink 3: Guhladriel's mirror. A click at step 4: the mirror scene, the wink, then step 5; a later look into the mirror
     * (the chapter replays its scene) brings no second wink.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_spiegel", timeoutTicks = 300)
    public static void ringknipoogSpiegel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spiegel = helper.absolutePos(new BlockPos(12, 3, 12));
        level.setBlockAndUpdate(spiegel, RingH4Feature.SPIEGEL.get().defaultBlockState());
        ServerPlayer p = speler(helper, 10, 12);
        Verhaallijn lijn = RingH4Feature.LIJN;
        opStap(p, 4, 4);
        kijk(p, spiegel);
        helper.assertTrue(Cutscenes.bezig(p) && lijn.stap(p) == 4, "the mirror scene plays");
        helper.onEachTick(() -> {
            stil(p);
            tik(p);
        });
        helper.runAfterDelay(3, () -> helper.assertTrue(Cutscenes.gezien(p, Spiegel.SCENE_ID) && Cutscenes.bezig(p) && Knipogen.gestart(p) == 1 && lijn.stap(p) == 4,
                "after the mirror scene the mirror has one more thing to show; the step waits"));
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.SPIEGEL) && lijn.stap(p) == 5, "then step 5 (" + lijn.stap(p) + ")");
            CompoundTag waar = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.SPIEGEL.id());
            helper.assertTrue(BlockPos.of(waar.getLongOr("Pos", 0L)).equals(spiegel), "anchored on the mirror itself");
            kijk(p, spiegel);
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gestart(p) == 1 && lijn.stap(p) == 5, "looking again: no second wink");
            level.setBlockAndUpdate(spiegel, Blocks.AIR.defaultBlockState());
            weg(helper, p);
            helper.succeed();
        });
    }

    private static void kijk(ServerPlayer p, BlockPos spiegel) {
        p.level().getBlockState(spiegel).useWithoutItem(p.level(), p, new BlockHitResult(Vec3.atCenterOf(spiegel), Direction.UP, spiegel, false));
    }

    /**
     * Wink 6, the second half: the feast at home. The feast scene, Sjokkel's arrival in the feast's own frame, and only then
     * the end of the story.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_feest", timeoutTicks = 300)
    public static void ringknipoogSjokkelOpHetFeest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 12, 12);
        Verhaallijn lijn = RingH6Feature.LIJN;
        opStap(p, 6, 6);
        Thuis.testLevel = level;
        Thuis.testPlek = p.blockPosition();
        GuhQuests.saved(p).putLong(Thuis.THUIS, p.blockPosition().asLong());
        secondeH6(p);
        helper.assertTrue(Cutscenes.bezig(p) && lijn.stap(p) == 6 && !Ring.klaar(p), "at home on the ground: the feast plays");
        helper.onEachTick(() -> {
            stil(p);
            tik(p);
        });
        helper.runAfterDelay(3, () -> helper.assertTrue(Cutscenes.gezien(p, "ringh6_feest") && Cutscenes.bezig(p) && Knipogen.gestart(p) == 1 && !lijn.klaar(p),
                "after the feast scene somebody shuffles in; the story is not over yet"));
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.SJOKKEL) && lijn.klaar(p) && Ring.klaar(p), "then the story is done");
            CompoundTag feest = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_ringh6_feest"), sjokkel = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.SJOKKEL.id());
            helper.assertTrue(feest.contains("Pos") && feest.getLongOr("Pos", 0L) == sjokkel.getLongOr("Pos", 1L), "Sjokkel arrives at the feast's own spot");
            Thuis.testLevel = null;
            Thuis.testPlek = null;
            weg(helper, p);
            helper.succeed();
        });
    }

    /**
     * Wink 5: a click on Sam-guh on the Derde Richel at step 4. Boris first; Sam-guh carries once the player may be touched
     * again. Away from the ledge, and the second time, he carries at once.
     */
    @GuhTest(template = "ringh6_test_berg", batch = BATCH + "_boris", timeoutTicks = 600)
    public static void ringknipoogBorisOpDeBerg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Berg.wisTest(level);
        Berg.Kopie berg = Berg.zetTest(level, Berg.test(), helper.absolutePos(new BlockPos(0, 1, 0)));
        ServerPlayer p = speler(helper, 0, 0), ver = speler(helper, 0, 0), tweede = speler(helper, 0, 0);
        Verhaallijn lijn = RingH6Feature.LIJN;
        for (ServerPlayer x : List.of(p, ver, tweede)) {
            opStap(x, 6, 4);
            Ring.geef(x);
            Gaven.geef(x);
            GuhQuests.saved(x).putBoolean("guhs_kaart_ring_h6", true);
        }
        // (this one saw Boris before)
        GuhQuests.saved(tweede).put("guhs_scene_" + Knipogen.BORIS.id(), gezienTag());
        zet(p, berg.midden("richel_3"));
        zet(tweede, berg.midden("richel_3"));
        zet(ver, berg.midden("richel_1"));
        helper.assertTrue(Verteller.gezien(p, "ring_h6"), "(the chapter's card was read)");
        for (ServerPlayer x : List.of(p, ver, tweede)) {
            Sam.roep(x);
            secondeH6(x);                                     // (ring-h6 finds the mountain its player stands on)
        }
        GuhEntity sam = Sam.van(p), samVer = Sam.van(ver), samTweede = Sam.van(tweede);
        helper.assertTrue(sam != null && samVer != null && samTweede != null && Berg.van(p) != null && Berg.van(ver) != null, "Sam-guh walks along, on the mountain");
        // far from the ledge, and for who saw Boris before: he just carries
        helper.assertTrue(klikSam(samVer, ver) == InteractionResult.SUCCESS && ver.getVehicle() == samVer && Knipogen.gestart(ver) == 0, "away from the Derde Richel: no Boris");
        helper.assertTrue(klikSam(samTweede, tweede) == InteractionResult.SUCCESS && tweede.getVehicle() == samTweede && Knipogen.gestart(tweede) == 0,
                "the second time he carries at once");
        ver.stopRiding();
        tweede.stopRiding();
        // on the ledge, the first time: Boris first
        helper.assertTrue(klikSam(sam, p) == InteractionResult.SUCCESS && Cutscenes.bezig(p) && p.getVehicle() == null && Knipogen.gestart(p) == 1,
                "on the Derde Richel: Boris has his say first");
        boolean[] gedragen = {false};
        int[] tel = {0};
        helper.onEachTick(() -> {
            stil(p);
            tik(p);
            tel[0]++;
            gedragen[0] |= p.getVehicle() == sam && Sam.doet(sam).equals(Sam.DRAAGT);
            helper.assertTrue(tel[0] > 40 || !gedragen[0], "not while the player is still looked after (tick " + tel[0] + ")");
        });
        helper.runAfterDelay(4, () -> helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.BORIS) && p.getVehicle() == null && lijn.stap(p) == 4,
                "Boris is gone; Sam-guh waits until his player may be touched"));
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(gedragen[0] && Knipogen.gestart(p) == 1, "and then he carried, by himself");
            p.stopRiding();
            Berg.wisTest(level);
            weg(helper, p, ver, tweede);
            helper.succeed();
        });
    }

    private static void zet(ServerPlayer p, Vec3 plek) {
        p.snapTo(plek.x, plek.y, plek.z);
        p.setOnGround(true);
        p.setDeltaMovement(Vec3.ZERO);
    }

    /** A click on a Sam-guh as ring-kern hands it round: the scenes' own answers first (ring-h6's is among them). */
    private static InteractionResult klikSam(GuhEntity sam, ServerPlayer p) {
        for (VerhaalGuhs.Klik k : Sam.BIJ_KLIK) {
            InteractionResult r = k.klik(sam, p, InteractionHand.MAIN_HAND);
            if (r != InteractionResult.PASS) {
                return r;
            }
        }
        return InteractionResult.PASS;
    }

    // =====================================================================================================================
    // the winks that find their own moment
    // =====================================================================================================================

    /**
     * Wink 7: the west forecourt of the mine. Walking onto it shows the chapter's narrator card; the moment it closes
     * Professor Knabbelkloon shuffles up, anchored just outside the gate. Whoever missed that moment gets it within a
     * second; nobody gets it off the forecourt, or at another step.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_kloon", timeoutTicks = 300)
    public static void ringknipoogKloonBijDePoort(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MijnProef.weg(level);
        // template block (24, 31, 50) is the room's (0, 2, 0): the forecourt lies in the room
        BlockPos nul = helper.absolutePos(new BlockPos(0, 2, 0)).subtract(new BlockPos(24, Plekken.BOVEN, 50));
        MijnProef.kopie(level, nul, Rotation.NONE, kamer(helper));
        Mijn m = Mijn.bij(level, helper.absolutePos(new BlockPos(12, 3, 12)));
        helper.assertTrue(m != null, "the mine's frame lies over the room");
        BlockPos poort = m.wereld(Plekken.POORT_BUITEN);
        ServerPlayer p = speler(helper, 0, 0), later = speler(helper, 0, 0), buiten = speler(helper, 0, 0), verder = speler(helper, 0, 0);
        Verhaallijn lijn = RingH3Feature.LIJN;
        opStap(p, 3, 0);
        opStap(later, 3, 1);
        opStap(buiten, 3, 1);
        opStap(verder, 3, 2);
        zet(p, m.midden(new BlockPos(30, Plekken.BOVEN, 62)));
        zet(later, m.midden(new BlockPos(30, Plekken.BOVEN, 64)));
        zet(verder, m.midden(new BlockPos(30, Plekken.BOVEN, 66)));
        zet(buiten, m.midden(new BlockPos(46, Plekken.BOVEN, 62)));         // (inside the mine, behind the gate)
        Mijn.vergeet(p.getUUID());
        p.tickCount = 0;
        MijnEvents.tick(p);
        helper.assertTrue(Cutscenes.bezig(p) && lijn.stap(p) == 0 && Knipogen.gestart(p) == 0, "on the forecourt: the narrator card of chapter 3");
        helper.assertTrue(!Knipogen.kloon(p), "not before the card is read");
        // whoever stands there at step 1 already (the card closed while they could not watch): within a second
        Knipogen.seconde(buiten);
        Knipogen.seconde(verder);
        helper.assertTrue(Knipogen.gestart(buiten) == 0 && Knipogen.gestart(verder) == 0 && !Cutscenes.bezig(buiten) && !Cutscenes.bezig(verder),
                "not off the forecourt, not at another step");
        Knipogen.seconde(later);
        helper.assertTrue(Cutscenes.bezig(later) && Knipogen.gestart(later) == 1, "at the gate riddle on the forecourt: the professor comes");
        helper.onEachTick(() -> {
            for (ServerPlayer x : List.of(p, later)) {
                stil(x);
                tik(x);
            }
        });
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(lijn.stap(p) == 1 && Verteller.gezien(p, RingH3Feature.KAART) && Cutscenes.bezig(p) && Knipogen.gestart(p) == 1,
                    "the card closes: step 1, and the professor at once (step " + lijn.stap(p) + ", started " + Knipogen.gestart(p) + ")");
            CompoundTag waar = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.KLOON.id());
            helper.assertTrue(BlockPos.of(waar.getLongOr("Pos", 0L)).equals(poort), "anchored just outside the gate: " + poort);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.KLOON) && Knipogen.gezien(later, Knipogen.KLOON) && lijn.stap(p) == 1, "seen; the gate riddle is next");
            helper.assertTrue(!Knipogen.kloon(p) && Knipogen.gestart(p) == 1, "once");
        });
        helper.runAfterDelay(50, () -> {
            Knipogen.seconde(p);
            helper.assertTrue(Duwtje.mag(p) && !Cutscenes.bezig(p) && Knipogen.gestart(p) == 1, "and never again");
            MijnProef.weg(level);
            weg(helper, p, later, buiten, verder);
            helper.succeed();
        });
    }

    /**
     * Wink 4: Sausuman's machine hall. A player at "pull the lever" who stands in the hall in front of the Ringenbakker:
     * the 626-guh comes by, anchored on the machine and turned like it. Not at another step, not once the machine is
     * filled, not outside the hall, not twice.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_stitch", timeoutTicks = 300)
    public static void ringknipoogStitchInDeToren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState bakker = RingSausumanFeature.RINGENBAKKER.get().defaultBlockState();
        // one machine that looks south (as in the template), one that looks west (a copy turned a quarter)
        BlockPos zuid = helper.absolutePos(new BlockPos(5, 3, 2)), west = helper.absolutePos(new BlockPos(22, 3, 20));
        level.setBlockAndUpdate(zuid, bakker.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        Verhaallijn lijn = RingSausumanFeature.LIJN;
        ServerPlayer p = speler(helper, 5, 7), vroeg = speler(helper, 4, 7), gevuld = speler(helper, 6, 7), opzij = speler(helper, 5, 16), gedraaid = speler(helper, 17, 20);
        for (ServerPlayer x : List.of(p, vroeg, gevuld, opzij, gedraaid)) {
            opStap(x, 5, 0);
            lijn.begin(x);
            lijn.zet(x, x == vroeg ? 1 : 2);
        }
        lijn.vlag(gevuld, Bakkerij.GEVULD, true);
        helper.assertTrue(lijn.aanDeBeurt(p) && lijn.stap(p) == 2, "(the tower's questline is at the lever)");
        Vec3 lokaal = Knipogen.lokaal(zuid, Rotation.NONE, p.position());
        helper.assertTrue(Math.abs(lokaal.x - 0.5) < 1e-6 && Math.abs(lokaal.y + 1) < 1e-6 && Math.abs(lokaal.z - 5.5) < 1e-6, "the hall's frame: " + lokaal);
        for (Rotation draai : Rotation.values()) {
            Vec3 heen = Cutscene.wereld(zuid, draai, new Vec3(-2.25, -1, 6.75));
            Vec3 terug = Knipogen.lokaal(zuid, draai, heen);
            helper.assertTrue(terug.distanceTo(new Vec3(-2.25, -1, 6.75)) < 1e-6, "world -> the machine's frame is the opposite of the engine's frame -> world, turned " + draai);
        }
        for (ServerPlayer x : List.of(vroeg, gevuld, opzij)) {
            Knipogen.seconde(x);
            helper.assertTrue(!Cutscenes.bezig(x) && Knipogen.gestart(x) == 0, "no wink before the ingredients, after the lever, or outside the hall");
        }
        Knipogen.seconde(p);
        helper.assertTrue(Cutscenes.bezig(p) && Knipogen.gestart(p) == 1, "in the hall with the three ingredients: the 626-guh comes by");
        CompoundTag waar = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.STITCH.id());
        helper.assertTrue(BlockPos.of(waar.getLongOr("Pos", 0L)).equals(zuid) && waar.getIntOr("Draai", -1) == Rotation.NONE.ordinal(), "anchored on the Ringenbakker, unturned");
        // a turned tower: the hall lies west of a machine that looks west
        level.setBlockAndUpdate(zuid, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(west, bakker.setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        Knipogen.seconde(opzij);
        helper.assertTrue(!Cutscenes.bezig(opzij), "south of a machine that looks west is not its hall");
        Knipogen.seconde(gedraaid);
        CompoundTag draai = GuhQuests.saved(gedraaid).getCompoundOrEmpty("guhs_scene_" + Knipogen.STITCH.id());
        helper.assertTrue(Cutscenes.bezig(gedraaid) && BlockPos.of(draai.getLongOr("Pos", 0L)).equals(west) && draai.getIntOr("Draai", -1) == Rotation.CLOCKWISE_90.ordinal(),
                "in front of it: the wink, turned with the machine");
        helper.onEachTick(() -> {
            for (ServerPlayer x : List.of(p, gedraaid)) {
                stil(x);
                tik(x);
            }
        });
        helper.runAfterDelay(4, () -> helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.STITCH) && lijn.stap(p) == 2 && !lijn.vlag(p, Bakkerij.GEVULD),
                "seen; nothing of the questline moved"));
        helper.runAfterDelay(50, () -> {
            level.setBlockAndUpdate(zuid, bakker.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            Knipogen.seconde(p);
            helper.assertTrue(Duwtje.mag(p) && !Cutscenes.bezig(p) && Knipogen.gestart(p) == 1, "once");
            level.setBlockAndUpdate(zuid, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(west, Blocks.AIR.defaultBlockState());
            weg(helper, p, vroeg, gevuld, opzij, gedraaid);
            helper.succeed();
        });
    }

    /**
     * Wink 2: level 1-1 as the generator wrote it. The ?-block with the medicine chest is the coin block at s 73; bumping
     * it gives its coin as always and, the first time, the wink in the lane's own frame (its camera side); the level stands
     * still meanwhile. Not before the bump, not from far away, not a second time.
     */
    @GuhTest(template = "empty", batch = BATCH + "_kistje", timeoutTicks = 4000)
    public static void ringknipoogKistjeInVraagblok(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String naam = "guhriow1_test_1_1";
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        helper.assertTrue(template != null, "the template guhs:" + naam);
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 40, 0));
        forceer(level, hoek, template, true);
        template.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        ServerPlayer p = speler(helper, 0, 0);
        helper.assertTrue(GuhrioSpel.start(p, cel(hoek, 2, 3)), "the start block of 1-1 lets the player in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(s != null && Knipogen.KISTJE_LEVEL.equals(s.level().level().id()), "level 1-1");
        BlockPos blok = cel(hoek, Knipogen.KISTJE_S, 6);
        BlockState state = level.getBlockState(blok);
        helper.assertTrue(state.getBlock() instanceof GuhrioBlocks.VraagBlok && state.getValue(GuhrioBlocks.INHOUD) == GuhrioBlocks.Inhoud.MUNT, "a coin ?-block at s 73: " + state);
        helper.assertTrue(blok.equals(Knipogen.kistjeBlok(s)), "the block with the medicine chest: " + Knipogen.kistjeBlok(s));
        helper.assertTrue(Knipogen.naarCamera(Direction.SOUTH) == Rotation.NONE && Knipogen.naarCamera(Direction.WEST) == Rotation.CLOCKWISE_90
                && Knipogen.naarCamera(Direction.NORTH) == Rotation.CLOCKWISE_180 && Knipogen.naarCamera(Direction.EAST) == Rotation.COUNTERCLOCKWISE_90, "a scene's +z to the camera's side");
        for (Direction kant : Direction.Plane.HORIZONTAL) {
            Vec3 z = Cutscene.wereld(BlockPos.ZERO, Knipogen.naarCamera(kant), new Vec3(0.5, 0, 4.5));
            helper.assertTrue(Direction.getApproximateNearest(z.x - 0.5, 0, z.z - 0.5) == kant, "+z of the scene points " + kant);
        }
        // under the block, not bumped yet
        zetCel(p, hoek, Knipogen.KISTJE_S, 3);
        GuhrioSpel.tick(p);
        helper.assertTrue(!Knipogen.kistje(p) && !Cutscenes.bezig(p), "nothing before the bump");
        int munten = s.munten;
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, blok, 0);
        helper.assertTrue(s.staat(blok) == 1 && s.munten == munten + 1, "bumped: its coin is the player's, as of any ?-block (" + s.munten + ")");
        // (somebody who is far from the block by the time the look comes round: not now)
        zetCel(p, hoek, 20, 3);
        helper.assertTrue(!Knipogen.kistje(p), "not from the other end of the lane");
        zetCel(p, hoek, Knipogen.KISTJE_S, 3);
        Knipogen.tik(p);
        helper.assertTrue(Cutscenes.bezig(p) && Knipogen.gestart(p) == 1, "the medicine chest pops out");
        CompoundTag waar = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + Knipogen.KISTJE.id());
        helper.assertTrue(BlockPos.of(waar.getLongOr("Pos", 0L)).equals(blok) && waar.getIntOr("Draai", -1) == Rotation.NONE.ordinal(),
                "anchored on the block; the lane runs east and its camera stands south: unturned");
        int ticks = s.ticks;
        GuhrioSpel.tick(p);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.ticks == ticks, "the level stands still while the wink plays");
        helper.onEachTick(() -> {
            stil(p);
            tik(p);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Knipogen.gezien(p, Knipogen.KISTJE) && GuhrioSpel.sessie(p) == s, "seen; the level goes on");
            helper.assertTrue(!Knipogen.kistje(p) && Knipogen.gestart(p) == 1, "once");
            GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
            weg(helper, p);
            BlockPos eind = hoek.offset(template.getSize().getX() - 1, template.getSize().getY() - 1, template.getSize().getZ() - 1);
            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(hoek.getX() - 2, hoek.getY() - 2, hoek.getZ() - 2, eind.getX() + 3, eind.getY() + 3, eind.getZ() + 3),
                    e -> !(e instanceof Player))) {
                e.discard();
            }
            for (BlockPos pos : BlockPos.betweenClosed(hoek, eind)) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
            forceer(level, hoek, template, false);
            helper.succeed();
        });
    }

    /** The block of cell (s, row) of a level whose test template stands at {@code hoek} (as guhrio-w1's tests). */
    private static BlockPos cel(BlockPos hoek, int s, int rij) {
        return hoek.offset(1 + s, 2 + rij, 1);
    }

    private static void zetCel(ServerPlayer p, BlockPos hoek, int s, int rij) {
        BlockPos c = cel(hoek, s, rij);
        p.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    private static void forceer(ServerLevel level, BlockPos hoek, StructureTemplate template, boolean aan) {
        for (int cx = hoek.getX() >> 4; cx <= (hoek.getX() + template.getSize().getX()) >> 4; cx++) {
            for (int cz = hoek.getZ() >> 4; cz <= (hoek.getZ() + template.getSize().getZ()) >> 4; cz++) {
                level.setChunkForced(cx, cz, aan);
            }
        }
    }

    /**
     * Wink 6, the first half: Sjokkel on the west bridge head of the mine. An inhabitant of every copy (he comes by
     * himself, once), a real Sjokkel that stands still, can't be hurt, fed or tamed, that only players at the great hall's
     * step see, and that shuffles towards the bridge while one of them is there; the player is told once who that is.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_sjokkel", timeoutTicks = 300)
    public static void ringknipoogSjokkelOpDeBrug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MijnProef.weg(level);
        // template block (52, 12, 6) is the room's (0, 2, 0): the west bridge head lies in the room
        BlockPos nul = helper.absolutePos(new BlockPos(0, 2, 0)).subtract(new BlockPos(52, Plekken.DIEP, 6));
        StructureStart start = MijnProef.kopie(level, nul, Rotation.NONE, kamer(helper));
        Mijn m = Mijn.bij(level, helper.absolutePos(new BlockPos(12, 3, 12)));
        helper.assertTrue(m != null && start != null, "the mine's frame lies over the room");
        BlockPos begin = m.wereld(Sjokkel.BEGIN);
        helper.assertTrue(level.getBlockState(begin.below()).blocksMotion() && level.getBlockState(begin).isAir(), "his spot is on the room's floor: " + begin);
        helper.assertTrue(Sjokkel.van(m) == null, "no Sjokkel yet");
        // he is an inhabitant of the mine: seen missing long enough, he comes (and only once)
        Bezetting.zetGemist(level, Sjokkel.ID, start, Bezetting.BEVESTIG + 1);
        Bezetting.controleer(level, begin);
        ShuckleEntity sjokkel = Sjokkel.van(m);
        helper.assertTrue(sjokkel != null && Sjokkel.isSjokkel(sjokkel) && Bezetting.isBezetting(sjokkel), "Sjokkel stands on the bridge head");
        Bezetting.zetGemist(level, Sjokkel.ID, start, Bezetting.BEVESTIG + 1);
        Bezetting.controleer(level, begin);
        helper.assertTrue(level.getEntitiesOfClass(ShuckleEntity.class, new AABB(begin).inflate(12), Sjokkel::isSjokkel).size() == 1, "one Sjokkel per mine");
        helper.assertTrue(sjokkel.isNoAi() && sjokkel.isInvulnerable() && sjokkel.isPersistenceRequired() && !sjokkel.isTame(), "he stands still, can't be hurt, never leaves");
        Vec3 van = m.wereld(Sjokkel.VAN), naar = m.wereld(Sjokkel.NAAR);
        helper.assertTrue(sjokkel.position().distanceTo(Vec3.atBottomCenterOf(begin)) < 0.01 && Math.abs(net.minecraft.util.Mth.wrapDegrees(sjokkel.getYRot() - 270f)) < 1f,
                "at the start of his line, looking east at the bridge");
        // who sees him: only players at the great hall's step
        ServerPlayer p = speler(helper, 0, 0), vroeg = speler(helper, 0, 0), klaar = speler(helper, 0, 0);
        opStap(p, 3, Sjokkel.STAP_HAL);
        opStap(vroeg, 3, 2);
        opStap(klaar, 4, 0);
        for (ServerPlayer x : List.of(p, vroeg, klaar)) {
            zet(x, m.midden(new BlockPos(55, Plekken.DIEP, 14)));
            x.setShiftKeyDown(true);                              // (sneaking: a Sjokkel does not pull into his shell for them)
        }
        helper.assertTrue(Zicht.magZien(p, sjokkel) && !Zicht.magZien(vroeg, sjokkel) && !Zicht.magZien(klaar, sjokkel), "only for players at the chase and the bridge");
        // he shuffles while such a player is there, a little a second, along the edge of the platform
        Knipogen.seconde(vroeg);
        Knipogen.seconde(klaar);
        helper.assertTrue(sjokkel.position().distanceTo(van) < 1e-6 || sjokkel.position().distanceTo(Vec3.atBottomCenterOf(begin)) < 0.01, "nobody at that step: he waits");
        helper.assertTrue(!GuhQuests.saved(p).getBooleanOr(Sjokkel.GEZIEN, false), "(not told yet)");
        Knipogen.seconde(p);
        helper.assertTrue(sjokkel.position().distanceTo(van) < 1e-6, "the first look: at the start of his journey: " + sjokkel.position());
        helper.assertTrue(GuhQuests.saved(p).getBooleanOr(Sjokkel.GEZIEN, false) && !GuhQuests.saved(vroeg).getBooleanOr(Sjokkel.GEZIEN, false), "the player is told who that is");
        helper.assertTrue(NlTekst.has("quest.guhs.ringknipoog.sjokkel.brug") && NlTekst.has("quest.guhs.ringknipoog.sjokkel.klik"), "the two lines about him exist");
        Knipogen.seconde(p);
        helper.assertTrue(sjokkel.position().distanceTo(van) < 1e-6, "one shuffle a game tick, however many look");
        int[] tel = {0};
        helper.onEachTick(() -> {
            if (++tel[0] <= 12) {
                Knipogen.seconde(p);
            }
        });
        helper.runAfterDelay(13, () -> {
            double ver = sjokkel.position().distanceTo(van);
            helper.assertTrue(Math.abs(ver - 12 * Sjokkel.STAP) < 1e-3 && Math.abs(sjokkel.getZ() - van.z) < 1e-6 && sjokkel.getX() > van.x && sjokkel.getX() < naar.x,
                    "twelve shuffles east along his line: " + ver);
            // a click: nothing but a line (no feeding, no taming, no picking up)
            PlayerInteractEvent.EntityInteractSpecific klik = new PlayerInteractEvent.EntityInteractSpecific(p, InteractionHand.MAIN_HAND, sjokkel, Vec3.ZERO);
            helper.assertTrue(NeoForge.EVENT_BUS.post(klik).isCanceled() && !sjokkel.isTame(), "a click on him is swallowed");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p, InteractionHand.MAIN_HAND, sjokkel)).isCanceled(), "both ways");
            // the end of his line: he waits at the head of the bridge
            sjokkel.snapTo(naar.x - 0.01, naar.y, naar.z, sjokkel.getYRot(), 0f);
            sjokkel.getPersistentData().putLong(Sjokkel.TAG, level.getGameTime() - 1);
            Knipogen.seconde(p);
            helper.assertTrue(sjokkel.position().distanceTo(naar) < 1e-6, "at the head of the bridge he waits");
            // nobody looked for a while: he starts again
            sjokkel.getPersistentData().putLong(Sjokkel.TAG, level.getGameTime() - Sjokkel.OPNIEUW - 1);
            Knipogen.seconde(p);
            helper.assertTrue(sjokkel.position().distanceTo(van) < 1e-6, "for the next player it is the start of his journey again");
            sjokkel.discard();
            MijnProef.weg(level);
            weg(helper, p, vroeg, klaar);
            helper.succeed();
        });
    }

    /** The characters the winks borrow exist, and every wink knows the story it winks at. */
    @GuhTest(template = "empty", batch = BATCH + "_gasten")
    public static void ringknipoogGastenBestaan(GameTestHelper helper) {
        for (GuhNpcEntity.Kind kind : List.of(GuhNpcEntity.Kind.BORIS, GuhNpcEntity.Kind.KNABBELKLOON, GuhNpcEntity.Kind.PADGUH, GuhNpcEntity.Kind.SAUSUMAN,
                GuhNpcEntity.Kind.GUHDALF, GuhNpcEntity.Kind.GUHLADRIEL)) {
            helper.assertTrue(NlTekst.has("entity.guhs.guh_npc." + kind.id()), "the character " + kind.id());
        }
        for (Cutscene k : Knipogen.alle()) {
            helper.assertTrue(NlTekst.has(k.titelKey()), "the title of " + k.id());
            for (Cutscene.Zeg z : k.zinnen()) {
                helper.assertTrue(NlTekst.has(k.tekstKey(z.key())), k.id() + " says " + z.key());
                helper.assertTrue(z.spreker().isEmpty() || NlTekst.has(k.tekstKey("naam." + z.spreker())), k.id() + ": the name of " + z.spreker());
                helper.assertTrue(z.t() + z.ticks() <= k.duur(), k.id() + ": '" + z.key() + "' is over before the scene is");
            }
            // every actor is an entity type that exists (the suppliers are only asked on a client)
            for (Cutscene.Acteur a : k.acteurs()) {
                helper.assertTrue(a.type() == null || a.type().get() != null, k.id() + ": the actor " + a.naam());
            }
        }
        // the lines the user gave, word for word
        helper.assertTrue("Hmmm. Volgens mij ben ik een verkeerde afslag afgevadst... Hier is geen medicijnenkistje...".equals(nl(Knipogen.BALTOGUH, "afslag")), "Baltoguh");
        helper.assertTrue("Daar zocht iemand naar, njeg.".equals(nl(Knipogen.KISTJE, "zocht")), "Pad-guh");
        helper.assertTrue("Aloha, njeg!".equals(nl(Knipogen.STITCH, "aloha")) && "WIE laat dat ding steeds binnen?!".equals(nl(Knipogen.STITCH, "wie")), "the 626-guh and Sausuman");
        helper.assertTrue("Een normale guh kan deze berg niet op... maar heel misschien...".equals(nl(Knipogen.BORIS, "berg"))
                && "Hé, dat is mijn moment.".equals(nl(Knipogen.BORIS, "moment")), "Boris and Sam-guh");
        helper.assertTrue("Twee ringen is twee keer zo lekker!".equals(nl(Knipogen.KLOON, "twee")) && "Nee.".equals(nl(Knipogen.KLOON, "nee")), "the professor and Guhdalf");
        helper.succeed();
    }

    private static String nl(Cutscene k, String key) {
        return NlTekst.get(k.tekstKey(key));
    }

    private RingKnipoogGameTests() {
    }
}
