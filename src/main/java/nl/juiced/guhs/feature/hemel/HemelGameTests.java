package nl.juiced.guhs.feature.hemel;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.band.Wolkjes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the hemel slice (DESIGN_30 §4): the wolkenhoeder's three things wake the Knuffelhart (outfits once), the
 * revive brings a guh back with everything (and not a living one, not for someone else, not from far away), a Herinnering
 * star at the heart, the heart can't be broken, the chapel template (one heart, the wolkenhoeder, the lifts), the protection.
 * Template hemel_test_wolk: 12 x 12 floor at template y 0 (= helper y 1: things stand at helper y 2).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class HemelGameTests {
    private static final String WOLK = "hemel_test_wolk";
    private static final String BATCH = "hemel";
    private static final BlockPos HART = new BlockPos(6, 2, 3);

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at, GameType mode) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(mode);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        HemelQuest.vergeet(p);
        Praat.vergeet(p);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static BlockPos hart(GameTestHelper helper) {
        helper.setBlock(HART, HemelFeature.KNUFFELHART.get());
        return helper.absolutePos(HART);
    }

    private static int tel(ServerPlayer p, GuhClothes c) {
        return GuhQuests.count(p, ModItems.clothingItem(c));
    }

    /** The three things (in any order, over several visits) wake the heart; the outfits come once; the screen only after that. */
    @GameTest(template = WOLK, batch = BATCH)
    public static void hemelDrieDingenMakenHetHartWakker(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 7), GameType.SURVIVAL);
        BlockPos hart = hart(helper);
        GuhNpcEntity hoeder = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(8, 2, 4));
        hoeder.setKind(GuhNpcEntity.Kind.WOLKENHOEDER);
        NpcRole rol = NpcRollen.rol(GuhNpcEntity.Kind.WOLKENHOEDER);
        helper.assertTrue(rol != null && NpcRollen.van(hoeder) instanceof Wolkenhoeder, "the wolkenhoeder has his role");
        rol.talk(hoeder, p);
        helper.assertTrue(HemelQuest.stap(p) == 1 && !HemelQuest.klopt(p) && Wolkenhoeder.INTRO.equals(Praat.lopend(p)), "met: the intro scene");
        helper.assertTrue(Wolkenhoeder.hart(hoeder) != null && Wolkenhoeder.hart(hoeder).equals(hart), "he knows his heart");
        helper.assertTrue(!Hemel.openScherm(p, hart), "no screen while the heart sleeps");
        p.getInventory().add(new ItemStack(ModItems.GUH_KRISTAL.get(), 3));
        p.getInventory().add(new ItemStack(nl.juiced.guhs.feature.vogels.VogelsFeature.PLUISVEERTJE.get()));
        rol.talk(hoeder, p);
        helper.assertTrue(HemelQuest.heeft(p, HemelQuest.Ding.KRISTAL) && HemelQuest.heeft(p, HemelQuest.Ding.VEERTJE)
                && HemelQuest.nodig(p).equals(List.of(HemelQuest.Ding.KNABBEL)), "two brought, the gouden kaasknabbel still needed");
        helper.assertTrue(GuhQuests.count(p, ModItems.GUH_KRISTAL.get()) == 2
                && GuhQuests.count(p, nl.juiced.guhs.feature.vogels.VogelsFeature.PLUISVEERTJE.get()) == 0, "one of each taken, not more");
        helper.assertTrue(!HemelQuest.klopt(p) && tel(p, GuhClothes.HEMEL_AUREOOLTJE) == 0, "not yet");
        helper.assertTrue(Wolkenhoeder.nodigKey(p).equals("gui.guhs.hemel.nodig.2"), "the line says what's missing: " + Wolkenhoeder.nodigKey(p));
        p.getInventory().add(new ItemStack(nl.juiced.guhs.feature.evenementen.EvenementenFeature.GOUDEN_KAASKNABBEL.get()));
        rol.talk(hoeder, p);
        helper.assertTrue(HemelQuest.klopt(p) && HemelQuest.stap(p) == 2 && Wolkenhoeder.KLOPT.equals(Praat.lopend(p)), "the heart beats: the scene");
        helper.assertTrue(tel(p, GuhClothes.HEMEL_AUREOOLTJE) == 1 && tel(p, GuhClothes.HEMEL_WOLKENVLEUGELTJES) == 1, "the two outfits");
        rol.talk(hoeder, p);
        HemelQuest.wakker(p);
        helper.assertTrue(tel(p, GuhClothes.HEMEL_AUREOOLTJE) == 1 && tel(p, GuhClothes.HEMEL_WOLKENVLEUGELTJES) == 1, "only once");
        helper.assertTrue(Hemel.openScherm(p, hart), "now the screen opens");
        helper.assertTrue("hemel".equals(KledingBronnen.bron(GuhClothes.HEMEL_AUREOOLTJE))
                && "hemel".equals(KledingBronnen.bron(GuhClothes.HEMEL_WOLKENVLEUGELTJES)), "the source of both pieces");
        hoeder.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The heart brings a guh back with everything, free and as often as needed; never a living one, never from far away. */
    @GameTest(template = WOLK, batch = BATCH, timeoutTicks = 300)
    public static void hemelTerugUitDeWolkjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 7), GameType.SURVIVAL);
        ServerPlayer ander = speler(helper, new BlockPos(5, 2, 7), GameType.SURVIVAL);
        BlockPos hart = hart(helper);
        HemelQuest.wakker(p);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 9));
        guh.tame(p);
        guh.setVariant(GuhVariant.CHOCO);
        guh.setCustomName(Component.literal("Wolkje"));
        guh.wear(GuhClothes.HEMEL_AUREOOLTJE);
        guh.wear(GuhClothes.RED_BOWTIE);
        Band.geefHartjes(guh, p, 250, Reden.OVERIG);
        UUID id = guh.getUUID();
        int hartjes = Band.hartjes(guh);
        guh.kill();
        helper.assertTrue(Hemel.lijst(p).stream().anyMatch(d -> d.bandId().equals(id) && d.naam().equals("Wolkje")), "in the list of the screen");
        CompoundTag data = Hemel.data(p, hart, null);
        helper.assertTrue(data.getList("Guhs", Tag.TAG_COMPOUND).size() == 1
                && data.getList("Guhs", Tag.TAG_COMPOUND).getCompound(0).getString("Naam").equals("Wolkje"), "the screen data");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(hart).inflate(12)).forEach(Entity::discard);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntity(id) == null, "(the body is gone first)");
            helper.assertTrue(Hemel.terug(ander, hart, id) == null, "someone else can't (their heart sleeps, and it isn't theirs)");
            HemelQuest.wakker(ander);
            helper.assertTrue(Hemel.terug(ander, hart, id) == null && Wolkjes.isDood(p.server, p.getUUID(), id), "not someone else's guh");
            Vec3 was = p.position();
            p.moveTo(was.x + 30, was.y, was.z);
            helper.assertTrue(Hemel.terug(p, hart, id) == null, "not from far away");
            p.moveTo(was.x, was.y, was.z);
            GuhEntity terug = Hemel.terug(p, hart, id);
            helper.assertTrue(terug != null && terug.getUUID().equals(id) && terug.isOwnedBy(p) && terug.getVariant() == GuhVariant.CHOCO
                    && "Wolkje".equals(terug.getCustomName().getString()) && terug.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.HEMEL_AUREOOLTJE
                    && terug.getClothes(GuhClothes.Slot.NECK) == GuhClothes.RED_BOWTIE, "the same guh with its clothes");
            helper.assertTrue(Band.hartjes(terug) == hartjes && terug.getHealth() == terug.getMaxHealth(), "all its hearts, healthy");
            helper.assertTrue(GuhHooks.heeft(terug, VerhaalVlaggen.GLANS) && HemelQuest.terug(p) == 1, "it sparkles; counted");
            helper.assertTrue(terug.position().distanceTo(Vec3.atCenterOf(hart)) < Hemel.BEREIK, "next to the heart");
            helper.assertTrue(Hemel.terug(p, hart, id) == null && Hemel.lijst(p).isEmpty(), "a living guh can't come back again");
            terug.discard();
            weg(helper, p, ander);
        });
    }

    /** A Herinnering star at the heart brings exactly its guh back; a star of a living guh stays a keepsake. */
    @GameTest(template = WOLK, batch = BATCH, timeoutTicks = 300)
    public static void hemelSterretjeBijHetHart(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 7), GameType.SURVIVAL);
        BlockPos hart = hart(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 9));
        guh.tame(p);
        guh.setCustomName(Component.literal("Sterretje"));
        UUID id = guh.getUUID();
        guh.kill();
        List<ItemEntity> sterren = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(9, 2, 9))).inflate(3),
                e -> e.getItem().is(HemelFeature.HERINNERING.get()));
        helper.assertTrue(sterren.size() == 1, "one star");
        ItemStack ster = sterren.get(0).getItem().copy();
        sterren.forEach(Entity::discard);
        helper.assertTrue(Herinnering.data(ster).getUUID("Band").equals(id), "its star");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntity(id) == null, "(the body is gone first)");
            helper.assertTrue(!Hemel.ster(p, hart, ster) && ster.getCount() == 1, "the heart sleeps: nothing, the star stays");
            HemelQuest.wakker(p);
            helper.assertTrue(Hemel.ster(p, hart, ster) && ster.isEmpty(), "the star brings it back and goes with it");
            GuhEntity terug = helper.getLevel().getEntity(id) instanceof GuhEntity g ? g : null;
            helper.assertTrue(terug != null && terug.isAlive() && "Sterretje".equals(terug.getCustomName().getString()), "Sterretje is back");
            ItemStack nog = Herinnering.maak(terug);
            helper.assertTrue(!Hemel.ster(p, hart, nog) && nog.getCount() == 1, "a star of a living guh: a keepsake");
            terug.discard();
            weg(helper, p);
        });
    }

    /** The Knuffelhart can't be broken (survival), nor blown up; creative players may. */
    @GameTest(template = WOLK, batch = BATCH)
    public static void hemelHartIsOnbreekbaar(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 5), GameType.SURVIVAL);
        BlockPos hart = hart(helper);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.getBlockState(hart).getDestroySpeed(level, hart) < 0, "hardness -1 (like bedrock)");
        helper.assertTrue(!p.gameMode.destroyBlock(hart) && level.getBlockState(hart).is(HemelFeature.KNUFFELHART.get()), "survival: stays");
        level.explode(null, hart.getX() + 0.5, hart.getY() + 0.5, hart.getZ() + 1.5, 4f, Level.ExplosionInteraction.BLOCK);
        helper.assertTrue(level.getBlockState(hart).is(HemelFeature.KNUFFELHART.get()), "an explosion: stays");
        helper.assertTrue(level.getBlockState(hart).getBlock().asItem() == net.minecraft.world.item.Items.AIR, "no item: not craftable, not in a tab");
        helper.assertTrue(level.getBlockEntity(hart) instanceof KnuffelhartBlockEntity, "its block entity (the beating heart)");
        ServerPlayer creatief = speler(helper, new BlockPos(7, 2, 5), GameType.CREATIVE);
        helper.assertTrue(creatief.gameMode.destroyBlock(hart) && level.getBlockState(hart).isAir(), "creative may");
        weg(helper, p, creatief);
        helper.succeed();
    }

    /** The chapel's template: exactly one Knuffelhart, the wolkenhoeder, both wolkenliften, the hemelkist. */
    @GameTest(template = WOLK, batch = BATCH)
    public static void hemelKapelletjeTemplate(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("hemelkapelletje")).orElse(null);
        helper.assertTrue(t != null, "the template exists");
        var instellingen = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, instellingen, HemelFeature.KNUFFELHART.get()).size() == 1, "one Knuffelhart");
        var liften = t.filterBlocks(BlockPos.ZERO, instellingen, nl.juiced.guhs.feature.eilanden.EilandenFeature.WOLKENLIFT.get());
        helper.assertTrue(liften.size() == 18, "two wolkenliften of 3x3: " + liften.size());
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, instellingen, Blocks.CHEST).size() == 1, "the hemelkist");
        CompoundTag nbt = t.save(new CompoundTag());
        long hoeders = nbt.getList("entities", Tag.TAG_COMPOUND).stream()
                .filter(e -> "wolkenhoeder".equals(((CompoundTag) e).getCompound("nbt").getString("Kind"))).count();
        helper.assertTrue(hoeders == 1, "one wolkenhoeder: " + hoeders);
        helper.assertTrue(t.getSize().getY() < 128 && t.getSize().getY() > HemelProtection.SKY_FROM + 20, "tall: the islet floats high");
        helper.succeed();
    }

    /** Inside the chapel nothing is broken or built (survival); the Herinnering may still be used on the heart. */
    @GameTest(template = WOLK, batch = BATCH)
    public static void hemelBescherming(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2), GameType.SURVIVAL);
        BlockPos steen = helper.absolutePos(new BlockPos(3, 2, 9));
        helper.getLevel().setBlockAndUpdate(steen, Blocks.WHITE_WOOL.defaultBlockState());
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(11, 6, 11)));
        HemelProtection.TEST_AREAS.add(box);
        try {
            helper.assertTrue(HemelProtection.protectedAt(helper.getLevel(), steen), "protected");
            helper.assertTrue(!p.gameMode.destroyBlock(steen) && helper.getLevel().getBlockState(steen).is(Blocks.WHITE_WOOL), "can't break it");
        } finally {
            HemelProtection.TEST_AREAS.remove(box);
        }
        helper.assertTrue(p.gameMode.destroyBlock(steen), "outside the chapel: fine");
        weg(helper, p);
        helper.succeed();
    }
}
