package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.opslag.Opslag;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagData;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1101-balans: the one-time cleanup {@link LootOpruimen} ({@code /guhs vadsrijk loot opruimen}). A gate-1 player (no story past
 * zone 1) with a zone-5 named item (Gorgonzola's moersleutel) and a zone-3 piece in his inventory / stash / stored Vadsrijk
 * inventory / bank / post, next to a zone-1 piece and the zone-1 named Klodderknots that must stay.
 * Run: {@code runGameTestServer -Pgt=VadsrijkLootOpruimen}.
 */
public class VadsrijkLootOpruimenGameTests {
    private static final String BATCH = "vadsrijk_loot_opruimen";
    static final String MOER = "z5baas_moersleutel", KNOTS = "baas_klodderknots";

    /** The first base type of the zone's pool that really belongs to that zone (zoneVan == zone) and has an item. */
    @Nullable
    static String vanZone(int zone) {
        for (String id : ZonePools.ids(zone)) {
            BasisType t = BasisTypes.van(id);
            if (t != null && ZonePools.zoneVan(id) == zone && !Rollen.stack(Rollen.rol(t, 1, Zeldzaamheid.GEWOON, RandomSource.create(1))).isEmpty()) {
                return id;
            }
        }
        return null;
    }

    static ItemStack stuk(String id, Zeldzaamheid z, int level, RandomSource r) {
        BasisType t = BasisTypes.van(id);
        return t == null ? ItemStack.EMPTY : Rollen.stack(Rollen.rol(t, level, z, r));
    }

    static CompoundTag metSlot(ServerPlayer p, ItemStack s, int slot) {
        return (CompoundTag) ItemStackWithSlot.CODEC.encodeStart(p.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                new ItemStackWithSlot(slot, s)).getOrThrow();
    }

    static long telType(List<CompoundTag> tags, String id) {
        return tags.stream().map(LootOpruimen::gear).filter(u -> u != null && u.type().equals(id)).count();
    }

    /** The set-up: returns {zone-3 id, zone-1 id}. 5 pieces are out of tier, 4 are fine. */
    static String[] vul(GameTestHelper helper, ServerPlayer p) {
        String z3 = vanZone(3), z1 = vanZone(1);
        helper.assertTrue(z3 != null && z1 != null, "a zone-3 and a zone-1 base type exist: " + z3 + " / " + z1);
        helper.assertTrue(ZonePools.zoneVan(MOER) == 5 && ZonePools.zoneVan(KNOTS) == 1, "named items by prefix: moersleutel 5, klodderknots 1");
        RandomSource r = RandomSource.create(1101);
        ItemStack moer = stuk(MOER, Zeldzaamheid.LEGENDARISCH, 52, r), drie = stuk(z3, Zeldzaamheid.ZELDZAAM, 35, r);
        ItemStack een = stuk(z1, Zeldzaamheid.GEWOON, 4, r), knots = stuk(KNOTS, Zeldzaamheid.LEGENDARISCH, 15, r);
        helper.assertTrue(!moer.isEmpty() && !drie.isEmpty() && !een.isEmpty() && !knots.isEmpty(), "all four stacks exist");
        SpelerData d = Spelers.data(p);
        d.extra.remove("npc");   // a gate-1 player: no story past zone 1
        p.getInventory().clearContent();
        d.rijkInventaris.clear();
        d.gevonden.clear();
        d.extra("economie").remove("post");
        p.getInventory().setItem(0, moer.copy());                 // flagged: live inventory
        p.getInventory().setItem(1, een.copy());                  // fine
        Inventaris.bewaar(p, drie.copy());                         // flagged: the stash (gevonden)
        d.rijkInventaris.add(metSlot(p, moer.copy(), 3));          // flagged: stored Vadsrijk inventory
        d.rijkInventaris.add(metSlot(p, knots.copy(), 4));         // fine (zone-1 named boss item)
        OpslagData o = Opslag.data(p);
        o.bankTab(0).setItem(0, drie.copy());                      // flagged: bank
        o.bankTab(0).setItem(1, een.copy());                       // fine
        o.zak("1").tab(0).setItem(0, knots.copy());                // fine: knapzak
        ListTag post = new ListTag();
        post.add(EcoUtil.tag(p.registryAccess(), drie.copy()));    // flagged: the Post
        d.extra("economie").put("post", post);
        return new String[] {z3, z1};
    }

    /** The rule: a player's open zone from his story, capped by the server. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void hoogsteOpenZone(GameTestHelper helper) {
        helper.assertTrue(LootOpruimen.hoogsteOpen(null, 6) == 1 && LootOpruimen.hoogsteOpen(new CompoundTag(), 6) == 1, "no story: zone 1");
        CompoundTag npc = new CompoundTag();
        npc.putBoolean("zone1_klaar", true);
        helper.assertTrue(LootOpruimen.hoogsteOpen(npc, 6) == 2 && LootOpruimen.hoogsteOpen(npc, 1) == 1, "zone 1 done: 2, but never above the server");
        CompoundTag q = new CompoundTag();
        CompoundTag st = new CompoundTag();
        st.putInt("s", 1);
        q.put("z3_hq01_test", st);
        npc.put("q", q);
        helper.assertTrue(LootOpruimen.hoogsteOpen(npc, 6) == 3 && LootOpruimen.hoogsteOpen(npc, 0) == 1, "a zone-3 quest started: 3 (server 0 -> 1)");
        Uitrusting moer = Rollen.rol(BasisTypes.van(MOER), 50, Zeldzaamheid.LEGENDARISCH, RandomSource.create(2));
        helper.assertTrue(LootOpruimen.teHoog(moer, 4) && !LootOpruimen.teHoog(moer, 5), "moersleutel: out of tier below zone 5 only");
        helper.succeed();
    }

    /** Dry run: finds exactly the 5 out-of-tier pieces (moersleutel, zone-3) and removes nothing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void proefrunVindtMaarLaatStaan(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        String[] ids = vul(helper, p);
        LootOpruimen.Resultaat r = LootOpruimen.scanOnline(p, 6);
        helper.assertTrue(r.open() == 1, "gate-1 player: open zone 1, got " + r.open());
        List<LootOpruimen.Vondst> v = r.vondsten();
        helper.assertTrue(v.size() == 5, "5 findings, got " + v.size() + ": " + v.stream().map(x -> x.plek() + "=" + x.type()).toList());
        helper.assertTrue(v.stream().allMatch(x -> x.type().equals(MOER) || x.type().equals(ids[0])), "only the moersleutel and the zone-3 piece");
        helper.assertTrue(v.stream().noneMatch(x -> x.type().equals(ids[1]) || x.type().equals(KNOTS)), "the zone-1 piece and the Klodderknots stay");
        helper.assertTrue(v.stream().filter(x -> x.type().equals(MOER)).allMatch(x -> x.itemZone() == 5 && x.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH), "moersleutel: zone 5");
        helper.assertTrue(v.stream().anyMatch(x -> x.plek().startsWith("kist")) && v.stream().anyMatch(x -> x.plek().contains("bank"))
                && v.stream().anyMatch(x -> x.plek().contains("post")) && v.stream().anyMatch(x -> x.plek().startsWith("Vadsrijk-inventaris"))
                && v.stream().anyMatch(x -> x.plek().startsWith("inventaris")), "every place found: " + v.stream().map(LootOpruimen.Vondst::plek).toList());
        SpelerData d = Spelers.data(p);
        helper.assertTrue(Rollen.van(p.getInventory().getItem(0)) != null && d.rijkInventaris.size() == 2 && telType(d.gevonden, ids[0]) == 1
                && !Opslag.data(p).bankTab(0).getItem(0).isEmpty() && d.extra("economie").getListOrEmpty("post").size() == 1, "nothing removed");
        helper.succeed();
    }

    /** --echt: removes exactly the flagged pieces; a second scan finds nothing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void echtVerwijdertPrecies(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        String[] ids = vul(helper, p);
        LootOpruimen.Resultaat r = LootOpruimen.scanOnline(p, 6);
        helper.assertTrue(LootOpruimen.verwijder(r.vondsten()) == 5, "5 removed");
        SpelerData d = Spelers.data(p);
        OpslagData o = Opslag.data(p);
        helper.assertTrue(p.getInventory().getItem(0).isEmpty(), "moersleutel out of the inventory");
        Uitrusting een = Rollen.van(p.getInventory().getItem(1));
        helper.assertTrue(een != null && een.type().equals(ids[1]), "zone-1 piece still in the inventory");
        helper.assertTrue(telType(d.gevonden, ids[0]) == 0, "zone-3 piece out of the stash");
        helper.assertTrue(d.rijkInventaris.size() == 1 && telType(d.rijkInventaris, KNOTS) == 1, "only the Klodderknots left in the stored Vadsrijk inventory");
        helper.assertTrue(o.bankTab(0).getItem(0).isEmpty() && Rollen.van(o.bankTab(0).getItem(1)) != null, "bank: zone-3 gone, zone-1 stays");
        helper.assertTrue(Rollen.van(o.zak("1").tab(0).getItem(0)) != null, "knapzak Klodderknots stays");
        helper.assertTrue(d.extra("economie").getListOrEmpty("post").isEmpty(), "post emptied");
        helper.assertTrue(LootOpruimen.scanOnline(p, 6).vondsten().isEmpty(), "a second scan finds nothing");
        helper.succeed();
    }

    /**
     * Offline: the same player saved as player.dat (attachments included) gives the same 5 findings, removes them from the TAG
     * only; an inactive character is judged by its own story (zone 4: its zone-3 piece stays, the moersleutel goes).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void offlineSpelerbestand(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        String[] ids = vul(helper, p);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.registryAccess());
        p.saveWithoutId(out);
        CompoundTag dat = out.buildResult();
        helper.assertTrue(LootOpruimen.heeftVadsrijk(dat), "player.dat has the Vadsrijk attachments");
        // an inactive character (T-KARAKTER) whose story reached zone 4
        CompoundTag kern = dat.getCompoundOrEmpty(LootOpruimen.NEOFORGE_ATTACHMENTS).getCompoundOrEmpty(KernRegistry.SPELER.getId().toString());
        CompoundTag extra = kern.getCompoundOrEmpty("extra");
        CompoundTag npc = new CompoundTag();
        npc.putBoolean("zone3_klaar", true);
        CompoundTag kExtra = new CompoundTag();
        kExtra.put("npc", npc);
        ListTag kInv = new ListTag();
        RandomSource r = RandomSource.create(7);
        kInv.add(metSlot(p, stuk(MOER, Zeldzaamheid.LEGENDARISCH, 52, r), 0));
        kInv.add(metSlot(p, stuk(ids[0], Zeldzaamheid.EPISCH, 40, r), 1));
        CompoundTag data = new CompoundTag();
        data.put("extra", kExtra);
        data.put("rijk_inventaris", kInv);
        CompoundTag s1 = new CompoundTag();
        s1.putString("naam", "tweede");
        s1.put("data", data);
        CompoundTag lijst = new CompoundTag();
        lijst.putInt("actief", 0);
        lijst.put("s1", s1);
        extra.put(LootOpruimen.KARAKTERS, lijst);
        kern.put("extra", extra);

        LootOpruimen.Resultaat res = LootOpruimen.scanDat(dat, 6);
        List<LootOpruimen.Vondst> v = res.vondsten();
        helper.assertTrue(res.open() == 1, "offline gate-1 player: open zone 1");
        helper.assertTrue(v.size() == 6, "5 + the inactive character's moersleutel, got " + v.size() + ": " + v.stream().map(x -> x.plek() + "=" + x.type()).toList());
        helper.assertTrue(v.stream().filter(x -> x.plek().startsWith("karakter")).allMatch(x -> x.type().equals(MOER) && x.open() == 4),
                "inactive character: only the moersleutel (zone 5 > 4)");
        helper.assertTrue(v.stream().noneMatch(x -> x.type().equals(ids[1]) || x.type().equals(KNOTS)), "zone-1 pieces stay");
        LootOpruimen.verwijder(v);
        helper.assertTrue(LootOpruimen.scanDat(dat, 6).vondsten().isEmpty(), "the tag is clean after removal");
        helper.assertTrue(kInv.size() == 1 && telType(List.of(kInv.getCompoundOrEmpty(0)), ids[0]) == 1, "the inactive character keeps its zone-3 piece");
        helper.assertTrue(Rollen.van(p.getInventory().getItem(0)) != null, "the live player is untouched by a tag scan");
        helper.succeed();
    }
}
