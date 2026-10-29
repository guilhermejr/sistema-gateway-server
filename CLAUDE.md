# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **API gateway** — the single entry point. Routes external requests to the right microservice, resolving instances through Eureka. Built on Spring Cloud Gateway (WebFlux).

| | |
|---|---|
| Port | `8080` |
| Public URL | `https://sistema-backend.guilhermejr.net` |

## Routes

Defined in `application.yml`, one per service: `/autenticacao-service/**`, `/energia-service/**`, `/supermercado-service/**`, `/remedios-service/**`, `/gastos-service/**`, `/ponto-service/**`, `/salario-service/**`, `/saude-service/**` — each to `lb://<service>/`.

There is **no route for `notificacao-service`**; it is internal only.

The route prefix is **not stripped**. Each service sets `server.servlet.context-path` to its own name, so `/saude-service/treinos/x` arrives at the service spelled exactly that way. If you add `StripPrefix`, you break it.

## What this does not do

It does **not** validate the JWT — it just forwards the `Authorization` header. Each service validates on its own.

## CORS

Configured per route through `metadata`, with the allowed origin coming from `frontend` in Vault.

## Debugging

A `503` means the load balancer found no instance in Eureka — the target service is down, or registered less than ~30s ago. A `500` with `Connection refused` means it routed correctly but nothing answered.

## Configuration comes from outside

This service stores almost no configuration of its own. `application.yml` only bootstraps `spring.config.import`, which pulls from:

- **Vault** — `secret/application` (shared: `JWTSecret`, actuator credentials, mail, AWS) and `secret/<service-name>` (its own DB credentials)
- **Config Server** — `server.port`, `server.servlet.context-path`, datasource, JPA settings

Both must be reachable or the service will not start.

`VAULT_TOKEN` is required and **has no default**. Without it Spring sends the literal string `${VAULT_TOKEN}` to Vault, gets a 403 that Spring Cloud Vault swallows (`fail-fast` is off), and the startup fails much later with a misleading `${someProperty} is malformed`. If you are chasing a confusing startup error, check `VAULT_TOKEN` first.

## Building and running

Java **21 only**. The Homebrew default JDK on this machine is newer and will break the build:

```bash
export JAVA_HOME=/Users/guilhermejr/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home
./mvnw clean package
VAULT_TOKEN=<token> java -jar target/*.jar --spring.profiles.active=dev
```

Do not raise `java.version` past 21 while ModelMapper is a dependency — the ByteBuddy bundled in it cannot generate classes on JDK 24+, and the app dies building its mappers with an `UnsupportedOperationException` that does not name the real cause.

## Deploying

`git push origin main` **is** the deploy. A `post-receive` hook on the VPS checks out, runs `mvn clean package` inside a throwaway `maven:3.9-amazoncorretto-21` container, builds the image and restarts it via docker compose. There is no separate release step.

Because the build happens on the VPS, any dependency from a private repository needs credentials **there**, not locally — the hook mounts `/home/guilhermejr/.m2` and passes `GITHUB_TOKEN`.

The `Dockerfile` only copies a prebuilt jar; it carries a `HEALTHCHECK` that polls `/actuator/health`.
