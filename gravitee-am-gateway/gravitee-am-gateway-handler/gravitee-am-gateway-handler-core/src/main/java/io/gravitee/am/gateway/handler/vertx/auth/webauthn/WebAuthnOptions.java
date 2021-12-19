/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2019 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.gravitee.am.gateway.handler.vertx.auth.webauthn;

import io.gravitee.am.gateway.handler.vertx.auth.jose.JWS;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.webauthn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.*;

import static io.gravitee.am.gateway.handler.vertx.auth.webauthn.PublicKeyCredential.ES256;
import static io.gravitee.am.gateway.handler.vertx.auth.webauthn.PublicKeyCredential.RS256;
import static io.vertx.ext.auth.webauthn.Attestation.NONE;
import static io.vertx.ext.auth.webauthn.AuthenticatorTransport.*;
import static io.vertx.ext.auth.webauthn.UserVerification.DISCOURAGED;

/**
 * Configuration for the webauthn object
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
// TODO to remove when updating to vert.x 4
public class WebAuthnOptions {
    private static Logger LOGGER = LoggerFactory.getLogger(WebAuthnOptions.class);

    /* Android Keystore Root is not published anywhere.
     * This certificate was extracted from one of the attestations
     * The last certificate in x5c must match this certificate
     * This needs to be checked to ensure that malicious party wont generate fake attestations
     */
    private static final String ANDROID_KEYSTORE_ROOT =
            "MIICizCCAjKgAwIBAgIJAKIFntEOQ1tXMAoGCCqGSM49BAMCMIGYMQswCQYDVQQG" +
                    "EwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNTW91bnRhaW4gVmll" +
                    "dzEVMBMGA1UECgwMR29vZ2xlLCBJbmMuMRAwDgYDVQQLDAdBbmRyb2lkMTMwMQYD" +
                    "VQQDDCpBbmRyb2lkIEtleXN0b3JlIFNvZnR3YXJlIEF0dGVzdGF0aW9uIFJvb3Qw" +
                    "HhcNMTYwMTExMDA0MzUwWhcNMzYwMTA2MDA0MzUwWjCBmDELMAkGA1UEBhMCVVMx" +
                    "EzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDU1vdW50YWluIFZpZXcxFTAT" +
                    "BgNVBAoMDEdvb2dsZSwgSW5jLjEQMA4GA1UECwwHQW5kcm9pZDEzMDEGA1UEAwwq" +
                    "QW5kcm9pZCBLZXlzdG9yZSBTb2Z0d2FyZSBBdHRlc3RhdGlvbiBSb290MFkwEwYH" +
                    "KoZIzj0CAQYIKoZIzj0DAQcDQgAE7l1ex+HA220Dpn7mthvsTWpdamguD/9/SQ59" +
                    "dx9EIm29sa/6FsvHrcV30lacqrewLVQBXT5DKyqO107sSHVBpKNjMGEwHQYDVR0O" +
                    "BBYEFMit6XdMRcOjzw0WEOR5QzohWjDPMB8GA1UdIwQYMBaAFMit6XdMRcOjzw0W" +
                    "EOR5QzohWjDPMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgKEMAoGCCqG" +
                    "SM49BAMCA0cAMEQCIDUho++LNEYenNVg8x1YiSBq3KNlQfYNns6KGYxmSGB7AiBN" +
                    "C/NR2TB8fVvaNTQdqEcbY6WFZTytTySn502vQX3xvw==";

    // https://pki.goog/repository/
    //  Name 	gsr2
    //  Public Key 	RSA
    //  Fingerprint (SHA1) 	69:e2:d0:6c:30:f3:66:16:61:65:e9:1d:68:d1:ce:e5:cc:47:58:4a:80:22:7e:76:66:60:86:c0:10:72:41:eb
    //  Valid Until 	2021-12-15
    private static final String ANDROID_SAFETYNET_ROOT =
            "MIIDvDCCAqSgAwIBAgINAgPk9GHsmdnVeWbKejANBgkqhkiG9w0BAQUFADBMMSAwHgYDVQQLExdH" +
                    "bG9iYWxTaWduIFJvb3QgQ0EgLSBSMjETMBEGA1UEChMKR2xvYmFsU2lnbjETMBEGA1UEAxMKR2xv" +
                    "YmFsU2lnbjAeFw0wNjEyMTUwODAwMDBaFw0yMTEyMTUwODAwMDBaMEwxIDAeBgNVBAsTF0dsb2Jh" +
                    "bFNpZ24gUm9vdCBDQSAtIFIyMRMwEQYDVQQKEwpHbG9iYWxTaWduMRMwEQYDVQQDEwpHbG9iYWxT" +
                    "aWduMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAps8kDr4ubyiZRULEqz4hVJsL03+E" +
                    "cPoSs8u/h1/Gf4bTsjBc1v2t8Xvc5fhglgmSEPXQU977e35ziKxSiHtKpspJpl6op4xaEbx6guu+" +
                    "jOmzrJYlB5dKmSoHL7Qed7+KD7UCfBuWuMW5Oiy81hK561l94tAGhl9eSWq1OV6INOy8eAwImIRs" +
                    "qM1LtKB9DHlN8LgtyyHK1WxbfeGgKYSh+dOUScskYpEgvN0L1dnM+eonCitzkcadG6zIy+jgoPQv" +
                    "kItN+7A2G/YZeoXgbfJhE4hcn+CTClGXilrOr6vV96oJqmC93Nlf33KpYBNeAAHJSvo/pOoHAyEC" +
                    "joLKA8KbjwIDAQABo4GcMIGZMA4GA1UdDwEB/wQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB0GA1Ud" +
                    "DgQWBBSb4gdXZxwewGoG3lm0mi3f3BmGLjAfBgNVHSMEGDAWgBSb4gdXZxwewGoG3lm0mi3f3BmG" +
                    "LjA2BgNVHR8ELzAtMCugKaAnhiVodHRwOi8vY3JsLmdsb2JhbHNpZ24ubmV0L3Jvb3QtcjIuY3Js" +
                    "MA0GCSqGSIb3DQEBBQUAA4IBAQANeX81Z1YqDIs4EaLjG0qPOxIzaJI/y4kiRj3a+y3KOx74clIk" +
                    "LuMgi/9/5iv/n+1LyhGU9g7174slbzJOPbSpp1eT19ST2mYbdgTLx/hm3tTLoHIY/w4ZbnQYwfnP" +
                    "wAG4RefnEFYPQJmpD+Wh8BJwBgtm2drTale/T6NBwmwnEFunfaMfMX3g6IBrx7VKnxIkJh/3p190" +
                    "WveLKgl9n7i5SWce/4woPimEn9WfEQWRvp6wKhaCKFjuCMuulEZusoOUJ4LfJnXxcuQTgIrSnwI7" +
                    "KfSSjsd42w3lX1fbgJp7vPmLM6OBRvAXuYRKTFqMAWbb7OaGIEE+cbxY6PDepnva";

    /**
     * Apple WebAuthn Root CA PEM
     *
     * Downloaded from https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem
     *
     * Valid until 03/14/2045 @ 5:00 PM PST
     */
    private static final String APPLE_WEBAUTHN_ROOT_CA =
            "MIICEjCCAZmgAwIBAgIQaB0BbHo84wIlpQGUKEdXcTAKBggqhkjOPQQDAzBLMR8w" +
                    "HQYDVQQDDBZBcHBsZSBXZWJBdXRobiBSb290IENBMRMwEQYDVQQKDApBcHBsZSBJ" +
                    "bmMuMRMwEQYDVQQIDApDYWxpZm9ybmlhMB4XDTIwMDMxODE4MjEzMloXDTQ1MDMx" +
                    "NTAwMDAwMFowSzEfMB0GA1UEAwwWQXBwbGUgV2ViQXV0aG4gUm9vdCBDQTETMBEG" +
                    "A1UECgwKQXBwbGUgSW5jLjETMBEGA1UECAwKQ2FsaWZvcm5pYTB2MBAGByqGSM49" +
                    "AgEGBSuBBAAiA2IABCJCQ2pTVhzjl4Wo6IhHtMSAzO2cv+H9DQKev3//fG59G11k" +
                    "xu9eI0/7o6V5uShBpe1u6l6mS19S1FEh6yGljnZAJ+2GNP1mi/YK2kSXIuTHjxA/" +
                    "pcoRf7XkOtO4o1qlcaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUJtdk" +
                    "2cV4wlpn0afeaxLQG2PxxtcwDgYDVR0PAQH/BAQDAgEGMAoGCCqGSM49BAMDA2cA" +
                    "MGQCMFrZ+9DsJ1PW9hfNdBywZDsWDbWFp28it1d/5w2RPkRX3Bbn/UbDTNLx7Jr3" +
                    "jAGGiQIwHFj+dJZYUJR786osByBelJYsVZd2GbHQu209b5RCmGQ21gpSAk9QZW4B" +
                    "1bWeT0vT";

    /**
     * Default FIDO2 MDS ROOT Certificate
     */
    String FIDO_MDS_ROOT_CERTIFICATE =
            "MIICQzCCAcigAwIBAgIORqmxkzowRM99NQZJurcwCgYIKoZIzj0EAwMwUzELMAkG" +
                    "A1UEBhMCVVMxFjAUBgNVBAoTDUZJRE8gQWxsaWFuY2UxHTAbBgNVBAsTFE1ldGFk" +
                    "YXRhIFRPQyBTaWduaW5nMQ0wCwYDVQQDEwRSb290MB4XDTE1MDYxNzAwMDAwMFoX" +
                    "DTQ1MDYxNzAwMDAwMFowUzELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUZJRE8gQWxs" +
                    "aWFuY2UxHTAbBgNVBAsTFE1ldGFkYXRhIFRPQyBTaWduaW5nMQ0wCwYDVQQDEwRS" +
                    "b290MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEFEoo+6jdxg6oUuOloqPjK/nVGyY+" +
                    "AXCFz1i5JR4OPeFJs+my143ai0p34EX4R1Xxm9xGi9n8F+RxLjLNPHtlkB3X4ims" +
                    "rfIx7QcEImx1cMTgu5zUiwxLX1ookVhIRSoso2MwYTAOBgNVHQ8BAf8EBAMCAQYw" +
                    "DwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQU0qUfC6f2YshA1Ni9udeO0VS7vEYw" +
                    "HwYDVR0jBBgwFoAU0qUfC6f2YshA1Ni9udeO0VS7vEYwCgYIKoZIzj0EAwMDaQAw" +
                    "ZgIxAKulGbSFkDSZusGjbNkAhAkqTkLWo3GrN5nRBNNk2Q4BlG+AvM5q9wa5WciW" +
                    "DcMdeQIxAMOEzOFsxX9Bo0h4LOFE5y5H8bdPFYW+l5gy1tQiJv+5NUyM2IBB55XU" +
                    "YjdBz56jSA==";

    private List<AuthenticatorTransport> transports;

    private RelyingParty relyingParty;

    private AuthenticatorAttachment authenticatorAttachment;
    private boolean requireResidentKey;
    private UserVerification userVerification;

    private Long timeout;
    private Attestation attestation;

    // Needs to be a list, order is important
    private List<PublicKeyCredential> pubKeyCredParams;

    private int challengeLength;
    private JsonObject extensions;

    private Map<String, X509Certificate> rootCertificates;
    private List<X509CRL> rootCrls;

    public WebAuthnOptions() {
        init();
    }

    public WebAuthnOptions(JsonObject json) {
        this();
        WebAuthnOptionsConverter.fromJson(json, this);
    }

    // sensible defaults
    private void init() {
        userVerification = DISCOURAGED;
        attestation = NONE;
        requireResidentKey = false;
        extensions = new JsonObject()
                .put("txAuthSimple", "");

        timeout = 60_000L;
        challengeLength = 64;
        // Support FIDO2 devices, MACOSX, default
        addPubKeyCredParam(ES256);
        // Support Windows devices (Hello)
        addPubKeyCredParam(RS256);
        // all known transports
        addTransport(USB);
        addTransport(NFC);
        addTransport(BLE);
        addTransport(INTERNAL);
        // default root certificates
        putRootCertificate("android-key", ANDROID_KEYSTORE_ROOT);
        putRootCertificate("android-safetynet", ANDROID_SAFETYNET_ROOT);
        putRootCertificate("apple", APPLE_WEBAUTHN_ROOT_CA);
        putRootCertificate("mds", FIDO_MDS_ROOT_CERTIFICATE);
    }

    public RelyingParty getRelyingParty() {
        return relyingParty;
    }

    public WebAuthnOptions setRelyingParty(RelyingParty relyingParty) {
        if (relyingParty.getName() == null) {
            throw new IllegalArgumentException("RelyingParty name cannot be null");
        }

        this.relyingParty = relyingParty;
        return this;
    }

    public List<AuthenticatorTransport> getTransports() {
        return transports;
    }

    public WebAuthnOptions setTransports(List<AuthenticatorTransport> transports) {
        if (transports == null) {
            throw new IllegalArgumentException("transports cannot be null");
        }

        this.transports = transports;
        return this;
    }

    public WebAuthnOptions addTransport(AuthenticatorTransport transport) {
        if (transport == null) {
            throw new IllegalArgumentException("transport cannot be null");
        }

        if (transports == null) {
            transports = new ArrayList<>();
        }

        this.transports.add(transport);
        return this;
    }

    public Attestation getAttestation() {
        return attestation;
    }

    public WebAuthnOptions setAttestation(Attestation attestation) {
        if (userVerification == null) {
            throw new IllegalArgumentException("userVerification cannot be null");
        }
        this.attestation = attestation;
        return this;
    }

    public List<PublicKeyCredential> getPubKeyCredParams() {
        return pubKeyCredParams;
    }

    public WebAuthnOptions addPubKeyCredParam(PublicKeyCredential pubKeyCredParam) {
        if (pubKeyCredParam == null) {
            throw new IllegalArgumentException("pubKeyCredParam cannot be null");
        }

        if (pubKeyCredParams == null) {
            pubKeyCredParams = new ArrayList<>();
        }
        if (!pubKeyCredParams.contains(pubKeyCredParam)) {
            pubKeyCredParams.add(pubKeyCredParam);
        }
        return this;
    }

    public WebAuthnOptions setPubKeyCredParams(List<PublicKeyCredential> pubKeyCredParams) {
        if (pubKeyCredParams.size() == 0) {
            throw new IllegalArgumentException("PubKeyCredParams must have at least 1 element");
        }
        this.pubKeyCredParams = pubKeyCredParams;
        return this;
    }

    public AuthenticatorAttachment getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public WebAuthnOptions setAuthenticatorAttachment(AuthenticatorAttachment authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
        return this;
    }

    public boolean getRequireResidentKey() {
        return requireResidentKey;
    }

    public WebAuthnOptions setRequireResidentKey(boolean requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
        return this;
    }

    public UserVerification getUserVerification() {
        return userVerification;
    }

    public WebAuthnOptions setUserVerification(UserVerification userVerification) {
        if (userVerification == null) {
            throw new IllegalArgumentException("userVerification cannot be null");
        }
        this.userVerification = userVerification;
        return this;
    }

    public Long getTimeout() {
        return timeout;
    }

    public WebAuthnOptions setTimeout(Long timeout) {
        if (timeout != null) {
            if (timeout < 0) {
                throw new IllegalArgumentException("Timeout must be >= 0");
            }
        }
        this.timeout = timeout;
        return this;
    }

    public int getChallengeLength() {
        return challengeLength;
    }

    public WebAuthnOptions setChallengeLength(int challengeLength) {
        if (challengeLength < 32) {
            throw new IllegalArgumentException("Challenge length must be >= 32");
        }
        this.challengeLength = challengeLength;
        return this;
    }

    public JsonObject getExtensions() {
        return extensions;
    }

    public WebAuthnOptions setExtensions(JsonObject extensions) {
        this.extensions = extensions;
        return this;
    }

    @GenIgnore(GenIgnore.PERMITTED_TYPE)
    public Map<String, X509Certificate> getRootCertificates() {
        return rootCertificates;
    }

    public X509Certificate getRootCertificate(String key) {
        if (rootCertificates == null) {
            return null;
        }
        return rootCertificates.get(key);
    }

    public WebAuthnOptions setRootCertificates(Map<String, String> rootCertificates) {
        if (rootCertificates == null) {
            this.rootCertificates = null;
        } else {
            for (Map.Entry<String, String> kv : rootCertificates.entrySet()) {
                putRootCertificate(kv.getKey(), kv.getValue());
            }
        }
        return this;
    }

    public WebAuthnOptions putRootCertificate(String key, String value) {
        if (rootCertificates == null) {
            rootCertificates = new HashMap<>();
        }
        try {
            X509Certificate cert = JWS.parseX5c(value);
            cert.checkValidity();
            this.rootCertificates.put(key, cert);
            return this;
        } catch (CertificateException e) {
            LOGGER.warn("Root Certificate {} can't be loaded due to {}, please update the certificate.", key, e.getMessage(), e);
            return this;
        }
    }

    @GenIgnore(GenIgnore.PERMITTED_TYPE)
    public List<X509CRL> getRootCrls() {
        return rootCrls;
    }

    public WebAuthnOptions setRootCrls(List<String> rootCrls) {
        if (rootCrls == null) {
            this.rootCrls = null;
        } else {
            for (String value : rootCrls) {
                addRootCrl(value);
            }
        }
        return this;
    }

    public WebAuthnOptions addRootCrl(String value) {
        if (rootCrls == null) {
            rootCrls = new ArrayList<>();
        }
        try {
            X509CRL crl = JWS.parseX5crl(value);
            if (crl.getNextUpdate().before(new Date())) {
                throw new IllegalArgumentException("Expired crl");
            }
            rootCrls.add(crl);
            return this;
        } catch (CRLException e) {
            throw new IllegalArgumentException("Invalid root crl", e);
        }
    }

    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        WebAuthnOptionsConverter.toJson(this, json);
        return json;
    }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}