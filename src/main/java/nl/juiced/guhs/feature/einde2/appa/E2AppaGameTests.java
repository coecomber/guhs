package nl.juiced.guhs.feature.einde2.appa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of einde2, slice appa: the Appa-guh is one per player and the grant is idempotent; the fluitje brings him back
 * after he died, was unloaded or stands in another dimension, with his chest and his name, and an older copy steps aside;
 * the seats (owner, three guests, five team seats, a carried guh) and the saddle chest; no getting off in the air; being
 * kept away; the flight by himself over a wall, also when the player may steer; the Momo-guh: one per player, onto the
 * shoulder, picking up (and what he leaves), the borrowed knabbel that comes back, sitting at home.
 * Templates einde2_appa_test_wei (16 x 16 grass) and einde2_appa_test_baan (14 x 44 grass with a wall across).
 */
public class E2AppaGameTests {
    private static final String WEI = "einde2_appa_test_wei";
    private static final String BAAN = "einde2_appa_test_baan";

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            AppaGuhEntity appa = Appa.geladen(p);
            if (appa != null) {
                appa.discard();
            }
            MomoGuhEntity momo = Momo.geladen(p);
            if (momo != null) {
                momo.discard();
            }
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** His Appa, moved onto our own floor (he lands a few blocks from the player: that can be outside the room). */
    static AppaGuhEntity appaOp(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        AppaGuhEntity appa = Appa.geef(p);
        helper.assertTrue(appa != null, "geef gives an Appa");
        BlockPos abs = helper.absolutePos(at);
        appa.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return appa;
    }

    private static boolean gezien(ServerPlayer p, GuhVariant pagina) {
        return GuhWorldData.get(p.level().getServer()).player(p.getUUID()).seen.contains(pagina);
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        return GuhQuests.count(p, item);
    }

    // =====================================================================================================================

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 200)
    public static void einde2AppaEenPerSpeler(GameTestHelper helper) {
        ServerPlayer a = speler(helper, new BlockPos(4, 2, 4));
        ServerPlayer b = speler(helper, new BlockPos(11, 2, 4));
        helper.assertTrue(!Appa.heeft(a) && Appa.roep(a) == null, "nobody has an Appa before the grant");
        AppaGuhEntity eerste = Appa.geef(a);
        helper.assertTrue(eerste != null && Appa.heeft(a) && eerste.isEigenaar(a) && !eerste.isEigenaar(b), "the grant makes his Appa");
        helper.assertTrue(tel(a, E2AppaFeature.APPA_FLUITJE.get()) == 1, "with a fluitje");
        helper.assertTrue(gezien(a, GuhVariant.APPA_GUH), "and his Guhdex page");
        AppaGuhEntity weer = Appa.geef(a);
        helper.assertTrue(weer == eerste && tel(a, E2AppaFeature.APPA_FLUITJE.get()) == 1, "a second grant: the same Appa, no second fluitje");
        helper.assertTrue(!Appa.heeft(b), "one per player: the other has none");
        AppaGuhEntity vanB = Appa.geef(b);
        helper.assertTrue(vanB != null && vanB != eerste && vanB.isEigenaar(b), "the other player gets their own");
        ServerLevel level = helper.getLevel();
        long appas = level.getEntitiesOfClass(AppaGuhEntity.class, helper.getBounds().inflate(12)).stream()
                .filter(x -> x.isEigenaar(a) || x.isEigenaar(b)).count();
        helper.assertTrue(appas == 2, "two players, two Appas (found " + appas + ")");
        weg(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 300)
    public static void einde2AppaKomtAltijdTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 5));
        AppaGuhEntity appa = appaOp(helper, p, new BlockPos(8, 2, 10));
        appa.setCustomName(Component.literal("Wolkje"));
        appa.kist().setItem(3, new ItemStack(Items.APPLE, 7));
        Appa.meld(appa);
        helper.assertTrue(Appa.kist(p).get(3).is(Items.APPLE) && Appa.kist(p).get(3).getCount() == 7, "his chest lives with the player too");
        helper.assertTrue("Wolkje".equals(Appa.naam(p) == null ? null : Appa.naam(p).getString()), "and his name");
        // nothing hurts him
        appa.hurtServer(level, level.damageSources().generic(), 50f);
        appa.hurtServer(level, level.damageSources().playerAttack(p), 50f);
        helper.assertTrue(appa.isAlive() && appa.getHealth() == appa.getMaxHealth(), "nothing hurts an Appa-guh");
        // dead (only /kill does that): the fluitje makes him again, the same Appa
        appa.kill(level);
        helper.assertTrue(Appa.geladen(p) == null, "killed: gone");
        AppaFluitjeItem.blaas(p, false);
        AppaGuhEntity nieuw = Appa.geladen(p);
        helper.assertTrue(nieuw != null && nieuw != appa && nieuw.isAlive(), "the fluitje brings a new one");
        helper.assertTrue(nieuw.kist().getItem(3).is(Items.APPLE) && nieuw.kist().getItem(3).getCount() == 7, "with the same chest");
        helper.assertTrue(nieuw.hasCustomName() && "Wolkje".equals(nieuw.getCustomName().getString()), "and the same name");
        // unloaded (his chunk went away): made again, and the old copy steps aside when it turns up
        int genVoor = nieuw.gen();
        nieuw.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        AppaGuhEntity derde = Appa.roep(p);
        helper.assertTrue(derde != null && derde != nieuw && derde.gen() == genVoor + 1, "unloaded: made again with a new number");
        AppaGuhEntity oud = E2AppaFeature.APPA_GUH.get().create(level, EntitySpawnReason.LOAD);
        oud.zetEigenaar(p.getUUID(), genVoor);
        oud.kist().setItem(3, new ItemStack(Items.APPLE, 7));
        BlockPos abs = helper.absolutePos(new BlockPos(3, 2, 12));
        oud.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        level.addFreshEntity(oud);
        // another dimension: he is made again where the player is
        ServerLevel ander = level.getServer().getLevel(Level.NETHER);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(oud.isRemoved() && Appa.geladen(p) == derde && !derde.isRemoved(), "the old copy removed itself, the new one stays");
            if (ander != null && ander != level) {
                derde.discard();
                AppaGuhEntity ver = E2AppaFeature.APPA_GUH.get().create(ander, EntitySpawnReason.TRIGGERED);
                ver.zetEigenaar(p.getUUID(), Appa.gen(p));
                ver.snapTo(0.5, 80, 0.5, 0, 0);
                ander.addFreshEntity(ver);
                Appa.meld(ver);
                helper.assertTrue(Appa.geladen(p) == ver, "his Appa stands in another dimension");
                AppaGuhEntity hier = Appa.roep(p);
                helper.assertTrue(hier != null && hier.level() == level && ver.isRemoved(), "called: he is made again in this dimension");
                helper.assertTrue(hier.kist().getItem(3).is(Items.APPLE), "still with the chest");
            }
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 200)
    public static void einde2AppaStoelenEnKist(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer baas = speler(helper, new BlockPos(8, 2, 3));
        ServerPlayer gast = speler(helper, new BlockPos(6, 2, 3));
        ServerPlayer gast2 = speler(helper, new BlockPos(10, 2, 3));
        AppaGuhEntity appa = appaOp(helper, baas, new BlockPos(8, 2, 9));
        // a click: the owner on his head, a guest in the basket; sneak + click: the chest, only for the owner
        appa.mobInteract(gast, InteractionHand.MAIN_HAND);
        helper.assertTrue(gast.getVehicle() == appa && appa.getControllingPassenger() == null, "a guest rides along but doesn't steer");
        appa.mobInteract(baas, InteractionHand.MAIN_HAND);
        helper.assertTrue(baas.getVehicle() == appa && appa.getControllingPassenger() == baas && appa.bestuurder() == baas, "the owner steers");
        gast2.setShiftKeyDown(true);
        appa.mobInteract(gast2, InteractionHand.MAIN_HAND);
        helper.assertTrue(gast2.containerMenu == gast2.inventoryMenu && gast2.getVehicle() == null, "the chest is not for a guest");
        gast2.setShiftKeyDown(false);
        baas.stopRiding();
        baas.setShiftKeyDown(true);
        appa.mobInteract(baas, InteractionHand.MAIN_HAND);
        baas.setShiftKeyDown(false);
        helper.assertTrue(baas.containerMenu != baas.inventoryMenu, "sneak + click opens the saddle chest for the owner");
        baas.containerMenu.getSlot(0).set(new ItemStack(Items.COOKIE, 5));
        helper.assertTrue(Appa.kist(baas).get(0).is(Items.COOKIE), "what goes into the chest is saved with the owner at once");
        baas.closeContainer();
        appa.mobInteract(baas, InteractionHand.MAIN_HAND);
        // the owner's own guh from their arm
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 4));
        guh.tame(baas);
        UUID guhId = guh.getUUID();
        baas.setItemInHand(InteractionHand.MAIN_HAND, PickedUpGuhItem.pickUp(guh));
        appa.mobInteract(baas, InteractionHand.MAIN_HAND);
        Entity opAppa = appa.gasten().stream().filter(e -> e instanceof GuhEntity).findFirst().orElse(null);
        helper.assertTrue(opAppa != null && opAppa.getUUID().equals(guhId) && baas.getMainHandItem().isEmpty(), "a carried guh hops into the basket");
        // three guests is full
        appa.mobInteract(gast2, InteractionHand.MAIN_HAND);
        helper.assertTrue(gast2.getVehicle() == appa && appa.gasten().size() == AppaGuhEntity.GASTEN, "the third guest fits");
        ServerPlayer gast3 = speler(helper, new BlockPos(12, 2, 3));
        appa.mobInteract(gast3, InteractionHand.MAIN_HAND);
        helper.assertTrue(gast3.getVehicle() == null, "a fourth guest doesn't");
        // the team seats: five, only through zetTeam
        List<Entity> team = new ArrayList<>();
        for (int i = 0; i < AppaGuhEntity.TEAM_STOELEN + 1; i++) {
            Entity e = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2 + i, 2, 13));
            boolean erop = appa.zetTeam(e);
            helper.assertTrue(erop == (i < AppaGuhEntity.TEAM_STOELEN), "team seat " + i + ": " + erop);
            team.add(e);
        }
        helper.assertTrue(appa.team().size() == AppaGuhEntity.TEAM_STOELEN && appa.getPassengers().size() == 9, "nine on his back");
        Set<String> plekken = new HashSet<>();
        for (Entity e : appa.getPassengers()) {
            Vec3 at = appa.getPassengerRidingPosition(e).subtract(appa.position());
            plekken.add(Math.round(at.x * 10) + "," + Math.round(at.z * 10));
            helper.assertTrue(at.y > 2.3 && at.horizontalDistance() < 2.6, "a seat on his back: " + at);
        }
        helper.assertTrue(plekken.size() == 9, "everyone has a seat of their own (" + plekken.size() + ")");
        // a pat on his flank: the guhs hop out; the team steps off with teamAf
        appa.hurtServer(level, level.damageSources().playerAttack(baas), 1f);
        helper.assertTrue(opAppa.getVehicle() == null && gast.getVehicle() == appa, "the owner's pat sends the guhs off, not the players");
        appa.teamAf();
        helper.assertTrue(appa.team().isEmpty() && team.stream().allMatch(e -> e.getVehicle() == null), "the team steps off");
        opAppa.discard();
        team.forEach(Entity::discard);
        weg(helper, baas, gast, gast2, gast3);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 200)
    public static void einde2AppaNietAfstappenInDeLucht(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 4));
        AppaGuhEntity appa = appaOp(helper, p, new BlockPos(8, 6, 9));
        appa.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getVehicle() == appa, "on his head");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(appa.isLucht(), "he hangs in the air with his rider");
            p.setShiftKeyDown(true);
            p.stopRiding();
            helper.assertTrue(p.getVehicle() == appa, "sneak in the air is not getting off");
            p.setShiftKeyDown(false);
            // kept away (the story's kidnapping): gone, and the fluitje can't bring him
            Appa.houdWeg(p, true);
            helper.assertTrue(appa.isRemoved() && p.getVehicle() == null && Appa.heeft(p) && Appa.isWeg(p), "kept away: he is gone, but still theirs");
            AppaFluitjeItem.blaas(p, false);
            helper.assertTrue(Appa.roep(p) == null && Appa.geladen(p) == null && Appa.geef(p) == null, "nothing brings him while he is kept away");
            Appa.houdWeg(p, false);
            AppaFluitjeItem.blaas(p, false);
            helper.assertTrue(Appa.geladen(p) != null, "let go: the fluitje brings him again");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = BAAN, batch = "einde2_appa", timeoutTicks = 900)
    public static void einde2AppaVliegtZelf(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 3));
        AppaGuhEntity appa = appaOp(helper, p, new BlockPos(7, 2, 6));
        BlockPos doelBlok = helper.absolutePos(new BlockPos(7, 2, 38));
        Vec3 doel = new Vec3(doelBlok.getX() + 0.5, doelBlok.getY(), doelBlok.getZ() + 0.5);
        boolean[] aangekomen = {false};
        Consumer<ServerPlayer> hoor = wie -> aangekomen[0] |= wie == p;
        Appa.AANGEKOMEN.add(hoor);
        helper.assertTrue(Appa.vlieg(p, doel, false), "the flight starts");
        helper.assertTrue(p.getVehicle() == appa && Appa.vliegt(p) && appa.vliegtZelf() && appa.getControllingPassenger() == null,
                "the player is seated and Appa flies himself");
        double[] hoogste = {0};
        helper.onEachTick(() -> hoogste[0] = Math.max(hoogste[0], appa.getY() - doel.y));
        helper.succeedWhen(() -> {
            helper.assertTrue(aangekomen[0], "not landed yet (" + appa.position().distanceTo(doel) + " to go)");
            Appa.AANGEKOMEN.remove(hoor);
            helper.assertTrue(appa.position().distanceTo(doel) < 1.5, "he landed on the goal, with his rider");
            helper.assertTrue(p.getVehicle() == appa && !Appa.vliegt(p) && !appa.vliegtZelf(), "the flight is over, the reins are the player's");
            helper.assertTrue(hoogste[0] > 5, "he went over the wall (" + hoogste[0] + " up)");
            weg(helper, p);
        });
    }

    @GuhTest(template = BAAN, batch = "einde2_appa", timeoutTicks = 900)
    public static void einde2AppaWijstDeWeg(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 3));
        AppaGuhEntity appa = appaOp(helper, p, new BlockPos(7, 2, 6));
        BlockPos doelBlok = helper.absolutePos(new BlockPos(7, 2, 30));
        Vec3 doel = new Vec3(doelBlok.getX() + 0.5, doelBlok.getY(), doelBlok.getZ() + 0.5);
        helper.assertTrue(AppaGuhEntity.pijl(p, doel).equals("↑"), "the goal is straight ahead");
        p.setYRot(90);
        helper.assertTrue(AppaGuhEntity.pijl(p, doel).equals("←"), "looking west, the goal in the south is to the left");
        p.setYRot(0);
        boolean[] aangekomen = {false};
        Consumer<ServerPlayer> hoor = wie -> aangekomen[0] |= wie == p;
        Appa.AANGEKOMEN.add(hoor);
        helper.assertTrue(Appa.vlieg(p, doel, true), "the flight starts");
        helper.assertTrue(!appa.vliegtZelf() && appa.getControllingPassenger() == p, "the player may steer: Appa only points the way");
        Vec3 start = appa.position();
        helper.runAfterDelay(AppaGuhEntity.LOSLATEN - 20, () -> helper.assertTrue(!appa.vliegtZelf() && appa.position().distanceTo(start) < 0.5,
                "he waits for the player's hands"));
        helper.runAfterDelay(AppaGuhEntity.LOSLATEN + 10, () -> helper.assertTrue(appa.vliegtZelf(), "three seconds without a key: he flies himself"));
        helper.succeedWhen(() -> {
            helper.assertTrue(aangekomen[0], "not landed yet");
            Appa.AANGEKOMEN.remove(hoor);
            helper.assertTrue(appa.position().distanceTo(doel) < 1.5, "he landed on the goal");
            Appa.stop(p);
            weg(helper, p);
        });
    }

    // =====================================================================================================================
    // the Momo-guh
    // =====================================================================================================================

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 400)
    public static void einde2AppaMomoEenPerSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        ServerPlayer ander = speler(helper, new BlockPos(3, 2, 3));
        helper.assertTrue(!Momo.heeft(p) && Momo.roep(p) == null, "nobody has a Momo before the grant");
        MomoGuhEntity momo = Momo.geef(p);
        helper.assertTrue(momo != null && Momo.heeft(p) && momo.isEigenaar(p) && Momo.LOS.equals(Momo.plek(p)), "the grant: he comes fluttering");
        helper.assertTrue(gezien(p, GuhVariant.MOMO_GUH) && !Momo.heeft(ander), "his page; one per player");
        helper.succeedWhen(() -> {
            helper.assertTrue(Momo.opSchouder(p) && momo.isRemoved(), "he hops onto the shoulder (the entity leaves the world)");
            // a second grant: the same Momo hops off and on again
            MomoGuhEntity weer = Momo.geef(p);
            helper.assertTrue(weer != null && weer != momo && Momo.heeft(p) && Momo.geladen(p) == weer, "a second grant: still one Momo");
            long momos = helper.getLevel().getEntitiesOfClass(MomoGuhEntity.class, helper.getBounds().inflate(8)).stream()
                    .filter(m -> m.isEigenaar(p) && !m.isRemoved()).count();
            helper.assertTrue(momos == 1, "one Momo in the world (" + momos + ")");
            Momo.opSchouder(p, weer);
            weg(helper, p, ander);
        });
    }

    private static ItemEntity leg(GameTestHelper helper, BlockPos at, ItemStack stack) {
        BlockPos abs = helper.absolutePos(at);
        ItemEntity item = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, stack);
        item.setNoPickUpDelay();
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 200)
    public static void einde2AppaMomoRaapt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        MomoGuhEntity momo = Momo.geef(p);
        Momo.opSchouder(p, momo);
        helper.assertTrue(Momo.opSchouder(p), "on the shoulder");
        ItemEntity dichtbij = leg(helper, new BlockPos(8, 2, 12), new ItemStack(Items.DIAMOND, 2));
        ItemEntity ver = leg(helper, new BlockPos(8, 2, 15), new ItemStack(Items.EMERALD));
        ItemEntity weggegooid = leg(helper, new BlockPos(6, 2, 8), new ItemStack(Items.DIRT));
        weggegooid.setThrower(p);
        ItemEntity doodsdrop = leg(helper, new BlockPos(10, 2, 8), new ItemStack(Items.GOLD_INGOT));
        doodsdrop.getPersistentData().putString(Momo.DOOD_VAN, UUID.randomUUID().toString());
        doodsdrop.getPersistentData().putLong(Momo.DOOD_TIJD, level.getGameTime());
        ItemEntity oudeDrop = leg(helper, new BlockPos(10, 2, 10), new ItemStack(Items.IRON_INGOT));
        oudeDrop.getPersistentData().putString(Momo.DOOD_VAN, UUID.randomUUID().toString());
        oudeDrop.getPersistentData().putLong(Momo.DOOD_TIJD, level.getGameTime() - 200);
        ItemEntity wacht = leg(helper, new BlockPos(8, 2, 6), new ItemStack(Items.STICK));
        wacht.setPickUpDelay(200);
        Momo.raap(p);
        helper.assertTrue(tel(p, Items.DIAMOND) == 2 && dichtbij.isRemoved(), "what lies within six blocks goes into the bag");
        helper.assertTrue(tel(p, Items.IRON_INGOT) == 1, "also what somebody's death dropped long ago");
        helper.assertTrue(tel(p, Items.EMERALD) == 0 && ver.isAlive(), "not what lies further away");
        helper.assertTrue(tel(p, Items.DIRT) == 0 && weggegooid.isAlive(), "not what you threw away yourself");
        helper.assertTrue(tel(p, Items.GOLD_INGOT) == 0 && doodsdrop.isAlive(), "not another player's fresh death drop");
        helper.assertTrue(tel(p, Items.STICK) == 0 && wacht.isAlive(), "not what can't be picked up yet");
        // a full bag: he leaves it
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).isEmpty()) {
                p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            }
        }
        ItemEntity teveel = leg(helper, new BlockPos(8, 2, 11), new ItemStack(Items.AMETHYST_SHARD));
        Momo.raap(p);
        helper.assertTrue(teveel.isAlive() && tel(p, Items.AMETHYST_SHARD) == 0, "a full bag: he leaves it on the ground");
        // at home he picks nothing up
        p.getInventory().clearContent();
        Momo.zetThuis(p, true);
        Momo.spelerTick(p);
        helper.assertTrue(teveel.isAlive() && Momo.THUIS.equals(Momo.plek(p)), "at home he picks nothing up");
        for (ItemEntity item : List.of(ver, weggegooid, doodsdrop, wacht, teveel)) {
            item.discard();
        }
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 200)
    public static void einde2AppaMomoKnabbelKomtTerug(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        MomoGuhEntity momo = Momo.geef(p);
        Momo.opSchouder(p, momo);
        var knabbel = ModItems.KAAS_KNABBELS.get();
        helper.assertTrue(!Momo.jat(p), "no knabbel in the bag: nothing to borrow");
        p.getInventory().add(new ItemStack(knabbel, 3));
        helper.assertTrue(Momo.jat(p) && tel(p, knabbel) == 2 && Momo.geleend(p) == 1, "he takes exactly one knabbel");
        for (int i = 0; i < Momo.TERUG / Momo.STAP + 1; i++) {
            Momo.spelerTick(p);
        }
        helper.assertTrue(tel(p, knabbel) == 3 && Momo.geleend(p) == 0, "...and he gives it back");
        // sent home while he has one: it comes back at once
        helper.assertTrue(Momo.jat(p) && tel(p, knabbel) == 2, "borrowed again");
        Momo.zetThuis(p, true);
        helper.assertTrue(tel(p, knabbel) == 3 && Momo.geleend(p) == 0, "sent home: the knabbel is back at once");
        // his clock: about once in twenty minutes on the shoulder
        Momo.opSchouder(p, null);
        GuhQuests.saved(p).getCompoundOrEmpty(Momo.KEY).putInt("Jat", Momo.STAP);
        Momo.spelerTick(p);
        helper.assertTrue(tel(p, knabbel) == 2 && Momo.geleend(p) == 1, "when his clock runs out he borrows one by himself");
        int wacht = GuhQuests.saved(p).getCompoundOrEmpty(Momo.KEY).getIntOr("Jat", 0);
        helper.assertTrue(wacht >= Momo.JAT_MIN && wacht < Momo.JAT_MAX, "and the next time is 15 to 25 minutes away (" + wacht + ")");
        Momo.geefTerug(p);
        helper.assertTrue(tel(p, knabbel) == 3, "nothing is ever lost");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_appa", timeoutTicks = 400)
    public static void einde2AppaMomoThuis(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        Momo.opSchouder(p, Momo.geef(p));
        BlockPos abs = helper.absolutePos(new BlockPos(4, 2, 4));
        Vec3 thuis = new Vec3(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        Momo.zetThuis(p, thuis);
        MomoGuhEntity zit = Momo.geladen(p);
        helper.assertTrue(zit != null && zit.isThuis() && Momo.THUIS.equals(Momo.plek(p)) && !Momo.opSchouder(p), "he sits at home");
        boolean[] geroepen = {false};
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(!zit.isRemoved() && zit.position().distanceTo(thuis) < 0.6 && Momo.THUIS.equals(Momo.plek(p)),
                    "and he stays where he was put");
            // sneak + fluitje: he comes back to the shoulder
            AppaFluitjeItem.blaas(p, true);
            helper.assertTrue(Momo.LOS.equals(Momo.plek(p)) && !zit.isThuis(), "called: on his way");
            geroepen[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(geroepen[0] && Momo.opSchouder(p) && zit.isRemoved(), "not back on the shoulder yet");
            weg(helper, p);
        });
    }
}
