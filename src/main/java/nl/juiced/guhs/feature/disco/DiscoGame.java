package nl.juiced.guhs.feature.disco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Guhdisco: Simon says on the light-up dance floor, to the beat of a real song (2.9). You pick a song at the DJ-guh
 * and the song is the level ({@link DiscoLiedje}: Vadsige Tango = makkelijk, the 70's remix of "Ze hangen aan me vet" =
 * medium, Mika-Mambo = lastig, Njeg-Njeg Boogie = bonus). The DJ plays a row of colours exactly on the beat of the song
 * (the tiles flash and each colour has its own note); the dancer steps on the tiles in the same order. Every round the
 * row grows by one; the tempo never changes (it's the song's), later rows come on "double counts" (two colours per
 * beat). One wrong step (or waiting too long) ends the game. The score is the longest row danced right: discomunten
 * for it (lastig +50 %), a personal record per song that survives death, a place on the song's world top 3 (the
 * floating {@link Scorebord} above the stage) and a welcome present the first time.
 * <p>
 * One dancer at a time per disco (others watch the actionbar and wait). While dancing you can't get hurt or hungry. The
 * game needs no items at all. Outside a game the tiles light up and sing when you walk over them, and the DJ plays
 * the remix ({@link DiscoMuziek}); the disco ball sparkles on its beat. The state lives in memory (a game only takes a
 * few minutes): after a restart the floor resets.
 */
public final class DiscoGame {
    public enum Phase { IDLE, COUNTDOWN, SHOW, INPUT, PAUSE, OVER }

    /** Actions from the screen: START = the standard (medium) song, START_LIED + ordinal = that song. */
    public static final int START = 0, SHOP = 1, STOP = 2, START_LIED = 10;
    /** How far from the DJ-guh the dance floor may be. */
    public static final int REACH = 32, BELOW = 8, ABOVE = 4;
    static final int OVER_TICKS = 40, FLASH_TICKS = 6;
    /** The countdown: "3, 2, 1" on beats 4, 5 and 6 of the song, "DANS!" on 7, the first colour on beat 8. */
    public static final int FIRST_BEAT = 8;
    private static final DiscoTileBlock.Kleur[] KLEUREN = DiscoTileBlock.Kleur.values();
    private static final String BEST = "guhs_disco_best", FIRST = "guhs_disco_first", LIEDJES = "guhs_disco_liedjes";
    /** The world's top 3 of the standard song (the most colours in a row); the other songs add _makkelijk/_lastig/_boogie. */
    public static final String BOARD = "disco_kleuren";
    /** The welcome present of your first dance. */
    public static final int FIRST_COINS = 4;
    /** A song counts as danced (for "all songs") from this many colours. */
    public static final int GEDANST = 3;
    /** Where the scoreboard floats: above the stage, in front of the DJ-guh (seen from the whole dance floor). */
    public static final Vec3 SCOREBORD_OFFSET = new Vec3(0, 5.0, 3.1);

    private static final Map<UUID, DiscoGame> GAMES = new HashMap<>();
    /** Everyone dancing right now, with the game time their game last saw them: no hunger, no damage. */
    private static final Map<UUID, Long> DANCERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The game of this DJ-guh. */
    public static DiscoGame of(GuhNpcEntity npc) {
        return GAMES.computeIfAbsent(npc.getUUID(), id -> new DiscoGame());
    }

    public static boolean isDancing(Player player) {
        return DANCERS.containsKey(player.getUUID());
    }

    // --- the floor, found once by scanning around the DJ-guh --------------------------------------------------------
    private final List<List<BlockPos>> pads = new ArrayList<>();
    private final List<BlockPos> balls = new ArrayList<>();
    /** Where the dancer starts: on the middle of the floor. */
    @Nullable
    private BlockPos centre;
    private double floorRadius;
    private long lastScan = Long.MIN_VALUE / 2;

    // --- the game ---------------------------------------------------------------------------------------------------
    private Phase phase = Phase.IDLE;
    private DiscoLiedje liedje = DiscoLiedje.DISCO70;
    @Nullable
    private UUID dancer;
    private final List<Integer> sequence = new ArrayList<>();
    private int shown, step, timer, score, lastPad = -1;
    /** Ticks since the song (and the game) started: the beat is songTicks / ticksPerBeat, never rounded along the way. */
    private int songTicks;
    /** How often the song was put on again, and the world time of its latest start (for the listeners). */
    private int loops;
    private long songStart;
    /** The beat the last beat event was for (countdown, sparkles). */
    private int lastBeat = -1;
    /** Where on the beat grid the DJ plays the current row (first colour), one colour per stap beats. */
    private double showStart, stap = 1;
    /** When the next round starts (a beat), during the little party after a round. */
    private double nextShow;
    /** When each colour of the current row was really played (song ticks; for the tests: exactly on the beat). */
    private final List<Integer> playedAt = new ArrayList<>();
    private final int[] flash = new int[KLEUREN.length];
    /** Ticks of this DJ-guh (for everything that happens every so many ticks). */
    private int ticks;
    /** Outside a game: the tile colour each player near the floor stands on (the tiles sing when you step on them). */
    private final Map<UUID, Integer> freePads = new HashMap<>();
    private final DiscoMuziek muziek = new DiscoMuziek();
    private int idleBeat = -1;

    private DiscoGame() {
        for (int i = 0; i < KLEUREN.length; i++) {
            pads.add(new ArrayList<>());
        }
    }

    public Phase phase() {
        return phase;
    }

    public boolean isRunning() {
        return phase != Phase.IDLE;
    }

    public DiscoLiedje liedje() {
        return liedje;
    }

    @Nullable
    public UUID dancer() {
        return dancer;
    }

    public List<Integer> sequence() {
        return List.copyOf(sequence);
    }

    public int score() {
        return score;
    }

    @Nullable
    public BlockPos centre() {
        return centre;
    }

    public int tiles(DiscoTileBlock.Kleur kleur) {
        return pads.get(kleur.ordinal()).size();
    }

    public DiscoMuziek muziek() {
        return muziek;
    }

    /** (For tests) the tiles of colour c. */
    List<BlockPos> tilesOf(int c) {
        return List.copyOf(pads.get(c));
    }

    /** (For tests) the song tick each colour of the current row was played at, and the row's beat grid. */
    List<Integer> playedAt() {
        return List.copyOf(playedAt);
    }

    double showStart() {
        return showStart;
    }

    double stap() {
        return stap;
    }

    int songTicks() {
        return songTicks;
    }

    // --- talking to the DJ-guh ----------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        DiscoGame game = of(npc);
        ServerPlayer other = game.isRunning() ? game.dancer((ServerLevel) npc.level()) : null;
        boolean mine = game.isRunning() && player.getUUID().equals(game.dancer);
        GuhQuests.say(player, npc, mine ? "quest.guhs.disco.dancing" : game.isRunning() ? "quest.guhs.disco.busy" : "quest.guhs.disco.hello");
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        data.putBoolean("Mine", mine);
        data.putString("Dancer", other == null ? "?" : other.getGameProfile().getName());
        data.putInt("Round", game.sequence.size());
        data.putInt("Liedje", game.liedje.ordinal());
        data.putInt("Best", best(player));
        for (DiscoLiedje l : DiscoLiedje.values()) {
            data.putInt("Best_" + l.id, best(player, l));
        }
        data.putBoolean("Played", GuhQuests.saved(player).getBoolean(FIRST));
        data.putInt("Munten", GuhQuests.count(player, DiscoBlocks.DISCOMUNT.get()));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new DiscoPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.DJGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        DiscoGame game = of(npc);
        if (action == SHOP) {
            npc.openShop(player);
        } else if (action == STOP) {
            if (game.isRunning() && player.getUUID().equals(game.dancer)) {
                game.finish(npc, (ServerLevel) npc.level(), player);
            }
        } else if (action == START) {
            game.start(npc, player, DiscoLiedje.DISCO70);
        } else if (action >= START_LIED && action < START_LIED + DiscoLiedje.values().length) {
            game.start(npc, player, DiscoLiedje.of(action - START_LIED));
        }
    }

    // --- playing -----------------------------------------------------------------------------------------------------

    /** Starts a game on the standard song. */
    public boolean start(GuhNpcEntity npc, ServerPlayer player) {
        return start(npc, player, DiscoLiedje.DISCO70);
    }

    /** Starts a game on this song (if the floor is free): onto the middle of the floor, the song starts, the countdown. */
    public boolean start(GuhNpcEntity npc, ServerPlayer player, DiscoLiedje song) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isRunning()) {
            if (!player.getUUID().equals(dancer)) {
                GuhQuests.say(player, npc, "quest.guhs.disco.busy");
            }
            return false;
        }
        if (isDancing(player)) {                                 // (already dancing in another disco)
            return false;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(player, npc, nl.juiced.guhs.feature.Minigames.DISCO)) {
            return false;
        }
        scan(npc, world);
        if (centre == null) {
            GuhQuests.say(player, npc, "quest.guhs.disco.broken");
            return false;
        }
        liedje = song;
        dancer = player.getUUID();
        sequence.clear();
        playedAt.clear();
        score = 0;
        lastPad = -1;
        phase = Phase.COUNTDOWN;
        songTicks = 0;
        loops = 0;
        lastBeat = -1;
        stap = 1;
        freePads.clear();
        allOff(world);
        DANCERS.put(dancer, world.getGameTime());
        nl.juiced.guhs.feature.Minigames.startKeeping(player);
        Vec3 to = npc.position().subtract(Vec3.atBottomCenterOf(centre));
        float yaw = (float) (Math.toDegrees(Math.atan2(-to.x, to.z)));
        player.stopRiding();                                     // (off the guh or boat first: you dance on your own feet)
        player.teleportTo(world, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, yaw, 0);
        songStart = world.getGameTime();
        muziek.start(world, centre, liedje);                     // the song starts now: beat 0
        player.sendSystemMessage(Component.translatable("quest.guhs.disco.go", liedje.naam(), liedje.niveauNaam())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        title(player, liedje.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("quest.guhs.disco.title.ready.sub", liedje.niveauNaam(), (int) Math.round(liedje.bpm)), 40);
        world.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        return true;
    }

    /** Where we are in the song, in beats (fractional). */
    public double beat() {
        return songTicks / liedje.ticksPerBeat();
    }

    /** Every tick (from the DJ-guh). */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        ticks++;
        if (ticks % 100 == 1) {
            showScores(npc);
        }
        if (phase == Phase.IDLE) {
            idle(npc, world);
            return;
        }
        ServerPlayer p = dancer(world);
        if (p == null || !p.isAlive() || p.level() != world || centre == null || !DANCERS.containsKey(dancer)) {
            reset(world);                                        // logged out (also: came back later), died, left the dimension
            return;
        }
        DANCERS.put(dancer, world.getGameTime());
        if (!onFloor(p)) {
            p.sendSystemMessage(Component.translatable("quest.guhs.disco.left").withStyle(ChatFormatting.LIGHT_PURPLE));
            finish(npc, world, p);
            return;
        }
        songTicks++;
        double tpb = liedje.ticksPerBeat();
        if (songTicks >= (loops + 1) * liedje.loopBeats * tpb) { // the song is through: the DJ puts it on again (same grid)
            loops++;
            songStart = world.getGameTime();
            muziek.start(world, centre, liedje);
        }
        if (ticks % 5 == 0) {
            muziek.tick(world, centre, liedje, songStart);
        }
        if (ticks % 20 == 0) {
            refresh(p);
        }
        tickFlashes(world);
        double pos = beat();
        int b = (int) Math.floor(pos);
        if (b != lastBeat) {                                     // on every beat: the disco ball flashes along
            lastBeat = b;
            onBeat(npc, world, p, b);
        }
        switch (phase) {
            case SHOW -> {
                double near = (songTicks + 0.5) / tpb;           // (the beat on the NEAREST tick: at most 25 ms off)
                if (shown < sequence.size() && near >= showStart + shown * stap) {
                    int c = sequence.get(shown++);
                    playedAt.add(songTicks);
                    play(world, c, onTicks(liedje, stap), 1.4f);
                } else if (shown >= sequence.size() && near >= showStart + sequence.size() * stap) {
                    phase = Phase.INPUT;
                    step = 0;
                    timer = stepTimeout(liedje);
                    lastPad = padUnder(world, p);
                    p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.jouw_beurt", 0, sequence.size())
                            .withStyle(ChatFormatting.GREEN), true);
                    if (lastPad >= 0 && lastPad == sequence.get(0)) {   // already standing on the right one: that counts
                        press(npc, world, p, lastPad);
                    }
                }
            }
            case INPUT -> {
                int pad = p.onGround() ? padUnder(world, p) : lastPad;  // (jumping doesn't count as stepping off)
                if (pad != lastPad) {
                    lastPad = pad;
                    if (pad >= 0) {
                        press(npc, world, p, pad);
                        return;
                    }
                }
                if (--timer <= 0) {
                    mistake(world, p, -1);
                }
            }
            case PAUSE -> {
                if (pos >= nextShow - 1) {
                    nextRound(world, p, nextShow);
                }
            }
            case OVER -> {
                if (--timer <= 0) {
                    finish(npc, world, p);
                }
            }
            default -> {
            }
        }
        if (phase != Phase.IDLE && ticks % 10 == 0) {
            bar(world, p);
        }
    }

    /** A new beat of the song during a game: the countdown, and the disco ball sparkles along. */
    private void onBeat(GuhNpcEntity npc, ServerLevel world, ServerPlayer p, int b) {
        beatSparkle(world, b);
        if (phase != Phase.COUNTDOWN) {
            return;
        }
        if (b >= FIRST_BEAT - 4 && b < FIRST_BEAT - 1) {
            int n = FIRST_BEAT - 1 - b;                          // 3, 2, 1
            title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 8);
            world.playSound(null, centre, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS, 0.8f, 0.8f + (3 - n) * 0.15f);
        } else if (b == FIRST_BEAT - 1) {
            title(p, Component.translatable("quest.guhs.disco.title.dans").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.disco.title.dans.sub"), 16);
            world.playSound(null, centre, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS, 0.8f, 1.6f);
            nextRound(world, p, FIRST_BEAT);
        }
    }

    /** One more colour (the first round: the song's start length), played from beat 'start' on. */
    private void nextRound(ServerLevel world, ServerPlayer p, double start) {
        int add = sequence.isEmpty() ? liedje.startLengte : 1;
        for (int k = 0; k < add; k++) {
            int last = sequence.isEmpty() ? -1 : sequence.get(sequence.size() - 1);
            int c;
            do {
                c = world.getRandom().nextInt(KLEUREN.length);
            } while (c == last);                    // never twice the same in a row: you'd have to step off and on again
            sequence.add(c);
        }
        boolean wasDubbel = stap < 1;
        stap = liedje.dubbel(sequence.size()) ? 0.5 : 1;
        showStart = start;
        shown = 0;
        playedAt.clear();
        phase = Phase.SHOW;
        if (stap < 1 && !wasDubbel) {
            title(p, Component.translatable("quest.guhs.disco.title.dubbel").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.disco.title.dubbel.sub"), 20);
        }
        p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.kijk", sequence.size()).withStyle(ChatFormatting.AQUA), true);
    }

    /** The dancer stepped on a tile of colour c. */
    void press(GuhNpcEntity npc, ServerLevel world, ServerPlayer p, int c) {
        if (phase != Phase.INPUT) {
            return;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "disco", c == sequence.get(step)); // samen
        if (c != sequence.get(step)) {
            mistake(world, p, sequence.get(step));
            return;
        }
        play(world, c, FLASH_TICKS, 1.0f);
        step++;
        timer = stepTimeout(liedje);
        if (step < sequence.size()) {
            p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.jouw_beurt", step, sequence.size()).withStyle(ChatFormatting.GREEN), true);
            return;
        }
        // the whole row right: a little party, then the next round on the next half bar (at least two beats later)
        score = sequence.size();
        phase = Phase.PAUSE;
        nextShow = Math.ceil(beat() + 2);
        if (((long) nextShow) % 2 != 0) {
            nextShow++;
        }
        for (int k = 0; k < KLEUREN.length; k++) {
            light(world, k, 10);
        }
        world.playSound(null, centre, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 0.8f, 1.2f);
        world.playSound(null, p.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        world.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 8, 0.8, 0.3, 0.8, 1);
        if (score % 5 == 0) {
            title(p, Component.translatable("quest.guhs.disco.title.vahoeg").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.disco.title.vahoeg.sub", score), 25);
        }
        p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.goed", score).withStyle(ChatFormatting.GOLD), true);
    }

    /** A wrong step (or too late, expected -1 = time's up): the right tile lights up, and it's over. */
    private void mistake(ServerLevel world, ServerPlayer p, int expected) {
        int right = expected >= 0 ? expected : sequence.get(step);
        phase = Phase.OVER;
        timer = OVER_TICKS;
        light(world, right, OVER_TICKS - 5);
        world.playSound(null, centre, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.RECORDS, 1.2f, 0.5f);
        world.playSound(null, p.blockPosition(), ModSounds.GUH_HURT.get(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        world.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.2, p.getZ(), 20, 0.4, 0.1, 0.4, 0.02);
        Component colour = Component.translatable("quest.guhs.disco.kleur." + KLEUREN[right].id());
        title(p, Component.translatable("quest.guhs.disco.title.njeg").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                expected >= 0 ? Component.translatable("quest.guhs.disco.title.fout", colour) : Component.translatable("quest.guhs.disco.title.te_laat", colour), 30);
    }

    /** The end of a game: discomunten, the record, the first-time present and the advancements. */
    private void finish(GuhNpcEntity npc, ServerLevel world, ServerPlayer p) {
        int coins = liedje.munten(score);
        CompoundTag saved = GuhQuests.saved(p);
        if (coins > 0) {
            give(p, new ItemStack(DiscoBlocks.DISCOMUNT.get(), coins));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.disco.done", score, coins).withStyle(ChatFormatting.GOLD));
        int best = best(p, liedje);
        if (score > best) {                                   // your personal best of this song, always in chat
            saved.putInt(bestKey(liedje), score);
            p.sendSystemMessage(best > 0 ? Component.translatable("quest.guhs.disco.record", score, best, liedje.naam()).withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("quest.guhs.disco.record_first", score, liedje.naam()).withStyle(ChatFormatting.YELLOW));
            world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        } else if (best > 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.disco.best", best, liedje.naam()).withStyle(ChatFormatting.GRAY));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.disco.no_best").withStyle(ChatFormatting.GRAY));
        }
        if (score > 0 && Scorebord.submit(p, liedje.board(), score, false) > 0) {   // the song's world top 3
            showScores(npc);
        }
        if (!saved.getBoolean(FIRST)) {                       // the first game ever: a welcome present
            saved.putBoolean(FIRST, true);
            give(p, new ItemStack(DiscoBlocks.DISCOMUNT.get(), FIRST_COINS));
            give(p, new ItemStack(ModItems.KAASKNABBEL_MILKSHAKE.get()));
            GuhQuests.say(p, npc, "quest.guhs.disco.first");
        }
        GuhAdvancements.grant(p, "disco_eerste");
        for (int goal : new int[]{5, 10, 15}) {
            if (score >= goal) {
                GuhAdvancements.grant(p, "disco_" + goal);
            }
        }
        // the Grote Guhspelen tab: every song, the Mika-Mambo master, all four songs danced
        if (score >= 1) {
            grantSpelen(p, "disco_" + liedje.lower());
        }
        if (liedje == DiscoLiedje.MAMBO && score >= 10) {
            grantSpelen(p, "disco_mambo_meester");
        }
        if (score >= GEDANST) {
            String list = saved.getString(LIEDJES);
            if (!(" " + list + " ").contains(" " + liedje.id + " ")) {
                saved.putString(LIEDJES, (list + " " + liedje.id).trim());
            }
            if (gedanst(p).size() == DiscoLiedje.values().length) {
                grantSpelen(p, "disco_alle_liedjes");
            }
        }
        reset(world);
    }

    /** Ends the game (the tiles go out, the music stops, the dancer is an ordinary player again). */
    void reset(ServerLevel world) {
        boolean was = phase != Phase.IDLE;
        allOff(world);
        java.util.Arrays.fill(flash, 0);
        if (dancer != null) {
            DANCERS.remove(dancer);
        }
        dancer = null;
        sequence.clear();
        playedAt.clear();
        phase = Phase.IDLE;
        timer = 0;
        step = 0;
        lastPad = -1;
        if (was) {
            muziek.stopAlles(world);
        }
    }

    // --- timing: the song's beat ---------------------------------------------------------------------------------------

    /** How long the DJ lights a colour: most of its beat (or half beat on double counts). */
    public static int onTicks(DiscoLiedje liedje, double stap) {
        return Math.max(2, (int) Math.round(stap * liedje.ticksPerBeat() * 0.65));
    }

    /** How long you may think about the next step (the song's number of beats). */
    public static int stepTimeout(DiscoLiedje liedje) {
        return (int) Math.ceil(liedje.stapBeats * liedje.ticksPerBeat());
    }

    /** Discomunten for a game (before the level bonus): nothing below 3, then one per two tones plus one, and a bonus from 10 and from 15. */
    public static int coins(int score) {
        return score < 3 ? 0 : (score - 1) / 2 + 1 + (score >= 10 ? 2 : 0) + (score >= 15 ? 3 : 0);
    }

    /** The floating top 3 of every song in front of the DJ booth (kept up to date every few seconds, and right after a game). */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        if (of(npc).centre() == null) {
            return;                                              // (not before the floor is found: the board hangs towards it)
        }
        List<String> boards = new ArrayList<>();
        List<Component> headings = new ArrayList<>();
        for (DiscoLiedje l : DiscoLiedje.values()) {
            boards.add(l.board());
            headings.add(Component.translatable("gui.guhs.scorebord.disco.lied", l.naam(), l.niveauNaam()));
        }
        Scorebord.show(world, scorebordPos(npc), "disco", Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.disco"),
                boards, headings, n -> n + " ♫"));
    }

    /**
     * Where the top 3 floats: {@link #SCOREBORD_OFFSET} (up, and forward) from the DJ-guh towards the middle of the
     * floor, so it hangs in front of the booth however the disco is turned.
     */
    public static Vec3 scorebordPos(GuhNpcEntity npc) {
        BlockPos c = of(npc).centre();
        Vec3 dir = c == null ? new Vec3(0, 0, 1) : new Vec3(c.getX() + 0.5 - npc.getX(), 0, c.getZ() + 0.5 - npc.getZ());
        if (dir.lengthSqr() < 1e-4) {
            dir = new Vec3(0, 0, 1);
        }
        return npc.position().add(dir.normalize().scale(SCOREBORD_OFFSET.z)).add(0, SCOREBORD_OFFSET.y, 0);
    }

    /** Your record on the standard song (the old record, the Highscores row "disco"). */
    public static int best(Player player) {
        return best(player, DiscoLiedje.DISCO70);
    }

    public static int best(Player player, DiscoLiedje liedje) {
        return GuhQuests.saved(player).getInt(bestKey(liedje));
    }

    private static String bestKey(DiscoLiedje liedje) {
        return liedje == DiscoLiedje.DISCO70 ? BEST : BEST + "_" + liedje.id;
    }

    /** The songs you danced at least {@link #GEDANST} colours on. */
    public static List<DiscoLiedje> gedanst(Player player) {
        String list = " " + GuhQuests.saved(player).getString(LIEDJES) + " ";
        List<DiscoLiedje> out = new ArrayList<>();
        for (DiscoLiedje l : DiscoLiedje.values()) {
            if (list.contains(" " + l.id + " ")) {
                out.add(l);
            }
        }
        return out;
    }

    /** Grants a shown advancement of the "De Grote Guhspelen" tab (guhs:grote_guhspelen/&lt;name&gt;). */
    static void grantSpelen(ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    // --- outside a game: singing tiles, the remix, the disco ball on its beat --------------------------------------------

    private void idle(GuhNpcEntity npc, ServerLevel world) {
        long now = world.getGameTime();
        if (centre == null) {
            if (now - lastScan > 100 && !world.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(REACH + 8)).isEmpty()) {
                scan(npc, world);
            }
            return;
        }
        tickFlashes(world);
        if (ticks % 5 == 0) {
            muziek.tick(world, centre, null, 0);
        }
        idleBeat(world, now);
        if (ticks % 2 != 0) {
            return;
        }
        List<ServerPlayer> near = world.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(centre).inflate(28, 8, 28));
        if (near.isEmpty()) {
            freePads.clear();
            return;
        }
        for (ServerPlayer p : near) {                            // tiles sing when you walk over them
            if (!p.onGround() || p.isSpectator()) {
                continue;
            }
            int pad = padUnder(world, p);
            Integer before = freePads.put(p.getUUID(), pad);
            if (pad >= 0 && (before == null || before != pad)) {
                play(world, pad, FLASH_TICKS, 0.6f);
            }
        }
    }

    /** The disco ball sparkles on the beat of the remix, as the first listener hears it (and a colour pulses on every bar). */
    private void idleBeat(ServerLevel world, long now) {
        DiscoMuziek.Luisteraar first = null;
        for (DiscoMuziek.Luisteraar l : muziek.luisteraars().values()) {
            if (first == null || l.sinds() < first.sinds()) {
                first = l;
            }
        }
        if (first == null) {
            idleBeat = -1;
            return;
        }
        double tpb = first.liedje().ticksPerBeat();
        long since = now - first.sinds();
        if (since > first.liedje().loopBeats * tpb) {
            return;                                              // (the fade-out and the breather: no beat)
        }
        int b = (int) Math.floor(since / tpb);
        if (b == idleBeat) {
            return;
        }
        idleBeat = b;
        beatSparkle(world, b, first.liedje());
        if (b % 4 == 0 && freePads.values().stream().noneMatch(pad -> pad >= 0)) {
            light(world, world.getRandom().nextInt(KLEUREN.length), 3);   // a quiet pulse on every bar (nobody on the tiles)
        }
    }

    private void beatSparkle(ServerLevel world, int b) {
        beatSparkle(world, b, liedje);
    }

    /** Light beams off the disco ball on the beat, in the song's own colours. */
    private void beatSparkle(ServerLevel world, int b, DiscoLiedje song) {
        if (balls.isEmpty()) {
            return;
        }
        int rgb = song.kleur(b);
        var dust = new DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.6f);
        for (int k = 0; k < 3; k++) {
            BlockPos ball = balls.get(world.getRandom().nextInt(balls.size()));
            Vec3 dir = new Vec3(world.getRandom().nextGaussian(), -Math.abs(world.getRandom().nextGaussian()), world.getRandom().nextGaussian()).normalize();
            world.sendParticles(ParticleTypes.END_ROD, ball.getX() + 0.5 + dir.x * 0.7, ball.getY() + 0.5 + dir.y * 0.7, ball.getZ() + 0.5 + dir.z * 0.7,
                    0, dir.x, dir.y, dir.z, 0.35);
            world.sendParticles(dust, ball.getX() + 0.5 + dir.x * 0.8, ball.getY() + 0.5 + dir.y * 0.8, ball.getZ() + 0.5 + dir.z * 0.8,
                    2, 0.15, 0.15, 0.15, 0);
        }
    }

    // --- the tiles ---------------------------------------------------------------------------------------------------

    /** Lights colour c for a while, with its note and sparkles above the tiles. */
    private void play(ServerLevel world, int c, int ticks, float volume) {
        light(world, c, ticks);
        DiscoTileBlock.Kleur kleur = KLEUREN[c];
        List<BlockPos> tiles = pads.get(c);
        if (tiles.isEmpty()) {
            return;
        }
        BlockPos mid = tiles.get(tiles.size() / 2);
        world.playSound(null, mid, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, volume, kleur.pitch);
        world.playSound(null, mid, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, volume * 0.5f, kleur.pitch);
        var dust = new DustParticleOptions(new org.joml.Vector3f(((kleur.rgb >> 16) & 255) / 255f, ((kleur.rgb >> 8) & 255) / 255f,
                (kleur.rgb & 255) / 255f), 1.4f);
        for (int i = 0; i < tiles.size(); i += 2) {
            BlockPos t = tiles.get(i);
            world.sendParticles(dust, t.getX() + 0.5, t.getY() + 1.15, t.getZ() + 0.5, 1, 0.25, 0.05, 0.25, 0);
        }
        world.sendParticles(ParticleTypes.NOTE, mid.getX() + 0.5, mid.getY() + 1.6, mid.getZ() + 0.5, 0, c / 4.0 + 0.1, 0, 0, 1);
    }

    private void light(ServerLevel world, int c, int ticks) {
        flash[c] = Math.max(flash[c], ticks);
        setLit(world, c, true);
    }

    private void tickFlashes(ServerLevel world) {
        for (int c = 0; c < flash.length; c++) {
            if (flash[c] > 0 && --flash[c] == 0) {
                setLit(world, c, false);
            }
        }
    }

    private void setLit(ServerLevel world, int c, boolean on) {
        for (BlockPos pos : pads.get(c)) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof DiscoTileBlock && state.getValue(DiscoTileBlock.LIT) != on) {
                world.setBlock(pos, state.setValue(DiscoTileBlock.LIT, on), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void allOff(ServerLevel world) {
        for (int c = 0; c < KLEUREN.length; c++) {
            setLit(world, c, false);
        }
    }

    /** The colour of the tile under the player's feet, or -1. */
    private int padUnder(ServerLevel world, Player p) {
        BlockState state = world.getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
        return state.getBlock() instanceof DiscoTileBlock tile ? tile.kleur.ordinal() : -1;
    }

    private boolean onFloor(Player p) {
        double dx = p.getX() - (centre.getX() + 0.5), dz = p.getZ() - (centre.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) <= floorRadius + 4 && p.getY() > centre.getY() - 2.5 && p.getY() < centre.getY() + 4;
    }

    /**
     * Finds the dance floor of this DJ-guh: the tiles nearest to him and every tile that hangs together with them (at
     * most one block apart, like the fields round the golden cross), and the disco ball. The dancer starts in the middle.
     */
    void scan(GuhNpcEntity npc, ServerLevel world) {
        lastScan = world.getGameTime();
        if (centre != null) {
            return;
        }
        pads.forEach(List::clear);
        balls.clear();
        BlockPos c = npc.blockPosition();
        Map<BlockPos, Integer> found = new HashMap<>();
        BlockPos nearest = null;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-REACH, -BELOW, -REACH), c.offset(REACH, ABOVE, REACH))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof DiscoTileBlock tile) {
                found.put(pos.immutable(), tile.kleur.ordinal());
                if (nearest == null || pos.distSqr(c) < nearest.distSqr(c)) {
                    nearest = pos.immutable();
                }
            } else if (state.is(DiscoBlocks.DISCOBAL.get()) && balls.size() < 256) {
                balls.add(pos.immutable());
            }
        }
        if (nearest == null) {
            return;
        }
        java.util.Set<BlockPos> floor = new java.util.HashSet<>(java.util.List.of(nearest));
        java.util.ArrayDeque<BlockPos> todo = new java.util.ArrayDeque<>(floor);
        while (!todo.isEmpty()) {
            BlockPos p = todo.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-2, 0, -2), p.offset(2, 0, 2))) {
                if (found.containsKey(n) && floor.add(n.immutable())) {
                    todo.add(n.immutable());
                }
            }
        }
        long sx = 0, sz = 0;
        for (BlockPos pos : floor) {
            pads.get(found.get(pos)).add(pos);
            sx += pos.getX();
            sz += pos.getZ();
        }
        if (pads.stream().anyMatch(List::isEmpty)) {
            pads.forEach(List::clear);
            return;                                                  // no complete floor here
        }
        pads.forEach(list -> list.sort(java.util.Comparator.comparingLong(BlockPos::asLong)));
        int n = floor.size();
        centre = new BlockPos((int) Math.floor((double) sx / n + 0.5), nearest.getY() + 1, (int) Math.floor((double) sz / n + 0.5));
        floorRadius = 0;
        for (BlockPos pos : floor) {
            floorRadius = Math.max(floorRadius, Math.sqrt(Math.pow(pos.getX() - centre.getX(), 2) + Math.pow(pos.getZ() - centre.getZ(), 2)) + 0.5);
        }
        allOff(world);                                              // (lit tiles left over from a restart mid-game)
    }

    // --- helpers -----------------------------------------------------------------------------------------------------

    @Nullable
    private ServerPlayer dancer(ServerLevel world) {
        return dancer == null ? null : world.getServer().getPlayerList().getPlayer(dancer);
    }

    /** What the dancer (and whoever watches) sees in the actionbar. */
    private void bar(ServerLevel world, ServerPlayer p) {
        Component watch = Component.translatable("quest.guhs.disco.bar.kijker", p.getDisplayName(), sequence.size(), score, liedje.naam())
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer other : world.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(centre).inflate(32, 12, 32))) {
            if (other != p) {
                other.displayClientMessage(watch, true);
            }
        }
        if (phase == Phase.SHOW) {
            p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.kijk", sequence.size()).withStyle(ChatFormatting.AQUA), true);
        } else if (phase == Phase.INPUT) {
            int seconds = (timer + 19) / 20;
            p.displayClientMessage(Component.translatable("quest.guhs.disco.bar.jouw_beurt_tijd", step, sequence.size(), seconds)
                    .withStyle(seconds <= 1 ? ChatFormatting.RED : ChatFormatting.GREEN), true);
        }
    }

    private static void title(ServerPlayer p, Component title, @Nullable Component sub, int stay) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(2, stay, 6));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub == null ? Component.empty() : sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Dancers don't get hungrier or weaker than when they started (no free healing), and never run out of air. */
    private static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }

    // --- events ------------------------------------------------------------------------------------------------------

    /** No hurting dancers (a fall off the stage, a stray arrow...). */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isDancing(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A dancer whose game stopped ticking (its chunk unloaded, the DJ-guh vanished) is an ordinary player again. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        Long seen = DANCERS.get(player.getUUID());
        if (seen != null && player.level().getGameTime() - seen > 40) {
            DANCERS.remove(player.getUUID());
        }
    }

    /** Logging out ends your game: its DJ-guh sees you're no longer dancing (also when you log back in quickly). */
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        DANCERS.remove(event.getEntity().getUUID());
    }

    /** Off to another dimension: the song is over. */
    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        DANCERS.remove(event.getEntity().getUUID());
    }

    /** The games live in memory: forget them when the (single player) server stops, so no half game comes back. */
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.values().forEach(g -> g.muziek.vergeet());
        GAMES.clear();
        DANCERS.clear();
    }
}
