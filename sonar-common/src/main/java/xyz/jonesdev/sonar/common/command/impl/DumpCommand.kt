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

package xyz.jonesdev.sonar.common.command.impl

import xyz.jonesdev.sonar.api.command.CommandInvocation
import xyz.jonesdev.sonar.api.command.subcommand.Subcommand
import xyz.jonesdev.sonar.api.command.subcommand.SubcommandInfo
import xyz.jonesdev.sonar.api.format.MemoryFormatter.formatMemory
import java.lang.management.ManagementFactory

// This command is only used to display helpful information in order to help
// the Sonar developer(s) to fix issues more quickly
@SubcommandInfo(
  name = "dump",
  description = "Print developer information",
  onlyConsole = true
)
class DumpCommand : Subcommand() {
  private val runtime = Runtime.getRuntime()
  private val runtimeMxBean = ManagementFactory.getRuntimeMXBean()

  override fun execute(invocation: CommandInvocation) {
    val virtualCores = runtime.availableProcessors()
    val freeMemory = runtime.freeMemory()
    val totalMemory = runtime.totalMemory()
    val maxMemory = runtime.maxMemory()
    val usedMemory = totalMemory - freeMemory
    val platform = sonar.server.platform.displayName
    val fullVersion = sonar.version.full

    sonar.logger.info("Virtual cores: $virtualCores")
    sonar.logger.info("JVM args: ${runtimeMxBean.inputArguments.joinToString(", ")}")
    sonar.logger.info("Java version: ${runtimeMxBean.vmVersion}")
    sonar.logger.info("Operating system: ${System.getProperty("os.name")}; ${System.getProperty("os.arch")}")
    sonar.logger.info("Data storage type: ${sonar.config.DATABASE}")
    sonar.logger.info("Total memory: ${formatMemory(totalMemory)}")
    sonar.logger.info("Max memory: ${formatMemory(maxMemory)}")
    sonar.logger.info("Free memory: ${formatMemory(freeMemory)}")
    sonar.logger.info("Used Memory: ${formatMemory(usedMemory)}")
    sonar.logger.info("Platform: $platform")
    sonar.logger.info("Version: $fullVersion")
  }
}
