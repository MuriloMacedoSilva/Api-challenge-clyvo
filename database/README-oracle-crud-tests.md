# Plano de testes CRUD no Oracle 19c

## Escopo e estado

Este documento prepara a primeira validação CRUD controlada da API CLYVO no schema Oracle 19c institucional. Ele não autoriza a execução do roteiro nem a limpeza posterior.

Já foi confirmado:

- o DDL das 14 entidades foi criado manualmente no schema `RM566462`;
- a API iniciou com o profile `oracle`, conectou ao Oracle e concluiu `ddl-auto=validate`;
- Hibernate 7.2.12 criou o `EntityManagerFactory` e a aplicação iniciou na porta 8080;
- na etapa inicial desse roteiro, 121 testes automatizados com H2 haviam passado; o último resultado local registrado no projeto foi de 153 testes, não reexecutados nesta revisão;
- o teste H2 de `Animal.history` persistiu 40.000 caracteres.
- uma execução Oracle parcial confirmou ping, cadastro e login de Tutor e cadastro de Animal;
- a leitura inicial de Animals falhou com `LazyInitializationException` porque `TutorService` devolvia a coleção lazy para conversão no controller;
- a correção local passou em três testes HTTP com OSIV desabilitado, sem nova conexão ao Oracle.

Ainda não foi executado:

- cadastro real de Veterinário e os fluxos de vínculo, consulta, prontuário, prescrição ou vacinação no Oracle;
- releitura Oracle de Animals após a correção local;
- validação Oracle de identities, `FLOAT(24)`, `DATE`, `TIMESTAMP(6)`, CLOB, transações ou `@OrderColumn`;
- qualquer limpeza DML no schema institucional.

Todas as consultas SQL deste documento são somente leitura. Nenhum `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `TRUNCATE`, DDL ou migration deve ser executado durante a preparação.

## Condições para autorizar a execução futura

O ensaio mutável deve ser adiado até que todas as condições sejam verdadeiras:

1. O responsável pelo schema autorizou por escrito a criação e posterior remoção da massa de teste.
2. Existe uma janela sem outros ensaios usando as contas fictícias.
3. As consultas de pré-verificação retornaram zero conflito para CPF, CRMV e marcadores.
4. Foi criado um manifesto local, fora do Git, para registrar cada ID retornado pela API.
5. A estratégia de limpeza por IDs foi revisada antes do primeiro `POST`.
6. A limpeza recebeu autorização separada e será executada em uma única transação revisável.

Não registrar senha em relatório, comando versionado, screenshot ou log compartilhado. As respostas legadas de Tutor e Veterinário também expõem a senha; salvar apenas os IDs e identificadores necessários.

## Massa fictícia proposta

O marcador `ORA21_20261104_A` identifica esta execução. Se o roteiro for repetido, criar outro marcador e outros identificadores; nunca reutilizar silenciosamente uma massa anterior.

| Campo | Valor fictício |
|---|---|
| Tutor | `ORA21_20261104_A Tutor` |
| CPF Tutor | `91000000001` |
| E-mail Tutor | `ora21.a.tutor@example.test` |
| Veterinário | `ORA21_20261104_A Vet` |
| CPF Veterinário | `92000000001` |
| CRMV | `T21001` / `SP` |
| CNPJ | `99000000000001` |
| Animal principal | `ORA21_20261104_A Pet` |
| Animal descartável | `ORA21_20261104_A DeleteMe` |
| Vacina | `ORA21_20261104_A Vaccine` |

CPF e CRMV não possuem constraints únicas no banco. O Service verifica CPF antes do cadastro, mas isso não protege contra concorrência ou dados inseridos externamente. Por isso, a pré-verificação SQL é obrigatória.

## Manifesto de IDs

Registrar, sem senha:

```text
runMarker=
tutorId=
tutorCpf=
veterinarianId=
veterinarianCpf=
animalId=
deleteCandidateAnimalId=
linkId=
vaccinationId=
appointmentId=
medicalRecordId=
prescriptionId=
originalPrescriptionItemIds=
updatedPrescriptionItemIds=
```

Identities podem consumir números mesmo quando uma transação sofre rollback. Lacunas são normais; nunca inferir um ID por sequência ou por `MAX(id)`.

## Matriz dos endpoints

| Recurso | Criação | Leitura | Atualização/transição | Exclusão física |
|---|---|---|---|---|
| Tutor | `POST /tutor` | `GET /tutor/{cpf}` | não exposta | não exposta |
| Veterinário | `POST /veterinarian` | `GET /veterinarian/{cpf}` | não exposta | não exposta |
| Animal | `POST /tutor/{cpf}/create-animal` | `GET /tutor/{cpf}/read-animals` | `PUT /tutor/{cpf}/animals/{id}` ou `PUT /veterinarian/{cpf}/animals/{id}` | `DELETE /tutor/{cpf}/animals/{id}` |
| Vínculo | `POST /v1/links/request` | pending, Tutores aceitos e Animals vinculados | `PATCH /v1/links/{id}/respond` | não exposta |
| Consulta | `POST /v1/appointments/tutor/{cpf}` | listas por Tutor/Veterinário | confirmar ou cancelar | não exposta |
| Prontuário | `POST /v1/medical-records/appointments/{id}` | histórico por Animal | `PUT /v1/medical-records/{id}` | não exposta |
| Vacinação | `POST /v1/vaccinations/animals/{id}` | histórico por Animal | `PUT /v1/vaccinations/{id}` | `DELETE /v1/vaccinations/{id}` |
| Prescrição | `POST /v1/prescriptions/medical-records/{id}` | por prontuário | `PUT /v1/prescriptions/{id}` | não exposta |

Vínculo, consulta e prontuário são máquinas de estado, não CRUD genérico. Não há `GET` individual de Animal, Vínculo, Consulta ou Prontuário por ID.

### Inventário exato

Tutor e Animal:

```text
GET    /tutor/ping
POST   /tutor
POST   /tutor/login
GET    /tutor/{cpf}
POST   /tutor/{cpf}/create-animal
GET    /tutor/{cpf}/read-animals
PUT    /tutor/{cpf}/animals/{animalId}
DELETE /tutor/{cpf}/animals/{animalId}
```

Veterinário:

```text
POST /veterinarian
POST /veterinarian/login
GET  /veterinarian/{cpf}
GET  /veterinarian/{cpf}/tutors
PUT  /veterinarian/{cpf}/animals/{animalId}
```

Vínculos:

```text
POST  /v1/links/request?crmvNumber=&tutorCpf=
PATCH /v1/links/{linkId}/respond?tutorCpf=&accept=
GET   /v1/links/pending?tutorCpf=
GET   /v1/links/veterinarian/{veterinarianCpf}/tutor/{tutorCpf}/animals
GET   /v1/links/veterinarian/{veterinarianCpf}/tutors
```

Consultas:

```text
POST  /v1/appointments/tutor/{tutorCpf}
GET   /v1/appointments/tutor/{tutorCpf}
GET   /v1/appointments/tutor/{tutorCpf}/veterinarians
GET   /v1/appointments/veterinarian/{veterinarianCpf}
PATCH /v1/appointments/{appointmentId}/confirm?veterinarianCpf=
PATCH /v1/appointments/{appointmentId}/cancel/tutor?tutorCpf=
PATCH /v1/appointments/{appointmentId}/cancel/veterinarian?veterinarianCpf=
```

Prontuários:

```text
POST /v1/medical-records/appointments/{appointmentId}?veterinarianCpf=
PUT  /v1/medical-records/{recordId}?veterinarianCpf=
GET  /v1/medical-records/tutor/{tutorCpf}/animals/{animalId}
GET  /v1/medical-records/veterinarian/{veterinarianCpf}/animals/{animalId}
```

Vacinações:

```text
POST   /v1/vaccinations/animals/{animalId}?veterinarianCpf=
PUT    /v1/vaccinations/{vaccinationId}?veterinarianCpf=
DELETE /v1/vaccinations/{vaccinationId}?veterinarianCpf=
GET    /v1/vaccinations/tutor/{tutorCpf}/animals/{animalId}
GET    /v1/vaccinations/veterinarian/{veterinarianCpf}/animals/{animalId}
```

Prescrições, incluídas por causa do HHH160246:

```text
POST /v1/prescriptions/medical-records/{medicalRecordId}?veterinarianCpf=
PUT  /v1/prescriptions/{prescriptionId}?veterinarianCpf=
GET  /v1/prescriptions/tutor/{tutorCpf}/medical-records/{medicalRecordId}
GET  /v1/prescriptions/veterinarian/{veterinarianCpf}/medical-records/{medicalRecordId}
```

Login e ping são ações, não operações CRUD. Cancelamento de Appointment altera o status para `CANCELLED`; não remove a linha. O caminho mínimo abaixo usa confirmação e conclusão. Os dois endpoints de cancelamento devem ser testados com Appointments descartáveis adicionais e horários distintos somente se a autorização incluir essas linhas extras.

## Pré-verificação somente leitura

Executar como o mesmo usuário da aplicação. As duas contagens de conflito abaixo devem ser zero antes do primeiro cadastro:

```sql
SELECT COUNT(*) AS tutor_conflicts
  FROM tutor
 WHERE cpf = '91000000001'
    OR email = 'ora21.a.tutor@example.test'
    OR name LIKE 'ORA21_20261104_A%';

SELECT COUNT(*) AS veterinarian_conflicts
  FROM veterinarian
 WHERE cpf = '92000000001'
    OR crmv_number = 'T21001'
    OR cnpj = '99000000000001'
    OR email = 'ora21.a.vet@example.test'
    OR name LIKE 'ORA21_20261104_A%';
```

Confirmar as identities sem alterá-las:

```sql
SELECT table_name, column_name, generation_type, identity_options
  FROM user_tab_identity_cols
 WHERE table_name IN (
       'TUTOR', 'VETERINARIAN', 'ANIMAL',
       'TB_VETERINARIAN_TUTOR_LINKS', 'TB_APPOINTMENTS',
       'TB_MEDICAL_RECORDS', 'TB_PRESCRIPTIONS',
       'TB_PRESCRIPTION_ITEMS', 'TB_EXAMS', 'TB_VACCINATIONS',
       'TB_NOTIFICATIONS', 'TB_DEVICE_PUSH_TOKENS',
       'TB_CONVERSATIONS', 'TB_MESSAGES'
 )
 ORDER BY table_name;
```

Esperado: 14 linhas, todas com `GENERATION_TYPE = 'BY DEFAULT'`.

## Roteiro HTTP ordenado

Os exemplos assumem:

```bash
BASE_URL=http://localhost:8080
```

Substituir IDs entre chaves pelos valores capturados. `2099-12-15T14:30:00` é deliberadamente futuro; antes da execução, confirmar que não conflita com outro teste do mesmo Veterinário.

### 1. Smoke test

```bash
curl -i "$BASE_URL/tutor/ping"
```

Esperado: `200` e `ping ok!`. Este passo não testa persistência.

### 2. Criar e ler Tutor

```bash
curl -i -X POST "$BASE_URL/tutor" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "ORA21_20261104_A Tutor",
    "email": "ora21.a.tutor@example.test",
    "cpf": "91000000001",
    "phoneNumber": "11910000001",
    "password": "Test2101",
    "role": "TUTOR"
  }'

curl -i "$BASE_URL/tutor/91000000001"
```

Esperado: `201`, depois `200`; capturar `tutorId`. O `id` deve ser gerado pelo Oracle, sem ser enviado no request.

Verificação SQL, substituindo `:tutor_id`:

```sql
SELECT id, name, email, cpf, phone_number, role
  FROM tutor
 WHERE id = :tutor_id
   AND cpf = '91000000001'
   AND name = 'ORA21_20261104_A Tutor';
```

Esperado: exatamente uma linha.

### 3. Criar e ler Veterinário

```bash
curl -i -X POST "$BASE_URL/veterinarian" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "ORA21_20261104_A Vet",
    "email": "ora21.a.vet@example.test",
    "cpf": "92000000001",
    "phoneNumber": "11920000001",
    "password": "Test2101",
    "role": "VETERINARIAN",
    "crmvNumber": "T21001",
    "crmvState": "SP",
    "cnpj": "99000000000001"
  }'

curl -i "$BASE_URL/veterinarian/92000000001"
```

Esperado: `201`, depois `200`; capturar `veterinarianId`.

```sql
SELECT id, name, email, cpf, crmv_number, crmv_state, cnpj
  FROM veterinarian
 WHERE id = :veterinarian_id
   AND cpf = '92000000001'
   AND crmv_number = 'T21001';
```

Esperado: exatamente uma linha e CRMV preservado como texto.

### 4. Criar dois Animals

O endpoint recebe um array. O primeiro Animal seguirá o fluxo clínico; o segundo será usado para validar exclusão sem FKs clínicas.

```bash
curl -i -X POST "$BASE_URL/tutor/91000000001/create-animal" \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "name": "ORA21_20261104_A Pet",
      "weight": 12.5,
      "height": 42.25,
      "age": 5,
      "race": "SRD",
      "species": "Canina",
      "history": "ORA21_20261104_A histórico CLOB"
    },
    {
      "name": "ORA21_20261104_A DeleteMe",
      "weight": 4.75,
      "height": 24.5,
      "age": 2,
      "race": "SRD",
      "species": "Felina",
      "history": null
    }
  ]'

curl -i "$BASE_URL/tutor/91000000001/read-animals"
```

Esperado: `201`, depois `200`; capturar os dois IDs pelo nome.

Na primeira execução parcial, o `POST` foi concluído, mas o `GET` respondeu `500` com `LazyInitializationException`: a coleção `Tutor.animals` era devolvida pelo service e percorrida pelo controller depois do encerramento da transação. O código agora consulta os Animals diretamente pelo CPF, converte para `AnimalResponseDTO` dentro de `@Transactional(readOnly = true)` e mantém OSIV desabilitado. Três testes MockMvc com H2 cobrem Tutor com Animals, coleção vazia e a listagem análoga de Tutores vinculados. O resultado Oracle deste passo continua pendente; não houve reteste nem limpeza institucional durante a correção.

```sql
SELECT id, tutor_id, name, weight, height, age, race, species,
       DBMS_LOB.GETLENGTH(history) AS history_length,
       DBMS_LOB.COMPARE(
           history,
           TO_CLOB('ORA21_20261104_A histórico CLOB')
       ) AS history_match
  FROM animal
 WHERE tutor_id = :tutor_id
   AND id IN (:animal_id, :delete_candidate_animal_id)
 ORDER BY id;
```

Esperado: duas linhas; FKs iguais a `:tutor_id`; floats próximos dos valores enviados; CLOB principal não nulo e `HISTORY_MATCH = 0`. O GET também deve devolver exatamente o histórico submetido. Comparar `FLOAT(24)` com tolerância, nunca por representação textual exata.

### 5. Atualizar e excluir o Animal descartável

```bash
curl -i -X PUT \
  "$BASE_URL/tutor/91000000001/animals/{deleteCandidateAnimalId}" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "ORA21_20261104_A DeleteMe Updated",
    "weight": 5.0,
    "height": 25.0,
    "age": 3,
    "race": "SRD",
    "species": "Felina",
    "history": "atualização antes da exclusão"
  }'

curl -i -X DELETE \
  "$BASE_URL/tutor/91000000001/animals/{deleteCandidateAnimalId}"
```

Esperado: `200`, depois `204`.

```sql
SELECT COUNT(*) AS remaining_delete_candidate
  FROM animal
 WHERE id = :delete_candidate_animal_id
   AND tutor_id = :tutor_id;
```

Esperado: zero. Isso valida `orphanRemoval` do Tutor sem dependências clínicas.

### 6. Solicitar e aceitar vínculo

```bash
curl -i -X POST \
  "$BASE_URL/v1/links/request?crmvNumber=T21001&tutorCpf=91000000001"

curl -i \
  "$BASE_URL/v1/links/pending?tutorCpf=91000000001"
```

Esperado: `200` nos dois requests; o segundo retorna uma entrada `PENDING`. Capturar `linkId`.

```sql
SELECT id, veterinarian_id, tutor_id, status, created_at, updated_at
  FROM tb_veterinarian_tutor_links
 WHERE id = :link_id
   AND veterinarian_id = :veterinarian_id
   AND tutor_id = :tutor_id;

SELECT id, type, is_read, tutor_id, veterinarian_id, link_id
  FROM tb_notifications
 WHERE link_id = :link_id
 ORDER BY id;

SELECT id, type, is_read, tutor_id, veterinarian_id, link_id
  FROM tb_notifications
 WHERE veterinarian_id = :veterinarian_id
   AND type = 'LINK_REQUEST_SENT'
 ORDER BY id DESC;
```

Esperado: vínculo `PENDING`; uma Notification `LINK_REQUEST_RECEIVED` com `LINK_ID` para o Tutor e uma `LINK_REQUEST_SENT` sem `LINK_ID` para o Veterinário, ambas com `IS_READ = 0`. Oracle 19c representa booleanos como `NUMBER(1,0)`, portanto não usar `TRUE`/`FALSE` em SQL.

Aceitar:

```bash
curl -i -X PATCH \
  "$BASE_URL/v1/links/{linkId}/respond?tutorCpf=91000000001&accept=true"

curl -i \
  "$BASE_URL/v1/links/veterinarian/92000000001/tutor/91000000001/animals"

curl -i \
  "$BASE_URL/v1/appointments/tutor/91000000001/veterinarians"
```

Esperado: `204` no PATCH, `200` no GET de Animals e `200` no GET de Veterinários; o Animal principal e o Veterinário devem aparecer. SQL deve mostrar `STATUS = 'ACCEPTED'` e `UPDATED_AT` não nulo.

### 7. Atualizar Animal pelo Veterinário vinculado

```bash
curl -i -X PUT \
  "$BASE_URL/veterinarian/92000000001/animals/{animalId}" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "ORA21_20261104_A Pet",
    "weight": 12.75,
    "height": 42.5,
    "age": 5,
    "race": "SRD",
    "species": "Canina",
    "history": "ORA21_20261104_A atualizado pelo Vet"
  }'
```

Esperado: `200`; Tutor e propriedade do Animal não mudam.

```sql
SELECT id, tutor_id, name, weight, height,
       DBMS_LOB.GETLENGTH(history) AS history_length
  FROM animal
 WHERE id = :animal_id
   AND tutor_id = :tutor_id;
```

### 8. CRUD completo de vacinação

Criação:

```bash
curl -i -X POST \
  "$BASE_URL/v1/vaccinations/animals/{animalId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "vaccineName": "ORA21_20261104_A Vaccine",
    "applicationDate": "2026-11-04",
    "nextDoseDate": "2027-11-04",
    "batchNumber": "ORA21-A-001",
    "manufacturer": "CLYVO Test Lab",
    "observations": "registro fictício para validação Oracle"
  }'
```

Esperado: `201`; capturar `vaccinationId`. A criação também persiste uma Notification do Tutor na mesma transação.

Leitura pelos dois participantes:

```bash
curl -i \
  "$BASE_URL/v1/vaccinations/tutor/91000000001/animals/{animalId}"

curl -i \
  "$BASE_URL/v1/vaccinations/veterinarian/92000000001/animals/{animalId}"
```

Esperado: `200` e o mesmo registro.

```sql
SELECT id, animal_id, veterinarian_id, vaccine_name,
       TO_CHAR(application_date, 'YYYY-MM-DD HH24:MI:SS') AS application_date,
       TO_CHAR(next_dose_date, 'YYYY-MM-DD HH24:MI:SS') AS next_dose_date,
       batch_number, manufacturer, created_at, updated_at
  FROM tb_vaccinations
 WHERE id = :vaccination_id
   AND animal_id = :animal_id
   AND veterinarian_id = :veterinarian_id;
```

Esperado: exatamente uma linha; componentes de hora das duas datas iguais a `00:00:00`.

Atualização:

```bash
curl -i -X PUT \
  "$BASE_URL/v1/vaccinations/{vaccinationId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "vaccineName": "ORA21_20261104_A Vaccine Updated",
    "applicationDate": "2026-11-04",
    "nextDoseDate": "2027-12-04",
    "batchNumber": "ORA21-A-002",
    "manufacturer": "CLYVO Test Lab",
    "observations": "atualização fictícia"
  }'
```

Esperado: `200`; mesmo ID, Animal, autor e `createdAt`; `updatedAt` preenchido.

Exclusão:

```bash
curl -i -X DELETE \
  "$BASE_URL/v1/vaccinations/{vaccinationId}?veterinarianCpf=92000000001"
```

Esperado: `204`; a Notification criada anteriormente permanece, pois não existe cascade ou remoção associada.

```sql
SELECT COUNT(*) AS remaining_vaccination
  FROM tb_vaccinations
 WHERE id = :vaccination_id;
```

Esperado: zero.

### 9. Criar, listar e confirmar consulta

```bash
curl -i -X POST \
  "$BASE_URL/v1/appointments/tutor/91000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "animalId": {animalId},
    "veterinarianCpf": "92000000001",
    "scheduledAt": "2099-12-15T14:30:00",
    "reason": "ORA21_20261104_A consulta fictícia"
  }'
```

Esperado: `201`, `status = PENDING`; capturar `appointmentId`.

```bash
curl -i "$BASE_URL/v1/appointments/tutor/91000000001"
curl -i "$BASE_URL/v1/appointments/veterinarian/92000000001"

curl -i -X PATCH \
  "$BASE_URL/v1/appointments/{appointmentId}/confirm?veterinarianCpf=92000000001"
```

Esperado: listas `200`; confirmação `200` com `status = CONFIRMED`.

```sql
SELECT id, animal_id, tutor_id, veterinarian_id,
       TO_CHAR(scheduled_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF6') AS scheduled_at,
       reason, status, created_at, updated_at
  FROM tb_appointments
 WHERE id = :appointment_id
   AND animal_id = :animal_id
   AND tutor_id = :tutor_id
   AND veterinarian_id = :veterinarian_id;
```

Esperado: uma linha, FKs corretas e `STATUS = 'CONFIRMED'` após o PATCH.

### 10. Criar e atualizar prontuário

```bash
curl -i -X POST \
  "$BASE_URL/v1/medical-records/appointments/{appointmentId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "diagnosis": "ORA21_20261104_A avaliação fictícia",
    "description": "Animal estável em ensaio controlado.",
    "weight": 12.75,
    "temperature": 38.4,
    "observations": "Sem valor clínico; massa de teste."
  }'
```

Esperado: `201`; capturar `medicalRecordId`. Na mesma transação, o Appointment deve mudar para `COMPLETED` e uma Notification deve ser criada para o Tutor.

```sql
SELECT mr.id, mr.appointment_id, mr.animal_id, mr.veterinarian_id,
       mr.diagnosis, mr.weight, mr.temperature,
       a.status AS appointment_status
  FROM tb_medical_records mr
  JOIN tb_appointments a ON a.id = mr.appointment_id
 WHERE mr.id = :medical_record_id
   AND mr.appointment_id = :appointment_id
   AND mr.animal_id = :animal_id
   AND mr.veterinarian_id = :veterinarian_id;

SELECT id, type, is_read, tutor_id
  FROM tb_notifications
 WHERE tutor_id = :tutor_id
   AND type = 'MEDICAL_RECORD_CREATED'
 ORDER BY id DESC;
```

Esperado: uma linha de prontuário, Appointment `COMPLETED` e Notification com `IS_READ = 0`. A presença conjunta valida o commit atômico bem-sucedido; não prova sozinha rollback em falha.

Leitura e atualização:

```bash
curl -i \
  "$BASE_URL/v1/medical-records/tutor/91000000001/animals/{animalId}"

curl -i \
  "$BASE_URL/v1/medical-records/veterinarian/92000000001/animals/{animalId}"

curl -i -X PUT \
  "$BASE_URL/v1/medical-records/{medicalRecordId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "diagnosis": "ORA21_20261104_A avaliação fictícia atualizada",
    "description": "Descrição atualizada para validar UPDATE.",
    "weight": 12.8,
    "temperature": 38.3,
    "observations": "Atualização sem valor clínico."
  }'
```

Esperado: leituras `200`; atualização `200`, mesmo ID/FKs/`createdAt` e `updatedAt` preenchido.

### 11. Testes negativos e transacionais

Repetir a criação do prontuário para o mesmo Appointment:

```bash
curl -i -X POST \
  "$BASE_URL/v1/medical-records/appointments/{appointmentId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "diagnosis": "duplicado",
    "description": "não deve persistir",
    "weight": 12.8,
    "temperature": 38.3,
    "observations": null
  }'
```

Esperado: `409`; a contagem abaixo continua 1:

```sql
SELECT COUNT(*) AS medical_record_count
  FROM tb_medical_records
 WHERE appointment_id = :appointment_id;
```

Tentar excluir o Animal principal depois que o Appointment o referencia:

```bash
curl -i -X DELETE \
  "$BASE_URL/tutor/91000000001/animals/{animalId}"
```

Esperado: `409` por integridade referencial. Confirmar que a transação foi revertida e o Animal permanece:

```sql
SELECT COUNT(*) AS animal_still_present
  FROM animal
 WHERE id = :animal_id
   AND tutor_id = :tutor_id;
```

Esperado: 1. Este é o teste de rollback mais seguro disponível pelo HTTP atual depois de uma tentativa real de exclusão. Valida rollback da remoção do Animal diante da FK, sem fabricar erro de schema.

## Ensaio específico do aviso HHH160246

### Causa

`Prescription.items` usa simultaneamente:

```java
@OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderColumn(name = "item_order")
```

`mappedBy` torna a coleção o lado inverso; `PrescriptionItem.prescription` é o proprietário de `prescription_id`. Hibernate 7.2 emite HHH160246 porque o lado inverso declara a coluna de ordem. O aviso não é específico do Oracle.

Hibernate mantém compatibilidade legada e tende a inserir os itens e depois atualizar `item_order`. O DDL permite esse estado intermediário porque `ITEM_ORDER` é nullable e não possui unique constraint. Isso deve ser validado no Oracle, não assumido.

### Criação em ordem não alfabética

```bash
curl -i -X POST \
  "$BASE_URL/v1/prescriptions/medical-records/{medicalRecordId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "instructions": "ORA21_20261104_A prescrição fictícia",
    "items": [
      {
        "medicationName": "Zeta Test",
        "dosage": "1 unidade",
        "frequency": "12/12h",
        "duration": "2 dias",
        "route": "ORAL",
        "instructions": "item zero"
      },
      {
        "medicationName": "Alpha Test",
        "dosage": "2 unidades",
        "frequency": "24/24h",
        "duration": "3 dias",
        "route": "ORAL",
        "instructions": "item um"
      },
      {
        "medicationName": "Mu Test",
        "dosage": "3 unidades",
        "frequency": "8/8h",
        "duration": "1 dia",
        "route": "TÓPICA",
        "instructions": "item dois"
      }
    ]
  }'
```

Esperado: `201`; capturar `prescriptionId` e todos os IDs originais dos itens.

```sql
SELECT id, prescription_id, medication_name, item_order
  FROM tb_prescription_items
 WHERE prescription_id = :prescription_id
 ORDER BY item_order;
```

Esperado: três linhas, `ITEM_ORDER` exatamente `0, 1, 2`, sem null/duplicidade, preservando `Zeta`, `Alpha`, `Mu`.

Confirmar leitura em requests posteriores, que usam outro contexto de persistência:

```bash
curl -i \
  "$BASE_URL/v1/prescriptions/tutor/91000000001/medical-records/{medicalRecordId}"

curl -i \
  "$BASE_URL/v1/prescriptions/veterinarian/92000000001/medical-records/{medicalRecordId}"
```

### Substituição, reordenação e orphan removal

```bash
curl -i -X PUT \
  "$BASE_URL/v1/prescriptions/{prescriptionId}?veterinarianCpf=92000000001" \
  -H 'Content-Type: application/json' \
  -d '{
    "instructions": "ORA21_20261104_A prescrição substituída",
    "items": [
      {
        "medicationName": "Alpha Test",
        "dosage": "2 unidades",
        "frequency": "24/24h",
        "duration": "4 dias",
        "route": "ORAL",
        "instructions": "agora item zero"
      },
      {
        "medicationName": "Beta New Test",
        "dosage": "1 unidade",
        "frequency": "12/12h",
        "duration": "2 dias",
        "route": "ORAL",
        "instructions": "novo item um"
      }
    ]
  }'
```

Esperado: `200`; dois itens em nova ordem. A implementação limpa a coleção e cria novas entidades, portanto todos os IDs originais devem desaparecer, inclusive o antigo `Alpha Test`.

```sql
SELECT id, medication_name, item_order
  FROM tb_prescription_items
 WHERE prescription_id = :prescription_id
 ORDER BY item_order;

SELECT COUNT(*) AS old_items_remaining
  FROM tb_prescription_items
 WHERE id IN (:old_item_id_1, :old_item_id_2, :old_item_id_3);
```

Esperado: posições `0, 1`; segunda consulta igual a zero. Se houver null, índice duplicado, ordem incorreta ou item antigo, registrar incompatibilidade e interromper o roteiro. Não alterar o mapping nesta fase.

## Inventário final somente leitura

Antes de solicitar limpeza, conferir toda dependência dos IDs capturados:

```sql
SELECT 'TUTOR' AS object_type, COUNT(*) AS row_count
  FROM tutor WHERE id = :tutor_id AND cpf = '91000000001'
UNION ALL
SELECT 'VETERINARIAN', COUNT(*) FROM veterinarian
 WHERE id = :veterinarian_id AND cpf = '92000000001'
UNION ALL
SELECT 'ANIMAL', COUNT(*)
  FROM animal WHERE tutor_id = :tutor_id
UNION ALL
SELECT 'LINK', COUNT(*) FROM tb_veterinarian_tutor_links
 WHERE tutor_id = :tutor_id OR veterinarian_id = :veterinarian_id
UNION ALL
SELECT 'APPOINTMENT', COUNT(*) FROM tb_appointments
 WHERE tutor_id = :tutor_id OR veterinarian_id = :veterinarian_id OR animal_id = :animal_id
UNION ALL
SELECT 'MEDICAL_RECORD', COUNT(*) FROM tb_medical_records
 WHERE veterinarian_id = :veterinarian_id OR animal_id = :animal_id OR appointment_id = :appointment_id
UNION ALL
SELECT 'PRESCRIPTION', COUNT(*) FROM tb_prescriptions
 WHERE medical_record_id = :medical_record_id
UNION ALL
SELECT 'PRESCRIPTION_ITEM', COUNT(*) FROM tb_prescription_items
 WHERE prescription_id = :prescription_id
UNION ALL
SELECT 'EXAM', COUNT(*) FROM tb_exams
 WHERE medical_record_id = :medical_record_id
UNION ALL
SELECT 'VACCINATION', COUNT(*) FROM tb_vaccinations
 WHERE animal_id = :animal_id OR veterinarian_id = :veterinarian_id
UNION ALL
SELECT 'NOTIFICATION', COUNT(*) FROM tb_notifications
 WHERE tutor_id = :tutor_id OR veterinarian_id = :veterinarian_id OR link_id = :link_id
UNION ALL
SELECT 'PUSH_TOKEN', COUNT(*) FROM tb_device_push_tokens
 WHERE tutor_id = :tutor_id OR veterinarian_id = :veterinarian_id
UNION ALL
SELECT 'CONVERSATION', COUNT(*) FROM tb_conversations
 WHERE tutor_id = :tutor_id OR veterinarian_id = :veterinarian_id
UNION ALL
SELECT 'MESSAGE', COUNT(*) FROM tb_messages m
 WHERE EXISTS (
       SELECT 1 FROM tb_conversations c
        WHERE c.id = m.conversation_id
          AND (c.tutor_id = :tutor_id OR c.veterinarian_id = :veterinarian_id)
 );
```

Qualquer vínculo, consulta, Animal ou conversa não previsto no manifesto deve bloquear a limpeza e abrir investigação.

## Estratégia de limpeza segura

### Limite da API

A API consegue remover fisicamente Vaccination e Animal sem dependências. Cancelar Appointment apenas altera status. Não existem endpoints para excluir Tutor, Veterinário, Vínculo, Appointment, MedicalRecord, Prescription, Exam, Conversation ou Message.

Depois da criação do Appointment, limpeza completa somente pela API é impossível. Os endpoints de limpeza de Notification removem todas as notificações do CPF e são amplos demais para uso sem provar que a conta é integralmente descartável.

### Limpeza posterior, condicionada a nova autorização

Preparar um script separado somente depois de capturar e conferir todos os IDs. O script deverá:

1. Exigir uma chave explícita de autorização e abortar por padrão.
2. Confirmar Tutor, Veterinário, Animal, Vínculo, Appointment e MedicalRecord pela combinação de ID e marcador.
3. Abortar se existirem relações inesperadas com qualquer conta descartável.
4. Abrir uma transação e um savepoint, sem DDL ou `TRUNCATE`.
5. Remover em ordem: Messages, Conversations, PrescriptionItems, Prescription, Exams, Vaccinations, MedicalRecord, Appointment, Notifications, DevicePushTokens, Link, Animals, Tutor, Veterinarian.
6. Restringir cada operação aos IDs capturados; CPF, CRMV, nome ou intervalo de datas isolados não são filtros suficientes.
7. Conferir `SQL%ROWCOUNT` esperado para cada recurso principal.
8. Reexecutar o inventário na mesma sessão.
9. Executar `COMMIT` somente após revisão humana; caso contrário, `ROLLBACK`.

Esta fase não cria nem executa esse script DML.

## Riscos e critérios de interrupção

- **Schema compartilhado:** nomes antigos não colidem com as 14 tabelas, mas novos dados de terceiros podem surgir durante o ensaio. Relação inesperada bloqueia a limpeza.
- **Identidade acadêmica:** CPF/CRMV em path/query não são autenticação. Executar somente em API local não exposta.
- **Ausência de unique:** CPF, CRMV, e-mail e CNPJ não são únicos no Oracle. Pré-verificar e evitar concorrência.
- **Senhas em texto:** requests e responses legados contêm senha. Não salvar respostas completas.
- **FKs sem cascade:** ordem errada de limpeza falha; nunca desabilitar constraints.
- **Identity:** gaps após rollback são normais; não reiniciar identities.
- **Booleanos:** Oracle usa `NUMBER(1,0)` e checks `0/1`.
- **Datas:** `LocalDate` usa `DATE`, que também comporta hora; esperar meia-noite. `LocalDateTime` usa `TIMESTAMP(6)` sem timezone.
- **FLOAT(24):** comparar peso/altura/temperatura com tolerância.
- **Unicode:** `VARCHAR2(... CHAR)` continua limitado a 4000 bytes em AL32UTF8. Não usar conteúdo multibyte próximo do limite no roteiro base.
- **String vazia:** Oracle converte `''` em `NULL`; usar `null` explicitamente para opcionais e texto real para obrigatórios.
- **CLOB:** `Animal.history` deve ser verificado por `DBMS_LOB.GETLENGTH`; não usar igualdade direta sobre CLOB.
- **Lazy loading:** responses devem materializar DTOs dentro da fronteira transacional. Não habilitar OSIV nem tornar `Tutor.animals` eager para contornar sessão encerrada.
- **Conflito de agenda:** a verificação ocorre antes do insert e não possui lock/constraint contra duas criações concorrentes no mesmo horário. Não executar o passo em paralelo.
- **Notificações:** vínculo, consulta, prontuário, prescrição e vacinação criam linhas adicionais. Push ocorre somente `AFTER_COMMIT`; falha externa não desfaz domínio já confirmado.
- **HHH160246:** qualquer ordem nula/duplicada, leitura reordenada ou orphan não removido deve interromper o ensaio e ser tratado como incompatibilidade antes de alterar mapping.

## Critério de conclusão

O ensaio será considerado aprovado somente quando:

- todos os status HTTP e estados persistidos corresponderem ao roteiro;
- IDs forem gerados e todas as FKs apontarem para os registros capturados;
- Animal CLOB, floats, datas e timestamps forem relidos corretamente;
- MedicalRecord, conclusão do Appointment e Notification aparecerem juntos;
- a tentativa de excluir Animal referenciado falhar sem remover o Animal;
- Vaccination completar create/read/update/delete;
- `ITEM_ORDER` preservar a ordem e o PUT de Prescription remover todos os itens antigos;
- o inventário final corresponder exatamente ao manifesto;
- a limpeza autorizada posterior remover apenas a massa fictícia e a verificação final retornar zero para os IDs capturados.
