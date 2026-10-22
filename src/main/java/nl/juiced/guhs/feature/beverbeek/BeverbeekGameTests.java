package nl.juiced.guhs.feature.beverbeek;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import org.slf4j.Logger;

/** 1.6.0 deel 2 (F10a): de Beverbeek. Its share of the Guhmension, the brooks, the ranger post and the petting beaver. */
public class BeverbeekGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMPTY = "empty";
    private static final int STAP = 32, N = 500;

    /** Rare (about 1% of the Guhmension surface, like the Bleekwoud) and in real forests, not specks. */
    @GuhTest(template = EMPTY, batch = "beverbeek_aandeel", timeoutTicks = 2400)
    public static void beverbeekAandeel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        MultiNoiseBiomeSource multi = (MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, source).getOrThrow();
        long total = 0, in = 0;
        List<Integer> patches = new ArrayList<>();
        for (long seed : new long[]{1L, 20281201L, -778899L}) {
            Climate.Sampler sampler = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed).sampler();
            boolean[][] is = new boolean[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    is[i][j] = multi.getNoiseBiome(sampler.sample(QuartPos.fromBlock(-8000 + i * STAP), QuartPos.fromBlock(100),
                            QuartPos.fromBlock(-8000 + j * STAP))).is(BeverbeekFeature.BIOME);
                    total++;
                    in += is[i][j] ? 1 : 0;
                }
            }
            patches.addAll(vlekken(is));
        }
        patches.sort(null);
        double cel = STAP * STAP / 10000.0, groot = 0;
        for (int p : patches) {
            groot += p * cel >= 1.0 ? p : 0;
        }
        double deel = in / (double) total, grootDeel = in == 0 ? 0 : groot / in;
        String tekst = String.format(java.util.Locale.ROOT, "%.2f%% in %d patches (biggest %.1f ha, %.0f%% in patches of 1 ha or more)",
                100 * deel, patches.size(), patches.isEmpty() ? 0 : patches.get(patches.size() - 1) * cel, 100 * grootDeel);
        LOGGER.info("Beverbeek share of the Guhmension surface ({} samples): {}", total, tekst);
        helper.assertTrue(deel >= 0.005 && deel <= 0.016, "the Beverbeek is about 1% of the Guhmension: " + tekst);
        helper.assertTrue(grootDeel >= 0.8, "most of it lies in real forests of a hectare or more: " + tekst);
        helper.succeed();
    }

    private static List<Integer> vlekken(boolean[][] is) {
        List<Integer> uit = new ArrayList<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!is[i][j]) {
                    continue;
                }
                int n = 0;
                is[i][j] = false;
                q.add(new int[]{i, j});
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    n++;
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int x = c[0] + d[0], z = c[1] + d[1];
                        if (x >= 0 && z >= 0 && x < N && z < N && is[x][z]) {
                            is[x][z] = false;
                            q.add(new int[]{x, z});
                        }
                    }
                }
                uit.add(n);
            }
        }
        return uit;
    }

    /** The brooks: a few percent of the land, a deep middle, and lines (almost every brook column has a brook neighbour). */
    @GuhTest(template = EMPTY)
    public static void beverbeekBeken(GameTestHelper helper) {
        long seed = 12345L;
        int n = 0, diep = 0, alleen = 0, W = 512;
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < W; z++) {
                int d = Beek.diepte(seed, x, z);
                if (d == 0) {
                    continue;
                }
                n++;
                diep += d == 2 ? 1 : 0;
                if (Beek.diepte(seed, x + 1, z) == 0 && Beek.diepte(seed, x - 1, z) == 0 && Beek.diepte(seed, x, z + 1) == 0 && Beek.diepte(seed, x, z - 1) == 0) {
                    alleen++;
                }
            }
        }
        double deel = n / (double) (W * W);
        String tekst = String.format(java.util.Locale.ROOT, "%.1f%% brook, %d deep, %d lonely", 100 * deel, diep, alleen);
        LOGGER.info("Beverbeek brooks: {}", tekst);
        helper.assertTrue(deel > 0.025 && deel < 0.12, "brooks, not a lake and not nothing: " + tekst);
        helper.assertTrue(diep > n / 6, "with a deeper middle: " + tekst);
        helper.assertTrue(alleen < n / 50, "they are lines, not dots: " + tekst);
        helper.succeed();
    }

    /** The ranger: his role answers, he keeps exactly one beaver around his post, and the beaver may only be petted. */
    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void beverbeekRangerEnAaibever(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.BEVERBEEK_RANGER) != null, "the ranger has a role");
        GuhNpcEntity ranger = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
        ranger.setKind(GuhNpcEntity.Kind.BEVERBEEK_RANGER);
        BlockPos post = helper.absolutePos(new BlockPos(2, 1, 2));
        ranger.snapTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0f, 0f);
        helper.getLevel().addFreshEntity(ranger);
        SafariDier a = BeverbeekRanger.zorgVoorBever(helper.getLevel(), post);
        SafariDier b = BeverbeekRanger.zorgVoorBever(helper.getLevel(), post);
        helper.assertTrue(a != null && a == b, "one beaver, made once");
        helper.assertTrue("bever".equals(a.soort()) && Aaibever.is(a), "a safari beaver with the aaibever tag");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(SafariDier.class, new AABB(post).inflate(20), Aaibever::is).size() == 1, "only one");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.snapTo(a.getX() + 1, a.getY(), a.getZ());
        p.interactOn(a, InteractionHand.MAIN_HAND, a.position());
        helper.assertTrue("blij".equals(a.actie()), "petting makes it happy: " + a.actie());
        helper.assertTrue(!a.isLeashed() && a.isAlive(), "and nothing else happens");
        helper.succeed();
    }

    /** The ranger post template: there, with the ranger on the porch. */
    @GuhTest(template = EMPTY)
    public static void beverbeekRangerpost(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("beverbeek_rangerpost")).orElse(null);
        helper.assertTrue(t != null, "the template loads");
        net.minecraft.nbt.CompoundTag tag = t.save(new net.minecraft.nbt.CompoundTag());
        String entities = tag.getListOrEmpty("entities").toString();
        helper.assertTrue(entities.contains("beverbeek_ranger"), "with the ranger in it");
        helper.succeed();
    }

    // --- 1.6.2 ------------------------------------------------------------------------------------------------------------

    /** The brooks swell and narrow: 2-5 blocks wide (small rivers in the wide stretches). */
    @GuhTest(template = EMPTY)
    public static void beverbeekBekenBreedte(GameTestHelper helper) {
        double min = 99, max = 0;
        for (int x = 0; x < 2048; x += 7) {
            for (int z = 0; z < 2048; z += 7) {
                double b = Beek.breed(12345L, x, z) * 2;
                min = Math.min(min, b);
                max = Math.max(max, b);
            }
        }
        helper.assertTrue(min >= 2.0 && min < 2.8 && max > 4.4 && max <= 5.3, "brooks 2-5 wide: " + min + " .. " + max);
        helper.succeed();
    }

    /** A wild beaver family: comes and goes (never saved, not persistent), pettable, never the ranger's Spaander. */
    @GuhTest(template = EMPTY, timeoutTicks = 60)
    public static void beverbeekWildeBevers(GameTestHelper helper) {
        BlockPos plek = helper.absolutePos(new BlockPos(2, 1, 2));
        int n = WildeBevers.gezin(helper.getLevel(), plek, helper.getLevel().getRandom());
        List<SafariDier> bevers = WildeBevers.in(helper.getLevel(), new AABB(plek).inflate(6));
        helper.assertTrue(n >= 2 && n <= 4 && bevers.size() == n, "a pair with 0-2 little ones: " + n + " / " + bevers.size());
        for (SafariDier b : bevers) {
            helper.assertTrue("bever".equals(b.soort()) && b.isWild() && !b.isPersistenceRequired(), "a wild bever, not kept: " + b);
            helper.assertTrue(nl.juiced.guhs.world.WildeDieren.isKomEnGa(b) && !b.shouldBeSaved(), "never written to disk");
            helper.assertTrue(Aaibever.aaibaar(b) && !Aaibever.is(b), "pettable, but not Spaander");
            b.discard();
        }
        helper.succeed();
    }

    /** The Beverbeek's guhs: some become a Beverguh, some wear the knaagdier hood; a tame guh is left alone. */
    @GuhTest(template = EMPTY, timeoutTicks = 60)
    public static void beverbeekGuhs(GameTestHelper helper) {
        int bever = 0, muts = 0;
        for (int i = 0; i < 120; i++) {
            nl.juiced.guhs.entity.GuhEntity g = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.NATURAL);
            String wat = BeverGuhs.kies(g);
            bever += "beverguh".equals(wat) && g.getVariant() == nl.juiced.guhs.entity.GuhVariant.BEVERGUH ? 1 : 0;
            muts += "knaagdier".equals(wat) && g.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.HEAD) == nl.juiced.guhs.entity.GuhClothes.KNAAGDIER_MUTS ? 1 : 0;
            helper.assertTrue(BeverGuhs.kies(g).isEmpty(), "once per guh");
        }
        helper.assertTrue(bever > 15 && bever < 60 && muts > 10, "Beverguhs " + bever + ", hoods " + muts + " of 120");
        nl.juiced.guhs.entity.GuhEntity tam = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.NATURAL);
        tam.setTame(true, false);
        helper.assertTrue(BeverGuhs.kies(tam).isEmpty() && tam.getVariant() == nl.juiced.guhs.entity.GuhVariant.NORMAL, "a tame guh stays itself");
        helper.assertTrue(nl.juiced.guhs.quest.GuhDex.ENTRIES.contains(nl.juiced.guhs.entity.GuhVariant.BEVERGUH)
                && nl.juiced.guhs.quest.GuhDex.EXTRA.contains(nl.juiced.guhs.entity.GuhVariant.BEVERGUH), "a (bonus) Guhdex page");
        helper.assertTrue(nl.juiced.guhs.feature.kleding.KledingBronnen.bron(nl.juiced.guhs.entity.GuhClothes.KNAAGDIER_MUTS) != null
                && nl.juiced.guhs.feature.kleding.KledingBronnen.bron(nl.juiced.guhs.entity.GuhClothes.KNAAGDIER_STAART) != null, "the outfit has a source");
        for (String r : List.of("/assets/guhs/textures/entity/guh_beverguh.png", "/assets/guhs/textures/entity/guh_slaap/guh_beverguh.png",
                "/assets/guhs/textures/entity/guh_clothes/knaagdier_muts.png", "/assets/guhs/textures/entity/guh_clothes/knaagdier_staart.png",
                "/assets/guhs/textures/item/knaagdier_muts.png", "/assets/guhs/textures/item/knaagdier_staart.png")) {
            helper.assertTrue(BeverbeekGameTests.class.getResource(r) != null, "missing " + r);
        }
        helper.succeed();
    }

    /** Fewer guhs: the biome's own spawner brings 1-2 at a time, at half the old chance. */
    @GuhTest(template = EMPTY)
    public static void beverbeekMinderGuhs(GameTestHelper helper) {
        var biome = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValue(BeverbeekFeature.BIOME);
        helper.assertTrue(biome != null, "the biome");
        var spawns = biome.getMobSettings();
        helper.assertTrue(spawns.getCreatureProbability() <= 0.07f, "creature chance " + spawns.getCreatureProbability());
        for (var e : spawns.getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap()) {
            if (e.value().type() == ModEntities.GUH.get()) {
                helper.assertTrue(e.value().maxCount() <= 2, "1-2 guhs at a time: " + e.value());
            }
        }
        helper.succeed();
    }
}
