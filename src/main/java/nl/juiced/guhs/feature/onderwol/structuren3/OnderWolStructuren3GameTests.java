package nl.juiced.guhs.feature.onderwol.structuren3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.onderwol.bouw.GrotBouwStructure;
import nl.juiced.guhs.feature.toveren.Spreuken;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the underground buildings part 3 (feature/onderwol/structuren3, tools/features/onderwol_structuren3.py):
 * the structures and their rarity, the templates' anchors, the loot (items, the super vads template, the Spreukjesboekjes,
 * the party clothes) and the texts. Template {@code onderwol_bouw_test} (part 1's field of air).
 */
public class OnderWolStructuren3GameTests {
    private static final String VELD = "onderwol_bouw_test";
    private static final String BATCH = "onderwol_structuren3";
    private static final List<String> ZELDZAAM = List.of("oude_guhtempel", "oude_bankkluis");
    private static final List<String> LOOT = List.of("grotdisco", "oude_wolspinnerij", "sterrengrot", "knuffelkuil", "verrassingsfeestje",
            "oude_guhtempel", "oude_guhtempel_geheim", "oude_bankkluis", "oude_bankkluis_hal");

    private static JsonObject json(String pad) {
        try (InputStream in = OnderWolStructuren3GameTests.class.getResourceAsStream(pad)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every structure is a guhs:grot_bouw above the saus sea, with a reach and biomes; rare ones are spread wider (chapter 2.4). */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren3Bestaan(GameTestHelper helper) {
        var structuren = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String naam : OnderWolStructuren3.STRUCTUREN) {
            Optional<Structure> s = structuren.getOptional(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam)));
            helper.assertTrue(s.isPresent() && s.get() instanceof GrotBouwStructure, naam + ": a guhs:grot_bouw");
            GrotBouwStructure g = (GrotBouwStructure) s.get();
            helper.assertTrue(g.bandOnder() > GrotBouwStructure.SAUS_TOT && g.maxY() >= g.bandOnder(), naam + ": its band lies above the saus sea");
            helper.assertTrue(g.keepClear() > 0 && g.biomes().size() > 5, naam + ": it claims its room and has biomes");
            Optional<StructureSet> set = sets.getOptional(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(naam)));
            helper.assertTrue(set.isPresent() && set.get().placement() instanceof RandomSpreadStructurePlacement, naam + ": a random spread set");
            int spacing = ((RandomSpreadStructurePlacement) set.get().placement()).spacing();
            if (ZELDZAAM.contains(naam)) {
                helper.assertTrue(spacing >= 64, naam + " is rare: spacing " + spacing);
            } else {
                helper.assertTrue(spacing >= 30 && spacing <= 48, naam + " comes now and then: spacing " + spacing);
            }
        }
        helper.succeed();
    }

    /** Every start template has its anchor and the shaft's marker; the Wolspinnerij is also a side building of the Mijnguhdorp. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren3Ankers(GameTestHelper helper) {
        var pools = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var manager = helper.getLevel().getStructureManager();
        for (String naam : OnderWolStructuren3.STRUCTUREN) {
            StructureTemplatePool pool = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/" + naam + "/start"))).orElse(null);
            helper.assertTrue(pool != null, naam + ": its start pool");
            for (StructurePoolElement e : pool.getShuffledTemplates(RandomSource.create(1))) {
                var jigsaws = e.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1));
                helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(Guhs.id(naam + "_midden"))), naam + ": the anchor");
                helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(GrotBouwStructure.INGANG)), naam + ": the shaft's marker");
            }
        }
        StructureTemplatePool zij = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/mijnguhdorp/zij"))).orElseThrow();
        StructurePoolElement spin = zij.getShuffledTemplates(RandomSource.create(1)).stream()
                .filter(e -> e instanceof SinglePoolElement && e.toString().contains("oude_wolspinnerij")).findFirst().orElse(null);
        helper.assertTrue(spin != null, "the Wolspinnerij is in the Mijnguhdorp's side pool");
        helper.assertTrue(spin.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1)).stream()
                .anyMatch(j -> j.name().equals(Guhs.id("mijnguhdorp_zij_in"))), "with the side tunnel's connector");
        helper.succeed();
    }

    /** The loot tables load, every item they name exists, the Spreukjesboekje pools use the guh spell tags. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren3LootBestaat(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        helper.assertTrue(enchantments.get(Spreuken.BOEKJE).isPresent() && enchantments.get(Spreuken.ALLE).isPresent(), "the spell tags exist");
        for (String naam : LOOT) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/onderwol/" + naam));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, naam + ": its loot table loads");
            JsonObject tabel = json("/data/guhs/loot_table/chests/onderwol/" + naam + ".json");
            helper.assertTrue(tabel != null, naam + ": its json");
            for (JsonElement p : tabel.getAsJsonArray("pools")) {
                for (JsonElement e : p.getAsJsonObject().getAsJsonArray("entries")) {
                    JsonObject entry = e.getAsJsonObject();
                    String type = entry.get("type").getAsString();
                    if (type.equals("minecraft:item")) {
                        String item = entry.get("name").getAsString();
                        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(item)), naam + ": the item " + item + " exists");
                    } else if (type.equals("minecraft:loot_table")) {
                        String sub = entry.get("value").getAsString();
                        helper.assertTrue(server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(sub)))
                                != LootTable.EMPTY, naam + ": the table " + sub + " exists");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static int tel(GameTestHelper helper, String naam, int rollen, java.util.function.Predicate<ItemStack> wat) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/onderwol/" + naam)));
        int n = 0;
        for (int i = 0; i < rollen; i++) {
            LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (wat.test(stack)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean boekje(ItemStack s) {
        return s.is(Items.ENCHANTED_BOOK) && Guhs.id("spreukjesboekje").equals(s.get(DataComponents.ITEM_MODEL))
                && s.get(DataComponents.STORED_ENCHANTMENTS) != null && !s.get(DataComponents.STORED_ENCHANTMENTS).isEmpty();
    }

    /** The temple and the vault give the super vads template now and then; the temple, the vault and the party give Spreukjesboekjes. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void onderwolStructuren3LootExtras(GameTestHelper helper) {
        Item sjabloon = BuiltInRegistries.ITEM.getValue(Guhs.id("super_vads_sjabloon"));
        for (String naam : List.of("oude_guhtempel", "oude_guhtempel_geheim", "oude_bankkluis")) {
            int n = tel(helper, naam, 200, s -> s.is(sjabloon));
            helper.assertTrue(n > 10 && n < 200 * 2, naam + ": the super vads template now and then: " + n);
        }
        for (String naam : List.of("oude_guhtempel", "oude_guhtempel_geheim", "oude_bankkluis", "verrassingsfeestje")) {
            helper.assertTrue(tel(helper, naam, 200, OnderWolStructuren3GameTests::boekje) > 5, naam + ": Spreukjesboekjes");
        }
        helper.assertTrue(tel(helper, "grotdisco", 100, s -> s.is(sjabloon) || boekje(s)) == 0, "not in the other buildings");
        helper.succeed();
    }

    /** The party clothes: their items, their textures, their source, and the party's chest gives them. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void onderwolStructuren3Feestkleding(GameTestHelper helper) {
        for (GuhClothes c : OnderWolStructuren3.KLEDING) {
            Item item = ModItems.clothingItem(c);
            helper.assertTrue(item != null && item != Items.AIR, c.id() + ": its item");
            helper.assertTrue(OnderWolStructuren3.KLEDINGBRON.equals(KledingBronnen.bron(c)) && KledingBronnen.prijs(c) != null, c.id() + ": its source");
            helper.assertTrue(OnderWolStructuren3GameTests.class.getResource("/assets/guhs/textures/entity/guh_clothes/" + c.id() + ".png") != null,
                    c.id() + ": its texture");
            helper.assertTrue(tel(helper, "verrassingsfeestje", 200, s -> s.is(item)) > 5, c.id() + " from the party's chest");
        }
        helper.assertTrue(GuhClothes.FEESTHOEDJE.slot == GuhClothes.Slot.HEAD && GuhClothes.SLINGER_SJAALTJE.slot == GuhClothes.Slot.NECK, "the slots");
        helper.succeed();
    }

    /** The talking guhs have their lines and every structure its Superkompas texts (nl_nl and en_us), the clothes their names. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolStructuren3Teksten(GameTestHelper helper) {
        for (String taal : List.of("nl_nl", "en_us")) {
            JsonObject lang = json("/assets/guhs/lang/" + taal + ".json");
            helper.assertTrue(lang != null, taal);
            OnderWolStructuren3.PRAAT.forEach((plek, p) -> {
                for (int i = 1; i <= p.regels(); i++) {
                    helper.assertTrue(lang.has("quest.guhs.onderwol_bouw." + plek + "." + i), taal + ": " + plek + " line " + i);
                }
                helper.assertTrue(!lang.has("quest.guhs.onderwol_bouw." + plek + "." + (p.regels() + 1)), taal + ": " + plek + " has no more lines than Java knows");
            });
            for (String s : OnderWolStructuren3.STRUCTUREN) {
                helper.assertTrue(lang.has("structure.guhs." + s) && lang.has("structure.guhs." + s + ".tooltip"), taal + ": " + s + ": its Superkompas texts");
            }
            for (GuhClothes c : OnderWolStructuren3.KLEDING) {
                helper.assertTrue(lang.has("item.guhs." + c.id()), taal + ": the name of " + c.id());
            }
            helper.assertTrue(lang.has("gui.guhs.kleding.prijs.hint.verrassingsfeestje_onder_de_wol"), taal + ": the source hint");
            helper.assertTrue(lang.has("sign.guhs.onderwol3.oude_bankkluis.geen_mika_s"), taal + ": the signs (one of the vault's)");
        }
        helper.succeed();
    }
}
