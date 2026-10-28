package nl.juiced.guhs.feature.hamsterbuis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.trap5.Thuispoort;

/**
 * A hamsterbuis station ({@code guhs:hamsterbuis_station}): a see-through hamster tube entrance you can put anywhere, in any
 * dimension, with a name of its own. It asks no vadskracht itself: the owner's Hamsterknooppunt pays every trip. Step into
 * the tube (or click it) and pick where to go; woesj, you come out of the other station. Who may use it: the owner, their
 * droomwolkje friends, and everyone when it is open ({@link HamsterNet#mag}).
 */
public final class Station {
    /** After arriving or closing the screen the tube waits this long (ticks) before it opens its screen again by itself. */
    static final int RUST = 60;
    /** player -> game time until which stepping into a tube does nothing (so you don't bounce straight back). */
    static final Map<UUID, Long> RUST_TOT = new HashMap<>();

    private Station() {
    }

    public static class BlockImpl extends BaseEntityBlock {
        public static final MapCodec<BlockImpl> CODEC = simpleCodec(BlockImpl::new);
        public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
        /** Only the ring at the bottom stops you: you walk into the tube. */
        private static final VoxelShape VLOER = Block.box(0, 0, 0, 16, 2, 16);
        private static final VoxelShape OMTREK = Block.box(1, 0, 1, 15, 16, 15);

        public BlockImpl(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return OMTREK;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VLOER;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof BlockEntityImpl s) {
                s.inschrijven(player);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl s) {
                s.open(speler);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
                                    boolean isPrecise) {
            if (entity instanceof ServerPlayer speler && !speler.isShiftKeyDown() && level.getBlockEntity(pos) instanceof BlockEntityImpl s) {
                long nu = level.getGameTime();
                if (RUST_TOT.getOrDefault(speler.getUUID(), 0L) <= nu) {
                    RUST_TOT.put(speler.getUUID(), nu + RUST);
                    s.open(speler);
                }
            }
        }
    }

    public static class BlockEntityImpl extends BlockEntity {
        @Nullable
        UUID id;
        @Nullable
        UUID eigenaar;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(HamsterbuisFeature.STATION_BE.get(), pos, state);
        }

        @Nullable
        public UUID id() {
            return id;
        }

        /** A new station of this player: into the network, named "Hamsterbuis n". */
        public void inschrijven(ServerPlayer player) {
            HamsterNet net = HamsterNet.van(player.level().getServer());
            if (id == null) {
                id = UUID.randomUUID();
            }
            eigenaar = player.getUUID();
            int nr = net.aantal(player.getUUID()) + 1;
            net.zet(id, eigenaar, GlobalPos.of(player.level().dimension(), worldPosition),
                    Component.translatable("block.guhs.hamsterbuis_station.naam_n", nr).getString());
            setChanged();
            if (net.knooppunt(eigenaar) == null) {
                player.sendSystemMessage(Component.translatable("block.guhs.hamsterbuis_station.geen_knooppunt").withStyle(ChatFormatting.GOLD));
            }
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server && id != null && eigenaar != null) {
                HamsterNet net = HamsterNet.van(server.getServer());
                HamsterNet.Station s = net.station(id);
                GlobalPos hier = GlobalPos.of(server.dimension(), worldPosition);
                if (s == null) {
                    net.zet(id, eigenaar, hier, "Hamsterbuis");
                } else if (!s.plek.equals(hier)) {
                    net.zet(id, eigenaar, hier, s.naam);
                }
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level instanceof ServerLevel server && id != null) {
                HamsterNet.van(server.getServer()).wis(id);
            }
            super.preRemoveSideEffects(pos, state);
        }

        void open(ServerPlayer speler) {
            HamsterNet net = HamsterNet.van(speler.level().getServer());
            HamsterNet.Station hier = id == null ? null : net.station(id);
            if (hier == null) {
                if (eigenaar == null) {
                    inschrijven(speler);   // (put there by a command: the first one to use it owns it)
                    hier = net.station(id);
                }
                if (hier == null) {
                    return;
                }
            }
            if (!HamsterNet.mag(speler.level().getServer(), hier, speler.getUUID())) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.hamsterbuis_station.niet_van_jou"));
                return;
            }
            HamsterPayloads.stuur(speler, new HamsterPayloads.Open(worldPosition, scherm(speler, net, hier)));
        }

        private CompoundTag scherm(ServerPlayer speler, HamsterNet net, HamsterNet.Station hier) {
            CompoundTag d = new CompoundTag();
            d.putString("Naam", hier.naam);
            d.putBoolean("Eigenaar", hier.eigenaar.equals(speler.getUUID()));
            d.putBoolean("Openbaar", hier.openbaar);
            d.putLong("Lading", net.lading(hier.eigenaar));
            d.putLong("Max", HamsterbuisFeature.BUFFER);
            d.putInt("Kost", HamsterbuisFeature.REIS);
            d.putBoolean("Draait", net.draait(hier.eigenaar));
            d.putBoolean("Knooppunt", net.knooppunt(hier.eigenaar) != null);
            ListTag lijst = new ListTag();
            for (HamsterNet.Station s : net.bereikbaar(speler.level().getServer(), speler.getUUID())) {
                if (s.id.equals(hier.id)) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.putString("Id", s.id.toString());
                c.putString("Naam", s.naam);
                c.putString("Dim", s.plek.dimension().identifier().toString());
                c.putInt("X", s.plek.pos().getX());
                c.putInt("Y", s.plek.pos().getY());
                c.putInt("Z", s.plek.pos().getZ());
                c.putBoolean("Eigen", s.eigenaar.equals(speler.getUUID()));
                c.putBoolean("Draait", net.draait(s.eigenaar));
                lijst.add(c);
            }
            d.put("Stations", lijst);
            return d;
        }

        /** An action from the screen (server side, the player stands at this station). */
        void actie(ServerPlayer speler, int actie, String tekst) {
            if (id == null || speler.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 64) {
                return;
            }
            HamsterNet net = HamsterNet.van(speler.level().getServer());
            HamsterNet.Station hier = net.station(id);
            if (hier == null || !HamsterNet.mag(speler.level().getServer(), hier, speler.getUUID())) {
                return;
            }
            boolean eigen = hier.eigenaar.equals(speler.getUUID());
            switch (actie) {
                case HamsterPayloads.Actie.REIS -> {
                    try {
                        reis(speler, UUID.fromString(tekst));
                    } catch (IllegalArgumentException e) {
                        // (not an id)
                    }
                    return;
                }
                case HamsterPayloads.Actie.HERNOEM -> {
                    if (eigen && !tekst.isBlank()) {
                        net.hernoem(id, tekst.strip());
                    }
                }
                case HamsterPayloads.Actie.OPENBAAR -> {
                    if (eigen) {
                        net.zetOpenbaar(id, !hier.openbaar);
                    }
                }
                default -> {
                    return;
                }
            }
            HamsterPayloads.stuur(speler, new HamsterPayloads.Open(worldPosition, scherm(speler, net, net.station(id))));
        }

        /**
         * The trip: the destination's owner pays {@link HamsterbuisFeature#REIS} VK from their buffer (their network carries
         * you), then woesj. Returns whether the player went.
         */
        public boolean reis(ServerPlayer speler, UUID naarId) {
            HamsterNet net = HamsterNet.van(speler.level().getServer());
            HamsterNet.Station naar = net.station(naarId);
            if (naar == null || naarId.equals(id) || !HamsterNet.mag(speler.level().getServer(), naar, speler.getUUID())) {
                return false;
            }
            ServerLevel doelLevel = speler.level().getServer().getLevel(naar.plek.dimension());
            if (doelLevel == null) {
                speler.sendSystemMessage(Component.translatable("block.guhs.hamsterbuis_station.weg").withStyle(ChatFormatting.GOLD));
                return false;
            }
            doelLevel.getChunk(naar.plek.pos());   // (loads it: a station was placed there, the chunk exists)
            if (!(doelLevel.getBlockEntity(naar.plek.pos()) instanceof BlockEntityImpl doel) || !naarId.equals(doel.id)) {
                net.wis(naarId);
                speler.sendSystemMessage(Component.translatable("block.guhs.hamsterbuis_station.weg").withStyle(ChatFormatting.GOLD));
                return false;
            }
            if (!net.betaal(naar.eigenaar)) {
                speler.sendSystemMessage(Component.translatable("block.guhs.hamsterbuis_station.stil", HamsterbuisFeature.REIS)
                        .withStyle(ChatFormatting.GOLD));
                return false;
            }
            ServerLevel van = speler.level();
            van.sendParticles(ParticleTypes.CLOUD, speler.getX(), speler.getY() + 0.8, speler.getZ(), 16, 0.25, 0.6, 0.25, 0.08);
            van.sendParticles(ParticleTypes.BUBBLE_POP, speler.getX(), speler.getY() + 1, speler.getZ(), 24, 0.3, 0.8, 0.3, 0.1);
            van.playSound(null, speler.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
            RUST_TOT.put(speler.getUUID(), doelLevel.getGameTime() + RUST);
            Thuispoort.BlockEntityImpl.verplaats(speler, doelLevel, Vec3.atBottomCenterOf(naar.plek.pos()).add(0, 0.15, 0));
            doelLevel.sendParticles(ParticleTypes.CLOUD, Vec3.atCenterOf(naar.plek.pos()).x, naar.plek.pos().getY() + 0.8,
                    Vec3.atCenterOf(naar.plek.pos()).z, 16, 0.25, 0.6, 0.25, 0.08);
            doelLevel.playSound(null, naar.plek.pos(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1f, 1.4f);
            speler.sendOverlayMessage(Component.translatable("block.guhs.hamsterbuis_station.woesj", naar.naam));
            return true;
        }

        @Override
        protected void saveAdditional(ValueOutput uit) {
            super.saveAdditional(uit);
            uit.storeNullable("Id", UUIDUtil.CODEC, id);
            uit.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            id = in.read("Id", UUIDUtil.CODEC).orElse(null);
            eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        }
    }
}
