package nl.juiced.guhs.feature.guhpixel.among.model;

/** The ten colours of the participants (a guh NPC is called by its colour; red is always a bit sus, njeg). */
public enum Kleur {
    ROOD("rood", 0xD8362F),
    BLAUW("blauw", 0x2F5BD8),
    GROEN("groen", 0x2FA84A),
    GEEL("geel", 0xF2D53C),
    ROZE("roze", 0xF28CC8),
    ORANJE("oranje", 0xF2902F),
    PAARS("paars", 0x8A45C9),
    WIT("wit", 0xEDEDF2),
    BRUIN("bruin", 0x8A5A33),
    MINT("mint", 0x7FE3C4);

    public final String id;
    public final int rgb;

    Kleur(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    public static Kleur op(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
