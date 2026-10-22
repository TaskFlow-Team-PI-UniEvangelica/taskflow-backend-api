/* =========================================================
   TASKFLOW - Account Console (Keycloak 24 / keycloak.v3)
   - Bloqueia rotas não essenciais (Aplicativos, Dispositivos, Grupos...)
   - Substitui o cabeçalho e a barra lateral do Keycloak por uma
     barra superior própria com abas em português.
   ========================================================= */
(function () {
  'use strict';

  var ABAS = [
    { rota: '/', label: 'Meu Perfil' },
    { rota: '/account-security/signing-in', label: 'Senha e Segurança' }
  ];
  var ROTAS_PERMITIDAS = ABAS.map(function (a) { return a.rota; });

  function rotaAtual() {
    var path = (window.location.hash || '#/').replace(/^#/, '').split(/[?&]/)[0] || '/';
    if (path.length > 1 && path.charAt(path.length - 1) === '/') path = path.slice(0, -1);
    return path;
  }

  // Qualquer rota fora da lista (ex.: #/applications) volta para o perfil.
  function protegerRota() {
    if (ROTAS_PERMITIDAS.indexOf(rotaAtual()) === -1) {
      window.location.replace(window.location.pathname + window.location.search + '#/');
      return false;
    }
    return true;
  }

  function atualizarAbaAtiva() {
    var rota = rotaAtual();
    var links = document.querySelectorAll('.tf-tabs a');
    for (var i = 0; i < links.length; i++) {
      var ativa = links[i].getAttribute('data-rota') === rota;
      links[i].classList.toggle('tf-active', ativa);
      if (ativa) links[i].setAttribute('aria-current', 'page');
      else links[i].removeAttribute('aria-current');
    }
  }

  function voltarAoApp() {
    // A conta é aberta em nova aba pelo frontend (window.open), então fechamos a aba.
    window.close();
    // Fallback caso o navegador não permita fechar a aba.
    setTimeout(function () {
      if (document.referrer) window.location.href = document.referrer;
      else window.history.back();
    }, 200);
  }

  function montarBarraSuperior() {
    if (document.querySelector('.tf-topbar')) return;

    var header = document.createElement('header');
    header.className = 'tf-topbar';

    var brand = document.createElement('div');
    brand.className = 'tf-brand';
    brand.innerHTML = '<span>Task</span>Flow';

    var nav = document.createElement('nav');
    nav.className = 'tf-tabs';
    nav.setAttribute('aria-label', 'Navegação da conta');
    ABAS.forEach(function (aba) {
      var a = document.createElement('a');
      a.href = '#' + aba.rota;
      a.textContent = aba.label;
      a.setAttribute('data-rota', aba.rota);
      a.addEventListener('click', function(e) {
        e.preventDefault();
        window.location.hash = aba.rota;
      });
      nav.appendChild(a);
    });

    var voltar = document.createElement('button');
    voltar.type = 'button';
    voltar.className = 'tf-back';
    voltar.textContent = 'Voltar ao TaskFlow';
    voltar.addEventListener('click', voltarAoApp);

    header.appendChild(brand);
    header.appendChild(nav);
    header.appendChild(voltar);

    // Inserido fora do #app para não interferir na árvore do React.
    document.body.insertBefore(header, document.body.firstChild);
    atualizarAbaAtiva();
  }

  // Executa antes do React iniciar, para que ele já carregue a rota correta.
  protegerRota();
  document.documentElement.setAttribute('lang', 'pt-BR');

  window.addEventListener('hashchange', function () {
    if (protegerRota()) atualizarAbaAtiva();
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', montarBarraSuperior);
  } else {
    montarBarraSuperior();
  }
})();
