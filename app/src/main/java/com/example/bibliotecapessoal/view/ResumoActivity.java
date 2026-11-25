package com.example.bibliotecapessoal.view;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bibliotecapessoal.R;
import com.example.bibliotecapessoal.persistencia.ILivroDAO;
import com.example.bibliotecapessoal.persistencia.LivroDAO;
import com.example.bibliotecapessoal.util.NavegacaoMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * VIEW — Tela "Resumo da Biblioteca".
 *
 * Mostra:
 *  - Um anel de progresso com o PERCENTUAL de livros lidos;
 *  - Três números em cartões: Total de livros, Livros lidos (verde) e Não lidos (terracota).
 * Os números são calculados direto no banco com SELECT COUNT(*) (ver LivroDAO.contar);
 * o percentual é calculado aqui mesmo a partir dessas contagens.
 */
public class ResumoActivity extends AppCompatActivity {

    private ILivroDAO dao;
    private TextView tvTotal;
    private TextView tvLidos;
    private TextView tvNaoLidos;
    private CircularProgressIndicator progressoLeitura; // Anel com o % de lidos
    private TextView tvPercentual;                      // Texto "62%" no centro do anel
    private TextView tvDescricaoProgresso;              // "X de Y livros lidos"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ícones da barra de status em branco, sobre a barra superior verde-petróleo
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_resumo);

        // Espaçamento para o conteúdo não ficar embaixo da barra de status
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, 0);
            return insets;
        });

        dao = new LivroDAO(this);

        // Componentes onde os números serão exibidos
        tvTotal = findViewById(R.id.tvTotal);
        tvLidos = findViewById(R.id.tvLidos);
        tvNaoLidos = findViewById(R.id.tvNaoLidos);
        progressoLeitura = findViewById(R.id.progressoLeitura);
        tvPercentual = findViewById(R.id.tvPercentual);
        tvDescricaoProgresso = findViewById(R.id.tvDescricaoProgresso);

        // Menu inferior com o item "Resumo" destacado
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavegacaoMenu.configurar(this, bottomNav, R.id.nav_resumo);
    }

    /** Sempre que a tela aparece, recalcula os números para estarem atualizados. */
    @Override
    protected void onResume() {
        super.onResume();
        atualizarResumo();
    }

    /** Consulta as três contagens no banco, mostra na tela e atualiza o anel de progresso. */
    private void atualizarResumo() {
        int total = dao.contar(null);      // todos
        int lidos = dao.contar(true);      // só lidos
        int naoLidos = dao.contar(false);  // só não lidos

        tvTotal.setText(String.valueOf(total));
        tvLidos.setText(String.valueOf(lidos));
        tvNaoLidos.setText(String.valueOf(naoLidos));

        // Percentual de lidos (0 a 100). Se não há livros, evita a divisão por zero e mostra 0%.
        int percentual = total == 0 ? 0 : Math.round(lidos * 100f / total);
        // setProgressCompat(valor, true) = preenche o anel com uma animação suave
        progressoLeitura.setProgressCompat(percentual, true);
        tvPercentual.setText(getString(R.string.resumo_percentual, percentual));

        // Frase abaixo do anel: "X de Y livros lidos", ou um convite quando a estante está vazia
        if (total == 0) {
            tvDescricaoProgresso.setText(R.string.resumo_progresso_vazio);
        } else {
            tvDescricaoProgresso.setText(
                    getString(R.string.resumo_progresso_descricao, lidos, total));
        }
    }
}
