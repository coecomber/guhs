package nl.juiced.guhs.feature.voetjes;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingKleermaker;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModFluids;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModVillagers;
import nl.juiced.guhs.storage.Nbt;

/** 1.7.0 (ronde 26): the FEET slot, its six pieces, walking on the sauces, the kleermaker's upgrade. */
public class VoetjesGameTests {
    private static final String EMPTY = "empty";
    private static final List<GuhClothes> STUKKEN = List.of(GuhClothes.PLUISSLOFJES, GuhClothes.REGENLAARSJES, GuhClothes.GOLFSCHOENTJES,
            GuhClothes.MIJNWERKERSLAARSJES, GuhClothes.KAASSAUSLOPERS, GuhClothes.LAVALOPERS);

    private static String bron(String pad) throws java.io.IOException {
        try (InputStream in = VoetjesGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** The seventh wardrobe slot, and every piece: in it, with a source, a texture, an icon and bones the guh model has. */
    @GuhTest(template = EMPTY, batch = "voetjes")
    public static void voetjesSlotEnStukken(GameTestHelper helper) throws Exception {
        List<GuhClothes.Slot> kast = GuhClothes.Slot.kleding();
        helper.assertTrue(kast.size() == 7 && kast.get(6) == GuhClothes.Slot.FEET && GuhWardrobeMenu.index(GuhClothes.Slot.FEET) == 6,
                "FEET is the seventh wardrobe slot (last, so older favourites still fit): " + kast);
        String geo = bron("/assets/guhs/geckolib/models/entity/guh.geo.json");
        helper.assertTrue(geo != null, "the guh model is there");
        for (GuhClothes c : STUKKEN) {
            helper.assertTrue(c.slot == GuhClothes.Slot.FEET, c + " goes on the feet");
            helper.assertTrue(KledingBronnen.bron(c) != null && KledingBronnen.prijs(c) != null, c + " has a source and a price/hint");
            helper.assertTrue(bron("/assets/guhs/textures/entity/guh_clothes/" + c.id() + ".png") != null, c + " has its texture");
            helper.assertTrue(VoetjesGameTests.class.getResource("/assets/guhs/textures/item/" + c.id() + ".png") != null, c + " has its icon");
            helper.assertTrue(ModItems.clothingItem(c) instanceof GuhClothingItem, c + " is an item");
            for (String bot : c.bones) {
                for (String poot : new String[]{"al", "ar", "vl", "vr"}) {
                    helper.assertTrue(geo.contains("\"" + bot + "_" + poot + "\""), c + ": the guh has the bone " + bot + "_" + poot);
                }
            }
        }
        helper.assertTrue("kleermaker".equals(KledingBronnen.bron(GuhClothes.LAVALOPERS)) && "golf".equals(KledingBronnen.bron(GuhClothes.GOLFSCHOENTJES))
                && KledingBronnen.bron(GuhClothes.KAASSAUSLOPERS).startsWith("loot_") && KledingBronnen.bron(GuhClothes.MIJNWERKERSLAARSJES).startsWith("loot_"),
                "the sources of ronde 26");
        helper.succeed();
    }

    /** Rare in the chests of the Mika-grillpaleis and the Spiesburcht; the miner's boots in the old cheese mine shaft. */
    @GuhTest(template = EMPTY, batch = "voetjes")
    public static void voetjesInDeKisten(GameTestHelper helper) throws Exception {
        for (String tabel : new String[]{"grillpaleis_schat", "grillpaleis_voorraad", "spiesburcht"}) {
            String json = bron("/data/guhs/loot_table/chests/" + tabel + ".json");
            helper.assertTrue(json != null && json.contains("\"guhs:kaassauslopers\""), "kaassauslopers in " + tabel);
        }
        String mijn = bron("/data/guhs/loot_table/chests/gatenkaas_mijnschacht.json");
        helper.assertTrue(mijn != null && mijn.contains("\"guhs:mijnwerkerslaarsjes\""), "mijnwerkerslaarsjes in the mine shaft");
        helper.succeed();
    }

    /** Saved as ClothesFeet; a guh saved before 1.7.0 (no such key) simply has empty feet and keeps the rest. */
    @GuhTest(template = EMPTY, batch = "voetjes")
    public static void voetjesOpslaanEnOudeGuhs(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        guh.wear(GuhClothes.PARTY_HAT);
        guh.wear(GuhClothes.PLUISSLOFJES);
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.FEET) == GuhClothes.PLUISSLOFJES && guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT,
                "it wears both");
        CompoundTag tag = Nbt.saveWithoutId(guh);
        helper.assertTrue("pluisslofjes".equals(tag.getStringOr("ClothesFeet", "")), "saved as ClothesFeet");
        GuhEntity kopie = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 2));
        Nbt.load(kopie, tag);
        helper.assertTrue(kopie.getClothes(GuhClothes.Slot.FEET) == GuhClothes.PLUISSLOFJES, "loaded again");
        CompoundTag oud = tag.copy();
        oud.remove("ClothesFeet");                          // a guh from before the FEET slot
        GuhEntity oude = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 2));
        Nbt.load(oude, oud);
        helper.assertTrue(oude.getClothes(GuhClothes.Slot.FEET) == null && oude.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.PARTY_HAT,
                "an old guh: empty feet, the rest as it was");
        CompoundTag fout = tag.copy();
        fout.putString("ClothesFeet", "party_hat");         // a piece of another slot is never put on the feet
        Nbt.load(oude, fout);
        helper.assertTrue(oude.getClothes(GuhClothes.Slot.FEET) == null, "no hat on the feet");
        helper.assertTrue(guh.takeOffClothes().contains(GuhClothes.PLUISSLOFJES) && guh.getClothes(GuhClothes.Slot.FEET) == null,
                "taking all clothes off takes the slippers too");
        helper.succeed();
    }

    /** Which fluids each pair of lopers walks on (and nothing without lopers). */
    @GuhTest(template = EMPTY, batch = "voetjes")
    public static void voetjesWaarOpJeLoopt(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        var kaas = ModFluids.KAAS_SAUS.get().defaultFluidState();
        var frituur = BarbecuetherFeature.KAASFRITUURSAUS.get().defaultFluidState();
        var lava = Fluids.LAVA.defaultFluidState();
        var water = Fluids.WATER.defaultFluidState();
        helper.assertTrue(!guh.canStandOnFluid(kaas) && !guh.canStandOnFluid(lava), "barefoot: in it, not on it");
        guh.wear(GuhClothes.PLUISSLOFJES);
        helper.assertTrue(!guh.canStandOnFluid(kaas) && !Voetjes.heeftLopers(guh), "slippers are only cute");
        guh.wear(GuhClothes.KAASSAUSLOPERS);
        helper.assertTrue(guh.canStandOnFluid(kaas) && guh.canStandOnFluid(frituur) && guh.canStandOnFluid(ModFluids.FLOWING_KAAS_SAUS.get().defaultFluidState()),
                "kaassauslopers: every kaassaus");
        helper.assertTrue(!guh.canStandOnFluid(lava) && !guh.canStandOnFluid(water), "but not lava or water");
        guh.wear(GuhClothes.LAVALOPERS);
        helper.assertTrue(guh.canStandOnFluid(kaas) && guh.canStandOnFluid(frituur) && guh.canStandOnFluid(lava) && !guh.canStandOnFluid(water),
                "lavalopers: lava and every kaassaus");
        helper.succeed();
    }

    private static GuhEntity opDeSaus(GameTestHelper helper, BlockState saus, GuhClothes lopers) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), saus);
            }
        }
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new Vec3(2.5, 2.4, 2.5));
        guh.setOrderedToSit(true);
        guh.wear(lopers);
        return guh;
    }

    private static void staatErBovenop(GameTestHelper helper, GuhEntity guh, String wat) {
        double top = helper.absolutePos(new BlockPos(2, 1, 2)).getY() + 0.45;
        helper.assertTrue(guh.getY() >= top, wat + ": the guh stands on top (y " + guh.getY() + ", top " + top + ")");
        helper.assertTrue(!guh.isOnFire() && guh.getHealth() >= guh.getMaxHealth(), wat + ": not burnt (health " + guh.getHealth() + ")");
    }

    @GuhTest(template = EMPTY, batch = "voetjes_kaas", timeoutTicks = 120)
    public static void voetjesOpKaassaus(GameTestHelper helper) {
        GuhEntity guh = opDeSaus(helper, ModBlocks.KAAS_SAUS.get().defaultBlockState(), GuhClothes.KAASSAUSLOPERS);
        helper.runAfterDelay(60, () -> {
            staatErBovenop(helper, guh, "kaassaus");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, batch = "voetjes_frituur", timeoutTicks = 120)
    public static void voetjesOpKaasfrituursaus(GameTestHelper helper) {
        GuhEntity guh = opDeSaus(helper, BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState(), GuhClothes.KAASSAUSLOPERS);
        helper.runAfterDelay(60, () -> {
            staatErBovenop(helper, guh, "kaasfrituursaus");
            helper.assertTrue(guh.fireImmune(), "nothing there burns it");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, batch = "voetjes_lava", timeoutTicks = 120)
    public static void voetjesLavalopersOpLava(GameTestHelper helper) {
        GuhEntity guh = opDeSaus(helper, Blocks.LAVA.defaultBlockState(), GuhClothes.LAVALOPERS);
        helper.runAfterDelay(60, () -> {
            staatErBovenop(helper, guh, "lava");
            guh.wear(GuhClothes.KAASSAUSLOPERS);
            helper.assertFalse(Voetjes.vuurvast(guh), "kaassauslopers are not made for real lava");
            guh.discard();
            helper.succeed();
        });
    }

    /** The kleermaker sells the slippers and the rain boots, and upgrades kaassauslopers (+4 magma cream, +1 vads) to lavalopers. */
    @GuhTest(template = EMPTY, batch = "voetjes")
    public static void voetjesKleermaker(GameTestHelper helper) {
        Villager kleermaker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        kleermaker.setVillagerData(kleermaker.getVillagerData().withType(ModVillagers.GUH).withProfession(ModVillagers.GUH_KLEERMAKER));
        KledingKleermaker.zorgVoorAanbod(kleermaker);
        List<GuhClothes> verkocht = kleermaker.getOffers().stream().map(MerchantOffer::getResult)
                .filter(s -> s.getItem() instanceof GuhClothingItem).map(s -> ((GuhClothingItem) s.getItem()).getClothes()).toList();
        helper.assertTrue(verkocht.contains(GuhClothes.PLUISSLOFJES) && verkocht.contains(GuhClothes.REGENLAARSJES), "slippers and rain boots: " + verkocht);

        @SuppressWarnings("removal")
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        KledingUnlocks.wis(p);
        p.getInventory().clearContent();
        helper.assertFalse(VoetjesFeature.waardeerOp(p, kleermaker), "nothing to upgrade yet");
        helper.assertTrue(VoetjesFeature.mist(p).size() == 3, "he misses the lopers, the magma cream and the vads");
        p.getInventory().add(new ItemStack(ModItems.clothingItem(GuhClothes.KAASSAUSLOPERS)));
        p.getInventory().add(new ItemStack(Items.MAGMA_CREAM, 5));
        helper.assertTrue(VoetjesFeature.mist(p).size() == 1, "still the vads");
        p.getInventory().add(new ItemStack(ModItems.VAHOEGE_VADS.get(), 2));
        helper.assertTrue(VoetjesFeature.waardeerOp(p, kleermaker), "upgraded");
        helper.assertTrue(p.getInventory().countItem(ModItems.clothingItem(GuhClothes.LAVALOPERS)) == 1
                && p.getInventory().countItem(ModItems.clothingItem(GuhClothes.KAASSAUSLOPERS)) == 0
                && p.getInventory().countItem(Items.MAGMA_CREAM) == 1 && p.getInventory().countItem(ModItems.VAHOEGE_VADS.get()) == 1,
                "the lopers, 4 magma cream and 1 vads went in, the lavalopers came out");
        helper.assertFalse(VoetjesFeature.waardeerOp(p, kleermaker), "only once (you have them)");
        // already unlocked kaassauslopers count too (the kleermaker copies the pattern)
        KledingUnlocks.wis(p);
        p.getInventory().clearContent();
        KledingUnlocks.ontgrendel(p, GuhClothes.KAASSAUSLOPERS);
        p.getInventory().add(new ItemStack(Items.MAGMA_CREAM, 4));
        p.getInventory().add(new ItemStack(ModItems.VAHOEGE_VADS.get(), 1));
        helper.assertTrue(VoetjesFeature.waardeerOp(p, kleermaker) && KledingUnlocks.heeft(p, GuhClothes.KAASSAUSLOPERS),
                "unlocked lopers: upgraded, and the unlock stays");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
