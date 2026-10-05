package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The pump of a {@link SauspompBlock}. While it has vadskracht and stands on a SOURCE block of one of the sauces of the
 * Guh-technologie, it lifts {@link SausGetallen#POMP_PER_TIK} mB per tick into its tank ({@link SausGetallen#POMP_TANK}) and
 * pushes that on through the hoses ({@link Slangen#duw}). The source block stays (a guh pump is polite: it never drinks a
 * lake dry, and the server world keeps its sauce sea). Full tank and nobody takes it: the face is surprised.
 * By hand an empty bucket scoops from the tank.
 */
public class SauspompBlockEntity extends SausMachineBlockEntity {
    private final SausTank tank;

    public SauspompBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.SAUSPOMP_BE.get(), pos, state, SausGetallen.POMP, 0);
        tank = maakTank(SausGetallen.POMP_TANK, FluidResource.EMPTY);
    }

    public SausTank tank() {
        return tank;
    }

    /** The sauce in the source block under the pump (null: no source of a sauce we know). */
    @Nullable
    public FluidResource bron() {
        if (level == null) {
            return null;
        }
        FluidState onder = level.getFluidState(worldPosition.below());
        if (!onder.isSource()) {
            return null;
        }
        FluidResource saus = FluidResource.of(onder.getType());
        return Sauzen.isTechniek(saus) ? saus : null;
    }

    /** The pump gives, it never takes from a hose. */
    @Override
    protected boolean slangIn() {
        return false;
    }

    @Override
    protected boolean slangUit() {
        return true;
    }

    @Override
    protected void slang(ServerLevel server) {
        if (!tank.isLeeg()) {
            Slangen.duw(server, worldPosition, sausHandler(null), tank, Math.min(SausGetallen.SLANG_PER_KEER, tank.inhoud()));
        }
    }

    @Override
    protected boolean kanWerken() {
        FluidResource bron = bron();
        return bron != null && tank.ruimte() > 0 && (tank.isLeeg() || tank.saus().equals(bron));
    }

    @Override
    protected boolean isVol() {
        return tank.ruimte() <= 0;
    }

    @Override
    protected void werk() {
        FluidResource bron = bron();
        if (bron != null && tank.vul(bron, SausGetallen.POMP_PER_TIK, false) > 0 && tank.inhoud() >= Sauzen.EMMER) {
            beloonEigenaar(SausBeloning.GEPOMPT);
        }
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        FluidResource bron = bron();
        if (bron == null) {
            return Component.translatable(SausTekst.K + "wacht.bron");
        }
        return !tank.isLeeg() && !tank.saus().equals(bron) ? Component.translatable(SausTekst.K + "wacht.andere_saus", SausTekst.naam(tank.saus())) : null;
    }
}
