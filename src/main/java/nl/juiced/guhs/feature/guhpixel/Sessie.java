package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * One running game in its own arena. A slice extends this class and registers a {@link SpelSoort}; {@link Sessies} does
 * the rest: start order = checks, arena, then per player {@link Kluis#bewaar}, teleport to the arena start and
 * {@link #uitrusting}, then {@link #begin}; end order per player = {@link Kluis#herstel}, teleport back, {@link #spelerWeg}.
 * A session never survives a logout or a restart and nobody can re-join; a session without players stops by itself.
 * <p>
 * Rules for subclasses: never give XP, remove your own potion effects in {@link #spelerWeg}, spawn only entities that are
 * {@code setPersistenceRequired()} or display entities, and keep them inside {@link Arena#doos} (they are removed when the
 * arena goes back).
 */
public abstract class Sessie {
    private final SessieStart start;
    private final List<ServerPlayer> spelers;
    int ticks;
    boolean gestopt;

    protected Sessie(SessieStart start) {
        this.start = start;
        this.spelers = new ArrayList<>(start.spelers());
    }

    public final UUID id() {
        return start.id();
    }

    public final SpelSoort soort() {
        return start.soort();
    }

    public final Arena arena() {
        return start.arena();
    }

    public final ServerLevel level() {
        return start.arena().level();
    }

    /** The players still in the game (a copy: safe to loop over while players leave). */
    public final List<ServerPlayer> spelers() {
        return List.copyOf(spelers);
    }

    public final boolean speelt(ServerPlayer p) {
        return spelers.contains(p);
    }

    /** Server ticks since {@link #begin}. */
    public final int ticks() {
        return ticks;
    }

    public final CompoundTag opties() {
        return start.opties();
    }

    public final boolean isGestopt() {
        return gestopt;
    }

    /** Give the game items; the inventory is empty at this point. */
    protected void uitrusting(ServerPlayer p) {
    }

    /** All players are in the arena. */
    protected abstract void begin();

    /** Every server tick. */
    protected abstract void tick();

    /** This player is out; they already have their own inventory back. */
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
    }

    /** The last call: remove your own entities and blocks; the arena goes back afterwards. */
    protected void einde() {
    }

    /** Where a player who fell into the void is put (default: the arena start). */
    public Vec3 terugzetPlek(ServerPlayer p) {
        return arena().start();
    }

    public float terugzetYaw(ServerPlayer p) {
        return arena().soort().startYaw();
    }

    public boolean magBreken(ServerPlayer p, BlockPos pos, BlockState s) {
        return false;
    }

    public boolean magPlaatsen(ServerPlayer p, BlockPos pos, BlockState s) {
        return false;
    }

    /** Right-click on a block (a bed, a panel, a chest). */
    public boolean magGebruiken(ServerPlayer p, BlockPos pos, BlockState s) {
        return false;
    }

    /** Right-click on an entity. */
    public boolean magEntiteit(ServerPlayer p, Entity e) {
        return true;
    }

    /** This player is done: inventory back, to the lobby anchor of the game. */
    public final void klaar(ServerPlayer p) {
        Sessies.verlaat(p, Vertrek.KLAAR);
    }

    /** Everybody out, {@link #einde}, the arena goes back. */
    public final void stop() {
        Sessies.stop(this, Vertrek.GESTOPT);
    }

    final void verwijder(ServerPlayer p) {
        spelers.remove(p);
    }

    final boolean leeg() {
        return spelers.isEmpty();
    }
}
