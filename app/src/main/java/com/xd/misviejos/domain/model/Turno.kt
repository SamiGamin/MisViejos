package com.xd.misviejos.domain.model

import java.time.Instant

enum class EstadoTurno {
    PENDIENTE,   // Creado por la cuñada, pero ningún hermano ha dicho "Yo lo cubro"
    CONFIRMADO,  // Carlos entró y hundió: "Me haré cargo"
    COMPLETADO,  // Ya salieron de la clínica y dejaron la nota del médico
    CANCELADO    // La EPS canceló o reprogramó
}

enum class TipoEventoMedico {
    CITA_MEDICA,
    RECOGER_MEDICAMENTOS,
    AUTORIZACION_TRAMITE,
    EXAMEN_MEDICO
}

data class Turno(
    val id: String = "",
    val groupId: String = "",          // El cerrojo: "MARTINEZ-2026"

    // Tipo de Evento
    val tipoEvento: TipoEventoMedico = TipoEventoMedico.CITA_MEDICA,

    // Datos del paciente
    val paciente: String = "",         // "Papá (Arturo)" o "Mamá (Mercedes)"
    val especialidad: String = "",     // "Cardiología" o el título de la tarea (ej: "Resonancia magnética", "Losartán 50mg")
    val lugar: String = "",            // Clínica, farmacia, EPS, laboratorio
    val fechaInstante: Instant = Instant.now(), // [La regla de Senior: nada de Date() de Java]

    // Campos condicionales/específicos
    val doctor: String? = null,        // Cita médica
    val consultorio: String? = null,   // Cita médica
    val medicamento: String? = null,   // Medicamentos
    val dosis: String? = null,         // Medicamentos
    val requisitos: String? = null,    // Examen (ej: ayunas, orden médica)
    val documentos: String? = null,    // Autorización (ej: historia clínica, fotocopia de cédula)

    // Los responsables
    val hermanoSugerido: String = "",  // A quién se la tiró el algoritmo
    val hermanoConfirmado: String? = null, // Quién aceptó ir físicamente

    val estado: EstadoTurno = EstadoTurno.PENDIENTE,

    // El "Testigo" (Las notas que deja el doctor para el siguiente turno)
    val notasDelMedico: String? = null
)