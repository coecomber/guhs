package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Guhhuisje (2.10, fundament): capacity 3/5/8 with guhs and a muisje, only your own, moving between
 * huisjes; residents stay home (walk back, no teleport to the owner), sleep inside at night and come out in the morning;
 * a chore runs from the huisje and its toggle is saved; HuisjeOpslag delivers to the chest and to the Bank Guh (never
 * loaned things); unique names; breaking the huisje sets everyone free; "waar is mijn guh" for residents.
 * (Template huisje_test_tuin: 24 x 24 grass at y 0.)
 */
public class HuisjeGameTests {
    private static final String TUIN = "huisje_test_tuin";
    private static final String BATCH = "huisje";
    /** A test chore: only guhs called "Kluskampioen" do it (so it never disturbs anything else). */
    static final String TEST_KLUS = "huisje_test";
    static final String KAMPIOEN = "Kluskampioen";
    static final AtomicInteger TEST_TICKS = new AtomicInteger();

    static {
        Klusjes.registreer(new Klus() {
            @Override
            public String id() {
                return TEST_KLUS;
            }

            @Override
            public ItemStack icoon() {
                return new ItemStack(Items.WOODEN_SHOVEL);
            }

            @Override
            public boolean kan(Mob bewoner) {
                return bewoner.hasCustomName() && bewoner.getCustomName().getString().equals(KAMPIOEN);
            }

            @Override
            public int wacht() {
                return 20;
            }

            @Nullable
            @Override
            public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
                return () -> TEST_TICKS.incrementAndGet() % 10 != 0;
            }
        });
    }

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
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

    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        return guh;
    }

    static Huisje bouw(GameTestHelper helper, BlockPos at, HuisjeMaat maat, ServerPlayer owner) {
        return HuisjeBlock.bouw(helper.getLevel(), helper.absolutePos(at), Direction.SOUTH, maat, owner.getUUID());
    }

    /** 1.2.5: the owner switches the rare-find chat messages off in the screen; it's saved with the huisje. */
    @GuhTest(template = TUIN, batch = BATCH + "_meldingen")
    public static void huisjeMeldingenUit(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        Huisje h = bouw(helper, new BlockPos(5, 1, 5), HuisjeMaat.KLEIN, p);
        helper.assertTrue(h.meldingen(), "on by default");
        HuisjePayloads.doe(p, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.MELDINGEN.ordinal(), "", "", false, -1));
        helper.assertTrue(!h.meldingen(), "switched off");
        Huisje kopie = Huisje.load(h.save());
        helper.assertTrue(kopie != null && !kopie.meldingen(), "saved off");
        HuisjePayloads.doe(p, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.MELDINGEN.ordinal(), "", "", true, -1));
        helper.assertTrue(h.meldingen(), "on again");
        weg(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================

    @GuhTest(template = TUIN, batch = BATCH)
    public static void huisjePlekkenDrieVijfAcht(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 1, 12));
        for (HuisjeMaat maat : HuisjeMaat.values()) {
            Huisje h = bouw(helper, new BlockPos(3 + maat.ordinal() * 7, 1, 3), maat, p);
            helper.assertTrue(h.maat().plekken() == List.of(3, 5, 8).get(maat.ordinal()), "capacity of " + maat);
            // a muisje first, then guhs until it is full
            TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(3 + maat.ordinal() * 7, 1, 12));
            muis.tame(p);
            helper.assertTrue(Huisjes.trekIn(h, muis) && h.soort(Band.id(muis)).equals("pieppiepmuisje"), "a muisje moves in");
            for (int i = 1; i < maat.plekken(); i++) {
                helper.assertTrue(Huisjes.trekIn(h, guh(helper, p, new BlockPos(3 + maat.ordinal() * 7, 1, 14 + i % 4))), maat + ": guh " + i);
            }
            helper.assertTrue(h.isVol() && h.bewoners().size() == maat.plekken(), maat + " is full");
            GuhEntity teVeel = guh(helper, p, new BlockPos(3 + maat.ordinal() * 7, 1, 20));
            helper.assertTrue(!Huisjes.trekIn(h, teVeel) && !Huisjes.isBewoner(teVeel), maat + ": one too many stays outside");
        }
        // only your own
        ServerPlayer ander = speler(helper, new BlockPos(20, 1, 20));
        Huisje h = bouw(helper, new BlockPos(3, 1, 20), HuisjeMaat.KLEIN, p);
        GuhEntity vreemd = guh(helper, ander, new BlockPos(6, 1, 20));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(7, 1, 20));
        helper.assertTrue(!Huisjes.trekIn(h, vreemd) && !Huisjes.trekIn(h, wild), "someone else's guh or a wild one can't move in");
        weg(helper, p, ander);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void huisjeVerhuizenEnUniekeNamen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 1, 12));
        Huisje a = bouw(helper, new BlockPos(3, 1, 3), HuisjeMaat.KLEIN, p);
        Huisje b = bouw(helper, new BlockPos(12, 1, 3), HuisjeMaat.KLEIN, p);
        helper.assertTrue(!a.naam().equalsIgnoreCase(b.naam()), "every huisje has its own name: " + a.naam() + " / " + b.naam());
        helper.assertTrue(!Huisjes.hernoem(p.level().getServer(), b, a.naam().toUpperCase()), "a name that is taken can't be used again");
        helper.assertTrue(Huisjes.hernoem(p.level().getServer(), b, "Villa Vadsig") && b.naam().equals("Villa Vadsig"), "renamed");
        helper.assertTrue(!Huisjes.hernoem(p.level().getServer(), b, "  ") && !Huisjes.hernoem(p.level().getServer(), b, "x".repeat(40)), "not empty, not too long");
        GuhEntity guh = guh(helper, p, new BlockPos(6, 1, 10));
        Huisjes.trekIn(a, guh);
        helper.assertTrue(Huisjes.thuisVan(guh) == a, "lives in a");
        Huisjes.trekIn(b, guh);
        helper.assertTrue(Huisjes.thuisVan(guh) == b && !a.bewoners().contains(Band.id(guh)), "moved to b");
        helper.assertTrue(GuhVolger.plek(p.level().getServer(), p.getUUID(), Band.id(guh)).soort() == PlekSoort.HUISJE, "waar is mijn guh: at home");
        Huisjes.trekUit(guh);
        helper.assertTrue(!Huisjes.isBewoner(guh) && b.bewoners().isEmpty(), "moved out");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 900)
    public static void huisjeBewonersBlijvenThuis(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(22, 1, 22));
        Huisje h = bouw(helper, new BlockPos(2, 1, 2), HuisjeMaat.KLEIN, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.DAG);
        GuhEntity guh = guh(helper, p, new BlockPos(21, 1, 21));
        Huisjes.trekIn(h, guh);
        helper.assertTrue(!h.inGebied(guh.blockPosition()), "it starts far from its new home");
        helper.assertTrue(!guh.shouldTryTeleportToOwner(), "a resident never teleports to its owner");
        // the owner walks away; the guh goes home to its huisje instead of following
        p.snapTo(helper.absolutePos(new BlockPos(23, 1, 23)).getCenter());
        helper.succeedWhen(() -> {
            helper.assertTrue(h.inGebied(guh.blockPosition()), "walked back home: " + guh.blockPosition() + " / " + h.midden());
            HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
            weg(helper, p);
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 900)
    public static void huisjeSlapenBinnenEnGapendWakker(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(22, 1, 22));
        Huisje h = bouw(helper, new BlockPos(4, 1, 4), HuisjeMaat.MEDIUM, p);
        GuhEntity guh = guh(helper, p, new BlockPos(9, 1, 12));
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(6, 1, 12));
        muis.tame(p);
        Huisjes.trekIn(h, guh);
        Huisjes.trekIn(h, muis);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        AtomicInteger fase = new AtomicInteger();
        helper.onEachTick(() -> {
            if (fase.get() == 0 && Huisjes.isBinnen(guh) && Huisjes.isBinnen(muis)) {
                helper.assertTrue(guh.isInvisible() && muis.isInvisible() && BandVlaggen.heeft(guh, BandVlaggen.HUISJE_BINNEN), "hidden inside");
                helper.assertTrue(GuhVolger.plek(p.level().getServer(), p.getUUID(), Band.id(guh)).soort() == PlekSoort.SLAAPT_IN_HUISJE, "asleep in its huisje");
                fase.set(1);
                HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.OCHTEND);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase.get() == 1, "both went inside at night");
            helper.assertTrue(!Huisjes.isBinnen(guh) && !Huisjes.isBinnen(muis) && !guh.isInvisible() && !muis.isInvisible(), "out in the morning");
            helper.assertTrue(guh.distanceToSqr(Vec3.atBottomCenterOf(h.deur())) < 9, "out of the door");
            helper.assertTrue(Huisjes.isBewoner(guh), "still living there");
            HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
            weg(helper, p);
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void huisjeKlusjeLooptEnSchakelaarBewaard(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        Huisje h = bouw(helper, new BlockPos(4, 1, 4), HuisjeMaat.KLEIN, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.DAG);
        GuhEntity kampioen = guh(helper, p, new BlockPos(6, 1, 8));
        kampioen.setCustomName(net.minecraft.network.chat.Component.literal(KAMPIOEN));
        GuhEntity lui = guh(helper, p, new BlockPos(8, 1, 8));
        Huisjes.trekIn(h, kampioen);
        Huisjes.trekIn(h, lui);
        UUID luiId = Band.id(lui);
        h.zetKlus(luiId, TEST_KLUS, false);
        helper.assertTrue(!h.klusAan(luiId, TEST_KLUS) && h.klusAan(Band.id(kampioen), TEST_KLUS), "switched off for one, on (default) for the other");
        // (merge 3.0) only the test chore for the kampioen: a real chore it happens to pick first (a long walk) made this flaky
        for (Klus k : Klusjes.alle()) {
            if (!k.id().equals(TEST_KLUS)) {
                h.zetKlus(Band.id(kampioen), k.id(), false);
            }
        }
        // saved and loaded: the toggles stay
        CompoundTag tag = Huisjes.get(p.level().getServer()).save(new CompoundTag(), p.registryAccess());
        Huisje terug = null;
        for (Huisje x : Huisjes.load(tag, p.registryAccess()).huisjesVoorTest()) {
            if (x.pos().equals(h.pos())) {
                terug = x;
            }
        }
        helper.assertTrue(terug != null && !terug.klusAan(luiId, TEST_KLUS) && terug.naam().equals(h.naam()) && terug.bewoners().size() == 2,
                "the chore toggle, name and residents are saved");
        int start = TEST_TICKS.get();
        helper.succeedWhen(() -> {
            helper.assertTrue(TEST_TICKS.get() - start >= 10, "the chore ran from the huisje: " + (TEST_TICKS.get() - start));
            helper.assertTrue(!lui.hasCustomName(), "(the other one never could)");
            HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
            weg(helper, p);
        });
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void huisjeOpslagKistEnBankGuh(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        Huisje h = bouw(helper, new BlockPos(4, 1, 4), HuisjeMaat.KLEIN, p);
        ServerLevel level = helper.getLevel();
        // no chest: it pops out at the door
        helper.assertTrue(HuisjeOpslag.kist(level, h) == null && !HuisjeOpslag.heeftBankGuh(level, h), "nothing around yet");
        helper.assertTrue(HuisjeOpslag.lever(level, h, new ItemStack(Items.WHEAT, 3)).isEmpty(), "delivered (at the door)");
        helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.deur()).inflate(2))
                .isEmpty(), "it lies at the door");
        // a chest next to it
        BlockPos kist = new BlockPos(6, 1, 4);
        helper.setBlock(kist, Blocks.CHEST);
        helper.assertTrue(HuisjeOpslag.kist(level, h) != null, "the huisje's chest");
        HuisjeOpslag.lever(level, h, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(kist));
        helper.assertTrue(chest != null && chest.countItem(ModItems.KAAS_KNABBELS.get()) == 5, "in the chest");
        // a Bank Guh in the home base sorts everything (but never a loaned thing)
        BlockPos bank = new BlockPos(12, 1, 10);
        helper.setBlock(bank, ModBlocks.BANK_GUH.get());
        helper.assertTrue(HuisjeOpslag.heeftBankGuh(level, h), "a Bank Guh nearby");
        HuisjeOpslag.lever(level, h, new ItemStack(ModItems.KAAS_KNABBELS.get(), 7));
        BankGuhBlockEntity be = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(bank));
        helper.assertTrue(be != null && be.getStorage().count(new ItemStack(ModItems.KAAS_KNABBELS.get())) == 7, "sorted into the Bank Guh");
        helper.assertTrue(chest.countItem(ModItems.KAAS_KNABBELS.get()) == 5, "the chest got nothing more");
        ItemStack geleend = loaned();
        if (!geleend.isEmpty()) {
            HuisjeOpslag.lever(level, h, geleend.copy());
            helper.assertTrue(be.getStorage().count(geleend) == 0 && chest.countItem(geleend.getItem()) == 1, "a loaned thing never goes into the bank");
        }
        weg(helper, p);
        helper.succeed();
    }

    /** Some item of the guhs:loaned tag (a golf club...), or empty. */
    private static ItemStack loaned() {
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(nl.juiced.guhs.feature.Features.LOANED)) {
            return new ItemStack(holder.value());
        }
        return ItemStack.EMPTY;
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 60)
    public static void huisjeKapotZetIedereenVrij(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        Huisje h = bouw(helper, new BlockPos(4, 1, 4), HuisjeMaat.MEDIUM, p);
        List<BlockPos> delen = new ArrayList<>(h.blokken());
        helper.assertTrue(delen.size() == 27, "a medium huisje is 3 x 3 x 3 blocks");
        for (BlockPos d : delen) {
            helper.assertTrue(helper.getLevel().getBlockState(d).getBlock() instanceof HuisjeBlock
                    || helper.getLevel().getBlockState(d).getBlock() instanceof HuisjeDeelBlock, "part at " + d);
        }
        helper.assertTrue(Huisjes.van(helper.getLevel(), delen.get(delen.size() - 1)) == h, "any part finds its huisje");
        GuhEntity guh = guh(helper, p, new BlockPos(8, 1, 12));
        Huisjes.trekIn(h, guh);
        Huisjes.naarBinnen(guh, h);
        helper.assertTrue(Huisjes.isBinnen(guh), "asleep inside");
        // breaking one invisible part breaks the whole huisje
        helper.getLevel().destroyBlock(delen.get(delen.size() - 1), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(Huisjes.op(p.level().getServer(), helper.getLevel().dimension(), h.pos()) == null, "the huisje is gone");
            for (BlockPos d : delen) {
                helper.assertTrue(helper.getLevel().getBlockState(d).isAir(), "no parts left at " + d);
            }
            helper.assertTrue(!Huisjes.isBewoner(guh) && !Huisjes.isBinnen(guh) && !guh.isInvisible(), "the resident is free again (and visible)");
            weg(helper, p);
        });
    }
}
