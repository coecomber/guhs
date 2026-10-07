package nl.juiced.guhs.feature.snuffel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.network.ModNetworking;
import org.slf4j.Logger;

/**
 * The DOG FORM: on Het Snuffeleiland a player is a dog, everywhere else never. There is one rule and it is checked every
 * tick for every player ({@link #controleer}), so it holds whatever brought the player in or out (the boat, the Guhstation,
 * the memory card, a command, a death, a login):
 * <pre>
 *     on an island ({@link Eiland#in})  &lt;=&gt;  dog form ({@link #actief})  &lt;=&gt;  the own inventory is in {@link SnuffelKluis}
 * </pre>
 * {@link #aan}: the inventory goes into the safe, the memory card comes into the last hotbar slot, the player gets the
 * dog's size (box and eye height, {@link #maat}) and everybody who sees them is told which dog to draw. {@link #uit}: the
 * inventory comes back exactly, what the island gave comes with it, the size and the look are the player's own again.
 * A dead player is left alone: a dog that dies (only /kill gets through) drops nothing and is a player with all their
 * things again at the respawn; a login on the death screen waits for the respawn too.
 * <p>
 * What a dog does: it holds poses ({@link #SNUFFELT}, {@link #ZIT}, {@link #KWISPELT}; the client says which keys are held,
 * the server tells everybody near) and makes gestures ({@link #BLAF}, {@link #GRAAF}).
 */
public final class Hondvorm {
    private static final Logger LOG = LogUtils.getLogger();
    /** The game id for {@link Minigames}: a dog is "playing" (one game at a time, no /lobby, nobody is hurt). */
    public static final String SPEL = "snuffeleiland";
    public static final int SNUFFELT = 1, ZIT = 2, KWISPELT = 4;
    public static final int BLAF = 1, GRAAF = 2;
    /** The hotbar slot of the memory card. */
    public static final int KAART_SLOT = 8;
    public static final int BLAF_TICKS = 8, BLAF_RUST = 12;
    private static final Identifier TRAAG = Guhs.id("snuffel_snuffelt");
    private static final String VORM = "Vorm", DOOD = "Dood";

    private static final Map<UUID, Integer> HOUDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BLAF_TOT = new ConcurrentHashMap<>();
    /** Client: which breed the dog is that this player is drawn as (null: no dog). Set by client.HondClient. */
    public static volatile Function<Player, Honden.Ras> clientRas = p -> null;

    private Hondvorm() {
    }

    /** Is this player a dog right now (server side)? */
    public static boolean actief(@Nullable Player p) {
        return p != null && SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr(VORM, false);
    }

    /** The one rule: a dog on an island, a player everywhere else. Cheap; called every tick and after every teleport. */
    public static void controleer(ServerPlayer p) {
        if (p instanceof FakePlayer || p.isRemoved() || p.isDeadOrDying()) {
            return;
        }
        boolean moet = Eiland.in(p), is = actief(p);
        if (moet && !is) {
            aan(p);
        } else if (!moet && is) {
            uit(p);
        }
    }

    /** Into dog form. Only {@link #controleer} decides when. */
    static void aan(ServerPlayer p) {
        if (actief(p)) {
            return;
        }
        if (SnuffelKluis.heeft(p)) {
            // (a snapshot without the form: it holds the real inventory, so it stays; what the player carries now goes home too)
            LOG.warn("Snuffeleiland: {} already has a stored inventory, keeping it", p.getGameProfile().name());
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                SnuffelKluis.post(p, inv.getItem(i).copy());
            }
            inv.clearContent();
        } else {
            SnuffelKluis.bewaar(p);
        }
        CompoundTag data = SnuffelData.van(p);
        data.putBoolean(VORM, true);
        data.putBoolean("Bezocht", true);
        p.stopRiding();
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        geefKaart(p);
        p.getInventory().setSelectedSlot(0);
        if (p.connection != null) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(0));
        }
        Minigames.forget(p);
        HOUDING.remove(p.getUUID());
        p.refreshDimensions();
        sync(p);
        Stand.stuur(p);
    }

    /** Out of dog form: everything back. Only {@link #controleer} decides when. */
    static void uit(ServerPlayer p) {
        if (!actief(p)) {
            return;
        }
        SnuffelData.van(p).putBoolean(VORM, false);
        HOUDING.remove(p.getUUID());
        Snuffelen.stop(p);
        traag(p, false);
        haalKaartWeg(p);
        if (!SnuffelKluis.herstel(p)) {
            LOG.warn("Snuffeleiland: {} was a dog without a stored inventory", p.getGameProfile().name());
        }
        SnuffelKluis.geefPost(p);
        Minigames.forget(p);
        Maatjes.weg(p);
        p.refreshDimensions();
        sync(p);
        Stand.stuur(p);
        if (SnuffelKluis.heeftPost(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.post_wacht", SnuffelKluis.postAantal(p)).withStyle(ChatFormatting.GOLD));
        }
    }

    /** The memory card lies in its slot (and nowhere else). */
    static void geefKaart(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i != KAART_SLOT && inv.getItem(i).is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        ItemStack daar = inv.getItem(KAART_SLOT);
        if (!daar.is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
            if (!daar.isEmpty()) {
                // (something else lies there: a creative op's block; it moves over, or goes home)
                ItemStack rest = daar.copy();
                inv.setItem(KAART_SLOT, ItemStack.EMPTY);
                if (!inv.add(rest) || !rest.isEmpty()) {
                    SnuffelKluis.post(p, rest);
                }
            }
            inv.setItem(KAART_SLOT, new ItemStack(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get()));
        }
        if (p.containerMenu.getCarried().is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    /** No memory card anywhere on this player (it only exists in a dog's pockets). */
    static void haalKaartWeg(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (p.containerMenu.getCarried().is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    // =====================================================================================================================
    // the dog's size
    // =====================================================================================================================

    /** The box and eye height of this player as a dog (null: no dog). Both sides. */
    @Nullable
    public static EntityDimensions maat(Player p) {
        Honden.Ras ras;
        if (p.level().isClientSide()) {
            ras = clientRas.apply(p);
        } else {
            ras = actief(p) ? Honden.ras(Keuze.vanOfStandaard(p).ras()) : null;
        }
        if (ras == null) {
            return null;
        }
        return EntityDimensions.scalable(Honden.BREEDTE, ras.hoogte()).withEyeHeight(ras.oog());
    }

    /** The dog changed (another breed): its size follows. */
    static void ververs(ServerPlayer p) {
        if (actief(p)) {
            p.refreshDimensions();
        }
    }

    // =====================================================================================================================
    // telling the clients
    // =====================================================================================================================

    static SnuffelPayloads.Vorm bericht(ServerPlayer p) {
        boolean dog = actief(p);
        Keuze k = Keuze.vanOfStandaard(p);
        return new SnuffelPayloads.Vorm(p.getUUID(), dog, k.ras(), k.kleur(), k.naam());
    }

    /** Everybody on the server learns what this player looks like now (it changes seldom; a stale dog would be worse). */
    public static void sync(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server == null) {
            return;
        }
        SnuffelPayloads.Vorm v = bericht(p);
        for (ServerPlayer q : server.getPlayerList().getPlayers()) {
            ModNetworking.sendTo(q, v);
        }
    }

    /** A player comes into view of another: the watcher learns the look and the pose. */
    static void toon(ServerPlayer wie, ServerPlayer aan) {
        ModNetworking.sendTo(aan, bericht(wie));
        if (actief(wie)) {
            ModNetworking.sendTo(aan, new SnuffelPayloads.Houding(wie.getUUID(), houding(wie)));
        }
    }

    private static void rond(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload bericht) {
        for (Player q : p.level().players()) {
            if (q instanceof ServerPlayer sp && (q == p || q.distanceToSqr(p) < 96 * 96)) {
                ModNetworking.sendTo(sp, bericht);
            }
        }
    }

    // =====================================================================================================================
    // poses and gestures
    // =====================================================================================================================

    /** The poses this dog holds (bits; 0 for a player who is no dog). */
    public static int houding(Player p) {
        return HOUDING.getOrDefault(p.getUUID(), 0);
    }

    public static boolean snuffelt(Player p) {
        return (houding(p) & SNUFFELT) != 0;
    }

    /** The client's keys (or a test): which poses the dog holds now. Sniffing and sitting don't go together: sniffing wins. */
    public static void zetHouding(ServerPlayer p, int vlaggen) {
        if (!actief(p)) {
            return;
        }
        vlaggen &= SNUFFELT | ZIT | KWISPELT;
        if ((vlaggen & SNUFFELT) != 0) {
            vlaggen &= ~ZIT;
        }
        int oud = houding(p);
        if (oud == vlaggen) {
            return;
        }
        if (vlaggen == 0) {
            HOUDING.remove(p.getUUID());
        } else {
            HOUDING.put(p.getUUID(), vlaggen);
        }
        traag(p, (vlaggen & SNUFFELT) != 0);
        if ((vlaggen & KWISPELT) != 0 && (oud & KWISPELT) == 0) {
            p.level().playSound(null, p.blockPosition(), SnuffelFeature.KWISPEL_GELUID.get(), SoundSource.PLAYERS, 0.5f, 1f);
        }
        if ((oud & SNUFFELT) != 0 && (vlaggen & SNUFFELT) == 0) {
            Snuffelen.stop(p);
        }
        rond(p, new SnuffelPayloads.Houding(p.getUUID(), vlaggen));
    }

    /** Nose to the ground: the dog walks slower. */
    private static void traag(ServerPlayer p, boolean aan) {
        AttributeInstance snelheid = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (snelheid == null) {
            return;
        }
        snelheid.removeModifier(TRAAG);
        if (aan) {
            snelheid.addTransientModifier(new AttributeModifier(TRAAG, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** "Njeg!" (a bark): everybody near hears and sees it. False while the dog is still barking. */
    public static boolean blaf(ServerPlayer p) {
        if (!actief(p)) {
            return false;
        }
        long nu = p.level().getGameTime();
        if (BLAF_TOT.getOrDefault(p.getUUID(), 0L) > nu) {
            return false;
        }
        BLAF_TOT.put(p.getUUID(), nu + BLAF_RUST);
        Honden.Ras ras = Honden.ras(Keuze.vanOfStandaard(p).ras());
        float toon = ras == null ? 1f : 1.55f - ras.hoogte() * 0.6f;   // (a small dog yaps, a big one woofs)
        p.level().playSound(null, p.blockPosition(), SnuffelFeature.BLAF_GELUID.get(), SoundSource.PLAYERS, 0.9f, toon + p.getRandom().nextFloat() * 0.1f);
        p.level().playSound(null, p.blockPosition(), SnuffelFeature.NJEG_GELUID.get(), SoundSource.PLAYERS, 0.5f, toon);
        gebaar(p, BLAF, BLAF_TICKS);
        return true;
    }

    /** Everybody near sees this dog do something short. */
    static void gebaar(ServerPlayer p, int wat, int ticks) {
        rond(p, new SnuffelPayloads.Gebaar(p.getUUID(), wat, ticks));
    }

    // =====================================================================================================================
    // every tick, and the life cycle
    // =====================================================================================================================

    /** Every tick for every player (after the game moved them). */
    static void tick(ServerPlayer p) {
        controleer(p);
        if (!p.isAlive()) {
            return;
        }
        if (!actief(p)) {
            if ((p.tickCount + p.getId()) % 20 == 0) {
                if (SnuffelKluis.heeftPost(p)) {
                    SnuffelKluis.geefPost(p);
                }
                // a memory card outside the island (a creative copy, a leftover): it only exists in a dog's pockets
                if (p.getInventory().hasAnyMatching(s -> s.is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get()))) {
                    haalKaartWeg(p);
                }
            }
            return;
        }
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null) {
            return;
        }
        if (!p.isCreative() && !p.isSpectator()) {
            Minigames.keep(p);   // (no hunger, no drowning, no free healing)
        }
        if ((p.tickCount + p.getId()) % 20 == 0) {
            geefKaart(p);
            // the last good spot: back here next time, and when the sea carries you off
            if (p.onGround() && !p.isInWater() && plaats.buiten(p.position()) <= 0) {
                Reis.onthoudEiland(p, plaats);
            }
        }
        if (!plaats.test()) {
            bewaak(p, plaats);
        }
        Snuffelen.tick(p);
        Maatjes.tick(p, plaats);
    }

    /** The sea around the real island: swim too far out and the current pushes you back; much too far and you wash ashore. */
    private static void bewaak(ServerPlayer p, Eiland.Plaats plaats) {
        if (p.isSpectator() || p.isCreative()) {
            return;
        }
        double buiten = plaats.buiten(p.position());
        int grens = plaats.opzet().grens();
        if (p.getY() < p.level().getMinY() - 8 || buiten > grens + 16) {
            Reis.zetOp(p, plaats, Reis.laatsteOfStrand(p, plaats));
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.aangespoeld").withStyle(ChatFormatting.AQUA));
        } else if (buiten > grens && p.tickCount % 10 == 0 && Duwtje.mag(p)) {
            Vec3 midden = plaats.doos().getCenter();
            Duwtje.duw(p, new Vec3(midden.x - p.getX(), 0, midden.z - p.getZ()), 0.9);
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.stroming").withStyle(ChatFormatting.AQUA));
        }
    }

    static void opLogin(ServerPlayer p) {
        // (a dead player waits for the respawn; a living one is checked at once: the island may be gone, or they may be on it)
        controleer(p);
        if (actief(p)) {
            p.refreshDimensions();
        }
        sync(p);
        Stand.stuur(p);
        // what the others on this server look like
        MinecraftServer server = p.level().getServer();
        if (server != null) {
            for (ServerPlayer q : server.getPlayerList().getPlayers()) {
                if (q != p && actief(q)) {
                    toon(q, p);
                }
            }
        }
    }

    static void opLogout(ServerPlayer p) {
        // a dog stays a dog on the island: the player file holds the dog's pockets AND the snapshot, and the next login
        // finds both. Only what lives in memory goes.
        HOUDING.remove(p.getUUID());
        BLAF_TOT.remove(p.getUUID());
        Keuze.vergeetOpen(p.getUUID());
        Snuffelen.vergeet(p.getUUID());
        traag(p, false);
        Maatjes.weg(p);
    }

    /** A dog died (only /kill and the void get through): nothing lies on the ground, the respawn gives everything back. */
    static void opDood(ServerPlayer p) {
        if (!actief(p)) {
            return;
        }
        haalKaartWeg(p);
        SnuffelData.van(p).putBoolean(DOOD, true);
        HOUDING.remove(p.getUUID());
        Snuffelen.stop(p);
        Maatjes.weg(p);
        Minigames.forget(p);
    }

    /** Did this player die as a dog (until the respawn)? */
    static boolean stierfAlsHond(Player p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr(DOOD, false);
    }

    static void opRespawn(ServerPlayer p) {
        if (SnuffelData.heeft(p)) {
            SnuffelData.van(p).remove(DOOD);
        }
        controleer(p);
        if (actief(p)) {
            p.refreshDimensions();
        }
        sync(p);
        Stand.stuur(p);
    }

    static void opStop() {
        HOUDING.clear();
        BLAF_TOT.clear();
    }

    /** (Tests, dev) the level a dog would be drawn in is the server's: the pose map is the server's too. */
    static void vergeet(ServerPlayer p) {
        HOUDING.remove(p.getUUID());
        BLAF_TOT.remove(p.getUUID());
        if (p.level() instanceof ServerLevel) {
            traag(p, false);
        }
    }
}
