package nl.juiced.guhs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Client -> server: "do this with my guh". Sent by the tap/hold handler and by the Guh menu buttons.
 * To add a new menu action: add an Action here, handle it in {@link #apply}, and add a button in GuhScreen.
 */
public record GuhActionPayload(int entityId, Action action, int value, String text) implements CustomPacketPayload {
    public enum Action {
        TAP,
        TOGGLE_TELEPORT,
        TOGGLE_WANDER,
        TOGGLE_GRAVITY,
        TOGGLE_SIT,
        TOGGLE_SOUNDS,
        SET_SOUND_FREQUENCY,
        SET_BEHAVIOR,
        SET_ATTACK_RADIUS,
        RENAME,
        REMOVE_ARMOR,
        REMOVE_CLOTHES,
        /** value = slot, text = the clothes id to put on from the player's inventory ("" = take off). */
        SET_CLOTHES,
        /** Open the wardrobe (clothes + backpack). */
        OPEN_WARDROBE,
        /** Right-click while riding: start the launch, or stop it while flying. */
        LAUNCH,
        /** 2.10: the menu's "Knuffelen!": a big cuddle (hearts, 30 s rest per guh). */
        KNUFFEL,
        /** 2.10: the menu's "Dagboekje": open this guh's page in the Guhdex tab "Mijn guhs". */
        DAGBOEK,
        /** 2.10.1: the menu's "Logeren in de Guhkamer": send this guh to the Guhkamer (like the Guhbel). */
        GUHKAMER_LOGEREN,
        /** 2.10.1: the menu's "Uit de logeerkamer": a guest of the Guhkamer comes to you (like the Guhbel's call). */
        GUHKAMER_UIT,
        /** 3.0: the button of a story variant (its VariantGedrag.speciaalKnop: Baltoguh sniffs the way, the 626 plays the ukelele...). */
        VARIANT_SPECIAAL
    }

    public static final int MAX_NAME_LENGTH = 32;

    public GuhActionPayload(int entityId, Action action) {
        this(entityId, action, 0, "");
    }

    public GuhActionPayload(int entityId, Action action, int value) {
        this(entityId, action, value, "");
    }

    public static final Type<GuhActionPayload> TYPE = new Type<>(Guhs.id("guh_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuhActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GuhActionPayload::entityId,
            ByteBufCodecs.VAR_INT, p -> p.action().ordinal(),
            ByteBufCodecs.VAR_INT, GuhActionPayload::value,
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), GuhActionPayload::text,
            (id, ordinal, value, text) -> new GuhActionPayload(id, Action.values()[Math.floorMod(ordinal, Action.values().length)], value, text));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuhActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.level().getEntity(payload.entityId()) instanceof GuhEntity guh)) {
            return;
        }
        // Only the owner, and only when reasonably close (bigger guhs have bigger reach).
        double reach = 8.0 + guh.getBbWidth() * 2;
        if (!guh.isOwnedBy(player) || player.distanceToSqr(guh) > reach * reach) {
            return;
        }
        apply(guh, player, payload);
    }

    /** Does the action (the owner and distance are already checked; public for the game tests). */
    public static void apply(GuhEntity guh, Player player, GuhActionPayload p) {
        switch (p.action()) {
            case TAP -> guh.onOwnerTap(player);
            case TOGGLE_TELEPORT -> guh.setTeleportEnabled(!guh.isTeleportEnabled());
            case TOGGLE_WANDER -> guh.setWandering(!guh.isWandering());
            case TOGGLE_GRAVITY -> guh.setGravityEnabled(!guh.isGravityEnabled());
            case TOGGLE_SIT -> guh.toggleSit();
            case TOGGLE_SOUNDS -> guh.setSoundsEnabled(!guh.areSoundsEnabled());
            case SET_SOUND_FREQUENCY -> guh.setSoundFrequency(p.value());
            case SET_BEHAVIOR -> guh.setBehavior(GuhEntity.Behavior.values()[Math.floorMod(p.value(), GuhEntity.Behavior.values().length)]);
            case SET_ATTACK_RADIUS -> guh.setAttackRadius(p.value());
            case RENAME -> {
                String name = p.text().strip();
                guh.setCustomName(name.isEmpty() ? null : net.minecraft.network.chat.Component.literal(name));
            }
            case SET_CLOTHES -> {
                // 2.9: clothes are unlocks: only pieces you unlocked, nothing comes back as an item (feature.kleding)
                var slot = nl.juiced.guhs.entity.GuhClothes.Slot.values()[Math.floorMod(p.value(), nl.juiced.guhs.entity.GuhClothes.Slot.values().length)];
                var wanted = nl.juiced.guhs.entity.GuhClothes.byId(p.text());
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    java.util.List<nl.juiced.guhs.entity.GuhClothes> outfit = new java.util.ArrayList<>();
                    for (var s : nl.juiced.guhs.entity.GuhClothes.Slot.kleding()) {
                        outfit.add(s == slot ? wanted : guh.getClothes(s));
                    }
                    nl.juiced.guhs.feature.kleding.KledingKast.kleed(sp, guh, outfit);
                }
            }
            case OPEN_WARDROBE -> guh.openWardrobe(player);
            case LAUNCH -> guh.onLaunchPressed(player);
            case KNUFFEL -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    nl.juiced.guhs.feature.band.BandEvents.knuffel(guh, sp);
                }
            }
            case DAGBOEK -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    nl.juiced.guhs.feature.band.MijnGuhs.openDagboek(sp, guh);
                }
            }
            case GUHKAMER_LOGEREN -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    nl.juiced.guhs.feature.guhkamer.GuhkamerPayloads.menuLogeren(sp, guh);
                }
            }
            case GUHKAMER_UIT -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    nl.juiced.guhs.feature.guhkamer.GuhkamerPayloads.menuUit(sp, guh);
                }
            }
            case VARIANT_SPECIAAL -> {
                var gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(guh);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp && gedrag != null && gedrag.speciaalKnop() != null
                        && player.distanceTo(guh) <= 8 + guh.getBbWidth()) {
                    gedrag.speciaal(guh, sp);
                }
            }
            case REMOVE_CLOTHES -> {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {   // (2.9: all off, except a backpack that still holds things)
                    nl.juiced.guhs.feature.kleding.KledingKast.kleed(sp, guh,
                            java.util.Collections.nCopies(nl.juiced.guhs.entity.GuhClothes.Slot.kleding().size(), null));
                }
            }
            case REMOVE_ARMOR -> {
                if (!guh.getBodyArmorItem().isEmpty()) {
                    player.getInventory().placeItemBackInInventory(guh.getBodyArmorItem().copy());
                    guh.setBodyArmorItem(net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
        }
    }
}
