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

package xyz.jonesdev.sonar.common.subcommand.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// This command is only used to display helpful information in order to help
// the Sonar contributors to fix issues more quickly
@SubcommandInfo(
  name = "dump",
  description = "Print developer information",
  onlyConsole = true
)
public final class DumpCommand extends Subcommand implements JVMProfiler {
  private static final Gson GSON = new GsonBuilder()
    .disableInnerClassSerialization()
    .create();

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    final String json = GSON.toJson(collectMappedInformation());
    SONAR.getLogger().info("Generated dump: {}", json);
  }

  @Unmodifiable
  private @NotNull Map<String, Object> collectMappedInformation() {
    val mappings = new WeakHashMap<String, Object>();
    mappings.put("sonar", new Dump.Sonar(
      SONAR.getVersion().getFull(),
      SONAR.getPlatform(),
      SONAR.getVersion().isOnMainBranch()
    ));
    mappings.put("runtime", new Dump.Runtime(
      getVirtualCores(),
      RUNTIME_MX_BEAN.getInputArguments(),
      RUNTIME_MX_BEAN.getVmName(),
      RUNTIME_MX_BEAN.getVmVendor(),
      RUNTIME_MX_BEAN.getVmVersion()
    ));
    mappings.put("os", new Dump.OS(
      System.getProperty("os.name"),
      System.getProperty("os.arch"),
      System.getProperty("os.version")
    ));
    mappings.put("memory", new Dump.Memory(
      formatMemory(getTotalMemory()),
      formatMemory(getMaxMemory()),
      formatMemory(getFreeMemory()),
      formatMemory(getUsedMemory())
    ));
    return mappings;
  }

  @SuppressWarnings("unused")
  private static final class Dump {

    @RequiredArgsConstructor
    private static final class Sonar {
      private final String version;
      private final SonarPlatform platform;
      private final boolean isOnMainBranch;
    }

    @RequiredArgsConstructor
    private static final class Runtime {
      private final int virtualCores;
      private final List<String> jvmArguments;
      private final String vmName;
      private final String vmVendor;
      private final String vmVersion;
    }

    @RequiredArgsConstructor
    private static final class Memory {
      private final String total;
      private final String max;
      private final String free;
      private final String used;
    }

    @RequiredArgsConstructor
    private static final class OS {
      private final String name;
      private final String arch;
      private final String version;
    }
  }
}
