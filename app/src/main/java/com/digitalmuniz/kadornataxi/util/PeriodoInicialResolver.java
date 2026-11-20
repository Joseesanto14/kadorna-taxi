package com.digitalmuniz.kadornataxi.util;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class PeriodoInicialResolver {

    public static class Resultado {
        private final String anoSelecionado;
        private final String mesSelecionado;
        private final List<String> mesesDoAno;

        public Resultado(String anoSelecionado, String mesSelecionado, List<String> mesesDoAno) {
            this.anoSelecionado = anoSelecionado;
            this.mesSelecionado = mesSelecionado;
            this.mesesDoAno = mesesDoAno != null
                    ? Collections.unmodifiableList(mesesDoAno)
                    : Collections.emptyList();
        }

        public boolean isVazio() {
            return anoSelecionado == null;
        }

        public String getAnoSelecionado() {
            return anoSelecionado;
        }

        public String getMesSelecionado() {
            return mesSelecionado;
        }

        public List<String> getMesesDoAno() {
            return mesesDoAno;
        }
    }

    private PeriodoInicialResolver() {
    }

    public static Resultado resolver(
            LocalDate dataReferencia,
            List<String> anosDisponiveis,
            Function<String, List<String>> provedorMesesPorAno
    ) {
        if (anosDisponiveis == null || anosDisponiveis.isEmpty() || provedorMesesPorAno == null) {
            return new Resultado(null, null, Collections.emptyList());
        }

        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();
        String anoAtual = String.format(Locale.US, "%04d", ref.getYear());
        String mesAtual = String.format(Locale.US, "%02d", ref.getMonthValue());

        if (anosDisponiveis.contains(anoAtual)) {
            List<String> mesesAnoAtual = provedorMesesPorAno.apply(anoAtual);
            if (mesesAnoAtual != null && !mesesAnoAtual.isEmpty()) {
                String mesEscolhido = mesesAnoAtual.contains(mesAtual)
                        ? mesAtual
                        : mesesAnoAtual.get(mesesAnoAtual.size() - 1);
                return new Resultado(anoAtual, mesEscolhido, mesesAnoAtual);
            }
        }

        for (String ano : anosDisponiveis) {
            List<String> meses = provedorMesesPorAno.apply(ano);
            if (meses != null && !meses.isEmpty()) {
                return new Resultado(ano, meses.get(meses.size() - 1), meses);
            }
        }

        return new Resultado(null, null, Collections.emptyList());
    }
}
