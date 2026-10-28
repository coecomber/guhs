package nl.juiced.guhs.feature.vadsbasis;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * De Knabbelketel ({@code guhs:knabbelketel}, 1.8.0): a craftable middle source of vadskracht that stokes kaasknabbels and
 * blocks of kaasknabbels ({@link KnabbelketelBlockEntity}). Right-click with knabbels (or a block of them) to fill its bakje,
 * or let a Knabbelbuis or a hopper do it; an empty hand tells how it is doing.
 */
public class KnabbelketelBlock extends BronBlock {
    public static final MapCodec<KnabbelketelBlock> CODEC = simpleCodec(KnabbelketelBlock::new);
    /** How bright the ketel is while it burns. */
    public static final int LICHT = 11;

    public KnabbelketelBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** (block properties) The ketel glows while it burns. */
    public static int licht(BlockState state) {
        return state.getValue(SNOET) == Snoet.SLAAPT ? 0 : LICHT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnabbelketelBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (KnabbelketelBlockEntity.waarde(stack) <= 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof KnabbelketelBlockEntity ketel)) {
            return InteractionResult.SUCCESS;
        }
        if (ketel.voer(stack, player.getAbilities().instabuild) <= 0) {
            player.sendOverlayMessage(Component.translatable("block.guhs.knabbelketel.vol"));
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
        player.sendOverlayMessage(Component.translatable("block.guhs.knabbelketel.gevoerd", ketel.voorraad()));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof KnabbelketelBlockEntity ketel)) {
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(ketel.brandt() ? Component.translatable("block.guhs.knabbelketel.brandt", ketel.voorraad(), ketel.brandTicks() / 20)
                : Component.translatable("block.guhs.knabbelketel.koud"));
        return InteractionResult.SUCCESS;
    }
}
