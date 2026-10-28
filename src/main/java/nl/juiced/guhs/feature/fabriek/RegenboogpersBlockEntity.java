package nl.juiced.guhs.feature.fabriek;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.BankStorage;

/**
 * De Regenboogpers (1.8.0, tier 3): presses pluis and kaassaus into ANY dye, no flowers needed. The colour: a dye in the
 * voorbeeld slot (2, it stays), else the one picked with sneak + empty hand ({@link #volgendeKleur}). Kaassaus comes from a
 * bucket in slot 1 (the empty bucket goes to slot 4) or from a Sausvat, a pump or a sausbuis right next to it, into its own
 * tank of {@link FabriekGetallen#PERS_TANK} mB. One job: one pluis + {@link FabriekGetallen#PERS_SAUS} mB =
 * {@link FabriekGetallen#PERS_KLEURSTOF} dye, into slot 3 or the linked bank.
 */
public class RegenboogpersBlockEntity extends TechBlockEntity implements MetBank {
    public static final int PLUIS = 0, EMMER = 1, VOORBEELD = 2, UIT = 3, LEEG = 4;

    private DyeColor gekozen = DyeColor.WHITE;
    private int saus;
    @Nullable
    private UUID bank;

    public RegenboogpersBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.REGENBOOGPERS_BE.get(), pos, state, FabriekGetallen.REGENBOOGPERS, 5);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.REGENBOOGPERS;
    }

    @Override
    public int duur() {
        return FabriekGetallen.PERS_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return switch (vak) {
            case PLUIS -> FabriekFeature.spinbaar(wat.toStack());
            case EMMER -> wat.toStack().is(ModItems.KAAS_SAUS_BUCKET.get());
            case VOORBEELD -> Kleuren.kleur(wat.toStack()) != null;
            default -> false;
        };
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

    public int saus() {
        return saus;
    }

    /** The colour it presses now: the voorbeeld's, else the picked one. */
    public DyeColor kleur() {
        DyeColor k = vakken().getAmountAsInt(VOORBEELD) > 0 ? Kleuren.kleur(vakken().getResource(VOORBEELD).toStack()) : null;
        return k != null ? k : gekozen;
    }

    public void kies(DyeColor kleur) {
        gekozen = kleur;
        setChanged();
    }

    /** Sneak + empty hand: the next of the sixteen colours. */
    public void volgendeKleur(ServerPlayer speler) {
        kies(DyeColor.byId((gekozen.getId() + 1) % DyeColor.values().length));
        speler.sendOverlayMessage(Component.translatable("gui.guhs.fabriek.regenboogpers.kleur",
                Component.translatable("color.minecraft." + gekozen.getName())));
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }

    /** Tops the tank up: a bucket from slot 1, else a slurp from the sauce blocks next to it. */
    private void tank() {
        if (level == null) {
            return;
        }
        if (saus + Sauzen.EMMER <= FabriekGetallen.PERS_TANK && vakken().getAmountAsInt(EMMER) > 0) {
            ItemResource leeg = vakken().getResource(LEEG);
            if (leeg.isEmpty() || leeg.toStack().is(Items.BUCKET) && vakken().getAmountAsInt(LEEG) < vakken().getCapacityAsInt(LEEG, leeg)) {
                vakken().set(EMMER, vakken().getResource(EMMER), vakken().getAmountAsInt(EMMER) - 1);
                vakken().set(LEEG, ItemResource.of(new ItemStack(Items.BUCKET)), vakken().getAmountAsInt(LEEG) + 1);
                saus += Sauzen.EMMER;
                setChanged();
            }
        }
        if (saus >= FabriekGetallen.PERS_SAUS) {
            return;
        }
        FluidResource kaassaus = Sauzen.kaassaus();
        for (Direction d : Direction.values()) {
            ResourceHandler<FluidResource> h = Sauzen.van(level, worldPosition.relative(d), d.getOpposite());
            if (h != null) {
                int n = Sauzen.neem(h, kaassaus, Math.min(FabriekGetallen.PERS_SLURP, FabriekGetallen.PERS_TANK - saus), false);
                if (n > 0) {
                    saus += n;
                    setChanged();
                    return;
                }
            }
        }
    }

    private ItemStack uitkomst() {
        return new ItemStack(Kleuren.kleurstof(kleur()), FabriekGetallen.PERS_KLEURSTOF);
    }

    private boolean ruimte(ItemStack kleur) {
        BankStorage maag = MetBank.maag(level, bank);
        if (maag != null && maag.room(kleur) >= kleur.getCount()) {
            return true;
        }
        ItemResource ligt = vakken().getResource(UIT);
        return ligt.isEmpty() || ligt.matches(kleur) && vakken().getAmountAsInt(UIT) + kleur.getCount() <= vakken().getCapacityAsInt(UIT, ligt);
    }

    @Override
    protected boolean kanWerken() {
        if (vakken().getAmountAsInt(PLUIS) == 0) {
            voortgang = 0;
            return false;
        }
        tank();
        if (saus < FabriekGetallen.PERS_SAUS || !ruimte(uitkomst())) {
            voortgang = 0;
            return false;
        }
        return true;
    }

    @Override
    protected boolean isVol() {
        return vakken().getAmountAsInt(PLUIS) > 0 && saus >= FabriekGetallen.PERS_SAUS && !ruimte(uitkomst());
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        ItemStack uit = uitkomst();
        if (vakken().getAmountAsInt(PLUIS) == 0 || saus < FabriekGetallen.PERS_SAUS || !ruimte(uit)) {
            return;
        }
        vakken().set(PLUIS, vakken().getResource(PLUIS), vakken().getAmountAsInt(PLUIS) - 1);
        saus -= FabriekGetallen.PERS_SAUS;
        BankStorage maag = MetBank.maag(server, bank);
        if (maag != null && maag.room(uit) >= uit.getCount()) {
            maag.insert(uit);
            MetBank.wakker(server, bank);
        } else {
            vakken().set(UIT, ItemResource.of(uit), vakken().getAmountAsInt(UIT) + uit.getCount());
        }
        setChanged();
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                6, 0.3, 0.1, 0.3, 0.02);
        server.playSound(null, worldPosition, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.4f, 1.3f);
        FabriekFeature.beloon(this, "fabriek_regenboogpers");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.fabriek.regenboogpers.kleur", Component.translatable("color.minecraft." + kleur().getName()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        regels.accept(Component.translatable("gui.guhs.fabriek.regenboogpers.saus", saus, FabriekGetallen.PERS_TANK).withStyle(ChatFormatting.GOLD));
        if (bank != null) {
            regels.accept(Component.translatable(MetBank.maag(level, bank) != null ? "gui.guhs.fabriek.bouwblokken.bank" : "gui.guhs.fabriek.bouwblokken.bank_weg")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (saus < FabriekGetallen.PERS_SAUS) {
            regels.accept(Component.translatable("gui.guhs.fabriek.regenboogpers.geen_saus").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Kleur", gekozen.getId());
        uit.putInt("Saus", saus);
        uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
    }

    @Override
    protected void laden(ValueInput in) {
        gekozen = DyeColor.byId(in.getIntOr("Kleur", 0));
        saus = in.getIntOr("Saus", 0);
        bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
    }
}
