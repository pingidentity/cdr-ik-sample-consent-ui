package com.pingidentity.ps.cdr.consentapp.impl.standard.extconsent;

import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.pingidentity.ps.cdr.consentapp.exceptions.BadConfigurationException;
import com.pingidentity.ps.cdr.consentapp.scope.ScopeDefinition;
import com.pingidentity.ps.cdr.consentapp.sdk.extconsent.IExternalConsentStorage;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

public class NoExternalConsentStorageImpl implements IExternalConsentStorage
{
    
    public NoExternalConsentStorageImpl(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration,
					final Map<String, ScopeDefinition> scopeDefinitions) throws BadConfigurationException
    {
        //empty constructor
    }
    
    @Override
    public String createConsentRecord(final String subject, final long durationSeconds, final String[] accounts,
                                      final String[] selectedScopes)
    {
        return UUID.randomUUID().toString();
    }
    
}
