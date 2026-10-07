package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Dev commands of the Guh-parkour (gamemasters, {@code /guhs px parkour ...}):
 * {@code baan} builds a whole demo course in front of you and lays out its route (start, horde, springplank, slalom,
 * kruiptunnel, evenwichtsbalk, knabbeltafeltje, finish, a scorebord) · {@code guh [n]} spawns n tame guhs of yours and puts
 * them on the nearest Startpaaltje · {@code scherm} opens the screen of the nearest Startpaaltje · {@code stand} prints
 * its route, guhs and scores.
 */
public final class ParkourCommando {
    /** Where each piece of the demo course stands (blocks from the start along the course). */
    public static final int HORDE = 3, SPRINGPLANK = 6, SLALOM = 12, KRUIPTUNNEL = 17, BALK = 22, TAFEL = 26, FINISH = 29;

    static void register(RegisterCommandsEvent event) {
        var parkour = Commands.literal("parkour").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        parkour.then(Commands.literal("baan").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Direction d = p.getDirection();
            StartpaalBlockEntity paal = proefbaan((ServerLevel) p.level(), p.blockPosition().relative(d, 2), d, p);
            meld(ctx.getSource(), "Guh-parkour demo course built: " + (paal == null ? "FAILED" : paal.stukken().size() + " pieces at " + paal.getBlockPos().toShortString()));
            return paal == null ? 0 : 1;
        }));
        parkour.then(Commands.literal("guh").executes(ctx -> guhs(ctx.getSource(), 1))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, Routes.MAX_GUHS)).executes(ctx -> guhs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "n")))));
        parkour.then(Commands.literal("scherm").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            StartpaalBlockEntity paal = dichtstbij(p);
            if (paal == null) {
                meld(ctx.getSource(), "No Startpaaltje within " + Routes.BEREIK + " blocks");
                return 0;
            }
            p.snapTo(paal.getBlockPos().getX() + 0.5, paal.getBlockPos().getY(), paal.getBlockPos().getZ() + 2.5);
            ParkourPayloads.open(p, paal);
            return 1;
        }));
        parkour.then(Commands.literal("stand").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            StartpaalBlockEntity paal = dichtstbij(p);
            if (paal == null) {
                meld(ctx.getSource(), "No Startpaaltje within " + Routes.BEREIK + " blocks");
                return 0;
            }
            meld(ctx.getSource(), "Startpaaltje " + paal.getBlockPos().toShortString() + ": " + paal.stukken().size() + " pieces, " + paal.guhs().size() + " guhs");
            for (Map.Entry<UUID, StartpaalBlockEntity.Score> e : paal.scores().entrySet()) {
                StartpaalBlockEntity.Score s = e.getValue();
                meld(ctx.getSource(), "  " + s.naam().getString() + ": " + s.rondjes() + " laps, best " + s.beste() + " ticks, last " + s.laatste() + " ticks");
            }
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(parkour)));
    }

    private static void meld(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal(tekst), false);
    }

    @Nullable
    private static StartpaalBlockEntity dichtstbij(ServerPlayer p) {
        List<StartpaalBlockEntity> palen = StartpaalBlockEntity.palen((ServerLevel) p.level(), p.blockPosition());
        return palen.isEmpty() ? null : palen.get(0);
    }

    private static int guhs(CommandSourceStack bron, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = bron.getPlayerOrException();
        StartpaalBlockEntity paal = dichtstbij(p);
        if (paal == null) {
            meld(bron, "No Startpaaltje within " + Routes.BEREIK + " blocks");
            return 0;
        }
        int op = 0;
        for (int i = 0; i < n; i++) {
            GuhEntity guh = ModEntities.GUH.get().create(p.level(), EntitySpawnReason.COMMAND);
            if (guh == null) {
                break;
            }
            BlockPos bij = paal.getBlockPos().relative(Direction.from2DDataValue(i), 2);
            guh.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5, 0, 0);
            guh.tame(p);
            p.level().addFreshEntity(guh);
            if (paal.zetOp(guh)) {
                op++;
            }
        }
        meld(bron, op + " guh(s) put on the route of " + paal.getBlockPos().toShortString());
        return op;
    }

    /**
     * Builds the demo course from start along richting (flat ground expected: a floor of pink concrete is laid where there
     * is none) and lays out its route. Also the course of the game tests.
     */
    @Nullable
    public static StartpaalBlockEntity proefbaan(ServerLevel level, BlockPos start, Direction richting, @Nullable Player eigenaar) {
        Direction naarStart = richting.getOpposite();
        for (int i = -1; i <= FINISH + 1; i++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos p = start.relative(richting, i).relative(richting.getClockWise(), z);
                if (level.getBlockState(p.below()).isAir()) {
                    level.setBlock(p.below(), Blocks.PINK_CONCRETE.defaultBlockState(), 3);
                }
                for (int y = 0; y < 3; y++) {
                    if (!level.getBlockState(p.above(y)).isAir()) {
                        level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        level.setBlock(start, ParkourSlice.STARTPAALTJE.get().defaultBlockState().setValue(StartpaalBlock.FACING, naarStart), 3);
        if (!(level.getBlockEntity(start) instanceof StartpaalBlockEntity paal)) {
            return null;
        }
        if (eigenaar != null) {
            paal.zetEigenaar(eigenaar);
        }
        Obstakel[] soorten = {Obstakel.HORDE, Obstakel.SPRINGPLANK, Obstakel.SLALOMPAALTJES, Obstakel.KRUIPTUNNEL, Obstakel.EVENWICHTSBALK,
                Obstakel.KNABBELTAFELTJE};
        int[] waar = {HORDE, SPRINGPLANK, SLALOM, KRUIPTUNNEL, BALK, TAFEL};
        for (int i = 0; i < soorten.length; i++) {
            BlockPos pos = start.relative(richting, waar[i]);
            ToestelBlock.bouw(level, pos, ParkourSlice.OBSTAKELS.get(soorten[i]).get(), naarStart);
            paal.klik(pos);
        }
        BlockPos finish = start.relative(richting, FINISH);
        level.setBlock(finish, ParkourSlice.FINISHPAALTJE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, naarStart), 3);
        paal.klik(finish);
        BlockPos bord = start.relative(richting.getClockWise(), 2);
        level.setBlock(bord, ParkourSlice.SCOREBORD.get().defaultBlockState().setValue(ScorebordBlock.FACING, naarStart), 3);
        paal.stuurBorden();
        return paal;
    }

    private ParkourCommando() {
    }
}
