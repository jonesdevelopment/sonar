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

package jones.sonar.common.fallback.dimension;

import lombok.Getter;
import lombok.ToString;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag.Builder;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.stream.Collectors;

// https://github.com/Elytrium/LimboAPI/blob/master/plugin/src/main/java/net/elytrium/limboapi/material/Biome.java
@Getter
public enum Biome {
  PLAINS(
    BuiltInBiome.PLAINS,
    "minecraft:plains",
    1,
    new Element(
      true, 0.125F, 0.8F, 0.05F, 0.4F, "plains",
      Effects.builder(7907327, 329011, 12638463, 415920)
        .moodSound(Effects.MoodSound.of(6000, 2.0, 8, "minecraft:ambient.cave"))
        .build()
    )
  ),
  SWAMP(
    BuiltInBiome.SWAMP,
    "minecraft:swamp",
    6,
    new Element(
      true, -0.2F, 0.8F, 0.1F, 0.9F, "swamp",
      Effects.builder(7907327, 329011, 12638463, 415920)
        .grassColorModifier("swamp")
        .foliageColor(6975545)
        .moodSound(Effects.MoodSound.of(6000, 2.0, 8, "minecraft:ambient.cave"))
        .build()
    )
  ),
  SWAMP_HILLS(
    BuiltInBiome.SWAMP_HILLS,
    "minecraft:swamp_hills",
    134,
    new Element(
      true, -0.1F, 0.8F, 0.3F, 0.9F, "swamp",
      Effects.builder(7907327, 329011, 12638463, 415920)
        .grassColorModifier("swamp")
        .foliageColor(6975545)
        .moodSound(Effects.MoodSound.of(6000, 2.0, 8, "minecraft:ambient.cave"))
        .build()
    )
  ),
  NETHER_WASTES(
    BuiltInBiome.NETHER_WASTES,
    "minecraft:nether_wastes",
    8,
    new Element(false, 0.1f, 2.0f, 0.2f, 0.0f, "nether",
      Effects.builder(7254527, 329011, 3344392, 4159204)
        .moodSound(Effects.MoodSound.of(6000, 2.0, 8, "minecraft:ambient.nether_wastes.mood"))
        .build()
    )
  ),
  THE_END(
    BuiltInBiome.THE_END,
    "minecraft:the_end",
    9,
    new Element(false, 0.1f, 0.5f, 0.2f, 0.5f, "the_end",
      Effects.builder(0, 10518688, 12638463, 4159204)
        .moodSound(Effects.MoodSound.of(6000, 2.0, 8, "minecraft:ambient.cave"))
        .build()
    )
  );

  public enum BuiltInBiome {
    PLAINS,
    SWAMP,
    SWAMP_HILLS,
    NETHER_WASTES,
    THE_END
  }

  private static final EnumMap<BuiltInBiome, Biome> BUILT_IN_BIOME_MAP = new EnumMap<>(BuiltInBiome.class);

  private final BuiltInBiome index;
  private final String name;
  private final int id;
  private final Element element;

  Biome(BuiltInBiome index, String name, int id, Element element) {
    this.index = index;
    this.name = name;
    this.id = id;
    this.element = element;
  }

  public CompoundBinaryTag encodeBiome(int version) {
    return CompoundBinaryTag.builder()
      .putString("name", this.name)
      .putInt("id", this.id)
      .put("element", this.element.encode(version))
      .build();
  }

  static {
    for (Biome biome : Biome.values()) {
      BUILT_IN_BIOME_MAP.put(biome.index, biome);
    }
  }

  public static Biome of(BuiltInBiome index) {
    return BUILT_IN_BIOME_MAP.get(index);
  }

  public static CompoundBinaryTag getRegistry(int version) {
    return CompoundBinaryTag.builder()
      .putString("type", "minecraft:worldgen/biome")
      .put("value",
        ListBinaryTag.from(Arrays.stream(Biome.values()).map(biome -> biome.encodeBiome(version)).collect(Collectors.toList())))
      .build();
  }

  @ToString
  public static class Element {
    public final boolean hasPrecipitation;
    public final float depth;
    public final float temperature;
    public final float scale;
    public final float downfall;
    public final String category;
    public final Effects effects;

    public Element(boolean hasPrecipitation, float depth, float temperature, float scale, float downfall,
                   String category, Effects effects) {
      this.hasPrecipitation = hasPrecipitation;
      this.depth = depth;
      this.temperature = temperature;
      this.scale = scale;
      this.downfall = downfall;
      this.category = category;
      this.effects = effects;
    }

    public CompoundBinaryTag encode(int version) {
      CompoundBinaryTag.Builder tagBuilder = CompoundBinaryTag.builder()
        .putFloat("depth", this.depth)
        .putFloat("temperature", this.temperature)
        .putFloat("scale", this.scale)
        .putFloat("downfall", this.downfall)
        .putString("category", this.category)
        .put("effects", this.effects.encode());

      if (version < 762) { // 1.19.4
        tagBuilder.putString("precipitation", this.hasPrecipitation ? "rain" : "none");
      } else {
        tagBuilder.putBoolean("has_precipitation", this.hasPrecipitation);
      }

      return tagBuilder.build();
    }
  }

  @ToString
  public static class Effects {
    private final int skyColor;
    private final int waterFogColor;
    private final int fogColor;
    private final int waterColor;

    private final Integer foliageColor;
    private final String grassColorModifier;
    private final Music music;
    private final String ambientSound;
    private final AdditionsSound additionsSound;
    private final MoodSound moodSound;
    private final Particle particle;

    public Effects(int skyColor,
                   int waterFogColor, int fogColor, int waterColor,
                   Integer foliageColor, String grassColorModifier, Music music,
                   String ambientSound, AdditionsSound additionsSound,
                   MoodSound moodSound, Particle particle) {
      this.skyColor = skyColor;
      this.waterFogColor = waterFogColor;
      this.fogColor = fogColor;
      this.waterColor = waterColor;
      this.foliageColor = foliageColor;
      this.grassColorModifier = grassColorModifier;
      this.music = music;
      this.ambientSound = ambientSound;
      this.additionsSound = additionsSound;
      this.moodSound = moodSound;
      this.particle = particle;
    }

    public CompoundBinaryTag encode() {
      Builder result = CompoundBinaryTag.builder();

      result.putInt("sky_color", this.skyColor);
      result.putInt("water_fog_color", this.waterColor);
      result.putInt("fog_color", this.fogColor);
      result.putInt("water_color", this.waterColor);

      if (this.foliageColor != null) {
        result.putInt("foliage_color", this.foliageColor);
      }

      if (this.grassColorModifier != null) {
        result.putString("grass_color_modifier", this.grassColorModifier);
      }

      if (this.music != null) {
        result.put("music", this.music.encode());
      }

      if (this.ambientSound != null) {
        result.putString("ambient_sound", this.ambientSound);
      }

      if (this.additionsSound != null) {
        result.put("additions_sound", this.additionsSound.encode());
      }

      if (this.moodSound != null) {
        result.put("mood_sound", this.moodSound.encode());
      }

      if (this.particle != null) {
        result.put("particle", this.particle.encode());
      }

      return result.build();
    }

    public static EffectsBuilder builder(int skyColor, int waterFogColor, int fogColor, int waterColor) {
      return new EffectsBuilder()
        .skyColor(skyColor)
        .waterFogColor(waterFogColor)
        .fogColor(fogColor)
        .waterColor(waterColor);
    }

    @ToString
    public static final class MoodSound {
      private final int tickDelay;
      private final double offset;
      private final int blockSearchExtent;
      private final String sound;

      private MoodSound(int tickDelay, double offset, int blockSearchExtent, String sound) {
        this.tickDelay = tickDelay;
        this.offset = offset;
        this.blockSearchExtent = blockSearchExtent;
        this.sound = sound;
      }

      public static MoodSound of(int tickDelay, double offset, int blockSearchExtent, String sound) {
        return new MoodSound(tickDelay, offset, blockSearchExtent, sound);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
          .putInt("tick_delay", this.tickDelay)
          .putDouble("offset", this.offset)
          .putInt("block_search_extent", this.blockSearchExtent)
          .putString("sound", this.sound)
          .build();
      }
    }

    @ToString
    public static final class Music {
      private final boolean replaceCurrentMusic;
      private final String sound;
      private final int maxDelay;
      private final int minDelay;

      private Music(boolean replaceCurrentMusic, String sound, int maxDelay, int minDelay) {
        this.replaceCurrentMusic = replaceCurrentMusic;
        this.sound = sound;
        this.maxDelay = maxDelay;
        this.minDelay = minDelay;
      }

      public static Music of(boolean replaceCurrentMusic, String sound, int maxDelay, int minDelay) {
        return new Music(replaceCurrentMusic, sound, maxDelay, minDelay);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
          .putBoolean("replace_current_music", this.replaceCurrentMusic)
          .putString("sound", this.sound)
          .putInt("max_delay", this.maxDelay)
          .putInt("min_delay", this.minDelay)
          .build();
      }
    }

    @ToString
    public static final class AdditionsSound {
      private final String sound;
      private final double tickChance;

      private AdditionsSound(String sound, double tickChance) {
        this.sound = sound;
        this.tickChance = tickChance;
      }

      public static AdditionsSound of(String sound, double tickChance) {
        return new AdditionsSound(sound, tickChance);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
          .putString("sound", this.sound)
          .putDouble("tick_chance", this.tickChance)
          .build();
      }
    }

    @ToString
    public static final class Particle {
      private final float probability;
      private final ParticleOptions options;

      private Particle(float probability, ParticleOptions options) {
        this.probability = probability;
        this.options = options;
      }

      public static Particle of(float probability, ParticleOptions options) {
        return new Particle(probability, options);
      }

      public CompoundBinaryTag encode() {
        return CompoundBinaryTag.builder()
          .putFloat("probability", this.probability)
          .put("options", this.options.encode())
          .build();
      }

      @ToString
      public static class ParticleOptions {
        private final String type;

        public ParticleOptions(String type) {
          this.type = type;
        }

        public CompoundBinaryTag encode() {
          return CompoundBinaryTag.builder()
            .putString("type", this.type)
            .build();
        }
      }
    }

    @ToString
    @SuppressWarnings("unused")
    public static class EffectsBuilder {
      private int skyColor;
      private int waterFogColor;
      private int fogColor;
      private int waterColor;
      private Integer foliageColor;
      private String grassColorModifier;
      private Music music;
      private String ambientSound;
      private AdditionsSound additionsSound;
      private MoodSound moodSound;
      private Particle particle;

      public EffectsBuilder skyColor(int skyColor) {
        this.skyColor = skyColor;
        return this;
      }

      public EffectsBuilder waterFogColor(int waterFogColor) {
        this.waterFogColor = waterFogColor;
        return this;
      }

      public EffectsBuilder fogColor(int fogColor) {
        this.fogColor = fogColor;
        return this;
      }

      public EffectsBuilder waterColor(int waterColor) {
        this.waterColor = waterColor;
        return this;
      }

      public EffectsBuilder foliageColor(Integer foliageColor) {
        this.foliageColor = foliageColor;
        return this;
      }

      public EffectsBuilder grassColorModifier(String grassColorModifier) {
        this.grassColorModifier = grassColorModifier;
        return this;
      }

      public EffectsBuilder music(Music music) {
        this.music = music;
        return this;
      }

      public EffectsBuilder ambientSound(String ambientSound) {
        this.ambientSound = ambientSound;
        return this;
      }

      public EffectsBuilder additionsSound(AdditionsSound additionsSound) {
        this.additionsSound = additionsSound;
        return this;
      }

      public EffectsBuilder moodSound(MoodSound moodSound) {
        this.moodSound = moodSound;
        return this;
      }

      public EffectsBuilder particle(Particle particle) {
        this.particle = particle;
        return this;
      }

      public Effects build() {
        return new Effects(
          this.skyColor,
          this.waterFogColor,
          this.fogColor,
          this.waterColor,
          this.foliageColor,
          this.grassColorModifier,
          this.music,
          this.ambientSound,
          this.additionsSound,
          this.moodSound,
          this.particle
        );
      }
    }
  }
}
