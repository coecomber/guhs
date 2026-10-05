package nl.juiced.guhs.feature.sausdieren;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of bbq2 (sausdieren). Templates: sausdieren_test_kamer (24 x 8 x 16: a floor of houtskoolsteen with a walled
 * tub of kaasfrituursaus, template x 3..10, z 4..11, one block above the floor) and sausdieren_test_plein (26 x 6 x 18, a
 * bare floor: the test lap is laid out around a Verzorger-guh that sits in no stable).
 * <ul>
 *   <li>the Sausloper stands on the sauce without burning, is cold on bare rock and warm on glowing coal;</li>
 *   <li>wild ones can only be tamed after the stable's questline; saddle, riding, the stick steers, it stays when told,
 *       nothing hurts it, its rider is kept from burning;</li>
 *   <li>the whole questline for two players at one Verzorger-guh (one finishes, the other is untouched), the lap's gates
 *       in order, the rewards once;</li>
 *   <li>a stable resident keeps a home and is nobody's; a loaner without its player leaves;</li>
 *   <li>the Sausblubje splits when hugged or fed, leaves blubroom, grows back, goes into a jar and out again;</li>
 *   <li>blubroom brews the Stuiterdrankje, the effect turns a fall into a bounce;</li>
 *   <li>the stable's template and structure, the spawns, the Guhdex pages.</li>
 * </ul>
 */
public class SausdierenGameTests {
    private static final String KAMER = "sausdieren_test_kamer", PLEIN = "sausdieren_test_plein", BATCH = "sausdieren";
    /** In the tub (helper coordinates: the sauce is at y 2). */
    private static final BlockPos IN_BAK = new BlockPos(6, 3, 7);
    private static final BlockPos DROOG = new BlockPos(17, 2, 7);
    private static final Verhaallijn LIJN = SausdierenFeature.LIJN;

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Proefrit.stop(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void inHand(ServerPlayer p, Item item, int n) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, n));
    }

    private static Item scheutjes() {
        return BarbecuetherFeature.PINDASCHEUTJES.get().asItem();
    }

    private static GuhNpcEntity verzorger(GameTestHelper helper, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.VERZORGERGUH);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        npc.setPersistenceRequired();
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    // =================================================================================================================
    // the Sausloper
    // =================================================================================================================

    /** On the sauce it stands (half sunk, like a strider), never burns and is warm; on bare rock it is cold and slow; glowing coal warms it. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void sausdierenLoperOpDeSaus(GameTestHelper helper) {
        SausloperEntity inBak = helper.spawn(SausdierenFeature.SAUSLOPER.get(), IN_BAK);
        SausloperEntity droog = helper.spawn(SausdierenFeature.SAUSLOPER.get(), DROOG);
        helper.setBlock(new BlockPos(20, 1, 12), BarbecuetherFeature.GLOEIKOOL.get());
        SausloperEntity opKool = helper.spawn(SausdierenFeature.SAUSLOPER.get(), new BlockPos(20, 2, 12));
        for (SausloperEntity l : List.of(inBak, droog, opKool)) {
            l.stilVoorTest();   // (they stay where they are put)
        }
        double basis = droog.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        double saus = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(inBak.isAlive() && inBak.inSaus(), "the Sausloper is in the sauce and alive");
            helper.assertTrue(!inBak.isOnFire() && inBak.getHealth() == inBak.getMaxHealth(), "the sauce does not burn it");
            helper.assertTrue(Math.abs(inBak.getY() - (saus + 0.5)) < 0.15, "it stands half sunk on the sauce, not under it: y " + (inBak.getY() - saus));
            helper.assertTrue(!inBak.isKoud(), "in the sauce it is warm");
            helper.assertTrue(droog.isKoud(), "on bare rock it is cold");
            helper.assertTrue(droog.getAttributeValue(Attributes.MOVEMENT_SPEED) < basis * 0.7, "cold makes it slow");
            helper.assertTrue(!opKool.isKoud() && Math.abs(opKool.getAttributeValue(Attributes.MOVEMENT_SPEED) - basis) < 1e-6, "glowing coal keeps it warm");
            helper.assertTrue(inBak.canStandOnFluid(BarbecuetherFeature.KAASFRITUURSAUS.get().defaultFluidState())
                    && !inBak.canStandOnFluid(net.minecraft.world.level.material.Fluids.WATER.defaultFluidState()), "sauce is ground to it, water is not");
            helper.succeed();
        });
    }

    /** Wild ones: not before the stable's questline, then with pindascheutjes; a saddle, riding, the stick, staying, never hurt. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void sausdierenTemmenEnRijden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(16, 2, 7));
        ServerPlayer ander = speler(helper, new BlockPos(16, 2, 9));
        SausloperEntity loper = helper.spawn(SausdierenFeature.SAUSLOPER.get(), DROOG);
        try {
            helper.assertTrue(SausloperEntity.isVoer(new ItemStack(scheutjes())) && !SausloperEntity.isVoer(new ItemStack(Items.WHEAT)), "it eats pindascheutjes");
            helper.assertTrue(SausloperEntity.isLokker(new ItemStack(SausdierenFeature.PINDASAUS_STOK.get())), "it follows the stick");
            // before the questline: nothing is taken, nothing is tamed
            inHand(p, scheutjes(), 64);
            for (int i = 0; i < 30; i++) {
                loper.mobInteract(p, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(!loper.isTame() && p.getMainHandItem().getCount() == 64, "a stranger can't tame one (and keeps the food)");
            helper.assertTrue(!SausdierenFeature.magTemmen(p), "not allowed yet");
            // a saddle does not go on a wild one
            inHand(p, Items.SADDLE, 1);
            loper.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(!loper.isSaddled() && !loper.isEquippableInSlot(new ItemStack(Items.SADDLE), EquipmentSlot.SADDLE), "no saddle on a wild one");
            // after the questline
            LIJN.zet(p, LIJN.stappen());
            helper.assertTrue(SausdierenFeature.magTemmen(p) && !SausdierenFeature.magTemmen(ander), "allowed after the questline, per player");
            inHand(p, scheutjes(), 64);
            for (int i = 0; i < 120 && !loper.isTame(); i++) {
                if (p.getMainHandItem().isEmpty()) {
                    inHand(p, scheutjes(), 64);
                }
                loper.mobInteract(p, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(loper.isTame() && loper.isOwnedBy(p) && loper.isPersistenceRequired(), "tamed with pindascheutjes, and it stays");
            // the saddle, getting on, the stick
            inHand(p, Items.SADDLE, 1);
            loper.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(loper.isSaddled() && p.getMainHandItem().isEmpty(), "the saddle goes on");
            // its owner tells it to stay and to walk again (sneak + click)
            p.setShiftKeyDown(true);
            loper.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(loper.isOrderedToSit(), "it stays");
            loper.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(!loper.isOrderedToSit(), "it walks again");
            p.setShiftKeyDown(false);
            loper.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(p.getVehicle() == loper, "right-click: the player rides it");
            helper.assertTrue(loper.getControllingPassenger() == null, "without the stick it goes its own way");
            inHand(p, SausdierenFeature.PINDASAUS_STOK.get(), 1);
            helper.assertTrue(loper.getControllingPassenger() == p, "with the stick the rider steers");
            helper.assertTrue(loper.boost() && !loper.boost(), "the stick makes it sprint (once at a time)");
            // nothing hurts it
            float voor = loper.getHealth();
            helper.assertTrue(!loper.hurtServer(level, level.damageSources().playerAttack(ander), 8f) && !loper.hurtServer(level, level.damageSources().lava(), 8f)
                    && loper.getHealth() == voor, "it can't be hurt");
        } catch (RuntimeException | Error e) {
            weg(helper, p, ander);
            throw e;
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(p.hasEffect(MobEffects.FIRE_RESISTANCE), "its rider is kept from burning");
            weg(helper, p, ander);
        });
    }

    /** A resident of the stable keeps a home, eats from everybody and is nobody's; a loaner whose player is gone leaves. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void sausdierenBewonerEnLeenloper(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(16, 2, 5));
        BlockPos abs = helper.absolutePos(new BlockPos(17, 2, 9));
        SausloperEntity bewoner = SausloperEntity.bewoner(level, Vec3.atBottomCenterOf(abs));
        bewoner.getPersistentData().putString(Bezetting.TAG, Stal.BEWONER + "0@test");
        level.addFreshEntity(bewoner);
        SausloperEntity leen = helper.spawn(SausdierenFeature.SAUSLOPER.get(), new BlockPos(20, 2, 4));
        leen.zetLeen(UUID.randomUUID());   // (a player who is not here)
        try {
            helper.assertTrue(bewoner.isBewoner() && bewoner.isPersistenceRequired() && !leen.isBewoner(), "a resident is a Bezetting inhabitant");
            helper.assertTrue(!leen.shouldBeSaved() && bewoner.shouldBeSaved(), "a loaner is never saved");
            LIJN.zet(p, LIJN.stappen());
            inHand(p, scheutjes(), 64);
            for (int i = 0; i < 40; i++) {
                bewoner.mobInteract(p, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(!bewoner.isTame() && bewoner.getOwnerReference() == null, "a resident is never tamed, by anybody");
            helper.assertTrue(p.getMainHandItem().getCount() == 24, "it does eat what it is given");
            inHand(p, Items.SADDLE, 1);
            bewoner.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(!bewoner.isSaddled() && p.getVehicle() == null && !bewoner.canBeLeashed(), "no saddle, no riding, no lead on a resident");
            helper.assertTrue(!bewoner.isEquippableInSlot(new ItemStack(Items.SADDLE), EquipmentSlot.SADDLE)
                    && leen.isEquippableInSlot(new ItemStack(Items.SADDLE), EquipmentSlot.SADDLE), "a saddle fits a loaner, not a resident");
        } catch (RuntimeException | Error e) {
            weg(helper, p);
            throw e;
        }
        helper.runAfterDelay(100, () -> {
            try {
                helper.assertTrue(bewoner.isAlive() && bewoner.hasHome() && bewoner.getHomePosition().distManhattan(abs) <= 3, "the resident has its home where it stood");
                helper.assertTrue(leen.isRemoved(), "the loaner whose player is gone has left");
                // strayed far (pushed, lured away and left): it is put back
                bewoner.snapTo(abs.getX() - 14.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
                bewoner.setHomeTo(abs.offset(0, 0, SausloperEntity.THUIS_TE_VER + 20), 3);
            } finally {
                weg(helper, p);
            }
            BlockPos thuis = bewoner.getHomePosition();
            helper.succeedWhen(() -> helper.assertTrue(bewoner.blockPosition().distManhattan(thuis) <= 2, "a resident far from home is put back"));
        });
    }

    // =================================================================================================================
    // the questline of the stable
    // =================================================================================================================

    /**
     * Two players at one Verzorger-guh: one does the whole questline (the stick, luring, feeding, the lap through the four
     * gates in order, the saddle), the other one's story does not move, and the stable's Sausloper stays nobody's.
     */
    @GuhTest(template = PLEIN, batch = BATCH, timeoutTicks = 400)
    public static void sausdierenQuestlijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(4, 2, 9));
        ServerPlayer b = speler(helper, new BlockPos(4, 2, 11));
        GuhNpcEntity npc = verzorger(helper, new BlockPos(3, 2, 9));
        NpcRole rol = NpcRollen.van(npc);
        BlockPos bij = helper.absolutePos(new BlockPos(6, 2, 9));
        SausloperEntity bewoner = SausloperEntity.bewoner(level, Vec3.atBottomCenterOf(bij));
        bewoner.getPersistentData().putString(Bezetting.TAG, Stal.BEWONER + "1@test");
        level.addFreshEntity(bewoner);
        Item stok = SausdierenFeature.PINDASAUS_STOK.get();
        try {
            helper.assertTrue(rol instanceof VerzorgerRol, "the Verzorger-guh has his role");
            helper.assertTrue(LIJN.stap(a) == 0 && LIJN.stappen() == 5 && !LIJN.begonnen(a), "five steps, not begun");
            // 0: talk, say yes: the stick
            rol.talk(npc, a);
            helper.assertTrue(LIJN.begonnen(a) && LIJN.stap(a) == 0 && GuhQuests.count(a, stok) == 0, "talking opens the screen, nothing yet");
            rol.antwoord(npc, a, -1);
            helper.assertTrue(LIJN.stap(a) == 0, "closing the screen is not a yes");
            rol.antwoord(npc, a, 1);
            helper.assertTrue(LIJN.stap(a) == 1 && GuhQuests.count(a, stok) == 1, "yes: step 1 and the stick");
            rol.antwoord(npc, a, 1);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, stok) == 1, "no second stick while you have one");
            GuhQuests.take(a, stok, 1);
            rol.talk(npc, a);
            helper.assertTrue(GuhQuests.count(a, stok) == 1, "lost it: he gives another");
            helper.assertTrue(!SausdierenEvents.lokCheck(a), "nothing follows yet (the stick is not in the hand)");
        } catch (RuntimeException | Error e) {
            weg(helper, a, b);
            throw e;
        }
        // 1: the Sausloper follows the stick in A's hand, next to the Verzorger-guh
        inHand(a, stok, 1);
        long[] gelokt = {-1};
        helper.onEachTick(() -> {
            if (gelokt[0] >= 0) {
                return;
            }
            if (bewoner.lokker() == a && SausdierenEvents.lokCheck(a)) {
                gelokt[0] = level.getGameTime();
                try {
                    rest(helper, a, b, npc, rol, bewoner);
                } finally {
                    weg(helper, a, b);
                }
                helper.succeed();
            }
        });
    }

    private static void rest(GameTestHelper helper, ServerPlayer a, ServerPlayer b, GuhNpcEntity npc, NpcRole rol, SausloperEntity bewoner) {
        ServerLevel level = helper.getLevel();
        Item stok = SausdierenFeature.PINDASAUS_STOK.get();
        helper.assertTrue(LIJN.stap(a) == 2 && LIJN.stap(b) == 0, "lured: step 2 for A only");
        // 2: he gives three pindascheutjes once; feeding the stable's Sausloper three times
        a.getInventory().clearContent();
        rol.talk(npc, a);
        helper.assertTrue(GuhQuests.count(a, scheutjes()) == SausdierenFeature.VOER_NODIG, "he gives the first three pindascheutjes");
        rol.talk(npc, a);
        helper.assertTrue(GuhQuests.count(a, scheutjes()) == SausdierenFeature.VOER_NODIG, "only once");
        a.getInventory().clearContent();
        inHand(b, scheutjes(), 5);
        bewoner.mobInteract(b, InteractionHand.MAIN_HAND);
        helper.assertTrue(LIJN.stap(b) == 0 && SausdierenFeature.voer(b) == 0 && b.getMainHandItem().getCount() == 4, "B feeds it too: eaten, but B's story does not move");
        inHand(a, scheutjes(), 5);
        bewoner.mobInteract(a, InteractionHand.MAIN_HAND);
        bewoner.mobInteract(a, InteractionHand.MAIN_HAND);
        helper.assertTrue(LIJN.stap(a) == 2 && SausdierenFeature.voer(a) == 2, "two of three");
        bewoner.mobInteract(a, InteractionHand.MAIN_HAND);
        helper.assertTrue(LIJN.stap(a) == 3 && !bewoner.isTame(), "three: it trusts A (and is still nobody's)");
        // 3: the lap. The Verzorger-guh whistles a saddled loaner for A alone
        a.getInventory().clearContent();
        rol.talk(npc, a);
        rol.antwoord(npc, a, 1);
        SausloperEntity loper = Proefrit.loper(a);
        Stal.Baan baan = Proefrit.baan(a);
        helper.assertTrue(loper != null && baan != null && loper.isSaddled() && a.getUUID().equals(loper.leen()), "a saddled Sausloper of A's own");
        helper.assertTrue(GuhQuests.count(a, stok) == 1, "and a stick again when A had none");
        helper.assertTrue(loper.distanceToSqr(baan.start()) < 1 && baan.poorten().size() == 4, "at the start of a lap of four gates");
        helper.assertTrue(Proefrit.bezig(a.getUUID()) && Proefrit.poort(a) == 0 && !Proefrit.bezig(b.getUUID()), "A is on a lap, B is not");
        inHand(b, Items.AIR, 1);
        loper.mobInteract(b, InteractionHand.MAIN_HAND);
        helper.assertTrue(b.getVehicle() == null, "B can't take A's Sausloper");
        // not riding: gates don't count
        loper.snapTo(baan.poorten().get(0).x, baan.poorten().get(0).y, baan.poorten().get(0).z, 0, 0);
        Proefrit.tick(a);
        helper.assertTrue(Proefrit.poort(a) == 0, "walking it to a gate is not riding through");
        inHand(a, stok, 1);
        loper.mobInteract(a, InteractionHand.MAIN_HAND);
        helper.assertTrue(a.getVehicle() == loper && loper.getControllingPassenger() == a, "A rides and steers");
        // a gate out of order does nothing; the right ones in order finish the lap
        zet(loper, baan.poorten().get(2));
        Proefrit.tick(a);
        helper.assertTrue(Proefrit.poort(a) == 0, "the third gate first: nothing");
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(LIJN.stap(a) == 3, "not done before the last gate");
            zet(loper, baan.poorten().get(i).add(1.2, 0, -0.8));
            Proefrit.tick(a);
        }
        helper.assertTrue(LIJN.stap(a) == 4 && Proefrit.poort(a) == -1 && !Proefrit.bezig(a.getUUID()), "four gates in order: step 4");
        // 4: back for the saddle, once
        a.stopRiding();
        a.getInventory().clearContent();
        rol.talk(npc, a);
        helper.assertTrue(LIJN.klaar(a) && GuhQuests.count(a, Items.SADDLE) == 1 && GuhQuests.count(a, scheutjes()) == 4, "done: the saddle and four pindascheutjes");
        helper.assertTrue(SausdierenFeature.magTemmen(a) && !SausdierenFeature.magTemmen(b), "A may tame wild Sauslopers now, B not yet");
        rol.talk(npc, a);
        rol.antwoord(npc, a, 2);
        helper.assertTrue(GuhQuests.count(a, Items.SADDLE) == 1, "no second saddle");
        // afterwards: another lap, timed; the old loaner is gone, a far ride is put back at the start
        UUID oud = loper.getUUID();
        rol.antwoord(npc, a, 1);
        SausloperEntity nieuw = Proefrit.loper(a);
        helper.assertTrue(nieuw != null && !nieuw.getUUID().equals(oud) && level.getEntity(oud) == null, "a new lap: a new loaner, the old one is gone");
        inHand(a, stok, 1);
        nieuw.mobInteract(a, InteractionHand.MAIN_HAND);
        zet(nieuw, baan.poorten().get(0));
        Proefrit.tick(a);
        helper.assertTrue(Proefrit.poort(a) == 1, "the first gate of the second lap");
        zet(nieuw, baan.start().add(Proefrit.TE_VER + 6, 0, 0));
        Proefrit.tick(a);
        helper.assertTrue(Proefrit.poort(a) == 0 && nieuw.distanceToSqr(baan.start()) < 9 && a.getHealth() == a.getMaxHealth(), "strayed far: back to the start, unharmed");
        for (int i = 0; i < 4; i++) {
            zet(nieuw, baan.poorten().get(i));
            Proefrit.tick(a);
        }
        helper.assertTrue(LIJN.teller(a, "record") > 0 && LIJN.stap(b) == 0 && LIJN.teller(b, "record") == 0, "A's best time is kept, B's story never moved");
        a.stopRiding();
    }

    private static void zet(SausloperEntity loper, Vec3 plek) {
        loper.snapTo(plek.x, plek.y, plek.z, loper.getYRot(), 0);
    }

    // =================================================================================================================
    // the Sausblubje
    // =================================================================================================================

    private static List<SausblubjeEntity> blubjes(GameTestHelper helper) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 8, 16);
        return helper.getLevel().getEntitiesOfClass(SausblubjeEntity.class, kamer, Entity::isAlive);
    }

    private static int blubroom(GameTestHelper helper) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 8, 16);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, kamer, i -> i.getItem().is(SausdierenFeature.BLUBROOM.get()))
                .stream().mapToInt(i -> i.getItem().getCount()).sum();
    }

    /** A hug or a knabbel splits a big one and a middle one (blubroom every time); a small one grows back; nothing hurts it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void sausdierenBlubjeSplitstEnGroeit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(16, 2, 7));
        try {
            SausblubjeEntity groot = helper.spawn(SausdierenFeature.SAUSBLUBJE.get(), new BlockPos(18, 2, 7));
            groot.setGrootte(SausblubjeEntity.GROOT);
            helper.assertTrue(groot.getBbWidth() > 1.3f && groot.schaal() > 2f, "a big one is big: " + groot.getBbWidth());
            helper.assertTrue(!groot.hurtServer(level, level.damageSources().playerAttack(p), 9f) && !groot.hurtServer(level, level.damageSources().lava(), 9f)
                    && groot.getHealth() == groot.getMaxHealth(), "it can't be hurt");
            // a hug (empty hand)
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            groot.mobInteract(p, InteractionHand.MAIN_HAND);
            List<SausblubjeEntity> midden = blubjes(helper);
            helper.assertTrue(groot.isRemoved() && midden.size() == 2 && midden.stream().allMatch(b -> b.grootte() == SausblubjeEntity.MIDDEL),
                    "a hugged big one becomes two middle ones: " + midden.size());
            helper.assertTrue(blubroom(helper) == 1, "and leaves one blubroom");
            helper.assertTrue(midden.stream().allMatch(SausblubjeEntity::isPersistenceRequired), "what a player hugged apart stays");
            // right away again: they need a moment (one long click doesn't split the family)
            midden.get(0).mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(blubjes(helper).size() == 2, "not twice in one click");
            // a knabbel splits a middle one
            midden.get(0).uitgerust();
            inHand(p, ModItems.KAAS_KNABBELS.get(), 10);
            midden.get(0).mobInteract(p, InteractionHand.MAIN_HAND);
            List<SausblubjeEntity> alle = blubjes(helper);
            helper.assertTrue(alle.size() == 3 && p.getMainHandItem().getCount() == 9 && blubroom(helper) == 2, "a fed middle one becomes two small ones, blubroom again");
            SausblubjeEntity klein = alle.stream().filter(b -> b.grootte() == SausblubjeEntity.KLEIN).findFirst().orElseThrow();
            // a small one: a hug is only a hug, three knabbels make it grow
            klein.uitgerust();
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            klein.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(klein.isAlive() && blubjes(helper).size() == 3 && blubroom(helper) == 2, "a small one does not split");
            inHand(p, ModItems.KAAS_KNABBELS.get(), 10);
            for (int i = 0; i < SausblubjeEntity.GROEI_VOER; i++) {
                helper.assertTrue(klein.grootte() == SausblubjeEntity.KLEIN, "still small after " + i);
                klein.uitgerust();
                klein.mobInteract(p, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(klein.grootte() == SausblubjeEntity.MIDDEL && klein.gevoerd() == 0 && p.getMainHandItem().getCount() == 7, "three knabbels: a middle one again");
            helper.assertTrue(blubroom(helper) == 2, "growing gives nothing: only splitting does");
            helper.assertTrue(!SausblubjeEntity.isVoer(new ItemStack(Items.WHEAT)), "it only eats knabbels");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** A small one goes into a glass bottle (a middle one does not fit) and comes out again as a small one that stays. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void sausdierenBlubjeInPotje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(16, 2, 7));
        try {
            SausblubjeEntity midden = helper.spawn(SausdierenFeature.SAUSBLUBJE.get(), new BlockPos(18, 2, 5));
            midden.setGrootte(SausblubjeEntity.MIDDEL);
            SausblubjeEntity klein = helper.spawn(SausdierenFeature.SAUSBLUBJE.get(), new BlockPos(18, 2, 9));
            klein.setGrootte(SausblubjeEntity.KLEIN);
            inHand(p, Items.GLASS_BOTTLE, 3);
            midden.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(midden.isAlive() && p.getMainHandItem().is(Items.GLASS_BOTTLE) && p.getMainHandItem().getCount() == 3, "a middle one does not fit");
            klein.mobInteract(p, InteractionHand.MAIN_HAND);
            Item potje = SausdierenFeature.SAUSBLUBJE_POTJE.get();
            helper.assertTrue(klein.isRemoved() && GuhQuests.count(p, potje) == 1 && GuhQuests.count(p, Items.GLASS_BOTTLE) == 2, "a small one hops into one bottle");
            helper.assertTrue(new ItemStack(potje).getMaxStackSize() == 16, "jars stack");
            // out again
            int voor = blubjes(helper).size();
            SausblubjeEntity vrij = SausblubjePotjeItem.laatVrij(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(20, 2, 9))), new ItemStack(potje));
            helper.assertTrue(vrij != null && vrij.grootte() == SausblubjeEntity.KLEIN && vrij.isPersistenceRequired() && blubjes(helper).size() == voor + 1,
                    "let out: a small Sausblubje that stays");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** It bounces over the sauce as over ground and never burns. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void sausdierenBlubjeOpDeSaus(GameTestHelper helper) {
        SausblubjeEntity blubje = helper.spawn(SausdierenFeature.SAUSBLUBJE.get(), IN_BAK);
        blubje.setGrootte(SausblubjeEntity.MIDDEL);
        blubje.setPersistenceRequired();
        double saus = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(blubje.isAlive() && !blubje.isOnFire() && blubje.getHealth() == blubje.getMaxHealth(), "the sauce does not burn it");
            helper.assertTrue(blubje.getY() >= saus + 0.4, "it is on the sauce, not under it: y " + (blubje.getY() - saus));
            helper.succeed();
        });
    }

    // =================================================================================================================
    // blubroom, the Stuiterdrankje
    // =================================================================================================================

    /** Blubroom brews the Stuiterdrankje; with the effect a fall does not hurt and throws you back up (not when sneaking). */
    @GuhTest(template = PLEIN, batch = BATCH)
    public static void sausdierenStuiterdrankje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 9));
        try {
            helper.assertTrue(Brouwsel.BLUBROOM.heeftDrankje() && Brouwsel.forIngredient(new ItemStack(SausdierenFeature.BLUBROOM.get())) == Brouwsel.BLUBROOM,
                    "blubroom is the ingredient of the Brouwsel BLUBROOM");
            helper.assertTrue(Brouwsel.BLUBROOM.drankje().is(SausdierenFeature.STUITERDRANKJE.get()), "it is bottled as the Stuiterdrankje");
            helper.assertTrue(Brouwsel.BLUBROOM.effects().stream().anyMatch(e -> e.getEffect().is(SausdierenFeature.STUITER)), "which gives Stuiterblub");
            // without the effect a fall hurts (a pig: mock players shrug off a fall)
            net.minecraft.world.entity.animal.pig.Pig varken = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(14, 2, 9));
            varken.causeFallDamage(10.0, 1f, level.damageSources().fall());
            helper.assertTrue(varken.getHealth() < varken.getMaxHealth(), "a fall of ten blocks hurts");
            varken.setHealth(varken.getMaxHealth());
            varken.invulnerableTime = 0;
            // with it: no damage, and up again
            varken.addEffect(new MobEffectInstance(SausdierenFeature.STUITER, 600, 0));
            varken.setDeltaMovement(Vec3.ZERO);
            varken.causeFallDamage(10.0, 1f, level.damageSources().fall());
            helper.assertTrue(varken.getHealth() == varken.getMaxHealth(), "with Stuiterblub the same fall does not hurt");
            helper.assertTrue(varken.getDeltaMovement().y > 0.8, "and bounces: " + varken.getDeltaMovement().y);
            varken.discard();
            // the same for a player
            p.addEffect(new MobEffectInstance(SausdierenFeature.STUITER, 600, 0));
            p.setDeltaMovement(Vec3.ZERO);
            p.causeFallDamage(10.0, 1f, level.damageSources().fall());
            helper.assertTrue(p.getHealth() == p.getMaxHealth() && p.getDeltaMovement().y > 0.8, "a player bounces too: " + p.getDeltaMovement().y);
            helper.assertTrue(SausdierenEvents.stuiterSnelheid(3) < SausdierenEvents.stuiterSnelheid(12) && SausdierenEvents.stuiterSnelheid(200) <= 1.1,
                    "a higher fall bounces higher, up to a limit");
            // a little hop does nothing, sneaking lands
            p.setDeltaMovement(Vec3.ZERO);
            p.causeFallDamage(1.2, 1f, level.damageSources().fall());
            helper.assertTrue(p.getDeltaMovement().y == 0, "a small step is not a bounce");
            p.setShiftKeyDown(true);
            p.causeFallDamage(10.0, 1f, level.damageSources().fall());
            helper.assertTrue(p.getDeltaMovement().y == 0 && p.getHealth() == p.getMaxHealth(), "sneaking: you land, unhurt");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the stable's template and structure, spawns, Guhdex
    // =================================================================================================================

    /** The template is there with sauce under the lap, the structure and its guaranteed copy exist, it is in the Superkompas. */
    @GuhTest(template = PLEIN, batch = BATCH)
    public static void sausdierenStalBestaat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(Stal.STRUCTUUR));
        helper.assertTrue(template.isPresent(), "the template guhs:sausloper_stal");
        StructureTemplate t = template.get();
        helper.assertTrue(t.getSize().getX() <= 48 && t.getSize().getZ() <= 48 && t.getSize().getY() > Stal.G + 8, "its size: " + t.getSize());
        Set<BlockPos> saus = new HashSet<>();
        for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get())) {
            saus.add(info.pos());
        }
        helper.assertTrue(saus.size() > 150, "a basin of sauce: " + saus.size());
        helper.assertTrue(saus.contains(Stal.START.below()), "the lap starts on the sauce");
        for (BlockPos poort : Stal.POORTEN) {
            helper.assertTrue(saus.contains(poort.below()), "gate " + poort + " is over the sauce");
        }
        helper.assertTrue(saus.contains(Stal.BEWONERS.get(0).below()) && saus.contains(Stal.BEWONERS.get(1).below()) && !saus.contains(Stal.BEWONERS.get(2).below()),
                "two residents in the basin, one in a stall");
        helper.assertTrue(!saus.contains(Stal.NPC.below()) && Stal.NPC.getY() == Stal.G + 1, "the Verzorger-guh sits on the yard");
        helper.assertTrue(Kopieen.structuur(level, Stal.STRUCTUUR) != null, "the structure guhs:sausloper_stal");
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        helper.assertTrue(sets.getValue(Guhs.id(Stal.STRUCTUUR)) != null && sets.getValue(Guhs.id(Stal.STRUCTUUR + "_gegarandeerd")) != null,
                "its structure set and its guaranteed copy");
        helper.assertTrue(SuperkompasItem.categoryOf(Stal.STRUCTUUR) >= 0, "it is in the Superkompas");
        // a Verzorger-guh in no stable: the lap is laid out around him, like the template's
        GuhNpcEntity npc = verzorger(helper, new BlockPos(3, 2, 9));
        Stal.Baan baan = Stal.baan(level, npc);
        Vec3 n = Vec3.atBottomCenterOf(npc.blockPosition());
        helper.assertTrue(baan.poorten().size() == 4 && baan.start().subtract(n).equals(new Vec3(Stal.START.getX() - Stal.NPC.getX(), 0, Stal.START.getZ() - Stal.NPC.getZ())),
                "the lap of a loose Verzorger-guh: " + baan.start().subtract(n));
        helper.assertTrue(Stal.verzorger(level, npc.blockPosition().offset(3, 0, 0), 8) == npc && Stal.verzorger(level, npc.blockPosition().offset(60, 0, 0), 8) == null,
                "the nearest Verzorger-guh is found");
        npc.discard();
        helper.succeed();
    }

    /** Where they spawn (the Sausloper in the sauce with air above, both in the biomes of the Barbecuether), and their Guhdex pages. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void sausdierenSpawnsEnGuhdex(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos saus = helper.absolutePos(new BlockPos(6, 2, 7)), droog = helper.absolutePos(DROOG);
        var type = SausdierenFeature.SAUSLOPER.get();
        helper.assertTrue(SausdierenFeature.IN_SAUS.isSpawnPositionOk(level, saus, type) && !SausdierenFeature.IN_SAUS.isSpawnPositionOk(level, droog, type),
                "a Sausloper's spawn spot is in the sauce");
        helper.assertTrue(SausloperEntity.magSpawnen(type, level, EntitySpawnReason.SPAWN_ITEM_USE, saus, level.getRandom()), "with open air above the sauce");
        helper.setBlock(new BlockPos(6, 3, 7), BarbecuetherFeature.HOUTSKOOLSTEEN.get());
        helper.assertTrue(!SausloperEntity.magSpawnen(type, level, EntitySpawnReason.SPAWN_ITEM_USE, saus, level.getRandom()), "not under rock");
        helper.assertTrue(SausblubjeEntity.magSpawnen(SausdierenFeature.SAUSBLUBJE.get(), level, EntitySpawnReason.SPAWN_ITEM_USE, droog, level.getRandom()),
                "a spawn egg always works");
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (String naam : List.of("houtskoolvlakte", "asdal", "satebos", "worstenwoud", "rookdelta")) {
            Biome biome = biomes.getValue(ResourceKey.create(Registries.BIOME, Guhs.id(naam)));
            helper.assertTrue(biome != null, "the biome " + naam);
            var wezens = biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
            helper.assertTrue(wezens.stream().anyMatch(w -> w.value().type() == SausdierenFeature.SAUSLOPER.get())
                    && wezens.stream().anyMatch(w -> w.value().type() == SausdierenFeature.SAUSBLUBJE.get()), "both spawn in " + naam);
        }
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.SAUSLOPER) && GuhDex.isCreaturePage(GuhVariant.SAUSBLUBJE), "their Guhdex pages");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.SAUSLOPER) && !GuhDex.EXTRA.contains(GuhVariant.SAUSBLUBJE), "which count");
        helper.assertTrue(Bescherming.class != null && SausdierenFeature.SAUSLOPER.get().fireImmune() && SausdierenFeature.SAUSBLUBJE.get().fireImmune(),
                "both are fire immune");
        helper.assertTrue(SausdierenFeature.SAUSLOPER.get().getCategory() == MobCategory.CREATURE, "creatures, not monsters");
        helper.assertTrue(new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()).getItem().getDescriptionId().equals("item.guhs.sausblubje_potje"), "the fixed ids");
        helper.assertTrue(SausdierenFeature.BLUBROOM.getId().equals(Guhs.id("blubroom")) && SausdierenFeature.SAUSBLUBJE.getId().equals(Guhs.id("sausblubje"))
                && SausdierenFeature.SAUSLOPER.getId().equals(Guhs.id("sausloper")), "blubroom, sausblubje, sausloper");
        helper.succeed();
    }
}
