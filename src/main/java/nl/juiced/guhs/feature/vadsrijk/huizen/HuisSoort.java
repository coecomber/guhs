package nl.juiced.guhs.feature.vadsrijk.huizen;

import javax.annotation.Nullable;

/**
 * U-HUIZEN: the three house sizes. Price in brons (100 brons = 1 zilver, 100 zilver = 1 goud), the character level you need, the
 * footprint (width across the street x depth), floors and how many deco blocks the owner may place (house + garden).
 */
public enum HuisSoort {
    HUISJE(1500, 10, 7, 7, 1, 150),
    HUIS(6000, 25, 9, 9, 2, 300),
    VILLA(25000, 45, 13, 10, 2, 500);

    public final long prijs;
    public final int level, breed, diep, vloeren, maxDeco;

    HuisSoort(long prijs, int level, int breed, int diep, int vloeren, int maxDeco) {
        this.prijs = prijs;
        this.level = level;
        this.breed = breed;
        this.diep = diep;
        this.vloeren = vloeren;
        this.maxDeco = maxDeco;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String naamKey() {
        return "vadsrijk.huizen.soort." + id();
    }

    /** The first column of the house (u). */
    public int u0() {
        return (WijkPlan.KAVEL - breed) / 2;
    }

    public int u1() {
        return u0() + breed - 1;
    }

    /** The front wall (v) and the back wall. */
    public int v0() {
        return 4;
    }

    public int v1() {
        return v0() + diep - 1;
    }

    /** The highest wall block (y); the roof starts one above. */
    public int muurTop() {
        return WijkPlan.VLOER + 4 * vloeren - 1;
    }

    @Nullable
    public HuisSoort groter() {
        return this == HUISJE ? HUIS : this == HUIS ? VILLA : null;
    }

    @Nullable
    public static HuisSoort van(String id) {
        for (HuisSoort s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
