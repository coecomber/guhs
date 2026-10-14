package nl.juiced.guhs.feature.park.plek;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.park.ParkStructure;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.4.2 park-plekjes b: the game tests of the five plekjes.
 * <ul>
 *   <li>{@link #parkbSchikking} (batch park_plekb_wereld): park layouts on real Guhmensie land of a few seeds: still complete,
 *       each once-per-park plekje at most once, at most one uitkijktoren, and the tower on the highest spot of every plekje
 *       it could stand on; plus layouts on the flat test land (the tower in about 3 of 4 parks);</li>
 *   <li>{@link #parkbToren}: the tower template built in the world and walked from its entrance up to both verrekijkers; the
 *       verrekijker grants its advancement;</li>
 *   <li>{@link #parkbDuivenOma}: Oma-guh's handful once a day, strewn crumbs bring the Duifguhtjes, feeding one, its page;</li>
 *   <li>{@link #parkbPoppenkast}: the show opens the curtains, closes them again, the end grants the advancement;</li>
 *   <li>{@link #parkbWensboom}: a wish hangs on a sign (safe text, wrapped), the per-player limit, an unnamed paper does
 *       nothing, taking your wishes down;</li>
 *   <li>{@link #parkbMika}: he grumbles, shoves without damage, takes a kaasknabbel, leaves, and is away for the day only.</li>
 * </ul>
 */
public class PlekBGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String KAMER = "parkb_test_kamer";
    private static final String BATCH = "park_plekb";

    private static ParkStructure park(GameTestHelper helper) {
        Structure s = Kopieen.structuur(helper.getLevel(), ParkFeature.STRUCTUUR);
        helper.assertTrue(s instanceof ParkStructure, "guhs:knabbelpark is a ParkStructure");
        return (ParkStructure) s;
    }

    private static boolean heeft(ServerPlayer p, String naam) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    // =================================================================================================================
    /** What a layout says about the plekjes of this slice: problems, and whether it has a tower. */
    private static boolean keurSchikking(ServerLevel level, PiecesContainer pieces, ParkStructure park, String waar, List<String> fouten) {
        var mgr = level.getStructureManager();
        Map<Identifier, Integer> n = ParkStructure.tel(pieces);
        if (!park.compleet(pieces)) {
            fouten.add(waar + ": not complete");
        }
        for (String u : Uitkijktoren.UNIEK) {
            if (n.getOrDefault(Guhs.id(u), 0) > 1) {
                fouten.add(waar + ": " + u + " x" + n.get(Guhs.id(u)));
            }
        }
        int torens = n.getOrDefault(Guhs.id(Uitkijktoren.TOREN), 0);
        if (torens > 1) {
            fouten.add(waar + ": " + torens + " towers");
        }
        int toren = Integer.MIN_VALUE, hoogste = Integer.MIN_VALUE;
        for (StructurePiece piece : pieces.pieces()) {
            Identifier id = PleinSlot.template(piece);
            if (id == null || !Uitkijktoren.plekje(piece, mgr) || !Uitkijktoren.past(piece, mgr)) {
                continue;
            }
            hoogste = Math.max(hoogste, piece.getBoundingBox().minY());
            if (id.getPath().equals(Uitkijktoren.TOREN)) {
                toren = piece.getBoundingBox().minY();
            }
        }
        if (torens == 1 && toren < hoogste) {
            fouten.add(waar + ": the tower stands at " + toren + ", a plekje spot at " + hoogste);
        }
        return torens == 1;
    }

    @GuhTest(template = "empty", batch = "park_plekb_wereld", timeoutTicks = 30000)
    public static void parkbSchikking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        ParkStructure park = park(helper);
        List<String> fouten = new ArrayList<>();
        // real Guhmensie land (hills): the tower on the highest spot
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource biomes;
        try (var reader = level.getServer().getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            biomes = BiomeSource.CODEC.parse(ops, JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator")
                    .getAsJsonObject("biome_source")).getOrThrow();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        Holder<NoiseGeneratorSettings> settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("guhmension")));
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
        LevelHeightAccessor height = LevelHeightAccessor.create(settings.value().noiseSettings().minY(), settings.value().noiseSettings().height());
        int parken = 0, torens = 0;
        for (long seed : new long[]{20261014L, 42L, 1420L}) {
            RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
            int hier = 0;
            for (int i = -40; i <= 40 && hier < 3; i += 8) {
                for (int j = -40; j <= 40 && hier < 3; j += 8) {
                    ChunkPos chunk = new ChunkPos(i, j);
                    var biome = biomes.getNoiseBiome(chunk.getMiddleBlockX() >> 2, 16, chunk.getMiddleBlockZ() >> 2, random.sampler());
                    if (!park.biomes().contains(biome) || !park.droogEnGlooiend(generator, random, height, chunk, 1)) {
                        continue;
                    }
                    WorldgenRandom wr = new WorldgenRandom(new LegacyRandomSource(0L));
                    wr.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
                    Structure.GenerationContext ctx = new Structure.GenerationContext(access, generator, biomes, random, level.getStructureManager(),
                            wr, seed, chunk, height, b -> true);
                    Optional<ParkStructure.Layout> layout = park.layout(ctx);
                    if (layout.isEmpty()) {
                        fouten.add("seed " + seed + " " + chunk + ": no layout");
                        continue;
                    }
                    hier++;
                    parken++;
                    torens += keurSchikking(level, layout.get().stub().getPiecesBuilder().build(), park, "seed " + seed + " " + chunk, fouten) ? 1 : 0;
                }
            }
        }
        // flat test land: the same rules, and the tower in about 3 of 4 parks
        var gen = level.getChunkSource().getGenerator();
        int vlak = 0, vlakTorens = 0;
        for (int k = 0; k < 8; k++) {
            ChunkPos chunk = new ChunkPos(2100 + 20 * k, 60);
            WorldgenRandom wr = new WorldgenRandom(new LegacyRandomSource(0L));
            wr.setLargeFeatureSeed(77L + k, chunk.x(), chunk.z());
            Structure.GenerationContext ctx = new Structure.GenerationContext(access, gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getStructureManager(), wr, 77L + k, chunk, level, b -> true);
            Optional<ParkStructure.Layout> layout = park.layout(ctx);
            if (layout.isPresent()) {
                vlak++;
                vlakTorens += keurSchikking(level, layout.get().stub().getPiecesBuilder().build(), park, "flat " + chunk, fouten) ? 1 : 0;
            }
        }
        String uit = "plekjes b: " + parken + " parks on hills (" + torens + " with a tower), " + vlak + " flat (" + vlakTorens + " with a tower)";
        LOGGER.info(uit);
        helper.assertTrue(fouten.isEmpty(), "the plekjes of every park right: " + fouten + " / " + uit);
        helper.assertTrue(parken >= 4 && vlak >= 6 && torens + vlakTorens >= 4, "enough parks and towers to look at: " + uit);
        helper.succeed();
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void parkbToren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate t = level.getStructureManager().getOrCreate(Guhs.id(Uitkijktoren.TOREN));
        helper.assertTrue(t.getSize().getX() == Uitkijktoren.TOREN_MAAT && t.getSize().getZ() == Uitkijktoren.TOREN_MAAT, "the tower's footprint");
        BlockPos hoek = new BlockPos(2200 * 16, level.getSeaLevel() + 20, 70 * 16);
        for (int cx = (hoek.getX() >> 4) - 1; cx <= (hoek.getX() >> 4) + 1; cx++) {
            for (int cz = (hoek.getZ() >> 4) - 1; cz <= (hoek.getZ() >> 4) + 1; cz++) {
                level.getChunk(cx, cz);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(hoek.offset(-2, -1, -2), hoek.offset(10, -1, 12))) {
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(hoek.offset(-2, 0, -2), hoek.offset(10, 22, 12))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        t.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        BlockPos oost = hoek.offset(PlekB.KIJKER_OOST), west = hoek.offset(PlekB.KIJKER_WEST);
        helper.assertTrue(level.getBlockState(oost).is(PlekB.VERREKIJKER.get()) && level.getBlockState(west).is(PlekB.VERREKIJKER.get()), "two verrekijkers");
        // walk from the entrance (south edge, middle) up the stairs
        Set<BlockPos> bereikt = loop(level, hoek.offset(Uitkijktoren.TOREN_MAAT / 2, 2, Uitkijktoren.TOREN_MAAT - 2),
                new BoundingBox(hoek.getX() - 1, hoek.getY(), hoek.getZ() - 1, hoek.getX() + 9, hoek.getY() + 20, hoek.getZ() + 9));
        int hoogst = bereikt.stream().mapToInt(BlockPos::getY).max().orElse(-1);
        for (BlockPos k : List.of(oost, west)) {
            helper.assertTrue(bereikt.stream().anyMatch(q -> q.getY() == k.getY() && q.distManhattan(k) <= 1),
                    "verrekijker at " + k.toShortString() + " reached on foot (" + bereikt.size() + " spots, highest y " + hoogst + ")");
        }
        // looking through it
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 7));
        try {
            p.snapTo(oost.getX() - 0.5, oost.getY(), oost.getZ() + 0.5);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(oost), Direction.UP, oost, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
            helper.assertTrue(level.getBlockState(oost).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit)
                    == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "looking works with anything in your hand");
            level.getBlockState(oost).useWithoutItem(level, p, hit);
            helper.assertTrue(heeft(p, "parkb_verrekijker"), "the verrekijker's advancement");
            helper.succeed();
        } finally {
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Every spot reached on foot from {@code van} inside {@code doos} (a floor, two free blocks; up 1 with room, down at most 3). */
    private static Set<BlockPos> loop(ServerLevel level, BlockPos van, BoundingBox doos) {
        Set<BlockPos> gezien = new HashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        if (!staan(level, van)) {
            return gezien;
        }
        rij.add(van);
        gezien.add(van);
        while (!rij.isEmpty()) {
            BlockPos q0 = rij.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = q0.relative(d);
                for (int dy : new int[]{0, 1, -1, -2, -3}) {
                    BlockPos q = n.above(dy);
                    if (!doos.isInside(q) || gezien.contains(q)) {
                        continue;
                    }
                    if (dy == 1 && !vrij(level, q0.above(2))) {
                        continue;
                    }
                    if (staan(level, q)) {
                        gezien.add(q);
                        rij.add(q);
                        break;
                    }
                    if (dy < 0 && !vrij(level, n.above(dy + 1))) {
                        break;
                    }
                }
            }
        }
        return gezien;
    }

    private static boolean vrij(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    private static boolean staan(ServerLevel level, BlockPos p) {
        BlockPos onder = p.below();
        var vorm = level.getBlockState(onder).getCollisionShape(level, onder);
        return !vorm.isEmpty() && vorm.max(Direction.Axis.Y) <= 1.0 && vrij(level, p) && vrij(level, p.above());
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void parkbDuivenOma(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 7));
        GuhNpcEntity oma = npc(helper, GuhNpcEntity.Kind.OMAGUH, new BlockPos(3, 2, 3));
        DuifguhtjeEntity duif = PlekB.DUIFGUHTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos duifPlek = helper.absolutePos(new BlockPos(11, 2, 11));
        duif.snapTo(duifPlek.getX() + 0.5, duifPlek.getY(), duifPlek.getZ() + 0.5, 0, 0);
        level.addFreshEntity(duif);
        DuivenOma.vergeetVoer();
        try {
            NpcRole rol = NpcRollen.rol(GuhNpcEntity.Kind.OMAGUH);
            helper.assertTrue(rol != null && NpcRollen.rol(GuhNpcEntity.Kind.GUHKINDJE) != null && NpcRollen.rol(GuhNpcEntity.Kind.MOKMIKA) != null,
                    "the three NPC kinds have a role");
            GuhQuests.saved(p).remove(DuivenOma.DAG);
            rol.talk(oma, p);
            helper.assertTrue(p.getInventory().countItem(PlekB.KRUIMELS.get()) == DuivenOma.HANDJE, "a handful of crumbs");
            // strew them: one is used, the crumbs lie there, the Duifguhtje wants to land round them
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PlekB.KRUIMELS.get(), DuivenOma.HANDJE));
            p.getMainHandItem().use(level, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(p.getMainHandItem().getCount() == DuivenOma.HANDJE - 1, "one handful strewn");
            helper.assertTrue(heeft(p, "parkb_kruimels"), "the strewing advancement");
            BlockPos voer = DuivenOma.voerplek(level, p.blockPosition(), 4);
            helper.assertTrue(voer != null, "the crumbs lie there");
            helper.assertTrue(DuivenOma.voerplek(level, duif.blockPosition(), 20) != null && duif.vliegt(), "the Duifguhtje comes for them");
            // the same day: no new handful (the crumbs in hand are fewer than a handful, still none)
            p.getInventory().clearContent();
            rol.talk(oma, p);
            helper.assertTrue(p.getInventory().countItem(PlekB.KRUIMELS.get()) == 0, "once a day");
            // fed from the hand
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));
            helper.assertTrue(duif.lekker(p.getMainHandItem()) && !duif.lekker(new ItemStack(Items.DIRT)), "it likes bread, not dirt");
            duif.interact(p, InteractionHand.MAIN_HAND, duif.position());
            helper.assertTrue(p.getMainHandItem().getCount() == 1 && heeft(p, "parkb_duif_gevoerd"), "fed one bread");
            helper.assertTrue(duif.voerder() == p, "holding food: it flutters round you");
            // the Guhdex page: a bonus creature page
            helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.DUIFGUHTJE) && GuhDex.EXTRA.contains(GuhVariant.DUIFGUHTJE)
                    && !GuhDex.TELLEND.contains(GuhVariant.DUIFGUHTJE) && GuhVariant.DUIFGUHTJE.isCharacter(), "the Duifguhtje's page");
            helper.succeed();
        } finally {
            oma.discard();
            duif.discard();
            DuivenOma.vergeetVoer();
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void parkbPoppenkast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 9));
        BlockPos podium = helper.absolutePos(new BlockPos(7, 3, 5));
        level.setBlockAndUpdate(podium, PlekB.POPPENKAST.get().defaultBlockState());
        GuhNpcEntity kind = npc(helper, GuhNpcEntity.Kind.GUHKINDJE, new BlockPos(7, 2, 8));
        Poppenkast.vergeet();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(podium), Direction.SOUTH, podium, false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(level.getBlockState(podium).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit)
                == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND, "the show plays whatever you hold");
        level.getBlockState(podium).useWithoutItem(level, p, hit);
        helper.assertTrue(level.getBlockState(podium).getValue(Poppenkast.OPEN) && Poppenkast.speelt(level, podium), "the curtains open");
        helper.assertTrue(Poppenkast.WIE.length == Poppenkast.SHOW, "a speaker for every page");
        NpcRollen.rol(GuhNpcEntity.Kind.GUHKINDJE).talk(kind, p);
        // the end of the show in the talking screen
        Praat.doeAlsOf(p, Poppenkast.SLEUTEL, null);
        Praat.antwoord(p, null, -1);
        helper.assertTrue(heeft(p, "parkb_poppenkast"), "the show's advancement");
        long begin = level.getGameTime();
        helper.onEachTick(() -> {
            if (!level.getBlockState(podium).getValue(Poppenkast.OPEN) && !Poppenkast.speelt(level, podium)) {
                helper.assertTrue(level.getGameTime() - begin >= Poppenkast.DUUR - Poppenkast.TIK * 2, "it played for a while");
                kind.discard();
                level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                helper.succeed();
            }
        });
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void parkbWensboom(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 7));
        // a little tree: a trunk of three, a branch of three to the east (wishes hang under it)
        BlockPos stam = helper.absolutePos(new BlockPos(5, 2, 7));
        for (int y = 0; y < 4; y++) {
            level.setBlockAndUpdate(stam.above(y), PlekB.WENSSTAM.get().defaultBlockState());
        }
        for (int x = 1; x <= 3; x++) {
            level.setBlockAndUpdate(stam.offset(x, 3, 0), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        try {
            BlockPos boom = Wensboom.boom(level, stam.above(2));
            helper.assertTrue(boom.equals(stam), "the tree is its lowest trunk block");
            helper.assertTrue(Wensboom.plekjes(level, boom).size() == 3, "three spots under the branch: " + Wensboom.plekjes(level, boom));
            // safe text: no formatting codes or direction marks, spaces folded, at most 40 characters, wrapped over four lines
            String schoon = Wensboom.schoon("  Ik §cwens‮   veel\u0007 knabbels voor iedereen in het hele park, njeg njeg njeg");
            helper.assertTrue(schoon.startsWith("Ik wens veel knabbels voor") && schoon.codePointCount(0, schoon.length()) <= Wensboom.MAX_TEKENS
                    && !schoon.contains("§") && !schoon.contains("‮"), "clean: " + schoon);
            List<String> regels = Wensboom.regels(schoon);
            helper.assertTrue(regels.size() <= 4 && regels.stream().allMatch(r -> r.codePointCount(0, r.length()) <= Wensboom.REGEL), "wrapped: " + regels);
            helper.assertTrue(Wensboom.regels("Supercalifragilistisch").get(0).length() == Wensboom.REGEL, "a long word is cut");
            // an unnamed paper: nothing
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stam), Direction.WEST, stam, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 4));
            level.getBlockState(stam).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(p.getMainHandItem().getCount() == 4 && Wensboom.tel(level, stam) == 0, "an unnamed paper is no wish");
            // three wishes of the same player: two hang (the third took the place of the first)
            for (String wens : List.of("Meer knabbels!", "Een hertje", "Zon in het park")) {
                ItemStack papier = new ItemStack(Items.PAPER);
                papier.set(DataComponents.CUSTOM_NAME, Component.literal(wens));
                p.setItemInHand(InteractionHand.MAIN_HAND, papier);
                level.getBlockState(stam).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(p.getMainHandItem().isEmpty(), "the paper is used: " + wens);
            }
            List<String> hangen = new ArrayList<>();
            for (BlockPos q : Wensboom.plekjes(level, boom)) {
                if (level.getBlockState(q).getBlock() instanceof CeilingHangingSignBlock) {
                    hangen.add(Wensboom.bordTekst(level, q));
                }
            }
            helper.assertTrue(hangen.size() == Wensboom.PER_SPELER && hangen.contains("Een hertje") && hangen.contains("Zon in het park"),
                    "two wishes per player, the newest: " + hangen);
            helper.assertTrue(heeft(p, "parkb_wens"), "the wish's advancement");
            helper.assertTrue(Wensboom.Wensen.get(level).van(boom, level).size() == 2, "saved");
            // sneak + empty hand: your wishes come down
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setShiftKeyDown(true);
            int weg = Wensboom.haalWeg(level, stam, p);
            p.setShiftKeyDown(false);
            helper.assertTrue(weg == 2 && Wensboom.tel(level, stam) == 0, "taken down: " + weg);
            helper.succeed();
        } finally {
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    // =================================================================================================================
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void parkbMika(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 10));
        GuhNpcEntity mika = npc(helper, GuhNpcEntity.Kind.MOKMIKA, new BlockPos(7, 2, 6));
        NpcRole rol = NpcRollen.rol(GuhNpcEntity.Kind.MOKMIKA);
        float leven = p.getHealth();
        // grumbling, and a shove (never damage)
        rol.talk(mika, p);
        helper.assertTrue(!mika.roleData.contains(MikaBankje.WEG_OM), "no knabbel: he stays");
        MikaBankje.duw(mika, p);
        helper.assertTrue(p.getHealth() == leven && p.getDeltaMovement().horizontalDistance() > 0.1, "a shove, no damage");
        // a kaasknabbel: he is happy and leaves
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        rol.talk(mika, p);
        helper.assertTrue(p.getMainHandItem().getCount() == 2 && mika.roleData.contains(MikaBankje.WEG_OM) && heeft(p, "parkb_mika_blij"), "he takes one");
        String sleutel = MikaBankje.sleutel(level, mika);
        long dag = DuivenOma.dag(level);
        MikaBankje.Weg weg = MikaBankje.Weg.get(level);
        helper.assertTrue(weg.weg(sleutel, dag) && !weg.weg(sleutel, dag + 1), "away today, back tomorrow");
        rol.talk(mika, p);
        helper.assertTrue(p.getMainHandItem().getCount() == 2, "leaving: no second knabbel");
        helper.onEachTick(() -> {
            if (mika.isRemoved()) {
                weg.zet(sleutel, dag - 1);
                helper.assertTrue(!weg.weg(sleutel, dag), "the next day he may come back");
                level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                helper.succeed();
            }
        });
    }
}
