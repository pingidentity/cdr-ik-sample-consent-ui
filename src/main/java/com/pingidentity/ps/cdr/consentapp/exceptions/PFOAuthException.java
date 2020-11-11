package com.pingidentity.ps.cdr.consentapp.exceptions;

public class PFOAuthException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public PFOAuthException(final String msg)
    {
        super(msg);
    }
    
    public PFOAuthException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
