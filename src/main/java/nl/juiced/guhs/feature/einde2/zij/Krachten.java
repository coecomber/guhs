package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;

/**
 * einde2 phase B (CONTRACT_E2_B 3): the ONE registry of the bending powers. Written COMPLETE by the skelet (registry,
 * cooldowns per player and power, {@link #gebruik}); the slice {@code zij} adds the key, the element wheel, the HUD icon,
 * the payloads and the four lasting powers; a chapter slice registers its story powers with {@code kan} = "inside my
 * structure" only. Server side.
 */
public final class Krachten {
    /**
     * A power: its id, its element, the item that draws its icon, its cooldown, where it can be used (asked with the
     * player's feet), and what it does (with the player's position).
     */
    public record Kracht(String id, Einde2Ids.Element element, String icoon, int afkoelTicks, BiPredicate<ServerPlayer, BlockPos> kan,
                         BiConsumer<ServerPlayer, Vec3> gebruik) {
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<String, Kracht> ALLE = new LinkedHashMap<>();
    /**
     * (zij) told right after a power was used, with the power: the side quests listen here (a wind corridor that sings, a
     * fire lock that lights, a trial that sees your pillar). A listener that throws is logged and skipped.
     */
    public static final List<BiConsumer<ServerPlayer, Kracht>> NA_GEBRUIK = new CopyOnWriteArrayList<>();
    /** Per player: power id -> the game time from which it can be used again. */
    private static final Map<UUID, Map<String, Long>> KLAAR_OP = new ConcurrentHashMap<>();

    private Krachten() {
    }

    /** Adds a power (common code or server; an id twice is a mistake). */
    public static void registreer(Kracht k) {
        synchronized (ALLE) {
            if (ALLE.containsKey(k.id())) {
                throw new IllegalStateException("The power " + k.id() + " is registered twice");
            }
            ALLE.put(k.id(), k);
        }
    }

    @Nullable
    public static Kracht van(String id) {
        synchronized (ALLE) {
            return ALLE.get(id);
        }
    }

    /** Every registered power, in the order they were registered. */
    public static List<Kracht> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.values());
        }
    }

    /** Every power whose {@code kan} says yes at this player's feet (cooling down or not), in registration order. */
    public static List<Kracht> beschikbaar(ServerPlayer p) {
        List<Kracht> uit = new ArrayList<>();
        BlockPos voeten = p.blockPosition();
        for (Kracht k : alle()) {
            if (k.kan().test(p, voeten)) {
                uit.add(k);
            }
        }
        return uit;
    }

    /** Ticks until this player can use this power again (0: now). */
    public static int afkoeling(ServerPlayer p, String id) {
        Long klaar = KLAAR_OP.getOrDefault(p.getUUID(), Map.of()).get(id);
        long nu = p.level().getGameTime();
        return klaar == null || klaar <= nu ? 0 : (int) Math.min(Integer.MAX_VALUE, klaar - nu);
    }

    /** Uses the power: only when it exists, may be used here and is not cooling down for this player. True = done. */
    public static boolean gebruik(ServerPlayer p, String id) {
        Kracht k = van(id);
        if (k == null || !k.kan().test(p, p.blockPosition()) || afkoeling(p, id) > 0) {
            return false;
        }
        KLAAR_OP.computeIfAbsent(p.getUUID(), u -> new ConcurrentHashMap<>()).put(id, p.level().getGameTime() + Math.max(0, k.afkoelTicks()));
        k.gebruik().accept(p, p.position());
        for (BiConsumer<ServerPlayer, Kracht> l : NA_GEBRUIK) {
            try {
                l.accept(p, k);
            } catch (RuntimeException ex) {
                LOGGER.warn("Guhs einde2: a listener of the power {} failed", k.id(), ex);
            }
        }
        KrachtPayloads.gebruikt(p);
        return true;
    }

    /** (fix4) the power is ready again at once (it found nothing to work on: no cooldown for a miss). */
    public static void klaar(ServerPlayer p, String id) {
        Map<String, Long> m = KLAAR_OP.get(p.getUUID());
        if (m != null) {
            m.remove(id);
        }
    }

    /** (logout, tests) forgets this player's cooldowns. */
    public static void vergeet(UUID speler) {
        KLAAR_OP.remove(speler);
    }
}
