package nl.juiced.guhs.feature.vadsrijk.udeco;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Critter;
import nl.juiced.guhs.registry.ModEntities;

/**
 * U-DECO (feedback round 4: "more bees in Honingtak: vanilla bees + guhbijtjes"): by day, around every player on the Honingtak
 * ring deck, up to {@link #BIJEN} bees (half vanilla bees, half Guhs guhbijtjes = {@code guhs:guh_bee}) plus {@link #GUH} zone-3
 * honey-bee critters buzz about, spawned 6-20 blocks away, preferably next to flowers.
 * <ul>
 * <li>The bees (both kinds; the guhbijtje is a Bee too) are tame decoration: invulnerable (so never angry), they never sting (their damage is cancelled), they
 * never go into a hive (stay-out countdown), and they vanish at night, in the rain or with no player within 64 blocks.</li>
 * <li>They carry the kern's display tag ({@link Getallen#TAG}), so they are not saved: a restart starts clean, nothing piles up
 * in the hives or the world.</li>
 * </ul>
 */
public final class HoningtakBijen {
    public static final String TAG = "guhs_udeco_bij";
    public static final int BIJEN = 8, GUH = 4;
    /** The ring town round the trunk: deck stand y 167, radius 30..80 from the trunk axis. */
    static final int DEK_Y = 167, R_MIN = 30, R_MAX = 80;

    private HoningtakBijen() {
    }

    /** Is this spot on (or just above) the Honingtak deck? */
    public static boolean opHoningtak(double x, double y, double z) {
        double r = Math.hypot(x - Z3WereldPlekken.STAM_X, z - Z3WereldPlekken.STAM_Z);
        return r >= R_MIN && r <= R_MAX && y >= DEK_Y - 6 && y <= DEK_Y + 20;
    }

    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 60 != 17) {
            return;
        }
        boolean dag = !sl.isDarkOutside() && !sl.isRaining();
        AABB dek = new AABB(Z3WereldPlekken.STAM_X - R_MAX - 20, DEK_Y - 12, Z3WereldPlekken.STAM_Z - R_MAX - 20,
                Z3WereldPlekken.STAM_X + R_MAX + 20, DEK_Y + 40, Z3WereldPlekken.STAM_Z + R_MAX + 20);
        // our vanilla bees: calm, out of hives, gone at night / far from everyone
        for (Bee b : sl.getEntitiesOfClass(Bee.class, dek, b -> b.entityTags().contains(TAG))) {
            b.stopBeingAngry();
            b.setStayOutOfHiveCountdown(Integer.MAX_VALUE / 2);
            if (!dag || sl.getNearestPlayer(b, 64) == null) {
                b.discard();
            }
        }
        if (!dag) {
            return;
        }
        RandomSource r = sl.getRandom();
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || !opHoningtak(p.getX(), p.getY(), p.getZ())) {
                continue;
            }
            AABB om = new AABB(p.blockPosition()).inflate(32, 16, 32);
            List<Bee> bijen = sl.getEntitiesOfClass(Bee.class, om, b -> b.entityTags().contains(TAG));
            if (bijen.size() < BIJEN) {
                BlockPos plek = plek(sl, p, r);
                long guhbijtjes = bijen.stream().filter(b -> b.getType() == ModEntities.GUH_BEE.get()).count();
                EntityType<? extends Bee> soort = guhbijtjes * 2 < bijen.size() + 1 ? ModEntities.GUH_BEE.get() : EntityType.BEE;
                Bee b = plek == null ? null : soort.create(sl, EntitySpawnReason.NATURAL);
                if (b != null) {
                    b.snapTo(plek.getX() + 0.5, plek.getY() + 1.2, plek.getZ() + 0.5, r.nextFloat() * 360, 0);
                    b.addTag(TAG);
                    b.addTag(Getallen.TAG);
                    b.setInvulnerable(true);
                    b.setStayOutOfHiveCountdown(Integer.MAX_VALUE / 2);
                    sl.addFreshEntity(b);
                }
            }
            List<Z3Critter> guh = sl.getEntitiesOfClass(Z3Critter.class, om, c -> c.getType() == Z3WezensRegistry.HONINGBIJ.get());
            if (guh.size() < GUH) {
                BlockPos plek = plek(sl, p, r);
                Z3Critter c = plek == null ? null : Z3WezensRegistry.HONINGBIJ.get().create(sl, EntitySpawnReason.NATURAL);
                if (c != null) {
                    c.snapTo(plek.getX() + 0.5, plek.getY() + 1.5, plek.getZ() + 0.5, r.nextFloat() * 360, 0);
                    sl.addFreshEntity(c);
                }
            }
        }
    }

    /** A free standing spot on the deck 6-20 blocks from the player; one next to flowers wins (6 tries). */
    static BlockPos plek(ServerLevel sl, ServerPlayer p, RandomSource r) {
        BlockPos reserve = null;
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 6 + r.nextDouble() * 14;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, 0, z)) || !opHoningtak(x, p.getY(), z)) {
                continue;
            }
            int y = vloer(sl, x, z, p.getBlockY() + 6, p.getBlockY() - 8);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos b = new BlockPos(x, y, z);
            if (bijBloemen(sl, b)) {
                return b;
            }
            if (reserve == null) {
                reserve = b;
            }
        }
        return reserve;
    }

    static boolean bijBloemen(ServerLevel sl, BlockPos b) {
        for (BlockPos q : BlockPos.betweenClosed(b.offset(-4, -1, -4), b.offset(4, 2, 4))) {
            if (sl.getBlockState(q).is(BlockTags.FLOWERS)) {
                return true;
            }
        }
        return false;
    }

    /** The first standing spot searching down (solid floor, 2 free blocks, no fluid), or MIN_VALUE. */
    static int vloer(ServerLevel sl, int x, int z, int boven, int onder) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = boven; y >= onder; y--) {
            m.set(x, y - 1, z);
            if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                continue;
            }
            m.set(x, y, z);
            boolean vrij1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
            m.set(x, y + 1, z);
            if (vrij1 && sl.getBlockState(m).getCollisionShape(sl, m).isEmpty()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Our bees never sting (and a hit on them does nothing: they are invulnerable). */
    static void geenSteek(LivingIncomingDamageEvent e) {
        Entity bron = e.getSource().getEntity();
        if (bron instanceof Bee b && b.entityTags().contains(TAG)) {
            e.setCanceled(true);
            b.stopBeingAngry();
        }
    }
}
