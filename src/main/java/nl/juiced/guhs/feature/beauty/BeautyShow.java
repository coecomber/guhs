package nl.juiced.guhs.feature.beauty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.core.UUIDUtil;
/**
 * The Guh Beauty Vads-wedstrijd: one show in the beauty theatre, run by its Showguh. A show has three rounds; every
 * round the Showguh picks a theme, the performer dresses the model guh on the guh-face stage from the loaner wardrobe
 * (a screen: the clothes only ever live on the model, nothing comes into your inventory), the model walks the catwalk
 * and the three jury guhs hold up their scores ({@link BeautyJury}). Scores become showrozetten; the best show total is
 * the record. The model is a temporary guh, or (if you like) your own tamed guh: it's borrowed for the show (no owner,
 * no AI, invulnerable) and gets its own clothes and owner back afterwards, even after a crash (see {@link #giveBack}).
 * <p>
 * One show per theatre at a time; shows live in memory only (a restart cleans up leftovers through the join event).
 */
public final class BeautyShow {
    /** Actions from the screens. */
    public static final int START = 0, START_OWN = 1, SHOP = 2, DRESS = 3, UNDRESS = 4, READY = 5, QUIT = 6, WARDROBE = 7;
    public static final int ROUNDS = 3, MAX_ROUND = 30;
    /** DRESS_TICKS is medium's dressing time; see {@link #kleedTijd(Niveau)}. */
    public static final int COUNTDOWN_TICKS = 60, DRESS_TICKS = 20 * 45, POSE_TICKS = 24, JURY_STEP = 30, AFTER_JURY = 40;
    public static final int JURY_TICKS = POSE_TICKS + 3 * JURY_STEP + AFTER_JURY;
    public static final double WALK_SPEED = 0.2;
    /** A show total that counts as "good" (quest), and a round that sends the jury into ecstasy. */
    public static final int GOOD_SHOW = 65, EXTASE_ROUND = 28;
    /** Extra show rosettes for the very first show. */
    public static final int FIRST_ROSETTES = 4;
    /** The performer has to stay this close to the Showguh; the theatre (for messages) is this big. */
    public static final int REACH = 72, THEATRE = 64;
    static final String MODEL_TAG = "guhs_beauty_model", JURY_TAG = "guhs_beauty_jury", SCORE_TAG = "guhs_beauty_score";
    static final String AUDIENCE_TAG = "guhs_beauty_publiek";
    static final String LOAN = "guhs_beauty_loan";
    /** The world's top 3 of best show totals (see {@link #showScores}). */
    public static final String SCOREBORD = "beauty_show";
    private static final String BEST = "guhs_beauty_best", SHOWS_PLAYED = "guhs_beauty_shows", FIRST = "guhs_beauty_first";
    /** Only common looks: a model must not fill in anybody's Guhdex. */
    private static final GuhVariant[] MODEL_LOOKS = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};
    private static final String[] MODEL_NAMES = {"Vadsy", "Guhlia", "Knabbeline", "Bolleke", "Pluisje", "Vadsiena", "Guhnther", "Mollie", "Kaasje", "Poekie"};
    /** The catwalk tune (semitones from F#), four ticks a note. */
    private static final int[] MELODY = {0, 4, 7, 12, 7, 4, 0, -5, 0, 4, 7, 11, 12, 11, 7, 4, 2, 5, 9, 12, 9, 5, 2, -3};

    public enum Phase { COUNTDOWN, DRESS, WALK, JURY }

    /** Running shows by Showguh, and who performs where. */
    private static final Map<UUID, BeautyShow> SHOWS = new HashMap<>();
    private static final Map<UUID, UUID> PERFORMERS = new HashMap<>();

    private final ServerLevel level;
    private final UUID npcId;
    private final UUID player;
    private final Component playerName;
    private final boolean ownGuh;
    private final UUID model;
    private final Component modelName;
    private final BlockPos modelSpot, endSpot;
    private final List<ShowTheme> themes;
    /** 2.9: your unlocked pieces fill up the dressing screen's rows (this many icons per row, see client.DressScreen). */
    public static final int OWN_PER_SLOT = 11;
    /** Your own pieces: what your own guh wore, plus your unlocked clothes (2.9). They're in the wardrobe for this show too. */
    private final List<GuhClothes> ownPieces;
    private final List<UUID> jury;
    private final List<UUID> displays = new ArrayList<>();
    private int round;
    private Phase phase = Phase.COUNTDOWN;
    private int timer;
    private int walkTicks;
    private int total, rosettes, bestRound;
    /** The level (2.9): the dressing time and how strict the jury is. */
    private Niveau niveau = Niveau.MEDIUM;
    @Nullable
    private BeautyJury.Verdict verdict;
    private long lastTick;

    private BeautyShow(ServerLevel level, GuhNpcEntity npc, ServerPlayer player, boolean ownGuh, GuhEntity model, Component modelName,
                       BlockPos[] spots, List<ShowTheme> themes, List<GuhClothes> ownPieces) {
        this.level = level;
        this.npcId = npc.getUUID();
        this.player = player.getUUID();
        this.playerName = player.getDisplayName();
        this.ownGuh = ownGuh;
        this.model = model.getUUID();
        this.modelName = modelName;
        this.modelSpot = spots[0];
        this.endSpot = spots[1];
        this.themes = themes;
        this.ownPieces = ownPieces;
        this.jury = findJury(level, spots[1]);
        this.lastTick = level.getGameTime();
    }

    // --- lookups ------------------------------------------------------------------------------------------------------

    /** The show this player is performing in (or null). */
    @Nullable
    public static BeautyShow of(Player player) {
        UUID npc = PERFORMERS.get(player.getUUID());
        return npc == null ? null : SHOWS.get(npc);
    }

    /** The show running at this Showguh (or null). */
    @Nullable
    public static BeautyShow at(GuhNpcEntity npc) {
        return SHOWS.get(npc.getUUID());
    }

    public static boolean isPerforming(Player player) {
        return PERFORMERS.containsKey(player.getUUID());
    }

    /** Is this performer in the theatre of their show (only there can't they get hurt)? */
    public static boolean inShowArea(Player player) {
        BeautyShow show = of(player);
        if (show == null || !(player.level() instanceof ServerLevel world)) {
            return false;
        }
        if (world.dimension() == nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            return BeautyProtection.inTheatre(world, player.blockPosition());
        }
        return world == show.level && player.blockPosition().distSqr(show.modelSpot) <= THEATRE * THEATRE;   // (a theatre built elsewhere)
    }

    /** Is this guh the model of a running show? */
    public static boolean isModel(Entity guh) {
        return SHOWS.values().stream().anyMatch(s -> s.model.equals(guh.getUUID()));
    }

    static boolean isScoreCard(Entity display) {
        return SHOWS.values().stream().anyMatch(s -> s.displays.contains(display.getUUID()));
    }

    public boolean isDressing() {
        return phase == Phase.DRESS;
    }

    public Phase phase() {
        return phase;
    }

    public int round() {
        return round;
    }

    public ShowTheme theme() {
        return themes.get(Math.min(round, themes.size() - 1));
    }

    public int total() {
        return total;
    }

    public int rosettes() {
        return rosettes;
    }

    public boolean withOwnGuh() {
        return ownGuh;
    }

    public Niveau niveau() {
        return niveau;
    }

    /** The time to dress the model on a level: makkelijk a whole minute, medium 45 seconds, lastig 30. */
    public static int kleedTijd(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 20 * 60;
            case MEDIUM -> DRESS_TICKS;
            case LASTIG -> 20 * 30;
        };
    }

    @Nullable
    public BeautyJury.Verdict lastVerdict() {
        return verdict;
    }

    @Nullable
    public GuhEntity model() {
        return level.getEntity(model) instanceof GuhEntity guh ? guh : null;
    }

    @Nullable
    private ServerPlayer performer() {
        return level.getServer().getPlayerList().getPlayer(player);
    }

    // --- talking to the Showguh ------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        BeautyShow show = at(npc);
        if (show != null && show.player.equals(player.getUUID())) {
            if (show.isDressing()) {
                show.openWardrobe(player);
            } else {
                GuhQuests.say(player, npc, "quest.guhs.beauty.busy_self");
            }
            return;
        }
        npc.level().playSound(null, npc, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.25f);
        CompoundTag saved = GuhQuests.saved(player);
        GuhQuests.say(player, npc, show != null ? "quest.guhs.beauty.running"
                : saved.getBooleanOr(FIRST, false) ? "quest.guhs.beauty.hello" : "quest.guhs.beauty.hello_first");
        CompoundTag data = new CompoundTag();
        data.putString("Mode", "lobby");
        data.putBoolean("Running", show != null);
        if (show != null) {
            data.putString("Performer", show.playerName.getString());
            data.putInt("Round", show.round + 1);
            data.putString("Theme", show.theme().id());
            data.putString("Niveau", show.niveau.id());
        }
        data.putInt("Best", best(player));
        Klassiekers.records(data, n -> best(player, n));
        data.putInt("Shows", saved.getIntOr(SHOWS_PLAYED, 0));
        GuhEntity own = ownGuh(player);
        data.putString("OwnGuh", own == null ? "" : own.getDisplayName().getString());
        ModNetworking.sendTo(player, new BeautyPayloads.Open(npc.getId(), data));
    }

    /** A button in one of the screens. */
    public static void action(GuhNpcEntity npc, ServerPlayer player, int action, int value) {
        if (npc.getKind() != GuhNpcEntity.Kind.SHOWGUH || player.level() != npc.level() || player.distanceToSqr(npc) > REACH * REACH) {
            return;
        }
        BeautyShow show = at(npc);
        int a = Klassiekers.actie(action);
        switch (a) {
            case START, START_OWN -> {
                if (player.distanceToSqr(npc) <= 16 * 16) {
                    start(npc, player, a == START_OWN, Klassiekers.niveau(action));
                }
            }
            case SHOP -> npc.openShop(player);
            default -> {
                if (show != null && show.player.equals(player.getUUID())) {
                    show.dressAction(player, a, value);
                }
            }
        }
    }

    // --- starting --------------------------------------------------------------------------------------------------------

    /** Starts a show on medium (returns it, or null when that's not possible: then the Showguh says why). */
    @Nullable
    public static BeautyShow start(GuhNpcEntity npc, ServerPlayer player, boolean withOwnGuh) {
        return start(npc, player, withOwnGuh, Niveau.MEDIUM);
    }

    /** Starts a show on this level (returns it, or null when that's not possible: then the Showguh says why). */
    @Nullable
    public static BeautyShow start(GuhNpcEntity npc, ServerPlayer player, boolean withOwnGuh, Niveau niveau) {
        ServerLevel world = (ServerLevel) npc.level();
        if (at(npc) != null) {
            GuhQuests.say(player, npc, "quest.guhs.beauty.running");
            return null;
        }
        if (isPerforming(player)) {
            GuhQuests.say(player, npc, "quest.guhs.beauty.busy_self");
            return null;
        }
        if (nl.juiced.guhs.feature.Minigames.refuse(player, npc, nl.juiced.guhs.feature.Minigames.BEAUTY)) {
            return null;
        }
        BlockPos[] spots = spots(npc);
        if (spots == null) {
            GuhQuests.say(player, npc, "quest.guhs.beauty.broken");
            return null;
        }
        GuhEntity guh;
        Component name;
        List<GuhClothes> ownPieces = new ArrayList<>();
        if (withOwnGuh) {
            guh = ownGuh(player);
            if (guh == null) {
                GuhQuests.say(player, npc, "quest.guhs.beauty.no_own_guh");
                return null;
            }
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                if (guh.getClothes(slot) != null) {
                    ownPieces.add(guh.getClothes(slot));
                }
            }
            name = guh.getDisplayName();
        } else {
            guh = ModEntities.GUH.get().create(world, EntitySpawnReason.TRIGGERED);
            if (guh == null) {
                return null;
            }
            guh.snapTo(spots[0].getX() + 0.5, spots[0].getY(), spots[0].getZ() + 0.5, 0, 0);
            guh.finalizeSpawn(world, world.getCurrentDifficultyAt(spots[0]), EntitySpawnReason.EVENT, null);
            guh.setVariant(MODEL_LOOKS[world.getRandom().nextInt(MODEL_LOOKS.length)]);
            guh.setGuhScale(1.1f);
            guh.takeOffClothes();
            guh.setNoAi(true);
            guh.setInvulnerable(true);
            guh.setPersistenceRequired();
            guh.addTag(MODEL_TAG);
            name = Component.literal(MODEL_NAMES[world.getRandom().nextInt(MODEL_NAMES.length)]);
            guh.setCustomName(Component.translatable("entity.guhs.beauty_model", name));
        }
        List<ShowTheme> themes = new ArrayList<>(List.of(ShowTheme.values()));
        // 2.9: "your own pieces" are the clothes you unlocked (feature.kleding), both with your own guh and with a model;
        // the loaner wardrobe itself stays the same (and your unlocked loaner pieces are marked as your own)
        for (GuhClothes c : ShowTheme.loaners()) {
            if (nl.juiced.guhs.feature.kleding.KledingUnlocks.heeft(player, c) && !ownPieces.contains(c)) {
                ownPieces.add(c);
            }
        }
        for (GuhClothes c : nl.juiced.guhs.feature.kleding.KledingUnlocks.eigenStukken(player, List.of(ShowTheme.SLOTS), OWN_PER_SLOT, ownPieces)) {
            if (!ownPieces.contains(c)) {
                ownPieces.add(c);
            }
        }
        Collections.shuffle(themes, new java.util.Random(world.getRandom().nextLong()));
        BeautyShow show = new BeautyShow(world, npc, player, withOwnGuh, guh, name, spots, List.copyOf(themes.subList(0, ROUNDS)), ownPieces);
        SHOWS.put(npc.getUUID(), show);
        PERFORMERS.put(player.getUUID(), npc.getUUID());
        if (withOwnGuh) {
            borrow(guh, npc);
        } else {
            world.addFreshEntity(guh);
        }
        GuhQuests.say(player, npc, "quest.guhs.beauty.start", name);
        show.niveau = niveau;
        if (niveau != Niveau.MEDIUM) {
            GuhQuests.say(player, npc, "quest.guhs.klassiekers.beauty." + niveau.id(), kleedTijd(niveau) / 20);
        }
        nl.juiced.guhs.feature.Minigames.startKeeping(player);
        show.beginRound();
        return show;
    }

    /** The nearest tamed guh of this player that could walk the catwalk. */
    @Nullable
    public static GuhEntity ownGuh(ServerPlayer player) {
        List<GuhEntity> guhs = player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(24),
                g -> g.isAlive() && g.isOwnedBy(player) && !g.isPassenger() && !g.getPersistentData().contains(LOAN));
        guhs.sort(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(player)));
        return guhs.isEmpty() ? null : guhs.get(0);
    }

    /** Your own guh joins the show: it remembers who it belongs to and what it wore, then it's all show guh. */
    private static void borrow(GuhEntity guh, GuhNpcEntity npc) {
        CompoundTag loan = new CompoundTag();
        if (guh.getOwnerUUID() != null) {
            loan.store("Owner", UUIDUtil.CODEC, guh.getOwnerUUID());
        }
        for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
            GuhClothes worn = guh.getClothes(slot);
            loan.putString(slot.name(), worn == null ? "" : worn.id());
        }
        loan.putBoolean("NoAi", guh.isNoAi());
        loan.putBoolean("Invulnerable", guh.isInvulnerable());
        loan.putBoolean("Sitting", guh.isOrderedToSit());
        loan.store("Npc", UUIDUtil.CODEC, npc.getUUID());
        guh.getPersistentData().put(LOAN, loan);
        guh.ejectPassengers();
        guh.setOwnerUUID(null);            // nobody can undress it, pick it up or ride it now
        guh.setNoAi(true);
        guh.setInvulnerable(true);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
    }

    /** Gives a borrowed guh back its owner, its own clothes and its own ways (after the show, or found after a crash). */
    public static void giveBack(GuhEntity guh) {
        CompoundTag loan = guh.getPersistentData().getCompoundOrEmpty(LOAN);
        if (loan.isEmpty()) {
            return;
        }
        for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
            guh.takeOff(slot);                 // (loaner clothes go back into the wardrobe)
            GuhClothes own = GuhClothes.byId(loan.getStringOr(slot.name(), ""));
            if (own != null && own.slot == slot) {
                guh.wear(own);
            }
        }
        if (loan.read("Owner", UUIDUtil.CODEC).isPresent()) {
            guh.setOwnerUUID(loan.read("Owner", UUIDUtil.CODEC).orElseThrow());
        }
        guh.setNoAi(loan.getBooleanOr("NoAi", false));
        guh.setInvulnerable(loan.getBooleanOr("Invulnerable", false));
        guh.setOrderedToSit(loan.getBooleanOr("Sitting", false));
        guh.setInSittingPose(loan.getBooleanOr("Sitting", false));
        guh.getPersistentData().remove(LOAN);
    }

    /** The stage (model spot) and the end of the catwalk, found once by their markers around the Showguh. */
    @Nullable
    public static BlockPos[] spots(GuhNpcEntity npc) {
        CompoundTag data = npc.roleData;
        if (data.contains("Model") && data.contains("Einde")) {
            BlockPos a = BlockPos.of(data.getLongOr("Model", 0L)), b = BlockPos.of(data.getLongOr("Einde", 0L));
            if (npc.level().getBlockState(a).is(BeautyFeature.PLEK.get()) && npc.level().getBlockState(b).is(BeautyFeature.PLEK.get())) {
                return new BlockPos[]{a, b};
            }
        }
        BlockPos modelPos = null, endPos = null;
        BlockPos c = npc.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-64, -10, -64), c.offset(64, 10, 64))) {
            var state = npc.level().getBlockState(pos);
            if (state.is(BeautyFeature.PLEK.get())) {
                BlockPos found = pos.immutable();
                if (state.getValue(BeautyBlocks.Plek.SPOT) == BeautyBlocks.Spot.MODEL) {
                    if (modelPos == null || found.distSqr(c) < modelPos.distSqr(c)) {
                        modelPos = found;
                    }
                } else if (endPos == null || found.distSqr(c) < endPos.distSqr(c)) {
                    endPos = found;
                }
            }
        }
        if (modelPos == null || endPos == null) {
            return null;
        }
        data.putLong("Model", modelPos.asLong());
        data.putLong("Einde", endPos.asLong());
        return new BlockPos[]{modelPos, endPos};
    }

    private static List<UUID> findJury(ServerLevel world, BlockPos end) {
        List<UUID> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String tag = JURY_TAG + i;
            List<GuhEntity> found = world.getEntitiesOfClass(GuhEntity.class, new AABB(end).inflate(24, 10, 24), g -> g.entityTags().contains(tag));
            list.add(found.isEmpty() ? null : found.get(0).getUUID());
        }
        return list;
    }

    @Nullable
    private GuhEntity judge(int i) {
        UUID id = jury.get(i);
        return id != null && level.getEntity(id) instanceof GuhEntity guh ? guh : null;
    }

    // --- dressing -------------------------------------------------------------------------------------------------------

    /** Is this piece in the wardrobe for this show? (The loaner wardrobe, plus what your own guh came in with.) */
    public boolean allowed(GuhClothes clothes) {
        return ShowTheme.isLoaner(clothes) || ownPieces.contains(clothes);
    }

    private void dressAction(ServerPlayer p, int action, int value) {
        if (action == QUIT) {
            end(false);
            return;
        }
        if (action == WARDROBE) {
            if (isDressing()) {
                openWardrobe(p);
            }
            return;
        }
        GuhEntity m = model();
        if (!isDressing() || m == null) {
            return;
        }
        if (action == DRESS) {
            GuhClothes clothes = GuhClothes.byIndex(value);
            if (clothes == null || !allowed(clothes) || !java.util.Arrays.asList(ShowTheme.SLOTS).contains(clothes.slot)) {
                return;
            }
            m.takeOff(clothes.slot);
            m.wear(clothes);
            level.playSound(null, m, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1f, 1.2f);
            m.triggerAnim("action", "happy");
            level.sendParticles(ParticleTypes.WAX_OFF, m.getX(), m.getY() + 0.8, m.getZ(), 8, 0.4, 0.4, 0.4, 0.5);
        } else if (action == UNDRESS && value >= 0 && value < GuhClothes.Slot.kleding().size()) {
            GuhClothes.Slot slot = GuhClothes.Slot.kleding().get(value);
            if (java.util.Arrays.asList(ShowTheme.SLOTS).contains(slot)) {
                m.takeOff(slot);
                level.playSound(null, m, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.8f, 0.9f);
            }
        } else if (action == READY) {
            timer = 0;
            startWalk();
        }
    }

    /** What the model wears in the contest slots right now. */
    public List<GuhClothes> worn() {
        List<GuhClothes> list = new ArrayList<>();
        GuhEntity m = model();
        if (m != null) {
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                if (m.getClothes(slot) != null) {
                    list.add(m.getClothes(slot));
                }
            }
        }
        return list;
    }

    /** Opens the loaner wardrobe screen (while dressing). */
    public void openWardrobe(ServerPlayer p) {
        GuhEntity m = model();
        if (m == null || !isDressing()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putString("Mode", "dress");
        data.putInt("Model", m.getId());
        data.putString("Theme", theme().id());
        data.putInt("Round", round + 1);
        data.putInt("Rounds", ROUNDS);
        data.putInt("Ticks", timer);
        List<GuhClothes> wardrobe = new ArrayList<>(ShowTheme.loaners());
        ownPieces.stream().filter(c -> !wardrobe.contains(c)).forEach(wardrobe::add);
        data.putIntArray("Wardrobe", wardrobe.stream().mapToInt(Enum::ordinal).toArray());
        data.putIntArray("Own", ownPieces.stream().mapToInt(Enum::ordinal).toArray());
        GuhNpcEntity npc = level.getEntity(npcId) instanceof GuhNpcEntity n ? n : null;
        ModNetworking.sendTo(p, new BeautyPayloads.Open(npc == null ? -1 : npc.getId(), data));
    }

    private static void closeScreen(@Nullable ServerPlayer p) {
        if (p != null) {
            CompoundTag data = new CompoundTag();
            data.putString("Mode", "close");
            ModNetworking.sendTo(p, new BeautyPayloads.Open(-1, data));
        }
    }

    // --- the show -----------------------------------------------------------------------------------------------------------

    /** Every tick, from the Showguh. */
    public void tick(GuhNpcEntity npc) {
        lastTick = level.getGameTime();
        ServerPlayer p = performer();
        if (npc.tickCount % 20 == 0) {
            if (p == null || !p.isAlive() || p.level() != level || p.distanceToSqr(npc) > REACH * REACH) {
                end(false);
                return;
            }
            refresh(p);
        }
        GuhEntity m = model();
        if (m == null) {
            if (level.isLoaded(modelSpot)) {
                end(false);           // the model is gone (killed with a command?): no show without a model
            }
            return;
        }
        switch (phase) {
            case COUNTDOWN -> countdown(p);
            case DRESS -> dress(p, m);
            case WALK -> walk(m);
            case JURY -> jury(p, m);
        }
    }

    /** Plays the rest of the current phase at once (for tests: a whole show in a few calls). */
    public void skip(GuhNpcEntity npc) {
        Phase from = phase;
        int r = round;
        switch (phase) {
            case COUNTDOWN -> timer = 1;
            case DRESS -> {
                startWalk();
                return;
            }
            case WALK -> walkTicks = 1000000;
            case JURY -> {
            }
        }
        for (int i = 0; i < JURY_TICKS + 5 && phase == from && round == r && SHOWS.get(npcId) == this; i++) {
            tick(npc);
        }
    }

    private void beginRound() {
        phase = Phase.COUNTDOWN;
        timer = COUNTDOWN_TICKS;
        verdict = null;
        clearScoreCards();
        GuhEntity m = model();
        if (m != null) {
            Vec3 at = Vec3.atBottomCenterOf(modelSpot);
            m.setPos(at.x, at.y, at.z);
            face(m, Vec3.atBottomCenterOf(endSpot));
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                m.takeOff(slot);                         // a fresh start every round
            }
            level.sendParticles(ParticleTypes.HEART, at.x, at.y + 1, at.z, 6, 0.4, 0.3, 0.4, 0.1);
        }
        tellAll(Component.translatable("quest.guhs.beauty.round", round + 1, ROUNDS,
                themeName(theme()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void countdown(@Nullable ServerPlayer p) {
        if (timer % 20 == 0 && timer > 0 && p != null) {
            title(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.beauty.theme", themeName(theme())).withStyle(ChatFormatting.LIGHT_PURPLE), 20);
            sound(Vec3.atCenterOf(modelSpot), SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1f);
        }
        if (--timer <= 0) {
            phase = Phase.DRESS;
            timer = kleedTijd(niveau);
            sound(Vec3.atCenterOf(modelSpot), SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
            if (p != null) {
                title(p, Component.translatable("quest.guhs.beauty.dress_title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("quest.guhs.beauty.theme", themeName(theme())).withStyle(ChatFormatting.GOLD), 40);
                p.sendSystemMessage(Component.translatable("quest.guhs.beauty.dress_help").withStyle(ChatFormatting.GRAY));
                openWardrobe(p);
            }
        }
    }

    private void dress(@Nullable ServerPlayer p, GuhEntity m) {
        if (timer % 20 == 0 && p != null) {
            p.displayClientMessage(Component.translatable("quest.guhs.beauty.bar", round + 1, ROUNDS, themeName(theme()), time(timer),
                    worn().size()).withStyle(timer <= 200 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE)
                    .append("  ").append(Klassiekers.naam(niveau)), true);
            if (timer <= 100 && timer > 0) {
                sound(m.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.4f);
            }
        }
        if (timer % 10 == 0) {
            level.sendParticles(sparkle(), m.getX(), m.getY() + 1.2, m.getZ(), 2, 0.5, 0.5, 0.5, 0);
        }
        if (--timer <= 0) {
            startWalk();
        }
    }

    private void startWalk() {
        phase = Phase.WALK;
        walkTicks = 0;
        ServerPlayer p = performer();
        closeScreen(p);
        if (p != null) {
            title(p, Component.translatable("quest.guhs.beauty.walk_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.beauty.theme", themeName(theme())).withStyle(ChatFormatting.LIGHT_PURPLE), 40);
        }
        tellAll(Component.translatable(worn().isEmpty() ? "quest.guhs.beauty.walk_naked" : "quest.guhs.beauty.walk", modelName, themeName(theme()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void walk(GuhEntity m) {
        Vec3 from = Vec3.atBottomCenterOf(modelSpot), to = Vec3.atBottomCenterOf(endSpot);
        double length = Math.max(0.01, from.distanceTo(to));
        double t = Math.min(1, ++walkTicks * WALK_SPEED / length);
        Vec3 at = from.lerp(to, t);
        m.setPos(at.x, at.y, at.z);
        face(m, to);
        if (walkTicks % 4 == 0) {                          // the catwalk tune, with a bass every other note
            int note = MELODY[(walkTicks / 4) % MELODY.length];
            sound(at, SoundEvents.NOTE_BLOCK_BELL.value(), 0.9f, (float) Math.pow(2, note / 12.0));
            if (walkTicks % 8 == 0) {
                sound(at, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, (float) Math.pow(2, (Math.floorMod(note, 12) - 12) / 12.0));
            }
        }
        if (walkTicks % 2 == 0) {
            level.sendParticles(sparkle(), at.x, at.y + 0.2, at.z, 2, 0.3, 0.05, 0.3, 0);
        }
        if (walkTicks % 10 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 3.5, at.z, 3, 0.2, 0.1, 0.2, 0.01); // the spotlight follows her
        }
        if (t >= 1) {
            phase = Phase.JURY;
            timer = JURY_TICKS;
            verdict = BeautyJury.judge(theme(), worn(), ownGuh, level.getRandom(), niveau);
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 1, at.z, 20, 0.4, 0.5, 0.4, 0.08);
            sound(at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1f);
            tellAll(Component.translatable("quest.guhs.beauty.pose", modelName).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
    }

    private void jury(@Nullable ServerPlayer p, GuhEntity m) {
        int elapsed = JURY_TICKS - timer;
        if (elapsed < POSE_TICKS) {                         // a twirl for the jury
            m.setYRot(m.getYRot() + 360f / POSE_TICKS);
            m.setYBodyRot(m.getYRot());
            m.setYHeadRot(m.getYRot());
        } else if (elapsed == POSE_TICKS) {
            face(m, Vec3.atBottomCenterOf(modelSpot).reverse().add(Vec3.atBottomCenterOf(endSpot).scale(2)));
        }
        for (int i = 0; i < 3; i++) {
            if (elapsed == POSE_TICKS + i * JURY_STEP) {
                reveal(i, p);
            }
        }
        if (elapsed == POSE_TICKS + 3 * JURY_STEP) {
            roundResult(p, m);
        }
        if (--timer <= 0) {
            if (round + 1 < ROUNDS) {
                round++;
                beginRound();
            } else {
                finish();
            }
        }
    }

    /** Jury member i holds up a score card. */
    private void reveal(int i, @Nullable ServerPlayer p) {
        if (verdict == null) {
            return;
        }
        nl.juiced.guhs.feature.samen.SamenSpel.uitslag(p, "beauty", verdict.scores()[i] >= 5); // samen
        int score = verdict.scores()[i];
        String judgeId = BeautyJury.JUDGES.get(i);
        GuhEntity judge = judge(i);
        Vec3 at = judge != null ? judge.position().add(0, judge.getBbHeight() + 1.1, 0)
                : Vec3.atBottomCenterOf(endSpot).add((i - 1) * 1.6, 3, 0);
        scoreCard(at, score);
        sound(at, SoundEvents.NOTE_BLOCK_BELL.value(), 1.2f, 0.5f + score * 0.1f);
        if (score >= 8) {
            sound(at, ModSounds.GUH_HAPPY.get(), 1f, 1.1f);
            level.sendParticles(ParticleTypes.HEART, at.x, at.y - 0.6, at.z, 6, 0.4, 0.3, 0.4, 0.1);
        } else if (score >= 5) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y - 0.6, at.z, 8, 0.4, 0.3, 0.4, 0.1);
        } else {
            sound(at, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1f, 0.6f);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y - 0.6, at.z, 10, 0.3, 0.3, 0.3, 0.02);
        }
        if (judge != null && score >= 7) {
            judge.triggerAnim("action", "happy");
        }
        MutableComponent name = Component.translatable("entity.guhs.beauty_jury." + judgeId);
        tellAll(Component.literal("<").append(name).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(score + "! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(BeautyJury.reason(i, verdict, theme(), worn(), modelName, level.getRandom()).copy().withStyle(ChatFormatting.WHITE)));
        if (p != null) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.beauty.jury_bar", name, score).withStyle(ChatFormatting.GOLD));
        }
    }

    private void roundResult(@Nullable ServerPlayer p, GuhEntity m) {
        if (verdict == null) {
            return;
        }
        int points = verdict.total(), got = verdict.rosettes() > 0 ? niveau.munten(verdict.rosettes()) : 0;
        total += points;
        rosettes += got;
        bestRound = Math.max(bestRound, points);
        Vec3 at = m.position();
        tellAll(Component.translatable("quest.guhs.beauty.round_total", modelName, points, MAX_ROUND, got).withStyle(ChatFormatting.GOLD));
        if (p != null) {
            if (got > 0) {
                give(p, new ItemStack(BeautyFeature.SHOWROZET.get(), got));
            }
            title(p, Component.literal(points + " / " + MAX_ROUND).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.beauty.rosettes", got).withStyle(ChatFormatting.LIGHT_PURPLE), 40);
            if (points >= EXTASE_ROUND) {
                GuhAdvancements.grant(p, "beauty_extase");
                p.sendSystemMessage(Component.translatable("quest.guhs.beauty.extase").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        }
        if (points >= 16) {
            cheer();
        }
        if (points >= 22) {
            sound(at, SoundEvents.PLAYER_LEVELUP, 1f, 1.3f);
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 2, at.z, 40, 1, 1, 1, 0.12);
        } else {
            sound(at, SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, points >= 15 ? 1.2f : 0.7f);
        }
    }

    /** The audience guhs on the benches jump for joy. */
    private void cheer() {
        for (GuhEntity fan : level.getEntitiesOfClass(GuhEntity.class, new AABB(modelSpot).inflate(THEATRE, 16, THEATRE),
                g -> g.entityTags().contains(AUDIENCE_TAG))) {
            fan.triggerAnim("action", "happy");
            level.sendParticles(ParticleTypes.HEART, fan.getX(), fan.getY() + 1, fan.getZ(), 3, 0.3, 0.2, 0.3, 0.1);
            if (level.getRandom().nextInt(3) == 0) {
                sound(fan.position(), ModSounds.GUH_HAPPY.get(), 0.8f, 1.1f + level.getRandom().nextFloat() * 0.3f);
            }
        }
    }

    /** The end of the third round: bonus rosettes, the record, the first-show present and the advancements. */
    private void finish() {
        ServerPlayer p = performer();
        if (p != null) {
            // the finish bonus only for a show that was actually dressed (three naked walks earn nothing: no AFK farming)
            int bonus = finishBonus(rosettes, total);
            bonus = bonus > 0 ? niveau.munten(bonus) : 0;
            rosettes += bonus;
            if (bonus > 0) {
                give(p, new ItemStack(BeautyFeature.SHOWROZET.get(), bonus));
            }
            CompoundTag saved = GuhQuests.saved(p);
            int best = best(p, niveau);
            boolean record = total > best;
            if (record) {
                saved.putInt(Klassiekers.sleutel(BEST, niveau), total);
            }
            saved.putInt(SHOWS_PLAYED, saved.getIntOr(SHOWS_PLAYED, 0) + 1);
            p.sendSystemMessage(Component.translatable("quest.guhs.beauty.finale", total, ROUNDS * MAX_ROUND, rosettes, bonus)
                    .withStyle(ChatFormatting.GOLD));
            if (!saved.getBooleanOr(FIRST, false)) {
                saved.putBoolean(FIRST, true);
                give(p, new ItemStack(BeautyFeature.SHOWROZET.get(), FIRST_ROSETTES));
                give(p, new ItemStack(ModItems.GUH_BALLON.get(), 3));
                give(p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 4));
                p.sendSystemMessage(Component.translatable("quest.guhs.beauty.first").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (rosettes > 0) {                                // (only a dressed show goes on the world's scoreboard)
                Scorebord.submit(p, niveau.board(SCOREBORD), total, false);
                Klassiekers.gespeeld(p, "beauty", niveau);
            }
            p.sendSystemMessage((record ? Component.translatable("quest.guhs.beauty.record", total)
                    : Component.translatable("quest.guhs.beauty.best", best)).withStyle(ChatFormatting.YELLOW));
            title(p, Component.translatable(record ? "quest.guhs.beauty.record_title" : "quest.guhs.beauty.finale_title")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.beauty.finale_sub", total, ROUNDS * MAX_ROUND).withStyle(ChatFormatting.LIGHT_PURPLE), 60);
            GuhAdvancements.grant(p, "beauty_first");
            if (total >= GOOD_SHOW) {
                GuhAdvancements.grant(p, "beauty_good");
            }
            if (ownGuh) {
                GuhAdvancements.grant(p, "beauty_own_guh");
            }
            Vec3 stage = Vec3.atCenterOf(modelSpot);
            sound(stage, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            level.sendParticles(ParticleTypes.FIREWORK, stage.x, stage.y + 3, stage.z, 80, 3, 2, 3, 0.15);
        }
        tellAll(Component.translatable("quest.guhs.beauty.applause", playerName, modelName, total).withStyle(ChatFormatting.LIGHT_PURPLE));
        end(true);
    }

    /** Ends the show (finished or not): the model goes home (or back to you), the score cards go, nobody is protected. */
    public void end(boolean finished) {
        SHOWS.remove(npcId, this);
        PERFORMERS.remove(player, npcId);
        clearScoreCards();
        ServerPlayer p = performer();
        GuhEntity m = level.getEntity(model) instanceof GuhEntity g ? g : null;
        if (m != null) {
            if (ownGuh) {
                giveBack(m);
                if (p != null && p.level() == level && p.isAlive()) {
                    m.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);  // back to you, in its own clothes
                }
            } else {
                level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                m.discard();
            }
        }
        if (!finished && p != null) {
            p.sendSystemMessage(Component.translatable("quest.guhs.beauty.stopped", rosettes).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        closeScreen(p);
    }

    /** Every second on the server: shows whose Showguh stopped ticking (unloaded, removed) end. */
    static void checkStale(net.minecraft.server.MinecraftServer server) {
        for (BeautyShow show : List.copyOf(SHOWS.values())) {
            if (show.level.getGameTime() - show.lastTick > 40) {
                show.end(false);
            }
        }
    }

    /** The server stops: every show ends now (so borrowed guhs are saved with their own clothes and owner). */
    static void endAll() {
        List.copyOf(SHOWS.values()).forEach(s -> s.end(false));
    }

    /** The performer logged out, died or went to another dimension. */
    static void leave(Player player) {
        BeautyShow show = of(player);
        if (show != null) {
            show.end(false);
        }
    }

    // --- helpers ----------------------------------------------------------------------------------------------------------

    /** The world's top 3 (best show totals), floating over the Showguh's head for the whole audience to see. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Scorebord.show(world, npc.position().add(0, 2.6, 0), "beauty", Klassiekers.bord(world.getServer(),
                Component.translatable("gui.guhs.scorebord.beauty"), SCOREBORD,
                n -> Component.translatable("gui.guhs.klassiekers.beauty.bord", kleedTijd(Niveau.of(n)) / 20),
                score -> score + " / " + ROUNDS * MAX_ROUND));
    }

    /** The finish bonus: 2 rosettes (5 for a good show), only for a show that was actually dressed (no AFK farming). */
    public static int finishBonus(int rosettesWon, int total) {
        return rosettesWon == 0 ? 0 : 2 + (total >= GOOD_SHOW ? 3 : 0);
    }

    public static int best(Player player) {
        return GuhQuests.saved(player).getIntOr(BEST, 0);
    }

    /** Your best show total on this level (medium = the old record). */
    public static int best(Player player, Niveau niveau) {
        return GuhQuests.saved(player).getIntOr(Klassiekers.sleutel(BEST, niveau), 0);
    }

    public static MutableComponent themeName(ShowTheme theme) {
        return Component.translatable("gui.guhs.beauty.theme." + theme.id());
    }

    /** Performers never get hungry or hurt (see {@link BeautyProtection#onDamage}). */
    private static void refresh(ServerPlayer player) {
        nl.juiced.guhs.feature.Minigames.keep(player);    // (no hungrier or weaker than at the start: no free healing)
    }

    private static void face(Entity e, Vec3 target) {
        float yaw = (float) (Mth.atan2(target.z - e.getZ(), target.x - e.getX()) * (180 / Math.PI)) - 90f;
        e.setYRot(yaw);
        if (e instanceof GuhEntity guh) {
            guh.setYBodyRot(yaw);
            guh.setYHeadRot(yaw);
        }
    }

    private static ParticleOptions sparkle() {
        return new DustParticleOptions(new org.joml.Vector3f(1f, 0.6f, 0.85f), 1.0f);
    }

    private void sound(Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.RECORDS, volume, pitch);
    }

    /** Everyone in the theatre (and the performer, wherever they are). */
    private void tellAll(Component message) {
        for (ServerPlayer p : level.players()) {
            if (p.getUUID().equals(player) || p.distanceToSqr(Vec3.atCenterOf(modelSpot)) < THEATRE * THEATRE) {
                p.sendSystemMessage(message);
            }
        }
    }

    private static void title(ServerPlayer p, Component title, Component subtitle, int stay) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(4, stay, 8));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** A big score card (a text display) floating over a jury guh's head. */
    private void scoreCard(Vec3 at, int score) {
        Display.TextDisplay card = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (card == null) {
            return;
        }
        ChatFormatting colour = score >= 8 ? ChatFormatting.DARK_PURPLE : score >= 5 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY;
        CompoundTag tag = new CompoundTag();
        tag.putString("text", Component.Serializer.toJson(Component.literal(" " + score + " ").withStyle(colour, ChatFormatting.BOLD),
                level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putInt("background", 0xF2FFF4F8);
        CompoundTag transform = new CompoundTag();
        transform.put("translation", floats(0, 0, 0));
        transform.put("left_rotation", floats(0, 0, 0, 1));
        transform.put("scale", floats(2.2f, 2.2f, 2.2f));
        transform.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", transform);
        card.load(tag);
        card.snapTo(at.x, at.y, at.z, 0, 0);
        card.addTag(SCORE_TAG);
        displays.add(card.getUUID());
        level.addFreshEntity(card);
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) {
            list.add(FloatTag.valueOf(v));
        }
        return list;
    }

    private void clearScoreCards() {
        for (UUID id : displays) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        displays.clear();
    }

    /** For tests: the score cards up right now. */
    public int scoreCards() {
        return (int) displays.stream().filter(id -> level.getEntity(id) != null).count();
    }

    public static String time(int ticks) {
        int s = (ticks + 19) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        nl.juiced.guhs.feature.Minigames.give(player, stack);   // (what doesn't fit drops in front of you)
    }
}
