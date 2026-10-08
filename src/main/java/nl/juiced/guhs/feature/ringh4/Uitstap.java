package nl.juiced.guhs.feature.ringh4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.4.1 (ring-h4): the steps out of the Guhduin, also at a tree city that was generated before they existed (the official
 * server world is never made again). The banks of the river stand a whole block above the kaassaus, so whoever fell in
 * could not get out. A step out is a cell of the bank at the water's edge made one block lower (a white stone level with
 * the sauce: you float onto it) with a half step behind it up to the bank. Where they are is the builder's choice
 * (tools/features/ring_h4_bouw.py uitstappen: no cell of the sauce is more than 8 cells of swimming from one) and comes
 * with {@link Plekken#UITSTAP}; a new copy has them from its template.
 * <p>
 * While a player is near a copy (about every two seconds, {@link #rond}), every step whose chunks are loaded is made
 * ONCE per copy (remembered in SavedData {@code guhs:ringh4_uitstap}; the {@code feature/wereld/Bezetting} pattern for
 * props). A step only ever replaces plain bank: a cell that becomes a block has to be natural ground now (block tag
 * {@code guhs:wereld/natuurlijk}), a cell that becomes air has to be air, a plant or natural ground, and none may hold a
 * fluid or a block entity. If one cell of a step is anything
 * else (somebody built there, the land grew something over it) the whole step is left out at that copy (logged), and so is
 * a step that is there already. Nothing else of the copy is touched.
 */
public final class Uitstap {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** A player's surroundings are looked at once per this many ticks. */
    public static final int CHECK_TICKS = 40;

    /** One cell of a step: where it is in the template of the whole build, and what it becomes. */
    private record Cel(BlockPos lokaal, BlockState wordt) {
    }

    /** (made on first use: the blocks are registered by then) every step, its cells top-down per column. */
    private static volatile List<List<Cel>> trappen;

    private static List<List<Cel>> trappen() {
        List<List<Cel>> t = trappen;
        if (t == null) {
            List<List<Cel>> alle = new ArrayList<>();
            for (List<Plekken.Cel> trap : Plekken.UITSTAP) {
                List<Cel> cellen = new ArrayList<>();
                for (Plekken.Cel c : trap) {
                    Block blok = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(c.blok()))
                            .orElseThrow(() -> new IllegalStateException("ringh4: no block '" + c.blok() + "' for a step out of the sauce"));
                    cellen.add(new Cel(c.lokaal(), blok.defaultBlockState()));
                }
                alle.add(List.copyOf(cellen));
            }
            trappen = t = List.copyOf(alle);
        }
        return t;
    }

    /** How many steps out of the sauce a tree city has. */
    public static int aantal() {
        return Plekken.UITSTAP.size();
    }

    /** Where a block of the template (coordinates of the whole build) is in this copy. */
    static BlockPos wereld(Boomstad.Kopie kopie, BlockPos lokaal) {
        return BlockPos.containing(kopie.wereld(new Vec3(lokaal.getX() + 0.5, lokaal.getY() + 0.5, lokaal.getZ() + 0.5)));
    }

    /** (every {@link #CHECK_TICKS} ticks of a player) the copy around this player gets the steps it misses. */
    static void rond(ServerPlayer p) {
        if (p.isSpectator() || Plekken.UITSTAP.isEmpty()) {
            return;
        }
        Boomstad.Kopie kopie = Boomstad.bij(p.level(), p.blockPosition());
        if (kopie != null) {
            controleer(p.level(), kopie, -1);
        }
    }

    /** Makes the steps this copy still misses, where their chunks are loaded; returns how many were made now. */
    public static int controleer(ServerLevel level, Boomstad.Kopie kopie) {
        return controleer(level, kopie, -1);
    }

    /** Like {@link #controleer(ServerLevel, Boomstad.Kopie)}, for one step only ({@code alleen}: its number; -1: all). */
    static int controleer(ServerLevel level, Boomstad.Kopie kopie, int alleen) {
        List<List<Cel>> alle = trappen();
        Gehad data = Gehad.get(level);
        long sleutel = kopie.anker().asLong();
        long gehad = data.gehad.getOrDefault(sleutel, 0L);
        long alles = alle.size() >= 64 ? -1L : (1L << alle.size()) - 1;
        if ((gehad & alles) == alles) {
            return 0;
        }
        int gemaakt = 0;
        for (int i = 0; i < alle.size() && i < 64; i++) {
            if ((gehad & (1L << i)) != 0 || (alleen >= 0 && i != alleen)) {
                continue;
            }
            Boolean uitkomst = maak(level, kopie, alle.get(i));
            if (uitkomst == null) {
                continue;                                     // (not loaded: later)
            }
            gehad |= 1L << i;
            data.gehad.put(sleutel, gehad);
            data.setDirty();
            if (uitkomst) {
                gemaakt++;
            }
        }
        if (gemaakt > 0) {
            LOGGER.info("Guhs: made {} step(s) out of the sauce at the tree city at {}", gemaakt, kopie.anker().toShortString());
        }
        return gemaakt;
    }

    /**
     * One step of one copy. True: made now. False: nothing to do (it is there already) or it can't be made (a cell is no
     * plain bank any more: logged). Null: not all of it is loaded.
     */
    private static Boolean maak(ServerLevel level, Boomstad.Kopie kopie, List<Cel> trap) {
        List<BlockPos> waar = new ArrayList<>();
        for (Cel c : trap) {
            BlockPos pos = wereld(kopie, c.lokaal());
            if (!level.isLoaded(pos)) {
                return null;
            }
            waar.add(pos);
        }
        boolean anders = false;
        for (int i = 0; i < trap.size(); i++) {
            BlockState nu = level.getBlockState(waar.get(i)), wordt = trap.get(i).wordt();
            if (nu == wordt) {
                continue;
            }
            anders = true;
            // (a plant: anything you walk through that is no fluid: the little saté sprout on the bank does not give way by itself)
            boolean grond = nu.is(Bezetting.NATUURLIJK) && nu.getFluidState().isEmpty();
            boolean mag = level.getBlockEntity(waar.get(i)) == null && (wordt.isAir()
                    ? grond || Bezetting.leeg(nu) || (nu.getFluidState().isEmpty() && nu.getCollisionShape(level, waar.get(i)).isEmpty()) : grond);
            if (!mag) {
                LOGGER.info("Guhs: no step out of the sauce at {} (tree city at {}): {} is in the way", waar.get(i).toShortString(),
                        kopie.anker().toShortString(), BuiltInRegistries.BLOCK.getKey(nu.getBlock()));
                return false;
            }
        }
        if (!anders) {
            return false;
        }
        for (int i = 0; i < trap.size(); i++) {
            if (level.getBlockState(waar.get(i)) != trap.get(i).wordt()) {
                level.setBlock(waar.get(i), trap.get(i).wordt(), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    /** Was this step of this copy handled already (made, there already, or left out)? */
    static boolean gehad(ServerLevel level, Boomstad.Kopie kopie, int nr) {
        return (Gehad.get(level).gehad.getOrDefault(kopie.anker().asLong(), 0L) & (1L << nr)) != 0;
    }

    /** How many steps of this copy were handled already. */
    static int gehad(ServerLevel level, Boomstad.Kopie kopie) {
        return Long.bitCount(Gehad.get(level).gehad.getOrDefault(kopie.anker().asLong(), 0L));
    }

    /** (tests, dev) as if this copy was never looked at. */
    static void vergeet(ServerLevel level, Boomstad.Kopie kopie) {
        Gehad data = Gehad.get(level);
        if (data.gehad.remove(kopie.anker().asLong()) != null) {
            data.setDirty();
        }
    }

    /** SavedData guhs:ringh4_uitstap (per dimension): per copy (its anchor) which steps were handled, one bit per step. */
    public static class Gehad extends SavedData {
        public static final SavedDataType<Gehad> TYPE = GuhSavedData.tagType("ringh4_uitstap", Gehad::new, Gehad::load, Gehad::save);
        private final Map<Long, Long> gehad = new HashMap<>();

        static Gehad get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            CompoundTag k = new CompoundTag();
            gehad.forEach((anker, bits) -> k.putLong(Long.toString(anker), bits));
            tag.put("Kopieen", k);
            return tag;
        }

        private static Gehad load(CompoundTag tag) {
            Gehad g = new Gehad();
            CompoundTag k = tag.getCompoundOrEmpty("Kopieen");
            for (String key : k.keySet()) {
                try {
                    g.gehad.put(Long.parseLong(key), k.getLongOr(key, 0L));
                } catch (NumberFormatException e) {
                    // (not ours: left out)
                }
            }
            return g;
        }
    }

    private Uitstap() {
    }
}
