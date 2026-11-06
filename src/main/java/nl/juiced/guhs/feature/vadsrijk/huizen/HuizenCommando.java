package nl.juiced.guhs.feature.vadsrijk.huizen;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Deur;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-HUIZEN: {@code /guhs vadsrijk huis ...}. Dev (op): koop, tp, wijk, reset, bezoek, bouw, rust, info, terug, groter, uiterlijk, post,
 * figuren. For everybody: vriend / ontvriend / deur (the same as the mailbox menu).
 */
final class HuizenCommando {
    private HuizenCommando() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void zeg(CommandSourceStack s, String key, Object... args) {
        s.sendSuccess(() -> Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    static void register(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> h = Commands.literal("huis");
        h.then(dev("koop").executes(c -> koop(c.getSource(), HuisSoort.HUISJE, 0))
                .then(Commands.argument("soort", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"huisje", "huis", "villa"}, b))
                        .executes(c -> koop(c.getSource(), soort(c.getArgument("soort", String.class)), 0))
                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, Ontwerp.PALETTEN.size() - 1))
                                .executes(c -> koop(c.getSource(), soort(c.getArgument("soort", String.class)), IntegerArgumentType.getInteger(c, "variant"))))));
        h.then(dev("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            HuizenData d = Kwartier.data(p.level().getServer());
            Integer k = d.sleutelVan(p.getUUID());
            return (k != null ? Kwartier.naarHuis(p, k) : Kwartier.naarWijk(p, 0)) ? 1 : 0;
        }));
        h.then(dev("wijk").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return Kwartier.naarWijk(p, Kwartier.data(p.level().getServer()).wijkMetPlek()) ? 1 : 0;
        }).then(Commands.literal("nieuw").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            HuizenData d = Kwartier.data(p.level().getServer());
            return Kwartier.naarWijk(p, d.wijken) ? 1 : 0;
        })).then(Commands.argument("nummer", IntegerArgumentType.integer(1, 64)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return Kwartier.naarWijk(p, IntegerArgumentType.getInteger(c, "nummer") - 1) ? 1 : 0;
        })));
        h.then(dev("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean ok = Kwartier.reset(p.level().getServer(), p.getUUID());
            zeg(c.getSource(), ok ? "vadsrijk.huizen.cmd.reset" : "vadsrijk.huizen.cmd.geen_huis");
            return ok ? 1 : 0;
        }).then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
            boolean ok = Kwartier.reset(p.level().getServer(), p.getUUID());
            zeg(c.getSource(), ok ? "vadsrijk.huizen.cmd.reset" : "vadsrijk.huizen.cmd.geen_huis");
            return ok ? 1 : 0;
        })));
        h.then(dev("bezoek").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException(), doel = EntityArgument.getPlayer(c, "speler");
            Integer k = Kwartier.data(p.level().getServer()).sleutelVan(doel.getUUID());
            if (k == null) {
                zeg(c.getSource(), "vadsrijk.huizen.cmd.geen_huis");
                return 0;
            }
            return Kwartier.naarHuis(p, k) ? 1 : 0;
        })));
        h.then(dev("terug").executes(c -> {
            Kwartier.terug(c.getSource().getPlayerOrException());
            return 1;
        }));
        h.then(dev("bouw").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            HuizenData d = Kwartier.data(p.level().getServer());
            Integer k = d.sleutelVan(p.getUUID());
            if (k == null) {
                zeg(c.getSource(), "vadsrijk.huizen.cmd.geen_huis");
                return 0;
            }
            Kwartier.herbouw(p, d, k);
            return 1;
        }).then(Commands.literal("wijk").executes(c -> {   // (the whole ward again, e.g. after a template change)
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel l = Kwartier.level(p.level().getServer());
            HuizenData d = Kwartier.data(p.level().getServer());
            int w = WijkPlan.wijkVan(p.getX());
            if (l == null || !Kwartier.in(p) || w >= d.wijken) {
                return 0;
            }
            for (int i = 0; i < WijkPlan.KAVELS; i++) {
                Huis hu = d.huis(w * WijkPlan.KAVELS + i);
                if (hu != null) {
                    Kwartier.ruimOp(l, d, hu);
                }
            }
            WijkBouw.bouw(l, w, d);
            Figuranten.wisAlles(p.level().getServer());
            return 1;
        })));
        h.then(dev("rust").then(Commands.argument("uren", DoubleArgumentType.doubleArg(0, 200)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            long n = RustXp.geef(p, DoubleArgumentType.getDouble(c, "uren"));
            zeg(c.getSource(), "vadsrijk.huizen.rust.erbij", n, RustXp.pot(p));
            return 1;
        })));
        h.then(dev("groter").executes(c -> Kwartier.vergroot(c.getSource().getPlayerOrException()) ? 1 : 0));
        h.then(dev("uiterlijk").then(Commands.argument("variant", IntegerArgumentType.integer(0, Ontwerp.PALETTEN.size() - 1))
                .executes(c -> Kwartier.uiterlijk(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "variant")) ? 1 : 0)));
        h.then(dev("post").executes(c -> {
            int n = Kwartier.haalPost(c.getSource().getPlayerOrException());
            zeg(c.getSource(), "vadsrijk.huizen.post.gehaald", n);
            return n;
        }));
        h.then(dev("info").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            MinecraftServer server = p.level().getServer();
            HuizenData d = Kwartier.data(server);
            Integer k = d.sleutelVan(p.getUUID());
            Huis hu = k == null ? null : d.huis(k);
            zeg(c.getSource(), "vadsrijk.huizen.cmd.info", d.wijken, d.huizen.size(), k == null ? "-" : (k / WijkPlan.KAVELS + 1) + "/" + (k % WijkPlan.KAVELS + 1),
                    hu == null ? "-" : hu.soort.id() + " " + Ontwerp.palet(hu.variant).id(), hu == null ? 0 : hu.geplaatst.size(), hu == null ? 0 : hu.vrienden.size(),
                    hu == null ? "-" : hu.deur.name().toLowerCase(java.util.Locale.ROOT), RustXp.pot(p), Kwartier.postAantal(p));
            return 1;
        }));
        h.then(dev("figuren").executes(c -> {
            Figuranten.wisAlles(c.getSource().getServer());
            zeg(c.getSource(), "vadsrijk.huizen.cmd.figuren");
            return 1;
        }));
        h.then(dev("poorten").executes(c -> {
            Poorten.vergeet();
            StringBuilder b = new StringBuilder();
            for (Poorten.Hub hub : Poorten.HUBS) {
                b.append(hub.id()).append('=').append(Poorten.plek(hub.id())).append(' ');
            }
            zeg(c.getSource(), "vadsrijk.huizen.cmd.poorten", b.toString());
            return 1;
        }));
        // ---- for everybody --------------------------------------------------------------------------------------------------------
        h.then(Commands.literal("vriend").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException(), v = EntityArgument.getPlayer(c, "speler");
            HuizenData d = Kwartier.data(p.level().getServer());
            Huis hu = d.huisVan(p.getUUID());
            if (hu == null) {
                zeg(c.getSource(), "vadsrijk.huizen.cmd.geen_huis");
                return 0;
            }
            boolean nieuw = Menus.voegVriendToe(d, hu, v);
            zeg(c.getSource(), nieuw ? "vadsrijk.huizen.vrienden.toegevoegd" : "vadsrijk.huizen.vrienden.al", v.getGameProfile().name());
            if (nieuw) {
                v.sendSystemMessage(Component.translatable("vadsrijk.huizen.vrienden.jij", hu.naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return nieuw ? 1 : 0;
        })));
        h.then(Commands.literal("ontvriend").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException(), v = EntityArgument.getPlayer(c, "speler");
            HuizenData d = Kwartier.data(p.level().getServer());
            Huis hu = d.huisVan(p.getUUID());
            boolean weg = hu != null && hu.vrienden.remove(v.getUUID()) != null;
            d.setDirty();
            zeg(c.getSource(), weg ? "vadsrijk.huizen.vrienden.weg" : "vadsrijk.huizen.vrienden.geen", v.getGameProfile().name());
            return weg ? 1 : 0;
        })));
        h.then(Commands.literal("deur").then(Commands.argument("stand", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"open", "vrienden", "slot"}, b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    HuizenData d = Kwartier.data(p.level().getServer());
                    Integer k = d.sleutelVan(p.getUUID());
                    Huis hu = k == null ? null : d.huis(k);
                    if (hu == null) {
                        zeg(c.getSource(), "vadsrijk.huizen.cmd.geen_huis");
                        return 0;
                    }
                    try {
                        hu.deur = Deur.valueOf(c.getArgument("stand", String.class).toUpperCase(java.util.Locale.ROOT));
                    } catch (IllegalArgumentException x) {
                        return 0;
                    }
                    d.setDirty();
                    ServerLevel l = Kwartier.level(p.level().getServer());
                    if (l != null && hu.deur != Deur.OPEN) {
                        Regels.deurDicht(l, WijkPlan.kavel(k), hu);
                    }
                    zeg(c.getSource(), "vadsrijk.huizen.deur.nu", Component.translatable(hu.deur.key()));
                    return 1;
                })));
        e.getDispatcher().register(VadsrijkCommando.wortel(h));
    }

    private static HuisSoort soort(String id) {
        HuisSoort s = HuisSoort.van(id);
        return s == null ? HuisSoort.HUISJE : s;
    }

    private static int koop(CommandSourceStack src, HuisSoort s, int variant) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        HuizenData d = Kwartier.data(p.level().getServer());
        ServerLevel l = Kwartier.level(p.level().getServer());
        if (l == null) {
            zeg(src, "vadsrijk.huizen.geen_dimensie");
            return 0;
        }
        int k = d.eersteVrije();
        if (k < 0) {
            Kwartier.zorgWijk(l, d, d.wijken);
            k = d.eersteVrije();
        }
        return Kwartier.koop(p, k, s, variant, true) ? 1 : 0;
    }
}
