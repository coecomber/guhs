package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config of the Vadsrijk atmosphere (config/guhs-vadsrijk-sfeer.toml): tone it down or switch it off. */
public final class SfeerConfig {
    public enum Niveau {
        VOL, ZACHT, UIT
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Niveau> NIVEAU;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        NIVEAU = b.comment("Het Vadsrijk: mist en licht per gebied, de lichtgolf van Zwamzoet, de sporenstorm, roze buitjes met regenboog,",
                        "het noorderlicht en de effecten van de overvliegende draak.",
                        "VOL = alles, ZACHT = minder deeltjes en zachtere mist, UIT = niets van dit alles (alleen de gewone lucht).")
                .defineEnum("sfeer", Niveau.VOL);
        SPEC = b.build();
    }

    static Niveau niveau() {
        return SPEC.isLoaded() ? NIVEAU.get() : Niveau.VOL;
    }

    static boolean aan() {
        return niveau() != Niveau.UIT;
    }

    /** 1 at VOL, 0.45 at ZACHT, 0 off: scales particle counts and the fog strength. */
    static float sterkte() {
        return switch (niveau()) {
            case VOL -> 1f;
            case ZACHT -> 0.45f;
            case UIT -> 0f;
        };
    }

    private SfeerConfig() {
    }
}
