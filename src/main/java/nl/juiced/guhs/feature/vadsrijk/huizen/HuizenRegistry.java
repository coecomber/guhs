package nl.juiced.guhs.feature.vadsrijk.huizen;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * U-HUIZEN (round U, decision V2): player housing as an FF14-style neighbourhood, Het Knuffelkwartier. See {@link Kwartier} (the
 * heart), {@link WijkPlan} (layout), {@link Ontwerp} (house templates), {@link Rechten} + {@link Regels} (permissions), {@link Menus},
 * {@link Poorten} (the way in from every hub), {@link Figuranten} (mount, pets, trophies, Mollie), {@link RustXp}. Resources:
 * tools/features/vadsrijk_u_huizen.py (texts + the dimension).
 */
public final class HuizenRegistry {
    private HuizenRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> HuizenCommando.register(e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Regels::plaats);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Regels::breek);
        NeoForge.EVENT_BUS.addListener(Regels::gereedschap);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Regels::klikBlok);
        NeoForge.EVENT_BUS.addListener(Regels::klikItem);
        NeoForge.EVENT_BUS.addListener(Regels::klikEntiteit);
        NeoForge.EVENT_BUS.addListener(Regels::klikEntiteitPrecies);
        NeoForge.EVENT_BUS.addListener(Regels::aanval);
        NeoForge.EVENT_BUS.addListener(Regels::schade);
        NeoForge.EVENT_BUS.addListener(Regels::binnenkomen);
        NeoForge.EVENT_BUS.addListener(Regels::uitgelogd);
        NeoForge.EVENT_BUS.addListener(Regels::ingelogd);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Figuranten.wisAlles(e.getServer()));
        Praat.luister(Menus.SLEUTEL, Menus::antwoord);
        QuestMotor.KLIK_VOOR.add(Menus::klik);
        Spelers.XP_EXTRA.add((p, n) -> RustXp.bonus(p, n));
        HuisdierEntity.ZELFSTANDIG.add(Figuranten::tuinTick);
        nl.juiced.guhs.feature.vadsrijk.karakter.Karakter.VOOR_WISSEL.add(p -> Figuranten.accountTrofeeen(p));   // (the leaving character's trophies stay)
    }

    static void tick(MinecraftServer server) {
        long nu = server.getTickCount();
        if (nu % 10 == 3) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Regels.tick(p, nu);
            }
        }
        Figuranten.tick(server);
        Poorten.tick(server);
        if (nu % 20 == 9) {
            Poortglans.tick(server);
        }
    }
}
