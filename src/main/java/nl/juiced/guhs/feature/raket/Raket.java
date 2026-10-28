package nl.juiced.guhs.feature.raket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * What a click on the Lanceerplatform does (texts gui.guhs.raket.*): hand in the current stage, call the crew, launch, or
 * (after the launch) fly to the Kaasmaan. The advancements quest/raket_romp, _motor, _brandstof, _bemanning and
 * raket_gelanceerd are the FTB quests' tasks.
 */
public final class Raket {
    private static final String G = "gui.guhs.raket.";

    private Raket() {
    }

    static void klik(ServerLevel level, BlockPos pos, LanceerplatformBlockEntity be, ServerPlayer p) {
        be.zetBouwer(p.getUUID());
        if (p.isShiftKeyDown()) {
            toonNodig(be, p);
            return;
        }
        int bezig = be.bezig();
        for (int s = 1; s <= be.klaar(); s++) {
            RaketBouw.plaats(level, pos, s, false);                                       // someone broke a bit: it grows back
        }
        if (bezig > RaketStappen.STAPPEN.size()) {
            if (!be.gelanceerd()) {
                lanceer(level, pos, be, p);
            } else {
                KaasMaan.naarMaan(p, pos);
            }
            return;
        }
        RaketStappen.Stap stap = RaketStappen.STAPPEN.get(bezig - 1);
        if (stap.bemanning()) {
            bemanning(level, pos, be, p);
        } else {
            lever(level, pos, be, p, stap);
        }
    }

    /** Hands in what the player carries of this stage; with all of it in, the stage is built. Returns how much went in. */
    static int lever(ServerLevel level, BlockPos pos, LanceerplatformBlockEntity be, ServerPlayer p, RaketStappen.Stap stap) {
        int totaal = 0;
        for (int i = 0; i < stap.eisen().size(); i++) {
            RaketStappen.Eis e = stap.eisen().get(i);
            int nodig = e.aantal() - be.binnen(i);
            if (nodig <= 0) {
                continue;
            }
            int n = neem(p, e.item().get(), nodig);
            if (n > 0) {
                be.zetBinnen(i, be.binnen(i) + n);
                totaal += n;
            }
        }
        boolean compleet = true;
        for (int i = 0; i < stap.eisen().size(); i++) {
            compleet &= be.binnen(i) >= stap.eisen().get(i).aantal();
        }
        if (compleet) {
            stapKlaar(level, pos, be, p, stap);
        } else if (totaal > 0) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
            p.sendSystemMessage(Component.translatable(G + "erin", totaal).withStyle(ChatFormatting.LIGHT_PURPLE));
            toonNodig(be, p);
        } else {
            toonNodig(be, p);
        }
        return totaal;
    }

    private static void stapKlaar(ServerLevel level, BlockPos pos, LanceerplatformBlockEntity be, ServerPlayer p, RaketStappen.Stap stap) {
        be.stapKlaar();
        RaketBouw.plaats(level, pos, be.klaar(), true);
        GuhAdvancements.grant(p, "raket_" + stap.key());
        p.sendSystemMessage(Component.translatable(G + "stap.klaar", Component.translatable(G + "stap." + stap.key()))
                .withStyle(ChatFormatting.GOLD));
        if (be.bezig() <= RaketStappen.STAPPEN.size()) {
            p.sendSystemMessage(Component.translatable(G + "volgende", Component.translatable(G + "stap."
                    + RaketStappen.STAPPEN.get(be.bezig() - 1).key())).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            p.sendSystemMessage(Component.translatable(G + "klaar_voor_lancering").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Takes up to {@code max} of an item from the player's inventory (a bucket gives its empty bucket back). */
    static int neem(ServerPlayer p, Item item, int max) {
        Inventory inv = p.getInventory();
        int genomen = 0;
        for (int i = 0; i < inv.getContainerSize() && genomen < max; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) {
                continue;
            }
            int n = Math.min(s.getCount(), max - genomen);
            s.shrink(n);
            genomen += n;
            if (isEmmer(item)) {                                          // a full bucket gives its empty bucket back
                ItemStack leeg = new ItemStack(Items.BUCKET, n);
                if (!inv.add(leeg)) {
                    p.drop(leeg, false);
                }
            }
        }
        return genomen;
    }

    private static boolean isEmmer(Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_bucket");
    }

    /** The crew: three tame guhs of the player, in an astronaut suit, near the platform; they climb into the cockpit. */
    static boolean bemanning(ServerLevel level, BlockPos pos, LanceerplatformBlockEntity be, ServerPlayer p) {
        List<GuhEntity> guhs = astronauten(level, pos, p);
        if (guhs.size() < RaketStappen.BEMANNING) {
            p.sendSystemMessage(Component.translatable(G + "bemanning.nodig", guhs.size(), RaketStappen.BEMANNING)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        RaketBouw.plaats(level, pos, 4, false);                       // the cockpit first, so they have a floor
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < RaketStappen.BEMANNING; i++) {
            GuhEntity g = guhs.get(i);
            Vec3 stoel = Vec3.atBottomCenterOf(pos.offset(RaketBouw.STOELEN.get(i)));
            g.getNavigation().stop();
            g.snapTo(stoel.x, stoel.y, stoel.z, 180f, 0f);
            g.setOrderedToSit(true);
            g.setInSittingPose(true);
            g.addTag(KaasMaan.TAG_BEMANNING);
            ids.add(g.getUUID());
        }
        be.zetBemanning(ids);
        stapKlaar(level, pos, be, p, RaketStappen.STAPPEN.get(RaketStappen.STAPPEN.size() - 1));
        return true;
    }

    static List<GuhEntity> astronauten(ServerLevel level, BlockPos pos, ServerPlayer p) {
        AABB box = new AABB(pos).inflate(RaketStappen.BEMANNING_BEREIK);
        return level.getEntitiesOfClass(GuhEntity.class, box, g -> g.isTame() && g.isOwnedBy(p)
                && g.getClothes(GuhClothes.Slot.BODY) == GuhClothes.ASTRONAUTENPAKJE);
    }

    /** All four done: the launch (the scene; without it straight on), then off to the Kaasmaan. */
    static void lanceer(ServerLevel level, BlockPos pos, LanceerplatformBlockEntity be, ServerPlayer p) {
        boolean gestart = Cutscenes.speel(p, RaketScene.LANCERING, pos, Rotation.NONE, q -> naLancering(level, pos, q));
        if (!gestart) {
            naLancering(level, pos, p);
        }
    }

    static void naLancering(ServerLevel level, BlockPos pos, ServerPlayer p) {
        if (level.getBlockEntity(pos) instanceof LanceerplatformBlockEntity be) {
            be.zetGelanceerd(true);
        }
        GuhAdvancements.grant(p, "raket_gelanceerd");
        p.sendSystemMessage(Component.translatable(G + "gelanceerd").withStyle(ChatFormatting.GOLD));
        KaasMaan.naarMaan(p, pos);
    }

    static void toonNodig(LanceerplatformBlockEntity be, ServerPlayer p) {
        int bezig = be.bezig();
        if (bezig > RaketStappen.STAPPEN.size()) {
            p.sendSystemMessage(Component.translatable(be.gelanceerd() ? G + "naar_maan" : G + "klaar_voor_lancering")
                    .withStyle(ChatFormatting.AQUA));
            return;
        }
        RaketStappen.Stap stap = RaketStappen.STAPPEN.get(bezig - 1);
        p.sendSystemMessage(Component.translatable(G + "stap.kop", bezig, RaketStappen.STAPPEN.size(),
                Component.translatable(G + "stap." + stap.key())).withStyle(ChatFormatting.GOLD));
        if (stap.bemanning()) {
            p.sendSystemMessage(Component.translatable(G + "bemanning.uitleg", RaketStappen.BEMANNING).withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < stap.eisen().size(); i++) {
            RaketStappen.Eis e = stap.eisen().get(i);
            int in = Math.min(be.binnen(i), e.aantal());
            MutableComponent r = Component.literal(" - ").append(e.item().get().getName(e.item().get().getDefaultInstance()))
                    .append(Component.literal(": " + in + "/" + e.aantal()));
            p.sendSystemMessage(r.withStyle(in >= e.aantal() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }
}
