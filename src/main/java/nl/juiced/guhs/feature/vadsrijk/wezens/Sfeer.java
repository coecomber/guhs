package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.onderwol.wezens.OnderWolWezens;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Fix round 1 ("leven"): harmless ambient critters around players in the zone, reusing existing Guhs critters (they never
 * fight, drop nothing and give no xp; the Vadsrijk has no natural spawning, so they come from here):
 * <ul>
 * <li>Klodderweide: knabbelvlindertjes (by day), kikkerguhs and guh-visjes at the ponds;</li>
 * <li>Sporenwoud: kaasmotten (the spore moths), glimguhtjes at night;</li>
 * <li>Druipgrotten caves: bats and grotglimwormen.</li>
 * </ul>
 * Every 2 s per player one kind is topped up (at most {@code max} of it within 28 blocks); every critter carries the tag
 * {@link #TAG} and is removed once no player is within 64 blocks. The client adds particles on top (WezensClient: fireflies,
 * spores, drips, splashes).
 */
public final class Sfeer {
    public static final String TAG = "guhs_vr_sfeer";
    private static final int BEREIK = 28, WEG = 64;

    private record Soort(Supplier<? extends EntityType<?>> type, int max, boolean water, boolean grot) {
    }

    private Sfeer() {
    }

    /** Every server tick (WezensRegistry). */
    public static void tick(ServerLevel sl) {
        long t = sl.getGameTime();
        if (t % 40 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (!p.isSpectator()) {
                    vulAan(sl, p);
                }
            }
        }
        if (t % 200 == 100) {
            for (Entity e : sl.getAllEntities()) {
                if (e.entityTags().contains(TAG) && e.isAlive()) {
                    var p = sl.getNearestPlayer(e, WEG);
                    if (p == null) {
                        e.discard();
                    }
                }
            }
        }
    }

    private static void vulAan(ServerLevel sl, ServerPlayer p) {
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (g == null) {
            return;
        }
        RandomSource r = sl.getRandom();
        boolean dag = sl.isBrightOutside();
        boolean inGrot = p.getY() < 62 && !sl.canSeeSky(p.blockPosition());
        Soort s = switch (g.id()) {
            case "klodderweide" -> switch (r.nextInt(3)) {
                case 0 -> dag ? new Soort(WaterdiertjesFeature.KNABBELVLINDERTJE, 5, false, false) : null;
                case 1 -> bijVijver(p) ? new Soort(KaasmoerasFeature.KIKKERGUH, 3, false, false) : null;
                default -> bijVijver(p) ? new Soort(ModEntities.GUH_VIS, 4, true, false) : null;
            };
            case "sporenwoud" -> r.nextBoolean() ? new Soort(KaasmoerasFeature.KAASMOT, 4, false, false)
                    : dag ? null : new Soort(WaterdiertjesFeature.GLIMGUHTJE, 5, false, false);
            case "druipgrotten" -> !inGrot ? null : r.nextBoolean() ? new Soort(() -> EntityType.BAT, 3, false, true)
                    : new Soort(OnderWolWezens.GROTGLIMWORM, 3, false, true);
            default -> null;
        };
        if (s == null) {
            return;
        }
        EntityType<?> type = s.type().get();
        int al = sl.getEntities(type, new AABB(p.blockPosition()).inflate(BEREIK, 16, BEREIK), e -> e.entityTags().contains(TAG)).size();
        if (al >= s.max()) {
            return;
        }
        for (int poging = 0; poging < 6; poging++) {
            double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 16;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            BlockPos plek = plek(sl, s, x, z, p.getBlockY());
            if (plek == null) {
                continue;
            }
            Entity e = type.create(sl, EntitySpawnReason.NATURAL);
            if (e == null) {
                return;
            }
            e.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, r.nextFloat() * 360, 0);
            if (e instanceof Mob m) {
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(plek), EntitySpawnReason.NATURAL, null);
            }
            e.addTag(TAG);
            sl.addFreshEntity(e);
            return;
        }
    }

    private static boolean bijVijver(ServerPlayer p) {
        for (int[] v : WezensPlekken.VIJVERS) {
            if (Math.hypot(p.getX() - v[0], p.getZ() - v[1]) < v[2] + 30) {
                return true;
            }
        }
        return false;
    }

    /** Where this kind may appear in column x/z: in water (fish), in cave air near the player's height, or above the ground. */
    @javax.annotation.Nullable
    private static BlockPos plek(ServerLevel sl, Soort s, int x, int z, int spelerY) {
        if (!sl.isLoaded(new BlockPos(x, spelerY, z))) {
            return null;
        }
        if (s.grot()) {
            for (int dy = 4; dy >= -4; dy--) {
                BlockPos b = new BlockPos(x, spelerY + dy, z);
                if (sl.getBlockState(b).isAir() && sl.getBlockState(b.above()).isAir() && !sl.canSeeSky(b)
                        && !sl.getBlockState(b.below()).isAir()) {
                    return dy > 0 ? b.above() : b;
                }
            }
            return null;
        }
        int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos b = new BlockPos(x, top, z);
        if (s.water()) {
            BlockPos in = b.below();
            return sl.getFluidState(in).is(net.minecraft.tags.FluidTags.WATER) && sl.getFluidState(in.below()).is(net.minecraft.tags.FluidTags.WATER) ? in.below() : null;
        }
        if (!sl.getFluidState(b.below()).isEmpty() || Math.abs(top - spelerY) > 20) {
            return null;
        }
        return b.above(sl.getRandom().nextInt(3));
    }

    public static boolean actief(ServerLevel sl) {
        return Vadsrijk.in(sl);
    }
}
