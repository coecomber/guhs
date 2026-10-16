package nl.juiced.guhs.feature.einde2.trap5;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * The Knabbelpoort-buis ({@code guhs:knabbelpoort_buis}): a piece of tube with a tiny Knabbelpoort in it (DESIGN 6). What a
 * Knabbelbuis, a hopper or a chore guh puts into it (slot 0) jumps through the little portal to its linked partner, also in
 * another dimension, and comes out of the partner's FRONT into whatever stands there (a tube, a chest, a machine). Both
 * sides must be loaded and both must have vadskracht; a partner that cannot take it holds it (its face is surprised).
 * Linking: sneak + click one piece with an empty hand, then sneak + click the other (each remembers the other).
 */
public final class Poortbuis {
    /** (not saved) who is linking: player -> the first piece. */
    static final Map<UUID, GlobalPos> KOPPELT = new ConcurrentHashMap<>();

    private Poortbuis() {
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
            if (!(player instanceof ServerPlayer speler) || !(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof BlockEntityImpl buis)) {
                return InteractionResult.SUCCESS;
            }
            if (buis.oefen) {
                speler.sendSystemMessage(Component.translatable("block.guhs.knabbelpoort_buis.oefen").withStyle(ChatFormatting.AQUA));
                return InteractionResult.SUCCESS;
            }
            if (!player.isSecondaryUseActive()) {
                speler.sendOverlayMessage(buis.stand());
                return InteractionResult.SUCCESS;
            }
            GlobalPos hier = GlobalPos.of(server.dimension(), pos);
            GlobalPos eerste = KOPPELT.remove(speler.getUUID());
            if (eerste == null || eerste.equals(hier)) {
                KOPPELT.put(speler.getUUID(), hier);
                speler.sendOverlayMessage(Component.translatable("block.guhs.knabbelpoort_buis.eerste"));
                server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            ServerLevel daar = server.getServer().getLevel(eerste.dimension());
            if (daar == null || !daar.isLoaded(eerste.pos()) || !(daar.getBlockEntity(eerste.pos()) instanceof BlockEntityImpl ander)) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.knabbelpoort_buis.eerste_weg"));
                return InteractionResult.SUCCESS;
            }
            ander.koppel(hier);
            buis.koppel(eerste);
            speler.sendOverlayMessage(Component.translatable("block.guhs.knabbelpoort_buis.gekoppeld", eerste.pos().getX(), eerste.pos().getY(),
                    eerste.pos().getZ(), GuhVolger.dimensie(eerste.dimension())));
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.2f);
            server.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.3);
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity {
        public static final int IN = 0, UIT = 1;
        @Nullable
        private GlobalPos partner;
        boolean oefen;
        private int wacht;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.POORTBUIS_BE.get(), pos, state, T5Getallen.POORTBUIS, 2);
        }

        @Nullable
        public GlobalPos partner() {
            return partner;
        }

        public void koppel(@Nullable GlobalPos naar) {
            partner = naar;
            sync();
        }

        @Override
        protected boolean isInvoer(int vak) {
            return vak == IN;
        }

        @Override
        protected boolean isUitvoer(int vak) {
            return vak == UIT;
        }

        @Override
        protected boolean kanWerken() {
            return vakken().getAmountAsInt(IN) > 0 || vakken().getAmountAsInt(UIT) > 0;
        }

        @Override
        protected boolean isVol() {
            return vakken().getAmountAsInt(UIT) >= vakken().getResource(UIT).getMaxStackSize() && vakken().getAmountAsInt(UIT) > 0;
        }

        /** The partner piece, when it is loaded (null: no partner, or not loaded). */
        @Nullable
        BlockEntityImpl ander(ServerLevel hier) {
            if (partner == null) {
                return null;
            }
            ServerLevel daar = hier.getServer().getLevel(partner.dimension());
            return daar != null && daar.isLoaded(partner.pos()) && daar.getBlockEntity(partner.pos()) instanceof BlockEntityImpl b ? b : null;
        }

        @Override
        protected void werk() {
            if (!(level instanceof ServerLevel server) || ++wacht < T5Getallen.POORTBUIS_TIK) {
                return;
            }
            wacht = 0;
            // out of the front: what came through the portal
            if (vakken().getAmountAsInt(UIT) > 0) {
                Direction voor = getBlockState().getValue(MachineBlock.FACING);
                ResourceHandler<ItemResource> daar = Kisten.van(server, worldPosition.relative(voor), voor.getOpposite());
                if (daar != null) {
                    Kisten.verplaats(handler(null), daar, s -> true, T5Getallen.POORTBUIS_STAPEL);
                }
            }
            // through the portal: to the partner's out slot
            if (vakken().getAmountAsInt(IN) > 0) {
                BlockEntityImpl b = ander(server);
                if (b == null || !b.heeftKracht() || !(b.getLevel() instanceof ServerLevel daarLevel)) {
                    return;
                }
                ItemResource soort = vakken().getResource(IN);
                int n = Math.min(T5Getallen.POORTBUIS_STAPEL, vakken().getAmountAsInt(IN));
                ItemStack stuur = soort.toStack(n);
                ItemStack rest = b.ontvang(stuur);
                int weg = n - rest.getCount();
                if (weg > 0) {
                    Kisten.neem(vakken(), s -> true, weg);
                    server.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 6,
                            0.2, 0.2, 0.2, 0.2);
                    daarLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, b.getBlockPos().getX() + 0.5, b.getBlockPos().getY() + 0.6,
                            b.getBlockPos().getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                    if (!oefen) {
                        E2Trap5.meld(T5Machine.speler(server, eigenaar()), E2Trap5.P_POORTBUIS);
                    }
                }
            }
        }

        /** Takes what fits into the out slot; returns the rest. */
        ItemStack ontvang(ItemStack stack) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemResource er = vakken().getResource(UIT);
            if (!er.isEmpty() && !er.matches(stack)) {
                return stack;
            }
            int ruimte = stack.getMaxStackSize() - vakken().getAmountAsInt(UIT);
            int n = Math.min(ruimte, stack.getCount());
            if (n <= 0) {
                return stack;
            }
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                int erin = vakken().insert(UIT, ItemResource.of(stack), n, tx);
                tx.commit();
                return stack.copyWithCount(stack.getCount() - erin);
            }
        }

        Component stand() {
            if (partner == null) {
                return Component.translatable("block.guhs.knabbelpoort_buis.los");
            }
            return Component.translatable("block.guhs.knabbelpoort_buis.stand", partner.pos().getX(), partner.pos().getY(), partner.pos().getZ(),
                    GuhVolger.dimensie(partner.dimension()));
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(stand().copy().withStyle(partner == null ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }

        @Override
        protected void clientTick() {
            if (level != null && bezig() && level.getRandom().nextInt(4) == 0) {
                level.addParticle(ParticleTypes.PORTAL, worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6, 0, 0.1, 0);
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.storeNullable("Partner", GlobalPos.CODEC, partner);
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            partner = in.read("Partner", GlobalPos.CODEC).orElse(null);
            oefen = in.getBooleanOr("Oefen", false);
        }
    }
}
