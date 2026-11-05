package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config of the VFX kit (config/guhs-vadsrijk-vfx.toml, like f-sfeer's slider): VOL / ZACHT / UIT for all new effects.
 * Telegraph decals are gameplay and always show (at UIT plain: no glow pulse, no sparkles). The screen shake has its own slider
 * (beleving: "schermschud").
 */
public final class VfxConfig {
    public enum Niveau {
        VOL, ZACHT, UIT
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Niveau> NIVEAU;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        NIVEAU = b.comment("Het Vadsrijk: de effecten van bazen en gevechten (eigen deeltjes, schokgolven, stralen, flitsen, sporen, lichtzuilen).",
                        "VOL = alles (max 700 deeltjes), ZACHT = minder (max 250, korter), UIT = geen effecten.",
                        "De telegraphs op de grond (cirkels, kegels, lijnen) blijven ALTIJD zichtbaar: die heb je nodig om te ontwijken.")
                .translation("guhs.configuration.vadsrijkVfx")
                .defineEnum("vfx", Niveau.VOL);
        SPEC = b.build();
    }

    private VfxConfig() {
    }

    /** A dev override from {@code /guhs vadsrijk vfx niveau} until the config is loaded (or if saving fails). */
    static Niveau override;

    public static Niveau niveau() {
        if (override != null) {
            return override;
        }
        return SPEC.isLoaded() ? NIVEAU.get() : Niveau.VOL;
    }

    static void zet(Niveau n) {
        override = n;
        try {
            if (SPEC.isLoaded()) {
                NIVEAU.set(n);
                NIVEAU.save();
                override = null;
            }
        } catch (RuntimeException e) {
            // keep the override for this session
        }
    }

    /** The particle budget (max alive at once, oldest go first). */
    static int budget() {
        return switch (niveau()) {
            case VOL -> 700;
            case ZACHT -> 250;
            case UIT -> 0;
        };
    }

    /** Scales spawn counts. */
    static float dichtheid() {
        return switch (niveau()) {
            case VOL -> 1f;
            case ZACHT -> 0.4f;
            case UIT -> 0f;
        };
    }

    static boolean cosmetisch() {
        return niveau() != Niveau.UIT;
    }
}
