package nl.juiced.guhs.feature.droomplein;

import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.Wekkerguh;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Droomplein (batch "droomplein"). The game test server has no datapack dimensions, so these check what
 * works without the dream: the template and plein.json (loaded, the right size, a steiger to every island, the Wekkerguh's
 * spot), the Wekkerguh's look, the Droomposthuisje's rules, and a fountain dream.
 */
public class DroompleinGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "droomplein";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** plein.json is read (not the placeholder), its template loads and fits, a steiger per island, a Wekkerguh spot. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void pleinTemplateEnJson(GameTestHelper helper) {
        DroomEilanden.Opzet o = DroomEilanden.opzet(DroomEilanden.PLEIN);
        helper.assertTrue(o.versie() >= 1 && o.template() != null, "plein.json is read (version " + o.versie() + ")");
        StructureTemplate t = helper.getLevel().getStructureManager().get(o.template()).orElse(null);
        helper.assertTrue(t != null, "the template loads: " + o.template());
        Vec3i maat = t.getSize(), max = DroomEilanden.MAX_MAAT;
        helper.assertTrue(maat.getX() <= max.getX() && maat.getY() <= max.getY() && maat.getZ() <= max.getZ(), "fits the island box: " + maat);
        helper.assertTrue(maat.getX() > 40 && maat.getZ() > 40, "a real plein, not empty: " + maat);
        for (String e : DroomEilanden.IDS) {
            if (!e.equals(DroomEilanden.PLEIN)) {
                helper.assertTrue(o.steigers().containsKey(e), "a steiger to " + e);
            }
        }
        helper.assertTrue(o.wekkerguh() != null && o.aankomst() != null, "the Wekkerguh's spot and the arrival");
        helper.assertTrue(o.midden().getX() < maat.getX() && o.midden().getY() < maat.getY() && o.midden().getZ() < maat.getZ(),
                "the centre lies in the template");
        helper.succeed();
    }

    /** The Wekkerguh gets part C's own NPC look (kind WEKKERGUH). */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void wekkerguhInEigenPakje(GameTestHelper helper) {
        Entity e = Wekkerguh.MAKER.apply(helper.getLevel());
        helper.assertTrue(e instanceof GuhNpcEntity n && n.getKind() == GuhNpcEntity.Kind.WEKKERGUH, "the Wekkerguh is the WEKKERGUH npc");
        e.discard();
        helper.succeed();
    }

    /** Outside a dream the post does nothing; only visited islands are known addresses; the fountain always has a dream. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void postEnFontein(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        DroomBezocht.zet(p, DroomEilanden.BADJE, false);
        helper.assertFalse(DroomBrievenbusBlock.post(p, DroomEilanden.BADJE), "no post outside a dream");
        helper.assertTrue(DroomBezocht.bezocht(p, DroomEilanden.PLEIN), "the plein is always known");
        helper.assertFalse(DroomBezocht.bezocht(p, DroomEilanden.BADJE), "the badje not yet");
        DroomBezocht.zet(p, DroomEilanden.BADJE, true);
        helper.assertTrue(DroomBezocht.bezocht(p, DroomEilanden.BADJE), "visited: a known address");
        helper.assertTrue(DroomBezocht.lijst(p).getString().length() > 0, "the plein's bus lists it");
        DroomBezocht.zet(p, DroomEilanden.BADJE, false);
        Component droom = DroomFontein.droom(helper.getLevel().getServer(), p, helper.getLevel().getRandom());
        helper.assertTrue(droom != null && !droom.getString().isBlank(), "the fountain tells a dream");
        helper.assertTrue(DroompleinFeature.BRIEVENBUSSEN.size() == DroomEilanden.IDS.size(), "a brievenbusje per island and the plein");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
