package nl.juiced.guhs.feature.beroepen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * What the beroepen characters share: finding their things around them (a block scan, remembered in roleData), a path
 * over the ground (for the Knabbeldief's paw prints), and a "session" (one helper at a time for the jobs that change the
 * world: the fires, the paw prints) that ends by itself when the helper walks away or takes too long.
 */
final class BeroepenHulp {
    /** A session ends when its helper has been gone (offline, another dimension, far away) this long... */
    static final int WEG = 20 * 60;
    /** ...or after this long anyway. */
    static final int MAX = 20 * 60 * 12;
    static final double VER = 80;

    // --- scanning ------------------------------------------------------------------------------------------------------

    /** All positions within r sideways and down..up around `at` whose state matches (loaded chunks only). */
    static List<BlockPos> zoek(ServerLevel level, BlockPos at, int r, int down, int up, Predicate<BlockState> wat) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (!level.hasChunkAt(at.offset(x, 0, z))) {
                    continue;
                }
                for (int y = -down; y <= up; y++) {
                    p.set(at.getX() + x, at.getY() + y, at.getZ() + z);
                    if (wat.test(level.getBlockState(p))) {
                        out.add(p.immutable());
                    }
                }
            }
        }
        return out;
    }

    static long[] longs(List<BlockPos> ps) {
        long[] out = new long[ps.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ps.get(i).asLong();
        }
        return out;
    }

    static List<BlockPos> posities(CompoundTag tag, String key) {
        List<BlockPos> out = new ArrayList<>();
        for (long l : tag.getLongArray(key)) {
            out.add(BlockPos.of(l));
        }
        return out;
    }

    // --- walking -------------------------------------------------------------------------------------------------------

    /** Can a body be in this block (air, plants, doors, carpets, markers, paw prints...)? */
    static boolean doorloopbaar(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.getFluidState().isEmpty()) {
            return false;
        }
        if (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        VoxelShape shape = s.getCollisionShape(level, p);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.1875;
    }

    /** Can you stand here (feet at p)? */
    static boolean staanbaar(ServerLevel level, BlockPos p) {
        if (!doorloopbaar(level, p) || !doorloopbaar(level, p.above())) {
            return false;
        }
        BlockState onder = level.getBlockState(p.below());
        VoxelShape shape = onder.getCollisionShape(level, p.below());
        boolean dun = level.getBlockState(p).getCollisionShape(level, p).max(Direction.Axis.Y) > 0;   // (a carpet: that's the floor)
        return dun || (!shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.5 && !(onder.getBlock() instanceof DoorBlock));
    }

    /**
     * A walking route over the ground from `van` to `naar` (both feet positions): up or down one block per step, within
     * `r` blocks of `van`, at most `max` spots looked at. Empty when there's none.
     */
    static List<BlockPos> route(ServerLevel level, BlockPos van, BlockPos naar, int r, int max) {
        Map<BlockPos, BlockPos> vorige = new HashMap<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        vorige.put(van, van);
        todo.add(van);
        int gezien = 0;
        while (!todo.isEmpty() && gezien++ < max) {
            BlockPos p = todo.poll();
            if (p.equals(naar)) {
                List<BlockPos> weg = new ArrayList<>();
                for (BlockPos q = p; !q.equals(van); q = vorige.get(q)) {
                    weg.add(q);
                }
                weg.add(van);
                java.util.Collections.reverse(weg);
                return weg;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[] {0, 1, -1}) {
                    BlockPos n = p.relative(d).above(dy);
                    if (vorige.containsKey(n) || Math.abs(n.getX() - van.getX()) > r || Math.abs(n.getZ() - van.getZ()) > r) {
                        continue;
                    }
                    if (dy == 1 && !doorloopbaar(level, p.above(2))) {
                        continue;
                    }
                    if (dy == -1 && !doorloopbaar(level, p.relative(d)) ) {
                        continue;
                    }
                    if (n.equals(naar) ? doorloopbaar(level, n) : staanbaar(level, n)) {
                        vorige.put(n, p);
                        todo.add(n);
                        break;
                    }
                }
            }
        }
        return List.of();
    }

    // --- one helper at a time ------------------------------------------------------------------------------------------

    @Nullable
    static UUID speler(GuhNpcEntity npc) {
        return npc.roleData.hasUUID("Speler") ? npc.roleData.getUUID("Speler") : null;
    }

    @Nullable
    static ServerPlayer online(GuhNpcEntity npc) {
        UUID id = speler(npc);
        return id != null && npc.level() instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(id) : null;
    }

    static void begin(GuhNpcEntity npc, ServerPlayer player) {
        long now = npc.level().getGameTime();
        npc.roleData.putUUID("Speler", player.getUUID());
        npc.roleData.putLong("Sinds", now);
        npc.roleData.putLong("Laatst", now);
    }

    static void eind(GuhNpcEntity npc) {
        npc.roleData.remove("Speler");
        npc.roleData.remove("Sinds");
        npc.roleData.remove("Laatst");
    }

    /** Is someone else busy with this character's job (true: say so)? */
    static boolean bezet(GuhNpcEntity npc, ServerPlayer player) {
        UUID id = speler(npc);
        return id != null && !id.equals(player.getUUID()) && !verlopen(npc);
    }

    /** Has the session run out (its helper gone too long, or it took too long)? Also keeps "last seen" up to date. */
    static boolean verlopen(GuhNpcEntity npc) {
        if (speler(npc) == null) {
            return false;
        }
        long now = npc.level().getGameTime();
        ServerPlayer p = online(npc);
        if (p != null && p.level() == npc.level() && p.distanceTo(npc) < VER) {
            npc.roleData.putLong("Laatst", now);
        }
        return now - npc.roleData.getLong("Laatst") > WEG || now - npc.roleData.getLong("Sinds") > MAX;
    }

    private BeroepenHulp() {
    }
}
