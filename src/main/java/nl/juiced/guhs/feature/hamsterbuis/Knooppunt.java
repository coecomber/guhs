package nl.juiced.guhs.feature.hamsterbuis;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsOpslag;

/**
 * The Hamsterknooppunt ({@code guhs:hamsterknooppunt}): the main station of an owner's hamsterbuis network, with spinning
 * see-through tubes and a guh in a wheel. One per owner (placing a new one makes that one the knooppunt). It asks no VK of
 * its own: like the Thuispoort it is a battery that only takes, and what its net has left over fills the owner's buffer in
 * {@link HamsterNet} (max {@link HamsterbuisFeature#BUFFER}). Every trip costs {@link HamsterbuisFeature#REIS} of it.
 */
public final class Knooppunt {
    private Knooppunt() {
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
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof BlockEntityImpl k) {
                k.wordtKnooppunt(player);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl k) {
                k.vertel(speler);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity implements VadsOpslag {
        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(HamsterbuisFeature.KNOOPPUNT_BE.get(), pos, state, 0, 0);
        }

        /** This one is the owner's knooppunt from now on. */
        public void wordtKnooppunt(ServerPlayer player) {
            zetEigenaar(player.getUUID());
            HamsterNet net = HamsterNet.van(player.level().getServer());
            GlobalPos oud = net.knooppunt(player.getUUID());
            GlobalPos hier = GlobalPos.of(player.level().dimension(), worldPosition);
            net.zetKnooppunt(player.getUUID(), hier);
            player.sendSystemMessage(Component.translatable(oud != null && !oud.equals(hier) ? "block.guhs.hamsterknooppunt.verhuisd"
                    : "block.guhs.hamsterknooppunt.nieuw").withStyle(ChatFormatting.LIGHT_PURPLE));
            sync();
        }

        /** Is this block the owner's registered knooppunt (an old one, replaced by a newer one, does nothing)? */
        boolean isHet() {
            UUID e = eigenaar();
            return e != null && level instanceof ServerLevel server
                    && GlobalPos.of(server.dimension(), worldPosition).equals(HamsterNet.van(server.getServer()).knooppunt(e));
        }

        void vertel(ServerPlayer speler) {
            UUID e = eigenaar();
            if (e == null) {
                wordtKnooppunt(speler);
                return;
            }
            HamsterNet net = HamsterNet.van(speler.level().getServer());
            if (!isHet()) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.hamsterknooppunt.niet_meer"));
                return;
            }
            speler.sendSystemMessage(Component.translatable("block.guhs.hamsterknooppunt.stand", net.lading(e), HamsterbuisFeature.BUFFER,
                    HamsterbuisFeature.REIS, net.aantal(e)).withStyle(net.draait(e) ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            // (the knooppunt of an owner without one, e.g. put back by a structure: it becomes theirs again)
            if (level instanceof ServerLevel server && eigenaar() != null && HamsterNet.van(server.getServer()).knooppunt(eigenaar()) == null) {
                HamsterNet.van(server.getServer()).zetKnooppunt(eigenaar(), GlobalPos.of(server.dimension(), worldPosition));
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level instanceof ServerLevel server && isHet()) {
                HamsterNet.van(server.getServer()).zetKnooppunt(eigenaar(), null);
            }
            super.preRemoveSideEffects(pos, state);
        }

        // --- vadskracht: a battery that only takes, into the owner's buffer ---

        @Override
        public long vadsInhoud() {
            return 0;
        }

        @Override
        public long vadsMax() {
            if (!(level instanceof ServerLevel server) || !isHet()) {
                return 0;
            }
            return HamsterbuisFeature.BUFFER - HamsterNet.van(server.getServer()).lading(eigenaar());
        }

        @Override
        public long vadsLaad(long vk) {
            if (!(level instanceof ServerLevel server) || !isHet()) {
                return 0;
            }
            return HamsterNet.van(server.getServer()).laad(eigenaar(), vk);
        }

        @Override
        public long vadsOntlaad(long vk) {
            return 0;
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return level instanceof ServerLevel server && eigenaar() != null
                    && HamsterNet.van(server.getServer()).lading(eigenaar()) >= HamsterbuisFeature.BUFFER;
        }

        @Override
        protected void werk() {
        }

        @Override
        protected boolean isInvoer(int vak) {
            return false;
        }

        @Override
        protected boolean isUitvoer(int vak) {
            return false;
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            if (level instanceof ServerLevel server && eigenaar() != null) {
                HamsterNet net = HamsterNet.van(server.getServer());
                regels.accept(Component.translatable("block.guhs.hamsterknooppunt.lading", net.lading(eigenaar()), HamsterbuisFeature.BUFFER)
                        .withStyle(ChatFormatting.AQUA));
                regels.accept(Component.translatable("block.guhs.hamsterknooppunt.reis", HamsterbuisFeature.REIS).withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        protected void clientTick() {
            if (level != null && level.getRandom().nextInt(5) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8,
                        worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, 0, 0.05, 0);
            }
        }
    }
}
