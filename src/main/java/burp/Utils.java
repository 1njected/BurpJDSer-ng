package burp;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import org.codehaus.jettison.util.StringIndenter;
import org.codehaus.jettison.AbstractXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.json.JSONObject;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;


public class Utils {
    private final PrintStream _stdout;
    private final PrintStream _stderr;
    private final IExtensionHelpers helpers;

    protected ClassLoader loader;
    public final String LIB_DIR = "./libs/";
    private XStream xstream = new XStream();

    Utils(IExtensionHelpers helpers, IBurpExtenderCallbacks callbacks) {
        this.helpers = helpers;
        this._stdout = new PrintStream(callbacks.getStdout());
        this._stderr = new PrintStream(callbacks.getStdout());
    }

    byte[] Serialize(byte[] content) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        xstream.addPermission(AnyTypePermission.ANY);
        xstream.setClassLoader(getSharedClassLoader());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        String xml = helpers.bytesToString(content).replace("\n", "");
        objectOutputStream.writeObject(xstream.fromXML(xml));
        objectOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    byte[] Deserialize(byte[] content) {

        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        // Use a custom OIS that uses our own ClassLoader
        try {
            loader = getSharedClassLoader();
            CustomLoaderObjectInputStream customLoaderObjectInputStream = null;
            try {
                customLoaderObjectInputStream = new CustomLoaderObjectInputStream(bais, loader);
                Object obj = customLoaderObjectInputStream.readObject();
                content = helpers.stringToBytes(xstream.toXML(obj));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(BurpExtender.class.getName()).log(Level.SEVERE, null, ex);
                content = helpers.stringToBytes("Cloud not serialize class.\nEither the input is malformed or you're missing some JARs\n\n" + getStackTrace(ex));
            } finally {
                if (customLoaderObjectInputStream != null) {
                    try {
                        customLoaderObjectInputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(BurpExtender.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(BurpExtender.class.getName()).log(Level.SEVERE, null, ex);
            content = helpers.stringToBytes("Could not initialize class loader:\n\n" + getStackTrace(ex));
        }
        return content;
    }

    protected ClassLoader createURLClassLoader(String libDir) {
        File dependencyDirectory = new File(libDir);
        File[] files = dependencyDirectory.listFiles();
        ArrayList<URL> urls = new ArrayList<>();

        for (File file : files != null ? files : new File[0]) {
            if (file.getName().endsWith(".jar")) {
                try {
                    _stdout.println("Loading: " + file.getName());
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(BurpExtender.class.getName()).log(Level.SEVERE, null, ex);
                    _stderr.println("!! Error loading: " + file.getName());
                }
            }
        }

        return new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
    }

    public ClassLoader getSharedClassLoader() {
        if (loader == null) {
            refreshSharedClassLoader();
        }
        return loader;
    }

    public void refreshSharedClassLoader() {
        loader = createURLClassLoader(LIB_DIR);
    }

    private String getStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        t.printStackTrace(printWriter);
        printWriter.flush();
        stringWriter.flush();

        return stringWriter.toString();
    }

    /**
     * Finds the first occurrence of the pattern in the text.
     */
    public static int indexOf(byte[] data, byte[] pattern) {
        if (data.length == 0) return -1;

        int[] failure = computeFailure(pattern);    
        int j = 0;

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}
