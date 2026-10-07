package nl.juiced.guhs.feature.guhriow1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * World 1 of Super Guhrio, "de binnentuin" (levels 1-1 and 1-2 of the Kasteel van de Grote Nether-Mika): everything of it
 * that is not a block of a level. The levels themselves are built by tools/features/guhrio_w1_bouw.py and played by the
 * engine (feature/guhrio); this class listens to the engine and keeps, per player:
 * <ul>
 *     <li>the tips of the two levels ({@link #tip}): one line above the panel when you walk through a
 *     {@link GuhrioW1Blocks.TipBlok}, once per run, and only as long as you never finished that level;</li>
 *     <li>the secret rooms ({@link #geheim}, {@link #geheimGevonden}): the mole's den of 1-1 and the coin greenhouse of
 *     1-2, found for good the first time you stand in them;</li>
 *     <li>the three big vadsmunten of each of the two levels ({@link #vadsKlaar}; the engine keeps the coins themselves);</li>
 *     <li>the gag at the end of the world ({@link #bedank}): at the flagpole of 1-2 Pad-guh, who stands there in front of
 *     the wrong part of the castle, thanks you - "maar de prinses is in een ander kasteeldeel, njeg" - every time, in
 *     the chat and above the panel, a moment after the engine's own "gehaald" line.</li>
 * </ul>
 * Kept in {@code GuhQuests.saved(player)["guhs_guhriow1"]}; nothing here is shared between players and nothing in the
 * world changes. Hidden advancements (for the FTB quests): quest/guhrio_w1_geheim_1_1, _geheim_1_2, _vads_1_1, _vads_1_2,
 * _prinses; visible: guhrio/guhrio_w1_binnentuin (the world is done), guhrio/guhrio_w1_tuingeheimen (both secrets and all
 * six big vadsmunten).
 */
public final class Binnentuin {
    /** The two levels of this world (the engine's ids). */
    public static final String LEVEL_1 = "kasteel_1_1", LEVEL_2 = "kasteel_1_2";
    public static final List<String> LEVELS = List.of(LEVEL_1, LEVEL_2);
    /** The tips there are texts for (gui.guhs.guhriow1.tip.0 .. TIPS - 1). */
    public static final int TIPS = 9;
    /**
     * A tip stays above the panel for TIP_TICKS + 60 ticks (the game forgets such a line after 60: too short to read a
     * sentence): it is said once more after TIP_TICKS, to somebody who is still within TIP_BIJ blocks of where it began.
     */
    public static final int TIP_TICKS = 50;
    public static final double TIP_BIJ = 5.0;
    /** Ticks between the flagpole of 1-2 and Pad-guh's thanks (the engine's own line comes first). */
    public static final int BEDANK_TICKS = 25;
    /** How many different lines Pad-guh has for somebody who comes back (quest.guhs.guhriow1.padguh.alweer.&lt;n&gt;). */
    public static final int ALWEER = 3;
    /** The player's own data of this world. */
    public static final String SLEUTEL = "guhs_guhriow1";
    /**
     * Pad-guh at the end of 1-2: his Bezetting id and NpcRollen plek, and where he stands in the coordinates of the whole
     * castle (tools/features/guhrio_w1_bouw.py puts him there in the template; guhrio_w1.selfcheck compares the two).
     */
    public static final String PADGUH_ID = "guhriow1_padguh", PADGUH_PLEK = "guhriow1";
    public static final BlockPos PADGUH = new BlockPos(107, 39, 39);
    public static final float PADGUH_YAW = 270f;
    /**
     * The once-mark of guhrio-beloning's Pad-guh (Verhaallijn "guhrio_beloning", eenmalig "gag_1"): set the moment he has
     * said "de prinses is in een ander kasteeldeel" about world 1, which he calls after whoever finishes the world for the
     * first time. Only read here, by its name: that slice is not known to this one (and without it the mark is never set).
     */
    static final String BELONING_GAG = "guhs_guhrio_beloning_e_gag_1";

    /** The tips a run has shown already (bit = tip). */
    private static final Map<GuhrioSpel.Sessie, Integer> GETOOND = new WeakHashMap<>();

    /** Somebody Pad-guh is about to thank. */
    private static final class Wacht {
        final ServerPlayer speler;
        int ticks;

        Wacht(ServerPlayer speler, int ticks) {
            this.speler = speler;
            this.ticks = ticks;
        }
    }

    private static final List<Wacht> WACHT = new ArrayList<>();

    /** A tip somebody is reading: said once more when its ticks are up (see TIP_TICKS). */
    private static final class Leest {
        final ServerPlayer speler;
        final GuhrioSpel.Sessie sessie;
        final Component tekst;
        final Vec3 waar;
        int ticks = TIP_TICKS;

        Leest(ServerPlayer speler, GuhrioSpel.Sessie sessie, Component tekst) {
            this.speler = speler;
            this.sessie = sessie;
            this.tekst = tekst;
            this.waar = speler.position();
        }
    }

    private static final List<Leest> LEEST = new ArrayList<>();

    private Binnentuin() {
    }

    /** (GuhrioW1Feature.register) */
    static void registreer() {
        GuhrioSpel.BIJ_KLAAR.add(Binnentuin::klaar);
        Bezetting.npc(PADGUH_ID, GuhrioKasteel.STRUCTUUR, null, PADGUH, GuhNpcEntity.Kind.PADGUH, PADGUH_PLEK, PADGUH_YAW);
        NpcRollen.zet(GuhNpcEntity.Kind.PADGUH, PADGUH_PLEK, new PadguhRol());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> serverTick());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            WACHT.removeIf(w -> w.speler == event.getEntity());
            LEEST.removeIf(l -> l.speler == event.getEntity());
        });
        NeoForge.EVENT_BUS.addListener(Binnentuin::commandos);
    }

    // =====================================================================================================================
    // what stays
    // =====================================================================================================================

    private static CompoundTag spaar(Player player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (saved.getCompound(SLEUTEL).isEmpty()) {
            saved.put(SLEUTEL, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(SLEUTEL);
    }

    /** "1_1" for kasteel_1_1: the tail of the names of this world's texts and advancements. */
    private static String staart(String levelId) {
        return levelId.startsWith("kasteel_") ? levelId.substring("kasteel_".length()) : levelId;
    }

    /** Has this player found the secret room of this level? */
    public static boolean geheimGevonden(Player player, String levelId) {
        return spaar(player).getBooleanOr("Geheim_" + staart(levelId), false);
    }

    /** Has this player all three big vadsmunten of this level? */
    public static boolean vadsKlaar(Player player, String levelId) {
        return GuhrioKasteel.vadsmunten(player, levelId) == 7;
    }

    /** How often Pad-guh has thanked this player at the end of 1-2. */
    public static int bedankt(Player player) {
        return spaar(player).getIntOr("Bedankt", 0);
    }

    /** Both levels done, both secrets found, all six big vadsmunten: the garden has no secrets left. */
    public static boolean allesGevonden(Player player) {
        for (String id : LEVELS) {
            if (!GuhrioKasteel.gehaald(player, id) || !geheimGevonden(player, id) || !vadsKlaar(player, id)) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================================================================
    // the pieces
    // =====================================================================================================================

    /**
     * The player walks through tip {@code nr}: one line above the panel, once per run, until the level was finished once.
     * True when the line was shown.
     */
    static boolean tip(ServerPlayer player, GuhrioSpel.Sessie sessie, int nr) {
        if (nr < 0 || nr >= TIPS) {
            return false;
        }
        int getoond = GETOOND.getOrDefault(sessie, 0);
        if ((getoond >> nr & 1) != 0) {
            return false;
        }
        GETOOND.put(sessie, getoond | 1 << nr);
        if (GuhrioKasteel.gehaald(player, sessie.level().level().id())) {
            return false;                                       // (whoever has finished this level knows)
        }
        Component tekst = Component.translatable("gui.guhs.guhriow1.tip." + nr).withStyle(ChatFormatting.YELLOW);
        player.sendOverlayMessage(tekst);
        LEEST.removeIf(l -> l.speler == player);
        LEEST.add(new Leest(player, sessie, tekst));
        return true;
    }

    /** Is this player still reading a tip (it will be said once more; for tests)? */
    static boolean leest(ServerPlayer player) {
        return LEEST.stream().anyMatch(l -> l.speler == player);
    }

    /** The tips this run has walked through (bit = tip; for tests). */
    static int getoond(GuhrioSpel.Sessie sessie) {
        return GETOOND.getOrDefault(sessie, 0);
    }

    /** The player stands in the secret room of the level they are in. */
    static void geheim(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        String id = sessie.level().level().id();
        if (geheimGevonden(player, id)) {
            return;
        }
        spaar(player).putBoolean("Geheim_" + staart(id), true);
        LEEST.removeIf(l -> l.speler == player);                 // (another line above the panel: the tip is over)
        boolean eigen = LEVELS.contains(id);
        Component tekst = Component.translatable(eigen ? "gui.guhs.guhriow1.geheim." + staart(id) : "gui.guhs.guhriow1.geheim").withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(tekst);
        player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow1.geheim").withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        player.level().sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.5, 0.6, 0.5, 0.02);
        if (eigen) {
            GuhAdvancements.grant(player, "guhrio_w1_geheim_" + staart(id));
            kijkAlles(player);
        }
    }

    // =====================================================================================================================
    // the flagpole
    // =====================================================================================================================

    /** (GuhrioSpel.BIJ_KLAAR) somebody reached a flagpole. */
    private static void klaar(ServerPlayer player, GuhrioSpel.Sessie sessie, int ticks, boolean record) {
        String id = sessie.level().level().id();
        if (!LEVELS.contains(id)) {
            return;
        }
        kijkVads(player, id);
        if (LEVEL_2.equals(id)) {
            WACHT.removeIf(w -> w.speler == player);
            WACHT.add(new Wacht(player, BEDANK_TICKS));
        }
        kijkAlles(player);
    }

    /** All three big vadsmunten of this level: its hidden advancement, and one proud line the first time. */
    private static void kijkVads(ServerPlayer player, String id) {
        if (!vadsKlaar(player, id)) {
            return;
        }
        GuhAdvancements.grant(player, "guhrio_w1_vads_" + staart(id));
        CompoundTag spaar = spaar(player);
        if (!spaar.getBooleanOr("Vads_" + staart(id), false)) {
            spaar.putBoolean("Vads_" + staart(id), true);
            player.sendSystemMessage(Component.translatable("gui.guhs.guhriow1.vads", levelNaam(id)).withStyle(ChatFormatting.GOLD));
        }
    }

    private static String levelNaam(String id) {
        return staart(id).replace('_', '-');
    }

    private static void kijkAlles(ServerPlayer player) {
        if (allesGevonden(player)) {
            GidsFeature.grant(player, "guhrio/guhrio_w1_tuingeheimen");
        }
    }

    private static void serverTick() {
        for (Iterator<Leest> it = LEEST.iterator(); it.hasNext();) {
            Leest l = it.next();
            if (l.speler.isRemoved() || l.speler.hasDisconnected() || GuhrioSpel.sessie(l.speler) != l.sessie || l.sessie.klaar()) {
                it.remove();                                    // (out of the level, a new run, or at the flagpole)
            } else if (--l.ticks <= 0) {
                it.remove();
                if (l.speler.position().distanceToSqr(l.waar) <= TIP_BIJ * TIP_BIJ) {
                    l.speler.sendOverlayMessage(l.tekst);       // (still there, reading: three more seconds)
                }
            }
        }
        if (WACHT.isEmpty()) {
            return;
        }
        for (Iterator<Wacht> it = WACHT.iterator(); it.hasNext();) {
            Wacht w = it.next();
            if (w.speler.isRemoved() || w.speler.hasDisconnected()) {
                it.remove();
            } else if (--w.ticks <= 0) {
                it.remove();
                bedank(w.speler);
            }
        }
    }

    /** Is Pad-guh about to thank this player (for tests)? */
    static boolean wacht(ServerPlayer player) {
        return WACHT.stream().anyMatch(w -> w.speler == player);
    }

    /**
     * Pad-guh's thanks at the end of the world: the line everybody knows, every time; the first time also where to go next,
     * later one of his lines for somebody who comes back. Counts, and the first time grants "quest/guhrio_w1_prinses" and
     * the visible "guhrio/guhrio_w1_binnentuin".
     */
    static void bedank(ServerPlayer player) {
        CompoundTag spaar = spaar(player);
        int keer = spaar.getIntOr("Bedankt", 0);
        spaar.putInt("Bedankt", keer + 1);
        for (Component regel : woorden(player, keer)) {
            player.sendSystemMessage(regel);
        }
        player.sendOverlayMessage(Component.translatable("quest.guhs.guhriow1.padguh.bedankt").withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.5f);
        if (keer == 0) {
            GuhAdvancements.grant(player, "guhrio_w1_prinses");
            GidsFeature.grant(player, "guhrio/guhrio_w1_binnentuin");
        }
    }

    /**
     * What Pad-guh says in the chat to somebody he has thanked {@code keer} times before: the line everybody knows, then
     * where to go next (the first time) or one of his lines for somebody who comes back. The first time the famous line
     * is left out when the Pad-guh of the forecourt has just called it after this player at this very flagpole
     * ({@link #BELONING_GAG}): nobody should read the same sentence twice (it still stands above the panel).
     */
    static List<Component> woorden(Player player, int keer) {
        List<Component> uit = new ArrayList<>();
        if (keer > 0 || !GuhQuests.saved(player).getBooleanOr(BELONING_GAG, false)) {
            uit.add(zegt("quest.guhs.guhriow1.padguh.bedankt"));
        }
        uit.add(zegt(keer == 0 ? "quest.guhs.guhriow1.padguh.verder" : "quest.guhs.guhriow1.padguh.alweer." + Math.floorMod(keer - 1, ALWEER)));
        return uit;
    }

    /** "&lt;Pad-guh&gt; text", like GuhQuests.say (which wants the NPC itself: he may be out of sight). */
    private static MutableComponent zegt(String key) {
        return Component.literal("<").append(Component.translatable("entity.guhs.guh_npc.padguh")).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }

    /** Pad-guh at the end of 1-2, for whoever gets to click him (nobody in a level can: an op walking through the hall). */
    private static final class PadguhRol implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            GuhQuests.say(player, npc, "quest.guhs.guhriow1.padguh.praat");
        }
    }

    // =====================================================================================================================
    // op commands (the AutoCheck script tools/autocheck/bbq2_guhrio-w1.txt; literal texts: for developers only)
    // =====================================================================================================================

    /**
     * /guhs guhriow1 bedank   Pad-guh's thanks, now
     * /guhs guhriow1 info     what this world remembers of you
     * /guhs guhriow1 wis      forget it (the secrets, the thanks; the engine's own data stays: /guhs guhrio wis)
     */
    private static void commandos(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("guhriow1").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bedank").executes(c -> {
                    bedank(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StringBuilder uit = new StringBuilder("Guhrio wereld 1:");
                    for (String id : LEVELS) {
                        uit.append(' ').append(levelNaam(id)).append(GuhrioKasteel.gehaald(p, id) ? " gehaald" : " open").append(", geheim ")
                                .append(geheimGevonden(p, id) ? "ja" : "nee").append(", vadsmunten ").append(Integer.bitCount(GuhrioKasteel.vadsmunten(p, id)))
                                .append("/3;");
                    }
                    uit.append(" bedankt ").append(bedankt(p)).append("x");
                    c.getSource().sendSuccess(() -> Component.literal(uit.toString()), false);
                    return 1;
                }))
                .then(Commands.literal("wis").executes(c -> {
                    GuhQuests.saved(c.getSource().getPlayerOrException()).remove(SLEUTEL);
                    c.getSource().sendSuccess(() -> Component.literal("Guhrio wereld 1: vergeten"), false);
                    return 1;
                }))));
    }
}
