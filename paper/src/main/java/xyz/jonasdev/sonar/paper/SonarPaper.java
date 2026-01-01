package xyz.jonasdev.sonar.paper;

import com.alessiodp.libby.PaperLibraryManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonasdev.sonar.paper.command.PaperSonarCommand;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;
import xyz.jonesdev.sonar.bukkit.SonarBukkit;
import xyz.jonesdev.sonar.bukkit.antibot.BukkitInjector;
import xyz.jonesdev.sonar.bukkit.listener.BukkitJoinListener;
import xyz.jonesdev.sonar.common.boot.SonarBootstrap;

import java.util.UUID;

@Getter
public final class SonarPaper extends SonarBootstrap<SonarPaperPlugin> {
  private final LoggerWrapper logger = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      getPlugin().getLogger().info(buildFullMessage(message, args));
    }

    @Override
    public void warn(final String message, final Object... args) {
      getPlugin().getLogger().warning(buildFullMessage(message, args));
    }

    @Override
    public void error(final String message, final Object... args) {
      getPlugin().getLogger().severe(buildFullMessage(message, args));
    }
  };
  private Metrics metrics;

  public SonarPaper(final @NotNull SonarPaperPlugin plugin) {
    super(plugin, SonarPlatform.PAPER, plugin.getDataFolder(), new PaperLibraryManager(plugin));
  }

  @Override
  public @Nullable Audience audience(@Nullable final UUID uniqueId) {
    if (uniqueId == null) return null;
    return Bukkit.getPlayer(uniqueId);
  }

  @Override
  public @NotNull Audience sender(@NotNull final Object object) {
    return (Audience) object;
  }

  @Override
  public @NotNull LoggerWrapper getLogger() {
    return logger;
  }

  @Override
  public void enable() {
    // Initialize bStats.org metrics
    metrics = new Metrics(getPlugin(), getPlatform().getMetricsId());

    // Add charts for some configuration options
    metrics.addCustomChart(new SimplePie("verification",
      () -> getConfig().getVerification().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("captcha",
      () -> getConfig().getVerification().getMap().getTiming().getDisplayName()));
    metrics.addCustomChart(new SimplePie("language",
      () -> getConfig().getLanguage().getName()));
    metrics.addCustomChart(new SimplePie("database_type",
      () -> getConfig().getDatabase().getType().getDisplayName()));

    // Register Sonar command
    getPlugin().getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
      event.registrar().register("sonar", new PaperSonarCommand()));

    // Try to inject into the server
    if (BukkitInjector.isLateBindEnabled()) {
      getPlugin().getServer().getScheduler().runTask(getPlugin(), BukkitInjector::inject);
    } else {
      getPlugin().getServer().getPluginManager().registerEvents(new BukkitJoinListener(), getPlugin());
    }

    // Let the injector know that the plugin has been enabled
    SonarBukkit.INITIALIZE_LISTENER.complete(null);
  }

  @Override
  public void disable() {
    // Make sure to properly stop the metrics
    if (metrics != null) {
      metrics.shutdown();
    }
  }
}
