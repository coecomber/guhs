package nl.juiced.guhs.feature.techklus;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.KlusBeloning;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;

/**
 * Machines bijvullen & leeghalen (bbq2): the resident keeps the guh machines of the home base going. It empties their
 * out slots (iron bars from the Guh Oven, Guhdrankjes from the Brouwautomaat, whatever the Knutselmachine made, the
 * Oogster's harvest...) and brings that to the Bank Guh / the Hapluikje / the chest like any other chore output; and it
 * carries what a machine was shown ({@link Klusmachines}) from the huisje's stock to the machine: up to
 * {@link #STAPELS} stacks per trip, and on the same trip it takes along what lies ready.
 * What does not fit after all goes back into the stock. Guhs only; a machine somebody else placed is left alone.
 * The one chore that is switched OFF for every resident until the owner switches it on ({@link #standaardAan}).
 */
public class MachineKlus extends BasisKlus {
    public static final String ID = "machines";
    /** How many stacks a resident carries to or from a machine in one trip (the 626-guh twice as many). */
    public static final int STAPELS = 4;
    public static final int WERK_TICKS = 24;
    /** How many machines one look at the home base examines at most (the overview). */
    static final int MAX_KIJK = 24;

    MachineKlus() {
        super(ID, () -> new ItemStack(GuhovenFeature.GUH_OVEN_ITEM.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    /**
     * Off until the owner switches it on for a resident: this chore carries the huisje's stock into machines that change
     * it for good (ore into bars, planks into chests), and a world from before the update may have an oven standing
     * around with something in it. Nobody's stock is baked without being asked.
     */
    @Override
    public boolean standaardAan(Mob bewoner) {
        return false;
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<BlockPos> machines = Klusmachines.rond(level, huisje);
        if (machines.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        int halen = 0, vullen = 0, honger = 0, bekeken = 0;
        for (BlockPos p : machines) {
            if (++bekeken > MAX_KIJK) {
                break;
            }
            if (Klusmachines.heeftUitvoer(level, p)) {
                halen++;
            }
            if (!Klusmachines.teVullen(level, huisje, p).isEmpty()) {
                vullen++;
            } else if (!Klusmachines.tekort(level, p).isEmpty()) {
                honger++;
            }
        }
        if (halen > 0) {
            return KlusStand.ja("halen", halen);
        }
        if (vullen > 0) {
            return KlusStand.ja("vullen", vullen);
        }
        return honger > 0 ? KlusStand.nee("geen_voorraad", honger) : KlusStand.straks("rustig", machines.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        boolean[] vullen = new boolean[1];
        BlockPos plek = KlusGebied.kies(level, huisje, TechklusFeature.MACHINE, bewoner, p -> {
            if (!Klusmachines.mag(level, huisje, p)) {
                return false;
            }
            vullen[0] = !Klusmachines.teVullen(level, huisje, p).isEmpty();
            return vullen[0] || Klusmachines.heeftUitvoer(level, p);
        });
        return plek == null ? null : new Taak(level, huisje, bewoner, plek, vullen[0]);
    }

    private class Taak extends StappenTaak {
        private final BlockPos plek;
        private final boolean vullen;
        /** On its way to the machine (goes back into the stock when the chore is cut short). */
        private final List<ItemStack> lading = new ArrayList<>();

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos plek, boolean vullen) {
            super(level, huisje, mob, MachineKlus.this);
            this.plek = plek;
            this.vullen = vullen;
        }

        private int stapels() {
            return STAPELS * VariantGedragen.draagFactor(mob);
        }

        @Override
        protected void begin() {
            claim(plek, 600);
            if (vullen) {
                erbij(loop(Voorraad.brengPlek(level, huisje), 2.3));
                erbij(doe(this::neemVoorraad));
            }
            erbij(loop(plek, 1.9));
            Vec3 midden = Vec3.atCenterOf(plek);
            erbij(werk(WERK_TICKS, midden, t -> {
                if (t % 8 == 0) {
                    level.playSound(null, plek, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.NEUTRAL, 0.5f, 1.2f + mob.getRandom().nextFloat() * 0.3f);
                }
            }));
            erbij(doe(this::bijMachine));
        }

        /** At the stock: takes what the machine is short of (as far as it is still there). */
        private void neemVoorraad() {
            for (Klusmachines.Vraag vraag : Klusmachines.teVullen(level, huisje, plek)) {
                if (lading.size() >= stapels()) {
                    break;
                }
                ItemStack stack = Voorraad.neem(level, huisje, vraag.wens()::past, vraag.aantal());
                if (!stack.isEmpty()) {
                    lading.add(stack);
                }
            }
            if (!lading.isEmpty()) {
                toon(lading.get(0));
            } else if (!Klusmachines.heeftUitvoer(level, plek)) {
                afbreken();   // (somebody took it in the meantime, and there is nothing to fetch either)
            }
        }

        /** At the machine: in goes what it brought, out comes what lies ready. */
        private void bijMachine() {
            if (!Klusmachines.mag(level, huisje, plek)) {
                return;   // (the machine is gone: what it carries goes back, see stop)
            }
            int erin = 0, eruit = 0;
            ResourceHandler<ItemResource> in = Klusmachines.in(level, plek);
            for (ItemStack stack : lading) {
                ItemStack rest = in == null ? stack : Kisten.stop(in, stack);
                erin += stack.getCount() - rest.getCount();
                if (!rest.isEmpty()) {
                    pak(rest);   // (did not fit after all: back into the stock)
                }
            }
            lading.clear();
            toon(ItemStack.EMPTY);
            ResourceHandler<ItemResource> uit = Klusmachines.uit(level, plek);
            for (int i = 0; uit != null && i < stapels(); i++) {
                ItemStack stack = Kisten.neem(uit, s -> true, 64);
                if (stack.isEmpty()) {
                    break;
                }
                eruit += stack.getCount();
                pak(stack);
            }
            if (erin + eruit <= 0) {
                return;
            }
            aantal = erin + eruit;
            level.playSound(null, plek, eruit > 0 ? SoundEvents.ITEM_PICKUP : SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.6f, 1.1f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, plek.getX() + 0.5, plek.getY() + 1.1, plek.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.0);
            gelukt();
            KlusBeloning.techniek(mob, "tech_klusjes_machines");
        }

        @Override
        public void stop() {
            for (ItemStack stack : lading) {
                pak(stack);   // (cut short before the machine: back into the stock)
            }
            lading.clear();
            super.stop();
        }
    }
}
