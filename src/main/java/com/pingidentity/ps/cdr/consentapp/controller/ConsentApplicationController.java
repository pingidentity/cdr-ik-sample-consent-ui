package com.pingidentity.ps.cdr.consentapp.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.pingidentity.ps.cdr.consentapp.utils.MASSLClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pingidentity.ps.cdr.consentapp.exceptions.AbstractConsentApplicationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.BadInitialisationException;
import com.pingidentity.ps.cdr.consentapp.exceptions.PFOAuthException;
import com.pingidentity.ps.cdr.consentapp.exceptions.SecurityException;
import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;
import com.pingidentity.ps.cdr.consentapp.scope.ScopeDefinition;
import com.pingidentity.ps.cdr.consentapp.sdk.account.Account;
import com.pingidentity.ps.cdr.consentapp.sdk.account.IAccountRetriever;
import com.pingidentity.ps.cdr.consentapp.sdk.extconsent.IExternalConsentStorage;
import com.pingidentity.ps.cdr.consentapp.utils.PFAgentlessHelper;
import com.pingidentity.ps.cdr.consentapp.utils.PFOAuthMgtHelper;

@Controller
public class ConsentApplicationController
{
    
    private final static Log LOGGER = LogFactory.getLog(MASSLClient.class);
    
    private static final int YEAR_IN_SECONDS = 31_536_000;
    private static final String DATE_FORMAT = "dd MMMM yyyy";
    
    private static final String CLAIM_SCOPE = "signedreqattr.scope";
    private static final String CLAIM_USERNAME = "chainedattr.username";
    private static final String CLAIM_CLIENTID = "com.pingidentity.adapter.input.parameter.oauth.client.id";
    private static final Object CLAIMS_SIGNEDCLAIMS = "signedreqattr.claims";
    private static final String CLAIM_CDRARRANGEMENTID = "signedreqattr.cdr_arrangement_id";
    
    private static final String SESSION_ATTR_RESUME_PATH = "RESUME_PATH";
    private static final String SESSION_LAST_PICKUP_RESULT = "LAST_PICKUP_RESULT";
    private static final String SESSION_AVAILABLE_ACCOUNTS = "AVAILABLE_ACCOUNTS";
    private static final String SESSION_CLIENTID = "CLIENT_ID";
    private static final String SESSION_CLIENTEXTENDEDPARAMS = "CLIENTEXTENDEDPARAMS";
    private static final String SESSION_USERID = "USER_ID";
    private static final String PARAMS_ACCOUNTS = "accounts";
    private static final String PARAMS_DECISION = "approved";
    
    private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

//	private static final String RETURNATTR_CLAIMS_CDRARRANGEMENT = "claims_cdrArrangementId";
    
    @Autowired
    private IAccountRetriever accountRetriever;
    
    @Autowired
    private PFAgentlessHelper pfAgentlessHelper;
    
    @Autowired
    private PFOAuthMgtHelper pfOAuthClientMgtHelper;
    
    @Autowired
    private Map<String, ScopeDefinition> scopeDefinitions;
    
    @Autowired
    private String pingfederateBaseUrl;
    
    @Autowired
    private IExternalConsentStorage externalConsentStorage;
    
    @Autowired
    private String baseUrl;
    
    @GetMapping("/")
    public String home(@RequestParam(value = "errorMsg", required = false) final String errorMsg, final Model model, final HttpSession session) throws AbstractConsentApplicationException
    {
        
        final JSONObject pickupObject = (JSONObject) session.getAttribute(SESSION_LAST_PICKUP_RESULT);
        @SuppressWarnings("unchecked") final List<Account> accountList = (List<Account>) session.getAttribute(SESSION_AVAILABLE_ACCOUNTS);
        
        if (pickupObject == null)
        {
            throw new UnhandledErrorException("Pickup not performed");
        }
        
        final String requestedScopes = (String) pickupObject.get(CLAIM_SCOPE);
        final String[] requestedScopeArr = requestedScopes.split(" ");
        final List<ScopeDefinition> requestedScopeList = new ArrayList<>();
        
        for (final String scope : requestedScopeArr)
        {
            if (!scopeDefinitions.containsKey(scope))
            {
                continue;
            }
            
            requestedScopeList.add(scopeDefinitions.get(scope));
        }
        
        final Instant now = Instant.now();
        
        final Date startDate = Date.from(now);
        
        long durationSeconds = getDurationFromPickup(pickupObject);
        
        if (durationSeconds > YEAR_IN_SECONDS)
        {
            durationSeconds = YEAR_IN_SECONDS;
        }
        
        Date endDate;
        
        if (durationSeconds > 0)
        {
            endDate = Date.from(Instant.now().plusSeconds(durationSeconds));
        } else
        {
            endDate = startDate;
        }
        
        model.addAttribute("requestedScopes", requestedScopeList);
        model.addAttribute("accountList", accountList);
        model.addAttribute("startdate", DATE_FORMATTER.format(startDate));
        model.addAttribute("pingfedBaseUrl", pingfederateBaseUrl);
        model.addAttribute("enddate", DATE_FORMATTER.format(endDate));
        model.addAttribute("errorDescription", errorMsg);
        model.addAttribute("action", baseUrl + "/complete");
        
        final JSONArray clientEntryParams = (JSONArray) session.getAttribute(SESSION_CLIENTEXTENDEDPARAMS);
        
        model.addAttribute("adr_client_uri", getClientEntryParam(clientEntryParams, "client_uri"));
        model.addAttribute("adr_org_id", getClientEntryParam(clientEntryParams, "org_id"));
        model.addAttribute("adr_org_name", getClientEntryParam(clientEntryParams, "org_name"));
        model.addAttribute("adr_tos_uri", getClientEntryParam(clientEntryParams, "tos_uri"));
        model.addAttribute("adr_client_description", getClientEntryParam(clientEntryParams, "client_description"));
        model.addAttribute("adr_policy_uri", getClientEntryParam(clientEntryParams, "policy_uri"));
        
        return "index";
    }
    
    @PostMapping("/init")
    public String init(@RequestParam("resumePath") final String resumePath, final HttpServletRequest request) throws AbstractConsentApplicationException
    {
        
        request.getSession().invalidate();
        
        request.getSession().setAttribute(SESSION_ATTR_RESUME_PATH, resumePath);
        
        final String incomingRefParameter = request.getParameter("REF");
        
        if (StringUtils.isEmpty(incomingRefParameter))
        {
            throw new BadInitialisationException("REF parameter not provided");
        }
        
        final JSONObject pickupResponseJSON = pfAgentlessHelper.pickupRef(incomingRefParameter);
        
        validatePickup(pickupResponseJSON);
        
        final String userId = (String) pickupResponseJSON.get(CLAIM_USERNAME);
        request.getSession().setAttribute(SESSION_USERID, userId);
        
        request.getSession().setAttribute(SESSION_LAST_PICKUP_RESULT, pickupResponseJSON);
        
        final List<Account> accounts = accountRetriever.getAccounts(userId, pickupResponseJSON);
        request.getSession().setAttribute(SESSION_AVAILABLE_ACCOUNTS, accounts);
        
        final String clientId = (String) pickupResponseJSON.get(CLAIM_CLIENTID);
        
        request.getSession().setAttribute(SESSION_CLIENTID, clientId);
        
        final JSONObject clientDetails = pfOAuthClientMgtHelper.getClientDetails(clientId);
        final JSONObject extendedParams = (JSONObject) clientDetails.get("extendedParams");
        final JSONArray extendedParamEntries = (JSONArray) extendedParams.get("entry");
        
        request.getSession().setAttribute(SESSION_CLIENTEXTENDEDPARAMS, extendedParamEntries);
        
        return "redirect:/";
    }
    
    @PostMapping("/complete")
    public String complete(final HttpServletRequest request) throws AbstractConsentApplicationException
    {
        
        final String resumePath = (String) request.getSession().getAttribute(SESSION_ATTR_RESUME_PATH);
        
        String decision = request.getParameter(PARAMS_DECISION);
        if (decision == null)
        {
            decision = "deny";
        }
        
        if ("allow".equals(decision))
        {
            return approveRequest(request, resumePath);
        }
        return denyRequest(request, resumePath);
        
    }
    
    @SuppressWarnings("unchecked")
    private String denyRequest(final HttpServletRequest request, final String resumePath) throws AbstractConsentApplicationException
    {
        final JSONObject dropoffPayload = new JSONObject();
        
        dropoffPayload.put("decision", "deny");
        
        final String refId = this.pfAgentlessHelper.dropoffRef(dropoffPayload);
        
        request.getSession().invalidate();
        
        return "redirect:" + pingfederateBaseUrl + resumePath + "?REF=" + refId;
    }
    
    @SuppressWarnings("unchecked")
    private String approveRequest(final HttpServletRequest request, final String resumePath) throws AbstractConsentApplicationException
    {
        
        final JSONObject pickupObject = (JSONObject) request.getSession().getAttribute(SESSION_LAST_PICKUP_RESULT);
        final String requestedScopeStr = (String) pickupObject.get(CLAIM_SCOPE);
        final String[] requestedScopes = requestedScopeStr.split(" ");
        
        final String[] selectedAccounts = request.getParameterValues(PARAMS_ACCOUNTS);
        if (selectedAccounts == null || selectedAccounts.length == 0)
        {
            String errorDescription = "Please select at least one account.";
            try
            {
                errorDescription = URLEncoder.encode(errorDescription, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                
                LOGGER.error("Unable to encode error description.");
            }
            return "redirect:/?errorMsg=" + errorDescription;
        }
        
        validateSelectedAccounts(request);
        
        final long duration = getDurationFromPickup(pickupObject);
        
        String consentId;
        final String userId = (String) request.getSession().getAttribute(SESSION_USERID);
        try
        {
            consentId = externalConsentStorage.createConsentRecord(userId, duration, selectedAccounts,
                    requestedScopes);
            
            if (consentId == null)
            {
                throw new UnhandledErrorException("Consent Id is null");
            }
            
        } catch (AbstractConsentApplicationException e)
        {
            throw new UnhandledErrorException("There was an error creating a consent record in deepthought", e);
        }
        
        final JSONObject dropoffPayload = new JSONObject();
        
        dropoffPayload.put("accounts", Arrays.asList(selectedAccounts));
        dropoffPayload.put("consent_id", consentId);
        dropoffPayload.put("scopes", requestedScopeStr);
        dropoffPayload.put("subject", userId);
        dropoffPayload.put("decision", "allow");
        
        final String refId = this.pfAgentlessHelper.dropoffRef(dropoffPayload);
        
        request.getSession().invalidate();
        
        return "redirect:" + pingfederateBaseUrl + resumePath + "?REF=" + refId;
    }
    
    private void validateSelectedAccounts(final HttpServletRequest request) throws SecurityException
    {
        @SuppressWarnings("unchecked") final List<Account> allowedAccounts = (List<Account>) request.getSession().getAttribute(SESSION_AVAILABLE_ACCOUNTS);
        final String[] selectedAccounts = request.getParameterValues(PARAMS_ACCOUNTS);
        
        for (final String selectedAccount : selectedAccounts)
        {
            boolean foundAccount = false;
            
            for (final Account allowedAccount : allowedAccounts)
            {
                if (allowedAccount.getId().equals(selectedAccount) && allowedAccount.isOwner())
                {
                    foundAccount = true;
                    break;
                }
            }
            
            if (!foundAccount)
            {
                throw new SecurityException("User attempted to insert account that does not belong to them");
            }
        }
        
    }
    
    private long getDurationFromPickup(final JSONObject pickupObject)
    {
        
        if (!pickupObject.containsKey(CLAIMS_SIGNEDCLAIMS))
        {
            return 0;
        }
        
        final JSONObject signedAttrClaims = (JSONObject) pickupObject.get(CLAIMS_SIGNEDCLAIMS);
        
        if (!signedAttrClaims.containsKey("sharing_duration"))
        {
            return 0;
        }
        
        return (Long) signedAttrClaims.get("sharing_duration");
    }
    
    private String getClientEntryParam(final JSONArray clientEntryParams, final String key)
    {
        for (final Object entryParam : clientEntryParams)
        {
            final JSONObject entryParamJSON = (JSONObject) entryParam;
            
            if (key.equals(entryParamJSON.get("key")) && entryParamJSON.containsKey("value"))
            {
                final JSONObject valueObj = (JSONObject) entryParamJSON.get("value");
                
                return valueObj.get("elements").toString();
            }
            
        }
        
        return null;
    }
    
    private void validatePickup(final JSONObject pickupResponseJSON) throws BadInitialisationException
    {
        
        if (pickupResponseJSON == null)
        {
            throw new BadInitialisationException("Pickup response is null");
        }
        
        if (!pickupResponseJSON.containsKey(CLAIM_USERNAME))
        {
            throw new BadInitialisationException("Unable to determine username from " + CLAIM_USERNAME);
        }
        
        if (!pickupResponseJSON.containsKey(CLAIM_SCOPE))
        {
            throw new BadInitialisationException("Unable to determine scopes from " + CLAIM_SCOPE);
        }
        
        if (!pickupResponseJSON.containsKey(CLAIM_CLIENTID))
        {
            throw new BadInitialisationException("Unable to determine client_id from " + CLAIM_CLIENTID);
        }
        
        if (pickupResponseJSON.containsKey(CLAIM_CDRARRANGEMENTID))
        {
            final String cdrArrangementId = (String) pickupResponseJSON.get(CLAIM_CDRARRANGEMENTID);
            final String expectedClientId = (String) pickupResponseJSON.get(CLAIM_CLIENTID);
            final String expectedUserId = (String) pickupResponseJSON.get(CLAIM_USERNAME);
            JSONObject grantDetails;
            
            try
            {
                grantDetails = this.pfOAuthClientMgtHelper.getGrantDetails(expectedUserId, cdrArrangementId);
            } catch (PFOAuthException e)
            {
                throw new BadInitialisationException("Unable to receive grant record from cdr arrangement id", e);
            }
            
            if (grantDetails == null)
            {
                throw new BadInitialisationException("Specified CDR Arrangement ID returned no results");
            }
            
            if (!grantDetails.containsKey("clientId") || !grantDetails.get("clientId").equals(expectedClientId))
            {
                throw new BadInitialisationException("Specified CDR Arrangement ID does not align with current client ID");
            }
        }
        
    }
    
    
}