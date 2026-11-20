package nl.juiced.guhs.feature.slijmhuisje;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * Het Slijmhuisje: a sweet little house of three by three blocks (about three high) for one guh slime. This block is the
 * kern (the middle of the front edge, with the round door, the snoet and the bowl); the other 26 are
 * {@link SlijmhuisjeDeelBlock} (they pass every click on to here).
 * <ul>
 *   <li>right-click with an {@link OpgepakteGuhSlimeItem}: the slime moves in (when nobody lives here yet);</li>
 *   <li>sneak + an empty hand: the slime moves out again, back into your hand as the item;</li>
 *   <li>an empty hand: you take the slimeballs from the bowl (or hear how the slime is doing when the bowl is empty).</li>
 * </ul>
 * What happens inside: {@link SlijmhuisjeBlockEntity}.
 */
public class SlijmhuisjeBlock extends MachineBlock {
    public static final MapCodec<SlijmhuisjeBlock> CODEC = simpleCodec(SlijmhuisjeBlock::new);

    public SlijmhuisjeBlock(Properties p) {
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
    public int hoog() {
        return 3;
    }

    @Override
    public int diep() {
        return 3;
    }

    @Override
    protected Block deel() {
        return SlijmhuisjeFeature.SLIJMHUISJE_DEEL.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlijmhuisjeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(stack.getItem() instanceof OpgepakteGuhSlimeItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SlijmhuisjeBlockEntity huisje)) {
            return InteractionResult.SUCCESS;
        }
        if (huisje.heeftSlijm()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.bezet").withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }
        Component naam = stack.getHoverName();
        huisje.trekIn(stack);
        stack.shrink(1);   // (also in creative: the slime moves, it is not copied)
        level.playSound(null, pos, SoundEvents.SLIME_JUMP_SMALL, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.erin", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SlijmhuisjeBlockEntity huisje)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (!huisje.heeftSlijm()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.niemand").withStyle(ChatFormatting.GRAY));
                return InteractionResult.SUCCESS;
            }
            ItemStack slijm = huisje.trekUit();
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.eruit", slijm.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, slijm);
            } else if (!player.getInventory().add(slijm)) {
                player.drop(slijm, false);
            }
            level.playSound(null, pos, SoundEvents.SLIME_JUMP_SMALL, SoundSource.BLOCKS, 0.8f, 1.5f);
            return InteractionResult.SUCCESS;
        }
        ItemStack ballen = huisje.leegSchaal();
        if (!ballen.isEmpty()) {
            int n = ballen.getCount();
            if (!player.getInventory().add(ballen)) {
                player.drop(ballen, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.4f);
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.gepakt", n).withStyle(ChatFormatting.GREEN));
        } else if (!huisje.heeftSlijm()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.leeg_klik").withStyle(ChatFormatting.GRAY));
        } else if (!huisje.heeftKracht()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.slaapt_klik", huisje.slijm().getHoverName()).withStyle(ChatFormatting.GOLD));
        } else {
            player.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.schaal_leeg", huisje.slijm().getHoverName(),
                    Math.max(1, (SlijmhuisjeBlockEntity.TIJD - huisje.voortgang() + 19) / 20)).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }
}
