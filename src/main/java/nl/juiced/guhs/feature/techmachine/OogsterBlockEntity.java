package nl.juiced.guhs.feature.techmachine;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The Oogster's inside: it looks over the field in front of its snoet ({@link #BREED} wide, {@link #DIEP} deep, from one
 * block below its own height to two above), a few spots per tick, and when a plant is ripe ({@link Oogst}) it cuts it in
 * {@link #TIJD} ticks. The harvest goes into its nine slots (pipes and guhs take it out); a harvest that does not fit is
 * not cut: then the face is surprised until there is room. It leaves protected buildings and other players' huisjes alone
 * ({@link Bescherming#magWijzigen}).
 */
public class OogsterBlockEntity extends TechBlockEntity {
    public static final int BREED = 5, DIEP = 5, ONDER = -1, BOVEN = 2;
    /** Ticks of work per plant. */
    public static final int TIJD = 20;
    /** How many spots of the field it looks at per tick while it has nothing to cut. */
    private static final int KIJK = 6;
    private static final int PLEKKEN = BREED * DIEP * (BOVEN - ONDER + 1);

    /** Which spot of the field is looked at next. */
    private int wijzer;
    /** The ripe plant it is cutting now. */
    @Nullable
    private BlockPos doel;
    /** The last harvest did not fit. */
    private boolean vol;

    public OogsterBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.OOGSTER_BE.get(), pos, state, VadsGetallen.OOGSTER, 9);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.OOGSTER;
    }

    @Override
    public int duur() {
        return TIJD;
    }

    /** The spot with this number of the field: rows from the machine outwards, each from left to right, bottom to top. */
    public BlockPos plek(int nr) {
        int hoogtes = BOVEN - ONDER + 1;
        int kolom = nr / hoogtes, dy = ONDER + nr % hoogtes;
        Direction voor = voor();
        return worldPosition.relative(voor, 1 + kolom / BREED).relative(voor.getClockWise(), kolom % BREED - BREED / 2).above(dy);
    }

    private boolean mag(ServerLevel server, BlockPos plek) {
        return magHier(server, plek);
    }

    @Override
    protected boolean kanWerken() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (doel != null && (!Oogst.isRijp(server, doel) || !mag(server, doel))) {
            doel = null;
            voortgang = 0;
        }
        for (int i = 0; doel == null && i < KIJK; i++) {
            BlockPos plek = plek(wijzer);
            wijzer = (wijzer + 1) % PLEKKEN;
            if (server.isLoaded(plek) && Oogst.isRijp(server, plek) && mag(server, plek)) {
                doel = plek;
                vol = false;
            }
        }
        return doel != null && !vol;
    }

    @Override
    protected boolean isVol() {
        return vol;
    }

    @Override
    protected void werk() {
        if (++voortgang < TIJD || !(level instanceof ServerLevel server) || doel == null) {
            return;
        }
        voortgang = 0;
        Oogst.Pluk pluk = Oogst.bekijk(server, doel);
        if (pluk == null || (!pluk.pos().equals(doel) && !Bescherming.magWijzigen(server, pluk.pos(), eigenaar()))) {
            doel = null;   // (nothing ripe after all, or the top of the stalk stands where it may not cut)
            return;
        }
        if (!pastAlles(pluk.oogst(), 0, 8)) {
            vol = true;   // (the plant stays; tried again as soon as something is taken out)
            return;
        }
        if (Oogst.doe(server, pluk)) {
            stopAlles(pluk.oogst(), 0, 8);
            beloon("geoogst");
        }
        doel = null;
    }

    @Override
    protected void vakkenVeranderd() {
        vol = false;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.techmachine.oogster.veld", BREED, DIEP).withStyle(ChatFormatting.GRAY));
        if (vol) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        }
    }
}
