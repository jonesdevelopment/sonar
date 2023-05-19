/*
 *  Copyright (c) 2023, jones (https://jonesdev.xyz) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jones.sonar.common.statistics;

import jones.sonar.api.statistics.StatisticType;
import jones.sonar.api.statistics.Statistics;

import java.util.HashMap;
import java.util.Map;

public final class SonarStatistics implements Statistics {
    private final Map<StatisticType, Integer> mappings = new HashMap<>();

    @Override
    public int add(final StatisticType type, final int value) {
        mappings.put(type, value);
        return get(type, value);
    }

    @Override
    public int replace(final StatisticType type, final int value) {
        if (mappings.containsKey(type)) {
            mappings.replace(type, value);
            return get(type, value);
        }
        return add(type, value);
    }

    @Override
    public int increment(final StatisticType type, final int by) {
        return replace(type, get(type, 0) + by);
    }

    @Override
    public int get(final StatisticType type, final int def) {
        return mappings.getOrDefault(type, def);
    }
}
