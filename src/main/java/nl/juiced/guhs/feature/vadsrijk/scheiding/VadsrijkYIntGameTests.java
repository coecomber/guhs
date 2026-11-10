package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten;
import nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Post;
import nl.juiced.guhs.feature.vadsrijk.huizen.Menus;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Y-INT: the two user decisions of round Y. Run: {@code runGameTestServer -Pgt=VadsrijkYInt}.
 * <ol>
 *   <li>Vadsmunten only buy Vadsrijk deco and clothing for home: Mollie's list and everything the Post can deliver pass
 *   {@link ThuisWaren}; vanilla building blocks don't, and the Post keeps them on the Vadsrijk side.</li>
 *   <li>FTB Essentials' virtual work blocks ({@code /anvil /crafting /smithing /stonecutter}) are refused inside for non-ops,
 *   end to end through the real command dispatcher (a stand-in command with the same name, since FTB Essentials is not loaded
 *   on the gametest server).</li>
 * </ol>
 */
public class VadsrijkYIntGameTests {
    private static final String BATCH = "vadsrijk_y_int";

    @GuhTest(template = "empty", batch = BATCH)
    public static void molliePostAlleenDeco(GameTestHelper helper) {
        for (ItemStack s : Menus.mollieWaren()) {
            helper.assertTrue(ThuisWaren.mag(s), "Mollie only sells Vadsrijk deco / Guhs furniture: " + s);
        }
        helper.assertTrue(Menus.mollieWaren().size() == 7, "Mollie has 7 wares left, not " + Menus.mollieWaren().size());
        for (ItemStack s : List.of(new ItemStack(Items.CHERRY_PLANKS), new ItemStack(Items.BOOKSHELF), new ItemStack(Items.LANTERN),
                new ItemStack(Items.PINK_BED), new ItemStack(Items.PINK_STAINED_GLASS_PANE), new ItemStack(Items.PINK_CARPET),
                new ItemStack(Items.FLOWER_POT), new ItemStack(ModBlocks.GUH_TAART.get()), new ItemStack(Items.DIAMOND))) {
            helper.assertTrue(!ThuisWaren.mag(s), "not Vadsrijk deco/clothing, never bought home: " + s);
            helper.assertTrue(Menus.mollieWaren().stream().noneMatch(m -> m.is(s.getItem())), "Mollie no longer sells " + s);
        }
        // everything the Post is fed today: Knutselwinkel/Werkplaats post goods + ambacht keepsakes (Recepten.POST), the market's
        // clothing + deco (Cosmetica.KLEDING, EcoSpul.DECO), the Knuffelmeter's heart glasses
        for (var e : Recepten.POST.entrySet()) {
            helper.assertTrue(ThuisWaren.mag(e.getValue().get()), "Post good '" + e.getKey() + "' is deco or clothing");
        }
        for (GuhClothes c : Cosmetica.KLEDING) {
            helper.assertTrue(ThuisWaren.mag(Cosmetica.kledingItem(c)), "market clothing " + c.id());
        }
        for (var d : EcoSpul.DECO) {
            helper.assertTrue(ThuisWaren.mag(new ItemStack(d.get())), "market deco " + d.getId());
        }
        helper.assertTrue(ThuisWaren.mag(Cosmetica.kledingItem(GuhClothes.HEART_GLASSES)), "the Knuffelmeter's heart glasses");
        // the Post's own guard: a building block is NOT queued for home
        ServerPlayer p = VadsrijkScheidingGameTests.speler(helper);
        int voor = Post.aantal(p);
        Post.stuur(p, new ItemStack(Items.CHERRY_PLANKS, 32));
        helper.assertTrue(Post.aantal(p) == voor, "cherry planks never go into the Post");
        helper.assertTrue(p.getInventory().countItem(Items.CHERRY_PLANKS) == 32, "...they stay with the player on the Vadsrijk side");
        Post.stuur(p, new ItemStack(EcoSpul.VADSRIJK_VAANDEL.get()));
        helper.assertTrue(Post.aantal(p) == voor + 1, "the Vadsrijk banner does go by Post");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void werkbankCommandosBinnen(GameTestHelper helper) {
        for (String c : new String[] {"anvil", "crafting", "smithing", "stonecutter"}) {
            helper.assertTrue(Grens.GEWEIGERD.contains(Grens.wortel(List.of(c))), "/" + c + " is refused inside");
            helper.assertTrue(Grens.GEWEIGERD.contains(Grens.wortel(List.of("ftbessentials", c))), "/ftbessentials " + c + " too");
        }
        helper.assertTrue(Grens.GEWEIGERD.containsAll(Grens.WERKBANKEN), "every work block is in GEWEIGERD");
        helper.assertTrue(Grens.GEWEIGERD.contains(Grens.wortel(List.of("open", "anvil"))), "/open anvil (FTB Essentials 26.1.2.4) is refused inside");
        // end to end: a stand-in /stonecutter on the real dispatcher (FTB Essentials is not loaded here)
        var server = helper.getLevel().getServer();
        int[] gedraaid = {0};
        server.getCommands().getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("stonecutter").executes(ctx -> {
            gedraaid[0]++;
            return 1;
        }));
        ServerPlayer p = VadsrijkScheidingGameTests.speler(helper);
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "stonecutter");
        helper.assertTrue(gedraaid[0] == 1, "at home /stonecutter works");
        VadsrijkScheidingGameTests.naarRijk(p);
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "stonecutter");
        helper.assertTrue(gedraaid[0] == 1, "inside the Vadsrijk /stonecutter is refused");
        VadsrijkScheidingGameTests.naarThuis(p);
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "stonecutter");
        helper.assertTrue(gedraaid[0] == 2, "back home it works again");
        helper.succeed();
    }
}
