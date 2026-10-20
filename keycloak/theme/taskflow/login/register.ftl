<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Criar Conta - TaskFlow</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
    <meta name="view-transition" content="same-origin">
    <script>
        (function () {
            var LOGIN_URL = "${url.loginUrl?no_esc}";

            function resolveDashboardUrl() {
                try {
                    var params = new URLSearchParams(window.location.search);
                    var redirectUri = params.get("redirect_uri");
                    if (redirectUri) {
                        return new URL(redirectUri).origin + "/dashboard";
                    }
                    if (document.referrer) {
                        var refUrl = new URL(document.referrer);
                        if (refUrl.origin !== window.location.origin) {
                            return refUrl.origin + "/dashboard";
                        }
                    }
                } catch (e) {}
                return "http://localhost:5173/dashboard";
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

            function guardRegistrationPage(evt) {
                if (hasActiveKeycloakSession()) {
                    document.documentElement.style.display = "none";
                    window.location.replace(resolveDashboardUrl());
                    return;
                }
                if (isBackForwardNav(evt) && window.name === "taskflow:registered") {
                    document.documentElement.style.display = "none";
                    window.name = "";
                    if (LOGIN_URL) {
                        window.location.replace(LOGIN_URL);
                    } else {
                        window.location.replace(resolveDashboardUrl().replace(/\/dashboard$/, "/"));
                    }
                }
            }

            guardRegistrationPage(null);
            window.addEventListener("pageshow", guardRegistrationPage);
        })();
    </script>
</head>
<body>
    <div class="container">
        <!-- O card no registro usa 'reverse' para colocar a foto na esquerda -->
        <div class="card reverse">
            
            <div class="formSection">
                <div class="formFade">
                    <div class="header" style="margin-bottom: 20px;">
                        <h2 class="title" style="font-size: 1.8rem;">Seja Bem Vindo</h2>
                        <p class="subtitle">Crie uma conta para fazer login no TaskFlow</p>
                    </div>

                    <#if message?has_content>
                        <div class="alert alert-${message.type}">
                            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <form id="kc-register-form" action="${url.registrationAction}" method="post">
                        
                        <div class="input-group register-group">
                            <label for="firstName">Nome</label>
                            <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}" autofocus placeholder="Seu nome" required />
                        </div>
                        
                        <div class="input-group register-group">
                            <label for="lastName">Sobrenome</label>
                            <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}" placeholder="Seu sobrenome" required />
                        </div>

                        <div class="input-group register-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" value="${(register.formData.email!'')}" autocomplete="email" placeholder="voce@exemplo.com" required />
                        </div>
                        
                        <div class="input-group" style="display: none;">
                            <label for="username">Username</label>
                            <input type="text" id="username" name="username" value="${(register.formData.username!'')}" autocomplete="username" />
                        </div>

                        <div class="input-group register-group">
                            <label for="password">Senha</label>
                            <input type="password" id="password" name="password" autocomplete="new-password" placeholder="Sua senha" required />
                        </div>

                        <div class="input-group register-group">
                            <label for="password-confirm">Confirme a senha</label>
                            <input type="password" id="password-confirm" name="password-confirm" placeholder="Confirme a senha" required />
                        </div>

                        <div class="actions">
                            <a href="${url.loginUrl}" class="cancelLink">Cancelar</a>
                            <button class="btn-primary" type="submit" style="width: auto; margin-top: 0; padding: 12px 25px;">Criar conta</button>
                        </div>
                    </form>

                </div>
            </div>

            <!-- DIREITA (agora fica na Esquerda pelo reverse): Imagem e Logo -->
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
                    if (document.referrer) {
                        var refUrl = new URL(document.referrer);
                        if (refUrl.origin !== window.location.origin) {
                            return refUrl.origin;
                        }
                    }
                } catch (e) {}
                return "http://localhost:5173";
            }

            // Ao clicar em "Cancelar", passa pelo frontend (:5173/?start_login=1)
            // para limpar a flag de criação de conta antes de abrir a tela de login
            var cancelLink = document.querySelector('.cancelLink');
            if (cancelLink) {
                cancelLink.addEventListener('click', function (e) {
                    e.preventDefault();
                    window.location.replace(resolveFrontendOrigin() + '/?start_login=1');
                });
            }

            document.getElementById('kc-register-form').addEventListener('submit', function() {
                var email = document.getElementById('email').value;
                var username = document.getElementById('username');
                if (username) {
                    username.value = email;
                }
            });
        })();
    </script>
</body>
</html>
