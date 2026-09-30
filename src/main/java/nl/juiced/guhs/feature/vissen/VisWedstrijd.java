package nl.juiced.guhs.feature.vissen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The Guhvis-wedstrijd: a fishing contest at the Visguh's pond. Talk to the Visguh, press start: you get a Guhvis-hengel
 * on loan, a 5 second countdown, then 3 minutes of fishing. Everything that bites in the pond is one of the special
 * fish ({@link VisSoort}), worth points by its weight; the fish are yours to keep. Friends can join while at least a
 * minute is left (everyone plays for their own score; the winner gets a bonus). At the end: visbonnen by points, your
 * records (best total, heaviest fish; saved with the player, so they survive dying), the world's top 3 on the floating
 * scoreboard above the pond's record board ({@link Scorebord}), and the loaned rod goes back. While fishing you can't get hurt or hungry.
 * <p>
 * One contest per Visguh (so per pond). Contests are not saved: when the server stops, everyone is logged out and so out
 * of the contest (the rod then disappears by itself, see {@link GuhvisHengel}).
 */
public final class VisWedstrijd {
    /** Countdown, contest length and how long you may still join (all in ticks); at most this many anglers. */
    public static final int COUNTDOWN = 20 * 5, DURATION = 20 * 180, JOIN_UNTIL = 20 * 60, MAX_PLAYERS = 6;
    /** How far from the Visguh the pond reaches, and how far you may walk off before you drop out. */
    public static final int POND_RADIUS = 68, AREA_RADIUS = 80;
    /** How far from the Visguh the "~ Visrecords ~" sign may be. */
    private static final int SIGN_REACH = 18;
    /** Actions from the screen. */
    public static final int START = 0, JOIN = 1, SHOP = 2, STOP = 3;
    /** Visbonnen: one per this many points (plus two, or one when you stop early), at most MAX_BONNEN; a personal best or a win gives BONUS more. */
    public static final int POINTS_PER_BON = 60, MAX_BONNEN = 13, BONUS = 3, FIRST_BONNEN = 4;
    public static final int GOOD_SCORE = 300;
    /** The two top-3 boards (see {@link Scorebord}): best contest total, and heaviest fish (grams). */
    public static final String BOARD_POINTS = "vissen_punten", BOARD_HEAVIEST = "vissen_zwaarste";

    /** Everyone in a contest right now: player -> their Visguh. */
    private static final Map<UUID, UUID> ANGLERS = new ConcurrentHashMap<>();
    /** The running contests: Visguh -> contest. */
    private static final Map<UUID, VisWedstrijd> CONTESTS = new ConcurrentHashMap<>();

    /** One angler's contest so far. */
    public static final class Score {
        public final String name;
        public int points, fish, heaviestGrams;
        /** Fish that wriggled off the hook (lastig). */
        public int escaped;
        @Nullable
        public VisSoort heaviest;

        Score(String name) {
            this.name = name;
        }
    }

    /** What one bite brought. */
    public record Catch(VisSoort soort, int grams, int points) {
    }

    private final UUID npcId;
    /**
     * The level of the contest (2.9), the same for everyone who joins: the bite window (makkelijk long, lastig short, see
     * {@link GuhvisDobber}) and on lastig fish can wriggle off the hook ({@link #ontsnapKans}).
     */
    private final Niveau niveau;
    /** Game time when the fishing starts (after the countdown) and ends. */
    private long startTick, endTick;
    private long lastTick;
    private final Map<UUID, Score> scores = new LinkedHashMap<>();
    private final Map<UUID, ServerBossEvent> bars = new ConcurrentHashMap<>();
    /** Joined by more than one angler at some point (then there's a winner). */
    private boolean together;

    private VisWedstrijd(UUID npcId, long now, Niveau niveau) {
        this.npcId = npcId;
        this.niveau = niveau;
        this.startTick = now + COUNTDOWN;
        this.endTick = startTick + DURATION;
        this.lastTick = now;
    }

    // --- looking things up --------------------------------------------------------------------------------------------

    public static boolean isFishing(ServerPlayer player) {
        UUID npc = ANGLERS.get(player.getUUID());
        return npc != null && CONTESTS.containsKey(npc);
    }

    @Nullable
    public static VisWedstrijd of(GuhNpcEntity npc) {
        return CONTESTS.get(npc.getUUID());
    }

    @Nullable
    private static VisWedstrijd of(ServerPlayer player) {
        UUID npc = ANGLERS.get(player.getUUID());
        return npc == null ? null : CONTESTS.get(npc);
    }

    @Nullable
    public static Score score(ServerPlayer player) {
        VisWedstrijd contest = of(player);
        return contest == null ? null : contest.scores.get(player.getUUID());
    }

    public boolean isCountingDown(long now) {
        return now < startTick;
    }

    public int ticksLeft(long now) {
        return (int) Math.max(0, Math.min(DURATION, endTick - now));
    }

    public boolean canJoin(long now) {
        return endTick - now >= JOIN_UNTIL && scores.size() < MAX_PLAYERS;
    }

    public int players() {
        return scores.size();
    }

    public Niveau niveau() {
        return niveau;
    }

    /** The level of the contest this player fishes in (medium when none). */
    public static Niveau niveauVan(ServerPlayer player) {
        VisWedstrijd contest = of(player);
        return contest == null ? Niveau.MEDIUM : contest.niveau;
    }

    /**
     * The chance a fish wriggles off the hook as you reel it in: only on lastig. The Mika-meerval never lets go (njeg!);
     * the others 10 % for a light one up to 30 % for the heaviest, the Gouden Guhvis 10 % more.
     */
    public static float ontsnapKans(VisSoort soort, int grams, Niveau niveau) {
        if (niveau != Niveau.LASTIG || soort.isBad()) {
            return 0f;
        }
        float span = Math.max(1f, (soort.maxKg - soort.minKg) * 1000f);
        float heavy = Math.max(0f, Math.min(1f, (grams - soort.minKg * 1000f) / span));
        return 0.10f + 0.20f * heavy + (soort == VisSoort.GOUDEN_GUHVIS ? 0.10f : 0f);
    }

    // --- the screen ---------------------------------------------------------------------------------------------------

    /** Right-clicked the Visguh: a line in the chat and her screen. */
    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        VisWedstrijd contest = of(npc);
        long now = npc.level().getGameTime();
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        if (contest != null && contest.scores.containsKey(player.getUUID())) {
            Score s = contest.scores.get(player.getUUID());
            GuhQuests.say(player, npc, "quest.guhs.vissen.playing", time(contest.ticksLeft(now)), s.points);
        } else if (contest != null) {
            GuhQuests.say(player, npc, contest.canJoin(now) ? "quest.guhs.vissen.running" : "quest.guhs.vissen.watch");
        } else {
            GuhQuests.say(player, npc, games(player) == 0 ? "quest.guhs.vissen.hello" : "quest.guhs.vissen.hello_again");
        }
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new VissenPayloads.Open(npc.getId(), screenData(npc, player)));
    }

    /** Everything the Visguh's screen shows. */
    public static CompoundTag screenData(GuhNpcEntity npc, ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        VisWedstrijd contest = of(npc);
        long now = npc.level().getGameTime();
        data.putBoolean("Running", contest != null);
        if (contest != null) {
            data.putBoolean("Joinable", contest.canJoin(now));
            data.putBoolean("Playing", contest.scores.containsKey(player.getUUID()));
            data.putInt("Players", contest.players());
            data.putInt("Seconds", contest.ticksLeft(now) / 20);
            data.putString("Niveau", contest.niveau.id());
        }
        CompoundTag saved = GuhQuests.saved(player);
        data.putInt("Best", best(player));
        Klassiekers.records(data, n -> best(player, n));
        for (Niveau n : Niveau.values()) {
            data.put("Top_" + n.id(), board(npc, n.board(BOARD_POINTS)));
            data.put("Zwaarste_" + n.id(), board(npc, n.board(BOARD_HEAVIEST)));
        }
        data.putInt("Heaviest", saved.getInt(HEAVIEST));
        data.putString("HeaviestSoort", saved.getString(HEAVIEST_SOORT));
        data.putInt("Games", games(player));
        data.put("Top", board(npc, BOARD_POINTS));
        data.put("Zwaarste", board(npc, BOARD_HEAVIEST));
        return data;
    }

    /** A button on the screen. */
    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.VISGUH || player.distanceToSqr(npc) > 100) {
            return;
        }
        VisWedstrijd contest = of(npc);
        Niveau niveau = Klassiekers.niveau(action);
        action = Klassiekers.actie(action);
        if (action == SHOP) {
            npc.openShop(player);
        } else if (action == STOP) {
            if (contest != null && contest.scores.containsKey(player.getUUID())) {
                contest.finishPlayer(npc, player, false, false);
                contest.endIfEmpty();
            }
        } else if (action == START || action == JOIN) {
            if (isFishing(player) || nl.juiced.guhs.feature.Minigames.refuse(player, npc, nl.juiced.guhs.feature.Minigames.VISSEN)) {
                return;
            }
            if (contest == null) {
                start(npc, player, niveau);
            } else if (contest.canJoin(npc.level().getGameTime())) {
                contest.join(npc, player);
            } else {
                GuhQuests.say(player, npc, "quest.guhs.vissen.full");
            }
        }
    }

    // --- playing ------------------------------------------------------------------------------------------------------

    private static void start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau) {
        VisWedstrijd contest = new VisWedstrijd(npc.getUUID(), npc.level().getGameTime(), niveau);
        CONTESTS.put(npc.getUUID(), contest);
        if (!contest.join(npc, player)) {
            CONTESTS.remove(npc.getUUID());
            return;
        }
        GuhQuests.say(player, npc, "quest.guhs.vissen.start");
        if (niveau != Niveau.MEDIUM) {
            GuhQuests.say(player, npc, "quest.guhs.klassiekers.vissen." + niveau.id());
        }
    }

    private boolean join(GuhNpcEntity npc, ServerPlayer player) {
        ANGLERS.put(player.getUUID(), npcId);
        if (!giveRod(player)) {
            ANGLERS.remove(player.getUUID());
            GuhQuests.say(player, npc, "quest.guhs.vissen.no_room");
            return false;
        }
        if (!scores.isEmpty()) {
            together = true;
            tell(npc, Component.translatable("quest.guhs.vissen.joined", player.getDisplayName()).withStyle(ChatFormatting.AQUA));
        }
        scores.put(player.getUUID(), new Score(player.getGameProfile().getName()));
        if (niveau != Niveau.MEDIUM) {
            player.sendSystemMessage(Component.translatable("gui.guhs.klassiekers.vissen.niveau", Klassiekers.naam(niveau)).withStyle(ChatFormatting.AQUA));
        }
        ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.guhs.guh_npc.visguh"), BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_10);
        bar.addPlayer(player);
        bars.put(player.getUUID(), bar);
        nl.juiced.guhs.feature.Minigames.startKeeping(player);
        updateBar(player, npc.level().getGameTime());
        player.sendSystemMessage(Component.translatable("quest.guhs.vissen.go", DURATION / 20 / 60).withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** Every tick, from the Visguh: countdown, timer, anglers who wandered off, and the end. */
    public static void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 == 1) {
            showScores(npc);
        }
        VisWedstrijd contest = of(npc);
        if (contest != null) {
            contest.tickContest(npc);
        }
    }

    private void tickContest(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        long now = world.getGameTime();
        lastTick = now;
        if (now < startTick && (startTick - now) % 20 == 19) {                   // 5, 4, 3, 2, 1...
            int secs = (int) ((startTick - now + 19) / 20);
            for (ServerPlayer p : online(world)) {
                title(p, Component.literal(String.valueOf(secs)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.vissen.ready"), 18);
                sound(p, SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 0.8f + 0.1f * (5 - secs));
            }
        } else if (now == startTick) {                                              // ...VISSEN!
            for (ServerPlayer p : online(world)) {
                title(p, Component.translatable("gui.guhs.vissen.go_title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.vissen.go_subtitle"), 30);
                sound(p, SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.2f);
                sound(p, ModSounds.GUH_HAPPY.get(), 1f, 1.1f);
            }
        }
        if (now % 20 == 0) {
            List<UUID> gone = new ArrayList<>();
            for (UUID id : List.copyOf(scores.keySet())) {
                ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
                if (p == null || !p.isAlive() || p.level() != world) {
                    gone.add(id);
                } else if (p.distanceToSqr(npc) > AREA_RADIUS * AREA_RADIUS) {
                    p.sendSystemMessage(Component.translatable("quest.guhs.vissen.walked_off").withStyle(ChatFormatting.AQUA));
                    finishPlayer(npc, p, false, false);
                }
            }
            gone.forEach(id -> drop(world, id));
            int left = ticksLeft(now);
            for (ServerPlayer p : online(world)) {
                refresh(p);
                updateBar(p, now);
                if (rods(p) == 0 && p.containerMenu == p.inventoryMenu) {
                    giveRod(p);                                                      // (lost it somehow: here's another)
                }
                if (now > startTick && left <= 20 * 10 && left > 0) {             // the last ten seconds tick away
                    sound(p, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 1.6f);
                    p.displayClientMessage(Component.translatable("gui.guhs.vissen.last_seconds", left / 20).withStyle(ChatFormatting.GOLD), true);
                }
            }
            if (endIfEmpty()) {
                return;
            }
        }
        if (now >= endTick) {
            finish(npc);
        }
    }

    /** Ends the contest when nobody is left in it. */
    private boolean endIfEmpty() {
        if (scores.isEmpty()) {
            bars.values().forEach(ServerBossEvent::removeAllPlayers);
            bars.clear();
            CONTESTS.remove(npcId, this);
            return true;
        }
        return false;
    }

    /** Time's up: results for everyone (and a winner when fishing together). */
    private void finish(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<Map.Entry<UUID, Score>> ranking = new ArrayList<>(scores.entrySet());
        ranking.sort(Comparator.comparingInt((Map.Entry<UUID, Score> e) -> e.getValue().points).reversed());
        if (together && ranking.size() > 1) {
            MutableComponent board = Component.translatable("quest.guhs.vissen.ranking").withStyle(ChatFormatting.GOLD);
            for (int i = 0; i < ranking.size(); i++) {
                Score s = ranking.get(i).getValue();
                board.append(Component.literal("\n " + (i + 1) + ". " + s.name + " - ").withStyle(ChatFormatting.WHITE))
                        .append(Component.translatable("gui.guhs.vissen.points_fish", s.points, s.fish).withStyle(ChatFormatting.WHITE));
            }
            online(world).forEach(p -> p.sendSystemMessage(board));
        }
        UUID winner = together && ranking.size() > 1 && ranking.get(0).getValue().points > 0 ? ranking.get(0).getKey() : null;
        for (Map.Entry<UUID, Score> e : ranking) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                finishPlayer(npc, p, e.getKey().equals(winner), true);
            } else {
                drop(world, e.getKey());
            }
        }
        endIfEmpty();
    }

    /**
     * One angler is done (time's up, stopped, walked off): the rod goes back, visbonnen for the points, records, the
     * first-contest present. Only a contest fished to the end gives the extra "you took part" visbon (so starting,
     * catching one fish and stopping over and over isn't the fastest way to earn them).
     */
    private void finishPlayer(GuhNpcEntity npc, ServerPlayer player, boolean won, boolean complete) {
        Score s = scores.remove(player.getUUID());
        ANGLERS.remove(player.getUUID());
        takeRods(player);
        ServerBossEvent bar = bars.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
        if (s == null) {
            return;
        }
        ServerLevel world = player.serverLevel();
        CompoundTag saved = GuhQuests.saved(player);
        String bestKey = Klassiekers.sleutel(BEST, niveau);
        int oldBest = best(player, niveau);
        boolean record = s.points > 0 && s.points > oldBest;
        int bonnen = (complete ? bonnen(s.points) : earlyBonnen(s.points)) + (record && saved.contains(bestKey) ? BONUS : 0) + (won ? BONUS : 0);
        bonnen = bonnen > 0 ? niveau.munten(bonnen) : 0;
        if (record) {
            saved.putInt(bestKey, s.points);
        }
        saved.putInt(GAMES, games(player) + 1);
        player.sendSystemMessage(Component.translatable("quest.guhs.vissen.result", s.fish, s.points).withStyle(ChatFormatting.AQUA));
        if (s.escaped > 0) {
            player.sendSystemMessage(Component.translatable("quest.guhs.klassiekers.vissen.ontsnapt_totaal", s.escaped).withStyle(ChatFormatting.GRAY));
        }
        if (s.heaviest != null) {
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.result_heaviest",
                    Component.translatable("item.guhs." + s.heaviest.id()), VisSoort.kg(s.heaviestGrams)).withStyle(ChatFormatting.AQUA));
        }
        if (won) {
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.winner", BONUS).withStyle(ChatFormatting.GOLD));
        }
        if (record) {
            player.sendSystemMessage(Component.translatable(oldBest > 0 ? "quest.guhs.vissen.record" : "quest.guhs.vissen.first_record", s.points,
                    BONUS).withStyle(ChatFormatting.YELLOW));
        } else if (oldBest > 0) {
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.best_was", oldBest).withStyle(ChatFormatting.GRAY));
        }
        if (bonnen > 0) {
            give(player, new ItemStack(VissenFeature.VISBON.get(), bonnen));
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.bonnen", bonnen).withStyle(ChatFormatting.GOLD));
        } else {
            player.sendSystemMessage((complete || s.points <= 0 ? Component.translatable("quest.guhs.vissen.no_bonnen")
                    : Component.translatable("quest.guhs.vissen.no_bonnen_early", POINTS_PER_BON)).withStyle(ChatFormatting.GRAY));
        }
        if (!saved.getBoolean(FIRST)) {                                         // the very first contest: a present
            saved.putBoolean(FIRST, true);
            give(player, new ItemStack(VissenFeature.VISBON.get(), FIRST_BONNEN));
            give(player, new ItemStack(ModItems.GEBAKKEN_GUH_VIS.get(), 4));
            give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.first").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        GuhAdvancements.grant(player, "vissen_eerste");
        if (s.points >= GOOD_SCORE) {
            GuhAdvancements.grant(player, "vissen_300");
        }
        title(player, Component.translatable(record ? "gui.guhs.vissen.end_record" : "gui.guhs.vissen.end_title")
                        .withStyle(record ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.vissen.end_subtitle", s.points, bonnen), 50);
        world.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, record ? 1.0f : 1.3f);
        world.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.2, player.getZ(), record ? 40 : 15, 0.6, 0.6, 0.6, 0.08);
        if (s.points > 0) {
            Scorebord.submit(player, niveau.board(BOARD_POINTS), s.points, false);
            if (complete) {
                Klassiekers.gespeeld(player, "vissen", niveau);
            }
        }
        showScores(npc);
    }

    /** An angler is gone (logged out, died, other dimension): out of the contest without a result. */
    private void drop(ServerLevel world, UUID id) {
        scores.remove(id);
        ANGLERS.remove(id);
        ServerBossEvent bar = bars.remove(id);
        if (bar != null) {
            bar.removeAllPlayers();
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
        if (p != null) {
            takeRods(p);
        }
    }

    /** Takes a player out of whatever contest they're in, without a result. */
    public static void leave(ServerPlayer player) {
        UUID npc = ANGLERS.remove(player.getUUID());
        VisWedstrijd contest = npc == null ? null : CONTESTS.get(npc);
        if (contest != null) {
            contest.drop(player.serverLevel(), player.getUUID());
            contest.endIfEmpty();
        }
        takeRods(player);
    }

    // --- a bite! -----------------------------------------------------------------------------------------------------

    /** Is this spot in the Visguh's pond? (In the Guhmension it must be in the guhvis pond structure too.) */
    public static boolean inPond(GuhNpcEntity npc, Vec3 pos) {
        double dx = pos.x - npc.getX(), dz = pos.z - npc.getZ(), dy = pos.y - npc.getY();
        if (dx * dx + dz * dz > POND_RADIUS * POND_RADIUS || dy < -14 || dy > 4) {
            return false;
        }
        ServerLevel world = (ServerLevel) npc.level();
        return world.dimension() != ModDimensions.GUHMENSION || VissenProtection.inVijver(world, BlockPos.containing(pos));
    }

    /** Something bit (vanilla fishing): in the contest pond it's one of our fish instead of the vanilla catch. */
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        VisWedstrijd contest = of(player);
        if (contest == null) {
            return;
        }
        ServerLevel world = player.serverLevel();
        if (!(world.getEntity(contest.npcId) instanceof GuhNpcEntity npc) || !inPond(npc, event.getHookEntity().position())) {
            if (player.getMainHandItem().getItem() instanceof GuhvisHengel || player.getOffhandItem().getItem() instanceof GuhvisHengel) {
                event.setCanceled(true);          // the loaned rod only catches in the pond
                player.displayClientMessage(Component.translatable("gui.guhs.vissen.wrong_water").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        event.setCanceled(true);
        if (contest.isCountingDown(world.getGameTime())) {
            player.displayClientMessage(Component.translatable("gui.guhs.vissen.too_early").withStyle(ChatFormatting.AQUA), true);
            return;
        }
        VisSoort soort = VisSoort.random(world.getRandom());
        contest.hooked(npc, player, soort, soort.randomGrams(world.getRandom()), event.getHookEntity().position(), world.getRandom().nextFloat());
    }

    /**
     * A fish on the hook, reeled in: on lastig it may wriggle off first (roll below {@link #ontsnapKans}: no points, a
     * splash and a giggle from the Visguh), else it's landed. Returns the catch, or null when it got away.
     */
    @Nullable
    public Catch hooked(GuhNpcEntity npc, ServerPlayer player, VisSoort soort, int grams, Vec3 from, float roll) {
        Score s = scores.get(player.getUUID());
        if (s == null) {
            return null;
        }
        if (roll < ontsnapKans(soort, grams, niveau)) {
            s.escaped++;
            ServerLevel world = player.serverLevel();
            world.sendParticles(ParticleTypes.SPLASH, from.x, from.y + 0.2, from.z, 30, 0.5, 0.1, 0.5, 0.2);
            world.sendParticles(ParticleTypes.BUBBLE_POP, from.x, from.y + 0.1, from.z, 10, 0.4, 0.1, 0.4, 0.02);
            world.playSound(null, from.x, from.y, from.z, SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 1f, 0.7f);
            world.playSound(null, from.x, from.y, from.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
            Component name = Component.translatable("item.guhs." + soort.id()).withStyle(soort.colour);
            player.displayClientMessage(Component.translatable("gui.guhs.klassiekers.vissen.ontsnapt", name, VisSoort.kg(grams))
                    .withStyle(ChatFormatting.AQUA), true);
            return null;
        }
        return land(npc, player, soort, grams, from);
    }

    /** Lands a fish: points, the fish (yours!), records, and a lot of fuss. */
    @Nullable
    public Catch land(GuhNpcEntity npc, ServerPlayer player, VisSoort soort, int grams, Vec3 from) {
        Score s = scores.get(player.getUUID());
        if (s == null) {
            return null;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(player, "vissen", !soort.isBad()); // samen
        ServerLevel world = player.serverLevel();
        int points = soort.points(grams);
        s.points += points;
        s.fish++;
        if (!soort.isBad() && grams > s.heaviestGrams) {
            s.heaviestGrams = grams;
            s.heaviest = soort;
        }
        ItemStack caught = new ItemStack(VissenFeature.vis(soort));
        if (soort == VisSoort.GOUDEN_GUHVIS) {                                  // a collector's piece: who, how heavy, which one
            CompoundTag saved = GuhQuests.saved(player);
            int nr = saved.getInt(GOLDEN) + 1;
            saved.putInt(GOLDEN, nr);
            VisItem.stamp(caught, s.name, grams, nr);
        }
        give(player, caught.copy());
        player.awardStat(Stats.FISH_CAUGHT);
        // the fish jumps out of the water towards you (just for show: the real one is in your pockets already)
        ItemEntity flying = new ItemEntity(world, from.x, from.y + 0.3, from.z, caught);
        double dx = player.getX() - from.x, dy = player.getY() - from.y, dz = player.getZ() - from.z;
        flying.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        flying.setNeverPickUp();
        flying.lifespan = 14;
        world.addFreshEntity(flying);
        world.sendParticles(ParticleTypes.SPLASH, from.x, from.y + 0.2, from.z, 20, 0.4, 0.1, 0.4, 0.1);
        world.sendParticles(ParticleTypes.BUBBLE_POP, from.x, from.y + 0.1, from.z, 8, 0.3, 0.1, 0.3, 0.02);

        Component name = Component.translatable("item.guhs." + soort.id()).withStyle(soort.colour);
        player.displayClientMessage(Component.translatable(soort.isBad() ? "gui.guhs.vissen.caught_bad" : "gui.guhs.vissen.caught",
                name, VisSoort.kg(grams), (points >= 0 ? "+" : "") + points, s.points).withStyle(ChatFormatting.WHITE), true);
        switch (soort) {
            case MIKA_MEERVAL -> {
                sound(player, ModSounds.MIKA_AMBIENT.get(), 1f, 1.2f);
                world.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 2, player.getZ(), 3, 0.4, 0.2, 0.4, 0);
                player.sendSystemMessage(Component.translatable("quest.guhs.vissen.mika", -points).withStyle(ChatFormatting.RED));
            }
            case GUHPUFFER -> {
                sound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 1f, 1.2f);
                world.sendParticles(ParticleTypes.CLOUD, from.x, from.y + 0.5, from.z, 10, 0.3, 0.3, 0.3, 0.02);
            }
            case GOUDEN_GUHVIS -> {
                GuhAdvancements.grant(player, "vissen_goud");
                sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                title(player, Component.translatable("gui.guhs.vissen.golden_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.vissen.golden_subtitle", VisSoort.kg(grams)), 40);
                tell(npc, Component.translatable("quest.guhs.vissen.golden", player.getDisplayName(), VisSoort.kg(grams)).withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.translatable("quest.guhs.vissen.golden_nr", golden(player)).withStyle(ChatFormatting.YELLOW));
            }
            case NJEGFOREL -> sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.3f);
            default -> sound(player, SoundEvents.FISH_SWIM, 0.8f, 1.2f);
        }
        if (!soort.isBad()) {
            sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 0.9f + Math.min(0.8f, points / 100f));
        }
        // the Visboek: every species once
        CompoundTag saved = GuhQuests.saved(player);
        int mask = saved.getInt(SPECIES) | 1 << soort.ordinal();
        saved.putInt(SPECIES, mask);
        if (mask == (1 << VisSoort.values().length) - 1) {
            GuhAdvancements.grant(player, "vissen_alle_soorten");
        }
        // your heaviest fish ever, and the pond's
        if (!soort.isBad() && grams > saved.getInt(HEAVIEST)) {
            boolean first = !saved.contains(HEAVIEST);
            saved.putInt(HEAVIEST, grams);
            saved.putString(HEAVIEST_SOORT, soort.id());
            if (!first) {
                player.sendSystemMessage(Component.translatable("quest.guhs.vissen.heaviest", name, VisSoort.kg(grams)).withStyle(ChatFormatting.YELLOW));
            }
        }
        if (!soort.isBad()) {
            List<Scorebord.Entry> heaviest = Scorebord.top(world.getServer(), niveau.board(BOARD_HEAVIEST));
            if (heaviest.isEmpty() || grams > heaviest.get(0).score()) {
                tell(npc, Component.translatable("quest.guhs.vissen.pond_record", player.getDisplayName(), name, VisSoort.kg(grams)).withStyle(ChatFormatting.YELLOW));
            }
            if (Scorebord.submit(player, niveau.board(BOARD_HEAVIEST), grams, false) > 0) {
                showScores(npc);
            }
        }
        updateBar(player, world.getGameTime());
        return new Catch(soort, grams, points);
    }

    // --- records ------------------------------------------------------------------------------------------------------

    private static final String BEST = "guhs_vissen_best", HEAVIEST = "guhs_vissen_heaviest", HEAVIEST_SOORT = "guhs_vissen_heaviest_soort",
            GAMES = "guhs_vissen_games", FIRST = "guhs_vissen_first", SPECIES = "guhs_vissen_species", GOLDEN = "guhs_vissen_golden";

    /** Your best contest total on medium (0 = none yet). */
    public static int best(ServerPlayer player) {
        return GuhQuests.saved(player).getInt(BEST);
    }

    /** Your best contest total on this level (medium = the old record; 0 = none yet). */
    public static int best(ServerPlayer player, Niveau niveau) {
        return GuhQuests.saved(player).getInt(Klassiekers.sleutel(BEST, niveau));
    }

    public static int heaviest(ServerPlayer player) {
        return GuhQuests.saved(player).getInt(HEAVIEST);
    }

    /** How many Gouden Guhvissen you ever caught. */
    public static int golden(ServerPlayer player) {
        return GuhQuests.saved(player).getInt(GOLDEN);
    }

    public static int games(ServerPlayer player) {
        return GuhQuests.saved(player).getInt(GAMES);
    }

    public static int bonnen(int points) {
        return points <= 0 ? 0 : Math.min(MAX_BONNEN, 2 + points / POINTS_PER_BON);
    }

    /** Visbonnen when you stop before the end: one less than fishing to the end (the extra one is for finishing). */
    public static int earlyBonnen(int points) {
        return points <= 0 ? 0 : Math.min(MAX_BONNEN, 1 + points / POINTS_PER_BON);
    }

    /** A top-3 board for the Visguh's screen: a list of {Name, Points}. */
    private static ListTag board(GuhNpcEntity npc, String board) {
        ListTag list = new ListTag();
        for (Scorebord.Entry e : Scorebord.top(npc.getServer(), board)) {
            CompoundTag t = new CompoundTag();
            t.putString("Name", e.name());
            t.putInt("Points", e.score());
            list.add(t);
        }
        return list;
    }

    /**
     * The world's top 3 (best contest totals and heaviest fish) floats above the record board at the Visguh's desk (the
     * wall with the sign "~ Visrecords ~"; remembered once found, looked for again until then: the pond may be turned any
     * way, or the sign's chunk not loaded yet), or above the Visguh while there's no such board.
     */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        CompoundTag data = npc.roleData;
        if (!data.getBoolean("Board")) {
            BlockPos found = recordSign(world, npc.blockPosition());
            if (found != null) {
                world.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(2, 3, 2),
                        d -> d.getTags().contains(Scorebord.TAG)).forEach(net.minecraft.world.entity.Entity::discard);   // (the one above the Visguh goes)
                data.putBoolean("Board", true);
                data.putInt("BoardX", found.getX());
                data.putInt("BoardY", found.getY());
                data.putInt("BoardZ", found.getZ());
            }
        }
        Vec3 pos = data.getBoolean("Board")
                ? new Vec3(data.getInt("BoardX") + 0.5, data.getInt("BoardY") + 2.7, data.getInt("BoardZ") + 0.5)
                : npc.position().add(0, 2.4, 0);
        // two boards side by side (2.9: every level has its own top 3): the most points, and beside it the heaviest fish
        net.minecraft.core.Direction side = net.minecraft.core.Direction.EAST;
        if (data.getBoolean("Board")) {
            var state = world.getBlockState(new BlockPos(data.getInt("BoardX"), data.getInt("BoardY"), data.getInt("BoardZ")));
            if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
                side = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getClockWise();
            }
        }
        var server = world.getServer();
        Scorebord.show(world, pos, "vissen", Klassiekers.bord(server, Component.translatable("gui.guhs.scorebord.vissen")
                .append(Component.literal(": ")).append(Component.translatable("gui.guhs.scorebord.vissen.punten")), BOARD_POINTS, p -> p + " punten"));
        Vec3 beside = pos.add(side.getStepX() * 3.6, 0, side.getStepZ() * 3.6);
        Scorebord.show(world, beside, "vissen_zwaarste", Klassiekers.bord(server, Component.translatable("gui.guhs.scorebord.vissen.zwaarste"),
                BOARD_HEAVIEST, VisSoort::kg));
    }

    /** The "~ Visrecords ~" sign within 18 blocks (any direction) of the Visguh, in loaded chunks (or null). */
    @Nullable
    private static BlockPos recordSign(ServerLevel world, BlockPos c) {
        BlockPos best = null;
        for (int cx = (c.getX() - SIGN_REACH) >> 4; cx <= (c.getX() + SIGN_REACH) >> 4; cx++) {
            for (int cz = (c.getZ() - SIGN_REACH) >> 4; cz <= (c.getZ() + SIGN_REACH) >> 4; cz++) {
                if (!(world.getChunkSource().getChunkNow(cx, cz) instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (be instanceof SignBlockEntity sign && Math.abs(pos.getX() - c.getX()) <= SIGN_REACH && Math.abs(pos.getZ() - c.getZ()) <= SIGN_REACH
                            && pos.getY() - c.getY() >= -3 && pos.getY() - c.getY() <= 5
                            && sign.getFrontText().getMessage(0, false).getString().contains("Visrecords")
                            && (best == null || pos.distSqr(c) < best.distSqr(c))) {
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    // --- the loaned rod -------------------------------------------------------------------------------------------------

    /** Puts a Guhvis-hengel in your hand (or pockets, or other hand). False when there's really no room. */
    static boolean giveRod(ServerPlayer player) {
        if (rods(player) > 0) {
            return true;
        }
        ItemStack rod = new ItemStack(VissenFeature.GUHVIS_HENGEL.get());
        Inventory inv = player.getInventory();
        if (inv.getSelected().isEmpty()) {
            inv.setItem(inv.selected, rod);
            return true;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {         // an empty hotbar slot becomes the selected one
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, rod);
                inv.selected = i;
                if (player.connection != null) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(i));
                }
                return true;
            }
        }
        int free = inv.getFreeSlot();                                    // hotbar full: your own item moves to the backpack
        if (free >= 0) {
            inv.setItem(free, inv.getSelected());
            inv.setItem(inv.selected, rod);
            return true;
        }
        if (player.getOffhandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, rod);
            return true;
        }
        return false;
    }

    /** How many loaned rods this player has (inventory, cursor, open container). */
    public static int rods(ServerPlayer player) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof GuhvisHengel) {
                n++;
            }
        }
        return n + (player.containerMenu.getCarried().getItem() instanceof GuhvisHengel ? 1 : 0);
    }

    /** Back to the Visguh with every loaned rod (and the line is reeled in). */
    public static void takeRods(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof GuhvisHengel) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        for (var menu : List.of(player.containerMenu, player.inventoryMenu)) {
            for (Slot slot : menu.slots) {
                if (slot.getItem().getItem() instanceof GuhvisHengel) {
                    slot.set(ItemStack.EMPTY);
                }
            }
        }
        if (player.containerMenu.getCarried().getItem() instanceof GuhvisHengel) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        if (player.fishing != null) {
            player.fishing.discard();
        }
    }

    // --- events -----------------------------------------------------------------------------------------------------------

    /** Anglers can't get hurt (falling off the pier, a stray arrow...). */
    /** Is this angler at the pond (only there can't they get hurt)? */
    static boolean atThePond(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        if (world.dimension() == ModDimensions.GUHMENSION) {
            return VissenProtection.inVijver(world, player.blockPosition());
        }
        UUID npc = ANGLERS.get(player.getUUID());
        return npc != null && world.getEntity(npc) instanceof GuhNpcEntity visguh && visguh.distanceToSqr(player) <= POND_RADIUS * POND_RADIUS;
    }

    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isFishing(player) && atThePond(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** The rod can't be thrown away: it jumps back into your pockets. */
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (stack.getItem() instanceof GuhvisHengel) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player && isFishing(player) && rods(player) == 0) {
                player.getInventory().add(stack.copy());
            }
        }
    }

    /** A loaned rod put in a barrel, a chest, a guh's backpack...: gone when the container closes (it can't be stored). */
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean took = false;
        for (Slot slot : event.getContainer().slots) {
            if (slot.container != player.getInventory() && slot.getItem().getItem() instanceof GuhvisHengel) {
                slot.set(ItemStack.EMPTY);
                took = true;
            }
        }
        if (took && isFishing(player)) {
            giveRod(player);
            player.displayClientMessage(Component.translatable("gui.guhs.vissen.no_drop").withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** The rod can't be hung in an item frame, put on an armor stand or given to an allay (they'd keep it). */
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (keepsItems(event.getTarget()) && event.getItemStack().getItem() instanceof GuhvisHengel) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onInteractEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (keepsItems(event.getTarget()) && event.getItemStack().getItem() instanceof GuhvisHengel) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static boolean keepsItems(Entity target) {
        return target instanceof ItemFrame || target instanceof ArmorStand || target instanceof Allay;
    }

    /** ...and it doesn't go into a decorated pot either (right-clicking one with an item puts it in). */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof GuhvisHengel
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DecoratedPotBlock) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            leave(player);
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ANGLERS.containsKey(player.getUUID())) {
            leave(player);
        }
    }

    /** (Just in case: a loaned rod never drops.) */
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> item.getItem().getItem() instanceof GuhvisHengel);
    }

    /** Anglers whose Visguh stopped ticking (unloaded, gone) drop out; leftover rods of old contests go. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        UUID npc = ANGLERS.get(player.getUUID());
        if (npc == null) {
            return;
        }
        VisWedstrijd contest = CONTESTS.get(npc);
        if (contest == null || player.serverLevel().getGameTime() - contest.lastTick > 60) {
            player.sendSystemMessage(Component.translatable("quest.guhs.vissen.stopped").withStyle(ChatFormatting.AQUA));
            leave(player);
        }
    }

    // --- little helpers ------------------------------------------------------------------------------------------------

    private void updateBar(ServerPlayer player, long now) {
        ServerBossEvent bar = bars.get(player.getUUID());
        Score s = scores.get(player.getUUID());
        if (bar == null || s == null) {
            return;
        }
        if (now < startTick) {
            bar.setName(Component.translatable("gui.guhs.vissen.bar_countdown", (startTick - now + 19) / 20));
            bar.setProgress(1f);
        } else {
            int left = ticksLeft(now);
            bar.setName(Component.translatable("gui.guhs.vissen.bar", time(left), s.points, s.fish).append("  ").append(Klassiekers.naam(niveau)));
            bar.setProgress(Math.max(0f, Math.min(1f, left / (float) DURATION)));
            bar.setColor(left <= 20 * 30 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PINK);
        }
    }

    /** Anglers don't get hungrier or weaker than at the start, and never run out of air. */
    private static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);    // (no hungrier or weaker than at the start: no free healing)
    }

    private void tell(GuhNpcEntity npc, Component message) {
        online((ServerLevel) npc.level()).forEach(p -> p.sendSystemMessage(message));
    }

    private List<ServerPlayer> online(ServerLevel world) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : scores.keySet()) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    private static void title(ServerPlayer player, Component title, Component subtitle, int stay) {
        if (player.connection == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, stay, 8));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }

    public static String time(int ticks) {
        int s = (ticks + 19) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    // --- for the GameTests ------------------------------------------------------------------------------------------------

    /** Skips the countdown (tests). */
    public static void skipCountdown(GuhNpcEntity npc) {
        VisWedstrijd contest = of(npc);
        if (contest != null) {
            contest.startTick = npc.level().getGameTime();
            contest.endTick = contest.startTick + DURATION;
        }
    }

    /** Lets the time run out now (tests). */
    public static void endNow(GuhNpcEntity npc) {
        VisWedstrijd contest = of(npc);
        if (contest != null) {
            contest.finish(npc);
        }
    }
}
