package nl.juiced.guhs.feature.vadsrijk.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * T-ITEMS: the smid's extra tabs on the server (economie.Smid sends every unknown action here and asks {@link #data} for the
 * extra screen numbers). Actions (args: {@code slot} = inventory slot of the piece on the anvil):
 * <ul>
 *   <li>{@code talisman} ({@code aan} 0/1): the Knuffeltalisman switch for star attempts;</li>
 *   <li>{@code koop_talisman}: buy one Knuffeltalisman (munten);</li>
 *   <li>{@code polijst} ({@code bonus}): a new value for one bonus within its range;</li>
 *   <li>{@code doorgroei}: Legendarisch/Guhddelijk + Vertrouwd 5 -> +10 levels;</li>
 *   <li>{@code overvads} ({@code offer} = inventory slot of the old piece, {@code bonus} = its bonus, {@code doel} = the bonus
 *   of the target it replaces, -1 = add).</li>
 * </ul>
 * Each action re-reads the slots, checks every price first and only then takes and changes (one tick; the piece on the anvil is
 * never removed). Returns {outcome, message key} like Smid's own actions.
 */
public final class SmidExtra {
    private static final String[] NIKS = {"", ""};

    private SmidExtra() {
    }

    public static void data(ServerPlayer p, CompoundTag d) {
        d.putInt("talismannen", EcoUtil.tel(p.getInventory(), ItemsRegistry.KNUFFELTALISMAN.get()));
        d.putInt("groeipluis", EcoUtil.tel(p.getInventory(), ItemsRegistry.GROEIPLUIS.get()));
        d.putBoolean("talisman_aan", Vangnet.talismanAan(p));
    }

    public static String[] doe(ServerPlayer p, String actie, CompoundTag a) {
        int slot = a.getIntOr("slot", -1);
        return switch (actie) {
            case "talisman" -> {
                Vangnet.zetTalisman(p, a.getIntOr("aan", 0) != 0);
                yield NIKS;
            }
            case "koop_talisman" -> koopTalisman(p, slot);
            case "polijst" -> polijst(p, slot, a.getIntOr("bonus", -1));
            case "doorgroei" -> doorgroei(p, slot);
            case "overvads" -> overvads(p, slot, a.getIntOr("offer", -1), a.getIntOr("bonus", -1), a.getIntOr("doel", -1));
            default -> NIKS;
        };
    }

    private static Uitrusting lees(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= Math.min(41, p.getInventory().getContainerSize())) {
            return null;
        }
        return Rollen.van(p.getInventory().getItem(slot));
    }

    private static void zet(ServerPlayer p, int slot, Uitrusting u) {
        p.getInventory().getItem(slot).set(KernRegistry.UITRUSTING.get(), u);
        p.getInventory().setChanged();
    }

    static String[] koopTalisman(ServerPlayer p, int slot) {
        Uitrusting u = lees(p, slot);
        long prijs = Vangnet.talismanPrijs(u == null ? Spelers.level(p) : u.level());
        if (!Spelers.neemMunten(p, prijs)) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        EcoUtil.geefOfBewaar(p, new ItemStack(ItemsRegistry.KNUFFELTALISMAN.get()));
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        return new String[] {"", "vadsrijk.items.talisman.gekocht"};
    }

    static String[] polijst(ServerPlayer p, int slot, int bonus) {
        Uitrusting u = lees(p, slot);
        if (u == null) {
            return NIKS;
        }
        if (u.verpluisd()) {
            return new String[] {"", "vadsrijk.economie.smid.eerst_repareren"};
        }
        if (bonus < 0 || bonus >= u.bonussen().size()) {
            return new String[] {"", "vadsrijk.items.polijst.kies"};
        }
        if (!Spelers.neemMunten(p, Erfstukken.polijstPrijs(u))) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        zet(p, slot, Erfstukken.polijst(u, bonus, p.getRandom()));
        p.level().sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1.3, p.getZ(), 20, 0.4, 0.4, 0.4, 0.5);
        EcoUtil.geluid(p, SoundEvents.AXE_WAX_OFF, 1f, 1.3f);
        return new String[] {"GEPOLIJST", "vadsrijk.items.polijst.klaar"};
    }

    static String[] doorgroei(ServerPlayer p, int slot) {
        Uitrusting u = lees(p, slot);
        if (u == null) {
            return NIKS;
        }
        String waarom = Erfstukken.kanDoorgroeien(u);
        if (!waarom.isEmpty()) {
            return new String[] {"", waarom};
        }
        int pluis = Erfstukken.groeipluis(u);
        if (EcoUtil.tel(p.getInventory(), ItemsRegistry.GROEIPLUIS.get()) < pluis) {
            return new String[] {"", "vadsrijk.items.doorgroei.geen_pluis"};
        }
        if (!Spelers.neemMunten(p, Erfstukken.doorgroeiPrijs(u))) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        EcoUtil.neem(p.getInventory(), ItemsRegistry.GROEIPLUIS.get(), pluis);
        zet(p, slot, Erfstukken.doorgroei(u));
        p.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 0.7, 0.5, 0.3);
        EcoUtil.geluid(p, SoundEvents.PLAYER_LEVELUP, 1f, 0.8f);
        return new String[] {"DOORGEGROEID", "vadsrijk.items.doorgroei.klaar"};
    }

    static String[] overvads(ServerPlayer p, int slot, int offerSlot, int bonus, int doelBonus) {
        Uitrusting u = lees(p, slot), o = lees(p, offerSlot);
        if (u == null || o == null || slot == offerSlot) {
            return new String[] {"", "vadsrijk.items.overvads.kies_offer"};
        }
        String waarom = Erfstukken.kanOvervadsen(u, o, bonus, doelBonus);
        if (!waarom.isEmpty()) {
            return new String[] {"", waarom};
        }
        if (!Spelers.neemMunten(p, Erfstukken.overvadsPrijs(u))) {
            return new String[] {"", "vadsrijk.economie.smid.te_weinig"};
        }
        boolean lief = p.getRandom().nextDouble() < ItemsBalans.get("overvads_mislukking");
        zet(p, slot, Erfstukken.overvads(u, o, bonus, doelBonus, lief));
        p.getInventory().getItem(offerSlot).shrink(1);
        p.getInventory().setChanged();
        p.level().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(), 12, 0.5, 0.3, 0.5, 0.02);
        EcoUtil.geluid(p, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.5f);
        return lief ? new String[] {"LIEF", "vadsrijk.items.overvads.lief"} : new String[] {"OVERGEVADST", "vadsrijk.items.overvads.klaar"};
    }
}
