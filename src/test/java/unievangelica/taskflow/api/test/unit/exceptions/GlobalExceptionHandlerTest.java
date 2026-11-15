package unievangelica.taskflow.api.test.unit.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.exception.ConflictException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.infra.exceptions.GlobalExceptionHandler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica, com serialização real (MockMvc), o formato das respostas de erro (ProblemDetail +
 * message/error/timestamp), o status de cada tipo de exceção e que mensagens técnicas não chegam ao cliente.
 */
public class GlobalExceptionHandlerTest {

    record Entrada(@NotBlank String titulo) {
    }

    @RestController
    static class RotasDeTeste {
        @GetMapping("/nao-encontrado")
        void naoEncontrado() { throw new ResourceNotFoundException("Tarefa não encontrada"); }

        @GetMapping("/regra")
        void regra() { throw new BusinessException("Este convite expirou."); }

        @GetMapping("/conflito")
        void conflito() { throw new ConflictException("Transfira a posse antes de excluir sua conta."); }

        @GetMapping("/proibido")
        void proibido() { throw new SecurityException("Você não tem permissão para alterar esta tarefa."); }

        @GetMapping("/bug")
        void bug() { throw new IllegalStateException("detalhe interno: conexao jdbc://segredo"); }

        @GetMapping("/argumento-jdk")
        void argumentoJdk() { throw new IllegalArgumentException("No enum constant x.y.Prioridade.urgente"); }

        @GetMapping("/integridade")
        void integridade() { throw new DataIntegrityViolationException("duplicate key value violates unique constraint uk_segredo"); }

        @GetMapping("/upload")
        void upload() { throw new MaxUploadSizeExceededException(2 * 1024 * 1024); }

        @GetMapping("/itens/{id}")
        String item(@PathVariable Long id) { return "ok"; }

        @PostMapping("/itens")
        String criar(@Valid @RequestBody Entrada entrada) { return "ok"; }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RotasDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static void assertFormato(ResultActions resultado, int status, String titulo, String mensagem) throws Exception {
        resultado
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                // "type" omitido = about:blank (RFC 9457), como o Spring serializa
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value(titulo))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(mensagem))
                .andExpect(jsonPath("$.instance").exists())
                // Campos de compatibilidade lidos pelo frontend
                .andExpect(jsonPath("$.message").value(mensagem))
                .andExpect(jsonPath("$.error").value(titulo))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ResourceNotFoundException deve responder 404 no formato ProblemDetail com message")
    void naoEncontradoRetorna404() throws Exception {
        assertFormato(mockMvc.perform(get("/nao-encontrado")), 404, "Not Found", "Tarefa não encontrada");
    }

    @Test
    @DisplayName("BusinessException deve responder 400 com a mensagem da regra")
    void regraDeNegocioRetorna400() throws Exception {
        assertFormato(mockMvc.perform(get("/regra")), 400, "Bad Request", "Este convite expirou.");
    }

    @Test
    @DisplayName("ConflictException deve responder 409 com a mensagem orientando o usuário")
    void conflitoRetorna409() throws Exception {
        assertFormato(mockMvc.perform(get("/conflito")), 409, "Conflict",
                "Transfira a posse antes de excluir sua conta.");
    }

    @Test
    @DisplayName("SecurityException deve responder 403 com a mensagem de permissão")
    void semPermissaoRetorna403() throws Exception {
        assertFormato(mockMvc.perform(get("/proibido")), 403, "Forbidden",
                "Você não tem permissão para alterar esta tarefa.");
    }

    @Test
    @DisplayName("Erro inesperado deve responder 500 com mensagem genérica, sem detalhes internos")
    void erroInesperadoRetorna500SemVazamento() throws Exception {
        ResultActions resultado = mockMvc.perform(get("/bug"));
        assertFormato(resultado, 500, "Internal Server Error", "Erro interno. Tente novamente mais tarde.");
        resultado.andExpect(content().string(not(containsString("segredo"))));
    }

    @Test
    @DisplayName("IllegalArgumentException de bibliotecas deve responder 400 genérico, sem a mensagem técnica")
    void argumentoInvalidoDaJvmNaoVaza() throws Exception {
        ResultActions resultado = mockMvc.perform(get("/argumento-jdk"));
        assertFormato(resultado, 400, "Bad Request", "Dados inválidos.");
        resultado.andExpect(content().string(not(containsString("No enum constant"))));
    }

    @Test
    @DisplayName("Violação de integridade deve responder 409 sem expor a constraint")
    void integridadeRetorna409() throws Exception {
        ResultActions resultado = mockMvc.perform(get("/integridade"));
        assertFormato(resultado, 409, "Conflict", "Conflito de integridade de dados.");
        resultado.andExpect(content().string(not(containsString("uk_segredo"))));
    }

    @Test
    @DisplayName("Upload acima do limite deve responder 413 com mensagem amigável")
    void uploadAcimaDoLimiteRetorna413() throws Exception {
        assertFormato(mockMvc.perform(get("/upload")), 413, "Content Too Large",
                "Arquivo muito grande. O tamanho máximo permitido é de 2MB.");
    }

    @Test
    @DisplayName("JSON malformado deve responder 400 sem a mensagem do parser")
    void jsonMalformadoRetorna400() throws Exception {
        ResultActions resultado = mockMvc.perform(post("/itens")
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": "));
        assertFormato(resultado, 400, "Bad Request", "Corpo da requisição inválido ou malformado.");
        resultado.andExpect(content().string(not(containsString("JSON parse error"))));
    }

    @Test
    @DisplayName("Falha de validação deve responder 400 indicando o campo")
    void validacaoRetorna400ComCampo() throws Exception {
        mockMvc.perform(post("/itens").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("titulo")));
    }

    @Test
    @DisplayName("Parâmetro de tipo inválido deve responder 400 citando apenas o nome do parâmetro")
    void tipoDeParametroInvalidoRetorna400() throws Exception {
        ResultActions resultado = mockMvc.perform(get("/itens/abc"));
        assertFormato(resultado, 400, "Bad Request", "Valor inválido para o parâmetro 'id'.");
        resultado.andExpect(content().string(not(containsString("java.lang"))));
    }

    @Test
    @DisplayName("Método HTTP não suportado deve responder 405")
    void metodoNaoSuportadoRetorna405() throws Exception {
        assertFormato(mockMvc.perform(delete("/regra")), 405, "Method Not Allowed",
                "Método HTTP não suportado para este recurso.");
    }

    @Test
    @DisplayName("Rota inexistente deve responder 404")
    void rotaInexistenteRetorna404() throws Exception {
        assertFormato(mockMvc.perform(get("/rota-que-nao-existe")), 404, "Not Found", "Recurso não encontrado.");
    }
}
