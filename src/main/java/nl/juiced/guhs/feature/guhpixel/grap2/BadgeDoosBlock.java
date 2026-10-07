package nl.juiced.guhs.feature.guhpixel.grap2;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * The Badgedoosje ({@code guhs:guhmon_badgedoos}): the keepsake of the Guhmon-gevecht, an open little case with room for
 * eight gymbadges. Right-click it with a badge to put the badge in (one block state property per badge); breaking the
 * case gives the case and its badges back (its loot table). Five places stay empty for ever: the other gyms are closed
 * for a nap.
 */
public class BadgeDoosBlock extends DecoBlock {
    public static final BooleanProperty DUTJES = BooleanProperty.create("dutjes"), NJEG = BooleanProperty.create("njeg"),
            KNABBEL = BooleanProperty.create("knabbel");
    public static final VoxelShape VORM = Block.box(1, 0, 3, 15, 4, 13);
    private static final MapCodec<BadgeDoosBlock> CODEC = simpleCodec(BadgeDoosBlock::new);

    public BadgeDoosBlock(Properties properties) {
        super(properties, VORM);
        registerDefaultState(defaultBlockState().setValue(DUTJES, false).setValue(NJEG, false).setValue(KNABBEL, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DUTJES, NJEG, KNABBEL);
    }

    public static BooleanProperty eigenschap(Guhmon.Badge b) {
        return switch (b) {
            case DUTJES -> DUTJES;
            case NJEG -> NJEG;
            case KNABBEL -> KNABBEL;
        };
    }

    public static int aantal(BlockState state) {
        int n = 0;
        for (Guhmon.Badge b : Guhmon.Badge.values()) {
            if (state.getValue(eigenschap(b))) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        for (Guhmon.Badge b : Guhmon.Badge.values()) {
            if (!stack.is(b.item())) {
                continue;
            }
            if (state.getValue(eigenschap(b))) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.guhmon.badgedoos.zit_er_al").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(eigenschap(b), true), Block.UPDATE_ALL);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.3f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.guhmon.badgedoos.stand", aantal(state)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }
}
