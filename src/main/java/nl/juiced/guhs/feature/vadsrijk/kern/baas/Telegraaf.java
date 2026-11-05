package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Ground telegraphs (design 4.6: red/pink circles on the ground). T-VFX: since the VFX kit these are real ground DECALS (flat
 * textured quads that fill up until they "go off", drawn by the client: vfx.client.Decals; one packet each instead of dust every
 * 3 ticks). {@link #DECALS} = false brings back the old dust rings. Kinds: {@link #cirkel}, {@link #lijn}, {@link #kegel},
 * {@link #donut}, {@link #veilig}.
 */
public final class Telegraaf {
    /** Colour language (fix round 1): red = damage, pink = safe, purple = mechanic, yellow = stack (soak together). */
    public static final int ROOD = 0xFF2A2A, ROZE = 0xFF5FC8, GRIJS = 0x8A8A8A, PAARS = 0xA040FF, GEEL = 0xFFD62A;

    private record Actief(ServerLevel level, Vec3 a, Vec3 b, double straal, int duur, int kleur, boolean lijn, int[] t) {
    }

    private static final List<Actief> ALLE = new ArrayList<>();

    private Telegraaf() {
    }

    /** T-VFX: true = telegraphs are ground decals (one packet, drawn by the client's VFX kit); false = the old dust rings. */
    public static boolean DECALS = true;

    /** A circle on the ground at {@code centrum} (its y = the ground surface) for {@code ticks}. */
    public static void cirkel(ServerLevel level, Vec3 centrum, double straal, int ticks, int kleur) {
        if (DECALS) {   // T-VFX
            nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.decal(level, nl.juiced.guhs.feature.vadsrijk.vfx.Vorm.cirkel(straal), centrum, 0, ticks, kleur, 0);
            return;
        }
        ALLE.add(new Actief(level, centrum, centrum, straal, ticks, kleur, false, new int[1]));
    }

    /** A strip from a to b, {@code breedte} wide. */
    public static void lijn(ServerLevel level, Vec3 a, Vec3 b, double breedte, int ticks, int kleur) {
        if (DECALS) {   // T-VFX
            nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.decalLijn(level, a, b, breedte, ticks, kleur, 0);
            return;
        }
        ALLE.add(new Actief(level, a, b, breedte / 2, ticks, kleur, true, new int[1]));
    }

    // ---- T-VFX: the new shapes (decals only) -----------------------------------------------------------------------------------

    /** A cone from {@code punt} towards {@code richting} (radians, atan2(dz, dx)), {@code graden} wide, r long. */
    public static void kegel(ServerLevel level, Vec3 punt, double richting, double r, double graden, int ticks, int kleur) {
        nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.decal(level, nl.juiced.guhs.feature.vadsrijk.vfx.Vorm.kegel(r, graden), punt, richting, ticks, kleur, 0);
    }

    /** A ring: safe within {@code binnen}, hit up to {@code buiten}. */
    public static void donut(ServerLevel level, Vec3 centrum, double binnen, double buiten, int ticks, int kleur) {
        nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.decal(level, nl.juiced.guhs.feature.vadsrijk.vfx.Vorm.donut(binnen, buiten), centrum, 0, ticks, kleur, 0);
    }

    /** A pink safe spot (the pattern; the danger around it is the caller's). */
    public static void veilig(ServerLevel level, Vec3 centrum, double straal, int ticks) {
        nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.decal(level, nl.juiced.guhs.feature.vadsrijk.vfx.Vorm.veilig(straal), centrum, 0, ticks, ROZE, 0);
    }

    public static void tick() {
        Iterator<Actief> it = ALLE.iterator();
        while (it.hasNext()) {
            Actief t = it.next();
            int nu = t.t()[0]++;
            if (nu >= t.duur()) {
                it.remove();
                continue;
            }
            if (nu % 3 != 0) {
                continue;
            }
            double vol = (double) nu / t.duur();
            DustParticleOptions stof = new DustParticleOptions(t.kleur(), 1.6f);
            if (!t.lijn()) {
                int n = (int) Math.max(12, t.straal() * 7);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    t.level().sendParticles(stof, t.a().x + Math.cos(a) * t.straal(), t.a().y + 0.1, t.a().z + Math.sin(a) * t.straal(), 1, 0, 0, 0, 0);
                }
                double r = t.straal() * vol;   // the inner ring grows to the edge: when it gets there, it goes off
                int m = (int) Math.max(6, r * 6);
                for (int i = 0; i < m; i++) {
                    double a = Math.PI * 2 * i / m;
                    t.level().sendParticles(stof, t.a().x + Math.cos(a) * r, t.a().y + 0.1, t.a().z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            } else {
                Vec3 d = t.b().subtract(t.a());
                double len = d.length();
                Vec3 n = new Vec3(-d.z, 0, d.x).normalize().scale(t.straal());
                for (double s = 0; s <= len; s += 0.6) {
                    Vec3 p = t.a().add(d.scale(s / Math.max(0.01, len)));
                    t.level().sendParticles(stof, p.x + n.x, p.y + 0.1, p.z + n.z, 1, 0, 0, 0, 0);
                    t.level().sendParticles(stof, p.x - n.x, p.y + 0.1, p.z - n.z, 1, 0, 0, 0, 0);
                    if (s <= len * vol) {
                        t.level().sendParticles(stof, p.x, p.y + 0.1, p.z, 1, t.straal() * 0.3, 0, t.straal() * 0.3, 0);
                    }
                }
            }
        }
    }
}
