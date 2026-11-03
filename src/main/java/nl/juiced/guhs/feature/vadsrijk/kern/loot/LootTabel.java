package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * A loot table: {@code rollen} tries, each dropping gear with chance {@code kans} (a weighted base type, a rolled rarity with
 * {@code geluk}, the mob's level), plus Vadsmunten (brons) between min and max. Register with {@link LootTabellen#registreer}.
 */
public record LootTabel(List<Regel> regels, int rollen, double kans, double geluk, int muntenMin, int muntenMax) {
    public record Regel(String basisType, int gewicht) {
    }

    public record Resultaat(List<ItemStack> items, long munten) {
    }

    public Resultaat rol(int level, RandomSource r) {
        List<ItemStack> uit = new ArrayList<>();
        int som = regels.stream().mapToInt(Regel::gewicht).sum();
        for (int i = 0; i < rollen && som > 0; i++) {
            if (r.nextDouble() >= kans) {
                continue;
            }
            int x = r.nextInt(som);
            for (Regel g : regels) {
                x -= g.gewicht();
                if (x < 0) {
                    BasisType t = BasisTypes.van(g.basisType());
                    if (t != null) {
                        ItemStack s = Rollen.stack(Rollen.rol(t, level, Zeldzaamheid.rol(r, geluk), r));
                        if (!s.isEmpty()) {
                            uit.add(s);
                        }
                    }
                    break;
                }
            }
        }
        long munten = muntenMax <= muntenMin ? muntenMin : muntenMin + r.nextInt(muntenMax - muntenMin + 1);
        return new Resultaat(uit, munten);
    }
}
