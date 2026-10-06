package nl.juiced.guhs.feature.guhrio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The Kasteel van de Grote Nether-Mika as a whole: what a player keeps of Super Guhrio beyond one level, all per player
 * (in {@link GuhrioSpel#spaar}):
 * <ul>
 *     <li>the six levels ({@link #LEVELS}) and the duel ({@link #DUEL}): which you finished ({@link #gehaald}), and the
 *     questline {@link #LIJN} ("guhrio": step 0 walk in, 1-6 the levels, 7 the duel) with its Guhdex page;</li>
 *     <li>the pocket of level coins ({@link GuhrioSpel#munten}; a coin only counts the first time, {@link #pak}; the shop
 *     takes them with {@link #betaal}) and the big vadsmunten (three per level, {@link #vadsmunten});</li>
 *     <li>the records: every level and the whole castle in one go (the six levels in order, starting at 1-1; time lost in
 *     a level you walked out of counts) go to the Scorebord boards {@link #bord} / {@link #BORD_KASTEEL}, so the Guhdex
 *     highscore page shows your own best and the server record;</li>
 *     <li>Guhshi's egg ({@link #heeftEi}) and the duel ({@link #winDuel}, {@link #duelGewonnen}).</li>
 * </ul>
 * The level slices and the reward slice only call what is public here.
 */
public final class GuhrioKasteel {
    public static final String STRUCTUUR = "guhrio_kasteel";
    /** The levels in order; a level's file names the one before it as {@code na}. */
    public static final List<String> LEVELS = List.of("kasteel_1_1", "kasteel_1_2", "kasteel_2_1", "kasteel_2_2", "kasteel_3_1", "kasteel_3_2");
    public static final String DUEL = "kasteel_duel";
    /** The Scorebord / Highscores board of the whole castle in one go. */
    public static final String BORD_KASTEEL = "guhrio_kasteel";
    /** How many big vadsmunten there are in all. */
    public static final int VADSMUNTEN = LEVELS.size() * 3;

    /** The questline (Guhdex tab Verhalen, group "guhrio"): 8 steps. */
    public static final Verhaallijn LIJN = Verhaallijn.maak("guhrio", "guhrio").stappen(LEVELS.size() + 2).icoon("guhs:guhrio_vadsmunt")
            .nodig(GuhrioKasteel::nodig).beloningen(GuhrioKasteel::beloningen)
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, STRUCTUUR, Component.translatable("structure.guhs." + STRUCTUUR)))
            .registreer();

    /** Somebody did the whole castle in one go. */
    public interface KasteelLuisteraar {
        void klaar(ServerPlayer player, int ticks, boolean record);
    }

    public static final List<KasteelLuisteraar> BIJ_KASTEEL = new CopyOnWriteArrayList<>();
    /** Somebody won the duel (every time; {@link #duelGewonnen(Player)} says whether it was the first). */
    public static final List<Consumer<ServerPlayer>> BIJ_DUEL = new CopyOnWriteArrayList<>();

    private GuhrioKasteel() {
    }

    /** (GuhrioFeature.register) */
    static void registreer() {
        nl.juiced.guhs.feature.wereld.Bescherming.registreer(STRUCTUUR, 4);
        nl.juiced.guhs.item.SuperkompasItem.voegToe("barbecue", STRUCTUUR);
        nl.juiced.guhs.feature.Minigames.registerGame(nl.juiced.guhs.feature.Minigames.GUHRIO, p -> GuhrioSpel.sessie(p) != null);
    }

    /** The Scorebord board of a level ("guhrio_1_1"). */
    public static String bord(String levelId) {
        return "guhrio_" + (levelId.startsWith("kasteel_") ? levelId.substring("kasteel_".length()) : levelId);
    }

    // =====================================================================================================================
    // what stays
    // =====================================================================================================================

    private static CompoundTag deel(Player player, String naam) {
        return GuhrioSpel.spaar(player).getCompoundOrEmpty(naam);
    }

    private static void zetDeel(Player player, String naam, CompoundTag deel) {
        GuhrioSpel.spaar(player).put(naam, deel);
    }

    /** Has this player ever reached the flagpole of this level (the duel: won it)? */
    public static boolean gehaald(Player player, String levelId) {
        return deel(player, "Gehaald").getBooleanOr(levelId, false);
    }

    /** How many of the six levels this player finished. */
    public static int aantalGehaald(Player player) {
        int n = 0;
        for (String id : LEVELS) {
            n += gehaald(player, id) ? 1 : 0;
        }
        return n;
    }

    /** May this player start this level (its {@code na} level is done, or a warp room opened it)? */
    public static boolean magIn(Player player, GuhrioLevel level) {
        return level.na() == null || gehaald(player, level.na()) || deel(player, "Open").getBooleanOr(level.id(), false);
    }

    /** A warp room: this level is open for the player, whatever they finished. */
    public static void ontgrendel(ServerPlayer player, String levelId) {
        CompoundTag open = deel(player, "Open");
        open.putBoolean(levelId, true);
        zetDeel(player, "Open", open);
    }

    /** The big vadsmunten of a level this player has: bit 0, 1, 2. */
    public static int vadsmunten(Player player, String levelId) {
        return deel(player, "Vads").getIntOr(levelId, 0) & 7;
    }

    /** All the big vadsmunten this player has (0..18). */
    public static int alleVadsmunten(Player player) {
        int n = 0;
        for (String id : LEVELS) {
            n += Integer.bitCount(vadsmunten(player, id));
        }
        return n;
    }

    static void vadsmunt(ServerPlayer player, String levelId, int nummer) {
        CompoundTag vads = deel(player, "Vads");
        int was = vads.getIntOr(levelId, 0);
        if ((was >> nummer & 1) != 0) {
            return;
        }
        vads.putInt(levelId, was | 1 << nummer);
        zetDeel(player, "Vads", vads);
        if (alleVadsmunten(player) >= VADSMUNTEN) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhrio_vadsmunten");
            GidsFeature.grant(player, "guhrio/guhrio_vadsmunten");
            player.sendSystemMessage(Component.translatable("gui.guhs.guhrio.vadsmunten_alle").withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * The coin of piece {@code nummer} of this level: true the first time this player ever takes it (it goes into the
     * pocket), false every time after.
     */
    static boolean pak(ServerPlayer player, String levelId, int nummer) {
        CompoundTag gepakt = deel(player, "Gepakt");
        long[] bits = gepakt.getLongArray(levelId).orElse(new long[0]);
        int woord = nummer >> 6;
        if (woord < bits.length && (bits[woord] >> (nummer & 63) & 1) != 0) {
            return false;
        }
        if (woord >= bits.length) {
            bits = java.util.Arrays.copyOf(bits, woord + 1);
        }
        bits[woord] |= 1L << (nummer & 63);
        gepakt.putLongArray(levelId, bits);
        zetDeel(player, "Gepakt", gepakt);
        CompoundTag spaar = GuhrioSpel.spaar(player);
        spaar.putInt("Munten", spaar.getIntOr("Munten", 0) + 1);
        spaar.putInt("MuntenOoit", spaar.getIntOr("MuntenOoit", 0) + 1);
        return true;
    }

    /** All the level coins this player ever found (spent or not). */
    public static int muntenOoit(Player player) {
        return GuhrioSpel.spaar(player).getIntOr("MuntenOoit", 0);
    }

    /** Pays {@code aantal} level coins out of the pocket. False (and nothing happens) when there are not enough. */
    public static boolean betaal(ServerPlayer player, int aantal) {
        CompoundTag spaar = GuhrioSpel.spaar(player);
        int heeft = spaar.getIntOr("Munten", 0);
        if (aantal < 0 || heeft < aantal) {
            return false;
        }
        spaar.putInt("Munten", heeft - aantal);
        return true;
    }

    /** Has this player found Guhshi's egg (world 2)? */
    public static boolean heeftEi(Player player) {
        return GuhrioSpel.spaar(player).getBooleanOr("Ei", false);
    }

    /** Guhshi's egg is found. True the first time. */
    public static boolean geefEi(ServerPlayer player) {
        if (heeftEi(player)) {
            return false;
        }
        GuhrioSpel.spaar(player).putBoolean("Ei", true);
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhrio_ei");
        return true;
    }

    /** Has this player won the duel with the Grote Nether-Mika? */
    public static boolean duelGewonnen(Player player) {
        return gehaald(player, DUEL);
    }

    /**
     * The duel is won (the duel slice calls this). The questline is done, the hidden advancement "guhrio_duel" is granted,
     * and {@link #BIJ_DUEL} hears of it (the reward slice: Guhshi is yours). True the first time.
     */
    public static boolean winDuel(ServerPlayer player) {
        boolean eerste = !gehaald(player, DUEL);
        markeer(player, DUEL);
        LIJN.zet(player, LIJN.stappen());
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhrio_duel");
        GidsFeature.grant(player, "guhrio/guhrio_duel");
        for (Consumer<ServerPlayer> l : BIJ_DUEL) {
            l.accept(player);
        }
        return eerste;
    }

    /** (dev command) this level counts as finished: for trying the later gates without playing everything. */
    static void devGehaald(ServerPlayer player, String levelId) {
        markeer(player, levelId);
        LIJN.begin(player);
        LIJN.zet(player, LEVELS.indexOf(levelId) + 2);
    }

    private static void markeer(Player player, String levelId) {
        CompoundTag gehaald = deel(player, "Gehaald");
        gehaald.putBoolean(levelId, true);
        zetDeel(player, "Gehaald", gehaald);
    }

    /** This player's best time (ticks) for the whole castle in one go, or 0. */
    public static int kasteelTijd(Player player) {
        return GuhrioSpel.spaar(player).getIntOr("KasteelTijd", 0);
    }

    // =====================================================================================================================
    // called by GuhrioSpel
    // =====================================================================================================================

    /** A player enters a level. */
    static void begin(ServerPlayer player, GuhrioSpel.Sessie s) {
        String id = s.level().level().id();
        int nr = LEVELS.indexOf(id);
        if (nr < 0) {
            return;
        }
        LIJN.begin(player);
        if (LIJN.stap(player) == 0) {
            LIJN.zet(player, 1);
        }
        GidsFeature.grant(player, "guhrio/root");
        CompoundTag loop = deel(player, "Loop");
        if (nr == 0) {
            loop = new CompoundTag();                           // (1-1 again: a new go at the whole castle)
            loop.putInt("Volgende", 0);
            loop.putInt("Ticks", 0);
            zetDeel(player, "Loop", loop);
            s.loop = true;
        } else {
            s.loop = loop.contains("Volgende") && loop.getIntOr("Volgende", -1) == nr;
        }
    }

    /** A player reached the flagpole. */
    static void klaar(ServerPlayer player, GuhrioSpel.Sessie s, boolean record) {
        String id = s.level().level().id();
        int nr = LEVELS.indexOf(id);
        if (DUEL.equals(id)) {
            winDuel(player);                                 // (a duel level that simply ends at a flagpole)
            return;
        }
        if (nr < 0) {
            return;
        }
        markeer(player, id);
        Scorebord.submit(player, bord(id), s.ticks, true);
        LIJN.zet(player, nr + 2);
        if (aantalGehaald(player) >= LEVELS.size()) {
            GidsFeature.grant(player, "guhrio/guhrio_levels");
        }
        if (!s.loop) {
            return;
        }
        s.loop = false;
        CompoundTag loop = deel(player, "Loop");
        int totaal = loop.getIntOr("Ticks", 0) + s.ticks;
        if (nr < LEVELS.size() - 1) {
            loop.putInt("Volgende", nr + 1);
            loop.putInt("Ticks", totaal);
            zetDeel(player, "Loop", loop);
            return;
        }
        GuhrioSpel.spaar(player).remove("Loop");
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhrio_kasteel_loop");
        int beste = kasteelTijd(player);
        boolean snelst = beste == 0 || totaal < beste;
        if (snelst) {
            GuhrioSpel.spaar(player).putInt("KasteelTijd", totaal);
        }
        Scorebord.submit(player, BORD_KASTEEL, totaal, true);
        player.sendSystemMessage(Component.translatable(snelst ? "gui.guhs.guhrio.kasteel.record" : "gui.guhs.guhrio.kasteel", GuhrioSpel.tijd(totaal))
                .withStyle(ChatFormatting.GOLD));
        for (KasteelLuisteraar l : BIJ_KASTEEL) {
            l.klaar(player, totaal, snelst);
        }
    }

    /** A level ended for a player, any way at all. */
    static void einde(ServerPlayer player, GuhrioSpel.Sessie s, GuhrioSpel.Einde reden) {
        if (reden == GuhrioSpel.Einde.KLAAR || !s.loop) {
            return;
        }
        // walked out of a level of a whole-castle go: the time is not given back
        CompoundTag loop = deel(player, "Loop");
        if (loop.contains("Volgende")) {
            loop.putInt("Ticks", loop.getIntOr("Ticks", 0) + s.ticks);
            zetDeel(player, "Loop", loop);
        }
    }

    // =====================================================================================================================
    // the Guhdex page
    // =====================================================================================================================

    /** While you are at a level's step: its three big vadsmunten, each ticked when you have it. */
    private static List<VerhaalStand.Nodig> nodig(ServerPlayer player, int stap) {
        if (stap < 1 || stap > LEVELS.size()) {
            return List.of();
        }
        int heeft = vadsmunten(player, LEVELS.get(stap - 1));
        List<VerhaalStand.Nodig> uit = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            uit.add(Verhaallijn.nodig("guhs:guhrio_vadsmunt", "gui.guhs.guhrio.vadsmunt." + i, heeft >> i & 1, 1));
        }
        return uit;
    }

    /** Per level: all three big vadsmunten; and the whole castle in one go. */
    private static List<VerhaalStand.Beloning> beloningen(ServerPlayer player) {
        List<VerhaalStand.Beloning> uit = new ArrayList<>();
        for (int i = 0; i < LEVELS.size(); i++) {
            uit.add(Verhaallijn.beloning("guhs:guhrio_vadsmunt", "gui.guhs.guhrio.vadsmunten." + (i / 2 + 1) + "_" + (i % 2 + 1),
                    vadsmunten(player, LEVELS.get(i)) == 7));
        }
        uit.add(Verhaallijn.beloning("minecraft:clock", "gui.guhs.guhrio.beloning.kasteel", kasteelTijd(player) > 0));
        return uit;
    }
}
