package nl.juiced.guhs.feature.vadsrijk.zichtbaar;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.items.ItemsRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-ZICHTBAAR: the sync of the worn looks (list per slot, only on change, the start-tracking answer, empty outside the Vadsrijk,
 * the payload codec) and the look rules (ids -> looks, the wardrobe's cape rule, LOD, every new piece has its own look + texture).
 * Run: {@code runGameTestServer -Pgt=VadsrijkUZichtbaar}.
 */
public class VadsrijkUZichtbaarGameTests {
    private static final String BATCH = "vadsrijk_u_zichtbaar";

    private static ItemStack stuk(String id, long seed) {
        return Rollen.stack(Rollen.rol(BasisTypes.van(id), 1, Zeldzaamheid.GEWOON, RandomSource.create(seed)));
    }

    private static void allesAf(ServerPlayer p) {
        for (Slot s : Slot.SIERADEN) {
            Sieraden.zet(p, s, ItemStack.EMPTY);
        }
    }

    /** The list follows Slot.SIERADEN; worn pieces give their item id, empty slots ""; outside the Vadsrijk everything is "". */
    @GuhTest(template = "empty", batch = BATCH)
    public static void looksVolgenDeSlots(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            allesAf(p);
            Sieraden.zet(p, Slot.CAPE, stuk("sporencape", 1));
            Sieraden.zet(p, Slot.HANDSCHOENEN, stuk("kristalhandschoenen", 2));
            Sieraden.zet(p, Slot.RING2, stuk("roze_ring", 3));
            List<String> l = Zichtbaar.looks(p, true);
            helper.assertTrue(l.size() == Slot.SIERADEN.length, "one entry per virtual slot (" + l.size() + ")");
            helper.assertTrue(Looks.van(l, Slot.CAPE).equals("guhs:vr_t_sporencape"), "cape id: " + Looks.van(l, Slot.CAPE));
            helper.assertTrue(Looks.van(l, Slot.HANDSCHOENEN).equals("guhs:vr_t_kristalhandschoenen"), "gloves id");
            helper.assertTrue(Looks.van(l, Slot.RING2).equals("guhs:vr_roze_ring"), "ring 2 id: " + Looks.van(l, Slot.RING2));
            helper.assertTrue(Looks.van(l, Slot.RING).isEmpty() && Looks.van(l, Slot.KETTING).isEmpty(), "empty slots are \"\"");
            helper.assertTrue(Zichtbaar.looks(p, false).stream().allMatch(String::isEmpty), "outside the Vadsrijk nothing shows");
        } finally {
            allesAf(p);
        }
        helper.succeed();
    }

    /** Sent only on a change; the start-tracking answer is the last list sent (or the list now); a logout forgets it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void syncAlleenBijVerandering(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            allesAf(p);
            Zichtbaar.vergeet(p.getUUID());
            Sieraden.zet(p, Slot.CAPE, stuk("knuffelcapeje", 4));
            List<String> a = Zichtbaar.looks(p, true);
            helper.assertTrue(Zichtbaar.ververs(p, a), "the first list is always sent");
            helper.assertTrue(!Zichtbaar.ververs(p, Zichtbaar.looks(p, true)), "no change: nothing sent");
            helper.assertTrue(Zichtbaar.voorKijker(p).looks().equals(a), "a new tracker gets the last list sent");
            Sieraden.zet(p, Slot.HANDSCHOENEN, stuk("pluiswantjes", 5));
            List<String> b = Zichtbaar.looks(p, true);
            helper.assertTrue(Zichtbaar.ververs(p, b), "gloves on: sent");
            helper.assertTrue(Zichtbaar.voorKijker(p).looks().equals(b), "the tracker answer follows");
            helper.assertTrue(Zichtbaar.ververs(p, Zichtbaar.looks(p, false)), "leaving the Vadsrijk (all empty) is a change too");
            Zichtbaar.vergeet(p.getUUID());
            helper.assertTrue(Zichtbaar.voorKijker(p).looks().equals(Zichtbaar.looks(p)), "never sent: the tracker gets the list now");
            helper.assertTrue(Zichtbaar.ververs(p, b), "after a logout the next poll sends again");
            // the payload survives the wire
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            ZichtbaarPayloads.Uitrusting u = new ZichtbaarPayloads.Uitrusting(p.getUUID(), b);
            ZichtbaarPayloads.Uitrusting.STREAM_CODEC.encode(buf, u);
            helper.assertTrue(ZichtbaarPayloads.Uitrusting.STREAM_CODEC.decode(buf).equals(u), "payload round trip");
        } finally {
            allesAf(p);
            Zichtbaar.vergeet(p.getUUID());
        }
        helper.succeed();
    }

    /** Ids -> looks, the wardrobe's cape rule, the LOD, and every new cape/glove/armband has its own look with a texture. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lookRegels(GameTestHelper helper) {
        helper.assertTrue(Looks.sleutel("guhs:vr_t_sporencape").equals("sporencape"), "t-items prefix");
        helper.assertTrue(Looks.sleutel("guhs:vr_z4_mob_lampioncape").equals("lampioncape"), "zone-4 prefix");
        helper.assertTrue(Looks.sleutel("guhs:vr_z3_baas_eierwarmer").equals("eierwarmer"), "boss prefix");
        helper.assertTrue(Looks.sleutel("guhs:vr_roze_ring").equals("roze_ring"), "kern prefix");
        helper.assertTrue(Looks.kaapTextuur("guhs:vr_iets_anders").endsWith("cape_standaard.png"), "unknown cape: standaard");
        helper.assertTrue(Looks.model(Slot.HANDSCHOENEN, "guhs:vr_z9_mob_x").endsWith("handschoen_standaard"), "unknown gloves: standaard");
        helper.assertTrue(Looks.kleur("guhs:vr_z2_mob_kristalring") == 0x6AD8FF, "keyword colour");
        helper.assertTrue(Looks.kleur("guhs:vr_z4_baas_lagunelampion") == 0x3AD8C8, "first keyword wins");
        helper.assertTrue(Looks.kleur("guhs:vr_onbekend") == Looks.STANDAARD_KLEUR, "default colour");
        // T-KLEDING's rule: cosmetic cape replaces, wings hide
        helper.assertTrue(Looks.statCapeTonen(true, false, false), "stat cape alone shows");
        helper.assertTrue(!Looks.statCapeTonen(true, true, false), "a cosmetic cape replaces it");
        helper.assertTrue(!Looks.statCapeTonen(true, false, true), "wings hide it");
        helper.assertTrue(!Looks.statCapeTonen(false, false, false), "no cape, nothing");
        // LOD
        helper.assertTrue(Looks.teken(Looks.Deel.CAPE, 1e9), "the cape always");
        helper.assertTrue(Looks.teken(Looks.Deel.RING, 15 * 15) && !Looks.teken(Looks.Deel.RING, 17 * 17), "rings to 16");
        helper.assertTrue(Looks.teken(Looks.Deel.KETTING, 30 * 30) && !Looks.teken(Looks.Deel.KETTING, 33 * 33), "necklace to 32");
        helper.assertTrue(Looks.teken(Looks.Deel.HANDSCHOENEN, 47 * 47) && !Looks.teken(Looks.Deel.HANDSCHOENEN, 49 * 49), "gloves to 48");
        // every t-items piece (gloves/cape/armband) has its own look, and its texture/model is in the jar
        for (ItemsRegistry.Stuk s : ItemsRegistry.STUKKEN.values()) {
            String id = "guhs:" + ItemsRegistry.itemNaam(s.id());
            String pad = switch (s.slot()) {
                case CAPE -> Looks.kaapTextuur(id);
                default -> "items/" + Looks.model(s.slot(), id) + ".json";
            };
            helper.assertTrue(!pad.contains("standaard"), s.id() + " has its own look (" + pad + ")");
            helper.assertTrue(VadsrijkUZichtbaarGameTests.class.getResource("/assets/guhs/" + pad) != null, "resource exists: " + pad);
        }
        for (Slot s : new Slot[]{Slot.RING, Slot.KETTING, Slot.OORBEL, Slot.ARMBAND, Slot.HANDSCHOENEN}) {
            String pad = "/assets/guhs/items/" + Looks.model(s, "guhs:vr_x") + ".json";
            helper.assertTrue(VadsrijkUZichtbaarGameTests.class.getResource(pad) != null, "generic model exists: " + pad);
        }
        helper.assertTrue(VadsrijkUZichtbaarGameTests.class.getResource("/assets/guhs/textures/vadsrijk/zichtbaar/cape_standaard.png") != null,
                "generic cape texture");
        helper.succeed();
    }
}
