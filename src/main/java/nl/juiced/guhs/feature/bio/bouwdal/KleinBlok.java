package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * biomes3 bouw-dal: a small decoration block that faces the one who places it. The shape is given for facing north. Used
 * for the twelve blocks of the Japan verzamelreeks (with an item) and for the fixed things in het weebhuisje (no item,
 * cannot be broken). With a klik key the block answers a right click with one line above the hotbar.
 */
public class KleinBlok extends HorizontalDirectionalBlock {
    private final Map<Direction, VoxelShape> vormen;
    @Nullable
    protected final String klik;
    private final MapCodec<KleinBlok> codec;

    public KleinBlok(Properties properties, VoxelShape noord, @Nullable String klik) {
        super(properties);
        this.vormen = Shapes.rotateHorizontal(noord);
        this.klik = klik;
        this.codec = simpleCodec(p -> new KleinBlok(p, noord, klik));
        registerDefaultState(standaard());
    }

    protected BlockState standaard() {
        return stateDefinition.any().setValue(FACING, Direction.NORTH);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vormen.get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** The line this block says when clicked (lang key), or null. */
    @Nullable
    protected String klikTekst(BlockState state) {
        return klik;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        String key = klikTekst(state);
        if (key == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    /** The windgong: tinkles softly now and then (client side only). */
    public static class Windgong extends KleinBlok {
        public Windgong(Properties properties, VoxelShape noord) {
            super(properties, noord, null);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(70) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                        0.35f, 1.3f + random.nextFloat() * 0.5f, false);
            }
        }
    }

    /** A block that comes in a few looks (the shelves of figurines, the posters). */
    public static class Soort extends KleinBlok {
        public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 3);

        public Soort(Properties properties, VoxelShape noord, @Nullable String klik) {
            super(properties, noord, klik);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SOORT);
        }
    }

    /**
     * A block with two states that the house sets itself: the loket sign (AAN = the loket is open) and the note on the
     * door (AAN = it hangs there; without it the block is nothing at all).
     */
    public static class Stand extends KleinBlok {
        public static final BooleanProperty AAN = BooleanProperty.create("aan");
        private final boolean wegAlsUit;

        public Stand(Properties properties, VoxelShape noord, String klik, boolean wegAlsUit) {
            super(properties, noord, klik);
            this.wegAlsUit = wegAlsUit;
        }

        @Override
        protected BlockState standaard() {
            return super.standaard().setValue(AAN, false);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, AAN);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return wegAlsUit && !state.getValue(AAN) ? Shapes.empty() : super.getShape(state, level, pos, context);
        }

        @Override
        protected String klikTekst(BlockState state) {
            if (wegAlsUit) {
                return state.getValue(AAN) ? klik : null;
            }
            return klik + (state.getValue(AAN) ? ".open" : ".dicht");
        }
    }
}
