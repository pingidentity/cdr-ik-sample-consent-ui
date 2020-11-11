package com.pingidentity.ps.cdr.consentapp.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeyStoreCreator
{
    
    public static KeyStore getKeyStore(final String keystorePass, final String pkFileName, final String[] certFiles) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException
    {
        return getKeyStore(keystorePass, pkFileName, certFiles, "JKS");
        
    }
    
    public static KeyStore getKeyStore(final String keystorePass, final String pkFileName, final String[] certFiles, final String keystoreType) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        final KeyStore keystore = KeyStore.getInstance(keystoreType);
        keystore.load(Files.newInputStream(Paths.get(pkFileName)), keystorePass.toCharArray());
        
        return keystore;
        
    }
    
    public static byte[] getFileBytes(final String filename)
    {
        final File file = new File(filename);
        final byte[] fileBytes = new byte[(int) file.length()];
        
        try (InputStream fis = Files.newInputStream(Paths.get(filename)))
        {
            fis.read(fileBytes);
        } catch (IOException e)
        {
            return null;
        }
        
        return fileBytes;
    }
}