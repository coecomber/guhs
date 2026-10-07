package nl.juiced.guhs.feature.guhpixel;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * A guh as data: taken out of the world ({@link #bewaar}) and put back as the very same guh ({@link #laatVrij}; same UUID,
 * so hearts, diary and friendships stay attached). Used by the Reisbureau ({@link PlekSoort#OP_VAKANTIE}) and the
 * Guhkantoor ({@link PlekSoort#OP_KANTOOR}).
 * <p>
 * Rules for both users: keep the returned tag in your OWN SavedData keyed by the owner (never only in a block entity);
 * delete your record only AFTER {@link #laatVrij} returned the entity; a stored guh is never also an item or an entity.
 * The tag: {@code Id}, {@code Eigenaar}, {@code Naam}, {@code Looks}, {@code Soort}, {@code Data} (the whole entity).
 */
public final class GuhOpslag {
    /**
     * Stores this guh and removes it from the world: out of its huisje, off whatever it rides, standing; its name and
     * looks are snapshotted for screens; "Waar is mijn guh?" then says {@code soort} with {@code detail}.
     */
    public static CompoundTag bewaar(GuhEntity guh, PlekSoort soort, Component detail) {
        if (Huisjes.isBewoner(guh)) {
            Huisjes.trekUit(guh);
        }
        guh.stopRiding();
        guh.ejectPassengers();
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        GuhKiezer.los(guh);
        CompoundTag opslag = new CompoundTag();
        UUID id = guh.getUUID();
        UUID eigenaar = guh.getOwnerUUID();
        opslag.store("Id", UUIDUtil.CODEC, id);
        if (eigenaar != null) {
            opslag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        Tekst.put(opslag, "Naam", guh.getName().copy());
        opslag.put("Looks", Band.looks(guh));
        opslag.putString("Soort", soort.id());
        CompoundTag data = new CompoundTag();
        Nbt.saveWithoutId(guh, data);
        data.putString("id", EntityType.getKey(guh.getType()).toString());
        data.putBoolean("Sitting", false);
        opslag.put("Data", data);
        ServerLevel level = (ServerLevel) guh.level();
        guh.discard();
        if (eigenaar != null) {
            GuhVolger.zet(eigenaar, id, new Plek(soort, level.dimension(), guh.blockPosition(), detail, level.getGameTime()));
        }
        return opslag;
    }

    /** The band id (= entity UUID) of a stored guh. */
    @Nullable
    public static UUID id(CompoundTag opslag) {
        return opslag.read("Id", UUIDUtil.CODEC).orElse(null);
    }

    @Nullable
    public static UUID eigenaar(CompoundTag opslag) {
        return opslag.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
    }

    public static Component naam(CompoundTag opslag) {
        return Tekst.get(opslag, "Naam");
    }

    /**
     * Puts the stored guh back in the world. Null (and nothing is lost: keep your record) when a guh with that UUID is
     * loaded already or the entity could not be made.
     */
    @Nullable
    public static Entity laatVrij(ServerLevel level, CompoundTag opslag, Vec3 pos, float yaw) {
        UUID id = id(opslag);
        CompoundTag data = opslag.getCompoundOrEmpty("Data");
        if (id == null || data.isEmpty() || Band.zoekGeladen(level.getServer(), id) != null) {
            return null;
        }
        CompoundTag t = data.copy();
        // (it comes back free: no huisje home or asleep-inside state, no claim, standing, with gravity)
        CompoundTag nf = t.getCompoundOrEmpty("NeoForgeData");
        boolean binnen = nf.getBooleanOr(Huisjes.BINNEN, false);
        nf.remove(Huisjes.THUIS);
        nf.remove(Huisjes.DIM);
        nf.remove(Huisjes.BINNEN);
        nf.remove(GuhKiezer.BEZET);
        t.put("NeoForgeData", nf);
        if (binnen) {
            t.putBoolean("NoGravity", false);
            t.putInt("KnusVlaggen", t.getIntOr("KnusVlaggen", 0) & ~BandVlaggen.HUISJE_BINNEN);
        }
        t.remove("Passengers");
        Entity e = PickedUpGuhItem.release(level, t, pos.x, pos.y, pos.z, yaw);
        if (e != null) {
            e.setDeltaMovement(Vec3.ZERO);
            e.fallDistance = 0;
            GuhVolger.zet(e, PlekSoort.WERELD, "");
        }
        return e;
    }

    private GuhOpslag() {
    }
}
