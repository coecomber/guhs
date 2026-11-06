package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.MateriaalTas;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen;

/**
 * T-INT: plugs t-opslag's {@link Materiaaltas} into t-ambacht's {@link MateriaalTas#BRON}. Gathered ambacht materials go into
 * the bag (when the player may use it here and has auto-pickup on); recipes and orders count and take from the inventory first,
 * then from the bag. Every ambacht material counts as a material for the bag ({@link Materiaaltas#regel}).
 */
public final class AmbachtTas implements MateriaalTas.Bron {
    private AmbachtTas() {
    }

    public static void koppel() {
        Materiaaltas.regel(s -> Materialen.van(s) != null);
        MateriaalTas.BRON = new AmbachtTas();
    }

    @Override
    public void stop(ServerPlayer p, ItemStack s) {
        OpslagData d = Opslag.data(p);
        if (!s.isEmpty() && Materiaaltas.isMateriaal(s) && Opslag.magOpenen(p) && !d.autoUit) {
            Materiaaltas.stop(d, s);
        }
    }

    @Override
    public int tel(ServerPlayer p, Predicate<ItemStack> wat) {
        long n = 0;
        for (OpslagData.Materiaal m : Opslag.data(p).materialen) {
            if (wat.test(m.soort)) {
                n += m.aantal;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    @Override
    public List<ItemStack> neem(ServerPlayer p, Predicate<ItemStack> wat, int n) {
        OpslagData d = Opslag.data(p);
        List<ItemStack> uit = new ArrayList<>();
        for (OpslagData.Materiaal m : new ArrayList<>(d.materialen)) {
            while (n > 0 && wat.test(m.soort)) {
                ItemStack s = Materiaaltas.haalUit(d, m.soort, n);
                if (s.isEmpty()) {
                    break;
                }
                n -= s.getCount();
                uit.add(s);
            }
            if (n <= 0) {
                break;
            }
        }
        return uit;
    }
}
