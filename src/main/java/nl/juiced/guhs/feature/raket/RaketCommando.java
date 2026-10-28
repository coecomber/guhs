package nl.juiced.guhs.feature.raket;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Test commands (permission level 2, in the /guhs tree), all on the nearest Lanceerplatform within 16 blocks:
 * <ul>
 *   <li>{@code /guhs raket stap <0-4>}: that many stages done (the rocket built that far);</li>
 *   <li>{@code /guhs raket spullen}: everything the stages ask, in your inventory;</li>
 *   <li>{@code /guhs raket bemanning}: three tame guhs of yours in astronaut clothes, next to you;</li>
 *   <li>{@code /guhs raket lanceer}: the launch (all stages done first);</li>
 *   <li>{@code /guhs raket maan} / {@code terug}: straight to the Kaasmaan and back;</li>
 *   <li>{@code /guhs raket reset}: an empty platform again (the rocket blocks stay; break them by hand).</li>
 * </ul>
 */
public final class RaketCommando {
    private RaketCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("raket").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, RaketStappen.STAPPEN.size()))
                        .executes(c -> stap(c.getSource(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("spullen").executes(c -> spullen(c.getSource())))
                .then(Commands.literal("bemanning").executes(c -> bemanning(c.getSource())))
                .then(Commands.literal("lanceer").executes(c -> lanceer(c.getSource())))
                .then(Commands.literal("maan").executes(c -> maan(c.getSource())))
                .then(Commands.literal("terug").executes(c -> terug(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))));
    }

    @Nullable
    static BlockPos zoek(ServerLevel level, BlockPos bij) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(bij.offset(-16, -16, -16), bij.offset(16, 16, 16))) {
            if (level.getBlockEntity(p) instanceof LanceerplatformBlockEntity) {
                double d = p.distSqr(bij);
                if (d < bd) {
                    bd = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Nullable
    private static LanceerplatformBlockEntity platform(CommandSourceStack src) {
        BlockPos p = zoek(src.getLevel(), BlockPos.containing(src.getPosition()));
        if (p == null) {
            src.sendFailure(Component.translatable("gui.guhs.raket.cmd.geen"));
            return null;
        }
        return (LanceerplatformBlockEntity) src.getLevel().getBlockEntity(p);
    }

    private static int stap(CommandSourceStack src, int n) {
        LanceerplatformBlockEntity be = platform(src);
        if (be == null) {
            return 0;
        }
        be.zetKlaar(n);
        for (int s = 1; s <= n; s++) {
            RaketBouw.plaats(src.getLevel(), be.getBlockPos(), s, s == n);
        }
        src.sendSuccess(() -> Component.translatable("gui.guhs.raket.cmd.stap", n), false);
        return 1;
    }

    private static int spullen(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            return 0;
        }
        for (RaketStappen.Stap s : RaketStappen.STAPPEN) {
            for (RaketStappen.Eis e : s.eisen()) {
                int rest = e.aantal();
                while (rest > 0) {
                    ItemStack st = new ItemStack(e.item().get(), Math.min(rest, e.item().get().getDefaultMaxStackSize()));
                    rest -= st.getCount();
                    if (!p.getInventory().add(st)) {
                        p.drop(st, false);
                    }
                }
            }
        }
        src.sendSuccess(() -> Component.translatable("gui.guhs.raket.cmd.spullen"), false);
        return 1;
    }

    private static int bemanning(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            return 0;
        }
        ServerLevel level = src.getLevel();
        for (int i = 0; i < RaketStappen.BEMANNING; i++) {
            GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
            if (g == null) {
                continue;
            }
            g.snapTo(p.getX() + i - 1, p.getY(), p.getZ() + 1.5, 180f, 0f);
            g.tame(p);
            g.wear(GuhClothes.ASTRONAUTENPAKJE);
            g.wear(GuhClothes.ASTRONAUTENHELM);
            level.addFreshEntity(g);
        }
        src.sendSuccess(() -> Component.translatable("gui.guhs.raket.cmd.bemanning"), false);
        return 1;
    }

    private static int lanceer(CommandSourceStack src) {
        LanceerplatformBlockEntity be = platform(src);
        ServerPlayer p = src.getPlayer();
        if (be == null || p == null) {
            return 0;
        }
        if (be.bezig() <= RaketStappen.STAPPEN.size()) {
            stap(src, RaketStappen.STAPPEN.size());
        }
        Raket.lanceer(src.getLevel(), be.getBlockPos(), be, p);
        return 1;
    }

    private static int maan(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            return 0;
        }
        KaasMaan.naarMaan(p, BlockPos.containing(p.position()).offset(-4, -1, -4));
        return 1;
    }

    private static int terug(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            return 0;
        }
        KaasMaan.terug(p);
        return 1;
    }

    private static int reset(CommandSourceStack src) {
        LanceerplatformBlockEntity be = platform(src);
        if (be == null) {
            return 0;
        }
        be.reset();
        src.sendSuccess(() -> Component.translatable("gui.guhs.raket.cmd.reset"), false);
        return 1;
    }
}
