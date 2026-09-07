package com.coolpick.tlmvision.mixin;
import com.coolpick.tlmvision.compat.OpenCodeGoSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.LLMSiteEditorScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TLM's editor constructs a base OpenAI site; keep the custom protocol when saving. */
@Pseudo
@Mixin(value = LLMSiteEditorScreen.class, remap = false)
public abstract class SiteEditorMixin {
    @Shadow @Final private LLMSite sourceSite;
    // The decision and the construction live in OpenCodeGoSite: see preserveProvider there for why.
    @Inject(method = "buildSite", at = @At("RETURN"), cancellable = true)
    private void preserveProvider(CallbackInfoReturnable<LLMSite> result) {
        result.setReturnValue(OpenCodeGoSite.preserveProvider(sourceSite, result.getReturnValue()));
    }
}
