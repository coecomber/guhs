package nl.juiced.guhs.feature.klusjes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeOpslag;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * Opruimen & sorteren: things lying around in the home base are picked up (a few per trip) and brought to the chest, or
 * to the Bank Guh, which sorts everything. Nothing lying around and there is a Bank Guh? Then the resident empties the
 * huisje's chest into the Bank Guh, one armful at a time (never loaned things: those stay in the chest). Only when there
 * is somewhere to put it (a chest with room or a Bank Guh), else things would only move to the door and back. Guhs and
 * pieppiepmuisjes.
 */
public class OpruimenKlus extends BasisKlus {
    public static final int PER_KEER = 4;
    /** Items that just landed stay put a moment (a player is still dropping things, a harvest is still flying). */
    public static final int RUST = 60;
    /** Items another resident is already walking to. */
    private static final Map<ItemEntity, Long> GECLAIMD = new WeakHashMap<>();

    OpruimenKlus() {
        super("opruimen", () -> new ItemStack(Items.CHEST), 200);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isMuisje(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        if (!Voorraad.heeftOpslag(level, huisje)) {
            return null;
        }
        ItemEntity item = vind(level, huisje, bewoner.position(), null);
        if (item != null) {
            return new Taak(level, huisje, bewoner, item, null);
        }
        if (HuisjeOpslag.heeftBankGuh(level, huisje)) {
            for (BlockPos kist : Voorraad.kisten(level, huisje)) {
                if (heeftSorteerbaars(level, kist)) {
                    return new Taak(level, huisje, bewoner, null, kist);
                }
            }
        }
        return null;
    }

    /** The nearest item in the home base worth tidying (that fits somewhere), or null. */
    @Nullable
    static ItemEntity vind(ServerLevel level, Huisje huisje, Vec3 bij, @Nullable Vec3 binnen6) {
        long nu = level.getGameTime();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, huisje.gebied(), i -> i.isAlive() && !i.getItem().isEmpty()
                && i.getAge() >= RUST && !i.hasPickUpDelay() && huisje.inGebied(i.blockPosition()) && KlusGebied.inTest(huisje, i.position())
                && (binnen6 == null || i.position().distanceToSqr(binnen6) <= 36)
                && !(GECLAIMD.containsKey(i) && GECLAIMD.get(i) > nu) && Voorraad.past(level, huisje, i.getItem()));
        return items.stream().min(Comparator.comparingDouble(i -> i.distanceToSqr(bij))).orElse(null);
    }

    static boolean heeftSorteerbaars(ServerLevel level, BlockPos kist) {
        IItemHandler handler = Voorraad.handler(level, kist);
        if (handler == null) {
            return false;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && !Features.isLoaned(s)) {
                return true;
            }
        }
        return false;
    }

    private class Taak extends StappenTaak {
        @Nullable
        private ItemEntity item;
        @Nullable
        private final BlockPos kist;
        private int keer;

        Taak(ServerLevel level, Huisje huisje, Mob mob, @Nullable ItemEntity item, @Nullable BlockPos kist) {
            super(level, huisje, mob, OpruimenKlus.this);
            this.item = item;
            this.kist = kist;
        }

        @Override
        protected void begin() {
            if (item != null) {
                naar(item);
            } else if (kist != null) {
                erbij(loop(kist, 2.0));
                erbij(werk(20, Vec3.atCenterOf(kist), null));
                erbij(doe(this::uitKist));
            }
        }

        private void naar(ItemEntity i) {
            item = i;
            GECLAIMD.put(i, level.getGameTime() + 300);
            erbij(loopNaar(() -> item != null && item.isAlive() ? item : null, 1.3));
            erbij(doe(this::raap));
        }

        private void raap() {
            if (item == null) {
                return;
            }
            Vec3 waar = item.position();
            int n = raapOp(waar, 1.6);
            if (n > 0) {
                keer++;
                aantal += n;
                gelukt();
            }
            if (keer < PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(mob)) {   // (3.0: the 626-guh carries twice as much)
                ItemEntity nog = vind(level, huisje, mob.position(), waar);
                if (nog != null) {
                    naar(nog);
                }
            }
        }

        /** An armful out of the chest, for the Bank Guh. */
        private void uitKist() {
            IItemHandler handler = Voorraad.handler(level, kist);
            if (handler == null) {
                return;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty() && !Features.isLoaned(s)) {
                    ItemStack uit = handler.extractItem(i, s.getCount(), false);
                    if (!uit.isEmpty()) {
                        pak(uit);
                        aantal += uit.getCount();
                        gelukt();
                        return;
                    }
                }
            }
        }
    }
}
