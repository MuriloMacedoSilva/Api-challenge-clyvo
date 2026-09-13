# API Challenge - Sistema de Gestão de Veterinários e Tutores

Este projeto é uma API RESTful desenvolvida com Spring Boot, focada na gestão de veterinários, tutores e seus animais.

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring Data JPA** (Persistência de dados)
- **Banco de Dados:**
  - **PostgreSQL 16** (desenvolvimento local persistente e produção)
  - **H2** (somente testes automatizados)
- **Lombok** (Redução de código boilerplate)
- **SpringDoc OpenAPI** (Swagger para documentação da API)

## Estrutura do Banco de Dados

As entidades principais são:

- **Tutor:** Armazena informações do tutor (nome, email, CPF, telefone, senha, role) e possui um conjunto de animais associados.
- **Veterinarian:** Armazena informações do veterinário.
- **Animal:** Entidade incorporada (`@ElementCollection`) em `Tutor`, representando os animais vinculados a um tutor.

## Endpoints Principais

### Tutor (`/tutor`)

- `GET /tutor/ping`: Verifica se a API está funcionando.
- `POST /tutor`: Cria um novo tutor.
- `POST /tutor/Login`: Autentica um tutor.
- `GET /tutor/{cpf}`: Busca um tutor pelo CPF.
- `PUT /tutor/{cpf}/CreateAnimal`: Adiciona um animal a um tutor específico.
- `GET /tutor/{cpf}/ReadAnimals`: Lista todos os animais de um tutor pelo CPF.

### Veterinarian (`/veterinarian`)

- `POST /veterinarian`: Cria um novo veterinário.
- `POST /veterinarian/Veterinarian/Login`: Autentica um veterinário.
- `GET /veterinarian/Veterinarian/{cpf}`: Busca um veterinário pelo CPF.

## IMPORTANTE Ressaltar que ao conforme o passar das sprints a api seguirá sendo atualizada e enrriquecida com mais funcionalidas rotas e tecnologias.

## Como Executar

1. Certifique-se de ter o JDK 21 e o Docker instalados.
2. Inicie o PostgreSQL local:
   ```bash
   docker compose up -d
   ```
3. Utilize o Maven Wrapper para rodar o projeto com o profile local default:
   ```bash
   ./mvnw spring-boot:run
   ```
4. A documentação da API (Swagger) estará disponível em `http://localhost:8080/swagger-ui.html`.

As configurações locais aceitam `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, com defaults compatíveis com o `docker-compose.yml`. Consulte `.env.example`.
