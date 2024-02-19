/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.jsonld.loader.SchemeRouter;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.ld.signature.method.MethodResolver;
import com.apicatalog.ld.signature.method.VerificationMethod;
import com.apicatalog.vc.Vc;
import com.apicatalog.vc.integrity.DataIntegrityProofOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.VerifierContext;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.security.signature.jws2020.JwkMethod;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.security.signature.jws2020.JwsSignatureProofOptions;
import org.eclipse.edc.security.signature.jws2020.TestDocumentLoader;
import org.eclipse.edc.security.signature.jws2020.TestFunctions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;

import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.MEMBERSHIP_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.NAME_CREDENTIAL_ISSUER;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.VC_CONTENT_CERTIFICATE_EXAMPLE;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createMembershipCredential;
import static org.eclipse.edc.verifiablecredentials.linkeddata.TestData.createNameCredential;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdpVerifierTest {

    private static final String VP_HOLDER = "did:web:vp-holder";
    private final ObjectMapper mapper = createObjectMapper();
    private final TestDocumentLoader testDocLoader = new TestDocumentLoader("https://org.eclipse.edc/", "", SchemeRouter.defaultInstance());
    private final MethodResolver mockDidResolver = mock();

    private final SignatureSuiteRegistry suiteRegistry = mock();
    private VerifierContext context = null;
    private LdpVerifier ldpVerifier;
    private TitaniumJsonLd jsonLd;

    @Nested
    class JsonWebSignature2020 {

        private final JwsSignature2020Suite jwsSignatureSuite = new JwsSignature2020Suite(mapper);

        @BeforeEach
        void setUp() throws URISyntaxException {
            jsonLd = new TitaniumJsonLd(mock());
            jsonLd.registerCachedDocument("https://www.w3.org/ns/odrl.jsonld", Thread.currentThread().getContextClassLoader().getResource("odrl.jsonld").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/ns/did/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
            jsonLd.registerCachedDocument("https://w3id.org/security/suites/jws-2020/v1", Thread.currentThread().getContextClassLoader().getResource("jws2020.json").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/v1", Thread.currentThread().getContextClassLoader().getResource("credentials.v1.json").toURI());
            jsonLd.registerCachedDocument("https://www.w3.org/2018/credentials/examples/v1", Thread.currentThread().getContextClassLoader().getResource("examples.v1.json").toURI());
            ldpVerifier = LdpVerifier.Builder.newInstance()
                    .signatureSuites(suiteRegistry)
                    .jsonLd(jsonLd)
                    .objectMapper(mapper)
                    .methodResolvers(List.of(mockDidResolver))
                    .loader(testDocLoader)
                    .build();
            context = VerifierContext.Builder.newInstance().verifier(ldpVerifier).build();

            when(suiteRegistry.getForId(any())).thenReturn(jwsSignatureSuite);
        }

        private DataIntegrityProofOptions generateEmbeddedProofOptions(ECKey vcKey, String id) {
            return jwsSignatureSuite
                    .createOptions()
                    .created(Instant.now())
                    .verificationMethod(TestFunctions.createKeyPair(vcKey, id)) // embedded proof
                    .purpose(URI.create("https://w3id.org/security#assertionMethod"));
        }

        @Nested
        class Presentations {
            @Test
            void verify_noCredentials_success() throws JOSEException {
                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted("");

                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);

                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();
            }

            @Test
            void verify_singleValidCredentials_success() throws JOSEException {
                // create signed VC
                var vcKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key")
                        .generate();
                var rawVc = LdpCreationUtils.signDocument(VC_CONTENT_CERTIFICATE_EXAMPLE, vcKey, generateEmbeddedProofOptions(vcKey, "did:web:test-issuer"), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(rawVc);

                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);

                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();

            }

            @Test
            void verify_multipleValidCredentials_success() throws JOSEException {
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProofOptions(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isSucceeded();
            }

            @Test
            void verify_singleInvalidVc_shouldFail() throws JOSEException {
                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProofOptions(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

                // create name credential, but altered after-the-fact
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER), testDocLoader);
                // now change the name
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");

            }

            @Test
            void verify_multipleInvalidVc_shouldSucceed() throws JOSEException {
                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProofOptions(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);
                // tamper with the status -> causes signature failure!
                signedMembershipCred = signedMembershipCred.replace("active", "super-active");

                // create name credential, but altered after-the-fact
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER), testDocLoader);
                // tamper with the name -> causes signature failure!
                signedNameCredential = signedNameCredential.replace("Test Person III", "Test Person IV");

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);

                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_forgedPresentation_shouldFail() throws JOSEException {
                // create IsoCertificate VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER), testDocLoader);

                // create Membership credential
                var membershipCredential = createMembershipCredential();
                var membershipKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("dataspace-issuance-key1")
                        .generate();
                var signedMembershipCred = LdpCreationUtils.signDocument(membershipCredential, membershipKey, generateEmbeddedProofOptions(membershipKey, MEMBERSHIP_CREDENTIAL_ISSUER), testDocLoader);

                // create signed VP, that contains the VC
                var vpKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vp-sign-key")
                        .generate();
                var content = "%s, %s".formatted(signedNameCredential, signedMembershipCred);
                var input = TestData.VP_CONTENT_TEMPLATE.formatted(content);


                var rawVp = LdpCreationUtils.signDocument(input, vpKey, generateEmbeddedProofOptions(vpKey, VP_HOLDER), testDocLoader);
                // tamper with the presentation
                rawVp = rawVp.replace("\"https://holder.test.com\"", "\"https://another-holder.test.com\"");
                // modify
                var res = ldpVerifier.verify(rawVp, context);
                assertThat(res).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_proofPurposeNotAssertionMethod() throws JOSEException {
                // create signed VP, that contains the VC
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey,
                        proofOptions.purpose(URI.create("https://test.org/notValid")), testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("InvalidProofPurpose");
            }
        }

        @Nested
        class Credentials {
            @Test
            void verify_success() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofKey = new JwkMethod(URI.create(TestData.NAME_CREDENTIAL_ISSUER), URI.create("https://w3id.org/security#JsonWebKey2020"), null, nameKey);
                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER).verificationMethod(proofKey);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, proofKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isSucceeded();
            }

            @Test
            void verify_forgedProof_shouldFail() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var forgedKey = new ECKeyGenerator(Curve.P_384)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();

                var proofKey = new JwkMethod(URI.create(TestData.NAME_CREDENTIAL_ISSUER), URI.create("https://w3id.org/security#JsonWebKey2020"), null, nameKey);
                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER).verificationMethod(proofKey);

                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, forgedKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed().detail().contains("InvalidSignature");
            }

            @Test
            void verify_noProof_fails() throws JsonProcessingException {
                var credential = jsonLd.expand(mapper.readValue(VC_CONTENT_CERTIFICATE_EXAMPLE, JsonObject.class)).getContent().toString();
                assertThat(ldpVerifier.verify(credential, context)).isFailed().detail().contains("MissingProof");
            }

            @Test
            void verify_proofPurposeNotAssertionMethod() throws JOSEException {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var nameCredential = createNameCredential();
                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey,
                        proofOptions.purpose(URI.create("https://test.org/notValid")), testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("InvalidProofPurpose");
            }

            @Test
            void verify_verificationMethodIsDid() throws JOSEException, DocumentError {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var identifier = URI.create("did:web-test-issuer");
                var nameCredential = createNameCredential(identifier.toString());
                VerificationMethod did = new JwkMethod(identifier, null, null, null);
                ArgumentMatcher<URI> uriMatcher = argument -> argument.equals(identifier);

                when(mockDidResolver.isAccepted(argThat(uriMatcher))).thenReturn(true);
                when(mockDidResolver.resolve(argThat(uriMatcher), any(), any())).thenReturn(new JwkMethod(identifier, null, null, nameKey));

                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER).verificationMethod(did);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isSucceeded();
            }

            @Test
            void verify_verificationMethodIsDid_doesNotMatchIssuer() throws JOSEException, DocumentError {
                var nameKey = new ECKeyGenerator(Curve.P_256)
                        .keyID("vc-sign-key1")
                        .generate();

                var identifier = URI.create("did:web-test-issuer");
                var nameCredential = createNameCredential("did:web:some-other-issuer");
                VerificationMethod did = new JwkMethod(identifier, null, null, null);
                ArgumentMatcher<URI> uriMatcher = argument -> argument.equals(identifier);

                when(mockDidResolver.isAccepted(argThat(uriMatcher))).thenReturn(true);
                when(mockDidResolver.resolve(argThat(uriMatcher), any(), any())).thenReturn(new JwkMethod(identifier, null, null, nameKey));

                var proofOptions = generateEmbeddedProofOptions(nameKey, NAME_CREDENTIAL_ISSUER).verificationMethod(did);
                var signedNameCredential = LdpCreationUtils.signDocument(nameCredential, nameKey, proofOptions, testDocLoader);

                assertThat(ldpVerifier.verify(signedNameCredential, context)).isFailed()
                        .detail().contains("Issuer and proof.verificationMethod mismatch");
            }


        }

        @Nested
        class Generate {

            private JWK issuerKey;

            @BeforeEach
            void setup() throws ParseException {
                issuerKey = JWK.parse("""
                            {"kty":"OKP","d":"riGLTHyP2ahmtKRja6ZHJrmdr1CtHRUWmw74werPw00","crv":"Ed25519","x":"ActoXZWU6HMz4_aWRvS6g7cni0rnutGuSPzWXtnt49U"}
                        """);
            }

            @ParameterizedTest
            @ValueSource(strings = { "did:web:localhost%3A7083", "did:web:localhost%3A7093", "did:web:alice-identityhub%3A7083:alice", "did:web:bob-identityhub%3A7083:bob" })
            void createCredential_membership(String did) throws JsonProcessingException, SigningError, DocumentError {
                var raw = """
                        {
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://w3id.org/security/suites/jws-2020/v1",
                                "https://www.w3.org/ns/did/v1",
                                {
                                  "cx-credentials": "https://w3id.org/catenax/credentials/",
                                  "membership": "cx-credentials:membership",
                                  "membershipType": "cx-credentials:membershipType",
                                  "website": "cx-credentials:website",
                                  "contact": "cx-credentials:contact",
                                  "since": "cx-credentials:since"
                                }
                            ],
                            "id": "http://org.yourdataspace.com/credentials/2347",
                            "type": [
                                "VerifiableCredential",
                                "http://org.yourdataspace.com#MembershipCredential"
                            ],
                            "issuer": "did:example:dataspace-issuer",
                            "issuanceDate": "2023-08-18T00:00:00Z",
                            "credentialSubject": {
                                "id": "%s",
                                "membership": {
                                    "membershipType": "FullMember",
                                    "website": "www.whatever.com",
                                    "contact": "mix.max@whatever.com",
                                    "since": "2023-01-01T00:00:00Z"
                                }
                            }
                        }
                        """.formatted(did);
                var jsonDoc = mapper.readValue(raw, JsonObject.class);

                var proofOptions = new JwsSignatureProofOptions(jwsSignatureSuite).verificationMethod(new JwkMethod(URI.create("did:example:dataspace-issuer#key1"), null, null, null))
                        .purpose(URI.create("https://w3id.org/security#assertionMethod"))
                        .created(Instant.now());

                var ldKeypair = TestFunctions.createKeyPair(issuerKey);
                var issuer = Vc.sign(jsonDoc, ldKeypair, proofOptions);

                System.out.println(issuer.getExpanded().toString());
            }

            @ParameterizedTest
            @ValueSource(strings = { "did:web:localhost%3A7083", "did:web:localhost%3A7093", "did:web:alice-identityhub%3A7083:alice", "did:web:bob-identityhub%3A7083:bob" })
            void createCredential_pcf(String did) throws JsonProcessingException, SigningError, DocumentError {
                var raw = """
                        {
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://w3id.org/security/suites/jws-2020/v1",
                                "https://www.w3.org/ns/did/v1",
                                {
                                  "cx-credentials": "https://w3id.org/catenax/credentials/",
                                  "holderIdentifier": "cx-credentials:holderIdentifier",
                                  "useCaseType": "cx-credentials:useCaseType",
                                  "contractTemplate": "cx-credentials:contractTemplate",
                                  "contractVersion": "cx-credentials:contractVersion"
                                }
                            ],
                            "id": "http://org.yourdataspace.com/credentials/2347",
                            "type": [
                                "VerifiableCredential",
                                "http://org.yourdataspace.com#PcfCredential"
                            ],
                            "issuer": "did:example:dataspace-issuer",
                            "issuanceDate": "2023-08-18T00:00:00Z",
                            "credentialSubject": {
                                "id": "%s",
                                "holderIdentifier": "BPN000000XYZ",
                                "contractTemplate": "https://public.catena-x.org/contracts/pcf.v1.pdf",
                                "contractVersion": "1.0.0"
                            }
                        }
                        """.formatted(did);
                var jsonDoc = mapper.readValue(raw, JsonObject.class);

                var proofOptions = new JwsSignatureProofOptions(jwsSignatureSuite).verificationMethod(new JwkMethod(URI.create("did:example:dataspace-issuer#key1"), null, null, null))
                        .purpose(URI.create("https://w3id.org/security#assertionMethod"))
                        .created(Instant.now());

                var ldKeypair = TestFunctions.createKeyPair(issuerKey);
                var issuer = Vc.sign(jsonDoc, ldKeypair, proofOptions);

                System.out.println(issuer.getExpanded().toString());
            }
        }
    }

    @Nested
    class Ed25519Signature2018 {
        // not yet implemented
    }
}