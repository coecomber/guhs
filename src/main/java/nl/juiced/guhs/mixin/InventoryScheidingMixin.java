package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.scheiding.Thuispost;

/**
 * Y-SCHEIDING (1.10.0): survival items that the rest of the mod hands out while the player's Vadsrijk inventory is live go to the
 * stored home inventory instead (the rule and its exceptions: feature/vadsrijk/scheiding/Thuispost).
 */
@Mixin(Inventory.class)
public abstract class InventoryScheidingMixin {
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void guhs$scheidingAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Thuispost.omleiden((Inventory) (Object) this, stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("HEAD"), cancellable = true)
    private void guhs$scheidingTerug(ItemStack stack, boolean sendPacket, CallbackInfo ci) {
        if (Thuispost.omleiden((Inventory) (Object) this, stack)) {
            ci.cancel();
        }
    }
}
