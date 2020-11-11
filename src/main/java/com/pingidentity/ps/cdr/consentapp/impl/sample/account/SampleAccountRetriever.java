package com.pingidentity.ps.cdr.consentapp.impl.sample.account;

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
 * It is based on DeepThought's Administrative API's for OpenBanking/CDR
 * https://github.com/bizaio/deepthought.
 * <p>
 * This class obtains an access_token through a Client Credentials grant in
 * order to authorise itself when calling the API's.
 * <p>
 * The following arguments are required in the JSON configuration:
 * <UL>
 * <LI>baseUrl -- Base URL of the DeepThought Admin API e.g.
 * http://localhost:8088/dio-au</LI>
 * <LI>client-id -- Client Credentials client_id</LI>
 * <LI>client-secret -- Client Credentials client_secret</LI>
 * <LI>scopes -- Required scopes for the API's e.g.
 * DEEPTHOUGHT:ADMIN:GRANT:WRITE DEEPTHOUGHT:ADMIN:CUSTOMER:READ
 * DEEPTHOUGHT:ADMIN:CUSTOMER:WRITE DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:READ
 * DEEPTHOUGHT:ADMIN:BANK_ACCOUNT:WRITE</LI>
 * <LI>brand-id -- The brand of the data holder which applies to the
 * request.</LI>
 * <LI>ignore-ssl -- For dev only.</LI>
 * </UL>
 */
public class SampleAccountRetriever extends AbstractAccountRetriever
{
    
    private static final int DEFAULT_HTTPTIMEOUT = 30_000;
    private static final String DT_ACCOUNTS_URL = "%s/v1/brand/%s/customer/%s/account";
    
    private static final String CONF_BASEURL = "baseUrl";
    private static final String CONF_CLIENTID = "client-id";
    private static final String CONF_CLIENTSECRET = "client-secret";
    private static final String CONF_SCOPES = "scopes";
    private static final String CONF_BRANDID = "brand-id";
    private static final String CONF_IGNORESSL = "ignore-ssl";
    
    private final String baseUrl, clientId, clientSecret, brandId, scopes;
    private final boolean isIgnoreSSL;
    
    private final Log log = LogFactory.getLog(this.getClass());
    
    private String accessToken;
    
    public SampleAccountRetriever(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration)
            throws BadConfigurationException
    {
        super(pfOAuthHelper, configuration);
        
        this.baseUrl = getConfig(configuration, CONF_BASEURL);
        this.clientId = getConfig(configuration, CONF_CLIENTID);
        this.clientSecret = getConfig(configuration, CONF_CLIENTSECRET);
        this.brandId = getConfig(configuration, CONF_BRANDID);
        this.scopes = getConfig(configuration, CONF_SCOPES);
        
        this.isIgnoreSSL = getBooleanConfig(configuration, CONF_IGNORESSL);
        
    }
    
    @Override
    public List<Account> getAccounts(final String userId, final JSONObject chainedAttributes) throws UnhandledErrorException, AccountCreationException
    {
        
        final String accountEndpoint = String.format(DT_ACCOUNTS_URL, this.baseUrl, this.brandId, userId);
        
        List<Account> returnAccounts;
        
        try
        {
            if (accessToken == null)
            {
                throw new RetryableException("Access token is null. Retry with a new access token.");
            }
            
            final HttpResponseObj accountsHttpResp = getAccountsFromAPI(accountEndpoint);
            
            if (accountsHttpResp != null && accountsHttpResp.getStatusCode() == 401)
            {
                throw new RetryableException("Unauthorized - reattempting with a new access token.");
            }
            
            returnAccounts = marshallAccountsFromHttpResponse(accountsHttpResp);
            
        } catch (RetryableException e)
        {
            obtainNewAccessToken();
            
            HttpResponseObj accountsHttpResp = null;
            try
            {
                accountsHttpResp = getAccountsFromAPI(accountEndpoint);
            } catch (AbstractConsentApplicationException e1)
            {
                throw new UnhandledErrorException("Unable to retrieve accounts with new access token", e);
            }
            
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
        final Object jsonObj;
        try
        {
            jsonObj = parser.parse(accountsHttpResp.getResponseBody());
        } catch (ParseException e)
        {
            throw new UnhandledErrorException("Could not parse account response", e);
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
    
    private HttpResponseObj getAccountsFromAPI(final String accountEndpoint) throws AbstractConsentApplicationException
    {
        
        final Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + this.accessToken);
        headers.put("Content-Type", "application/json");
        
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
    
    private synchronized void obtainNewAccessToken() throws UnhandledErrorException
    {
        
        try
        {
            this.accessToken = this.getPfOAuthHelper().getCCAccessToken(this.clientId, this.clientSecret, this.scopes);
            
            if (this.accessToken == null)
            {
                throw new UnhandledErrorException(String.format("Access token received is null using tokenEndpoint=%s, clientId=%s, clientSecret=xxx, scopes=%s", this.getPfOAuthHelper().getTokenEndpoint(), this.clientId, this.scopes));
            }
            
        } catch (PFOAuthException e)
        {
            throw new UnhandledErrorException(String.format("Unable to receive access token using tokenEndpoint=%s, clientId=%s, clientSecret=xxx, scopes=%s", this.getPfOAuthHelper().getTokenEndpoint(), this.clientId, this.scopes), e);
        }
        
    }
    
}
