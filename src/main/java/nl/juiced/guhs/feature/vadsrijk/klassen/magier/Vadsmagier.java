package nl.juiced.guhs.feature.vadsrijk.klassen.magier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import nl.juiced.guhs.feature.vadsrijk.klassen.Basisaanval;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.jager.Pluisjager;
import nl.juiced.guhs.feature.vadsrijk.klassen.schot.Schoten;
import nl.juiced.guhs.storage.Nbt;

/**
 * De Vadsmagier (ronde 8): elements + support. Staves, wands and books; the basic attack is a magic bolt ({@link Basisaanval}).
 * <ul>
 *   <li>R Roze Vuurbal: a slow ball that explodes (radius 3.5, 2x weapon damage) in the element of your weapon, with that
 *   element's status (Vuur: Brandend, Natuur: Gesporen, Water: Plakkerig; Roze/none: Roze, x1.5 on ontvadst creatures).</li>
 *   <li>V Blink: a short teleport (up to 10 blocks where you look, stops before walls).</li>
 *   <li>G Vadsbron: a heal circle at your feet (radius 4, 6 s): everyone inside heals 4 % of their Vads + 0.25x weapon damage each second.</li>
 *   <li>Z Meteoorguh (ult): a giant guh-meteor falls on the aimed spot (5x, radius 6) and leaves pink fire for 6 s.</li>
 * </ul>
 */
public final class Vadsmagier implements Klasse {
    public static final Vadsmagier INSTANCE = new Vadsmagier();
    private final List<Vaardigheid> skills = List.of(new RozeVuurbal(), new Blink(), new Vadsbron(), new Meteoorguh());
    private final List<BoomKnoop> boom = Bomen.magier();
    /** Vadsoverloop (adv 2): skills cast since the last free one. */
    private static final Map<UUID, Integer> OVERLOOP = new HashMap<>();

    private Vadsmagier() {
    }

    public static void init() {
        Klassen.registreer(INSTANCE);
    }

    @Override
    public String id() {
        return Klassen.MAGIER;
    }

    @Override
    public Stats basis(int level) {
        int l = level - 1;
        return Stats.van(Stat.VADS, 80 + 10 * l, Stat.KRACHT, 2 + 0.4 * l, Stat.BEHENDIGHEID, 4 + 0.5 * l, Stat.WIJSHEID, 11 + 2.1 * l,
                Stat.VERDEDIGING, 4 + 0.9 * l, Stat.KRITIEK, 5, Stat.ELEMENTSCHADE, 5, Stat.HEALKRACHT, 10);
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
        return Set.of("staf", "toverstok", "boek");
    }

    @Override
    public double maxEnergie(int level) {
        return 130 + 3 * (level - 1);
    }

    @Override
    public double energieRegen(int level) {
        return 8;
    }

    @Override
    public String titelKey(int level) {
        return level >= 20 ? "vadsrijk.klassen.titel.vadsmagier.2" : level >= 10 ? "vadsrijk.klassen.titel.vadsmagier.1" : naamKey();
    }

    /** Vadsoverloop: every 3rd skill (not the ult) gives its energy back. Called after a cast. */
    static void overloop(ServerPlayer p, double energie) {
        if (!Boom.heeft(p, "m_adv2") || energie <= 0) {
            return;
        }
        int n = OVERLOOP.merge(p.getUUID(), 1, Integer::sum);
        if (n >= 3) {
            OVERLOOP.put(p.getUUID(), 0);
            Later.na(0, () -> {
                SpelerData d = Spelers.data(p);
                d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + energie);
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.4, 0.6, 0.4, 0.05);
            });
        }
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

        @Override
        public final boolean gebruik(SkillContext c) {
            boolean ok = doe(c);
            if (ok && !ult()) {
                overloop(c.speler(), energie);
            }
            return ok;
        }

        abstract boolean doe(SkillContext c);
    }

    // ---- R: Roze Vuurbal ------------------------------------------------------------------------------------------------

    static final class RozeVuurbal extends Skill {
        RozeVuurbal() {
            super("roze_vuurbal", 0, 25, 100);
        }

        @Override
        boolean doe(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Element el = c.element() == Element.GEEN ? Element.ROZE : c.element();
            boolean groot = Boom.heeft(p, "m_reuzenbal"), drie = Boom.heeft(p, "m_drievuur");
            Vec3 kijk = p.getLookAngle();
            if (drie) {
                for (int z = -1; z <= 1; z++) {
                    bal(p, kijk.yRot((float) Math.toRadians(14 * z)), c.wapenSchade() * 2.0 * 0.65, 2.6, el);
                }
            } else {
                bal(p, kijk, c.wapenSchade() * 2.0 * (groot ? 1.2 : 1), groot ? 5.25 : 3.5, el);
            }
            Fx.geluid(sl, p.position(), SoundEvents.FIRECHARGE_USE, 0.9f, el == Element.VUUR ? 0.9f : 1.4f);
            Fx.geluid(sl, p.position(), SoundEvents.EVOKER_CAST_SPELL, 0.5f, 1.5f);
            return true;
        }

        private static void bal(ServerPlayer p, Vec3 richting, double schade, double straal, Element el) {
            Schoten.Schot s = Schoten.vuur(p, p.getEyePosition().add(richting.scale(0.8)).add(0, -0.2, 0), richting.normalize().scale(1.15));
            s.straal = 0.6;
            s.maxLeeftijd = 30;
            int k = Fx.kleur(el);
            s.spoor = (l, a, b, t) -> {
                Fx.lijn(l, Fx.stof(k, 2.4f), a, b, 0.3);
                l.sendParticles(el == Element.VUUR ? ParticleTypes.FLAME : el == Element.WATER ? ParticleTypes.DRIPPING_WATER
                        : el == Element.NATUUR ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.CHERRY_LEAVES, b.x, b.y, b.z, 3, 0.2, 0.2, 0.2, 0.01);
                l.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 1, 0.15, 0.15, 0.15, 0.01);
            };
            s.opRaak = (x, e) -> false;
            s.opEinde = (x, waar, blok) -> ontplof(p, waar, straal, schade, el);
        }

        private static void ontplof(ServerPlayer p, Vec3 waar, double straal, double schade, Element el) {
            ServerLevel sl = (ServerLevel) p.level();
            for (LivingEntity e : Fx.mobsBij(sl, waar, straal)) {
                Basisaanval.treffer(p, e, schade, el, Schade.Soort.MAGIE, true, "roze_vuurbal");
                status(p, e, el, schade);
            }
            Fx.plof(sl, waar, straal * 0.7, el);
            Fx.ring(sl, Fx.stof(Fx.kleur(el), 2f), waar, straal, (int) (straal * 8));
            Fx.geluid(sl, waar, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.5f);
            Fx.geluid(sl, waar, SoundEvents.AMETHYST_BLOCK_BREAK, 1f, 0.8f);
            if (Boom.heeft(p, "m_adv1")) {
                // Sterrenwever: an element puddle for 3 s
                Vec3 g = Fx.grond(sl, waar);
                for (int i = 1; i <= 3; i++) {
                    Later.na(i * 20, () -> {
                        Fx.ring(sl, Fx.stof(Fx.kleur(el), 1.6f), g, 2.5, 18);
                        sl.sendParticles(Fx.stof(Fx.kleur(el), 1.2f), g.x, g.y + 0.2, g.z, 14, 1.2, 0.05, 1.2, 0);
                        for (LivingEntity e : Fx.mobsBij(sl, g, 2.5)) {
                            Basisaanval.treffer(p, e, schade * 0.175, el, Schade.Soort.MAGIE, false, "sterrenwever");
                            status(p, e, el, schade);
                        }
                    });
                }
            }
        }

        static void status(ServerPlayer p, LivingEntity e, Element el, double schade) {
            switch (el) {
                case VUUR -> Statussen.geef(e, Status.BRANDEND, 80, Math.max(1, schade * 0.12), p);
                case NATUUR -> Statussen.geef(e, Status.GESPOREN, 80, Math.max(1, schade * 0.1), p);
                case WATER -> Statussen.geef(e, Status.PLAKKERIG, 80, 1, p);
                default -> {
                }
            }
        }
    }

    // ---- V: Blink -------------------------------------------------------------------------------------------------------

    static final class Blink extends Skill {
        Blink() {
            super("blink", 1, 20, 120);
        }

        @Override
        boolean doe(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 kijk = p.getLookAngle();
            if (kijk.y < -0.3) {
                kijk = new Vec3(kijk.x, -0.3, kijk.z).normalize();
            }
            Vec3 oud = p.position();
            Vec3 best = null;
            for (double d = 10; d >= 1.5; d -= 0.5) {
                Vec3 v = oud.add(kijk.scale(d));
                if (sl.noCollision(p, p.getBoundingBox().move(v.subtract(oud))) && vrijPad(sl, p.getEyePosition(), p.getEyePosition().add(kijk.scale(d)))) {
                    best = v;
                    break;
                }
            }
            if (best == null) {
                return false;
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, oud.x, oud.y + 1, oud.z, 30, 0.3, 0.6, 0.3, 0.05);
            sl.sendParticles(Fx.stof(Fx.ROZE, 1.6f), oud.x, oud.y + 1, oud.z, 16, 0.3, 0.6, 0.3, 0.02);
            Fx.lijn(sl, Fx.stof(Fx.LICHTROZE, 0.8f), oud.add(0, 1, 0), best.add(0, 1, 0), 0.5);
            p.teleportTo(best.x, best.y, best.z);
            p.resetFallDistance();
            sl.sendParticles(ParticleTypes.END_ROD, best.x, best.y + 1, best.z, 16, 0.3, 0.6, 0.3, 0.05);
            Fx.geluid(sl, oud, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.5f);
            Fx.geluid(sl, best, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.8f);
            if (Boom.heeft(p, "m_glitterblink")) {
                Element el = c.element() == Element.GEEN ? Element.ROZE : c.element();
                Vec3 o = oud;
                Later.na(4, () -> {
                    Basisaanval.gebied(p, o, 3, c.wapenSchade() * 1.2, el, Schade.Soort.MAGIE, "glitterblink", null);
                    Fx.plof(sl, o, 1.6, el);
                    Fx.geluid(sl, o, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f, 1.2f);
                });
            }
            if (Boom.heeft(p, "m_tijdsprong")) {
                Later.na(0, () -> {
                    SpelerData d = Spelers.data(p);
                    d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + 15);
                    d.klaarOp.remove("roze_vuurbal");
                });
            }
            return true;
        }

        private static boolean vrijPad(ServerLevel sl, Vec3 a, Vec3 b) {
            return sl.clip(new net.minecraft.world.level.ClipContext(a, b, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()))
                    .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
        }
    }

    // ---- G: Vadsbron ----------------------------------------------------------------------------------------------------

    static final class Vadsbron extends Skill {
        Vadsbron() {
            super("vadsbron", 2, 35, 280);
        }

        @Override
        boolean doe(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 plek = Fx.grond(sl, p.position());
            boolean groot = Boom.heeft(p, "m_grote_bron"), schild = Boom.heeft(p, "m_bronschild");
            double r = groot ? 6 : 4;
            double wapen = c.wapenSchade();
            Set<UUID> beschermd = new HashSet<>();
            Fx.geluid(sl, plek, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
            Fx.geluid(sl, plek, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.0f);
            for (int t = 0; t <= 120; t += 5) {
                int tt = t;
                Later.na(t, () -> {
                    if (p.isRemoved()) {
                        return;
                    }
                    Fx.ring(sl, Fx.stof(Fx.ROZE, 1.5f), plek, r, (int) (r * 7));
                    Fx.ring(sl, Fx.stof(Fx.LICHTROZE, 1.0f), plek, r * (tt % 20) / 20.0, (int) (r * 4));
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, plek.x, plek.y + 0.5, plek.z, 3, r * 0.4, 0.2, r * 0.4, 0);
                    if (tt % 20 != 0) {
                        return;
                    }
                    sl.sendParticles(ParticleTypes.HEART, plek.x, plek.y + 1, plek.z, 3, r * 0.4, 0.3, r * 0.4, 0);
                    for (ServerPlayer s : Fx.vriendenBij(p, plek, r)) {
                        Schade.heal(p, s, Spelers.maxVads(s) * 0.04 + wapen * 0.25);
                        if (groot) {
                            Statussen.geef(s, Status.VAHOEG, 30, 0.15, p);
                        }
                        if (schild && beschermd.add(s.getUUID())) {
                            Statussen.geef(s, Status.KNUFFELSCHILD, 100, Spelers.maxVads(p) * 0.12, p);
                            sl.sendParticles(ParticleTypes.END_ROD, s.getX(), s.getY() + 1, s.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
                        }
                    }
                });
            }
            return true;
        }
    }

    // ---- Z: Meteoorguh --------------------------------------------------------------------------------------------------

    static final class Meteoorguh extends Skill {
        Meteoorguh() {
            super("meteoorguh", 3, 0, 200);
        }

        @Override
        boolean doe(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 doel = Fx.mikpunt(p, c.doel(), 30);
            double w = c.wapenSchade();
            meteoor(p, sl, doel, 4.5f, 30, 6, w * 5, true);
            if (Boom.heeft(p, "m_meteoorregen")) {
                for (int i = 0; i < 3; i++) {
                    double a = Math.PI * 2 * i / 3 + sl.getRandom().nextDouble();
                    Vec3 v = Fx.grond(sl, doel.add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
                    int na = 10 + 6 * i;
                    Later.na(na, () -> meteoor(p, sl, v, 1.8f, 24, 3, w * 1.5, false));
                }
            }
            Fx.geluid(sl, p.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 0.8f);
            return true;
        }

        private static void meteoor(ServerPlayer p, ServerLevel sl, Vec3 doel, float grootte, int val, double straal, double schade, boolean vuurzone) {
            Telegraaf.cirkel(sl, doel, straal, val, Telegraaf.ROZE);
            Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
            if (d == null) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            CompoundTag blok = new CompoundTag();
            blok.putString("Name", "guhs:tegel_guhgezicht");
            tag.put("block_state", blok);
            tag.putInt("teleport_duration", 2);
            tag.putFloat("view_range", 3f);
            CompoundTag tr = new CompoundTag();
            tr.put("translation", Schoten.floats(-grootte / 2, -grootte / 2, -grootte / 2));
            tr.put("left_rotation", Schoten.floats(0.2f, 0.3f, 0.1f, 0.92f));
            tr.put("scale", Schoten.floats(grootte, grootte, grootte));
            tr.put("right_rotation", Schoten.floats(0, 0, 0, 1));
            tag.put("transformation", tr);
            Nbt.load(d, tag);
            Vec3 start = doel.add(-10, 32, -6);
            d.snapTo(start.x, start.y, start.z, 0, 0);
            sl.addFreshEntity(d);
            for (int t = 1; t <= val; t++) {
                int tt = t;
                Later.na(t, () -> {
                    if (d.isRemoved()) {
                        return;
                    }
                    double f = tt / (double) val;
                    Vec3 v = start.add(doel.add(0, grootte / 2, 0).subtract(start).scale(f * f));
                    d.teleportTo(v.x, v.y, v.z);
                    sl.sendParticles(ParticleTypes.FLAME, v.x, v.y, v.z, 4, grootte * 0.3, grootte * 0.3, grootte * 0.3, 0.02);
                    sl.sendParticles(Fx.stof(Fx.ROZE, 3f), v.x, v.y, v.z, 6, grootte * 0.3, grootte * 0.3, grootte * 0.3, 0);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 1, v.z, 2, 0.3, 0.3, 0.3, 0.01);
                    if (tt == val / 2) {
                        Fx.geluid(sl, doel, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f);
                    }
                });
            }
            Later.na(val + 1, () -> {
                d.discard();
                for (LivingEntity e : Fx.mobsBij(sl, doel, straal)) {
                    Basisaanval.treffer(p, e, schade, Element.ROZE, Schade.Soort.MAGIE, true, "meteoorguh");
                    Statussen.geef(e, Status.BRANDEND, 60, Math.max(1, schade * 0.04), p);
                    Vec3 weg = e.position().subtract(doel).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(e.getDeltaMovement().add(weg.x * 0.6, 0.5, weg.z * 0.6));
                    e.hurtMarked = true;
                }
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, doel.x, doel.y + 0.5, doel.z, 1, 0, 0, 0, 0);
                Fx.plof(sl, doel, straal * 0.8, Element.ROZE);
                sl.sendParticles(ParticleTypes.HEART, doel.x, doel.y + 1.5, doel.z, 12, straal * 0.4, 0.5, straal * 0.4, 0);
                Fx.geluid(sl, doel, SoundEvents.GENERIC_EXPLODE.value(), vuurzone ? 2f : 1f, 0.7f);
                Fx.geluid(sl, doel, SoundEvents.WOOL_BREAK, 2f, 0.5f);
                if (vuurzone) {
                    for (int s = 1; s <= 6; s++) {
                        Later.na(s * 20, () -> {
                            sl.sendParticles(Fx.stof(Fx.ROZE, 2f), doel.x, doel.y + 0.3, doel.z, 30, straal * 0.5, 0.2, straal * 0.5, 0);
                            sl.sendParticles(ParticleTypes.FLAME, doel.x, doel.y + 0.3, doel.z, 14, straal * 0.5, 0.2, straal * 0.5, 0.01);
                            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, doel.x, doel.y + 0.6, doel.z, 8, straal * 0.5, 0.3, straal * 0.5, 0.01);
                            for (LivingEntity e : Fx.mobsBij(sl, doel, straal)) {
                                Basisaanval.treffer(p, e, schade * 0.1, Element.ROZE, Schade.Soort.MAGIE, false, "meteoorvuur");
                                Statussen.geef(e, Status.BRANDEND, 40, Math.max(1, schade * 0.03), p);
                            }
                        });
                    }
                }
            });
        }
    }

    /** (shared with the Pluisjager) */
    static void verkort(ServerPlayer p, String skill, int ticks) {
        Pluisjager.verkort(p, skill, ticks);
    }
}
