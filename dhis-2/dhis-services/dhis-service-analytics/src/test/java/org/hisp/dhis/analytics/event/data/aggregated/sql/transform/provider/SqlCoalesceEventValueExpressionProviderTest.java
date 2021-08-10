/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.event.data.aggregated.sql.transform.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.event.data.aggregated.sql.transform.model.element.where.PredicateElement;
import org.junit.Test;

public class SqlCoalesceEventValueExpressionProviderTest extends DhisSpringTest
{
    private String sqlWithCoalesce;

    private String sqlWithoutCoalesce;

    @Override
    public void setUpTest()
    {
        sqlWithCoalesce = "select count(pi) as value \n" +
            "from analytics_enrollment_uyjxktbwrnf as ax \n" +
            "where cast((select \"PFXeJV8d7ja\" \n" +
            "from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi \n" +
            "and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n" +
            "order by executiondate desc limit 1 ) as date) < cast( '2022-01-01' as date )\n" +
            "and cast((select \"PFXeJV8d7ja\" \n" +
            "from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n" +
            "order by executiondate desc limit 1 ) as date) >= cast( '2021-01-01' as date )\n" +
            "and (uidlevel1 = 'VGTTybr8UcS' ) \n" +
            "and (((select count(\"ovY6E8BSdto\") \n" +
            "from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"ovY6E8BSdto\" is not null and \"ovY6E8BSdto\" = 'Positive' and ps = 'dDHkBd3X8Ce') > 0) \n"
            +
            "and coalesce((select \"bOYWVEBaWy6\" \n" +
            "from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"bOYWVEBaWy6\" is not null and ps = 'iVfs6Jyp7I8' \n" +
            "order by executiondate desc limit 1 ),'') = 'Recovered') limit 100001";

        sqlWithoutCoalesce = "select count(pi) as value,'20200201' as Daily \n" +
            "from analytics_enrollment_uyjxktbwrnf as ax where \n" +
            "cast((select \"PFXeJV8d7ja\" from analytics_event_uYjxkTbwRNf \n" +
            "\t  where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n"
            +
            "\t  order by executiondate desc limit 1 ) as date) < cast( '2020-02-02' as date )and \n" +
            "cast((select \"PFXeJV8d7ja\" from analytics_event_uYjxkTbwRNf \n" +
            "\t  where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n"
            +
            "\t  order by executiondate desc limit 1 ) as date) >= cast( '2020-02-01' as date )\n" +
            "and (uidlevel1 = 'VGTTybr8UcS' ) \n" +
            "and (((select count(\"ovY6E8BSdto\") \n" +
            "\t   from analytics_event_uYjxkTbwRNf \n" +
            "\t   where analytics_event_uYjxkTbwRNf.pi = ax.pi \n" +
            "\t   and \"ovY6E8BSdto\" is not null \n" +
            "\t   and \"ovY6E8BSdto\" = 'Positive' \n" +
            "\t   and ps = 'dDHkBd3X8Ce') > 0)) limit 100001";
    }

    @Test
    public void verifySqlCoalesceEventValueExpressionWithCoalesceFunction()
    {
        SqlCoalesceEventValueExpressionProvider provider = new SqlCoalesceEventValueExpressionProvider();

        List<PredicateElement> predicateElementList = provider.getProvider().apply( sqlWithCoalesce );

        assertEquals( 1, predicateElementList.size() );

        assertEquals( "bOYWVEBaWy6.\"bOYWVEBaWy6\"", predicateElementList.get( 0 ).getLeftExpression() );

        assertEquals( "=", predicateElementList.get( 0 ).getRelation() );

        assertEquals( "'Recovered'", predicateElementList.get( 0 ).getRightExpression() );

        assertEquals( "and", predicateElementList.get( 0 ).getLogicalOperator() );
    }

    @Test
    public void verifySqlCoalesceEventValueExpressionWithNoCoalesceFunction()
    {
        SqlCoalesceEventValueExpressionProvider provider = new SqlCoalesceEventValueExpressionProvider();

        List<PredicateElement> predicateElementList = provider.getProvider().apply( sqlWithoutCoalesce );

        assertEquals( 0, predicateElementList.size() );
    }
}