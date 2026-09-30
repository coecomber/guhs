package nl.juiced.guhs.feature.beauty;

import java.util.Collection;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.registry.ModItems;

/**
 * The three jury guhs of the beauty contest and how they score an outfit (each 1-10):
 * <ul>
 *     <li>Juf Vadsma (strict): does every piece fit the theme? Off-theme pieces cost a point each.</li>
 *     <li>Meneer Glitterguh (style): a complete outfit (all four slots), a bonus for pieces that belong together, and he
 *     hates a mess of off-theme pieces.</li>
 *     <li>Oma Knabbel (heart): a bit of luck, and she loves your own guh, the Showster set and a well-matched look.</li>
 * </ul>
 */
public final class BeautyJury {
    public static final List<String> JUDGES = List.of("vadsma", "glitterguh", "knabbel");

    /** The jury's scores for one walk on the catwalk. */
    public record Verdict(int[] scores, int fit, int maxFit, int worn, int offTheme, boolean set, int showster, boolean ownGuh) {
        public int total() {
            int total = 0;
            for (int s : scores) {
                total += s;
            }
            return total;
        }

        /** Rosettes for this round: nothing for a naked model (no AFK farming), else 2 up to 5 for a top score. */
        public int rosettes() {
            int total = total();
            return worn == 0 ? 0 : total >= 27 ? 5 : total >= 22 ? 4 : total >= 16 ? 3 : 2;
        }

        /** How the judge's comment sounds: naked / low / mid / high / top. */
        public String band(int judge) {
            int s = scores[judge];
            return worn == 0 ? "naked" : s >= 10 ? "top" : s >= 8 ? "high" : s >= 5 ? "mid" : "low";
        }
    }

    public static Verdict judge(ShowTheme theme, Collection<GuhClothes> worn, boolean ownGuh, RandomSource random) {
        return judge(theme, worn, ownGuh, random, nl.juiced.guhs.feature.spelen.Niveau.MEDIUM);
    }

    /**
     * The jury on a level (2.9). Medium is the jury as it always was. Makkelijk: a mild jury (every judge one point more for
     * a dressed model). Lastig: a strict jury: Juf Vadsma takes two points per off-theme piece instead of one, Meneer
     * Glitterguh wants a matching set (one point less without) and hates even one off-theme piece, and Oma Knabbel is a
     * little harder to please.
     */
    public static Verdict judge(ShowTheme theme, Collection<GuhClothes> worn, boolean ownGuh, RandomSource random,
                                nl.juiced.guhs.feature.spelen.Niveau niveau) {
        int fit = 0, off = 0, showster = 0;
        for (GuhClothes c : worn) {
            int f = theme.fit(c);
            fit += f;
            if (f == 0) {
                off++;
            }
            if (ShowTheme.SHOWSTER.contains(c)) {
                showster++;
            }
        }
        int maxFit = Math.max(1, theme.maxFit());
        int n = worn.size();
        boolean set = ShowTheme.hasSet(worn);
        float match = Math.min(1f, fit / (float) maxFit);

        int vadsma = n == 0 ? 1 : Mth.clamp(1 + Math.round(9 * match) - off, 1, 10);
        int glitter = n == 0 ? 1 : Mth.clamp(1 + 2 * n + (set ? 1 : 0) - (off >= 2 ? 2 : 0), 1, 10);
        int knabbel = Mth.clamp(4 + random.nextInt(4) + (ownGuh ? 1 : 0) + Math.min(2, showster) + (match >= 0.75f ? 1 : 0)
                - (n == 0 ? 3 : 0), 1, 10);
        if (n > 0 && niveau == nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK) {
            vadsma = Math.min(10, vadsma + 1);
            glitter = Math.min(10, glitter + 1);
            knabbel = Math.min(10, knabbel + 1);
        } else if (n > 0 && niveau == nl.juiced.guhs.feature.spelen.Niveau.LASTIG) {
            vadsma = Mth.clamp(vadsma - off, 1, 10);
            glitter = Mth.clamp(glitter - (set ? 0 : 1) - (off == 1 ? 2 : 0), 1, 10);
            knabbel = Mth.clamp(knabbel - 1, 1, 10);
        }
        return new Verdict(new int[]{vadsma, glitter, knabbel}, fit, maxFit, n, off, set, showster, ownGuh);
    }

    /**
     * The funny reason a jury guh says with its score, about THIS outfit ("Kijk nou: Winterdas bij Winterpret! Past
     * perfect, VAHOEG!"). Falls back on a general line for the score band when there's nothing specific to say.
     */
    public static Component reason(int judge, Verdict verdict, ShowTheme theme, List<GuhClothes> worn, Component model, RandomSource random) {
        String band = verdict.band(judge);
        String base = "quest.guhs.beauty.reason." + JUDGES.get(judge) + ".";
        Component themeName = Component.translatable("gui.guhs.beauty.theme." + theme.id());
        if (worn.isEmpty()) {
            return Component.translatable("quest.guhs.beauty.jury." + JUDGES.get(judge) + ".naked", themeName);
        }
        int score = verdict.scores()[judge];
        switch (judge) {
            case 0 -> {                                        // Juf Vadsma: the theme, piece by piece
                GuhClothes off = worn.stream().filter(c -> theme.fit(c) == 0).findFirst().orElse(null);
                GuhClothes best = worn.stream().max(java.util.Comparator.comparingInt(theme::fit)).orElse(null);
                if (off != null && (score < 8 || random.nextBoolean())) {
                    return Component.translatable(base + "off", name(off), themeName);
                }
                if (best != null && theme.fit(best) >= 3) {
                    return Component.translatable(base + "perfect", name(best), themeName);
                }
                if (best != null && theme.fit(best) == 2) {
                    return Component.translatable(base + "good", name(best), themeName);
                }
            }
            case 1 -> {                                        // Meneer Glitterguh: a complete, matching look
                for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                    if (worn.stream().noneMatch(c -> c.slot == slot)) {
                        return Component.translatable(base + "missing." + slot.name().toLowerCase(java.util.Locale.ROOT));
                    }
                }
                List<GuhClothes> set = ShowTheme.matching(worn);
                if (set.size() >= 2) {
                    return Component.translatable(base + "set", name(set.get(0)), name(set.get(1)));
                }
            }
            default -> {                                       // Oma Knabbel: her heart
                if (verdict.ownGuh() && random.nextInt(3) > 0) {
                    return Component.translatable(base + "own", model);
                }
                GuhClothes showster = worn.stream().filter(ShowTheme.SHOWSTER::contains).findFirst().orElse(null);
                if (showster != null) {
                    return Component.translatable(base + "showster", name(showster));
                }
                if (score >= 6 && random.nextBoolean()) {
                    return Component.translatable(base + "piece", name(worn.get(random.nextInt(worn.size()))));
                }
            }
        }
        return Component.translatable("quest.guhs.beauty.jury." + JUDGES.get(judge) + "." + band, themeName);
    }

    private static Component name(GuhClothes clothes) {
        return ModItems.clothingItem(clothes).getDescription();
    }

    private BeautyJury() {
    }
}
