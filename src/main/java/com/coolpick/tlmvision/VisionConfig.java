package com.coolpick.tlmvision;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/tlmvision.json}: which model answers the maid-eye
 * screenshots. Blank {@code pinnedSite} means "None": fall back to whatever
 * model the maid is currently chatting with, and just try it (works when that
 * model supports vision, errors to chat otherwise).
 */
public final class VisionConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String LEGACY_DEFAULT_PROMPT = "Describe what is visible in one or two short sentences, using Minecraft terms. If nothing interesting is visible, say so briefly.";
    private static final String DEFAULT_PROMPT = "Describe the Minecraft scene accurately and concisely.";

    public String pinnedSite = "";
    public String pinnedModel = "";
    /** Additional direction for the vision model; the required three-line format stays in the system prompt. */
    public String prompt = DEFAULT_PROMPT;
    public int maxWidth = 768;
    public int maxTokens = 1000;
    /** How many maids one press can reach. */
    public int maxMaids = 3;
    /** A maid joins the group only while she is this close to every maid already in it. */
    public double groupRange = 16;
    @SerializedName(value = "showObservation", alternate = "debugChat")
    public boolean showObservation = false;
    @SerializedName(value = "chatImprovements", alternate = "aiFixes")
    public boolean chatImprovements = false;
    private VisionConfig() {
    }

    /**
     * Screen render and mouse hooks ask for this every frame, so it is read once per screen
     * open rather than parsed from disk in the render loop.
     */
    private static boolean cachedChatImprovements;
    public static boolean chatImprovements() { return cachedChatImprovements; }
    public static void refreshChatImprovements() { cachedChatImprovements = load().chatImprovements; }

    public boolean hasPinned() {
        return pinnedSite != null && !pinnedSite.isBlank() && pinnedModel != null && !pinnedModel.isBlank();
    }

    public static VisionConfig load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("tlmvision.json");
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                VisionConfig loaded = GSON.fromJson(reader, VisionConfig.class);
                if (loaded != null) {
                    // Upgrade only the old built-in value. User-written prompts are left untouched.
                    if (LEGACY_DEFAULT_PROMPT.equals(loaded.prompt)) {
                        loaded.prompt = DEFAULT_PROMPT;
                        loaded.save();
                    }
                    loaded.maxWidth = Math.clamp(loaded.maxWidth, 128, 1920);
                    loaded.maxTokens = Math.clamp(loaded.maxTokens, 64, 1000);
                    loaded.maxMaids = Math.clamp(loaded.maxMaids, 1, 16);
                    loaded.groupRange = Math.clamp(loaded.groupRange, 1, 256);
                    return loaded;
                }
            } catch (IOException | com.google.gson.JsonParseException e) {
                LOGGER.warn("[tlmvision] Failed to read config, using defaults", e);
            }
        }
        VisionConfig fresh = new VisionConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("tlmvision.json");
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(this, writer);
        } catch (IOException | com.google.gson.JsonParseException e) {
            LOGGER.warn("[tlmvision] Failed to write config", e);
        }
    }
}
