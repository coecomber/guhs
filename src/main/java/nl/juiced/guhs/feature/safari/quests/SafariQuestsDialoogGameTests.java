package nl.juiced.guhs.feature.safari.quests;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Test round 4 (design ronde 49): the safari quests talk in the dialog window (never the chat) and the quest tracker shows
 * where you are.
 */
public class SafariQuestsDialoogGameTests {
    private static final String BATCH = "safari";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "safari_r4_test"));
        BlockPos b = helper.absolutePos(new BlockPos(1, 2, 1));
        p.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        return p;
    }

    private static void opruimen(GameTestHelper helper, ServerPlayer p) {
        SafariDialoog.vergeet(p);
        SafariSpeler.wis(helper.getLevel().getServer(), p.getUUID());
        KledingUnlocks.wis(p);
        for (Entity e : helper.getLevel().getEntities((Entity) null, new AABB(p.blockPosition()).inflate(8), e -> e instanceof GuhEntity)) {
            e.discard();
        }
    }

    private static String sleutel(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    private static Object[] args(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getArgs() : new Object[0];
    }

    /** The quests talk in the dialog: the ranger asks, the disguised guh answers, each a page of one scene. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void questsPratenInDialoog(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        var server = helper.getLevel().getServer();
        helper.assertTrue(SafariQuests.hoofdranger(p, null), "the ranger talks about the quest the first time");
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).equals("gevraagd"), "asked");
        helper.assertTrue(!SafariQuests.hoofdranger(p, null), "the second time his own menu comes");
        VermomdeGuh.vind(p, 0, null);
        List<String> pag = SafariDialoog.wachtend(p);
        helper.assertTrue(pag.equals(List.of("gui.guhs.safari.vermomd.ranger", "gui.guhs.safari.vermomd.ranger_2", "gui.guhs.safari.vermomd.1")),
                "three pages waiting, got " + pag);
        SafariDialoog.vergeet(p);
        VaheOlifant.praat(p, net.minecraft.world.item.ItemStack.EMPTY);
        helper.assertTrue(SafariDialoog.wachtend(p).contains("gui.guhs.safari.olifant.start"), "Dikkie talks in the dialog too");
        opruimen(helper, p);
        helper.succeed();
    }

    /** The tracker: one line per busy quest, with where to look. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void questTrackerTekst(GameTestHelper helper) {
        CompoundTag stand = new CompoundTag();
        helper.assertTrue(QuestTracker.regels(stand).isEmpty(), "nothing busy: no tracker");
        CompoundTag q = new CompoundTag();
        q.putString(VermomdeGuh.QUEST, "1");
        stand.put("Quests", q);
        List<Component> r = QuestTracker.regels(stand);
        helper.assertTrue(r.size() == 1 && sleutel(r.get(0)).equals("gui.guhs.safari.tracker.vermomd"), "the disguised guh line");
        Object[] a = args(r.get(0));
        helper.assertTrue(a.length == 3 && a[0].equals(1) && a[1].equals(3) && a[2] instanceof Component w
                && sleutel(w).equals("gui.guhs.safari.tracker.waar.reuzenpanda"), "1/3, look with the pandas");
        q.putString(VaheOlifant.QUEST, "dieet");
        q.putString(VaheOlifant.GEGETEN, "appel");
        q.putString(VermomdeGuh.QUEST, "klaar");
        r = QuestTracker.regels(stand);
        helper.assertTrue(r.size() == 1 && sleutel(r.get(0)).equals("gui.guhs.safari.tracker.dieet"), "only Dikkie's diet is busy");
        String nog = r.get(0).getString();
        helper.assertTrue(!nog.contains("gui.guhs.safari.olifant.appel"), "the apple is eaten: " + nog);
        q.putString(VaheOlifant.QUEST, "loopband");
        q.putString(VaheOlifant.RONDEN, "2");
        r = QuestTracker.regels(stand);
        helper.assertTrue(sleutel(r.get(0)).equals("gui.guhs.safari.tracker.loopband") && args(r.get(0))[0].equals(2), "2 laps");
        List<QuestTracker.Quest> o = QuestTracker.overzicht(stand);
        helper.assertTrue(o.size() == 3 && o.get(0).klaar() && !o.get(1).klaar() && !o.get(2).klaar(), "the overview: vermomd done");
        helper.succeed();
    }
}
