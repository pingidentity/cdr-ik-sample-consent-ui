package com.pingidentity.ps.cdr.consentapp.exceptions;

public class BadInitialisationException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public BadInitialisationException(final String msg)
    {
        super(msg);
    }
    
    public BadInitialisationException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
