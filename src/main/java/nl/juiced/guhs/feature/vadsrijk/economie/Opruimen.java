package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.GearVergelijk;

/**
 * 1.10.1 "Opruimen": what to do with old gear that is worse than what you wear. Two ways, both shown in the tooltip:
 * <ul>
 *   <li><b>verkopen</b> (shops, and the smid buys too): {@link Waarde#gear};</li>
 *   <li><b>ontmantelen</b> (salvage; at the smid, the "⚒" button next to your inventory, or "Ontmantel alle slechtere" in a shop):
 *   the piece becomes smithing material by rarity ({@link #opbrengst}): grey -> Smeedstofjes, green -> Smeedsplinters, blue and
 *   up -> Smeedkristallen. The smid melts those into what the stars and crafts use ({@link #OMSMELTEN}).</li>
 * </ul>
 * Never touched: anything that is not Vadsrijk gear (survival items), the piece in your main hand and the worn armour/jewellery
 * (only bag slots 0-35 that are not the selected hotbar slot), and pieces you locked ({@link #vergrendeld}, key L in the
 * inventory or the lock button). Blue and up (or with stars / a crystal in a socket) asks for a confirmation first.
 * "Alle slechtere" = bag pieces that are worse than what you wear in that slot ({@link GearVergelijk}, exactly like the tooltip),
 * unlocked, not worn, without stars, crystals or verpluisd. The quest step "Opruimen" (hq03c) listens to
 * {@code NpcApi.meld(p, "opruimen", "verkocht" / "ontmanteld")}, i.e. the keys "opruimen:verkocht" and "opruimen:ontmanteld".
 */
public final class Opruimen {
    /** Screen context: from the inventory button (salvage only), at the smid (also sells + melts). */
    public static final String INVENTARIS = "inventaris", SMID = "smid";
    public static final String K = "vadsrijk.economie.opruim.";

    /** One melt recipe at the smid: n of {@code in} -> {@code uit}. */
    public record Omsmelt(String id, Supplier<? extends Item> in, int n, Supplier<ItemStack> uit) {
    }

    /** What the salvage materials feed into: the star stones, repair thread, reroll knabbels and the crafts' rare Glinstergruis. */
    public static final List<Omsmelt> OMSMELTEN = List.of(
            new Omsmelt("stofje_steentje", EcoSpul.SMEEDSTOFJE, 3, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())),
            new Omsmelt("splinter_steentje", EcoSpul.SMEEDSPLINTER, 1, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())),
            new Omsmelt("splinter_draad", EcoSpul.SMEEDSPLINTER, 1, () -> new ItemStack(EcoSpul.PLUISDRAAD.get())),
            new Omsmelt("kristal_gruis", EcoSpul.SMEEDKRISTAL, 2, () -> nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen.stack("glinstergruis", 0, 1)),
            new Omsmelt("kristal_knabbel", EcoSpul.SMEEDKRISTAL, 3, () -> new ItemStack(EcoSpul.HERROLKNABBEL.get())));

    /** Sale value (brons) of one salvage material: low on purpose (salvaging is for the smid, not a money loop). */
    static final long WAARDE_STOFJE = 2, WAARDE_SPLINTER = 10, WAARDE_KRISTAL = 40;

    private Opruimen() {
    }

    /** Called from {@link EconomieRegistry#register}: the materials' sale values, the materiaaltas, the quest hooks. */
    static void init() {
        Waarde.EXTRA.add(s -> s.is(EcoSpul.SMEEDSTOFJE.get()) ? WAARDE_STOFJE : s.is(EcoSpul.SMEEDSPLINTER.get()) ? WAARDE_SPLINTER
                : s.is(EcoSpul.SMEEDKRISTAL.get()) ? WAARDE_KRISTAL : 0);
        nl.juiced.guhs.feature.vadsrijk.opslag.Materiaaltas.regel(s -> s.is(EcoSpul.SMEEDSTOFJE.get()) || s.is(EcoSpul.SMEEDSPLINTER.get())
                || s.is(EcoSpul.SMEEDKRISTAL.get()));
        // the tutorial step hq03c "Opruimen": Sergeant Pluizebol hands out two old practice weapons
        nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.ACTIES.put("opruimspullen", (p, a) -> geefOudeSpullen(p));
    }

    // =====================================================================================================================
    // the rules (pure, tested in VadsrijkOpruimenGameTests)
    // =====================================================================================================================

    /** The material a piece of this rarity salvages into. */
    public static Item materiaal(Zeldzaamheid z) {
        return switch (z) {
            case GEWOON -> EcoSpul.SMEEDSTOFJE.get();
            case ONGEWOON -> EcoSpul.SMEEDSPLINTER.get();
            default -> EcoSpul.SMEEDKRISTAL.get();
        };
    }

    /**
     * How many: grey 2, green 1, blue 1, purple 2, orange 4, pink 8; + 1 per 20 levels (grey/green) or per 25 levels (crystals);
     * + 1 per 2 stars (a bit of what went into it comes back).
     */
    public static int aantal(Uitrusting u) {
        int basis = switch (u.zeldzaamheid()) {
            case GEWOON -> 2;
            case ONGEWOON, ZELDZAAM -> 1;
            case EPISCH -> 2;
            case LEGENDARISCH -> 4;
            case GUHDDELIJK -> 8;
        };
        int lv = Math.max(1, u.level());
        int perLevel = u.zeldzaamheid().ordinal() >= Zeldzaamheid.ZELDZAAM.ordinal() ? lv / 25 : lv / 20;
        return basis + perLevel + Math.max(0, u.sterren()) / 2;
    }

    /** What salvaging this piece gives. */
    public static ItemStack opbrengst(Uitrusting u) {
        return new ItemStack(materiaal(u.zeldzaamheid()), aantal(u));
    }

    public static boolean vergrendeld(ItemStack s) {
        return Boolean.TRUE.equals(s.get(KernRegistry.VERGRENDELD.get()));
    }

    /** Has a crystal in a socket. */
    static boolean heeftKristal(Uitrusting u) {
        for (String k : u.sokjes()) {
            if (!k.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Worth a confirmation before salvaging: blue and up, stars, or a crystal in a socket. */
    public static boolean kostbaar(Uitrusting u) {
        return u.zeldzaamheid().ordinal() >= Zeldzaamheid.ZELDZAAM.ordinal() || u.sterren() > 0 || heeftKristal(u);
    }

    // =====================================================================================================================
    // the player's bag
    // =====================================================================================================================

    /** The slot of the main hand (that weapon counts as worn). */
    static int hand(ServerPlayer p) {
        return p.getInventory().getSelectedSlot();
    }

    /** The piece the player wears in the slot of {@code u} (see {@link GearVergelijk#gedragen}); leeg[0] = nothing worn. */
    @Nullable
    static Uitrusting gedragen(ServerPlayer p, Uitrusting u, boolean[] leeg) {
        BasisType bt = u.basisType();
        if (bt == null) {
            return null;
        }
        return GearVergelijk.gedragen(u, p.getMainHandItem(), p::getItemBySlot, s -> Vadsrijk.in(p) ? Rollen.van(Sieraden.get(p, s)) : null,
                klasse(p, bt), leeg);
    }

    private static String klasse(ServerPlayer p, BasisType bt) {
        return bt.slot() == Slot.WAPEN ? Spelers.wapenKlasse(bt) : Spelers.klasse(p).id();
    }

    /**
     * The verdict compared with what you wear (tooltip logic): 1 beter, 0 gelijk, -1 slechter, {@link #GEEN_OORDEEL} when there is
     * nothing to compare with (another class's weapon in your hand), {@link #LEEG} when that slot is empty (it would be better).
     */
    public static int oordeel(ServerPlayer p, Uitrusting u) {
        BasisType bt = u.basisType();
        if (bt == null || u.verpluisd()) {
            return GEEN_OORDEEL;
        }
        boolean[] leeg = {false};
        Uitrusting oud = gedragen(p, u, leeg);
        if (leeg[0]) {
            return LEEG;
        }
        if (oud == null || oud.equals(u)) {
            return GEEN_OORDEEL;
        }
        return GearVergelijk.vergelijk(u, oud, klasse(p, bt)).oordeel();
    }

    public static final int GEEN_OORDEEL = -9, LEEG = 9;

    /** Is this bag slot something you may sell/salvage at all? "" = yes, else the message key why not. */
    public static String magWeg(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= EcoUtil.TAS) {
            return K + "gedragen";   // (armour, offhand: worn)
        }
        ItemStack s = p.getInventory().getItem(slot);
        if (Rollen.van(s) == null) {
            return K + "geen_uitrusting";   // (survival items, materials: never)
        }
        if (slot == hand(p)) {
            return K + "gedragen";
        }
        if (vergrendeld(s)) {
            return K + "vergrendeld";
        }
        return "";
    }

    /** The bag slots that count as "slechter" for the bulk buttons (worse than worn, unlocked, not worn, no stars/crystals/verpluisd). */
    public static List<Integer> slechtere(ServerPlayer p) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack s = p.getInventory().getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u == null || !magWeg(p, i).isEmpty() || u.sterren() > 0 || heeftKristal(u) || u.verpluisd()) {
                continue;
            }
            if (oordeel(p, u) == -1) {
                l.add(i);
            }
        }
        return l;
    }

    /** The summary for a confirm window: per rarity how many, the total sale value and what salvaging gives. */
    public static CompoundTag samenvatting(ServerPlayer p, List<Integer> slots) {
        CompoundTag t = new CompoundTag();
        int[] per = new int[Zeldzaamheid.values().length];
        long waarde = 0;
        int stof = 0, splinter = 0, kristal = 0;
        for (int i : slots) {
            Uitrusting u = Rollen.van(p.getInventory().getItem(i));
            if (u == null) {
                continue;
            }
            per[u.zeldzaamheid().ordinal()]++;
            waarde += Waarde.gear(u);
            switch (u.zeldzaamheid()) {
                case GEWOON -> stof += aantal(u);
                case ONGEWOON -> splinter += aantal(u);
                default -> kristal += aantal(u);
            }
        }
        t.putInt("aantal", slots.size());
        t.putIntArray("per", per);
        t.putLong("waarde", waarde);
        t.putInt("stofjes", stof);
        t.putInt("splinters", splinter);
        t.putInt("kristallen", kristal);
        return t;
    }

    // =====================================================================================================================
    // actions (each returns {message key, argument}; everything is checked before anything changes). The Vadsrijk check is
    // done by the callers (the screens' doe, the payload): the gametest server has no Vadsrijk dimension.
    // =====================================================================================================================

    /** Salvage one bag slot. {@code bevestigd}: the player confirmed (needed for {@link #kostbaar} pieces). */
    public static String[] ontmantel(ServerPlayer p, int slot, boolean bevestigd) {
        String waarom = magWeg(p, slot);
        if (!waarom.isEmpty()) {
            return new String[] {waarom, ""};
        }
        ItemStack s = p.getInventory().getItem(slot);
        Uitrusting u = Rollen.van(s);
        if (kostbaar(u) && !bevestigd) {
            return new String[] {K + "bevestig", ""};
        }
        ItemStack uit = opbrengst(u);
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        String naam = uit.getCount() + "x " + uit.getHoverName().getString();
        EcoUtil.geefOfBewaar(p, uit);
        p.level().sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 14, 0.4, 0.4, 0.4, 0.15);
        EcoUtil.geluid(p, SoundEvents.SMITHING_TABLE_USE, 0.7f, 1.4f);
        meld(p, "ontmanteld");
        return new String[] {K + "ontmanteld", naam};
    }

    /** Salvage every "slechter" piece at once (the player saw the counts in the confirm window). */
    public static String[] ontmantelSlechtere(ServerPlayer p) {
        List<Integer> l = slechtere(p);
        if (l.isEmpty()) {
            return new String[] {K + "geen_slechtere", ""};
        }
        CompoundTag sam = samenvatting(p, l);
        for (int i : l) {
            Uitrusting u = Rollen.van(p.getInventory().getItem(i));
            p.getInventory().setItem(i, ItemStack.EMPTY);
            EcoUtil.geefOfBewaar(p, opbrengst(u));
        }
        EcoUtil.geluid(p, SoundEvents.SMITHING_TABLE_USE, 0.8f, 1.2f);
        meld(p, "ontmanteld");
        return new String[] {K + "ontmanteld_alle", String.valueOf(sam.getIntOr("aantal", 0))};
    }

    /** Sell one bag slot (shops and the smid). Refuses locked and worn pieces; non-gear stacks sell like in the shop. */
    public static String[] verkoop(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= EcoUtil.TAS) {
            return new String[] {"", ""};
        }
        ItemStack s = p.getInventory().getItem(slot);
        boolean gear = Rollen.van(s) != null;
        if (gear) {
            String waarom = magWeg(p, slot);
            if (!waarom.isEmpty()) {
                return new String[] {waarom, ""};
            }
        } else if (vergrendeld(s)) {
            return new String[] {K + "vergrendeld", ""};
        }
        long v = Waarde.verkoop(s);
        if (v <= 0) {
            return new String[] {"", ""};
        }
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        Spelers.geefMunten(p, v);
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        if (gear) {
            meld(p, "verkocht");
        }
        return new String[] {"vadsrijk.economie.winkel.verkocht", Spelers.munten(v)};
    }

    /** Sell every "slechter" piece at once. */
    public static String[] verkoopSlechtere(ServerPlayer p) {
        List<Integer> l = slechtere(p);
        if (l.isEmpty()) {
            return new String[] {K + "geen_slechtere", ""};
        }
        long totaal = 0;
        for (int i : l) {
            totaal += Waarde.gear(Rollen.van(p.getInventory().getItem(i)));
            p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        Spelers.geefMunten(p, totaal);
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        meld(p, "verkocht");
        return new String[] {"vadsrijk.economie.winkel.verkocht", Spelers.munten(totaal)};
    }

    /** Lock / unlock a piece of Vadsrijk gear in the bag (or worn armour: slots up to 40). */
    public static String[] vergrendel(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= Math.min(41, p.getInventory().getContainerSize())) {
            return new String[] {"", ""};
        }
        ItemStack s = p.getInventory().getItem(slot);
        if (Rollen.van(s) == null) {
            return new String[] {K + "geen_uitrusting", ""};
        }
        boolean nu = !vergrendeld(s);
        if (nu) {
            s.set(KernRegistry.VERGRENDELD.get(), true);
        } else {
            s.remove(KernRegistry.VERGRENDELD.get());
        }
        p.getInventory().setChanged();
        EcoUtil.geluid(p, nu ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, 0.6f, 1.5f);
        return new String[] {K + (nu ? "op_slot" : "van_slot"), s.getHoverName().getString()};
    }

    /** Melt salvage material into what the smid and the crafts use. */
    public static String[] omsmelt(ServerPlayer p, String id) {
        for (Omsmelt o : OMSMELTEN) {
            if (o.id().equals(id)) {
                if (EcoUtil.tel(p.getInventory(), o.in().get()) < o.n()) {
                    return new String[] {K + "te_weinig", ""};
                }
                EcoUtil.neem(p.getInventory(), o.in().get(), o.n());
                ItemStack uit = o.uit().get();
                String naam = uit.getHoverName().getString();
                EcoUtil.geefOfBewaar(p, uit);
                EcoUtil.geluid(p, SoundEvents.ANVIL_USE, 0.5f, 1.6f);
                return new String[] {K + "omgesmolten", naam};
            }
        }
        return new String[] {"", ""};
    }

    private static void meld(ServerPlayer p, String wat) {
        nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "opruimen", wat);
    }

    // =====================================================================================================================
    // the tutorial step (hq03c): two old practice weapons, worse than your starter gear
    // =====================================================================================================================

    /** Two plain level-1 weapons of your class without bonuses ("Oud oefenwapen"): worse than both class weapons you have. */
    public static void geefOudeSpullen(ServerPlayer p) {
        BasisType t = null;
        String k = Spelers.data(p).klasse;
        for (BasisType b : BasisTypes.alle()) {
            if (b.slot() == Slot.WAPEN && k.equals(b.klasse())) {
                t = b;
                break;
            }
        }
        if (t == null) {
            t = BasisTypes.van("kussenzwaard");
        }
        if (t == null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            Uitrusting u = new Uitrusting(t.id(), Zeldzaamheid.GEWOON, 1, List.of(), t.element(), 0, 0, false, false, p.getRandom().nextLong());
            EcoUtil.geefOfBewaar(p, Rollen.stack(u));
        }
        nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.npc.tip.opruimen");
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    /** Opens the Opruimen screen ({@link #INVENTARIS}: salvage + lock; {@link #SMID}: also sell and melt). Only in the Vadsrijk. */
    public static void open(ServerPlayer p, String context) {
        if (Vadsrijk.in(p)) {
            stuur(p, context, "", "");
        }
    }

    static void stuur(ServerPlayer p, String context, String melding, String arg) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        boolean smid = SMID.equals(context);
        d.putString("context", context);
        d.putBoolean("verkoopt", smid);
        d.putBoolean("smelt", smid);
        d.putLong("munten", Spelers.data(p).munten);
        d.putString("melding", melding);
        d.putString("meldingArg", arg);
        ListTag gear = new ListTag();
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack s = p.getInventory().getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u == null) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.putInt("slot", i);
            t.putInt("item", items.size());
            items.add(s.copy());
            t.putLong("waarde", Waarde.gear(u));
            t.putInt("oordeel", oordeel(p, u));
            t.putBoolean("hand", i == hand(p));
            t.putBoolean("slot_op", vergrendeld(s));
            t.putBoolean("kostbaar", kostbaar(u));
            t.putInt("opbrengst", items.size());
            items.add(opbrengst(u));
            gear.add(t);
        }
        d.put("gear", gear);
        d.put("slechter", samenvatting(p, slechtere(p)));
        d.putInt("stofjes", EcoUtil.tel(p.getInventory(), EcoSpul.SMEEDSTOFJE.get()));
        d.putInt("splinters", EcoUtil.tel(p.getInventory(), EcoSpul.SMEEDSPLINTER.get()));
        d.putInt("kristallen", EcoUtil.tel(p.getInventory(), EcoSpul.SMEEDKRISTAL.get()));
        if (smid) {
            ListTag sm = new ListTag();
            for (Omsmelt o : OMSMELTEN) {
                CompoundTag t = new CompoundTag();
                t.putString("id", o.id());
                t.putInt("n", o.n());
                t.putInt("in", items.size());
                items.add(new ItemStack(o.in().get()));
                t.putInt("uit", items.size());
                items.add(o.uit().get());
                t.putBoolean("kan", EcoUtil.tel(p.getInventory(), o.in().get()) >= o.n());
                sm.add(t);
            }
            d.put("omsmelten", sm);
        }
        EcoPayloads.open(p, EcoPayloads.OPRUIM, context, d, items);
    }

    static void doe(ServerPlayer p, String context, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        boolean smid = SMID.equals(context);
        int slot = a.getIntOr("slot", -1);
        String[] r = switch (actie) {
            case "ontmantel" -> ontmantel(p, slot, a.getBooleanOr("ok", false));
            case "ontmantelslechter" -> ontmantelSlechtere(p);
            case "vergrendel" -> vergrendel(p, slot);
            case "verkoop" -> smid ? verkoop(p, slot) : new String[] {"", ""};
            case "verkoopslechter" -> smid ? verkoopSlechtere(p) : new String[] {"", ""};
            case "omsmelt" -> smid ? omsmelt(p, a.getStringOr("id", "")) : new String[] {"", ""};
            case "terug" -> {
                if (smid) {
                    Smid.open(p);
                    yield null;
                }
                yield new String[] {"", ""};
            }
            default -> new String[] {"", ""};
        };
        if (r != null) {
            stuur(p, context, r[0], r[1]);
        }
    }
}
