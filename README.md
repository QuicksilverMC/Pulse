# Pulse

Profiling and render instrumentation for Minecraft 1.8.9 (Ornithe / Fabric).

- **spark**, bundled: `/spark` on the server and in singleplayer, `/sparkc` for the client.
- **Readable profiles**: stack frames are remapped to Feather names before upload, since the spark viewer has no Feather mappings.
- **Draw calls in F3**: per-frame GL draw calls, split into categories (terrain, sky, entities, tiles, particles, text, GUI, other). Counted at the LWJGL level, so draws from mods that bypass vanilla rendering are included.

Requires [lenis](https://github.com/notdevcody/lenis) or [legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) on the client, and Java 25.

