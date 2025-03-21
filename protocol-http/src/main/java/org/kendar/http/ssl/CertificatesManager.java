package org.kendar.http.ssl;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.kendar.utils.FileResourcesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CertificatesManager {

    public static final String PASSPHRASE = "passphrase";
    public static final String PRIVATE_CERT = "privateCert";
    private static final Logger sslLog = LoggerFactory.getLogger("org.kendar.http.SSL");
    private static final String BC_PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private final FileResourcesUtils fileResourcesUtils;
    private final ConcurrentHashMap<String, String> certificateHosts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> registeredHosts = new ConcurrentHashMap<>();
    private GeneratedCert caCertificate;

    public CertificatesManager(
            FileResourcesUtils fileResourcesUtils) {
        this.fileResourcesUtils = fileResourcesUtils;
        Provider aProvider = Security.getProvider("BC");
        if (aProvider == null) {
            updateProvider();
        }
    }

    private static KeyStore setupKeystore(GeneratedCert domain)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ksTemp = KeyStore.getInstance("jks"); //PKCS12
        ksTemp.load(null, null); // Initialize it
        ksTemp.setCertificateEntry("Alias", domain.certificate);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        // save the temp keystore
        ksTemp.store(bOut, PASSPHRASE.toCharArray());
        // Now create the keystore to be used by jsse
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new ByteArrayInputStream(bOut.toByteArray()), PASSPHRASE.toCharArray());
        return keyStore;
    }

    public static GeneratedCert generateRootCertificate(String cnName, GeneratedCert issuer) throws Exception {
        // Initialize a new KeyPair generator
        var keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
        keyPairGenerator.initialize(2048);

        // Setup start date to yesterday and end date for 1 year validity
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        var startDate = calendar.getTime();

        calendar.add(Calendar.YEAR, 20);
        var endDate = calendar.getTime();

        // First step is to create a root certificate
        // First Generate a KeyPair,
        // then a random serial number
        // then generate a certificate using the KeyPair
        var rootKeyPair = keyPairGenerator.generateKeyPair();
        BigInteger rootSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        // Issued By and Issued To same for root certificate
        var rootCertIssuer = new X500Name(cnName);
        ContentSigner rootCertContentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER).build(rootKeyPair.getPrivate());
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(rootCertIssuer, rootSerialNum, startDate, endDate, rootCertIssuer, rootKeyPair.getPublic());

        // Add Extensions
        // A BasicConstraint to mark root certificate as CA certificate
        JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils();
        rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        rootCertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                rootCertExtUtils.createSubjectKeyIdentifier(rootKeyPair.getPublic()));


        rootCertBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(
                        new KeyPurposeId[]{
                                KeyPurposeId.id_kp_serverAuth,
                                KeyPurposeId.id_kp_clientAuth,
                                KeyPurposeId.id_kp_codeSigning,
                                KeyPurposeId.id_kp_timeStamping,
                                KeyPurposeId.id_kp_emailProtection,}));

        rootCertBuilder.addExtension(
                Extension.keyUsage,
                false,
                new X509KeyUsage(
                        X509KeyUsage.digitalSignature
                                | X509KeyUsage.nonRepudiation
                                | X509KeyUsage.cRLSign
                                | X509KeyUsage.keyCertSign
                                | X509KeyUsage.keyAgreement
                                | X509KeyUsage.keyEncipherment
                                | X509KeyUsage.dataEncipherment));

        // Create a cert holder and export to X509Certificate
        X509CertificateHolder rootCertHolder = rootCertBuilder.build(rootCertContentSigner);
        var rootCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(rootCertHolder);
        return new GeneratedCert(rootKeyPair.getPrivate(), rootCert);

    }

    private SSLContext getSslContext(List<String> hosts, String cname, String der, String key) throws Exception {
        for (var host : hosts) {
            certificateHosts.put(host, host);
        }
        var newHostsList = new ArrayList<>(certificateHosts.keySet());
        var root =
                loadRootCertificate(der, key);

        GeneratedCert domain =
                createCertificate(
                        cname,
                        null,
                        root,
                        newHostsList,
                        false);

        KeyStore keyStoreTs = setupKeystore(domain);
        // now lets do the same with the keystore
        KeyStore keyStore = setupKeystore(domain);
        // HERE IS THE CHAIN
        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = domain.certificate;
        keyStore.setKeyEntry(PRIVATE_CERT, domain.privateKey, PASSPHRASE.toCharArray(), chain);

        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStoreTs);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, PASSPHRASE.toCharArray());

        // create SSLContext to establish the secure connection
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ctx;
    }

    public void unsetSll(HttpsServer port, List<String> inserted, String cname, String der, String key) throws Exception {
        var hostsSize = registeredHosts.size();
        var changed = false;
        for (var host : inserted) {
            if (registeredHosts.containsKey(host)) {
                registeredHosts.remove(host);
                changed = true;
            }
        }
        if (!changed) return;
        reinitializeSslContext(port, inserted, cname, der, key, hostsSize);
    }

    private void reinitializeSslContext(HttpsServer port, List<String> inserted, String cname, String der, String key, int hostsSize) throws Exception {
        if (hostsSize == registeredHosts.size() && port.getHttpsConfigurator() != null) {
            return;
        }

        sslLog.debug("[SERVER] Changed ssl hosts: {}", String.join(",", inserted));
        var sslContextInt = getSslContext(new ArrayList<>(registeredHosts.values()), cname, der, key);
        port.setHttpsConfigurator(
                new HttpsConfigurator(sslContextInt) {
                    @Override
                    public void configure(HttpsParameters params) {
                        try {
                            // initialise the SSL context

                            SSLEngine engine = sslContextInt.createSSLEngine();
                            params.setNeedClientAuth(false);
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());

                            // Set the SSL parameters
                            SSLParameters sslParameters = sslContextInt.getSupportedSSLParameters();
                            params.setSSLParameters(sslParameters);

                        } catch (Exception ex) {
                            sslLog.error("Error configuring https", ex);
                        }
                    }
                });
    }

    public void setupSll(HttpsServer port, List<String> hosts, String cname, String der, String key) throws Exception {
        var hostsSize = registeredHosts.size();
        var inserted = new ArrayList<String>();
        var changed = false;
        for (var host : hosts) {
            if (host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1")) {
                continue;
            }
            var hstSpl = host.split("\\.");
            if (hstSpl.length > 2) {
                StringBuilder newHost = new StringBuilder("*");
                for (var i = 1; i < hstSpl.length; i++) {
                    newHost.append(".").append(hstSpl[i]);
                }
                if (!registeredHosts.containsKey(newHost.toString())) {
                    changed = true;
                    inserted.add(newHost.toString());
                    registeredHosts.put(newHost.toString(), newHost.toString());
                }
            }

            if (!registeredHosts.containsKey(host)) {
                changed = true;
                inserted.add(host);
                registeredHosts.put(host, host);
            }
        }
        if (!changed) return;
        reinitializeSslContext(port, inserted, cname, der, key, hostsSize);
    }

    public void updateProvider() {
        //Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.addProvider(new BouncyCastleProvider());
    }

    public GeneratedCert getCaCertificate() {
        return caCertificate;
    }

    public GeneratedCert loadRootCertificate(String derFile, String keyFile)
            throws CertificateException, IOException {
        var caStream = fileResourcesUtils.getFileFromResourceAsStream(derFile);
        var certificateFactory = CertificateFactory.getInstance("X.509");
        var certificate = (X509Certificate) certificateFactory.generateCertificate(caStream);

        var keyStream = fileResourcesUtils.getFileFromResourceAsStream(keyFile);

        PEMParser pemParser = new PEMParser(new InputStreamReader(keyStream));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        var privateKey = kp.getPrivate();

        caCertificate = new GeneratedCert(privateKey, certificate);
        return caCertificate;
    }

    private GeneratedCert createSNACertificate(String cnName,
                                               String rootDomain,
                                               GeneratedCert issuer,
                                               List<String> childDomains) throws Exception {

        // Generate the key-pair with the official Java API's
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair certKeyPair = keyGen.generateKeyPair();
        X500Name name = new X500Name(cnName);

        // If you issue more than just test certificates, you might want a decent serial number schema
        // ^.^
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Instant now = Instant.now();
        Instant validFrom = now.minus(360, ChronoUnit.DAYS);
        Instant validUntil = now.plus(360, ChronoUnit.DAYS);

        // If there is no issuer, we self-sign our certificate.
        X500Name issuerName;
        PrivateKey issuerKey;
        if (issuer == null) {
            issuerName = name;
            issuerKey = certKeyPair.getPrivate();
        } else {
            issuerName = new X500Name(issuer.certificate.getSubjectDN().getName());
            issuerKey = issuer.privateKey;
        }

        // The cert builder to build up our certificate information
        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        issuerName,
                        serialNumber,
                        Date.from(validFrom),
                        Date.from(validUntil),
                        name,
                        certKeyPair.getPublic());

        // Make the cert to a Cert Authority to sign more certs when needed

        byte[] bytes = new byte[20];
        SecureRandom.getInstanceStrong().nextBytes(bytes);
        SubjectKeyIdentifier securityKeyIdentifier =
                new SubjectKeyIdentifier(bytes);
        builder.addExtension(Extension.subjectKeyIdentifier, false, securityKeyIdentifier);

        // Modern browsers demand the DNS name entry
        if (rootDomain != null) {
            builder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName, rootDomain)));
        } else if (!childDomains.isEmpty()) {
            var generalNames = new GeneralName[childDomains.size()];
            for (int i = 0; i < childDomains.size(); i++) {
                generalNames[i] = new GeneralName(GeneralName.dNSName, childDomains.get(i));
            }

            // GeneralNames subjectAltNames = GeneralNames.getInstance(generalNames);
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames));
        }
        if (issuer != null) {
            byte[] extvalue =
                    //issuer.certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
                    issuer.certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
            if (extvalue != null) {
                byte[] filteredByteArray =
                        Arrays.copyOfRange(extvalue, extvalue.length - 20, extvalue.length);

                AuthorityKeyIdentifier authorityKeyIdentifier =
                        new AuthorityKeyIdentifier(filteredByteArray);
                builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
            }
            builder.addExtension(
                    new ASN1ObjectIdentifier("2.5.29.19"), false, new BasicConstraints(false));
            builder.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    new ExtendedKeyUsage(
                            new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));

            builder.addExtension(
                    Extension.keyUsage,
                    false,
                    new X509KeyUsage(
                            X509KeyUsage.digitalSignature
                                    | X509KeyUsage.nonRepudiation
                                    | X509KeyUsage.keyEncipherment
                                    | X509KeyUsage.dataEncipherment));
        }

        // Finally, sign the certificate:
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(issuerKey);
        //ContentSigner signer = new JcaContentSignerBuilder("SHA256").build(issuerKey);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

        return new GeneratedCert(certKeyPair.getPrivate(), cert);
    }

    public GeneratedCert createCertificate(
            String cnName,
            String rootDomain,
            GeneratedCert issuer,
            List<String> childDomains,
            boolean isCa)
            throws Exception {
        if (!isCa) {
            return createSNACertificate(cnName, rootDomain, issuer, childDomains);
        } else {
            return generateRootCertificate(cnName, issuer);
        }
    }
}
