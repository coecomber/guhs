package nl.juiced.guhs.feature.creche;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.feature.creche.WiegjeBlock.Baby;
import nl.juiced.guhs.feature.creche.WiegjeBlock.Wens;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Juf Knuffel's two games in the Knuffelcreche (one player at a time per creche; the state lives in memory, per Juf):
 * <ul>
 *   <li><b>De verzorgronde</b> (calm, no timer): {@value #VERZORG_BABYS} babies wake up. Each shows what it wants in a
 *       bubble above its guh_wiegje: a babyflesje (honger), a schone luier (luier), tucking in with a knuffeldekentje
 *       (slaap) and a slaapliedje (liedje: the rhythm screen; {@link Slaapliedje}). While the feesttaakje FEESTSLINGERS
 *       is open the babies first knutsel feestslingers (paper). All asleep: speenmunten (and the feestslingers).</li>
 *   <li><b>Babyguhtjes terugbrengen</b> ({@value #SPEL_TICKS} ticks): every few seconds a baby crawls out of its crib
 *       ({@link CrecheBabyguh}). Right-click it to pick it up (two at most), bring it to an empty crib (right-click the
 *       crib, or just walk up to it). Quick and in a row gives more points; a baby that stays out too long or crawls too
 *       far is fetched by Juf Knuffel herself (no points, the row starts over). Points: a highscore (board
 *       {@value #BOARD}, the floating top 3 above the Juf) and speenmunten.</li>
 * </ul>
 * Players in a game can't get hurt or hungry (like every minigame).
 */
public final class CrecheGame {
    public enum Modus { GEEN, VERZORGEN, TERUGBRENGEN }

    /** Actions from Juf Knuffel's screen. */
    public static final int VERZORGEN = 0, TERUGBRENGEN = 1, SHOP = 2, STOP = 3;
    /** How far from the Juf her cribs may be. */
    public static final int REACH = 14, BELOW = 3, ABOVE = 8;
    public static final int VERZORG_BABYS = 3, VERZORG_TICKS = 20 * 60 * 8;
    public static final int SPEL_TICKS = 20 * 75, AFTEL_TICKS = 60, WEGLOOP_TICKS = 20 * 20, ESCAPE_RADIUS = 18, MAX_DRAGEN = 2;
    /** The world's top 3 of the minigame (also the Guhdex highscores). */
    public static final String BOARD = "creche";
    /** Rewards (each one more than it would have been before 2.7.0's "+1 per reward" rule). */
    public static final int VERZORG_MUNTEN = 2 + 1, FIRST_COINS = 3 + 1, RECORD_BONUS = 1 + 1;
    /** From this many babies back in one game without anyone escaping: the advancement "alle babys terug". */
    public static final int ALLE_TERUG = 8;
    static final String FIRST_ZORG = "guhs_creche_eerste_zorg", FIRST_SPEL = "guhs_creche_eerste_spel";

    private static final Map<UUID, CrecheGame> GAMES = new HashMap<>();
    /** Everyone in a creche game, with the game time their game last saw them. */
    private static final Map<UUID, Long> SPELERS = new ConcurrentHashMap<>();

    public static CrecheGame of(GuhNpcEntity npc) {
        return GAMES.computeIfAbsent(npc.getUUID(), id -> new CrecheGame(npc.getUUID()));
    }

    public static boolean isPlaying(Player player) {
        return SPELERS.containsKey(player.getUUID());
    }

    // --- the cribs, found once by scanning around the Juf -------------------------------------------------------------
    private final UUID jufId;
    final List<BlockPos> wiegjes = new ArrayList<>();
    @Nullable
    private ResourceKey<Level> dim;
    @Nullable
    private BlockPos centre;
    private long lastScan = Long.MIN_VALUE / 2;

    // --- the game ------------------------------------------------------------------------------------------------------
    private Modus modus = Modus.GEEN;
    @Nullable
    private UUID speler;
    int timer, ticks;
    // the care round
    final Map<BlockPos, Integer> zorg = new LinkedHashMap<>();
    boolean feest;
    Slaapliedje liedje = Slaapliedje.STERRETJES;
    // the minigame
    int punten, terug, ontsnapt, reeks, volgende, aftellen;
    final Set<UUID> babys = new java.util.HashSet<>();

    private CrecheGame(UUID jufId) {
        this.jufId = jufId;
    }

    public Modus modus() {
        return modus;
    }

    public boolean isRunning() {
        return modus != Modus.GEEN;
    }

    @Nullable
    public UUID speler() {
        return speler;
    }

    public int punten() {
        return punten;
    }

    @Nullable
    public BlockPos centre() {
        return centre;
    }

    // =================================================================================================================
    // talking
    // =================================================================================================================

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        CrecheGame game = of(npc);
        GuhAdvancements.grant(player, "creche_juf");
        boolean mine = game.isRunning() && player.getUUID().equals(game.speler);
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        GuhQuests.say(player, npc, mine ? "quest.guhs.juf_knuffel.bezig_jij" : game.isRunning() ? "quest.guhs.juf_knuffel.bezig"
                : "quest.guhs.juf_knuffel.hoi." + npc.getRandom().nextInt(3));
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game.isRunning());
        data.putBoolean("Mine", mine);
        data.putString("Modus", game.modus.name());
        ServerPlayer other = game.spelerIn((ServerLevel) npc.level());
        data.putString("Speler", other == null ? "?" : other.getGameProfile().name());
        data.putInt("Best", best(player));
        data.putInt("Munten", GuhQuests.count(player, CrecheFeature.SPEENMUNT.get()));
        data.putBoolean("Feest", Knusfeest.open(player, Feesttaak.FEESTSLINGERS));
        data.putBoolean("Gespeeld", GuhQuests.saved(player).getBooleanOr(FIRST_SPEL, false));
        data.putInt("Liedjes", KnusVoortgang.ontdekt(player, CrecheVoortgang.SLAAPLIEDJES).size());
        ModNetworking.sendTo(player, new CrechePayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.JUF_KNUFFEL || player.distanceToSqr(npc) > 64) {
            return;
        }
        CrecheGame game = of(npc);
        switch (action) {
            case SHOP -> npc.openShop(player);
            case STOP -> {
                if (game.isRunning() && player.getUUID().equals(game.speler)) {
                    GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.gestopt");
                    game.stop((ServerLevel) npc.level(), player, true);
                }
            }
            case VERZORGEN -> game.startVerzorgen(npc, player);
            case TERUGBRENGEN -> game.startTerugbrengen(npc, player);
            default -> {
            }
        }
    }

    /** Can this player start something here? Says why not. */
    private boolean kanStarten(GuhNpcEntity npc, ServerPlayer player, int nodig) {
        if (isRunning()) {
            GuhQuests.say(player, npc, player.getUUID().equals(speler) ? "quest.guhs.juf_knuffel.bezig_jij" : "quest.guhs.juf_knuffel.bezig");
            return false;
        }
        if (isPlaying(player) || Minigames.refuse(player, npc, Minigames.CRECHE)) {
            return false;
        }
        scan(npc, (ServerLevel) npc.level());
        if (wiegjes.size() < nodig) {
            GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.geen_wiegjes");
            return false;
        }
        return true;
    }

    // =================================================================================================================
    // the care round
    // =================================================================================================================

    /** The steps of every baby in a care round (feest: knutselen first). */
    public static List<Wens> stappen(boolean feest) {
        return feest ? List.of(Wens.KNUTSELEN, Wens.HONGER, Wens.LUIER, Wens.SLAAP, Wens.LIEDJE) : List.of(Wens.HONGER, Wens.LUIER, Wens.SLAAP, Wens.LIEDJE);
    }

    public boolean startVerzorgen(GuhNpcEntity npc, ServerPlayer player) {
        if (!kanStarten(npc, player, VERZORG_BABYS)) {
            return false;
        }
        ServerLevel level = (ServerLevel) npc.level();
        modus = Modus.VERZORGEN;
        speler = player.getUUID();
        timer = VERZORG_TICKS;
        ticks = 0;
        zorg.clear();
        feest = Knusfeest.open(player, Feesttaak.FEESTSLINGERS);
        liedje = volgendLiedje(player, level);
        List<BlockPos> kies = new ArrayList<>(wiegjes);
        net.minecraft.Util.shuffle(kies, level.getRandom());
        Wens eerste = stappen(feest).get(0);
        for (BlockPos pos : kies.subList(0, VERZORG_BABYS)) {
            zorg.put(pos, 0);
            zet(level, pos, Baby.WAKKER, eerste);
        }
        SPELERS.put(speler, level.getGameTime());
        Minigames.startKeeping(player);
        // what you need: a bottle and a nappy per baby, paper to knutsel with, and a blanket (yours to keep)
        Minigames.give(player, new ItemStack(CrecheFeature.BABYFLESJE.get(), VERZORG_BABYS));
        Minigames.give(player, new ItemStack(CrecheFeature.SCHONE_LUIER.get(), VERZORG_BABYS));
        if (feest) {
            Minigames.give(player, new ItemStack(Items.PAPER, VERZORG_BABYS));
        }
        if (GuhQuests.count(player, CrecheFeature.KNUFFELDEKENTJE.get()) == 0) {
            Minigames.give(player, new ItemStack(CrecheFeature.KNUFFELDEKENTJE.get()));
        }
        GuhQuests.say(player, npc, feest ? "quest.guhs.juf_knuffel.verzorgen_feest" : "quest.guhs.juf_knuffel.verzorgen");
        GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.liedje_vandaag", Component.translatable("gui.guhs.knus.slaapliedjes." + liedje.id()));
        level.playSound(null, npc, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        return true;
    }

    /** The song the Juf teaches this round: the first one you don't know yet, else a random one. */
    static Slaapliedje volgendLiedje(ServerPlayer player, ServerLevel level) {
        Set<String> bekend = KnusVoortgang.ontdekt(player, CrecheVoortgang.SLAAPLIEDJES);
        for (Slaapliedje s : Slaapliedje.values()) {
            if (!bekend.contains(s.id())) {
                return s;
            }
        }
        return Slaapliedje.values()[level.getRandom().nextInt(Slaapliedje.values().length)];
    }

    /** Something (an item, or an empty hand) used on a crib of this care round. */
    private void verzorg(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        List<Wens> stap = stappen(feest);
        int i = zorg.get(pos);
        if (i >= stap.size()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.slaapt_al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Wens wens = stap.get(i);
        boolean goed = switch (wens) {
            case KNUTSELEN -> stack.is(Items.PAPER);
            case HONGER -> stack.is(CrecheFeature.BABYFLESJE.get());
            case LUIER -> stack.is(CrecheFeature.SCHONE_LUIER.get());
            case SLAAP -> stack.is(CrecheFeature.KNUFFELDEKENTJE.get());
            case LIEDJE -> true;
            default -> false;
        };
        if (!goed) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.wil", Component.translatable(wens.key()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (wens == Wens.LIEDJE) {
            ModNetworking.sendTo(player, new CrechePayloads.Liedje(jufEntityId(level), pos, liedje.ordinal()));
            return;
        }
        if (wens != Wens.SLAAP && !player.getAbilities().instabuild) {
            stack.shrink(1);   // (the blanket is yours to keep)
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
        switch (wens) {
            case KNUTSELEN -> {
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1.2f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 8, 0.3, 0.2, 0.3, 0.02);
            }
            case HONGER -> {
                level.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.BLOCKS, 0.8f, 1.6f);
                level.sendParticles(ParticleTypes.HEART, x, y, z, 3, 0.2, 0.1, 0.2, 0.01);
            }
            case LUIER -> {
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1.3f);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.25, 0.1, 0.25, 0.01);
            }
            case SLAAP -> {
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 0.9f);
                level.sendParticles(CrecheFeature.SLAAPSTERRETJE.get(), x, y, z, 4, 0.25, 0.1, 0.25, 0.01);
            }
            default -> {
            }
        }
        level.playSound(null, pos, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 0.7f, 1.4f + level.getRandom().nextFloat() * 0.3f);
        verder(level, pos, player);
    }

    /** The next step for the baby in this crib (asleep after the last). */
    private void verder(ServerLevel level, BlockPos pos, ServerPlayer player) {
        List<Wens> stap = stappen(feest);
        int i = zorg.get(pos) + 1;
        zorg.put(pos, i);
        if (i < stap.size()) {
            zet(level, pos, stap.get(i) == Wens.LIEDJE || stap.get(i - 1) == Wens.SLAAP ? Baby.INGESTOPT : Baby.WAKKER, stap.get(i));
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.nu", Component.translatable(stap.get(i).key()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        // asleep
        zet(level, pos, Baby.INGESTOPT, Wens.GEEN);
        level.sendParticles(CrecheFeature.SLAAPSTERRETJE.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
        KnusVoortgang.tel(player, CrecheVoortgang.INGESTOPT, 1);
        long wakker = zorg.values().stream().filter(v -> v < stap.size()).count();
        player.sendOverlayMessage(Component.translatable("gui.guhs.creche.slaapt", VERZORG_BABYS - wakker, VERZORG_BABYS)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (wakker == 0) {
            klaarVerzorgen(level, player);
        }
    }

    /** The lullaby was sung (from the screen): enough notes in time and the baby sleeps. */
    public void liedjeKlaar(GuhNpcEntity npc, ServerPlayer player, BlockPos pos, int nummer, int raak) {
        if (modus != Modus.VERZORGEN || !player.getUUID().equals(speler) || !zorg.containsKey(pos) || nummer != liedje.ordinal()
                || raak < 0 || raak > liedje.aantal()) {
            return;
        }
        List<Wens> stap = stappen(feest);
        int i = zorg.get(pos);
        if (i >= stap.size() || stap.get(i) != Wens.LIEDJE) {
            return;
        }
        ServerLevel level = (ServerLevel) npc.level();
        if (!liedje.gelukt(raak)) {
            GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.liedje_opnieuw", raak, liedje.aantal());
            level.playSound(null, pos, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 0.8f, 1.6f);
            return;
        }
        if (KnusVoortgang.ontdek(player, CrecheVoortgang.SLAAPLIEDJES, liedje.id())) {
            GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.liedje_geleerd", Component.translatable("gui.guhs.knus.slaapliedjes." + liedje.id()));
        }
        int bekend = KnusVoortgang.ontdekt(player, CrecheVoortgang.SLAAPLIEDJES).size();
        KnusVoortgang.hoogste(player, CrecheVoortgang.LIEDJES, bekend);
        if (bekend >= Slaapliedje.values().length) {
            GuhAdvancements.grant(player, "creche_slaapliedjes");
            CrecheVoortgang.toon(player, "creche_slaapliedjes");
        }
        level.playSound(null, pos, CrecheFeature.SLAAPLIEDJE.get(), SoundSource.NEUTRAL, 0.6f, 1.0f);
        verder(level, pos, player);
    }

    private void klaarVerzorgen(ServerLevel level, ServerPlayer player) {
        GuhNpcEntity npc = juf(level);
        CompoundTag saved = GuhQuests.saved(player);
        Minigames.give(player, new ItemStack(CrecheFeature.SPEENMUNT.get(), VERZORG_MUNTEN));
        if (!saved.getBooleanOr(FIRST_ZORG, false)) {
            saved.putBoolean(FIRST_ZORG, true);
            Minigames.give(player, new ItemStack(CrecheFeature.SPEENMUNT.get(), FIRST_COINS));
        }
        if (npc != null) {
            GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.verzorgd", VERZORG_MUNTEN);
        }
        if (feest && Knusfeest.open(player, Feesttaak.FEESTSLINGERS)) {
            Minigames.give(player, new ItemStack(CrecheFeature.FEESTSLINGERS_ITEM.get()));
            Knusfeest.gemaakt(player, Feesttaak.FEESTSLINGERS);
            if (npc != null) {
                GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.slingers");
            }
        }
        KnusVoortgang.tel(player, CrecheVoortgang.RONDES, 1);
        GuhAdvancements.grant(player, "creche_ingestopt");
        CrecheVoortgang.toon(player, "creche_ingestopt");
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        reset(level);
    }

    // =================================================================================================================
    // the minigame: babyguhtjes terugbrengen
    // =================================================================================================================

    public boolean startTerugbrengen(GuhNpcEntity npc, ServerPlayer player) {
        if (!kanStarten(npc, player, 4)) {
            return false;
        }
        ServerLevel level = (ServerLevel) npc.level();
        modus = Modus.TERUGBRENGEN;
        speler = player.getUUID();
        timer = SPEL_TICKS;
        aftellen = AFTEL_TICKS;
        ticks = 0;
        punten = terug = ontsnapt = reeks = 0;
        volgende = 20;
        babys.clear();
        for (BlockPos pos : wiegjes) {
            zet(level, pos, Baby.WAKKER, Wens.GEEN);
        }
        SPELERS.put(speler, level.getGameTime());
        Minigames.startKeeping(player);
        GuhQuests.say(player, npc, "quest.guhs.juf_knuffel.terugbrengen");
        title(player, Component.translatable("gui.guhs.creche.titel.klaar").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("gui.guhs.creche.titel.klaar.sub"), 40);
        level.playSound(null, npc, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        return true;
    }

    /** Points for a baby brought back after this many seconds, in a row of this many. */
    public static int punten(int seconden, int reeks) {
        return 10 + Math.max(0, 10 - seconden) + Math.min(10, 2 * reeks);
    }

    /** Speenmunten for a game: one per 60 points (at most 12) plus one, and the "+1 per reward" of 2.7.0; nothing for 0. */
    public static int munten(int score) {
        return score <= 0 ? 0 : Math.min(12, score / 60) + 1 + 1;
    }

    /** How many babies may be out at once (more as the game goes on). */
    private int maxWakker() {
        int elapsed = SPEL_TICKS - timer;
        return Math.max(1, Math.min(wiegjes.size() - 1, 2 + elapsed / (SPEL_TICKS / 3)));
    }

    private void tickTerugbrengen(GuhNpcEntity npc, ServerLevel level, ServerPlayer p) {
        if (aftellen > 0) {
            aftellen--;
            if (aftellen > 0 && aftellen % 20 == 0) {
                title(p, Component.literal(String.valueOf(aftellen / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 16);
                level.playSound(null, npc.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 1f, 1.2f);
            } else if (aftellen == 0) {
                title(p, Component.translatable("gui.guhs.creche.titel.go").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.creche.titel.go.sub"), 20);
            }
            return;
        }
        if (--timer <= 0) {
            finish(level, p);
            return;
        }
        long now = level.getGameTime();
        if (--volgende <= 0) {
            if (babys.size() < maxWakker()) {
                wordWakker(level);
            }
            int elapsed = SPEL_TICKS - timer;
            volgende = Math.max(30, 90 - elapsed / 30);
        }
        // the babies: carried ones go into an empty crib you walk up to; the others crawl, or the Juf fetches them
        for (UUID id : List.copyOf(babys)) {
            if (!(level.getEntity(id) instanceof CrecheBabyguh baby) || !baby.isAlive()) {
                babys.remove(id);
                continue;
            }
            if (baby.drager != null) {
                if (baby.drager.equals(speler) && ticks % 2 == 0) {
                    BlockPos leeg = legeWiegBij(level, p.position(), 1.9);
                    if (leeg != null) {
                        legNeer(level, p, baby, leeg);
                    }
                }
                continue;
            }
            double dx = baby.getX() - (centre.getX() + 0.5), dz = baby.getZ() - (centre.getZ() + 0.5);
            if (now - baby.uitSinds > WEGLOOP_TICKS || dx * dx + dz * dz > ESCAPE_RADIUS * ESCAPE_RADIUS || Math.abs(baby.getY() - centre.getY()) > 6) {
                jufHaaltTerug(level, p, baby);
            }
        }
        if (ticks % 10 == 0) {
            int s = (timer + 19) / 20;
            p.displayClientMessage(Component.translatable("gui.guhs.creche.bar", terug, punten, s, reeks)
                    .withStyle(s <= 10 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** A baby wakes up and crawls out of its crib. */
    void wordWakker(ServerLevel level) {
        List<BlockPos> vol = wiegjes.stream().filter(pos -> baby(level, pos) == Baby.WAKKER).toList();
        if (vol.isEmpty()) {
            return;
        }
        BlockPos wieg = vol.get(level.getRandom().nextInt(vol.size()));
        CrecheBabyguh baby = CrecheFeature.BABYGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baby == null) {
            return;
        }
        Vec3 at = naastWieg(level, wieg);
        baby.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
        baby.juf = jufId;
        baby.wieg = wieg;
        baby.uitSinds = level.getGameTime();
        level.addFreshEntity(baby);
        babys.add(baby.getUUID());
        zet(level, wieg, Baby.LEEG, Wens.GEEN);
        level.playSound(null, wieg, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.3f + level.getRandom().nextFloat() * 0.3f);
        level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 0.6, at.z, 1, 0, 0, 0, 0.5);
    }

    /** A free spot on the floor next to a crib (or on top of it). */
    private static Vec3 naastWieg(ServerLevel level, BlockPos wieg) {
        BlockState state = level.getBlockState(wieg);
        Direction front = state.hasProperty(WiegjeBlock.FACING) ? state.getValue(WiegjeBlock.FACING) : Direction.NORTH;
        for (Direction d : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
            BlockPos n = wieg.relative(d);
            if (level.getBlockState(n).getCollisionShape(level, n).isEmpty() && level.getBlockState(n.above()).getCollisionShape(level, n.above()).isEmpty()
                    && level.getBlockState(n.below()).isFaceSturdy(level, n.below(), Direction.UP)) {
                return Vec3.atBottomCenterOf(n);
            }
        }
        return Vec3.atBottomCenterOf(wieg).add(0, 0.6, 0);
    }

    /** An empty crib of this creche within reach of a spot, or null. */
    @Nullable
    BlockPos legeWiegBij(ServerLevel level, Vec3 at, double reach) {
        BlockPos best = null;
        double bestD = reach * reach;
        for (BlockPos pos : wiegjes) {
            double dx = pos.getX() + 0.5 - at.x, dz = pos.getZ() + 0.5 - at.z, dy = pos.getY() - at.y;
            double d = dx * dx + dz * dz;
            if (d <= bestD && Math.abs(dy) <= 1.6 && baby(level, pos) == Baby.LEEG) {
                best = pos;
                bestD = d;
            }
        }
        return best;
    }

    /** A carried baby goes back into an empty crib: points. */
    void legNeer(ServerLevel level, ServerPlayer p, CrecheBabyguh baby, BlockPos wieg) {
        int seconden = (int) ((level.getGameTime() - baby.uitSinds) / 20);
        int erbij = punten(seconden, reeks);
        punten += erbij;
        reeks++;
        terug++;
        babys.remove(baby.getUUID());
        baby.discard();
        zet(level, wieg, Baby.WAKKER, Wens.GEEN);
        KnusVoortgang.tel(p, CrecheVoortgang.TERUG, 1);
        level.playSound(null, wieg, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.5f);
        level.playSound(null, wieg, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 0.6f, 1.0f + Math.min(1f, reeks * 0.08f));
        level.sendParticles(ParticleTypes.HEART, wieg.getX() + 0.5, wieg.getY() + 0.9, wieg.getZ() + 0.5, 3, 0.25, 0.15, 0.25, 0.01);
        p.sendOverlayMessage(Component.translatable("gui.guhs.creche.terug", erbij, reeks).withStyle(ChatFormatting.GOLD));
    }

    /** Out too long (or too far): Juf Knuffel brings it back herself. No points, the row starts over. */
    private void jufHaaltTerug(ServerLevel level, ServerPlayer p, CrecheBabyguh baby) {
        BlockPos wieg = baby.wieg != null && baby(level, baby.wieg) == Baby.LEEG ? baby.wieg : legeWiegBij(level, Vec3.atCenterOf(centre), 64);
        level.sendParticles(ParticleTypes.POOF, baby.getX(), baby.getY() + 0.2, baby.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
        babys.remove(baby.getUUID());
        baby.discard();
        if (wieg != null) {
            zet(level, wieg, Baby.WAKKER, Wens.GEEN);
            level.sendParticles(ParticleTypes.POOF, wieg.getX() + 0.5, wieg.getY() + 0.8, wieg.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
        }
        ontsnapt++;
        reeks = 0;
        p.sendOverlayMessage(Component.translatable("gui.guhs.creche.ontsnapt").withStyle(ChatFormatting.RED));
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.RECORDS, 0.6f, 0.8f);
    }

    /** Picking up a crawling baby (right-click on it). */
    static void pakOp(CrecheBabyguh baby, ServerPlayer player) {
        CrecheGame game = baby.juf == null ? null : GAMES.get(baby.juf);
        if (game == null || game.modus != Modus.TERUGBRENGEN || baby.isGedragen()) {
            return;
        }
        if (!player.getUUID().equals(game.speler)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.niet_jouw_spel").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (game.gedragen((ServerLevel) baby.level(), player) >= MAX_DRAGEN) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.handen_vol", MAX_DRAGEN).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        baby.draag(player);
        baby.level().playSound(null, baby, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.6f);
    }

    /** How many babies this player carries now. */
    int gedragen(ServerLevel level, Player player) {
        int n = 0;
        for (UUID id : babys) {
            if (level.getEntity(id) instanceof CrecheBabyguh b && player.getUUID().equals(b.drager)) {
                n++;
            }
        }
        return n;
    }

    /** Which arm a carried baby sits on: the first one left, the second one right (-1 / 1), a single one in the middle. */
    static double armZijde(CrecheBabyguh baby, Player player) {
        CrecheGame game = baby.juf == null ? null : GAMES.get(baby.juf);
        if (game == null || !(baby.level() instanceof ServerLevel level) || game.gedragen(level, player) < 2) {
            return 0;
        }
        for (UUID id : game.babys) {
            if (level.getEntity(id) instanceof CrecheBabyguh b && player.getUUID().equals(b.drager)) {
                return b == baby ? -1 : 1;
            }
        }
        return 0;
    }

    /** Does this baby still belong to a running game? (Otherwise it goes away.) */
    static boolean hoortErbij(CrecheBabyguh baby) {
        CrecheGame game = baby.juf == null ? null : GAMES.get(baby.juf);
        return game != null && game.modus == Modus.TERUGBRENGEN && game.babys.contains(baby.getUUID());
    }

    /** The end of a game: all babies back in bed, speenmunten, the record, the advancements. */
    private void finish(ServerLevel level, ServerPlayer p) {
        GuhNpcEntity npc = juf(level);
        int score = punten;
        int coins = munten(score);
        CompoundTag saved = GuhQuests.saved(p);
        if (coins > 0) {
            Minigames.give(p, new ItemStack(CrecheFeature.SPEENMUNT.get(), coins));
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.creche.klaar", terug, score, coins).withStyle(ChatFormatting.GOLD));
        int best = best(p);
        if (score > 0 && score > best) {
            if (best > 0) {
                Minigames.give(p, new ItemStack(CrecheFeature.SPEENMUNT.get(), RECORD_BONUS));
            }
            p.sendSystemMessage((best > 0 ? Component.translatable("gui.guhs.creche.record", score, best, RECORD_BONUS)
                    : Component.translatable("gui.guhs.creche.record_eerste", score)).withStyle(ChatFormatting.YELLOW));
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        } else if (best > 0) {
            p.sendSystemMessage(Component.translatable("gui.guhs.creche.best", best).withStyle(ChatFormatting.GRAY));
        }
        if (score > 0) {
            Scorebord.submit(p, BOARD, score, false);
            KnusVoortgang.hoogste(p, CrecheVoortgang.RECORD, score);
        }
        if (!saved.getBooleanOr(FIRST_SPEL, false)) {
            saved.putBoolean(FIRST_SPEL, true);
            Minigames.give(p, new ItemStack(CrecheFeature.SPEENMUNT.get(), FIRST_COINS));
            if (npc != null) {
                GuhQuests.say(p, npc, "quest.guhs.juf_knuffel.eerste_spel", FIRST_COINS);
            }
        }
        KnusVoortgang.tel(p, CrecheVoortgang.SPELLETJES, 1);
        GuhAdvancements.grant(p, "creche_terugbrengen");
        if (ontsnapt == 0 && terug >= ALLE_TERUG) {
            GuhAdvancements.grant(p, "creche_alle_babys_terug");
            CrecheVoortgang.toon(p, "creche_alle_babys_terug");
        }
        title(p, Component.translatable("gui.guhs.creche.titel.klaar_spel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.creche.titel.klaar_spel.sub", score), 40);
        if (npc != null) {
            showScores(npc);
        }
        reset(level);
    }

    // =================================================================================================================
    // the Juf's tick, stopping, resetting
    // =================================================================================================================

    public void tick(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        ticks++;
        if (ticks % 100 == 1 && centre != null) {
            showScores(npc);
        }
        if (modus == Modus.GEEN) {
            idle(npc, level);
            return;
        }
        ServerPlayer p = spelerIn(level);
        if (p == null || !p.isAlive() || p.level() != level || centre == null || !SPELERS.containsKey(speler)) {
            stop(level, p, false);
            return;
        }
        SPELERS.put(speler, level.getGameTime());
        if (ticks % 20 == 0) {
            Minigames.keep(p);
        }
        double dx = p.getX() - (centre.getX() + 0.5), dz = p.getZ() - (centre.getZ() + 0.5);
        if (dx * dx + dz * dz > (ESCAPE_RADIUS + 12) * (ESCAPE_RADIUS + 12)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.creche.weggelopen").withStyle(ChatFormatting.LIGHT_PURPLE));
            stop(level, p, true);
            return;
        }
        if (modus == Modus.TERUGBRENGEN) {
            tickTerugbrengen(npc, level, p);
        } else if (--timer <= 0) {
            GuhQuests.say(p, npc, "quest.guhs.juf_knuffel.te_lang");
            stop(level, p, false);
        } else if (ticks % 60 == 0) {
            // a soft reminder above the babies that still want something
            for (var e : zorg.entrySet()) {
                BlockPos pos = e.getKey();
                if (e.getValue() < stappen(feest).size()) {
                    level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 1, 0, 0, 0, 0.3);
                }
            }
        }
    }

    /** Outside a game: the babies sleep (sleepy stars, the Juf hums a lullaby now and then). */
    private void idle(GuhNpcEntity npc, ServerLevel level) {
        long now = level.getGameTime();
        if (centre == null) {
            if (now - lastScan > 100 && !level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(REACH + 16)).isEmpty()) {
                scan(npc, level);
            }
            return;
        }
        if (ticks % 400 == 0 && !level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(12)).isEmpty()) {
            level.playSound(null, npc, CrecheFeature.SLAAPLIEDJE.get(), SoundSource.NEUTRAL, 0.35f, 1.0f);
        }
    }

    /** Stops whatever runs: the babies go back to bed. With {@code rewardPlayed} a minigame still pays out what you did. */
    void stop(ServerLevel level, @Nullable ServerPlayer p, boolean rewardPlayed) {
        if (modus == Modus.TERUGBRENGEN && p != null && rewardPlayed && punten > 0) {
            finish(level, p);
            return;
        }
        reset(level);
    }

    /** Everything back to sleep, nobody playing. */
    void reset(ServerLevel level) {
        for (UUID id : babys) {
            if (level.getEntity(id) instanceof CrecheBabyguh b) {
                b.discard();
            }
        }
        babys.clear();
        for (BlockPos pos : wiegjes) {
            zet(level, pos, Baby.INGESTOPT, Wens.GEEN);
        }
        zorg.clear();
        if (speler != null) {
            SPELERS.remove(speler);
        }
        speler = null;
        modus = Modus.GEEN;
        timer = 0;
        feest = false;
    }

    // =================================================================================================================
    // the cribs
    // =================================================================================================================

    /** Finds the Juf's cribs (all guh_wiegjes around her); the middle of them is where the game happens. */
    void scan(GuhNpcEntity npc, ServerLevel level) {
        lastScan = level.getGameTime();
        if (centre != null && dim == level.dimension()) {
            return;
        }
        wiegjes.clear();
        BlockPos c = npc.blockPosition();
        long sx = 0, sy = 0, sz = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-REACH, -BELOW, -REACH), c.offset(REACH, ABOVE, REACH))) {
            if (level.getBlockState(pos).getBlock() instanceof WiegjeBlock) {
                wiegjes.add(pos.immutable());
                sx += pos.getX();
                sy += pos.getY();
                sz += pos.getZ();
            }
        }
        if (wiegjes.isEmpty()) {
            return;
        }
        int n = wiegjes.size();
        dim = level.dimension();
        centre = new BlockPos((int) Math.floor((double) sx / n + 0.5), (int) Math.floor((double) sy / n), (int) Math.floor((double) sz / n + 0.5));
        if (modus == Modus.GEEN) {
            for (BlockPos pos : wiegjes) {
                zet(level, pos, Baby.INGESTOPT, Wens.GEEN);   // (after a restart mid-game: everybody back to bed)
            }
        }
    }

    static Baby baby(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof WiegjeBlock ? state.getValue(WiegjeBlock.BABY) : Baby.LEEG;
    }

    static Wens wens(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof WiegjeBlock ? state.getValue(WiegjeBlock.WENS) : Wens.GEEN;
    }

    static void zet(ServerLevel level, BlockPos pos, Baby baby, Wens wens) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WiegjeBlock) {
            BlockState now = state.setValue(WiegjeBlock.BABY, baby).setValue(WiegjeBlock.WENS, wens);
            if (now != state) {
                level.setBlock(pos, now, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The game whose crib this is (null: a crib of nobody, e.g. at home). */
    @Nullable
    static CrecheGame bij(ServerLevel level, BlockPos pos) {
        for (CrecheGame game : GAMES.values()) {
            if (game.dim == level.dimension() && game.wiegjes.contains(pos)) {
                return game;
            }
        }
        return null;
    }

    /** A player used a crib (with this item, or an empty hand). True when a game handled it. */
    static boolean opWiegje(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        CrecheGame game = bij(level, pos);
        if (game == null || !game.isRunning()) {
            return false;
        }
        if (!player.getUUID().equals(game.speler)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.niet_jouw_spel").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        if (game.modus == Modus.VERZORGEN) {
            if (game.zorg.containsKey(pos)) {
                game.verzorg(level, pos, player, stack);
            } else {
                player.sendOverlayMessage(Component.translatable("gui.guhs.creche.deze_slaapt").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return true;
        }
        // the minigame: a carried baby into this empty crib
        if (baby(level, pos) == Baby.LEEG) {
            for (UUID id : List.copyOf(game.babys)) {
                if (level.getEntity(id) instanceof CrecheBabyguh b && player.getUUID().equals(b.drager)) {
                    game.legNeer(level, player, b, pos);
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Rocking a crib (right-click with an empty hand, or anything a game didn't want): a lullaby and sleepy stars; the baby
     * guhs around it grow up a little (each at most once in {@value #WIEG_COOLDOWN} ticks).
     */
    static void wieg(ServerLevel level, BlockPos pos, ServerPlayer player) {
        level.playSound(null, pos, CrecheFeature.SLAAPLIEDJE.get(), SoundSource.BLOCKS, 0.5f, 1.0f + level.getRandom().nextFloat() * 0.1f);
        level.sendParticles(CrecheFeature.SLAAPSTERRETJE.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.01);
        int gegroeid = 0;
        long now = level.getGameTime();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(4))) {
            if (!guh.isBaby() || (guh.getPersistentData().contains(GEWIEGD) && now - guh.getPersistentData().getLongOr(GEWIEGD, 0L) < WIEG_COOLDOWN)) {
                continue;
            }
            guh.getPersistentData().putLong(GEWIEGD, now);
            guh.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-guh.getAge()), true);
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.6, guh.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            gegroeid++;
        }
        if (gegroeid > 0) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.creche.gewiegd", gegroeid).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Your best score in the minigame (the Guhdex highscores keep it; 0: never played). */
    public static int best(Player player) {
        return GuhQuests.saved(player).getCompoundOrEmpty(nl.juiced.guhs.quest.Highscores.KEY).getIntOr(BOARD, 0);
    }

    public static final String GEWIEGD = "guhs_creche_gewiegd";
    public static final int WIEG_COOLDOWN = 20 * 60 * 5;

    // =================================================================================================================
    // helpers, scoreboard, events
    // =================================================================================================================

    @Nullable
    private ServerPlayer spelerIn(ServerLevel level) {
        return speler == null ? null : level.getServer().getPlayerList().getPlayer(speler);
    }

    @Nullable
    private GuhNpcEntity juf(ServerLevel level) {
        return level.getEntity(jufId) instanceof GuhNpcEntity npc ? npc : null;
    }

    private int jufEntityId(ServerLevel level) {
        GuhNpcEntity npc = juf(level);
        return npc == null ? -1 : npc.getId();
    }

    /** The floating top 3 above Juf Knuffel. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        Scorebord.show(level, npc.position().add(0, 3.2, 0), "creche", Scorebord.text(level.getServer(), Component.translatable("gui.guhs.scorebord.creche"),
                List.of(BOARD), List.of(Component.translatable("gui.guhs.scorebord.creche.punten")), n -> n + " pt"));
    }

    private static void title(ServerPlayer p, Component title, @Nullable Component sub, int stay) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(2, stay, 6));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub == null ? Component.empty() : sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** No hurting players in a creche game. */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isPlaying(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A player whose game stopped ticking (its chunk unloaded, the Juf vanished) is an ordinary player again. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }
        Long seen = SPELERS.get(player.getUUID());
        if (seen != null && player.level().getGameTime() - seen > 40) {
            SPELERS.remove(player.getUUID());
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SPELERS.remove(event.getEntity().getUUID());
    }

    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        SPELERS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        SPELERS.clear();
    }

    /** (Tests) forget a Juf's game. */
    static void vergeet(GuhNpcEntity npc) {
        CrecheGame game = GAMES.remove(npc.getUUID());
        if (game != null && game.speler != null) {
            SPELERS.remove(game.speler);
        }
    }
}
