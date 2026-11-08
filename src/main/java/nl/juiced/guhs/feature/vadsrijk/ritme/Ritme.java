package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.GuhTime;

/**
 * U-LEVEN-RITME: shared constants and small helpers of the slice (code-spawned display/interaction entities that are never kept:
 * they carry {@link #TAG}, and {@code RitmeRegistry} cancels any that come back from disk).
 */
public final class Ritme {
    /** Lang prefix. */
    public static final String P = "vadsrijk.uritme.";
    /** Every entity this slice spawns carries this tag (never kept: a reload drops them, the owner makes them again). */
    public static final String TAG = "guhs_vr_uritme";

    private Ritme() {
    }

    /** Time of day 0..23999 (0 sunrise, 6000 noon, 12000 sunset, 18000 midnight). */
    public static long tijd(net.minecraft.world.level.Level l) {
        return GuhTime.timeOfDay(l);
    }

    public static long dag(net.minecraft.world.level.Level l) {
        return GuhTime.day(l);
    }

    /** Somebody (not a spectator) within r blocks (flat distance, |dy| < 48). */
    public static boolean spelerBij(ServerLevel sl, BlockPos pos, double r) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && Math.abs(p.getY() - pos.getY()) < 48 && p.distanceToSqr(pos.getX() + 0.5, p.getY(), pos.getZ() + 0.5) < r * r) {
                return true;
            }
        }
        return false;
    }

    /** A cell a guh/player can stand in: solid floor, two free blocks. */
    public static boolean vrij(ServerLevel sl, BlockPos b) {
        BlockState onder = sl.getBlockState(b.below());
        return !onder.getCollisionShape(sl, b.below()).isEmpty() && sl.getBlockState(b).getCollisionShape(sl, b).isEmpty()
                && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty() && sl.getFluidState(b).isEmpty();
    }

    /** The free standing cell nearest to {@code rond} at distance min..max (same y +-2), or null. */
    @Nullable
    public static BlockPos vrijeplek(ServerLevel sl, BlockPos rond, int min, int max) {
        BlockPos beste = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -max; dx <= max; dx++) {
            for (int dz = -max; dz <= max; dz++) {
                int d = Math.max(Math.abs(dx), Math.abs(dz));
                if (d < min || d > max) {
                    continue;
                }
                for (int dy : new int[] {0, -1, 1, -2, 2}) {
                    BlockPos b = rond.offset(dx, dy, dz);
                    if (vrij(sl, b)) {
                        double dd = dx * dx + dz * dz + dy * dy * 4;
                        if (dd < bd) {
                            bd = dd;
                            beste = b;
                        }
                        break;
                    }
                }
            }
        }
        return beste;
    }

    // ---- display entities ------------------------------------------------------------------------------------------------------

    static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    static CompoundTag transformatie(float schaal, float ty) {
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(-schaal / 2f, ty, -schaal / 2f));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        return tr;
    }

    /** A floating text (billboard, soft background), tagged. */
    @Nullable
    static Display.TextDisplay tekst(ServerLevel sl, double x, double y, double z, Component t, float schaal, int achtergrond) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), t).getOrThrow());
        tag.putString("billboard", "center");
        tag.putInt("background", achtergrond);
        tag.putBoolean("shadow", true);
        tag.putFloat("view_range", 0.6f);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        Nbt.load(d, tag);
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
        return d;
    }

    /** A block shown as a prop (scaled around its own centre-bottom), optionally full bright (a glowing lampion). */
    @Nullable
    static Display.BlockDisplay blok(ServerLevel sl, double x, double y, double z, BlockState s, float schaal, float yaw, boolean helder) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(s));
        tag.putFloat("view_range", 0.8f);
        tag.put("transformation", transformatie(schaal, 0));
        if (helder) {
            CompoundTag br = new CompoundTag();
            br.putInt("block", 15);
            br.putInt("sky", 15);
            tag.put("brightness", br);
        }
        Nbt.load(d, tag);
        d.snapTo(x, y, z, yaw, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
        return d;
    }

    /** An invisible click box (width x height) standing on x/y/z. */
    @Nullable
    static Interaction klikvak(ServerLevel sl, double x, double y, double z, float breed, float hoog, String soort) {
        Interaction i = EntityType.INTERACTION.create(sl, EntitySpawnReason.TRIGGERED);
        if (i == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.putFloat("width", breed);
        tag.putFloat("height", hoog);
        tag.putBoolean("response", true);
        Nbt.load(i, tag);
        i.snapTo(x, y, z, 0, 0);
        i.addTag(TAG);
        i.addTag(TAG + "_" + soort);
        sl.addFreshEntity(i);
        return i;
    }

    /** Removes the entities with these ids (those still loaded). */
    static void weg(ServerLevel sl, List<UUID> ids) {
        for (UUID u : ids) {
            Entity e = sl.getEntity(u);
            if (e != null) {
                e.discard();
            }
        }
        ids.clear();
    }

    /** True when every id is a loaded, living entity. */
    static boolean leeft(ServerLevel sl, List<UUID> ids) {
        if (ids.isEmpty()) {
            return false;
        }
        for (UUID u : ids) {
            Entity e = sl.getEntity(u);
            if (e == null || e.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    static List<UUID> lijst() {
        return new ArrayList<>();
    }
}
