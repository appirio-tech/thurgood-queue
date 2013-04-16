package com.cloudspokes.squirrelforce.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

/**
 * Framework-independent I/O utilities.
 */
public class IOUtils {

    private static final int READ_BUFFER_SIZE = 8096;
    private static final int MAX_HTTP_REQUEST_TIMEOUT = 0; // infinite
    private static final String HTTP_GET_METHOD = "GET";
    private static final String TEXT_FILE_ENCODING = "UTF-8";
    private static final String TEMPDIR_PROPERTY = "java.io.tmpdir";
    private static final String DEFAULT_TEMPDIR_IF_NONE_SYSTEM = ".";
    private static final String TEMP_PATH_PREFIX = "octavius3-";

    private IOUtils() {
        // singleton helper class
    }

    public static File downloadFromUrlToTempFile(String url) {
        File tempFile = new File(randomTempPath());
        try {
            OutputStream outputStream = new FileOutputStream(tempFile);
            try {
                copy(httpGet(url), outputStream);
                return tempFile;

            } finally {
                outputStream.close();
            }

        } catch (Exception e) {
            tempFile.delete();
            throw new RuntimeException(e);
        }
    }

    private static InputStream httpGet(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setReadTimeout(MAX_HTTP_REQUEST_TIMEOUT);
            conn.setRequestMethod(HTTP_GET_METHOD);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpServletResponse.SC_OK) {
                String responseMessage = conn.getResponseMessage();
                throw new RuntimeException(responseCode + "(" + responseMessage + ") error on " + HTTP_GET_METHOD + " " + url);
            }
            return conn.getInputStream();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File unzipToTempDir(File zip) {
        File tempDir = new File(randomTempPath());
        tempDir.mkdir();

        try {
            String outDir = tempDir.getCanonicalPath();
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File fileEntry = new File(tempDir, entry.getName());

                if (entry.isDirectory()) {
                    fileEntry.mkdir();

                } else { // file
                    if(!fileEntry.getCanonicalPath().startsWith(outDir)) {
                        throw new RuntimeException(
                            "Corrupted zip file with entry: " + fileEntry.getCanonicalPath());
                    }
                    fileEntry.getParentFile().mkdirs();
                    FileOutputStream os = new FileOutputStream(fileEntry);
                    copy(zipFile.getInputStream(entry), os);
                    os.close();
                }
            }

            zipFile.close();
            return tempDir;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String randomTempPath() {
        String path = System.getProperty(TEMPDIR_PROPERTY, DEFAULT_TEMPDIR_IF_NONE_SYSTEM);
        if (!path.endsWith(String.valueOf(File.separatorChar))) {
            path += File.separatorChar;
        }
        path += TEMP_PATH_PREFIX + UUID.randomUUID();
        return path;
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();

            for (File file: files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }

            dir.delete();
        }
    }

    public static String readContentAsText(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            StringWriter binary = new StringWriter();
            PrintWriter out = new PrintWriter(binary);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName(TEXT_FILE_ENCODING)));
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            return binary.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            safeClose(is);
        }
    }

    public static String readContentAsBase64(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] binary = new byte[(int) file.length()];
            is.read(binary);
            return new String(Base64.encodeBase64(binary));

        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            safeClose(is);
        }
    }

    private static void copy(InputStream is, OutputStream os) {
        try {
            int bytesRead;
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            while ((bytesRead = is.read(buffer)) > 0) {
                os.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            safeClose(is);
        }
    }

    private static void safeClose(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
