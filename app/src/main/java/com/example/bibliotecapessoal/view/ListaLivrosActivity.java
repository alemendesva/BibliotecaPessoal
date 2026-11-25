package com.example.bibliotecapessoal.view;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliotecapessoal.R;
import com.example.bibliotecapessoal.adapter.AdapterLivro;
import com.example.bibliotecapessoal.model.Livro;
import com.example.bibliotecapessoal.persistencia.ILivroDAO;
import com.example.bibliotecapessoal.persistencia.LivroDAO;
import com.example.bibliotecapessoal.util.NavegacaoMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * VIEW — Tela "Meus Livros" (tela INICIAL do app, definida no AndroidManifest.xml).
 *
 * Responsabilidades:
 *  - Mostrar todos os livros cadastrados numa lista rolável (RecyclerView);
 *  - Mostrar o "estado vazio" ilustrado quando não houver nenhum livro;
 *  - Toque simples num livro → abre a tela de cadastro em modo EDIÇÃO;
 *  - Toque longo num livro → pergunta se deseja EXCLUIR;
 *  - Deslizar o cartão para o lado → exclui na hora, com opção de DESFAZER.
 *
 * Implementa AdapterLivro.OnLivroClickListener para receber os cliques vindos do Adapter.
 */
public class ListaLivrosActivity extends AppCompatActivity
        implements AdapterLivro.OnLivroClickListener {

    private ILivroDAO dao;              // Acesso ao banco (declarado pela interface)
    private AdapterLivro adapter;       // Liga a lista de livros à RecyclerView
    private View layoutLista;           // Dica de uso + lista (exibidos quando há livros)
    private View layoutListaVazia;      // Ilustração + mensagem + botão (exibidos quando não há livros)
    private BottomNavigationView bottomNav;

    /**
     * onCreate: primeiro método do ciclo de vida, executado quando a tela é criada.
     * Aqui montamos a tela e inicializamos os componentes.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Faz o app desenhar "de ponta a ponta", inclusive atrás da barra de status.
        // SystemBarStyle.dark = ícones da barra de status (hora, bateria) em BRANCO,
        // para ficarem legíveis sobre a barra superior verde-petróleo, no modo claro e no escuro.
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        // Liga esta classe Java ao layout XML da tela
        setContentView(R.layout.activity_lista_livros);

        // Como o app desenha atrás das barras do sistema, adiciona um espaçamento (padding)
        // do tamanho dessas barras para o conteúdo não ficar escondido embaixo delas.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, 0);
            return insets;
        });

        dao = new LivroDAO(this);

        layoutLista = findViewById(R.id.layoutLista);
        layoutListaVazia = findViewById(R.id.layoutListaVazia);

        // Botão do estado vazio: leva direto ao formulário de cadastro
        findViewById(R.id.btnPrimeiroLivro).setOnClickListener(v ->
                NavegacaoMenu.irPara(this, CadastroLivroActivity.class));

        // Configura a RecyclerView: LinearLayoutManager = itens em lista vertical
        RecyclerView rvLivros = findViewById(R.id.rvLivros);
        rvLivros.setLayoutManager(new LinearLayoutManager(this));
        // Começa vazio (null); "this" = esta tela recebe os cliques
        adapter = new AdapterLivro(null, this);
        rvLivros.setAdapter(adapter);
        configurarArrastarParaExcluir(rvLivros);

        // Configura o menu inferior, destacando o item "Livros"
        bottomNav = findViewById(R.id.bottomNav);
        NavegacaoMenu.configurar(this, bottomNav, R.id.nav_lista);
    }

    /**
     * onResume: executado toda vez que a tela fica visível (inclusive ao voltar de outra).
     * Recarregar aqui garante que a lista esteja sempre atualizada.
     */
    @Override
    protected void onResume() {
        super.onResume();
        carregarLivros();
    }

    /** Busca os livros no banco, entrega ao Adapter e alterna entre a lista e o estado vazio. */
    private void carregarLivros() {
        List<Livro> livros = dao.listarTodos();
        adapter.atualizarLista(livros);
        boolean vazia = livros.isEmpty();
        layoutListaVazia.setVisibility(vazia ? View.VISIBLE : View.GONE);
        layoutLista.setVisibility(vazia ? View.GONE : View.VISIBLE);
    }

    /**
     * Toque simples num livro: abre a tela de cadastro enviando o livro no Intent
     * (por isso Livro é Serializable). A tela de cadastro percebe que recebeu um livro
     * e entra em modo de edição.
     */
    @Override
    public void onLivroClick(int position) {
        Livro livro = adapter.getLivro(position);
        Intent intent = new Intent(this, CadastroLivroActivity.class);
        intent.putExtra(CadastroLivroActivity.EXTRA_LIVRO, livro);
        NavegacaoMenu.irPara(this, intent);
    }

    /**
     * Toque longo num livro: mostra uma caixa de diálogo de confirmação.
     * Se o usuário confirmar, exclui do banco, mostra um aviso (Toast) e recarrega a lista.
     * MaterialAlertDialogBuilder = mesmo diálogo de antes, com o visual do Material 3.
     */
    @Override
    public void onLivroLongClick(int position) {
        Livro livro = adapter.getLivro(position);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialogo_excluir_titulo)
                .setMessage(getString(R.string.dialogo_excluir_mensagem, livro.getTitulo()))
                .setPositiveButton(R.string.dialogo_btn_sim, (dialog, which) -> {
                    if (dao.excluir(livro.getId())) {
                        Toast.makeText(this, R.string.msg_excluido_sucesso, Toast.LENGTH_SHORT).show();
                        carregarLivros();
                    } else {
                        Toast.makeText(this, R.string.msg_erro_excluir, Toast.LENGTH_SHORT).show();
                    }
                })
                // "Cancelar" apenas fecha o diálogo (null = nenhuma ação extra)
                .setNegativeButton(R.string.dialogo_btn_nao, null)
                .show();
    }

    // ======================= ARRASTAR PARA EXCLUIR =======================

    /**
     * Liga o gesto de "deslizar para o lado" nos cartões da lista.
     *
     * ItemTouchHelper é a classe do Android que detecta o gesto de arrastar itens de uma
     * RecyclerView. O SimpleCallback recebe:
     *  - 0 → não permite reordenar (arrastar para cima/baixo);
     *  - LEFT | RIGHT → permite deslizar para os dois lados.
     */
    private void configurarArrastarParaExcluir(RecyclerView rvLivros) {
        // Ícone de lixeira e cor de fundo exibidos "atrás" do cartão enquanto ele desliza
        final Drawable iconeExcluir = ContextCompat.getDrawable(this, R.drawable.ic_excluir);
        if (iconeExcluir != null) {
            iconeExcluir.mutate().setTint(Color.WHITE);
        }
        final ColorDrawable fundo = new ColorDrawable(
                ContextCompat.getColor(this, R.color.fundo_excluir));

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            /** Reordenar não é permitido, então este método não faz nada. */
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            /** Chamado quando o usuário termina de deslizar o cartão para fora da tela. */
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    excluirComDesfazer(pos);
                }
            }

            /**
             * Desenha o fundo colorido e a lixeira no espaço que o cartão vai "descobrindo"
             * enquanto desliza. dX = quanto o cartão já andou (positivo = direita).
             */
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View item = viewHolder.itemView;
                if (dX != 0) {
                    // Fundo: só na área já descoberta (à esquerda ou à direita do cartão)
                    if (dX > 0) {
                        fundo.setBounds(item.getLeft(), item.getTop(),
                                item.getLeft() + (int) dX, item.getBottom());
                    } else {
                        fundo.setBounds(item.getRight() + (int) dX, item.getTop(),
                                item.getRight(), item.getBottom());
                    }
                    fundo.draw(c);

                    // Lixeira: centralizada na vertical, perto da borda descoberta
                    if (iconeExcluir != null) {
                        int tamanho = iconeExcluir.getIntrinsicHeight();
                        int margem = (item.getHeight() - tamanho) / 2;
                        int topo = item.getTop() + margem;
                        int esquerda = dX > 0
                                ? item.getLeft() + margem
                                : item.getRight() - margem - tamanho;
                        iconeExcluir.setBounds(esquerda, topo, esquerda + tamanho, topo + tamanho);
                        iconeExcluir.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvLivros);
    }

    /**
     * Exclui o livro deslizado e mostra uma Snackbar com o botão "Desfazer".
     *
     * Ao desfazer, o MESMO objeto Livro é gravado de novo com salvar(): o banco gera
     * um id novo para ele, mas título, autor, ano, nota e status voltam exatamente iguais.
     */
    private void excluirComDesfazer(int position) {
        final Livro livro = adapter.getLivro(position);

        if (!dao.excluir(livro.getId())) {
            Toast.makeText(this, R.string.msg_erro_excluir, Toast.LENGTH_SHORT).show();
            // Devolve o cartão à posição original (ele tinha saído da tela com o gesto)
            adapter.notifyItemChanged(position);
            return;
        }
        carregarLivros();

        Snackbar.make(findViewById(R.id.main),
                        getString(R.string.msg_excluido_desfazer, livro.getTitulo()),
                        Snackbar.LENGTH_LONG)
                // Faz a Snackbar aparecer ACIMA do menu inferior (e não por cima dele)
                .setAnchorView(bottomNav)
                .setAction(R.string.acao_desfazer, v -> {
                    if (dao.salvar(livro)) {
                        carregarLivros();
                    } else {
                        Toast.makeText(this, R.string.msg_erro_desfazer, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
