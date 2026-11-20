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
        float somaPt = 0f;
        for (ColunasPdf coluna : ColunasPdf.values()) {
            somaPt += coluna.getLarguraPt();
        }
        assertEquals(28.00f * 28.35f, somaPt, 0.05f);

        // 0 serviços
        Viagem semServicos = criarViagemBase("Sem servico", Collections.emptyList());
        assertEquals("-", ColunasPdf.SERVICOS.extrairDado(semServicos));

        // 1 serviço
        Viagem umServico = criarViagemBase("Um servico", Collections.singletonList(
                new Servico("Pedágio", 15f)
        ));
        assertEquals("- Pedágio: R$ 15,00", ColunasPdf.SERVICOS.extrairDado(umServico));

        // Múltiplos serviços
        Viagem multiServicos = criarViagemBase("Multi servicos", Arrays.asList(
                new Servico("Pedágio", 15f),
                new Servico("Estacionamento", 25f)
        ));
        String textoMulti = ColunasPdf.SERVICOS.extrairDado(multiServicos);
        assertTrue(textoMulti.contains("- Pedágio: R$ 15,00"));
        assertTrue(textoMulti.contains("- Estacionamento: R$ 25,00"));
        assertTrue(textoMulti.contains("Total serv.: R$ 40,00"));
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
    public void viagemCom45Servicos_divideEmPaginasComContinuacaoSemTransbordarRodape() {
        List<Servico> muitosServicos = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
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
        boolean encontrouTotalServicos = false;
        for (RelatorioPdfGenerator.PageLayoutInfo pagina : paginas) {
            assertTrue(
                    "Conteúdo ultrapassou limite da página " + pagina.getNumeroPagina()
                            + ": y=" + pagina.getYFinalConteudo() + " > limite=" + pagina.getLimiteYConteudo(),
                    pagina.getYFinalConteudo() <= pagina.getLimiteYConteudo()
            );
            if (pagina.getDescricoesLinhas().contains("Continuação")) {
                encontrouContinuacao = true;
            }
            for (String s : pagina.getServicosLinhas()) {
                if (s.contains("Total serv.: R$ 450,00")) {
                    encontrouTotalServicos = true;
                }
            }
        }
        assertTrue(encontrouContinuacao);
        assertTrue(encontrouTotalServicos);

        RecordingPdfBackend backend = new RecordingPdfBackend();
        File pdf = generator.gerarArquivoPdf(backend);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
        assertFalse(pdf.getName().isEmpty());
        assertEquals(paginas.size(), backend.pagesStarted);
        assertTrue(backend.drawnTexts.contains("R$ 450,00"));
        assertTrue(pdf.delete() || !pdf.exists());
    }

    private Viagem criarViagemBase(String descricao, List<Servico> servicos) {
        Viagem v = new Viagem(
                1L, "Itu", "2026-09-10", "08:30", "São Paulo", descricao,
                20f, 60f, 0f, "Marcelo", 0f, 0f, "Quartzolit", 60f, "Marcos"
        );
        v.setServicos(servicos);
        return v;
    }

    private static class RecordingPdfBackend implements RelatorioPdfGenerator.PdfBackend {
        int pagesStarted = 0;
        boolean closed = false;
        final List<String> drawnTexts = new ArrayList<>();
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
