package nl.juiced.guhs.feature.slaapkleding;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/** 1.8.0 (U2): the six sleepy guh clothes and the player's droompyjama with its set bonus. */
public class SlaapkledingGameTests {
    private static final String EMPTY = "empty";

    /** Every piece: its slot, an item, a texture, an icon and exactly one source; a guh wears them all at once. */
    @GuhTest(template = EMPTY, batch = "slaapkleding")
    public static void slaapkledingAantrekken(GameTestHelper helper) {
        for (GuhClothes c : SlaapkledingFeature.KLEDING) {
            helper.assertTrue(ModItems.clothingItem(c) instanceof GuhClothingItem, c + " is an item");
            helper.assertTrue(KledingBronnen.bron(c) != null && KledingBronnen.prijs(c) != null, c + " has a source and a price/hint");
            helper.assertTrue(SlaapkledingGameTests.class.getResource("/assets/guhs/textures/entity/guh_clothes/" + c.id() + ".png") != null,
                    c + " has its texture");
            helper.assertTrue(SlaapkledingGameTests.class.getResource("/assets/guhs/textures/item/" + c.id() + ".png") != null, c + " has its icon");
        }
        helper.assertTrue(SlaapkledingFeature.WINKEL.equals(KledingBronnen.bron(GuhClothes.SLAAPMASKER))
                && SlaapkledingFeature.VERHAAL.equals(KledingBronnen.bron(GuhClothes.KNUFFELTJE)), "the shop and Snoes' thank-you");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        for (GuhClothes c : SlaapkledingFeature.KLEDING) {
            guh.wear(c);
        }
        for (GuhClothes c : SlaapkledingFeature.KLEDING) {
            helper.assertTrue(guh.getClothes(c.slot) == c, "the guh wears " + c + " (" + c.slot + ")");
        }
        helper.succeed();
    }

    /** The droompyjama: one point of armour per piece, and the set bonus only with all four pieces on. */
    @GuhTest(template = EMPTY, batch = "slaapkleding")
    public static void droompyjamaSetbonus(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(!Droompyjama.draagtSet(p) && Droompyjama.stukken(p) == 0, "nothing on: no bonus");
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Droompyjama.MUTS.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Droompyjama.JASJE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Droompyjama.BROEK.get()));
        helper.assertTrue(!Droompyjama.draagtSet(p) && Droompyjama.stukken(p) == 3, "three pieces: not yet");
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Droompyjama.SLOFFEN.get()));
        helper.assertTrue(Droompyjama.draagtSet(p) && Droompyjama.stukken(p) == 4, "the whole set: the dream bonus");
        helper.assertTrue(Droompyjama.VRAAG_TICKS < nl.juiced.guhs.feature.dromen.DroomSlapen.VRAAG_TICKS && Droompyjama.FADE_FACTOR < 1f,
                "the bonus: a sooner bed question and a quicker fade");
        helper.assertTrue(Droompyjama.MATERIAAL.defense().values().stream().allMatch(v -> v == 1), "one soft point of armour per piece");
        helper.succeed();
    }
}
