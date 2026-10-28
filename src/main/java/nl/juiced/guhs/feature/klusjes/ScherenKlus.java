package nl.juiced.guhs.feature.klusjes;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * (1.8.0) Scheren: the guh shears the fluffy tame (or kept) Wolkenschaapjes in the home base and brings the wolkenpluis to
 * the chest, for the Spinmachine. Up to {@link #PER_KEER} schaapjes per trip; the fluff grows back by itself. Guhs only.
 */
public class ScherenKlus extends BasisKlus {
    public static final int PER_KEER = 3;

    public ScherenKlus() {
        super("scheren", () -> new ItemStack(Bio.item("wolkenpluis", Items.WHITE_WOOL)), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    private static List<WolkenschaapjeEntity> schaapjes(ServerLevel level, Huisje huisje) {
        return level.getEntitiesOfClass(WolkenschaapjeEntity.class, huisje.gebied(), s -> s.isAlive() && (s.isTame() || s.isGehouden())
                && huisje.inGebied(s.blockPosition()) && KlusGebied.inTest(huisje, s.position()) && !KlusGebied.beschermd(level, s.blockPosition()));
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<WolkenschaapjeEntity> l = schaapjes(level, huisje);
        long pluizig = l.stream().filter(s -> !s.isGeschoren() && !s.isBaby()).count();
        if (pluizig > 0) {
            return KlusStand.ja("pluizig", (int) pluizig);
        }
        return l.isEmpty() ? KlusStand.nee("geen", 0) : KlusStand.straks("kaal", l.size());
    }

    @Nullable
    static WolkenschaapjeEntity schaapje(ServerLevel level, Huisje huisje, Vec3 bij) {
        return schaapjes(level, huisje).stream().filter(s -> !s.isGeschoren() && !s.isBaby() && !KlusGebied.geclaimd(level, s.blockPosition()))
                .min(Comparator.comparingDouble(s -> s.position().distanceToSqr(bij))).orElse(null);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        WolkenschaapjeEntity s = schaapje(level, huisje, bewoner.position());
        return s == null ? null : new Taak(level, huisje, bewoner, s);
    }

    private class Taak extends StappenTaak {
        private WolkenschaapjeEntity schaapje;
        private int keer;

        Taak(ServerLevel level, Huisje huisje, Mob mob, WolkenschaapjeEntity schaapje) {
            super(level, huisje, mob, ScherenKlus.this);
            this.schaapje = schaapje;
        }

        @Override
        protected void begin() {
            naar(schaapje);
        }

        private void naar(WolkenschaapjeEntity s) {
            schaapje = s;
            claim(s.blockPosition(), 200);
            erbij(loopNaar(() -> schaapje, 1.9));
            erbij(werk(24, s.position().add(0, s.getBbHeight() * 0.7, 0), t -> {
                if (t % 8 == 0 && schaapje != null) {
                    hartjes(schaapje, 1);
                    schaapje.getNavigation().stop();
                }
            }));
            erbij(doe(this::scheer));
        }

        private void scheer() {
            if (schaapje == null || !schaapje.isAlive() || schaapje.isGeschoren() || schaapje.isBaby()) {
                return;
            }
            ItemStack pluis = schaapje.scheer(null, level);
            pak(pluis);
            aantal += pluis.getCount();
            keer++;
            gelukt();
            if (keer < PER_KEER) {
                WolkenschaapjeEntity nog = schaapje(level, huisje, mob.position());
                if (nog != null && nog.distanceToSqr(mob) <= 100) {
                    naar(nog);
                }
            }
        }
    }
}
