package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.piep.PiepMaatje;

import net.minecraft.core.UUIDUtil;
/**
 * All Guhhuisjes of the world (2.10; SavedData {@code guhs_huisjes} in the overworld, keyed by dimension + controller
 * block) and the resident rules: a resident is a band guh or a tamed maatje (muisje, Schilly, Poepschilly) of the
 * huisje's owner; a huisje holds at most {@link HuisjeMaat#plekken()} residents; one huisje per resident. A resident
 * carries its home in its persistent data ({@link #THUIS}, {@link #DIM}; {@link #BINNEN} while asleep inside).
 */
public final class Huisjes extends SavedData {
    public static final String NAAM = "guhs_huisjes";
    /** The home base (and the dome): this many blocks around the huisje, horizontally. */
    public static final int BEREIK = 16;
    public static final long NAAM_SALT = 20210202L;
    /** Persistent data keys on a resident. */
    public static final String THUIS = "guhs_huisje_thuis", DIM = "guhs_huisje_dim", BINNEN = "guhs_huisje_binnen";
    public static final int MAX_NAAM = 32;

    /** The cute names a new huisje can get (then numbered when they are all taken). */
    static final List<String> NAMEN = List.of("Knabbelkasteeltje", "Villa Vads", "Huize Njeg", "Het Pluisnestje", "Snoetjeshuis",
            "Oortjeshof", "Vadsig Paleisje", "Knuffelhoekje", "Kaasknabbelkot", "Guhlief", "Roze Wolkje", "Het Warme Snoetje",
            "Pootjeshuis", "Zoete Knabbelstee", "Villa Vahoeg", "Het Dikke Kussentje", "Huize Pluisoor", "Knabbelkoepeltje",
            "Snurkhuisje", "Het Lieve Vadsje", "Slaapsnoetje", "Njegnestje", "Knusse Kaaskamer", "Guhtje Thuis");

    /** 1.1.0: saved data type guhs:huisjes (the 1.0.0 file guhs_huisjes.dat is moved once). */
    static final net.minecraft.world.level.saveddata.SavedDataType<Huisjes> TYPE = nl.juiced.guhs.storage.GuhSavedData.tagType("huisjes",
            Huisjes::new, t -> load(t, null), h -> h.save(new CompoundTag(), null));

    private final Map<String, Huisje> huisjes = new LinkedHashMap<>();
    @Nullable
    private static Huisjes laatste;

    static Huisjes get(MinecraftServer server) {
        Huisjes h = nl.juiced.guhs.storage.GuhSavedData.get(server.overworld(), TYPE, NAAM);
        laatste = h;
        return h;
    }

    /** (Huisje.zetKlus) mark the data as changed. */
    static void dirty() {
        MinecraftServer s = Band.server();
        if (s != null) {
            get(s).setDirty();
        } else if (laatste != null) {
            laatste.setDirty();
        }
    }

    private static String sleutel(ResourceKey<Level> dim, BlockPos pos) {
        return dim.identifier() + "|" + pos.asLong();
    }

    // =====================================================================================================================
    // finding huisjes
    // =====================================================================================================================

    /** The huisje this block belongs to (the controller or any of its invisible parts), or null. */
    @Nullable
    public static Huisje van(ServerLevel level, BlockPos anyPartPos) {
        BlockState state = level.getBlockState(anyPartPos);
        BlockPos controller = anyPartPos;
        if (state.getBlock() instanceof HuisjeDeelBlock) {
            controller = HuisjeDeelBlock.controller(state, anyPartPos);
        }
        return get(level.getServer()).huisjes.get(sleutel(level.dimension(), controller));
    }

    /** The huisje at this controller position (no block check). */
    @Nullable
    public static Huisje op(MinecraftServer s, ResourceKey<Level> dim, BlockPos controller) {
        return get(s).huisjes.get(sleutel(dim, controller));
    }

    /** The huisje this resident lives in (checked against the huisje's list), or null. */
    @Nullable
    public static Huisje thuisVan(Entity bewoner) {
        MinecraftServer s = bewoner.level().getServer();
        CompoundTag data = bewoner.getPersistentData();
        if (s == null || !data.contains(THUIS)) {
            return null;
        }
        Identifier dim = Identifier.tryParse(data.getStringOr(DIM, ""));
        Huisje h = get(s).huisjes.get(sleutel(ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.identifier() : dim),
                BlockPos.of(data.getLongOr(THUIS, 0L))));
        return h != null && h.bewoners.contains(Band.id(bewoner)) ? h : null;
    }

    /** Lives in a huisje (its persistent data says so; cheap, works on both sides for the server's entities). */
    public static boolean isBewoner(Entity e) {
        return e.getPersistentData().contains(THUIS);
    }

    /** Asleep inside its huisje. */
    public static boolean isBinnen(Entity e) {
        return e.getPersistentData().getBooleanOr(BINNEN, false) || BandVlaggen.heeft(e, BandVlaggen.HUISJE_BINNEN);
    }

    public static List<Huisje> vanEigenaar(MinecraftServer s, UUID eigenaar) {
        return get(s).huisjes.values().stream().filter(h -> h.eigenaar.equals(eigenaar)).toList();
    }

    /** Huisjes whose controller is within r blocks of pos in this level. */
    public static List<Huisje> rond(ServerLevel level, BlockPos pos, int r) {
        List<Huisje> out = new ArrayList<>();
        for (Huisje h : get(level.getServer()).huisjes.values()) {
            if (h.dim.equals(level.dimension()) && h.pos.closerThan(pos, r)) {
                out.add(h);
            }
        }
        return out;
    }

    /** The huisje of an owner that this band id lives in, or null. */
    @Nullable
    public static Huisje vanBewoner(MinecraftServer s, UUID eigenaar, UUID bandId) {
        for (Huisje h : get(s).huisjes.values()) {
            if (h.eigenaar.equals(eigenaar) && h.bewoners.contains(bandId)) {
                return h;
            }
        }
        return null;
    }

    public static List<Huisje> alle(MinecraftServer s) {
        return List.copyOf(get(s).huisjes.values());
    }

    /** (tests) the huisjes of this data (e.g. after a save and load). */
    List<Huisje> huisjesVoorTest() {
        return List.copyOf(huisjes.values());
    }

    // =====================================================================================================================
    // building and breaking
    // =====================================================================================================================

    /** A new huisje was placed: registered with a cute unique name. */
    static Huisje registreer(ServerLevel level, BlockPos pos, Direction facing, HuisjeMaat maat, UUID eigenaar) {
        Huisjes data = get(level.getServer());
        Huisje h = new Huisje(level.dimension(), pos, facing, maat, eigenaar, data.nieuweNaam(pos));
        h.eigenaarNaam = naamVan(level.getServer(), eigenaar);
        data.huisjes.put(sleutel(level.dimension(), pos), h);
        data.setDirty();
        if (level.getBlockEntity(pos) instanceof HuisjeBlockEntity be) {
            be.zetEigenaar(eigenaar);
        }
        return h;
    }

    // =====================================================================================================================
    // 3.0: who owns it (only the owner, or an op, may change or break a huisje)
    // =====================================================================================================================

    /** May this player change this huisje (move guhs in/out, chores, the name) or break it: its owner, or an op (level 2). */
    public static boolean magBewerken(net.minecraft.world.entity.player.Player p, Huisje h) {
        return h.eigenaar().equals(p.getUUID()) || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    /** "Dit is het huisje van X" (gui.guhs.huisje.van_wie). */
    public static net.minecraft.network.chat.Component vanWie(Huisje h) {
        return net.minecraft.network.chat.Component.translatable("gui.guhs.huisje.van_wie",
                h.eigenaarNaam().isEmpty() ? "?" : h.eigenaarNaam(), h.naam());
    }

    /** A player's name by UUID (online, else the profile cache; "" when unknown). */
    static String naamVan(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        if (p != null) {
            return p.getGameProfile().name();
        }
        return s.services().nameToIdCache().get(id).map(net.minecraft.server.players.NameAndId::name).orElse("");
    }

    /** (tests, AutoCheck) sets the owner's name shown for a huisje. */
    public static void zetEigenaarNaam(MinecraftServer s, Huisje h, String naam) {
        h.eigenaarNaam = naam;
        get(s).setDirty();
    }

    /** (login) keeps the owner's name of this player's huisjes up to date. */
    static void naamBijwerken(ServerPlayer player) {
        Huisjes data = get(player.level().getServer());
        String naam = player.getGameProfile().name();
        for (Huisje h : data.huisjes.values()) {
            if (h.eigenaar().equals(player.getUUID()) && !naam.equals(h.eigenaarNaam)) {
                h.eigenaarNaam = naam;
                data.setDirty();
            }
        }
    }

    /** A huisje is gone: its residents come out and live freely again. */
    static void verwijder(ServerLevel level, BlockPos pos) {
        Huisjes data = get(level.getServer());
        Huisje h = data.huisjes.remove(sleutel(level.dimension(), pos));
        if (h == null) {
            return;
        }
        data.setDirty();
        for (UUID id : h.bewoners) {
            Entity e = zoekBewoner(level, h, id);
            if (e != null) {
                ontruim(e, h);
            }
        }
    }

    private String nieuweNaam(BlockPos pos) {
        Random rng = new Random(NAAM_SALT ^ pos.asLong() * 31);
        int start = rng.nextInt(NAMEN.size());
        for (int n = 1; ; n++) {
            for (int i = 0; i < NAMEN.size(); i++) {
                String naam = NAMEN.get((start + i) % NAMEN.size()) + (n == 1 ? "" : " " + n);
                if (!naamBezet(naam, null)) {
                    return naam;
                }
            }
        }
    }

    boolean naamBezet(String naam, @Nullable Huisje behalve) {
        String n = naam.strip().toLowerCase(Locale.ROOT);
        return huisjes.values().stream().anyMatch(h -> h != behalve && h.naam.toLowerCase(Locale.ROOT).equals(n));
    }

    /** Renames (the owner, from the screen): false when empty, too long or another huisje has that name. */
    public static boolean hernoem(MinecraftServer s, Huisje h, String naam) {
        String n = naam.strip();
        Huisjes data = get(s);
        if (n.isEmpty() || n.length() > MAX_NAAM || data.naamBezet(n, h)) {
            return false;
        }
        h.naam = n;
        data.setDirty();
        return true;
    }

    // =====================================================================================================================
    // residents
    // =====================================================================================================================

    /** Can this entity live in huisjes at all (a band guh or a tamed maatje)? */
    public static boolean kanBewoner(Entity e) {
        return Band.isBandGuh(e) || (e instanceof PiepMaatje && e instanceof TamableAnimal a && a.isTame() && a.getOwnerUUID() != null);
    }

    /**
     * Moves a guh or maatje into this huisje: false when it is full, not the owner's, or not a guh/maatje. A resident of
     * another huisje moves (out there, in here).
     */
    public static boolean trekIn(Huisje h, Entity bewoner) {
        MinecraftServer s = bewoner.level().getServer();
        if (s == null || !kanBewoner(bewoner) || !h.eigenaar.equals(Band.eigenaar(bewoner))) {
            return false;
        }
        UUID id = Band.id(bewoner);
        if (h.bewoners.contains(id)) {
            return true;
        }
        if (h.isVol()) {
            return false;
        }
        Huisje oud = thuisVan(bewoner);
        if (oud != null) {
            trekUit(bewoner);
        }
        Huisjes data = get(s);
        h.bewoners.add(id);
        h.soorten.put(id, bewoner instanceof PiepMaatje m ? m.soort() : "guh");
        h.namen.put(id, bewoner.getName().getString());
        data.setDirty();
        CompoundTag p = bewoner.getPersistentData();
        p.putLong(THUIS, h.pos.asLong());
        p.putString(DIM, h.dim.identifier().toString());
        p.remove(BINNEN);
        if (bewoner instanceof PathfinderMob mob) {
            Vec3 m = h.midden();
            mob.setHomeTo(BlockPos.containing(m), BEREIK);
            HuisjeGoal.zorgVoor(mob);
        }
        GuhVolger.zet(bewoner, PlekSoort.HUISJE, h.naam);
        bewoner.level().playSound(null, h.deur(), HuisjeFeature.DEUR_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        ServerPlayer owner = s.getPlayerList().getPlayer(h.eigenaar);
        if (bewoner instanceof Mob mob) {
            Band.moment(mob, owner, Moment.HUISJE_IN, h.naam);
        }
        if (owner != null) {
            owner.sendOverlayMessage(Component.translatable("gui.guhs.huisje.trekt_in", bewoner.getDisplayName(), h.naam)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            nl.juiced.guhs.quest.GuhAdvancements.grant(owner, "huisje_bewoner");
            if (h.isVol()) {
                GidsFeature.grant(owner, "lieve_vadsjes/huisje_vol");
            }
        }
        return true;
    }

    /** Moves out of its huisje (and out of the door when it was asleep inside). */
    public static void trekUit(Entity bewoner) {
        Huisje h = thuisVan(bewoner);
        if (h != null) {
            UUID id = Band.id(bewoner);
            h.bewoners.remove(id);
            h.soorten.remove(id);
            h.namen.remove(id);
            h.klussen.remove(id);
            get(bewoner.level().getServer()).setDirty();
        }
        ontruim(bewoner, h);
    }

    /** Takes a resident out of its huisje's list by band id (the screen's "Uit huis" for a resident that isn't loaded). */
    public static void trekUit(MinecraftServer s, Huisje h, UUID id) {
        ServerLevel level = s.getLevel(h.dim);
        Entity e = level == null ? null : zoekBewoner(level, h, id);
        if (e != null) {
            trekUit(e);
            return;
        }
        h.bewoners.remove(id);
        h.soorten.remove(id);
        h.namen.remove(id);
        h.klussen.remove(id);
        get(s).setDirty();
    }

    /** Clears an entity's home (its huisje is gone or it moved out): out of the door, free again. */
    static void ontruim(Entity e, @Nullable Huisje h) {
        if (isBinnen(e)) {
            naarBuiten(e, h, false);
        }
        CompoundTag p = e.getPersistentData();
        p.remove(THUIS);
        p.remove(DIM);
        p.remove(BINNEN);
        if (e instanceof PathfinderMob mob) {
            mob.clearHome();
        }
        if (Band.isBandGuh(e) || e instanceof PiepMaatje) {
            GuhVolger.zet(e, PlekSoort.WERELD, "");
        }
    }

    /** A loaded resident of this huisje by band id (guhs by UUID, maatjes by their band id nearby), or null. */
    @Nullable
    public static Entity zoekBewoner(ServerLevel level, Huisje h, UUID id) {
        Entity e = level.getEntity(id);
        if (e != null) {
            return e;
        }
        for (Entity m : level.getEntitiesOfClass(TamableAnimal.class, h.gebied().inflate(48), x -> x instanceof PiepMaatje)) {
            if (m.getPersistentData().read(Band.BAND_ID, UUIDUtil.CODEC).isPresent() && m.getPersistentData().read(Band.BAND_ID, UUIDUtil.CODEC).orElseThrow().equals(id)) {
                return m;
            }
        }
        return null;
    }

    // =====================================================================================================================
    // inside (asleep) and outside
    // =====================================================================================================================

    /** Goes inside through the door: hidden, no name, not pushable, zzz at the windows. */
    public static void naarBinnen(Entity e, Huisje h) {
        e.getPersistentData().putBoolean(BINNEN, true);
        BandVlaggen.zet(e, BandVlaggen.HUISJE_BINNEN, true);
        if (e instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        Vec3 m = h.midden();
        e.snapTo(m.x, h.pos.getY() + 0.1, m.z, e.getYRot(), e.getXRot());
        houdBinnen(e);
        e.level().playSound(null, h.deur(), HuisjeFeature.DEUR_GELUID.get(), SoundSource.NEUTRAL, 0.7f, 1f);
        GuhVolger.zet(e, PlekSoort.SLAAPT_IN_HUISJE, h.naam);
        if (e instanceof Mob mob) {
            Band.moment(mob, null, Moment.SLAAP, "");
        }
        ServerPlayer owner = e.level().getServer() == null ? null : e.level().getServer().getPlayerList().getPlayer(h.eigenaar);
        if (owner != null) {
            nl.juiced.guhs.quest.GuhAdvancements.grant(owner, "huisje_slapen");
        }
    }

    /** Every tick while inside: stay hidden, still and out of the way. */
    static void houdBinnen(Entity e) {
        e.noPhysics = true;
        if (!e.isNoGravity()) {
            e.setNoGravity(true);
        }
        if (!e.isInvisible()) {
            e.setInvisible(true);
        }
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
    }

    /** Comes out of the door (in the morning with a yawn). h may be null when the huisje is gone. */
    public static void naarBuiten(Entity e, @Nullable Huisje h, boolean gapen) {
        e.getPersistentData().remove(BINNEN);
        BandVlaggen.zet(e, BandVlaggen.HUISJE_BINNEN, false);
        e.noPhysics = false;
        e.setNoGravity(false);
        e.setInvisible(false);
        if (h != null) {
            BlockPos d = h.deur();
            e.snapTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, h.facing.toYRot(), 0);
            e.level().playSound(null, d, HuisjeFeature.DEUR_GELUID.get(), SoundSource.NEUTRAL, 0.7f, 1.1f);
            GuhVolger.zet(e, PlekSoort.HUISJE, h.naam);
        } else {
            BlockPos p = e.blockPosition();
            while (!e.level().getBlockState(p).isAir() && p.getY() < e.level().getMaxY() + 1) {
                p = p.above();
            }
            e.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, e.getYRot(), 0);
        }
        if (gapen && e instanceof GuhEntity guh) {
            guh.emotes.start(nl.juiced.guhs.feature.emotes.Emote.GAPEN, false, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF);
        }
        if (e instanceof Mob mob) {
            Band.moment(mob, null, Moment.WAKKER, "");
        }
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        huisjes.values().forEach(h -> list.add(h.save()));
        tag.put("Huisjes", list);
        return tag;
    }

    public static Huisjes load(CompoundTag tag, HolderLookup.Provider registries) {
        Huisjes d = new Huisjes();
        ListTag list = tag.getListOrEmpty("Huisjes");
        for (int i = 0; i < list.size(); i++) {
            Huisje h = Huisje.load(list.getCompoundOrEmpty(i));
            if (h != null) {
                d.huisjes.put(sleutel(h.dim, h.pos), h);
            }
        }
        return d;
    }
}
