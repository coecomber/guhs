package nl.juiced.guhs.feature.vadsrijk.puzzels;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * T-PUZZELS: every puzzle block is this one class with a {@link #soort} (= the json element "blok"). Two shared properties:
 * {@code aan} (lit / pressed / running) and {@code stand} (colour, note, symbol, mirror stand or direction; 0..7). Unbreakable,
 * no loot; all logic lives in {@link Puzzels} (a block that is not part of a known puzzle does nothing).
 */
public class PuzzelBlok extends Block {
    public static final MapCodec<PuzzelBlok> CODEC = simpleCodec(p -> new PuzzelBlok(p, "klanksteen"));
    public static final BooleanProperty AAN = BooleanProperty.create("aan");
    public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 7);
    private static final VoxelShape PLAAT = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape KNOP = Block.box(4, 0, 4, 12, 4, 12);
    private static final VoxelShape HENDEL = Block.box(4, 0, 4, 12, 10, 12);
    private static final VoxelShape LANTAARN = Block.box(5, 0, 5, 11, 9, 11);

    public final String soort;

    public PuzzelBlok(BlockBehaviour.Properties p, String soort) {
        super(p);
        this.soort = soort;
        registerDefaultState(stateDefinition.any().setValue(AAN, false).setValue(STAND, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AAN, STAND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (soort) {
            case "drukplaat", "eindplaat" -> PLAAT;
            case "probeerknop", "herstelknop" -> KNOP;
            case "hendel" -> HENDEL;
            case "startlantaarn" -> LANTAARN;
            default -> Shapes.block();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
            Puzzels.klik(sl, pos, p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // (a weapon in your hand must not stop a puzzle click: in the Vadsrijk you nearly always hold one)
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
            Puzzels.klik(sl, pos, p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if ((soort.equals("drukplaat") || soort.equals("eindplaat")) && level instanceof ServerLevel sl && entity instanceof ServerPlayer p) {
            Puzzels.stap(sl, pos, p);
        }
        super.stepOn(level, pos, state, entity);
    }
}
