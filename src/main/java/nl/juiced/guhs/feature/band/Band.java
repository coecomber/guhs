package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModEntities;
import org.slf4j.Logger;

import net.minecraft.core.UUIDUtil;
/**
 * De hartjesmeter (2.10 "Lieve vadsjes van elkaar"): every tamed guh has hearts with its owner, and hearts NEVER go
 * down. Three levels ({@link BandNiveau}): "lieve vadsjes van elkaar", "mega lieve vadsjes van elkaar", "zielsguh bff
 * 5evr &lt;3". This class is the shared API of the 2.10 features:
 * <ul>
 *   <li>identity: {@link #isBandGuh}, {@link #id}, {@link #eigenaar}, {@link #samenGuhs};</li>
 *   <li>hearts: {@link #geefHartjes} (per-day caps per {@link Reden}, x1.5 when blij), {@link #hartjes}, {@link #niveau},
 *       {@link #aantal}, listeners {@link #opNiveau} / {@link #opHartjes};</li>
 *   <li>the moments bus: {@link #moment} / {@link #opMoment} (see {@link Moment} for who produces what);</li>
 *   <li>the happy buff: {@link #isBlij}, {@link #maakBlij}, {@link #klusSnelheid}.</li>
 * </ul>
 * Band guhs are tamed {@code guhs:guh} (exact type, not the RaceGuh/Parade/Kapper stand-ins) with an owner; calls for
 * other mobs are harmless no-ops. The data lives in {@link BandData} (keyed by owner and band id).
 */
public final class Band {
    private static final Logger LOG = LogUtils.getLogger();
    /** Persistent data key of a maatje's own band id (its entity UUID changes when it is picked up). */
    public static final String BAND_ID = "guhs_band_id";
    /** Persistent data key: blij until this game time. */
    public static final String BLIJ_TOT = "guhs_band_blij_tot";

    @Nullable
    private static MinecraftServer server;

    private Band() {
    }

    // =====================================================================================================================
    // identity
    // =====================================================================================================================

    /** A tamed guhs:guh with an owner (not a race/parade/kapper stand-in). */
    public static boolean isBandGuh(Entity e) {
        return e instanceof GuhEntity g && g.getType() == ModEntities.GUH.get() && g.isTame() && g.getOwnerUUID() != null;
    }

    /**
     * The band id: a guh's entity UUID (kept when it is picked up); a maatje's own id in its persistent data (made once, it
     * survives being picked up although its entity UUID doesn't).
     */
    public static UUID id(Entity e) {
        if (e instanceof GuhEntity) {
            return e.getUUID();
        }
        CompoundTag data = e.getPersistentData();
        if (!data.read(BAND_ID, UUIDUtil.CODEC).isPresent()) {
            data.store(BAND_ID, UUIDUtil.CODEC, e.getUUID());
        }
        return data.read(BAND_ID, UUIDUtil.CODEC).orElseThrow();
    }

    @Nullable
    public static UUID eigenaar(Entity e) {
        return e instanceof OwnableEntity o ? nl.juiced.guhs.entity.Owners.uuid(o) : null;
    }

    /** The player's own loaded band guhs within r blocks (not the ones asleep inside a huisje). */
    public static List<GuhEntity> samenGuhs(ServerPlayer p, double r) {
        return p.level().getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(r),
                g -> isBandGuh(g) && p.getUUID().equals(g.getOwnerUUID()) && !Huisjes.isBinnen(g) && g.distanceTo(p) <= r);
    }

    /** The owner, when online. */
    @Nullable
    public static ServerPlayer eigenaarOnline(Entity e) {
        UUID owner = eigenaar(e);
        MinecraftServer s = e.level().getServer();
        return owner == null || s == null ? null : s.getPlayerList().getPlayer(owner);
    }

    // =====================================================================================================================
    // the server + day
    // =====================================================================================================================

    static void server(@Nullable MinecraftServer s) {
        server = s;
    }

    /** The running server (set on server start), or null. */
    @Nullable
    public static MinecraftServer server() {
        return server;
    }

    /** The Minecraft day (overworld clock). */
    public static long dag(MinecraftServer s) {
        return nl.juiced.guhs.world.GuhTime.dayTime(s.overworld()) / 24000L;
    }

    @Nullable
    static BandData.Rec rec(Entity guh) {
        if (!isBandGuh(guh) || guh.level().getServer() == null) {
            return null;
        }
        return BandData.get(guh.level().getServer()).rec(eigenaar(guh), id(guh));
    }

    /** The record of a band guh, made and filled in when needed (name, looks, the day it became yours). */
    @Nullable
    static BandData.Rec bijwerken(Entity guh) {
        BandData.Rec r = rec(guh);
        if (r != null && guh instanceof GuhEntity g) {
            r.naam = g.hasCustomName() ? g.getCustomName().copy() : Component.empty();   // (1.2.0: see BandData.Rec.naam)
            r.looks = looks(g);
            if (r.sindsDag < 0) {
                r.sindsDag = dag(g.level().getServer());
            }
        }
        return r;
    }

    /** The looks of a guh for the Guhdex preview (what the client needs to build a copy). */
    public static CompoundTag looks(GuhEntity g) {
        CompoundTag t = new CompoundTag();
        t.putString("Variant", g.getVariant().id());
        t.putString("Personality", g.getPersonality().id());
        CompoundTag clothes = new CompoundTag();
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = g.getClothes(slot);
            if (c != null) {
                clothes.putString(slot.name(), c.id());
            }
        }
        t.put("Clothes", clothes);
        t.putInt("Haar", g.getHaarkleur());
        t.putFloat("Scale", g.getGuhScale());
        t.putBoolean("Baby", g.isBaby());
        if (g.hasCustomName()) {
            nl.juiced.guhs.taal.Tekst.put(t, "Naam", g.getCustomName());
        }
        return t;
    }

    // =====================================================================================================================
    // hearts
    // =====================================================================================================================

    @FunctionalInterface
    public interface NiveauLuisteraar {
        /** Fired once per new level; for an offline owner at the next login (then guh may be null). */
        void nieuw(ServerPlayer eigenaar, @Nullable Mob guh, UUID bandId, BandNiveau niveau);
    }

    @FunctionalInterface
    public interface HartjesLuisteraar {
        /** Hearts were added (erbij &gt; 0). The owner is null when offline. */
        void erbij(Mob guh, @Nullable ServerPlayer eigenaar, int erbij, Reden reden);
    }

    @FunctionalInterface
    public interface MomentLuisteraar {
        void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde);
    }

    private static final List<NiveauLuisteraar> NIVEAU = new CopyOnWriteArrayList<>();
    private static final List<HartjesLuisteraar> HARTJES = new CopyOnWriteArrayList<>();
    private static final List<MomentLuisteraar> MOMENTEN = new CopyOnWriteArrayList<>();

    public static void opNiveau(NiveauLuisteraar l) {
        NIVEAU.add(l);
    }

    public static void opHartjes(HartjesLuisteraar l) {
        HARTJES.add(l);
    }

    public static void opMoment(MomentLuisteraar l) {
        MOMENTEN.add(l);
    }

    /**
     * Gives hearts (never takes any). Returns what was really added: 0 when capped for today or not a band guh. Blij guhs
     * get x1.5 (rounded up). Shows floating hearts and "+N" to the owner, and fires the level-ups.
     */
    public static int geefHartjes(Mob guh, @Nullable ServerPlayer speler, int aantal, Reden reden) {
        if (aantal <= 0 || !isBandGuh(guh) || !(guh.level() instanceof ServerLevel level)) {
            return 0;
        }
        MinecraftServer s = level.getServer();
        BandData data = BandData.get(s);
        BandData.Rec r = bijwerken(guh);
        if (r == null) {
            return 0;
        }
        long vandaag = dag(s);
        if (r.dag != vandaag) {
            r.dag = vandaag;
            java.util.Arrays.fill(r.vandaag, 0);
        }
        int n = isBlij(guh) ? (int) Math.ceil(aantal * 1.5) : aantal;
        int dagMax = reden.dagMax() * (reden == Reden.VOEREN && guh instanceof GuhEntity g ? nl.juiced.guhs.feature.verhaal.VariantGedragen.voerFactor(g) : 1);
        n = Math.min(n, dagMax - r.vandaag[reden.ordinal()]);
        if (n <= 0) {
            return 0;
        }
        r.vandaag[reden.ordinal()] += n;
        BandNiveau voor = r.niveau();
        r.hartjes = (int) Math.min(Integer.MAX_VALUE, (long) r.hartjes + n);
        data.setDirty();
        ServerPlayer eigenaar = s.getPlayerList().getPlayer(eigenaar(guh));
        toonHartjes(level, guh, eigenaar, n, reden, r);
        for (HartjesLuisteraar l : HARTJES) {
            try {
                l.erbij(guh, eigenaar, n, reden);
            } catch (RuntimeException e) {
                LOG.warn("Band hartjes listener failed", e);
            }
        }
        if (r.niveau() != voor) {
            niveauOmhoog(guh, r);
        }
        return n;
    }

    private static void toonHartjes(ServerLevel level, Mob guh, @Nullable ServerPlayer eigenaar, int n, Reden reden, BandData.Rec r) {
        level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(),
                Math.min(6, 1 + n / 3), guh.getBbWidth() * 0.4, 0.15, guh.getBbWidth() * 0.4, 0.02);
        if (eigenaar != null && !reden.stil()) {
            ModNetworking.sendTo(eigenaar, new BandPayloads.Hartjes(guh.getId(), n));
            BandNiveau volgende = r.niveau().volgende();
            Component voortgang = volgende == null ? Component.translatable("gui.guhs.band.hartjes_max", r.hartjes)
                    : Component.translatable("gui.guhs.band.hartjes_voortgang", r.hartjes, volgende.drempel());
            eigenaar.sendOverlayMessage(Component.translatable("gui.guhs.band.hartjes_erbij", n, guh.getDisplayName(), voortgang)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The guh reached one or more new levels: announce each (now, or at the owner's next login). */
    private static void niveauOmhoog(Mob guh, BandData.Rec r) {
        MinecraftServer s = guh.level().getServer();
        BandData data = BandData.get(s);
        ServerPlayer eigenaar = s.getPlayerList().getPlayer(eigenaar(guh));
        BandNiveau nu = r.niveau();
        for (int i = r.niveau + 1; i <= nu.ordinal(); i++) {
            r.niveau = i;
            if (eigenaar != null) {
                meld(eigenaar, guh, r.id, BandNiveau.byIndex(i));
            } else {
                r.teMelden.add(i);
            }
        }
        r.niveau = Math.max(r.niveau, nu.ordinal());
        data.setDirty();
        if (nu == BandNiveau.ZIELSGUH) {
            BandVlaggen.zet(guh, BandVlaggen.ZIELSGUH, true);
        }
    }

    /** One level-up for an online owner: chat, sound, big hearts, dagboek, advancement, listeners. */
    static void meld(ServerPlayer eigenaar, @Nullable Mob guh, UUID bandId, BandNiveau niveau) {
        if (niveau == BandNiveau.GEEN) {
            return;
        }
        BandData.Rec r = BandData.get(eigenaar.level().getServer()).vind(eigenaar.getUUID(), bandId);
        Component naam = guh != null ? guh.getDisplayName() : r == null ? Component.literal("Guh") : r.weergave();
        eigenaar.sendSystemMessage(Component.translatable("gui.guhs.band.niveau_omhoog", naam, niveau.naam().copy()
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
        eigenaar.sendOverlayMessage(Component.translatable("gui.guhs.band.niveau_titel", niveau.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
        eigenaar.level().playSound(null, guh != null ? guh.blockPosition() : eigenaar.blockPosition(), BandFeature.NIVEAU_GELUID.get(),
                SoundSource.NEUTRAL, 1f, 1f);
        if (guh != null && guh.level() instanceof ServerLevel level) {
            level.sendParticles(BandFeature.GROOT_HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.6, guh.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.6, guh.getZ(), 14,
                    guh.getBbWidth() * 0.7, 0.4, guh.getBbWidth() * 0.7, 0.05);
            if (guh instanceof GuhEntity g) {
                g.triggerAnim("action", "happy");
            }
            ModNetworking.sendTo(eigenaar, new BandPayloads.Hartjes(guh.getId(), -1 - niveau.ordinal()));
        }
        String eerste = "eerste_" + niveau.id();
        if (guh != null) {
            Dagboek.eersteKeer(guh, eigenaar, eerste);
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band.niveau_" + niveau.id(), eigenaar.getGameProfile().name());
        } else {
            Dagboek.eersteKeer(eigenaar.level().getServer(), eigenaar.getUUID(), bandId, eerste);
            Dagboek.wistJeDat(eigenaar.level().getServer(), eigenaar.getUUID(), bandId, "gui.guhs.wistjedat.band.niveau_" + niveau.id(),
                    eigenaar.getGameProfile().name());
        }
        GidsFeature.grant(eigenaar, "lieve_vadsjes/band_" + niveau.id());
        for (NiveauLuisteraar l : NIVEAU) {
            try {
                l.nieuw(eigenaar, guh, bandId, niveau);
            } catch (RuntimeException e) {
                LOG.warn("Band niveau listener failed", e);
            }
        }
    }

    /** At login: the level-ups that happened while the owner was away. */
    static void meldAchterstallig(ServerPlayer eigenaar) {
        BandData data = BandData.get(eigenaar.level().getServer());
        for (BandData.Rec r : data.guhsVan(eigenaar.getUUID())) {
            if (r.teMelden.isEmpty()) {
                continue;
            }
            List<Integer> te = new ArrayList<>(r.teMelden);
            r.teMelden.clear();
            data.setDirty();
            Mob guh = zoekGeladen(eigenaar.level().getServer(), r.id);
            for (int i : te) {
                meld(eigenaar, guh, r.id, BandNiveau.byIndex(i));
            }
        }
    }

    /** A loaded guh with this band id anywhere, or null. */
    @Nullable
    public static Mob zoekGeladen(MinecraftServer s, UUID bandId) {
        for (ServerLevel level : s.getAllLevels()) {
            if (level.getEntity(bandId) instanceof Mob m) {
                return m;
            }
        }
        return null;
    }

    public static int hartjes(Mob guh) {
        if (!isBandGuh(guh) || guh.level().getServer() == null) {
            return 0;
        }
        BandData.Rec r = BandData.get(guh.level().getServer()).vind(eigenaar(guh), id(guh));
        return r == null ? 0 : r.hartjes;
    }

    public static int hartjes(MinecraftServer s, UUID eigenaar, UUID bandId) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r == null ? 0 : r.hartjes;
    }

    public static BandNiveau niveau(Mob guh) {
        return BandNiveau.van(hartjes(guh));
    }

    public static BandNiveau niveau(MinecraftServer s, UUID eigenaar, UUID bandId) {
        return BandNiveau.van(hartjes(s, eigenaar, bandId));
    }

    /** How many of this owner's guhs reached at least this level (e.g. the zielsguhs for the Guhkamer). */
    public static int aantal(MinecraftServer s, UUID eigenaar, BandNiveau minstens) {
        return (int) BandData.get(s).guhsVan(eigenaar).stream().filter(r -> r.niveau().ordinal() >= minstens.ordinal()).count();
    }

    // =====================================================================================================================
    // moments
    // =====================================================================================================================

    /** Something happened to a band guh: fundament's own bookkeeping, then every listener. No-op for other mobs. */
    public static void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (!isBandGuh(guh) || guh.level().isClientSide() || guh.level().getServer() == null) {
            return;
        }
        try {
            eigen(guh, speler, m, waarde);
        } catch (RuntimeException e) {
            LOG.warn("Band moment {} failed", m, e);
        }
        for (MomentLuisteraar l : MOMENTEN) {
            try {
                l.moment(guh, speler, m, waarde);
            } catch (RuntimeException e) {
                LOG.warn("Band moment listener failed ({})", m, e);
            }
        }
    }

    private static final Map<String, String> DIM_EERSTE = Map.of(
            "guhs:guhmension", "eerste_guhmension",
            "guhs:guheinde", "eerste_guheinde");

    /** Fundament's own reaction to a moment (dagboek stats and first times). */
    private static void eigen(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        switch (m) {
            case GETEMD -> {
                BandData.Rec r = bijwerken(guh);
                if (r != null) {
                    BandData.get(guh.level().getServer()).setDirty();
                }
                if (Dagboek.eersteKeer(guh, speler, "getemd")) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band.getemd", speler != null ? speler.getGameProfile().name() : "?");
                }
                if (speler != null) {
                    GidsFeature.grant(speler, "lieve_vadsjes/root");
                }
            }
            case GEGETEN -> Dagboek.tel(guh, DagboekStat.KNABBELS_GEGETEN, 1);
            case AANGEAAID -> Dagboek.eersteKeer(guh, speler, "eerste_aai");
            case GEKNUFFELD -> {
                Dagboek.tel(guh, DagboekStat.KNUFFELS, 1);
                if (Dagboek.eersteKeer(guh, speler, "eerste_knuffel")) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band.eerste_knuffel");
                }
            }
            case RIT -> Dagboek.eersteKeer(guh, speler, "eerste_rit");
            case DIMENSIE -> {
                String eerste = DIM_EERSTE.get(waarde);
                if (eerste != null && Dagboek.eersteKeer(guh, speler, eerste)) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band." + eerste);
                }
            }
            case HUISJE_IN -> {
                if (Dagboek.eersteKeer(guh, speler, "eerste_huisje")) {
                    Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band.eerste_huisje", nl.juiced.guhs.feature.huisje.Huisje.tekst(waarde));
                }
            }
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // the happy buff
    // =====================================================================================================================

    public static boolean isBlij(Mob guh) {
        return guh.getPersistentData().getLongOr(BLIJ_TOT, 0L) > guh.level().getGameTime();
    }

    /** Happy for (at least) this many more ticks: sets {@link BandVlaggen#BLIJ} (fundament shows subtle sparkles). */
    public static void maakBlij(Mob guh, int ticks) {
        if (ticks <= 0 || guh.level().isClientSide()) {
            return;
        }
        long tot = Math.max(guh.getPersistentData().getLongOr(BLIJ_TOT, 0L), guh.level().getGameTime() + ticks);
        guh.getPersistentData().putLong(BLIJ_TOT, tot);
        BandVlaggen.zet(guh, BandVlaggen.BLIJ, true);
    }

    /** Chore speed: 1.5 when blij, else 1.0. */
    public static float klusSnelheid(Mob guh) {
        return isBlij(guh) ? 1.5f : 1.0f;
    }
}
