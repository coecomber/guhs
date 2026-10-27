package nl.juiced.guhs.feature.blub.klus;

import java.util.function.Supplier;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.KlusGebied;

/**
 * 1.8.0 ronde 3b: the four chores only a Blub-guh does in a Guhhuisje (like Sjokkel's {@code polijsten}: {@link #kan} is
 * "a Blub", {@link #doeners} "blub", so the huisje screen shows them as "alleen Blubs" for everyone else):
 * <ul>
 *   <li>{@link BellenKlus Bellenbesproeier}: bubbles of water over the growing crops (a mild extra growth tick);</li>
 *   <li>{@link BezorgKlus Knabbelbezorger}: knabbels from the chest to the tame guhs and critters around the huisje;</li>
 *   <li>{@link ZingKlus Slaapliedjeszanger}: the lullaby in the evening, the huisje's guhs are uitgerust the next day;</li>
 *   <li>{@link BadKlus Bubbelbadmeester}: gets the {@link BlubBadjeBlock Blub-bubbelbadje} ready with foam; guhs of the huisje
 *       take a bath and feel {@link FrisEnBlijEffect fris en blij}.</li>
 * </ul>
 */
public final class BlubKlusjes {
    /** The home base scans for these (KlusGebied.registreer): crops that still grow, and Blub-bubbelbadjes. */
    public static final String GROEIEND = "blub_groeiend", BADJE = "blub_badje";

    private BlubKlusjes() {
    }

    public static void registreer() {
        KlusGebied.registreer(GROEIEND, (level, pos, state) -> state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state));
        KlusGebied.registreer(BADJE, (level, pos, state) -> state.getBlock() instanceof BlubBadjeBlock);
        Klusjes.registreer(new BellenKlus());
        Klusjes.registreer(new BezorgKlus());
        Klusjes.registreer(new ZingKlus());
        Klusjes.registreer(new BadKlus());
    }

    /** The common part: only a Blub does it ("alleen Blubs" in the huisje screen). */
    abstract static class BlubKlus extends BasisKlus {
        BlubKlus(String id, Supplier<ItemStack> icoon, int wacht) {
            super(id, icoon, wacht);
        }

        @Override
        public boolean kan(Mob bewoner) {
            return bewoner instanceof BlubGuhEntity;
        }

        @Override
        public String doeners() {
            return "blub";
        }
    }
}
