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

package xyz.jonesdev.sonar.api.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.config.SonarConfiguration;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

@RequiredArgsConstructor
public final class DiscordWebhook {
  private final String url;

  private static final Gson GSON = new GsonBuilder().create();
  private static final ExecutorService HTTP_REQUEST_SERVICE = Executors.newSingleThreadExecutor();

  /**
   * Asynchronously posts a Discord webhook via HTTPS
   */
  public void post(final Supplier<SonarConfiguration.Webhook.Embed> embed) {
    HTTP_REQUEST_SERVICE.execute(() -> {
      try {
        final String serializedContent = prepareSerializedContent(embed.get());
        final HttpsURLConnection urlConnection = prepareConnection(new URL(url), serializedContent);

        try (final OutputStream outputStream = urlConnection.getOutputStream()) {
          outputStream.write(serializedContent.getBytes(StandardCharsets.UTF_8));
        }

        // Discord replies with HTTP_NO_CONTENT (204) if the request is successful
        final int code = urlConnection.getResponseCode();
        if (code != HTTP_NO_CONTENT) {
          Sonar.get0().getLogger().warn("Unexpected Discord webhook response code {}", code);
        }
      } catch (Exception exception) {
        Sonar.get0().getLogger().error("Could not send webhook: {}", exception);
      }
    });
  }

  private static String prepareSerializedContent(final SonarConfiguration.Webhook.@NotNull Embed embed) {
    final String content = Sonar.get0().getConfig().getWebhook().getContent();
    final String username = Sonar.get0().getConfig().getWebhook().getUsername();
    final String avatarUrl = Sonar.get0().getConfig().getWebhook().getAvatarUrl();

    int rgb = embed.getR();
    rgb = (rgb << 8) + embed.getG();
    rgb = (rgb << 8) + embed.getB();

    final Webhook.EmbedMessage embedMessage = new Webhook.EmbedMessage(
      embed.getTitle(), embed.getDescription(), embed.getTitleUrl(), rgb, new Webhook.Field[0],
      new Webhook.Footer(Sonar.get0().getConfig().getWebhook().getFooter().getText(),
        Sonar.get0().getConfig().getWebhook().getFooter().getIconUrl()));
    final List<Webhook.EmbedMessage> embeds = Collections.singletonList(embedMessage);

    final Webhook webhook = new Webhook(content, username, avatarUrl, false, embeds);
    return GSON.toJson(webhook);
  }

  private @NotNull HttpsURLConnection prepareConnection(final @NotNull URL url,
                                                        final @NotNull String serializedContent) throws Exception {
    final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
    // Set connection timeout
    urlConnection.setConnectTimeout(15000);
    // Set necessary properties
    urlConnection.setRequestProperty("Content-Type", "application/json");
    urlConnection.setRequestProperty("Content-Length", String.valueOf(serializedContent.length()));
    urlConnection.setRequestProperty("User-Agent", "Sonar");
    urlConnection.setDoOutput(true);
    urlConnection.setRequestMethod("POST");
    return urlConnection;
  }

  @SuppressWarnings("unused")
  @RequiredArgsConstructor
  static final class Webhook {
    private final String content, username, avatar_url;
    private final boolean tts;
    private final List<EmbedMessage> embeds;

    @RequiredArgsConstructor
    static final class EmbedMessage {
      private final String title;
      private final String description;
      private final String url;
      private final int color;
      private final Field[] fields;
      private final Footer footer;
    }

    @RequiredArgsConstructor
    static final class Field {
      private final String name;
      private final String value;
      private final boolean inline;
    }

    @RequiredArgsConstructor
    static final class Footer {
      private final String text;
      private final String icon_url;
    }
  }
}
