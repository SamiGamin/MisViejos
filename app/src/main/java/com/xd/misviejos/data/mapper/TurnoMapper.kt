package com.xd.misviejos.data.mapper

import com.google.firebase.Timestamp
import com.xd.misviejos.data.model.TurnoDTO
import com.xd.misviejos.domain.model.EstadoTurno
import com.xd.misviejos.domain.model.TipoEventoMedico
import com.xd.misviejos.domain.model.Turno
import java.time.Instant
import java.util.Date

fun TurnoDTO.toDomain(): Turno {
    return Turno(
        id = this.id,
        groupId = this.groupId,
        tipoEvento = runCatching { TipoEventoMedico.valueOf(this.tipoEvento) }.getOrDefault(TipoEventoMedico.CITA_MEDICA),
        paciente = this.paciente,
        especialidad = this.especialidad,
        lugar = this.lugar,
        fechaInstante = Instant.ofEpochSecond(this.fecha.seconds, this.fecha.nanoseconds.toLong()),
        doctor = this.doctor,
        consultorio = this.consultorio,
        medicamento = this.medicamento,
        dosis = this.dosis,
        requisitos = this.requisitos,
        documentos = this.documentos,
        hermanoSugerido = this.hermanoSugerido,
        hermanoConfirmado = this.hermanoConfirmado,
        estado = runCatching { EstadoTurno.valueOf(this.estado) }.getOrDefault(EstadoTurno.PENDIENTE),
        notasDelMedico = this.notasDelMedico
    )
}

fun Turno.toDTO(): TurnoDTO {
    return TurnoDTO(
        id = this.id,
        groupId = this.groupId,
        tipoEvento = this.tipoEvento.name,
        paciente = this.paciente,
        especialidad = this.especialidad,
        lugar = this.lugar,
        fecha = Timestamp(Date.from(this.fechaInstante)),
        doctor = this.doctor,
        consultorio = this.consultorio,
        medicamento = this.medicamento,
        dosis = this.dosis,
        requisitos = this.requisitos,
        documentos = this.documentos,
        hermanoSugerido = this.hermanoSugerido,
        hermanoConfirmado = this.hermanoConfirmado,
        estado = this.estado.name,
        notasDelMedico = this.notasDelMedico
    )
}