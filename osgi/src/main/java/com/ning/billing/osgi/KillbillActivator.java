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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ning.billing.account.api.AccountUserApi;
import com.ning.billing.beatrix.bus.api.ExternalBus;
import com.ning.billing.payment.plugin.api.PaymentPluginApi;

public class KillbillActivator implements BundleActivator {

    private final AccountUserApi accountUserApi;

    private final ExternalBus externalBus;

    private volatile BundleContext context = null;
    private volatile ServiceRegistration accountApiRegistration = null;
    private volatile ServiceRegistration externalBusRegistration = null;

    public KillbillActivator(final AccountUserApi accountUserApi, final ExternalBus externalBus) {

        this.accountUserApi = accountUserApi;
        this.externalBus = externalBus;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        this.context = context;
        this.accountApiRegistration = context.registerService(AccountUserApi.class.getName(), accountUserApi, null);
        this.externalBusRegistration = context.registerService(ExternalBus.class.getName(), externalBus, null);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (accountApiRegistration != null) {
            accountApiRegistration.unregister();
            accountApiRegistration = null;
        }

        if (externalBusRegistration != null) {
            externalBusRegistration.unregister();
            externalBusRegistration = null;
        }
        this.context = null;
    }

    public Bundle[] getBundles() {
        if (context != null) {
            return context.getBundles();
        }
        return null;
    }

    public PaymentPluginApi getPaymentPluginApiForPlugin(final String pluginName) {
        try {
            final ServiceReference<PaymentPluginApi>[] paymentApiReferences = (ServiceReference<PaymentPluginApi>[]) context.getServiceReferences(PaymentPluginApi.class.getName(), "(name=hello)");
            final PaymentPluginApi pluginApi = context.getService(paymentApiReferences[0]);
            return pluginApi;
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            // STEPH TODO leak reference here
        }
        return null;
    }

}
