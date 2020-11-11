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

public class PFOAuthHelper
{
    
    private static final int DEFAULT_REQUEST_TIMEOUT = 30_000;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String ENDPOINT_TOKEN = "/as/token.oauth2";
    
    private final String keystoreLocation, keystorePassword, trustedCALocation, tokenEndpoint;
    private final boolean ignoreSSLErrors;
    
    public PFOAuthHelper(final String pfBaseUrl, final boolean ignoreSSLErrors,
                         final String keystoreLocation, final String keystorePassword, final String trustedCALocation)
    {
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.trustedCALocation = trustedCALocation;
        this.ignoreSSLErrors = ignoreSSLErrors;
        
        this.tokenEndpoint = pfBaseUrl + ENDPOINT_TOKEN;
    }
    
    public String getCCAccessToken(final String clientId, final String clientSecret, final String scopes) throws PFOAuthException
    {
        
        final Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
        
        HttpResponseObj tokenResponse;
        
        final Map<String, String> parameters = new HashMap<>();
        
        parameters.put("grant_type", "client_credentials");
        parameters.put("scope", scopes);
        
        try
        {
            tokenResponse = MASSLClient.executeHTTP(this.getTokenEndpoint(), "POST", httpHeaders,
                    parameters, null, this.keystoreLocation, this.trustedCALocation,
                    this.keystorePassword, DEFAULT_KEYSTORE_TYPE, ignoreSSLErrors, DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new PFOAuthException("Unable to get CC Access Token", e);
        }
        
        if (tokenResponse == null)
        {
            throw new PFOAuthException("Access Token response is null");
        } else if (tokenResponse.getStatusCode() != 200)
        {
            throw new PFOAuthException(
                    "Access Token response returned status code: " + tokenResponse.getStatusCode());
        }
        
        final JSONParser parser = new JSONParser();
        JSONObject responseJSON = null;
        try
        {
            responseJSON = (JSONObject) parser.parse(tokenResponse.getResponseBody());
            
            if (responseJSON == null)
            {
                throw new PFOAuthException("JSON parsed access token response is null");
            }
        } catch (ParseException e)
        {
            throw new PFOAuthException(
                    "There was an issue parsing JSON access token content - " + tokenResponse.getResponseBody(), e);
        }
        
        if (!responseJSON.containsKey("access_token"))
        {
            throw new PFOAuthException("access_token claim was not provided in the access token response");
        }
        
        return (String) responseJSON.get("access_token");
    }
    
    public String getTokenEndpoint()
    {
        return tokenEndpoint;
    }
    
}
