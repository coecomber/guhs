package nl.juiced.guhs.feature.guhkamer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.MaagManager;

/**
 * De Guhkamer in je Guhmaag (2.10): a guest room where your tamed guhs can stay when they don't come along. It lives in
 * your own maag plot, {@value #AFSTAND} blocks south of the maag's middle (so the maag's rules for building and visiting
 * count there too), behind a door in the maag. It grows with your guhs on "zielsguh bff 5evr &lt;3"
 * ({@link #breedte}, {@link #plekken}). Huisjes and toys work there like anywhere.
 * <p>
 * Guests ({@link GuhkamerData.Gast}) are real guhs in the room while somebody is in it; when the room is empty they are
 * kept as data, so the Guhbel ({@link #stuur}, {@link #roep}) always finds them.
 */
public final class Guhkamer {
    public static final long SALT = 20210501L;
    /** How far south of the maag's middle the room is. */
    public static final int AFSTAND = 200;
    /** Persistent data: this guh is a guest of the Guhkamer. */
    public static final String GAST = "guhs_guhkamer_gast";
    static final String DEUR_TOT = "guhs_guhkamer_deur_tot";

    /** Where a room is: its level and the middle of its floor. */
    public record Plek(ServerLevel level, BlockPos midden) {
    }

    /** Tests: the room of this owner is here (the game test server has no guhmaag). */
    public static final Map<UUID, Plek> TEST_PLEK = new ConcurrentHashMap<>();
    /** Per owner: how long guests in the world could not be found (their chunk unloaded). */
    private static final Map<UUID, Integer> KWIJT = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ChunkPos>> GEFORCEERD = new ConcurrentHashMap<>();

    public enum Uitkomst { OK, NIET_JOUW, GEEN_MAAG, VOL }

    private Guhkamer() {
    }

    // =====================================================================================================================
    // size
    // =====================================================================================================================

    /** The room's width (and depth): 16, and 4 more for every zielsguh (up to 48). */
    public static int breedte(int zielsguhs) {
        return 16 + 4 * Math.min(Math.max(0, zielsguhs), 8);
    }

    public static int hoogte(int zielsguhs) {
        return 7 + Math.min(Math.max(0, zielsguhs), 8) / 2;
    }

    /** How many guests fit: 6, and 3 more per zielsguh (up to 30). */
    public static int plekken(int zielsguhs) {
        return 6 + 3 * Math.min(Math.max(0, zielsguhs), 8);
    }

    /** The owner's guhs on "zielsguh bff 5evr &lt;3". */
    public static int zielsguhs(MinecraftServer s, UUID eigenaar) {
        return Band.aantal(s, eigenaar, BandNiveau.ZIELSGUH);
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /** The owner's room, or null (no maag yet / the guhmaag is not there). */
    @Nullable
    public static Plek plek(MinecraftServer s, UUID eigenaar) {
        Plek test = TEST_PLEK.get(eigenaar);
        if (test != null) {
            return test;
        }
        GuhWorldData.Maag maag = GuhWorldData.get(s).maagOf(eigenaar);
        ServerLevel level = MaagManager.level(s);
        if (maag == null || level == null) {
            return null;
        }
        return new Plek(level, MaagManager.center(maag.index).offset(0, 0, AFSTAND));
    }

    public static boolean heeftMaag(MinecraftServer s, UUID eigenaar) {
        return TEST_PLEK.containsKey(eigenaar) || GuhWorldData.get(s).maagOf(eigenaar) != null;
    }

    /** The inside of a room of this width and height around m. */
    public static AABB binnen(BlockPos m, int breedte, int hoogte) {
        int h = breedte / 2;
        return new AABB(m.getX() - h, m.getY(), m.getZ() - h, m.getX() + h, m.getY() + hoogte, m.getZ() + h);
    }

    /** Where you arrive in the room: just inside the door. */
    public static Vec3 aankomst(BlockPos m, int breedte) {
        BlockPos d = GuhkamerBouw.kamerDeur(m, breedte);
        return new Vec3(d.getX() + 0.5, d.getY(), d.getZ() - 1.5);
    }

    public static boolean isGast(Entity e) {
        return e.getPersistentData().getBoolean(GAST);
    }

    /** Marks a guh as a guest (or not): the server-side mark and the synced flag the Guh menu reads (2.10.1). */
    public static void markeer(Entity e, boolean gast) {
        if (gast) {
            e.getPersistentData().putBoolean(GAST, true);
        } else {
            e.getPersistentData().remove(GAST);
        }
        BandVlaggen.zet(e, BandVlaggen.GUHKAMER_GAST, gast);
    }

    /** Which Guhkamer button the Guh menu shows for this guh (2.10.1; works on the client too: synced data only). */
    public enum MenuKnop { GEEN, LOGEREN, UIT }

    public static MenuKnop menuKnop(GuhEntity guh) {
        if (guh.getType() != nl.juiced.guhs.registry.ModEntities.GUH.get() || !guh.isTame() || guh.getOwnerUUID() == null) {
            return MenuKnop.GEEN;
        }
        return BandVlaggen.heeft(guh, BandVlaggen.GUHKAMER_GAST) ? MenuKnop.UIT : MenuKnop.LOGEREN;
    }

    /** Anybody (a player) in this room right now? */
    public static boolean iemandBinnen(Plek p, GuhkamerData.Kamer k) {
        if (k.breedte <= 0) {
            return false;
        }
        AABB box = binnen(p.midden(), k.breedte, k.hoogte).inflate(2);
        return !p.level().getEntitiesOfClass(ServerPlayer.class, box, pl -> !pl.isSpectator()).isEmpty();
    }

    // =====================================================================================================================
    // building and growing
    // =====================================================================================================================

    /** Builds the owner's room, or makes it bigger when there are more zielsguhs now. True when something was built. */
    public static boolean zorgGebouwd(MinecraftServer s, UUID eigenaar) {
        Plek p = plek(s, eigenaar);
        if (p == null) {
            return false;
        }
        GuhkamerData data = GuhkamerData.get(s);
        GuhkamerData.Kamer k = data.kamer(eigenaar);
        int z = zielsguhs(s, eigenaar);
        int b = breedte(z), h = hoogte(z);
        if (k.breedte >= b && k.hoogte >= h) {
            return false;
        }
        b = Math.max(b, k.breedte);
        h = Math.max(h, k.hoogte);
        boolean eerste = k.breedte == 0;
        GuhkamerBouw.bouw(p.level(), p.midden(), b, h, k.breedte, k.hoogte);
        k.breedte = b;
        k.hoogte = h;
        data.setDirty();
        if (!eerste) {
            ServerPlayer baas = s.getPlayerList().getPlayer(eigenaar);
            if (baas != null) {
                baas.displayClientMessage(Component.translatable("gui.guhs.guhkamer.gegroeid", b, b, plekken(z)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                GidsFeature.grant(baas, "lieve_vadsjes/guhkamer_groei");
            }
            p.level().playSound(null, p.midden(), GuhkamerFeature.GROEI.get(), SoundSource.BLOCKS, 1f, 1f);
        }
        return true;
    }

    // =====================================================================================================================
    // the Guhbel: send a guh to the room, call it back
    // =====================================================================================================================

    /** Sends one of the player's own guhs to their Guhkamer. */
    public static Uitkomst stuur(ServerPlayer speler, GuhEntity guh) {
        MinecraftServer s = speler.server;
        if (!Band.isBandGuh(guh) || !speler.getUUID().equals(guh.getOwnerUUID())) {
            return Uitkomst.NIET_JOUW;
        }
        if (!heeftMaag(s, speler.getUUID())) {
            return Uitkomst.GEEN_MAAG;
        }
        GuhkamerData data = GuhkamerData.get(s);
        GuhkamerData.Kamer k = data.kamer(speler.getUUID());
        UUID id = guh.getUUID();
        if (!k.gasten.containsKey(id) && k.gasten.size() >= plekken(zielsguhs(s, speler.getUUID()))) {
            return Uitkomst.VOL;
        }
        ServerLevel van = (ServerLevel) guh.level();
        van.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        van.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), guh.getX(), guh.getY() + 1, guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
        van.playSound(null, guh.blockPosition(), GuhkamerFeature.WEG.get(), SoundSource.NEUTRAL, 1f, 1f);
        if (Huisjes.isBewoner(guh)) {
            Huisjes.trekUit(guh);
        }
        guh.stopRiding();
        guh.ejectPassengers();
        guh.setOrderedToSit(false);
        Band.moment(guh, speler, Moment.GUHKAMER, "");
        if (Dagboek.eersteKeer(guh, speler, "eerste_guhkamer")) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.guhkamer.eerste");
        } else if (van.random.nextInt(3) == 0) {
            Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.guhkamer.logeren_" + (1 + van.random.nextInt(3)));
        }
        markeer(guh, true);
        GuhkamerData.Gast gast = k.gasten.computeIfAbsent(id, i -> new GuhkamerData.Gast(i, guh.getName().getString()));
        gast.naam = guh.getName().getString();
        gast.looks = Band.looks(guh);
        gast.sinds = s.overworld().getGameTime();
        gast.plek = null;
        Plek p = plek(s, speler.getUUID());
        boolean alInKamer = p != null && guh.level() == p.level() && k.breedte > 0
                && binnen(p.midden(), k.breedte, k.hoogte).contains(guh.position());
        if (!alInKamer) {
            gast.data = bewaarData(guh);
            guh.discard();
            GuhVolger.zet(speler.getUUID(), id, new nl.juiced.guhs.feature.band.Plek(PlekSoort.GUHKAMER, MaagManager.GUHMAAG,
                    p == null ? BlockPos.ZERO : p.midden(), "", van.getGameTime()));
        } else {
            GuhVolger.zet(guh, PlekSoort.GUHKAMER, "");
        }
        data.setDirty();
        GidsFeature.grant(speler, "lieve_vadsjes/guhkamer_bel");
        if (p != null && iemandBinnen(p, k)) {
            materialiseer(p, k);
        }
        return Uitkomst.OK;
    }

    /** Calls a guest back to the player (from the room, wherever the player is). Returns the guh, or null. */
    @Nullable
    public static Entity roep(ServerPlayer speler, UUID id) {
        MinecraftServer s = speler.server;
        GuhkamerData data = GuhkamerData.get(s);
        GuhkamerData.Kamer k = data.vind(speler.getUUID());
        GuhkamerData.Gast gast = k == null ? null : k.gasten.get(id);
        if (gast == null) {
            return null;
        }
        CompoundTag tag;
        if (gast.opgeslagen()) {
            Huisje h = Huisjes.vanBewoner(s, speler.getUUID(), id);
            if (h != null) {
                Huisjes.trekUit(s, h, id);
            }
            tag = gast.data;
        } else {
            Plek p = plek(s, speler.getUUID());
            Entity live = p != null ? p.level().getEntity(id) : null;
            if (live == null) {
                live = Band.zoekGeladen(s, id);
            }
            if (live == null) {
                speler.displayClientMessage(Component.translatable("gui.guhs.guhkamer.even_geduld", gast.naam).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                KWIJT.merge(speler.getUUID(), 200, Math::max);   // (load its room to find it)
                return null;
            }
            if (Huisjes.isBewoner(live)) {
                Huisjes.trekUit(live);
            }
            live.stopRiding();
            tag = bewaarData(live);
            live.discard();
        }
        tag = bevrijd(tag);
        ServerLevel naar = speler.serverLevel();
        Vec3 kijk = speler.getLookAngle();
        Vec3 voor = speler.position().add(kijk.x * 1.5, 0, kijk.z * 1.5);
        AABB ruimte = EntityType.byString(tag.getString("id")).map(t -> t.getDimensions().makeBoundingBox(voor)).orElse(new AABB(BlockPos.containing(voor)));
        Vec3 at = naar.noCollision(ruimte) ? voor : speler.position();
        Entity e = PickedUpGuhItem.release(naar, tag, at.x, at.y, at.z, speler.getYRot() + 180);
        if (e == null) {
            return null;
        }
        k.gasten.remove(id);
        data.setDirty();
        naar.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.4, e.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        naar.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.2, 0.3, 0);
        naar.playSound(null, e.blockPosition(), GuhkamerFeature.TERUG.get(), SoundSource.NEUTRAL, 1f, 1f);
        GuhVolger.zet(e, PlekSoort.WERELD, "");
        speler.displayClientMessage(Component.translatable("gui.guhs.guhkamer.geroepen", e.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return e;
    }

    /** A guh as saved data (like picking it up: same UUID, owner, everything). */
    static CompoundTag bewaarData(Entity e) {
        CompoundTag tag = new CompoundTag();
        e.saveWithoutId(tag);
        tag.putString("id", EntityType.getKey(e.getType()).toString());
        tag.putBoolean("Sitting", false);
        return tag;
    }

    /** Takes the guest mark, and any huisje home / asleep-inside state, off a guh's saved data. */
    static CompoundTag bevrijd(CompoundTag tag) {
        CompoundTag t = tag.copy();
        CompoundTag nf = t.getCompound("NeoForgeData");
        nf.remove(GAST);
        t.putInt("KnusVlaggen", t.getInt("KnusVlaggen") & ~BandVlaggen.GUHKAMER_GAST);
        boolean binnen = nf.getBoolean(Huisjes.BINNEN);
        nf.remove(Huisjes.THUIS);
        nf.remove(Huisjes.DIM);
        nf.remove(Huisjes.BINNEN);
        t.put("NeoForgeData", nf);
        if (binnen) {
            t.putBoolean("NoGravity", false);
            t.putInt("KnusVlaggen", t.getInt("KnusVlaggen") & ~BandVlaggen.HUISJE_BINNEN);
        }
        return t;
    }

    // =====================================================================================================================
    // guests in the room while somebody is there, kept as data when it's empty
    // =====================================================================================================================

    /** Puts the kept guests in the room. */
    static void materialiseer(Plek p, GuhkamerData.Kamer k) {
        if (k.breedte <= 0) {
            return;
        }
        AABB box = binnen(p.midden(), k.breedte, k.hoogte);
        boolean anders = false;
        for (GuhkamerData.Gast g : k.gasten.values()) {
            if (!g.opgeslagen()) {
                continue;
            }
            Vec3 at = g.plek != null && box.contains(g.plek) ? g.plek : vrijePlek(p, k);
            Entity e = PickedUpGuhItem.release(p.level(), g.data, at.x, at.y, at.z, p.level().random.nextFloat() * 360);
            if (e != null) {
                markeer(e, true);
                if (e instanceof PathfinderMob mob && !Huisjes.isBewoner(e)) {
                    mob.restrictTo(p.midden(), k.breedte / 2 + 1);
                }
                g.data = new CompoundTag();
                anders = true;
            }
        }
        if (anders) {
            GuhkamerData.get(p.level().getServer()).setDirty();
        }
    }

    /** Keeps the guests in the room as data (nobody is there any more). Guests that can't be found stay "in the world". */
    static boolean bewaar(Plek p, GuhkamerData.Kamer k) {
        boolean alles = true;
        List<UUID> weg = new ArrayList<>();
        for (GuhkamerData.Gast g : k.gasten.values()) {
            if (g.opgeslagen()) {
                continue;
            }
            Entity e = p.level().getEntity(g.id);
            if (e == null || !e.isAlive()) {
                BlockPos waar = g.plek != null ? BlockPos.containing(g.plek) : p.midden();
                if (p.level().areEntitiesLoaded(ChunkPos.asLong(waar)) && Band.zoekGeladen(p.level().getServer(), g.id) == null) {
                    weg.add(g.id);   // (its chunk is there but it isn't: picked up as an item, or gone; it's not a guest any more)
                } else {
                    alles = false;
                }
                continue;
            }
            if (!binnen(p.midden(), k.breedte, k.hoogte).inflate(2).contains(e.position())) {
                markeer(e, false);   // (it left the room somehow: not a guest any more)
                weg.add(g.id);
                continue;
            }
            e.stopRiding();
            g.plek = e.position();
            g.naam = e.getName().getString();
            if (e instanceof GuhEntity guh) {
                g.looks = Band.looks(guh);
            }
            g.data = bewaarData(e);
            e.discard();
            GuhVolger.zet(k.eigenaar, g.id, new nl.juiced.guhs.feature.band.Plek(PlekSoort.GUHKAMER, p.level().dimension(), p.midden(), "",
                    p.level().getGameTime()));
        }
        weg.forEach(k.gasten::remove);
        GuhkamerData.get(p.level().getServer()).setDirty();
        return alles;
    }

    /** A random free spot on the floor of the room. */
    static Vec3 vrijePlek(Plek p, GuhkamerData.Kamer k) {
        int h = Math.max(2, k.breedte / 2 - 2);
        for (int i = 0; i < 12; i++) {
            BlockPos q = p.midden().offset(p.level().random.nextInt(2 * h) - h, 0, p.level().random.nextInt(2 * h) - h);
            if (p.level().getBlockState(q).getCollisionShape(p.level(), q).isEmpty()
                    && p.level().getBlockState(q.above()).getCollisionShape(p.level(), q.above()).isEmpty()) {
                return Vec3.atBottomCenterOf(q);
            }
        }
        return Vec3.atBottomCenterOf(p.midden());
    }

    /** Every 10 ticks: rooms with somebody in them get their guests, empty ones keep them as data. */
    static void tick(MinecraftServer s) {
        GuhkamerData data = GuhkamerData.get(s);
        for (GuhkamerData.Kamer k : List.copyOf(data.alle())) {
            if (k.gasten.isEmpty() || k.breedte <= 0) {
                continue;
            }
            Plek p = plek(s, k.eigenaar);
            if (p == null) {
                continue;
            }
            if (iemandBinnen(p, k)) {
                KWIJT.remove(k.eigenaar);
                materialiseer(p, k);
            } else if (k.iemandBuiten()) {
                if (bewaar(p, k)) {
                    KWIJT.remove(k.eigenaar);
                    losLaten(p, k.eigenaar);
                } else {
                    int kwijt = KWIJT.merge(k.eigenaar, 10, Integer::sum);
                    if (kwijt >= 200) {
                        vasthouden(p, k);   // their chunks unloaded before they were kept: load the room to find them
                    }
                }
            }
        }
    }

    /** Keeps the room's chunks loaded (until its guests are kept as data again). */
    private static void vasthouden(Plek p, GuhkamerData.Kamer k) {
        if (GEFORCEERD.containsKey(k.eigenaar)) {
            return;
        }
        List<ChunkPos> chunks = new ArrayList<>();
        AABB box = binnen(p.midden(), k.breedte, k.hoogte);
        for (int cx = ((int) Math.floor(box.minX)) >> 4; cx <= ((int) Math.floor(box.maxX)) >> 4; cx++) {
            for (int cz = ((int) Math.floor(box.minZ)) >> 4; cz <= ((int) Math.floor(box.maxZ)) >> 4; cz++) {
                chunks.add(new ChunkPos(cx, cz));
                p.level().setChunkForced(cx, cz, true);
            }
        }
        GEFORCEERD.put(k.eigenaar, chunks);
    }

    private static void losLaten(Plek p, UUID eigenaar) {
        List<ChunkPos> chunks = GEFORCEERD.remove(eigenaar);
        if (chunks != null) {
            for (ChunkPos c : chunks) {
                p.level().setChunkForced(c.x, c.z, false);
            }
        }
    }

    /** Before the server stops: every guest back into the data (so nobody gets lost). */
    static void allesBewaren(MinecraftServer s) {
        for (GuhkamerData.Kamer k : GuhkamerData.get(s).alle()) {
            Plek p = plek(s, k.eigenaar);
            if (p != null && k.iemandBuiten()) {
                bewaar(p, k);
                losLaten(p, k.eigenaar);
            }
        }
        KWIJT.clear();
    }

    // =====================================================================================================================
    // the doors
    // =====================================================================================================================

    /** Somebody walks through a Guhkamer door. */
    static void deur(ServerPlayer speler, BlockPos onder, GuhkamerDeurBlock.Kant kant) {
        long nu = speler.level().getGameTime();
        if (speler.getPersistentData().getLong(DEUR_TOT) > nu) {
            return;
        }
        speler.getPersistentData().putLong(DEUR_TOT, nu + 40);
        UUID eigenaar = eigenaarBij(speler.serverLevel(), onder);
        if (eigenaar == null) {
            return;
        }
        if (kant == GuhkamerDeurBlock.Kant.MAAG) {
            GuhWorldData.Maag maag = GuhWorldData.get(speler.server).maagOf(eigenaar);
            if (maag != null && !maag.mayVisit(speler.getUUID())) {
                speler.displayClientMessage(Component.translatable("gui.guhs.guhkamer.prive", maag.ownerName).withStyle(ChatFormatting.RED), true);
                return;
            }
            gaNaarBinnen(speler, eigenaar);
        } else {
            gaNaarBuiten(speler, eigenaar);
        }
    }

    /** Whose room / maag a door belongs to. */
    @Nullable
    static UUID eigenaarBij(ServerLevel level, BlockPos pos) {
        UUID test = null;
        double best = 40 * 40;
        for (Map.Entry<UUID, Plek> t : TEST_PLEK.entrySet()) {
            double d = t.getValue().midden().distSqr(pos);
            if (t.getValue().level() == level && d < best) {
                best = d;
                test = t.getKey();
            }
        }
        if (test != null) {
            return test;
        }
        GuhWorldData.Maag maag = MaagManager.maagAt(level.getServer(), pos);
        return maag == null ? null : maag.owner;
    }

    /** Into the owner's Guhkamer (building it first when needed). */
    public static boolean gaNaarBinnen(ServerPlayer speler, UUID eigenaar) {
        MinecraftServer s = speler.server;
        zorgGebouwd(s, eigenaar);
        Plek p = plek(s, eigenaar);
        GuhkamerData.Kamer k = GuhkamerData.get(s).kamer(eigenaar);
        if (p == null || k.breedte <= 0) {
            return false;
        }
        Vec3 at = aankomst(p.midden(), k.breedte);
        speler.getPersistentData().putLong(DEUR_TOT, speler.level().getGameTime() + 40);
        speler.teleportTo(p.level(), at.x, at.y, at.z, 180f, 0f);
        p.level().playSound(null, BlockPos.containing(at), GuhkamerFeature.DEUR_GELUID.get(), SoundSource.PLAYERS, 1f, 1f);
        materialiseer(p, k);
        if (speler.getUUID().equals(eigenaar)) {
            GidsFeature.grant(speler, "lieve_vadsjes/guhkamer_binnen");
            speler.displayClientMessage(Component.translatable("gui.guhs.guhkamer.welkom_thuis", k.gasten.size()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            GidsFeature.grant(speler, "lieve_vadsjes/guhkamer_bezoek");
            GuhWorldData.Maag maag = GuhWorldData.get(s).maagOf(eigenaar);
            speler.displayClientMessage(Component.translatable("gui.guhs.guhkamer.welkom_bezoek", maag == null ? "?" : maag.ownerName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return true;
    }

    /** Out of the room, back to its door in the maag. */
    public static void gaNaarBuiten(ServerPlayer speler, UUID eigenaar) {
        MinecraftServer s = speler.server;
        Plek p = plek(s, eigenaar);
        if (p == null) {
            return;
        }
        GuhkamerData.Kamer k = GuhkamerData.get(s).kamer(eigenaar);
        Vec3 at;
        GuhWorldData.Maag maag = GuhWorldData.get(s).maagOf(eigenaar);
        if (k.maagDeur != null) {
            at = Vec3.atBottomCenterOf(k.maagDeur.north());
        } else if (maag != null && !TEST_PLEK.containsKey(eigenaar)) {
            at = MaagManager.entryPoint(maag.index);
        } else {
            at = Vec3.atBottomCenterOf(p.midden().offset(0, 0, -(k.breedte / 2 + 4)));
        }
        speler.getPersistentData().putLong(DEUR_TOT, speler.level().getGameTime() + 40);
        speler.teleportTo(p.level(), at.x, at.y, at.z, 180f, 0f);
        p.level().playSound(null, BlockPos.containing(at), GuhkamerFeature.DEUR_GELUID.get(), SoundSource.PLAYERS, 1f, 0.9f);
    }

    /** Makes sure the maag has its door to the Guhkamer. */
    static void zorgMaagDeur(ServerLevel level, GuhWorldData.Maag maag) {
        GuhkamerData data = GuhkamerData.get(level.getServer());
        GuhkamerData.Kamer k = data.kamer(maag.owner);
        if (k.maagDeur != null && level.isLoaded(k.maagDeur)) {
            if (level.getBlockState(k.maagDeur).getBlock() instanceof GuhkamerDeurBlock) {
                return;
            }
            if (level.getBlockState(k.maagDeur).isAir() && level.getBlockState(k.maagDeur.above()).isAir()) {
                GuhkamerBouw.zetDeur(level, k.maagDeur, net.minecraft.core.Direction.NORTH, GuhkamerDeurBlock.Kant.MAAG);
                return;
            }
        } else if (k.maagDeur != null) {
            return;   // (not loaded: check again later)
        }
        BlockPos c = MaagManager.center(maag.index);
        if (!level.isLoaded(c)) {
            return;
        }
        k.maagDeur = GuhkamerBouw.zetMaagDeur(level, c);
        data.setDirty();
    }

    /** A guh became a zielsguh: its owner's room may grow (right away when it's there). */
    static void nieuwNiveau(ServerPlayer eigenaar, BandNiveau niveau) {
        if (niveau != BandNiveau.ZIELSGUH) {
            return;
        }
        GuhkamerData.Kamer k = GuhkamerData.get(eigenaar.server).vind(eigenaar.getUUID());
        if (k != null && k.breedte > 0) {
            zorgGebouwd(eigenaar.server, eigenaar.getUUID());
        } else if (heeftMaag(eigenaar.server, eigenaar.getUUID())) {
            eigenaar.displayClientMessage(Component.translatable("gui.guhs.guhkamer.groeit_straks").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
    }
}
