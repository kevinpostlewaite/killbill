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

package com.ning.billing.payment;

import static org.testng.Assert.assertNotNull;

import java.util.UUID;

import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.ning.billing.account.api.Account;
import com.ning.billing.invoice.api.Invoice;
import com.ning.billing.payment.api.InvoicePayment;
import com.ning.billing.payment.setup.PaymentTestModule;

@Guice(modules = PaymentTestModule.class)
public class TestNotifyInvoicePaymentApi extends TestPaymentProvider {

    @Test
    public void testNotifyPaymentSuccess() {
        final Account account = createTestAccount();
        final Invoice invoice = createTestInvoice(account);

        PaymentAttempt paymentAttempt = new PaymentAttempt(UUID.randomUUID(), invoice);
        invoicePaymentApi.paymentSuccessful(invoice.getId(),
                                     invoice.getAmountOutstanding(),
                                     invoice.getCurrency(),
                                     paymentAttempt.getPaymentAttemptId(),
                                     paymentAttempt.getPaymentAttemptDate());

        InvoicePayment invoicePayment = invoicePaymentApi.getInvoicePayment(invoice.getId(), paymentAttempt.getPaymentAttemptId());

        assertNotNull(invoicePayment);
    }

    @Test
    public void testNotifyPaymentFailure() {
        final Account account = createTestAccount();
        final Invoice invoice = createTestInvoice(account);

        PaymentAttempt paymentAttempt = new PaymentAttempt(UUID.randomUUID(), invoice);
        invoicePaymentApi.paymentFailed(invoice.getId(),
                                 paymentAttempt.getPaymentAttemptId(),
                                 paymentAttempt.getPaymentAttemptDate());

        InvoicePayment invoicePayment = invoicePaymentApi.getInvoicePayment(invoice.getId(), paymentAttempt.getPaymentAttemptId());

        assertNotNull(invoicePayment);
    }

}