package nl.juiced.guhs.feature.knus;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import nl.juiced.guhs.Guhs;

/**
 * The six "feesttaakjes" of the Grote Knusfeest (2.8): Burgemeester Vadsema asks them, one per building/feature
 * ({@link #pkg} owns the activity and the feest-item), the player brings the item (item tag {@code guhs:knus/<id>}).
 * See {@link Knusfeest} for the flow.
 */
public enum Feesttaak {
    FEESTTAART("bakkerij"),
    THEESERVIES("theehuis"),
    FEESTKAPSELS("kapper"),
    FEESTSLINGERS("creche"),
    FEESTBLOEMEN("tuintjes"),
    STERRENLANTAARNS("sterrenwacht");

    /** The package (slice) that offers the activity and gives the item. */
    public final String pkg;
    private final TagKey<Item> tag;

    Feesttaak(String pkg) {
        this.pkg = pkg;
        this.tag = TagKey.create(Registries.ITEM, Guhs.id("knus/" + name().toLowerCase(Locale.ROOT)));
    }

    /** Lower case, e.g. "feesttaart". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** guhs:knus/&lt;id&gt;: what the player brings to the Burgemeester. */
    public TagKey<Item> tag() {
        return tag;
    }

    /** Can a Kruimel-Mika steal this one on the way (the cake, the tea set and the garlands)? */
    public boolean steelbaar() {
        return this == FEESTTAART || this == THEESERVIES || this == FEESTSLINGERS;
    }

    /** lang gui.guhs.knusfeest.taak.&lt;id&gt;. */
    public Component naam() {
        return Component.translatable("gui.guhs.knusfeest.taak." + id());
    }

    /** Is this stack one of the six feest-items (for the Burgemeester: never food for a buffet, a tea table or a Mika)? */
    public static boolean isFeestItem(net.minecraft.world.item.ItemStack stack) {
        for (Feesttaak t : values()) {
            if (stack.is(t.tag)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Feesttaak byId(String id) {
        for (Feesttaak t : values()) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }
}
