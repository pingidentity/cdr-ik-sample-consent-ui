package com.pingidentity.ps.cdr.consentapp.impl.sample.extconsent;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.pingidentity.ps.cdr.consentapp.exceptions.AbstractConsentApplicationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.BadConfigurationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.PFOAuthException;
import com.pingidentity.ps.cdr.consentapp.exceptions.RetryableException;
import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;
import com.pingidentity.ps.cdr.consentapp.scope.ScopeDefinition;
import com.pingidentity.ps.cdr.consentapp.sdk.extconsent.IExternalConsentStorage;
import com.pingidentity.ps.cdr.consentapp.utils.HttpResponseObj;
import com.pingidentity.ps.cdr.consentapp.utils.MASSLClient;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

/**
 * This is a sample implementation of how to store consent externally by calling
 * an API. It is based on DeepThought's Administrative API's for OpenBanking/CDR
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
 * <LI>ignore-ssl -- For dev only.</LI>
 * </UL>
 */
public class SampleConsentStorageImpl implements IExternalConsentStorage
{
    
    private enum ScopeMappings
    {
        TRANSACTIONS_READ("bank:transactions:read"), CUSTOMER_DETAIL_READ("common:customer.detail:read"),
        PAYMENTS_READ("bank:regular_payments:read"), PAYEES_READ("bank:payees:read"),
        ACCOUNTS_DETAIL_READ("bank:accounts.detail:read"), ACCOUNTS_BASIC_READ("bank:accounts.basic:read"),
        CUSTOMER_BASIC_READ("common:customer.basic:read");
        
        private final String scopeStr;
        
        ScopeMappings(final String scopeStr)
        {
            this.scopeStr = scopeStr;
        }
        
        public static ScopeMappings findByScope(final String scopeStr)
        {
            for (final ScopeMappings v : values())
            {
                if (v.scopeStr.equals(scopeStr))
                {
                    return v;
                }
            }
            return null;
        }
        
    }
    
    ;
    
    private static final int DEFAULT_HTTPTIMEOUT = 30_000;
    private static final String DT_GRANT_URL = "%s/v1/grant";
    
    private static final String CONF_BASEURL = "baseUrl";
    private static final String CONF_CLIENTID = "client-id";
    private static final String CONF_CLIENTSECRET = "client-secret";
    private static final String CONF_SCOPES = "scopes";
    private static final String CONF_IGNORESSL = "ignore-ssl";
    
    private final String baseUrl, clientId, clientSecret, scopes;
    private final boolean isIgnoreSSL;
    
    private final PFOAuthHelper pfOAuthHelper;
    
    private final Map<String, ScopeDefinition> scopeDefinitions;
    
    private final Log log = LogFactory.getLog(this.getClass());
    
    private String accessToken;
    
    public SampleConsentStorageImpl(final PFOAuthHelper pfOAuthHelper, final JSONObject configuration,
                                    final Map<String, ScopeDefinition> scopeDefinitions) throws BadConfigurationException
    {
        this.pfOAuthHelper = pfOAuthHelper;
        
        this.baseUrl = getConfig(configuration, CONF_BASEURL);
        this.clientId = getConfig(configuration, CONF_CLIENTID);
        this.clientSecret = getConfig(configuration, CONF_CLIENTSECRET);
        this.scopes = getConfig(configuration, CONF_SCOPES);
        
        this.isIgnoreSSL = getBooleanConfig(configuration, CONF_IGNORESSL);
        this.scopeDefinitions = scopeDefinitions;
        
    }
    
    @Override
    public String createConsentRecord(final String subject, final long durationSeconds, final String[] accounts, final String[] selectedScopes)
            throws AbstractConsentApplicationException
    {
        
        String consentId = null;
        
        try
        {
            if (accessToken == null)
            {
                throw new RetryableException("Access token is null. Retry with a new access token.");
            }
            
            final HttpResponseObj consentHttpResp = createDeepThoughtConsent(subject, durationSeconds, accounts,
                    selectedScopes);
            
            consentId = getIdFromResponse(consentHttpResp);
            
        } catch (RetryableException e)
        {
            obtainNewAccessToken();
            
            final HttpResponseObj consentHttpResp = createDeepThoughtConsent(subject, durationSeconds, accounts, selectedScopes);
            consentId = getIdFromResponse(consentHttpResp);
        }
        
        return consentId;
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
    
    private String getIdFromResponse(final HttpResponseObj consentHttpResp) throws UnhandledErrorException
    {
        
        final JSONParser parser = new JSONParser();
        JSONObject jsonObj = null;
        
        try
        {
            jsonObj = (JSONObject) parser.parse(consentHttpResp.getResponseBody());
        } catch (ParseException e)
        {
            throw new UnhandledErrorException("Unable to parse json response", e);
        }
        
        if (jsonObj == null || !jsonObj.containsKey("id"))
        {
            throw new UnhandledErrorException("Unable to obtain id from response");
        }
        
        return (String) jsonObj.get("id");
    }
    
    @SuppressWarnings("unchecked")
    private HttpResponseObj createDeepThoughtConsent(final String subject, final long durationSeconds, final String[] accounts,
                                                     final String[] selectedScopes) throws AbstractConsentApplicationException
    {
        final String grantEndpoint = String.format(DT_GRANT_URL, this.baseUrl);
        
        final JSONObject createGrantRequest = new JSONObject();
        createGrantRequest.put("subject", subject);
        createGrantRequest.put("length", durationSeconds);
        
        final JSONArray customerAccounts = new JSONArray();
        
        for (final String account : accounts)
        {
            final JSONObject accountObj = new JSONObject();
            accountObj.put("customerAccountId", account);
            
            final List<String> deepthoughtGrants = getDeepthoughtGrants(selectedScopes);
            accountObj.put("permissions", deepthoughtGrants);
            
            customerAccounts.add(accountObj);
        }
        
        createGrantRequest.put("customerAccounts", customerAccounts);
        
        final String requestContent = createGrantRequest.toJSONString();
        
        if (log.isDebugEnabled())
        {
            log.debug("Access Token: " + accessToken);
            log.debug("URL: " + grantEndpoint);
            log.debug("Content: " + requestContent);
        }
        
        final Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");
        
        final HttpResponseObj responseObj;
        try
        {
            responseObj = MASSLClient.executeHTTP(grantEndpoint, "POST", headers, requestContent, null, null, null,
                    null, null, this.isIgnoreSSL, DEFAULT_HTTPTIMEOUT);
            
            if (responseObj == null)
            {
                throw new UnhandledErrorException("response object is null");
            }
            
            if (responseObj.getStatusCode() == 401)
            {
                throw new RetryableException("401 unauthorised");
            }
            
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new UnhandledErrorException("Unhandled error creating consent record", e);
        }
        
        if (responseObj.getStatusCode() != 200 || responseObj.getResponseBody() == null)
        {
            throw new UnhandledErrorException(
                    "Bad status code (" + responseObj.getStatusCode() + ") or response body is null");
        }
        
        return responseObj;
    }
    
    private List<String> getDeepthoughtGrants(final String[] requestedScope)
    {
        final List<String> returnGrants = new ArrayList<>();
        for (final String currentPermission : requestedScope)
        {
            if (this.scopeDefinitions.containsKey(currentPermission))
            {
                final ScopeMappings scopeMapping = ScopeMappings.findByScope(currentPermission);
                if (scopeMapping != null)
                {
                    returnGrants.add(scopeMapping.toString());
                }
            }
        }
        return returnGrants;
    }
    
    private synchronized void obtainNewAccessToken() throws UnhandledErrorException
    {
        
        try
        {
            this.accessToken = this.pfOAuthHelper.getCCAccessToken(this.clientId, this.clientSecret, this.scopes);
            
            if (this.accessToken == null)
            {
                throw new UnhandledErrorException(String.format("Access token received is null using tokenEndpoint=%s, clientId=%s, clientSecret=xxx, scopes=%s", pfOAuthHelper.getTokenEndpoint(), this.clientId, this.scopes));
            }
            
        } catch (PFOAuthException e)
        {
            throw new UnhandledErrorException(String.format("Unable to receive access token using tokenEndpoint=%s, clientId=%s, clientSecret=xxx, scopes=%s", pfOAuthHelper.getTokenEndpoint(), this.clientId, this.scopes), e);
        }
        
    }
    
}
