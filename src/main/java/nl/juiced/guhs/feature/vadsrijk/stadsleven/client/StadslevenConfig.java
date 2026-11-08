package nl.juiced.guhs.feature.vadsrijk.stadsleven.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** U-LEVEN-STAD client config (config/guhs-vadsrijk-stadsleven.toml): how much town life you see. */
public final class StadslevenConfig {
    public enum Niveau {
        VOL, ZACHT, UIT
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Niveau> NIVEAU;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        NIVEAU = b.comment("Het Vadsrijk: leven in de guh-stadjes (muisjes, blauwe klodders, katten, eendjes, vogeltjes, wandelende guhs met",
                        "hun huisdier, muzikanten, de ijskar, picknicks, tikkertje, de speeltuin, kletsende guhs op terrasjes).",
                        "VOL = alles, ZACHT = de helft (en alleen dichtbij), UIT = niets (scheelt FPS op een trage computer).")
                .defineEnum("stadsleven", Niveau.VOL);
        SPEC = b.build();
    }

    public static Niveau niveau() {
        try {
            return SPEC.isLoaded() ? NIVEAU.get() : Niveau.VOL;
        } catch (RuntimeException e) {
            return Niveau.VOL;
        }
    }

    private StadslevenConfig() {
    }
}
