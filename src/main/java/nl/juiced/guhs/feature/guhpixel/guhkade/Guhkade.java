package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The Guhkade on the server: who stands at which cabinet, and what a finished game means.
 * <p>
 * A player's game: right-click ({@link #open}) hands the client the cabinet's top 5 and a fresh seed; the client says
 * when the game starts ({@link #start}) and, when it is over, sends its input of every step ({@link #klaar}). The server
 * plays that input again ({@link Sim#speelAf}): the score is the server's own, a client never reports one. Then
 * {@link #verwerk}: the cabinet's list, the player's own bests, and the guhs whose score was just beaten (they look sad
 * and practise more: {@link #verdrietig}).
 * <p>
 * Per player (PxData slice "guhkade"): {@code Best_<spel>}, {@code Potjes_<spel>}, {@code Verslagen} (guhs beaten).
 */
public final class Guhkade {
    public static final String DEEL = "guhkade";
    /** How close you must stand to play. */
    public static final double BEREIK = 6.0;
    /** A player keeps the buttons this long after the last sign of life (a game lasts at most ten minutes). */
    private static final int VASTHOUDEN = 20 * 60 * 11;
    /** FTB milestones (hidden advancements). */
    public static final int FLAPPY_MIJLPAAL = 10, PONG_MIJLPAAL = 30;

    /** A player at a cabinet. */
    private static final class Beurt {
        final ResourceKey<Level> dim;
        final BlockPos pos;
        long seed;
        boolean bezig;
        long begin;

        Beurt(ResourceKey<Level> dim, BlockPos pos, long seed) {
            this.dim = dim;
            this.pos = pos;
            this.seed = seed;
        }
    }

    private static final Map<UUID, Beurt> BEURTEN = new ConcurrentHashMap<>();

    /** What a finished game did. */
    public record Uitslag(int score, int plaats, boolean record, List<Component> verslagen) {
    }

    // =====================================================================================================================
    // the loaded cabinets (so a guh finds one nearby without scanning blocks)
    // =====================================================================================================================

    public static final class Kasten {
        private static final Map<ResourceKey<Level>, Set<BlockPos>> ALLE = new ConcurrentHashMap<>();

        static void erbij(Level level, BlockPos pos) {
            ALLE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }

        static void weg(Level level, BlockPos pos) {
            Set<BlockPos> s = ALLE.get(level.dimension());
            if (s != null) {
                s.remove(pos);
            }
        }

        static void vergeet() {
            ALLE.clear();
        }

        /** The loaded cabinets within bereik blocks of rond, the nearest first. */
        public static List<KastBlockEntity> rond(ServerLevel level, BlockPos rond, int bereik) {
            List<KastBlockEntity> uit = new ArrayList<>();
            Set<BlockPos> s = ALLE.get(level.dimension());
            if (s == null) {
                return uit;
            }
            for (BlockPos pos : s) {
                if (pos.distSqr(rond) <= (double) bereik * bereik && level.isLoaded(pos)) {
                    KastBlockEntity be = kast(level, pos);
                    if (be != null) {
                        uit.add(be);
                    }
                }
            }
            uit.sort(java.util.Comparator.comparingDouble(be -> be.getBlockPos().distSqr(rond)));
            return uit;
        }

        private Kasten() {
        }
    }

    /** The cabinet whose lower half is at pos (or null). */
    @Nullable
    public static KastBlockEntity kast(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof KastBlockEntity be && !be.isBoven() && be.getBlockState().getBlock() instanceof KastBlock ? be : null;
    }

    // =====================================================================================================================
    // a player's game
    // =====================================================================================================================

    /** Right-click on a cabinet: the game screen, unless somebody else is at the buttons. */
    public static void open(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        KastBlockEntity be = kast(level, pos);
        if (be == null) {
            return;
        }
        if (!be.vrijVoor(p.getUUID())) {
            p.sendOverlayMessage(Component.translatable(be.modus() == KastBlockEntity.SPELER ? "gui.guhs.guhkade.bezet.speler" : "gui.guhs.guhkade.bezet.guh",
                    be.aanZet()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Beurt oud = BEURTEN.get(p.getUUID());
        if (oud != null && !(oud.pos.equals(pos) && oud.dim == level.dimension())) {
            laatLos(p.level().getServer(), p.getUUID(), oud);
        }
        Beurt b = new Beurt(level.dimension(), pos.immutable(), level.getRandom().nextLong());
        BEURTEN.put(p.getUUID(), b);
        be.neem(p.getUUID(), p.getName(), KastBlockEntity.SPELER, level.getGameTime() + VASTHOUDEN);
        ModNetworking.sendTo(p, new GuhkadePayloads.Open(pos, be.spel().id, b.seed, be.schermTag(), best(p, be.spel())));
    }

    @Nullable
    private static Beurt beurt(ServerPlayer p, BlockPos pos) {
        Beurt b = BEURTEN.get(p.getUUID());
        return b != null && b.pos.equals(pos) && b.dim == p.level().dimension() ? b : null;
    }

    /** The seed of the game this player may play now (tests; a real client gets it in the Open / Uitslag payload). */
    static long seedVan(ServerPlayer p) {
        Beurt b = BEURTEN.get(p.getUUID());
        return b == null ? 0L : b.seed;
    }

    private static boolean dichtbij(ServerPlayer p, BlockPos pos) {
        return p.isAlive() && p.distanceToSqr(Vec3.atCenterOf(pos)) <= (BEREIK + 2) * (BEREIK + 2);
    }

    /** The client started the game it was given the seed for. */
    public static boolean start(ServerPlayer p, BlockPos pos, long seed) {
        Beurt b = beurt(p, pos);
        KastBlockEntity be = kast(p.level(), pos);
        if (b == null || be == null || b.seed != seed || !dichtbij(p, pos) || !be.vrijVoor(p.getUUID())) {
            return false;
        }
        b.bezig = true;
        b.begin = p.level().getGameTime();
        be.neem(p.getUUID(), p.getName(), KastBlockEntity.SPELER, b.begin + VASTHOUDEN);
        p.level().playSound(null, pos, GuhkadeSlice.MUNTJE.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        return true;
    }

    /**
     * The client's game is over: its input of every step. The server plays it again and that is the score. Returns null
     * (and nothing counts) when this was no game the server handed out: another seed, not started, too far away, or more
     * steps than there was time for.
     */
    @Nullable
    public static Uitslag klaar(ServerPlayer p, BlockPos pos, long seed, int stappen, byte[] invoer) {
        Beurt b = beurt(p, pos);
        KastBlockEntity be = kast(p.level(), pos);
        if (b == null || be == null || !b.bezig || b.seed != seed || !dichtbij(p, pos)) {
            return null;
        }
        b.bezig = false;
        long verstreken = p.level().getGameTime() - b.begin;
        b.seed = p.level().getRandom().nextLong();                 // (a seed is good for one game)
        if (stappen < 0 || stappen > Sim.MAX_STAPPEN || stappen > verstreken * Sim.PER_TICK * 3 + 300) {
            stuurUitslag(p, be, b, new Uitslag(0, 0, false, List.of()));
            return null;
        }
        Sim.Uitkomst u = Sim.speelAf(be.spel(), seed, invoer, stappen);
        Uitslag uit = verwerk(p, be, u.score());
        stuurUitslag(p, be, b, uit);
        return uit;
    }

    private static void stuurUitslag(ServerPlayer p, KastBlockEntity be, Beurt b, Uitslag u) {
        ModNetworking.sendTo(p, new GuhkadePayloads.Uitslag(be.getBlockPos(), u.score(), u.plaats(), u.record(), u.verslagen().size(), b.seed, be.schermTag(),
                best(p, be.spel())));
    }

    /** The player walked away from the cabinet (the screen closed). */
    public static void stop(ServerPlayer p, BlockPos pos) {
        Beurt b = beurt(p, pos);
        if (b != null) {
            BEURTEN.remove(p.getUUID());
            laatLos(p.level().getServer(), p.getUUID(), b);
        }
    }

    private static void laatLos(MinecraftServer server, UUID wie, Beurt b) {
        ServerLevel level = server.getLevel(b.dim);
        if (level != null && level.isLoaded(b.pos)) {
            KastBlockEntity be = kast(level, b.pos);
            if (be != null) {
                be.laatLos(wie);
            }
        }
    }

    /** Once a second: players who left, went far away or whose cabinet is gone let go of the buttons. */
    static void tick(MinecraftServer server) {
        if (BEURTEN.isEmpty() || server.getTickCount() % 20 != 0) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Beurt>> it = BEURTEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Beurt> e = it.next();
            Beurt b = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            boolean weg = p == null || p.isRemoved() || p.level().dimension() != b.dim || !dichtbij(p, b.pos) || kast(p.level(), b.pos) == null;
            if (weg) {
                it.remove();
                laatLos(server, e.getKey(), b);
            }
        }
    }

    static void vergeet() {
        BEURTEN.clear();
        Kasten.vergeet();
    }

    // =====================================================================================================================
    // what a score means
    // =====================================================================================================================

    public static CompoundTag data(ServerPlayer p) {
        return PxData.deel(p, DEEL);
    }

    /** The player's best ever at this game (on any cabinet). */
    public static int best(ServerPlayer p, Spel spel) {
        return data(p).getIntOr("Best_" + spel.id, 0);
    }

    public static int potjes(ServerPlayer p, Spel spel) {
        return data(p).getIntOr("Potjes_" + spel.id, 0);
    }

    /** How many times this player beat a guh's score. */
    public static int verslagen(ServerPlayer p) {
        return data(p).getIntOr("Verslagen", 0);
    }

    /**
     * A player's finished game with this (server-made) score: the player's own numbers, the cabinet's list, the hidden
     * advancements, and every guh on the list that had a better score than the player until now and is beaten by this one.
     */
    public static Uitslag verwerk(ServerPlayer p, KastBlockEntity be, int score) {
        Spel spel = be.spel();
        ServerLevel level = p.level();
        CompoundTag data = data(p);
        data.putInt("Potjes_" + spel.id, data.getIntOr("Potjes_" + spel.id, 0) + 1);
        if (score > data.getIntOr("Best_" + spel.id, 0)) {
            data.putInt("Best_" + spel.id, score);
        }
        int vorig = be.scoreVan(p.getUUID());
        List<Component> verslagen = new ArrayList<>();
        List<UUID> sip = new ArrayList<>();
        if (score > vorig) {
            for (KastBlockEntity.Regel r : be.regels()) {
                if (r.guh() && r.score() < score && r.score() >= vorig) {
                    verslagen.add(r.naam());
                    sip.add(r.id());
                }
            }
        }
        int plaats = be.voegToe(p.getUUID(), p.getName(), score, false);
        if (!verslagen.isEmpty()) {
            data.putInt("Verslagen", data.getIntOr("Verslagen", 0) + verslagen.size());
            GuhAdvancements.grant(p, "guhkade_guh_verslagen");
        }
        PxData.vuil(level.getServer());
        if (score > 0) {
            GuhAdvancements.grant(p, "guhkade_gespeeld");
        }
        if (spel == Spel.FLAPPY && score >= FLAPPY_MIJLPAAL) {
            GuhAdvancements.grant(p, "guhkade_flappy_mijlpaal");
        }
        if (spel == Spel.PONG && score >= PONG_MIJLPAAL) {
            GuhAdvancements.grant(p, "guhkade_pong_mijlpaal");
        }
        for (int i = 0; i < sip.size(); i++) {
            UUID id = sip.get(i);
            Entity e = level.getEntity(id);
            boolean erbij = e instanceof GuhEntity guh && guh.isAlive() && guh.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) < 24 * 24;
            if (erbij) {
                verdrietig((GuhEntity) e, p);
            } else {
                be.zetGeklopt(id, true);        // (it finds out the next time it walks by)
            }
            p.sendSystemMessage(Component.translatable(erbij ? "gui.guhs.guhkade.verslagen" : "gui.guhs.guhkade.verslagen.weg", verslagen.get(i))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (plaats == 1) {
            level.playSound(null, be.getBlockPos(), GuhkadeSlice.RECORD.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return new Uitslag(score, plaats, plaats > 0, verslagen);
    }

    /**
     * A guh found out that a player beat its score: it looks sad for a moment (the Verdrietje emote, a tear or two) and
     * wants a few practice games, soon. Nothing else happens to it: it is only a game, njeg.
     */
    public static void verdrietig(GuhEntity guh, @Nullable ServerPlayer door) {
        if (!(guh.level() instanceof ServerLevel level) || guh.getType() != ModEntities.GUH.get()) {
            return;
        }
        var data = guh.getPersistentData();
        data.putInt(GuhKunde.OEFEN, Math.min(GuhKunde.OEFEN_MAX, data.getIntOr(GuhKunde.OEFEN, 0) + GuhKunde.OEFEN_NA_VERLIES));
        data.putLong(GuhKunde.RUST, level.getGameTime() + 100);
        boolean emote = guh.emotes.start(Emote.VERDRIETJE, false, GuhEmotes.Source.SELF);
        if (emote && door != null) {
            guh.emotes.setLookTarget(door.getUUID());
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, guh.getX(), guh.getY() + guh.getBbHeight() * 0.8, guh.getZ(), 4, 0.15, 0.05, 0.15, 0);
        if (guh.areSoundsEnabled()) {
            level.playSound(null, guh.blockPosition(), GuhkadeSlice.SIP.get(), SoundSource.NEUTRAL, 0.8f, guh.getVoicePitch());
        }
    }

    private Guhkade() {
    }
}
