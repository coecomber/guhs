package nl.juiced.guhs.entity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

/**
 * 26.1 port: {@code TamableAnimal#getOwnerUUID()} / {@code setOwnerUUID(UUID)} are gone (owners are an
 * {@link EntityReference} now). {@link GuhEntity} keeps both methods; for any other tamable / ownable entity use these.
 */
public final class Owners {
    private Owners() {
    }

    /** The owner's UUID, or null (1.21.1 {@code getOwnerUUID()}). */
    @Nullable
    public static UUID uuid(OwnableEntity entity) {
        EntityReference<LivingEntity> ref = entity.getOwnerReference();
        return ref == null ? null : ref.getUUID();
    }

    /** Is this UUID the owner? (null-safe). */
    public static boolean isOwner(OwnableEntity entity, @Nullable UUID player) {
        UUID owner = uuid(entity);
        return owner != null && owner.equals(player);
    }

    /** A reference for {@code setOwnerReference} (1.21.1 {@code setOwnerUUID(uuid)}); null clears the owner. */
    @Nullable
    public static EntityReference<LivingEntity> ref(@Nullable UUID uuid) {
        return uuid == null ? null : EntityReference.of(uuid);
    }
}
