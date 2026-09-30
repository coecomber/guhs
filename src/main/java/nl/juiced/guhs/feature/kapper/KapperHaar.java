package nl.juiced.guhs.feature.kapper;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.item.GuhClothingItem;

/**
 * Hair on your own tamed guh (outside the kappersshow): a kapsel item gives it that hairstyle for good (the old hair is
 * simply cut off, nothing comes back), a haarverf dyes it, and the kappersschaar (shift + right-click) cuts it all off
 * again. Only the owner can do this, and only on a tamed guh. Rainbow hair keeps changing colour (server side, a few
 * times a second, like a real rainbow guh).
 */
public final class KapperHaar {
    /** Persistent data of a guh: which dye it has (the colour itself is GuhEntity.getHaarkleur). */
    public static final String VERF = "guhs_kapper_verf";

    /** GuhHooks.klik: kapsel items, dyes and the scissors on a guh. */
    public static InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Kapsel kapsel = stack.getItem() instanceof GuhClothingItem c ? Kapsel.van(c.getClothes()) : null;
        Haarverf verf = stack.getItem() instanceof HaarverfItem h ? h.verf : null;
        boolean schaar = stack.getItem() instanceof KappersschaarItem;
        if (kapsel == null && verf == null && !schaar || guh instanceof KapperKlantEntity) {
            return InteractionResult.PASS;
        }
        if (guh.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (!guh.isTame() || !player.getUUID().equals(guh.getOwnerUUID())) {
            player.displayClientMessage(Component.translatable("gui.guhs.kapper.alleen_eigen").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.CONSUME;
        }
        if (kapsel != null) {
            geefKapsel(guh, kapsel);
            stack.consume(1, player);
            KapperVoortgang.eigenGuh(sp, kapsel, null);
            player.displayClientMessage(Component.translatable("gui.guhs.kapper.nieuw_kapsel", guh.getDisplayName(),
                    Component.translatable("gui.guhs.kapper.kapsel." + kapsel.stijl())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else if (verf != null) {
            if (guh.getClothes(GuhClothes.Slot.HAAR) == null) {
                player.displayClientMessage(Component.translatable("gui.guhs.kapper.eerst_kapsel").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResult.CONSUME;
            }
            verf(guh, verf);
            stack.consume(1, player);
            glitter(guh, 12);
            KapperVoortgang.eigenGuh(sp, null, verf);
            player.displayClientMessage(Component.translatable("gui.guhs.kapper.geverfd", guh.getDisplayName(),
                    Component.translatable("gui.guhs.kapper.verf." + verf.kleur())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else if (player.isShiftKeyDown()) {
            if (guh.getClothes(GuhClothes.Slot.HAAR) == null) {
                player.displayClientMessage(Component.translatable("gui.guhs.kapper.al_kaal").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResult.CONSUME;
            }
            knipEraf(guh);
            player.displayClientMessage(Component.translatable("gui.guhs.kapper.kaal", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            GuhClothes haar = guh.getClothes(GuhClothes.Slot.HAAR);
            Kapsel nu = Kapsel.van(haar);
            Haarverf kleur = verfVan(guh);
            player.displayClientMessage(nu == null ? Component.translatable("gui.guhs.kapper.geen_kapsel", guh.getDisplayName())
                    : Component.translatable("gui.guhs.kapper.huidig", guh.getDisplayName(), Component.translatable("gui.guhs.kapper.kapsel." + nu.stijl()),
                    Component.translatable(kleur == null ? "gui.guhs.kapper.verf.naturel" : "gui.guhs.kapper.verf." + kleur.kleur()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Gives a guh this hairstyle (the old one is cut off: nothing comes back). */
    public static void geefKapsel(GuhEntity guh, Kapsel kapsel) {
        guh.wear(kapsel.kleding);
        knip(guh, 10);
        guh.triggerAnim("action", "happy");
    }

    /** Dyes a guh's hair (null: natural again). */
    public static void verf(GuhEntity guh, @Nullable Haarverf verf) {
        if (verf == null) {
            guh.getPersistentData().remove(VERF);
            guh.setHaarkleur(-1);
        } else {
            guh.getPersistentData().putString(VERF, verf.kleur());
            guh.setHaarkleur(verf == Haarverf.REGENBOOG ? regenboog(guh.level().getGameTime() + guh.getId() * 7L) : verf.rgb);
        }
    }

    /** The dye of this guh's hair, or null (natural). */
    @Nullable
    public static Haarverf verfVan(GuhEntity guh) {
        String id = guh.getPersistentData().getString(VERF);
        return id.isEmpty() ? null : Haarverf.byId(id);
    }

    /** All hair off, natural colour again. */
    public static void knipEraf(GuhEntity guh) {
        guh.takeOff(GuhClothes.Slot.HAAR);
        verf(guh, null);
        knip(guh, 16);
    }

    /** GuhHooks.tick: rainbow hair runs through the colours. */
    public static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 5 == 0 && guh.getClothes(GuhClothes.Slot.HAAR) != null
                && Haarverf.REGENBOOG.kleur().equals(guh.getPersistentData().getString(VERF))) {
            guh.setHaarkleur(regenboog(guh.level().getGameTime() + guh.getId() * 7L));
        }
    }

    /** The rainbow colour at this moment (a soft pastel rainbow, once round in 8 seconds). */
    public static int regenboog(long time) {
        float hue = (time % 160) / 160f;
        return Mth.hsvToRgb(hue, 0.45f, 1.0f) & 0xFFFFFF;
    }

    /** Snip snip: the scissors' sound and little tufts of hair. */
    static void knip(GuhEntity guh, int plukjes) {
        if (guh.level() instanceof ServerLevel level) {
            level.playSound(null, guh.blockPosition(), KapperFeature.KNIP.get(), SoundSource.NEUTRAL, 1f, 1.1f + guh.getRandom().nextFloat() * 0.3f);
            level.sendParticles(KapperFeature.HAARPLUKJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.1, guh.getZ(), plukjes, 0.3, 0.15, 0.3, 0.02);
        }
    }

    /** Sparkles round a freshly dyed head. */
    static void glitter(GuhEntity guh, int n) {
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(KapperFeature.KRULGLITTER.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), n, 0.35, 0.2, 0.35, 0.01);
            level.playSound(null, guh.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.4f);
        }
    }

    private KapperHaar() {
    }
}
