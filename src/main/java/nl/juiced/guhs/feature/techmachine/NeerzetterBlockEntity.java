package nl.juiced.guhs.feature.techmachine;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.minecraft.world.level.block.entity.BlockEntity;
import nl.juiced.guhs.feature.techbezorg.HaltepaaltjeBlockEntity;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The Neerzetter's inside: nine slots of blocks (anything that is placed as a block: seeds and saplings too). When the
 * spot in front of its snoet is free it puts the first block it holds there, one per {@link #TIJD} ticks of work, the way
 * a player standing behind it would (a seed only where it can grow, a torch only where it can hang). A spot that is taken
 * makes the face surprised. It places nothing in a protected building, a protected area or the huisje area of another
 * player ({@link Bescherming#magWijzigen}).
 */
public class NeerzetterBlockEntity extends TechBlockEntity {
    /** Ticks of work per block. */
    public static final int TIJD = 20;

    /** The block at hand could not be placed there (a seed without farmland...): wait until something changes. */
    private boolean lukteNiet;
    private BlockState stondEr;

    public NeerzetterBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.NEERZETTER_BE.get(), pos, state, VadsGetallen.NEERZETTER, 9);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.NEERZETTER;
    }

    @Override
    public int duur() {
        return TIJD;
    }

    /** The spot in front of the snoet. */
    public BlockPos doel() {
        return worldPosition.relative(voor());
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return wat.getItem() instanceof BlockItem;
    }

    /** The first slot with a block in it, or -1. */
    private int eerste() {
        for (int i = 0; i < vakken().size(); i++) {
            if (vakken().getAmountAsInt(i) > 0 && vakken().getResource(i).getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean vrij(ServerLevel server) {
        BlockPos doel = doel();
        return server.isLoaded(doel) && !server.isOutsideBuildHeight(doel) && server.getBlockState(doel).canBeReplaced();
    }

    @Override
    protected boolean kanWerken() {
        if (!(level instanceof ServerLevel server) || eerste() < 0 || !vrij(server) || !magHier(server, doel())) {
            voortgang = 0;
            return false;
        }
        if (lukteNiet && server.getBlockState(doel()) == stondEr) {
            return false;
        }
        lukteNiet = false;
        return true;
    }

    /** Surprised: it holds a block but the spot in front of it is taken. */
    @Override
    protected boolean isVol() {
        return level instanceof ServerLevel server && eerste() >= 0 && !vrij(server);
    }

    @Override
    protected void werk() {
        if (++voortgang < TIJD || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        int vak = eerste();
        if (vak < 0) {
            return;
        }
        ItemResource soort = vakken().getResource(vak);
        ItemStack een = soort.toStack(1);
        BlockPos doel = doel();
        Direction voor = voor();
        BlockState eerst = server.getBlockState(doel);
        InteractionResult gelukt = ((BlockItem) soort.getItem()).place(new DirectionalPlaceContext(server, doel, voor, een, voor.getOpposite()));
        if (gelukt.consumesAction()) {
            vakken().set(vak, soort, vakken().getAmountAsInt(vak) - 1);
            geefDoor(server, doel);
            beloon("neergezet");
        } else {
            lukteNiet = true;
            stondEr = eerst;
        }
    }

    /**
     * A machine (or a Haltepaaltje) the Neerzetter put down belongs to whoever placed the Neerzetter. Without an owner it
     * would be refused in every huisje's home base, earn nobody an advancement, and the chore guhs of any huisje around
     * would serve it.
     */
    private void geefDoor(ServerLevel server, BlockPos doel) {
        if (eigenaar() == null) {
            return;
        }
        BlockEntity be = server.getBlockEntity(doel);
        if (be instanceof MachineBlockEntity machine && machine.eigenaar() == null) {
            machine.zetEigenaar(eigenaar());
        } else if (be instanceof HaltepaaltjeBlockEntity halte && halte.eigenaar() == null) {
            halte.zetEigenaar(eigenaar());
        }
    }

    @Override
    protected void vakkenVeranderd() {
        lukteNiet = false;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (eerste() < 0) {
            regels.accept(Component.translatable("gui.guhs.techmachine.neerzetter.leeg").withStyle(ChatFormatting.GRAY));
        } else if (level instanceof ServerLevel server && !vrij(server)) {
            regels.accept(Component.translatable("gui.guhs.techmachine.neerzetter.bezet").withStyle(ChatFormatting.GOLD));
        } else if (level instanceof ServerLevel server && !Bescherming.magWijzigen(server, doel(), eigenaar())) {
            regels.accept(Component.translatable("gui.guhs.techmachine.mag_niet").withStyle(ChatFormatting.GOLD));
        } else if (lukteNiet) {
            regels.accept(Component.translatable("gui.guhs.techmachine.neerzetter.past_niet").withStyle(ChatFormatting.GOLD));
        }
    }
}
