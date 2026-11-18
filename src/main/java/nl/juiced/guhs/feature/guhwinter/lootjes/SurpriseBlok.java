package nl.juiced.guhs.feature.guhwinter.lootjes;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A placed Surprise (design R5/R6: "vast decor met botsing; je kunt erop lopen, maar niets erin werkt"). ONE object made of
 * invisible blocks: the {@link Kern} (one per gift; its block entity holds the whole gift and draws it, client:
 * SurpriseRenderer) and a {@link Deel} for every other block of the build. Each of them takes the shape of the block it
 * stands for (stairs are stairs, a fence is a fence: you walk on it and bump into it), lets light through, gives the light of
 * a lantern it stands for ({@link #LICHT}), and does nothing: doors, buttons and beds are decoration. Unbreakable (pistons,
 * explosions, withers and fluids too); the one who placed it picks the whole gift up again with shift + right-click
 * ({@link SurprisePlaatsen}).
 */
public abstract class SurpriseBlok extends Block implements EntityBlock {
    public static final IntegerProperty LICHT = IntegerProperty.create("licht", 0, 15);

    protected SurpriseBlok(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LICHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LICHT);
    }

    /** The block this part stands for (null: its data isn't there, e.g. on the client a moment before it arrives). */
    @Nullable
    public static BlockState inhoud(BlockGetter level, BlockPos pos) {
        BlockEntity b;
        try {
            b = level.getBlockEntity(pos);
        } catch (RuntimeException e) {
            return null;
        }
        return b instanceof SurpriseBlokEntity s ? s.inhoud : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState in = inhoud(level, pos);
        if (in == null || in.getBlock() instanceof SurpriseBlok) {
            return Shapes.block();
        }
        VoxelShape s = in.getShape(level, pos, context);
        return s.isEmpty() ? Shapes.box(0.25, 0, 0.25, 0.75, 0.5, 0.75) : s;   // (a flower: still something to click)
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState in = inhoud(level, pos);
        if (in == null || in.getBlock() instanceof SurpriseBlok) {
            return Shapes.block();
        }
        return in.getCollisionShape(level, pos, context);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            SurprisePlaatsen.toon(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The part that holds the whole gift and draws it. */
    public static final class Kern extends SurpriseBlok {
        public static final MapCodec<Kern> CODEC = simpleCodec(Kern::new);

        public Kern(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SurpriseBlokEntity.kern(pos, state);
        }
    }

    /** Every other block of the gift. */
    public static final class Deel extends SurpriseBlok {
        public static final MapCodec<Deel> CODEC = simpleCodec(Deel::new);

        public Deel(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return SurpriseBlokEntity.deel(pos, state);
        }
    }
}
