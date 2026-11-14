package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The smidguh (design 9): stars with risk ({@link SterRegels}), rerolling the bonuses with Herrolknabbels (optionally keep one
 * bonus), and repairing verpluisd pieces. API for the npc slice: {@link #open(ServerPlayer)}.
 * Every action re-reads the slot, checks the price and the materials first, and only then takes them and changes the piece
 * (one server tick: no half-done states). The piece itself is never removed.
 */
public final class Smid {
    /** Bag + armour + offhand slots the smid looks at. */
    public static final int SLOTS = 41;

    private Smid() {
    }

    /** API (npc slice): opens the smid screen. Only in the Vadsrijk. */
    public static void open(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            stuur(p, -1, "", "");
        }
    }

    private static void stuur(ServerPlayer p, int kies, String uitkomst, String melding) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        ListTag l = new ListTag();
        for (int i = 0; i < Math.min(SLOTS, p.getInventory().getContainerSize()); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (Rollen.van(s) != null) {
                CompoundTag t = new CompoundTag();
                t.putInt("slot", i);
                t.putInt("item", items.size());
                items.add(s.copy());
                l.add(t);
            }
        }
        d.put("gear", l);
        d.putLong("munten", Spelers.data(p).munten);
        d.putInt("steentjes", EcoUtil.tel(p.getInventory(), EcoSpul.VERBETERSTEENTJE.get()));
        d.putInt("knabbels", EcoUtil.tel(p.getInventory(), EcoSpul.HERROLKNABBEL.get()));
        d.putInt("draad", EcoUtil.tel(p.getInventory(), EcoSpul.PLUISDRAAD.get()));
        d.putInt("slechter", Opruimen.slechtere(p).size());   // 1.10.1: on the Opruimen button
        d.putInt("kies", kies);
        d.putString("uitkomst", uitkomst);
        d.putString("melding", melding);
        nl.juiced.guhs.feature.vadsrijk.items.SmidExtra.data(p, d);   // T-ITEMS: talismans, Groeipluis, the talisman switch
        EcoPayloads.open(p, EcoPayloads.SMID, "", d, items);
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        int slot = a.getIntOr("slot", -1);
        String[] r = switch (actie) {
            case "ster" -> ster(p, slot, p.getRandom());
            case "herrol" -> herrol(p, slot, a.getIntOr("houd", -1), p.getRandom());
            case "repareer" -> repareer(p, slot);
            case "opruimen" -> {   // 1.10.1: the Opruimen screen (salvage, sell, melt) from the smid
                Opruimen.open(p, Opruimen.SMID);
                yield null;
            }
            default -> nl.juiced.guhs.feature.vadsrijk.items.SmidExtra.doe(p, actie, a);   // T-ITEMS: polijst, doorgroei, overvads, talisman
        };
        if (r != null) {
            stuur(p, slot, r[0], r[1]);
        }
    }

    private static Uitrusting lees(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= Math.min(SLOTS, p.getInventory().getContainerSize())) {
            return null;
        }
        return Rollen.van(p.getInventory().getItem(slot));
    }

    private static void zet(ServerPlayer p, int slot, Uitrusting u) {
        ItemStack s = p.getInventory().getItem(slot);
        s.set(nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.UITRUSTING.get(), u);
        p.getInventory().setChanged();
    }

    /** One star attempt. Returns {outcome (SterRegels.Uitkomst name or ""), message key}. */
    static String[] ster(ServerPlayer p, int slot, RandomSource r) {
        Uitrusting u = lees(p, slot);
        if (u == null) {
            return new String[] {"", ""};
        }
        if (u.verpluisd()) {
            return new String[] {"", "vadsrijk.economie.smid.eerst_repareren"};
        }
        if (u.sterren() >= SterRegels.MAX) {
            return new String[] {"", "vadsrijk.economie.smid.max"};
        }
        long prijs = SterRegels.prijs(u.level(), u.sterren());
        int steen = SterRegels.steentjes(u.sterren());
        if (EcoUtil.tel(p.getInventory(), EcoSpul.VERBETERSTEENTJE.get()) < steen) {
            return new String[] {"", "vadsrijk.economie.smid.geen_steentjes"};
        }
        if (!Spelers.neemMunten(p, prijs)) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        EcoUtil.neem(p.getInventory(), EcoSpul.VERBETERSTEENTJE.get(), steen);
        SterRegels.Uitkomst k = nl.juiced.guhs.feature.vadsrijk.items.Vangnet.rol(p, u, r);   // T-ITEMS: pity + Knuffeltalisman
        Uitrusting na = nl.juiced.guhs.feature.vadsrijk.items.Vangnet.naPoging(SterRegels.pasToe(u, k), u, k);   // T-ITEMS: the pity counter
        zet(p, slot, na);
        feest(p, k);
        if (na.sterren() >= SterRegels.MAX) {
            Cosmetica.geefTitel(p, Cosmetica.STERRENSMID);
        }
        return new String[] {k.name(), "vadsrijk.economie.smid.uitkomst." + k.name().toLowerCase(java.util.Locale.ROOT)};
    }

    private static void feest(ServerPlayer p, SterRegels.Uitkomst k) {
        ServerLevel sl = p.level();
        switch (k) {
            case GELUKT -> {
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.6, 0.5, 0.25);
                EcoUtil.geluid(p, SoundEvents.ANVIL_USE, 0.6f, 1.4f);
            }
            case VERPLUISD -> {
                sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.2, p.getZ(), 25, 0.5, 0.5, 0.5, 0.05);
                EcoUtil.geluid(p, SoundEvents.WOOL_BREAK, 1f, 0.7f);
            }
            default -> {
                sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
                EcoUtil.geluid(p, SoundEvents.SLIME_SQUISH, 1f, 1.2f);
            }
        }
    }

    /** Rerolls the bonuses (same number). {@code houd} = index of the bonus to keep, or -1. */
    static String[] herrol(ServerPlayer p, int slot, int houd, RandomSource r) {
        Uitrusting u = lees(p, slot);
        if (u == null) {
            return new String[] {"", ""};
        }
        if (u.verpluisd()) {
            return new String[] {"", "vadsrijk.economie.smid.eerst_repareren"};
        }
        if (u.bonussen().isEmpty()) {
            return new String[] {"", "vadsrijk.economie.smid.geen_bonussen"};
        }
        boolean slotje = houd >= 0 && houd < u.bonussen().size() && u.bonussen().size() > 1;
        long prijs = SterRegels.herrolPrijs(u.level(), slotje);
        int knabbels = SterRegels.herrolKnabbels(slotje);
        if (EcoUtil.tel(p.getInventory(), EcoSpul.HERROLKNABBEL.get()) < knabbels) {
            return new String[] {"", "vadsrijk.economie.smid.geen_knabbels"};
        }
        if (!Spelers.neemMunten(p, prijs)) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        EcoUtil.neem(p.getInventory(), EcoSpul.HERROLKNABBEL.get(), knabbels);
        zet(p, slot, u.metBonussen(herrolBonussen(u, slotje ? houd : -1, r)));
        p.level().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.4, p.getZ(), 40, 0.5, 0.5, 0.5, 0.6);
        EcoUtil.geluid(p, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1.3f);
        return new String[] {"HERROLD", "vadsrijk.economie.smid.herrold"};
    }

    /** New bonuses: as many as before; the kept one stays (same place, same value), the others are distinct from it. */
    public static List<Bonus> herrolBonussen(Uitrusting u, int houd, RandomSource r) {
        int n = u.bonussen().size();
        List<Bonus> nieuw = Bonussen.rol(n + 1, u.level(), u.zeldzaamheid(), r, u.basisType() == null ? null : u.basisType().slot());   // T-ITEMS: slot pool
        List<Bonus> uit = new ArrayList<>();
        Bonus vast = houd >= 0 ? u.bonussen().get(houd) : null;
        for (Bonus b : nieuw) {
            if (uit.size() >= (vast == null ? n : n - 1)) {
                break;
            }
            if (vast == null || b.stat() != vast.stat()) {
                uit.add(b);
            }
        }
        if (vast != null) {
            uit.add(Math.min(houd, uit.size()), vast);
        }
        return uit;
    }

    static String[] repareer(ServerPlayer p, int slot) {
        Uitrusting u = lees(p, slot);
        if (u == null || !u.verpluisd()) {
            return new String[] {"", ""};
        }
        long prijs = SterRegels.reparatiePrijs(u.level(), u.sterren());
        int draad = SterRegels.reparatieDraad(u.sterren());
        if (EcoUtil.tel(p.getInventory(), EcoSpul.PLUISDRAAD.get()) < draad) {
            return new String[] {"", "vadsrijk.economie.smid.geen_draad"};
        }
        if (!Spelers.neemMunten(p, prijs)) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        EcoUtil.neem(p.getInventory(), EcoSpul.PLUISDRAAD.get(), draad);
        zet(p, slot, SterRegels.repareer(u));
        p.level().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(), 8, 0.5, 0.3, 0.5, 0.02);
        EcoUtil.geluid(p, SoundEvents.ANVIL_USE, 0.6f, 1.8f);
        return new String[] {"GEREPAREERD", "vadsrijk.economie.smid.gerepareerd"};
    }
}
