package com.digitalmuniz.kadornataxi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.CustoDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Custo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CustoDAOTest {

    private DbHelper dbHelper;
    private CustoDAO custoDAO;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context, null);
        custoDAO = new CustoDAO(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void crudBasico_inserirBuscarAtualizarDeletar() {
        Custo novo = new Custo("Combustível", 180.50, "15/03/2026");
        long id = custoDAO.inserir(novo);
        assertTrue(id > 0L);
        assertEquals(id, novo.getId());
        assertEquals("2026-03-15", novo.getData());

        Custo carregado = custoDAO.getById(id);
        assertNotNull(carregado);
        assertEquals("Combustível", carregado.getDescricao());
        assertEquals(180.50, carregado.getValor(), 0.001);
        assertEquals("2026-03-15", carregado.getData());

        carregado.setDescricao("Combustível Aditivado");
        carregado.setValor(210.00);
        carregado.setData("2026-03-16");
        assertEquals(1, custoDAO.atualizar(carregado));

        Custo atualizado = custoDAO.getById(id);
        assertNotNull(atualizado);
        assertEquals("Combustível Aditivado", atualizado.getDescricao());
        assertEquals(210.00, atualizado.getValor(), 0.001);
        assertEquals("2026-03-16", atualizado.getData());

        assertEquals(1, custoDAO.deletar(id));
        assertNull(custoDAO.getById(id));
    }

    @Test
    public void getAllEBuscarPorPeriodo_ordenamPorDataDescIdDesc() {
        Custo c1 = new Custo("Pedágio", 20.0, "2026-03-01");
        Custo c2 = new Custo("Lavagem", 45.0, "2026-03-10");
        Custo c3 = new Custo("Combustível", 150.0, "2026-03-10");
        Custo c4 = new Custo("Seguro", 300.0, "2026-04-01");

        custoDAO.inserir(c1);
        custoDAO.inserir(c2);
        custoDAO.inserir(c3);
        custoDAO.inserir(c4);

        List<Custo> todos = custoDAO.getAll();
        assertEquals(4, todos.size());
        assertEquals("Seguro", todos.get(0).getDescricao());
        assertEquals("Combustível", todos.get(1).getDescricao());
        assertEquals("Lavagem", todos.get(2).getDescricao());
        assertEquals("Pedágio", todos.get(3).getDescricao());

        List<Custo> marco = custoDAO.buscarPorPeriodo("2026-03-01", "2026-04-01");
        assertEquals(3, marco.size());
        assertEquals("Combustível", marco.get(0).getDescricao());
        assertEquals("Pedágio", marco.get(2).getDescricao());
    }

    @Test
    public void inserirLote_deduplicaPorDataDescricaoTrimmedCaseInsensitiveEValor() {
        Custo inicial = new Custo("Combustível", 150.00, "2026-03-05");
        custoDAO.inserir(inicial);

        List<Custo> lote = Arrays.asList(
                new Custo("  combustível  ", 150.002, "2026-03-05"),
                new Custo("Combustível", 155.00, "2026-03-05"),
                new Custo("Pedágio", 18.50, "2026-03-06")
        );

        int inseridos = custoDAO.inserirLote(lote, false);
        assertEquals(2, inseridos);
        assertEquals(3, custoDAO.getAll().size());

        int substituidos = custoDAO.inserirLote(Arrays.asList(
                new Custo("Manutenção", 420.0, "2026-03-20")
        ), true);
        assertEquals(1, substituidos);
        List<Custo> restantes = custoDAO.getAll();
        assertEquals(1, restantes.size());
        assertEquals("Manutenção", restantes.get(0).getDescricao());
    }
}
