package nl.juiced.guhs.feature.snuffelsteiger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.snuffel.BewonerEntity;
import nl.juiced.guhs.feature.snuffel.BoompjeEntity;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.MaatjeEntity;
import nl.juiced.guhs.feature.snuffel.Reis;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.taal.NlTekst;
import nl.juiced.guhs.world.BouwRuimte;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * Game tests of the dock of Het Snuffeleiland (run with {@code -Pgt=SnuffelsteigerGameTests}). The game test server has no
 * Guhmensie, so the story tests stand a copy of {@code guhs:steigerhuisje} by hand on the bare floor
 * {@code snuffelsteiger_test_vloer} ({@link Kopieen#test}: only its box and its turn, no blocks) and let the story play
 * everywhere ({@link SteigerVerhaal#OVERAL}); every test that does so has a batch of its own (a test copy is the level's).
 * <ul>
 *   <li>{@link #snuffelsteigerStartPerSpeler}: the story starts for whoever sets foot on a dock, and only for them; the
 *   sickbed, the choice and the captain, each player at their own point.</li>
 *   <li>{@link #snuffelsteigerOverdracht}: the hand-over: the crossing ends with the player on the island's beach, a dog,
 *   the step done, their home exactly the spot on the pier; other players untouched; later crossings land in the
 *   harbour; coming home to the dock plays the boat's scene.</li>
 *   <li>{@link #snuffelsteigerBewoners}: the puppy, the neighbour, the captain and the boat come to a copy, on their spots,
 *   also in a turned copy; the dock is protected.</li>
 *   <li>{@link #snuffelsteigerKust}: in a real Guhmension generator the structure only starts on shores, its pier over
 *   water and its plot on land, and the guaranteed copy is found.</li>
 *   <li>{@link #snuffelsteigerTeksten}: every text, scene, model and sound of the dock is in the jar.</li>
 * </ul>
 */
public class SnuffelsteigerGameTests {
    private static final String VLOER = "snuffelsteiger_test_vloer", EMPTY = "empty";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
    /** The corner of the copy on the test floor: the template's deck (where you stand: G + 1) is the floor's top. */
    private static final BlockPos HOEK = new BlockPos(1, 2 - (Steiger.G + 1), 1);

    private static Verhaallijn lijn() {
        return SnuffelFeature.LIJN;
    }

    /** A copy of the steigerhuisje (its box and its turn; no blocks) with its corner at this helper position. */
    private static StructureStart kopie(GameTestHelper helper, BlockPos hoek, Rotation draai) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, Steiger.STRUCTUUR);
        helper.assertTrue(structure instanceof KustStructure, "the structure guhs:steigerhuisje is a kust_steiger");
        BlockPos pos = helper.absolutePos(hoek);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Steiger.STRUCTUUR).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, draai);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z, 20f, 5f);
        p.setOnGround(true);
        return p;
    }

    private static void zet(ServerPlayer p, Vec3 waar) {
        p.teleportTo(p.level(), waar.x, waar.y, waar.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        p.setOnGround(true);
    }

    /** What the server does for a real player every tick. */
    private static void tick(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    private static SteigerBewoner bewoner(GameTestHelper helper, Steiger.Oord o, String rol, Vec3 template, float yaw) {
        SteigerBewoner e = SnuffelsteigerFeature.STEIGER_BEWONER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        e.zetRol(rol);
        Vec3 w = o.wereld(template);
        e.snapTo(w.x, w.y, w.z, o.yaw(yaw), 0f);
        helper.getLevel().addFreshEntity(e);
        return e;
    }

    private static void ruimOp(GameTestHelper helper, List<ServerPlayer> spelers, Eiland.Plaats eiland) {
        ServerLevel level = helper.getLevel();
        for (ServerPlayer p : spelers) {
            if (Hondvorm.actief(p)) {
                Reis.naarHuis(p);
            }
            SteigerVerhaal.vergeet(p);
            lijn().wis(p);
            CompoundTag saved = GuhQuests.saved(p);
            saved.remove("guhs_snuffel");
            saved.remove("guhs_snuffel_kluis");
            saved.remove("guhs_snuffel_post");
            Praat.vergeet(p);
            Minigames.forget(p);
            if (!p.isRemoved()) {
                level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            }
        }
        for (Entity e : level.getEntities((Entity) null, helper.getBounds().inflate(12),
                x -> x instanceof BewonerEntity || x instanceof SteigerBoot || x instanceof BoompjeEntity || x instanceof MaatjeEntity)) {
            e.discard();
        }
        if (eiland != null) {
            Eiland.testWeg(eiland);
        }
        Kopieen.testWissen(level);
        SteigerVerhaal.OVERAL = false;
    }

    private static boolean dicht(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b) < 1e-4;
    }

    // =====================================================================================================================
    // the start, per player; the sickbed, the choice, the captain
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = "snuffelsteiger_start", timeoutTicks = 200)
    public static void snuffelsteigerStartPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ServerPlayer> spelers = new ArrayList<>();
        SteigerVerhaal.OVERAL = true;
        StructureStart start = kopie(helper, HOEK, Rotation.NONE);
        BlockPos anker = helper.absolutePos(new BlockPos(1 + Steiger.VOET.getX(), 2, 1 + Steiger.VOET.getZ()));
        Steiger.Oord o = Steiger.oord(level, anker);
        helper.assertTrue(o != null && o.anker().equals(anker) && o.draai() == Rotation.NONE, "the copy is found, its anchor is the first plank: " + o);
        helper.assertTrue(Bezetting.start(level, Steiger.STRUCTUUR, anker) == start, "it is this copy");
        Vec3 pier = o.wereld(10.5, 0, 17.5), buiten = o.wereld(-8.5, 0, 5.5), hoog = o.wereld(10.5, 30, 17.5);
        helper.assertTrue(o.op(pier) && o.op(o.wereld(Steiger.BUUR)) && !o.op(buiten) && !o.op(hoog), "on the dock = on its plot or its pier");

        ServerPlayer a = speler(helper, buiten), b = speler(helper, buiten);
        spelers.add(a);
        spelers.add(b);
        helper.assertTrue(!lijn().begonnen(a) && !lijn().begonnen(b), "nobody began");
        helper.assertTrue(!SteigerVerhaal.kijk(a) && !lijn().begonnen(a) && !Cutscenes.bezig(a), "next to the dock is not on it: nothing starts");
        Doel doel = lijn().doel(a);
        helper.assertTrue(doel != null && Steiger.STRUCTUUR.equals(doel.structuur()), "Mijn verhaal points at a steigerhuisje: " + doel);
        boolean inKompas = SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("verhalen") && c.structures().contains(Steiger.STRUCTUUR));
        helper.assertTrue(inKompas, "the steigerhuisje is in the Superkompas, tab Verhalen");

        // A sets foot on the pier: the story begins for A, the feast plays, and only for A
        zet(a, pier);
        a.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(!SteigerVerhaal.kijk(a), "a spectator starts nothing");
        a.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(SteigerVerhaal.kijk(a), "the feast starts for whoever sets foot on the dock");
        helper.assertTrue(Cutscenes.bezig(a) && lijn().begonnen(a) && lijn().stap(a) == SnuffelFeature.STAP_STEIGER, "watching; the step moves at the END of the scene");
        helper.assertTrue(!SteigerVerhaal.kijk(a), "not twice at once");
        helper.assertTrue(!lijn().begonnen(b) && !Cutscenes.bezig(b), "the other player has nothing to do with it");
        helper.runAfterDelay(4, () -> {
            tick(a);
            helper.assertTrue(!Cutscenes.bezig(a) && lijn().stap(a) == SnuffelFeature.STAP_UITVAREN && Cutscenes.gezien(a, SteigerScenes.FEEST.id()),
                    "after the feast A is at the sickbed's step: " + lijn().stap(a));
            helper.assertTrue(lijn().stap(b) == SnuffelFeature.STAP_STEIGER && !lijn().begonnen(b), "B is where B was");
            helper.assertTrue(!SteigerVerhaal.kijk(a), "the feast does not start again for A");

            SteigerBewoner pup = bewoner(helper, o, SteigerBewoner.PUP, Steiger.PUP, Steiger.PUP_YAW);
            SteigerBewoner buur = bewoner(helper, o, SteigerBewoner.BUUR, Steiger.BUUR, Steiger.BUUR_YAW);
            SteigerBewoner kapitein = bewoner(helper, o, SteigerBewoner.KAPITEIN, Steiger.KAPITEIN, Steiger.KAPITEIN_YAW);
            helper.assertTrue(pup.alsSpeler() && pup.pup() && pup.ligt() && !buur.ligt() && "golden".equals(buur.ras()) && "kapitein".equals(kapitein.bewoner()),
                    "the puppy is the viewer's own and lies down, the neighbour is a golden, the captain is Zoutsnoet");
            helper.assertTrue(kapitein.sleutel().startsWith("steiger_") && pup.sleutel().startsWith("steiger_"), "never one of the island's role keys");

            // the captain first sends A to the sickbed; sailing is not possible yet
            Praat.vergeet(a);
            SteigerVerhaal.klik(kapitein, a);
            helper.assertTrue(Praat.lopend(a) == null && !SteigerVerhaal.magKiezen(a) && !SteigerVerhaal.vaarUit(a), "first the sickbed: no choice, no sailing");
            // the sickbed: the neighbour (or the puppy) tells; "Ik moet er even over nadenken" changes nothing
            SteigerVerhaal.klik(pup, a);
            helper.assertTrue(SteigerVerhaal.PRAAT_ZIEKBED.equals(Praat.lopend(a)), "the puppy's click opens the sickbed talk too");
            Praat.antwoord(a, buur, 0);
            helper.assertTrue(!lijn().vlag(a, SteigerVerhaal.ZIEKBED), "thinking it over: nothing yet");
            SteigerVerhaal.klik(buur, a);
            Praat.antwoord(a, buur, 1);
            helper.assertTrue(lijn().vlag(a, SteigerVerhaal.ZIEKBED) && lijn().stap(a) == SnuffelFeature.STAP_UITVAREN, "A decided to go after papa");
            helper.assertTrue(!lijn().vlag(b, SteigerVerhaal.ZIEKBED), "B decided nothing");
            // the captain asks who A is: the kern's choice screen, opened by the server for A only
            SteigerVerhaal.klik(kapitein, a);
            helper.assertTrue(SteigerVerhaal.PRAAT_WIE.equals(Praat.lopend(a)) && !Keuze.magKiezen(a), "the captain asks; the screen is not open yet");
            Praat.antwoord(a, kapitein, 1);
            helper.assertTrue(Keuze.magKiezen(a) && !Keuze.magKiezen(b), "the choice screen is open for A, not for B");
            helper.assertTrue(Keuze.zet(a, new Keuze("corgi", "sable", "Woef", "c")) && Keuze.heeft(a) && !Keuze.heeft(b), "A chose a dog and a companion");
            SteigerVerhaal.klik(kapitein, a);
            helper.assertTrue(SteigerVerhaal.PRAAT_KLAAR.equals(Praat.lopend(a)), "now the captain asks whether A is ready");
            // choosing again is allowed as often as A likes
            Praat.antwoord(a, kapitein, 2);
            helper.assertTrue(Keuze.magKiezen(a), "another dog after all: the screen opens again");
            helper.assertTrue(Keuze.zet(a, new Keuze("teckel", "choco", "Worst", "a")) && "teckel".equals(Keuze.van(a).ras()), "chosen again");

            // B walks up to the neighbour before ever standing on the pier: the story begins for B there and then
            zet(b, o.wereld(Steiger.BUUR).add(1, 0, 0));
            Praat.vergeet(b);
            SteigerVerhaal.klik(buur, b);
            helper.assertTrue(Cutscenes.bezig(b) && lijn().begonnen(b) && Praat.lopend(b) == null, "B's own feast starts at B's own moment");
            helper.assertTrue(lijn().stap(a) == SnuffelFeature.STAP_UITVAREN && lijn().vlag(a, SteigerVerhaal.ZIEKBED), "and A is still where A was");
            ruimOp(helper, spelers, null);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the hand-over to the island, later crossings, coming home
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = "snuffelsteiger_reis", timeoutTicks = 300)
    public static void snuffelsteigerOverdracht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ServerPlayer> spelers = new ArrayList<>();
        SteigerVerhaal.OVERAL = true;
        kopie(helper, HOEK, Rotation.NONE);
        BlockPos anker = helper.absolutePos(new BlockPos(1 + Steiger.VOET.getX(), 2, 1 + Steiger.VOET.getZ()));
        Steiger.Oord o = Steiger.oord(level, anker);
        helper.assertTrue(o != null, "the copy is found");
        // this test's island: 13 x 13 on the far end of the floor's quay (the kern's test shape), outside the dock's talk
        Eiland.Opzet opzet = new Eiland.Opzet(1, BlockPos.ZERO, new Vec3i(9, 5, 9), List.of(), new Eiland.Punt(new Vec3(4.5, 1, 6.5), 180f),
                new Eiland.Punt(new Vec3(6.5, 1, 4.5), 90f), new BlockPos(4, 1, 2), 0, 0, List.of(), List.of(), List.of());
        Eiland.Plaats eiland = Eiland.test(level, helper.absolutePos(new BlockPos(1, 30, 1)), opzet);
        helper.assertTrue(Eiland.plaats(level.getServer()) != null && Eiland.plaats(level.getServer()).level() == level, "this test's island is the server's");

        Vec3 bijKapitein = o.wereld(12.5, 0, 29.5);
        ServerPlayer a = speler(helper, bijKapitein), b = speler(helper, o.wereld(10.5, 0, 20.5));
        spelers.add(a);
        spelers.add(b);
        for (ServerPlayer p : spelers) {
            lijn().begin(p);
            lijn().zet(p, SnuffelFeature.STAP_UITVAREN);
        }
        lijn().vlag(a, SteigerVerhaal.ZIEKBED, true);
        Keuze.zet(a, new Keuze("mops", "abrikoos", "Knor", "b"));
        a.getInventory().setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 5));

        helper.assertTrue(!SteigerVerhaal.vaarUit(b), "B did not hear the sickbed's story: no crossing");
        helper.assertTrue(SteigerVerhaal.vaarUit(a) && Cutscenes.bezig(a) && !Hondvorm.actief(a) && lijn().stap(a) == SnuffelFeature.STAP_UITVAREN,
                "the crossing plays first: A is still at the dock, the step is not done yet");
        helper.assertTrue(!SteigerVerhaal.vaarUit(a), "not twice at once");
        helper.runAfterDelay(4, () -> {
            tick(a);
            // the scene's end: washed ashore on the island's beach, a dog, the step done; the island's own story takes over
            helper.assertTrue(!Cutscenes.bezig(a) && Cutscenes.gezien(a, SteigerScenes.OVERTOCHT.id()), "the crossing was watched");
            helper.assertTrue(Hondvorm.actief(a) && Eiland.in(a) && dicht(a.position(), eiland.wereld(opzet.strand().plek())), "A is a dog on the beach: " + a.position());
            helper.assertTrue(lijn().stap(a) == SnuffelFeature.STAP_STRAND && Reis.bezocht(a), "the dock's step is done: the island's first step is A's now");
            Reis.Thuis thuis = Reis.thuis(a);
            helper.assertTrue(thuis != null && dicht(thuis.plek(), bijKapitein) && thuis.dim() == level.dimension(), "home is exactly the spot on the pier: " + thuis);
            helper.assertTrue(a.getInventory().getItem(3).isEmpty(), "A's own things stay behind, safe (a dog has no pockets)");
            helper.assertTrue(!Hondvorm.actief(b) && lijn().stap(b) == SnuffelFeature.STAP_UITVAREN && dicht(b.position(), o.wereld(10.5, 0, 20.5)), "B is still B, on the pier");
            helper.assertTrue(!SteigerVerhaal.vaarUit(a) && !SteigerVerhaal.kijk(a), "a dog on the island has no business with the dock");

            // home again (the island's captain or the memory card): exactly the spot on the pier, a player with everything
            tick(a);
            helper.assertTrue(Snuffel.naarHuis(a) && !Hondvorm.actief(a) && dicht(a.position(), bijKapitein), "home: the spot on the pier: " + a.position());
            helper.assertTrue(a.getInventory().getItem(3).is(net.minecraft.world.item.Items.COOKED_BEEF) && a.getInventory().getItem(3).getCount() == 5, "with everything A had");
            tick(a);
            helper.assertTrue(!Cutscenes.bezig(a), "the boat's scene waits a moment (the client's world must be back)");
            helper.runAfterDelay(SteigerVerhaal.THUIS_WACHT + 2, () -> {
                tick(a);
                helper.assertTrue(Cutscenes.bezig(a), "coming home to a dock: the boat brings A in");
                helper.runAfterDelay(4, () -> {
                    tick(a);
                    helper.assertTrue(!Cutscenes.bezig(a) && Cutscenes.gezien(a, SteigerScenes.THUISKOMST.id()), "watched");
                    tick(a);
                    helper.assertTrue(!Cutscenes.bezig(a), "once per homecoming");
                    // a later crossing: the short scene, and the boat lands in the island's harbour
                    helper.assertTrue(SteigerVerhaal.vaarUit(a) && Cutscenes.bezig(a), "the captain sails A again");
                    helper.runAfterDelay(4, () -> {
                        tick(a);
                        helper.assertTrue(Hondvorm.actief(a) && dicht(a.position(), eiland.wereld(opzet.haven().plek())) && Cutscenes.gezien(a, SteigerScenes.VAART.id()),
                                "the later crossing lands in the harbour: " + a.position());
                        helper.assertTrue(lijn().stap(a) == SnuffelFeature.STAP_STRAND, "and moves no step");
                        ruimOp(helper, spelers, eiland);
                        helper.succeed();
                    });
                });
            });
        });
    }

    // =====================================================================================================================
    // who lives there
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = "snuffelsteiger_bewoners", timeoutTicks = 300)
    public static void snuffelsteigerBewoners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SteigerVerhaal.OVERAL = true;
        // a copy turned half round: its corner is the far corner of the floor
        BlockPos hoek = new BlockPos(1 + Steiger.SX - 1, 2 - (Steiger.G + 1), 1 + Steiger.SZ - 1);
        StructureStart start = kopie(helper, hoek, Rotation.CLOCKWISE_180);
        BlockPos anker = Kopieen.wereld(start, null, Steiger.VOET);
        Steiger.Oord o = Steiger.oord(level, anker);
        helper.assertTrue(o != null && o.draai() == Rotation.CLOCKWISE_180 && o.anker().equals(anker), "the turned copy is found");
        helper.assertTrue(anker.equals(helper.absolutePos(new BlockPos(1 + Steiger.SX - 1 - Steiger.VOET.getX(), 2, 1 + Steiger.SZ - 1 - Steiger.VOET.getZ()))),
                "its anchor is the template's first plank, turned: " + anker);
        // (a scene position and a resident's spot are the same point of the world, however the copy is turned)
        Vec3 pup = o.wereld(Steiger.PUP);
        Vec3 viaBlok = SnuffelsteigerFeature.precies(Vec3.atBottomCenterOf(Kopieen.wereld(start, null, SnuffelsteigerFeature.blok(Steiger.PUP))), Steiger.PUP,
                Rotation.CLOCKWISE_180);
        helper.assertTrue(dicht(pup, viaBlok), "the puppy's spot, by the scene's maths and by the block's: " + pup + " " + viaBlok);
        helper.assertTrue(dicht(Cutscene.wereld(o.anker(), o.draai(), Steiger.punt(Steiger.BOOT)), o.wereld(Steiger.BOOT)), "the boat's berth");
        helper.assertTrue(Bescherming.beschermd(level, anker) && Bescherming.beschermd(level, BlockPos.containing(pup)), "the dock is protected");

        helper.assertTrue(Bezetting.controleer(level, anker) == 0, "missing once: nothing yet");
        helper.runAfterDelay(Bezetting.BEVESTIG + 5, () -> {
            int gemaakt = Bezetting.controleer(level, anker);
            AABB doos = new AABB(anker).inflate(48);
            List<SteigerBewoner> honden = level.getEntitiesOfClass(SteigerBewoner.class, doos, Entity::isAlive);
            List<SteigerBoot> boten = level.getEntitiesOfClass(SteigerBoot.class, doos, Entity::isAlive);
            helper.assertTrue(gemaakt == 4 && honden.size() == 3 && boten.size() == 1, "missing twice: the three dogs and the boat came: " + gemaakt + " " + honden.size() + " " + boten.size());
            for (SteigerBewoner h : honden) {
                Vec3 hoort = switch (h.rol()) {
                    case SteigerBewoner.PUP -> o.wereld(Steiger.PUP);
                    case SteigerBewoner.BUUR -> o.wereld(Steiger.BUUR);
                    default -> o.wereld(Steiger.KAPITEIN);
                };
                helper.assertTrue(dicht(h.position(), hoort) && h.isInvulnerable(), h.rol() + " stands on its spot and cannot be hurt: " + h.position() + " / " + hoort);
            }
            SteigerBewoner zieke = honden.stream().filter(h -> SteigerBewoner.PUP.equals(h.rol())).findFirst().orElseThrow();
            helper.assertTrue(zieke.isNoGravity() && zieke.ligt() && zieke.alsSpeler(), "the puppy lies on its bed (it does not fall), as the viewer's own puppy");
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(zieke.getYRot() - (Steiger.PUP_YAW + 180f))) < 0.01f, "turned with the copy: " + zieke.getYRot());
            helper.assertTrue(dicht(boten.get(0).position(), o.wereld(Steiger.BOOT)) && boten.get(0).canBeCollidedWith(null) && boten.get(0).isPickable(),
                    "the boat lies at its berth; you can stand on it and click it");
            helper.assertTrue(Bezetting.controleer(level, anker) == 0, "nobody twice");
            // somebody removed the captain: he is back after two looks
            honden.stream().filter(h -> SteigerBewoner.KAPITEIN.equals(h.rol())).forEach(Entity::discard);
            Bezetting.controleer(level, anker);
            helper.runAfterDelay(Bezetting.BEVESTIG + 5, () -> {
                helper.assertTrue(Bezetting.controleer(level, anker) == 1, "the captain is back");
                for (Entity e : level.getEntities((Entity) null, doos, x -> x instanceof SteigerBewoner || x instanceof SteigerBoot)) {
                    e.discard();
                }
                Kopieen.testWissen(level);
                SteigerVerhaal.OVERAL = false;
                helper.succeed();
            });
        });
    }

    // =====================================================================================================================
    // the shore: where a steigerhuisje starts in a real Guhmension
    // =====================================================================================================================

    /**
     * A Guhmension generator built from the dimension's JSON (seed 20261001, as PlaatsingGameTests): every chunk of the
     * normal set within 3000 blocks is asked. A start only comes on a shore (its anchor column is sea, the column behind it
     * land), with its plot on land near the deck's height, its pier and the boat's berth in water, and open sea further
     * out; there are enough of them; and the guaranteed copy is found in its ring and really starts there.
     */
    @GuhTest(template = EMPTY, batch = "snuffelsteiger_kust", timeoutTicks = 12000)
    public static void snuffelsteigerKust(GameTestHelper helper) {
        long seed = 20261001L;
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource biomes;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source")).getOrThrow();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guhmension")));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForNormal(random, seed, biomes, access.lookupOrThrow(Registries.STRUCTURE_SET));
        state.ensureStructuresGenerated();
        LevelHeightAccessor height = LevelHeightAccessor.create(settings.value().noiseSettings().minY(), settings.value().noiseSettings().height());
        BouwRuimte.remember(random, state);
        GegarandeerdPlacement.onthoud(level, state, seed, generator, height);

        Holder<Structure> houder = access.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, Guhs.id(Steiger.STRUCTUUR)));
        KustStructure kust = (KustStructure) houder.value();
        StructureSet set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Steiger.STRUCTUUR));
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement, "the normal set is a random spread");
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
        NormalNoise noise = random.getOrCreateNoise(kust.zee().noise());
        KustStructure.Hoogte hoogte = (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);

        Map<KustStructure.Reden, Integer> telling = new EnumMap<>(KustStructure.Reden.class);
        List<String> fouten = new ArrayList<>();
        int kandidaten = 0, starts = 0, geweken = 0;
        List<BoundingBox> dozen = new ArrayList<>();
        int r = 3000 / 16, s = spread.spacing();
        StringBuilder waar = new StringBuilder();
        for (int rx = Math.floorDiv(-r, s); rx <= Math.floorDiv(r, s); rx++) {
            for (int rz = Math.floorDiv(-r, s); rz <= Math.floorDiv(r, s); rz++) {
                ChunkPos c = spread.getPotentialStructureChunk(seed, rx * s, rz * s);
                if (Math.abs(c.x()) > r || Math.abs(c.z()) > r) {
                    continue;
                }
                kandidaten++;
                KustStructure.Uitkomst u = kust.zoek(noise, c, hoogte);
                telling.merge(u.reden(), 1, Integer::sum);
                if (u.plek() == null) {
                    continue;
                }
                // what the chunk generator would do with this chunk
                StructureStart start = kust.generate(houder, level.dimension(), access, generator, biomes, random, level.getStructureManager(), seed, c, 0, height,
                        kust.biomes()::contains);
                if (!start.isValid()) {
                    geweken++;      // (a story place too near 0,0, or another guhs building that goes first stands there: BouwRuimte)
                    continue;
                }
                starts++;
                dozen.add(start.getBoundingBox());
                KustStructure.Plek p = u.plek();
                BlockPos a = p.anker();
                int dx = p.zee().getStepX(), dz = p.zee().getStepZ();
                int hier = hoogte.van(a.getX(), a.getZ()), achter = hoogte.van(a.getX() - dx, a.getZ() - dz), eind = hoogte.van(a.getX() + dx * 17, a.getZ() + dz * 17);
                if (waar.length() < 600) {
                    waar.append(' ').append(a.toShortString()).append(' ').append(p.zee().getName()).append(" land ").append(achter - 1).append(" bodem ").append(eind - 1).append(';');
                }
                if (a.getY() != Steiger.DEK_Y || hier > kust.zee().waterY() || achter <= kust.zee().waterY()) {
                    fouten.add("the anchor at " + a + " is not the water's edge: " + hier + " / " + achter);
                }
                if (eind > kust.zee().waterY() - 3) {
                    fouten.add("the end of the pier at " + a + " stands in less than 4 blocks of water: " + eind);
                }
                // the piece: one template, its anchor block on the spot, the pier pointing out to sea
                StructurePiece stuk = start.getPieces().get(0);
                BlockPos voet = Kopieen.wereld(start, null, Steiger.ANKER);
                if (start.getPieces().size() != 1 || !a.equals(voet) || Kopieen.draai(start, null) != KustStructure.draaiNaar(p.zee())) {
                    fouten.add("the piece at " + a + " is not placed on its anchor: " + voet + " " + Kopieen.draai(start, null));
                }
                BlockPos pierEind = Kopieen.wereld(start, null, new BlockPos(Steiger.ANKER.getX(), Steiger.G, Steiger.SZ - 2));
                if (pierEind == null || !pierEind.equals(a.offset(dx * (Steiger.SZ - 2 - Steiger.ANKER.getZ()), 0, dz * (Steiger.SZ - 2 - Steiger.ANKER.getZ())))) {
                    fouten.add("the pier at " + a + " does not point out to sea: " + pierEind);
                }
                if (Math.hypot(stuk.getBoundingBox().getCenter().getX() - c.getMiddleBlockX(), stuk.getBoundingBox().getCenter().getZ() - c.getMiddleBlockZ()) > kust.keepClear()) {
                    fouten.add("the piece at " + a + " lies further from its start chunk than keep_clear");
                }
            }
        }
        // the guaranteed copy
        StructureSet zeker = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Steiger.STRUCTUUR + "_gegarandeerd"));
        helper.assertTrue(zeker != null && zeker.placement() instanceof GegarandeerdPlacement g && g.alleenNieuw(), "the guaranteed set is alleen_nieuw");
        GegarandeerdPlacement g = (GegarandeerdPlacement) zeker.placement();
        Optional<ChunkPos> plek = g.plek(state, seed);
        String zekerWaar = "none";
        if (plek.isEmpty()) {
            fouten.add("the guaranteed copy has no spot");
        } else {
            ChunkPos c = plek.get();
            int d = (int) Math.round(Math.hypot(c.getMinBlockX(), c.getMinBlockZ()));
            zekerWaar = c + " (" + d + " blocks)";
            if (d < g.minAfstand() || d > g.maxAfstand()) {
                fouten.add("the guaranteed copy lies " + d + " blocks from 0,0");
            }
            StructureStart start = kust.generate(houder, level.dimension(), access, generator, biomes, random, level.getStructureManager(), seed, c, 0, height,
                    kust.biomes()::contains);
            if (!start.isValid()) {
                fouten.add("the guaranteed copy does not start at " + c);
            } else if (!dozen.contains(start.getBoundingBox())) {
                dozen.add(start.getBoundingBox());
            }
        }
        // two docks never stand side by side (the world check of 1.4.0: on one fitting shore they stood in a row, yards 10
        // blocks apart, the guaranteed copy in the middle): eigen_afstand blocks between any two, the guaranteed one included
        int eigen = kust.eigenAfstand(), dichtst = Integer.MAX_VALUE;
        for (int a = 0; a < dozen.size(); a++) {
            for (int b = a + 1; b < dozen.size(); b++) {
                BoundingBox een = dozen.get(a), twee = dozen.get(b);
                int tussen = Math.max(Math.max(een.minX(), twee.minX()) - Math.min(een.maxX(), twee.maxX()),
                        Math.max(een.minZ(), twee.minZ()) - Math.min(een.maxZ(), twee.maxZ()));
                dichtst = Math.min(dichtst, tussen);
                if (een.inflatedBy(eigen).intersects(twee)) {
                    fouten.add("two docks stand " + tussen + " blocks apart: " + een + " and " + twee);
                }
            }
        }
        LOGGER.info("Snuffelsteiger kust: {} candidates within 3000 blocks, outcomes {}, {} starts ({} gave way), guaranteed {}, the two nearest docks {} "
                + "blocks apart; starts:{}; problems: {}", kandidaten, telling, starts, geweken, zekerWaar, dichtst, waar, fouten);
        helper.assertTrue(fouten.isEmpty(), "every start stands right on a shore: " + fouten);
        helper.assertTrue(starts >= 16, "enough docks along the shores within 3000 blocks: " + starts + " " + telling);
        helper.assertTrue(eigen >= 64, "docks keep their distance: " + eigen);
        // (a good spot gives way to a story place within 600 blocks of 0,0, to another building, or to a dock next door)
        helper.assertTrue(geweken <= starts, "at least half of the good spots really get their dock: " + geweken + " gave way, " + starts + " start");
        helper.assertTrue(houder.is(BouwRuimte.VERHAAL), "a story place: never within 600 blocks of 0,0");
        helper.assertTrue(kust.keepClear() > 0, "it takes part in BouwRuimte: " + kust.keepClear());
        helper.succeed();
    }

    // =====================================================================================================================
    // everything it names is in the jar
    // =====================================================================================================================

    private static boolean bestaat(String pad) {
        try (InputStream in = SnuffelsteigerGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static JsonObject lees(String pad) throws Exception {
        try (InputStream in = SnuffelsteigerGameTests.class.getResourceAsStream(pad)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @GuhTest(template = EMPTY, batch = "snuffelsteiger")
    public static void snuffelsteigerTeksten(GameTestHelper helper) throws Exception {
        List<String> mist = new ArrayList<>();
        for (String key : List.of("structure.guhs.steigerhuisje", "structure.guhs.steigerhuisje.tooltip", "entity.guhs.steiger_bewoner", "entity.guhs.steiger_bewoner.pup",
                "entity.guhs.steiger_bewoner.buur", "entity.guhs.steiger_boot", "quest.guhs.snuffelsteiger.feest_zo", "quest.guhs.snuffelsteiger.hint.ziekbed",
                "quest.guhs.snuffelsteiger.hint.kapitein", "quest.guhs.snuffelsteiger.buur.dapper", "quest.guhs.snuffelsteiger.buur.ga", "quest.guhs.snuffelsteiger.buur.terug",
                "quest.guhs.snuffelsteiger.buur.spoor", "quest.guhs.snuffelsteiger.pup.ga", "quest.guhs.snuffelsteiger.pup.slaapt", "quest.guhs.snuffelsteiger.pup.spoor",
                "quest.guhs.snuffelsteiger.kapitein.eerst_ziekbed", "quest.guhs.snuffelsteiger.kapitein.wie", "quest.guhs.snuffelsteiger.kapitein.gekozen",
                "quest.guhs.snuffelsteiger.kapitein.klaar", "quest.guhs.snuffelsteiger.kapitein.weer", "gui.guhs.snuffelsteiger.optie.achterna",
                "gui.guhs.snuffelsteiger.optie.nadenken", "gui.guhs.snuffelsteiger.optie.kies", "gui.guhs.snuffelsteiger.optie.nog_niet", "gui.guhs.snuffelsteiger.optie.uitvaren",
                "gui.guhs.snuffelsteiger.optie.andere_hond", "gui.guhs.snuffelsteiger.optie.varen", "gui.guhs.snuffelsteiger.optie.blijven")) {
            if (!NlTekst.has(key)) {
                mist.add(key);
            }
        }
        for (int i = 1; i <= 6; i++) {
            if (!NlTekst.has("quest.guhs.snuffelsteiger.ziekbed." + i)) {
                mist.add("ziekbed." + i);
            }
        }
        // the four scenes: registered, as long as the design says, a title and every line they speak
        Map<Cutscene, Integer> scenes = Map.of(SteigerScenes.FEEST, 400, SteigerScenes.OVERTOCHT, 500, SteigerScenes.VAART, SteigerScenes.VAART_DUUR,
                SteigerScenes.THUISKOMST, SteigerScenes.THUISKOMST_DUUR);
        for (Map.Entry<Cutscene, Integer> e : scenes.entrySet()) {
            Cutscene s = e.getKey();
            helper.assertTrue(Cutscene.van(s.id()) == s && s.duur() == e.getValue(), s.id() + " is registered and " + e.getValue() + " ticks long");
            if (!NlTekst.has(s.titelKey())) {
                mist.add(s.titelKey());
            }
            for (Cutscene.Zeg z : s.zinnen()) {
                if (!NlTekst.has(s.tekstKey(z.key()))) {
                    mist.add(s.tekstKey(z.key()));
                }
                if (!z.spreker().isEmpty() && !Cutscene.SPELER.equals(z.spreker()) && !NlTekst.has(s.tekstKey("naam." + z.spreker()))) {
                    mist.add(s.tekstKey("naam." + z.spreker()));
                }
            }
            // calm camera shake, sound effects only
            for (Cutscene.Schud sch : s.schudden()) {
                helper.assertTrue(sch.kracht() <= 0.6f, s.id() + ": calm shake, not " + sch.kracht());
            }
            for (Cutscene.Geluid gl : s.geluiden()) {
                String id = gl.geluid().get().location().toString();
                helper.assertTrue(!id.contains("music") && !id.contains("record"), s.id() + ": no music: " + id);
            }
        }
        helper.assertTrue("snuffeleiland".equals(SteigerScenes.FEEST.lijn()) && "snuffeleiland".equals(SteigerScenes.OVERTOCHT.lijn()),
                "the two story scenes can be watched again from the Guhdex");
        helper.assertTrue(SteigerScenes.OVERTOCHT.zwartOp(SteigerScenes.OVERTOCHT_DUUR - 1) >= 0.99f && SteigerScenes.FEEST.zwartOp(SteigerScenes.FEEST_DUUR - 1) >= 0.99f,
                "the crossing (and the feast) end in black: the island's scene starts in black");
        helper.assertTrue(SteigerScenes.THUISKOMST.zwartOp(0) >= 0.99f, "coming home starts in black");
        // the boat and the puppies' lying down
        for (String pad : List.of("/assets/guhs/geckolib/models/entity/steiger_boot.geo.json", "/assets/guhs/geckolib/animations/entity/steiger_boot.animation.json",
                "/assets/guhs/textures/entity/steiger_boot.png", "/data/guhs/structure/steigerhuisje.nbt")) {
            if (!bestaat(pad)) {
                mist.add(pad);
            }
        }
        JsonObject boot = lees("/assets/guhs/geckolib/animations/entity/steiger_boot.animation.json").getAsJsonObject("animations");
        helper.assertTrue(boot.has("dobber") && boot.has(SteigerBoot.STORM), "the boat's two animations");
        for (String ras : List.of("shiba", "jackrussell", "teckel", "corgi", "golden", "mops")) {
            JsonObject anims = lees("/assets/guhs/geckolib/animations/entity/snuffelhond_" + ras + "_pup.animation.json").getAsJsonObject("animations");
            if (!anims.has(SteigerBewoner.LIG)) {
                mist.add("lig for the " + ras + " puppy");
            }
            for (String a : nl.juiced.guhs.feature.snuffel.SnuffelHond.ANIMATIES) {
                if (!anims.has(a)) {
                    mist.add(a + " for the " + ras + " puppy (the kern's own)");
                }
            }
        }
        JsonObject geluiden = lees("/assets/guhs/sounds.json");
        for (String g : List.of("piep", "plof", "donder", "bel", "plank", "riem", "golf")) {
            if (!geluiden.has("snuffelsteiger." + g) || !NlTekst.has("subtitles.guhs.snuffelsteiger." + g)) {
                mist.add("sound " + g);
            }
        }
        helper.assertTrue(mist.isEmpty(), "everything the dock names is in the jar: " + mist);
        helper.succeed();
    }
}
