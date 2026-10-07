package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The lobby parkour (players; not the Guh-parkour for guhs): from the start block under the PARKOUR_START anchor, over the
 * roofs, to the finish block on the shop roof. The three kinds of blocks do the work, wherever they stand:
 * <ul>
 *   <li>standing on {@code guhs:lobby_parkour_start} arms the clock; it starts the moment you step off;</li>
 *   <li>{@code guhs:lobby_parkour_tussenpunt}: a checkpoint; a run needs {@link #TUSSENPUNTEN} different ones (no short cuts);</li>
 *   <li>{@code guhs:lobby_parkour_finish}: the end. The time counts in server ticks.</li>
 * </ul>
 * Per player: the personal best and the number of finishes ({@code PxData "lobby"}); the first finish pays
 * {@link #MUNTJES} muntjes (once-key {@code lobby:parkour}) and grants quest/lobby_parkour. Every finish goes to the
 * world's top 3 (Scorebord board {@link #BORD}). Flying, gliding, riding, being moved far in one tick or teleported, a
 * potion effect that moves you, or standing on the ground again away from the start plate (fallen off, or walked away)
 * ends the run: nothing is lost, just start again. During a run no item from the pockets can be used. Any number of
 * players run at the same time.
 */
public final class LobbyParkour {
    public static final int MUNTJES = 50, TUSSENPUNTEN = 3;
    public static final String SLEUTEL = "lobby:parkour", BORD = "lobby_parkour";
    private static final String BEST = "ParkourBest", KEREN = "ParkourKeren";
    private static final int MAX_TICKS = 20 * 60 * 15;
    private static final double MAX_SPRONG = 12.0, GEVALLEN_AFSTAND = 6.0;

    /** A run: armed on the start block (start < 0), then running. */
    private static final class Loop {
        long start = -1;
        BlockPos plaat;
        final Set<BlockPos> tussen = new HashSet<>();
        Vec3 laatst;
        long melding;
    }

    private static final Map<UUID, Loop> LOPEN = new ConcurrentHashMap<>();

    /** The personal best in ticks (0: never finished). */
    public static int best(ServerPlayer p) {
        return LobbySlice.data(p).getIntOr(BEST, 0);
    }

    public static int keren(ServerPlayer p) {
        return LobbySlice.data(p).getIntOr(KEREN, 0);
    }

    /** Ever finished? */
    public static boolean gehaald(ServerPlayer p) {
        return Muntjes.isVerdiend(p, SLEUTEL) || best(p) > 0;
    }

    public static boolean bezig(ServerPlayer p) {
        Loop l = LOPEN.get(p.getUUID());
        return l != null && l.start >= 0;
    }

    /** "0:42.35" (a tick is five hundredths). */
    public static String tijd(int ticks) {
        int honderdsten = Math.max(0, ticks) * 5;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%02d", honderdsten / 6000, (honderdsten / 100) % 60, honderdsten % 100);
    }

    public static Component tijdTekst(int ticks) {
        return Component.literal(tijd(ticks));
    }

    /** Every tick for a player on the plaza: looks at the block they stand on. */
    static void tick(ServerPlayer p) {
        BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        boolean staat = Math.abs(p.getY() - (onder.getY() + 1)) < 0.05;
        stap(p, staat ? p.level().getBlockState(onder) : null, onder, p.level().getGameTime());
    }

    /**
     * One step of the state machine. {@code onder}: the block the player stands on (null: in the air), {@code nu}: the time
     * in ticks. (Game tests call this directly.)
     */
    static void stap(ServerPlayer p, BlockState onder, BlockPos pos, long nu) {
        Loop l = LOPEN.get(p.getUUID());
        if (onder != null && onder.is(LobbySlice.PARKOUR_START.get())) {
            if (l == null || l.start >= 0) {
                l = new Loop();
                LOPEN.put(p.getUUID(), l);
                p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.parkour.klaar").withStyle(ChatFormatting.GREEN));
                PxGeluid.speel(p, LobbySlice.PARKOUR_START_GELUID.get(), SoundSource.PLAYERS, 0.7f, 1f);
            }
            l.laatst = p.position();
            l.plaat = pos.immutable();
            return;
        }
        if (l == null) {
            return;
        }
        if (l.start < 0) {
            l.start = nu;   // stepped off the start block: the clock runs
        }
        int ticks = (int) Math.min(Integer.MAX_VALUE, nu - l.start);
        if (p.isFallFlying() || p.getAbilities().flying || p.isPassenger() || p.isSpectator()) {
            stop(p, "gui.guhs.lobby.parkour.vliegen");
            return;
        }
        if (l.laatst != null && l.laatst.distanceTo(p.position()) > MAX_SPRONG) {
            stop(p, "gui.guhs.lobby.parkour.weg");
            return;
        }
        if (hulp(p)) {
            stop(p, "gui.guhs.lobby.parkour.hulp");
            return;
        }
        if (ticks > MAX_TICKS) {
            stop(p, "gui.guhs.lobby.parkour.te_lang");
            return;
        }
        l.laatst = p.position();
        boolean parkourBlok = onder != null && (onder.is(LobbySlice.PARKOUR_TUSSENPUNT.get()) || onder.is(LobbySlice.PARKOUR_FINISH.get()));
        if (onder != null && !parkourBlok && l.plaat != null && pos.getY() <= l.plaat.getY()
                && Math.hypot(pos.getX() - l.plaat.getX(), pos.getZ() - l.plaat.getZ()) > GEVALLEN_AFSTAND) {
            // back on the ground, away from the start plate: fell off (or just walked away); the course never touches the ground
            stop(p, "gui.guhs.lobby.parkour.gevallen");
            return;
        }
        if (onder != null && onder.is(LobbySlice.PARKOUR_TUSSENPUNT.get())) {
            if (l.tussen.add(pos.immutable())) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.parkour.tussenpunt", l.tussen.size(), TUSSENPUNTEN).withStyle(ChatFormatting.YELLOW));
                PxGeluid.speel(p, LobbySlice.PARKOUR_START_GELUID.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
                l.melding = nu + 30;
            }
        } else if (onder != null && onder.is(LobbySlice.PARKOUR_FINISH.get())) {
            if (l.tussen.size() >= TUSSENPUNTEN) {
                LOPEN.remove(p.getUUID());
                finish(p, Math.max(1, ticks));
            } else if (nu >= l.melding) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.parkour.sluiproute", l.tussen.size(), TUSSENPUNTEN).withStyle(ChatFormatting.RED));
                l.melding = nu + 30;
            }
            return;
        }
        if (nu >= l.melding && nu % 4 == 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.parkour.loopt", tijdTekst(ticks), l.tussen.size(), TUSSENPUNTEN).withStyle(ChatFormatting.AQUA));
        }
    }

    /** Does something other than the player's own legs help right now (a potion effect that moves you, a riptide spin)? */
    static boolean hulp(ServerPlayer p) {
        return p.isAutoSpinAttack() || p.hasEffect(MobEffects.SPEED) || p.hasEffect(MobEffects.JUMP_BOOST) || p.hasEffect(MobEffects.SLOW_FALLING)
                || p.hasEffect(MobEffects.LEVITATION);
    }

    /**
     * The player uses an item (right-click in the air): true = not now. On the start plate and during a run nothing from the
     * pockets is used (ender pearls, wind charges, potions, rockets): the time on the world board is a time on foot.
     */
    static boolean geenItems(ServerPlayer p) {
        if (!LOPEN.containsKey(p.getUUID()) || p.getAbilities().instabuild) {
            return false;
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.lobby.parkour.hulp").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** The player was teleported (an ender pearl thrown earlier, a chorus fruit, a command): the run is over. */
    static void geteleporteerd(ServerPlayer p) {
        stop(p, "gui.guhs.lobby.parkour.weg");
    }

    private static void stop(ServerPlayer p, String waarom) {
        if (LOPEN.remove(p.getUUID()) != null) {
            p.sendOverlayMessage(Component.translatable(waarom).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The player left the plaza (a game, home, logged out): the run is over, silently. */
    static void vergeet(ServerPlayer p) {
        LOPEN.remove(p.getUUID());
    }

    /** A finished run of {@code ticks}. Returns true when it is a new personal best. */
    static boolean finish(ServerPlayer p, int ticks) {
        CompoundTag d = LobbySlice.data(p);
        int oud = d.getIntOr(BEST, 0);
        boolean record = oud <= 0 || ticks < oud;
        if (record) {
            d.putInt(BEST, ticks);
        }
        d.putInt(KEREN, d.getIntOr(KEREN, 0) + 1);
        PxData.vuil(p.level().getServer());
        boolean eerste = Muntjes.verdienEens(p, SLEUTEL, MUNTJES);
        GuhAdvancements.grant(p, "lobby_parkour");
        Component tijd = tijdTekst(ticks).copy().withStyle(ChatFormatting.WHITE);
        PxGeluid.titel(p, Component.translatable("gui.guhs.lobby.parkour.finish.titel").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.translatable(record ? "gui.guhs.lobby.parkour.finish.record" : "gui.guhs.lobby.parkour.finish.tijd", tijd).withStyle(ChatFormatting.YELLOW), 50);
        if (eerste) {
            p.sendSystemMessage(Component.translatable("gui.guhs.lobby.parkour.chat.eerste", tijd, MUNTJES).withStyle(ChatFormatting.AQUA));
        } else if (record) {
            p.sendSystemMessage(Component.translatable("gui.guhs.lobby.parkour.chat.record", tijd, tijdTekst(oud)).withStyle(ChatFormatting.AQUA));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.lobby.parkour.chat.tijd", tijd, tijdTekst(oud)).withStyle(ChatFormatting.AQUA));
        }
        PxGeluid.speel(p, LobbySlice.PARKOUR_FINISH_GELUID.get(), SoundSource.PLAYERS, 0.9f, 1f);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 24, 0.6, 0.8, 0.6, 0.1);
            level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1.5, p.getZ(), 30, 0.3, 0.3, 0.3, 0.12);
        }
        Scorebord.submit(p, BORD, ticks, true);
        LobbyBorden.meteen(p);
        return record;
    }

    /** (Dev) forgets this player's parkour results (not the muntjes: a once-key stays paid). */
    static void reset(ServerPlayer p) {
        CompoundTag d = LobbySlice.data(p);
        d.remove(BEST);
        d.remove(KEREN);
        PxData.vuil(p.level().getServer());
        LOPEN.remove(p.getUUID());
        LobbyBorden.meteen(p);
    }

    private LobbyParkour() {
    }
}
