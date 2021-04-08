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
package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_NONCE_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_UPGRADE_INSECURE_ENABLED;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class CspNonceFilter extends OncePerRequestFilter
{
    public static final String CSP_REQUEST_NONCE_ATTR_NAME = "cspRequestNonce";

    public static final String CSP_UPGRADE_INSECURE_REQUESTS_VALUE = "upgrade-insecure-requests;";

    private final boolean enabled;

    private final boolean upgradeInsecure;

    private final String allowedExternalHosts;

    public CspNonceFilter( DhisConfigurationProvider dhisConfig,
        DhisOidcProviderRepository dhisOidcProviderRepository )
    {
        this.enabled = dhisConfig.isEnabled( CSP_NONCE_ENABLED );
        this.upgradeInsecure = dhisConfig.isEnabled( CSP_UPGRADE_INSECURE_ENABLED );
        this.allowedExternalHosts = buildAllowedExternalHostsString( dhisOidcProviderRepository );
    }

    @Override
    protected void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
        throws ServletException,
        IOException
    {
        if ( enabled )
        {
            String nonce = UUID.randomUUID().toString().replaceAll( "-", "" );

            req.getSession().setAttribute( CSP_REQUEST_NONCE_ATTR_NAME, nonce );

            chain.doFilter( req,
                new CSPNonceResponseWrapper( res, nonce, allowedExternalHosts,
                    upgradeInsecure ? CSP_UPGRADE_INSECURE_REQUESTS_VALUE : null ) );

            return;
        }

        chain.doFilter( req, res );
    }

    private String buildAllowedExternalHostsString( DhisOidcProviderRepository dhisOidcProviderRepository )
    {
        Set<String> allRegistrationId = dhisOidcProviderRepository.getAllRegistrationId();

        StringBuilder builder = new StringBuilder();
        for ( String id : allRegistrationId )
        {
            DhisOidcClientRegistration registration = dhisOidcProviderRepository
                .getDhisOidcClientRegistration( id );

            ClientRegistration clientRegistration = registration.getClientRegistration();
            String authorizationUri = clientRegistration.getProviderDetails().getAuthorizationUri();

            builder.append( authorizationUri ).append( " " );
        }

        return builder.toString();
    }

    /**
     * Wrapper to fill the nonce value
     */
    public static class CSPNonceResponseWrapper extends HttpServletResponseWrapper
    {
        private final String nonce;

        private final String allowedHosts;

        private final String upgradeInsecure;

        public CSPNonceResponseWrapper( HttpServletResponse response, String nonce, String allowedHosts,
            String upgradeInsecure )
        {
            super( response );

            this.nonce = ObjectUtils.firstNonNull( nonce, "" );
            this.allowedHosts = ObjectUtils.firstNonNull( allowedHosts, "" );
            this.upgradeInsecure = ObjectUtils.firstNonNull( upgradeInsecure, "" );
        }

        @Override
        public void setHeader( String name, String value )
        {
            if ( name.equals( "Content-Security-Policy" ) && StringUtils.isNotBlank( value ) )
            {
                super.setHeader( name, replaceVariables( value ) );
            }
            else
            {
                super.setHeader( name, value );
            }
        }

        @Override
        public void addHeader( String name, String value )
        {
            if ( name.equals( "Content-Security-Policy" ) && StringUtils.isNotBlank( value ) )
            {
                super.addHeader( name, replaceVariables( value ) );
            }
            else
            {
                super.addHeader( name, value );
            }
        }

        private String replaceVariables( String value )
        {
            return value.replace( "{nonce}", nonce ).replace( "{form-allowed-external-hosts}", allowedHosts )
                .replace( "{upgrade-insecure}", upgradeInsecure );
        }
    }
}
