package nl.juiced.guhs.feature.vadsrijk.kern.wereld;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * Het Vadsrijk: the hand-built map gets into the world here (CONTRACT_VADSRIJK 10). The biome {@code guhs:vadsrijk} places this
 * feature once per chunk (placed feature {@code guhs:vr_kaart}, no modifiers). It finds the 64x320x64 tile
 * {@code guhs:vadsrijk/tegels/t_<tx>_<tz>} that holds this chunk and places only the part inside the chunk. No tile:
 * {@link Ondergrond#vul} (void, except kern's proeftuin).
 * <p>Deterministic, only new chunks, worldgen thread only. Entities in tiles are skipped (NPCs/mobs are code).
 */
public class KaartFeature extends Feature<NoneFeatureConfiguration> {
    public static final int TEGEL = 64, HOOGTE = 320;
    /** Tiles known to be missing (no file), so we don't ask the template manager 16 times per tile. */
    private static final Map<Long, Boolean> GEEN = new ConcurrentHashMap<>();
    /**
     * Template memory (integration 1.10.0, was wereld's TegelGeheugen): vanilla's StructureTemplateManager keeps every loaded
     * template forever (690 tiles, ~12.5 M blocks). A tile is forgotten as soon as all 16 of its chunks are placed; tiles that
     * stay half-generated are forgotten by {@link #ruimOp} after 2 minutes without use (reloading one costs ~10 ms).
     */
    private static final Map<Long, java.util.Set<Long>> GEPLAATST = new ConcurrentHashMap<>();
    private static final Map<Long, Long> LAATST = new ConcurrentHashMap<>();

    public KaartFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static Identifier tegelId(int tx, int tz) {
        return Guhs.id("vadsrijk/tegels/t_" + tx + "_" + tz);
    }

    /**
     * U-GROOTTE: the tile at (tx, tz): from the compact pack {@code data/guhs/vadsrijk/kaart.gpak} (the jar ships only that),
     * else (no pack, e.g. a tree built without the kaartPak task) from the structure manager's .nbt. Empty = no tile there.
     */
    public static Optional<StructureTemplate> tegel(ServerLevel sl, int tx, int tz) {
        Optional<StructureTemplate> t = nl.juiced.guhs.feature.vadsrijk.grootte.KaartPakLader.tegel(sl.getServer(), tx, tz);
        return t != null ? t : sl.getStructureManager().get(tegelId(tx, tz));
    }

    /** U-GROOTTE: drops a tile from memory (pack cache and structure manager). */
    public static void vergeet(ServerLevel sl, int tx, int tz) {
        nl.juiced.guhs.feature.vadsrijk.grootte.KaartPakLader.vergeet(tx, tz);
        sl.getStructureManager().remove(tegelId(tx, tz));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cp = ChunkPos.containing(ctx.origin());
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        int tx = Math.floorDiv(x0, TEGEL), tz = Math.floorDiv(z0, TEGEL);
        long sleutel = ((long) tx << 32) | (tz & 0xFFFFFFFFL);
        StructureTemplate tegel = null;
        if (!GEEN.containsKey(sleutel)) {
            ServerLevel sl = level.getLevel();
            Optional<StructureTemplate> t = tegel(sl, tx, tz);   // U-GROOTTE: from the compact pack (or the .nbt)
            if (t.isPresent()) {
                tegel = t.get();
            } else {
                GEEN.put(sleutel, Boolean.TRUE);
            }
        }
        if (tegel == null) {
            Ondergrond.vul(level, cp);
            return true;
        }
        BoundingBox chunk = new BoundingBox(x0, level.getMinY(), z0, x0 + 15, level.getMaxY(), z0 + 15);
        StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(chunk).setIgnoreEntities(true).setKnownShape(true);
        BlockPos hoek = new BlockPos(tx * TEGEL, 0, tz * TEGEL);
        if (nl.juiced.guhs.feature.vadsrijk.indeling.ZeeVulling.zeeOnderTegel(cp)) {   // T-WERELD: sea under Hoogvads' tiles
            nl.juiced.guhs.feature.vadsrijk.indeling.ZeeVulling.vul(level, cp);
        }
        tegel.placeInWorld(level, hoek, hoek, s, ctx.random(), 2);
        LAATST.put(sleutel, System.currentTimeMillis());
        java.util.Set<Long> klaar = GEPLAATST.computeIfAbsent(sleutel, k -> ConcurrentHashMap.newKeySet());
        klaar.add(cp.pack());
        if (klaar.size() >= (TEGEL / 16) * (TEGEL / 16)) {
            GEPLAATST.remove(sleutel);
            LAATST.remove(sleutel);
            vergeet(level.getLevel(), tx, tz);
        }
        return true;
    }

    /** Forgets the tiles not used for 2 minutes (call now and then from the server thread, e.g. once a minute). */
    public static void ruimOp(ServerLevel sl) {
        long grens = System.currentTimeMillis() - 120_000L;
        for (var e : LAATST.entrySet()) {
            if (e.getValue() < grens) {
                long k = e.getKey();
                LAATST.remove(k);
                vergeet(sl, (int) (k >> 32), (int) k);
            }
        }
    }
}
