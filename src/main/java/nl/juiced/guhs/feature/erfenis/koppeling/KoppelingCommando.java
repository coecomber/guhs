package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.erfenis.ErfenisVadskracht;
import nl.juiced.guhs.feature.erfenis.LeverkistPost;
import nl.juiced.guhs.registry.ModItems;

/**
 * Dev commands (op) of the koppeling, under /guhs erfenis:
 * <ul>
 *   <li>{@code brief} the Postguh brings the makelaarsbrief now; {@code brief status} the clock; {@code brief reset} back to
 *       "waiting" (clock 0); {@code brief klok <minuten>} sets the clock (e.g. 29 = the letter in a minute)</li>
 *   <li>{@code leverpost [n]} sends n (default 3) test parcels to your Leverkisten (cheese, wool, logs); {@code leverpost info}</li>
 *   <li>{@code dorpszender} what arrives in your village from your Dorpszenders</li>
 *   <li>{@code decobon} how many Decobonnen you have</li>
 * </ul>
 */
final class KoppelingCommando {
    private KoppelingCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("erfenis")
                .then(Commands.literal("brief").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Makelaarsbrief.bezorg(p);
                            KoppelingData d = KoppelingData.get(c.getSource().getServer());
                            KoppelingData.Speler s = d.speler(p.getUUID());
                            if (s.briefStaat == KoppelingData.WACHT) {
                                s.briefStaat = KoppelingData.BEZORGD;
                                s.briefTicks = Math.max(s.briefTicks, Makelaarsbrief.WACHTTIJD);
                            }
                            d.setDirty();
                            return 1;
                        })
                        .then(Commands.literal("status").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            MinecraftServer server = c.getSource().getServer();
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.brief_status",
                                    Makelaarsbrief.staat(server, p.getUUID()), Makelaarsbrief.ticks(server, p.getUUID()) / 1200,
                                    Makelaarsbrief.WACHTTIJD / 1200), false);
                            return 1;
                        }))
                        .then(Commands.literal("reset").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            KoppelingData d = KoppelingData.get(c.getSource().getServer());
                            KoppelingData.Speler s = d.speler(p.getUUID());
                            s.briefStaat = KoppelingData.WACHT;
                            s.briefTicks = 0;
                            d.setDirty();
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.brief_reset"), false);
                            return 1;
                        }))
                        .then(Commands.literal("klok").then(Commands.argument("minuten", IntegerArgumentType.integer(0, 600)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            KoppelingData d = KoppelingData.get(c.getSource().getServer());
                            int min = IntegerArgumentType.getInteger(c, "minuten");
                            d.speler(p.getUUID()).briefTicks = min * 1200;
                            d.setDirty();
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.brief_klok", min), false);
                            return 1;
                        }))))
                .then(Commands.literal("leverpost").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> leverpost(c.getSource(), 3))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(c -> leverpost(c.getSource(),
                                IntegerArgumentType.getInteger(c, "n"))))
                        .then(Commands.literal("info").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            MinecraftServer server = c.getSource().getServer();
                            List<GlobalPos> kisten = Leverkisten.kisten(server, p.getUUID());
                            c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.leverpost_info", kisten.size(),
                                    LeverkistPost.wacht(server, p.getUUID())), false);
                            return 1;
                        })))
                .then(Commands.literal("dorpszender").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int vk = ErfenisVadskracht.beschikbaar(c.getSource().getServer(), p.getUUID());
                    c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.dorpszender", vk), false);
                    return 1;
                }))
                .then(Commands.literal("decobon").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = Dorpsbeloningen.decobonnen(c.getSource().getServer(), p.getUUID());
                    c.getSource().sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.decobon", n), false);
                    return 1;
                }))));
    }

    private static int leverpost(CommandSourceStack src, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        UUID id = p.getUUID();
        List<ItemStack> spul = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            spul.add(switch (i % 3) {
                case 0 -> new ItemStack(ModItems.KAAS_KNABBELS.get(), 3);
                case 1 -> new ItemStack(Items.WHITE_WOOL, 16);
                default -> new ItemStack(Items.OAK_LOG, 8);
            });
        }
        LeverkistPost.verstuur(src.getServer(), id, spul);
        src.sendSuccess(() -> Component.translatable("erfenis.koppeling.cmd.leverpost", n, LeverkistPost.wacht(src.getServer(), id)), false);
        return 1;
    }
}
