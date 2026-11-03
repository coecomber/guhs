package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Ground telegraphs (design 4.6: red/pink circles on the ground): a ring of coloured dust that pulses and fills inwards until
 * it "goes off". Particles only (cheap, visible to everyone). Kinds: {@link #cirkel}, {@link #lijn}.
 */
public final class Telegraaf {
    public static final int ROOD = 0xFF2A2A, ROZE = 0xFF5FC8, GRIJS = 0x8A8A8A;

    private record Actief(ServerLevel level, Vec3 a, Vec3 b, double straal, int duur, int kleur, boolean lijn, int[] t) {
    }

    private static final List<Actief> ALLE = new ArrayList<>();

    private Telegraaf() {
    }

    /** A circle on the ground at {@code centrum} (its y = the ground surface) for {@code ticks}. */
    public static void cirkel(ServerLevel level, Vec3 centrum, double straal, int ticks, int kleur) {
        ALLE.add(new Actief(level, centrum, centrum, straal, ticks, kleur, false, new int[1]));
    }

    /** A strip from a to b, {@code breedte} wide. */
    public static void lijn(ServerLevel level, Vec3 a, Vec3 b, double breedte, int ticks, int kleur) {
        ALLE.add(new Actief(level, a, b, breedte / 2, ticks, kleur, true, new int[1]));
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
