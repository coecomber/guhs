package nl.juiced.guhs.feature.techmachine;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Vadsmolen's inside: slot 0 holds what is to be ground, slot 1 what came out. One item becomes its meel (or sugar,
 * bone meal, gravel...: {@link Maalrecepten}) after the recipe's ticks of work: knabbelgraan in a second, four times as
 * fast as the guh-molentje in calm weather, whatever the weather. A full sack makes the face surprised.
 */
public class VadsmolenBlockEntity extends TechBlockEntity {
    public static final int IN = 0, UIT = 1;

    public VadsmolenBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.VADSMOLEN_BE.get(), pos, state, VadsGetallen.MOLEN, 2);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.VADSMOLEN;
    }

    /** The recipe for what lies in the in slot. */
    @Nullable
    private Maalrecepten.Recept recept() {
        return vakken().getAmountAsInt(IN) > 0 ? Maalrecepten.van(vakken().getResource(IN).toStack()) : null;
    }

    @Override
    public int duur() {
        Maalrecepten.Recept r = recept();
        return r == null ? Maalrecepten.STANDAARD_TICKS : r.ticks();
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return Maalrecepten.van(wat.toStack()) != null;
    }

    /** Does what this recipe gives fit into the sack? */
    private boolean ruimte(Maalrecepten.Recept r) {
        ItemResource ligt = vakken().getResource(UIT);
        ItemStack komt = r.resultaat();
        if (ligt.isEmpty()) {
            return true;
        }
        return ligt.matches(komt) && vakken().getAmountAsInt(UIT) + komt.getCount() <= vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        Maalrecepten.Recept r = recept();
        if (r == null || !ruimte(r)) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        Maalrecepten.Recept r = recept();
        return r != null && !ruimte(r);
    }

    @Override
    protected void werk() {
        Maalrecepten.Recept r = recept();
        if (r == null || ++voortgang < r.ticks() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        ItemResource in = vakken().getResource(IN);
        ItemStack komt = r.resultaat();
        int lag = vakken().getAmountAsInt(UIT);
        vakken().set(IN, in, vakken().getAmountAsInt(IN) - 1);
        vakken().set(UIT, ItemResource.of(komt), lag + komt.getCount());
        server.sendParticles(BakkerijFeature.MEELSTOFJE.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 0.45, worldPosition.getZ() + 0.5,
                4, 0.3, 0.1, 0.3, 0.01);
        server.playSound(null, worldPosition, GuhpolderFeature.MOLENTJE_MAAL.get(), SoundSource.BLOCKS, 0.35f, 0.8f + server.getRandom().nextFloat() * 0.2f);
        beloon("gemalen");
    }

    /** (client) The sails: round and round while it grinds, a lazy turn while it has vadskracht and nothing to do. */
    @Override
    protected float tempo() {
        return bezig() ? 1f : snoet() == nl.juiced.guhs.feature.vadskracht.Snoet.SLAAPT ? 0f : 0.2f;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (vakken().getAmountAsInt(IN) == 0) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vadsmolen.leeg").withStyle(ChatFormatting.GRAY));
        }
    }
}
