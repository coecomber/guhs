package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import javax.annotation.Nullable;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * X-ZONE1 server config ({@code serverconfig/guhs-vadsrijk-zones.toml} in the world, or {@code defaultconfigs/} for new worlds):
 * how many zones of Het Vadsrijk are open. {@code /guhs vadsrijk zones open <n>} sets it live and saves it.
 */
public final class ZonePoortConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue OPEN;
    /** Gametests / a server without the config loaded: overrides the config when not null. */
    @Nullable
    public static Integer TEST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        OPEN = b.comment("Het Vadsrijk: hoeveel zones er open zijn (1 = alleen Knuffelkamp + zone 1 Het Klodderdal, 6 = alles).",
                        "Alles voorbij deze zone is dicht voor spelers: geen schip, geen reis, geen quests, geen dungeon daar;",
                        "wie er toch komt, wordt vriendelijk teruggezet. Ops en creative mogen er altijd in (testen).",
                        "Tijdens het spelen aanpassen kan ook: /guhs vadsrijk zones open <n> (meteen actief, wordt bewaard).")
                .defineInRange("openZones", 6, 1, 6);
        SPEC = b.build();
    }

    private ZonePoortConfig() {
    }

    public static int open() {
        if (TEST != null) {
            return TEST;
        }
        try {
            return SPEC.isLoaded() ? OPEN.get() : 6;
        } catch (RuntimeException e) {
            return 6;
        }
    }

    /** Sets and saves (live: every check reads {@link #open()}). Without a loaded config only the in-memory value changes. */
    public static void zet(int n) {
        n = Math.max(1, Math.min(6, n));
        try {
            if (SPEC.isLoaded()) {
                OPEN.set(n);
                OPEN.save();
                TEST = null;
                return;
            }
        } catch (RuntimeException ignored) {
            // (fall through)
        }
        TEST = n;
    }
}
