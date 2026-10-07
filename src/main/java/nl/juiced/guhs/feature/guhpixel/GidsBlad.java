package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The server side of the Guhdex tab "Guhpixel &amp; uitjes" (client.GidsGuhpixelTab): the player's muntjes and rank, then
 * the sections the slices registered, in {@link GidsSectie#volgorde order}. Sections that need Guhpixel are replaced by one
 * "Zoek het Guh-internetcafé, njeg" line until the player unlocked it. The whole page travels as one tag
 * (guhs:guhpixel_gids); texts stay Components, so every player reads their own language.
 */
public final class GidsBlad {
    private static final List<GidsSectie> SECTIES = new ArrayList<>();
    /** Row types in the tag ("T"). */
    public static final String KOP = "kop", REGEL = "regel", STAT = "stat", VOORTGANG = "voortgang", PLAATJE = "plaatje", STAP = "stap";

    public static void registreer(GidsSectie s) {
        SECTIES.removeIf(x -> x.id().equals(s.id()));
        SECTIES.add(s);
        SECTIES.sort(Comparator.comparingInt(GidsSectie::volgorde));
    }

    public static List<GidsSectie> secties() {
        return List.copyOf(SECTIES);
    }

    /** The whole page for this player. */
    public static CompoundTag stand(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        boolean toegang = Toegang.heeft(p);
        t.putBoolean("Toegang", toegang);
        t.putInt("Saldo", Muntjes.saldo(p));
        t.putInt("Totaal", Muntjes.totaal(p));
        t.putInt("Rang", Muntjes.rang(p).ordinal());
        t.putBoolean("RangAan", Rangen.aan(p));
        ListTag rijen = new ListTag();
        Rijen b = new Rijen(p, rijen);
        boolean zoek = false;
        for (GidsSectie s : SECTIES) {
            try {
                if (!s.zichtbaar(p)) {
                    continue;
                }
                if (!toegang && !s.zonderToegang()) {
                    zoek = true;
                    continue;
                }
                s.vul(p, b);
            } catch (RuntimeException e) {
                LogUtils.getLogger().error("Guhpixel: Guhdex section {} failed", s.id(), e);
            }
        }
        if (!toegang && (zoek || SECTIES.isEmpty())) {
            ListTag voor = new ListTag();
            new Rijen(p, voor).regel(Component.translatable("gui.guhs.guhpixel.gids.zoek_cafe"));
            voor.addAll(rijen);
            rijen = voor;
        }
        t.put("Rijen", rijen);
        return t;
    }

    /** Builds the rows of the tag. */
    private record Rijen(ServerPlayer p, ListTag rijen) implements Bouwer {
        private CompoundTag rij(String type) {
            CompoundTag r = new CompoundTag();
            r.putString("T", type);
            rijen.add(r);
            return r;
        }

        @Override
        public void kop(Component c) {
            Tekst.put(rij(KOP), "A", c);
        }

        @Override
        public void regel(Component c) {
            Tekst.put(rij(REGEL), "A", c);
        }

        @Override
        public void stat(Component label, Component waarde) {
            CompoundTag r = rij(STAT);
            Tekst.put(r, "A", label);
            Tekst.put(r, "B", waarde);
        }

        @Override
        public void voortgang(Component label, int heb, int totaal) {
            CompoundTag r = rij(VOORTGANG);
            Tekst.put(r, "A", label);
            r.putInt("Heb", heb);
            r.putInt("Totaal", totaal);
        }

        @Override
        public void grap(String grapId) {
            Grap g = Grappen.van(grapId);
            int n = g == null ? 0 : g.stappen();
            int stap = Grappen.stap(p, grapId);
            boolean klaar = Grappen.isKlaar(p, grapId);
            kop(Component.translatable("gui.guhs." + grapId + ".grap.naam"));
            regel(Component.translatable("gui.guhs." + grapId + ".grap.uitleg"));
            voortgang(Component.translatable("gui.guhs.guhpixel.gids.stappen"), Math.min(stap, n), n);
            for (int i = 1; i <= n; i++) {
                CompoundTag r = rij(STAP);
                // a step you have not reached yet stays a secret
                Tekst.put(r, "A", i <= stap + 1 ? Component.translatable("gui.guhs." + grapId + ".grap.stap." + i)
                        : Component.translatable("gui.guhs.guhpixel.gids.geheim"));
                r.putBoolean("Klaar", i <= stap);
            }
            if (klaar) {
                stat(Component.translatable("gui.guhs.guhpixel.gids.gespeeld"), Component.literal(String.valueOf(Grappen.keren(p, grapId))));
            }
        }

        @Override
        public void plaatje(ItemStack icoon, Component naam, @Nullable Component tip, boolean behaald) {
            CompoundTag r = rij(PLAATJE);
            r.put("Icoon", Nbt.saveStack(p.registryAccess(), icoon));
            Tekst.put(r, "A", naam);
            if (tip != null) {
                Tekst.put(r, "B", tip);
            }
            r.putBoolean("Klaar", behaald);
        }
    }

    private GidsBlad() {
    }
}
