package dev.rdh.pulse.spark.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.sampler.ThreadDumper.GameThread;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;

import dev.rdh.pulse.render.GlInfo;
import dev.rdh.pulse.spark.AbstractFabricSparkPlugin;

import me.lucko.spark.common.platform.world.WorldInfoProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.pack.ResourcePacks;

import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FabricClientSparkPlugin extends AbstractFabricSparkPlugin {
    private static final List<String> ALIASES = List.of("sparkc", "sparkclient");

    private static FabricClientSparkPlugin instance;

    private final SparkPlatform platform = new SparkPlatform(this);

    public FabricClientSparkPlugin() {
        super(new GameThread(), PlatformInfo.Type.CLIENT);
    }

    public static void initialize() {
        if (instance != null) return;
        instance = new FabricClientSparkPlugin();
        instance.platform.enable();
        MinecraftClientEvents.TICK_START.register(_ -> instance.gameThreadDumper.setThread(Thread.currentThread()));
        MinecraftClientEvents.STOP.register(_ -> instance.close());
    }

    public static boolean executeClientCommand(String message) {
        if (instance == null || !message.startsWith("/")) {
            return false;
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || !ALIASES.contains(parts[0])) {
            return false;
        }
        String[] arguments = new String[parts.length - 1];
        System.arraycopy(parts, 1, arguments, 0, arguments.length);
        instance.platform.executeCommand(sender(), arguments).exceptionally(exception -> {
            sender().sendMessage(Component.text("spark command failed: " + exception.getMessage()));
            return null;
        });
        return true;
    }

    public static String completeClientCommand(String message) {
        if (instance == null || !message.startsWith("/")) {
            return message;
        }
        String[] parts = message.substring(1).split("\\s+", -1);
        List<String> completions;
        if (parts.length == 1) {
            completions = ALIASES.stream().filter(command -> command.startsWith(parts[0])).collect(Collectors.toList());
        } else if (ALIASES.contains(parts[0])) {
            completions = instance.platform.tabCompleteCommand(sender(), Arrays.copyOfRange(parts, 1, parts.length));
        } else {
            return message;
        }
        if (completions.isEmpty()) {
            return message;
        }
        int start = message.lastIndexOf(' ') + 1;
        return message.substring(0, start) + completions.stream().reduce(FabricClientSparkPlugin::commonPrefix).orElse("");
    }

    private static String commonPrefix(String first, String second) {
        int length = 0;
        while (length < first.length() && length < second.length() && first.charAt(length) == second.charAt(length)) {
            length++;
        }
        return first.substring(0, length);
    }

    private void close() {
        this.platform.disable();
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.of(sender());
    }

    @Override
    public void executeSync(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    @Override
    public TickHook createTickHook() {
        return new FabricClientTickHook();
    }

    @Override
    public TickReporter createTickReporter() {
        return new FabricClientTickReporter();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FabricClientWorldInfoProvider();
    }

    @Override
    public MetadataProvider createExtraMetadataProvider() {
        return () -> {
            JsonArray packs = new JsonArray();
            for (ResourcePacks.Entry pack : Minecraft.getInstance().getResourcePacks().getApplied()) {
                packs.add(pack.getName());
            }
            JsonObject metadata = new JsonObject();
            metadata.add("resourcePacks", packs);
            GlInfo gl = GlInfo.get();
            if (gl != null) {
                JsonObject gpu = new JsonObject();
                gpu.addProperty("vendor", gl.vendor());
                gpu.addProperty("renderer", gl.renderer());
                gpu.addProperty("version", gl.version());
                metadata.add("gpu", gpu);
            }
            return Map.of("client", metadata);
        };
    }

    private static CommandSender sender() {
        return sender(Minecraft.getInstance().getSession().getProfile().getId());
    }

    static CommandSender sender(UUID uniqueId) {
        return new FabricClientCommandSender(uniqueId);
    }
}
