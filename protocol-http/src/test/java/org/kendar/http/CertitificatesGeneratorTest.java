package org.kendar.http;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.kendar.http.ssl.CertificatesManager;
import org.kendar.http.ssl.GeneratedCert;
import org.kendar.utils.FileResourcesUtils;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CertitificatesGeneratorTest {
    @Test
    public void generateFromScratch() throws Exception {
        var target = new CertificatesManager(null);
        var extraDomains = new ArrayList<String>();
        GeneratedCert rootCA =
                target.createCertificate("CN=do_not_trust_test_certs_root", null, null, extraDomains, true);
        GeneratedCert issuer =
                target.createCertificate(
                        "CN=do_not_trust_test_certs_issuer", null, rootCA, extraDomains, true);
        GeneratedCert domain =
                target.createCertificate(
                        "CN=local.cergentest.info", "local.cergentest.info", issuer, extraDomains, false);
        GeneratedCert otherD =
                target.createCertificate(
                        "CN=other.cergentest.info", "other.cergentest.info", issuer, extraDomains, false);
    }

    @Test
    public void loadRootCertificate() throws Exception {
        var resourcesLoader = new FileResourcesUtils();
        var target = new CertificatesManager(resourcesLoader);
        var root = target.loadRootCertificate("resource://certificates/ca.der", "resource://certificates/ca.key");
    }

    @Test
    public void generateFromRoot() throws Exception {
        var resourcesLoader = new FileResourcesUtils();
        var target = new CertificatesManager(resourcesLoader);
        var root = target.loadRootCertificate("resource://certificates/ca.der", "resource://certificates/ca.key");
        var extraDomains = new ArrayList<String>();
        GeneratedCert domain =
                target.createCertificate(
                        "CN=local.cergentest.info", "local.cergentest.info", root, extraDomains, false);
        var encodedBytes = domain.certificate.getEncoded();
        final FileOutputStream os = new FileOutputStream("target/local.cergentest.info.cer");
        os.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.write(Base64.encodeBase64(encodedBytes, true));
        os.write("-----END CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.close();
    }

    @Test
    public void exportCertificate() throws Exception {

        var resourcesLoader = new FileResourcesUtils();
        var target = new CertificatesManager(resourcesLoader);
        var root = target.loadRootCertificate("resource://certificates/ca.der", "resource://certificates/ca.key");
        var encodedBytes = root.certificate.getEncoded();

        final FileOutputStream os = new FileOutputStream("target/cert.cer");
        os.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.write(Base64.encodeBase64(encodedBytes, true));
        os.write("-----END CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.close();
    }

    @Test
    public void generateMultiple() throws Exception {

        var resourcesLoader = new FileResourcesUtils();
        var target = new CertificatesManager(resourcesLoader);
        var root = target.loadRootCertificate("resource://certificates/ca.der", "resource://certificates/ca.key");
        var extraDomains = new ArrayList<String>();
        extraDomains.add("*.eu-west-1.tsaws.kendar.org");
        extraDomains.add("*.tsint.kendar.org");
        extraDomains.add("*.kendar.org");
        extraDomains.add("kendar.org");
        GeneratedCert domain =
                target.createCertificate(
                        "CN=kendar.org,O=Local Development, C=US", null, root, extraDomains, false);
        var encodedBytes = domain.certificate.getEncoded();

        final FileOutputStream os = new FileOutputStream("target/kendar.org.cer");
        os.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.write(Base64.encodeBase64(encodedBytes, true));
        os.write("-----END CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.close();
    }

    @Test
    public void generateMultiple2() throws Exception {

        var resourcesLoader = new FileResourcesUtils();
        var target = new CertificatesManager(resourcesLoader);
        var root = target.loadRootCertificate("resource://certificates/ca.der", "resource://certificates/ca.key");
        var extraDomains = new ArrayList<String>();
        extraDomains.add("kendar.org");
        extraDomains.add("*.kendar.org");
        GeneratedCert domain =
                target.createCertificate(
                        "C=US,O=Local Development,CN=kendar.org", null, root, extraDomains, false);
        var encodedBytes = domain.certificate.getEncoded();

        final FileOutputStream os = new FileOutputStream("target/kendar.org2.cer");
        os.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.write(Base64.encodeBase64(encodedBytes, true));
        os.write("-----END CERTIFICATE-----\n".getBytes(StandardCharsets.US_ASCII));
        os.close();
    }
}
