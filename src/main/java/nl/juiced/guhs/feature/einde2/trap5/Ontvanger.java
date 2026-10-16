package nl.juiced.guhs.feature.einde2.trap5;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;

/**
 * The Vadsontvanger ({@code guhs:vadsontvanger}): a source in ITS net that gives exactly what that net is short of, as long
 * as its {@link Zender} sends (DESIGN 6). It tunes itself to the nearest zender of its owner within
 * {@link T5Getallen#ZENDER_BEREIK} blocks. Once per second it looks at its own net: what the machines there ask minus what the
 * other sources there give is what it asks of its zender ({@link #vraagt}); the zender adds all of that to its own demand,
 * so the two nets are one in the rule (too little anywhere = everything still). A source without a kind: no cap per net.
 */
public final class Ontvanger {
    /** The ontvangers that are loaded, per level (the zender counts them: no block scan). */
    static final Map<ResourceKey<Level>, Set<BlockPos>> GELADEN = new ConcurrentHashMap<>();

    private Ontvanger() {
    }

    public static class Block extends BronBlock {
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
            if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BlockEntityImpl o) {
                o.eigenaar = player.getUUID();
                o.setChanged();
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BlockEntityImpl o) {
                player.sendOverlayMessage(o.stand());
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends BronBlockEntity {
        @Nullable
        private BlockPos zender;
        @Nullable
        UUID eigenaar;
        /** What its own net is short of without it (VK), at the last look. */
        private int vraagt;
        /** What it gives now (its zender sends: {@link #vraagt}; else 0). */
        private int geeft;
        boolean oefen;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.ONTVANGER_BE.get(), pos, state);
        }

        @Nullable
        public BlockPos zender() {
            return zender;
        }

        public int vraagt() {
            return vraagt;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server) {
                GELADEN.computeIfAbsent(server.dimension(), k -> ConcurrentHashMap.newKeySet()).add(worldPosition);
            }
        }

        @Override
        public void setRemoved() {
            super.setRemoved();
            if (level != null && !level.isClientSide()) {
                Set<BlockPos> s = GELADEN.get(level.dimension());
                if (s != null) {
                    s.remove(worldPosition);
                }
            }
        }

        @Nullable
        @Override
        public BronSoort vadsSoort() {
            return null;
        }

        @Override
        public int vadsAanbod() {
            return geeft;
        }

        @Override
        protected Snoet gezicht() {
            return geeft > 0 ? Snoet.WERKT : zender != null && vraagt > 0 ? Snoet.VOL : Snoet.SLAAPT;
        }

        @Override
        protected void tik(ServerLevel level) {
            if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), 20L) != 3) {
                return;
            }
            // 1. the zender: still there, still in range? else the nearest one with room
            Zender.BlockEntityImpl z = zender != null && level.isLoaded(zender) && level.getBlockEntity(zender) instanceof Zender.BlockEntityImpl zz
                    && zender.distSqr(worldPosition) <= (double) T5Getallen.ZENDER_BEREIK * T5Getallen.ZENDER_BEREIK ? zz : null;
            if (z == null) {
                z = Zender.zoek(level, worldPosition, eigenaar, null);
                BlockPos nieuw = z == null ? null : z.getBlockPos();
                if (nieuw == null ? zender != null : !nieuw.equals(zender)) {
                    zender = nieuw;
                    sync();
                }
            }
            // 2. what its own net is short of without it
            VadsNet net = VadsKracht.net(level, worldPosition);
            int anderen = net.aanbod() - (net.teltMee(this) ? geeft : 0);
            int nuVraagt = Math.max(0, net.vraag() - Math.max(0, anderen));
            // 3. what it gives: all of that, while the zender sends
            int nuGeeft = z != null && z.zendt() ? nuVraagt : 0;
            if (nuVraagt != vraagt || nuGeeft != geeft) {
                vraagt = nuVraagt;
                geeft = nuGeeft;
                sync();
            }
            if (geeft > 0 && !oefen && net.draait() && net.vraag() > 0) {
                E2Trap5.meld(T5Machine.speler(level, eigenaar), E2Trap5.P_ZENDER);
            }
        }

        Component stand() {
            if (zender == null) {
                return Component.translatable("block.guhs.vadsontvanger.geen_zender", T5Getallen.ZENDER_BEREIK).withStyle(ChatFormatting.GOLD);
            }
            return Component.translatable("block.guhs.vadsontvanger.stand", zender.getX(), zender.getY(), zender.getZ(), geeft, vraagt);
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(stand().copy().withStyle(zender == null ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }

        @Override
        protected void clientTick() {
            if (level != null && geeft > 0 && level.getRandom().nextInt(14) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5, 0, -0.02, 0);
            }
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            zender = in.read("Zender", BlockPos.CODEC).orElse(null);
            eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
            vraagt = Math.max(0, in.getIntOr("Vraagt", 0));
            geeft = Math.max(0, in.getIntOr("Geeft", 0));
            oefen = in.getBooleanOr("Oefen", false);
        }

        @Override
        protected void saveAdditional(ValueOutput uit) {
            super.saveAdditional(uit);
            uit.storeNullable("Zender", BlockPos.CODEC, zender);
            uit.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
            uit.putInt("Vraagt", vraagt);
            uit.putInt("Geeft", geeft);
            uit.putBoolean("Oefen", oefen);
        }
    }
}
