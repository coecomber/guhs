package nl.juiced.guhs.mixin;

import java.util.Optional;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.ResourceManagerTemplateSource;
import nl.juiced.guhs.feature.vadsrijk.grootte.StructuurPakLader;

/**
 * U-GROOTTE: the guhs structures ship as one compact pack (data/guhs/structuren.gpak) instead of 1263 .nbt files; a structure
 * whose .nbt is not in the resources is read from the pack (see {@link StructuurPakLader}). Only this resource source: the
 * world's own saved structures (generated/) and the test structures still come first, as in vanilla.
 */
@Mixin(ResourceManagerTemplateSource.class)
public abstract class StructuurPakMixin {
    @Shadow
    private ResourceManager resourceManager;

    @Inject(method = "load(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void guhs$uitPak(Identifier id, CallbackInfoReturnable<Optional<StructureTemplate>> cir) {
        Optional<StructureTemplate> t = StructuurPakLader.laad(this.resourceManager, id);
        if (t != null) {
            cir.setReturnValue(t);
        }
    }

    @Inject(method = "list", at = @At("RETURN"), cancellable = true)
    private void guhs$lijst(CallbackInfoReturnable<Stream<Identifier>> cir) {
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), StructuurPakLader.lijst(this.resourceManager)));
    }
}
