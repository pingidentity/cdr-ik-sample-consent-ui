package com.pingidentity.ps.cdr.consentapp.exceptions;

public class PFAgentlessException extends AbstractConsentApplicationException
{
    
    /**
     *
     */
    private static final long serialVersionUID = -99241535494400881L;
    
    public PFAgentlessException(final String msg)
    {
        super(msg);
    }
    
    public PFAgentlessException(final String msg, final Throwable e)
    {
        super(msg, e);
    }
    
}
