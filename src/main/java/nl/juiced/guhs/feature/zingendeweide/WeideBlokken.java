package nl.juiced.guhs.feature.zingendeweide;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import nl.juiced.guhs.feature.blub.BlubFeature;

/** The special blocks (and the towel) of De Zingende Weide. */
public final class WeideBlokken {
    private WeideBlokken() {
    }

    /** Salmon-clay grass and seagreen grass: plain soil for the meadow's plants (no spreading, never turns to dirt). */
    public static class Gras extends Block implements BonemealableBlock {
        public Gras(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return level.getBlockState(pos.above()).isAir();
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        /** Bonemeal: a little patch of the pink grasses and a singing flower now and then. */
        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            for (int i = 0; i < 24; i++) {
                BlockPos p = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
                if (level.getBlockState(p).getBlock() instanceof Gras && level.getBlockState(p.above()).isAir()) {
                    WeideFeatures.plantje(level, p.above(), random);
                }
            }
        }
    }

    /** The short pink grasses: bonemeal makes them tall (their own tall kind). */
    public static class KortGras extends TallGrassBlock {
        private final Supplier<? extends DoublePlantBlock> hoog;

        public KortGras(Supplier<? extends DoublePlantBlock> hoog, Properties properties) {
            super(properties);
            this.hoog = hoog;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            DoublePlantBlock h = hoog.get();
            if (h.defaultBlockState().canSurvive(level, pos) && level.isEmptyBlock(pos.above())) {
                DoublePlantBlock.placeAt(level, h.defaultBlockState(), pos, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The tall pink grasses (two blocks). */
    public static class HoogGras extends DoublePlantBlock {
        public HoogGras(Properties properties) {
            super(properties);
        }
    }

    /** A singing flower: now and then a little music note rises from it. */
    public static class Zingbloemetje extends FlowerBlock {
        public Zingbloemetje(Holder<MobEffect> effect, float seconds, Properties properties) {
            super(effect, seconds, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(40) == 0) {
                level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, random.nextInt(25) / 24.0, 0, 0);
            }
        }
    }

    /** A pastel meadow crystal: see-through, it rings softly when you step on it. */
    public static class Kristal extends TransparentBlock {
        public static final MapCodec<Kristal> CODEC = simpleCodec(Kristal::new);

        public Kristal(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends TransparentBlock> codec() {
            return CODEC;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (!level.isClientSide() && level.getRandom().nextInt(30) == 0) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.2f + level.getRandom().nextFloat() * 0.4f);
            }
            super.stepOn(level, pos, state, entity);
        }
    }

    /** Little pastel crystals (amethyst's shape): they glimmer. */
    public static class Kristalletjes extends AmethystClusterBlock {
        public Kristalletjes(float height, float width, Properties properties) {
            super(height, width, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                        0, 0.01, 0);
            }
        }
    }

    /** Wolkjesboom leaves: now and then a soft white fluff floats down (vanilla's cherry petal). */
    public static class Bladeren extends UntintedParticleLeavesBlock {
        public Bladeren(Properties properties) {
            super(0.04f, ParticleTypes.CHERRY_LEAVES, properties);
        }
    }

    /** One sapling grows a little wolkjesboom; four in a square a giant one. */
    public static class Zaailing extends SaplingBlock {
        public Zaailing(Properties properties) {
            super(ZingendeWeideFeature.GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.getBlock() instanceof Gras;
        }
    }

    /** A sleepy little guh face in a wolkjesboom's trunk. Right-click: a soft snore. An axe strips it. */
    public static class Gezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<Gezicht> CODEC = simpleCodec(Gezicht::new);

        public Gezicht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.4f, 0.55f + level.getRandom().nextFloat() * 0.15f);
                ((ServerLevel) level).sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
            return InteractionResult.SUCCESS;
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return ZingendeWeideFeature.GESTRIPT.get().defaultBlockState();
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }

    /** The towel of the bath hut: dry off, fresh and happy (the Blub's bath buff, Fris en blij). */
    public static class Handdoekje extends Item {
        public Handdoekje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide()) {
                player.addEffect(new MobEffectInstance(BlubFeature.FRIS_EN_BLIJ, 20 * 60 * 3, 0));
                player.removeEffect(ZingendeWeideFeature.BUBBELIG);
                ((ServerLevel) level).sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.6, player.getZ(), 4, 0.4, 0.3, 0.4, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
                player.sendSystemMessage(Component.translatable("gui.guhs.weide.handdoekje"));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
