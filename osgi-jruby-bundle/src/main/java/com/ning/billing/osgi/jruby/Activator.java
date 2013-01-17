/*
 * Copyright 2010-2012 Ning, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.billing.account.api.AccountUserApi;
import com.ning.billing.analytics.api.sanity.AnalyticsSanityApi;
import com.ning.billing.analytics.api.user.AnalyticsUserApi;
import com.ning.billing.catalog.api.CatalogUserApi;
import com.ning.billing.entitlement.api.migration.EntitlementMigrationApi;
import com.ning.billing.entitlement.api.timeline.EntitlementTimelineApi;
import com.ning.billing.entitlement.api.transfer.EntitlementTransferApi;
import com.ning.billing.entitlement.api.user.EntitlementUserApi;
import com.ning.billing.invoice.api.InvoiceMigrationApi;
import com.ning.billing.invoice.api.InvoicePaymentApi;
import com.ning.billing.invoice.api.InvoiceUserApi;
import com.ning.billing.meter.api.MeterUserApi;
import com.ning.billing.overdue.OverdueUserApi;
import com.ning.billing.payment.api.PaymentApi;
import com.ning.billing.tenant.api.TenantUserApi;
import com.ning.billing.usage.api.UsageUserApi;
import com.ning.billing.util.api.AuditUserApi;
import com.ning.billing.util.api.CustomFieldUserApi;
import com.ning.billing.util.api.ExportUserApi;
import com.ning.billing.util.api.TagUserApi;

public class Activator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private final List<ServiceReference<?>> apiReferences = new ArrayList<ServiceReference<?>>();

    private String pluginMainClass = null;
    private JRubyPlugin plugin = null;

    public void start(final BundleContext context) throws Exception {
        // TODO instantiate the plugin depending on the config

        context.getBundle().getBundleId();

        // Validate and instantiate the plugin
        final Map<String, Object> killbillApis = retrieveKillbillApis(context);
        plugin.instantiatePlugin(killbillApis);

        log.info("Starting JRuby plugin {}", pluginMainClass);
        plugin.startPlugin(context);
    }

    public void stop(final BundleContext context) throws Exception {
        log.info("Stopping JRuby plugin {}", pluginMainClass);
        plugin.stopPlugin(context);

        for (final ServiceReference<?> apiReference : apiReferences) {
            context.ungetService(apiReference);
        }
    }

    private Map<String, Object> retrieveKillbillApis(final BundleContext context) {
        final Map<String, Object> killbillUserApis = new HashMap<String, Object>();

        // See killbill/plugin.rb for the naming convention magic
        killbillUserApis.put("account_user_api", retrieveApi(context, AccountUserApi.class.getName()));
        killbillUserApis.put("analytics_sanity_api", retrieveApi(context, AnalyticsSanityApi.class.getName()));
        killbillUserApis.put("analytics_user_api", retrieveApi(context, AnalyticsUserApi.class.getName()));
        killbillUserApis.put("catalog_user_api", retrieveApi(context, CatalogUserApi.class.getName()));
        killbillUserApis.put("entitlement_migration_api", retrieveApi(context, EntitlementMigrationApi.class.getName()));
        killbillUserApis.put("entitlement_timeline_api", retrieveApi(context, EntitlementTimelineApi.class.getName()));
        killbillUserApis.put("entitlement_transfer_api", retrieveApi(context, EntitlementTransferApi.class.getName()));
        killbillUserApis.put("entitlement_user_api", retrieveApi(context, EntitlementUserApi.class.getName()));
        killbillUserApis.put("invoice_migration_api", retrieveApi(context, InvoiceMigrationApi.class.getName()));
        killbillUserApis.put("invoice_payment_api", retrieveApi(context, InvoicePaymentApi.class.getName()));
        killbillUserApis.put("invoice_user_api", retrieveApi(context, InvoiceUserApi.class.getName()));
        killbillUserApis.put("meter_user_api", retrieveApi(context, MeterUserApi.class.getName()));
        killbillUserApis.put("overdue_user_api", retrieveApi(context, OverdueUserApi.class.getName()));
        killbillUserApis.put("payment_api", retrieveApi(context, PaymentApi.class.getName()));
        killbillUserApis.put("tenant_user_api", retrieveApi(context, TenantUserApi.class.getName()));
        killbillUserApis.put("usage_user_api", retrieveApi(context, UsageUserApi.class.getName()));
        killbillUserApis.put("audit_user_api", retrieveApi(context, AuditUserApi.class.getName()));
        killbillUserApis.put("custom_field_user_api", retrieveApi(context, CustomFieldUserApi.class.getName()));
        killbillUserApis.put("export_user_api", retrieveApi(context, ExportUserApi.class.getName()));
        killbillUserApis.put("tag_user_api", retrieveApi(context, TagUserApi.class.getName()));

        return killbillUserApis;
    }

    private <T> T retrieveApi(final BundleContext context, final String apiClazzName) {
        final ServiceReference apiReference = context.getServiceReference(apiClazzName);
        if (apiReference != null) {
            // Keep references to stop the bundle properly
            apiReferences.add(apiReference);

            //noinspection unchecked
            return (T) context.getService(apiReference);
        } else {
            return null;
        }
    }
}
