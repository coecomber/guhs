package nl.juiced.guhs.feature.torenpeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De Rookguh-vuurtoren (structure guhs:rookguh_vuurtoren, one template of the same name): where its things are, in template
 * coordinates, and everything of the Torenwachter-guh's questline that happens away from him.
 * The numbers come from tools/features/toren_peper_bouw.py (PLEKKEN_TOREN); the generator's self-check compares them with
 * this file, so keep the {@code new BlockPos(x, y, z)} lines in this shape.
 * <ul>
 *   <li><b>The lamp</b> ({@link VuurtorenlampBlock}): the lamps that tick report themselves here ({@link #meld}); a lamp
 *       burns while a player who lit one is near ({@link #moetBranden}); {@link #klikLamp} is step 2 of the questline.</li>
 *   <li><b>The lost Rookguhs</b> ({@link VerdwaaldeRookguhEntity}): at step 3 every player has up to {@link #ROOKGUHS} of
 *       their own drifting around the nearest lamp ({@link #tik}, once they are within {@link #ROOKGUH_BEREIK} blocks of it); with the seinlantaarn in hand they follow, and within
 *       {@link #THUIS_STRAAL} blocks of the burning lamp they see the light, eat their fill and float home
 *       ({@link #thuisgekomen}). They belong to one player, are never saved and leave when that player does.</li>
 * </ul>
 */
public final class Vuurtoren {
    public static final String STRUCTUUR = "rookguh_vuurtoren";
    /** The Bezetting id (and the tag of the template's own NPC). */
    public static final String TORENWACHTER = "torenpeper_torenwachter";
    /** The template's ground layer. */
    public static final int G = 3;

    public static final BlockPos NPC = new BlockPos(15, 4, 20);
    public static final float NPC_YAW = 0.0f;
    /** The lamp in the lantern room. */
    public static final BlockPos LAMP = new BlockPos(15, 24, 13);
    /** On the gallery, in front of the lantern room's door. */
    public static final BlockPos BAK = new BlockPos(15, 23, 17);
    /** Just outside the tower door. */
    public static final BlockPos DEUR = new BlockPos(15, 4, 17);

    /** Gloeikoolgruis the keeper asks for (step 1), and how many lost Rookguhs have to come home (step 3). */
    public static final int GRUIS_NODIG = 4, ROOKGUHS = 3;
    /** A lamp burns for players (who lit one) within this many blocks of it. */
    public static final int BEREIK = 96;
    /** A player's lost Rookguhs appear once the player is within this many blocks of the lamp (so they are where the world ticks). */
    public static final int ROOKGUH_BEREIK = 48;
    /** Within this many blocks (sideways) of a burning lamp a lost Rookguh sees the light. */
    public static final double THUIS_STRAAL = 9.0;
    /** Lost Rookguhs appear between these distances (sideways) from the lamp. */
    public static final int START_MIN = 16, START_MAX = 28;

    private static final Verhaallijn LIJN = TorenpeperFeature.VUURTOREN;
    /** The lamps that are ticking, per dimension: position -> the game time they last reported. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> LAMPEN = new ConcurrentHashMap<>();

    private Vuurtoren() {
    }

    // --- the lamps ---------------------------------------------------------------------------------------------------------------

    /** A lamp's block entity ticked (once a second). */
    public static void meld(ServerLevel level, BlockPos pos) {
        LAMPEN.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>()).put(pos.immutable(), level.getGameTime());
    }

    public static void vergeet(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> lampen = LAMPEN.get(level.dimension());
        if (lampen != null) {
            lampen.remove(pos);
        }
    }

    /** The ticking lamp nearest to this spot within {@code bereik} blocks (null: none). */
    @Nullable
    public static BlockPos lampBij(ServerLevel level, BlockPos bij, double bereik) {
        Map<BlockPos, Long> lampen = LAMPEN.get(level.dimension());
        if (lampen == null) {
            return null;
        }
        BlockPos beste = null;
        double d = bereik * bereik;
        long nu = level.getGameTime();
        for (Map.Entry<BlockPos, Long> e : lampen.entrySet()) {
            if (nu - e.getValue() > 100 || nu < e.getValue()) {
                lampen.remove(e.getKey());   // (its chunk was unloaded)
                continue;
            }
            double a = e.getKey().distSqr(bij);
            if (a <= d) {
                d = a;
                beste = e.getKey();
            }
        }
        return beste;
    }

    /** Has this player lit the lamp (the questline's step 2 is behind them)? */
    public static boolean heeftAangestoken(ServerPlayer p) {
        return LIJN.stap(p) >= 3;
    }

    /** Should this lamp burn now: is a player who lit one within {@link #BEREIK} blocks? */
    public static boolean moetBranden(ServerLevel level, BlockPos lamp) {
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.blockPosition().distSqr(lamp) <= (double) BEREIK * BEREIK && heeftAangestoken(p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean brandt(Level level, BlockPos lamp) {
        BlockState state = level.getBlockState(lamp);
        return state.is(TorenpeperFeature.VUURTORENLAMP.get()) && state.getValue(VuurtorenlampBlock.LIT);
    }

    /** The player clicked a lamp: at step 2, with the lampkooltje, it is lit (for good, as far as this player goes). */
    public static void klikLamp(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        int stap = LIJN.stap(p);
        if (stap < 2) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.lamp.uit").withStyle(ChatFormatting.GRAY));
        } else if (stap == 2) {
            if (GuhQuests.count(p, TorenpeperFeature.LAMPKOOLTJE.get()) == 0) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.lamp.geen_kooltje").withStyle(ChatFormatting.GRAY));
                return;
            }
            GuhQuests.take(p, TorenpeperFeature.LAMPKOOLTJE.get(), 1);
            if (LIJN.verder(p, 2)) {
                meld(level, pos);
                level.setBlock(pos, level.getBlockState(pos).setValue(VuurtorenlampBlock.LIT, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
                level.playSound(null, pos, TorenpeperFeature.LAMP_AAN.get(), SoundSource.BLOCKS, 1.5f, 1f);
                level.playSound(null, pos, TorenpeperFeature.MISTHOORN.get(), SoundSource.BLOCKS, 4f, 1f);
                level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.03);
                p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.lamp.aan").withStyle(ChatFormatting.GOLD));
                GuhQuests.hint(p, "quest.guhs.torenpeper.hint.rookguhs");
            }
        } else {
            p.sendOverlayMessage(Component.translatable(stap == 3 ? "gui.guhs.torenpeper.lamp.brandt_zoek" : "gui.guhs.torenpeper.lamp.brandt",
                    thuis(p), ROOKGUHS).withStyle(ChatFormatting.GOLD));
        }
    }

    // --- the lost Rookguhs ----------------------------------------------------------------------------------------------------------

    /** How many lost Rookguhs this player brought home (0..{@link #ROOKGUHS}). */
    public static int thuis(ServerPlayer p) {
        return Math.min(ROOKGUHS, LIJN.teller(p, "thuis"));
    }

    public static boolean houdtLantaarn(ServerPlayer p) {
        return p.getMainHandItem().is(TorenpeperFeature.SEINLANTAARN.get()) || p.getOffhandItem().is(TorenpeperFeature.SEINLANTAARN.get());
    }

    /** This player's lost Rookguhs within reach of the lamp that still have to come home. */
    public static List<VerdwaaldeRookguhEntity> van(ServerLevel level, UUID gids, BlockPos lamp) {
        return level.getEntitiesOfClass(VerdwaaldeRookguhEntity.class, new AABB(lamp).inflate(BEREIK + 16),
                r -> r.isAlive() && gids.equals(r.gids()) && !r.isThuis());
    }

    /** One of this player's lost Rookguhs came home (it turned VAHOEG): counted once; the last one finishes step 3. */
    public static void thuisgekomen(ServerPlayer p, VerdwaaldeRookguhEntity rookguh) {
        if (LIJN.stap(p) != 3) {
            return;
        }
        int n = LIJN.teller(p, "thuis") + 1;
        LIJN.teller(p, "thuis", n);
        if (n >= ROOKGUHS) {
            if (LIJN.verder(p, 3)) {
                GuhQuests.hint(p, "quest.guhs.torenpeper.hint.allemaal_thuis");
            }
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.rookguh.thuis", n, ROOKGUHS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * Once a second for every player ({@link TorenpeperEvents}): at step 3, near a lamp, the lost Rookguhs this player still
     * has to bring home are there (one new one per second, so three don't pop up at once).
     */
    public static void tik(ServerPlayer p) {
        if (LIJN.stap(p) != 3 || p.isSpectator()) {
            return;
        }
        ServerLevel level = p.level();
        BlockPos lamp = lampBij(level, p.blockPosition(), ROOKGUH_BEREIK);
        if (lamp == null) {
            return;
        }
        if (thuis(p) + van(level, p.getUUID(), lamp).size() < ROOKGUHS) {
            laatVerdwalen(p, lamp);
        }
    }

    /** A new lost Rookguh for this player, somewhere around this lamp (null: it could not be made). */
    @Nullable
    public static VerdwaaldeRookguhEntity laatVerdwalen(ServerPlayer p, BlockPos lamp) {
        ServerLevel level = p.level();
        VerdwaaldeRookguhEntity rookguh = TorenpeperFeature.VERDWAALDE_ROOKGUH.get().create(level, EntitySpawnReason.EVENT);
        if (rookguh == null) {
            return null;
        }
        Vec3 plek = startplek(level, lamp, rookguh, level.getRandom());
        rookguh.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        rookguh.begin(p.getUUID(), lamp);
        level.addFreshEntity(rookguh);
        level.sendParticles(ParticleTypes.CLOUD, plek.x, plek.y + 0.7, plek.z, 12, 0.5, 0.4, 0.5, 0.01);
        return rookguh;
    }

    /**
     * Where a lost Rookguh starts: a free spot {@link #START_MIN}..{@link #START_MAX} blocks from the lamp, low enough to walk
     * to, a little above the ground. When the cave gives no such spot (or the lamp stands somewhere odd): next to the
     * gallery, where a real tower always has room.
     */
    static Vec3 startplek(ServerLevel level, BlockPos lamp, VerdwaaldeRookguhEntity rookguh, RandomSource random) {
        for (int poging = 0; poging < 40; poging++) {
            double hoek = random.nextDouble() * Math.PI * 2;
            double afstand = START_MIN + random.nextDouble() * (START_MAX - START_MIN);
            double x = lamp.getX() + 0.5 + Math.cos(hoek) * afstand, z = lamp.getZ() + 0.5 + Math.sin(hoek) * afstand;
            int top = lamp.getY() - 4;
            // (only where entities tick: a lost Rookguh in a chunk that sleeps would never come, or leave)
            if (!level.isPositionEntityTicking(BlockPos.containing(x, top, z)) || !vrij(level, rookguh, x, top, z)) {
                continue;
            }
            // down to the ground (at most the height of the tower), then a little up again
            int y = top;
            while (y > lamp.getY() - 26 && y > level.getMinY() + 2 && vrij(level, rookguh, x, y - 1, z)) {
                y--;
            }
            if (y == lamp.getY() - 26) {
                continue;   // (no ground under it: a hole, or the edge of a cliff)
            }
            int hoog = Math.min(top, y + 2);
            if (level.getFluidState(BlockPos.containing(x, y - 1, z)).isEmpty() && vrij(level, rookguh, x, hoog, z)) {
                return new Vec3(x, hoog, z);
            }
        }
        Vec3 plek = Vec3.atBottomCenterOf(lamp.above(2));
        for (int poging = 0; poging < 16; poging++) {
            double hoek = random.nextDouble() * Math.PI * 2;
            plek = new Vec3(lamp.getX() + 0.5 + Math.cos(hoek) * 7.0, lamp.getY() - 2.0, lamp.getZ() + 0.5 + Math.sin(hoek) * 7.0);
            if (level.isPositionEntityTicking(BlockPos.containing(plek)) && vrij(level, rookguh, plek.x, plek.y, plek.z)) {
                break;
            }
        }
        return plek;
    }

    private static boolean vrij(ServerLevel level, VerdwaaldeRookguhEntity rookguh, double x, double y, double z) {
        return level.noCollision(rookguh, rookguh.getType().getDimensions().makeBoundingBox(x, y, z).inflate(0.2));
    }

    /** All lost Rookguhs of this player, wherever they are (they leave when the player logs out or changes dimension). */
    public static List<VerdwaaldeRookguhEntity> alleVan(ServerLevel level, UUID gids) {
        List<VerdwaaldeRookguhEntity> uit = new ArrayList<>();
        for (var e : level.getAllEntities()) {
            if (e instanceof VerdwaaldeRookguhEntity r && gids.equals(r.gids())) {
                uit.add(r);
            }
        }
        return uit;
    }

    // --- dev -------------------------------------------------------------------------------------------------------------------------

    /** (dev command, tests) Places a template of this slice with its corner here, unturned, with its entities. */
    public static boolean plaats(ServerLevel level, String structuur, BlockPos hoek) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(structuur));
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        return true;
    }
}
