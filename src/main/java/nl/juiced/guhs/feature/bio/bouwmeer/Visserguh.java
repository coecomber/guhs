package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * De visser-guh (NPC BOTENHUISJE_VISSERGUH) on the end of the jetty of a botenhuisje: calm, a little philosophical about
 * koi. He greets you by the time of day, hands out a handful of koivoer once a day, lends the roeibootje, and teaches the
 * lake in four small steps ("Het meer leren kennen"):
 * <ol>
 *   <li>feed a koi (he gives the koivoer);</li>
 *   <li>bring him a floating petal;</li>
 *   <li>row to a large island and step ashore;</li>
 *   <li>scoop a koi up in a bucket (he gives the bucket) and let it go again.</li>
 * </ol>
 * Everything is the player's own: the step and the koivoer day live in {@code GuhQuests.saved(player)} (keys
 * {@code guhs_bio_bouw_meer_*}), so any number of players do it side by side, at any botenhuisje. The deeds themselves
 * are seen by {@link BouwMeerEvents} (koivoer near a koi, once a second the island and the bucket) and reported here.
 */
public final class Visserguh implements NpcRole {
    public static final Visserguh ROL = new Visserguh();

    // --- the player's own data -----------------------------------------------------------------------------------------------
    /** The step the player is at ({@link #NIET} .. {@link #KLAAR}). */
    public static final String STAP = "guhs_bio_bouw_meer_stap";
    /** The deed of the step is done: go and tell him. */
    public static final String GEDAAN = "guhs_bio_bouw_meer_gedaan";
    /** Step 4: a koi was in the player's bucket. */
    public static final String GEVANGEN = "guhs_bio_bouw_meer_gevangen";
    /** The day (number + 1) the player last got koivoer; 0 = never. */
    public static final String VOERDAG = "guhs_bio_bouw_meer_voerdag";
    public static final int NIET = 0, VOER = 1, BLAADJE = 2, EILAND = 3, KOI = 4, KLAAR = 5;

    /** Answer ids. */
    public static final int BIJTEN = 1, KOIVOER = 2, BOOTJE = 3, MEER = 4;
    /** How much koivoer a day, and for the first step. */
    public static final int VOER_PER_DAG = 4;
    /** How many greetings per part of the day, and how many thoughts about koi. */
    public static final int GROETEN = 2, GEDACHTEN = 3;
    /** Step 1: koivoer counts when a koi is this close. */
    public static final double KOI_DICHTBIJ = 9;
    /** Step 3: stepping ashore counts this long (ticks) after rowing. */
    public static final int NA_ROEIEN = 1200;
    /** Step 3: a large island is at least this wide somewhere, and water is at most this far in every direction. */
    public static final int EILAND_BREED = 12, EILAND_WATER = 30;
    /** The mooring is looked for this far around him (it stands eleven blocks back along the jetty). */
    private static final int MEERPAAL_ZOEK = 13;

    private record Vaart(long tijd, int waterY) {
    }

    /** (not saved) when and at which water level each player last sat in a roeibootje; the koi-emmers they carried. */
    private static final Map<UUID, Vaart> GEVAREN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EMMERS = new ConcurrentHashMap<>();

    private Visserguh() {
    }

    public static int stap(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(STAP, NIET);
    }

    public static boolean gedaan(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(GEDAAN, false);
    }

    /** (Also for tests and the dev command) puts the player at this step, its deed not done. */
    public static void zetStap(ServerPlayer p, int stap) {
        CompoundTag data = GuhQuests.saved(p);
        data.putInt(STAP, stap);
        data.putBoolean(GEDAAN, false);
        data.putBoolean(GEVANGEN, false);
        EMMERS.remove(p.getUUID());
    }

    static Item koivoer() {
        return Bio.item("koivoer", Items.WHEAT_SEEDS);
    }

    static Item koiEmmer() {
        return Bio.item("koi_emmer", Items.COD_BUCKET);
    }

    static Item blaadje() {
        return Bio.item("drijvende_bloesemblaadjes", Items.PINK_PETALS);
    }

    // --- talking --------------------------------------------------------------------------------------------------------------

    private static Praat.Optie[] opties(ServerPlayer p) {
        int stap = stap(p);
        String meer = stap == NIET ? "gui.guhs.botenhuisje.optie.meer" : stap == KLAAR ? "gui.guhs.botenhuisje.optie.meer_klaar" : "gui.guhs.botenhuisje.optie.meer_bezig";
        return new Praat.Optie[] {new Praat.Optie(BIJTEN, "gui.guhs.botenhuisje.optie.bijten"), new Praat.Optie(KOIVOER, "gui.guhs.botenhuisje.optie.koivoer"),
                new Praat.Optie(BOOTJE, "gui.guhs.botenhuisje.optie.bootje"), new Praat.Optie(MEER, meer)};
    }

    private static void zeg(GuhNpcEntity npc, ServerPlayer p, String key) {
        Praat.open(p, npc, null, key, new Object[0], opties(p));
    }

    /** The greeting for this time of day (two per part of the day, by the day and the player). */
    public static String groet(GuhNpcEntity npc, ServerPlayer p) {
        BlockPos pos = npc.blockPosition();
        int n = (int) Math.floorMod(Klok.dag(npc.level(), pos) + p.getUUID().getLeastSignificantBits(), (long) GROETEN);
        return "gui.guhs.botenhuisje.visser." + Klok.dagdeel(npc.level(), pos).id() + "." + n;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 0.8f);
        GuhAdvancements.grant(p, "botenhuisje_visser");
        String nieuws = voortgang(npc, p);
        zeg(npc, p, nieuws != null ? nieuws : groet(npc, p));
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        switch (optie) {
            case BIJTEN -> zeg(npc, p, "gui.guhs.botenhuisje.visser.koi." + Math.floorMod(p.tickCount / 40 + npc.getId(), GEDACHTEN));
            case KOIVOER -> zeg(npc, p, geefKoivoer(npc, p) ? "gui.guhs.botenhuisje.visser.koivoer" : "gui.guhs.botenhuisje.visser.koivoer_op");
            case BOOTJE -> {
                bootje(npc);
                zeg(npc, p, "gui.guhs.botenhuisje.visser.bootje");
            }
            case MEER -> meer(npc, p);
            default -> {
            }
        }
    }

    /** The mooring beside him lays a boat ready when its berth is empty. */
    private static void bootje(GuhNpcEntity npc) {
        if (npc.level() instanceof ServerLevel level) {
            for (BlockPos pos : BlockPos.betweenClosed(npc.blockPosition().offset(-MEERPAAL_ZOEK, -2, -MEERPAAL_ZOEK), npc.blockPosition().offset(MEERPAAL_ZOEK, 0, MEERPAAL_ZOEK))) {
                if (level.getBlockState(pos).getBlock() instanceof MeerpaalBlock) {
                    MeerpaalBlock.zorg(level, pos.immutable());
                    return;
                }
            }
        }
    }

    /** The day's handful of koivoer; false when this player already had it today. */
    public static boolean geefKoivoer(GuhNpcEntity npc, ServerPlayer p) {
        long dag = Klok.dag(npc.level(), npc.blockPosition()) + 1;
        CompoundTag data = GuhQuests.saved(p);
        if (data.getLongOr(VOERDAG, 0L) == dag) {
            return false;
        }
        data.putLong(VOERDAG, dag);
        Minigames.give(p, new ItemStack(koivoer(), VOER_PER_DAG));
        npc.level().playSound(null, npc, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6f, 1.2f);
        GuhAdvancements.grant(p, "botenhuisje_koivoer");
        return true;
    }

    /** The option about the lake: start the lessons, or say again what the step is. */
    private void meer(GuhNpcEntity npc, ServerPlayer p) {
        String nieuws = voortgang(npc, p);
        if (nieuws != null) {
            zeg(npc, p, nieuws);
            return;
        }
        switch (stap(p)) {
            case NIET -> {
                zetStap(p, VOER);
                Minigames.give(p, new ItemStack(koivoer(), VOER_PER_DAG));
                zeg(npc, p, "gui.guhs.botenhuisje.les.begin");
                GuhQuests.hint(p, "quest.guhs.botenhuisje.voer");
            }
            case VOER -> zeg(npc, p, "gui.guhs.botenhuisje.les.voer_nog");
            case BLAADJE -> zeg(npc, p, "gui.guhs.botenhuisje.les.blaadje_nog");
            case EILAND -> zeg(npc, p, "gui.guhs.botenhuisje.les.eiland_nog");
            case KOI -> zeg(npc, p, GuhQuests.saved(p).getBooleanOr(GEVANGEN, false) ? "gui.guhs.botenhuisje.les.koi_loslaten" : "gui.guhs.botenhuisje.les.koi_nog");
            default -> zeg(npc, p, "gui.guhs.botenhuisje.les.na");
        }
    }

    /**
     * Moves the player on when the deed of their step is done (or the petal is in their pockets); returns what he says
     * about it, or null when there is no news.
     */
    public static String voortgang(GuhNpcEntity npc, ServerPlayer p) {
        int stap = stap(p);
        boolean gedaan = gedaan(p);
        if (stap == VOER && gedaan) {
            zetStap(p, BLAADJE);
            GuhQuests.hint(p, "quest.guhs.botenhuisje.blaadje");
            return "gui.guhs.botenhuisje.les.voer_klaar";
        }
        if (stap == BLAADJE && GuhQuests.count(p, blaadje()) > 0) {
            GuhQuests.take(p, blaadje(), 1);
            zetStap(p, EILAND);
            GuhQuests.hint(p, "quest.guhs.botenhuisje.eiland");
            return "gui.guhs.botenhuisje.les.blaadje_klaar";
        }
        if (stap == EILAND && gedaan) {
            zetStap(p, KOI);
            Minigames.give(p, new ItemStack(Items.BUCKET));
            GuhQuests.hint(p, "quest.guhs.botenhuisje.koi");
            return "gui.guhs.botenhuisje.les.eiland_klaar";
        }
        if (stap == KOI && gedaan) {
            zetStap(p, KLAAR);
            beloon(npc, p);
            return "gui.guhs.botenhuisje.les.klaar";
        }
        return null;
    }

    /** The lessons are done: a few things for a place of your own by the water. */
    private static void beloon(GuhNpcEntity npc, ServerPlayer p) {
        Minigames.give(p, new ItemStack(BouwMeerSlice.STEIGERLANTAARN_ITEM.get(), 2));
        Minigames.give(p, new ItemStack(BouwMeerSlice.HENGELSTANDAARD_ITEM.get()));
        Minigames.give(p, new ItemStack(BouwMeerSlice.KOIWINDZAK_ITEM.get()));
        GuhAdvancements.grant(p, "botenhuisje_visser_klaar");
        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.3f);
        p.sendSystemMessage(Component.translatable("gui.guhs.botenhuisje.les.klaar.chat").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- the deeds (BouwMeerEvents and the roeibootje call these) -------------------------------------------------------------

    private static void klaar(ServerPlayer p, String hint) {
        GuhQuests.saved(p).putBoolean(GEDAAN, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        GuhQuests.hint(p, hint);
    }

    /** Is a koi near this spot? */
    public static boolean koiBij(Level level, BlockPos pos, double straal) {
        EntityType<?> koi = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("koi")).orElse(null);
        return koi != null && !level.getEntities((Entity) null, new AABB(pos).inflate(straal), e -> e.getType() == koi && e.isAlive()).isEmpty();
    }

    /** Step 1: the player used koivoer (on the water or on a koi) with a koi near. */
    public static boolean gevoerd(ServerPlayer p) {
        if (stap(p) != VOER || gedaan(p) || !koiBij(p.level(), p.blockPosition(), KOI_DICHTBIJ)) {
            return false;
        }
        klaar(p, "quest.guhs.botenhuisje.terug");
        return true;
    }

    /** The player sits in a roeibootje (remembered for step 3). */
    public static void inBootje(ServerPlayer p) {
        if (p.getVehicle() instanceof RoeibootjeEntity boot) {
            int waterY = boot.thuis() != null ? boot.thuis().getY() : BlockPos.containing(boot.getX(), boot.getY() - 0.2, boot.getZ()).getY();
            GEVAREN.put(p.getUUID(), new Vaart(p.level().getGameTime(), waterY));
        }
    }

    /** Once a second for every player: the island of step 3 and the bucket of step 4. */
    public static void tik(ServerPlayer p) {
        if (p.getVehicle() instanceof RoeibootjeEntity) {
            inBootje(p);
        }
        int stap = stap(p);
        if (stap != EILAND && stap != KOI || gedaan(p)) {
            return;
        }
        if (stap == EILAND) {
            Vaart v = GEVAREN.get(p.getUUID());
            if (v != null && p.level().getGameTime() - v.tijd() <= NA_ROEIEN && p.getVehicle() == null && p.onGround() && !p.isInWater()
                    && grootEiland(p.level(), p.blockPosition(), v.waterY(), EILAND_WATER)) {
                klaar(p, "quest.guhs.botenhuisje.terug");
            }
            return;
        }
        int emmers = GuhQuests.count(p, koiEmmer()) + (p.getOffhandItem().is(koiEmmer()) ? 1 : 0);
        Integer eerder = EMMERS.put(p.getUUID(), emmers);
        CompoundTag data = GuhQuests.saved(p);
        if (emmers > 0 && !data.getBooleanOr(GEVANGEN, false)) {
            data.putBoolean(GEVANGEN, true);
            GuhQuests.hint(p, "quest.guhs.botenhuisje.loslaten");
        } else if (data.getBooleanOr(GEVANGEN, false) && eerder != null && emmers < eerder && versVrij(p)) {
            klaar(p, "quest.guhs.botenhuisje.terug");
        }
    }

    /** A koi that came out of a bucket a moment ago swims near the player. */
    private static boolean versVrij(ServerPlayer p) {
        EntityType<?> koi = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("koi")).orElse(null);
        return koi != null && !p.level().getEntities((Entity) null, p.getBoundingBox().inflate(8), e -> e.getType() == koi && e.isAlive() && e.tickCount < 100).isEmpty();
    }

    /**
     * Does this spot lie on a large island of a lake whose top water block is at waterY? Land all round the spot, water
     * within {@code bereik} blocks in all eight directions, and somewhere at least {@link #EILAND_BREED} wide. (The
     * mainland fails: inland there is no water. A stepping stone or accent island fails: too small.)
     */
    public static boolean grootEiland(Level level, BlockPos plek, int waterY, int bereik) {
        if (water(level, plek.getX(), waterY, plek.getZ())) {
            return false;
        }
        int[] ver = new int[8];
        int[][] richting = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        for (int i = 0; i < 8; i++) {
            int a = 1;
            while (a <= bereik && !water(level, plek.getX() + richting[i][0] * a, waterY, plek.getZ() + richting[i][1] * a)) {
                a++;
            }
            if (a > bereik) {
                return false;
            }
            ver[i] = a;
        }
        int breed = 0;
        for (int i = 0; i < 4; i++) {
            breed = Math.max(breed, ver[i] + ver[i + 4] - 1);
        }
        return breed >= EILAND_BREED;
    }

    private static boolean water(Level level, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return level.isLoaded(p) && level.getFluidState(p).is(FluidTags.WATER);
    }

    /** (Tests) forget what is remembered outside the player's saved data. */
    public static void vergeet(ServerPlayer p) {
        GEVAREN.remove(p.getUUID());
        EMMERS.remove(p.getUUID());
    }

    /** (Tests) as if the player rowed on a lake with this water level just now. */
    public static void doeAlsofGevaren(ServerPlayer p, int waterY) {
        GEVAREN.put(p.getUUID(), new Vaart(p.level().getGameTime(), waterY));
    }
}
