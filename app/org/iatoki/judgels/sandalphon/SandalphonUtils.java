package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableList;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SandalphonUtils {
    private SandalphonUtils() {
        // prevent instantiation
    }


    public static List<String> getDefaultRole() {
        return ImmutableList.of("user");
    }

    public static void saveRoleInSession(List<String> roles) {
        Http.Context.current().session().put("role", StringUtils.join(roles, ","));
    }

    public static boolean hasRole(String role) {
        return Arrays.asList(Http.Context.current().session().get("role").split(",")).contains(role);
    }
}
