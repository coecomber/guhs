package nl.juiced.guhs.feature.bio.blokkendal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The esdoorn (maple) of the Klaterdal: leaves in red and in orange that let a leaf of their own colour fall, and the sapling. */
public final class Esdoorn {
    /** The colours of the falling leaves (and of the leaf textures). */
    public static final int ROOD = 0xFFD9432F, ORANJE = 0xFFF28C28;

    /** Esdoorn leaves: now and then a leaf of this colour floats down. */
    public static class Bladeren extends LeavesBlock {
        public static final MapCodec<Bladeren> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(Codec.INT.fieldOf("kleur").forGetter(b -> b.kleur), propertiesCodec()).apply(i, Bladeren::new));
        private final int kleur;

        public Bladeren(int kleur, Properties properties) {
            super(0.03f, properties);
            this.kleur = kleur;
        }

        @Override
        public MapCodec<? extends LeavesBlock> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, kleur));
        }
    }

    /** The sapling: grows a red or an orange esdoorn. Happy on moss and on the Guhmensie's wool too. */
    public static class Zaailing extends SaplingBlock {
        public Zaailing(Properties properties) {
            super(BlokkenDalSlice.ESDOORN_GROEI, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || (state.isFaceSturdy(level, pos, Direction.UP)
                    && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS));
        }
    }

    private Esdoorn() {
    }
}
