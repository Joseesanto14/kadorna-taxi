package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.controller.RelatorioPdfGenerator;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.model.enums.ColunasPdf;
import com.digitalmuniz.kadornataxi.model.enums.ServicoItemPdf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RelatorioPdfGeneratorTest {

    private Context context;
    private Configuracao config;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        config = new Configuracao(
                1L, 3.0f, 30f, "Marcelo", "Quartzolit",
                "12.345.678/0001-90", "Kadorna Táxi", "Marcelo", "11999999999", "contato@kadorna.com"
        );
    }

    @Test
    public void colunasPdf_somaExatamente28CmEFormataZeroUmEMultiplosServicosSemEmojis() {
        assertTrue(ColunasPdf.DESTINO.ordinal() < ColunasPdf.SOLICITANTE.ordinal());
        assertTrue(ColunasPdf.SOLICITANTE.ordinal() < ColunasPdf.DESCRICAO.ordinal());

        float somaPt = 0f;
        for (ColunasPdf coluna : ColunasPdf.values()) {
            somaPt += coluna.getLarguraPt();
        }
        assertEquals(28.00f * 28.35f, somaPt, 0.05f);

        // 0 serviços
        Viagem semServicos = criarViagemBase("Sem servico", Collections.emptyList());
        assertEquals("-", ColunasPdf.SERVICOS.extrairDado(semServicos));
        assertTrue(ColunasPdf.Companion.extrairItensServicos(semServicos).isEmpty());

        // 1 serviço
        Viagem umServico = criarViagemBase("Um servico", Collections.singletonList(
                new Servico("Pedágio", 15f)
        ));
        assertEquals("Pedágio: R$ 15,00", ColunasPdf.SERVICOS.extrairDado(umServico));
        List<ServicoItemPdf> itensUm = ColunasPdf.Companion.extrairItensServicos(umServico);
        assertEquals(1, itensUm.size());
        assertEquals("Pedágio", itensUm.get(0).getDescricao());
        assertEquals("R$ 15,00", itensUm.get(0).getValorFormatado());
        assertFalse(itensUm.get(0).getEhTotal());

        // Múltiplos serviços
        Viagem multiServicos = criarViagemBase("Multi servicos", Arrays.asList(
                new Servico("Pedágio", 15f),
                new Servico("Estacionamento", 25f)
        ));
        String textoMulti = ColunasPdf.SERVICOS.extrairDado(multiServicos);
        assertTrue(textoMulti.contains("Pedágio: R$ 15,00"));
        assertTrue(textoMulti.contains("Estacionamento: R$ 25,00"));
        assertTrue(textoMulti.contains("Total serv.: R$ 40,00"));
        assertFalse(textoMulti.contains("- Pedágio"));

        List<ServicoItemPdf> itensMulti = ColunasPdf.Companion.extrairItensServicos(multiServicos);
        assertEquals(3, itensMulti.size());
        assertTrue(itensMulti.get(2).getEhTotal());
        assertEquals("Total serv.:", itensMulti.get(2).getDescricao());
        assertEquals("R$ 40,00", itensMulti.get(2).getValorFormatado());
    }

    @Test
    public void gerarPdf_comZeroUmEMultiplosServicos_renderizaTotaisCorretamente() {
        List<Viagem> viagens = Arrays.asList(
                criarViagemBase("Corrida A", Collections.emptyList()),
                criarViagemBase("Corrida B", Collections.singletonList(new Servico("Pedágio", 12f))),
                criarViagemBase("Corrida C", Arrays.asList(
                        new Servico("Pedágio", 15f),
                        new Servico("Balsa", 30f)
                ))
        );

        RelatorioPdfGenerator generator = new RelatorioPdfGenerator(context, viagens, config, "2026-09");
        RecordingPdfBackend backend = new RecordingPdfBackend();
        File pdf = generator.gerarArquivoPdf(backend);

        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
        assertEquals(1, backend.pagesStarted);
        assertTrue(backend.closed);
        assertTrue("Soma dos serviços nos totais deveria ser R$ 57,00",
                backend.drawnTexts.contains("R$ 57,00"));
        assertTrue(backend.drawnTexts.contains("Página 1 de 1"));
        assertTrue(pdf.delete() || !pdf.exists());
    }

    @Test
    public void gerarPdf_divisoriaVerticalInternaServicos_desenhadaApenasQuandoHaServicos() {
        float xDivisoriaInterna = calcularXDivisoriaInternaServicos();

        // Apenas viagens sem serviços: não deve desenhar divisória vertical interna na coluna SERVICOS
        Viagem semServicos = criarViagemBase("Sem servico", Collections.emptyList());
        RecordingPdfBackend backendSem = new RecordingPdfBackend();
        File pdfSem = new RelatorioPdfGenerator(context, Collections.singletonList(semServicos), config, "2026-09")
                .gerarArquivoPdf(backendSem);
        assertFalse(
                "Não deveria desenhar divisória vertical interna quando não há serviços",
                contemDivisoriaVerticalEm(backendSem.drawnLines, xDivisoriaInterna)
        );
        assertTrue(pdfSem.delete() || !pdfSem.exists());

        // Viagem com serviços: deve desenhar divisória vertical interna em 58% da coluna SERVICOS
        Viagem comServicos = criarViagemBase("Com servico", Arrays.asList(
                new Servico("Pedágio", 18f),
                new Servico("Estacionamento", 22f)
        ));
        RecordingPdfBackend backendCom = new RecordingPdfBackend();
        File pdfCom = new RelatorioPdfGenerator(context, Collections.singletonList(comServicos), config, "2026-09")
                .gerarArquivoPdf(backendCom);
        assertTrue(
                "Deveria desenhar divisória vertical interna na coluna SERVICOS quando há serviços",
                contemDivisoriaVerticalEm(backendCom.drawnLines, xDivisoriaInterna)
        );
        assertTrue(pdfCom.delete() || !pdfCom.exists());
    }

    @Test
    public void gerarPdf_servicoComDescricaoLongaEValorGrande_alinhaPareadoSemQuebrarRodape() {
        Viagem curto = criarViagemBase("Curto", Arrays.asList(
                new Servico("Pedágio", 15f),
                new Servico("Estacionamento", 25f)
        ));
        Viagem longo = criarViagemBase("Longo", Arrays.asList(
                new Servico("Pedágio rodoviário interestadual\ncom retorno e taxa administrativa\nde guincho", 99999.99f),
                new Servico("Estacionamento", 25f)
        ));

        RelatorioPdfGenerator genCurto = new RelatorioPdfGenerator(context, Collections.singletonList(curto), config, "2026-09");
        RelatorioPdfGenerator genLongo = new RelatorioPdfGenerator(context, Collections.singletonList(longo), config, "2026-09");

        List<RelatorioPdfGenerator.PageLayoutInfo> paginasCurto = genCurto.inspecionarPaginacao();
        List<RelatorioPdfGenerator.PageLayoutInfo> paginasLongo = genLongo.inspecionarPaginacao();

        assertEquals(1, paginasCurto.size());
        assertEquals(1, paginasLongo.size());
        assertTrue(paginasLongo.get(0).getYFinalConteudo() <= paginasLongo.get(0).getLimiteYConteudo());
        assertTrue(
                "Descrição longa multi-linha deveria aumentar a altura da linha pareada",
                paginasLongo.get(0).getYFinalConteudo() > paginasCurto.get(0).getYFinalConteudo()
        );

        RecordingPdfBackend backend = new RecordingPdfBackend();
        File pdf = genLongo.gerarArquivoPdf(backend);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
        assertTrue(backend.drawnTexts.contains("R$ 100.024,99"));
        assertTrue(pdf.delete() || !pdf.exists());
    }

    @Test
    public void viagemCom45Servicos_divideEmPaginasComContinuacaoSemTransbordarRodape() {
        validarPaginacaoComMuitosServicos(45, "R$ 450,00");
        validarPaginacaoComMuitosServicos(50, "R$ 500,00");
    }

    private void validarPaginacaoComMuitosServicos(int quantidade, String totalEsperado) {
        List<Servico> muitosServicos = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            muitosServicos.add(new Servico("Serviço extra item " + i, 10f));
        }
        Viagem viagemGigante = criarViagemBase("Viagem extensa", muitosServicos);

        RelatorioPdfGenerator generator = new RelatorioPdfGenerator(
                context,
                Collections.singletonList(viagemGigante),
                config,
                "2026-09-Quartzolit"
        );

        List<RelatorioPdfGenerator.PageLayoutInfo> paginas = generator.inspecionarPaginacao();
        assertTrue("Deveria quebrar em pelo menos 2 páginas", paginas.size() >= 2);

        boolean encontrouContinuacao = false;
        boolean encontrouTotalNaUltimaContinuacao = false;
        for (RelatorioPdfGenerator.PageLayoutInfo pagina : paginas) {
            assertTrue(
                    "Conteúdo ultrapassou limite da página " + pagina.getNumeroPagina()
                            + ": y=" + pagina.getYFinalConteudo() + " > limite=" + pagina.getLimiteYConteudo(),
                    pagina.getYFinalConteudo() <= pagina.getLimiteYConteudo()
            );
            List<String> descs = pagina.getDescricoesLinhas();
            List<String> servs = pagina.getServicosLinhas();
            for (int i = 0; i < descs.size(); i++) {
                if ("Continuação".equals(descs.get(i))) {
                    encontrouContinuacao = true;
                    if (servs.get(i).contains("Total serv.: " + totalEsperado)) {
                        encontrouTotalNaUltimaContinuacao = true;
                    }
                }
            }
        }
        assertTrue(encontrouContinuacao);
        assertTrue(encontrouTotalNaUltimaContinuacao);

        RecordingPdfBackend backend = new RecordingPdfBackend();
        File pdf = generator.gerarArquivoPdf(backend);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
        assertFalse(pdf.getName().isEmpty());
        assertEquals(paginas.size(), backend.pagesStarted);
        assertTrue(backend.drawnTexts.contains(totalEsperado));
        assertTrue(contemDivisoriaVerticalEm(backend.drawnLines, calcularXDivisoriaInternaServicos()));
        assertTrue(pdf.delete() || !pdf.exists());
    }

    @Test
    public void multiServicos_desenhaDivisoriaHorizontalEntreCadaItemServico() {
        Viagem umServico = criarViagemBase("Viagem 1 serv", Collections.singletonList(
                new Servico("Pedagio", 15f)
        ));
        RelatorioPdfGenerator genUm = new RelatorioPdfGenerator(
                context,
                Collections.singletonList(umServico),
                config,
                "2026-09-Quartzolit"
        );
        RecordingPdfBackend backendUmServico = new RecordingPdfBackend();
        File pdfUm = genUm.gerarArquivoPdf(backendUmServico);

        Viagem tresServicos = criarViagemBase("Viagem 3 serv", Arrays.asList(
                new Servico("Pedagio", 15f),
                new Servico("Estacionamento", 20f),
                new Servico("Ajudante", 35f)
        ));
        RelatorioPdfGenerator genTres = new RelatorioPdfGenerator(
                context,
                Collections.singletonList(tresServicos),
                config,
                "2026-09-Quartzolit"
        );
        RecordingPdfBackend backendTresServicos = new RecordingPdfBackend();
        File pdfTres = genTres.gerarArquivoPdf(backendTresServicos);

        assertEquals(0, contarDivisoriasHorizontaisInternasServicos(backendUmServico.drawnLines));
        assertEquals(3, contarDivisoriasHorizontaisInternasServicos(backendTresServicos.drawnLines));

        assertTrue(pdfUm.delete() || !pdfUm.exists());
        assertTrue(pdfTres.delete() || !pdfTres.exists());
    }

    private int contarDivisoriasHorizontaisInternasServicos(List<DrawnLine> lines) {
        float xInicio = 24f;
        for (ColunasPdf coluna : ColunasPdf.values()) {
            if (coluna == ColunasPdf.SERVICOS) {
                break;
            }
            xInicio += coluna.getLarguraPt();
        }
        float xFim = xInicio + ColunasPdf.SERVICOS.getLarguraPt();

        int count = 0;
        for (DrawnLine line : lines) {
            boolean ehHorizontal = Math.abs(line.startY - line.stopY) < 0.1f;
            boolean cobreColunaServicos = Math.abs(line.startX - xInicio) < 0.5f
                    && Math.abs(line.stopX - xFim) < 0.5f;
            if (ehHorizontal && cobreColunaServicos) {
                count++;
            }
        }
        return count;
    }

    private float calcularXDivisoriaInternaServicos() {
        float x = 24f;
        for (ColunasPdf coluna : ColunasPdf.values()) {
            if (coluna == ColunasPdf.SERVICOS) {
                return x + (coluna.getLarguraPt() * 0.58f);
            }
            x += coluna.getLarguraPt();
        }
        throw new AssertionError("Coluna SERVICOS não encontrada");
    }

    private boolean contemDivisoriaVerticalEm(List<DrawnLine> lines, float xEsperado) {
        for (DrawnLine line : lines) {
            boolean ehVertical = Math.abs(line.startX - line.stopX) < 0.1f
                    && Math.abs(line.startY - line.stopY) > 1.0f;
            if (ehVertical && Math.abs(line.startX - xEsperado) < 0.5f) {
                return true;
            }
        }
        return false;
    }

    private Viagem criarViagemBase(String descricao, List<Servico> servicos) {
        Viagem v = new Viagem(
                1L, "Itu", "2026-09-10", "08:30", "São Paulo", descricao,
                20f, 60f, 0f, "Marcelo", 0f, 0f, "Quartzolit", 60f, "Marcos"
        );
        v.setServicos(servicos);
        return v;
    }

    private static class DrawnLine {
        final float startX;
        final float startY;
        final float stopX;
        final float stopY;

        DrawnLine(float startX, float startY, float stopX, float stopY) {
            this.startX = startX;
            this.startY = startY;
            this.stopX = stopX;
            this.stopY = stopY;
        }
    }

    private static class RecordingPdfBackend implements RelatorioPdfGenerator.PdfBackend {
        int pagesStarted = 0;
        boolean closed = false;
        final List<String> drawnTexts = new ArrayList<>();
        final List<DrawnLine> drawnLines = new ArrayList<>();
        private Bitmap currentBitmap;

        @NonNull
        @Override
        public Canvas startPage(int width, int height, int pageNumber) {
            pagesStarted++;
            currentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            return new Canvas(currentBitmap) {
                @Override
                public void drawText(@NonNull String text, float x, float y, @NonNull Paint paint) {
                    drawnTexts.add(text);
                    super.drawText(text, x, y, paint);
                }

                @Override
                public void drawLine(float startX, float startY, float stopX, float stopY, @NonNull Paint paint) {
                    drawnLines.add(new DrawnLine(startX, startY, stopX, stopY));
                    super.drawLine(startX, startY, stopX, stopY, paint);
                }
            };
        }

        @Override
        public void finishPage() {
            if (currentBitmap != null) {
                currentBitmap.recycle();
                currentBitmap = null;
            }
        }

        @Override
        public void writeTo(@NonNull FileOutputStream outputStream) {
            try {
                outputStream.write(("%PDF-1.4\nPAGES=" + pagesStarted).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
