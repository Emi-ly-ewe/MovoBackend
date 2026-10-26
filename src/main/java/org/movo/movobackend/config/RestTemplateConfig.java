package org.movo.movobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateConfig {

    /**
     * www.titsa.com serves an incomplete chain (leaf only, no Sectigo intermediate).
     * Browsers fix this via AIA fetching, Java does not -> PKIX path building failed.
     * We bundle the missing intermediate + its root and trust them in addition to the JDK defaults.
     */
    private static final String[] EXTRA_CERTS = {
            "certs/sectigo-ov-r36.pem",
            "certs/sectigo-root-r46.pem"
    };

    @Bean
    public RestTemplate restTemplate() throws GeneralSecurityException, IOException {
        SSLSocketFactory sslSocketFactory = buildSslContext().getSocketFactory();

        // Without timeouts a slow upstream (TITSA / Metro) can hold a Tomcat thread forever
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection https) {
                    https.setSSLSocketFactory(sslSocketFactory);
                }
                super.prepareConnection(connection, httpMethod);
            }
        };
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    private static SSLContext buildSslContext() throws GeneralSecurityException, IOException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        // 1) Everything the JDK already trusts
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);
        int i = 0;
        for (TrustManager tm : defaultTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509) {
                for (X509Certificate ca : x509.getAcceptedIssuers()) {
                    trustStore.setCertificateEntry("jdk-" + i++, ca);
                }
            }
        }

        // 2) Bundled certs to complete TITSA's broken chain
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (String path : EXTRA_CERTS) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                for (Certificate cert : cf.generateCertificates(in)) {
                    trustStore.setCertificateEntry("extra-" + i++, cert);
                }
            }
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }
}
