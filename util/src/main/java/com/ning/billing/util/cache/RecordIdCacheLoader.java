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

package com.ning.billing.util.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.ning.billing.ObjectType;
import com.ning.billing.util.dao.TableName;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

public class RecordIdCacheLoader implements CacheLoader {

    private final IDBI dbi;

    private Status cacheLoaderStatus;


    @Inject
    public RecordIdCacheLoader(final IDBI dbi) {
        this.dbi = dbi;
        this.cacheLoaderStatus = Status.STATUS_UNINITIALISED;

    }

    @Override
    public Object load(final Object key, final Object argument) throws CacheException {

        checkCacheLoaderStatus();

        if (!(argument instanceof ObjectType)) {
            throw new IllegalArgumentException("Unexpected argument type of " +
                                               argument != null ? argument.getClass().getName() : "null");
        }
        if (!(key instanceof String)) {
            throw new IllegalArgumentException("Unexpected key type of " +
                                               key != null ? key.getClass().getName() : "null");

        }
        final String objectId = (String) key;
        final ObjectType objectType = (ObjectType) argument;
        Long value = retrieveRecordIdFromIdAndType(objectId, objectType);
        return value;
    }

    @Override
    public Object load(final Object key) throws CacheException {
        throw new IllegalStateException("Method load is not implemented ");
    }

    @Override
    public Map loadAll(final Collection keys) {
        throw new IllegalStateException("Method loadAll is not implemented ");
    }


    @Override
    public Map loadAll(final Collection keys, final Object argument) {
        throw new IllegalStateException("Method loadAll is not implemented ");
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public CacheLoader clone(final Ehcache cache) throws CloneNotSupportedException {
        throw new IllegalStateException("Method clone is not implemented ");
    }

    @Override
    public void init() {
        this.cacheLoaderStatus = Status.STATUS_ALIVE;
    }

    @Override
    public void dispose() throws CacheException {
        cacheLoaderStatus = Status.STATUS_SHUTDOWN;
    }

    @Override
    public Status getStatus() {
        return cacheLoaderStatus;
    }


    private Long retrieveRecordIdFromIdAndType(final String objectId, final ObjectType objectType) {

        final Long recordId;

        final TableName tableName = TableName.fromObjectType(objectType);
        if (tableName != null) {
            recordId = dbi.withHandle(new HandleCallback<Long>() {
                @Override
                public Long withHandle(final Handle handle) throws Exception {

                    final List<Map<String, Object>> values = handle.select(String.format("select record_id from %s where id = ?;", tableName.getTableName()), objectId);
                    if (values.size() == 0) {
                        return null;
                    } else {
                        final Object recordId = values.get(0).get("record_id");
                        return recordId == null ? null : Long.valueOf(recordId.toString());
                    }
                }
            });
        } else {
            recordId = null;
        }
        return recordId;
    }

    private void checkCacheLoaderStatus() {
        if (getStatus() != Status.STATUS_ALIVE) {
            throw new CacheException("CacheLoader is not available!");
        }
    }
}
