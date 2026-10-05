package nl.juiced.guhs.feature.verhaal;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.6): the next-goal pointer. The Superkompas entry "Mijn verhaal" and a companion
 * (Sam-guh) ask where the player's followed questline wants them to go:
 * <pre>
 * Doel d = Doelen.van(p);                       // of the followed line (null: nothing to do, or no goal for this step)
 * BlockPos daar = d == null ? null : Doelen.zoek(p, d);   // in the world (null: in another dimension, or not to be found)
 * BlockPos wijs = Doelen.wijs(p);               // what a compass shows: the goal, or the portal last used when it is elsewhere
 * </pre>
 * A structure that Guhdalfs sluier still hides for the player is never a goal.
 */
public final class Doelen {
    /** How long a structure lookup is kept per player (ticks). */
    public static final int ZOEK_TICKS = 600;
    /** GuhQuests.saved: per dimension the portal the player used last ({dimension id: block position}). */
    private static final String PORTALEN = "guhs_verhaal_portalen";

    private record Gezocht(String structuur, ResourceKey<Level> dim, Optional<BlockPos> plek, long tick, BlockPos vanaf) {
    }

    private static final Map<UUID, Gezocht> GEZOCHT = new ConcurrentHashMap<>();

    /** The goal of the line this player follows (null: none). */
    @Nullable
    public static Doel van(ServerPlayer p) {
        Verhaallijn l = Verhaallijnen.gevolgd(p);
        return l == null ? null : van(p, l);
    }

    /** The goal of this line for this player (null: none for the step, done, or hidden behind the sluier). */
    @Nullable
    public static Doel van(ServerPlayer p, Verhaallijn l) {
        Doel d = l.doel(p);
        if (d != null && d.structuur() != null && Sluiers.isVerborgen(p, d.structuur())) {
            return null;
        }
        return d;
    }

    /**
     * Where the goal is in the world: its spot, or the middle of the nearest copy of its structure (looked up at most every
     * {@link #ZOEK_TICKS} ticks per player, or after walking 96 blocks). Null: the goal is in another dimension, the
     * structure is hidden for this player, or there is none.
     */
    @Nullable
    public static BlockPos zoek(ServerPlayer p, Doel d) {
        if (d.dim() != p.level().dimension()) {
            return null;
        }
        if (d.plek() != null) {
            return d.plek();
        }
        if (d.structuur() == null || Sluiers.isVerborgen(p, d.structuur())) {
            return null;
        }
        long nu = p.level().getServer().getTickCount();
        Gezocht g = GEZOCHT.get(p.getUUID());
        if (g != null && g.structuur().equals(d.structuur()) && g.dim() == d.dim() && nu - g.tick() < ZOEK_TICKS
                && g.vanaf().closerThan(p.blockPosition(), 96)) {
            return g.plek().orElse(null);
        }
        BlockPos gevonden = GuhCompassItem.findCenter(p.level(), ResourceKey.create(Registries.STRUCTURE, Guhs.id(d.structuur())), p.blockPosition());
        GEZOCHT.put(p.getUUID(), new Gezocht(d.structuur(), d.dim(), Optional.ofNullable(gevonden), nu, p.blockPosition()));
        return gevonden;
    }

    /**
     * What "Mijn verhaal" points at: the goal when it is in this dimension, else the portal the player used last in this
     * dimension (the way to where the story goes on). Null: nothing to point at.
     */
    @Nullable
    public static BlockPos wijs(ServerPlayer p) {
        Doel d = van(p);
        if (d == null) {
            return null;
        }
        return d.dim() == p.level().dimension() ? zoek(p, d) : portaal(p);
    }

    /** The portal this player used last in the dimension they are in (null: none known). */
    @Nullable
    public static BlockPos portaal(ServerPlayer p) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(PORTALEN);
        String dim = p.level().dimension().identifier().toString();
        return t.contains(dim) ? BlockPos.of(t.getLongOr(dim, 0L)) : null;
    }

    private static void onthoud(ServerPlayer p) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(PORTALEN).copy();
        t.putLong(p.level().dimension().identifier().toString(), p.blockPosition().asLong());
        GuhQuests.saved(p).put(PORTALEN, t);
    }

    /** Leaving a dimension: where the player stands now is the portal they took. */
    @SubscribeEvent
    public static void onVertrek(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.isCanceled()) {
            onthoud(p);
        }
    }

    /** Arriving: this is the portal back. */
    @SubscribeEvent
    public static void onAankomst(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            onthoud(p);
            GEZOCHT.remove(p.getUUID());
        }
    }

    static void vergeet(UUID speler) {
        GEZOCHT.remove(speler);
    }

    private Doelen() {
    }
}
