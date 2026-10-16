package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 fix round 1 (fix-kern, playtest 1: "ik wil ietsjes meer hints"): a hint per story step. A step declares WHERE its
 * next part starts (a marker of a structure, or any spot), and optionally a text, a short "look here" scene and a marker
 * in the world; the hint shows by itself when the step starts, after "Verder met het verhaal" ({@link E2Hervat}) and on
 * {@link #toon}. Everything is per player.
 * <pre>
 * // common code (both sides: the look scene must be known on the client), e.g. from your Feature.register:
 * static final Cutscene KIJK_ZWEVEN = StapHint.kijkScene("einde2_h1_lucht_kijk_zweven", true);   // lang: scene.guhs.&lt;id&gt;.titel + .tekst
 * StapHint.naar(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "zweef_start")      // the target (also where "Verder" puts you)
 *         .tekst("gui.guhs.einde2_h1_lucht.hint.zweven")               // the chat line; %s = the direction ("in het oosten, ...")
 *         .kijk(KIJK_ZWEVEN)                                          // the ~3 s look scene (optional)
 *         .bijStart()                                                 // show it when the step starts (else only on toon / Verder)
 *         .baken()                                                    // a soft column of light at the target while the step is on
 *         .zet(E2Verhaal.LUCHT, E2H1Lucht.ZWEVEN);
 * StapHint.toon(player, E2Verhaal.LUCHT);                              // show the hint of the player's current step now
 * Component r = Richting.van(player.position(), Vec3.atCenterOf(pos)); // the direction phrase alone
 * </pre>
 * <ul>
 *   <li><b>The look scene</b> ({@link #kijkScene}): {@value #KIJK_DUUR} ticks; the camera glides in from the player's side
 *       (one of four sides) towards the target and holds there, then goes back to the player. At most one run-on caption
 *       ({@code Builder.lied}: it never waits for a click); no spoken lines, so the click-per-line rule is untouched. It is
 *       always skippable (hold the button): it counts as seen before it plays. It only plays when the target is within
 *       {@value #KIJK_BEREIK} blocks in the same dimension (further away the client has no chunks there); else only the text.
 *       Without a scene of its own a hint uses the caption-less {@link #KIJK}.</li>
 *   <li><b>The text</b>: one chat line (your key, with the direction as {@code %s}; without a key the step's own
 *       {@code gui.guhs.verhalen.<lijn>.nu.<sleutel>} text and the direction) and the direction in the action bar.</li>
 *   <li><b>The marker</b> ({@link Hint#baken()}, asked with {@link Hint#bakenAan()}): every half second, for this player only, a thin column of END_ROD
 *       particles at the target while the step is on and the player is within {@value #BAKEN_BEREIK} blocks.</li>
 * </ul>
 * Texts of this class: {@code gui.guhs.einde2_verhaal.hint.*} (tools/features/einde2_verhaal.py, English in c91).
 */
public final class StapHint {
    public static final String P = "gui.guhs.einde2_verhaal.hint.";
    /** The length of a look scene (ticks). */
    public static final int KIJK_DUUR = 64;
    /** A look scene only plays within this many blocks of the target; the marker shows within that many. */
    public static final double KIJK_BEREIK = 96, BAKEN_BEREIK = 160;
    /** The default wait (ticks) between a step starting and its hint (a scene that ends the step before is over by then). */
    public static final int START_VERTRAGING = 30;
    /** A hint that waits (the player is watching something) gives up after this many ticks. */
    public static final int GEDULD = 20 * 90;
    /** merge_fix2: a hint asked for while a scene runs waits this long at most (a story scene with clicks can be long). */
    public static final int GEDULD_SCENE = 20 * 60 * 5;
    /** merge_fix2: the rest (ticks) between two hints that waited, so two look scenes never run back to back. */
    public static final int RUST = 20;

    // =====================================================================================================================
    // targets
    // =====================================================================================================================

    /** Where a hint points: a dimension and a block, worked out for this player when it is needed (null: not known yet). */
    public record Doel(ResourceKey<Level> dim, BiFunction<ServerPlayer, ServerLevel, BlockPos> waar) {
        /** The block in its level (null: the level does not exist, or the spot is not known yet). */
        @Nullable
        public BlockPos blok(ServerPlayer p) {
            MinecraftServer server = p.level().getServer();
            ServerLevel level = server == null ? null : server.getLevel(dim);
            return level == null ? null : waar.apply(p, level);
        }
    }

    /** A marker of a structure in the Guheinde (its guaranteed copy, {@code Plekken.wereld}). */
    public static Doel marker(String structuur, String plek) {
        return new Doel(GuheindeFeature.GUHEINDE, (p, level) -> Plekken.bekend(level, structuur) ? Plekken.wereld(level, structuur, plek) : null);
    }

    /** Any spot (the Geestenwereld, a spot worked out by code). */
    public static Doel plek(ResourceKey<Level> dim, BiFunction<ServerPlayer, ServerLevel, BlockPos> waar) {
        return new Doel(dim, waar);
    }

    // =====================================================================================================================
    // the hints
    // =====================================================================================================================

    /** A declared hint (build it with {@link #naar} and its methods, then {@link #zet}). */
    public static final class Hint {
        private final Doel doel;
        @Nullable
        private String tekst;
        @Nullable
        private Cutscene kijk;
        private boolean bijStart, baken, metKijk = true;
        private int vertraging = START_VERTRAGING;
        @Nullable
        private Doel terug;

        private Hint(Doel doel) {
            this.doel = doel;
        }

        /** The chat line (a lang key; {@code %s} = the direction phrase). Without one: the step's own "nu" text + direction. */
        public Hint tekst(String key) {
            this.tekst = key;
            return this;
        }

        /** The look scene of this hint (from {@link #kijkScene}; one with its own caption). Default: {@link #KIJK}. */
        public Hint kijk(Cutscene scene) {
            this.kijk = scene;
            return this;
        }

        /** No look scene at all (only the text and the marker). */
        public Hint zonderKijk() {
            this.metKijk = false;
            return this;
        }

        /** Shows the hint by itself when the step starts ({@value #START_VERTRAGING} ticks later, after any scene). */
        public Hint bijStart() {
            return bijStart(START_VERTRAGING);
        }

        /** The same, this many ticks after the step started (room for talk lines that come first). */
        public Hint bijStart(int ticks) {
            this.bijStart = true;
            this.vertraging = Math.max(1, ticks);
            return this;
        }

        /** A soft column of light at the target while the step is on (for this player only). */
        public Hint baken() {
            this.baken = true;
            return this;
        }

        /** Where "Verder met het verhaal" puts the player for this step, when that is not the target itself. */
        public Hint terug(Doel waar) {
            this.terug = waar;
            return this;
        }

        /** The same, a marker of a structure in the Guheinde. */
        public Hint terug(String structuur, String plek) {
            return terug(marker(structuur, plek));
        }

        /** Declares this hint for these steps of the line (common code; a step has one hint, the last one declared wins). */
        public Hint zet(String lijn, int... stappen) {
            Map<Integer, Hint> m = HINTS.computeIfAbsent(lijn, k -> new ConcurrentHashMap<>());
            for (int s : stappen) {
                m.put(s, this);
            }
            luister(lijn);
            return this;
        }

        public Doel doel() {
            return doel;
        }

        @Nullable
        public String tekst() {
            return tekst;
        }

        /** The scene that plays (null: none). */
        @Nullable
        public Cutscene kijk() {
            return !metKijk ? null : kijk != null ? kijk : KIJK;
        }

        public boolean bijStartAan() {
            return bijStart;
        }

        public int vertraging() {
            return vertraging;
        }

        public boolean bakenAan() {
            return baken;
        }

        /** Where "Verder" puts the player: {@link #terug(Doel)} or else the target. */
        public Doel terugDoel() {
            return terug != null ? terug : doel;
        }
    }

    /** line id -> step -> hint. */
    private static final Map<String, Map<Integer, Hint>> HINTS = new ConcurrentHashMap<>();
    /** The lines whose step listener is on. */
    private static final Set<String> LUISTERT = ConcurrentHashMap.newKeySet();

    /** A new hint that points at a marker of a structure in the Guheinde. */
    public static Hint naar(String structuur, String plek) {
        return new Hint(marker(structuur, plek));
    }

    /** A new hint that points at any spot. */
    public static Hint naar(Doel doel) {
        return new Hint(doel);
    }

    /** The hint of this step of the line (null: none declared). */
    @Nullable
    public static Hint hint(String lijn, int stap) {
        Map<Integer, Hint> m = HINTS.get(lijn);
        return m == null ? null : m.get(stap);
    }

    /** The lines that have hints. */
    public static Set<String> lijnen() {
        return Set.copyOf(HINTS.keySet());
    }

    /** The hint of the player's current step of this line (null: none, or the line is done / not registered). */
    @Nullable
    public static Hint nu(ServerPlayer p, String lijn) {
        Verhaallijn l = Verhaallijnen.van(lijn);
        return l == null || l.klaar(p) ? null : hint(lijn, l.stap(p));
    }

    // =====================================================================================================================
    // the look scenes
    // =====================================================================================================================

    /** The caption-less look scene every hint without one of its own uses. */
    public static final String KIJK_ID = "einde2_verhaal_kijk";
    public static final Cutscene KIJK = kijkScene(KIJK_ID, false);

    /**
     * Registers a look scene (common code, both sides; once per id). {@code metTekst}: one run-on caption, lang
     * {@code scene.guhs.<id>.tekst}; always a title {@code scene.guhs.<id>.titel}. The camera script is relative to the
     * target block: in from 26 blocks out and 14 up, gliding to 10 out and 6 up in two seconds, then a slow drift; the
     * player's side is chosen when it plays ({@link #draaiVoor}). The camera steps out of walls ({@code cameraOntwijkt}).
     */
    public static Cutscene kijkScene(String id, boolean metTekst) {
        Cutscene.Builder b = Cutscene.maak(id).duur(KIJK_DUUR).cameraOntwijkt()
                .camera(0, new Vec3(0.5, 14, 26.5), new Vec3(0.5, 1.5, 0.5))
                .camera(40, new Vec3(0.5, 6, 10.5), new Vec3(0.5, 1.2, 0.5))
                .camera(KIJK_DUUR, new Vec3(0.5, 5.5, 9.0), new Vec3(0.5, 1.2, 0.5));
        if (metTekst) {
            b.lied(8, "tekst", KIJK_DUUR - 8);
        }
        return b.registreer();
    }

    /** The rotation that puts the scene's camera (written on the +z side) on the side of the target where the player is. */
    public static Rotation draaiVoor(Vec3 speler, BlockPos doel) {
        double dx = speler.x - (doel.getX() + 0.5), dz = speler.z - (doel.getZ() + 0.5);
        if (Math.abs(dz) >= Math.abs(dx)) {
            return dz >= 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        }
        return dx < 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
    }

    /** Plays a look scene at this block for the player (skippable at once). False: they watch something, or it is too far. */
    public static boolean speelKijk(ServerPlayer p, Cutscene scene, BlockPos doel, @Nullable Consumer<ServerPlayer> daarna) {
        if (Cutscenes.bezig(p) || !p.isAlive() || p.position().distanceTo(Vec3.atCenterOf(doel)) > KIJK_BEREIK) {
            return false;
        }
        // (counts as seen: the 1.4.4 engine lets a seen scene be skipped by holding the button)
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty("guhs_scene_" + scene.id()).copy();
        t.putBoolean("Gezien", true);
        GuhQuests.saved(p).put("guhs_scene_" + scene.id(), t);
        return Cutscenes.speel(p, scene, doel, draaiVoor(p.position(), doel), daarna);
    }

    // =====================================================================================================================
    // showing a hint
    // =====================================================================================================================

    /** Shows the hint of the player's current step of this line now, with its look scene. False: no hint for that step. */
    public static boolean toon(ServerPlayer p, String lijn) {
        return toon(p, lijn, true);
    }

    /**
     * Shows the hint of the player's current step of this line: the look scene (when {@code kijken}, the hint has one, the
     * target is near enough and the player watches nothing else), then the chat line and the direction in the action bar.
     * A step without a declared hint gets its own "nu" text and the direction to where {@link E2Hervat} would bring them.
     * False: nothing to show (no such line, done, or no place known at all).
     */
    public static boolean toon(ServerPlayer p, String lijn, boolean kijken) {
        Verhaallijn l = Verhaallijnen.van(lijn);
        if (l == null || l.klaar(p)) {
            return false;
        }
        // merge_fix2: one at a time. A scene runs (a story scene, another hint's look): this hint waits for it to end; and a
        // start hint of this line that still waits is now shown here, so it never comes twice.
        if (Cutscenes.bezig(p)) {
            wachtOp(p, new Wacht(lijn, l.stap(p), tijd, tijd + GEDULD_SCENE, kijken));
            return true;
        }
        haalWeg(p.getUUID(), lijn);
        LAATST.put(p.getUUID(), tijd);
        Hint h = hint(lijn, l.stap(p));
        BlockPos doel = h == null ? null : h.doel().blok(p);
        boolean hier = h != null && doel != null && p.level().dimension() == h.doel().dim();
        if (h == null || doel == null) {
            E2Hervat.Plek w = E2Hervat.waar(p, l);
            if (w == null || w.pos() == null) {
                return false;
            }
            boolean zelfde = p.level().dimension() == w.dim();
            regel(p, l, null, zelfde ? Richting.van(p.position(), w.pos()) : null);
            return true;
        }
        Cutscene scene = h.kijk();
        Consumer<ServerPlayer> tekst = s -> regel(s, l, h, s.level().dimension() == h.doel().dim()
                ? Richting.van(s.getEyePosition(), Vec3.atCenterOf(doel)) : null);
        if (kijken && hier && scene != null && speelKijk(p, scene, doel, tekst)) {
            return true;
        }
        tekst.accept(p);
        return true;
    }

    /** The chat line and the action bar of a hint (h null: the step's own text). */
    static void regel(ServerPlayer p, Verhaallijn l, @Nullable Hint h, @Nullable MutableComponent richting) {
        Component waar = richting != null ? richting : Component.translatable(P + "elders");
        MutableComponent zin;
        if (h != null && h.tekst() != null) {
            zin = Component.translatable(h.tekst(), waar);
        } else {
            zin = Component.translatable(P + "standaard", Component.translatable("gui.guhs.verhalen." + l.id() + ".nu." + l.sleutel(p)), waar);
        }
        p.sendSystemMessage(Component.literal("➜ ").withStyle(ChatFormatting.GOLD).append(zin.withStyle(ChatFormatting.YELLOW)));
        if (richting != null) {
            p.sendOverlayMessage(Component.translatable(P + "balk", richting.copy()).withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================================================================
    // the step listener, the waiting hints, the markers, a tiny scheduler
    // =====================================================================================================================

    private record Wacht(String lijn, int stap, long vanaf, long tot, boolean kijken) {
    }

    private record Later(long op, Consumer<ServerPlayer> wat) {
    }

    /**
     * The hints that wait to be shown, per player: at most one per line (the newest step of a line wins), shown one at a
     * time in the order they came, never while a scene runs ({@code merge_fix2}: hints of different chapters / side lines no
     * longer push each other out, and their look scenes never overlap).
     */
    private static final Map<UUID, List<Wacht>> WACHT = new ConcurrentHashMap<>();
    /** When the player last got a hint (server ticks of this class): the queue keeps {@value #RUST} ticks between two. */
    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();
    /** Small delayed jobs per player (run with the player as they are then; dropped when they left). */
    private static final Map<UUID, List<Later>> LATER = new ConcurrentHashMap<>();
    private static volatile long tijd;

    /** Runs this for the player in so many ticks (only when they are still online; the newest player object is passed). */
    public static void later(ServerPlayer p, int ticks, Consumer<ServerPlayer> wat) {
        LATER.computeIfAbsent(p.getUUID(), u -> new CopyOnWriteArrayList<>()).add(new Later(tijd + Math.max(1, ticks), wat));
    }

    /** Puts the step listener on a line (once; a line that is not registered yet gets it when the server has started). */
    private static void luister(String lijn) {
        Verhaallijn l = Verhaallijnen.van(lijn);
        if (l == null || !LUISTERT.add(lijn)) {
            return;
        }
        l.opStap((p, oud, nieuw) -> {
            Hint h = hint(lijn, nieuw);
            if (h != null && h.bijStartAan() && nieuw < l.stappen()) {
                wachtOp(p, new Wacht(lijn, nieuw, tijd + h.vertraging(), tijd + h.vertraging() + GEDULD, true));
            } else {
                haalWeg(p.getUUID(), lijn);                       // (a hint of the step before must not show any more)
            }
        });
    }

    /** From E2Verhaal.register (common code). */
    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> HINTS.keySet().forEach(StapHint::luister));
        NeoForge.EVENT_BUS.addListener(StapHint::opTick);
    }

    private static void opTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tijd++;
        // the delayed jobs
        for (Map.Entry<UUID, List<Later>> e : LATER.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            List<Later> lijst = e.getValue();
            if (p == null) {
                LATER.remove(e.getKey(), lijst);
                continue;
            }
            for (Later j : lijst) {
                if (j.op() <= tijd) {
                    lijst.remove(j);
                    try {
                        j.wat().accept(p);
                    } catch (RuntimeException ex) {
                        org.slf4j.LoggerFactory.getLogger("guhs").warn("einde2 StapHint: a delayed job failed", ex);
                    }
                }
            }
            if (lijst.isEmpty()) {
                LATER.remove(e.getKey(), lijst);
            }
        }
        // the hints that wait (for their step's start, or for a scene to end): at most one per player per tick
        for (Map.Entry<UUID, List<Wacht>> e : WACHT.entrySet()) {
            List<Wacht> lijst = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                WACHT.remove(e.getKey(), lijst);
                continue;
            }
            for (Wacht w : lijst) {
                Verhaallijn l = Verhaallijnen.van(w.lijn());
                if (l == null || l.klaar(p) || l.stap(p) != w.stap() || tijd > w.tot()) {
                    lijst.remove(w);
                }
            }
            if (lijst.isEmpty()) {
                WACHT.remove(e.getKey(), lijst);
                continue;
            }
            Long laatst = LAATST.get(e.getKey());
            if (Cutscenes.bezig(p) || laatst != null && tijd - laatst < RUST
                    || p.isPassenger() && p.getVehicle() != null && p.getVehicle().getDeltaMovement().lengthSqr() > 0.01) {
                continue;
            }
            for (Wacht w : lijst) {
                if (tijd >= w.vanaf()) {
                    lijst.remove(w);
                    toon(p, w.lijn(), w.kijken());
                    break;
                }
            }
        }
        // the markers
        if (tijd % 10 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                bakens(p);
            }
        }
    }

    private static void bakens(ServerPlayer p) {
        if (Cutscenes.bezig(p)) {
            return;
        }
        for (Map.Entry<String, Map<Integer, Hint>> e : HINTS.entrySet()) {
            Verhaallijn l = Verhaallijnen.van(e.getKey());
            if (l == null || l.klaar(p)) {
                continue;
            }
            Hint h = e.getValue().get(l.stap(p));
            if (h == null || !h.bakenAan() || p.level().dimension() != h.doel().dim()) {
                continue;
            }
            BlockPos b = h.doel().waar().apply(p, p.level());
            if (b == null) {
                continue;
            }
            double d = p.position().distanceTo(Vec3.atCenterOf(b));
            if (d > BAKEN_BEREIK || d < 3) {
                continue;
            }
            ServerLevel level = p.level();
            double x = b.getX() + 0.5, y = b.getY() + 0.2, z = b.getZ() + 0.5;
            level.sendParticles(p, ParticleTypes.END_ROD, true, true, x, y + 5.0, z, 5, 0.04, 2.6, 0.04, 0.0);
            level.sendParticles(p, ParticleTypes.END_ROD, true, true, x, y + 0.3, z, 2, 0.3, 0.1, 0.3, 0.01);
        }
    }

    /** Puts a hint in the player's queue (one per line: it replaces a waiting one of the same line). */
    private static void wachtOp(ServerPlayer p, Wacht w) {
        List<Wacht> lijst = WACHT.computeIfAbsent(p.getUUID(), u -> new CopyOnWriteArrayList<>());
        lijst.removeIf(o -> o.lijn().equals(w.lijn()));
        lijst.add(w);
    }

    private static void haalWeg(UUID speler, String lijn) {
        List<Wacht> lijst = WACHT.get(speler);
        if (lijst != null) {
            lijst.removeIf(o -> o.lijn().equals(lijn));
        }
    }

    /** (dev, tests) forgets what waits for this player. */
    public static void vergeet(UUID speler) {
        WACHT.remove(speler);
        LATER.remove(speler);
        LAATST.remove(speler);
    }

    /** (tests) the newest hint that waits for this player: "lijn/stap", or null. */
    @Nullable
    public static String wacht(UUID speler) {
        List<Wacht> lijst = WACHT.get(speler);
        Wacht w = lijst == null || lijst.isEmpty() ? null : lijst.get(lijst.size() - 1);
        return w == null ? null : w.lijn() + "/" + w.stap();
    }

    /** (tests) every hint that waits for this player, oldest first: "lijn/stap". */
    public static List<String> wachtAlle(UUID speler) {
        List<Wacht> lijst = WACHT.get(speler);
        return lijst == null ? List.of() : lijst.stream().map(w -> w.lijn() + "/" + w.stap()).toList();
    }

    /** (tests) every declared hint as "lijn/stap". */
    public static List<String> alle() {
        List<String> uit = new ArrayList<>();
        HINTS.forEach((l, m) -> m.keySet().forEach(s -> uit.add(l + "/" + s)));
        return uit;
    }

    private StapHint() {
    }
}
