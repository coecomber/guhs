package nl.juiced.guhs.feature.disco;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tall glass of guhshake on the bar of the Guhdisco (decoration): four flavours. Right-click for a slurp... it's only
 * for show, so the glass stays full.
 */
public class DiscoMilkshakeBlock extends Block {
    public enum Smaak implements StringRepresentable {
        AARDBEI, MUNT, CHOCO, KAAS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<DiscoMilkshakeBlock> CODEC = simpleCodec(DiscoMilkshakeBlock::new);
    public static final EnumProperty<Smaak> SMAAK = EnumProperty.create("smaak", Smaak.class);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 13, 11);

    public DiscoMilkshakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SMAAK, Smaak.AARDBEI));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SMAAK);
    }

    /** A random flavour when you put one down. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Smaak[] all = Smaak.values();
        return defaultBlockState().setValue(SMAAK, all[context.getLevel().getRandom().nextInt(all.length)]);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        level.playSound(player, pos, SoundEvents.GENERIC_DRINK, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("block.guhs.disco_milkshake.slurp").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }
}
