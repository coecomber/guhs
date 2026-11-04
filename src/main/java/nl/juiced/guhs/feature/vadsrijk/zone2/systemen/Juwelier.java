package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The juweliersguh in Kandijkom ("De Glinsterkist"; ronde 15: simple, no minigame). API for the z2-npc slice:
 * {@link #open(ServerPlayer)}.
 * <ul>
 *   <li>{@code zet}: a crystal from your bag goes straight into an empty socket of a piece (free);</li>
 *   <li>{@code haal}: takes a gem out again for a small fee ({@link #prijs}); the crystal comes back to your bag (full bag: to
 *   kern's found items, {@code /guhs vadsrijk kist}). A gem never breaks and is never lost.</li>
 * </ul>
 * Every action re-reads the slots and checks everything before changing anything (one server tick, no half states).
 * The pure socket maths ({@link #zetIn}, {@link #haalUit}) is used by the gametests.
 */
public final class Juwelier {
    /** Bag + armour + offhand. */
    public static final int SLOTS = 41;
    /** The fee to take a gem out, per quality (brons). */
    public static final long[] PRIJS = {20, 60, 150};
    private static final Set<UUID> OPEN = ConcurrentHashMap.newKeySet();

    private Juwelier() {
    }

    public static long prijs(String gem) {
        return PRIJS[Kristallen.kwaliteit(gem)];
    }

    /** API (z2-npc): opens the juwelier screen. Only in the Vadsrijk. */
    public static void open(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            OPEN.add(p.getUUID());
            stuur(p, -1, "", "");
        }
    }

    static void dicht(ServerPlayer p) {
        OPEN.remove(p.getUUID());
    }

    static void vergeet(UUID id) {
        OPEN.remove(id);
    }

    static void stuur(ServerPlayer p, int kies, String melding, String arg) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        ListTag gear = new ListTag(), kristallen = new ListTag();
        for (int i = 0; i < Math.min(SLOTS, p.getInventory().getContainerSize()); i++) {
            ItemStack s = p.getInventory().getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.sokjes().isEmpty()) {
                CompoundTag t = new CompoundTag();
                t.putInt("slot", i);
                t.putInt("item", items.size());
                items.add(s.copy());
                gear.add(t);
            } else if (Kristallen.gem(s) != null) {
                CompoundTag t = new CompoundTag();
                t.putInt("slot", i);
                t.putInt("item", items.size());
                items.add(s.copy());
                kristallen.add(t);
            }
        }
        d.put("gear", gear);
        d.put("kristallen", kristallen);
        d.putLong("munten", Spelers.data(p).munten);
        d.putInt("kies", kies);
        d.putString("melding", melding);
        d.putString("meldingArg", arg);
        JuwelierPayloads.stuur(p, d, items);
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p) || !OPEN.contains(p.getUUID())) {
            return;   // (not opened through the API: ignored)
        }
        int slot = a.getIntOr("slot", -1);
        String[] r = switch (actie) {
            case "zet" -> zet(p, slot, a.getIntOr("sokje", -1), a.getIntOr("kristal", -1));
            case "haal" -> haal(p, slot, a.getIntOr("sokje", -1));
            default -> new String[] {"", ""};
        };
        stuur(p, slot, r[0], r[1]);
    }

    // ---- the pure maths -----------------------------------------------------------------------------------------------------

    /** The piece with {@code gem} in socket {@code i}, or null when that socket doesn't exist or isn't empty. */
    public static Uitrusting zetIn(Uitrusting u, int i, String gem) {
        if (i < 0 || i >= u.sokjes().size() || !u.sokjes().get(i).isEmpty() || Kristallen.soort(gem) == null) {
            return null;
        }
        List<String> l = new ArrayList<>(u.sokjes());
        l.set(i, gem);
        return u.metSokjes(l);
    }

    /** The piece with socket {@code i} empty, or null when there is no gem in it. */
    public static Uitrusting haalUit(Uitrusting u, int i) {
        if (i < 0 || i >= u.sokjes().size() || u.sokjes().get(i).isEmpty()) {
            return null;
        }
        List<String> l = new ArrayList<>(u.sokjes());
        l.set(i, "");
        return u.metSokjes(l);
    }

    // ---- the actions --------------------------------------------------------------------------------------------------------

    private static boolean geldig(int slot) {
        return slot >= 0 && slot < SLOTS;
    }

    /** Puts one crystal from bag slot {@code kristalSlot} into socket {@code i} of the piece in {@code slot}. */
    public static String[] zet(ServerPlayer p, int slot, int i, int kristalSlot) {
        if (!geldig(slot) || !geldig(kristalSlot) || slot == kristalSlot) {
            return new String[] {"", ""};
        }
        ItemStack stuk = p.getInventory().getItem(slot), kristal = p.getInventory().getItem(kristalSlot);
        Uitrusting u = Rollen.van(stuk);
        String gem = Kristallen.gem(kristal);
        if (u == null || gem == null) {
            return new String[] {"vadsrijk.z2.systemen.juwelier.weg", ""};
        }
        Uitrusting nieuw = zetIn(u, i, gem);
        if (nieuw == null) {
            return new String[] {"vadsrijk.z2.systemen.juwelier.vol", ""};
        }
        kristal.shrink(1);
        stuk.set(nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.UITRUSTING.get(), nieuw);
        p.getInventory().setChanged();
        if (p.level() instanceof ServerLevel sl) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1f, 1.3f);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.6f);
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
        }
        Spelers.sync(p);
        return new String[] {"vadsrijk.z2.systemen.juwelier.gezet", stuk.getHoverName().getString()};
    }

    /** Takes the gem out of socket {@code i} of the piece in {@code slot} for the fee; the crystal comes back. */
    public static String[] haal(ServerPlayer p, int slot, int i) {
        if (!geldig(slot)) {
            return new String[] {"", ""};
        }
        ItemStack stuk = p.getInventory().getItem(slot);
        Uitrusting u = Rollen.van(stuk);
        if (u == null) {
            return new String[] {"vadsrijk.z2.systemen.juwelier.weg", ""};
        }
        Uitrusting nieuw = haalUit(u, i);
        if (nieuw == null) {
            return new String[] {"", ""};
        }
        String gem = u.sokjes().get(i);
        ItemStack terug = Kristallen.stack(gem);
        if (terug.isEmpty()) {
            return new String[] {"", ""};
        }
        long prijs = prijs(gem);
        if (!Spelers.neemMunten(p, prijs)) {
            return new String[] {"vadsrijk.z2.systemen.juwelier.te_weinig", Spelers.munten(prijs)};
        }
        stuk.set(nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.UITRUSTING.get(), nieuw);
        EcoUtil.geefOfBewaar(p, terug);
        p.getInventory().setChanged();
        if (p.level() instanceof ServerLevel sl) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1f, 1.1f);
        }
        Spelers.sync(p);
        return new String[] {"vadsrijk.z2.systemen.juwelier.gehaald", Spelers.munten(prijs)};
    }
}
