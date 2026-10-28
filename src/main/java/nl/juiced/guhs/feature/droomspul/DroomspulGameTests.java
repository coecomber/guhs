package nl.juiced.guhs.feature.droomspul;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomStart;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the dream's finishing touches (batch "droomspul"): the new boxes are sleeping places, the tuck-in items
 * pick their islands, the islanders' lists are shipped and read, and an islander is put down properly (a Blub with a real
 * draw, a sleeping guh that stays asleep, the Droompostguh).
 */
public class DroomspulGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "droomspul";

    @GuhTest(template = TUIN, batch = BATCH)
    public static void dozenEnInstopSpullen(GameTestHelper helper) {
        for (var b : DroomspulFeature.DOOS_BLOKKEN.values()) {
            helper.assertTrue(b.get().defaultBlockState().is(DromenFeature.SLAAPPLEKKEN), b.getId() + " is a sleeping place");
        }
        helper.assertTrue(DroomStart.eilandVoor(new ItemStack(DroomspulFeature.ZEEPJE.get())).equals(DroomEilanden.BADJE), "zeepje: badje");
        helper.assertTrue(DroomStart.eilandVoor(new ItemStack(DroomspulFeature.STERLAMPJE.get())).equals(DroomEilanden.STERREN), "sterlampje: sterren");
        helper.assertTrue(DroomStart.eilandVoor(new ItemStack(DroomspulFeature.FOTOOTJE.get())).equals(DroomEilanden.HERINNERING), "fotootje: herinnering");
        var maan = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("guhs:maanlampje_maan"));
        helper.assertTrue(DroomStart.eilandVoor(new ItemStack(maan)).equals(DroomEilanden.MOLENTJE), "maanlampje: molentje");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void bewonersLijsten(GameTestHelper helper) {
        var plein = DroomBewoners.lijst(DroomEilanden.PLEIN).bewoners();
        helper.assertTrue(plein.stream().anyMatch(b -> b.soort().equals("postguh")), "the Postguh lives on the plein");
        helper.assertTrue(plein.stream().anyMatch(b -> b.soort().equals("guh_slaapt")), "a guh sleeps in the plein's nestje");
        var liedjes = DroomBewoners.lijst(DroomEilanden.LIEDJES).bewoners();
        helper.assertTrue(liedjes.stream().filter(b -> b.soort().equals("blub")).count() >= 4, "Blubs in the holletjes");
        helper.assertTrue(liedjes.stream().anyMatch(b -> b.soort().equals("guh_slaapt")), "guhs asleep on the open nestjes");
        var json = DroomBewoners.lees(JsonParser.parseString(
                "{\"versie\": 3, \"bewoners\": [{\"soort\": \"blub\", \"plek\": [1, 2, 3], \"yaw\": 90}]}").getAsJsonObject());
        helper.assertTrue(json.versie() == 3 && json.bewoners().get(0).plek().equals(new Vec3(1, 2, 3)), "a list reads");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void bewonersNeerzetten(GameTestHelper helper) {
        Vec3 a = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 3)));
        Entity guh = DroomBewoners.zet(helper.getLevel(), "test", 0, new DroomBewoners.Bewoner("guh_slaapt", Vec3.ZERO, 90), a);
        Entity blub = DroomBewoners.zet(helper.getLevel(), "test", 1, new DroomBewoners.Bewoner("blub", Vec3.ZERO, 0), a.add(3, 0, 0));
        Entity post = DroomBewoners.zet(helper.getLevel(), "test", 2, new DroomBewoners.Bewoner("postguh", Vec3.ZERO, 180), a.add(0, 0, 3));
        helper.assertTrue(guh instanceof GuhEntity, "a guh");
        helper.assertTrue(((GuhEntity) guh).getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0, "it stays in its nestje");
        helper.assertTrue(blub instanceof BlubGuhEntity, "a Blub");
        helper.assertTrue(post instanceof GuhNpcEntity n && n.getKind() == GuhNpcEntity.Kind.POSTGUH, "the Droompostguh");
        for (Entity e : new Entity[] {guh, blub, post}) {
            helper.assertTrue(DroomBewoners.isBewoner(e) && e.getPersistentData().getStringOr(DroomBewoners.ID, "").startsWith("test#"),
                    "tagged as an islander");
        }
        GuhEntity slaper = (GuhEntity) guh;
        helper.succeedWhen(() -> {                                // once it has landed in its spot, it sleeps
            DroomBewoners.slaap(slaper);
            helper.assertTrue(slaper.emotes.current() == Emote.SLAPEN, "the guh sleeps");
            blub.discard();
            post.discard();
            slaper.discard();
        });
    }
}
