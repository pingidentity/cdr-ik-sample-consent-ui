package com.pingidentity.ps.cdr.consentapp.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.pingidentity.ps.cdr.consentapp.exceptions.PFAgentlessException;
import org.json.simple.parser.ParseException;

public class PFAgentlessHelper
{
    
    private static final int DEFAULT_REQUEST_TIMEOUT = 30000;
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
    private static final String ENDPOINT_PICKUP = "/ext/ref/pickup?REF=";
    private static final String ENDPOINT_DROPOFF = "/ext/ref/dropoff";
    
    private final String keystoreLocation, keystorePassword, trustedCALocation, pickupEndpoint, dropoffEndpoint;
    private final boolean ignoreSSLErrors;
    
    private final Map<String, String> httpHeaders;
    
    public PFAgentlessHelper(final String pfBaseUrl, final boolean ignoreSSLErrors, final String username, final String password,
                             final String instanceId, final String keystoreLocation, final String keystorePassword, final String trustedCALocation)
    {
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.trustedCALocation = trustedCALocation;
        this.ignoreSSLErrors = ignoreSSLErrors;
        
        this.pickupEndpoint = pfBaseUrl + ENDPOINT_PICKUP;
        this.dropoffEndpoint = pfBaseUrl + ENDPOINT_DROPOFF;
        
        final Map<String, String> httpHeaders = new HashMap<>();
        
        httpHeaders.put("ping.uname", username);
        httpHeaders.put("ping.pwd", password);
        httpHeaders.put("ping.instanceId", instanceId);
        
        this.httpHeaders = Collections.unmodifiableMap(httpHeaders);
    }
    
    public JSONObject pickupRef(final String ref) throws PFAgentlessException
    {
        HttpResponseObj refTokenResponse;
        try
        {
            refTokenResponse = MASSLClient.executeGETHTTP(this.pickupEndpoint + ref, this.httpHeaders, null,
                    this.keystoreLocation, this.trustedCALocation, this.keystorePassword, DEFAULT_KEYSTORE_TYPE,
                    ignoreSSLErrors, DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new PFAgentlessException("Unable to pickup REF token", e);
        }
        
        if (refTokenResponse == null)
        {
            throw new PFAgentlessException("Agentless pickup response is null");
        } else if (refTokenResponse.getStatusCode() != 200)
        {
            throw new PFAgentlessException(
                    "Agentless pickup response returned status code: " + refTokenResponse.getStatusCode());
        }
        
        final JSONParser parser = new JSONParser();
        JSONObject responseJSON;
        try
        {
            responseJSON = (JSONObject) parser.parse(refTokenResponse.getResponseBody());
            
            if (responseJSON == null)
            {
                throw new PFAgentlessException("JSON parsed pickup response is null");
            }
        } catch (ParseException e)
        {
            throw new PFAgentlessException(
                    "There was an issue parsing JSON pickup Content - " + refTokenResponse.getResponseBody(), e);
        }
        
        return responseJSON;
    }
    
    public String dropoffRef(final JSONObject dataObject) throws PFAgentlessException
    {
        HttpResponseObj refTokenResponse;
        try
        {
            refTokenResponse = MASSLClient.executeHTTP(this.dropoffEndpoint, "POST", this.httpHeaders,
                    dataObject.toJSONString(), null, this.keystoreLocation, this.trustedCALocation,
                    this.keystorePassword, DEFAULT_KEYSTORE_TYPE, ignoreSSLErrors, DEFAULT_REQUEST_TIMEOUT);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException e)
        {
            throw new PFAgentlessException("Unable to dropoff REF artefact", e);
        }
        
        if (refTokenResponse == null)
        {
            throw new PFAgentlessException("Agentless dropoff response is null");
        } else if (refTokenResponse.getStatusCode() != 200)
        {
            throw new PFAgentlessException(
                    "Agentless dropoff response returned status code: " + refTokenResponse.getStatusCode());
        }
        
        final JSONParser parser = new JSONParser();
        JSONObject responseJSON;
        try
        {
            responseJSON = (JSONObject) parser.parse(refTokenResponse.getResponseBody());
            
            if (responseJSON == null)
            {
                throw new PFAgentlessException("JSON parsed dropoff response is null");
            }
        } catch (ParseException e)
        {
            throw new PFAgentlessException(
                    "There was an issue parsing JSON pickup Content - " + refTokenResponse.getResponseBody(), e);
        }
        
        if (!responseJSON.containsKey("REF"))
        {
            throw new PFAgentlessException("REF claim was not provided in the dropoff");
        }
        
        return (String) responseJSON.get("REF");
    }
    
}
