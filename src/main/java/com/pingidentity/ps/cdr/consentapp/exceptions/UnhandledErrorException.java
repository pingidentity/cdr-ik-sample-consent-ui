package com.pingidentity.ps.cdr.consentapp.exceptions;

public class UnhandledErrorException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public UnhandledErrorException(final String msg)
    {
        super(msg);
    }
    
    public UnhandledErrorException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
