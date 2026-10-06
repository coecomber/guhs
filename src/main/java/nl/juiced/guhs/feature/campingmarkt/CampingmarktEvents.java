package nl.juiced.guhs.feature.campingmarkt;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.spiesburcht.NetherMikaRuil;

/**
 * bbq2 (camping-markt): the game events of the slice. Nearly everything happens in the blocks and the roles themselves;
 * here is only what hooks into somebody else's creature: the better barter rates of a certified customer with the wild
 * Nether-Mika's (feature/spiesburcht/NetherMikaRuil is not touched: {@link Ruilmarkt#klant} looks at the click before its
 * handler does, {@link Ruilmarkt#gegooid} at what the Mika throws afterwards).
 */
public final class CampingmarktEvents {
    private CampingmarktEvents() {
    }

    /** Before the Nether-Mika's own barter handler (normal priority): who is it going to barter with? */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void klikWezen(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer p) {
            Ruilmarkt.klant(p, event.getTarget(), event.getItemStack());
        }
    }

    /** Something a Nether-Mika throws comes into the world: the present of a barter (or the bar of one that was broken off). */
    @SubscribeEvent
    public static void opJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ItemEntity item) {
            Entity gooier = item.getOwner();
            if (NetherMikaRuil.isNetherMika(gooier)) {
                Ruilmarkt.gegooid(level, (MikaEntity) gooier, item.getItem());
            }
        }
    }

    /** A haggle and a stack on the scales are forgotten when the player leaves. */
    @SubscribeEvent
    public static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        Ruilmarkt.vergeet(event.getEntity().getUUID());
    }
}
