package nl.juiced.guhs.feature.doolhof;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * One run through Het Guhdoolhof. Talk to Meneer Vadskronkel, pick a level, and he grows a brand-new maze
 * ({@link DoolhofKaart}) out of the hedge field, row by row. Then you stand at the start: find all the kaasknabbels
 * the Mika's hid (8 / 12 / 16) and run out of the exit gate in the north. Your score is your time (lower is better,
 * boards {@code doolhof_<niveau>}). The Heg-Mika's ({@link DoolhofMikaEntity}: 1 slow / 2 / 3 faster ones) giggle,
 * pinch one knabbel back when they touch you and hide it somewhere else; on lastig some dead ends hold fake knabbels
 * (Mika-lokaas: +{@value #NEP_STRAF_TICKS} ticks). Nobody gets hurt. One player at a time per maze; the others can
 * watch from the lookout guh on the bridge. The maze stays until the next game.
 */
public final class DoolhofGame {
    /** Actions of the NPC screen: START + level ordinal (0..2), the shop. */
    public static final int START = 0, SHOP = 3;
    public enum Fase { BOUWEN, AFTELLEN, SPELEN }

    public static final int AFTEL_TICKS = 60;
    /** Rows of hedges grown per tick while building. */
    static final int RIJEN_PER_TICK = 2;
    /** A fake knabbel costs this much time. */
    public static final int NEP_STRAF_TICKS = 20 * 5;
    /** The game gives up after this long (no reward: the Mika's keep their knabbels). */
    public static final int MAX_TICKS = 20 * 60 * 12;
    /** Base coins per level; the par times (ticks) for the speed bonus. */
    static final int[] BASIS = {2, 3, 3};
    public static final int[] PAR = {20 * 100, 20 * 170, 20 * 260};
    /** A knabbel is picked up within this distance (horizontally: it floats above the hedges, you walk under it). */
    static final double PAK = 1.25;
    public static final String TAG = "guhs_doolhof";
    /** 2.10: the tag of a fake knabbel (lastig), so a lost one is never taken for a real one. */
    public static final String TAG_NEP = "guhs_doolhof_nep";
    /**
     * 2.10: the knabbels float this high (template y) over their cell, just above the hedges, sparkling: you see where they
     * are from far away, but you still have to find the way there.
     */
    public static final double ZWEEF_Y = DoolhofVeld.HEG_TOT + 1.6;
    /** 2.10: every this many ticks the game checks that all knabbels you still need are really hidden in the maze. */
    public static final int HERSTEL_TICKS = 20;

    private static final Map<UUID, DoolhofGame> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();

    private final UUID npcId;
    private final UUID player;
    private final String playerName;
    public final Niveau niveau;
    public final Anker anker;
    public final DoolhofKaart kaart;
    private final List<List<DoolhofVeld.Blok>> plan;
    private Fase fase = Fase.BOUWEN;
    private int rij, aftel, ticks;
    int inZak, straf, gepikt, nepGevonden;
    /** The knabbels in the maze: entity id -> fake? */
    final Map<UUID, Boolean> knabbels = new HashMap<>();
    final List<UUID> mikas = new ArrayList<>();
    private Vec3 laatsteGoed;
    private long lastTick;
    private int uitgangMelding;

    private DoolhofGame(UUID npcId, ServerPlayer player, Niveau niveau, Anker anker, long seed) {
        this.npcId = npcId;
        this.player = player.getUUID();
        this.playerName = player.getGameProfile().name();
        this.niveau = niveau;
        this.anker = anker;
        this.kaart = new DoolhofKaart(niveau, seed);
        this.plan = DoolhofVeld.plan(anker, kaart);
        this.laatsteGoed = start();
    }

    // --- who plays where -------------------------------------------------------------------------------------------------

    public static boolean isPlaying(Player player) {
        return PLAYERS.containsKey(player.getUUID());
    }

    @Nullable
    public static DoolhofGame of(GuhNpcEntity npc) {
        return GAMES.get(npc.getUUID());
    }

    @Nullable
    static DoolhofGame byNpc(@Nullable UUID npc) {
        return npc == null ? null : GAMES.get(npc);
    }

    @Nullable
    public static DoolhofGame gameOf(Player player) {
        UUID npc = PLAYERS.get(player.getUUID());
        return npc == null ? null : GAMES.get(npc);
    }

    public Fase fase() {
        return fase;
    }

    /** Still going (building, counting down or playing). */
    boolean bezig() {
        return GAMES.get(npcId) == this;
    }

    /** Running through the maze right now (the Mika's hunt only then). */
    boolean speelt() {
        return fase == Fase.SPELEN;
    }

    UUID spelerId() {
        return player;
    }

    public int tijd() {
        return ticks + straf;
    }

    public int inZak() {
        return inZak;
    }

    @Nullable
    ServerPlayer speler(ServerLevel level) {
        return level.getServer().getPlayerList().getPlayer(player);
    }

    Vec3 start() {
        return DoolhofVeld.cel(anker, kaart.startX, kaart.startZ, DoolhofVeld.G + 1);
    }

    // --- the NPC's screen ----------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        DoolhofGame game = of(npc);
        boolean mine = game != null && game.player.equals(player.getUUID());
        GuhQuests.say(player, npc, game == null ? (GuhQuests.saved(player).getBooleanOr(PLAYED_KEY, false) ? "quest.guhs.doolhof.hello_again" : "quest.guhs.doolhof.hello")
                : mine ? "quest.guhs.doolhof.busy_you" : "quest.guhs.doolhof.busy", game == null ? "" : game.playerName);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.9f);
        Adv.grant(player, "doolhof_gevonden");
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game != null);
        if (game != null) {
            data.putString("Player", game.playerName);
            data.putInt("Niveau", game.niveau.ordinal());
            data.putInt("Tijd", game.tijd());
            data.putInt("Zak", game.inZak);
            data.putInt("Wil", DoolhofKaart.aantalKnabbels(game.niveau));
        }
        for (Niveau n : Niveau.values()) {
            data.putInt("Best" + n.ordinal(), best(player, n));
        }
        data.putInt("Games", GuhQuests.saved(player).getIntOr(GAMES_KEY, 0));
        data.putBoolean("Anker", DoolhofVeld.anker(npc) != null);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new DoolhofPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.DOOLHOFGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        if (action == SHOP) {
            GuhQuests.say(player, npc, "quest.guhs.doolhof.shop");
            npc.openShop(player);
        } else if (action >= START && action < START + 3) {
            start(npc, player, Niveau.of(action - START));
        }
    }

    // --- starting --------------------------------------------------------------------------------------------------------

    /** Starts a game on this level, if the maze is free. */
    @Nullable
    public static DoolhofGame start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau) {
        ServerLevel world = (ServerLevel) npc.level();
        DoolhofGame running = of(npc);
        if (player.isSpectator() || !player.isAlive()) {
            return null;
        }
        if (running != null) {
            GuhQuests.say(player, npc, running.player.equals(player.getUUID()) ? "quest.guhs.doolhof.busy_you" : "quest.guhs.doolhof.busy", running.playerName);
            return null;
        }
        if (isPlaying(player) || Minigames.refuse(player, npc, Minigames.DOOLHOF)) {
            return null;
        }
        Anker anker = DoolhofVeld.anker(npc);
        if (anker == null) {
            GuhQuests.say(player, npc, "quest.guhs.doolhof.broken");
            return null;
        }
        DoolhofGame game = new DoolhofGame(npc.getUUID(), player, niveau, anker, world.getRandom().nextLong());
        game.lastTick = world.getGameTime();
        GAMES.put(npc.getUUID(), game);
        PLAYERS.put(player.getUUID(), npc.getUUID());
        player.closeContainer();
        player.stopRiding();
        opruimen(world, anker);
        weg(world, anker, player);
        Minigames.startKeeping(player);
        GuhQuests.say(player, npc, "quest.guhs.doolhof.go." + niveau.id(), DoolhofKaart.aantalKnabbels(niveau), DoolhofKaart.aantalMikas(niveau));
        world.playSound(null, npc.blockPosition(), DoolhofFeature.GROEI.get(), SoundSource.NEUTRAL, 1.2f, 0.9f);
        return game;
    }

    /** Old knabbels and Mika's of an earlier game go. */
    static void opruimen(ServerLevel world, Anker anker) {
        var box = DoolhofVeld.veld(anker).inflate(4);
        for (Entity e : world.getEntitiesOfClass(ItemEntity.class, box, e -> e.entityTags().contains(TAG))) {
            e.discard();
        }
        for (DoolhofMikaEntity m : world.getEntitiesOfClass(DoolhofMikaEntity.class, box)) {
            m.discard();
        }
    }

    /** Walkers in the field while the hedges grow go to the plaza (nobody gets stuck in a hedge). */
    static void weg(ServerLevel world, Anker anker, @Nullable ServerPlayer behalve) {
        Vec3 plein = anker.punt(DoolhofVeld.AX + 0.5, DoolhofVeld.G + 1, DoolhofVeld.AZ + 3.5);
        for (ServerPlayer p : world.players()) {
            if (p == behalve || p.isSpectator() || !DoolhofVeld.inVeld(anker, p.position(), 0)) {
                continue;
            }
            double h = DoolhofVeld.hoogte(anker, p.getY());
            if (h < DoolhofVeld.G + 6) {
                teleport(p, world, plein.x, plein.y, plein.z, p.getYRot());
                p.sendSystemMessage(Component.translatable("quest.guhs.doolhof.opzij").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // --- every tick of the NPC -------------------------------------------------------------------------------------------

    void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        ServerPlayer p = speler(world);
        if (p == null || !p.isAlive() || p.level() != world) {
            stop(world, p, "quest.guhs.doolhof.stopped");
            return;
        }
        Minigames.keep(p);
        switch (fase) {
            case BOUWEN -> bouw(world, p);
            case AFTELLEN -> aftellen(world, p);
            case SPELEN -> spelen(npc, world, p);
        }
    }

    private void bouw(ServerLevel world, ServerPlayer p) {
        for (int i = 0; i < RIJEN_PER_TICK && rij < plan.size(); i++, rij++) {
            List<DoolhofVeld.Blok> blokken = plan.get(rij);
            for (DoolhofVeld.Blok b : blokken) {
                if (world.getBlockState(b.pos()) != b.state()) {
                    world.setBlock(b.pos(), b.state(), Block.UPDATE_CLIENTS);
                }
            }
            if (rij % 4 == 0 && !blokken.isEmpty()) {
                BlockPos m = blokken.get(blokken.size() / 2).pos();
                world.sendParticles(ParticleTypes.COMPOSTER, m.getX() + 0.5, m.getY() + 1.5, m.getZ() + 0.5, 30, 20, 1.5, 0.5, 0.02);
                world.playSound(null, m, DoolhofFeature.GROEI.get(), SoundSource.BLOCKS, 0.7f, 0.8f + world.getRandom().nextFloat() * 0.4f);
            }
        }
        if (rij >= plan.size()) {
            fase = Fase.AFTELLEN;
            weg(world, anker, p);
            Vec3 s = start();
            teleport(p, world, s.x, s.y, s.z, anker.yaw(180f));
            laatsteGoed = s;
            p.sendSystemMessage(Component.translatable("quest.guhs.doolhof.klaar_staan", DoolhofKaart.aantalKnabbels(niveau)).withStyle(ChatFormatting.GREEN));
        }
    }

    private void aftellen(ServerLevel world, ServerPlayer p) {
        aftel++;
        if (aftel == 1 || aftel == 21 || aftel == 41) {
            int n = 3 - (aftel - 1) / 20;
            title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.doolhof.ready"), 0, 22, 2);
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + 0.2f * (3 - n));
        } else if (aftel >= AFTEL_TICKS) {
            fase = Fase.SPELEN;
            title(p, Component.translatable("quest.guhs.doolhof.title.go").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.doolhof.title.go.sub", DoolhofKaart.aantalKnabbels(niveau)), 0, 30, 10);
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.2f);
            verstop(world);
            mikas(world);
        }
    }

    /** The kaasknabbels (and on lastig the fake ones) go into the maze. */
    void verstop(ServerLevel world) {
        for (int[] c : kaart.knabbels) {
            knabbel(world, c[0], c[1], false);
        }
        for (int[] c : kaart.nep) {
            knabbel(world, c[0], c[1], true);
        }
    }

    ItemEntity knabbel(ServerLevel world, int cx, int cz, boolean nep) {
        Vec3 at = DoolhofVeld.cel(anker, cx, cz, ZWEEF_Y);
        ItemStack stack = new ItemStack(DoolhofFeature.GESTOLEN_KNABBEL.get());
        ItemEntity item = new ItemEntity(world, at.x, at.y, at.z, stack, 0, 0, 0);
        item.setNoGravity(true);
        item.setNeverPickUp();
        item.setUnlimitedLifetime();
        item.addTag(TAG);
        if (nep) {
            item.addTag(TAG_NEP);
        }
        world.addFreshEntity(item);
        knabbels.put(item.getUUID(), nep);
        return item;
    }

    /** How many real knabbels are hidden in the maze right now (the ones you still have to find). */
    public int verstopt() {
        return (int) knabbels.values().stream().filter(nep -> !nep).count();
    }

    /**
     * 2.10 (self-heal): every knabbel you still need ((8 / 12 / 16) minus the ones in your bag) is always hidden in the maze.
     * A knabbel whose entity went away (its chunk unloaded for a moment, a Mika hid it where no cell was found, it got
     * removed some other way) is hidden again far from you; a lost one that turns up again is taken back (or goes, when
     * it isn't needed any more). Returns how many were hidden again.
     */
    int herstel(ServerLevel world) {
        knabbels.keySet().removeIf(id -> !(world.getEntity(id) instanceof ItemEntity item) || !item.isAlive());
        int mist = DoolhofKaart.aantalKnabbels(niveau) - inZak - verstopt();
        for (ItemEntity los : world.getEntitiesOfClass(ItemEntity.class, DoolhofVeld.veld(anker).inflate(4),
                e -> e.entityTags().contains(TAG) && !knabbels.containsKey(e.getUUID()))) {
            if (mist > 0 && !los.entityTags().contains(TAG_NEP) && los.isAlive()) {
                knabbels.put(los.getUUID(), false);           // (there it is again)
                mist--;
            } else {
                los.discard();
            }
        }
        ServerPlayer p = speler(world);
        Vec3 bij = p != null ? p.position() : start();
        int opnieuw = 0;
        for (; mist > 0; mist--) {
            Vec3 ver = willekeurigeCel(world.getRandom(), bij, 6, 0, bij);
            int[] cel = ver == null ? null : DoolhofVeld.celVan(anker, ver);
            if (cel == null) {
                List<int[]> cellen = kaart.cellen();
                cel = cellen.get(world.getRandom().nextInt(cellen.size()));
            }
            knabbel(world, cel[0], cel[1], false);
            opnieuw++;
        }
        return opnieuw;
    }

    void mikas(ServerLevel world) {
        double snelheid = switch (niveau) {
            case MAKKELIJK -> 0.17;
            case MEDIUM -> 0.22;
            case LASTIG -> 0.27;
        };
        RandomSource rng = world.getRandom();
        for (int i = 0; i < DoolhofKaart.aantalMikas(niveau); i++) {
            Vec3 at = willekeurigeCel(rng, start(), 9, 0, start());
            if (at == null) {
                continue;
            }
            DoolhofMikaEntity mika = DoolhofMikaEntity.maak(world, npcId, at, snelheid);
            if (mika != null) {
                mikas.add(mika.getUUID());
            }
        }
    }

    private void spelen(GuhNpcEntity npc, ServerLevel world, ServerPlayer p) {
        ticks++;
        Vec3 pos = p.position();
        double h = DoolhofVeld.hoogte(anker, p.getY());
        if (DoolhofVeld.bijUitgang(anker, pos)) {
            uitgang(npc, world, p);
            return;
        }
        if (!DoolhofVeld.inVeld(anker, pos, -0.5) || h < DoolhofVeld.G - 2) {
            stop(world, p, "quest.guhs.doolhof.weggelopen");
            return;
        }
        if (h > DoolhofVeld.HEG_TOT + 1.2) {
            // (on top of the hedges? that's cheating, njeg: back down)
            teleport(p, world, laatsteGoed.x, laatsteGoed.y, laatsteGoed.z, p.getYRot());
            p.sendOverlayMessage(Component.translatable("quest.guhs.doolhof.niet_klimmen").withStyle(ChatFormatting.GOLD));
        } else if (p.onGround() && ticks % 10 == 0) {
            laatsteGoed = pos;
        }
        // every knabbel you still need really is in the maze (none gets lost)
        if (ticks % HERSTEL_TICKS == 0) {
            herstel(world);
        }
        // picking up: walk under the floating knabbel
        boolean opDeGrond = h > DoolhofVeld.G && h < DoolhofVeld.G + 3.5;
        for (var it = knabbels.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (!(world.getEntity(e.getKey()) instanceof ItemEntity item) || !item.isAlive()) {
                continue;                                  // (herstel hides it again)
            }
            double dx = item.getX() - pos.x, dz = item.getZ() - pos.z;
            if (opDeGrond && dx * dx + dz * dz < PAK * PAK) {
                it.remove();
                pak(world, p, item, e.getValue());
            } else if (!e.getValue() && ticks % 10 == (item.getId() & 7)) {
                glinster(world, p, item);
            }
        }
        if (ticks % 5 == 0) {
            Component bar = niveau == Niveau.MAKKELIJK
                    ? Component.translatable("quest.guhs.doolhof.bar_makkelijk", Highscores.tijd(tijd()), inZak, DoolhofKaart.aantalKnabbels(niveau), verstopt())
                    : Component.translatable("quest.guhs.doolhof.bar", Highscores.tijd(tijd()), inZak, DoolhofKaart.aantalKnabbels(niveau));
            p.sendOverlayMessage(bar.copy().withStyle(ChatFormatting.GREEN));
        }
        if (uitgangMelding > 0) {
            uitgangMelding--;
        }
        if (ticks >= MAX_TICKS) {
            stop(world, p, "quest.guhs.doolhof.te_lang");
        }
    }

    /**
     * A real floating knabbel sparkles (golden glitter and a little star): for everyone close by, and for the player from
     * the other end of the maze too (long distance particles).
     */
    private static void glinster(ServerLevel world, ServerPlayer p, ItemEntity item) {
        world.sendParticles(ParticleTypes.WAX_ON, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        world.sendParticles(p, ParticleTypes.END_ROD, true, item.getX(), item.getY() + 0.35, item.getZ(), 2, 0.18, 0.25, 0.18, 0.004);
        world.sendParticles(p, ParticleTypes.WAX_ON, true, item.getX(), item.getY() + 0.3, item.getZ(), 1, 0.15, 0.15, 0.15, 0.01);
    }

    /** A knabbel picked up: a real one into your bag, a fake one costs time. */
    void pak(ServerLevel world, ServerPlayer p, ItemEntity item, boolean nep) {
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "doolhof", !nep); // samen
        item.discard();
        if (nep) {
            nepGevonden++;
            straf += NEP_STRAF_TICKS;
            world.sendParticles(ParticleTypes.POOF, item.getX(), item.getY(), item.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
            world.playSound(null, item.blockPosition(), DoolhofFeature.GIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.6f);
            p.sendOverlayMessage(Component.translatable("quest.guhs.doolhof.nep", NEP_STRAF_TICKS / 20).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        inZak++;
        ItemStack stack = new ItemStack(DoolhofFeature.GESTOLEN_KNABBEL.get());
        p.getInventory().add(stack);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, item.getX(), item.getY() + 0.2, item.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, p.blockPosition(), DoolhofFeature.KNABBEL.get(), SoundSource.PLAYERS, 0.9f, 0.9f + 0.05f * inZak);
        int wil = DoolhofKaart.aantalKnabbels(niveau);
        if (inZak >= wil) {
            title(p, Component.empty(), Component.translatable("quest.guhs.doolhof.alles").withStyle(ChatFormatting.YELLOW), 0, 40, 10);
            world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        } else {
            p.sendOverlayMessage(Component.translatable("quest.guhs.doolhof.gevonden", inZak, wil).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** A Mika touched the player: one knabbel back into the maze, far away (or a tongue, with an empty bag). */
    void gepikt(DoolhofMikaEntity mika, ServerPlayer p) {
        ServerLevel world = (ServerLevel) mika.level();
        mika.giechel();
        world.sendParticles(ParticleTypes.CHERRY_LEAVES, mika.getX(), mika.getY() + 0.6, mika.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        if (inZak <= 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.doolhof.tong").withStyle(ChatFormatting.LIGHT_PURPLE));
            mika.wegrennen(this, p);
            return;
        }
        inZak--;
        gepikt++;
        neemEen(p);
        herstel(world);                                        // (hidden again, far away: always, see herstel)
        p.sendOverlayMessage(Component.translatable("quest.guhs.doolhof.gepikt", inZak, DoolhofKaart.aantalKnabbels(niveau))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        world.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7f, 0.6f);
        mika.wegrennen(this, p);
    }

    /** At the exit gate: out with all the knabbels, or back in for the rest. */
    private void uitgang(GuhNpcEntity npc, ServerLevel world, ServerPlayer p) {
        int wil = DoolhofKaart.aantalKnabbels(niveau);
        if (inZak >= wil) {
            finish(npc, world, p);
            return;
        }
        Vec3 terug = DoolhofVeld.cel(anker, kaart.uitX, kaart.uitZ, DoolhofVeld.G + 1);
        teleport(p, world, terug.x, terug.y, terug.z, anker.yaw(0f));
        if (uitgangMelding == 0) {
            GuhQuests.say(p, npc, "quest.guhs.doolhof.nog_niet", wil - inZak);
            uitgangMelding = 60;
        }
    }

    /** Somewhere in the maze (far from `weg`, at least minVan cells; near `bij`, at most maxBij cells when > 0). */
    @Nullable
    Vec3 willekeurigeCel(RandomSource rng, @Nullable Vec3 weg, int minVan, int maxBij, Vec3 bij) {
        List<int[]> cellen = kaart.cellen();
        for (int poging = 0; poging < 40; poging++) {
            int[] c = cellen.get(rng.nextInt(cellen.size()));
            Vec3 at = DoolhofVeld.cel(anker, c[0], c[1], DoolhofVeld.G + 1);
            if (weg != null && at.distanceTo(weg) < minVan * DoolhofVeld.P) {
                continue;
            }
            if (maxBij > 0 && at.distanceTo(bij) > maxBij * DoolhofVeld.P) {
                continue;
            }
            return at;
        }
        int[] c = cellen.get(rng.nextInt(cellen.size()));
        return DoolhofVeld.cel(anker, c[0], c[1], DoolhofVeld.G + 1);
    }

    /** Is this Mika still in the maze's cells? */
    boolean binnen(Entity e) {
        int[] c = DoolhofVeld.celVan(anker, e.position());
        double h = DoolhofVeld.hoogte(anker, e.getY());
        return c != null && kaart.actief[c[0]][c[1]] && h > DoolhofVeld.G && h < DoolhofVeld.HEG_TOT;
    }

    void zetTerug(DoolhofMikaEntity mika) {
        Vec3 at = willekeurigeCel(mika.getRandom(), null, 0, 0, mika.position());
        if (at != null) {
            mika.getNavigation().stop();
            mika.teleportTo(at.x, at.y, at.z);
        }
    }

    // --- the end ---------------------------------------------------------------------------------------------------------

    /** Coins for a finished run: the level's base plus a speed bonus (par / fast), lastig +50 %, and +1 like every reward. */
    public static int munten(Niveau niveau, int tijd) {
        int par = PAR[niveau.ordinal()];
        int bonus = (tijd <= par ? 1 : 0) + (tijd <= par * 6 / 10 ? 1 : 0);
        return niveau.munten(BASIS[niveau.ordinal()] + bonus) + 1;
    }

    void finish(GuhNpcEntity npc, ServerLevel world, ServerPlayer p) {
        int tijd = tijd();
        end(world, p);
        CompoundTag saved = GuhQuests.saved(p);
        saved.putBoolean(PLAYED_KEY, true);
        saved.putInt(GAMES_KEY, saved.getIntOr(GAMES_KEY, 0) + 1);
        int best = best(p, niveau);
        boolean record = best < 0 || tijd < best;
        if (record) {
            saved.putInt(BEST_KEY + niveau.id(), tijd);
        }
        int munten = munten(niveau, tijd);
        Minigames.give(p, new ItemStack(DoolhofFeature.DOOLHOFKNABBEL.get(), munten));
        Scorebord.submit(p, board(niveau), tijd, true);
        showScores(npc);
        p.sendSystemMessage(Component.translatable("quest.guhs.doolhof.done", niveau.naam(), Highscores.tijd(tijd), gepikt, nepGevonden)
                .withStyle(ChatFormatting.GREEN));
        p.sendSystemMessage(Component.translatable("quest.guhs.doolhof.munten", munten).withStyle(ChatFormatting.YELLOW));
        p.sendSystemMessage((record ? Component.translatable("quest.guhs.doolhof.record", Highscores.tijd(tijd))
                : Component.translatable("quest.guhs.doolhof.best", Highscores.tijd(best))).withStyle(record ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        Adv.grant(p, "doolhof_gevonden");
        Adv.grant(p, "doolhof_gespeeld");
        Adv.grant(p, "doolhof_" + niveau.id());
        if (gepikt == 0 && niveau != Niveau.MAKKELIJK) {
            Adv.grant(p, "doolhof_ongepikt");
        }
        title(p, Component.translatable(record ? "quest.guhs.doolhof.title.record" : "quest.guhs.doolhof.title.end").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal(Highscores.tijd(tijd)), 5, 60, 20);
        GuhQuests.say(p, npc, tijd <= PAR[niveau.ordinal()] ? "quest.guhs.doolhof.end.snel" : "quest.guhs.doolhof.end.ok");
        world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.1f);
        world.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 6, 0.5, 0.3, 0.5, 0);
    }

    /** Walked off, logged out, died, or it took too long: no coins. */
    void stop(ServerLevel world, @Nullable ServerPlayer p, String key) {
        end(world, p);
        if (p != null) {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The game is over: Mika's and leftover knabbels go, the knabbels leave your bag. The maze stays. */
    private void end(ServerLevel world, @Nullable ServerPlayer p) {
        GAMES.remove(npcId, this);
        PLAYERS.remove(player, npcId);
        for (UUID id : mikas) {
            if (world.getEntity(id) instanceof DoolhofMikaEntity m) {
                m.poef();
            }
        }
        for (UUID id : knabbels.keySet()) {
            Entity e = world.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        knabbels.clear();
        if (p != null) {
            cleanup(p);
        }
    }

    /** Takes the gestolen knabbels out of the pockets. */
    public static void cleanup(ServerPlayer p) {
        PLAYERS.remove(p.getUUID());
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(DoolhofFeature.GESTOLEN_KNABBEL.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (p.containerMenu.getCarried().is(DoolhofFeature.GESTOLEN_KNABBEL.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void neemEen(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(DoolhofFeature.GESTOLEN_KNABBEL.get())) {
                inv.getItem(i).shrink(1);
                return;
            }
        }
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && isPlaying(p) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

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

    /** A game whose NPC stopped ticking (his chunk unloaded) is over for its player. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) {
            return;
        }
        if (!isPlaying(p)) {
            if (p.tickCount % 200 == 0 && p.getInventory().contains(new ItemStack(DoolhofFeature.GESTOLEN_KNABBEL.get()))) {
                cleanup(p);                                        // (left over from a game that ended while you were away)
            }
            return;
        }
        DoolhofGame game = gameOf(p);
        if (game == null || p.level().getGameTime() - game.lastTick > 40) {
            if (game != null) {
                game.end(p.level(), p);
            }
            cleanup(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.doolhof.stopped").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        PLAYERS.clear();
    }

    /** Ends this player's game (without rewards). */
    public static void stopFor(ServerPlayer p) {
        DoolhofGame game = gameOf(p);
        if (game != null) {
            game.stop(p.level(), p, "quest.guhs.doolhof.stopped");
        }
        cleanup(p);
    }

    // --- records and helpers ---------------------------------------------------------------------------------------------

    static final String BEST_KEY = "guhs_doolhof_best_", GAMES_KEY = "guhs_doolhof_games", PLAYED_KEY = "guhs_doolhof_played";

    /** The Highscores board of a level: doolhof_makkelijk / doolhof_medium / doolhof_lastig. */
    public static String board(Niveau niveau) {
        return "doolhof_" + niveau.id();
    }

    /** Your best time on this level (ticks), -1 when you never finished it. */
    public static int best(Player player, Niveau niveau) {
        CompoundTag saved = GuhQuests.saved(player);
        return saved.contains(BEST_KEY + niveau.id()) ? saved.getIntOr(BEST_KEY + niveau.id(), 0) : -1;
    }

    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Component text = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.doolhof"),
                List.of(board(Niveau.MAKKELIJK), board(Niveau.MEDIUM), board(Niveau.LASTIG)),
                List.of(Niveau.MAKKELIJK.naam(), Niveau.MEDIUM.naam(), Niveau.LASTIG.naam()), Highscores::tijd);
        Scorebord.show(world, npc.position().add(0, 2.4, 0), "doolhof", text);
        Anker anker = DoolhofVeld.anker(npc);
        if (anker != null) {
            // the second board hangs over the exit gate, where you come out
            Scorebord.show(world, anker.punt(DoolhofVeld.FX + DoolhofVeld.P * DoolhofKaart.MIDDEN + 2.0, DoolhofVeld.G + 6.2, DoolhofVeld.FZ - 4.5),
                    "doolhof_uitgang", text);
        }
    }

    static void teleport(ServerPlayer p, ServerLevel world, double x, double y, double z, float yRot) {
        if (p instanceof FakePlayer || p.connection == null) {
            p.snapTo(x, y, z, yRot, 0);
        } else {
            p.teleportTo(world, x, y, z, yRot, 0);
        }
    }

    static void title(ServerPlayer p, Component title, Component subtitle, int in, int stay, int out) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** (Tests) grow the whole maze at once and skip the countdown. */
    void meteen(ServerLevel world, ServerPlayer p) {
        while (fase == Fase.BOUWEN) {
            bouw(world, p);
        }
        aftel = AFTEL_TICKS - 1;
        aftellen(world, p);
    }
}
