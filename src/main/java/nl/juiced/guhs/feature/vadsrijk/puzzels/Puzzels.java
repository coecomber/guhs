package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-PUZZELS: the puzzle toolbox. One generic engine runs every puzzle of the data files ({@link PuzzelDef}); the blocks are
 * {@link PuzzelBlok}s. Kinds:
 * <ul>
 * <li><b>volgorde</b>: pressure plates or singing stones in the right order (a demo stone plays the tune);</li>
 * <li><b>licht</b>: turn mirrors until the beam from the light stone hits the target;</li>
 * <li><b>symbool</b>: turn symbol stones to the code, press the try button;</li>
 * <li><b>patroon</b>: light the star stones of the star map (only at night), press the try button;</li>
 * <li><b>schuif</b>: push blocks onto the target tiles (a reset button sets them back);</li>
 * <li><b>tijdrit</b>: click the start lantern, reach the end plate in time (per player);</li>
 * <li><b>wachtwoord</b>: click the whisper stone, say the answer in chat (per player; the answer is never shown to others);</li>
 * <li><b>geheim</b>: a cracked wall or a hidden lever: click = found (doors/cracks open for a while).</li>
 * </ul>
 * <b>Multiplayer:</b> the puzzle itself is shared (it is real blocks); everyone near it when it is solved gets the reward once
 * (per player, {@link PuzzelSpeler}); a solved puzzle stays solved for {@code reset} ticks, then sets itself back; one that
 * was left half done sets itself back after 2 minutes without anybody near. Failed tries are counted per player: after
 * {@code hintNa} the first tip, after twice as many the second (and from then on the tip stays on the screen line).
 * <b>Placement:</b> {@link PuzzelStempel} (anchor POI + offset, nearest flat spot, remembered in {@link PuzzelData}).
 */
public final class Puzzels {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String MAP = "vadsrijk_puzzels";
    static final int IDLE_RESET = 2400, STAMP_AFSTAND = 72;

    /** Every puzzle definition, by id (sorted). */
    private static final Map<String, PuzzelDef> DEFS = new TreeMap<>();
    /** Live instances: the world ones by puzzle id, test ones by "test:..." ids. */
    private static final Map<String, Inst> INST = new ConcurrentHashMap<>();
    private static final Map<GlobalPos, Ref> INDEX = new ConcurrentHashMap<>();
    /** T-KLEDING etc.: called once when a player unlocks a cosmetic id (from a puzzle reward). */
    public static final List<BiConsumer<ServerPlayer, String>> ONTGRENDEL_HAAK = new CopyOnWriteArrayList<>();
    /** Dev/tests: treat it as night everywhere (star puzzles). */
    public static volatile boolean nachtForceren = false;

    private Puzzels() {
    }

    record Ref(Inst inst, int idx) {
    }

    /** A puzzle placed somewhere: its origin (local 0,0,0 = the west-north corner at standing level) and its live state. */
    public static final class Inst {
        public final PuzzelDef def;
        public final ServerLevel level;
        public final BlockPos origin;
        public final boolean test;
        /** current world position per element (push blocks move; everything else = origin + local). */
        final BlockPos[] pos;
        int voortgang;
        long opgelostTot, laatsteActie;
        boolean schoon = false;
        final Map<UUID, Integer> fouten = new HashMap<>();
        final Map<UUID, Integer> draaien = new HashMap<>();
        final Map<UUID, Long> race = new HashMap<>();
        final Map<UUID, Long> luistert = new HashMap<>();
        int demoStap = -1;
        long demoVolgende, eindUit;

        Inst(PuzzelDef def, ServerLevel level, BlockPos origin, boolean test) {
            this.def = def;
            this.level = level;
            this.origin = origin;
            this.test = test;
            this.pos = new BlockPos[def.elementen().size()];
            for (int i = 0; i < pos.length; i++) {
                pos[i] = origin.offset(def.elementen().get(i).lokaal());
            }
        }

        public BlockPos wereld(BlockPos lokaal) {
            return origin.offset(lokaal);
        }

        public BlockPos element(int i) {
            return pos[i];
        }

        public AABB doos(double marge) {
            return new AABB(origin.getX() - marge, origin.getY() - 2 - marge, origin.getZ() - marge, origin.getX() + def.breed() + marge,
                    origin.getY() + def.hoogte() + marge, origin.getZ() + def.diep() + marge);
        }

        public boolean opgelost() {
            return opgelostTot > 0;
        }

        public int fouten(ServerPlayer p) {
            return fouten.getOrDefault(p.getUUID(), 0);
        }

        public BlockPos midden() {
            return origin.offset(def.breed() / 2, 0, def.diep() / 2);
        }
    }

    // ---- definitions --------------------------------------------------------------------------------------------------------

    /** (Re)reads every data/guhs/vadsrijk_puzzels/*.json. World instances with a saved spot come back (state reset). */
    public static synchronized int laad(MinecraftServer server) {
        Map<String, PuzzelDef> nieuw = new TreeMap<>();
        Map<Identifier, Resource> bestanden = server.getResourceManager().listResources(MAP,
                id -> id.getNamespace().equals(Guhs.MODID) && id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> e : bestanden.entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                PuzzelDef d = PuzzelDef.lees(JsonParser.parseReader(r).getAsJsonObject());
                nieuw.put(d.id(), d);
            } catch (Exception ex) {
                LOG.warn("Puzzel {} niet gelezen: {}", e.getKey(), ex.toString());
            }
        }
        DEFS.clear();
        DEFS.putAll(nieuw);
        for (String id : new ArrayList<>(INST.keySet())) {
            if (!INST.get(id).test) {
                verwijder(id);
            }
        }
        ServerLevel rijk = Vadsrijk.level(server);
        if (rijk != null) {
            PuzzelData data = PuzzelData.get(server);
            for (PuzzelDef d : DEFS.values()) {
                PuzzelData.Plek pl = data.plek(d.id());
                if (pl != null) {
                    activeer(d, rijk, pl.origin(), false);
                }
            }
        }
        return DEFS.size();
    }

    public static Collection<PuzzelDef> defs() {
        return DEFS.values();
    }

    @Nullable
    public static PuzzelDef def(String id) {
        return DEFS.get(id);
    }

    @Nullable
    public static Inst inst(String id) {
        return INST.get(id);
    }

    /** Registers an instance at this origin (blocks must already be there, see {@link PuzzelStempel#stempel}). */
    public static Inst activeer(PuzzelDef d, ServerLevel level, BlockPos origin, boolean test) {
        String sleutel = test ? "test:" + d.id() + "@" + origin.toShortString() : d.id();
        verwijder(sleutel);
        Inst in = new Inst(d, level, origin, test);
        INST.put(sleutel, in);
        for (int i = 0; i < in.pos.length; i++) {
            INDEX.put(GlobalPos.of(level.dimension(), in.pos[i]), new Ref(in, i));
        }
        return in;
    }

    public static void verwijder(String sleutel) {
        Inst oud = INST.remove(sleutel);
        if (oud != null) {
            INDEX.values().removeIf(r -> r.inst == oud);
        }
    }

    /** Removes one instance (tests, the "hier" copies). */
    public static void verwijder(Inst in) {
        INST.values().remove(in);
        INDEX.values().removeIf(r -> r.inst == in);
    }

    @Nullable
    static Ref ref(ServerLevel level, BlockPos pos) {
        return INDEX.get(GlobalPos.of(level.dimension(), pos));
    }

    @Nullable
    public static Inst bij(ServerLevel level, BlockPos pos) {
        Ref r = ref(level, pos);
        return r == null ? null : r.inst;
    }

    // ---- block states --------------------------------------------------------------------------------------------------------

    static BlockState staat(String soort, int stand, boolean aan) {
        PuzzelBlok b = PuzzelRegistry.blok(soort);
        return b == null ? Blocks.AIR.defaultBlockState() : b.defaultBlockState().setValue(PuzzelBlok.STAND, Math.max(0, Math.min(7, stand)))
                .setValue(PuzzelBlok.AAN, aan);
    }

    static int stand(Inst in, int i) {
        BlockState s = in.level.getBlockState(in.pos[i]);
        return s.getBlock() instanceof PuzzelBlok ? s.getValue(PuzzelBlok.STAND) : in.def.elementen().get(i).stand();
    }

    static boolean aan(Inst in, int i) {
        BlockState s = in.level.getBlockState(in.pos[i]);
        return s.getBlock() instanceof PuzzelBlok && s.getValue(PuzzelBlok.AAN);
    }

    static void zet(Inst in, int i, int stand, boolean aan) {
        in.level.setBlock(in.pos[i], staat(in.def.elementen().get(i).blok(), stand, aan), 3);
    }

    // ---- input ------------------------------------------------------------------------------------------------------------------

    /** A right-click on a puzzle block. */
    public static void klik(ServerLevel level, BlockPos pos, ServerPlayer p) {
        Ref r = ref(level, pos);
        if (r == null) {
            return;
        }
        Inst in = r.inst;
        PuzzelDef d = in.def;
        String blok = d.elementen().get(r.idx).blok();
        long nu = level.getGameTime();
        in.laatsteActie = nu;
        if (!d.stil()) {
            Tips.toon(p, "vadsrijk.puzzels.tip.eerste");
        }
        boolean perSpeler = d.soort().equals("tijdrit") || d.soort().equals("wachtwoord");
        if (in.opgelost() && !perSpeler) {
            if (!d.geheim()) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.rust").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        in.schoon = false;
        switch (blok) {
            case "klanksteen" -> {
                speelNoot(in, r.idx);
                if (d.soort().equals("volgorde")) {
                    volgordeStap(in, r.idx, p);
                }
            }
            case "voorspeelsteen" -> {
                if (in.demoStap < 0) {
                    in.demoStap = 0;
                    in.demoVolgende = nu + 10;
                    in.voortgang = 0;
                    alleUit(in);
                    p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.demo").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            case "symboolsteen" -> {
                int s = (stand(in, r.idx) + 1) % 6;
                zet(in, r.idx, s, false);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8f, 0.9f + s * 0.05f);
                p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.symbool",
                        Component.translatable("vadsrijk.puzzels.symbool." + SYMBOLEN[s])).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case "probeerknop" -> {
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1f, 1.2f);
                probeer(in, p);
            }
            case "herstelknop" -> {
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1f, 0.8f);
                herstel(in);
                p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.herstel").withStyle(ChatFormatting.GRAY));
                fout(in, p, false);
            }
            case "spiegel" -> {
                zet(in, r.idx, 1 - (stand(in, r.idx) & 1), false);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.3f);
                if (straal(in).raak()) {
                    opgelost(in, p);
                } else {
                    int n = in.draaien.merge(p.getUUID(), 1, Integer::sum);
                    if (n % 8 == 0) {
                        fout(in, p, false);   // (no "wrong": just counting towards the tips)
                    }
                }
            }
            case "schuifblok" -> duw(in, r.idx, richtingVan(p, pos), p);
            case "startlantaarn" -> startRace(in, r.idx, p);
            case "scheur", "hendel" -> {
                if (blok.equals("hendel")) {
                    zet(in, r.idx, 0, true);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1f, 0.8f);
                } else {
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                }
                opgelost(in, p);
            }
            case "fluistersteen" -> {
                in.luistert.put(p.getUUID(), nu + 1200);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.4f);
                p.sendSystemMessage(Component.translatable(d.key("vraag")).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.luister").withStyle(ChatFormatting.GRAY));
            }
            case "sterrensteen" -> {
                if (!nacht(level)) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.nacht").withStyle(ChatFormatting.BLUE));
                    return;
                }
                boolean a = !aan(in, r.idx);
                zet(in, r.idx, 0, a);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, a ? 1.6f : 0.9f);
            }
            case "sterrenkaart" -> toonKaart(in, p);
            case "drukplaat", "eindplaat" -> stap(level, pos, p);
            default -> {
            }
        }
    }

    /** A player standing on a plate (pressure plate / end plate). */
    public static void stap(ServerLevel level, BlockPos pos, ServerPlayer p) {
        Ref r = ref(level, pos);
        if (r == null) {
            return;
        }
        Inst in = r.inst;
        String blok = in.def.elementen().get(r.idx).blok();
        in.laatsteActie = level.getGameTime();
        if (blok.equals("eindplaat")) {
            eindRace(in, r.idx, p);
            return;
        }
        if (in.opgelost() || aan(in, r.idx) || in.demoStap >= 0) {
            return;
        }
        in.schoon = false;
        Tips.toon(p, "vadsrijk.puzzels.tip.eerste");
        level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1f, 1f);
        speelNoot(in, r.idx);
        volgordeStap(in, r.idx, p);
    }

    /** Chat: an answer for a whisper stone this player clicked in the last minute (within 12 blocks). True = consumed. */
    public static boolean zeg(ServerPlayer p, String tekst) {
        long nu = p.level().getGameTime();
        for (Inst in : INST.values()) {
            Long tot = in.luistert.get(p.getUUID());
            if (tot == null || tot < nu || in.level != p.level() || p.distanceToSqr(in.midden().getCenter()) > 144 + in.def.breed() * in.def.breed()) {
                continue;
            }
            in.luistert.remove(p.getUUID());
            String w = normaal(tekst);
            for (String goed : in.def.woorden()) {
                if (normaal(goed).equals(w)) {
                    opgelost(in, p);
                    return true;
                }
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.woord.fout").withStyle(ChatFormatting.LIGHT_PURPLE));
            in.level.playSound(null, in.midden(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1f, 1.5f);
            fout(in, p, false);
            return true;
        }
        return false;
    }

    static String normaal(String s) {
        return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim().replaceAll("\\s+", " ");
    }

    public static boolean nacht(ServerLevel level) {
        return nachtForceren || level.isDarkOutside();
    }

    // ---- the kinds ----------------------------------------------------------------------------------------------------------

    static final String[] SYMBOLEN = {"ster", "hart", "maan", "zon", "druppel", "blad"};
    private static final int[] TOON = {0, 2, 4, 5, 7, 9, 11, 12};

    static void speelNoot(Inst in, int i) {
        PuzzelDef.Element e = in.def.elementen().get(i);
        int s = e.blok().equals("drukplaat") ? TOON[(e.stand() * 2) % 8] : TOON[Math.max(0, Math.min(7, e.stand()))];
        float pitch = (float) Math.pow(2, (s - 6) / 12.0);
        in.level.playSound(null, in.pos[i], SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.2f, pitch);
        in.level.sendParticles(ParticleTypes.NOTE, in.pos[i].getX() + 0.5, in.pos[i].getY() + 1.2, in.pos[i].getZ() + 0.5, 1, 0, 0, 0, s / 24.0);
    }

    static void volgordeStap(Inst in, int i, ServerPlayer p) {
        List<Integer> opl = in.def.oplossing();
        if (in.voortgang >= opl.size()) {
            in.voortgang = 0;
        }
        if (opl.get(in.voortgang) == i) {
            zet(in, i, stand(in, i), true);
            in.voortgang++;
            if (in.voortgang >= opl.size()) {
                opgelost(in, p);
            }
        } else {
            in.voortgang = 0;
            alleUit(in);
            in.level.playSound(null, in.pos[i], SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.5f);
            p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.fout").withStyle(ChatFormatting.RED));
            fout(in, p, false);
        }
    }

    static void alleUit(Inst in) {
        for (int i = 0; i < in.pos.length; i++) {
            String b = in.def.elementen().get(i).blok();
            if ((b.equals("drukplaat") || b.equals("klanksteen")) && aan(in, i)) {
                zet(in, i, stand(in, i), false);
            }
        }
    }

    static void probeer(Inst in, ServerPlayer p) {
        PuzzelDef d = in.def;
        boolean goed;
        if (d.soort().equals("symbool")) {
            List<Integer> stenen = d.van("symboolsteen");
            goed = stenen.size() == d.oplossing().size();
            for (int k = 0; goed && k < stenen.size(); k++) {
                goed = stand(in, stenen.get(k)) == d.oplossing().get(k);
            }
        } else if (d.soort().equals("patroon")) {
            if (!nacht(in.level)) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.nacht").withStyle(ChatFormatting.BLUE));
                return;
            }
            Set<Integer> aan = new HashSet<>();
            for (int k : d.van("sterrensteen")) {
                if (aan(in, k)) {
                    aan.add(k);
                }
            }
            goed = aan.equals(new HashSet<>(d.oplossing()));
        } else {
            return;
        }
        if (goed) {
            opgelost(in, p);
        } else {
            in.level.playSound(null, in.midden(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1f, 0.5f);
            p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.fout").withStyle(ChatFormatting.RED));
            fout(in, p, false);
        }
    }

    /** The beam: from the light stone, straight until a mirror turns it, a solid block or the target stops it. */
    public record Straal(boolean raak, List<BlockPos> pad) {
    }

    private static final int[][] DXZ = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public static Straal straal(Inst in) {
        PuzzelDef d = in.def;
        List<Integer> bronnen = d.van("lichtbron"), doelen = d.van("lichtdoel");
        List<BlockPos> pad = new ArrayList<>();
        if (bronnen.isEmpty() || doelen.isEmpty()) {
            return new Straal(false, pad);
        }
        BlockPos doel = in.pos[doelen.get(0)];
        BlockPos b = in.pos[bronnen.get(0)];
        int r = stand(in, bronnen.get(0)) & 3;
        Map<BlockPos, Integer> spiegels = new HashMap<>();
        for (int k : d.van("spiegel")) {
            spiegels.put(in.pos[k], k);
        }
        for (int stap = 0; stap < 64; stap++) {
            b = b.offset(DXZ[r][0], 0, DXZ[r][1]);
            if (b.equals(doel)) {
                return new Straal(true, pad);
            }
            Integer sp = spiegels.get(b);
            if (sp != null) {
                r = spiegel(r, stand(in, sp) & 1);
                pad.add(b);
                continue;
            }
            BlockState s = in.level.getBlockState(b);
            if (!s.getCollisionShape(in.level, b).isEmpty()) {
                return new Straal(false, pad);
            }
            pad.add(b);
        }
        return new Straal(false, pad);
    }

    /** Stand 0 = "/", 1 = "\" seen from above (north up). Directions N0 O1 Z2 W3 (same as the generator). */
    static int spiegel(int r, int s) {
        if (s == 0) {
            return switch (r) {
                case 1 -> 0;
                case 0 -> 1;
                case 3 -> 2;
                default -> 3;
            };
        }
        return switch (r) {
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 0;
            default -> 3;
        };
    }

    /** The direction a push goes: away from the player, along the bigger axis (0..3 = N O Z W). */
    static int richtingVan(ServerPlayer p, BlockPos blok) {
        double dx = blok.getX() + 0.5 - p.getX(), dz = blok.getZ() + 0.5 - p.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? 1 : 3;
        }
        return dz > 0 ? 2 : 0;
    }

    /** Pushes push block {@code i} one step in direction r (if that cell is free floor inside the puzzle). */
    public static boolean duw(Inst in, int i, int r, @Nullable ServerPlayer p) {
        BlockPos van = in.pos[i], naar = van.offset(DXZ[r][0], 0, DXZ[r][1]);
        int lx = naar.getX() - in.origin.getX(), lz = naar.getZ() - in.origin.getZ();
        boolean binnen = lx >= 0 && lz >= 0 && lx < in.def.breed() && lz < in.def.diep();
        BlockState doel = in.level.getBlockState(naar);
        boolean vrij = binnen && doel.isAir() && !in.level.getBlockState(naar.below()).getCollisionShape(in.level, naar.below()).isEmpty();
        if (!vrij) {
            if (p != null) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.duw.vast").withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        BlockState s = in.level.getBlockState(van);
        in.level.setBlock(van, Blocks.AIR.defaultBlockState(), 3);
        in.level.setBlock(naar, s, 3);
        INDEX.remove(GlobalPos.of(in.level.dimension(), van));
        in.pos[i] = naar;
        INDEX.put(GlobalPos.of(in.level.dimension(), naar), new Ref(in, i));
        in.level.playSound(null, naar, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, 1.3f);
        in.schoon = false;
        // the target tiles light up under a block
        boolean alles = true;
        Set<BlockPos> blokken = new HashSet<>();
        for (int k : in.def.van("schuifblok")) {
            blokken.add(in.pos[k]);
        }
        for (int k : in.def.van("doelvloer")) {
            boolean bezet = blokken.contains(in.pos[k].above());
            if (aan(in, k) != bezet) {
                zet(in, k, 0, bezet);
            }
            alles &= bezet;
        }
        if (alles && p != null) {
            opgelost(in, p);
        }
        return true;
    }

    static void startRace(Inst in, int i, ServerPlayer p) {
        long nu = in.level.getGameTime();
        in.race.put(p.getUUID(), nu + in.def.tijd() * 20L);
        zet(in, i, 0, true);
        in.level.playSound(null, in.pos[i], SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1f, 1.5f);
        p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.tijd.start", in.def.tijd()).withStyle(ChatFormatting.GOLD));
    }

    static void eindRace(Inst in, int i, ServerPlayer p) {
        Long tot = in.race.remove(p.getUUID());
        if (tot == null) {
            if (!aan(in, i)) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.tijd.niet_bezig").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (tot >= in.level.getGameTime()) {
            zet(in, i, 0, true);
            in.eindUit = in.level.getGameTime() + 60;   // (only the end plate glows a moment; the race is per player)
            opgelost(in, p);
        }
        if (in.race.isEmpty()) {
            for (int k : in.def.van("startlantaarn")) {
                zet(in, k, 0, false);
            }
        }
    }

    static void toonKaart(Inst in, ServerPlayer p) {
        p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.kaart.kop", Component.translatable(in.def.naamKey()))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
        for (String rij : in.def.kaart()) {
            StringBuilder sb = new StringBuilder("   ");
            for (char c : rij.toCharArray()) {
                sb.append(c == 'X' ? "★ " : "· ");
            }
            p.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.YELLOW));
        }
    }

    // ---- outcome --------------------------------------------------------------------------------------------------------------

    /** A failed try (counts towards the tips; per player). */
    static void fout(Inst in, ServerPlayer p, boolean stil) {
        int n = in.fouten.merge(p.getUUID(), 1, Integer::sum);
        int na = in.def.hintNa();
        if (n == na || n == na * 2) {
            String k = in.def.key(n == na ? "hint1" : "hint2");
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.tip", Component.translatable(k)).withStyle(ChatFormatting.AQUA));
            in.level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.8f);
        }
    }

    /** The tip key this player has unlocked (null = none yet). */
    @Nullable
    public static String tipVoor(Inst in, ServerPlayer p) {
        int n = in.fouten(p), na = in.def.hintNa();
        return n >= na * 2 ? in.def.key("hint2") : n >= na ? in.def.key("hint1") : null;
    }

    /** Solved: open the doors/cracks, celebrate, reward everyone near (once per player). */
    static void opgelost(Inst in, ServerPlayer trigger) {
        PuzzelDef d = in.def;
        long nu = in.level.getGameTime();
        boolean perSpeler = d.soort().equals("tijdrit") || d.soort().equals("wachtwoord");
        if (!perSpeler) {
            in.opgelostTot = nu + Math.max(40, d.reset());
            in.demoStap = -1;
            for (BlockPos l : d.deur()) {
                in.level.setBlock(in.wereld(l), Blocks.AIR.defaultBlockState(), 3);
            }
            for (int k : d.van("scheur")) {
                in.level.setBlock(in.pos[k], Blocks.AIR.defaultBlockState(), 3);
                in.level.sendParticles(ParticleTypes.CLOUD, in.pos[k].getX() + 0.5, in.pos[k].getY() + 0.5, in.pos[k].getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
            }
            for (int k : d.van("lichtdoel")) {
                zet(in, k, 0, true);
            }
        }
        BlockPos m = in.midden();
        in.level.sendParticles(new DustParticleOptions(0xFF8CC8, 1.5f), m.getX() + 0.5, m.getY() + 1.5, m.getZ() + 0.5, 40, d.breed() / 3.0, 1, d.diep() / 3.0, 0);
        in.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX() + 0.5, m.getY() + 1, m.getZ() + 0.5, 20, d.breed() / 3.0, 0.5, d.diep() / 3.0, 0);
        in.level.playSound(null, m, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.4f);
        Set<ServerPlayer> beloond = new HashSet<>();
        beloond.add(trigger);
        if (!perSpeler) {
            for (ServerPlayer o : in.level.getEntitiesOfClass(ServerPlayer.class, in.doos(8))) {
                beloond.add(o);
            }
        }
        for (ServerPlayer o : beloond) {
            PuzzelSpeler.beloon(o, d);
        }
    }

    /** Back to the start state (elements, push blocks, doors, cracks). Needs the area loaded. */
    public static void herstel(Inst in) {
        PuzzelDef d = in.def;
        in.voortgang = 0;
        in.opgelostTot = 0;
        in.demoStap = -1;
        in.race.clear();
        for (int i = 0; i < in.pos.length; i++) {
            PuzzelDef.Element e = d.elementen().get(i);
            BlockPos thuis = in.origin.offset(e.lokaal());
            if (!in.pos[i].equals(thuis)) {
                if (in.level.getBlockState(in.pos[i]).getBlock() instanceof PuzzelBlok) {
                    in.level.setBlock(in.pos[i], Blocks.AIR.defaultBlockState(), 3);
                }
                INDEX.remove(GlobalPos.of(in.level.dimension(), in.pos[i]));
                in.pos[i] = thuis;
                INDEX.put(GlobalPos.of(in.level.dimension(), thuis), new Ref(in, i));
            }
        }
        for (int i = 0; i < in.pos.length; i++) {
            PuzzelDef.Element e = d.elementen().get(i);
            in.level.setBlock(in.pos[i], staat(e.blok(), e.stand(), false), 3);
        }
        BlockState deur = staat("geheimdeur", 0, false);
        for (BlockPos l : d.deur()) {
            in.level.setBlock(in.wereld(l), deur, 3);
        }
        in.schoon = true;
    }

    // ---- ticking -----------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel rijk = Vadsrijk.level(server);
        if (rijk != null && rijk.getGameTime() % 20 == 0) {
            PuzzelStempel.tick(server, rijk);
        }
        for (Inst in : INST.values()) {
            tick(in);
        }
        // plates: whoever stands on one (stepOn is not reliable for a 1-pixel plate)
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel sl = (ServerLevel) p.level();
            if ((sl == rijk || !INST.isEmpty()) && !p.isSpectator()) {
                BlockPos voet = p.blockPosition();
                if (sl.getBlockState(voet).getBlock() instanceof PuzzelBlok b && (b.soort.equals("drukplaat") || b.soort.equals("eindplaat"))) {
                    stap(sl, voet, p);
                }
            }
        }
    }

    static void tick(Inst in) {
        ServerLevel l = in.level;
        long nu = l.getGameTime();
        if (!l.isLoaded(in.origin) || !l.isLoaded(in.origin.offset(in.def.breed(), 0, in.def.diep()))) {
            return;
        }
        if (!in.schoon && in.laatsteActie == 0 && !in.test) {
            herstel(in);   // (first time loaded since the server started: a clean start)
        }
        if (in.opgelostTot > 0 && nu >= in.opgelostTot) {
            herstel(in);
        }
        if (!in.schoon && in.opgelostTot == 0 && in.laatsteActie > 0 && nu - in.laatsteActie > IDLE_RESET
                && l.getEntitiesOfClass(ServerPlayer.class, in.doos(16)).isEmpty()) {
            herstel(in);
        }
        // the demo tune
        if (in.demoStap >= 0 && nu >= in.demoVolgende) {
            List<Integer> opl = in.def.oplossing();
            if (in.demoStap > 0) {
                int vorige = opl.get(in.demoStap - 1);
                zet(in, vorige, stand(in, vorige), false);
            }
            if (in.demoStap < opl.size()) {
                int i = opl.get(in.demoStap);
                zet(in, i, stand(in, i), true);
                speelNoot(in, i);
                in.demoStap++;
                in.demoVolgende = nu + 14;
            } else {
                in.demoStap = -1;
            }
        }
        // races (per player)
        if (!in.race.isEmpty()) {
            for (Map.Entry<UUID, Long> e : new ArrayList<>(in.race.entrySet())) {
                ServerPlayer p = l.getServer().getPlayerList().getPlayer(e.getKey());
                if (p == null) {
                    in.race.remove(e.getKey());
                } else if (nu > e.getValue()) {
                    in.race.remove(e.getKey());
                    p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.tijd.op").withStyle(ChatFormatting.RED));
                    l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1f, 0.5f);
                    fout(in, p, false);
                } else if ((e.getValue() - nu) % 20 == 0) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.puzzels.tijd.over", (e.getValue() - nu) / 20).withStyle(ChatFormatting.GOLD));
                }
            }
            if (in.race.isEmpty()) {
                for (int k : in.def.van("startlantaarn")) {
                    zet(in, k, 0, false);
                }
            }
        }
        in.luistert.values().removeIf(t -> t < nu);
        if (in.eindUit > 0 && nu >= in.eindUit) {
            in.eindUit = 0;
            for (int k : in.def.van("eindplaat")) {
                zet(in, k, 0, false);
            }
        }
        if (nu % 10 == 0 && in.def.soort().equals("licht")) {
            List<ServerPlayer> dichtbij = l.getEntitiesOfClass(ServerPlayer.class, in.doos(24));
            if (!dichtbij.isEmpty()) {
                Straal s = straal(in);
                for (BlockPos b : s.pad()) {
                    l.sendParticles(new DustParticleOptions(0xFFF0A0, 1.0f), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.15, 0.05, 0.15, 0);
                }
            }
        }
        // the puzzle line on screen ("puzzeltip constant in beeld", round 16); secrets stay quiet
        if (nu % 40 == 0 && !in.def.stil()) {
            for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, in.doos(2))) {
                if (in.race.containsKey(p.getUUID())) {
                    continue;
                }
                String tip = tipVoor(in, p);
                Component regel = tip != null ? Component.translatable("vadsrijk.puzzels.tip", Component.translatable(tip))
                        : Component.translatable("vadsrijk.puzzels.soortkop", Component.translatable(in.def.naamKey()), Component.translatable(in.def.key("uitleg")));
                p.sendOverlayMessage(regel.copy().withStyle(in.opgelost() ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Dev/tests: the instance key of a test instance. */
    static Map<String, Inst> alleInst() {
        return INST;
    }
}
