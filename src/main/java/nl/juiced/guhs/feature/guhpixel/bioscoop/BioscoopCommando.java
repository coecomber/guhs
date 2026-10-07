package nl.juiced.guhs.feature.guhpixel.bioscoop;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guhpixel.Films;

/**
 * Dev commands of the Guhbioscoop (gamemasters): {@code /guhs px bioscoop zaal} (builds a little cinema in front of you:
 * a 7 x 4 screen, a projector on a pillar, four seats, a popcornmachine), {@code speel <film>} (plays a film on the
 * nearest projector, whether you have the film or not), {@code spoel <seconden>} (skips forward), {@code stop}, {@code gezien wis|alles} (what you watched).
 * Unlocking films themselves is the kern's {@code /guhs px film <id>|alles|wis}.
 */
public final class BioscoopCommando {
    static void register(RegisterCommandsEvent event) {
        var bioscoop = Commands.literal("bioscoop");
        bioscoop.then(Commands.literal("zaal").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BlockPos projector = bouwZaal(p.level(), p.blockPosition(), p.getDirection());
            return zeg(ctx.getSource(), "zaaltje gebouwd; de projector staat op " + projector.toShortString());
        }));
        bioscoop.then(Commands.literal("speel").then(Commands.argument("film", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Films.IDS, b))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String film = StringArgumentType.getString(ctx, "film");
                    ServerLevel level = p.level();
                    FilmInfo info = FilmInfo.van(level.getServer(), film);
                    ProjectorBlockEntity be = dichtstbij(level, p.blockPosition());
                    if (info == null || be == null) {
                        ctx.getSource().sendFailure(Component.literal(info == null ? "onbekende film: " + film : "geen projector binnen 16 blokken"));
                        return 0;
                    }
                    if (!Bioscoop.start(level, be, info)) {
                        ctx.getSource().sendFailure(Component.literal("de projector ziet geen Bioscoopdoek"));
                        return 0;
                    }
                    return zeg(ctx.getSource(), film + " draait (" + info.duur() / 20 + " s) op " + be.getBlockPos().toShortString());
                })));
        bioscoop.then(Commands.literal("spoel").then(Commands.argument("seconden", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 120))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ProjectorBlockEntity be = dichtstbij(p.level(), p.blockPosition());
                    if (be == null || !be.speelt()) {
                        ctx.getSource().sendFailure(Component.literal("er draait geen film binnen 16 blokken"));
                        return 0;
                    }
                    be.spoel(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "seconden") * 20);
                    return zeg(ctx.getSource(), "doorgespoeld");
                })));
        bioscoop.then(Commands.literal("stop").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ProjectorBlockEntity be = dichtstbij(p.level(), p.blockPosition());
            if (be == null) {
                ctx.getSource().sendFailure(Component.literal("geen projector binnen 16 blokken"));
                return 0;
            }
            be.stop(p.level(), false);
            return zeg(ctx.getSource(), "projector uit");
        }));
        bioscoop.then(Commands.literal("gezien")
                .then(Commands.literal("wis").executes(ctx -> {
                    Bioscoop.wisGezien(ctx.getSource().getPlayerOrException());
                    return zeg(ctx.getSource(), "niks meer gezien");
                }))
                .then(Commands.literal("alles").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Films.IDS.forEach(f -> Bioscoop.markeerGezien(p, f));
                    return zeg(ctx.getSource(), "gezien: " + Bioscoop.aantalGezien(p) + " / " + Films.IDS.size());
                })));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(bioscoop)));
    }

    private static int zeg(CommandSourceStack source, String tekst) {
        source.sendSuccess(() -> Component.literal("[bioscoop] " + tekst), false);
        return 1;
    }

    /** The nearest projector within 16 blocks. */
    @Nullable
    static ProjectorBlockEntity dichtstbij(ServerLevel level, BlockPos rond) {
        ProjectorBlockEntity beste = null;
        double besteD = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(rond.offset(-16, -6, -16), rond.offset(16, 6, 16))) {
            if (level.isLoaded(q) && level.getBlockState(q).getBlock() instanceof ProjectorBlock && level.getBlockEntity(q) instanceof ProjectorBlockEntity be) {
                double d = q.distSqr(rond);
                if (d < besteD) {
                    beste = be;
                    besteD = d;
                }
            }
        }
        return beste;
    }

    /**
     * A little cinema, seen from {@code hier} looking towards {@code kijk}: the screen (7 x 4) eleven blocks ahead and one
     * block above the floor, the projector two blocks ahead on a pillar, four seats in a row in between, a popcornmachine
     * at the side. Returns the projector's position.
     */
    public static BlockPos bouwZaal(ServerLevel level, BlockPos hier, Direction kijk) {
        Direction rechts = kijk.getClockWise();
        for (int a = -3; a <= 3; a++) {
            for (int b = 0; b < Doek.MAX_H; b++) {
                level.setBlock(hier.relative(kijk, 11).relative(rechts, a).above(1 + b),
                        BioscoopSlice.DOEK.get().defaultBlockState().setValue(DoekBlock.FACING, kijk.getOpposite()), 3);
            }
        }
        for (int a : new int[]{-2, -1, 1, 2}) {
            level.setBlock(hier.relative(kijk, 6).relative(rechts, a), BioscoopSlice.STOELTJE.get().defaultBlockState().setValue(StoeltjeBlock.FACING, kijk), 3);
        }
        BlockPos paal = hier.relative(kijk, 2);
        level.setBlock(paal, Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
        level.setBlock(paal.above(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
        BlockPos projector = paal.above(2);
        level.setBlock(projector, BioscoopSlice.PROJECTOR.get().defaultBlockState().setValue(ProjectorBlock.FACING, kijk), 3);
        level.setBlock(hier.relative(kijk, 5).relative(rechts, 4), BioscoopSlice.POPCORNMACHINE.get().defaultBlockState()
                .setValue(PopcornmachineBlock.FACING, kijk.getOpposite()), 3);
        return projector.immutable();
    }

    private BioscoopCommando() {
    }
}
