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

package jones.sonar.api.fallback;

import jones.sonar.api.Sonar;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class FallbackQueue {
    @Getter
    private final Map<InetAddress, Runnable> queuedPlayers = Collections.synchronizedMap(new HashMap<>());

    public void queue(final InetAddress inetAddress, final Runnable runnable) {
        queuedPlayers.put(inetAddress, runnable);
    }

    public void poll() {
        synchronized (queuedPlayers) {
            for (int i = 0; i < Sonar.get().getConfig().MAXIMUM_QUEUE_POLLS; i++) {
                if (queuedPlayers.isEmpty()) break;

                queuedPlayers.keySet().stream()
                        .findFirst()
                        .ifPresent(inetAddress -> {
                            queuedPlayers.get(inetAddress).run();
                            queuedPlayers.remove(inetAddress);
                        });
            }
        }
    }
}
