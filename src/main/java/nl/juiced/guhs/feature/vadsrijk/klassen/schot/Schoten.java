package nl.juiced.guhs.feature.vadsrijk.klassen.schot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.storage.Nbt;

/**
 * Server-simulated projectiles of the Pluisjager and the Vadsmagier: arrows, bolts, pebbles, magic orbs and fireballs. Each
 * one is a position + velocity that moves every tick (real travel time), sweeps its path against blocks and Vadsrijk mobs
 * (and, for a heal arrow, other players), leaves a particle trail, and optionally carries a visible item (an ItemDisplay that
 * points along its flight and glides smoothly with {@code teleport_duration}). No entity class, no client code, nothing
 * saved: a projectile lives a few seconds at most and simply vanishes on a restart.
 */
public final class Schoten {
    /** What a projectile does when it touches a living target: return true to keep flying (pierce). */
    @FunctionalInterface
    public interface Raak {
        boolean raak(Schot s, LivingEntity doel);
    }

    /** At the end (a block, out of range, or the last pierce): {@code waar} is where it stopped. */
    @FunctionalInterface
    public interface Einde {
        void einde(Schot s, Vec3 waar, boolean blok);
    }

    /** Particles along the path (called every tick with the old and new position). */
    @FunctionalInterface
    public interface Spoor {
        void spoor(ServerLevel sl, Vec3 van, Vec3 naar, int leeftijd);
    }

    public static final class Schot {
        public final ServerLevel level;
        public final UUID eigenaar;
        public Vec3 pos, snelheid;
        public double zwaartekracht, straal = 0.3, sturen;
        public int leeftijd, maxLeeftijd = 40, doorboren;
        public boolean raaktSpelers;
        public final Set<UUID> geraakt = new HashSet<>();
        @Nullable
        public Display.ItemDisplay beeld;
        @Nullable
        public Raak opRaak;
        @Nullable
        public Einde opEinde;
        @Nullable
        public Spoor spoor;
        /** Free data for the skill (the number of the shot, a damage factor...). */
        public double waarde = 1;
        boolean weg;

        Schot(ServerLevel level, UUID eigenaar, Vec3 pos, Vec3 snelheid) {
            this.level = level;
            this.eigenaar = eigenaar;
            this.pos = pos;
            this.snelheid = snelheid;
        }

        @Nullable
        public ServerPlayer schutter() {
            return level.getServer().getPlayerList().getPlayer(eigenaar);
        }

        public void stop() {
            weg = true;
        }
    }

    private static final List<Schot> ALLE = new ArrayList<>();
    private static final List<Schot> NIEUW = new ArrayList<>();

    private Schoten() {
    }

    /** A new projectile from {@code van} with {@code snelheid} (blocks per tick). Configure the returned shot, it flies next tick. */
    public static Schot vuur(ServerPlayer p, Vec3 van, Vec3 snelheid) {
        Schot s = new Schot((ServerLevel) p.level(), p.getUUID(), van, snelheid);
        NIEUW.add(s);
        return s;
    }

    /** Gives the shot a visible item (e.g. "minecraft:arrow") that points along the flight. {@code schuin}: the item's own
     *  picture points diagonally up-right (arrows, swords); otherwise it is shown flat. */
    public static void metBeeld(Schot s, String item, float schaal, boolean schuin) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(s.level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        CompoundTag it = new CompoundTag();
        it.putString("id", item);
        it.putInt("count", 1);
        tag.put("item", it);
        tag.putString("item_display", "none");
        tag.putInt("teleport_duration", 1);
        tag.putFloat("view_range", 1.5f);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        // The item renderer turns the item 180 degrees around Y; the tip of a diagonal item picture is then at (-1, 1, 0).
        // Turn that tip onto local +Z (= the entity's yaw/pitch forward), then the entity's rotation aims it.
        Quaternionf q = schuin ? new Quaternionf().rotationTo(new Vector3f(-1, 1, 0).normalize(), new Vector3f(0, 0, 1)) : new Quaternionf();
        tr.put("left_rotation", floats(q.x, q.y, q.z, q.w));
        tr.put("scale", floats(schaal, schaal, schaal));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        Nbt.load(d, tag);
        richt(d, s.pos, s.snelheid);
        s.level.addFreshEntity(d);
        s.beeld = d;
    }

    private static void richt(Display d, Vec3 pos, Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-v.x, v.z)));
        float pitch = (float) (Math.toDegrees(-Math.atan2(v.y, h)));
        d.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
    }

    private static void beweeg(Display d, Vec3 pos, Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        d.setYRot((float) (Math.toDegrees(Math.atan2(-v.x, v.z))));
        d.setXRot((float) (Math.toDegrees(-Math.atan2(v.y, h))));
        d.teleportTo(pos.x, pos.y, pos.z);
    }

    public static void tick() {
        ALLE.addAll(NIEUW);
        NIEUW.clear();
        Iterator<Schot> it = ALLE.iterator();
        while (it.hasNext()) {
            Schot s = it.next();
            if (!s.weg) {
                stap(s);
            }
            if (s.weg) {
                if (s.beeld != null) {
                    s.beeld.discard();
                }
                it.remove();
            }
        }
    }

    public static void wisAlles() {
        for (Schot s : ALLE) {
            if (s.beeld != null) {
                s.beeld.discard();
            }
        }
        ALLE.clear();
        NIEUW.clear();
    }

    private static void stap(Schot s) {
        if (s.leeftijd++ >= s.maxLeeftijd) {
            einde(s, s.pos, false);
            return;
        }
        ServerPlayer schutter = s.schutter();
        if (s.sturen > 0) {
            LivingEntity doel = dichtsteVoor(s);
            if (doel != null) {
                Vec3 naar = doel.getBoundingBox().getCenter().subtract(s.pos).normalize();
                double v = s.snelheid.length();
                s.snelheid = s.snelheid.normalize().scale(1 - s.sturen).add(naar.scale(s.sturen)).normalize().scale(v);
            }
        }
        s.snelheid = s.snelheid.add(0, -s.zwaartekracht, 0);
        Vec3 van = s.pos, naar = s.pos.add(s.snelheid);
        BlockHitResult blok = s.level.clip(new ClipContext(van, naar, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 eind = blok.getType() == HitResult.Type.MISS ? naar : blok.getLocation();
        // everything living along van..eind, nearest first
        AABB veeg = new AABB(van, eind).inflate(s.straal + 1.0);
        List<LivingEntity> langs = new ArrayList<>();
        for (LivingEntity e : s.level.getEntitiesOfClass(LivingEntity.class, veeg, e -> geldig(s, e, schutter))) {
            if (e.getBoundingBox().inflate(s.straal).clip(van, eind).isPresent() || e.getBoundingBox().inflate(s.straal).contains(van)) {
                langs.add(e);
            }
        }
        langs.sort(Comparator.comparingDouble(e -> e.distanceToSqr(van)));
        for (LivingEntity e : langs) {
            s.geraakt.add(e.getUUID());
            boolean verder = s.opRaak == null || s.opRaak.raak(s, e);
            if (!verder || s.doorboren-- <= 0) {
                Vec3 waar = e.getBoundingBox().inflate(s.straal).clip(van, eind).orElse(e.getBoundingBox().getCenter());
                if (s.spoor != null) {
                    s.spoor.spoor(s.level, van, waar, s.leeftijd);
                }
                einde(s, waar, false);
                return;
            }
        }
        if (s.spoor != null) {
            s.spoor.spoor(s.level, van, eind, s.leeftijd);
        }
        s.pos = eind;
        if (blok.getType() != HitResult.Type.MISS) {
            einde(s, eind.subtract(s.snelheid.normalize().scale(0.1)), true);
            return;
        }
        if (s.beeld != null && !s.beeld.isRemoved()) {
            beweeg(s.beeld, s.pos, s.snelheid);
        }
    }

    private static boolean geldig(Schot s, LivingEntity e, @Nullable ServerPlayer schutter) {
        if (s.geraakt.contains(e.getUUID()) || !e.isAlive()) {
            return false;
        }
        if (e instanceof VadsrijkMob m) {
            return !m.geplofd();
        }
        return s.raaktSpelers && e instanceof ServerPlayer sp && sp != schutter && !sp.isSpectator();
    }

    @Nullable
    private static LivingEntity dichtsteVoor(Schot s) {
        Vec3 richting = s.snelheid.normalize();
        LivingEntity beste = null;
        double score = Double.MAX_VALUE;
        for (LivingEntity e : s.level.getEntitiesOfClass(LivingEntity.class, new AABB(s.pos, s.pos).inflate(10),
                e -> e instanceof VadsrijkMob m && !m.geplofd() && !s.geraakt.contains(e.getUUID()))) {
            Vec3 naar = e.getBoundingBox().getCenter().subtract(s.pos);
            double d = naar.length();
            if (d < 0.01 || naar.normalize().dot(richting) < 0.55) {
                continue;
            }
            if (d < score) {
                score = d;
                beste = e;
            }
        }
        return beste;
    }

    private static void einde(Schot s, Vec3 waar, boolean blok) {
        s.weg = true;
        if (s.opEinde != null) {
            s.opEinde.einde(s, waar, blok);
        }
    }

    public static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }
}
