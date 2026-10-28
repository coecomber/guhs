package nl.juiced.guhs.feature.raket;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/** 1.8.0 Vadskracht: the Kaasraket's stages (handing in, building, the crew) and the Kaasmaan's low gravity. */
public class RaketGameTests {
    private static final String BATCH = "raket", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    static LanceerplatformBlockEntity platform(GameTestHelper helper, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        helper.getLevel().setBlock(pos, RaketFeature.LANCEERPLATFORM.get().defaultBlockState(), 3);
        return (LanceerplatformBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    static void geef(ServerPlayer p, Item item, int n) {
        while (n > 0) {
            ItemStack s = new ItemStack(item, Math.min(n, item.getDefaultMaxStackSize()));
            n -= s.getCount();
            p.getInventory().add(s);
        }
    }

    static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    /** A stage goes in in portions; with all of it in, the stage is built above the platform; buckets come back empty. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void stappenLeverenEnBouwen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerLevel level = helper.getLevel();
        LanceerplatformBlockEntity be = platform(helper, new BlockPos(2, 1, 2));
        BlockPos pos = be.getBlockPos();
        RaketStappen.Stap romp = RaketStappen.STAPPEN.get(0);
        Item steen = romp.eisen().get(0).item().get();
        geef(p, steen, 30);
        Raket.lever(level, pos, be, p, romp);
        helper.assertTrue(be.binnen(0) == 30 && be.klaar() == 0 && tel(p, steen) == 0, "a portion goes in, the stage is still open");
        for (RaketStappen.Eis e : romp.eisen()) {
            geef(p, e.item().get(), e.aantal());
        }
        Raket.lever(level, pos, be, p, romp);
        helper.assertTrue(be.klaar() == 1, "the romp is done");
        helper.assertTrue(tel(p, steen) == 30, "only what was still needed was taken");
        helper.assertTrue(level.getBlockState(pos.offset(2, 2, 0)).is(Blocks.WHITE_CONCRETE), "the body stands");
        be.zetKlaar(2);
        RaketStappen.Stap brandstof = RaketStappen.STAPPEN.get(2);
        for (RaketStappen.Eis e : brandstof.eisen()) {
            geef(p, e.item().get(), e.aantal());
        }
        Raket.lever(level, pos, be, p, brandstof);
        helper.assertTrue(be.klaar() == 3, "the fuel is in");
        helper.assertTrue(tel(p, Items.BUCKET) == brandstof.eisen().get(1).aantal(), "the saus buckets came back empty");
        helper.succeed();
    }

    /** The crew: three tame guhs of the player in astronaut suits climb into the cockpit (two are not enough). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void bemanningStaptIn(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerLevel level = helper.getLevel();
        LanceerplatformBlockEntity be = platform(helper, new BlockPos(2, 1, 2));
        be.zetKlaar(3);
        BlockPos pos = be.getBlockPos();
        for (int i = 0; i < RaketStappen.BEMANNING; i++) {
            GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
            g.snapTo(pos.getX() + 3.5 + i, pos.getY() + 1, pos.getZ() + 4.5, 0f, 0f);
            g.tame(p);
            if (i > 0) {
                g.wear(GuhClothes.ASTRONAUTENPAKJE);
            }
            level.addFreshEntity(g);
        }
        helper.assertFalse(Raket.bemanning(level, pos, be, p), "two astronauts are not a crew");
        for (GuhEntity g : Raket.astronauten(level, pos, p)) {
            helper.assertTrue(g.getClothes(GuhClothes.Slot.BODY) == GuhClothes.ASTRONAUTENPAKJE, "only guhs in a suit count");
        }
        level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(12)).forEach(g -> g.wear(GuhClothes.ASTRONAUTENPAKJE));
        helper.assertTrue(Raket.bemanning(level, pos, be, p), "three astronauts: in they go");
        helper.assertTrue(be.klaar() == 4 && be.bemanning().size() == 3, "all four stages done, the crew is known");
        helper.assertTrue(be.bezig() > RaketStappen.STAPPEN.size(), "ready for the launch");
        helper.succeed();
    }

    /** On the Kaasmaan you are lighter; elsewhere you are not. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void maanIsLicht(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        KaasMaan.zwaarte(p, true);
        double licht = p.getAttributeValue(Attributes.GRAVITY);
        KaasMaan.zwaarte(p, false);
        double gewoon = p.getAttributeValue(Attributes.GRAVITY);
        helper.assertTrue(licht < gewoon * 0.5, "the moon's gravity is less than half");
        helper.succeed();
    }
}
