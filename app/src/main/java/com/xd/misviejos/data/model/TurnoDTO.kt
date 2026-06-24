package com.xd.misviejos.data.model

import com.google.firebase.Timestamp
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico

data class TurnoDTO(
    var id: String = "",
    var groupId: String = "",
    var tipoEvento: String = TipoEventoMedico.CITA_MEDICA.name,
    var paciente: String = "",
    var especialidad: String = "",
    var lugar: String = "",
    var fecha: Timestamp = Timestamp.now(),
    
    // Campos condicionales/específicos
    var doctor: String? = null,
    var consultorio: String? = null,
    var medicamento: String? = null,
    var dosis: String? = null,
    var requisitos: String? = null,
    var documentos: String? = null,

    var hermanoSugerido: String = "",
    var hermanoConfirmado: String? = null,
    var estado: String = EstadoTurno.PENDIENTE.name,
    var notasDelMedico: String? = null
)