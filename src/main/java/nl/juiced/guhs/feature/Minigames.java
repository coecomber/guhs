package nl.juiced.guhs.feature;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.juiced.guhs.feature.beauty.BeautyShow;
import nl.juiced.guhs.feature.disco.DiscoGame;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.meppen.MepGame;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.smul.SmulGame;
import nl.juiced.guhs.feature.vissen.VisWedstrijd;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.VerstopGame;

/**
 * What all the minigames share: one game at a time ({@link #refuse}), and while you play you don't get hungrier or
 * weaker than you were when you started ({@link #startKeeping}, {@link #keep}): no free healing by starting a game and
 * quitting at once. A protected player can't hurt others either (no fighting in god mode).
 */
public final class Minigames {
    public static final String BEAUTY = "beauty", RACE = "race", MEPPEN = "meppen", DISCO = "disco", GOLF = "golf", SMUL = "smul",
            VISSEN = "vissen", VERSTOP = "verstop";
    /** The 2.8 games (each feature replaces its predicate with {@link #registerGame} in its own register). */
    public static final String BAKKERIJ = "bakkerij", CRECHE = "creche", THEEHUIS = "theehuis", KAPPER = "kapper",
            STERRENWACHT = "sterrenwacht", BALLON = "ballon", KNUFFELBAD = "knuffelbad", GRIJPMACHINE = "grijpmachine";
    /** The 2.9 games (De Grote Guhspelen; phase 1 registers each as "never playing", its feature replaces that). */
    public static final String SJOELEN = "sjoelen", DOOLHOF = "doolhof", KATAPULT = "katapult", KNABBELSPELEN = "knabbelspelen",
            ELFTOCHT = "elftocht", CIRCUIT = "circuit", BEROEPEN = "beroepen";

    /** Games registered by features (2.8): id -> "is this player playing it now?". A later registration replaces an earlier one. */
    private static final Map<String, java.util.function.Predicate<ServerPlayer>> GAMES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Registers a game (2.8): {@link #playing} also asks this predicate, so {@link #refuse} keeps players to one game at
     * a time and the game protects them like the others. Registering the same id again replaces the predicate.
     */
    public static void registerGame(String id, java.util.function.Predicate<ServerPlayer> playing) {
        GAMES.put(id, playing);
    }

    /** Per player: the food, saturation and health they may not drop below while playing. */
    private static final Map<UUID, float[]> KEEP = new ConcurrentHashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(Minigames::onAttack);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> KEEP.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> KEEP.clear());
    }

    /** The game this player is in right now (null: none). */
    @Nullable
    public static String playing(ServerPlayer player) {
        if (BeautyShow.isPerforming(player)) {
            return BEAUTY;
        } else if (RaceGame.isRacing(player)) {
            return RACE;
        } else if (MepGame.isPlaying(player)) {
            return MEPPEN;
        } else if (DiscoGame.isDancing(player)) {
            return DISCO;
        } else if (GolfGame.isGolfing(player)) {
            return GOLF;
        } else if (SmulGame.isPlaying(player)) {
            return SMUL;
        } else if (VisWedstrijd.isFishing(player)) {
            return VISSEN;
        } else if (VerstopGame.isSeeking(player)) {
            return VERSTOP;
        }
        for (var game : GAMES.entrySet()) {
            if (game.getValue().test(player)) {
                return game.getKey();
            }
        }
        return null;
    }

    /** Is this player busy with a game other than {@code self}? */
    public static boolean busyElsewhere(ServerPlayer player, String self) {
        String game = playing(player);
        return game != null && !game.equals(self);
    }

    /** Says no (friendly, as {@code npc}) when the player is still in another game; true if so. */
    public static boolean refuse(ServerPlayer player, Entity npc, String self) {
        if (!busyElsewhere(player, self)) {
            return false;
        }
        GuhQuests.say(player, npc, "quest.guhs.minigame.busy");
        return true;
    }

    // --- no free healing ---------------------------------------------------------------------------------------------

    /** A game starts: from now on the player keeps (at least) the food and health they have now. */
    public static void startKeeping(ServerPlayer player) {
        KEEP.put(player.getUUID(), new float[] {player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), player.getHealth()});
        player.setAirSupply(player.getMaxAirSupply());
    }

    /** During a game: food and health don't go down (what you eat or heal on the way counts), and you never run out of air. */
    public static void keep(ServerPlayer player) {
        float[] floor = KEEP.computeIfAbsent(player.getUUID(),
                id -> new float[] {player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), player.getHealth()});
        var food = player.getFoodData();
        floor[0] = Math.max(floor[0], food.getFoodLevel());
        floor[1] = Math.max(floor[1], food.getSaturationLevel());
        floor[2] = Math.min(player.getMaxHealth(), Math.max(floor[2], player.getHealth()));
        food.setFoodLevel((int) floor[0]);
        food.setSaturation(Math.min(floor[1], floor[0]));
        if (player.getHealth() < floor[2]) {
            player.setHealth(floor[2]);
        }
        player.setAirSupply(player.getMaxAirSupply());
    }

    // --- no fighting in god mode -------------------------------------------------------------------------------------

    /**
     * Is this player protected by a game (can't get hurt)? 1.2.7: Mika-meppen too (its players can't be hurt either, and
     * whacking the Mikas is a click on their heads, which are blocks: no reason to let them hit other players or guhs).
     */
    public static boolean invulnerable(ServerPlayer player) {
        return playing(player) != null;
    }

    /** A player who can't get hurt can't hurt anyone else either. */
    public static void onAttack(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker
                && !(event.getEntity() instanceof nl.juiced.guhs.entity.GuhNpcEntity) && invulnerable(attacker)) {
            event.setCanceled(true);
        }
    }

    // --- prizes ------------------------------------------------------------------------------------------------------

    /**
     * Gives a prize (tickets, coins, a present): into the pockets, and whatever doesn't fit there drops on the ground right
     * in front of the player (never silently gone). Returns true when (part of) it had to go on the ground.
     */
    public static boolean give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (rest.isEmpty()) {
            return false;
        }
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(player.level(),
                player.getX(), player.getEyeY() - 0.3, player.getZ(), rest);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        item.setDeltaMovement(look.lengthSqr() > 1e-4 ? look.normalize().scale(0.15) : net.minecraft.world.phys.Vec3.ZERO);
        item.setPickUpDelay(20);
        item.setTarget(player.getUUID());                  // only the winner may pick it up
        player.level().addFreshEntity(item);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quest.guhs.minigame.dropped", rest.getCount(),
                rest.getHoverName()).withStyle(net.minecraft.ChatFormatting.GOLD));
        return true;
    }

    /** (Tests) forget the kept values. */
    public static void forget(Player player) {
        KEEP.remove(player.getUUID());
    }

    private Minigames() {
    }
}
