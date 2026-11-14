package nl.juiced.guhs.feature.vadsrijk.kern.veilig;

import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * Kern's safety net in the Vadsrijk (fix round 1): the unstuck key/command ({@link Losknuffelen}), the "Vast?" hint, the
 * rescue from under the map, and the safe spots ({@link VeiligePlekken}). Called from KernRegistry / KernClient.
 */
public final class Veilig {
    private Veilig() {
    }

    /** Client -> server: the unstuck key was pressed. */
    public record Vast() implements CustomPacketPayload {
        public static final Type<Vast> TYPE = new Type<>(Guhs.id("vr_kern_vast"));
        public static final StreamCodec<FriendlyByteBuf, Vast> STREAM_CODEC = StreamCodec.unit(new Vast());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        nl.juiced.guhs.world.TikWacht.post("Veilig:38", e -> Losknuffelen.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("vast").executes(c -> {
            Losknuffelen.probeer(c.getSource().getPlayerOrException());
            return 1;
        }))));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Losknuffelen.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> VeiligePlekken.vergeet());
        Schade.NA_KLAP.add(u -> {
            if (u.klap().doel() instanceof ServerPlayer p && u.schade() > 0) {
                Losknuffelen.geraakt(p);
            }
        });
    }

    public static void payloads(PayloadRegistrar r) {
        r.playToServer(Vast.TYPE, Vast.STREAM_CODEC, (v, c) -> {
            if (c.player() instanceof ServerPlayer p) {
                Losknuffelen.probeer(p);
            }
        });
    }
}
