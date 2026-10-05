package nl.juiced.guhs.feature.guhpixel.among;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * The three ship things of Among Guhs as blocks (the task panel, the emergency button, the vent) and the three game items
 * (the Mika's pillow and sabotage map, everybody's voting slip).
 * <p>
 * Inside a round a click on one of the blocks never reaches the block: AmongSlice catches it first
 * ({@link AmongSessie#gebruikBlok}; that also works for droomguhs). At home they are decoration with a little joke.
 */
public final class AmongBlokken {
    /** A ship block as decoration: a click plays a sound and says a line (lang {@code gui.guhs.among.thuis.<id>}). */
    public static class SchipBlock extends DecoBlock {
        private final String regel;
        private final Supplier<SoundEvent> geluid;
        private final MapCodec<SchipBlock> eigenCodec;

        public SchipBlock(Properties properties, VoxelShape noord, String regel, Supplier<SoundEvent> geluid) {
            super(properties, noord);
            this.regel = regel;
            this.geluid = geluid;
            this.eigenCodec = simpleCodec(p -> new SchipBlock(p, noord, regel, geluid));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return eigenCodec;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                level.playSound(null, pos, geluid.get(), SoundSource.BLOCKS, 0.7f, 1f);
                sp.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis." + regel).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A game item: it only does something in the hands of a participant of a running round. */
    public static class SpelItem extends Item {
        public enum Soort { KUSSEN, SABOTEERKAART, STEMBRIEFJE }

        private final Soort soort;

        public SpelItem(Properties properties, Soort soort) {
            super(properties);
            this.soort = soort;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                if (Sessies.van(sp) instanceof AmongSessie s) {
                    switch (soort) {
                        case KUSSEN -> s.duw(sp, -1);
                        case SABOTEERKAART -> s.kaart(sp);
                        default -> s.briefje(sp);
                    }
                } else {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis.spelitem").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    private AmongBlokken() {
    }
}
