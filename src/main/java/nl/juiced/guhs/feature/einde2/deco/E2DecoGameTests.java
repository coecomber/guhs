package nl.juiced.guhs.feature.einde2.deco;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * Game tests of the einde2 slice deco. The buildings are data, so the tests read what the game made of it: every building
 * has its structure, its own set with its own salt, its biome tag and a template that fits the rules (size, one modest
 * chest at most, a sign, the anchor in the middle of its ground layer); every template stands whole after it is put down
 * the way worldgen does it (nothing falls off for want of support, its creatures appear); the dimension's generator starts
 * them rarely, only in their own biome and with the ground layer on the island's surface ({@link Einde2Test}: the headless
 * test server has no Guheinde); and the four little blocks keep their own rules. The python self-check of every template
 * (tools/features/einde2_deco_bouw.py: walkable, lit, nothing floating) runs when the resources are generated.
 */
public class E2DecoGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMPTY = "empty";
    private static final String KAMER = "einde2_deco_test_kamer";
    private static final String VELD = "einde2_deco_test_veld";
    private static final String BATCH = "einde2_deco";
    private static final Set<String> KISTEN = Set.of("guhs:chests/einde2_deco", "guhs:chests/einde2_deco_thee", "guhs:chests/einde2_deco_mika");
    private static final List<String> GEWOON = List.of("gatenkaasheuvels", "korstwoestijn");
    /** The biomes each building belongs to (DESIGN 4; tools/features/einde2_deco.py DECO). */
    private static final Map<String, List<String>> BIOMES = new LinkedHashMap<>();

    static {
        for (String id : List.of("deco_luchtbizonstal", "deco_meditatiepaal", "deco_luchtbalveld_ruine")) {
            BIOMES.put(id, List.of("kruimelwolken"));
        }
        for (String id : List.of("deco_geestenoase_vijver", "deco_ijsvissershutje", "deco_waterstam_kano", "deco_bevroren_kaasbol")) {
            BIOMES.put(id, List.of("schimmelkaasvlakte"));
        }
        for (String id : List.of("deco_das_molguh_grot", "deco_aardrommel_ring", "deco_koolkraam_wrak", "deco_geheime_tunnel")) {
            BIOMES.put(id, List.of("kristalkaasklippen"));
        }
        for (String id : List.of("deco_sintelstrand_huisje", "deco_zonnekrijger_altaar", "deco_theekraampje")) {
            BIOMES.put(id, List.of("knabbelkoorbos"));
        }
        BIOMES.put("deco_mika_wachtpost", GEWOON);
        BIOMES.put("deco_vetschipwrak", GEWOON);
        BIOMES.put("deco_boemerangboom", List.of("gatenkaasheuvels"));
        for (String id : List.of("deco_karavaanresten", "deco_pai_sho_tafeltje", "deco_appa_afdruk")) {
            BIOMES.put(id, List.of("korstwoestijn"));
        }
    }

    /** One block of a template: where, and what. */
    private record Blok(BlockPos pos, BlockState state, CompoundTag nbt) {}

    /** Every block of a template, read from the template itself. */
    private static List<Blok> blokken(ServerLevel level, StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(net.minecraft.nbt.NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompoundOrEmpty(i)));
        }
        List<Blok> uit = new ArrayList<>();
        ListTag blocks = tag.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompoundOrEmpty(i);
            ListTag pos = b.getListOrEmpty("pos");
            uit.add(new Blok(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), states.get(b.getIntOr("state", 0)),
                    b.getCompound("nbt").orElse(null)));
        }
        return uit;
    }

    private static boolean isBord(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("_sign");
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the data of the twenty buildings
    // ------------------------------------------------------------------------------------------------------------------
    /** Every building: its structure, its own set and salt, its biome tag, its room rules, its template and its sidecar. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2DecoBouwsels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        helper.assertTrue(E2DecoFeature.BOUWSELS.size() == 20 && Set.copyOf(E2DecoFeature.BOUWSELS).equals(BIOMES.keySet()), "the twenty buildings");
        Set<Integer> salts = new java.util.HashSet<>();
        for (StructureSet set : access.lookupOrThrow(Registries.STRUCTURE_SET)) {
            if (set.placement() instanceof RandomSpreadStructurePlacement) {
                salts.add(saltVan(access, set));
            }
        }
        int kisten = 0;
        for (int i = 0; i < E2DecoFeature.BOUWSELS.size(); i++) {
            String id = E2DecoFeature.BOUWSELS.get(i);
            Structure structure = access.lookupOrThrow(Registries.STRUCTURE).getValue(Guhs.id(id));
            helper.assertTrue(structure instanceof E2DecoStructure, id + ": a guhs:einde2_deco_bouwsel structure");
            E2DecoStructure ruimte = (E2DecoStructure) structure;
            helper.assertTrue(ruimte.minSurfaceY() >= 50 && ruimte.maxVerschil() <= E2DecoCommando.GROND, id + ": on an island, and no steeper than its footing is deep");
            helper.assertTrue(ruimte.voorrang() >= 5 && ruimte.voorrang() <= 19, id + ": gives way to everything (voorrang " + ruimte.voorrang() + ")");
            helper.assertTrue(ruimte.keepClear() >= 8 && ruimte.keepClear() <= 28, id + ": claims the room of its piece (" + ruimte.keepClear() + ")");
            StructureSet set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(id));
            helper.assertTrue(set != null && set.structures().size() == 1 && set.placement() instanceof RandomSpreadStructurePlacement, id + ": its own set");
            RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
            helper.assertTrue(saltVan(access, set) == E2DecoFeature.EERSTE_SALT + i * E2DecoFeature.SALT_STAP,
                    id + ": its own salt, in the order of the list: " + saltVan(access, set));
            // (the two common biomes are everywhere: a wide grid; a rare biome is a small patch and keeps its buildings rare by itself)
            boolean overal = BIOMES.get(id).stream().anyMatch(GEWOON::contains);
            helper.assertTrue(spread.spacing() >= (overal ? 40 : 12) && spread.separation() >= (overal ? 16 : 5),
                    id + ": a wide grid: " + spread.spacing() + " / " + spread.separation());
            // the biome tag: exactly the biomes of the design, never the main island
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, Guhs.id("has_structure/" + id));
            List<String> in = new ArrayList<>();
            for (Holder<Biome> b : access.lookupOrThrow(Registries.BIOME).getTagOrEmpty(tag)) {
                in.add(b.unwrapKey().orElseThrow().identifier().getPath());
            }
            helper.assertTrue(in.size() == BIOMES.get(id).size() && in.containsAll(BIOMES.get(id)), id + ": bound to " + BIOMES.get(id) + ", not " + in);
            // the template
            StructureTemplate template = E2DecoCommando.template(level, id);
            helper.assertTrue(template != null, id + ": its template einde2_deco/" + id);
            Vec3i size = template.getSize();
            helper.assertTrue(size.getX() <= 24 && size.getZ() <= 24 && size.getY() <= 24, id + ": at most 24 x 24: " + size);
            int borden = 0, laden = 0, ankers = 0;
            for (Blok b : blokken(level, template)) {
                if (isBord(b.state())) {
                    borden++;
                    helper.assertTrue(b.nbt() != null && b.nbt().getCompoundOrEmpty("front_text").getListOrEmpty("messages").toString().contains("sign.guhs.einde2_deco."),
                            id + ": the sign at " + b.pos() + " reads a text of this slice");
                }
                if (b.nbt() != null && b.nbt().contains("LootTable")) {
                    laden++;
                    String loot = b.nbt().getStringOr("LootTable", "");
                    helper.assertTrue(KISTEN.contains(loot), id + ": the loot of its chest: " + loot);
                    LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)));
                    helper.assertTrue(table != LootTable.EMPTY, id + ": the loot table " + loot + " exists");
                }
                if (b.state().is(Blocks.JIGSAW)) {
                    ankers++;
                    helper.assertTrue(b.nbt() != null && b.nbt().getStringOr("name", "").equals("guhs:einde2_deco/" + id), id + ": the anchor's name");
                    helper.assertTrue(b.pos().getY() == E2DecoCommando.GROND && b.pos().getX() == size.getX() / 2 && b.pos().getZ() == size.getZ() / 2,
                            id + ": the anchor in the middle of the ground layer: " + b.pos());
                }
            }
            helper.assertTrue(borden >= 1, id + ": a sign with a joke");
            helper.assertTrue(laden <= 1, id + ": one modest chest at most: " + laden);
            helper.assertTrue(ankers == 1, id + ": one anchor: " + ankers);
            kisten += laden;
            // the sidecar: a later phase finds its spots
            Map<String, Plekken.Plek> plekken = Plekken.van(id);
            helper.assertTrue(plekken.containsKey("midden") && plekken.containsKey("bordje"), id + ": the sidecar with midden and bordje: " + plekken.keySet());
            helper.assertTrue((laden == 1) == plekken.containsKey("kist"), id + ": the spot kist and the chest go together");
            helper.assertTrue(plekken.values().stream().allMatch(p -> ("einde2_deco/" + id).equals(p.stuk())), id + ": every spot names its piece");
        }
        helper.assertTrue(kisten >= 8 && kisten <= 12, "about half of the buildings have a chest: " + kisten);
        helper.assertTrue(salts.size() >= 20, "the salts are unique");
        helper.succeed();
    }

    private static int saltVan(net.minecraft.core.RegistryAccess access, StructureSet set) {
        // (the salt is not public: read it from the set's own serialised form)
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access);
        var json = StructureSet.DIRECT_CODEC.encodeStart(ops, set).getOrThrow().getAsJsonObject();
        return json.getAsJsonObject("placement").get("salt").getAsInt();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the templates in the world
    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Every template put down the way a structure piece is (blocks look at their neighbours): afterwards every block of
     * the template is still there (nothing fell off a wall or a ceiling), its creatures stand in it and nothing lies on the
     * ground as an item.
     */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 600)
    public static void einde2DecoBlijftHeel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        int dieren = 0;
        List<String> fout = new ArrayList<>();
        for (String id : E2DecoFeature.BOUWSELS) {
            StructureTemplate template = E2DecoCommando.template(level, id);
            List<Blok> blokken = blokken(level, template);
            Vec3i size = template.getSize();
            helper.assertTrue(template.placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS), id + ": placed");
            List<String> weg = new ArrayList<>();
            for (Blok b : blokken) {
                BlockState nu = level.getBlockState(at.offset(b.pos()));
                boolean goed = b.state().is(Blocks.JIGSAW) ? !nu.isAir() : nu.is(b.state().getBlock());
                if (!goed) {
                    weg.add(b.state().getBlock().getName().getString() + " at " + b.pos() + " is now " + nu.getBlock().getName().getString());
                }
            }
            if (!weg.isEmpty()) {
                fout.add(id + ": " + weg.size() + " blocks did not stay, e.g. " + weg.subList(0, Math.min(4, weg.size())));
            }
            AABB doos = new AABB(at.getX() - 1, at.getY() - 1, at.getZ() - 1, at.getX() + size.getX() + 1, at.getY() + size.getY() + 1, at.getZ() + size.getZ() + 1);
            List<Entity> entities = level.getEntities((Entity) null, doos, e -> !(e instanceof Player));
            if (entities.stream().anyMatch(e -> e instanceof ItemEntity)) {
                fout.add(id + ": something dropped as an item: " + entities.stream().filter(e -> e instanceof ItemEntity).map(e -> ((ItemEntity) e).getItem().toString()).toList());
            }
            int verwacht = (int) Plekken.van(id).values().stream().filter(p -> Plekken.WEZEN.equals(p.soort())).count();
            int staat = (int) entities.stream().filter(e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(Guhs.MODID)).count();
            if (staat != verwacht) {
                fout.add(id + ": its creatures stand in it: " + staat + " of " + verwacht);
            }
            dieren += staat;
            // away with it, for the next one
            entities.forEach(Entity::discard);
            for (Blok b : blokken) {
                level.setBlock(at.offset(b.pos()), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
            level.getEntities((Entity) null, doos, e -> !(e instanceof Player)).forEach(Entity::discard);
        }
        helper.assertTrue(fout.isEmpty(), fout.size() + " buildings did not stay whole: " + fout);
        helper.assertTrue(dieren >= 5, "the element animals live at their buildings: " + dieren);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // placement, sampled from the dimension's own generator
    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Two sets with the same grid do not put their buildings side by side: vanilla seeds a grid cell with world seed + salt, and
     * salts that lie close together give nearly the same spot in the cell (with consecutive salts every pair shares its x).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2DecoLosVanElkaar(GameTestHelper helper) {
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        java.util.Random r = new java.util.Random(21400850L);
        int paren = 0;
        for (int a = 0; a < E2DecoFeature.BOUWSELS.size(); a++) {
            for (int b = a + 1; b < E2DecoFeature.BOUWSELS.size(); b++) {
                RandomSpreadStructurePlacement een = (RandomSpreadStructurePlacement) sets.getValue(Guhs.id(E2DecoFeature.BOUWSELS.get(a))).placement();
                RandomSpreadStructurePlacement twee = (RandomSpreadStructurePlacement) sets.getValue(Guhs.id(E2DecoFeature.BOUWSELS.get(b))).placement();
                if (een.spacing() != twee.spacing() || een.separation() != twee.separation()) {
                    continue;
                }
                paren++;
                int zelfdeX = 0, zelfdeZ = 0, n = 300, ruimte = een.spacing() - een.separation();
                for (int i = 0; i < n; i++) {
                    long seed = r.nextLong();
                    int rx = (r.nextInt(600) - 300) * een.spacing(), rz = (r.nextInt(600) - 300) * een.spacing();
                    ChunkPos p = een.getPotentialStructureChunk(seed, rx, rz), q = twee.getPotentialStructureChunk(seed, rx, rz);
                    zelfdeX += p.x() == q.x() ? 1 : 0;
                    zelfdeZ += p.z() == q.z() ? 1 : 0;
                }
                // (chance: one in `ruimte`; three times that is far from it and far below what close salts give: always)
                int mag = Math.max(n / 4, 3 * n / ruimte);
                helper.assertTrue(zelfdeX <= mag && zelfdeZ <= mag, E2DecoFeature.BOUWSELS.get(a) + " and " + E2DecoFeature.BOUWSELS.get(b)
                        + " stand side by side: the same x in " + zelfdeX + " and the same z in " + zelfdeZ + " of " + n + " cells");
            }
        }
        helper.assertTrue(paren >= 40, "the sets that share a grid: " + paren + " pairs");
        helper.succeed();
    }

    /**
     * In a square of 4000 x 4000 blocks of the outer islands, for three seeds: the buildings are rare (the void stays quiet)
     * but not so rare that nobody ever meets one; each starts only in its own biome, far from the main island, with its
     * ground layer on the surface in the middle of its piece and under the top of the island buildings.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2DecoZeldzaam(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        int alleZeldzaam = 0;
        for (long seed : new long[]{21400801L, 21400802L, 21400803L}) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            int totaal = 0, zeldzaam = 0, gewoon = 0, geprobeerd = 0;
            StringBuilder per = new StringBuilder();
            for (String id : E2DecoFeature.BOUWSELS) {
                StructureSet set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(id));
                RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, Guhs.id("has_structure/" + id));
                int cel = spread.spacing() * 16, n = 0, kans = 0;
                // the square x 1500 .. 5500, z -2000 .. 2000: every cell of the set's grid whose candidate chunk lies in it
                for (int rx = Math.floorDiv(1500, cel); rx <= Math.floorDiv(5500, cel); rx++) {
                    for (int rz = Math.floorDiv(-2000, cel); rz <= Math.floorDiv(2000, cel); rz++) {
                        ChunkPos chunk = spread.getPotentialStructureChunk(seed, rx * spread.spacing(), rz * spread.spacing());
                        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
                        if (x < 1500 || x >= 5500 || z < -2000 || z >= 2000 || !Einde2Test.biome(w, chunk.getMinBlockX(), chunk.getMinBlockZ()).is(tag)) {
                            continue;       // (a cheap look first: the generator's own biome check comes after its nine height samples)
                        }
                        geprobeerd++;
                        kans++;
                        StructureStart start = Einde2Test.start(w, id, chunk);
                        if (start == null) {
                            continue;
                        }
                        n++;
                        BoundingBox doos = start.getPieces().get(0).getBoundingBox();
                        int mx = (doos.minX() + doos.maxX()) / 2, mz = (doos.minZ() + doos.maxZ()) / 2;
                        int grond = w.generator().getFirstFreeHeight(mx, mz, Heightmap.Types.WORLD_SURFACE_WG, w.hoogte(), w.random()) - 1;
                        helper.assertTrue(start.getPieces().size() == 1, id + ": one piece");
                        helper.assertTrue(doos.minY() + E2DecoCommando.GROND == grond, id + " at " + chunk + ": the ground layer lies on the surface: " + doos.minY()
                                + " + " + E2DecoCommando.GROND + " vs " + grond);
                        helper.assertTrue(grond >= 51 && doos.maxY() <= Einde2Ids.EILANDBOUW_MAX, id + ": on an island and under the top of the island buildings: " + doos);
                        // the whole piece stands on the island, and the island is flat enough under it
                        int laag = Integer.MAX_VALUE, hoog = Integer.MIN_VALUE;
                        for (int[] p : E2DecoStructure.punten(doos)) {
                            int h = w.generator().getBaseHeight(p[0], p[1], Heightmap.Types.WORLD_SURFACE_WG, w.hoogte(), w.random());
                            laag = Math.min(laag, h);
                            hoog = Math.max(hoog, h);
                        }
                        helper.assertTrue(laag >= 52 && hoog - laag <= E2DecoCommando.GROND, id + " at " + chunk + ": island under all of it, and flat enough: " + laag + ".." + hoog);
                        helper.assertTrue(Math.hypot(x, z) >= Einde2Ids.STRAAL_MIDDEN + 100, id + ": far from the main island");
                        helper.assertTrue(Einde2Test.biome(w, mx, mz).is(tag) || Einde2Test.biome(w, chunk.getMinBlockX(), chunk.getMinBlockZ()).is(tag),
                                id + " at " + chunk + ": in its own biome");
                    }
                }
                totaal += n;
                helper.assertTrue(n <= 14, "seed " + seed + ": " + id + " is not everywhere: " + n + " in 4000 x 4000 blocks");
                if (BIOMES.get(id).stream().anyMatch(GEWOON::contains)) {
                    gewoon += n;
                } else {
                    zeldzaam += n;
                }
                per.append(id.substring(5)).append(' ').append(n).append('/').append(kans).append(", ");
            }
            LOGGER.info("einde2 deco, seed {}: {} buildings in 4000 x 4000 blocks ({} in the common biomes, {} in the rare ones; {} spots in their biome tried; per building started/tried): {}",
                    seed, totaal, gewoon, zeldzaam, geprobeerd, per);
            // (a square of this size holds a handful of patches of the rare biomes, or hardly any: they are counted over the seeds)
            helper.assertTrue(totaal >= 10 && totaal <= 80, "seed " + seed + ": rare, but there: " + totaal + " buildings in 4000 x 4000 blocks");
            helper.assertTrue(gewoon >= 4 && gewoon <= 40, "seed " + seed + ": in the two common biomes: " + gewoon);
            alleZeldzaam += zeldzaam;
        }
        helper.assertTrue(alleZeldzaam >= 8, "in the rare biomes too, over the three seeds: " + alleZeldzaam);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the four little blocks, and the chests
    // ------------------------------------------------------------------------------------------------------------------
    /** A kool is squashed by whoever lands on it (and that is a moment to remember); a squashed one still drops a kool. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2DecoKool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, E2DecoFeature.KOOL.get());
        BlockPos abs = helper.absolutePos(pos);
        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(GameType.SURVIVAL);
        BlockState heel = level.getBlockState(abs);
        heel.getBlock().fallOn(level, heel, abs, speler, 0.3);
        helper.assertTrue(!level.getBlockState(abs).getValue(E2DecoBlokken.Kool.GEPLET), "a step is not a jump: the kool is whole");
        heel.getBlock().fallOn(level, heel, abs, speler, 1.2);
        helper.assertTrue(level.getBlockState(abs).getValue(E2DecoBlokken.Kool.GEPLET), "a jump squashes it");
        var adv = level.getServer().getAdvancements().get(Guhs.id("guheinde/einde2_deco_kolen"));
        helper.assertTrue(adv != null && speler.getAdvancements().getOrStartProgress(adv).isDone(), "MIJN KOLEN!");
        helper.assertTrue(!E2DecoBlokken.Kool.plet(level, abs, level.getBlockState(abs)), "flat is flat");
        helper.assertTrue(Block.getDrops(level.getBlockState(abs), level, abs, null).stream().anyMatch(s -> s.is(E2DecoFeature.KOOL_ITEM.get())),
                "a squashed kool still drops a kool");
        helper.getLevel().removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** The windgong hangs under a block, the eeuwige vlam and the lotustegel stand on one; the flame gives light. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void einde2DecoHangenEnStaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos balk = new BlockPos(2, 4, 2);
        helper.setBlock(balk, Blocks.SPRUCE_FENCE);
        helper.setBlock(balk.below(), E2DecoFeature.WINDGONG.get());
        helper.assertTrue(E2DecoFeature.WINDGONG.get().defaultBlockState().canSurvive(level, helper.absolutePos(balk.below())), "a windgong hangs under a fence");
        helper.assertTrue(!E2DecoFeature.WINDGONG.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(4, 4, 4))), "not in mid air");
        level.setBlock(helper.absolutePos(balk), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertBlockPresent(Blocks.AIR, balk.below());
        for (Block blok : List.of(E2DecoFeature.EEUWIGE_VLAM.get(), E2DecoFeature.LOTUSTEGEL.get())) {
            BlockPos sokkel = new BlockPos(4, 2, 2);
            helper.setBlock(sokkel, Blocks.CHISELED_SANDSTONE);
            helper.setBlock(sokkel.above(), blok);
            helper.assertBlockPresent(blok, sokkel.above());
            level.setBlock(helper.absolutePos(sokkel), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertBlockPresent(Blocks.AIR, sokkel.above());
        }
        helper.assertTrue(E2DecoFeature.EEUWIGE_VLAM.get().defaultBlockState().getLightEmission() == 15, "the eternal flame gives light");
        for (var blok : E2DecoFeature.BLOCK_ITEMS) {
            BlockPos p = helper.absolutePos(new BlockPos(1, 2, 1));
            helper.assertTrue(Block.getDrops(blok.get().defaultBlockState(), level, p, null).stream().anyMatch(s -> s.is(blok.get().asItem())),
                    blok.getId() + " drops itself");
        }
        helper.succeed();
    }

    /** The three chests are modest: a handful of stacks, every item exists, nothing a player would farm a building for. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 100)
    public static void einde2DecoBescheidenLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<net.minecraft.world.item.Item> teRijk = Set.of(Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE, Items.DIAMOND_BLOCK, Items.GOLD_BLOCK, Items.ELYTRA, Items.DRAGON_HEAD, Items.TOTEM_OF_UNDYING);
        for (String loot : KISTEN) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(loot)));
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                    .create(LootContextParamSets.CHEST);
            int stapels = 0, stuks = 0, leeg = 0;
            for (int i = 0; i < 200; i++) {
                List<ItemStack> items = table.getRandomItems(params);
                leeg += items.isEmpty() ? 1 : 0;
                helper.assertTrue(items.size() <= 6, loot + ": at most a handful of stacks: " + items.size());
                for (ItemStack s : items) {
                    helper.assertTrue(!s.isEmpty() && !teRijk.contains(s.getItem()) && !s.isEnchanted(), loot + ": modest: " + s);
                    stapels++;
                    stuks += s.getCount();
                }
            }
            helper.assertTrue(leeg == 0, loot + ": never empty");
            helper.assertTrue(stuks <= 200 * 16 && stapels >= 200 * 2, loot + ": on average a few things: " + stuks + " items in " + stapels + " stacks over 200 chests");
        }
        helper.succeed();
    }
}
