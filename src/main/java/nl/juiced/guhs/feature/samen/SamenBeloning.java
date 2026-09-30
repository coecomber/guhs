package nl.juiced.guhs.feature.samen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.kleding.KledingOntgrendel;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * What the hartjes levels unlock (2.10, samen), per player (for all their guhs), the first time any of their guhs reaches
 * the level (also for a level-up while they were offline: at the next login, and as a catch-up for guhs that were already
 * that far):
 * <ul>
 *   <li>lieve vadsjes van elkaar: the hartjesspeldje (ears) and the emote HARTJES;</li>
 *   <li>mega lieve vadsjes van elkaar: the knuffeltruitje and the emote KNUFFELDANSJE;</li>
 *   <li>zielsguh bff 5evr &lt;3: the zielskroontje, the exclusive gouden hartjes-halsbandje and the emote BFF_KNUFFEL.</li>
 * </ul>
 * The clothes are normal clothing unlocks (source "band"); the three emotes are locked until then
 * ({@link #heeft}: server = the player's saved bits {@link #EMOTES}, client = the synced copy).
 */
public final class SamenBeloning {
    /** GuhQuests.saved(player): the bits (Emote ordinals) of the unlocked hartjes emotes. */
    public static final String EMOTES = "guhs_samen_emotes";

    private static final Map<BandNiveau, List<GuhClothes>> KLEDING = Map.of(
            BandNiveau.LIEF, List.of(GuhClothes.SAMEN_HARTJESSPELDJE),
            BandNiveau.MEGA, List.of(GuhClothes.SAMEN_KNUFFELTRUITJE),
            BandNiveau.ZIELSGUH, List.of(GuhClothes.SAMEN_ZIELSKROONTJE, GuhClothes.GOUDEN_HARTJESHALSBANDJE));

    private SamenBeloning() {
    }

    /** The clothes a level unlocks (empty for GEEN). */
    public static List<GuhClothes> kleding(BandNiveau niveau) {
        return KLEDING.getOrDefault(niveau, List.of());
    }

    /** All four hartjes pieces, in level order. */
    public static List<GuhClothes> alleKleding() {
        List<GuhClothes> out = new ArrayList<>();
        for (BandNiveau n : BandNiveau.values()) {
            out.addAll(kleding(n));
        }
        return out;
    }

    /** The emote a level unlocks (null for GEEN). */
    @Nullable
    public static Emote emote(BandNiveau niveau) {
        return switch (niveau) {
            case LIEF -> Emote.HARTJES;
            case MEGA -> Emote.KNUFFELDANSJE;
            case ZIELSGUH -> Emote.BFF_KNUFFEL;
            default -> null;
        };
    }

    /** The level that unlocks this emote, or null when it isn't a hartjes emote (then it's never locked). */
    @Nullable
    public static BandNiveau niveau(Emote emote) {
        return switch (emote) {
            case HARTJES -> BandNiveau.LIEF;
            case KNUFFELDANSJE -> BandNiveau.MEGA;
            case BFF_KNUFFEL -> BandNiveau.ZIELSGUH;
            default -> null;
        };
    }

    public static boolean isBandEmote(Emote emote) {
        return niveau(emote) != null;
    }

    // =====================================================================================================================
    // the emote lock
    // =====================================================================================================================

    /** May this player pick this emote? (Server: the saved bits; client: the synced copy.) */
    public static boolean heeft(Player player, Emote emote) {
        if (!isBandEmote(emote)) {
            return true;
        }
        int bits = player.level().isClientSide ? Client.bits : GuhQuests.saved(player).getInt(EMOTES);
        return (bits & (1 << emote.ordinal())) != 0;
    }

    /** The unlocked hartjes emotes of this player (bits, server). */
    public static int bits(Player player) {
        return GuhQuests.saved(player).getInt(EMOTES);
    }

    /** Unlocks a hartjes emote; true when it was new (saved, synced, a message). */
    public static boolean ontgrendelEmote(ServerPlayer player, Emote emote) {
        CompoundTag saved = GuhQuests.saved(player);
        int bits = saved.getInt(EMOTES);
        int bit = 1 << emote.ordinal();
        if ((bits & bit) != 0) {
            return false;
        }
        saved.putInt(EMOTES, bits | bit);
        sync(player);
        player.sendSystemMessage(Component.translatable("gui.guhs.samen.emote_ontgrendeld", emote.displayName().copy()
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** "Nog op slot": the owner asked for a hartjes emote they haven't unlocked yet. */
    public static void opSlot(ServerPlayer player, Emote emote) {
        BandNiveau n = niveau(emote);
        player.displayClientMessage(Component.translatable("gui.guhs.samen.emote_op_slot", emote.displayName(),
                n == null ? Component.empty() : n.naam()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** (Tests / admin) forget the unlocked hartjes emotes. */
    public static void wisEmotes(ServerPlayer player) {
        GuhQuests.saved(player).remove(EMOTES);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new SamenPayloads.Emotes(bits(player)));
    }

    // =====================================================================================================================
    // the rewards
    // =====================================================================================================================

    /** Band.opNiveau: a guh of this player reached a new level. */
    static void nieuwNiveau(ServerPlayer eigenaar, @Nullable Mob guh, UUID bandId, BandNiveau niveau) {
        geef(eigenaar, niveau, guh);
    }

    /**
     * Gives everything this level (and the ones below it) unlocks that the player doesn't have yet. Returns how many
     * things were new.
     */
    public static int geef(ServerPlayer player, BandNiveau niveau, @Nullable Mob guh) {
        int nieuw = 0;
        for (BandNiveau n : BandNiveau.values()) {
            if (n == BandNiveau.GEEN || n.ordinal() > niveau.ordinal()) {
                continue;
            }
            for (GuhClothes c : kleding(n)) {
                if (!KledingUnlocks.heeft(player, c) && KledingOntgrendel.ontgrendel(player, c)) {
                    nieuw++;
                    if (c == GuhClothes.GOUDEN_HARTJESHALSBANDJE) {
                        player.sendSystemMessage(Component.translatable("gui.guhs.samen.halsbandje").withStyle(ChatFormatting.GOLD));
                        GidsFeature.grant(player, "lieve_vadsjes/samen_halsbandje");
                    }
                }
            }
            Emote e = emote(n);
            if (e != null && ontgrendelEmote(player, e)) {
                nieuw++;
                if (e == Emote.BFF_KNUFFEL) {
                    player.sendSystemMessage(Component.translatable("gui.guhs.samen.bff_uitleg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
            GidsFeature.grant(player, "lieve_vadsjes/samen_beloning_" + n.id());
        }
        if (nieuw > 0 && guh != null && guh.level() instanceof ServerLevel level) {
            level.playSound(null, guh.blockPosition(), SamenFeature.JUICH.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        }
        return nieuw;
    }

    /** At login: rewards of levels this player's guhs already reached (e.g. before this update). */
    static void inhalen(ServerPlayer player) {
        BandNiveau hoogste = BandNiveau.GEEN;
        for (BandNiveau n : BandNiveau.values()) {
            if (n != BandNiveau.GEEN && Band.aantal(player.server, player.getUUID(), n) > 0) {
                hoogste = n;
            }
        }
        if (hoogste != BandNiveau.GEEN) {
            geef(player, hoogste, null);
        }
        sync(player);
    }

    /** The client's copy of its own unlocked hartjes emotes (set by the payload). */
    public static final class Client {
        static volatile int bits;

        public static void zet(int b) {
            bits = b;
        }

        public static int bits() {
            return bits;
        }

        private Client() {
        }
    }
}
