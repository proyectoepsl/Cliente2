package com.nfc.proyecto.cliente;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

@SuppressLint("NewApi")
public class Crypt {

    private static final String tag = Crypt.class.getSimpleName();

    private static final String characterEncoding = "UTF-8";
    // Definición del modo de cifrado a utilizar
    private static final String cipherTransformation = "AES/CBC/PKCS5Padding";
    // Definición del tipo de algoritmo a utilizar (AES, DES, RSA)
    private static final String aesEncryptionAlgorithm = "AES";
    //Clave que deben compartir el cliente y el servidor
    private static final String key = "this is my key";
    //Vector de inicialización a utilizar
    private static byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private static byte[] keyBytes;

    private static Crypt instance = null;


    Crypt()
    {
        //Generar vector de inicialización a utilizar
        SecureRandom random = new SecureRandom();
        Crypt.ivBytes = new byte[16];
        random.nextBytes(Crypt.ivBytes);
    }

    public static Crypt getInstance() {
        if(instance == null){
            instance = new Crypt();
        }

        return instance;
    }

    public static Crypt getInstance(String key) {
        if(instance == null){
            instance = new Crypt();

            key = key;
        }

        return instance;
    }
    /**
     * Función de tipo String que recibe una llave (key), un vector de inicialización (ivBytes)
     * y el texto que se desea cifrar(plain)
     * encrypt_string=Devuelve el texto cifrado en modo String
     *Exception puede devolver excepciones de los siguientes tipos: NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException
     */
    public String encrypt_string(final String plain) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
        return Base64.encodeToString(encrypt(plain.getBytes()), Base64.DEFAULT);
    }
    /**
     * Función de tipo String que recibe una llave (key), un vector de inicialización (ivBytes)
     * y el texto que se desea descifrar
     * plain: el texto cifrado en modo String
     * decrypt_string:el texto desencriptado en modo String
     * Puede devolver excepciones de los siguientes tipos: NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException
     */
    public String decrypt_string(final String plain) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
        byte[] encryptedBytes = decrypt(Base64.decode(plain, 0));
        return new String(encryptedBytes);
    }



    public   byte[] encrypt(   byte[] mes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException {

        keyBytes = key.getBytes("UTF-8");
        Log.d(tag,"Long KEY: "+keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();

        Log.d(tag,"Long KEY: "+keyBytes.length);

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = null;
        cipher = Cipher.getInstance(cipherTransformation);

        SecureRandom random = new SecureRandom();
        Crypt.ivBytes = new byte[16];
        random.nextBytes(Crypt.ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, newKey, random);
        byte[] destination = new byte[ivBytes.length + mes.length];
        System.arraycopy(ivBytes, 0, destination, 0, ivBytes.length);
        System.arraycopy(mes, 0, destination, ivBytes.length, mes.length);
        return  cipher.doFinal(destination);

    }

    public   byte[] decrypt(   byte[] bytes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException, ClassNotFoundException {

        keyBytes = key.getBytes("UTF-8");
        Log.d(tag,"Long KEY: "+keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();
        Log.d(tag,"Long KEY: "+keyBytes.length);

        byte[] ivB = Arrays.copyOfRange(bytes,0,16);
        Log.d(tag, "IV: "+new String(ivB));
        byte[] codB = Arrays.copyOfRange(bytes,16,bytes.length);


        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivB);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = Cipher.getInstance(cipherTransformation);
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        byte[] res = cipher.doFinal(codB);
        return  res;

    }


}