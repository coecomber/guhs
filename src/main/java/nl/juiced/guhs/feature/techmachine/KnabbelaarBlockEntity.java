package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The Knabbelaar's inside: it gnaws away the block in front of its snoet in {@link #TIJD} ticks of work (two seconds,
 * whatever the block), with the cracks of a block being mined and a crunch at every bite. It bites like an iron pickaxe:
 * everything such a pickaxe gets through, with the drops that pickaxe would give ({@link #magKnabbelen}). What it never
 * touches: anything with a block entity (chests, machines, banks), any vadskracht block, the blocks of
 * {@link TechmachineFeature#KNABBELT_NIET}, fluids, and anything in a protected building, a protected area or the huisje
 * area of another player ({@link Bescherming#magWijzigen}). The drops go into its nine slots; when they do not fit the
 * block stays and the face is surprised.
 */
public class KnabbelaarBlockEntity extends TechBlockEntity {
    /** Ticks of work per block. */
    public static final int TIJD = 40;

    /** The block it is gnawing on (a different block in front starts over). */
    @Nullable
    private BlockState hap;
    private boolean vol;

    public KnabbelaarBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.KNABBELAAR_BE.get(), pos, state, VadsGetallen.KNABBELAAR, 9);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.KNABBELAAR;
    }

    @Override
    public int duur() {
        return TIJD;
    }

    /** The spot in front of the snoet. */
    public BlockPos doel() {
        return worldPosition.relative(voor());
    }

    /**
     * Would an iron pickaxe get through this block, and is it something a machine may eat at all? (Not whether it may be
     * changed HERE: that is {@link Bescherming#magWijzigen}.)
     */
    public static boolean magKnabbelen(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        if (state.hasBlockEntity() || state.is(VadsKracht.TOON) || state.is(TechmachineFeature.KNABBELT_NIET)) {
            return false;   // never containers, never machines
        }
        return !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
    }

    /** May the block in front be gnawed right now? */
    private boolean eetbaar(ServerLevel server) {
        BlockPos doel = doel();
        return server.isLoaded(doel) && magKnabbelen(server, doel, server.getBlockState(doel)) && magHier(server, doel);
    }

    @Override
    protected boolean kanWerken() {
        if (!(level instanceof ServerLevel server) || !eetbaar(server)) {
            laatLos();
            return false;
        }
        return !vol;
    }

    @Override
    protected boolean isVol() {
        return vol;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockPos doel = doel();
        BlockState state = server.getBlockState(doel);
        if (state != hap) {
            hap = state;
            voortgang = 0;
        }
        voortgang++;
        server.destroyBlockProgress(breekId(), doel, Math.min(9, voortgang * 10 / TIJD));
        if (voortgang % 5 == 0) {
            // a bite: crumbs of the block fly at the snoet
            server.playSound(null, worldPosition, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.45f, 0.8f + server.getRandom().nextFloat() * 0.4f);
            BlockPos p = worldPosition;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + 0.5 + voor().getStepX() * 0.6, p.getY() + 0.35,
                    p.getZ() + 0.5 + voor().getStepZ() * 0.6, 4, 0.12, 0.1, 0.12, 0.02);
        }
        if (voortgang < TIJD) {
            return;
        }
        List<ItemStack> buit = new ArrayList<>();
        // (the tool it bites like: an iron pickaxe)
        for (ItemStack d : Block.getDrops(state, server, doel, null, null, new ItemStack(Items.IRON_PICKAXE))) {
            if (!d.isEmpty()) {
                buit.add(d.copy());
            }
        }
        if (!pastAlles(buit, 0, 8)) {
            vol = true;                 // (the block stays, almost gnawed through; on as soon as there is room)
            voortgang = TIJD - 1;
            return;
        }
        server.destroyBlockProgress(breekId(), doel, -1);
        server.levelEvent(2001, doel, Block.getId(state));   // the sound and the bits of a broken block
        server.setBlock(doel, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        server.gameEvent(GameEvent.BLOCK_DESTROY, doel, GameEvent.Context.of(state));
        stopAlles(buit, 0, 8);
        hap = null;
        voortgang = 0;
        beloon("geknabbeld");
    }

    /** Stops gnawing: the cracks in the block go away. */
    private void laatLos() {
        if (hap != null || voortgang > 0) {
            if (level instanceof ServerLevel server) {
                server.destroyBlockProgress(breekId(), doel(), -1);
            }
            hap = null;
            voortgang = 0;
        }
    }

    /** The breaker id of the cracks this machine makes (players have positive ids). */
    private int breekId() {
        return -1 - (worldPosition.hashCode() & 0xFFFFFF);
    }

    @Override
    public void vadsStroom(boolean aan) {
        super.vadsStroom(aan);
        if (!aan) {
            laatLos();
        }
    }

    @Override
    protected void vakkenVeranderd() {
        vol = false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        laatLos();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (vol) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (level instanceof ServerLevel server) {
            BlockPos doel = doel();
            BlockState state = server.getBlockState(doel);
            if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                if (!magKnabbelen(server, doel, state)) {
                    regels.accept(Component.translatable("gui.guhs.techmachine.knabbelaar.lust_niet", state.getBlock().getName()).withStyle(ChatFormatting.GOLD));
                } else if (!Bescherming.magWijzigen(server, doel, eigenaar())) {
                    regels.accept(Component.translatable("gui.guhs.techmachine.mag_niet").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }
}
