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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.billing.account.api.AccountUserApi;
import com.ning.billing.beatrix.bus.api.ExternalBus;
import com.ning.billing.lifecycle.LifecycleHandlerType;
import com.ning.billing.lifecycle.LifecycleHandlerType.LifecycleLevel;
import com.ning.billing.osgi.api.OSGIService;
import com.ning.billing.payment.plugin.api.PaymentPluginApi;

public class DefaultOSGIService implements OSGIService {

    public static final String OSGI_SERVICE_NAME = "osgi-service";


    private final static String HELLO_WORLD_BUNDLE =
            "file:/Users/stephanebrossier/Work/OpenSource/killbill/killbill/osgi-bundles//target/killbill-osgi-bundles-0.1.51-SNAPSHOT.jar";


    private final static String EVENT_ADMIN_BUNDLE =
            "file:/Users/stephanebrossier/.m2/repository/org/apache/felix/org.apache.felix.eventadmin/1.3.2/org.apache.felix.eventadmin-1.3.2.jar";

    private final static Logger logger = LoggerFactory.getLogger(DefaultOSGIService.class);


    private KillbillActivator killbillActivator;
    private Framework framework;

    private final AccountUserApi accountUserApi;
    private final ExternalBus externalBus;

    @Inject
    public DefaultOSGIService(final AccountUserApi accountUserApi, final ExternalBus externalBus) {
        this.accountUserApi = accountUserApi;
        this.externalBus = externalBus;

        this.killbillActivator = null;
        this.framework = null;

    }

    @Override
    public String getName() {
        return OSGI_SERVICE_NAME;
    }


    @LifecycleHandlerType(LifecycleLevel.INIT_SERVICE)
    public void initialize() {
        try {

            // STEPH Prune the existing felix bundle cache
            this.framework = createAndInitFramework();
            framework.start();

            installAndStartBundles(framework);

        } catch (BundleException e) {
            logger.error("Failed to initialize Killbill OSGIService " + e.getMessage());
        }
    }

    @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
    public void startFramework() {
        PaymentPluginApi pluginApi = killbillActivator.getPaymentPluginApiForPlugin("hello");
        final String name = pluginApi.getName();
        System.out.println("name = " + name);
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.REGISTER_EVENTS)
    public void registerForExternalEvents() {
        externalBus.register(killbillActivator);
    }

    @LifecycleHandlerType(LifecycleHandlerType.LifecycleLevel.UNREGISTER_EVENTS)
    public void unregisterForExternalEvents() {
        externalBus.unregister(killbillActivator);
    }

    @LifecycleHandlerType(LifecycleLevel.STOP_SERVICE)
    public void stop() {
        try {
            framework.stop();
            framework.waitForStop(0);
        } catch (BundleException e) {
            logger.error("Failed to Stop Killbill OSGIService " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Failed to Stop Killbill OSGIService " + e.getMessage());
        }
    }


    private void installAndStartBundles(final Framework framework) throws BundleException {
        final BundleContext context = framework.getBundleContext();
        List<Bundle> installedBundles = new LinkedList<Bundle>();

        installedBundles.add(context.installBundle(HELLO_WORLD_BUNDLE));
        installedBundles.add(context.installBundle(EVENT_ADMIN_BUNDLE));
        for (Bundle bundle : installedBundles) {
            bundle.start();
        }
    }

    private Framework createAndInitFramework() throws BundleException {

        final Map<String, String> config = new HashMap<String, String>();

        // STEPH export all KB Apis.
        final StringBuilder tmp = new StringBuilder()
                .append("com.ning.billing.account.api,")
                .append("com.ning.billing.beatrix.bus.api,")
                .append("com.ning.billing.payment.plugin.api,")
                .append("com.ning.billing.util.callcontext,")
                .append("com.google.common.eventbus");

        config.put("org.osgi.framework.system.packages.extra", tmp.toString());
        config.put("felix.cache.rootdir", "/var/tmp/felix");
        config.put("org.osgi.framework.storage", "osgi-cache");

        return createAndInitFelixFrameworkWithSystemBundle(config);
    }


    private Framework createAndInitFelixFrameworkWithSystemBundle(Map<String, String> config) throws BundleException {


        Map<Object, Object> felixConfig = new HashMap<Object, Object>();
        felixConfig.putAll(config);

        this.killbillActivator = new KillbillActivator(accountUserApi, externalBus);

        felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Collections.singletonList(killbillActivator));

        Framework felix = new Felix(felixConfig);
        felix.init();
        return felix;
    }
}
