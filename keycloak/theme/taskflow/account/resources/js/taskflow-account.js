/* =========================================================
   TASKFLOW - Account Console (Keycloak 26 / createBrowserRouter)
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

  function resolverBasePath() {
    var match = window.location.pathname.match(/^(.*?\/realms\/[^/]+\/account)(?:\/.*)?$/);
    return match ? match[1] : null;
  }

  function rotaAtual() {
    if (window.location.hash && window.location.hash.indexOf('#/') === 0) {
      var hashPath = window.location.hash.replace(/^#/, '').split(/[?&]/)[0] || '/';
      if (hashPath.length > 1 && hashPath.charAt(hashPath.length - 1) === '/') {
        hashPath = hashPath.slice(0, -1);
      }
      return hashPath;
    }
    var basePath = resolverBasePath();
    if (!basePath) return '/';
    var rel = window.location.pathname.slice(basePath.length) || '/';
    if (rel.length > 1 && rel.charAt(rel.length - 1) === '/') {
      rel = rel.slice(0, -1);
    }
    return rel;
  }

  function urlDaRota(rota) {
    var basePath = resolverBasePath();
    if (!basePath) return '#' + rota;
    var sufixo = rota === '/' ? '/' : rota;
    return basePath + sufixo + window.location.search;
  }

  // Qualquer rota fora da lista (ex.: /applications) volta para o perfil.
  function protegerRota() {
    var basePath = resolverBasePath();
    if (!basePath) return true;
    var rota = rotaAtual();
    if (ROTAS_PERMITIDAS.indexOf(rota) === -1) {
      window.location.replace(basePath + '/' + window.location.search);
      return false;
    }
    if (window.location.hash && window.location.hash.indexOf('#/') === 0) {
      window.location.replace(urlDaRota(rota));
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
      a.href = urlDaRota(aba.rota);
      a.textContent = aba.label;
      a.setAttribute('data-rota', aba.rota);
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

  function sincronizarNavegacao() {
    if (protegerRota()) atualizarAbaAtiva();
  }

  function envolverHistoryApi() {
    ['pushState', 'replaceState'].forEach(function (metodo) {
      var original = window.history[metodo];
      if (typeof original !== 'function') return;
      window.history[metodo] = function () {
        var retorno = original.apply(this, arguments);
        setTimeout(sincronizarNavegacao, 0);
        return retorno;
      };
    });
  }

  // Executa antes do React iniciar, para que ele já carregue a rota correta.
  protegerRota();
  envolverHistoryApi();
  document.documentElement.setAttribute('lang', 'pt-BR');

  window.addEventListener('popstate', sincronizarNavegacao);
  window.addEventListener('hashchange', sincronizarNavegacao);

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', montarBarraSuperior);
  } else {
    montarBarraSuperior();
  }
})();
