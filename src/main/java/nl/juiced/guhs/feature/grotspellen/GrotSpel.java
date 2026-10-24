package nl.juiced.guhs.feature.grotspellen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * 1.7.0 "Onder de Wol" (design: ontwerp-guhs170-mijnen.md, rondes 41-43): what the three cave minigames share. Each game
 * lives in its own cave hall under a little entrance hut (the Wolkenliftje takes you down), with a host guh who talks with
 * the 2.8 talking screen ({@link Praat}): pick makkelijk / medium / lastig, the shop, "hoe werkt het?".
 * <p>
 * One player at a time per hall; a game is: countdown (3 s), play ({@link #speelTicks}), the result. While you play you
 * can't get hurt or hungry ({@link Minigames#startKeeping}). The result: points (the board &lt;game&gt;_&lt;level&gt;, the
 * world's top 3 floats above the host), coins ({@link #munten(Niveau, int, boolean)}: capped per level, +5 for beating your
 * own record on that level) and, the very first time, a little present. Games live in memory (a restart ends them);
 * the hall is found by scanning around the host for the game's own blocks ({@link #zoek}) and reset after every game
 * ({@link #opruimen}).
 */
public abstract class GrotSpel {
    public enum Fase { RUST, AFTEL, SPELEN, UITSLAG }

    /** The three games: id (boards, lang, the Minigames tab), host kind, structure. */
    public enum Soort {
        SCHATGRAVEN("schatgraven", "grotspel_schatgraven", Schatgraven::new),
        GLIMWORMVANGEN("glimwormvangen", "grotspel_glimwormen", Glimwormvangen::new),
        LICHTJESPUZZEL("lichtjespuzzel", "grotspel_lichtjes", Lichtjespuzzel::new);

        public final String id;
        public final String structuur;
        final Function<UUID, GrotSpel> maak;

        Soort(String id, String structuur, Function<UUID, GrotSpel> maak) {
            this.id = id;
            this.structuur = structuur;
            this.maak = maak;
        }

        /** The Scorebord board of a level: &lt;id&gt;_makkelijk / _medium / _lastig. */
        public String bord(Niveau n) {
            return id + "_" + n.id();
        }

        public List<String> borden() {
            List<String> b = new ArrayList<>();
            for (Niveau n : Niveau.values()) {
                b.add(bord(n));
            }
            return b;
        }

        public GuhNpcEntity.Kind npc() {
            return switch (this) {
                case SCHATGRAVEN -> GuhNpcEntity.Kind.SCHATGRAAFGUH;
                case GLIMWORMVANGEN -> GuhNpcEntity.Kind.GLIMWORMGUH;
                case LICHTJESPUZZEL -> GuhNpcEntity.Kind.LAMPJESGUH;
            };
        }

        public Item munt() {
            return switch (this) {
                case SCHATGRAVEN -> GrotSpellenFeature.SCHATMUNTJE.get();
                case GLIMWORMVANGEN -> GrotSpellenFeature.GLIMMERTJE.get();
                case LICHTJESPUZZEL -> GrotSpellenFeature.LAMPJESMUNT.get();
            };
        }

        @Nullable
        public static Soort van(GuhNpcEntity.Kind kind) {
            for (Soort s : values()) {
                if (s.npc() == kind) {
                    return s;
                }
            }
            return null;
        }
    }

    public static final int AFTEL_TICKS = 61, UITSLAG_TICKS = 60, BORD_TICKS = 100;
    /** How far around the host the hall's blocks may be (horizontal, and down/up), and when you've walked away. */
    public static final int SCAN = 30, SCAN_ONDER = 4, SCAN_BOVEN = 10, WEG = 34, WEG_HOOGTE = 12;
    /** A game whose host hasn't ticked for this long is over. */
    private static final int STALE_TICKS = 60;
    /** The most coins of a game per level (a record adds 5 on top). */
    public static final int[] MAX_MUNTEN = {25, 45, 70};
    public static final int RECORD_BONUS = 5;
    /** Answers in the talking screen. */
    public static final int OPTIE_MAKKELIJK = 1, OPTIE_MEDIUM = 2, OPTIE_LASTIG = 3, OPTIE_WINKEL = 4, OPTIE_UITLEG = 5, OPTIE_STOP = 6,
            OPTIE_TERUG = 8, OPTIE_DOEI = 9;

    private static final Map<UUID, GrotSpel> SPELLEN = new ConcurrentHashMap<>();
    /** Everyone playing right now: player -> their host. */
    private static final Map<UUID, UUID> SPELERS = new ConcurrentHashMap<>();

    protected final UUID npcId;
    @Nullable
    protected UUID speler;
    protected String spelerNaam = "";
    protected Niveau niveau = Niveau.MEDIUM;
    protected Fase fase = Fase.RUST;
    protected int aftel, over, uitslagTimer;
    protected long laatsteTick, stilTot;
    protected boolean gescand;
    /** The spot next to the host where you end up after a game. */
    @Nullable
    protected BlockPos thuis;
    /** A player left while the host's level wasn't at hand: the hall is reset on the host's next tick. */
    private boolean moetOpruimen;

    protected GrotSpel(UUID npcId) {
        this.npcId = npcId;
    }

    public abstract Soort soort();

    // --- what each game fills in ------------------------------------------------------------------------------------
    /** Finds the hall's game blocks around the host (once; again when they're gone). */
    protected abstract boolean zoek(ServerLevel world, GuhNpcEntity npc);

    /** Are the hall's game blocks still there? */
    protected abstract boolean heel(ServerLevel world);

    /** How long a game lasts on this level (ticks). */
    protected abstract int speelTicks();

    /** A new game: set up the hall for this level, put the player at the start. */
    protected abstract void begin(ServerLevel world, ServerPlayer p, GuhNpcEntity npc);

    /** Every tick while playing. */
    protected abstract void speelTick(ServerLevel world, ServerPlayer p, GuhNpcEntity npc);

    /** Done before the time is up (everything found, the puzzle solved)? */
    protected abstract boolean klaar();

    /** The line above the hotbar while playing. */
    protected abstract Component balk();

    protected abstract int punten();

    /** Coins for this game ({@link #munten(Niveau, int, boolean)} with the game's own sum). */
    protected abstract int munten(boolean record);

    /** The chat lines of the result (after the title line). */
    protected abstract List<Component> uitslagRegels();

    /** After every game (and when a player left): the hall back as it was, ready for the next one. */
    protected abstract void opruimen(ServerLevel world);

    /** The tools you borrow for a game. */
    protected abstract List<ItemStack> leen();

    /** Every tick of the host, also without a game (tidying up, the idle look). */
    protected void rustTick(ServerLevel world, GuhNpcEntity npc) {
    }

    // --- the registry -----------------------------------------------------------------------------------------------
    @Nullable
    public static GrotSpel van(GuhNpcEntity npc) {
        Soort soort = Soort.van(npc.getKind());
        if (soort == null) {
            return null;
        }
        GrotSpel spel = SPELLEN.computeIfAbsent(npc.getUUID(), soort.maak);
        return spel.soort() == soort ? spel : null;
    }

    public static boolean speelt(Player p) {
        return SPELERS.containsKey(p.getUUID());
    }

    /** Is this player playing this game now? */
    public static boolean speelt(Player p, Soort soort) {
        GrotSpel spel = spelVan(p);
        return spel != null && spel.soort() == soort;
    }

    /** The game this player is in (null: none). */
    @Nullable
    public static GrotSpel spelVan(Player p) {
        UUID npc = SPELERS.get(p.getUUID());
        GrotSpel spel = npc == null ? null : SPELLEN.get(npc);
        return spel != null && spel.isPlayedBy(p) ? spel : null;
    }

    public boolean isRunning() {
        return speler != null;
    }

    public boolean isPlayedBy(Player p) {
        return p.getUUID().equals(speler);
    }

    public Fase fase() {
        return fase;
    }

    public Niveau niveau() {
        return niveau;
    }

    public int over() {
        return over;
    }

    // --- talking ----------------------------------------------------------------------------------------------------
    private String t(String rest) {
        return "gui.guhs." + soort().id + "." + rest;
    }

    public static void talk(GuhNpcEntity npc, ServerPlayer p) {
        GrotSpel spel = van(npc);
        if (spel == null) {
            return;
        }
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.95f);
        SpelGroepen.bezoek(p, spel.soort().id);
        GrotSpellenFeature.checkKleding(p);
        String g = "gui.guhs.grotspel.optie.";
        if (spel.isPlayedBy(p)) {
            Praat.open(p, npc, null, spel.t("bezig_jij"), new Object[0], new Praat.Optie(OPTIE_STOP, g + "stop"),
                    new Praat.Optie(OPTIE_DOEI, g + "verder"));
        } else if (spel.isRunning()) {
            Praat.open(p, npc, null, "gui.guhs.grotspel.bezig", new Object[]{spel.spelerNaam}, new Praat.Optie(OPTIE_WINKEL, g + "winkel"),
                    new Praat.Optie(OPTIE_UITLEG, g + "uitleg"), new Praat.Optie(OPTIE_DOEI, g + "doei"));
        } else {
            Object[] records = {record(p, spel.soort(), Niveau.MAKKELIJK), record(p, spel.soort(), Niveau.MEDIUM), record(p, spel.soort(), Niveau.LASTIG)};
            Praat.open(p, npc, null, spel.t("hallo." + npc.getRandom().nextInt(3)), records,
                    new Praat.Optie(OPTIE_MAKKELIJK, g + "makkelijk"), new Praat.Optie(OPTIE_MEDIUM, g + "medium"),
                    new Praat.Optie(OPTIE_LASTIG, g + "lastig"), new Praat.Optie(OPTIE_WINKEL, g + "winkel"),
                    new Praat.Optie(OPTIE_UITLEG, g + "uitleg"), new Praat.Optie(OPTIE_DOEI, g + "doei"));
        }
    }

    private static String record(ServerPlayer p, Soort soort, Niveau n) {
        int best = best(p, soort, n);
        return best < 0 ? "-" : String.valueOf(best);
    }

    public static void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        GrotSpel spel = van(npc);
        if (spel == null || p.distanceToSqr(npc) > 100) {
            return;
        }
        String g = "gui.guhs.grotspel.optie.";
        switch (optie) {
            case OPTIE_MAKKELIJK, OPTIE_MEDIUM, OPTIE_LASTIG -> {
                Praat.sluit(p);
                spel.start(npc, p, Niveau.of(optie - 1));
            }
            case OPTIE_WINKEL -> {
                Praat.sluit(p);
                npc.openShop(p);
            }
            case OPTIE_UITLEG -> Praat.open(p, npc, null, spel.t("uitleg"), new Object[0], new Praat.Optie(OPTIE_TERUG, g + "terug"),
                    new Praat.Optie(OPTIE_DOEI, g + "doei"));
            case OPTIE_STOP -> {
                Praat.sluit(p);
                if (spel.isPlayedBy(p)) {
                    spel.einde(npc, p, "quest.guhs.grotspel.gestopt", true);
                }
            }
            case OPTIE_TERUG -> talk(npc, p);
            case OPTIE_DOEI -> Praat.sluit(p);
            default -> {
            }
        }
    }

    // --- a game -----------------------------------------------------------------------------------------------------
    /** Starts a game for this player on this level; false (with the host saying why) when it can't. */
    public boolean start(GuhNpcEntity npc, ServerPlayer p, Niveau n) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return false;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.grotspel.bezig", spelerNaam);
            return false;
        }
        if (speelt(p)) {
            GuhQuests.say(p, npc, "quest.guhs.grotspel.elders");
            return false;
        }
        if (Minigames.refuse(p, npc, soort().id)) {
            return false;
        }
        if (gescand && !heel(world)) {
            gescand = false;
        }
        if (!gescand && !scan(world, npc)) {
            GuhQuests.say(p, npc, "quest.guhs.grotspel.kapot");
            return false;
        }
        int vrij = 0;
        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            vrij += p.getInventory().getItem(i).isEmpty() || GrotSpellenFeature.geleend(p.getInventory().getItem(i)) ? 1 : 0;
        }
        if (vrij < leen().size()) {
            GuhQuests.say(p, npc, "quest.guhs.grotspel.vol");
            return false;
        }
        if (moetOpruimen) {
            opruimen(world);
            moetOpruimen = false;
        }
        speler = p.getUUID();
        spelerNaam = p.getGameProfile().name();
        niveau = n;
        SPELERS.put(speler, npcId);
        laatsteTick = world.getGameTime();
        Minigames.startKeeping(p);
        p.clearFire();
        begin(world, p, npc);
        geefGereedschap(p);
        fase = Fase.AFTEL;
        aftel = AFTEL_TICKS;
        over = speelTicks();
        GuhQuests.say(p, npc, "quest.guhs." + soort().id + ".start", n.naam());
        p.sendSystemMessage(Component.translatable("quest.guhs." + soort().id + ".hoe").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Every tick of the host: the board now and then, and the game. */
    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        laatsteTick = world.getGameTime();
        if (npc.tickCount % BORD_TICKS == 1) {
            if (!gescand && world.getNearestPlayer(npc, 40) != null) {
                scan(world, npc);
            }
            toonScores(npc);
        }
        if (moetOpruimen && speler == null && gescand) {
            opruimen(world);
            moetOpruimen = false;
        }
        if (npc.tickCount % 40 == 0) {
            for (ServerPlayer near : world.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(12))) {
                GrotSpellenFeature.checkKleding(near);
            }
        }
        rustTick(world, npc);
        if (speler == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(speler);
        if (p == null || !p.isAlive() || p.level() != world || weg(p, npc)) {
            if (p != null && p.isAlive() && p.level() == world) {
                einde(npc, p, "quest.guhs.grotspel.weggelopen", false);
            } else {
                if (p != null) {
                    neemTerug(p);
                }
                einde(npc, null, null, false);
            }
            return;
        }
        if (npc.tickCount % 20 == 0) {
            Minigames.keep(p);
            p.clearFire();
            if (fase == Fase.AFTEL || fase == Fase.SPELEN) {
                for (ItemStack tool : leen()) {
                    if (!p.getInventory().hasAnyMatching(s -> s.is(tool.getItem())) && p.getInventory().getFreeSlot() >= 0) {
                        p.getInventory().add(tool.copy());              // (lost it somehow? here it is again)
                    }
                }
            }
        }
        switch (fase) {
            case AFTEL -> {
                aftel--;
                if (aftel == 60 || aftel == 40 || aftel == 20) {
                    titel(p, Component.literal(String.valueOf(aftel / 20)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.grotspel.aftel_sub"), 0, 22, 0);
                    geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 0.9f, 1.0f);
                }
                if (aftel <= 0) {
                    fase = Fase.SPELEN;
                    titel(p, Component.translatable("gui.guhs.grotspel.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs." + soort().id + ".go_sub"), 0, 30, 10);
                    geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2.0f);
                    npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.0f);
                }
            }
            case SPELEN -> {
                over--;
                speelTick(world, p, npc);
                if (speler == null) {
                    return;
                }
                if (klaar() || over <= 0) {
                    uitslag(npc, p);
                    return;
                }
                if (over == 200 || over == 100) {
                    geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.4f);
                }
            }
            case UITSLAG -> {
                if (--uitslagTimer <= 0) {
                    einde(npc, p, null, true);
                    return;
                }
            }
            default -> {
            }
        }
        if (fase == Fase.SPELEN && npc.tickCount % 10 == 0 && world.getGameTime() >= stilTot) {
            p.sendOverlayMessage(balk());
        }
    }

    /** The time left as "1:05". */
    protected Component klok() {
        int s = Math.max(0, (over + 19) / 20);
        return Component.literal(s / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60));
    }

    /** Shows a message above the hotbar for a moment (the status line waits). */
    protected void flits(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst);
        stilTot = p.level().getGameTime() + 30;
    }

    /** The time is up or the game is done: points, coins, the board, the host's verdict. */
    protected void uitslag(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        int punten = punten();
        CompoundTag saved = GuhQuests.saved(p);
        int best = best(p, soort(), niveau);
        boolean record = best >= 0 && punten > best;
        if (punten > 0 && (best < 0 || punten > best)) {
            saved.putInt(bestKey(soort(), niveau), punten);
        }
        String potjes = "guhs_" + soort().id + "_potjes";
        saved.putInt(potjes, saved.getIntOr(potjes, 0) + 1);
        int munten = munten(record);
        p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.uitslag", Component.translatable("gui.guhs.spelgroep." + soort().id),
                niveau.naam(), punten).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (Component regel : uitslagRegels()) {
            p.sendSystemMessage(regel.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (record) {
            p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.record", punten, best, RECORD_BONUS).withStyle(ChatFormatting.GOLD));
        } else if (best < 0 && punten > 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.eerste_record", punten).withStyle(ChatFormatting.GOLD));
        } else if (best >= 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.geen_record", best).withStyle(ChatFormatting.GRAY));
        }
        ItemStack munt = new ItemStack(soort().munt(), Math.max(1, munten));
        p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.munten", munten, munt.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (munten > 0) {
            Minigames.give(p, new ItemStack(soort().munt(), munten));
        }
        String eerste = "guhs_" + soort().id + "_eerste";
        if (!saved.getBooleanOr(eerste, false)) {                        // the very first game: a present from the host
            saved.putBoolean(eerste, true);
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            Minigames.give(p, GrotSpellenFeature.cadeautje(soort()));
            p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.cadeautje").withStyle(ChatFormatting.GOLD));
        }
        GrotSpellenFeature.advancement(p, soort().id + "_gespeeld");
        if (punten > 0 && Scorebord.submit(p, soort().bord(niveau), punten, false) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.grotspel.huis_record").withStyle(ChatFormatting.GOLD));
        }
        toonScores(npc);
        int max = MAX_MUNTEN[niveau.ordinal()];
        String oordeel = munten >= max * 6 / 10 ? "goed" : munten >= max / 4 ? "aardig" : "oefenen";
        titel(p, Component.translatable("gui.guhs.grotspel.klaar_" + oordeel, punten).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.grotspel.klaar_sub", munten, munt.getHoverName()), 5, 60, 15);
        geluid(p, oordeel.equals("goed") ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
        npc.playSound(ModSounds.GUH_HAPPY.get(), 1f, oordeel.equals("goed") ? 1.3f : 1.0f);
        world.sendParticles(oordeel.equals("goed") ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                24, 0.8, 0.6, 0.8, 0.1);
        GuhQuests.say(p, npc, "quest.guhs." + soort().id + ".oordeel_" + oordeel);
        neemTerug(p);
        fase = Fase.UITSLAG;
        uitslagTimer = UITSLAG_TICKS;
    }

    /** The game ends: tools back, the hall reset. After a whole game you're back next to the host. */
    protected void einde(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String bericht, boolean naarHuis) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null) {
            neemTerug(p);
            if (bericht != null) {
                p.sendSystemMessage(Component.translatable(bericht).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (naarHuis && thuis != null && p.level() == world && p.isAlive()) {
                p.teleportTo(world, thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5, java.util.Set.of(), p.getYRot(), 0, true);
            }
        }
        if (speler != null) {
            SPELERS.remove(speler, npcId);
        }
        speler = null;
        fase = Fase.RUST;
        if (gescand) {
            opruimen(world);
        }
    }

    /** A player logged out, died or went to another dimension: their game is over (the hall resets on the next tick). */
    public static void verlaat(ServerPlayer p) {
        UUID npc = SPELERS.remove(p.getUUID());
        neemTerug(p);
        GrotSpel spel = npc == null ? null : SPELLEN.get(npc);
        if (spel != null && spel.isPlayedBy(p)) {
            spel.speler = null;
            spel.fase = Fase.RUST;
            spel.moetOpruimen = true;
        }
    }

    /** Is this player's host still there, and still ticking? */
    public static boolean hostInDeBuurt(ServerPlayer p) {
        UUID npc = SPELERS.get(p.getUUID());
        GrotSpel spel = npc == null ? null : SPELLEN.get(npc);
        return spel != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - spel.laatsteTick <= STALE_TICKS;
    }

    private boolean weg(ServerPlayer p, GuhNpcEntity npc) {
        return Math.max(Math.abs(p.getX() - npc.getX()), Math.abs(p.getZ() - npc.getZ())) > WEG || Math.abs(p.getY() - npc.getY()) > WEG_HOOGTE;
    }

    /**
     * The air of the host's own hall: everything you can walk or fly through that is connected to the host's spot, within
     * {@link #SCAN} blocks sideways and from one below to {@link #SCAN_BOVEN} above (the walls stop it, so a hall next door
     * is never mixed in). The game blocks are found next to it.
     */
    protected static java.util.Set<BlockPos> zaalLucht(ServerLevel world, GuhNpcEntity npc) {
        BlockPos c = npc.blockPosition();
        java.util.Set<BlockPos> gezien = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> rij = new java.util.ArrayDeque<>();
        rij.add(c);
        gezien.add(c);
        while (!rij.isEmpty() && gezien.size() < 40000) {
            BlockPos p = rij.poll();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos q = p.relative(d);
                if (gezien.contains(q) || Math.abs(q.getX() - c.getX()) > SCAN || Math.abs(q.getZ() - c.getZ()) > SCAN || q.getY() < c.getY() - 1
                        || q.getY() > c.getY() + SCAN_BOVEN || !world.isLoaded(q) || !world.getBlockState(q).getCollisionShape(world, q).isEmpty()) {
                    continue;
                }
                gezien.add(q);
                rij.add(q);
            }
        }
        return gezien;
    }

    /** The blocks of this kind right under the hall's air (a field, a raster in the floor). */
    protected static List<BlockPos> inDeVloer(ServerLevel world, java.util.Set<BlockPos> lucht, net.minecraft.world.level.block.Block blok) {
        List<BlockPos> uit = new ArrayList<>();
        for (BlockPos p : lucht) {
            if (world.getBlockState(p.below()).is(blok)) {
                uit.add(p.below());
            }
        }
        return uit;
    }

    /** Scans the hall (and a spot next to the host to come back to). */
    protected boolean scan(ServerLevel world, GuhNpcEntity npc) {
        if (!zoek(world, npc)) {
            return false;
        }
        BlockPos c = npc.blockPosition();
        thuis = null;
        for (int[] d : new int[][]{{0, 2}, {2, 0}, {0, -2}, {-2, 0}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {0, 3}, {3, 0}, {0, -3}, {-3, 0}}) {
            BlockPos pos = c.offset(d[0], 0, d[1]);
            var shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if ((shape.isEmpty() || shape.max(net.minecraft.core.Direction.Axis.Y) <= 0.25) && world.isEmptyBlock(pos.above())
                    && world.getBlockState(pos.below()).isSolid()) {
                thuis = pos;
                break;
            }
        }
        if (thuis == null) {
            thuis = c;
        }
        gescand = true;
        return true;
    }

    /** The world's top 3 per level floats above the host. */
    public void toonScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<Component> koppen = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            koppen.add(n.naam());
        }
        Component tekst = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord." + soort().id), soort().borden(), koppen,
                s -> s + " pt");
        Scorebord.show(world, npc.position().add(0, 2.6, 0), soort().id, tekst);
    }

    // --- coins and records ------------------------------------------------------------------------------------------
    /** Coins: this many, at most {@link #MAX_MUNTEN} for the level, +5 for a personal record. */
    public static int munten(Niveau n, int munten, boolean record) {
        return Math.max(0, Math.min(MAX_MUNTEN[n.ordinal()], munten)) + (record ? RECORD_BONUS : 0);
    }

    static String bestKey(Soort soort, Niveau n) {
        return "guhs_" + soort.bord(n) + "_best";
    }

    /** Your best points on a level (-1: none yet). */
    public static int best(Player p, Soort soort, Niveau n) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(bestKey(soort, n)) ? saved.getIntOr(bestKey(soort, n), 0) : -1;
    }

    // --- the tools ----------------------------------------------------------------------------------------------------
    private void geefGereedschap(ServerPlayer p) {
        neemTerug(p);
        boolean eerste = true;
        for (ItemStack tool : leen()) {
            if (eerste && p.getInventory().getSelectedItem().isEmpty()) {
                p.getInventory().setItem(p.getInventory().getSelectedSlot(), tool.copy());
            } else {
                p.getInventory().add(tool.copy());
            }
            eerste = false;
        }
    }

    /** Takes back every borrowed tool of the cave games (inventory, crafting grid, the mouse cursor). */
    public static void neemTerug(Player p) {
        if (p instanceof ServerPlayer sp && p.getInventory().hasAnyMatching(s -> s.is(GrotSpellenFeature.GLIMPOTJE_RUG.get()))) {
            GrotSpellenPayloads.stuur(sp, -1);
        }
        p.getInventory().clearOrCountMatchingItems(GrotSpellenFeature::geleend, -1, p.inventoryMenu.getCraftSlots());
        if (GrotSpellenFeature.geleend(p.containerMenu.getCarried())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    // --- helpers ------------------------------------------------------------------------------------------------------
    protected static void titel(ServerPlayer p, Component titel, Component sub, int in, int stay, int out) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(titel));
    }

    /** A sound only this player hears (26.1: ServerPlayer#playNotifySound is gone). */
    protected static void geluid(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(),
                volume, pitch, p.getRandom().nextLong()));
    }

    /** A spot in the world as the middle of a block, one up (for teleports). */
    protected static Vec3 op(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    // --- for the GameTests ------------------------------------------------------------------------------------------
    /** (tests) Skips the countdown. */
    public void testAftelOver() {
        if (fase == Fase.AFTEL) {
            aftel = 1;
        }
    }

    /** (tests) Sets the time left. */
    public void testTijd(int ticks) {
        over = ticks;
    }

    /** (tests) Forget every game. */
    public static void testVergeet() {
        SPELLEN.clear();
        SPELERS.clear();
    }
}
