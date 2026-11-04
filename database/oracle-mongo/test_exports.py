import base64
import copy
import json
import tempfile
import unittest
from types import SimpleNamespace
from pathlib import Path

from exports import COLLECTIONS, decode_spool, load_array, run, validated


ROOT = Path(__file__).parent


class ExportTests(unittest.TestCase):
    def test_fictitious_fixtures_and_reimport(self):
        for collection in COLLECTIONS:
            with self.subTest(collection=collection):
                documents = load_array(ROOT / "examples" / f"{collection}.json", collection)
                self.assertEqual(len({doc["_id"] for doc in documents}), len(documents))

    def test_large_unicode_document_and_empty_spool(self):
        document = load_array(ROOT / "examples" / "animal_profiles.json", "animal_profiles")[0]
        document["vaccinations"][0]["vaccineName"] = "Fictícia 🐾" * 6000
        payload = json.dumps(document, ensure_ascii=False).encode("utf-8")
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "animal_profiles.b64"
            # Fronteiras de chunk nao podem introduzir quebras dentro de strings JSON.
            chunks = [payload[i:i + 3000] for i in range(0, len(payload), 3000)]
            file.write_text("DOC_BEGIN\n" + "\n".join(base64.b64encode(chunk).decode("ascii")
                         for chunk in chunks) + "\nDOC_END\n", encoding="ascii")
            self.assertEqual(decode_spool(file, "animal_profiles"), [document])
            file.write_text("", encoding="ascii")
            self.assertEqual(decode_spool(file, "animal_profiles"), [])

    def test_invalid_keys_ids_and_duplicate_upsert_keys(self):
        document = load_array(ROOT / "examples" / "tutor_summaries.json", "tutor_summaries")[0]
        for key, value in (("cpf", "12345678901"), ("password", "test")):
            bad = copy.deepcopy(document)
            bad[key] = value
            with self.assertRaises(ValueError):
                validated([bad], "tutor_summaries")
        bad = copy.deepcopy(document)
        bad["tutorId"] = "11.0"
        with self.assertRaises(ValueError):
            validated([bad], "tutor_summaries")
        with self.assertRaises(ValueError):
            validated([document, copy.deepcopy(document)], "tutor_summaries")

    def test_partial_or_non_spool_output_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "bad.b64"
            for content in ("DOC_BEGIN\nYWJj\n", "SP2-0000: erro\n", "DOC_BEGIN\nINVALID!\nDOC_END\n"):
                file.write_text(content, encoding="ascii")
                with self.assertRaises(ValueError):
                    decode_spool(file, "animal_profiles")

    def test_convert_and_validate_all_collections_including_empty(self):
        with tempfile.TemporaryDirectory() as directory:
            spool_dir = Path(directory) / "raw"
            output_dir = Path(directory) / "converted"
            spool_dir.mkdir()
            for collection in COLLECTIONS:
                documents = load_array(ROOT / "examples" / f"{collection}.json", collection)
                if collection == "veterinary_appointments":
                    documents = []
                blocks = []
                for document in documents:
                    encoded = base64.b64encode(json.dumps(document, ensure_ascii=False).encode()).decode()
                    blocks.append(f"DOC_BEGIN\n{encoded}\nDOC_END\n")
                (spool_dir / f"{collection}.b64").write_text("".join(blocks), encoding="ascii")
            run(SimpleNamespace(action="convert", input_dir=spool_dir, output_dir=output_dir))
            run(SimpleNamespace(action="validate", input_dir=output_dir))
            self.assertEqual((output_dir / "veterinary_appointments.json").read_text(), "[]\n")
            self.assertEqual((output_dir / "veterinary_appointments.jsonl").read_text(), "")


if __name__ == "__main__":
    unittest.main()
