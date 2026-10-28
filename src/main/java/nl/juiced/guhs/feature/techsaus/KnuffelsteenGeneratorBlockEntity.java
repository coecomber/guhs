package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * 1.8.0 (vadskracht): the works of a {@link KnuffelsteenGeneratorBlock}. Kaassaus and water meet, as lava and water do in a
 * cobblestone generator, but softer: every {@link SausGetallen#GEN_TIKKEN} ticks on vadskracht {@link SausGetallen#GEN_SAUS} mB
 * kaassaus + {@link SausGetallen#GEN_WATER} mB water become one knuffelsteen. Water also comes from a water source block right
 * next to it (it is never used up: the generator only "borrows" its cold). The knuffelsteen goes into the out slot
 * ({@link #UIT}: knabbelbuizen, the Opzuiger and hoppers take it from there) and straight into a chest, a Bank Guh or any
 * other block with an inventory that touches it.
 */
public class KnuffelsteenGeneratorBlockEntity extends SausMachineBlockEntity {
    public static final int UIT = 0;

    private final SausTank saus, water;
    private int voortgang;
    private long gemaakt;

    public KnuffelsteenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.KNUFFELSTEEN_GENERATOR_BE.get(), pos, state, VadsGetallen.KNUFFELSTEEN_GENERATOR, 1);
        saus = maakTank(SausGetallen.GEN_TANK, Sauzen.kaassaus());
        water = maakTank(SausGetallen.GEN_TANK, Sauzen.water());
    }

    public SausTank saus() {
        return saus;
    }

    public SausTank water() {
        return water;
    }

    /** How many knuffelstenen it made since it was placed (saved). */
    public long gemaakt() {
        return gemaakt;
    }

    private static ItemStack knuffelsteen() {
        return new ItemStack(KnuffeldalFeature.KNUFFELSTEEN.get());
    }

    /** A water source block next to it (it borrows its cold: the water stays). */
    private boolean waterNaast() {
        if (level == null) {
            return false;
        }
        for (Direction d : Direction.values()) {
            var f = level.getFluidState(worldPosition.relative(d));
            if (f.isSource() && f.is(net.minecraft.tags.FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean heeftWater() {
        return water.inhoud() >= SausGetallen.GEN_WATER || waterNaast();
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return true;
    }

    @Override
    protected boolean kanWerken() {
        return saus.inhoud() >= SausGetallen.GEN_SAUS && heeftWater() && !isVol();
    }

    @Override
    protected boolean isVol() {
        return ruimte(knuffelsteen(), UIT, UIT + 1) <= 0;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (++voortgang < SausGetallen.GEN_TIKKEN) {
            return;
        }
        voortgang = 0;
        saus.tap(SausGetallen.GEN_SAUS, false);
        if (!waterNaast()) {
            water.tap(SausGetallen.GEN_WATER, false);
        }
        leg(knuffelsteen(), UIT, UIT + 1);
        gemaakt++;
        geefAf(server);
        if (gemaakt % 4 == 0) {
            server.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.4f, 1.3f);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    2, 0.2, 0.05, 0.2, 0.01);
        }
        setChanged();
    }

    /** What is in the out slot goes straight into a chest / Bank Guh / anything with an inventory that touches it. */
    private void geefAf(ServerLevel server) {
        for (Direction d : Direction.values()) {
            ItemStack klaar = vakken().getResource(UIT).toStack(vakken().getAmountAsInt(UIT));
            if (klaar.isEmpty()) {
                return;
            }
            BlockPos buur = worldPosition.relative(d);
            if (server.getBlockEntity(buur) instanceof KnuffelsteenGeneratorBlockEntity) {
                continue;
            }
            ResourceHandler<ItemResource> in = server.getCapability(Capabilities.Item.BLOCK, buur, d.getOpposite());
            if (in == null) {
                continue;
            }
            ItemStack rest = Kisten.stop(in, klaar.copy());
            int weg = klaar.getCount() - rest.getCount();
            if (weg > 0) {
                Kisten.neem(vakken(), s -> true, weg);
            }
        }
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        if (saus.inhoud() < SausGetallen.GEN_SAUS) {
            return Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.kaassaus()));
        }
        return heeftWater() ? null : Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.water()));
    }

    @Override
    protected void stand(java.util.function.Consumer<Component> regels) {
        super.stand(regels);
        if (waterNaast()) {
            regels.accept(Component.translatable("block.guhs.knuffelsteen_generator.water_naast").withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        regels.accept(Component.translatable("block.guhs.knuffelsteen_generator.gemaakt", gemaakt).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putInt("Voortgang", voortgang);
        uit.putLong("Gemaakt", gemaakt);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        voortgang = in.getIntOr("Voortgang", 0);
        gemaakt = in.getLongOr("Gemaakt", 0L);
    }
}
