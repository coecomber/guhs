package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;

/**
 * einde2, slice geestmodellen: the creatures of the Geestenwereld (DESIGN_EINDE2 5 "De Geestenwereld"), one row per entity
 * type: its hitbox, how many looks ({@code Soort}), whether a look is a texture of its own, whether it is drawn see-through
 * and with a glow mask, and its animations besides {@code idle} with their length in ticks (0: a pose or a loop that lasts
 * until another is set; the first is always {@code praat}). The models, animations and textures are written by
 * tools/features/einde2_geestmodellen_wezens.py, whose table WEZENS is the same (its check compares the two).
 */
public enum GeestWezens {
    /** Koh de Gezichtenrover: a giant centipede spirit, about 12 blocks long; Soort = the face he wears. */
    KOH(2.6f, 3.6f, 6, true, false, true, "praat:24", "kruip:0", "wissel:20", "steel:60"),
    /** Hei Bai-guh: Soort 0 the little panda spirit, 1 the angry giant (its own hitbox: {@link HeiBaiGuhEntity#GROOT}). */
    HEI_BAI_GUH(0.8f, 1.0f, 2, false, false, true, "praat:24", "groei:40", "kalm:40"),
    /** Fang, the spirit dragon of Roku-guh: red, see-through, glowing; a seat on his neck (bone zadel). */
    FANG(3.0f, 2.4f, 0, false, true, true, "praat:24", "vlieg:0", "buig:40"),
    /** Tui (Soort 0, white with a black spot) and La (Soort 1, black with a white spot): huge see-through koi that swim in the air. */
    GEEST_KOI(2.0f, 1.6f, 2, true, true, true, "praat:24", "zwem:0"),
    /** The libelkonijnguh: a tiny bunny with the guh face and four dragonfly wings; curious, it follows you and darts off. */
    LIBELKONIJNGUH(0.4f, 0.4f, 0, false, true, true, "praat:16", "vlieg:0", "schiet_weg:16"),
    /** A lantaarngeest: a floating paper lantern with a painted face; Soort 0..3 = red, amber, pink, blue. */
    LANTAARNGEEST(0.7f, 0.9f, 4, true, false, true, "praat:24", "knipoog:20"),
    /** A schaduwgeest: a dark, smoky, tall shape with two white eyes; it only ever shoves. */
    SCHADUWGEEST(0.8f, 2.6f, 0, false, true, true, "praat:24", "duw:16"),
    // phase B (CONTRACT_E2_B 7.7): the new creatures of the White Lotus and the mysteries
    /** A kennisvosje: a spirit fox with guh ears and a scroll; Soort 0 silver-blue, 1 the rare golden one. Leads to a secret. */
    KENNISVOSJE(0.6f, 0.7f, 2, true, true, true, "praat:24", "loop:0", "wijs:40", "snuffel:30"),
    /** A mistreiger: a small white heron on the roofs; it flies up when you come close. */
    MISTREIGER(0.5f, 0.9f, 0, false, false, true, "praat:24", "vlieg:0", "opvlieg:20", "poets:40"),
    /** The Oude Reigerguh: a huge see-through crane spirit with guh ears, asleep round the stone lotus. */
    OUDE_REIGERGUH(3.5f, 3.0f, 0, false, true, true, "praat:24", "slaap:0", "ontwaak:80", "spreek:0"),
    /** Wan-guh: the hooded figure in the distance (no page). */
    WAN_GUH(0.8f, 2.0f, 0, false, true, true, "praat:24", "verdwijn:30", "ontkap:0", "wijs:40"),
    /** A geestreus: a giant silhouette, only ever far away; Soort 0..3 = four forms (bones vorm_0..3). No page. */
    GEESTREUS(2.0f, 6.0f, 4, false, true, true, "praat:24", "verschijn:60", "drijf:0"),
    /** A kijkmasker: a mask of Koh's cave that slowly turns its face to you; Soort 0..11 = the faces of the geestenmasker. No page. */
    KIJKMASKER(0.8f, 0.9f, 12, true, false, true, "praat:24", "knipper:16");

    public final float breed, hoog;
    /** How many looks (NBT int Soort; 0: one look). */
    public final int soorten;
    /** A look is a texture of its own (&lt;id&gt;.png, &lt;id&gt;_1.png ...); else the looks are bones the renderer shows or hides. */
    public final boolean textuurPerSoort;
    /** Drawn translucent (the texture's alpha counts). */
    public final boolean doorzichtig;
    /** Has a glow mask (&lt;texture&gt;_glowmask.png for every texture), drawn full bright on top. */
    public final boolean gloed;
    private final List<String> animaties;
    private final int[] duren;

    GeestWezens(float breed, float hoog, int soorten, boolean textuurPerSoort, boolean doorzichtig, boolean gloed, String... animaties) {
        this.breed = breed;
        this.hoog = hoog;
        this.soorten = soorten;
        this.textuurPerSoort = textuurPerSoort;
        this.doorzichtig = doorzichtig;
        this.gloed = gloed;
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

    /** Can {@link GeestWezen#zetAnimatie} play this name? */
    public boolean heeft(String animatie) {
        return animatie.equals("idle") || animaties.contains(animatie);
    }

    /** Ticks of a one-shot; 0 for a pose that lasts and for idle. */
    public int duur(String animatie) {
        int i = animaties.indexOf(animatie);
        return i < 0 ? 0 : duren[i];
    }

    /** Does this animation last until another one is set (idle, a pose, a flying or swimming loop)? */
    public boolean blijft(String animatie) {
        return animatie.equals("idle") || heeft(animatie) && duur(animatie) == 0;
    }

    /** The loop it plays while it moves (null: it just keeps its idle). */
    @Nullable
    public String beweegLoop() {
        if (animaties.contains("vlieg")) {
            return "vlieg";
        }
        if (animaties.contains("loop")) {
            return "loop";
        }
        return animaties.contains("zwem") ? "zwem" : null;
    }

    /** The texture of a look. */
    public String textuur(int soort) {
        return textuurPerSoort && soort > 0 ? id() + "_" + soort : id();
    }

    @Nullable
    public static GeestWezens van(EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        for (GeestWezens w : values()) {
            if (w.id().equals(id)) {
                return w;
            }
        }
        return null;
    }
}
