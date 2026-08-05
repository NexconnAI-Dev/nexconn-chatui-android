package ai.nexconn.chatui.utils.system;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Utility for certificate authentication with self-signed certificates.
 *
 * <p>When using self-signed certificates, call {@link #setHostnameVerifier(HostnameVerifier)} and
 * {@link #setSSLContext(SSLContext)} to configure certificate verification.
 *
 * <p>Must be called before SDK initialization.
 *
 * <p>Must also be configured in processes other than the main process.
 */
public class SSLUtils {
    private static HostnameVerifier sHostnameVerifier;
    private static SSLContext sSSLContext;
    private static SSLSocketFactory sslSocketFactory;

    public static SSLContext getSSLContext() {
        return sSSLContext;
    }

    public static void setSSLContext(SSLContext sslContext) {
        sSSLContext = sslContext;
    }

    public static void setHostnameVerifier(HostnameVerifier verifier) {
        sHostnameVerifier = verifier;
    }

    public static HostnameVerifier getHostVerifier() {
        return sHostnameVerifier;
    }

    public static SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public static void setSslSocketFactory(SSLSocketFactory factory) {
        sslSocketFactory = factory;
    }
}
