package nl.juiced.guhs.feature.guhpixel.bioscoop;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Popcornmachine: with one within {@link Voorstelling#POPCORN_BEREIK} blocks of the projector every guh in the audience
 * gets a bakje popcorn during the film (and the machine pops along). A player takes a bakje by right-clicking, once a
 * minute per player (it is a snack, not a farm).
 */
public class PopcornmachineBlock extends DecoBlock {
    public static final MapCodec<PopcornmachineBlock> CODEC = simpleCodec(PopcornmachineBlock::new);
    /** Ticks between two bakjes for one player. */
    public static final int WACHT = 1200;
    static final String LAATST = "guhs_px_bioscoop_popcorn";

    public PopcornmachineBlock(Properties properties) {
        super(properties, Block.box(1, 0, 1, 15, 16, 15));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            neem(server, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** The player takes a bakje popcorn; false: still munching on the last one. */
    public static boolean neem(ServerLevel level, BlockPos pos, ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        long nu = level.getGameTime();
        long laatst = saved.getLongOr(LAATST, Long.MIN_VALUE);
        if (laatst != Long.MIN_VALUE && nu >= laatst && nu - laatst < WACHT) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhbioscoop.popcorn.wacht").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        saved.putLong(LAATST, nu);
        Minigames.give(p, new ItemStack(BioscoopSlice.POPCORN.get()));
        plop(level, pos, 6);
        p.sendOverlayMessage(Component.translatable("gui.guhs.guhbioscoop.popcorn.pak").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Pop! A few kernels jump out of the top. */
    public static void plop(ServerLevel level, BlockPos pos, int aantal) {
        RandomSource r = level.getRandom();
        level.playSound(null, pos, BioscoopSlice.POP.get(), SoundSource.BLOCKS, 0.6f, 0.9f + r.nextFloat() * 0.4f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, BioscoopSlice.POPCORN.get()), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                aantal, 0.15, 0.05, 0.15, 0.06);
    }
}
