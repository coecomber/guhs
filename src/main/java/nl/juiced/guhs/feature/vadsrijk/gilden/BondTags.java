package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-GILDEN: the bond badge by the name, while in the Vadsrijk: {@code ⟨TAG⟩} in the bond's banner colour after the name, in chat +
 * the player list (server NameFormat / TabListNameFormat) and above the head (client NameFormat, from the map sent with
 * {@link Tags}). Same technique as t-karakter's Badges.
 */
public final class BondTags {
    /** uuid -> "TAG|rgb" for every online player whose active character is in a bond. */
    public record Tags(CompoundTag codes) implements CustomPacketPayload {
        public static final Type<Tags> TYPE = new Type<>(Guhs.id("vr_gilden_tags"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tags> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Tags::codes, Tags::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final Map<UUID, String> SERVER = new java.util.concurrent.ConcurrentHashMap<>();
    static volatile Map<UUID, String> client = Map.of();
    /** Set by the client: refresh the names after new tags. */
    public static Runnable ontvangen = () -> {
    };

    private BondTags() {
    }

    static void payloads(PayloadRegistrar r) {
        r.playToClient(Tags.TYPE, Tags.STREAM_CODEC, (p, c) -> {
            Map<UUID, String> m = new HashMap<>();
            for (String k : p.codes().keySet()) {
                try {
                    m.put(UUID.fromString(k), p.codes().getStringOr(k, ""));
                } catch (IllegalArgumentException ignored) {
                    // (not a uuid)
                }
            }
            client = m;
            ontvangen.run();
        });
    }

    @Nullable
    static String code(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return null;
        }
        Bond b = Bonden.van(p);
        return b == null ? null : b.tag + "|" + Banier.rgb(b.basis);
    }

    /** Recomputes every online player's tag and tells every client (cheap: a small server). */
    public static void ververs(@Nullable MinecraftServer s) {
        if (s == null) {
            return;
        }
        boolean anders = false;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String c = code(p);
            String oud = c == null ? SERVER.remove(p.getUUID()) : SERVER.put(p.getUUID(), c);
            if (!java.util.Objects.equals(c, oud)) {
                anders = true;
                p.refreshDisplayName();
                p.refreshTabListName();
            }
        }
        SERVER.keySet().removeIf(id -> s.getPlayerList().getPlayer(id) == null);
        if (anders) {
            stuur(s);
        }
    }

    static void stuur(MinecraftServer s) {
        CompoundTag t = new CompoundTag();
        SERVER.forEach((id, c) -> t.putString(id.toString(), c));
        PacketDistributor.sendToAllPlayers(new Tags(t));
    }

    static void stuur(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        SERVER.forEach((id, c) -> t.putString(id.toString(), c));
        PacketDistributor.sendToPlayer(p, new Tags(t));
    }

    static void vergeet(UUID id) {
        SERVER.remove(id);
    }

    /** "⟨TAG⟩" in its colour. */
    public static MutableComponent badge(String code) {
        int i = code.indexOf('|');
        String tag = i < 0 ? code : code.substring(0, i);
        int rgb = 0xFF9AD5;
        try {
            rgb = i < 0 ? rgb : Integer.parseInt(code.substring(i + 1));
        } catch (NumberFormatException ignored) {
            // (default pink)
        }
        final int kleur = rgb;
        return Component.literal("⟨" + tag + "⟩").withStyle(st -> st.withColor(TextColor.fromRgb(kleur)));
    }

    static void naam(PlayerEvent.NameFormat e) {
        Player p = e.getEntity();
        String code = p instanceof ServerPlayer ? SERVER.get(p.getUUID())
                : p.level().isClientSide() && Vadsrijk.in(p) ? client.get(p.getUUID()) : null;
        if (code != null && !code.isEmpty()) {
            e.setDisplayname(e.getDisplayname().copy().append(Component.literal(" ")).append(badge(code)));
        }
    }

    static void tabNaam(PlayerEvent.TabListNameFormat e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            String code = SERVER.get(p.getUUID());
            if (code != null) {
                Component basis = e.getDisplayName() != null ? e.getDisplayName() : p.getName();
                e.setDisplayName(basis.copy().append(Component.literal(" ")).append(badge(code)));
            }
        }
    }
}
