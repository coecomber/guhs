package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knabbelvelden.KnabbelveldenData;
import nl.juiced.guhs.feature.knabbelvelden.Pensioen;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.quest.GuhNummers;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.taal.Tekst;

/**
 * De Erfenis: the Guhpension, where a few of your own tamed guhs come to stay (round 2 / 7 of the design). The screen is
 * like the Eeuwige Knabbelvelden: left all your guhs and where they are, right the guhs staying in your village and the
 * limit (by the Guhpension's level).
 * <ul>
 *   <li>Sending: the guh is stored as data like a Reisbureau trip ({@link GuhOpslag#bewaar}, "Waar is mijn guh?" says
 *   {@link PlekSoort#OP_VAKANTIE} "logeert in het Guhpension"). It must be loaded (near you, or called with "Roep naar mij"
 *   first: we try that ourselves).</li>
 *   <li>Each logé helps one building: +5% (max 2 per building), shown in the overview; you pick which.</li>
 *   <li>Bringing home: it comes back as a picked-up guh in your pockets (everything kept), like the Knabbelvelden.</li>
 *   <li>While somebody is on the plot, a stand-in with its looks and name walks around on it (never saved, invulnerable,
 *   wild: the real guh stays safely stored).</li>
 * </ul>
 */
public final class Guhpension {
    /** Persistent data key of a stand-in (its base id): a stand-in that isn't ours (loaded after a crash) never comes in. */
    static final String STANDIN = "guhs_erfenis_loge";
    /** Stand-ins alive now: entity uuid -> (base id). */
    private static final Map<UUID, UUID> LEVEND = new HashMap<>();
    /** Per base: logé id -> its stand-in's entity uuid. */
    private static final Map<UUID, Map<UUID, UUID>> PER_BASE = new HashMap<>();

    private Guhpension() {
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    static void open(ServerPlayer p, Base b) {
        PacketDistributor.sendToPlayer(p, new ErfenisPayloads.PensionData(data(p, b)));
    }

    /** What the screen shows: your guhs (where they are, why one can't come) and the logé guhs with their building. */
    static CompoundTag data(ServerPlayer p, Base b) {
        MinecraftServer s = p.level().getServer();
        CompoundTag root = new CompoundTag();
        root.putInt("Plekken", b.pensionPlekken());
        ListTag thuis = new ListTag();
        KnabbelveldenData velden = KnabbelveldenData.get(s);
        for (BandData.Rec r : BandData.get(s).guhsVan(p.getUUID())) {
            if (r.dood || velden.vind(p.getUUID(), r.id) != null || isLoge(b, r.id)) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, r.id);
            t.putInt("Nr", GuhNummers.nummer(s, p.getUUID(), r.id));
            Tekst.put(t, "Naam", r.weergave());
            if (r.plek != null) {
                Tekst.put(t, "Plek", GuhVolger.tekst(r.plek));
            }
            t.put("Looks", r.looks.copy());
            String niet = Pensioen.nietNu(s, p.getUUID(), r.id);
            if (niet == null && Pensioen.bezig(s, p.getUUID(), r.id) != null) {
                niet = "bezig";
            }
            t.putString("Niet", niet == null ? "" : niet);
            thuis.add(t);
        }
        root.put("Thuis", thuis);
        ListTag hier = new ListTag();
        for (Base.Loge l : b.loges) {
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, l.id);
            Tekst.put(t, "Naam", GuhOpslag.naam(l.opslag));
            t.put("Looks", l.opslag.getCompoundOrEmpty("Looks").copy());
            t.putInt("Gebouw", l.gebouw);
            hier.add(t);
        }
        root.put("Loges", hier);
        return root;
    }

    static boolean isLoge(Base b, UUID id) {
        for (Base.Loge l : b.loges) {
            if (l.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // sending and bringing home
    // =====================================================================================================================

    /** One of your guhs comes to stay. Null = done, else why not. */
    @Nullable
    static Base.Fout stuur(ServerPlayer p, Base b, UUID id) {
        MinecraftServer s = p.level().getServer();
        if (b.besteLevel(GebouwType.GUHPENSION) <= 0) {
            return new Base.Fout("erfenis.fout.geen_pension");
        }
        if (b.loges.size() >= b.pensionPlekken()) {
            return new Base.Fout("erfenis.fout.pension_vol", Integer.toString(b.pensionPlekken()));
        }
        String niet = Pensioen.nietNu(s, p.getUUID(), id);
        if (niet != null) {
            return new Base.Fout("erfenis.fout.pension_niet");
        }
        if (Pensioen.bezig(s, p.getUUID(), id) != null) {
            return new Base.Fout("erfenis.fout.pension_bezig");
        }
        Entity e = Band.zoekGeladen(s, id);
        if (e == null) {
            Roepen.Uitkomst u = Roepen.roep(p, id);
            e = Band.zoekGeladen(s, id);
            if (u != Roepen.Uitkomst.OK || e == null) {
                return new Base.Fout("erfenis.fout.pension_roep");
            }
        }
        if (!(e instanceof GuhEntity guh) || !p.getUUID().equals(guh.getOwnerUUID())) {
            return new Base.Fout("erfenis.fout.pension_niet");
        }
        ServerLevel level = (ServerLevel) guh.level();
        level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        CompoundTag opslag = GuhOpslag.bewaar(guh, PlekSoort.OP_VAKANTIE, Component.translatable("erfenis.pension.plek", b.naam));
        b.loges.add(new Base.Loge(id, opslag, kiesGebouw(b)));
        return null;
    }

    /** The production building with the fewest logés (max 2 each), or -1. */
    static int kiesGebouw(Base b) {
        int beste = -1, minst = 2;
        for (Gebouw g : b.gebouwen) {
            if (g.type.maakt() && g.level > 0 && b.logesBij(g.id) < minst) {
                minst = b.logesBij(g.id);
                beste = g.id;
            }
        }
        return beste;
    }

    /** A logé goes home: as a picked-up guh into your pockets (or at your feet when they are full). */
    @Nullable
    static Base.Fout haal(ServerPlayer p, Base b, UUID id) {
        Base.Loge l = null;
        for (Base.Loge x : b.loges) {
            if (x.id.equals(id)) {
                l = x;
            }
        }
        if (l == null) {
            return new Base.Fout("erfenis.fout.weg");
        }
        MinecraftServer s = p.level().getServer();
        haalStandIn(s, b, id);
        if (Band.zoekGeladen(s, id) != null) {
            return new Base.Fout("erfenis.fout.pension_dubbel");   // (never doubled)
        }
        CompoundTag data = itemData(p, l.opslag);
        if (data.isEmpty()) {
            return new Base.Fout("erfenis.fout.weg");
        }
        ItemStack stack = PickedUpGuhItem.of(data);
        b.loges.remove(l);
        ItemStack kopie = stack.copy();
        if (p.getInventory().add(stack)) {
            GuhVolger.item(kopie, PlekSoort.ITEM_SPELER, p.level().dimension(), p.blockPosition(), p.getGameProfile().name(), p.level().getGameTime());
        } else {
            ItemEntity item = new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), stack);
            item.setDefaultPickUpDelay();
            p.level().addFreshEntity(item);
            GuhVolger.item(kopie, PlekSoort.ITEM_GROND, p.level().dimension(), item.blockPosition(), "", p.level().getGameTime());
        }
        p.level().playSound(null, p.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1f);
        return null;
    }

    /** The picked-up item's guh data: the stored entity, free again (like the Knabbelvelden's way home). */
    private static CompoundTag itemData(ServerPlayer p, CompoundTag opslag) {
        CompoundTag t = opslag.getCompoundOrEmpty("Data").copy();
        if (t.isEmpty()) {
            return t;
        }
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
        t.putBoolean("Sitting", false);
        CompoundTag looks = opslag.getCompoundOrEmpty("Looks");
        if (looks.contains("Naam")) {
            Tekst.put(t, "GuhDisplayName", Tekst.get(looks, "Naam"));
        }
        t.putString(PickedUpGuhItem.EIGENAAR, p.getUUID().toString());
        t.putString(PickedUpGuhItem.EIGENAAR_NAAM, p.getGameProfile().name());
        UUID id = GuhOpslag.id(opslag);
        if (id != null) {
            int nr = GuhNummers.nummer(p.level().getServer(), p.getUUID(), id);
            if (nr > 0) {
                t.putInt(PickedUpGuhItem.NUMMER, nr);
            }
            t.putInt(PickedUpGuhItem.HARTJES, Band.hartjes(p.level().getServer(), p.getUUID(), id));
        }
        return t;
    }

    /** Which building a logé helps (-1: none; max 2 per building). */
    @Nullable
    static Base.Fout helpt(Base b, UUID id, int gebouw) {
        for (Base.Loge l : b.loges) {
            if (l.id.equals(id)) {
                if (gebouw >= 0) {
                    Gebouw g = b.gebouw(gebouw);
                    if (g == null || !g.type.maakt()) {
                        return new Base.Fout("erfenis.fout.weg");
                    }
                    if (l.gebouw != gebouw && b.logesBij(gebouw) >= 2) {
                        return new Base.Fout("erfenis.fout.loges_max");
                    }
                }
                l.gebouw = gebouw;
                return null;
            }
        }
        return new Base.Fout("erfenis.fout.weg");
    }

    // =====================================================================================================================
    // the stand-ins walking on the plot
    // =====================================================================================================================

    /** Every second: stand-ins for every base somebody is on, none for the others. */
    static void tick(MinecraftServer s) {
        ServerLevel level = Erfenis.level(s);
        if (level == null) {
            return;
        }
        ErfenisData data = ErfenisData.get(s);
        for (Base b : data.alleBases()) {
            boolean bezocht = !b.loges.isEmpty() && Erfenis.bezocht(s, b);
            Map<UUID, UUID> hier = PER_BASE.computeIfAbsent(b.id, k -> new HashMap<>());
            // gone: logés that left, or nobody here
            for (var it = hier.entrySet().iterator(); it.hasNext();) {
                var e = it.next();
                Entity ent = level.getEntity(e.getValue());
                if (!bezocht || !isLoge(b, e.getKey()) || ent == null || !ent.isAlive()) {
                    if (ent != null) {
                        ent.discard();
                    }
                    LEVEND.remove(e.getValue());
                    it.remove();
                }
            }
            if (!bezocht) {
                continue;
            }
            for (Base.Loge l : b.loges) {
                if (!hier.containsKey(l.id)) {
                    GuhEntity g = maak(level, b, l);
                    if (g != null) {
                        hier.put(l.id, g.getUUID());
                    }
                }
            }
        }
    }

    private static void haalStandIn(MinecraftServer s, Base b, UUID logeId) {
        Map<UUID, UUID> hier = PER_BASE.get(b.id);
        UUID e = hier == null ? null : hier.remove(logeId);
        ServerLevel level = Erfenis.level(s);
        if (e != null && level != null) {
            Entity ent = level.getEntity(e);
            if (ent != null) {
                ent.discard();
            }
            LEVEND.remove(e);
        }
    }

    /** A stand-in with the logé's looks and name, near its building, wandering the plot. */
    @Nullable
    static GuhEntity maak(ServerLevel level, Base b, Base.Loge l) {
        GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (g == null) {
            return null;
        }
        CompoundTag looks = l.opslag.getCompoundOrEmpty("Looks");
        g.setVariant(GuhVariant.byId(looks.getStringOr("Variant", "")));
        GuhPersonality aard = GuhPersonality.byId(looks.getStringOr("Personality", ""));
        if (aard != null) {
            g.setPersonality(aard);
        }
        CompoundTag kleren = looks.getCompoundOrEmpty("Clothes");
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(kleren.getStringOr(slot.name(), ""));
            if (c != null) {
                g.wear(c);
            }
        }
        int haar = looks.getIntOr("Haar", -1);
        if (haar >= 0) {
            g.setHaarkleur(haar);
        }
        g.setGuhScale(Math.min(looks.getFloatOr("Scale", 1f), 1.4f));
        if (looks.getBooleanOr("Baby", false)) {
            g.setAge(-24000);
        }
        g.setCustomName(GuhOpslag.naam(l.opslag).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        BlockPos o = Plot.oorsprong(b.cx, b.cz);
        Gebouw bij = b.gebouw(l.gebouw);
        int tx = bij != null ? bij.x + bij.b() / 2 : Plot.MIDDEN + 12, tz = bij != null ? bij.z + bij.d() + 1 : Plot.MIDDEN + 12;
        BlockPos plek = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.offset(tx, 0, tz));
        g.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        g.setHomeTo(o.offset(Plot.MAAT / 2, 1, Plot.MAAT / 2), Plot.MAAT / 2);
        g.setInvulnerable(true);
        g.getPersistentData().putString(STANDIN, b.id.toString());
        LEVEND.put(g.getUUID(), b.id);
        if (!level.addFreshEntity(g)) {
            LEVEND.remove(g.getUUID());
            return null;
        }
        level.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + 0.8, g.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
        return g;
    }

    /** A stand-in that isn't one of ours (saved by a crash, loaded from disk) never comes in. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getPersistentData().contains(STANDIN)
                && !LEVEND.containsKey(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Is this entity a logé stand-in (the server keeps it off Band's books)? */
    public static boolean isStandIn(Entity e) {
        return e.getPersistentData().contains(STANDIN);
    }

    /** The server stops: no stand-in is saved. */
    static void allesWeg(MinecraftServer s) {
        ServerLevel level = Erfenis.level(s);
        for (UUID id : List.copyOf(LEVEND.keySet())) {
            Entity e = level == null ? null : level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        LEVEND.clear();
        PER_BASE.clear();
    }

    /** (tests) the stand-ins alive now. */
    static List<UUID> standIns() {
        return new ArrayList<>(LEVEND.keySet());
    }

    static Vec3 midden(Base b) {
        return Vec3.atCenterOf(Plot.oorsprong(b.cx, b.cz).offset(Plot.MAAT / 2, 1, Plot.MAAT / 2));
    }
}
