<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aviso - TaskFlow</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
    <meta name="view-transition" content="same-origin">
</head>
<body>
    <#-- Só a página de "e-mail confirmado" usa o redirecionamento automático para o login do TaskFlow.
         As demais páginas de aviso (ex.: e-mails de ação enviados pelo administrador, "conta atualizada")
         seguem o destino indicado pelo Keycloak, como no tema base. -->
    <#assign isEmailVerified = false>
    <#if message?has_content && message.summary?has_content>
        <#if message.summary == msg("emailVerifiedMessage") || message.summary == msg("emailVerifiedAlreadyMessage")>
            <#assign isEmailVerified = true>
        </#if>
    </#if>

    <#assign candidateUrl = "">
    <#if pageRedirectUri?has_content>
        <#assign candidateUrl = pageRedirectUri>
    <#elseif actionUri?has_content>
        <#assign candidateUrl = actionUri>
    <#elseif (client.baseUrl)?has_content>
        <#assign candidateUrl = client.baseUrl>
    <#else>
        <#assign candidateUrl = url.loginUrl>
    </#if>

    <div class="container">
        <div class="card">
            
            <div class="formSection">
                <div class="formFade" style="text-align: center;">
                    <#if isEmailVerified>
                        <!-- Ícone de Confirmação de E-mail -->
                        <div style="display: flex; justify-content: center; margin-bottom: 20px;">
                            <div style="width: 64px; height: 64px; border-radius: 50%; background-color: #ecfdf5; border: 2px solid #10b981; display: flex; align-items: center; justify-content: center;">
                                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                            </div>
                        </div>

                        <div class="header">
                            <h2 class="title" style="color: #0f172a;">E-mail Confirmado!</h2>
                            <p class="subtitle" style="margin-top: 14px; font-size: 1.05rem; line-height: 1.5; color: #475569;">
                                Confirmamos o seu endereço de e-mail com sucesso.
                            </p>
                            <p style="margin-top: 8px; font-size: 0.95rem; color: #64748b; line-height: 1.4;">
                                Por segurança, acesse sua conta informando suas credenciais para começar a usar o TaskFlow.
                            </p>
                        </div>

                        <div class="actions" style="margin-top: 30px; display: flex; flex-direction: column; align-items: center; gap: 12px;">
                            <a id="auto-redirect-btn" href="${candidateUrl}" class="btn-primary" style="text-decoration: none; padding: 14px 36px; display: inline-block; font-weight: 600; font-size: 1rem; border-radius: 8px;">
                                Fazer Login
                            </a>

                            <div style="margin-top: 10px; display: flex; flex-direction: column; align-items: center; gap: 6px;">
                                <span style="font-size: 0.85rem; color: #64748b;">
                                    Redirecionando em <span id="countdown" style="font-weight: 700; color: #3b82f6;">5</span> segundos...
                                </span>
                                <button type="button" id="pause-btn" style="background: none; border: none; color: #64748b; font-size: 0.8rem; cursor: pointer; text-decoration: underline; padding: 4px;">
                                    Pausar redirecionamento
                                </button>
                            </div>
                        </div>
                    <#else>
                        <!-- Mensagem informativa genérica: o botão leva ao destino indicado pelo Keycloak (sem redirecionamento automático) -->
                        <div class="header">
                            <h2 class="title">Aviso do Sistema</h2>
                            <p id="kc-message" class="subtitle" style="margin-top: 20px; font-size: 1.1rem; color: var(--text-dark);">
                                ${kcSanitize(message.summary)?no_esc}<#if requiredActions?has_content>: <b><#list requiredActions as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#list></b></#if>
                            </p>
                        </div>

                        <#if !(skipLink??)>
                        <div class="actions" style="margin-top: 35px; display: flex; flex-direction: column; align-items: center; gap: 12px;">
                            <#if pageRedirectUri?has_content>
                                <a id="info-continue-btn" href="${pageRedirectUri}" class="btn-primary" style="text-decoration: none; padding: 12px 30px; display: inline-block;">Voltar para o TaskFlow</a>
                            <#elseif actionUri?has_content>
                                <a id="info-continue-btn" href="${actionUri}" class="btn-primary" style="text-decoration: none; padding: 12px 30px; display: inline-block;">Continuar</a>
                            <#elseif (client.baseUrl)?has_content>
                                <a id="info-continue-btn" href="${client.baseUrl}" class="btn-primary" style="text-decoration: none; padding: 12px 30px; display: inline-block;">Voltar para o TaskFlow</a>
                            </#if>
                        </div>
                        </#if>
                    </#if>

                </div>
            </div>

            <div class="brandSection">
                <div class="brandContent">
                    <h1 class="logoTitle">
                        <span>Task</span> <br /> Flow
                    </h1>
                </div>
            </div>

        </div>
    </div>

    <#if isEmailVerified>
    <script>
        (function() {
            // 1. Limpeza estrita de sessão para garantir: "validar sem logar"
            try {
                var cookies = ["KEYCLOAK_SESSION", "KEYCLOAK_SESSION_LEGACY", "KEYCLOAK_IDENTITY", "KEYCLOAK_IDENTITY_LEGACY", "KC_RESTART"];
                var paths = ["/", window.location.pathname];
                for (var i = 0; i < cookies.length; i++) {
                    for (var p = 0; p < paths.length; p++) {
                        document.cookie = cookies[i] + "=; Path=" + paths[p] + "; Expires=Thu, 01 Jan 1970 00:00:01 GMT;";
                    }
                }
            } catch (e) {
                console.warn("Erro ao limpar cookies de sessão:", e);
            }

            // 2. Resolução dinâmica do destino SEM mockar nem fixar IP/porta/domínio
            function safeHttpOrigin(rawUrl) {
                if (!rawUrl || String(rawUrl).trim() === "") return null;
                try {
                    var parsed = new URL(rawUrl, window.location.origin);
                    if ((parsed.protocol === "http:" || parsed.protocol === "https:") && parsed.origin && parsed.origin !== "null") {
                        return parsed.origin;
                    }
                } catch (_e) {}
                return null;
            }

            function resolveDynamicDestination() {
                var candidate = "${candidateUrl?js_string}";
                var loginUrl = "${url.loginUrl?js_string}";
                var candidateOrigin = safeHttpOrigin(candidate);
                var hasExternalCandidate = Boolean(candidateOrigin && candidateOrigin !== window.location.origin);

                // Prioriza redirect_uri somente se compatível com a origem conhecida do cliente (evita open redirect)
                try {
                    var urlParams = new URLSearchParams(window.location.search);
                    var redirectOrigin = safeHttpOrigin(urlParams.get("redirect_uri"));
                    if (redirectOrigin && redirectOrigin !== window.location.origin) {
                        if (!hasExternalCandidate || redirectOrigin === candidateOrigin) {
                            return redirectOrigin + "/?start_login=1&verified=1";
                        }
                    }
                } catch (_e) {}

                // Origem vinda de pageRedirectUri ou client.baseUrl (mais confiável ao abrir o link via webmail/Mailpit)
                if (hasExternalCandidate) {
                    return candidateOrigin + "/?start_login=1&verified=1";
                }

                // Fallback via document.referrer apenas quando não há origem configurada do cliente
                var refOrigin = safeHttpOrigin(document.referrer);
                if (refOrigin && refOrigin !== window.location.origin) {
                    return refOrigin + "/?start_login=1&verified=1";
                }

                // Fallback dinâmico: URL nativa de login do Keycloak neste servidor
                return loginUrl || "/";
            }

            var targetUrl = resolveDynamicDestination();
            var actionBtn = document.getElementById("auto-redirect-btn");
            if (actionBtn) {
                actionBtn.href = targetUrl;
            }

            // 3. Controle da contagem regressiva e opção de pausar
            var timeLeft = 5;
            var isPaused = false;
            var countdownEl = document.getElementById("countdown");
            var pauseBtn = document.getElementById("pause-btn");

            if (pauseBtn) {
                pauseBtn.addEventListener("click", function() {
                    isPaused = !isPaused;
                    pauseBtn.textContent = isPaused ? "Retomar redirecionamento" : "Pausar redirecionamento";
                });
            }

            var timer = setInterval(function() {
                if (isPaused) return;
                timeLeft--;
                if (countdownEl) {
                    countdownEl.textContent = timeLeft;
                }
                if (timeLeft <= 0) {
                    clearInterval(timer);
                    window.location.replace(targetUrl);
                }
            }, 1000);
        })();
    </script>
    </#if>
</body>
</html>
