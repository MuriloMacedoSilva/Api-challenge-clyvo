// Uso pelo import.sh com MONGODB_DB definido; nunca remove collections.
const name = process.env.MONGODB_DB;
if (!/^[A-Za-z0-9_-]+$/.test(name || "")) throw new Error("MONGODB_DB invalido");
const database = db.getSiblingDB(name);
database.animal_profiles.createIndex({ animalId: 1 }, { unique: true });
database.animal_profiles.createIndex({ tutorId: 1 });
database.tutor_summaries.createIndex({ tutorId: 1 }, { unique: true });
database.veterinary_appointments.createIndex({ appointmentId: 1 }, { unique: true });
database.veterinary_appointments.createIndex({ veterinarianId: 1, scheduledAtLocal: 1 });
database.veterinary_appointments.createIndex({ animalId: 1 });
print("Indices verificados para " + name);
