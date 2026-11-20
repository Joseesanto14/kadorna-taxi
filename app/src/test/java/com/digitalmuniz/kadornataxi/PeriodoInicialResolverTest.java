package com.digitalmuniz.kadornataxi;

import com.digitalmuniz.kadornataxi.util.PeriodoInicialResolver;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PeriodoInicialResolverTest {

    @Test
    public void resolver_priorizaAnoEMesAtuaisQuandoTemDados() {
        LocalDate referencia = LocalDate.of(2026, 4, 15);
        List<String> anos = Arrays.asList("2026", "2025");
        Map<String, List<String>> meses = new HashMap<>();
        meses.put("2026", Arrays.asList("01", "03", "04", "05"));
        meses.put("2025", Arrays.asList("11", "12"));

        PeriodoInicialResolver.Resultado resultado =
                PeriodoInicialResolver.resolver(referencia, anos, meses::get);

        assertFalse(resultado.isVazio());
        assertEquals("2026", resultado.getAnoSelecionado());
        assertEquals("04", resultado.getMesSelecionado());
        assertEquals(Arrays.asList("01", "03", "04", "05"), resultado.getMesesDoAno());
    }

    @Test
    public void resolver_quandoAnoAtualExisteSemMesAtual_escolheMesMaisRecenteDoAnoAtual() {
        LocalDate referencia = LocalDate.of(2026, 4, 15);
        List<String> anos = Arrays.asList("2026", "2025");
        Map<String, List<String>> meses = new HashMap<>();
        meses.put("2026", Arrays.asList("01", "02", "03"));
        meses.put("2025", Arrays.asList("04", "12"));

        PeriodoInicialResolver.Resultado resultado =
                PeriodoInicialResolver.resolver(referencia, anos, meses::get);

        assertFalse(resultado.isVazio());
        assertEquals("2026", resultado.getAnoSelecionado());
        assertEquals("03", resultado.getMesSelecionado());
    }

    @Test
    public void resolver_quandoAnoAtualNaoTemDados_escolheAnoMaisRecenteESeuUltimoMes() {
        LocalDate referencia = LocalDate.of(2026, 4, 15);
        List<String> anos = Arrays.asList("2025", "2024");
        Map<String, List<String>> meses = new HashMap<>();
        meses.put("2025", Arrays.asList("02", "07", "11"));
        meses.put("2024", Arrays.asList("01", "12"));

        PeriodoInicialResolver.Resultado resultado =
                PeriodoInicialResolver.resolver(referencia, anos, meses::get);

        assertFalse(resultado.isVazio());
        assertEquals("2025", resultado.getAnoSelecionado());
        assertEquals("11", resultado.getMesSelecionado());
        assertEquals(Arrays.asList("02", "07", "11"), resultado.getMesesDoAno());
    }

    @Test
    public void resolver_quandoBancoVazio_retornaResultadoVazio() {
        LocalDate referencia = LocalDate.of(2026, 4, 15);

        PeriodoInicialResolver.Resultado resultado =
                PeriodoInicialResolver.resolver(referencia, Collections.emptyList(), ano -> Collections.emptyList());

        assertTrue(resultado.isVazio());
        assertNull(resultado.getAnoSelecionado());
        assertNull(resultado.getMesSelecionado());
        assertTrue(resultado.getMesesDoAno().isEmpty());
    }
}
