package com.cgro.appnettsegur.ui.analista

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.Proyecto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Genera en el dispositivo el PDF de una carta de presentación (M2, fase 4, CU-09).
 *
 * No se usa Cloud Storage (plan Spark): el PDF se crea aquí con android.graphics.pdf.PdfDocument y
 * luego CartasRepository lo sube en Base64. Contiene encabezado, fecha, cliente, proyecto, la tabla
 * de técnicos (nombre y cédula) y el nombre del analista que la emite.
 */
object CartaPdf {

    private const val ANCHO = 595  // A4 a 72 dpi (vertical)
    private const val ALTO = 842
    private const val MARGEN = 48f
    private val FMT = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")

    suspend fun generar(
        context: Context,
        proyecto: Proyecto,
        tecnicos: List<Empleado>,
        nombreAnalista: String,
        hoy: LocalDate = LocalDate.now(),
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
        val canvas = pagina.canvas

        val titulo = Paint().apply { color = COLOR_AZUL; textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val subtitulo = Paint().apply { color = COLOR_OSCURO; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val normal = Paint().apply { color = COLOR_OSCURO; textSize = 11f }
        val tenue = Paint().apply { color = COLOR_GRIS; textSize = 10f }
        val linea = Paint().apply { color = COLOR_GRIS; strokeWidth = 0.8f }

        var y = MARGEN + 10f

        // Encabezado de la empresa.
        canvas.drawText("NETTSEGUR TECNOLOGÍA & SEGURIDAD", MARGEN, y, titulo)
        y += 18f
        canvas.drawText("ELECTRÓNICA S.A.S.", MARGEN, y, titulo)
        y += 16f
        canvas.drawText("Seguridad electrónica", MARGEN, y, tenue)
        y += 14f
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea)
        y += 28f

        // Fecha y título del documento.
        canvas.drawText("Chía, ${hoy.format(FMT)}", MARGEN, y, normal)
        y += 26f
        canvas.drawText("CARTA DE PRESENTACIÓN", MARGEN, y, subtitulo)
        y += 24f

        // Datos del proyecto y del cliente.
        canvas.drawText("Cliente: ${proyecto.nombreCliente}", MARGEN, y, normal); y += 16f
        canvas.drawText("Proyecto: ${proyecto.nombreProyecto}", MARGEN, y, normal); y += 24f

        canvas.drawText(
            "Por medio de la presente, Nettsegur Tecnología & Seguridad Electrónica S.A.S. presenta al",
            MARGEN, y, normal,
        ); y += 15f
        canvas.drawText(
            "siguiente personal técnico autorizado para prestar el servicio en el proyecto referido:",
            MARGEN, y, normal,
        ); y += 24f

        // Tabla de técnicos: Nombre | Cédula.
        val colNombre = MARGEN
        val colCedula = ANCHO - MARGEN - 150f
        canvas.drawText("Nombre", colNombre, y, subtitulo)
        canvas.drawText("Cédula", colCedula, y, subtitulo)
        y += 8f
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea)
        y += 18f
        tecnicos.forEach { t ->
            canvas.drawText(t.nombre, colNombre, y, normal)
            canvas.drawText(t.cedula, colCedula, y, normal)
            y += 18f
        }
        y += 24f

        // Firma del analista que emite.
        canvas.drawText("Atentamente,", MARGEN, y, normal); y += 40f
        canvas.drawLine(MARGEN, y, MARGEN + 200f, y, linea); y += 16f
        canvas.drawText(nombreAnalista.ifBlank { "Área administrativa" }, MARGEN, y, normal); y += 15f
        canvas.drawText("Área administrativa · Nettsegur S.A.S.", MARGEN, y, tenue)

        doc.finishPage(pagina)

        val carpeta = File(context.cacheDir, "cartas").apply { mkdirs() }
        val archivo = File(carpeta, "carta_${proyecto.proyectoId}_${System.currentTimeMillis()}.pdf")
        archivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        archivo
    }

    private const val COLOR_AZUL = 0xFF1F3C80.toInt()
    private const val COLOR_OSCURO = 0xFF29282D.toInt()
    private const val COLOR_GRIS = 0xFF5C5B61.toInt()
}
