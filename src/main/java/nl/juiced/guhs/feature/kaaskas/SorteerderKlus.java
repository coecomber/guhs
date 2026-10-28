package nl.juiced.guhs.feature.kaaskas;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.vadskracht.Kisten;

/**
 * Sorteerder (1.8.0): the resident waits at the end of a Guh-sorteerband ({@link SorteerbandBlock}) in the klus-area, picks up
 * what came riding along and sorts it: every thing goes into the chest of the home base that already holds that kind of thing
 * (so a chest "learns" what belongs in it: put one of a kind in yourself). What has no chest of its own goes the usual way: the
 * Bank Guh, a Hapluikje, any chest, the door ({@link Voorraad#lever}). Guhs only.
 */
public class SorteerderKlus extends BasisKlus {
    public static final String ID = "sorteerder";
    /** The KlusGebied marker for the last piece of a sorteerband. */
    public static final String BANDEIND = "sorteerband_eind";
    public static final int SORTEER_TICKS = 20;

    SorteerderKlus() {
        super(ID, () -> new ItemStack(KaaskasFeature.SORTEERBAND_ITEM.get()), 100);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    /** What lies waiting at the end of a band. */
    static List<ItemEntity> wacht(ServerLevel level, BlockPos eind) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(eind).inflate(0.4, 0.6, 0.4), i -> i.isAlive() && !i.getItem().isEmpty());
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<BlockPos> einden = KlusGebied.van(level, huisje, BANDEIND);
        if (einden.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        int n = 0;
        for (BlockPos p : einden) {
            for (ItemEntity i : wacht(level, p)) {
                n += i.getItem().getCount();
            }
        }
        return n > 0 ? KlusStand.ja("wacht", n) : KlusStand.straks("leeg", einden.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos eind = KlusGebied.kies(level, huisje, BANDEIND, bewoner, p -> !wacht(level, p).isEmpty());
        return eind == null ? null : new Taak(level, huisje, bewoner, eind);
    }

    /** Puts the stack into a chest of the home base that already holds that kind; returns what is left. */
    public static ItemStack sorteer(ServerLevel level, Huisje h, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (BlockPos kist : Voorraad.kisten(level, h)) {
            ResourceHandler<ItemResource> handler = Kisten.van(level, kist, null);
            if (handler == null || Kisten.tel(handler, s -> ItemStack.isSameItemSameComponents(s, stack)) <= 0) {
                continue;
            }
            rest = Kisten.stop(handler, rest);
            if (rest.isEmpty()) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, kist.getX() + 0.5, kist.getY() + 1.0, kist.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
                break;
            }
        }
        return rest;
    }

    private class Taak extends StappenTaak {
        private final BlockPos eind;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos eind) {
            super(level, huisje, mob, SorteerderKlus.this);
            this.eind = eind;
        }

        @Override
        protected void begin() {
            claim(eind, 400);
            erbij(loop(eind, 1.8));
            erbij(werk(SORTEER_TICKS, Vec3.atCenterOf(eind), null));
            erbij(doe(this::sorteer));
        }

        private void sorteer() {
            int n = raapOp(Vec3.atCenterOf(eind), 1.0);
            if (n <= 0) {
                return;
            }
            Iterator<ItemStack> it = buit.iterator();
            while (it.hasNext()) {
                ItemStack s = it.next();
                ItemStack rest = SorteerderKlus.sorteer(level, huisje, s);
                if (rest.isEmpty()) {
                    it.remove();
                } else {
                    s.setCount(rest.getCount());
                }
            }
            aantal = n;
            gelukt();
        }
    }
}
