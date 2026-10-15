package nl.juiced.guhs.feature.bio.bouwmeer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * DEV ({@code /guhs bio bouw-meer tel <straal>}): how many botenhuisjes and picknickeilandjes does each lake get? Nothing
 * is generated: the lakes are found by asking the biome source on a grid of 32 blocks (touching cells are one lake), the
 * buildings by asking each structure set's candidate chunks the way the chunk generator would. The answer goes to the
 * log and to {@code bio_bouw_meer_tel.txt} in the server directory. This is how the spacing of the two sets was chosen.
 */
final class Telling {
    private static final int RASTER = 32;
    /** A building this far (blocks) outside a lake's box still belongs to it (the shore lies in the valley floor). */
    private static final int RAND = 40;

    private record Meer(int cellen, int minX, int minZ, int maxX, int maxZ) {
        int middenX() {
            return (minX + maxX) / 2;
        }

        int middenZ() {
            return (minZ + maxZ) / 2;
        }
    }

    static List<String> tel(ServerLevel level, BlockPos midden, int straal) {
        ServerChunkCache chunks = level.getChunkSource();
        ChunkGenerator generator = chunks.getGenerator();
        var sampler = chunks.randomState().sampler();
        // --- the lakes ---
        int n = straal * 2 / RASTER + 1, x0 = midden.getX() - straal, z0 = midden.getZ() - straal;
        int[] groep = new int[n * n];
        java.util.Arrays.fill(groep, -1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x0 + i * RASTER), QuartPos.fromBlock(64), QuartPos.fromBlock(z0 + j * RASTER), sampler)
                        .is(Bio.BLOESEMMEERTJE)) {
                    groep[i * n + j] = i * n + j;
                }
            }
        }
        boolean veranderd = true;
        while (veranderd) {                       // (touching cells take the lowest number: a small grid, a few passes)
            veranderd = false;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int g = groep[i * n + j];
                    if (g < 0) {
                        continue;
                    }
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int a = i + di, b = j + dj;
                            if (a >= 0 && b >= 0 && a < n && b < n && groep[a * n + b] >= 0 && groep[a * n + b] < g) {
                                g = groep[a * n + b];
                            }
                        }
                    }
                    if (g != groep[i * n + j]) {
                        groep[i * n + j] = g;
                        veranderd = true;
                    }
                }
            }
        }
        Map<Integer, int[]> boxen = new HashMap<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int g = groep[i * n + j];
                if (g < 0) {
                    continue;
                }
                int x = x0 + i * RASTER, z = z0 + j * RASTER;
                int[] b = boxen.computeIfAbsent(g, k -> new int[]{0, x, z, x, z});
                b[0]++;
                b[1] = Math.min(b[1], x);
                b[2] = Math.min(b[2], z);
                b[3] = Math.max(b[3], x);
                b[4] = Math.max(b[4], z);
            }
        }
        List<Meer> meren = new ArrayList<>();
        for (int[] b : boxen.values()) {
            // (a lake cut off by the edge of the search is left out)
            if (b[1] > x0 + RASTER && b[2] > z0 + RASTER && b[3] < x0 + (n - 2) * RASTER && b[4] < z0 + (n - 2) * RASTER) {
                meren.add(new Meer(b[0], b[1], b[2], b[3], b[4]));
            }
        }
        meren.sort(java.util.Comparator.comparingInt((Meer m) -> -m.cellen()));
        // --- the buildings ---
        List<String> uit = new ArrayList<>();
        Map<String, List<BlockPos>> starts = new HashMap<>();
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        long seed = level.getSeed();
        for (String id : List.of("botenhuisje", "picknickeilandje")) {
            List<BlockPos> gevonden = new ArrayList<>();
            starts.put(id, gevonden);
            StructureSet set = sets.getValue(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(id)));
            if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
                uit.add(id + ": no random set");
                continue;
            }
            Holder<Structure> holder = set.structures().get(0).structure();
            int lo = (x0 >> 4), loZ = (z0 >> 4), hi = ((x0 + 2 * straal) >> 4), hiZ = ((z0 + 2 * straal) >> 4);
            for (int rx = Math.floorDiv(lo, spread.spacing()); rx <= Math.floorDiv(hi, spread.spacing()); rx++) {
                for (int rz = Math.floorDiv(loZ, spread.spacing()); rz <= Math.floorDiv(hiZ, spread.spacing()); rz++) {
                    ChunkPos c = spread.getPotentialStructureChunk(seed, rx * spread.spacing(), rz * spread.spacing());
                    if (!spread.isStructureChunk(chunks.getGeneratorState(), c.x(), c.z())) {
                        continue;
                    }
                    StructureStart start;
                    try {
                        start = holder.value().generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(), chunks.randomState(),
                                level.getStructureManager(), seed, c, 0, level, holder.value().biomes()::contains);
                    } catch (RuntimeException e) {
                        continue;
                    }
                    if (start.isValid()) {
                        gevonden.add(start.getBoundingBox().getCenter());
                    }
                }
            }
            uit.add(id + ": " + gevonden.size() + " starts within " + straal + " blocks of " + midden.getX() + " " + midden.getZ() + " (set: spacing " + spread.spacing()
                    + ", separation " + spread.separation() + ")");
        }
        // --- per lake ---
        int[] steigers = new int[5];
        int metPicknick = 0, groot = 0, losSteiger = 0, losPicknick = 0;
        List<String> regels = new ArrayList<>();
        Map<BlockPos, Boolean> gebruikt = new HashMap<>();
        for (Meer m : meren) {
            int s = 0, p = 0;
            for (BlockPos b : starts.get("botenhuisje")) {
                if (b.getX() >= m.minX() - RAND && b.getX() <= m.maxX() + RAND && b.getZ() >= m.minZ() - RAND && b.getZ() <= m.maxZ() + RAND && gebruikt.putIfAbsent(b, true) == null) {
                    s++;
                }
            }
            for (BlockPos b : starts.get("picknickeilandje")) {
                if (b.getX() >= m.minX() - RAND && b.getX() <= m.maxX() + RAND && b.getZ() >= m.minZ() - RAND && b.getZ() <= m.maxZ() + RAND && gebruikt.putIfAbsent(b, true) == null) {
                    p++;
                }
            }
            if (m.cellen() >= 6) {
                groot++;
                steigers[Math.min(4, s)]++;
                metPicknick += p > 0 ? 1 : 0;
            }
            regels.add("  lake " + m.middenX() + " " + m.middenZ() + " (" + m.cellen() + " cells, " + (m.maxX() - m.minX() + RASTER) + " x " + (m.maxZ() - m.minZ() + RASTER)
                    + "): " + s + " botenhuisje, " + p + " picknickeilandje");
        }
        for (BlockPos b : starts.get("botenhuisje")) {
            losSteiger += gebruikt.containsKey(b) ? 0 : 1;
        }
        for (BlockPos b : starts.get("picknickeilandje")) {
            losPicknick += gebruikt.containsKey(b) ? 0 : 1;
        }
        uit.add("lakes (whole, within the search): " + meren.size() + ", of which " + groot + " of 6 cells or more (about 80 x 80 and up)");
        uit.add("botenhuisjes per lake of 6+ cells: none " + steigers[0] + ", one " + steigers[1] + ", two " + steigers[2] + ", three " + steigers[3] + ", four or more " + steigers[4]);
        uit.add("lakes of 6+ cells with a picknickeilandje: " + metPicknick + " of " + groot);
        uit.add("not at a whole lake (edge of the search, or a shore far up a river mouth): " + losSteiger + " botenhuisjes, " + losPicknick + " picknickeilandjes");
        List<String> alles = new ArrayList<>(uit);
        alles.addAll(regels);
        for (String id : List.of("botenhuisje", "picknickeilandje")) {
            StringBuilder b = new StringBuilder(id + " at:");
            for (BlockPos p : starts.get(id)) {
                b.append(' ').append(p.getX()).append(',').append(p.getZ());
            }
            alles.add(b.toString());
        }
        try {
            Files.write(Path.of("bio_bouw_meer_tel.txt"), alles);
        } catch (IOException e) {
            uit.add("tel: " + e);
        }
        return uit;
    }

    private Telling() {
    }
}
