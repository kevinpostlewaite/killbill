/*
 * Copyright 2010-2011 Ning, Inc.
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

package com.ning.billing.util.glue;

import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;

import com.ning.billing.util.bus.DefaultBusService;
import com.ning.billing.util.bus.InMemoryInternalBus;
import com.ning.billing.util.bus.PersistentBusConfig;
import com.ning.billing.util.bus.PersistentInternalBus;
import com.ning.billing.util.svcsapi.bus.BusService;
import com.ning.billing.util.svcsapi.bus.InternalBus;

import com.google.inject.AbstractModule;

public class BusModule extends AbstractModule {

    private final BusType type;
    private final ConfigSource configSource;

    public BusModule(final ConfigSource configSource) {
        this(BusType.PERSISTENT, configSource);
    }

    protected BusModule(final BusType type, final ConfigSource configSource) {
        this.type = type;
        this.configSource = configSource;
    }

    public enum BusType {
        MEMORY,
        PERSISTENT
    }

    @Override
    protected void configure() {
        bind(BusService.class).to(DefaultBusService.class);
        switch (type) {
            case MEMORY:
                configureInMemoryEventBus();
                break;
            case PERSISTENT:
                configurePersistentEventBus();
                break;
            default:
                new RuntimeException("Unrecognized EventBus type " + type);
        }

    }

    protected void configurePersistentBusConfig() {
        final PersistentBusConfig config = new ConfigurationObjectFactory(configSource).build(PersistentBusConfig.class);
        bind(PersistentBusConfig.class).toInstance(config);
    }

    private void configurePersistentEventBus() {
        configurePersistentBusConfig();
        bind(InternalBus.class).to(PersistentInternalBus.class).asEagerSingleton();
    }

    private void configureInMemoryEventBus() {
        bind(InternalBus.class).to(InMemoryInternalBus.class).asEagerSingleton();
    }
}
