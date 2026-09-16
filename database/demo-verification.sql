-- Consultas somente leitura para a demonstracao academica do CLYVO.
-- Execute com: psql --pset=pager=off -f /seed/demo-verification.sql, se o arquivo for copiado,
-- ou cole as consultas na sessao psql aberta no container PostgreSQL.

-- Contagem das 14 tabelas depois do seed.
SELECT 'tutor' AS table_name, COUNT(*) AS rows FROM tutor
UNION ALL SELECT 'veterinarian', COUNT(*) FROM veterinarian
UNION ALL SELECT 'animal', COUNT(*) FROM animal
UNION ALL SELECT 'tb_veterinarian_tutor_links', COUNT(*) FROM tb_veterinarian_tutor_links
UNION ALL SELECT 'tb_appointments', COUNT(*) FROM tb_appointments
UNION ALL SELECT 'tb_medical_records', COUNT(*) FROM tb_medical_records
UNION ALL SELECT 'tb_prescriptions', COUNT(*) FROM tb_prescriptions
UNION ALL SELECT 'tb_prescription_items', COUNT(*) FROM tb_prescription_items
UNION ALL SELECT 'tb_exams', COUNT(*) FROM tb_exams
UNION ALL SELECT 'tb_vaccinations', COUNT(*) FROM tb_vaccinations
UNION ALL SELECT 'tb_notifications', COUNT(*) FROM tb_notifications
UNION ALL SELECT 'tb_device_push_tokens', COUNT(*) FROM tb_device_push_tokens
UNION ALL SELECT 'tb_conversations', COUNT(*) FROM tb_conversations
UNION ALL SELECT 'tb_messages', COUNT(*) FROM tb_messages
ORDER BY table_name;

-- Relação Tutor -> Animal. Troque o CPF ou ID durante o CRUD do video.
SELECT a.id AS animal_id, a.name AS animal_name, a.species, a.race,
       a.weight, a.height, a.age, a.history,
       t.id AS tutor_id, t.name AS tutor_name, t.cpf AS tutor_cpf
FROM animal a
JOIN tutor t ON t.id = a.tutor_id
WHERE t.cpf = '12345678901'
ORDER BY a.id;

-- Dois registros significativos: uma dose passada e uma programada de Luna.
SELECT v.id AS vaccination_id, a.name AS animal_name, t.name AS tutor_name,
       vet.name AS veterinarian_name, vet.crmv_number, vet.crmv_state,
       v.vaccine_name, v.application_date, v.next_dose_date,
       v.batch_number, v.manufacturer,
       CASE
           WHEN v.next_dose_date IS NULL THEN 'SEM_PROXIMA_DOSE'
           WHEN v.next_dose_date < CURRENT_DATE THEN 'DATA_PASSADA'
           WHEN v.next_dose_date = CURRENT_DATE THEN 'PREVISTA_HOJE'
           ELSE 'PROGRAMADA'
       END AS dose_status
FROM tb_vaccinations v
JOIN animal a ON a.id = v.animal_id
JOIN tutor t ON t.id = a.tutor_id
JOIN veterinarian vet ON vet.id = v.veterinarian_id
WHERE v.id IN (1, 2)
ORDER BY v.id;

-- Confirme o vinculo exigido antes das operacoes veterinarias.
SELECT l.id AS link_id, vet.name AS veterinarian_name, vet.cpf AS veterinarian_cpf,
       t.name AS tutor_name, t.cpf AS tutor_cpf, l.status
FROM tb_veterinarian_tutor_links l
JOIN veterinarian vet ON vet.id = l.veterinarian_id
JOIN tutor t ON t.id = l.tutor_id
WHERE vet.cpf = '98765432100' AND t.cpf = '12345678901';

-- Depois de POST/PUT/DELETE de Animal, confira a linha pelo ID retornado.
-- SELECT * FROM animal WHERE id = <ANIMAL_ID>;

-- Depois de POST/PUT de Vaccination, confira a linha pelo ID retornado.
-- SELECT * FROM tb_vaccinations WHERE id = <VACCINATION_ID>;
