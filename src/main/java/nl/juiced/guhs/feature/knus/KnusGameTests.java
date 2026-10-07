package nl.juiced.guhs.feature.knus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.evenementen.EvenementType;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the shared Knus framework of 2.8 (package feature.knus) and the scaffolding phase 1 made for every
 * 2.8 feature: seasons and parts of the day, the Knus progress (counters, milestones, claims, collections, the payloads),
 * the Knusfeest hooks, the guh hooks and flags, the hair slot, registerGame, the plein slots, the tags, the new emotes,
 * NPC kinds and Guhdex pages, the superkompas category, the Highscores rows and the Knusfeest event type.
 */
public class KnusGameTests {
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // --- seasons and parts of the day -----------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, batch = "knus_seizoen_a")
    public static void knusSeizoenWisseltEnGaatNooitTerug(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<Seizoen> heard = new ArrayList<>();
        Seizoen.bijWissel((s, oud, nieuw) -> heard.add(nieuw));
        Seizoen.zet(server, Seizoen.WINTER);   // (2.9: so the loop's first season, lente, is a change too - also when this test runs first, -Pgt)
        heard.clear();
        long nummer = Seizoen.nummer(helper.getLevel());
        for (Seizoen s : Seizoen.values()) {
            Seizoen.zet(server, s);
            helper.assertTrue(Seizoen.huidig(helper.getLevel()) == s, "now it is " + s + ": " + Seizoen.huidig(helper.getLevel()));
            helper.assertTrue(Seizoen.dagInSeizoen(helper.getLevel()) == 0, "the first day of " + s);
            long n = Seizoen.nummer(helper.getLevel());
            helper.assertTrue(n > nummer, "the season number only goes up: " + nummer + " -> " + n);
            nummer = n;
            helper.assertTrue(Seizoen.van(n) == s, "van(nummer) is the season");
        }
        helper.assertTrue(heard.size() >= 4, "the listeners heard every change: " + heard);
        helper.assertTrue(Seizoen.byId("herfst") == Seizoen.HERFST && Seizoen.LENTE.volgende() == Seizoen.ZOMER
                && Seizoen.WINTER.volgende() == Seizoen.LENTE, "ids and order");
        helper.assertTrue(Seizoen.DAGEN == 7, "a season lasts 7 days");
        // the parts of the day
        helper.assertTrue(Dagdeel.van(0) == Dagdeel.OCHTEND && Dagdeel.van(23500) == Dagdeel.OCHTEND && Dagdeel.van(1500) == Dagdeel.DAG
                && Dagdeel.van(6000) == Dagdeel.DUTJE && Dagdeel.van(7999) == Dagdeel.DUTJE && Dagdeel.van(8000) == Dagdeel.DAG
                && Dagdeel.van(11500) == Dagdeel.AVOND && Dagdeel.van(13500) == Dagdeel.NACHT && Dagdeel.van(22999) == Dagdeel.NACHT
                && Dagdeel.van(24000 + 12000) == Dagdeel.AVOND, "the parts of the day");
        // the command
        var root = server.getCommands().getDispatcher().getRoot().getChild("guhs");
        var node = root == null ? null : root.getChild("seizoen");
        helper.assertTrue(node != null && node.getChild("lente") != null && node.getChild("winter") != null, "/guhs seizoen <lente|...|winter>");
        var source = server.createCommandSourceStack();
        helper.assertTrue(node.canUse(source.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.forLevel(net.minecraft.server.permissions.PermissionLevel.byId(0)))) && !node.getChild("lente").canUse(source.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.forLevel(net.minecraft.server.permissions.PermissionLevel.byId(0))))
                && node.getChild("lente").canUse(source.withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.forLevel(net.minecraft.server.permissions.PermissionLevel.byId(2)))), "asking is for everyone, setting for ops");
        helper.succeed();
    }

    // --- the Knus tab -----------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void knusVoortgangTeltClaimtEnOntdekt(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(KnusVoortgang.onderdelen().stream().map(KnusVoortgang.Onderdeel::id).toList().equals(Knus.ONDERDELEN),
                    "the thirteen sections, in order: " + KnusVoortgang.onderdelen());
            KnusVoortgang.Mijlpaal m = KnusVoortgang.mijlpaal("knuffeldal_taakjes");
            helper.assertTrue(m != null && m.doel() == 6, "the milestone of the six feesttaakjes");
            helper.assertTrue(!KnusVoortgang.claim(p, m.id()), "not reached: nothing to claim");
            helper.assertTrue(KnusVoortgang.tel(p, m.teller(), 4) == 4 && !KnusVoortgang.bereikt(p, m), "4 of 6");
            helper.assertTrue(KnusVoortgang.tel(p, m.teller(), 2) == 6 && KnusVoortgang.bereikt(p, m), "6 of 6: reached");
            int before = count(p, ModItems.KAAS_KNABBELS.get());
            helper.assertTrue(KnusVoortgang.claim(p, m.id()) && KnusVoortgang.geclaimd(p, m.id()), "claimed");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == before + 16, "the reward: 16 kaasknabbels");
            helper.assertTrue(!KnusVoortgang.claim(p, m.id()), "only once");
            helper.assertTrue(KnusVoortgang.hoogste(p, "knus.test_record", 10) == 10 && KnusVoortgang.hoogste(p, "knus.test_record", 7) == 10
                    && KnusVoortgang.hoogste(p, "knus.test_record", 12) == 12, "hoogste keeps the best");
            helper.assertTrue(KnusVoortgang.tel(p, "knus.test_teller", -5) == 0, "never below 0");
            // a milestone with a quest advancement
            KnusVoortgang.tel(p, "knuffeldal.cocotje", 1);
            helper.assertTrue(advancement(p, "knuffeldal_cocotje"), "reaching it grants its quest advancement");
            // collections
            helper.assertTrue(KnusVoortgang.ontdek(p, "seizoensplakboek", "lente_kransje"), "a new entry");
            helper.assertTrue(!KnusVoortgang.ontdek(p, "seizoensplakboek", "lente_kransje"), "only once");
            helper.assertTrue(!KnusVoortgang.ontdek(p, "seizoensplakboek", "bestaat_niet"), "unknown entries don't count");
            helper.assertTrue(KnusVoortgang.heeft(p, "seizoensplakboek", "lente_kransje")
                    && KnusVoortgang.ontdekt(p, "seizoensplakboek").size() == 1, "found: " + KnusVoortgang.ontdekt(p, "seizoensplakboek"));
            // survives dying (it's in the player's persisted data)
            CompoundTag saved = nl.juiced.guhs.quest.GuhQuests.saved(p);
            helper.assertTrue(saved.contains(KnusVoortgang.KEY), "saved in the persisted player data");
            // the payloads
            KnusPayloads.KnusData data = new KnusPayloads.KnusData(saved.getCompoundOrEmpty(KnusVoortgang.KEY).copy(), new CompoundTag());
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            KnusPayloads.KnusData.STREAM_CODEC.encode(buf, data);
            helper.assertTrue(KnusPayloads.KnusData.STREAM_CODEC.decode(buf).equals(data), "guhs:knus_data survives the trip");
            buf = new FriendlyByteBuf(Unpooled.buffer());
            KnusPayloads.KnusClaim.STREAM_CODEC.encode(buf, new KnusPayloads.KnusClaim("abc"));
            helper.assertTrue(KnusPayloads.KnusClaim.STREAM_CODEC.decode(buf).mijlpaal().equals("abc"), "guhs:knus_claim");
            buf = new FriendlyByteBuf(Unpooled.buffer());
            KnusPayloads.SeizoenSync.STREAM_CODEC.encode(buf, new KnusPayloads.SeizoenSync(-123456789L));
            helper.assertTrue(KnusPayloads.SeizoenSync.STREAM_CODEC.decode(buf).offset() == -123456789L, "guhs:seizoen_sync");
            // registering in an unknown section is refused
            boolean refused = false;
            try {
                KnusVoortgang.mijlpaal("bestaat_niet", "x", "x.y", 1, () -> ItemStack.EMPTY);
            } catch (IllegalArgumentException e) {
                refused = true;
            }
            helper.assertTrue(refused, "only the sections of the contract");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- the Knusfeest --------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void knusfeestHaakjesVoorDeFeatures(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            List<String> steps = new ArrayList<>();
            Knusfeest.luister((pl, taak, stap) -> {
                if (pl == p) {
                    steps.add(taak.id() + ":" + stap);
                }
            });
            helper.assertTrue(!Knusfeest.open(p, Feesttaak.FEESTTAART), "nothing is open before the Burgemeester asks");
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTTAART) == null && !advancement(p, "knusfeest_feesttaart_gemaakt"),
                    "made before it was asked: doesn't count");
            Knusfeest.nieuweRonde(p, 0, EnumSet.allOf(Feesttaak.class));
            for (Feesttaak t : Feesttaak.values()) {
                helper.assertTrue(Knusfeest.open(p, t) && !Knusfeest.gebracht(p, t), t + " is open");
                helper.assertTrue(t.tag().location().equals(Guhs.id("knus/" + t.id())), "its tag: " + t.tag());
            }
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);        // (idempotent)
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEMAAKT && Knusfeest.open(p, Feesttaak.FEESTTAART),
                    "made: still open until it's brought");
            helper.assertTrue(advancement(p, "knusfeest_feesttaart_gemaakt"), "the quest advancement");
            helper.assertTrue(steps.stream().filter(s -> s.equals("feesttaart:GEMAAKT")).count() == 1, "one GEMAAKT step: " + steps);
            helper.assertTrue(Feesttaak.FEESTTAART.steelbaar() && Feesttaak.THEESERVIES.steelbaar() && Feesttaak.FEESTSLINGERS.steelbaar()
                    && !Feesttaak.FEESTKAPSELS.steelbaar(), "the Kruimel-Mika's steal the cake, the tea set and the garlands");
            helper.assertTrue(Feesttaak.FEESTTAART.pkg.equals("bakkerij") && Feesttaak.STERRENLANTAARNS.pkg.equals("sterrenwacht"), "owners");
            for (Feesttaak t : Feesttaak.values()) {
                Knusfeest.zet(p, t, Knusfeest.Stap.GEBRACHT);
            }
            helper.assertTrue(Knusfeest.alleGebracht(p) && !Knusfeest.open(p, Feesttaak.FEESTTAART), "all brought");
            Knusfeest.rondeGevierd(p);
            helper.assertTrue(Knusfeest.isKlaar(p) && !Knusfeest.rondeBezig(p) && Knusfeest.taken(p).isEmpty(), "the Grote Knusfeest is done");
            Knusfeest.nieuweRonde(p, 7, EnumSet.of(Feesttaak.FEESTBLOEMEN, Feesttaak.THEESERVIES));
            helper.assertTrue(Knusfeest.open(p, Feesttaak.FEESTBLOEMEN) && !Knusfeest.open(p, Feesttaak.FEESTTAART) && Knusfeest.ronde(p) == 7,
                    "a seasonal round reopens some tasks through the same hooks");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- guh hooks, flags, the hair slot --------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void knusGuhHooksVlaggenEnHaar(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        guh.setPersistenceRequired();
        GuhHooks.zet(guh, GuhHooks.GLANZEND | GuhHooks.BLOSJES, true);
        GuhHooks.zet(guh, GuhHooks.BLOSJES, false);
        helper.assertTrue(GuhHooks.heeft(guh, GuhHooks.GLANZEND) && !GuhHooks.heeft(guh, GuhHooks.BLOSJES), "flags");
        GuhHooks.bezig(guh, 40);
        helper.assertTrue(GuhHooks.isBezig(guh), "busy");
        GuhHooks.maakBewoner(guh, new BlockPos(1, 2, 3));
        helper.assertTrue(GuhHooks.isBewoner(guh) && new BlockPos(1, 2, 3).equals(GuhHooks.thuis(guh)), "a resident with a home");
        // the hair slot: saved, synced, not a wardrobe slot, not taken off with the clothes
        // (2.9: OREN is the seventh slot and a wardrobe slot: the wardrobe has six)
        helper.assertTrue(GuhClothes.Slot.HAAR.ordinal() == 5 && !GuhClothes.Slot.kleding().contains(GuhClothes.Slot.HAAR)
                && GuhClothes.Slot.OREN.ordinal() == 6 && GuhClothes.Slot.kleding().contains(GuhClothes.Slot.OREN)
                && GuhClothes.Slot.kleding().size() == 6, "HAAR is the sixth slot, OREN the seventh, the wardrobe keeps six");
        helper.assertTrue(nl.juiced.guhs.menu.GuhWardrobeMenu.index(GuhClothes.Slot.OREN) == 5, "the wardrobe has six clothing slots");
        guh.setHaarkleur(0xFF88CC);
        guh.wear(GuhClothes.WINTER_SCARF);
        CompoundTag tag = new CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(guh, tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(copy, tag);
        helper.assertTrue(copy.getHaarkleur() == 0xFF88CC && GuhHooks.heeft(copy, GuhHooks.GLANZEND) && GuhHooks.isBewoner(copy),
                "hair colour, flags and residents survive saving");
        guh.setHaarkleur(-1);
        helper.assertTrue(guh.getHaarkleur() == -1, "natural again");
        helper.assertTrue(guh.takeOffClothes().size() == 1, "taking the clothes off gives the scarf only");
        // the tick hook runs on the server
        int[] ticks = {0};
        GuhHooks.tick(g -> {
            if (g == guh) {
                ticks[0]++;
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(ticks[0] >= 3, "the tick hook runs: " + ticks[0]));
    }

    // --- registerGame, Binnenkort, NPC kinds and pages ------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void knusSteigersVoorDeAndereFeatures(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(Minigames.playing(p) == null, "not playing anything");
            Minigames.registerGame("knus_test", pl -> pl == p);
            helper.assertTrue("knus_test".equals(Minigames.playing(p)) && Minigames.busyElsewhere(p, Minigames.BAKKERIJ), "a registered game counts");
            Minigames.registerGame("knus_test", pl -> false);
            helper.assertTrue(Minigames.playing(p) == null, "replaced");
            for (GuhNpcEntity.Kind kind : List.of(GuhNpcEntity.Kind.BAKKERGUH, GuhNpcEntity.Kind.JUF_KNUFFEL, GuhNpcEntity.Kind.THEEGUH,
                    GuhNpcEntity.Kind.KAPPERGUH, GuhNpcEntity.Kind.BOERINNEGUH, GuhNpcEntity.Kind.STERRENKIJKERGUH, GuhNpcEntity.Kind.BALLONGUH,
                    GuhNpcEntity.Kind.OPA_GUH, GuhNpcEntity.Kind.BADMEESTERGUH)) {
                helper.assertTrue(Features.role(kind) != null, kind + " has a role (" + Features.role(kind) + ")");
                helper.assertTrue(GuhVariant.ofCharacter(kind) != null && GuhDex.ENTRIES.contains(GuhVariant.ofCharacter(kind)), kind + " has a Guhdex page");
            }
            helper.assertTrue(Features.role(GuhNpcEntity.Kind.BURGEMEESTERGUH) != Binnenkort.ROLE && Features.role(GuhNpcEntity.Kind.COCOTJE) != null,
                    "the Burgemeester and Cocotje are phase 1's own");
            // GuhVariant: PLUISGUH is a real variant (before REISGUH), the pages come after GRILLGUH in the contract's order
            // (2.9: PINGUH sits between PLUISGUH and REISGUH)
            helper.assertTrue(!GuhVariant.PLUISGUH.isCharacter() && GuhVariant.PLUISGUH.ordinal() == GuhVariant.PINGUH.ordinal() - 1
                    && !GuhVariant.PINGUH.isCharacter() && GuhVariant.PINGUH.ordinal() == GuhVariant.REISGUH.ordinal() - 1,
                    "PLUISGUH, PINGUH just before REISGUH");
            helper.assertTrue(GuhVariant.PLUISGUH.shows("pluis_kuif") && !GuhVariant.NORMAL.shows("pluis_kuif"), "its fluffy bones");
            List<GuhVariant> pages = List.of(GuhVariant.BURGEMEESTERGUH, GuhVariant.KRUIMEL_MIKA, GuhVariant.BAKKERGUH, GuhVariant.JUF_KNUFFEL,
                    GuhVariant.THEEGUH, GuhVariant.KAPPERGUH, GuhVariant.BOERINNEGUH, GuhVariant.GUHSCHAAPJE, GuhVariant.KNABBELKIPPETJE,
                    GuhVariant.GUHKOE, GuhVariant.STERRENKIJKERGUH, GuhVariant.BALLONGUH, GuhVariant.OPA_GUH, GuhVariant.BADMEESTERGUH,
                    GuhVariant.IJSCOGUH, GuhVariant.COCOTJE);
            for (int i = 0; i < pages.size(); i++) {
                helper.assertTrue(pages.get(i).ordinal() == GuhVariant.GRILLGUH.ordinal() + 1 + i && pages.get(i).isCharacter(), "page order " + pages.get(i));
                helper.assertTrue(GuhDex.ENTRIES.contains(pages.get(i)), "in the Guhdex: " + pages.get(i));
            }
            helper.assertTrue(GuhVariant.KRUIMEL_MIKA.npcKind() == null && GuhVariant.GUHKOE.npcKind() == null, "creature pages have no NPC kind");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- plein slots, tags, superkompas, highscores, the event type -----------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void knusPleinSlotsTagsEnMenus(GameTestHelper helper) {
        var level = helper.getLevel();
        var templates = level.getStructureManager();
        for (PleinSlot slot : PleinSlot.values()) {
            helper.assertTrue(slot.template().equals(Guhs.id("knuffeldal_stadje/" + slot.id())) && slot.pool().equals(slot.template()), "ids of " + slot);
            var t = templates.get(slot.template());
            helper.assertTrue(t.isPresent(), "a (placeholder) template for " + slot);
            var size = t.get().getSize();
            if (slot == PleinSlot.GRIJPMACHINE) {
                helper.assertTrue(size.getX() <= 5 && size.getY() <= 6 && size.getZ() <= 5, "the grijpmachine piece fits under the arcade: " + size);
            } else {
                helper.assertTrue(size.getX() == 31 && size.getZ() == 31 && size.getY() >= 16 && size.getY() <= 48, "31 x H x 31: " + slot + " " + size);
                var jigsaws = t.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                        Blocks.JIGSAW, true);
                helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().equals(new BlockPos(15, 4, 30))
                        && jigsaws.get(0).nbt().getStringOr("name", "").equals("guhs:plein_ingang"), "one jigsaw guhs:plein_ingang at (15, 4, 30): " + slot);
            }
            Identifier pool = slot.pool();
            helper.assertTrue(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL).containsKey(pool),
                    "a pool for " + slot);
        }
        // the test areas
        BlockPos a = helper.absolutePos(new BlockPos(0, 1, 0));
        PleinSlot.testStadje(new BoundingBox(a.getX(), a.getY() - 1, a.getZ(), a.getX() + 4, a.getY() + 3, a.getZ() + 4));
        PleinSlot.testStuk(PleinSlot.KAPPER, new BoundingBox(a.getX(), a.getY(), a.getZ(), a.getX() + 1, a.getY() + 1, a.getZ() + 1));
        helper.assertTrue(PleinSlot.inStadje(level, a.offset(2, 0, 2)) && PleinSlot.bij(level, a) == PleinSlot.KAPPER
                && PleinSlot.stuk(level, a.offset(3, 0, 3), PleinSlot.KAPPER) != null, "test areas count as a town and a slot");
        PleinSlot.testWissen();
        helper.assertTrue(!PleinSlot.inStadje(level, a.offset(2, 0, 2)), "and forget them again");
        // the tags
        helper.assertTrue(new ItemStack(ModItems.KAAS_KNABBELS.get()).is(KnusTags.LEKKERNIJ), "kaasknabbels are a treat");
        helper.assertTrue(new ItemStack(ModItems.KERMISBON.get()).is(KnusTags.GRIJPTICKETS), "a kermisbon is a grijpticket");
        helper.assertTrue(KnusTags.FEESTTAART.equals(Feesttaak.FEESTTAART.tag()) && KnusTags.STERRENLANTAARNS.equals(Feesttaak.STERRENLANTAARNS.tag()),
                "the Knusfeest tags");
        for (var tag : List.of(KnusTags.KAASMELK, KnusTags.OOGST, KnusTags.GEBAK, KnusTags.THEE, KnusTags.MARSHMALLOW, KnusTags.PLUISWOL)) {
            helper.assertTrue(BuiltInRegistries.ITEM.get(tag).isPresent(), "the tag exists: " + tag.location());
        }
        // the superkompas: category knus with the six cosy places (plus the Reisbureau of guhpixel), all real structures
        var knus = SuperkompasItem.CATEGORIES.stream().filter(c -> c.id().equals("knus")).findFirst().orElse(null);
        // (biomes3: later updates append their cosy places with SuperkompasItem.voegToe, after these seven)
        helper.assertTrue(knus != null && knus.structures().size() >= 7 && knus.structures().subList(0, 7).equals(List.of("knuffeldal_stadje",
                "guhboerderij", "guh_sterrenwacht", "ballonfestival", "kampeerplekje", "knuffelbad", "reisbureau")), "the knus category: " + knus);
        var structures = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (String s : knus.structures()) {
            helper.assertTrue(structures.containsKey(Guhs.id(s)), "structure " + s);
        }
        // the Highscores rows of 2.8
        for (String id : List.of("bakkerij", "creche", "kapper", "glijbaan_roze_trechter", "glijbaan_glimtunnel", "glijbaan_grote_plons")) {
            var g = Highscores.game(id);
            helper.assertTrue(g != null && g.board().equals(id) && !g.lowerIsBetter() && g.format().apply(12).equals("12 pt"), "the row " + id);
        }
        // the seasonal Knusfeest is never picked at random
        var random = net.minecraft.util.RandomSource.create(5);
        for (int i = 0; i < 500; i++) {
            helper.assertTrue(EvenementType.choose(random, i % 2 == 0, null) != EvenementType.KNUSFEEST, "never the Knusfeest at random");
        }
        helper.assertTrue(!EvenementType.KNUSFEEST.possible(true) && !EvenementType.RANDOM.contains(EvenementType.KNUSFEEST), "not possible, not random");
        var ev = level.getServer().getCommands().getDispatcher().getRoot().getChild("guhs").getChild("evenement");
        helper.assertTrue(ev != null && ev.getChild("knusfeest") != null, "/guhs evenement knusfeest");
        helper.succeed();
    }

    // --- the new emotes; singing is a signal -------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 120)
    public static void knusNieuweEmotesZingenIsEenSignaal(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.assertTrue(Emote.byId("gapen") == Emote.GAPEN && Emote.byId("zingen") == Emote.ZINGEN && Emote.byId("knuffelen") == Emote.KNUFFELEN
                && Emote.values().length == 16 && Emote.KNUFFELEN.ordinal() == 9 && Emote.VERDRIETJE.ordinal() == 13 && Emote.UKELELE.ordinal() == 14
                && Emote.AAIEN.ordinal() == 15,
                "sixteen emotes (2.10: four more, 3.0: the ukelele, 1.2.0: petting), the new ones last");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        guh.setPersistenceRequired();
        int[] sung = {0};
        KnusSignalen.opZang((level, pos, radius) -> {
            if (level == helper.getLevel() && pos.closerThan(guh.blockPosition(), 2)) {
                sung[0]++;
            }
        });
        helper.runAfterDelay(5, () -> helper.assertTrue(guh.emotes.start(Emote.ZINGEN, true, GuhEmotes.Source.OWNER), "it sings"));
        helper.succeedWhen(() -> helper.assertTrue(sung[0] >= 2, "singing tells the tuintjes: " + sung[0]));
    }
}
