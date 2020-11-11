package com.pingidentity.ps.cdr.consentapp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.pingidentity.ps.cdr.consentapp.scope.ScopeDefinition;
import com.pingidentity.ps.cdr.consentapp.sdk.account.IAccountRetriever;
import com.pingidentity.ps.cdr.consentapp.sdk.extconsent.IExternalConsentStorage;
import com.pingidentity.ps.cdr.consentapp.utils.ClassLoaderUtil;
import com.pingidentity.ps.cdr.consentapp.utils.PFAgentlessHelper;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthMgtHelper;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthHelper;

@Configuration
@ComponentScan("com.pingidentity.ps.cdr.consentapp")
public class AppConfig
{
    
    private final Properties configProps;
    
    private final Log log = LogFactory.getLog(this.getClass());
    
    public AppConfig() throws IOException
    {
        configProps = new Properties();
        try (InputStream configPropsIS = ClassLoaderUtil.getResourceAsStream("application.properties",
                this.getClass()))
        {
            
            configProps.load(configPropsIS);
            
        } catch (IOException e)
        {
            log.error("Unable to load config.");
        }
    }
    
    @Bean
    public PFAgentlessHelper pfAgentlessHelper()
    {
        final String pfBaseUrl = pingfederateBaseBackendUrl();
        final String username = pingfederateAgentlessUsername();
        final String password = pingfederateAgentlessPassword();
        final String instanceId = pingfederateAgentlessInstance();
        final String keystoreLocation = pingfederateAgentlessMtlsKeystoreLocation();
        final String keystorePassword = pingfederateAgentlessMtlsKeystorePassword();
        final String trustedCALocation = pingfederateAgentlessMtlsTrustedCALocation();
        final boolean ignoreSSLErrors = pingfederateIgnoreSSL();
        
        return new PFAgentlessHelper(pfBaseUrl, ignoreSSLErrors, username, password, instanceId, keystoreLocation, keystorePassword, trustedCALocation);
    }
    
    @Bean
    public PFOAuthHelper pfOAuthHelper()
    {
        final String pfBaseUrl = pingfederateBaseBackendUrl();
        final String keystoreLocation = pingfederateAgentlessMtlsKeystoreLocation();
        final String keystorePassword = pingfederateAgentlessMtlsKeystorePassword();
        final String trustedCALocation = pingfederateAgentlessMtlsTrustedCALocation();
        final boolean ignoreSSLErrors = pingfederateIgnoreSSL();
        
        return new PFOAuthHelper(pfBaseUrl, ignoreSSLErrors, keystoreLocation, keystorePassword, trustedCALocation);
    }
    
    @Bean
    public PFOAuthMgtHelper pfOAuthClientMgtHelper()
    {
        final String pfBaseUrl = pingfederateBaseBackendUrl();
        final String keystoreLocation = pingfederateAgentlessMtlsKeystoreLocation();
        final String keystorePassword = pingfederateAgentlessMtlsKeystorePassword();
        final String trustedCALocation = pingfederateAgentlessMtlsTrustedCALocation();
        final String username = pingfederateOAuthClientMgtUsername();
        final String password = pingfederateOAuthClientMgtPassword();
        final boolean ignoreSSLErrors = pingfederateIgnoreSSL();
        
        return new PFOAuthMgtHelper(pfBaseUrl, username, password, ignoreSSLErrors, keystoreLocation, keystorePassword, trustedCALocation);
    }
    
    @Bean
    public IAccountRetriever accountRetriever() throws ParseException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException
    {
        final PFOAuthHelper pfOAuthHelper = pfOAuthHelper();
        
        final String config = getConfig("account.retriever.configuration");
        final JSONParser parser = new JSONParser();
        final JSONObject configurationJSON = (JSONObject) parser.parse(config);
        
        final String className = getConfig("account.retriever.class.impl");
        @SuppressWarnings("rawtypes") final Class clazz = Class.forName(className);
        
        @SuppressWarnings({"rawtypes", "unchecked"}) final Constructor constructor =
                clazz.getConstructor(PFOAuthHelper.class, JSONObject.class);
        
        return (IAccountRetriever) constructor.newInstance(pfOAuthHelper, configurationJSON);
    }
    
    @Bean
    public IExternalConsentStorage externalConsentStorage() throws ParseException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException
    {
        final PFOAuthHelper pfOAuthHelper = pfOAuthHelper();
        
        final String config = getConfig("external.consent.configuration");
        final JSONParser parser = new JSONParser();
        final JSONObject configurationJSON = (JSONObject) parser.parse(config);
        
        final String className = getConfig("external.consent.class.impl");
        @SuppressWarnings("rawtypes") final Class clazz = Class.forName(className);
        
        @SuppressWarnings({"rawtypes", "unchecked"}) final Constructor constructor =
                clazz.getConstructor(PFOAuthHelper.class, JSONObject.class, Map.class);
        
        return (IExternalConsentStorage) constructor.newInstance(pfOAuthHelper, configurationJSON, scopeDefinitions());
    }
    
    @Bean
    public Map<String, ScopeDefinition> scopeDefinitions()
    {
        final Map<String, ScopeDefinition> scopeDefinitions = new HashMap<>();
        
        for (final Object key : configProps.keySet())
        {
            final String keyStr = key.toString();
            
            if (!keyStr.startsWith("cdr.scope.") || !keyStr.endsWith(".title"))
            {
                continue;
            }
            
            final String scopeKeyPrefix = keyStr.replace(".title", "");
            
            final String scope = configProps.getProperty(scopeKeyPrefix + ".value");
            final String title = configProps.getProperty(scopeKeyPrefix + ".title");
            final String shortReason = configProps.getProperty(scopeKeyPrefix + ".shortReason");
            final String longReason = configProps.getProperty(scopeKeyPrefix + ".longReason");
            
            final ScopeDefinition scopeDefinition = new ScopeDefinition(scope, title, shortReason, longReason);
            
            scopeDefinitions.put(scope, scopeDefinition);
            
        }
        
        return scopeDefinitions;
    }
    
    @Bean
    public String baseUrl()
    {
        return getConfig("server.baseurl");
    }
    
    @Bean
    public String pingfederateBaseUrl()
    {
        return getConfig("pf.baseurl");
    }
    
    @Bean
    public String pingfederateBaseBackendUrl()
    {
        return getConfig("pf.baseurl.backend");
    }
    
    private String pingfederateOAuthClientMgtUsername()
    {
        return getConfig("pf.oauth.client.mgt.username");
    }
    
    private String pingfederateOAuthClientMgtPassword()
    {
        return getConfig("pf.oauth.client.mgt.password");
    }
    
    private boolean pingfederateIgnoreSSL()
    {
        final String config = getConfig("pf.ignoresslerrors");
        
        return Boolean.parseBoolean(config);
    }
    
    private String pingfederateAgentlessUsername()
    {
        return getConfig("pf.agentless.username");
    }
    
    private String pingfederateAgentlessPassword()
    {
        return getConfig("pf.agentless.password");
    }
    
    private String pingfederateAgentlessInstance()
    {
        return getConfig("pf.agentless.instanceid");
    }
    
    private String pingfederateAgentlessMtlsKeystoreLocation()
    {
        return getConfig("pf.agentless.mtls.keystore.location");
    }
    
    private String pingfederateAgentlessMtlsKeystorePassword()
    {
        return getConfig("pf.agentless.mtls.keystore.password");
    }
    
    private String pingfederateAgentlessMtlsTrustedCALocation()
    {
        return getConfig("pf.agentless.mtls.trustedca.location");
    }
    
    private String getConfig(final String configName)
    {
        String envName = "CONSENT_APP-" + configName.replaceAll("\\.", "-");
        
        if (System.getenv(envName) != null && !System.getenv(envName).isEmpty())
        {
            log.info("Reading config from envVar: " + envName);
            return System.getenv(envName);
        }
        
        envName = "CONSENT_APP_" + configName.replaceAll("\\.", "_");
        
        if (System.getenv(envName) != null && !System.getenv(envName).isEmpty())
        {
            log.info("Reading config from envVar: " + envName);
            return System.getenv(envName);
        }
        
        return configProps.getProperty(configName);
    }
    
}
