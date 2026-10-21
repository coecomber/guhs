package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;

/**
 * One setting of a tool (shown by the settings screen, the radial menu and the keet's Vormen/Daken tabs, all from the same
 * description): a choice, a number, on/off, one block, a mix of blocks or a text. The label is
 * {@code gui.guhs.bouwhulp.optie.<key>}, a choice's value {@code gui.guhs.bouwhulp.keuze.<key>.<id>}.
 */
public record Optie(String key, Soort soort, int min, Function<BouwNiveau, Integer> max, int standaard, List<String> keuzes,
                    Predicate<CompoundTag> zichtbaar, BiFunction<Ctx, Integer, String> dicht) {
    public enum Soort { KEUZE, GETAL, AAN_UIT, BLOK, MIX, TEKST }

    /** Who asks, at what level (for the limits and the locked choices). */
    public record Ctx(@Nullable ServerPlayer speler, BouwNiveau niveau) {
    }

    private static final Predicate<CompoundTag> ALTIJD = t -> true;
    private static final BiFunction<Ctx, Integer, String> OPEN = (c, i) -> null;

    public static Optie keuze(String key, List<String> keuzes, int standaard) {
        return new Optie(key, Soort.KEUZE, 0, n -> keuzes.size() - 1, standaard, keuzes, ALTIJD, OPEN);
    }

    public static Optie getal(String key, int min, int max, int standaard) {
        return new Optie(key, Soort.GETAL, min, n -> max, standaard, List.of(), ALTIJD, OPEN);
    }

    public static Optie getal(String key, int min, Function<BouwNiveau, Integer> max, int standaard) {
        return new Optie(key, Soort.GETAL, min, max, standaard, List.of(), ALTIJD, OPEN);
    }

    public static Optie aanUit(String key, boolean standaard) {
        return new Optie(key, Soort.AAN_UIT, 0, n -> 1, standaard ? 1 : 0, List.of(), ALTIJD, OPEN);
    }

    public static Optie blok(String key) {
        return new Optie(key, Soort.BLOK, 0, n -> 0, 0, List.of(), ALTIJD, OPEN);
    }

    public static Optie mix(String key) {
        return new Optie(key, Soort.MIX, 0, n -> 0, 0, List.of(), ALTIJD, OPEN);
    }

    public static Optie tekst(String key, String standaard) {
        return new Optie(key, Soort.TEKST, 0, n -> 24, 0, List.of(standaard), ALTIJD, OPEN);
    }

    /** Shown only when this holds for the current settings. */
    public Optie als(Predicate<CompoundTag> p) {
        return new Optie(key, soort, min, max, standaard, keuzes, p, dicht);
    }

    /** A choice that may be locked (the function gives the reason's text key, or null when it is open). */
    public Optie dicht(BiFunction<Ctx, Integer, String> d) {
        return new Optie(key, soort, min, max, standaard, keuzes, zichtbaar, d);
    }

    /** The current value of a number/choice/on-off setting in a settings tag. */
    public int waarde(CompoundTag t) {
        return t.getIntOr(key, standaard);
    }

    /** Is choice i locked now (its reason, or null)? */
    @Nullable
    public String waarom(Ctx c, int i) {
        return dicht.apply(c, i);
    }
}
