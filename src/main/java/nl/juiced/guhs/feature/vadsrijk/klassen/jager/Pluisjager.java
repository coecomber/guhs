package nl.juiced.guhs.feature.vadsrijk.klassen.jager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.SkillContext;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Balans;
import nl.juiced.guhs.feature.vadsrijk.klassen.Basisaanval;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.schot.Schoten;

/**
 * De Pluisjager (ronde 8): the mobile archer. Bows, catapults and crossbows; the basic attack is {@link Basisaanval}.
 * <ul>
 *   <li>R Pluizenregen: 3 waves of arrows on the aimed spot (radius 4), 0.6x weapon damage per wave.</li>
 *   <li>V Rolsalto: a backflip away from where you look; your next shot (within 5 s) is a guaranteed crit.</li>
 *   <li>G Knuffelpijl: a heart arrow; a teammate it hits heals 12 % of their Vads + 0.5x weapon damage, an enemy takes 1.8x (Roze).</li>
 *   <li>Z Sterrenschot (ult): 1 s charging, then a huge laser arrow 40 blocks through everything: 4x weapon damage.</li>
 * </ul>
 */
public final class Pluisjager implements Klasse {
    public static final Pluisjager INSTANCE = new Pluisjager();
    private final List<Vaardigheid> skills = List.of(new Pluizenregen(), new Rolsalto(), new Knuffelpijl(), new Sterrenschot());
    private final List<BoomKnoop> boom = Bomen.jager();

    private Pluisjager() {
    }

    public static void init() {
        Klassen.registreer(INSTANCE);
    }

    @Override
    public String id() {
        return Klassen.JAGER;
    }

    @Override
    public Stats basis(int level) {
        int l = level - 1;
        return Stats.van(Stat.VADS, 85 + 11 * l, Stat.KRACHT, 3 + 0.5 * l, Stat.BEHENDIGHEID, 10 + 2 * l, Stat.WIJSHEID, 4 + 0.5 * l,
                Stat.VERDEDIGING, 5 + 1.0 * l, Stat.KRITIEK, 10, Stat.KRITIEKSCHADE, 10, Stat.LOOPSNELHEID, 5);
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
        return Set.of("boog", "katapult", "kruisboog");
    }

    @Override
    public double energieRegen(int level) {
        return 6.5;
    }

    @Override
    public String titelKey(int level) {
        return level >= 20 ? "vadsrijk.klassen.titel.pluisjager.2" : level >= 10 ? "vadsrijk.klassen.titel.pluisjager.1" : naamKey();
    }

    private abstract static class Skill implements Vaardigheid {
        private final String id;
        private final int slot, cd;
        private final double energie;

        Skill(String id, int slot, double energie, int cd) {
            this.id = id;
            this.slot = slot;
            this.energie = energie;
            this.cd = cd;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int slot() {
            return slot;
        }

        @Override
        public double energie() {
            return energie;
        }

        @Override
        public int cooldownTicks() {
            return cd;
        }
    }

    static final class Pluizenregen extends Skill {
        Pluizenregen() {
            super("pluizenregen", 0, 25, 120);
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 doel = Fx.mikpunt(p, c.doel(), 28);
            boolean vuur = Boom.heeft(p, "j_vuurregen"), kleef = Boom.heeft(p, "j_kleefregen");
            int golven = kleef ? 4 : 3;
            double straal = 4;
            Element el = c.element();
            double basis = c.wapenSchade() * Balans.PLUIZENREGEN;
            Telegraaf.cirkel(sl, doel, straal, 8 + 8 * golven, vuur ? 0xFF7A3A : kleef ? 0x7FE07F : Telegraaf.ROZE);
            Fx.geluid(sl, p.position(), SoundEvents.ARROW_SHOOT, 1f, 0.7f);
            Fx.geluid(sl, p.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.8f, 1.4f);
            sl.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 2, p.getZ(), 20, 0.2, 1.5, 0.2, 0.4);
            for (int g = 0; g < golven; g++) {
                int golf = g;
                Later.na(8 + 8 * g, () -> golf(p, sl, doel, straal, basis, el, vuur, kleef, golf));
            }
            return true;
        }

        private static void golf(ServerPlayer p, ServerLevel sl, Vec3 c, double r, double basis, Element el, boolean vuur, boolean kleef, int golf) {
            if (p.isRemoved() || p.level() != sl) {
                return;
            }
            for (int i = 0; i < 6; i++) {
                double a = sl.getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(sl.getRandom().nextDouble()) * r;
                Vec3 boven = c.add(Math.cos(a) * d + 1.5, 9, Math.sin(a) * d + 0.5);
                Schoten.Schot s = Schoten.vuur(p, boven, new Vec3(-0.25, -1.8, -0.08));
                s.maxLeeftijd = 8;
                s.straal = 0;
                s.opRaak = (x, e) -> false;
                Schoten.metBeeld(s, "minecraft:arrow", 0.8f, true);
                s.spoor = (l, van, naar, t) -> l.sendParticles(vuur ? ParticleTypes.SMALL_FLAME : ParticleTypes.CRIT, naar.x, naar.y, naar.z, 1, 0, 0, 0, 0);
            }
            Later.na(4, () -> {
                double tot = 0;
                for (LivingEntity e : Fx.mobsBij(sl, c, r)) {
                    tot += Basisaanval.treffer(p, e, basis, el, Schade.Soort.AFSTAND, false, "pluizenregen").schade();
                    if (vuur) {
                        Statussen.geef(e, Status.BRANDEND, 80, Math.max(1, basis * 0.4), p);
                    }
                    if (kleef) {
                        Statussen.geef(e, Status.PLAKKERIG, 80, 1, p);
                    }
                }
                sl.sendParticles(vuur ? ParticleTypes.FLAME : kleef ? ParticleTypes.ITEM_SLIME : ParticleTypes.CHERRY_LEAVES, c.x, c.y + 0.3, c.z, 30, r * 0.5, 0.2, r * 0.5, 0.02);
                Fx.geluid(sl, c, SoundEvents.ARROW_HIT, 1f, 0.8f + golf * 0.1f);
            });
        }
    }

    static final class Rolsalto extends Skill {
        Rolsalto() {
            super("rolsalto", 1, 20, 160);
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 terug = p.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.45);
            Vec3 oud = p.position();
            Fx.duw(p, new Vec3(terug.x, 0.55, terug.z));
            p.resetFallDistance();
            long nu = sl.getGameTime();
            Basisaanval.geefSaltoKrit(p, nu + 100);
            Statussen.geef(p, Status.SNELPOOTJES, 40, 1, p);
            sl.sendParticles(ParticleTypes.CLOUD, oud.x, oud.y + 0.2, oud.z, 14, 0.4, 0.1, 0.4, 0.04);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, oud.x, oud.y + 1, oud.z, 12, 0.4, 0.5, 0.4, 0.02);
            Fx.geluid(sl, oud, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.5f);
            Fx.geluid(sl, oud, SoundEvents.ENDER_DRAGON_FLAP, 0.4f, 2f);
            if (Boom.heeft(p, "j_plakbom")) {
                Later.na(10, () -> {
                    sl.sendParticles(ParticleTypes.ITEM_SLIME, oud.x, oud.y + 0.4, oud.z, 40, 1.3, 0.3, 1.3, 0.05);
                    Fx.geluid(sl, oud, SoundEvents.SLIME_SQUISH, 1f, 0.8f);
                    for (LivingEntity e : Fx.mobsBij(sl, oud, 3)) {
                        Statussen.geef(e, Status.PLAKKERIG, 60, 1, p);
                        Basisaanval.treffer(p, e, c.wapenSchade() * Balans.PLAKBOM, Element.WATER, Schade.Soort.AFSTAND, false, "plakbom");
                    }
                });
                // (the -2 s cooldown: KlassenRegistry.kernHaken via kern's COOLDOWN_MOD)
            }
            if (Boom.heeft(p, "j_pluizenmantel")) {
                Statussen.geef(p, Status.KNUFFELSCHILD, 80, Spelers.maxVads(p) * 0.2, p);
                sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 16, 0.5, 0.6, 0.5, 0.02);
            }
            if (Boom.heeft(p, "j_adv1")) {
                // Saltosalvo: mid-flip, 3 arrows at the nearest enemy
                for (int i = 0; i < 3; i++) {
                    Later.na(4 + 3 * i, () -> salvo(p, c));
                }
            }
            return true;
        }

        private static void salvo(ServerPlayer p, SkillContext c) {
            if (p.isRemoved()) {
                return;
            }
            LivingEntity doel = null;
            double best = 22 * 22;
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(22),
                    x -> x instanceof VadsrijkMob m && !m.geplofd())) {
                double d = e.distanceToSqr(p);
                if (d < best) {
                    best = d;
                    doel = e;
                }
            }
            Vec3 richting = doel != null ? doel.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize() : p.getLookAngle();
            Basisaanval.vuur(p, "boog", Basisaanval.SOORTEN.get("boog"), richting, c.wapenSchade() * Balans.SALVO, c.element(), false, false);
            Fx.geluid((ServerLevel) p.level(), p.position(), SoundEvents.ARROW_SHOOT, 0.6f, 1.6f);
        }
    }

    static final class Knuffelpijl extends Skill {
        Knuffelpijl() {
            super("knuffelpijl", 2, 25, 140);
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            boolean hartjes = Boom.heeft(p, "j_hartjespijl");
            Vec3 kijk = p.getLookAngle();
            Schoten.Schot s = Schoten.vuur(p, p.getEyePosition().add(kijk.scale(0.5)).add(0, -0.12, 0), kijk.scale(2.4));
            s.raaktSpelers = true;
            s.straal = 0.6;
            s.maxLeeftijd = 18;
            s.zwaartekracht = 0.005;
            Schoten.metBeeld(s, "minecraft:tipped_arrow", 1.0f, true);
            s.spoor = (l, a, b, t) -> {
                Fx.lijn(l, Fx.stof(Fx.ROZE, 1.0f), a, b, 0.5);
                if (t % 2 == 0) {
                    l.sendParticles(ParticleTypes.HEART, b.x, b.y, b.z, 1, 0, 0, 0, 0);
                }
            };
            s.opRaak = (schot, doel) -> {
                if (doel instanceof ServerPlayer vriend) {
                    double n = (Spelers.maxVads(vriend) * 0.12 + c.wapenSchade() * 0.5) * (hartjes ? 1.6 : 1);
                    nl.juiced.guhs.feature.vadsrijk.kern.Schade.heal(p, vriend, n);
                    if (hartjes) {
                        Statussen.geef(vriend, Status.SNELPOOTJES, 80, 1, p);
                    }
                    sl.sendParticles(ParticleTypes.HEART, vriend.getX(), vriend.getY() + 1.8, vriend.getZ(), 10, 0.5, 0.4, 0.5, 0);
                    Fx.geluid(sl, vriend.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                } else {
                    boolean krit = Basisaanval.neemSaltoKrit(p);
                    Basisaanval.treffer(p, doel, c.wapenSchade() * Balans.KNUFFELPIJL * (hartjes ? 1.3 : 1), Element.ROZE, Schade.Soort.AFSTAND, krit, "knuffelpijl");
                    sl.sendParticles(Fx.stof(Fx.ROZE, 2f), doel.getX(), doel.getY() + 1, doel.getZ(), 16, 0.4, 0.4, 0.4, 0.02);
                }
                return false;
            };
            Fx.geluid(sl, p.position(), SoundEvents.ARROW_SHOOT, 1f, 1.8f);
            Fx.geluid(sl, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 1.6f);
            return true;
        }
    }

    static final class Sterrenschot extends Skill {
        Sterrenschot() {
            super("sterrenschot", 3, 0, 200);
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Statussen.geef(p, Status.PLAKKERIG, 20, 1, p);
            Fx.geluid(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 1.6f);
            for (int t = 0; t < 20; t += 2) {
                int tt = t;
                Later.na(t, () -> {
                    Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(1.2));
                    double r = 2.0 * (1 - tt / 20.0);
                    for (int i = 0; i < 6; i++) {
                        double a = tt * 0.6 + i * Math.PI / 3;
                        sl.sendParticles(ParticleTypes.END_ROD, hand.x + Math.cos(a) * r, hand.y + Math.sin(a) * r * 0.6, hand.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                    sl.sendParticles(Fx.stof(Fx.GOUD, 1.6f), hand.x, hand.y, hand.z, 3, 0.1, 0.1, 0.1, 0);
                    if (tt % 4 == 0) {
                        Fx.geluid(sl, p.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 0.6f + tt * 0.06f);
                    }
                });
            }
            Later.na(20, () -> laser(p, c));
            return true;
        }

        private static void laser(ServerPlayer p, SkillContext c) {
            if (p.isRemoved() || !(p.level() instanceof ServerLevel sl)) {
                return;
            }
            boolean ster = Boom.heeft(p, "j_vallende_ster");
            Vec3 a = p.getEyePosition().add(0, -0.2, 0);
            Vec3 b = a.add(p.getLookAngle().scale(40));
            double tot = 0;
            Set<UUID> al = new HashSet<>();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(1.6), x -> x instanceof VadsrijkMob m && !m.geplofd())) {
                if (e.getBoundingBox().inflate(1.3).clip(a, b).isPresent() && al.add(e.getUUID())) {
                    boolean krit = Basisaanval.neemSaltoKrit(p);
                    tot += Basisaanval.treffer(p, e, c.wapenSchade() * Balans.STERRENSCHOT * (ster ? 1.5 : 1), c.element() == Element.GEEN ? Element.ROZE : c.element(),
                            Schade.Soort.AFSTAND, krit, "sterrenschot").schade();
                    sl.sendParticles(ParticleTypes.FIREWORK, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.5, 0.3, 0.15);
                }
            }
            Fx.lijn(sl, ParticleTypes.END_ROD, a, b, 0.4);
            Fx.lijn(sl, Fx.stof(Fx.ROZE, 2.5f), a, b, 0.3);
            Fx.lijn(sl, Fx.stof(Fx.GOUD, 1.5f), a.add(0, 0.15, 0), b.add(0, 0.15, 0), 0.6);
            Fx.geluid(sl, p.position(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.8f);
            Fx.geluid(sl, p.position(), SoundEvents.TRIDENT_THUNDER.value(), 0.4f, 1.8f);
            Fx.duw(p, p.getLookAngle().scale(-0.4).add(0, 0.1, 0));
            if (ster) {
                SpelerData d = Spelers.data(p);
                Later.na(0, () -> d.ultLading = Math.min(100, d.ultLading + 25));
            }
        }
    }

    /** Takes {@code ticks} off the cooldown kern just set for this skill (call with Later.na(0, ...)). */
    public static void verkort(ServerPlayer p, String skill, int ticks) {
        SpelerData d = Spelers.data(p);
        Long t = d.klaarOp.get(skill);
        if (t != null) {
            d.klaarOp.put(skill, t - ticks);
        }
    }
}
