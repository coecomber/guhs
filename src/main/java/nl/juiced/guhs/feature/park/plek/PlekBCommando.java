package nl.juiced.guhs.feature.park.plek;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.wereld.Bezetting;

/**
 * 1.4.2 (park-plekjes b): op commands (moderation, dev checks, AutoCheck scripts):
 * <ul>
 *   <li>{@code /guhs parkb wensen wis}: takes every wish down from the wish tree nearest to you (within 16 blocks);</li>
 *   <li>{@code /guhs parkb mika terug}: every sulking Mika that left today comes back (Bezetting brings him when a player is
 *       near);</li>
 *   <li>{@code /guhs parkb toren}: the park you stand in: its uitkijktoren (where) and the height of every plekje spot.</li>
 * </ul>
 */
final class PlekBCommando {
    private PlekBCommando() {
    }

    static void registreer(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("parkb").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("wensen").then(Commands.literal("wis").executes(PlekBCommando::wensenWis)))
                .then(Commands.literal("mika").then(Commands.literal("terug").executes(PlekBCommando::mikaTerug)))
                .then(Commands.literal("toren").executes(PlekBCommando::toren))));
    }

    private static int wensenWis(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos bij = BlockPos.containing(c.getSource().getPosition());
        BlockPos stam = null;
        for (BlockPos p : BlockPos.withinManhattan(bij, 16, 8, 16)) {
            if (level.getBlockState(p).getBlock() instanceof Wensboom.Stam) {
                stam = p.immutable();
                break;
            }
        }
        if (stam == null) {
            c.getSource().sendFailure(Component.literal("Geen wensboom binnen 16 blokken."));
            return 0;
        }
        BlockPos boom = Wensboom.boom(level, stam);
        int n = Wensboom.Wensen.get(level).haalWeg(level, boom, w -> true);
        for (BlockPos p : Wensboom.plekjes(level, boom)) {
            if (!level.getBlockState(p).isAir()) {
                level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());   // (the examples too)
            }
        }
        int weg = n;
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.parkb.wens.gewist", weg), true);
        return n;
    }

    private static int mikaTerug(CommandContext<CommandSourceStack> c) {
        int n = MikaBankje.Weg.get(c.getSource().getLevel()).allesTerug();
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.parkb.mika.terug", n), true);
        return n;
    }

    private static int toren(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        StructureStart start = Bezetting.start(level, ParkFeature.STRUCTUUR, BlockPos.containing(c.getSource().getPosition()));
        if (start == null) {
            c.getSource().sendFailure(Component.literal("Geen Knabbelpark hier."));
            return 0;
        }
        List<String> torens = new java.util.ArrayList<>();
        int toren = Integer.MIN_VALUE, hoogste = Integer.MIN_VALUE;
        for (StructurePiece piece : start.getPieces()) {
            var id = PleinSlot.template(piece);
            if (id == null || !Uitkijktoren.plekje(piece, level.getStructureManager())) {
                continue;
            }
            hoogste = Math.max(hoogste, piece.getBoundingBox().minY());
            if (id.getPath().equals(Uitkijktoren.TOREN)) {
                torens.add(piece.getBoundingBox().getCenter().toShortString());
                toren = piece.getBoundingBox().minY();
            }
        }
        String t = String.join("; ", torens);
        int a = toren, b = hoogste;
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.parkb.cmd.toren", t.isEmpty() ? "-" : t, a == Integer.MIN_VALUE ? "-" : a,
                b == Integer.MIN_VALUE ? "-" : b), false);
        return torens.size();
    }
}
