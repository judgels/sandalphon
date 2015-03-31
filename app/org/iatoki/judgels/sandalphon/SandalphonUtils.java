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

    public static List<File> getSortedFilesInDir(File dir) {
        File[] files = dir.listFiles();

        if (files == null) {
            return ImmutableList.of();
        }

        Comparator<String> comparator = new NaturalFilenameComparator();
        Arrays.sort(files, (File a, File b) -> comparator.compare(a.getName(), b.getName()));

        return ImmutableList.copyOf(files);
    }

    public static void uploadZippedFiles(File targetDir, File zippedFiles) {
        byte[] buffer = new byte[4096];
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedFiles));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String filename = ze.getName();
                File file = new File(targetDir, filename);

                // only process outer files
                if (file.isDirectory() || targetDir.getAbsolutePath().equals(file.getParentFile().getAbsolutePath())) {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
