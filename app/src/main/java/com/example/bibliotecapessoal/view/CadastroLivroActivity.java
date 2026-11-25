package com.example.bibliotecapessoal.view;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bibliotecapessoal.R;
import com.example.bibliotecapessoal.model.Livro;
import com.example.bibliotecapessoal.persistencia.ILivroDAO;
import com.example.bibliotecapessoal.persistencia.LivroDAO;
import com.example.bibliotecapessoal.util.NavegacaoMenu;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

/**
 * VIEW — Tela de Cadastro / Edição de livro.
 *
 * A MESMA tela serve para dois modos:
 *  - CADASTRO: aberta pelo menu "Cadastrar" → formulário vazio, botão "Salvar";
 *  - EDIÇÃO:   aberta ao tocar num livro da lista → recebe o livro pelo Intent,
 *              preenche o formulário, o botão vira "Atualizar" e a Toolbar ganha
 *              uma seta "voltar" que retorna à lista.
 *
 * Antes de gravar, valida os campos e mostra o erro embaixo do campo inválido.
 */
public class CadastroLivroActivity extends AppCompatActivity {

    // "Chave" usada para enviar/receber o livro dentro do Intent
    public static final String EXTRA_LIVRO = "extra_livro";

    // Regras de validação
    private static final int ANO_MINIMO = 1450;  // Ano aproximado da invenção da prensa de Gutenberg
    private static final int NOTA_MINIMA = 0;
    private static final int NOTA_MAXIMA = 10;

    // Campos do formulário
    private EditText etTitulo;
    private EditText etAutor;
    private EditText etAno;
    private EditText etNota;
    private CheckBox cbLido;

    private ILivroDAO dao;
    // Livro recebido da lista. Se for null, estamos CADASTRANDO; se não, EDITANDO.
    private Livro livroEmEdicao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ícones da barra de status em branco, sobre a barra superior verde-petróleo
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_cadastro_livro);

        // Espaçamento para o conteúdo não ficar embaixo da barra de status
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, 0);
            return insets;
        });

        dao = new LivroDAO(this);

        // Liga as variáveis Java aos componentes do layout XML (pelo id)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etTitulo = findViewById(R.id.etTitulo);
        etAutor = findViewById(R.id.etAutor);
        etAno = findViewById(R.id.etAno);
        etNota = findViewById(R.id.etNota);
        cbLido = findViewById(R.id.cbLido);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        // Verifica se a tela recebeu um livro (modo EDIÇÃO)
        livroEmEdicao = (Livro) getIntent().getSerializableExtra(EXTRA_LIVRO);
        if (livroEmEdicao != null) {
            // Preenche o formulário com os dados atuais do livro
            etTitulo.setText(livroEmEdicao.getTitulo());
            etAutor.setText(livroEmEdicao.getAutor());
            etAno.setText(String.valueOf(livroEmEdicao.getAno()));
            if (livroEmEdicao.getNota() != null) {
                etNota.setText(String.valueOf(livroEmEdicao.getNota()));
            }
            cbLido.setChecked(livroEmEdicao.isLido());
            // Ajusta textos para o modo edição
            btnSalvar.setText(R.string.btn_atualizar);
            toolbar.setTitle(R.string.titulo_edicao);
            configurarVoltarParaLista(toolbar);
        }

        // Ao tocar no botão, chama o método salvar()
        btnSalvar.setOnClickListener(v -> salvar());

        // Menu inferior com o item "Cadastrar" destacado
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavegacaoMenu.configurar(this, bottomNav, R.id.nav_cadastro);
    }

    /**
     * MODO EDIÇÃO: faz a seta da Toolbar E o botão "voltar" do celular retornarem à lista.
     *
     * Sem isso, como a lista foi fechada ao abrir a edição (NavegacaoMenu.irPara chama
     * finish()), o "voltar" do celular fecharia o app inteiro em vez de voltar à lista.
     * OnBackPressedCallback é a forma atual recomendada pelo Android de tratar o "voltar".
     */
    private void configurarVoltarParaLista(MaterialToolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_voltar);
        toolbar.setNavigationContentDescription(R.string.descricao_voltar);
        toolbar.setNavigationOnClickListener(v -> voltarParaLista());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                voltarParaLista();
            }
        });
    }

    /** Abre a lista de livros e fecha esta tela (sem salvar nada). */
    private void voltarParaLista() {
        NavegacaoMenu.irPara(this, ListaLivrosActivity.class);
    }

    /**
     * Lê os campos, VALIDA e grava no banco (insere ou atualiza).
     *
     * A variável "valido" começa true e vira false a cada erro encontrado. Todos os
     * campos são verificados (e não só o primeiro), para o usuário ver todos os erros de uma vez.
     */
    private void salvar() {
        // Lê o texto digitado; trim() remove espaços no início e no fim
        String titulo = etTitulo.getText().toString().trim();
        String autor = etAutor.getText().toString().trim();
        String anoTexto = etAno.getText().toString().trim();
        String notaTexto = etNota.getText().toString().trim();

        boolean valido = true;

        // Título: obrigatório
        if (titulo.isEmpty()) {
            etTitulo.setError(getString(R.string.erro_titulo_obrigatorio));
            valido = false;
        }

        // Autor: obrigatório
        if (autor.isEmpty()) {
            etAutor.setError(getString(R.string.erro_autor_obrigatorio));
            valido = false;
        }

        // Ano: obrigatório, numérico e entre 1450 e o ano atual
        int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
        int ano = 0;
        if (anoTexto.isEmpty()) {
            etAno.setError(getString(R.string.erro_ano_obrigatorio));
            valido = false;
        } else {
            try {
                ano = Integer.parseInt(anoTexto);
                if (ano < ANO_MINIMO || ano > anoAtual) {
                    etAno.setError(getString(R.string.erro_ano_intervalo, ANO_MINIMO, anoAtual));
                    valido = false;
                }
            } catch (NumberFormatException e) {
                // Texto não é um número válido
                etAno.setError(getString(R.string.erro_ano_intervalo, ANO_MINIMO, anoAtual));
                valido = false;
            }
        }

        // Nota: OPCIONAL; se preenchida, deve estar entre 0 e 10. Vazia = null (sem nota)
        Integer nota = null;
        if (!notaTexto.isEmpty()) {
            try {
                int notaInformada = Integer.parseInt(notaTexto);
                if (notaInformada < NOTA_MINIMA || notaInformada > NOTA_MAXIMA) {
                    etNota.setError(getString(R.string.erro_nota_intervalo, NOTA_MINIMA, NOTA_MAXIMA));
                    valido = false;
                } else {
                    nota = notaInformada;
                }
            } catch (NumberFormatException e) {
                etNota.setError(getString(R.string.erro_nota_intervalo, NOTA_MINIMA, NOTA_MAXIMA));
                valido = false;
            }
        }

        // Se houve qualquer erro, para aqui e NÃO grava nada
        if (!valido) {
            return;
        }

        boolean lido = cbLido.isChecked();

        if (livroEmEdicao != null) {
            // ---- MODO EDIÇÃO: altera o livro recebido e faz UPDATE no banco ----
            livroEmEdicao.setTitulo(titulo);
            livroEmEdicao.setAutor(autor);
            livroEmEdicao.setAno(ano);
            livroEmEdicao.setLido(lido);
            livroEmEdicao.setNota(nota);

            if (dao.atualizar(livroEmEdicao)) {
                Toast.makeText(this, R.string.msg_atualizado_sucesso, Toast.LENGTH_SHORT).show();
                NavegacaoMenu.irPara(this, ListaLivrosActivity.class); // volta para a lista
            } else {
                Toast.makeText(this, R.string.msg_erro_atualizar, Toast.LENGTH_SHORT).show();
            }
        } else {
            // ---- MODO CADASTRO: cria um livro novo (id 0, o banco gera o real) e faz INSERT ----
            Livro novoLivro = new Livro(0, titulo, autor, ano, lido, nota);

            if (dao.salvar(novoLivro)) {
                Toast.makeText(this, R.string.msg_salvo_sucesso, Toast.LENGTH_SHORT).show();
                NavegacaoMenu.irPara(this, ListaLivrosActivity.class); // volta para a lista
            } else {
                Toast.makeText(this, R.string.msg_erro_salvar, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
