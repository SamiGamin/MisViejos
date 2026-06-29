"""
Script de migración: Fondo (Realtime DB) → transacciones_fondo (Firestore)
Genera un archivo JSON listo para importar manualmente a Firestore.

Cada documento en transacciones_fondo/{id} tendrá:
  id            : mismo id del RTDB
  groupId       : se debe reemplazar con el groupId real de la familia en Firestore
  tipo          : "INGRESO" (deposito) o "GASTO" (gasto)
  monto         : cuanto
  titulo        : "Aporte de {quien}" para INGRESO, descripcion (o "Gasto") para GASTO
  autorToken    : "MIGRACION_HISTORICA"
  timestamp     : creadoEn (milliseconds)
  mes           : mes calculado desde campo 'fecha'
  anio          : año calculado desde campo 'fecha'
  nombreHermano : quien (solo para INGRESO, None para GASTO)
  saldoAcumulado: None (se recalcula desde el app)
"""

import json
from datetime import datetime, timezone

# ─── CONFIGURACIÓN ──────────────────────────────────────────────────────────────
INPUT_FILE  = r"f:\AndroidStudio\MisViejos\mb-money-3c1e1-default-rtdb-export.json"
OUTPUT_FILE = r"f:\AndroidStudio\MisViejos\fondo_firestore_import.json"

# ⚠️  REEMPLAZA ESTO con el groupId real de la familia en tu colección family_groups
GROUP_ID = "MARTINEZBARRERA-2816"
# ────────────────────────────────────────────────────────────────────────────────

def epoch_ms_to_date(ms: int) -> tuple[int, int]:
    """Convierte milliseconds epoch a (mes, año)."""
    dt = datetime.fromtimestamp(ms / 1000, tz=timezone.utc)
    return dt.month, dt.year

with open(INPUT_FILE, "r", encoding="utf-8") as f:
    data = json.load(f)

fondo = data.get("Fondo", {})

docs = {}
saldo_acumulado = 0.0

# Ordenar por campo 'creadoEn' para calcular saldo cronológicamente
entries = sorted(fondo.values(), key=lambda x: x.get("creadoEn", 0))

stats = {"INGRESO": 0, "GASTO": 0, "total_ingresos": 0.0, "total_gastos": 0.0}

for entry in entries:
    old_id      = entry["id"]
    cuanto      = float(entry.get("cuanto", 0))
    tipo_old    = entry.get("tipo", "deposito")
    quien       = entry.get("quien", "Desconocido")
    descripcion = entry.get("descripcion", "")
    creadoEn    = entry.get("creadoEn", 0)
    fecha_ms    = entry.get("fecha", creadoEn)

    # Usar 'fecha' para determinar el mes/año del aporte
    mes, anio = epoch_ms_to_date(fecha_ms)

    if tipo_old == "deposito":
        tipo_nuevo      = "INGRESO"
        titulo          = f"Aporte de {quien}"
        nombre_hermano  = quien
        saldo_acumulado += cuanto
        stats["INGRESO"] += 1
        stats["total_ingresos"] += cuanto
    else:  # gasto
        tipo_nuevo      = "GASTO"
        titulo          = descripcion if descripcion else "Gasto"
        nombre_hermano  = None
        saldo_acumulado -= cuanto
        stats["GASTO"] += 1
        stats["total_gastos"] += cuanto

    doc = {
        "id":             old_id,
        "groupId":        GROUP_ID,
        "tipo":           tipo_nuevo,
        "monto":          cuanto,
        "titulo":         titulo,
        "autorToken":     "MIGRACION_HISTORICA",
        "timestamp":      creadoEn,
        "mes":            mes,
        "anio":           anio,
        "nombreHermano":  nombre_hermano,
    }

    docs[old_id] = doc

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump({"transacciones_fondo": docs, "saldo_final": saldo_acumulado}, f, ensure_ascii=False, indent=2)

print(f"✅  Migración completada:")
print(f"   Aportes (INGRESO): {stats['INGRESO']:>4}  →  {stats['total_ingresos']:>12,.0f} COP")
print(f"   Gastos  (GASTO)  : {stats['GASTO']:>4}  →  {stats['total_gastos']:>12,.0f} COP")
print(f"   Saldo neto final : {'':>10}  {saldo_acumulado:>12,.0f} COP")
print(f"\n📄  Archivo generado: {OUTPUT_FILE}")
print(f"\n⚠️  Recuerda:")
print(f"   1. Reemplazar GROUP_ID = 'TU_GROUP_ID_AQUI' con el ID real de tu familia.")
print(f"   2. Actualizar 'saldo_fondo_actual' en el doc de la familia en Firestore con el saldo neto.")
