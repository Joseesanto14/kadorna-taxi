package com.digitalmuniz.kadornataxi.data.mock;

import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Locale;

public class ViagensMock {

    private static final String[] CIDADES_PEQUENAS = {"Itu", "Jaú", "Ubá", "Ijuí", "Crato", "Leme", "Araxá"};
    private static final String[] CIDADES_MEDIAS = {"Sorocaba", "São Paulo", "Curitiba", "Campinas", "Joinville", "Uberlândia", "Bauru"};
    private static final String[] CIDADES_GRANDES = {"Itaquaquecetuba", "Itapecerica da Serra", "São José dos Campos", "Santana de Parnaíba", "Ribeirão das Neves", "Governador Valadares"};

    private static final String[] DESCRICOES = {"Viagem de negócios", "Transporte de passageiro", "Consulta médica", "Viagem particular", "Trecho de rotina", "Entrega de documentos"};
    private static final String[] MOTORISTAS = {"Marcelo", "Kadorna", "João", "Ricardo", "Felipe"};
    private static final String[] CLASSIFICACOES = {"Comum", "Quartzolit", "Prefeitura Municipal", "Trecho Especial", "Cliente VIP"};
    private static final String[] SOLICITANTES = {"Marcos (Encarregado)", "Carlos", "Fernanda (RH)", "Roberto", ""};

    /**
     * Gera uma lista de viagens fictícias para teste.
     * @param quantidade Número de viagens a serem geradas.
     * @return List de objetos Viagem populados aleatoriamente.
     */
    public static List<Viagem> gerarViagens(int quantidade) {
        List<Viagem> lista = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < quantidade; i++) {
            // Sorteia cidades garantindo que origem e destino sejam diferentes
            String origem = sortearCidade(random);
            String destino = sortearCidade(random);
            while (origem.equals(destino)) {
                destino = sortearCidade(random);
            }

            // Gera data (YYYY-MM-DD) e hora (HH:mm)
            String data = String.format(Locale.getDefault(), "2026-%02d-%02d",
                    random.nextInt(12) + 1, random.nextInt(28) + 1);
            String hora = String.format(Locale.getDefault(), "%02d:%02d", 
                    random.nextInt(24), random.nextInt(60));
            
            String descricao = DESCRICOES[random.nextInt(DESCRICOES.length)];
            
            // Atributos numéricos
            float kmsRodados = 2.0f + random.nextFloat() * 80.0f; // 2 a 82 km
            float valorKms = kmsRodados * 2.5f; // Simulando R$ 2,50 por KM
            float valorServico = random.nextBoolean() ? 0f : (15 + random.nextInt(40)); // Serviço extra opcional
            
            float horaEspera = random.nextFloat() * 1.5f; // 0 a 1.5 horas
            float valorHoraEspera = horaEspera * 30.0f; // R$ 30,00 a hora
            
            String motorista = MOTORISTAS[random.nextInt(MOTORISTAS.length)];
            String classificacao = CLASSIFICACOES[random.nextInt(CLASSIFICACOES.length)];
            String solicitante = SOLICITANTES[random.nextInt(SOLICITANTES.length)];
            
            float valorTotal = valorKms + valorHoraEspera + valorServico;

            lista.add(new Viagem(
                    i + 1, // ID incremental
                    origem,
                    data,
                    hora,
                    destino,
                    descricao,
                    kmsRodados,
                    valorKms,
                    valorServico,
                    motorista,
                    horaEspera,
                    valorHoraEspera,
                    classificacao,
                    valorTotal,
                    solicitante
            ));
        }
        return lista;
    }

    private static String sortearCidade(Random random) {
        int categoria = random.nextInt(3);
        switch (categoria) {
            case 0: return CIDADES_PEQUENAS[random.nextInt(CIDADES_PEQUENAS.length)];
            case 1: return CIDADES_MEDIAS[random.nextInt(CIDADES_MEDIAS.length)];
            default: return CIDADES_GRANDES[random.nextInt(CIDADES_GRANDES.length)];
        }
    }
}
