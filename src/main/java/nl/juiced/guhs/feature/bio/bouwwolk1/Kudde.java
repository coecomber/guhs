package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.storage.Nbt;

/**
 * The herd in the fold beside the wolkenhoeder's hut: {@value #AANTAL} wolkenschaapjes (slice dieren's entity, by id)
 * that belong to the place. The template puts them there kept and persistent (so dieren never tidies them up) with the
 * entity tag {@value #TAG}. The rules that keep it a fixed small herd:
 * <ul>
 *   <li><b>never more</b>: a schaapje of the herd gets no lammetje (BouwWolk1Events cancels it), so feeding them does
 *       not fill the fold;</li>
 *   <li><b>never away</b>: a lead does not go on a herd schaapje (the wolkenhoeder gives you your own); every
 *       {@value #ELKE} ticks the wolkenhoeder looks around once ({@link #hoed}): a herd schaapje that strayed more than
 *       {@value #LOS} blocks from the fold (pushed out of the gate, off the island: it floats down, it never falls) is
 *       put back in it with a puff;</li>
 *   <li><b>never fewer for long</b>: when one is really gone (not just out of sight: none within {@value #ZOEK} blocks)
 *       he calls a new one, up to {@value #AANTAL}; never more than that at once, so nothing piles up.</li>
 * </ul>
 * One entity query per wolkenhoeder every ten seconds; nothing per tick.
 */
public final class Kudde {
    public static final String TAG = "guhs_bio_bouw_wolk1_kudde";
    public static final int AANTAL = 4;
    public static final int ELKE = 200;
    /** Further than this from the middle of the fold (sideways), or this far above or below it: back into the fold. */
    public static final double LOS = 4.8, LOS_HOOG = 3.0;
    /** A herd schaapje within this many blocks still counts (it is fetched, not replaced). */
    public static final int ZOEK = 64;
    /** roleData of the wolkenhoeder: the middle of the fold (found once from where the herd stands). */
    private static final String WEI = "KuddeWei", MIS = "KuddeMis";

    /** The wolkenschaapje's entity type (slice dieren), when it is there. */
    public static Optional<EntityType<?>> soort() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("wolkenschaapje"));
    }

    public static boolean isKudde(Entity e) {
        return e.entityTags().contains(TAG);
    }

    /** The herd schaapjes around this wolkenhoeder. */
    public static List<Mob> kudde(ServerLevel level, GuhNpcEntity npc) {
        return level.getEntitiesOfClass(Mob.class, new AABB(npc.blockPosition()).inflate(ZOEK, 96, ZOEK), m -> m.isAlive() && isKudde(m));
    }

    /** The middle of the fold: where the herd stood when the wolkenhoeder first looked (remembered in his roleData). */
    static Vec3 wei(GuhNpcEntity npc, List<Mob> kudde) {
        CompoundTag d = npc.roleData;
        if (d.contains(WEI)) {
            BlockPos p = BlockPos.of(d.getLongOr(WEI, 0L));
            return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        }
        Vec3 som = Vec3.ZERO;
        int n = 0;
        for (Mob m : kudde) {
            if (m.distanceToSqr(npc) < 16 * 16) {
                som = som.add(m.position());
                n++;
            }
        }
        if (n == 0) {
            return npc.position();           // (no herd to learn the fold from yet: nothing is remembered)
        }
        Vec3 midden = som.scale(1.0 / n);
        d.putLong(WEI, BlockPos.containing(midden.x, npc.getY(), midden.z).asLong());
        return new Vec3(midden.x, npc.getY(), midden.z);
    }

    /** The wolkenhoeder looks after his herd once. Returns {fetched back, called anew}. */
    public static int[] hoed(ServerLevel level, GuhNpcEntity npc) {
        List<Mob> kudde = kudde(level, npc);
        boolean bekend = npc.roleData.contains(WEI);
        Vec3 wei = wei(npc, kudde);
        if (!bekend && !npc.roleData.contains(WEI)) {
            return new int[]{0, 0};
        }
        int terug = 0, nieuw = 0;
        List<Mob> inWei = new ArrayList<>();
        for (Mob m : kudde) {
            if (m.isLeashed()) {
                m.dropLeash();
            }
            double dx = m.getX() - wei.x, dz = m.getZ() - wei.z;
            if (dx * dx + dz * dz > LOS * LOS || Math.abs(m.getY() - wei.y) > LOS_HOOG) {
                zetTerug(level, m, wei, kudde.indexOf(m));
                terug++;
            }
            inWei.add(m);
        }
        if (inWei.size() < AANTAL) {
            // (really gone, not a chunk whose entities are still coming in: missed twice in a row, with the fold loaded)
            int mis = npc.roleData.getIntOr(MIS, 0);
            if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(BlockPos.containing(wei))) || mis < 1) {
                npc.roleData.putInt(MIS, mis + 1);
                return new int[]{terug, 0};
            }
        }
        npc.roleData.remove(MIS);
        for (int i = inWei.size(); i < AANTAL; i++) {
            if (roep(level, wei, i) != null) {
                nieuw++;
            }
        }
        return new int[]{terug, nieuw};
    }

    private static Vec3 plek(Vec3 wei, int i) {
        return wei.add((i % 2 == 0 ? -1.0 : 1.0), 0.1, (i / 2 % 2 == 0 ? -1.0 : 1.0));
    }

    private static void zetTerug(ServerLevel level, Mob m, Vec3 wei, int i) {
        level.sendParticles(ParticleTypes.CLOUD, m.getX(), m.getY() + 0.6, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        Vec3 p = plek(wei, i);
        m.getNavigation().stop();
        m.teleportTo(p.x, p.y, p.z);
        m.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.6, p.z, 8, 0.3, 0.3, 0.3, 0.02);
    }

    /** A new herd schaapje in the fold: kept (dieren's own flag, through its saved form), persistent, tagged. */
    static Mob roep(ServerLevel level, Vec3 wei, int i) {
        Entity e = soort().map(t -> t.create(level, EntitySpawnReason.TRIGGERED)).orElse(null);
        if (!(e instanceof Mob m)) {
            return null;
        }
        CompoundTag tag = Nbt.saveWithoutId(m);
        tag.putBoolean("Gehouden", true);
        Nbt.load(m, tag);
        Vec3 p = plek(wei, i);
        m.snapTo(p.x, p.y, p.z, level.getRandom().nextFloat() * 360, 0);
        m.setPersistenceRequired();
        m.addTag(TAG);
        level.addFreshEntity(m);
        level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.6, p.z, 10, 0.3, 0.3, 0.3, 0.02);
        return m;
    }

    private Kudde() {
    }
}
