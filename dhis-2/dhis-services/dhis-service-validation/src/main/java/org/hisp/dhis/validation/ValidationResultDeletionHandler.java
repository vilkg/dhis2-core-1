package org.hisp.dhis.validation;
/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 
 * @author Stian Sandvold
 */
@Component( "org.hisp.dhis.validation.ValidationResultDeletionHandler" )
public class ValidationResultDeletionHandler
    extends DeletionHandler
{

    private final ValidationResultService validationResultService;

    public ValidationResultDeletionHandler( ValidationResultService validationResultService )
    {
        checkNotNull( validationResultService );
        this.validationResultService = validationResultService;
    }

    @Override
    public String getClassName()
    {
        return ValidationResult.class.getSimpleName();
    }

    @Override
    public void deleteValidationRule( ValidationRule validationRule )
    {
        validationResultService.getAllValidationResults().forEach( validationResult ->
        {
            if ( validationResult.getValidationRule().equals( validationRule ) )
            {
                validationResultService.deleteValidationResult( validationResult );
            }
        } );
    }

    @Override
    public void deletePeriod( Period period )
    {
        validationResultService.getAllValidationResults().forEach( validationResult ->
        {
            if ( validationResult.getPeriod().equals( period ) )
            {
                validationResultService.deleteValidationResult( validationResult );
            }
        } );
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        validationResultService.getAllValidationResults().forEach( validationResult ->
        {
            if ( validationResult.getOrganisationUnit().equals( organisationUnit ) )
            {
                validationResultService.deleteValidationResult( validationResult );
            }
        } );
    }

    @Override
    public void deleteCategoryOptionCombo( CategoryOptionCombo dataElementCategoryOptionCombo )
    {
        validationResultService.getAllValidationResults().forEach( validationResult ->
        {
            if ( validationResult.getAttributeOptionCombo().equals( dataElementCategoryOptionCombo ) )
            {
                validationResultService.deleteValidationResult( validationResult );
            }
        } );
    }

    @Override
    public String allowDeleteValidationRule( ValidationRule validationRule )
    {
        for ( ValidationResult validationResult : validationResultService.getAllValidationResults() )
        {
            if ( validationResult.getValidationRule().equals( validationRule ) )
            {
                return ERROR;
            }
        }

        return null;
    }

}