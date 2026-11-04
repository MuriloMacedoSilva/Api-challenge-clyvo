#!/usr/bin/env python3
"""Decode SQL*Plus CLOB spools and validate CLYVO's versioned Mongo snapshots."""

import argparse
import base64
import binascii
import json
import re
from datetime import date, datetime, timezone
from pathlib import Path


COLLECTIONS = {
    "animal_profiles": ("animal", "animalId", {
        "tutorId", "species", "ageYears", "vaccinations",
    }),
    "tutor_summaries": ("tutor", "tutorId", {
        "animalCount", "vaccinationsAttentionCount", "pendingAppointmentsCount", "animals",
    }),
    "veterinary_appointments": ("appointment", "appointmentId", {
        "animalId", "tutorId", "veterinarianId", "scheduledAtLocal", "status",
    }),
}
COMMON = {"_id", "schemaVersion", "sourceSystem", "exportedAt"}
DECIMAL_ID = re.compile(r"[1-9][0-9]{0,18}\Z")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def fields(value, expected, context):
    require(type(value) is dict and set(value) == expected, f"{context}: campos inesperados/ausentes")


def identifier(value, context):
    require(type(value) is str and DECIMAL_ID.fullmatch(value) is not None
            and int(value) <= 9223372036854775807, f"{context}: ID decimal invalido")


def nonnegative(value, context):
    require(type(value) is int and value >= 0, f"{context}: contador invalido")


def iso_date(value, context):
    require(type(value) is str and re.fullmatch(r"\d{4}-\d{2}-\d{2}", value),
            f"{context}: data ISO invalida")
    date.fromisoformat(value)


def validate(document, collection):
    kind, source_key, extra = COLLECTIONS[collection]
    fields(document, COMMON | {source_key} | extra, collection)
    identifier(document[source_key], source_key)
    require(document["_id"] == f"oracle:{kind}:{document[source_key]}", "_id instavel")
    require(type(document["schemaVersion"]) is int and document["schemaVersion"] == 1,
            "schemaVersion invalido")
    require(document["sourceSystem"] == "CLYVO_ORACLE_19C", "sourceSystem invalido")
    stamp = document["exportedAt"]
    require(type(stamp) is str and re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z", stamp),
            "exportedAt invalido")
    require(datetime.fromisoformat(stamp.replace("Z", "+00:00")).tzinfo == timezone.utc,
            "exportedAt nao esta em UTC")

    if collection == "animal_profiles":
        identifier(document["tutorId"], "tutorId")
        require(type(document["species"]) is str and bool(document["species"]), "species invalida")
        nonnegative(document["ageYears"], "ageYears")
        require(type(document["vaccinations"]) is list, "vaccinations invalida")
        ids = set()
        for item in document["vaccinations"]:
            fields(item, {"vaccinationId", "vaccineName", "applicationDate", "nextDoseDate"}, "vaccination")
            identifier(item["vaccinationId"], "vaccinationId")
            require(item["vaccinationId"] not in ids, "vaccinationId repetido")
            ids.add(item["vaccinationId"])
            require(type(item["vaccineName"]) is str and bool(item["vaccineName"]), "vaccineName invalido")
            iso_date(item["applicationDate"], "applicationDate")
            if item["nextDoseDate"] is not None:
                iso_date(item["nextDoseDate"], "nextDoseDate")
    elif collection == "tutor_summaries":
        for key in ("animalCount", "vaccinationsAttentionCount", "pendingAppointmentsCount"):
            nonnegative(document[key], key)
        require(type(document["animals"]) is list, "animals invalido")
        ids = set()
        for item in document["animals"]:
            fields(item, {"animalId", "species"}, "animal resumido")
            identifier(item["animalId"], "animalId")
            require(item["animalId"] not in ids, "animalId repetido")
            ids.add(item["animalId"])
            require(type(item["species"]) is str and bool(item["species"]), "species invalida")
        require(document["animalCount"] == len(ids), "animalCount diverge da lista")
    else:
        for key in ("animalId", "tutorId", "veterinarianId"):
            identifier(document[key], key)
        scheduled = document["scheduledAtLocal"]
        require(type(scheduled) is str and re.fullmatch(
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{6}", scheduled),
            "scheduledAtLocal invalido")
        datetime.fromisoformat(scheduled)
        require(document["status"] in ("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"),
                "status invalido")


def validated(documents, collection):
    require(type(documents) is list, f"{collection}: esperado array JSON")
    seen = set()
    for document in documents:
        validate(document, collection)
        require(document["_id"] not in seen, f"{collection}: _id duplicado")
        seen.add(document["_id"])
    # Simula o mesmo upsert por _id em duas importacoes, inclusive array vazio.
    by_id = {}
    for _ in range(2):
        for document in documents:
            by_id[document["_id"]] = document
    require(len(by_id) == len(documents), f"{collection}: reimportacao duplicaria documentos")
    return documents


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"chave JSON repetida: {key}")
        result[key] = value
    return result


def decode_spool(path, collection):
    documents = []
    pieces = None
    for line_number, raw_line in enumerate(path.read_text(encoding="ascii").splitlines(), 1):
        line = raw_line.strip()
        if not line:
            continue
        if line == "DOC_BEGIN":
            require(pieces is None, f"{path}:{line_number}: inicio duplicado")
            pieces = []
        elif line == "DOC_END":
            require(pieces is not None and bool(pieces), f"{path}:{line_number}: documento incompleto")
            try:
                payload = b"".join(base64.b64decode(piece, validate=True) for piece in pieces)
                document = json.loads(payload.decode("utf-8"), object_pairs_hook=unique_object)
            except (binascii.Error, UnicodeError, json.JSONDecodeError) as exc:
                raise ValueError(f"{path}:{line_number}: base64/JSON invalido") from exc
            documents.append(document)
            pieces = None
        else:
            require(pieces is not None, f"{path}:{line_number}: saida inesperada do SQL*Plus")
            pieces.append(line)
    require(pieces is None, f"{path}: documento sem DOC_END")
    return validated(documents, collection)


def load_array(path, collection):
    return validated(json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object), collection)


def run(args):
    for collection in COLLECTIONS:
        if args.action == "convert":
            documents = decode_spool(args.input_dir / f"{collection}.b64", collection)
            args.output_dir.mkdir(parents=True, exist_ok=True)
            (args.output_dir / f"{collection}.json").write_text(
                json.dumps(documents, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
            (args.output_dir / f"{collection}.jsonl").write_text(
                "".join(json.dumps(doc, ensure_ascii=False, separators=(",", ":")) + "\n"
                        for doc in documents), encoding="utf-8")
        else:
            documents = load_array(args.input_dir / f"{collection}.json", collection)
            jsonl = args.input_dir / f"{collection}.jsonl"
            if jsonl.exists():
                lines = [json.loads(line, object_pairs_hook=unique_object)
                         for line in jsonl.read_text(encoding="utf-8").splitlines() if line.strip()]
                require(validated(lines, collection) == documents, f"{collection}: JSON e JSONL diferem")
        print(f"{collection}: {len(documents)} documentos validos")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("convert", "validate"))
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, help="Obrigatorio para convert")
    arguments = parser.parse_args()
    if arguments.action == "convert" and not arguments.output_dir:
        parser.error("--output-dir obrigatorio para convert")
    try:
        run(arguments)
    except (ValueError, OSError, json.JSONDecodeError) as error:
        parser.exit(1, f"Erro: {error}\n")
