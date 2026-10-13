/*
 * Copyright (C) 2026 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.jonesdev.sonar.bungee;

import com.alessiodp.libby.BungeeLibraryManager;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.common.boot.LibraryLoader;

@Getter
public final class SonarBungeePlugin extends Plugin {
  private ProxyServer server;
  private SonarBungee bootstrap;

  @Override
  public void onEnable() {
    server = ProxyServer.getInstance();

    // HACK:
    // Adventure is not bundled in the jar, so the libraries have to be on the classpath
    // before SonarBungee is loaded. The JVM resolves its Adventure types during class verification.
    final BungeeLibraryManager libraryManager = new BungeeLibraryManager(this);
    LibraryLoader.loadLibraries(libraryManager, SonarPlatform.BUNGEE);

    bootstrap = new SonarBungee(this, libraryManager);
    bootstrap.initialize();
  }

  @Override
  public void onDisable() {
    bootstrap.shutdown();
  }
}
