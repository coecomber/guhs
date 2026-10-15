package nl.juiced.guhs.feature.bio.systemen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.dieren.KikkerBlad;
import nl.juiced.guhs.feature.bio.kompas.BiomeKompas;
import nl.juiced.guhs.feature.bio.wereld.BioModel;
import nl.juiced.guhs.feature.bio.wereld.Kaart;
import nl.juiced.guhs.feature.bio.wereld.WolkTerrein;
import nl.juiced.guhs.feature.kaasmoeras.KikkerguhEntity;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * biomes3 systemen: the few proofs no building slice made, for the FTB quests and the titles. All per player; one look every
 * {@link #STAP} ticks per player, and the dearer parts only in the three new biomes:
 * <ul>
 *   <li>{@code biosystemen_kompas_biome}: the player holds a Superkompas that looks for a biome (the tab "Biomes" was used);</li>
 *   <li>{@code biosystemen_kompas_gevonden}: and stands in that biome with it;</li>
 *   <li>{@code biosystemen_kikker_op_blad}: a kikkerguh sits on a leaf within {@link #KIKKER} blocks;</li>
 *   <li>{@code biosystemen_hoogste_eiland}: the player stands on the summit of a stack of cloud islands ({@link #opTop});</li>
 *   <li>{@code biosystemen_reus_drie}: the giant sneezed the player out of his castle {@link #NIEZEN} times ({@link #nies},
 *       called from ReuzenguhEntity.blaasWeg).</li>
 * </ul>
 */
public final class Bewijzen {
    public static final int STAP = 40;
    public static final double KIKKER = 8;
    /** A stack counts as a summit when it is a real chain (not one loose island) of at least this many islands... */
    public static final int MIN_EILANDEN = 3;
    /** ...whose top island lies at least this far above the meadow under the stack. */
    public static final int MIN_HOOGTE = 30;
    public static final int NIEZEN = 3;
    /** Per player (GuhQuests.saved): how often the giant blew them out; the summit was reached. */
    public static final String NIES_KEY = "guhs_bio_systemen_niezen", TOP_KEY = "guhs_bio_systemen_top";
    public static final String KOMPAS_BIOME = "biosystemen_kompas_biome", KOMPAS_GEVONDEN = "biosystemen_kompas_gevonden",
            KIKKER_OP_BLAD = "biosystemen_kikker_op_blad", HOOGSTE_EILAND = "biosystemen_hoogste_eiland", REUS_DRIE = "biosystemen_reus_drie";

    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % STAP == 0 && !p.isSpectator()) {
            kijk(p);
        }
    }

    /** The regular look (also called by the tests). */
    public static void kijk(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos hier = p.blockPosition();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = p.getItemInHand(hand);
            if (stack.getItem() instanceof SuperkompasItem) {
                kompas(p, BiomeKompas.gekozen(stack), level.getBiome(hier).unwrapKey().map(k -> k.identifier().toString()).orElse(""));
            }
        }
        if (level.dimension() != ModDimensions.GUHMENSION || !Bio.inNieuw(level, hier)) {
            return;
        }
        if (kikkerOpBlad(p)) {
            GuhAdvancements.grant(p, KIKKER_OP_BLAD);
        }
        if (!GuhQuests.saved(p).getBooleanOr(TOP_KEY, false) && (p.onGround() || p.isInWater()) && Bio.in(level, hier, Bio.WOLKENWEIDE)
                && p.getY() >= WolkTerrein.WEIDE_Y + MIN_HOOGTE - 8
                && opTop(BioModel.van(level.getChunkSource().randomState()), p.getX(), p.getY(), p.getZ())) {
            top(p);
        }
    }

    /** Does a kikkerguh sit on a leaf within {@link #KIKKER} blocks of this player? */
    public static boolean kikkerOpBlad(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(KikkerguhEntity.class, p.getBoundingBox().inflate(KIKKER), k -> k.isAlive() && KikkerBlad.opBlad(k)).isEmpty();
    }

    /** The compass rule, apart for the tests: {@code gekozen} = the biome the compass looks for (or null), {@code hier} = the biome id here. */
    public static void kompas(ServerPlayer p, String gekozen, String hier) {
        if (gekozen == null || gekozen.isEmpty()) {
            return;
        }
        GuhAdvancements.grant(p, KOMPAS_BIOME);
        if (hier.equals(Guhs.MODID + ":" + gekozen)) {
            GuhAdvancements.grant(p, KOMPAS_GEVONDEN);
        }
    }

    /**
     * Is a player with their feet at (x, y, z) standing on the summit of a stack: on the highest island of a real chain of
     * at least {@link #MIN_EILANDEN} islands whose top lies {@link #MIN_HOOGTE} or more above the meadow? Asked of the
     * terrain model, so it holds for the natural islands only (a building's own island, a cloud or a tower a player built
     * is no summit), and it stays true when the island's top was dug away a little.
     */
    public static boolean opTop(BioModel m, double x, double y, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, bx, bz, bx + 1, bz + 1)) {
            WolkTerrein.Eiland top = top(m, s);
            if (top == null) {
                continue;
            }
            int boven = top.boven(bx, bz);
            if (boven != Kaart.GEEN && y >= boven - 1.5 && y <= boven + 3.5) {
                return true;
            }
        }
        return false;
    }

    /** The summit island of this stack, or null when the stack is too small or too low to count. */
    public static WolkTerrein.Eiland top(BioModel m, WolkTerrein.Stapel s) {
        if (s.los || s.eilanden.size() < MIN_EILANDEN) {
            return null;
        }
        WolkTerrein.Eiland top = s.hoogste();
        return top.top - WolkTerrein.grond(m, s.x, s.z) >= MIN_HOOGTE ? top : null;
    }

    /** The player reached a summit (also for the dev command and the tests). */
    public static void top(ServerPlayer p) {
        GuhQuests.saved(p).putBoolean(TOP_KEY, true);
        GuhAdvancements.grant(p, HOOGSTE_EILAND);
    }

    /** The giant blew this player out of his castle once more; returns how often now. */
    public static int nies(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        int n = saved.getIntOr(NIES_KEY, 0) + 1;
        saved.putInt(NIES_KEY, n);
        if (n >= NIEZEN) {
            GuhAdvancements.grant(p, REUS_DRIE);
        }
        return n;
    }

    public static int niezen(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(NIES_KEY, 0);
    }

    private Bewijzen() {
    }
}
