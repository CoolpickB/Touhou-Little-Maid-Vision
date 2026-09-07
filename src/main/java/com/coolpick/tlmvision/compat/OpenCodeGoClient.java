package com.coolpick.tlmvision.compat;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAIClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.request.ChatCompletion;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.FunctionTool;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import java.net.URI;
import java.net.http.*;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public final class OpenCodeGoClient extends LLMOpenAIClient {
    public OpenCodeGoClient(HttpClient http, OpenCodeGoSite site) { super(http, site); }
    /** TLM's LLM error codes: 0 sending, 1 received, 2 decode. Any other value renders as an empty chat line. */
    private static final int SENDING = 0, DECODE = 2;
    @Override public void chat(LLMCallback callback) {
        HttpRequest request;
        if (site.secretKey() == null || site.secretKey().isBlank()) {
            callback.onFailure(null, new Throwable("OpenCode Go has no API key. Paste one in Site config."), SENDING);
            return;
        }
        try {
            var maid = callback.getMaid();
            ChatCompletion body = ChatCompletion.create().model(callback.getChatManager().getLLMModel());
            for (var message : callback.getMessages()) switch (message.role()) {
                case USER -> body.userChat(message.message());
                case SYSTEM -> body.systemChat(message.message());
                case ASSISTANT -> body.assistantChat(message.message(), message.toolCalls());
                case TOOL -> body.toolChat(message.message(), message.toolCallId());
                default -> { }
            }
            if (callback.needAddTools) ToolRegister.getAllTools().forEach((name, tool) -> {
                if (tool != null && tool.trigger(maid, body)) body.addTool(FunctionTool.create().setName(name)
                        .setDescription(tool.summary(maid)).setParameters(tool.parameters(ObjectParameter.create(), maid)).build());
            });
            request = ProviderProtocol.request(site.url(), site.secretKey(), site.headers(), GSON.toJsonTree(body).getAsJsonObject(), MAX_TIMEOUT);
        } catch (Exception error) { callback.onFailure(null, error, SENDING); return; }
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
            if (error != null) { handle(callback, response, error, request); return; }
            try {
                // Hand a failed status back untouched so TLM formats its own status-and-body message.
                if (response.statusCode() / 100 != 2) { handle(callback, response, null, request); return; }
                String normalized = ProviderProtocol.decode(response.body(), ProviderProtocol.format(request.uri().toString())).toString();
                handle(callback, new NormalizedResponse(response, normalized), null, request);
            } catch (Exception failure) { callback.onFailure(request, failure, DECODE); }
        });
    }
    private record NormalizedResponse(HttpResponse<String> original, String body) implements HttpResponse<String> {
        public int statusCode() { return original.statusCode(); }
        public HttpRequest request() { return original.request(); }
        public Optional<HttpResponse<String>> previousResponse() { return original.previousResponse(); }
        public HttpHeaders headers() { return original.headers(); }
        public Optional<SSLSession> sslSession() { return original.sslSession(); }
        public URI uri() { return original.uri(); }
        public HttpClient.Version version() { return original.version(); }
    }
}
