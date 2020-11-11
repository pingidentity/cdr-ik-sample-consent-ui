package com.pingidentity.ps.cdr.consentapp.utils;

public class HttpResponseObj
{
    
    private final int statusCode;
    private final String responseBody;
    
    public HttpResponseObj(final int statusCode, final String responseBody)
    {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public int getStatusCode()
    {
        return statusCode;
    }
    
    public String getResponseBody()
    {
        return responseBody;
    }
}
