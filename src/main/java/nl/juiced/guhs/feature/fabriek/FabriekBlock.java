package nl.juiced.guhs.feature.fabriek;

import java.util.function.BiFunction;

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
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.techmachine.TechBlock;

/**
 * The block of the five Fabriekshal machines (1.8.0): a {@link TechBlock} (facing, snoet, vadskracht, its screen) whose block
 * entity is made by the {@link Soort}. A Banksleutel links a {@link MetBank} machine to that Bank Guh. The Bouwblokken-fabriek: sneak +
 * empty hand starts linking a chest (click the chest next), sneak + empty hand on a linked fabriek forgets its links. The
 * Regenboogpers: sneak + empty hand picks the next colour.
 */
public class FabriekBlock extends TechBlock {
    public enum Soort {
        SPINMACHINE(SpinmachineBlockEntity::new),
        VERFBAD(VerfbadBlockEntity::new),
        KNUFFELRECYCLER(KnuffelrecyclerBlockEntity::new),
        SCHEERSTAL(ScheerstalBlockEntity::new),
        BOUWBLOKKEN_FABRIEK(BouwblokkenFabriekBlockEntity::new),
        KLEURMOLEN(KleurmolenBlockEntity::new),
        REGENBOOGPERS(RegenboogpersBlockEntity::new);

        final BiFunction<BlockPos, BlockState, BlockEntity> maak;

        Soort(BiFunction<BlockPos, BlockState, BlockEntity> maak) {
            this.maak = maak;
        }
    }

    private final Soort soort;
    private final MapCodec<FabriekBlock> codec;

    public FabriekBlock(Properties p, Soort soort) {
        super(p);
        this.soort = soort;
        this.codec = simpleCodec(q -> new FabriekBlock(q, soort));
    }

    public Soort soort() {
        return soort;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return soort.maak.apply(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(BankFeature.BANK_SLEUTEL.get()) || !(level.getBlockEntity(pos) instanceof MetBank machine)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            machine.sleutel(sp, stack, level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (soort == Soort.BOUWBLOKKEN_FABRIEK && player.isSecondaryUseActive()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BouwblokkenFabriekBlockEntity fabriek) {
                Koppelen.begin(sp, fabriek);
            }
            return InteractionResult.SUCCESS;
        }
        if (soort == Soort.REGENBOOGPERS && player.isSecondaryUseActive()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof RegenboogpersBlockEntity pers) {
                pers.volgendeKleur(sp);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
