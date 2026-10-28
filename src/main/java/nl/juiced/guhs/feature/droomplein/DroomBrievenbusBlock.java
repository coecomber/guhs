package nl.juiced.guhs.feature.droomplein;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;

/**
 * A brievenbusje of the Droomposthuisje: click it in a dream and a big bubble floats you to its island, when you have been
 * there before ({@link DroomBezocht}). The plein's own brievenbusje tells which islands you can post yourself to.
 */
public class DroomBrievenbusBlock extends HorizontalDirectionalBlock {
    static final VoxelShape VORM = Shapes.or(Block.box(7, 0, 7, 9, 8, 9), Block.box(4, 8, 3, 12, 15, 13));
    public static final MapCodec<DroomBrievenbusBlock> CODEC = simpleCodec(p -> new DroomBrievenbusBlock(p, DroomEilanden.PLEIN));
    static final String T = "gui.guhs.droomplein.";

    private final String eiland;

    public DroomBrievenbusBlock(Properties properties, String eiland) {
        super(properties);
        this.eiland = eiland;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public String eiland() {
        return eiland;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        post(p, eiland);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** What clicking the brievenbusje of this island does for this player (also used by the gametest). */
    static boolean post(ServerPlayer p, String eiland) {
        if (!DroomReis.inDroom(p)) {
            p.sendOverlayMessage(Component.translatable(T + "bus.wakker").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (eiland.equals(DroomEilanden.PLEIN)) {
            p.sendSystemMessage(Component.translatable(T + "bus.plein", DroomBezocht.lijst(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (!DroomBezocht.bezocht(p, eiland)) {
            p.sendOverlayMessage(Component.translatable(T + "bus.onbekend", Component.translatable("gui.guhs.dromen.eiland." + eiland))
                    .withStyle(ChatFormatting.GOLD));
            return false;
        }
        p.sendSystemMessage(Component.translatable(T + "bus.post" + p.getRandom().nextInt(3)).withStyle(ChatFormatting.LIGHT_PURPLE));
        DroomReis.zweef(p, eiland);
        return true;
    }

    /** Plain decoration facing one way (the reuzenwekker's little clocks). */
    public static class Deco extends HorizontalDirectionalBlock {
        public static final MapCodec<Deco> CODEC = simpleCodec(Deco::new);

        public Deco(Properties properties) {
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
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Block.box(2, 0, 6, 14, 16, 10);
        }
    }
}
