package nl.juiced.guhs.feature.zingendeweide;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for De Zingende Weide (permission level 2; Brigadier merges them into the /guhs tree), see TESTEN_WEIDE.md:
 * <ul>
 *   <li>{@code /guhs weide tp}: to the nearest Zingende Weide (in the Guhmension);</li>
 *   <li>{@code /guhs weide plaats <wat>}: one of the builds or shapes in front of you (koorkom, poeltjes, guhwand, boog,
 *       pilaar, reuzenboom, kristallen, poel, holletje, kloof, schildrots, boompje).</li>
 * </ul>
 */
public final class WeideCommando {
    static final List<String> WAT = List.of("koorkom", "poeltjes", "guhwand", "boog", "pilaar", "reuzenboom", "kristallen", "poel", "holletje", "kloof",
            "schildrots", "boompje");

    private WeideCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("weide").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("tp").executes(c -> tp(c.getSource())))
                .then(Commands.literal("plaats").then(Commands.argument("wat", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WAT, b))
                        .executes(c -> plaats(c.getSource(), StringArgumentType.getString(c, "wat")))))));
    }

    private static int tp(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        Pair<BlockPos, Holder<Biome>> gevonden = level.findClosestBiome3d(b -> b.is(ZingendeWeideFeature.ZINGENDE_WEIDE), player.blockPosition(), 12800, 32, 64);
        if (gevonden == null) {
            source.sendFailure(Component.translatable("gui.guhs.weide.tp.geen"));
            return 0;
        }
        BlockPos p = gevonden.getFirst();
        level.getChunk(p.getX() >> 4, p.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
        player.teleportTo(level, p.getX() + 0.5, y + 1, p.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        source.sendSuccess(() -> Component.translatable("gui.guhs.weide.tp", p.getX(), p.getZ()), false);
        return 1;
    }

    static WeideTeken.Zet zet(ServerLevel level) {
        return new WeideTeken.Zet() {
            @Override
            public void zet(int x, int y, int z, BlockState state) {
                BlockPos p = new BlockPos(x, y, z);
                level.setBlock(p, state, Block.UPDATE_CLIENTS);
                if (!state.getFluidState().isEmpty()) {
                    level.scheduleTick(p, state.getFluidState().getType(), 1);
                }
            }

            @Override
            public BlockState get(int x, int y, int z) {
                return level.getBlockState(new BlockPos(x, y, z));
            }
        };
    }

    private static int plaats(CommandSourceStack source, String wat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        Direction kijk = player.getDirection();
        int afstand = switch (wat) {
            case "koorkom", "poeltjes", "guhwand" -> 26;
            case "boog", "reuzenboom" -> 16;
            default -> 8;
        };
        int x = player.getBlockX() + kijk.getStepX() * afstand, z = player.getBlockZ() + kijk.getStepZ() * afstand;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        RandomSource r = level.getRandom();
        WeideTeken.Zet zet = zet(level);
        switch (wat) {
            case "koorkom", "poeltjes" -> WeideBouw.bouw(wat, zet, (a, c) -> level.getHeight(Heightmap.Types.WORLD_SURFACE, a, c), r, x, y, z, kijk,
                    pos -> WeideBouw.badguh(level, pos));
            case "guhwand" -> WeideBouw.bouw(wat, zet, (a, c) -> level.getHeight(Heightmap.Types.WORLD_SURFACE, a, c), r, x, y, z, kijk.getOpposite(), pos -> {
            });
            case "boog" -> WeideTeken.boog(zet, r, x, y, z, 16, 12, kijk.getAxis() == Direction.Axis.Z);
            case "pilaar" -> WeideTeken.pilaar(zet, r, x, y, z, 20 + r.nextInt(20), 2.5, (r.nextDouble() - 0.5) * 4, (r.nextDouble() - 0.5) * 4, 4, true, true);
            case "reuzenboom" -> WeideTeken.reuzenboom(zet, r, x, y, z, 24, true);
            case "kristallen" -> WeideTeken.kristallen(zet, r, x, y, z);
            case "poel" -> WeideTeken.poel(zet, r, x, y, z, 5, 4, 2);
            case "holletje" -> WeideTeken.holletje(zet, r, x, y, z, kijk.getOpposite());
            case "kloof" -> WeideTeken.kloof(zet, r, x, y, z, kijk.getClockWise(), 26);
            case "schildrots" -> WeideTeken.schildrots(zet, r, x, y, z);
            case "boompje" -> WeideTeken.boompje(zet, r, x, y, z);
            default -> {
                source.sendFailure(Component.literal("? " + String.join(", ", WAT)));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.translatable("gui.guhs.weide.plaats", wat), false);
        return 1;
    }
}
