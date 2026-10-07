package nl.juiced.guhs.feature.einde2.biomes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2, slice biomes: {@code /guhs einde2 biomes kaart <biome> <straal>}, for looking at a biome without a client.
 * It finds the nearest spot on an island of that biome (from the ring where the outer islands begin), GENERATES the
 * chunks around it and writes what stands there to {@code <game dir>/einde2_biomes/<biome>.txt}: the top block of every
 * column (T), a cross-section through the middle (S) and how many of each guhs block there are (C);
 * tools/features/einde2_biomes_kaart.py draws a picture of it.
 * <p>
 * It makes new chunks, so it only exists in a dev run (never on the official world).
 */
public final class E2BiomesCommando {
    private E2BiomesCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        List<String> namen = Einde2Ids.BUITEN.stream().map(k -> k.identifier().getPath()).toList();
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("biomes")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("kaart").then(Commands.argument("biome", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(namen, b))
                        .then(Commands.argument("straal", IntegerArgumentType.integer(16, 96))
                                .executes(c -> kaart(c.getSource(), StringArgumentType.getString(c, "biome"), IntegerArgumentType.getInteger(c, "straal")))))))));
    }

    private static boolean land(ServerLevel level, ChunkGenerator generator, RandomState random, ResourceKey<Biome> biome, int x, int z) {
        return generator.getBiomeSource().getNoiseBiome(x >> 2, 16, z >> 2, random.sampler()).is(biome)
                && generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) > level.getMinY() + 12;
    }

    /** The nearest spot (walking outwards over rings from where the outer islands begin) well inside an island of this biome. */
    private static BlockPos zoek(ServerLevel level, ResourceKey<Biome> biome) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        for (int r = 1100; r <= 8000; r += 48) {
            int stappen = (int) (2 * Math.PI * r / 48);
            for (int i = 0; i < stappen; i++) {
                double a = 2 * Math.PI * i / stappen;
                int x = (int) (Math.cos(a) * r), z = (int) (Math.sin(a) * r);
                if (land(level, generator, random, biome, x, z) && land(level, generator, random, biome, x + 28, z) && land(level, generator, random, biome, x - 28, z)
                        && land(level, generator, random, biome, x, z + 28) && land(level, generator, random, biome, x, z - 28)) {
                    return new BlockPos(x, 0, z);
                }
            }
        }
        return null;
    }

    private static int kaart(CommandSourceStack source, String naam, int straal) {
        ServerLevel level = source.getServer().getLevel(GuheindeFeature.GUHEINDE);
        ResourceKey<Biome> biome = Einde2Ids.BUITEN.stream().filter(k -> k.identifier().getPath().equals(naam)).findFirst().orElse(null);
        BlockPos midden = level == null || biome == null ? null : zoek(level, biome);
        if (midden == null) {
            source.sendFailure(Component.literal("Geen eiland van " + naam + " gevonden."));
            return 0;
        }
        List<String> regels = new ArrayList<>();
        regels.add("M " + midden.getX() + " " + midden.getZ() + " " + straal + " " + naam);
        Map<String, Integer> aantal = new TreeMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -straal; dx <= straal; dx++) {
            for (int dz = -straal; dz <= straal; dz++) {
                int x = midden.getX() + dx, z = midden.getZ() + dz;
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                String biomeHier = level.getBiome(p.set(x, 64, z)).unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
                regels.add("T " + dx + " " + dz + " " + top + " " + (top < level.getMinY() ? "-" : id(level.getBlockState(p.set(x, top, z)))) + " " + biomeHier);
                for (int y = level.getMinY(); y < 128; y++) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (s.isAir()) {
                        continue;
                    }
                    String id = id(s);
                    if (id.startsWith(Guhs.MODID + ":") || !s.getFluidState().isEmpty()) {
                        aantal.merge(id, 1, Integer::sum);
                    }
                    if (dz == 0) {
                        regels.add("S " + dx + " " + y + " " + id);
                    }
                }
            }
        }
        aantal.forEach((id, n) -> regels.add("C " + id + " " + n));
        Path uit = FMLPaths.GAMEDIR.get().resolve("einde2_biomes").resolve(naam + ".txt");
        try {
            Files.createDirectories(uit.getParent());
            Files.write(uit, regels);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Schrijven mislukt: " + e));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Kaart van " + naam + " rond " + midden.getX() + ", " + midden.getZ() + " geschreven: " + uit + " " + aantal), false);
        return 1;
    }

    private static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
