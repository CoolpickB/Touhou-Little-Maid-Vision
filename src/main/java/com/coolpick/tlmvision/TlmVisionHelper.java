package com.coolpick.tlmvision;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;
import com.coolpick.tlmvision.network.MaidLookPayload;
import com.coolpick.tlmvision.network.MaidLookController;
import net.neoforged.neoforge.common.NeoForge;
@Mod(TlmVisionHelper.MOD_ID)
public class TlmVisionHelper {
    public static final String MOD_ID = "tlmvision";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<ThirdEyeItem> THIRD_EYE = ITEMS.register("third_eye", ThirdEyeItem::new);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VISION_TAB =
            CREATIVE_TABS.register("vision", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tlmvision"))
                    .icon(() -> THIRD_EYE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(THIRD_EYE.get()))
                    .build());
    public TlmVisionHelper(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        modBus.addListener(MaidLookPayload::register);
        modBus.addListener(com.coolpick.tlmvision.network.MaidSettingsPayload::register);
        NeoForge.EVENT_BUS.addListener(com.coolpick.tlmvision.network.MaidSettingsPayload::startTracking);
        NeoForge.EVENT_BUS.addListener(MaidLookController::tick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.coolpick.tlmvision.client.VisionKeys.init(modBus);
            container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (mod, parent) -> new com.coolpick.tlmvision.client.ModConfigScreen(parent));
        }
    }
}
