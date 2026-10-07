package nl.juiced.guhs.feature.band;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhDex;

/**
 * The Guhdex tab "Mijn guhs" (2.10), server side: a snapshot of all of an owner's band guhs (looks for the 3D preview,
 * hearts, favourites, friends, chores, where it is, statistics, eerste keren, wist-je-datjes), sent with
 * {@code guhs:band_mijn_guhs} when the Guhdex opens, on request ({@code guhs:band_vraag}) and from the menu's
 * "Dagboekje" (with the guh to show). The client never needs the guhs themselves to be loaded.
 */
public final class MijnGuhs {
    private MijnGuhs() {
    }

    /** Sends the snapshot (focus: the band id whose page the client should open, or null). */
    public static void stuur(ServerPlayer player, @Nullable UUID focus) {
        ModNetworking.sendTo(player, new BandPayloads.MijnGuhsData(snapshot(player, focus)));
    }

    /** The menu's "Dagboekje": the Guhdex opens on this guh's page. */
    public static void openDagboek(ServerPlayer player, GuhEntity guh) {
        if (!Band.isBandGuh(guh)) {
            return;
        }
        Band.bijwerken(guh);
        GuhVolger.zet(guh, BandEvents.plekSoort(guh), BandEvents.plekDetail(guh));
        stuur(player, Band.id(guh));
        GuhDex.open(player);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(player, "lieve_vadsjes/band_dagboekje");
    }

    /** Everything the tab shows, for this owner. */
    public static CompoundTag snapshot(ServerPlayer player, @Nullable UUID focus) {
        MinecraftServer s = player.level().getServer();
        BandData data = BandData.get(s);
        CompoundTag root = new CompoundTag();
        ListTag guhs = new ListTag();
        var registries = player.registryAccess();
        for (BandData.Rec r : data.guhsVan(player.getUUID())) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", r.id.toString());
            nl.juiced.guhs.taal.Tekst.put(t, "Naam", r.weergave());   // (1.2.0: Components, read on the client)
            t.put("Looks", r.looks.copy());
            t.putInt("Hartjes", r.hartjes);
            t.putInt("Niveau", r.niveau().ordinal());
            BandNiveau volgende = r.niveau().volgende();
            t.putInt("Volgende", volgende == null ? -1 : volgende.drempel());
            // favourites: the discovered ones by name, the others "???"
            ListTag fav = new ListTag();
            for (FavorietSoort soort : FavorietSoort.values()) {
                CompoundTag f = new CompoundTag();
                f.putString("Soort", soort.id());
                String waarde = r.fav.get(soort);
                f.putString("Naam", r.ontdekt.contains(soort) && waarde != null ? json(Favorieten.naam(soort, waarde), registries) : "");
                fav.add(f);
            }
            t.put("Fav", fav);
            // friends
            ListTag vrienden = new ListTag();
            UUID bestie = Vriendjes.bestie(s, r.id);
            for (UUID v : Vriendjes.vriendenVan(s, r.id)) {
                BandData.Rec vr = data.vindOveral(v);
                CompoundTag f = new CompoundTag();
                nl.juiced.guhs.taal.Tekst.put(f, "Naam", vr == null ? Component.literal("Guh") : vr.weergave());
                f.putBoolean("Bestie", v.equals(bestie));
                vrienden.add(f);
            }
            t.put("Vrienden", vrienden);
            // its huisje and chores
            Huisje h = Huisjes.vanBewoner(s, player.getUUID(), r.id);
            nl.juiced.guhs.taal.Tekst.put(t, "Huisje", h == null ? Component.empty() : h.naamTekst());
            ListTag klussen = new ListTag();
            if (h != null) {
                for (Klus k : Klusjes.alle()) {
                    if (h.klusAan(r.id, k.id())) {
                        klussen.add(StringTag.valueOf(json(k.naam(), registries)));
                    }
                }
            }
            t.put("Klussen", klussen);
            t.putString("Plek", json(GuhVolger.tekst(r.plek), registries));
            t.putString("PlekSoort", r.plek.soort().id());   // 1.2.5: "Roep naar mij" is grey for a picked-up guh
            CompoundTag stats = new CompoundTag();
            for (DagboekStat st : DagboekStat.values()) {
                stats.putLong(st.id(), r.stat(st));
            }
            t.put("Stats", stats);
            ListTag eerste = new ListTag();
            for (BandData.Eerste e : r.eerste) {
                CompoundTag c = new CompoundTag();
                c.putString("Id", e.id());
                c.putLong("Dag", e.dag());
                eerste.add(c);
            }
            t.put("Eerste", eerste);
            ListTag wist = new ListTag();
            for (BandData.WistJeDat w : r.wist) {
                CompoundTag c = new CompoundTag();
                c.putString("Tekst", json(Component.translatable(w.key(), w.args().toArray()), registries));
                c.putLong("Dag", w.dag());
                wist.add(c);
            }
            t.put("Wist", wist);
            t.putLong("Sinds", r.sindsDag);
            t.putBoolean("Dood", r.dood);   // 3.0: "In de wolkjes... njeg"
            guhs.add(t);
        }
        root.put("Guhs", guhs);
        root.put("Vadsjes", vadsjes(data, player, registries));
        root.putLong("Dag", Band.dag(s));
        if (focus != null) {
            root.putString("Focus", focus.toString());
        }
        return root;
    }

    /**
     * 1.2.10, the tab "Mijn andere vadsjes": every tamed maatje of this owner (muisjes, Schilly, Poepschilly, landdiertjes...)
     * with its kind, name and place, sorted by kind. One that died (place unknown) is left out.
     */
    private static ListTag vadsjes(BandData data, ServerPlayer player, net.minecraft.core.HolderLookup.Provider registries) {
        List<BandData.Rec> recs = new java.util.ArrayList<>(data.van(player.getUUID()));
        recs.removeIf(r -> r.guh || r.plek == null || r.plek.soort() == PlekSoort.ONBEKEND);
        recs.sort(java.util.Comparator.comparing(r -> r.soort));
        ListTag out = new ListTag();
        for (BandData.Rec r : recs) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", r.id.toString());
            nl.juiced.guhs.taal.Tekst.put(t, "Naam", r.naam);
            t.putString("Soort", r.soort);
            t.putString("Plek", json(GuhVolger.tekst(r.plek), registries));
            t.putString("PlekSoort", r.plek.soort().id());
            out.add(t);
        }
        return out;
    }

    private static String json(Component c, net.minecraft.core.HolderLookup.Provider registries) {
        // 1.1.0 (MC 26.1): Component.Serializer is gone; same compact JSON through the component codec
        return net.minecraft.network.chat.ComponentSerialization.CODEC
                .encodeStart(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), c).getOrThrow().toString();
    }

    /** The favourite kinds this owner's guh has discovered (tests). */
    static Set<FavorietSoort> ontdekt(MinecraftServer s, UUID eigenaar, UUID id) {
        return Favorieten.ontdekt(s, eigenaar, id);
    }

    static List<BandData.Rec> guhs(MinecraftServer s, UUID eigenaar) {
        return BandData.get(s).guhsVan(eigenaar);
    }
}
