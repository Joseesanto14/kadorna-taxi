package com.digitalmuniz.kadornataxi.controller

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.digitalmuniz.kadornataxi.model.entities.Configuracao
import com.digitalmuniz.kadornataxi.model.entities.Viagem
import com.digitalmuniz.kadornataxi.model.enums.ColunasPdf
import com.digitalmuniz.kadornataxi.model.enums.Meses
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Gerador de Relatório PDF em formato ISO A4 Paisagem (842 x 595 pt).
 * Implementa paginação pré-calculada ("Página X de Y"), cabeçalho corporativo,
 * tabela com zebra striping, alinhamento semântico por coluna e tratamento seguro de recursos.
 */
class RelatorioPdfGenerator(
    private val context: Context,
    private val listaViagens: List<Viagem>,
    private val config: Configuracao,
    mesAno: String
) {

    companion object {
        // ISO A4 Paisagem em PostScript points (1/72 inch): 842 x 595 pt (29,7 cm x 21,0 cm)
        private const val PAGE_WIDTH = 842
        private const val PAGE_HEIGHT = 595

        private const val MARGEM_HORIZONTAL = 24f
        private const val MARGEM_SUPERIOR = 24f
        private const val MARGEM_INFERIOR = 34f

        private const val PADDING_CELULA_H = 3.5f
        private const val PADDING_CELULA_V = 4.5f
        private const val ALTURA_LINHA_PADRAO = 15f
        private const val ALTURA_BLOCO_TOTAIS = 96f

        private const val FILE_NAME = "relatorio_viagens"
        private const val TAG = "RelatorioPdfGenerator"
    }

    private val mesAnoExtenso = converterMesAnoParaExtenso(mesAno)
    private val centroPagina = PAGE_WIDTH / 2f
    private val larguraTabela = ColunasPdf.entries.sumOf { it.larguraPt.toDouble() }.toFloat()
    private val dataHoraEmissao = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ColunasPdf.LOCALE_PT_BR).format(Date())

    private val painter = PdfPainter()

    private data class LinhaMedida(
        val layouts: List<StaticLayout>,
        val altura: Float,
        val indiceGlobal: Int
    )

    private data class PaginaPlanejada(
        val linhas: List<LinhaMedida>,
        val contemTotais: Boolean
    )

    /**
     * Gera o arquivo PDF em disco e abre o visualizador (ideal para chamada simples).
     */
    fun gerarRelatorio() {
        val arquivo = gerarArquivoPdf()
        abrirDocumento(arquivo)
    }

    /**
     * Gera apenas o arquivo PDF (pode ser executado em background thread)
     * e retorna o File salvo no armazenamento do aplicativo.
     */
    fun gerarArquivoPdf(): File {
        val document = PdfDocument()
        try {
            val layoutsCabecalhoColunas = criarLayoutsCabecalhoColunas()
            val alturaCabecalhoColunas = layoutsCabecalhoColunas.maxOf { it.height }.toFloat() + (PADDING_CELULA_V * 2f)
            val alturaTopoPagina = calcularAlturaCabecalhoInstitucional() + alturaCabecalhoColunas
            val limiteYConteudo = PAGE_HEIGHT - MARGEM_INFERIOR

            // 1. Medir todas as linhas de viagens
            val todasLinhasMedidas = listaViagens.mapIndexed { index, viagem ->
                val layouts = criarLayoutsParaLinha(viagem)
                val alturaTexto = layouts.maxOf { it.height }.toFloat()
                LinhaMedida(
                    layouts = layouts,
                    altura = alturaTexto + (PADDING_CELULA_V * 2f),
                    indiceGlobal = index
                )
            }

            // 2. Planejar paginação para saber o total exato de páginas ("Página X de Y")
            val paginas = planejarPaginas(todasLinhasMedidas, alturaTopoPagina, limiteYConteudo)
            val totalPaginas = paginas.size

            // 3. Renderizar cada página
            paginas.forEachIndexed { pageIndex, pagina ->
                val numeroPagina = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, numeroPagina).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var yPos = desenharCabecalhoInstitucional(canvas)
                yPos = desenharCabecalhoColunas(canvas, yPos, layoutsCabecalhoColunas, alturaCabecalhoColunas)

                pagina.linhas.forEach { linha ->
                    desenharLinhaViagem(canvas, yPos, linha)
                    yPos += linha.altura
                }

                if (pagina.contemTotais) {
                    yPos += 10f
                    desenharTotais(canvas, yPos)
                }

                desenharRodape(canvas, numeroPagina, totalPaginas)
                document.finishPage(page)
            }

            return salvarDocumento(document)
        } finally {
            document.close()
        }
    }

    /**
     * Abre o PDF gerado no visualizador padrão ou exibe a Share Sheet como fallback.
     */
    fun abrirDocumento(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            context.startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nenhum leitor de PDF padrão; abrindo compartilhamento.", e)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Viagens - $mesAnoExtenso")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Abrir ou Compartilhar Relatório PDF"))
        }
    }

    // --- Planejamento de Paginação ---

    private fun planejarPaginas(
        linhas: List<LinhaMedida>,
        alturaTopoPagina: Float,
        limiteYConteudo: Float
    ): List<PaginaPlanejada> {
        if (linhas.isEmpty()) {
            return listOf(PaginaPlanejada(emptyList(), contemTotais = true))
        }

        val paginas = mutableListOf<MutableList<LinhaMedida>>()
        var paginaAtual = mutableListOf<LinhaMedida>()
        var yAtual = alturaTopoPagina

        for (linha in linhas) {
            if (paginaAtual.isNotEmpty() && (yAtual + linha.altura > limiteYConteudo)) {
                paginas.add(paginaAtual)
                paginaAtual = mutableListOf()
                yAtual = alturaTopoPagina
            }
            paginaAtual.add(linha)
            yAtual += linha.altura
        }
        if (paginaAtual.isNotEmpty()) {
            paginas.add(paginaAtual)
        }

        // Verifica se o bloco de totais cabe na última página
        val cabeTotaisNaUltima = (yAtual + 10f + ALTURA_BLOCO_TOTAIS) <= limiteYConteudo
        val resultado = paginas.mapIndexed { idx, lista ->
            val isUltima = idx == paginas.lastIndex
            PaginaPlanejada(lista, contemTotais = isUltima && cabeTotaisNaUltima)
        }.toMutableList()

        if (!cabeTotaisNaUltima) {
            resultado.add(PaginaPlanejada(emptyList(), contemTotais = true))
        }

        return resultado
    }

    // --- Funções de Desenho ---

    private fun calcularAlturaCabecalhoInstitucional(): Float {
        var y = MARGEM_SUPERIOR
        val temLinha1 = !config.nomeFantasia.isNullOrBlank() || !config.titularCNPJ.isNullOrBlank() || !config.cnpj.isNullOrBlank()
        val temLinha2 = !config.telefone.isNullOrBlank() || !config.email.isNullOrBlank()

        if (temLinha1) y += ALTURA_LINHA_PADRAO
        if (temLinha2) y += ALTURA_LINHA_PADRAO - 2f
        y += ALTURA_LINHA_PADRAO + 10f
        return y
    }

    private fun desenharCabecalhoInstitucional(canvas: Canvas): Float {
        var y = MARGEM_SUPERIOR + 10f

        // Linha 1: Nome Fantasia / Titular + CNPJ
        val partesLinha1 = mutableListOf<String>()
        if (!config.nomeFantasia.isNullOrBlank()) partesLinha1.add(config.nomeFantasia.trim())
        if (!config.titularCNPJ.isNullOrBlank()) partesLinha1.add(config.titularCNPJ.trim())
        if (!config.cnpj.isNullOrBlank()) partesLinha1.add("CNPJ: ${config.cnpj.trim()}")

        if (partesLinha1.isNotEmpty()) {
            canvas.drawText(partesLinha1.joinToString("  •  "), centroPagina, y, painter.companyTitleStyle)
            y += ALTURA_LINHA_PADRAO
        }

        // Linha 2: Telefone e E-mail
        val partesLinha2 = mutableListOf<String>()
        if (!config.telefone.isNullOrBlank()) partesLinha2.add("Tel.: ${config.telefone.trim()}")
        if (!config.email.isNullOrBlank()) partesLinha2.add("E-mail: ${config.email.trim()}")

        if (partesLinha2.isNotEmpty()) {
            canvas.drawText(partesLinha2.joinToString("  •  "), centroPagina, y, painter.headerSubtitleStyle)
            y += ALTURA_LINHA_PADRAO - 2f
        }

        // Linha 3: Título do Relatório
        canvas.drawText("Relação de Táxi — Mês: $mesAnoExtenso", centroPagina, y + 2f, painter.reportTitleStyle)
        return y + 10f
    }

    private fun desenharCabecalhoColunas(
        canvas: Canvas,
        yTopo: Float,
        layouts: List<StaticLayout>,
        alturaCabecalho: Float
    ): Float {
        // Fundo verde escuro institucional (#1B5E20)
        val rectCabecalho = RectF(
            MARGEM_HORIZONTAL,
            yTopo,
            MARGEM_HORIZONTAL + larguraTabela,
            yTopo + alturaCabecalho
        )
        canvas.drawRect(rectCabecalho, painter.tableHeaderBgPaint)

        var x = MARGEM_HORIZONTAL
        layouts.forEachIndexed { index, layout ->
            val coluna = ColunasPdf.entries[index]
            val offsetVertical = (alturaCabecalho - layout.height) / 2f
            canvas.save()
            canvas.translate(x + PADDING_CELULA_H, yTopo + offsetVertical)
            layout.draw(canvas)
            canvas.restore()
            x += coluna.larguraPt
        }

        return yTopo + alturaCabecalho
    }

    private fun desenharLinhaViagem(canvas: Canvas, yTopo: Float, linha: LinhaMedida) {
        val yBase = yTopo + linha.altura

        // Zebra striping (linhas ímpares com fundo suave)
        if (linha.indiceGlobal % 2 == 1) {
            canvas.drawRect(
                MARGEM_HORIZONTAL,
                yTopo,
                MARGEM_HORIZONTAL + larguraTabela,
                yBase,
                painter.zebraRowBgPaint
            )
        }

        var x = MARGEM_HORIZONTAL
        linha.layouts.forEachIndexed { index, layout ->
            val coluna = ColunasPdf.entries[index]
            val offsetVertical = (linha.altura - layout.height) / 2f

            canvas.save()
            canvas.translate(x + PADDING_CELULA_H, yTopo + offsetVertical)
            layout.draw(canvas)
            canvas.restore()

            x += coluna.larguraPt
            // Linha divisória vertical interna sutil
            if (index < ColunasPdf.entries.lastIndex) {
                canvas.drawLine(x, yTopo, x, yBase, painter.verticalGridPaint)
            }
        }

        // Bordas laterais e linha horizontal inferior da linha
        canvas.drawLine(MARGEM_HORIZONTAL, yTopo, MARGEM_HORIZONTAL, yBase, painter.horizontalGridPaint)
        canvas.drawLine(MARGEM_HORIZONTAL + larguraTabela, yTopo, MARGEM_HORIZONTAL + larguraTabela, yBase, painter.horizontalGridPaint)
        canvas.drawLine(MARGEM_HORIZONTAL, yBase, MARGEM_HORIZONTAL + larguraTabela, yBase, painter.horizontalGridPaint)
    }

    private fun desenharTotais(canvas: Canvas, yInicial: Float) {
        val totais = calcularResumoFinanceiro()
        val larguraBox = 235f
        val xDireita = MARGEM_HORIZONTAL + larguraTabela
        val xEsquerda = xDireita - larguraBox
        val rectBox = RectF(xEsquerda, yInicial, xDireita, yInicial + ALTURA_BLOCO_TOTAIS)

        // Fundo suave e borda para destacar o quadro de totais
        canvas.drawRoundRect(rectBox, 6f, 6f, painter.totalsBoxBgPaint)
        canvas.drawRoundRect(rectBox, 6f, 6f, painter.totalsBoxBorderPaint)

        val paresTotais = listOf(
            "Número de Viagens:" to "${listaViagens.size}",
            "Total valor KM:" to ColunasPdf.formatarMoeda(totais.kms),
            "Total valor Espera:" to ColunasPdf.formatarMoeda(totais.espera),
            "Total valor Serviço:" to ColunasPdf.formatarMoeda(totais.servico)
        )

        var currentY = yInicial + 16f
        val xLabel = xEsquerda + 12f
        val xValor = xDireita - 12f

        paresTotais.forEach { (label, valor) ->
            canvas.drawText(label, xLabel, currentY, painter.totalLabelStyle)
            canvas.drawText(valor, xValor, currentY, painter.totalValueStyle)
            currentY += 15f
        }

        // Linha divisória antes do Total Geral
        canvas.drawLine(xLabel, currentY - 9f, xValor, currentY - 9f, painter.horizontalGridPaint)
        currentY += 5f

        canvas.drawText("TOTAL GERAL:", xLabel, currentY, painter.grandTotalLabelStyle)
        canvas.drawText(ColunasPdf.formatarMoeda(totais.geral), xValor, currentY, painter.grandTotalValueStyle)
    }

    private fun desenharRodape(canvas: Canvas, paginaAtual: Int, totalPaginas: Int) {
        val yLinha = PAGE_HEIGHT - MARGEM_INFERIOR + 8f
        val yTexto = PAGE_HEIGHT - 12f
        val xDireita = MARGEM_HORIZONTAL + larguraTabela

        canvas.drawLine(MARGEM_HORIZONTAL, yLinha, xDireita, yLinha, painter.horizontalGridPaint)
        canvas.drawText("Kadorna Táxi • Emitido em $dataHoraEmissao", MARGEM_HORIZONTAL, yTexto, painter.footerLeftStyle)
        canvas.drawText("Página $paginaAtual de $totalPaginas", xDireita, yTexto, painter.footerRightStyle)
    }

    // --- Builders de StaticLayout ---

    private fun criarLayoutsCabecalhoColunas(): List<StaticLayout> {
        return ColunasPdf.entries.map { coluna ->
            val larguraInterna = (coluna.larguraPt - (PADDING_CELULA_H * 2f)).toInt().coerceAtLeast(10)
            StaticLayout.Builder.obtain(
                coluna.nome,
                0,
                coluna.nome.length,
                painter.columnHeaderTextPaint,
                larguraInterna
            )
                .setAlignment(coluna.alinhamento)
                .setIncludePad(false)
                .build()
        }
    }

    private fun criarLayoutsParaLinha(viagem: Viagem): List<StaticLayout> {
        return ColunasPdf.entries.map { coluna ->
            val texto = coluna.extrairDado(viagem)
            val larguraInterna = (coluna.larguraPt - (PADDING_CELULA_H * 2f)).toInt().coerceAtLeast(10)
            StaticLayout.Builder.obtain(
                texto,
                0,
                texto.length,
                painter.bodyTextPaint,
                larguraInterna
            )
                .setAlignment(coluna.alinhamento)
                .setIncludePad(false)
                .build()
        }
    }

    private fun calcularResumoFinanceiro() = object {
        val kms = listaViagens.sumOf { it.valorKms.toDouble() }
        val espera = listaViagens.sumOf { it.valorHoraEspera.toDouble() }
        val servico = listaViagens.sumOf { it.valorServico.toDouble() }
        val geral = listaViagens.sumOf { it.valorTotal.toDouble() }
    }

    private fun converterMesAnoParaExtenso(mesAno: String): String {
        val partes = mesAno.split("-")
        if (partes.size < 2) return mesAno
        val mesExtenso = Meses.buscarPorNumero(partes[1])
        val ano = partes[0]

        return if (partes.size > 2) {
            "$mesExtenso de $ano - ${partes.subList(2, partes.size).joinToString("-")}"
        } else {
            "$mesExtenso de $ano"
        }
    }

    private fun salvarDocumento(document: PdfDocument): File {
        val nomeArquivo = "${FILE_NAME}_${mesAnoExtenso}.pdf"
        val mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (mediaDir != null && !mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        val arquivo = File(mediaDir, nomeArquivo)

        try {
            FileOutputStream(arquivo).use { fos ->
                document.writeTo(fos)
                Log.d(TAG, "PDF salvo em: ${arquivo.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao salvar PDF", e)
            throw e
        }
        return arquivo
    }

    /**
     * Paleta de estilos tipográficos e gráficos para renderização do relatório.
     */
    private class PdfPainter {
        val companyTitleStyle = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textAlign = Paint.Align.CENTER
            textSize = 11.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerSubtitleStyle = Paint().apply {
            color = Color.parseColor("#424242")
            textAlign = Paint.Align.CENTER
            textSize = 9.5f
            isAntiAlias = true
        }

        val reportTitleStyle = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val zebraRowBgPaint = Paint().apply {
            color = Color.parseColor("#F5F7F5")
            style = Paint.Style.FILL
        }

        val horizontalGridPaint = Paint().apply {
            color = Color.parseColor("#D6D6D6")
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val verticalGridPaint = Paint().apply {
            color = Color.parseColor("#E8E8E8")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        // Importante: TextPaint usado com StaticLayout deve ter textAlign = LEFT
        // para que o alinhamento seja controlado pelo Layout.Alignment de cada coluna.
        val columnHeaderTextPaint = TextPaint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyTextPaint = TextPaint().apply {
            color = Color.parseColor("#212121")
            textAlign = Paint.Align.LEFT
            textSize = 8.5f
            isAntiAlias = true
        }

        val totalsBoxBgPaint = Paint().apply {
            color = Color.parseColor("#F5F7F5")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val totalsBoxBorderPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val totalLabelStyle = Paint().apply {
            color = Color.parseColor("#424242")
            textAlign = Paint.Align.LEFT
            textSize = 9.5f
            isAntiAlias = true
        }

        val totalValueStyle = Paint().apply {
            color = Color.parseColor("#212121")
            textAlign = Paint.Align.RIGHT
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val grandTotalLabelStyle = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textAlign = Paint.Align.LEFT
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val grandTotalValueStyle = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textAlign = Paint.Align.RIGHT
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val footerLeftStyle = Paint().apply {
            color = Color.parseColor("#757575")
            textAlign = Paint.Align.LEFT
            textSize = 8f
            isAntiAlias = true
        }

        val footerRightStyle = Paint().apply {
            color = Color.parseColor("#757575")
            textAlign = Paint.Align.RIGHT
            textSize = 8f
            isFakeBoldText = true
            isAntiAlias = true
        }
    }
}
