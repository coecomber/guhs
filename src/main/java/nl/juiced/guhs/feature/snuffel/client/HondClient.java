package nl.juiced.guhs.feature.snuffel.client;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.snuffel.HondEntity;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;
import nl.juiced.guhs.feature.snuffel.Snuffelen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Client: the DOG FORM as you see and steer it.
 * <ul>
 *   <li><b>Seeing.</b> The server tells which players are dogs and which dog ({@code guhs:snuffel_vorm}). Such a player is
 *   not drawn: in its place a {@link HondEntity} that exists only in this client (one per dog-player, never in the level)
 *   is drawn at exactly the player's position, turned like the player, with the player's name above it and the animation
 *   of what the player does (walking, sniffing, sitting, wagging, barking, digging). In first person there is no hand.</li>
 *   <li><b>Size.</b> A dog-player gets a dog's box and eye height on this side too ({@link Hondvorm#clientRas}): the camera
 *   is as low as the dog's eyes.</li>
 *   <li><b>Steering</b> (your own dog): hold the sniff key to walk nose to the ground, the sit and wag keys switch those
 *   poses on and off (walking ends a sit), the bark key barks, and the attack button digs (a dog breaks nothing anyway).
 *   While it digs or sits the dog stands still.</li>
 * </ul>
 */
public final class HondClient {
    /** A dog as this client was told about it. */
    record Hond(String ras, String kleur, String naam) {
    }

    private static final Map<UUID, Hond> VORMEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HOUDINGEN = new ConcurrentHashMap<>();
    /** Until which client tick a dog barks / digs. */
    private static final Map<UUID, Long> BLAFT = new ConcurrentHashMap<>(), GRAAFT = new ConcurrentHashMap<>();
    private static final Map<Player, HondEntity> KOPIEEN = new WeakHashMap<>();
    /** The breed whose size a player object was last given ("" = its own size). */
    private static final Map<Player, String> MAAT = new WeakHashMap<>();

    private static boolean zit, kwispelt;
    private static int verzonden = -1;

    private HondClient() {
    }

    // =====================================================================================================================
    // what the server says
    // =====================================================================================================================

    static void vorm(SnuffelPayloads.Vorm p) {
        if (p.actief()) {
            VORMEN.put(p.speler(), new Hond(p.ras(), p.kleur(), p.naam()));
        } else {
            VORMEN.remove(p.speler());
            HOUDINGEN.remove(p.speler());
            BLAFT.remove(p.speler());
            GRAAFT.remove(p.speler());
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && p.speler().equals(mc.player.getUUID())) {
            zit = false;
            kwispelt = false;
            verzonden = -1;
            SnuffelHud.meter(SnuffelPayloads.Meter.NIETS);
            if (p.actief()) {
                // (no music on the island: whatever was playing stops here)
                mc.getMusicManager().stopPlaying();
            }
        }
    }

    static void houding(SnuffelPayloads.Houding p) {
        if (p.vlaggen() == 0) {
            HOUDINGEN.remove(p.speler());
        } else {
            HOUDINGEN.put(p.speler(), p.vlaggen());
        }
    }

    static void gebaar(SnuffelPayloads.Gebaar p) {
        (p.wat() == Hondvorm.GRAAF ? GRAAFT : BLAFT).put(p.speler(), SnuffelClient.ticks() + p.ticks());
    }

    static void wis() {
        VORMEN.clear();
        HOUDINGEN.clear();
        BLAFT.clear();
        GRAAFT.clear();
        KOPIEEN.clear();
        MAAT.clear();
        zit = false;
        kwispelt = false;
        verzonden = -1;
    }

    // =====================================================================================================================
    // who is a dog
    // =====================================================================================================================

    @Nullable
    static Hond van(@Nullable Player p) {
        return p == null ? null : VORMEN.get(p.getUUID());
    }

    /** The breed of the dog this player is drawn as (null: no dog). The hook of {@link Hondvorm#clientRas}. */
    @Nullable
    static Honden.Ras ras(Player p) {
        Hond h = VORMEN.get(p.getUUID());
        return h == null ? null : Honden.ras(h.ras());
    }

    /** Is the local player a dog? */
    public static boolean eigenHond() {
        return van(Minecraft.getInstance().player) != null;
    }

    static boolean graaftNu(Player p) {
        return GRAAFT.getOrDefault(p.getUUID(), -1L) >= SnuffelClient.ticks();
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer ik = mc.player;
        if (level == null || ik == null) {
            return;
        }
        // every player object has the size that goes with what it is now (a new object after a respawn or another dimension too)
        for (Player p : level.players()) {
            Hond h = VORMEN.get(p.getUUID());
            String moet = h == null ? "" : h.ras();
            if (!moet.equals(MAAT.getOrDefault(p, ""))) {
                MAAT.put(p, moet);
                p.refreshDimensions();
            }
        }
        if (van(ik) == null) {
            SnuffelKeys.leeg();
            return;
        }
        boolean vrij = mc.screen == null && !Cutscenes.bezig(ik);
        int vlaggen = 0;
        if (vrij) {
            while (SnuffelKeys.ZIT.consumeClick()) {
                zit = !zit;
            }
            while (SnuffelKeys.KWISPEL.consumeClick()) {
                kwispelt = !kwispelt;
            }
            while (SnuffelKeys.BLAF.consumeClick()) {
                ClientPacketDistributor.sendToServer(new SnuffelPayloads.Doe(Hondvorm.BLAF));
            }
            while (SnuffelKeys.BOEKJE.consumeClick()) {
                mc.setScreen(new BoekjeScherm());
            }
            if (SnuffelKeys.SNUFFEL.isDown()) {
                vlaggen |= Hondvorm.SNUFFELT;
                zit = false;
            }
            if (zit && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                    || mc.options.keyJump.isDown())) {
                zit = false;   // (walking off ends a sit)
            }
        } else {
            SnuffelKeys.leeg();
        }
        if (zit) {
            vlaggen |= Hondvorm.ZIT;
        }
        if (kwispelt) {
            vlaggen |= Hondvorm.KWISPELT;
        }
        if (vlaggen != verzonden) {
            verzonden = vlaggen;
            ClientPacketDistributor.sendToServer(new SnuffelPayloads.Actie(vlaggen));
        }
    }

    /** The attack button is the dig button, and nothing else, for a dog. */
    static void toets(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || van(mc.player) == null || mc.player.getAbilities().instabuild) {
            return;
        }
        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!graaftNu(mc.player) && mc.player.onGround()) {
                zit = false;
                ClientPacketDistributor.sendToServer(new SnuffelPayloads.Doe(Hondvorm.GRAAF));
            }
        } else if (event.isPickBlock()) {
            event.setCanceled(true);
        } else if (event.isUseItem()) {
            event.setSwingHand(false);   // (a paw does not swing; what the click does is the server's)
        }
    }

    /** A dog that digs or sits stands still. */
    static void stilStaan(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer p)) {
            return;
        }
        boolean stil = van(p) != null && graaftNu(p);
        if (stil) {
            if (!(p.input instanceof StilInput)) {
                p.input = new StilInput();
            }
        } else if (p.input instanceof StilInput) {
            p.input = new net.minecraft.client.player.KeyboardInput(Minecraft.getInstance().options);
        }
    }

    /** An input that stands still (the base ClientInput never reads the keys). */
    private static final class StilInput extends net.minecraft.client.player.ClientInput {
    }

    static void hand(RenderHandEvent event) {
        if (eigenHond()) {
            event.setCanceled(true);
        }
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    /** The stand-in of this player (made when needed; a new one when the level changed). */
    @Nullable
    private static HondEntity kopie(Player p, Hond h) {
        HondEntity k = KOPIEEN.get(p);
        if (k == null || k.level() != p.level()) {
            k = SnuffelFeature.SNUFFEL_HOND.get().create(p.level(), EntitySpawnReason.TRIGGERED);
            if (k == null) {
                return null;
            }
            KOPIEEN.put(p, k);
        }
        if (!h.ras().equals(k.ras()) || !h.kleur().equals(k.kleur())) {
            k.zetHond(h.ras(), h.kleur(), false);
        }
        return k;
    }

    /** In place of a dog-player its dog is drawn, exactly where the player is. */
    static void teken(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !(level.getEntity(event.getRenderState().id) instanceof Player p)) {
            return;
        }
        Hond h = van(p);
        if (h == null) {
            return;
        }
        event.setCanceled(true);
        event.getRenderState().shadowPieces.clear();   // (a player's shadow under a dog is too big; the dog brings its own)
        if (p.isInvisible() || p.isSpectator()) {
            return;
        }
        HondEntity k = kopie(p, h);
        if (k == null) {
            return;
        }
        float pt = event.getPartialTick();
        k.tickCount = p.tickCount;
        k.setPos(p.getX(), p.getY(), p.getZ());
        k.xo = p.xo;
        k.yo = p.yo;
        k.zo = p.zo;
        k.xOld = p.xOld;
        k.yOld = p.yOld;
        k.zOld = p.zOld;
        k.setYRot(p.getYRot());
        k.yRotO = p.yRotO;
        k.setXRot(p.getXRot());
        k.xRotO = p.xRotO;
        k.yBodyRot = p.yBodyRot;
        k.yBodyRotO = p.yBodyRotO;
        k.yHeadRot = p.yHeadRot;
        k.yHeadRotO = p.yHeadRotO;
        k.hurtTime = p.hurtTime;
        k.deathTime = p.deathTime;
        k.setOnGround(p.onGround());
        k.zetHouding(HOUDINGEN.getOrDefault(p.getUUID(), 0));
        k.loopt = p.walkAnimation.speed(pt) > 0.12f;
        long nu = SnuffelClient.ticks();
        k.graafTot = GRAAFT.getOrDefault(p.getUUID(), -1L) >= nu ? p.tickCount + 2 : -1;
        k.blafTot = BLAFT.getOrDefault(p.getUUID(), -1L) >= nu ? p.tickCount + 2 : -1;
        // the dog carries the player's name (your own is not drawn, like your own name never is)
        k.setCustomName(p.getDisplayName());
        k.setCustomNameVisible(p != mc.player && !p.isDiscrete());
        var dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(k, pt);
        state.lightCoords = event.getRenderState().lightCoords;
        dispatcher.submit(state, mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState, 0, 0, 0, event.getPoseStack(),
                event.getSubmitNodeCollector());
    }

    /** (For the screens) a dog of the local player's choice, or its puppy. */
    @Nullable
    static HondEntity maak(String ras, String kleur, boolean pup) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        HondEntity e = SnuffelFeature.SNUFFEL_HOND.get().create(mc.level, EntitySpawnReason.TRIGGERED);
        if (e != null) {
            e.zetHond(ras, kleur, pup);
        }
        return e;
    }

    /** How long a dig locks the dog (the server's number). */
    static int graafTicks() {
        return Snuffelen.GRAAF_TICKS;
    }
}
