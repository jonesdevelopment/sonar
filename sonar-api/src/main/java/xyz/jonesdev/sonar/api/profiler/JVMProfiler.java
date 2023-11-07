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

package xyz.jonesdev.sonar.api.profiler;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public interface JVMProfiler {
  Runtime RUNTIME = Runtime.getRuntime();
  OperatingSystemMXBean OPERATING_SYSTEM_MX_BEAN = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();
  char[] MEMORY_UNITS = {'k', 'M', 'G', 'T', 'P', 'E'};

  default String formatMemory(final long size) {
    if (size < 1024L) {
      return size + " B";
    }
    final int group = (63 - Long.numberOfLeadingZeros(size)) / 10;
    final double formattedSize = (double) size / (1L << (group * 10));
    final char unit = MEMORY_UNITS[group - 1];
    return String.format("%.1f %sB", formattedSize, unit);
  }

  default int getVirtualCores() {
    return RUNTIME.availableProcessors();
  }

  default double getProcessCPUUsage() {
    return OPERATING_SYSTEM_MX_BEAN.getProcessCpuLoad() * 100;
  }

  default double getAverageProcessCPUUsage() {
    return getProcessCPUUsage() / getVirtualCores();
  }

  default double getSystemCPUUsage() {
    return OPERATING_SYSTEM_MX_BEAN.getSystemCpuLoad() * 100;
  }

  default double getAverageSystemCPUUsage() {
    return getSystemCPUUsage() / getVirtualCores();
  }

  default double getSystemLoadAverage() {
    return OPERATING_SYSTEM_MX_BEAN.getSystemLoadAverage() * 100;
  }

  default long getMaxMemory() {
    return RUNTIME.maxMemory();
  }

  default long getTotalMemory() {
    return RUNTIME.totalMemory();
  }

  default long getFreeMemory() {
    return RUNTIME.freeMemory();
  }

  default long getUsedMemory() {
    return getTotalMemory() - getFreeMemory();
  }
}
