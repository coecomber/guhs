package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * One RUNNING bouwdoos attack ({@link Aanval}) of one boss: telegraph &rarr; wind-up &rarr; hit, step by step, plus the
 * interrupt meter. Made and ticked by {@link BaasVfx}; the boss stands still and faces its target meanwhile.
 */
public final class Bouwdoos {
    /** A step whose shapes were laid: where, which way, and whether it hit already. */
    private record Gelegd(Aanval.Stap stap, List<Vec3> plekken, List<Double> richtingen, boolean[] klaar) {
    }

    final VadsrijkBaas baas;
    final Aanval aanval;
    final int groep = Vfx.nieuweGroep();
    private final List<Gelegd> gelegd = new ArrayList<>();
    private final boolean[] getoond;
    private int t;
    private boolean eersteTreffer = true, klaar;
    /** Damage taken during an interruptible cast. */
    double castSchade;
    private float breukGestuurd = -1;
    /** For gametests: how many players were hit in total, and whether it was interrupted. */
    public int geraakt;
    public boolean onderbroken;

    Bouwdoos(VadsrijkBaas baas, Aanval aanval) {
        this.baas = baas;
        this.aanval = aanval;
        this.getoond = new boolean[aanval.stappen.size()];
    }

    void start(ServerLevel sl) {
        baas.getNavigation().stop();
        if (aanval.cast) {
            Vfx.cast(baas, aanval.naamKey(), aanval.duur(), aanval.onderbreekbaar());
        }
        int eersteHit = Integer.MAX_VALUE;
        for (Aanval.Stap s : aanval.stappen) {
            eersteHit = Math.min(eersteHit, s.na + s.telegraaf);
        }
        if (aanval.windup != null) {
            BaasAnims.speel(baas, aanval.windup);
        }
        Vfx.pose(sl, baas, "windup", eersteHit);
        Vfx.deeltjes(sl, Deeltje.GLOED, baas.position().add(0, baas.getBbHeight() * 0.5, 0), 12, aanval.onderbreekbaar() ? 0xA040FF : 0xFFE0A0,
                baas.getBbWidth() * 0.6, 0.02);
        sl.playSound(null, baas.blockPosition(), aanval.onderbreekbaar() ? SoundEvents.EVOKER_PREPARE_SUMMON : SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 1.4f, aanval.onderbreekbaar() ? 1.0f : 1.6f);
        if (aanval.bijStart != null) {
            aanval.bijStart.accept(baas, sl);
        }
    }

    boolean klaar() {
        return klaar;
    }

    int t() {
        return t;
    }

    /** One tick; false once it is over. */
    boolean tick(ServerLevel sl) {
        if (klaar) {
            return false;
        }
        if (baas.geplofd() || !baas.isAlive()) {
            stop(sl, false);
            return false;
        }
        ServerPlayer d = BaasVfx.doel(baas);
        if (d != null) {
            baas.getLookControl().setLookAt(d, 30, 30);
        }
        baas.getNavigation().stop();
        // the interrupt (a stun counts too)
        if (aanval.onderbreekbaar()) {
            double nodig = aanval.onderbreek * Math.max(1, baas.maxVads());
            float breuk = (float) Math.min(1, castSchade / nodig);
            if (breuk >= 1 || Statussen.heeft(baas, Status.VERDOOFD)) {
                onderbreek(sl);
                return false;
            }
            if (Math.abs(breuk - breukGestuurd) > 0.04f && t % 3 == 0) {
                breukGestuurd = breuk;
                Vfx.castBreuk(baas, breuk);
            }
        }
        // lay the steps that are due
        for (int i = 0; i < aanval.stappen.size(); i++) {
            Aanval.Stap s = aanval.stappen.get(i);
            if (!getoond[i] && t >= s.na) {
                getoond[i] = true;
                gelegd.add(leg(sl, s));
            }
        }
        // hit the ones whose telegraph ran out
        boolean alles = true;
        for (Gelegd g : gelegd) {
            if (!g.klaar()[0]) {
                if (t >= g.stap().na + g.stap().telegraaf) {
                    g.klaar()[0] = true;
                    raak(sl, g);
                } else {
                    alles = false;
                }
            }
        }
        for (boolean b : getoond) {
            alles &= b;
        }
        t++;
        if (alles && t > aanval.duur() + 6) {
            klaar = true;
            if (aanval.cast) {
                Vfx.castStop(baas, false);
            }
            if (aanval.onderbreekbaar() && aanval.nietOnderbroken != null) {
                aanval.nietOnderbroken.accept(baas, sl);
            }
            return false;
        }
        return true;
    }

    private Gelegd leg(ServerLevel sl, Aanval.Stap s) {
        List<Vec3> plekken = new ArrayList<>();
        List<Double> richtingen = new ArrayList<>();
        Vec3 b = grond(baas.position());
        ServerPlayer d = BaasVfx.doel(baas);
        List<ServerPlayer> vechters = BaasVfx.vechters(baas);
        if (d == null && !vechters.isEmpty()) {
            d = vechters.get(0);
        }
        double naarDoel = d == null ? Math.toRadians(baas.getYRot() + 90) : Math.atan2(d.getZ() - baas.getZ(), d.getX() - baas.getX());
        double draai = Math.toRadians(s.draai);
        switch (s.anker) {
            case BAAS -> {
                plekken.add(b);
                richtingen.add(naarDoel + draai);
            }
            case DOEL -> {
                plekken.add(d == null ? b : baas.voetenVan(d));
                richtingen.add(naarDoel + draai);
            }
            case ELKE_SPELER -> {
                for (ServerPlayer p : vechters) {
                    plekken.add(baas.voetenVan(p));
                    richtingen.add(Math.atan2(p.getZ() - baas.getZ(), p.getX() - baas.getX()) + draai);
                }
                if (plekken.isEmpty()) {
                    plekken.add(b);
                    richtingen.add(naarDoel);
                }
            }
            case WILLEKEURIG -> {
                Vec3 m = baas.gevechtsMidden();
                double r = Math.max(4, baas.gevechtsStraal() * 0.75);
                for (int i = 0; i < s.aantal; i++) {
                    double a = baas.getRandom().nextDouble() * Math.PI * 2, rr = Math.sqrt(baas.getRandom().nextDouble()) * r;
                    plekken.add(grond(new Vec3(m.x + Math.cos(a) * rr, m.y + 0.01, m.z + Math.sin(a) * rr)));
                    richtingen.add(baas.getRandom().nextDouble() * Math.PI * 2);
                }
            }
            case MIDDEN -> {
                plekken.add(grond(baas.gevechtsMidden().add(0, 0.01, 0)));
                richtingen.add(naarDoel + draai);
            }
        }
        for (int i = 0; i < plekken.size(); i++) {
            if (s.vorm.soort() == Vorm.Soort.VEILIG) {   // the danger is everywhere else: a red ring from the safe edge to the arena edge
                Vfx.decal(sl, Vorm.donut(s.vorm.r(), Math.max(s.vorm.r() + 4, baas.gevechtsStraal() + 4)), plekken.get(i), 0, s.telegraaf, 0xFF2A2A, groep);
                Vfx.decal(sl, s.vorm, plekken.get(i), 0, s.telegraaf, 0xFF5FC8, groep);
                Vfx.zuil(sl, plekken.get(i), 6, 0xFF8AD0, s.telegraaf);
            } else {
                Vfx.decal(sl, s.vorm, plekken.get(i), richtingen.get(i), s.telegraaf, s.kleur, groep);
            }
        }
        return new Gelegd(s, plekken, richtingen, new boolean[1]);
    }

    /** The ground under a spot (the arena floors are flat; a little search so a decal never floats). */
    private Vec3 grond(Vec3 v) {
        net.minecraft.core.BlockPos.MutableBlockPos m = new net.minecraft.core.BlockPos.MutableBlockPos();
        int x = net.minecraft.util.Mth.floor(v.x), z = net.minecraft.util.Mth.floor(v.z), y0 = net.minecraft.util.Mth.floor(v.y + 0.01);
        for (int dy = 1; dy >= -4; dy--) {
            m.set(x, y0 + dy - 1, z);
            if (!baas.level().getBlockState(m).getCollisionShape(baas.level(), m).isEmpty() && baas.level().getBlockState(m.above()).getCollisionShape(baas.level(), m.above()).isEmpty()) {
                return new Vec3(v.x, y0 + dy, v.z);
            }
        }
        return new Vec3(v.x, Math.floor(v.y + 0.01), v.z);
    }

    private void raak(ServerLevel sl, Gelegd g) {
        Aanval.Stap s = g.stap();
        if (eersteTreffer) {
            eersteTreffer = false;
            BaasAnims.speel(baas, aanval.treffer == null ? "vfx_treffer" : aanval.treffer);
            if (aanval.treffer != null) {
                BaasAnims.speel(baas, "vfx_treffer");
            }
            Vfx.pose(sl, baas, "treffer", 8);
        }
        List<ServerPlayer> vechters = BaasVfx.vechters(baas);
        double slag = baas.tVfxSlag();
        java.util.Set<ServerPlayer> alGeraakt = new java.util.HashSet<>();
        for (int i = 0; i < g.plekken().size(); i++) {
            Vec3 plek = g.plekken().get(i);
            double richting = g.richtingen().get(i);
            List<ServerPlayer> binnen = new ArrayList<>();
            for (ServerPlayer p : vechters) {
                if (s.vorm.soort() == Vorm.Soort.VEILIG) {   // safe spots: hit only who stands in NONE of them (once)
                    if (i == 0 && !veilig(g, p)) {
                        binnen.add(p);
                    }
                } else if (Math.abs(p.getY() - plek.y) < 3 && s.vorm.raakt(plek, richting, p.position()) && (s.stapel || alGeraakt.add(p))) {
                    binnen.add(p);
                }
            }
            double deel = s.stapel ? (binnen.size() <= 1 ? 0.6 : 1.0 / binnen.size()) : 1.0;
            for (ServerPlayer p : binnen) {
                Schade.doe(new Schade.Klap(baas, p, slag * s.factor * deel, baas.element(), Schade.Soort.MAGIE, false, "vfx_" + aanval.id));
                if (s.status != null) {
                    Statussen.geef(p, s.status, s.stTicks, Math.max(0.4, baas.mobLevel() * 0.5), baas);
                }
                if (s.duw > 0) {
                    Vec3 weg = p.position().subtract(plek).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 0.01 ? new Vec3(Math.cos(richting), 0, Math.sin(richting)) : weg.normalize();
                    p.setDeltaMovement(weg.x * s.duw, 0.45, weg.z * s.duw);
                    p.hurtMarked = true;
                }
                geraakt++;
            }
            effect(sl, s, plek, richting);
        }
        Vfx.schud(sl, g.plekken().isEmpty() ? baas.position() : g.plekken().get(0), 24, s.schud, 10);
    }

    private static boolean veilig(Gelegd g, ServerPlayer p) {
        for (Vec3 v : g.plekken()) {
            if (!g.stap().vorm.raakt(v, 0, p.position())) {
                return true;
            }
        }
        return false;
    }

    /** The look of one hit, by shape. */
    private void effect(ServerLevel sl, Aanval.Stap s, Vec3 plek, double richting) {
        int k = s.effect.kleur;
        Vec3 op = plek.add(0, 0.3, 0);
        switch (s.vorm.soort()) {
            case CIRKEL -> {
                Vfx.golf(sl, plek, s.vorm.r() * 1.15, k, 10);
                Vfx.flits(sl, op.add(0, 0.5, 0), Math.max(1.5, s.vorm.r() * 0.7), k);
                Vfx.deeltjes(sl, s.effect.deeltje, op, (int) Math.min(40, 8 + s.vorm.r() * 5), k, s.vorm.r() * 0.6, 0.25);
            }
            case DONUT -> {
                Vfx.golf(sl, plek, s.vorm.r2(), k, 12);
                Vfx.golf(sl, plek, s.vorm.r(), 0xFFFFFF, 8);
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI * 2 * i / 10, r = (s.vorm.r() + s.vorm.r2()) / 2;
                    Vfx.deeltjes(sl, s.effect.deeltje, op.add(Math.cos(a) * r, 0, Math.sin(a) * r), 4, k, 0.8, 0.2);
                }
            }
            case VEILIG -> {
                Vfx.golf(sl, plek, Math.max(s.vorm.r() + 4, baas.gevechtsStraal()), k, 14);
                Vfx.flits(sl, plek.add(0, 1, 0), s.vorm.r(), 0xFF8AD0);
                Vfx.deeltjes(sl, Deeltje.HART, plek.add(0, 1, 0), 10, 0xFF8AD0, s.vorm.r() * 0.5, 0.1);
                Vfx.deeltjes(sl, s.effect.deeltje, baas.gevechtsMidden().add(0, 1, 0), 40, k, baas.gevechtsStraal() * 0.6, 0.3);
            }
            case KEGEL, LIJN, KRUIS -> {
                double len = s.vorm.soort() == Vorm.Soort.KEGEL ? s.vorm.r() : s.vorm.lengte();
                int stappen = (int) Math.min(10, Math.max(3, len / 1.5));
                double[] rs = s.vorm.soort() == Vorm.Soort.KRUIS ? new double[] {richting, richting + Math.PI / 2, richting + Math.PI, richting - Math.PI / 2}
                        : new double[] {richting};
                for (double r : rs) {
                    Vec3 u = new Vec3(Math.cos(r), 0, Math.sin(r));
                    Vfx.straal(sl, op.add(0, 0.6, 0), op.add(u.scale(len)).add(0, 0.6, 0), Math.max(0.6, s.vorm.breedte() * 0.5), k, 8);
                    for (int i = 1; i <= stappen; i++) {
                        Vfx.deeltjes(sl, s.effect.deeltje, op.add(u.scale(len * i / stappen)), 3, k, 0.6, 0.15);
                    }
                }
                Vfx.flits(sl, op.add(0, 0.6, 0), 1.6, k);
            }
        }
    }

    /** Interrupted: decals away, the boss reels (stunned 2.5 s), stars, a cheer. */
    private void onderbreek(ServerLevel sl) {
        onderbroken = true;
        stop(sl, true);
        Statussen.geef(baas, Status.VERDOOFD, 50, 1, null);
        BaasAnims.speel(baas, "vfx_geraakt");
        Vfx.pose(sl, baas, "geraakt", 14);
        Vec3 kop = baas.position().add(0, baas.getBbHeight() + 0.3, 0);
        Vfx.deeltjes(sl, Deeltje.STER, kop, 24, 0xFFF0A0, baas.getBbWidth() * 0.5, 0.15);
        Vfx.flits(sl, kop, 2.5, 0xFFFFFF);
        sl.playSound(null, baas.blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.HOSTILE, 2f, 0.8f);
        for (ServerPlayer p : Vfx.kijkers(baas, sl)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.vfx.onderbroken", Component.translatable(aanval.naamKey())).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Ends it now (interrupted, the boss is gone, a dev reset): decals away, cast bar away. */
    void stop(ServerLevel sl, boolean doorOnderbreking) {
        if (klaar) {
            return;
        }
        klaar = true;
        Vfx.weg(sl, baas.position(), groep);
        if (!doorOnderbreking && eersteTreffer) {
            BaasAnims.speel(baas, "vfx_treffer");   // (release the held wind-up)
        }
        if (aanval.cast) {
            Vfx.castStop(baas, doorOnderbreking);
        }
    }

    @Nullable
    Aanval aanval() {
        return aanval;
    }
}
