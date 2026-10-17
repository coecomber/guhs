package nl.juiced.guhs.feature.einde2.spzij;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 7.10): the small helpers every class of this slice shares: the text
 * prefixes, where a marker of Bikinibodem lies in the world, whether a player is in town, the gate of the side lines, and the
 * ways of telling a player something (a voice in the chat, a title, a line over the hotbar, a sound only they hear).
 * Texts: tools/features/einde2_sp_zij.py (Dutch), tools/lang/en/c119_e2_sp_zij.json (English).
 */
public final class SpZij {
    /** The text prefix of this slice (everything that is not a story line, a scene or a hint). */
    public static final String P = "gui.guhs.einde2_sp_zij.";
    /** The structure all three side lines play in. */
    public static final String BB = SpVerhaal.BIKINIBODEM;
    /** The entity tag of every entity of ONE player this slice makes (never kept: thrown away when its chunk loads again). */
    public static final String EIGEN = "guhs_einde2_sp_zij_eigen";

    private SpZij() {
    }

    // =====================================================================================================================
    // the gate of the side lines
    // =====================================================================================================================

    /**
     * Are the side lines of the town open for this player (CONTRACT_E2_C 2.1, decision 12.6): Bikinibodem open for them and
     * they live and work in town: part 1 past the job interview ({@code R_SOLLICITATIE}), or part 1 done.
     */
    public static boolean open(ServerPlayer p) {
        return SpVerhaal.open(p) && (SpVerhaal.receptKlaar(p) || SpVerhaal.stap(p, SpVerhaal.RECEPT) > SpVerhaal.R_SOLLICITATIE);
    }

    // =====================================================================================================================
    // places
    // =====================================================================================================================

    /** The Guheinde of this server (null: none). */
    @Nullable
    public static ServerLevel guheinde(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(GuheindeFeature.GUHEINDE);
    }

    /** A marker of Bikinibodem in the world (null: the town is not found yet, or no such marker). */
    @Nullable
    public static BlockPos plek(ServerLevel guheinde, String marker) {
        return Plekken.bekend(guheinde, BB) ? Plekken.wereld(guheinde, BB, marker) : null;
    }

    /** The middle of the bottom of a marker's block (where you stand). */
    @Nullable
    public static Vec3 voeten(ServerLevel guheinde, String marker) {
        BlockPos b = plek(guheinde, marker);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** Where the town's template corner lies in the world (null: not found yet). */
    @Nullable
    public static BlockPos oorsprong(ServerLevel guheinde) {
        return Plekken.bekend(guheinde, BB) ? Plekken.oorsprong(guheinde, BB) : null;
    }

    /** Is the player in the Guheinde, inside Bikinibodem's box? */
    public static boolean inStad(ServerPlayer p) {
        if (p.level().dimension() != GuheindeFeature.GUHEINDE || !Plekken.bekend(p.level(), BB)) {
            return false;
        }
        BoundingBox doos = Plekken.doos(p.level(), BB);
        return doos != null && doos.isInside(p.blockPosition());
    }

    /** Horizontal distance (squared) and height difference: is {@code pos} within {@code straal} blocks and 3 high of {@code doel}? */
    public static boolean bij(Vec3 pos, @Nullable Vec3 doel, double straal) {
        if (doel == null) {
            return false;
        }
        double dx = pos.x - doel.x, dz = pos.z - doel.z;
        return dx * dx + dz * dz <= straal * straal && Math.abs(pos.y - doel.y) <= 3.0;
    }

    // =====================================================================================================================
    // telling a player something
    // =====================================================================================================================

    /** "&lt;Name&gt; text" in the chat for this player, without an entity (a voice from upstairs, a character far away). */
    public static void stem(ServerPlayer p, String naamKey, String key, Object... args) {
        MutableComponent line = Component.literal("<").append(Component.translatable(naamKey)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(line);
    }

    /** A short line over the hotbar (a counter, an instruction of a minigame). */
    public static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A grey line in the chat (what you notice, no speaker). */
    public static void merk(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** A big title with a subtitle, for this player only. */
    public static void titel(ServerPlayer p, String titelKey, @Nullable String onderKey, Object... args) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(6, 50, 14));
        if (onderKey != null) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(onderKey, args).withStyle(ChatFormatting.YELLOW)));
        }
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(titelKey, args).withStyle(ChatFormatting.GOLD)));
    }

    /** A sound only this player hears, where they stand. */
    public static void geluid(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        Schijn.geluid(p, s, p.blockPosition(), volume, pitch);
    }

    /** Into the pockets (or at the feet when they are full). */
    public static void geef(ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            Minigames.give(p, s.copy());
        }
    }

    /** These rewards once per player and name (true the first time). */
    public static boolean eenmalig(Verhaallijn lijn, ServerPlayer p, String naam, ItemStack... stacks) {
        if (!lijn.eenmalig(p, naam)) {
            return false;
        }
        geef(p, stacks);
        return true;
    }

    /** The server's tick count (the scheduled jobs run on it, never on a player's own tickCount: CONTRACT_E2_C 7.1). */
    public static long nu(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        return s == null ? p.level().getGameTime() : s.getTickCount();
    }
}
