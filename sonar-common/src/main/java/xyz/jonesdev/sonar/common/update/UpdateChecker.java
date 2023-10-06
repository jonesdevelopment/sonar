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
        final JsonObject json = parseJson(urlConnection.getInputStream());
        final String latestStableVersion = json.get("tag_name").getAsString();

        if (!latestStableVersion.equals(Sonar.get().getVersion().getSemanticVersion())) {
          LOGGER.warn("A new version of Sonar is available: {}", latestStableVersion);
          LOGGER.warn("Please make sure to update to the latest version to ensure stability and security:");
          LOGGER.warn("https://github.com/jonesdevelopment/sonar/releases");
        } else {
          LOGGER.info("You are currently using the latest stable release of Sonar!");
        }
      } catch (Exception exception) {
        LOGGER.warn("Could not retrieve latest version information: {}", exception);
      }
    });
  }

  private @NotNull JsonObject parseJson(final @NotNull InputStream inputStream) throws IOException {
    final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    final StringBuilder content = new StringBuilder();

    // Read the entire page content
    try (final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        content.append(line);
      }
    }

    // Parse site content as JSON using Gson
    final JsonParser jsonParser = new JsonParser();
    return jsonParser.parse(content.toString()).getAsJsonObject();
  }

  private @NotNull HttpsURLConnection prepareConnection(final @NotNull URL url) throws Exception {
    final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    // Set connection timeout
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(15000);
    // Set necessary properties
    urlConnection.setRequestMethod("GET");
    urlConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
    // Only continue if the connection is a success
    if (urlConnection.getResponseCode() != 200) {
      throw new IllegalStateException("Unexpected response code " + urlConnection.getResponseCode());
    }
    return urlConnection;
  }
}
