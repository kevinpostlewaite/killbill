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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEmbeddedOSGI {


    private final static Logger logger = LoggerFactory.getLogger(TestEmbeddedOSGI.class);

    public static void main(String [] args) throws Exception {

        System.out.println("YO");

        System.out.println("CURRENT:\n" + getCurrentClasspathString());
        //logger.info("CURRENT:\n" + getCurrentClasspathString());

        System.out.println("CONTEXT:\n" + getContextClasspathString());
        //logger.info("CONTEXT:\n" + getContextClasspathString());


        FrameworkFactory frameworkFactory = ServiceLoader.load(
                FrameworkFactory.class).iterator().next();
        Map<String, String> config = new HashMap<String, String>();
        Framework framework = frameworkFactory.newFramework(config);
        framework.start();



        BundleContext context = framework.getBundleContext();
        List<Bundle> installedBundles = new LinkedList<Bundle>();

        installedBundles.add(context.installBundle(
                "file:/Users/stephanebrossier/Work/OpenSource/killbill/killbill/osgi-bundles//target/killbill-osgi-bundles-0.1.51-SNAPSHOT.jar"));
/*
        installedBundles.add(context.installBundle(
                "file:org.apache.felix.shell.tui-1.4.1.jar"));
*/
        for (Bundle bundle : installedBundles) {
            bundle.start();
        }
    }


    public static String getCurrentClasspathString() {
        StringBuffer classpath = new StringBuffer();
        ClassLoader applicationClassLoader = Thread.currentThread().getClass().getClassLoader();
        if (applicationClassLoader == null) {
            applicationClassLoader = ClassLoader.getSystemClassLoader();
        }
        URL[] urls = ((URLClassLoader)applicationClassLoader).getURLs();
        for(int i=0; i < urls.length; i++) {
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
        URL[] urls = ((URLClassLoader)applicationClassLoader).getURLs();
        for(int i=0; i < urls.length; i++) {
            classpath.append(urls[i].getFile()).append("\r\n");
        }

        return classpath.toString();
    }

}
