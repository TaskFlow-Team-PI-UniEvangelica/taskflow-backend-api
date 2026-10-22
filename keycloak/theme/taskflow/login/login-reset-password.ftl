<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recuperar Senha - TaskFlow</title>
    <meta name="view-transition" content="same-origin">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
</head>
<body>
    <div class="container">
        <div class="card">
            
            <div class="formSection">
                <div class="formFade">
                    <div class="header">
                        <h2 class="title">Recuperar Senha</h2>
                        <p class="subtitle">Insira seu e-mail para receber o link de recuperação.</p>
                    </div>

                    <#if message?has_content >
                        <div class="alert alert-${message.type}">
                            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <form id="kc-reset-password-form" class="form" action="${url.loginAction}" method="post">
                        <div class="input-group">
                            <label for="username">E-mail de Recuperação</label>
                            <input type="text" id="username" name="username" class="form-control" autofocus autocomplete="email" placeholder="seu@email.com" required/>
                        </div>

                        <button tabindex="4" class="btn-primary" type="submit">Enviar Instruções</button>

                        <div style="text-align: center; margin-top: 15px;">
                            <a href="${url.loginUrl}" class="forgotLink" style="font-weight: bold;">Voltar para o Login</a>
                        </div>
                    </form>

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
