package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;

/**
 * Het Vadsrijk, slice "economie" (CONTRACT_VADSRIJK 1): Vadsmunten (coins + HUD pop-up), the shops ({@link Winkels}), the smid
 * ({@link Smid}: stars with risk, reroll, repair; sets {@code Rollen.STER_BONUS}/{@code STER_SCHADE}), the market ({@link Markt})
 * and direct trade ({@link Handel}), mounts with stats ({@link Rijdieren}, {@link Stal}), the extra loot base types
 * ({@link EcoBasisTypes}) and the cosmetics that go back to the rest of the mod ({@link Cosmetica}, {@link Post}).
 * APIs for the npc slice: {@code Winkels.open(p, id)}, {@code Smid.open(p)}, {@code Markt.open(p)}, {@code Stal.open(p)},
 * {@code Rijdieren.geefEerste(p)}.
 */
public final class EconomieRegistry {
    /** Server: the wallet value last sent to each player (the HUD coin pop-up). */
    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private EconomieRegistry() {
    }

    public static void register(IEventBus modBus) {
        EcoSpul.ITEMS.register(modBus);
        EcoSpul.BLOCKS.register(modBus);
        EcoSpul.ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(EcoSpul.HOEDHUPPER.get(), RijdierEntity.attributen().build());
            e.put(EcoSpul.KLODDERROS.get(), RijdierEntity.attributen().build());
        });
        EcoBasisTypes.registreer();
        Winkels.registreer();
        Cosmetica.registreer();
        Rollen.STER_BONUS = SterRegels::bonus;
        Rollen.STER_SCHADE = SterRegels::schade;

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> EcoCommando.register(e));
        NeoForge.EVENT_BUS.addListener(EconomieRegistry::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Handel.annuleer(p, "vadsrijk.economie.handel.weg");
                Rijdieren.vergeet(p);
                EcoPayloads.vergeet(p.getUUID());
                Handel.vergeet(p.getUUID());
                LAATST.remove(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Markt.meld(p);
            }
        });
        // trade: sneak + right-click another player with an empty hand, in the Vadsrijk
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getHand() == InteractionHand.MAIN_HAND && e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer doel
                    && p.isShiftKeyDown() && p.getMainHandItem().isEmpty() && Vadsrijk.in(p)) {
                Handel.vraag(p, doel);
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        Haken.BINNEN.add((p, binnen) -> {
            if (!binnen) {
                Handel.annuleer(p, "vadsrijk.economie.handel.geannuleerd");
                Rijdieren.vergeet(p);
                EcoPayloads.sluit(p);
                Post.bezorg(p, true);
            }
        });
        // hit while riding: the mount runs off
        Schade.NA_KLAP.add(u -> {
            if (u.klap().doel() instanceof ServerPlayer p && u.schade() > 0) {
                Rijdieren.geraakt(p);
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        EcoPayloads.register(registrar);
    }

    private static void tick(ServerTickEvent.Post e) {
        long t = e.getServer().getTickCount();
        if (t % 10 != 0) {
            return;
        }
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (Vadsrijk.in(p)) {
                long m = Spelers.data(p).munten;
                Long oud = LAATST.put(p.getUUID(), m);
                if (oud == null || oud != m) {
                    EcoPayloads.stuurMunten(p, m);
                }
            } else if (t % 100 == 0) {
                Post.bezorg(p, false);
            }
        }
    }
}
