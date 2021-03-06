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

package com.ning.billing.tenant.api;

import java.util.List;
import java.util.UUID;

import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;

public interface TenantUserApi {

    public Tenant createTenant(final TenantData data, final CallContext context) throws TenantApiException;

    public Tenant getTenantByApiKey(final String key) throws TenantApiException;

    public Tenant getTenantById(final UUID tenantId) throws TenantApiException;

    public List<String> getTenantValueForKey(final String key, final TenantContext context) throws TenantApiException;

    public void addTenantKeyValue(final String key, final String value, final CallContext context) throws TenantApiException;

    public void deleteTenantKey(final String key, final CallContext context) throws TenantApiException;
}
