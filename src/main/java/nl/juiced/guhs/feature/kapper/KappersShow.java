package nl.juiced.guhs.feature.kapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * De kappersshow of Kapper Krulletje: customer guhs come in one by one and sit down in the kappersstoel nearest to
 * Krulletje, each with a picture of the hairstyle it wants (a style and a colour, or natural). In the knip screen you
 * wash its hair (three times: foam!), cut the style, dye it, and blow it dry: the föhn finishes the customer and the
 * points are counted. Everything right and fast gives the most points, and perfect customers in a row give a combo. Every
 * customer has a bit less patience than the one before; from the fourth customer on the picture is only shown for a
 * few seconds (look again: -2 points). 8 customers per show (the feest round: 6 festive ones).
 * <p>
 * Krulmunten for the score ({@link #munten}, +1 like every 2.8 reward), a personal record (Guhdex highscores, board
 * {@link #BOARD}), a top 3 above the showstoel, a welcome present the first time, the Knus tab counters and collection.
 * While the Burgemeester's task FEESTKAPSELS is open, Krulletje offers a feest round: at least {@link #FEEST_MIN} points
 * gives the feestkapselset (and Knusfeest.gemaakt). One player per salon at a time; the state lives in memory.
 */
public final class KappersShow {
    public enum Fase { IDLE, AFTELLEN, KLANT, TUSSEN }

    /** Actions from the screens. KNIP + kapsel ordinal, VERF + dye ordinal (VERF + {@link #NATUREL}: natural). */
    public static final int START = 0, SHOP = 1, STOP = 2, FEEST = 3, WAS = 10, FOHN = 11, KIJK = 12, KNIP = 20, VERF = 40;
    public static final int NATUREL = Haarverf.values().length;
    public static final int KLANTEN = 8, FEEST_KLANTEN = 6, WASSEN = 3;
    static final int AFTELLEN_TICKS = 60, TUSSEN_TICKS = 30, VERTREK_TICKS = 25, FOTO_TICKS = 100, KIJK_TICKS = 50;
    /** From this customer (0-based) on the picture is only shown for a few seconds. */
    public static final int FOTO_WEG_VANAF = 3;
    /** Looking at the picture again costs this many points. */
    public static final int KIJK_PRIJS = 2;
    /** The feest round needs this many points for the feestkapselset. */
    public static final int FEEST_MIN = 60;
    /** How far from the showstoel you may walk during a show; how far Krulletje looks for his chairs. */
    public static final int BLIJF = 14, REACH = 16;
    /** The welcome present of your first show (3 + 1). */
    public static final int FIRST_MUNTEN = 3 + 1;
    public static final String BOARD = "kapper";
    static final String BEST = "guhs_kapper_best", FIRST = "guhs_kapper_first";

    /** The customers' looks. Only common looks, like BeautyShow's models: a customer is a GuhEntity, so
     *  GuhDex would mark any rare look as seen for every player standing near the chair. */
    static final List<GuhVariant> KLANT_VARIANTEN = List.of(GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW);
    static final List<Kapsel> FEEST_KAPSELS = List.of(Kapsel.STRIKJES, Kapsel.KNOTJES, Kapsel.KRULLEN, Kapsel.PLUISBOL, Kapsel.KUIFJE);
    static final List<Haarverf> FEEST_VERVEN = List.of(Haarverf.ROZE, Haarverf.REGENBOOG, Haarverf.LAVENDEL, Haarverf.CITROEN, Haarverf.PERZIK);

    private static final Map<UUID, KappersShow> SHOWS = new HashMap<>();
    /** Everyone in a show right now, with the game time their show last saw them. */
    private static final Map<UUID, Long> SPELERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static KappersShow of(GuhNpcEntity npc) {
        return SHOWS.computeIfAbsent(npc.getUUID(), id -> new KappersShow(id));
    }

    public static boolean isPlaying(Player player) {
        return SPELERS.containsKey(player.getUUID());
    }

    /** Is this customer still part of a running show? (Leftovers poof.) */
    static boolean hoortBij(KapperKlantEntity klant) {
        KappersShow show = klant.kapper == null ? null : SHOWS.get(klant.kapper);
        return show != null && show.fase != Fase.IDLE && klant.getUUID().equals(show.klant);
    }

    private final UUID kapperId;
    @Nullable
    private BlockPos stoel;
    private long lastScan = Long.MIN_VALUE / 2;

    private Fase fase = Fase.IDLE;
    @Nullable
    private UUID speler;
    @Nullable
    private UUID klant;
    private boolean feest;
    private int nr, aantal, score, combo, perfect, timer, geduld, gewassen, fotoTot, ticks;
    @Nullable
    private Kapsel wens, gekozen, vorigeWens;
    /** The wished dye (null: natural) and the chosen one (-1: nothing yet, NATUREL: natural). */
    @Nullable
    private Haarverf wensVerf;
    private int gekozenVerf = -1;
    /** The last customer's result, for the screen: lang key and points. */
    private String uitslag = "";
    private int uitslagPunten;

    private KappersShow(UUID kapperId) {
        this.kapperId = kapperId;
    }

    public Fase fase() {
        return fase;
    }

    public boolean isRunning() {
        return fase != Fase.IDLE;
    }

    public int score() {
        return score;
    }

    public int nr() {
        return nr;
    }

    public int aantal() {
        return aantal;
    }

    public int combo() {
        return combo;
    }

    public int timer() {
        return timer;
    }

    public int gewassen() {
        return gewassen;
    }

    @Nullable
    public Kapsel wens() {
        return wens;
    }

    @Nullable
    public Haarverf wensVerf() {
        return wensVerf;
    }

    public boolean fotoZichtbaar() {
        return fotoTot != 0;
    }

    @Nullable
    public BlockPos stoel() {
        return stoel;
    }

    @Nullable
    public UUID speler() {
        return speler;
    }

    /** The customer in the chair now (or null). */
    @Nullable
    public KapperKlantEntity klant(ServerLevel level) {
        return klant != null && level.getEntity(klant) instanceof KapperKlantEntity k ? k : null;
    }

    // --- scoring (static: the tests check these) ---------------------------------------------------------------------------

    /**
     * Points for one customer: the style right and the colour right = 15, plus half the seconds of patience left and 2
     * per customer of the combo before it (at most 5); only the style right 8, only the colour right 4, nothing right 1
     * (a lief guh says thank you anyway).
     */
    public static int punten(boolean kapselGoed, boolean kleurGoed, int secondenOver, int combo) {
        if (kapselGoed && kleurGoed) {
            return 15 + Math.max(0, secondenOver) / 2 + 2 * Math.min(Math.max(0, combo), 5);
        }
        return kapselGoed ? 8 : kleurGoed ? 4 : 1;
    }

    /** Krulmunten for a show: nothing for 0 points, else one per 20 points (at most 15), +1. */
    public static int munten(int score) {
        return score <= 0 ? 0 : Math.min(15, score / 20) + 1;
    }

    /** The patience of customer nr (0-based), in ticks: 30 seconds, 2 less for every next customer, at least 16. */
    public static int geduld(int nr, boolean feest) {
        return (feest ? Math.max(18, 26 - nr) : Math.max(16, 30 - 2 * nr)) * 20;
    }

    // --- talking to Krulletje -------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        KappersShow show = of(npc);
        boolean mine = show.isRunning() && player.getUUID().equals(show.speler);
        GuhAdvancements.grant(player, "kapper_krulletje");
        GuhQuests.say(player, npc, mine ? "quest.guhs.kapper.bezig" : show.isRunning() ? "quest.guhs.kapper.druk"
                : "quest.guhs.kapper.hoi." + npc.getRandom().nextInt(3));
        if (!player.getInventory().hasAnyMatching(s -> s.is(KapperFeature.KAPPERSSCHAAR.get()))) {
            Minigames.give(player, new ItemStack(KapperFeature.KAPPERSSCHAAR.get()));
            GuhQuests.say(player, npc, "quest.guhs.kapper.schaar");
        }
        if (mine) {
            show.openKnip(npc, player);
            return;
        }
        ServerPlayer other = show.isRunning() ? show.speler((ServerLevel) npc.level()) : null;
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", show.isRunning());
        data.putString("Speler", other == null ? "?" : other.getGameProfile().name());
        data.putInt("Best", best(player));
        data.putBoolean("Played", GuhQuests.saved(player).getBooleanOr(FIRST, false));
        data.putInt("Munten", GuhQuests.count(player, KapperFeature.KRULMUNT.get()));
        data.putBoolean("Feest", Knusfeest.open(player, Feesttaak.FEESTKAPSELS));
        ModNetworking.sendTo(player, new KapperPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.KAPPERGUH || player.distanceToSqr(npc) > (BLIJF + 4) * (BLIJF + 4)) {
            return;
        }
        KappersShow show = of(npc);
        ServerLevel level = (ServerLevel) npc.level();
        if (action == SHOP) {
            npc.openShop(player);
        } else if (action == START || action == FEEST) {
            show.start(npc, player, action == FEEST);
        } else if (!show.isRunning() || !player.getUUID().equals(show.speler)) {
            return;
        } else if (action == STOP) {
            show.finish(npc, level, player);
        } else if (action == WAS) {
            show.was(npc, level, player);
        } else if (action == FOHN) {
            show.fohn(npc, level, player);
        } else if (action == KIJK) {
            show.kijk(npc, player);
        } else if (action >= KNIP && action < KNIP + Kapsel.values().length) {
            show.knip(npc, level, player, Kapsel.values()[action - KNIP]);
        } else if (action >= VERF && action <= VERF + NATUREL) {
            show.verf(npc, level, player, action - VERF);
        }
    }

    /** Right-click on the customer: the knip screen (only for the player of the show). */
    static void klik(KapperKlantEntity klant, ServerPlayer player) {
        KappersShow show = klant.kapper == null ? null : SHOWS.get(klant.kapper);
        if (show == null || !show.isRunning()) {
            return;
        }
        GuhNpcEntity npc = show.kapper((ServerLevel) klant.level());
        if (npc == null) {
            return;
        }
        if (player.getUUID().equals(show.speler)) {
            show.openKnip(npc, player);
        } else {
            player.sendOverlayMessage(Component.translatable("quest.guhs.kapper.niet_jouw_klant").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // --- playing ---------------------------------------------------------------------------------------------------------

    /** Starts a show for this player (the salon must be free). */
    public boolean start(GuhNpcEntity npc, ServerPlayer player, boolean feestRonde) {
        ServerLevel level = (ServerLevel) npc.level();
        if (isRunning()) {
            if (!player.getUUID().equals(speler)) {
                GuhQuests.say(player, npc, "quest.guhs.kapper.druk");
            }
            return false;
        }
        if (isPlaying(player) || Minigames.refuse(player, npc, Minigames.KAPPER)) {
            return false;
        }
        if (feestRonde && !Knusfeest.open(player, Feesttaak.FEESTKAPSELS)) {
            return false;
        }
        scan(npc, level);
        if (stoel == null) {
            GuhQuests.say(player, npc, "quest.guhs.kapper.geen_stoel");
            return false;
        }
        speler = player.getUUID();
        feest = feestRonde;
        aantal = feest ? FEEST_KLANTEN : KLANTEN;
        nr = score = combo = perfect = 0;
        vorigeWens = null;
        uitslag = "";
        fase = Fase.AFTELLEN;
        timer = AFTELLEN_TICKS;
        SPELERS.put(speler, level.getGameTime());
        Minigames.startKeeping(player);
        player.sendSystemMessage(Component.translatable(feest ? "quest.guhs.kapper.uitleg_feest" : "quest.guhs.kapper.uitleg", aantal)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        title(player, Component.translatable(feest ? "quest.guhs.kapper.title.feest" : "quest.guhs.kapper.title.show").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("quest.guhs.kapper.title.show.sub"), 30);
        level.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        return true;
    }

    /** Every tick (from Krulletje). */
    public void tick(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        ticks++;
        if (ticks % 100 == 1) {
            showScores(npc);
        }
        if (fase == Fase.IDLE) {
            if (stoel == null && level.getGameTime() - lastScan > 200
                    && !level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(REACH + 8)).isEmpty()) {
                scan(npc, level);
            }
            return;
        }
        ServerPlayer p = speler(level);
        if (p == null || !p.isAlive() || p.level() != level || stoel == null || !SPELERS.containsKey(speler)) {
            reset(level);
            return;
        }
        SPELERS.put(speler, level.getGameTime());
        if (p.distanceToSqr(Vec3.atCenterOf(stoel)) > BLIJF * BLIJF) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kapper.weggelopen").withStyle(ChatFormatting.LIGHT_PURPLE));
            finish(npc, level, p);
            return;
        }
        if (ticks % 20 == 0) {
            Minigames.keep(p);
        }
        switch (fase) {
            case AFTELLEN -> {
                timer--;
                if (timer > 0 && timer % 20 == 0) {
                    title(p, Component.literal(String.valueOf(timer / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 16);
                    level.playSound(null, stoel, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS, 1f, 0.8f + (3 - timer / 20) * 0.15f);
                } else if (timer <= 0) {
                    nieuweKlant(npc, level, p);
                }
            }
            case KLANT -> {
                KapperKlantEntity k = klant(level);
                if (k == null) {                                   // (gone: e.g. its chunk unloaded) a new one comes
                    nieuweKlant(npc, level, p);
                    return;
                }
                k.snapTo(stoelPlek().x, stoelPlek().y, stoelPlek().z, k.getYRot(), 0);
                if (fotoTot > 0 && --fotoTot == 0) {
                    stuur(npc, p);                                 // the picture goes away
                    p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.foto_weg").withStyle(ChatFormatting.YELLOW));
                }
                if (--timer <= 0) {
                    teLaat(npc, level, p, k);
                } else if (ticks % 10 == 0) {
                    bar(p);
                }
            }
            case TUSSEN -> {
                if (--timer <= 0) {
                    if (nr >= aantal) {
                        finish(npc, level, p);
                    } else {
                        nieuweKlant(npc, level, p);
                    }
                }
            }
            default -> {
            }
        }
    }

    /** The next customer comes in and sits down, with the picture of its wish. */
    void nieuweKlant(GuhNpcEntity npc, ServerLevel level, ServerPlayer p) {
        RandomSource r = level.getRandom();
        KapperKlantEntity old = klant(level);
        if (old != null && old.weg == 0) {
            old.discard();
        }
        do {
            wens = feest ? FEEST_KAPSELS.get(r.nextInt(FEEST_KAPSELS.size())) : Kapsel.values()[r.nextInt(Kapsel.values().length)];
        } while (wens == vorigeWens);
        vorigeWens = wens;
        if (feest) {
            wensVerf = FEEST_VERVEN.get(r.nextInt(FEEST_VERVEN.size()));
        } else {
            wensVerf = r.nextFloat() < (nr < 2 ? 0.45f : 0.2f) ? null : Haarverf.values()[r.nextInt(Haarverf.values().length)];
        }
        KapperKlantEntity k = KapperFeature.KAPPER_KLANT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (k == null) {
            return;
        }
        k.kapper = kapperId;
        k.setVariant(KLANT_VARIANTEN.get(r.nextInt(KLANT_VARIANTEN.size())));
        if (r.nextBoolean()) {                                   // a messy "before": some other hairstyle
            Kapsel voor;
            do {
                voor = Kapsel.values()[r.nextInt(Kapsel.values().length)];
            } while (voor == wens);
            k.wear(voor.kleding);
        }
        Vec3 plek = stoelPlek();
        BlockState state = level.getBlockState(stoel);
        float yaw = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING).toYRot() : 0f;
        k.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        k.setYHeadRot(yaw);
        k.setYBodyRot(yaw);
        k.setNoGravity(true);
        k.setOrderedToSit(true);
        k.setInSittingPose(true);
        level.addFreshEntity(k);
        klant = k.getUUID();
        fase = Fase.KLANT;
        geduld = timer = geduld(nr, feest);
        gewassen = 0;
        gekozen = null;
        gekozenVerf = -1;
        fotoTot = nr >= FOTO_WEG_VANAF ? FOTO_TICKS : -1;
        level.playSound(null, stoel, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1f, 1.5f);
        level.sendParticles(ParticleTypes.HEART, plek.x, plek.y + 1.1, plek.z, 2, 0.2, 0.1, 0.2, 0);
        p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.tingeling", nr + 1, aantal).withStyle(ChatFormatting.LIGHT_PURPLE));
        openKnip(npc, p);
    }

    /** One more round of foam (three are needed before cutting). */
    void was(GuhNpcEntity npc, ServerLevel level, ServerPlayer p) {
        KapperKlantEntity k = klant(level);
        if (fase != Fase.KLANT || k == null) {
            return;
        }
        if (gewassen < WASSEN) {
            gewassen++;
            level.sendParticles(ParticleTypes.BUBBLE_POP, k.getX(), k.getY() + k.getBbHeight() + 0.2, k.getZ(), 8 * gewassen, 0.3, 0.15, 0.3, 0.02);
            level.sendParticles(ParticleTypes.CLOUD, k.getX(), k.getY() + k.getBbHeight() + 0.1, k.getZ(), 3 + 2 * gewassen, 0.25, 0.05, 0.25, 0.0);
            level.playSound(null, k.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1f, 0.9f + 0.2f * gewassen);
            if (gewassen == WASSEN) {
                level.playSound(null, k.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.7f, 1.4f);
            }
        }
        stuur(npc, p);
    }

    /** Cut a hairstyle (after washing). */
    void knip(GuhNpcEntity npc, ServerLevel level, ServerPlayer p, Kapsel kapsel) {
        KapperKlantEntity k = klant(level);
        if (fase != Fase.KLANT || k == null) {
            return;
        }
        if (gewassen < WASSEN) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.eerst_wassen").withStyle(ChatFormatting.RED));
        } else {
            gekozen = kapsel;
            KapperHaar.geefKapsel(k, kapsel);
        }
        stuur(npc, p);
    }

    /** Dye it (after cutting); NATUREL = natural. */
    void verf(GuhNpcEntity npc, ServerLevel level, ServerPlayer p, int verf) {
        KapperKlantEntity k = klant(level);
        if (fase != Fase.KLANT || k == null) {
            return;
        }
        if (gekozen == null) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.eerst_knippen").withStyle(ChatFormatting.RED));
        } else {
            gekozenVerf = verf;
            KapperHaar.verf(k, verf >= NATUREL ? null : Haarverf.values()[verf]);
            KapperHaar.glitter(k, 8);
        }
        stuur(npc, p);
    }

    /** Look at the picture again (costs points). */
    void kijk(GuhNpcEntity npc, ServerPlayer p) {
        if (fase == Fase.KLANT && fotoTot == 0) {
            fotoTot = KIJK_TICKS;
            score = Math.max(0, score - KIJK_PRIJS);
        }
        stuur(npc, p);
    }

    /** The föhn: finishes this customer, and the points are counted. */
    void fohn(GuhNpcEntity npc, ServerLevel level, ServerPlayer p) {
        KapperKlantEntity k = klant(level);
        if (fase != Fase.KLANT || k == null) {
            return;
        }
        if (gekozen == null) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.eerst_knippen").withStyle(ChatFormatting.RED));
            stuur(npc, p);
            return;
        }
        boolean kapselGoed = gekozen == wens;
        boolean kleurGoed = wensVerf == null ? gekozenVerf == NATUREL || gekozenVerf < 0 : gekozenVerf == wensVerf.ordinal();
        int punten = punten(kapselGoed, kleurGoed, timer / 20, combo);
        score += punten;
        level.playSound(null, k.blockPosition(), KapperFeature.FOHN.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CLOUD, k.getX(), k.getY() + k.getBbHeight(), k.getZ(), 10, 0.3, 0.2, 0.3, 0.05);
        if (kapselGoed) {
            KnusVoortgang.tel(p, KapperVoortgang.KLANTEN, 1);
            GuhAdvancements.grant(p, "kapper_eerste_klant");
            KapperVoortgang.toon(p, "kapper_eerste_kapsel");
        }
        if (kapselGoed && kleurGoed) {
            combo++;
            perfect++;
            KnusVoortgang.tel(p, KapperVoortgang.PERFECT, 1);
            KapperVoortgang.ontdek(p, wens, wensVerf);
            KapperHaar.glitter(k, 16);
            level.sendParticles(ParticleTypes.HEART, k.getX(), k.getY() + k.getBbHeight() + 0.3, k.getZ(), 5, 0.3, 0.2, 0.3, 0);
            level.playSound(null, k.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
            uitslag = combo >= 3 ? "quest.guhs.kapper.uitslag.combo" : "quest.guhs.kapper.uitslag.perfect";
            title(p, Component.translatable("quest.guhs.kapper.title.vahoeg").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable(uitslag, punten, combo), 20);
        } else {
            combo = 0;
            uitslag = kapselGoed ? "quest.guhs.kapper.uitslag.kleur_fout" : kleurGoed ? "quest.guhs.kapper.uitslag.kapsel_fout" : "quest.guhs.kapper.uitslag.njeg";
            level.playSound(null, k.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 0.8f);
            title(p, Component.translatable("quest.guhs.kapper.title.njeg").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable(uitslag, punten, combo), 20);
        }
        uitslagPunten = punten;
        klaarMetKlant(npc, k, p);
    }

    /** Out of patience: the customer leaves (still lief: it has to go to the bakery), no points. */
    private void teLaat(GuhNpcEntity npc, ServerLevel level, ServerPlayer p, KapperKlantEntity k) {
        combo = 0;
        uitslag = "quest.guhs.kapper.uitslag.te_laat";
        uitslagPunten = 0;
        level.playSound(null, k.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 0.7f);
        title(p, Component.translatable("quest.guhs.kapper.title.te_laat").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable(uitslag, 0, 0), 20);
        klaarMetKlant(npc, k, p);
    }

    private void klaarMetKlant(GuhNpcEntity npc, KapperKlantEntity k, ServerPlayer p) {
        k.vertrek(VERTREK_TICKS);
        nr++;
        fase = Fase.TUSSEN;
        timer = TUSSEN_TICKS;
        stuur(npc, p);
    }

    /** The end of a show: krulmunten, the record, the first present, the advancements and the feest round. */
    void finish(GuhNpcEntity npc, ServerLevel level, ServerPlayer p) {
        int munten = munten(score);
        CompoundTag saved = GuhQuests.saved(p);
        if (munten > 0) {
            Minigames.give(p, new ItemStack(KapperFeature.KRULMUNT.get(), munten));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.kapper.klaar", score, perfect, nr, munten).withStyle(ChatFormatting.GOLD));
        int best = best(p);
        if (score > best) {
            saved.putInt(BEST, score);
            p.sendSystemMessage(best > 0 ? Component.translatable("quest.guhs.kapper.record", score, best).withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("quest.guhs.kapper.record_eerste", score).withStyle(ChatFormatting.YELLOW));
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        } else if (best > 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kapper.best", best).withStyle(ChatFormatting.GRAY));
        }
        if (score > 0 && Scorebord.submit(p, BOARD, score, false) > 0) {
            showScores(npc);
        }
        if (!saved.getBooleanOr(FIRST, false)) {
            saved.putBoolean(FIRST, true);
            Minigames.give(p, new ItemStack(KapperFeature.KRULMUNT.get(), FIRST_MUNTEN));
            GuhQuests.say(p, npc, "quest.guhs.kapper.eerste");
        }
        KnusVoortgang.tel(p, KapperVoortgang.SHOWS, 1);
        KnusVoortgang.hoogste(p, KapperVoortgang.RECORD, score);
        GuhAdvancements.grant(p, "kapper_eerste_show");
        if (score >= KapperVoortgang.SHOW_DOEL) {
            GuhAdvancements.grant(p, "kapper_show");
            KapperVoortgang.toon(p, "kapper_show");
        }
        if (feest) {
            if (score >= FEEST_MIN && Knusfeest.open(p, Feesttaak.FEESTKAPSELS)) {
                Minigames.give(p, new ItemStack(KapperFeature.FEESTKAPSELSET.get()));
                Knusfeest.gemaakt(p, Feesttaak.FEESTKAPSELS);
                GuhQuests.say(p, npc, "quest.guhs.kapper.feest_gelukt");
            } else if (score < FEEST_MIN) {
                GuhQuests.say(p, npc, "quest.guhs.kapper.feest_bijna", FEEST_MIN);
            }
        }
        sluitKnip(npc, p);
        reset(level);
    }

    /** Ends the show (the customer poofs, the player is an ordinary player again). */
    void reset(ServerLevel level) {
        KapperKlantEntity k = klant(level);
        if (k != null && k.weg == 0) {
            k.vertrek(5);
        }
        if (speler != null) {
            SPELERS.remove(speler);
        }
        speler = null;
        klant = null;
        fase = Fase.IDLE;
        timer = 0;
        wens = gekozen = null;
        wensVerf = null;
        gekozenVerf = -1;
    }

    // --- the chair ----------------------------------------------------------------------------------------------------------

    /** Finds the showstoel: the kappersstoel nearest to Krulletje. */
    void scan(GuhNpcEntity npc, ServerLevel level) {
        lastScan = level.getGameTime();
        if (stoel != null && level.getBlockState(stoel).is(KapperFeature.KAPPERSSTOEL.get())) {
            return;
        }
        stoel = null;
        BlockPos c = npc.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-REACH, -4, -REACH), c.offset(REACH, 4, REACH))) {
            if (level.getBlockState(pos).is(KapperFeature.KAPPERSSTOEL.get()) && (stoel == null || pos.distSqr(c) < stoel.distSqr(c))) {
                stoel = pos.immutable();
            }
        }
    }

    /** Where a customer sits: on the seat of the showstoel. */
    Vec3 stoelPlek() {
        return new Vec3(stoel.getX() + 0.5, stoel.getY() + 0.45, stoel.getZ() + 0.5);
    }

    // --- the screens ---------------------------------------------------------------------------------------------------------

    /** The state of the show for the knip screen. */
    CompoundTag data(ServerLevel level) {
        CompoundTag d = new CompoundTag();
        KapperKlantEntity k = klant(level);
        d.putString("Fase", fase.name());
        d.putInt("Klant", k == null ? -1 : k.getId());
        d.putString("Variant", k == null ? "normal" : k.getVariant().id());
        d.putInt("Nr", nr);
        d.putInt("Aantal", aantal);
        d.putInt("Score", score);
        d.putInt("Combo", combo);
        d.putInt("Timer", fase == Fase.KLANT ? timer : 0);
        d.putInt("Geduld", geduld);
        d.putInt("Wens", wens == null ? -1 : wens.ordinal());
        d.putInt("WensVerf", wensVerf == null ? -1 : wensVerf.ordinal());
        d.putBoolean("Foto", fotoTot != 0);
        d.putInt("Gewassen", gewassen);
        d.putInt("Gekozen", gekozen == null ? -1 : gekozen.ordinal());
        d.putInt("GekozenVerf", gekozenVerf);
        d.putString("Uitslag", uitslag);
        d.putInt("UitslagPunten", uitslagPunten);
        d.putBoolean("Feest", feest);
        return d;
    }

    void openKnip(GuhNpcEntity npc, ServerPlayer p) {
        CompoundTag d = data((ServerLevel) npc.level());
        d.putBoolean("Open", true);
        ModNetworking.sendTo(p, new KapperPayloads.Knip(npc.getId(), d));
    }

    /** Updates the knip screen if it's open (it doesn't open it). */
    void stuur(GuhNpcEntity npc, ServerPlayer p) {
        ModNetworking.sendTo(p, new KapperPayloads.Knip(npc.getId(), data((ServerLevel) npc.level())));
    }

    void sluitKnip(GuhNpcEntity npc, ServerPlayer p) {
        CompoundTag d = new CompoundTag();
        d.putBoolean("Sluit", true);
        ModNetworking.sendTo(p, new KapperPayloads.Knip(npc.getId(), d));
    }

    /** The actionbar: customer, patience, score. */
    private void bar(ServerPlayer p) {
        int s = (timer + 19) / 20;
        p.sendOverlayMessage(Component.translatable("quest.guhs.kapper.bar.klant", nr + 1, aantal, s, score)
                .withStyle(s <= 5 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE));
    }

    /** The floating top 3 above the showstoel. */
    public static void showScores(GuhNpcEntity npc) {
        KappersShow show = of(npc);
        if (show.stoel == null) {
            return;
        }
        ServerLevel level = (ServerLevel) npc.level();
        Scorebord.show(level, Vec3.atBottomCenterOf(show.stoel).add(0, 3.2, 0), "kapper", Scorebord.text(level.getServer(),
                Component.translatable("gui.guhs.scorebord.kapper"), List.of(BOARD), List.of(Component.translatable("gui.guhs.scorebord.kapper.punten")),
                n -> n + " pt"));
    }

    public static int best(Player player) {
        return GuhQuests.saved(player).getIntOr(BEST, 0);
    }

    @Nullable
    private ServerPlayer speler(ServerLevel level) {
        return speler == null ? null : level.getServer().getPlayerList().getPlayer(speler);
    }

    @Nullable
    GuhNpcEntity kapper(ServerLevel level) {
        return level.getEntity(kapperId) instanceof GuhNpcEntity npc ? npc : null;
    }

    private static void title(ServerPlayer p, Component title, @Nullable Component sub, int stay) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(2, stay, 6));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub == null ? Component.empty() : sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // --- events --------------------------------------------------------------------------------------------------------------

    /** No hurting players in a show. */
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isPlaying(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A player whose show stopped ticking (its chunk unloaded, Krulletje vanished) is an ordinary player again. */
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
        SHOWS.clear();
        SPELERS.clear();
    }

    /** (Tests) the customers' wishes of this show: set them by hand. */
    void wens(Kapsel kapsel, @Nullable Haarverf verf) {
        wens = kapsel;
        wensVerf = verf;
    }

    /** (Tests) the customer entities nearby that belong to no show any more. */
    static List<Entity> leftovers(ServerLevel level, net.minecraft.world.phys.AABB box) {
        return new ArrayList<>(level.getEntitiesOfClass(KapperKlantEntity.class, box, k -> !hoortBij(k) && k.weg == 0));
    }
}
