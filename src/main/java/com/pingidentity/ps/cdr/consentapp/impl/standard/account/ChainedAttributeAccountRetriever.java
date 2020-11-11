package com.pingidentity.ps.cdr.consentapp.impl.standard.account;

import java.util.ArrayList;
import java.util.List;

import com.pingidentity.ps.cdr.consentapp.exceptions.AccountCreationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.pingidentity.ps.cdr.consentapp.exceptions.BadConfigurationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;
import com.pingidentity.ps.cdr.consentapp.sdk.account.AbstractAccountRetriever;
import com.pingidentity.ps.cdr.consentapp.sdk.account.Account;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

/**
 * This implementation receives account information from the authentication chain.
 * <p>
 * The expected pickup attribute is a JSONArray of Account objects, containing the following members:
 * <UL>
 * <LI>id - Account unique identifier</LI>
 * <LI>openStatus - Status of the account</LI>
 * <LI>displayName - Display name of the account</LI>
 * <LI>nickName - Nickname of the account</LI>
 * <LI>accountNumber - Meaningful account number of the account</LI>
 * <LI>owner - A boolean flag to state whether the account is a singular owner or not</LI>
 * <p>
 * The following arguments are required in the JSON configuration:
 * <UL>
 * <LI>pickupAttribute -- Pickup attribute which contains account information</LI>
 * </UL>
 */
public class ChainedAttributeAccountRetriever extends AbstractAccountRetriever
{
    
    private static final String CONF_CHAINEDATTRIBUTE = "pickupAttribute";
    
    private final String pickupAttribute;
    
    private final Log log = LogFactory.getLog(this.getClass());
    
    public ChainedAttributeAccountRetriever(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration) throws BadConfigurationException
    {
        super(pfOAuthHelper, configuration);
        
        this.pickupAttribute = getConfig(configuration, CONF_CHAINEDATTRIBUTE);
    }
    
    @Override
    public List<Account> getAccounts(final String userId, final JSONObject pickupJSONObject) throws UnhandledErrorException, AccountCreationException
    {
        
        return marshallAccountsFromPickup(pickupJSONObject);
    }
    
    private String getConfig(final JSONObject configuration, final String configName) throws BadConfigurationException
    {
        if (!configuration.containsKey(configName))
        {
            throw new BadConfigurationException("Missing config: " + configName);
        }
        
        return (String) configuration.get(configName);
    }
    
    
    private List<Account> marshallAccountsFromPickup(final JSONObject pickupJSONObject) throws UnhandledErrorException, AccountCreationException
    {
        
        if (pickupJSONObject.containsKey(this.pickupAttribute))
        {
            throw new UnhandledErrorException("Account information not found in pickup object");
        }
        
        final JSONArray jsonObj;
        try
        {
            jsonObj = (JSONArray) pickupJSONObject.get(this.pickupAttribute);
        } catch (Exception e)
        {
            throw new UnhandledErrorException("Error reading accounts", e);
        }
        
        final List<Account> returnAccounts = new ArrayList<>();
        
        for (final Object currentObj : (JSONArray) jsonObj)
        {
            if (!(currentObj instanceof JSONObject))
            {
                
                log.debug("Omitting currentObj because it isn't a JSONObject");
                
                continue;
            }
            
            final JSONObject currentJSON = (JSONObject) currentObj;
            
            if (!currentJSON.containsKey("bankAccount"))
            {
                
                log.debug("Ommitting currentObj because it doesn't contain bankAccount");
                
                continue;
            }
            
            final JSONObject bankAccountJSON = (JSONObject) currentJSON.get("bankAccount");
            
            final Account newAccount;
            try
            {
                newAccount = Account.createAccount(currentJSON.get("id"), bankAccountJSON.get("openStatus"),
                        bankAccountJSON.get("displayName"), bankAccountJSON.get("nickName"),
                        bankAccountJSON.get("accountNumber"), currentJSON.get("owner"));
            } catch (UnhandledErrorException e)
            {
                throw new AccountCreationException("Unable to create account object", e);
            }
            
            returnAccounts.add(newAccount);
        }
        
        return returnAccounts;
    }
    
}
