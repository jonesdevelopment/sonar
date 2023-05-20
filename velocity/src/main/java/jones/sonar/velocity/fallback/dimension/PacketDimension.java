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

package jones.sonar.velocity.fallback.dimension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// pasted from
// https://github.com/Elytrium/LimboAPI/blob/91bedd5dad5e659092fbb0a7411bd00d67044d01/api/src/main/java/net/elytrium/limboapi/api/chunk/Dimension.java
@Getter
@RequiredArgsConstructor
public enum PacketDimension {
    OVERWORLD("minecraft:overworld", 0, 0, 28, true), // (384 + 64) / 16
    NETHER("minecraft:the_nether", -1, 1, 16, false), // 256 / 16
    THE_END("minecraft:the_end", 1, 2, 16, false); // 256 / 16

    private final String key;
    private final int legacyID;
    private final int modernID;
    private final int maxSections;
    private final boolean hasLegacySkyLight;
}
