package com.pingidentity.ps.cdr.consentapp.sdk.account;

import java.util.List;

import com.pingidentity.ps.cdr.consentapp.exceptions.AccountCreationException;
import org.json.simple.JSONObject;

import com.pingidentity.ps.cdr.consentapp.exceptions.BadConfigurationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

public abstract class AbstractAccountRetriever implements IAccountRetriever
{
    
    private final JSONObject configuration;
    private final PFOAuthHelper pfOAuthHelper;
    
    protected AbstractAccountRetriever(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration) throws BadConfigurationException
    {
        this.configuration = configuration;
        this.pfOAuthHelper = pfOAuthHelper;
        
        if (configuration == null)
        {
            throw new BadConfigurationException("Could not resolve configuration");
        }
    }
    
    protected PFOAuthHelper getPfOAuthHelper()
    {
        return pfOAuthHelper;
    }
    
    protected JSONObject getConfiguration()
    {
        return this.configuration;
    }
    
    @Override
    public abstract List<Account> getAccounts(String userId, JSONObject pickupJSONObject) throws UnhandledErrorException, AccountCreationException;
    
}
