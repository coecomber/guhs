package nl.juiced.guhs.feature.blub.klus;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;

/**
 * Bubbelbadmeester (a Blub's chore, id {@value #ID}): the Blub hops to a {@link BlubBadjeBlock Blub-bubbelbadje} in the
 * home base without foam and gets it ready: it stirs, blows bubbles and sings a little, and then there is foam. The guhs
 * around then take a bath now and then (the badje does that) and feel fris en blij.
 */
public class BadKlus extends BlubKlusjes.BlubKlus {
    public static final String ID = "blub_bad";
    public static final int KLAARMAAK_TICKS = 80;

    BadKlus() {
        super(ID, () -> new ItemStack(BlubFeature.BADJE_ITEM.get()), 400);
    }

    static boolean zonderSchuim(ServerLevel level, BlockPos p) {
        var s = level.getBlockState(p);
        return s.getBlock() instanceof BlubBadjeBlock && !s.getValue(BlubBadjeBlock.SCHUIM);
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        var badjes = KlusGebied.van(level, huisje, BlubKlusjes.BADJE);
        if (badjes.isEmpty()) {
            return KlusStand.nee("geen_badje", 0);
        }
        long leeg = badjes.stream().filter(p -> zonderSchuim(level, p)).count();
        return leeg > 0 ? KlusStand.ja("klaarmaken", (int) leeg) : KlusStand.straks("schuim", badjes.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos p = KlusGebied.kies(level, huisje, BlubKlusjes.BADJE, bewoner, q -> zonderSchuim(level, q));
        return p == null ? null : new Taak(level, huisje, bewoner, p);
    }

    private class Taak extends StappenTaak {
        private final BlockPos badje;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos badje) {
            super(level, huisje, mob, BadKlus.this);
            this.badje = badje;
        }

        @Override
        protected void begin() {
            claim(badje, 600);
            erbij(loop(badje, 1.6));
            erbij(werk(KLAARMAAK_TICKS, Vec3.atCenterOf(badje), t -> {
                if (t % 10 == 0) {
                    BlubBadjeBlock.bubbels(level, badje, 4);
                }
                if (t % 30 == 1 && mob instanceof BlubGuhEntity b) {
                    BlubGeluid.KLUS_BADDEREN.speel(b);
                }
            }));
            erbij(doe(() -> {
                if (!zonderSchuim(level, badje)) {
                    return;
                }
                BlubBadjeBlock.schuim(level, badje);
                aantal = 1;
                gelukt();
            }));
        }
    }
}
