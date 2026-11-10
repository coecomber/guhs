package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Y-COSMETICA: Vadsrijk outfits as pure looks in survival. Unlocks only come from the Vadsrijk (the source check + a story flag),
 * at home the wardrobe never unlocks or collects anything, wearing at home changes no attribute / item / armour, no movement trick
 * or soft landing at home, what other clients get (looks only, no unlock list), the zone gate in the list, survival armour never
 * becomes a Vadsrijk transmog look, and {@code /guhs garderobe}. The gametest level is the overworld = "home".
 * Run: {@code runGameTestServer -Pgt=VadsrijkYCosmetica}.
 */
public class VadsrijkYCosmeticaGameTests {
    private static final String BATCH = "vadsrijk_y_cosmetica";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        KledingData.wis(p);
        return p;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void ontgrendelAlleenInRijk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Questlog.zetVlag(p, "zone1_klaar", true);
        String spullen = spullen(p);
        try {
            // at home: opening the Garderobe shows, it never unlocks (not even the starters)
            Kledingkast.open(p, null, false);
            helper.assertTrue(Kledingkast.alleHeeft(p).isEmpty(), "at home the wardrobe unlocks nothing, got " + Kledingkast.alleHeeft(p));
            // in the Vadsrijk: the same open runs the source check -> the Knuffelorde cape (zone-1 story) + the starters
            Kledingkast.open(p, null, true);
            helper.assertTrue(Kledingkast.heeft(p, Cosmetica.KAAP_KNUFFELORDE.id()), "the zone-1 story gives the Knuffelorde cape");
            helper.assertTrue(Kledingkast.heeft(p, Cosmetica.PLOF_CONFETTI.id()), "starters on the first Vadsrijk visit");
            // recorded per account (UUID), as a list of unlocks: no item anywhere
            helper.assertTrue(KledingData.kast(p).getCompoundOrEmpty("heeft").getBooleanOr(Cosmetica.KAAP_KNUFFELORDE.id(), false), "in the account store");
            helper.assertTrue(spullen(p).equals(spullen), "no item handed out");
        } finally {
            Questlog.zetVlag(p, "zone1_klaar", false);
        }
        helper.succeed();
    }

    /** Every stack in the inventory + equipment, as text (to compare before/after). */
    private static String spullen(ServerPlayer p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty()) {
                b.append(i).append('=').append(s).append(';');
            }
        }
        for (EquipmentSlot s : EquipmentSlot.values()) {
            b.append(s.getName()).append('=').append(p.getItemBySlot(s)).append(';');
        }
        return b.toString();
    }

    private static Map<String, Double> attributen(ServerPlayer p) {
        Map<String, Double> m = new TreeMap<>();
        BuiltInRegistries.ATTRIBUTE.listElements().forEach(h -> {
            AttributeInstance a = p.getAttribute(h);
            if (a != null) {
                m.put(h.key().identifier().toString(), a.getValue());
            }
        });
        return m;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void thuisAlleenUiterlijk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Map<String, Double> voor = attributen(p);
        int armor = p.getArmorValue();
        String spullen = spullen(p);
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            Kledingkast.ontgrendel(p, d.id(), false);
        }
        helper.assertTrue(Kledingkast.kies(p, Cosmetica.Soort.VLEUGEL, Cosmetica.PLUIS.id()), "soft-landing wings on");
        helper.assertTrue(Kledingkast.kies(p, Cosmetica.Soort.ACCESSOIRE, Cosmetica.FEESTMUTS.id()), "party hat on");
        Kledingkast.kies(p, Cosmetica.Soort.VERF, Cosmetica.VERF_MINT.id());
        Kledingkast.kies(p, Cosmetica.Soort.NAAMKLEUR, Cosmetica.NAAM_ROZE.id());
        Kledingkast.kies(p, Cosmetica.Soort.SPOOR, Cosmetica.SPOOR_BLAADJES.id());
        Kledingkast.open(p, null, false);
        helper.assertTrue(attributen(p).equals(voor), "no attribute changed by wearing at home: " + voor + " -> " + attributen(p));
        helper.assertTrue(p.getArmorValue() == armor, "no armour value");
        helper.assertTrue(spullen(p).equals(spullen), "no item / equipment change: " + spullen + " -> " + spullen(p));
        // no movement trick / glide / soft landing at home
        helper.assertTrue(!Zweven.mag(p) && !Zweven.kan(p), "no glide at home");
        Zweven.ontvang(p, 1);
        helper.assertTrue(!Zweven.zweeft(p), "a glide claim at home is ignored");
        LivingFallEvent val = new LivingFallEvent(p, 12, 1f);
        Zweven.val(val);
        helper.assertTrue(!val.isCanceled(), "the soft-landing wings do NOT cancel fall damage at home");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void syncData(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Kledingkast.ontgrendel(p, Cosmetica.KAAP_KNUFFELORDE.id(), false);
        Kledingkast.ontgrendel(p, Cosmetica.STRIKJE.id(), false);
        Kledingkast.ontgrendel(p, Cosmetica.VERF_GEEL.id(), false);
        Kledingkast.kies(p, Cosmetica.Soort.KAAP, Cosmetica.KAAP_KNUFFELORDE.id());
        Kledingkast.kies(p, Cosmetica.Soort.ACCESSOIRE, Cosmetica.STRIKJE.id());
        Kledingkast.kies(p, Cosmetica.Soort.VERF, Cosmetica.VERF_GEEL.id());
        CompoundTag u = Kledingkast.uiterlijk(p);
        helper.assertTrue(u.getStringOr("rug", "").equals(Cosmetica.KAAP_KNUFFELORDE.id()), "others see the tabard cape");
        helper.assertTrue(u.getStringOr("hoofd", "").equals(Cosmetica.STRIKJE.id()), "others see the bow");
        helper.assertTrue(u.getIntOr("verf", -1) == Cosmetica.VERF_GEEL.kleur(), "others see the dye");
        helper.assertTrue(!u.contains("heeft") && !u.contains("looks") && !u.contains("kills"), "no unlock list goes to other clients");
        // taken off = gone for everybody
        helper.assertTrue(Kledingkast.kies(p, Cosmetica.Soort.KAAP, ""), "take the cape off");
        helper.assertTrue(!Kledingkast.uiterlijk(p).contains("rug"), "off for the others too");
        // the payload carries exactly this tag
        KledingPayloads.Uiterlijk pl = new KledingPayloads.Uiterlijk(p.getUUID(), Kledingkast.uiterlijk(p));
        helper.assertTrue(pl.speler().equals(p.getUUID()) && pl.data().getStringOr("hoofd", "").equals(Cosmetica.STRIKJE.id()), "the sync payload");
        CompoundTag scherm = Kledingkast.schermData(p);
        helper.assertTrue(!scherm.getBooleanOr("binnen", true), "the screen knows it is home (Garderobe, no armour tab)");
        helper.assertTrue(scherm.getIntOr("poort", -1) == Garderobe.poort(), "the screen gets the zone gate");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geenOverlevingsHarnas(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.getInventory().add(new ItemStack(Items.DIAMOND_HELMET));
        p.getInventory().add(new ItemStack(KernRegistry.PLUISHELM.get()));
        Kledingkast.open(p, null, false);
        helper.assertTrue(Kledingkast.looks(p, EquipmentSlot.HEAD).isEmpty(), "at home no look is collected");
        Kledingkast.open(p, null, true);
        java.util.List<String> l = Kledingkast.looks(p, EquipmentSlot.HEAD);
        helper.assertTrue(l.contains("guhs:vr_pluishelm"), "Vadsrijk armour becomes a look, got " + l);
        helper.assertTrue(!l.contains("minecraft:diamond_helmet"), "survival armour never becomes a Vadsrijk look");
        helper.assertTrue(!Kledingkast.kiesLook(p, EquipmentSlot.HEAD, "minecraft:diamond_helmet"), "and cannot be picked");
        p.getInventory().clearContent();
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void zonepoort(GameTestHelper helper) {
        helper.assertTrue(Garderobe.zone(Cosmetica.KAAP_KNUFFELORDE) == 1 && Garderobe.zone(Cosmetica.KRISTAL) == 2, "story/book zones");
        helper.assertTrue(Garderobe.zone(Cosmetica.STERRENSTOF) == 1 && Garderobe.zone(Cosmetica.POOLLICHT) == 2 && Garderobe.zone(Cosmetica.KAAP_MOS) == 3,
                "puzzle rewards follow their puzzle's zone");
        helper.assertTrue(Garderobe.zone(Cosmetica.PLUIS) == 3 && Garderobe.zone(Cosmetica.KAAP_REIS) == 3, "secrets that need zone 3");
        helper.assertTrue(Garderobe.zone(Cosmetica.FEESTMUTS) == 0 && Garderobe.zone(Cosmetica.VERF_ROZE) == 0, "general pieces");
        // gate 1: only zone-1 and general locked pieces are listed; what you have is always listed
        helper.assertTrue(Garderobe.zichtbaar(Cosmetica.KAAP_ROZE, false, 1) && Garderobe.zichtbaar(Cosmetica.FEESTMUTS, false, 1), "zone 1 + general");
        helper.assertTrue(!Garderobe.zichtbaar(Cosmetica.KRISTAL, false, 1) && !Garderobe.zichtbaar(Cosmetica.HONINGBIJ, false, 1)
                && !Garderobe.zichtbaar(Cosmetica.LAMPION, false, 1) && !Garderobe.zichtbaar(Cosmetica.POOLLICHT, false, 1), "no closed-zone spoilers");
        helper.assertTrue(Garderobe.zichtbaar(Cosmetica.KRISTAL, true, 1), "an owned piece always shows");
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            helper.assertTrue(Garderobe.zichtbaar(d, false, 6), "gate 6: everything listed (" + d.id() + ")");
            if (d.soort() == Cosmetica.Soort.VLEUGEL && d.zone() == 6) {
                helper.assertTrue(!Garderobe.zichtbaar(d, false, 1), "the raid wings stay hidden at gate 1");
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void commando(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        var guhs = root.getChild("guhs");
        helper.assertTrue(guhs != null && guhs.getChild("garderobe") != null, "/guhs garderobe exists");
        helper.assertTrue(guhs.getChild("garderobe").getRequirement().test(helper.getLevel().getServer().createCommandSourceStack()), "usable");
        helper.succeed();
    }
}
