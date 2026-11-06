package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.SkillContext;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.jager.Pluisjager;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

/**
 * KERN ADAPTER (see NOTITIES_KLASSEN.md): the Knuffelridder is kern's file ({@code klassen/ridder}) and has no tree yet.
 * This wrapper re-registers the class under the same id ({@link Klassen#registreer} replaces), delegating everything to
 * {@link Knuffelridder#INSTANCE} and adding the skill tree, the job titles, and the node effects: each kern skill is wrapped
 * (extra effect after the kern effect, cooldown/energy tweaks right after kern paid) and the rest runs on the kern
 * damage hooks ({@link Schade#VOOR_KLAP}, {@link Schade#NA_KLAP} filtered on the skill id). At integration the tree and the
 * effects can move into Knuffelridder itself.
 */
public final class RidderMetBoom implements Klasse {
    private static final Knuffelridder K = Knuffelridder.INSTANCE;
    /** Dubbelklap (adv 1): while the echo swing runs, Kussenklap does 60 %. */
    private static final Map<UUID, Long> ECHO = new HashMap<>();
    public static final RidderMetBoom INSTANCE = new RidderMetBoom();
    private final List<BoomKnoop> boom = Bomen.ridder();
    private final List<Vaardigheid> skills;

    private RidderMetBoom() {
        List<Vaardigheid> l = new ArrayList<>();
        for (Vaardigheid v : K.vaardigheden()) {
            l.add(new Omhulsel(v));
        }
        skills = List.copyOf(l);
    }

    public static void init() {
        Klassen.registreer(INSTANCE);
        Schade.VOOR_KLAP.add(RidderMetBoom::voorKlap);
        Schade.NA_KLAP.add(RidderMetBoom::naKlap);
    }

    @Override
    public String id() {
        return K.id();
    }

    @Override
    public String naamKey() {
        return K.naamKey();
    }

    @Override
    public Stats basis(int level) {
        return K.basis(level);
    }

    @Override
    public List<Vaardigheid> vaardigheden() {
        return skills;
    }

    @Override
    public List<BoomKnoop> boom() {
        return boom;
    }

    @Override
    public Set<String> wapenSoorten() {
        return K.wapenSoorten();
    }

    @Override
    public double maxEnergie(int level) {
        return K.maxEnergie(level);
    }

    @Override
    public double energieRegen(int level) {
        return K.energieRegen(level);
    }

    @Override
    public String titelKey(int level) {
        return level >= 20 ? "vadsrijk.klassen.titel.knuffelridder.2" : level >= 10 ? "vadsrijk.klassen.titel.knuffelridder.1" : naamKey();
    }

    /** A kern skill with the tree's extras around it. */
    private static final class Omhulsel implements Vaardigheid {
        private final Vaardigheid v;

        Omhulsel(Vaardigheid v) {
            this.v = v;
        }

        @Override
        public String id() {
            return v.id();
        }

        @Override
        public int slot() {
            return v.slot();
        }

        @Override
        public double energie() {
            return v.energie();
        }

        @Override
        public int cooldownTicks() {
            return v.cooldownTicks();
        }

        @Override
        public boolean ult() {
            return v.ult();
        }

        @Override
        public String naamKey() {
            return v.naamKey();
        }

        @Override
        public Identifier icoon() {
            return v.icoon();
        }

        @Override
        public boolean gebruik(SkillContext c) {
            if (!v.gebruik(c)) {
                return false;
            }
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            switch (v.id()) {
                case "kussenklap" -> {
                    // (r_kussenstorm: -1.5 s cooldown and 8 energy cheaper, KlassenRegistry.kernHaken via kern's COOLDOWN_MOD/ENERGIE_MOD)
                    if (nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs.heeft(p, nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec.SCHILDRIDDER, 1)) {   // U-SPECS: Dubbelklap = Schildridder step 1
                        Later.na(6, () -> {
                            if (p.isRemoved() || !Vadsrijk.in(p)) {
                                return;
                            }
                            ECHO.put(p.getUUID(), p.level().getGameTime());
                            v.gebruik(new SkillContext(p, Spelers.stats(p), c.level(), null, Spelers.wapenElement(p), Spelers.wapenSchade(p)));
                            ECHO.remove(p.getUUID());
                            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 10, 1.2, 0.3, 1.2, 0.02);
                        });
                    }
                }
                case "stormsprong" -> {
                    if (Boom.heeft(p, "r_aanloop")) {
                        // (-3 s cooldown: KlassenRegistry.kernHaken via kern's COOLDOWN_MOD)
                        Statussen.geef(p, Status.SNELPOOTJES, 100, 1, p);
                    }
                }
                case "knuffelschild" -> {
                    if (Boom.heeft(p, "r_doornknuffel")) {
                        Schade.heal(p, p, Spelers.maxVads(p) * 0.08);
                        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2, p.getZ(), 4, 0.4, 0.2, 0.4, 0);
                    }
                    if (Boom.heeft(p, "r_beschermknuffel")) {
                        double n = Spelers.maxVads(p) * 0.15;
                        for (ServerPlayer l : Fx.party(p, 8)) {
                            if (l != p) {
                                Statussen.geef(l, Status.KNUFFELSCHILD, 100, n, p);
                                sl.sendParticles(ParticleTypes.END_ROD, l.getX(), l.getY() + 1, l.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
                            }
                        }
                        Fx.ring(sl, Fx.stof(Fx.ROZE, 1.4f), p.position(), 8, 48);
                    }
                }
                case "vahoege_plof" -> {
                    if (Boom.heeft(p, "r_reuzenkussen")) {
                        Vec3 doel = c.doel() != null ? c.doel().position() : p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
                        Later.na(16, () -> {
                            for (ServerPlayer l : Fx.party(p, 64)) {
                                if (l == p || l.position().distanceTo(doel) <= 8) {
                                    Schade.heal(p, l, Spelers.maxVads(l) * 0.15);
                                    Statussen.geef(l, Status.VAHOEG, 120, 0.2, p);
                                }
                            }
                            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, doel.x, doel.y + 1, doel.z, 40, 3, 0.8, 3, 0.2);
                        });
                    }
                }
                default -> {
                }
            }
            return true;
        }
    }

    private static double voorKlap(Schade.Klap k, double n) {
        if (k.bron() instanceof ServerPlayer p && "kussenklap".equals(k.skillId())) {
            if (ECHO.containsKey(p.getUUID())) {
                n *= 0.6;
            }
            if (Spelers.heeftKnoop(p, "r_kussenstorm")) {
                n *= 1.25;
            }
        }
        if (k.doel() instanceof ServerPlayer d && k.soort() != Schade.Soort.WAAR && Spelers.klasse(d).id().equals(Klassen.RIDDER)) {
            if (Spelers.heeftKnoop(d, "r_taai") && Spelers.vads(d) < Spelers.maxVads(d) * 0.35) {
                n *= 0.7;
            }
            if (Knuffelridder.blokt(d) && Spelers.heeftKnoop(d, "r_doornknuffel") && k.bron() != null && k.bron() != d) {
                // Doornenknuffel: kern bounces 50 %, this is the other 50 %
                // (kern's block already ran: n is 20 % of the hit, kern bounced 50 % of the full hit)
                LivingEntity bron = k.bron();
                double terug = n / 0.2 * 0.5;
                Later.na(0, () -> Schade.doe(new Schade.Klap(d, bron, terug, Element.ROZE, Schade.Soort.WAAR, false, "doornknuffel")));
            }
        }
        if (k.doel() instanceof ServerPlayer d && k.soort() != Schade.Soort.WAAR && k.bron() instanceof VadsrijkMob) {
            // Knuffelbolwerk: a blocking Knuffelridder with the node protects the party within 8 blocks (-25 %)
            for (ServerPlayer r : Fx.party(d, 8)) {
                if (r != d && Knuffelridder.blokt(r) && Spelers.heeftKnoop(r, "r_bolwerk")) {
                    n *= 0.75;
                    break;
                }
            }
            if (Knuffelridder.blokt(d) && Spelers.heeftKnoop(d, "r_bolwerk")) {
                n *= 0.75;
            }
        }
        return n;
    }

    private static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (!(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m) || m.geplofd() || u.schade() <= 0) {
            return;
        }
        if (!Spelers.klasse(p).id().equals(Klassen.RIDDER)) {
            return;
        }
        switch (k.skillId()) {
            case "kussenklap" -> {
                if (Spelers.heeftKnoop(p, "r_vuurkussen")) {
                    Statussen.geef(m, Status.BRANDEND, 80, Math.max(1, Spelers.wapenSchade(p) * 0.25), p);
                    ((ServerLevel) m.level()).sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + 0.8, m.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
                } else if (Spelers.heeftKnoop(p, "r_slijmkussen")) {
                    Statussen.geef(m, Status.PLAKKERIG, 80, 1, p);
                    Later.na(1, () -> {
                        // pull instead of push (after kern's knockback)
                        Vec3 naar = p.position().subtract(m.position()).multiply(1, 0, 1);
                        double d = naar.length();
                        if (d > 1.2) {
                            Vec3 v = naar.normalize().scale(Math.min(1.1, d * 0.3));
                            m.setDeltaMovement(v.x, 0.25, v.z);
                            m.hurtMarked = true;
                        }
                    });
                    ((ServerLevel) m.level()).sendParticles(ParticleTypes.ITEM_SLIME, m.getX(), m.getY() + 0.8, m.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                }
            }
            case "stormsprong" -> {
                if (Spelers.heeftKnoop(p, "r_schokgolf")) {
                    Statussen.geef(m, Status.VERDOOFD, 30, 1, p);
                }
            }
            default -> {
            }
        }
    }

    /** Paladijnsaura (U-SPECS: the Tank branch keystone r_paladijnsaura, was adv 2): every 2 s you and your party within 10 blocks heal 1.5 % of their Vads. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!Spelers.klasse(p).id().equals(Klassen.RIDDER) || !Spelers.heeftKnoop(p, "r_paladijnsaura")) {
                continue;
            }
            for (ServerPlayer l : Fx.party(p, 10)) {
                if (Spelers.vads(l) < Spelers.maxVads(l)) {
                    Schade.heal(p, l, Spelers.maxVads(l) * 0.015);
                }
            }
            Fx.ring(sl, Fx.stof(Fx.GOUD, 0.9f), p.position(), 1.2, 10);
        }
    }
}
