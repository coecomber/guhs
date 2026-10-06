package nl.juiced.guhs.feature.ring.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import nl.juiced.guhs.client.GuhRenderFrame;

/**
 * bbq2 (ring-kern), client: the named cutscene animations of the cast. The story engine only STORES the name a scene gives
 * an actor ({@code Cutscene.Builder.animatie(acteur, t, naam)}, read back with {@code Cutscenes.animatie(entity)}); this
 * class turns a name and the ticks since it started into bone moves. Every character of the Knabbelring plays every name:
 * the sitting-guh characters (Guhdalf, Araguh, Leguhlas, Gimguh, Merrie, Pippguh, Guhrond, Guhladriel) with their arms, the
 * four-legged ones (Boromika, Smikagol as a character and as an entity, Sam-guh) with their front paws.
 * <p>
 * The names ({@link #NAMEN}; "" stops; an unknown name does nothing):
 * praat (talking, a paw gesturing), knik (nodding yes), schud (shaking no), buig (a bow), juich (cheering, hopping, paws
 * up), wijs (pointing ahead), schrik (a startled hop back), ruzie (bickering: leaning in, paws waving), schaam (ashamed:
 * head down, ears drooping), grijp (a lunge with both paws), eet (munching), slaap (dozing off), toover (a paw / the
 * staff raised high, trembling), val (falling back and sinking away: Guhdalf at the bridge), kniel (kneeling), lach
 * (shaking with laughter), sluip (sneaking low), huil (sobbing behind the paws), draag (holding something out),
 * kijk (looking around). Besides those, three names change a look instead of a pose (see RingClient): wit / grijs
 * (Guhdalf), kroon (Araguh).
 * <p>
 * The engine's own four (zwaai, buk, sta, spring) keep working on every actor; a character that is moved by a scene
 * ({@code loop}) hops along by itself ({@link #huppel}).
 */
public final class CastAnimaties {
    public static final List<String> NAMEN = List.of("praat", "knik", "schud", "buig", "juich", "wijs", "schrik", "ruzie", "schaam", "grijp", "eet",
            "slaap", "toover", "val", "kniel", "lach", "sluip", "huil", "draag", "kijk");

    /** How a character is built: sitting upright with two arms, or on four paws. */
    public enum Lijf {
        ZITTEND("arm_left", "arm_right"), VIERPOOT("leg_front_left", "leg_front_right");

        final String links, rechts;

        Lijf(String links, String rechts) {
            this.links = links;
            this.rechts = rechts;
        }
    }

    private static float golf(float t, float periode) {
        return Mth.sin(t * Mth.TWO_PI / periode);
    }

    /** 0 -> 1 in duur ticks, eased. */
    private static float in(float t, float duur) {
        float x = Mth.clamp(t / duur, 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    /** 0 -> 1 -> 0 over duur ticks (then 0). */
    private static float bult(float t, float duur) {
        return t >= duur ? 0f : Mth.sin(Mth.clamp(t / duur, 0f, 1f) * Mth.PI);
    }

    /**
     * The bone moves of animation {@code naam}, {@code t} ticks (partial ticks included) after it started; null: no such
     * animation. In a snapshot a positive X turn lifts a nose / swings a paw up and forward, a negative one bows.
     */
    @Nullable
    public static GuhRenderFrame.BoneMove maak(String naam, float t, Lijf lijf) {
        final String L = lijf.links, R = lijf.rechts;
        // (a four-legged character lifts a paw less far than an arm goes)
        final float arm = lijf == Lijf.ZITTEND ? 1f : 0.55f;
        switch (naam) {
            case "praat": {
                float h = golf(t, 14) * 0.09f, z = golf(t, 23) * 0.05f, a = (0.55f + golf(t, 17) * 0.3f) * arm;
                return b -> {
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() + h).setRotZ(s.getRotZ() + z));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                };
            }
            case "knik": {
                float h = -0.38f * Math.abs(golf(t, 16));
                return b -> b.ifPresent("head", s -> s.setRotX(s.getRotX() + h));
            }
            case "schud": {
                float h = golf(t, 10) * 0.42f;
                return b -> b.ifPresent("head", s -> s.setRotY(s.getRotY() + h));
            }
            case "buig": {
                float k = in(t, 10);
                return b -> {
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.45f * k));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.3f * k));
                };
            }
            case "juich": {
                float hop = Math.abs(golf(t, 10)) * 3.0f, a = (2.2f + golf(t, 5) * 0.25f) * arm, oor = golf(t, 6) * 0.2f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + hop));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() + oor));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() - oor));
                    b.ifPresent("tail", s -> s.setRotY(s.getRotY() + oor * 2));
                };
            }
            case "wijs": {
                float k = in(t, 6), a = 1.45f * k * arm;
                return b -> {
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() + 0.08f * k));
                };
            }
            case "schrik": {
                float sprong = bult(t, 9) * 4.5f, terug = in(t, 5) * 2.2f, a = 2.0f * in(t, 4) * arm, bibber = t < 30 ? golf(t, 3) * 0.06f : 0f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + sprong).setTranslateZ(s.getTranslateZ() + terug));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() + 0.14f));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent("head", s -> s.setRotY(s.getRotY() + bibber));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() - 0.35f));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + 0.35f));
                };
            }
            case "ruzie": {
                float h = golf(t, 7) * 0.24f, a = (1.15f + golf(t, 8) * 0.6f) * arm, c = (1.0f + golf(t + 4, 8) * 0.6f) * arm, wip = Math.abs(golf(t, 8)) * 0.8f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + wip));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.2f));
                    b.ifPresent("head", s -> s.setRotY(s.getRotY() + h).setRotX(s.getRotX() + 0.12f));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + c));
                };
            }
            case "schaam": {
                float k = in(t, 14), wieg = golf(t, 40) * 0.04f;
                return b -> {
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.6f * k).setRotY(s.getRotY() + 0.25f * k));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.1f * k).setRotZ(s.getRotZ() + wieg));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() - 0.55f * k));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + 0.55f * k));
                    b.ifPresent("tail", s -> s.setRotX(s.getRotX() - 0.3f * k));
                };
            }
            case "grijp": {
                float k = bult(t, 14), a = (0.4f + 1.4f * k) * arm;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateZ(s.getTranslateZ() - 4.5f * k).setTranslateY(s.getTranslateY() + 1.2f * k));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.32f * k));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                };
            }
            case "eet": {
                float kauw = golf(t, 5), a = (1.3f + kauw * 0.14f) * arm;
                return b -> {
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.14f + kauw * 0.09f));
                };
            }
            case "slaap": {
                float k = in(t, 30), adem = golf(t, 60) * 0.03f;
                return b -> {
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.12f * k).setScaleY(1f + adem));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.5f * k + adem).setRotZ(s.getRotZ() + 0.15f * k));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() - 0.4f * k));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + 0.4f * k));
                };
            }
            case "toover": {
                float k = in(t, 8), a = (2.45f + golf(t, 4) * 0.07f) * k * arm, tril = golf(t, 3) * 0.3f * k;
                return b -> {
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() + 0.25f * k));
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + 0.8f * k + tril));
                };
            }
            case "val": {
                float k = in(t, 12), zak = Math.max(0f, t - 8f) * 1.1f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() - Math.min(zak, 120f)).setTranslateZ(s.getTranslateZ() + 3f * k));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() + 0.9f * k));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + 2.3f * k * arm));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + 2.3f * k * arm));
                };
            }
            case "kniel": {
                float k = in(t, 12);
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() - 2.4f * k));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.25f * k));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.35f * k));
                };
            }
            case "lach": {
                float schok = Math.abs(golf(t, 5)) * 0.9f, oor = golf(t, 5) * 0.15f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + schok));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() + 0.28f));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() + oor));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() - oor));
                };
            }
            case "sluip": {
                float k = in(t, 10), kijk = golf(t, 50) * 0.5f;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() - 1.6f * k));
                    b.ifPresent("body", s -> s.setRotX(s.getRotX() - 0.2f * k));
                    b.ifPresent("head", s -> s.setRotY(s.getRotY() + kijk * k));
                    b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() - 0.3f * k));
                    b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + 0.3f * k));
                };
            }
            case "huil": {
                float snik = Math.abs(golf(t, 9)) * 0.7f, a = 1.9f * in(t, 8) * arm;
                return b -> {
                    b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + snik));
                    b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.42f));
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                };
            }
            case "draag": {
                float a = 1.05f * in(t, 6) * arm;
                return b -> {
                    b.ifPresent(L, s -> s.setRotX(s.getRotX() + a));
                    b.ifPresent(R, s -> s.setRotX(s.getRotX() + a));
                };
            }
            case "kijk": {
                float h = golf(t, 60) * 0.7f;
                return b -> b.ifPresent("head", s -> s.setRotY(s.getRotY() + h));
            }
            default:
                return null;
        }
    }

    /** A character that a scene moves along the ground hops (it has no walking legs): ticks = its age, snelheid = blocks per tick. */
    public static GuhRenderFrame.BoneMove huppel(float ticks, float snelheid) {
        float hop = Math.abs(Mth.sin(ticks * 0.55f)) * Math.min(3.2f, 1.2f + snelheid * 14f), wieg = Mth.sin(ticks * 0.55f) * 0.09f;
        return b -> {
            b.ifPresent("root", s -> s.setTranslateY(s.getTranslateY() + hop));
            b.ifPresent("body", s -> s.setRotZ(s.getRotZ() + wieg));
            b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() + wieg * 2));
            b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + wieg * 2));
        };
    }

    /** The engine's "zwaai" on a character without a vanilla arm: the right paw waves for as long as the swing lasts (0..1). */
    public static GuhRenderFrame.BoneMove zwaai(float voortgang, Lijf lijf) {
        float a = (1.9f + Mth.sin(voortgang * Mth.TWO_PI * 2) * 0.4f) * Mth.sin(voortgang * Mth.PI) * (lijf == Lijf.ZITTEND ? 1f : 0.55f);
        return b -> b.ifPresent(lijf.rechts, s -> s.setRotX(s.getRotX() + a));
    }

    /** Both moves, one after the other (either may be null). */
    @Nullable
    public static GuhRenderFrame.BoneMove samen(@Nullable GuhRenderFrame.BoneMove a, @Nullable GuhRenderFrame.BoneMove b) {
        return a == null ? b : b == null ? a : bones -> {
            a.apply(bones);
            b.apply(bones);
        };
    }

    private CastAnimaties() {
    }
}
