package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 1.3.2: sleeping in the logeerbedje inside a Guhhuisje counts as normal sleeping for the world the huisje stands in.
 * <ul>
 *   <li>The player really sleeps (vanilla {@code startSleepInBed} in the room: the lying pose, the fade, the statistic,
 *   the advancement trigger, the "time since rest" reset that keeps phantoms away). The room dimension's bed rule lets a
 *   bed be used at any time and never sets a spawn point, so this class only lets you lie down while it is dark in the
 *   huisje's own world ({@link #doel}).</li>
 *   <li>The room dimension itself never skips a night (mixin: its sleeping list is not kept). Instead, every tick the
 *   sleepers of the rooms are counted WITH the players of the huisje's world: when enough of them sleep (the game rule
 *   {@code players_sleeping_percentage}, exactly as there) and have slept long enough, that world's night is skipped the
 *   vanilla way (its clock to the wake-up marker when the game rule advance_time is on, its weather cleared when
 *   advance_weather is on and it rains), everybody who sleeps there and in the rooms wakes up.</li>
 *   <li>When it gets light there in another way (somebody else's sleep, the morning), the sleepers in the rooms wake up.</li>
 * </ul>
 * The huisje's world is the dimension it stands in when that dimension has a day clock (the overworld, the Guhmensie);
 * a huisje in a dimension without one (the Guhmaag, the Barbecuether...) counts for the overworld, the clock its residents
 * live by ({@link HuisjeGoal#dagdeel}).
 */
public final class BinnenSlaap {
    /** Per world: the last "n of m sleeping" that was announced. */
    private static final Map<ResourceKey<Level>, Long> GEMELD = new HashMap<>();

    private BinnenSlaap() {
    }

    static void vergeet() {
        GEMELD.clear();
    }

    /** The world whose night a sleeper in this huisje's room sleeps through. */
    public static ServerLevel doel(MinecraftServer s, Huisje h) {
        ServerLevel thuis = s.getLevel(h.dim);
        return thuis != null && !thuis.dimensionType().hasFixedTime() && thuis.dimensionType().defaultClock().isPresent() ? thuis : s.overworld();
    }

    /** How many sleepers are needed among this many players (vanilla SleepStatus#sleepersNeeded). */
    public static int nodig(int actief, int procent) {
        return Math.max(1, Mth.ceil(actief * procent / 100.0F));
    }

    /** A click on the logeerbedje. */
    static void slaap(ServerPlayer p, Huisje h, BinnenKamer k) {
        if (p.isSleeping() || !(p.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer s = level.getServer();
        if (!doel(s, h).isDarkOutside()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.slaap.dag").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        BlockPos hoofd = Binnen.oorsprong(h.cel).offset(k.logeerHoofd());
        BlockState bed = level.getBlockState(hoofd);
        if (!(bed.getBlock() instanceof BedBlock)) {
            return;
        }
        if (bed.getValue(BedBlock.OCCUPIED)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.slaap.bezet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        p.startSleepInBed(hoofd).ifLeft(probleem -> {
            if (probleem.message() != null) {
                p.sendOverlayMessage(probleem.message());
            }
        }).ifRight(ok -> p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.slaap.welterusten").withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    static void tick(MinecraftServer s) {
        ServerLevel kamers = s.getLevel(Binnen.DIM);
        if (kamers == null) {
            return;
        }
        Map<ServerLevel, List<ServerPlayer>> slapers = new LinkedHashMap<>();
        for (ServerPlayer p : List.copyOf(kamers.players())) {
            if (!p.isSleeping()) {
                continue;
            }
            Huisje h = Binnen.kamerVan(p);
            ServerLevel doel = h == null ? s.overworld() : doel(s, h);
            if (!doel.isDarkOutside()) {
                wek(p);   // (it got light there: good morning)
                continue;
            }
            slapers.computeIfAbsent(doel, x -> new ArrayList<>()).add(p);
        }
        if (slapers.isEmpty()) {
            if (!GEMELD.isEmpty()) {
                GEMELD.clear();
            }
            return;
        }
        int procent = s.overworld().getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        GEMELD.keySet().removeIf(dim -> slapers.keySet().stream().noneMatch(l -> l.dimension() == dim));
        for (Map.Entry<ServerLevel, List<ServerPlayer>> e : slapers.entrySet()) {
            ServerLevel doel = e.getKey();
            List<ServerPlayer> hier = e.getValue();
            int actief = hier.size(), slaapt = hier.size(), diep = (int) hier.stream().filter(ServerPlayer::isSleepingLongEnough).count();
            for (ServerPlayer q : doel.players()) {
                if (!q.isSpectator()) {
                    actief++;
                    if (q.isSleeping()) {
                        slaapt++;
                        if (q.isSleepingLongEnough()) {
                            diep++;
                        }
                    }
                }
            }
            int nodig = nodig(actief, procent);
            if (procent > 100) {
                continue;   // (the game rule says nobody sleeps through the night)
            }
            long stand = ((long) slaapt << 32) | nodig;
            Long vorige = GEMELD.put(doel.dimension(), stand);
            if (vorige == null || vorige != stand) {
                Component bericht = slaapt >= nodig ? Component.translatable("sleep.skipping_night")
                        : Component.translatable("sleep.players_sleeping", slaapt, nodig);
                hier.forEach(q -> q.sendOverlayMessage(bericht));
                doel.players().forEach(q -> q.sendOverlayMessage(bericht));
            }
            if (slaapt >= nodig && diep >= nodig) {
                wordtOchtend(s, doel, hier);
                GEMELD.remove(doel.dimension());
            }
        }
    }

    /** Enough sleepers: the night of this world is over, the vanilla way (ServerLevel#tick). */
    private static void wordtOchtend(MinecraftServer s, ServerLevel doel, List<ServerPlayer> inKamers) {
        Optional<Holder<WorldClock>> klok = doel.dimensionType().defaultClock();
        if (doel.getGameRules().get(GameRules.ADVANCE_TIME) && klok.isPresent()) {
            var sprong = net.neoforged.neoforge.event.EventHooks.onSleepFinished(doel,
                    new net.neoforged.neoforge.common.util.ClockAdjustment.Marker(ClockTimeMarkers.WAKE_UP_FROM_SLEEP));
            if (sprong != null) {
                sprong.apply(s.clockManager(), klok.get());
            }
        }
        for (ServerPlayer q : List.copyOf(doel.players())) {
            if (q.isSleeping()) {
                q.stopSleepInBed(false, false);
            }
        }
        doel.updateSleepingPlayerList();
        if (doel.getGameRules().get(GameRules.ADVANCE_WEATHER) && doel.isRaining()) {
            doel.resetWeatherCycle();
        }
        inKamers.forEach(BinnenSlaap::wek);
    }

    private static void wek(ServerPlayer p) {
        p.stopSleepInBed(false, false);
        p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.slaap.ochtend").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
