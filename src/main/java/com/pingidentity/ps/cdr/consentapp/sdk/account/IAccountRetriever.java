package com.pingidentity.ps.cdr.consentapp.sdk.account;

import java.util.List;

import com.pingidentity.ps.cdr.consentapp.exceptions.AccountCreationException;
import org.json.simple.JSONObject;

import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;


/**
 * Interface requires implementations to receive account information based on userId.
 */
public interface IAccountRetriever
{
    
    /**
     * This method will receive an account list based on userId or agentless pickup attributes.
     *
     * @param userId           is the identifier of the user record
     * @param pickupJSONObject is a JSON Object representing the agentless pickup result.
     * @return List of Accounts
     */
    List<Account> getAccounts(String userId, JSONObject pickupJSONObject) throws UnhandledErrorException, AccountCreationException;
    
}
