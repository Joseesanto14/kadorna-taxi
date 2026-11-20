package com.digitalmuniz.kadornataxi.model.enums

import android.text.Layout
import com.digitalmuniz.kadornataxi.model.entities.Viagem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class ServicoItemPdf(
    val descricao: String,
    val valorFormatado: String,
    val ehTotal: Boolean = false
) {
    fun formatarTexto(): String =
        if (ehTotal) "$descricao $valorFormatado" else "$descricao: $valorFormatado"
}

/**
 * Representa as colunas da tabela do Relatório PDF em formato A4 Paisagem (842 x 595 pt).
 * Soma total das larguras: 28,00 cm (793,8 pt), cabendo perfeitamente na área útil de 794 pt
 * (842 pt de largura total - 48 pt de margens laterais).
 */
enum class ColunasPdf(
    val nome: String,
    larguraCm: Float,
    val alinhamento: Layout.Alignment
) {
    DIA("Dia", 1.20f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return try {
                viagem.diaMes ?: "-"
            } catch (_: Exception) {
                viagem.data.orEmpty().ifBlank { "-" }
            }
        }
    },
    HORA("Hora", 1.10f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.hora.orEmpty().ifBlank { "-" }
        }
    },
    ORIGEM("Origem", 2.25f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.origem.orEmpty().ifBlank { "-" }
        }
    },
    DESTINO("Destino", 2.45f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.destino.orEmpty().ifBlank { "-" }
        }
    },
    SOLICITANTE("Solicitante", 2.10f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.solicitante.orEmpty().trim().ifBlank { "-" }
        }
    },
    DESCRICAO("Descrição", 3.00f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.descricao.orEmpty().ifBlank { "-" }
        }
    },
    KMS_RODADOS("Kms", 1.55f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return String.format(LOCALE_PT_BR, "%.1f km", viagem.kmsRodados)
        }
    },
    VALOR_KMS("Valor Kms", 2.05f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorKms.toDouble())
        }
    },
    SERVICOS("Serviços", 4.65f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            val itens = extrairItensServicos(viagem)
            return if (itens.isEmpty()) "-" else itens.joinToString("\n") { it.formatarTexto() }
        }
    },
    HORAS_ESPERA("Espera (h)", 1.55f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return String.format(LOCALE_PT_BR, "%.2f", viagem.horaEspera)
        }
    },
    VALOR_ESPERA("Valor Esp.", 2.00f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorHoraEspera.toDouble())
        }
    },
    VALOR_TOTAL("Valor Total", 2.25f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorTotal.toDouble())
        }
    },
    MOTORISTA("Motorista", 1.85f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.motorista.orEmpty().ifBlank { "-" }
        }
    };

    val larguraPt: Float = larguraCm * 28.35f

    fun getMetadeLargura(): Float = larguraPt / 2f

    abstract fun extrairDado(viagem: Viagem): String

    companion object {
        val LOCALE_PT_BR: Locale = Locale("pt", "BR")

        fun formatarMoeda(valor: Double): String {
            val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(LOCALE_PT_BR))
            return "R$ " + df.format(valor)
        }

        fun extrairItensServicos(viagem: Viagem): List<ServicoItemPdf> {
            val servicos = viagem.servicos
            if (servicos.isEmpty()) {
                val totalLegado = viagem.valorTotalServicos
                return if (totalLegado > 0f) {
                    listOf(ServicoItemPdf("Serviço adicional", formatarMoeda(totalLegado.toDouble())))
                } else {
                    emptyList()
                }
            }

            val itens = servicos.map { servico ->
                val desc = servico.descricao.trim().ifBlank { "Serviço adicional" }
                ServicoItemPdf(desc, formatarMoeda(servico.valor.toDouble()))
            }.toMutableList()

            if (servicos.size >= 2) {
                itens.add(
                    ServicoItemPdf(
                        descricao = "Total serv.:",
                        valorFormatado = formatarMoeda(viagem.valorTotalServicos.toDouble()),
                        ehTotal = true
                    )
                )
            }
            return itens
        }

        fun formatarLinhasServicos(viagem: Viagem): List<String> {
            return extrairItensServicos(viagem).map { it.formatarTexto() }
        }
    }
}
