package nl.juiced.guhs.feature.vadsrijk.ftb;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.huizen.Kwartier;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakters;
import nl.juiced.guhs.feature.vadsrijk.karakter.Modus;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Y-FTB: the compact FTB chapter "Het Vadsrijk". The tasks tick off on the real triggers (a zone's last main quest, the Knuffelkamp
 * training, the raid, all secrets), for any character and any game mode; the zone gate shows "Zone N is open!"; FTB rewards are
 * refused in the Vadsrijk and allowed in the overworld, the Guhmensie and the Knuffelkwartier; the 14 deco rewards exist, can be
 * placed in survival (not in the Vadsrijk) and carry their tooltip; the chapter file has 14 + 5 quests with only deco rewards.
 * (FTB Quests itself is not loaded in gametests: the mixin's decision {@link FtbClaimRegel#weiger} is what is tested.)
 * Run: {@code runGameTestServer -Pgt=VadsrijkFtb}.
 */
public class VadsrijkFtbGameTests {
    private static final String BATCH = "vadsrijk_ftb";
    static final Map<Integer, String> EIND = Map.of(1, "hq12_schaduw", 2, "z2_hq12_hartsplinter", 3, "z3_hq15_hoogvads",
            4, "z4_hq07_opstijgpier", 5, "z5_hq10_stormrand", 6, "z6_hq10_moederoase");

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Questlog.wisAlles(p);
        for (String a : VrFtb.alle()) {
            VrFtb.neemTerug(p, a);
        }
        return p;
    }

    /** The last main quest of each zone is what tools/features/vadsrijk_y_ftb.py (EIND) and the wiki say. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eindQuestPerZone(GameTestHelper helper) {
        for (int z = 1; z <= 6; z++) {
            String e = VrFtb.eindQuest(z);
            helper.assertTrue(EIND.get(z).equals(e), "zone " + z + ": last main quest " + EIND.get(z) + ", found " + e);
            helper.assertTrue(Quests.van(e) != null && Quests.van(e).hoofd(), "it is a main quest: " + e);
        }
        helper.assertTrue(Quests.van(VrFtb.REKRUUT_QUEST) != null && Quests.van(VrFtb.REKRUUT_QUEST).hoofd(), "the recruit quest exists");
        helper.succeed();
    }

    /**
     * The real triggers: handing in a zone's last main quest ticks "Zone N uitgespeeld" (the hand-in hook, at once); the Knuffelkamp
     * training ticks the recruit; earlier main quests tick nothing. In survival, adventure and creative.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void hoofdquestEindeVinktAf(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            Questlog.voltooi(p, Quests.van("hq11_opperzwam"));
            helper.assertTrue(!VrFtb.heeft(p, "vr_ftb_zone1"), "the second-last main quest does not tick zone 1");
            Questlog.voltooi(p, Quests.van(VrFtb.REKRUUT_QUEST));
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_rekruut"), "the Knuffelkamp training ticks 'Word rekruut'");
            GameType[] modi = {GameType.SURVIVAL, GameType.ADVENTURE, GameType.CREATIVE, GameType.SURVIVAL, GameType.ADVENTURE, GameType.CREATIVE};
            for (int z = 1; z <= 6; z++) {
                p.setGameMode(modi[z - 1]);
                Quest q = Quests.van(EIND.get(z));
                helper.assertTrue(!VrFtb.heeft(p, "vr_ftb_zone" + z), "zone " + z + " not ticked yet");
                Questlog.voltooi(p, q);
                helper.assertTrue(VrFtb.heeft(p, "vr_ftb_zone" + z), "zone " + z + " ticked at once by the hand-in (" + modi[z - 1] + ")");
            }
            // a quest that was already done before (an older save / a dev command without the hook): the interval check catches it
            VrFtb.neemTerug(p, "vr_ftb_zone3");
            helper.assertTrue(VrFtb.controleer(p) >= 1 && VrFtb.heeft(p, "vr_ftb_zone3"), "the periodic check ticks a zone that was already done");
        } finally {
            p.setGameMode(GameType.SURVIVAL);
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** Any character counts: zone 1 on the Normaal knight, zone 2 on a new Eénvads knight (empty quest log); both stay ticked. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void elkKarakterTelt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            helper.assertTrue(Karakter.modus(p) == Modus.NORMAAL, "slot 0 = Normaal");
            Questlog.voltooi(p, Quests.van(EIND.get(1)));
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_zone1"), "zone 1 on the Normaal knight");
            helper.assertTrue(Karakters.maak(p, 2, "Eenvads Test") && Karakters.wissel(p, 2, true), "a new Eénvads knight, switched to");
            helper.assertTrue(Karakter.modus(p) == Modus.EENVADS && !Questlog.klaar(p, EIND.get(1)), "his quest log is empty");
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_zone1") && VrFtb.controleer(p) == 0, "zone 1 stays ticked (the player has it, not the knight)");
            Questlog.voltooi(p, Quests.van(EIND.get(2)));
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_zone2"), "zone 2 on the Eénvads knight");
            helper.assertTrue(Karakters.wissel(p, 0, true) && VrFtb.heeft(p, "vr_ftb_zone2"), "back on the Normaal knight: zone 2 stays ticked");
        } finally {
            if (Karakter.slot(p) != 0) {
                Karakters.wissel(p, 0, true);
            }
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** The raid: the clear hook ticks it; so does the raid's own "first clear" flag (checked periodically). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void raidEersteClear(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            Spelers.data(p).extra("z6raid").remove("clear");
            VrFtb.controleer(p);
            helper.assertTrue(!VrFtb.heeft(p, "vr_ftb_raid"), "no clear, no tick");
            VrFtb.raidOpgevrolijkt(p);
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_raid"), "Ringeling cheered up: ticked");
            VrFtb.neemTerug(p, "vr_ftb_raid");
            Spelers.data(p).extra("z6raid").putBoolean("clear", true);
            VrFtb.controleer(p);
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_raid"), "the raid's 'clear' flag (an earlier clear) ticks it too");
        } finally {
            Spelers.data(p).extra("z6raid").remove("clear");
        }
        helper.succeed();
    }

    /** All secrets of zone 1 (secrets, lost knuffels, puzzles of the Verkenningsboek page): ticked only when the last one is found. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void geheimenZone1(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            int[] g = VrFtb.geheimen(p, 1);
            helper.assertTrue(g[1] > 5 && g[0] == 0, "zone 1 has secrets to find: " + g[0] + "/" + g[1]);
            List<Voltooiing.Ding> dingen = new ArrayList<>();
            for (Voltooiing.Categorie c : Voltooiing.categorieen(Voltooiing.zone("zone1"))) {
                if (!c.later() && VrFtb.GEHEIM_CATS.contains(c.id())) {
                    dingen.addAll(c.dingen());
                }
            }
            for (int i = 0; i < dingen.size(); i++) {
                vind(p, dingen.get(i).id());
                VrFtb.controleer(p);
                boolean laatste = i == dingen.size() - 1;
                helper.assertTrue(VrFtb.heeft(p, "vr_ftb_geheimen1") == laatste,
                        "after " + (i + 1) + "/" + dingen.size() + " (" + dingen.get(i).id() + "): ticked = " + laatste);
            }
            helper.assertTrue(!VrFtb.heeft(p, "vr_ftb_geheimen2"), "zone 2's secrets are another quest");
        } finally {
            Spelers.data(p).extra.remove("puzzels");
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** Marks one secret of a zone page found, by its id ("knuffels_x", "aanwijzingen_x", "schatten_x", "pz_x"). */
    private static void vind(ServerPlayer p, String id) {
        if (id.startsWith("pz_")) {
            CompoundTag d = Spelers.data(p).extra("puzzels");
            if (!(d.get("opgelost") instanceof CompoundTag)) {
                d.put("opgelost", new CompoundTag());
            }
            ((CompoundTag) d.get("opgelost")).putBoolean(id.substring(3), true);
            return;
        }
        for (String map : new String[] {"knuffels", "aanwijzingen", "schatten"}) {
            if (id.startsWith(map + "_")) {
                Questlog.map(p, map).putBoolean(id.substring(map.length() + 1), true);
                return;
            }
        }
        throw new IllegalStateException("unknown kind of secret on the zone-1 page: " + id);
    }

    /** The zone gate: "Zone N is open!" for exactly the open zones (2..open), never taken back. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zonepoort(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            ZonePoortConfig.TEST = 1;
            VrFtb.controleer(p);
            for (int z = 2; z <= 6; z++) {
                helper.assertTrue(!VrFtb.heeft(p, "vr_ftb_poort" + z), "gate 1: zone " + z + " stays hidden");
            }
            ZonePoortConfig.TEST = 3;
            VrFtb.controleer(p);
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_poort2") && VrFtb.heeft(p, "vr_ftb_poort3") && !VrFtb.heeft(p, "vr_ftb_poort4"),
                    "gate 3: zones 2 and 3 appear, 4 not");
            ZonePoortConfig.TEST = 6;
            VrFtb.controleer(p);
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_poort6"), "gate 6: everything");
            ZonePoortConfig.TEST = 1;
            VrFtb.controleer(p);
            helper.assertTrue(VrFtb.heeft(p, "vr_ftb_poort6"), "a lowered gate takes nothing back");
        } finally {
            ZonePoortConfig.TEST = null;
        }
        helper.succeed();
    }

    /** The claim rule: refused in the Vadsrijk (with the message), allowed in the overworld, the Guhmensie and the Knuffelkwartier. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void claimAlleenBuitenHetVadsrijk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerLevel thuis = (ServerLevel) p.level();
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        helper.assertTrue(vr != null, "the Vadsrijk level is there");
        FtbClaimRegel.vergeet();
        int voor = FtbClaimRegel.geweigerd;
        helper.assertTrue(thuis.dimension() == Level.OVERWORLD && FtbClaimRegel.magClaimen(p) && !FtbClaimRegel.weiger(p),
                "the overworld: claim allowed");
        for (ServerLevel l : new ServerLevel[] {helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION),
                helper.getLevel().getServer().getLevel(Kwartier.DIM), helper.getLevel().getServer().getLevel(Level.NETHER)}) {
            if (l == null) {
                continue;
            }
            p.teleportTo(l, 0.5, 200, 0.5, Set.<Relative>of(), 0, 0, true);
            helper.assertTrue(p.level() == l && !FtbClaimRegel.weiger(p), l.dimension().identifier() + ": claim allowed");
        }
        p.teleportTo(vr, 0.5, 300, 0.5, Set.<Relative>of(), 0, 0, true);
        try {
            helper.assertTrue(Vadsrijk.in(p), "the player is in the Vadsrijk");
            helper.assertTrue(!FtbClaimRegel.magClaimen(p) && FtbClaimRegel.weiger(p), "the Vadsrijk: claim refused");
            helper.assertTrue(FtbClaimRegel.weiger(p) && FtbClaimRegel.weiger(p), "claim all: every reward refused (one message)");
            helper.assertTrue(FtbClaimRegel.geweigerd - voor == 3, "three refusals counted: " + (FtbClaimRegel.geweigerd - voor));
        } finally {
            p.teleportTo(thuis, p.getX(), 100, p.getZ(), Set.<Relative>of(), 0, 0, true);
        }
        helper.assertTrue(FtbClaimRegel.magClaimen(p) && !FtbClaimRegel.weiger(p), "home again: claim allowed");
        helper.assertTrue(FtbClaimRegel.magClaimen(null), "no player (a server-side claim): not ours to refuse");
        helper.succeed();
    }

    /** The 14 deco rewards: block + item, placeable by a survival player (facing him), with the tooltip; not in the Vadsrijk. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void decoBlokken(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        JsonObject nl = lang("nl_nl");
        helper.assertTrue(VrFtbRegistry.TROFEEEN.size() == 14 && VrFtbRegistry.ITEMS_PER_ID.size() == 14, "14 trophies");
        ServerLevel vr = Vadsrijk.level(helper.getLevel().getServer());
        int x = 0;
        for (Map.Entry<String, DeferredBlock<TrofeeBlock>> e : VrFtbRegistry.TROFEEEN.entrySet()) {
            String id = e.getKey();
            helper.assertTrue(!id.startsWith("vr_"), id + ": not a 'vr_' id (those count as Vadsrijk items)");
            DeferredItem<BlockItem> item = VrFtbRegistry.ITEMS_PER_ID.get(id);
            helper.assertTrue(item.get().getBlock() == e.getValue().get(), id + ": its item places it");
            // the tooltip
            List<Component> regels = new ArrayList<>();
            ItemStack st = new ItemStack(item.get());
            item.get().appendHoverText(st, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, regels::add, TooltipFlag.NORMAL);
            String key = "block.guhs." + id + ".lore";
            helper.assertTrue(!regels.isEmpty() && regels.get(0).getContents() instanceof TranslatableContents t && t.getKey().equals(key),
                    id + ": tooltip " + key);
            helper.assertTrue(nl.has(key) && nl.get(key).getAsString().startsWith("Beloning: ") && nl.has("block.guhs." + id),
                    id + ": Dutch name + 'Beloning: ...' line");
            if (id.startsWith("vadsrijk_trofee_zone")) {
                String n = id.substring("vadsrijk_trofee_zone".length());
                helper.assertTrue(nl.get(key).getAsString().equals("Beloning: Zone " + n + " van Het Vadsrijk uitgespeeld"), id + ": " + nl.get(key));
            }
            // placed by a survival player on the floor of the test room
            BlockPos vloer = new BlockPos(1 + x, 1, 2);
            helper.setBlock(vloer, Blocks.STONE);
            BlockPos abs = helper.absolutePos(vloer);
            ItemStack hand = new ItemStack(item.get(), 2);
            p.setItemInHand(InteractionHand.MAIN_HAND, hand);
            item.get().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            helper.assertBlockPresent(e.getValue().get(), vloer.above());
            helper.assertTrue(p.getMainHandItem().getCount() == 1, id + ": survival uses one up");
            helper.assertTrue(helper.getBlockState(vloer.above()).getValue(TrofeeBlock.FACING) == p.getDirection().getOpposite(), id + ": faces the player");
            // not in the Vadsrijk
            if (vr != null) {
                BlockPos ergens = new BlockPos(0, 200, 0);
                BlockPlaceContext ctx = new BlockPlaceContext(vr, p, InteractionHand.MAIN_HAND, new ItemStack(item.get()),
                        new BlockHitResult(Vec3.atCenterOf(ergens), Direction.UP, ergens, false));
                helper.assertTrue(e.getValue().get().getStateForPlacement(ctx) == null, id + ": cannot be placed in the Vadsrijk");
            }
            x++;
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.succeed();
    }

    /** The chapter file: 1 intro + 14 quests + 5 gate quests, rewards are only our deco blocks (no kaasknabbels, no xp). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void hoofdstukBestand(GameTestHelper helper) {
        String h = tekst("ftbquests/chapters/guhs_vadsrijk.json5");
        helper.assertTrue(h != null, "the chapter ships");
        int quests = (int) java.util.regex.Pattern.compile("(?m)^\\s*shape:|^\\s*\"?icon\"?\\s*:").matcher(h).results().count();
        helper.assertTrue(!h.contains("kaas_knabbels") && !h.contains("type: \"xp\"") && !h.contains("\"type\": \"xp\""), "no kaasknabbels, no xp");
        for (String id : VrFtbRegistry.TROFEEEN.keySet()) {
            helper.assertTrue(h.contains("guhs:" + id), "the reward " + id + " is in the chapter");
        }
        int invisible = (int) java.util.regex.Pattern.compile("(?m)^\\s*\"?invisible\"?\\s*:\\s*true").matcher(h).results().count();
        helper.assertTrue(invisible == 5, "five gate quests stay invisible until their zone opens: " + invisible);
        int verborgen = (int) java.util.regex.Pattern.compile("(?m)^\\s*\"?hide_until_deps_complete\"?\\s*:\\s*true").matcher(h).results().count();
        helper.assertTrue(verborgen == 11, "11 quests hidden behind the gate: " + verborgen + " (icons/lines: " + quests + ")");
        helper.succeed();
    }

    private static String tekst(String pad) {
        try (InputStream in = VadsrijkFtbGameTests.class.getClassLoader().getResourceAsStream(pad)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static JsonObject lang(String taal) {
        String t = tekst("assets/guhs/lang/" + taal + ".json");
        return t == null ? new JsonObject() : JsonParser.parseString(t).getAsJsonObject();
    }
}
