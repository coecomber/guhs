package nl.juiced.guhs.feature.vadsrijk.economie;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * /guhs vadsrijk eco — for everyone: {@code handel <speler>}, {@code handel accepteer}.
 * Dev (op 2): {@code winkel <id>}, {@code smid}, {@code markt}, {@code stal}, {@code rijdier <soort>}, {@code rijdierxp <n>},
 * {@code spul} (smid materials), {@code ster <n>} / {@code verpluis} (the held piece), {@code post} (deliver now), {@code ga <plek>}.
 */
final class EcoCommando {
    private EcoCommando() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> eco = Commands.literal("eco");
        eco.then(Commands.literal("handel")
                .then(Commands.literal("accepteer").executes(c -> {
                    Handel.accepteer(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    Handel.vraag(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "speler"));
                    return 1;
                })));
        eco.then(dev("winkel").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Winkels.ids(), b)).executes(c -> {
                    Winkels.open(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"));
                    return 1;
                })));
        eco.then(dev("smid").executes(c -> {
            Smid.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        eco.then(dev("markt").executes(c -> {
            Markt.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        eco.then(dev("stal").executes(c -> {
            Stal.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        eco.then(dev("rijdier").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(RijdierSoort.values()).map(RijdierSoort::id), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RijdierSoort s = RijdierSoort.van(StringArgumentType.getString(c, "soort"));
                    if (s == RijdierSoort.HOEDHUPPER) {
                        Rijdieren.geefEerste(p);
                    } else if (s != null) {
                        Rijdieren.geef(p, s);
                    }
                    return 1;
                })));
        eco.then(dev("rijdierxp").then(Commands.argument("n", IntegerArgumentType.integer(1, 100000)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            RijdierSoort s = Rijdieren.actief(p);
            if (s != null) {
                Rijdieren.geefXp(p, s, IntegerArgumentType.getInteger(c, "n"));
            }
            return 1;
        })));
        eco.then(dev("spul").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 30));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.HERROLKNABBEL.get(), 8));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.PLUISDRAAD.get(), 12));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.VADSDRANKJE.get(), 5));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.MUNT_ZILVER.get(), 10));
            return 1;
        }));
        eco.then(dev("ster").then(Commands.argument("n", IntegerArgumentType.integer(0, SterRegels.MAX)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Uitrusting u = Rollen.van(p.getMainHandItem());
            if (u != null) {
                p.getMainHandItem().set(KernRegistry.UITRUSTING.get(), u.metSterren(IntegerArgumentType.getInteger(c, "n")));
            }
            return 1;
        })));
        eco.then(dev("verpluis").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Uitrusting u = Rollen.van(p.getMainHandItem());
            if (u != null) {
                p.getMainHandItem().set(KernRegistry.UITRUSTING.get(), u.metVerpluisd(true));
            }
            return 1;
        }));
        eco.then(dev("post").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = Post.bezorg(p, true);
            c.getSource().sendSuccess(() -> Component.literal("post: " + n + " bezorgd, " + Post.aantal(p) + " wacht nog"), false);
            return n;
        }));
        eco.then(dev("ga").then(Commands.argument("plek", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"smid", "drankjes", "uitrusting", "markt", "cosmetica", "stal"}, b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos pos = switch (StringArgumentType.getString(c, "plek")) {
                        case "smid" -> EconomiePlekken.SMID;
                        case "drankjes" -> EconomiePlekken.DRANKJES;
                        case "uitrusting" -> EconomiePlekken.UITRUSTING;
                        case "markt" -> EconomiePlekken.MARKT;
                        case "cosmetica" -> EconomiePlekken.COSMETICA;
                        default -> EconomiePlekken.STAL;
                    };
                    p.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    return 1;
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(eco));
    }
}
