package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Verstopguh (hide-and-seek) in the verstopguh house. Verstopguhtje on the roof hides 5 / 8 / 12 guhs (1.5 / 1 / 0.5
 * blocks long) on random spots of the house (the invisible "verstopplek" markers) and sends the seekers inside. Right-click
 * a hidden guh to find it. The game ends when all are found (tickets for everyone, a record when you played alone) or when
 * the last seeker walks out through the exit. Others can join a running game; then it doesn't count for records.
 * <p>
 * The state lives in Verstopguhtje (saved with her), so a game survives a restart.
 */
public final class VerstopGame {
    /** Extra tickets for finding them all quickly (within the level's bonusTicks). */
    public static final int QUICK_BONUS = 2;

    public enum Level {
        MAKKELIJK(5, 1.5f, 2, 3, 20 * 120),
        MEDIUM(8, 1.0f, 3, 2, 20 * 240),
        MOEILIJK(12, 0.5f, 5, 1, 20 * 360);

        public final int guhs;
        /** Length of the hidden guhs in blocks (a guh of scale 1 is 1.45 blocks long). */
        public final float length;
        public final int tickets;
        /** How often the hidden guhs make their soft guh noises (GuhEntity sound frequency). */
        public final int soundFrequency;
        /** Find them all within this many ticks for a bonus ticket. */
        public final int bonusTicks;

        Level(int guhs, float length, int tickets, int soundFrequency, int bonusTicks) {
            this.guhs = guhs;
            this.length = length;
            this.tickets = tickets;
            this.soundFrequency = soundFrequency;
            this.bonusTicks = bonusTicks;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Actions from the screen. */
    public static final int START = 0, JOIN = 3, SHOP = 4;
    /** How far from Verstopguhtje the house reaches (it's 80x80, she stands in the middle of the roof). */
    public static final int HOUSE_RADIUS = 42, HOUSE_DEPTH = 16;
    private static final GuhVariant[] HIDERS = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};

    /**
     * Everyone searching in any verstopguh house right now (player -> their Verstopguhtje): they can't get hurt, hungrier or
     * out of breath. A seeker whose game is gone (logged out, died, left, the house stopped ticking) drops out, see
     * {@link #onPlayerTick}.
     */
    private static final java.util.Map<UUID, UUID> SEEKERS = new java.util.concurrent.ConcurrentHashMap<>();
    /** A game whose Verstopguhtje hasn't ticked for this long doesn't protect its seekers any more. */
    private static final int STALE_TICKS = 60;

    public static boolean isSeeking(net.minecraft.world.entity.player.Player player) {
        return SEEKERS.containsKey(player.getUUID());
    }

    @Nullable
    private Level level;
    private long startTick;
    /** The game time Verstopguhtje last ticked this game. */
    private long lastTick;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final List<UUID> hidden = new ArrayList<>();
    private int found;
    private boolean together;
    /** Hiding spots and the start, found once by scanning the house. */
    @Nullable
    private List<BlockPos> spots;
    @Nullable
    private BlockPos start;
    /** The lampgions inside the house (for the "lights out" hint). */
    @Nullable
    private List<BlockPos> lamps;
    /** The windows and the one-way glass of the house (they turn to tinted glass with the "lights out" hint). */
    @Nullable
    private List<BlockPos> glass;

    // hints: every 30-40 seconds a new one, and they stay for the rest of the game
    public static final int HINT_MIN = 20 * 30, HINT_RANDOM = 20 * 10;
    private long nextHint;
    private int hints;
    /** Hidden guhs that keep sniffing (seekers hear them). */
    private final List<UUID> sniffers = new ArrayList<>();
    /** Lampgions switched off by a hint, with the lamp that was there (put back when the game ends). */
    private final java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> lampsOff = new java.util.LinkedHashMap<>();

    public boolean isRunning() {
        return level != null;
    }

    @Nullable
    public Level level() {
        return level;
    }

    public int found() {
        return found;
    }

    public boolean isPlaying(ServerPlayer player) {
        return players.contains(player.getUUID());
    }

    // --- talking to Verstopguhtje ------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        VerstopGame game = npc.verstop;
        GuhQuests.say(player, npc, game.isRunning() ? "quest.guhs.verstop.running" : "quest.guhs.verstop.hello");
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        if (game.isRunning()) {
            data.putString("Level", game.level.id());
            data.putInt("Found", game.found);
            data.putInt("Total", game.level.guhs);
            data.putInt("Players", game.players.size());
        }
        for (Level l : Level.values()) {
            data.putInt("Best_" + l.id(), best(player, l));
        }
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new nl.juiced.guhs.network.MaagPayloads.VerstopOpen(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.VERSTOPGUHTJE || player.distanceToSqr(npc) > 64) {
            return;
        }
        VerstopGame game = npc.verstop;
        if (action == SHOP) {
            npc.openShop(player);
        } else if (!game.isPlaying(player) && (isSeeking(player) || nl.juiced.guhs.feature.Minigames.busyElsewhere(player, nl.juiced.guhs.feature.Minigames.VERSTOP))) {
            GuhQuests.say(player, npc, "quest.guhs.minigame.busy");   // (still in another game, or seeking in another house)
        } else if (game.isRunning()) {
            game.join(npc, player);
        } else if (action >= START && action < START + Level.values().length) {
            game.start(npc, player, Level.values()[action - START]);
        }
    }

    // --- playing -----------------------------------------------------------------------------------------------------

    private void start(GuhNpcEntity npc, ServerPlayer player, Level newLevel) {
        ServerLevel world = (ServerLevel) npc.level();
        findMarkers(npc);
        if (start == null || spots.size() < newLevel.guhs) {
            GuhQuests.say(player, npc, "quest.guhs.verstop.broken");
            return;
        }
        List<BlockPos> free = new ArrayList<>(spots);
        java.util.Collections.shuffle(free, new java.util.Random(world.getRandom().nextLong()));
        hidden.clear();
        for (int i = 0; i < newLevel.guhs; i++) {
            BlockPos spot = free.get(i);
            GuhEntity guh = ModEntities.GUH.get().create(world);
            if (guh == null) {
                continue;
            }
            guh.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
            guh.finalizeSpawn(world, world.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
            guh.setVariant(HIDERS[world.getRandom().nextInt(HIDERS.length)]);
            guh.setGuhScale(newLevel.length / 1.45f);
            guh.setHiddenBy(npc.getUUID(), newLevel.soundFrequency);
            world.addFreshEntity(guh);
            hidden.add(guh.getUUID());
        }
        level = newLevel;
        found = 0;
        together = false;
        tipped = false;
        lastTip = Long.MIN_VALUE / 2;
        ensureTipguh(npc, world);
        startTick = world.getGameTime();
        lastTick = startTick;
        nextHint = startTick + HINT_MIN + world.getRandom().nextInt(HINT_RANDOM);
        hints = 0;
        sniffers.clear();
        players.clear();
        join(npc, player);
    }

    private void join(GuhNpcEntity npc, ServerPlayer player) {
        if (!players.add(player.getUUID())) {
            return;
        }
        SEEKERS.put(player.getUUID(), npc.getUUID());
        nl.juiced.guhs.feature.Minigames.startKeeping(player);
        if (players.size() > 1) {
            together = true; // searching together: fun, but no records
        }
        player.teleportTo((ServerLevel) npc.level(), start.getX() + 0.5, start.getY(), start.getZ() + 0.5, player.getYRot(), 0);
        player.sendSystemMessage(Component.translatable("quest.guhs.verstop.go", level.guhs - found, Component.translatable(
                "gui.guhs.verstop." + level.id())).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (players.size() > 1) {
            tell(npc, Component.translatable("quest.guhs.verstop.joined", player.getDisplayName()));
        }
    }

    /** Someone right-clicked a hidden guh. */
    public static void foundGuh(GuhEntity guh, ServerPlayer player) {
        if (!(guh.level() instanceof ServerLevel world) || !(world.getEntity(guh.getHiddenBy()) instanceof GuhNpcEntity npc)) {
            return;
        }
        VerstopGame game = npc.verstop;
        if (!game.isPlaying(player) || !game.hidden.remove(guh.getUUID())) {
            player.displayClientMessage(Component.translatable("quest.guhs.verstop.not_playing").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        world.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, 0.55f, 0.8f), 1.2f),
                guh.getX(), guh.getY() + 0.3, guh.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        soundForSeekers(guh, ModSounds.GUH_HAPPY.get(), 1f, 1.4f);
        guh.discard();
        game.found++;
        game.tell(npc, Component.translatable("quest.guhs.verstop.found", player.getDisplayName(), game.found, game.level.guhs));
        if (game.found >= game.level.guhs) {
            game.finish(npc);
        }
    }

    private void finish(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        int ticks = (int) (world.getGameTime() - startTick);
        for (ServerPlayer player : online(world)) {
            int tickets = level.tickets + (ticks <= level.bonusTicks ? QUICK_BONUS : 0);
            give(player, new ItemStack(ModItems.VERSTOPGUHTICKET.get(), tickets));
            player.sendSystemMessage(Component.translatable("quest.guhs.verstop.won", time(ticks), tickets).withStyle(ChatFormatting.GOLD));
            if (tipped) {
                player.sendSystemMessage(Component.translatable("quest.guhs.verstop.tip.norecord").withStyle(ChatFormatting.GRAY));
            } else if (!together) {
                int best = best(player, level);
                if (best < 0 || ticks < best) {
                    GuhQuests.saved(player).putInt("guhs_verstop_best_" + level.id(), ticks);
                    player.sendSystemMessage(Component.translatable("quest.guhs.verstop.record", time(ticks)).withStyle(ChatFormatting.YELLOW));
                }
                Scorebord.submit(player, "verstop_" + level.id(), ticks, true);
            }
            GuhAdvancements.grant(player, "verstop_" + level.id());
            world.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
            backToRoof(npc, player);
        }
        reset(npc);
    }

    // --- the Tipguh: in the hall; once a minute it makes a guh light up (and then the time doesn't count for records) ---

    /** Tip time: once a minute per game; how long the guh lights up. */
    public static final int TIP_COOLDOWN = 20 * 60, TIP_GLOW = 20 * 10, CONFIRM_TICKS = 20 * 10;
    private boolean tipped;
    private long lastTip = Long.MIN_VALUE / 2;
    /** Who clicked the Tipguh once (and when): the second click within 10 s gets the tip. */
    private final java.util.Map<UUID, Long> confirming = new java.util.HashMap<>();

    public boolean isTipped() {
        return tipped;
    }

    /** Older houses have no Tipguh yet: the first game puts one next to the start. */
    private void ensureTipguh(GuhNpcEntity npc, ServerLevel world) {
        if (!world.getEntitiesOfClass(GuhNpcEntity.class, new AABB(start).inflate(HOUSE_RADIUS, 4, HOUSE_RADIUS),
                n -> n.getKind() == GuhNpcEntity.Kind.TIPGUH).isEmpty()) {
            return;
        }
        for (int[] d : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {3, 0}, {0, 3}}) {
            BlockPos pos = start.offset(d[0], 0, d[1]);
            if (world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above()) && !world.isEmptyBlock(pos.below())) {
                GuhNpcEntity tip = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(world);
                if (tip != null) {
                    tip.setKind(GuhNpcEntity.Kind.TIPGUH);
                    tip.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0);
                    world.addFreshEntity(tip);
                }
                return;
            }
        }
    }

    /** Someone right-clicked the Tipguh. */
    public static void askTip(GuhNpcEntity tipguh, ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        List<GuhNpcEntity> hosts = world.getEntitiesOfClass(GuhNpcEntity.class, new AABB(tipguh.blockPosition()).inflate(HOUSE_RADIUS + 8, HOUSE_DEPTH + 8, HOUSE_RADIUS + 8),
                n -> n.getKind() == GuhNpcEntity.Kind.VERSTOPGUHTJE);
        if (hosts.isEmpty() || !hosts.get(0).verstop.isRunning() || !hosts.get(0).verstop.isPlaying(player)) {
            GuhQuests.say(player, tipguh, "quest.guhs.verstop.tip.nogame");
            return;
        }
        GuhNpcEntity host = hosts.get(0);
        VerstopGame game = host.verstop;
        long now = world.getGameTime();
        long wait = game.lastTip + TIP_COOLDOWN - now;
        if (wait > 0) {
            GuhQuests.say(player, tipguh, "quest.guhs.verstop.tip.wait", (wait + 19) / 20);
            return;
        }
        Long asked = game.confirming.get(player.getUUID());
        if (!game.tipped && (asked == null || now - asked > CONFIRM_TICKS)) {
            game.confirming.put(player.getUUID(), now);
            GuhQuests.say(player, tipguh, "quest.guhs.verstop.tip.confirm");
            return;
        }
        List<GuhEntity> guhs = game.hiders(world);
        if (guhs.isEmpty()) {
            return;
        }
        GuhEntity guh = guhs.get(world.getRandom().nextInt(guhs.size()));
        guh.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, TIP_GLOW, 0, false, false));
        game.tipped = true;
        game.lastTip = now;
        game.confirming.clear();
        game.tell(host, Component.translatable("quest.guhs.verstop.tip.given", player.getDisplayName()));
    }

    /** Someone stepped on the way out. */
    public static void walkOut(ServerPlayer player, BlockPos exit) {
        ServerLevel world = player.serverLevel();
        List<GuhNpcEntity> npcs = world.getEntitiesOfClass(GuhNpcEntity.class, new AABB(exit).inflate(HOUSE_RADIUS + 8, HOUSE_DEPTH + 8, HOUSE_RADIUS + 8),
                n -> n.getKind() == GuhNpcEntity.Kind.VERSTOPGUHTJE);
        if (npcs.isEmpty()) {
            return;
        }
        GuhNpcEntity npc = npcs.get(0);
        VerstopGame game = npc.verstop;
        SEEKERS.remove(player.getUUID());
        if (game.players.remove(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("quest.guhs.verstop.stopped", game.found, game.level.guhs).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (game.players.isEmpty()) {
                game.reset(npc);
            }
        }
        backToRoof(npc, player);
    }

    /** Every second: the timer for the seekers, and seekers who are gone (logged out, died, far away) drop out. */
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 == 1) {
            showScores(npc);
        }
        if (!isRunning() || npc.tickCount % 20 != 0) {
            return;
        }
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        players.removeIf(id -> {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
            boolean gone = p == null || !p.isAlive() || p.level() != world || p.distanceToSqr(npc) > (HOUSE_RADIUS + 30) * (HOUSE_RADIUS + 30);
            if (gone) {
                SEEKERS.remove(id, npc.getUUID());
            } else {
                SEEKERS.put(id, npc.getUUID());           // (logged out and back in, in the house: protected again)
            }
            return gone;
        });
        if (players.isEmpty()) {
            reset(npc);
            return;
        }
        Component bar = Component.translatable("quest.guhs.verstop.bar", Component.translatable("gui.guhs.verstop." + level.id()), found,
                level.guhs, time((int) (world.getGameTime() - startTick))).withStyle(ChatFormatting.LIGHT_PURPLE);
        online(world).forEach(p -> {
            p.displayClientMessage(bar, true);
            refresh(p);
        });
        hidden.removeIf(id -> world.getEntity(id) == null && world.isLoaded(npc.blockPosition())); // (lost somehow: don't wait for it forever)
        if (world.getGameTime() >= nextHint) {
            giveHint(npc, world);
            nextHint = world.getGameTime() + HINT_MIN + world.getRandom().nextInt(HINT_RANDOM);
        }
        if (npc.tickCount % 60 == 0) {                          // the sniffers sniff
            sniffers.removeIf(id -> !hidden.contains(id));
            for (UUID id : sniffers) {
                if (world.getEntity(id) instanceof GuhEntity guh) {
                    soundForSeekers(guh, SoundEvents.FOX_SNIFF, 0.8f, 1.4f + world.getRandom().nextFloat() * 0.2f);
                }
            }
        }
        if (hidden.isEmpty() && found < level.guhs) {
            found = level.guhs;
            finish(npc);
        }
    }

    /** Ends the game: hidden guhs that weren't found go home. */
    private void reset(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lampsOff.forEach((pos, lamp) -> {                           // the lights go back on
            if (world.getBlockState(pos).is(ModBlocks.LAMPION_UIT.get()) || world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TINTED_GLASS)) {
                world.setBlock(pos, lamp, 3);
            }
        });
        lampsOff.clear();
        sniffers.clear();
        for (UUID id : hidden) {
            if (world.getEntity(id) instanceof GuhEntity guh) {
                guh.discard();
            }
        }
        hidden.clear();
        players.forEach(id -> SEEKERS.remove(id, npc.getUUID()));
        players.clear();
        level = null;
        found = 0;
    }

    // --- hints ------------------------------------------------------------------------------------------------------

    /** Chat (where a guh still is), lights out (a room without guhs), a sniffing guh: in turn, skipping what can't be done. */
    public void giveHint(GuhNpcEntity npc, ServerLevel world) {
        for (int tries = 0; tries < 3; tries++) {
            int kind = hints++ % 3;
            if (kind == 0 ? chatHint(npc, world) : kind == 1 ? lightsHint(npc, world) : sniffHint(npc, world)) {
                return;
            }
        }
    }

    private List<GuhEntity> hiders(ServerLevel world) {
        List<GuhEntity> list = new ArrayList<>();
        for (UUID id : hidden) {
            if (world.getEntity(id) instanceof GuhEntity guh) {
                list.add(guh);
            }
        }
        return list;
    }

    private boolean chatHint(GuhNpcEntity npc, ServerLevel world) {
        List<GuhEntity> guhs = hiders(world);
        if (guhs.isEmpty()) {
            return false;
        }
        GuhEntity guh = guhs.get(world.getRandom().nextInt(guhs.size()));
        BlockPos c = npc.blockPosition(), p = guh.blockPosition();
        String floor = p.getY() - c.getY() <= -8 ? "beneden" : "boven";
        int dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
        String ns = dz < -13 ? "noord" : dz > 13 ? "zuid" : "", ew = dx < -13 ? "west" : dx > 13 ? "oost" : "";
        String where = ns.isEmpty() && ew.isEmpty() ? "midden" : ns.isEmpty() ? ew : ew.isEmpty() ? ns : ns + ew;
        tell(npc, Component.translatable("quest.guhs.verstop.hint.floor", Component.translatable("quest.guhs.verstop.hint." + floor),
                Component.translatable("quest.guhs.verstop.hint." + where)));
        return true;
    }

    /** Which room of the house a spot is in: floor and one of the 3x3 rooms. */
    private static int room(GuhNpcEntity npc, BlockPos p) {
        BlockPos c = npc.blockPosition();
        int col = net.minecraft.util.Mth.clamp((p.getX() - c.getX() + 40) * 3 / 80, 0, 2);
        int row = net.minecraft.util.Mth.clamp((p.getZ() - c.getZ() + 40) * 3 / 80, 0, 2);
        return (p.getY() - c.getY() <= -8 ? 0 : 9) + row * 3 + col;
    }

    private boolean lightsHint(GuhNpcEntity npc, ServerLevel world) {
        findMarkers(npc);
        Set<Integer> occupied = new java.util.HashSet<>();
        hiders(world).forEach(g -> occupied.add(room(npc, g.blockPosition())));
        java.util.Map<Integer, List<BlockPos>> rooms = new java.util.TreeMap<>();
        for (BlockPos lamp : lamps) {
            int r = room(npc, lamp);
            if (!occupied.contains(r) && !lampsOff.containsKey(lamp) && world.getBlockState(lamp).getBlock() instanceof net.minecraft.world.level.block.LanternBlock
                    && !world.getBlockState(lamp).is(ModBlocks.LAMPION_UIT.get())) {
                rooms.computeIfAbsent(r, k -> new ArrayList<>()).add(lamp);
            }
        }
        if (rooms.isEmpty()) {
            return false;
        }
        List<Integer> choices = new ArrayList<>(rooms.keySet());
        int room = choices.get(world.getRandom().nextInt(choices.size()));
        for (BlockPos lamp : rooms.get(room)) {
            var state = world.getBlockState(lamp);
            lampsOff.put(lamp, state);
            world.setBlock(lamp, ModBlocks.LAMPION_UIT.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, state.getValue(net.minecraft.world.level.block.LanternBlock.HANGING)), 3);
        }
        // and its windows (and the glass roof above it) go dark, so you can see it in daylight too
        for (BlockPos pane : glass) {
            if (room(npc, pane) == room && !lampsOff.containsKey(pane)) {
                lampsOff.put(pane, world.getBlockState(pane));
                world.setBlock(pane, net.minecraft.world.level.block.Blocks.TINTED_GLASS.defaultBlockState(), 3);
            }
        }
        tell(npc, Component.translatable("quest.guhs.verstop.hint.lights"));
        return true;
    }

    private boolean sniffHint(GuhNpcEntity npc, ServerLevel world) {
        List<UUID> quiet = new ArrayList<>(hidden);
        quiet.removeAll(sniffers);
        if (quiet.isEmpty()) {
            return false;
        }
        sniffers.add(quiet.get(world.getRandom().nextInt(quiet.size())));
        tell(npc, Component.translatable("quest.guhs.verstop.hint.sniff"));
        return true;
    }

    /** For tests: the lampgions put out by hints so far. */
    public int lampsOff() {
        return lampsOff.size();
    }

    public int sniffers() {
        return sniffers.size();
    }

    /** Seekers never run out of food (so they can always sprint) or health. */
    private static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);    // (no hungrier or weaker than at the start: no free healing)
    }

    // --- seekers that are gone ---------------------------------------------------------------------------------------------

    /** Is this seeker's game still on, with them in it, near the house, and still ticking? */
    public static boolean stillSeeking(ServerPlayer player) {
        UUID npcId = SEEKERS.get(player.getUUID());
        if (npcId == null || !(player.serverLevel().getEntity(npcId) instanceof GuhNpcEntity npc)) {
            return false;
        }
        VerstopGame game = npc.verstop;
        return game.isRunning() && game.isPlaying(player) && player.serverLevel().getGameTime() - game.lastTick <= STALE_TICKS
                && player.distanceToSqr(npc) <= (HOUSE_RADIUS + 30) * (HOUSE_RADIUS + 30);
    }

    /** Every second: a seeker whose game is gone (another world, far away, the house stopped ticking) is an ordinary player again. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && isSeeking(player) && !stillSeeking(player)) {
            SEEKERS.remove(player.getUUID());
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SEEKERS.remove(event.getEntity().getUUID());
    }

    public static void onChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        SEEKERS.remove(event.getEntity().getUUID());
    }

    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        SEEKERS.remove(event.getEntity().getUUID());
    }

    /** The server stops (singleplayer: back to the title screen): nobody is seeking any more, in no world. */
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SEEKERS.clear();
    }

    /** No hurting seekers (fall damage, drowning in the bath, a stray arrow...). */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && isSeeking(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A sound only the seekers of this guh's game hear. */
    public static void soundForSeekers(GuhEntity guh, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (!(guh.level() instanceof ServerLevel world) || !(world.getEntity(guh.getHiddenBy()) instanceof GuhNpcEntity npc)) {
            return;
        }
        var packet = new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.NEUTRAL, guh.getX(), guh.getY(), guh.getZ(), volume, pitch, world.getRandom().nextLong());
        for (ServerPlayer p : npc.verstop.online(world)) {
            if (p.distanceToSqr(guh) < 24 * 24 && p.connection != null) {
                p.connection.send(packet);
            }
        }
    }

    /** Is this hidden guh still part of a game? (Leftovers of a game that ended while they weren't loaded go away.) */
    public boolean isHiding(UUID guh) {
        return hidden.contains(guh);
    }

    private void tell(GuhNpcEntity npc, Component message) {
        online((ServerLevel) npc.level()).forEach(p -> p.sendSystemMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private List<ServerPlayer> online(ServerLevel world) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : players) {
            ServerPlayer p = world.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    private static void backToRoof(GuhNpcEntity npc, ServerPlayer player) {
        net.minecraft.world.phys.Vec3 look = net.minecraft.world.phys.Vec3.directionFromRotation(0, npc.getYRot());
        player.teleportTo((ServerLevel) npc.level(), npc.getX() + look.x * 2.5, npc.getY(), npc.getZ() + look.z * 2.5, npc.getYRot() + 180, 0);
    }

    private void findMarkers(GuhNpcEntity npc) {
        if (spots != null && start != null && lamps != null && glass != null) {
            return;
        }
        spots = new ArrayList<>();
        lamps = new ArrayList<>();
        glass = new ArrayList<>();
        start = null;
        BlockPos c = npc.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-HOUSE_RADIUS, -HOUSE_DEPTH, -HOUSE_RADIUS), c.offset(HOUSE_RADIUS, 0, HOUSE_RADIUS))) {
            var state = npc.level().getBlockState(pos);
            if (state.is(ModBlocks.VERSTOPPLEK.get())) {
                spots.add(pos.immutable());
            } else if (state.is(ModBlocks.VERSTOPSTART.get())) {
                start = pos.immutable();
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.LanternBlock && pos.getY() < c.getY()) {
                lamps.add(pos.immutable());
            } else if ((state.is(net.minecraft.world.level.block.Blocks.PINK_STAINED_GLASS_PANE) || state.is(ModBlocks.EENRICHTINGSGLAS.get()))
                    && pos.getY() < c.getY()) {
                glass.add(pos.immutable());
            }
        }
    }

    /** The top 3 of each level floats above Verstopguhtje. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<String> boards = new java.util.ArrayList<>();
        List<Component> headings = new java.util.ArrayList<>();
        for (Level l : Level.values()) {
            boards.add("verstop_" + l.id());
            headings.add(Component.translatable("gui.guhs.verstop." + l.id()));
        }
        Scorebord.show(world, npc.position().add(0, 2.4, 0), "verstop",
                Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.verstop"), boards, headings, VerstopGame::time));
    }

    public static int best(ServerPlayer player, Level level) {
        CompoundTag data = GuhQuests.saved(player);
        String key = "guhs_verstop_best_" + level.id();
        return data.contains(key) ? data.getInt(key) : -1;
    }

    public static String time(int ticks) {
        int s = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }

    // --- saving (with Verstopguhtje) -----------------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (level != null) {
            tag.putString("Level", level.id());
            tag.putLong("Start", startTick);
            tag.putInt("Found", found);
            tag.putBoolean("Together", together);
            tag.putBoolean("Tipped", tipped);
            tag.putLong("LastTip", lastTip);
            ListTag p = new ListTag();
            players.forEach(id -> p.add(NbtUtils.createUUID(id)));
            tag.put("Players", p);
            ListTag h = new ListTag();
            hidden.forEach(id -> h.add(NbtUtils.createUUID(id)));
            tag.put("Hidden", h);
        }
        tag.putLong("NextHint", nextHint);
        tag.putInt("Hints", hints);
        ListTag sn = new ListTag();
        sniffers.forEach(id -> sn.add(NbtUtils.createUUID(id)));
        tag.put("Sniffers", sn);
        ListTag off = new ListTag();
        lampsOff.forEach((pos, lamp) -> {
            CompoundTag l = new CompoundTag();
            l.putLong("Pos", pos.asLong());
            l.put("State", NbtUtils.writeBlockState(lamp));
            off.add(l);
        });
        tag.put("LampsOff", off);
        if (lamps != null && glass != null) {
            tag.put("Lamps", new LongArrayTag(lamps.stream().mapToLong(BlockPos::asLong).toArray()));
            tag.put("Glass", new LongArrayTag(glass.stream().mapToLong(BlockPos::asLong).toArray()));
        }
        if (spots != null && start != null) {
            tag.put("Spots", new LongArrayTag(spots.stream().mapToLong(BlockPos::asLong).toArray()));
            tag.putLong("StartPos", start.asLong());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        level = null;
        for (Level l : Level.values()) {
            if (l.id().equals(tag.getString("Level"))) {
                level = l;
            }
        }
        startTick = tag.getLong("Start");
        found = tag.getInt("Found");
        together = tag.getBoolean("Together");
        tipped = tag.getBoolean("Tipped");
        lastTip = tag.contains("LastTip") ? tag.getLong("LastTip") : Long.MIN_VALUE / 2;
        players.clear();
        tag.getList("Players", Tag.TAG_INT_ARRAY).forEach(t -> players.add(NbtUtils.loadUUID(t)));
        // (the seekers get their protection back on the next tick, if they are still there)
        hidden.clear();
        tag.getList("Hidden", Tag.TAG_INT_ARRAY).forEach(t -> hidden.add(NbtUtils.loadUUID(t)));
        nextHint = tag.getLong("NextHint");
        hints = tag.getInt("Hints");
        sniffers.clear();
        tag.getList("Sniffers", Tag.TAG_INT_ARRAY).forEach(t -> sniffers.add(NbtUtils.loadUUID(t)));
        lampsOff.clear();
        for (Tag t : tag.getList("LampsOff", Tag.TAG_COMPOUND)) {
            CompoundTag l = (CompoundTag) t;
            lampsOff.put(BlockPos.of(l.getLong("Pos")), NbtUtils.readBlockState(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), l.getCompound("State")));
        }
        if (tag.contains("Lamps") && tag.contains("Glass")) {
            lamps = new ArrayList<>();
            for (long l : tag.getLongArray("Lamps")) {
                lamps.add(BlockPos.of(l));
            }
            glass = new ArrayList<>();
            for (long l : tag.getLongArray("Glass")) {
                glass.add(BlockPos.of(l));
            }
        }
        if (tag.contains("Spots")) {
            spots = new ArrayList<>();
            for (long l : tag.getLongArray("Spots")) {
                spots.add(BlockPos.of(l));
            }
            start = BlockPos.of(tag.getLong("StartPos"));
        }
    }

    // --- the verstopguh house itself -------------------------------------------------------------------------------------

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> HOUSE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, nl.juiced.guhs.Guhs.id("verstopguh_huis"));

    /** No wild guhs pop up in (or on) the house: they'd get mixed up with the hidden ones. */
    public static boolean inHouse(ServerLevel world, BlockPos pos) {
        var structure = world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(HOUSE);
        return structure != null && world.structureManager().getStructureAt(pos, structure).isValid();
    }
}
