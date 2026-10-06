package nl.juiced.guhs.feature.techmachine;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.Minigames;

/**
 * De Plantagebak: a wooden bed of three by three blocks full of soil, with a guh face on the front plank. Put a sapling
 * or a zwammetje in it and, on vadskracht, a real tree stands in the middle within a minute
 * ({@link PlantagebakBlockEntity}). This block is the kern (the middle of the front edge); the other eight are
 * {@link PlantagebakDeelBlock}. Anything may be planted on the bed (it is soil for every plant), and a tree that grows on
 * it leaves the bed as it is.
 * <p>
 * Right-click with an axe while a tree stands: the whole tree comes down in one go, the harvest lands in your pockets.
 * Right-click with a sapling: it goes into the bak. Otherwise: the screen.
 */
public class PlantagebakBlock extends TechBlock {
    public static final MapCodec<PlantagebakBlock> CODEC = simpleCodec(PlantagebakBlock::new);

    public PlantagebakBlock(Properties p) {
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
    protected Block deel() {
        return TechmachineFeature.PLANTAGEBAK_DEEL.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlantagebakBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlantagebakBlockEntity bak)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(ItemTags.AXES) && bak.heeftBoom()) {
            if (!level.isClientSide() && player instanceof ServerPlayer speler) {
                for (ItemStack buit : bak.hak(speler)) {
                    Minigames.give(speler, buit);
                }
                stack.hurtAndBreak(1, speler, hand);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1f, 0.8f);
            }
            return InteractionResult.SUCCESS;
        }
        if (Plantagebakken.isZaailing(stack)) {
            if (!level.isClientSide() && bak.plant(stack) > 0) {
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1.1f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // --- the bed is soil --------------------------------------------------------------------------------------------

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        return facing == Direction.UP ? TriState.TRUE : TriState.DEFAULT;
    }

    /** A tree grows on the bed: it stays what it is (a tree would turn the block under its trunk into dirt). */
    @Override
    public boolean onTreeGrow(BlockState state, WorldGenLevel level, BiConsumer<BlockPos, BlockState> placeFunction, RandomSource randomSource,
                              BlockPos pos, TreeConfiguration config) {
        return true;
    }
}
