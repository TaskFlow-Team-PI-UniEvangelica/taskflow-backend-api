package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.persistence.entities.UserAvatarEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserAvatarRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.ImageTypeDetector;
import unievangelica.taskflow.api.domain.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AvatarUploadTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] GIF = "GIF89a\u0001\u0000".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] WEBP = "RIFF$\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] HTML = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAvatarRepository avatarRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Detector deve reconhecer JPEG, PNG, GIF e WEBP pela assinatura")
    void detectaFormatosAceitos() {
        assertEquals(Optional.of("image/jpeg"), ImageTypeDetector.detect(JPEG));
        assertEquals(Optional.of("image/png"), ImageTypeDetector.detect(PNG));
        assertEquals(Optional.of("image/gif"), ImageTypeDetector.detect(GIF));
        assertEquals(Optional.of("image/webp"), ImageTypeDetector.detect(WEBP));
    }

    @Test
    @DisplayName("Detector deve recusar conteúdo que não é imagem, arquivo curto, vazio e nulo")
    void recusaConteudoInvalido() {
        assertTrue(ImageTypeDetector.detect(HTML).isEmpty());
        assertTrue(ImageTypeDetector.detect("RIFF0000WAVE".getBytes(StandardCharsets.ISO_8859_1)).isEmpty());
        assertTrue(ImageTypeDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xD8}).isEmpty());
        assertTrue(ImageTypeDetector.detect(new byte[0]).isEmpty());
        assertTrue(ImageTypeDetector.detect(null).isEmpty());
    }

    @Test
    @DisplayName("HTML enviado como image/png deve ser recusado sem gravar")
    void recusaHtmlDisfarcadoDeImagem() {
        MockMultipartFile arquivo = new MockMultipartFile("file", "avatar.png", "image/png", HTML);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateAvatar("kc-1", arquivo));

        assertTrue(ex.getMessage().contains("Formato de imagem não suportado"));
        verifyNoInteractions(avatarRepository);
    }

    @Test
    @DisplayName("Deve gravar o tipo detectado no conteúdo, não o Content-Type declarado")
    void gravaTipoDetectado() {
        MockMultipartFile pngDeclaradoComoJpeg = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", PNG);
        MockMultipartFile semContentType = new MockMultipartFile("file", "avatar", null, JPEG);

        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(1L)));
        when(userRepository.findByKeycloakId("kc-2")).thenReturn(Optional.of(usuario(2L)));

        userService.updateAvatar("kc-1", pngDeclaradoComoJpeg);
        userService.updateAvatar("kc-2", semContentType);

        ArgumentCaptor<UserAvatarEntity> salvo = ArgumentCaptor.forClass(UserAvatarEntity.class);
        verify(avatarRepository, times(2)).save(salvo.capture());
        assertEquals(1L, salvo.getAllValues().get(0).getUserId());
        assertEquals("image/png", salvo.getAllValues().get(0).getContentType());
        assertArrayEquals(PNG, salvo.getAllValues().get(0).getContent());
        assertEquals(2L, salvo.getAllValues().get(1).getUserId());
        assertEquals("image/jpeg", salvo.getAllValues().get(1).getContentType());
    }

    private static UserEntity usuario(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("Arquivo vazio deve ser recusado")
    void recusaArquivoVazio() {
        MockMultipartFile vazio = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThrows(BusinessException.class, () -> userService.updateAvatar("kc-1", vazio));
        verifyNoInteractions(userRepository, avatarRepository);
    }
}
