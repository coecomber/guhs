package nl.juiced.guhs.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaBaasEntity;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.MaagManager;

/** Network messages for the guh stomachs, the Mika-baas' game and the Guhdex. */
public final class MaagPayloads {

    // --- the whistle key (client -> server) ---------------------------------------------------------------------------------

    public record MaagKey() implements CustomPacketPayload {
        public static final Type<MaagKey> TYPE = new Type<>(Guhs.id("maag_key"));
        public static final StreamCodec<FriendlyByteBuf, MaagKey> STREAM_CODEC = StreamCodec.unit(new MaagKey());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MaagKey payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                MaagManager.whistle(player);
            }
        }
    }

    // --- stomach settings: server -> client (open the screen), client -> server (change something) ------------------------

    public record MaagSettings(CompoundTag data) implements CustomPacketPayload {
        public static final Type<MaagSettings> TYPE = new Type<>(Guhs.id("maag_settings"));
        public static final StreamCodec<FriendlyByteBuf, MaagSettings> STREAM_CODEC =
                ByteBufCodecs.COMPOUND_TAG.map(MaagSettings::new, MaagSettings::data).cast();

        public static MaagSettings of(GuhWorldData.Maag maag) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Access", maag.access.name());
            tag.putInt("Size", maag.size);
            int lvl = MaagManager.sizeLevel(maag.size);
            tag.putInt("NextSize", lvl + 1 < MaagManager.SIZES.length ? MaagManager.SIZES[lvl + 1] : 0);
            ListTag list = new ListTag();
            maag.whitelist.forEach((id, entry) -> {
                CompoundTag e = new CompoundTag();
                e.putUUID("Id", id);
                e.putString("Name", entry.name);
                e.putBoolean("Build", entry.build);
                list.add(e);
            });
            tag.put("Whitelist", list);
            return new MaagSettings(tag);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MaagSettings payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.openMaagSettings(payload.data());
        }
    }

    public record MaagSettingsAction(int action, String text) implements CustomPacketPayload {
        public static final int SET_ACCESS = 0, ADD = 1, REMOVE = 2, TOGGLE_BUILD = 3;
        public static final Type<MaagSettingsAction> TYPE = new Type<>(Guhs.id("maag_settings_action"));
        public static final StreamCodec<FriendlyByteBuf, MaagSettingsAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MaagSettingsAction::action, ByteBufCodecs.stringUtf8(64), MaagSettingsAction::text,
                MaagSettingsAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MaagSettingsAction p, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GuhWorldData data = GuhWorldData.get(player.server);
            GuhWorldData.Maag maag = data.maagOf(player.getUUID());
            if (maag == null) {
                return;
            }
            switch (p.action()) {
                case SET_ACCESS -> {
                    try {
                        maag.access = GuhWorldData.Access.valueOf(p.text());
                    } catch (IllegalArgumentException ignored) {
                        return;
                    }
                }
                case ADD -> {
                    String name = p.text().strip();
                    var profile = player.server.getProfileCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty()
                            : player.server.getProfileCache().get(name);
                    ServerPlayer online = player.server.getPlayerList().getPlayerByName(name);
                    UUID id = online != null ? online.getUUID() : profile.map(com.mojang.authlib.GameProfile::getId).orElse(null);
                    if (id == null || id.equals(player.getUUID())) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.maag.unknown_player", name), true);
                        return;
                    }
                    maag.whitelist.putIfAbsent(id, new GuhWorldData.WhitelistEntry(online != null ? online.getGameProfile().getName() : name, false));
                }
                case REMOVE -> maag.whitelist.remove(UUID.fromString(p.text()));
                case TOGGLE_BUILD -> {
                    GuhWorldData.WhitelistEntry entry = maag.whitelist.get(UUID.fromString(p.text()));
                    if (entry != null) {
                        entry.build = !entry.build;
                    }
                }
                default -> {
                    return;
                }
            }
            data.setDirty();
            nl.juiced.guhs.network.ModNetworking.sendTo(player, MaagSettings.of(maag));
        }
    }

    // --- rock-paper-scissors-VADS ---------------------------------------------------------------------------------------------

    public record RpsState(int mikaId, int streak, int mikaChoice, boolean won, boolean open, boolean vads) implements CustomPacketPayload {
        public static final Type<RpsState> TYPE = new Type<>(Guhs.id("rps_state"));
        public static final StreamCodec<FriendlyByteBuf, RpsState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RpsState::mikaId, ByteBufCodecs.VAR_INT, RpsState::streak, ByteBufCodecs.INT, RpsState::mikaChoice,
                ByteBufCodecs.BOOL, RpsState::won, ByteBufCodecs.BOOL, RpsState::open, ByteBufCodecs.BOOL, RpsState::vads, RpsState::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RpsState payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.rpsState(payload);
        }
    }

    public record RpsChoice(int mikaId, int choice) implements CustomPacketPayload {
        public static final Type<RpsChoice> TYPE = new Type<>(Guhs.id("rps_choice"));
        public static final StreamCodec<FriendlyByteBuf, RpsChoice> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RpsChoice::mikaId, ByteBufCodecs.VAR_INT, RpsChoice::choice, RpsChoice::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RpsChoice p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.mikaId()) instanceof MikaBaasEntity mika
                    && player.distanceToSqr(mika) < 12 * 12 && p.choice() >= 0 && p.choice() < GuhQuests.Rps.values().length) {
                GuhQuests.playRps(player, mika, GuhQuests.Rps.values()[p.choice()]);
            }
        }
    }

    // --- the Guhdex -------------------------------------------------------------------------------------------------------------

    public record GuhDexData(List<String> seen, List<String> tamed, List<Integer> claimed) implements CustomPacketPayload {
        public static final Type<GuhDexData> TYPE = new Type<>(Guhs.id("guhdex_data"));
        public static final StreamCodec<FriendlyByteBuf, GuhDexData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), GuhDexData::seen,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), GuhDexData::tamed,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), GuhDexData::claimed, GuhDexData::new);

        public static GuhDexData of(GuhWorldData.PlayerData p) {
            return new GuhDexData(p.seen.stream().map(GuhVariant::id).toList(), p.tamed.stream().map(GuhVariant::id).toList(),
                    new ArrayList<>(p.rewards));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(GuhDexData payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.openGuhDex(payload);
        }
    }

    /** One minigame on the Guhdex Highscores page: your best (formatted like the game does), and the server record + holder. */
    public record HighscoreRow(String game, boolean played, String best, String record, String holder) {
        public static final StreamCodec<FriendlyByteBuf, HighscoreRow> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, HighscoreRow::game,
                ByteBufCodecs.BOOL, HighscoreRow::played,
                ByteBufCodecs.STRING_UTF8, HighscoreRow::best,
                ByteBufCodecs.STRING_UTF8, HighscoreRow::record,
                ByteBufCodecs.STRING_UTF8, HighscoreRow::holder, HighscoreRow::new);

        public boolean hasRecord() {
            return !holder.isEmpty();
        }
    }

    /** The Guhdex Highscores page (server -> client), sent on opening the Guhdex and whenever a score changes. */
    public record HighscoresData(List<HighscoreRow> rows) implements CustomPacketPayload {
        public static final Type<HighscoresData> TYPE = new Type<>(Guhs.id("highscores_data"));
        public static final StreamCodec<FriendlyByteBuf, HighscoresData> STREAM_CODEC =
                HighscoreRow.STREAM_CODEC.apply(ByteBufCodecs.list()).map(HighscoresData::new, HighscoresData::rows);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HighscoresData payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.highscores(payload);
        }
    }

    public record GuhDexClaim(int milestone) implements CustomPacketPayload {
        public static final Type<GuhDexClaim> TYPE = new Type<>(Guhs.id("guhdex_claim"));
        public static final StreamCodec<FriendlyByteBuf, GuhDexClaim> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(GuhDexClaim::new, GuhDexClaim::milestone).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(GuhDexClaim p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                GuhDex.claim(player, p.milestone());
            }
        }
    }

    // --- verstopguh: Verstopguhtje's screen ----------------------------------------------------------------------------

    public record VerstopOpen(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<VerstopOpen> TYPE = new Type<>(Guhs.id("verstop_open"));
        public static final StreamCodec<FriendlyByteBuf, VerstopOpen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, VerstopOpen::npcId, ByteBufCodecs.COMPOUND_TAG, VerstopOpen::data, VerstopOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(VerstopOpen payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.openVerstop(payload);
        }
    }

    public record VerstopAction(int npcId, int action) implements CustomPacketPayload {
        public static final Type<VerstopAction> TYPE = new Type<>(Guhs.id("verstop_action"));
        public static final StreamCodec<FriendlyByteBuf, VerstopAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, VerstopAction::npcId, ByteBufCodecs.VAR_INT, VerstopAction::action, VerstopAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(VerstopAction p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(p.npcId()) instanceof nl.juiced.guhs.entity.GuhNpcEntity npc) {
                nl.juiced.guhs.quest.VerstopGame.action(npc, player, p.action());
            }
        }
    }

    // --- Reisguh: the waypoint menu ----------------------------------------------------------------------------------

    public record ReisOpen(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<ReisOpen> TYPE = new Type<>(Guhs.id("reis_open"));
        public static final StreamCodec<FriendlyByteBuf, ReisOpen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ReisOpen::npcId, ByteBufCodecs.COMPOUND_TAG, ReisOpen::data, ReisOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ReisOpen payload, IPayloadContext context) {
            nl.juiced.guhs.client.GuhsClientHooks.openReis(payload);
        }
    }

    public record ReisAction(int npcId, int action, String text) implements CustomPacketPayload {
        public static final Type<ReisAction> TYPE = new Type<>(Guhs.id("reis_action"));
        public static final StreamCodec<FriendlyByteBuf, ReisAction> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ReisAction::npcId, ByteBufCodecs.VAR_INT, ReisAction::action, ByteBufCodecs.stringUtf8(64), ReisAction::text,
                ReisAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ReisAction p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(p.npcId()) instanceof nl.juiced.guhs.entity.GuhNpcEntity npc) {
                nl.juiced.guhs.quest.Reisguh.action(npc, player, p.action(), p.text());
            }
        }
    }

    /** The super compass menu: what to look for (client -> server). */
    public record SuperkompasChoice(boolean mainHand, String structure) implements CustomPacketPayload {
        public static final Type<SuperkompasChoice> TYPE = new Type<>(Guhs.id("superkompas_choice"));
        public static final StreamCodec<FriendlyByteBuf, SuperkompasChoice> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SuperkompasChoice::mainHand, ByteBufCodecs.stringUtf8(64), SuperkompasChoice::structure, SuperkompasChoice::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SuperkompasChoice p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && nl.juiced.guhs.item.SuperkompasItem.allowed(p.structure())) {
                var stack = player.getItemInHand(p.mainHand() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
                if (stack.getItem() instanceof nl.juiced.guhs.item.SuperkompasItem) {
                    nl.juiced.guhs.item.SuperkompasItem.choose(stack, p.structure());
                    nl.juiced.guhs.feature.verhaal.VerhaalFeature.kompasGekozen(player, p.structure());   // 3.0: the Verhalen tab
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.guhs.guhmensie_superkompas.chosen",
                            net.minecraft.network.chat.Component.translatable("structure.guhs." + p.structure())), true);
                }
            }
        }
    }

    public static void register(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        registrar.playToServer(SuperkompasChoice.TYPE, SuperkompasChoice.STREAM_CODEC, SuperkompasChoice::handle);
        registrar.playToClient(ReisOpen.TYPE, ReisOpen.STREAM_CODEC, ReisOpen::handle);
        registrar.playToServer(ReisAction.TYPE, ReisAction.STREAM_CODEC, ReisAction::handle);
        registrar.playToClient(VerstopOpen.TYPE, VerstopOpen.STREAM_CODEC, VerstopOpen::handle);
        registrar.playToServer(VerstopAction.TYPE, VerstopAction.STREAM_CODEC, VerstopAction::handle);
        registrar.playToServer(MaagKey.TYPE, MaagKey.STREAM_CODEC, MaagKey::handle);
        registrar.playToClient(MaagSettings.TYPE, MaagSettings.STREAM_CODEC, MaagSettings::handle);
        registrar.playToServer(MaagSettingsAction.TYPE, MaagSettingsAction.STREAM_CODEC, MaagSettingsAction::handle);
        registrar.playToClient(RpsState.TYPE, RpsState.STREAM_CODEC, RpsState::handle);
        registrar.playToServer(RpsChoice.TYPE, RpsChoice.STREAM_CODEC, RpsChoice::handle);
        registrar.playToClient(GuhDexData.TYPE, GuhDexData.STREAM_CODEC, GuhDexData::handle);
        registrar.playToServer(GuhDexClaim.TYPE, GuhDexClaim.STREAM_CODEC, GuhDexClaim::handle);
        registrar.playToClient(HighscoresData.TYPE, HighscoresData.STREAM_CODEC, HighscoresData::handle);
    }

    private MaagPayloads() {
    }
}
