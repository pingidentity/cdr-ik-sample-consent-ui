package com.pingidentity.ps.cdr.consentapp.sdk.extconsent;

import com.pingidentity.ps.cdr.consentapp.exceptions.AbstractConsentApplicationException;


/**
 * Interface requires implementations to persist consent information to a third party repository.
 * <p>
 * This implementation is optional and in some deployments PingFederate's grant storage is sufficient.
 * Where external consent storage is not required, consider using: com.pingidentity.ps.cdr.consentapp.impl.standard.extconsent.NoExternalConsentStorageImpl.
 */
public interface IExternalConsentStorage
{
    
    /**
     * This method will persist approved consent information into an external user store.
     *
     * @param subject          is the identifier of the user record.
     * @param durationSeconds  number of seconds the consent will be active for. This value will be capped to 365 days.
     * @param selectedAccounts is the selected accounts for which this consent will apply to.
     * @param selectedScopes   is the selected scopes for which this consent will apply to.
     * @return List of Accounts
     */
    String createConsentRecord(String subject, long durationSeconds, String[] selectedAccounts,
                               String[] selectedScopes) throws AbstractConsentApplicationException;
}
