package nl.juiced.guhs.feature.droomplein;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Droomfontein in the middle of the Droomplein: throw a kaasknabbel in and you hear what a guh is dreaming: a random
 * tamed guh of a random player who is online (its own name and its owner's), with a random dream. A player without a loaded
 * guh: a dream of "a guh of" that player. Nobody else online: one of your own guhs, or the fountain's own dream.
 */
public final class DroomFontein {
    /** The fountain's basin, relative to the plein's centre (the template's water ring, radius ~4.6, one block up). */
    static final int STRAAL = 6, ONDER = 0, BOVEN = 4;
    /** How many dreams there are (gui.guhs.droomplein.fontein.droom0..), and the lines without an owner. */
    static final int DROMEN = 12, ALLEEN = 3;
    /** At most this many of one player's guhs are looked at (per level) to pick a dreamer from. */
    static final int GUH_MAX = 64;

    private DroomFontein() {
    }

    static AABB bak() {
        BlockPos m = DroomEilanden.midden(DroomEilanden.PLEIN);
        return new AABB(m.getX() - STRAAL, m.getY() + ONDER, m.getZ() - STRAAL, m.getX() + STRAAL + 1, m.getY() + BOVEN, m.getZ() + STRAAL + 1);
    }

    static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 != 2) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bak(), i -> i.getItem().is(ModItems.KAAS_KNABBELS.get()))) {
            gooi(level, item);
        }
    }

    /** One knabbel goes in: bubbles, a plop, and the thrower (and everyone at the fountain) hears a guh's dream. */
    static void gooi(ServerLevel level, ItemEntity item) {
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) {
            item.discard();
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, item.getX(), item.getY() + 0.3, item.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 1, item.getZ(), 8, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, item.blockPosition(), SoundEvents.BUBBLE_POP, SoundSource.BLOCKS, 1f, 1.2f);
        Entity gooier = item.getOwner();
        if (gooier instanceof ServerPlayer sp) {
            nl.juiced.guhs.feature.snoesverhaal.DroomBronnen.fontein(sp);   // (integration) the knabbel weerpotje, once
        }
        Component droom = droom(level.getServer(), gooier instanceof ServerPlayer p ? p : null, level.getRandom());
        for (ServerPlayer p : level.players()) {
            if (p == gooier || bak().inflate(12).contains(p.position())) {
                p.sendSystemMessage(droom);
            }
        }
    }

    /** A dream: of a tamed guh of a random online player (preferably not the thrower), with its name and its owner. */
    static Component droom(MinecraftServer server, @Nullable ServerPlayer gooier, RandomSource rnd) {
        List<ServerPlayer> anderen = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p != gooier) {
                anderen.add(p);
            }
        }
        ServerPlayer baas = !anderen.isEmpty() ? anderen.get(rnd.nextInt(anderen.size())) : gooier;
        Component wat = Component.translatable(DroomBrievenbusBlock.T + "fontein.droom" + rnd.nextInt(DROMEN));
        if (baas == null) {
            return kop(Component.translatable(DroomBrievenbusBlock.T + "fontein.alleen" + rnd.nextInt(ALLEEN)));
        }
        GuhEntity guh = guhVan(baas, rnd);
        Component wie = guh != null
                ? Component.translatable(DroomBrievenbusBlock.T + "fontein.wie", guh.getName(), baas.getName())
                : Component.translatable(DroomBrievenbusBlock.T + "fontein.wie_iemand", baas.getName());
        return kop(Component.translatable(DroomBrievenbusBlock.T + "fontein.droomt", wie, wat));
    }

    private static Component kop(Component tekst) {
        return Component.translatable(DroomBrievenbusBlock.T + "fontein.kop").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" ")).append(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }

    /** A random tamed guh of this player, anywhere it is loaded (a dreaming player's guhs sleep at home: another level). */
    @Nullable
    static GuhEntity guhVan(ServerPlayer baas, RandomSource rnd) {
        List<GuhEntity> guhs = new ArrayList<>();
        for (ServerLevel l : baas.level().getServer().getAllLevels()) {
            l.getEntities(EntityTypeTest.forClass(GuhEntity.class), g -> g.isOwnedBy(baas), guhs, GUH_MAX);
        }
        return guhs.isEmpty() ? null : guhs.get(rnd.nextInt(guhs.size()));
    }
}
