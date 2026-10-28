package nl.juiced.guhs.feature.centrale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.techbron.BronBlock;

/**
 * De Mega-knuffelcentrale: a giant cushion of 5 x 3 (Meerblok's biggest footprint) (the kern is the corner you place; the rest are
 * {@code guhs:mega_knuffelcentrale_deel}). Sixteen tamed guhs come and cuddle on it and give a lot of vadskracht, as long
 * as they are cared for: food (knabbels, roze broodjes, blocks of kaasknabbels) and fresh blankets (wool) by hand or by
 * pipe ({@link MegaKnuffelcentraleBlockEntity}).
 */
public class MegaKnuffelcentraleBlock extends BronBlock {
    public static final MapCodec<MegaKnuffelcentraleBlock> CODEC = simpleCodec(MegaKnuffelcentraleBlock::new);
    /** How high the cushion is, in pixels. */
    public static final int HOOGTE = 6;
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, HOOGTE, 16);

    public MegaKnuffelcentraleBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int breed() {
        return 5;
    }

    @Override
    public int diep() {
        return 3;
    }

    @Override
    public Block deel() {
        return CentraleFeature.DEEL.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MegaKnuffelcentraleBlockEntity(pos, state);
    }

    /** Food or wool in the hand: into the compartments. Anything else: nothing, so you can build against it. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        boolean voer = MegaKnuffelcentraleBlockEntity.isVoer(stack), wol = MegaKnuffelcentraleBlockEntity.isWol(stack);
        if (!voer && !wol) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MegaKnuffelcentraleBlockEntity centrale)) {
            return InteractionResult.SUCCESS;
        }
        int erin = centrale.stop(stack, player.getAbilities().instabuild);
        if (erin <= 0) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.centrale.vol"));
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, voer ? SoundEvents.GENERIC_EAT.value() : SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
        player.sendOverlayMessage(centrale.status());
        return InteractionResult.SUCCESS;
    }

    /** An empty hand: how the guhs are doing. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MegaKnuffelcentraleBlockEntity centrale) {
            player.sendOverlayMessage(centrale.status());
        }
        return InteractionResult.SUCCESS;
    }
}
