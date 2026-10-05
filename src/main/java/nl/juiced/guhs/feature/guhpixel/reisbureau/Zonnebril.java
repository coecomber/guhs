package nl.juiced.guhs.feature.guhpixel.reisbureau;

import net.minecraft.nbt.CompoundTag;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.Klok;

/**
 * A guh that comes back from its trip wears sunglasses for a while ({@value #MINUTEN} real minutes): no unlock, just the
 * look. What it wore on its eyes before is remembered in its persistent data and put back afterwards. When the player
 * changes the eyes slot in the wardrobe meanwhile, that choice stays.
 */
public final class Zonnebril {
    public static final int MINUTEN = 20;
    public static final String TOT = "guhs_px_reisbureau_bril_tot", OUD = "guhs_px_reisbureau_bril_oud";

    /** Sunglasses on (a guh that wears its own sunglasses already just keeps them). */
    public static void zetOp(GuhEntity guh) {
        GuhClothes nu = guh.getClothes(GuhClothes.Slot.EYES);
        CompoundTag pd = guh.getPersistentData();
        if (nu == GuhClothes.SUNGLASSES && !pd.contains(TOT)) {
            return;
        }
        if (!pd.contains(TOT)) {
            pd.putString(OUD, nu == null ? "" : nu.name());
        }
        guh.wear(GuhClothes.SUNGLASSES);
        pd.putLong(TOT, Klok.nu() + MINUTEN * 60_000L);
    }

    public static boolean draagt(GuhEntity guh) {
        return guh.getPersistentData().contains(TOT);
    }

    /** (GuhHooks.tick) now and then: is the while over? */
    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 != 0 || guh.level().isClientSide()) {
            return;
        }
        CompoundTag pd = guh.getPersistentData();
        if (pd.contains(TOT) && Klok.nu() >= pd.getLongOr(TOT, 0L)) {
            zetAf(guh);
        }
    }

    /** Sunglasses off: back to what it wore before (unless the player dressed it differently meanwhile). */
    public static void zetAf(GuhEntity guh) {
        CompoundTag pd = guh.getPersistentData();
        if (!pd.contains(TOT)) {
            return;
        }
        String oud = pd.getStringOr(OUD, "");
        pd.remove(TOT);
        pd.remove(OUD);
        if (guh.getClothes(GuhClothes.Slot.EYES) != GuhClothes.SUNGLASSES) {
            return;
        }
        GuhClothes terug = null;
        for (GuhClothes c : GuhClothes.values()) {
            if (c.name().equals(oud) && c.slot == GuhClothes.Slot.EYES) {
                terug = c;
            }
        }
        if (terug != null) {
            guh.wear(terug);
        } else {
            guh.takeOff(GuhClothes.Slot.EYES);
        }
    }

    private Zonnebril() {
    }
}
