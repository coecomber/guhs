package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;

/**
 * U-GILDEN: the four ranks of a Knuffelbond and what each may do ({@link Recht}). A rank may only act on LOWER ranks
 * (kick, promote, demote); promoting goes at most to one below your own rank (only the leader makes officers).
 */
public enum Rang {
    LEIDER("leider", ChatFormatting.GOLD, EnumSet.allOf(Recht.class)),
    OFFICIER("officier", ChatFormatting.LIGHT_PURPLE, EnumSet.of(Recht.UITNODIGEN, Recht.VERWIJDEREN, Recht.BEVORDEREN, Recht.BANK_OPNEMEN,
            Recht.BANK_ALLES, Recht.KASTEEL, Recht.WERVEN)),
    LID("lid", ChatFormatting.AQUA, EnumSet.of(Recht.UITNODIGEN, Recht.BANK_OPNEMEN)),
    REKRUUT("rekruut", ChatFormatting.GRAY, EnumSet.noneOf(Recht.class));

    /** What a rank may do. Everybody may chat, deposit in the bank, donate to the castle and use the teleport stone. */
    public enum Recht {
        /** invite a character (the new one starts as rekruut) */
        UITNODIGEN,
        /** remove a member of a lower rank */
        VERWIJDEREN,
        /** promote/demote lower ranks (to at most one below your own) */
        BEVORDEREN,
        /** take things out of bank tab 1 */
        BANK_OPNEMEN,
        /** take things out of every bank tab */
        BANK_ALLES,
        /** build the next castle level (spends the kas + the depot) */
        KASTEEL,
        /** recruiting settings + accept/refuse applications */
        WERVEN,
        /** banner + colours */
        BANIER,
        /** hand over the leadership, disband */
        OPHEFFEN
    }

    public final String id;
    public final ChatFormatting kleur;
    private final Set<Recht> rechten;

    Rang(String id, ChatFormatting kleur, Set<Recht> rechten) {
        this.id = id;
        this.kleur = kleur;
        this.rechten = rechten;
    }

    public boolean mag(Recht r) {
        return rechten.contains(r);
    }

    /** Higher than {@code ander} (LEIDER is the highest). */
    public boolean boven(Rang ander) {
        return ordinal() < ander.ordinal();
    }

    public String naamKey() {
        return "vadsrijk.gilden.rang." + id;
    }

    @Nullable
    public Rang hoger() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    @Nullable
    public Rang lager() {
        return ordinal() == values().length - 1 ? null : values()[ordinal() + 1];
    }

    public static Rang van(String id) {
        for (Rang r : values()) {
            if (r.id.equals(id.toLowerCase(Locale.ROOT))) {
                return r;
            }
        }
        return REKRUUT;
    }
}
