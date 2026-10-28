package nl.juiced.guhs.feature.fabriek;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;
import nl.juiced.guhs.storage.BankStorage;

/**
 * De Kleurmolen (1.8.0, tier 2): a little millstone with a guh face that grinds flowers into dye, with a bonus: what the
 * flower's own recipe gives + {@link FabriekGetallen#MOLEN_BONUS} + maybe one more ({@link Kleuren}: all sixteen colours).
 * Slot 0 the flowers, slot 1 the dye; with a Banksleutel link the dye goes straight into the bank.
 */
public class KleurmolenBlockEntity extends TechBlockEntity implements MetBank {
    public static final int IN = 0, UIT = 1;

    @Nullable
    private UUID bank;

    public KleurmolenBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.KLEURMOLEN_BE.get(), pos, state, FabriekGetallen.KLEURMOLEN, 2);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.KLEURMOLEN;
    }

    @Override
    public int duur() {
        return FabriekGetallen.MOLEN_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return vak == IN && Kleuren.maalbaar(wat.toStack());
    }

    @Nullable
    @Override
    public UUID bank() {
        return bank;
    }

    @Override
    public void koppelBank(@Nullable UUID id) {
        bank = id;
        setChanged();
    }

    private ItemStack kleurstof() {
        return vakken().getAmountAsInt(IN) > 0 && level instanceof ServerLevel server
                ? Kleuren.van(server, vakken().getResource(IN).toStack()) : ItemStack.EMPTY;
    }

    /** Fits the most this flower can give (base + bonus + 1) somewhere? */
    private boolean ruimte(ItemStack kleur) {
        int max = kleur.getCount() + FabriekGetallen.MOLEN_BONUS + 1;
        BankStorage maag = MetBank.maag(level, bank);
        if (maag != null && maag.room(kleur) >= max) {
            return true;
        }
        ItemResource ligt = vakken().getResource(UIT);
        return ligt.isEmpty() || ligt.matches(kleur) && vakken().getAmountAsInt(UIT) + max <= vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        ItemStack k = kleurstof();
        if (k.isEmpty() || !ruimte(k)) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        ItemStack k = kleurstof();
        return !k.isEmpty() && !ruimte(k);
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        ItemStack k = kleurstof();
        if (k.isEmpty() || !ruimte(k)) {
            return;
        }
        ItemStack uit = k.copyWithCount(k.getCount() + FabriekGetallen.MOLEN_BONUS + server.getRandom().nextInt(2));
        vakken().set(IN, vakken().getResource(IN), vakken().getAmountAsInt(IN) - 1);
        BankStorage maag = MetBank.maag(server, bank);
        if (maag != null && maag.room(uit) >= uit.getCount()) {
            maag.insert(uit);
            MetBank.wakker(server, bank);
        } else {
            vakken().set(UIT, ItemResource.of(uit), vakken().getAmountAsInt(UIT) + uit.getCount());
        }
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                3, 0.25, 0.1, 0.25, 0.02);
        server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25f, 1.5f);
        FabriekFeature.beloon(this, "fabriek_kleurmolen");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (bank != null) {
            regels.accept(Component.translatable(MetBank.maag(level, bank) != null ? "gui.guhs.fabriek.bouwblokken.bank" : "gui.guhs.fabriek.bouwblokken.bank_weg")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (vakken().getAmountAsInt(IN) == 0) {
            regels.accept(Component.translatable("gui.guhs.fabriek.kleurmolen.leeg").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
    }

    @Override
    protected void laden(ValueInput in) {
        bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
    }
}
