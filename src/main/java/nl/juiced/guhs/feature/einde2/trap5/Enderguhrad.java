package nl.juiced.guhs.feature.einde2.trap5;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Enderguhrad ({@code guhs:enderguhrad}, DESIGN 6): a giant wheel (5 x 5 blocks) in which a Vahoege Enderguh flies its
 * rounds: {@link T5Getallen#ENDERGUHRAD} VK, at most one per net ({@link BronSoort#ENDERGUHRAD}). Put the Enderguh in as a
 * picked-up guh; click the wheel with an empty hand to get it back. Only a Vahoege Enderguh flies high enough for it ("een
 * gewone guh krijgt er hoogtevrees van"). The wheel and the guh are drawn by {@code client.EnderguhradRenderer} (the
 * Guhrad's ring, three times as big, in ender colours).
 */
public final class Enderguhrad {
    private Enderguhrad() {
    }

    public static class Kern extends BronBlock {
        public static final MapCodec<Kern> CODEC = simpleCodec(Kern::new);

        public Kern(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        public int breed() {
            return 5;
        }

        @Override
        public int hoog() {
            return 5;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (!stack.is(ModItems.PICKED_UP_GUH.get())) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!(player instanceof ServerPlayer speler) || !(level.getBlockEntity(pos) instanceof BlockEntityImpl rad)) {
                return InteractionResult.SUCCESS;
            }
            if (rad.heeftGuh()) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.enderguhrad.bezet"));
                return InteractionResult.CONSUME;
            }
            CompoundTag guh = PickedUpGuhItem.guhData(stack);
            if (!GuhVariant.VAHOEGE_ENDER.id().equals(guh.getStringOr("Variant", ""))) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.enderguhrad.alleen_vahoege"));
                return InteractionResult.CONSUME;
            }
            rad.zet(guh, speler.getUUID());
            GuhVolger.item(stack, PlekSoort.GUHWIEL, level.dimension(), pos, "", level.getGameTime());
            stack.consume(1, speler);
            level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.4f);
            speler.sendOverlayMessage(Component.translatable("block.guhs.enderguhrad.vliegt", T5Getallen.ENDERGUHRAD));
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(player instanceof ServerPlayer speler) || !(level.getBlockEntity(pos) instanceof BlockEntityImpl rad)) {
                return InteractionResult.SUCCESS;
            }
            if (!rad.heeftGuh()) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.enderguhrad.leeg"));
                return InteractionResult.CONSUME;
            }
            if (rad.baas != null && !rad.baas.equals(speler.getUUID()) && !speler.getAbilities().instabuild) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.guh_wheel.not_yours"));
                return InteractionResult.CONSUME;
            }
            CompoundTag guh = rad.haalEruit();
            if (guh != null) {
                ItemStack item = PickedUpGuhItem.of(guh);
                GuhVolger.inZakken(item, speler);
                speler.getInventory().placeItemBackInInventory(item);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends BronBlockEntity {
        @Nullable
        private CompoundTag guh;
        @Nullable
        UUID baas;
        boolean oefen;
        // client
        private float draai, draaiO;
        @Nullable
        private GuhEntity toon;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.ENDERGUHRAD_BE.get(), pos, state);
        }

        public boolean heeftGuh() {
            return guh != null;
        }

        void zet(CompoundTag data, UUID wie) {
            guh = data.copy();
            baas = wie;
            sync();
        }

        @Nullable
        CompoundTag haalEruit() {
            CompoundTag uit = guh;
            guh = null;
            toon = null;
            sync();
            return uit;
        }

        @Override
        public BronSoort vadsSoort() {
            return BronSoort.ENDERGUHRAD;
        }

        @Override
        public int vadsAanbod() {
            return guh != null ? T5Getallen.ENDERGUHRAD : 0;
        }

        @Override
        protected Snoet gezicht() {
            return guh != null ? Snoet.WERKT : Snoet.SLAAPT;
        }

        @Override
        protected void tik(ServerLevel level) {
            if (guh != null && teltMee() && !oefen && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 100L) == 0) {
                E2Trap5.meld(T5Machine.speler(level, baas), E2Trap5.P_ENDERGUHRAD);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.5, 12,
                        1.2, 1.2, 0.3, 0.02);
            }
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(guh == null ? Component.translatable("block.guhs.enderguhrad.leeg").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("block.guhs.enderguhrad.vliegt", T5Getallen.ENDERGUHRAD).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // --- the client: the turning wheel and the flying guh ---

        @Override
        protected void clientTick() {
            draaiO = draai;
            if (guh == null) {
                toon = null;
                return;
            }
            draai += 4.5f;
            GuhEntity g = toonGuh();
            if (g != null) {
                g.tickCount++;
                g.walkAnimation.update(1.2f, 0.4f, 1.0f);
            }
            if (level != null && level.getRandom().nextInt(5) == 0) {
                level.addParticle(ParticleTypes.PORTAL, worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 3, worldPosition.getY() + 1
                        + level.getRandom().nextDouble() * 3, worldPosition.getZ() + 0.5, 0, 0, 0);
            }
        }

        public float draai(float partialTick) {
            return draaiO + (draai - draaiO) * partialTick;
        }

        @Nullable
        public GuhEntity toonGuh() {
            if (toon == null && guh != null && level != null) {
                toon = EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), guh), level, EntitySpawnReason.LOAD)
                        .filter(e -> e instanceof GuhEntity).map(e -> (GuhEntity) e).orElse(null);
                if (toon != null) {
                    toon.setInSittingPose(false);
                    toon.setRunningInWheel(true);
                }
            }
            return toon;
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level != null && !level.isClientSide() && guh != null) {
                ItemStack item = PickedUpGuhItem.of(guh);
                GuhVolger.item(item, PlekSoort.ITEM_GROND, level.dimension(), pos, "", level.getGameTime());
                Block.popResource(level, pos, item);
                guh = null;
            }
            super.preRemoveSideEffects(pos, state);
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            CompoundTag nieuw = in.read("Guh", CompoundTag.CODEC).orElse(null);
            if (nieuw == null || !nieuw.equals(guh)) {
                toon = null;
            }
            guh = nieuw;
            baas = in.read("Baas", UUIDUtil.CODEC).orElse(null);
            oefen = in.getBooleanOr("Oefen", false);
        }

        @Override
        protected void saveAdditional(ValueOutput uit) {
            super.saveAdditional(uit);
            uit.putBoolean("HeeftGuh", guh != null);   // (an empty update would be ignored by the client)
            if (guh != null) {
                uit.store("Guh", CompoundTag.CODEC, guh);
            }
            uit.storeNullable("Baas", UUIDUtil.CODEC, baas);
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveWithoutMetadata(registries);
        }
    }
}
