package nl.juiced.guhs.feature.oudescenes;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.baltoslee.SleeTocht;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.hemel.Wolkenhoeder;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoVerhaal;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.Timmerguh;
import nl.juiced.guhs.feature.timmerguh.TimmerguhFeature;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * bbq2 (oude-scenes): the six scenes of the older stories. For each: the story's own moment starts the scene and waits for
 * it (the trigger), the story goes on when it was watched, and a second time the story goes on at once without a scene
 * (once per player). Besides: a player who is past the moments gets the scenes in the Guhdex and nothing else; a mock
 * player without a client is left alone (the old stories' own tests see no difference); no scene when a camera would hang
 * in a block, another rotation when there is no copy of the building; the anchor and rotation of a real copy; the rules of
 * DESIGN_VERHALENPAD B for the scripts (15 to 25 seconds, calm shake, the weather only for the viewer).
 * Mock players are not ticked by the server and get no payloads: the tests post their tick themselves, and a scene "plays"
 * two ticks for them. Template oudescenes_test_kamer: 15 x 12 x 15, a floor at y 0 (things stand at helper y 2).
 */
public class OudeScenesGameTests {
    private static final String KAMER = "oudescenes_test_kamer";
    private static final String BATCH = "oudescenes";
    private static final BlockPos MIDDEN = new BlockPos(7, 2, 7);
    /** Ticks after which a mock player's scene is surely over. */
    private static final int NA = 8;

    private static ServerPlayer speler(GameTestHelper helper, boolean metClient) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(MIDDEN);
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        OudeScenes.proef(p, metClient);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            OudeScenes.proef(p, false);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void tik(ServerPlayer p) {
        if (!p.isRemoved()) {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(9, 2, 7));
        npc.setKind(kind);
        return npc;
    }

    private static boolean heeft(ServerPlayer p, String quest) {
        AdvancementHolder a = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + quest));
        return a != null && p.getAdvancements().getOrStartProgress(a).isDone();
    }

    /**
     * The rule every scene keeps: the story's moment ({@code moment}) starts the scene for p and the story waits
     * ({@code verder} is false); when the scene was watched the story has gone on and the scene counts as seen; with the
     * story put back before the moment ({@code terug}) the moment plays NO scene and the story goes on at once.
     */
    private static void eenKeerPerSpeler(GameTestHelper helper, OudeScene s, ServerPlayer p, Runnable moment, BooleanSupplier verder, Runnable terug,
                                         Runnable opruimen) {
        helper.assertTrue(!Cutscenes.gezien(p, s.id()) && !Cutscenes.bezig(p) && !verder.getAsBoolean() && !s.voorbij().test(p),
                s.naam() + ": before the moment");
        moment.run();
        helper.assertTrue(Cutscenes.bezig(p), s.naam() + ": the story's moment starts the scene");
        helper.assertTrue(!verder.getAsBoolean() && !Cutscenes.gezien(p, s.id()), s.naam() + ": the story waits for the scene");
        helper.assertTrue(OudeScenes.bijwerken(p) == 0 && !Cutscenes.gezien(p, s.id()), s.naam() + ": watching is not \"past the moment\"");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(NA, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Cutscenes.gezien(p, s.id()), s.naam() + ": watched to the end, seen");
            helper.assertTrue(verder.getAsBoolean(), s.naam() + ": the story went on after the scene");
            CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + s.id());
            helper.assertTrue(BlockPos.of(t.getLongOr("Pos", 0L)).closerThan(p.blockPosition(), 12), s.naam() + ": the Guhdex replays it where it played");
            // once per player: the same moment again (the story put back) plays nothing
            terug.run();
            helper.assertTrue(!verder.getAsBoolean(), s.naam() + ": (the story is back before its moment)");
            moment.run();
            helper.assertTrue(!Cutscenes.bezig(p), s.naam() + ": no second scene for the same player");
            helper.assertTrue(verder.getAsBoolean(), s.naam() + ": the second time the story goes on at once");
            opruimen.run();
            weg(helper, p);
            helper.succeed();
        });
    }

    private static List<GuhNpcEntity> wolven(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, new AABB(helper.absolutePos(MIDDEN)).inflate(12),
                n -> n.getKind() == GuhNpcEntity.Kind.WITTE_WOLFGUH);
    }

    /** Balto: the dieptepunt of the ride back. The first time the scene (and no talking scene, no wolf NPC), then the howl. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesBalto(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        BaltoVerhaal.zet(p, BaltoVerhaal.TERUG);
        boolean[] tweede = {false};
        eenKeerPerSpeler(helper, OudeScenes.BALTO, p, () -> BaltoVerhaal.opMoment(p, SleeTocht.Moment.DIEPTEPUNT),
                // the first time: the howl happened (its advancement); the second time: the old talking scene put the wolf-guh there
                () -> tweede[0] ? !wolven(helper).isEmpty() : heeft(p, "balto_wolf"),
                () -> {
                    helper.assertTrue(wolven(helper).isEmpty(), "balto: the scene had its own wolf-guh, no NPC was put in the world");
                    tweede[0] = true;
                },
                () -> wolven(helper).forEach(Entity::discard));
    }

    /** Mewtwo: the professor has all six notes: what he remembers plays before he gives the next step. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesMewtwo(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
        for (int n = 1; n <= MewtwoFeature.NOTITIES; n++) {
            MewtwoVoortgang.vondNotitie(p, n);
        }
        eenKeerPerSpeler(helper, OudeScenes.MEWTWO, p, () -> MewtwoVerhaal.notitiesKlaar(p, null),
                () -> MewtwoVoortgang.stap(p) == MewtwoVoortgang.ONDERDELEN, () -> MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES), () -> {
                });
    }

    /** Lilo &amp; Stitch: back at Lilo-guh at the ohana step. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesOhana(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        GuhNpcEntity lilo = npc(helper, GuhNpcEntity.Kind.LILO_GUH);
        Ohana.zet(p, Ohana.OHANA);
        eenKeerPerSpeler(helper, OudeScenes.OHANA, p, () -> Ohana.LILO.talk(lilo, p), () -> Ohana.stap(p) == Ohana.KLAAR, () -> Ohana.zet(p, Ohana.OHANA),
                lilo::discard);
    }

    /** Hemelkapelletje: the wolkenhoeder has all three things: the heart only beats (for the server) after the scene. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesHemel(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        GuhNpcEntity hoeder = npc(helper, GuhNpcEntity.Kind.WOLKENHOEDER);
        Wolkenhoeder rol = new Wolkenhoeder();
        Runnable alles = () -> {
            HemelQuest.vergeet(p);
            HemelQuest.zetStap(p, 1);
            GuhQuests.saved(p).putInt(HemelQuest.GEBRACHT, 7);
        };
        alles.run();
        eenKeerPerSpeler(helper, OudeScenes.HEMEL, p, () -> rol.talk(hoeder, p), () -> HemelQuest.klopt(p), alles, hoeder::discard);
    }

    /** Grillguh: his pit burns again. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesGrill(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        GuhNpcEntity grillguh = npc(helper, GuhNpcEntity.Kind.GRILLGUH);
        Grillguh.setStep(p, Grillguh.BLOKJES);
        eenKeerPerSpeler(helper, OudeScenes.GRILL, p, () -> Grillguh.complete(p, grillguh), () -> Grillguh.step(p) == Grillguh.DONE,
                () -> Grillguh.setStep(p, Grillguh.BLOKJES), grillguh::discard);
    }

    /** Timmerguh: the roof is whole (no ghost tile left) and the player who laid it talks to him. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesTimmer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true);
        GuhNpcEntity timmerguh = npc(helper, GuhNpcEntity.Kind.TIMMERGUH);
        BlockPos dak = new BlockPos(9, 5, 9);
        helper.setBlock(dak, TimmerguhFeature.DAKPLEK.get());
        helper.assertTrue(Timmerguh.plekken(timmerguh).size() == 1, "(the Timmerguh knows his roof: one spot)");
        helper.setBlock(dak, Blocks.PINK_WOOL);
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.DAK);
        eenKeerPerSpeler(helper, OudeScenes.TIMMER, p, () -> Timmerguh.ROLE.talk(timmerguh, p), () -> TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.BEWONER,
                () -> TimmerguhVoortgang.zet(p, TimmerguhVoortgang.DAK), timmerguh::discard);
    }

    /**
     * Every viewer has their own scene: a second player at the same moment gets theirs while the first still watches, and
     * one player's scene is not the other's.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesPerSpeler(GameTestHelper helper) {
        ServerPlayer a = speler(helper, true), b = speler(helper, true);
        GuhNpcEntity grillguh = npc(helper, GuhNpcEntity.Kind.GRILLGUH);
        Grillguh.setStep(a, Grillguh.BLOKJES);
        Grillguh.setStep(b, Grillguh.BLOKJES);
        Grillguh.complete(a, grillguh);
        helper.assertTrue(Cutscenes.bezig(a) && !Cutscenes.bezig(b), "only the player whose moment it is watches");
        Grillguh.complete(b, grillguh);
        helper.assertTrue(Cutscenes.bezig(a) && Cutscenes.bezig(b), "two players watch at the same spot");
        helper.onEachTick(() -> {
            tik(a);
            tik(b);
        });
        helper.runAfterDelay(NA, () -> {
            helper.assertTrue(Grillguh.step(a) == Grillguh.DONE && Grillguh.step(b) == Grillguh.DONE, "both stories went on");
            helper.assertTrue(Cutscenes.gezien(a, OudeScenes.GRILL.id()) && Cutscenes.gezien(b, OudeScenes.GRILL.id()), "both saw it");
            grillguh.discard();
            weg(helper, a, b);
            helper.succeed();
        });
    }

    /**
     * A player who is past the moments already (they were further when this was added): nothing plays, nothing is asked of
     * them, the six scenes are in their Guhdex to watch (far from the buildings: as a picture book). A fresh player has none.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesVoorbij(GameTestHelper helper) {
        ServerPlayer p = speler(helper, true), nieuw = speler(helper, true);
        GuhNpcEntity grillguh = npc(helper, GuhNpcEntity.Kind.GRILLGUH);
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR);
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        Ohana.zet(p, Ohana.KLAAR);
        GuhQuests.saved(p).putBoolean(HemelQuest.HART, true);
        Grillguh.setStep(p, Grillguh.DONE);
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KLAAR);
        for (OudeScene s : OudeScenes.ALLE) {
            helper.assertTrue(s.voorbij().test(p) && !s.voorbij().test(nieuw) && !Cutscenes.gezien(p, s.id()), s.naam() + ": past its moment, never seen");
        }
        boolean[] daarna = {false};
        OudeScenes.opLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        OudeScenes.opLogin(new PlayerEvent.PlayerLoggedInEvent(nieuw));
        for (OudeScene s : OudeScenes.ALLE) {
            helper.assertTrue(Cutscenes.gezien(p, s.id()), s.naam() + ": in the Guhdex of whoever is past it");
            helper.assertTrue(!Cutscenes.gezien(nieuw, s.id()), s.naam() + ": not in the Guhdex of a player who has not got there");
            helper.assertTrue(!OudeScenes.speel(p, s, p.blockPosition(), x -> daarna[0] = true) && !Cutscenes.bezig(p) && !daarna[0],
                    s.naam() + ": never played for a player who is past it");
        }
        helper.assertTrue(OudeScenes.bijwerken(p) == 0 && OudeScenes.bijwerken(nieuw) == 0, "nothing more to do the second time");
        // the stories themselves are untouched for them
        Grillguh.complete(p, grillguh);
        helper.assertTrue(Grillguh.step(p) == Grillguh.DONE && !Cutscenes.bezig(p), "a finished story stays finished, no scene");
        // the Guhdex button: no copy of the chapel here, so the picture book
        Cutscenes.herbekijk(p, OudeScenes.HEMEL.id());
        helper.assertTrue(Cutscenes.bezig(p), "the Guhdex replays a scene the player never saw in the world");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(NA, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && HemelQuest.klopt(p), "the replay is over; nothing changed");
            grillguh.discard();
            weg(helper, p, nieuw);
            helper.succeed();
        });
    }

    /** A mock player without a client (every test of the old stories): no scene, the story goes on at once as it always did. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void oudescenesZonderClient(GameTestHelper helper) {
        ServerPlayer p = speler(helper, false);
        GuhNpcEntity grillguh = npc(helper, GuhNpcEntity.Kind.GRILLGUH);
        Grillguh.setStep(p, Grillguh.BLOKJES);
        Grillguh.complete(p, grillguh);
        helper.assertTrue(!Cutscenes.bezig(p) && Grillguh.step(p) == Grillguh.DONE, "no client, no scene: the story went on at once");
        helper.assertTrue(!Cutscenes.gezien(p, OudeScenes.GRILL.id()), "not seen...");
        helper.assertTrue(OudeScenes.bijwerken(p) == 1 && Cutscenes.gezien(p, OudeScenes.GRILL.id()), "...but past its moment: it is in the Guhdex");
        grillguh.discard();
        weg(helper, p);
        helper.succeed();
    }

    /**
     * No camera in a block: with something built where a camera hangs the scene is turned another way (there is no copy
     * of the building here), and when every way is blocked it does not play at all and the story simply goes on.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void oudescenesCameraVrij(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, true);
        OudeScene s = OudeScenes.TIMMER;
        BlockPos anker = p.blockPosition();
        // (the cameras of this scene stay inside the test room; the long scenes reach into the rooms next door, where another
        // test may have built something: for those it is enough that there is a way to turn them)
        for (Rotation r : Rotation.values()) {
            helper.assertTrue(OudeScenes.vrij(level, s.scene(), anker, r),
                    s.naam() + " " + r + ": every camera is free in an empty room (" + OudeScenes.geblokkeerd(level, s.scene(), anker, r) + ")");
        }
        for (OudeScene elk : OudeScenes.ALLE) {
            helper.assertTrue(OudeScenes.plek(level, elk, anker) != null, elk.naam() + ": plays in an empty room");
        }
        // the wall of barriers the test runner puts round a room is no wall for a camera (it is invisible)
        BlockPos rand = helper.absolutePos(new BlockPos(15, 4, 7));
        helper.assertTrue(level.getBlockState(rand).is(Blocks.BARRIER) || level.getBlockState(rand).isAir(), "(the rim of the room: " + level.getBlockState(rand) + ")");
        OudeScenes.Plek eerst = OudeScenes.plek(level, s, anker);
        helper.assertTrue(eerst != null && eerst.anker().equals(anker) && eerst.draai() == Rotation.NONE, "without a copy: on the spot, not turned");
        java.util.List<BlockPos> gebouwd = new java.util.ArrayList<>();
        Vec3 camera = s.scene().cameraOp(0)[0];
        BlockPos blok = BlockPos.containing(Cutscene.wereld(anker, Rotation.NONE, camera));
        level.setBlockAndUpdate(blok, Blocks.STONE.defaultBlockState());
        gebouwd.add(blok);
        helper.assertTrue(!OudeScenes.vrij(level, s.scene(), anker, Rotation.NONE), "a block where the first camera hangs");
        OudeScenes.Plek gedraaid = OudeScenes.plek(level, s, anker);
        helper.assertTrue(gedraaid != null && gedraaid.draai() != Rotation.NONE, "the scene is turned to where its cameras are free");
        for (Rotation r : Rotation.values()) {
            BlockPos b = BlockPos.containing(Cutscene.wereld(anker, r, camera));
            level.setBlockAndUpdate(b, Blocks.STONE.defaultBlockState());
            gebouwd.add(b);
        }
        helper.assertTrue(OudeScenes.plek(level, s, anker) == null, "blocked every way: no place for the scene");
        boolean[] daarna = {false};
        helper.assertTrue(!OudeScenes.speel(p, s, anker, x -> daarna[0] = true) && !Cutscenes.bezig(p) && !daarna[0] && !Cutscenes.gezien(p, s.id()),
                "no free camera: no scene (the story goes on by itself)");
        // water counts too (a camera under water)
        gebouwd.forEach(b -> level.setBlockAndUpdate(b, Blocks.AIR.defaultBlockState()));
        level.setBlockAndUpdate(blok, Blocks.WATER.defaultBlockState());
        helper.assertTrue(!OudeScenes.vrij(level, s.scene(), anker, Rotation.NONE), "a camera never hangs in water");
        level.setBlockAndUpdate(blok, Blocks.AIR.defaultBlockState());
        helper.assertTrue(OudeScenes.speel(p, s, anker, x -> daarna[0] = true) && Cutscenes.bezig(p), "the way is free again: it plays");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(NA, () -> {
            helper.assertTrue(daarna[0] && Cutscenes.gezien(p, s.id()), "watched: daarna ran");
            weg(helper, p);
            helper.succeed();
        });
    }

    /**
     * At a real copy of its building a scene is anchored on its own template block, turned like the copy (here: a copy of
     * the chapel made by hand around the test room, a quarter turn).
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void oudescenesKopie(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, true);
        OudeScene s = OudeScenes.HEMEL;
        try {
            helper.assertTrue(OudeScenes.kopie(level, s, p.blockPosition()) == null, "no copy of the chapel here yet");
            Structure structure = Kopieen.structuur(level, s.structuur());
            helper.assertTrue(structure != null, "the structure guhs:" + s.structuur() + " exists");
            for (OudeScene elk : OudeScenes.ALLE) {
                helper.assertTrue(Kopieen.structuur(level, elk.structuur()) != null, "the structure guhs:" + elk.structuur() + " of " + elk.naam() + " exists");
            }
            BlockPos hoek = helper.absolutePos(new BlockPos(7, 1, 7));
            Rotation draai = Rotation.CLOCKWISE_90;
            StructurePoolElement element = StructurePoolElement.single("guhs:" + KAMER).apply(StructureTemplatePool.Projection.RIGID);
            BoundingBox box = element.getBoundingBox(level.getStructureManager(), hoek, draai);
            StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, hoek, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
            StructureStart start = new StructureStart(structure, ChunkPos.containing(hoek), 0, new PiecesContainer(List.of(piece)));
            Kopieen.test(level, start);
            OudeScenes.Plek plek = OudeScenes.kopie(level, s, p.blockPosition());
            BlockPos hoort = Kopieen.wereld(start, s.stuk(), s.anker());
            helper.assertTrue(plek != null && plek.anker().equals(hoort) && plek.draai() == draai, "anchored on the copy's own block, turned like the copy: " + plek);
            helper.assertTrue(!hoort.equals(p.blockPosition()), "(not where the player stands)");
            boolean[] daarna = {false};
            helper.assertTrue(OudeScenes.speel(p, s, p.blockPosition(), x -> daarna[0] = true), "the scene plays at the copy");
            CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + s.id());
            helper.assertTrue(BlockPos.of(t.getLongOr("Pos", 0L)).equals(hoort) && t.getIntOr("Draai", -1) == draai.ordinal(), "the engine got the copy's anchor and rotation");
            // a player who is past the moment and comes to a copy: the Guhdex replays it there from then on
            ServerPlayer oud = speler(helper, true);
            GuhQuests.saved(oud).putBoolean(HemelQuest.HART, true);
            helper.assertTrue(OudeScenes.bijwerken(oud) == 1, "past the moment, at a copy");
            CompoundTag o = GuhQuests.saved(oud).getCompoundOrEmpty("guhs_scene_" + s.id());
            helper.assertTrue(o.getBooleanOr("Gezien", false) && BlockPos.of(o.getLongOr("Pos", 0L)).equals(hoort) && o.getIntOr("Draai", -1) == draai.ordinal(),
                    "their replay is anchored on the copy too");
            helper.onEachTick(() -> tik(p));
            helper.runAfterDelay(NA, () -> {
                Kopieen.testWissen(level);
                helper.assertTrue(daarna[0] && !Cutscenes.bezig(p), "watched at the copy");
                weg(helper, p, oud);
                helper.succeed();
            });
        } catch (RuntimeException e) {
            Kopieen.testWissen(level);
            throw e;
        }
    }

    /** The rules of the scripts (DESIGN_VERHALENPAD B). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void oudescenesScripts(GameTestHelper helper) {
        helper.assertTrue(OudeScenes.ALLE.size() == 6, "six older stories, six scenes");
        for (OudeScene s : OudeScenes.ALLE) {
            Cutscene c = s.scene();
            helper.assertTrue(c.id().equals("oudescenes_" + s.naam()) && Cutscene.van(c.id()) == c && OudeScenes.van(c) == s && OudeScenes.van(s.naam()) == s,
                    s.naam() + ": registered under its id");
            helper.assertTrue(c.duur() >= 15 * 20 && c.duur() <= 25 * 20, s.naam() + ": 15 to 25 seconds, not " + c.duur() / 20.0);
            helper.assertTrue(c.lijn() != null && (VerhalenVoortgang.NIEUW.contains(c.lijn()) || VerhalenVoortgang.OUD.contains(c.lijn())),
                    s.naam() + ": the Guhdex page of its own story replays it (" + c.lijn() + ")");
            helper.assertTrue(c.id().equals(c.kaart()), s.naam() + ": its picture-book replay has its own picture");
            helper.assertTrue(OudeScenes.class.getResource("/assets/guhs/textures/gui/verhaal/kaart_" + c.id() + ".png") != null, s.naam() + ": that picture is in the jar");
            for (Cutscene.Schud schud : c.schudden()) {
                helper.assertTrue(schud.kracht() <= 0.6f, s.naam() + ": calm shake only (" + schud.kracht() + " at " + schud.t() + ")");
            }
            helper.assertTrue(c.cameraOp(0) != null && !c.acteurs().isEmpty() && !c.zinnen().isEmpty(), s.naam() + ": a camera, a cast, something said");
            for (Effecten.Stroom stroom : s.effecten().stromen()) {
                helper.assertTrue(stroom.van() >= 0 && stroom.tot() <= c.duur() && stroom.tot() > stroom.van() && stroom.deeltje().get() != null,
                        s.naam() + ": a stream of particles inside the scene");
            }
            for (int flits : s.effecten().flitsen()) {
                helper.assertTrue(flits > 0 && flits < c.duur(), s.naam() + ": lightning inside the scene");
            }
            if (s != OudeScenes.BALTO) {
                helper.assertTrue(!s.effecten().heeft(Effecten.ZICHT) && !s.effecten().heeft(Effecten.SNEEUW), s.naam() + ": no snowstorm outside the tundra");
            }
        }
        // Balto: it snows much harder and you can barely see three blocks; then it clears
        Effecten balto = OudeScenes.BALTO.effecten();
        float dichtst = Float.MAX_VALUE, sneeuw = 0;
        for (int t = 0; t <= OudeScenes.BALTO.scene().duur(); t++) {
            dichtst = Math.min(dichtst, balto.kanaal(t, Effecten.ZICHT));
            sneeuw = Math.max(sneeuw, balto.kanaal(t, Effecten.SNEEUW));
        }
        helper.assertTrue(dichtst >= 3f && dichtst <= 4.5f && sneeuw >= 0.99f, "balto: barely three blocks of sight in the thick of it (" + dichtst + ")");
        helper.assertTrue(balto.kanaal(OudeScenes.BALTO.scene().duur(), Effecten.ZICHT) > 40f, "balto: the storm has cleared at the end");
        helper.assertTrue(OudeScenes.MEWTWO.effecten().heeft(Effecten.REGEN) && OudeScenes.MEWTWO.effecten().heeft(Effecten.DONDER)
                && OudeScenes.MEWTWO.effecten().flitsen().length >= 2 && OudeScenes.MEWTWO.effecten().heeft(Effecten.NACHT), "mewtwo: a thunderstorm at night");
        helper.assertTrue(OudeScenes.OHANA.effecten().heeft(Effecten.NACHT), "ohana: night (the starry sky)");
        // the weather of a scene is the viewer's own: the world's weather is not touched
        ServerLevel level = helper.getLevel();
        float regen = level.getRainLevel(1f), donder = level.getThunderLevel(1f);
        long dag = level.getDefaultClockTime();
        ServerPlayer p = speler(helper, true);
        helper.assertTrue(OudeScenes.speel(p, OudeScenes.MEWTWO, p.blockPosition(), null), "(the thunderstorm scene plays)");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(NA, () -> {
            helper.assertTrue(level.getRainLevel(1f) == regen && level.getThunderLevel(1f) == donder,
                    "the server's weather is what it was: the storm is only in the viewer's game");
            helper.assertTrue(level.getDefaultClockTime() - dag < 200, "and so is the night");
            weg(helper, p);
            helper.succeed();
        });
    }
}
