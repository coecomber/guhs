package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorData.Werk;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.taal.Tekst;

/**
 * The rules of the Guhkantoor (all server side; the screen only asks).
 * <ul>
 *   <li>A Bureautje connects to the nearest Prikklok within {@link #BEREIK} blocks that has room ({@link #MAX_BUREAUS}).</li>
 *   <li>{@link #klokIn}: one of YOUR guhs, standing near, goes to work: it is stored as data ({@link GuhOpslag},
 *   "Waar is mijn guh?": aan het werk op het Guhkantoor) and the desk shows a sleeping copy.</li>
 *   <li>Every {@link #DIENST} (8 real hours, {@link Klok}) since clocking in a loonstrookje waits at the clock, at most
 *   {@link #POSTVAK} per guh; with every third loonstrookje of a player comes a kwartaalrapport.</li>
 *   <li>The hours each guh slept are counted per real month; the guh with the most is the "Werknemer van de maand" and
 *   gets an oorkonde when the month is over.</li>
 *   <li>A guh comes back as the very same guh when it clocks out, or when its desk or clock is broken by anyone or
 *   anything; a record whose desk has vanished is released next to its owner ({@link #controleer}).</li>
 * </ul>
 * No muntjes, no useful output: only paper.
 */
public final class Kantoor {
    /** One shift: 8 real hours. */
    public static final long DIENST = 8 * Klok.UUR;
    public static final int MAX_BUREAUS = 4, POSTVAK = 3, BEREIK = 8, HOOGTE = 4;
    public static final double GUH_AFSTAND = 24, KLIK_AFSTAND = 10;
    /** The slice's name in PxData. */
    public static final String SLICE = "kantoor";
    private static final String G = "gui.guhs.guhkantoor.";
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static final int VERVERS = 0, KLOK_IN = 1, KLOK_UIT = 2, LOON = 3;

    // =====================================================================================================================
    // per-player data
    // =====================================================================================================================

    /** This player's Guhkantoor numbers: Loonstrookjes, Rapporten, Oorkondes, Maand, Guhs (per guh: Naam, Ms, Loon), Postvak. */
    public static CompoundTag data(MinecraftServer server, UUID speler) {
        return PxData.deel(server, speler, SLICE);
    }

    private static CompoundTag guhTag(CompoundTag data, UUID guh) {
        return PxData.sub(PxData.sub(data, "Guhs"), guh.toString());
    }

    public static int loonstrookjes(ServerPlayer p) {
        return data(p.level().getServer(), p.getUUID()).getIntOr("Loonstrookjes", 0);
    }

    public static int rapporten(ServerPlayer p) {
        return data(p.level().getServer(), p.getUUID()).getIntOr("Rapporten", 0);
    }

    public static int oorkondes(ServerPlayer p) {
        return data(p.level().getServer(), p.getUUID()).getIntOr("Oorkondes", 0);
    }

    private static ZonedDateTime tijd(long ms) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
    }

    static String datum(long ms) {
        return DATUM.format(tijd(ms));
    }

    /** year * 12 + month (0..11) of a Klok stamp, in the server's time zone. */
    public static int maandSleutel(long ms) {
        ZonedDateTime t = tijd(ms);
        return t.getYear() * 12 + t.getMonthValue() - 1;
    }

    public record Topper(UUID guh, Component naam, long ms) {
        public int uren() {
            return (int) (ms / Klok.UUR);
        }
    }

    /** The guh of this player that slept the longest this month (null: nobody slept yet). */
    @Nullable
    public static Topper topper(MinecraftServer server, UUID speler) {
        return topper(data(server, speler));
    }

    @Nullable
    private static Topper topper(CompoundTag data) {
        CompoundTag guhs = data.getCompoundOrEmpty("Guhs");
        Topper beste = null;
        for (String key : guhs.keySet()) {
            CompoundTag g = guhs.getCompoundOrEmpty(key);
            long ms = g.getLongOr("Ms", 0L);
            if (ms > 0 && (beste == null || ms > beste.ms())) {
                try {
                    beste = new Topper(UUID.fromString(key), Tekst.get(g, "Naam"), ms);
                } catch (IllegalArgumentException ignored) {
                    // (not a UUID: skip it)
                }
            }
        }
        return beste;
    }

    /**
     * Counts the hours of this player's working guhs up to now, and closes the month when a new real month began: the
     * longest sleeper gets an oorkonde (into the postvak), every counter goes back to zero.
     */
    public static void bijwerken(MinecraftServer server, UUID speler) {
        KantoorData kantoor = KantoorData.get(server);
        CompoundTag d = data(server, speler);
        long nu = Klok.nu();
        for (Werk w : kantoor.vanEigenaar(speler)) {
            long erbij = nu - w.geteldTot;
            if (erbij > 0) {
                CompoundTag g = guhTag(d, w.guh);
                g.putLong("Ms", g.getLongOr("Ms", 0L) + erbij);
                Tekst.put(g, "Naam", GuhOpslag.naam(w.opslag));
            }
            w.geteldTot = nu;
        }
        int maand = maandSleutel(nu), oud = d.getIntOr("Maand", -1);
        if (oud != maand) {
            Topper t = oud < 0 ? null : topper(d);
            if (t != null && t.ms() >= Klok.UUR) {
                ItemStack oorkonde = Papier.oorkonde(t.naam(), Tekst.get(d, "Baas"), Math.floorMod(oud, 12), Math.floorDiv(oud, 12), t.uren(), zaad());
                postvak(d).add(Papier.tag(oorkonde));
                d.putInt("Oorkondes", d.getIntOr("Oorkondes", 0) + 1);
            }
            CompoundTag guhs = d.getCompoundOrEmpty("Guhs");
            for (String key : guhs.keySet()) {
                guhs.getCompoundOrEmpty(key).putLong("Ms", 0L);
            }
            d.putInt("Maand", maand);
        }
        kantoor.setDirty();
        PxData.vuil(server);
    }

    private static ListTag postvak(CompoundTag data) {
        if (data.getList("Postvak").isEmpty()) {
            data.put("Postvak", new ListTag());
        }
        return data.getListOrEmpty("Postvak");
    }

    private static long zaad() {
        return ThreadLocalRandom.current().nextLong();
    }

    // =====================================================================================================================
    // desks and clocks
    // =====================================================================================================================

    private static boolean binnenBereik(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= BEREIK && Math.abs(a.getZ() - b.getZ()) <= BEREIK && Math.abs(a.getY() - b.getY()) <= HOOGTE;
    }

    /** The clock this desk really belongs to (both sides agree), or null. */
    @Nullable
    public static PrikklokBlockEntity klokVan(ServerLevel level, BlockPos bureau) {
        if (!(level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be) || be.klok() == null || !level.isLoaded(be.klok())) {
            return null;
        }
        return level.getBlockEntity(be.klok()) instanceof PrikklokBlockEntity klok && klok.bureaus().contains(bureau) ? klok : null;
    }

    /** Drops the desks that are gone or belong to another clock; returns the clock's desks. */
    public static List<BlockPos> schoon(ServerLevel level, PrikklokBlockEntity klok) {
        for (BlockPos bureau : klok.bureaus()) {
            if (level.isLoaded(bureau) && !(level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be && klok.getBlockPos().equals(be.klok()))) {
                klok.verwijder(bureau);
            }
        }
        return klok.bureaus();
    }

    /** Connects a desk to the nearest Prikklok with room. True when it is (or already was) connected. */
    public static boolean koppel(ServerLevel level, BlockPos bureau, @Nullable ServerPlayer meld) {
        if (!(level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be)) {
            return false;
        }
        if (klokVan(level, bureau) != null) {
            return true;
        }
        PrikklokBlockEntity beste = null;
        boolean vol = false;
        for (BlockPos p : BlockPos.betweenClosed(bureau.offset(-BEREIK, -HOOGTE, -BEREIK), bureau.offset(BEREIK, HOOGTE, BEREIK))) {
            if (level.isLoaded(p) && level.getBlockState(p).is(KantoorSlice.PRIKKLOK.get()) && level.getBlockEntity(p) instanceof PrikklokBlockEntity klok) {
                schoon(level, klok);
                if (klok.vol()) {
                    vol = true;
                } else if (beste == null || p.distSqr(bureau) < beste.getBlockPos().distSqr(bureau)) {
                    beste = klok;
                }
            }
        }
        if (beste == null) {
            be.zetKlok(null);
            if (meld != null) {
                meld.sendOverlayMessage(Component.translatable(G + (vol ? "meld.vol" : "meld.geen_klok")));
            }
            return false;
        }
        beste.voegToe(bureau);
        be.zetKlok(beste.getBlockPos());
        if (meld != null) {
            meld.sendOverlayMessage(Component.translatable(G + "meld.gekoppeld", beste.bureaus().size()));
        }
        return true;
    }

    /** A Prikklok was placed: the loose desks around it connect, nearest first. */
    public static void klokGeplaatst(ServerLevel level, BlockPos pos, @Nullable ServerPlayer meld) {
        if (!(level.getBlockEntity(pos) instanceof PrikklokBlockEntity klok)) {
            return;
        }
        List<BlockPos> los = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-BEREIK, -HOOGTE, -BEREIK), pos.offset(BEREIK, HOOGTE, BEREIK))) {
            if (level.isLoaded(p) && level.getBlockState(p).is(KantoorSlice.BUREAUTJE.get()) && klokVan(level, p) == null) {
                los.add(p.immutable());
            }
        }
        los.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));
        for (BlockPos p : los) {
            if (!klok.vol() && level.getBlockEntity(p) instanceof BureautjeBlockEntity be && klok.voegToe(p)) {
                be.zetKlok(pos);
            }
        }
        if (meld != null) {
            int n = klok.bureaus().size();
            meld.sendOverlayMessage(n == 0 ? Component.translatable(G + "meld.geen_bureaus") : Component.translatable(G + "meld.gekoppeld", n));
        }
    }

    /** The desk block goes away (broken, blown up, replaced): its guh goes home, its clock forgets it. */
    static void bureauWeg(ServerLevel level, BlockPos pos, BlockState state, BureautjeBlockEntity be) {
        klokUit(level, pos, state, null, true);
        if (be.klok() != null && level.isLoaded(be.klok()) && level.getBlockEntity(be.klok()) instanceof PrikklokBlockEntity klok) {
            klok.verwijder(pos);
        }
        be.vergeet();
    }

    /** The Prikklok goes away: every guh of its desks goes home, the desks are loose again. */
    static void klokWeg(ServerLevel level, BlockPos pos, PrikklokBlockEntity klok) {
        for (BlockPos bureau : klok.bureaus()) {
            if (level.isLoaded(bureau) && level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be && pos.equals(be.klok())) {
                klokUit(level, bureau, level.getBlockState(bureau), null, false);
                be.zetKlok(null);
            }
        }
    }

    // =====================================================================================================================
    // clocking in and out
    // =====================================================================================================================

    /** Puts one of the player's own guhs to work at this desk. Null = done; else why not. */
    @Nullable
    public static Component klokIn(ServerPlayer p, ServerLevel level, BlockPos bureau, UUID guhId) {
        MinecraftServer server = level.getServer();
        if (!(level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be)) {
            return Component.translatable(G + "meld.mislukt");
        }
        if (klokVan(level, bureau) == null) {
            return Component.translatable(G + "meld.geen_klok");
        }
        KantoorData kantoor = KantoorData.get(server);
        if (be.bezet() || kantoor.op(level.dimension(), bureau) != null) {
            return Component.translatable(G + "meld.bezet");
        }
        GuhEntity guh = GuhKiezer.zoek(p, guhId, GUH_AFSTAND);
        if (guh == null) {
            return Component.translatable(G + "meld.te_ver");
        }
        Component bezig = GuhKiezer.bezet(guh);
        if (bezig != null) {
            return bezig;
        }
        bijwerken(server, p.getUUID());
        Component naam = guh.getName().copy();
        Component baas = p.getName().copy();
        CompoundTag looks = Band.looks(guh);
        CompoundTag opslag = GuhOpslag.bewaar(guh, PlekSoort.OP_KANTOOR, Component.empty());
        GuhVolger.zet(p.getUUID(), guhId, new Plek(PlekSoort.OP_KANTOOR, level.dimension(), bureau, Component.empty(), level.getGameTime()));
        kantoor.zet(new Werk(guhId, p.getUUID(), opslag, level.dimension(), bureau, Klok.nu()));
        CompoundTag d = data(server, p.getUUID());
        Tekst.put(d, "Baas", baas);
        Tekst.put(guhTag(d, guhId), "Naam", naam);
        PxData.vuil(server);
        be.zet(guhId, p.getUUID(), naam, baas, looks);
        level.playSound(null, bureau, KantoorSlice.INKLOKKEN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        return null;
    }

    /** How many loonstrookjes wait for this guh (0..{@link #POSTVAK}). */
    public static int wachtend(Werk w) {
        return (int) Math.min(POSTVAK, Math.max(0L, (Klok.nu() - w.loonVanaf) / DIENST));
    }

    /**
     * Sends the guh at this desk home: the very same guh steps out next to the desk, with the loonstrookjes it earned.
     * door: the player who asked (null: the desk or clock went away); blokWeg: the desk block itself is being removed.
     * True when a guh came back.
     */
    public static boolean klokUit(ServerLevel level, BlockPos bureau, @Nullable BlockState state, @Nullable ServerPlayer door, boolean blokWeg) {
        MinecraftServer server = level.getServer();
        Werk w = KantoorData.get(server).op(level.dimension(), bureau);
        BureautjeBlockEntity be = !blokWeg && level.getBlockEntity(bureau) instanceof BureautjeBlockEntity b ? b : null;
        if (w == null) {
            if (be != null) {
                be.leeg();
            }
            return false;
        }
        Direction voor = state != null && state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        BlockPos plek = bureau.relative(voor);
        if (!level.getBlockState(plek).getCollisionShape(level, plek).isEmpty()) {
            plek = blokWeg ? bureau : bureau.above();
        }
        Component naam = GuhOpslag.naam(w.opslag);
        boolean terug = vrij(level, w, Vec3.atBottomCenterOf(plek), voor.toYRot());
        if (!terug) {
            return false;
        }
        if (be != null) {
            be.leeg();
        }
        level.playSound(null, bureau, KantoorSlice.UITKLOKKEN.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        ServerPlayer baas = server.getPlayerList().getPlayer(w.eigenaar);
        if (baas != null && baas != door) {
            baas.sendSystemMessage(Component.translatable(G + "meld.weg", naam));
        }
        return true;
    }

    /** Releases a stored guh at a spot, pays its finished shifts, deletes the record. False: it could not appear (the record stays). */
    private static boolean vrij(ServerLevel level, Werk w, Vec3 plek, float yaw) {
        MinecraftServer server = level.getServer();
        bijwerken(server, w.eigenaar);
        Entity e = GuhOpslag.laatVrij(level, w.opslag, plek, yaw);
        if (e == null && Band.zoekGeladen(server, w.guh) == null) {
            return false;   // (nothing appeared and it is not in the world either: keep it safe in the record)
        }
        List<ItemStack> loon = maakLoon(server, w);
        KantoorData.get(server).weg(w.guh);
        lever(server, w.eigenaar, loon);
        return true;
    }

    /** The loonstrookjes (and reports) of every finished shift of this guh that was not paid yet; moves the pay stamp. */
    private static List<ItemStack> maakLoon(MinecraftServer server, Werk w) {
        long diensten = Math.max(0L, (Klok.nu() - w.loonVanaf) / DIENST);
        if (diensten == 0) {
            return List.of();
        }
        int n = (int) Math.min(POSTVAK, diensten);
        long eerste = w.loonVanaf + (diensten - n) * DIENST;
        w.loonVanaf += diensten * DIENST;
        KantoorData.get(server).setDirty();
        CompoundTag d = data(server, w.eigenaar);
        CompoundTag g = guhTag(d, w.guh);
        Component naam = GuhOpslag.naam(w.opslag), baas = Tekst.get(d, "Baas");
        List<ItemStack> uit = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int nr = g.getIntOr("Loon", 0) + 1;
            g.putInt("Loon", nr);
            uit.add(Papier.loon(naam, baas, nr, zaad(), datum(eerste + (i + 1) * DIENST)));
            int totaal = d.getIntOr("Loonstrookjes", 0) + 1;
            d.putInt("Loonstrookjes", totaal);
            if (totaal % 3 == 0) {
                int kwartaal = d.getIntOr("Rapporten", 0) + 1;
                d.putInt("Rapporten", kwartaal);
                List<Component> medewerkers = new ArrayList<>();
                for (Werk collega : KantoorData.get(server).vanEigenaar(w.eigenaar)) {
                    if (medewerkers.size() < MAX_BUREAUS) {
                        medewerkers.add(GuhOpslag.naam(collega.opslag));
                    }
                }
                Topper t = topper(d);
                uit.add(Papier.kwartaal(baas, kwartaal, zaad(), medewerkers, t == null ? Component.empty() : t.naam()));
            }
        }
        PxData.vuil(server);
        return uit;
    }

    /** Papers for a player: into the pockets when online and not in a Guhpixel game, else into the postvak. */
    private static void lever(MinecraftServer server, UUID eigenaar, List<ItemStack> papieren) {
        if (papieren.isEmpty()) {
            return;
        }
        ServerPlayer p = server.getPlayerList().getPlayer(eigenaar);
        if (p != null && Sessies.van(p) == null) {
            geef(p, papieren);
            return;
        }
        ListTag vak = postvak(data(server, eigenaar));
        for (ItemStack s : papieren) {
            vak.add(Papier.tag(s));
        }
        PxData.vuil(server);
    }

    private static void geef(ServerPlayer p, List<ItemStack> papieren) {
        for (ItemStack s : papieren) {
            Minigames.give(p, s);
        }
        p.level().playSound(null, p.blockPosition(), KantoorSlice.PRINTER.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        mijlpalen(p);
    }

    /** The hidden advancements of the FTB quests follow the saved counters (so papers earned while offline count too). */
    private static void mijlpalen(ServerPlayer p) {
        if (loonstrookjes(p) > 0) {
            GuhAdvancements.grant(p, "guhkantoor_loonstrookje");
        }
        if (rapporten(p) > 0) {
            GuhAdvancements.grant(p, "guhkantoor_kwartaal");
        }
    }

    /** Hands over what waits in the postvak (papers earned while offline, the oorkonde of last month). True when there was post. */
    public static boolean bezorg(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        CompoundTag d = data(server, p.getUUID());
        ListTag vak = d.getListOrEmpty("Postvak");
        if (vak.isEmpty() || Sessies.van(p) != null) {
            return false;
        }
        List<ItemStack> papieren = new ArrayList<>();
        Component held = null;
        for (Tag raw : vak) {
            if (raw instanceof CompoundTag t) {
                papieren.add(Papier.stapel(t));
                if (Papier.soort(t) == Papier.Soort.OORKONDE) {
                    held = Tekst.get(t, "Naam");
                }
            }
        }
        d.put("Postvak", new ListTag());
        PxData.vuil(server);
        geef(p, papieren);
        p.sendSystemMessage(held != null ? Component.translatable(G + "meld.oorkonde", held) : Component.translatable(G + "meld.postvak"));
        return true;
    }

    /** Collects the loonstrookjes of the guh at this desk. Returns what to tell the player. */
    public static Component haalLoon(ServerPlayer p, ServerLevel level, BlockPos bureau) {
        MinecraftServer server = level.getServer();
        Werk w = KantoorData.get(server).op(level.dimension(), bureau);
        if (w == null) {
            return Component.translatable(G + "meld.nog_niks");
        }
        if (!w.eigenaar.equals(p.getUUID())) {
            return Component.translatable(G + "meld.niet_jouw");
        }
        bijwerken(server, p.getUUID());
        int rapporten = rapporten(p);
        List<ItemStack> loon = maakLoon(server, w);
        if (loon.isEmpty()) {
            return Component.translatable(G + "meld.nog_niks");
        }
        int nieuw = rapporten(p) - rapporten;
        geef(p, loon);
        Component tekst = Component.translatable(G + "meld.loon", loon.size() - nieuw, GuhOpslag.naam(w.opslag));
        return nieuw > 0 ? Component.empty().append(tekst).append(" ").append(Component.translatable(G + "meld.rapport")) : tekst;
    }

    /**
     * A record without its desk (the block vanished without telling, the dimension is gone): the guh steps out next to
     * its owner, wherever that is (not inside Guhpixel: guhs do not come there). Called now and then for online players.
     */
    public static void controleer(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (!p.isAlive() || Guhpixel.in(p) || !(p.level() instanceof ServerLevel hier)) {
            return;
        }
        for (Werk w : KantoorData.get(server).vanEigenaar(p.getUUID())) {
            ServerLevel level = server.getLevel(w.dim);
            if (level != null && !level.isLoaded(w.pos)) {
                continue;
            }
            boolean klopt = level != null && level.getBlockEntity(w.pos) instanceof BureautjeBlockEntity be && w.guh.equals(be.guh());
            if (!klopt) {
                Component naam = GuhOpslag.naam(w.opslag);
                if (vrij(hier, w, p.position(), p.getYRot())) {
                    p.sendSystemMessage(Component.translatable(G + "meld.terug", naam));
                }
            }
        }
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    /** A right-click on a desk: the screen of its Prikklok (a loose desk first looks for one). */
    public static void klikBureau(ServerPlayer p, ServerLevel level, BlockPos bureau) {
        if (!koppel(level, bureau, klokVan(level, bureau) == null ? p : null)) {
            return;
        }
        PrikklokBlockEntity klok = klokVan(level, bureau);
        if (klok != null) {
            open(p, level, klok.getBlockPos(), null);
        }
    }

    /** Opens the Prikklok screen for this player. */
    public static void open(ServerPlayer p, ServerLevel level, BlockPos klokPos, @Nullable Component melding) {
        stuur(p, level, klokPos, melding, true);
    }

    /** Sends the screen's data; open = false only refreshes a screen that is still open (the answer to an action). */
    private static void stuur(ServerPlayer p, ServerLevel level, BlockPos klokPos, @Nullable Component melding, boolean open) {
        if (!(level.getBlockEntity(klokPos) instanceof PrikklokBlockEntity klok)) {
            return;
        }
        bijwerken(level.getServer(), p.getUUID());
        bezorg(p);
        CompoundTag stand = stand(p, level, klok, melding);
        stand.putBoolean("Open", open);
        ModNetworking.sendTo(p, new KantoorPayloads.Stand(stand));
    }

    /** What the screen shows: the desks of this clock, the player's guhs to pick from, the Werknemer van de maand. */
    public static CompoundTag stand(ServerPlayer p, ServerLevel level, PrikklokBlockEntity klok, @Nullable Component melding) {
        MinecraftServer server = level.getServer();
        KantoorData kantoor = KantoorData.get(server);
        long nu = Klok.nu();
        CompoundTag t = new CompoundTag();
        t.putLong("Klok", klok.getBlockPos().asLong());
        ListTag bureaus = new ListTag();
        for (BlockPos bureau : schoon(level, klok)) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", bureau.asLong());
            Werk w = kantoor.op(level.dimension(), bureau);
            c.putBoolean("Bezet", w != null);
            if (w != null) {
                Tekst.put(c, "Naam", GuhOpslag.naam(w.opslag));
                if (level.getBlockEntity(bureau) instanceof BureautjeBlockEntity be) {
                    Tekst.put(c, "Baas", be.baas());
                }
                c.putBoolean("Mijn", w.eigenaar.equals(p.getUUID()));
                c.putLong("Verstreken", Math.max(0L, nu - w.sinds));
                c.putLong("Tot", DIENST - Math.floorMod(nu - w.loonVanaf, DIENST));
                c.putInt("Wachtend", wachtend(w));
            }
            bureaus.add(c);
        }
        t.put("Bureaus", bureaus);
        ListTag guhs = GuhKiezer.lijst(p, r -> true);
        for (Tag raw : guhs) {
            if (!(raw instanceof CompoundTag g)) {
                continue;
            }
            UUID id = g.read("Id", UUIDUtil.CODEC).orElse(null);
            String plek = g.getStringOr("PlekSoort", "");
            Component uit = null;
            if (plek.equals(PlekSoort.OP_KANTOOR.id()) || (id != null && kantoor.van(id) != null)) {
                uit = Component.translatable(G + "kiezer.werkt");
            } else if (plek.equals(PlekSoort.OP_VAKANTIE.id())) {
                uit = Component.translatable(G + "kiezer.vakantie");
            } else {
                GuhEntity guh = id == null ? null : GuhKiezer.zoek(p, id, GUH_AFSTAND);
                uit = guh == null ? Component.translatable(G + "kiezer.ver") : GuhKiezer.bezet(guh);
            }
            if (uit != null) {
                Tekst.put(g, "Uit", uit);
            }
        }
        t.put("Guhs", guhs);
        Topper top = topper(server, p.getUUID());
        if (top != null) {
            CompoundTag m = new CompoundTag();
            Tekst.put(m, "Naam", top.naam());
            m.putInt("Uren", top.uren());
            t.put("Maand", m);
        }
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        t.putInt("Grap", 1 + Math.floorMod(klok.getBlockPos().hashCode(), 3));
        return t;
    }

    /** What the screen asks: {@link #VERVERS}, {@link #KLOK_IN} (desk + guh), {@link #KLOK_UIT} (desk), {@link #LOON} (desk). Everything is checked here. */
    public static void actie(ServerPlayer p, BlockPos klokPos, int actie, int bureauNr, UUID guhId) {
        if (!(p.level() instanceof ServerLevel level) || !level.isLoaded(klokPos)
                || p.distanceToSqr(Vec3.atCenterOf(klokPos)) > KLIK_AFSTAND * KLIK_AFSTAND
                || !(level.getBlockEntity(klokPos) instanceof PrikklokBlockEntity klok)) {
            return;
        }
        List<BlockPos> bureaus = schoon(level, klok);
        Component melding = null;
        if (actie != VERVERS) {
            if (bureauNr < 0 || bureauNr >= bureaus.size()) {
                return;
            }
            BlockPos bureau = bureaus.get(bureauNr);
            Werk w = KantoorData.get(level.getServer()).op(level.dimension(), bureau);
            switch (actie) {
                case KLOK_IN -> {
                    melding = klokIn(p, level, bureau, guhId);
                    if (melding == null) {
                        Werk nieuw = KantoorData.get(level.getServer()).van(guhId);
                        melding = Component.translatable(G + "meld.ingeklokt", nieuw == null ? Component.literal("Guh") : GuhOpslag.naam(nieuw.opslag));
                    }
                }
                case KLOK_UIT -> {
                    if (w == null) {
                        break;
                    }
                    if (!w.eigenaar.equals(p.getUUID())) {
                        melding = Component.translatable(G + "meld.niet_jouw");
                        break;
                    }
                    Component naam = GuhOpslag.naam(w.opslag);
                    melding = klokUit(level, bureau, level.getBlockState(bureau), p, false)
                            ? Component.translatable(G + "meld.uitgeklokt", naam) : Component.translatable(G + "meld.mislukt");
                }
                case LOON -> melding = haalLoon(p, level, bureau);
                default -> {
                    return;
                }
            }
        }
        stuur(p, level, klokPos, melding, false);
    }

    private Kantoor() {
    }
}
