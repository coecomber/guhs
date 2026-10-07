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
    /** The player's saved Appa: Heeft, Gen, Weg, Naam, Items. */
    public static final String KEY = "guhs_einde2_appa_appa";

    /** Server: owner -> their loaded Appa (the newest one; see {@link #meld}). */
    private static final Map<UUID, AppaGuhEntity> GELADEN = new ConcurrentHashMap<>();

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
    }

    /** Takes an Appa out of the world (everybody steps off first; his chest is safe in the owner's data). */
    private static void verdwijn(AppaGuhEntity appa) {
        appa.uitstappen = true;
        try {
            appa.ejectPassengers();
        } finally {
            appa.uitstappen = false;
        }
        appa.discard();
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
            appa.uitstappen = true;
            try {
                appa.ejectPassengers();
            } finally {
                appa.uitstappen = false;
            }
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
        appa.vliegNaar(p, doel, spelerStuurt);
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
     * right above them (he sinks down by himself when there is ground).
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
