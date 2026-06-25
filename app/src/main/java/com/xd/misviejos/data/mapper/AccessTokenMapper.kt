package com.xd.misviejos.data.mapper

import com.xd.misviejos.data.model.AccessTokenDTO
import com.xd.misviejos.domain.model.AccessToken

fun AccessTokenDTO.toDomain(): AccessToken {
    return AccessToken(
        token = token,
        groupId = groupId,
        nombreUsuario = nombreUsuario,
        rol = rol,
        pin = pin
    )
}

fun AccessToken.toDTO(): AccessTokenDTO {
    return AccessTokenDTO(
        token = token,
        groupId = groupId,
        nombreUsuario = nombreUsuario,
        rol = rol,
        pin = pin
    )
}
