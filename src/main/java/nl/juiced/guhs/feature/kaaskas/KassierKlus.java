package nl.juiced.guhs.feature.kaaskas;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.FarmenKlus;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * Kassier (1.8.0): the resident works the Kaaskassen of the home base. It walks into a kas that has ripe plants, picks up to
 * {@link KaaskasGetallen#KASSIER_PER_KEER} of them in one go, plants every one again (one seed of each harvest goes back into
 * the ground, {@link FarmenKlus#oogst}) and brings the rest to the Bank Guh / the chest. Only kassen its owner placed. Guhs only.
 */
public class KassierKlus extends BasisKlus {
    public static final String ID = "kassier";
    /** The KlusGebied marker for the kern of a Kaaskas. */
    public static final String KAS = "kaaskas_kern";
    public static final int PLUK_TICKS = 30;

    KassierKlus() {
        super(ID, () -> new ItemStack(KaaskasFeature.KAASKAS_ITEM.get()), 200);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    /** The kas whose kern stands here, when the residents of this huisje may work it. */
    @Nullable
    static KaaskasBlockEntity kas(ServerLevel level, Huisje h, BlockPos kern) {
        if (level.isLoaded(kern) && level.getBlockEntity(kern) instanceof KaaskasBlockEntity kas && !kas.isRemoved()
                && (kas.eigenaar() == null || kas.eigenaar().equals(h.eigenaar())) && !Bescherming.beschermd(level, kern)) {
            return kas;
        }
        return null;
    }

    /** The kassen of this home base its residents work. */
    public static List<KaaskasBlockEntity> kassen(ServerLevel level, Huisje h) {
        List<KaaskasBlockEntity> uit = new ArrayList<>();
        for (BlockPos p : KlusGebied.van(level, h, KAS)) {
            KaaskasBlockEntity kas = kas(level, h, p);
            if (kas != null) {
                uit.add(kas);
            }
        }
        return uit;
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<KaaskasBlockEntity> kassen = kassen(level, huisje);
        if (kassen.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        int rijp = 0, gewassen = 0;
        for (KaaskasBlockEntity kas : kassen) {
            rijp += kas.rijp(level).size();
            gewassen += kas.gewassen(level).size();
        }
        if (rijp > 0) {
            return KlusStand.ja("rijp", rijp);
        }
        return gewassen > 0 ? KlusStand.straks("groeit", gewassen) : KlusStand.nee("leeg", kassen.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos kern = KlusGebied.kies(level, huisje, KAS, bewoner, p -> {
            KaaskasBlockEntity kas = kas(level, huisje, p);
            return kas != null && !kas.rijp(level).isEmpty();
        });
        return kern == null ? null : new Taak(level, huisje, bewoner, kern);
    }

    private class Taak extends StappenTaak {
        private final BlockPos kern;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos kern) {
            super(level, huisje, mob, KassierKlus.this);
            this.kern = kern;
        }

        @Override
        protected void begin() {
            claim(kern, 600);
            erbij(loop(kern, 2.5));
            erbij(werk(PLUK_TICKS, Vec3.atCenterOf(kern), t -> {
                if (t % 8 == 0) {
                    mob.swing(InteractionHand.MAIN_HAND);
                }
            }));
            erbij(doe(this::pluk));
        }

        private void pluk() {
            KaaskasBlockEntity kas = kas(level, huisje, kern);
            if (kas == null) {
                afbreken();
                return;
            }
            int n = 0;
            for (BlockPos p : kas.rijp(level)) {
                if (n >= KaaskasGetallen.KASSIER_PER_KEER) {
                    break;
                }
                for (ItemStack d : FarmenKlus.oogst(level, p, mob)) {
                    pak(d);
                }
                n++;
            }
            if (n > 0) {
                aantal = n;
                gelukt();
            }
        }
    }
}
