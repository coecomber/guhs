package nl.juiced.guhs.feature.erfenis.kaart;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.erfenis.ErfenisEconomie;

/**
 * /guhs erfenis kaart — opens the world map (everyone); {@code /guhs erfenis kaart bestellingen} opens the orders.
 * Dev (op): {@code npc groei [n]} (growth ticks: villages follow their player + cosy growth), {@code npc snel aan|uit}
 * (every 15 s instead of every 4 min), {@code bestel snel aan|uit} (carts drive and rest 10 s), {@code bestel ster}
 * (today's star order again), {@code bestel goed <id> <n>} (goods into your village storage), {@code bestel vul}
 * (enough of everything for every open order), {@code hulp reset} (help again today).
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
                .then(Commands.literal("bestellingen").executes(c -> {
                    Bestellingen.open(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("groei")
                                .executes(c -> groei(c.getSource(), 1))
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(c -> groei(c.getSource(),
                                        IntegerArgumentType.getInteger(c, "n")))))
                        .then(Commands.literal("snel")
                                .then(Commands.literal("aan").executes(c -> snel(c.getSource(), true)))
                                .then(Commands.literal("uit").executes(c -> snel(c.getSource(), false)))))
                .then(Commands.literal("bestel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("snel")
                                .then(Commands.literal("aan").executes(c -> bestelSnel(c.getSource(), true)))
                                .then(Commands.literal("uit").executes(c -> bestelSnel(c.getSource(), false))))
                        .then(Commands.literal("ster").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BestelData data = BestelData.get(c.getSource().getServer());
                            BestelData.Speler s = data.van(p.getUUID());
                            s.ster = null;
                            s.sterDag = -1;
                            data.setDirty();
                            Bestellingen.bij(c.getSource().getServer(), p.getUUID());
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.ster"), true);
                            return 1;
                        }))
                        .then(Commands.literal("vul").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BestelData.Speler s = Bestellingen.bij(c.getSource().getServer(), p.getUUID());
                            java.util.List<Bestelling> alle = new java.util.ArrayList<>(s.karren);
                            if (s.ster != null) {
                                alle.add(s.ster);
                            }
                            for (Bestelling b : alle) {
                                for (Bestelling.Regel r : b.regels) {
                                    ErfenisEconomie.geef(c.getSource().getServer(), p.getUUID(), r.goed(), r.n());
                                }
                            }
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.vul"), true);
                            return 1;
                        }))
                        .then(Commands.literal("goed").then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 100000)).executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(c, "id");
                                    int n = IntegerArgumentType.getInteger(c, "n");
                                    ErfenisEconomie.geef(c.getSource().getServer(), p.getUUID(), id, n);
                                    c.getSource().sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.goed", n, id), true);
                                    return n;
                                })))))
                .then(Commands.literal("hulp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reset").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            BestelData data = BestelData.get(c.getSource().getServer());
                            BestelData.Speler s = data.van(p.getUUID());
                            s.geholpen.clear();
                            s.hulpBeloond = 0;
                            data.setDirty();
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.kaart.cmd.hulp_reset"), true);
                            return 1;
                        }))))));
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

    private static int bestelSnel(CommandSourceStack source, boolean aan) {
        BestelData d = BestelData.get(source.getServer());
        d.snel = aan;
        d.setDirty();
        source.sendSuccess(() -> Component.translatable(aan ? "erfenis.kaart.cmd.bestel_snel_aan" : "erfenis.kaart.cmd.bestel_snel_uit"), true);
        return 1;
    }
}
