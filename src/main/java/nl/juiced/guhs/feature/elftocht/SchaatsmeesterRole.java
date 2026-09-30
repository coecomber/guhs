package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;

/**
 * Schaatsmeester Guhglij (SCHAATSMEESTERGUH) at the start in Guhwarden: his screen ({@code ElftochtPayloads.Open}) has
 * "Start de tocht" (skates + stempelkaart + the countdown), "Vrij schaatsen", "Stoppen" and his shop; above him floats the
 * board of the fastest tours. His RoleData ({@code StartVooruit}, {@code StartLinks}, from the template) says where the
 * start line is, seen from where he stands and faces ({@code Kijk}: his yaw when he came into the world).
 */
public class SchaatsmeesterRole implements NpcRole {
    public static final int START = 0, VRIJ = 1, STOP = 2, SHOP = 3;
    public static final int PRIJS_MUTS = 4, PRIJS_TRUITJE = 10, PRIJS_SJAAL = 6, PRIJS_OORWARMERS = 6;

    /** Which way he faces (as placed; he turns his head to look at people). */
    public static float kijk(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Kijk")) {
            npc.roleData.putFloat("Kijk", npc.getYRot());
        }
        return npc.roleData.getFloat("Kijk");
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        kijk(npc);
        ElftochtVoortgang.kleding(player);
        CompoundTag data = new CompoundTag();
        ElftochtTocht.Rit rit = ElftochtTocht.rit(player);
        data.putInt("Bezig", rit == null ? 0 : rit.vrij ? 2 : 1);
        CompoundTag saved = GuhQuests.saved(player);
        int[] pb = saved.getIntArray(ElftochtTocht.PB);
        data.putInt("Best", pb.length == ElftochtTocht.VOLGORDE.length ? pb[pb.length - 1] : -1);
        data.putInt("Ritten", saved.getInt(ElftochtTocht.RITTEN));
        List<Scorebord.Entry> top = Scorebord.top(player.server, ElftochtTocht.BOARD);
        if (!top.isEmpty()) {
            data.putString("RecordNaam", top.get(0).name());
            data.putInt("Record", top.get(0).score());
        }
        GuhQuests.say(player, npc, rit == null ? "quest.guhs.elftocht.hallo" : "quest.guhs.elftocht.hallo_bezig");
        ModNetworking.sendTo(player, new ElftochtPayloads.Open(npc.getId(), data));
    }

    /** A button of his screen. */
    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.SCHAATSMEESTERGUH || npc.distanceToSqr(player) > 64) {
            return;
        }
        switch (action) {
            case START -> ElftochtTocht.start(npc, player, false);
            case VRIJ -> ElftochtTocht.start(npc, player, true);
            case STOP -> {
                if (ElftochtTocht.isBezig(player)) {
                    ElftochtTocht.stop(player, null);
                    GuhQuests.say(player, npc, "quest.guhs.elftocht.gestopt");
                }
            }
            case SHOP -> npc.openShop(player);
            default -> {
            }
        }
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount == 1 || npc.tickCount % 200 == 0) {
            kijk(npc);
        }
        if (npc.tickCount % 100 == 7 && npc.level() instanceof ServerLevel level) {
            Scorebord.show(level, npc.position().add(0, 2.7, 0), ElftochtTocht.BOARD, Scorebord.text(level.getServer(),
                    Component.translatable("gui.guhs.scorebord.elftocht"), List.of(ElftochtTocht.BOARD),
                    List.of(Component.translatable("gui.guhs.scorebord.elftocht.tijd")), Highscores::tijd));
        }
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_MUTS, new ItemStack(ModItems.clothingItem(GuhClothes.ELFTOCHT_SCHAATSMUTS))));
        offers.add(offer(PRIJS_SJAAL, new ItemStack(ModItems.clothingItem(GuhClothes.ELFTOCHT_SJAAL))));
        offers.add(offer(PRIJS_OORWARMERS, new ItemStack(ModItems.clothingItem(GuhClothes.ELFTOCHT_OORWARMERS))));
        offers.add(offer(PRIJS_TRUITJE, new ItemStack(ModItems.clothingItem(GuhClothes.ELFTOCHT_TRUITJE))));
        offers.add(offer(1, new ItemStack(ElftochtFeature.WARME_CHOCOVET.get(), 2)));
        offers.add(offer(1, new ItemStack(ElftochtFeature.SNERT_KOMMETJE.get(), 2)));
        offers.add(offer(2, new ItemStack(ElftochtFeature.LAMPION_ITEM.get(), 2)));
        offers.add(offer(3, new ItemStack(ElftochtFeature.VUURKORF_ITEM.get())));
        return offers;
    }

    private static MerchantOffer offer(int prijs, ItemStack wat) {
        return new MerchantOffer(new ItemCost(ElftochtFeature.ELFSTEMPEL.get(), prijs), wat, Integer.MAX_VALUE, 0, 0);
    }
}
