<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TaskFlow - Login</title>
    <meta name="view-transition" content="same-origin">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
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
</body>
</html>
