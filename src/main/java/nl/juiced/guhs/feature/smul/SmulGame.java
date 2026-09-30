package nl.juiced.guhs.feature.smul;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Het Vadsig eetfestijn, the minigame: talk to the Smulguh, and she puts you in the arena with a borrowed smulschaal.
 * After a 3-2-1 countdown food falls for 60 seconds, from the sky and out of the chutes: walk into it to catch it.
 * Catching in a row builds a combo (x2, x3); a golden smulknabbel is worth 10 and doubles everything for 5 seconds;
 * Mika-vet costs 5 points, your combo and makes you slow. Afterwards: smulmunten by score (more for better scores), your
 * record, and the bowl goes back. One game at a time per Smulguh (others watch from the stands).
 * <p>
 * A game only lasts a minute, so it isn't saved: after a restart it's simply over (the bowl and the food clean themselves up).
 * The arena is found once from its invisible markers and remembered in the Smulguh's roleData.
 */
public final class SmulGame {
    /** Actions from the Smulguh's screen. */
    public static final int START = 0, SHOP = 1;
    public static final int COUNTDOWN_TICKS = 60, GAME_TICKS = 20 * 60;
    /** How high above the arena floor the food appears; how long the golden smulmodus lasts. */
    public static final int DROP_HEIGHT = 15, GOLD_TICKS = 100;
    /** Mika-vet: points off, and how long you're slow. */
    public static final int MIKA_PENALTY = 5, MIKA_SLOW_TICKS = 60;
    /** The first game ever: a few extra smulmunten. */
    public static final int FIRST_BONUS = 4;
    /** The start marker is within SCAN of the Smulguh, the corners and chutes within ARENA_REACH of the start. */
    static final int SCAN = 32, ARENA_REACH = 14;

    /** Running games by Smulguh (server side). */
    private static final Map<UUID, SmulGame> GAMES = new ConcurrentHashMap<>();
    /** Who is playing, and at which Smulguh. */
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();

    /** The arena: where you start, the floor area food falls on (inclusive), and the ends of the chutes. */
    public record Arena(BlockPos start, BlockPos min, BlockPos max, List<BlockPos> chutes) {
        boolean contains(Player player) {
            return player.getX() > min.getX() - 2 && player.getX() < max.getX() + 3 && player.getZ() > min.getZ() - 2
                    && player.getZ() < max.getZ() + 3 && player.getY() > min.getY() - 3 && player.getY() < min.getY() + 24;
        }

        AABB box() {
            return new AABB(min.getX() - 2, min.getY() - 2, min.getZ() - 2, max.getX() + 3, min.getY() + DROP_HEIGHT + 6, max.getZ() + 3);
        }
    }

    private final UUID npcId;
    private final UUID player;
    private final String playerName;
    private final Arena arena;
    /** The level (2.9): makkelijk less Mika-vet and slower food, lastig more Mika-vet and faster food. */
    private final Niveau niveau;
    /** Ticks since the start (the countdown included). */
    private int ticks;
    private int nextSpawn;
    int score, caught, golden, streak, bestStreak, mikas;
    /** Food falls (the GameTests switch it off to catch by hand). */
    boolean spawning = true;
    /** Game tick (after the countdown) until which the golden smulmodus lasts; when the last catch message was shown. */
    private int goldUntil, lastMessage = -100;
    private long lastTick;

    private SmulGame(UUID npcId, ServerPlayer player, Arena arena, long now, Niveau niveau) {
        this.npcId = npcId;
        this.player = player.getUUID();
        this.playerName = player.getGameProfile().name();
        this.arena = arena;
        this.lastTick = now;
        this.niveau = niveau;
    }

    /** How much faster (or slower) the food falls on a level: makkelijk 0.8x, medium 1x, lastig 1.3x. */
    public static float valSnelheid(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 0.8f;
            case MEDIUM -> 1f;
            case LASTIG -> 1.3f;
        };
    }

    public Niveau niveau() {
        return niveau;
    }

    // --- who plays where ------------------------------------------------------------------------------------------------

    public static boolean isPlaying(Player player) {
        return PLAYERS.containsKey(player.getUUID());
    }

    /** Is the game of this Smulguh on? */
    public static boolean isRunning(UUID npc) {
        return GAMES.containsKey(npc);
    }

    @Nullable
    public static SmulGame of(GuhNpcEntity npc) {
        return GAMES.get(npc.getUUID());
    }

    public int score() {
        return score;
    }

    public int streak() {
        return streak;
    }

    public int caught() {
        return caught;
    }

    public boolean goldMode() {
        return playTicks() < goldUntil;
    }

    public boolean counting() {
        return ticks < COUNTDOWN_TICKS;
    }

    /** Ticks since the food started falling (0 during the countdown). */
    public int playTicks() {
        return Math.max(0, ticks - COUNTDOWN_TICKS);
    }

    public Arena arena() {
        return arena;
    }

    // --- talking to the Smulguh ---------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        SmulGame game = of(npc);
        boolean mine = game != null && game.player.equals(player.getUUID());
        GuhQuests.say(player, npc, game == null ? "quest.guhs.smul.hello" : mine ? "quest.guhs.smul.busy_you" : "quest.guhs.smul.busy");
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.2f);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game != null);
        if (game != null) {
            data.putString("Player", game.playerName);
            data.putInt("Left", Math.max(0, (COUNTDOWN_TICKS + GAME_TICKS - game.ticks) / 20));
            data.putInt("Score", game.score);
            data.putString("Niveau", game.niveau.id());
        }
        data.putInt("Best", best(player));
        Klassiekers.records(data, n -> best(player, n));
        data.putInt("Games", GuhQuests.saved(player).getIntOr(GAMES_KEY, 0));
        data.putBoolean("First", !GuhQuests.saved(player).getBooleanOr(PLAYED_KEY, false));
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new SmulPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.SMULGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        int a = Klassiekers.actie(action);
        if (a == SHOP) {
            GuhQuests.say(player, npc, "quest.guhs.smul.shop");
            npc.openShop(player);
        } else if (a == START) {
            start(npc, player, Klassiekers.niveau(action));
        }
    }

    // --- playing --------------------------------------------------------------------------------------------------------

    /** Starts a game (medium) for this player, if the arena is free. Returns whether it did. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player) {
        return start(npc, player, Niveau.MEDIUM);
    }

    /** Starts a game on this level for this player, if the arena is free. Returns whether it did. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau) {
        ServerLevel world = (ServerLevel) npc.level();
        SmulGame running = of(npc);
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        if (running != null) {
            GuhQuests.say(player, npc, running.player.equals(player.getUUID()) ? "quest.guhs.smul.busy_you" : "quest.guhs.smul.busy");
            return false;
        }
        if (isPlaying(player)) {
            GuhQuests.say(player, npc, "quest.guhs.smul.elsewhere");
            return false;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(player, npc, nl.juiced.guhs.feature.Minigames.SMUL)) {
            return false;
        }
        Arena arena = arena(npc);
        if (arena == null) {
            GuhQuests.say(player, npc, "quest.guhs.smul.broken");
            return false;
        }
        if (!giveBowl(player)) {
            GuhQuests.say(player, npc, "quest.guhs.smul.full");
            return false;
        }
        SmulGame game = new SmulGame(npc.getUUID(), player, arena, world.getGameTime(), niveau);
        GAMES.put(npc.getUUID(), game);
        PLAYERS.put(player.getUUID(), npc.getUUID());
        player.closeContainer();
        player.stopRiding();
        BlockPos s = arena.start();
        float yaw = (float) Math.toDegrees(Math.atan2(-(npc.getX() - s.getX() - 0.5), npc.getZ() - s.getZ() - 0.5));   // looking at her
        teleport(player, world, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, yaw);
        nl.juiced.guhs.feature.Minigames.startKeeping(player);
        GuhQuests.say(player, npc, "quest.guhs.smul.go");
        if (niveau != Niveau.MEDIUM) {
            GuhQuests.say(player, npc, "quest.guhs.klassiekers.smul." + niveau.id());
        }
        world.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        return true;
    }

    /** Every tick of the Smulguh while her game is on. */
    void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || !arena.contains(p)) {
            stop(npc, p);
            return;
        }
        refresh(p);
        keepBowl(p);
        ticks++;
        if (ticks <= COUNTDOWN_TICKS) {
            countdown(npc, p, world);
            return;
        }
        int t = playTicks();
        float progress = Math.min(1f, t / (float) GAME_TICKS);
        if (spawning && t >= nextSpawn) {
            spawn(world, progress);
            if (progress > 0.6f && world.getRandom().nextFloat() < 0.35f) {
                spawn(world, progress);                                   // near the end it pours
            }
            nextSpawn = t + Math.round(14 - 8 * progress) + world.getRandom().nextInt(3);
        }
        catchAll(npc, p, world);
        int left = GAME_TICKS - t;
        if (left == 20 * 30) {
            cheer(npc, p, "quest.guhs.smul.cheer.half");
        } else if (left == 20 * 10) {
            cheer(npc, p, "quest.guhs.smul.cheer.ten");
        }
        if (left > 0 && left <= 100 && left % 20 == 0) {
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1f, 1.6f);
        }
        if (t % 5 == 0 && t - lastMessage > 12) {                    // (a catch message stays readable for a moment)
            bar(p, left);
        }
        if (t >= GAME_TICKS) {
            finish(npc, p);
        }
    }

    private void countdown(GuhNpcEntity npc, ServerPlayer p, ServerLevel world) {
        if (ticks == 1 || ticks == 21 || ticks == 41) {
            int n = 3 - (ticks - 1) / 20;
            title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.smul.ready"), 0, 22, 2);
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + 0.2f * (3 - n));
        } else if (ticks == COUNTDOWN_TICKS) {
            title(p, Component.translatable("quest.guhs.smul.title.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.smul.title.go.sub"), 0, 20, 10);
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.5f);
            cheer(npc, p, "quest.guhs.smul.cheer.start");
        }
    }

    /** One bit of food: from a chute (a third of the time) or somewhere out of the sky above the arena. */
    SmulHapje spawn(ServerLevel world, float progress) {
        SmulHapje.Soort soort = SmulHapje.Soort.pick(world.getRandom(), progress, niveau);
        double x, y, z;
        if (!arena.chutes().isEmpty() && world.getRandom().nextFloat() < 0.33f) {
            BlockPos chute = arena.chutes().get(world.getRandom().nextInt(arena.chutes().size()));
            x = chute.getX() + 0.5;
            y = chute.getY();
            z = chute.getZ() + 0.5;
            world.sendParticles(ParticleTypes.CLOUD, x, y + 0.3, z, 6, 0.3, 0.1, 0.3, 0.02);
            world.playSound(null, x, y, z, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1f, 0.7f);
        } else {
            int w = arena.max().getX() - arena.min().getX() - 1, d = arena.max().getZ() - arena.min().getZ() - 1;
            x = arena.min().getX() + 1 + (w > 0 ? world.getRandom().nextInt(w) : 0) + 0.5;
            z = arena.min().getZ() + 1 + (d > 0 ? world.getRandom().nextInt(d) : 0) + 0.5;
            y = arena.start().getY() + DROP_HEIGHT;
        }
        float speed = (0.17f + 0.13f * progress + world.getRandom().nextFloat() * 0.03f + (soort == SmulHapje.Soort.GOUD ? 0.05f : 0f))
                * valSnelheid(niveau);
        return spawnAt(world, soort, new Vec3(x, y, z), speed);
    }

    SmulHapje spawnAt(ServerLevel world, SmulHapje.Soort soort, Vec3 pos, float speed) {
        SmulHapje hapje = SmulHapje.create(world, soort, speed, npcId);
        hapje.snapTo(pos.x, pos.y, pos.z, world.getRandom().nextFloat() * 360f, 0);
        world.addFreshEntity(hapje);
        return hapje;
    }

    /** What's the catching reach of the player with the bowl: a bit wider and higher than the player. */
    private static AABB reach(Player p) {
        return p.getBoundingBox().inflate(0.6, 0.2, 0.6).expandTowards(0, 0.8, 0);
    }

    private void catchAll(GuhNpcEntity npc, ServerPlayer p, ServerLevel world) {
        for (SmulHapje hapje : world.getEntitiesOfClass(SmulHapje.class, reach(p), h -> h.isAlive() && npcId.equals(h.game()))) {
            catchHapje(npc, p, hapje);
        }
    }

    /** The player caught this bit of food (or Mika-vet...). */
    public void catchHapje(GuhNpcEntity npc, ServerPlayer p, SmulHapje hapje) {
        if (!hapje.isAlive() || counting()) {
            return;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "smul", hapje.soort().good()); // samen
        ServerLevel world = (ServerLevel) p.level();
        SmulHapje.Soort soort = hapje.soort();
        hapje.discard();
        if (!soort.good()) {
            mikas++;
            score = Math.max(0, score - MIKA_PENALTY);
            streak = 0;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MIKA_SLOW_TICKS, 2, false, true));
            world.sendParticles(ParticleTypes.LARGE_SMOKE, hapje.getX(), hapje.getY() + 0.3, hapje.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            world.playSound(null, p.blockPosition(), ModSounds.MIKA_HURT.get(), SoundSource.PLAYERS, 0.9f, 1.3f);
            p.sendOverlayMessage(Component.translatable("quest.guhs.smul.mika", MIKA_PENALTY).withStyle(ChatFormatting.DARK_GRAY));
            lastMessage = playTicks();
            if (mikas == 1 || world.getRandom().nextInt(3) == 0) {
                cheer(npc, p, "quest.guhs.smul.cheer.mika");
            }
            return;
        }
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        int gained = soort.points * combo(streak) * (goldMode() ? 2 : 1);
        score += gained;
        caught++;
        world.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, hapje.stack()), hapje.getX(), hapje.getY() + 0.3, hapje.getZ(),
                8, 0.2, 0.2, 0.2, 0.08);
        world.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.6f, 1.2f + world.getRandom().nextFloat() * 0.2f);
        world.playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f,
                0.8f + Math.min(streak, 15) * 0.07f);
        if (soort == SmulHapje.Soort.GOUD) {
            golden++;
            goldUntil = playTicks() + GOLD_TICKS;
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.6, 0.5, 0.3);
            world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.8f);
            title(p, Component.empty(), Component.translatable("quest.guhs.smul.gold").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 0, 30, 10);
            cheer(npc, p, "quest.guhs.smul.cheer.gold");
        }
        if (streak == 5 || streak == 10) {
            world.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2.1, p.getZ(), 5, 0.4, 0.2, 0.4, 0);
            title(p, Component.empty(), Component.translatable("quest.guhs.smul.combo", combo(streak)).withStyle(ChatFormatting.LIGHT_PURPLE), 0, 20, 8);
            if (streak == 10) {
                cheer(npc, p, "quest.guhs.smul.cheer.combo");
            }
        }
        p.sendOverlayMessage(Component.translatable("quest.guhs.smul.caught", gained, score).withStyle(ChatFormatting.LIGHT_PURPLE));
        lastMessage = playTicks();
    }

    /** The combo multiplier: x2 from 5 in a row, x3 from 10. */
    public static int combo(int streak) {
        return 1 + Math.min(streak, 10) / 5;
    }

    /** A good bit of food of this game went splat: the combo is broken. */
    static void missed(UUID npc) {
        SmulGame game = GAMES.get(npc);
        if (game != null) {
            game.streak = 0;
        }
    }

    private void bar(ServerPlayer p, int left) {
        Component bar = Component.translatable(goldMode() ? "quest.guhs.smul.bar.gold" : "quest.guhs.smul.bar", score,
                String.format(java.util.Locale.ROOT, "%d", (left + 19) / 20), combo(streak)).withStyle(goldMode() ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("  ")).append(Klassiekers.naam(niveau));
        p.sendOverlayMessage(bar);
    }

    /** The Smulguh cheers you on (in the chat, with a happy squeak and hearts over her head). */
    private void cheer(GuhNpcEntity npc, ServerPlayer p, String key) {
        GuhQuests.say(p, npc, key);
        ServerLevel world = (ServerLevel) npc.level();
        world.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1f + world.getRandom().nextFloat() * 0.3f);
        world.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + npc.getBbHeight() + 0.4, npc.getZ(), 4, 0.4, 0.2, 0.4, 0);
    }

    /** Now and then, when nobody is playing, the Smulguh calls people near her to come and play. */
    static void invite(GuhNpcEntity npc) {
        if (of(npc) != null || npc.tickCount % (20 * 30) != 0 || !(npc.level() instanceof ServerLevel world)) {
            return;
        }
        Player near = world.getNearestPlayer(npc, 10);
        if (near instanceof ServerPlayer p && !isPlaying(p) && !p.isSpectator()) {
            p.sendOverlayMessage(Component.literal("<").append(npc.getDisplayName()).append("> ")
                    .append(Component.translatable("quest.guhs.smul.invite" + (1 + world.getRandom().nextInt(3)))).withStyle(ChatFormatting.LIGHT_PURPLE));
            npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.3f);
            world.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + npc.getBbHeight() + 0.4, npc.getZ(), 2, 0.3, 0.1, 0.3, 0);
        }
    }

    // --- the end ------------------------------------------------------------------------------------------------------------

    /** Smulmunten for a score: none without catching anything, 2 for playing along, up to 8 for a vadsige score. */
    public static int munten(int score) {
        if (score <= 0) {
            return 0;                                                   // (no smulmunten for standing still)
        }
        int[] steps = {25, 50, 80, 120, 170, 230};
        int munten = 2;
        for (int step : steps) {
            if (score >= step) {
                munten++;
            }
        }
        return munten;
    }

    /** The minute is over: smulmunten, the record, advancements, and back to the Smulguh. */
    void finish(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        end(npc, p);
        CompoundTag saved = GuhQuests.saved(p);
        int munten = niveau.munten(munten(score));
        boolean first = !saved.getBooleanOr(PLAYED_KEY, false);
        saved.putBoolean(PLAYED_KEY, true);
        saved.putInt(GAMES_KEY, saved.getIntOr(GAMES_KEY, 0) + 1);
        saved.putInt(GOLD_KEY, saved.getIntOr(GOLD_KEY, 0) + golden);
        int best = best(p, niveau);
        boolean record = score > best;
        if (record) {
            saved.putInt(Klassiekers.sleutel(BEST_KEY, niveau), score);
        }
        if (munten + (first ? FIRST_BONUS : 0) > 0) {
            give(p, new ItemStack(SmulFeature.SMULMUNT.get(), munten + (first ? FIRST_BONUS : 0)));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.smul.done", score, caught, golden, bestStreak).withStyle(ChatFormatting.GOLD));
        if (munten > 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.smul.munten", munten).withStyle(ChatFormatting.YELLOW));
        }
        if (first) {
            give(p, new ItemStack(ModItems.GUH_TAART.get()));
            GuhQuests.say(p, npc, "quest.guhs.smul.first", FIRST_BONUS);
        }
        p.sendSystemMessage((record ? Component.translatable("quest.guhs.smul.record", score)
                : Component.translatable("quest.guhs.smul.best", best)).withStyle(record ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        if (score > 0) {
            Scorebord.submit(p, niveau.board(BOARD), score, false);     // (says so in the chat when you made the top 3)
            Klassiekers.gespeeld(p, "smul", niveau);
        }
        showScores(npc);
        title(p, Component.translatable(record ? "quest.guhs.smul.title.record" : "quest.guhs.smul.title.end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("quest.guhs.smul.title.points", score), 5, 60, 20);
        GuhAdvancements.grant(p, "smul_gespeeld");
        if (score >= 100) {
            GuhAdvancements.grant(p, "smul_100");
        }
        if (score >= 200) {
            GuhAdvancements.grant(p, "smul_200");
        }
        if (golden > 0) {
            GuhAdvancements.grant(p, "smul_goud");
        }
        GuhQuests.say(p, npc, score >= 200 ? "quest.guhs.smul.end.super" : score >= 100 ? "quest.guhs.smul.end.good" : "quest.guhs.smul.end.ok");
        world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.2f);
        world.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1f, 1f);
        world.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1.5, p.getZ(), 40, 0.6, 0.6, 0.6, 0.15);
        world.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + npc.getBbHeight() + 0.4, npc.getZ(), 8, 0.5, 0.3, 0.5, 0);
    }

    /** The player walked off, logged out, died, or the arena went away: no smulmunten. */
    void stop(GuhNpcEntity npc, @Nullable ServerPlayer p) {
        end(npc, null);
        if (p != null) {
            cleanup(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.smul.stopped", score).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The game is over: the food goes away, the bowl goes back, and the player (if given) back to the Smulguh. */
    private void end(GuhNpcEntity npc, @Nullable ServerPlayer p) {
        GAMES.remove(npcId, this);
        PLAYERS.remove(player, npcId);
        ServerLevel world = (ServerLevel) npc.level();
        for (SmulHapje hapje : world.getEntitiesOfClass(SmulHapje.class, arena.box(), h -> npcId.equals(h.game()))) {
            hapje.discard();
        }
        if (p != null) {
            cleanup(p);
            Vec3 exit = exitSpot(npc, arena);
            float yaw = (float) Math.toDegrees(Math.atan2(-(npc.getX() - exit.x), npc.getZ() - exit.z));
            teleport(p, world, exit.x, exit.y, exit.z, yaw);
        }
    }

    /** Takes back the smulschaal (from everywhere in the inventory) and the Mika-vet slowness. */
    public static void cleanup(ServerPlayer p) {
        PLAYERS.remove(p.getUUID());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(SmulFeature.SMULSCHAAL.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (p.containerMenu.getCarried().is(SmulFeature.SMULSCHAAL.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
        MobEffectInstance slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getDuration() <= MIKA_SLOW_TICKS && slow.getAmplifier() == 2) {
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    // --- the borrowed bowl --------------------------------------------------------------------------------------------------

    /** Puts the smulschaal in the player's hand: in the selected slot, a free hotbar slot, or swapped with a free slot. */
    static boolean giveBowl(ServerPlayer p) {
        Inventory inv = p.getInventory();
        ItemStack bowl = new ItemStack(SmulFeature.SMULSCHAAL.get());
        if (inv.getItem(inv.getSelectedSlot()).isEmpty()) {
            inv.setItem(inv.getSelectedSlot(), bowl);
            return true;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setSelectedSlot(i);
                if (p.connection != null) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(i));
                }
                inv.setItem(i, bowl);
                return true;
            }
        }
        int free = inv.getFreeSlot();
        if (free < 0) {
            return false;
        }
        inv.setItem(free, inv.getItem(inv.getSelectedSlot()));
        inv.setItem(inv.getSelectedSlot(), bowl);
        return true;
    }

    /** The bowl can't be put away in a chest (or a guh's wardrobe...) or thrown away: it comes back to you. */
    private static void keepBowl(ServerPlayer p) {
        for (Slot slot : p.containerMenu.slots) {
            if (slot.container != p.getInventory() && slot.getItem().is(SmulFeature.SMULSCHAAL.get())) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (!p.getInventory().contains(new ItemStack(SmulFeature.SMULSCHAAL.get())) && !p.containerMenu.getCarried().is(SmulFeature.SMULSCHAAL.get())) {
            giveBowl(p);
        }
    }

    // --- events ---------------------------------------------------------------------------------------------------------------

    /**
     * Closing a chest (or a guh's wardrobe...) with the bowl in it: it doesn't stay there (a click and a close can come in the
     * same tick, before {@link #keepBowl} sees it). The game hands a new one back.
     */
    public static void onContainerClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            for (Slot slot : event.getContainer().slots) {
                if (slot.container != p.getInventory() && slot.getItem().is(SmulFeature.SMULSCHAAL.get())) {
                    slot.set(ItemStack.EMPTY);
                }
            }
        }
    }

    /** The bowl can't be handed to anything (item frames, armour stands, allays, guhs...); talking to guh characters is fine. */
    public static void onInteractEntity(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().is(SmulFeature.SMULSCHAAL.get()) && !(event.getTarget() instanceof GuhNpcEntity)) {
            event.setCanceled(true);
        }
    }

    public static void onInteractEntityAt(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getItemStack().is(SmulFeature.SMULSCHAAL.get()) && !(event.getTarget() instanceof GuhNpcEntity)) {
            event.setCanceled(true);
        }
    }

    /** No hurting players while they smul (a stray arrow, fall damage...). */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && isPlaying(p) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** Dying (/kill...) ends the game before the bowl could drop. */
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    /** A game whose Smulguh stopped ticking (her chunk unloaded) is over for its player. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !isPlaying(p)) {
            return;
        }
        UUID npc = PLAYERS.get(p.getUUID());
        SmulGame game = npc == null ? null : GAMES.get(npc);
        if (game == null || p.level().getGameTime() - game.lastTick > 40) {
            if (game != null) {
                GAMES.remove(npc, game);
            }
            cleanup(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.smul.stopped", game == null ? 0 : game.score).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        PLAYERS.clear();
    }

    /** Ends this player's game (without rewards), wherever it is. */
    static void stopFor(ServerPlayer p) {
        UUID npcId = PLAYERS.get(p.getUUID());
        SmulGame game = npcId == null ? null : GAMES.get(npcId);
        if (game != null && p.level().getServer() != null) {
            for (ServerLevel level : p.level().getServer().getAllLevels()) {
                if (level.getEntity(npcId) instanceof GuhNpcEntity npc) {
                    game.stop(npc, p);
                    return;
                }
            }
            GAMES.remove(npcId, game);
        }
        cleanup(p);
    }

    // --- the arena ------------------------------------------------------------------------------------------------------------

    /** The arena of this Smulguh: remembered in her roleData, or found by its markers. Null when there is none. */
    @Nullable
    public static Arena arena(GuhNpcEntity npc) {
        CompoundTag data = npc.roleData;
        if (data.contains("ArenaStart")) {
            List<BlockPos> chutes = new ArrayList<>();
            for (long l : data.getLongArray("ArenaChutes").orElse(new long[0])) {
                chutes.add(BlockPos.of(l));
            }
            return new Arena(BlockPos.of(data.getLongOr("ArenaStart", 0L)), BlockPos.of(data.getLongOr("ArenaMin", 0L)), BlockPos.of(data.getLongOr("ArenaMax", 0L)), chutes);
        }
        Arena arena = findArena(npc);
        if (arena != null) {
            data.putLong("ArenaStart", arena.start().asLong());
            data.putLong("ArenaMin", arena.min().asLong());
            data.putLong("ArenaMax", arena.max().asLong());
            data.put("ArenaChutes", new LongArrayTag(arena.chutes().stream().mapToLong(BlockPos::asLong).toArray()));
        }
        return arena;
    }

    @Nullable
    private static Arena findArena(GuhNpcEntity npc) {
        BlockPos c = npc.blockPosition();
        List<BlockPos> starts = new ArrayList<>(), corners = new ArrayList<>(), chutes = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-SCAN, -6, -SCAN), c.offset(SCAN, DROP_HEIGHT + 8, SCAN))) {
            var state = npc.level().getBlockState(pos);
            if (state.is(SmulFeature.SMUL_START.get())) {
                starts.add(pos.immutable());
            } else if (state.is(SmulFeature.SMUL_HOEK.get())) {
                corners.add(pos.immutable());
            } else if (state.is(SmulFeature.SMUL_TRECHTER.get())) {
                chutes.add(pos.immutable());
            }
        }
        BlockPos start = starts.stream().min(java.util.Comparator.comparingDouble(s -> s.distSqr(c))).orElse(null);
        if (start == null) {
            return null;
        }
        corners.removeIf(p -> Math.max(Math.abs(p.getX() - start.getX()), Math.abs(p.getZ() - start.getZ())) > ARENA_REACH);
        chutes.removeIf(p -> Math.max(Math.abs(p.getX() - start.getX()), Math.abs(p.getZ() - start.getZ())) > ARENA_REACH);
        if (corners.size() < 2) {
            return null;
        }
        int x0 = corners.stream().mapToInt(BlockPos::getX).min().orElseThrow(), x1 = corners.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int z0 = corners.stream().mapToInt(BlockPos::getZ).min().orElseThrow(), z1 = corners.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        return new Arena(start, new BlockPos(x0, start.getY(), z0), new BlockPos(x1, start.getY(), z1), chutes);
    }

    // --- records and little helpers ---------------------------------------------------------------------------------------

    static final String BEST_KEY = "guhs_smul_best", GAMES_KEY = "guhs_smul_games", PLAYED_KEY = "guhs_smul_played", GOLD_KEY = "guhs_smul_gouden";

    /** The world's top 3 (in {@link Scorebord}): the most points in one game. */
    public static final String BOARD = "smul_punten";

    /** The top 3 of the whole world floats above the Smulguh's head on her podium. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Scorebord.show(world, npc.position().add(0, 2.4, 0), "smul", Klassiekers.bord(world.getServer(), Component.translatable("gui.guhs.scorebord.smul"),
                BOARD, s -> s + " pt"));
    }

    /** Your highest score (0 = never played), on medium. */
    public static int best(Player player) {
        return GuhQuests.saved(player).getIntOr(BEST_KEY, 0);
    }

    /** Your highest score on this level (medium = the old record). */
    public static int best(Player player, Niveau niveau) {
        return GuhQuests.saved(player).getIntOr(Klassiekers.sleutel(BEST_KEY, niveau), 0);
    }

    /** While playing: no hungrier or weaker than at the start, never out of breath. */
    private static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);    // (no hungrier or weaker than at the start: no free healing)
    }

    /**
     * Where you come out after a game: 2.5 blocks in front of the Smulguh, on the side away from the arena (she turns her
     * head to look at people, so her own rotation can't be trusted), on the first floor below that spot.
     */
    static Vec3 exitSpot(GuhNpcEntity npc, Arena arena) {
        double dx = npc.getX() - (arena.start().getX() + 0.5), dz = npc.getZ() - (arena.start().getZ() + 0.5);
        double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double x = npc.getX() + dx / len * 2.5, z = npc.getZ() + dz / len * 2.5;
        var level = npc.level();
        BlockPos top = BlockPos.containing(x, npc.getY() + 1, z);
        for (int dy = 0; dy < 6; dy++) {
            BlockPos feet = top.below(dy);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                return new Vec3(x, feet.getY(), z);
            }
        }
        return new Vec3(x, npc.getY(), z);
    }

    private static void teleport(ServerPlayer p, ServerLevel world, double x, double y, double z, float yRot) {
        if (p instanceof FakePlayer || p.connection == null) {
            p.snapTo(x, y, z, yRot, 0);
        } else {
            p.teleportTo(world, x, y, z, yRot, 0);
        }
    }

    private static void title(ServerPlayer p, Component title, Component subtitle, int in, int stay, int out) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }
}
