// Contagens apos importacao; comparar com exports.py validate e com Oracle no mesmo snapshot.
const name = process.env.MONGODB_DB;
if (!/^[A-Za-z0-9_-]+$/.test(name || "")) throw new Error("MONGODB_DB invalido");
const database = db.getSiblingDB(name);
for (const collection of ["animal_profiles", "tutor_summaries", "veterinary_appointments"]) {
    print(collection + ": " + database.getCollection(collection).countDocuments({}));
}
