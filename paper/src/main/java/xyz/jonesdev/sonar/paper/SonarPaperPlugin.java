package xyz.jonesdev.sonar.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.jonesdev.sonar.bukkit.antibot.BukkitInjector;

public final class SonarPaperPlugin extends JavaPlugin {
  private SonarPaper bootstrap;

  @Override
  public void onLoad() {
    // Inject early if late-bind is disabled
    if (!BukkitInjector.isLateBindEnabled()) {
      BukkitInjector.inject();
    }
  }

  @Override
  public void onEnable() {
    bootstrap = new SonarPaper(this);
    bootstrap.initialize();
  }

  @Override
  public void onDisable() {
    bootstrap.shutdown();
  }
}
