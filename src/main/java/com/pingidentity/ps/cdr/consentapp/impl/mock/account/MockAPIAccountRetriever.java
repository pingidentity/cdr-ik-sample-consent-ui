package com.pingidentity.ps.cdr.consentapp.impl.mock.account;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pingidentity.ps.cdr.consentapp.exceptions.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.pingidentity.ps.cdr.consentapp.sdk.account.AbstractAccountRetriever;
import com.pingidentity.ps.cdr.consentapp.sdk.account.Account;
import com.pingidentity.ps.cdr.consentapp.utils.HttpResponseObj;
import com.pingidentity.ps.cdr.consentapp.utils.MASSLClient;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

/**
 * This is a sample implementation of how to receive Accounts by calling an API.
 * It is based on a mock bank API service that Ping Identity has created.
 * <p>
 * The following arguments are required in the JSON configuration:
 * <UL>
 * <LI>baseUrl -- Base URL of the DeepThought Admin API e.g.
 * http://localhost:6878</LI>
 * <LI>ignore-ssl -- For dev only.</LI>
 * </UL>
 */
public class MockAPIAccountRetriever extends AbstractAccountRetriever
{
    
    private static final int DEFAULT_HTTPTIMEOUT = 30_000;
    private static final String API_ACCOUNTS_URL = "%s/cds-au/v1/banking/accounts";
    
    private static final String CONF_BASEURL = "baseUrl";
    private static final String CONF_IGNORESSL = "ignore-ssl";
    
    private final String baseUrl;
    private final boolean isIgnoreSSL;
    
    private final Log log = LogFactory.getLog(this.getClass());
    
    public MockAPIAccountRetriever(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration)
            throws BadConfigurationException
    {
        super(pfOAuthHelper, configuration);
        
        this.baseUrl = getConfig(configuration, CONF_BASEURL);        
        this.isIgnoreSSL = getBooleanConfig(configuration, CONF_IGNORESSL);
        
    }
    
    @Override
    public List<Account> getAccounts(final String userId, final JSONObject chainedAttributes) throws UnhandledErrorException, AccountCreationException
    {
        
        final String accountEndpoint = String.format(API_ACCOUNTS_URL, this.baseUrl);
        
        List<Account> returnAccounts;
        
        try
        {            
            final HttpResponseObj accountsHttpResp = getAccountsFromAPI(accountEndpoint, userId);
            
            returnAccounts = marshallAccountsFromHttpResponse(accountsHttpResp);
            
        } catch (AbstractConsentApplicationException e)
        {
            throw new UnhandledErrorException("Unable to receive account information", e);
        }
        
        return returnAccounts;
    }
    
    private String getConfig(final JSONObject configuration, final String configName) throws BadConfigurationException
    {
        if (!configuration.containsKey(configName))
        {
            throw new BadConfigurationException("Missing config: " + configName);
        }
        
        return (String) configuration.get(configName);
    }
    
    private Boolean getBooleanConfig(final JSONObject configuration, final String configName) throws BadConfigurationException
    {
        if (!configuration.containsKey(configName))
        {
            throw new BadConfigurationException("Missing config: " + configName);
        }
        
        return (Boolean) configuration.get(configName);
    }
    
    private List<Account> marshallAccountsFromHttpResponse(final HttpResponseObj accountsHttpResp)
            throws AccountCreationException, UnhandledErrorException
    {
        
        final JSONParser parser = new JSONParser();
        final JSONObject jsonObj;
        try
        {
            jsonObj = (JSONObject) parser.parse(accountsHttpResp.getResponseBody());
        } catch (ParseException e)
        {
            throw new UnhandledErrorException("Could not parse account response", e);
        }
        
        if(!jsonObj.containsKey("data") || !(jsonObj.get("data") instanceof JSONObject))
            throw new AccountCreationException("Account API response does not contain a data object");
        
        JSONObject dataObject = (JSONObject) jsonObj.get("data");

        if(!dataObject.containsKey("accounts") || !(dataObject.get("accounts") instanceof JSONArray))
            throw new AccountCreationException("Account API response does not contain a accounts array");
        
        JSONArray accountArray = (JSONArray) dataObject.get("accounts");
        
        final List<Account> returnAccounts = new ArrayList<>();
        
        for (final Object currentObj : accountArray)
        {
            if (!(currentObj instanceof JSONObject))
            {
                
                log.debug("Omitting currentObj because it isn't a JSONObject");
                continue;
            }
            
            final JSONObject currentJSON = (JSONObject) currentObj;
            
            log.info("JSON: " + currentJSON.toJSONString());
            
            final Account newAccount;
            try
            {
                newAccount = Account.createAccount(currentJSON.get("accountId"), currentJSON.get("openStatus"),
                		getValue(currentJSON, "displayName"), getValue(currentJSON, "nickname"),
                		getValue(currentJSON, "maskedNumber"), currentJSON.get("isOwned"));
            } catch (UnhandledErrorException e)
            {
                throw new AccountCreationException("Unable to create account object", e);
            }
            
            returnAccounts.add(newAccount);
        }
        
        return returnAccounts;
    }
    
    private Object getValue(JSONObject jsonObj, String claim)
    {
    	if(!jsonObj.containsKey(claim))
    		return null;
    	else
    		return jsonObj.get(claim);
    }
    
    private HttpResponseObj getAccountsFromAPI(final String accountEndpoint, final String userId) throws AbstractConsentApplicationException
    {
        
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-USER", userId);
        
        HttpResponseObj accountResponse;
        
        try
        {
            accountResponse = MASSLClient.executeGETHTTP(accountEndpoint, headers, null, null, null, null, null,
                    isIgnoreSSL, DEFAULT_HTTPTIMEOUT);
            
            if (accountResponse == null)
            {
                throw new UnhandledErrorException("account response is null");
            } else if (accountResponse.getStatusCode() == 401)
            {
                throw new RetryableException("bad account response code: " + accountResponse.getStatusCode());
            } else if (accountResponse.getStatusCode() != 200)
            {
                throw new UnhandledErrorException("bad account response code: " + accountResponse.getStatusCode());
            }
            
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new UnhandledErrorException("Error reading accounts", e);
        }
        
        return accountResponse;
    }
    
}
