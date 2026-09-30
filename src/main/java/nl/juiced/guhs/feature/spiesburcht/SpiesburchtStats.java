package nl.juiced.guhs.feature.spiesburcht;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Per player: how many Rookguhs they saved (kept through death, in the player's persisted data). Shown on the Rookguh's
 * own Guhdex page (2.10.1; the client gets it with {@link Teller}), and good for an advancement and FTB quests.
 */
public final class SpiesburchtStats {
    public static final String ROOKGUHS = "guhs_rookguhs_gered";
    /** Saved this many Rookguhs: the "Rookguh-redder" challenge. */
    public static final int REDDER = 10;
    /** Client side: the number the Guhdex shows. */
    public static volatile int clientRookguhs;

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
    }

    public static int rookguhs(Player player) {
        return data(player).getIntOr(ROOKGUHS, 0);
    }

    /** One more Rookguh safely home, thanks to this player. */
    public static void rookguhSaved(ServerPlayer player) {
        int count = rookguhs(player) + 1;
        data(player).putInt(ROOKGUHS, count);
        player.sendSystemMessage(Component.translatable("quest.guhs.rookguh.gered", count).withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(player, "rookguh_gered");
        award(player, "barbecuether/rookguh");
        if (count >= REDDER) {
            GuhAdvancements.grant(player, "rookguh_redder");
            award(player, "barbecuether/rookguh_redder");
        }
        nl.juiced.guhs.quest.GuhDex.zie(player, nl.juiced.guhs.entity.GuhVariant.ROOKGUH);   // (its page shows the count)
        sync(player);
    }

    /** An advancement of our tab (criterion "done", granted by the mod). */
    public static void award(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    public static void sync(ServerPlayer player) {
        if (rookguhs(player) > 0) {   // (2.10.1: saved some before the Rookguh had a page: it's filled in)
            nl.juiced.guhs.quest.GuhDex.zie(player, nl.juiced.guhs.entity.GuhVariant.ROOKGUH);
        }
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new Teller(rookguhs(player)));
    }

    static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Teller.TYPE, Teller.STREAM_CODEC, Teller::handle);
    }

    /** Server to client: your number of saved Rookguhs. */
    public record Teller(int rookguhs) implements CustomPacketPayload {
        public static final Type<Teller> TYPE = new Type<>(Guhs.id("rookguh_teller"));
        public static final StreamCodec<FriendlyByteBuf, Teller> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Teller::new, Teller::rookguhs).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Teller payload, IPayloadContext context) {
            clientRookguhs = payload.rookguhs();
        }
    }

    private SpiesburchtStats() {
    }
}
