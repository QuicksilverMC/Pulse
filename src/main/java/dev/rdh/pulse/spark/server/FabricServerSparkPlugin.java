package dev.rdh.pulse.spark.server;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;

import dev.rdh.pulse.spark.AbstractFabricSparkPlugin;
import dev.rdh.pulse.spark.mixin.MinecraftServerAccessor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.AbstractCommand;
import net.minecraft.server.command.handler.CommandRegistry;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.stream.Stream;

public final class FabricServerSparkPlugin extends AbstractFabricSparkPlugin {
    private static FabricServerSparkPlugin instance;

    private final MinecraftServer server;
    private final SparkPlatform platform;

    private FabricServerSparkPlugin(MinecraftServer server) {
        super(new ThreadDumper.GameThread(((MinecraftServerAccessor) server)::spark$getThread), PlatformInfo.Type.SERVER);
        this.server = server;
        this.platform = new SparkPlatform(this);
    }

    public static void initialize(MinecraftServer server) {
        if (instance != null) return;
        instance = new FabricServerSparkPlugin(server);
        instance.platform.enable();
        ((CommandRegistry) server.getCommandHandler()).register(new SparkCommand(instance));
    }

    public static void stop(MinecraftServer server) {
        if (instance != null && instance.server == server) {
            instance.platform.disable();
            instance = null;
        }
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return this.server.getPlayerManager().getAll().stream().map(FabricServerSparkPlugin::sender);
    }

    @Override
    public void executeSync(Runnable task) {
        this.server.execute(task);
    }

    @Override
    public TickHook createTickHook() {
        return new FabricServerTickHook();
    }

    @Override
    public TickReporter createTickReporter() {
        return new FabricServerTickReporter();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FabricServerWorldInfoProvider(this.server);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new FabricPlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FabricServerConfigProvider();
    }

    static CommandSender sender(CommandSource source) {
        return new FabricServerCommandSender(source);
    }

    private static final class SparkCommand extends AbstractCommand {
        private final FabricServerSparkPlugin plugin;

        private SparkCommand(FabricServerSparkPlugin plugin) {
            this.plugin = plugin;
        }

        @Override public String getName() { return "spark"; }
        @Override public boolean canUse(CommandSource source) { return FabricServerCommandSender.isHost(source) || super.canUse(source); }
        @Override public String getUsage(CommandSource source) { return "/spark <command>"; }
        @Override public void run(CommandSource source, String[] arguments) { this.plugin.platform.executeCommand(sender(source), arguments); }
        @Override public List<String> getSuggestions(CommandSource source, String[] arguments, BlockPos position) { return this.plugin.platform.tabCompleteCommand(sender(source), arguments); }
    }
}
