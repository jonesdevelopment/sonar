/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.api.dependencies;

import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

@UtilityClass
public class DependencyLoader {
  public ConnectionSource setUpDriverAndConnect() throws Throwable {
    final SonarConfiguration config = Sonar.get().getConfig();

    final URL[] urls = new URL[config.getDatabase().getType().getDependencies().length];
    for (final Dependency dependency : config.getDatabase().getType().getDependencies()) {
      final URL url = dependency.getClassLoaderURL();
      final ClassLoader currentClassLoader = DependencyLoader.class.getClassLoader();

      final Method addPath = currentClassLoader.getClass().getDeclaredMethod("addPath", Path.class);
      addPath.setAccessible(true);
      addPath.invoke(currentClassLoader, new File(url.toURI()).toPath());

      urls[dependency.ordinal()] = url;
    }

    final String type = config.getDatabase().getType().name().toLowerCase();
    final String databaseURL = String.format("jdbc:%s://%s:%d/%s",
      type, config.getDatabase().getUrl(), config.getDatabase().getPort(), config.getDatabase().getName());

    final ExternalClassLoader classLoader = new ExternalClassLoader(urls);
    final Connection connection = connect(classLoader, databaseURL, config.getDatabase());
    return new JdbcSingleConnectionSource(databaseURL, connection);
  }

  // Mostly taken from
  // https://github.com/Elytrium/LimboAuth/blob/master/src/main/java/net/elytrium/limboauth/dependencies/DatabaseLibrary.java#L134
  private Connection connect(final @NotNull ClassLoader classLoader,
                             final @NotNull String databaseURL,
                             final @NotNull SonarConfiguration.Database config) throws Throwable {
    final Class<?> driverClass = classLoader.loadClass(config.getType().getDriverClassName());
    final Object driver = driverClass.getDeclaredConstructor().newInstance();

    DriverManager.registerDriver((Driver) driver);

    final Properties properties = new Properties();
    properties.put("user", config.getUsername());
    if (!config.getPassword().isEmpty()) {
      properties.put("password", config.getPassword());
    }

    final Method connect = driverClass.getDeclaredMethod("connect", String.class, Properties.class);
    connect.setAccessible(true);
    return (Connection) connect.invoke(driver, databaseURL, properties);
  }
}
