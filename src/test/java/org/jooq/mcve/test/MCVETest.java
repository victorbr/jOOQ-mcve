/*
 * This work is dual-licensed
 * - under the Apache Software License 2.0 (the "ASL")
 * - under the jOOQ License and Maintenance Agreement (the "jOOQ License")
 * =============================================================================
 * You may choose which license applies to you:
 *
 * - If you're using this work with Open Source databases, you may choose
 *   either ASL or jOOQ License.
 * - If you're using this work with at least one commercial database, you must
 *   choose jOOQ License
 *
 * For more information, please visit http://www.jooq.org/licenses
 *
 * Apache Software License 2.0:
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * jOOQ License and Maintenance Agreement:
 * -----------------------------------------------------------------------------
 * Data Geekery grants the Customer the non-exclusive, timely limited and
 * non-transferable license to install and use the Software under the terms of
 * the jOOQ License and Maintenance Agreement.
 *
 * This library is distributed with a LIMITED WARRANTY. See the jOOQ License
 * and Maintenance Agreement for more details: http://www.jooq.org/licensing
 */
package org.jooq.mcve.test;

import static org.jooq.mcve.Tables.TEST;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;

import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.mcve.tables.records.TestRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MCVETest {

    Connection connection;
    DSLContext ctx;

    @Before
    public void setup() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:~/mcve", "sa", "");
        ctx = DSL.using(connection);
    }

    @After
    public void after() throws Exception {
        ctx = null;
        connection.close();
        connection = null;
    }

    @Test
    public void mcveTest() {
        TestRecord result =
        ctx.insertInto(TEST)
           .columns(TEST.VALUE, TEST.CHARVALUE)
           .values(42, "123q456")
           .returning(TEST.ID)
           .fetchOne();

        result.refresh();
        assertEquals(42, (int) result.getValue());

        DataType<Long> longDataType = SQLDataType.VARCHAR.asConvertedDataType(new StringToLongConverter());
        DataType<Integer> integerDataType = longDataType.asConvertedDataType(new LongToIntConverter());

        Field<Integer> charValueAsInteger = DSL.cast(TEST.CHARVALUE, integerDataType);
        Integer fetched = ctx.select(charValueAsInteger)
                .from(TEST)
                .where(TEST.ID.eq(result.value1()))
                .fetchOne(charValueAsInteger);
        assertEquals(1230456, fetched.intValue());
    }

    private static class StringToLongConverter implements Converter<String, Long> {
        @Override
        public Long from(String s) {
            if (s != null)
                return Long.parseLong(s.replaceAll("[^0-9]", "0"));
            else
                return null;
        }

        @Override
        public String to(Long aLong) {
            if (aLong != null)
                return aLong.toString();
            else
                return null;
        }

        @Override
        public Class<String> fromType() {
            return String.class;
        }

        @Override
        public Class<Long> toType() {
            return Long.class;
        }
    }

    private static class LongToIntConverter implements Converter<Long, Integer> {
        @Override
        public Integer from(Long aLong) {
            if (aLong != null)
                return aLong.intValue();
            else
                return null;
        }

        @Override
        public Long to(Integer integer) {
            if (integer != null)
                return integer.longValue();
            else
                return null;
        }

        @Override
        public Class<Long> fromType() {
            return Long.class;
        }

        @Override
        public Class<Integer> toType() {
            return Integer.class;
        }
    }
}
