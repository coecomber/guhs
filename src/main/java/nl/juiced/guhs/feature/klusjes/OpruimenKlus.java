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
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * Opruimen & sorteren: things lying around in the home base are picked up (a few per trip) and brought to the chest, or
 * to the Bank Guh, which sorts everything. Nothing lying around and there is a Bank Guh? Then the resident empties the
 * huisje's chest into the Bank Guh, one armful at a time (never loaned things: those stay in the chest). Only when there
 * is somewhere to put it (a chest with room or a Bank Guh), else things would only move to the door and back. Guhs and
 * pieppiepmuisjes.
 * <p>
 * bbq2: a Bank Guh holds at most 256 of one kind (unless upgraded). So only what the bank still has room for counts as
 * "to be sorted", and an armful is never bigger than that room: what the bank is full of simply stays in the chest (else
 * a guh would carry it to the bank and back for ever).
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

    @Override
    public String doeners() {
        return "guhs_muisjes";
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        if (!Voorraad.heeftOpslag(level, huisje)) {
            return KlusStand.nee("geen_opslag", 0);
        }
        int n = liggend(level, huisje, null, false).size();
        if (n > 0) {
            return KlusStand.ja("spullen", n);
        }
        if (HuisjeOpslag.heeftBankGuh(level, huisje)) {
            for (BlockPos kist : Voorraad.kisten(level, huisje)) {
                if (heeftSorteerbaars(level, huisje, kist)) {
                    return KlusStand.ja("sorteren", 0);
                }
            }
        }
        return KlusStand.straks("netjes", 0);
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
                if (heeftSorteerbaars(level, huisje, kist)) {
                    return new Taak(level, huisje, bewoner, null, kist);
                }
            }
        }
        return null;
    }

    /** The nearest item in the home base worth tidying (that fits somewhere), or null. */
    @Nullable
    static ItemEntity vind(ServerLevel level, Huisje huisje, Vec3 bij, @Nullable Vec3 binnen6) {
        return liggend(level, huisje, binnen6, true).stream().min(Comparator.comparingDouble(i -> i.distanceToSqr(bij))).orElse(null);
    }

    /** Everything lying around in the home base worth tidying (that fits somewhere); vrij: not what another resident walks to. */
    static List<ItemEntity> liggend(ServerLevel level, Huisje huisje, @Nullable Vec3 binnen6, boolean vrij) {
        long nu = level.getGameTime();
        return level.getEntitiesOfClass(ItemEntity.class, huisje.gebied(), i -> i.isAlive() && !i.getItem().isEmpty()
                && i.getAge() >= RUST && !i.hasPickUpDelay() && huisje.inGebied(i.blockPosition()) && KlusGebied.inTest(huisje, i.position())
                && !KlusGebied.beschermd(level, i.blockPosition())
                && (binnen6 == null || i.position().distanceToSqr(binnen6) <= 36)
                && !(vrij && GECLAIMD.containsKey(i) && GECLAIMD.get(i) > nu) && Voorraad.past(level, huisje, i.getItem()));
    }

    /** Is there something in this chest that a Bank Guh of the home base still has room for? */
    static boolean heeftSorteerbaars(ServerLevel level, Huisje huisje, BlockPos kist) {
        IItemHandler handler = Voorraad.handler(level, kist);
        if (handler == null) {
            return false;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && !Features.isLoaned(s) && HuisjeOpslag.bankRuimte(level, huisje, s) > 0) {
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

        /** An armful out of the chest, for the Bank Guh: never more than the bank still takes of it. */
        private void uitKist() {
            IItemHandler handler = Voorraad.handler(level, kist);
            if (handler == null) {
                return;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                long ruimte = s.isEmpty() ? 0 : HuisjeOpslag.bankRuimte(level, huisje, s);
                if (ruimte > 0) {
                    ItemStack uit = handler.extractItem(i, (int) Math.min(s.getCount(), ruimte), false);
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
