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

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class CspNonceFilter extends OncePerRequestFilter
{
    public static final String CSP_REQUEST_NONCE_ATTR_NAME = "cspRequestNonce";

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

    @Override
    protected void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
        throws ServletException, IOException
    {
        if ( enabled )
        {
            String nonce = UUID.randomUUID().toString().replaceAll( "-", "" );

            req.getSession().setAttribute( "cspRequestNonce", nonce );

            chain.doFilter( req,
                new CSPNonceResponseWrapper( res, nonce, allowedExternalHosts,
                    upgradeInsecure ? "upgrade-insecure-requests;" : null ) );

            return;
        }

        chain.doFilter( req, res );
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
            return value.replace( "{nonce}", nonce ).
                replace( "{form-allowed-external-hosts}", allowedHosts )
                .replace( "{upgrade-insecure}", upgradeInsecure );
        }
    }
}

