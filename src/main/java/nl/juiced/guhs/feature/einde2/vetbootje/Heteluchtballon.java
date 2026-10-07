package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2, slice vetbootje: the small personal heteluchtballon. A player gets it once, as the folded item
 * (guhs:heteluchtballon): use it on the ground to unfold it, sneak + click the balloon to fold it up again. The item
 * remembers whose it is and which copy it is; {@link #opnieuw} hands out the next copy, after which an older item or an
 * older unfolded balloon is "lek" and vanishes when touched (so a lost balloon can be replaced without doubling it).
 * <p>
 * hook fase C: the end reward of the Vredesballon gives it ({@link #geef}); the Werktuigguh can replace a lost one
 * ({@link #opnieuw}).
 */
public final class Heteluchtballon {
    /** Hidden advancement (guhs:quest/...) of the first time a player gets the balloon. */
    public static final String GEKREGEN = "einde2_vetbootje_ballon";

    public static boolean heeft(ServerPlayer p) {
        return Vetbootje.blad(p).ballon;
    }

    /** Gives this player their heteluchtballon, once (a second call does nothing). */
    public static void geef(ServerPlayer p) {
        // hook fase C: the reward of the fifth stage of the Vredesballon calls this
        Vloot.Boot b = Vetbootje.blad(p);
        if (b.ballon) {
            return;
        }
        b.ballon = true;
        b.ballonNr = 1;
        b.eigenaarNaam = p.getGameProfile().name();
        b.bewaar();
        Minigames.give(p, stapel(p.getUUID(), b.ballonNr));
        GuhAdvancements.grant(p, GEKREGEN);
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.gekregen").withStyle(ChatFormatting.GOLD));
    }

    /**
     * A new copy for a player who lost theirs: the old item and the old unfolded balloon stop working. False when the
     * player never had one.
     */
    public static boolean opnieuw(ServerPlayer p) {
        // hook fase C: the Werktuigguh hands out a new one to whoever lost theirs
        Vloot.Boot b = Vetbootje.blad(p);
        if (!b.ballon) {
            return false;
        }
        b.ballonNr++;
        b.bewaar();
        Minigames.give(p, stapel(p.getUUID(), b.ballonNr));
        return true;
    }

    // --- the folded item ----------------------------------------------------------------------------------------------------

    /** The folded balloon of this owner, copy nr. */
    public static ItemStack stapel(@Nullable UUID eigenaar, int nr) {
        ItemStack stack = new ItemStack(E2VetbootjeFeature.HETELUCHTBALLON_ITEM.get());
        if (eigenaar != null) {
            CompoundTag tag = new CompoundTag();
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            tag.putInt("Nr", nr);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    /** Whose folded balloon this is (null: nobody's, a loose one from the creative tab). */
    @Nullable
    public static UUID eigenaar(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag().read("Eigenaar", UUIDUtil.CODEC).orElse(null);
    }

    static int nr(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getIntOr("Nr", 0);
    }

    /** Does this copy still count (nobody's balloon always does)? */
    public static boolean geldig(MinecraftServer server, @Nullable UUID eigenaar, int nr) {
        if (eigenaar == null) {
            return true;
        }
        Vloot.Boot b = Vloot.van(server).boot(eigenaar);
        return b.ballon && b.ballonNr == nr;
    }

    /**
     * Unfolds this folded balloon at a spot (the basket's floor). Null when there is no room, or when the item is an old
     * copy (it is lek). The caller takes the item away.
     */
    @Nullable
    public static HeteluchtballonEntity vouwUit(ServerLevel level, Vec3 plek, float yaw, ItemStack stack) {
        UUID eigenaar = eigenaar(stack);
        if (!geldig(level.getServer(), eigenaar, nr(stack))) {
            return null;
        }
        HeteluchtballonEntity ballon = E2VetbootjeFeature.HETELUCHTBALLON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ballon == null) {
            return null;
        }
        ballon.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        if (!level.noCollision(ballon, ballon.getBoundingBox().deflate(0.05)) || !level.noCollision(ballon.getBoundingBox().move(0, 1.5, 0))) {
            ballon.discard();
            return null;
        }
        ballon.zetEigenaar(eigenaar);
        ballon.nr = nr(stack);
        level.addFreshEntity(ballon);
        level.playSound(null, ballon.blockPosition(), E2VetbootjeFeature.VOUW.get(), SoundSource.NEUTRAL, 0.9f, 0.9f);
        return ballon;
    }

    private Heteluchtballon() {
    }
}
