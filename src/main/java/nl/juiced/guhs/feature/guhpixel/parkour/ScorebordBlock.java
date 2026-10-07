package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.taal.Tekst;

/**
 * Het Scorebord of the Guh-parkour: a wide board on two legs that shows, per guh, the laps and the best lap time of the
 * Startpaaltje it belongs to (the nearest one within reach when it is placed; click it while laying out a route to link it
 * to that route). Right-click: the whole list in the chat, with the last lap time too.
 */
public class ScorebordBlock extends BaseEntityBlock {
    public static final MapCodec<ScorebordBlock> CODEC = simpleCodec(ScorebordBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(Block.box(0, 0, 6.5, 16, 24, 9.5));

    public ScorebordBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScorebordBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel sl && sl.getBlockEntity(pos) instanceof ScorebordBlockEntity bord) {
            List<StartpaalBlockEntity> palen = StartpaalBlockEntity.palen(sl, pos);
            if (!palen.isEmpty()) {
                bord.koppel(palen.get(0).getBlockPos());
                bord.zet(palen.get(0).rijen());
            }
            if (placer instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable(palen.isEmpty() ? "gui.guhs.guhparkour.bord.geen_paal" : "gui.guhs.guhparkour.bord.gekoppeld")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ScorebordBlockEntity bord) {
            ListTag rijen = bord.rijen();
            if (bord.paal() == null) {
                sp.sendSystemMessage(Component.translatable("gui.guhs.guhparkour.bord.geen_paal").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (rijen.isEmpty()) {
                sp.sendSystemMessage(Component.translatable("gui.guhs.guhparkour.bord.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                sp.sendSystemMessage(Component.translatable("gui.guhs.guhparkour.bord.kop").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                for (int i = 0; i < rijen.size(); i++) {
                    CompoundTag r = rijen.getCompoundOrEmpty(i);
                    sp.sendSystemMessage(Component.translatable("gui.guhs.guhparkour.bord.rij", i + 1, Tekst.get(r, "Naam"), ParkourStats.tijd(r.getIntOr("Beste", 0)),
                            r.getIntOr("Rondjes", 0), ParkourStats.tijd(r.getIntOr("Laatste", 0))));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
