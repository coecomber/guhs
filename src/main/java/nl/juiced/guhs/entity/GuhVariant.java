package nl.juiced.guhs.entity;

import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.Guhs;

/**
 * Guh variants: other fur colours and shapes (the brontosaurus guh). Clothes are separate (GuhClothes). Only wild guhs born in the Guhmension can be a
 * variant (and most of them are still normal guhs). The textures are made by tools/make_guh_variants.py, which also
 * adds the clothing / neck bones to the guh model; {@link #bones} says which of those bones a variant shows.
 */
public enum GuhVariant {
    NORMAL(0),
    // 1.3.1: the three common colours are a third rarer (were 25, 25 and 20 out of 1000); the rest was under 1% already
    MINT(17),
    CHOCO(17),
    SNOW(13),
    BRONTOSAURUS(6, "neck"),
    GOLDEN(2),
    /** Its fur slowly runs through all the colours. */
    RAINBOW(3),
    /** Dark purple with twinkling stars that glow in the dark. 1.2.5: only from the Sterrenregen event (Sterrenregen), never rolled here. */
    STARRY(0),
    /** See-through; only comes out at night (and vanishes at sunrise, unless tamed). */
    GHOST(4),
    /** An extra long body with six legs. */
    TECKEL(5, "teckel"),
    /** The guh with the secret note (never rolled here, see GuhEntity.SECRET_NOTE_CHANCE); it wears the pink onesie. */
    BROCOCOLIEF(0),
    /** Black and purple with dragon wings: flies (and you can fly on it). 1.2.8: only wild in the Guheinde, around players who beat Opper-Mika (feature/guheinde/Enderguhs). */
    ENDER(0, "ender"),
    /** The Koningguh: royal purple, a white mane, a moustache and golden socks. One on the throne of every guh castle. */
    KONING(0, "koning"),
    /** The Wolkguh: white and fluffy with a sky-blue shine, a little cloud on its head and a cloud tail. Only one, on the floating guh islands (eilanden feature). */
    WOLK(0, "wolk"),
    /** The Zeemeerguh: a guh with a fish tail. Rare, only in the guh sea (onderwater feature: Zeemeerguh); swims fast, ridden under water. */
    ZEEMEERGUH(0, "zeemeer"),
    /** A starved, grey guh from the cells of the Knabbelkelder (Guheinde): feed it a kaasknabbel and it gets its colour back. */
    MAGER(0),
    /** The Vahoege Enderguh: the freed Enderguh of the Guheinde (and its babies from an Enderguh-ei): flies like the ender guh, in gold and pink. */
    VAHOEGE_ENDER(0, "ender"),
    /** De Kaasmoerasguh: groen-geel gevlekt. Only wild guhs born in the kaasmoeras (kaasmoeras feature: KaasmoerasEvents). */
    KAASMOERASGUH(0),
    /** De Asguh: grey and sooty with glowing cheeks, fire can't hurt it. Only born in the Asdal of the Barbecuether (spiesburcht feature). */
    ASGUH(0, "asguh"),
    // <bio_dieren>
    // biomes3: de Bloesemguh (petals in its fur, a blossom by its ear; only born in the Bloesemmeertje) and de Tanukiguh (a ringed
    // bushy tail, a dark mask, a leaf on its head; only born in the Klaterdal): feature/bio/dieren/BiomeGuhs
    BLOESEMGUH(0, "bloesem"), TANUKIGUH(0, "tanuki"),
    // </bio_dieren>
    /** De Pluisguh (2.8): extra fluffy and pink, with a fluffy tuft and cheek fluff. Only born in the Knuffeldal (knuffeldal feature). */
    PLUISGUH(0, "pluis"),
    /** De Pinguh (2.9): klassiek or keizer look, belly-slides on ice. Only born in the Guhpolder (elftocht slice: guhpolder feature). */
    PINGUH(0, "pinguh"),
    /**
     * Guhdex pages for guh characters that aren't variants of the guh itself (seen, never tamed): each has the name of
     * its GuhNpcEntity.Kind. Everything from REISGUH on is a character.
     */
    REISGUH(0),
    POORTWACHTER(0),
    SHOWGUH(0),
    RACEGUH(0),
    MEPGUH(0),
    DJGUH(0),
    GOLFGUH(0),
    SMULGUH(0),
    VISGUH(0),
    MIJNGUH(0),
    BIBLIOTHECARIS(0),
    // --- 2.7: Guhdex pages of the kaasmoeras creatures (no NPC kind: their id is the entity id, see npcKind) ---
    KIKKERGUH(0),
    KAASMOT(0),
    MOERASHEKS_MIKA(0),
    BOSWACHTERGUH(0),
    KNABBELPLUKKER(0),
    GRILLGUH(0),
    // --- 2.8 (Knuffeldal): the characters (NPC kinds) and creatures (KRUIMEL_MIKA, GUHSCHAAPJE, KNABBELKIPPETJE, GUHKOE,
    // IJSCOGUH: their id is their entity id) ---
    BURGEMEESTERGUH(0),
    KRUIMEL_MIKA(0),
    BAKKERGUH(0),
    JUF_KNUFFEL(0),
    THEEGUH(0),
    KAPPERGUH(0),
    BOERINNEGUH(0),
    GUHSCHAAPJE(0),
    KNABBELKIPPETJE(0),
    GUHKOE(0),
    STERRENKIJKERGUH(0),
    BALLONGUH(0),
    OPA_GUH(0),
    BADMEESTERGUH(0),
    IJSCOGUH(0),
    COCOTJE(0),
    // --- 2.9 (De Grote Guhspelen): the characters, in GuhNpcEntity.Kind order ---
    SJOELGUH(0),
    DOOLHOFGUH(0),
    KATAPULTGUH(0),
    SPELLEIDERGUH(0),
    SCHAATSMEESTERGUH(0),
    STEMPELGUH(0),
    CIRCUITGUH(0),
    BRANDWEERGUH(0),
    POLITIEGUH(0),
    APOTHEKERGUH(0),
    BOUWVAKKERGUH(0),
    // --- 2.10.1: the Rookguh of the Barbecuether (a creature page: its id is its entity id) with your saved-Rookguh count ---
    ROOKGUH(0),
    // --- 3.0 (Guhverhalen): the story guhs (tameable once per player, own fur: isVerhaalGuh, not characters) ---
    BALTOGUH(0, "balto"), MEWTWO(0, "mewtwo"), STITCH626(0, "stitch"),
    // --- 3.0: character + creature pages (isCharacter; NPC pages = their Kind name, creature pages = their entity id) ---
    MEW(0),                                                    // creature guhs:mew (mewtwo)
    TIMMERGUH(0),                                              // Kind (timmerguh)
    BORIS(0), STEELE_MIKA(0), MUK(0), LUK(0), ROSY(0), WITTE_WOLFGUH(0),   // Kinds (balto)
    KNABBELKLOON(0),                                           // Kind (mewtwo)
    WOLKENHOEDER(0),                                           // Kind (hemel)
    LILO_GUH(0), NANI_GUH(0),                                  // Kinds (guhwaii)
    TIKIGUH(0),                                                // Kind (guhwaii-spellen)
    PLUISVINKJE(0), KAASMEESJE(0), GUH_UILTJE(0), ZEEMEEUWTJE(0),                         // creatures (vogels)
    GUHXOLOTL(0), GUH_EENDJE(0), KNABBELVLINDERTJE(0), GLIMGUHTJE(0), LIEVEHEERSBEESTJE(0), // creatures (waterdiertjes)
    PLUISEGELTJE(0), GUH_KONIJNTJE(0), PLUISEEKHOORNTJE(0), SHUCKLE(0),                    // creatures (landdiertjes)
    // --- 1.2.8 (het Bleekwoud): creature pages (their id is their entity id); bonus pages, see GuhDex.EXTRA ---
    KRAAKGUH(0), KRAAK_MIKA(0),
    // --- bbq2 (CONTRACT_130 5.3): the story guhs Sam-guh and Guhshi (isVerhaalGuh), then four creature pages (their id is their
    // entity id); all six count for "compleet" ---
    // <bbq2>
    SAM_GUH(0, "samguh"), GUHSHI(0, "guhshi"), SAUSBLUBJE(0), SAUSLOPER(0), WORSTZWIJNTJE(0), BEZORGGUHTJE(0),
    // </bbq2>
    // <balto>
    // </balto>
    // <mewtwo>
    // </mewtwo>
    // <guhwaii>
    // </guhwaii>
    // <guhwaiispellen>
    // </guhwaiispellen>
    // <bio_dieren_wezens>
    KOI(0), WOLKENSCHAAPJE(0),   // biomes3: creature pages (their id is their entity id); bonus pages, see GuhDex.EXTRA
    // </bio_dieren_wezens>
    // <timmerguh>
    // </timmerguh>
    // <hemel>
    // </hemel>
    ;

    /** The Guhdex page is a guh character (GuhNpcEntity), not a variant: it can't be tamed. */
    public boolean isCharacter() {
        return ordinal() >= REISGUH.ordinal() && !isVerhaalGuh();
    }

    /**
     * 3.0: a story guh (Baltoguh, Guhtwo, 626-guh): appended after the characters, but a real tameable variant with
     * its own fur (tameable once per player through nl.juiced.guhs.feature.verhaal.VerhaalGuhs, never by kaas knabbels).
     */
    public boolean isVerhaalGuh() {
        return nl.juiced.guhs.feature.verhaal.VerhaalGuh.van(this) != null;   // (bbq2: also Sam-guh and Guhshi)
    }

    /** biomes3: the Bloesemguh and the Tanukiguh: real tameable variants, each bound to its biome (no plush, bonus Guhdex pages). */
    public boolean isBioGuh() {
        return this == BLOESEMGUH || this == TANUKIGUH;
    }

    /** The kind of guh character this page is about (null for a real variant). */
    @javax.annotation.Nullable
    public GuhNpcEntity.Kind npcKind() {
        if (!isCharacter()) {
            return null;
        }
        for (GuhNpcEntity.Kind kind : GuhNpcEntity.Kind.values()) {   // (a creature page, like the kikkerguh, has no kind)
            if (kind.name().equals(name())) {
                return kind;
            }
        }
        return null;
    }

    /** The Guhdex page of a guh character (null when that kind has none). */
    @javax.annotation.Nullable
    public static GuhVariant ofCharacter(GuhNpcEntity.Kind kind) {
        for (GuhVariant v : values()) {
            if (v.isCharacter() && v.name().equals(kind.name())) {
                return v;
            }
        }
        return null;
    }

    /** Out of this many Guhmension guhs, the weights above are variants and the rest are normal. */
    public static final int ROLL_OUT_OF = 1000;
    /** Bone prefixes that only some variants show (all hidden on a normal guh). */
    public static final List<String> VARIANT_BONES = List.of("outfit_", "neck", "teckel", "ender", "koning", "wolk", "zeemeer", "asguh", "pluis", "pinguh",
            "balto", "mewtwo", "stitch", "samguh", "guhshi",
            "bloesem", "tanuki");   // biomes3
    /** How far the brontosaurus guh's head sits up (and forward) on its neck, in model pixels. */
    public static final float NECK_UP = 22f, NECK_FORWARD = 4.5f;
    /** How far the teckel guh's back legs and tail sit further back, in model pixels. */
    public static final float TECKEL_STRETCH = 7f;
    public static final int RAINBOW_FRAMES = 8;

    public final int weight;
    public final List<String> bones;

    GuhVariant(int weight, String... bones) {
        this.weight = weight;
        this.bones = List.of(bones);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The name shown above its head (when it has no name of its own). */
    public Component displayName() {
        // (a plain guh with a character's id looks like a normal guh, see texture(), so it's just called Guh too)
        return this == NORMAL || isCharacter() ? Component.translatable("entity.guhs.guh") : Component.translatable("entity.guhs.guh." + id());
    }

    /**
     * The fur texture on the guh model. Character pages (REISGUH ... GRILLGUH, the kikkerguh ...) are no fur of their own:
     * their NPC/creature textures don't fit the guh model, so a plain guh that got one of those ids (/summon, NBT) just
     * looks like a normal guh instead of a purple-black missing texture.
     */
    public Identifier texture() {
        return Guhs.id(this == NORMAL || isCharacter() ? "textures/entity/guh.png" : "textures/entity/guh_" + id() + ".png");
    }

    /** The rainbow guh cycles through its colour frames; everyone else has one texture. */
    public Identifier texture(int tick) {
        return this == RAINBOW ? Guhs.id("textures/entity/guh_rainbow_" + Math.floorMod(tick / 10, RAINBOW_FRAMES) + ".png") : texture();
    }

    /** Whether this variant shows a bone (non-variant bones always show). */
    public boolean shows(String bone) {
        if (VARIANT_BONES.stream().noneMatch(bone::startsWith)) {
            return true;
        }
        return bones.stream().anyMatch(bone::startsWith);
    }

    /** A random variant for a guh born in the Guhmension: usually NORMAL. */
    public static GuhVariant roll(RandomSource random) {
        int r = random.nextInt(ROLL_OUT_OF);
        for (GuhVariant variant : values()) {
            if (r < variant.weight) {
                return variant;
            }
            r -= variant.weight;
        }
        return NORMAL;
    }

    public static GuhVariant byId(String id) {
        for (GuhVariant variant : values()) {
            if (variant.id().equals(id)) {
                return variant;
            }
        }
        return NORMAL;
    }
}
