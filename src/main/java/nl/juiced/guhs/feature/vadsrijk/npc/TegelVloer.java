package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.TegelBlokken;

/**
 * 1.10.3 (npc-vloer): the map tiles as a {@link NpcVloer.Wereld}, for checks on the shipped map data (game tests). Light is
 * approximated: a cell is "lit" when a path of at most 14 light steps (1 + the block's dampening per step) leads to open sky
 * straight above, or to a light source bright enough; the sealed air pockets inside the hollowed terrain and the void under the
 * map are dark. {@link #afgesloten} tells a sealed pocket inside the terrain from a dim room or a spot under a big canopy.
 */
public final class TegelVloer implements NpcVloer.Wereld {
    private final TegelBlokken t;
    private final Map<Long, Boolean> licht = new HashMap<>();

    public TegelVloer(ServerLevel sl) {
        this.t = new TegelBlokken(sl);
    }

    public TegelBlokken blokken() {
        return t;
    }

    public boolean heeftTegel(BlockPos p) {
        return t.heeftTegel(p);
    }

    public BlockState staat(BlockPos p) {
        return t.staat(p);
    }

    public String naam(BlockPos p) {
        return BuiltInRegistries.BLOCK.getKey(staat(p).getBlock()).getPath();
    }

    @Override
    public boolean botst(BlockPos p) {
        return !staat(p).getCollisionShape(EmptyBlockGetter.INSTANCE, p).isEmpty();
    }

    /** Open sky straight above (nothing that dims light up to the top of the tile). */
    private boolean lucht(BlockPos p) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = p.getY() + 1; y < TegelBlokken.H; y++) {
            m.set(p.getX(), y, p.getZ());
            if (staat(m).getLightDampening() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * A sealed air pocket: the walkable air around this cell (no collision or a door/trapdoor/gate, 6 directions) is a closed
     * region that never reaches open sky and holds no lamp (the hollowed inside of the terrain: "under the ground"; a shop with a
     * closed door and a lantern is a room). A region larger than {@code max} cells counts as open.
     */
    public boolean afgesloten(BlockPos start, int max) {
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        rij.add(start);
        gezien.add(start.asLong());
        while (!rij.isEmpty()) {
            BlockPos p = rij.poll();
            if (lucht(p)) {
                return false;
            }
            if (gezien.size() > max) {
                return false;
            }
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (q.getY() < 0 || q.getY() >= TegelBlokken.H) {
                    continue;
                }
                BlockState s = staat(q);
                if (s.getLightEmission() > 0) {
                    return false;   // (a lamp: a built room, not a hole in the rock)
                }
                boolean door = s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.FENCE_GATES);
                if ((door || !botst(q)) && gezien.add(q.asLong())) {
                    rij.add(q);
                }
            }
        }
        return true;
    }

    @Override
    public boolean licht(BlockPos start) {
        return licht.computeIfAbsent(start.asLong(), k -> {
            Map<Long, Integer> kosten = new HashMap<>();
            @SuppressWarnings("unchecked")
            ArrayDeque<BlockPos>[] emmers = new ArrayDeque[16];
            for (int i = 0; i < emmers.length; i++) {
                emmers[i] = new ArrayDeque<>();
            }
            emmers[0].add(start);
            kosten.put(start.asLong(), 0);
            Set<Long> klaar = new HashSet<>();
            for (int c = 0; c < 15; c++) {
                while (!emmers[c].isEmpty()) {
                    BlockPos p = emmers[c].poll();
                    if (!klaar.add(p.asLong())) {
                        continue;
                    }
                    if (lucht(p)) {
                        return true;
                    }
                    for (Direction d : Direction.values()) {
                        BlockPos q = p.relative(d);
                        BlockState s = staat(q);
                        if (s.getLightEmission() > c + 1) {
                            return true;
                        }
                        int nc = c + 1 + s.getLightDampening();
                        if (s.getLightDampening() >= 15 || nc > 14 || q.getY() < 0) {
                            continue;
                        }
                        Integer oud = kosten.get(q.asLong());
                        if (oud == null || nc < oud) {
                            kosten.put(q.asLong(), nc);
                            emmers[nc].add(q);
                        }
                    }
                }
            }
            return false;
        });
    }
}
