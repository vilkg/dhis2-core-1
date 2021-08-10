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

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.event.data.aggregated.sql.transform.FunctionXt;

/**
 * @author Dusan Bernat
 */
public class SqlEligibleForTransformationValueProvider
{
    public FunctionXt<String, Boolean> getProvider()
    {
        return sqlStatement -> {

            SqlSelectStatementReminderProvider sqlSelectStatementReminderProvider = new SqlSelectStatementReminderProvider();

            Pair<String, String> selectAndRemainder = sqlSelectStatementReminderProvider.getProvider()
                .apply( sqlStatement );

            List<Boolean> checkList = new ArrayList<>();

            Statement select = CCJSqlParserUtil.parse( selectAndRemainder.getLeft() );
            select.accept( new StatementVisitorAdapter()
            {
                @Override
                public void visit( Select select )
                {
                    select.getSelectBody().accept( new SelectVisitorAdapter()
                    {
                        @Override
                        public void visit( PlainSelect plainSelect )
                        {
                            checkMainSelect( checkList, plainSelect );
                            checkMainWhere( checkList, plainSelect );
                        }
                    } );
                }
            } );

            return checkList.stream().allMatch( b -> b );
        };
    }

    private static void checkMainSelect( List<Boolean> checkList, PlainSelect plainSelect )
    {
        plainSelect.getSelectItems().forEach( i -> i.accept( new SelectItemVisitorAdapter()
        {
            @Override
            public void visit( SelectExpressionItem item )
            {
                MutableBoolean countChecked = new MutableBoolean();
                countChecked.setTrue();

                MutableBoolean quotedValueChecked = new MutableBoolean();
                quotedValueChecked.setTrue();

                item.getExpression().accept( new ExpressionVisitorAdapter()
                {
                    @Override
                    public void visit( net.sf.jsqlparser.expression.Function function )
                    {
                        if ( !"count".equalsIgnoreCase( function.getName() ) )
                        {
                            countChecked.setFalse();
                        }
                    }

                    @Override
                    public void visit( StringValue value )
                    {
                        if ( value.toString().isEmpty() ||
                            !"'".equals( value.toString().substring( 0, 1 ) ) ||
                            !"'".equals( value.toString().substring( value.toString().length() - 1 ) ) )
                        {
                            quotedValueChecked.setFalse();
                        }
                    }
                } );

                checkList.add( countChecked.getValue() );
                checkList.add( quotedValueChecked.getValue() );
            }
        } ) );
    }

    private static void checkMainWhere( List<Boolean> checkList, PlainSelect plainSelect )
    {
        MutableBoolean subSelectChecked = new MutableBoolean();
        subSelectChecked.setFalse();

        plainSelect.getWhere().accept( new ExpressionVisitorAdapter()
        {
            @Override
            public void visit( SubSelect subSelect )
            {
                subSelectChecked.setTrue();
            }
        } );
        checkList.add( subSelectChecked.getValue() );
    }
}