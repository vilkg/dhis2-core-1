package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_NONCE_ENABLED;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class CspNonceFilter extends OncePerRequestFilter
{
    public static final String CSP_REQUEST_NONCE_ATTR_NAME = "cspRequestNonce";

    private final boolean enabled;

    public CspNonceFilter( DhisConfigurationProvider dhisConfig )
    {
        this.enabled = dhisConfig.isEnabled( CSP_NONCE_ENABLED );
    }

    @Override
    protected void doFilterInternal( HttpServletRequest req, HttpServletResponse res, FilterChain chain )
        throws ServletException, IOException
    {
        if ( enabled )
        {
            String nonce = UUID.randomUUID().toString().replaceAll( "-", "" );

            req.getSession().setAttribute( "cspRequestNonce", nonce );

            chain.doFilter( req, new CSPNonceResponseWrapper( res, nonce ) );

            return;
        }

        chain.doFilter( req, res );
    }

    /**
     * Wrapper to fill the nonce value
     */
    public static class CSPNonceResponseWrapper extends HttpServletResponseWrapper
    {
        private String nonce;

        public CSPNonceResponseWrapper( HttpServletResponse response, String nonce )
        {
            super( response );
            this.nonce = nonce;
        }

        @Override
        public void setHeader( String name, String value )
        {
            if ( name.equals( "Content-Security-Policy" ) && StringUtils.isNotBlank( value ) )
            {
                super.setHeader( name, value.replace( "{nonce}", nonce ) );
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
                super.addHeader( name, value.replace( "{nonce}", nonce ) );
            }
            else
            {
                super.addHeader( name, value );
            }
        }
    }
}

