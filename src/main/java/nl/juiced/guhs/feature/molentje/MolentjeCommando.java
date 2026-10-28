package nl.juiced.guhs.feature.molentje;

import java.util.Arrays;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.dromen.DromenFeature;

/**
 * 1.8.0 test build, op commands for the Maanmolentje (permission level 2; merged into the /guhs tree; in Guhdromenland):
 * <ul>
 *   <li>{@code /guhs molentje los}: all cogs up and the nearest Blubs onto the three cushions (the sails start at the next look);</li>
 *   <li>{@code /guhs molentje schud}: stop the sails, let the Blubs go and shuffle the cogs;</li>
 *   <li>{@code /guhs molentje info}: the cogs, how many Blubs blow, whether it turns.</li>
 * </ul>
 */
public final class MolentjeCommando {
    private MolentjeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("molentje").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("los").executes(c -> los(c.getSource())))
                .then(Commands.literal("schud").executes(c -> schud(c.getSource())))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))));
    }

    private static Molen.Plekken molen(CommandSourceStack source) {
        if (source.getLevel().dimension() != DromenFeature.DIM) {
            source.sendFailure(Component.translatable("gui.guhs.molentje.cmd.geen"));
            return null;
        }
        Molen.Plekken p = Molen.opEiland();
        if (p == null || !(source.getLevel().getBlockState(p.wieken()).getBlock() instanceof MolenBlokken.Wieken)) {
            source.sendFailure(Component.translatable("gui.guhs.molentje.cmd.geen"));
            return null;
        }
        return p;
    }

    private static int los(CommandSourceStack source) {
        Molen.Plekken p = molen(source);
        if (p == null) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        for (int i = 0; i < p.tandwielen().size(); i++) {
            var st = level.getBlockState(p.tandwielen().get(i));
            level.setBlock(p.tandwielen().get(i), st.setValue(MolenBlokken.Tandwiel.STAND, 0), 3);
        }
        int n = 0;
        for (int i = 0; i < p.kussentjes().size(); i++) {
            if (Molen.bezetter(level, p, i) != null) {
                n++;
                continue;
            }
            for (BlubGuhEntity b : level.getEntitiesOfClass(BlubGuhEntity.class, p.gebied(), b -> b.isAlive() && !b.isTame() && !b.isBaby()
                    && b.getPersistentData().getIntOr(Molen.KUSSEN, 0) == 0)) {
                Molen.stuur(b, i);
                b.snapTo(p.kussentjes().get(i).getX() + 0.5, p.kussentjes().get(i).getY() + 0.25, p.kussentjes().get(i).getZ() + 0.5, 0, 0);
                n++;
                break;
            }
        }
        int blubs = n;
        source.sendSuccess(() -> Component.translatable("gui.guhs.molentje.cmd.los", blubs), true);
        return 1;
    }

    private static int schud(CommandSourceStack source) {
        Molen.Plekken p = molen(source);
        if (p == null) {
            return 0;
        }
        Molen.reset(source.getLevel(), p);
        source.sendSuccess(() -> Component.translatable("gui.guhs.molentje.cmd.schud"), true);
        return 1;
    }

    private static int info(CommandSourceStack source) {
        Molen.Plekken p = molen(source);
        if (p == null) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        String standen = Arrays.toString(Molen.standen(level, p));
        int blazen = Molen.blubsVan(level, p).size();
        boolean draait = Molen.loopt(level, p);
        source.sendSuccess(() -> Component.translatable("gui.guhs.molentje.cmd.info", standen, blazen, String.valueOf(draait)), false);
        return 1;
    }
}
