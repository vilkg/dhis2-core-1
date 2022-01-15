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

import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface TrackerValidationHook
{
    default void validate( ValidationErrorReporter reporter, TrackerImportValidationContext context )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Event or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param event entity to validate
     */
    default void validateEvent( ValidationErrorReporter reporter, Event event )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Enrollment or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param enrollment entity to validate
     */
    default void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Relationship or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param relationship entity to validate
     */
    default void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == TrackedEntity or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param tei entity to validate
     */
    default void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
    }

    default boolean needsToRun( TrackerImportStrategy strategy )
    {
        return strategy != TrackerImportStrategy.DELETE;
    }

    /**
     * Signal the implementing Validator hook that, upon validation error, the
     * Tracker entity under validation must be removed from the payload.
     *
     */
    default boolean removeOnError()
    {
        return false;
    }

}
