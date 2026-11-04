package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * The soft lantern rule of the Wortelduister (ZONE3_PLAN §4.5, user decision "lantaarn zacht"): a creature outside every light is
 * a dark silhouette and takes -50 % damage. Light is:
 * <ul>
 *   <li>a player carrying a Nestlantaarn (any item whose id contains "nestlantaarn", in hand or in the inventory; z3-npc/systemen
 *       owns the item) within {@link #STRAAL} blocks: a party shares its light simply because every carrier counts;</li>
 *   <li>bright block light ({@link #BLOK_LICHT}+) at the spot: the pink veins, lit cave lanterns, glow mushrooms;</li>
 *   <li>anything another slice adds to {@link #BRONNEN} (e.g. lit lantern posts in the Pelszaal or the Hart: bazen) or
 *       {@link #DRAGERS} (other items/statuses that make a player a light).</li>
 * </ul>
 * Public so bosses can use the same rule ({@link #inLicht}).
 */
public final class Lantaarnlicht {
    public static final double STRAAL = 12;
    public static final int BLOK_LICHT = 10;
    public static final double DONKER_FACTOR = 0.5;

    /** Extra light sources (level, spot) -> lit? */
    public static final List<BiPredicate<ServerLevel, Vec3>> BRONNEN = new CopyOnWriteArrayList<>();
    /** Extra "this player carries light" rules. */
    public static final List<Predicate<Player>> DRAGERS = new CopyOnWriteArrayList<>();

    private static final Map<UUID, long[]> CACHE = new HashMap<>();

    private Lantaarnlicht() {
    }

    /** Is this spot lit (a lantern carrier near, bright block light, or an extra source)? */
    public static boolean inLicht(ServerLevel sl, Vec3 pos) {
        BlockPos b = BlockPos.containing(pos);
        if (sl.isLoaded(b) && sl.getBrightness(LightLayer.BLOCK, b) >= BLOK_LICHT) {
            return true;
        }
        for (Player p : sl.players()) {
            if (!p.isSpectator() && p.isAlive() && p.position().distanceToSqr(pos) <= STRAAL * STRAAL && draagtLicht(p)) {
                return true;
            }
        }
        for (BiPredicate<ServerLevel, Vec3> bron : BRONNEN) {
            if (bron.test(sl, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Does this player carry a lantern? (Cached per second: the inventory scan is not free.) */
    public static boolean draagtLicht(Player p) {
        long t = p.level().getGameTime() / 20;
        long[] c = CACHE.get(p.getUUID());
        if (c != null && c[0] == t) {
            return c[1] == 1;
        }
        boolean ja = false;
        for (Predicate<Player> d : DRAGERS) {
            if (d.test(p)) {
                ja = true;
                break;
            }
        }
        if (!ja) {
            ja = lantaarn(p.getMainHandItem()) || lantaarn(p.getOffhandItem());
            for (int i = 0; !ja && i < p.getInventory().getContainerSize(); i++) {
                ja = lantaarn(p.getInventory().getItem(i));
            }
        }
        CACHE.put(p.getUUID(), new long[] {t, ja ? 1 : 0});
        return ja;
    }

    private static boolean lantaarn(ItemStack s) {
        return !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("nestlantaarn");
    }

    public static void vergeet() {
        CACHE.clear();
    }
}
