package com.digitalmuniz.kadornataxi.model.entities;

import android.content.Context;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.view.dto.MesViagens;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Viagem implements Serializable {
    private long id;
    private String origem;
    private String data;
    private String hora;
    private String destino;
    private String descricao;
    private float kmsRodados;
    private float valorKms;
    private float valorServico;
    private String motorista;
    private float horaEspera;
    private float valorHoraEspera;
    private String classificacao;
    private float valorTotal;
    private String solicitante = "";

    // --------------- construtores ---------------

    public Viagem() {}

    public Viagem(long id, String origem, String data, String hora, String destino, String descricao, float kmsRodados, float valorKms, float valorServico, String motorista, float horaEspera, float valorHoraEspera, String classificacao, float valorTotal) {
        this(id, origem, data, hora, destino, descricao, kmsRodados, valorKms, valorServico, motorista, horaEspera, valorHoraEspera, classificacao, valorTotal, "");
    }

    public Viagem(long id, String origem, String data, String hora, String destino, String descricao, float kmsRodados, float valorKms, float valorServico, String motorista, float horaEspera, float valorHoraEspera, String classificacao, float valorTotal, String solicitante) {
        this.id = id;
        this.origem = origem;
        this.data = data;
        this.hora = hora;
        this.destino = destino;
        this.descricao = descricao;
        this.kmsRodados = kmsRodados;
        this.valorKms = valorKms;
        this.valorServico = valorServico;
        this.motorista = motorista;
        this.horaEspera = horaEspera;
        this.valorHoraEspera = valorHoraEspera;
        this.classificacao = classificacao;
        this.valorTotal = valorTotal;
        this.solicitante = solicitante != null ? solicitante : "";
    }

    public Viagem(String origem, String data, String hora, String destino, String descricao, float kmsRodados, float horaEspera, String motorista, String cliente, Context context, float valorServico) {
        this(origem, data, hora, destino, descricao, kmsRodados, horaEspera, motorista, cliente, context, valorServico, "");
    }

    public Viagem(String origem, String data, String hora, String destino, String descricao, float kmsRodados, float horaEspera, String motorista, String cliente, Context context, float valorServico, String solicitante) {
        this.origem = origem;
        this.data = data;
        this.hora = hora;
        this.destino = destino;
        this.descricao = descricao;
        this.kmsRodados = kmsRodados;
        this.valorServico = valorServico;
        this.motorista = motorista;
        this.horaEspera = horaEspera;
        this.classificacao = cliente;
        this.solicitante = solicitante != null ? solicitante : "";

        Configuracao config = new ConfiguracaoDAO(context).getConfiguracao();
        valorKms = (kmsRodados * config.getValorKmRodado());
        valorHoraEspera = horaEspera * config.getValorHoraEspera();
        valorTotal = getValorKms() + getValorHoraEspera() + getValorServico();
    }

    // --------------- úteis ---------------

    public static String formatarDataISO8601(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        // Tenta o formato ISO (yyyy-MM-dd) primeiro
        try {
            LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
            return data;
        } catch (DateTimeParseException ignored) {}

        // Se falhar, tenta o formato brasileiro (dd/MM/yyyy)
        try {
            DateTimeFormatter formatterBR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate localDate = LocalDate.parse(data, formatterBR);
            return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // Se ambos falharem, retorna a string original ou loga um aviso
            return data;
        }
    }

    public String getDiaMes() {
        /*
        * Receive a Date in ISO 8601 format (YYYY-MM-DD) and return the day and month in the format DD/MM (Brazilian legibility)
         */

        String[] partes = getData().split("-");
        return partes[2] + "/" + partes[1];
    }

    public static List<MesViagens> gerarListaDeMeses(List<Viagem> todas) {
        Map<String, List<Viagem>> viagensPorMes = new LinkedHashMap<>();

        for(Viagem viagem : todas) {
            String mesAno = viagem.getData().substring(0, 7);

            if (viagem.isNaoComum()) {
                mesAno += "-" + viagem.getClassificacao();
            }

            viagensPorMes
                    .computeIfAbsent(mesAno, k -> new ArrayList<>())
                    .add(viagem);
        }

        List<MesViagens> listaMeses = new ArrayList<>();

        for (String mes : viagensPorMes.keySet()) {
            listaMeses.add(new MesViagens(mes, viagensPorMes.get(mes)));
        }
        return listaMeses;
    }

    public boolean isNaoComum() {
        return !getClassificacao().equals("Comum");
    }

    // --------------- getters e setters ---------------

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public float getKmsRodados() {
        return kmsRodados;
    }

    public void setKmsRodados(float kmsRodados) {
        this.kmsRodados = kmsRodados;
    }

    public float getValorServico() {
        return valorServico;
    }

    public void setValorServico(float valorServico) {
        this.valorServico = valorServico;
    }

    public float getValorKms() {
        return valorKms;
    }

    public void setValorKms(float valorKms) {
        this.valorKms = valorKms;
    }

    public String getMotorista() {
        return motorista;
    }

    public void setMotorista(String motorista) {
        this.motorista = motorista;
    }

    public float getHoraEspera() {
        return horaEspera;
    }

    public void setHoraEspera(float horaEspera) {
        this.horaEspera = horaEspera;
    }

    public float getValorHoraEspera() {
        return valorHoraEspera;
    }

    public void setValorHoraEspera(float valorHoraEspera) {
        this.valorHoraEspera = valorHoraEspera;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

    public float getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(float valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getSolicitante() {
        return solicitante != null ? solicitante : "";
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante != null ? solicitante : "";
    }
}


