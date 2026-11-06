package nl.juiced.guhs.feature.vadsrijk.tapijt;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * U-TAPIJT: wires the flying carpet in. The entity itself is registered by economie.EcoSpul (one per RijdierSoort; the TAPIJT
 * species gets {@link TapijtEntity}); its looks by {@link Tapijt#registreerUiterlijk} (from rijdier.Uiterlijk). Here: the NPC
 * role {@code z6_tapijt_geef} (zone 6: Tapijtweefster Knoopje), the dev command {@code /guhs vadsrijk tapijt ...}, the tuning
 * sync to the players and its payload.
 */
public final class TapijtRegistry {
    private TapijtRegistry() {
    }

    public static void register(IEventBus modBus) {
        Tapijt.registreerUiterlijk();
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("z6_tapijt_geef", Tapijt::geef);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> TapijtCommando.register(e));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 7) {
                Tapijt.spelerTik(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Tapijt.weg(e.getEntity().getUUID()));
    }

    public static void payloads(PayloadRegistrar r) {
        TapijtPayloads.register(r);
    }
}
