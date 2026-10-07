package nl.juiced.guhs.feature.bio.kompas;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.feature.bio.BiomeLijst;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3: the Superkompas pointing at a BIOME. The choice is kept in the compass next to the structure choice of the
 * other tabs (custom data "Biome": a guhs biome id without namespace; choosing one clears the other), and
 * {@code GuhCompassItem.inventoryTick} hands the compass to {@link #tick} when a biome is chosen.
 * <p>
 * The search ({@link BiomeZoeker}) can take a while when the biome is far away or not there at all, so it runs on one
 * background thread; the compass says "zoeken..." in the meantime, and the result is picked up on the server thread the
 * next time the compass ticks (once a second). It is looked up again only after the holder walked a good way: an eighth
 * of the distance to the spot found (at least {@link #OPNIEUW_MIN} blocks), or {@link #OPNIEUW_MAX} blocks when nothing
 * was found.
 */
public final class BiomeKompas {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Custom data of the compass: the chosen biome, and after how many blocks walked it looks again. */
    public static final String BIOME = "Biome", OPNIEUW = "BiomeOpnieuw";
    /** How far the compass looks (blocks around the holder), and its grid. */
    public static final int STRAAL = 8192, STAP = 64;
    public static final int OPNIEUW_MIN = 32, OPNIEUW_MAX = 512;

    /** A running or finished search of one player for one biome. */
    private static final class Taak implements Runnable {
        final String biome, dim;
        final BlockPos van;
        final BiomeZoeker zoeker;
        final long gemaakt = System.nanoTime();
        volatile boolean klaar, gestopt;

        Taak(String biome, String dim, BlockPos van, BiomeZoeker zoeker) {
            this.biome = biome;
            this.dim = dim;
            this.van = van;
            this.zoeker = zoeker;
        }

        @Override
        public void run() {
            try {
                while (!gestopt && !zoeker.stap(4096)) {
                    // on
                }
            } catch (RuntimeException e) {
                LOGGER.error("biomes3: the biome search for {} failed", biome, e);   // (counts as "nothing found")
            } finally {
                klaar = true;
            }
        }
    }

    private static final Map<String, Taak> TAKEN = new ConcurrentHashMap<>();
    private static final ExecutorService DRAAD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Guhs biomekompas");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /** The biome this compass looks for (guhs id without namespace), or null. */
    @Nullable
    public static String gekozen(ItemStack stack) {
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(BIOME, "");
        return id.isEmpty() ? null : id;
    }

    /** Makes this compass look for a biome (and forget the structure it looked for). */
    public static void kies(ItemStack stack, String biome) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(BIOME, biome);
            tag.remove("Structure");
            tag.remove("SearchedAt");
            tag.remove("Target");
            tag.remove(OPNIEUW);
        });
        stack.remove(DataComponents.LODESTONE_TRACKER);
    }

    /** (From SuperkompasItem.choose) a structure was chosen: the biome choice goes. */
    public static void wis(CompoundTag tag) {
        tag.remove(BIOME);
        tag.remove(OPNIEUW);
    }

    /** (From SuperkompasItem.getName) "Superkompas: &lt;biome&gt;" while a biome is chosen, else null. */
    @Nullable
    public static Component naam(ItemStack stack) {
        String biome = gekozen(stack);
        return biome == null ? null : Component.translatable("item.guhs.guhmensie_superkompas.named", biomeNaam(biome));
    }

    public static Component biomeNaam(String biome) {
        return Component.translatable("biome.guhs." + biome);
    }

    public static Component sectieNaam(String sectie) {
        return Component.translatable("gui.guhs.superkompas.sectie." + sectie);
    }

    /**
     * (Server) the player chose a biome in the menu. Refused when the biome is not listed, its section is locked for this
     * player (the teaser always is), or the hand does not hold a Superkompas.
     */
    public static boolean kiesVoor(ServerPlayer player, InteractionHand hand, String biome) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SuperkompasItem) || !BiomesMenu.magKiezen(player, biome)) {
            return false;
        }
        kies(stack, biome);
        player.sendOverlayMessage(Component.translatable("item.guhs.guhmensie_superkompas.chosen", biomeNaam(biome)));
        return true;
    }

    /**
     * (From GuhCompassItem.inventoryTick, once a second) true when this compass looks for a biome: then it is handled
     * here and the structure search is skipped.
     */
    public static boolean tick(ItemStack stack, ServerLevel level, Entity entity) {
        String biome = gekozen(stack);
        if (biome == null) {
            return false;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        boolean vast = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        BiomeLijst.Sectie hier = BiomeLijst.van(level.dimension());
        if (hier == null || !BiomeLijst.heeft(hier.id(), biome)) {
            elders(stack, player, biome, vast);
            return true;
        }
        ResourceKey<Biome> key = BiomeLijst.sleutel(biome);
        volg(stack, player, level.dimension(), biome, vast, () -> BiomeZoeker.voor(level, key, player.blockPosition(), STRAAL, STAP),
                () -> level.getBiome(player.blockPosition()).is(key), false);
        return true;
    }

    /** The chosen biome is not of the dimension the holder is in: the needle spins, and the compass says where it is. */
    static void elders(ItemStack stack, ServerPlayer player, String biome, boolean vast) {
        stop(player, biome);
        if (stack.has(DataComponents.LODESTONE_TRACKER)) {
            stack.remove(DataComponents.LODESTONE_TRACKER);
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains("SearchedAt") || data.contains("Target")) {
            data.remove("SearchedAt");
            data.remove("Target");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        if (vast) {
            BiomeLijst.Sectie s = BiomesMenu.sectieVan(biome);
            Component tekst = s == null ? Component.translatable("gui.guhs.biokompas.niets", biomeNaam(biome))
                    : Component.translatable("gui.guhs.biokompas.elders", biomeNaam(biome), sectieNaam(s.id()));
            player.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * The compass follows a biome of the dimension the holder is in (apart from {@link #tick} for the tests): picks up a
     * finished search, starts one when the holder walked far enough from where the last one began, points the needle,
     * and tells the holder how it stands. {@code meteen}: the search runs here and now instead of on the search thread.
     *
     * @return what the compass says: "hier", "afstand", "zoeken" or "niets"
     */
    static String volg(ItemStack stack, ServerPlayer player, ResourceKey<Level> dimensie, String biome, boolean vast, Supplier<BiomeZoeker> maker,
            BooleanSupplier erin, boolean meteen) {
        opruimen();
        String sleutel = player.getUUID() + "|" + biome;
        String dim = dimensie.identifier().toString();
        BlockPos hier = player.blockPosition();
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean veranderd = false;
        Taak taak = TAKEN.get(sleutel);
        if (taak != null && !taak.dim.equals(dim)) {
            taak.gestopt = true;
            TAKEN.remove(sleutel);
            taak = null;
        }
        boolean vers = dim.equals(data.getStringOr("SearchedDim", "")) && data.contains("SearchedAt")
                && BlockPos.of(data.getLongOr("SearchedAt", 0L)).closerThan(hier, Math.max(OPNIEUW_MIN, data.getIntOr(OPNIEUW, OPNIEUW_MIN)));
        if (!vers && taak == null) {
            taak = new Taak(biome, dim, hier, maker.get());
            if (meteen) {
                taak.run();
            } else {
                TAKEN.put(sleutel, taak);
                DRAAD.execute(taak);
            }
        }
        if (taak != null && taak.klaar) {
            TAKEN.remove(sleutel, taak);
            if (!taak.gestopt) {
                BlockPos plek = taak.zoeker.gevonden();
                data.putString("SearchedDim", dim);
                data.putLong("SearchedAt", taak.van.asLong());
                if (plek != null) {
                    data.putLong("Target", plek.asLong());
                    double ver = Math.hypot(plek.getX() - taak.van.getX(), plek.getZ() - taak.van.getZ());
                    data.putInt(OPNIEUW, Mth.clamp((int) (ver / 8), OPNIEUW_MIN, OPNIEUW_MAX));
                    LodestoneTracker tracker = new LodestoneTracker(Optional.of(GlobalPos.of(dimensie, plek)), false);
                    if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
                        stack.set(DataComponents.LODESTONE_TRACKER, tracker);
                    }
                } else {
                    data.remove("Target");
                    data.putInt(OPNIEUW, OPNIEUW_MAX);
                    stack.remove(DataComponents.LODESTONE_TRACKER);
                }
                veranderd = true;
            }
            taak = null;
        }
        if (veranderd) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        String zegt;
        Component tekst;
        if (vast && erin.getAsBoolean()) {
            zegt = "hier";
            tekst = Component.translatable("gui.guhs.biokompas.hier", biomeNaam(biome));
        } else if (data.contains("Target") && dim.equals(data.getStringOr("SearchedDim", ""))) {
            zegt = "afstand";
            BlockPos t = BlockPos.of(data.getLongOr("Target", 0L));
            int blokken = (int) Math.round(Math.hypot(t.getX() - hier.getX(), t.getZ() - hier.getZ()) / 10) * 10;
            tekst = Component.translatable("gui.guhs.biokompas.afstand", biomeNaam(biome), Math.max(blokken, 5));
        } else if (taak != null) {
            zegt = "zoeken";
            tekst = Component.translatable("gui.guhs.biokompas.zoeken", biomeNaam(biome));
        } else {
            zegt = "niets";
            tekst = Component.translatable("gui.guhs.biokompas.niets", biomeNaam(biome));
        }
        if (vast) {
            player.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return zegt;
    }

    /** Stops this player's search for this biome (the result is thrown away). */
    static void stop(ServerPlayer player, String biome) {
        Taak taak = TAKEN.remove(player.getUUID() + "|" + biome);
        if (taak != null) {
            taak.gestopt = true;
        }
    }

    /** Stops every search of this player (logout), or all of them (null: the server stops). */
    static void stopAlles(@Nullable ServerPlayer player) {
        String begin = player == null ? "" : player.getUUID() + "|";
        TAKEN.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(begin)) {
                e.getValue().gestopt = true;
                return true;
            }
            return false;
        });
    }

    /** Searches nobody picked up (the compass was put away while it looked) go after a minute. */
    private static void opruimen() {
        if (TAKEN.isEmpty()) {
            return;
        }
        long nu = System.nanoTime();
        TAKEN.values().removeIf(t -> {
            if (nu - t.gemaakt > 60_000_000_000L) {
                t.gestopt = true;
                return true;
            }
            return false;
        });
    }

    /** (Tests) how many searches run or wait to be picked up. */
    static int taken() {
        return TAKEN.size();
    }

    private BiomeKompas() {
    }
}
