# Touhou Little Maid: Vision

Third Eye lets you share what you see with your maids.

## What it does

- Equip a Third Eye in a maid's bauble slot.
- Press **]** to share your view with nearby maids wearing a Third Eye.
- Hold **[** to look through one maid's eyes. Move the mouse to turn her view. Use the mouse wheel to switch maids.
- Recommend changing the keybinds to something more comfortable. 

The maid receives a short description of the view, then replies in character through normal chat.

## Getting started

Install this mod and Touhou Little Maid 1.5.3 or newer on both the client and server.

Craft a Third Eye at TLM's altar with 1 Eye of Ender, 1 Aya's Camera, 4 Redstone Dust, and 0.2 power. You can also use `/give @s tlmvision:third_eye`.

Open a maid's chat settings and choose a service that can read images. The Vision menu lets you choose a separate service for sight sharing, or use the maid's normal chat choice. Open the menu from the maid's chat settings screen.

Vision descriptions use up to 1,000 output tokens. If a service spends too long thinking instead of answering, choose a faster service or turn off its thinking mode.

## Other Notes

OpenCode Go also added as another default provider to TLM. I usually use Mimo V2.5 for images/chatting as it's cheap, though it's not as smart with Minecraft structures as something like GPT 5.6 Luna.

I considered adding an ambient vision, where the AI could take it's own screenshots to look at every once in a while or upon finding new biomes/structures. It'd probably be too expensive to keep on though, maybe I'll add it later as an experimental option.

## Settings

The Vision menu has a switch called **Show observation in chat**. It shows the raw sight description in your chat before the maid replies.

The mod's config screen has optional chat screen improvements: a history scroll bar and a clearer context counter. Recommended to turn it on, shouldn't break anything. Probably.

## Building

Use Java 21. Put `touhoulittlemaid-1.5.3-neoforge+mc1.21.1.jar` in `libs/`, then run `.\gradlew.bat build` on Windows. The built mod is in `build/libs/`. The TLM JAR stays out of Git.
