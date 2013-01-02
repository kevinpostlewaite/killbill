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

import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.ning.billing.mock.glue.MockDbHelperModule;
import com.ning.billing.util.UtilTestSuiteWithEmbeddedDB;
import com.ning.billing.util.glue.CacheModule;
import com.ning.billing.util.glue.ClockModule;

@Guice(modules = {ClockModule.class, CacheModule.class, MockDbHelperModule.class} )
public class TestCache extends UtilTestSuiteWithEmbeddedDB {

    @Test(groups = "slow", enabled=false)
    public void test() {

    }
}
