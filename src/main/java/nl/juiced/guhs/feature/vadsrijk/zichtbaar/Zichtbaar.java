package nl.juiced.guhs.feature.vadsrijk.zichtbaar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * U-ZICHTBAAR server side: keeps every client up to date about what each player wears in the virtual slots (cape, gloves,
 * armband, rings, necklace, earring), so the pieces can be DRAWN on the player (ZichtbaarLaag, client).
 * <ul>
 *   <li>The list = the item ids in {@link Looks#VOLGORDE}; outside the Vadsrijk every entry is "" (the stat gear only counts and
 *   shows in the dimension; the wardrobe's cosmetics have their own sync and go home by themselves).</li>
 *   <li>Sent on CHANGE: every {@link #ELKE} ticks each player's list is compared with the last one sent ({@link #LAATST}); a
 *   difference goes to the player and everyone tracking them. Polling (instead of hooks in every place that changes the slots:
 *   Sieraden, the smid, t-karakter's character swap, the dev commands, the dimension change) costs one map look-up per player.</li>
 *   <li>Sent on START TRACKING: a player who comes into view gets that player's list at once ({@link #voorKijker}).</li>
 * </ul>
 */
public final class Zichtbaar {
    /** How often (ticks) the worn looks are compared. */
    public static final int ELKE = 10;
    static final Map<UUID, List<String>> LAATST = new ConcurrentHashMap<>();

    private Zichtbaar() {
    }

    /** The worn looks of {@code p}; all "" when not {@code binnen}. */
    public static List<String> looks(ServerPlayer p, boolean binnen) {
        List<String> l = new ArrayList<>(Looks.VOLGORDE.size());
        for (Slot s : Looks.VOLGORDE) {
            ItemStack st = binnen ? Sieraden.get(p, s) : ItemStack.EMPTY;
            l.add(st.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
        }
        return l;
    }

    public static List<String> looks(ServerPlayer p) {
        return looks(p, Vadsrijk.in(p));
    }

    /** Compare with the last list sent; when different (or never sent) remember and send it round. Returns whether it was sent. */
    public static boolean ververs(ServerPlayer p, List<String> nu) {
        List<String> oud = LAATST.get(p.getUUID());
        if (nu.equals(oud)) {
            return false;
        }
        LAATST.put(p.getUUID(), List.copyOf(nu));
        ZichtbaarPayloads.stuurRond(p, new ZichtbaarPayloads.Uitrusting(p.getUUID(), List.copyOf(nu)));
        return true;
    }

    public static boolean ververs(ServerPlayer p) {
        return ververs(p, looks(p));
    }

    /** What a player who starts tracking {@code doel} gets: the last list sent, or (not yet sent) the list right now. */
    public static ZichtbaarPayloads.Uitrusting voorKijker(ServerPlayer doel) {
        List<String> l = LAATST.get(doel.getUUID());
        return new ZichtbaarPayloads.Uitrusting(doel.getUUID(), l != null ? l : looks(doel));
    }

    public static void vergeet(UUID u) {
        LAATST.remove(u);
    }

    // ---- events -------------------------------------------------------------------------------------------------------------------

    static void tick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % ELKE != 0) {
            return;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            ververs(p);
        }
    }

    static void startTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer doel && e.getEntity() instanceof ServerPlayer kijker) {
            ZichtbaarPayloads.stuur(kijker, voorKijker(doel));
        }
    }

    static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            vergeet(p.getUUID());   // (the new client knows nothing yet: the next poll sends it)
        }
    }

    static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        vergeet(e.getEntity().getUUID());
    }
}
