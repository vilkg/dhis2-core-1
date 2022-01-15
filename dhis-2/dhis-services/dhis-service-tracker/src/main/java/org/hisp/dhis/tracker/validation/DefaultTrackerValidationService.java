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
package org.hisp.dhis.tracker.validation;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.Timing;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrackerValidationService
    implements TrackerValidationService
{

    @Qualifier( "validationHooks" )
    private final List<TrackerValidationHook> validationHooks;

    @Qualifier( "ruleEngineValidationHooks" )
    private final List<TrackerValidationHook> ruleEngineValidationHooks;

    @Override
    public TrackerValidationReport validate( TrackerBundle bundle )
    {
        return validate( bundle, validationHooks );
    }

    @Override
    public TrackerValidationReport validateRuleEngine( TrackerBundle bundle )
    {
        return validate( bundle, ruleEngineValidationHooks );
    }

    private TrackerValidationReport validate( TrackerBundle bundle, List<TrackerValidationHook> hooks )
    {
        TrackerValidationReport validationReport = new TrackerValidationReport();

        User user = bundle.getUser();

        if ( (user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return validationReport;
        }

        // Note that the bundle gets cloned internally, so the original bundle
        // is always available
        TrackerImportValidationContext context = new TrackerImportValidationContext( bundle );
        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        try
        {
            for ( TrackerValidationHook hook : hooks )
            {
                Timer hookTimer = Timer.startTimer();

                validate( reporter, context, hook );

                validationReport.addTiming( new Timing(
                    hook.getClass().getName(),
                    hookTimer.toString() ) );
            }
        }
        catch ( ValidationFailFastException e )
        {
            // exit early when in FAIL_FAST validation mode
        }
        validationReport
            .addErrors( reporter.getReportList() )
            .addWarnings( reporter.getWarningsReportList() );

        removeInvalidObjects( bundle, reporter );

        return validationReport;
    }

    /**
     * Delegating validate method, this delegates validation to the different
     * implementing hooks.
     *
     * @param context validation context
     */
    private void validate( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        TrackerValidationHook hook )
    {
        TrackerBundle bundle = context.getBundle();
        /*
         * Validate the bundle, by passing each Tracker entities collection to
         * the validation hooks. If a validation hook reports errors and has
         * 'removeOnError=true' the Tracker entity under validation will be
         * removed from the bundle.
         */

        hook.validate( reporter, context );
        validateTrackerDtos( reporter, context, hook,
            ( r, dto ) -> hook.validateTrackedEntity( r, (TrackedEntity) dto ), bundle.getTrackedEntities() );
        validateTrackerDtos( reporter, context, hook, ( r, dto ) -> hook.validateEnrollment( r, (Enrollment) dto ),
            bundle.getEnrollments() );
        validateTrackerDtos( reporter, context, hook, ( r, dto ) -> hook.validateEvent( r, (Event) dto ),
            bundle.getEvents() );
        validateTrackerDtos( reporter, context, hook, ( r, dto ) -> hook.validateRelationship( r, (Relationship) dto ),
            bundle.getRelationships() );
    }

    private void validateTrackerDtos( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        TrackerValidationHook hook,
        BiConsumer<ValidationErrorReporter, TrackerDto> validation,
        List<? extends TrackerDto> dtos )
    {
        Iterator<? extends TrackerDto> iter = dtos.iterator();
        while ( iter.hasNext() )
        {
            TrackerDto dto = iter.next();
            if ( hook.needsToRun( context.getStrategy( dto ) ) )
            {
                validation.accept( reporter, dto );
                if ( hook.removeOnError() && didNotPassValidation( reporter, dto.getUid() ) )
                {
                    iter.remove();
                }
            }
        }
    }

    private boolean didNotPassValidation( ValidationErrorReporter reporter, String uid )
    {
        return reporter.getReportList().stream().anyMatch( r -> r.getUid().equals( uid ) );
    }

    private void removeInvalidObjects( TrackerBundle bundle, ValidationErrorReporter reporter )
    {
        bundle.setEvents( bundle.getEvents().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setEnrollments( bundle.getEnrollments().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setTrackedEntities( bundle.getTrackedEntities().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setRelationships( bundle.getRelationships().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
    }
}
