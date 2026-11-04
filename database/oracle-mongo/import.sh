#!/usr/bin/env bash
set -euo pipefail

: "${MONGODB_URI:?Defina MONGODB_URI explicitamente}"
: "${MONGODB_DB:?Defina MONGODB_DB explicitamente}"
[[ "$MONGODB_DB" =~ ^[A-Za-z0-9_-]+$ ]] || { printf 'Nome de banco invalido\n' >&2; exit 1; }
[[ $# -eq 1 && -d "$1" ]] || { printf 'Uso: bash import.sh /caminho/exports\n' >&2; exit 1; }
command -v mongosh >/dev/null || { printf 'mongosh nao encontrado\n' >&2; exit 1; }
command -v mongoimport >/dev/null || { printf 'mongoimport nao encontrado\n' >&2; exit 1; }

script_dir="$(dirname "${BASH_SOURCE[0]}")"
python3 "$script_dir/exports.py" validate --input-dir "$1"

# Indices unicos sao criados antes dos writes; falha em dados legados duplicados interrompe a importacao.
mongosh "$MONGODB_URI" --quiet --file "$script_dir/indexes.mongosh.js"
for collection in animal_profiles tutor_summaries veterinary_appointments; do
    if [[ ! -s "$1/$collection.jsonl" ]]; then
        printf '%s: snapshot vazio; sem documentos para upsert\n' "$collection"
        continue
    fi
    mongoimport --uri "$MONGODB_URI" --db "$MONGODB_DB" \
        --collection "$collection" --type json --file "$1/$collection.jsonl" \
        --mode upsert --upsertFields _id
done
mongosh "$MONGODB_URI" --quiet --file "$script_dir/counts.mongosh.js"
