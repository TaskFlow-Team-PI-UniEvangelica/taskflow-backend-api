package unievangelica.taskflow.api.test.unit.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import unievangelica.taskflow.api.infra.exceptions.GlobalExceptionHandler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalExceptionHandlerUploadTest {

    @Test
    @DisplayName("Upload acima do limite deve responder 413 com mensagem amigável")
    void uploadAcimaDoLimiteRetorna413() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> resposta =
                handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(2 * 1024 * 1024));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(resposta.getBody()).containsEntry("status", 413);
        assertThat((String) resposta.getBody().get("message")).isEqualTo("Arquivo muito grande. O tamanho máximo permitido é de 2MB.");
    }
}
