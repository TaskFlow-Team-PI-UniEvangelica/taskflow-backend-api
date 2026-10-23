# Estágio builder para otimizar o Keycloak com o tema embutido
FROM --platform=$BUILDPLATFORM quay.io/keycloak/keycloak:24.0.0 AS builder
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
COPY theme/taskflow /opt/keycloak/themes/taskflow
RUN /opt/keycloak/bin/kc.sh build

# Imagem final de runtime
FROM quay.io/keycloak/keycloak:24.0.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm-export.json /opt/keycloak/data/import/realm.json

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
