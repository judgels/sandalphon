package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public final class SandalphonUtils {
    private SandalphonUtils() {
        // prevent instantiation
    }

    public static String timestampToFormattedDate(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd - HH:mm");
        return formatter.format(timestamp);
    }

    public static String byteCountToFormattedSize(long bytes) {
        return FileUtils.byteCountToDisplaySize(bytes);
    }

    public static String hashSHA256(String s) {
        return messageDigest(s, "SHA-256");
    }

    public static String hashMD5(String s) {
        return messageDigest(s, "MD5");
    }

    private static String messageDigest(String s, String algorithm) {
        byte[] hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            hash = md.digest(s.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // No way this thing will ever happen
            e.printStackTrace();
        }
        return new String(Hex.encodeHex(hash));
    }

    public static List<String> getDefaultRole() {
        return ImmutableList.of("user");
    }

    public static void saveRoleInSession(List<String> roles) {
        System.out.println(roles);
        System.out.println("AAAAAA");
        Http.Context.current().session().put("role", StringUtils.join(roles, ","));
    }

    public static boolean hasRole(String role) {
        return Arrays.asList(Http.Context.current().session().get("role").split(",")).contains(role);
    }
}
