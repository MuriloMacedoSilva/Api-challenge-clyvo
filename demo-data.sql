-- CLYVO - Massa manual de dados para demonstracao
-- Compatibilidade validada com H2 2.4.240 e o schema criado pelo Hibernate.
-- Execute este arquivo somente depois de iniciar a API.
-- As datas sao relativas ao momento da execucao para manter o dashboard atual.

-- =====================================================
-- LIMPEZA
-- =====================================================

DELETE FROM tb_messages;
DELETE FROM tb_device_push_tokens;
DELETE FROM tb_notifications;
DELETE FROM tb_prescription_items;
DELETE FROM tb_exams;
DELETE FROM tb_prescriptions;
DELETE FROM tb_medical_records;
DELETE FROM tb_vaccinations;
DELETE FROM tb_appointments;
DELETE FROM tb_conversations;
DELETE FROM tb_veterinarian_tutor_links;
DELETE FROM animal;
DELETE FROM tutor;
DELETE FROM veterinarian;

-- =====================================================
-- TUTORES
-- =====================================================

INSERT INTO tutor (id, name, email, cpf, phone_number, password, role) VALUES
    (1, 'Mariana Oliveira', 'mariana.oliveira@demo.clyvo.com', '12345678901', '11987654321', '12345678', 'tutor'),
    (2, 'Carlos Mendes', 'carlos.mendes@demo.clyvo.com', '23456789012', '11976543210', '12345678', 'tutor'),
    (3, 'Fernanda Costa', 'fernanda.costa@demo.clyvo.com', '34567890123', '11965432109', '12345678', 'tutor'),
    (4, 'Rafael Almeida', 'rafael.almeida@demo.clyvo.com', '45678901234', '11954321098', '12345678', 'tutor'),
    (5, 'Juliana Martins', 'juliana.martins@demo.clyvo.com', '56789012345', '11943210987', '12345678', 'tutor'),
    (6, 'Lucas Pereira', 'lucas.pereira@demo.clyvo.com', '67890123456', '11932109876', '12345678', 'tutor'),
    (7, 'Camila Rocha', 'camila.rocha@demo.clyvo.com', '78901234567', '11921098765', '12345678', 'tutor'),
    (8, 'Bruno Santos', 'bruno.santos@demo.clyvo.com', '89012345678', '11910987654', '12345678', 'tutor'),
    (9, 'Patricia Lima', 'patricia.lima@demo.clyvo.com', '90123456789', '11909876543', '12345678', 'tutor'),
    (10, 'Diego Nascimento', 'diego.nascimento@demo.clyvo.com', '11223344556', '11988776655', '12345678', 'tutor');

-- =====================================================
-- VETERINARIOS
-- =====================================================

INSERT INTO veterinarian (
    id, name, email, cpf, phone_number, password, role,
    crmv_number, crmv_state, cnpj
) VALUES
    (1, 'Dr. Gabriel Martins', 'gabriel.martins@demo.clyvo.com', '98765432100', '11999887766', '12345678', 'veterinarian', '12345', 'SP', '12345678000190'),
    (2, 'Dra. Amanda Ribeiro', 'amanda.ribeiro@demo.clyvo.com', '87654321099', '11988776644', '12345678', 'veterinarian', '23456', 'SP', '23456789000101'),
    (3, 'Dr. Henrique Souza', 'henrique.souza@demo.clyvo.com', '76543210988', '11977665533', '12345678', 'veterinarian', '34567', 'RJ', '34567890000112'),
    (4, 'Dra. Beatriz Fernandes', 'beatriz.fernandes@demo.clyvo.com', '65432109877', '11966554422', '12345678', 'veterinarian', '45678', 'MG', '45678901000123');

-- =====================================================
-- ANIMAIS
-- Animal nao possui coluna de sexo no modelo atual.
-- =====================================================

INSERT INTO animal (id, tutor_id, name, weight, height, age, race, species, history) VALUES
    (1, 1, 'Luna', 29.4, 0.58, 5, 'Golden Retriever', 'Cachorro', 'Paciente acompanhada desde filhote. Historico de sensibilidade cutanea e episodios gastrointestinais leves.'),
    (2, 1, 'Mingau', 5.2, 0.28, 4, 'SRD', 'Gato', 'Felino domiciliado, castrado e com rotina de acompanhamento preventivo.'),
    (3, 1, 'Thor', 12.8, 0.34, 3, 'Bulldog Frances', 'Cachorro', 'Acompanhamento respiratorio preventivo e controle de peso.'),
    (4, 2, 'Mel', 8.7, 0.39, 6, 'Beagle', 'Cachorro', 'Paciente ativa, com consultas anuais de rotina.'),
    (5, 2, 'Bob', 18.3, 0.51, 8, 'Border Collie', 'Cachorro', 'Historico de acompanhamento pos-operatorio sem intercorrencias.'),
    (6, 3, 'Nina', 4.6, 0.26, 2, 'Siames', 'Gato', 'Acompanhamento preventivo e vacinacao em dia.'),
    (7, 3, 'Zeus', 32.1, 0.63, 7, 'Pastor Alemao', 'Cachorro', 'Paciente adulto em acompanhamento de rotina.'),
    (8, 3, 'Kiwi', 0.42, 0.19, 2, 'Calopsita', 'Ave', 'Ave domiciliada com acompanhamento preventivo.'),
    (9, 4, 'Amora', 7.4, 0.36, 4, 'Shih-tzu', 'Cachorro', 'Historico clinico sem alteracoes relevantes.'),
    (10, 4, 'Fred', 6.1, 0.31, 9, 'SRD', 'Gato', 'Paciente senior acompanhado periodicamente.'),
    (11, 5, 'Simba', 5.8, 0.30, 5, 'Maine Coon', 'Gato', 'Felino de temperamento tranquilo e manejo domiciliar.'),
    (12, 5, 'Nala', 23.5, 0.55, 4, 'Labrador', 'Cachorro', 'Paciente acompanhada em consultas preventivas.'),
    (13, 6, 'Pipoca', 2.3, 0.24, 2, 'Mini Lop', 'Coelho', 'Coelho domiciliado com dieta e manejo acompanhados.'),
    (14, 6, 'Max', 10.9, 0.41, 6, 'Cocker Spaniel', 'Cachorro', 'Historico de avaliacao dermatologica ocasional.'),
    (15, 7, 'Belinha', 6.8, 0.33, 5, 'Poodle', 'Cachorro', 'Paciente em acompanhamento com outro profissional da equipe.'),
    (16, 7, 'Chico', 4.9, 0.27, 3, 'SRD', 'Gato', 'Sem historico clinico relevante informado.'),
    (17, 8, 'Jade', 21.2, 0.52, 4, 'Husky Siberiano', 'Cachorro', 'Paciente ativa e sociavel.'),
    (18, 9, 'Toby', 9.5, 0.38, 7, 'Dachshund', 'Cachorro', 'Acompanhamento preventivo regular.'),
    (19, 10, 'Sol', 3.9, 0.25, 2, 'Persa', 'Gato', 'Felino jovem em acompanhamento preventivo.');

-- =====================================================
-- VINCULOS VETERINARIO-TUTOR
-- Gabriel: 6 ACCEPTED, 2 PENDING e 1 REJECTED.
-- Os pares com atividade clinica ou chat possuem ACCEPTED.
-- =====================================================

INSERT INTO tb_veterinarian_tutor_links (
    id, veterinarian_id, tutor_id, status, created_at, updated_at
) VALUES
    (1, 1, 1, 'ACCEPTED', DATEADD('MONTH', -10, CURRENT_TIMESTAMP), DATEADD('MONTH', -10, CURRENT_TIMESTAMP)),
    (2, 1, 2, 'ACCEPTED', DATEADD('MONTH', -9, CURRENT_TIMESTAMP), DATEADD('MONTH', -9, CURRENT_TIMESTAMP)),
    (3, 1, 3, 'ACCEPTED', DATEADD('MONTH', -8, CURRENT_TIMESTAMP), DATEADD('MONTH', -8, CURRENT_TIMESTAMP)),
    (4, 1, 4, 'ACCEPTED', DATEADD('MONTH', -7, CURRENT_TIMESTAMP), DATEADD('MONTH', -7, CURRENT_TIMESTAMP)),
    (5, 1, 5, 'ACCEPTED', DATEADD('MONTH', -6, CURRENT_TIMESTAMP), DATEADD('MONTH', -6, CURRENT_TIMESTAMP)),
    (6, 1, 6, 'ACCEPTED', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('MONTH', -5, CURRENT_TIMESTAMP)),
    (7, 1, 7, 'PENDING', DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL),
    (8, 1, 8, 'PENDING', DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),
    (9, 1, 9, 'REJECTED', DATEADD('DAY', -12, CURRENT_TIMESTAMP), DATEADD('DAY', -10, CURRENT_TIMESTAMP)),
    (10, 2, 1, 'ACCEPTED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('MONTH', -4, CURRENT_TIMESTAMP)),
    (11, 2, 8, 'ACCEPTED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('MONTH', -3, CURRENT_TIMESTAMP)),
    (12, 2, 9, 'ACCEPTED', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('MONTH', -5, CURRENT_TIMESTAMP)),
    (13, 3, 7, 'ACCEPTED', DATEADD('MONTH', -6, CURRENT_TIMESTAMP), DATEADD('MONTH', -6, CURRENT_TIMESTAMP)),
    (14, 3, 10, 'ACCEPTED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('MONTH', -2, CURRENT_TIMESTAMP)),
    (15, 4, 3, 'ACCEPTED', DATEADD('MONTH', -7, CURRENT_TIMESTAMP), DATEADD('MONTH', -7, CURRENT_TIMESTAMP)),
    (16, 4, 9, 'ACCEPTED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('MONTH', -4, CURRENT_TIMESTAMP));

-- =====================================================
-- CONSULTAS
-- Gabriel possui 19: 4 PENDING, 4 CONFIRMED, 10 COMPLETED e 1 CANCELLED.
-- Quatro consultas nao canceladas estao agendadas para hoje.
-- Os concluidos cobrem os seis meses usados pelo dashboard.
-- =====================================================

INSERT INTO tb_appointments (
    id, animal_id, tutor_id, veterinarian_id, scheduled_at,
    reason, status, created_at, updated_at
) VALUES
    (1, 1, 1, 1, DATEADD('HOUR', 9, CAST(CURRENT_DATE AS TIMESTAMP)),
        'Retorno por episodios de vomito e baixa ingestao alimentar', 'COMPLETED', DATEADD('DAY', -5, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
    (2, 2, 1, 1, DATEADD('HOUR', 11, CAST(CURRENT_DATE AS TIMESTAMP)),
        'Avaliacao preventiva anual', 'CONFIRMED', DATEADD('DAY', -8, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
    (3, 4, 2, 1, DATEADD('HOUR', 14, CAST(CURRENT_DATE AS TIMESTAMP)),
        'Inapetencia observada nas ultimas horas', 'PENDING', DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),
    (4, 3, 1, 1, DATEADD('MINUTE', 30, DATEADD('HOUR', 17, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Acompanhamento respiratorio preventivo', 'CONFIRMED', DATEADD('DAY', -10, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
    (5, 1, 1, 1, DATEADD('HOUR', 9, DATEADD('DAY', 1, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Retorno para avaliacao da evolucao clinica', 'PENDING', CURRENT_TIMESTAMP, NULL),
    (6, 2, 1, 1, DATEADD('HOUR', 10, DATEADD('DAY', 2, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Consulta de acompanhamento preventivo', 'CONFIRMED', DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
    (7, 7, 3, 1, DATEADD('HOUR', 15, DATEADD('DAY', 4, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Avaliacao de rotina e controle de peso', 'PENDING', DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL),
    (8, 9, 4, 1, DATEADD('MINUTE', 30, DATEADD('HOUR', 9, DATEADD('DAY', 7, CAST(CURRENT_DATE AS TIMESTAMP)))),
        'Check-up semestral', 'CONFIRMED', DATEADD('DAY', -6, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
    (9, 11, 5, 1, DATEADD('HOUR', 16, DATEADD('DAY', 15, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Avaliacao preventiva felina', 'PENDING', CURRENT_TIMESTAMP, NULL),

    (10, 1, 1, 1, DATEADD('HOUR', 10, DATEADD('DAY', 6, DATE_TRUNC('MONTH', DATEADD('MONTH', -1, CURRENT_TIMESTAMP)))),
        'Coceira frequente e vermelhidao na pele', 'COMPLETED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('MONTH', -1, CURRENT_TIMESTAMP)),
    (11, 4, 2, 1, DATEADD('HOUR', 14, DATEADD('DAY', 12, DATE_TRUNC('MONTH', DATEADD('MONTH', -1, CURRENT_TIMESTAMP)))),
        'Avaliacao clinica de rotina', 'COMPLETED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('MONTH', -1, CURRENT_TIMESTAMP)),
    (12, 1, 1, 1, DATEADD('HOUR', 9, DATEADD('DAY', 8, DATE_TRUNC('MONTH', DATEADD('MONTH', -2, CURRENT_TIMESTAMP)))),
        'Desconforto auricular e coceira', 'COMPLETED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('MONTH', -2, CURRENT_TIMESTAMP)),
    (13, 2, 1, 1, DATEADD('HOUR', 15, DATEADD('DAY', 17, DATE_TRUNC('MONTH', DATEADD('MONTH', -2, CURRENT_TIMESTAMP)))),
        'Alteracao na frequencia urinaria', 'COMPLETED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('MONTH', -2, CURRENT_TIMESTAMP)),
    (14, 5, 2, 1, DATEADD('HOUR', 11, DATEADD('DAY', 5, DATE_TRUNC('MONTH', DATEADD('MONTH', -3, CURRENT_TIMESTAMP)))),
        'Acompanhamento pos-operatorio', 'COMPLETED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('MONTH', -3, CURRENT_TIMESTAMP)),
    (15, 6, 3, 1, DATEADD('HOUR', 16, DATEADD('DAY', 18, DATE_TRUNC('MONTH', DATEADD('MONTH', -3, CURRENT_TIMESTAMP)))),
        'Check-up preventivo felino', 'COMPLETED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('MONTH', -3, CURRENT_TIMESTAMP)),
    (16, 1, 1, 1, DATEADD('HOUR', 10, DATEADD('DAY', 9, DATE_TRUNC('MONTH', DATEADD('MONTH', -4, CURRENT_TIMESTAMP)))),
        'Check-up anual e atualizacao do acompanhamento', 'COMPLETED', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('MONTH', -4, CURRENT_TIMESTAMP)),
    (17, 3, 1, 1, DATEADD('HOUR', 13, DATEADD('DAY', 20, DATE_TRUNC('MONTH', DATEADD('MONTH', -5, CURRENT_TIMESTAMP)))),
        'Avaliacao de tosse ocasional', 'COMPLETED', DATEADD('MONTH', -6, CURRENT_TIMESTAMP), DATEADD('MONTH', -5, CURRENT_TIMESTAMP)),
    (18, 14, 6, 1, DATEADD('HOUR', 9, DATEADD('DAY', 22, DATE_TRUNC('MONTH', DATEADD('MONTH', -4, CURRENT_TIMESTAMP)))),
        'Avaliacao dermatologica', 'COMPLETED', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('MONTH', -4, CURRENT_TIMESTAMP)),
    (19, 12, 5, 1, DATEADD('DAY', -3, CURRENT_TIMESTAMP),
        'Consulta preventiva cancelada pelo Tutor', 'CANCELLED', DATEADD('DAY', -12, CURRENT_TIMESTAMP), DATEADD('DAY', -5, CURRENT_TIMESTAMP)),

    (20, 1, 1, 2, DATEADD('MONTH', -2, CURRENT_TIMESTAMP),
        'Segunda avaliacao dermatologica', 'COMPLETED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('MONTH', -2, CURRENT_TIMESTAMP)),
    (21, 18, 9, 2, DATEADD('HOUR', 11, DATEADD('DAY', 3, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Consulta preventiva', 'PENDING', CURRENT_TIMESTAMP, NULL),
    (22, 15, 7, 3, DATEADD('MONTH', -1, CURRENT_TIMESTAMP),
        'Avaliacao de rotina', 'COMPLETED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('MONTH', -1, CURRENT_TIMESTAMP)),
    (23, 19, 10, 3, DATEADD('HOUR', 13, DATEADD('DAY', 5, CAST(CURRENT_DATE AS TIMESTAMP))),
        'Retorno preventivo felino', 'CONFIRMED', DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
    (24, 8, 3, 4, DATEADD('MONTH', -3, CURRENT_TIMESTAMP),
        'Avaliacao preventiva da ave', 'COMPLETED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('MONTH', -3, CURRENT_TIMESTAMP)),
    (25, 18, 9, 4, DATEADD('DAY', -6, CURRENT_TIMESTAMP),
        'Consulta cancelada por indisponibilidade', 'CANCELLED', DATEADD('DAY', -15, CURRENT_TIMESTAMP), DATEADD('DAY', -8, CURRENT_TIMESTAMP));

-- =====================================================
-- PRONTUARIOS
-- Todo Appointment COMPLETED possui exatamente um MedicalRecord coerente.
-- Luna concentra cinco registros, quatro de Gabriel e um de Amanda.
-- =====================================================

INSERT INTO tb_medical_records (
    id, animal_id, veterinarian_id, appointment_id, diagnosis, description,
    weight, temperature, observations, created_at, updated_at
) VALUES
    (1, 1, 1, 1, 'Quadro gastrointestinal leve',
        'Paciente apresentou episodios pontuais de vomito e menor ingestao alimentar. Avaliacao geral sem sinais de urgencia.',
        29.4, 38.6, 'Manter observacao da ingestao de agua e alimento durante as proximas horas.', CURRENT_TIMESTAMP, NULL),
    (2, 1, 1, 10, 'Dermatite alergica',
        'Lesoes superficiais e prurido compativeis com quadro dermatologico leve.',
        29.1, 38.4, 'Retorno recomendado para acompanhar a resposta ao manejo prescrito.', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), NULL),
    (3, 4, 1, 11, 'Avaliacao clinica de rotina',
        'Exame clinico preventivo sem alteracoes relevantes observadas.',
        8.7, 38.5, 'Manter rotina de atividade e acompanhamento anual.', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), NULL),
    (4, 1, 1, 12, 'Otite externa leve',
        'Sensibilidade auricular e secrecao discreta observadas durante a avaliacao.',
        28.9, 38.7, 'Evitar umidade no conduto auditivo durante o periodo de acompanhamento.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (5, 2, 1, 13, 'Quadro urinario em investigacao',
        'Tutor relatou mudanca na frequencia urinaria. Solicitados exames complementares para acompanhamento.',
        5.2, 38.3, 'Observar ingestao de agua e uso da caixa de areia.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (6, 5, 1, 14, 'Acompanhamento pos-operatorio',
        'Evolucao satisfatoria no retorno, sem intercorrencias relatadas pelo Tutor.',
        18.1, 38.5, 'Manter restricao de atividade pelo periodo orientado.', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), NULL),
    (7, 6, 1, 15, 'Avaliacao clinica de rotina',
        'Exame preventivo sem alteracoes clinicas relevantes.',
        4.6, 38.2, 'Paciente liberada para continuidade da rotina habitual.', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), NULL),
    (8, 1, 1, 16, 'Avaliacao clinica de rotina',
        'Check-up anual com condicao corporal e parametros gerais adequados.',
        28.5, 38.5, 'Reforcada a importancia do acompanhamento preventivo.', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), NULL),
    (9, 3, 1, 17, 'Irritacao respiratoria leve',
        'Tosse ocasional relatada, sem alteracoes importantes na avaliacao inicial.',
        12.6, 38.4, 'Monitorar frequencia dos episodios e retornar se houver piora.', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), NULL),
    (10, 14, 1, 18, 'Dermatite localizada',
        'Area pequena de irritacao cutanea identificada durante o exame.',
        10.9, 38.6, 'Acompanhar aspecto da pele ate o retorno.', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), NULL),
    (11, 1, 2, 20, 'Reavaliacao dermatologica',
        'Reavaliacao do quadro cutaneo com melhora relatada e evolucao favoravel.',
        29.0, 38.4, 'Manter cuidados de higiene e observacao.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (12, 15, 3, 22, 'Avaliacao clinica de rotina',
        'Consulta preventiva com parametros gerais dentro do esperado.',
        6.8, 38.5, NULL, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), NULL),
    (13, 8, 4, 24, 'Avaliacao preventiva',
        'Avaliacao geral de plumagem, comportamento e condicao corporal.',
        0.42, 41.0, 'Orientacoes gerais de manejo fornecidas ao Tutor.', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), NULL);

-- =====================================================
-- PRESCRICOES
-- Uma prescricao por prontuario, sempre ligada a Appointment COMPLETED.
-- =====================================================

INSERT INTO tb_prescriptions (id, medical_record_id, instructions, created_at, updated_at) VALUES
    (1, 1, 'Administrar conforme os horarios orientados e manter alimentacao leve durante o acompanhamento.', CURRENT_TIMESTAMP, NULL),
    (2, 2, 'Realizar o cuidado dermatologico pelo periodo indicado e retornar em caso de piora.', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), NULL),
    (3, 4, 'Aplicar os itens conforme orientacao e manter o ouvido seco.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (4, 5, 'Administrar somente conforme orientacao demonstrativa e aguardar os exames solicitados.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (5, 6, 'Seguir os cuidados pos-operatorios e respeitar o periodo de repouso.', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), NULL),
    (6, 9, 'Manter acompanhamento dos sintomas durante o periodo orientado.', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), NULL),
    (7, 11, 'Continuar cuidados topicos ate o retorno programado.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL),
    (8, 12, 'Uso demonstrativo conforme orientacoes registradas.', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), NULL);

-- =====================================================
-- ITENS DAS PRESCRICOES
-- item_order e zero-based e continuo em cada prescricao.
-- Conteudo exclusivamente ficticio para demonstracao da interface.
-- =====================================================

INSERT INTO tb_prescription_items (
    id, prescription_id, medication_name, dosage, frequency,
    duration, route, instructions, item_order
) VALUES
    (1, 1, 'Medicamento gastrointestinal demonstrativo', '1 comprimido', 'A cada 12 horas', '5 dias', 'Oral', 'Oferecer apos pequena porcao de alimento.', 0),
    (2, 1, 'Probiotico veterinario demonstrativo', '1 sache', 'Uma vez ao dia', '7 dias', 'Oral', 'Misturar ao alimento.', 1),
    (3, 1, 'Solucao de reidratacao demonstrativa', 'Conforme orientacao', 'Fracionado ao longo do dia', '3 dias', 'Oral', 'Manter agua fresca disponivel.', 2),
    (4, 2, 'Shampoo dermatologico demonstrativo', 'Uso topico', 'Duas vezes por semana', '3 semanas', 'Topica', 'Evitar contato com olhos e mucosas.', 0),
    (5, 2, 'Antialergico demonstrativo', '1 comprimido', 'Uma vez ao dia', '7 dias', 'Oral', NULL, 1),
    (6, 3, 'Solucao otologica demonstrativa', '4 gotas', 'A cada 12 horas', '7 dias', 'Otologica', 'Higienizar suavemente antes da aplicacao.', 0),
    (7, 3, 'Higienizador auricular', 'Quantidade suficiente', 'Uma vez ao dia', '7 dias', 'Otologica', NULL, 1),
    (8, 4, 'Suplemento urinario demonstrativo', '1 medida', 'Uma vez ao dia', '10 dias', 'Oral', 'Misturar ao alimento umido.', 0),
    (9, 4, 'Alimento umido demonstrativo', 'Porcao orientada', 'Duas vezes ao dia', '14 dias', 'Oral', 'Estimular ingestao hidrica.', 1),
    (10, 5, 'Analgesico demonstrativo', 'Dose orientada', 'A cada 12 horas', '4 dias', 'Oral', NULL, 0),
    (11, 5, 'Antisseptico topico', 'Aplicacao local', 'Uma vez ao dia', '5 dias', 'Topica', 'Aplicar apenas na area indicada.', 1),
    (12, 6, 'Xarope demonstrativo', 'Dose orientada', 'A cada 12 horas', '5 dias', 'Oral', 'Observar a frequencia da tosse.', 0),
    (13, 7, 'Loção dermatologica demonstrativa', 'Camada fina', 'Uma vez ao dia', '10 dias', 'Topica', NULL, 0),
    (14, 7, 'Shampoo suave demonstrativo', 'Uso topico', 'Uma vez por semana', '4 semanas', 'Topica', NULL, 1),
    (15, 8, 'Suplemento vitaminico demonstrativo', '1 medida', 'Uma vez ao dia', '15 dias', 'Oral', 'Uso exclusivamente demonstrativo.', 0),
    (16, 8, 'Higienizador demonstrativo', 'Aplicacao local', 'Em dias alternados', '10 dias', 'Topica', NULL, 1);

-- =====================================================
-- EXAMES
-- Totais: 6 REQUESTED, 6 COMPLETED e 3 CANCELLED.
-- Gabriel e responsavel por 5 exames REQUESTED no dashboard.
-- =====================================================

INSERT INTO tb_exams (
    id, medical_record_id, exam_name, exam_type, status,
    request_date, result_date, result, observations, created_at, updated_at
) VALUES
    (1, 1, 'Ultrassonografia abdominal', 'Imagem', 'REQUESTED', CURRENT_TIMESTAMP, NULL, NULL, 'Agendar para acompanhamento do quadro gastrointestinal.', CURRENT_TIMESTAMP, NULL),
    (2, 1, 'Hemograma completo', 'Laboratorial', 'COMPLETED', DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'Resultado dentro dos parametros esperados, sem alteracoes relevantes para esta demonstracao.', NULL, DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
    (3, 2, 'Raspado cutaneo', 'Dermatologico', 'COMPLETED', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 2, DATEADD('MONTH', -1, CURRENT_TIMESTAMP)), 'Amostra analisada, sem achados adicionais relevantes no registro demonstrativo.', NULL, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 2, DATEADD('MONTH', -1, CURRENT_TIMESTAMP))),
    (4, 4, 'Cultura de secrecao auricular', 'Laboratorial', 'REQUESTED', DATEADD('DAY', -4, CURRENT_TIMESTAMP), NULL, NULL, 'Coleta orientada para o retorno.', DATEADD('DAY', -4, CURRENT_TIMESTAMP), NULL),
    (5, 4, 'Painel alergico', 'Laboratorial', 'CANCELLED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), NULL, NULL, 'Cancelado apos reavaliacao do plano de acompanhamento.', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -2, CURRENT_TIMESTAMP))),
    (6, 5, 'Exame de urina', 'Laboratorial', 'REQUESTED', DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL, NULL, 'Tutor recebeu orientacoes para coleta.', DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL),
    (7, 5, 'Ultrassonografia abdominal', 'Imagem', 'COMPLETED', DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('DAY', 3, DATEADD('MONTH', -2, CURRENT_TIMESTAMP)), 'Exame concluido e disponibilizado para acompanhamento clinico.', NULL, DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('DAY', 3, DATEADD('MONTH', -2, CURRENT_TIMESTAMP))),
    (8, 6, 'Raio-X de controle', 'Imagem', 'REQUESTED', DATEADD('DAY', -6, CURRENT_TIMESTAMP), NULL, NULL, 'Controle do acompanhamento pos-operatorio.', DATEADD('DAY', -6, CURRENT_TIMESTAMP), NULL),
    (9, 7, 'Hemograma completo', 'Laboratorial', 'COMPLETED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -3, CURRENT_TIMESTAMP)), 'Resultado demonstrativo sem alteracoes relevantes.', NULL, DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -3, CURRENT_TIMESTAMP))),
    (10, 8, 'Exame de fezes', 'Laboratorial', 'CANCELLED', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), NULL, NULL, 'Solicitacao cancelada por decisao de acompanhamento.', DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -4, CURRENT_TIMESTAMP))),
    (11, 9, 'Raio-X toracico', 'Imagem', 'COMPLETED', DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('DAY', 2, DATEADD('MONTH', -5, CURRENT_TIMESTAMP)), 'Imagem avaliada sem alteracoes relevantes no registro demonstrativo.', NULL, DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('DAY', 2, DATEADD('MONTH', -5, CURRENT_TIMESTAMP))),
    (12, 10, 'Perfil bioquimico', 'Laboratorial', 'REQUESTED', DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, NULL, 'Coleta pendente.', DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),
    (13, 11, 'Teste dermatologico de controle', 'Dermatologico', 'REQUESTED', DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL, NULL, 'Exame sob responsabilidade da Dra. Amanda.', DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL),
    (14, 12, 'Hemograma preventivo', 'Laboratorial', 'COMPLETED', DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -1, CURRENT_TIMESTAMP)), 'Resultado dentro dos parametros esperados.', NULL, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -1, CURRENT_TIMESTAMP))),
    (15, 13, 'Avaliacao complementar de imagem', 'Imagem', 'CANCELLED', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), NULL, NULL, 'Exame cancelado apos avaliacao clinica.', DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('DAY', 1, DATEADD('MONTH', -3, CURRENT_TIMESTAMP)));

-- =====================================================
-- VACINACOES
-- Inclui proxima dose futura, prevista para hoje, ausente e no passado.
-- Datas vencidas representam apenas nextDoseDate cadastrada, sem inferencia de protocolo.
-- =====================================================

INSERT INTO tb_vaccinations (
    id, animal_id, veterinarian_id, vaccine_name, application_date,
    next_dose_date, batch_number, manufacturer, observations, created_at, updated_at
) VALUES
    (1, 1, 1, 'V10', DATEADD('DAY', -330, CURRENT_DATE), DATEADD('DAY', -30, CURRENT_DATE), 'V10-L001', 'VetBio', 'Proxima dose cadastrada em data passada para demonstracao visual.', DATEADD('DAY', -330, CURRENT_TIMESTAMP), NULL),
    (2, 1, 1, 'Antirrabica', DATEADD('DAY', -60, CURRENT_DATE), DATEADD('DAY', 305, CURRENT_DATE), 'AR-L102', 'Saude Animal', NULL, DATEADD('DAY', -60, CURRENT_TIMESTAMP), NULL),
    (3, 1, 1, 'Gripe Canina', DATEADD('DAY', -20, CURRENT_DATE), NULL, 'GC-L203', 'VetBio', 'Aplicacao recente sem proxima dose informada.', DATEADD('DAY', -20, CURRENT_TIMESTAMP), NULL),
    (4, 2, 1, 'Triplice Felina', DATEADD('DAY', -200, CURRENT_DATE), DATEADD('DAY', 165, CURRENT_DATE), 'TF-L304', 'FelixLab', NULL, DATEADD('DAY', -200, CURRENT_TIMESTAMP), NULL),
    (5, 2, 1, 'Antirrabica', DATEADD('DAY', -400, CURRENT_DATE), DATEADD('DAY', -35, CURRENT_DATE), 'AR-L405', 'Saude Animal', 'Proxima dose cadastrada em data passada.', DATEADD('DAY', -400, CURRENT_TIMESTAMP), NULL),
    (6, 3, 1, 'V10', DATEADD('DAY', -90, CURRENT_DATE), DATEADD('DAY', 275, CURRENT_DATE), 'V10-L506', 'VetBio', NULL, DATEADD('DAY', -90, CURRENT_TIMESTAMP), NULL),
    (7, 3, 1, 'Gripe Canina', DATEADD('DAY', -370, CURRENT_DATE), DATEADD('DAY', -5, CURRENT_DATE), 'GC-L607', 'VetBio', 'Registro com proxima dose potencialmente atrasada.', DATEADD('DAY', -370, CURRENT_TIMESTAMP), NULL),
    (8, 4, 1, 'V8', DATEADD('DAY', -45, CURRENT_DATE), DATEADD('DAY', 320, CURRENT_DATE), 'V8-L708', 'PetCare', NULL, DATEADD('DAY', -45, CURRENT_TIMESTAMP), NULL),
    (9, 4, 1, 'Antirrabica', DATEADD('DAY', -300, CURRENT_DATE), DATEADD('DAY', 65, CURRENT_DATE), 'AR-L809', 'Saude Animal', NULL, DATEADD('DAY', -300, CURRENT_TIMESTAMP), NULL),
    (10, 5, 1, 'V10', DATEADD('DAY', -180, CURRENT_DATE), DATEADD('DAY', 185, CURRENT_DATE), 'V10-L910', 'VetBio', NULL, DATEADD('DAY', -180, CURRENT_TIMESTAMP), NULL),
    (11, 6, 1, 'Triplice Felina', DATEADD('DAY', -25, CURRENT_DATE), DATEADD('DAY', 340, CURRENT_DATE), 'TF-L111', 'FelixLab', NULL, DATEADD('DAY', -25, CURRENT_TIMESTAMP), NULL),
    (12, 7, 1, 'Antirrabica', DATEADD('DAY', -360, CURRENT_DATE), CURRENT_DATE, 'AR-L212', 'Saude Animal', 'Proxima dose prevista para hoje.', DATEADD('DAY', -360, CURRENT_TIMESTAMP), NULL),
    (13, 8, 1, 'Vacina polivalente aviaria', DATEADD('DAY', -120, CURRENT_DATE), DATEADD('DAY', 245, CURRENT_DATE), 'AV-L313', 'Aves Brasil', NULL, DATEADD('DAY', -120, CURRENT_TIMESTAMP), NULL),
    (14, 9, 1, 'V10', DATEADD('DAY', -50, CURRENT_DATE), DATEADD('DAY', 315, CURRENT_DATE), 'V10-L414', 'VetBio', NULL, DATEADD('DAY', -50, CURRENT_TIMESTAMP), NULL),
    (15, 10, 1, 'Antirrabica', DATEADD('DAY', -390, CURRENT_DATE), DATEADD('DAY', -25, CURRENT_DATE), 'AR-L515', 'Saude Animal', 'Proxima dose cadastrada em data passada.', DATEADD('DAY', -390, CURRENT_TIMESTAMP), NULL),
    (16, 11, 1, 'Triplice Felina', DATEADD('DAY', -75, CURRENT_DATE), DATEADD('DAY', 290, CURRENT_DATE), 'TF-L616', 'FelixLab', NULL, DATEADD('DAY', -75, CURRENT_TIMESTAMP), NULL),
    (17, 12, 1, 'Antirrabica', DATEADD('DAY', -250, CURRENT_DATE), DATEADD('DAY', 115, CURRENT_DATE), 'AR-L717', 'Saude Animal', NULL, DATEADD('DAY', -250, CURRENT_TIMESTAMP), NULL),
    (18, 13, 1, 'Vacina contra mixomatose', DATEADD('DAY', -100, CURRENT_DATE), DATEADD('DAY', 265, CURRENT_DATE), 'MX-L818', 'ExoticCare', NULL, DATEADD('DAY', -100, CURRENT_TIMESTAMP), NULL),
    (19, 14, 1, 'V10', DATEADD('DAY', -15, CURRENT_DATE), NULL, 'V10-L919', 'VetBio', 'Sem proxima dose informada.', DATEADD('DAY', -15, CURRENT_TIMESTAMP), NULL),
    (20, 18, 2, 'Antirrabica', DATEADD('DAY', -40, CURRENT_DATE), DATEADD('DAY', 325, CURRENT_DATE), 'AR-L020', 'Saude Animal', NULL, DATEADD('DAY', -40, CURRENT_TIMESTAMP), NULL),
    (21, 19, 3, 'Triplice Felina', DATEADD('DAY', -70, CURRENT_DATE), DATEADD('DAY', 295, CURRENT_DATE), 'TF-L121', 'FelixLab', NULL, DATEADD('DAY', -70, CURRENT_TIMESTAMP), NULL);

-- =====================================================
-- NOTIFICACOES
-- Destinatario Tutor ou Veterinario e preenchido de forma exclusiva.
-- =====================================================

INSERT INTO tb_notifications (
    id, message, type, is_read, created_at, tutor_id, veterinarian_id, link_id
) VALUES
    (1, 'Seu vinculo com Dr. Gabriel Martins foi aceito.', 'LINK_REQUEST_ACCEPTED', TRUE, DATEADD('MONTH', -10, CURRENT_TIMESTAMP), 1, NULL, 1),
    (2, 'Consulta de Mingau confirmada para hoje.', 'APPOINTMENT_CONFIRMED', FALSE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (3, 'Prontuario de Luna foi registrado.', 'MEDICAL_RECORD_CREATED', FALSE, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (4, 'Nova prescricao disponivel para Luna.', 'PRESCRIPTION_CREATED', FALSE, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (5, 'Ultrassonografia abdominal solicitada para Luna.', 'EXAM_REQUESTED', TRUE, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (6, 'Resultado do hemograma de Luna disponivel.', 'EXAM_RESULT_AVAILABLE', TRUE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (7, 'Vacina Gripe Canina registrada para Luna.', 'VACCINATION_REGISTERED', TRUE, DATEADD('DAY', -20, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (8, 'Consulta de Thor confirmada.', 'APPOINTMENT_CONFIRMED', TRUE, DATEADD('DAY', -3, CURRENT_TIMESTAMP), 1, NULL, NULL),
    (9, 'Consulta preventiva cancelada.', 'APPOINTMENT_CANCELLED', TRUE, DATEADD('DAY', -5, CURRENT_TIMESTAMP), 5, NULL, NULL),
    (10, 'Novo exame solicitado para Mingau.', 'EXAM_REQUESTED', FALSE, DATEADD('DAY', -3, CURRENT_TIMESTAMP), 1, NULL, NULL),

    (11, 'Mariana Oliveira solicitou uma consulta para Luna.', 'APPOINTMENT_REQUESTED', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, 1, NULL),
    (12, 'Carlos Mendes solicitou uma consulta para Mel.', 'APPOINTMENT_REQUESTED', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, 1, NULL),
    (13, 'Camila Rocha recebeu sua solicitacao de vinculo.', 'LINK_REQUEST_SENT', TRUE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL, 1, 7),
    (14, 'Bruno Santos recebeu sua solicitacao de vinculo.', 'LINK_REQUEST_SENT', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, 1, 8),
    (15, 'Patricia Lima rejeitou sua solicitacao de vinculo.', 'LINK_REQUEST_REJECTED', TRUE, DATEADD('DAY', -10, CURRENT_TIMESTAMP), NULL, 1, 9),
    (16, 'Consulta de Mingau foi confirmada.', 'APPOINTMENT_CONFIRMED', TRUE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL, 1, NULL),
    (17, 'Novo resultado de exame registrado para Luna.', 'EXAM_RESULT_AVAILABLE', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, 1, NULL),

    (18, 'Dr. Gabriel Martins enviou uma solicitacao de vinculo.', 'LINK_REQUEST_RECEIVED', FALSE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), 7, NULL, 7),
    (19, 'Dr. Gabriel Martins enviou uma solicitacao de vinculo.', 'LINK_REQUEST_RECEIVED', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), 8, NULL, 8),
    (20, 'Consulta de Toby solicitada para Dra. Amanda Ribeiro.', 'APPOINTMENT_REQUESTED', FALSE, CURRENT_TIMESTAMP, NULL, 2, NULL),
    (21, 'Prontuario preventivo de Belinha foi registrado.', 'MEDICAL_RECORD_CREATED', TRUE, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), 7, NULL, NULL);

-- =====================================================
-- CONVERSAS
-- Gabriel possui seis conversas; a conversa com Lucas nao tem mensagens.
-- =====================================================

INSERT INTO tb_conversations (id, tutor_id, veterinarian_id, created_at, updated_at) VALUES
    (1, 1, 1, DATEADD('MONTH', -5, CURRENT_TIMESTAMP), DATEADD('MINUTE', -10, CURRENT_TIMESTAMP)),
    (2, 2, 1, DATEADD('MONTH', -4, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
    (3, 3, 1, DATEADD('MONTH', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
    (4, 4, 1, DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
    (5, 5, 1, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
    (6, 6, 1, DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('DAY', -4, CURRENT_TIMESTAMP)),
    (7, 1, 2, DATEADD('MONTH', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -5, CURRENT_TIMESTAMP)),
    (8, 7, 3, DATEADD('MONTH', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP));

-- =====================================================
-- MENSAGENS
-- Mensagens antigas estao lidas; mensagens recentes da contraparte podem estar nao lidas.
-- =====================================================

INSERT INTO tb_messages (
    id, conversation_id, sender_type, content, is_read, sent_at, read_at
) VALUES
    (1, 1, 'TUTOR', 'Ola, doutor. A Luna apresentou um pouco de enjoo ontem.', TRUE, DATEADD('HOUR', -30, CURRENT_TIMESTAMP), DATEADD('HOUR', -29, CURRENT_TIMESTAMP)),
    (2, 1, 'VETERINARIAN', 'Ola, Mariana. Ela conseguiu beber agua normalmente?', TRUE, DATEADD('HOUR', -29, CURRENT_TIMESTAMP), DATEADD('HOUR', -28, CURRENT_TIMESTAMP)),
    (3, 1, 'TUTOR', 'Sim, bebeu agua e descansou durante a noite.', TRUE, DATEADD('HOUR', -27, CURRENT_TIMESTAMP), DATEADD('HOUR', -26, CURRENT_TIMESTAMP)),
    (4, 1, 'VETERINARIAN', 'Otimo. Observe tambem se ela volta a se alimentar pela manha.', TRUE, DATEADD('HOUR', -25, CURRENT_TIMESTAMP), DATEADD('HOUR', -24, CURRENT_TIMESTAMP)),
    (5, 1, 'TUTOR', 'Hoje ela aceitou uma pequena porcao da racao.', TRUE, DATEADD('HOUR', -20, CURRENT_TIMESTAMP), DATEADD('HOUR', -19, CURRENT_TIMESTAMP)),
    (6, 1, 'VETERINARIAN', 'Perfeito. Isso e um bom sinal. Continue oferecendo pequenas porcoes.', TRUE, DATEADD('HOUR', -18, CURRENT_TIMESTAMP), DATEADD('HOUR', -17, CURRENT_TIMESTAMP)),
    (7, 1, 'TUTOR', 'Combinado. Vou continuar observando.', TRUE, DATEADD('HOUR', -12, CURRENT_TIMESTAMP), DATEADD('HOUR', -11, CURRENT_TIMESTAMP)),
    (8, 1, 'VETERINARIAN', 'Se houver qualquer mudanca, pode me chamar por aqui.', TRUE, DATEADD('HOUR', -10, CURRENT_TIMESTAMP), DATEADD('HOUR', -9, CURRENT_TIMESTAMP)),
    (9, 1, 'TUTOR', 'Ela esta bem melhor agora e voltou a brincar.', FALSE, DATEADD('MINUTE', -25, CURRENT_TIMESTAMP), NULL),
    (10, 1, 'TUTOR', 'Obrigada pelo acompanhamento, doutor!', FALSE, DATEADD('MINUTE', -10, CURRENT_TIMESTAMP), NULL),

    (11, 2, 'TUTOR', 'Boa tarde, doutor. A Mel tem consulta hoje?', TRUE, DATEADD('HOUR', -8, CURRENT_TIMESTAMP), DATEADD('HOUR', -7, CURRENT_TIMESTAMP)),
    (12, 2, 'VETERINARIAN', 'Boa tarde, Carlos. A solicitacao aparece para as 14 horas.', TRUE, DATEADD('HOUR', -7, CURRENT_TIMESTAMP), DATEADD('HOUR', -6, CURRENT_TIMESTAMP)),
    (13, 2, 'TUTOR', 'Certo. Separei os registros anteriores dela.', TRUE, DATEADD('HOUR', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
    (14, 2, 'VETERINARIAN', 'Perfeito, eles podem ajudar durante a avaliacao.', TRUE, DATEADD('HOUR', -4, CURRENT_TIMESTAMP), DATEADD('HOUR', -3, CURRENT_TIMESTAMP)),
    (15, 2, 'VETERINARIAN', 'Se puder, chegue alguns minutos antes.', TRUE, DATEADD('HOUR', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', -2, CURRENT_TIMESTAMP)),
    (16, 2, 'TUTOR', 'Pode deixar, estaremos ai.', FALSE, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), NULL),

    (17, 3, 'TUTOR', 'Doutor, gostaria de agendar o retorno do Zeus.', TRUE, DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -3, CURRENT_TIMESTAMP))),
    (18, 3, 'VETERINARIAN', 'Claro. Tenho disponibilidade para a proxima semana.', TRUE, DATEADD('HOUR', 3, DATEADD('DAY', -3, CURRENT_TIMESTAMP)), DATEADD('HOUR', 4, DATEADD('DAY', -3, CURRENT_TIMESTAMP))),
    (19, 3, 'TUTOR', 'A tarde funciona melhor para nos.', TRUE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -2, CURRENT_TIMESTAMP))),
    (20, 3, 'TUTOR', 'Enviei a solicitacao pelo aplicativo.', TRUE, DATEADD('HOUR', -30, CURRENT_TIMESTAMP), DATEADD('HOUR', -29, CURRENT_TIMESTAMP)),
    (21, 3, 'VETERINARIAN', 'Recebi. Vou revisar a agenda e confirmar em breve.', FALSE, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),

    (22, 4, 'TUTOR', 'A Amora esta bem desde a ultima consulta.', TRUE, DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', 2, DATEADD('DAY', -5, CURRENT_TIMESTAMP))),
    (23, 4, 'VETERINARIAN', 'Que bom, Rafael. Ela manteve a rotina de alimentacao?', TRUE, DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('HOUR', 2, DATEADD('DAY', -4, CURRENT_TIMESTAMP))),
    (24, 4, 'TUTOR', 'Sim, sem nenhuma mudanca.', TRUE, DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', 2, DATEADD('DAY', -3, CURRENT_TIMESTAMP))),
    (25, 4, 'VETERINARIAN', 'Perfeito. Seguimos com o check-up programado.', TRUE, DATEADD('HOUR', -52, CURRENT_TIMESTAMP), DATEADD('HOUR', -50, CURRENT_TIMESTAMP)),
    (26, 4, 'TUTOR', 'Obrigado! Ate a consulta.', FALSE, DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL),

    (27, 5, 'TUTOR', 'Ola, doutor. Preciso remarcar a consulta da Nala.', TRUE, DATEADD('DAY', -6, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -6, CURRENT_TIMESTAMP))),
    (28, 5, 'VETERINARIAN', 'Sem problema, Juliana. Voce pode cancelar e solicitar um novo horario.', TRUE, DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -5, CURRENT_TIMESTAMP))),
    (29, 5, 'TUTOR', 'Consegui cancelar. Vou verificar outra data.', TRUE, DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -4, CURRENT_TIMESTAMP))),
    (30, 5, 'VETERINARIAN', 'Certo. Fico a disposicao.', TRUE, DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', -3, CURRENT_TIMESTAMP))),

    (31, 7, 'TUTOR', 'Dra. Amanda, a pele da Luna melhorou bastante.', TRUE, DATEADD('HOUR', -12, CURRENT_TIMESTAMP), DATEADD('HOUR', -11, CURRENT_TIMESTAMP)),
    (32, 7, 'VETERINARIAN', 'Fico feliz com a evolucao. Houve algum novo episodio de coceira?', TRUE, DATEADD('HOUR', -10, CURRENT_TIMESTAMP), DATEADD('HOUR', -9, CURRENT_TIMESTAMP)),
    (33, 7, 'TUTOR', 'Nao, ela esta bem mais confortavel.', TRUE, DATEADD('HOUR', -7, CURRENT_TIMESTAMP), DATEADD('HOUR', -6, CURRENT_TIMESTAMP)),
    (34, 7, 'VETERINARIAN', 'Otimo. Mantenha os cuidados ate o retorno.', FALSE, DATEADD('HOUR', -5, CURRENT_TIMESTAMP), NULL),

    (35, 8, 'TUTOR', 'Dr. Henrique, a Belinha esta ativa e se alimentando bem.', TRUE, DATEADD('HOUR', -9, CURRENT_TIMESTAMP), DATEADD('HOUR', -8, CURRENT_TIMESTAMP)),
    (36, 8, 'VETERINARIAN', 'Excelente. Os exames preventivos tambem ficaram adequados.', TRUE, DATEADD('HOUR', -7, CURRENT_TIMESTAMP), DATEADD('HOUR', -6, CURRENT_TIMESTAMP)),
    (37, 8, 'VETERINARIAN', 'Podemos manter o acompanhamento de rotina.', TRUE, DATEADD('HOUR', -5, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
    (38, 8, 'TUTOR', 'Perfeito, obrigada pelo retorno.', FALSE, DATEADD('HOUR', -4, CURRENT_TIMESTAMP), NULL);

-- =====================================================
-- AJUSTE DAS COLUNAS IDENTITY
-- IDs explicitos nao avancam necessariamente o proximo valor no H2.
-- Estes valores garantem que novos inserts do aplicativo nao colidam com o seed.
-- =====================================================

ALTER TABLE tutor ALTER COLUMN id RESTART WITH 11;
ALTER TABLE veterinarian ALTER COLUMN id RESTART WITH 5;
ALTER TABLE animal ALTER COLUMN id RESTART WITH 20;
ALTER TABLE tb_veterinarian_tutor_links ALTER COLUMN id RESTART WITH 17;
ALTER TABLE tb_notifications ALTER COLUMN id RESTART WITH 22;
ALTER TABLE tb_device_push_tokens ALTER COLUMN id RESTART WITH 1;
ALTER TABLE tb_appointments ALTER COLUMN id RESTART WITH 26;
ALTER TABLE tb_medical_records ALTER COLUMN id RESTART WITH 14;
ALTER TABLE tb_prescriptions ALTER COLUMN id RESTART WITH 9;
ALTER TABLE tb_prescription_items ALTER COLUMN id RESTART WITH 17;
ALTER TABLE tb_exams ALTER COLUMN id RESTART WITH 16;
ALTER TABLE tb_vaccinations ALTER COLUMN id RESTART WITH 22;
ALTER TABLE tb_conversations ALTER COLUMN id RESTART WITH 9;
ALTER TABLE tb_messages ALTER COLUMN id RESTART WITH 39;

-- =====================================================
-- CONSULTAS DE CONFERENCIA
-- Remova o comentario das consultas desejadas no H2 Console.
-- =====================================================

-- SELECT COUNT(*) AS tutors FROM tutor;
-- SELECT COUNT(*) AS veterinarians FROM veterinarian;
-- SELECT COUNT(*) AS animals FROM animal;
-- SELECT status, COUNT(*) AS total FROM tb_veterinarian_tutor_links GROUP BY status ORDER BY status;
-- SELECT status, COUNT(*) AS total FROM tb_appointments GROUP BY status ORDER BY status;
-- SELECT COUNT(*) AS medical_records FROM tb_medical_records;
-- SELECT COUNT(*) AS prescriptions FROM tb_prescriptions;
-- SELECT COUNT(*) AS prescription_items FROM tb_prescription_items;
-- SELECT status, COUNT(*) AS total FROM tb_exams GROUP BY status ORDER BY status;
-- SELECT COUNT(*) AS vaccinations FROM tb_vaccinations;
-- SELECT COUNT(*) AS conversations FROM tb_conversations;
-- SELECT COUNT(*) AS messages FROM tb_messages;
-- SELECT COUNT(*) AS notifications FROM tb_notifications;

-- Dashboard do Dr. Gabriel Martins (veterinarian_id = 1)
-- SELECT COUNT(*) AS linked_tutors FROM tb_veterinarian_tutor_links WHERE veterinarian_id = 1 AND status = 'ACCEPTED';
-- SELECT COUNT(*) AS active_patients FROM animal a WHERE EXISTS (SELECT 1 FROM tb_veterinarian_tutor_links l WHERE l.tutor_id = a.tutor_id AND l.veterinarian_id = 1 AND l.status = 'ACCEPTED');
-- SELECT COUNT(*) AS appointments_today FROM tb_appointments WHERE veterinarian_id = 1 AND scheduled_at >= CURRENT_DATE AND scheduled_at < DATEADD('DAY', 1, CURRENT_DATE) AND status <> 'CANCELLED';
-- SELECT status, COUNT(*) AS total FROM tb_appointments WHERE veterinarian_id = 1 GROUP BY status ORDER BY status;
-- SELECT COUNT(*) AS pending_exams FROM tb_exams e JOIN tb_medical_records mr ON mr.id = e.medical_record_id WHERE mr.veterinarian_id = 1 AND e.status = 'REQUESTED';
-- SELECT a.species, COUNT(*) AS total FROM animal a WHERE EXISTS (SELECT 1 FROM tb_veterinarian_tutor_links l WHERE l.tutor_id = a.tutor_id AND l.veterinarian_id = 1 AND l.status = 'ACCEPTED') GROUP BY a.species ORDER BY total DESC;
-- SELECT id, animal_id, scheduled_at, status FROM tb_appointments WHERE veterinarian_id = 1 AND scheduled_at >= CURRENT_TIMESTAMP AND status IN ('PENDING', 'CONFIRMED') ORDER BY scheduled_at FETCH FIRST 5 ROWS ONLY;

-- =====================================================
-- CREDENCIAIS PARA DEMONSTRACAO
-- =====================================================

-- Tutor principal
-- Nome: Mariana Oliveira
-- CPF: 12345678901
-- Senha demonstrativa: 8 caracteres (consulte este arquivo localmente)

-- Veterinario principal
-- Nome: Dr. Gabriel Martins
-- CPF: 98765432100
-- CRMV: 12345/SP
-- Senha demonstrativa: 8 caracteres (consulte este arquivo localmente)
