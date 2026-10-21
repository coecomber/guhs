package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The Bouwkeet (design 2.2): a yellow-pink hut of 3 x 3 x 3 with a little crane on the roof, plans on the wall and a design
 * table inside, drawn by GeckoLib from its kern block (the bottom middle of the front; the rest is invisible
 * {@code machine_deel}). A click opens its menu; a Banksleutel that remembers a bank links that bank; an upgrade item
 * raises its level. Everything else is {@link BouwkeetBlockEntity}. Resources: tools/features/bouwhulp.py.
 */
public class BouwkeetBlock extends MachineBlock {
    public static final MapCodec<BouwkeetBlock> CODEC = simpleCodec(BouwkeetBlock::new);

    public BouwkeetBlock(Properties p) {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // GeckoLib draws the whole hut
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BouwkeetBlockEntity(pos, state);
    }

    /** The machine's own tick, then the keet's (progress, bouwguhs): also without vadskracht. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> machine = super.getTicker(level, state, type);
        if (level.isClientSide()) {
            return machine;
        }
        return (l, pos, s, be) -> {
            if (machine != null) {
                machine.tick(l, pos, s, be);
            }
            if (be instanceof BouwkeetBlockEntity keet && !keet.isRemoved()) {
                keet.altijd();
            }
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(BankFeature.BANK_SLEUTEL.get())) {
            UUID bank = BankSleutelItem.bank(stack);
            if (bank == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BouwkeetBlockEntity keet && player instanceof ServerPlayer sp) {
                if (!magBewerken(keet, sp)) {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.niet_van_jou"));
                } else {
                    keet.zetBank(bank);
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.bank_gekoppeld"));
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.6f);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof BouwkeetUpgradeItem upgrade) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BouwkeetBlockEntity keet && player instanceof ServerPlayer sp) {
                upgrade.gebruik(stack, keet, sp);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BouwkeetBlockEntity keet && player instanceof ServerPlayer sp) {
            BouwkeetPayloads.open(sp, keet, null);
        }
        return InteractionResult.SUCCESS;
    }

    /** Who may change a keet: whoever placed it, an op, or anybody when it was placed by a structure or a command. */
    public static boolean magBewerken(BouwkeetBlockEntity keet, ServerPlayer p) {
        return keet.eigenaar() == null || keet.eigenaar().equals(p.getUUID())
                || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Bouwguhs.keetWeg(level, pos);
    }
}
