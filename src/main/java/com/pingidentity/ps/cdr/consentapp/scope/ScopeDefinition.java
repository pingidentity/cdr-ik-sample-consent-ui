package com.pingidentity.ps.cdr.consentapp.scope;

public class ScopeDefinition
{
    
    private final String value, title, shortReason, longReason;
    
    public ScopeDefinition(final String value, final String title, final String shortReason, final String longReason)
    {
        this.value = value;
        this.title = title;
        this.shortReason = shortReason;
        this.longReason = longReason;
    }
    
    public String getValue()
    {
        return value;
    }
    
    public String getTitle()
    {
        return title;
    }
    
    public String getShortReason()
    {
        return shortReason;
    }
    
    public String getLongReason()
    {
        return longReason;
    }
}
