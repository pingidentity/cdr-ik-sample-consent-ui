package com.pingidentity.ps.cdr.consentapp.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.pingidentity.ps.cdr.consentapp.exceptions.PFOAuthException;
import org.json.simple.parser.ParseException;

public class PFOAuthMgtHelper
{
    
    private static final int DEFAULT_REQUEST_TIMEOUT = 30000;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String ENDPOINT_CLIENTS = "/pf-ws/rest/oauth/clients";
    private static final String ENDPOINT_GRANT = "/pf-ws/rest/oauth/users/%s/grants";
    
    private final String keystoreLocation, keystorePassword, trustedCALocation, clientMgtEndpoint, grantMgtEndpoint;
    private final boolean ignoreSSLErrors;
    private final Map<String, String> httpHeaders;
    
    public PFOAuthMgtHelper(final String pfBaseUrl, final String username, final String password, final boolean ignoreSSLErrors,
                            final String keystoreLocation, final String keystorePassword, final String trustedCALocation)
    {
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.trustedCALocation = trustedCALocation;
        this.ignoreSSLErrors = ignoreSSLErrors;
        
        this.clientMgtEndpoint = pfBaseUrl + ENDPOINT_CLIENTS;
        this.grantMgtEndpoint = pfBaseUrl + ENDPOINT_GRANT;
        
        httpHeaders = new HashMap<>();
        httpHeaders.put("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        httpHeaders.put("X-XSRF-Header", "PingFederate");
    }
    
    public JSONObject getClientDetails(final String clientId) throws PFOAuthException
    {
        
        //TODO: Cache this response
        
        HttpResponseObj clientReadResponse = null;
        
        try
        {
            clientReadResponse = MASSLClient.executeGETHTTP(this.clientMgtEndpoint + "/" + clientId, httpHeaders, null,
                    keystoreLocation, trustedCALocation, keystorePassword, DEFAULT_KEYSTORE_TYPE, ignoreSSLErrors,
                    DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new PFOAuthException("Unable to get Client Record", e);
        }
        
        if (clientReadResponse == null)
        {
            throw new PFOAuthException("ClientRecord response is null");
        } else if (clientReadResponse.getStatusCode() != 200)
        {
            throw new PFOAuthException(
                    "Client Record response returned status code: " + clientReadResponse.getStatusCode());
        }
        
        final JSONParser parser = new JSONParser();
        JSONObject responseJSON = null;
        try
        {
            responseJSON = (JSONObject) parser.parse(clientReadResponse.getResponseBody());
            
            if (responseJSON == null)
            {
                throw new PFOAuthException("JSON parsed Client Record response is null");
            }
        } catch (ParseException e)
        {
            throw new PFOAuthException(
                    "There was an issue parsing JSON Client Record content - " + clientReadResponse.getResponseBody(),
                    e);
        }
        
        return responseJSON;
    }
    
    public JSONObject getGrantDetails(final String userKey, final String grantId) throws PFOAuthException
    {
        
        HttpResponseObj grantReadResponse;
        
        try
        {
            grantReadResponse = MASSLClient.executeGETHTTP(String.format(this.grantMgtEndpoint, userKey) + "/" + grantId, httpHeaders, null,
                    keystoreLocation, trustedCALocation, keystorePassword, DEFAULT_KEYSTORE_TYPE, ignoreSSLErrors,
                    DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new PFOAuthException("Unable to get Grant Record", e);
        }
        
        if (grantReadResponse == null)
        {
            throw new PFOAuthException("Grant response is null");
        } else if (grantReadResponse.getStatusCode() != 200)
        {
            throw new PFOAuthException(
                    "Grant Record response returned status code: " + grantReadResponse.getStatusCode());
        }
        
        final JSONParser parser = new JSONParser();
        JSONObject responseJSON;
        try
        {
            responseJSON = (JSONObject) parser.parse(grantReadResponse.getResponseBody());
            
            if (responseJSON == null)
            {
                throw new PFOAuthException("JSON parsed Grant Record response is null");
            }
        } catch (ParseException e)
        {
            throw new PFOAuthException(
                    "There was an issue parsing JSON Grant Record content - " + grantReadResponse.getResponseBody(),
                    e);
        }
        
        return responseJSON;
    }
    
}
