package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice geest_modellen. The cosmetic libelkonijnguh pet, the reward of the lantern
 * questline (DESIGN_EINDE2 5 "Verdwaalde lantaarns"): ONE per player, never lost. Its state lives in the player's data
 * ({@code GuhQuests.saved}: {@value #HEEFT}, the entity id {@value #ID}, and {@value #BLIJF} when the owner told it to stay
 * somewhere): every {@link #CONTROLE} ticks a player who has one gets it back at their side when it is not in their world
 * within {@link #BIJ} blocks (another dimension, a far chunk, a server restart): an old copy is taken away, so there is always
 * exactly one. The owner's sneak-click tells it to stay where it is (or to come along again); a plain click is a happy loop.
 * It never fights, never blocks, never picks anything up.
 */
public final class Libelkonijnguh {
    public static final String HEEFT = "guhs_einde2_geestmodellen_libel";
    public static final String ID = "guhs_einde2_geestmodellen_libel_id";
    public static final String BLIJF = "guhs_einde2_geestmodellen_libel_blijf";
    public static final int CONTROLE = 40;
    public static final double BIJ = 48.0;

    private Libelkonijnguh() {
    }

    public static boolean heeft(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(HEEFT, false);
    }

    /** Gives p their pet, or calls it back to their side (also when it was told to stay); null: it could not be made. */
    @Nullable
    public static Entity geef(ServerPlayer p) {
        CompoundTag data = GuhQuests.saved(p);
        data.putBoolean(HEEFT, true);
        data.remove(BLIJF);
        LibelkonijnguhEntity oud = vind(p);
        if (oud != null && oud.level() == p.level()) {
            Vec3 bij = naast(p);
            oud.snapTo(bij.x, bij.y, bij.z, p.getYRot(), 0f);
            return oud;
        }
        if (oud != null) {
            oud.discard();
        }
        return maak(p);
    }

    /** The pet of p wherever it is loaded (null: none loaded). */
    @Nullable
    public static LibelkonijnguhEntity vind(ServerPlayer p) {
        String id = GuhQuests.saved(p).getStringOr(ID, "");
        if (id.isEmpty()) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            if (level.getEntity(uuid) instanceof LibelkonijnguhEntity pet && pet.isAlive()) {
                return pet;
            }
        }
        return null;
    }

    /** Is this entity p's pet, the one and only? */
    public static boolean isVan(ServerPlayer p, Entity e) {
        return heeft(p) && e.getUUID().toString().equals(GuhQuests.saved(p).getStringOr(ID, ""));
    }

    /** Sneak-click of the owner: stay here / come along again. Returns: it stays now. */
    public static boolean wisselBlijf(ServerPlayer p) {
        CompoundTag data = GuhQuests.saved(p);
        boolean blijf = !data.getBooleanOr(BLIJF, false);
        if (blijf) {
            data.putBoolean(BLIJF, true);
        } else {
            data.remove(BLIJF);
        }
        return blijf;
    }

    public static boolean blijft(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(BLIJF, false);
    }

    /** Dev and tests: the pet is gone and forgotten. */
    public static void wis(ServerPlayer p) {
        LibelkonijnguhEntity pet = vind(p);
        if (pet != null) {
            pet.discard();
        }
        CompoundTag data = GuhQuests.saved(p);
        data.remove(HEEFT);
        data.remove(ID);
        data.remove(BLIJF);
    }

    /** "Never lost": a player who has one and did not tell it to stay gets it at their side when it is not near. */
    public static void controleer(ServerPlayer p) {
        if (!heeft(p) || blijft(p) || p.isSpectator() || !p.isAlive()) {
            return;
        }
        LibelkonijnguhEntity pet = vind(p);
        if (pet != null && pet.level() == p.level() && pet.distanceTo(p) <= BIJ) {
            return;
        }
        if (pet != null) {
            pet.discard();
        }
        maak(p);
    }

    static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % CONTROLE == 7) {
            controleer(p);
        }
    }

    private static Vec3 naast(ServerPlayer p) {
        return p.getEyePosition().add(p.getLookAngle().yRot(1.2f).multiply(1.4, 0, 1.4)).add(0, -0.2, 0);
    }

    @Nullable
    private static LibelkonijnguhEntity maak(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        LibelkonijnguhEntity pet = E2GeestWezensFeature.LIBELKONIJNGUH.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (pet == null) {
            return null;
        }
        Vec3 bij = naast(p);
        pet.snapTo(bij.x, bij.y, bij.z, p.getYRot(), 0f);
        pet.zetEigenaar(p.getUUID());
        level.addFreshEntity(pet);
        level.sendParticles(ParticleTypes.END_ROD, bij.x, bij.y, bij.z, 6, 0.2, 0.2, 0.2, 0.02);
        GuhQuests.saved(p).putString(ID, pet.getUUID().toString());
        return pet;
    }
}
