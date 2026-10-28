package nl.juiced.guhs.feature.fossiel;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhVariant;

/** 1.8.0: the four oerguhs and their fossils ({@code guhs:fossiel_<id>}): which fossil hatches which guh variant. */
public enum OerSoort {
    MAMMOET("mammoet", GuhVariant.MAMMOETGUH),
    TGUH("tguh", GuhVariant.TGUH),
    PTERO("pteroguh", GuhVariant.PTEROGUH),
    STOKSTAARTJE("stokstaartje", GuhVariant.STOKSTAARTJEGUH);

    public final String id;
    public final GuhVariant variant;

    OerSoort(String id, GuhVariant variant) {
        this.id = id;
        this.variant = variant;
    }

    /** The item id path of its fossil. */
    public String fossiel() {
        return "fossiel_" + id;
    }

    /** The oerguh this fossil hatches, or null when the stack is no fossil. */
    @Nullable
    public static OerSoort van(ItemStack stack) {
        for (OerSoort s : values()) {
            if (!stack.isEmpty() && stack.is(FossielFeature.FOSSIELEN.get(s).get())) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static OerSoort byId(String id) {
        for (OerSoort s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static OerSoort van(GuhVariant variant) {
        for (OerSoort s : values()) {
            if (s.variant == variant) {
                return s;
            }
        }
        return null;
    }
}
