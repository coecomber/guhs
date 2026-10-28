package nl.juiced.guhs.feature.fossiel;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Fossiel-broedmachine ({@code guhs:fossiel_broedmachine}, 1.8.0 vadskracht): a guh machine with a glass bell on top.
 * Right-click with a fossil: it goes in (one at a time); with kaasknabbels: they go in; empty hand: how it is doing.
 * Everything else: {@link BroedmachineBlockEntity}.
 */
public class BroedmachineBlock extends MachineBlock {
    public static final MapCodec<BroedmachineBlock> CODEC = simpleCodec(BroedmachineBlock::new);
    private static final VoxelShape VORM = Shapes.or(box(0, 0, 0, 16, 9, 16), box(3, 9, 3, 13, 15, 13));

    public BroedmachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BroedmachineBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean fossiel = OerSoort.van(stack) != null;
        if (!fossiel && !stack.is(ModItems.KAAS_KNABBELS.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BroedmachineBlockEntity be) {
            ItemStack een = stack.copyWithCount(fossiel ? 1 : stack.getCount());
            ItemStack rest = Kisten.stop(be.handler(null), een);
            int erin = een.getCount() - rest.getCount();
            if (erin > 0) {
                if (!sp.isCreative()) {
                    stack.shrink(erin);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1.2f);
            } else if (fossiel) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.fossiel.al_een").withStyle(ChatFormatting.GRAY));
            }
            sp.sendOverlayMessage(be.status().copy().withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BroedmachineBlockEntity be) {
            sp.sendOverlayMessage(be.status().copy().withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }
}
