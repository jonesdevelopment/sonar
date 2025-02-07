/*
 * Copyright (C) 2025 Sonar Contributors
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

package xyz.jonesdev.sonar.api.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;

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
  @Getter
  private CheckResult lastCheckResult = CheckResult.UNRESOLVED;

  public void checkForUpdates() {
    ASYNC_EXECUTOR.execute(() -> {
      try {
        final URL url = new URL("https://api.github.com/repos/jonesdevelopment/sonar/releases/latest");
        final HttpsURLConnection urlConnection = prepareConnection(url);
        final JsonObject json = parseJson(urlConnection);
        final String latestStableRelease = json.get("tag_name").getAsString();
        final int convertedLatestVersion = convertVersion(latestStableRelease);
        final int convertedCurrentVersion = convertVersion(Sonar.get0().getVersion().getVersion());

        if (convertedCurrentVersion < convertedLatestVersion) {
          Sonar.get0().getLogger().warn("A new version of Sonar is available: {}", latestStableRelease);
          Sonar.get0().getLogger().warn("Please make sure to update to the latest version to ensure stability and security:");
          Sonar.get0().getLogger().warn("https://github.com/jonesdevelopment/sonar/releases/latest");
          lastCheckResult = CheckResult.OUTDATED_VERSION;
        } else if (convertedCurrentVersion > convertedLatestVersion || !Sonar.get0().getVersion().getGitBranch().equals("main")) {
          Sonar.get0().getLogger().warn("You are currently using an unreleased version of Sonar!");
          Sonar.get0().getLogger().warn("The contributors of Sonar are not responsible for any damage done by using an unstable version");
          lastCheckResult = CheckResult.UNSTABLE_VERSION;
        } else if (lastCheckResult != CheckResult.LATEST_VERSION) {
          Sonar.get0().getLogger().info("You are currently using the latest stable release of Sonar!");
          lastCheckResult = CheckResult.LATEST_VERSION;
        }
      } catch (Throwable throwable) {
        Sonar.get0().getLogger().warn("Unable to retrieve version information: {}", throwable);
        lastCheckResult = CheckResult.API_ERROR;
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

  @Getter
  @RequiredArgsConstructor
  public enum CheckResult {
    API_ERROR("api-error"),
    OUTDATED_VERSION("outdated-version"),
    UNSTABLE_VERSION("unstable-version"),
    LATEST_VERSION(null),
    UNRESOLVED(null);

    private final String configKey;
  }
}
