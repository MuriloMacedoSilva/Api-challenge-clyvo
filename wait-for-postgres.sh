#!/bin/sh
set -eu

: "${DB_URL:?DB_URL deve ser definida}"
: "${DB_USERNAME:?DB_USERNAME deve ser definida}"
: "${DB_PASSWORD:?DB_PASSWORD deve ser definida}"

case "$DB_URL" in
    jdbc:postgresql://*) ;;
    *)
        echo "DB_URL invalida: use jdbc:postgresql://host:porta/banco" >&2
        exit 1
        ;;
esac

connection="${DB_URL#jdbc:postgresql://}"
host_port="${connection%%/*}"
database="${connection#*/}"
database="${database%%\?*}"

case "$host_port" in
    *:*)
        db_host="${host_port%%:*}"
        db_port="${host_port##*:}"
        ;;
    *)
        db_host="$host_port"
        db_port="5432"
        ;;
esac

if [ -z "$db_host" ] || [ -z "$database" ]; then
    echo "DB_URL invalida: use jdbc:postgresql://host:porta/banco" >&2
    exit 1
fi

attempt=1
max_attempts="${DB_WAIT_MAX_ATTEMPTS:-60}"
sleep_seconds="${DB_WAIT_INTERVAL_SECONDS:-2}"

case "$db_port:$max_attempts:$sleep_seconds" in
    *[!0-9:]*|:*|*::*|*:)
        echo "Porta e parametros de espera devem ser inteiros positivos." >&2
        exit 1
        ;;
esac
if [ "$db_port" -lt 1 ] || [ "$db_port" -gt 65535 ] \
    || [ "$max_attempts" -lt 1 ] || [ "$sleep_seconds" -lt 1 ]; then
    echo "Porta ou parametros de espera fora do intervalo permitido." >&2
    exit 1
fi

echo "Aguardando PostgreSQL em ${db_host}:${db_port}/${database}..."
until PGPASSWORD="$DB_PASSWORD" psql \
    --host="$db_host" \
    --port="$db_port" \
    --username="$DB_USERNAME" \
    --dbname="$database" \
    --no-password \
    --tuples-only \
    --command='SELECT 1' >/dev/null 2>&1; do
    if [ "$attempt" -ge "$max_attempts" ]; then
        echo "PostgreSQL nao ficou pronto apos ${max_attempts} tentativas." >&2
        exit 1
    fi
    echo "PostgreSQL indisponivel; tentativa ${attempt}/${max_attempts}."
    attempt=$((attempt + 1))
    sleep "$sleep_seconds"
done

echo "PostgreSQL pronto; iniciando API CLYVO."
exec java -jar /app/app.jar
