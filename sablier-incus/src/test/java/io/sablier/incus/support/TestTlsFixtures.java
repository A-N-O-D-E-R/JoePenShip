package io.sablier.incus.support;

/**
 * Self-signed test certificates, generated once with:
 * {@code openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 3650 -nodes -subj "/CN=..."}
 * then {@code openssl pkcs8 -topk8 -nocrypt -in key.pem -out key8.pem} for the client key. Used
 * only to exercise {@code IncusClient}'s PEM-parsing / SSLContext-building code — not real
 * credentials.
 */
public final class TestTlsFixtures {

    public static final String CLIENT_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDHTCCAgWgAwIBAgIURQtE5nEuldFiddU//1IPOpQ482kwDQYJKoZIhvcNAQEL
            BQAwHjEcMBoGA1UEAwwTc2FibGllci10ZXN0LWNsaWVudDAeFw0yNjA4MjMyMDU5
            MzlaFw0zNjA4MjAyMDU5MzlaMB4xHDAaBgNVBAMME3NhYmxpZXItdGVzdC1jbGll
            bnQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDHhVmbjMVBgivwuQAf
            qQoOtNfPS5CzjeRz7x4ukxcE9N4fT44lGmMm3iw0mKQTTH6i71VW/87i7yhrHwlm
            TVs/IzrOkJgmHd6h2REsPfCk0Mt0/ncbRjqbyq+ZGBW/EiPTYoQ67mdBnUeXoKJC
            jn+T4d7042tlqO4WjchnbRGSoj2gUevgJCA2rLoqAi8LvI1KSnm6KEAiSVeFSUw9
            LCBpxikjEpl51ZolzCtJV2y3VRs1oWSDezSUYs2XGn4oXo+qvOV1VgFgEa1vK1Vv
            mATfOxflk1+Xl1G5zMrBL5rypeCu+7KguBlYVldkNrJk6iyTshnJRPESkgdY4yN4
            wZ/xAgMBAAGjUzBRMB0GA1UdDgQWBBTI3RMmVZSpsqtgxH99N44v+9JzsjAfBgNV
            HSMEGDAWgBTI3RMmVZSpsqtgxH99N44v+9JzsjAPBgNVHRMBAf8EBTADAQH/MA0G
            CSqGSIb3DQEBCwUAA4IBAQBo3mfKILfB2fVUwQgv2LH7g66vpYt8E/VZfG1IoGeq
            fJYmRPxY8QHu9NlvbrxbTy2oCjr6aAvW1xL5gTnKt/l5Dv0p+huk8r8zk+vxNt02
            Bk4hCAoCKGR6NjWRZLcu0Sf9zG9y06VjSeeIQWmsC9lAxQy0ajtqChWFL9u2U9DK
            eZHnFs37ENW8l0B9uHnLrEzxo2Q8tir6pcum/3uD3M6RQcn1Oo1VH9MmwGXiI07/
            X1JemuZDVn82BMEZboiwDlNm4ljzXmlwEHDLGMCpvH+Y9OyxSlbg2cY11B/9qoqQ
            ADbqKixDJTvfm/rExcIJSoLBgCLQcU9BoiNi6aRYbjSB
            -----END CERTIFICATE-----
            """;

    public static final String CLIENT_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDHhVmbjMVBgivw
            uQAfqQoOtNfPS5CzjeRz7x4ukxcE9N4fT44lGmMm3iw0mKQTTH6i71VW/87i7yhr
            HwlmTVs/IzrOkJgmHd6h2REsPfCk0Mt0/ncbRjqbyq+ZGBW/EiPTYoQ67mdBnUeX
            oKJCjn+T4d7042tlqO4WjchnbRGSoj2gUevgJCA2rLoqAi8LvI1KSnm6KEAiSVeF
            SUw9LCBpxikjEpl51ZolzCtJV2y3VRs1oWSDezSUYs2XGn4oXo+qvOV1VgFgEa1v
            K1VvmATfOxflk1+Xl1G5zMrBL5rypeCu+7KguBlYVldkNrJk6iyTshnJRPESkgdY
            4yN4wZ/xAgMBAAECggEABMA6QxJ6PdQFRsP+1UqyNc/4k5G6IWpE3t/6NvChKY8v
            YTxJDaxSHORrTVEMn6kExZpRmaHMqkd867I0stNzqMXAHYbNfxJSvgF0qm1K4eYS
            nNV/JNLZaeKquIymAHJ3ux6z0gR7P+KXZhcHxhRYKq5dqDUCGru7CTb7Vh1D0Q5D
            POc2Xui1NswK8s4kyHQgkDFSvOLpcbaaPVmUiRmDVFc2e6m4ufVrEoydlRjp+iuR
            6m/x+ZDcvTivsDWa4SBsOv4Fxuzrd+l8NdG0Te00UsEZyCI1W4ocM3o41l1xkECp
            WxnyZxuz7QB6l+0SNl2gH5t8tTcZGubSY2v8eO/AQQKBgQD9GEiNWB8urGFUKqCO
            qhD1FTq6+A8WLENnZsupSWfLXirYRP6aEhlzEruy2yHQAmozSIpvvHAXTg7bMK02
            8TUV4k0WEf/r/KqneOHhp34/d/jKAJD6zsv9ZJTFZiGO2fxB+Y4TTe/LteW8668W
            fxzM5LE2C+Wx7+WSt4rzmBPzxwKBgQDJz6Pd9u9E6c3TBqOTObZdOrOSx/7tiHnq
            ZW6g8A/8VMUcbQFt6mbPgDelHsBZtQ64OGCDOmqrrNpQ1Nj/l9LEjYkOcfFAKphk
            ZFfj1M1I0oI3YbrjJDj/r9svQVoWzFrgUovHZXdmoQRxXvMimHyEW+gfHmNfLeJx
            5mEg5X9ehwKBgHcnZZDVueh5U2ESqIBB5LXdhsbbXg7sS9d0d9F9M5Z1AOrMoKjc
            eIrKeP+dVu/dEy6Nqk8sggEWyLu80O+a44kn/26yjrAFRjqOGJnqBu/OhZxkY90Q
            Ws0y+y9sA8SDL9XHrXG9MXXQbxZgRw/qTB7SU/PD6iG5dXV7X7mLEWmlAoGAUe6h
            2ai6JWFCtcz5Nfl1R05gv1PA9NC2pmn9ywsLgmcsC0laDjTe/plQfhIJB6KRUktZ
            K43Y5s/rZmuzmbka0b+giCPMTT+91OxEHnQzz7/fK/radAMtvOi5dOr0V1MqBe6d
            XxubqSfv9NMWpNIBo37os1GUCH1JdPKSNlfWKRsCgYAyEsloQC8QlWGh7FRIIVyr
            MC/vtvydP4wKCOtsvDFJzjG1ohmIVqE+s8uA6aAGUHH4JzPLJ8uWFYMdhbx/SmxI
            hxvrL0/O6scjJnfNx+yiWOEsiYFb/Z7+nE6Y1+jFcxLxskZ+39mdX05VVR93R46W
            w8MaOxEp8p5Lbu0xjoRF+Q==
            -----END PRIVATE KEY-----
            """;

    public static final String CA_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDFTCCAf2gAwIBAgIUUaYCYI7VCyQWzxR0qMSKH0J0C40wDQYJKoZIhvcNAQEL
            BQAwGjEYMBYGA1UEAwwPc2FibGllci10ZXN0LWNhMB4XDTI2MDgyMzIwNTk0N1oX
            DTM2MDgyMDIwNTk0N1owGjEYMBYGA1UEAwwPc2FibGllci10ZXN0LWNhMIIBIjAN
            BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsrKaulGgYxzU5azy1MDk60tRacF9
            gYPrpdoZtLi0HaGEH2NaSNZ2l3qBnfhZd7tKlHQS3vyB93//3224Qa7APxsDfqIt
            SYCFkA0Ww/yQXxSm65h7yujQLqzgWKPgcTSpaqVWVTG5dHXZof6d84gDYh3uJJ2v
            2JZB94pcx4BESp4lk98cbXsMDLRomX7Qg8M3dVl9DsRr9mbcHkUE7I+kWb7YxH2R
            xyNchpZvxe0FPq3nCDVSKF1fTajMG/46kho2LcxVfJnL599rGDzbRHpT8sIAe6/f
            oe7/DtCOQnJS3fXu/LQ/8r29DGprCjkFKcN3u8eAqoA7v9/6s2upRUQmlwIDAQAB
            o1MwUTAdBgNVHQ4EFgQU+vWTa/uqx5F2Iyn/C08L5yaDd8AwHwYDVR0jBBgwFoAU
            +vWTa/uqx5F2Iyn/C08L5yaDd8AwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B
            AQsFAAOCAQEAQHxRYSCs1Up+Sk/J7pa6Vx/LyoFc4v5x95FYFA9F/DP84noso68q
            i83UKB1D6ILaKnREZSjERqfAT1ntnKhMdcKtadsx70h/7BU4Ki7nTPfLI9hrXXsV
            LlaVY+Ij4qqJxZXRWE0zo3C/z+4H+KXGdgAQ6LkyQUv7ue3kZXFz34FEM30ldxYe
            4T4NaL/x3gxLT5ZEBYDXiJzPRJq3VtEglk/NsuM0vt4N5aXDeCC8hqAHFNVLRqDi
            jymA3oc3oPiWIVwVlXEDvaj6rMS9ae8ozNt2h0kSWVY4dfma2tl85BcaqkC891ca
            s/LQ+pROQ10zHZnH4gjyCI0F+tG2a+csyQ==
            -----END CERTIFICATE-----
            """;

    private TestTlsFixtures() {}
}
