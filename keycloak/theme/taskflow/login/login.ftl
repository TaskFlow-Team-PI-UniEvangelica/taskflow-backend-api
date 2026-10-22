<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TaskFlow - Login</title>
    <meta name="view-transition" content="same-origin">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
    <script>
        (function () {
            var clientId = "${(client.clientId)!''}";
            var isAccountClient = (clientId === "account-console" || clientId === "account");
            var hasKcAction = window.location.search.indexOf("kc_action=") !== -1;

            if (window.name === "taskflow:registered") {
                window.name = "";
            }

            function resolveFrontendOrigin() {
                try {
                    var params = new URLSearchParams(window.location.search);
                    var redirectUri = params.get("redirect_uri");
                    if (redirectUri) {
                        var parsed = new URL(redirectUri);
                        if (parsed.origin && parsed.origin !== "null") return parsed.origin;
                    }
                    if (document.referrer) {
                        var refUrl = new URL(document.referrer);
                        if (refUrl.origin && refUrl.origin !== "null" && refUrl.origin !== window.location.origin) {
                            return refUrl.origin;
                        }
                    }
                } catch (e) {}
                return "${(client.baseUrl)!(client.rootUrl)!''}" || window.location.origin;
            }

            function resolveDashboardUrl() {
                var origin = resolveFrontendOrigin();
                return origin ? (origin + "/dashboard") : "/";
            }

            function hasActiveKeycloakSession() {
                return /(?:^|;\s*)KEYCLOAK_SESSION(?:_LEGACY)?=/.test(document.cookie);
            }

            function isBackForwardNav(evt) {
                if (evt && evt.persisted) return true;
                try {
                    var nav = performance.getEntriesByType && performance.getEntriesByType("navigation")[0];
                    if (nav && nav.type === "back_forward") return true;
                } catch (e) {}
                return Boolean(window.performance && window.performance.navigation && window.performance.navigation.type === 2);
            }

            function guardLoginPage(evt) {
                if (hasActiveKeycloakSession() && ((!isAccountClient && !hasKcAction) || isBackForwardNav(evt))) {
                    document.documentElement.style.display = "none";
                    window.location.replace(resolveDashboardUrl());
                }
            }

            guardLoginPage(null);
            window.addEventListener("pageshow", guardLoginPage);
        })();
    </script>
</head>
<body>
    <div class="container">
        <div class="card">
            
            <!-- ESQUERDA: Formulário -->
            <div class="formSection">
                <div class="formFade">
                    <div class="header">
                        <h2 class="title">Bem Vindo de volta</h2>
                        <p class="subtitle">Por favor insira seus dados para acessar o painel.</p>
                    </div>

                    <#if message?has_content >
                        <div class="alert alert-${message.type}">
                            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>
                    <div id="kc-registered-success" class="alert alert-success" style="display: none;">
                        <span class="kc-feedback-text">Conta criada com sucesso! Faça login para acessar o painel.</span>
                    </div>
                    <div id="kc-verified-success" class="alert alert-success" style="display: none;">
                        <span class="kc-feedback-text">E-mail confirmado com sucesso! Faça login para acessar o painel.</span>
                    </div>

                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        
                        <div class="input-group">
                            <label for="username">Email ou Usuário</label>
                            <input tabindex="1" id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="off" placeholder="Insira seu email" required />
                        </div>

                        <div class="input-group">
                            <label for="password">Senha</label>
                            <input tabindex="2" id="password" name="password" type="password" autocomplete="off" placeholder="••••••••" required />
                        </div>

                        <div class="forgotPassword">
                            <#if realm.resetPasswordAllowed>
                                <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="forgotLink">Esqueceu a senha?</a>
                            </#if>
                        </div>

                        <button tabindex="4" class="btn-primary" name="login" id="kc-login" type="submit">Login</button>
                    </form>

                    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                        <div class="socialLogin">
                            <div class="divider"></div>
                            <p class="footerText">
                                Não tem uma conta?
                                <a href="${url.registrationUrl}" class="signupLink">Se inscreva</a>
                            </p>
                        </div>
                    </#if>
                </div>
            </div>

            <!-- DIREITA: Imagem e Logo -->
            <div class="brandSection">
                <div class="brandContent">
                    <h1 class="logoTitle">
                        <span>Task</span> <br /> Flow
                    </h1>
                </div>
            </div>

        </div>
    </div>
    <script>
        (function () {
            function resolveFrontendOrigin() {
                try {
                    var params = new URLSearchParams(window.location.search);
                    var redirectUri = params.get("redirect_uri");
                    if (redirectUri) {
                        return new URL(redirectUri).origin;
                    }
            // Se veio do redirecionamento pós-confirmação ou pós-cadastro, exibe o aviso apropriado
            var searchParams = new URLSearchParams(window.location.search);
            if (searchParams.get("verified") === "1") {
                var vBanner = document.getElementById("kc-verified-success");
                if (vBanner) {
                    vBanner.style.display = "block";
                }
            } else if (searchParams.get("prompt") === "login" && searchParams.get("login_hint")) {
                var banner = document.getElementById("kc-registered-success");
                if (banner) {
                    banner.style.display = "block";
                }
            }

            // Ao clicar em "Se inscreva", passa pela origem do frontend (:5173/?start_signup=1)
            // para registrar no localStorage que o fluxo é de criação de conta
            var signupLink = document.querySelector(".signupLink");
            if (signupLink) {
                signupLink.addEventListener("click", function (e) {
                    e.preventDefault();
                    window.location.replace(resolveFrontendOrigin() + "/?start_signup=1");
                });
            }
        })();
    </script>
</body>
</html>
