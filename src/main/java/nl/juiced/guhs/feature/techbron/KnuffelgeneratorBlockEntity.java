package nl.juiced.guhs.feature.techbron;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Knuffelgenerator's source: {@link VadsGetallen#KNUFFEL_PER_GUH} VK for every tamed guh that lies on the cushion, at
 * most {@link VadsGetallen#KNUFFEL_MAX_GUHS} guhs. Free tamed guhs within {@link TechbronGetallen#BEREIK} blocks come and
 * lie down by themselves ({@link GuhTrek}: curled up asleep, the SLAPEN emote). Lying on the cushion is what makes a guh
 * happy: it is "blij" ({@link Band#maakBlij}) for as long as it lies there and a minute after, so a guh that comes straight
 * from the cushion runs extra hard in a Guhrad.
 */
public class KnuffelgeneratorBlockEntity extends BronBlockEntity {
    /** How far the spots are apart: a little more than a guh is wide (0.9), so guhs that lie on their spots do not push each other. */
    private static final double TUSSEN = 0.95;
    /**
     * The eight spots, as (to the right, to the back) of the middle of the cushion seen from the front: a grid of 3 x 3
     * without the back middle (where the cushion's ears stand). The first guh lies in the middle, the next ones beside it,
     * then the back corners and the front row.
     */
    private static final double[][] PLEKKEN = {{0, 0}, {-TUSSEN, 0}, {TUSSEN, 0}, {-TUSSEN, TUSSEN}, {TUSSEN, TUSSEN}, {0, -TUSSEN},
            {-TUSSEN, -TUSSEN}, {TUSSEN, -TUSSEN}};

    private final GuhTrek.Groep groep = new GuhTrek.Groep(VadsGetallen.KNUFFEL_MAX_GUHS, Emote.SLAPEN);
    private int guhs;

    public KnuffelgeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(TechbronFeature.KNUFFELGENERATOR_BE.get(), pos, state);
    }

    /** How many guhs lie on the cushion right now. */
    public int guhs() {
        return guhs;
    }

    /** The box a guh must be in to lie "on the cushion": the cushion, a little around it, and the air above it. */
    public AABB zone() {
        return zone(0.5);
    }

    /** The eight spots on the cushion (the first is its middle). */
    public List<Vec3> plekken() {
        Direction facing = getBlockState().getValue(BronBlock.FACING);
        Direction rechts = facing.getClockWise(), achter = facing.getOpposite();
        AABB vloer = vloer();
        Vec3 midden = new Vec3((vloer.minX + vloer.maxX) / 2, vloer.minY + KnuffelgeneratorBlock.HOOGTE / 16.0, (vloer.minZ + vloer.maxZ) / 2);
        List<Vec3> uit = new ArrayList<>(PLEKKEN.length);
        for (double[] p : PLEKKEN) {
            uit.add(midden.add(rechts.getStepX() * p[0] + achter.getStepX() * p[1], 0, rechts.getStepZ() * p[0] + achter.getStepZ() * p[1]));
        }
        return uit;
    }

    // --- vadskracht ---

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.KNUFFELGENERATOR;
    }

    @Override
    public int vadsAanbod() {
        return Math.min(guhs, VadsGetallen.KNUFFEL_MAX_GUHS) * VadsGetallen.KNUFFEL_PER_GUH;
    }

    @Override
    protected Snoet gezicht() {
        return guhs <= 0 ? Snoet.SLAAPT : guhs >= VadsGetallen.KNUFFEL_MAX_GUHS ? Snoet.VOL : Snoet.WERKT;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (guhs <= 0) {
            regels.accept(Component.translatable("gui.guhs.techbron.knuffel.leeg", TechbronGetallen.BEREIK).withStyle(ChatFormatting.GRAY));
        } else {
            regels.accept(Component.translatable("gui.guhs.techbron.knuffel.guhs", guhs, VadsGetallen.KNUFFEL_MAX_GUHS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- the guhs ---

    @Override
    protected void tik(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), TechbronGetallen.KIJK) != 0) {
            return;
        }
        kijk(level);
    }

    /** One look at the guhs around the cushion (once per second; tests call it to skip the wait). */
    public void kijk(ServerLevel level) {
        int nu = groep.kijk(level, worldPosition, zone(), zone(0), plekken(), Float.NaN);
        for (GuhEntity guh : groep.bezig()) {
            Band.maakBlij(guh, TechbronGetallen.BLIJ_NA_KNUFFEL);
        }
        if (nu > 0) {
            beloon("knuffel");
        }
        if (nu != guhs) {
            guhs = nu;
            setChanged();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            groep.laatLos(server, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        guhs = Math.max(0, in.getIntOr("Guhs", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        uit.putInt("Guhs", guhs);
    }
}
