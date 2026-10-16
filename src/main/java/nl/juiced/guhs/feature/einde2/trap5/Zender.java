package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Vadszender ({@code guhs:vadszender}): a guh machine with a little antenna and wiggling ears that sends the vadskracht
 * of ITS net to the {@link Ontvanger}s within {@link T5Getallen#ZENDER_BEREIK} blocks (at most {@link T5Getallen#ZENDER_MAX}).
 * Together they stay one net with the same rule (DESIGN 6): the zender asks its own {@link T5Getallen#ZENDER} VK plus
 * everything its ontvangers ask, so when its net has too little, the zender stands still, its ontvangers give nothing and
 * every machine behind them stands still as well. There is no channel over any distance.
 * <p>
 * One jump only: a zender whose own net holds an ontvanger sends nothing ("wat binnenkomt, zend ik niet door"), so no chain or
 * circle of zenders can make vadskracht out of nothing.
 */
public final class Zender {
    /** The zenders that are loaded, per level (for the ontvangers' search: no block scan). */
    static final Map<ResourceKey<Level>, Set<BlockPos>> GELADEN = new ConcurrentHashMap<>();

    private Zender() {
    }

    /** The nearest loaded zender of this owner (any owner when one of the two has none) within range, that has room. */
    @Nullable
    static BlockEntityImpl zoek(ServerLevel level, BlockPos bij, @Nullable UUID eigenaar, @Nullable BlockPos huidige) {
        Set<BlockPos> alle = GELADEN.get(level.dimension());
        if (alle == null) {
            return null;
        }
        BlockEntityImpl beste = null;
        double besteAfstand = Double.MAX_VALUE;
        for (BlockPos pos : List.copyOf(alle)) {
            double d = pos.distSqr(bij);
            if (d > (double) T5Getallen.ZENDER_BEREIK * T5Getallen.ZENDER_BEREIK || d >= besteAfstand || !level.isLoaded(pos)
                    || !(level.getBlockEntity(pos) instanceof BlockEntityImpl z)) {
                continue;
            }
            if (eigenaar != null && z.eigenaar() != null && !eigenaar.equals(z.eigenaar())) {
                continue;
            }
            if (!pos.equals(huidige) && z.aantalOntvangers(level) >= T5Getallen.ZENDER_MAX) {
                continue;
            }
            beste = z;
            besteAfstand = d;
        }
        return beste;
    }

    // =====================================================================================================================
    // the block
    // =====================================================================================================================

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
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BlockEntityImpl z) {
                z.vertel(server, player);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    // the block entity
    // =====================================================================================================================

    public static class BlockEntityImpl extends MachineBlockEntity {
        /** What the ontvangers ask together (VK), as counted at the last look. */
        private int doorgeven;
        /** Its own net holds an ontvanger: it sends nothing. */
        private boolean weigert;
        /** A practice setup of the workshop (no owner, no projects). */
        boolean oefen;
        private boolean bezigMetKijken;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.ZENDER_BE.get(), pos, state, T5Getallen.ZENDER, 1);
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

        /** Does it send now: it has vadskracht and its own net holds no ontvanger. */
        public boolean zendt() {
            return heeftKracht() && !weigert;
        }

        public boolean weigert() {
            return weigert;
        }

        public int doorgeven() {
            return doorgeven;
        }

        /** The loaded ontvangers that are tuned to this zender. */
        List<Ontvanger.BlockEntityImpl> ontvangers(ServerLevel level) {
            List<Ontvanger.BlockEntityImpl> uit = new ArrayList<>();
            Set<BlockPos> alle = Ontvanger.GELADEN.get(level.dimension());
            if (alle == null) {
                return uit;
            }
            for (BlockPos pos : List.copyOf(alle)) {
                if (pos.distSqr(worldPosition) <= (double) T5Getallen.ZENDER_BEREIK * T5Getallen.ZENDER_BEREIK && level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof Ontvanger.BlockEntityImpl o && worldPosition.equals(o.zender())) {
                    uit.add(o);
                }
            }
            return uit;
        }

        int aantalOntvangers(ServerLevel level) {
            return ontvangers(level).size();
        }

        @Override
        public int vadsVraag() {
            int eigen = super.vadsVraag();
            return eigen <= 0 ? 0 : eigen + (weigert ? 0 : doorgeven);
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        /** (asked by the machine's tick every tick, also without vadskracht: the zender counts its ontvangers once per second) */
        @Override
        protected boolean isVol() {
            if (level instanceof ServerLevel server && !bezigMetKijken && Math.floorMod(server.getGameTime() + worldPosition.asLong(), 20L) == 0) {
                bezigMetKijken = true;
                try {
                    kijk(server);
                } finally {
                    bezigMetKijken = false;
                }
            }
            return weigert;
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
        protected void werk() {
            if (level instanceof ServerLevel server && Math.floorMod(server.getGameTime() + worldPosition.asLong(), 40L) == 0 && !weigert && doorgeven > 0) {
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5,
                        3, 0.15, 0.1, 0.15, 0.02);
            }
        }

        /** Once per second: counts what the ontvangers ask, checks the one-jump rule. */
        void kijk(ServerLevel level) {
            int som = 0;
            for (Ontvanger.BlockEntityImpl o : ontvangers(level)) {
                som += o.vraagt();
            }
            boolean nuWeigert = false;
            VadsNet net = VadsKracht.net(level, worldPosition);
            for (BlockPos k : net.knopen()) {
                if (!k.equals(worldPosition) && level.getBlockEntity(k) instanceof Ontvanger.BlockEntityImpl) {
                    nuWeigert = true;
                    break;
                }
            }
            if (som != doorgeven || nuWeigert != weigert) {
                doorgeven = som;
                weigert = nuWeigert;
                meld();
                sync();
            }
        }

        void vertel(ServerLevel level, Player player) {
            List<Ontvanger.BlockEntityImpl> lijst = ontvangers(level);
            player.sendOverlayMessage(weigert ? Component.translatable("block.guhs.vadszender.weigert").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("block.guhs.vadszender.stand", lijst.size(), T5Getallen.ZENDER_MAX, doorgeven,
                            Component.translatable(heeftKracht() ? "gui.guhs.einde2_trap5_machines.aan" : "gui.guhs.einde2_trap5_machines.uit")));
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(weigert ? Component.translatable("block.guhs.vadszender.weigert").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("block.guhs.vadszender.regel", doorgeven).withStyle(ChatFormatting.AQUA));
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.putInt("Doorgeven", doorgeven);
            uit.putBoolean("Weigert", weigert);
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            doorgeven = Math.max(0, in.getIntOr("Doorgeven", 0));
            weigert = in.getBooleanOr("Weigert", false);
            oefen = in.getBooleanOr("Oefen", false);
        }

        @Override
        protected void clientTick() {
            if (level != null && bezig() && level.getRandom().nextInt(12) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }
}
