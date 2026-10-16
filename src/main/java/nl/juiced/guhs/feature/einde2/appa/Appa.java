package nl.juiced.guhs.feature.einde2.appa;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * einde2, slice appa: a player's own Appa-guh (exactly one per player, never lost).
 * <p>
 * What makes an Appa THIS player's Appa lives in the player's own saved data ({@value #KEY} in {@code GuhQuests.saved}): that
 * they have one, the contents of his saddle chest, his name, whether he is being kept away, and a number ("Gen") that goes
 * up every time he is made again. The entity in the world carries a copy; {@link #roep} moves the loaded one or, when he is
 * dead, unloaded or in another dimension, makes him again from the saved data. An entity with an older number that turns up
 * later (its chunk loads) removes itself ({@link #meld}), so there are never two, and nothing can be taken out of a stale
 * chest: only his owner opens the chest, and a stale copy is gone before its owner can click it.
 * <p>
 * hook fase B: nothing in phase A gives an Appa in survival; the story calls {@link #geef}, flies the player to the next
 * structure with {@link #vlieg} and hears about the landing through {@link #AANGEKOMEN}; chapter 3 uses {@link #houdWeg}.
 */
public final class Appa {
    /** Called when an auto flight ({@link #vlieg}) lands, with the rider. */
    public static final List<Consumer<ServerPlayer>> AANGEKOMEN = new CopyOnWriteArrayList<>();
    /**
     * einde2 phase B (reizen): the whistle blown while sitting on your own Appa's head. True = it was handled (the list of
     * places to fly to opened); false = the old line "you are sitting on him already".
     */
    public static volatile java.util.function.Predicate<ServerPlayer> OP_ZIJN_KOP = p -> false;
    /**
     * einde2 fix-reizen: his owner right-clicks him (not sneaking, not sitting on him). True = it was handled (his menu "Waar
     * vliegen we heen?" opened); false = they climb on as before.
     */
    public static volatile java.util.function.Predicate<ServerPlayer> KLIK = p -> false;
    /** The player's saved Appa: Heeft, Gen, Weg, Naam, Items. */
    public static final String KEY = "guhs_einde2_appa_appa";

    /**
     * einde2 fix-reizen: where a flight ends. {@code appa}: the spot he lands on (his feet), looking {@code yaw} (NaN: as he
     * comes in); {@code speler}: where his rider is set down (null: a free spot beside him); {@code gebied}: when his rider
     * is taken off him inside it (a cutscene of the place began while he was still on the way), he lands on his spot at
     * once (null: he just stops); {@code afstappen}: his rider gets off onto their spot when he has landed.
     */
    public record Landing(Vec3 appa, float yaw, @Nullable Vec3 speler, @Nullable net.minecraft.world.phys.AABB gebied, boolean afstappen) {
        /** Just a spot: he lands on it as he comes in, the rider stays on. */
        public static Landing op(Vec3 doel) {
            return new Landing(doel, Float.NaN, null, null, false);
        }

        /** Where the rider of this Appa is set down: their own spot when it is free, else a free spot beside him. */
        public Vec3 spelerOf(AppaGuhEntity appa, ServerPlayer p) {
            if (speler != null) {
                Vec3 vrij = vrijePlek(appa.level(), p.getType(), speler, 3, true);
                if (vrij != null) {
                    return vrij;
                }
            }
            return appa.getDismountLocationForPassenger(p);
        }
    }

    /** Server: owner -> their loaded Appa (the newest one; see {@link #meld}). */
    private static final Map<UUID, AppaGuhEntity> GELADEN = new ConcurrentHashMap<>();

    /** einde2 fix-reizen: a player who got off him in the air and is set down here once they are free (see {@link #zetNeer}). */
    private record Neer(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, Vec3 waar, Vec3 van, int tot) {
    }

    private static final Map<UUID, Neer> NEER = new ConcurrentHashMap<>();
    /** Player -> until which server tick a fall or a wall does them no harm (just set down). */
    private static final Map<UUID, Integer> ZACHT = new ConcurrentHashMap<>();
    /** How long (ticks) a {@link #zetNeer} waits for a cutscene or card to let the player go. */
    private static final int NEER_WACHT = 20 * 60 * 20;

    // --- the saved state --------------------------------------------------------------------------------------------------------

    private static CompoundTag staat(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    public static boolean heeft(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getBooleanOr("Heeft", false);
    }

    /** Is he being kept away from this player ({@link #houdWeg})? */
    public static boolean isWeg(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getBooleanOr("Weg", false);
    }

    static int gen(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Gen", 0);
    }

    static void leesKist(ServerPlayer p, SimpleContainer kist) {
        kist.clearContent();
        ContainerHelper.loadAllItems(Nbt.input(p.registryAccess(), staat(p)), kist.getItems());
    }

    static void schrijfKist(ServerPlayer p, SimpleContainer kist) {
        CompoundTag staat = staat(p);
        staat.remove(ContainerHelper.TAG_ITEMS);
        staat.merge(Nbt.write(p.registryAccess(), out -> ContainerHelper.saveAllItems(out, kist.getItems())));
    }

    /** The saved contents of this player's saddle chest (a copy; for tests and for whoever wants to look). */
    public static List<ItemStack> kist(ServerPlayer p) {
        SimpleContainer kist = new SimpleContainer(AppaGuhEntity.KIST_VAKKEN);
        leesKist(p, kist);
        return kist.getItems().stream().map(ItemStack::copy).toList();
    }

    /** The name a player's Appa was given (null: none). */
    @Nullable
    public static Component naam(ServerPlayer p) {
        return Tekst.read(staat(p), "Naam").orElse(null);
    }

    /** How a player is called in a line about their Appa or Momo (their name when online, "iemand anders" when not). */
    static Component naamVan(@Nullable MinecraftServer server, UUID speler) {
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(speler);
        return p != null ? p.getDisplayName() : Component.translatable("gui.guhs.einde2_appa.iemand");
    }

    // --- the loaded Appa --------------------------------------------------------------------------------------------------------

    /**
     * Called by every owned Appa a few times a second. While his owner is online: an Appa that is not the owner's current
     * one (an older number, or the owner has none / he is kept away) removes itself; the current one is remembered, reads
     * his chest from the owner once and tells the owner's data his name.
     */
    static void meld(AppaGuhEntity appa) {
        UUID eigenaar = appa.eigenaar();
        if (eigenaar == null || appa.isRemoved()) {
            return;
        }
        ServerPlayer baas = appa.baas();
        if (baas == null) {
            return;                                 // (offline: we can't know; nobody but the owner opens the chest)
        }
        if (!heeft(baas) || isWeg(baas) || appa.gen() != gen(baas)) {
            verdwijn(appa);
            return;
        }
        AppaGuhEntity oud = GELADEN.put(eigenaar, appa);
        if (oud != null && oud != appa && !oud.isRemoved() && oud.gen() == appa.gen()) {
            verdwijn(oud);                          // (two with the same number can't be made; but never two)
        }
        if (!appa.isKistGelezen()) {
            appa.leesKist(baas);
        }
        CompoundTag staat = staat(baas);
        Component naam = appa.hasCustomName() ? appa.getCustomName() : null;
        if (naam == null ? staat.contains("Naam") : !naam.equals(Tekst.read(staat, "Naam").orElse(null))) {
            if (naam == null) {
                staat.remove("Naam");
            } else {
                Tekst.put(staat, "Naam", naam);
            }
        }
    }

    static void vergeet(AppaGuhEntity appa) {
        UUID eigenaar = appa.eigenaar();
        if (eigenaar != null) {
            GELADEN.remove(eigenaar, appa);
        }
    }

    /** Server stopped: nothing is loaded any more. */
    static void leeg() {
        GELADEN.clear();
        NEER.clear();
        ZACHT.clear();
    }

    // --- getting off safely (einde2 fix-reizen) ------------------------------------------------------------------------------

    /**
     * This player got off their Appa up in the air (a cutscene took them off, a teleport): as soon as nothing holds them
     * any more (no cutscene or card) and they still hang near where they got off, they are put on a free spot at
     * {@code waar} (on solid ground, never in a wall), and a fall or a wall does them no harm for a few seconds. A later
     * call replaces an earlier one. Nothing happens when they got on something again, flew away (creative) or were taken
     * somewhere else (more than 10 blocks from where they got off: a teleport of the story).
     */
    public static void zetNeer(ServerPlayer p, Vec3 waar) {
        MinecraftServer server = p.level().getServer();
        int nu = server == null ? 0 : server.getTickCount();
        NEER.put(p.getUUID(), new Neer(p.level().dimension(), waar, p.position(), nu + NEER_WACHT));
        ZACHT.put(p.getUUID(), nu + NEER_WACHT);
    }

    /** Every tick for every player: a waiting {@link #zetNeer}. */
    static void neerTick(ServerPlayer p) {
        Neer n = NEER.get(p.getUUID());
        if (n == null || p.level().getServer() == null) {
            return;
        }
        int nu = p.level().getServer().getTickCount();
        if (nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p)) {
            return;                                            // (a scene holds them: afterwards)
        }
        boolean weg = p.level().dimension() != n.dim() || p.isPassenger() || nu > n.tot() || p.position().distanceToSqr(n.van()) > 10 * 10
                || p.getAbilities().flying || p.isSpectator() || !p.isAlive();
        NEER.remove(p.getUUID(), n);
        if (weg) {
            ZACHT.put(p.getUUID(), nu + 40);
            return;
        }
        Vec3 w = vrijePlek(p.level(), p.getType(), n.waar(), 4, true);
        if (w == null) {
            w = grondOnder(p.level(), p.getType(), n.waar());
        }
        if (w == null) {
            w = n.waar();
        }
        p.teleportTo(w.x, w.y, w.z);
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        ZACHT.put(p.getUUID(), nu + 60);
    }

    /** Is this player just set down (or about to be), so a fall or a wall must not hurt them? */
    static boolean zacht(ServerPlayer p) {
        Integer tot = ZACHT.get(p.getUUID());
        if (tot == null || p.level().getServer() == null) {
            return false;
        }
        if (p.level().getServer().getTickCount() > tot) {
            ZACHT.remove(p.getUUID(), tot);
            return false;
        }
        return true;
    }

    /**
     * The first free spot with solid ground under it straight below {@code van} (under an overhang or a bridge too: it looks
     * down, not at the heightmap), else a free spot on the ground near the heightmap top of that column. Null: only void.
     */
    @Nullable
    public static Vec3 grondOnder(net.minecraft.world.level.Level level, net.minecraft.world.entity.EntityType<?> type, Vec3 van) {
        var maat = type.getDimensions();
        int top = net.minecraft.util.Mth.floor(van.y);
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(van))) {
            return null;
        }
        for (int y = top; y > level.getMinY() + 1; y--) {
            var doos = maat.makeBoundingBox(van.x, y, van.z);
            var onder = new net.minecraft.world.phys.AABB(van.x - 0.25, y - 0.99, van.z - 0.25, van.x + 0.25, y - 0.01, van.z + 0.25);
            if (!level.noCollision(onder) && level.noCollision(doos) && !level.containsAnyLiquid(doos)) {
                return new Vec3(van.x, y, van.z);
            }
        }
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, net.minecraft.util.Mth.floor(van.x),
                net.minecraft.util.Mth.floor(van.z));
        return y > level.getMinY() + 1 ? vrijePlek(level, type, new Vec3(van.x, y, van.z), 4, true) : null;
    }

    /**
     * A free spot for an entity of this type near {@code bij} (the nearest first, within {@code straal} blocks sideways and
     * a few up and down): its box free of blocks and liquid, and with {@code opGrond} something solid right under it. Null:
     * none (or the chunks are not there).
     */
    @Nullable
    public static Vec3 vrijePlek(net.minecraft.world.level.Level level, net.minecraft.world.entity.EntityType<?> type, Vec3 bij, int straal,
                                 boolean opGrond) {
        var maat = type.getDimensions();
        int bx = net.minecraft.util.Mth.floor(bij.x), by = net.minecraft.util.Mth.floor(bij.y + 0.01), bz = net.minecraft.util.Mth.floor(bij.z);
        int[] hoogtes = {0, 1, -1, 2, -2, 3, 4};
        for (int r = 0; r <= straal; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy : hoogtes) {
                        double x = r == 0 ? bij.x : bx + dx + 0.5, z = r == 0 ? bij.z : bz + dz + 0.5;
                        double y = r == 0 && dy == 0 ? bij.y : by + dy;
                        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(x, y, z))) {
                            continue;
                        }
                        var doos = maat.makeBoundingBox(x, y, z);
                        if (!level.noCollision(doos) || level.containsAnyLiquid(doos)) {
                            continue;
                        }
                        var onder = new net.minecraft.world.phys.AABB(x - 0.25, y - 0.99, z - 0.25, x + 0.25, y - 0.01, z + 0.25);
                        if (opGrond && level.noCollision(onder)) {
                            continue;
                        }
                        return new Vec3(x, y, z);
                    }
                }
            }
        }
        return null;
    }

    /** Takes an Appa out of the world (everybody steps off first; his chest is safe in the owner's data). */
    private static void verdwijn(AppaGuhEntity appa) {
        stapUit(appa, null);
        if (appa.baas() != null && appa.baas().containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu && menu.getContainer() == appa.kist()) {
            appa.baas().closeContainer();          // (his saddle chest was open: it goes with him)
        }
        appa.discard();
    }

    /**
     * Everybody off him. Whoever would fall into the void (no ground within 64 blocks below: a guest, a guh from the basket)
     * is set down on ground instead: at {@code veilig}, else where he last stood on the ground. Without either they float
     * down as before.
     */
    static void stapUit(AppaGuhEntity appa, @Nullable Vec3 veilig) {
        List<Entity> wie = List.copyOf(appa.getPassengers());
        appa.uitstappen = true;
        try {
            appa.ejectPassengers();
        } finally {
            appa.uitstappen = false;
        }
        Vec3 naar = veilig != null ? veilig : appa.laatsteGrond();
        if (naar == null) {
            return;
        }
        for (Entity e : wie) {
            if (e.isAlive() && !e.isRemoved() && !AppaGuhEntity.grondOnder(e, 64)) {
                e.teleportTo(naar.x, naar.y + 0.1, naar.z);
                e.resetFallDistance();
                e.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    /**
     * A player who leaves the game on an Appa that carries another player too: the game then keeps him in the world (it only
     * takes a mount along with its one player), so whoever leaves would come back in mid-air without him, and a guest who is
     * the last to leave would take him along in their own data. They step off first, onto the ground he last stood on.
     */
    static void bijVertrek(ServerPlayer p) {
        if (!(p.getVehicle() instanceof AppaGuhEntity appa)) {
            return;
        }
        boolean alleen = appa.getPassengers().stream().filter(e -> e instanceof net.minecraft.world.entity.player.Player).count() <= 1;
        if (alleen && appa.isEigenaar(p)) {
            return;                                 // (the owner alone: he goes along with them, as any mount)
        }
        appa.uitstappen = true;
        try {
            p.stopRiding();
        } finally {
            appa.uitstappen = false;
        }
        Vec3 naar = appa.laatsteGrond();
        if (naar != null && !AppaGuhEntity.grondOnder(p, 64)) {
            p.teleportTo(naar.x, naar.y + 0.1, naar.z);
            p.resetFallDistance();
        }
    }

    /** This player's loaded Appa, or null (none, dead, in an unloaded chunk). */
    @Nullable
    public static AppaGuhEntity geladen(ServerPlayer p) {
        AppaGuhEntity appa = GELADEN.get(p.getUUID());
        if (appa == null || appa.isRemoved() || !appa.isAlive() || appa.gen() != gen(p) || !appa.isEigenaar(p)) {
            return null;
        }
        return appa;
    }

    // --- the API ----------------------------------------------------------------------------------------------------------------

    /**
     * Gives this player their Appa-guh, once: the first time they also get an Appa-fluitje and his Guhdex page; when they
     * have one already, that one comes ({@link #roep}). Null only while he is being kept away ({@link #houdWeg}).
     */
    @Nullable
    public static AppaGuhEntity geef(ServerPlayer p) {
        if (!heeft(p)) {
            staat(p).putBoolean("Heeft", true);
            Minigames.give(p, new ItemStack(E2AppaFeature.APPA_FLUITJE.get()));
            GuhDex.zie(p, GuhVariant.APPA_GUH);
        }
        return roep(p);
    }

    /**
     * Brings the player's Appa-guh to them: the loaded one moves, an unloaded, dead or far-away one is made again right
     * here (the same chest, the same name). Null: they have none, or he is being kept away.
     */
    @Nullable
    public static AppaGuhEntity roep(ServerPlayer p) {
        if (!heeft(p) || isWeg(p)) {
            return null;
        }
        AppaGuhEntity appa = geladen(p);
        if (appa != null && appa.getVehicle() == null && p.getVehicle() == appa) {
            return appa;                            // (they sit on him)
        }
        ServerLevel level = p.level();
        Vec3 plek = landplek(p);
        if (appa != null && appa.level() == level) {
            appa.stopVlucht();
            // (a guest or a guh that would drop into the void comes along to where he lands, when that is on the ground)
            stapUit(appa, level.noCollision(E2AppaFeature.APPA_GUH.get().getSpawnAABB(plek.x, plek.y, plek.z).move(0, -1, 0)) ? null : plek);
            appa.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
            appa.setDeltaMovement(Vec3.ZERO);
            return appa;
        }
        if (appa != null) {
            verdwijn(appa);                         // (another dimension: he is made again here)
        }
        CompoundTag staat = staat(p);
        staat.putInt("Gen", gen(p) + 1);            // whatever copy still lies in an unloaded chunk is now an old one
        appa = E2AppaFeature.APPA_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (appa == null) {
            return null;
        }
        appa.zetEigenaar(p.getUUID(), gen(p));
        appa.leesKist(p);
        Component naam = Tekst.read(staat, "Naam").orElse(null);
        if (naam != null) {
            appa.setCustomName(naam);
        }
        appa.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
        level.addFreshEntity(appa);
        GELADEN.put(p.getUUID(), appa);
        return appa;
    }

    /**
     * While true the Appa-fluitje only says a line and {@link #roep} gives nothing: he is taken out of the world (his chest
     * and name stay with the player). False again: he comes with the next {@link #roep} or whistle.
     */
    public static void houdWeg(ServerPlayer p, boolean weg) {
        if (!heeft(p) || isWeg(p) == weg) {
            return;
        }
        if (weg) {
            AppaGuhEntity appa = geladen(p);
            staat(p).putBoolean("Weg", true);
            if (appa != null) {
                verdwijn(appa);
            }
        } else {
            staat(p).remove("Weg");
        }
    }

    /**
     * Flies the player on their Appa to {@code doel} and lands there ({@link #AANGEKOMEN}). He comes to the player first
     * if needed and seats them. With {@code spelerStuurt} he only points the way (a line above the hotbar with an arrow
     * and the distance, sparkles from his nose) until the player lets go for 3 seconds, then he flies himself; the last
     * blocks and the landing are always his.
     *
     * @return false when the flight can't start (no Appa, he is kept away, or the player can't get on)
     */
    public static boolean vlieg(ServerPlayer p, Vec3 doel, boolean spelerStuurt) {
        return vlieg(p, Landing.op(doel), spelerStuurt);
    }

    /**
     * einde2 fix-reizen: as {@link #vlieg(ServerPlayer, Vec3, boolean)}, onto a {@link Landing}: he lands on its spot looking
     * its way, sets his rider down on theirs when it says so, and lands at once when his rider is taken off him inside its
     * {@code gebied}. On the way he flies at journey speed ({@link AppaGuhEntity#REIS_SNELHEID}).
     */
    public static boolean vlieg(ServerPlayer p, Landing landing, boolean spelerStuurt) {
        AppaGuhEntity appa = roep(p);
        if (appa == null) {
            return false;
        }
        if (p.getVehicle() != appa) {
            p.stopRiding();
            if (!p.startRiding(appa, true, false)) {
                return false;
            }
        }
        appa.vliegNaar(p, landing, spelerStuurt);
        return true;
    }

    /** Is this player on an auto flight right now? */
    public static boolean vliegt(ServerPlayer p) {
        AppaGuhEntity appa = geladen(p);
        return appa != null && appa.vluchtDoel() != null;
    }

    /** Breaks the player's auto flight off (Appa hangs where he is; the player has the reins). */
    public static void stop(ServerPlayer p) {
        AppaGuhEntity appa = geladen(p);
        if (appa != null) {
            appa.stopVlucht();
        }
    }

    /**
     * Where he lands when called: a free spot on the ground a few blocks in front of or around the player, else in the air
     * right above them (he sinks down by himself when there is ground); einde2 fix-reizen: the first height above them where
     * he is free of blocks, so nobody who climbs on him ends up in a roof.
     */
    private static Vec3 landplek(ServerPlayer p) {
        ServerLevel level = p.level();
        var type = E2AppaFeature.APPA_GUH.get();
        float yaw = p.getYRot();
        for (int ring = 4; ring <= 8; ring += 2) {
            for (int k = 0; k < 8; k++) {
                Vec3 opzij = new Vec3(0, 0, ring).yRot(-(yaw + k * 45f * (k % 2 == 0 ? 1 : -1)) * (float) (Math.PI / 180.0));
                for (int dy = 3; dy >= -3; dy--) {
                    Vec3 at = new Vec3(Math.floor(p.getX() + opzij.x) + 0.5, Math.floor(p.getY()) + dy, Math.floor(p.getZ() + opzij.z) + 0.5);
                    var doos = type.getSpawnAABB(at.x, at.y, at.z);
                    if (level.noCollision(doos) && !level.noCollision(doos.move(0, -1, 0)) && !level.containsAnyLiquid(doos)) {
                        return at;
                    }
                }
            }
        }
        for (int dy = 4; dy <= 32; dy++) {
            Vec3 at = p.position().add(0, dy, 0);
            if (level.noCollision(type.getSpawnAABB(at.x, at.y, at.z).expandTowards(0, 2, 0))) {
                return at;
            }
        }
        return p.position().add(0, 3.5, 0);
    }

    /** For {@link Entity}-less callers (commands): the Appa this entity is, if it is one of a player. */
    @Nullable
    public static AppaGuhEntity van(@Nullable Entity e) {
        return e instanceof AppaGuhEntity a && a.eigenaar() != null ? a : null;
    }

    private Appa() {
    }
}
