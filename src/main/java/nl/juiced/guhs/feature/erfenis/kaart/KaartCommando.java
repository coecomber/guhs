package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /guhs erfenis kaart — opens the world map (everyone). Dev (op): {@code npc nieuw <n>} (n new NPC villages),
 * {@code npc groei [n]} (force growth ticks), {@code npc snel aan|uit} (grow every 15 s instead of every 4 min).
 * (Brigadier merges this "guhs erfenis" branch with the core's own /guhs erfenis.)
 */
final class KaartCommando {
    private KaartCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("erfenis").then(Commands.literal("kaart")
                .executes(c -> {
                    KaartServer.stuur(c.getSource().getPlayerOrException(), true);
                    return 1;
                })
                .then(Commands.literal("npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("nieuw").then(Commands.argument("n", IntegerArgumentType.integer(1, 50)).executes(c -> {
                            List<NpcDorp> l = KaartServer.nieuw(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "n"));
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.nieuw", l.size(),
                                    KaartData.get(c.getSource().getServer()).npcs().size()), true);
                            return l.size();
                        })))
                        .then(Commands.literal("groei")
                                .executes(c -> groei(c.getSource(), 1))
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(c -> groei(c.getSource(),
                                        IntegerArgumentType.getInteger(c, "n")))))
                        .then(Commands.literal("snel")
                                .then(Commands.literal("aan").executes(c -> snel(c.getSource(), true)))
                                .then(Commands.literal("uit").executes(c -> snel(c.getSource(), false))))))));
    }

    private static int groei(CommandSourceStack source, int n) {
        int totaal = 0;
        for (int i = 0; i < n; i++) {
            totaal += KaartServer.groeiNu(source.getServer()).size();
        }
        int t = totaal;
        source.sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.groei", n, t), true);
        return Math.max(1, totaal);
    }

    private static int snel(CommandSourceStack source, boolean aan) {
        KaartData d = KaartData.get(source.getServer());
        d.snel = aan;
        d.setDirty();
        KaartServer.herstart();
        source.sendSuccess(() -> Component.translatable(aan ? "erfenis.kaart.cmd.snel_aan" : "erfenis.kaart.cmd.snel_uit"), true);
        return 1;
    }
}
