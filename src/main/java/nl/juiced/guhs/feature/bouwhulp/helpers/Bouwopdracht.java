package nl.juiced.guhs.feature.bouwhulp.helpers;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.kern.Materiaal;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.quests.SafariQuests;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfData;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * The Bouwopdracht of a verblijfsbord (design 2.8 + 2.9): the family's job. One project per board at a time (ronde 30),
 * within 48 blocks of it; the helpers are free (only the blocks cost, from the owner's inventory and the board's Bank Guh);
 * a happier family builds bigger and prettier ({@link #grootte}). The family walks to the site ("werkuitje"), plays its
 * work animations there while the ghost is built at the family's pace (the board is the ghost's {@link
 * nl.juiced.guhs.feature.bouwhulp.kern.Bouwer}), and walks home when it is done. Two jobs build nothing: the kangaroo's
 * pouch transport between two chests and the elephant carrying for a Bouwkeet project nearby (the keet builds faster).
 * <p>
 * The state lives in the board's tag {@code Bouwopdracht} ({@link VerblijfsbordBlockEntity#bouwopdracht()}).
 */
public final class Bouwopdracht {
    /** The board's state keys. */
    private static final String KEUZE = "Keuze", VARIANT = "Variant", SPOOK = "Spook", TAAK = "Taak", SOORT = "Soort", PUNTEN = "Punten",
            OORSPRONG = "Oorsprong", START = "Start", AANGEKOMEN = "Aangekomen", BANK = "Bank", KLAAR = "Klaar", TRANSPORT_STAP = "Stap",
            GEPLAATST = "Geplaatst";
    /** How long the family may take to walk to the site before the work starts anyway (ticks). */
    static final int MAX_LOPEN = 600;

    /** Elephants carrying for a keet: per dimension, keet → (board → until when). */
    private static final Map<ResourceKey<Level>, Map<Long, Map<Long, Long>>> SJOUWERS = new HashMap<>();

    /** (Gametests) how far "Zoek plek" looks (the test areas are small and side by side). */
    public static volatile int testZoekStraal = -1;

    // =====================================================================================================================
    // what the family can do
    // =====================================================================================================================

    /** The tasks of this board's species that have a template (verblijf.json's "taken"). */
    public static List<String> taken(VerblijfsbordBlockEntity be) {
        VerblijfData.Dier d = VerblijfData.dier(be.soort());
        List<String> uit = new ArrayList<>();
        if (d != null) {
            for (String t : d.taken()) {
                if (Sjablonen.bestaat(t)) {
                    uit.add(t);
                }
            }
        }
        return uit;
    }

    /** 0..2: a happy family (70+) builds big, a content one (40+) medium, a grumpy one small. */
    public static int grootte(VerblijfsbordBlockEntity be) {
        int s = be.score();
        return s >= 70 ? 2 : s >= 40 ? 1 : 0;
    }

    static CompoundTag tag(VerblijfsbordBlockEntity be) {
        return be.bouwopdracht();
    }

    public static String keuze(VerblijfsbordBlockEntity be) {
        String k = tag(be).getStringOr(KEUZE, "");
        List<String> t = taken(be);
        return t.contains(k) ? k : t.isEmpty() ? "" : t.get(0);
    }

    public static int variant(VerblijfsbordBlockEntity be) {
        return tag(be).getIntOr(VARIANT, 0);
    }

    public static Klus klus(ServerLevel level, VerblijfsbordBlockEntity be, String taak) {
        int straal = testZoekStraal > 0 ? testZoekStraal : Klus.MAX_AFSTAND;
        return new Klus(level, be.getBlockPos(), be.soort(), taak, Math.floorMod(variant(be), Math.max(1, Sjablonen.varianten(taak))), grootte(be),
                straal, net.minecraft.util.RandomSource.create(be.getBlockPos().asLong() * 31 + taak.hashCode()));
    }

    // =====================================================================================================================
    // the running project
    // =====================================================================================================================

    @Nullable
    public static Spook spook(ServerLevel level, VerblijfsbordBlockEntity be) {
        String id = tag(be).getStringOr(SPOOK, "");
        if (id.isEmpty()) {
            return null;
        }
        try {
            return Spookbouw.get(level, UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Is a job running (a ghost of this board, or the pouch transport, or the carrying)? */
    public static boolean bezig(ServerLevel level, VerblijfsbordBlockEntity be) {
        CompoundTag t = tag(be);
        String soort = t.getStringOr(SOORT, "");
        if (soort.isEmpty()) {
            return false;
        }
        if (soort.equals(Sjabloon.Soort.SPOOK.name())) {
            return spook(level, be) != null;
        }
        return true;
    }

    private static List<BlockPos> punten(CompoundTag t) {
        List<BlockPos> uit = new ArrayList<>();
        for (long l : t.getLongArray(PUNTEN).orElse(new long[0])) {
            uit.add(BlockPos.of(l));
        }
        return uit;
    }

    private static void zetPunten(CompoundTag t, List<BlockPos> punten) {
        t.putLongArray(PUNTEN, punten.stream().mapToLong(BlockPos::asLong).toArray());
    }

    /**
     * Starts the job: {@code punten} null = "Zoek plek" (the family looks itself), else the spots the player pointed at.
     * The ghost appears with its control panel (shift, turn, Bouw!); false (and a message) when it cannot.
     */
    public static boolean start(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be, String taak, @Nullable List<BlockPos> punten) {
        if (bezig(level, be)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.nee.al_bezig").withStyle(ChatFormatting.RED));
            return false;
        }
        Sjabloon s = Sjablonen.van(taak);
        if (s == null || !taken(be).contains(taak) && !taak.equals("verblijf")) {
            return false;
        }
        Klus k = klus(level, be, taak);
        try {
            List<BlockPos> plek = punten == null ? s.zoek(k) : punten;
            for (BlockPos q : plek) {
                if (q.distSqr(be.getBlockPos().atY(q.getY())) > (double) Klus.MAX_AFSTAND * Klus.MAX_AFSTAND) {
                    throw Klus.nee("te_ver");
                }
            }
            return switch (s.soort()) {
                case SPOOK -> spookStart(p, level, be, taak, s.maak(k, plek), plek);
                case TRANSPORT -> transportStart(p, level, be, taak, plek);
                case SJOUWEN -> sjouwStart(p, level, be, taak, plek);
            };
        } catch (Klus.Nee nee) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.nee." + nee.sleutel).withStyle(ChatFormatting.RED));
            laatsteNee = nee.sleutel;
            return false;
        }
    }

    /** "Bouw passend verblijf": the fitting home as this board's project. */
    public static boolean verblijf(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be) {
        if (bezig(level, be)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.nee.al_bezig").withStyle(ChatFormatting.RED));
            return false;
        }
        try {
            Map<BlockPos, BlockState> m = PassendVerblijf.maak(klus(level, be, "verblijf"), be);
            return spookStart(p, level, be, "verblijf", m, List.of(be.getBlockPos()));
        } catch (Klus.Nee nee) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.nee." + nee.sleutel).withStyle(ChatFormatting.RED));
            laatsteNee = nee.sleutel;
            return false;
        }
    }

    /** Why the last start said no (tests). */
    @Nullable
    public static volatile String laatsteNee;

    /** Only what can and must change: no protected spots, no chests, no spot that already is what the template wants. */
    static Map<BlockPos, BlockState> filter(ServerLevel level, Map<BlockPos, BlockState> m) {
        Map<BlockPos, BlockState> uit = new java.util.LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            BlockState nu = level.getBlockState(e.getKey());
            if (nu != e.getValue() && Spoken.magBouwen(level, e.getKey(), nu) && Materiaal.kost(e.getValue()) != null) {
                uit.put(e.getKey(), e.getValue());
            }
        }
        return uit;
    }

    private static BlockPos laag(Map<BlockPos, BlockState> m) {
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
        for (BlockPos b : m.keySet()) {
            x = Math.min(x, b.getX());
            y = Math.min(y, b.getY());
            z = Math.min(z, b.getZ());
        }
        return new BlockPos(x, y, z);
    }

    private static boolean spookStart(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be, String taak, Map<BlockPos, BlockState> ruw,
                                      List<BlockPos> plek) {
        Map<BlockPos, BlockState> m = filter(level, ruw);
        if (m.isEmpty()) {
            throw Klus.nee("niets_te_doen");
        }
        BlockPos oorsprong = laag(m);
        Ontwerp o = Ontwerp.van(m);
        AABB doos = new AABB(oorsprong.getX(), oorsprong.getY(), oorsprong.getZ(), oorsprong.getX() + o.grootte().getX(),
                oorsprong.getY() + o.grootte().getY(), oorsprong.getZ() + o.grootte().getZ());
        double dx = doos.getCenter().x - be.getBlockPos().getX(), dz = doos.getCenter().z - be.getBlockPos().getZ();
        if (dx * dx + dz * dz > (double) Klus.MAX_AFSTAND * Klus.MAX_AFSTAND) {
            throw Klus.nee("te_ver");
        }
        // one helper at a time on a spot (design 2.8, "combo's"): no overlap with another family's ghost
        for (Spook ander : Spoken.van(level).alle()) {
            if (ander.bouwerPos() != null && !ander.bouwerPos().equals(be.getBlockPos())
                    && level.getBlockEntity(ander.bouwerPos()) instanceof VerblijfsbordBlockEntity && ander.doos().intersects(doos)) {
                throw Klus.nee("andere_helper");
            }
        }
        Spook s = Spookbouw.start(p, o, oorsprong, Rotation.NONE, Mirror.NONE);
        if (s == null) {
            return false;
        }
        Spookbouw.koppel(level, s, be.getBlockPos());
        Spookbouw.zetNaam(level, s, Component.translatable("gui.guhs.bouwopdracht.spook", Component.translatable("entity.guhs.safari." + be.soort()),
                Component.translatable(taak.equals("verblijf") ? "gui.guhs.verblijf.tevreden.bouw" : "gui.guhs.verblijf.taak." + taak)).getString());
        if (be.getLevel() != null && Spookbouw.get(level, s.id()) != null) {
            CompoundTag t = tag(be);
            t.putString(SPOOK, s.id().toString());
            t.putString(TAAK, taak);
            t.putString(SOORT, Sjabloon.Soort.SPOOK.name());
            zetPunten(t, plek);
            t.putLong(OORSPRONG, oorsprong.asLong());
            t.putLong(START, -1);
            t.putBoolean(AANGEKOMEN, false);
            t.putInt(GEPLAATST, 0);
            be.setChanged();
        }
        Spookbouw.toonBediening(p, s);
        return true;
    }

    /** The kangaroo: two chests (A then B); every two seconds a pouch of up to 8 items hops from A to B. */
    private static boolean transportStart(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be, String taak, List<BlockPos> plek) {
        if (plek.size() < 2 || !(level.getBlockEntity(plek.get(0)) instanceof Container) || !(level.getBlockEntity(plek.get(1)) instanceof Container)
                || plek.get(0).equals(plek.get(1))) {
            throw Klus.nee("twee_kisten");
        }
        CompoundTag t = tag(be);
        t.putString(TAAK, taak);
        t.putString(SOORT, Sjabloon.Soort.TRANSPORT.name());
        t.remove(SPOOK);
        zetPunten(t, plek);
        t.putInt(TRANSPORT_STAP, 0);
        be.setChanged();
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.transport.start"));
        return true;
    }

    /** The two nearest containers near the board (the kangaroo's "Zoek plek"). */
    static List<BlockPos> tweeKisten(Klus k) {
        List<BlockPos> kisten = new ArrayList<>();
        int r = Math.min(24, k.zoekStraal());
        for (BlockPos q : BlockPos.betweenClosed(k.bord().offset(-r, -4, -r), k.bord().offset(r, 4, r))) {
            if (k.level().getBlockEntity(q) instanceof Container && !(k.level().getBlockEntity(q) instanceof VerblijfsbordBlockEntity)) {
                kisten.add(q.immutable());
            }
        }
        kisten.sort(java.util.Comparator.comparingDouble(q -> q.distSqr(k.bord())));
        if (kisten.size() < 2) {
            throw Klus.nee("twee_kisten");
        }
        return List.of(kisten.get(0), kisten.get(kisten.size() > 1 ? 1 : 0));
    }

    /** The elephant: carries for the Bouwkeet (its project builds faster while the elephant is at it). */
    private static boolean sjouwStart(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be, String taak, List<BlockPos> plek) {
        BouwkeetBlockEntity keet = keetBij(level, plek.get(0));
        if (keet == null) {
            throw Klus.nee("geen_keet");
        }
        CompoundTag t = tag(be);
        t.putString(TAAK, taak);
        t.putString(SOORT, Sjabloon.Soort.SJOUWEN.name());
        t.remove(SPOOK);
        zetPunten(t, List.of(keet.getBlockPos()));
        be.setChanged();
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.sjouwen.start"));
        return true;
    }

    @Nullable
    static BouwkeetBlockEntity keetBij(ServerLevel level, BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-2, -2, -2), p.offset(2, 2, 2))) {
            if (level.getBlockEntity(q) instanceof BouwkeetBlockEntity keet) {
                return keet;
            }
        }
        return null;
    }

    /** The nearest Bouwkeet (with a project) within reach of the board. */
    static BlockPos keetZoek(Klus k) {
        int r = Math.min(Klus.MAX_AFSTAND, k.zoekStraal());
        BlockPos beste = null;
        double d = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(k.bord().offset(-r, -8, -r), k.bord().offset(r, 8, r))) {
            if (k.level().getBlockEntity(q) instanceof BouwkeetBlockEntity && q.distSqr(k.bord()) < d) {
                d = q.distSqr(k.bord());
                beste = q.immutable();
            }
        }
        if (beste == null) {
            throw Klus.nee("geen_keet");
        }
        return beste;
    }

    /** How many elephants carry for this keet now (each adds a bouwguh's tempo; at most 2 count). */
    public static int sjouwers(ServerLevel level, BouwkeetBlockEntity keet) {
        Map<Long, Map<Long, Long>> per = SJOUWERS.get(level.dimension());
        if (per == null) {
            return 0;
        }
        Map<Long, Long> borden = per.get(keet.getBlockPos().asLong());
        if (borden == null) {
            return 0;
        }
        long nu = level.getGameTime();
        borden.values().removeIf(tot -> tot < nu);
        return Math.min(2, borden.size());
    }

    /** "Herbereken": the template again where the ghost now stands (moved with the arrows), with the current variant. */
    public static boolean herbereken(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be) {
        Spook s = spook(level, be);
        CompoundTag t = tag(be);
        if (s == null || s.status() == Spook.Status.BOUWT) {
            return false;
        }
        String taak = t.getStringOr(TAAK, "");
        BlockPos oud = BlockPos.of(t.getLongOr(OORSPRONG, s.oorsprong().asLong()));
        BlockPos verschil = s.oorsprong().subtract(oud);
        List<BlockPos> plek = new ArrayList<>();
        for (BlockPos q : punten(t)) {
            plek.add(q.offset(verschil));
        }
        try {
            Map<BlockPos, BlockState> m;
            if (taak.equals("verblijf")) {
                m = filter(level, PassendVerblijf.maak(klus(level, be, taak), be));
            } else {
                Sjabloon sj = Sjablonen.van(taak);
                if (sj == null) {
                    return false;
                }
                m = filter(level, sj.maak(klus(level, be, taak), plek));
            }
            if (m.isEmpty()) {
                throw Klus.nee("niets_te_doen");
            }
            BlockPos nieuw = laag(m);
            Spookbouw.vervang(level, s, Ontwerp.van(m));
            Spookbouw.zet(level, s, nieuw, Rotation.NONE, Mirror.NONE);
            t.putLong(OORSPRONG, nieuw.asLong());
            zetPunten(t, plek);
            be.setChanged();
            Spookbouw.toonBediening(p, s);
            return true;
        } catch (Klus.Nee nee) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwopdracht.nee." + nee.sleutel).withStyle(ChatFormatting.RED));
            return false;
        }
    }

    /** Stops the job: the ghost goes (what is built stays), the family comes home. */
    public static void stop(ServerLevel level, VerblijfsbordBlockEntity be) {
        Spook s = spook(level, be);
        if (s != null) {
            Spookbouw.annuleer(level, s);
        }
        klaar(level, be, false);
    }

    private static void klaar(ServerLevel level, VerblijfsbordBlockEntity be, boolean gelukt) {
        CompoundTag t = tag(be);
        for (String k : new String[]{SPOOK, TAAK, SOORT, PUNTEN, OORSPRONG, START, AANGEKOMEN, TRANSPORT_STAP, GEPLAATST}) {
            t.remove(k);
        }
        if (gelukt) {
            t.putInt(KLAAR, t.getIntOr(KLAAR, 0) + 1);
        }
        for (ThuisDier d : be.dieren(level)) {
            d.naarHuis(be, gelukt);
        }
        be.setChanged();
    }

    // =====================================================================================================================
    // the Bouwer side (called by the board, which is the ghost's builder)
    // =====================================================================================================================

    /** Blocks per second: nothing until the family is at the site (or has walked long enough), then 1.5 .. 4 by happiness. */
    public static double tempo(ServerLevel level, VerblijfsbordBlockEntity be, Spook s) {
        CompoundTag t = tag(be);
        if (!t.getBooleanOr(AANGEKOMEN, false)) {
            long start = t.getLongOr(START, -1);
            if (start < 0 || level.getGameTime() - start < MAX_LOPEN) {
                return 0;
            }
        }
        double tempo = 1.5 + 2.5 * be.score() / 100.0;
        return Math.max(tempo, s.totaal() / 3600.0);
    }

    public static Materiaal.Bronnen bronnen(ServerLevel level, VerblijfsbordBlockEntity be, @Nullable ServerPlayer bouwer) {
        return Materiaal.bronnen(level, bouwer, bank(be), null);
    }

    @Nullable
    public static UUID bank(VerblijfsbordBlockEntity be) {
        String b = tag(be).getStringOr(BANK, "");
        try {
            return b.isEmpty() ? null : UUID.fromString(b);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void zetBank(VerblijfsbordBlockEntity be, @Nullable UUID bank) {
        if (bank == null) {
            tag(be).remove(BANK);
        } else {
            tag(be).putString(BANK, bank.toString());
        }
        be.setChanged();
    }

    /** F12: whose work sounds like gnawing (bouwhulp.knaag) and whose like digging (bouwhulp.graaf). */
    private static final java.util.Set<String> KNAGERS = java.util.Set.of("bever", "specht"),
            GRAVERS = java.util.Set.of("stokstaartje", "das", "wombat", "olifant", "neushoorn", "kafferbuffel", "mammoet", "otter");

    /** A block went in: now and then the nearest family member plays a work animation toward it, with a little sound. */
    public static void geplaatst(ServerLevel level, VerblijfsbordBlockEntity be, Spook s, BlockPos pos) {
        CompoundTag t = tag(be);
        int n = t.getIntOr(GEPLAATST, 0) + 1;
        t.putInt(GEPLAATST, n);
        if (n % 6 != 0) {
            return;
        }
        ThuisDier dichtst = null;
        double d = Double.MAX_VALUE;
        for (ThuisDier dier : be.dieren(level)) {
            double dd = dier.distanceToSqr(pos.getCenter());
            if (dd < d) {
                d = dd;
                dichtst = dier;
            }
        }
        if (dichtst != null && d < 16 * 16) {
            dichtst.werk(pos);
        }
        // F12: gnawers gnaw, diggers dig, the rest plonk their blocks in
        String soort = be.soort();
        net.minecraft.sounds.SoundEvent geluid = KNAGERS.contains(soort) ? nl.juiced.guhs.feature.bouwhulp.BouwGeluid.KNAAG.get()
                : GRAVERS.contains(soort) ? nl.juiced.guhs.feature.bouwhulp.BouwGeluid.GRAAF.get() : nl.juiced.guhs.feature.bouwhulp.BouwGeluid.PLONK.get();
        level.playSound(null, pos, geluid, SoundSource.NEUTRAL, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.3f);
    }

    public static void gestopt(ServerLevel level, VerblijfsbordBlockEntity be, Spook s, boolean gelukt) {
        if (!gelukt) {
            return;                                     // (out of material: the rest stays a ghost, the family waits there)
        }
        String taak = tag(be).getStringOr(TAAK, "");
        ServerPlayer eigenaar = be.eigenaar() == null ? null : level.getServer().getPlayerList().getPlayer(be.eigenaar());
        if (eigenaar != null) {
            eigenaar.sendSystemMessage(Component.translatable("gui.guhs.bouwopdracht.klaar", Component.translatable("entity.guhs.safari." + be.soort()),
                    Component.translatable(taak.equals("verblijf") ? "gui.guhs.verblijf.tevreden.bouw" : "gui.guhs.verblijf.taak." + taak))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            if (taak.equals("dam")) {
                SafariQuests.beverdamGebouwd(eigenaar);          // Beverbaas: 5 beaver dams
            }
        }
        klaar(level, be, true);
    }

    // =====================================================================================================================
    // ticking (every second, from the board)
    // =====================================================================================================================

    public static void tick(ServerLevel level, VerblijfsbordBlockEntity be) {
        CompoundTag t = tag(be);
        String soort = t.getStringOr(SOORT, "");
        if (soort.isEmpty()) {
            return;
        }
        List<BlockPos> plek = punten(t);
        if (soort.equals(Sjabloon.Soort.SPOOK.name())) {
            Spook s = spook(level, be);
            if (s == null) {
                klaar(level, be, false);                // cancelled in its panel
                return;
            }
            if (s.status() == Spook.Status.SPOOK) {
                return;                                 // not confirmed yet: the family waits at home
            }
            if (t.getLongOr(START, -1) < 0) {
                t.putLong(START, level.getGameTime());
            }
            AABB doos = s.doos();
            BlockPos doel = BlockPos.containing(doos.getCenter().x, doos.minY, doos.getCenter().z);
            boolean er = false;
            for (ThuisDier d : be.dieren(level)) {
                d.werkuitje(doel, (int) Math.max(doos.getXsize(), doos.getZsize()) / 2 + 2);
                er |= !d.isBaby() && d.distanceToSqr(doel.getCenter()) < Math.pow(Math.max(doos.getXsize(), doos.getZsize()) / 2 + 6, 2);
            }
            if (er || be.terrarium() || be.dieren(level).isEmpty()) {
                t.putBoolean(AANGEKOMEN, true);
            }
            return;
        }
        if (plek.isEmpty()) {
            klaar(level, be, false);
            return;
        }
        if (soort.equals(Sjabloon.Soort.TRANSPORT.name())) {
            transport(level, be, t, plek);
        } else {
            BlockPos keet = plek.get(0);
            if (!(level.getBlockEntity(keet) instanceof BouwkeetBlockEntity)) {
                klaar(level, be, false);
                return;
            }
            boolean heen = (level.getGameTime() / 200) % 2 == 0;
            for (ThuisDier d : be.dieren(level)) {
                if (!d.isBaby()) {
                    d.werkuitje(heen ? keet : be.getBlockPos(), 3);
                }
            }
            SJOUWERS.computeIfAbsent(level.dimension(), x -> new HashMap<>()).computeIfAbsent(keet.asLong(), x -> new HashMap<>())
                    .put(be.getBlockPos().asLong(), level.getGameTime() + 60);
        }
    }

    private static void transport(ServerLevel level, VerblijfsbordBlockEntity be, CompoundTag t, List<BlockPos> plek) {
        BlockPos a = plek.get(0), b = plek.get(1);
        if (!level.isLoaded(a) || !level.isLoaded(b)) {
            return;
        }
        BlockEntity ba = level.getBlockEntity(a), bb = level.getBlockEntity(b);
        if (!(ba instanceof Container van) || !(bb instanceof Container naar)) {
            klaar(level, be, false);
            return;
        }
        int stap = t.getIntOr(TRANSPORT_STAP, 0) + 1;
        t.putInt(TRANSPORT_STAP, stap);
        boolean heen = stap % 4 < 2;
        for (ThuisDier d : be.dieren(level)) {
            if (!d.isBaby()) {
                d.werkuitje(heen ? a : b, 2);
            }
        }
        if (stap % 2 != 0) {
            return;
        }
        for (int i = 0; i < van.getContainerSize(); i++) {
            ItemStack s = van.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            ItemStack buidel = van.removeItem(i, Math.min(8, s.getCount()));
            ItemStack rest = HopperBlockEntity.addItem(van, naar, buidel, null);
            if (!rest.isEmpty()) {
                ItemStack terug = van.getItem(i);
                if (terug.isEmpty()) {
                    van.setItem(i, rest);
                } else {
                    terug.grow(rest.getCount());
                }
            }
            if (rest.getCount() < buidel.getCount()) {
                van.setChanged();
                naar.setChanged();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.getX() + 0.5, b.getY() + 1.1, b.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0);
                level.playSound(null, b, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 0.5f, 1.2f);
            }
            break;
        }
    }

    // =====================================================================================================================
    // the menu (VerblijfPayloads: the Bouwopdracht tab)
    // =====================================================================================================================

    /** What the screen shows: the tasks, the chosen one and its variants, the running job and its progress. */
    public static CompoundTag staat(ServerLevel level, VerblijfsbordBlockEntity be) {
        CompoundTag uit = new CompoundTag();
        ListTag taken = new ListTag();
        taken(be).forEach(x -> taken.add(StringTag.valueOf(x)));
        uit.put("Taken", taken);
        String keuze = keuze(be);
        uit.putString("Keuze", keuze);
        int n = Math.max(1, Sjablonen.varianten(keuze));
        uit.putInt("Varianten", n);
        uit.putInt("Variant", Math.floorMod(variant(be), n));
        Sjabloon s = Sjablonen.van(keuze);
        uit.putInt("Punten", s == null ? 1 : s.punten());
        uit.putInt("Grootte", grootte(be));
        uit.putBoolean("Bank", bank(be) != null);
        uit.putInt("Klaar", tag(be).getIntOr(KLAAR, 0));
        CompoundTag t = tag(be);
        String soort = t.getStringOr(SOORT, "");
        if (!soort.isEmpty() && bezig(level, be)) {
            CompoundTag p = new CompoundTag();
            p.putString("Taak", t.getStringOr(TAAK, ""));
            p.putString("Soort", soort);
            Spook sp = spook(level, be);
            if (sp != null) {
                p.putString("Status", sp.status().name());
                p.putInt("Gedaan", sp.gedaan());
                p.putInt("Totaal", Math.max(1, sp.totaal()));
                p.putBoolean("Lopen", !t.getBooleanOr(AANGEKOMEN, false) && sp.status() == Spook.Status.BOUWT);
            }
            uit.put("Project", p);
        }
        return uit;
    }

    /** The tab's buttons. */
    public enum Knop { KIES, VARIANT, ZOEK, WIJS, HERBEREKEN, BEDIEN, STOP, VERBLIJF }

    public static void doe(ServerPlayer p, ServerLevel level, VerblijfsbordBlockEntity be, Knop knop, int waarde) {
        switch (knop) {
            case KIES -> {
                List<String> t = taken(be);
                if (waarde >= 0 && waarde < t.size()) {
                    tag(be).putString(KEUZE, t.get(waarde));
                    tag(be).putInt(VARIANT, 0);
                    be.setChanged();
                }
            }
            case VARIANT -> {
                tag(be).putInt(VARIANT, Math.floorMod(variant(be) + 1, Math.max(1, Sjablonen.varianten(keuze(be)))));
                be.setChanged();
            }
            case ZOEK -> start(p, level, be, keuze(be), null);
            case WIJS -> Aanwijzen.begin(p, be, keuze(be));
            case HERBEREKEN -> herbereken(p, level, be);
            case BEDIEN -> {
                Spook s = spook(level, be);
                if (s != null) {
                    Spookbouw.toonBediening(p, s);
                }
            }
            case STOP -> stop(level, be);
            case VERBLIJF -> verblijf(p, level, be);
        }
    }

    /** (The work animations of a species: its "bouw_" animations.) */
    public static List<String> werkAnimaties(String soort) {
        ParkData.Soort s = ParkData.get().soort(soort);
        List<String> uit = new ArrayList<>();
        if (s != null) {
            for (String a : s.animaties()) {
                if (a.startsWith("bouw_")) {
                    uit.add(a);
                }
            }
        }
        return uit;
    }

    private Bouwopdracht() {
    }
}
