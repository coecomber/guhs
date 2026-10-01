package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * 1.2.0: the pictures of the Guhs chapters with text painted in (the chapter titles and section headers) have an English
 * twin in textures/ftbquests/&lt;chapter&gt;/en/; with the Guhs texts in English the quest book shows that one
 * (compat/FtbQuestsTaal.image). FTB Quests is optional: @Pseudo and require = 0, client only.
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.quest.ChapterImage", remap = false)
public abstract class FtbQuestsImageMixin {
    @ModifyReturnValue(method = "getImage", at = @At("RETURN"), require = 0)
    private dev.ftb.mods.ftblibrary.icon.Icon<?> guhs$taal(dev.ftb.mods.ftblibrary.icon.Icon<?> icon) {
        return nl.juiced.guhs.compat.FtbQuestsTaal.image(icon);
    }
}
