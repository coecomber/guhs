package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gevecht;
import nl.juiced.guhs.feature.vadsrijk.kern.Kampvuren;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.ZoneBoek;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * 1.10.3 "Kampvuurreizen": once a zone's Verkenningsboek page is at 100 %, every campfire of that zone the player has lit
 * ({@link Voltooiing#vuurAan}) is a fast-travel point. Right-clicking such a campfire (the checkpoint action still happens first, see
 * {@code Kampvuren}; sneaking = only the checkpoint) opens a small screen with the lit campfires of ALL the player's 100 % zones,
 * grouped per zone and named like the book ("Kampvuur: Sporenwoud 2"); a click = an instant teleport next to that campfire (a short
 * fade + particles + a chime, no scene). Free; 3 s anti-spam. The server checks everything again on the click ({@link #kies}):
 * the screen was opened at a campfire of a 100 % zone within 2 minutes and the player still stands near it, the target is in
 * {@link #lijst} (lit, its zone at 100 % and open for him: the zone gate), not in combat ({@link Gevecht}), not riding, not in a
 * dungeon/raid, a boss arena, an escort or a scene, and the target has a safe floor next to the campfire ({@link #veiligePlek}).
 * The first time a zone reaches 100 % (also for saves that were already there): one chat line + a toast ({@link #meldVrij}).
 * Zones are the book pages with id "zone&lt;N&gt;" and a "kampvuur" category (zone 1..6); other pages (the crafts book) never count.
 */
public final class KampvuurReizen {
    public static final String K = "vadsrijk.voltooiing.reis.";
    /** Anti-spam between two trips (ticks). */
    public static final int WACHT = 60;
    /** The screen stays valid this long after opening (ticks). */
    static final long GELDIG = 20 * 60 * 2;
    /** You must still stand within this many blocks of the campfire the screen was opened at. */
    static final double BIJ = 8;
    /** The toast payload's "percent" for "campfire travel unlocked" (VoltooiingPayloads.Toast). */
    public static final int TOAST_VRIJ = -1;

    /** Tests: the gametest player is not in the dimension (the level is the player's own). */
    public static boolean TEST;
    /** Tests: the zone pages to use instead of the registered ones (null = the registered pages). */
    @Nullable
    static List<ZoneBoek> TEST_ZONES;

    /** One destination: the book entry's id ("vuur_x,y,z"), zone number + page, its name (book key + arg) and spot. */
    public record Vuur(String id, int zone, ZoneBoek boek, String naamKey, String naamArg, BlockPos plek) {
    }

    private record Open(BlockPos vuur, long tijd) {
    }

    private static final Map<UUID, Open> OPEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private KampvuurReizen() {
    }

    // ---- the rules ---------------------------------------------------------------------------------------------------------------

    static List<ZoneBoek> zones() {
        return TEST_ZONES != null ? TEST_ZONES : Voltooiing.zones();
    }

    /** Tests: the id prefix of a test page ("kampreistest_zone2" = zone 2, never mixed with the real pages' extra categories). */
    static final String TEST_PREFIX = "kampreistest_";

    /** "zone3" = 3; anything else (the crafts book "zone_ambachten") = 0. */
    public static int zoneNr(ZoneBoek z) {
        String id = z.id();
        if (TEST_ZONES != null && id.startsWith(TEST_PREFIX)) {
            id = id.substring(TEST_PREFIX.length());
        }
        if (id.length() == 5 && id.startsWith("zone") && id.charAt(4) >= '1' && id.charAt(4) <= '9') {
            return id.charAt(4) - '0';
        }
        return 0;
    }

    /** The campfire entries of a zone page (its "kampvuur" category). */
    static List<Ding> vuren(ZoneBoek z) {
        List<Ding> l = new ArrayList<>();
        for (Categorie c : Voltooiing.categorieen(z)) {
            if (c.id().equals("kampvuur")) {
                for (Ding d : c.dingen()) {
                    if (d.plek() != null) {
                        l.add(d);
                    }
                }
            }
        }
        return l;
    }

    /** Is campfire travel unlocked in this zone for this player (a numbered zone with campfires, at 100 %)? */
    public static boolean vrij(ServerPlayer p, ZoneBoek z) {
        if (zoneNr(z) <= 0 || vuren(z).isEmpty()) {
            return false;
        }
        Voltooiing.Telling t = Voltooiing.tel(p, z);
        return t.totaal() > 0 && t.vol();
    }

    /** Every destination for this player: the lit campfires of his 100 % zones that are open for him, in zone + book order. */
    public static List<Vuur> lijst(ServerPlayer p) {
        List<Vuur> l = new ArrayList<>();
        for (ZoneBoek z : zones()) {
            int n = zoneNr(z);
            if (n <= 0 || ZonePoort.dicht(p, n) || !vrij(p, z)) {
                continue;
            }
            for (Ding d : vuren(z)) {
                if (Voltooiing.vuurAan(p, d.plek())) {
                    l.add(new Vuur(d.id(), n, z, d.naamKey(), d.naamArg(), d.plek()));
                }
            }
        }
        return l;
    }

    /** The book campfire at (or within 3 blocks of) this spot, in any numbered zone page; null = not a book campfire. */
    @Nullable
    public static Vuur bij(BlockPos pos) {
        Vuur beste = null;
        double best = 27.01;
        for (ZoneBoek z : zones()) {
            int n = zoneNr(z);
            if (n <= 0) {
                continue;
            }
            for (Ding d : vuren(z)) {
                double a = d.plek().distSqr(pos);
                if (a < best) {
                    best = a;
                    beste = new Vuur(d.id(), n, z, d.naamKey(), d.naamArg(), d.plek());
                }
            }
        }
        return beste;
    }

    /**
     * Why this player may not travel right now (a lang key under {@link #K}{@code weiger.}), or null = may. Combat (the Vads regen's
     * 5 s rule), riding, a dungeon or the raid, a boss arena, an escort, a scene, the 3 s anti-spam.
     */
    @Nullable
    public static String weiger(ServerPlayer p) {
        if (Gevecht.in(p)) {
            return "gevecht";
        }
        if (p.isPassenger()) {
            return "rijden";
        }
        if (nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij.dungeonVan(p.getUUID()) != null
                || nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij.inRaid(p)) {
            return "kerker";
        }
        if (Vadsrijk.in(p) && nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas.op(p.getX(), p.getY(), p.getZ()) != null) {
            return "arena";
        }
        for (var e : nl.juiced.guhs.feature.vadsrijk.npc.Escortes.alle()) {
            if (!e.aangekomen() && nl.juiced.guhs.feature.vadsrijk.npc.Escortes.hoort(p, e)) {
                return "escorte";
            }
        }
        if (nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(p) || nl.juiced.guhs.feature.vadsrijk.npc.Verhaal.bezig(p)
                || nl.juiced.guhs.feature.vadsrijk.karakter.Karakters.bezig(p)) {
            return "scene";
        }
        Long t = LAATST.get(p.getUUID());
        long nu = p.level().getGameTime();
        if (t != null && nu - t < WACHT && nu >= t) {
            return "wacht";
        }
        return null;
    }

    // ---- the screen ----------------------------------------------------------------------------------------------------------------

    /** Haken.KAMPVUUR_KLIK: a campfire of a 100 % zone opens the screen (when there is somewhere else to go). Sneaking = rest only. */
    static void klik(ServerPlayer p, BlockPos pos) {
        if (p.isShiftKeyDown()) {
            return;
        }
        Vuur hier = bij(pos);
        if (hier == null || !vrij(p, hier.boek())) {
            return;
        }
        List<Vuur> l = lijst(p);
        if (l.stream().noneMatch(v -> !v.id().equals(hier.id()))) {
            return;
        }
        open(p, hier, l, null);
    }

    /** Opens (or refreshes, with a message) the screen at this campfire. */
    static void open(ServerPlayer p, Vuur hier, List<Vuur> l, @Nullable String melding) {
        OPEN.put(p.getUUID(), new Open(hier.plek(), p.level().getGameTime()));
        VoltooiingPayloads.stuur(p, new VoltooiingPayloads.Reis(schermData(hier, l, melding)));
    }

    /** What the client draws: the campfire you stand at + the destinations per zone (book names). */
    static CompoundTag schermData(Vuur hier, List<Vuur> l, @Nullable String melding) {
        CompoundTag uit = new CompoundTag();
        uit.putString("hier", hier.id());
        uit.putString("hn", hier.naamKey());
        uit.putString("hna", hier.naamArg());
        ListTag zl = new ListTag();
        CompoundTag zt = null;
        ListTag vl = null;
        int zone = -1;
        for (Vuur v : l) {
            if (v.zone() != zone) {
                zone = v.zone();
                zt = new CompoundTag();
                zt.putString("naam", v.boek().naamKey());
                vl = new ListTag();
                zt.put("v", vl);
                zl.add(zt);
            }
            CompoundTag vt = new CompoundTag();
            vt.putString("id", v.id());
            vt.putString("n", v.naamKey());
            vt.putString("na", v.naamArg());
            vl.add(vt);
        }
        uit.put("zones", zl);
        if (melding != null) {
            uit.putString("melding", K + "weiger." + melding);
        }
        return uit;
    }

    /** The client clicked a destination: travel, or show why not in the screen. */
    static void opKies(ServerPlayer p, String id) {
        Open o = OPEN.get(p.getUUID());
        String r = kies(p, id);
        if (r == null || r.equals("dicht") || o == null) {
            return;
        }
        Vuur hier = bij(o.vuur());
        if (hier != null) {
            open(p, hier, lijst(p), r);
        }
    }

    /**
     * Travel to destination {@code id} (a {@link Vuur#id()}). Null = done; else the refusal (a key under {@code weiger.}, or
     * "dicht" = no valid screen: ignored silently).
     */
    @Nullable
    public static String kies(ServerPlayer p, String id) {
        Open o = OPEN.get(p.getUUID());
        long nu = p.level().getGameTime();
        if (o == null || nu - o.tijd() > GELDIG || nu < o.tijd() || !(TEST || Vadsrijk.in(p)) || !(p.level() instanceof ServerLevel sl)) {
            return "dicht";
        }
        if (p.distanceToSqr(Vec3.atCenterOf(o.vuur())) > BIJ * BIJ) {
            return "ver";
        }
        Vuur hier = bij(o.vuur());
        if (hier == null || !vrij(p, hier.boek()) || !Voltooiing.vuurAan(p, hier.plek())) {
            return "niet_vrij";
        }
        String w = weiger(p);
        if (w != null) {
            return w;
        }
        Vuur doel = null;
        for (Vuur v : lijst(p)) {
            if (v.id().equals(id)) {
                doel = v;
            }
        }
        if (doel == null) {
            return "onbekend";
        }
        if (doel.id().equals(hier.id())) {
            return "hier";
        }
        BlockPos vuur = Kampvuren.zoekVuur(sl, doel.plek());
        BlockPos plek = vuur == null ? null : veiligePlek(sl, vuur);
        if (plek == null) {
            return "geen_plek";
        }
        reis(p, sl, vuur, plek, doel);
        return null;
    }

    private static void reis(ServerPlayer p, ServerLevel sl, BlockPos vuur, BlockPos plek, Vuur doel) {
        OPEN.remove(p.getUUID());
        LAATST.put(p.getUUID(), sl.getGameTime());
        Vec3 van = p.position();
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, van.x, van.y + 1, van.z, 30, 0.5, 0.8, 0.5, 0.02);
        sl.sendParticles(ParticleTypes.FLAME, van.x, van.y + 0.6, van.z, 12, 0.3, 0.5, 0.3, 0.01);
        sl.playSound(null, BlockPos.containing(van), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);
        double dx = vuur.getX() - plek.getX(), dz = vuur.getZ() - plek.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;   // looking at the campfire
        p.teleportTo(sl, plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, Set.of(), yaw, 10f, true);
        p.fallDistance = 0;
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, plek.getX() + 0.5, plek.getY() + 1, plek.getZ() + 0.5, 40, 0.6, 0.8, 0.6, 0.02);
        sl.sendParticles(ParticleTypes.HEART, vuur.getX() + 0.5, vuur.getY() + 1.4, vuur.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0);
        sl.playSound(null, plek, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.3f);
        SpelerData d = Spelers.data(p);
        d.checkpoint = plek;   // (you rest at the campfire you travelled to)
        Spelers.sync(p);
        p.sendOverlayMessage(Component.translatable(K + "onderweg", Component.translatable(doel.naamKey(), naamArgs(doel.naamArg())))
                .withStyle(ChatFormatting.GOLD));
        VoltooiingPayloads.stuur(p, new VoltooiingPayloads.ReisKlaar(14));
    }

    /** The %s args of a book name ("key#n" = a lang key + a literal number, see BoekScherm.naam). */
    static Object[] naamArgs(String arg) {
        if (arg.isEmpty()) {
            return new Object[0];
        }
        int h = arg.indexOf('#');
        if (h >= 0) {
            return new Object[] {Component.translatable(arg.substring(0, h)), arg.substring(h + 1)};
        }
        return new Object[] {Component.translatable(arg)};
    }

    /**
     * A safe spot to stand next to this campfire (never in it, never on it): 2 blocks out first (south, north, east, west), then the
     * other spots around it, one block up or down allowed. Feet and head free of blocks and fluids, no fire / berry bush / powder
     * snow at the feet, a solid floor that is not a campfire, magma, fire or cactus. Null = none.
     */
    @Nullable
    public static BlockPos veiligePlek(ServerLevel sl, BlockPos vuur) {
        int[][] ring = {{0, 2}, {0, -2}, {2, 0}, {-2, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}, {2, 2}, {-2, 2}, {2, -2},
                {-2, -2}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}, {2, 1}, {2, -1}, {-2, 1}, {-2, -1}};
        for (int dy : new int[] {0, 1, -1}) {
            for (int[] r : ring) {
                BlockPos b = vuur.offset(r[0], dy, r[1]);
                if (staanbaar(sl, b)) {
                    return b.immutable();
                }
            }
        }
        return null;
    }

    static boolean staanbaar(ServerLevel sl, BlockPos b) {
        BlockState voet = sl.getBlockState(b), hoofd = sl.getBlockState(b.above()), vloer = sl.getBlockState(b.below());
        if (!voet.getCollisionShape(sl, b).isEmpty() || !hoofd.getCollisionShape(sl, b.above()).isEmpty()) {
            return false;
        }
        if (!voet.getFluidState().isEmpty() || !hoofd.getFluidState().isEmpty()) {
            return false;
        }
        if (voet.is(BlockTags.FIRE) || voet.getBlock() instanceof CampfireBlock || voet.is(Blocks.SWEET_BERRY_BUSH) || voet.is(Blocks.POWDER_SNOW)
                || voet.is(Blocks.COBWEB)) {
            return false;
        }
        if (vloer.getCollisionShape(sl, b.below()).isEmpty() || vloer.getBlock() instanceof CampfireBlock || vloer.is(Blocks.MAGMA_BLOCK)
                || vloer.is(BlockTags.FIRE) || vloer.is(Blocks.CACTUS) || !vloer.getFluidState().isEmpty()) {
            return false;
        }
        return true;
    }

    // ---- the unlock message --------------------------------------------------------------------------------------------------------

    /** Voltooiing.controleer: the first time zone {@code z} is at 100 %: a chat line + a toast (once per zone, also for old saves). */
    static void meldVrij(ServerPlayer p, ZoneBoek z) {
        if (zoneNr(z) <= 0 || vuren(z).isEmpty()) {
            return;
        }
        CompoundTag m = Voltooiing.map(p, "reis");
        if (m.getBooleanOr(z.id(), false)) {
            return;
        }
        m.putBoolean(z.id(), true);
        p.sendSystemMessage(Component.translatable(K + "vrij.chat", Component.translatable(z.naamKey())).withStyle(ChatFormatting.GOLD));
        VoltooiingPayloads.stuur(p, new VoltooiingPayloads.Toast(z.naamKey(), TOAST_VRIJ));
        Spelers.opgeslagen(p);
    }

    /** Was the unlock already announced for this zone (tests)? */
    static boolean gemeld(ServerPlayer p, String zone) {
        return Voltooiing.map(p, "reis").getBooleanOr(zone, false);
    }

    // ---- housekeeping / tests ------------------------------------------------------------------------------------------------------

    static void vergeet(UUID id) {
        OPEN.remove(id);
        LAATST.remove(id);
    }

    /** Tests: pretend the screen was opened at this campfire just now. */
    static void openVoorTest(ServerPlayer p, BlockPos vuur) {
        OPEN.put(p.getUUID(), new Open(vuur, p.level().getGameTime()));
    }
}
