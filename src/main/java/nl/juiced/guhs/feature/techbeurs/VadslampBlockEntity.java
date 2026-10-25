package nl.juiced.guhs.feature.techbeurs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Vadslamp's block entity: a machine of {@link VadsGetallen#VADSLAMP} VK without slots. While it has vadskracht it keeps
 * up to {@link #RICHTINGEN} invisible light blocks ({@link VadslampLicht}) about {@link #BEREIK} blocks out around it:
 * <ul>
 *   <li>one ray per direction from the lamp; the light goes in the last AIR spot the ray reaches before something solid
 *       (it passes grass, torches, wire: whatever has no collision), never closer than 2 blocks: so it lights the room or
 *       cave the lamp stands in, not the one behind the wall;</li>
 *   <li>placed when the power comes, looked over every {@link #ELKE} ticks (something was built where a light was, a wall
 *       was dug away), all taken away when the power has been gone {@link #UIT_NA} ticks (a short dip at chunk load does
 *       not flicker them), when the lamp is broken, and by themselves when the lamp unloads (see {@link VadslampLicht}).</li>
 * </ul>
 * The cost: one ray walk of at most 6 block lookups per direction every {@link #ELKE} ticks, nothing else.
 */
public class VadslampBlockEntity extends MachineBlockEntity {
    /** How far out the lights go (blocks). */
    public static final int BEREIK = 6;
    /** Every this many ticks a burning lamp looks its lights over. */
    static final int ELKE = 40;
    /** Without vadskracht this long, the lights go out. */
    static final int UIT_NA = 30;
    /** The directions the lamp sends its light: a ring around it at its own height, a smaller one higher up, and one low. */
    static final int[][] RICHTINGEN = {
            {6, 0, 0}, {-6, 0, 0}, {0, 0, 6}, {0, 0, -6}, {4, 0, 4}, {4, 0, -4}, {-4, 0, 4}, {-4, 0, -4},
            {4, 3, 0}, {-4, 3, 0}, {0, 3, 4}, {0, 3, -4}, {0, 5, 0},
            {3, -2, 3}, {-3, -2, -3}, {3, -2, -3}, {-3, -2, 3}};

    /** Where its lights are now (absolute). */
    private final List<BlockPos> lichten = new ArrayList<>();
    private boolean geregistreerd;
    private int zonder;
    private int teller;

    public VadslampBlockEntity(BlockPos pos, BlockState state) {
        super(TechbeursFeature.VADSLAMP_BE.get(), pos, state, VadsGetallen.VADSLAMP, 0);
    }

    @Override
    protected boolean kanWerken() {
        return true;
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
    }

    /** Where its lights are now (tests). */
    public List<BlockPos> lichten() {
        return List.copyOf(lichten);
    }

    // =================================================================================================================
    // the lights
    // =================================================================================================================

    /** Every server tick, after the machine's own tick (VadslampBlock's ticker). */
    void lampTick() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (heeftKracht()) {
            zonder = 0;
            if (!geregistreerd) {
                geregistreerd = true;
                VadslampLicht.aan(server, worldPosition);
                teller = 0;
            }
            if (teller-- <= 0) {
                teller = ELKE + Math.floorMod(worldPosition.hashCode(), 7);
                zetLichten(server);
            }
            return;
        }
        if (!lichten.isEmpty() || geregistreerd) {
            if (!geregistreerd) {
                // (loaded with lights still around: own them for a moment, the power may come back in a second)
                geregistreerd = true;
                VadslampLicht.aan(server, worldPosition);
            }
            if (++zonder >= UIT_NA) {
                doof(server);
            }
        }
    }

    /** Puts the lights where they belong now, takes away the ones that are no longer right. */
    private void zetLichten(ServerLevel server) {
        BlockState licht = TechbeursFeature.VADSLAMP_LICHT.get().defaultBlockState();
        List<BlockPos> nieuw = new ArrayList<>(RICHTINGEN.length);
        for (int[] r : RICHTINGEN) {
            BlockPos p = plek(server, r);
            if (p == null || nieuw.contains(p)) {
                continue;
            }
            BlockState s = server.getBlockState(p);
            if (s.isAir()) {
                server.setBlock(p, licht, Block.UPDATE_ALL);
            } else if (!s.is(licht.getBlock())) {
                continue;
            }
            nieuw.add(p);
        }
        for (BlockPos oud : lichten) {
            if (!nieuw.contains(oud)) {
                weg(server, oud);
            }
        }
        if (!nieuw.equals(lichten)) {
            lichten.clear();
            lichten.addAll(nieuw);
            setChanged();
        }
    }

    /**
     * The spot for the light in this direction: walking out from the lamp, the last air (or own light) spot before a block
     * with a collision shape stops the ray; null when that is closer than 2 blocks or in an unloaded chunk.
     */
    private BlockPos plek(ServerLevel server, int[] r) {
        int n = Math.max(Math.abs(r[0]), Math.max(Math.abs(r[1]), Math.abs(r[2])));
        BlockPos beste = null;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= n; i++) {
            p.set(worldPosition.getX() + Math.round(r[0] * i / (float) n), worldPosition.getY() + Math.round(r[1] * i / (float) n),
                    worldPosition.getZ() + Math.round(r[2] * i / (float) n));
            if (server.isOutsideBuildHeight(p) || !server.isLoaded(p)) {
                break;
            }
            BlockState s = server.getBlockState(p);
            if (s.isAir() || s.is(TechbeursFeature.VADSLAMP_LICHT.get())) {
                if (Math.abs(p.getX() - worldPosition.getX()) + Math.abs(p.getY() - worldPosition.getY())
                        + Math.abs(p.getZ() - worldPosition.getZ()) >= 2) {
                    beste = p.immutable();
                }
            } else if (!s.getCollisionShape(server, p).isEmpty() || !s.getFluidState().isEmpty()) {
                break;
            }
        }
        return beste;
    }

    /** Its lights go out (another burning lamp near a light keeps that one: it is its light too). */
    private void doof(ServerLevel server) {
        geregistreerd = false;
        VadslampLicht.uit(server, worldPosition);
        for (BlockPos p : lichten) {
            if (!VadslampLicht.lampBij(server, p, worldPosition)) {
                weg(server, p);
            }
        }
        lichten.clear();
        zonder = 0;
        setChanged();
    }

    private static void weg(Level level, BlockPos p) {
        if (level.isLoaded(p) && level.getBlockState(p).is(TechbeursFeature.VADSLAMP_LICHT.get())) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Broken (however): its lights go at once. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            doof(server);
        }
    }

    /** Unloaded (or broken): it no longer owns its lights; the ones in loaded chunks go by themselves. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            VadslampLicht.uit(level, worldPosition);
            geregistreerd = false;
        }
    }

    // =================================================================================================================
    // saving: where its lights are, relative to the lamp
    // =================================================================================================================

    @Override
    protected void opslaan(ValueOutput uit) {
        int[] rel = new int[lichten.size() * 3];
        for (int i = 0; i < lichten.size(); i++) {
            BlockPos d = lichten.get(i).subtract(worldPosition);
            rel[i * 3] = d.getX();
            rel[i * 3 + 1] = d.getY();
            rel[i * 3 + 2] = d.getZ();
        }
        uit.putIntArray("Lichten", rel);
    }

    @Override
    protected void laden(ValueInput in) {
        lichten.clear();
        int[] rel = in.getIntArray("Lichten").orElse(new int[0]);
        for (int i = 0; i + 2 < rel.length; i += 3) {
            lichten.add(worldPosition.offset(rel[i], rel[i + 1], rel[i + 2]));
        }
    }
}
