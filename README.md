# Touhou Little Maid: Vision

Third Eye lets you share what you see with your maids, and vice versa.

## What it does

- Equip a Third Eye in a maid's bauble slot.
- Press **]** to share your view with nearby maids wearing a Third Eye.
- Hold **[** to look through one maid's eyes. Move the mouse to turn her view. Use the mouse wheel to switch maids.
- Recommend changing the keybinds to something more comfortable. 

The mod makes one sight request, then shares its short description with up to three nearby maids wearing a Third Eye by default. Every maid in that group must be within the configured range of every other maid. You can change both values in the mod config screen; set the range to zero to share with only the nearest maid. Each maid then replies in character through normal chat.

## Getting started

Install this mod and Touhou Little Maid 1.5.3 or newer on both the client and server.

Craft a Third Eye at TLM's altar with 1 Eye of Ender, 1 Aya's Camera, 4 Redstone Dust, and 0.2 power. You can also use `/give @s tlmvision:third_eye`.

Open a maid's chat settings and choose a service that can read images. The Vision menu lets you choose a separate service for sight sharing, or use the maid's normal chat choice. Open the menu from the maid's chat settings screen.

Vision descriptions use up to 2,048 output tokens and wait up to 25 seconds. Sight pictures are resized before sending. If a service is slow or reaches its output limit, try again or choose a faster service.

## Other Notes

OpenCode Go also added as another default provider to TLM. I usually use Mimo V2.5 for images/chatting as it's cheap, though it's not as smart with Minecraft structures as something like GPT 5.6 Luna.

I considered adding an ambient vision, where the AI could take it's own screenshots to look at every once in a while or upon finding new biomes/structures. It'd probably be too expensive to keep on though, maybe I'll add it later as an experimental option.

## Settings

Most of the settings are in the mod config screen. **Show observation in chat** puts the short sight description in chat before the maids reply. Useful if you want to see exactly what they were told.

**Share with up to** changes how many Third Eye maids can react to one picture. It still only sends one picture request, but every maid gets their own reply afterwards, so a high number can get a little noisy.

**Maid group range** controls how close the maids need to be to each other to join the same share. Setting it to 0 just picks the nearest maid.

**Vision output limit** gives the image service more room to answer. Leave it at 2,048 unless you are getting cut-off answers. Higher can be slower and cost a little more.

**TLM chat screen improvements** adds a history scroll bar and a clearer context counter. Recommended to leave it on, shouldn't break anything. Probably.

## Building

Use Java 21. Put `touhoulittlemaid-1.5.3-neoforge+mc1.21.1.jar` in `libs/`, then run `.\gradlew.bat build` on Windows. The built mod is in `build/libs/`. The TLM JAR stays out of Git.
