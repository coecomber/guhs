package nl.juiced.guhs.feature.guhpixel;

import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.guhkamer.Guhkamer;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.taal.Tekst;

/**
 * Picking one of your own guhs (shared by the Reisbureau, Guhkantoor, Guhkade, Guhbioscoop, Guh-parkour and Guhmon).
 * <ul>
 *   <li>{@link #lijst}: every tamed guh of the player from BandData (no entity is loaded for it), for a picker screen
 *   (client.GuhKiezerLijst): per guh {@code Id}, {@code Naam}, {@code Looks}, {@code Hartjes}, {@code PlekSoort};</li>
 *   <li>{@link #zoek}: the loaded entity of a picked guh, when it is really the player's own guh and near;</li>
 *   <li>{@link #bezet}: why a guh cannot be taken now; {@link #claim} / {@link #los}: one px feature at a time uses a guh.</li>
 * </ul>
 */
public final class GuhKiezer {
    /** Persistent data of a guh: the namespace of the px feature that uses it now. */
    public static final String BEZET = "guhs_px_bezet";

    /** The player's living tamed guhs (not maatjes), each as a tag for a picker screen. */
    public static ListTag lijst(ServerPlayer p, Predicate<BandData.Rec> filter) {
        ListTag out = new ListTag();
        for (BandData.Rec r : BandData.get(p.level().getServer()).guhsVan(p.getUUID())) {
            if (!r.guh || r.dood || !filter.test(r)) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, r.id);
            Tekst.put(t, "Naam", r.weergave());
            t.put("Looks", r.looks.copy());
            t.putInt("Hartjes", r.hartjes);
            t.putString("PlekSoort", r.plek.soort().id());
            out.add(t);
        }
        return out;
    }

    /** The loaded guh with this band id: the player's own, a real guh (exact type guhs:guh), alive and within straal. */
    @Nullable
    public static GuhEntity zoek(ServerPlayer p, UUID bandId, double straal) {
        Entity e = p.level().getEntity(bandId);
        if (e instanceof GuhEntity guh && Band.isBandGuh(guh) && guh.isAlive() && p.getUUID().equals(guh.getOwnerUUID())
                && guh.distanceToSqr(p) <= straal * straal) {
            return guh;
        }
        return null;
    }

    /** Null = free; else why this guh cannot be taken now. */
    @Nullable
    public static Component bezet(GuhEntity guh) {
        if (guh.isPassenger() || guh.isVehicle()) {
            return Component.translatable("gui.guhs.guhpixel.kiezer.bezet.rijdt");
        }
        if (Huisjes.isBinnen(guh)) {
            return Component.translatable("gui.guhs.guhpixel.kiezer.bezet.huisje");
        }
        if (Guhkamer.isGast(guh)) {
            return Component.translatable("gui.guhs.guhpixel.kiezer.bezet.guhkamer");
        }
        if (guh.isBaby()) {
            return Component.translatable("gui.guhs.guhpixel.kiezer.bezet.baby");
        }
        if (!geclaimd(guh).isEmpty()) {
            return Component.translatable("gui.guhs.guhpixel.kiezer.bezet.bezig");
        }
        return null;
    }

    /** The namespace that uses this guh now ("" = nobody). */
    public static String geclaimd(GuhEntity guh) {
        return guh.getPersistentData().getStringOr(BEZET, "");
    }

    /** A px feature starts using this guh (its namespace: "guhkade", "guhparkour"...). */
    public static void claim(GuhEntity guh, String ns) {
        guh.getPersistentData().putString(BEZET, ns);
    }

    public static void los(GuhEntity guh) {
        guh.getPersistentData().remove(BEZET);
    }

    private GuhKiezer() {
    }
}
