package com.xd.misviejos.data.mapper

import com.xd.misviejos.data.model.AhorroMensualDTO
import com.xd.misviejos.data.model.GastoDTO
import com.xd.misviejos.domain.model.AhorroMensual
import com.xd.misviejos.domain.model.Gasto

fun GastoDTO.toDomain(): Gasto {
    return Gasto(
        id = this.gastoId,
        descripcion = this.descripcion,
        monto = this.monto,
        fechaRegistro = this.fechaRegistro
    )
}

fun Gasto.toDTO(): GastoDTO {
    return GastoDTO(
        gastoId = this.id,
        descripcion = this.descripcion,
        monto = this.monto,
        fechaRegistro = this.fechaRegistro
    )
}

fun AhorroMensualDTO.toDomain(): AhorroMensual {
    return AhorroMensual(
        ahorroId = this.ahorroId,
        groupId = this.groupId,
        mes = this.mes,
        anio = this.anio,
        metaSugerida = this.metaSugerida,
        totalRecolectado = this.totalRecolectado,
        aportes = this.aportes,
        totalGastado = this.totalGastado,
        gastos = this.gastos.map { it.toDomain() }
    )
}

fun AhorroMensual.toDTO(): AhorroMensualDTO {
    return AhorroMensualDTO(
        ahorroId = this.ahorroId,
        groupId = this.groupId,
        mes = this.mes,
        anio = this.anio,
        metaSugerida = this.metaSugerida,
        totalRecolectado = this.totalRecolectado,
        aportes = this.aportes,
        totalGastado = this.totalGastado,
        gastos = this.gastos.map { it.toDTO() }
    )
}
