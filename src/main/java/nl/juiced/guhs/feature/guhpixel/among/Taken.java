package nl.juiced.guhs.feature.guhpixel.among;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;

/**
 * The eight task mini-games of Among Guhs and the two repair jobs, server side: what the panel asks (the "opgave", made
 * here and sent to the client with the panel) and whether the answer the client sent back is right. The screens are in
 * {@code among.client.TaakSpellen}; the same tag keys are used on both sides.
 * <table>
 *   <caption>kinds</caption>
 *   <tr><td>worstjes</td><td>Rechts[4]: which colour hangs at each right hook</td><td>Paren[4]: the right hook of every left sausage</td></tr>
 *   <tr><td>pasje</td><td>Min, Max: the swipe time in ms</td><td>Ms</td></tr>
 *   <tr><td>kruimelbak</td><td>Houd: ticks to hold the lever</td><td>Vast</td></tr>
 *   <tr><td>pindasaus</td><td>Doel, Marge: the line on the tank in %</td><td>Peil</td></tr>
 *   <tr><td>sorteren</td><td>Soorten[6]: 0 kaasknabbel, 1 knabbel, 2 kruimel</td><td>Bakken[6]</td></tr>
 *   <tr><td>dromen</td><td>Wacht: ticks the bar takes</td><td>(nothing: the server keeps the time)</td></tr>
 *   <tr><td>wegen</td><td>Stil: ticks to stand still</td><td>Stil</td></tr>
 *   <tr><td>schakelaars</td><td>Begin, Doel: six switches as bits</td><td>Stand</td></tr>
 *   <tr><td>herstel_licht</td><td>Begin: five switches as bits</td><td>Stand (all on)</td></tr>
 *   <tr><td>herstel_alarm</td><td>Code: four digits</td><td>Code</td></tr>
 * </table>
 * {@link #oplossing} gives a right answer for an opgave (tests, and the proof that every opgave can be solved).
 */
public final class Taken {
    public static final String WORSTJES = "worstjes", PASJE = "pasje", KRUIMELBAK = "kruimelbak", PINDASAUS = "pindasaus", SORTEREN = "sorteren",
            DROMEN = "dromen", WEGEN = "wegen", SCHAKELAARS = "schakelaars";
    /** The eight kinds of the ship table, in the order of the design. */
    public static final List<String> SOORTEN = List.of(WORSTJES, PASJE, KRUIMELBAK, PINDASAUS, SORTEREN, DROMEN, WEGEN, SCHAKELAARS);
    public static final int WORSTEN = 4, KNABBELS = 6, BAKKEN = 3, SCHAKELS = 6, LICHTEN = 5, CODE_LENGTE = 4;

    /** A kind with a fixed shortest time (in ticks, before the round's own pace is applied). */
    private abstract static class Spel implements TaakSoorten.TaakSoort {
        private final String id;
        private final int basis;

        Spel(String id, int basis) {
            this.id = id;
            this.basis = basis;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int duur(Balans balans) {
            return basis;
        }

        @Override
        public int minTicks(Balans balans) {
            return Math.max(1, basis * balans.spelerTaakTijd / 100);
        }
    }

    private static int[] reeks(CompoundTag tag, String sleutel, int lengte) {
        int[] a = tag.getIntArray(sleutel).orElse(null);
        return a != null && a.length == lengte ? a : null;
    }

    static void registreer() {
        TaakSoorten.registreer(new Spel(WORSTJES, 30) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                List<Integer> rechts = new ArrayList<>(List.of(0, 1, 2, 3));
                do {
                    Collections.shuffle(rechts, rng);
                } while (rechts.equals(List.of(0, 1, 2, 3)));
                CompoundTag t = new CompoundTag();
                t.putIntArray("Rechts", rechts.stream().mapToInt(Integer::intValue).toArray());
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                int[] rechts = reeks(opgave, "Rechts", WORSTEN), paren = reeks(resultaat, "Paren", WORSTEN);
                if (rechts == null || paren == null) {
                    return false;
                }
                for (int i = 0; i < WORSTEN; i++) {
                    if (paren[i] < 0 || paren[i] >= WORSTEN || rechts[paren[i]] != i) {
                        return false;
                    }
                }
                return true;
            }
        });
        TaakSoorten.registreer(new Spel(PASJE, 15) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Min", 550);
                t.putInt("Max", 950);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                int ms = resultaat.getIntOr("Ms", -1);
                return opgave.contains("Min") && ms >= opgave.getIntOr("Min", 0) && ms <= opgave.getIntOr("Max", 0);
            }
        });
        TaakSoorten.registreer(new Spel(KRUIMELBAK, 50) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Houd", 60);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Houd") && resultaat.getIntOr("Vast", 0) >= opgave.getIntOr("Houd", 0);
            }
        });
        TaakSoorten.registreer(new Spel(PINDASAUS, 30) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Doel", 62 + rng.nextInt(27));
                t.putInt("Marge", 5);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Doel") && resultaat.contains("Peil")
                        && Math.abs(resultaat.getIntOr("Peil", -100) - opgave.getIntOr("Doel", 0)) <= opgave.getIntOr("Marge", 0);
            }
        });
        TaakSoorten.registreer(new Spel(SORTEREN, 30) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                int[] soorten = new int[KNABBELS];
                for (int i = 0; i < KNABBELS; i++) {
                    soorten[i] = i < BAKKEN ? i : rng.nextInt(BAKKEN);       // every bin gets at least one
                }
                for (int i = KNABBELS - 1; i > 0; i--) {
                    int j = rng.nextInt(i + 1), h = soorten[i];
                    soorten[i] = soorten[j];
                    soorten[j] = h;
                }
                CompoundTag t = new CompoundTag();
                t.putIntArray("Soorten", soorten);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                int[] soorten = reeks(opgave, "Soorten", KNABBELS), bakken = reeks(resultaat, "Bakken", KNABBELS);
                return soorten != null && bakken != null && java.util.Arrays.equals(soorten, bakken);
            }
        });
        TaakSoorten.registreer(new Spel(DROMEN, 170) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Wacht", 170);
                t.putBoolean("Upload", stap > 0);
                t.putInt("Droom", rng.nextInt(8));
                return t;
            }

            @Override
            public int minTicks(Balans balans) {
                return Math.max(1, 160 * balans.spelerTaakTijd / 100);
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Wacht");
            }
        });
        TaakSoorten.registreer(new Spel(WEGEN, 80) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Stil", 90);
                t.putInt("Gewicht", 3 + rng.nextInt(7));
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Stil") && resultaat.getIntOr("Stil", 0) >= opgave.getIntOr("Stil", 0);
            }
        });
        TaakSoorten.registreer(new Spel(SCHAKELAARS, 20) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                int alle = (1 << SCHAKELS) - 1, doel = rng.nextInt(alle + 1), begin;
                do {
                    begin = rng.nextInt(alle + 1);
                } while (Integer.bitCount(begin ^ doel) < 3);
                CompoundTag t = new CompoundTag();
                t.putInt("Begin", begin);
                t.putInt("Doel", doel);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Doel") && resultaat.contains("Stand") && resultaat.getIntOr("Stand", -1) == opgave.getIntOr("Doel", -2);
            }
        });
        TaakSoorten.registreer(new Herstel(TaakSoorten.HERSTEL_LICHT) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                int alle = (1 << LICHTEN) - 1, begin;
                do {
                    begin = rng.nextInt(alle);
                } while (Integer.bitCount(begin) > LICHTEN - 2);
                CompoundTag t = new CompoundTag();
                t.putInt("Begin", begin);
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Begin") && resultaat.getIntOr("Stand", 0) == (1 << LICHTEN) - 1;
            }
        });
        TaakSoorten.registreer(new Herstel(TaakSoorten.HERSTEL_ALARM) {
            @Override
            public CompoundTag opgave(Random rng, int stap) {
                CompoundTag t = new CompoundTag();
                t.putInt("Code", 1000 + rng.nextInt(9000));
                return t;
            }

            @Override
            public boolean geldig(CompoundTag opgave, CompoundTag resultaat) {
                return opgave.contains("Code") && resultaat.contains("Code") && resultaat.getIntOr("Code", -1) == opgave.getIntOr("Code", -2);
            }
        });
    }

    /** A repair job: a quick panel; the shortest time follows the round's repair time. */
    private abstract static class Herstel implements TaakSoorten.TaakSoort {
        private final String id;

        Herstel(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int duur(Balans balans) {
            return balans.herstelTijd;
        }

        @Override
        public int minTicks(Balans balans) {
            return Math.max(1, balans.herstelTijd / 3);
        }
    }

    /** A right answer to this opgave of this kind. */
    public static CompoundTag oplossing(String soort, CompoundTag opgave) {
        CompoundTag r = new CompoundTag();
        switch (soort) {
            case WORSTJES -> {
                int[] rechts = opgave.getIntArray("Rechts").orElse(new int[WORSTEN]), paren = new int[WORSTEN];
                for (int haak = 0; haak < rechts.length; haak++) {
                    paren[rechts[haak]] = haak;
                }
                r.putIntArray("Paren", paren);
            }
            case PASJE -> r.putInt("Ms", (opgave.getIntOr("Min", 0) + opgave.getIntOr("Max", 0)) / 2);
            case KRUIMELBAK -> r.putInt("Vast", opgave.getIntOr("Houd", 0));
            case PINDASAUS -> r.putInt("Peil", opgave.getIntOr("Doel", 0));
            case SORTEREN -> r.putIntArray("Bakken", opgave.getIntArray("Soorten").orElse(new int[KNABBELS]).clone());
            case WEGEN -> r.putInt("Stil", opgave.getIntOr("Stil", 0));
            case SCHAKELAARS -> r.putInt("Stand", opgave.getIntOr("Doel", 0));
            case TaakSoorten.HERSTEL_LICHT -> r.putInt("Stand", (1 << LICHTEN) - 1);
            case TaakSoorten.HERSTEL_ALARM -> r.putInt("Code", opgave.getIntOr("Code", 0));
            default -> {
            }
        }
        return r;
    }

    private Taken() {
    }
}
