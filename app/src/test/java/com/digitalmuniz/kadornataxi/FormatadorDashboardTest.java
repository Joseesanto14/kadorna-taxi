package com.digitalmuniz.kadornataxi;

import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormatadorDashboardTest {

    @Test
    public void formatarMoeda_aplicaPadraoBrasileiroESinal() {
        assertEquals("R$ 0,00", FormatadorDashboard.formatarMoeda(0.0));
        assertEquals("R$ 0,00", FormatadorDashboard.formatarMoeda(-0.001));
        assertEquals("R$ 1.234,50", FormatadorDashboard.formatarMoeda(1234.5));
        assertEquals("-R$ 50,25", FormatadorDashboard.formatarMoeda(-50.25));
    }

    @Test
    public void formatarKmsEReaisPorKm_formataComPrecisaoCorreta() {
        assertEquals("0,0 km", FormatadorDashboard.formatarKms(0.0));
        assertEquals("1.234,5 km", FormatadorDashboard.formatarKms(1234.5));
        assertEquals("R$ 2,50/km", FormatadorDashboard.formatarReaisPorKm(2.5));
    }

    @Test
    public void formatarDuracaoEspera_converteHorasDecimaisParaHorasEMinutos() {
        assertEquals("0h 00m", FormatadorDashboard.formatarDuracaoEspera(0.0));
        assertEquals("1h 30m", FormatadorDashboard.formatarDuracaoEspera(1.5));
        assertEquals("2h 05m", FormatadorDashboard.formatarDuracaoEspera(2.0 + (5.0 / 60.0)));
    }

    @Test
    public void formatarVariacaoPercentual_cobrePositivoNegativoZeroENulo() {
        assertEquals("+12,4%", FormatadorDashboard.formatarVariacaoPercentual(12.4));
        assertEquals("-5,0%", FormatadorDashboard.formatarVariacaoPercentual(-5.0));
        assertEquals("0,0%", FormatadorDashboard.formatarVariacaoPercentual(0.0));
        assertEquals("0,0%", FormatadorDashboard.formatarVariacaoPercentual(-0.01));
        assertEquals("Sem base anterior", FormatadorDashboard.formatarVariacaoPercentual(null));
    }

    @Test
    public void abreviarParaGrafico_abreviaMilharesEMilhoesComSeguranca() {
        assertEquals("0", FormatadorDashboard.abreviarParaGrafico(0.0));
        assertEquals("850", FormatadorDashboard.abreviarParaGrafico(850.0));
        assertEquals("1k", FormatadorDashboard.abreviarParaGrafico(1000.0));
        assertEquals("1,2k", FormatadorDashboard.abreviarParaGrafico(1200.0));
        assertEquals("1,5M", FormatadorDashboard.abreviarParaGrafico(1_500_000.0));
    }

    @Test
    public void mesesPTBR_retornaSiglasENomesCorretos() {
        assertEquals("Jan", FormatadorDashboard.getSiglaMes("01"));
        assertEquals("Dez", FormatadorDashboard.getSiglaMes("12"));
        assertEquals("Março", FormatadorDashboard.getNomeMes("03"));
        assertEquals("Março 2026", FormatadorDashboard.formatarPeriodoTitulo("2026", "03"));
        assertEquals("Ano 2026", FormatadorDashboard.formatarPeriodoTitulo("2026", null));
    }
}
