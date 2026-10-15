package nl.juiced.guhs.feature.einde2.ballon;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase C, slice trap5_ballon: the Vredesballon-beeldje (block guhs:vredesballon), a reward of the line: the balloon,
 * small, on a little plinth. Placing it ticks off its FTB quest; a click lets a crumb of knabbel float out of the basket.
 */
public class BeeldjeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BeeldjeBlock> CODEC = simpleCodec(BeeldjeBlock::new);
    private static final VoxelShape VORM = Shapes.or(Block.box(3, 0, 3, 13, 3, 13), Block.box(2.5, 3, 2.5, 13.5, 16, 13.5));
    private static final String KLIK = "guhs_einde2_trap5_ballon_beeldje";

    public BeeldjeBlock(Properties properties) {
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
        return VORM;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player) {
            GuhAdvancements.grant(player, Vredesballon.ADV_BEELDJE);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            long nu = server.getGameTime();
            if (nu - player.getPersistentData().getLongOr(KLIK, 0L) > 30) {
                player.getPersistentData().putLong(KLIK, nu);
                server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25f, 1.8f);
                server.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6f, 1.6f);
                server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3, 0.05, 0.05, 0.05, 0.005);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.01);
                player.sendOverlayMessage(Component.translatable("block.guhs.vredesballon.klik").withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
