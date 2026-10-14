package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhTime;
import nl.juiced.guhs.world.WildeDieren;

/**
 * Game tests of the zwevers slice: the bounce on a Kruimelkwal (who bounces, the soft landing after it), the jar (catch,
 * lamp, breaking lets the kwal go, silk touch keeps it), the Leegtewalvisguh's per-player flag with two players (sent to,
 * carried by, fed by the one with the flag only; the Guhdex of the other notices nothing), its pearl once a Minecraft day,
 * its level flight and what it steers clear of, the otter's sled state, the schildpad-eendguh's bread and shell, the
 * das-molguh's digging (no block changes), the lemur's hanging, creatures placed by a template (Honk, Slee), the rules of the
 * three top-ups, and the Guhdex pages. Templates einde2_zwevers_test_kamer (a stone floor with ice, dirt, a basin of water
 * and a beam) and einde2_zwevers_test_honk (the creatures as a building places them).
 */
public class E2ZweversGameTests {
    private static final String KAMER = "einde2_zwevers_test_kamer";
    private static final String HONK = "einde2_zwevers_test_honk";
    private static final String BATCH = "einde2_zwevers";
    /** Spots of the room (helper coordinates; the floor is helper y 1). */
    private static final BlockPos IJS = new BlockPos(3, 2, 1), STEEN = new BlockPos(6, 2, 5), AARDE = new BlockPos(2, 2, 10), BALK = new BlockPos(6, 6, 6),
            WATER = new BlockPos(10, 2, 10);

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
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static boolean heeft(ServerPlayer p, String quest) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + quest));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** A whale high above the room (it never touches the other tests: only players with the flag meet its back). */
    static LeegtewalvisguhEntity walvis(GameTestHelper helper, int hoogte) {
        ServerLevel level = helper.getLevel();
        LeegtewalvisguhEntity w = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        Vec3 plek = helper.absoluteVec(new Vec3(7, hoogte, 7));
        w.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        w.setPersistenceRequired();
        level.addFreshEntity(w);
        return w;
    }

    // =====================================================================================================================
    // Kruimelkwallen
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversStuiter(GameTestHelper helper) {
        KruimelkwalEntity kwal = helper.spawn(E2ZweversFeature.KRUIMELKWAL.get(), STEEN.above(2));
        kwal.setThuis(kwal.position(), 1);
        KruimelkwalEntity ander = helper.spawn(E2ZweversFeature.KRUIMELKWAL.get(), STEEN.above(4));
        ander.setThuis(ander.position(), 1);
        ServerPlayer p = speler(helper, STEEN);
        p.snapTo(kwal.getX(), kwal.getY() + 0.6, kwal.getZ());
        helper.assertFalse(kwal.stuitertOp(p), "hanging still above it: no bounce");
        p.fallDistance = 3;
        helper.assertTrue(kwal.stuitertOp(p), "falling onto its bell: a bounce");
        p.setShiftKeyDown(true);
        helper.assertFalse(kwal.stuitertOp(p), "sneaking: you sink through");
        p.setShiftKeyDown(false);
        p.snapTo(kwal.getX(), kwal.getY(), kwal.getZ());
        helper.assertFalse(kwal.stuitertOp(p), "from the side or from below: no bounce");
        ander.fallDistance = 3;
        helper.assertFalse(kwal.stuitertOp(ander), "a kwal never bounces on a kwal");
        helper.assertTrue(E2ZweversEvents.zachtGeland(100, 100) && E2ZweversEvents.zachtGeland(100 + KruimelkwalEntity.ZACHT_LANDEN, 100)
                && !E2ZweversEvents.zachtGeland(101 + KruimelkwalEntity.ZACHT_LANDEN, 100), "the soft landing lasts ZACHT_LANDEN ticks");
        // for real: the kwal's own tick finds whoever falls onto it
        p.snapTo(kwal.getX(), kwal.getY() + 0.6, kwal.getZ());
        p.fallDistance = 3;
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(p.getDeltaMovement().y > 0.9, "the player is sent up, was " + p.getDeltaMovement().y);
            helper.assertTrue(p.fallDistance == 0, "the fall so far is forgotten");
            helper.assertTrue(heeft(p, "einde2_zwevers_gestuiterd"), "the bounce advancement");
            helper.assertTrue(p.getPersistentData().getLongOr(KruimelkwalEntity.GESTUITERD, -1L) >= 0, "the bounce is remembered on the player");
            float hart = p.getHealth();
            helper.assertFalse(p.causeFallDamage(12, 1f, p.damageSources().fall()), "the landing after a bounce costs nothing");
            helper.assertTrue(p.getHealth() == hart, "no damage");
            helper.assertTrue(p.getPersistentData().getLongOr(KruimelkwalEntity.GESTUITERD, -1L) < 0, "one landing per bounce");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversPot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KruimelkwalEntity kwal = helper.spawn(E2ZweversFeature.KRUIMELKWAL.get(), STEEN.above());
        ServerPlayer p = speler(helper, STEEN.east());
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.assertTrue(kwal.mobInteract(p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "a glass bottle catches it");
        helper.assertTrue(kwal.isRemoved() && p.getMainHandItem().getCount() == 1, "the kwal is gone, one bottle is used");
        helper.assertTrue(p.getInventory().countItem(E2ZweversFeature.KRUIMELKWAL_POT_ITEM.get()) == 1, "the player has the kruimelkwal_pot");
        // the lamp: on the floor and hanging under the beam
        BlockState pot = E2ZweversFeature.KRUIMELKWAL_POT.get().defaultBlockState();
        BlockPos vloer = helper.absolutePos(STEEN), hangt = helper.absolutePos(BALK.below());
        level.setBlockAndUpdate(vloer, pot);
        level.setBlockAndUpdate(hangt, pot.setValue(LanternBlock.HANGING, true));
        helper.assertTrue(level.getBlockState(vloer).getLightEmission(level, vloer) == KruimelkwalPotBlock.LICHT, "the jar gives light 12");
        helper.assertTrue(level.getBlockState(vloer).canSurvive(level, vloer) && level.getBlockState(hangt).canSurvive(level, hangt), "it stands and it hangs");
        AABB kamer = new AABB(vloer).inflate(6);
        // broken by hand: the kwal floats out and stays, the bottle comes back
        Block.dropResources(level.getBlockState(vloer), level, vloer, null, p, ItemStack.EMPTY);
        level.removeBlock(vloer, false);
        List<? extends KruimelkwalEntity> vrij = level.getEntities(E2ZweversFeature.KRUIMELKWAL.get(), kamer, k -> k.isAlive());
        helper.assertTrue(vrij.size() == 1 && vrij.get(0).isPersistenceRequired() && !WildeDieren.isKomEnGa(vrij.get(0)), "breaking the jar lets the kwal go, to stay");
        helper.assertTrue(heeftItem(level, kamer, Items.GLASS_BOTTLE.getDefaultInstance()), "the glass bottle drops");
        // with silk touch: kwal and jar stay together
        ItemStack zijde = new ItemStack(Items.DIAMOND_PICKAXE);
        zijde.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        Block.dropResources(level.getBlockState(hangt), level, hangt, null, p, zijde);
        level.removeBlock(hangt, false);
        helper.assertTrue(level.getEntities(E2ZweversFeature.KRUIMELKWAL.get(), kamer, k -> k.isAlive()).size() == 1, "silk touch: no kwal comes out");
        helper.assertTrue(heeftItem(level, kamer, new ItemStack(E2ZweversFeature.KRUIMELKWAL_POT_ITEM.get())), "silk touch: the whole jar drops");
        weg(helper, p);
        helper.succeed();
    }

    private static boolean heeftItem(ServerLevel level, AABB box, ItemStack wat) {
        return !level.getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(wat.getItem())).isEmpty();
    }

    // =====================================================================================================================
    // the Leegtewalvisguh
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversWalviszicht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, STEEN), b = speler(helper, STEEN.east());
        helper.assertTrue(!Walviszicht.ziet(a) && !Walviszicht.ziet(b), "by default nobody sees the whale");
        LeegtewalvisguhEntity w = walvis(helper, 30);
        helper.assertTrue(!w.broadcastToPlayer(a) && !w.broadcastToPlayer(b) && !w.draagt(a), "without the flag: not sent, not carried");
        Walviszicht.zet(a, true);
        helper.assertTrue(Walviszicht.ziet(a) && !Walviszicht.ziet(b), "the flag is per player");
        helper.assertTrue(GuhQuests.saved(a).getBooleanOr(Walviszicht.SLEUTEL, false), "it is kept in the player's own saved data");
        helper.assertTrue(w.broadcastToPlayer(a) && !w.broadcastToPlayer(b), "the whale is only sent to the player with the flag");
        helper.assertTrue(w.draagt(a) && !w.draagt(b), "its back only carries the player with the flag");
        helper.runAfterDelay(25, () -> {
            double rug = w.rugY();
            // the real collision path, at its middle and far out towards its head and tail (twenty blocks from its own box)
            for (double vooruit : new double[]{0, 20, -18}) {
                Vec3 plek = w.position().add(LeegtewalvisguhEntity.vooruit(w.getYRot()).scale(vooruit));
                AABB voeten = new AABB(plek.x - 0.3, rug - 0.6, plek.z - 0.3, plek.x + 0.3, rug + 0.4, plek.z + 0.3);
                helper.assertTrue(!level.getEntityCollisions(a, voeten).isEmpty(), "the back is solid for the player with the flag (" + vooruit + ")");
                helper.assertTrue(level.getEntityCollisions(b, voeten).isEmpty(), "and air for the other (" + vooruit + ")");
            }
            // for the world it is not there: no search finds a Leegtewalvisguh
            a.snapTo(w.getX(), rug, w.getZ());
            b.snapTo(w.getX(), rug, w.getZ());
            for (Entity e : level.getEntities(b, b.getBoundingBox().inflate(16))) {
                helper.assertTrue(e.getType() != E2ZweversFeature.LEEGTEWALVISGUH.get(), "a search around a player never meets a Leegtewalvisguh");
            }
            GuhDex.seeCreatures(b);
            helper.assertFalse(heeft(b, "seen_leegtewalvisguh"), "the Guhdex of the player without the flag notices nothing");
            helper.assertTrue(w.staatOp(a), "the player stands on its back");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(heeft(a, "seen_leegtewalvisguh"), "the whale fills in its page for the player who sees it");
            helper.assertTrue(heeft(a, "einde2_zwevers_walvisrug"), "and the player on its back gets the advancement");
            helper.assertFalse(heeft(b, "seen_leegtewalvisguh"), "and not for the other, right next to it");
            // feeding: only with the flag
            ItemStack vrucht = new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), 3);
            b.setItemInHand(InteractionHand.MAIN_HAND, vrucht.copy());
            helper.assertTrue(w.klik(b, InteractionHand.MAIN_HAND) == InteractionResult.PASS && b.getMainHandItem().getCount() == 3, "no flag: no feeding");
            helper.assertTrue(((LeegtewalvisguhEntity.Rug) w.getParts()[0]).interact(b, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.PASS,
                    "nor through its back");
            Walviszicht.zet(a, false);
            helper.assertTrue(!Walviszicht.ziet(a) && !w.broadcastToPlayer(a) && !w.draagt(a), "the flag can be taken away again");
            w.discard();
            weg(helper, a, b);
            helper.succeed();
        });
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversWalvisVoeren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, STEEN);
        Walviszicht.zet(a, true);
        LeegtewalvisguhEntity w = walvis(helper, 45);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), 3));
        helper.assertTrue(w.klik(a, InteractionHand.OFF_HAND) == InteractionResult.PASS, "an empty hand does nothing");
        helper.assertTrue(((LeegtewalvisguhEntity.Rug) w.getParts()[2]).interact(a, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.SUCCESS,
                "a knabbelkoorvrucht on its back is eaten");
        helper.assertTrue(a.getMainHandItem().getCount() == 2 && a.getInventory().countItem(E2ZweversFeature.LEEGTEPAREL.get()) == 1, "one fruit, one leegteparel");
        helper.assertTrue(heeft(a, "einde2_zwevers_walvis_gevoerd") && w.isZingend(), "the advancement, and it sings");
        helper.assertTrue(w.klik(a, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "it eats a second one");
        helper.assertTrue(a.getMainHandItem().getCount() == 1 && a.getInventory().countItem(E2ZweversFeature.LEEGTEPAREL.get()) == 1,
                "but there is one pearl per Minecraft day");
        GuhQuests.saved(a).putLong(LeegtewalvisguhEntity.PAREL_DAG, GuhTime.day(level) - 1);
        helper.assertTrue(w.voer(a, a.getMainHandItem()), "the next day: a pearl again");
        helper.assertTrue(a.getInventory().countItem(E2ZweversFeature.LEEGTEPAREL.get()) == 2, "two pearls now");
        w.discard();
        weg(helper, a);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversWalvisVlucht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LeegtewalvisguhEntity w = walvis(helper, 60);
        Vec3 start = w.position();
        // the rules of its top-up
        int ver = Einde2Ids.STRAAL_MIDDEN + 500;
        helper.assertFalse(Leegtewalvissen.magErbij(false, ver, 0, -1), "no flag: no whale ever comes");
        helper.assertTrue(Leegtewalvissen.magErbij(true, ver, 0, -1), "with the flag, among the outer islands: one comes");
        helper.assertFalse(Leegtewalvissen.magErbij(true, 100, 100, -1), "not around the main island");
        helper.assertFalse(Leegtewalvissen.magErbij(true, ver, 0, Leegtewalvissen.BEREIK - 1), "at most one within 256 blocks");
        helper.assertTrue(Leegtewalvissen.magErbij(true, ver, 0, Leegtewalvissen.BEREIK + 1), "a far one doesn't count");
        helper.assertTrue(Leegtewalvissen.dichtste(level, start) >= 0 && Leegtewalvissen.dichtste(level, start) < 1, "the nearest whale is found (searches skip it otherwise)");
        helper.assertTrue(LeegtewalvisguhEntity.MIN_Y + LeegtewalvisguhEntity.RUG >= 90 && LeegtewalvisguhEntity.MAX_Y + LeegtewalvisguhEntity.RUG <= Einde2Ids.NATUUR_MAX,
                "its back stays in the natural band");
        // what it steers clear of: the boxes of the deep band's big places, with MIJD blocks around them
        helper.assertFalse(w.inDeWeg(level, w.getX(), w.getZ() + 4), "open sky ahead");
        int rand = (int) Math.floor(w.getZ()) + 5 + LeegtewalvisguhEntity.MIJD;
        BoundingBox doos = new BoundingBox((int) w.getX() - 10, 4, rand, (int) w.getX() + 10, 117, rand + 30);
        LeegtewalvisguhEntity.zetMijd(level, List.of(doos));
        helper.assertTrue(w.inDeWeg(level, w.getX(), rand - LeegtewalvisguhEntity.MIJD + 0.5), LeegtewalvisguhEntity.MIJD + " blocks before a big place of the deep band: in the way");
        helper.assertFalse(w.inDeWeg(level, w.getX(), rand - LeegtewalvisguhEntity.MIJD - 2), "further off: free");
        LeegtewalvisguhEntity.zetMijd(level, new ArrayList<>());
        helper.assertFalse(w.isPushable() || w.isPickable(), "it can't be pushed (you point at its back)");
        helper.assertFalse(w.hurtServer(level, level.damageSources().generic(), 50f), "it can't be hurt");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(Math.abs(w.getY() - start.y) < 1e-6, "it flies level (was " + start.y + ", is " + w.getY() + ")");
            double ver2 = Math.sqrt(w.position().subtract(start).horizontalDistanceSqr());
            helper.assertTrue(ver2 > 100 * LeegtewalvisguhEntity.VAART * 0.5 && ver2 < 100 * LeegtewalvisguhEntity.VAART * 1.2, "slowly on its way: " + ver2);
            helper.assertTrue(w.getHealth() == w.getMaxHealth(), "unharmed");
            w.discard();
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the element animals
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversSlee(GameTestHelper helper) {
        OtterPinguinguhEntity otter = helper.spawn(E2ZweversFeature.OTTER_PINGUINGUH.get(), IJS);
        ServerPlayer p = speler(helper, IJS.east());
        List<String> gezien = new ArrayList<>();
        OtterPinguinguhEntity.OPGESTAPT.add((o, wie) -> {
            if (o == otter) {
                gezien.add("op");
            }
        });
        OtterPinguinguhEntity.AFGESTAPT.add((o, wie) -> {
            if (o == otter) {
                gezien.add("af");
            }
        });
        helper.assertTrue(OtterPinguinguhEntity.glad(Blocks.PACKED_ICE.defaultBlockState()) && OtterPinguinguhEntity.glad(Blocks.SNOW_BLOCK.defaultBlockState())
                && OtterPinguinguhEntity.glad(Blocks.SNOW.defaultBlockState()) && !OtterPinguinguhEntity.glad(Blocks.STONE.defaultBlockState()), "ice and snow are slippery, stone isn't");
        helper.assertTrue(!otter.isSlee(), "an ordinary otter-pinguinguh is no sled");
        otter.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(!p.isPassenger() && otter.getControllingPassenger() == null, "an empty hand only pets it");
        otter.laatRijden(true);
        helper.assertTrue(otter.isSlee(), "laatRijden makes it a sled");
        helper.assertTrue(otter.mobInteract(p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && p.getVehicle() == otter, "now the player sits on it");
        helper.assertTrue(otter.getControllingPassenger() == p && otter.isGlijdend(), "the rider steers, the otter lies on its belly");
        helper.assertTrue(gezien.equals(List.of("op")), "the hook for getting on was called: " + gezien);
        // its speed: kept on ice, dragged on bare ground, pushed off, braked, capped
        double v = 0.4;
        helper.assertTrue(OtterPinguinguhEntity.volgendeVaart(v, true, 0) > v * 0.99 && OtterPinguinguhEntity.volgendeVaart(v, false, 0) < v * 0.91, "ice keeps the speed, bare ground eats it");
        helper.assertTrue(OtterPinguinguhEntity.volgendeVaart(v, true, 1) > v && OtterPinguinguhEntity.volgendeVaart(v, true, -1) < v * 0.95, "forward pushes off, back brakes");
        helper.assertTrue(OtterPinguinguhEntity.volgendeVaart(5, true, 1) == OtterPinguinguhEntity.MAX_VAART && OtterPinguinguhEntity.volgendeVaart(0, false, -1) == 0, "between 0 and MAX_VAART");
        otter.laatRijden(false);
        helper.assertTrue(!p.isPassenger() && !otter.isSlee() && gezien.equals(List.of("op", "af")), "no sled any more: the rider is off (" + gezien + ")");
        helper.assertTrue(otter.isVoer(new ItemStack(Items.COD)) && !otter.isVoer(new ItemStack(Items.BREAD)), "it likes fish");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void einde2ZweversBrood(GameTestHelper helper) {
        SchildpadEendguhEntity eend = helper.spawn(E2ZweversFeature.SCHILDPAD_EENDGUH.get(), new BlockPos(10, 2, 5));
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 5));
        helper.assertTrue(eend.isVoer(new ItemStack(Items.BREAD)) && !eend.isVoer(new ItemStack(Items.COD)), "bread is its snack");
        // a tap: into its shell, unharmed
        float hart = eend.getHealth();
        helper.assertFalse(eend.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 4f), "a tap doesn't hurt it");
        helper.assertTrue(eend.isInSchild() && eend.getHealth() == hart, "it hides in its shell");
        // bread from the hand: eaten, and it peeks out at once
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 3));
        helper.assertTrue(eend.mobInteract(p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "it takes the bread");
        helper.assertTrue(eend.broodjes() == 1 && p.getMainHandItem().getCount() == 2 && !eend.isInSchild(), "one piece eaten, out of its shell");
        // and it follows whoever holds bread
        helper.succeedWhen(() -> {
            helper.assertTrue(eend.distanceTo(p) < 3.0, "the schildpad-eendguh waddles to the bread (" + eend.distanceTo(p) + " away)");
            weg(helper, p);
        });
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversZwemmen(GameTestHelper helper) {
        SchildpadEendguhEntity eend = helper.spawn(E2ZweversFeature.SCHILDPAD_EENDGUH.get(), WATER);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(eend.isInWater(), "it is in the basin");
            double top = helper.absolutePos(WATER).getY() + 1.0;
            helper.assertTrue(eend.getY() > top - 0.75, "it floats at the surface (y " + eend.getY() + ", water top " + top + ")");
            helper.succeed();
        });
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversDas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DasMolguhEntity opSteen = helper.spawn(E2ZweversFeature.DAS_MOLGUH.get(), STEEN);
        DasMolguhEntity das = helper.spawn(E2ZweversFeature.DAS_MOLGUH.get(), AARDE);
        das.setHonk(helper.absolutePos(AARDE), 2);
        helper.assertTrue(DasMolguhEntity.zacht(Blocks.DIRT.defaultBlockState()) && DasMolguhEntity.zacht(Blocks.SAND.defaultBlockState())
                && !DasMolguhEntity.zacht(Blocks.STONE.defaultBlockState()), "earth and sand are soft, stone isn't");
        ServerPlayer p = speler(helper, AARDE.east(2));
        p.setShiftKeyDown(true);
        helper.assertFalse(DasMolguhEntity.stapt(p), "it doesn't feel a sneaking player");
        weg(helper, p);
        boolean[] gegraven = {false};
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(opSteen.graafIn(60), "it can't dig in on stone");
            helper.assertTrue(das.graafIn(40) && das.isOnder(), "on earth it digs in");
            helper.assertTrue(das.getBbHeight() < 0.4, "only a strip of back stays above the ground");
            helper.assertTrue(das.isBezig() && !das.isPushable(), "busy, and you walk over it");
            gegraven[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gegraven[0], "it has not dug in yet");
            helper.assertTrue(!das.isOnder(), "it comes up again by itself");
            helper.assertTrue(das.getBbHeight() > 0.6, "full size again");
            for (int x = 1; x < 5; x++) {
                for (int z = 9; z < 13; z++) {
                    helper.assertBlockPresent(Blocks.DIRT, new BlockPos(x, 1, z));
                }
            }
            helper.assertTrue(level.getBlockState(helper.absolutePos(AARDE)).isAir(), "it changed no block");
        });
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversLemur(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        VleermuisLemurguhEntity lemur = helper.spawn(E2ZweversFeature.VLEERMUIS_LEMURGUH.get(), STEEN);
        BlockPos balk = helper.absolutePos(BALK);
        helper.assertTrue(VleermuisLemurguhEntity.kanHangen(level, balk), "it can hang under the beam");
        helper.assertFalse(VleermuisLemurguhEntity.kanHangen(level, helper.absolutePos(STEEN.below())), "not under the floor");
        helper.assertTrue(lemur.stand() == VleermuisLemurguhEntity.GROND && !lemur.isNoGravity(), "it starts on the ground");
        lemur.hangAan(balk);
        helper.assertTrue(lemur.stand() == VleermuisLemurguhEntity.HANGT && lemur.isNoGravity() && lemur.isBezig(), "it hangs");
        helper.assertTrue(Math.abs(lemur.getY() + lemur.getBbHeight() - balk.getY()) < 1e-6, "its feet are at the beam");
        boolean[] balkWeg = {false};
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(lemur.stand() == VleermuisLemurguhEntity.HANGT && Math.abs(lemur.getY() + lemur.getBbHeight() - balk.getY()) < 0.01, "and it stays there");
            level.removeBlock(balk, false);
            balkWeg[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(balkWeg[0], "the beam is still there");
            helper.assertTrue(lemur.stand() == VleermuisLemurguhEntity.GROND && !lemur.isNoGravity(), "the beam is gone: it lets go");
            helper.assertTrue(lemur.onGround() && lemur.getHealth() == lemur.getMaxHealth(), "and lands unharmed");
        });
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2ZweversVliegen(GameTestHelper helper) {
        VleermuisLemurguhEntity lemur = helper.spawn(E2ZweversFeature.VLEERMUIS_LEMURGUH.get(), STEEN.north(2));
        BlockPos balk = helper.absolutePos(BALK);
        boolean[] opgestegen = {false};
        helper.runAfterDelay(5, () -> {
            lemur.vliegNaar(new Vec3(balk.getX() + 0.5, balk.getY() - (double) lemur.getBbHeight(), balk.getZ() + 0.5), balk);
            helper.assertTrue(lemur.stand() == VleermuisLemurguhEntity.VLIEGT && lemur.isNoGravity(), "it takes off");
            opgestegen[0] = true;
        });
        helper.succeedWhen(() -> helper.assertTrue(opgestegen[0] && lemur.stand() == VleermuisLemurguhEntity.HANGT,
                "a short hop up to the beam, and it hangs (stand " + lemur.stand() + ")"));
    }

    /** Creatures as a building places them: PersistenceRequired, Honk (they stay within it), the otter a sled. */
    @GuhTest(template = HONK, batch = BATCH, timeoutTicks = 400)
    public static void einde2ZweversHonk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(11, 6, 11);
        helper.runAfterDelay(5, () -> {
            List<Elementdier> dieren = level.getEntitiesOfClass(Elementdier.class, kamer);
            helper.assertTrue(dieren.size() == 4, "the template placed the four element animals, found " + dieren.size());
            for (Elementdier d : dieren) {
                helper.assertTrue(d.isPersistenceRequired() && d.honk() == 3 && d.honkThuis() != null && d.hasHome() && d.getHomeRadius() == 3,
                        d.getType() + ": placed for good, with a Honk of 3 around where it was put");
                helper.assertTrue(!WildeDieren.isKomEnGa(d), d.getType() + " never despawns");
                if (d instanceof OtterPinguinguhEntity o) {
                    helper.assertTrue(o.isSlee(), "Slee: 1b makes the otter a sled");
                }
            }
            List<? extends KruimelkwalEntity> kwallen = level.getEntities(E2ZweversFeature.KRUIMELKWAL.get(), kamer, k -> true);
            helper.assertTrue(kwallen.size() == 1 && kwallen.get(0).isPersistenceRequired(), "and the kwal");
        });
        helper.runAfterDelay(300, () -> {
            for (Elementdier d : level.getEntitiesOfClass(Elementdier.class, kamer.inflate(8))) {
                helper.assertTrue(d.honkThuis().distSqr(d.blockPosition()) <= 4.5 * 4.5, d.getType() + " stayed near home: " + Math.sqrt(d.honkThuis().distSqr(d.blockPosition())));
            }
            for (KruimelkwalEntity k : level.getEntities(E2ZweversFeature.KRUIMELKWAL.get(), kamer.inflate(8), k -> true)) {
                helper.assertTrue(k.thuis() != null && k.thuis().distanceTo(k.position()) <= 4.5, "the kwal stayed near home");
            }
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the top-ups, the pages
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversAanvullen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // swarms
        helper.assertTrue(Kruimelkwallen.magErbij(0) && Kruimelkwallen.magErbij(1) && !Kruimelkwallen.magErbij(2), "at most two swarms around a player");
        BlockPos midden = helper.absolutePos(STEEN.above(3));
        helper.assertFalse(Kruimelkwallen.hier(level, midden), "the test world is no place for swarms");
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(14, 8, 14);
        Kruimelkwallen.ookIn((l, pos) -> l == level && kamer.contains(pos.getX(), pos.getY(), pos.getZ()));
        helper.assertTrue(Kruimelkwallen.hier(level, midden) && Kruimelkwallen.ook(level, midden), "until another slice names the spot (ookIn)");
        helper.assertFalse(Kruimelkwallen.hier(level, midden.above(40)), "and only that spot");
        List<KruimelkwalEntity> zwerm = Kruimelkwallen.zwerm(level, midden, level.getRandom());
        helper.assertTrue(zwerm.size() >= Kruimelkwallen.MIN && zwerm.size() <= Kruimelkwallen.MAX, "a swarm is 4 to 7 kwallen, was " + zwerm.size());
        for (KruimelkwalEntity k : zwerm) {
            helper.assertTrue(WildeDieren.isKomEnGa(k) && k.zwerm() == zwerm.get(0).zwerm() && !k.shouldBeSaved(), "come-and-go, one swarm");
        }
        helper.assertTrue(Kruimelkwallen.zwermen(level, Vec3.atCenterOf(midden)) >= 1, "the swarm is counted");
        zwerm.forEach(Entity::discard);
        // element animals
        helper.assertTrue(Elementdieren.magErbij(1) && !Elementdieren.magErbij(2), "at most two wild ones of a kind around a player");
        helper.assertTrue(Elementdieren.soort(Einde2Ids.Element.LUCHT) == E2ZweversFeature.VLEERMUIS_LEMURGUH.get()
                && Elementdieren.soort(Einde2Ids.Element.WATER) == E2ZweversFeature.OTTER_PINGUINGUH.get()
                && Elementdieren.soort(Einde2Ids.Element.AARDE) == E2ZweversFeature.DAS_MOLGUH.get()
                && Elementdieren.soort(Einde2Ids.Element.VUUR) == E2ZweversFeature.SCHILDPAD_EENDGUH.get(), "every element has its animal");
        helper.assertTrue(Elementdieren.elementVan(level.getBiome(midden)) == null, "the test world is no element's biome: nothing comes");
        for (Einde2Ids.Element element : Einde2Ids.Element.values()) {
            Elementdier dier = Elementdieren.op(level, helper.absolutePos(STEEN), Elementdieren.soort(element), level.getRandom());
            helper.assertTrue(dier != null && dier.element() == element && WildeDieren.isKomEnGa(dier) && !dier.shouldBeSaved(), element + ": a wild one comes and goes");
            dier.discard();
        }
        helper.assertTrue(Elementdieren.op(level, helper.absolutePos(STEEN.above(4)), E2ZweversFeature.DAS_MOLGUH.get(), level.getRandom()) == null, "never in mid-air");
        SchildpadEendguhEntity opWater = Elementdieren.op(level, helper.absolutePos(WATER.above()), E2ZweversFeature.SCHILDPAD_EENDGUH.get(), level.getRandom());
        helper.assertTrue(opWater != null, "the schildpad-eendguh also comes on water");
        opWater.discard();
        helper.assertTrue(Elementdieren.op(level, helper.absolutePos(WATER.above()), E2ZweversFeature.DAS_MOLGUH.get(), level.getRandom()) == null, "the others don't");
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2ZweversPaginas(GameTestHelper helper) {
        for (GuhVariant pagina : List.of(GuhVariant.KRUIMELKWAL, GuhVariant.LEEGTEWALVISGUH, GuhVariant.VLEERMUIS_LEMURGUH, GuhVariant.OTTER_PINGUINGUH,
                GuhVariant.DAS_MOLGUH, GuhVariant.SCHILDPAD_EENDGUH)) {
            helper.assertTrue(GuhDex.isCreaturePage(pagina) && GuhDex.ENTRIES.contains(pagina), pagina + " is a Guhdex creature page");
        }
        helper.assertTrue(GuhDex.EXTRA.contains(GuhVariant.LEEGTEWALVISGUH) && GuhDex.EXTRA.contains(GuhVariant.KRUIMELKWAL), "bonus pages, as every creature page of the outer islands");
        ServerPlayer p = speler(helper, STEEN);
        List<Entity> dieren = new ArrayList<>();
        for (EntityType<?> soort : List.<EntityType<?>>of(E2ZweversFeature.KRUIMELKWAL.get(), E2ZweversFeature.VLEERMUIS_LEMURGUH.get(),
                E2ZweversFeature.OTTER_PINGUINGUH.get(), E2ZweversFeature.DAS_MOLGUH.get(), E2ZweversFeature.SCHILDPAD_EENDGUH.get())) {
            dieren.add(helper.spawn(soort, STEEN.north()));
        }
        GuhDex.seeCreatures(p);
        for (String id : List.of("kruimelkwal", "vleermuis_lemurguh", "otter_pinguinguh", "das_molguh", "schildpad_eendguh")) {
            helper.assertTrue(heeft(p, "seen_" + id), "next to a " + id + ": its page is filled in");
        }
        helper.assertFalse(heeft(p, "seen_leegtewalvisguh"), "not the whale's");
        dieren.forEach(Entity::discard);
        weg(helper, p);
        helper.succeed();
    }
}
