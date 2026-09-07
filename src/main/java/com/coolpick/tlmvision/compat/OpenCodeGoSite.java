package com.coolpick.tlmvision.compat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** One saved key and automatic model-specific routing, in TLM's normal provider list. */
public final class OpenCodeGoSite extends LLMOpenAISite {
    public static final String TYPE = "opencode_go";
    public OpenCodeGoSite() {
        super(TYPE, ResourceLocation.fromNamespaceAndPath("tlmvision", "textures/gui/opencode_go.png"), ProviderProtocol.GO_BASE,
                false, "", false, Map.of(), defaults());
    }
    public OpenCodeGoSite(LLMOpenAISite site) {
        super(site.id(), site.icon(), site.url(), site.enabled(), site.secretKey(), site.hasThinkingField(), new HashMap<>(site.headers()), new LinkedHashMap<>(site.modelEntries()));
    }
    /**
     * Mixin bytecode must not contain {@code new OpenCodeGoSite(...)}: the Mixin preprocessor
     * resolves the owner of every INVOKESPECIAL when it attaches, and mod classes are not
     * visible to it then, which fails the whole transform and stops the editor screen opening.
     */
    public static com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite preserveProvider(
            com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite source,
            com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite edited) {
        if (source instanceof OpenCodeGoSite && edited instanceof LLMOpenAISite openAi) return new OpenCodeGoSite(openAi);
        return edited;
    }
    @Override public String getApiType() { return TYPE; }
    @Override public String getNameKey() { return "tlmvision.provider.opencode_go"; }
    @Override public LLMClient client() { return new OpenCodeGoClient(LLM_HTTP_CLIENT, this); }
    private static Map<String, ModelEntry> defaults() {
        Map<String, ModelEntry> models = new LinkedHashMap<>();
        for (String id : List.of("mimo-v2.5", "mimo-v2.5-pro", "gpt-5.6-luna", "grok-4.6", "glm-5.3-flash", "glm-5.3", "glm-5.2", "glm-5.1", "kimi-k3", "kimi-k2.7-code", "kimi-k2.6", "longcat-2.0", "deepseek-v4-pro", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp", "minimax-m3", "minimax-m2.7", "qwen3.8-max", "qwen3.8-flash", "qwen3.7-max", "qwen3.7-plus", "qwen3.6-plus", "muse-spark-1.3-contributor", "muse-spark-1.2-contributor", "hy4-preview", "hy3", "omen-alpha")) models.put(id, new ModelEntry(id));
        return models;
    }
    public static final class Serializer implements SerializableSite<OpenCodeGoSite> {
        private static final Codec<OpenCodeGoSite> CODEC = new LLMOpenAISite.Serializer().codec().xmap(OpenCodeGoSite::new, site -> site);
        @Override public Codec<OpenCodeGoSite> codec() { return CODEC; }
        @Override public OpenCodeGoSite defaultSite() { return new OpenCodeGoSite(); }
    }
}
