package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Y-SCHEIDING: one test per leak channel between the Vadsrijk and the survival worlds (the matrix: SCHEIDING_RAPPORT.md).
 * The gametest server may not have the Vadsrijk dimension: then the swaps are driven directly (kern's {@code Inventaris} +
 * {@link Lichaam}, the same calls kern's reconciler makes) and the border rules are checked through their decision functions;
 * when the dimension IS there, the tests also do the real teleport. Run: {@code runGameTestServer -Pgt=VadsrijkScheiding}.
 */
public class VadsrijkScheidingGameTests {
    private static final String BATCH = "vadsrijk_scheiding";

    /** A survival (non-op) mock player outside, with a clean Vadsrijk record. */
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Scheiding.NOOIT_VRIJ.add(p.getUUID());
        return p;
    }

    /** In and out the way kern's reconciler does it (inventory first, then the hooks: here only the body). */
    static void naarRijk(ServerPlayer p) {
        Inventaris.naarRijk(p);
        Lichaam.verzoen(p);
    }

    static void naarThuis(ServerPlayer p) {
        Inventaris.naarThuis(p);
        Lichaam.verzoen(p);
    }

    static boolean leeg(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (!p.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ---- 1. the inventory swap -----------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal01InventarisWissel(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        var ench = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack zwaard = new ItemStack(Items.DIAMOND_SWORD);
        zwaard.enchant(ench.getOrThrow(Enchantments.SHARPNESS), 5);
        zwaard.set(DataComponents.CUSTOM_NAME, Component.literal("Knuffelkling"));
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.EMERALD, 7))));
        // a full inventory: every main slot filled
        for (int i = 0; i < 36; i++) {
            p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 1 + i));
        }
        p.getInventory().setItem(0, zwaard.copy());
        p.getInventory().setItem(1, shulker.copy());
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        p.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 3));   // on the cursor
        p.inventoryMenu.getCraftSlots().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));   // in the 2x2 grid
        // an accessory slot (Curios/Accessories are not in the pack: a test provider walks the same code path)
        ItemStack[] vak = {new ItemStack(Items.TOTEM_OF_UNDYING)};
        ExtraVakken.Bron bron = new ExtraVakken.Bron() {
            public String id() {
                return "y_scheiding_test";
            }

            public List<ExtraVakken.Vak> vakken(ServerPlayer x) {
                return x == p ? List.of(new ExtraVakken.Vak() {
                    public ItemStack get() {
                        return vak[0];
                    }

                    public void set(ItemStack s) {
                        vak[0] = s;
                    }
                }) : List.of();
            }
        };
        ExtraVakken.ALLE.add(bron);
        try {
            naarRijk(p);
            helper.assertTrue(leeg(p), "inside: nothing of the home inventory is live (36 + armour + offhand)");
            helper.assertTrue(p.inventoryMenu.getCarried().isEmpty(), "the cursor item went into the stash too");
            helper.assertTrue(p.inventoryMenu.getCraftSlots().isEmpty(), "the 2x2 grid went into the stash too");
            helper.assertTrue(vak[0].isEmpty(), "the accessory slot is empty inside");
            // something of the Vadsrijk
            p.getInventory().setItem(4, new ItemStack(Items.STICK, 5));
            naarThuis(p);
            helper.assertTrue(ItemStack.matches(p.getInventory().getItem(0), zwaard), "the enchanted, named sword is back exactly");
            helper.assertTrue(ItemStack.matches(p.getInventory().getItem(1), shulker), "the shulker box is back with the same contents");
            helper.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && p.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS),
                    "the armour is back");
            helper.assertTrue(p.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD), "the offhand is back");
            helper.assertTrue(vak[0].is(Items.TOTEM_OF_UNDYING), "the accessory is back in its slot");
            helper.assertTrue(!p.getInventory().contains(new ItemStack(Items.STICK)), "the Vadsrijk stick stayed in the Vadsrijk");
            // the cursor + grid items had no room in the full inventory: kept, in the home "gevonden" stash (never lost, never inside)
            SpelerData d = Spelers.data(p);
            int goud = tel(p, Items.GOLD_INGOT), plank = tel(p, Items.OAK_PLANKS);
            helper.assertTrue(goud + gevonden(p, d, Items.GOLD_INGOT) == 3 && plank + gevonden(p, d, Items.OAK_PLANKS) == 2,
                    "cursor/grid items are kept on the home side (inventory or gevonden)");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8)).isEmpty(),
                    "nothing was dropped on the ground (it would be the other world's ground)");
            naarRijk(p);
            helper.assertTrue(p.getInventory().getItem(4).is(Items.STICK) && tel(p, Items.GOLD_INGOT) == 0, "inside again: the stick, no gold");
            naarThuis(p);
        } finally {
            ExtraVakken.ALLE.remove(bron);
        }
        helper.succeed();
    }

    static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    static int gevonden(ServerPlayer p, SpelerData d, net.minecraft.world.item.Item item) {
        var ops = p.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        int n = 0;
        for (var t : d.gevonden) {
            if (!Inventaris.THUIS.equals(t.getStringOr(Inventaris.WERELD, Inventaris.THUIS))) {
                continue;
            }
            var s = net.minecraft.world.ItemStackWithSlot.CODEC.parse(ops, t).result();
            if (s.isPresent() && s.get().stack().is(item)) {
                n += s.get().stack().getCount();
            }
        }
        return n;
    }

    // ---- 2. the ender chest ---------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal02Enderkist(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.getEnderChestInventory().setItem(0, new ItemStack(Items.NETHERITE_INGOT));
        naarRijk(p);
        // however it is opened (the block is refused by kern; FTB Essentials /enderchest, an op's /invsee...): it closes at once
        p.openMenu(new SimpleMenuProvider((id, inv, x) -> ChestMenu.threeRows(id, inv, x.getEnderChestInventory()), Component.literal("ender")));
        helper.assertTrue(p.containerMenu == p.inventoryMenu, "inside, the (survival) ender chest menu does not stay open");
        helper.assertTrue(Grens.GEWEIGERD.contains(Grens.wortel(List.of("enderchest"))) && Grens.GEWEIGERD.contains(Grens.wortel(List.of("ftbessentials", "ec"))),
                "/enderchest (FTB Essentials) is refused inside");
        naarThuis(p);
        p.openMenu(new SimpleMenuProvider((id, inv, x) -> ChestMenu.threeRows(id, inv, x.getEnderChestInventory()), Component.literal("ender")));
        helper.assertTrue(p.containerMenu != p.inventoryMenu, "at home the ender chest works as always");
        p.closeContainer();
        helper.assertTrue(p.getEnderChestInventory().getItem(0).is(Items.NETHERITE_INGOT), "its contents are untouched");
        helper.succeed();
    }

    // ---- 3. containers carried across ---------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal03Containers(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ItemStack shulker = new ItemStack(Items.PINK_SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND_BLOCK, 64))));
        ItemStack bundel = new ItemStack(Items.BUNDLE);
        bundel.set(DataComponents.BUNDLE_CONTENTS, bundel(new ItemStack(Items.EMERALD, 16)));
        p.getInventory().setItem(3, shulker.copy());
        p.getInventory().setItem(4, bundel.copy());
        naarRijk(p);
        helper.assertTrue(tel(p, Items.PINK_SHULKER_BOX) == 0 && tel(p, Items.BUNDLE) == 0, "the containers don't come along (the whole inventory swaps)");
        // a bundle found inside stays inside, contents and all
        ItemStack rijkBundel = new ItemStack(Items.BUNDLE);
        rijkBundel.set(DataComponents.BUNDLE_CONTENTS, bundel(new ItemStack(Items.STICK, 3)));
        p.getInventory().setItem(8, rijkBundel.copy());
        naarThuis(p);
        helper.assertTrue(ItemStack.matches(p.getInventory().getItem(3), shulker) && ItemStack.matches(p.getInventory().getItem(4), bundel),
                "back home: same containers, same contents");
        helper.assertTrue(p.getInventory().getItem(8).isEmpty(), "the Vadsrijk bundle did not come out");
        naarRijk(p);
        helper.assertTrue(ItemStack.matches(p.getInventory().getItem(8), rijkBundel), "and is still inside next time");
        naarThuis(p);
        helper.succeed();
    }

    static BundleContents bundel(ItemStack s) {
        BundleContents.Mutable m = new BundleContents.Mutable(BundleContents.EMPTY);
        m.tryInsert(s);
        return m.toImmutable();
    }

    // ---- 4. entities at the border ------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal04GrensEntiteiten(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ResourceKeyLevels k = new ResourceKeyLevels();
        ItemEntity item = new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.DIAMOND));
        Entity kar = EntityType.CHEST_MINECART.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        Entity trechter = EntityType.HOPPER_MINECART.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        Entity lijst = EntityType.ITEM_FRAME.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        Entity orb = EntityType.EXPERIENCE_ORB.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        for (Entity e : new Entity[] {item, kar, trechter, lijst, orb}) {
            helper.assertTrue(!Grens.magReizen(e, k.thuis, Vadsrijk.DIM, true), e.getType() + " may not go in, not even through Vadsrijk code");
            helper.assertTrue(!Grens.magReizen(e, Vadsrijk.DIM, k.thuis, true), e.getType() + " may not come out");
            helper.assertTrue(Grens.magReizen(e, k.thuis, k.nether, false), e.getType() + " between two home worlds: untouched");
        }
        // the real thing, when this server has the dimension
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr != null) {
            BlockPos plek = new BlockPos(8, Vadsrijk.GROND + 1, 8);
            vr.getChunk(plek);
            ItemEntity echt = new ItemEntity(helper.getLevel(), p.getX(), p.getY() + 1, p.getZ(), new ItemStack(Items.DIAMOND));
            helper.getLevel().addFreshEntity(echt);
            Entity na = echt.teleport(new TeleportTransition(vr, Vec3.atBottomCenterOf(plek), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
            helper.assertTrue(na == null && echt.isAlive() && echt.level() == helper.getLevel(), "a dropped item thrown at the border stays home");
            echt.discard();
        }
        helper.succeed();
    }

    /** (the home dimensions used in the border tests) */
    static final class ResourceKeyLevels {
        final net.minecraft.resources.ResourceKey<Level> thuis = Level.OVERWORLD, nether = Level.NETHER;
    }

    // ---- 5. vanilla xp ----------------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal05VanillaXp(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.setExperienceLevels(30);
        p.experienceProgress = 0.5f;
        naarRijk(p);
        helper.assertTrue(p.experienceLevel == 0 && p.experienceProgress == 0f, "inside: no survival levels to spend (anvil), got " + p.experienceLevel);
        p.giveExperienceLevels(4);
        naarThuis(p);
        helper.assertTrue(p.experienceLevel == 30 && Math.abs(p.experienceProgress - 0.5f) < 1e-4, "home: exactly the 30.5 levels again, got " + p.experienceLevel);
        naarRijk(p);
        helper.assertTrue(p.experienceLevel == 4, "inside again: the 4 levels from inside, got " + p.experienceLevel);
        naarThuis(p);
        helper.assertTrue(!Lichaam.verzoen(p) && p.experienceLevel == 30, "a second reconcile does nothing");
        helper.succeed();
    }

    // ---- 6. effects, health, hunger, fire, absorption ------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal06Lijf(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1));
        p.setAbsorptionAmount(4f);
        p.setHealth(6f);
        p.getFoodData().setFoodLevel(8);
        p.setRemainingFireTicks(100);
        naarRijk(p);
        helper.assertTrue(p.getActiveEffects().isEmpty(), "inside: no survival effects");
        helper.assertTrue(p.getAbsorptionAmount() == 0f && p.getRemainingFireTicks() <= 0, "inside: no absorption, not burning");
        helper.assertTrue(p.getHealth() == p.getMaxHealth() && p.getFoodData().getFoodLevel() == 20, "inside: kern's full health and food");
        // what the Vadsrijk gives stays there
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0));
        naarThuis(p);
        helper.assertTrue(!p.hasEffect(MobEffects.NIGHT_VISION) && !p.hasEffect(MobEffects.WATER_BREATHING), "home: the Vadsrijk effects are gone");
        MobEffectInstance kracht = p.getEffect(MobEffects.STRENGTH);
        helper.assertTrue(kracht != null && kracht.getAmplifier() == 1 && kracht.getDuration() > 2000, "home: Strength II is back with its time");
        helper.assertTrue(Math.abs(p.getHealth() - 6f) < 1e-3, "home: the same 3 hearts (no free heal), got " + p.getHealth());
        helper.assertTrue(p.getFoodData().getFoodLevel() == 8, "home: the same hunger (no free meal)");
        helper.assertTrue(Math.abs(p.getAbsorptionAmount() - 4f) < 1e-3, "home: the absorption hearts are back, got " + p.getAbsorptionAmount());
        helper.assertTrue(p.getRemainingFireTicks() > 0, "home: still burning (no free extinguish)");
        p.clearFire();
        helper.succeed();
    }

    // ---- 7. death ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal07Dood(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        naarRijk(p);
        p.getInventory().setItem(0, new ItemStack(Items.STICK));
        naarThuis(p);
        // dying at home with Vadsrijk items stashed: the stash is in the player attachment, which is copied on death
        ServerPlayer na = GuhMockPlayer.of(helper);
        na.restoreFrom(p, false);
        SpelerData d = Spelers.data(na);
        helper.assertTrue(!d.binnen && d.rijkInventaris.size() == 1, "after death (respawn copy) the Vadsrijk stash is still there");
        // the home inventory after a death is vanilla's business (drops / keepInventory); a dying player inside is caught by kern
        // (faint instead of death) and can never respawn inside: the respawn point can't be set there
        ServerPlayer q = speler(helper);
        var voor = q.getRespawnConfig();
        q.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(Vadsrijk.DIM, new BlockPos(8, 70, 8)), 0f, 0f), false), false);
        helper.assertTrue(q.getRespawnConfig() == voor, "no respawn point in the Vadsrijk");
        q.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, new BlockPos(8, 70, 8)), 0f, 0f), false), false);
        helper.assertTrue(q.getRespawnConfig() != null && q.getRespawnConfig().respawnData().dimension() == Level.OVERWORLD, "at home it works");
        helper.succeed();
    }

    // ---- 8. logout / crash -------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal08UitloggenCrash(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 10));
        p.setExperienceLevels(12);
        naarRijk(p);
        p.getInventory().setItem(2, new ItemStack(Items.STICK, 2));
        // log out / crash inside = player.dat as it is now: live (Vadsrijk) inventory + attachment (home stash) in ONE file
        var reg = helper.getLevel().registryAccess();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, reg);
        p.saveWithoutId(out);
        var tag = out.buildResult();
        ServerPlayer terug = GuhMockPlayer.of(helper);
        terug.load(TagValueInput.create(ProblemReporter.DISCARDING, reg, tag));
        SpelerData d = Spelers.data(terug);
        helper.assertTrue(d.binnen && terug.getInventory().getItem(2).is(Items.STICK) && tel(terug, Items.DIAMOND) == 0,
                "loaded inside: the Vadsrijk inventory is live, no diamonds");
        helper.assertTrue(terug.experienceLevel == 0 && Lichaam.thuisWacht(terug), "loaded inside: Vadsrijk xp live, the home body waits");
        // logging in somewhere at home (kicked out of the dimension, or it was removed): the reconciler swaps back
        naarThuis(terug);
        helper.assertTrue(tel(terug, Items.DIAMOND) == 10 && terug.experienceLevel == 12 && tel(terug, Items.STICK) == 0, "back home: all of it");
        // the swap is idempotent: a second call (login + tick + dimension event in the same second) changes nothing
        helper.assertTrue(!Inventaris.naarThuis(terug) && !Lichaam.verzoen(terug), "no double swap");
        helper.succeed();
    }

    // ---- 9. pets, mounts, leashes ----------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal09Dieren(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ResourceKeyLevels k = new ResourceKeyLevels();
        Pig varken = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        Entity wolf = helper.spawn(EntityType.WOLF, new BlockPos(2, 2, 1));
        Entity paard = helper.spawn(EntityType.HORSE, new BlockPos(3, 2, 1));
        for (Entity e : new Entity[] {varken, wolf, paard}) {
            helper.assertTrue(!Grens.magReizen(e, k.thuis, Vadsrijk.DIM, true) && !Grens.magReizen(e, Vadsrijk.DIM, k.thuis, true),
                    e.getType() + " never crosses (pets, mounts, leashed animals)");
        }
        // a Vadsrijk pet or mount (vr_ entity) can't come out either: same rule, whatever the entity
        helper.assertTrue(!Grens.magReizen(helper.spawn(nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry.HUISDIER.get(), new BlockPos(1, 2, 2)),
                Vadsrijk.DIM, k.thuis, true), "a Vadsrijk pet stays inside");
        // the player crossing: off the mount, leash dropped
        varken.setLeashedTo(p, true);
        p.startRiding(paard, true, false);
        Grens.losLaten(p);
        helper.assertTrue(!p.isPassenger() && !varken.isLeashed(), "crossing: off the horse, the leash stays behind");
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr != null) {
            p.startRiding(paard, true, false);
            VadsrijkCommando.naarBinnen(p, false);
            helper.assertTrue(Vadsrijk.in(p) && paard.level() == helper.getLevel() && paard.isAlive(), "the horse stayed home");
            VadsrijkCommando.terug(p);
        }
        helper.succeed();
    }

    // ---- 10. teleports from other mods -------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal10Teleports(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ResourceKeyLevels k = new ResourceKeyLevels();
        helper.assertTrue(!Grens.magReizen(p, k.thuis, Vadsrijk.DIM, false), "a non-op can't be teleported in by another mod (/home, /back, /tpa)");
        helper.assertTrue(Grens.magReizen(p, k.thuis, Vadsrijk.DIM, true), "...but the Vadsrijk's own entry works");
        helper.assertTrue(Grens.magReizen(p, Vadsrijk.DIM, k.thuis, false), "leaving is always allowed (the swap follows the dimension change)");
        helper.assertTrue(Grens.vanOns() == false, "a gametest is not Vadsrijk code (the stack check is not fooled)");
        for (String c : new String[] {"home", "back", "tpa", "tpahere", "tpaccept", "warp", "rtp", "sethome"}) {
            helper.assertTrue(Grens.GEWEIGERD.contains(Grens.wortel(List.of(c))), "/" + c + " is refused inside");
        }
        helper.assertTrue(!Grens.GEWEIGERD.contains(Grens.wortel(List.of("guhs", "vadsrijk"))), "/guhs vadsrijk still works inside");
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        if (vr != null) {
            BlockPos plek = new BlockPos(8, Vadsrijk.GROND + 1, 8);
            vr.getChunk(plek);
            ServerPlayer na = p.teleport(new TeleportTransition(vr, Vec3.atBottomCenterOf(plek), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
            helper.assertTrue(na == null && !Vadsrijk.in(p), "a plain teleport (like FTB Essentials /home) into the Vadsrijk is refused");
            VadsrijkCommando.naarBinnen(p, false);
            helper.assertTrue(Vadsrijk.in(p) && Spelers.data(p).binnen && Lichaam.thuisWacht(p), "the real entry: inside, swapped");
            p.teleport(new TeleportTransition(helper.getLevel(), p.position(), Vec3.ZERO, 0f, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
            helper.assertTrue(!Vadsrijk.in(p) && !Spelers.data(p).binnen && !Lichaam.thuisWacht(p), "leaving by any teleport swaps back");
        }
        helper.succeed();
    }

    // ---- 14. ops -----------------------------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void kanaal14Ops(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Scheiding.NOOIT_VRIJ.remove(p.getUUID());
        boolean op = p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        helper.assertTrue(Scheiding.vrijgesteld(p) == op, "ops (and only ops) skip the border rules");
        if (op) {
            helper.assertTrue(Grens.magReizen(p, Level.OVERWORLD, Vadsrijk.DIM, false), "an op may teleport in directly (testing)");
        }
        // ...but the swaps are for everyone: an op's inventory and body swap like anyone's
        p.setExperienceLevels(7);
        naarRijk(p);
        helper.assertTrue(p.experienceLevel == 0, "an op's xp swaps too");
        naarThuis(p);
        helper.assertTrue(p.experienceLevel == 7, "and comes back");
        // entities never skip
        helper.assertTrue(!Grens.magReizen(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.DIAMOND)), Level.OVERWORLD, Vadsrijk.DIM, true),
                "items never cross");
        helper.succeed();
    }

    static List<ItemStack> alles(Container c) {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            l.add(c.getItem(i));
        }
        return l;
    }
}
