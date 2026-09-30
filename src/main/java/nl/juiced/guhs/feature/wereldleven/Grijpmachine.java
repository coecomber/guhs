package nl.juiced.guhs.feature.wereldleven;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The grijpmachine game (2.8, wereldleven). One ticket (#guhs:knus/grijptickets) opens the claw screen: steer the claw
 * over the plushies (a few seconds), drop it... the claw closes on the plushie under it, or it slips. Whatever it
 * catches is yours (Minigames.give, in front of you when your pockets are full) plus one kermisbon back (every reward +1),
 * and goes into your knuffelkast. Twenty plushies (one per real guh variant, the rare ones rarer) and now and then the
 * glitter one (harder to hold). One game at a time; a claw that isn't dropped in time drops by itself.
 */
public final class Grijpmachine {
    /** Plushies in the case. */
    public static final int PRIJZEN = 9;
    /** Steering time (ticks) on the client; the server allows a bit more before the turn is over. */
    public static final int STUURTIJD = 20 * 20, MAX_BEURT = 20 * 40;
    /** The claw grabs the nearest plushie within this distance (the case floor is 1 x 1). */
    public static final float GRIJP_BEREIK = 0.14f;
    /** The glitter plushie's chance to be the new one, and how much harder it is to hold. */
    public static final float GLITTER_KANS = 0.03f, GLITTER_GREEP = 0.5f;
    /** The prize chute (front left): no plushies there. */
    public static final float GOOT_X = 0.22f, GOOT_Z = 0.78f;
    /** Kermisbonnen back with every plushie you catch (the +1 of every reward). */
    public static final int BON_ERBIJ = 1;

    private record Beurt(BlockPos pos, long start) {
    }

    private static final Map<UUID, Beurt> BEURTEN = new ConcurrentHashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BEURTEN.clear());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> BEURTEN.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(Grijpmachine::onTick);
    }

    /** (Minigames) is this player steering a claw? */
    public static boolean speelt(ServerPlayer player) {
        return BEURTEN.containsKey(player.getUUID());
    }

    // --- the prizes -------------------------------------------------------------------------------------------------------------

    /** How common a variant's plushie is in the machine (the rare guhs are rarer plushies too). */
    public static int gewicht(String id) {
        return switch (id) {
            case "normal", "mint", "choco", "snow", "pluisguh" -> 10;
            case "teckel", "brontosaurus", "kaasmoerasguh", "asguh", "mager", "ghost" -> 6;
            case "rainbow", "starry", "brococolief", "wolk", "zeemeerguh" -> 4;
            default -> 3;    // golden, ender, koning, vahoege_ender
        };
    }

    /** A new plushie for the case, on a free spot (not in the chute, not right on top of another). */
    public static GrijpmachineBlockEntity.Prijs nieuwePrijs(RandomSource random, List<GrijpmachineBlockEntity.Prijs> al) {
        String id;
        if (random.nextFloat() < GLITTER_KANS) {
            id = WereldlevenFeature.GLITTER;
        } else {
            int total = 0;
            for (String k : WereldlevenFeature.KNUFFEL_IDS) {
                total += k.equals(WereldlevenFeature.GLITTER) ? 0 : gewicht(k);
            }
            int r = random.nextInt(total);
            id = "normal";
            for (String k : WereldlevenFeature.KNUFFEL_IDS) {
                if (k.equals(WereldlevenFeature.GLITTER)) {
                    continue;
                }
                r -= gewicht(k);
                if (r < 0) {
                    id = k;
                    break;
                }
            }
        }
        float x = 0.5f, z = 0.5f;
        for (int tries = 0; tries < 30; tries++) {
            x = 0.1f + random.nextFloat() * 0.8f;
            z = 0.1f + random.nextFloat() * 0.8f;
            if (x < GOOT_X + 0.08f && z > GOOT_Z - 0.08f) {
                continue;
            }
            boolean vrij = true;
            for (GrijpmachineBlockEntity.Prijs p : al) {
                if (Math.hypot(p.x() - x, p.z() - z) < 0.16) {
                    vrij = false;
                    break;
                }
            }
            if (vrij) {
                break;
            }
        }
        return new GrijpmachineBlockEntity.Prijs(id, x, z);
    }

    /** The prize under the claw at x/z (the nearest within reach), or -1. */
    public static int onderKlauw(List<GrijpmachineBlockEntity.Prijs> prijzen, float x, float z) {
        int best = -1;
        double bestD = GRIJP_BEREIK;
        for (int i = 0; i < prijzen.size(); i++) {
            double d = Math.hypot(prijzen.get(i).x() - x, prijzen.get(i).z() - z);
            if (d <= bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** How likely the claw holds on to a prize it closes on at distance d from its middle (right on it: 90 %). */
    public static double greep(GrijpmachineBlockEntity.Prijs prijs, float x, float z) {
        double d = Math.hypot(prijs.x() - x, prijs.z() - z);
        double kans = Math.max(0.35, Math.min(0.9, 0.95 - d * 3.5));
        return prijs.knuffel().equals(WereldlevenFeature.GLITTER) ? kans * GLITTER_GREEP : kans;
    }

    // --- a turn ---------------------------------------------------------------------------------------------------------------

    /** Right-click on a machine: pay a ticket and open the claw screen. */
    public static void start(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof GrijpmachineBlockEntity machine)) {
            return;
        }
        if (Minigames.busyElsewhere(player, Minigames.GRIJPMACHINE)) {
            player.displayClientMessage(Component.translatable("quest.guhs.minigame.busy").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        Beurt al = BEURTEN.get(player.getUUID());
        if (al == null || !al.pos().equals(pos)) {
            if (!betaal(player)) {
                player.displayClientMessage(Component.translatable("gui.guhs.wereldleven.grijp_kaartje").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
            BEURTEN.put(player.getUUID(), new Beurt(pos, player.level().getGameTime()));
            player.level().playSound(null, pos, WereldlevenFeature.GRIJPKLAUW.get(), SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        machine.vul(player.getRandom());
        machine.veranderd();
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new WereldlevenPayloads.GrijpOpen(pos, machine.prijzenTag()));
    }

    /** Takes one ticket (the hand first, then the pockets); creative players play for free. */
    static boolean betaal(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.is(KnusTags.GRIJPTICKETS)) {
                s.shrink(1);
                return true;
            }
        }
        for (ItemStack s : player.getInventory().items) {
            if (s.is(KnusTags.GRIJPTICKETS)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** The result of a drop: the prize index (-1: nothing under the claw), whether the claw held it, and the plushie. */
    public record Uitslag(int index, boolean gepakt, String knuffel) {
    }

    /**
     * The player dropped the claw at x/z (0..1 on the case floor). Works out what it catches, gives the prize, refills
     * the case, tells the client. Returns null when the player has no turn at this machine.
     */
    @Nullable
    public static Uitslag grijp(ServerPlayer player, BlockPos pos, float x, float z) {
        Beurt beurt = BEURTEN.get(player.getUUID());
        if (beurt == null || !beurt.pos().equals(pos) || !(player.level().getBlockEntity(pos) instanceof GrijpmachineBlockEntity machine)) {
            return null;
        }
        BEURTEN.remove(player.getUUID());
        x = Math.max(0f, Math.min(1f, x));
        z = Math.max(0f, Math.min(1f, z));
        machine.vul(player.getRandom());
        List<GrijpmachineBlockEntity.Prijs> prijzen = machine.prijzen();
        int i = onderKlauw(prijzen, x, z);
        boolean gepakt = i >= 0 && player.getRandom().nextDouble() < greep(prijzen.get(i), x, z);
        String knuffel = i >= 0 ? prijzen.get(i).knuffel() : "";
        ServerLevel level = player.serverLevel();
        level.playSound(null, pos, WereldlevenFeature.GRIJPKLAUW.get(), SoundSource.BLOCKS, 1f, gepakt ? 1.2f : 0.8f);
        if (gepakt) {
            prijzen.remove(i);
            machine.vul(player.getRandom());
            geefPrijs(player, knuffel);
            level.sendParticles(WereldlevenFeature.IJSJESHARTJE.get(), pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        } else {
            player.displayClientMessage(Component.translatable(i >= 0 ? "gui.guhs.wereldleven.grijp_glipt" : "gui.guhs.wereldleven.grijp_mis")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        machine.veranderd();
        Uitslag uitslag = new Uitslag(i, gepakt, knuffel);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new WereldlevenPayloads.GrijpUitslag(pos, i, gepakt, knuffel, machine.prijzenTag()));
        return uitslag;
    }

    /** The plushie + one kermisbon, the knuffelkast, the counters and the advancements. */
    static void geefPrijs(ServerPlayer player, String knuffel) {
        var block = WereldlevenFeature.knuffel(knuffel);
        if (block == null) {
            return;
        }
        ItemStack stack = new ItemStack(block);
        Minigames.give(player, stack.copy());
        Minigames.give(player, new ItemStack(ModItems.KERMISBON.get(), BON_ERBIJ));
        Knuffels.ontdek(player, stack);
        KnusVoortgang.tel(player, WereldlevenVoortgang.GRIJPEN, 1);
        WereldlevenVoortgang.toon(player, "wereldleven_grijpmachine");
        player.displayClientMessage(Component.translatable(knuffel.equals(WereldlevenFeature.GLITTER) ? "gui.guhs.wereldleven.grijp_glitter"
                : "gui.guhs.wereldleven.grijp_gepakt", stack.getHoverName()).withStyle(ChatFormatting.GOLD), true);
    }

    /** The player closed the screen without dropping: the turn is over (the ticket is spent). */
    public static void stop(ServerPlayer player, BlockPos pos) {
        Beurt beurt = BEURTEN.get(player.getUUID());
        if (beurt != null && beurt.pos().equals(pos)) {
            BEURTEN.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("gui.guhs.wereldleven.grijp_gestopt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (BEURTEN.isEmpty() || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (var e : Map.copyOf(BEURTEN).entrySet()) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || p.level().getGameTime() - e.getValue().start() > MAX_BEURT
                    || p.distanceToSqr(Vec3.atCenterOf(e.getValue().pos())) > 10 * 10) {
                BEURTEN.remove(e.getKey());
                if (p != null) {
                    GuhQuests.say(p, p, "gui.guhs.wereldleven.grijp_gestopt");
                }
            }
        }
    }

    /** (Tests) start a turn without the screen. */
    public static void testBeurt(ServerPlayer player, BlockPos pos) {
        BEURTEN.put(player.getUUID(), new Beurt(pos, player.level().getGameTime()));
    }

    private Grijpmachine() {
    }
}
