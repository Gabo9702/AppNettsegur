package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.data.model.DocumentoCumplimiento
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.EstadoCumplimiento
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import java.time.LocalDate

/** Una alerta de cumplimiento de un documento concreto de un empleado (M3, fase 4). */
data class AlertaVencimiento(
    val empleadoId: String,
    val empleadoNombre: String,
    val tipo: TipoCumplimiento,
    val estado: EstadoCumplimiento, // VENCIDO o POR_VENCER (nunca VIGENTE)
    val fechaVencimiento: LocalDate?, // null si el documento falta
) {
    /** Clave estable por empleado, tipo y vencimiento (para el ID del aviso y para no duplicar). */
    val clave: String get() = "venc_${empleadoId}_${tipo.name}_${fechaVencimiento ?: "FALTA"}"
}

/** Resumen de vencimientos para la tarjeta del inicio de RH. */
data class ResumenVencimientos(
    val vencidos: Int,
    val porVencer: Int,
    val alertas: List<AlertaVencimiento>,
)

/**
 * Revisa el cumplimiento de los empleados (M3, fase 4, flujo alterno de CU-07).
 *
 * Lógica PURA (sin Android ni Firebase) para probarla con una fecha fija (fase 5). Aplica la misma
 * regla que Empleado.estadoCumplimiento, pero documento por documento (necesitamos la fecha de cada
 * uno para el aviso): un documento que falta se considera VENCIDO; uno que vence dentro de
 * [Limites.DIAS_POR_VENCER] días o menos, POR_VENCER.
 *
 * Estas alertas NO se escriben en Firebase: son locales de la app de RH.
 */
object VencimientosRh {

    fun revisar(empleados: List<Empleado>, hoy: LocalDate): ResumenVencimientos {
        val alertas = buildList {
            for (empleado in empleados) {
                for (tipo in TipoCumplimiento.entries) {
                    val doc = empleado.cumplimiento[tipo]
                    val estado = estadoDocumento(doc, hoy)
                    if (estado != EstadoCumplimiento.VIGENTE) {
                        add(
                            AlertaVencimiento(
                                empleadoId = empleado.empleadoId,
                                empleadoNombre = empleado.nombre,
                                tipo = tipo,
                                estado = estado,
                                fechaVencimiento = doc?.fechaVencimiento,
                            ),
                        )
                    }
                }
            }
        }
        return ResumenVencimientos(
            vencidos = alertas.count { it.estado == EstadoCumplimiento.VENCIDO },
            porVencer = alertas.count { it.estado == EstadoCumplimiento.POR_VENCER },
            alertas = alertas,
        )
    }

    private fun estadoDocumento(doc: DocumentoCumplimiento?, hoy: LocalDate): EstadoCumplimiento = when {
        doc == null -> EstadoCumplimiento.VENCIDO
        doc.fechaVencimiento.isBefore(hoy) -> EstadoCumplimiento.VENCIDO
        !doc.fechaVencimiento.isAfter(hoy.plusDays(Limites.DIAS_POR_VENCER)) -> EstadoCumplimiento.POR_VENCER
        else -> EstadoCumplimiento.VIGENTE
    }
}
