package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;

/**
 * Guhwinter (npc slice): Ozovads, the Sint's horse. A vanilla white horse with a guh twist: a soft pink blanket (dyed leather
 * horse armour), his name in pink, a guh-ish "hinnikvads" on every click. Never ridden, tamed, fed, leashed or hurt (the
 * interact events stop all of that, see GuhwinterNpcFeature), he stands still at his spot (movement speed 0) unless a scene
 * walks him somewhere ({@link #loopNaar}).
 */
public final class Ozovads {
    /** The pink of his blanket. */
    public static final int ROZE = 0xFF9CCF;
    private static final double SNELHEID_LOPEN = 0.2;

    /** A scene's target per horse (only while walking). */
    private static final Map<UUID, Vec3> LOOPT = new ConcurrentHashMap<>();
    /** Held by a scene: the keeper leaves him where he is. */
    private static final Map<UUID, Boolean> VAST = new ConcurrentHashMap<>();

    private Ozovads() {
    }

    @Nullable
    static Mob maak(ServerLevel level, NpcDefs.Def d, GuhwinterNpcs.Plek p) {
        Horse h = EntityType.HORSE.create(level, EntitySpawnReason.EVENT);
        if (h == null) {
            return null;
        }
        h.setComponent(DataComponents.HORSE_VARIANT, Variant.WHITE);
        h.snapTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0);
        h.setYHeadRot(p.yaw());
        h.setYBodyRot(p.yaw());
        h.setInvulnerable(true);
        h.setPersistenceRequired();
        h.setCustomName(Component.translatable(d.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
        h.setCustomNameVisible(true);
        ItemStack deken = new ItemStack(Items.LEATHER_HORSE_ARMOR);
        deken.set(DataComponents.DYED_COLOR, new DyedItemColor(ROZE));
        h.setBodyArmorItem(deken);
        h.setDropChance(net.minecraft.world.entity.EquipmentSlot.BODY, 0f);
        stil(h, true);
        return h;
    }

    /** Stands still (speed 0) or may walk (a scene). */
    private static void stil(Mob h, boolean stil) {
        if (h.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(stil ? 0 : SNELHEID_LOPEN);
        }
    }

    /** The keeper, every second: done walking, then still again; strayed far (not held): back to his spot. */
    static void houdBij(Mob h, GuhwinterNpcs.Plek p) {
        Vec3 doel = LOOPT.get(h.getUUID());
        if (doel != null) {
            if (h.position().distanceToSqr(doel) < 2.0 * 2.0 || h.getNavigation().isDone()) {
                LOOPT.remove(h.getUUID());
                h.getNavigation().stop();
                stil(h, true);
            }
            return;
        }
        if (!VAST.getOrDefault(h.getUUID(), false) && h.distanceToSqr(p.pos()) > 6 * 6) {
            h.snapTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0);
            h.getNavigation().stop();
        }
    }

    static void loopNaar(Mob h, Vec3 doel, double snelheid) {
        VAST.put(h.getUUID(), true);
        LOOPT.put(h.getUUID(), doel);
        stil(h, false);
        h.getNavigation().moveTo(doel.x, doel.y, doel.z, snelheid);
    }

    static boolean loopt(Mob h) {
        return LOOPT.containsKey(h.getUUID());
    }

    static void vast(Mob h, boolean vast) {
        if (vast) {
            VAST.put(h.getUUID(), true);
        } else {
            VAST.remove(h.getUUID());
            LOOPT.remove(h.getUUID());
            stil(h, true);
        }
    }

    /** A happy whinny with a few hearts. */
    static void hinnik(Mob h) {
        if (h.level() instanceof ServerLevel sl) {
            sl.playSound(null, h.getX(), h.getY(), h.getZ(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.9f, 1.25f);
            sl.sendParticles(ParticleTypes.HEART, h.getX(), h.getY() + 1.9, h.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        }
    }
}
