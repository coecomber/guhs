package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.storage.Nbt;

/**
 * How zone-2 creatures are defeated (ronde 15, "knullig-gevaarlijk": no pink plof):
 * <ul>
 *   <li>{@link #scherven}: a crystal creature shatters: a burst of grey glass, then 5-7 little grey crystal shards (block
 *       displays) lie on the ground and melt away in ~3 s (they shrink while dripping);</li>
 *   <li>{@link #rookpluim}: a Mika flees in a puff of smoke, a little smoke trail heading for the Mika fort and a parting shout;</li>
 *   <li>{@link #stof}: a plain beast sinks into grey snow dust.</li>
 * </ul>
 * The shards are never saved for long: anything with the shard tag that joins a level without being tracked here (after a
 * restart) is removed again.
 */
public final class Verslagen {
    public static final String TAG = "guhs_vr_z2_scherf";
    private static final int SMELT = 60;

    private record Scherf(ServerLevel level, Display.BlockDisplay d, int[] t) {
    }

    private static final List<Scherf> SCHERVEN = new ArrayList<>();
    private static final List<Entity> BEKEND = new ArrayList<>();

    private Verslagen() {
    }

    public static void scherven(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        RandomSource r = sl.getRandom();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAY_STAINED_GLASS.defaultBlockState()), pos.x, pos.y + hoog * 0.5, pos.z, 50,
                breed * 0.5, hoog * 0.4, breed * 0.5, 0.2);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), pos.x, pos.y + hoog * 0.5, pos.z, 20,
                breed * 0.4, hoog * 0.3, breed * 0.4, 0.15);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1f, 0.6f);
        int n = 5 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.3 + r.nextDouble() * (breed * 0.6 + 0.5);
            Vec3 p = pos.add(Math.cos(a) * d, 0.01, Math.sin(a) * d);
            float s = 0.18f + r.nextFloat() * 0.22f;
            Display.BlockDisplay b = blok(sl, p, i % 3 == 0 ? "minecraft:light_gray_stained_glass" : "minecraft:gray_stained_glass", s, r);
            if (b != null) {
                b.addTag(TAG);
                BEKEND.add(b);
                sl.addFreshEntity(b);
                SCHERVEN.add(new Scherf(sl, b, new int[] {0}));
            }
        }
    }

    public static void rookpluim(ServerLevel sl, Vec3 pos, float hoog, BlockPos fort, Component roep) {
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + hoog * 0.5, pos.z, 30, 0.5, hoog * 0.4, 0.5, 0.03);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + hoog * 0.3, pos.z, 10, 0.4, 0.3, 0.4, 0.02);
        sl.sendParticles(ParticleTypes.POOF, pos.x, pos.y + hoog * 0.5, pos.z, 20, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1f, 1.3f);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 0.7f, 1.5f);
        Vec3 naar = Vec3.atCenterOf(fort).subtract(pos).multiply(1, 0, 1);
        if (naar.lengthSqr() > 1) {
            naar = naar.normalize();
            for (int i = 1; i <= 10; i++) {   // the smoke trail: off it goes, back to the fort
                Vec3 p = pos.add(naar.scale(i * 0.9)).add(0, hoog * 0.5 + i * 0.25, 0);
                sl.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 4, 0.12, 0.12, 0.12, 0.01);
            }
        }
        if (roep != null) {
            Getallen.tekst(sl, pos.x, pos.y + hoog + 0.6, pos.z, roep, 0.9f);
        }
    }

    public static void stof(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        sl.sendParticles(new DustParticleOptions(0x9A98A0, 1.8f), pos.x, pos.y + hoog * 0.4, pos.z, 30, breed * 0.5, hoog * 0.4, breed * 0.5, 0);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + hoog * 0.4, pos.z, 16, breed * 0.5, hoog * 0.3, breed * 0.5, 0.03);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), pos.x, pos.y + 0.2, pos.z, 20, breed * 0.5, 0.1,
                breed * 0.5, 0.1);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
    }

    private static Display.BlockDisplay blok(ServerLevel sl, Vec3 p, String blok, float s, RandomSource r) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        // a random tilt, a flat-ish shard: x and z the size, y a bit thinner
        float ax = (r.nextFloat() - 0.5f) * 1.2f, az = (r.nextFloat() - 0.5f) * 1.2f, ay = r.nextFloat() * 6.28f;
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotateY(ay).rotateX(ax).rotateZ(az);
        CompoundTag tag = transformatie(s, s * 0.7f, q, 0);
        CompoundTag state = new CompoundTag();
        state.putString("Name", blok);
        tag.put("block_state", state);
        tag.putFloat("view_range", 0.8f);
        Nbt.load(d, tag);
        d.snapTo(p.x, p.y, p.z, 0f, 0f);
        return d;
    }

    private static CompoundTag transformatie(float s, float sy, org.joml.Quaternionf q, int duur) {
        CompoundTag tag = new CompoundTag();
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(-s / 2, 0, -s / 2));
        t.put("left_rotation", floats(q.x, q.y, q.z, q.w));
        t.put("scale", floats(s, sy, s));
        t.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", t);
        if (duur > 0) {
            tag.putInt("interpolation_duration", duur);
            tag.putInt("start_interpolation", 0);
        }
        return tag;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    /** Every server tick: the shards lie still, then shrink (melt) while dripping, then go. */
    public static void tick() {
        Iterator<Scherf> it = SCHERVEN.iterator();
        while (it.hasNext()) {
            Scherf s = it.next();
            int t = ++s.t()[0];
            if (s.d().isRemoved()) {
                it.remove();
                continue;
            }
            if (t == 20) {
                // melt: interpolate to nothing over SMELT ticks (keeps the rotation it had)
                CompoundTag huidig = Nbt.saveWithoutId(s.d());
                CompoundTag tr = huidig.getCompound("transformation").orElse(new CompoundTag());
                CompoundTag nieuw = new CompoundTag();
                CompoundTag t2 = tr.copy();
                t2.put("scale", floats(0.01f, 0.01f, 0.01f));
                t2.put("translation", floats(0, 0, 0));
                nieuw.put("transformation", t2);
                nieuw.putInt("interpolation_duration", SMELT);
                nieuw.putInt("start_interpolation", 0);
                Nbt.load(s.d(), nieuw);
            }
            if (t > 20 && t % 8 == 0) {
                s.level().sendParticles(ParticleTypes.DRIPPING_WATER, s.d().getX(), s.d().getY() + 0.15, s.d().getZ(), 1, 0.08, 0.02, 0.08, 0);
            }
            if (t > 20 + SMELT) {
                s.d().discard();
                BEKEND.remove(s.d());
                it.remove();
            }
        }
    }

    /** Shards left over from before a restart: gone. */
    public static void binnen(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && e.getEntity().entityTags().contains(TAG) && !BEKEND.contains(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    public static int aantalScherven() {
        return SCHERVEN.size();
    }
}
