package nl.juiced.guhs.feature.droombadje;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * The Wolkenbadje's activity (ontwerp ronde 10, "bad klaarmaken").
 * <ul>
 *   <li><b>The bubble ride:</b> stand on the bubble pad and a soap bubble carries you along {@link BadjeOpzet#route()}: up past
 *       the three clouds and back down by Snoes' bath. At each cloud it waits ({@link #WACHT} ticks at most, or until you
 *       click the bath thing). Sneak = hop out (the dream's slow falling brings you down gently).</li>
 *   <li><b>The bath things:</b> clicking a {@link BadspulletjeBlock} takes it along (per player; it stays on the cloud for the
 *       others).</li>
 *   <li><b>Snoes:</b> the Snoes NPC by the little bath (roleData {@link #NPC_TAG}): with all three things he dares his first
 *       bubble bath ({@link BadjeScene}); after it the piece "bad" of his dream is yours ({@link DroomVoortgang}).</li>
 * </ul>
 * The ride works without the story too (just floating for fun).
 */
public final class Badje {
    /** Player data: which bath things this player has ({@link #KEY}: soort id -> true). */
    static final String KEY = "guhs_droombadje";
    /** The roleData flag of the Snoes on this island (the template's NPC). */
    public static final String NPC_TAG = "guhs_droombadje";
    /** Blocks per tick the bubble floats; how long it waits at a cloud; how close counts as "there". */
    static final double SNELHEID = 0.32, DAAR = 0.7;
    static final int WACHT = 160;

    /** A ride: the route point it floats to, and how long it has waited at a cloud. */
    static final class Rit {
        int punt;
        int wacht;
        boolean gepakt;
    }

    static final Map<UUID, Rit> RITTEN = new ConcurrentHashMap<>();

    private Badje() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Badje::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Badje::onPraat);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> RITTEN.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> RITTEN.remove(e.getEntity().getUUID()));
    }

    // =====================================================================================================================
    // the bath things
    // =====================================================================================================================

    static CompoundTag data(ServerPlayer p) {
        CompoundTag t = p.getPersistentData().getCompoundOrEmpty(KEY);
        p.getPersistentData().put(KEY, t);
        return t;
    }

    public static boolean heeft(ServerPlayer p, BadspulletjeBlock.Soort s) {
        return data(p).getBooleanOr(s.id(), false);
    }

    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (BadspulletjeBlock.Soort s : BadspulletjeBlock.Soort.values()) {
            n += heeft(p, s) ? 1 : 0;
        }
        return n;
    }

    /** (tests, the command) forget the bath things. */
    public static void vergeet(ServerPlayer p) {
        p.getPersistentData().remove(KEY);
    }

    /** A player clicked a bath thing: it comes along (once); the bubble may float on. */
    public static void pak(ServerPlayer p, BadspulletjeBlock.Soort s, BlockPos pos) {
        ServerLevel level = (ServerLevel) p.level();
        Rit rit = RITTEN.get(p.getUUID());
        if (rit != null) {
            rit.gepakt = true;
        }
        if (heeft(p, s)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.droombadje.al").withStyle(ChatFormatting.AQUA));
            return;
        }
        data(p).putBoolean(s.id(), true);
        int n = aantal(p);
        level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 5, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.3f);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 0.9f + 0.2f * n);
        p.sendSystemMessage(Component.translatable("gui.guhs.droombadje.pak",
                Component.translatable("gui.guhs.droombadje.spulletje." + s.id()), n).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (n == BadspulletjeBlock.Soort.values().length) {
            p.sendSystemMessage(Component.translatable("gui.guhs.droombadje.alle").withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================================================================
    // the bubble ride
    // =====================================================================================================================

    public static boolean rijdt(ServerPlayer p) {
        return RITTEN.containsKey(p.getUUID());
    }

    /** Starts the ride (the pad, or the test command). */
    public static boolean start(ServerPlayer p) {
        BadjeOpzet o = BadjeOpzet.get();
        if (o == null || o.route().isEmpty() || rijdt(p)) {
            return false;
        }
        RITTEN.put(p.getUUID(), new Rit());
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 25, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1f, 1.3f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.droombadje.start").withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** Ends the ride: pop. {@code klaar} = the route's end (gently set down) rather than hopping out. */
    static void stop(ServerPlayer p, boolean klaar) {
        if (RITTEN.remove(p.getUUID()) == null) {
            return;
        }
        ServerLevel level = (ServerLevel) p.level();
        p.resetFallDistance();
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.1f);
        p.sendOverlayMessage(Component.translatable(klaar ? "gui.guhs.droombadje.klaar" : "gui.guhs.droombadje.uit")
                .withStyle(ChatFormatting.AQUA));
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !DroomReis.inDroom(p)) {
            return;
        }
        BadjeOpzet o = BadjeOpzet.get();
        if (o == null) {
            return;
        }
        Rit rit = RITTEN.get(p.getUUID());
        if (rit == null) {
            if (p.onGround() && (p.tickCount % 5) == 0 && dichtbij(p.position(), wereld(o.start()), 1.2, 1.5)
                    && DroomEilanden.dichtstbij(p.position()).equals(DroomEilanden.BADJE)) {
                start(p);
            }
            return;
        }
        if (p.isShiftKeyDown() || p.isSpectator() || p.getAbilities().flying) {
            stop(p, false);
            return;
        }
        rij(p, o, rit);
    }

    /** One tick of floating: towards the next route point; at a cloud it hovers until the thing is clicked. */
    static void rij(ServerPlayer p, BadjeOpzet o, Rit rit) {
        ServerLevel level = (ServerLevel) p.level();
        if (rit.punt >= o.route().size()) {
            stop(p, true);
            return;
        }
        Vec3 doel = wereld(o.route().get(rit.punt));
        Vec3 naar = doel.subtract(p.position());
        BadjeOpzet.Stop stop = o.stopBij(rit.punt);
        if (naar.length() < DAAR) {
            if (stop != null && !rit.gepakt && rit.wacht < WACHT && !heeft(p, stop.soort())) {
                if (rit.wacht == 0) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.droombadje.stop").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                rit.wacht++;
                p.setDeltaMovement(naar.scale(0.2).add(0, 0.01, 0));     // hover (a little against the slow fall)
            } else {
                rit.punt++;
                rit.wacht = 0;
                rit.gepakt = false;
            }
        } else {
            p.setDeltaMovement(naar.normalize().scale(Math.min(SNELHEID, naar.length())));
        }
        p.hurtMarked = true;
        p.resetFallDistance();
        if ((p.tickCount % 4) == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 0.9, p.getZ(), 4, 0.7, 0.9, 0.7, 0.01);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.8, 0.9, 0.8, 0.0);
        }
        if ((p.tickCount % 30) == 0) {
            level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }

    static Vec3 wereld(Vec3 rel) {
        return DroomEilanden.wereld(DroomEilanden.BADJE, rel);
    }

    static boolean dichtbij(Vec3 a, Vec3 b, double horizontaal, double verticaal) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz <= horizontaal * horizontaal && Math.abs(a.y - b.y) <= verticaal;
    }

    // =====================================================================================================================
    // Snoes and his bath
    // =====================================================================================================================

    private static void onPraat(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GuhNpcEntity npc) || !(event.getEntity() instanceof ServerPlayer p)
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !npc.roleData.getBooleanOr(NPC_TAG, false)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        praat(p, npc);
    }

    /** Talking to Snoes by his bath: a hint, his first bath (with all three things), or how clean he is now. */
    public static void praat(ServerPlayer p, @Nullable GuhNpcEntity snoes) {
        if (DroomVoortgang.heeft(p, DroomVoortgang.Stukje.BAD)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.droombadje.snoes.al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int n = aantal(p);
        if (n < BadspulletjeBlock.Soort.values().length) {
            p.sendSystemMessage(Component.translatable("gui.guhs.droombadje.snoes.nog", n).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        BlockPos anker = DroomEilanden.midden(DroomEilanden.BADJE);
        boolean gestart = p.level() instanceof ServerLevel && DroomReis.inDroom(p)
                && Cutscenes.speel(p, BadjeScene.BAD, anker, Rotation.NONE, Badje::klaar);
        if (!gestart) {
            klaar(p);                                  // (no scene: outside the dream or already watching) straight to the end
        }
    }

    /** After Snoes' bath: the piece "bad" of his dream. */
    static void klaar(ServerPlayer p) {
        if (DroomVoortgang.geef(p, DroomVoortgang.Stukje.BAD)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.droombadje.stukje", DroomVoortgang.aantal(p)).withStyle(ChatFormatting.GOLD));
            if (p.level() instanceof ServerLevel level) {
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
            }
        }
    }
}
