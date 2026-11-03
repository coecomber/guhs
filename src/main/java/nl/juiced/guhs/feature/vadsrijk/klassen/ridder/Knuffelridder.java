package nl.juiced.guhs.feature.vadsrijk.klassen.ridder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.SkillContext;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.storage.Nbt;

/**
 * De Knuffelridder (ronde 8): heavy bruiser, a bit of a tank. Swords, clubs and hammers.
 * <ul>
 *   <li>R Kussenklap: a swing in a cone in front (110 degrees, 4.5 blocks), 1.6x weapon damage, big knockback.</li>
 *   <li>V Stormsprong: a leap to the target under the crosshair (or 8 blocks forward); the landing is a shockwave (3.5 blocks, 1.3x).</li>
 *   <li>G Knuffelschild: 3 s: hits on you do 20 %, half of each hit bounces back, every mob within 10 blocks turns to you.</li>
 *   <li>Z Vahoege Plof (ult, charges by dealing damage): a giant pillow falls from the sky on the target: 3x damage + stunned
 *   3 s within 6 blocks, and the party within 8 blocks heals 15 % of their Vads.</li>
 * </ul>
 * Placeholder FX: particles, vanilla sounds and a falling pink wool block display.
 */
public final class Knuffelridder implements Klasse {
    public static final Knuffelridder INSTANCE = new Knuffelridder();
    private static final Map<UUID, Long> BLOK_TOT = new HashMap<>();
    private static final Map<UUID, Long> SPRONG = new HashMap<>();

    private record Kussen(ServerLevel level, Display.BlockDisplay d, Vec3 doel, UUID eigenaar, double schade, int[] t) {
    }

    private static final List<Kussen> KUSSENS = new ArrayList<>();

    private final List<Vaardigheid> skills = List.of(new Kussenklap(), new Stormsprong(), new Knuffelschild(), new VahoegePlof());

    private Knuffelridder() {
    }

    public static void init() {
        Klassen.registreer(INSTANCE);
        Schade.VOOR_KLAP.add((k, n) -> {
            if (k.doel() instanceof ServerPlayer p && blokt(p) && k.soort() != Schade.Soort.WAAR) {
                if (k.bron() != null && k.bron() != p) {
                    Schade.doe(new Schade.Klap(p, k.bron(), n * 0.5, Element.ROZE, Schade.Soort.WAAR, false, "knuffelschild_kaats"));
                }
                if (p.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.5, 0.4, 0.1);
                    sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1f, 1.3f);
                }
                return n * 0.2;
            }
            return n;
        });
    }

    public static boolean blokt(ServerPlayer p) {
        Long t = BLOK_TOT.get(p.getUUID());
        return t != null && p.level().getGameTime() < t;
    }

    @Override
    public String id() {
        return Klassen.RIDDER;
    }

    @Override
    public String naamKey() {
        return "vadsrijk.kern.klasse.knuffelridder";
    }

    @Override
    public Stats basis(int level) {
        int l = level - 1;
        return Stats.van(Stat.VADS, 100 + 14 * l, Stat.KRACHT, 10 + 2 * l, Stat.BEHENDIGHEID, 4 + 0.5 * l, Stat.WIJSHEID, 3 + 0.5 * l,
                Stat.VERDEDIGING, 8 + 1.5 * l, Stat.KRITIEK, 5, Stat.KRITIEKSCHADE, 0);
    }

    @Override
    public List<Vaardigheid> vaardigheden() {
        return skills;
    }

    @Override
    public Set<String> wapenSoorten() {
        return Set.of("zwaard", "knots", "hamer");
    }

    // ---- the skills -----------------------------------------------------------------------------------------------------

    static final class Kussenklap implements Vaardigheid {
        @Override
        public String id() {
            return "kussenklap";
        }

        @Override
        public String naamKey() {
            return "vadsrijk.kern.skill.kussenklap";
        }

        @Override
        public int slot() {
            return 0;
        }

        @Override
        public double energie() {
            return 20;
        }

        @Override
        public int cooldownTicks() {
            return 60;
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
            double totaal = 0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5, 2, 4.5), x -> x instanceof VadsrijkMob m && !m.geplofd())) {
                Vec3 naar = e.position().subtract(p.position()).multiply(1, 0, 1);
                if (naar.length() > 4.8 || (naar.length() > 0.8 && naar.normalize().dot(kijk) < Math.cos(Math.toRadians(55)))) {
                    continue;
                }
                Schade.Uitkomst u = Schade.doe(new Schade.Klap(p, e, c.wapenSchade() * 1.6, c.element(), Schade.Soort.MELEE, true, id()));
                totaal += u.schade();
                e.knockback(1.2, -kijk.x, -kijk.z);
                e.hurtMarked = true;
            }
            Spelers.laadUlt(p, totaal);
            for (int i = -5; i <= 5; i++) {
                double a = Math.atan2(kijk.z, kijk.x) + Math.toRadians(i * 11);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 2.6, p.getY() + 1.1, p.getZ() + Math.sin(a) * 2.6, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX() + Math.cos(a) * 3.2, p.getY() + 1.0, p.getZ() + Math.sin(a) * 3.2, 2, 0.1, 0.2, 0.1, 0.02);
            }
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
            sl.playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            return true;
        }
    }

    static final class Stormsprong implements Vaardigheid {
        @Override
        public String id() {
            return "stormsprong";
        }

        @Override
        public String naamKey() {
            return "vadsrijk.kern.skill.stormsprong";
        }

        @Override
        public int slot() {
            return 1;
        }

        @Override
        public double energie() {
            return 25;
        }

        @Override
        public int cooldownTicks() {
            return 160;
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            Vec3 doel = c.doel() != null ? c.doel().position() : p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(8));
            Vec3 d = doel.subtract(p.position());
            double afstand = Math.min(14, d.multiply(1, 0, 1).length());
            Vec3 vlak = d.multiply(1, 0, 1).normalize();
            // a ballistic hop: horizontal speed so that it lands near the target in ~ 14 ticks
            double v = afstand / 14.0;
            p.setDeltaMovement(vlak.x * v * 1.25, 0.75 + Math.max(0, d.y) * 0.08, vlak.z * v * 1.25);
            p.hurtMarked = true;
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            SPRONG.put(p.getUUID(), p.level().getGameTime());
            ServerLevel sl = (ServerLevel) p.level();
            sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
            sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.7f, 1.4f);
            return true;
        }
    }

    static final class Knuffelschild implements Vaardigheid {
        @Override
        public String id() {
            return "knuffelschild";
        }

        @Override
        public String naamKey() {
            return "vadsrijk.kern.skill.knuffelschild";
        }

        @Override
        public int slot() {
            return 2;
        }

        @Override
        public double energie() {
            return 30;
        }

        @Override
        public int cooldownTicks() {
            return 300;
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            BLOK_TOT.put(p.getUUID(), sl.getGameTime() + 60);
            for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10), x -> x instanceof VadsrijkMob vm && !vm.geplofd())) {
                m.setTarget(p);
            }
            sl.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1.2f);
            sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
            return true;
        }
    }

    static final class VahoegePlof implements Vaardigheid {
        @Override
        public String id() {
            return "vahoege_plof";
        }

        @Override
        public String naamKey() {
            return "vadsrijk.kern.skill.vahoege_plof";
        }

        @Override
        public int slot() {
            return 3;
        }

        @Override
        public double energie() {
            return 0;
        }

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean gebruik(SkillContext c) {
            ServerPlayer p = c.speler();
            ServerLevel sl = (ServerLevel) p.level();
            Vec3 doel = c.doel() != null ? c.doel().position() : p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
            int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(doel.x), (int) Math.floor(doel.z));
            doel = new Vec3(doel.x, Math.min(doel.y + 1, gy), doel.z);
            Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
            if (d == null) {
                return false;
            }
            CompoundTag tag = new CompoundTag();
            CompoundTag blok = new CompoundTag();
            blok.putString("Name", "minecraft:pink_wool");
            tag.put("block_state", blok);
            tag.putInt("teleport_duration", 2);
            CompoundTag tr = new CompoundTag();
            tr.put("translation", floats(-2.5f, 0, -2.5f));
            tr.put("left_rotation", floats(0, 0, 0, 1));
            tr.put("scale", floats(5, 2.2f, 5));
            tr.put("right_rotation", floats(0, 0, 0, 1));
            tag.put("transformation", tr);
            Nbt.load(d, tag);
            d.snapTo(doel.x, doel.y + 16, doel.z, 0, 0);
            sl.addFreshEntity(d);
            KUSSENS.add(new Kussen(sl, d, doel, p.getUUID(), c.wapenSchade() * 3, new int[1]));
            sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.6f);
            return true;
        }
    }

    // ---- ticking: landings, shields, pillows ------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> it = SPRONG.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            long sinds = p.level().getGameTime() - e.getValue();
            if (sinds > 3 && p.onGround() || sinds > 60) {
                it.remove();
                landing(p);
            }
        }
        BLOK_TOT.entrySet().removeIf(e -> {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || p.level().getGameTime() >= e.getValue()) {
                return true;
            }
            if (p.level() instanceof ServerLevel sl && p.tickCount % 2 == 0) {
                double a = p.tickCount * 0.5;
                for (int i = 0; i < 3; i++) {
                    double b = a + i * Math.PI * 2 / 3;
                    sl.sendParticles(new DustParticleOptions(0xFF8AD8, 1.2f), p.getX() + Math.cos(b) * 0.9, p.getY() + 1, p.getZ() + Math.sin(b) * 0.9, 1, 0, 0.3, 0, 0);
                }
            }
            return false;
        });
        Iterator<Kussen> ki = KUSSENS.iterator();
        while (ki.hasNext()) {
            Kussen k = ki.next();
            int t = k.t()[0]++;
            if (k.d().isRemoved()) {
                ki.remove();
                continue;
            }
            if (t < 15) {
                double y = k.doel().y + 16 * (1 - (t + 1) / 15.0) * (1 - (t + 1) / 15.0);
                k.d().teleportTo(k.doel().x, y, k.doel().z);
            } else if (t == 15) {
                plof(k);
            } else if (t > 30) {
                k.d().discard();
                ki.remove();
            }
        }
    }

    private static void landing(ServerPlayer p) {
        ServerLevel sl = (ServerLevel) p.level();
        double totaal = 0;
        double basis = Spelers.wapenSchade(p) * 1.3;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(3.5, 2, 3.5), x -> x instanceof VadsrijkMob m && !m.geplofd())) {
            totaal += Schade.doe(new Schade.Klap(p, e, basis, Spelers.wapenElement(p), Schade.Soort.MELEE, true, "stormsprong")).schade();
            Vec3 weg = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(weg.x * 0.5, 0.45, weg.z * 0.5));
            e.hurtMarked = true;
        }
        Spelers.laadUlt(p, totaal);
        p.resetFallDistance();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            sl.sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 2.5, p.getY() + 0.2, p.getZ() + Math.sin(a) * 2.5, 1, 0, 0, 0, 0.02);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX() + Math.cos(a) * 3.3, p.getY() + 0.4, p.getZ() + Math.sin(a) * 3.3, 1, 0, 0.1, 0, 0);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1f, 1.0f);
    }

    private static void plof(Kussen k) {
        ServerLevel sl = k.level();
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(k.eigenaar());
        Vec3 c = k.doel();
        sl.playSound(null, net.minecraft.core.BlockPos.containing(c), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 2f, 0.5f);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 0.5, c.z, 120, 3, 0.6, 3, 0.1);
        sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, 40, 3, 0.3, 3, 0.05);
        sl.sendParticles(ParticleTypes.HEART, c.x, c.y + 1.5, c.z, 10, 3, 0.5, 3, 0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(net.minecraft.core.BlockPos.containing(c)).inflate(6, 3, 6),
                x -> x instanceof VadsrijkMob m && !m.geplofd())) {
            if (p != null) {
                Schade.doe(new Schade.Klap(p, e, k.schade(), Element.ROZE, Schade.Soort.MELEE, true, "vahoege_plof"));
            }
            if (!(e instanceof VadsrijkMob m) || !m.geplofd()) {
                Statussen.geef(e, Status.VERDOOFD, 60, 1, p);
            }
        }
        if (p != null) {
            for (ServerPlayer l : Partijen.ledenInBuurt(p, 64)) {
                if (l.position().distanceTo(c) <= 8 || l == p) {
                    Schade.heal(p, l, Spelers.maxVads(l) * 0.15);
                }
            }
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }
}
