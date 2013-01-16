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

import java.io.File;
import java.io.FilenameFilter;
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
import com.ning.billing.util.config.OSGIConfig;

public class DefaultOSGIService implements OSGIService {

    public static final String OSGI_SERVICE_NAME = "osgi-service";

    private final static Logger logger = LoggerFactory.getLogger(DefaultOSGIService.class);


    private KillbillActivator killbillActivator;
    private Framework framework;

    private final AccountUserApi accountUserApi;
    private final ExternalBus externalBus;
    private final OSGIConfig osgiConfig;

    @Inject
    public DefaultOSGIService(final OSGIConfig osgiConfig, final AccountUserApi accountUserApi, final ExternalBus externalBus) {
        this.accountUserApi = accountUserApi;
        this.externalBus = externalBus;
        this.osgiConfig = osgiConfig;
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

            // We start by deleting existing osi cache; we might optimize later keeping the cache
            pruneOSGICache();

            // Create the system bundle for killbill and start the framework
            this.framework = createAndInitFramework();
            framework.start();

            // This will call the start() method for the bundles
            installAndStartBundles(framework);

        } catch (BundleException e) {
            logger.error("Failed to initialize Killbill OSGIService " + e.getMessage());
        }
    }

    @LifecycleHandlerType(LifecycleLevel.START_SERVICE)
    public void startFramework() {
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

        // Install the bundles
        List<Bundle> installedBundles = new LinkedList<Bundle>();
        for (String bundlePath : lookupInstalledBundles()) {
            installedBundles.add(context.installBundle(bundlePath));
        }

        // Start all the bundles
        for (Bundle bundle : installedBundles) {
            bundle.start();
        }
    }

    private String[] lookupInstalledBundles() {

        final List<String> result = new LinkedList<String>();
        final File bundleInstallationDir = new File(osgiConfig.getBundleInstallationDir());
        for (File f : bundleInstallationDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".jar");
            }
        })) {
            result.add("file:" + f.getAbsolutePath());
        }
        return result.toArray(new String[result.size()]);
    }


    private Framework createAndInitFramework() throws BundleException {

        final Map<String, String> config = new HashMap<String, String>();
        config.put("org.osgi.framework.system.packages.extra", osgiConfig.getSystemBundleExportPackages());
        config.put("felix.cache.rootdir", osgiConfig.getRootDir());
        config.put("org.osgi.framework.storage", osgiConfig.getBundleCacheName());
        return createAndInitFelixFrameworkWithSystemBundle(config);
    }


    private Framework createAndInitFelixFrameworkWithSystemBundle(Map<String, String> config) throws BundleException {

        // From standard properties add Felix specific property to add a System bundle activator
        final Map<Object, Object> felixConfig = new HashMap<Object, Object>();
        felixConfig.putAll(config);

        // Create the system bundle activator that will register the required services for Killbill
        this.killbillActivator = new KillbillActivator(accountUserApi, externalBus);
        felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Collections.singletonList(killbillActivator));

        Framework felix = new Felix(felixConfig);
        felix.init();
        return felix;
    }


    private void pruneOSGICache() {
        final String path = osgiConfig.getRootDir() + "/" + osgiConfig.getBundleCacheName();
        deleteUnderDirectory(new File(path));
    }

    static private void deleteUnderDirectory(File path) {
        deleteDirectory(path, false);
    }

    static private void deleteDirectory(final File path, final boolean deleteParent) {
        if (path == null) {
            return;
        }
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory()) {
                    deleteDirectory(f, true);
                    f.delete();
                } else {
                    f.delete();
                }
            }
            if (deleteParent) {
                path.delete();
            }
        }
    }

}
