package nl.juiced.guhs.feature.vadsrijk.zone3.systemen.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.world.GuhTime;

/**
 * (P-VERHUIS, ronde 20) The evening light + life layer of the revamped zone 3, client only, particles only (no shaders):
 * <ul>
 *   <li><b>lantern glow:</b> at dusk/night (and always inside the trunk and the roots) random block samples around you; a lantern,
 *   lampion, candle, shroomlight or glow block gets a soft warm halo of drifting motes, so the lampion strings and windows read as
 *   points of light from afar;</li>
 *   <li><b>chimney smoke:</b> above every zone-3 house roof (the {@code gebouw.*.doos_max} POIs) within 72 blocks, a lazy cosy plume;</li>
 *   <li><b>campfires:</b> a few sparks over every {@link Z3WereldPlekken#KAMPVUREN} within 48 blocks;</li>
 *   <li><b>bee swarms:</b> small yellow clouds circling the {@code bijenzwerm_*} POIs (or, without them, the beekeeper villages and
 *   Honingtak) by day;</li>
 *   <li><b>per layer:</b> fireflies + falling blossom in the crown and on the branches at dusk, amber motes in the hollow trunk,
 *   violet sap motes in the roots and the pink pulse of the egg near {@code ei}.</li>
 * </ul>
 * Layers by height (boom2.py): roots 4-78, trunk 80-160, branches 160-216, crown 216-312; everything by POI id, so it follows the map.
 */
public final class Z3Avondlicht {
    private static final RandomSource R = RandomSource.create();
    /** Layer bounds (y), the trunk radius (inside the hollow trunk = r < STAM_R). */
    static final int WORTEL_TOT = 79, STAM_TOT = 160, TAK_TOT = 216, STAM_R = 36;
    private static final DustParticleOptions WARM = new DustParticleOptions(0xFFC46A, 0.9f), ROZE = new DustParticleOptions(0xFF8AD0, 1.1f),
            AMBER = new DustParticleOptions(0xE8A040, 0.6f), PAARS = new DustParticleOptions(0xA070FF, 0.8f), BIJ = new DustParticleOptions(0xFFD21E, 0.45f);
    private static List<BlockPos> schoorstenen, zwermen;
    private static int teller;

    private Z3Avondlicht() {
    }

    /** Is it evening or night (the lights are on)? */
    static boolean avond(ClientLevel l) {
        long t = GuhTime.timeOfDay(l);
        return t > 11800 && t < 23400;
    }

    private static void plekken() {
        if (schoorstenen != null) {
            return;
        }
        List<BlockPos> s = new ArrayList<>(), z = new ArrayList<>();
        for (Map.Entry<String, BlockPos> e : Z3WereldPlekken.ALLE.entrySet()) {
            String id = e.getKey();
            if (id.startsWith("gebouw.") && id.endsWith(".doos_max")) {
                BlockPos min = Z3WereldPlekken.ALLE.get(id.replace(".doos_max", ".doos_min"));
                BlockPos max = e.getValue();
                s.add(min == null ? max.above() : new BlockPos((min.getX() + max.getX()) / 2 + 1, max.getY() + 1, (min.getZ() + max.getZ()) / 2 - 1));
            } else if (id.startsWith("bijenzwerm_")) {
                z.add(e.getValue());
            }
        }
        if (z.isEmpty()) {
            for (Map.Entry<String, BlockPos> e : Z3WereldPlekken.ALLE.entrySet()) {
                if (e.getKey().startsWith("imkerdorp_") && !e.getKey().contains(".") || e.getKey().equals("plein")) {
                    z.add(e.getValue().above(4));
                }
            }
        }
        schoorstenen = List.copyOf(s);
        zwermen = List.copyOf(z);
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null || mc.isPaused() || !Vadsrijk.in(l) || !Z3WereldPlekken.inZone3(p.getX(), p.getZ())) {
            return;
        }
        plekken();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double rStam = Math.hypot(x - Z3WereldPlekken.STAM_X, z - Z3WereldPlekken.STAM_Z);
        boolean binnen = y <= WORTEL_TOT || (y <= STAM_TOT && rStam < STAM_R);
        boolean avond = avond(l);
        teller++;
        // lantern glow: sample blocks around the player
        if (avond || binnen) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 28; i++) {
                m.set(x + R.nextInt(41) - 20, y + R.nextInt(19) - 6, z + R.nextInt(41) - 20);
                BlockState s = l.getBlockState(m);
                if (s.isAir()) {
                    continue;
                }
                String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                if (id.contains("lantern") || id.contains("lampion") || id.contains("candle") || id.equals("shroomlight") || id.equals("glowstone")
                        || id.equals("ochre_froglight") || id.equals("pearlescent_froglight")) {
                    l.addParticle(id.contains("roze") || id.contains("pearlescent") ? ROZE : WARM, m.getX() + 0.5 + R.nextGaussian() * 0.45,
                            m.getY() + 0.4 + R.nextDouble() * 0.6, m.getZ() + 0.5 + R.nextGaussian() * 0.45, 0, 0.004, 0);
                }
            }
        }
        // chimney smoke (every 6 ticks per house in range)
        if (teller % 6 == 0) {
            for (BlockPos b : schoorstenen) {
                if (b.distToCenterSqr(x, y, z) < 72 * 72 && R.nextInt(2) == 0) {
                    l.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, b.getX() + 0.5, b.getY() + 0.2, b.getZ() + 0.5, 0, 0.035, 0);
                }
            }
        }
        // campfire sparks
        if (teller % 4 == 0) {
            for (BlockPos k : Z3WereldPlekken.KAMPVUREN) {
                if (k.distToCenterSqr(x, y, z) < 48 * 48) {
                    l.addParticle(ParticleTypes.LAVA, k.getX() + 0.5, k.getY() + 0.6, k.getZ() + 0.5, 0, 0, 0);
                    if (avond) {
                        l.addParticle(WARM, k.getX() + 0.5 + R.nextGaussian() * 0.6, k.getY() + 1.2, k.getZ() + 0.5 + R.nextGaussian() * 0.6, 0, 0.02, 0);
                    }
                }
            }
        }
        // bee swarms by day: a little cloud circling its spot
        if (!avond && teller % 2 == 0) {
            double a0 = teller * 0.09;
            for (BlockPos b : zwermen) {
                if (b.distToCenterSqr(x, y, z) < 56 * 56) {
                    for (int i = 0; i < 4; i++) {
                        double a = a0 + i * 1.57 + R.nextGaussian() * 0.3, rr = 1.5 + R.nextDouble() * 1.8;
                        l.addParticle(BIJ, b.getX() + 0.5 + Math.cos(a) * rr, b.getY() + 1.5 + Math.sin(a * 2.3) * 0.8, b.getZ() + 0.5 + Math.sin(a) * rr, 0, 0, 0);
                    }
                }
            }
        }
        // per layer
        if (y <= WORTEL_TOT) {
            if (R.nextInt(2) == 0) {
                l.addParticle(PAARS, x + R.nextGaussian() * 9, y + R.nextDouble() * 5, z + R.nextGaussian() * 9, 0, 0.01, 0);
            }
            BlockPos ei = Z3WereldPlekken.plek("ei");
            if (ei != null && ei.distToCenterSqr(x, y, z) < 40 * 40 && teller % 3 == 0) {
                double puls = 0.5 + 0.5 * Math.sin(teller * 0.08);
                for (int i = 0; i < 1 + (int) (puls * 3); i++) {
                    l.addParticle(ROZE, ei.getX() + 0.5 + R.nextGaussian() * 1.3, ei.getY() + 0.5 + R.nextDouble() * 2, ei.getZ() + 0.5 + R.nextGaussian() * 1.3, 0, 0.01, 0);
                }
            }
        } else if (y <= STAM_TOT && rStam < STAM_R) {
            if (R.nextInt(2) == 0) {
                l.addParticle(AMBER, x + R.nextGaussian() * 7, y + R.nextDouble() * 8, z + R.nextGaussian() * 7, 0, -0.003, 0);
            }
        } else if (avond && R.nextInt(2) == 0) {
            l.addParticle(ParticleTypes.FIREFLY, x + R.nextGaussian() * 14, y + 1 + R.nextDouble() * 6, z + R.nextGaussian() * 14, 0, 0, 0);
            if (y > TAK_TOT && R.nextInt(3) == 0) {
                l.addParticle(ParticleTypes.CHERRY_LEAVES, x + R.nextGaussian() * 10, y + 8, z + R.nextGaussian() * 10, 0, 0, 0);
            }
        }
    }
}
