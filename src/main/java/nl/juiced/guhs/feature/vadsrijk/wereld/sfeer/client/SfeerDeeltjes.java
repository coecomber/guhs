package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerPayloads;

/**
 * Client particles and sounds of the atmosphere: the thick spores of the spore storm, the pink splashes of the shower, and the
 * dragon pass near the player (wind gusts and a whoosh, swaying lanterns, small flowers that turn grey for 10 s; client-side
 * block swap to a closed eyeblossom, put back afterwards).
 */
final class SfeerDeeltjes {
    private static final RandomSource R = RandomSource.create();
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF9ED2, 0.8f);
    private static final int GRIJS_TICKS = 200;

    private static final class Pas {
        final int draak;
        final long eind;
        boolean wind, grijs;
        final List<BlockPos> lampen = new ArrayList<>();

        Pas(int draak, long eind) {
            this.draak = draak;
            this.eind = eind;
        }
    }

    private static final List<Pas> PASSEN = new ArrayList<>();
    /** Flowers turned grey: position -> original state; put back at {@link #grijsTot}. */
    private static final Map<BlockPos, BlockState> GRIJS = new HashMap<>();
    private static long grijsTot;
    private static ClientLevel grijsLevel;

    private SfeerDeeltjes() {
    }

    static void pas(SfeerPayloads.DraakPas p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            PASSEN.add(new Pas(p.draak(), mc.level.getGameTime() + p.ticks() + 20));
        }
    }

    static void wis() {
        PASSEN.clear();
        herstel();
    }

    static void tick(Minecraft mc) {
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l == null || p == null) {
            return;
        }
        float k = SfeerConfig.sterkte();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        // the spore storm: thick spores and glowing motes
        float storm = Momenten.storm(l) * GebiedMist.wWoud;
        if (storm > 0.01f) {
            int n = (int) (10 * storm * k + R.nextFloat());
            for (int i = 0; i < n; i++) {
                double px = x + R.nextGaussian() * 12, py = y + R.nextDouble() * 9 - 1, pz = z + R.nextGaussian() * 12;
                switch (R.nextInt(4)) {
                    case 0 -> l.addParticle(ParticleTypes.CRIMSON_SPORE, px, py, pz, 0, 0, 0);
                    case 1 -> l.addParticle(ParticleTypes.WARPED_SPORE, px, py, pz, 0, 0, 0);
                    default -> l.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, px, py, pz, 0, 0, 0);
                }
            }
            if (R.nextFloat() < storm * k) {
                l.addParticle(ParticleTypes.GLOW, x + R.nextGaussian() * 8, y + 1 + R.nextDouble() * 4, z + R.nextGaussian() * 8, 0, 0.02, 0);
            }
            if (R.nextInt(160) == 0) {
                l.playLocalSound(x + R.nextGaussian() * 6, y + 3, z + R.nextGaussian() * 6, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.5f,
                        0.5f + R.nextFloat() * 0.3f, false);
            }
        }
        // the pink shower: splashes on the ground
        float bui = Momenten.bui(l) * GebiedMist.wWeide;
        if (bui > 0.01f) {
            int n = (int) (8 * bui * k + R.nextFloat());
            for (int i = 0; i < n; i++) {
                int px = (int) Math.floor(x + R.nextGaussian() * 9), pz = (int) Math.floor(z + R.nextGaussian() * 9);
                int gy = l.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
                if (Math.abs(gy - y) < 12) {
                    l.addParticle(R.nextInt(3) == 0 ? ParticleTypes.SPLASH : ROZE, px + R.nextDouble(), gy + 0.1, pz + R.nextDouble(), 0, 0.05, 0);
                }
            }
            if (R.nextInt(30) == 0) {
                l.playLocalSound(x, y + 4, z, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.12f * bui, 1.3f, false);
            }
        }
        drakenpas(mc, l, p, k);
        if (!GRIJS.isEmpty() && (l != grijsLevel || l.getGameTime() > grijsTot)) {
            herstel();
        }
    }

    private static void drakenpas(Minecraft mc, ClientLevel l, LocalPlayer p, float k) {
        long nu = l.getGameTime();
        Iterator<Pas> it = PASSEN.iterator();
        while (it.hasNext()) {
            Pas pas = it.next();
            if (nu > pas.eind) {
                it.remove();
                continue;
            }
            Entity d = l.getEntity(pas.draak);
            if (d == null) {
                continue;
            }
            double dx = d.getX() - p.getX(), dz = d.getZ() - p.getZ();
            double dh = Math.sqrt(dx * dx + dz * dz);
            double wx = d.getX() - d.xo, wz = d.getZ() - d.zo;     // the dragon's speed = the wind
            double wl = Math.sqrt(wx * wx + wz * wz);
            if (wl > 1e-3) {
                wx /= wl;
                wz /= wl;
            }
            if (dh < 100 && !pas.wind) {
                pas.wind = true;
                l.playLocalSound(p.getX(), p.getY() + 2, p.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.55f, 0.55f, false);
                // the lanterns round the player sway
                BlockPos.betweenClosedStream(p.blockPosition().offset(-20, -8, -20), p.blockPosition().offset(20, 14, 20)).forEach(b -> {
                    if (pas.lampen.size() < 48) {
                        BlockState s = l.getBlockState(b);
                        if (s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.SHROOMLIGHT) || s.is(Blocks.PEARLESCENT_FROGLIGHT)
                                || s.is(Blocks.OCHRE_FROGLIGHT)) {
                            pas.lampen.add(b.immutable());
                        }
                    }
                });
            }
            if (dh < 75) {
                int n = (int) (5 * k + R.nextFloat());
                for (int i = 0; i < n; i++) {
                    double px = p.getX() + R.nextGaussian() * 9, py = p.getY() + 0.5 + R.nextDouble() * 4, pz = p.getZ() + R.nextGaussian() * 9;
                    double v = 0.35 + R.nextDouble() * 0.4;
                    l.addParticle(R.nextInt(3) == 0 ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.CLOUD, px, py, pz, wx * v, 0.02, wz * v);
                }
                if (R.nextInt(25) == 0) {
                    l.addParticle(ParticleTypes.SMALL_GUST, p.getX() + R.nextGaussian() * 6, p.getY() + 1, p.getZ() + R.nextGaussian() * 6, 0, 0, 0);
                }
                for (BlockPos b : pas.lampen) {
                    if (R.nextInt(3) == 0) {
                        double v = 0.08 + R.nextDouble() * 0.1;
                        l.addParticle(R.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.END_ROD, b.getX() + 0.5, b.getY() + 0.3, b.getZ() + 0.5,
                                wx * v, -0.01, wz * v);
                    }
                }
            }
            if (dh < 60 && !pas.grijs) {
                pas.grijs = true;
                grijzeBloemen(l, p.blockPosition());
            }
        }
    }

    /** Small flowers round the player turn grey (a closed eyeblossom) for 10 s, client side only. */
    private static void grijzeBloemen(ClientLevel l, BlockPos m) {
        if (l != grijsLevel) {
            herstel();
        }
        grijsLevel = l;
        grijsTot = l.getGameTime() + GRIJS_TICKS;
        BlockState grijs = Blocks.CLOSED_EYEBLOSSOM.defaultBlockState();
        BlockPos.betweenClosedStream(m.offset(-22, -8, -22), m.offset(22, 8, 22)).forEach(b -> {
            if (GRIJS.size() >= 320) {
                return;
            }
            BlockState s = l.getBlockState(b);
            if (s.is(BlockTags.SMALL_FLOWERS) && !s.is(Blocks.CLOSED_EYEBLOSSOM) && !s.is(Blocks.WITHER_ROSE)) {
                BlockPos v = b.immutable();
                GRIJS.put(v, s);
                l.setBlock(v, grijs, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                if (R.nextInt(3) == 0) {
                    l.addParticle(ParticleTypes.WHITE_ASH, v.getX() + 0.5, v.getY() + 0.6, v.getZ() + 0.5, 0, 0.02, 0);
                }
            }
        });
    }

    private static void herstel() {
        ClientLevel l = Minecraft.getInstance().level;
        if (l != null && l == grijsLevel) {
            for (Map.Entry<BlockPos, BlockState> e : GRIJS.entrySet()) {
                if (l.getBlockState(e.getKey()).is(Blocks.CLOSED_EYEBLOSSOM)) {
                    l.setBlock(e.getKey(), e.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
        GRIJS.clear();
    }
}
