package nl.juiced.guhs.feature.huisje;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.network.ModNetworking;

/**
 * 1.2.8: the overview of the huisje screen ("Wat kan hier?"): what the blue area around one huisje offers right now.
 * <ul>
 *   <li>per chore (all of them, in the screen's order): can it be done ({@link KlusStand}, answered by the chore itself
 *       from its own checks: {@link Klus#stand}), who can do it ({@link Klus#doeners}) and how many residents of this
 *       huisje can (and have it switched on);</li>
 *   <li>per thing residents play with at random ({@link Speelgoed#tel}: the toys, counted the way they are searched) and
 *       the two other things guhs react to by themselves (a jukebox: they dance while it plays; a burning campfire: pyjamas
 *       late in the evening): how many stand in the area, also when that is none; bbq2: and the working Hapluikjes
 *       (where chore output goes when no Bank Guh stands in the area).</li>
 * </ul>
 * The client asks ({@link HuisjePayloads.OverzichtVraag}: when the dialog opens, on its refresh button and every few
 * seconds while it is open); only the owner (or an op) within 24 blocks gets an answer, like the screen's buttons, and at
 * most once per {@link #RUST} ticks per player. Cheap: the blocks come from the chores' own scan ({@link KlusGebied#van},
 * kept {@link KlusGebied#GELDIG} ticks per huisje), plus a handful of entity lookups in the area.
 */
public final class HuisjeOverzicht {
    /** Minimum ticks between two answers to one player. */
    public static final int RUST = 10;
    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

    private HuisjeOverzicht() {
    }

    /** The payload's handler: rate-limited, then {@link #vraag}. */
    static void handle(ServerPlayer player, BlockPos pos) {
        long nu = player.level().getGameTime();
        Long vorige = LAATST.get(player.getUUID());
        if (vorige != null && nu >= vorige && nu - vorige < RUST) {
            return;
        }
        LAATST.put(player.getUUID(), nu);
        if (LAATST.size() > 256) {
            LAATST.entrySet().removeIf(e -> e.getValue() + RUST < nu || e.getValue() > nu);
        }
        vraag(player, pos);
    }

    /**
     * A player asks for the overview of the huisje at pos: checked like the screen's buttons (same level, within 24 blocks,
     * a huisje there, its owner or an op). Sends the answer and returns it; null = refused.
     */
    @Nullable
    public static CompoundTag vraag(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level) || player.distanceToSqr(pos.getCenter()) > 24 * 24) {
            return null;
        }
        Huisje h = Huisjes.op(level.getServer(), level.dimension(), pos);
        if (h == null) {
            return null;
        }
        if (!Huisjes.magBewerken(player, h)) {
            player.sendOverlayMessage(Huisjes.vanWie(h).copy().withStyle(ChatFormatting.GRAY));
            return null;
        }
        CompoundTag data = data(level, h);
        ModNetworking.sendTo(player, new HuisjePayloads.Overzicht(data));
        return data;
    }

    /** What the dialog shows. */
    public static CompoundTag data(ServerLevel level, Huisje h) {
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", h.pos().asLong());
        // the residents, once (loaded ones are asked themselves, the others count as what they were when they moved in)
        Mob[] geladen = new Mob[h.bewoners().size()];
        boolean[] guh = new boolean[geladen.length];
        int i = 0;
        for (UUID id : h.bewoners()) {
            Entity e = Huisjes.zoekBewoner(level, h, id);
            geladen[i] = e instanceof Mob m ? m : null;
            guh[i] = "guh".equals(h.soort(id));
            i++;
        }
        ListTag klusjes = new ListTag();
        for (Klus k : Klusjes.alle()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", k.id());
            c.putString("Icoon", BuiltInRegistries.ITEM.getKey(k.icoon().getItem()).toString());
            KlusStand stand;
            try {
                stand = k.stand(level, h);
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("Klus {} overview failed", k.id(), ex);
                stand = KlusStand.ONBEKEND;
            }
            c.putByte("Staat", (byte) stand.staat().ordinal());
            c.putString("Reden", stand.reden());
            c.putInt("Aantal", stand.aantal());
            c.putString("Wie", k.doeners());
            int kunnen = 0, aan = 0;
            i = 0;
            for (UUID id : h.bewoners()) {
                boolean kan;
                try {
                    kan = geladen[i] != null ? k.kan(geladen[i]) : guh[i];
                } catch (RuntimeException ex) {
                    kan = false;
                }
                if (kan) {
                    kunnen++;
                    if (geladen[i] != null ? h.klusAan(geladen[i], k.id()) : h.klusAan(id, k.id())) {
                        aan++;
                    }
                }
                i++;
            }
            c.putInt("Kunnen", kunnen);
            c.putInt("Aan", aan);
            klusjes.add(c);
        }
        t.put("Klusjes", klusjes);
        ListTag dingen = new ListTag();
        for (Speeltje.Telling s : Speelgoed.tel(level, h.pos(), Huisjes.BEREIK,
                p -> h.inGebied(p) && KlusGebied.inTest(h, Vec3.atCenterOf(p)))) {
            dingen.add(ding(s.id(), s.icoon(), s.icoon().getItem().getDescriptionId(), s.aantal()));
        }
        dingen.add(ding("jukebox", new ItemStack(Items.JUKEBOX), "gui.guhs.huisje.overzicht.ding.jukebox.naam",
                KlusGebied.van(level, h, KlusGebied.Soort.JUKEBOX).size()));
        dingen.add(ding("kampvuur", new ItemStack(Items.CAMPFIRE), "gui.guhs.huisje.overzicht.ding.kampvuur.naam",
                KlusGebied.van(level, h, KlusGebied.Soort.KAMPVUUR).size()));
        // bbq2: the working Hapluikjes of the area (chore output goes into one when no Bank Guh stands in the area)
        dingen.add(ding("hapluikje", new ItemStack(BankFeature.HAPLUIKJE_ITEM.get()), "block.guhs.hapluikje", Voorraad.luikjes(level, h).size()));
        t.put("Dingen", dingen);
        return t;
    }

    private static CompoundTag ding(String id, ItemStack icoon, String naam, int aantal) {
        CompoundTag c = new CompoundTag();
        c.putString("Id", id);
        c.putString("Icoon", BuiltInRegistries.ITEM.getKey(icoon.getItem()).toString());
        c.putString("Naam", naam);
        c.putInt("Aantal", aantal);
        return c;
    }

    /** (Tests) forgets who asked when. */
    public static void vergeet() {
        LAATST.clear();
    }
}
