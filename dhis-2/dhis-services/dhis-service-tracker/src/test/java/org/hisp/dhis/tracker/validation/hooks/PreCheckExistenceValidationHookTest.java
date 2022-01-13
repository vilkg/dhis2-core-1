/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1002;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1030;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1032;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1063;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1080;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1081;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1082;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1113;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1114;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4015;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationReport.hasError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Enrico Colasante
 */
class PreCheckExistenceValidationHookTest
{

    private PreCheckExistenceValidationHook validationHook;

    private final static String SOFT_DELETED_TEI_UID = "SoftDeletedTEIId";

    private final static String TEI_UID = "TEIId";

    private final static String NOT_PRESENT_TEI_UID = "NotPresentTEIId";

    private final static String SOFT_DELETED_ENROLLMENT_UID = "SoftDeletedEnrollmentId";

    private final static String ENROLLMENT_UID = "EnrollmentId";

    private final static String NOT_PRESENT_ENROLLMENT_UID = "NotPresentEnrollmentId";

    private final static String SOFT_DELETED_EVENT_UID = "SoftDeletedEventId";

    private final static String EVENT_UID = "EventId";

    private final static String NOT_PRESENT_EVENT_UID = "NotPresentEventId";

    private final static String NOT_PRESENT_RELATIONSHIP_UID = "NotPresentRelationshipId";

    private final static String RELATIONSHIP_UID = "RelationshipId";

    private TrackerBundle bundle;

    private TrackerImportValidationContext ctx;

    private TrackerValidationReport report;

    @BeforeEach
    public void setUp()
    {
        ctx = mock( TrackerImportValidationContext.class );
        bundle = TrackerBundle.builder().build();
        report = new TrackerValidationReport();

        validationHook = new PreCheckExistenceValidationHook();
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsCreateAndTeiIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.CREATE );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenTeiIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.CREATE_AND_UPDATE );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationSuccessWhenIsUpdate()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsSoftDeleted()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( SOFT_DELETED_TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getTrackedEntityInstance( SOFT_DELETED_TEI_UID ) ).thenReturn( getSoftDeletedTei() );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        hasError( report, E1114, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsCreateAndTEIIsAlreadyPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.CREATE );
        when( ctx.getTrackedEntityInstance( TEI_UID ) ).thenReturn( getTei() );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        hasError( report, E1002, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenIsUpdateAndTEIIsNotPresent()
    {
        // given
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( NOT_PRESENT_TEI_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( trackedEntity, TrackerImportStrategy.UPDATE );

        validationHook.validateTrackedEntity( report, ctx, trackedEntity );

        // then
        hasError( report, E1063, TRACKED_ENTITY, trackedEntity.getUid() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE_AND_UPDATE );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsUpdate()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsSoftDeleted()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( SOFT_DELETED_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgramInstance( SOFT_DELETED_ENROLLMENT_UID ) ).thenReturn( getSoftDeletedEnrollment() );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        hasError( report, E1113, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE );
        when( ctx.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        hasError( report, E1080, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( enrollment, TrackerImportStrategy.UPDATE );

        validationHook.validateEnrollment( report, ctx, enrollment );

        // then
        hasError( report, E1081, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsCreateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.CREATE );

        validationHook.validateEvent( report, ctx, event );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.CREATE_AND_UPDATE );

        validationHook.validateEvent( report, ctx, event );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEventValidationSuccessWhenIsUpdate()
    {
        // given
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );

        validationHook.validateEvent( report, ctx, event );

        // then
        assertFalse( report.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsWhenIsSoftDeleted()
    {
        // given
        Event event = Event.builder()
            .event( SOFT_DELETED_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.CREATE_AND_UPDATE );
        when( ctx.getProgramStageInstance( SOFT_DELETED_EVENT_UID ) ).thenReturn( getSoftDeletedEvent() );

        validationHook.validateEvent( report, ctx, event );

        // then
        hasError( report, E1082, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsCreateAndEventIsAlreadyPresent()
    {
        // given
        Event event = Event.builder()
            .event( EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.CREATE );
        when( ctx.getProgramStageInstance( EVENT_UID ) ).thenReturn( getEvent() );

        validationHook.validateEvent( report, ctx, event );

        // then
        hasError( report, E1030, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsWhenIsUpdateAndEventIsNotPresent()
    {
        // given
        Event event = Event.builder()
            .event( NOT_PRESENT_EVENT_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( event, TrackerImportStrategy.UPDATE );

        validationHook.validateEvent( report, ctx, event );

        // then
        hasError( report, E1032, EVENT, event.getUid() );
    }

    @Test
    void verifyRelationshipValidationSuccessWhenIsCreate()
    {
        // given
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        // when
        when( ctx.getBundle() ).thenReturn( bundle );
        bundle.setStrategy( rel, TrackerImportStrategy.CREATE_AND_UPDATE );

        validationHook.validateRelationship( report, ctx, rel );

        // then
        assertFalse( report.hasErrors() );
        assertThat( report.getWarnings(), empty() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenUpdate()
    {
        // given
        Relationship rel = getPayloadRelationship();

        // when
        when( ctx.getRelationship( getPayloadRelationship() ) ).thenReturn( getRelationship() );

        bundle.setStrategy( rel, TrackerImportStrategy.CREATE_AND_UPDATE );

        validationHook.validateRelationship( report, ctx, rel );

        // then
        assertFalse( report.hasErrors() );
        assertTrue( report.hasWarning( r -> E4015.equals( r.getWarningCode() ) &&
            TrackerType.RELATIONSHIP.equals( r.getTrackerType() ) &&
            rel.getUid().equals( r.getUid() ) ) );
    }

    private TrackedEntityInstance getSoftDeletedTei()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( SOFT_DELETED_TEI_UID );
        trackedEntityInstance.setDeleted( true );
        return trackedEntityInstance;
    }

    private TrackedEntityInstance getTei()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( TEI_UID );
        trackedEntityInstance.setDeleted( false );
        return trackedEntityInstance;
    }

    private ProgramInstance getSoftDeletedEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( SOFT_DELETED_ENROLLMENT_UID );
        programInstance.setDeleted( true );
        return programInstance;
    }

    private ProgramInstance getEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_UID );
        programInstance.setDeleted( false );
        return programInstance;
    }

    private ProgramStageInstance getSoftDeletedEvent()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( SOFT_DELETED_EVENT_UID );
        programStageInstance.setDeleted( true );
        return programStageInstance;
    }

    private ProgramStageInstance getEvent()
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( EVENT_UID );
        programStageInstance.setDeleted( false );
        return programStageInstance;
    }

    private Relationship getPayloadRelationship()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_UID )
            .build();
    }

    private org.hisp.dhis.relationship.Relationship getRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( EVENT_UID );
        return relationship;
    }
}