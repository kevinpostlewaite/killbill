/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.osgi;

import com.ning.billing.lifecycle.LifecycleHandlerType;
import com.ning.billing.lifecycle.LifecycleHandlerType.LifecycleLevel;
import com.ning.billing.osgi.api.OSGIService;

public class DefaultOSGIService implements OSGIService {

    public static final String OSGI_SERVICE_NAME = "osgi-service";

    @Override
    public String getName() {
        return OSGI_SERVICE_NAME;
    }


    @LifecycleHandlerType(LifecycleLevel.INIT_SERVICE)
    public void initialize() {

    }

    @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
    public void start() {

    }

    @LifecycleHandlerType(LifecycleLevel.STOP_SERVICE)
    public void stop() {

    }
}
