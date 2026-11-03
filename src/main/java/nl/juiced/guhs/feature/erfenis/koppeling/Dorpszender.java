package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * De Dorpszender ({@code guhs:dorpszender}, ronde 11 + balans 3.4): the upgrade of a Vadszender (Vadszender + 4 guhkristal + 1
 * blok kaasknabbels). It stands on your vadskracht net like any machine and asks the power it is set to send
 * ({@link #STAPPEN}: right-click = more, sneak + right-click = less); when its net runs, that power goes to the Vadsmast in
 * your village with {@value #VERLIES_PROCENT}% loss on the way (100 VK sent = 75 VK in the village). Several zenders add up
 * ({@link #beschikbaar}). The state is remembered in {@link KoppelingData} so it also counts while the base is unloaded.
 */
public final class Dorpszender {
    public static final int VERLIES_PROCENT = 25;
    /** What you can set it to send (VK, before the loss). */
    public static final int[] STAPPEN = {10, 20, 40, 60, 80, 100, 140, 200};
    public static final int START = 40;

    private Dorpszender() {
    }

    /** What arrives in the village from this many VK sent. */
    public static int aankomst(int vermogen) {
        return vermogen * (100 - VERLIES_PROCENT) / 100;
    }

    /** The VK that arrives in the village of this owner: every running Dorpszender, after the loss. */
    public static int beschikbaar(MinecraftServer s, UUID eigenaar) {
        int som = 0;
        for (KoppelingData.Zender z : KoppelingData.get(s).zenders.values()) {
            if (z.aan() && eigenaar.equals(z.eigenaar())) {
                som += aankomst(z.vermogen());
            }
        }
        return som;
    }

    public static class Block extends MachineBlock {
        public static final MapCodec<Block> CODEC = simpleCodec(Block::new);

        public Block(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BlockEntityImpl z) {
                if (z.eigenaar() == null) {
                    z.zetEigenaar(player.getUUID());
                }
                z.stap(player.isShiftKeyDown() ? -1 : 1);
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6f, 0.8f + z.vermogen / 250f);
                player.sendOverlayMessage(Component.translatable("erfenis.koppeling.dorpszender.stand", z.vermogen, aankomst(z.vermogen))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity {
        int vermogen = START;
        private boolean kracht;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(KoppelingFeature.DORPSZENDER_BE.get(), pos, state, START, 0);
        }

        public int vermogen() {
            return vermogen;
        }

        /** One step up or down the list (wraps around). */
        void stap(int richting) {
            int i = 0;
            for (int k = 0; k < STAPPEN.length; k++) {
                if (STAPPEN[k] == vermogen) {
                    i = k;
                }
            }
            zetVermogen(STAPPEN[Math.floorMod(i + richting, STAPPEN.length)]);
        }

        /** (also tests) sets what it sends; the net hears about it. */
        public void zetVermogen(int vk) {
            vermogen = Math.max(0, vk);
            setChanged();
            meld();
            onthoud();
        }

        @Override
        public int vadsVraag() {
            return aan() ? vermogen : 0;
        }

        @Override
        public void vadsStroom(boolean aan) {
            super.vadsStroom(aan);
            if (kracht != aan) {
                kracht = aan;
                onthoud();
            }
        }

        @Override
        public void zetEigenaar(@Nullable UUID wie) {
            super.zetEigenaar(wie);
            onthoud();
        }

        @Override
        public void onLoad() {
            super.onLoad();
            onthoud();
        }

        /** Remembers this zender (owner, power, running) in the saved data, so it counts while unloaded too. */
        void onthoud() {
            if (level instanceof ServerLevel server && eigenaar() != null) {
                KoppelingData.get(server.getServer()).zetZender(new KoppelingData.Zender(GlobalPos.of(server.dimension(), worldPosition),
                        eigenaar(), vermogen, kracht));
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level instanceof ServerLevel server) {
                KoppelingData.get(server.getServer()).haalZenderWeg(GlobalPos.of(server.dimension(), pos));
            }
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return false;
        }

        @Override
        protected void werk() {
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putInt("Vermogen", vermogen);
            uit.putBoolean("Kracht", kracht);
        }

        @Override
        protected void laden(ValueInput in) {
            vermogen = in.getIntOr("Vermogen", START);
            kracht = in.getBooleanOr("Kracht", false);
        }
    }
}
