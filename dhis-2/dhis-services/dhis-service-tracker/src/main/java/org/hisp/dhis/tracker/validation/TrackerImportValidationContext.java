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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;

import com.google.common.base.Preconditions;

// TODO is this class really needed? what is the purpose of this class and why aren't the two caches moved to preheat?
/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
public class TrackerImportValidationContext
{
    private Map<String, CategoryOptionCombo> eventCocCacheMap = new HashMap<>();

    private TrackerBundle bundle;

    public TrackerImportValidationContext( TrackerBundle bundle )
    {
        // Create a copy of the bundle
        this.bundle = bundle;
    }

    public TrackerBundle getBundle()
    {
        return this.bundle;
    }

    public void cacheEventCategoryOptionCombo( String key, CategoryOptionCombo categoryOptionCombo )
    {
        Preconditions.checkArgument( !StringUtils.isEmpty( key ),
            "Event Category Option Combo cache key 'event uid', can't be null or empty" );

        Preconditions.checkNotNull( categoryOptionCombo, "Event Category Option Combo can't be null or empty" );

        if ( !eventCocCacheMap.containsKey( key ) )
        {
            eventCocCacheMap.put( key, categoryOptionCombo );
        }
    }

    public CategoryOptionCombo getCachedEventCategoryOptionCombo( String key )
    {
        return eventCocCacheMap.get( key );
    }

    public OrganisationUnit getOrganisationUnit( String id )
    {
        return getBundle().getPreheat().get( OrganisationUnit.class, id );
    }

    public TrackedEntityInstance getTrackedEntityInstance( String id )
    {
        return getBundle().getPreheat().getTrackedEntity( getBundle().getIdentifier(), id );
    }

    public TrackedEntityAttribute getTrackedEntityAttribute( String id )
    {
        return getBundle().getPreheat().get( TrackedEntityAttribute.class, id );
    }

    public DataElement getDataElement( String id )
    {
        return getBundle().getPreheat().get( DataElement.class, id );
    }

    public TrackedEntityType getTrackedEntityType( String id )
    {
        return getBundle().getPreheat().get( TrackedEntityType.class, id );
    }

    public RelationshipType getRelationShipType( String id )
    {
        return getBundle().getPreheat().get( RelationshipType.class, id );
    }

    public Program getProgram( String id )
    {
        return getBundle().getPreheat().get( Program.class, id );
    }

    public ProgramInstance getProgramInstance( String id )
    {
        return getBundle().getPreheat().getEnrollment( getBundle().getIdentifier(), id );
    }

    public OrganisationUnit getOwnerOrganisationUnit( String teiUid, String programUid )
    {
        Map<String, TrackedEntityProgramOwnerOrgUnit> programOwner = getBundle().getPreheat().getProgramOwner()
            .get( teiUid );
        if ( programOwner == null || programOwner.get( programUid ) == null )
        {
            return null;
        }
        else
        {
            return programOwner.get( programUid ).getOrganisationUnit();
        }
    }

    public boolean programInstanceHasEvents( String programInstanceUid )
    {
        return getBundle().getPreheat().getProgramInstanceWithOneOrMoreNonDeletedEvent().contains( programInstanceUid );
    }

    public boolean programStageHasEvents( String programStageUid, String enrollmentUid )
    {
        return getBundle().getPreheat().getProgramStageWithEvents()
            .contains( Pair.of( programStageUid, enrollmentUid ) );
    }

    public Optional<TrackedEntityComment> getNote( String uid )
    {
        return getBundle().getPreheat().getNote( uid );
    }

    public ProgramStage getProgramStage( String id )
    {
        return getBundle().getPreheat().get( ProgramStage.class, id );
    }

    public ProgramStageInstance getProgramStageInstance( String event )
    {
        return getBundle().getPreheat().getEvent( getBundle().getIdentifier(), event );
    }

    public org.hisp.dhis.relationship.Relationship getRelationship( Relationship relationship )
    {
        return getBundle().getPreheat().getRelationship( getBundle().getIdentifier(), relationship );
    }

    public CategoryOptionCombo getCategoryOptionCombo( String id )
    {
        return getBundle().getPreheat().get( CategoryOptionCombo.class, id );
    }

    public CategoryOption getCategoryOption( String id )
    {
        return getBundle().getPreheat().get( CategoryOption.class, id );
    }

    public boolean usernameExists( String username )
    {
        return getBundle().getPreheat().getUsers().containsKey( username );
    }

    public FileResource getFileResource( String id )
    {
        return getBundle().getPreheat().get( FileResource.class, id );
    }

    public Optional<ReferenceTrackerEntity> getReference( String uid )
    {
        return getBundle().getPreheat().getReference( uid );
    }

    public TrackerIdentifierParams getIdentifiers()
    {
        return getBundle().getPreheat().getIdentifiers();
    }

    public Map<String, List<String>> getProgramWithOrgUnitsMap()
    {
        return getBundle().getPreheat().getProgramWithOrgUnitsMap();
    }
}
