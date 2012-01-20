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

package com.ning.billing.account.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ning.billing.catalog.api.Currency;
import com.ning.billing.util.customfield.CustomField;
import com.ning.billing.util.tag.Tag;

public class MockAccountUserApi implements AccountUserApi {
    private final CopyOnWriteArrayList<Account> accounts = new CopyOnWriteArrayList<Account>();

    public Account createAccount(UUID id,
                                 String externalKey,
                                 String email,
                                 String name,
                                 int firstNameLength,
                                 String phone,
                                 Currency currency,
                                 int billCycleDay,
                                 String paymentProviderName,
                                 BigDecimal balance) {
        Account result = new DefaultAccount(id, externalKey, email, name, firstNameLength, phone, currency, billCycleDay, paymentProviderName, balance);
        accounts.add(result);
        return result;
    }

    @Override
    public Account createAccount(AccountData data, List<CustomField> fields, List<Tag> tags) throws AccountApiException {
        Account result = new DefaultAccount(data);
        accounts.add(result);
        return result;
    }

    @Override
    public Account getAccountByKey(String key) {
        for (Account account : accounts) {
            if (key.equals(account.getExternalKey())) {
                return account;
            }
        }
        return null;
    }

    @Override
    public Account getAccountById(UUID uid) {
        for (Account account : accounts) {
            if (uid.equals(account.getId())) {
                return account;
            }
        }
        return null;
    }

    @Override
    public List<Account> getAccounts() {
        return new ArrayList<Account>(accounts);
    }

    @Override
    public UUID getIdFromKey(String externalKey) {
        for (Account account : accounts) {
            if (externalKey.equals(account.getExternalKey())) {
                return account.getId();
            }
        }
        return null;
    }

    @Override
    public void updateAccount(Account account) {
        throw new UnsupportedOperationException();
    }
}