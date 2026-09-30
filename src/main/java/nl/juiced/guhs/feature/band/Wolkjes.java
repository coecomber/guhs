package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * 3.0 (Guhverhalen): tamed guhs that died are "In de wolkjes... njeg". Nothing is lost: at the death of a band guh
 * ({@link BandEvents#onDeath}) its whole entity NBT goes into its {@link BandData.Rec} ({@code dood}, {@code doodDag},
 * {@code lichaam}), it leaves its huisje and the Guhkamer, "Waar is hij?" says {@link PlekSoort#IN_DE_WOLKJES}, its dagboekje
 * gets a wist-je-datje, the moment {@link Moment#DOOD} fires, and a glowing star "Herinnering aan &lt;naam&gt;"
 * ({@code nl.juiced.guhs.feature.hemel.Herinnering}) drops. The Knuffelhart (hemel) lists {@link #dood} and brings one back
 * with {@link #terug}: the very same guh (same UUID = band id) with its hearts, favourites, dagboek, clothes, variant, size,
 * personality and backpack.
 */
public final class Wolkjes {
    /** One tamed guh in the wolkjes, for the Knuffelhart's screen. */
    public record DodeGuh(UUID bandId, UUID eigenaar, String naam, String variant, int hartjes, BandNiveau niveau, CompoundTag looks, long doodDag) {
    }

    /** Told about every tamed guh that goes to the wolkjes (after the bookkeeping; eigenaar null when offline). */
    @FunctionalInterface
    public interface DoodLuisteraar {
        void dood(ServerLevel level, GuhEntity guh, @Nullable ServerPlayer eigenaar);
    }

    private static final List<DoodLuisteraar> LUISTERAARS = new CopyOnWriteArrayList<>();

    public static void opDood(DoodLuisteraar l) {
        LUISTERAARS.add(l);
    }

    /** The dead tamed guhs of this owner, newest first. */
    public static List<DodeGuh> dood(MinecraftServer s, UUID eigenaar) {
        List<DodeGuh> out = new ArrayList<>();
        List<BandData.Rec> recs = BandData.get(s).guhsVan(eigenaar);
        for (int i = recs.size() - 1; i >= 0; i--) {
            BandData.Rec r = recs.get(i);
            if (r.dood) {
                out.add(new DodeGuh(r.id, eigenaar, r.naam.isEmpty() ? "Guh" : r.naam, r.looks.getStringOr("Variant", "").isEmpty() ? "normal"
                        : r.looks.getStringOr("Variant", ""), r.hartjes, r.niveau(), r.looks.copy(), r.doodDag));
            }
        }
        out.sort(Comparator.comparingLong(DodeGuh::doodDag).reversed());
        return out;
    }

    public static boolean isDood(MinecraftServer s, UUID eigenaar, UUID bandId) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r != null && r.dood;
    }

    /**
     * Brings a dead guh back (the Knuffelhart): rebuilt from the snapshot taken when it died, same UUID, full health, not
     * sitting, at pos; its record is alive again, Plek WERELD, the moment {@link Moment#TERUG}, and the first time in its
     * dagboek "terug_uit_de_wolkjes". Null when it isn't dead, isn't this owner's, or its UUID is still in some level.
     */
    @Nullable
    public static GuhEntity terug(ServerLevel level, ServerPlayer eigenaar, UUID bandId, Vec3 pos) {
        MinecraftServer s = level.getServer();
        BandData data = BandData.get(s);
        BandData.Rec r = data.vind(eigenaar.getUUID(), bandId);
        if (r == null || !r.dood || !r.guh) {
            return null;
        }
        for (ServerLevel l : s.getAllLevels()) {
            if (l.getEntity(bandId) != null) {
                return null;
            }
        }
        GuhEntity guh = maak(level, r, bandId, eigenaar.getUUID(), pos);
        if (guh == null || !level.addFreshEntity(guh)) {
            return null;
        }
        r.dood = false;
        r.doodDag = -1;
        r.lichaam = new CompoundTag();
        data.setDirty();
        Band.bijwerken(guh);
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        Band.moment(guh, eigenaar, Moment.TERUG, "");
        Dagboek.eersteKeer(guh, eigenaar, "terug_uit_de_wolkjes");
        level.broadcastEntityEvent(guh, (byte) 7);   // hearts
        return guh;
    }

    /** The guh from its snapshot (or, for an old record without one, a new guh with its looks and the same UUID). */
    @Nullable
    private static GuhEntity maak(ServerLevel level, BandData.Rec r, UUID bandId, UUID eigenaar, Vec3 pos) {
        CompoundTag tag = r.lichaam.copy();
        if (!tag.isEmpty()) {
            tag.putString("id", nl.juiced.guhs.Guhs.id("guh").toString());
            tag.putFloat("Health", GuhEntity.TAMED_HEALTH);
            tag.putShort("DeathTime", (short) 0);
            tag.putShort("HurtTime", (short) 0);
            tag.putShort("Fire", (short) -20);
            tag.putFloat("FallDistance", 0f);
            tag.putBoolean("Sitting", false);
            tag.remove("Passengers");
            tag.remove("ActiveEffects");
            tag.remove("active_effects");
            ListTag motion = new ListTag();
            motion.add(DoubleTag.valueOf(0));
            motion.add(DoubleTag.valueOf(0));
            motion.add(DoubleTag.valueOf(0));
            tag.put("Motion", motion);
            tag.store("UUID", UUIDUtil.CODEC, bandId);
            Entity e = EntityType.loadEntityRecursive(tag, level, ent -> {
                ent.moveTo(pos.x, pos.y, pos.z, ent.getYRot(), 0f);
                return ent;
            });
            if (e instanceof GuhEntity guh) {
                klaarzetten(guh);
                return guh;
            }
            return null;
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.setUUID(bandId);
        guh.setVariant(GuhVariant.byId(r.looks.getStringOr("Variant", "")));
        if (r.looks.contains("Scale")) {
            guh.setGuhScale(r.looks.getFloatOr("Scale", 0.0F));
        }
        guh.setOwnerUUID(eigenaar);
        guh.setTame(true, true);
        if (!r.naam.isEmpty() && !r.naam.equals("Guh")) {
            guh.setCustomName(Component.literal(r.naam));
        }
        guh.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
        klaarzetten(guh);
        return guh;
    }

    private static void klaarzetten(GuhEntity guh) {
        guh.setHealth(guh.getMaxHealth());
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        guh.setPersistenceRequired();
        guh.getPersistentData().remove(Huisjes.THUIS);
        guh.getPersistentData().remove(Huisjes.DIM);
        guh.getPersistentData().remove(Huisjes.BINNEN);
        guh.getPersistentData().remove(nl.juiced.guhs.feature.guhkamer.Guhkamer.GAST);
        guh.setInvisible(false);
        guh.noPhysics = false;
        guh.setNoGravity(false);
    }

    /** (BandEvents.onDeath) a band guh dies: snapshot, out of its huisje and the Guhkamer, the star, the dagboek. */
    static void naarDeWolkjes(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer s = level.getServer();
        UUID eigenaar = Band.eigenaar(guh), id = Band.id(guh);
        ServerPlayer online = s.getPlayerList().getPlayer(eigenaar);
        BandData.Rec r = Band.bijwerken(guh);
        if (r == null) {
            return;
        }
        // out of its huisje and the Guhkamer first, so the snapshot is of a guh that lives in the world
        if (Huisjes.isBewoner(guh)) {
            Huisjes.trekUit(guh);
        }
        if (nl.juiced.guhs.feature.guhkamer.Guhkamer.isGast(guh)) {
            var kamers = nl.juiced.guhs.feature.guhkamer.GuhkamerData.get(s);
            var kamer = kamers.vanGast(id);
            if (kamer != null) {
                kamer.gasten.remove(id);
                kamers.setDirty();
            }
            nl.juiced.guhs.feature.guhkamer.Guhkamer.markeer(guh, false);
        }
        CompoundTag lichaam = guh.saveWithoutId(new CompoundTag());
        lichaam.putString("id", nl.juiced.guhs.Guhs.id("guh").toString());
        r.lichaam = lichaam;
        r.dood = true;
        r.doodDag = Band.dag(s);
        BandData.get(s).setDirty();
        GuhVolger.zet(eigenaar, id, new Plek(PlekSoort.IN_DE_WOLKJES, level.dimension(), guh.blockPosition(), "", level.getGameTime()));
        Dagboek.wistJeDat(guh, "gui.guhs.wolkjes.wist", r.naam.isEmpty() ? "Guh" : r.naam);
        Band.moment(guh, online, Moment.DOOD, "");
        ItemStack ster = nl.juiced.guhs.feature.hemel.Herinnering.maak(guh);
        if (!ster.isEmpty()) {
            guh.spawnAtLocation(ster, 0.5f);
        }
        if (online != null) {
            online.sendSystemMessage(Component.translatable("gui.guhs.wolkjes.dood", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        for (DoodLuisteraar l : LUISTERAARS) {
            try {
                l.dood(level, guh, online);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Wolkjes listener failed", e);
            }
        }
    }

    private Wolkjes() {
    }
}
