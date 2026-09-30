package nl.juiced.guhs.feature.ballon;

import java.util.Comparator;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * Op command (for testing, and the autocheck): {@code /guhs ballonvlucht} puts you in the nearest (non-decoration)
 * guh balloon and takes off on your next route, Kapitein Wolkje in the basket (the nearest BALLONGUH steps in, else
 * he is only drawn); {@code /guhs ballonvlucht stop} lands it at once. (Brigadier merges this into the /guhs tree.)
 */
public final class BallonCommando {
    public static final double BEREIK = 24;

    private BallonCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("ballonvlucht").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> vlieg(ctx.getSource()))
                .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())))));
    }

    private static int vlieg(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LuchtballonEntity ballon = player.level().getEntitiesOfClass(LuchtballonEntity.class, player.getBoundingBox().inflate(BEREIK),
                        b -> !b.isRemoved() && !b.isDeco() && !b.vliegt())
                .stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(player))).orElse(null);
        if (ballon == null) {
            source.sendFailure(Component.literal("Njeg: geen guh-luchtballon in de buurt (binnen " + (int) BEREIK + " blokken)."));
            return 0;
        }
        if (player.isSpectator()) {
            player.setGameMode(GameType.CREATIVE);           // a spectator can't sit in a basket
        }
        GuhNpcEntity kapitein = player.level().getEntitiesOfClass(GuhNpcEntity.class, ballon.getBoundingBox().inflate(BEREIK),
                        n -> n.getKind() == GuhNpcEntity.Kind.BALLONGUH && !n.isInvisible())
                .stream().min(Comparator.comparingDouble(n -> n.distanceToSqr(ballon))).orElse(null);
        BallonRoute route = BallonVlucht.volgendeRoute(player);
        if (!ballon.stijgOp(player, route, kapitein)) {
            source.sendFailure(Component.literal("Njeg: deze ballon kan nu niet opstijgen."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("VAHOEG! De ballon stijgt op: " + route.id()), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (player.getVehicle() instanceof LuchtballonEntity ballon && ballon.vliegt()) {
            ballon.land(false);
            source.sendSuccess(() -> Component.literal("De ballon is weer geland."), false);
            return 1;
        }
        source.sendFailure(Component.literal("Njeg: je zit niet in een vliegende ballon."));
        return 0;
    }
}
