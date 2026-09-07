package com.coolpick.tlmvision.client;

import com.coolpick.tlmvision.TlmVisionHelper;
import com.coolpick.tlmvision.VisionConfig;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Only compiled/loaded by -PeyeSmoke; never included in the release jar. */
@EventBusSubscriber(modid = "tlmvision", value = Dist.CLIENT)
public final class EyeSmoke {
    private static boolean started;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (started || mc.screen == null || mc.getOverlay() != null) return;
        started = true;
        try {
            check(BaubleManager.getBauble(TlmVisionHelper.THIRD_EYE.toStack()) != null, "bauble binding");
            var model = mc.getModelManager().getModel(ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("tlmvision", "third_eye")));
            check(model != mc.getModelManager().getMissingModel(), "item model loaded");
            check(model.getParticleIcon().contents().width() == 16, "animated sprite width");
            check(model.getParticleIcon().contents().height() == 16, "animated sprite height");
            CompletableFuture.runAsync(() -> {
                try { networkChecks(); finish(mc, "PASS: bauble binding, item model, animated sprite, JPEG dimensions/colors, request format, response decoding, HTTP errors, and native image cleanup."); }
                catch (Throwable failure) { finish(mc, "FAIL: " + failure); }
            });
        } catch (Throwable failure) { finish(mc, "FAIL: " + failure); }
    }
    private static void networkChecks() throws Exception {
        ProtocolChecks.run();
        var endpointClass = Class.forName("com.coolpick.tlmvision.client.MaidCamera$VisionEndpoint");
        var endpointConstructor = endpointClass.getDeclaredConstructor(String.class, String.class, String.class, java.util.Map.class);
        endpointConstructor.setAccessible(true);
        var method = MaidCamera.class.getDeclaredMethod("askVisionModel", endpointClass, VisionConfig.class, String.class, NativeImage.class);
        method.setAccessible(true);
        var configConstructor = VisionConfig.class.getDeclaredConstructor();
        configConstructor.setAccessible(true);
        VisionConfig config = configConstructor.newInstance();
        config.maxWidth = 128;
        AtomicReference<Throwable> serverError = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger(200);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/vision", exchange -> {
            try {
                var body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                check("smoke-vision".equals(body.get("model").getAsString()), "model forwarded");
                check("Bearer fake-local-key".equals(exchange.getRequestHeaders().getFirst("Authorization")), "auth header");
                var parts = body.getAsJsonArray("messages").get(1).getAsJsonObject().getAsJsonArray("content");
                String data = parts.get(1).getAsJsonObject().getAsJsonObject("image_url").get("url").getAsString();
                check(data.startsWith("data:image/jpeg;base64,"), "JPEG data URL");
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1))));
                check(decoded.getWidth() == 128 && decoded.getHeight() == 64, "aspect-preserving downscale");
                int red = decoded.getRGB(16, 32), blue = decoded.getRGB(112, 32);
                check(((red >> 16) & 255) > 220 && (red & 255) < 30, "red channel");
                check((blue & 255) > 220 && ((blue >> 16) & 255) < 30, "blue channel");
                byte[] response = (status.get() == 200 ? "{\"choices\":[{\"message\":{\"content\":\"A red wall beside a blue wall.\"}}]}" : "provider-error").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status.get(), response.length);
                exchange.getResponseBody().write(response);
            } catch (Throwable failure) { serverError.set(failure); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            Object endpoint = endpointConstructor.newInstance("http://127.0.0.1:" + server.getAddress().getPort() + "/vision", "fake-local-key", "smoke-vision", java.util.Map.of());
            NativeImage image = sample();
            Object answer = method.invoke(null, endpoint, config, "test context", image);
            check("A red wall beside a blue wall.".equals(answer), "response decoded");
            checkClosed(image);
            status.set(400);
            image = sample();
            try { method.invoke(null, endpoint, config, "test context", image); throw new AssertionError("HTTP 400 accepted"); }
            catch (InvocationTargetException error) { check(error.getCause() instanceof IOException, "HTTP error surfaced"); }
            checkClosed(image);
            if (serverError.get() != null) throw new AssertionError("Fake provider assertion", serverError.get());
        } finally { server.stop(0); }
    }
    private static NativeImage sample() {
        NativeImage image = new NativeImage(256, 128, false);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 256; x++) image.setPixelRGBA(x, y, x < 128 ? 0xFF0000FF : 0xFFFF0000);
        return image;
    }
    private static void checkClosed(NativeImage image) {
        try { image.getPixelRGBA(0, 0); throw new AssertionError("image not closed"); }
        catch (IllegalStateException expected) { }
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    private static void finish(Minecraft mc, String result) {
        TlmVisionHelper.LOGGER.info("[eye-smoke] {}", result);
        try { Files.writeString(mc.gameDirectory.toPath().resolve("../build/eye-smoke-result.txt"), result); }
        catch (IOException error) { TlmVisionHelper.LOGGER.error("Could not save smoke result", error); }
        mc.execute(mc::stop);
    }
}
