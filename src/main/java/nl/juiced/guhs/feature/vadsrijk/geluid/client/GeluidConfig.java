package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * T-GELUID: the sliders (config/guhs-vadsrijk-geluid.toml; in game: Mods -> Guhs -> Config, every value 0-100 is a slider).
 * They stack ON TOP of Minecraft's own sliders: music, boss themes and stingers also follow "Muziek", the ambience follows
 * "Omgeving". 0 = off.
 */
public final class GeluidConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue GEBIEDSMUZIEK, BAZENMUZIEK, STINGERS, OMGEVING, PAUZE_MIN, PAUZE_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GEBIEDSMUZIEK = b.comment("Het Vadsrijk: zachte gebiedsmuziek buiten de steden (procent; 0 = uit, dan speelt er buiten de steden niets).",
                "Volgt ook de schuif Muziek van Minecraft.").defineInRange("gebiedsmuziek", 70, 0, 100);
        BAZENMUZIEK = b.comment("Het Vadsrijk: het thema van elke baas tijdens het gevecht (procent; 0 = uit).").defineInRange("bazenmuziek", 85, 0, 100);
        STINGERS = b.comment("Het Vadsrijk: korte muzikale 'ta-daa's: nieuw gebied, quest klaar, level omhoog, baas verslagen, geheim gevonden",
                "(procent; 0 = uit).").defineInRange("stingers", 80, 0, 100);
        OMGEVING = b.comment("Het Vadsrijk: omgevingsgeluid buiten de steden: vogels, water, belletjes, krekels 's nachts, druppels in grotten,",
                "bijen in Honingtak, golven en meeuwen aan de kust (procent; 0 = uit). Volgt ook de schuif Omgeving.").defineInRange("omgeving", 70, 0, 100);
        PAUZE_MIN = b.comment("Stilte tussen twee gebiedsstukken: minstens zoveel seconden (alleen omgevingsgeluid).").defineInRange("pauze_min", 60, 0, 240);
        PAUZE_MAX = b.comment("Stilte tussen twee gebiedsstukken: hoogstens zoveel seconden.").defineInRange("pauze_max", 180, 0, 240);
        SPEC = b.build();
    }

    private GeluidConfig() {
    }

    private static float procent(ModConfigSpec.IntValue v, int standaard) {
        return (SPEC.isLoaded() ? v.get() : standaard) / 100f;
    }

    public static float gebied() {
        return procent(GEBIEDSMUZIEK, 70);
    }

    public static float bazen() {
        return procent(BAZENMUZIEK, 85);
    }

    public static float stingers() {
        return procent(STINGERS, 80);
    }

    public static float omgeving() {
        return procent(OMGEVING, 70);
    }

    /** The silence after an area piece, in ticks (random between the two sliders). */
    public static int pauze(net.minecraft.util.RandomSource r) {
        int a = SPEC.isLoaded() ? PAUZE_MIN.get() : 60;
        int b = SPEC.isLoaded() ? PAUZE_MAX.get() : 180;
        if (b < a) {
            int t = a;
            a = b;
            b = t;
        }
        return (a + (b > a ? r.nextInt(b - a + 1) : 0)) * 20;
    }
}
