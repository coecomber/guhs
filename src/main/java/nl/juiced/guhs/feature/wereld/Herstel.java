package nl.juiced.guhs.feature.wereld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * bbq2: shared things come back by themselves, so every player gets their turn at a quest (DESIGN 0: nothing "only for the
 * first"). What a player changes in the world for a quest (a fire lit, a block brushed away, a bridge that collapsed) is put
 * back a little later:
 * <ul>
 *   <li>{@link #na(ServerLevel, BlockPos, BlockState, int)}: this block is this state again after so many ticks;</li>
 *   <li>{@link #na(ServerLevel, BlockPos, Identifier, Rotation, int)}: a small template is placed again (the bridge).</li>
 * </ul>
 * The list is saved per dimension (SavedData {@code guhs:herstel}), so it also happens after a restart, and when the chunk
 * is not loaded at that moment it happens as soon as it is. Asking again for the same spot replaces the earlier request.
 */
public final class Herstel {
    /** The list is looked at once per this many ticks. */
    public static final int STAP = 20;

    private Herstel() {
    }

    /** One request: a block state, or a template (with its rotation), at a spot, from a game time on. */
    private static final class Verzoek {
        final BlockPos pos;
        final long vanaf;
        BlockState terug;
        Identifier template;
        Rotation draai = Rotation.NONE;

        Verzoek(BlockPos pos, long vanaf) {
            this.pos = pos;
            this.vanaf = vanaf;
        }
    }

    /** Puts {@code terug} back at {@code pos} after {@code ticks} ticks (at once when the chunk is loaded then, else when it loads). */
    public static void na(ServerLevel level, BlockPos pos, BlockState terug, int ticks) {
        Verzoek v = new Verzoek(pos.immutable(), level.getGameTime() + Math.max(0, ticks));
        v.terug = terug;
        Lijst.get(level).zet(v);
    }

    /**
     * Places the template again with its corner (template 0,0,0) at {@code hoek}, turned {@code draai}, after {@code ticks}
     * ticks. The whole template is placed (its air too), entities in it are not.
     */
    public static void na(ServerLevel level, BlockPos hoek, Identifier template, Rotation draai, int ticks) {
        Verzoek v = new Verzoek(hoek.immutable(), level.getGameTime() + Math.max(0, ticks));
        v.template = template;
        v.draai = draai;
        Lijst.get(level).zet(v);
    }

    /** Is something waiting to be put back at this spot (a block, or the corner of a template)? */
    public static boolean wacht(ServerLevel level, BlockPos pos) {
        for (Verzoek v : Lijst.get(level).verzoeken) {
            if (v.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** How many requests wait in this level. */
    public static int aantal(ServerLevel level) {
        return Lijst.get(level).verzoeken.size();
    }

    /** (tests, dev) as if this many ticks went by for every request of this level. */
    public static void verschuif(ServerLevel level, long ticks) {
        Lijst lijst = Lijst.get(level);
        List<Verzoek> nieuw = new ArrayList<>();
        for (Verzoek v : lijst.verzoeken) {
            Verzoek n = new Verzoek(v.pos, v.vanaf - ticks);
            n.terug = v.terug;
            n.template = v.template;
            n.draai = v.draai;
            nieuw.add(n);
        }
        lijst.verzoeken.clear();
        lijst.verzoeken.addAll(nieuw);
        lijst.setDirty();
    }

    /** Every {@link #STAP} ticks per level: what is due and loaded is put back. */
    static void opLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % STAP == 0) {
            verwerk(level);
        }
    }

    /** Puts back what is due now (and loaded); returns how many. */
    public static int verwerk(ServerLevel level) {
        Lijst lijst = level.getDataStorage().get(Lijst.TYPE);
        if (lijst == null || lijst.verzoeken.isEmpty()) {
            return 0;
        }
        long nu = level.getGameTime();
        int gedaan = 0;
        for (Iterator<Verzoek> it = lijst.verzoeken.iterator(); it.hasNext(); ) {
            Verzoek v = it.next();
            if (v.vanaf > nu) {
                continue;
            }
            if (v.template == null) {
                if (!level.isLoaded(v.pos)) {
                    continue;
                }
                if (v.terug != null && level.getBlockState(v.pos) != v.terug) {
                    level.setBlock(v.pos, v.terug, Block.UPDATE_ALL);
                }
            } else {
                Optional<StructureTemplate> template = level.getStructureManager().get(v.template);
                if (template.isPresent()) {
                    StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(v.draai).setMirror(Mirror.NONE).setIgnoreEntities(true);
                    BoundingBox box = template.get().getBoundingBox(settings, v.pos);
                    if (!geladen(level, box)) {
                        continue;
                    }
                    template.get().placeInWorld(level, v.pos, v.pos, settings, level.getRandom(), Block.UPDATE_ALL);
                }
            }
            it.remove();
            lijst.setDirty();
            gedaan++;
        }
        return gedaan;
    }

    private static boolean geladen(ServerLevel level, BoundingBox box) {
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** SavedData guhs:herstel (per dimension): the requests that wait. */
    public static class Lijst extends SavedData {
        public static final SavedDataType<Lijst> TYPE = GuhSavedData.tagType("herstel", Lijst::new, Lijst::load, Lijst::save);
        private final List<Verzoek> verzoeken = new ArrayList<>();

        static Lijst get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        void zet(Verzoek v) {
            verzoeken.removeIf(o -> o.pos.equals(v.pos) && (o.template == null) == (v.template == null));
            verzoeken.add(v);
            setDirty();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (Verzoek v : verzoeken) {
                CompoundTag t = new CompoundTag();
                t.putLong("Pos", v.pos.asLong());
                t.putLong("Vanaf", v.vanaf);
                if (v.template != null) {
                    t.putString("Template", v.template.toString());
                    t.putString("Draai", v.draai.name());
                } else if (v.terug != null) {
                    t.put("Blok", NbtUtils.writeBlockState(v.terug));
                }
                list.add(t);
            }
            tag.put("Verzoeken", list);
            return tag;
        }

        private static Lijst load(CompoundTag tag) {
            Lijst lijst = new Lijst();
            ListTag list = tag.getListOrEmpty("Verzoeken");
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompoundOrEmpty(i);
                Verzoek v = new Verzoek(BlockPos.of(t.getLongOr("Pos", 0L)), t.getLongOr("Vanaf", 0L));
                String template = t.getStringOr("Template", "");
                if (!template.isEmpty()) {
                    v.template = Identifier.tryParse(template);
                    try {
                        v.draai = Rotation.valueOf(t.getStringOr("Draai", "NONE"));
                    } catch (IllegalArgumentException ignored) {
                        // (no rotation)
                    }
                    if (v.template == null) {
                        continue;
                    }
                } else {
                    v.terug = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, t.getCompoundOrEmpty("Blok"));
                }
                lijst.verzoeken.add(v);
            }
            return lijst;
        }
    }
}
