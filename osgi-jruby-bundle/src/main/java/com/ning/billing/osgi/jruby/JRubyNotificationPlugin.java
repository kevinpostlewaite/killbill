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

package com.ning.billing.osgi.jruby;

import org.jruby.javasupport.JavaEmbedUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.billing.beatrix.bus.api.ExtBusEvent;
import com.ning.billing.beatrix.bus.api.ExternalBus;

import com.google.common.eventbus.Subscribe;

public class JRubyNotificationPlugin extends JRubyPlugin {

    private final Logger logger = LoggerFactory.getLogger(JRubyNotificationPlugin.class);

    public JRubyNotificationPlugin(final String pluginMainClass, final String pluginLibdir) {
        super(pluginMainClass, pluginLibdir);
    }

    @Override
    public void startPlugin(final BundleContext context) {
        super.startPlugin(context);

        final ServiceReference<ExternalBus> externalBusReference = (ServiceReference) context.getServiceReference(ExternalBus.class.getName());
        try {
            final ExternalBus externalBus = context.getService(externalBusReference);
            externalBus.register(this);
        } catch (Exception e) {
            logger.warn("Error registering notification plugin service", e);
        } finally {
            if (externalBusReference != null) {
                context.ungetService(externalBusReference);
            }
        }
    }

    @Subscribe
    public void onEvent(final ExtBusEvent event) {
        checkValidNotificationPlugin();
        checkPluginIsRunning();

        pluginInstance.callMethod("on_event", JavaEmbedUtils.javaToRuby(getRuntime(), event));
    }
}
