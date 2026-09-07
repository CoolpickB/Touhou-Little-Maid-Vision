package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.compat.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import static com.coolpick.tlmvision.compat.ProviderProtocol.*;

final class ProtocolChecks {
    /** The three grouping cases the rule was written from. */
    private static void groupChecks(){
        java.util.function.Function<Vec3,Vec3> self=v->v;
        Vec3 player=Vec3.ZERO;
        check(MaidCamera.group(java.util.List.of(new Vec3(10,0,0),new Vec3(12,0,0),new Vec3(40,0,0)),self,player,3,16)
                .size()==2,"a maid 28 blocks from the pair stays out");
        check(MaidCamera.group(java.util.List.of(new Vec3(1,0,0),new Vec3(2,0,0),new Vec3(3,0,0),new Vec3(4,0,0)),self,player,3,16)
                .equals(java.util.List.of(new Vec3(1,0,0),new Vec3(2,0,0),new Vec3(3,0,0))),"the cap drops the furthest maid");
        check(MaidCamera.group(java.util.List.of(new Vec3(20,0,0),new Vec3(30,0,0),new Vec3(60,0,0)),self,player,3,16)
                .equals(java.util.List.of(new Vec3(20,0,0),new Vec3(30,0,0))),"a chain cannot reach past the range");
        check(MaidCamera.group(java.util.List.of(new Vec3(0,0,0),new Vec3(10,0,0),new Vec3(20,0,0)),self,player,3,16)
                .size()==2,"every member is inside range of every other, not just the anchor");
    }
    static void run() throws Exception {
        groupChecks();
        check(observation("{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"A tree.\"}]}}]}",Format.CHAT).equals("A tree."), "array content");
        fails("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":null,\"reasoning_content\":\"thinking\"}}]}","token budget");
        fails("{\"choices\":[{\"message\":{\"content\":null}}]}","no visible description");
        fails("{\"choices\":[]}","no message"); fails("not json","invalid JSON");
        String responses="{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\",\"summary\":[]},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"A tree.\"}]}],\"usage\":{\"input_tokens\":10,\"output_tokens\":4}}";
        check(observation(responses,Format.RESPONSES).equals("A tree."),"Responses text");
        check(decode(responses,Format.RESPONSES).getAsJsonObject("usage").get("total_tokens").getAsInt()==14,"usage mapping");
        check(observation("{\"content\":[{\"type\":\"thinking\",\"thinking\":\"private\"},{\"type\":\"text\",\"text\":\"A tree.\"}]}",Format.MESSAGES).equals("A tree."),"Messages text only");
        JsonObject chat=JsonParser.parseString("""
          {"model":"mimo-v2.5","max_tokens":400,"response_format":{"type":"text"},"messages":[
            {"role":"system","content":"Be a maid."},
            {"role":"user","content":[{"type":"text","text":"Look"},{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,AAAA"}}]},
            {"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"wave","arguments":"{}"}}]},
            {"role":"tool","tool_call_id":"call_1","content":"done"},
            {"role":"user","content":"Thanks."}
          ],"tools":[{"type":"function","function":{"name":"wave","description":"Wave","parameters":{"type":"object","properties":{}}}}]}
          """).getAsJsonObject();
        JsonObject responseRequest=encode(chat,Format.RESPONSES);
        check(!responseRequest.has("messages")&&!responseRequest.has("response_format"),"Responses excludes chat-only fields");
        check(responseRequest.getAsJsonArray("input").get(2).getAsJsonObject().get("call_id").getAsString().equals("call_1"),"Responses tool call ID");
        check(responseRequest.getAsJsonArray("input").get(3).getAsJsonObject().get("type").getAsString().equals("function_call_output"),"Responses tool output");
        check(responseRequest.getAsJsonArray("input").get(1).getAsJsonObject().getAsJsonArray("content").get(1).getAsJsonObject().has("image_url"),"Responses image");
        JsonObject messagesRequest=encode(chat,Format.MESSAGES);
        check(messagesRequest.has("system")&&messagesRequest.getAsJsonArray("messages").size()==3,"Messages system and consecutive roles");
        check(messagesRequest.getAsJsonArray("messages").get(0).getAsJsonObject().getAsJsonArray("content").get(1).getAsJsonObject().getAsJsonObject("source").get("media_type").getAsString().equals("image/jpeg"),"Messages image source");
        check(messagesRequest.getAsJsonArray("tools").get(0).getAsJsonObject().has("input_schema"),"Messages tool schema");
        check(endpoint(GO_BASE+"/responses","mimo-v2.5").endsWith("/chat/completions"),"legacy Go endpoint corrected");
        check(endpoint(GO_BASE,"gpt-5.6-luna").endsWith("/responses"),"Go Responses routing");
        check(endpoint(GO_BASE,"minimax-m3").endsWith("/messages"),"Go Messages routing");
        var request=request(GO_BASE,"fake-key",Map.of("x-custom","kept"),chat,Duration.ofSeconds(1));
        check(request.headers().firstValue("x-opencode-session").isPresent(),"Go session header");
        check(request.headers().firstValue("User-Agent").orElse("").startsWith("TLMVision/"),"honest user agent");
        check(request.headers().firstValue("x-custom").orElse("").equals("kept"),"custom headers");
        JsonObject tool=decode("{\"output\":[{\"type\":\"function_call\",\"call_id\":\"c1\",\"name\":\"wave\",\"arguments\":\"{}\"}]}",Format.RESPONSES);
        check(tool.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").getAsJsonArray("tool_calls").get(0).getAsJsonObject().get("id").getAsString().equals("c1"),"response tool preserved");
        tool=decode("{\"content\":[{\"type\":\"tool_use\",\"id\":\"c2\",\"name\":\"wave\",\"input\":{}}]}",Format.MESSAGES);
        check(tool.getAsJsonArray("choices").get(0).getAsJsonObject().get("finish_reason").getAsString().equals("tool_calls"),"Messages tool preserved");
        var site=new OpenCodeGoSite(); site.setEnabled(true); site.setSecretKey("fake-key");
        var codec=new OpenCodeGoSite.Serializer().codec();
        var decoded=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,site).getOrThrow()).getOrThrow();
        check(decoded.enabled()&&decoded.secretKey().equals("fake-key")&&decoded.client() instanceof OpenCodeGoClient,"provider persistence and client");
    }
    private static void fails(String json,String text) throws Exception {
        try{observation(json,Format.CHAT);throw new AssertionError("bad response accepted");}
        catch(IOException e){check(e.getMessage().contains(text),"actionable error: "+text);}
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
