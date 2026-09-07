package com.coolpick.tlmvision.compat;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Stateless wire adapters. TLM retains ownership of conversations and tool execution. */
public final class ProviderProtocol {
    public enum Format { CHAT, RESPONSES, MESSAGES }
    public static final String GO_BASE = "https://opencode.ai/zen/go/v1";
    private static final String SESSION = UUID.randomUUID().toString();
    public static boolean isGo(String url) {
        URI uri = URI.create(url);
        return "opencode.ai".equalsIgnoreCase(uri.getHost()) && uri.getPath().startsWith("/zen/go/v1");
    }
    public static Format goFormat(String model) {
        if (model.startsWith("gpt-") || model.startsWith("grok-") || model.startsWith("muse-spark-")) return Format.RESPONSES;
        if (model.startsWith("minimax-") || model.startsWith("qwen")) return Format.MESSAGES;
        return Format.CHAT;
    }
    public static String endpoint(String url, String model) {
        if (!isGo(url)) return url;
        return GO_BASE + switch (goFormat(model)) {
            case CHAT -> "/chat/completions";
            case RESPONSES -> "/responses";
            case MESSAGES -> "/messages";
        };
    }
    public static Format format(String url) {
        String path = URI.create(url).getPath().replaceAll("/+$", "");
        return path.endsWith("/responses") ? Format.RESPONSES : path.endsWith("/messages") ? Format.MESSAGES : Format.CHAT;
    }
    public static HttpRequest request(String url, String key, Map<String,String> headers, JsonObject chat, Duration timeout) {
        String model = str(chat, "model");
        String target = endpoint(url, model);
        Format format = format(target);
        JsonObject body = encode(chat, format);
        if (isGo(target) && model.startsWith("mimo-")) body.add("thinking", object("type", "disabled"));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target)).timeout(timeout);
        headers.forEach(builder::setHeader);
        builder.setHeader("Content-Type", "application/json");
        builder.setHeader("Authorization", "Bearer " + key);
        if (format == Format.MESSAGES) {
            builder.setHeader("x-api-key", key);
            builder.setHeader("anthropic-version", "2023-06-01");
        }
        if (isGo(target)) {
            builder.setHeader("User-Agent", "TLMVision/0.2.0 (Minecraft maid companion)");
            builder.setHeader("x-opencode-session", SESSION);
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
    }
    public static JsonObject encode(JsonObject chat, Format format) {
        if (format == Format.CHAT) return chat.deepCopy();
        JsonObject out = object("model", str(chat, "model"));
        JsonArray messages = new JsonArray(), systems = new JsonArray();
        for (JsonElement entry : array(chat, "messages")) {
            JsonObject msg = entry.getAsJsonObject();
            String role = str(msg, "role");
            if (format == Format.RESPONSES) {
                if (role.equals("tool")) {
                    JsonObject result = object("type", "function_call_output");
                    result.addProperty("call_id", str(msg, "tool_call_id"));
                    result.addProperty("output", text(msg.get("content")));
                    messages.add(result);
                    continue;
                }
                JsonArray parts = parts(msg.get("content"), format, role);
                if (!parts.isEmpty()) {
                    JsonObject message = object("role", role);
                    message.add("content", parts);
                    messages.add(message);
                }
                for (JsonElement call : array(msg, "tool_calls")) {
                    JsonObject c = call.getAsJsonObject(), fn = c.getAsJsonObject("function");
                    JsonObject item = object("type", "function_call");
                    item.addProperty("call_id", str(c, "id"));
                    item.addProperty("name", str(fn, "name"));
                    item.addProperty("arguments", str(fn, "arguments"));
                    messages.add(item);
                }
            } else {
                if (role.equals("system") || role.equals("developer")) { systems.addAll(parts(msg.get("content"), format, role)); continue; }
                JsonArray parts = new JsonArray();
                if (role.equals("tool")) {
                    JsonObject result = object("type", "tool_result");
                    result.addProperty("tool_use_id", str(msg, "tool_call_id"));
                    result.addProperty("content", text(msg.get("content")));
                    parts.add(result); role = "user";
                } else {
                    parts.addAll(parts(msg.get("content"), format, role));
                    for (JsonElement call : array(msg, "tool_calls")) {
                        JsonObject c = call.getAsJsonObject(), fn = c.getAsJsonObject("function");
                        JsonObject item = object("type", "tool_use");
                        item.addProperty("id", str(c, "id")); item.addProperty("name", str(fn, "name"));
                        item.add("input", JsonParser.parseString(str(fn, "arguments"))); parts.add(item);
                    }
                }
                if (!parts.isEmpty()) {
                    // Anthropic requires consecutive messages of the same role to be merged.
                    JsonObject last = messages.isEmpty() ? null : messages.get(messages.size()-1).getAsJsonObject();
                    if (last != null && str(last,"role").equals(role)) last.getAsJsonArray("content").addAll(parts);
                    else { JsonObject message = object("role", role); message.add("content", parts); messages.add(message); }
                }
            }
        }
        out.add(format == Format.RESPONSES ? "input" : "messages", messages);
        int maxTokens = chat.has("max_tokens") ? chat.get("max_tokens").getAsInt() : 4096;
        out.addProperty(format == Format.RESPONSES ? "max_output_tokens" : "max_tokens", maxTokens);
        if (format == Format.RESPONSES) out.addProperty("store", false);
        else if (!systems.isEmpty()) out.add("system", systems);
        JsonArray tools = new JsonArray();
        for (JsonElement entry : array(chat, "tools")) {
            JsonObject function = entry.getAsJsonObject().getAsJsonObject("function").deepCopy();
            if (format == Format.RESPONSES) { function.addProperty("type", "function"); function.addProperty("strict", false); }
            else { function.add("input_schema", function.remove("parameters")); }
            tools.add(function);
        }
        if (!tools.isEmpty()) out.add("tools", tools);
        return out;
    }
    private static JsonArray parts(JsonElement content, Format format, String role) {
        JsonArray result = new JsonArray();
        if (content == null || content.isJsonNull()) return result;
        JsonArray source = content.isJsonArray() ? content.getAsJsonArray() : new JsonArray();
        if (!content.isJsonArray()) { if (text(content).isBlank()) return result; JsonObject t = object("type", "text"); t.addProperty("text", text(content)); source.add(t); }
        for (JsonElement entry : source) {
            JsonObject part = entry.getAsJsonObject();
            if (str(part, "type").equals("image_url")) {
                String url = str(part.getAsJsonObject("image_url"), "url");
                JsonObject image = object("type", format == Format.RESPONSES ? "input_image" : "image");
                if (format == Format.RESPONSES) image.addProperty("image_url", url);
                else {
                    JsonObject src;
                    if (url.startsWith("data:")) { int comma = url.indexOf(','); src = object("type", "base64"); src.addProperty("media_type", url.substring(5, url.indexOf(';'))); src.addProperty("data", url.substring(comma+1)); }
                    else { src = object("type", "url"); src.addProperty("url", url); }
                    image.add("source", src);
                }
                result.add(image);
            } else if (part.has("text")) {
                JsonObject t = object("type", format == Format.RESPONSES ? (role.equals("assistant") ? "output_text" : "input_text") : "text");
                t.addProperty("text", text(part.get("text"))); result.add(t);
            }
        }
        return result;
    }
    /** Normalize to TLM's ChatCompletionResponse shape, including tool calls and token usage. */
    public static JsonObject decode(String response, Format format) throws IOException {
        final JsonObject json;
        try { json = JsonParser.parseString(response).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IOException("Vision provider returned invalid JSON."); }
        if (json.has("error") && !json.get("error").isJsonNull()) throw new IOException("Provider returned an API error. Check the model and endpoint.");
        if (format == Format.CHAT) {
            JsonArray choices = array(json, "choices");
            if (choices.isEmpty() || !choices.get(0).getAsJsonObject().has("message")) throw new IOException("Provider response has no message.");
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            message.addProperty("content", text(message.get("content")));
            return json;
        }
        JsonObject message = object("role", "assistant");
        StringBuilder content = new StringBuilder(); JsonArray calls = new JsonArray();
        JsonArray output = array(json, format == Format.RESPONSES ? "output" : "content");
        for (JsonElement entry : output) {
            JsonObject item = entry.getAsJsonObject(); String type = str(item, "type");
            if (type.equals("message")) content.append(text(item.get("content")));
            else if (type.equals("text")) content.append(text(item.get("text")));
            else if (type.equals("function_call") || type.equals("tool_use")) {
                JsonObject call = object("type", "function"), fn = object("name", str(item,"name"));
                call.addProperty("id", str(item, type.equals("tool_use") ? "id" : "call_id"));
                fn.addProperty("arguments", type.equals("tool_use") ? item.get("input").toString() : str(item,"arguments"));
                call.add("function", fn); calls.add(call);
            }
        }
        message.addProperty("content", content.toString());
        if (!calls.isEmpty()) message.add("tool_calls", calls);
        JsonObject choice = new JsonObject(); choice.add("message", message);
        choice.addProperty("finish_reason", !calls.isEmpty() ? "tool_calls" : (str(json,"status").equals("incomplete") || str(json,"stop_reason").equals("max_tokens") ? "length" : "stop"));
        JsonObject normalized = new JsonObject(); JsonArray choices = new JsonArray(); choices.add(choice); normalized.add("choices", choices);
        if (json.has("usage") && json.get("usage").isJsonObject()) {
            JsonObject usage = json.getAsJsonObject("usage"), mapped = new JsonObject();
            int in = number(usage,"input_tokens"), out = number(usage,"output_tokens");
            mapped.addProperty("prompt_tokens",in); mapped.addProperty("completion_tokens",out); mapped.addProperty("total_tokens", in+out);
            normalized.add("usage",mapped);
        }
        return normalized;
    }
    public static String observation(String response, Format format) throws IOException {
        JsonObject choice = decode(response, format).getAsJsonArray("choices").get(0).getAsJsonObject();
        String answer = text(choice.getAsJsonObject("message").get("content")).trim();
        if (answer.isEmpty()) throw new IOException(str(choice,"finish_reason").equals("length")
                ? "Vision model used its token budget before answering. Disable reasoning or increase the vision token limit."
                : "Vision model returned no visible description. Try a different vision model.");
        return answer;
    }
    public static String text(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            StringJoiner text = new StringJoiner("\n");
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonObject() && Set.of("text", "output_text", "input_text").contains(str(e.getAsJsonObject(),"type"))) text.add(text(e.getAsJsonObject().get("text")));
            }
            return text.toString();
        }
        return "";
    }
    public static IOException httpError(int code) {
        return new IOException("Vision provider HTTP " + code + ": " + switch(code) {
            case 400, 422 -> "request rejected; check model capabilities and endpoint.";
            case 401, 403 -> "check the API key and subscription access.";
            case 429 -> "provider rate or usage limit reached. Try again later.";
            case 502, 503, 504 -> "provider temporarily unavailable. Try again shortly.";
            default -> "request failed.";
        });
    }
    public static JsonObject object(String key, String value) { JsonObject o=new JsonObject(); o.addProperty(key,value); return o; }
    public static String str(JsonObject o, String key) { return o == null ? "" : text(o.get(key)); }
    public static JsonArray array(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray(); }
    private static int number(JsonObject o,String key) { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0; }
}
