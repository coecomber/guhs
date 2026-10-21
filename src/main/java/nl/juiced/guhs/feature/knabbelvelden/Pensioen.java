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
 *       guest of the Guhkamer) only goes when the player confirmed it ({@link Uitkomst#BEVESTIGEN}). Not: a picked-up guh
 *       (an item somewhere), one on holiday / at the office, one in the wolkjes.</li>
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
        for (BandData.Rec r : BandData.get(s).guhsVan(p.getUUID())) {
            if (r.dood || velden.vind(p.getUUID(), r.id) != null) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.store("Id", UUIDUtil.CODEC, r.id);
            t.putInt("Nr", GuhNummers.nummer(s, p.getUUID(), r.id));
            Tekst.put(t, "Naam", r.weergave());
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
        if (r == null || !r.guh) {
            return "niet_jouw";
        }
        if (r.dood) {
            return "dood";
        }
        if (Band.zoekGeladen(s, id) != null) {
            return null;
        }
        return switch (r.plek.soort()) {
            case ITEM_SPELER, ITEM_KIST, ITEM_RUGZAK, ITEM_BANK, ITEM_GROND -> "opgepakt";
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
        return switch (r.plek.soort()) {
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
        return ONDERWEG.containsKey(id);
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
        if (r == null || Band.zoekGeladen(s, id) != null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = PickedUpGuhItem.of(itemData(p, r));
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
