package nl.juiced.guhs.feature.guhwaiispellen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * The surf beach's messages: Lilo-guh's screen (open / an action), the surf ride (start, the surfer's keys per step, the
 * end with the result) and the hula dance (start: the song and whether you dance or watch, a danced step, the score).
 */
public final class GuhwaiiSpellenPayloads {
    /** Server -> client: Lilo-guh's screen (surf or hula, the records, the coins). */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("guhwaiispellen_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            nl.juiced.guhs.feature.guhwaiispellen.client.GuhwaiiSpellenClient.open(p);
        }
    }

    /** Client -> server: a button of Lilo-guh's screen (START + level, STOP). */
    public record Actie(int npcId, int actie) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("guhwaiispellen_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Actie::npcId, ByteBufCodecs.VAR_INT, Actie::actie, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc
                    && npc.getKind() == GuhNpcEntity.Kind.LILO_GUH) {
                String plek = npc.roleData.getStringOr(NpcRollen.PLEK, "");
                if (plek.equals("surf")) {
                    SurfSpel.actie(npc, player, p.actie());
                } else if (plek.equals("hula")) {
                    HulaSpel.actie(npc, player, p.actie());
                }
            }
        }
    }

    /** Server -> client: your surf game starts (boards, the surf spot, level, seed: your game runs the same ride). */
    public record SurfStart(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SurfStart> TYPE = new Type<>(Guhs.id("guhwaiispellen_surf_start"));
        public static final StreamCodec<FriendlyByteBuf, SurfStart> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, SurfStart::data, SurfStart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SurfStart p, IPayloadContext context) {
            nl.juiced.guhs.feature.guhwaiispellen.client.SurfClient.start(p.data());
        }
    }

    /** Client -> server: the keys of one step of your ride. */
    public record SurfStuur(int bord, int stap, int bits) implements CustomPacketPayload {
        public static final Type<SurfStuur> TYPE = new Type<>(Guhs.id("guhwaiispellen_surf_stuur"));
        public static final StreamCodec<FriendlyByteBuf, SurfStuur> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SurfStuur::bord, ByteBufCodecs.VAR_INT, SurfStuur::stap, ByteBufCodecs.VAR_INT, SurfStuur::bits, SurfStuur::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SurfStuur p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                SurfSpel.invoer(player, p.bord(), p.stap(), p.bits());
            }
        }
    }

    /** Server -> client: your surf game is over (the result). */
    public record SurfEinde(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SurfEinde> TYPE = new Type<>(Guhs.id("guhwaiispellen_surf_einde"));
        public static final StreamCodec<FriendlyByteBuf, SurfEinde> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, SurfEinde::data, SurfEinde::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SurfEinde p, IPayloadContext context) {
            nl.juiced.guhs.feature.guhwaiispellen.client.SurfClient.einde(p.data());
        }
    }

    /** Server -> client: a hula song starts (Npc, Liedje; Danser: you dance, else you watch Lilo-guh). */
    public record HulaStart(CompoundTag data) implements CustomPacketPayload {
        public static final Type<HulaStart> TYPE = new Type<>(Guhs.id("guhwaiispellen_hula_start"));
        public static final StreamCodec<FriendlyByteBuf, HulaStart> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, HulaStart::data, HulaStart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HulaStart p, IPayloadContext context) {
            nl.juiced.guhs.feature.guhwaiispellen.client.HulaClient.start(p.data());
        }
    }

    /** Client -> server: step `noot` (move `pas`) danced `ms` after the song's start. */
    public record HulaTik(int npcId, int noot, int ms, int pas) implements CustomPacketPayload {
        public static final Type<HulaTik> TYPE = new Type<>(Guhs.id("guhwaiispellen_hula_tik"));
        public static final StreamCodec<FriendlyByteBuf, HulaTik> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HulaTik::npcId, ByteBufCodecs.VAR_INT, HulaTik::noot, ByteBufCodecs.VAR_INT, HulaTik::ms,
                ByteBufCodecs.VAR_INT, HulaTik::pas, HulaTik::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HulaTik p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(p.npcId()) instanceof GuhNpcEntity npc
                    && npc.getKind() == GuhNpcEntity.Kind.LILO_GUH) {
                HulaSpel.of(npc).tik(npc, player, p.noot(), p.ms(), p.pas());
            }
        }
    }

    /** Server -> client: the dance's score (Score, Combo, the judgements; Einde: the song is over). */
    public record HulaStand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<HulaStand> TYPE = new Type<>(Guhs.id("guhwaiispellen_hula_stand"));
        public static final StreamCodec<FriendlyByteBuf, HulaStand> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, HulaStand::data, HulaStand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HulaStand p, IPayloadContext context) {
            nl.juiced.guhs.feature.guhwaiispellen.client.HulaClient.stand(p.data());
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
        registrar.playToClient(SurfStart.TYPE, SurfStart.STREAM_CODEC, SurfStart::handle);
        registrar.playToServer(SurfStuur.TYPE, SurfStuur.STREAM_CODEC, SurfStuur::handle);
        registrar.playToClient(SurfEinde.TYPE, SurfEinde.STREAM_CODEC, SurfEinde::handle);
        registrar.playToClient(HulaStart.TYPE, HulaStart.STREAM_CODEC, HulaStart::handle);
        registrar.playToServer(HulaTik.TYPE, HulaTik.STREAM_CODEC, HulaTik::handle);
        registrar.playToClient(HulaStand.TYPE, HulaStand.STREAM_CODEC, HulaStand::handle);
    }

    private GuhwaiiSpellenPayloads() {
    }
}
