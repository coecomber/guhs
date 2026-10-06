package nl.juiced.guhs.feature.torenpeper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Growing in the kweekbakken of the kas, per player. A player has ONE plant per kind of kweekbak (green, red, pink), in
 * every kweekbak of that kind of every Pepertuin: what is saved is when each was planted
 * ({@code guhs_torenpeper_kweek} in {@link GuhQuests#saved}, three longs: a {@link #stempel} of the game time, 0 = empty). The plant goes through its four stages by
 * itself, {@link #GROEI_TICKS} apiece, also while the player is away. Nothing in the world changes, so any number of
 * players grow side by side in the same three troughs; the client draws each player their own plant
 * ({@link Stand}, client.KweekRenderer).
 * <p>
 * A click on a kweekbak ({@link #klik}): empty + peperzaadjes in a hand = planted; growing = how long still; ripe =
 * {@link #PEPERS} peppers of that kind and the seed back, and the trough is empty again.
 */
public final class Kweek {
    /** Ticks per stage: planted (1), 2, 3, ripe (4) after three of these. */
    public static final int GROEI_TICKS = 300;
    public static final int RIJP = 4;
    /** What a ripe plant of a kweekbak gives (plus the seed it grew from). */
    public static final int PEPERS = 2;
    private static final String SLEUTEL = "guhs_torenpeper_kweek";
    /** Added to the game time in what is saved, so that 0 can mean "empty" also in a world that has only just begun. */
    private static final long NUL = 1L << 40;

    private Kweek() {
    }

    /** What is saved for "planted at this game time" (never 0). */
    public static long stempel(long gameTime) {
        return gameTime + NUL;
    }

    /** When this player planted in the green, red and pink kweekbak (a {@link #stempel}; 0 = empty). */
    public static long[] geplant(Player p) {
        long[] a = GuhQuests.saved(p).getLongArray(SLEUTEL).orElse(null);
        return a != null && a.length == PeperSoort.values().length ? a.clone() : new long[PeperSoort.values().length];
    }

    private static void zet(ServerPlayer p, long[] geplant) {
        GuhQuests.saved(p).putLongArray(SLEUTEL, geplant);
        sync(p);
    }

    /** The stage, at this game time, of a plant with this {@link #stempel}: 0 empty, 1..4 (4 = ripe). Both sides. */
    public static int groei(long geplant, long gameTime) {
        return geplant <= 0 ? 0 : (int) Math.min(RIJP, 1 + Math.max(0, stempel(gameTime) - geplant) / GROEI_TICKS);
    }

    public static int groei(ServerPlayer p, PeperSoort soort) {
        return groei(geplant(p)[soort.ordinal()], p.level().getGameTime());
    }

    /** Plants this kind for the player as if it happened {@code geleden} ticks ago (tests, the op command). */
    public static void plant(ServerPlayer p, PeperSoort soort, long geleden) {
        long[] a = geplant(p);
        a[soort.ordinal()] = stempel(p.level().getGameTime() - geleden);
        zet(p, a);
    }

    /** (op command) every kweekbak of this player holds a ripe plant. */
    public static int maakRijp(ServerPlayer p) {
        for (PeperSoort s : PeperSoort.values()) {
            plant(p, s, 3L * GROEI_TICKS);
        }
        return PeperSoort.values().length;
    }

    /** The player clicked a kweekbak of this kind. */
    public static void klik(ServerPlayer p, BlockPos pos, PeperSoort soort) {
        ServerLevel level = p.level();
        if (TorenpeperFeature.PEPERTUIN.stap(p) < 1) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.kweek.eerst_praten").withStyle(ChatFormatting.GRAY));
            return;
        }
        long[] a = geplant(p);
        long nu = level.getGameTime();
        int groei = groei(a[soort.ordinal()], nu);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
        if (groei == 0) {
            InteractionHand hand = p.getMainHandItem().is(TorenpeperFeature.PEPERZAADJES.get()) ? InteractionHand.MAIN_HAND
                    : p.getOffhandItem().is(TorenpeperFeature.PEPERZAADJES.get()) ? InteractionHand.OFF_HAND : null;
            if (hand == null) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.kweek.leeg").withStyle(ChatFormatting.GRAY));
                return;
            }
            p.getItemInHand(hand).consume(1, p);
            a[soort.ordinal()] = stempel(nu);
            zet(p, a);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1f, 1.1f);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, x, y, z, 6, 0.25, 0.15, 0.25, 0.0);
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.kweek.geplant", GROEI_TICKS * (RIJP - 1) / 20).withStyle(ChatFormatting.GREEN));
        } else if (groei < RIJP) {
            long nog = a[soort.ordinal()] + (long) GROEI_TICKS * (RIJP - 1) - stempel(nu);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, x, y, z, 3, 0.25, 0.2, 0.25, 0.0);
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.kweek.groeit", Math.max(1, (nog + 19) / 20)).withStyle(ChatFormatting.YELLOW));
        } else {
            a[soort.ordinal()] = 0;
            zet(p, a);
            Minigames.give(p, new ItemStack(soort.peper(), PEPERS));
            Minigames.give(p, new ItemStack(TorenpeperFeature.PEPERZAADJES.get()));
            level.playSound(null, pos, TorenpeperFeature.PLUK.get(), SoundSource.BLOCKS, 1f, 1f);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, x, y + 0.3, z, 8, 0.3, 0.3, 0.3, 0.0);
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.kweek.geplukt", Component.translatable(soort.peper().getDescriptionId()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            Pepertuin.opGeplukt(p, soort);
        }
    }

    // --- to the client ---------------------------------------------------------------------------------------------------------

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    /** Tells the player's client when each of their plants was planted (login, respawn, dimension change, every change). */
    public static void sync(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.putLongArray("Geplant", geplant(p));
        ModNetworking.sendTo(p, new Stand(data));
    }

    /** data: "Geplant" (long[3]: when the local player planted green, red, pink, as a {@link #stempel}; 0 = empty). */
    public record Stand(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("torenpeper_kweek"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Stand::data, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand payload, IPayloadContext context) {
            nl.juiced.guhs.feature.torenpeper.client.TorenpeperClient.zetKweek(payload.data());
        }
    }
}
