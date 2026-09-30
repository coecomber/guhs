package nl.juiced.guhs.feature.piep;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A maatje on your shoulder (2.8.1 the muisje; 3.0 any maatje with {@link PiepMaatje#kanOpSchouder}, e.g. the
 * pluiseekhoorntje): it lives in the player's persistent data ({@value #KEY}: the whole maatje as NBT, with its entity type
 * in "id") instead of in the world, like a vanilla parrot, and every client that sees the player draws it there (payload
 * guhs:piep_schouder, client: PiepClient's shoulder layer, left shoulder, one at a time). It comes down when you sneak +
 * right-click a block with an empty hand, and when you die (next to where you fell).
 */
public final class Schouder {
    public static final String KEY = "guhs_piep_schouder";

    public static boolean heeft(ServerPlayer player) {
        return player.getPersistentData().contains(KEY);
    }

    /** The maatje goes onto the player's shoulder (it leaves the world if it was in it). */
    public static void zet(ServerPlayer player, PiepMaatje maatje) {
        TamableAnimal dier = maatje.dier();
        if (dier.isTame() && dier.getOwnerUUID() != null) {   // 2.10: "waar is mijn guh": on a shoulder
            nl.juiced.guhs.feature.band.GuhVolger.zet(dier.getOwnerUUID(), nl.juiced.guhs.feature.band.Band.id(dier), new nl.juiced.guhs.feature.band.Plek(nl.juiced.guhs.feature.band.PlekSoort.SCHOUDER,
                    player.level().dimension(), player.blockPosition(), player.getGameProfile().name(), player.level().getGameTime()));
        }
        CompoundTag tag = new CompoundTag();
        dier.saveWithoutId(tag);
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(dier.getType()).toString());
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        dier.discard();                                       // (it leaves the world, if it was in it)
        player.getPersistentData().put(KEY, tag);
        player.level().playSound(null, player.blockPosition(), maatje.oppakGeluid(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.5f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep.op_schouder").withStyle(ChatFormatting.LIGHT_PURPLE));
        sync(player);
    }

    /** The maatje hops down here; returns it (null: there was none). */
    @Nullable
    public static PiepMaatje eraf(ServerPlayer player, Vec3 at) {
        if (!heeft(player)) {
            return null;
        }
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(KEY);
        player.getPersistentData().remove(KEY);
        sync(player);
        ServerLevel level = player.level();
        Entity e = maak(tag, level);
        if (!(e instanceof PiepMaatje maatje) || !(e instanceof TamableAnimal dier)) {
            return null;
        }
        dier.snapTo(at.x, at.y, at.z, player.getYRot(), 0);
        dier.setDeltaMovement(Vec3.ZERO);
        dier.setPersistenceRequired();
        level.addFreshEntity(dier);
        dier.playSound(maatje.oppakGeluid(), 0.8f, 1.3f);
        if (dier.isTame() && dier.getOwnerUUID() != null) {   // 2.10: "waar is mijn guh": back on its paws
            nl.juiced.guhs.feature.band.GuhVolger.zet(dier, nl.juiced.guhs.feature.band.PlekSoort.WERELD, "");
        }
        return maatje;
    }

    /** The maatje of a shoulder tag (its "id"; older saves without one: a muisje), not yet in the world. */
    @Nullable
    public static Entity maak(CompoundTag tag, net.minecraft.world.level.Level level) {
        EntityType<?> type = tag.contains("id") ? EntityType.byString(tag.getStringOr("id", "")).orElse(PiepFeature.PIEPPIEPMUISJE.get())
                : PiepFeature.PIEPPIEPMUISJE.get();
        Entity e = type.create(level, EntitySpawnReason.TRIGGERED);
        if (e != null) {
            CompoundTag copy = tag.copy();
            copy.remove("id");
            e.load(copy);
        }
        return e;
    }

    /** Tells everyone who sees this player (and the player) whether a maatje sits on their shoulder (and which). */
    public static void sync(ServerPlayer player) {
        PiepPayloads.naarKijkers(player, bericht(player));
    }

    static PiepPayloads.SchouderData bericht(ServerPlayer player) {
        CompoundTag tag = heeft(player) ? player.getPersistentData().getCompoundOrEmpty(KEY) : new CompoundTag();
        CompoundTag uiterlijk = new CompoundTag();
        if (tag.contains("CustomName")) {
            uiterlijk.putString("CustomName", tag.getStringOr("CustomName", ""));
        }
        // 3.0: the type (and its look: the whole maatje minus its inventory-like parts) so the client draws the right one
        uiterlijk.putString("id", tag.contains("id") ? tag.getStringOr("id", "") : BuiltInRegistries.ENTITY_TYPE.getKey(PiepFeature.PIEPPIEPMUISJE.get()).toString());
        CompoundTag look = tag.copy();
        for (String k : new String[]{"Inventory", "Items", "ArmorItems", "HandItems", "Brain", "Attributes", "NeoForgeData", "neoforge:attachments"}) {
            look.remove(k);
        }
        uiterlijk.put("Uiterlijk", look);
        return new PiepPayloads.SchouderData(player.getId(), heeft(player), uiterlijk);
    }

    private Schouder() {
    }
}
