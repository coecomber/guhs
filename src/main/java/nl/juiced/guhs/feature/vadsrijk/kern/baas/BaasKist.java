package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.storage.Nbt;

/**
 * The personal boss chest (ronde 11): one per player who helped more than 5 %. Only its owner can open it (the loot flies into
 * their bags, the rest lands at their feet for them only), then it goes "plof". Others get a friendly no.
 */
public class BaasKist extends BaseEntityBlock {
    public static final MapCodec<BaasKist> CODEC = simpleCodec(BaasKist::new);

    public BaasKist(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Inhoud(pos, state);
    }

    /** Puts a chest for {@code p} with this loot at {@code pos} (the coins go straight to them). */
    public static void plaats(ServerLevel sl, ServerPlayer p, BlockPos pos, LootTabel.Resultaat r) {
        if (r.munten() > 0) {
            Spelers.geefMunten(p, r.munten());
        }
        BlockPos plek = pos;
        for (int i = 0; i < 4 && !sl.getBlockState(plek).canBeReplaced(); i++) {
            plek = plek.above();
        }
        sl.setBlock(plek, KernRegistry.BAASKIST.get().defaultBlockState(), 3);
        if (sl.getBlockEntity(plek) instanceof Inhoud in) {
            in.eigenaar = p.getUUID();
            in.naam = p.getGameProfile().name();
            for (ItemStack s : r.items()) {
                in.items.add(Nbt.saveStack(sl.registryAccess(), s));
            }
            in.setChanged();
        }
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, plek.getX() + 0.5, plek.getY() + 1, plek.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.2);
        sl.playSound(null, plek, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1.4f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer p) || !(level.getBlockEntity(pos) instanceof Inhoud in)) {
            return InteractionResult.SUCCESS;
        }
        if (in.eigenaar != null && !in.eigenaar.equals(p.getUUID())) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.kern.baaskist.niet_jouw", in.naam).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        for (CompoundTag t : in.items) {
            ItemStack s = Nbt.parseStack(sl.registryAccess(), t);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack kopie = s.copy();
            Haken.LOOT.forEach(h -> h.accept(p, kopie));
            if (!p.getInventory().add(s) || !s.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(sl, p.getX(), p.getY(), p.getZ(), s);
                e.setTarget(p.getUUID());
                e.setUnlimitedLifetime();
                sl.addFreshEntity(e);
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.kern.baaskist.kreeg", kopie.getHoverName()));
        }
        in.items.clear();
        Vec3 c = Vec3.atCenterOf(pos);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y, c.z, 30, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 1.2f);
        sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.8f);
        sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return InteractionResult.SUCCESS;
    }

    /** The chest's owner and loot (raw item tags). */
    public static class Inhoud extends BlockEntity {
        @Nullable
        UUID eigenaar;
        String naam = "";
        final List<CompoundTag> items = new ArrayList<>();

        public Inhoud(BlockPos pos, BlockState state) {
            super(KernRegistry.BAASKIST_BE.get(), pos, state);
        }

        @Override
        protected void saveAdditional(ValueOutput out) {
            super.saveAdditional(out);
            if (eigenaar != null) {
                out.store("eigenaar", UUIDUtil.CODEC, eigenaar);
            }
            out.putString("naam", naam);
            out.store("items", CompoundTag.CODEC.listOf(), List.copyOf(items));
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            eigenaar = in.read("eigenaar", UUIDUtil.CODEC).orElse(null);
            naam = in.getStringOr("naam", "");
            items.clear();
            items.addAll(in.read("items", CompoundTag.CODEC.listOf()).orElse(List.of()));
        }
    }

    static Codec<UUID> uuid() {
        return UUIDUtil.CODEC;
    }
}
