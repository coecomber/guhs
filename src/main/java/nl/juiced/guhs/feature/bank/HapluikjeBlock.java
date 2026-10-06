package nl.juiced.guhs.feature.bank;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The Hapluikje ({@code guhs:hapluikje}): a guh machine with a little hatch for a mouth. Whatever is put in (by hand: click
 * it with the stack; by hopper, Knabbelbuis, chore guh or Bezorgguhtje: through its item capability) lands in the Bank Guh
 * it is linked to, however far away. Click it with a Banksleutel that remembers a bank to link it; click it with an empty
 * hand to hear how it is doing. Needs vadskracht. Everything else: {@link HapluikjeBlockEntity}.
 */
public class HapluikjeBlock extends MachineBlock {
    public static final MapCodec<HapluikjeBlock> CODEC = simpleCodec(HapluikjeBlock::new);

    public HapluikjeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HapluikjeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof HapluikjeBlockEntity luikje) {
            if (stack.is(BankFeature.BANK_SLEUTEL.get())) {
                luikje.sleutel(speler, stack);
            } else {
                luikje.voer(speler, hand);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof HapluikjeBlockEntity luikje) {
            luikje.vertel(speler);
        }
        return InteractionResult.SUCCESS;
    }
}
