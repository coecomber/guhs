package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Het Vadsrijk, I-VOLTOOIING: the Verkenningsboek ({@link Voltooiing}). Registers the two zone pages + their titles, the two
 * hooks that remember what nobody stored yet (areas walked into, campfires lit), a check every 5 s for milestones (25/50/75 %
 * toasts, 100 % = title + Vadsmunten), the payloads and the dev command
 * {@code /guhs vadsrijk boek [scherm | tel | check | reset]}.
 */
public final class VoltooiingRegistry {
    /** Ticks between milestone checks (per player in the dimension). */
    public static final int CHECK = 100;

    private VoltooiingRegistry() {
    }

    public static void register(IEventBus modBus) {
        ZoneBoeken.registreer();
        Haken.GEBIED_BETREDEN.add(Voltooiing::markeerGebied);
        Haken.KAMPVUUR.add(Voltooiing::kampvuur);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null || sl.getGameTime() % 20 != 7) {
                return;
            }
            for (ServerPlayer p : sl.players()) {
                // spread the players over the 5 s; never during a scene (the title card would be lost behind it)
                if ((sl.getGameTime() / 20 + p.getId()) % (CHECK / 20) == 0 && !RijkScenes.bezig(p)) {
                    Voltooiing.controleer(p);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(VoltooiingRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        VoltooiingPayloads.register(registrar);
    }

    /** Sends the book (the key, the tab, the command). Also checks milestones first, so the book and the toasts agree. */
    public static void open(ServerPlayer p) {
        Voltooiing.controleer(p);
        VoltooiingPayloads.stuur(p, new VoltooiingPayloads.Boek(Voltooiing.schermData(p)));
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("boek").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("scherm").executes(c -> {
            open(c.getSource().getPlayerOrException());
            return 1;
        }));
        m.then(Commands.literal("tel").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Voltooiing.ZoneBoek z : Voltooiing.zones()) {
                Voltooiing.Telling t = Voltooiing.tel(p, z);
                StringBuilder sb = new StringBuilder(z.id() + ": " + t.klaar() + "/" + t.totaal() + " (" + t.procent() + "%)");
                for (Voltooiing.Categorie cat : Voltooiing.categorieen(z)) {
                    Voltooiing.Telling ct = Voltooiing.tel(p, cat);
                    sb.append("  ").append(cat.id()).append(' ').append(ct.klaar()).append('/').append(ct.totaal());
                }
                c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            }
            return 1;
        }));
        m.then(Commands.literal("check").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var nieuw = Voltooiing.controleer(p);
            c.getSource().sendSuccess(() -> Component.literal("nieuwe mijlpalen: " + nieuw), false);
            return 1;
        }));
        m.then(Commands.literal("reset").executes(c -> {
            Voltooiing.reset(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal("verkenningsboek gewist (mijlpalen, beloningen, gebieden, kampvuren)"), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }
}
