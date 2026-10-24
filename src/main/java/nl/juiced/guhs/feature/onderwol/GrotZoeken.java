package nl.juiced.guhs.feature.onderwol;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Op command (speeltest 3): {@code /guhs grot <naam>} finds the nearest cave biome of that kind in the Guhmension and
 * takes you there (into its cave air). Vanilla's {@code /locate biome} only looks at a few heights (your own y, +-64), so
 * it misses the thin bands the cave biomes live in; this one samples each biome's own band.
 */
public final class GrotZoeken {
    /** Cave biome -> the heights its band is sampled at. */
    private static final Map<String, int[]> BANDEN = new LinkedHashMap<>();
    static {
        BANDEN.put("glitterspelonk", new int[]{30, 36, 42});
        BANDEN.put("pluisgrot", new int[]{50, 56});
        BANDEN.put("slaapgrot", new int[]{50, 56});
        BANDEN.put("knuffelwortelgrot", new int[]{52, 58});
        BANDEN.put("belletjesgrot", new int[]{30, 36, 42});
        BANDEN.put("kristalpegelgrot", new int[]{14, 22, 30, 38});
        BANDEN.put("gatenkaasgrotten", new int[]{8, 20, 32, 44, 56});
        BANDEN.put("guh_kristalmijn", new int[]{8, 20, 32, 44, 56});
    }
    private static final int STAP = 32;
    private static final int BEREIK = 4096;

    private GrotZoeken() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("grot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(BANDEN.keySet(), b))
                        .executes(ctx -> zoek(ctx.getSource(), StringArgumentType.getString(ctx, "naam"))))));
    }

    private static int zoek(CommandSourceStack source, String naam) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int[] band = BANDEN.get(naam);
        ServerLevel level = source.getServer().getLevel(ModDimensions.GUHMENSION);
        if (band == null || level == null) {
            source.sendFailure(Component.literal("Onbekende grot: " + naam));
            return 0;
        }
        BiomeSource biomes = level.getChunkSource().getGenerator().getBiomeSource();
        RandomState random = level.getChunkSource().randomState();
        Climate.Sampler sampler = random.sampler();
        BlockPos van = player.level() == level ? player.blockPosition() : BlockPos.ZERO;
        // outward in rings of STAP: the first hit is (about) the nearest
        for (int r = 0; r <= BEREIK; r += STAP) {
            for (int dx = -r; dx <= r; dx += STAP) {
                for (int dz = -r; dz <= r; dz += STAP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = van.getX() + dx, z = van.getZ() + dz;
                    for (int y : band) {
                        Holder<Biome> b = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
                        if (b.is(Guhs.id(naam))) {
                            return gevonden(source, player, level, naam, new BlockPos(x, y, z), van);
                        }
                    }
                }
            }
        }
        source.sendFailure(Component.literal("Geen " + naam + " binnen " + BEREIK + " blokken"));
        return 0;
    }

    private static int gevonden(CommandSourceStack source, ServerPlayer player, ServerLevel level, String naam, BlockPos plek, BlockPos van) {
        // into the cave air near the spot (the biome lies in the rock too: look around a little)
        BlockPos doel = null;
        for (int d = 0; d <= 12 && doel == null; d++) {
            for (BlockPos p : BlockPos.betweenClosed(plek.offset(-d, -d, -d), plek.offset(d, d, d))) {
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBiome(p).is(Guhs.id(naam))) {
                    doel = p.immutable();
                    break;
                }
            }
        }
        BlockPos naar = doel != null ? doel : plek;
        player.teleportTo(level, naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        int afstand = (int) Math.sqrt(naar.distSqr(van));
        boolean inRots = doel == null;
        source.sendSuccess(() -> Component.literal(naam + " op " + naar.toShortString() + " (" + afstand + " blokken)"
                + (inRots ? ", nog in de rots: graaf even of vlieg in spectator" : "")), false);
        return 1;
    }
}
