package nl.juiced.guhs.feature.erfenis;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /guhs erfenis — for everyone: (no argument) to your own erfenis (made on first use), {@code terug} back to where you were,
 * {@code bovenaanzicht} the overview of the plot you stand on, {@code bezoek <speler>} somebody else's erfenis (read only).
 * Dev (op): {@code bouwspul <n>}, {@code snel aan|uit}, {@code reset}, {@code npc [level]} (a generated guh village, and you go there),
 * {@code info}.
 */
final class ErfenisCommando {
    private ErfenisCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("erfenis")
                .executes(c -> ga(c.getSource()))
                .then(Commands.literal("terug").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!Erfenis.in(p)) {
                        c.getSource().sendFailure(Component.translatable("erfenis.cmd.niet_in"));
                        return 0;
                    }
                    Erfenis.terug(p);
                    return 1;
                }))
                .then(Commands.literal("bovenaanzicht").executes(c -> {
                    Erfenis.actie(c.getSource().getPlayerOrException(), ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.OPEN));
                    return 1;
                }))
                .then(Commands.literal("bezoek").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerPlayer doel = EntityArgument.getPlayer(c, "speler");
                    Base b = ErfenisData.get(p.level().getServer()).van(doel.getUUID());
                    if (b == null) {
                        c.getSource().sendFailure(Component.translatable("erfenis.cmd.geen_base", doel.getGameProfile().name()));
                        return 0;
                    }
                    return Erfenis.gaNaar(p, b) ? 1 : 0;
                })))
                .then(Commands.literal("bouwspul").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 1_000_000)).executes(c -> {
                            Base b = eigen(c.getSource());
                            b.bouwspul = IntegerArgumentType.getInteger(c, "n");
                            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.bouwspul", b.bouwspul, b.opslag()));
                        })))
                .then(Commands.literal("snel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("aan").executes(c -> snel(c.getSource(), true)))
                        .then(Commands.literal("uit").executes(c -> snel(c.getSource(), false))))
                .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    Base b = eigen(c.getSource());
                    b.vulStart(System.currentTimeMillis());
                    MinecraftServer s = c.getSource().getServer();
                    if (Erfenis.bezocht(s, b)) {
                        Erfenis.stempel(s, b);
                    }
                    return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.reset"));
                }))
                .then(Commands.literal("npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> npc(c.getSource(), 0, null))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, GebouwType.MAX_STADHUIS))
                                .executes(c -> npc(c.getSource(), IntegerArgumentType.getInteger(c, "level"), null))
                                .then(Commands.argument("naam", StringArgumentType.greedyString())
                                        .executes(c -> npc(c.getSource(), IntegerArgumentType.getInteger(c, "level"),
                                                StringArgumentType.getString(c, "naam"))))))
                .then(Commands.literal("info").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    ErfenisData d = ErfenisData.get(c.getSource().getServer());
                    for (Base b : d.alleBases()) {
                        c.getSource().sendSuccess(() -> Component.literal((b.npc ? "[npc] " : "") + b.naam + " cel " + b.cx + "," + b.cz
                                + " stadhuis " + b.stadhuisLevel() + ", " + b.gebouwen.size() + " gebouwen, bouwspul " + b.bouwspul + "/" + b.opslag()
                                + (b.bouw != null ? ", bouwt " + b.bouw.gebouw() : "")), false);
                    }
                    c.getSource().sendSuccess(() -> Component.literal(d.alleBases().size() + " erfenissen, snel " + (d.snel ? "aan" : "uit")), false);
                    return d.alleBases().size();
                }))));
    }

    private static int ga(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Base b = ErfenisData.get(source.getServer()).vanSpeler(p.getUUID(), p.getGameProfile().name(), System.currentTimeMillis());
        if (!Erfenis.gaNaar(p, b)) {
            source.sendFailure(Component.translatable("erfenis.cmd.geen_dim"));
            return 0;
        }
        return 1;
    }

    private static Base eigen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        return ErfenisData.get(source.getServer()).vanSpeler(p.getUUID(), p.getGameProfile().name(), System.currentTimeMillis());
    }

    private static int klaar(CommandSourceStack source, Base b, Component tekst) {
        MinecraftServer s = source.getServer();
        ErfenisData.get(s).setDirty();
        Erfenis.syncKijkers(s, b);
        source.sendSuccess(() -> tekst, false);
        return 1;
    }

    private static int snel(CommandSourceStack source, boolean aan) {
        ErfenisData d = ErfenisData.get(source.getServer());
        d.snel = aan;
        d.setDirty();
        for (Base b : d.alleBases()) {
            if (Erfenis.bijwerken(source.getServer(), b, true)) {
                Erfenis.syncKijkers(source.getServer(), b);
            }
        }
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            Base b = Erfenis.onder(p);
            if (b != null) {
                Erfenis.sync(p, b, false);
            }
        }
        source.sendSuccess(() -> Component.translatable(aan ? "erfenis.cmd.snel_aan" : "erfenis.cmd.snel_uit"), true);
        return 1;
    }

    private static final String[] VOOR = {"Knabbel", "Snoet", "Pluis", "Vads", "Njeg", "Kaas", "Knuffel", "Oortjes", "Wiebel", "Snurk"};
    private static final String[] ACHTER = {"dorp", "hoeve", "stee", "veld", "nest", "burcht", "hoek", "dal"};

    private static int npc(CommandSourceStack source, int level, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        long seed = System.nanoTime();
        java.util.Random r = new java.util.Random(seed);
        String n = naam != null ? naam : VOOR[r.nextInt(VOOR.length)] + ACHTER[r.nextInt(ACHTER.length)];
        int lvl = level > 0 ? level : 1 + r.nextInt(GebouwType.MAX_STADHUIS);
        Base b = ErfenisData.get(source.getServer()).maakNpcBase(n, lvl, seed, System.currentTimeMillis());
        source.sendSuccess(() -> Component.translatable("erfenis.cmd.npc", b.naam, lvl, b.cx, b.cz), false);
        return Erfenis.gaNaar(p, b) ? 1 : 0;
    }
}
