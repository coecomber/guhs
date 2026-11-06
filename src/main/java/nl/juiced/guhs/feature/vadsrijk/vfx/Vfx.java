package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * The server side of the VFX kit v1 (roadmap 3.2): every call sends ONE small packet to the players around, the client draws it
 * with its own system (decals on the ground, textured particles, meshes; budget, LOD and the VOL/ZACHT/UIT slider are the
 * client's). Nothing here spawns vanilla particles.
 * <ul>
 *   <li>{@link #decal}: a ground telegraph ({@link Vorm}) that fills up over {@code ticks}; {@link #weg} removes a group early
 *       (an interrupted cast). Decals are gameplay: they show even at VFX level UIT (then plain, without sparkle).</li>
 *   <li>{@link #deeltjes}, {@link #golf} (shockwave ring), {@link #straal} (beam), {@link #flits} (impact flash), {@link #spoor}
 *       (ribbon behind an entity), {@link #zuil} (light pillar): cosmetic.</li>
 *   <li>{@link #pose}: wind-up / hit / phase pose for renderers without GeckoLib (the Klodderreus' squash).</li>
 *   <li>{@link #cast}: the cast bar under the boss bar ("Koning Klodder laadt: Grote Plof").</li>
 *   <li>{@link #schud}: screen shake through the existing beleving slider.</li>
 * </ul>
 */
public final class Vfx {
    /** Decals reach further than cosmetics (you must see what's coming). */
    public static final double DECAL_BEREIK = 96, COSMETISCH_BEREIK = 64;
    private static int groepTeller = 1;

    private Vfx() {
    }

    /** A fresh decal group id (to remove a set of decals together with {@link #weg}). */
    public static int nieuweGroep() {
        groepTeller = groepTeller >= 1_000_000 ? 1 : groepTeller + 1;
        return groepTeller;
    }

    static List<ServerPlayer> rond(ServerLevel sl, Vec3 v, double r) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(v) <= r * r) {
                l.add(p);
            }
        }
        return l;
    }

    private static void naar(ServerLevel sl, Vec3 v, double r, String soort, CompoundTag d) {
        for (ServerPlayer p : rond(sl, v, r)) {
            VfxPayloads.stuur(p, soort, d);
        }
    }

    private static void pos(CompoundTag d, String k, Vec3 v) {
        d.putDouble(k + "x", v.x);
        d.putDouble(k + "y", v.y);
        d.putDouble(k + "z", v.z);
    }

    // ---- decals --------------------------------------------------------------------------------------------------------------

    /**
     * A ground telegraph: {@code vorm} laid at {@code anker} (its y = the ground surface), facing {@code richting} (radians,
     * atan2(dz, dx); only cones/strips/crosses use it), filling up over {@code ticks}, in {@code kleur} (Telegraaf colours: red =
     * damage, pink = safe, purple = mechanic, yellow = stack). {@code groep}: 0 or an id from {@link #nieuweGroep}.
     */
    public static void decal(ServerLevel sl, Vorm vorm, Vec3 anker, double richting, int ticks, int kleur, int groep) {
        CompoundTag d = new CompoundTag();
        d.putString("vorm", vorm.soort().name());
        pos(d, "", anker);
        d.putDouble("r", vorm.r());
        d.putDouble("r2", vorm.r2());
        d.putDouble("hoek", vorm.hoek());
        d.putDouble("lengte", vorm.lengte());
        d.putDouble("breedte", vorm.breedte());
        d.putDouble("richting", richting);
        d.putInt("ticks", Math.max(2, ticks));
        d.putInt("kleur", kleur);
        d.putInt("groep", groep);
        naar(sl, anker, DECAL_BEREIK + vorm.bereik(), "decal", d);
    }

    /** A strip from a to b (its anchor a, its direction towards b): the old Telegraaf.lijn as a decal. */
    public static void decalLijn(ServerLevel sl, Vec3 a, Vec3 b, double breedte, int ticks, int kleur, int groep) {
        Vec3 d = b.subtract(a);
        decal(sl, Vorm.lijn(Math.max(0.5, Math.sqrt(d.x * d.x + d.z * d.z)), breedte), a, Math.atan2(d.z, d.x), ticks, kleur, groep);
    }

    /** Removes every decal of a group early (an interrupted cast, a boss that reset). */
    public static void weg(ServerLevel sl, Vec3 bij, int groep) {
        CompoundTag d = new CompoundTag();
        d.putInt("groep", groep);
        naar(sl, bij, DECAL_BEREIK + 32, "weg", d);
    }

    // ---- cosmetics -----------------------------------------------------------------------------------------------------------

    /** A burst of {@code n} of our own particles at v (spread = box half size, speed = outward speed). */
    public static void deeltjes(ServerLevel sl, Deeltje soort, Vec3 v, int n, int kleur, double spreid, double snelheid) {
        CompoundTag d = new CompoundTag();
        d.putString("soort", soort.name());
        pos(d, "", v);
        d.putInt("n", n);
        d.putInt("kleur", kleur);
        d.putDouble("spreid", spreid);
        d.putDouble("snel", snelheid);
        naar(sl, v, COSMETISCH_BEREIK, "deeltjes", d);
    }

    /** An expanding shockwave ring on the ground, from 0 to r in {@code ticks}. */
    public static void golf(ServerLevel sl, Vec3 v, double r, int kleur, int ticks) {
        CompoundTag d = new CompoundTag();
        pos(d, "", v);
        d.putDouble("r", r);
        d.putInt("kleur", kleur);
        d.putInt("ticks", ticks);
        naar(sl, v, COSMETISCH_BEREIK + r, "golf", d);
    }

    /** A beam (a camera-facing strip) from a to b, {@code breedte} wide, for {@code ticks}. */
    public static void straal(ServerLevel sl, Vec3 a, Vec3 b, double breedte, int kleur, int ticks) {
        CompoundTag d = new CompoundTag();
        pos(d, "a", a);
        pos(d, "b", b);
        d.putDouble("breedte", breedte);
        d.putInt("kleur", kleur);
        d.putInt("ticks", ticks);
        naar(sl, a.lerp(b, 0.5), COSMETISCH_BEREIK + a.distanceTo(b) / 2, "straal", d);
    }

    /** A short impact flash (a big soft glow that pops and fades in ~6 ticks). */
    public static void flits(ServerLevel sl, Vec3 v, double grootte, int kleur) {
        CompoundTag d = new CompoundTag();
        pos(d, "", v);
        d.putDouble("grootte", grootte);
        d.putInt("kleur", kleur);
        naar(sl, v, COSMETISCH_BEREIK, "flits", d);
    }

    /** A ribbon trail behind an entity for {@code ticks} (a charge, a dive, a jump). */
    public static void spoor(ServerLevel sl, Entity e, int kleur, int ticks, double breedte) {
        CompoundTag d = new CompoundTag();
        d.putInt("id", e.getId());
        d.putInt("kleur", kleur);
        d.putInt("ticks", ticks);
        d.putDouble("breedte", breedte);
        naar(sl, e.position(), COSMETISCH_BEREIK, "spoor", d);
    }

    /** A vertical light pillar (loot, a safe spot, a phase change), {@code hoogte} high. */
    public static void zuil(ServerLevel sl, Vec3 v, double hoogte, int kleur, int ticks) {
        CompoundTag d = new CompoundTag();
        pos(d, "", v);
        d.putDouble("hoogte", hoogte);
        d.putInt("kleur", kleur);
        d.putInt("ticks", ticks);
        naar(sl, v, COSMETISCH_BEREIK, "zuil", d);
    }

    /**
     * A pose for renderers without GeckoLib (and a charge glow for all): {@code soort} = windup (crouch over ticks), treffer (pop),
     * geraakt (wobble), fase (big wobble), juich (cheer-up bounce).
     */
    public static void pose(ServerLevel sl, Entity e, String soort, int ticks) {
        CompoundTag d = new CompoundTag();
        d.putInt("id", e.getId());
        d.putString("pose", soort);
        d.putInt("ticks", ticks);
        naar(sl, e.position(), COSMETISCH_BEREIK, "pose", d);
    }

    /** Screen shake around a spot (the beleving slider "schermschud" scales it per player; 0 = off). */
    public static void schud(ServerLevel sl, Vec3 v, double straal, float sterkte, int ticks) {
        nl.juiced.guhs.feature.vadsrijk.kern.beleving.Beleving.schudRond(sl, v, straal, sterkte, ticks);
    }

    // ---- the cast bar --------------------------------------------------------------------------------------------------------

    /**
     * The cast bar under the boss bar for everyone in this boss's fight: "&lt;boss&gt; laadt: &lt;naam&gt;" (both lang keys) filling over
     * {@code ticks}. {@code onderbreekbaar}: the bar shows a break meter (deal damage to stop it).
     */
    public static void cast(VadsrijkBaas baas, String naamKey, int ticks, boolean onderbreekbaar) {
        if (!(baas.level() instanceof ServerLevel sl)) {
            return;
        }
        CompoundTag d = new CompoundTag();
        d.putInt("id", baas.getId());
        d.putString("baas", baas.titelKey());
        d.putString("naam", naamKey);
        d.putInt("ticks", ticks);
        d.putBoolean("onderbreek", onderbreekbaar);
        for (ServerPlayer p : kijkers(baas, sl)) {
            VfxPayloads.stuur(p, "cast", d);
        }
    }

    /** The break meter of an interruptible cast (0..1). */
    public static void castBreuk(VadsrijkBaas baas, float breuk) {
        if (!(baas.level() instanceof ServerLevel sl)) {
            return;
        }
        CompoundTag d = new CompoundTag();
        d.putInt("id", baas.getId());
        d.putFloat("breuk", breuk);
        for (ServerPlayer p : kijkers(baas, sl)) {
            VfxPayloads.stuur(p, "cast_breuk", d);
        }
    }

    /** Ends the cast bar: {@code onderbroken} shows "Onderbroken!" for a moment, else it just fades. */
    public static void castStop(VadsrijkBaas baas, boolean onderbroken) {
        if (!(baas.level() instanceof ServerLevel sl)) {
            return;
        }
        CompoundTag d = new CompoundTag();
        d.putInt("id", baas.getId());
        d.putBoolean("onderbroken", onderbroken);
        for (ServerPlayer p : kijkers(baas, sl)) {
            VfxPayloads.stuur(p, "cast_stop", d);
        }
    }

    static List<ServerPlayer> kijkers(VadsrijkBaas baas, ServerLevel sl) {
        List<ServerPlayer> l = new ArrayList<>();
        double r = Math.max(40, baas.gevechtsStraal() + 16);
        for (ServerPlayer p : sl.players()) {
            if (baas.inGevecht(p) || p.distanceToSqr(baas) <= r * r) {
                l.add(p);
            }
        }
        return l;
    }

    /** Sets one player's own VFX level (VOL / ZACHT / UIT; the dev command). */
    public static void niveau(ServerPlayer p, String niveau) {
        CompoundTag d = new CompoundTag();
        d.putString("niveau", niveau);
        VfxPayloads.stuur(p, "niveau", d);
    }
}
