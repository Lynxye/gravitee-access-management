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
package io.gravitee.am.gateway.handler.oidc.resources.handler.authorization;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.am.common.exception.oauth2.InvalidRequestException;
import io.gravitee.am.common.oidc.Parameters;
import io.gravitee.am.common.utils.ConstantKeys;
import io.gravitee.am.gateway.handler.oauth2.resources.handler.authorization.AuthorizationRequestParseRequestObjectHandler;
import io.gravitee.am.gateway.handler.oauth2.service.par.PushedAuthorizationRequestService;
import io.gravitee.am.gateway.handler.oidc.service.request.RequestObjectService;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.oidc.Client;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthorizationRequestParseRequestObjectHandlerTest {

    @Mock
    private RequestObjectService roService;

    @Mock
    private PushedAuthorizationRequestService parService;

    @Mock
    private Domain domain;

    private AuthorizationRequestParseRequestObjectHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new AuthorizationRequestParseRequestObjectHandler(roService, domain, parService);
    }

    @Test
    public void shouldExistWithoutProcessing() {
        when(domain.usePlainFapiProfile()).thenReturn(false);

        final RoutingContext context = mock(RoutingContext.class);
        final Client client = new Client();
        client.setRequireParRequest(false);
        when(context.get(ConstantKeys.CLIENT_CONTEXT_KEY)).thenReturn(client);
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getParam(Parameters.REQUEST)).thenReturn(null);
        when(request.getParam(Parameters.REQUEST_URI)).thenReturn(null);

        when(context.request()).thenReturn(request);
        handler.handle(context);

        verify(context).next();
    }

    @Test
    public void shouldFailsWithout_Request_FapiMode() {
        when(domain.usePlainFapiProfile()).thenReturn(true);

        final RoutingContext context = mock(RoutingContext.class);
        final Client client = new Client();
        client.setRequireParRequest(false);
        when(context.get(ConstantKeys.CLIENT_CONTEXT_KEY)).thenReturn(client);
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getParam(Parameters.REQUEST)).thenReturn(null);
        when(request.getParam(Parameters.REQUEST_URI)).thenReturn(null);

        when(context.request()).thenReturn(request);
        handler.handle(context);

        verify(context, never()).next();
        verify(context).fail(argThat(e -> e instanceof  InvalidRequestException));
    }

    @Test
    public void shouldFailsWithout_Request_ParRequired() {
        final RoutingContext context = mock(RoutingContext.class);
        final Client client = new Client();
        client.setRequireParRequest(true);
        when(context.get(ConstantKeys.CLIENT_CONTEXT_KEY)).thenReturn(client);
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getParam(Parameters.REQUEST_URI)).thenReturn(null);

        when(context.request()).thenReturn(request);
        handler.handle(context);

        verify(context, never()).next();
        verify(context).fail(argThat(e -> e instanceof  InvalidRequestException));
    }

    private RSAKey getRSAKey() throws Exception {
        File file = new File(getClass().getClassLoader().getResource("postman_request_object/request_object.key").toURI());
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int) file.length()];
        dis.readFully(keyBytes);
        dis.close();

        String content = IOUtils.readFileToString(file, StandardCharsets.UTF_8);
        return (RSAKey) JWK.parseFromPEMEncodedObjects(content);
    }

    @Test
    public void invalid_request_object() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        String jwt = signedJWT.serialize();
        System.out.println(jwt);
    }

    @Test
    public void invalid_client() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("client_id", "unknown_client")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        System.out.println(new PlainJWT(claimsSet).serialize());
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        String jwt = signedJWT.serialize();
        System.out.println(jwt);
    }

    @Test
    public void invalid_do_not_override_state_and_nonce() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("state", "override-state")
                .claim("nonce", "override-nonce")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        String jwt = signedJWT.serialize();
        System.out.println(jwt);
    }

    @Test
    public void override_max_age() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("max_age", 360000)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        String jwt = signedJWT.serialize();
        System.out.println(jwt);
    }

    @Test
    public void override_redirect_uri() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("redirect_uri", "https://op-test:60001/authz_cb")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        String jwt = signedJWT.serialize();
        System.out.println(jwt);
    }

    @Test
    public void encrypted_request_object() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("redirect_uri", "https://op-test:60001/authz_cb")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-encryption").build(),
                claimsSet);

        signedJWT.sign(signer);

        // Create JWE object with signed JWT as payload
        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .contentType("JWT") // required to indicate nested JWT
                        .build(),
                new Payload(signedJWT));

        // Encrypt with the recipient's public key
        jweObject.encrypt(new RSAEncrypter(rsaKey));

        String jwt = jweObject.serialize();
        System.out.println(jwt);
    }

    @Test
    public void encrypted_override_max_age() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("max_age", 360000)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        // Create JWE object with signed JWT as payload
        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .contentType("JWT") // required to indicate nested JWT
                        .build(),
                new Payload(signedJWT));

        // Encrypt with the recipient's public key
        jweObject.encrypt(new RSAEncrypter(rsaKey));

        String jwt = jweObject.serialize();
        System.out.println(jwt);
    }

    @Test
    public void encrypted_override_redirect_uri() throws Exception {
        RSAKey rsaKey = getRSAKey();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://c2id.com")
                .claim("redirect_uri", "https://op-test:60001/authz_cb")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-signature").build(),
                claimsSet);

        signedJWT.sign(signer);

        // Create JWE object with signed JWT as payload
        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .contentType("JWT") // required to indicate nested JWT
                        .build(),
                new Payload(signedJWT));

        // Encrypt with the recipient's public key
        jweObject.encrypt(new RSAEncrypter(rsaKey));

        String jwt = jweObject.serialize();
        System.out.println(jwt);
    }
}
