package com.pingidentity.ps.cdr.consentapp.exceptions;

public class AccountCreationException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public AccountCreationException(final String msg)
    {
        super(msg);
    }
    
    public AccountCreationException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
