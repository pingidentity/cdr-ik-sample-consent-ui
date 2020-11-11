package com.pingidentity.ps.cdr.consentapp.exceptions;

public class BadConfigurationException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public BadConfigurationException(final String msg)
    {
        super(msg);
    }
    
    public BadConfigurationException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
