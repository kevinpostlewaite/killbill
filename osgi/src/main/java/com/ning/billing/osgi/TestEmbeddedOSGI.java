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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEmbeddedOSGI {


    private final static String HELLO_WORLD_BUNDLE =
            "file:/Users/stephanebrossier/Work/OpenSource/killbill/killbill/osgi-bundles//target/killbill-osgi-bundles-0.1.51-SNAPSHOT.jar";


    private final static Logger logger = LoggerFactory.getLogger(TestEmbeddedOSGI.class);


    private KillbillActivator killbillActivator;

    public TestEmbeddedOSGI() {
        this.killbillActivator = null;
    }

    public void doIt() throws BundleException, InterruptedException {

        final Framework framework = createAndInitFramework(true);

        framework.start();

        installAndStartBundles(framework);


        Bundle [] foundInstalledBundles = getInstalledBundles();
        for (Bundle cur : foundInstalledBundles) {
            System.out.println("Found installed bundle " + cur.toString());
        }

        scheduleStop(framework);


        FrameworkEvent event = framework.waitForStop(0);
        System.out.println("Got event " + event);
    }


    public static void scheduleStop(final Framework framework) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(10000);
                    System.out.println("Will stop framework : STOP!!!!!");
                    framework.stop();
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }).start();
    }

    public Bundle[] getInstalledBundles() {
        // Use the system bundle activator to gain external
        // access to the set of installed bundles.
        return killbillActivator.getBundles();
    }


    public static void main(String[] args) throws Exception {

        new TestEmbeddedOSGI().doIt();
    }


    private Framework createAndInitFramework(boolean withSystemBundle) throws BundleException {

        final Map<String, String> config = new HashMap<String, String>();

        config.put("felix.cache.rootdir", "/var/tmp/felix");
        config.put("org.osgi.framework.storage", "osgi-cache");

        if (withSystemBundle) {
            return createAndInitFelixFrameworkWithSystemBundle(config);
        } else {
            return createAndIntOSGIFramework(config);
        }
    }


    private Framework createAndInitFelixFrameworkWithSystemBundle(Map<String, String> config) throws BundleException {


        Map<Object,Object> felixConfig = new HashMap<Object, Object>();
        felixConfig.putAll(config);

        this.killbillActivator = new KillbillActivator(null, null);

        felixConfig.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Collections.singletonList(killbillActivator));

        Framework felix = new Felix(felixConfig);
        felix.init();
        return felix;
    }

    private Framework createAndIntOSGIFramework(Map<String, String> config) throws BundleException {
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();


        Framework framework = frameworkFactory.newFramework(config);

        framework.init();

        return framework;
    }

    private void installAndStartBundles(final Framework framework) throws BundleException {
        final BundleContext context = framework.getBundleContext();
        List<Bundle> installedBundles = new LinkedList<Bundle>();

        installedBundles.add(context.installBundle(HELLO_WORLD_BUNDLE));
        for (Bundle bundle : installedBundles) {
            bundle.start();
        }
    }



    //////  For debugging...

    public static String getCurrentClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader applicationClassLoader = Thread.currentThread().getClass().getClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
        for (int i = 0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }

    public static String getContextClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader applicationClassLoader = Thread.currentThread().getContextClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader) applicationClassLoader).getURLs();
        for (int i = 0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }

}
