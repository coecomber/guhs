package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The works of a {@link FrituurautomaatBlock}: the frying pan that fries by itself. It fries in kaasfrituursaus (the frying
 * pan wants Mika's vet) on vadskracht: every {@link SausGetallen#FRITUUR_TIKKEN} ticks one snack from slot {@link #IN} comes
 * out fried ({@link Frituur}) in the slots {@link #UIT_VAN}..{@link #UIT_TOT} and {@link SausGetallen#FRITUUR_SAUS} mB of sauce
 * is used up. The tank ({@link SausGetallen#FRITUUR_TANK} mB) fills through a Sausslang or from a bucket.
 */
public class FrituurautomaatBlockEntity extends SausMachineBlockEntity {
    public static final int IN = 0, UIT_VAN = 1, UIT_TOT = 3;

    private final SausTank tank;
    private int voortgang;
    /** (not saved) so the sizzle does not sound for every single snack. */
    private int gesis;

    public FrituurautomaatBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.FRITUURAUTOMAAT_BE.get(), pos, state, SausGetallen.FRITUURAUTOMAAT, UIT_TOT);
        tank = maakTank(SausGetallen.FRITUUR_TANK, Sauzen.frituursaus());
    }

    public SausTank tank() {
        return tank;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return vak == IN;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return vak >= UIT_VAN;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == IN && !Frituur.resultaat(wat.toStack()).isEmpty();
    }

    /** What the snack that lies ready becomes (EMPTY: nothing lies ready). */
    private ItemStack straks() {
        return vakken().getAmountAsInt(IN) > 0 ? Frituur.resultaat(vakken().getResource(IN).toStack()) : ItemStack.EMPTY;
    }

    @Override
    protected boolean kanWerken() {
        ItemStack straks = straks();
        return !straks.isEmpty() && tank.inhoud() >= SausGetallen.FRITUUR_SAUS && ruimte(straks, UIT_VAN, UIT_TOT) > 0;
    }

    @Override
    protected boolean isVol() {
        ItemStack straks = straks();
        return straks.isEmpty() ? propvol(UIT_VAN, UIT_TOT) : ruimte(straks, UIT_VAN, UIT_TOT) <= 0;
    }

    @Override
    protected void werk() {
        if (++voortgang < SausGetallen.FRITUUR_TIKKEN || level == null) {
            return;
        }
        voortgang = 0;
        ItemStack klaar = straks();
        tank.tap(SausGetallen.FRITUUR_SAUS, false);
        pak(IN, 1);
        leg(klaar, UIT_VAN, UIT_TOT);
        if (gesis++ % 8 == 0) {
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.35f, 1.7f);
        }
        beloonEigenaar(SausBeloning.GEFRITUURD);
    }

    @Override
    protected void geoogst(ServerPlayer player, ItemStack stack) {
        SausBeloning.geef(player, SausBeloning.GEFRITUURD);
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        if (straks().isEmpty()) {
            return Component.translatable(SausTekst.K + "wacht.snack");
        }
        return tank.inhoud() < SausGetallen.FRITUUR_SAUS ? Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.frituursaus())) : null;
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        voortgang = in.getIntOr("Voortgang", 0);
    }
}
