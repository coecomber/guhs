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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;

/**
 * De Spinmachine (1.8.0): a spinning wheel with a guh face. Slot 0 holds pluis ({@code #guhs:spinbaar}: the Knuffelrecycler's
 * pluis and the wolkenpluis of a wolkenschaapje), slot 1 the wool it spun: {@link FabriekGetallen#PLUIS_PER_WOL} pluis is one
 * white wool, a job of {@link FabriekGetallen#SPIN_TICKS} ticks on vadskracht.
 */
public class SpinmachineBlockEntity extends TechBlockEntity {
    public static final int IN = 0, UIT = 1;

    public SpinmachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.SPINMACHINE_BE.get(), pos, state, FabriekGetallen.SPINMACHINE, 2);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.SPINMACHINE;
    }

    @Override
    public int duur() {
        return FabriekGetallen.SPIN_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == IN && FabriekFeature.spinbaar(wat.toStack());
    }

    private boolean ruimte() {
        ItemResource ligt = vakken().getResource(UIT);
        return ligt.isEmpty() || ligt.toStack().is(Items.WHITE_WOOL) && vakken().getAmountAsInt(UIT) < vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        if (vakken().getAmountAsInt(IN) < FabriekGetallen.PLUIS_PER_WOL || !ruimte()) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        return vakken().getAmountAsInt(IN) >= FabriekGetallen.PLUIS_PER_WOL && !ruimte();
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        ItemResource in = vakken().getResource(IN);
        vakken().set(IN, in, vakken().getAmountAsInt(IN) - FabriekGetallen.PLUIS_PER_WOL);
        vakken().set(UIT, ItemResource.of(new ItemStack(Items.WHITE_WOOL)), vakken().getAmountAsInt(UIT) + 1);
        server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                3, 0.2, 0.05, 0.2, 0.01);
        server.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.4f, 1.4f);
        FabriekFeature.beloon(this, "fabriek_spin");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (vakken().getAmountAsInt(IN) < FabriekGetallen.PLUIS_PER_WOL) {
            regels.accept(Component.translatable("gui.guhs.fabriek.spinmachine.leeg").withStyle(ChatFormatting.GRAY));
        }
    }
}
