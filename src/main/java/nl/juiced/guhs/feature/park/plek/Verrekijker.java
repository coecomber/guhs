package nl.juiced.guhs.feature.park.plek;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * 1.4.2 (park-plekjes b): the verrekijker on the deck of the uitkijktoren (also a block of its own, for creative). Right-click
 * it (with anything in your hand): you look through it, zoomed in like a spyglass ({@link Kijk} to the client:
 * client/PlekBClient narrows the view, slows the mouse and draws the spyglass scope); sneak or step away to stop. The
 * first look grants the hidden advancement parkb_verrekijker.
 */
public class Verrekijker extends HorizontalDirectionalBlock {
    public static final MapCodec<Verrekijker> CODEC = simpleCodec(Verrekijker::new);
    /** How far you may stand from it while you look (blocks). */
    public static final double BIJ = 2.5;
    private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(Shapes.or(Block.box(3, 0, 3, 13, 11, 13), Block.box(5, 10, 0, 11, 16, 15)));

    public Verrekijker(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer sp) {
            kijk(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** This player looks through the verrekijker at {@code pos}. */
    public static void kijk(ServerPlayer p, BlockPos pos) {
        ModNetworking.sendTo(p, new Kijk(pos.immutable()));
        p.level().playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, 1f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.verrekijker.kijk").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(p, "parkb_verrekijker");
        if (Uitkijktoren.opToren(p.level(), pos)) {
            nl.juiced.guhs.quest.GuhQuests.hint(p, "gui.guhs.parkb.verrekijker.toren");
        }
    }

    // =====================================================================================================================
    /** guhs:parkb_kijk (server -> client): look through the verrekijker at this block. */
    public record Kijk(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Kijk> TYPE = new Type<>(Guhs.id("parkb_kijk"));
        public static final StreamCodec<FriendlyByteBuf, Kijk> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Kijk::pos, Kijk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kijk payload, IPayloadContext context) {
            nl.juiced.guhs.feature.park.plek.client.PlekBClient.kijk(payload.pos());
        }
    }
}
