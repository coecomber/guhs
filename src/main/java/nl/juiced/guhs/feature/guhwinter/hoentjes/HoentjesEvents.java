package nl.juiced.guhs.feature.guhwinter.hoentjes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The hoentjes around a player (every half second): the NOWAY chat line when one comes into view, and now and then the
 * rare natural spawn ({@link HoentjesSpawner#misschien}).
 * <p>
 * NOWAY: the first time a player is within {@link #NOWAY_BEREIK} blocks of a (wild) hoentje that they can see (line of
 * sight), they get a chat line only they see, with a little jingle only they hear:
 * <i>"NOWAY EEN WATERHOEN!????"</i> or, even more, <i>"NOWAYYY EEN ALPINE HOENTJE?!?!??!?!"</i>. Then that species stays
 * quiet for that player for {@link #NOWAY_AFKOEL} ticks (ten minutes). Scene hoentjes ({@link Hoentje#isScene}) never
 * trigger it: in the kerstwandeling the guhs shout it themselves.
 */
public final class HoentjesEvents {
    public static final double NOWAY_BEREIK = 12;
    public static final int NOWAY_AFKOEL = 20 * 60 * 10;

    /** Per player: the server tick of their last NOWAY for the waterhoen / the alpine hoentje. */
    private static final Map<UUID, long[]> NOWAY = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 10 != 0) {
            return;
        }
        if (!player.isSpectator()) {
            kijk(player);
        }
        if ((player.tickCount + player.getId()) % HoentjesSpawner.TIJD == 0) {
            HoentjesSpawner.misschien(player);
        }
    }

    /** Looks for hoentjes the player can see (within NOWAY_BEREIK) and says NOWAY when it may. */
    public static void kijk(ServerPlayer player) {
        long[] laatst = NOWAY.computeIfAbsent(player.getUUID(), k -> new long[] {-NOWAY_AFKOEL, -NOWAY_AFKOEL});
        int nu = player.level().getServer().getTickCount();
        for (Hoentje h : player.level().getEntitiesOfClass(Hoentje.class, player.getBoundingBox().inflate(NOWAY_BEREIK),
                h -> h.isAlive() && !h.isScene() && h.distanceToSqr(player) <= NOWAY_BEREIK * NOWAY_BEREIK)) {
            int s = GuhwinterHoentjes.WATERHOEN.equals(h.soort()) ? 0 : 1;
            if (nu - laatst[s] >= NOWAY_AFKOEL && player.hasLineOfSight(h)) {
                laatst[s] = nu;
                noway(player, h.soort());
            }
        }
    }

    /**
     * The NOWAY chat line (and its jingle) for this player only. Public: a story step may call it too. {@code soort}:
     * "waterhoen" or "alpinehoen".
     */
    public static void noway(ServerPlayer player, String soort) {
        boolean alpine = GuhwinterHoentjes.ALPINEHOEN.equals(GuhwinterHoentjes.soort(soort));
        String k = "gui.guhs.guhwinter_hoentjes.noway." + (alpine ? "alpinehoen" : "waterhoen");
        MutableComponent regel;
        if (alpine) {
            regel = Component.literal("✦✧✦ ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable(k).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" ✦✧✦").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal(" ").append(Component.translatable(k + ".sub")).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        } else {
            regel = Component.literal("✦ ").withStyle(ChatFormatting.AQUA)
                    .append(Component.translatable(k).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
                    .append(Component.literal(" ✦").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ").append(Component.translatable(k + ".sub")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        player.sendSystemMessage(regel);
        player.connection.send(new ClientboundSoundPacket(alpine ? GuhwinterHoentjes.NOWAY_EXTREEM : GuhwinterHoentjes.NOWAY,
                SoundSource.NEUTRAL, player.getX(), player.getY(), player.getZ(), 0.8f, 1.0f, player.getRandom().nextLong()));
    }

    /** Forgets this player's NOWAY cool-downs (op command). */
    public static void vergeet(UUID speler) {
        NOWAY.remove(speler);
    }

    private HoentjesEvents() {
    }
}
