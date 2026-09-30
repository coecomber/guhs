package nl.juiced.guhs.feature.guheinde.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.GuhvleugelsItem;

/** The Guhvleugels on your back: the elytra model with little purple Enderguh wings. */
public class GuhvleugelsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M> {
    private static final Identifier TEXTURE = Guhs.id("textures/entity/guhvleugels.png");

    public GuhvleugelsLayer(RenderLayerParent<T, M> renderer, EntityModelSet models) {
        super(renderer, models);
    }

    @Override
    public boolean shouldRender(ItemStack stack, T entity) {
        return stack.getItem() instanceof GuhvleugelsItem;
    }

    @Override
    public Identifier getElytraTexture(ItemStack stack, T entity) {
        return TEXTURE;
    }
}
