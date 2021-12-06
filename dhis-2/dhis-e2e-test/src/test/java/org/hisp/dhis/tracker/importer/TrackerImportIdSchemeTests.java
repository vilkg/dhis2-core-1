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

package org.hisp.dhis.tracker.importer;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImportIdSchemeTests
    extends TrackerNtiApiTest
{
    private OrgUnitActions orgUnitActions;

    private String programId = Constants.TRACKER_PROGRAM_ID;

    private String programStageId = Constants.TRACKER_PROGRAM_STAGE_IDS[0];

    private String orgUnitId = Constants.ORG_UNIT_IDS[1];

    private static Stream<Arguments> idSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "ATTRIBUTE:wLFJWAiOPZY", "attributeValues.value[0]" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();

        loginActions.loginAsAdmin();
    }

    @MethodSource( "idSchemeArguments" )
    @ParameterizedTest
    public void shouldImportWithOrgUnitScheme( String ouScheme, String ouProperty )
    {
        String ou = orgUnitActions.get( orgUnitId ).validateStatus( 200 ).extractString( ouProperty );
        assertNotNull( ou, String.format( "Org unit property %s was not present.", ouProperty ) );

        JsonObject payload = new TeiDataBuilder()
            .buildWithEnrollmentAndEvent( Constants.TRACKED_ENTITY_TYPE, ou, programId,
                programStageId );

        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( payload, new QueryParamsBuilder().add( "orgUnitIdScheme=" + ouScheme ) )
            .validateSuccessfulImport();

        trackerActions.getTrackedEntity( response.extractImportedTeis().get( 0 ) )
            .validate().body( "orgUnit", equalTo( orgUnitId ) );
    }

    @MethodSource( "idSchemeArguments" )
    @ParameterizedTest
    public void shouldImportWithProgramScheme( String scheme, String property )
    {
        String programPropertyValue = programActions.get( programId ).extractString( property );
        assertNotNull( programPropertyValue, String.format( "Program property %s was not present.", property ) );

        JsonObject payload = new TeiDataBuilder()
            .buildWithEnrollmentAndEvent( Constants.TRACKED_ENTITY_TYPE, orgUnitId, programPropertyValue,
                programStageId );

        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( payload, new QueryParamsBuilder().add( "programIdScheme=" + scheme ).add( "async=false" ) )
            .validateSuccessfulImport();

        trackerActions.getEnrollment( response.extractImportedEnrollments().get( 0 ) )
            .validate()
            .statusCode( 200 )
            .body( "program", equalTo( programId ) );

    }
}
