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

package com.ning.billing.account.dao;

import java.util.List;
import java.util.UUID;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Transaction;

import com.google.inject.Inject;
import com.ning.billing.account.api.IAccount;

public class AccountDao implements IAccountDao {

    private final IAccountDaoSql dao;

    @Inject
    public AccountDao(DBI dbi) {
        this.dao = dbi.onDemand(IAccountDaoSql.class);
    }

    @Override
    public void createAccount(IAccount account) {
        dao.insertAccount(account);
    }

    @Override
    public IAccount getAccountByKey(String key) {
        return dao.getAccountByKey(key);
    }

    @Override
    public IAccount getAccountFromId(UUID uid) {
        return dao.getAccountFromId(uid.toString());
    }

    @Override
    public List<IAccount> getAccounts() {
        return dao.getAccounts();
    }
}