package unievangelica.taskflow.api.domain.service;

import java.util.Optional;

/**
 * Identifica o formato de uma imagem pela assinatura do conteúdo (magic bytes),
 * sem confiar no Content-Type informado pelo cliente.
 * Formatos aceitos para avatar: JPEG, PNG, GIF e WEBP.
 */
public final class ImageTypeDetector {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87A = {'G', 'I', 'F', '8', '7', 'a'};
    private static final byte[] GIF89A = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private ImageTypeDetector() {
    }

    /** @return o MIME type detectado ("image/jpeg", "image/png", "image/gif" ou "image/webp"), ou vazio se não reconhecido. */
    public static Optional<String> detect(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        if (startsWith(content, 0, JPEG)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(content, 0, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(content, 0, GIF87A) || startsWith(content, 0, GIF89A)) {
            return Optional.of("image/gif");
        }
        if (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP)) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
