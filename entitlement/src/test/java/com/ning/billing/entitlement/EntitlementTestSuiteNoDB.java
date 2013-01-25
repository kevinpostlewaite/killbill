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

package com.ning.billing.entitlement;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.ning.billing.GuicyKillbillTestSuiteNoDB;
import com.ning.billing.account.api.AccountData;
import com.ning.billing.account.api.BillCycleDay;
import com.ning.billing.api.TestApiListener;
import com.ning.billing.api.TestListenerStatus;
import com.ning.billing.catalog.DefaultCatalogService;
import com.ning.billing.catalog.api.BillingPeriod;
import com.ning.billing.catalog.api.Catalog;
import com.ning.billing.catalog.api.CatalogService;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.catalog.api.Duration;
import com.ning.billing.catalog.api.PhaseType;
import com.ning.billing.catalog.api.PlanPhaseSpecifier;
import com.ning.billing.entitlement.api.EntitlementService;
import com.ning.billing.entitlement.api.migration.EntitlementMigrationApi;
import com.ning.billing.entitlement.api.migration.EntitlementMigrationApi.EntitlementAccountMigration;
import com.ning.billing.entitlement.api.timeline.EntitlementTimelineApi;
import com.ning.billing.entitlement.api.transfer.EntitlementTransferApi;
import com.ning.billing.entitlement.api.user.EntitlementUserApi;
import com.ning.billing.entitlement.api.user.EntitlementUserApiException;
import com.ning.billing.entitlement.api.user.SubscriptionBundle;
import com.ning.billing.entitlement.api.user.SubscriptionData;
import com.ning.billing.entitlement.api.user.TestUtil;
import com.ning.billing.entitlement.api.user.TestUtil.EntitlementSubscriptionMigrationCaseWithCTD;
import com.ning.billing.entitlement.engine.core.Engine;
import com.ning.billing.entitlement.engine.dao.EntitlementDao;
import com.ning.billing.entitlement.engine.dao.MockEntitlementDaoMemory;
import com.ning.billing.entitlement.events.EntitlementEvent;
import com.ning.billing.entitlement.glue.MockEngineModuleMemory;
import com.ning.billing.mock.MockAccountBuilder;
import com.ning.billing.util.bus.DefaultBusService;
import com.ning.billing.util.clock.ClockMock;
import com.ning.billing.util.config.EntitlementConfig;
import com.ning.billing.util.events.EffectiveSubscriptionInternalEvent;
import com.ning.billing.util.svcapi.entitlement.EntitlementInternalApi;
import com.ning.billing.util.svcsapi.bus.BusService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import static org.testng.Assert.assertNotNull;

public class EntitlementTestSuiteNoDB extends GuicyKillbillTestSuiteNoDB  {


    protected static final Logger log = LoggerFactory.getLogger(EntitlementTestSuiteNoDB.class);

    @Inject
    protected EntitlementService entitlementService;
    @Inject
    protected EntitlementUserApi entitlementApi;
    @Inject
    protected EntitlementInternalApi entitlementInternalApi;
    @Inject
    protected EntitlementTransferApi transferApi;

    @Inject
    protected EntitlementMigrationApi migrationApi;
    @Inject
    protected EntitlementTimelineApi repairApi;

    @Inject
    protected CatalogService catalogService;
    @Inject
    protected EntitlementConfig config;
    @Inject
    protected EntitlementDao dao;
    @Inject
    protected ClockMock clock;
    @Inject
    protected BusService busService;

    @Inject
    protected TestUtil testUtil;
    @Inject
    protected TestApiListener testListener;
    @Inject
    protected TestListenerStatus testListenerStatus;

    protected Catalog catalog;
    protected AccountData accountData;
    protected SubscriptionBundle bundle;


    //
    // The date on which we make our test start; just to ensure that running tests at different dates does not
    // produce different results. nothing specific about that date; we could change it to anything.
    //
    protected DateTime testStartDate = new DateTime(2012, 5, 7, 0, 3, 42, 0);

    public static void loadSystemPropertiesFromClasspath(final String resource) {
        final URL url = EntitlementTestSuiteNoDB.class.getResource(resource);
        assertNotNull(url);

        try {
            System.getProperties().load(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        try {
            ((DefaultBusService) busService).stopBus();
        } catch (Exception e) {
            log.warn("Failed to tearDown test properly ", e);
        }
    }


    @BeforeClass(alwaysRun = true)
    public void setup() throws Exception {
        loadSystemPropertiesFromClasspath("/entitlement.properties");
        final Injector g = Guice.createInjector(Stage.PRODUCTION, new MockEngineModuleMemory());

        g.injectMembers(this);

        /*
        entitlementService = g.getInstance(EntitlementService.class);
        entitlementApi = g.getInstance(Key.get(EntitlementUserApi.class, RealImplementation.class));
        entitlementInternalApi = g.getInstance(EntitlementInternalApi.class);
        migrationApi = g.getInstance(EntitlementMigrationApi.class);
        repairApi = g.getInstance(EntitlementTimelineApi.class);
        transferApi = g.getInstance(EntitlementTransferApi.class);
        catalogService = g.getInstance(CatalogService.class);
        busService = g.getInstance(BusService.class);
        config = g.getInstance(EntitlementConfig.class);
        dao = g.getInstance(EntitlementDao.class);
        clock = (ClockMock) g.getInstance(Clock.class);
        */
        init();
    }

    private void init() throws Exception {
        ((DefaultCatalogService) catalogService).loadCatalog();

        final BillCycleDay billCycleDay = Mockito.mock(BillCycleDay.class);
        Mockito.when(billCycleDay.getDayOfMonthUTC()).thenReturn(1);
        accountData = new MockAccountBuilder().name(UUID.randomUUID().toString())
                                              .firstNameLength(6)
                                              .email(UUID.randomUUID().toString())
                                              .phone(UUID.randomUUID().toString())
                                              .migrated(false)
                                              .isNotifiedForInvoices(false)
                                              .externalKey(UUID.randomUUID().toString())
                                              .billingCycleDay(billCycleDay)
                                              .currency(Currency.USD)
                                              .paymentMethodId(UUID.randomUUID())
                                              .timeZone(DateTimeZone.forID("Europe/Paris"))
                                              .build();

        assertNotNull(accountData);
        catalog = catalogService.getFullCatalog();
        assertNotNull(catalog);
    }


    @BeforeMethod(alwaysRun = true)
    public void setupTest() throws Exception {
        log.warn("RESET TEST FRAMEWORK");

        // CLEANUP ALL DB TABLES OR IN MEMORY STRUCTURES
        ((MockEntitlementDaoMemory) dao).reset();

        // RESET LIST OF EXPECTED EVENTS
        if (testListener != null) {
            testListener.reset();
            testListenerStatus.resetTestListenerStatus();
        }

        // RESET CLOCK
        clock.resetDeltaFromReality();

        // START BUS AND REGISTER LISTENER
        busService.getBus().start();
        busService.getBus().register(testListener);

        // START NOTIFICATION QUEUE FOR ENTITLEMENT
        ((Engine) entitlementService).initialize();
        ((Engine) entitlementService).start();

        // SETUP START DATE
        clock.setDeltaFromReality(testStartDate.getMillis() - clock.getUTCNow().getMillis());

        // CREATE NEW BUNDLE FOR TEST
        final UUID accountId = UUID.randomUUID();
        bundle = entitlementApi.createBundleForAccount(accountId, "myDefaultBundle", callContext);
        assertNotNull(bundle);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupTest() throws Exception {
        // UNREGISTER TEST LISTENER AND STOP BUS
        busService.getBus().unregister(testListener);
        busService.getBus().stop();

        // STOP NOTIFICATION QUEUE
        ((Engine) entitlementService).stop();

        log.warn("DONE WITH TEST");
    }

    protected void assertListenerStatus() {
        ((EntitlementTestListenerStatus) testListenerStatus).assertListenerStatus();
    }

    protected SubscriptionData createSubscription(final String productName, final BillingPeriod term, final String planSet, final DateTime requestedDate)
            throws EntitlementUserApiException {
        return testUtil.createSubscriptionWithBundle(bundle.getId(), productName, term, planSet, requestedDate);
    }

    protected SubscriptionData createSubscription(final String productName, final BillingPeriod term, final String planSet)
            throws EntitlementUserApiException {
        return testUtil.createSubscriptionWithBundle(bundle.getId(), productName, term, planSet, null);
    }

    protected SubscriptionData createSubscriptionWithBundle(final UUID bundleId, final String productName, final BillingPeriod term, final String planSet, final DateTime requestedDate)
            throws EntitlementUserApiException {
        return testUtil.createSubscriptionWithBundle(bundleId, productName, term, planSet, requestedDate);
    }

    protected void checkNextPhaseChange(final SubscriptionData subscription, final int expPendingEvents, final DateTime expPhaseChange) {

        testUtil.checkNextPhaseChange(subscription, expPendingEvents, expPhaseChange);
    }

    protected void assertDateWithin(final DateTime in, final DateTime lower, final DateTime upper) {
        testUtil.assertDateWithin(in, lower, upper);
    }

    protected Duration getDurationDay(final int days) {
        return testUtil.getDurationDay(days);
    }

    protected Duration getDurationMonth(final int months) {
        return testUtil.getDurationMonth(months);
    }

    protected Duration getDurationYear(final int years) {
        return testUtil.getDurationYear(years);
    }

    protected PlanPhaseSpecifier getProductSpecifier(final String productName, final String priceList,
                                                     final BillingPeriod term,
                                                     @Nullable final PhaseType phaseType) {
        return testUtil.getProductSpecifier(productName, priceList, term, phaseType);
    }

    protected void printEvents(final List<EntitlementEvent> events) {
        testUtil.printEvents(events);
    }

    protected void printSubscriptionTransitions(final List<EffectiveSubscriptionInternalEvent> transitions) {
        testUtil.printSubscriptionTransitions(transitions);
    }

    /**
     * ***********************************************************
     * Utilities for migration tests
     * *************************************************************
     */

    protected EntitlementAccountMigration createAccountForMigrationTest(final List<List<EntitlementSubscriptionMigrationCaseWithCTD>> cases) {
        return testUtil.createAccountForMigrationTest(cases);
    }

    protected EntitlementAccountMigration createAccountForMigrationWithRegularBasePlanAndAddons(final DateTime initialBPstart, final DateTime initalAddonStart) {
        return testUtil.createAccountForMigrationWithRegularBasePlanAndAddons(initialBPstart, initalAddonStart);
    }

    protected EntitlementAccountMigration createAccountForMigrationWithRegularBasePlan(final DateTime startDate) {
        return testUtil.createAccountForMigrationWithRegularBasePlan(startDate);
    }

    protected EntitlementAccountMigration createAccountForMigrationWithRegularBasePlanFutreCancelled(final DateTime startDate) {
        return testUtil.createAccountForMigrationWithRegularBasePlanFutreCancelled(startDate);
    }

    protected EntitlementAccountMigration createAccountForMigrationFuturePendingPhase(final DateTime trialDate) {
        return testUtil.createAccountForMigrationFuturePendingPhase(trialDate);
    }

    protected EntitlementAccountMigration createAccountForMigrationFuturePendingChange() {
        return testUtil.createAccountForMigrationFuturePendingChange();
    }
}
