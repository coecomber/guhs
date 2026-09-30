package nl.juiced.guhs.feature.kapper;

import java.util.Locale;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhClothes;

/**
 * The eight hairstyles of Kapper Krulletje (GuhClothes.Slot.HAAR, bones {@code outfit_haar_<stijl>*}). A hairstyle is
 * permanent: the kappersshow's customers get one, and your own tamed guh gets one from a kapsel item (see
 * {@link KapperHaar}). The collection entry (Knus tab, "kapsels") and the item are both called {@code kapsel_<stijl>}.
 */
public enum Kapsel {
    KRULLEN(GuhClothes.KAPSEL_KRULLEN),
    KUIFJE(GuhClothes.KAPSEL_KUIFJE),
    KNOTJES(GuhClothes.KAPSEL_KNOTJES),
    STRIKJES(GuhClothes.KAPSEL_STRIKJES),
    PLUISBOL(GuhClothes.KAPSEL_PLUISBOL),
    VLECHTJES(GuhClothes.KAPSEL_VLECHTJES),
    HANENKAM(GuhClothes.KAPSEL_HANENKAM),
    MATJE(GuhClothes.KAPSEL_MATJE);

    public final GuhClothes kleding;

    Kapsel(GuhClothes kleding) {
        this.kleding = kleding;
    }

    /** "kapsel_krullen": the item id and the collection entry. */
    public String id() {
        return kleding.id();
    }

    /** "krullen" (lang: gui.guhs.kapper.kapsel.&lt;stijl&gt;). */
    public String stijl() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Kapsel van(@Nullable GuhClothes kleding) {
        for (Kapsel k : values()) {
            if (k.kleding == kleding) {
                return k;
            }
        }
        return null;
    }

    @Nullable
    public static Kapsel byId(String id) {
        for (Kapsel k : values()) {
            if (k.id().equals(id) || k.stijl().equals(id)) {
                return k;
            }
        }
        return null;
    }
}
