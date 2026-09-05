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
    <div class="container">
        <div class="card">
            
            <div class="formSection">
                <div class="formFade" style="text-align: center;">
                    <div class="header">
                        <h2 class="title">Aviso do Sistema</h2>
                        <p id="kc-message" class="subtitle" style="margin-top: 20px; font-size: 1.1rem; color: var(--text-dark);">
                            ${kcSanitize(message.summary)?no_esc}
                        </p>
                    </div>

                    <div class="actions" style="margin-top: 40px; display: flex; flex-direction: column; align-items: center; gap: 15px;">
                        
                        <#assign redirectUrl = "">
                        <#if pageRedirectUri?has_content>
                            <#assign redirectUrl = pageRedirectUri>
                        <#elseif actionUri?has_content>
                            <#assign redirectUrl = actionUri>
                        <#elseif (client.baseUrl)?has_content>
                            <#assign redirectUrl = client.baseUrl>
                        <#else>
                            <!-- Se o Keycloak se perder, forca a devolucao pro Frontend (Local) -->
                            <#assign redirectUrl = "http://localhost:5173">
                        </#if>

                        <a id="auto-redirect-btn" href="${redirectUrl}" class="btn-primary" style="text-decoration: none; padding: 12px 30px; display: inline-block;">
                            Continuar
                        </a>

                        <span style="font-size: 0.85rem; color: #888; margin-top: 10px;">
                            Você será redirecionado automaticamente em <span id="countdown">4</span> segundos...
                        </span>
                    </div>

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

    <script>
        let timeLeft = 4;
        const countdownEl = document.getElementById('countdown');
        let btnUrl = document.getElementById('auto-redirect-btn').href;

        // 1. Correcao de Rota (Se o Keycloak mandou pro 9090 raiz, forca o React)
        if (btnUrl.endsWith(':9090/') || btnUrl.endsWith(':9090')) {
            btnUrl = 'http://localhost:5173';
            document.getElementById('auto-redirect-btn').href = btnUrl;
        }

        const timer = setInterval(() => {
            timeLeft--;
            countdownEl.textContent = timeLeft;
            if (timeLeft <= 0) {
                clearInterval(timer);
                window.location.href = btnUrl;
            }
        }, 1000);
    </script>
</body>
</html>
