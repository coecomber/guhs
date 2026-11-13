package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.storage.GuhSavedData;
import nl.juiced.guhs.world.NieuwTerrein;

/**
 * 1.10.0 (y-tempel): THE fixed ordetempel. The worldgen temples ({@code guhs:vr_w_portaaltempel}) only appear in new
 * chunks, so on an explored server an op places one copy of the same template by hand:
 * <ul>
 *   <li>{@link #zoek}: spots near the middle with only natural blocks, flat, dry and unprotected (top 5, with a score);</li>
 *   <li>{@link #plaats}: snapshots the blocks it replaces (a template file in the world folder), clears the hills/trees above the
 *       floor, fills small dips under it, places the template (its portal sleeps, Werver Wimpel comes like at any temple), and
 *       registers it (SavedData, server-wide);</li>
 *   <li>{@link #ongedaan}: puts the snapshot back and forgets the temple.</li>
 * </ul>
 * The Superkompas entry "Het Vadsrijk" points here first ({@link #wijs}). Commands: {@code /guhs vadsrijk tempel ...}.
 */
public final class VasteTempel extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final SavedDataType<VasteTempel> TYPE = GuhSavedData.tagType("vr_vaste_tempel", VasteTempel::new, VasteTempel::load,
            VasteTempel::save);
    public static final Identifier TEMPLATE = Guhs.id("vadsrijk/wereld/portaaltempel");
    /** Half the side of the square the finder checks (the template is 61 x 60, any turn fits in 63). */
    public static final int HALF = 31;
    /** Template layers below the ground (worldgen: start_height -3 on the surface). */
    static final int ONDER = 3;
    /** Most height difference (blocks) over the footprint that still counts as flat. */
    static final int MAX_VERSCHIL = 5;
    static final int BOVEN = 10, FUNDERING = 6;

    // ---- the saved registration ---------------------------------------------------------------------------------------------
    @Nullable
    String dim;
    @Nullable
    BlockPos midden, portaal;
    int draai;
    @Nullable
    BoundingBox doos, backupDoos;
    String backup = "";
    long tijd;

    public static VasteTempel get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (dim != null && midden != null && doos != null && backupDoos != null) {
            t.putString("dim", dim);
            t.putLong("midden", midden.asLong());
            t.putLong("portaal", (portaal == null ? midden : portaal).asLong());
            t.putInt("draai", draai);
            t.putIntArray("doos", new int[]{doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ()});
            t.putIntArray("backupDoos", new int[]{backupDoos.minX(), backupDoos.minY(), backupDoos.minZ(), backupDoos.maxX(), backupDoos.maxY(),
                    backupDoos.maxZ()});
            t.putString("backup", backup);
            t.putLong("tijd", tijd);
        }
        return t;
    }

    private static VasteTempel load(CompoundTag t) {
        VasteTempel v = new VasteTempel();
        if (t.contains("dim")) {
            v.dim = t.getStringOr("dim", "");
            v.midden = BlockPos.of(t.getLongOr("midden", 0L));
            v.portaal = BlockPos.of(t.getLongOr("portaal", 0L));
            v.draai = t.getIntOr("draai", 0);
            int[] d = t.getIntArray("doos").orElse(new int[6]);
            int[] b = t.getIntArray("backupDoos").orElse(new int[6]);
            v.doos = new BoundingBox(d[0], d[1], d[2], d[3], d[4], d[5]);
            v.backupDoos = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
            v.backup = t.getStringOr("backup", "");
            v.tijd = t.getLongOr("tijd", 0L);
        }
        return v;
    }

    public boolean geregistreerd() {
        return dim != null && doos != null;
    }

    @Nullable
    public BlockPos midden() {
        return midden;
    }

    @Nullable
    public BlockPos portaal() {
        return portaal;
    }

    @Nullable
    public String dim() {
        return dim;
    }

    /** The fixed temple's box when it stands in this level, else null. */
    @Nullable
    public static BoundingBox doos(ServerLevel level) {
        VasteTempel v = get(level.getServer());
        return v.geregistreerd() && level.dimension().identifier().toString().equals(v.dim) ? v.doos : null;
    }

    /** Is this spot inside the fixed temple (Werver's gate treats it like a worldgen temple)? */
    public static boolean in(ServerLevel level, BlockPos pos) {
        BoundingBox b = doos(level);
        return b != null && b.isInside(pos);
    }

    /** Where the Superkompas entry "Het Vadsrijk" points in this level: the fixed temple's portal, or null. */
    @Nullable
    public static BlockPos wijs(ServerLevel level) {
        VasteTempel v = get(level.getServer());
        return doos(level) != null ? (v.portaal != null ? v.portaal : v.midden) : null;
    }

    // ---- natural blocks ----------------------------------------------------------------------------------------------------

    /** Guhs blocks that the Guhmension's terrain, trees and plants are made of (from its worldgen data). */
    private static final Set<String> GUHS_NATUUR = Set.of("as_aarde", "bladerhoopje", "bleekhout_bladeren", "bleekhout_stam",
            "bleekhout_zaailing", "bleekmos", "bleekmos_tapijt", "diamanterts_kristal", "dorre_korststruik", "esdoorn_bladeren_oranje",
            "esdoorn_bladeren_rood", "esdoorn_stam", "gatenkaas", "gebarsten_korst", "glinsterpaddenstoel", "glitterrank", "glitterrank_bes",
            "gouderts_knuffel", "gouderts_kristal", "guh_ijsbloempje", "guh_kristal_blok", "guh_kristal_cluster", "guh_kristalsteen",
            "guhbloesem_leaves", "guhbloesem_log", "guhbloesem_sapling", "guhklei", "guhoortjes", "guhpaddenstoel", "guhpaddenstoel_hoed",
            "guhpaddenstoel_steel", "guhwaii_orchidee", "guhwaii_paradijsbloem", "guhwaii_plumeria", "ijzererts_knuffel", "ijzererts_kristal",
            "ijzererts_vilt", "kaasbloem", "kaasknabbel_cobblestone", "kaasknabbel_deepslate", "kaasknabbel_dirt", "kaasknabbel_stone",
            "kaasknabbelerts_knuffel", "kaasknabbelerts_vilt", "kaaskorrelerts", "kaaskorrelerts_knuffel", "kaaskorrelerts_kristal", "kaaskorst",
            "kaasmodder", "kaasriet", "klipkaas", "knabbelbessenstruik", "knabbelroos", "knuffelgras", "knuffelsteen", "knuffelwortelaarde",
            "knuffelwortels", "kolenerts_knuffel", "kolenerts_vilt", "kopererts_knuffel", "kopererts_vilt", "korstgras", "kristalkaas_erts",
            "kristalkaas_kristal", "kruimelzand", "lapiserts_knuffel", "lapiserts_kristal", "lichtmos", "lichtmosblok", "modderig_kaasgras",
            "moerasgras", "oogbloempje", "parelmoer", "pigmenterts_knuffel", "pigmenterts_kristal", "pluisbolletje", "pluisgras",
            "pluizenboom_bladeren", "pluizenboom_stam", "pluizenboom_zaailing", "redstoneerts_kristal", "rijpgras", "rijpsprietjes", "roze_gras",
            "roze_guhbloem", "roze_hibiscus", "schildjeszand", "schimmelkaas", "schimmelpaddenstoel", "smaragderts_knuffel", "smaragderts_vilt",
            "vadserts_knuffel", "vadserts_kristal", "vadshout_bladeren", "vadshout_stam", "vadshout_zaailing", "vadsmos", "viltsteen",
            "wolkenweide_gras", "wolkenweide_steen", "wolkenweide_steen_blauw", "wolkenweide_steen_lila", "zalmklei", "zalmklei_gras",
            "zalmklei_streep_a", "zalmklei_streep_b", "zeegroen_gras");
    private static final Set<Block> VANILLA_NATUUR = Set.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.STONE, Blocks.GRAVEL, Blocks.CLAY, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.PINK_PETALS,
            Blocks.LEAF_LITTER, Blocks.SANDSTONE, Blocks.SNOW_BLOCK, Blocks.MUD, Blocks.BEDROCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
            Blocks.FERN, Blocks.LARGE_FERN, Blocks.SWEET_BERRY_BUSH, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.COAL_ORE,
            Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.CALCITE, Blocks.LILY_PAD);

    /** Could the world have made this block by itself (no player build, no structure, no container)? */
    public static boolean natuur(BlockState s) {
        if (s.isAir()) {
            return true;
        }
        if (s.hasBlockEntity()) {
            return false;
        }
        if (s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.PERSISTENT)) {
            return false;   // (leaves a player placed)
        }
        if (s.is(BlockTags.PLANKS) || s.is(BlockTags.WOOL)) {
            return false;
        }
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (Guhs.MODID.equals(id.getNamespace())) {
            return GUHS_NATUUR.contains(id.getPath());
        }
        return VANILLA_NATUUR.contains(s.getBlock()) || s.is(BlockTags.DIRT) || s.is(BlockTags.GRASS_BLOCKS) || s.is(BlockTags.SUBSTRATE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.SAPLINGS)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SMALL_FLOWERS) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE)
                || (s.is(BlockTags.REPLACEABLE) && s.getFluidState().isEmpty());
    }

    // ---- the finder --------------------------------------------------------------------------------------------------------

    /** A spot: x/z = middle, y = the ground (the first free block above the terrain), score 0..100 (higher = better). */
    public record Plek(BlockPos pos, int score, int verschil) {
    }

    /** Why a spot is not allowed (null = fine). */
    @Nullable
    public static String keur(ServerLevel level, BlockPos midden, int half) {
        int x0 = midden.getX() - half, z0 = midden.getZ() - half, x1 = midden.getX() + half, z1 = midden.getZ() + half;
        if (!geladen(level, x0, z0, x1, z1, true)) {
            return "terrein nog niet gegenereerd";
        }
        int[] h = hoogtes(level, x0, z0, x1, z1, 1);
        int min = h[0], max = h[1], y = h[2];
        if (max - min > MAX_VERSCHIL) {
            return "niet vlak genoeg (" + (max - min) + " blokken verschil)";
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0 - 2; x <= x1 + 2; x++) {
            for (int z = z0 - 2; z <= z1 + 2; z++) {
                for (int yy = min - 4; yy <= max + 12; yy++) {
                    m.set(x, yy, z);
                    BlockState s = level.getBlockState(m);
                    if (!s.getFluidState().isEmpty()) {
                        return "water of lava bij " + m.toShortString();
                    }
                    if (!natuur(s)) {
                        return "bouwsel (" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()) + ") bij " + m.toShortString();
                    }
                }
                if (((x - x0) & 3) == 0 && ((z - z0) & 3) == 0) {
                    m.set(x, y, z);
                    if (Bescherming.beschermd(level, m) || Protected.at(level, m)) {
                        return "beschermd gebied bij " + m.toShortString();
                    }
                }
            }
        }
        for (var huisje : nl.juiced.guhs.feature.huisje.Huisjes.rond(level, midden, half + 48)) {
            return "een Guhhuisje vlakbij (" + huisje.pos().toShortString() + ")";
        }
        return null;
    }

    /** min, max and median of the ground heights (first free block, leaves ignored) over the square, every {@code stap} blocks. */
    static int[] hoogtes(ServerLevel level, int x0, int z0, int x1, int z1, int stap) {
        List<Integer> hs = new ArrayList<>();
        for (int x = x0; x <= x1; x += stap) {
            for (int z = z0; z <= z1; z += stap) {
                hs.add(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
            }
        }
        hs.sort(Comparator.naturalOrder());
        return new int[]{hs.get(0), hs.get(hs.size() - 1), hs.get(hs.size() / 2)};
    }

    /** Are all chunks of the square there (generated; loaded on demand when {@code laden})? Never generates new terrain. */
    static boolean geladen(ServerLevel level, int x0, int z0, int x1, int z1, boolean laden) {
        NieuwTerrein t = NieuwTerrein.van(level);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (level.hasChunk(cx, cz)) {
                    continue;
                }
                if (!t.bestaat(cx, cz)) {
                    return false;
                }
                if (laden) {
                    level.getChunk(cx, cz);
                }
            }
        }
        return true;
    }

    /** The best (at most {@code n}) allowed spots within {@code straal} of {@code midden}, best first. */
    public static List<Plek> zoek(ServerLevel level, BlockPos midden, int straal, int n) {
        List<Plek> ruw = new ArrayList<>();
        for (int dx = -straal; dx <= straal; dx += 16) {
            for (int dz = -straal; dz <= straal; dz += 16) {
                if (dx * dx + dz * dz > straal * straal) {
                    continue;
                }
                int x = midden.getX() + dx, z = midden.getZ() + dz;
                if (!geladen(level, x - HALF, z - HALF, x + HALF, z + HALF, true)) {
                    continue;
                }
                int[] h = hoogtes(level, x - HALF, z - HALF, x + HALF, z + HALF, 4);
                int verschil = h[1] - h[0];
                if (verschil > MAX_VERSCHIL) {
                    continue;
                }
                double afstand = Math.sqrt(dx * dx + dz * dz) / Math.max(1, straal);
                int score = (int) Math.round(100 - 8 * verschil - 20 * afstand);
                ruw.add(new Plek(new BlockPos(x, h[2], z), score, verschil));
            }
        }
        ruw.sort(Comparator.comparingInt(Plek::score).reversed());
        List<Plek> uit = new ArrayList<>();
        for (Plek p : ruw) {
            if (uit.size() >= n) {
                break;
            }
            if (uit.stream().anyMatch(q -> Math.abs(q.pos().getX() - p.pos().getX()) < 2 * HALF && Math.abs(q.pos().getZ() - p.pos().getZ()) < 2 * HALF)) {
                continue;   // (overlaps a better one)
            }
            if (keur(level, p.pos(), HALF) == null) {
                int[] h = hoogtes(level, p.pos().getX() - HALF, p.pos().getZ() - HALF, p.pos().getX() + HALF, p.pos().getZ() + HALF, 1);
                uit.add(new Plek(new BlockPos(p.pos().getX(), h[2], p.pos().getZ()), p.score(), h[1] - h[0]));
            }
        }
        return uit;
    }

    // ---- placing -----------------------------------------------------------------------------------------------------------

    static Rotation rotatie(int draai) {
        return switch (((draai % 360) + 360) % 360 / 90) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Result of {@link #plaats}: null = placed, else why not. */
    @Nullable
    public static String plaats(ServerLevel level, BlockPos grond, int draai, boolean opnieuw) {
        MinecraftServer server = level.getServer();
        VasteTempel v = get(server);
        if (v.geregistreerd()) {
            if (!opnieuw) {
                return "er staat al een vaste tempel bij " + v.midden.toShortString() + " (" + v.dim + "); gebruik --opnieuw of eerst 'ongedaan'";
            }
            String f = ongedaan(server);
            if (f != null) {
                return "de oude tempel weghalen lukte niet: " + f;
            }
        }
        String reden = keur(level, grond, HALF);
        if (reden != null) {
            return "deze plek kan niet: " + reden;
        }
        StructureTemplate t = level.getStructureManager().get(TEMPLATE).orElse(null);
        if (t == null) {
            return "template " + TEMPLATE + " ontbreekt";
        }
        StructurePlaceSettings ps = new StructurePlaceSettings().setRotation(rotatie(draai)).setIgnoreEntities(true);
        BoundingBox nul = t.getBoundingBox(ps, BlockPos.ZERO);
        BlockPos oorsprong = new BlockPos(grond.getX() - (nul.minX() + nul.maxX()) / 2, grond.getY() - ONDER, grond.getZ() - (nul.minZ() + nul.maxZ()) / 2);
        BoundingBox doos = t.getBoundingBox(ps, oorsprong);
        BoundingBox bdoos = new BoundingBox(doos.minX(), doos.minY() - FUNDERING, doos.minZ(), doos.maxX(), doos.maxY() + BOVEN, doos.maxZ());
        // 1. the snapshot of everything that will change
        StructureTemplate kopie = new StructureTemplate();
        kopie.fillFromWorld(level, new BlockPos(bdoos.minX(), bdoos.minY(), bdoos.minZ()), new Vec3i(bdoos.getXSpan(), bdoos.getYSpan(), bdoos.getZSpan()),
                false, List.of());
        String naam = "vr_vaste_tempel_backup_" + System.currentTimeMillis() + ".nbt";
        try {
            Path map = server.getWorldPath(LevelResource.ROOT).resolve("data");
            Files.createDirectories(map);
            NbtIo.writeCompressed(kopie.save(new CompoundTag()), map.resolve(naam));
        } catch (IOException e) {
            LOGGER.error("[Vadsrijk] vaste tempel: backup schrijven mislukt", e);
            return "backup schrijven mislukt: " + e.getMessage();
        }
        // 2. clear hills and trees above the floor, fill small dips under it
        int vloer = grond.getY();
        BlockState top = level.getBlockState(new BlockPos(grond.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, grond.getX(), grond.getZ()) - 1, grond.getZ()));
        if (!top.isSolidRender() || top.getBlock() instanceof LeavesBlock) {
            top = Blocks.GRASS_BLOCK.defaultBlockState();
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = doos.minX(); x <= doos.maxX(); x++) {
            for (int z = doos.minZ(); z <= doos.maxZ(); z++) {
                for (int y = vloer; y <= bdoos.maxY(); y++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
                for (int y = vloer - 1, i = 0; i < FUNDERING; y--, i++) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (!s.isAir() && !s.canBeReplaced()) {
                        break;
                    }
                    level.setBlock(m, i == 0 ? top : Blocks.DIRT.defaultBlockState(), 2 | 16);
                }
            }
        }
        // 3. the temple itself (its portal sleeps, exactly like a worldgen one)
        t.placeInWorld(level, oorsprong, oorsprong, ps, level.getRandom(), 2);
        BlockPos portaal = null;
        List<StructureTemplate.StructureBlockInfo> poort = t.filterBlocks(oorsprong, ps, WereldRegistry.PORTAAL.get());
        if (!poort.isEmpty()) {
            long sx = 0, sz = 0;
            int minY = Integer.MAX_VALUE;
            for (var b : poort) {
                sx += b.pos().getX();
                sz += b.pos().getZ();
                minY = Math.min(minY, b.pos().getY());
            }
            portaal = new BlockPos((int) Math.round((double) sx / poort.size()), minY, (int) Math.round((double) sz / poort.size()));
        }
        // 4. register
        v.dim = level.dimension().identifier().toString();
        v.midden = grond.immutable();
        v.portaal = portaal;
        v.draai = draai;
        v.doos = doos;
        v.backupDoos = bdoos;
        v.backup = naam;
        v.tijd = System.currentTimeMillis();
        v.setDirty();
        LOGGER.info("[Vadsrijk] vaste ordetempel geplaatst in {} bij {} (draai {}), doos {}, portaal {}, backup data/{}", v.dim, grond.toShortString(),
                draai, doos, portaal, naam);
        return null;
    }

    /** Puts the snapshot back and forgets the fixed temple. Null = done, else why not. */
    @Nullable
    public static String ongedaan(MinecraftServer server) {
        VasteTempel v = get(server);
        if (!v.geregistreerd()) {
            return "er is geen vaste tempel";
        }
        ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, Identifier.parse(v.dim)));
        if (level == null) {
            return "dimensie " + v.dim + " bestaat niet";
        }
        Path f = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(v.backup);
        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            LOGGER.error("[Vadsrijk] vaste tempel: backup lezen mislukt", e);
            return "backup " + f + " lezen mislukt: " + e.getMessage();
        }
        StructureTemplate kopie = new StructureTemplate();
        kopie.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
        BoundingBox b = v.backupDoos;
        geladen(level, b.minX(), b.minZ(), b.maxX(), b.maxZ(), true);
        level.getEntitiesOfClass(nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh.class, net.minecraft.world.phys.AABB.of(b).inflate(4),
                g -> nl.juiced.guhs.feature.vadsrijk.npc.Werver.ID.equals(g.npcId())).forEach(net.minecraft.world.entity.Entity::discard);
        BlockPos min = new BlockPos(b.minX(), b.minY(), b.minZ());
        kopie.placeInWorld(level, min, min, new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true), level.getRandom(), 2 | 16);
        nl.juiced.guhs.feature.vadsrijk.npc.Werver.vergeet(v.doos);
        LOGGER.info("[Vadsrijk] vaste ordetempel bij {} ongedaan gemaakt (backup data/{} teruggezet)", v.midden.toShortString(), v.backup);
        v.dim = null;
        v.midden = null;
        v.portaal = null;
        v.doos = null;
        v.backupDoos = null;
        v.backup = "";
        v.setDirty();
        return null;
    }
}
