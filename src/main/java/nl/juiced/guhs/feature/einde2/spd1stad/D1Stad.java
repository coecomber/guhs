package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.function.Consumer;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh", slice sp_d1_stad (CONTRACT_E2_C 7.4): what the two steps of this slice share. Part 1 steps
 * {@value SpVerhaal#R_KWALLEN} (jellyfishing with Patguh, {@link Kwallen}) and {@value SpVerhaal#R_EXAMEN} (the boating exam
 * with Sponsguh beside you, {@link Examen}). Every bit of progress lives in the line {@link SpVerhaal#RECEPT_LIJN}: the step,
 * and while a step runs its two flags and one counter of this slice (cleared when the step is done):
 * <ul>
 *   <li>{@value #V_NET}: the scene {@code patguh} was seen (you have the net): step 5 shows as {@code 5_b} (catch);</li>
 *   <li>{@value #T_KWALLEN}: how many jellies are in your jar (0..{@value Kwallen#NODIG});</li>
 *   <li>{@value #V_EXAMEN}: the scene {@code pufguh} was seen (the boat waits): step 6 shows as {@code 6_b} (sail);</li>
 *   <li>{@value #V_GESLAAGD}: through the finish (only the scene after it is still to come).</li>
 * </ul>
 * Actors of ONE player (the field's Patguh, the jellies, the boat and its Sponsguh) carry {@value #EIGEN} and
 * {@code Zicht.alleenVoor}; they are never kept: a loaded one is thrown away ({@link E2SpD1StadFeature}).
 */
public final class D1Stad {
    public static final String LIJN = SpVerhaal.RECEPT;
    public static final String STRUCTUUR = SpVerhaal.BIKINIBODEM;
    public static final int KWALLEN = SpVerhaal.R_KWALLEN, EXAMEN = SpVerhaal.R_EXAMEN;
    /** The lang prefix of the slice. */
    public static final String P = "gui.guhs.einde2_sp_d1_stad.";
    /** The line's flags and counter of this slice (CONTRACT_E2_C 2.4: {@code <slice>_<naam>}). */
    public static final String V_NET = "sp_d1_stad_net", V_EXAMEN = "sp_d1_stad_examen",
            V_GESLAAGD = "sp_d1_stad_geslaagd", T_KWALLEN = "sp_d1_stad_kwallen";
    /** Entity tag of every actor of one player (never kept). */
    public static final String EIGEN = "guhs_einde2_sp_d1_stad_eigen";

    /** (tests) no scenes, no waits: a mock player cannot click a scene on. */
    static volatile boolean PROEF;

    private D1Stad() {
    }

    static Verhaallijn lijn() {
        return SpVerhaal.RECEPT_LIJN;
    }

    /** Is this step of part 1 the player's own step now (the gate open, part 1 on, exactly at it)? */
    static boolean aanZet(ServerPlayer p, int stap) {
        return SpVerhaal.open(p) && SpVerhaal.actief(p, LIJN) && lijn().stap(p) == stap;
    }

    /** The second half of the step began (the reason scene was seen). */
    static boolean tweedeHelft(ServerPlayer p, int stap) {
        return lijn().vlag(p, stap == KWALLEN ? V_NET : V_EXAMEN);
    }

    /** The sleutel of steps 5 and 6: "5" / "5_b" and "6" / "6_b" (CONTRACT_E2_C 2.3). */
    static String sleutel(ServerPlayer p, int stap) {
        return tweedeHelft(p, stap) ? stap + "_b" : String.valueOf(stap);
    }

    // =====================================================================================================================
    // places (the guaranteed copy of Bikinibodem; Leegte builds are never rotated)
    // =====================================================================================================================

    @Nullable
    static ServerLevel guheinde(MinecraftServer server) {
        return server == null ? null : server.getLevel(GuheindeFeature.GUHEINDE);
    }

    /** The world block of a marker of Bikinibodem (null: not known yet / no such marker). */
    @Nullable
    static BlockPos wereld(@Nullable ServerLevel g, String plek) {
        return g == null || !Plekken.bekend(g, STRUCTUUR) ? null : Plekken.wereld(g, STRUCTUUR, plek);
    }

    /** The middle of a marker's floor in the world (null: not known). */
    @Nullable
    static Vec3 midden(@Nullable ServerLevel g, String plek) {
        BlockPos b = wereld(g, plek);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** A spot relative to a marker (template axes; null: not known). */
    @Nullable
    static Vec3 bij(@Nullable ServerLevel g, String plek, double dx, double dy, double dz) {
        Vec3 m = midden(g, plek);
        return m == null ? null : m.add(dx, dy, dz);
    }

    /** Is the spot inside a zone marker of Bikinibodem, grown by {@code rand} blocks? */
    static boolean inZone(@Nullable ServerLevel g, String zone, Vec3 pos, double rand) {
        if (g == null || !Plekken.bekend(g, STRUCTUUR)) {
            return false;
        }
        Plekken.Plek z = Plekken.plek(STRUCTUUR, zone);
        BlockPos o = Plekken.oorsprong(g, STRUCTUUR);
        if (z == null || o == null) {
            return false;
        }
        BlockPos a = o.offset(z.pos()), b = o.offset(z.tot() == null ? z.pos() : z.tot());
        AABB doos = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1).inflate(rand);
        return doos.contains(pos);
    }

    /** The world points of a pad marker (empty: not known). */
    static java.util.List<Vec3> pad(@Nullable ServerLevel g, String plek) {
        if (g == null || !Plekken.bekend(g, STRUCTUUR)) {
            return java.util.List.of();
        }
        Plekken.Plek z = Plekken.plek(STRUCTUUR, plek);
        BlockPos o = Plekken.oorsprong(g, STRUCTUUR);
        return z == null || o == null ? java.util.List.of() : z.punten().stream().map(b -> Vec3.atBottomCenterOf(o.offset(b))).toList();
    }

    // =====================================================================================================================
    // talking
    // =====================================================================================================================

    /** "&lt;Name&gt; text" for this player, spoken by a character (its own display name). */
    static void zeg(ServerPlayer p, Entity wie, String key, Object... args) {
        p.sendSystemMessage(Component.literal("<").append(wie.getDisplayName()).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(P + key, args).withStyle(ChatFormatting.WHITE)));
    }

    /** "&lt;Name&gt; text" with a cast name key ("entity.guhs.patguh"). */
    static void zegAls(ServerPlayer p, String naamKey, String key, Object... args) {
        p.sendSystemMessage(Component.literal("<").append(Component.translatable(naamKey)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(P + key, args).withStyle(ChatFormatting.WHITE)));
    }

    /** A sound for this player only, where they stand. */
    static void geluid(ServerPlayer p, net.minecraft.sounds.SoundEvent geluid, float volume, float toon) {
        if (p.connection != null) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(geluid), net.minecraft.sounds.SoundSource.PLAYERS,
                    p.getX(), p.getY(), p.getZ(), volume, toon, p.getRandom().nextLong()));
        }
    }

    /** A line over the hotbar. */
    static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(P + key, args).withStyle(ChatFormatting.AQUA));
    }

    /** A title in the middle of the screen (and a subtitle). */
    static void titel(ServerPlayer p, String key, @Nullable String onder, Object... args) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 14));
        if (onder != null) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(P + onder, args).withStyle(ChatFormatting.YELLOW)));
        }
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(P + key).withStyle(ChatFormatting.GOLD)));
    }

    // =====================================================================================================================
    // scenes and hints
    // =====================================================================================================================

    /**
     * Plays a scene at a marker of Bikinibodem for the player; when it cannot play (no copy, the player watches something,
     * or a test) {@code daarna} runs at once, so the story never sticks on a scene.
     */
    static void speel(ServerPlayer p, Cutscene scene, String anker, Consumer<ServerPlayer> daarna) {
        BlockPos a = PROEF ? null : wereld(guheinde(p.level().getServer()), anker);
        if (a == null || p.connection == null || !Cutscenes.speel(p, scene, a, Rotation.NONE, daarna)) {
            daarna.accept(p);
        }
    }

    /**
     * The hint of a phase of a step (CONTRACT_E2_C 4.4): its ~3 s look scene at the target, then one chat line (WHAT and
     * WHERE, the direction as %s) and the direction over the hotbar. Waits while a scene runs (at most five minutes) and is
     * dropped when the player's step changed meanwhile.
     */
    static void hint(ServerPlayer p, int stap, Cutscene kijk, String marker, String tekst, int wacht) {
        hint(p, stap, kijk, marker, tekst, wacht, 15);
    }

    private static void hint(ServerPlayer p, int stap, Cutscene kijk, String marker, String tekst, int wacht, int pogingen) {
        StapHint.later(p, wacht, s -> {
            if (!aanZet(s, stap)) {
                return;
            }
            if (Cutscenes.bezig(s)) {
                if (pogingen > 0) {
                    hint(s, stap, kijk, marker, tekst, 20, pogingen - 1);
                }
                return;
            }
            BlockPos doel = wereld(guheinde(s.level().getServer()), marker);
            if (doel == null || s.level().dimension() != GuheindeFeature.GUHEINDE) {
                return;
            }
            Consumer<ServerPlayer> regel = q -> regel(q, tekst, Vec3.atCenterOf(doel));
            if (!StapHint.speelKijk(s, kijk, doel, regel)) {
                regel.accept(s);
            }
        });
    }

    /** "➜ text (direction)" in gold and yellow, and the direction over the hotbar. */
    static void regel(ServerPlayer p, String tekst, Vec3 doel) {
        MutableComponent richting = Richting.van(p.getEyePosition(), doel);
        p.sendSystemMessage(Component.literal("➜ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(P + tekst, richting).withStyle(ChatFormatting.YELLOW)));
        p.sendOverlayMessage(Component.translatable(StapHint.P + "balk", richting.copy()).withStyle(ChatFormatting.GOLD));
    }

    // =====================================================================================================================
    // actors of one player
    // =====================================================================================================================

    /** A character of the cast for this player only, standing on its mark (null: none could be made). */
    @Nullable
    static <T extends SponsguhWezen> T wezen(ServerLevel level, EntityType<T> type, Vec3 plek, float yaw, ServerPlayer voor) {
        T e = type.create(level, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.zetStil(true);
        eigen(e, voor);
        level.addFreshEntity(e);
        return e;
    }

    /** Marks an entity as an actor of this one player (seen only by them, never kept). */
    static void eigen(Entity e, ServerPlayer voor) {
        e.addTag(EIGEN);
        Zicht.alleenVoor(e, voor.getUUID());
    }

    /** The loaded entity with this id in the Guheinde (null: gone, unloaded). */
    @Nullable
    static Entity levend(@Nullable ServerLevel level, @Nullable java.util.UUID id) {
        if (level == null || id == null) {
            return null;
        }
        Entity e = level.getEntity(id);
        return e != null && e.isAlive() ? e : null;
    }

    static void weg(@Nullable ServerLevel level, @Nullable java.util.UUID id) {
        Entity e = levend(level, id);
        if (e instanceof LesbootEntity boot) {
            boot.weg();
        } else if (e != null) {
            e.discard();
        }
    }
}
