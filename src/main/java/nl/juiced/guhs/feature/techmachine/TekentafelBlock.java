package nl.juiced.guhs.feature.techmachine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * De Tekentafel: a little drawing table with a guh face. Lay a recipe on it, give it a sheet, and it draws the recipe on
 * a Bouwtekening ({@link TekentafelMenu}). It needs no vadskracht (it draws by hand, well, by snoet), but it has the three
 * faces all the same: asleep while nobody draws, awake while somebody stands at it, and surprised ("oh, that one I
 * know!") while a recipe it recognises lies on it. It has no block entity and so no moving part: its little animation is
 * on its sheet, where the drawing draws itself while somebody stands at it (an animated texture, see
 * tools/features/tech_machines_modellen.py).
 */
public class TekentafelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TekentafelBlock> CODEC = simpleCodec(TekentafelBlock::new);
    public static final EnumProperty<Snoet> SNOET = MachineBlock.SNOET;
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 15, 16);

    public TekentafelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SNOET, Snoet.SLAAPT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SNOET);
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
        if (!level.isClientSide() && player instanceof ServerPlayer speler) {
            speler.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TekentafelMenu(id, inventory, ContainerLevelAccess.create(level, pos), pos),
                    Component.translatable("block.guhs.tekentafel")), pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sets the table's face (when it still stands there). */
    static void zetSnoet(Level level, BlockPos pos, Snoet snoet) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TekentafelBlock && state.getValue(SNOET) != snoet) {
            level.setBlock(pos, state.setValue(SNOET, snoet), Block.UPDATE_CLIENTS);
        }
    }

    /** Is anybody else still drawing at this table? */
    static boolean nogIemand(ServerLevel level, BlockPos pos, Player behalve) {
        for (ServerPlayer p : level.players()) {
            if (p != behalve && p.containerMenu instanceof TekentafelMenu menu && menu.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
