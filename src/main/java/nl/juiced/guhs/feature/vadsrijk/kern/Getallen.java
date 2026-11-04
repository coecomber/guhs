package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.storage.Nbt;

/**
 * Floating damage numbers and short world texts (server side: small text displays that rise and vanish after ~0.9 s, never
 * saved). Also {@link #hologram} for lasting texts (the boss respawn board).
 */
public final class Getallen {
    public static final String TAG = "guhs_vr_getal";
    private static final List<Display.TextDisplay> ACTIEF = new ArrayList<>();
    private static final int LEVEN = 18;

    private Getallen() {
    }

    public static void toon(ServerLevel sl, Entity doel, double n, boolean krit, Element el, double factor) {
        // BELEVING (fix round 1): size by damage, gold star crit, element colour, Zwak!/Taai, merged DoT ticks
        nl.juiced.guhs.feature.vadsrijk.kern.beleving.Getalstijl.toon(sl, doel, n, krit, el, factor);
    }

    /** A short rising text at this spot (BELEVING: returns the display, null if none). */
    @Nullable
    public static Display.TextDisplay tekst(ServerLevel sl, double x, double y, double z, Component t, float schaal) {
        Display.TextDisplay d = maak(sl, t, schaal, 0);
        if (d == null) {
            return null;
        }
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
        ACTIEF.add(d);
        return d;
    }

    /** A lasting text display (not saved: re-made by its owner after a restart). */
    @Nullable
    public static Display.TextDisplay hologram(ServerLevel sl, double x, double y, double z, Component t) {
        Display.TextDisplay d = maak(sl, t, 1.0f, 0x60000000);
        if (d == null) {
            return null;
        }
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
        return d;
    }

    public static void zetTekst(Display.TextDisplay d, Component t) {
        CompoundTag tag = Nbt.saveWithoutId(d);
        tag.put("text", ComponentSerialization.CODEC.encodeStart(d.registryAccess().createSerializationContext(NbtOps.INSTANCE), t).getOrThrow());
        Nbt.load(d, tag);
    }

    @Nullable
    private static Display.TextDisplay maak(ServerLevel sl, Component t, float schaal, int achtergrond) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), t).getOrThrow());
        tag.putString("billboard", "center");
        tag.putInt("background", achtergrond);
        tag.putBoolean("shadow", true);
        tag.putInt("teleport_duration", 2);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        Nbt.load(d, tag);
        return d;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    public static void tick() {
        Iterator<Display.TextDisplay> it = ACTIEF.iterator();
        while (it.hasNext()) {
            Display.TextDisplay d = it.next();
            if (d.isRemoved()) {
                it.remove();
                continue;
            }
            if (d.tickCount > LEVEN) {
                d.discard();
                it.remove();
                continue;
            }
            d.teleportTo(d.getX(), d.getY() + 0.06, d.getZ());
        }
    }
}
