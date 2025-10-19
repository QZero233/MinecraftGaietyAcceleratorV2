package com.qzero.mcga.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Utils {

    public static String getFileSHA256(File file) throws Exception{
        InputStream fis = new FileInputStream(file);
        byte buffer[] = new byte[10240];
        MessageDigest md5 = MessageDigest.getInstance("SHA-256");
        for (int numRead = 0; (numRead = fis.read(buffer)) > 0; ) {
            md5.update(buffer, 0, numRead);
        }
        fis.close();

        return byte2Hex(md5.digest());
    }

    public static byte[] getSHA256(byte[] data){
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // sha256 of "1" is
    // 6B:86:B2:73:FF:34:FC:E1:9D:6B:80:4E:FF:5A:3F:57:47:AD:A4:EA:A2:2F:1D:49:C0:1E:52:DD:B7:87:5B:4B
    public static String getHexEncodedSHA256(String data){
        byte[] buf=getSHA256(data.getBytes());
        if(buf==null)
            return null;

        return byte2Hex(buf).toUpperCase();
    }



    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                //1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
            stringBuffer.append(":");
        }
        stringBuffer.deleteCharAt(stringBuffer.length()-1);
        return stringBuffer.toString();
    }

}