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

package jones.sonar.velocity.fallback;

import jones.sonar.api.fallback.Fallback;
import jones.sonar.api.fallback.FallbackCleaner;
import jones.sonar.velocity.SonarVelocity;

public final class FallbackPlayerCleaner implements FallbackCleaner {

    @Override
    public void clean(final Fallback fallback) {
        fallback.getConnected()
                .removeIf(inetAddress -> SonarVelocity.INSTANCE.getPlugin().getServer().getAllPlayers().stream()
                .noneMatch(player -> player.getRemoteAddress().getAddress().toString().equals(inetAddress.toString())));
    }
}
