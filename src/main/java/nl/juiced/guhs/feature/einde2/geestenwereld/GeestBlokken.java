package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.Locale;
import java.util.function.IntFunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * einde2, slice geestenwereld: the blocks of the Geestenwereld that are more than a cube: the meditatiesteen (back to your
 * body), the gedenksteen (its lore), the lotuspuzzeltegel (turns a quarter per click), nevelwater (mist that falls up),
 * the lotusblad (half a block high) and the geestenmasker (a face on the wall of Koh's Grot). Resources:
 * tools/features/einde2_geestenwereld.py.
 */
public final class GeestBlokken {
    private GeestBlokken() {
    }

    /** Right-click in the Geestenwereld: your soul goes back to your body ({@link Geestenwereld#terug}). */
    public static class Meditatiesteen extends Block {
        public Meditatiesteen(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer p)) {
                return InteractionResult.PASS;
            }
            if (!Geestenwereld.in(level, pos)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_geestenwereld.niet_hier").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.SUCCESS;
            }
            ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.04);
            Geestenwereld.terug(p);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
            }
        }
    }

    /** A memorial stone: a click shows its lore (the marker {@code lore_<id>} at its spot names it). */
    public static class Gedenksteen extends HorizontalDirectionalBlock {
        public static final MapCodec<Gedenksteen> CODEC = simpleCodec(Gedenksteen::new);

        public Gedenksteen(BlockBehaviour.Properties properties) {
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
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                lees(p, Geestenwereld.in(level, pos) ? lore(pos) : null);
            }
            return InteractionResult.SUCCESS;
        }

        /** The lore id of the stone at this world position of the Geestenwereld, or null. */
        public static String lore(BlockPos pos) {
            Plekken.Plek p = Geestenwereld.plekOp(pos);
            return p != null && p.id().startsWith("lore_") ? p.id().substring("lore_".length()) : null;
        }

        /** Shows the lore (its title and text) to the player; without an id: the stone is too old to read. */
        public static void lees(ServerPlayer p, String id) {
            if (id == null) {
                p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.geen_lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return;
            }
            String key = "gui.guhs.einde2_geestenwereld.lore." + id;
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.lore_kop", Component.translatable(key + ".titel"))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            p.sendSystemMessage(Component.translatable(key + ".tekst").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Which piece of the big lotus a tile carries. */
    public enum Stuk implements StringRepresentable {
        HOEK, RAND, MIDDEN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A tile of the lotus puzzle before the gate of the White Lotus: a click turns it a quarter; when the nine lie as the
     * lotus, the puzzle is solved ({@link Lotuspuzzel}).
     */
    public static class Lotuspuzzeltegel extends Block {
        public static final EnumProperty<Stuk> STUK = EnumProperty.create("stuk", Stuk.class);
        public static final IntegerProperty DRAAI = IntegerProperty.create("draai", 0, 3);

        public Lotuspuzzeltegel(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STUK, Stuk.MIDDEN).setValue(DRAAI, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STUK, DRAAI);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            level.setBlock(pos, state.setValue(DRAAI, (state.getValue(DRAAI) + 1) % 4), Block.UPDATE_ALL);
            level.playSound(null, pos, E2GeestenwereldFeature.TEGEL.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            if (player instanceof ServerPlayer p && Geestenwereld.in(level, pos)) {
                Lotuspuzzel.na(p, (ServerLevel) level);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Mist that falls UP: see-through, no collision, rising sparks, and whoever stands in it floats down softly. */
    public static class Nevelwater extends HalfTransparentBlock {
        public static final MapCodec<Nevelwater> CODEC = simpleCodec(Nevelwater::new);

        public Nevelwater(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HalfTransparentBlock> codec() {
            return CODEC;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, 0.06 + random.nextDouble() * 0.05, 0);
            }
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
            Vec3 v = entity.getDeltaMovement();
            if (v.y < -0.08) {
                entity.setDeltaMovement(v.x, -0.08, v.z);
            }
            entity.resetFallDistance();
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
    }

    /** A lotus leaf: half a block high. */
    public static class Lotusblad extends Block {
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 8, 16);

        public Lotusblad(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }
    }

    /** A face on the wall of Koh's Grot: a thin plate on the block behind it, looking the way of FACING (gezicht 0..11). */
    public static class Geestenmasker extends HorizontalDirectionalBlock {
        public static final MapCodec<Geestenmasker> CODEC = simpleCodec(Geestenmasker::new);
        public static final IntegerProperty GEZICHT = IntegerProperty.create("gezicht", 0, 11);
        private static final IntFunction<VoxelShape> VORMEN = i -> switch (Direction.from2DDataValue(i)) {
            case NORTH -> Block.box(1, 1, 15, 15, 15, 16);
            case SOUTH -> Block.box(1, 1, 0, 15, 15, 1);
            case WEST -> Block.box(15, 1, 1, 16, 15, 15);
            default -> Block.box(0, 1, 1, 1, 15, 15);
        };

        public Geestenmasker(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GEZICHT, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, GEZICHT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction d = context.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, d).setValue(GEZICHT, context.getLevel().getRandom().nextInt(12));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORMEN.apply(state.getValue(FACING).get2DDataValue());
        }
    }
}
