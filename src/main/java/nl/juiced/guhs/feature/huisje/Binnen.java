package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guhpixel.Regels;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import org.slf4j.Logger;

/**
 * 1.3.2 "Huisje betreden": the INSIDE of a Guhhuisje. The button "Naar binnen" of the huisje screen (owner and visitors)
 * brings the player to the room of that huisje in the hidden void dimension {@code guhs:huisje_binnen}; the door there
 * brings them back to just in front of the huisje.
 * <ul>
 *   <li><b>Cells.</b> Every huisje gets its own room on a grid: cell k has its min corner at
 *   ({@value #CEL} * (k % {@value #PER_RIJ}), {@value #Y}, {@value #CEL} * (k / {@value #PER_RIJ})). The cell number is
 *   kept with the huisje ({@link Huisje#cel}, from a counter in {@link Huisjes}: never reused, so rooms never collide) and
 *   given out the first time somebody goes in, also for huisjes that were built before this update. The room is stamped
 *   from the template of the huisje's size ({@link BinnenKamer}) when it is first needed and again when the template
 *   version went up ({@link #zorg}).</li>
 *   <li><b>The way back</b> is kept per player in their own saved data ({@code GuhQuests.saved(p)["guhs_huisje_binnen"]}:
 *   the huisje, the spot in front of its door and where the player stood), never in the respawn point. Without a usable
 *   spot: the own respawn point, else the world spawn ({@link #terug}).</li>
 *   <li><b>Safe.</b> The rooms are a {@link Regels#zone safe zone} of the Guhpixel rules (nothing is broken, placed, hurt,
 *   tossed or lost, no hunger) and a {@link Protected} area; a bed never sets the spawn point; a player who gets out of the
 *   room (a fall, an ender pearl) stands on the doormat again; a player found in the dimension without a valid room is put
 *   outside; nothing but players is ever loaded from disk there.</li>
 *   <li><b>The residents</b> you see are stand-ins ({@link BinnenInrichting}): the real guhs never change dimension or
 *   state because of a visit.</li>
 * </ul>
 * The game test server has no datapack dimensions: a test gives a huisje a room in its own test area ({@link #testKamer}).
 */
public final class Binnen {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("huisje_binnen"));
    /** The grid: cells this far apart, this many per row, rooms at this height. */
    public static final int CEL = 256, PER_RIJ = 512, Y = 64;
    /** The player's saved data: which huisje they are in and the way back. */
    public static final String DATA = "guhs_huisje_binnen";
    /** The tag of everything {@link BinnenInrichting} puts in a room (stand-ins, notes, blankets...). */
    public static final String TAG = "guhs_huisje_binnen";
    /** Ticks between the click and the step through the door (the fade covers it). */
    static final int FADE = 9;

    /** (Game tests) rooms outside the dimension: cell -&gt; the level and min corner. */
    private record TestCel(ServerLevel level, BlockPos oorsprong) {
    }

    private static final Map<Integer, TestCel> TEST = new ConcurrentHashMap<>();

    /** Somebody is on their way through the door (the fade runs). */
    private record Wacht(int tick, boolean naarBinnen, ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<UUID, Wacht> WACHT = new ConcurrentHashMap<>();
    /** The cells with somebody inside (their furnishing is kept up to date). */
    private static final Set<Integer> ACTIEF = new HashSet<>();

    private Binnen() {
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /** The level that holds this cell: the dimension, or a test's own level. Null on a server without the dimension. */
    @Nullable
    public static ServerLevel wereld(MinecraftServer server, int cel) {
        TestCel t = TEST.get(cel);
        return t != null ? t.level() : server.getLevel(DIM);
    }

    /** The min corner of the room in this cell. */
    public static BlockPos oorsprong(int cel) {
        TestCel t = TEST.get(cel);
        return t != null ? t.oorsprong() : new BlockPos(CEL * Math.floorMod(cel, PER_RIJ), Y, CEL * (cel / PER_RIJ));
    }

    /** The box of this huisje's room (null: it has no room yet, or its layout is unknown). */
    @Nullable
    public static AABB doos(MinecraftServer server, Huisje h) {
        BinnenKamer k = BinnenKamer.van(server, h.maat);
        return h.cel < 0 || k == null ? null : Stempel.doos(oorsprong(h.cel), k.maat());
    }

    /** In the room dimension (or in a test's room). */
    public static boolean in(@Nullable Entity e) {
        return e != null && in(e.level(), e.blockPosition());
    }

    public static boolean in(Level level, BlockPos pos) {
        if (level.dimension() == DIM) {
            return true;
        }
        if (!TEST.isEmpty() && !level.isClientSide()) {
            for (TestCel t : TEST.values()) {
                if (t.level() == level && Stempel.doos(t.oorsprong(), BinnenKamer.MAX).inflate(1).contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The huisje whose room this player's saved data names (null: none, or that huisje is gone). */
    @Nullable
    public static Huisje kamerVan(ServerPlayer p) {
        CompoundTag t = GuhQuests.saved(p).getCompound(DATA).orElse(null);
        if (t == null) {
            return null;
        }
        Identifier dim = Identifier.tryParse(t.getStringOr("HDim", ""));
        if (dim == null) {
            return null;
        }
        Huisje h = Huisjes.op(p.level().getServer(), ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLongOr("HPos", 0L)));
        return h == null || h.cel < 0 ? null : h;
    }

    /** Really standing in a room right now: the huisje, or null. */
    @Nullable
    public static Huisje binnenIn(ServerPlayer p) {
        if (!in(p)) {
            return null;
        }
        Huisje h = kamerVan(p);
        MinecraftServer s = p.level().getServer();
        AABB doos = h == null ? null : doos(s, h);
        return doos != null && p.level() == wereld(s, h.cel) && doos.inflate(0.5).contains(p.position()) ? h : null;
    }

    /** The players standing in this huisje's room. */
    public static List<ServerPlayer> spelers(MinecraftServer server, Huisje h) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (binnenIn(p) == h) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** Where a player stands after coming in (feet, the middle of the doormat). */
    static Vec3 mat(Huisje h, BinnenKamer k) {
        BlockPos o = oorsprong(h.cel);
        return new Vec3(o.getX() + k.mat().getX() + 0.5, o.getY() + k.mat().getY() + 0.07, o.getZ() + k.mat().getZ() + 0.5);
    }

    // =====================================================================================================================
    // the room
    // =====================================================================================================================

    /**
     * Makes sure this huisje has a cell and that its room stands there in the current template version. False when the
     * template is missing.
     */
    static boolean zorg(MinecraftServer server, Huisje h) {
        BinnenKamer k = BinnenKamer.van(server, h.maat);
        if (k == null) {
            return false;
        }
        if (h.cel < 0) {
            h.cel = Huisjes.nieuweCel(server);
            h.celVersie = 0;
            Huisjes.dirty();
        }
        ServerLevel level = wereld(server, h.cel);
        if (level == null) {
            return false;
        }
        int versie = BinnenKamer.versie(server);
        if (h.celVersie == versie) {
            return true;
        }
        BlockPos o = oorsprong(h.cel);
        BinnenInrichting.ruim(level, h);
        Stempel.leeg(level, o, BinnenKamer.MAX);
        Stempel.ruim(level, Stempel.doos(o, BinnenKamer.MAX).inflate(1));
        if (!Stempel.plaats(level, BinnenKamer.template(h.maat), o)) {
            return false;
        }
        LOGGER.debug("Guhs: room of huisje {} stamped in cell {} (version {})", h.naam, h.cel, versie);
        h.celVersie = versie;
        Huisjes.dirty();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {   // (somebody stood in the old room: back on the mat)
            if (p.level() == level && kamerVan(p) == h && Stempel.doos(o, BinnenKamer.MAX).inflate(1).contains(p.position())) {
                opDeMat(p, h, k, false);
            }
        }
        return true;
    }

    /** (Game tests) gives this huisje a room at this spot of the test's own level. */
    public static void testKamer(Huisje h, ServerLevel level, BlockPos oorsprong) {
        if (h.cel < 0) {
            h.cel = Huisjes.nieuweCel(level.getServer());
            h.celVersie = 0;
        }
        TEST.put(h.cel, new TestCel(level, oorsprong.immutable()));
    }

    /** (Game tests) forgets a test room (its blocks go with the test area). */
    public static void testKlaar(Huisje h) {
        if (h.cel >= 0) {
            TestCel t = TEST.remove(h.cel);
            if (t != null) {
                BinnenInrichting.ruim(t.level(), t.oorsprong());
            }
            ACTIEF.remove(h.cel);
            BinnenInrichting.vergeet(h.cel);
        }
    }

    // =====================================================================================================================
    // going in
    // =====================================================================================================================

    /** Why this player cannot go in now (null: they can). */
    @Nullable
    public static Component weigering(ServerPlayer p, Huisje h) {
        if (p.isPassenger() || p.isVehicle()) {
            return Component.translatable("gui.guhs.huisje.binnen.weiger.afstappen");
        }
        if (p.isSleeping()) {
            return Component.translatable("gui.guhs.huisje.binnen.weiger.wakker");
        }
        if (Minigames.playing(p) != null) {
            return Component.translatable("gui.guhs.huisje.binnen.weiger.spel");
        }
        MinecraftServer s = p.level().getServer();
        if (BinnenKamer.van(s, h.maat) == null || (h.cel >= 0 ? wereld(s, h.cel) : s.getLevel(DIM)) == null) {
            return Component.translatable("gui.guhs.huisje.binnen.weiger.storing");
        }
        return null;
    }

    /**
     * The button "Naar binnen": the doorbell rings, the screen fades and a moment later the player stands on the doormat.
     * False when refused (the reason is shown).
     */
    public static boolean vraag(ServerPlayer p, Huisje h) {
        if (WACHT.containsKey(p.getUUID()) || in(p)) {
            return false;
        }
        Component nee = weigering(p, h);
        if (nee != null) {
            p.sendOverlayMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        MinecraftServer s = p.level().getServer();
        ServerLevel buiten = s.getLevel(h.dim);
        if (buiten != null) {
            buiten.playSound(null, h.deur(), HuisjeFeature.BEL_GELUID.get(), SoundSource.BLOCKS, 0.9f, 1.26f);
        }
        ModNetworking.sendTo(p, new HuisjePayloads.BinnenFx(true));
        WACHT.put(p.getUUID(), new Wacht(s.getTickCount() + FADE, true, h.dim, h.pos));
        return true;
    }

    /** Through the door, now: the player stands on the doormat of this huisje's room. False when it cannot be done. */
    public static boolean betreed(ServerPlayer p, Huisje h) {
        MinecraftServer s = p.level().getServer();
        Component nee = weigering(p, h);
        BinnenKamer k = BinnenKamer.van(s, h.maat);
        if (nee == null && (k == null || !zorg(s, h))) {
            nee = Component.translatable("gui.guhs.huisje.binnen.weiger.storing");
        }
        ServerLevel level = nee != null ? null : wereld(s, h.cel);
        if (level == null) {
            p.sendOverlayMessage((nee == null ? Component.translatable("gui.guhs.huisje.binnen.weiger.storing") : nee).copy()
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        onthoud(p, h);
        for (Leashable dier : Leashable.leashableLeashedTo(p)) {
            dier.dropLeash();
        }
        Minigames.forget(p);   // (food and health stay what they are from here on)
        Vec3 mat = mat(h, k);
        p.teleport(new TeleportTransition(level, mat, Vec3.ZERO, k.yaw(), 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        level.playSound(null, BlockPos.containing(mat), HuisjeFeature.BEL_GELUID.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
        boolean eigen = h.eigenaar.equals(p.getUUID());
        p.sendOverlayMessage((eigen ? Component.translatable("gui.guhs.huisje.binnen.welkom", h.naamTekst())
                : Component.translatable("gui.guhs.huisje.binnen.welkom_gast", h.naamTekst(), h.eigenaarNaam.isEmpty() ? "?" : h.eigenaarNaam))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(p, "huisje_binnen");
        ACTIEF.add(h.cel);
        BinnenInrichting.ververs(level, h, k, true);
        return true;
    }

    /** Remembers which huisje the player is in and the way back (the way back only when they are not inside already). */
    private static void onthoud(ServerPlayer p, Huisje h) {
        CompoundTag oud = GuhQuests.saved(p).getCompound(DATA).orElse(null);
        CompoundTag t = new CompoundTag();
        if (in(p) && oud != null) {
            t = oud.copy();
        } else {
            BlockPos d = h.deur();
            t.putString("Dim", h.dim.identifier().toString());
            t.putDouble("X", d.getX() + 0.5);
            t.putDouble("Y", d.getY());
            t.putDouble("Z", d.getZ() + 0.5);
            t.putFloat("Yaw", h.facing.toYRot());
            // where the player stood (within reach of the huisje): the spare spot when the one at the door is blocked
            t.putString("EDim", p.level().dimension().identifier().toString());
            t.putDouble("EX", p.getX());
            t.putDouble("EY", p.getY());
            t.putDouble("EZ", p.getZ());
            t.putFloat("EYaw", p.getYRot());
        }
        t.putString("HDim", h.dim.identifier().toString());
        t.putLong("HPos", h.pos.asLong());
        GuhQuests.saved(p).put(DATA, t);
    }

    /** Back on the doormat (fell out of the room, or the room was stamped again). */
    static void opDeMat(ServerPlayer p, Huisje h, BinnenKamer k, boolean melden) {
        ServerLevel level = wereld(p.level().getServer(), h.cel);
        if (level == null) {
            return;
        }
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        p.stopRiding();
        Vec3 mat = mat(h, k);
        p.teleportTo(level, mat.x, mat.y, mat.z, Set.of(), k.yaw(), 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        if (melden) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.op_de_mat").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // =====================================================================================================================
    // going out
    // =====================================================================================================================

    /** The door inside: the screen fades and a moment later the player stands in front of the huisje. */
    public static boolean verlaat(ServerPlayer p) {
        if (WACHT.containsKey(p.getUUID()) || !in(p)) {
            return false;
        }
        ModNetworking.sendTo(p, new HuisjePayloads.BinnenFx(false));
        p.level().playSound(null, p.blockPosition(), HuisjeFeature.DEUR_GELUID.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
        WACHT.put(p.getUUID(), new Wacht(p.level().getServer().getTickCount() + FADE, false, Level.OVERWORLD, BlockPos.ZERO));
        return true;
    }

    /** Outside, now: in front of the huisje (or the best spot left, see {@link #terug}). The saved way back is used up. */
    public static void naarBuiten(ServerPlayer p, @Nullable Component bericht) {
        CompoundTag t = GuhQuests.saved(p).getCompound(DATA).orElse(null);
        GuhQuests.saved(p).remove(DATA);
        WACHT.remove(p.getUUID());
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        p.stopRiding();
        TeleportTransition tr = terug(p, t);
        p.teleport(tr);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        Minigames.forget(p);
        tr.newLevel().playSound(null, BlockPos.containing(tr.position()), HuisjeFeature.DEUR_GELUID.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
        if (bericht != null) {
            p.sendOverlayMessage(bericht.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * The way back: the spot in front of the huisje's door, else where the player stood when they went in, else the first
     * free spot above the door; without saved data (or when that dimension is gone) the own respawn point, else the world
     * spawn.
     */
    static TeleportTransition terug(ServerPlayer p, @Nullable CompoundTag t) {
        MinecraftServer s = p.level().getServer();
        if (t != null) {
            ServerLevel level = level(s, t.getStringOr("Dim", ""));
            if (level != null) {
                Vec3 deur = new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 80), t.getDoubleOr("Z", 0));
                float yaw = t.getFloatOr("Yaw", 0f);
                if (vrij(level, deur)) {
                    return new TeleportTransition(level, deur, Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING);
                }
                ServerLevel eigen = level(s, t.getStringOr("EDim", ""));
                Vec3 stond = new Vec3(t.getDoubleOr("EX", 0), t.getDoubleOr("EY", 80), t.getDoubleOr("EZ", 0));
                if (eigen != null && t.contains("EX") && vrij(eigen, stond)) {
                    return new TeleportTransition(eigen, stond, Vec3.ZERO, t.getFloatOr("EYaw", 0f), 0f, TeleportTransition.DO_NOTHING);
                }
                for (int dy = 1; dy <= 8; dy++) {
                    if (vrij(level, deur.add(0, dy, 0))) {
                        return new TeleportTransition(level, deur.add(0, dy, 0), Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING);
                    }
                }
            }
        }
        return p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
    }

    @Nullable
    private static ServerLevel level(MinecraftServer s, String dim) {
        Identifier id = Identifier.tryParse(dim);
        ResourceKey<Level> key = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        return key == null || key == DIM ? null : s.getLevel(key);
    }

    /** Room for a player to stand here (feet and head free, inside the world). */
    private static boolean vrij(ServerLevel level, Vec3 v) {
        BlockPos voet = BlockPos.containing(v.x, v.y + 0.01, v.z);
        if (!level.isInWorldBounds(voet) || !level.getWorldBorder().isWithinBounds(voet)) {
            return false;
        }
        for (BlockPos pos : List.of(voet, voet.above())) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * A huisje is gone (broken, picked up): whoever is inside stands outside again where it stood, its stand-ins are gone
     * and its room is cleared. The cell is never used again.
     */
    static void huisjeWeg(MinecraftServer server, Huisje h) {
        if (h.cel < 0) {
            return;
        }
        ServerLevel level = wereld(server, h.cel);
        String dim = h.dim.identifier().toString();
        for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) {
            CompoundTag t = GuhQuests.saved(p).getCompound(DATA).orElse(null);
            if (t != null && t.getLongOr("HPos", 0L) == h.pos.asLong() && dim.equals(t.getStringOr("HDim", "")) && in(p)) {
                naarBuiten(p, Component.translatable("gui.guhs.huisje.binnen.huisje_weg"));
            }
        }
        ACTIEF.remove(h.cel);
        BinnenInrichting.vergeet(h.cel);
        if (level != null) {
            BlockPos o = oorsprong(h.cel);
            BinnenInrichting.ruim(level, o);
            try {
                Stempel.leeg(level, o, BinnenKamer.MAX);
                Stempel.ruim(level, Stempel.doos(o, BinnenKamer.MAX).inflate(1));
            } catch (RuntimeException e) {
                LOGGER.warn("Guhs: clearing the room of huisje {} (cell {}) failed", h.naam, h.cel, e);
            }
        }
        TEST.remove(h.cel);
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    private static void tick(MinecraftServer server) {
        int nu = server.getTickCount();
        if (!WACHT.isEmpty()) {
            for (Map.Entry<UUID, Wacht> e : List.copyOf(WACHT.entrySet())) {
                if (nu < e.getValue().tick()) {
                    continue;
                }
                WACHT.remove(e.getKey());
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null || !p.isAlive()) {
                    continue;
                }
                Wacht w = e.getValue();
                if (w.naarBinnen()) {
                    Huisje h = Huisjes.op(server, w.dim(), w.pos());
                    if (h != null && !in(p)) {
                        betreed(p, h);
                    }
                } else if (in(p)) {
                    naarBuiten(p, Component.translatable("gui.guhs.huisje.binnen.buiten"));
                }
            }
        }
        Set<Integer> nuActief = new HashSet<>();
        for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) {
            if (!in(p) || !p.isAlive()) {
                continue;
            }
            boolean kijker = p.isSpectator() || p.isCreative();
            Huisje h = kamerVan(p);
            BinnenKamer k = h == null ? null : BinnenKamer.van(server, h.maat);
            ServerLevel level = h == null ? null : wereld(server, h.cel);
            if (k == null || level != p.level()) {
                // in the dimension without a valid room (the huisje is gone, or they got here another way): outside
                if (!kijker || h == null && GuhQuests.saved(p).contains(DATA)) {
                    naarBuiten(p, Component.translatable(GuhQuests.saved(p).contains(DATA) ? "gui.guhs.huisje.binnen.huisje_weg"
                            : "gui.guhs.huisje.binnen.niet_hier"));
                }
                continue;
            }
            if (h.celVersie != BinnenKamer.versie(server)) {
                zorg(server, h);   // (a newer room template: stamped again, everybody back on the mat)
            }
            AABB doos = Stempel.doos(oorsprong(h.cel), k.maat());
            if (!doos.inflate(0.3).contains(p.position())) {
                if (kijker && level.dimension() == DIM) {
                    continue;   // (an op looking around from outside)
                }
                opDeMat(p, h, k, true);
            }
            nuActief.add(h.cel);
            if ((nu + h.cel) % 20 == 0) {
                BinnenInrichting.ververs(level, h, k, false);
            }
            BinnenInrichting.tick(level, h, k, nu);
        }
        // rooms nobody is in any more: their stand-ins go away
        if (!ACTIEF.equals(nuActief)) {
            for (int cel : List.copyOf(ACTIEF)) {
                if (!nuActief.contains(cel)) {
                    ServerLevel level = wereld(server, cel);
                    if (level != null) {
                        BinnenInrichting.ruim(level, oorsprong(cel));
                    }
                    BinnenInrichting.vergeet(cel);
                }
            }
            ACTIEF.clear();
            ACTIEF.addAll(nuActief);
        }
        BinnenSlaap.tick(server);
    }

    /** Is somebody in this cell right now? */
    static boolean actief(int cel) {
        return ACTIEF.contains(cel);
    }

    // =====================================================================================================================
    // clicks in the room
    // =====================================================================================================================

    private static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!in(level, event.getPos())) {
            return;
        }
        if (level.isClientSide()) {
            // (the real dimension only: the server decides, the client just swings its arm and predicts nothing)
            if (!event.getEntity().getAbilities().instabuild) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Huisje h = binnenIn(p);
        BinnenKamer k = h == null ? null : BinnenKamer.van(p.level().getServer(), h.maat);
        if (k == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || WACHT.containsKey(p.getUUID())) {
            return;
        }
        BlockPos lokaal = event.getPos().subtract(oorsprong(h.cel));
        if (k.deur().contains(lokaal)) {
            verlaat(p);
        } else if (lokaal.equals(k.prikbord())) {
            BinnenInrichting.prikbord(p, h);
        } else if (lokaal.equals(k.logeerHoofd()) || lokaal.equals(k.logeerVoet())) {
            BinnenSlaap.slaap(p, h, k);
        } else {
            for (int i = 0; i < k.bedden().size(); i++) {
                BinnenKamer.Bed b = k.bedden().get(i);
                if (lokaal.equals(b.bed()) || lokaal.equals(b.bord()) || lokaal.equals(b.kastje()) || lokaal.equals(b.haak())) {
                    BinnenInrichting.bedKlik(p, h, i);
                    return;
                }
            }
        }
    }

    // =====================================================================================================================
    // registration
    // =====================================================================================================================

    static void register() {
        Regels.zone(Binnen::in);     // (no breaking, placing, damage, hunger, tossing: the Guhpixel rules)
        Protected.add(Binnen::in);   // (fire and fluids)
        NeoForge.EVENT_BUS.addListener(Binnen::opKlik);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerSetSpawnEvent event) -> {
            if (event.getSpawnLevel() == DIM) {
                event.setCanceled(true);   // (the logeerbedje never becomes your spawn point)
            }
        });
        // nothing but players is ever loaded from disk here: the stand-ins are made again when somebody comes in
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.loadedFromDisk() && !(event.getEntity() instanceof Player) && event.getLevel().dimension() == DIM) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getFrom() == DIM || event.getTo() == DIM) {
                Minigames.forget(event.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> WACHT.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            BinnenKamer.vergeet();
            WACHT.clear();
            ACTIEF.clear();
            BinnenInrichting.vergeetAlles();
            BinnenSlaap.vergeet();
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            // (the stand-ins are never saved: gone before the world is)
            for (int cel : List.copyOf(ACTIEF)) {
                ServerLevel level = wereld(event.getServer(), cel);
                if (level != null) {
                    BinnenInrichting.ruim(level, oorsprong(cel));
                }
            }
            ACTIEF.clear();
            BinnenInrichting.vergeetAlles();
        });
    }
}
