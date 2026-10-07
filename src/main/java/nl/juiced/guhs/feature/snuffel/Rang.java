package nl.juiced.guhs.feature.snuffel;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The five sniffing ranks, by the number of scents a player has learned (0 / 20 / 50 / 100 / 150). The rank decides what
 * the nose can smell: a scent of rank n is only smelled by a dog of rank n or higher ({@link Geur#rang}).
 * <p>
 * Only the first rank is reachable in this update ({@link #HOOGSTE_NU}): whoever would have enough scents for more stays
 * a Snuffelpup until the story goes on. The later update raises that one constant.
 */
public enum Rang {
    SNUFFELPUP(0),
    SNUFFELNEUS(20),
    SNUFFELSPEURDER(50),
    SNUFFELMEESTER(100),
    OPPER_SNUFFELMEESTER(150);

    /** The highest rank a player can have now. */
    public static final Rang HOOGSTE_NU = SNUFFELPUP;

    private final int drempel;

    Rang(int drempel) {
        this.drempel = drempel;
    }

    /** How many learned scents this rank asks. */
    public int drempel() {
        return drempel;
    }

    /** 1 (the lowest) .. 5 (the highest). */
    public int nummer() {
        return ordinal() + 1;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Can a player reach this rank in this update? */
    public boolean bereikbaar() {
        return ordinal() <= HOOGSTE_NU.ordinal();
    }

    public Component naam() {
        return Component.translatable("gui.guhs.snuffel.rang." + id());
    }

    /** "Snuffelpup (rang 1 (laagste) van 5 (hoogste))". */
    public Component regel() {
        String key = this == SNUFFELPUP ? "gui.guhs.snuffel.rang.regel.laagste" : this == OPPER_SNUFFELMEESTER ? "gui.guhs.snuffel.rang.regel.hoogste"
                : "gui.guhs.snuffel.rang.regel";
        return Component.translatable(key, naam(), nummer(), values().length);
    }

    /** One line of the list of all five: "2. Snuffelneus: vanaf 20 geuren". */
    public Component lijstRegel() {
        return Component.translatable(bereikbaar() ? "gui.guhs.snuffel.rang.lijst" : "gui.guhs.snuffel.rang.lijst_later", nummer(), naam(), drempel);
    }

    /** The rank that goes with this many learned scents (never above {@link #HOOGSTE_NU}). */
    public static Rang van(int geuren) {
        Rang r = SNUFFELPUP;
        for (Rang x : values()) {
            if (geuren >= x.drempel && x.bereikbaar()) {
                r = x;
            }
        }
        return r;
    }

    public static Rang van(ServerPlayer p) {
        return van(Geuren.aantal(p));
    }

    public static Rang metNummer(int nummer) {
        return values()[Math.max(1, Math.min(values().length, nummer)) - 1];
    }
}
