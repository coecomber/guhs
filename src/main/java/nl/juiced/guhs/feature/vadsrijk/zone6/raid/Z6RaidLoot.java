package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * U-Z6RAID: raid-tier loot (lv 70). User decision: EVERY clear gives a chest (no week limit), per player, own loot, no rolling.
 * <ul>
 *   <li><b>Sterrenzijde-set</b> (Legendarisch, lv 70, 4 armour pieces): 2 pieces "+15 Verdediging en +150 Vads", 4 pieces
 *       "Schade tegen bazen +10%".</li>
 *   <li><b>Named items</b>: Ringbreker (zwaard, Ridder), Komeetboog (Jager), Sterrenstaf (Magier), Ringelings Teenring (ring,
 *       Guhddelijk, very rare).</li>
 *   <li><b>Rare</b>: the Sterrenring-vleugels (wings with the movement trick "luchtdash", via t-kleding's Kledingkast) 3 %, the pet
 *       Sterrenpluisje 4 %, the Sterrennacht-halster (the raid colour of the bultguh mount) 2 %, the Ringreplica (guild trophy) 12 %.</li>
 *   <li><b>The clear chest</b>: 1 sure Sterrenzijde piece (a missing one first) + 30 % a class weapon (yours) + coins 2500-3500 + the
 *       rares; the first clear guarantees a raid weapon and the replica. Bosses 1-3 give a small chest (coins + 30 % a set piece).</li>
 *   <li><b>Titles</b>: "Ringbreker" (first clear), "Sterrenaansteker" (a clear without fainting once).</li>
 * </ul>
 */
public final class Z6RaidLoot {
    public static final int LEVEL = 70;
    public static final String SET = "sterrenzijde";
    public static final List<String> STUKKEN = List.of("sterrenzijde_tulband", "sterrenzijde_mantel", "sterrenzijde_pofbroek", "sterrenzijde_teenslofjes");
    public static final List<Slot> SLOTS = List.of(Slot.HOOFD, Slot.BORST, Slot.BENEN, Slot.VOETEN);
    public static final String UITERLIJK = "vr_z6raid_sterrenzijde";
    public static final double KANS_VLEUGELS = 0.03, KANS_HUISDIER = 0.04, KANS_HALSTER = 0.02, KANS_REPLICA = 0.12, KANS_TEENRING = 0.015,
            KANS_WAPEN = 0.30;
    public static final String VLEUGELS = "vleugel:sterrenring", HUISDIER = "sterrenpluisje", HALSTER_RIJDIER = "bultguh:sterrennacht";

    private Z6RaidLoot() {
    }

    private static DeferredItem<Item> wapen(String id) {
        return Z6RaidRegistry.ITEMS.registerItem("vr_z6raid_" + id, p -> new BazenLoot.BaasItem(p, "z6raid_" + id), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    public static final List<DeferredItem<Item>> SET_ITEMS = new ArrayList<>();

    static {
        for (int i = 0; i < STUKKEN.size(); i++) {
            Slot s = SLOTS.get(i);
            String id = STUKKEN.get(i);
            SET_ITEMS.add(Z6RaidRegistry.ITEMS.registerItem("vr_z6raid_" + id, p -> new BazenLoot.BaasItem(p, "z6raid_" + id),
                    () -> KernRegistry.harnasEigenschappen(s, UITERLIJK)));
        }
    }

    public static final DeferredItem<Item> RINGBREKER = wapen("ringbreker");
    public static final DeferredItem<Item> KOMEETBOOG = wapen("komeetboog");
    public static final DeferredItem<Item> STERRENSTAF = wapen("sterrenstaf");
    public static final DeferredItem<Item> TEENRING = Z6RaidRegistry.ITEMS.registerItem("vr_z6raid_teenring", p -> new BazenLoot.BaasItem(p, "z6raid_teenring"),
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> REPLICA = Z6RaidRegistry.ITEMS.registerItem("vr_z6raid_ringreplica", BazenLoot.Trofee::new,
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> HALSTER = Z6RaidRegistry.ITEMS.registerItem("vr_z6raid_halster", Z6RaidHalster::new,
            () -> new Item.Properties().stacksTo(1));

    /** The base types (called from the registry after the items exist). ~8 % above the zone-4 boss line per level step. */
    static void typen() {
        Object[][] set = {
                {Stat.VERDEDIGING, 24, Stat.VADS, 150, Stat.WIJSHEID, 10, Stat.KRACHT, 10},
                {Stat.VERDEDIGING, 36, Stat.VADS, 230, Stat.ELEMENTWEERSTAND, 10},
                {Stat.VERDEDIGING, 30, Stat.VADS, 190, Stat.BEHENDIGHEID, 10},
                {Stat.VERDEDIGING, 20, Stat.VADS, 130, Stat.LOOPSNELHEID, 6}};
        for (int i = 0; i < STUKKEN.size(); i++) {
            Object[] s = set[i];
            Stats basis = Stats.van(s), groei = Stats.van((Stat) s[0], ((Number) s[1]).doubleValue() * 0.1, (Stat) s[2], ((Number) s[3]).doubleValue() * 0.08);
            BasisTypes.registreer(new BasisType("z6raid_" + STUKKEN.get(i), "guhs:vr_z6raid_" + STUKKEN.get(i), SLOTS.get(i), "harnas", null, Element.ROZE, 0, 0, basis,
                    groei, 1));
        }
        BasisTypes.registreer(BasisType.wapen("z6raid_ringbreker", "guhs:vr_z6raid_ringbreker", "zwaard", Klassen.RIDDER, Element.ROZE, 10.6, 1.9,
                Stats.van(Stat.KRACHT, 15, Stat.VERDEDIGING, 12, Stat.KRITIEK, 4), Stats.van(Stat.KRACHT, 1.05), 1));
        BasisTypes.registreer(BasisType.wapen("z6raid_komeetboog", "guhs:vr_z6raid_komeetboog", "boog", Klassen.JAGER, Element.ROZE, 10.4, 1.9,
                Stats.van(Stat.BEHENDIGHEID, 15, Stat.KRITIEK, 7), Stats.van(Stat.BEHENDIGHEID, 1.05), 1));
        BasisTypes.registreer(BasisType.wapen("z6raid_sterrenstaf", "guhs:vr_z6raid_sterrenstaf", "staf", Klassen.MAGIER, Element.ROZE, 10.4, 1.9,
                Stats.van(Stat.WIJSHEID, 16, Stat.HEALKRACHT, 12, Stat.ELEMENTSCHADE, 6), Stats.van(Stat.WIJSHEID, 1.05), 1));
        BasisTypes.registreer(BasisType.sieraad("z6raid_teenring", "guhs:vr_z6raid_teenring", Slot.RING,
                Stats.van(Stat.KRITIEK, 8, Stat.ELEMENTSCHADE, 12, Stat.LOOPSNELHEID, 5), Stats.van(Stat.KRITIEK, 0.12), 1));
    }

    // ---- the set -------------------------------------------------------------------------------------------------------------

    public static int stukken(LivingEntity e) {
        if (!(e instanceof Player p)) {
            return 0;
        }
        int n = 0;
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Uitrusting u = Rollen.van(p.getItemBySlot(es));
            if (u != null && !u.verpluisd() && u.type().startsWith("z6raid_sterrenzijde")) {
                n++;
            }
        }
        return Math.min(4, n);
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p) >= 2) {
            s.plus(Stat.VERDEDIGING, 15);
            s.plus(Stat.VADS, 150);
        }
        return s;
    }

    public static final double BAAS_FACTOR = 1.10;

    // ---- chests ----------------------------------------------------------------------------------------------------------------

    public static ItemStack setStuk(int i, RandomSource r) {
        BasisType t = BasisTypes.van("z6raid_" + STUKKEN.get(i));
        return t == null ? ItemStack.EMPTY : Rollen.stack(Rollen.rol(t, LEVEL, Zeldzaamheid.LEGENDARISCH, r).metGebonden(true));
    }

    public static ItemStack wapenVoor(ServerPlayer p, RandomSource r) {
        String k = Spelers.data(p).klasse;
        String id = Klassen.JAGER.equals(k) ? "z6raid_komeetboog" : Klassen.MAGIER.equals(k) ? "z6raid_sterrenstaf" : "z6raid_ringbreker";
        BasisType t = BasisTypes.van(id);
        return t == null ? ItemStack.EMPTY : Rollen.stack(Rollen.rol(t, LEVEL, Zeldzaamheid.LEGENDARISCH, r).metGebonden(true));
    }

    public static ItemStack teenring(RandomSource r) {
        BasisType t = BasisTypes.van("z6raid_teenring");
        return t == null ? ItemStack.EMPTY : Rollen.stack(Rollen.rol(t, LEVEL, Zeldzaamheid.GUHDDELIJK, r).metGebonden(true));
    }

    /** A set piece this player doesn't own yet (bag + worn), else a random one. */
    static int ontbrekend(ServerPlayer p, RandomSource r) {
        List<Integer> mist = new ArrayList<>();
        for (int i = 0; i < STUKKEN.size(); i++) {
            String type = "z6raid_" + STUKKEN.get(i);
            boolean heeft = false;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                Uitrusting u = Rollen.van(s);
                heeft |= u != null && u.type().equals(type);
            }
            for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                Uitrusting u = Rollen.van(p.getItemBySlot(es));
                heeft |= u != null && u.type().equals(type);
            }
            if (!heeft) {
                mist.add(i);
            }
        }
        return mist.isEmpty() ? r.nextInt(STUKKEN.size()) : mist.get(r.nextInt(mist.size()));
    }

    /** Bosses 1-3: coins + 30 % a set piece. */
    public static LootTabel.Resultaat baasKist(String baas, ServerPlayer p, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        if (r.nextDouble() < 0.3) {
            items.add(setStuk(ontbrekend(p, r), r));
        }
        return new LootTabel.Resultaat(items, 700 + r.nextInt(400));
    }

    /** The clear chest (every clear). */
    public static LootTabel.Resultaat clearKist(ServerPlayer p, @Nullable Z6Raid.Groep g, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        CompoundTag extra = Spelers.data(p).extra("z6raid");
        boolean eerste = !extra.getBooleanOr("clear", false);
        items.add(setStuk(ontbrekend(p, r), r));
        if (eerste || r.nextDouble() < KANS_WAPEN) {
            items.add(wapenVoor(p, r));
        }
        if (eerste || r.nextDouble() < KANS_REPLICA) {
            items.add(new ItemStack(REPLICA.get()));
        }
        if (r.nextDouble() < KANS_TEENRING) {
            items.add(teenring(r));
            if (p.level() instanceof ServerLevel sl) {
                sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z6raid.loot.teenring", p.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        // V-INT: the Sterrennacht-Bultguh is a mount drop now: RijdierBuit "z6raid_sterrennacht" (10 % per clear + pity 15), rolled in
        // the Z6RaidHaken.OPGEVROLIJKT hook (vint.VInt); the halter item stays for the dev command
        if (r.nextDouble() < KANS_VLEUGELS) {
            vleugels(p);
        }
        if (r.nextDouble() < KANS_HUISDIER) {
            nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.geef(p, HUISDIER);
        }
        extra.putBoolean("clear", true);
        extra.putInt("clears", extra.getIntOr("clears", 0) + 1);
        List<ItemStack> uit = new ArrayList<>();
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                uit.add(s);
            }
        }
        return new LootTabel.Resultaat(uit, 2500 + r.nextInt(1000));
    }

    /** The wings (t-kleding): unlocked in the Kledingkast, a chat line. */
    public static boolean vleugels(ServerPlayer p) {
        boolean nieuw = nl.juiced.guhs.feature.vadsrijk.kleding.Kledingkast.ontgrendel(p, VLEUGELS, true);
        if (nieuw && p.level() instanceof ServerLevel sl) {
            sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z6raid.loot.vleugels", p.getDisplayName())
                    .withStyle(ChatFormatting.GOLD), false);
        }
        return nieuw;
    }

    /** The titles of a clear (the predicates read these flags). */
    static void titels(ServerPlayer p, Z6Raid.Groep g) {
        Spelers.data(p).gezien.add("z6raid:ringbreker");
        if (!g.flauw.contains(p.getUUID())) {
            Spelers.data(p).gezien.add("z6raid:sterrenaansteker");
        }
    }

    public static boolean heeftTitel(Player p, String t) {
        return p instanceof ServerPlayer sp && Spelers.data(sp).gezien.contains("z6raid:" + t);
    }
}
