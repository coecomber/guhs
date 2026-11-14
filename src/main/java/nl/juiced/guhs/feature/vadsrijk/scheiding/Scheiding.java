package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Y-SCHEIDING (round Y, 1.10.0): the Vadsrijk and the survival worlds are truly separate. No origin stamp on items: every leak
 * channel is tested ({@code VadsrijkScheidingGameTests}) and the ones that leaked are closed here. The full matrix is in
 * SCHEIDING_RAPPORT.md. The deliberate bridges (FTB deco rewards, the Vadsrijk-vitrine, outfits as cosmetics) are other slices.
 * <ul>
 *   <li>{@link Lichaam}: vanilla xp, health, hunger, absorption, fire and potion effects swap like the inventory;</li>
 *   <li>{@link Grens}: nothing but players crosses; players only enter through Vadsrijk code; no FTB teleports or ender chest
 *   command inside; no respawn point inside;</li>
 *   <li>{@link Menus}: no survival storage (ender chest, Guhs storage) inside, no Vadsrijk storage outside;</li>
 *   <li>{@link #geefThuis}: the one rule for Guhs code that hands out SURVIVAL items: inside, they wait with the home inventory.</li>
 * </ul>
 */
public final class Scheiding {
    /** Tests: players that never count as op here (the gametest mock player may be one). */
    public static final Set<UUID> NOOIT_VRIJ = ConcurrentHashMap.newKeySet();

    private Scheiding() {
    }

    public static void register(IEventBus modBus) {
        // after kern's swap (Haken.BINNEN runs right after it) and as a safety net every second
        Haken.BINNEN.add((p, binnen) -> Lichaam.verzoen(p));
        nl.juiced.guhs.world.TikWacht.post("Scheiding:46", e -> {
            if (e.getServer().getTickCount() % 20 == 10) {
                for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
                    Lichaam.verzoen(p);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Lichaam.verzoen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (EntityTravelToDimensionEvent e) -> Grens.reis(e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (net.neoforged.neoforge.event.entity.item.ItemTossEvent e) -> Vangnet.gooi(e));
        NeoForge.EVENT_BUS.addListener((PlayerSetSpawnEvent e) -> Grens.spawnpunt(e));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (CommandEvent e) -> Grens.commando(e));
        NeoForge.EVENT_BUS.addListener(GrensCheck::commando);   // Y-INT: /guhs grenscheck (dev, op): the real border check in a client run
        NeoForge.EVENT_BUS.addListener((PlayerContainerEvent.Open e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Menus.open(p, e.getContainer());
            }
        });
    }

    /** Is the player on the Vadsrijk side (in the dimension, or the Vadsrijk inventory is live)? */
    public static boolean binnen(ServerPlayer p) {
        return Vadsrijk.in(p) || Spelers.data(p).binnen;
    }

    /** Ops (permission 2) may skip the border rules for testing. The swaps (inventory, body) apply to everyone. */
    public static boolean vrijgesteld(ServerPlayer p) {
        return !NOOIT_VRIJ.contains(p.getUUID()) && p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * THE rule for Guhs code that gives a player a SURVIVAL item (a reward, post, a refund, a quest item...) at a moment the player
     * may be anywhere: outside the Vadsrijk it goes into the inventory (else dropped at their feet); inside it waits with the
     * stored home inventory and comes out on the way home (any free slot, else the "gevonden" stash: {@code /guhs vadsrijk kist}).
     * Never put a survival item into the live inventory of someone who may be inside.
     */
    public static void geefThuis(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return;
        }
        if (Vadsrijk.in(p) && !Spelers.data(p).binnen) {
            // (just teleported in, kern's swap runs within the second: the live inventory is still the home one and will be
            // stashed as a whole; a stack that doesn't fit must not be dropped on Vadsrijk ground: it goes to "gevonden", home side)
            p.getInventory().add(s);
            if (!s.isEmpty()) {
                Inventaris.bewaar(p, s);
                s.setCount(0);
            }
            return;
        }
        Inventaris.legThuis(p, s);
    }
}
