package nl.juiced.guhs.feature.vadsrijk.gilden;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-GILDEN commands.
 * <ul>
 *   <li><b>For everyone</b>: {@code /bond} (the panel) and its buttons: {@code leden|taken|perks|kasteel|werving|banier|bord|ranglijst|uitleg},
 *   {@code maak <TAG> <naam>} (at the Bondsklerk), {@code uitnodig <speler>}, {@code accepteer|weiger}, {@code aanmelden <id>},
 *   {@code keur <sleutel> ja|nee}, {@code verlaat [bevestig]}, {@code verwijder|bevorder|degradeer|leider <naam>}, {@code ophef bevestig},
 *   {@code chat} (toggle), {@code werving aan|uit|open|dicht|level <n>|tekst <...>}, {@code banier basis <kleur>|laag <n> <patroon> <kleur>|weg <n>},
 *   {@code kasteel doneer|bouw}, {@code kas <brons>}, {@code thuis}, {@code reis <doel>}; and {@code /bc <tekst>} (bond chat).</li>
 *   <li><b>Dev (op 2)</b>: {@code /guhs vadsrijk gilde maak <TAG> <naam>|uitnodig <speler>|xp <n>|level <n>|kasteel [<n>|bouw]|tp|vallei|bank|
 *   trofee <key>|week klaar|info|export|wis}.</li>
 * </ul>
 */
public final class BondCommando {
    private BondCommando() {
    }

    static ServerPlayer speler(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    static int uit(CommandContext<CommandSourceStack> c, BondActies.Uitslag u) throws CommandSyntaxException {
        speler(c).sendSystemMessage(u.tekst());
        return u.ok() ? 1 : 0;
    }

    static int uitEnPaneel(CommandContext<CommandSourceStack> c, BondActies.Uitslag u, Runnable paneel) throws CommandSyntaxException {
        int r = uit(c, u);
        if (u.ok()) {
            paneel.run();
        }
        return r;
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("bond").executes(c -> {
            BondPaneel.toon(speler(c));
            return 1;
        });
        b.then(Commands.literal("leden").executes(c -> paneel(c, BondPaneel::leden)));
        b.then(Commands.literal("taken").executes(c -> paneel(c, BondPaneel::taken)));
        b.then(Commands.literal("perks").executes(c -> paneel(c, BondPaneel::perks)));
        b.then(Commands.literal("bord").executes(c -> paneel(c, BondPaneel::bord)));
        b.then(Commands.literal("ranglijst").executes(c -> paneel(c, BondPaneel::ranglijst)));
        b.then(Commands.literal("uitleg").executes(c -> paneel(c, BondPaneel::uitleg)));
        b.then(Commands.literal("maak").then(Commands.argument("tag", StringArgumentType.word())
                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = speler(c);
                    return uitEnPaneel(c, BondActies.maak(p, StringArgumentType.getString(c, "naam"), StringArgumentType.getString(c, "tag"),
                            BondNpcs.bijKlerk(p)), () -> BondPaneel.toon(p));
                }))));
        b.then(Commands.literal("uitnodig").then(Commands.argument("speler", EntityArgument.player())
                .executes(c -> uit(c, BondActies.nodigUit(speler(c), EntityArgument.getPlayer(c, "speler"))))));
        b.then(Commands.literal("accepteer").executes(c -> uitEnPaneel(c, BondActies.accepteer(speler(c)), () -> BondPaneel.toon(sp(c)))));
        b.then(Commands.literal("weiger").executes(c -> uit(c, BondActies.weiger(speler(c)))));
        b.then(Commands.literal("aanmelden").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> {
            ServerPlayer p = speler(c);
            Bond bond = Bonden.get(p.level().getServer()).bond(IntegerArgumentType.getInteger(c, "id"));
            if (bond == null) {
                return uit(c, BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_bond_id"));
            }
            return uit(c, BondActies.meldAan(p, bond));
        })));
        b.then(Commands.literal("keur").then(Commands.argument("sleutel", StringArgumentType.string())
                .then(Commands.literal("ja").executes(c -> uitEnPaneel(c, BondActies.keurGoed(speler(c), StringArgumentType.getString(c, "sleutel"), true),
                        () -> BondPaneel.leden(sp(c)))))
                .then(Commands.literal("nee").executes(c -> uit(c, BondActies.keurGoed(speler(c), StringArgumentType.getString(c, "sleutel"), false))))));
        b.then(Commands.literal("verlaat").executes(c -> {
            speler(c).sendSystemMessage(BondPaneel.rij(Component.translatable("vadsrijk.gilden.verlaat.zeker").withStyle(ChatFormatting.RED),
                    BondPaneel.knop("vadsrijk.gilden.knop.ja", "/bond verlaat bevestig", ChatFormatting.RED)));
            return 1;
        }).then(Commands.literal("bevestig").executes(c -> uit(c, BondActies.verlaat(speler(c))))));
        b.then(Commands.literal("ophef").executes(c -> {
            speler(c).sendSystemMessage(BondPaneel.rij(Component.translatable("vadsrijk.gilden.ophef.zeker").withStyle(ChatFormatting.RED),
                    BondPaneel.knop("vadsrijk.gilden.knop.ja", "/bond ophef bevestig", ChatFormatting.RED)));
            return 1;
        }).then(Commands.literal("bevestig").executes(c -> uit(c, BondActies.hefOp(speler(c))))));
        b.then(Commands.literal("verwijder").then(Commands.argument("naam", StringArgumentType.string()).suggests(BondCommando::leden)
                .executes(c -> uitEnPaneel(c, BondActies.verwijder(speler(c), StringArgumentType.getString(c, "naam")), () -> BondPaneel.leden(sp(c))))));
        b.then(Commands.literal("bevorder").then(Commands.argument("naam", StringArgumentType.string()).suggests(BondCommando::leden)
                .executes(c -> uitEnPaneel(c, BondActies.rang(speler(c), StringArgumentType.getString(c, "naam"), true), () -> BondPaneel.leden(sp(c))))));
        b.then(Commands.literal("degradeer").then(Commands.argument("naam", StringArgumentType.string()).suggests(BondCommando::leden)
                .executes(c -> uitEnPaneel(c, BondActies.rang(speler(c), StringArgumentType.getString(c, "naam"), false), () -> BondPaneel.leden(sp(c))))));
        b.then(Commands.literal("leider").then(Commands.argument("naam", StringArgumentType.string()).suggests(BondCommando::leden)
                .executes(c -> uitEnPaneel(c, BondActies.draagOver(speler(c), StringArgumentType.getString(c, "naam")), () -> BondPaneel.leden(sp(c))))));
        b.then(Commands.literal("chat").executes(c -> {
            ServerPlayer p = speler(c);
            if (Bonden.van(p) == null) {
                return uit(c, BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_bond"));
            }
            boolean aan = BondChat.wissel(p);
            p.sendSystemMessage(Component.translatable(aan ? "vadsrijk.gilden.chat.aan" : "vadsrijk.gilden.chat.uit").withStyle(ChatFormatting.LIGHT_PURPLE));
            return 1;
        }));
        b.then(Commands.literal("werving").executes(c -> paneel(c, BondPaneel::werving))
                .then(Commands.literal("aan").executes(c -> werving(c, true, null, null, null)))
                .then(Commands.literal("uit").executes(c -> werving(c, false, null, null, null)))
                .then(Commands.literal("open").executes(c -> werving(c, null, true, null, null)))
                .then(Commands.literal("dicht").executes(c -> werving(c, null, false, null, null)))
                .then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, 60))
                        .executes(c -> werving(c, null, null, IntegerArgumentType.getInteger(c, "n"), null))))
                .then(Commands.literal("tekst").then(Commands.argument("tekst", StringArgumentType.greedyString())
                        .executes(c -> werving(c, null, null, null, StringArgumentType.getString(c, "tekst"))))));
        b.then(Commands.literal("banier").executes(c -> paneel(c, BondPaneel::banier))
                .then(Commands.literal("basis").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BondCommando::kleuren)
                        .executes(c -> uitEnPaneel(c, BondActies.banierBasis(speler(c), Banier.kleur(StringArgumentType.getString(c, "kleur"))), () -> BondPaneel.banier(sp(c))))))
                .then(Commands.literal("laag").then(Commands.argument("n", IntegerArgumentType.integer(1, Banier.MAX_LAGEN))
                        .then(Commands.argument("patroon", StringArgumentType.word()).suggests(BondCommando::patronen)
                                .then(Commands.argument("kleur", StringArgumentType.word()).suggests(BondCommando::kleuren).executes(c -> uitEnPaneel(c,
                                        BondActies.banierLaag(speler(c), IntegerArgumentType.getInteger(c, "n") - 1, StringArgumentType.getString(c, "patroon"),
                                                Banier.kleur(StringArgumentType.getString(c, "kleur"))), () -> BondPaneel.banier(sp(c))))))))
                .then(Commands.literal("weg").then(Commands.argument("n", IntegerArgumentType.integer(1, Banier.MAX_LAGEN)).executes(c -> uitEnPaneel(c,
                        BondActies.banierLaag(speler(c), IntegerArgumentType.getInteger(c, "n") - 1, "", DyeColor.WHITE), () -> BondPaneel.banier(sp(c)))))));
        b.then(Commands.literal("kasteel").executes(c -> paneel(c, BondPaneel::kasteel))
                .then(Commands.literal("doneer").executes(c -> uitEnPaneel(c, Kasteel.doneer(speler(c)), () -> BondPaneel.kasteel(sp(c)))))
                .then(Commands.literal("bouw").executes(c -> uit(c, Kasteel.bouwVolgende(speler(c))))));
        b.then(Commands.literal("kas").then(Commands.argument("brons", LongArgumentType.longArg(1))
                .executes(c -> uit(c, BondActies.kasGeef(speler(c), LongArgumentType.getLong(c, "brons"))))));
        b.then(Commands.literal("thuis").executes(c -> uit(c, BondTeleport.thuis(speler(c), false))));
        b.then(Commands.literal("reis").then(Commands.argument("doel", StringArgumentType.word())
                .executes(c -> uit(c, BondTeleport.reis(speler(c), StringArgumentType.getString(c, "doel"), false)))));
        event.getDispatcher().register(b);
        event.getDispatcher().register(Commands.literal("bc").then(Commands.argument("tekst", StringArgumentType.greedyString())
                .executes(c -> BondChat.zeg(speler(c), StringArgumentType.getString(c, "tekst")) ? 1 : 0)));
        event.getDispatcher().register(VadsrijkCommando.wortel(dev()));
    }

    private static ServerPlayer sp(CommandContext<CommandSourceStack> c) {
        return c.getSource().getPlayer();
    }

    private static int paneel(CommandContext<CommandSourceStack> c, java.util.function.Consumer<ServerPlayer> f) throws CommandSyntaxException {
        f.accept(speler(c));
        return 1;
    }

    private static int werving(CommandContext<CommandSourceStack> c, Boolean werft, Boolean vrij, Integer lv, String tekst) throws CommandSyntaxException {
        return uitEnPaneel(c, BondActies.werving(speler(c), werft, vrij, lv, tekst), () -> BondPaneel.werving(sp(c)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> leden(CommandContext<CommandSourceStack> c,
            com.mojang.brigadier.suggestion.SuggestionsBuilder sb) {
        ServerPlayer p = c.getSource().getPlayer();
        Bond b = p == null ? null : Bonden.van(p);
        return SharedSuggestionProvider.suggest(b == null ? java.util.List.of() : b.leden.values().stream().map(l -> l.naam).toList(), sb);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> kleuren(CommandContext<CommandSourceStack> c,
            com.mojang.brigadier.suggestion.SuggestionsBuilder sb) {
        return SharedSuggestionProvider.suggest(java.util.Arrays.stream(DyeColor.values()).map(DyeColor::getName), sb);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> patronen(CommandContext<CommandSourceStack> c,
            com.mojang.brigadier.suggestion.SuggestionsBuilder sb) {
        return SharedSuggestionProvider.suggest(Banier.PATRONEN.stream().map(x -> x[0]), sb);
    }

    // ---- dev ---------------------------------------------------------------------------------------------------------------------------

    private static void zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static Bond mijn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Bond b = Bonden.van(speler(c));
        if (b == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("je zit niet in een bond (maak er een: /guhs vadsrijk gilde maak TAG naam)"))
                    .create();
        }
        return b;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev() {
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal("gilde").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        g.executes(c -> {
            BondPaneel.toon(speler(c));
            return 1;
        });
        g.then(Commands.literal("maak").then(Commands.argument("tag", StringArgumentType.word())
                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = speler(c);
                    return uit(c, BondActies.maak(p, StringArgumentType.getString(c, "naam"), StringArgumentType.getString(c, "tag"), true, true));
                }))));
        g.then(Commands.literal("uitnodig").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
            Bond b = mijn(c);
            ServerPlayer doel = EntityArgument.getPlayer(c, "speler");
            BondActies.Uitslag u = BondActies.treedToe(doel, b);
            doel.sendSystemMessage(u.tekst());
            return uit(c, u);
        })));
        g.then(Commands.literal("xp").then(Commands.argument("n", LongArgumentType.longArg()).executes(c -> {
            Bond b = mijn(c);
            BondXp.bijXp(c.getSource().getServer(), b, LongArgumentType.getLong(c, "n"));
            zeg(c, b.naam + ": " + b.xp + " bondsxp, level " + b.level());
            return 1;
        })));
        g.then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, BondXp.MAX)).executes(c -> {
            Bond b = mijn(c);
            long doel = BondXp.DREMPELS[IntegerArgumentType.getInteger(c, "n") - 1];
            BondXp.bijXp(c.getSource().getServer(), b, doel - b.xp);
            zeg(c, b.naam + ": level " + b.level() + " (" + b.xp + " xp); bank-tabs " + BondXp.bankTabs(b.level()) + ", reiskorting "
                    + Math.round(BondXp.reisKorting(b.level()) * 100) + "%, teleport elke " + BondXp.tpMinuten(b.level()) + " min");
            return 1;
        })));
        g.then(Commands.literal("kasteel").executes(c -> {
            BondPaneel.kasteel(speler(c));
            return 1;
        }).then(Commands.literal("bouw").executes(c -> {
            Bond b = mijn(c);
            Kasteel.bouw(c.getSource().getServer(), b);
            zeg(c, "kasteel " + b.kasteel + " opnieuw gebouwd op kavel " + b.kavel + " (" + Vallei.kavelHoek(b.kavel).toShortString() + ")");
            return 1;
        })).then(Commands.literal("vol").executes(c -> {
            Bond b = mijn(c);
            Kasteel.Kosten k = Kasteel.volgende(b);
            if (k != null) {
                b.kas = Math.max(b.kas, k.kas());
                k.materialen().forEach((m, n) -> b.depot.put(m, n));
                BondXp.bijXp(c.getSource().getServer(), b, Math.max(0, BondXp.DREMPELS[k.bondLevel() - 1] - b.xp));
            }
            zeg(c, "kas, depot en bondslevel gevuld voor het volgende kasteel; klik [Bouwen] in /bond kasteel");
            return 1;
        })).then(Commands.argument("n", IntegerArgumentType.integer(1, Kasteel.MAX)).executes(c -> {
            Bond b = mijn(c);
            b.kasteel = IntegerArgumentType.getInteger(c, "n");
            Bonden.get(c.getSource().getServer()).setDirty();
            BondBank.sluitAlles(c.getSource().getServer(), b);
            Kasteel.bouw(c.getSource().getServer(), b);
            zeg(c, "kasteel nu niveau " + b.kasteel + " (" + Component.translatable(Kasteel.naamKey(b.kasteel)).getString() + ")");
            return 1;
        })));
        g.then(Commands.literal("tp").executes(c -> uit(c, BondTeleport.thuis(speler(c), true))));
        g.then(Commands.literal("vallei").executes(c -> {
            Vallei.naarVallei(speler(c));
            return 1;
        }));
        g.then(Commands.literal("poort").executes(c -> {
            Vallei.naarHoogvads(speler(c));
            return 1;
        }));
        g.then(Commands.literal("klerk").executes(c -> {
            ServerPlayer p = speler(c);
            net.minecraft.core.BlockPos k = BondNpcs.waar(BondNpcs.KLERK);
            if (k != null) {
                Vallei.zet(p, k.offset(1, 0, 0), 90);
            }
            return 1;
        }));
        g.then(Commands.literal("bank").executes(c -> {
            ServerPlayer p = speler(c);
            BondBank.open(p, mijn(c), p.blockPosition());
            return 1;
        }));
        g.then(Commands.literal("trofee").then(Commands.argument("key", StringArgumentType.greedyString()).executes(c -> {
            Bond b = mijn(c);
            Kasteel.trofee(c.getSource().getServer(), b, StringArgumentType.getString(c, "key"));
            Kasteel.trofeeen(c.getSource().getServer(), b);
            zeg(c, b.trofeeen.size() + " trofeeën");
            return 1;
        })));
        g.then(Commands.literal("week").then(Commands.literal("klaar").executes(c -> {
            Bond b = mijn(c);
            BondXp.week(b);
            for (String t : java.util.List.copyOf(b.taken)) {
                BondXp.voortgang(c.getSource().getServer(), b, t, 1_000_000);
            }
            zeg(c, "alle weektaken klaar");
            return 1;
        })).then(Commands.literal("nieuw").executes(c -> {
            Bond b = mijn(c);
            b.week = -1;
            BondXp.week(b);
            zeg(c, "nieuwe weektaken: " + b.taken);
            return 1;
        })));
        g.then(Commands.literal("rust").executes(c -> {
            ServerPlayer p = speler(c);
            BondXp.tag(p).putLong("uit", BondXp.KLOK.getAsLong() - 24 * 3_600_000L);
            BondXp.ingelogd(p);
            zeg(c, "rustpot nu " + BondXp.rust(p) + " xp (alsof je 24 uur offline was)");
            return 1;
        }));
        g.then(Commands.literal("tpklaar").executes(c -> {
            BondXp.tag(speler(c)).putLong("tp", 0L);
            zeg(c, "teleport-cooldown gewist");
            return 1;
        }));
        g.then(Commands.literal("info").executes(c -> {
            MinecraftServer s = c.getSource().getServer();
            Bonden d = Bonden.get(s);
            zeg(c, d.alle().size() + " bonden, " + d.gestempeld + " kavels gestempeld, plein " + d.plein);
            for (Bond b : d.ranglijst()) {
                zeg(c, "#" + b.id + " [" + b.tag + "] " + b.naam + ": lv " + b.level() + " (" + b.xp + " xp), " + b.leden.size() + " leden, kasteel " + b.kasteel
                        + " (gebouwd " + b.gebouwd + "), kavel " + b.kavel + ", kas " + b.kas + ", depot " + b.depot);
            }
            return 1;
        }));
        g.then(Commands.literal("export").executes(c -> {
            zeg(c, "geschreven: " + BondExport.exporteer(c.getSource().getServer()));
            return 1;
        }));
        g.then(Commands.literal("wis").executes(c -> {
            Bonden.get(c.getSource().getServer()).wisAlles();
            BondTags.ververs(c.getSource().getServer());
            zeg(c, "alle bonden vergeten (de blokken in de vallei blijven staan)");
            return 1;
        }));
        return g;
    }
}
