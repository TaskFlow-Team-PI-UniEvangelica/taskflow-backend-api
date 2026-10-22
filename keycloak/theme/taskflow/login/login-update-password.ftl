<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Atualizar Senha - TaskFlow</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
    <meta name="view-transition" content="same-origin">
</head>
<body>
    <div class="container">
        <!-- Mantendo o mesmo padrao de layout (imagem na direita) -->
        <div class="card">
            
            <div class="formSection">
                <div class="formFade">
                    <div class="header">
                        <h2 class="title">Atualizar Senha</h2>
                        <p class="subtitle">Por favor, digite sua nova senha de acesso abaixo.</p>
                    </div>

                    <#if message?has_content>
                        <div class="alert alert-${message.type}">
                            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <form id="kc-passwd-update-form" class="form" action="${url.loginAction}" method="post">
                        
                        <div class="input-group">
                            <label for="password-new">Nova Senha</label>
                            <input type="password" id="password-new" name="password-new" class="form-control" autofocus autocomplete="new-password" placeholder="Sua nova senha" required/>
                        </div>

                        <div class="input-group">
                            <label for="password-confirm">Confirme a nova senha</label>
                            <input type="password" id="password-confirm" name="password-confirm" class="form-control" autocomplete="new-password" placeholder="Confirme a nova senha" required/>
                        </div>

                        <!-- Botoes -->
                        <div class="actions" style="margin-top: 25px;">
                            <button tabindex="4" class="btn-primary" type="submit">Atualizar Senha</button>
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
