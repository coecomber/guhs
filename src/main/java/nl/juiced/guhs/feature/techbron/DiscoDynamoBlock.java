package nl.juiced.guhs.feature.techbron;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * De Disco-dynamo: a dance floor of 3 x 3 blocks with a built-in turntable. The kern is the front middle block (with the
 * little DJ desk: the face, the ears and the turntable on top); the other eight are {@code guhs:techbron_vloer_deel}.
 * Right-click it (anywhere) with a music disc and it plays, over and over; tamed guhs within 8 blocks come and dance, and
 * every dancer gives vadskracht ({@link DiscoDynamoBlockEntity}). An empty hand takes the disc out again. Every disc has
 * its own light show on the floor ({@link LichtShow}); the floor gives light while it plays.
 */
public class DiscoDynamoBlock extends BronBlock {
    public static final MapCodec<DiscoDynamoBlock> CODEC = simpleCodec(DiscoDynamoBlock::new);
    /** How high the floor is, in pixels. */
    public static final int VLOER = 4;
    /** How bright the floor is while a disc plays. */
    public static final int LICHT = 13;
    private static final Map<Direction, VoxelShape> VORMEN = new EnumMap<>(Direction.class);

    static {
        VoxelShape vloer = Block.box(0, 0, 0, 16, VLOER, 16);
        // the DJ desk stands on the front edge of the kern (the model: 2..14 wide, up to 12 high, 6 deep)
        VORMEN.put(Direction.NORTH, Shapes.or(vloer, Block.box(2, VLOER, 0, 14, 12, 6)));
        VORMEN.put(Direction.SOUTH, Shapes.or(vloer, Block.box(2, VLOER, 10, 14, 12, 16)));
        VORMEN.put(Direction.WEST, Shapes.or(vloer, Block.box(0, VLOER, 2, 6, 12, 14)));
        VORMEN.put(Direction.EAST, Shapes.or(vloer, Block.box(10, VLOER, 2, 16, 12, 14)));
    }

    public DiscoDynamoBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int breed() {
        return 3;
    }

    @Override
    public int diep() {
        return 3;
    }

    @Override
    public Block deel() {
        return TechbronFeature.VLOER_DEEL.get();
    }

    /** (block properties) The floor shines while a disc plays. */
    public static int licht(BlockState state) {
        return state.getValue(SNOET) == Snoet.SLAAPT ? 0 : LICHT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.getOrDefault(state.getValue(FACING), VORMEN.get(Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiscoDynamoBlockEntity(pos, state);
    }

    /** A music disc in your hand: on the turntable it goes (the one that was on it comes back). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (JukeboxSong.fromStack(stack).isEmpty()) {
            return InteractionResult.PASS;   // (something else in your hand: the disc stays on, and you can build against the floor)
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DiscoDynamoBlockEntity disco) {
            ItemStack oud = disco.zetPlaat(stack.consumeAndReturn(1, player));
            if (!oud.isEmpty()) {
                TechbronFeature.geef(player, oud);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand: the disc comes off. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DiscoDynamoBlockEntity disco) {
            ItemStack plaat = disco.zetPlaat(ItemStack.EMPTY);
            if (plaat.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.techbron.disco.geen_plaat"));
            } else {
                TechbronFeature.geef(player, plaat);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
