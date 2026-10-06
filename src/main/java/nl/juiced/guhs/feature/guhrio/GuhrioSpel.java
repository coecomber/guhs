package nl.juiced.guhs.feature.guhrio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Super Guhrio on the server: who is in which level ({@link Sessie}, one per player, nothing shared between players but
 * the level itself and its creatures), and everything that happens there.
 * <ul>
 *     <li>{@link #start}: a player enters the level of a start block: locked on the first lane, the player's game gets the
 *     lanes and switches to the side view. {@link #stop}: out again, for whatever reason ({@link Einde}); always puts
 *     jumping, size and the player's game back to normal.</li>
 *     <li>Every tick ({@link #tick}): the player must be on the lane's line (a little off: put back; far off, another
 *     dimension, dead: the level ends), falling under the lane or into sauce brings you back to your flag
 *     ({@link #terug}), the pieces you stand in get their turn ({@link GuhrioStuk#binnen}: flags, the flagpole, coins).</li>
 *     <li>What the player's game reports ({@link #actie}): a bump under a block, a coin, ducking on a pipe, a creature you
 *     landed on or that touched you. The server checks that it can be true and lets the piece or the creature answer.</li>
 *     <li>Nobody is ever hurt in a level: being touched costs your power-up, or else brings you back to your flag
 *     ({@link #geraakt}).</li>
 *     <li>A level in use ({@link Actief}) knows its pieces (found by walking the lanes once) and keeps its creatures
 *     alive ({@link GuhrioStuk#wek}); they are gone a few seconds after the last player left.</li>
 * </ul>
 * Per player forever ({@link #spaar}): the coins and the best time per level; what a whole castle keeps on top of that is
 * {@link GuhrioKasteel}.
 * <p>
 * bbq2 engine: a cutscene always wins from a level (the level stands still while {@code Cutscenes.bezig}), players are put
 * back with {@code Duwtje.terug} and are only "caught" when {@code Duwtje.mag} says so, the Vuurpeper throws knabbels
 * ({@link #gooi}), Guhshi carries you ({@link #zetGuhshi}), switches are per player ({@link #zetKanaal}), level coins only
 * count for your pocket the first time ({@link #muntVan}) and the three big vadsmunten of a level stay yours
 * ({@link #vadsmunt}).
 */
public final class GuhrioSpel {
    /** A higher jump and a little more weight while you are in a level (snappier than normal walking). */
    public static final Identifier SPRONG = Guhs.id("guhrio_sprong"), ZWAARTE = Guhs.id("guhrio_zwaarte"), GROOT = Guhs.id("guhrio_groot");
    public static final Identifier SNEL = Guhs.id("guhrio_snel");
    /**
     * The feel of a level (with client.BaanBesturing's numbers: a lighter rise while space is held, a heavier fall): jump
     * strength 0.66 instead of 0.42, gravity 0.10 instead of 0.08, walking a fifth faster. Worked out with the game's own
     * movement sums: a held jump rises 3.3 blocks, a tap 1.5; about 4.4 blocks far walking, 6.4 running.
     */
    public static final double SPRONG_ERBIJ = 0.24, ZWAARTE_ERBIJ = 0.02, GROOT_ERBIJ = 0.5, SNEL_ERBIJ = 0.2;
    /** Off the line by more than this: put back on it. By more than {@link #WEG}: something took you away, the level ends. */
    public static final double NAAST = 0.45, WEG = 4.0;
    /** How far a reported piece or creature may be from where the server has you. */
    public static final double BEREIK = 4.5;
    /** Ticks: down into a pipe (and up out of it), at the flagpole before you are let go, safe after losing a power-up. */
    public static final int PIJP_TICKS = 12, KLAAR_TICKS = 70, VEILIG_TICKS = 40;
    /** The Vuurpeper: knabbels in the air at once per player, ticks between two throws. */
    public static final int KNABBELS = 2, GOOI_RUST = 8;
    /** Guhshi's tongue: how far it reaches along the lane, ticks between two licks. */
    public static final double TONG = 4.0;
    public static final int TONG_RUST = 12;
    /** How long a timed switch stays on. */
    public static final int SCHAKEL_TICKS = 160;
    /** The switch channels of a level (per player). */
    public static final int KANALEN = 8;
    /** The mark (persistent data: the rider's UUID) of the Guhshi that carries a player in a level; never saved for long. */
    public static final String GUHSHI_TAG = "guhs_guhrio_guhshi";
    /** How big the Guhshi under a player is (a guh you can sit on). */
    public static final float GUHSHI_SCHAAL = 1.3f;
    /** In a level your food never drops under this (so you can always run). */
    public static final int RENNEN_ETEN = 8;
    /** How deep in the pipe you go (a player is 1.8 high). */
    public static final double PIJP_DIEP = 1.85;

    /** Why a level ended for a player. */
    public enum Einde {
        GESTOPT, KLAAR, WEG, DIMENSIE, DOOD, UITGELOGD
    }

    /**
     * What you carry: nothing, the Superknabbel (bigger, breaks bricks, one free touch) or the Vuurpeper (as big, and you
     * throw bouncing knabbels).
     */
    public enum Kracht {
        GEEN, SUPER, VUUR;

        public boolean groot() {
            return this != GEEN;
        }
    }

    /** One piece of a level in use: a Guhrio block in a lane. */
    public record Stuk(BlockPos pos, BlockState state, int baan, double s) {
    }

    /** A level somebody is playing: its pieces and its creatures. */
    public static final class Actief {
        public final GuhrioLevel.Geplaatst level;
        public final List<Stuk> stukken = new ArrayList<>();
        /** The creature that belongs to a piece (a Guhmba to its spot). */
        public final Map<BlockPos, Entity> wezens = new HashMap<>();
        /** Creatures without a spot of their own (thrown knabbels...): gone with the level too. */
        public final List<Entity> los = new ArrayList<>();
        /** Every piece's number in this level (its place in {@link #stukken}: the same for every copy of the level). */
        public final Map<BlockPos, Integer> nummer = new HashMap<>();
        int leeg;

        Actief(GuhrioLevel.Geplaatst level) {
            this.level = level;
        }

        String sleutel() {
            return sleutelVan(level.dimensie().identifier().toString(), level.anker());
        }
    }

    /** A player in a level. */
    public static final class Sessie {
        public final UUID speler;
        public final Actief actief;
        /** The lane you are on. */
        public int baan;
        /** Where you come back to: the lane and the spot (your last flag, or the start). */
        public int vlagBaan;
        public Vec3 vlag;
        @Nullable
        public BlockPos vlagPos;
        /** Coins of this run, ticks in the level. */
        public int munten, ticks;
        /** How far along your lane the server last saw you. */
        public double s;
        public Kracht kracht = Kracht.GEEN;
        /** You ride Guhshi (and the guh that shows it). */
        public boolean guhshi;
        @Nullable
        Entity guhshiDier;
        /** The switch channels that are on for you (bit k = channel k), and until which tick a timed one stays on (0: for good). */
        public int kanalen;
        final int[] kanaalTot = new int[KANALEN];
        /** The big vadsmunten of this level you have (bit = its number): this run or an earlier one. */
        public int vads;
        /** Your knabbels in the air; ticks until the next throw / lick. */
        int knabbels, gooiRust;
        /** The whole castle in one go: this level counts for it (see {@link GuhrioKasteel}). */
        boolean loop;
        /** Per piece: 0 as built; else what the piece made of it (a coin taken, a ?-block empty, a brick broken). */
        public final Map<BlockPos, Integer> staat = new HashMap<>();
        /** Per piece: the tick ({@link #ticks}) until which it does not listen to you again (a switch, a hint). */
        public final Map<BlockPos, Integer> rust = new HashMap<>();
        /** The pipe you are in, or null. */
        @Nullable
        Pijpreis pijp;
        /** Counting down at the flagpole; safe ticks after losing a power-up. */
        int klaar, veilig;
        /** Your food when you came in (given back when you leave: a level never makes you hungry). */
        int eten;
        float verzadiging;

        Sessie(UUID speler, Actief actief) {
            this.speler = speler;
            this.actief = actief;
        }

        public GuhrioLevel.Geplaatst level() {
            return actief.level;
        }

        public Baan lane() {
            return actief.level.banen().get(baan);
        }

        public int staat(BlockPos pos) {
            return staat.getOrDefault(pos, 0);
        }

        public boolean inPijp() {
            return pijp != null;
        }

        public boolean klaar() {
            return klaar > 0;
        }
    }

    /** Through a pipe: from one mouth to the other. */
    static final class Pijpreis {
        final BlockPos van, naar;
        final int naarBaan;
        int tick;

        Pijpreis(BlockPos van, BlockPos naar, int naarBaan) {
            this.van = van;
            this.naar = naar;
            this.naarBaan = naarBaan;
        }
    }

    /** What the player's own game knows (set by the client; the blocks ask it for the look and for what you can walk through). */
    public interface ClientKant {
        boolean speelt(Player player);

        int staat(Player player, BlockPos pos);

        boolean inPijp(Player player);

        int kanalen(Player player);
    }

    @Nullable
    public static ClientKant client;

    /** Somebody reached a flagpole (for quests, the highscores, the Guhdex...). */
    public interface KlaarLuisteraar {
        void klaar(ServerPlayer player, Sessie sessie, int ticks, boolean record);
    }

    /** Called when a player finishes a level (add yours at mod construction). */
    public static final List<KlaarLuisteraar> BIJ_KLAAR = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** What a level does with an action of its own that the player's game reports (see {@link #actie}). */
    public interface ActieDoener {
        void doe(ServerPlayer player, Sessie sessie, BlockPos pos, int wezen);
    }

    private static final Map<Integer, ActieDoener> ACTIES = new ConcurrentHashMap<>();

    /**
     * A new kind of {@link GuhrioPayloads.Actie} (soort 100 and up; the engine's own are below). The player's game sends it
     * with {@code new GuhrioPayloads.Actie(soort, pos, wezen)}; the doer is only called for a player who is in a level and
     * not in a pipe or at the flagpole, and has to check itself that the action can be true.
     */
    public static void registreerActie(int soort, ActieDoener doener) {
        if (soort < 100) {
            throw new IllegalArgumentException("guhrio: actions below 100 are the engine's");
        }
        ACTIES.put(soort, doener);
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();
    private static final Map<String, Actief> ACTIEF = new ConcurrentHashMap<>();
    /** Until when (game time) a start block leaves a player alone (you must step out of it first). */
    private static final Map<UUID, Long> RUST = new ConcurrentHashMap<>();

    private GuhrioSpel() {
    }

    // =====================================================================================================================
    // asking
    // =====================================================================================================================

    @Nullable
    public static Sessie sessie(Player player) {
        return SESSIES.get(player.getUUID());
    }

    /** Is this player in a level (on the server, and in the player's own game)? */
    public static boolean speelt(Player player) {
        if (player.level().isClientSide()) {
            return client != null && client.speelt(player);
        }
        return SESSIES.containsKey(player.getUUID());
    }

    /** The state of a piece for this player (both sides). */
    public static int staat(Player player, BlockPos pos) {
        if (player.level().isClientSide()) {
            return client == null ? 0 : client.staat(player, pos);
        }
        Sessie s = SESSIES.get(player.getUUID());
        return s == null ? 0 : s.staat(pos);
    }

    /** Is switch channel {@code k} on for this player (both sides; nobody in a level: every channel is off)? */
    public static boolean kanaal(Player player, int k) {
        if (player.level().isClientSide()) {
            return client != null && (client.kanalen(player) >> k & 1) != 0;
        }
        Sessie s = SESSIES.get(player.getUUID());
        return s != null && (s.kanalen >> k & 1) != 0;
    }

    /** Is this player going through a pipe (both sides: pipes don't stop you then)? */
    public static boolean inPijp(Player player) {
        if (player.level().isClientSide()) {
            return client != null && client.inPijp(player);
        }
        Sessie s = SESSIES.get(player.getUUID());
        return s != null && s.inPijp();
    }

    /** Everybody in a level (for the creatures). */
    public static List<ServerPlayer> spelers(ServerLevel level, Actief actief) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (Sessie s : SESSIES.values()) {
            if (s.actief == actief && level.getPlayerByUUID(s.speler) instanceof ServerPlayer p) {
                uit.add(p);
            }
        }
        return uit;
    }

    static String sleutelVan(String dimensie, BlockPos anker) {
        return dimensie + "|" + anker.asLong();
    }

    // =====================================================================================================================
    // in and out
    // =====================================================================================================================

    /** The start block at {@code pos} lets this player in. False (with a line for the player) when that can't be. */
    public static boolean start(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        if (SESSIES.containsKey(player.getUUID()) || player.isSpectator() || !player.isAlive()
                || nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(player)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof GuhrioBlocks.StartBlockEntity be) || !(level.getBlockState(pos).getBlock() instanceof GuhrioBlocks.StartBlok)) {
            return false;
        }
        GuhrioLevel def = GuhrioLevel.vind(level.getServer(), be.level());
        if (def == null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhrio.geen_level", be.level()).withStyle(ChatFormatting.RED));
            return false;
        }
        if (!GuhrioKasteel.magIn(player, def)) {
            GuhrioLevel eerst = GuhrioLevel.vind(level.getServer(), def.na());
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhrio.eerst", eerst == null ? def.na() : eerst.wereld()).withStyle(ChatFormatting.RED));
            return false;
        }
        Direction kant = level.getBlockState(pos).getValue(GuhrioBlocks.StartBlok.FACING);
        String sleutel = sleutelVan(level.dimension().identifier().toString(), pos);
        Actief actief = ACTIEF.get(sleutel);
        if (actief == null) {
            actief = new Actief(def.plaats(level.dimension(), pos, kant));
            zoekStukken(level, actief);
            ACTIEF.put(sleutel, actief);
        }
        actief.leeg = 0;
        if (player.isPassenger()) {
            player.stopRiding();
        }
        Sessie s = new Sessie(player.getUUID(), actief);
        s.vlag = actief.level.start();
        s.eten = player.getFoodData().getFoodLevel();
        s.verzadiging = player.getFoodData().getSaturationLevel();
        SESSIES.put(player.getUUID(), s);
        GuhrioKasteel.begin(player, s);
        for (Stuk stuk : actief.stukken) {
            if (stuk.state.getBlock() instanceof GuhrioStuk blok) {
                int staat = blok.begin(player, s, stuk.pos, stuk.state);
                if (staat != 0) {
                    s.staat.put(stuk.pos, staat);
                }
            }
        }
        zetLijf(player, true);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(s.vlag.x, s.vlag.y, s.vlag.z);
        stuurLevel(player, s);
        wek(level, actief);
        geluid(level, s.vlag, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.2f);
        return true;
    }

    /** Tells the player's game the level (again): the lanes, the lane you are on, what every piece is for you. */
    private static void stuurLevel(ServerPlayer player, Sessie s) {
        CompoundTag data = s.actief.level.naarTag();
        data.putInt("Baan", s.baan);
        data.putLongArray("Staat", s.staat.keySet().stream().mapToLong(BlockPos::asLong).toArray());
        data.putIntArray("Standen", s.staat.keySet().stream().mapToInt(s.staat::get).toArray());
        GuhrioPayloads.send(player, new GuhrioPayloads.Start(data));
        stuurStaat(player, s);
    }

    /** The level ends for this player. */
    public static void stop(ServerPlayer player, Einde reden) {
        Sessie s = SESSIES.remove(player.getUUID());
        if (s == null) {
            return;
        }
        zetLijf(player, false);
        zetGroot(player, false);
        if (s.guhshiDier != null) {
            s.guhshiDier.discard();
            s.guhshiDier = null;
        }
        s.guhshi = false;
        GuhrioKasteel.einde(player, s, reden);
        player.getFoodData().setFoodLevel(s.eten);
        player.getFoodData().setSaturation(s.verzadiging);
        RUST.put(player.getUUID(), player.level().getGameTime() + 40);
        GuhrioPayloads.send(player, new GuhrioPayloads.Stop(reden.ordinal()));
        BlockPos ingang = s.level().ingang();
        if (reden == Einde.UITGELOGD && player.level().dimension() == s.level().dimensie()) {
            // (you come back at the level's entrance, free to walk; PlayerList saves the player right after this)
            Vec3 in = ingang == null ? s.level().start() : Vec3.atBottomCenterOf(ingang);
            player.snapTo(in.x, in.y, in.z);
        } else if (reden == Einde.KLAAR && s.level().uitgang() != null) {
            BlockPos uit = s.level().uitgang();
            player.teleportTo(uit.getX() + 0.5, uit.getY(), uit.getZ() + 0.5);
        } else if (reden == Einde.GESTOPT && ingang != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(ingang.getX() + 0.5, ingang.getY(), ingang.getZ() + 0.5);
        }
    }

    /** The jump and the weight of a level, on or off. */
    private static void zetLijf(ServerPlayer player, boolean aan) {
        zet(player.getAttribute(Attributes.JUMP_STRENGTH), SPRONG, aan ? SPRONG_ERBIJ : 0);
        zet(player.getAttribute(Attributes.GRAVITY), ZWAARTE, aan ? ZWAARTE_ERBIJ : 0);
        AttributeInstance snel = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (snel != null) {
            snel.removeModifier(SNEL);
            if (aan) {
                snel.addOrUpdateTransientModifier(new AttributeModifier(SNEL, SNEL_ERBIJ, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    private static void zetGroot(ServerPlayer player, boolean aan) {
        zet(player.getAttribute(Attributes.SCALE), GROOT, aan ? GROOT_ERBIJ : 0);
    }

    private static void zet(@Nullable AttributeInstance attribuut, Identifier id, double erbij) {
        if (attribuut == null) {
            return;
        }
        if (erbij == 0) {
            attribuut.removeModifier(id);
        } else {
            attribuut.addOrUpdateTransientModifier(new AttributeModifier(id, erbij, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** No start block or gate takes this player for the next {@code ticks} (a refused gate does not nag every tick). */
    static void rust(ServerPlayer player, int ticks) {
        if (!SESSIES.containsKey(player.getUUID())) {
            RUST.put(player.getUUID(), player.level().getGameTime() + ticks);
        }
    }

    /** May a start block take this player now (not right after a level, not while still standing in it)? */
    static boolean magStarten(ServerPlayer player) {
        long nu = player.level().getGameTime();
        Long tot = RUST.get(player.getUUID());
        if (tot != null && tot > nu) {
            RUST.put(player.getUUID(), nu + 20);
            return false;
        }
        return !SESSIES.containsKey(player.getUUID());
    }

    // =====================================================================================================================
    // the level's pieces and creatures
    // =====================================================================================================================

    /** Walks the lanes and remembers every Guhrio piece. */
    static void zoekStukken(ServerLevel level, Actief actief) {
        actief.stukken.clear();
        actief.nummer.clear();
        List<Baan> banen = actief.level.banen();
        for (int i = 0; i < banen.size(); i++) {
            Baan baan = banen.get(i);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (BlockPos kolom : baan.kolommen()) {
                // (a lane may run further than what is loaded around the player: the level is read whole, once)
                for (int y = baan.onder; y <= baan.boven; y++) {
                    pos.set(kolom.getX(), y, kolom.getZ());
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof GuhrioStuk && !actief.nummer.containsKey(pos)) {
                        actief.nummer.put(pos.immutable(), actief.stukken.size());
                        actief.stukken.add(new Stuk(pos.immutable(), state, i, baan.plek(pos.getX() + 0.5, pos.getZ() + 0.5).s()));
                    }
                }
            }
        }
    }

    private static void wek(ServerLevel level, Actief actief) {
        for (Stuk stuk : actief.stukken) {
            if (stuk.state.getBlock() instanceof GuhrioStuk blok) {
                blok.wek(level, actief, stuk);
            }
        }
    }

    private static void ruimOp(Actief actief) {
        for (Entity e : actief.wezens.values()) {
            if (e != null && !e.isRemoved()) {
                e.discard();
            }
        }
        actief.wezens.clear();
        for (Entity e : actief.los) {
            if (e != null && !e.isRemoved()) {
                e.discard();
            }
        }
        actief.los.clear();
    }

    /** Is this level still in use (its creatures ask: a creature of a level that is gone removes itself)? */
    public static boolean leeft(Actief actief) {
        return ACTIEF.get(actief.sleutel()) == actief;
    }

    /** Every creature of a level in use: the ones on their spots and the loose ones. */
    public static List<Entity> wezens(Actief actief) {
        List<Entity> uit = new ArrayList<>(actief.wezens.values());
        uit.addAll(actief.los);
        uit.removeIf(e -> e == null || e.isRemoved());
        return uit;
    }

    /** Every server tick: levels nobody plays any more lose their creatures; the others keep them alive. */
    public static void serverTick(MinecraftServer server) {
        if (ACTIEF.isEmpty()) {
            return;
        }
        for (Iterator<Actief> it = ACTIEF.values().iterator(); it.hasNext();) {
            Actief actief = it.next();
            boolean iemand = false;
            for (Sessie s : SESSIES.values()) {
                if (s.actief == actief) {
                    iemand = true;
                    break;
                }
            }
            ServerLevel level = server.getLevel(actief.level.dimensie());
            if (!iemand || level == null) {
                if (++actief.leeg > 100 || level == null) {
                    ruimOp(actief);
                    it.remove();
                }
                continue;
            }
            actief.leeg = 0;
            actief.los.removeIf(e -> e == null || e.isRemoved());
            if (server.getTickCount() % 20 == 0) {
                wek(level, actief);
            }
        }
    }

    /** The server stops: nothing is left. */
    public static void vergeetAlles() {
        for (Actief actief : ACTIEF.values()) {
            ruimOp(actief);
        }
        ACTIEF.clear();
        SESSIES.clear();
        RUST.clear();
        GuhrioLevel.vergeet();
    }

    /** The level in use at this start block, or null. */
    @Nullable
    public static Actief actief(ServerLevel level, BlockPos anker) {
        return ACTIEF.get(sleutelVan(level.dimension().identifier().toString(), anker));
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    public static void tick(ServerPlayer player) {
        Sessie s = SESSIES.get(player.getUUID());
        if (s == null) {
            return;
        }
        ServerLevel level = player.level();
        if (!player.isAlive()) {
            stop(player, Einde.DOOD);
            return;
        }
        if (level.dimension() != s.level().dimensie()) {
            stop(player, Einde.DIMENSIE);
            return;
        }
        if (nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(player)) {
            // a cutscene wins: the level stands still for you (no time, no falling, nothing touches you) until it is over
            s.veilig = Math.max(s.veilig, VEILIG_TICKS);
            if (level.getGameTime() % 10 == 0) {
                stuurStaat(player, s);
            }
            return;
        }
        s.ticks++;
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.clearFire();
        player.resetFallDistance();
        if (player.getFoodData().getFoodLevel() < RENNEN_ETEN) {
            player.getFoodData().setFoodLevel(RENNEN_ETEN);       // (running needs more than 6; you get your own food back when you leave)
        }
        if (s.veilig > 0) {
            s.veilig--;
        }
        if (s.gooiRust > 0) {
            s.gooiRust--;
        }
        for (int k = 0; k < KANALEN; k++) {
            if (s.kanaalTot[k] > 0 && s.ticks >= s.kanaalTot[k]) {
                zetKanaal(player, s, k, false);
            }
        }
        if (s.ticks % 10 == 0) {
            stuurStaat(player, s);
        }
        volgGuhshi(player, s);
        if (s.pijp != null) {
            tickPijp(player, s);
            return;
        }
        Baan baan = s.lane();
        Baan.Plek plek = baan.plek(player.getX(), player.getZ());
        if (plek.naast() > WEG || player.getY() < baan.onder - 32 || player.getY() > baan.boven + 32) {
            stop(player, Einde.WEG);
            return;
        }
        if (plek.naast() > NAAST) {
            Vec3 op = baan.punt(plek.s(), player.getY());
            player.teleportTo(op.x, op.y, op.z);
        }
        s.s = plek.s();
        if (s.klaar > 0) {
            if (--s.klaar == 0) {
                stop(player, Einde.KLAAR);
            }
            return;
        }
        if (player.getY() < baan.onder || inSaus(player)) {
            terug(player, s, 0);
            return;
        }
        // the pieces you stand in
        AABB box = player.getBoundingBox();
        int x = Mth.floor(player.getX()), z = Mth.floor(player.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = Mth.floor(box.minY + 0.01); y <= Mth.floor(box.maxY - 0.01); y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GuhrioStuk blok) {
                blok.binnen(player, s, pos.immutable(), state);
                if (SESSIES.get(player.getUUID()) != s || s.pijp != null || s.klaar > 0) {
                    return;
                }
            }
        }
    }

    /** In lava or in any sauce (every fluid but water): back to the flag, unhurt. */
    private static boolean inSaus(ServerPlayer player) {
        if (player.isInLava()) {
            return true;
        }
        FluidState fluid = player.level().getFluidState(player.blockPosition());
        return !fluid.isEmpty() && !fluid.is(FluidTags.WATER);
    }

    private static void tickPijp(ServerPlayer player, Sessie s) {
        Pijpreis reis = s.pijp;
        reis.tick++;
        if (reis.tick == PIJP_TICKS) {
            ServerLevel level = player.level();
            s.baan = reis.naarBaan;
            Vec3 binnen = GuhrioBlocks.PijpBlok.binnen(level, reis.naar, level.getBlockState(reis.naar));
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(binnen.x, binnen.y, binnen.z);
            GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.PIJP_UIT, reis.naar, s.baan));
            geluid(level, Vec3.atCenterOf(reis.naar), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 0.7f, 0.7f);
        } else if (reis.tick >= PIJP_TICKS * 2 + 10) {
            s.pijp = null;
        }
    }

    /** How deep a player can sit in the pipe under this mouth: all the way when it is at least two blocks of pipe. */
    public static double pijpDiepte(net.minecraft.world.level.BlockGetter level, BlockPos mond) {
        return level.getBlockState(mond.below()).getBlock() instanceof GuhrioBlocks.PijpLijfBlok ? PIJP_DIEP : 1.0;
    }

    static void stuurStaat(ServerPlayer player, Sessie s) {
        GuhrioPayloads.send(player, new GuhrioPayloads.Staat(s.munten, munten(player), s.ticks, s.kracht.ordinal(), s.baan, s.kanalen, s.vads,
                s.guhshi ? (s.guhshiDier == null ? -1 : s.guhshiDier.getId()) : 0));
    }

    // =====================================================================================================================
    // what happens to a player
    // =====================================================================================================================

    /** Back to your flag (waarom: 0 fell, 1 bumped into something). Nothing is lost. */
    public static void terug(ServerPlayer player, Sessie s, int waarom) {
        s.baan = s.vlagBaan;
        s.pijp = null;
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
        nl.juiced.guhs.feature.verhaal.Duwtje.terug(player, s.level().dimensie(), s.vlag, player.getYRot());
        s.veilig = VEILIG_TICKS;
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.TERUG, BlockPos.containing(s.vlag), waarom));
        stuurStaat(player, s);
        geluid(player.level(), s.vlag, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 0.8f, 1.5f);
    }

    /**
     * Something touched you (a Guhmba from the side, a spit, a kiss of a Hapbloem...). Nobody is hurt: with a power-up you
     * lose it and are safe for a moment; without, you are back at your flag.
     */
    public static void geraakt(ServerPlayer player, Sessie s) {
        if (s.veilig > 0 || s.pijp != null || s.klaar > 0 || !nl.juiced.guhs.feature.verhaal.Duwtje.mag(player)) {
            return;
        }
        if (s.guhshi) {
            // Guhshi takes the bump: he runs off (back to his spot), you stand
            zetGuhshi(player, s, false);
            s.veilig = VEILIG_TICKS;
            GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.KRIMP, player.blockPosition(), 1));
            geluid(player.level(), player.position(), nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 0.9f, 1.8f);
            return;
        }
        if (s.kracht != Kracht.GEEN) {
            zetKracht(player, s, Kracht.GEEN);
            s.veilig = VEILIG_TICKS;
            GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.KRIMP, player.blockPosition(), 0));
            geluid(player.level(), player.position(), SoundEvents.BEEHIVE_SHEAR, 0.8f, 0.8f);
            return;
        }
        terug(player, s, 1);
    }

    /** Gives (or takes) a power-up. */
    public static void zetKracht(ServerPlayer player, Sessie s, Kracht kracht) {
        s.kracht = kracht;
        zetGroot(player, kracht.groot());
        stuurStaat(player, s);
    }

    /**
     * Coins on the panel of this run only (a bonus of a level's own). They do NOT go into the pocket: what the shop takes
     * is {@link #muntVan}, which every coin of a level gives once per player.
     */
    public static void munt(ServerPlayer player, Sessie s, int aantal) {
        s.munten += aantal;
        stuurStaat(player, s);
    }

    /**
     * The coin of the piece at {@code pos} (a coin in the lane, a ?-block, a hidden block): on the panel of this run, and
     * in your pocket the FIRST time you ever take this one (no farming: a level played again shows its coins again, but
     * they are worth nothing more). True when it went into the pocket.
     */
    public static boolean muntVan(ServerPlayer player, Sessie s, BlockPos pos) {
        s.munten++;
        Integer nr = s.actief.nummer.get(pos);
        boolean nieuw = nr != null && GuhrioKasteel.pak(player, s.level().level().id(), nr);
        stuurStaat(player, s);
        return nieuw;
    }

    /** Big vadsmunt {@code nummer} (0..2) of this level is yours, for good. */
    public static void vadsmunt(ServerPlayer player, Sessie s, BlockPos pos, int nummer) {
        if ((s.vads >> nummer & 1) != 0) {
            return;
        }
        s.vads |= 1 << nummer;
        GuhrioKasteel.vadsmunt(player, s.level().level().id(), nummer);
        zetStaat(player, s, pos, 1);
        stuurStaat(player, s);
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.VADSMUNT, pos, Integer.bitCount(s.vads)));
        geluidAnderen(player, Vec3.atCenterOf(pos), SoundEvents.PLAYER_LEVELUP, 0.5f, 1.8f);
    }

    /** Switch channel {@code k} on or off for this player (the blocks of that channel change for them alone). */
    public static void zetKanaal(ServerPlayer player, Sessie s, int k, boolean aan) {
        if (k < 0 || k >= KANALEN) {
            return;
        }
        s.kanaalTot[k] = 0;
        int nieuw = aan ? s.kanalen | 1 << k : s.kanalen & ~(1 << k);
        if (nieuw != s.kanalen) {
            s.kanalen = nieuw;
            stuurStaat(player, s);
            geluid(player.level(), player.position(), SoundEvents.STONE_BUTTON_CLICK_ON, 0.8f, aan ? 1.3f : 0.8f);
        }
    }

    /** Switch channel {@code k} on for {@code ticks} (it ticks back off by itself). */
    public static void zetKanaalTijd(ServerPlayer player, Sessie s, int k, int ticks) {
        if (k < 0 || k >= KANALEN) {
            return;
        }
        zetKanaal(player, s, k, true);
        s.kanaalTot[k] = s.ticks + ticks;
    }

    /**
     * Guhshi carries this player (or not any more). While he does: a flutter jump (the player's game), a tongue
     * ({@link #tong}), and a touch costs Guhshi before anything else. The guh you see is only a look: you keep walking.
     */
    public static void zetGuhshi(ServerPlayer player, Sessie s, boolean aan) {
        if (s.guhshi == aan) {
            return;
        }
        s.guhshi = aan;
        ServerLevel level = player.level();
        if (s.guhshiDier != null) {
            level.sendParticles(ParticleTypes.POOF, s.guhshiDier.getX(), s.guhshiDier.getY() + 0.4, s.guhshiDier.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            s.guhshiDier.discard();
            s.guhshiDier = null;
        }
        if (aan) {
            nl.juiced.guhs.entity.GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (guh != null) {
                guh.setVariant(nl.juiced.guhs.entity.GuhVariant.GUHSHI);
                guh.setGuhScale(GUHSHI_SCHAAL);
                guh.setNoAi(true);
                guh.setInvulnerable(true);
                guh.setSilent(true);
                guh.noPhysics = true;
                guh.setNoGravity(true);
                guh.getPersistentData().putString(GUHSHI_TAG, player.getUUID().toString());
                nl.juiced.guhs.feature.knus.GuhHooks.zet(guh, nl.juiced.guhs.feature.verhaal.VerhaalVlaggen.VERHAAL_NPC, true);
                guh.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
                level.addFreshEntity(guh);
                s.guhshiDier = guh;
            }
            geluid(level, player.position(), nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 0.9f, 1.3f);
        }
        for (Stuk stuk : s.actief.stukken) {
            if (stuk.state.getBlock() instanceof GuhrioStukken.GuhshiPlek) {
                zetStaat(player, s, stuk.pos, aan ? 1 : 0);
            }
        }
        stuurStaat(player, s);
    }

    /** Every tick: the Guhshi that carries you is where you are. */
    private static void volgGuhshi(ServerPlayer player, Sessie s) {
        if (!s.guhshi) {
            return;
        }
        Entity guh = s.guhshiDier;
        if (guh == null || guh.isRemoved()) {
            s.guhshi = false;                       // (something removed him: get a new one the same way)
            s.guhshiDier = null;
            zetGuhshi(player, s, true);
            guh = s.guhshiDier;
            if (guh == null) {
                return;
            }
        }
        guh.setDeltaMovement(Vec3.ZERO);
        guh.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
        guh.setYHeadRot(player.getYRot());
        guh.setYBodyRot(player.getYRot());
    }

    /**
     * The Vuurpeper: a knabbel flies off along the lane, the way the player faces ({@code teken} +1 further, -1 back). It
     * bounces, squashes Guhmba's, knocks a Schild-Mika into its shell and flips switches (for the thrower).
     */
    public static boolean gooi(ServerPlayer player, Sessie s, int teken) {
        if (s.kracht != Kracht.VUUR || s.gooiRust > 0 || s.knabbels >= KNABBELS || s.pijp != null || s.klaar > 0) {
            return false;
        }
        ServerLevel level = player.level();
        KnabbelEntity knabbel = GuhrioFeature.GUHRIO_KNABBEL.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (knabbel == null) {
            return false;
        }
        Baan baan = s.lane();
        Baan.Plek plek = baan.plek(player.getX(), player.getZ());
        Vec3 uit = baan.punt(plek.s() + (teken < 0 ? -0.5 : 0.5), player.getY() + player.getBbHeight() * 0.55);
        knabbel.snapTo(uit.x, uit.y, uit.z, 0f, 0f);
        knabbel.zetBaan(s.actief, baan, null);
        knabbel.gooi(player, teken < 0 ? -1 : 1);
        level.addFreshEntity(knabbel);
        s.actief.los.add(knabbel);
        s.knabbels++;
        s.gooiRust = GOOI_RUST;
        geluid(level, uit, SoundEvents.SNOWBALL_THROW, 0.6f, 1.4f);
        return true;
    }

    /** (KnabbelEntity) one of this player's knabbels is gone. */
    static void knabbelWeg(UUID speler) {
        Sessie s = SESSIES.get(speler);
        if (s != null && s.knabbels > 0) {
            s.knabbels--;
        }
    }

    /**
     * Guhshi's tongue: the nearest thing within {@link #TONG} blocks ahead on the lane is eaten: a creature that lets itself
     * be eaten ({@link GuhrioWezen#tong}) or a coin (yours, as if you walked through it).
     */
    public static boolean tong(ServerPlayer player, Sessie s, int teken) {
        if (!s.guhshi || s.gooiRust > 0 || s.pijp != null || s.klaar > 0) {
            return false;
        }
        s.gooiRust = TONG_RUST;
        ServerLevel level = player.level();
        Baan baan = s.lane();
        double hier = baan.plek(player.getX(), player.getZ()).s();
        teken = teken < 0 ? -1 : 1;
        Entity beste = null;
        double besteAf = TONG + 0.5;
        for (Entity e : wezens(s.actief)) {
            if (!(e instanceof GuhrioWezen) || !e.isAlive() || Math.abs(e.getY() - player.getY()) > 1.6) {
                continue;
            }
            Baan.Plek p = baan.plek(e.getX(), e.getZ());
            double af = (p.s() - hier) * teken;
            if (p.naast() < 0.6 && af > 0 && af < besteAf) {
                beste = e;
                besteAf = af;
            }
        }
        Stuk munt = null;
        double muntAf = besteAf;
        for (Stuk stuk : s.actief.stukken) {
            if (stuk.baan != s.baan || !(stuk.state.getBlock() instanceof GuhrioBlocks.MuntBlok) || s.staat(stuk.pos) != 0
                    || Math.abs(stuk.pos.getY() - player.getY()) > 2.2) {
                continue;
            }
            double af = (stuk.s - hier) * teken;
            if (af > 0 && af < muntAf) {
                munt = stuk;
                muntAf = af;
            }
        }
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.TONG, player.blockPosition(),
                (int) Math.round(Math.min(TONG, Math.min(besteAf, muntAf)) * 10) * teken));
        geluid(level, player.position(), SoundEvents.FROG_TONGUE, 0.9f, 1.1f);
        if (munt != null) {
            ((GuhrioStuk) munt.state.getBlock()).binnen(player, s, munt.pos, munt.state);
            return true;
        }
        if (beste != null && ((GuhrioWezen) beste).tong(player, s)) {
            level.sendParticles(ParticleTypes.HEART, beste.getX(), beste.getY() + 0.6, beste.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
            return true;
        }
        return false;
    }


    /** Sets a piece's state for this player (and tells the player's game). */
    public static void zetStaat(ServerPlayer player, Sessie s, BlockPos pos, int staat) {
        if (staat == 0) {
            s.staat.remove(pos);
        } else {
            s.staat.put(pos.immutable(), staat);
        }
        GuhrioPayloads.send(player, new GuhrioPayloads.Stuk(pos, staat));
    }

    /** This flag is the player's now. */
    public static void vlag(ServerPlayer player, Sessie s, BlockPos pos) {
        if (pos.equals(s.vlagPos)) {
            return;
        }
        int vlagBaan = Math.max(0, s.level().baanVan(pos));
        Baan baan = s.level().banen().get(vlagBaan);
        s.vlagPos = pos.immutable();
        s.vlagBaan = vlagBaan;
        s.vlag = baan.punt(baan.plek(pos.getX() + 0.5, pos.getZ() + 0.5).s(), pos.getY());
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.VLAG, pos, 0));
        player.sendOverlayMessage(Component.translatable("gui.guhs.guhrio.vlag").withStyle(ChatFormatting.GREEN));
        geluid(player.level(), Vec3.atCenterOf(pos), SoundEvents.NOTE_BLOCK_BELL.value(), 0.9f, 1.4f);
        player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.02);
    }

    /** The flagpole: the level is done. The player is let go a few seconds later. */
    public static void klaar(ServerPlayer player, Sessie s, BlockPos pos) {
        if (s.klaar > 0) {
            return;
        }
        s.klaar = KLAAR_TICKS;
        CompoundTag tijden = spaar(player).getCompoundOrEmpty("Tijden");
        String id = s.level().level().id();
        int beste = tijden.getIntOr(id, 0);
        boolean record = beste == 0 || s.ticks < beste;
        if (record) {
            tijden.putInt(id, s.ticks);
            spaar(player).put("Tijden", tijden);
        }
        GuhrioKasteel.klaar(player, s, record);
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.KLAAR, pos, s.ticks));
        stuurStaat(player, s);
        for (KlaarLuisteraar l : BIJ_KLAAR) {
            l.klaar(player, s, s.ticks, record);
        }
        player.sendSystemMessage(Component.translatable(record ? "gui.guhs.guhrio.klaar.record" : "gui.guhs.guhrio.klaar",
                s.level().level().wereld(), tijd(s.ticks), s.munten).withStyle(ChatFormatting.GOLD));
        ServerLevel level = player.level();
        geluid(level, Vec3.atCenterOf(pos), SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 30, 0.4, 1.5, 0.4, 0.08);
    }

    /** Into the pipe at {@code van}, out of the one at {@code naar}. */
    public static void pijp(ServerPlayer player, Sessie s, BlockPos van, BlockPos naar) {
        ServerLevel wereld = player.level();
        int naarBaan = s.level().baanVan(BlockPos.containing(GuhrioBlocks.PijpBlok.buiten(wereld, naar, wereld.getBlockState(naar))));
        if (naarBaan < 0) {
            naarBaan = s.level().baanVan(naar);
        }
        if (naarBaan < 0 || s.pijp != null) {
            return;
        }
        s.pijp = new Pijpreis(van.immutable(), naar.immutable(), naarBaan);
        player.setDeltaMovement(Vec3.ZERO);
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.PIJP_IN, van, PIJP_TICKS));
        geluid(player.level(), Vec3.atCenterOf(van), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.7f, 0.7f);
    }

    /** Through the door at {@code van}: you stand in the door at {@code naar} at once. */
    public static void deur(ServerPlayer player, Sessie s, BlockPos van, BlockPos naar) {
        int naarBaan = s.level().baanVan(naar);
        if (naarBaan < 0 || s.pijp != null) {
            return;
        }
        s.baan = naarBaan;
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(naar.getX() + 0.5, naar.getY(), naar.getZ() + 0.5);
        GuhrioPayloads.send(player, new GuhrioPayloads.Moment(GuhrioPayloads.Moment.DEUR, naar, naarBaan));
        stuurStaat(player, s);
        geluid(player.level(), Vec3.atCenterOf(van), SoundEvents.WOODEN_DOOR_OPEN, 0.8f, 1.0f);
        geluid(player.level(), Vec3.atCenterOf(naar), SoundEvents.WOODEN_DOOR_CLOSE, 0.8f, 1.0f);
    }

    static void geluid(ServerLevel level, Vec3 waar, SoundEvent geluid, float volume, float toon) {
        level.playSound(null, waar.x, waar.y, waar.z, geluid, SoundSource.PLAYERS, volume, toon);
    }

    /** A sound for everybody but this player (whose own game made it already, the moment it happened). */
    static void geluidAnderen(ServerPlayer behalve, Vec3 waar, SoundEvent geluid, float volume, float toon) {
        behalve.level().playSound(behalve, waar.x, waar.y, waar.z, geluid, SoundSource.PLAYERS, volume, toon);
    }

    /** 1:23.4 */
    public static String tijd(int ticks) {
        int tienden = ticks / 2;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%d", tienden / 600, tienden / 10 % 60, tienden % 10);
    }

    // =====================================================================================================================
    // what the player's game reports
    // =====================================================================================================================

    public static void actie(ServerPlayer player, int soort, BlockPos pos, int wezen) {
        Sessie s = SESSIES.get(player.getUUID());
        if (s != null && soort == GuhrioPayloads.Actie.WEER) {
            stuurLevel(player, s);
            return;
        }
        if (s != null && soort == GuhrioPayloads.Actie.STOP) {
            stop(player, Einde.GESTOPT);
            return;
        }
        if (s == null || s.pijp != null || s.klaar > 0 || nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(player)) {
            return;
        }
        ServerLevel level = player.level();
        if (soort == GuhrioPayloads.Actie.GOOI) {
            if (s.guhshi) {
                tong(player, s, wezen);
            } else {
                gooi(player, s, wezen);
            }
            return;
        }
        ActieDoener eigen = ACTIES.get(soort);
        if (eigen != null) {
            eigen.doe(player, s, pos, wezen);
            return;
        }
        if (soort == GuhrioPayloads.Actie.STAMP || soort == GuhrioPayloads.Actie.GERAAKT) {
            Entity e = level.getEntity(wezen);
            if (e instanceof GuhrioWezen w && e.isAlive() && e.getBoundingBox().inflate(1.5).intersects(player.getBoundingBox())) {
                if (soort == GuhrioPayloads.Actie.STAMP) {
                    w.stamp(player, s);
                } else {
                    w.raakt(player, s);
                }
            }
            return;
        }
        if (!level.isLoaded(pos) || pos.distToCenterSqr(player.getX(), player.getY() + 0.9, player.getZ()) > BEREIK * BEREIK
                || s.level().baanVan(pos) < 0) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GuhrioStuk blok)) {
            return;
        }
        switch (soort) {
            case GuhrioPayloads.Actie.BOTS -> blok.bots(player, s, pos, state);
            case GuhrioPayloads.Actie.RAAK -> blok.binnen(player, s, pos, state);
            case GuhrioPayloads.Actie.DUIK -> blok.duik(player, s, pos, state);
            case GuhrioPayloads.Actie.DEUR -> blok.deur(player, s, pos, state);
            case GuhrioPayloads.Actie.STAP -> blok.stap(player, s, pos, state);
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // what stays (per player)
    // =====================================================================================================================

    /** The player's own Guhrio data that stays (coins, best times). */
    public static CompoundTag spaar(Player player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (saved.getCompound("guhrio").isEmpty()) {
            saved.put("guhrio", new CompoundTag());
        }
        return saved.getCompoundOrEmpty("guhrio");
    }

    /** The coins in this player's pocket (what the shop takes; see {@link GuhrioKasteel#betaal}). */
    public static int munten(Player player) {
        return spaar(player).getIntOr("Munten", 0);
    }

    /** The player's best time on a level in ticks, or 0. */
    public static int besteTijd(Player player, String level) {
        return spaar(player).getCompoundOrEmpty("Tijden").getIntOr(level, 0);
    }
}
