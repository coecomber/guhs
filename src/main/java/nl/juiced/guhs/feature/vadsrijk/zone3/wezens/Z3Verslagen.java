package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Verslagen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;

/**
 * How zone-3 creatures are defeated (ZONE3_PLAN §1 "knullig-gevaarlijk", nothing gory): rot creatures fall apart into grey fluff
 * and dead leaves that the wind blows away, Mika's flee in a smoke puff to their zeppelin, beasts sink into dust and leaves, ghosts
 * fade into dark wisps, resin golems break into amber shards. Particles and sounds only (nothing is left lying around).
 */
public final class Z3Verslagen {
    private Z3Verslagen() {
    }

    /** The wind: a fixed east-ish drift so a whole fight's fluff blows the same way. */
    private static final Vec3 WIND = new Vec3(0.9, 0.25, 0.35).normalize();

    public static void pluis(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        double y = pos.y + hoog * 0.5;
        sl.sendParticles(new DustParticleOptions(0xA9A7B2, 2.0f), pos.x, y, pos.z, 36, breed * 0.5, hoog * 0.4, breed * 0.5, 0);
        sl.sendParticles(ParticleTypes.WHITE_ASH, pos.x, y, pos.z, 50, breed * 0.6, hoog * 0.5, breed * 0.6, 0.02);
        for (int i = 0; i < 8; i++) {   // the fluff and leaves drift off with the wind
            Vec3 p = pos.add(WIND.scale(i * 0.7)).add(0, hoog * 0.4 + i * 0.12, 0);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEAD_BUSH.defaultBlockState()), p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.01);
        }
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.1f, 0.7f);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 0.9f, 0.6f);
    }

    public static void blaadjes(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        sl.sendParticles(new DustParticleOptions(0x8C7A62, 1.8f), pos.x, pos.y + hoog * 0.4, pos.z, 30, breed * 0.5, hoog * 0.4, breed * 0.5, 0);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), pos.x, pos.y + hoog * 0.5, pos.z, 24,
                breed * 0.5, hoog * 0.4, breed * 0.5, 0.1);
        sl.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.3, pos.z, 10, breed * 0.4, 0.1, breed * 0.4, 0.03);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
    }

    public static void rook(ServerLevel sl, Vec3 pos, float hoog, @Nullable Component roep) {
        Verslagen.rookpluim(sl, pos, hoog, Z3WezensPlekken.zeppelin(pos.x, pos.y, pos.z), roep);
    }

    public static void wisp(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + hoog * 0.5, pos.z, 20, breed * 0.4, hoog * 0.4, breed * 0.4, 0.02);
        sl.sendParticles(new DustParticleOptions(0x3A3550, 1.6f), pos.x, pos.y + hoog * 0.5, pos.z, 30, breed * 0.5, hoog * 0.5, breed * 0.5, 0);
        sl.sendParticles(ParticleTypes.SOUL, pos.x, pos.y + hoog * 0.6, pos.z, 6, breed * 0.3, hoog * 0.3, breed * 0.3, 0.03);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
    }

    public static void hars(ServerLevel sl, Vec3 pos, float breed, float hoog) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ORANGE_STAINED_GLASS.defaultBlockState()), pos.x, pos.y + hoog * 0.5, pos.z, 40,
                breed * 0.5, hoog * 0.4, breed * 0.5, 0.2);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.defaultBlockState()), pos.x, pos.y + hoog * 0.5, pos.z, 24,
                breed * 0.5, hoog * 0.4, breed * 0.5, 0.15);
        sl.sendParticles(new DustParticleOptions(0xFF8AC8, 1.4f), pos.x, pos.y + hoog * 0.6, pos.z, 16, breed * 0.4, hoog * 0.3, breed * 0.4, 0);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.1f, 0.6f);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.HOSTILE, 1f, 0.7f);
    }

    /** A contract elite that leaves (its players went away): a puff, no loot. */
    public static void weg(ServerLevel sl, Z3Wezen w) {
        sl.sendParticles(ParticleTypes.POOF, w.getX(), w.getY() + w.getBbHeight() * 0.5, w.getZ(), 30, w.getBbWidth() * 0.5, w.getBbHeight() * 0.4,
                w.getBbWidth() * 0.5, 0.05);
        sl.playSound(null, w.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.HOSTILE, 1f, 0.6f);
    }
}
