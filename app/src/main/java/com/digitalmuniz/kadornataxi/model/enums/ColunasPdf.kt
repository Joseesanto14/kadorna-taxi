package com.digitalmuniz.kadornataxi.model.enums

import android.text.Layout
import com.digitalmuniz.kadornataxi.model.entities.Viagem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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
    DIA("Dia", 1.25f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return try {
                viagem.diaMes ?: "-"
            } catch (_: Exception) {
                viagem.data.orEmpty().ifBlank { "-" }
            }
        }
    },
    HORA("Hora", 1.15f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.hora.orEmpty().ifBlank { "-" }
        }
    },
    ORIGEM("Origem", 2.40f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.origem.orEmpty().ifBlank { "-" }
        }
    },
    DESTINO("Destino", 2.60f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.destino.orEmpty().ifBlank { "-" }
        }
    },
    DESCRICAO("Descrição", 4.10f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.descricao.orEmpty().ifBlank { "-" }
        }
    },
    SOLICITANTE("Solicitante", 2.40f, Layout.Alignment.ALIGN_NORMAL) {
        override fun extrairDado(viagem: Viagem): String {
            return viagem.solicitante.orEmpty().trim().ifBlank { "-" }
        }
    },
    KMS_RODADOS("Kms", 1.65f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return String.format(LOCALE_PT_BR, "%.1f km", viagem.kmsRodados)
        }
    },
    VALOR_KMS("Valor Kms", 2.15f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorKms.toDouble())
        }
    },
    VALOR_SERVICO("Valor Serv.", 2.15f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorServico.toDouble())
        }
    },
    HORAS_ESPERA("Espera (h)", 1.65f, Layout.Alignment.ALIGN_CENTER) {
        override fun extrairDado(viagem: Viagem): String {
            return String.format(LOCALE_PT_BR, "%.2f", viagem.horaEspera)
        }
    },
    VALOR_ESPERA("Valor Esp.", 2.10f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorHoraEspera.toDouble())
        }
    },
    VALOR_TOTAL("Valor Total", 2.30f, Layout.Alignment.ALIGN_OPPOSITE) {
        override fun extrairDado(viagem: Viagem): String {
            return formatarMoeda(viagem.valorTotal.toDouble())
        }
    },
    MOTORISTA("Motorista", 2.10f, Layout.Alignment.ALIGN_NORMAL) {
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
    }
}
