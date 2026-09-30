package nl.juiced.guhs.feature.spiesburcht;

import java.io.InputStream;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Spiesburcht: the Rookguh (fed by hand and with thrown knabbels, floats home, counts per player,
 * can't be hurt), the Nether-Mikas (vads keeps them calm, hitting one makes them angry, the trade), the brewing (every
 * step, the drankjes, and working without guhs:moeraskaas and guhs:stil), the Aangebrande Mika (the T calls him,
 * he can't be hurt while he bakes up, he breaks nothing, he drops the gloeister), the Knabbelbaken (players and tamed
 * guhs, more layers more effects), the Asguh, the drops, and the data (structures, tiles, spawns, compass).
 */
public class SpiesburchtGameTests {
    private static final String ROOM = "spiesburcht_testkamer";
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void remove(GameTestHelper helper, Entity e) {
        if (e instanceof ServerPlayer p) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        } else {
            e.discard();
        }
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void normalDifficulty(GameTestHelper helper) {
        if (helper.getLevel().getDifficulty() == Difficulty.PEACEFUL) {
            helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        }
    }

    // --- the Rookguh -------------------------------------------------------------------------------------------------------

    /** Six knabbels by hand: rounder every time, then VAHOEG, it floats home (gone) and the player saved one. */
    @GuhTest(template = ROOM, timeoutTicks = 200)
    public static void rookguhFedByHandFloatsHome(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        RookguhEntity guh = helper.spawn(SpiesburchtFeature.ROOKGUH.get(), new BlockPos(8, 3, 8));
        int before = SpiesburchtStats.rookguhs(player);
        // it can't be hurt: you feed a Rookguh, you don't hit it
        helper.assertFalse(guh.hurt(helper.getLevel().damageSources().playerAttack(player), 10f), "a Rookguh can't be hurt");
        helper.assertTrue(guh.getHealth() == guh.getMaxHealth(), "not a scratch");
        for (int i = 0; i < RookguhEntity.NEEDED; i++) {
            helper.assertFalse(guh.isVahoeg(), "not vahoeg yet after " + i);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
            guh.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(guh.fed() == i + 1, "fed " + (i + 1) + ": " + guh.fed());
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "the knabbel is eaten");
        }
        helper.assertTrue(guh.isVahoeg() && guh.plumpness() >= 1f, "VAHOEG: round and rosy");
        helper.assertTrue(SpiesburchtStats.rookguhs(player) == before + 1, "one more saved Rookguh for the player");
        double startY = guh.getY();
        helper.runAfterDelay(20, () -> helper.assertTrue(guh.isRemoved() || guh.getY() > startY + 0.5, "it floats up"));
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.isRemoved(), "home at last");
            remove(helper, player);
        });
    }

    /**
     * 2.10.1: the Rookguh has its own Guhdex page (a creature page: its id is its entity id, no NPC kind, can't be tamed)
     * with its texts; saving a Rookguh fills in that page (it shows "Rookguhs gered: N", no longer above the whole Guhdex).
     */
    @GuhTest(template = ROOM)
    public static void rookguhHeeftEenGuhdexPagina(GameTestHelper helper) {
        GuhVariant page = GuhVariant.ROOKGUH;
        helper.assertTrue(GuhDex.ENTRIES.contains(page) && page.isCharacter() && page.npcKind() == null && !GuhDex.TAMEABLE.contains(page),
                "a creature page in the Guhdex");
        helper.assertTrue(GuhDex.ENTRIES.indexOf(page) == GuhDex.ENTRIES.indexOf(GuhVariant.ASGUH) + 1, "next to the Asguh (Barbecuether)");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(Guhs.id(page.id())) == SpiesburchtFeature.ROOKGUH.get(), "its id is the Rookguh's entity id");
        helper.assertTrue(GuhVariant.values()[GuhVariant.BALTOGUH.ordinal() - 1] == page, "added at the end of 2.10.1 (saved ordinals stay; 3.0 goes after it)");
        var lang = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/assets/guhs/lang/nl_nl.json");
        for (String key : List.of("entity.guhs.rookguh", "gui.guhs.guhdex.rarity.rookguh", "gui.guhs.guhdex.info.rookguh", "gui.guhs.guhdex.rookguhs")) {
            helper.assertTrue(lang.has(key), "text " + key);
        }
        helper.assertTrue(lang.get("gui.guhs.guhdex.rookguhs").getAsString().startsWith("Rookguhs gered"), "Rookguhs gered: N");
        ServerPlayer player = player(helper);
        var data = nl.juiced.guhs.world.GuhWorldData.get(helper.getLevel().getServer()).player(player.getUUID());
        data.seen.remove(page);
        SpiesburchtStats.rookguhSaved(player);
        helper.assertTrue(data.seen.contains(page), "saving a Rookguh fills in its page");
        remove(helper, player);
        helper.succeed();
    }

    /**
     * 2.10.1 (user decision): the Rookguh page is a bonus page. It doesn't count for "alles verzameld": the all-pages
     * milestones, the full-Guhdex maag upgrade and the seen counter are exactly what they were in 2.10.0.
     */
    @GuhTest(template = ROOM)
    public static void rookguhPaginaTeltNietMeeVoorVoltooiing(GameTestHelper helper) {
        GuhVariant page = GuhVariant.ROOKGUH;
        helper.assertTrue(GuhDex.EXTRA.contains(page) && !GuhDex.TELLEND.contains(page) && GuhDex.TELLEND.size() == GuhDex.ENTRIES.size() - 1,
                "a bonus page, not counted");
        helper.assertTrue(GuhDex.MILESTONES.get(2).seen() == GuhDex.TELLEND.size() && GuhDex.MILESTONES.get(3).seen() == GuhDex.TELLEND.size(),
                "the all-pages milestones ask for the counting pages only");
        java.util.Set<GuhVariant> alles = java.util.EnumSet.noneOf(GuhVariant.class);
        alles.addAll(GuhDex.TELLEND);
        helper.assertTrue(GuhDex.vol(alles) && GuhDex.geteld(alles) == GuhDex.TELLEND.size(), "everything but the Rookguh: a full Guhdex");
        alles.add(page);
        helper.assertTrue(GuhDex.geteld(alles) == GuhDex.TELLEND.size(), "the Rookguh doesn't add to the count");
        alles.remove(GuhVariant.NORMAL);
        helper.assertFalse(GuhDex.vol(alles), "the Rookguh can't stand in for a missing page");
        helper.assertTrue(GuhDex.geteldIds(List.of("normal", "rookguh")) == 1, "the client's counter skips it too");
        // the milestone reward: every counting page seen, never near a Rookguh
        ServerPlayer player = player(helper);
        var data = nl.juiced.guhs.world.GuhWorldData.get(helper.getLevel().getServer()).player(player.getUUID());
        data.seen.clear();
        data.rewards.remove(2);
        data.seen.addAll(GuhDex.TELLEND);
        GuhDex.claim(player, 2);
        helper.assertTrue(data.rewards.contains(2) && nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.GUH_KRISTAL_VERREKIJKER.get()) == 1,
                "all pages without the Rookguh: the milestone's reward");
        remove(helper, player);
        helper.succeed();
    }

    /**
     * 2.8: the Rookguh is a ghast of 3/4 size (hitbox 3x3, the model a 16-unit cube with nine tentacles, drawn 3.375
     * blocks big) with a flat guh face and its cheeks, and the animations its code plays; still peaceful.
     */
    @GuhTest(template = EMPTY)
    public static void rookguhIsASmallGhastWithAGuhFace(GameTestHelper helper) {
        RookguhEntity guh = helper.spawn(SpiesburchtFeature.ROOKGUH.get(), new BlockPos(1, 4, 1));
        helper.assertTrue(Math.abs(guh.getBbWidth() - 3.0f) < 0.01f && Math.abs(guh.getBbHeight() - 3.0f) < 0.01f,
                "3/4 of a ghast: " + guh.getBbWidth() + " x " + guh.getBbHeight());
        helper.assertTrue(guh.getEyeHeight() > 1.8f && guh.getEyeHeight() < 2.6f, "its eyes on its face: " + guh.getEyeHeight());
        helper.assertFalse(guh.hurt(helper.getLevel().damageSources().generic(), 5f), "still can't be hurt");
        var geo = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/assets/guhs/geo/entity/rookguh.geo.json");
        helper.assertTrue(geo != null, "the model");
        var bones = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.bones(geo);
        long tentacles = bones.entrySet().stream().filter(e -> e.getKey().startsWith("tentacle_") && e.getValue().equals("body")).count();
        helper.assertTrue(tentacles == 9, "nine tentacles on the body: " + tentacles);
        helper.assertTrue(bones.containsKey("cheeks") && bones.containsKey("body") && !bones.containsKey("ear_left"), "a ghast (no guh ears), with cheeks");
        var anims = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/assets/guhs/animations/entity/rookguh.animation.json");
        helper.assertTrue(anims != null, "the animations");
        for (String a : new String[]{"float", "eat", "vahoeg"}) {
            helper.assertTrue(anims.getAsJsonObject("animations").has("animation.rookguh." + a), "animation " + a);
        }
        helper.assertTrue(Guhs.class.getResource("/assets/guhs/textures/entity/rookguh.png") != null, "its own texture");
        guh.discard();
        helper.succeed();
    }

    /** Thrown knabbels: it swoops down to eat them, and whoever threw them gets the credit. */
    @GuhTest(template = ROOM, timeoutTicks = 300)
    public static void rookguhEatsThrownKnabbels(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        int before = SpiesburchtStats.rookguhs(player);
        RookguhEntity guh = helper.spawn(SpiesburchtFeature.ROOKGUH.get(), new BlockPos(8, 2, 8));
        ItemEntity knabbels = new ItemEntity(helper.getLevel(), guh.getX() + 1, guh.getY(), guh.getZ(),
                new ItemStack(ModItems.KAAS_KNABBELS.get(), RookguhEntity.NEEDED));
        knabbels.setThrower(player);
        helper.getLevel().addFreshEntity(knabbels);
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.isVahoeg() || guh.isRemoved(), "it ate them all: " + guh.fed());
            helper.assertTrue(SpiesburchtStats.rookguhs(player) == before + 1, "the thrower saved it");
            remove(helper, player);
            if (!guh.isRemoved()) {
                guh.discard();
            }
        });
    }

    // --- the Nether-Mikas -----------------------------------------------------------------------------------------------------

    private static MikaEntity netherMika(GameTestHelper helper, BlockPos at) {
        MikaEntity mika = ModEntities.NETHER_MIKA.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        BlockPos p = helper.absolutePos(at);
        mika.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        helper.getLevel().addFreshEntity(mika);
        return mika;
    }

    /** Without vads they go for you; with a piece of vads armour they leave you alone, until you hit one of them. */
    @GuhTest(template = ROOM)
    public static void netherMikasLeaveVadsWearersAlone(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        MikaEntity mika = netherMika(helper, new BlockPos(8, 1, 8));
        MikaEntity friend = netherMika(helper, new BlockPos(10, 1, 8));
        helper.assertTrue(NetherMikaRuil.isHostileTo(mika, player), "no vads: angry");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.VADS_HELMET.get()));
        helper.assertTrue(NetherMikaRuil.wearsVads(player) && !NetherMikaRuil.isHostileTo(mika, player), "vads helmet: calm");
        mika.setTarget(player);
        helper.assertTrue(mika.getTarget() == null, "it won't even target a vads wearer");
        mika.hurt(helper.getLevel().damageSources().playerAttack(player), 1f);
        helper.assertTrue(NetherMikaRuil.isAngryAt(mika, player) && NetherMikaRuil.isAngryAt(friend, player), "hit one and they're all angry");
        helper.assertTrue(NetherMikaRuil.isHostileTo(friend, player), "vads or no vads");
        helper.assertFalse(NetherMikaRuil.offer(mika, player, new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get())), "and no trading with you");
        remove(helper, player);
        mika.discard();
        friend.discard();
        helper.succeed();
    }

    /** Give a Nether-Mika a vahoege vads ingot: it sniffs it, then throws you something. */
    @GuhTest(template = ROOM, timeoutTicks = NetherMikaRuil.ADMIRE_TICKS + 80)
    public static void netherMikaTradesAnIngot(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VADS_CHESTPLATE.get()));
        MikaEntity mika = netherMika(helper, new BlockPos(6, 1, 6));
        ItemStack ingots = new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get(), 3);
        helper.assertTrue(NetherMikaRuil.offer(mika, player, ingots), "it takes the ingot");
        helper.assertTrue(ingots.getCount() == 2 && NetherMikaRuil.isAdmiring(mika)
                && mika.getItemBySlot(EquipmentSlot.MAINHAND).is(ModItems.VAHOEGE_VADS_INGOT.get()), "and holds it up");
        helper.assertFalse(NetherMikaRuil.offer(mika, player, ingots), "one at a time");
        // the loot itself: barbecue things
        boolean kool = false;
        for (int i = 0; i < 200 && !kool; i++) {
            kool = NetherMikaRuil.barter(helper.getLevel(), mika).stream().anyMatch(s -> s.is(BarbecuetherFeature.GRILLKOOL_ITEM.get()));
        }
        helper.assertTrue(kool, "grillkool is one of the trades");
        helper.succeedWhen(() -> {
            helper.assertFalse(NetherMikaRuil.isAdmiring(mika), "done sniffing");
            List<ItemEntity> gifts = helper.getLevel().getEntitiesOfClass(ItemEntity.class, mika.getBoundingBox().inflate(12),
                    e -> !e.getItem().is(ModItems.VAHOEGE_VADS_INGOT.get()));
            helper.assertTrue(!gifts.isEmpty(), "a present was thrown");
            helper.assertTrue(mika.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(), "the ingot is gone");
            gifts.forEach(Entity::discard);
            mika.discard();
            remove(helper, player);
        });
    }

    // --- brewing ------------------------------------------------------------------------------------------------------------

    private static GuhbrouwketelBlockEntity ketel(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, SpiesburchtFeature.GUHBROUWKETEL.get());
        return (GuhbrouwketelBlockEntity) helper.getBlockEntity(at);
    }

    private static void use(ServerPlayer player, GuhbrouwketelBlockEntity ketel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ketel.use(player, InteractionHand.MAIN_HAND);
    }

    /** Stoke it, pour kaassaus in, stir in knabbels, let it bubble, bottle three Drankjes van Vahoegheid, drink one. */
    @GuhTest(template = ROOM, timeoutTicks = GuhbrouwketelBlockEntity.BREW_TICKS + 60)
    public static void brewingAGuhdrankje(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        BlockPos at = new BlockPos(5, 1, 5);
        player.getInventory().setSelectedSlot(8);             // (the drankjes go into the first free slots, not into the hand)
        GuhbrouwketelBlockEntity ketel = ketel(helper, at);
        use(player, ketel, new ItemStack(ModItems.KAAS_KNABBELS.get()));
        helper.assertTrue(!ketel.isBrewing() && ketel.portions() == 0, "no sauce yet: nothing happens");
        use(player, ketel, new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()));
        helper.assertTrue(ketel.portions() == GuhbrouwketelBlockEntity.PORTIONS && player.getMainHandItem().is(Items.BUCKET), "a pan of kaasbouillon");
        use(player, ketel, new ItemStack(ModItems.KAAS_KNABBELS.get()));
        helper.assertTrue(!ketel.isBrewing(), "a cold barbecue doesn't brew");
        use(player, ketel, new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get()));
        helper.assertTrue(ketel.fuel() == GuhbrouwketelBlockEntity.BREWS_PER_POWDER && helper.getBlockState(at).getValue(GuhbrouwketelBlock.LIT), "stoked");
        use(player, ketel, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        helper.assertTrue(ketel.isBrewing() && player.getMainHandItem().getCount() == 1, "it bubbles");
        use(player, ketel, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "not before it's done");
        helper.succeedWhen(() -> {
            GuhbrouwketelBlockEntity now = (GuhbrouwketelBlockEntity) helper.getBlockEntity(at);
            helper.assertTrue(!now.isBrewing() && now.contents() == Brouwsel.VAHOEGHEID, "done: Vahoegheid (brewing " + now.isBrewing()
                    + ", " + now.contents() + ", same " + (now == ketel) + ", state " + helper.getBlockState(at) + ")");
            int drankjes = 0;
            for (int i = 0; i < 3; i++) {
                use(player, ketel, new ItemStack(Items.GLASS_BOTTLE));
            }
            for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
                drankjes += s.is(SpiesburchtFeature.DRANKJE_VAN_VAHOEGHEID.get()) ? s.getCount() : 0;
            }
            helper.assertTrue(drankjes == 3 && ketel.portions() == 0 && ketel.contents() == Brouwsel.BOUILLON, "three drankjes, an empty pan: " + drankjes);
            ItemStack drankje = new ItemStack(SpiesburchtFeature.DRANKJE_VAN_VAHOEGHEID.get());
            ItemStack left = drankje.finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(player.hasEffect(MobEffects.SPEED) && left.is(Items.GLASS_BOTTLE), "VAHOEG! (and the bottle stays)");
            player.getInventory().clearContent();
            remove(helper, player);
        });
    }

    /** Every Guhdrankje can be brewed, also without guhs:moeraskaas (Mika's vet) and without the effect guhs:stil. */
    @GuhTest(template = EMPTY)
    public static void brewingWorksWithoutTheOtherSlices(GameTestHelper helper) {
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(ModItems.KAAS_KNABBELS.get())) == Brouwsel.VAHOEGHEID, "knabbels: Vahoegheid");
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(BarbecuetherFeature.GLOEIKOOLGRUIS.get())) == Brouwsel.ROOKLOOP, "gloeikoolgruis: Rookloop");
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(ModItems.MIKA_VET.get())) == Brouwsel.SLUIPKNABBEL, "Mika's vet: Sluipknabbel");
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(BuiltInRegistries.ITEM.get(Guhs.id("guh_slimeball")))) == Brouwsel.GUHSPRONG, "guh slime: Guhsprong");
        helper.assertTrue(Brouwsel.forIngredient(new ItemStack(Items.DIRT)) == null, "dirt brews nothing");
        // guhs:moeraskaas (Kaasmoeras) is an optional ingredient: when it's there it brews Sluipknabbel too
        Identifier moeraskaas = Guhs.id("moeraskaas");
        if (BuiltInRegistries.ITEM.containsKey(moeraskaas)) {
            helper.assertTrue(Brouwsel.forIngredient(new ItemStack(BuiltInRegistries.ITEM.get(moeraskaas))) == Brouwsel.SLUIPKNABBEL, "moeraskaas: Sluipknabbel");
        }
        // guhs:stil (the Stille Voorraadkelder) or, without it, invisibility
        var effects = Brouwsel.SLUIPKNABBEL.effects();
        helper.assertTrue(effects.size() == 1, "one effect");
        if (Brouwsel.stil().isPresent()) {
            helper.assertTrue(effects.get(0).getEffect().is(Brouwsel.STIL), "guhs:stil when it exists");
        } else {
            helper.assertTrue(effects.get(0).getEffect().equals(MobEffects.INVISIBILITY), "invisibility without guhs:stil");
        }
        for (Brouwsel b : Brouwsel.values()) {
            helper.assertTrue(b == Brouwsel.BOUILLON || (!b.drankje().isEmpty() && !b.effects().isEmpty()), "a drankje for " + b.id());
        }
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Guhs.id("grillspiespoeder")).isPresent()
                && helper.getLevel().getRecipeManager().byKey(Guhs.id("guhbrouwketel")).isPresent(), "powder and ketel recipes");
        helper.succeed();
    }

    // --- the Aangebrande Mika -----------------------------------------------------------------------------------------------

    /** A T of ash with three heads: he wakes up (the T is used up), can't be hurt while he bakes, breaks nothing, drops the gloeister. */
    @GuhTest(template = ROOM, timeoutTicks = 200)
    public static void aangebrandeMikaFromTheT(GameTestHelper helper) {
        floor(helper);
        normalDifficulty(helper);
        ServerPlayer player = player(helper);
        // a few blocks around that must stay whole
        for (BlockPos p : List.of(new BlockPos(2, 1, 2), new BlockPos(13, 1, 13), new BlockPos(3, 5, 12))) {
            helper.setBlock(p, Blocks.OAK_PLANKS);
        }
        BlockPos base = new BlockPos(8, 1, 8);
        helper.setBlock(base, BarbecuetherFeature.AS_BLOK.get());
        for (int dx = -1; dx <= 1; dx++) {
            helper.setBlock(base.offset(dx, 1, 0), BarbecuetherFeature.AS_BLOK.get());
        }
        helper.setBlock(base.offset(-1, 2, 0), SpiesburchtFeature.VERKOOLDE_MIKAKOP.get());
        helper.setBlock(base.offset(1, 2, 0), SpiesburchtFeature.VERKOOLDE_MIKAKOP.get());
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(AangebrandeMikaEntity.class, helper.getBounds()).isEmpty(), "two heads: nothing yet");
        helper.setBlock(base.offset(0, 2, 0), SpiesburchtFeature.VERKOOLDE_MIKAKOP.get());
        List<AangebrandeMikaEntity> bosses = helper.getLevel().getEntitiesOfClass(AangebrandeMikaEntity.class, helper.getBounds().inflate(2));
        helper.assertTrue(bosses.size() == 1, "the third head wakes him up");
        AangebrandeMikaEntity boss = bosses.get(0);
        helper.assertBlockNotPresent(BarbecuetherFeature.AS_BLOK.get(), base);
        helper.assertBlockNotPresent(SpiesburchtFeature.VERKOOLDE_MIKAKOP.get(), base.offset(0, 2, 0));
        helper.assertTrue(boss.spawningTicks() > 0, "he bakes up first");
        helper.assertFalse(boss.hurt(helper.getLevel().damageSources().playerAttack(player), 20f), "and can't be hurt then");
        boss.finishSpawning();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(boss.spawningTicks() == 0, "awake");
            float hp = boss.getHealth();
            helper.assertTrue(boss.hurt(helper.getLevel().damageSources().playerAttack(player), 20f) && boss.getHealth() < hp, "now he can be hurt");
            boss.invulnerableTime = 0;
            boss.hurt(helper.getLevel().damageSources().playerAttack(player), boss.getHealth() * 0.6f);
            helper.assertTrue(boss.isDoorgebakken() || boss.getHealth() <= boss.getMaxHealth() / 2f, "half way: doorgebakken");
            boss.invulnerableTime = 0;
            boss.hurt(helper.getLevel().damageSources().playerAttack(player), 1000f);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(boss.isDeadOrDying(), "down");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4),
                    e -> e.getItem().is(SpiesburchtFeature.GLOEISTER.get()));
            helper.assertTrue(!drops.isEmpty(), "the gloeister");
            for (BlockPos p : List.of(new BlockPos(2, 1, 2), new BlockPos(13, 1, 13), new BlockPos(3, 5, 12))) {
                helper.assertBlockPresent(Blocks.OAK_PLANKS, p);
            }
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4)).forEach(Entity::discard);
            helper.getLevel().getEntitiesOfClass(VonkMikaEntity.class, helper.getBounds().inflate(8)).forEach(Entity::discard);
            remove(helper, player);
        });
    }

    /** His burning coals (and the Vonk-Mika's embers) singe what they hit, but never set a block on fire. */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void kooltjesBurnNoBlocks(GameTestHelper helper) {
        floor(helper);
        for (int y = 1; y < 6; y++) {
            for (int z = 4; z < 12; z++) {
                helper.setBlock(new BlockPos(13, y, z), Blocks.OAK_PLANKS);
            }
        }
        BlockPos start = helper.absolutePos(new BlockPos(3, 3, 8));
        for (var type : List.of(SpiesburchtFeature.BRANDEND_KOOLTJE.get(), SpiesburchtFeature.GLOEIEND_KOOLTJE.get())) {
            GloeiendKooltje coal = new GloeiendKooltje(type, helper.getLevel());
            coal.snapTo(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5);
            coal.setDeltaMovement(new Vec3(1, 0, 0));
            coal.accelerationPower = 0.1;
            helper.getLevel().addFreshEntity(coal);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(GloeiendKooltje.class, helper.getBounds()).isEmpty(), "they hit the wall");
            for (int y = 1; y < 7; y++) {
                for (int z = 4; z < 12; z++) {
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(13, Math.min(y, 5), z));
                    helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(12, y, z));
                }
            }
        });
    }

    // --- the Knabbelbaken ---------------------------------------------------------------------------------------------------

    /** On a pyramid it gives its effect to players and tamed guhs (not wild ones); a second layer unlocks Guhsprong. */
    @GuhTest(template = ROOM, timeoutTicks = 60)
    public static void knabbelbakenForYouAndYourGuhs(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        BlockPos baken = new BlockPos(8, 2, 8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(baken.offset(dx, -1, dz), ModBlocks.BLOCK_OF_KAASKNABBELS.get());
            }
        }
        helper.setBlock(baken, SpiesburchtFeature.KNABBELBAKEN.get());
        KnabbelbakenBlockEntity be = (KnabbelbakenBlockEntity) helper.getBlockEntity(baken);
        GuhEntity tame = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 8));
        tame.tame(player);
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(12, 1, 8));
        be.refresh();
        helper.assertTrue(be.levels() == 1 && be.gunst() == KnabbelbakenBlockEntity.Gunst.VAHOEG, "one layer: VAHOEG");
        be.cycle(player);
        helper.assertTrue(be.gunst() == KnabbelbakenBlockEntity.Gunst.VAHOEG, "one layer only has VAHOEG");
        be.pulse();
        helper.assertTrue(player.hasEffect(MobEffects.SPEED), "you get VAHOEG");
        helper.assertTrue(tame.hasEffect(MobEffects.SPEED), "your guh too");
        helper.assertFalse(wild.hasEffect(MobEffects.SPEED), "not a wild guh");
        // a second layer (5x5 of vads and knabbels) under the first
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(baken.offset(dx, -2, dz), (dx + dz) % 2 == 0 ? ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS.get() : ModBlocks.BLOCK_OF_KAASKNABBELS.get());
            }
        }
        be.cycle(player);
        helper.assertTrue(be.levels() == 2 && be.gunst() == KnabbelbakenBlockEntity.Gunst.GUHSPRONG, "two layers: Guhsprong");
        be.pulse();
        helper.assertTrue(player.hasEffect(MobEffects.JUMP_BOOST) && tame.hasEffect(MobEffects.JUMP_BOOST), "jump boost for both");
        helper.setBlock(baken.offset(1, -1, 1), Blocks.DIRT);
        be.refresh();
        helper.assertTrue(be.levels() == 0, "a hole in the pyramid: off");
        tame.discard();
        wild.discard();
        remove(helper, player);
        helper.succeed();
    }

    // --- the Asguh, the drops, the heads -----------------------------------------------------------------------------------

    /** The Asguh: fire can't hurt it, it has a Guhdex page and it can be tamed. */
    @GuhTest(template = ROOM, timeoutTicks = 60)
    public static void asguhIsFireproofAndInTheGuhdex(GameTestHelper helper) {
        floor(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 1, 8));
        guh.setVariant(GuhVariant.ASGUH);
        float hp = guh.getHealth();
        guh.hurt(helper.getLevel().damageSources().lava(), 4f);
        guh.hurt(helper.getLevel().damageSources().inFire(), 4f);
        helper.assertTrue(guh.getHealth() == hp, "fire doesn't hurt an Asguh");
        guh.igniteForSeconds(5f);
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.ASGUH) && !GuhVariant.ASGUH.isCharacter() && GuhDex.TAMEABLE.contains(GuhVariant.ASGUH),
                "an Asguh page you can tame");
        helper.assertTrue(GuhVariant.ASGUH.shows("asguh_wangen") && !GuhVariant.NORMAL.shows("asguh_wangen"), "only it has the glowing cheeks");
        GuhEntity normal = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 4));
        float nhp = normal.getHealth();
        normal.hurt(helper.getLevel().damageSources().inFire(), 2f);
        helper.assertTrue(normal.getHealth() < nhp, "(a normal guh does feel it)");
        helper.succeedWhen(() -> {
            helper.assertFalse(guh.isOnFire(), "the flames go right out");
            guh.discard();
            normal.discard();
        });
    }

    private static List<ItemStack> roll(GameTestHelper helper, String table, Entity victim, ServerPlayer killer) {
        LootTable loot = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id(table)));
        var source = helper.getLevel().damageSources().playerAttack(killer);
        LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position()).withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY);
        return loot.getRandomItems(params);
    }

    /** Vonk-Mika's drop grillspiesen (two powder each), Knekel-Mika's now and then a head, the boss always the gloeister. */
    @GuhTest(template = ROOM)
    public static void theDrops(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        VonkMikaEntity vonk = helper.spawn(SpiesburchtFeature.VONK_MIKA.get(), new BlockPos(4, 2, 4));
        KnekelMikaEntity knekel = helper.spawn(SpiesburchtFeature.KNEKEL_MIKA.get(), new BlockPos(10, 1, 10));
        AangebrandeMikaEntity boss = SpiesburchtFeature.AANGEBRANDE_MIKA.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        boss.snapTo(helper.absoluteVec(new Vec3(8, 3, 8)));
        int spiesen = 0, koppen = 0;
        for (int i = 0; i < 1500; i++) {
            for (ItemStack s : roll(helper, "entities/vonk_mika", vonk, player)) {
                spiesen += s.is(SpiesburchtFeature.GRILLSPIES.get()) ? 1 : 0;
            }
            for (ItemStack s : roll(helper, "entities/knekel_mika", knekel, player)) {
                koppen += s.is(SpiesburchtFeature.VERKOOLDE_MIKAKOP_ITEM.get()) ? 1 : 0;
            }
        }
        helper.assertTrue(spiesen > 300, "Vonk-Mika's drop grillspiesen: " + spiesen);
        helper.assertTrue(koppen > 5 && koppen < 200, "a verkoolde mikakop now and then: " + koppen);
        helper.assertTrue(roll(helper, "entities/aangebrande_mika", boss, player).stream().anyMatch(s -> s.is(SpiesburchtFeature.GLOEISTER.get())),
                "the gloeister, always");
        var equip = Equipable.get(new ItemStack(SpiesburchtFeature.VERKOOLDE_MIKAKOP_ITEM.get()));
        helper.assertTrue(equip != null && equip.getEquipmentSlot() == EquipmentSlot.HEAD, "you can wear a mikakop");
        vonk.discard();
        knekel.discard();
        remove(helper, player);
        helper.succeed();
    }

    // --- the data: structures, tiles, spawns, the compass ------------------------------------------------------------------

    private static CompoundTag tile(ServerLevel level, String structure, int i, int j) {
        var rm = level.getServer().getResourceManager();
        var res = rm.getResource(Guhs.id("structure/" + structure + "/stuk_" + i + "_" + j + ".nbt"));
        if (res.isEmpty()) {
            return null;
        }
        try (InputStream in = res.get().open()) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @GuhTest(template = EMPTY)
    public static void theSpiesburchtDataIsComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String s : new String[]{"spiesburcht", "mika_grillpaleis"}) {
            helper.assertTrue(structures.get(Guhs.id(s)) instanceof BurchtStructure, "structure " + s);
            helper.assertTrue(SuperkompasItem.allowed(s), "the super compass finds " + s);
        }
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).containsKey(Guhs.id("barbecue_burchten")), "their structure set");
        // the tiles: the Vonk-Mika spawner and chests in the Spiesburcht, the Nether-Mikas and the knabbel pile in the palace
        int spawners = 0, chests = 0, mikas = 0, knabbels = 0;
        var manager = level.getStructureManager();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                var t = manager.get(Guhs.id("spiesburcht/stuk_" + i + "_" + j));
                if (t.isPresent()) {
                    for (StructureTemplate.StructureBlockInfo info : t.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SPAWNER)) {
                        spawners += info.nbt() != null && info.nbt().getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity").getStringOr("id", "").equals("guhs:vonk_mika") ? 1 : 0;
                    }
                    chests += t.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST).size();
                }
                CompoundTag nbt = tile(level, "mika_grillpaleis", i, j);
                if (nbt != null) {
                    for (Tag e : nbt.getListOrEmpty("entities")) {
                        mikas += ((CompoundTag) e).getCompoundOrEmpty("nbt").getStringOr("id", "").equals("guhs:nether_mika") ? 1 : 0;
                    }
                    var gp = manager.get(Guhs.id("mika_grillpaleis/stuk_" + i + "_" + j)).orElseThrow();
                    knabbels += gp.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.BLOCK_OF_KAASKNABBELS.get()).size();
                }
            }
        }
        helper.assertTrue(spawners == 1, "one Vonk-Mika spawner: " + spawners);
        helper.assertTrue(chests >= 5, "chests in the Spiesburcht: " + chests);
        helper.assertTrue(mikas >= 10, "Nether-Mikas at home in the palace: " + mikas);
        helper.assertTrue(knabbels >= 60, "a mountain of stolen knabbels: " + knabbels);
        // spawns per biome
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        MobSpawnSettings asdal = biomes.get(SpiesburchtFeature.ASDAL).getMobSettings();
        helper.assertTrue(asdal.getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream().anyMatch(d -> d.type == SpiesburchtFeature.KNEKEL_MIKA.get()),
                "Knekel-Mika's in the Asdal");
        helper.assertTrue(asdal.getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream().anyMatch(d -> d.type == ModEntities.GUH.get()),
                "(Asguhs) in the Asdal");
        MobSpawnSettings delta = biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("rookdelta"))).getMobSettings();
        helper.assertTrue(delta.getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream().anyMatch(d -> d.type == SpiesburchtFeature.ROOKGUH.get()),
                "Rookguhs in the Rookdelta");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BLOCK).getTag(SpiesburchtFeature.BAKEN_BASIS).isPresent(), "the baken's base tag");
        helper.assertTrue(EntityType.getKey(SpiesburchtFeature.AANGEBRANDE_MIKA.get()).equals(Guhs.id("aangebrande_mika")), "the boss's id");
        helper.succeed();
    }
}
