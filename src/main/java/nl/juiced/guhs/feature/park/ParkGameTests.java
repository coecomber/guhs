package nl.juiced.guhs.feature.park;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.Reisguh;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.4.2 (Het Knabbelpark): the park's game tests.
 * <ul>
 *   <li>{@link #parkVeleZaden}: real Guhmensie generators of eight seeds: at every dry, gentle spot of a grid the park's
 *       layout (what generation does, without the room checks) has all required pieces exactly once, every piece is
 *       joined to the plaza, nothing reaches past the jigsaw limit; and how many steps of 2+ the walks would have on the
 *       raw land (ParkPaden smooths those);</li>
 *   <li>{@link #parkGebouwd}: three whole parks really built in the test world (far away, flat land) and walked: from the
 *       Reisguh you reach the parkwachter, the kooi-verzorger, the three plaques, the kooi's fence and every piece; nothing
 *       floats;</li>
 *   <li>{@link #parkQuestlijn}: the questline from the first talk to the reward (twice: once per player), the Hertguhtjes
 *       of the kooi are fed and never tamed;</li>
 *   <li>{@link #parkBlokken}: the litter bin, the plaque, the own statue (its look on the item and back), the fixed
 *       Reisguh, the Guhdex page.</li>
 * </ul>
 */
public class ParkGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String KAMER = "park_test_kamer";
    private static final long[] ZADEN = {20261001L, 1L, 42L, 1420L, 777L, 31337L, 9001L, 123456789L};

    private static ParkStructure park(GameTestHelper helper) {
        Structure s = Kopieen.structuur(helper.getLevel(), ParkFeature.STRUCTUUR);
        helper.assertTrue(s instanceof ParkStructure, "guhs:knabbelpark is a ParkStructure");
        return (ParkStructure) s;
    }

    // =================================================================================================================
    @GuhTest(template = "empty", batch = "park_wereld", timeoutTicks = 40000)
    public static void parkVeleZaden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        ParkStructure park = park(helper);
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
        int parken = 0, geschikt = 0, mislukt = 0, stukken = 0, steil = 0, padCellen = 0, maxPoging = 0;
        List<String> fouten = new ArrayList<>();
        for (long seed : ZADEN) {
            RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
            int hier = 0;
            for (int i = -48; i <= 48 && hier < 4; i += 6) {
                for (int j = -48; j <= 48 && hier < 4; j += 6) {
                    ChunkPos chunk = new ChunkPos(i, j);
                    Holder<net.minecraft.world.level.biome.Biome> biome = biomes.getNoiseBiome(chunk.getMiddleBlockX() >> 2, 16, chunk.getMiddleBlockZ() >> 2,
                            random.sampler());
                    if (!park.biomes().contains(biome) || !park.droogEnGlooiend(generator, random, height, chunk, 1)) {
                        continue;
                    }
                    geschikt++;
                    WorldgenRandom wr = new WorldgenRandom(new LegacyRandomSource(0L));
                    wr.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
                    Structure.GenerationContext ctx = new Structure.GenerationContext(access, generator, biomes, random, level.getStructureManager(),
                            wr, seed, chunk, height, b -> true);
                    Optional<ParkStructure.Layout> layout = park.layout(ctx);
                    if (layout.isEmpty()) {
                        mislukt++;
                        fouten.add("seed " + seed + " " + chunk + ": no complete layout");
                        continue;
                    }
                    PiecesContainer pieces = layout.get().stub().getPiecesBuilder().build();
                    parken++;
                    hier++;
                    stukken += pieces.pieces().size();
                    maxPoging = Math.max(maxPoging, layout.get().poging());
                    String waar = "seed " + seed + " " + chunk;
                    Map<Identifier, Integer> n = ParkStructure.tel(pieces);
                    for (Identifier id : park.verplicht()) {
                        if (n.getOrDefault(id, 0) != 1) {
                            fouten.add(waar + ": " + id + " x" + n.getOrDefault(id, 0));
                        }
                    }
                    if (!verbonden(pieces.pieces())) {
                        fouten.add(waar + ": not every piece is joined to the plaza");
                    }
                    BoundingBox alles = pieces.calculateBoundingBox();
                    int mx = chunk.getMiddleBlockX(), mz = chunk.getMiddleBlockZ();
                    int reik = Math.max(Math.max(mx - alles.minX(), alles.maxX() - mx), Math.max(mz - alles.minZ(), alles.maxZ() - mz));
                    if (reik > 128) {
                        fouten.add(waar + ": reaches " + reik + " blocks");
                    }
                    int[] s = steil(level, generator, random, height, pieces.pieces());
                    steil += s[0];
                    padCellen += s[1];
                }
            }
        }
        String uit = String.format("Knabbelpark: %d seeds, %d dry and gentle spots, %d parks (%d without a complete layout), %d pieces on average, "
                        + "highest attempt %d, steps of 2+ on the raw land: %d of %d path cells", ZADEN.length, geschikt, parken, mislukt,
                parken == 0 ? 0 : stukken / parken, maxPoging, steil, padCellen);
        LOGGER.info(uit);
        helper.assertTrue(parken >= ZADEN.length * 2, "enough parks to look at: " + uit);
        helper.assertTrue(fouten.isEmpty(), "every park complete and joined: " + fouten + " / " + uit);
        helper.succeed();
    }

    /** Every piece joined to the first one (the plaza) through the jigsaw junctions. */
    private static boolean verbonden(List<StructurePiece> pieces) {
        int n = pieces.size();
        boolean[] gezien = new boolean[n];
        ArrayDeque<Integer> rij = new ArrayDeque<>();
        rij.add(0);
        gezien[0] = true;
        while (!rij.isEmpty()) {
            int a = rij.poll();
            for (int b = 0; b < n; b++) {
                if (!gezien[b] && (raakt(pieces.get(a), pieces.get(b)) || raakt(pieces.get(b), pieces.get(a)))) {
                    gezien[b] = true;
                    rij.add(b);
                }
            }
        }
        for (boolean g : gezien) {
            if (!g) {
                return false;
            }
        }
        return true;
    }

    /** Does a junction of a lie on b (b's box, one block around)? */
    private static boolean raakt(StructurePiece a, StructurePiece b) {
        if (!(a instanceof PoolElementStructurePiece pa)) {
            return false;
        }
        BoundingBox bb = b.getBoundingBox().inflatedBy(1);
        for (JigsawJunction j : pa.getJunctions()) {
            if (j.getSourceX() >= bb.minX() && j.getSourceX() <= bb.maxX() && j.getSourceZ() >= bb.minZ() && j.getSourceZ() <= bb.maxZ()) {
                return true;
            }
        }
        return false;
    }

    /** {steps of 2+ between neighbouring path cells on the raw land, path cells} of the walks. */
    private static int[] steil(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState random, LevelHeightAccessor height, List<StructurePiece> pieces) {
        int steil = 0, cellen = 0;
        for (StructurePiece piece : pieces) {
            if (!(piece instanceof PoolElementStructurePiece pool) || pool.getElement().getProjection() != StructureTemplatePool.Projection.TERRAIN_MATCHING) {
                continue;
            }
            Identifier id = PleinSlot.template(piece);
            StructureTemplate t = level.getStructureManager().getOrCreate(id);
            List<StructureTemplate.StructureBlockInfo> pad = t.filterBlocks(pool.getPosition(), new StructurePlaceSettings().setRotation(pool.getRotation()),
                    Blocks.DIRT_PATH, false);
            java.util.Map<Long, Integer> h = new java.util.HashMap<>();
            for (StructureTemplate.StructureBlockInfo b : pad) {
                h.put(BlockPos.asLong(b.pos().getX(), 0, b.pos().getZ()),
                        generator.getBaseHeight(b.pos().getX(), b.pos().getZ(), Heightmap.Types.WORLD_SURFACE_WG, height, random));
            }
            for (Map.Entry<Long, Integer> e : h.entrySet()) {
                cellen++;
                BlockPos p = BlockPos.of(e.getKey());
                for (Direction d : new Direction[]{Direction.EAST, Direction.SOUTH}) {
                    Integer o = h.get(BlockPos.asLong(p.getX() + d.getStepX(), 0, p.getZ() + d.getStepZ()));
                    if (o != null && Math.abs(o - e.getValue()) >= 2) {
                        steil++;
                    }
                }
            }
        }
        return new int[]{steil, cellen};
    }

    // =================================================================================================================
    @GuhTest(template = "empty", batch = "park_bouw", timeoutTicks = 12000)
    public static void parkGebouwd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ParkStructure park = park(helper);
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        List<String> fouten = new ArrayList<>();
        List<String> goed = new ArrayList<>();
        int[] chunks = {1900, 1920, 1940};
        for (int k = 0; k < chunks.length; k++) {
            ChunkPos chunk = new ChunkPos(chunks[k], 40);
            WorldgenRandom wr = new WorldgenRandom(new LegacyRandomSource(0L));
            wr.setLargeFeatureSeed(1420L + k, chunk.x(), chunk.z());
            Structure.GenerationContext ctx = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                    level.getStructureManager(), wr, 1420L + k, chunk, level, b -> true);
            Optional<ParkStructure.Layout> layout = park.layout(ctx);
            if (layout.isEmpty()) {
                fouten.add(chunk + ": no layout on flat land");
                continue;
            }
            StructureStart start = new StructureStart(park, chunk, 0, layout.get().stub().getPiecesBuilder().build());
            BoundingBox box = start.getBoundingBox();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    ChunkPos c = new ChunkPos(cx, cz);
                    start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                            new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY(), c.getMaxBlockZ()), c);
                }
            }
            ParkKeur.Uitslag u = ParkKeur.keur(level, start);
            (u.goed() ? goed : fouten).add(chunk + ": " + u);
            // the fountain round the Reisguh holds water
            BlockPos midden = Kopieen.wereld(start, ParkPlekken.PLEIN, ParkPlekken.REISGUH);
            int water = 0;
            for (BlockPos q : BlockPos.betweenClosed(midden.offset(-4, -2, -4), midden.offset(4, 0, 4))) {
                water += level.getFluidState(q).isSource() ? 1 : 0;
            }
            if (water < 12) {
                fouten.add(chunk + ": the fountain has " + water + " water");
            }
        }
        LOGGER.info("Knabbelpark built: {} / {}", goed, fouten);
        helper.assertTrue(fouten.isEmpty(), "every built park walkable and complete: " + fouten);
        helper.succeed();
    }

    // =================================================================================================================
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

    @GuhTest(template = KAMER, batch = "park", timeoutTicks = 400)
    public static void parkQuestlijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 7));
        ServerPlayer p2 = speler(helper, new BlockPos(8, 2, 7));
        Park.LIJN.wis(p);
        Park.LIJN.wis(p2);
        GuhNpcEntity wachter = npc(helper, GuhNpcEntity.Kind.PARKWACHTERGUH, new BlockPos(3, 2, 3));
        GuhNpcEntity verzorger = npc(helper, GuhNpcEntity.Kind.KOOIVERZORGERGUH, new BlockPos(11, 2, 3));
        NpcRole w = NpcRollen.rol(GuhNpcEntity.Kind.PARKWACHTERGUH), v = NpcRollen.rol(GuhNpcEntity.Kind.KOOIVERZORGERGUH);
        helper.assertTrue(w != null && v != null, "both have a role");
        List<GuhEntity> herten = new ArrayList<>();
        for (int n = 1; n <= Park.HERTEN; n++) {
            GuhEntity hert = Park.kooiHert(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2 + 3 * n, 2, 11))));
            hert.getPersistentData().putString(Bezetting.TAG, ParkFeature.HERT_ID + n + "@test");
            level.addFreshEntity(hert);
            herten.add(hert);
        }
        try {
            // 0 -> 1: the parkwachter asks for help
            w.talk(wachter, p);
            w.antwoord(wachter, p, 1);
            helper.assertTrue(Park.LIJN.stap(p) == Park.PLAQUETTES_STAP && Park.LIJN.stap(p2) == 0, "begun (only for this player)");
            // 1 -> 2: three plaques (one twice)
            Park.lees(p, 1);
            Park.lees(p, 1);
            Park.lees(p, 2);
            helper.assertTrue(Park.LIJN.stap(p) == Park.PLAQUETTES_STAP && Park.gelezen(p) == 2, "two read");
            Park.lees(p, 3);
            helper.assertTrue(Park.LIJN.stap(p) == Park.VOEREN, "all three read");
            // 2 -> 3: a knabbel for each Hertguhtje; they are never tamed
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
            Park.klik(herten.get(0), p, InteractionHand.MAIN_HAND);
            Park.klik(herten.get(0), p, InteractionHand.MAIN_HAND);   // (the same one again: no count)
            helper.assertTrue(Park.gevoerd(p) == 1, "the same Hertguhtje counts once");
            Park.klik(herten.get(1), p, InteractionHand.MAIN_HAND);
            Park.klik(herten.get(2), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(Park.LIJN.stap(p) == Park.KLUSJE, "all three fed");
            helper.assertTrue(p.getMainHandItem().getCount() == 1, "one knabbel each: " + p.getMainHandItem().getCount());
            helper.assertTrue(herten.stream().noneMatch(GuhEntity::isTame) && herten.stream().allMatch(Park::kooiHert), "kooi Hertguhtjes stay the kooi's");
            // 3 -> 4: wheat for the kooi-verzorger
            v.talk(verzorger, p);
            helper.assertTrue(Park.LIJN.stap(p) == Park.KLUSJE, "no wheat yet");
            p.getInventory().add(new ItemStack(Items.WHEAT, Park.HOOI));
            v.talk(verzorger, p);
            helper.assertTrue(Park.LIJN.stap(p) == Park.TERUG && p.getInventory().countItem(Items.WHEAT) == 0, "the wheat is delivered");
            // 4 -> 5: the reward, once
            w.talk(wachter, p);
            w.antwoord(wachter, p, 3);
            helper.assertTrue(Park.LIJN.klaar(p), "done");
            helper.assertTrue(p.getInventory().countItem(ParkFeature.EIGEN_BEELD_ITEM.get()) == 1, "the little statue");
            List<GuhEntity> eigen = level.getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(6),
                    g -> g.isTame() && g.isOwnedBy(p) && g.getVariant() == GuhVariant.HERTGUHTJE);
            helper.assertTrue(eigen.size() == 1, "one Hertguhtje of your own: " + eigen.size());
            Park.LIJN.zet(p, Park.TERUG);
            w.antwoord(wachter, p, 3);
            helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(6),
                    g -> g.isTame() && g.isOwnedBy(p) && g.getVariant() == GuhVariant.HERTGUHTJE).size() == 1
                    && p.getInventory().countItem(ParkFeature.EIGEN_BEELD_ITEM.get()) == 1, "never twice");
            // the own guh poses for the statue
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ParkFeature.EIGEN_BEELD_ITEM.get()));
            Park.klik(eigen.get(0), p, InteractionHand.MAIN_HAND);
            var data = p.getMainHandItem().get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && "hertguhtje".equals(data.copyTag().getStringOr(BeeldBlockEntity.VARIANT, "")), "the statue took its look");
            eigen.forEach(Entity::discard);
            helper.succeed();
        } finally {
            herten.forEach(Entity::discard);
            wachter.discard();
            verzorger.discard();
            Park.LIJN.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(p2, Entity.RemovalReason.DISCARDED);
        }
    }

    @GuhTest(template = KAMER, batch = "park", timeoutTicks = 200)
    public static void parkBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 7));
        try {
            // the litter bin eats one, never a tool
            BlockPos bak = helper.absolutePos(new BlockPos(5, 2, 5));
            level.setBlockAndUpdate(bak, ParkFeature.PRULLENBAK.get().defaultBlockState());
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bak), Direction.UP, bak, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 5));
            level.getBlockState(bak).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(p.getMainHandItem().getCount() == 4, "one eaten");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            level.getBlockState(bak).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD), "a sword is too nice");
            // the own statue keeps its look when broken
            BlockPos beeld = helper.absolutePos(new BlockPos(9, 2, 5));
            level.setBlockAndUpdate(beeld, ParkFeature.EIGEN_BEELD.get().defaultBlockState());
            helper.assertTrue(level.getBlockEntity(beeld) instanceof BeeldBlockEntity, "a statue block entity");
            ((BeeldBlockEntity) level.getBlockEntity(beeld)).zet(GuhVariant.MINT, net.minecraft.network.chat.Component.literal("Muntje"));
            List<ItemStack> drops = Block.getDrops(level.getBlockState(beeld), level, beeld, level.getBlockEntity(beeld));
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(ParkFeature.EIGEN_BEELD_ITEM.get())
                    && "mint".equals(drops.get(0).get(DataComponents.CUSTOM_DATA).copyTag().getStringOr(BeeldBlockEntity.VARIANT, "")), "drops itself with its look");
            // the park's Reisguh stays where she is
            GuhNpcEntity reis = npc(helper, GuhNpcEntity.Kind.REISGUH, new BlockPos(3, 2, 10));
            reis.getPersistentData().putBoolean(Reisguh.VAST, true);
            Reisguh.pickUp(reis, p);
            helper.assertTrue(!reis.isRemoved(), "she can not be picked up");
            reis.discard();
            // a park statue on its plinth; the plaque reads (outside the questline nothing changes)
            BlockState b = ParkFeature.BEELD.get().defaultBlockState().setValue(ParkBlokken.BEELD, ParkBlokken.Soort.OPA_KAASGUH);
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(11, 2, 9)), b);
            helper.assertTrue(level.getBlockEntity(helper.absolutePos(new BlockPos(11, 2, 9))) instanceof BeeldBlockEntity, "a park statue");
            Park.LIJN.wis(p);
            Park.lees(p, 2);
            helper.assertTrue(Park.LIJN.stap(p) == 0 && Park.gelezen(p) == 0, "reading outside the questline counts for nothing");
            // the Hertguhtje: a variant with antlers, a bonus Guhdex page, and the kooi flag
            helper.assertTrue(GuhVariant.HERTGUHTJE.shows("hert_gewei_links") && !GuhVariant.NORMAL.shows("hert_gewei_links")
                    && !GuhVariant.HERTGUHTJE.isCharacter() && GuhDex.ENTRIES.contains(GuhVariant.HERTGUHTJE) && GuhDex.EXTRA.contains(GuhVariant.HERTGUHTJE)
                    && GuhDex.TAMEABLE.contains(GuhVariant.HERTGUHTJE), "the Hertguhtje's page");
            GuhEntity hert = Park.kooiHert(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 11))));
            helper.assertTrue(GuhHooks.heeft(hert, ParkFeature.KOOIHERT) && hert.getVariant() == GuhVariant.HERTGUHTJE, "a kooi Hertguhtje");
            helper.succeed();
        } finally {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }
}
