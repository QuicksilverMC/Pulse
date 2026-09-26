# Pulse

Profiling and render instrumentation for Minecraft 1.8.9 (Ornithe / Fabric).

- **spark**, bundled: `/spark` on the server and in singleplayer, `/sparkc` for the client.
- **Readable profiles**: stack frames are remapped to Feather names before upload, since the spark viewer has no Feather mappings.
- **Tracy**: run with `-Dpulse.tracy=true` and connect a [Tracy](https://github.com/wolfpld/tracy) profiler. Every `Profiler` section in vanilla and in other mods becomes a zone, on both the client and server threads, plus a frame mark per frame. GPU zones are added on Windows and Linux, where the driver provides `ARB_timer_query`. To capture without the GUI, run `tracy-capture -a 127.0.0.1 -o session.tracy`. The client library is bundled; it must match your Tracy version (see `tracy_version` in `gradle.properties`).
- **Draw calls in F3**: per-frame GL draw calls, split into categories (terrain, sky, entities, tiles, particles, text, GUI, other). Counted at the LWJGL level, so draws from mods that bypass vanilla rendering are included.

Requires [lenis](https://github.com/notdevcody/lenis) or [legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) on the client, and Java 25.

