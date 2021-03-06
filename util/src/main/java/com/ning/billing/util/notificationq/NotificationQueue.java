/*
 * Copyright 2010-2011 Ning, Inc.
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

package com.ning.billing.util.notificationq;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;

import com.ning.billing.util.callcontext.InternalCallContext;
import com.ning.billing.util.entity.dao.EntitySqlDao;
import com.ning.billing.util.entity.dao.EntitySqlDaoWrapperFactory;
import com.ning.billing.util.notificationq.NotificationQueueService.NotificationQueueHandler;
import com.ning.billing.util.queue.QueueLifecycle;

public interface NotificationQueue extends QueueLifecycle {

    /**
     * Record the need to be called back when the notification is ready
     *
     * @param futureNotificationTime the time at which the notification is ready
     * @param notificationKey        the key for that notification
     */
    public void recordFutureNotification(final DateTime futureNotificationTime,
                                         final NotificationKey notificationKey,
                                         final InternalCallContext context)
            throws IOException;

    /**
     * Record from within a transaction the need to be called back when the notification is ready
     *
     * @param transactionalDao       the transactionalDao
     * @param futureNotificationTime the time at which the notification is ready
     * @param notificationKey        the key for that notification
     */
    public void recordFutureNotificationFromTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> transactionalDao,
                                                        final DateTime futureNotificationTime,
                                                        final NotificationKey notificationKey,
                                                        final InternalCallContext context)
            throws IOException;

    /**
     * Retrieve all future pending notifications for a given account (taken from the context)
     * Results are ordered by effective date asc.
     *
     * @param context internal call context
     * @return future notifications matching that key
     */
    public List<Notification> getFutureNotificationsForAccount(final InternalCallContext context);

    /**
     * Retrieve all future pending notifications for a given account (taken from the context) in a transaction.
     * Results are ordered by effective date asc.
     *
     * @param context internal call context
     * @return future notifications matching that key
     */
    public List<Notification> getFutureNotificationsForAccountFromTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> transactionalDao,
                                                                              final InternalCallContext context);

    public void removeNotification(final UUID notificationId,
                                   final InternalCallContext context);

    public void removeNotificationFromTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> transactionalDao,
                                                  final UUID notificationId,
                                                  final InternalCallContext context);

    /**
     * @return the name of that queue
     */
    public String getFullQName();

    /**
     * @return the service name associated to that queue
     */
    public String getServiceName();

    /**
     * @return the queue name associated
     */
    public String getQueueName();

    public NotificationQueueHandler getHandler();
}
