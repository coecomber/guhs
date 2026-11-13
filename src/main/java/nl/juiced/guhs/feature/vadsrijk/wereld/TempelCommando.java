package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.world.ModDimensions;

/**
 * y-tempel: {@code /guhs vadsrijk tempel zoek [straal] | plaats <x y z> [draai] [--opnieuw] | ongedaan | info} (ops; works from the
 * console / rcon). Always in the Guhmension (the test server without it: the level of the one who asks).
 */
final class TempelCommando {
    private TempelCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> t = Commands.literal("tempel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        t.then(Commands.literal("zoek").executes(c -> zoek(c, 192))
                .then(Commands.argument("straal", IntegerArgumentType.integer(32, 512)).executes(c -> zoek(c, IntegerArgumentType.getInteger(c, "straal")))));
        t.then(Commands.literal("plaats").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(c -> plaats(c, 0, false))
                .then(Commands.literal("--opnieuw").executes(c -> plaats(c, 0, true)))
                .then(Commands.argument("draai", IntegerArgumentType.integer(0, 270)).executes(c -> plaats(c, IntegerArgumentType.getInteger(c, "draai"), false))
                        .then(Commands.literal("--opnieuw").executes(c -> plaats(c, IntegerArgumentType.getInteger(c, "draai"), true))))));
        t.then(Commands.literal("ongedaan").executes(c -> {
            String f = VasteTempel.ongedaan(c.getSource().getServer());
            if (f != null) {
                c.getSource().sendFailure(Component.literal("Niet gelukt: " + f));
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.literal("Vaste tempel weggehaald, het oude terrein staat terug."), true);
            return 1;
        }));
        t.then(Commands.literal("info").executes(c -> {
            VasteTempel v = VasteTempel.get(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal(v.geregistreerd()
                    ? "Vaste tempel: " + v.dim() + " " + v.midden().toShortString() + ", portaal " + (v.portaal() == null ? "?" : v.portaal().toShortString())
                    : "Er is (nog) geen vaste tempel."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(t));
    }

    private static ServerLevel level(CommandSourceStack src) {
        ServerLevel g = src.getServer().getLevel(ModDimensions.GUHMENSION);
        return g != null ? g : src.getLevel();
    }

    private static int zoek(CommandContext<CommandSourceStack> c, int straal) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = level(src);
        ServerPlayer p = src.getPlayer();
        BlockPos midden = p != null && p.level() == level ? p.blockPosition() : BlockPos.ZERO;
        long t0 = System.currentTimeMillis();
        List<VasteTempel.Plek> plekken = VasteTempel.zoek(level, midden, straal, 5);
        long ms = System.currentTimeMillis() - t0;
        if (plekken.isEmpty()) {
            src.sendFailure(Component.literal("Geen veilige plek binnen " + straal + " blokken van " + midden.toShortString() + " (" + ms + " ms). Probeer een grotere straal of ga ergens anders staan."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Beste plekken rond " + midden.toShortString() + " in " + level.dimension().identifier() + " (" + ms + " ms):"), false);
        int i = 1;
        for (VasteTempel.Plek pl : plekken) {
            int n = i++;
            BlockPos b = pl.pos();
            src.sendSuccess(() -> Component.literal(n + ". " + b.getX() + " " + b.getY() + " " + b.getZ() + "  score " + pl.score() + "  (hoogteverschil "
                    + pl.verschil() + ")  -> /guhs vadsrijk tempel plaats " + b.getX() + " " + b.getY() + " " + b.getZ()), false);
        }
        return plekken.size();
    }

    private static int plaats(CommandContext<CommandSourceStack> c, int draai, boolean opnieuw) throws CommandSyntaxException {
        CommandSourceStack src = c.getSource();
        ServerLevel level = level(src);
        BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
        if (draai % 90 != 0) {
            src.sendFailure(Component.literal("draai moet 0, 90, 180 of 270 zijn"));
            return 0;
        }
        String f = VasteTempel.plaats(level, pos, draai, opnieuw);
        if (f != null) {
            src.sendFailure(Component.literal("Niet geplaatst: " + f));
            return 0;
        }
        VasteTempel v = VasteTempel.get(src.getServer());
        src.sendSuccess(() -> Component.literal("Vaste ordetempel geplaatst bij " + pos.toShortString() + " (portaal "
                + (v.portaal() == null ? "?" : v.portaal().toShortString()) + "). Ongedaan maken: /guhs vadsrijk tempel ongedaan"), true);
        return 1;
    }
}
