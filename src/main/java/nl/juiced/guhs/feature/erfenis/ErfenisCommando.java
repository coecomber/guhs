package nl.juiced.guhs.feature.erfenis;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * /guhs erfenis — for everyone: (no argument) to your own erfenis (made on first use), {@code terug} back to where you were,
 * {@code bovenaanzicht} the overview of the plot you stand on, {@code bezoek <speler>} somebody else's erfenis (read only).
 * Dev (op), all on your own erfenis: {@code geef <goed|guhlden|alles> <n>}, {@code hoeve <level>}, {@code tijd <minuten>}
 * (time passes: production, builds and residents catch up), {@code snel aan|uit} (every job finishes at once),
 * {@code bewoners <n>}, {@code bestellingen <n>}, {@code opruimen alles|<n>}, {@code kavels <n>}, {@code vk <n>}
 * (extra vadskracht at the Vadsmast), {@code reset}, {@code npc [level] [naam]}, {@code info}.
 */
final class ErfenisCommando {
    private ErfenisCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> e = Commands.literal("erfenis")
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
                })));
        e.then(dev("geef").then(Commands.argument("wat", StringArgumentType.word()).suggests((c, sb) -> {
            java.util.List<String> ids = new java.util.ArrayList<>(java.util.List.of(ErfenisEconomie.GUHLDEN, "alles"));
            for (Goed g : Goed.values()) {
                ids.add(g.id);
            }
            return SharedSuggestionProvider.suggest(ids, sb);
        }).then(Commands.argument("n", IntegerArgumentType.integer(1, 1_000_000)).executes(c -> {
            Base b = eigen(c.getSource());
            String wat = StringArgumentType.getString(c, "wat");
            int n = IntegerArgumentType.getInteger(c, "n");
            Erfenis.bijwerken(c.getSource().getServer(), b, false);
            int erin;
            if ("alles".equals(wat)) {
                erin = b.geefGuhlden(n);
                for (Goed g : Goed.values()) {
                    erin += b.geefGoed(g, Math.min(n, Math.max(0, b.opslagVrij() / Math.max(1, Goed.values().length))));
                }
            } else if (ErfenisEconomie.GUHLDEN.equals(wat)) {
                erin = b.geefGuhlden(n);
            } else {
                Goed g = Goed.van(wat);
                if (g == null) {
                    c.getSource().sendFailure(Component.literal("Onbekend goed: " + wat));
                    return 0;
                }
                erin = b.geefGoed(g, n);
            }
            int x = erin;
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.geef", x, wat, b.guhlden, b.guhldenCap(), b.opslagGebruikt(), b.opslag()));
        }))));
        e.then(dev("hoeve").then(Commands.argument("level", IntegerArgumentType.integer(1, GebouwType.MAX_HOEVE)).executes(c -> {
            Base b = eigen(c.getSource());
            int lvl = IntegerArgumentType.getInteger(c, "level");
            Erfenis.bijwerken(c.getSource().getServer(), b, false);
            Gebouw h = b.hoeve();
            if (h != null) {
                h.level = lvl;
                if (!h.paletGekozen) {
                    h.palet = GebouwVormen.vormTier(h.type.id, lvl);
                }
                b.bouwen.removeIf(x -> x.soort() == Base.Bouw.GEBOUW && x.ref() == h.id);
                ErfenisMomenten.melding(c.getSource().getServer(), b.eigenaar, "hoeve_level:" + lvl);
                restempel(c.getSource().getServer(), b);
            }
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.hoeve", lvl));
        })));
        e.then(dev("tijd").then(Commands.argument("minuten", IntegerArgumentType.integer(1, 60 * 24 * 14)).executes(c -> {
            Base b = eigen(c.getSource());
            int min = IntegerArgumentType.getInteger(c, "minuten");
            Erfenis.bijwerken(c.getSource().getServer(), b, false);
            b.verschuif(min * 60_000L);
            Erfenis.bijwerken(c.getSource().getServer(), b, true);
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.tijd", min));
        })));
        e.then(dev("snel").then(Commands.literal("aan").executes(c -> snel(c.getSource(), true)))
                .then(Commands.literal("uit").executes(c -> snel(c.getSource(), false))));
        e.then(dev("bewoners").then(Commands.argument("n", IntegerArgumentType.integer(0, 500)).executes(c -> {
            Base b = eigen(c.getSource());
            Erfenis.bijwerken(c.getSource().getServer(), b, false);
            b.bewoners = IntegerArgumentType.getInteger(c, "n");
            b.volgendeIntrek = 0;
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.bewoners", b.bewoners, b.maxBewoners()));
        })));
        e.then(dev("bestellingen").then(Commands.argument("n", IntegerArgumentType.integer(0, 100_000)).executes(c -> {
            Base b = eigen(c.getSource());
            b.bestellingen = IntegerArgumentType.getInteger(c, "n");
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.bestellingen", b.bestellingen));
        })));
        e.then(dev("opruimen").then(Commands.literal("alles").executes(c -> opruimen(c.getSource(), Plot.PLUKJES)))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, Plot.PLUKJES)).executes(c -> opruimen(c.getSource(),
                        IntegerArgumentType.getInteger(c, "n")))));
        e.then(dev("kavels").then(Commands.argument("n", IntegerArgumentType.integer(0, Plot.KAVELS)).executes(c -> {
            Base b = eigen(c.getSource());
            int n = IntegerArgumentType.getInteger(c, "n");
            b.kavels = n >= Plot.KAVELS ? (1 << Plot.KAVELS) - 1 : (1 << n) - 1;
            restempel(c.getSource().getServer(), b);
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.kavels", n));
        })));
        e.then(dev("vk").then(Commands.argument("n", IntegerArgumentType.integer(0, 1000)).executes(c -> {
            ErfenisData d = ErfenisData.get(c.getSource().getServer());
            d.devVk = IntegerArgumentType.getInteger(c, "n");
            d.setDirty();
            Base b = eigen(c.getSource());
            Erfenis.bijwerken(c.getSource().getServer(), b, false);
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.vk", d.devVk, b.vkInDorp()));
        })));
        e.then(dev("demo").executes(c -> demo(c.getSource())));
        e.then(dev("ui").then(Commands.argument("wat", StringArgumentType.word())
                .executes(c -> ui(c.getSource(), StringArgumentType.getString(c, "wat"), ""))
                .then(Commands.argument("arg", StringArgumentType.greedyString())
                        .executes(c -> ui(c.getSource(), StringArgumentType.getString(c, "wat"), StringArgumentType.getString(c, "arg"))))));
        e.then(dev("doe").then(Commands.argument("wat", StringArgumentType.greedyString())
                .executes(c -> doe(c.getSource(), StringArgumentType.getString(c, "wat")))));
        e.then(dev("reset").executes(c -> {
            Base b = eigen(c.getSource());
            b.vulStart(System.currentTimeMillis());
            MinecraftServer s = c.getSource().getServer();
            if (Erfenis.bezocht(s, b)) {
                Erfenis.stempel(s, b);
            }
            return klaar(c.getSource(), b, Component.translatable("erfenis.cmd.reset"));
        }));
        e.then(dev("npc").executes(c -> npc(c.getSource(), 0, null))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, GebouwType.MAX_HOEVE))
                        .executes(c -> npc(c.getSource(), IntegerArgumentType.getInteger(c, "level"), null))
                        .then(Commands.argument("naam", StringArgumentType.greedyString())
                                .executes(c -> npc(c.getSource(), IntegerArgumentType.getInteger(c, "level"), StringArgumentType.getString(c, "naam"))))));
        e.then(dev("info").executes(c -> {
            ErfenisData d = ErfenisData.get(c.getSource().getServer());
            for (Base b : d.alleBases()) {
                c.getSource().sendSuccess(() -> Component.literal((b.npc ? "[npc] " : "") + b.naam + " cel " + b.cx + "," + b.cz
                        + " hoeve " + b.hoeveLevel() + ", " + b.gebouwen.size() + " gebouwen, " + b.guhlden + " guhlden, goederen "
                        + b.opslagGebruikt() + "/" + b.opslag() + ", bewoners " + b.bewoners + "/" + b.maxBewoners() + ", plukjes "
                        + b.opgeruimdTotaal() + "/72, bouwers " + b.bouwen.size() + "/" + b.bouwers()), false);
            }
            c.getSource().sendSuccess(() -> Component.literal(d.alleBases().size() + " erfenissen, snel " + (d.snel ? "aan" : "uit") + ", dev-vk "
                    + d.devVk), false);
            return d.alleBases().size();
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(e));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int ga(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!Erfenis.magErfenis(p)) {
            source.sendFailure(Component.translatable("erfenis.cmd.nog_geen_erfenis"));
            return 0;
        }
        Base b = Erfenis.eigenBase(p);
        if (!Erfenis.gaNaar(p, b)) {
            source.sendFailure(Component.translatable("erfenis.cmd.geen_dim"));
            return 0;
        }
        return 1;
    }

    private static Base eigen(CommandSourceStack source) throws CommandSyntaxException {
        return Erfenis.eigenBase(source.getPlayerOrException());
    }

    private static void restempel(MinecraftServer s, Base b) {
        b.moetStempelen = true;
        if (Erfenis.bezocht(s, b)) {
            Erfenis.stempel(s, b);
        }
    }

    private static int opruimen(CommandSourceStack source, int n) throws CommandSyntaxException {
        Base b = eigen(source);
        Erfenis.bijwerken(source.getServer(), b, false);
        int gedaan = 0;
        for (int i = 0; i < Plot.PLUKJES && gedaan < n; i++) {
            if (!b.opgeruimd[i]) {
                b.opgeruimd[i] = true;
                gedaan++;
            }
        }
        b.bouwen.removeIf(x -> x.soort() == Base.Bouw.PLUKJE);
        restempel(source.getServer(), b);
        return klaar(source, b, Component.translatable("erfenis.cmd.opruimen", gedaan, b.opgeruimdTotaal()));
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

    /**
     * Dev: a ready-made village to look at and test with (Hoeve 6, all plukjes cleared, about twenty buildings at
     * levels 1-6, residents, money and goods, and an hour and a half of production in the baskets).
     */
    private static int demo(CommandSourceStack source) throws CommandSyntaxException {
        Base b = eigen(source);
        long nu = System.currentTimeMillis();
        b.vulStart(nu);
        b.hoeve().level = 6;
        b.hoeve().palet = GebouwVormen.vormTier("hoeve", 6);
        java.util.Arrays.fill(b.opgeruimd, true);
        Object[][] plan = {
                {GebouwType.KIPPENHOK, 4, 53, 41}, {GebouwType.OPSLAGSCHUUR, 5, 41, 54}, {GebouwType.OPSLAGSCHUUR, 4, 64, 66},
                {GebouwType.GUHHUTJE, 4, 49, 54}, {GebouwType.GUHHUTJE, 4, 55, 54}, {GebouwType.GUHHUTJE, 3, 49, 59}, {GebouwType.GUHHUTJE, 3, 55, 59},
                {GebouwType.GUHHUTJE, 2, 34, 54}, {GebouwType.MARKTPLEIN, 3, 64, 41}, {GebouwType.SCHAAPSKOOI, 4, 24, 24}, {GebouwType.AKKER, 4, 24, 40},
                {GebouwType.AKKER, 3, 24, 49}, {GebouwType.BOSJE, 3, 64, 24}, {GebouwType.HOUTZAGERIJ, 4, 36, 24}, {GebouwType.KOEIENSTAL, 2, 76, 52},
                {GebouwType.MOLEN, 3, 44, 24}, {GebouwType.BAKKERIJ, 3, 52, 24}, {GebouwType.WEVERIJ, 2, 36, 32}, {GebouwType.SPEELTUIN, 2, 24, 58},
                {GebouwType.GUHPENSION, 2, 34, 66}, {GebouwType.DETECTIVEBUREAU, 2, 44, 66}, {GebouwType.TIMMERSCHUUR, 1, 52, 66},
                {GebouwType.GROEVE, 2, 76, 66}, {GebouwType.DORPSCAFE, 1, 24, 68}, {GebouwType.LOKET, 1, 74, 40},
                {GebouwType.BOUWBLOKKENWERKPLAATS, 2, 24, 78}, {GebouwType.STEENBAKKERIJ, 2, 36, 78}};
        for (Object[] p : plan) {
            GebouwType t = (GebouwType) p[0];
            int x = (Integer) p[2], z = (Integer) p[3];
            if (b.vrij(x, z, t.breedte, t.diepte, -1)) {
                Gebouw g = b.nieuwGebouw(t, (Integer) p[1], x, z, 0, nu);
                if (t == GebouwType.BOUWBLOKKENWERKPLAATS) {
                    g.selectie.addAll(Bouwkeet.sets().get(0).subList(0, Math.min(3, Bouwkeet.sets().get(0).size())));
                }
                if (t == GebouwType.AKKER && x == 24 && z == 49) {
                    g.keuze = 2;
                }
            }
        }
        b.kippenhokGratis = false;
        b.bewoners = Math.min(24, b.maxBewoners());
        b.bestellingen = 40;
        b.guhlden = 9000;
        for (Object[] kv : new Object[][] {{Goed.PLANK, 320}, {Goed.BAKSTEEN, 140}, {Goed.WOL, 60}, {Goed.EI, 45}, {Goed.GRAAN, 120}, {Goed.MELK, 30},
                {Goed.MEEL, 20}, {Goed.STAM, 200}, {Goed.KLEI, 40}, {Goed.HONING, 12}}) {
            b.geefGoed((Goed) kv[0], (Integer) kv[1]);
        }
        b.vondsten.add("roestig_sleuteltje");
        b.decoOpslag.put("roestige_weerhaan", 1);
        b.bijgewerkt = nu - 90 * 60_000L;
        Erfenis.bijwerken(source.getServer(), b, false);
        restempel(source.getServer(), b);
        return klaar(source, b, Component.translatable("erfenis.cmd.demo"));
    }

    /** Dev (AutoCheck): steers the open overview (select a building / plukje / kavel, a shop tab, a window). */
    private static int ui(CommandSourceStack source, String wat, String arg) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if ("venster".equals(wat) && "pension".equals(arg)) {
            Base b = Erfenis.onder(p);
            if (b != null) {
                Guhpension.open(p, b);
            }
            return 1;
        }
        Erfenis.melding(p, "erfenis.dev.ui", true, wat, arg);
        return 1;
    }

    /**
     * Dev (AutoCheck): does what a click in the own overview does, through the same server path ({@link Erfenis#actie}):
     * {@code plaats <type> <x> <z> [rot]}, {@code verbeter <type>}, {@code ophalen <type>}, {@code allesophalen},
     * {@code opruimen <plukje>}, {@code deco <id> <x> <z> [rot]}, {@code naarhuis <goed> <n>}, {@code bureau}, {@code markt}. A type means its first building.
     * Deco slice: {@code koop}, {@code bon}, {@code trek}, {@code lijn}, {@code gum}, {@code decoweg}, {@code decodraai}, {@code decoverplaats}.
     */
    private static int doe(CommandSourceStack source, String wat) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Base b = Erfenis.onder(p);
        String[] a = wat.trim().split(" +");
        if (b == null || a.length == 0) {
            source.sendFailure(Component.literal("Sta op je eigen erfenis."));
            return 0;
        }
        java.util.function.IntFunction<String> arg = i -> i < a.length ? a[i] : "0";
        java.util.function.Function<String, Integer> eerste = id -> {
            for (Gebouw g : b.gebouwen) {
                if (g.type.id.equals(id)) {
                    return g.id;
                }
            }
            return -1;
        };
        ErfenisPayloads.Actie act = switch (a[0]) {
            case "plaats" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.PLAATS, 0, Integer.parseInt(arg.apply(2)), Integer.parseInt(arg.apply(3)),
                    0, 0, Integer.parseInt(arg.apply(4)), arg.apply(1));
            case "verbeter" -> ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.VERBETER, eerste.apply(arg.apply(1)));
            case "ophalen" -> ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.OPHALEN, eerste.apply(arg.apply(1)));
            case "allesophalen" -> ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.OPHALEN_ALLES);
            case "opruimen" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.OPRUIMEN, 0, Integer.parseInt(arg.apply(1)), 0, 0, 0, 0, "");
            case "deco" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, Integer.parseInt(arg.apply(2)), Integer.parseInt(arg.apply(3)),
                    ErfenisPayloads.Actie.DECO_PLAATS, 0, Integer.parseInt(arg.apply(4)), arg.apply(1));
            // deco slice: koop <id> [x z rot] (with x: buy and place), bon <id> x z [rot], trek <soort> x,z;x,z..., lijn <soort> x0 z0 x1 z1,
            // gum x,z;..., decoweg x z, decodraai x z, decoverplaats x z nx nz [rot]
            case "koop", "bon" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, (a.length > 2 ? ErfenisPayloads.Actie.KOOP_PLAATS : 0)
                    | ("bon".equals(a[0]) ? ErfenisPayloads.Actie.KOOP_BON : 0), Integer.parseInt(arg.apply(2)), Integer.parseInt(arg.apply(3)),
                    ErfenisPayloads.Actie.DECO_KOOP, 0, Integer.parseInt(arg.apply(4)), arg.apply(1));
            case "trek" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, 0, 0, ErfenisPayloads.Actie.DECO_TREK, 0, 0,
                    arg.apply(1) + ":" + arg.apply(2));
            case "lijn" -> {
                int x0 = Integer.parseInt(arg.apply(2)), z0 = Integer.parseInt(arg.apply(3));
                java.util.List<int[]> l = new java.util.ArrayList<>();
                l.add(new int[] {x0, z0});
                l.addAll(Paden.stap(x0, z0, Integer.parseInt(arg.apply(4)), Integer.parseInt(arg.apply(5))));
                yield new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, 0, 0, ErfenisPayloads.Actie.DECO_TREK, 0, 0,
                        arg.apply(1) + ":" + Paden.codeer(l));
            }
            case "gum" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, 0, 0, ErfenisPayloads.Actie.DECO_GUM, 0, 0, arg.apply(1));
            case "decoweg" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, Integer.parseInt(arg.apply(1)), Integer.parseInt(arg.apply(2)),
                    ErfenisPayloads.Actie.DECO_WEG, 0, 0, "");
            case "decodraai" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, 0, Integer.parseInt(arg.apply(1)), Integer.parseInt(arg.apply(2)),
                    ErfenisPayloads.Actie.DECO_DRAAI, 0, 0, "");
            case "decoverplaats" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.DECO, Integer.parseInt(arg.apply(3)), Integer.parseInt(arg.apply(1)),
                    Integer.parseInt(arg.apply(2)), ErfenisPayloads.Actie.DECO_VERPLAATS, Integer.parseInt(arg.apply(4)), Integer.parseInt(arg.apply(5)), "");
            case "naarhuis" -> new ErfenisPayloads.Actie(ErfenisPayloads.Actie.NAAR_HUIS, 0, Integer.parseInt(arg.apply(2)), 0, 0, 0, 0, arg.apply(1));
            case "bureau" -> ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.BUREAU, eerste.apply("detectivebureau"));
            case "markt" -> ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.MARKT, eerste.apply("marktplein"));
            default -> null;
        };
        if (act == null) {
            source.sendFailure(Component.literal("Onbekend: " + a[0]));
            return 0;
        }
        Erfenis.actie(p, act);
        return 1;
    }

    private static final String[] VOOR = {"Knabbel", "Snoet", "Pluis", "Vads", "Njeg", "Kaas", "Knuffel", "Oortjes", "Wiebel", "Snurk"};
    private static final String[] ACHTER = {"dorp", "hoeve", "stee", "veld", "nest", "burcht", "hoek", "dal"};

    private static int npc(CommandSourceStack source, int level, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        long seed = System.nanoTime();
        java.util.Random r = new java.util.Random(seed);
        String n = naam != null ? naam : VOOR[r.nextInt(VOOR.length)] + ACHTER[r.nextInt(ACHTER.length)];
        int lvl = level > 0 ? level : 1 + r.nextInt(GebouwType.MAX_HOEVE);
        Base b = ErfenisData.get(source.getServer()).maakNpcBase(n, lvl, seed, System.currentTimeMillis());
        source.sendSuccess(() -> Component.translatable("erfenis.cmd.npc", b.naam, lvl, b.cx, b.cz), false);
        return Erfenis.gaNaar(p, b) ? 1 : 0;
    }
}
