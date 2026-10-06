package nl.juiced.guhs.feature.techsaus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.vadskracht.SausTank;
import nl.juiced.guhs.feature.vadskracht.Sauzen;

/**
 * The works of a {@link GrillkoolpersBlock}: two tanks ({@link SausGetallen#PERS_TANK} mB each), one for kaasfrituursaus and
 * one for water, both filled through Sausslangen (one hose net may bring both: each tank only slurps its own sauce) or from
 * buckets. {@link SausGetallen#PERS_SAUS} + {@link SausGetallen#PERS_WATER} mB are squeezed for {@link SausGetallen#PERS_TIKKEN}
 * ticks into ONE block of grillkool in the out slot ({@link #UIT}): the same exchange as in the world, where a source of
 * frituursaus touched by water turns into grillkool.
 */
public class GrillkoolpersBlockEntity extends SausMachineBlockEntity {
    public static final int UIT = 0;

    private final SausTank saus, water;
    private boolean perst;
    private int voortgang;

    public GrillkoolpersBlockEntity(BlockPos pos, BlockState state) {
        super(TechsausFeature.GRILLKOOLPERS_BE.get(), pos, state, SausGetallen.GRILLKOOLPERS, 1);
        saus = maakTank(SausGetallen.PERS_TANK, Sauzen.frituursaus());
        water = maakTank(SausGetallen.PERS_TANK, Sauzen.water());
    }

    public SausTank saus() {
        return saus;
    }

    public SausTank water() {
        return water;
    }

    public boolean perst() {
        return perst;
    }

    public int voortgang() {
        return voortgang;
    }

    private static ItemStack grillkool() {
        return new ItemStack(BarbecuetherFeature.GRILLKOOL_ITEM.get());
    }

    /** Nothing goes in as an item: the press only takes sauce. */
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
        return perst || saus.inhoud() >= SausGetallen.PERS_SAUS && water.inhoud() >= SausGetallen.PERS_WATER && !isVol();
    }

    @Override
    protected boolean isVol() {
        return ruimte(grillkool(), UIT, UIT + 1) <= 0;
    }

    @Override
    protected void werk() {
        if (level == null) {
            return;
        }
        if (!perst) {
            saus.tap(SausGetallen.PERS_SAUS, false);
            water.tap(SausGetallen.PERS_WATER, false);
            voortgang = 0;
            zetPerst(true);
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, 0.7f);
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
        if (++voortgang >= SausGetallen.PERS_TIKKEN) {
            voortgang = 0;
            if (leg(grillkool(), UIT, UIT + 1) > 0) {   // (cannot happen: the room was checked at the start)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, grillkool());
            }
            zetPerst(false);
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, 0.7f);
            beloonEigenaar(SausBeloning.GEPERST);
        }
    }

    private void zetPerst(boolean nu) {
        perst = nu;
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(GrillkoolpersBlock.PERST) && state.getValue(GrillkoolpersBlock.PERST) != nu) {
            level.setBlock(worldPosition, state.setValue(GrillkoolpersBlock.PERST, nu), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    @Override
    protected void geoogst(ServerPlayer player, ItemStack stack) {
        SausBeloning.geef(player, SausBeloning.GEPERST);
    }

    @Nullable
    @Override
    protected Component wachtOp() {
        if (perst) {
            return null;
        }
        if (saus.inhoud() < SausGetallen.PERS_SAUS) {
            return Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.frituursaus()));
        }
        return water.inhoud() < SausGetallen.PERS_WATER ? Component.translatable(SausTekst.K + "wacht.saus", SausTekst.naam(Sauzen.water())) : null;
    }

    @Override
    protected void stand(java.util.function.Consumer<Component> regels) {
        super.stand(regels);
        if (perst) {
            regels.accept(Component.translatable(SausTekst.K + "perst", voortgang * 100 / SausGetallen.PERS_TIKKEN).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putBoolean("Perst", perst);
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        perst = in.getBooleanOr("Perst", false);
        voortgang = in.getIntOr("Voortgang", 0);
    }
}
