package com.example.recepiesuggestor.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public class ApiKeyManager {
    private static final String KEYSTORE_ALIAS = "GeminiApiKeyAlias";
    private static final String PREFS_NAME = "secure_prefs";
    private static final String ENCRYPTED_KEY_PREF = "encrypted_gemini_key";
    private static final String IV_PREF = "gemini_key_iv";

    private static ApiKeyManager instance;
    private final Context context;

    private ApiKeyManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ApiKeyManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiKeyManager(context);
        }
        return instance;
    }

    public void storeApiKey(String apiKey) {
        try {
            generateSecretKey();
            String encryptedKey = encryptApiKey(apiKey);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(ENCRYPTED_KEY_PREF, encryptedKey).apply();
        } catch (Exception e) {
            Log.e("ApiKeyManager", "Failed to store API key", e);
        }
    }

    public String getApiKey() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String encryptedKey = prefs.getString(ENCRYPTED_KEY_PREF, null);

            if (encryptedKey != null) {
                return decryptApiKey(encryptedKey);
            }
        } catch (Exception e) {
            Log.e("ApiKeyManager", "Failed to retrieve API key", e);
        }
        return null;
    }

    private void generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false)
                .build();
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    private String encryptApiKey(String apiKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV();
        byte[] encryptedData = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(IV_PREF, Base64.encodeToString(iv, Base64.DEFAULT)).apply();

        return Base64.encodeToString(encryptedData, Base64.DEFAULT);
    }

    private String decryptApiKey(String encryptedKey) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String ivString = prefs.getString(IV_PREF, null);

        if (ivString == null) return null;

        byte[] iv = Base64.decode(ivString, Base64.DEFAULT);
        byte[] encryptedData = Base64.decode(encryptedKey, Base64.DEFAULT);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}