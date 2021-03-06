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

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ning.billing.util.UtilTestSuiteWithEmbeddedDB;
import com.ning.billing.util.clock.ClockMock;
import com.ning.billing.util.entity.dao.EntitySqlDao;
import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionWrapper;
import com.ning.billing.util.entity.dao.EntitySqlDaoTransactionalJdbiWrapper;
import com.ning.billing.util.entity.dao.EntitySqlDaoWrapperFactory;
import com.ning.billing.util.notificationq.NotificationQueueService.NotificationQueueHandler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.testng.Assert.assertEquals;

public class TestNotificationQueue extends UtilTestSuiteWithEmbeddedDB {

    private final Logger log = LoggerFactory.getLogger(TestNotificationQueue.class);

    private EntitySqlDaoTransactionalJdbiWrapper entitySqlDaoTransactionalJdbiWrapper;

    private int eventsReceived;

    private static final class TestNotificationKey implements NotificationKey, Comparable<TestNotificationKey> {

        private final String value;

        @JsonCreator
        public TestNotificationKey(@JsonProperty("value") final String value) {
            super();
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int compareTo(TestNotificationKey arg0) {
            return value.compareTo(arg0.value);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }
    }

    @Override
    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        super.beforeClass();
        entitySqlDaoTransactionalJdbiWrapper = new EntitySqlDaoTransactionalJdbiWrapper(getDBI(), clock, cacheControllerDispatcher, nonEntityDao);
    }

    @Override
    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        // Reset time to real value
        clock.resetDeltaFromReality();
        eventsReceived = 0;
    }

    /**
     * Test that we can post a notification in the future from a transaction and get the notification
     * callback with the correct key when the time is ready
     *
     * @throws Exception
     */
    @Test(groups = "slow")
    public void testSimpleNotification() throws Exception {

        final Map<NotificationKey, Boolean> expectedNotifications = new TreeMap<NotificationKey, Boolean>();

        final NotificationQueue queue = queueService.createNotificationQueue("test-svc",
                                                                             "foo",
                                                                             new NotificationQueueHandler() {
                                                                                 @Override
                                                                                 public void handleReadyNotification(final NotificationKey notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                                                                                     synchronized (expectedNotifications) {
                                                                                         log.info("Handler received key: " + notificationKey);

                                                                                         expectedNotifications.put(notificationKey, Boolean.TRUE);
                                                                                         expectedNotifications.notify();
                                                                                     }
                                                                                 }
                                                                             });

        queue.startQueue();

        final UUID key = UUID.randomUUID();
        final DummyObject obj = new DummyObject("foo", key);
        final DateTime now = new DateTime();
        final DateTime readyTime = now.plusMillis(2000);
        final NotificationKey notificationKey = new TestNotificationKey(key.toString());

        expectedNotifications.put(notificationKey, Boolean.FALSE);

        // Insert dummy to be processed in 2 sec'
        entitySqlDaoTransactionalJdbiWrapper.execute(new EntitySqlDaoTransactionWrapper<Void>()

        {
            @Override
            public Void inTransaction(
                    final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws
                                                                                               Exception {

                entitySqlDaoWrapperFactory.transmogrify(DummySqlTest.class).insertDummy(obj);
                queue.recordFutureNotificationFromTransaction(entitySqlDaoWrapperFactory, readyTime, notificationKey, internalCallContext);
                log.info("Posted key: " + notificationKey);

                return null;
            }
        }

                                                    );

        // Move time in the future after the notification effectiveDate
        ((ClockMock) clock).

                                   setDeltaFromReality(3000);

        // Notification should have kicked but give it at least a sec' for thread scheduling
        await()

                .

                        atMost(1, MINUTES)

                .

                        until(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws
                                                  Exception {
                                return expectedNotifications.get(notificationKey);
                            }
                        }

                             );

        queue.stopQueue();
        Assert.assertTrue(expectedNotifications.get(notificationKey));
    }

    @Test(groups = "slow")
    public void testManyNotifications() throws Exception {
        final Map<NotificationKey, Boolean> expectedNotifications = new TreeMap<NotificationKey, Boolean>();

        final NotificationQueue queue = queueService.createNotificationQueue("test-svc",
                                                                             "many",
                                                                             new NotificationQueueHandler() {
                                                                                 @Override
                                                                                 public void handleReadyNotification(final NotificationKey notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                                                                                     synchronized (expectedNotifications) {
                                                                                         log.info("Handler received key: " + notificationKey.toString());

                                                                                         expectedNotifications.put(notificationKey, Boolean.TRUE);
                                                                                         expectedNotifications.notify();
                                                                                     }
                                                                                 }
                                                                             });
        queue.startQueue();

        final DateTime now = clock.getUTCNow();
        final int MAX_NOTIFICATIONS = 100;
        for (int i = 0; i < MAX_NOTIFICATIONS; i++) {

            final int nextReadyTimeIncrementMs = 1000;

            final UUID key = UUID.randomUUID();
            final DummyObject obj = new DummyObject("foo", key);
            final int currentIteration = i;

            final NotificationKey notificationKey = new TestNotificationKey(new Integer(i).toString());
            expectedNotifications.put(notificationKey, Boolean.FALSE);

            entitySqlDaoTransactionalJdbiWrapper.execute(new EntitySqlDaoTransactionWrapper<Void>() {
                @Override
                public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {

                    entitySqlDaoWrapperFactory.transmogrify(DummySqlTest.class).insertDummy(obj);
                    queue.recordFutureNotificationFromTransaction(entitySqlDaoWrapperFactory, now.plus((currentIteration + 1) * nextReadyTimeIncrementMs),
                                                                  notificationKey, internalCallContext);
                    return null;
                }
            });

            // Move time in the future after the notification effectiveDate
            if (i == 0) {
                ((ClockMock) clock).setDeltaFromReality(nextReadyTimeIncrementMs);
            } else {
                ((ClockMock) clock).addDeltaFromReality(nextReadyTimeIncrementMs);
            }
        }

        // Wait a little longer since there are a lot of callback that need to happen
        int nbTry = MAX_NOTIFICATIONS + 1;
        boolean success = false;
        do {
            synchronized (expectedNotifications) {
                final Collection<Boolean> completed = Collections2.filter(expectedNotifications.values(), new Predicate<Boolean>() {
                    @Override
                    public boolean apply(final Boolean input) {
                        return input;
                    }
                });

                if (completed.size() == MAX_NOTIFICATIONS) {
                    success = true;
                    break;
                }
                log.info(String.format("BEFORE WAIT : Got %d notifications at time %s (real time %s)", completed.size(), clock.getUTCNow(), new DateTime()));
                expectedNotifications.wait(1000);
            }
        } while (nbTry-- > 0);

        queue.stopQueue();
        log.info("STEPH GOT SIZE " + Collections2.filter(expectedNotifications.values(), new Predicate<Boolean>() {
            @Override
            public boolean apply(final Boolean input) {
                return input;
            }
        }).size());
        assertEquals(success, true);
    }

    /**
     * Test that we can post a notification in the future from a transaction and get the notification
     * callback with the correct key when the time is ready
     *
     * @throws Exception
     */
    @Test(groups = "slow")
    public void testMultipleHandlerNotification() throws Exception {
        final Map<NotificationKey, Boolean> expectedNotificationsFred = new TreeMap<NotificationKey, Boolean>();
        final Map<NotificationKey, Boolean> expectedNotificationsBarney = new TreeMap<NotificationKey, Boolean>();

        final NotificationQueue queueFred = queueService.createNotificationQueue("UtilTest", "Fred", new NotificationQueueHandler() {
            @Override
            public void handleReadyNotification(final NotificationKey notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                log.info("Fred received key: " + notificationKey);
                expectedNotificationsFred.put(notificationKey, Boolean.TRUE);
                eventsReceived++;
            }
        });

        final NotificationQueue queueBarney = queueService.createNotificationQueue("UtilTest", "Barney", new NotificationQueueHandler() {
            @Override
            public void handleReadyNotification(final NotificationKey notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                log.info("Barney received key: " + notificationKey);
                expectedNotificationsBarney.put(notificationKey, Boolean.TRUE);
                eventsReceived++;
            }
        });
        queueFred.startQueue();
        //		We don't start Barney so it can never pick up notifications

        final UUID key = UUID.randomUUID();
        final DummyObject obj = new DummyObject("foo", key);
        final DateTime now = new DateTime();
        final DateTime readyTime = now.plusMillis(2000);
        final NotificationKey notificationKeyFred = new TestNotificationKey("Fred");

        final NotificationKey notificationKeyBarney = new TestNotificationKey("Barney");

        expectedNotificationsFred.put(notificationKeyFred, Boolean.FALSE);
        expectedNotificationsFred.put(notificationKeyBarney, Boolean.FALSE);

        // Insert dummy to be processed in 2 sec'
        entitySqlDaoTransactionalJdbiWrapper.execute(new EntitySqlDaoTransactionWrapper<Void>() {
            @Override
            public Void inTransaction(final EntitySqlDaoWrapperFactory<EntitySqlDao> entitySqlDaoWrapperFactory) throws Exception {
                entitySqlDaoWrapperFactory.transmogrify(DummySqlTest.class).insertDummy(obj);

                queueFred.recordFutureNotificationFromTransaction(entitySqlDaoWrapperFactory, readyTime, notificationKeyFred, internalCallContext);
                log.info("posted key: " + notificationKeyFred.toString());
                queueBarney.recordFutureNotificationFromTransaction(entitySqlDaoWrapperFactory, readyTime, notificationKeyBarney, internalCallContext);
                log.info("posted key: " + notificationKeyBarney.toString());
                return null;
            }
        });

        // Move time in the future after the notification effectiveDate
        ((ClockMock) clock).setDeltaFromReality(3000);

        // Note the timeout is short on this test, but expected behaviour is that it times out.
        // We are checking that the Fred queue does not pick up the Barney event
        try {
            await().atMost(5, TimeUnit.SECONDS).until(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return eventsReceived >= 2;
                }
            });
            Assert.fail("There should only have been one event for the queue to pick up - it got more than that");
        } catch (Exception e) {
            // expected behavior
        }

        queueFred.stopQueue();
        Assert.assertTrue(expectedNotificationsFred.get(notificationKeyFred));
        Assert.assertFalse(expectedNotificationsFred.get(notificationKeyBarney));
    }
}
