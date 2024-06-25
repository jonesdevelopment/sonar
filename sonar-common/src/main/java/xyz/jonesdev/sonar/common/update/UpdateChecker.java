/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.common.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.logger.LoggerWrapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class UpdateChecker {
  private final ExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();

  private final LoggerWrapper LOGGER = new LoggerWrapper() {

    @Override
    public void info(final String message, final Object... args) {
      Sonar.get().getLogger().info("[update-checker] " + message, args);
    }

    @Override
    public void warn(final String message, final Object... args) {
      Sonar.get().getLogger().warn("[update-checker] " + message, args);
    }

    @Override
    public void error(final String message, final Object... args) {
      Sonar.get().getLogger().error("[update-checker] " + message, args);
    }
  };

  public void checkForUpdates() {
    ASYNC_EXECUTOR.execute(() -> {
      try {
        final URL url = new URL("https://api.github.com/repos/jonesdevelopment/sonar/releases/latest");
        final HttpsURLConnection urlConnection = prepareConnection(url);
        final JsonObject json = parseJson(urlConnection);
        final String latestStableRelease = json.get("tag_name").getAsString();
        final int convertedLatestVersion = convertVersion(latestStableRelease);
        final int convertedCurrentVersion = convertVersion(Sonar.get().getVersion().getVersion());

        if (convertedCurrentVersion < convertedLatestVersion) {
          LOGGER.warn("A new version of Sonar is available: {}", latestStableRelease);
          LOGGER.warn("Please make sure to update to the latest version to ensure stability and security:");
          LOGGER.warn("https://github.com/jonesdevelopment/sonar/releases/tag/{}", latestStableRelease);
        } else if (convertedCurrentVersion > convertedLatestVersion || !Sonar.get().getVersion().getGitBranch().equals("main")) {
          LOGGER.warn("You are currently using an unreleased version of Sonar!");
          LOGGER.warn("The contributors of Sonar are not responsible for any damage done by using an unstable version");
        } else {
          LOGGER.info("You are currently using the latest stable release of Sonar!");
        }
      } catch (Throwable throwable) {
        LOGGER.warn("Unable to retrieve version information: {}", throwable);
      }
    });
  }

  private @NotNull JsonObject parseJson(final @NotNull HttpsURLConnection urlConnection) throws IOException {
    try (final InputStream inputStream = urlConnection.getInputStream();
         final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
         final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      // Parse site content as JSON using Gson
      final JsonParser jsonParser = new JsonParser();
      return jsonParser.parse(bufferedReader).getAsJsonObject();
    }
  }

  private @NotNull HttpsURLConnection prepareConnection(final @NotNull URL url) throws Exception {
    final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    // Set connection timeout
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(15000);
    // Set necessary properties
    urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
    // Only continue if the connection is a success
    if (urlConnection.getResponseCode() != 200) {
      throw new IllegalStateException("Unexpected response code " + urlConnection.getResponseCode());
    }
    return urlConnection;
  }

  private int convertVersion(final @NotNull String version) throws NumberFormatException, IllegalStateException {
    final String[] convertedParts = version.split("\\.");
    // Validate the length of the converted parts
    if (convertedParts.length != 3) {
      throw new IllegalStateException("Converted version length mismatch");
    }
    // Major updates are more important than minor updates,
    // and minor updates are more important than patches.
    final int convertedMajor = Integer.parseInt(convertedParts[0]) * 100000; // multiply by weight
    final int convertedMinor = Integer.parseInt(convertedParts[1]) * 10000; // multiply by weight
    final int patch = Integer.parseInt(convertedParts[2]);
    return convertedMajor + convertedMinor + patch; // sum the converted version parts
  }
}
