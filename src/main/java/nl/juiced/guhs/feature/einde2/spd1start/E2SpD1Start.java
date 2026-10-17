package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 2.2, 7.3), slice sp_d1_start: part 1 steps 0-4 of the line {@value SpVerhaal#RECEPT}
 * ({@link SpVerhaal#RECEPT_LIJN}, registered by the skelet; this slice owns only its steps through {@code SpStappen}):
 * <ol start="0">
 *   <li>{@link SpVerhaal#R_BRIEF}: a soap bubble with a letter drifts to the player in the Guheinde ({@link Bubbelbrief}); a
 *       click pops it: scene {@code brief};</li>
 *   <li>{@link SpVerhaal#R_REIS}: to Bikinibodem (Appa, the compass); landing on the island above the dome: the narrator card
 *       and scene {@code aankomst} ({@link D1Stad});</li>
 *   <li>{@link SpVerhaal#R_BUBBEL}: down the bubbelstroom; on the plein: scene {@code welkom} ({@link D1Stad});</li>
 *   <li>{@link SpVerhaal#R_SOLLICITATIE}: a click on Meneer Krabguh: scene {@code sollicitatie}, the Krokante Knabbel cap
 *       ({@link D1Stad});</li>
 *   <li>{@link SpVerhaal#R_BAKKEN}: the grill game, five orders: scene {@code bakken_uitleg} before, {@code bakken_klaar} after
 *       ({@link Bakken}, {@link GrillSpel}); then the line goes on to sp_d1_stad's jellyfishing.</li>
 * </ol>
 * All state lives in the line (per player): its step, and the counter {@value #T_BESTELD} while the grill game runs. Texts:
 * tools/features/einde2_sp_d1_start.py (+ _tekst), English in c112_e2_sp_d1_start.
 */
public final class E2SpD1Start {
    /** The lang prefix of the slice. */
    public static final String P = "gui.guhs.einde2_sp_d1_start.";
    /** The narrator card of part 1 (and the map behind the picture-book replays of this slice's scenes). */
    public static final String KAART = "einde2_sp_d1_start";
    /** The clothes source of the cap. */
    public static final String KLEDINGBRON = "einde2_sp_d1_start";
    /** The place. */
    public static final String BB = SpVerhaal.BIKINIBODEM;
    /** Counter of the line (only while step 4 is on): how many orders of the grill game are done. */
    public static final String T_BESTELD = "sp_d1_start_besteld";
    /** Counter of the line: the best time of the grill game (seconds; a score, never a limit). */
    public static final String T_BESTE_TIJD = "sp_d1_start_beste_tijd";

    private E2SpD1Start() {
    }

    static Verhaallijn lijn() {
        return SpVerhaal.RECEPT_LIJN;
    }

    static int stap(ServerPlayer p) {
        return lijn().stap(p);
    }

    /** Is this player's part 1 on, at exactly this step, with the gate open for them? */
    static boolean bij(ServerPlayer p, int stap) {
        return SpVerhaal.open(p) && SpVerhaal.actief(p, SpVerhaal.RECEPT) && stap(p) == stap;
    }

    static MutableComponent tekst(String key, Object... args) {
        return Component.translatable(P + key, args);
    }

    // =====================================================================================================================
    // places (the guaranteed copy of Bikinibodem)
    // =====================================================================================================================

    @Nullable
    static ServerLevel guheinde(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(GuheindeFeature.GUHEINDE);
    }

    /** The world block of a marker of Bikinibodem (null: the spot is not known yet). */
    @Nullable
    static BlockPos plek(@Nullable ServerLevel g, String id) {
        return g != null && Plekken.bekend(g, BB) ? Plekken.wereld(g, BB, id) : null;
    }

    /** The middle of a marker's block, on its floor. */
    @Nullable
    static Vec3 midden(@Nullable ServerLevel g, String id) {
        BlockPos b = plek(g, id);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** A zone marker of Bikinibodem in world coordinates (both corners included). */
    @Nullable
    static AABB zone(@Nullable ServerLevel g, String id) {
        Plekken.Plek z = Plekken.plek(BB, id);
        BlockPos o = g != null && Plekken.bekend(g, BB) ? Plekken.oorsprong(g, BB) : null;
        if (z == null || o == null) {
            return null;
        }
        BlockPos tot = z.tot() == null ? z.pos() : z.tot();
        return new AABB(Vec3.atLowerCornerOf(o.offset(z.pos())), Vec3.atLowerCornerOf(o.offset(tot)).add(1, 1, 1));
    }

    /** Is the player in the Guheinde and inside the box of Bikinibodem? */
    static boolean inBikinibodem(ServerPlayer p) {
        if (!GuheindeFeature.isGuheinde(p.level()) || !Plekken.bekend(p.level(), BB)) {
            return false;
        }
        BoundingBox doos = Plekken.doos(p.level(), BB);
        return doos != null && doos.inflatedBy(2).isInside(p.blockPosition());
    }

    /** Puts the player at a marker of Bikinibodem (dev, a fresh start), looking that way. */
    static boolean zetNaar(ServerPlayer p, String id, float yaw) {
        ServerLevel g = guheinde(p.level().getServer());
        Vec3 naar = midden(g, id);
        if (naar == null) {
            return false;
        }
        p.teleportTo(g, naar.x, naar.y + 0.05, naar.z, Set.of(), yaw, 0f, true);
        p.resetFallDistance();
        return true;
    }

    /** The marker blocks of the five crates round the grill (d1s_krat_1..5, this slice's own markers). */
    static List<BlockPos> kratten(@Nullable ServerLevel g) {
        List<BlockPos> uit = new ArrayList<>();
        for (int i = 1; i <= GrillSpel.Deel.values().length; i++) {
            BlockPos b = plek(g, "d1s_krat_" + i);
            if (b == null) {
                return List.of();
            }
            uit.add(b);
        }
        return uit;
    }

    // =====================================================================================================================
    // small things for the player
    // =====================================================================================================================

    /** A big title (and an optional subtitle) for this player only. */
    static void titel(ServerPlayer p, Component titel, @Nullable Component onder, int blijf) {
        if (p.connection == null || p instanceof FakePlayer) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, blijf, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(onder == null ? Component.empty() : onder));
        p.connection.send(new ClientboundSetTitleTextPacket(titel));
    }

    /** Particles this player sees from far away (a column of light over a spot). */
    static void ver(ServerPlayer p, ParticleOptions deeltje, Vec3 plek, int aantal, double dx, double dy, double dz, double snelheid) {
        if (p.connection != null && !(p instanceof FakePlayer)) {
            p.level().sendParticles(p, deeltje, true, true, plek.x, plek.y, plek.z, aantal, dx, dy, dz, snelheid);
        }
    }

    /** A grey "* ..." line in the chat (what you notice, not what somebody says). */
    static void zie(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.literal("* ").append(tekst(key, args)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** The cap of the job interview (once per player; the item unlocks the piece in the wardrobe). */
    static void geefPetje(ServerPlayer p) {
        if (lijn().eenmalig(p, "sp_d1_start_petje")) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.SPONSGUH_KNABBELPETJE)));
            p.sendSystemMessage(tekst("beloning.petje").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
