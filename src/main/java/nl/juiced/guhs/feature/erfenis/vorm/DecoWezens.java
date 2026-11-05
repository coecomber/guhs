package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.landdiertjes.GuhKonijntjeEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The creatures of a shape (the animals in their meadow, the NPC at the counter, the guhs on the terrace): decoration only.
 * Tagged {@value #TAG}, invulnerable, never despawn, silent about drops, no interaction (no eggs, wool or milk for free, no
 * quests), and kept inside their own footprint: animals wander inside it (home = its middle), anything that ends up
 * outside is put back on its own spot. They are saved with the chunk like any mob, and every stamp of a footprint first
 * takes away the old ones there, so they never pile up; plain guhs and NPCs stand still on their spot (no AI for guhs).
 */
public final class DecoWezens {
    public static final String TAG = "guhs_erfenis_deco";
    private static final String DATA = "guhs_erfdeco";
    private static final Set<Mob> LEVEND = Collections.newSetFromMap(new WeakHashMap<>());
    private static long laatsteStempel;

    private DecoWezens() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(DecoWezens::opJoin);
        NeoForge.EVENT_BUS.addListener(DecoWezens::opLeave);
        NeoForge.EVENT_BUS.addListener(DecoWezens::opInteract);
        NeoForge.EVENT_BUS.addListener(DecoWezens::opInteractAt);
        NeoForge.EVENT_BUS.addListener(DecoWezens::opDrops);
        NeoForge.EVENT_BUS.addListener(DecoWezens::opTick);
    }

    public static boolean isDeco(Entity e) {
        return e != null && e.entityTags().contains(TAG);
    }

    /** Takes away the decorative creatures standing in a footprint. */
    static void weg(ServerLevel lvl, BlockPos o, int bx, int bz, int h) {
        AABB box = new AABB(o.getX(), o.getY() - 2, o.getZ(), o.getX() + bx, o.getY() + h, o.getZ() + bz);
        for (Entity e : new ArrayList<>(lvl.getEntities((Entity) null, box, DecoWezens::isDeco))) {
            e.discard();
        }
    }

    /** Spawns the creatures of a shape stamped at o (turned rot). */
    static void zet(ServerLevel lvl, VormBestand v, BlockPos o, int bx, int bz, int rot) {
        long stempel = Math.max(System.currentTimeMillis(), ++laatsteStempel);   // (1.9.6: which stamp made it, see ontdubbel)
        laatsteStempel = stempel;
        for (VormBestand.Wezen w : v.wezens) {
            Entity e = maak(lvl, w);
            if (e == null) {
                continue;
            }
            double[] p = Draai.punt(w.x(), w.z(), v.w, v.d, rot);
            double x = o.getX() + clamp(p[0], 0.3, bx - 0.3), z = o.getZ() + clamp(p[1], 0.3, bz - 0.3);
            double y = o.getY() + 1 + w.y();
            float yaw = (w.yaw() + 180f + 90f * rot) % 360f;   // render yaw: 0 = north, clockwise; Minecraft: 0 = south
            e.snapTo(x, y, z, yaw, 0f);
            e.setYHeadRot(yaw);
            e.setYBodyRot(yaw);
            e.addTag(TAG);
            e.setInvulnerable(true);
            CompoundTag t = new CompoundTag();
            t.putInt("x1", o.getX());
            t.putInt("z1", o.getZ());
            t.putInt("x2", o.getX() + bx);
            t.putInt("z2", o.getZ() + bz);
            t.putInt("y", o.getY());
            t.putDouble("hx", x);
            t.putDouble("hy", y);
            t.putDouble("hz", z);
            t.putLong("s", stempel);
            e.getPersistentData().put(DATA, t);
            if (e instanceof Mob m) {
                m.setPersistenceRequired();
                m.setHomeTo(BlockPos.containing(o.getX() + bx / 2.0, o.getY() + 1, o.getZ() + bz / 2.0), Math.max(2, Math.min(bx, bz) / 2));
                if (e instanceof GuhEntity && !"guh_mammoetguh".equals(w.skin())) {
                    m.setNoAi(true);   // plain guhs stand on their spot (a wild guh would run off or flee)
                }
            }
            lvl.addFreshEntity(e);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** The entity for a marker (null when the game doesn't know it). */
    static Entity maak(ServerLevel lvl, VormBestand.Wezen w) {
        String t = w.t();
        if (t.equals("npc")) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(lvl, EntitySpawnReason.COMMAND);
            if (npc == null) {
                return null;
            }
            try {
                npc.setKind(GuhNpcEntity.Kind.valueOf(w.kind().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException ex) {
                VormBestand.ONBEKEND.putIfAbsent("npc:" + w.kind(), "NPC bestaat niet");
                return null;
            }
            schaal(npc, w.schaal() * npc.getKind().scale);
            return npc;
        }
        if (t.equals("guh")) {
            GuhEntity g = ModEntities.GUH.get().create(lvl, EntitySpawnReason.COMMAND);
            if (g == null) {
                return null;
            }
            g.setVariant(variant(w.skin()));
            g.setGuhScale(Math.min(GuhEntity.MAX_SCALE, w.schaal()));
            if (w.zit()) {
                g.setInSittingPose(true);
            }
            return g;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id(t)).orElse(null);
        if (type == null) {
            VormBestand.ONBEKEND.putIfAbsent("wezen:" + t, "wezen bestaat niet");
            return null;
        }
        Entity e = type.create(lvl, EntitySpawnReason.COMMAND);
        if (e == null) {
            return null;
        }
        String skin = w.skin();
        if (e instanceof GuhKonijntjeEntity k) {
            GuhKonijntjeEntity.Kleur kl = GuhKonijntjeEntity.Kleur.ROZE;
            for (GuhKonijntjeEntity.Kleur c : GuhKonijntjeEntity.Kleur.values()) {
                if (skin != null && skin.endsWith("_" + c.id())) {
                    kl = c;
                }
            }
            k.setKleur(kl);
        }
        if (e instanceof AgeableMob a && skin != null && skin.endsWith("_kuiken")) {
            a.setAge(-1_000_000_000);   // stays a kuikentje (for ~1.5 years of play)
        }
        if (w.schaal() != 1f && e instanceof Mob m) {
            schaal(m, w.schaal());
        }
        return e;
    }

    private static void schaal(Mob m, float s) {
        var a = m.getAttribute(Attributes.SCALE);
        if (a != null) {
            a.setBaseValue(s);
        }
    }

    static GuhVariant variant(String skin) {
        if (skin == null || skin.equals("guh")) {
            return GuhVariant.NORMAL;
        }
        String n = skin.startsWith("guh_") ? skin.substring(4) : skin;
        if (n.equals("mammoetguh")) {
            return GuhVariant.MAMMOETGUH;
        }
        try {
            return GuhVariant.valueOf(n.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GuhVariant.NORMAL;
        }
    }

    // ================================================================================================= events

    private static void opJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) {
            return;
        }
        Entity ent = e.getEntity();
        if (ent instanceof Mob m && isDeco(m)) {
            LEVEND.add(m);
        } else if (ent instanceof ItemEntity item && !e.loadedFromDisk()) {
            // no eggs, wool or milk out of the decoration (a content kippetje lays next to itself)
            if (!e.getLevel().getEntities((Entity) null, item.getBoundingBox().inflate(1.5), DecoWezens::isDeco).isEmpty()) {
                e.setCanceled(true);
            }
        }
    }

    private static void opLeave(EntityLeaveLevelEvent e) {
        if (e.getEntity() instanceof Mob m) {
            LEVEND.remove(m);
        }
    }

    private static void opInteract(PlayerInteractEvent.EntityInteract e) {
        if (isDeco(e.getTarget())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void opInteractAt(PlayerInteractEvent.EntityInteractSpecific e) {
        if (isDeco(e.getTarget())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void opDrops(LivingDropsEvent e) {
        if (isDeco(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    /**
     * 1.9.6: a footprint is stamped again while its old creatures were not loaded yet (their chunk still loading): "weg"
     * didn't see them, so when they load they stand next to the new ones (two marktmeester-Mikas behind one counter). Per
     * footprint only the newest stamp stays, and of one stamp only one creature per spot.
     */
    static void ontdubbel() {
        java.util.Map<String, java.util.List<Mob>> perPlek = new java.util.HashMap<>();
        for (Mob m : new ArrayList<>(LEVEND)) {
            CompoundTag t = m.getPersistentData().getCompoundOrEmpty(DATA);
            if (m.isRemoved() || t.isEmpty()) {
                continue;
            }
            String k = m.level().dimension().identifier() + "|" + t.getIntOr("x1", 0) + "," + t.getIntOr("z1", 0) + ","
                    + t.getIntOr("x2", 0) + "," + t.getIntOr("z2", 0) + "," + t.getIntOr("y", 0);
            perPlek.computeIfAbsent(k, x -> new ArrayList<>()).add(m);
        }
        for (java.util.List<Mob> groep : perPlek.values()) {
            if (groep.size() < 2) {
                continue;
            }
            long nieuwste = 0;
            for (Mob m : groep) {
                nieuwste = Math.max(nieuwste, m.getPersistentData().getCompoundOrEmpty(DATA).getLongOr("s", 0L));
            }
            Set<String> plekken = new java.util.HashSet<>();
            for (Mob m : groep) {
                CompoundTag t = m.getPersistentData().getCompoundOrEmpty(DATA);
                String wie = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()) + "|" + (m instanceof GuhNpcEntity n ? n.getKind() : "")
                        + "|" + Math.round(t.getDoubleOr("hx", 0) * 4) + "," + Math.round(t.getDoubleOr("hy", 0) * 4) + "," + Math.round(t.getDoubleOr("hz", 0) * 4);
                if (t.getLongOr("s", 0L) < nieuwste || !plekken.add(wie)) {
                    LEVEND.remove(m);
                    m.discard();
                }
            }
        }
    }

    private static void opTick(ServerTickEvent.Post e) {
        GebouwVormen.tick();
        if (e.getServer().getTickCount() % 40 != 0 || LEVEND.isEmpty()) {
            return;
        }
        ontdubbel();
        for (Mob m : new ArrayList<>(LEVEND)) {
            if (m.isRemoved()) {
                LEVEND.remove(m);
                continue;
            }
            if (m.entityTags().contains("guhs_bertha") && m.level() instanceof ServerLevel sl && m.getRandom().nextInt(3) == 0) {
                // (1.9 afwerking, Sherlock zaak 6) Bertha de koe sings on the Muziektent: little notes, now and then a proud boe
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, m.getX(), m.getY() + 1.9, m.getZ(), 1, 0.3, 0.1, 0.3, m.getRandom().nextDouble());
                if (m.getRandom().nextInt(6) == 0) {
                    m.playAmbientSound();
                }
            }
            CompoundTag t = m.getPersistentData().getCompoundOrEmpty(DATA);
            if (t.isEmpty()) {
                continue;
            }
            int x1 = t.getIntOr("x1", 0), x2 = t.getIntOr("x2", 0), z1 = t.getIntOr("z1", 0), z2 = t.getIntOr("z2", 0), y = t.getIntOr("y", 0);
            double x = m.getX(), z = m.getZ();
            if (x < x1 || x > x2 || z < z1 || z > z2 || m.getY() < y - 2 || m.getY() > y + GebouwVormen.WIS_HOOGTE) {
                m.getNavigation().stop();
                m.snapTo(t.getDoubleOr("hx", x), t.getDoubleOr("hy", m.getY()), t.getDoubleOr("hz", z), m.getYRot(), 0f);
                m.setDeltaMovement(0, 0, 0);
            }
        }
    }
}
