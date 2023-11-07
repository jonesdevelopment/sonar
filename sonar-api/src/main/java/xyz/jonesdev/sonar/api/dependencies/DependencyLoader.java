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
import xyz.jonesdev.sonar.api.SonarPlatform;
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
    final SonarConfiguration.Database database = Sonar.get().getConfig().getDatabase();

    final ClassLoader currentClassLoader = DependencyLoader.class.getClassLoader();
    ClassLoader classLoader = currentClassLoader;

    // Velocity doesn't include the JDBC driver, which is why we need to load it manually
    if (Sonar.get().getPlatform() == SonarPlatform.VELOCITY) {
      final URL url = database.getType().getDependency().getClassLoaderURL();

      final Method addPath = currentClassLoader.getClass().getDeclaredMethod("addPath", Path.class);
      addPath.setAccessible(true);
      addPath.invoke(currentClassLoader, new File(url.toURI()).toPath());

      classLoader = new ExternalClassLoader(new URL[]{url});
    }

    final String jdbcURL = String.format("jdbc:%s://%s:%d/%s",
      database.getType().name().toLowerCase(), database.getUrl(), database.getPort(), database.getName());

    final Connection connection = connect(classLoader, jdbcURL, database);
    return new JdbcSingleConnectionSource(jdbcURL, connection);
  }

  // Mostly taken from
  // https://github.com/Elytrium/LimboAuth/blob/master/src/main/java/net/elytrium/limboauth/dependencies/DatabaseLibrary.java#L137
  private Connection connect(final @NotNull ClassLoader classLoader,
                             final @NotNull String databaseURL,
                             final @NotNull SonarConfiguration.Database database) throws Throwable {
    final Class<?> driverClass = classLoader.loadClass(database.getType().getDriverClassName());
    final Object driver = driverClass.getDeclaredConstructor().newInstance();

    DriverManager.registerDriver((Driver) driver);

    final Properties properties = new Properties();
    properties.put("user", database.getUsername());
    properties.put("password", database.getPassword());

    final Method connect = driverClass.getDeclaredMethod("connect", String.class, Properties.class);
    connect.setAccessible(true);
    return (Connection) connect.invoke(driver, databaseURL, properties);
  }
}
