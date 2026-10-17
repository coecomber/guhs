package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 fix4-appa (playtest 3): the player's own Appa-platform, "&lt;naam&gt;s Appa-platform".
 * <ul>
 *   <li><b>The block</b> ({@link AppaPlatformBlok}): the middle of the platform. Whoever finished the whole Guhvatar story
 *       (the finale, the challenge "De Guhvatar") gets one ({@link #geschenkTick}); it is also craftable and drops itself.
 *       Placed by such a player it becomes THEIR platform in that dimension (one per player per dimension; a newer one
 *       replaces the older). A click on it shows whether it is ready; a click on someone else's makes it yours too.</li>
 *   <li><b>The 5x5 rule</b> ({@link #controleer}): the platform block in the middle of a 5x5 square whose 24 other blocks
 *       have a solid top, and 3 blocks of free air above all 25. Checked when it is placed, clicked and used (whistle or
 *       flight); what is missing is said (how many, one spot) and shown with little clouds.</li>
 *   <li><b>The whistle outside the Guheinde</b> ({@link #fluit}): within {@value #BEREIK} blocks of your own (complete)
 *       platform in that dimension Appa comes down onto it; anywhere else he does not come, and a line says why. In the
 *       Guheinde the whistle keeps working everywhere; the Geestenwereld keeps its own rules.</li>
 *   <li><b>The menu</b>: your platforms come first in Appa's list ({@link Fluitlijst}); a flight there is a real flight in
 *       the same dimension, and the dive through the void from another one ({@link AppaReis}).</li>
 * </ul>
 * Saved per player in {@value #KEY}: {dimension id -> [x, y, z]}.
 */
public final class AppaPlatform {
    /** The player data: {"minecraft:overworld": [x, y, z], ...}. */
    public static final String KEY = "guhs_einde2_reizen_platform";
    /** The player data flag: the platform of the story's end was given. */
    public static final String GEGEVEN = "guhs_einde2_reizen_platform_gegeven";
    /** How near (blocks) the whistle must be blown to your own platform outside the Guheinde. */
    public static final double BEREIK = 20;
    /** Half the side of the square (5x5) and the free air above it. */
    public static final int HALF = 2, LUCHT = 3;
    /** The id prefix of a platform row in Appa's menu: "#platform:" + dimension id. */
    public static final String RIJ = "#platform:";

    private AppaPlatform() {
    }

    // =====================================================================================================================
    // the name
    // =====================================================================================================================

    /**
     * "&lt;naam&gt;s Appa-platform" in good Dutch: Joosts, Anna's, Max' (EN always "Name's"). The form is picked here from the
     * name's last character; each language says it with its own key.
     */
    public static Component naam(String speler) {
        return Component.translatable("gui.guhs.einde2_reizen.platform.naam." + vorm(speler), speler);
    }

    /**
     * The Dutch genitive form of a name: "a" = only an apostrophe (after s, x, z: Max'), "as" = apostrophe + s (after a, i,
     * o, u, y, a digit or another sign: Anna's, Steve_'s), "s" = a plain s (everything else: Joosts, Annes).
     */
    public static String vorm(String speler) {
        if (speler.isEmpty()) {
            return "as";
        }
        char c = Character.toLowerCase(speler.charAt(speler.length() - 1));
        if (c == 's' || c == 'x' || c == 'z') {
            return "a";
        }
        if ("aiouy".indexOf(c) >= 0 || !Character.isLetter(c)) {
            return "as";
        }
        return "s";
    }

    // =====================================================================================================================
    // the saved platforms
    // =====================================================================================================================

    /** The player's platforms: dimension -> the middle block, in the order they were made. */
    public static Map<ResourceKey<Level>, BlockPos> alle(ServerPlayer p) {
        Map<ResourceKey<Level>, BlockPos> uit = new LinkedHashMap<>();
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(KEY);
        for (String dim : t.keySet()) {
            int[] xyz = t.getIntArray(dim).orElse(new int[0]);
            Identifier id = Identifier.tryParse(dim);
            if (xyz.length == 3 && id != null) {
                uit.put(ResourceKey.create(Registries.DIMENSION, id), new BlockPos(xyz[0], xyz[1], xyz[2]));
            }
        }
        return uit;
    }

    /** The player's platform in this dimension, or null. */
    @Nullable
    public static BlockPos van(ServerPlayer p, ResourceKey<Level> dim) {
        return alle(p).get(dim);
    }

    /** Makes this block the player's platform in its dimension (an older one there stops counting). */
    public static void zet(ServerPlayer p, ResourceKey<Level> dim, BlockPos pos) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY).copy();
        t.putIntArray(dim.identifier().toString(), new int[]{pos.getX(), pos.getY(), pos.getZ()});
        saved.put(KEY, t);
    }

    /** Forgets the player's platform in this dimension. */
    public static void vergeet(ServerPlayer p, ResourceKey<Level> dim) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY).copy();
        t.remove(dim.identifier().toString());
        saved.put(KEY, t);
    }

    /** May this player have a platform (the whole Guhvatar story is done)? */
    public static boolean mag(ServerPlayer p) {
        return E2Verhaal.finaleKlaar(p);
    }

    // =====================================================================================================================
    // the 5x5 rule
    // =====================================================================================================================

    /**
     * What a platform still misses. {@code blok}: the platform block itself stands in the middle; {@code vloer}: the spots of
     * the 5x5 square (around it, at its height) without a solid top; {@code inDeWeg}: blocks in the 3 blocks of air above
     * the square.
     */
    public record Controle(boolean blok, List<BlockPos> vloer, List<BlockPos> inDeWeg) {
        public boolean ok() {
            return blok && vloer.isEmpty() && inDeWeg.isEmpty();
        }
    }

    /** Checks the platform with its middle at {@code midden} (the chunk must be loaded). */
    public static Controle controleer(Level level, BlockPos midden) {
        boolean blok = level.getBlockState(midden).is(E2ReizenFeature.APPA_PLATFORM.get());
        List<BlockPos> vloer = new ArrayList<>();
        List<BlockPos> weg = new ArrayList<>();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                BlockPos q = midden.offset(dx, 0, dz);
                if ((dx != 0 || dz != 0) && !level.getBlockState(q).isFaceSturdy(level, q, Direction.UP)) {
                    vloer.add(q);
                }
                for (int dy = 1; dy <= LUCHT; dy++) {
                    BlockPos b = q.above(dy);
                    BlockState s = level.getBlockState(b);
                    if (!s.getCollisionShape(level, b).isEmpty() || !s.getFluidState().isEmpty()) {
                        weg.add(b);
                    }
                }
            }
        }
        return new Controle(blok, vloer, weg);
    }

    /** Tells the player what the platform at {@code midden} misses (or that it is ready), with clouds at the spots. */
    public static void meld(ServerPlayer p, ServerLevel level, BlockPos midden, Controle c) {
        if (!c.blok()) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.weg"), ChatFormatting.LIGHT_PURPLE);
            return;
        }
        Component naam = naam(p.getGameProfile().name());
        if (c.ok()) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.klaar", naam, (int) BEREIK), ChatFormatting.GREEN);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, midden.getX() + 0.5, midden.getY() + 1.2, midden.getZ() + 0.5,
                    20, 1.6, 0.3, 1.6, 0);
            return;
        }
        if (!c.vloer().isEmpty()) {
            BlockPos b = c.vloer().get(0);
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.vloer", naam, c.vloer().size(), plek(b)), ChatFormatting.GOLD);
            for (BlockPos q : c.vloer()) {
                level.sendParticles(p, ParticleTypes.ANGRY_VILLAGER, true, false, q.getX() + 0.5, q.getY() + 1.1, q.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0);
            }
        }
        if (!c.inDeWeg().isEmpty()) {
            BlockPos b = c.inDeWeg().get(0);
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.ruimte", naam, c.inDeWeg().size(), plek(b), LUCHT), ChatFormatting.GOLD);
            for (BlockPos q : c.inDeWeg()) {
                level.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, false, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    private static String plek(BlockPos b) {
        return b.getX() + " " + b.getY() + " " + b.getZ();
    }

    // =====================================================================================================================
    // the block (AppaPlatformBlok calls these)
    // =====================================================================================================================

    /** The block was put down by this player. */
    static void geplaatst(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!mag(p)) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.te_vroeg"), ChatFormatting.LIGHT_PURPLE);
            return;
        }
        BlockPos oud = van(p, level.dimension());
        zet(p, level.dimension(), pos);
        if (oud != null && !oud.equals(pos)) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.verplaatst"), ChatFormatting.AQUA);
        }
        meld(p, level, pos, controleer(level, pos));
    }

    /** A click on the block (empty hand). */
    static void klik(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (!mag(p)) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.te_vroeg"), ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (!pos.equals(van(p, level.dimension()))) {
            zet(p, level.dimension(), pos);
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.van_jou", naam(p.getGameProfile().name())), ChatFormatting.AQUA);
        }
        meld(p, level, pos, controleer(level, pos));
    }

    /** The block is broken by this player: when it was their platform, it is forgotten. */
    static void gesloopt(ServerPlayer p, ServerLevel level, BlockPos pos) {
        if (pos.equals(van(p, level.dimension()))) {
            vergeet(p, level.dimension());
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.opgeruimd"), ChatFormatting.LIGHT_PURPLE);
        }
    }

    // =====================================================================================================================
    // the whistle outside the Guheinde
    // =====================================================================================================================

    /**
     * The Appa-fluitje blown (not on his head, not sneaking) by a player who has an Appa that is not kept away. True: handled
     * here (outside the Guheinde: Appa comes down onto the platform, or a line why he doesn't); false: the old whistle (in
     * the Guheinde, and in the Geestenwereld: he comes wherever you are).
     */
    public static boolean fluit(ServerPlayer p) {
        Level level = p.level();
        if (GuheindeFeature.isGuheinde(level) || level.dimension() == Geestenwereld.DIM || !(level instanceof ServerLevel server)) {
            return false;
        }
        BlockPos pos = van(p, level.dimension());
        double ver = pos == null ? Double.MAX_VALUE : Math.sqrt(p.distanceToSqr(Vec3.atBottomCenterOf(pos.above())));
        if (pos == null || ver > BEREIK) {
            zeg(p, Component.translatable("gui.guhs.einde2_reizen.platform.alleen_guheinde"), ChatFormatting.LIGHT_PURPLE);
            if (pos != null) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.platform.te_ver", naam(p.getGameProfile().name()), (int) Math.round(ver),
                        (int) BEREIK).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return true;
        }
        Controle c = controleer(server, pos);
        if (!c.ok()) {
            if (!c.blok()) {
                vergeet(p, level.dimension());
            }
            meld(p, server, pos, c);
            return true;
        }
        AppaGuhEntity appa = Appa.roepNaar(p, aanvliegen(server, pos));
        if (appa != null) {
            appa.brul();
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.platform.komt", appa.getDisplayName(), naam(p.getGameProfile().name()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** Where Appa appears when called to his platform: up to 12 blocks over its middle, as high as the column is free; he sinks onto it. */
    static Vec3 aanvliegen(ServerLevel level, BlockPos midden) {
        var type = E2AppaFeature.APPA_GUH.get();
        Vec3 op = Vec3.atBottomCenterOf(midden.above());
        for (int h = 12; h > 0; h--) {
            var kolom = type.getSpawnAABB(op.x, op.y, op.z).expandTowards(0, h, 0);
            if (level.noCollision(kolom)) {
                return op.add(0, h, 0);
            }
        }
        return op;
    }

    /** The landing onto this platform: Appa on its middle, his riders stay on him. */
    static Appa.Landing landing(BlockPos midden) {
        return new Appa.Landing(Vec3.atBottomCenterOf(midden.above()), Float.NaN, null, null, false);
    }

    /** Where a rider stands after the dive through the void onto a platform: on the square, beside Appa. */
    static Vec3 staanBij(BlockPos midden) {
        return Vec3.atBottomCenterOf(midden.above()).add(0, 0, -HALF);
    }

    /** The level of a saved dimension, or null (it is not there). */
    @Nullable
    static ServerLevel level(MinecraftServer server, ResourceKey<Level> dim) {
        return server.getLevel(dim);
    }

    /** The menu row id of a platform in this dimension. */
    static String rijId(ResourceKey<Level> dim) {
        return RIJ + dim.identifier();
    }

    /** The dimension of a platform row id, or null (not one). */
    @Nullable
    static ResourceKey<Level> dimVan(String rij) {
        if (!rij.startsWith(RIJ)) {
            return null;
        }
        Identifier id = Identifier.tryParse(rij.substring(RIJ.length()));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    /** The lang key of a dimension's name in the menu (falls back on the plain id). */
    public static String dimSleutel(String dim) {
        return "gui.guhs.einde2_reizen.platform.dim." + dim.replace(':', '.').toLowerCase(Locale.ROOT);
    }

    // =====================================================================================================================
    // the gift at the story's end
    // =====================================================================================================================

    /** Every few seconds per player: whoever finished the story (also before this update) gets the platform once. */
    static void geschenkTick(ServerPlayer p) {
        if (!mag(p) || GuhQuests.saved(p).getBooleanOr(GEGEVEN, false)) {
            return;
        }
        GuhQuests.saved(p).putBoolean(GEGEVEN, true);
        Minigames.give(p, new ItemStack(E2ReizenFeature.APPA_PLATFORM.get()));
        E2Reizen.zeg(p, "aang", "gui.guhs.einde2_reizen.platform.geschenk.1");
        E2Reizen.zeg(p, "", "gui.guhs.einde2_reizen.platform.geschenk.2");
    }

    private static void zeg(ServerPlayer p, Component tekst, ChatFormatting kleur) {
        p.sendSystemMessage(tekst.copy().withStyle(kleur));
    }
}
