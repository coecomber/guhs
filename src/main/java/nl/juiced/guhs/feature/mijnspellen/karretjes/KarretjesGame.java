package nl.juiced.guhs.feature.mijnspellen.karretjes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.mijnspellen.MijnspelBlokken;
import nl.juiced.guhs.feature.mijnspellen.Mijnspel;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Baan;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Bak;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Erts;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Kant;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Punt;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Rit;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesNet.Wissel;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A shift at the switches with Conducteur Piepwiel: one player at a time per Wisselgrot (the others watch from the
 * balcony). You pick a network (makkelijk, medium, lastig); Piepwiel counts down and the carts start rolling out of the
 * tunnel, each full of one ore. On the balcony railing there is a lever above every switch (on = right); the rail of the
 * switch shows where it sends the next cart. A cart in its own bin: +1 ("klopt!"); in the wrong bin it is gone; three
 * gone and the shift is over. Every cart comes a little sooner and all of them roll a little faster.
 * <p>
 * (1.7.0 playtest) A calm start: a five-second countdown that already says the first cart's ore, the action bar always
 * shows the next cart's ore, the first {@link #RUSTIG} carts roll slower (and come no sooner), and the very first one
 * waits {@link #WACHT} ticks in the tunnel mouth before it rolls on to the first switch ({@link #tickenTotEersteWissel}).
 * <p>
 * Ertsbonnetjes: the carts you sorted times 0.6 / 1.0 / 1.4 (makkelijk / medium / lastig), at most {@link #MAX_MUNTEN},
 * +5 for beating your own record on that network ({@link #munten}). The world's top 3 per network float above Piepwiel.
 * While you play you can't get hurt or hungry. Games live in memory (a restart ends them; the carts then disappear by
 * themselves). The networks are found once by their tunnel mouths (karretjes_spoorbegin) around Piepwiel, and laid
 * (rails, bins, levers) when a shift starts.
 */
public final class KarretjesGame {
    /** Options of Piepwiel's talking screen. */
    public static final int OPTIE_MAKKELIJK = 1, OPTIE_MEDIUM = 2, OPTIE_LASTIG = 3, OPTIE_WINKEL = 4, OPTIE_UITLEG = 5, OPTIE_STOP = 6;
    public static final int LEVENS = 3, AFTELLEN = 101, IDLE_LIMIT = 20 * 60 * 15, NA_TIJD = 60, MAX_KARREN = 120, MAX_MUNTEN = 90;
    /** How fast the carts roll at the start (blocks per tick), how much faster per cart, at most how much faster. */
    static final double[] SNELHEID = {0.09, 0.11, 0.13};
    static final double SNELLER = 0.0015, MAX_SNELLER = 0.07;
    /** Ticks between two carts at the start, the factor after every cart, the shortest. */
    static final int[] INTERVAL = {150, 125, 110}, MIN_INTERVAL = {56, 46, 38};
    static final double KORTER = 0.95;
    /** The calm start: the speed of the first carts (factor), and how long the very first one waits in the tunnel mouth. */
    static final double[] RUSTIG = {0.6, 0.75, 0.9};
    public static final int WACHT = 40;
    static final double[] MUNTEN = {0.6, 1.0, 1.4};
    public static final int SCAN = 40, WEG = 48, SCORES_TICKS = 100;
    public static final String BORD = "karretjes";
    static final String BEST = "guhs_karretjes_best_", POTJES = "guhs_karretjes_potjes";
    /** Placing rails without vanilla reshaping them (no onPlace, no neighbour updates). */
    private static final int STIL = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

    public enum Phase { IDLE, AFTELLEN, SPELEN, NA }

    private static final Map<UUID, KarretjesGame> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> SPELERS = new ConcurrentHashMap<>();
    private static final int STALE_TICKS = 60;

    /** One cart on its way: its ride and its entity. */
    static final class Kar {
        final Rit rit;
        final UUID entity;
        /** Its own speed (times the shift's), and how long it still waits in the tunnel mouth (u 0). */
        final double factor;
        int wacht;

        Kar(Rit rit, UUID entity, double factor, int wacht) {
            this.rit = rit;
            this.entity = entity;
            this.factor = factor;
            this.wacht = wacht;
        }

        /** Waiting in the tunnel mouth now? (then it doesn't roll this tick) */
        boolean wachtNu() {
            if (wacht > 0 && rit.totaal() >= -KarretjesNet.START.u()) {
                wacht--;
                return true;
            }
            return false;
        }
    }

    private final UUID npcId;
    @Nullable
    private UUID player;
    private String playerName = "";
    private Phase phase = Phase.IDLE;
    private Niveau niveau = Niveau.MEDIUM;
    private int timer, gestuurd, goed, kwijt;
    private double interval;
    private long lastTick, gestart;
    private final List<Kar> karren = new ArrayList<>();
    private boolean[] standen = new boolean[0];
    /** The ore of the next cart (said in the countdown and in the action bar). */
    @Nullable
    private Erts volgende;

    // the networks, found once
    private boolean scanned;
    private final BlockPos[] begin = new BlockPos[3];
    private final Direction[] richting = {Direction.SOUTH, Direction.SOUTH, Direction.SOUTH};

    private KarretjesGame(UUID npcId) {
        this.npcId = npcId;
    }

    public static KarretjesGame of(GuhNpcEntity npc) {
        KarretjesGame game = GAMES.computeIfAbsent(npc.getUUID(), KarretjesGame::new);
        if (!game.scanned) {
            game.load(npc.roleData);
        }
        return game;
    }

    public static boolean isPlaying(Player p) {
        return SPELERS.containsKey(p.getUUID());
    }

    public boolean isRunning() {
        return player != null;
    }

    public boolean isPlayedBy(Player p) {
        return p.getUUID().equals(player);
    }

    boolean owns(ErtsKarretjeEntity e) {
        if (!isRunning()) {
            return false;
        }
        for (Kar k : karren) {
            if (k.entity.equals(e.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public Phase phase() {
        return phase;
    }

    public int goed() {
        return goed;
    }

    public int kwijt() {
        return kwijt;
    }

    public Niveau niveau() {
        return niveau;
    }

    public List<Kar> karren() {
        return karren;
    }

    @Nullable
    public BlockPos begin(Niveau n) {
        return begin[n.ordinal()];
    }

    public Direction richting(Niveau n) {
        return richting[n.ordinal()];
    }

    // --- the baan's frame -----------------------------------------------------------------------------------------------

    /** The block of cell (u, v) of this network, at height dy above its floor. */
    public BlockPos cel(Niveau n, int u, int v, int dy) {
        Direction f = richting[n.ordinal()];
        return begin[n.ordinal()].relative(f, u).relative(f.getClockWise(), v).above(dy);
    }

    /** The world point of (u, v) (cell centres at whole numbers) at height dy above the floor block's top. */
    public Vec3 punt(Niveau n, double u, double v, double dy) {
        Direction f = richting[n.ordinal()], r = f.getClockWise();
        BlockPos b = begin[n.ordinal()];
        return new Vec3(b.getX() + 0.5 + f.getStepX() * u + r.getStepX() * v, b.getY() + 1 + dy,
                b.getZ() + 0.5 + f.getStepZ() * u + r.getStepZ() * v);
    }

    private Direction wereld(Niveau n, Kant k) {
        Direction f = richting[n.ordinal()];
        return switch (k) {
            case VOOR -> f;
            case ACHTER -> f.getOpposite();
            case RECHTS -> f.getClockWise();
            case LINKS -> f.getCounterClockWise();
        };
    }

    static RailShape vorm(Direction a, Direction b) {
        if (a.getAxis() == b.getAxis()) {
            return a.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }
        Direction ns = a.getAxis() == Direction.Axis.Z ? a : b, ew = ns == a ? b : a;
        if (ns == Direction.NORTH) {
            return ew == Direction.EAST ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        }
        return ew == Direction.EAST ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
    }

    // --- laying the network ------------------------------------------------------------------------------------------------

    /** Lays the rails, the bins and the levers of this network (idempotent; the levers all go off). */
    public void leg(ServerLevel world, Niveau n) {
        if (begin[n.ordinal()] == null) {
            return;
        }
        Baan baan = KarretjesNet.baan(n);
        standen = new boolean[baan.wissels().size()];
        Map<Punt, Set<Kant>> rails = KarretjesNet.rails(baan);
        for (int pass = 0; pass < 2; pass++) {
            for (var e : rails.entrySet()) {
                BlockPos pos = cel(n, e.getKey().u(), e.getKey().v(), 1);
                Kant[] k = KarretjesNet.getoond(baan, e.getKey(), e.getValue(), standen);
                BlockState rail = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, vorm(wereld(n, k[0]), wereld(n, k[1])));
                if (world.getBlockState(pos) != rail) {
                    world.setBlock(pos, rail, STIL);
                }
                BlockPos boven = pos.above();
                if (!world.getBlockState(boven).isAir()) {
                    world.setBlock(boven, Blocks.AIR.defaultBlockState(), STIL);
                }
            }
        }
        // (1.7.0 playtest) glitterrank the cave decoration hung from the ceiling, over the network and the balcony: gone
        for (int u = KarretjesNet.BALKON_U - 3; u <= baan.uMax() + 1; u++) {
            for (int v = baan.vMin() - 1; v <= baan.vMax() + 1; v++) {
                for (int dy = 1; dy <= 11; dy++) {
                    BlockPos pos = cel(n, u, v, dy);
                    if (world.getBlockState(pos).is(nl.juiced.guhs.feature.onderwol.OnderWolFeature.GLITTERRANK.get())
                            || world.getBlockState(pos).is(nl.juiced.guhs.feature.onderwol.OnderWolFeature.GLITTERRANK_BES.get())) {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        Direction terug = richting[n.ordinal()].getOpposite();
        for (Bak b : baan.bakken()) {
            BlockState bak = KarretjesFeature.ERTSBAK.get().defaultBlockState().setValue(MijnspelBlokken.Soort.FACING, terug)
                    .setValue(MijnspelBlokken.SOORT, b.erts().ordinal());
            world.setBlock(cel(n, b.p().u(), b.p().v(), 1), bak, Block.UPDATE_CLIENTS);
        }
        for (Wissel w : baan.wissels()) {
            BlockPos lever = hendel(n, w);
            BlockState onder = world.getBlockState(lever.below());
            if (onder.isAir()) {
                world.setBlock(lever.below(), KarretjesFeature.BIELZENHOUT.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            world.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                    .setValue(LeverBlock.FACING, richting[n.ordinal()]).setValue(LeverBlock.POWERED, false), Block.UPDATE_ALL);
        }
    }

    /** Where the lever of this switch stands. */
    public BlockPos hendel(Niveau n, Wissel w) {
        return cel(n, KarretjesNet.HENDEL_U, w.p().v(), KarretjesNet.HENDEL_Y);
    }

    /** Reads the levers (true = on = right) and turns the switch rails to match. */
    private void leesHendels(ServerLevel world, @Nullable ServerPlayer p) {
        Baan baan = KarretjesNet.baan(niveau);
        if (standen.length != baan.wissels().size()) {
            standen = new boolean[baan.wissels().size()];
        }
        for (Wissel w : baan.wissels()) {
            BlockState s = world.getBlockState(hendel(niveau, w));
            boolean aan = s.is(Blocks.LEVER) && s.getValue(LeverBlock.POWERED);
            if (aan != standen[w.nr()]) {
                standen[w.nr()] = aan;
                Kant[] k = KarretjesNet.getoond(baan, w.p(), Set.of(), standen);
                world.setBlock(cel(niveau, w.p().u(), w.p().v(), 1), Blocks.RAIL.defaultBlockState()
                        .setValue(RailBlock.SHAPE, vorm(wereld(niveau, k[0]), wereld(niveau, k[1]))), STIL);
                Vec3 at = punt(niveau, w.p().u(), w.p().v(), 0.3);
                world.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.2, 0.1, 0.2, 0.05);
                world.playSound(null, BlockPos.containing(at), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
            }
        }
    }

    /** (tests, hints) the switch settings now. */
    public boolean[] standen() {
        return standen.clone();
    }

    // --- talking to Piepwiel --------------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer p) {
        KarretjesGame game = of(npc);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.1f);
        KarretjesFeature.checkKleding(p);
        if (game.isPlayedBy(p)) {
            Praat.open(p, npc, null, "quest.guhs.karretjes.bezig_jij", new Object[] {game.goed, game.kwijt},
                    new Praat.Optie(OPTIE_STOP, "gui.guhs.karretjes.optie.stop"));
            return;
        }
        if (game.isRunning()) {
            Praat.open(p, npc, null, "quest.guhs.karretjes.bezig", new Object[] {game.playerName},
                    new Praat.Optie(OPTIE_WINKEL, "gui.guhs.karretjes.optie.winkel"), new Praat.Optie(OPTIE_UITLEG, "gui.guhs.karretjes.optie.uitleg"));
            return;
        }
        Praat.open(p, npc, null, "quest.guhs.karretjes.hallo" + (1 + npc.getRandom().nextInt(4)),
                new Object[] {bestTekst(p, Niveau.MAKKELIJK), bestTekst(p, Niveau.MEDIUM), bestTekst(p, Niveau.LASTIG)},
                new Praat.Optie(OPTIE_MAKKELIJK, "gui.guhs.karretjes.optie.makkelijk"), new Praat.Optie(OPTIE_MEDIUM, "gui.guhs.karretjes.optie.medium"),
                new Praat.Optie(OPTIE_LASTIG, "gui.guhs.karretjes.optie.lastig"), new Praat.Optie(OPTIE_WINKEL, "gui.guhs.karretjes.optie.winkel"),
                new Praat.Optie(OPTIE_UITLEG, "gui.guhs.karretjes.optie.uitleg"));
    }

    public static void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        if (p.distanceToSqr(npc) > 100) {
            return;
        }
        KarretjesGame game = of(npc);
        switch (optie) {
            case OPTIE_MAKKELIJK, OPTIE_MEDIUM, OPTIE_LASTIG -> {
                Praat.sluit(p);
                game.start(npc, p, Niveau.of(optie - OPTIE_MAKKELIJK));
            }
            case OPTIE_WINKEL -> {
                Praat.sluit(p);
                npc.openShop(p);
            }
            case OPTIE_UITLEG -> Praat.open(p, npc, null, "quest.guhs.karretjes.uitleg", new Object[] {LEVENS},
                    new Praat.Optie(OPTIE_MAKKELIJK, "gui.guhs.karretjes.optie.makkelijk"), new Praat.Optie(OPTIE_MEDIUM, "gui.guhs.karretjes.optie.medium"),
                    new Praat.Optie(OPTIE_LASTIG, "gui.guhs.karretjes.optie.lastig"));
            case OPTIE_STOP -> {
                Praat.sluit(p);
                if (game.isPlayedBy(p)) {
                    game.einde(npc, p, "quest.guhs.karretjes.gestopt", true);
                }
            }
            default -> {
            }
        }
    }

    // --- a shift -----------------------------------------------------------------------------------------------------------

    /** Starts a shift on this network (true: started). */
    public boolean start(GuhNpcEntity npc, ServerPlayer p, Niveau n) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return false;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.karretjes.bezig", playerName);
            return false;
        }
        if (Minigames.refuse(p, npc, KarretjesFeature.ID)) {
            return false;
        }
        if (!scan(npc, true) || begin[n.ordinal()] == null) {
            GuhQuests.say(p, npc, "quest.guhs.karretjes.kapot");
            return false;
        }
        niveau = n;
        leg(world, n);
        clearKarren(world);
        player = p.getUUID();
        playerName = p.getGameProfile().name();
        SPELERS.put(player, npcId);
        goed = kwijt = gestuurd = 0;
        interval = INTERVAL[n.ordinal()];
        lastTick = gestart = world.getGameTime();
        Minigames.startKeeping(p);
        p.clearFire();
        Vec3 balkon = punt(n, KarretjesNet.BALKON_U, 0, KarretjesNet.BALKON_Y - 1);
        p.teleportTo(world, balkon.x, balkon.y, balkon.z, java.util.Set.of(), richting[n.ordinal()].toYRot(), 35f, true);
        phase = Phase.AFTELLEN;
        timer = AFTELLEN;
        volgende = kies(world);
        GuhQuests.say(p, npc, "quest.guhs.karretjes.start_" + n.id());
        p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.hoe").withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.rustig").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Every tick of Piepwiel: the scoreboard, the countdown, the levers and the carts. */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        if (npc.tickCount % SCORES_TICKS == 1) {
            if (!scanned && world.getNearestPlayer(npc, 32) != null) {
                scan(npc, false);
            }
            showScores(npc);
        }
        if (player == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || Mijnspel.afstand(p, npc) > WEG) {
            einde(npc, p != null && p.isAlive() && p.level() == world ? p : null, "quest.guhs.karretjes.weggelopen", false);
            return;
        }
        if (npc.tickCount % 20 == 0) {
            Minigames.keep(p);
            p.clearFire();
        }
        leesHendels(world, p);
        switch (phase) {
            case AFTELLEN -> {
                timer--;
                if (timer > 0 && timer % 20 == 0) {
                    Mijnspel.titel(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            timer > 40 || volgende == null ? Component.translatable("gui.guhs.karretjes.aftellen")
                                    : Component.translatable("gui.guhs.karretjes.eerste", ertsNaam(volgende)), 0, 22, 0);
                    Mijnspel.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 0.9f, 1.0f);
                }
                if (timer % 10 == 5 && volgende != null) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.karretjes.eerste", ertsNaam(volgende)).withStyle(ChatFormatting.YELLOW));
                }
                if (timer <= 0) {
                    phase = Phase.SPELEN;
                    timer = 0;
                    Mijnspel.titel(p, Component.translatable("gui.guhs.karretjes.tuut").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.karretjes.tuut_sub"), 0, 30, 10);
                    Mijnspel.geluid(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1f, 1.4f);
                    npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.1f);
                }
            }
            case SPELEN -> {
                if (--timer <= 0 && gestuurd < MAX_KARREN) {
                    int nr = gestuurd;
                    stuur(world);
                    timer = volgendeTimer(nr, interval);
                    interval = krimp(niveau, nr, interval);
                }
                rijden(world, npc, p);
                if (phase == Phase.SPELEN && (kwijt >= LEVENS || gestuurd >= MAX_KARREN && karren.isEmpty())) {
                    afgelopen(npc, p);
                } else if (world.getGameTime() - gestart > IDLE_LIMIT) {
                    einde(npc, p, "quest.guhs.karretjes.te_lang", true);
                    return;
                }
            }
            case NA -> {
                if (--timer <= 0) {
                    einde(npc, p, null, false);
                    return;
                }
            }
            default -> {
            }
        }
        if (isRunning() && phase == Phase.SPELEN && npc.tickCount % 10 == 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.karretjes.balk", goed, kwijt, LEVENS,
                    volgende == null ? Component.literal("-") : ertsNaam(volgende), niveau.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** How fast the carts roll now. */
    public double snelheid() {
        return SNELHEID[niveau.ordinal()] + Math.min(MAX_SNELLER, SNELLER * gestuurd);
    }

    /** A new cart out of the tunnel: the announced ore (the next one at random among this network's bins), calm at the start. */
    private void stuur(ServerLevel world) {
        Erts erts = volgende != null ? volgende : kies(world);
        int nr = gestuurd;
        stuur(world, erts, factor(nr), nr == 0 ? WACHT : 0);
        volgende = kies(world);
    }

    private Erts kies(ServerLevel world) {
        List<Erts> ertsen = KarretjesNet.baan(niveau).ertsen();
        return ertsen.get(world.getRandom().nextInt(ertsen.size()));
    }

    static Component ertsNaam(Erts erts) {
        return Component.translatable("gui.guhs.karretjes.erts." + erts.id());
    }

    /** The speed factor of the cart with this number (0 = the first of the shift). */
    static double factor(int nr) {
        return nr < RUSTIG.length ? RUSTIG[nr] : 1.0;
    }

    /** Ticks until the next cart after cart nr was sent (the first one also waits in the tunnel mouth). */
    static int volgendeTimer(int nr, double interval) {
        return (int) Math.round(interval) + (nr == 0 ? WACHT : 0);
    }

    /** The interval after cart nr: the calm carts keep it, then every cart a little sooner. */
    static double krimp(Niveau n, int nr, double interval) {
        return nr < RUSTIG.length ? interval : Math.max(MIN_INTERVAL[n.ordinal()], interval * KORTER);
    }

    /** (also tests) a cart with this ore, at full speed. */
    public Kar stuur(ServerLevel world, Erts erts) {
        return stuur(world, erts, 1.0, 0);
    }

    private Kar stuur(ServerLevel world, Erts erts, double factor, int wacht) {
        Rit rit = new Rit(KarretjesNet.baan(niveau), erts);
        double[] at = rit.positie();
        Vec3 pos = punt(niveau, at[0], at[1], 0);
        ErtsKarretjeEntity e = ErtsKarretjeEntity.create(world, this, pos, yaw(rit), lading(erts));
        Kar kar = new Kar(rit, e.getUUID(), factor, wacht);
        karren.add(kar);
        gestuurd++;
        world.playSound(null, BlockPos.containing(pos), SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.25f, 1.3f);
        return kar;
    }

    /** The block in the cart: the ore of the Onder de Wol layer, or a vanilla one. */
    static BlockState lading(Erts erts) {
        Block b = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(erts.blok));
        if (b == Blocks.AIR) {
            b = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(erts.reserve));
        }
        return b.defaultBlockState();
    }

    private float yaw(Rit rit) {
        Direction d = wereld(niveau, rit.richting());
        return (float) (Math.atan2(-d.getStepZ(), -d.getStepX()) * 180 / Math.PI);
    }

    /** Every cart one step on; arrivals are counted. */
    private void rijden(ServerLevel world, GuhNpcEntity npc, @Nullable ServerPlayer p) {
        double v = snelheid();
        for (int i = karren.size() - 1; i >= 0; i--) {
            Kar k = karren.get(i);
            if (k.wachtNu()) {
                continue;
            }
            Bak bak = k.rit.stap(v * k.factor, standen);
            double[] at = k.rit.positie();
            Vec3 pos = punt(niveau, at[0], at[1], 0);
            if (world.getEntity(k.entity) instanceof ErtsKarretjeEntity e) {
                e.zet(pos, yaw(k.rit));
            }
            if (bak != null) {
                karren.remove(i);
                if (world.getEntity(k.entity) instanceof ErtsKarretjeEntity e) {
                    e.discard();
                }
                aangekomen(world, npc, p, k.rit.erts, bak, pos);
            }
        }
        if (niveau == Niveau.MAKKELIJK && !karren.isEmpty() && world.getGameTime() % 6 == 0) {   // makkelijk: the bin of the first cart sparkles
            Kar eerste = karren.get(0);
            for (Bak b : KarretjesNet.baan(niveau).bakken()) {
                if (b.erts() == eerste.rit.erts) {
                    Vec3 at = punt(niveau, b.p().u(), b.p().v(), 1.2);
                    world.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 2, 0.25, 0.2, 0.25, 0);
                }
            }
        }
    }

    /** A cart in a bin: right or wrong (also tests). */
    void aangekomen(ServerLevel world, GuhNpcEntity npc, @Nullable ServerPlayer p, Erts erts, Bak bak, Vec3 pos) {
        BlockParticleOption stof = new BlockParticleOption(ParticleTypes.BLOCK, lading(erts));
        if (bak.erts() == erts) {
            goed++;
            world.sendParticles(stof, pos.x, pos.y + 0.6, pos.z, 18, 0.3, 0.2, 0.3, 0.1);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 6, 0.3, 0.3, 0.3, 0);
            world.playSound(null, BlockPos.containing(pos), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.9f, 1.0f + Math.min(1f, goed * 0.02f));
            if (p != null) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.karretjes.klopt", Component.translatable("gui.guhs.karretjes.erts." + erts.id()), goed)
                        .withStyle(ChatFormatting.GREEN));
                if (goed % 10 == 0) {
                    GuhQuests.say(p, npc, "quest.guhs.karretjes.tien" + (1 + (goed / 10 - 1) % 3), goed);
                }
            }
        } else {
            kwijt++;
            world.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 14, 0.3, 0.3, 0.3, 0.05);
            world.sendParticles(stof, pos.x, pos.y + 0.6, pos.z, 10, 0.4, 0.2, 0.4, 0.15);
            world.playSound(null, BlockPos.containing(pos), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.6f);
            world.playSound(null, BlockPos.containing(pos), ModSounds.GUH_HURT.get(), SoundSource.NEUTRAL, 0.6f, 1.4f);
            if (p != null) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.karretjes.fout", Component.translatable("gui.guhs.karretjes.erts." + erts.id()),
                        Component.translatable("gui.guhs.karretjes.erts." + bak.erts().id()), LEVENS - kwijt).withStyle(ChatFormatting.RED));
            }
        }
    }

    /** Three carts gone (or every cart done): Piepwiel counts. */
    private void afgelopen(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        clearKarren(world);
        CompoundTag saved = GuhQuests.saved(p);
        String key = BEST + niveau.id();
        int oud = saved.contains(key) ? saved.getIntOr(key, 0) : -1;
        boolean record = oud >= 0 && goed > oud;
        if (oud < 0 || goed > oud) {
            saved.putInt(key, goed);
        }
        saved.putInt(POTJES, saved.getIntOr(POTJES, 0) + 1);
        int munten = munten(niveau, goed, record);
        p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.telling", goed, niveau.naam()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (record) {
            p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.record", goed, oud).withStyle(ChatFormatting.GOLD));
        } else if (oud < 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.eerste_record", goed).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.geen_record", oud).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.munten", munten).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (munten > 0) {
            Minigames.give(p, new ItemStack(KarretjesFeature.ERTSBONNETJE.get(), munten));
        }
        GuhAdvancements.grant(p, "karretjes_gespeeld");
        if (goed >= 25) {
            GuhAdvancements.grant(p, "karretjes_25");
        }
        if (niveau == Niveau.LASTIG && goed >= 40) {
            GuhAdvancements.grant(p, "karretjes_lastig_40");
        }
        if (Scorebord.submit(p, BORD + "_" + niveau.id(), goed, false) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.karretjes.grot_record").withStyle(ChatFormatting.GOLD));
        }
        showScores(npc);
        Mijnspel.titel(p, Component.translatable("gui.guhs.karretjes.klaar", goed).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.karretjes.klaar_sub", munten), 5, 60, 15);
        Mijnspel.geluid(p, goed >= 25 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
        npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.2f);
        GuhQuests.say(p, npc, "quest.guhs.karretjes.oordeel_" + (goed >= 40 ? "top" : goed >= 25 ? "goed" : goed >= 10 ? "aardig" : "oefenen"));
        phase = Phase.NA;
        timer = NA_TIJD;
    }

    /** Ertsbonnetjes for a shift: the sorted carts x 0.6 / 1.0 / 1.4 (at most {@link #MAX_MUNTEN}), +5 for a record. */
    public static int munten(Niveau n, int goed, boolean record) {
        return Math.min(MAX_MUNTEN, (int) Math.round(goed * MUNTEN[n.ordinal()])) + (record ? 5 : 0);
    }

    /** The shift ends (without counting, unless it was counted already). */
    private void einde(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String melding, boolean zeg) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null && melding != null) {
            if (zeg) {
                GuhQuests.say(p, npc, melding);
            } else {
                p.sendSystemMessage(Component.translatable(melding).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        clearKarren(world);
        if (player != null) {
            SPELERS.remove(player, npcId);
        }
        player = null;
        phase = Phase.IDLE;
    }

    private void clearKarren(ServerLevel world) {
        for (Kar k : karren) {
            if (world.getEntity(k.entity) instanceof ErtsKarretjeEntity e) {
                e.discard();
            }
        }
        karren.clear();
    }

    /** A player logged out, died or changed dimension: the shift is over (the carts go by themselves). */
    public static void leave(ServerPlayer p) {
        UUID npc = SPELERS.remove(p.getUUID());
        KarretjesGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.player = null;
            game.phase = Phase.IDLE;
            game.karren.clear();
        }
    }

    /** Is this player's Piepwiel still there, and still ticking? */
    public static boolean hasPiepwielNearby(ServerPlayer p) {
        UUID npc = SPELERS.get(p.getUUID());
        KarretjesGame game = npc == null ? null : GAMES.get(npc);
        return game != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - game.lastTick <= STALE_TICKS;
    }

    // --- finding the networks -------------------------------------------------------------------------------------------------

    public boolean scan(GuhNpcEntity npc, boolean force) {
        if (scanned) {
            return true;
        }
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos c = npc.blockPosition();
        BlockPos min = c.offset(-SCAN, -14, -SCAN), max = c.offset(SCAN, 8, SCAN);
        if (!force && !world.hasChunksAt(min, max)) {
            return false;
        }
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState s = world.getBlockState(pos);
            if (s.is(KarretjesFeature.SPOORBEGIN.get())) {
                int b = s.getValue(MijnspelBlokken.BAAN);
                begin[b] = pos.immutable();
                richting[b] = s.getValue(MijnspelBlokken.Begin.FACING);
                any = true;
            }
        }
        if (!any) {
            return false;
        }
        scanned = true;
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < 3; i++) {
            if (begin[i] != null) {
                tag.putLong("Begin" + i, begin[i].asLong());
                tag.putString("Richting" + i, richting[i].getName());
            }
        }
        npc.roleData.put("Wisselgrot", tag);
        return true;
    }

    private void load(CompoundTag data) {
        CompoundTag tag = data.getCompoundOrEmpty("Wisselgrot");
        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (tag.contains("Begin" + i)) {
                begin[i] = BlockPos.of(tag.getLongOr("Begin" + i, 0L));
                Direction d = Direction.byName(tag.getStringOr("Richting" + i, "south"));
                richting[i] = d == null || d.getAxis().isVertical() ? Direction.SOUTH : d;
                any = true;
            }
        }
        scanned = any;
    }

    /** (tests) use this network without scanning. */
    public void testBegin(Niveau n, BlockPos pos, Direction f) {
        begin[n.ordinal()] = pos;
        richting[n.ordinal()] = f;
        scanned = true;
    }

    /** The next cart's ore (null: none announced). */
    @Nullable
    public Erts volgende() {
        return volgende;
    }

    /**
     * How many ticks the first cart of a shift takes from leaving the tunnel to reaching the first switch: rolling at its
     * calm speed to the tunnel mouth, waiting there, rolling on (the levers stay as they are).
     */
    public static int tickenTotEersteWissel(Niveau n) {
        Baan baan = KarretjesNet.baan(n);
        Kar k = new Kar(new Rit(baan, baan.ertsen().get(0)), new UUID(0, 0), factor(0), WACHT);
        double tot = baan.wortel().p().u() - KarretjesNet.START.u();
        int ticks = 0;
        while (k.rit.totaal() < tot && ticks < 10000) {
            if (!k.wachtNu()) {
                k.rit.stap(SNELHEID[n.ordinal()] * k.factor, new boolean[baan.wissels().size()]);
            }
            ticks++;
        }
        return ticks;
    }

    /**
     * (tests) The first carts of a shift as the tick loop sends them (all levers off): the smallest distance (blocks) between
     * two carts that are both still rolling on the shared stem of the network (up to the first switch).
     */
    public static double kleinsteAfstandOpDeStam(Niveau n, int aantal) {
        Baan baan = KarretjesNet.baan(n);
        double stam = baan.wortel().p().u() - KarretjesNet.START.u();
        boolean[] uit = new boolean[baan.wissels().size()];
        List<Kar> rij = new ArrayList<>();
        double interval = INTERVAL[n.ordinal()], kleinste = Double.MAX_VALUE;
        int timer = 0, gestuurd = 0;
        for (int t = 0; t < 20 * 120; t++) {
            if (--timer <= 0 && gestuurd < aantal) {
                rij.add(new Kar(new Rit(baan, baan.ertsen().get(0)), new UUID(0, gestuurd), factor(gestuurd), gestuurd == 0 ? WACHT : 0));
                timer = volgendeTimer(gestuurd, interval);
                interval = krimp(n, gestuurd, interval);
                gestuurd++;
            }
            double v = SNELHEID[n.ordinal()] + Math.min(MAX_SNELLER, SNELLER * gestuurd);
            for (Kar k : rij) {
                if (k.rit.aangekomen() == null && !k.wachtNu()) {
                    k.rit.stap(v * k.factor, uit);
                }
            }
            for (int i = 1; i < rij.size(); i++) {
                Kar voor = rij.get(i - 1), achter = rij.get(i);
                if (voor.rit.aangekomen() == null && voor.rit.totaal() < stam) {
                    kleinste = Math.min(kleinste, voor.rit.totaal() - achter.rit.totaal());
                }
            }
        }
        return kleinste;
    }

    /** (tests) start with the countdown skipped. */
    public void testNiveau(Niveau n) {
        niveau = n;
    }

    public void testSkipCountdown() {
        if (phase == Phase.AFTELLEN) {
            timer = 1;
        }
    }

    /** (tests) skip the countdown and send no carts by themselves (only the ones the test sends). */
    public void testSpelen() {
        phase = Phase.SPELEN;
        timer = Integer.MAX_VALUE;
        gestuurd = 0;
    }

    // --- the scoreboard -----------------------------------------------------------------------------------------------------

    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<String> boards = new ArrayList<>();
        List<Component> headings = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add(BORD + "_" + n.id());
            headings.add(n.naam());
        }
        Component text = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.karretjes"), boards, headings,
                s -> s + " ✔");
        Scorebord.show(world, npc.position().add(0, 2.6, 0), BORD, text);
    }

    /** Your best shift on this network (-1: never played it). */
    public static int best(Player p, Niveau n) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(BEST + n.id()) ? saved.getIntOr(BEST + n.id(), 0) : -1;
    }

    static Component bestTekst(Player p, Niveau n) {
        int b = best(p, n);
        return b < 0 ? Component.literal("-") : Component.literal(String.valueOf(b));
    }

    /** (debug) a short summary. */
    @Override
    public String toString() {
        return Guhs.MODID + ":karretjes[" + phase + " " + niveau.id() + " goed=" + goed + " kwijt=" + kwijt + "]";
    }
}
