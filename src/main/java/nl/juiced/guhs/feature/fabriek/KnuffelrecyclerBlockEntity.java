package nl.juiced.guhs.feature.fabriek;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;

/**
 * De Knuffelrecycler (1.8.0): shreds what nobody plays with any more into pluis ({@link FabriekFeature#pluisVoor}: wool,
 * carpets, knuffels, guh clothes, cushions). Slot 0 in, slot 1 the pluis.
 */
public class KnuffelrecyclerBlockEntity extends TechBlockEntity {
    public static final int IN = 0, UIT = 1;

    public KnuffelrecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.KNUFFELRECYCLER_BE.get(), pos, state, FabriekGetallen.KNUFFELRECYCLER, 2);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.KNUFFELRECYCLER;
    }

    @Override
    public int duur() {
        return FabriekGetallen.RECYCLE_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == IN && FabriekFeature.pluisVoor(wat.toStack()) > 0;
    }

    private int pluis() {
        return vakken().getAmountAsInt(IN) > 0 ? FabriekFeature.pluisVoor(vakken().getResource(IN).toStack()) : 0;
    }

    private boolean ruimte(int n) {
        ItemResource ligt = vakken().getResource(UIT);
        ItemStack pluis = new ItemStack(FabriekFeature.PLUIS.get());
        return ligt.isEmpty() || ligt.matches(pluis) && vakken().getAmountAsInt(UIT) + n <= vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        int n = pluis();
        if (n == 0 || !ruimte(n)) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        int n = pluis();
        return n > 0 && !ruimte(n);
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        int n = pluis();
        if (n == 0 || !ruimte(n)) {
            return;
        }
        vakken().set(IN, vakken().getResource(IN), vakken().getAmountAsInt(IN) - 1);
        vakken().set(UIT, ItemResource.of(new ItemStack(FabriekFeature.PLUIS.get())), vakken().getAmountAsInt(UIT) + n);
        server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                5, 0.25, 0.1, 0.25, 0.03);
        server.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.4f, 0.8f);
        FabriekFeature.beloon(this, "fabriek_recycler");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (pluis() == 0) {
            regels.accept(Component.translatable("gui.guhs.fabriek.knuffelrecycler.leeg").withStyle(ChatFormatting.GRAY));
        }
    }
}
