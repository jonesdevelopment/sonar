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

package jones.sonar.api.verbose;

import java.util.Collection;

public interface Verbose {
    Collection<String> getSubscribers();

    default boolean isSubscribed(final String subscriber) {
        return getSubscribers().contains(subscriber);
    }

    default void subscribe(final String username) {
        getSubscribers().add(username);
    }

    default void unsubscribe(final String subscriber) {
        getSubscribers().remove(subscriber);
    }
}
