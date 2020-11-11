package com.pingidentity.ps.cdr.consentapp.exceptions;

public abstract class AbstractConsentApplicationException extends Exception
{
    
    /**
     *
     */
    private static final long serialVersionUID = -5758048331405620996L;
    
    protected AbstractConsentApplicationException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
    protected AbstractConsentApplicationException(final String msg)
    {
        super(msg);
    }
    
}
