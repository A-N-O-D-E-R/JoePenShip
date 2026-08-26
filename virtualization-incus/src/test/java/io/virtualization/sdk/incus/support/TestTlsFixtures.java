package io.virtualization.sdk.incus.support;

/**
 * A self-signed test certificate and its PKCS#8 private key, generated once with:
 * {@code openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 3650 -nodes -subj "/CN=incus-test-client"}
 * then {@code openssl pkcs8 -topk8 -nocrypt -in key.pem -out key8.pem}. Used only to exercise
 * {@code IncusApiClient}'s PEM-parsing / SSLContext-building code — not a real credential.
 */
public final class TestTlsFixtures {

    public static final String CLIENT_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDGTCCAgGgAwIBAgIUBTk/axuW8DLKL/Po92szQXeILJ0wDQYJKoZIhvcNAQEL
            BQAwHDEaMBgGA1UEAwwRaW5jdXMtdGVzdC1jbGllbnQwHhcNMjYwODIzMTcwMjIy
            WhcNMzYwODIwMTcwMjIyWjAcMRowGAYDVQQDDBFpbmN1cy10ZXN0LWNsaWVudDCC
            ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKGSnGw1/hqqRj8Z/646uamW
            l4hQWIbXyz5pOk9RkPYrw24xyC+ww9k233g3cvy/snUVkRLJ3uqXeXQpGpwDYpcQ
            ep85smWvN4FmGpTWU6Ywz+olyTgNT3pQjn9iedomMyixxDpNgopa8f3F8PYgPjeM
            pEZHhH+N4Ps7x8jdcQbTb2pSf1I4LcCz6HM7HXPi2Smpq4EO+93ZFKM5eKS05bCw
            jXYvNxWDyzVBGzH5rC425PBNmuyptKeBpN8QBytxFUM6E/gP91C8+v/qxNQHEHf3
            MN6Kx41i1lRPPR8tsV1qr5lZDEtEAVBrswAQaqEqqrqDgLMN454Ramf3/ztQqesC
            AwEAAaNTMFEwHQYDVR0OBBYEFLwo4OdmNu0LU3xYVWkIdM/+fk8tMB8GA1UdIwQY
            MBaAFLwo4OdmNu0LU3xYVWkIdM/+fk8tMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZI
            hvcNAQELBQADggEBAG2dbAOpn+D+4PwRixCCnVq5Rb0vCQmYmCJwpoHlIUAbO8BG
            X4ETiN5uIpL1U+HpN6vYmc8R5zTEVoHL87K3NM4kubhhAehaNBXnB0kIy2JGb/xd
            MY21nCcJ4/5R5FJbYSYUFlh+mkRY9bZXcMnJU8go7lik4Xik+pHCYcFMhW1Xmfnc
            wnLPBkpO+2NYbGcVIlVlsoddnOXUQ/JCw6+3pl3+l6jKPeQP+5vr7Zicr45bvfKm
            UubXsCeRBLGtx1yz6ujJO4oYV3biZmelfIVxoX6sZkpNA39ui+UGn+7Djh3AojJN
            AC4PjjWGT8mHnuujGSldkm0uEre8pz1HhKeQ/t8=
            -----END CERTIFICATE-----
            """;

    public static final String CLIENT_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQChkpxsNf4aqkY/
            Gf+uOrmplpeIUFiG18s+aTpPUZD2K8NuMcgvsMPZNt94N3L8v7J1FZESyd7ql3l0
            KRqcA2KXEHqfObJlrzeBZhqU1lOmMM/qJck4DU96UI5/YnnaJjMoscQ6TYKKWvH9
            xfD2ID43jKRGR4R/jeD7O8fI3XEG029qUn9SOC3As+hzOx1z4tkpqauBDvvd2RSj
            OXiktOWwsI12LzcVg8s1QRsx+awuNuTwTZrsqbSngaTfEAcrcRVDOhP4D/dQvPr/
            6sTUBxB39zDeiseNYtZUTz0fLbFdaq+ZWQxLRAFQa7MAEGqhKqq6g4CzDeOeEWpn
            9/87UKnrAgMBAAECggEAQ14LbY0HoZuchYP0GPOwZPb+wJ3rg3GncyLR5q+WNyYh
            KqmEE+pqhn98JrZL8fmF0Y7H+eME2KCGyfm3eBRrP5xl3EzkxsQLax8k9yj0IMea
            cUEHCKF4IYJEH/Xx45To4M9aB4s44jIwUwT8xsrD1kqEVi9MnhiMlYQj9WrrTRIi
            tWztwO5mCK0HgIyOdKxIo4vqWIEGZP1GlyVHMrOJXSoaOI7bCr3bBT0ESTQFiCct
            ZYHzj/kkdGA0RGuOlKcosGuvuDsv/Y/mfF+9rvqeS+FnWd3xSq7BkMPSIMaFuU/h
            208IoIDKYKNEkQu3GkwzFAryHeReo6BEvvM2Ud5igQKBgQDXGg6p4P+GB/OhePmL
            qZYg9ivEpVqbymn1TG1eqMu5Jk/NclpX6UV+3WnCfywbSxorLSD2xH6av8mNwqJx
            rlXcLgu5SKZgI6qDfn2aGvMVa3w17KWPPYh3DRY0KZz1KUrNWfT/YgwRs1ngWzMb
            yRt2CfcDXEsVkvM3gPm1lnnZawKBgQDASw85GfYYwdCGe0FzzKTuRyazUDLqxukG
            bJXFJvA2vbHFL1VoPQJiSvC8KFB6zv3GtT6qaAu7N5gBoewJKPqzmo60IwQzvtQx
            lenOLbCquRDT7oi1dw4ivkKwth2TvN3isg2rHf83HWrQCpJGIsQXDMzxk21TdfjB
            BWA5Te4RgQKBgQCe/lm8Y907+wCwaDU8cJvRvWRYtEinQxTTBi0JfQco0hLGrzHs
            LTb7MToNU+cMPZUYOQFWpaNXS0/2AtoD4XMBjt+HkxW3lmVhUMwSbr0jJhHn4gX2
            POtz5C53XBLyfBUYeNfJNXXqV4GlfoPmk95Cf9cfmsa9KaOxcRoiiRmEBQKBgE19
            cfMG3T94yzmGbgfmTgifr57PENW/tqQaGfjLCj5lD0BxJK/O9ij6Hi0U6emzhF0R
            KOGu8BtP+feecun2E6FRBXTeL5FSs1wQF5o4m1sRHRDqrTjDc3TYyzM/EIg6sgfq
            zMtKWVCykBqqR+vvkqnfRK+NQ59AiS/vpKkzGF8BAoGAVjEleaVG80eh0H7li4D5
            4+bpD5O0oFIozw3Tot+7DttcxP6KRuO4KaCBfJstiRzJ1JMZJA//7mRPDhDAjl/6
            IDadPfSIe2wiLbCScZ0DrxUsYHVxLHk4tMwoXfXuQNfxeUf33/L8ohhOHTVpd1JB
            MvLbhVMf06YV25QdNpQcFTI=
            -----END PRIVATE KEY-----
            """;

    private TestTlsFixtures() {}
}
