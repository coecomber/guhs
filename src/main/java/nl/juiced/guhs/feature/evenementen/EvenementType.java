package nl.juiced.guhs.feature.evenementen;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;

/** The three kinds of guh events in the Guhmension, with their boss bar colour and how often the scheduler picks them. */
public enum EvenementType {
    /** About a minute of kaasknabbels (now and then a golden one) falling around you. */
    KAASREGEN(BossEvent.BossBarColor.YELLOW, ChatFormatting.GOLD, 40),
    /** The Vadsparade: a drummer guh and ten dressed-up guhs marching along a route, with music and confetti at the end. */
    PARADE(BossEvent.BossBarColor.PINK, ChatFormatting.LIGHT_PURPLE, 35),
    /** Falling stars at night; where one lands a starry guh turns up for a little while. */
    STERRENREGEN(BossEvent.BossBarColor.PURPLE, ChatFormatting.AQUA, 35),
    /**
     * The seasonal Knusfeest (2.8, knuffeldal feature: KnusfeestEvenement): once per season, after the Grote Knusfeest,
     * the Burgemeester asks a few feesttaakjes again; bringing them all starts this feast at the feestbuffet. Never picked
     * by the random scheduler (weight 0, {@link #possible} is false for it).
     */
    KNUSFEEST(BossEvent.BossBarColor.PINK, ChatFormatting.LIGHT_PURPLE, 0);

    /** The kinds the scheduler picks at random (and that count for "alle evenementen"). */
    public static final java.util.List<EvenementType> RANDOM = java.util.List.of(KAASREGEN, PARADE, STERRENREGEN);

    public final BossEvent.BossBarColor bar;
    public final ChatFormatting colour;
    /** Scheduler weight (sterrenregen only counts at night). */
    public final int weight;

    EvenementType(BossEvent.BossBarColor bar, ChatFormatting colour, int weight) {
        this.bar = bar;
        this.colour = colour;
        this.weight = weight;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("gui.guhs.evenement." + id());
    }

    @Nullable
    public static EvenementType byId(String id) {
        for (EvenementType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /** Can this kind happen now? Falling stars only at night. */
    public boolean possible(boolean night) {
        return this != KNUSFEEST && (this != STERRENREGEN || night);
    }

    /**
     * A fair random pick for the scheduler: by weight among the kinds that can happen now, and never the same kind twice
     * in a row for the same player (when there's a choice).
     */
    public static EvenementType choose(RandomSource random, boolean night, @Nullable EvenementType last) {
        int total = 0;
        for (EvenementType type : values()) {
            if (type.possible(night) && type != last) {
                total += type.weight;
            }
        }
        int roll = random.nextInt(total);
        for (EvenementType type : values()) {
            if (type.possible(night) && type != last) {
                roll -= type.weight;
                if (roll < 0) {
                    return type;
                }
            }
        }
        return KAASREGEN;
    }
}
