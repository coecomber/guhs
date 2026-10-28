package nl.juiced.guhs.feature.koepel;

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
 * The five endgame machines of the Koepel (1.8.0, Dromen en vadskracht): one block class, the {@link Soort} says the size
 * (a {@link nl.juiced.guhs.feature.vadskracht.Meerblok} box: its parts are the ready-made guhs:machine_deel), which block
 * entity it gets and what a click does (the block entity's {@link KoepelMachine#klik}).
 */
public class KoepelBlock extends MachineBlock {
    public enum Soort {
        MEGA_OVEN("mega_oven", 3, 2, 2),
        WEERSTATION("knabbel_weerstation", 1, 2, 1),
        STERRENKOEPEL("sterrenkoepel", 3, 2, 2),
        DRAAIMOLEN("draaimolen", 3, 2, 2),
        WOLKENVANGER("wolkenvanger", 1, 1, 1);

        public final String id;
        final int breed, hoog, diep;

        Soort(String id, int breed, int hoog, int diep) {
            this.id = id;
            this.breed = breed;
            this.hoog = hoog;
            this.diep = diep;
        }
    }

    private final Soort soort;
    private final MapCodec<KoepelBlock> codec;

    public KoepelBlock(Soort soort, Properties properties) {
        super(properties);
        this.soort = soort;
        this.codec = simpleCodec(p -> new KoepelBlock(soort, p));
    }

    public Soort soort() {
        return soort;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public int breed() {
        return soort.breed;
    }

    @Override
    public int hoog() {
        return soort.hoog;
    }

    @Override
    public int diep() {
        return soort.diep;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (soort) {
            case MEGA_OVEN -> new MegaOvenBlockEntity(pos, state);
            case WEERSTATION -> new WeerstationBlockEntity(pos, state);
            case STERRENKOEPEL -> new SterrenkoepelBlockEntity(pos, state);
            case DRAAIMOLEN -> new DraaimolenBlockEntity(pos, state);
            case WOLKENVANGER -> new WolkenvangerBlockEntity(pos, state);
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.getBlockEntity(pos) instanceof KoepelMachine machine) {
            if (player instanceof ServerPlayer sp) {
                return machine.klik(sp, stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof KoepelMachine machine) {
            machine.klik(sp, ItemStack.EMPTY);
        }
        return InteractionResult.SUCCESS;
    }

    /** What every Koepel machine's block entity does with a click (an empty stack: an empty hand). True: handled. */
    public interface KoepelMachine {
        boolean klik(ServerPlayer speler, ItemStack inHand);
    }
}
