package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.2): a camera cutscene: a camera path, black bars, actors that play in the
 * world (only the viewer sees them) and subtitles. This class is only the script; {@link Cutscenes} plays it. Register it
 * from common code (both sides know a scene by its id):
 * <pre>
 * public static final Cutscene AANKOMST = Cutscene.maak("ringh2_aankomst").duur(200).bij("ring_h2").kaart("ring_h2")
 *         .speler(new Vec3(0.5, 0, 6.5), 180)
 *         .npc("guhrond", GuhNpcEntity.Kind.GUHROND, new Vec3(0.5, 0, 0.5), 0)
 *         .camera(0, new Vec3(6, 3, 8), new Vec3(0.5, 1, 3)).camera(120, new Vec3(3, 2, 5), new Vec3(0.5, 1, 1))
 *         .cameraKnipVolgt(121, new Vec3(-3, 1.5, 3), "speler")
 *         .loop("speler", 20, 100, new Vec3(0.5, 0, 2.5)).kijk("guhrond", 100, new Vec3(0.5, 1, 2.5))
 *         .zeg(110, "guhrond", "welkom", 80)
 *         .registreer();
 * </pre>
 * All positions are relative to the anchor block, in the coordinates of an unrotated template: (0.5, 0, 0.5) is the middle
 * of the anchor block's floor, and {@link #wereld} turns them with the structure's rotation exactly as the template's
 * blocks were turned. Times are ticks from the start. Texts: {@code scene.guhs.<id>.<key>} and the title
 * {@code scene.guhs.<id>.titel} (tools/features/verhaal_motor.py {@code scene(h, ...)}).
 */
public final class Cutscene {
    /** The name of the player's stand-in as an actor. */
    public static final String SPELER = "speler";

    /** A camera keyframe: where it is, and what it looks at (a point, or an actor); knip = jump here instead of gliding. */
    public record CameraPunt(int t, Vec3 pos, @Nullable Vec3 kijk, @Nullable String volgt, boolean knip) {
    }

    /** An actor: type null = the stand-in with the viewer's skin. */
    public record Acteur(String naam, @Nullable Supplier<? extends EntityType<?>> type, Vec3 start, float yaw, @Nullable Consumer<CompoundTag> nbt) {
    }

    public record Loop(String acteur, int t0, int t1, Vec3 naar) {
    }

    public record Kijk(String acteur, int t, Vec3 naar) {
    }

    public record Animatie(String acteur, int t, String naam) {
    }

    public record Zeg(int t, String spreker, String key, int ticks) {
    }

    public record Geluid(int t, Supplier<SoundEvent> geluid, float volume, float pitch) {
    }

    public record Deeltjes(int t, ParticleOptions deeltje, Vec3 pos, int aantal, double spreiding) {
    }

    public record Schud(int t, float kracht, int ticks) {
    }

    public record Zwart(int t0, int t1) {
    }

    private static final Map<String, Cutscene> ALLE = new LinkedHashMap<>();

    private final String id;
    private final int duur;
    @Nullable
    private final String lijn, kaart;
    private final double verbergEcht;
    private final List<CameraPunt> camera;
    private final List<Acteur> acteurs;
    private final List<Loop> lopen;
    private final List<Kijk> kijken;
    private final List<Animatie> animaties;
    private final List<Zeg> zinnen;
    private final List<Geluid> geluiden;
    private final List<Deeltjes> deeltjes;
    private final List<Schud> schudden;
    private final List<Zwart> zwart;

    private Cutscene(Builder b) {
        this.id = b.id;
        this.duur = b.duur;
        this.lijn = b.lijn;
        this.kaart = b.kaart;
        this.verbergEcht = b.verbergEcht;
        this.camera = b.camera.stream().sorted(Comparator.comparingInt(CameraPunt::t)).toList();
        this.acteurs = List.copyOf(b.acteurs);
        this.lopen = b.lopen.stream().sorted(Comparator.comparingInt(Loop::t0)).toList();
        this.kijken = b.kijken.stream().sorted(Comparator.comparingInt(Kijk::t)).toList();
        this.animaties = b.animaties.stream().sorted(Comparator.comparingInt(Animatie::t)).toList();
        this.zinnen = b.zinnen.stream().sorted(Comparator.comparingInt(Zeg::t)).toList();
        this.geluiden = List.copyOf(b.geluiden);
        this.deeltjes = List.copyOf(b.deeltjes);
        this.schudden = List.copyOf(b.schudden);
        this.zwart = List.copyOf(b.zwart);
    }

    /** A new scene; id = "&lt;pkg&gt;_&lt;naam&gt;". */
    public static Builder maak(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private int duur = 100;
        @Nullable
        private String lijn, kaart;
        private double verbergEcht;
        private final List<CameraPunt> camera = new ArrayList<>();
        private final List<Acteur> acteurs = new ArrayList<>();
        private final List<Loop> lopen = new ArrayList<>();
        private final List<Kijk> kijken = new ArrayList<>();
        private final List<Animatie> animaties = new ArrayList<>();
        private final List<Zeg> zinnen = new ArrayList<>();
        private final List<Geluid> geluiden = new ArrayList<>();
        private final List<Deeltjes> deeltjes = new ArrayList<>();
        private final List<Schud> schudden = new ArrayList<>();
        private final List<Zwart> zwart = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder duur(int ticks) {
            this.duur = Math.max(20, ticks);
            return this;
        }

        /** A camera keyframe: the camera glides smoothly from one to the next. */
        public Builder camera(int t, Vec3 pos, Vec3 kijkNaar) {
            camera.add(new CameraPunt(t, pos, kijkNaar, null, false));
            return this;
        }

        /** A keyframe that keeps looking at an actor ("speler" = the player's stand-in). */
        public Builder cameraVolgt(int t, Vec3 pos, String acteur) {
            camera.add(new CameraPunt(t, pos, null, acteur, false));
            return this;
        }

        /** A cut: at t the camera jumps to this shot (no glide from the keyframe before it). */
        public Builder cameraKnip(int t, Vec3 pos, Vec3 kijkNaar) {
            camera.add(new CameraPunt(t, pos, kijkNaar, null, true));
            return this;
        }

        /** A cut to a shot that keeps looking at an actor. */
        public Builder cameraKnipVolgt(int t, Vec3 pos, String acteur) {
            camera.add(new CameraPunt(t, pos, null, acteur, true));
            return this;
        }

        public Builder acteur(String naam, Supplier<? extends EntityType<?>> type, Vec3 start, float yaw) {
            return acteur(naam, type, start, yaw, null);
        }

        /** An actor with entity data (e.g. the Kind of a GuhNpcEntity, the variant of a GuhEntity), merged into its save tag. */
        public Builder acteur(String naam, Supplier<? extends EntityType<?>> type, Vec3 start, float yaw, @Nullable Consumer<CompoundTag> nbt) {
            acteurs.add(new Acteur(naam, type, start, yaw, nbt));
            return this;
        }

        /** A guh character (a sitting GuhNpcEntity of this Kind) as an actor. */
        public Builder npc(String naam, nl.juiced.guhs.entity.GuhNpcEntity.Kind kind, Vec3 start, float yaw) {
            return acteur(naam, nl.juiced.guhs.registry.ModEntities.GUH_NPC, start, yaw, tag -> tag.putString("Kind", kind.id()));
        }

        /** A guh of this variant (a walking GuhEntity) as an actor. */
        public Builder guh(String naam, nl.juiced.guhs.entity.GuhVariant variant, Vec3 start, float yaw) {
            return acteur(naam, nl.juiced.guhs.registry.ModEntities.GUH, start, yaw, tag -> tag.putString("Variant", variant.id()));
        }

        /** A stand-in with the player's skin that the scene may move; the real player is hidden while it plays. */
        public Builder speler(Vec3 start, float yaw) {
            acteurs.add(new Acteur(SPELER, null, start, yaw, null));
            return this;
        }

        /** The actor walks from where it is at t0 to naar, arriving at t1 (it faces where it goes). */
        public Builder loop(String acteur, int t0, int t1, Vec3 naar) {
            lopen.add(new Loop(acteur, t0, Math.max(t0 + 1, t1), naar));
            return this;
        }

        /** From t the actor looks at this point. */
        public Builder kijk(String acteur, int t, Vec3 naar) {
            kijken.add(new Kijk(acteur, t, naar));
            return this;
        }

        /**
         * At t the actor plays this animation: "zwaai" (swings its arm), "buk" / "sta" (crouch, stand) and "spring" work on
         * any actor; every other name is for the actor's own renderer or animation controller, which reads
         * {@link Cutscenes#animatie(net.minecraft.world.entity.Entity)} ("" stops it).
         */
        public Builder animatie(String acteur, int t, String naam) {
            animaties.add(new Animatie(acteur, t, naam));
            return this;
        }

        /** A subtitle under the bars: "&lt;name&gt;: text" (spreker = an actor name, or "" for the narrator); lang scene.guhs.&lt;id&gt;.&lt;key&gt;. */
        public Builder zeg(int t, String spreker, String key, int ticks) {
            zinnen.add(new Zeg(t, spreker, key, Math.max(10, ticks)));
            return this;
        }

        public Builder geluid(int t, Supplier<SoundEvent> s, float volume, float pitch) {
            geluiden.add(new Geluid(t, s, volume, pitch));
            return this;
        }

        public Builder deeltjes(int t, ParticleOptions p, Vec3 pos, int aantal, double spreiding) {
            deeltjes.add(new Deeltjes(t, p, pos, aantal, spreiding));
            return this;
        }

        /** The camera shakes (kracht 1 = clearly, 3 = an earthquake). */
        public Builder schud(int t, float kracht, int ticks) {
            schudden.add(new Schud(t, kracht, ticks));
            return this;
        }

        /** Fades to black at t0 and back at t1. */
        public Builder zwart(int t0, int t1) {
            zwart.add(new Zwart(t0, Math.max(t0 + 1, t1)));
            return this;
        }

        /** Hides the real entities within this range of the anchor for the viewer (so the real NPC doesn't stand next to its actor). */
        public Builder verbergEcht(double straal) {
            this.verbergEcht = straal;
            return this;
        }

        /** The questline whose Guhdex page (and travel map) gets this scene's replay button. */
        public Builder bij(String lijnId) {
            this.lijn = lijnId;
            return this;
        }

        /** The narrator card ({@link Verteller}) whose map is behind the picture-book replay. */
        public Builder kaart(String vertellerId) {
            this.kaart = vertellerId;
            return this;
        }

        /** Registers the scene (common code: both sides know it by id). */
        public Cutscene registreer() {
            Cutscene s = new Cutscene(this);
            synchronized (ALLE) {
                if (ALLE.put(s.id, s) != null) {
                    throw new IllegalStateException("Cutscene " + s.id + " is registered twice");
                }
            }
            return s;
        }
    }

    @Nullable
    public static Cutscene van(@Nullable String id) {
        synchronized (ALLE) {
            return id == null ? null : ALLE.get(id);
        }
    }

    public static List<Cutscene> alle() {
        synchronized (ALLE) {
            return List.copyOf(ALLE.values());
        }
    }

    // =====================================================================================================================
    // the script
    // =====================================================================================================================

    public String id() {
        return id;
    }

    public int duur() {
        return duur;
    }

    /** The questline it belongs to (null: none). */
    @Nullable
    public String lijn() {
        return lijn;
    }

    /** The narrator card behind its picture-book replay (null: a plain dark page). */
    @Nullable
    public String kaart() {
        return kaart;
    }

    public double verbergEcht() {
        return verbergEcht;
    }

    public List<CameraPunt> camera() {
        return camera;
    }

    public List<Acteur> acteurs() {
        return acteurs;
    }

    public List<Animatie> animaties() {
        return animaties;
    }

    public List<Zeg> zinnen() {
        return zinnen;
    }

    public List<Geluid> geluiden() {
        return geluiden;
    }

    public List<Deeltjes> deeltjes() {
        return deeltjes;
    }

    public List<Schud> schudden() {
        return schudden;
    }

    public String titelKey() {
        return "scene.guhs." + id + ".titel";
    }

    public String tekstKey(String key) {
        return "scene.guhs." + id + "." + key;
    }

    @Nullable
    public Acteur acteur(String naam) {
        for (Acteur a : acteurs) {
            if (a.naam().equals(naam)) {
                return a;
            }
        }
        return null;
    }

    public boolean heeftSpeler() {
        return acteur(SPELER) != null;
    }

    /** Where an actor is at time t (relative to the anchor): its start, moved by every walk so far. */
    public Vec3 plek(String acteur, double t) {
        Acteur a = acteur(acteur);
        Vec3 pos = a == null ? Vec3.ZERO : a.start();
        for (Loop l : lopen) {
            if (!l.acteur().equals(acteur) || t < l.t0()) {
                continue;
            }
            if (t >= l.t1()) {
                pos = l.naar();
            } else {
                double f = (t - l.t0()) / (l.t1() - l.t0());
                return pos.lerp(l.naar(), f);
            }
        }
        return pos;
    }

    /** Is the actor walking at time t? */
    public boolean looptNu(String acteur, double t) {
        for (Loop l : lopen) {
            if (l.acteur().equals(acteur) && t >= l.t0() && t < l.t1()) {
                return true;
            }
        }
        return false;
    }

    /** Which way an actor faces at time t (relative yaw, before the anchor's rotation): its last walk or look. */
    public float yaw(String acteur, double t) {
        Acteur a = acteur(acteur);
        float yaw = a == null ? 0 : a.yaw();
        int laatste = Integer.MIN_VALUE;
        Vec3 pos = a == null ? Vec3.ZERO : a.start();
        for (Loop l : lopen) {
            if (!l.acteur().equals(acteur) || t < l.t0()) {
                continue;
            }
            Vec3 d = l.naar().subtract(pos);
            if (d.horizontalDistanceSqr() > 1e-6) {
                yaw = yawVan(d);
                laatste = l.t0();
            }
            if (t >= l.t1()) {
                pos = l.naar();
            } else {
                break;
            }
        }
        Vec3 nu = plek(acteur, t);
        for (Kijk k : kijken) {
            if (k.acteur().equals(acteur) && t >= k.t() && k.t() >= laatste && !looptNu(acteur, t)) {
                Vec3 d = k.naar().subtract(nu);
                if (d.horizontalDistanceSqr() > 1e-6) {
                    yaw = yawVan(d);
                }
            }
        }
        return yaw;
    }

    /** The camera at time t: {position, the point it looks at} relative to the anchor (null: the scene has no camera). */
    @Nullable
    public Vec3[] cameraOp(double t) {
        if (camera.isEmpty()) {
            return null;
        }
        int i = 0;
        while (i + 1 < camera.size() && camera.get(i + 1).t() <= t) {
            i++;
        }
        CameraPunt a = camera.get(i);
        if (i + 1 >= camera.size() || t <= a.t() || camera.get(i + 1).knip()) {
            return new Vec3[] {a.pos(), doelVan(a, t)};
        }
        CameraPunt b = camera.get(i + 1);
        double f = (t - a.t()) / Math.max(1, b.t() - a.t());
        // the neighbours on the same shot (a cut starts a new one), for a smooth curve through the keyframes
        CameraPunt voor = i > 0 && !a.knip() ? camera.get(i - 1) : a;
        CameraPunt na = i + 2 < camera.size() && !camera.get(i + 2).knip() ? camera.get(i + 2) : b;
        Vec3 pos = spline(voor.pos(), a.pos(), b.pos(), na.pos(), f);
        Vec3 kijk = doelVan(a, t).lerp(doelVan(b, t), zacht(f));
        return new Vec3[] {pos, kijk};
    }

    private Vec3 doelVan(CameraPunt p, double t) {
        return p.volgt() != null ? plek(p.volgt(), t).add(0, 1.0, 0) : p.kijk() == null ? p.pos().add(0, 0, 1) : p.kijk();
    }

    private static double zacht(double f) {
        return f * f * (3 - 2 * f);
    }

    /** Catmull-Rom through p1..p2. */
    private static Vec3 spline(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double f) {
        return new Vec3(Mth.catmullrom((float) f, (float) p0.x, (float) p1.x, (float) p2.x, (float) p3.x),
                Mth.catmullrom((float) f, (float) p0.y, (float) p1.y, (float) p2.y, (float) p3.y),
                Mth.catmullrom((float) f, (float) p0.z, (float) p1.z, (float) p2.z, (float) p3.z));
    }

    /** How black the screen is at time t (0..1). */
    public float zwartOp(double t) {
        float max = 0;
        for (Zwart z : zwart) {
            double in = (t - z.t0()) / 8.0, uit = (z.t1() + 8 - t) / 8.0;
            max = Math.max(max, (float) Mth.clamp(Math.min(in, uit), 0, 1));
        }
        return max;
    }

    /** The subtitle at time t (null: none). */
    @Nullable
    public Zeg zinOp(double t) {
        Zeg nu = null;
        for (Zeg z : zinnen) {
            if (t >= z.t() && t < z.t() + z.ticks()) {
                nu = z;
            }
        }
        return nu;
    }

    // =====================================================================================================================
    // anchor maths (both sides)
    // =====================================================================================================================

    /** A scene position in the world: turned with the anchor's rotation like the blocks of a template, then moved to the anchor. */
    public static Vec3 wereld(BlockPos anker, Rotation draai, Vec3 rel) {
        Vec3 r = StructureTemplate.transform(rel, Mirror.NONE, draai, BlockPos.ZERO);
        return new Vec3(anker.getX() + r.x, anker.getY() + r.y, anker.getZ() + r.z);
    }

    /** A scene yaw in the world. */
    public static float wereldYaw(Rotation draai, float yaw) {
        return switch (draai) {
            case CLOCKWISE_90 -> yaw + 90f;
            case CLOCKWISE_180 -> yaw + 180f;
            case COUNTERCLOCKWISE_90 -> yaw + 270f;
            default -> yaw;
        };
    }

    /** The yaw that looks along d (Minecraft: 0 = south). */
    public static float yawVan(Vec3 d) {
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    /** The pitch that looks along d (down is positive). */
    public static float pitchVan(Vec3 d) {
        return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
    }

    @Override
    public String toString() {
        return "Cutscene[" + id + "]";
    }
}
