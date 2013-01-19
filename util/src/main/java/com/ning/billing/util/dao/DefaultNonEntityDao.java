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

package com.ning.billing.util.dao;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.skife.jdbi.v2.IDBI;

import com.ning.billing.ObjectType;
import com.ning.billing.util.cache.CacheController;

public class DefaultNonEntityDao implements NonEntityDao {

    private final NonEntitySqlDao nonEntitySqlDao;


    @Inject
    public DefaultNonEntityDao(final IDBI dbi) {
        this.nonEntitySqlDao = dbi.onDemand(NonEntitySqlDao.class);
    }

    public Long retrieveRecordIdFromObject(final UUID objectId, final ObjectType objectType, @Nullable final CacheController<Object, Object> cache) {

        final Long cachedResult = (cache != null) ? (Long) cache.get(objectId.toString(), objectType) : null;
        if (cachedResult != null) {
            return cachedResult;
        }

        final TableName tableName = TableName.fromObjectType(objectType);
        return nonEntitySqlDao.getRecordIdFromObject(objectId.toString(), tableName.getTableName());
    }

    public Long retrieveAccountRecordIdFromObject(final UUID objectId, final ObjectType objectType, @Nullable final CacheController<Object, Object> cache) {

        final Long cachedResult = (cache != null) ? (Long) cache.get(objectId.toString(), objectType) : null;
        if (cachedResult != null) {
            return cachedResult;
        }

        final TableName tableName = TableName.fromObjectType(objectType);
        switch (tableName) {
            case TAG_DEFINITIONS:
            case TAG_DEFINITION_HISTORY:
                return null;

            case ACCOUNT:
                return nonEntitySqlDao.getAccountRecordIdFromAccount(objectId.toString());

            case ACCOUNT_HISTORY:
                return nonEntitySqlDao.getAccountRecordIdFromAccountHistory(objectId.toString());

            default:
                return nonEntitySqlDao.getAccountRecordIdFromObjectOtherThanAccount(objectId.toString(), tableName.getTableName());
        }
    }


    public Long retrieveTenantRecordIdFromObject(final UUID objectId, final ObjectType objectType, @Nullable final CacheController<Object, Object> cache) {

        final Long cachedResult = (cache != null) ? (Long) cache.get(objectId.toString(), objectType) : null;
        if (cachedResult != null) {
            return cachedResult;
        }
        final TableName tableName = TableName.fromObjectType(objectType);
        switch (tableName) {
            case TENANT:
                return nonEntitySqlDao.getTenantRecordIdFromTenant(objectId.toString());

            default:
                return nonEntitySqlDao.getTenantRecordIdFromObjectOtherThanTenant(objectId.toString(), tableName.getTableName());
        }
    }
}
