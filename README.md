# API Challenge - Sistema de Gestão de Veterinários e Tutores

Este projeto é uma API RESTful desenvolvida com Spring Boot, focada na gestão de veterinários, tutores e seus animais.

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring Data JPA** (Persistência de dados)
- **Banco de Dados:**
  - **H2** (Em memória para desenvolvimento)
  - **Oracle Database** (Driver `ojdbc11` para produção)
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

1. Certifique-se de ter o JDK 21 instalado.
2. Utilize o Maven Wrapper para rodar o projeto:
   ```bash
   ./mvnw spring-boot:run
   ```
3. A documentação da API (Swagger) estará disponível em `/swagger-ui.html` (verifique a porta, geralmente 8080).
