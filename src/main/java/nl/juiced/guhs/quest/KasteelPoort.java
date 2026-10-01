package nl.juiced.guhs.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ServerChatEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhSeatEntity;

/**
 * The two gate guards of the guh castle only let friends of the guhs in. Prove it by being nice and vadsig: sit still
 * on the guh bench by the gate for 30 seconds, or say the secret guh word ("njeg") in the chat. The guards give hints.
 * Friends are remembered forever (for every castle). Everyone else gets gently pushed back out of the gate.
 */
public final class KasteelPoort {
    public static final String FRIEND = "guhs_guhvriend";
    /** How long you have to sit (vadsig) on the bench. */
    public static final int SIT_TICKS = 20 * 30;
    private static final double REACH = 20;
    /** Who's been sitting on a bench near the gate for how long (in ticks). */
    private static final Map<UUID, Integer> SITTING = new HashMap<>();

    public static boolean isFriend(net.minecraft.world.entity.player.Player player) {
        return GuhQuests.saved(player).getBooleanOr(FRIEND, false);
    }

    public static void makeFriend(ServerPlayer player, GuhNpcEntity guard, String how) {
        if (isFriend(player)) {
            return;
        }
        GuhQuests.saved(player).putBoolean(FRIEND, true);
        GuhQuests.say(player, guard, "quest.guhs.poort.welcome_" + how);
        player.level().playSound(null, guard.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
        player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, player.getX(), player.getY() + 2, player.getZ(), 8, 0.5, 0.3, 0.5, 0);
        GuhAdvancements.grant(player, "guhvriend");
    }

    /** Right-clicking a guard: a friendly nod, or the rules and a hint. */
    public static void talk(GuhNpcEntity guard, ServerPlayer player) {
        if (isFriend(player)) {
            GuhQuests.say(player, guard, "quest.guhs.poort.friend");
        } else {
            GuhQuests.say(player, guard, "quest.guhs.poort.halt");
            GuhQuests.say(player, guard, "quest.guhs.poort.hint" + (1 + player.getRandom().nextInt(3)));
        }
    }

    /** The secret guh word in the chat near a guard: "njeg", or its English "nyeg" (1.2.0: the English hints spell that). */
    public static final java.util.List<String> GEHEIM_WOORD = java.util.List.of("njeg", "nyeg");

    /** Does this chat line say the secret guh word? */
    public static boolean zegtGeheimWoord(String tekst) {
        String t = tekst.toLowerCase(java.util.Locale.ROOT);
        return GEHEIM_WOORD.stream().anyMatch(t::contains);
    }

    /** "njeg" in the chat near a guard: the secret guh word. */
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (isFriend(player) || !zegtGeheimWoord(event.getRawText())) {
            return;
        }
        List<GuhNpcEntity> guards = guards(player.level(), player.position(), REACH);
        if (!guards.isEmpty()) {
            GuhNpcEntity guard = guards.get(0);
            player.level().getServer().execute(() -> makeFriend(player, guard, "word"));
        }
    }

    private static List<GuhNpcEntity> guards(ServerLevel level, Vec3 near, double reach) {
        return level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(near, near).inflate(reach),
                n -> n.getKind() == GuhNpcEntity.Kind.POORTWACHTER);
    }

    /** Every few ticks (from each guard): count the bench-sitters, and keep strangers out of the gate. */
    public static void tick(GuhNpcEntity guard) {
        ServerLevel level = (ServerLevel) guard.level();
        if (!guard.roleData.contains("GateYaw")) {
            // the way the gate faces, fixed from the moment it's placed (the guard itself turns to look at people)
            guard.roleData.putFloat("GateYaw", guard.getYRot());
        }
        if (guard.tickCount % 5 != 0) {
            return;
        }
        // the gate: between this guard and the other one, the castle is behind them (they look outwards)
        List<GuhNpcEntity> pair = guards(level, guard.position(), 16);
        pair.remove(guard);
        if (pair.isEmpty() || guard.getUUID().compareTo(pair.get(0).getUUID()) > 0) {
            return; // one of the two does the work
        }
        GuhNpcEntity other = pair.get(0);
        Vec3 mid = guard.position().add(other.position()).scale(0.5);
        float gateYaw = guard.roleData.getFloatOr("GateYaw", 0.0F);
        Vec3 inward = Vec3.directionFromRotation(0, gateYaw).reverse();
        Vec3 across = new Vec3(-inward.z, 0, inward.x);
        double halfWidth = guard.position().distanceTo(other.position()) / 2 + 1.5;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(mid, mid).inflate(REACH))) {
            if (player.isSpectator() || player.getAbilities().instabuild) {
                continue;
            }
            // a vadsig rest on the bench
            if (!isFriend(player) && guard.tickCount % 20 == 0) {
                if (player.getVehicle() instanceof GuhSeatEntity) {
                    int sat = SITTING.merge(player.getUUID(), 20, Integer::sum);
                    player.sendOverlayMessage(Component.translatable("quest.guhs.poort.sitting", sat / 20, SIT_TICKS / 20)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    if (sat >= SIT_TICKS) {
                        SITTING.remove(player.getUUID());
                        makeFriend(player, guard, "lazy");
                    }
                } else {
                    SITTING.remove(player.getUUID());
                }
            }
            // strangers who walk through the gate are gently put back outside
            Vec3 rel = player.position().subtract(mid);
            double in = rel.dot(inward), side = Math.abs(rel.dot(across));
            if (!isFriend(player) && in > 0.3 && in < 10 && side < halfWidth && Math.abs(rel.y) < 6) {
                Vec3 back = mid.subtract(inward.scale(3));
                player.teleportTo(level, back.x, mid.y, back.z, java.util.Set.of(), gateYaw, player.getXRot(), true);
                player.sendOverlayMessage(Component.translatable("quest.guhs.poort.pushed").withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.NEUTRAL, 0.6f, 1.4f);
            }
        }
    }

    public static void forget(UUID player) {
        SITTING.remove(player);
    }

    private KasteelPoort() {
    }
}
