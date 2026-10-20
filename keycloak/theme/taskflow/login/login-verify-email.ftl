<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Confirmar E-mail - TaskFlow</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/taskflow-theme.css">
    <meta name="view-transition" content="same-origin">
</head>
<body>
    <div class="container">
        <div class="card">
            
            <!-- ESQUERDA: Informações de Verificação -->
            <div class="formSection">
                <div class="formFade">
                    <div class="header">
                        <h2 class="title" style="font-size: 1.8rem;">Verifique seu E-mail</h2>
                        <p class="subtitle">Um link de ativação foi enviado para a sua caixa de entrada.</p>
                    </div>

                    <#if message?has_content>
                        <div class="alert alert-${message.type}" style="margin-bottom: 20px;">
                            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <div style="background: rgba(59, 130, 246, 0.08); padding: 14px 18px; border-radius: 10px; border: 1px solid rgba(59, 130, 246, 0.25); margin-bottom: 22px;">
                        <span style="font-size: 0.85rem; color: var(--text-light); display: block; margin-bottom: 4px;">E-mail cadastrado:</span>
                        <strong style="font-size: 1.05rem; color: var(--primary-blue); word-break: break-all;">
                            ${(user.email)!(user.username)!''}
                        </strong>
                    </div>

                    <p style="font-size: 0.95rem; line-height: 1.5; color: var(--text-dark); margin-bottom: 25px;">
                        Para concluir o seu cadastro e acessar suas organizações no TaskFlow, abra seu e-mail e clique no botão de confirmação.
                    </p>

                    <div class="actions" style="display: flex; flex-direction: column; gap: 12px; margin-top: 15px;">
                        <a href="${url.loginAction}" class="btn-primary" style="text-decoration: none; text-align: center; display: block; box-sizing: border-box; padding: 12px 20px;">
                            Reenviar e-mail de ativação
                        </a>
                        <a href="http://localhost:5173/" class="cancelLink" style="text-align: center; display: block; margin-top: 8px;">
                            Voltar para o TaskFlow
                        </a>
                    </div>
                </div>
            </div>

            <!-- DIREITA: Imagem e Logo TaskFlow -->
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
