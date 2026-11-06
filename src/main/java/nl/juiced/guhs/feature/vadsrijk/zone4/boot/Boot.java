package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-Z4BOOT: the player's own guh sailboat in zone 4 (decision W-2). The boat is a mount soort ({@link RijdierSoort#ZEILBOOTJE},
 * {@code water() = true}): it is in the mount screen (N) with its level, the mount key (X) calls it, sailing gives it xp. This
 * class holds the rest:
 * <ul>
 *   <li><b>getting it:</b> {@link #geef} (zone-4 story, t-z4verhaal; also {@code /guhs vadsrijk boot geef}); it sets the quest flag
 *       {@link #gekregen} and fires {@link #GEKREGEN};</li>
 *   <li><b>which mount the key calls</b> ({@link #kiesSoort}, a hook in Rijdieren.toets): in the water or on a dock of the zone-4
 *       sea = the boat, on land = your land mount (also when the boat is the chosen one);</li>
 *   <li><b>putting it on the water</b> ({@link #plaats}, a hook in Rijdieren.roep): the nearest free water within 6 blocks;</li>
 *   <li><b>docks</b> ({@link BootZee}): sailing past one remembers it; Shift next to one = moor on its planks;</li>
 *   <li><b>logging out at sea</b>: you come back on the last dock ({@link #ingelogd});</li>
 *   <li><b>looks</b> ({@link BootVariant}): sail colour, pennant, figurehead; own, choose, buy ({@link #kies}, {@link #koop},
 *       {@link #geefVariant}).</li>
 * </ul>
 * Stored per player in {@code SpelerData.extra("boot")}: {@code gekregen}, {@code varianten} ("zeil_mint,boeg_draakje"), the chosen
 * {@code zeil}/{@code wimpel}/{@code boeg}, {@code steiger} (last dock id) and {@code opzee} (riding the boat right now).
 */
public final class Boot {
    public static final String SLICE = "boot";
    /** Story hook: fires once when a player gets the boat (t-z4verhaal can advance its quest here). */
    public static final List<Consumer<ServerPlayer>> GEKREGEN = new CopyOnWriteArrayList<>();
    /** Dev: players who may sail the boat anywhere on water ({@code /guhs vadsrijk boot proefvaart}); not saved. */
    static final Set<UUID> PROEF = ConcurrentHashMap.newKeySet();
    /** How far (blocks) the key looks for water to put the boat on. */
    public static final int ZOEK = 6;

    private Boot() {
    }

    static CompoundTag tag(ServerPlayer p) {
        return Spelers.data(p).extra(SLICE);
    }

    // ---- getting it --------------------------------------------------------------------------------------------------------------

    public static boolean heeft(ServerPlayer p) {
        return Rijdieren.heeft(p, RijdierSoort.ZEILBOOTJE);
    }

    /** The quest flag: the story gave you the boat (true also after a dev geef). */
    public static boolean gekregen(ServerPlayer p) {
        return tag(p).getBooleanOr("gekregen", false);
    }

    /**
     * API (t-z4verhaal, early in the zone-4 story): gives the player their own Zeilbootje (mount level 1, the start looks) and
     * sets the quest flag. False when they already had it (the flag is set anyway).
     */
    public static boolean geef(ServerPlayer p) {
        boolean nieuw = Rijdieren.geef(p, RijdierSoort.ZEILBOOTJE);
        CompoundTag t = tag(p);
        boolean eerst = !t.getBooleanOr("gekregen", false);
        t.putBoolean("gekregen", true);
        if (t.getStringOr("steiger", "").isEmpty()) {
            t.putString("steiger", BootZee.thuis().id());
        }
        if (nieuw) {
            Tips.toon(p, "vadsrijk.z4boot.tip.boot");
        }
        if (eerst) {
            for (Consumer<ServerPlayer> h : GEKREGEN) {
                h.accept(p);
            }
        }
        return nieuw;
    }

    // ---- which mount, where -----------------------------------------------------------------------------------------------------

    /** Is this spot on the boat's sea (zone 4, or anywhere for a dev test sailor)? */
    static boolean magVaren(ServerPlayer p, double x, double z) {
        return PROEF.contains(p.getUUID()) || BootZee.opZee(x, z);
    }

    /**
     * Hook (Rijdieren.toets): which mount the key calls. In the water or on a dock of the zone-4 sea: the boat (when you have it).
     * On land with the boat chosen: your first land mount, or null after a message (no land mount: only the boat).
     */
    @Nullable
    public static RijdierSoort kiesSoort(ServerPlayer p, @Nullable RijdierSoort s) {
        if (!heeft(p)) {
            return s;
        }
        boolean zee = magVaren(p, p.getX(), p.getZ());
        boolean wilBoot = zee && (p.isInWater() || BootZee.dichtbij(p.getX(), p.getZ(), BootZee.AANLEG_AFSTAND) != null
                || (s == RijdierSoort.ZEILBOOTJE && zoekWater(p.level(), p.position(), ZOEK) != null));
        return kiesSoort(p, s, wilBoot);
    }

    /** (tests) the choice with the "at the water" answer given. */
    @Nullable
    static RijdierSoort kiesSoort(ServerPlayer p, @Nullable RijdierSoort s, boolean opWater) {
        if (!heeft(p)) {
            return s;
        }
        if (opWater) {
            return RijdierSoort.ZEILBOOTJE;
        }
        if (s != RijdierSoort.ZEILBOOTJE) {
            return s;
        }
        for (RijdierSoort x : RijdierSoort.values()) {
            if (!x.water() && Rijdieren.heeft(p, x)) {
                return x;
            }
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.alleen_water").withStyle(ChatFormatting.LIGHT_PURPLE));
        return null;
    }

    /**
     * The nearest spot within r blocks where the boat floats: water with air above, and the 3x3 around it water too (room for the
     * hull). Returns where the boat's feet go (the surface minus {@link BootEntity#DIEPGANG}), or null.
     */
    @Nullable
    public static Vec3 zoekWater(Level level, Vec3 bij, int r) {
        BlockPos midden = BlockPos.containing(bij);
        Vec3 beste = null;
        double b = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy >= -4; dy--) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = midden.getX() + dx, y = midden.getY() + dy, z = midden.getZ() + dz;
                    double d = dx * dx + dz * dz + dy * dy * 0.25;
                    if (d >= b || d > r * r) {
                        continue;
                    }
                    if (drijfplek(level, m, x, y, z)) {
                        FluidState f = level.getFluidState(m.set(x, y, z));
                        b = d;
                        beste = new Vec3(x + 0.5, y + f.getHeight(level, m) - BootEntity.DIEPGANG, z + 0.5);
                    }
                }
            }
        }
        return beste;
    }

    private static boolean drijfplek(Level level, BlockPos.MutableBlockPos m, int x, int y, int z) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!level.getFluidState(m.set(x + i, y, z + j)).is(FluidTags.WATER)) {
                    return false;
                }
                if (!level.getBlockState(m.set(x + i, y + 1, z + j)).isAir() || !level.getBlockState(m.set(x + i, y + 2, z + j)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Hook (Rijdieren.roep): puts a summoned boat on the water near the player, in their chosen looks. False = no room (message). */
    public static boolean plaats(ServerPlayer p, RijdierEntity r) {
        Vec3 plek = zoekWater(p.level(), p.position(), ZOEK);
        if (plek == null) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.geen_plek").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        r.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
        if (r instanceof BootEntity b) {
            for (BootVariant.As as : BootVariant.As.values()) {
                b.zetVariant(gekozen(p, as));
            }
        }
        BootZee.Steiger s = BootZee.dichtbij(plek.x, plek.z, BootZee.AANLEG_AFSTAND);
        if (s != null) {
            tag(p).putString("steiger", s.id());
        }
        Tips.toon(p, "vadsrijk.z4boot.tip.varen");
        return true;
    }

    // ---- at sea ---------------------------------------------------------------------------------------------------------------

    /** Server, every 10 ticks while sailing: "at sea" flag, docks you pass, the mooring hint, the current's warning. */
    static void opZeeTik(ServerPlayer p, BootEntity b) {
        CompoundTag t = tag(p);
        if (!t.getBooleanOr("opzee", false)) {
            t.putBoolean("opzee", true);
        }
        double diepte = BootZee.kring().r(b.getX(), b.getZ()) < BootZee.STROOM_TOT ? BootZee.kring().diepte(b.getX(), b.getZ()) : 0;
        if (diepte > 0) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.stroming").withStyle(ChatFormatting.AQUA));
            return;
        }
        BootZee.Steiger s = BootZee.dichtbij(b.getX(), b.getZ(), BootZee.AANLEG_AFSTAND);
        if (s != null) {
            t.putString("steiger", s.id());
            p.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.aanleggen", Component.translatable(s.naamKey()).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The rider stepped off onto a dock. */
    static void aangemeerd(ServerPlayer p, BootZee.Steiger s) {
        CompoundTag t = tag(p);
        t.putString("steiger", s.id());
        t.remove("opzee");
        p.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.aangemeerd", Component.translatable(s.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WOOD_STEP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    /** The last dock (or Lampionhaven). */
    public static BootZee.Steiger laatsteSteiger(ServerPlayer p) {
        BootZee.Steiger s = BootZee.steiger(tag(p).getStringOr("steiger", ""));
        return s != null ? s : BootZee.thuis();
    }

    /** Is the player marked as "at sea" (riding the boat at their last save)? */
    public static boolean opZee(ServerPlayer p) {
        return tag(p).getBooleanOr("opzee", false);
    }

    /** Server, every second: off the boat = no longer at sea. */
    static void spelerTik(ServerPlayer p) {
        if (!(p.getVehicle() instanceof BootEntity) && opZee(p)) {
            tag(p).remove("opzee");
        }
    }

    /** Login: logged out at sea? Then you wake up on the last dock (the boat is in its shed). True when moved. */
    public static boolean ingelogd(ServerPlayer p) {
        if (!opZee(p)) {
            return false;
        }
        tag(p).remove("opzee");
        if (!Vadsrijk.in(p)) {
            return false;
        }
        BootZee.Steiger s = laatsteSteiger(p);
        ServerLevel sl = p.level();
        BlockPos b = s.plek();
        p.teleportTo(sl, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        p.sendSystemMessage(Component.translatable("vadsrijk.z4boot.terug_steiger", Component.translatable(s.naamKey()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ---- looks ----------------------------------------------------------------------------------------------------------------

    public static boolean heeftVariant(ServerPlayer p, BootVariant v) {
        if (v.bron.equals("start")) {
            return true;
        }
        for (String s : tag(p).getStringOr("varianten", "").split(",")) {
            if (s.equals(v.id())) {
                return true;
            }
        }
        return false;
    }

    /** API (quests, bosses, rewards): unlocks a look ({@link BootVariant}, e.g. ZEIL_STERRENNACHT). False when already owned. */
    public static boolean geefVariant(ServerPlayer p, BootVariant v) {
        if (heeftVariant(p, v)) {
            return false;
        }
        String oud = tag(p).getStringOr("varianten", "");
        tag(p).putString("varianten", oud.isEmpty() ? v.id() : oud + "," + v.id());
        p.sendSystemMessage(Component.translatable("vadsrijk.z4boot.nieuw_variant", Component.translatable(v.naamKey()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** API by id ("zeil_sterrennacht", "boeg:draakje"). False when unknown or already owned. */
    public static boolean geefVariant(ServerPlayer p, String id) {
        BootVariant v = BootVariant.van(id);
        return v != null && geefVariant(p, v);
    }

    public static BootVariant gekozen(ServerPlayer p, BootVariant.As as) {
        BootVariant v = BootVariant.van(tag(p).getStringOr(as.id(), ""));
        return v != null && v.as == as && heeftVariant(p, v) ? v : as.standaard();
    }

    /** Picks a look you own (and shows it at once on the boat you sail). */
    public static boolean kies(ServerPlayer p, BootVariant v) {
        if (!heeftVariant(p, v)) {
            return false;
        }
        tag(p).putString(v.as.id(), v.id());
        if (p.getVehicle() instanceof BootEntity b) {
            b.zetVariant(v);
        }
        return true;
    }

    /** Buys a look in the boat screen. Returns the message key. */
    static String koop(ServerPlayer p, BootVariant v) {
        if (!heeft(p)) {
            return "vadsrijk.z4boot.scherm.geen_boot";
        }
        if (heeftVariant(p, v)) {
            return "vadsrijk.z4boot.scherm.al";
        }
        if (v.prijs <= 0) {
            return "";
        }
        if (!Spelers.neemMunten(p, v.prijs)) {
            return "vadsrijk.economie.smid.te_weinig";
        }
        geefVariant(p, v);
        kies(p, v);
        return "vadsrijk.z4boot.scherm.gekocht";
    }

    /** Dev: every look. */
    static void alleVarianten(ServerPlayer p) {
        for (BootVariant v : BootVariant.values()) {
            geefVariant(p, v);
        }
    }

    /** Dev: forget the boat (and its looks, docks, flags). */
    static void reset(ServerPlayer p) {
        if (p.getVehicle() instanceof BootEntity b) {
            Rijdieren.poef(b);
        }
        Spelers.data(p).extra.remove(SLICE);
        net.minecraft.nbt.CompoundTag eco = Spelers.data(p).extra("economie");
        eco.getCompoundOrEmpty("rijdieren").remove(RijdierSoort.ZEILBOOTJE.id());
        if (RijdierSoort.ZEILBOOTJE.id().equals(eco.getStringOr("actief", ""))) {
            eco.remove("actief");
        }
        PROEF.remove(p.getUUID());
    }

    // ---- the screen ------------------------------------------------------------------------------------------------------------

    static CompoundTag schermData(ServerPlayer p) {
        CompoundTag d = new CompoundTag();
        boolean heeft = heeft(p);
        d.putBoolean("heeft", heeft);
        int level = heeft ? Rijdieren.level(p, RijdierSoort.ZEILBOOTJE) : 1;
        d.putInt("level", level);
        d.putLong("xp", heeft ? Rijdieren.xp(p, RijdierSoort.ZEILBOOTJE) : 0);
        d.putLong("nodig", RijdierSoort.xpNodig(level));
        d.putDouble("snelheid", RijdierSoort.ZEILBOOTJE.snelheid(level));
        d.putLong("munten", Spelers.data(p).munten);
        d.putString("steiger", laatsteSteiger(p).naamKey());
        ListTag l = new ListTag();
        for (BootVariant v : BootVariant.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", v.id());
            t.putBoolean("heeft", heeftVariant(p, v));
            t.putBoolean("gekozen", gekozen(p, v.as) == v);
            l.add(t);
        }
        d.put("varianten", l);
        return d;
    }

    static void scherm(ServerPlayer p, String melding) {
        CompoundTag d = schermData(p);
        d.putString("melding", melding);
        BootPayloads.stuur(p, new BootPayloads.Scherm(d));
    }

    static void doe(ServerPlayer p, String actie, String id) {
        if (!Vadsrijk.in(p) && !PROEF.contains(p.getUUID())) {
            return;
        }
        String melding = "";
        BootVariant v = BootVariant.van(id);
        switch (actie) {
            case "open" -> {
            }
            case "kies" -> {
                if (v != null && kies(p, v)) {
                    melding = "vadsrijk.z4boot.scherm.gekozen";
                    p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
                }
            }
            case "koop" -> melding = v == null ? "" : koop(p, v);
            default -> {
                return;
            }
        }
        scherm(p, melding);
    }
}
