package com.pingidentity.ps.cdr.consentapp.exceptions;

public class RetryableException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public RetryableException(final String msg)
    {
        super(msg);
    }
    
    public RetryableException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
