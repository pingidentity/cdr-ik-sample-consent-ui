package com.pingidentity.ps.cdr.consentapp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

public class MASSLClient
{
    
    private final static Log LOGGER = LogFactory.getLog(MASSLClient.class);
    
    private final static String[] DEFAULT_HTTPSPROTOCOLS = {"TLSv1.1", "TLSv1.2"};
    private final static HostnameVerifier DEFAULT_HOSTNAMEVERIFIER = SSLConnectionSocketFactory
            .getDefaultHostnameVerifier();
    
    private static Map<String, KeyStore> keyStoreCache = new HashMap<>();
    
    public static void removeKeystoreCacheItem(final String keystoreIdentifier)
    {
        
        keyStoreCache.remove(keystoreIdentifier);
        
    }
    
    public static HttpResponseObj executeGETHTTP(final String url, final Map<String, String> headers, final String[] httpsProtocolSupport,
                                                 final String keystoreLocation, final String rootCALocation, final String keystorePassword,
						 final String keystoreType, final boolean ignoreSSLErrors,
                                                 final int requestTimeout) throws IOException, CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException
    {
        
        return executeHTTP(url, "GET", headers, new StringEntity(""), httpsProtocolSupport, keystoreLocation, rootCALocation,
                keystorePassword, keystoreType, ignoreSSLErrors, requestTimeout);
    }
    
    public static HttpResponseObj executeHTTP(final String url, final String method, final Map<String, String> headers, final String data,
                                              final String[] httpsProtocolSupport, final String keystoreLocation, final String rootCALocation,
					      final String keystorePassword, final String keystoreType,
                                              final boolean ignoreSSLErrors, final int requestTimeout) throws CertificateException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException
    {
        
        final StringEntity entity = new StringEntity(data);
        
        return executeHTTP(url, method, headers, entity, httpsProtocolSupport, keystoreLocation, keystoreType, rootCALocation, keystorePassword,
			ignoreSSLErrors,
                requestTimeout);
    }
    
    public static HttpResponseObj executeHTTP(final String url, final String method, final Map<String, String> headers,
                                              final Map<String, String> params, final String[] httpsProtocolSupport, final String keystoreLocation,
					      final String rootCALocation,
                                              final String keystorePassword, final String keystoreType, final boolean ignoreSSLErrors,
					      final int requestTimeout) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
		    KeyStoreException, KeyManagementException, IOException
    {
        
        final List<NameValuePair> urlParameters = new ArrayList<>();
        if (params != null)
        {
            for (final String paramName : params.keySet())
            {
                
                LOGGER.trace(String.format("Adding http post params: %s=%s", paramName, params.get(paramName)));
                
                urlParameters.add(new BasicNameValuePair(paramName, params.get(paramName)));
            }
        }
        
        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(urlParameters);
        
        return executeHTTP(url, method, headers, entity, httpsProtocolSupport, keystoreLocation, keystoreType, rootCALocation, keystorePassword,
			ignoreSSLErrors,
                requestTimeout);
    }
    
    public static HttpResponseObj executeHTTP(final String url, final String method, final Map<String, String> headers,
                                              final StringEntity stringEntity, final String[] protocolSupport, final String keystoreLocation,
					      final String keystoreType, final String rootCALocation,
                                              final String keystorePassword, final boolean ignoreSSLErrors, final int requestTimeout) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException
    {
        
        LOGGER.trace(String.format("Executing HTTP request with url: %s, method: %s, isIgnoreSSL: %s", url, method,
                ignoreSSLErrors));
        String[] httpsProtocolSupport = protocolSupport;
        if (httpsProtocolSupport == null || httpsProtocolSupport.length == 0)
        {
            httpsProtocolSupport = DEFAULT_HTTPSPROTOCOLS;
        }
        
        HostnameVerifier hostnameVerifier = DEFAULT_HOSTNAMEVERIFIER;
        
        final SSLContextBuilder sslCtx = SSLContexts.custom();
        
        if (keystoreLocation != null)
        {
            final String[] certFiles = new String[]{rootCALocation};
            
            final KeyStore keystore = KeyStoreCreator.getKeyStore(keystorePassword, keystoreLocation, certFiles, keystoreType);
            
            LOGGER.trace("Attempting to load private credentials");
            
            if (keystore == null)
            {
                
                LOGGER.trace(
                        String.format("New private key found, creating alias '%s' and caching private credentials",
                                keystoreLocation));
                
                keyStoreCache.put(keystoreLocation, keystore);
            } else
            {
                
                LOGGER.trace(String.format("Found keystore in cache for alias '%s'", keystoreLocation));
            }
            
            sslCtx.loadKeyMaterial(keystore, keystorePassword.toCharArray());
        }
        
        //TODO: reintroduce this
        if (ignoreSSLErrors)
        {
            hostnameVerifier = new NoopHostnameVerifier();
            sslCtx.loadTrustMaterial(
                    null,
                    new TrustStrategy()
                    {
                        
                        @Override
                        public boolean isTrusted(final java.security.cert.X509Certificate[] arg0, final String arg1)
                                throws java.security.cert.CertificateException
                        {
                            // TODO Auto-generated method stub
                            return true;
                        }
                    });
        }
        
        final SSLContext sslCtxBuild = sslCtx.build();
        
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslCtxBuild, httpsProtocolSupport,
                null, hostnameVerifier);
        
        final HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        
        final RequestConfig requestCfg = RequestConfig.custom().setConnectTimeout(requestTimeout)
                .setSocketTimeout(requestTimeout).build();
        
        HttpRequestBase request = null;
        switch (method.toUpperCase())
        {
            case "POST":
                
                LOGGER.trace("Initiating post request");
                
                final HttpPost post = new HttpPost(url);
                
                post.setEntity(stringEntity);
                
                request = post;
                
                break;
            case "PUT":
                
                LOGGER.trace("Initiating put request");
                
                final HttpPut put = new HttpPut(url);
                
                put.setEntity(stringEntity);
                
                request = put;
                
                break;
            case "DELETE":
                
                LOGGER.trace("Initiating delete request");
                
                request = new HttpDelete(url);
                break;
            default:
                request = new HttpGet(url);
                break;
        }
        
        if (headers != null)
        {
            // add request header
            for (final String headerName : headers.keySet())
            {
                
                LOGGER.trace(String.format("Adding http header: %s=%s", headerName, headers.get(headerName)));
                
                request.addHeader(headerName, headers.get(headerName));
            }
        }
        
        request.setConfig(requestCfg);
        
        final HttpResponse response = client.execute(request);
        
        if (response == null)
        {
            
            LOGGER.trace("Response is null for the request");
            
            return null;
        }
        
        if (response.getStatusLine().getStatusCode() == 204)
        {
            return new HttpResponseObj(response.getStatusLine().getStatusCode(), null);
        }
        
        final StringBuffer result;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent())))
        {
            result = new StringBuffer();
            
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine())
            {
                result.append(line);
            }
        }
        
        LOGGER.trace("Response is: " + result.toString());
        
        return new HttpResponseObj(response.getStatusLine().getStatusCode(), result.toString());
        
    }
    
}
