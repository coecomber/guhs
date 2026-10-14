package nl.juiced.guhs.feature.einde2.castschurk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;

/**
 * einde2, slice cast_schurk: the nine creatures of the Guhvatar story (CONTRACT_E2 11.6), one row per entity type: its hitbox,
 * how many looks ("Soort"), and its animations besides {@code idle} with their length in ticks (0: a pose or a loop that lasts
 * until another is set; the first is always {@code praat}). The models, animations and textures are written by
 * tools/features/einde2_cast_schurk_wezens.py, whose table WEZENS is the same (its check compares the two).
 */
public enum Wezens {
    DRAAKGUH(2.4f, 2.4f, 2, "praat:24", "vlieg_rond:0", "vuur:40", "buig:40", "slaap:0"),
    BOSCO(1.3f, 1.4f, 0, "praat:24", "loop:0", "zit:0", "eet:60", "brom:30"),
    WAN_SHI_GUH(2.2f, 3.6f, 0, "praat:24", "draai_kop:40", "spreid:0", "buig:40"),
    VOSGUH(0.6f, 0.7f, 0, "praat:24", "loop:0", "snuffel:40", "breng:30"),
    LEEUWSCHILDPAD_GUH(1.0f, 1.0f, 2, "praat:0", "ontwaak:60", "zegen:50"),
    FLOPSIE(1.6f, 2.4f, 0, "praat:24", "loop:0", "knuffel:40", "spring:30", "lik:30"),
    LUCHTBIZON_GUH(2.9f, 2.5f, 5, "praat:24", "walk:0", "vlieg:0", "brul:40"),
    MAANVIS(0.5f, 0.3f, 0, "praat:24", "zwem:0", "gloei:40", "gevangen:0"),
    OCEAANVIS(0.5f, 0.3f, 0, "praat:24", "zwem:0", "gloei:40");

    public final float breed, hoog;
    /** How many looks (NBT int Soort; 0: one look). */
    public final int soorten;
    private final List<String> animaties;
    private final int[] duren;

    Wezens(float breed, float hoog, int soorten, String... animaties) {
        this.breed = breed;
        this.hoog = hoog;
        this.soorten = soorten;
        this.animaties = Arrays.stream(animaties).map(a -> a.substring(0, a.indexOf(':'))).toList();
        this.duren = Arrays.stream(animaties).mapToInt(a -> Integer.parseInt(a.substring(a.indexOf(':') + 1))).toArray();
    }

    /** The entity id (and the name of its model, animation and texture files). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Its animations besides idle (praat first). */
    public List<String> animaties() {
        return animaties;
    }

    /** Can {@link CastWezen#zetAnimatie} play this name? */
    public boolean heeft(String animatie) {
        return animatie.equals("idle") || animaties.contains(animatie);
    }

    /** Ticks of a one-shot; 0 for a pose that lasts and for idle. */
    public int duur(String animatie) {
        int i = animaties.indexOf(animatie);
        return i < 0 ? 0 : duren[i];
    }

    /** Does this animation last until another one is set (idle, a pose, a walking or swimming loop)? */
    public boolean blijft(String animatie) {
        return animatie.equals("idle") || heeft(animatie) && duur(animatie) == 0;
    }

    /** The loop it plays while it moves (null: it just keeps its idle). */
    @Nullable
    public String beweegLoop() {
        if (animaties.contains("loop")) {
            return "loop";
        }
        if (animaties.contains("walk")) {
            return "walk";
        }
        return animaties.contains("zwem") ? "zwem" : null;
    }

    /** Its looks are textures of their own (&lt;id&gt;.png, &lt;id&gt;_1.png ...); the lion turtle's are poses (soort_&lt;n&gt;). */
    public boolean textuurPerSoort() {
        return soorten > 0 && this != LEEUWSCHILDPAD_GUH;
    }

    /** The texture of a look. */
    public String textuur(int soort) {
        return textuurPerSoort() && soort > 0 ? id() + "_" + soort : id();
    }

    @Nullable
    public static Wezens van(EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        for (Wezens w : values()) {
            if (w.id().equals(id)) {
                return w;
            }
        }
        return null;
    }
}
