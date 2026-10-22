package nl.juiced.guhs.feature.knabbelvelden;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.UitItem;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.guhkamer.Guhkamer;
import nl.juiced.guhs.feature.guhkamer.GuhkamerData;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhNummers;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.taal.Tekst;

/**
 * Sending a tamed guh into retirement and bringing it home again (server side of the Knabbelportaal screen).
 * <ul>
 *   <li>{@link #stuur}: any tamed guh of the player, wherever it is: a loaded one goes at once ({@link GuhOpslag#bewaar}),
 *       one in a Guhrad is taken out of the wheel, an unloaded one is fetched first (its chunk loads, like "Roep naar mij":
 *       {@link Roepen}) and goes the moment it arrives. A busy guh (living in a huisje, running in a Guhrad, riding, a
 *       guest of the Guhkamer) only goes when the player confirmed it ({@link Uitkomst#BEVESTIGEN}). Not: one on holiday /
 *       at the office, one in the wolkjes. 1.6.2: a picked-up guh (an item in a chest, pockets, Bank Guh, backpack, on the
 *       ground) goes too, after a question: it is taken out first ({@link UitItem}, the "Roep naar mij" way). And every
 *       maatje of "Mijn andere vadsjes" can retire as well ({@link Vadsjes}): it is kept as its own pick-up item.</li>
 *   <li>{@link #terug}: the guh comes home as a picked-up guh item (everything kept: number, name, clothes, personality,
 *       fur...), into your pockets, or on the ground at the portal when they are full. Never doubled: no item while a guh
 *       with its UUID is in the world, and the record goes only after the item is made.</li>
 * </ul>
 * In the Guhdex it says "Met pensioen in De Eeuwige Knabbelvelden" ({@link PlekSoort#IN_DE_KNABBELVELDEN}).
 */
public final class Pensioen {
    public enum Uitkomst { OK, BEVESTIGEN, ONDERWEG, NIET_JOUW, NIET_NU, KWIJT }

    /** The screen's actions (KnabbelPayloads.Actie). */
    public static final int STUUR = 0, HAAL = 1, NAAR_VELDEN = 2, NAAR_HUIS = 3;
    /** How close to the portal you must stand. */
    public static final double BEREIK = 8;
    /** How long we wait for a guh that is being fetched (Roepen gives up after 1200 ticks). */
    static final int WACHT_MAX = 1400;

    private record Wacht(UUID speler, int[] ticks) {
    }

    /** Guhs being fetched before they go (band id -> who sent them). */
    private static final Map<UUID, Wacht> ONDERWEG = new ConcurrentHashMap<>();
    /** Which portal each player has the screen of open. */
    private static final Map<UUID, BlockPos> OPEN = new ConcurrentHashMap<>();

    private Pensioen() {
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    public static void open(ServerPlayer p, BlockPos portaal) {
        OPEN.put(p.getUUID(), portaal.immutable());
        ModNetworking.sendTo(p, new KnabbelPayloads.Open(data(p, portaal, null)));
    }

    static void ververs(ServerPlayer p, @Nullable Component melding) {
        BlockPos portaal = OPEN.get(p.getUUID());
        if (portaal != null) {
            ModNetworking.sendTo(p, new KnabbelPayloads.Open(data(p, portaal, melding)));
        }
    }

    static boolean bijPortaal(ServerPlayer p, BlockPos portaal) {
        return p.level().getBlockState(portaal).is(KnabbelveldenFeature.PORTAAL.get())
                && p.position().distanceToSqr(Vec3.atCenterOf(portaal)) <= BEREIK * BEREIK;
    }

    /** What the screen shows: your guhs at home (with why one is busy or can't go) and your guhs in the Knabbelvelden. */
    public static CompoundTag data(ServerPlayer p, BlockPos portaal, @Nullable Component melding) {
        MinecraftServer s = p.level().getServer();
        KnabbelveldenData velden = KnabbelveldenData.get(s);
        CompoundTag root = new CompoundTag();
        root.putLong("Pos", portaal.asLong());
        root.putBoolean("InVelden", Knabbelvelden.echt(p.level()));
        root.putLong("Dag", Band.dag(s));
        if (melding != null) {
            Tekst.put(root, "Melding", melding);
        }
        ListTag thuis = new ListTag();
        for (BandData.Rec r : BandData.get(s).van(p.getUUID())) {
            if (r.dood || velden.vind(p.getUUID(), r.id) != null) {
                continue;
            }
            if (!r.guh && (r.plek == null || r.plek.soort() == PlekSoort.ONBEKEND)) {
                continue;   // (a maatje that died: like "Mijn andere vadsjes")
            }
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, r.id);
            t.putInt("Nr", r.guh ? GuhNummers.nummer(s, p.getUUID(), r.id) : 0);
            t.putString("Soort", r.guh ? "guh" : r.soort);
            Tekst.put(t, "Naam", r.guh ? r.weergave() : !nl.juiced.guhs.taal.Tekst.empty(r.naam) ? r.naam
                    : Component.translatable("entity.guhs." + r.soort));
            if (r.plek != null) {
                Tekst.put(t, "Plek", GuhVolger.tekst(r.plek));
            }
            t.put("Looks", r.looks.copy());
            t.putLong("Sinds", r.sindsDag);
            String bezig = bezig(s, p.getUUID(), r.id), niet = nietNu(s, p.getUUID(), r.id);
            t.putString("Bezig", bezig == null ? "" : bezig);
            t.putString("Niet", niet == null ? "" : niet);
            t.putBoolean("Onderweg", ONDERWEG.containsKey(r.id));
            thuis.add(t);
        }
        root.put("Thuis", thuis);
        ListTag hier = new ListTag();
        for (KnabbelveldenData.Rust r : velden.van(p.getUUID())) {
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, r.id());
            t.putInt("Nr", r.nr());
            t.putString("Soort", r.soort());
            Tekst.put(t, "Naam", GuhOpslag.naam(r.opslag()));
            t.put("Looks", r.opslag().getCompoundOrEmpty("Looks").copy());
            t.putLong("Sinds", r.sindsDag());
            hier.add(t);
        }
        root.put("Velden", hier);
        return root;
    }

    /** (KnabbelPayloads.Actie) a button of the screen. */
    static void actie(ServerPlayer p, BlockPos portaal, int soort, List<UUID> ids, boolean bevestigd) {
        BlockPos open = OPEN.get(p.getUUID());
        if (open == null || !open.equals(portaal) || !bijPortaal(p, portaal)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.knabbelvelden.te_ver").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        switch (soort) {
            case NAAR_VELDEN -> {
                OPEN.remove(p.getUUID());
                Knabbelvelden.naar(p);
            }
            case NAAR_HUIS -> {
                OPEN.remove(p.getUUID());
                Knabbelvelden.terug(p);
            }
            case STUUR -> {
                int ok = 0, onderweg = 0, niet = 0;
                for (UUID id : ids) {
                    switch (stuur(p, id, bevestigd)) {
                        case OK -> ok++;
                        case ONDERWEG -> onderweg++;
                        default -> niet++;
                    }
                }
                ververs(p, ok + onderweg == 0 ? Component.translatable("gui.guhs.knabbelvelden.melding.niks")
                        : Component.translatable("gui.guhs.knabbelvelden.melding.weg", ok + onderweg)
                        .append(niet > 0 ? Component.translatable("gui.guhs.knabbelvelden.melding.niet", niet) : Component.empty()));
            }
            case HAAL -> {
                int ok = 0;
                for (UUID id : ids) {
                    if (!terug(p, id, Vec3.atCenterOf(portaal).add(0, 0.2, 0)).isEmpty()) {
                        ok++;
                    }
                }
                ververs(p, Component.translatable(ok == 0 ? "gui.guhs.knabbelvelden.melding.niks" : "gui.guhs.knabbelvelden.melding.thuis", ok));
            }
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // why not / busy
    // =====================================================================================================================

    /** Why this guh can't go now (a lang suffix: gui.guhs.knabbelvelden.niet.X), or null when it can. */
    @Nullable
    public static String nietNu(MinecraftServer s, UUID eigenaar, UUID id) {
        if (KnabbelveldenData.get(s).vind(eigenaar, id) != null) {
            return "al";
        }
        BandData.Rec r = BandData.get(s).vind(eigenaar, id);
        if (r == null) {
            return "niet_jouw";
        }
        if (r.dood) {
            return "dood";
        }
        if (r.guh ? Band.zoekGeladen(s, id) != null : Vadsjes.zoekGeladen(s, id) != null) {
            return null;
        }
        return switch (r.plek.soort()) {
            case OP_VAKANTIE, OP_KANTOOR -> "weg";
            case IN_DE_WOLKJES -> "dood";
            case ONBEKEND -> "kwijt";
            default -> null;
        };
    }

    /** Why this guh is busy (gui.guhs.knabbelvelden.bezig.X: then you are asked first), or null. */
    @Nullable
    public static String bezig(MinecraftServer s, UUID eigenaar, UUID id) {
        Entity e = Band.zoekGeladen(s, id);
        if (e instanceof GuhEntity guh) {
            if (Huisjes.isBewoner(guh)) {
                return "huisje";
            }
            if (Guhkamer.isGast(guh)) {
                return "guhkamer";
            }
            if (guh.isPassenger() || guh.isVehicle()) {
                return "rijdt";
            }
            return null;
        }
        BandData.Rec r = BandData.get(s).vind(eigenaar, id);
        if (r == null) {
            return null;
        }
        if (!r.guh) {
            return Roepen.isItem(r.plek.soort()) ? "opgepakt" : null;
        }
        return switch (r.plek.soort()) {
            case ITEM_SPELER, ITEM_KIST, ITEM_RUGZAK, ITEM_BANK, ITEM_GROND -> "opgepakt";   // 1.6.2: taken out first
            case HUISJE, SLAAPT_IN_HUISJE -> "huisje";
            case GUHWIEL -> "guhwiel";
            case GUHKAMER -> "guhkamer";
            case RIJDT_OP, RIJDT_MEE -> "rijdt";
            default -> Huisjes.vanBewoner(s, eigenaar, id) != null ? "huisje" : null;
        };
    }

    // =====================================================================================================================
    // sending
    // =====================================================================================================================

    /** Sends one guh (see the class doc). The player gets no message here; the screen tells (the action's line). */
    public static Uitkomst stuur(ServerPlayer p, UUID id, boolean bevestigd) {
        MinecraftServer s = p.level().getServer();
        BandData.Rec rec = BandData.get(s).vind(p.getUUID(), id);
        if (rec != null && !rec.guh) {
            return stuurMaatje(p, rec, bevestigd);
        }
        Entity e = Band.zoekGeladen(s, id);
        if (e != null && !(e instanceof GuhEntity g && Band.isBandGuh(g) && p.getUUID().equals(g.getOwnerUUID()))) {
            return Uitkomst.NIET_JOUW;
        }
        String niet = nietNu(s, p.getUUID(), id);
        if (niet != null) {
            return niet.equals("niet_jouw") ? Uitkomst.NIET_JOUW : niet.equals("kwijt") ? Uitkomst.KWIJT : Uitkomst.NIET_NU;
        }
        if (!bevestigd && bezig(s, p.getUUID(), id) != null) {
            return Uitkomst.BEVESTIGEN;
        }
        if (ONDERWEG.containsKey(id)) {
            return Uitkomst.ONDERWEG;
        }
        if (e instanceof GuhEntity guh) {
            bewaar(p.getUUID(), guh);
            return Uitkomst.OK;
        }
        BandData.Rec r = BandData.get(s).vind(p.getUUID(), id);
        if (r != null && r.plek.soort() == PlekSoort.GUHWIEL) {
            return uitHetWiel(s, p.getUUID(), r) ? Uitkomst.OK : Uitkomst.NIET_NU;
        }
        Roepen.Uitkomst u = Roepen.roep(p, id);
        if (u == Roepen.Uitkomst.OK) {
            Entity n = Band.zoekGeladen(s, id);
            if (n instanceof GuhEntity guh) {
                bewaar(p.getUUID(), guh);
                return Uitkomst.OK;
            }
            return Uitkomst.KWIJT;
        }
        if (u == Roepen.Uitkomst.BEZIG) {
            ONDERWEG.put(id, new Wacht(p.getUUID(), new int[1]));
            return Uitkomst.ONDERWEG;
        }
        return u == Roepen.Uitkomst.NIET_JOUW ? Uitkomst.NIET_JOUW : u == Roepen.Uitkomst.KWIJT ? Uitkomst.KWIJT : Uitkomst.NIET_NU;
    }

    /** A maatje waiting to be fetched: its place (loaded with the "Roep naar mij" ticket) and how long we wait. */
    private record MaatjeWacht(UUID speler, ServerLevel level, BlockPos pos, PlekSoort soort, int[] ticks) {
    }

    private static final Map<UUID, MaatjeWacht> MAATJES = new ConcurrentHashMap<>();

    /**
     * 1.6.2: one of "Mijn andere vadsjes" retires: a loaded one at once, a picked-up one out of the pockets / chest / Bank
     * Guh / backpack / ground, an unloaded one when its chunk is there (we wait a while, like "Roep naar mij").
     */
    static Uitkomst stuurMaatje(ServerPlayer p, BandData.Rec r, boolean bevestigd) {
        MinecraftServer s = p.level().getServer();
        String niet = nietNu(s, p.getUUID(), r.id);
        if (niet != null) {
            return niet.equals("kwijt") ? Uitkomst.KWIJT : niet.equals("niet_jouw") ? Uitkomst.NIET_JOUW : Uitkomst.NIET_NU;
        }
        if (!bevestigd && bezig(s, p.getUUID(), r.id) != null) {
            return Uitkomst.BEVESTIGEN;
        }
        if (MAATJES.containsKey(r.id)) {
            return Uitkomst.ONDERWEG;
        }
        net.minecraft.world.entity.TamableAnimal dier = Vadsjes.zoekGeladen(s, r.id);
        if (dier != null) {
            if (!p.getUUID().equals(Band.eigenaar(dier))) {
                return Uitkomst.NIET_JOUW;
            }
            Vadsjes.bewaar(p.getUUID(), dier);
            return Uitkomst.OK;
        }
        if (Roepen.isItem(r.plek.soort())) {
            ItemStack item = UitItem.uitSpelers(s, p.getUUID(), r.id);
            if (item != null) {
                Vadsjes.bewaarItem(s, p.getUUID(), r.id, item, p.level().dimension(), p.blockPosition());
                return Uitkomst.OK;
            }
        }
        ServerLevel level = s.getLevel(r.plek.dim());
        if (level == null || r.plek.soort() == PlekSoort.ONBEKEND) {
            return Uitkomst.KWIJT;
        }
        level.getChunkSource().addTicketWithRadius(Roepen.ticket(), net.minecraft.world.level.ChunkPos.containing(r.plek.pos()), Roepen.TICKET_RADIUS);
        MAATJES.put(r.id, new MaatjeWacht(p.getUUID(), level, r.plek.pos(), r.plek.soort(), new int[1]));
        return Uitkomst.ONDERWEG;
    }

    /** Every tick: a maatje we are fetching: is its chunk (and it, or its item) there yet? */
    private static void tickMaatjes(MinecraftServer s) {
        for (Iterator<Map.Entry<UUID, MaatjeWacht>> it = MAATJES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, MaatjeWacht> e = it.next();
            UUID id = e.getKey();
            MaatjeWacht w = e.getValue();
            w.ticks()[0]++;
            ServerPlayer p = s.getPlayerList().getPlayer(w.speler());
            boolean klaar = false;
            if (w.ticks()[0] % 10 == 0 && w.level().isLoaded(w.pos())) {
                net.minecraft.world.entity.TamableAnimal dier = Vadsjes.zoekGeladen(s, id);
                if (dier != null && w.speler().equals(Band.eigenaar(dier))) {
                    Vadsjes.bewaar(w.speler(), dier);
                    klaar = true;
                } else if (Roepen.isItem(w.soort())) {
                    ItemStack item = UitItem.uitWereld(w.level(), w.pos(), w.soort(), id);
                    if (item == null) {
                        item = UitItem.uitSpelers(s, w.speler(), id);
                    }
                    if (item != null) {
                        Vadsjes.bewaarItem(s, w.speler(), id, item, w.level().dimension(), w.pos());
                        klaar = true;
                    }
                }
            }
            if (klaar || p == null || w.ticks()[0] > WACHT_MAX) {
                it.remove();
                w.level().getChunkSource().removeTicketWithRadius(Roepen.ticket(), net.minecraft.world.level.ChunkPos.containing(w.pos()), Roepen.TICKET_RADIUS);
                if (p != null) {
                    ververs(p, klaar ? null : Component.translatable("gui.guhs.knabbelvelden.melding.kwijt"));
                }
            }
        }
    }

    /** A loaded guh of this owner goes: out of the Guhkamer and its huisje, stored, a poof of pink cheese dust. */
    static void bewaar(UUID eigenaar, GuhEntity guh) {
        ServerLevel level = (ServerLevel) guh.level();
        MinecraftServer s = level.getServer();
        UUID id = guh.getUUID();
        if (Guhkamer.isGast(guh)) {
            GuhkamerData kamers = GuhkamerData.get(s);
            var kamer = kamers.vanGast(id);
            if (kamer != null) {
                kamer.gasten.remove(id);
                kamers.setDirty();
            }
            Guhkamer.markeer(guh, false);
        }
        level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 14, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, guh.getX(), guh.getY() + 0.8, guh.getZ(), 10, 0.3, 0.5, 0.3, 0.04);
        level.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        int nr = GuhNummers.nummer(s, eigenaar, id);
        CompoundTag opslag = GuhOpslag.bewaar(guh, PlekSoort.IN_DE_KNABBELVELDEN, Component.translatable("gui.guhs.knabbelvelden.naam"));
        KnabbelveldenData.get(s).zet(eigenaar, new KnabbelveldenData.Rust(opslag, Band.dag(s), nr));
    }

    /** A guh running in a Guhrad: taken out of the wheel (its chunk is loaded for a moment) and stored. */
    private static boolean uitHetWiel(MinecraftServer s, UUID eigenaar, BandData.Rec r) {
        ServerLevel level = s.getLevel(r.plek.dim());
        if (level == null) {
            return false;
        }
        BlockPos pos = r.plek.pos();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (!(level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wiel) || !wiel.hasGuh()) {
            return false;
        }
        CompoundTag data = wiel.takeOut();
        if (data == null || !r.id.equals(data.read("UUID", UUIDUtil.CODEC).orElse(null))) {
            if (data != null) {
                wiel.insert(data);   // (another guh runs there now: leave it)
            }
            return false;
        }
        if (level.getBlockState(pos).hasProperty(GuhWheelBlock.RUNNING)) {
            level.setBlock(pos, level.getBlockState(pos).setValue(GuhWheelBlock.RUNNING, false), 3);
        }
        CompoundTag opslag = new CompoundTag();
        opslag.store("Id", UUIDUtil.CODEC, r.id);
        opslag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        Tekst.put(opslag, "Naam", r.weergave());
        opslag.put("Looks", r.looks.copy());
        opslag.putString("Soort", PlekSoort.IN_DE_KNABBELVELDEN.id());
        CompoundTag d = data.copy();
        if (!d.contains("id")) {
            d.putString("id", nl.juiced.guhs.Guhs.id("guh").toString());
        }
        d.putBoolean("Sitting", false);
        opslag.put("Data", d);
        GuhVolger.zet(eigenaar, r.id, new Plek(PlekSoort.IN_DE_KNABBELVELDEN, level.dimension(), pos,
                Component.translatable("gui.guhs.knabbelvelden.naam"), level.getGameTime()));
        KnabbelveldenData.get(s).zet(eigenaar, new KnabbelveldenData.Rust(opslag, Band.dag(s), GuhNummers.nummer(s, eigenaar, r.id)));
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
        return true;
    }

    /** Every tick: is a guh we are fetching here yet? Then it goes. */
    static void tick(ServerTickEvent.Post event) {
        if (!MAATJES.isEmpty()) {
            tickMaatjes(event.getServer());
        }
        if (ONDERWEG.isEmpty()) {
            return;
        }
        MinecraftServer s = event.getServer();
        for (Iterator<Map.Entry<UUID, Wacht>> it = ONDERWEG.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Wacht> e = it.next();
            UUID id = e.getKey();
            Wacht w = e.getValue();
            w.ticks()[0]++;
            ServerPlayer p = s.getPlayerList().getPlayer(w.speler());
            Entity guh = Band.zoekGeladen(s, id);
            if (guh instanceof GuhEntity g && !Roepen.bezig(id) && Band.isBandGuh(g) && w.speler().equals(g.getOwnerUUID())) {
                it.remove();
                bewaar(w.speler(), g);
                if (p != null) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.knabbelvelden.aangekomen", GuhOpslag.naam(KnabbelveldenData.get(s)
                            .vind(w.speler(), id).opslag())).withStyle(ChatFormatting.LIGHT_PURPLE));
                    ververs(p, null);
                }
            } else if (p == null || (!Roepen.bezig(id) && guh == null) || w.ticks()[0] > WACHT_MAX) {
                it.remove();
                if (p != null) {
                    ververs(p, Component.translatable("gui.guhs.knabbelvelden.melding.kwijt"));
                }
            }
        }
    }

    /** (tests) is this guh being fetched? */
    public static boolean onderweg(UUID id) {
        return ONDERWEG.containsKey(id) || MAATJES.containsKey(id);
    }

    // =====================================================================================================================
    // bringing home
    // =====================================================================================================================

    /**
     * Brings a guh home as a picked-up guh item: into the player's pockets, or (full) on the ground at drop (else at the
     * player). Returns the item (empty: it isn't in the Knabbelvelden, or a guh with its UUID is in the world).
     */
    public static ItemStack terug(ServerPlayer p, UUID id, @Nullable Vec3 drop) {
        MinecraftServer s = p.level().getServer();
        KnabbelveldenData velden = KnabbelveldenData.get(s);
        KnabbelveldenData.Rust r = velden.vind(p.getUUID(), id);
        if (r == null || (r.isMaatje() ? Vadsjes.zoekGeladen(s, id) != null : Band.zoekGeladen(s, id) != null)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = r.isMaatje() ? Vadsjes.item(s, r.opslag()) : PickedUpGuhItem.of(itemData(p, r));
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;   // (can't be read: keep the record, nothing is lost)
        }
        velden.haalWeg(p.getUUID(), id);
        Geesten.weg(s, id);
        ItemStack kopie = stack.copy();
        if (p.getInventory().add(stack)) {
            GuhVolger.item(kopie, PlekSoort.ITEM_SPELER, p.level().dimension(), p.blockPosition(), p.getGameProfile().name(), p.level().getGameTime());
        } else {
            Vec3 plek = drop == null ? p.position() : drop;
            ItemEntity item = new ItemEntity(p.level(), plek.x, plek.y, plek.z, stack);
            item.setDefaultPickUpDelay();
            item.setDeltaMovement(Vec3.ZERO);
            p.level().addFreshEntity(item);
            GuhVolger.item(kopie, PlekSoort.ITEM_GROND, p.level().dimension(), item.blockPosition(), "", p.level().getGameTime());
            p.sendOverlayMessage(Component.translatable("gui.guhs.knabbelvelden.zakken_vol", GuhOpslag.naam(r.opslag()))
                    .withStyle(ChatFormatting.GOLD));
        }
        p.level().playSound(null, p.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1f);
        return kopie;
    }

    /** The item's guh data: the stored entity, free again (no huisje, no claim, standing), with the tooltip's extras. */
    static CompoundTag itemData(ServerPlayer p, KnabbelveldenData.Rust r) {
        CompoundTag t = r.opslag().getCompoundOrEmpty("Data").copy();
        CompoundTag nf = t.getCompoundOrEmpty("NeoForgeData");
        boolean binnen = nf.getBooleanOr(Huisjes.BINNEN, false);
        nf.remove(Huisjes.THUIS);
        nf.remove(Huisjes.DIM);
        nf.remove(Huisjes.BINNEN);
        nf.remove(GuhKiezer.BEZET);
        t.put("NeoForgeData", nf);
        if (binnen) {
            t.putBoolean("NoGravity", false);
            t.putInt("KnusVlaggen", t.getIntOr("KnusVlaggen", 0) & ~BandVlaggen.HUISJE_BINNEN);
        }
        t.remove("Passengers");
        t.putBoolean("Sitting", false);
        CompoundTag looks = r.opslag().getCompoundOrEmpty("Looks");
        if (looks.contains("Naam")) {
            Tekst.put(t, "GuhDisplayName", Tekst.get(looks, "Naam"));
        }
        t.putString(PickedUpGuhItem.EIGENAAR, p.getUUID().toString());
        t.putString(PickedUpGuhItem.EIGENAAR_NAAM, p.getGameProfile().name());
        if (r.nr() > 0) {
            t.putInt(PickedUpGuhItem.NUMMER, r.nr());
        }
        t.putInt(PickedUpGuhItem.HARTJES, Band.hartjes(p.level().getServer(), p.getUUID(), r.id()));
        return t;
    }

    static void vergeet(UUID speler) {
        OPEN.remove(speler);
    }

    static void stopAlles() {
        ONDERWEG.clear();
        for (MaatjeWacht w : MAATJES.values()) {
            w.level().getChunkSource().removeTicketWithRadius(Roepen.ticket(), net.minecraft.world.level.ChunkPos.containing(w.pos()), Roepen.TICKET_RADIUS);
        }
        MAATJES.clear();
        OPEN.clear();
    }

    /** (tests) the guhs of this player in the Knabbelvelden. */
    public static List<UUID> inVelden(ServerPlayer p) {
        List<UUID> uit = new ArrayList<>();
        for (KnabbelveldenData.Rust r : KnabbelveldenData.get(p.level().getServer()).van(p.getUUID())) {
            uit.add(r.id());
        }
        return uit;
    }
}
