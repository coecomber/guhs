package nl.juiced.guhs.feature.techbron;

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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Het Blubkacheltje: a little stove with a glass jar on top in which a Sausblubje bobs. Right-click the empty stove with a
 * Sausblubje in a jar (any item of the tag {@code guhs:techbron/blubje_in_pot}; the item {@code guhs:sausblubje_potje} of
 * the sausdieren slice is in it) to put the blubje in, feed it a kaasknabbel now and then (right-click with it, or let a
 * Knabbelbuis or a hopper bring them: the tag {@code guhs:techbron/blubvoer}) and it gives vadskracht
 * ({@link BlubkacheltjeBlockEntity}). Sneak + an empty hand takes the blubje out again, in its jar, exactly as it went in.
 */
public class BlubkacheltjeBlock extends BronBlock {
    public static final MapCodec<BlubkacheltjeBlock> CODEC = simpleCodec(BlubkacheltjeBlock::new);
    /** How bright the stove is while the blubje is warm. */
    public static final int LICHT = 9;
    private static final VoxelShape VORM = Shapes.or(Block.box(1, 0, 1, 15, 8, 15), Block.box(3, 8, 3, 13, 16, 13));

    public BlubkacheltjeBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** (block properties) The stove glows while the blubje is warm. */
    public static int licht(BlockState state) {
        return state.getValue(SNOET) == Snoet.SLAAPT ? 0 : LICHT;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlubkacheltjeBlockEntity(pos, state);
    }

    /** A Sausblubje in a jar goes in (when the stove is empty); a knabbel goes into the bakje. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        boolean blubje = stack.is(TechbronFeature.BLUBJE_IN_POT), voer = stack.is(TechbronFeature.BLUBVOER);
        if (!blubje && !voer) {
            return InteractionResult.PASS;   // (something else in your hand: nothing, so you can build against the stove)
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlubkacheltjeBlockEntity kachel)) {
            return InteractionResult.SUCCESS;
        }
        if (blubje) {
            if (kachel.heeftBlubje()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.techbron.blub.bezet"));
                return InteractionResult.CONSUME;
            }
            kachel.zetBlubje(stack.consumeAndReturn(1, player));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 0.8f);
            player.sendOverlayMessage(Component.translatable(kachel.warm() ? "gui.guhs.techbron.blub.erin" : "gui.guhs.techbron.blub.erin_trek"));
            return InteractionResult.SUCCESS;
        }
        int erin = kachel.voer(stack, player.getAbilities().instabuild);
        if (erin <= 0) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.techbron.blub.vol"));
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, ModSounds.GUH_EAT.get(), SoundSource.BLOCKS, 0.6f, 1.5f);
        player.sendOverlayMessage(kachel.heeftBlubje() ? Component.translatable("gui.guhs.techbron.blub.gevoerd", kachel.voorraad(), TechbronGetallen.BLUB_VOER_MAX)
                : Component.translatable("gui.guhs.techbron.blub.voer_zonder_blubje"));
        return InteractionResult.SUCCESS;
    }

    /** An empty hand: how the blubje is doing; sneaking: the blubje comes out, jar and all. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlubkacheltjeBlockEntity kachel)) {
            return InteractionResult.SUCCESS;
        }
        if (!kachel.heeftBlubje()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.techbron.blub.leeg"));
        } else if (player.isShiftKeyDown()) {
            TechbronFeature.geef(player, kachel.zetBlubje(ItemStack.EMPTY));
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 0.8f);
        } else {
            player.sendOverlayMessage(kachel.warm() ? Component.translatable("gui.guhs.techbron.blub.warm", kachel.voorraad(), TechbronGetallen.BLUB_VOER_MAX)
                    : Component.translatable("gui.guhs.techbron.blub.trek"));
        }
        return InteractionResult.SUCCESS;
    }
}
