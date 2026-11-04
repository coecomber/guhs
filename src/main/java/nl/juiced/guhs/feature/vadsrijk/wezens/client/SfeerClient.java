package nl.juiced.guhs.feature.vadsrijk.wezens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensPlekken;

/**
 * Fix round 1 ("leven"): client-only ambient particles around the player in the zone (no server cost, no network):
 * fireflies and floating spores in the Sporenwoud, drips from the cave ceilings and glow dust in the Druipgrotten,
 * drifting petals over the Klodderweide and fish splashes on its ponds. The ambient entities come from the server ({@code Sfeer}).
 */
public final class SfeerClient {
    private SfeerClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        Player p = mc.player;
        if (l == null || p == null || mc.isPaused() || !l.dimension().equals(Vadsrijk.DIM) || l.getGameTime() % 2 != 0) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
        if (g == null) {
            return;
        }
        RandomSource r = l.getRandom();
        boolean nacht = !l.isBrightOutside();
        switch (g.id()) {
            case "sporenwoud" -> {
                for (int i = 0; i < (nacht ? 3 : 1); i++) {
                    double x = p.getX() + (r.nextDouble() - 0.5) * 32, z = p.getZ() + (r.nextDouble() - 0.5) * 32;
                    double y = grond(l, x, z, p.getY()) + 0.5 + r.nextDouble() * 4;
                    l.addParticle(ParticleTypes.FIREFLY, x, y, z, 0, 0, 0);
                }
                double x = p.getX() + (r.nextDouble() - 0.5) * 24, z = p.getZ() + (r.nextDouble() - 0.5) * 24;
                l.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, p.getY() + r.nextDouble() * 6, z, 0, 0, 0);
            }
            case "druipgrotten" -> {
                if (p.getY() < 62 && !l.canSeeSky(p.blockPosition())) {
                    for (int i = 0; i < 2; i++) {
                        int x = (int) Math.floor(p.getX() + (r.nextDouble() - 0.5) * 24), z = (int) Math.floor(p.getZ() + (r.nextDouble() - 0.5) * 24);
                        BlockPos plafond = plafond(l, x, (int) p.getY(), z);
                        if (plafond != null) {
                            l.addParticle(r.nextInt(3) == 0 ? ParticleTypes.DRIPPING_DRIPSTONE_WATER : ParticleTypes.DRIPPING_WATER, x + r.nextDouble(),
                                    plafond.getY() - 0.05, z + r.nextDouble(), 0, 0, 0);
                        }
                    }
                    l.addParticle(ParticleTypes.GLOW, p.getX() + (r.nextDouble() - 0.5) * 16, p.getY() + r.nextDouble() * 4,
                            p.getZ() + (r.nextDouble() - 0.5) * 16, 0, 0.01, 0);
                } else if (r.nextInt(4) == 0) {
                    l.addParticle(ParticleTypes.WHITE_ASH, p.getX() + (r.nextDouble() - 0.5) * 24, p.getY() + r.nextDouble() * 6,
                            p.getZ() + (r.nextDouble() - 0.5) * 24, 0, 0, 0);
                }
            }
            case "klodderweide" -> {
                if (r.nextInt(3) == 0) {
                    l.addParticle(ParticleTypes.CHERRY_LEAVES, p.getX() + (r.nextDouble() - 0.5) * 28, p.getY() + 3 + r.nextDouble() * 5,
                            p.getZ() + (r.nextDouble() - 0.5) * 28, 0, 0, 0);
                }
                if (nacht) {
                    double x = p.getX() + (r.nextDouble() - 0.5) * 32, z = p.getZ() + (r.nextDouble() - 0.5) * 32;
                    l.addParticle(ParticleTypes.FIREFLY, x, grond(l, x, z, p.getY()) + 0.5 + r.nextDouble() * 2, z, 0, 0, 0);
                }
                // jumping fish: now and then a splash ring on a pond near the player
                if (r.nextInt(30) == 0) {
                    for (int[] v : WezensPlekken.VIJVERS) {
                        if (Math.hypot(p.getX() - v[0], p.getZ() - v[1]) < v[2] + 40) {
                            double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * v[2] * 0.7;
                            int x = (int) Math.floor(v[0] + Math.cos(a) * d), z = (int) Math.floor(v[1] + Math.sin(a) * d);
                            int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                            if (l.getFluidState(new BlockPos(x, y - 1, z)).is(FluidTags.WATER)) {
                                for (int i = 0; i < 8; i++) {
                                    l.addParticle(ParticleTypes.SPLASH, x + 0.5, y + 0.1, z + 0.5, (r.nextDouble() - 0.5) * 0.3, 0.3, (r.nextDouble() - 0.5) * 0.3);
                                }
                                l.addParticle(ParticleTypes.BUBBLE_POP, x + 0.5, y + 0.1, z + 0.5, 0, 0, 0);
                            }
                            break;
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    /** The ground near the player's height in this column (looking down at most 12). */
    private static double grond(ClientLevel l, double x, double z, double vanY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos((int) Math.floor(x), (int) Math.floor(vanY) + 4, (int) Math.floor(z));
        for (int i = 0; i < 16; i++) {
            if (!l.getBlockState(m).isAir()) {
                return m.getY() + 1;
            }
            m.move(0, -1, 0);
        }
        return vanY;
    }

    /** The lowest solid block above (y .. y+12) in this column, or null. */
    private static BlockPos plafond(ClientLevel l, int x, int y, int z) {
        for (int dy = 2; dy <= 12; dy++) {
            BlockPos b = new BlockPos(x, y + dy, z);
            if (l.getBlockState(b).isSolidRender()) {
                return b;
            }
        }
        return null;
    }
}
