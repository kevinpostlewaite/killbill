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

package com.ning.billing.osgi.pluginconf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import com.ning.billing.osgi.api.config.PluginRubyConfig;
import com.ning.billing.util.config.OSGIConfig;

public class PluginFinder {


    private final static String INSTALATION_PROPERTIES = "killbill.properties";

    private final OSGIConfig osgiConfig;

    private final Map<String, List<DefaultPluginRubyConfig>> rubyPlugins;

    @Inject
    public PluginFinder(final OSGIConfig osgiConfig) {
        this.osgiConfig = osgiConfig;
        this.rubyPlugins = new HashMap<String, List<DefaultPluginRubyConfig>>();
    }

    public List<PluginRubyConfig> getLatestRubyPlugins() throws PluginConfigException {

        loadRubyPluginsIfRequired();

        final List<PluginRubyConfig> result = new LinkedList<PluginRubyConfig>();
        for (final String pluginName : rubyPlugins.keySet()) {
            result.add(rubyPlugins.get(pluginName).get(0));
        }
        return result;
    }

    private void loadRubyPluginsIfRequired() throws PluginConfigException {

        synchronized (rubyPlugins) {

            if (rubyPlugins.size() > 0) {
                return;
            }

            final String rootDirPath = osgiConfig.getRootInstallationDir() + "/" + DefaultPluginRubyConfig.PLUGIN_LANGUGAGE;
            final File rootDir = new File(rootDirPath);
            if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
                throw new PluginConfigException("Configuration root dir " + rootDirPath + " is not a valid directory");
            }
            for (final File curPlugin : rootDir.listFiles()) {
                // Skip any non directory entry
                if (!curPlugin.isDirectory()) {
                    continue;
                }
                final String pluginName = curPlugin.getName();

                for (final File curVersion : curPlugin.listFiles()) {
                    // Skip any non directory entry
                    if (!curVersion.isDirectory()) {
                        continue;
                    }
                    final String version = curVersion.getName();

                    final DefaultPluginRubyConfig plugin = extractPluginRubyConfig(pluginName, version, curVersion);
                    List<DefaultPluginRubyConfig> curPluginVersionlist = rubyPlugins.get(plugin.getPluginName());
                    if (curPluginVersionlist == null) {
                        curPluginVersionlist = new LinkedList<DefaultPluginRubyConfig>();
                        rubyPlugins.put(plugin.getPluginName(), curPluginVersionlist);
                    }
                    curPluginVersionlist.add(plugin);
                }
            }

            // Order for each plugin by versions starting from highest version
            for (final String pluginName : rubyPlugins.keySet()) {
                final List<DefaultPluginRubyConfig> value = rubyPlugins.get(pluginName);
                Collections.sort(value, new Comparator<DefaultPluginRubyConfig>() {
                    @Override
                    public int compare(final DefaultPluginRubyConfig o1, final DefaultPluginRubyConfig o2) {
                        return o1.getVersion().compareTo(o2.getVersion());
                    }
                });
            }
        }
    }


    private DefaultPluginRubyConfig extractPluginRubyConfig(final String pluginName, final String pluginVersion, final File pluginVersionDir) throws PluginConfigException {

        Properties props = null;
        try {
            for (final File cur : pluginVersionDir.listFiles()) {

                if (cur.isFile() && cur.getName().equals(INSTALATION_PROPERTIES)) {
                    props = readPluginConfigurationFile(cur);
                }

                if (props != null) {
                    break;
                }
            }

            if (props == null) {
                throw new PluginConfigException("Invalid plugin configuration file for " + pluginName + "-" + pluginVersion);
            }

        } catch (IOException e) {
            throw new PluginConfigException("Failed to read property file for " + pluginName + "-" + pluginVersion, e);
        }

        return new DefaultPluginRubyConfig(pluginName, pluginVersion, pluginVersionDir, props);
    }


    private Properties readPluginConfigurationFile(final File config) throws IOException {

        final Properties props = new Properties();
        final BufferedReader br = new BufferedReader(new FileReader(config));
        String line;
        while ((line = br.readLine()) != null) {
            final String[] parts = line.split("\\s*=\\s*");
            final String key = parts[0];
            final String value = parts[1];
            props.put(key, value);
        }
        br.close();
        return props;
    }

}
