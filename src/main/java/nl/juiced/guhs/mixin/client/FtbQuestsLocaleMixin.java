package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * 1.2.0: FTB Quests (optional, hence @Pseudo and require = 0) shows the Guhs chapters in the language of the Guhs switch
 * (compat/FtbQuestsTaal.locale). Without FTB Quests this mixin does nothing.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.client.ClientQuestFile", remap = false)
public abstract class FtbQuestsLocaleMixin {
    @ModifyReturnValue(method = "getLocale", at = @At("RETURN"), require = 0)
    private String guhs$taal(String locale) {
        return nl.juiced.guhs.compat.FtbQuestsTaal.locale(locale);
    }
}
