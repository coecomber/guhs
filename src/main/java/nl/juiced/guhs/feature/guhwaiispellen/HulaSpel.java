package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The hula dance at the surf beach of Guhwai'i (3.0): Lilo-guh on the hula podium (plek "hula") plays one of three
 * original songs of a fifties guh crooner (the song is the level, {@link HulaLiedje}) and you dance her steps on the beat:
 * hips left and right (A / D), arms up (W), down the knees (S) and VAHOEG! (space) on the shouts. The steps come sliding
 * in on your screen; your game judges each key press against the song's beat ({@link HulaKaart#oordeel}) and tells the
 * server which step it was and how many ms off; the server checks it (the step exists, it's that move, the time is
 * plausible) and counts: VAHOEG! / Njeg! / Guh. / Mis, a combo that multiplies (up to x3). Your guhs dance along.
 * <p>
 * One dancer at a time per podium; the others watch. While dancing you can't walk (your keys are the dance) and can't
 * get hurt or hungry. Schelpjesmunten for the score (lastig +50 %), a record per level, the level's world top 3 floating
 * over the podium and the advancements (a flawless dance!).
 */
public final class HulaSpel {
    public static final int START = 10, STOP = 2;
    public static final String SPEL = "guhwaii_hula";
    /** How far from Lilo-guh the flower mat may be. */
    public static final int MAT_BEREIK = 12;
    /** A step not danced this long after its beat is a miss (the dancer's game says "mis" after the GUH window already). */
    public static final int MIS_NA_MS = 450;
    /** The welcome present of your first dance. */
    public static final int EERSTE_MUNTEN = 4;
    /** A danced step's time (the dancer's clock) may be this far from the server's (lag, a slow start of the song). */
    public static final int PLAUSIBEL = 3000;
    /** Who hears the song. */
    public static final double HOREN = 40;

    private static final Map<UUID, HulaSpel> SPELLEN = new HashMap<>();
    private static final Map<UUID, UUID> DANSERS = new ConcurrentHashMap<>();

    public enum Fase { IDLE, BEZIG }

    private Fase fase = Fase.IDLE;
    @Nullable
    private UUID danser;
    private HulaLiedje liedje = HulaLiedje.GUHLA_HULA_ROCK;
    private long start;
    @Nullable
    private BlockPos mat;
    private HulaKaart.Oordeel[] oordelen = new HulaKaart.Oordeel[0];
    private int score, combo, maxCombo, ticks, dansBeat = -1;
    private final int[] tellers = new int[HulaKaart.Oordeel.values().length];
    private final Set<UUID> luisteraars = new HashSet<>();

    public static HulaSpel of(GuhNpcEntity npc) {
        return SPELLEN.computeIfAbsent(npc.getUUID(), id -> new HulaSpel());
    }

    public static boolean danst(Player p) {
        return DANSERS.containsKey(p.getUUID());
    }

    public boolean bezig() {
        return fase == Fase.BEZIG;
    }

    @Nullable
    public UUID danser() {
        return danser;
    }

    public HulaLiedje liedje() {
        return liedje;
    }

    public int score() {
        return score;
    }

    public int combo() {
        return combo;
    }

    public int tel(HulaKaart.Oordeel o) {
        return tellers[o.ordinal()];
    }

    @Nullable
    public BlockPos mat() {
        return mat;
    }

    /** The song tick (ticks since beat 0) at this game time. */
    public long songTick(long gameTime) {
        return gameTime - start;
    }

    // --- Lilo-guh on the podium ----------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        HulaSpel spel = of(npc);
        boolean mijn = spel.bezig() && player.getUUID().equals(spel.danser);
        CompoundTag saved = GuhwaiiSpellenFeature.data(player);
        GuhQuests.say(player, npc, mijn ? "quest.guhs.guhwaiispellen.hula.bezig" : spel.bezig() ? "quest.guhs.guhwaiispellen.hula.druk"
                : saved.getBooleanOr("EersteHula", false) ? "quest.guhs.guhwaiispellen.hula.hallo" + (1 + player.getRandom().nextInt(4))
                : "quest.guhs.guhwaiispellen.hula.welkom");
        CompoundTag data = GuhwaiiSpellenFeature.scherm(player, "hula");
        data.putBoolean("Mine", mijn);
        data.putBoolean("Running", spel.bezig());
        ServerPlayer d = spel.danser == null ? null : player.level().getServer().getPlayerList().getPlayer(spel.danser);
        data.putString("Danser", d == null ? "?" : d.getGameProfile().name());
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new GuhwaiiSpellenPayloads.Open(npc.getId(), data));
    }

    public static void actie(GuhNpcEntity npc, ServerPlayer player, int actie) {
        if (player.distanceToSqr(npc) > 144) {
            return;
        }
        HulaSpel spel = of(npc);
        if (actie == STOP) {
            if (spel.bezig() && player.getUUID().equals(spel.danser)) {
                spel.klaar(npc, player);
            }
        } else if (actie >= START && actie < START + Niveau.values().length) {
            spel.start(npc, player, HulaLiedje.of(Niveau.of(actie - START)));
        }
    }

    /** Starts a dance on this song (if the podium is free): onto the flower mat, the song, the steps. */
    public boolean start(GuhNpcEntity npc, ServerPlayer player, HulaLiedje song) {
        ServerLevel level = (ServerLevel) npc.level();
        if (bezig()) {
            if (!player.getUUID().equals(danser)) {
                GuhQuests.say(player, npc, "quest.guhs.guhwaiispellen.hula.druk");
            }
            return false;
        }
        if (danst(player) || Minigames.refuse(player, npc, SPEL)) {
            return false;
        }
        mat = zoekMat(level, npc.blockPosition());
        if (mat == null) {
            GuhQuests.say(player, npc, "quest.guhs.guhwaiispellen.hula.geen_mat");
            return false;
        }
        liedje = song;
        danser = player.getUUID();
        oordelen = new HulaKaart.Oordeel[HulaKaart.van(song).size()];
        score = combo = maxCombo = 0;
        java.util.Arrays.fill(tellers, 0);
        dansBeat = -1;
        DANSERS.put(danser, npc.getUUID());
        fase = Fase.BEZIG;
        player.stopRiding();
        Vec3 naar = npc.position().subtract(Vec3.atBottomCenterOf(mat));
        float yaw = (float) Math.toDegrees(Math.atan2(-naar.x, naar.z));
        player.teleportTo(level, mat.getX() + 0.5, mat.getY() + 0.07, mat.getZ() + 0.5, yaw, 5);
        Minigames.startKeeping(player);
        start = level.getGameTime();
        muziek(level, npc, true);
        GuhwaiiSpellenFeature.titel(player, song.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.guhwaiispellen.hula.titel.sub", song.niveau.naam(), (int) Math.round(song.bpm)), 50);
        player.sendSystemMessage(Component.translatable("quest.guhs.guhwaiispellen.hula.uitleg." + song.niveau.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, npc.blockPosition(), GuhwaiiSpellenBlocks.ALOHA.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        return true;
    }

    /** The song for everybody around the podium, from beat 0 now; the dancer's game gets the steps (the others: Lilo dances). */
    private void muziek(ServerLevel level, GuhNpcEntity npc, boolean aan) {
        ServerPlayer d = danser == null ? null : level.getServer().getPlayerList().getPlayer(danser);
        if (aan) {
            luisteraars.clear();
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(npc.blockPosition()).inflate(HOREN))) {
                if (p.connection != null) {
                    p.connection.send(new ClientboundSoundPacket(GuhwaiiSpellenBlocks.lied(liedje), SoundSource.RECORDS, mat.getX() + 0.5,
                            mat.getY() + 1.5, mat.getZ() + 0.5, 1.0f, 1.0f, level.getRandom().nextLong()));
                }
                luisteraars.add(p.getUUID());
                CompoundTag t = new CompoundTag();
                t.putInt("Npc", npc.getId());
                t.putInt("Liedje", liedje.ordinal());
                t.putBoolean("Danser", p == d);
                t.putInt("Record", GuhwaiiSpellenFeature.data(p).getIntOr("Hula_" + liedje.niveau.id(), 0));
                nl.juiced.guhs.network.ModNetworking.sendTo(p, new GuhwaiiSpellenPayloads.HulaStart(t));
            }
        } else {
            for (UUID id : luisteraars) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                if (p != null && p.connection != null) {
                    p.connection.send(new ClientboundStopSoundPacket(Guhs.id(liedje.geluid()), SoundSource.RECORDS));
                    CompoundTag t = new CompoundTag();
                    t.putInt("Npc", npc.getId());
                    t.putBoolean("Einde", true);
                    nl.juiced.guhs.network.ModNetworking.sendTo(p, new GuhwaiiSpellenPayloads.HulaStand(t));
                }
            }
            luisteraars.clear();
        }
    }

    /** The flower mat nearest to Lilo-guh (you dance on it). */
    @Nullable
    public static BlockPos zoekMat(ServerLevel level, BlockPos bij) {
        BlockPos best = null;
        for (BlockPos p : BlockPos.betweenClosed(bij.offset(-MAT_BEREIK, -3, -MAT_BEREIK), bij.offset(MAT_BEREIK, 3, MAT_BEREIK))) {
            if (level.getBlockState(p).is(GuhwaiiSpellenBlocks.HULA_BLOEMENMAT.get()) && (best == null || p.distSqr(bij) < best.distSqr(bij))) {
                best = p.immutable();
            }
        }
        return best;
    }

    // --- dancing -------------------------------------------------------------------------------------------------------

    /** Every tick (from Lilo-guh's role). */
    public void tick(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        ticks++;
        if (ticks % 100 == 1) {
            toonScores(npc);
        }
        if (fase != Fase.BEZIG) {
            return;
        }
        ServerPlayer p = danser == null ? null : level.getServer().getPlayerList().getPlayer(danser);
        if (p == null || !p.isAlive() || p.level() != level || mat == null || !DANSERS.containsKey(danser)) {
            reset(level, npc);
            return;
        }
        if (p.position().distanceToSqr(Vec3.atBottomCenterOf(mat)) > 9) {
            p.sendSystemMessage(Component.translatable("quest.guhs.guhwaiispellen.hula.weg").withStyle(ChatFormatting.LIGHT_PURPLE));
            klaar(npc, p);
            return;
        }
        Minigames.keep(p);
        long songTick = level.getGameTime() - start;
        double ms = songTick * 50.0;
        List<HulaKaart.Noot> kaart = HulaKaart.van(liedje);
        for (HulaKaart.Noot n : kaart) {
            if (oordelen[n.index()] == null && liedje.ms(n.beat()) + MIS_NA_MS < ms) {
                oordeel(n, HulaKaart.Oordeel.MIS);
            }
        }
        int beat = (int) Math.floor(songTick / liedje.ticksPerBeat());
        if (beat != dansBeat && beat >= 0) {
            dansBeat = beat;
            opDeBeat(level, npc, p, beat);
        }
        if (ticks % 5 == 0) {
            stand(p, npc, false);
        }
        if (ms > liedje.ms(kaart.get(kaart.size() - 1).beat()) + 1500) {
            klaar(npc, p);
        }
    }

    /** On every beat: flowers over the mat, and the dancer's own guhs dance along (every other bar). */
    private void opDeBeat(ServerLevel level, GuhNpcEntity npc, ServerPlayer p, int beat) {
        if (beat % 2 == 0) {
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, mat.getX() + 0.5, mat.getY() + 3.2, mat.getZ() + 0.5, 3, 1.4, 0.2, 1.4, 0);
        }
        if (beat % 8 == 0) {
            for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(mat).inflate(14, 4, 14), Band::isBandGuh)) {
                if (p.getUUID().equals(guh.getOwnerUUID()) && guh.emotes.current() == null) {
                    guh.emotes.start(Emote.DANSEN, false, GuhEmotes.Source.SELF);
                }
            }
        }
    }

    /** The dancer's game: step `noot` danced `ms` after the song's start. */
    public void tik(GuhNpcEntity npc, ServerPlayer player, int noot, int ms, int pas) {
        if (fase != Fase.BEZIG || !player.getUUID().equals(danser)) {
            return;
        }
        List<HulaKaart.Noot> kaart = HulaKaart.van(liedje);
        if (noot < 0 || noot >= kaart.size() || oordelen[noot] != null) {
            return;
        }
        HulaKaart.Noot n = kaart.get(noot);
        if (n.pas().ordinal() != pas) {
            return;
        }
        double server = (npc.level().getGameTime() - start) * 50.0;
        if (Math.abs(server - ms) > PLAUSIBEL) {
            return;                                             // (not plausible: a very old or made-up press)
        }
        HulaKaart.Oordeel o = HulaKaart.oordeel(liedje, ms - liedje.ms(n.beat()));
        oordeel(n, o);
        if (o != HulaKaart.Oordeel.MIS && npc.level() instanceof ServerLevel level) {
            level.sendParticles(o == HulaKaart.Oordeel.VAHOEG ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.NOTE, player.getX(), player.getY() + 2.1,
                    player.getZ(), o == HulaKaart.Oordeel.VAHOEG ? 4 : 1, 0.4, 0.1, 0.4, 0.5);
        }
    }

    /** (Tests) the song started this many ticks earlier. */
    void verschuif(long ticks) {
        start -= ticks;
    }

    /** (Also for the tests.) */
    void oordeel(HulaKaart.Noot n, HulaKaart.Oordeel o) {
        oordelen[n.index()] = o;
        tellers[o.ordinal()]++;
        if (o == HulaKaart.Oordeel.MIS) {
            combo = 0;
        } else {
            combo++;
            maxCombo = Math.max(maxCombo, combo);
            score += HulaKaart.punten(o, combo);
        }
    }

    /** Were all steps danced (none missed)? */
    public boolean foutloos() {
        for (HulaKaart.Oordeel o : oordelen) {
            if (o == null || o == HulaKaart.Oordeel.MIS) {
                return false;
            }
        }
        return oordelen.length > 0;
    }

    private void stand(ServerPlayer p, GuhNpcEntity npc, boolean einde) {
        CompoundTag t = new CompoundTag();
        t.putInt("Npc", npc.getId());
        t.putInt("Score", score);
        t.putInt("Combo", combo);
        t.putInt("MaxCombo", maxCombo);
        for (HulaKaart.Oordeel o : HulaKaart.Oordeel.values()) {
            t.putInt(o.id(), tellers[o.ordinal()]);
        }
        t.putBoolean("Einde", einde);
        nl.juiced.guhs.network.ModNetworking.sendTo(p, new GuhwaiiSpellenPayloads.HulaStand(t));
    }

    /** The end of the song (or you stopped): schelpjesmunten, the record, the board, the advancements. */
    void klaar(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel level = (ServerLevel) npc.level();
        CompoundTag data = GuhwaiiSpellenFeature.data(p);
        Niveau niveau = liedje.niveau;
        int munten = niveau.munten(HulaKaart.munten(score));
        if (munten > 0) {
            Minigames.give(p, new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), munten));
            level.playSound(null, p.blockPosition(), GuhwaiiSpellenBlocks.SCHELPJE.get(), SoundSource.PLAYERS, 1f, 1f);
        }
        boolean foutloos = foutloos();
        p.sendSystemMessage(Component.translatable("quest.guhs.guhwaiispellen.hula.uitslag", score, liedje.naam(), munten,
                tel(HulaKaart.Oordeel.VAHOEG), tel(HulaKaart.Oordeel.NJEG), tel(HulaKaart.Oordeel.GUH), tel(HulaKaart.Oordeel.MIS), maxCombo)
                .withStyle(ChatFormatting.GOLD));
        int oud = data.getIntOr("Hula_" + niveau.id(), 0);
        if (score > oud) {
            data.putInt("Hula_" + niveau.id(), score);
            p.sendSystemMessage((oud > 0 ? Component.translatable("quest.guhs.guhwaiispellen.record", score, oud)
                    : Component.translatable("quest.guhs.guhwaiispellen.record_eerste", score)).withStyle(ChatFormatting.YELLOW));
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        if (score > 0) {
            if (Scorebord.submit(p, liedje.board(), score, false) > 0) {
                toonScores(npc);
            }
            GuhwaiiSpellenFeature.gespeeld(p, liedje.board());
            GuhAdvancements.grant(p, "guhwaii_spellen_hula_" + niveau.id());
            GuhwaiiSpellenFeature.grant(p, "guhwaii_spellen_hula");
        }
        if (foutloos) {
            GuhQuests.say(p, npc, "quest.guhs.guhwaiispellen.hula.foutloos");
            if (niveau != Niveau.MAKKELIJK) {
                GuhwaiiSpellenFeature.grant(p, "guhwaii_spellen_hula_foutloos");
            }
        } else if (score > 0) {
            GuhQuests.say(p, npc, "quest.guhs.guhwaiispellen.hula.klaar" + (1 + level.getRandom().nextInt(3)));
        }
        if (!data.getBooleanOr("EersteHula", false) && score > 0) {
            data.putBoolean("EersteHula", true);
            Minigames.give(p, new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), EERSTE_MUNTEN));
            Minigames.give(p, new ItemStack(GuhwaiiSpellenBlocks.HULA_BLOEMENMAT.get(), 2));
            GuhQuests.say(p, npc, "quest.guhs.guhwaiispellen.hula.eerste");
        }
        stand(p, npc, true);
        reset(level, npc);
    }

    void reset(ServerLevel level, GuhNpcEntity npc) {
        boolean was = fase == Fase.BEZIG;
        if (danser != null) {
            DANSERS.remove(danser);
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(danser);
            if (p != null) {
                Minigames.forget(p);
            }
        }
        if (was) {
            muziek(level, npc, false);
        }
        danser = null;
        fase = Fase.IDLE;
    }

    /** The world top 3 of the three songs, floating over the podium. */
    public static void toonScores(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<String> boards = new ArrayList<>();
        List<Component> heads = new ArrayList<>();
        for (HulaLiedje l : HulaLiedje.values()) {
            boards.add(l.board());
            heads.add(Component.translatable("gui.guhs.scorebord.guhwaiispellen.lied", l.naam(), l.niveau.naam()));
        }
        Scorebord.show(level, npc.position().add(0, 3.6, 0), "hula", Scorebord.text(level.getServer(),
                Component.translatable("gui.guhs.scorebord.guhwaiispellen.hula"), boards, heads, n -> n + " ✿"));
    }

    // --- events --------------------------------------------------------------------------------------------------------

    static void opUitloggen(ServerPlayer p) {
        DANSERS.remove(p.getUUID());
    }

    static void vergeetAlles() {
        SPELLEN.clear();
        DANSERS.clear();
    }

    /** (Tests) the game this player dances in. */
    @Nullable
    public static UUID npcVan(Player p) {
        return DANSERS.get(p.getUUID());
    }
}
