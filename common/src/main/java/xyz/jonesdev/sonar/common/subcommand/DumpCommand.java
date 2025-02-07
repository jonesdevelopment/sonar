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

package xyz.jonesdev.sonar.common.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.SonarPlatform;
import xyz.jonesdev.sonar.api.command.InvocationSource;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.WeakHashMap;

import static xyz.jonesdev.sonar.api.profiler.SimpleProcessProfiler.*;

// This command is only used to display helpful information to help
// the Sonar contributors to fix issues more quickly
@SubcommandInfo(
  name = "dump",
  onlyConsole = true
)
public final class DumpCommand extends Subcommand {

  private static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();
  private static final Gson GSON = new GsonBuilder().create();

  @Override
  protected void execute(final @NotNull InvocationSource source, final String @NotNull [] args) {
    final var mappings = new WeakHashMap<String, Object>();
    mappings.put("sonar", new Dump.Sonar(
      Sonar.get0().getVersion().getFormatted(),
      Sonar.get0().getPlatform(),
      Sonar.get0().getVersion().getGitBranch(),
      Sonar.get0().getVersion().getGitCommit()
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
    Sonar.get0().getLogger().info(Sonar.get0().getConfig().getMessagesConfig().getString("commands.dump.log")
      .replace("<dumped-json-data>", GSON.toJson(mappings)));
  }

  @SuppressWarnings("unused")
  private static final class Dump {

    @RequiredArgsConstructor
    private static final class Sonar {
      private final String version;
      private final SonarPlatform platform;
      private final String gitBranch;
      private final String gitCommit;
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
