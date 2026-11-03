package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The kern hooks other slices listen to (add a listener from your registry class; they are called on the server thread).
 * Quests (npc slice) count with these; the economie/bazen slices react to loot and defeats.
 */
public final class Haken {
    /** A mob was "opgevrolijkt": the mob, the players who helped (contributors, incl. the last hit). */
    public record Verslagen(VadsrijkMob mob, Set<ServerPlayer> helpers) {
    }

    public static final List<Consumer<Verslagen>> VERSLAGEN = new CopyOnWriteArrayList<>();
    /** A player reached a new level in a class: (player, class id + ":" + level). */
    public static final List<BiConsumer<ServerPlayer, String>> LEVEL_OP = new CopyOnWriteArrayList<>();
    /** A player walked into another area (Gebieden id). */
    public static final List<BiConsumer<ServerPlayer, String>> GEBIED_BETREDEN = new CopyOnWriteArrayList<>();
    /** A player got a piece of loot (already in the world as a drop, or in a chest). */
    public static final List<BiConsumer<ServerPlayer, ItemStack>> LOOT = new CopyOnWriteArrayList<>();
    /** A player lit a campfire checkpoint. */
    public static final List<BiConsumer<ServerPlayer, BlockPos>> KAMPVUUR = new CopyOnWriteArrayList<>();
    /** A status was given to an entity. */
    public static final List<BiConsumer<LivingEntity, Status>> STATUS = new CopyOnWriteArrayList<>();
    /** A player entered (true) or left (false) the dimension, after the inventory swap. */
    public static final List<BiConsumer<ServerPlayer, Boolean>> BINNEN = new CopyOnWriteArrayList<>();
    /** A player fainted (before waking at the checkpoint). */
    public static final List<Consumer<ServerPlayer>> FLAUW = new CopyOnWriteArrayList<>();

    private Haken() {
    }
}
