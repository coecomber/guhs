package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The works of a {@link BrouwautomaatBlock}. One pan, exactly as in the Guhbrouwketel you stir by hand: a bucket of kaassaus
 * ({@link SausGetallen#BROUW_SAUS}) + ONE ingredient (the item tags {@code guhs:brouwsel/<id>}, {@link Brouwsel}) + three
 * glass bottles become three Guhdrankjes after {@link SausGetallen#BROUW_TIKKEN} ticks. The fire is vadskracht: no
 * grillspiespoeder. It only starts a pan when everything for it is there AND the three drankjes will fit.
 * <p>
 * Slots: {@link #INGREDIENT} and {@link #FLESJES} in (pipes, hoppers, chore guhs, a hand), {@link #UIT_VAN}..{@link #UIT_TOT}
 * out. The tank ({@link SausGetallen#BROUW_TANK} mB of kaassaus) fills through a Sausslang or from a bucket.
 */
public class BrouwautomaatBlockEntity extends SausMachineBlockEntity {
    public static final int INGREDIENT = 0, FLESJES = 1, UIT_VAN = 2, UIT_TOT = 8;

    private final SausTank tank;
    /** What bubbles now (null: nothing). */
    @Nullable
    private Brouwsel brouwsel;
    private int voortgang;

    public BrouwautomaatBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.BROUWAUTOMAAT_BE.get(), pos, state, SausGetallen.BROUWAUTOMAAT, UIT_TOT);
        tank = maakTank(SausGetallen.BROUW_TANK, Sauzen.kaassaus());
    }

    public SausTank tank() {
        return tank;
    }

    /** What bubbles in the pan right now (synced: the bubbles have its colour); null when it does not brew. */
    @Nullable
    public Brouwsel brouwsel() {
        return brouwsel;
    }

    public int voortgang() {
        return voortgang;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return vak < UIT_VAN;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return vak >= UIT_VAN;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == INGREDIENT ? Brouwsel.forIngredient(wat.toStack()) != null : vak == FLESJES && wat.is(Items.GLASS_BOTTLE);
    }

    /** What the ingredient that lies ready would brew (null: there is none). */
    @Nullable
    private Brouwsel volgende() {
        return vakken().getAmountAsInt(INGREDIENT) > 0 ? Brouwsel.forIngredient(vakken().getResource(INGREDIENT).toStack()) : null;
    }

    private boolean kanBeginnen() {
        Brouwsel b = volgende();
        return b != null && tank.inhoud() >= SausGetallen.BROUW_SAUS && vakken().getAmountAsInt(FLESJES) >= SausGetallen.BROUW_FLESJES
                && ruimte(b.drankje(), UIT_VAN, UIT_TOT) >= SausGetallen.BROUW_FLESJES;
    }

    @Override
    protected boolean kanWerken() {
        return brouwsel != null || kanBeginnen();
    }

    /** The next pan of drankjes would not fit (or, with nothing waiting, every out slot is crammed). */
    @Override
    protected boolean isVol() {
        Brouwsel b = brouwsel != null ? brouwsel : volgende();
        return b == null ? propvol(UIT_VAN, UIT_TOT) : ruimte(b.drankje(), UIT_VAN, UIT_TOT) < SausGetallen.BROUW_FLESJES;
    }

    @Override
    protected void werk() {
        if (level == null) {
            return;
        }
        if (brouwsel == null) {
            // a new pan: the sauce, one ingredient and the bottles go in now
            brouwsel = volgende();
            voortgang = 0;
            tank.tap(SausGetallen.BROUW_SAUS, false);
            pak(INGREDIENT, 1);
            pak(FLESJES, SausGetallen.BROUW_FLESJES);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.8f);
            sync();
        }
        if (++voortgang >= SausGetallen.BROUW_TIKKEN) {
            ItemStack drankjes = brouwsel.drankje().copyWithCount(SausGetallen.BROUW_FLESJES);
            int rest = leg(drankjes, UIT_VAN, UIT_TOT);
            if (rest > 0) {   // (cannot happen: the room was checked and nothing but this machine fills the out slots)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                        drankjes.copyWithCount(rest));
            }
            brouwsel = null;
            voortgang = 0;
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.4f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                        6, 0.3, 0.2, 0.3, 0.0);
            }
            beloonEigenaar(SausBeloning.GEBROUWEN);
            sync();
        }
    }

    /** Taking Guhdrankjes out by hand counts as brewing them (the same advancement as the Guhbrouwketel gives). */
    @Override
    protected void geoogst(ServerPlayer player, ItemStack stack) {
        GuhAdvancements.grant(player, "guhdrankje_gebrouwen");
        SausBeloning.geef(player, SausBeloning.GEBROUWEN);
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        if (brouwsel != null) {
            return null;
        }
        if (volgende() == null) {
            return Component.translatable(SausTekst.K + "wacht.ingredient");
        }
        if (tank.inhoud() < SausGetallen.BROUW_SAUS) {
            return Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.kaassaus()));
        }
        return vakken().getAmountAsInt(FLESJES) < SausGetallen.BROUW_FLESJES ? Component.translatable(SausTekst.K + "wacht.flesjes") : null;
    }

    @Override
    protected void stand(java.util.function.Consumer<Component> regels) {
        super.stand(regels);
        if (brouwsel != null) {
            regels.accept(Component.translatable(SausTekst.K + "brouwt", Component.translatable("quest.guhs.guhbrouwketel.brouwsel." + brouwsel.id()),
                    voortgang * 100 / SausGetallen.BROUW_TIKKEN).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putString("Brouwsel", brouwsel == null ? "" : brouwsel.id());
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        String id = in.getStringOr("Brouwsel", "");
        brouwsel = null;
        for (Brouwsel b : Brouwsel.values()) {
            if (b != Brouwsel.BOUILLON && b.id().equals(id)) {
                brouwsel = b;
            }
        }
        voortgang = in.getIntOr("Voortgang", 0);
    }
}
