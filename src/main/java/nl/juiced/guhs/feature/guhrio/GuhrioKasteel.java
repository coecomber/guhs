package nl.juiced.guhs.feature.guhrio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Kopieen;
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
 * And one thing of the building itself: its level halls are sealed boxes, so whoever is in one without playing its level is
 * put at that level's gate ({@link #bewaak}).
 * The level slices and the reward slice only call what is public here.
 */
public final class GuhrioKasteel {
    public static final String STRUCTUUR = "guhrio_kasteel";
    /** The levels in order; a level's file names the one before it as {@code na}. */
    public static final List<String> LEVELS = List.of("kasteel_1_1", "kasteel_1_2", "kasteel_2_1", "kasteel_2_2", "kasteel_3_1", "kasteel_3_2");
    public static final String DUEL = "kasteel_duel";
    /** The Scorebord / Highscores board of the whole castle in one go. */
    public static final String BORD_KASTEEL = "guhrio_kasteel";
    /** The castle's group in the Minigames tab of the Guhdex (also the clothes source of the reward slice). */
    public static final String GROEP = "guhrio_beloning";
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

    // =====================================================================================================================
    // the sealed level halls
    // =====================================================================================================================

    /**
     * A sealed level hall of the castle (the lane, its gallery and the trench under it), in the coordinates of the whole
     * build: the level that is played in it, where that level's start block is and which way it faces. Written by
     * tools/features/guhrio_kasteel.py ({@code hallen}) to data/guhs/guhrio_kasteel/hallen.json.
     */
    public record Hal(String level, BlockPos start, Direction kijkt, BoundingBox doos) {
    }

    /** How often a player without a level is looked for in a hall, and how long he must have been there (ticks). */
    public static final int BEWAAK_ELKE = 40;
    @Nullable
    private static volatile List<Hal> hallen;
    /** Who was seen in a hall without playing its level, and when (game time): put back the second time. */
    private static final java.util.Map<java.util.UUID, Long> VERDWAALD = new java.util.concurrent.ConcurrentHashMap<>();

    /** The halls of the castle (read once; again after the data packs were read again). */
    public static List<Hal> hallen(MinecraftServer server) {
        List<Hal> uit = hallen;
        if (uit != null) {
            return uit;
        }
        List<Hal> gelezen = new ArrayList<>();
        var res = server.getResourceManager().getResource(nl.juiced.guhs.Guhs.id(STRUCTUUR + "/hallen.json"));
        if (res.isPresent()) {
            try (java.io.Reader reader = res.get().openAsReader()) {
                for (com.google.gson.JsonElement e : com.google.gson.JsonParser.parseReader(reader).getAsJsonArray()) {
                    com.google.gson.JsonObject o = e.getAsJsonObject();
                    com.google.gson.JsonArray s = o.getAsJsonArray("start"), d = o.getAsJsonArray("doos");
                    Direction kijkt = Direction.byName(o.get("kijkt").getAsString());
                    gelezen.add(new Hal(o.get("level").getAsString(), new BlockPos(s.get(0).getAsInt(), s.get(1).getAsInt(), s.get(2).getAsInt()),
                            kijkt == null ? Direction.SOUTH : kijkt, new BoundingBox(d.get(0).getAsInt(), d.get(1).getAsInt(), d.get(2).getAsInt(),
                            d.get(3).getAsInt(), d.get(4).getAsInt(), d.get(5).getAsInt())));
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("guhs").error("Guhrio: the halls of the castle can't be read", e);
            }
        }
        hallen = List.copyOf(gelezen);
        return hallen;
    }

    /** (the data packs were read again, the server stopped) */
    static void vergeetHallen() {
        hallen = null;
        VERDWAALD.clear();
    }

    /** Where a level hall lets somebody out who does not play its level: that level's gate in the hall of the keep. */
    public record Uitweg(Hal hal, BlockPos poort) {
    }

    /**
     * The level hall of a castle this spot lies in, with the way out of it; null: no hall here (or the castle around it
     * is not a copy the world knows). Looks at the loaded chunks only.
     */
    @Nullable
    public static Uitweg halBij(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.levelgen.structure.Structure structuur = Kopieen.structuur(level, STRUCTUUR);
        if (structuur == null) {
            return null;
        }
        for (net.minecraft.world.level.levelgen.structure.StructureStart kopie : Kopieen.bij(level, structuur, pos, 0)) {
            BlockPos lokaal = Kopieen.lokaal(kopie, null, pos);
            if (lokaal == null) {
                continue;
            }
            for (Hal hal : hallen(level.getServer())) {
                if (!hal.doos().isInside(lokaal)) {
                    continue;
                }
                BlockPos start = Kopieen.wereld(kopie, null, hal.start());
                GuhrioLevel def = GuhrioLevel.vind(level.getServer(), hal.level());
                if (start == null || def == null) {
                    return null;
                }
                // (the start block itself says which way the level runs in this copy; else: the copy's turn)
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(start);
                Direction kijkt = state.getBlock() instanceof GuhrioBlocks.StartBlok ? state.getValue(GuhrioBlocks.StartBlok.FACING)
                        : Kopieen.draai(kopie, null).rotate(hal.kijkt());
                BlockPos poort = def.plaats(level.dimension(), start, kijkt).ingang();
                return poort == null ? null : new Uitweg(hal, poort);
            }
        }
        return null;
    }

    /**
     * (every {@link #BEWAAK_ELKE} ticks, for a player who is in no level) Somebody who stands in a sealed level hall
     * without playing its level cannot get out: a hall has no door (the server stopped between the end of a level and the
     * step outside, an operator's teleport, a pearl thrown through a window). Seen there twice in a row, a survival or
     * adventure player is put at that level's gate. True when the player was put back now.
     */
    static boolean bewaak(ServerPlayer player) {
        java.util.UUID id = player.getUUID();
        if (GuhrioSpel.sessie(player) != null || player.isCreative() || player.isSpectator() || !player.isAlive()
                || nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(player)) {
            VERDWAALD.remove(id);
            return false;
        }
        ServerLevel level = player.level();
        Uitweg uit = halBij(level, player.blockPosition());
        if (uit == null) {
            VERDWAALD.remove(id);
            return false;
        }
        long nu = level.getGameTime();
        Long sinds = VERDWAALD.putIfAbsent(id, nu);
        if (sinds == null || nu - sinds < BEWAAK_ELKE) {
            return false;                                     // (a level that ended this very tick lets go of you by itself)
        }
        VERDWAALD.remove(id);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.resetFallDistance();
        player.teleportTo(uit.poort().getX() + 0.5, uit.poort().getY(), uit.poort().getZ() + 0.5);
        player.sendOverlayMessage(Component.translatable("gui.guhs.guhrio.verdwaald").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** (GuhrioFeature.register) */
    static void registreer() {
        nl.juiced.guhs.feature.wereld.Bescherming.registreer(STRUCTUUR, 4);
        nl.juiced.guhs.item.SuperkompasItem.voegToe("barbecue", STRUCTUUR);
        nl.juiced.guhs.feature.Minigames.registerGame(nl.juiced.guhs.feature.Minigames.GUHRIO, p -> GuhrioSpel.sessie(p) != null);
        // the castle's group in the Minigames tab of the Guhdex: its Highscores rows, and (the group's id is their source) the
        // clothes of the reward slice. Without a structure: the castle is in the Superkompas tab "barbecue", not "minigames",
        // and is no building of the explorer advancements.
        List<String> rijen = new ArrayList<>(List.of(BORD_KASTEEL));
        LEVELS.forEach(id -> rijen.add(bord(id)));
        nl.juiced.guhs.feature.spelen.SpelGroepen.groep(new nl.juiced.guhs.feature.spelen.SpelGroepen.Groep(GROEP,
                nl.juiced.guhs.feature.spelen.SpelGroepen.Tijdperk.VERHALEN,
                nl.juiced.guhs.feature.spelen.SpelGroepen.icoon("guhrio_vadsmunt", net.minecraft.world.item.Items.GOLD_NUGGET), null,
                nl.juiced.guhs.entity.GuhNpcEntity.Kind.PADGUH, rijen));
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

    /**
     * Puts {@code aantal} level coins into the pocket (Pad-guh's tip at a flagpole; nothing below zero). They do not count
     * as "ever found": that number is the coins of the levels themselves ({@link #pak}).
     */
    public static void geef(ServerPlayer player, int aantal) {
        if (aantal > 0) {
            CompoundTag spaar = GuhrioSpel.spaar(player);
            spaar.putInt("Munten", spaar.getIntOr("Munten", 0) + aantal);
        }
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
