package com.digitalmuniz.kadornataxi.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import java.util.ArrayList;
import java.util.List;

public class ViagemDAO {
    private SQLiteDatabase database;
    private final DbHelper dbHelper;

    private void open() {
        database = dbHelper.getWritableDatabase();
    }

    private void close() {
        dbHelper.close();
    }

    public ViagemDAO(Context context) {
        dbHelper = new DbHelper(context);
    }

    // CREATE - Inserir um novo registro (viagem)

    public void inserirNoDatabase(Viagem viagem, Context context) {
        open();

        Log.d("ViagemDAO", "Viagem inserida com o ID: " + insert(viagem));
        Toast.makeText(context, "Viagem criada com sucesso!", Toast.LENGTH_SHORT).show();

        close();
    }

    public int inserirLote(List<Viagem> viagens, boolean substituirExistentes) {
        open();
        int inseridos = 0;
        database.beginTransaction();
        try {
            if (substituirExistentes) {
                database.delete(DbHelper.Viagem.NOME_TABELA, null, null);
            }
            for (Viagem viagem : viagens) {
                if (!substituirExistentes && existeViagemDuplicada(viagem)) {
                    continue;
                }
                insert(viagem);
                inseridos++;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            close();
        }
        return inseridos;
    }

    private boolean existeViagemDuplicada(Viagem viagem) {
        String dataFormatada = Viagem.formatarDataISO8601(viagem.getData());
        String selecao = DbHelper.Viagem.DATA + " = ? AND " +
                DbHelper.Viagem.HORA + " = ? AND " +
                DbHelper.Viagem.DESTINO + " = ? AND " +
                DbHelper.Viagem.VALOR_TOTAL + " = ?";
        String[] args = new String[]{
                dataFormatada != null ? dataFormatada : "",
                viagem.getHora() != null ? viagem.getHora() : "",
                viagem.getDestino() != null ? viagem.getDestino() : "",
                String.valueOf(viagem.getValorTotal())
        };

        try (Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                new String[]{DbHelper.Viagem.ID},
                selecao, args, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    private long insert(Viagem viagem) {
        ContentValues values = new ContentValues();
        
        values.put(DbHelper.Viagem.ORIGEM, 
                viagem.getOrigem());
        values.put(DbHelper.Viagem.DATA, 
                Viagem.formatarDataISO8601(viagem.getData()));
        values.put(DbHelper.Viagem.HORA, 
                viagem.getHora());
        values.put(DbHelper.Viagem.DESTINO, 
                viagem.getDestino());
        values.put(DbHelper.Viagem.DESCRICAO, 
                viagem.getDescricao());
        values.put(DbHelper.Viagem.KMS_RODADOS, 
                viagem.getKmsRodados());
        values.put(DbHelper.Viagem.VALOR_KM,
                viagem.getValorKms());
        values.put(DbHelper.Viagem.VALOR_SERVICO,
                viagem.getValorServico());
        values.put(DbHelper.Viagem.MOTORISTA, 
                viagem.getMotorista());
        values.put(DbHelper.Viagem.HORA_ESPERA, 
                viagem.getHoraEspera());
        values.put(DbHelper.Viagem.VALOR_HORA_ESPERA, 
                viagem.getValorHoraEspera());
        values.put(DbHelper.Viagem.CLASSIFICACAO,
                viagem.getClassificacao());
        values.put(DbHelper.Viagem.VALOR_TOTAL,
                viagem.getValorTotal());

        return database.insert(DbHelper.Viagem.NOME_TABELA, null, values);
    }

    // READ - Listar todos os registros (viagens)

    public List<Viagem> getAllOrdenadoPorDataDesc() {
        open();

        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                null, null, null,
                null, null, DbHelper.Viagem.DATA + " DESC, " + DbHelper.Viagem.HORA + " DESC");

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorToViagem(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();

        close();

        return lista;
    }

    public List<Viagem> getAll() {
        open();

        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                null, null, null,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorToViagem(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();

        close();

        return lista;
    }

    // UPTADE - atualizar os resgistros (viagem)

    public int update(Viagem viagem) {
        open();

        ContentValues values = new ContentValues();
        values.put(DbHelper.Viagem.ORIGEM, viagem.getOrigem());
        values.put(DbHelper.Viagem.DATA, Viagem.formatarDataISO8601(viagem.getData()));
        values.put(DbHelper.Viagem.HORA, viagem.getHora());
        values.put(DbHelper.Viagem.DESTINO, viagem.getDestino());
        values.put(DbHelper.Viagem.DESCRICAO, viagem.getDescricao());
        values.put(DbHelper.Viagem.KMS_RODADOS, viagem.getKmsRodados());
        values.put(DbHelper.Viagem.VALOR_KM, viagem.getValorKms());
        values.put(DbHelper.Viagem.VALOR_SERVICO, viagem.getValorServico());
        values.put(DbHelper.Viagem.MOTORISTA, viagem.getMotorista());
        values.put(DbHelper.Viagem.HORA_ESPERA, viagem.getHoraEspera());
        values.put(DbHelper.Viagem.VALOR_HORA_ESPERA, viagem.getValorHoraEspera());
        values.put(DbHelper.Viagem.CLASSIFICACAO, viagem.getClassificacao());
        values.put(DbHelper.Viagem.VALOR_TOTAL, viagem.getValorTotal());

        int linhasAfetadas = database.update(DbHelper.Viagem.NOME_TABELA, values,
                DbHelper.Viagem.ID + " = ?", new String[]{String.valueOf(viagem.getId())});

        close();
        return linhasAfetadas;
    }

    // DELETE - remover um registro do banco de dados

    public int delete(long id) {
        open();
        
        int linhasAfetadas = database.delete(DbHelper.Viagem.NOME_TABELA,
                DbHelper.Viagem.ID + " = ?", new String[]{String.valueOf(id)});

        close();

        return linhasAfetadas;
    }

    // DELETAR TODOS - remover todos os registros do banco de dados

    public int deleteAll() {
        open();
        int linhasAfetadas = database.delete(DbHelper.Viagem.NOME_TABELA,
                null, null);
        close();

        return linhasAfetadas;
    }

    // CONTAR TODOS OS REGISTROS

    public int getCount() {
        open();
        int count = 0;
        try (Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + DbHelper.Viagem.NOME_TABELA, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        }
        close();
        return count;
    }

    // BUSCAR UM REGISTRO USANDO OUTROS DADOS

    public Viagem getViagemById(long id) {
        open();
        close();
        return null;
    }

    public List<Viagem> getViagemByLocal(String local) {
        open();
        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                null,
                DbHelper.Viagem.ORIGEM + " = ? OR " +
                        DbHelper.Viagem.DESTINO + " = ?",
                new String[]{local, local}, null, null, null);

        while (cursor.moveToNext()) {
            lista.add(cursorToViagem(cursor));
        }

        cursor.close();
        close();
        return lista;
    }

    public List<Viagem> getViagemByDestino(String destino) {
        open();
        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                null, DbHelper.Viagem.DESTINO +
                        " = ?", new String[]{destino}, null,
                null, null);

        while (cursor.moveToNext()) {
            lista.add(cursorToViagem(cursor));
        }

        cursor.close();
        close();
        return lista;
    }

    public List<Viagem> getViagemByPeriodo(String anoMes) {
        open();
        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(
                DbHelper.Viagem.NOME_TABELA,
                null,
                DbHelper.Viagem.DATA + " LIKE ?",
                new String[]{anoMes + "%"},
                null, null,
                DbHelper.Viagem.DATA + " ASC");

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorToViagem(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        close();
        return lista;
    }

    public List<Viagem> getViagensByIntervalo(String dataInicio, String dataFim) {
        List<Viagem> lista = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.Viagem.NOME_TABELA,
                null, DbHelper.Viagem.DATA + " BETWEEN ? AND ?",
                new String[]{dataInicio, dataFim}, null, null, null);

        while (cursor.moveToNext()) {
            lista.add(cursorToViagem(cursor));
        }

        cursor.close();

        return lista;
    }

    private Viagem cursorToViagem(Cursor cursor) {

        return new Viagem(
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.Viagem.ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.ORIGEM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DATA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.HORA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DESTINO)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DESCRICAO)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.KMS_RODADOS)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_KM)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_SERVICO)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.MOTORISTA)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.HORA_ESPERA)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_HORA_ESPERA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.CLASSIFICACAO)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_TOTAL))
        );
    }
}
