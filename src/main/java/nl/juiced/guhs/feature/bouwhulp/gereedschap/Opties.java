package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;

/**
 * Writes a tool's settings for the client (what to show: every visible option with its value, limits and locked choices)
 * and takes the client's changes (checked against the same description). A block or mix setting takes the block from a
 * hotbar slot (or the offhand: slot -1); slot -2 empties a mix.
 */
public final class Opties {
    /** The option list for the screen. {@code alleen}: only these keys (the keet's tabs), {@code modus}: force the vormstaf's mode. */
    public static CompoundTag schrijf(Gereedschap g, ServerPlayer p, BouwNiveau n, @Nullable Predicate<String> alleen, int modus) {
        CompoundTag t = BouwSpelers.van(p.level().getServer()).instelling(p.getUUID(), g);
        CompoundTag zicht = t.copy();
        if (modus >= 0) {
            zicht.putInt("modus", modus);
        }
        Optie.Ctx ctx = new Optie.Ctx(p, n);
        CompoundTag uit = new CompoundTag();
        uit.putString("Gereedschap", g.id());
        uit.putInt("Niveau", n.nummer());
        ListTag lijst = new ListTag();
        for (Optie o : g.opties()) {
            if (alleen != null && !alleen.test(o.key()) || !o.zichtbaar().test(zicht)) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("Key", o.key());
            c.putInt("Soort", o.soort().ordinal());
            c.putInt("Min", o.min());
            c.putInt("Max", o.max().apply(n));
            switch (o.soort()) {
                case KEUZE -> {
                    c.putInt("Waarde", Math.min(o.keuzes().size() - 1, o.waarde(t)));
                    ListTag k = new ListTag();
                    for (int i = 0; i < o.keuzes().size(); i++) {
                        CompoundTag e = new CompoundTag();
                        e.putString("Id", o.keuzes().get(i));
                        String dicht = o.waarom(ctx, i);
                        if (dicht != null) {
                            e.putString("Dicht", dicht);
                        }
                        k.add(e);
                    }
                    c.put("Keuzes", k);
                }
                case GETAL -> c.putInt("Waarde", Math.max(o.min(), Math.min(o.max().apply(n), o.waarde(t))));
                case AAN_UIT -> c.putInt("Waarde", o.waarde(t));
                case BLOK, MIX -> {
                    ListTag b = new ListTag();
                    Mix.laad(t.getListOrEmpty(o.key())).blokken().forEach(x -> b.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(x.asItem()).toString())));
                    c.put("Blokken", b);
                }
                case TEKST -> c.putString("Tekst", t.getStringOr(o.key(), o.keuzes().get(0)));
            }
            lijst.add(c);
        }
        uit.put("Opties", lijst);
        return uit;
    }

    /** A change from the client. False when it was not allowed (a locked choice, an unknown key). */
    public static boolean zet(Gereedschap g, ServerPlayer p, BouwNiveau n, String key, int waarde, String tekst) {
        Optie o = g.optie(key);
        if (o == null) {
            return false;
        }
        BouwSpelers data = BouwSpelers.van(p.level().getServer());
        CompoundTag t = data.instelling(p.getUUID(), g);
        switch (o.soort()) {
            case KEUZE -> {
                int i = Math.floorMod(waarde, o.keuzes().size());
                if (o.waarom(new Optie.Ctx(p, n), i) != null) {
                    return false;
                }
                t.putInt(key, i);
            }
            case GETAL -> t.putInt(key, Math.max(o.min(), Math.min(o.max().apply(n), waarde)));
            case AAN_UIT -> t.putInt(key, waarde != 0 ? 1 : 0);
            case TEKST -> t.putString(key, tekst.length() > 24 ? tekst.substring(0, 24) : tekst);
            case BLOK, MIX -> {
                if (waarde == -2) {
                    t.put(key, new ListTag());
                    break;
                }
                ItemStack st = waarde == -1 ? p.getOffhandItem() : waarde >= 0 && waarde < 9 ? p.getInventory().getItem(waarde) : ItemStack.EMPTY;
                Block b = Mix.uitItem(st);
                if (b == null) {
                    return false;
                }
                zetBlok(t, o, b);
            }
        }
        data.veranderd();
        return true;
    }

    /** A block for a block or mix setting (the pipette: sneak-click a block with the tool). */
    public static void zetBlok(CompoundTag t, Optie o, Block b) {
        Mix nu = Mix.laad(t.getListOrEmpty(o.key()));
        t.put(o.key(), (o.soort() == Optie.Soort.MIX ? nu.met(b) : Mix.van(b)).opslaan());
    }

    /** A number/choice/on-off setting's value (its default when unset; 0 for a key the tool doesn't have). */
    public static int waarde(Gereedschap g, CompoundTag t, String key) {
        Optie o = g.optie(key);
        return o == null ? t.getIntOr(key, 0) : o.waarde(t);
    }

    /** The settings tag of a player's tool. */
    public static CompoundTag van(ServerPlayer p, Gereedschap g) {
        return BouwSpelers.van(p.level().getServer()).instelling(p.getUUID(), g);
    }

    /** The keys the keet's Vormen tab shows, and its Daken tab. */
    public static final List<String> KEET_VORMEN = List.of("vorm", "b", "h", "d", "hol", "dikte", "wand", "rand", "tekst"),
            KEET_DAKEN = List.of("dak", "b", "d", "overstek", "gevels", "wand", "rand");

    private Opties() {
    }
}
