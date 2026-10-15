package nl.juiced.guhs.feature.einde2.trap5;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The Komeetkern ({@code guhs:komeetkern}, DESIGN 6): the shard of the knabbel comet of the finale, the strongest source of
 * the mod so far: {@link T5Getallen#KOMEETKERN} VK, at most one per net ({@link BronSoort#KOMEETKERN}), one per player.
 * <p>
 * It is not crafted. After the finale it lies at {@code vuurnatie_hoofdstad/komeetscherf}, shown only to a player who
 * finished the story and has not taken it yet (a block of their own through main's {@link Schijn}, glittering for them
 * alone); stepping onto it picks it up ({@link #kijk}). Broken, it drops itself: nobody ever loses theirs.
 */
public final class Komeetkern {
    private Komeetkern() {
    }

    // =====================================================================================================================
    // the shard at the capital
    // =====================================================================================================================

    /** Where the shard lies (world), or null while the capital's spot is not known. */
    @Nullable
    static BlockPos scherf(ServerLevel guheinde) {
        return Plekken.bekend(guheinde, Einde2Ids.VUURNATIE_HOOFDSTAD) ? Plekken.wereld(guheinde, Einde2Ids.VUURNATIE_HOOFDSTAD, "komeetscherf") : null;
    }

    /** Does this player still have to pick up the shard (and may)? */
    static boolean wacht(ServerPlayer p) {
        return E2Trap5.open(p) && !GuhQuests.saved(p).getBooleanOr(E2Trap5.KOMEET_GEPAKT, false);
    }

    /** Schijn: the shard as a block of its own for every player who may pick it up. */
    static void schijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (p.level().dimension() != GuheindeFeature.GUHEINDE || !wacht(p)) {
            return;
        }
        BlockPos s = scherf(p.level());
        if (s != null && Schijn.dichtbij(p, s)) {
            gewenst.put(s, E2Trap5Feature.KOMEETKERN.get().defaultBlockState().setValue(BronBlock.SNOET, Snoet.WERKT));
        }
    }

    /** Every 10 ticks per player in the Guheinde: glitter near the shard, and the pickup when they stand on it. */
    static void kijk(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel guheinde) || guheinde.dimension() != GuheindeFeature.GUHEINDE || !wacht(p)) {
            return;
        }
        BlockPos s = scherf(guheinde);
        if (s == null) {
            return;
        }
        Vec3 midden = Vec3.atCenterOf(s);
        double d = p.position().distanceTo(midden);
        if (d > 48) {
            return;
        }
        Schijn.deeltjes(p, ParticleTypes.END_ROD, midden.add(0, 0.6, 0), 3, 0.4, 0.02);
        if (d <= T5Getallen.SCHERF_AFSTAND) {
            pak(p);
        }
    }

    /** The player takes the shard: the Komeetkern, once. */
    static void pak(ServerPlayer p) {
        GuhQuests.saved(p).putBoolean(E2Trap5.KOMEET_GEPAKT, true);
        Minigames.give(p, new ItemStack(E2Trap5Feature.KOMEETKERN_ITEM.get()));
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_trap5_machines.komeet.gepakt").withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.7f);
        Schijn.deeltjes(p, ParticleTypes.FIREWORK, p.position().add(0, 1, 0), 30, 0.6, 0.1);
        Schijn.straks(p);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "guheinde/einde2_trap5_machines_komeet");
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, "einde2_trap5_machines_komeet_gepakt");
    }

    // =====================================================================================================================
    // the block
    // =====================================================================================================================

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
            if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BlockEntityImpl k) {
                k.baas = player.getUUID();
                k.setChanged();
            }
        }
    }

    public static class BlockEntityImpl extends BronBlockEntity {
        @Nullable
        UUID baas;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.KOMEETKERN_BE.get(), pos, state);
        }

        @Override
        public BronSoort vadsSoort() {
            return BronSoort.KOMEETKERN;
        }

        @Override
        public int vadsAanbod() {
            return T5Getallen.KOMEETKERN;
        }

        @Override
        protected Snoet gezicht() {
            return Snoet.WERKT;
        }

        @Override
        protected void tik(ServerLevel level) {
            if (teltMee() && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 100L) == 0) {
                E2Trap5.meld(T5Machine.speler(level, baas), E2Trap5.P_KOMEET);
            }
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(Component.translatable("block.guhs.komeetkern.regel", T5Getallen.KOMEETKERN).withStyle(ChatFormatting.GOLD));
        }

        @Override
        protected void clientTick() {
            if (level != null && level.getRandom().nextInt(3) == 0) {
                level.addParticle(level.getRandom().nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.FLAME,
                        worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.9, worldPosition.getY() + 1.05,
                        worldPosition.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.9, 0, 0.03, 0);
            }
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            baas = in.read("Baas", UUIDUtil.CODEC).orElse(null);
        }

        @Override
        protected void saveAdditional(ValueOutput uit) {
            super.saveAdditional(uit);
            uit.storeNullable("Baas", UUIDUtil.CODEC, baas);
        }
    }
}
