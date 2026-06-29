package com.xd.misviejos.data.mapper

import com.xd.misviejos.data.model.TransaccionFondoDTO
import com.xd.misviejos.domain.model.TransaccionFondo

fun TransaccionFondoDTO.toDomain(): TransaccionFondo {
    return TransaccionFondo(
        id = this.id,
        groupId = this.groupId,
        tipo = this.tipo,
        monto = this.monto,
        titulo = this.titulo,
        autorToken = this.autorToken,
        nombreHermano = this.nombreHermano,
        timestamp = this.timestamp,
        mes = this.mes,
        anio = this.anio
    )
}

fun TransaccionFondo.toDTO(): TransaccionFondoDTO {
    return TransaccionFondoDTO(
        id = this.id,
        groupId = this.groupId,
        tipo = this.tipo,
        monto = this.monto,
        titulo = this.titulo,
        autorToken = this.autorToken,
        nombreHermano = this.nombreHermano,
        timestamp = this.timestamp,
        mes = this.mes,
        anio = this.anio
    )
}
