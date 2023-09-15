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

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.command.CommandInvocation;
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand;
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo;
import xyz.jonesdev.sonar.api.profiler.JVMProfiler;

// This command is only used to display helpful information in order to help
// the Sonar developer(s) to fix issues more quickly
@SubcommandInfo(
  name = "dump",
  description = "Print developer information",
  onlyConsole = true
)
public final class DumpCommand extends Subcommand implements JVMProfiler {

  @Override
  public void execute(final @NotNull CommandInvocation invocation) {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String jvmArgs = String.join(",", RUNTIME_MX_BEAN.getInputArguments());

    invocation.getSender().sendMessage("Virtual cores: " + getVirtualCores());
    invocation.getSender().sendMessage("JVM args: " + jvmArgs);
    invocation.getSender().sendMessage("Java name: " + RUNTIME_MX_BEAN.getVmName());
    invocation.getSender().sendMessage("Java version: " + RUNTIME_MX_BEAN.getVmVersion());
    invocation.getSender().sendMessage("Java vendor: " + RUNTIME_MX_BEAN.getVmVendor());
    invocation.getSender().sendMessage("Operating system: " + osName + ", " + osArch);
    invocation.getSender().sendMessage("Total memory: " + formatMemory(getTotalMemory()));
    invocation.getSender().sendMessage("Max memory: " + formatMemory(getMaxMemory()));
    invocation.getSender().sendMessage("Free memory: " + formatMemory(getFreeMemory()));
    invocation.getSender().sendMessage("Used Memory: " + formatMemory(getUsedMemory()));
    invocation.getSender().sendMessage("Platform: " + SONAR.getServer().getPlatform().getDisplayName());
    invocation.getSender().sendMessage("Version: " + SONAR.getVersion().getFull());
  }
}
