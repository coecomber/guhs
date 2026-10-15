package nl.juiced.guhs.feature.paleizen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * bbq2 (paleizen): the game events of the three questlines.
 * <ul>
 *   <li>every tick for a player who has not paid toll: the gate of a Mika-brugpaleis near them ({@link TolQuest#poort}); once
 *       a second for a player who is looking for Knorretje: their runaway ({@link StalQuest#tick});</li>
 *   <li>a right-click on a block with a plank of the Tolwachter or the sack of feed (before the building's protection sees
 *       it: these two are the questline's own way of changing the building), with a plank into thin air (the hole), and on
 *       the tolbel;</li>
 *   <li>an ingot held out to a Nether-Mika by a friend of Mika-oma ({@link OmaQuest#ruil}).</li>
 * </ul>
 */
public final class PaleizenEvents {
    /** How often the Mika-brugpaleis near a player is looked up again (ticks). */
    private static final int ZOEK_TICKS = 40;

    private record Buurt(ServerLevel level, long tick, @Nullable StructureStart brug) {
    }

    private static final Map<UUID, Buurt> BUURT = new ConcurrentHashMap<>();

    /** The Mika-brugpaleis this player is at (looked up every two seconds; null: none). */
    @Nullable
    static StructureStart brugBij(ServerPlayer p) {
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        Buurt b = BUURT.get(p.getUUID());
        if (b == null || b.level != level || nu - b.tick >= ZOEK_TICKS || nu < b.tick) {
            b = new Buurt(level, nu, PaleisPlekken.kopie(level, PaleisPlekken.BRUGPALEIS, p.blockPosition()));
            BUURT.put(p.getUUID(), b);
        }
        return b.brug;
    }

    @SubscribeEvent
    public static void spelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (!TolQuest.magDoor(p) && !p.isSpectator() && !p.getAbilities().instabuild) {
            TolQuest.poort(p, brugBij(p));
        }
        if ((p.tickCount + p.getId()) % 20 == 0 && StalQuest.zoekt(p)) {
            StalQuest.tick(p, PaleisPlekken.kopie(p.level(), PaleisPlekken.STAL, p.blockPosition()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void klikBlok(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean vanOns = false;
        if (stack.is(PaleizenFeature.LOSSE_PLANK.get())) {
            vanOns = TolQuest.legPlank(p, stack);
        } else if (stack.is(PaleizenFeature.ZWIJNENVOER.get())) {
            vanOns = StalQuest.vulVoerbak(p, event.getPos(), stack);
        }
        if (vanOns) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else if (event.getHand() == InteractionHand.MAIN_HAND) {
            TolQuest.bel(p, event.getPos());   // (the bell rings by itself: the click goes on)
        }
    }

    /** A plank of the Tolwachter aimed into the hole itself (no block under the crosshair): the same as a click on the deck. */
    @SubscribeEvent
    public static void klikLucht(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer p && event.getItemStack().is(PaleizenFeature.LOSSE_PLANK.get())
                && TolQuest.legPlank(p, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Before the Nether-Mika's own barter handler (feature/spiesburcht/NetherMikaRuil, normal priority). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void klikWezen(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer p) {
            OmaQuest.ruil(p, event.getTarget(), event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void weg(PlayerEvent.PlayerLoggedOutEvent event) {
        BUURT.remove(event.getEntity().getUUID());
        TolQuest.vergeet(event.getEntity().getUUID());
    }

    private PaleizenEvents() {
    }
}
