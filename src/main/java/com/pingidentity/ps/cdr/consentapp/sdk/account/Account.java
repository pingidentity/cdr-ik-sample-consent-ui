package com.pingidentity.ps.cdr.consentapp.sdk.account;

import java.io.Serializable;

import com.pingidentity.ps.cdr.consentapp.exceptions.UnhandledErrorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

public class Account implements Serializable
{
    
    /**
     *
     */
    private static final long serialVersionUID = 2421951039461369957L;
    
    private static Log log = LogFactory.getLog(Account.class);
    
    private final String id;
    private final String openStatus;
    private final String displayName;
    private final String nickName;
    private final Number accountNumber;
    private final Boolean owner;
    
    public static Account createAccount(final Object idObj, final Object openStatusObj, final Object displayNameObj, final Object nickNameObj,
					final Object accountNumberObj, final Object ownerObj) throws UnhandledErrorException
    {
        final String id = getStringValue("id", idObj, true);
        final String openStatus = getStringValue("openStatus", openStatusObj, true);
        final String displayName = getStringValue("displayName", displayNameObj, false);
        final String nickName = getStringValue("nickName", nickNameObj, false);
        final Number accountNumber = getNumberValue("accountNumber", accountNumberObj, true);
        final Boolean owner = getBooleanValue("ownerObj", ownerObj, true);
        
        return new Account(id, openStatus, displayName, nickName, accountNumber, owner);
    }
    
    private static Boolean getBooleanValue(final String name, final Object value, final boolean isRequired) throws UnhandledErrorException
    {
        if (isRequired && value == null)
        {
            final String msg = "Required value not present: " + name;
            log.error(msg);
            throw new UnhandledErrorException(msg);
        } else if (value == null)
        {
            return null;
        }
        
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        } else
        {
            final String msg = "Expected boolean value: " + value;
            log.error(msg);
            throw new UnhandledErrorException(msg);
        }
    }
    
    private static String getStringValue(final String name, final Object value, final boolean isRequired) throws UnhandledErrorException
    {
        if (isRequired && value == null)
        {
            final String msg = "Required value not present: " + name;
            log.error(msg);
            throw new UnhandledErrorException(msg);
        } else if (value == null)
        {
            return null;
        }
        
        return value.toString();
        
    }
    
    private static Number getNumberValue(final String name, final Object value, final boolean isRequired) throws UnhandledErrorException
    {
        if (isRequired && value == null)
        {
            final String msg = "Required value not present: " + name;
            log.error(msg);
            throw new UnhandledErrorException(msg);
        } else if (value == null)
        {
            return null;
        }
        
        if (value instanceof Number)
        {
            return (Number) value;
        } else
        {
            final String msg = "Expected number value: " + value;
            log.error(msg);
            throw new UnhandledErrorException(msg);
        }
        
    }
    
    private Account(final String id, final String openStatus, final String displayName, final String nickName, final Number accountNumber, final Boolean owner)
    {
        this.id = id;
        this.openStatus = openStatus;
        this.displayName = displayName;
        this.nickName = nickName;
        this.accountNumber = accountNumber;
        this.owner = owner;
    }
    
    public String getId()
    {
        return id;
    }
    
    public String getOpenStatus()
    {
        return openStatus;
    }
    
    public String getDisplayName()
    {
        return displayName;
    }
    
    public String getNickName()
    {
        return nickName;
    }
    
    public Number getAccountNumber()
    {
        return accountNumber;
    }
    
    public Boolean isOwner()
    {
        return owner;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public String toString()
    {
        final JSONObject returnObj = new JSONObject();
        returnObj.put("id", this.id);
        returnObj.put("displayname", this.displayName);
        returnObj.put("nickname", this.nickName);
        
        return returnObj.toJSONString();
    }
}
