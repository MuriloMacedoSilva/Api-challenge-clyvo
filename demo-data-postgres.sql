-- CLYVO - Massa manual para PostgreSQL em ambiente local/demonstracao.
-- NAO EXECUTAR EM PRODUCAO: o script limpa e recria todos os dados da aplicacao.
-- Execute somente depois que a API criar o schema pelo Hibernate.

-- O seed original usa DATEADD para continuar validavel pela suite H2. Estas
-- funcoes auxiliares preservam as datas relativas e sao removidas ao final.
\set ON_ERROR_STOP on
BEGIN;

CREATE FUNCTION dateadd(unit text, amount integer, value date)
RETURNS timestamp
LANGUAGE sql
AS $$
    SELECT value::timestamp + CASE upper(unit)
        WHEN 'MONTH' THEN make_interval(months => amount)
        WHEN 'DAY' THEN make_interval(days => amount)
        WHEN 'HOUR' THEN make_interval(hours => amount)
        WHEN 'MINUTE' THEN make_interval(mins => amount)
    END
$$;

CREATE FUNCTION dateadd(unit text, amount integer, value timestamp)
RETURNS timestamp
LANGUAGE sql
AS $$
    SELECT value + CASE upper(unit)
        WHEN 'MONTH' THEN make_interval(months => amount)
        WHEN 'DAY' THEN make_interval(days => amount)
        WHEN 'HOUR' THEN make_interval(hours => amount)
        WHEN 'MINUTE' THEN make_interval(mins => amount)
    END
$$;

CREATE FUNCTION dateadd(unit text, amount integer, value timestamptz)
RETURNS timestamptz
LANGUAGE sql
AS $$
    SELECT value + CASE upper(unit)
        WHEN 'MONTH' THEN make_interval(months => amount)
        WHEN 'DAY' THEN make_interval(days => amount)
        WHEN 'HOUR' THEN make_interval(hours => amount)
        WHEN 'MINUTE' THEN make_interval(mins => amount)
    END
$$;

\ir demo-data.sql

DROP FUNCTION dateadd(text, integer, date);
DROP FUNCTION dateadd(text, integer, timestamp);
DROP FUNCTION dateadd(text, integer, timestamptz);

COMMIT;
