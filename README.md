# 💬 Messagerie web instantanée

> Application de messagerie web : API Spring Boot et interface Next.js.

[![CI](https://github.com/tameze-prince/Spring-Application-Messagerie-Web-instatane-code/actions/workflows/ci.yml/badge.svg)](https://github.com/tameze-prince/Spring-Application-Messagerie-Web-instatane-code/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-000000?logo=nextdotjs&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Structure

```
.
├── Backend/    API Spring Boot (Maven, Dockerfile, docker-compose.dev.yml, Makefile)
├── frontend/   Interface Next.js (TypeScript)
└── docs/       Documentation
```

Cahiers des charges : [`Backend/PRD.md`](Backend/PRD.md) et [`frontend/PRD-Frontend.md`](frontend/PRD-Frontend.md).

## Démarrage rapide

### Backend

```bash
cd Backend
cp .env.example .env              # puis renseigner les valeurs
docker compose -f docker-compose.dev.yml up -d
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

### Tests

```bash
cd Backend  && ./mvnw test
cd frontend && npm run lint
```

## Licence

[MIT](LICENSE) © ZEUFACK TAMEZE Prince