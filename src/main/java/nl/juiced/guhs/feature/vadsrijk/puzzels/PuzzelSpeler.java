package nl.juiced.guhs.feature.vadsrijk.puzzels;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;

/**
 * T-PUZZELS: what a player has done, in {@code SpelerData.extra("puzzels")}:
 * <pre>
 *   opgelost     {puzzle id: true}     solved/found once (the reward is given once)
 *   bladzijdes   {"1".."9": true}      torn pages found (also as items; the item can get lost, this can't)
 *   ontgrendeld  {unlock id: true}     cosmetic unlocks (T-KLEDING reads {@link #ontgrendeld})
 * </pre>
 */
public final class PuzzelSpeler {
    public static final String SLICE = "puzzels";

    private PuzzelSpeler() {
    }

    static CompoundTag map(ServerPlayer p, String naam) {
        CompoundTag d = Spelers.data(p).extra(SLICE);
        if (!(d.get(naam) instanceof CompoundTag t)) {
            CompoundTag n = new CompoundTag();
            d.put(naam, n);
            return n;
        }
        return t;
    }

    public static boolean opgelost(ServerPlayer p, String id) {
        return map(p, "opgelost").getBooleanOr(id, false);
    }

    public static boolean bladzijde(ServerPlayer p, int n) {
        return map(p, "bladzijdes").getBooleanOr(Integer.toString(n), false);
    }

    /** T-KLEDING: has this player unlocked this cosmetic id through a puzzle (e.g. "pz_vleugels_sterrenstof")? */
    public static boolean ontgrendeld(ServerPlayer p, String id) {
        return map(p, "ontgrendeld").getBooleanOr(id, false);
    }

    public static int aantalOpgelost(ServerPlayer p) {
        return map(p, "opgelost").keySet().size();
    }

    /** Dev/tests: forget one puzzle (or everything with null). */
    public static void vergeet(ServerPlayer p, String id) {
        if (id == null) {
            Spelers.data(p).extra.remove(SLICE);
        } else {
            map(p, "opgelost").remove(id);
        }
        Spelers.opgeslagen(p);
    }

    /** The reward of a puzzle, once per player. */
    static void beloon(ServerPlayer p, PuzzelDef d) {
        if (opgelost(p, d.id())) {
            p.sendSystemMessage(Component.translatable(d.stil() ? "vadsrijk.puzzels.geheim_gevonden" : "vadsrijk.puzzels.opgelost",
                    Component.translatable(d.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.al_opgelost").withStyle(ChatFormatting.GRAY));
            return;
        }
        map(p, "opgelost").putBoolean(d.id(), true);
        Component naam = Component.translatable(d.naamKey());
        RijkScenes.titel(p, Component.translatable(d.stil() ? "vadsrijk.puzzels.geheim_gevonden" : "vadsrijk.puzzels.opgelost", naam)
                .withStyle(ChatFormatting.LIGHT_PURPLE), Component.translatable("vadsrijk.puzzels.soort." + d.soort()), 60, 0xFF8CC8);
        p.sendSystemMessage(Component.translatable(d.stil() ? "vadsrijk.puzzels.geheim_gevonden" : "vadsrijk.puzzels.opgelost", naam)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.3f);
        PuzzelDef.Beloning b = d.beloning();
        if (b.munten() > 0) {
            Spelers.geefMunten(p, b.munten());
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.beloning.munten", Voltooiing.muntTekst(b.munten())).withStyle(ChatFormatting.GOLD));
        }
        for (PuzzelDef.ItemBeloning ib : b.items()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(ib.item()));
            if (item == null || item == Items.AIR) {
                continue;
            }
            geef(p, new ItemStack(item, ib.aantal()));
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.beloning.item", ib.aantal(), item.getName(new ItemStack(item)))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (b.bladzijde() > 0) {
            map(p, "bladzijdes").putBoolean(Integer.toString(b.bladzijde()), true);
            Item pagina = PuzzelRegistry.bladzijde(b.bladzijde());
            if (pagina != null) {
                geef(p, new ItemStack(pagina));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.beloning.bladzijde",
                    Component.translatable("vadsrijk.puzzels.bladzijde." + b.bladzijde() + ".titel")).withStyle(ChatFormatting.GOLD));
        }
        if (b.ontgrendel() != null && !b.ontgrendel().isEmpty()) {
            map(p, "ontgrendeld").putBoolean(b.ontgrendel(), true);
            p.sendSystemMessage(Component.translatable("vadsrijk.puzzels.beloning.ontgrendel",
                    Component.translatable("vadsrijk.puzzels.ontgrendel." + b.ontgrendel())).withStyle(ChatFormatting.GOLD));
            for (var h : Puzzels.ONTGRENDEL_HAAK) {
                try {
                    h.accept(p, b.ontgrendel());
                } catch (RuntimeException e) {
                    // (a broken listener never breaks the reward)
                }
            }
        }
        Spelers.opgeslagen(p);
    }

    static void geef(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
    }
}
