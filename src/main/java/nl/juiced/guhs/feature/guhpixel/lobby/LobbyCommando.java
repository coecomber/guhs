package nl.juiced.guhs.feature.guhpixel.lobby;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.Toegang;

/**
 * The dev commands of the lobby slice (gamemasters; Brigadier merges them under the kern's {@code /guhs px lobby}, which
 * itself still unlocks and enters): {@code /guhs px lobby knabbels toon|alle}, {@code parkour klaar <ticks>|reset|toon},
 * {@code chat} (a lobby guh says something now), {@code kabel} (a Netwerkkabeltje), {@code welkom reset} (the greeter
 * greets you again), {@code borden} (your own boards again). Once-keys (the muntjes of knabbels and parkour) are only
 * forgotten by the kern's {@code /guhs px muntjes wis}.
 */
final class LobbyCommando {
    static void register(RegisterCommandsEvent event) {
        var lobby = Commands.literal("lobby");
        lobby.then(Commands.literal("knabbels")
                .then(Commands.literal("toon").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    StringBuilder sb = new StringBuilder("knabbels " + Knabbels.gevonden(p) + "/" + Knabbels.AANTAL + ":");
                    var kaart = LobbyKaart.van(ctx.getSource().getServer());
                    for (int i = 0; i < kaart.knabbels().size(); i++) {
                        sb.append(' ').append(i).append(Knabbels.heeft(p, i) ? "+" : "-").append('(').append(kaart.knabbels().get(i).toShortString()).append(')');
                    }
                    return zeg(ctx.getSource(), sb.toString());
                }))
                .then(Commands.literal("alle").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Toegang.ontgrendel(p);
                    var kaart = LobbyKaart.van(ctx.getSource().getServer());
                    for (int i = 0; i < Knabbels.AANTAL; i++) {
                        Knabbels.pak(p, i < kaart.knabbels().size() ? kaart.knabbels().get(i) : p.blockPosition(), i);
                    }
                    return zeg(ctx.getSource(), "knabbels " + Knabbels.gevonden(p) + "/" + Knabbels.AANTAL);
                })));
        lobby.then(Commands.literal("parkour")
                .then(Commands.literal("klaar").then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20 * 60 * 15)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Toegang.ontgrendel(p);
                    boolean record = LobbyParkour.finish(p, IntegerArgumentType.getInteger(ctx, "ticks"));
                    return zeg(ctx.getSource(), "parkour: " + LobbyParkour.tijd(LobbyParkour.best(p)) + (record ? " (record)" : "") + ", " + LobbyParkour.keren(p) + "x");
                })))
                .then(Commands.literal("reset").executes(ctx -> {
                    LobbyParkour.reset(ctx.getSource().getPlayerOrException());
                    return zeg(ctx.getSource(), "parkour gewist (de muntjes blijven betaald)");
                }))
                .then(Commands.literal("toon").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return zeg(ctx.getSource(), "parkour: best " + LobbyParkour.tijd(LobbyParkour.best(p)) + ", " + LobbyParkour.keren(p) + "x, bezig " + LobbyParkour.bezig(p)
                            + ", betaald " + Muntjes.isVerdiend(p, LobbyParkour.SLEUTEL));
                })));
        lobby.then(Commands.literal("chat").executes(ctx -> {
            ServerLevel level = Guhpixel.level(ctx.getSource().getServer());
            if (level == null) {
                return zeg(ctx.getSource(), "geen guhpixel-dimensie");
            }
            int n = Chatguhs.zeg(level, LobbySlice.OORSPRONG, level.getRandom().nextInt(Chatguhs.NAMEN), Chatguhs.volgendeRegel(level.getRandom()));
            return zeg(ctx.getSource(), "lobbychat naar " + n + " speler(s)");
        }));
        lobby.then(Commands.literal("kabel").executes(ctx -> zeg(ctx.getSource(), "kabeltje: " + Toegang.geefKabeltje(ctx.getSource().getPlayerOrException()))));
        lobby.then(Commands.literal("welkom").then(Commands.literal("reset").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            CompoundTag d = LobbySlice.data(p);
            d.remove(Welkomstguh.BEGROET);
            PxData.vuil(ctx.getSource().getServer());
            return zeg(ctx.getSource(), "de Welkomstguh kent je niet meer");
        })));
        lobby.then(Commands.literal("borden").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Zweeftekst.weg(p);
            LobbyBorden.meteen(p);
            ServerLevel level = Guhpixel.level(ctx.getSource().getServer());
            if (level != null && Guhpixel.inLobby(p)) {
                Chatguhs.zorg(level, LobbySlice.OORSPRONG);
                LobbyBorden.gedeeld(level, LobbySlice.OORSPRONG);
            }
            return zeg(ctx.getSource(), "borden ververst");
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(lobby)));
    }

    private static int zeg(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal("[px lobby] " + tekst), false);
        return 1;
    }

    private LobbyCommando() {
    }
}
