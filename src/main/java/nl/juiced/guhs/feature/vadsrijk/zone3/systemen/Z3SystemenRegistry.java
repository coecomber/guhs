package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2Winkel;

/**
 * Het Vadsrijk zone 3 "De Schemerwortel", slice "systemen" (Z3_BRIEF): layered areas ({@link Z3Lagen}), cap 50 (kern Levels),
 * xp pacing ({@link Z3Xp}), tree rows 6/7 ({@link Z3Boomrijen}), the 4th job advancement ({@link Advancement4}), zone-3 weapons
 * ({@link Z3Wapens}) and crystals ({@link Z3Kristallen}), the Premiebord + penningwinkel ({@link Premies}, {@link Premiebord}),
 * hunting contracts ({@link Contracten}), Vahoeg support + Gouden Veren ({@link Vahoeg}, {@link GoudenVeren}), wind columns +
 * glide ({@link Z3Wind}, client), and the Honingtak shop ({@link #HONINGKRAAM}). Registered after zone-2 systemen.
 * Commands (op 2): {@code /guhs vadsrijk z3systemen bord [tab] | winkel | penningen <n> | veren <n> | ruilveren | premie <slot>
 * | dag | zegel | kristal <soort> <q>}.
 */
public final class Z3SystemenRegistry {
    public static final String HONINGKRAAM = "z3_honingkraam";
    private static volatile MinecraftServer server;

    private Z3SystemenRegistry() {
    }

    public static void register(IEventBus modBus) {
        Z3Wapens.ITEMS.register(modBus);
        Z3Kristallen.ITEMS.register(modBus);
        Z3Spul.ITEMS.register(modBus);
        Z3Lagen.registreer();
        Z3Wapens.registreer();
        Z3Kristallen.init();
        Z3Boomrijen.init();
        Advancement4.init();
        winkel();
        Haken.VERSLAGEN.add(Premies::verslagen);
        Contracten.koppel(() -> server);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> server = e.getServer());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Z3Wind.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Z3Wind.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(Z3SystemenRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        Premiebord.register(registrar);
    }

    /** The Honingtak shop (z3-npc: {@code Winkels.open(p, Z3SystemenRegistry.HONINGKRAAM)}): tier-3 potions + the old tiers. */
    static void winkel() {
        Winkels.registreer(new Winkels.Winkel(HONINGKRAAM, true, List.of(
                Winkels.itemWaar("grote_vadsdrank", 300, () -> new ItemStack(Z2Winkel.GROTE_VADSDRANK.get())),
                Winkels.itemWaar("reuzen_vadsdrank", 900, () -> new ItemStack(Z3Spul.REUZEN_VADSDRANK.get())),
                Winkels.itemWaar("grote_energiedrank", 55, () -> new ItemStack(Z2Winkel.GROTE_ENERGIEDRANK.get())),
                Winkels.itemWaar("reuzen_energiedrank", 150, () -> new ItemStack(Z3Spul.REUZEN_ENERGIEDRANK.get())),
                Winkels.itemWaar("bloesemthee", 120, () -> new ItemStack(Z3Spul.BLOESEMTHEE.get())),
                Winkels.itemWaar("vahoegdrankje", 60, () -> new ItemStack(EcoSpul.VAHOEGDRANKJE.get())),
                Winkels.itemWaar("snelpootjesdrankje", 45, () -> new ItemStack(EcoSpul.SNELPOOTJESDRANKJE.get())),
                Winkels.itemWaar("verbetersteentje", 50, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())))));
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("z3systemen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("bord").executes(c -> {
            Premiebord.open(c.getSource().getPlayerOrException(), Premiebord.PREMIES);
            return 1;
        }).then(Commands.argument("tab", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                List.of(Premiebord.PREMIES, Premiebord.JACHT, Premiebord.WINKEL), b)).executes(c -> {
                    Premiebord.open(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "tab"));
                    return 1;
                })));
        k.then(Commands.literal("winkel").executes(c -> {
            Winkels.open(c.getSource().getPlayerOrException(), HONINGKRAAM);
            return 1;
        }));
        k.then(Commands.literal("penningen").then(Commands.argument("n", IntegerArgumentType.integer(-10000, 10000)).executes(c -> {
            Z3Data.geefPenningen(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
            return 1;
        })));
        k.then(Commands.literal("veren").then(Commands.argument("n", IntegerArgumentType.integer(1, 100)).executes(c -> {
            GoudenVeren.geef(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
            return 1;
        })));
        k.then(Commands.literal("ruilveren").executes(c -> GoudenVeren.ruil(c.getSource().getPlayerOrException()) ? 1 : 0));
        k.then(Commands.literal("premie").then(Commands.argument("slot", IntegerArgumentType.integer(0, 2)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int s = IntegerArgumentType.getInteger(c, "slot");
            var ids = Premies.ids(p);
            if (s < ids.size()) {
                var x = Premies.POOL.get(ids.get(s));
                var t = Premies.vandaag(p);
                int[] v = Premies.getallen(t, "voortgang");
                v[s] = x.aantal();
                Premies.zet(t, "voortgang", v);
            }
            return 1;
        })));
        k.then(Commands.literal("dag").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("dag " + Z3Data.dag() + ", week " + Z3Data.week() + ", reset over "
                    + Z3Data.secondenTotReset() / 60 + " min, contracten " + Contracten.vandaag()), false);
            return 1;
        }));
        k.then(Commands.literal("zegel").executes(c -> {
            c.getSource().getPlayerOrException().getInventory().add(new ItemStack(Z3Spul.VAHOEGZEGEL.get()));
            return 1;
        }));
        k.then(Commands.literal("kristal").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3Kristallen.SOORTEN, b))
                .then(Commands.argument("kwaliteit", IntegerArgumentType.integer(0, 2)).executes(c -> {
                    c.getSource().getPlayerOrException().getInventory().add(Kristallen.stack(StringArgumentType.getString(c, "soort"),
                            IntegerArgumentType.getInteger(c, "kwaliteit"), 1));
                    return 1;
                }))));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }
}
