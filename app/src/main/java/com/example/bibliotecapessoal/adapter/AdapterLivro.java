package com.example.bibliotecapessoal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bibliotecapessoal.R;
import com.example.bibliotecapessoal.model.Livro;

import java.util.ArrayList;
import java.util.List;

/**
 * ADAPTER — "Ponte" entre a lista de livros (dados) e a RecyclerView (componente visual).
 *
 * A RecyclerView é a lista rolável da tela principal. Ela não sabe nada sobre livros:
 * quem diz QUANTOS itens existem e COMO desenhar cada um é este Adapter.
 *
 * Para economizar memória, a RecyclerView RECICLA os cartões que saem da tela:
 * em vez de criar um cartão novo para cada livro, ela reaproveita os que já existem
 * e só troca os textos (onBindViewHolder).
 */
public class AdapterLivro extends RecyclerView.Adapter<AdapterLivro.LivroViewHolder> {

    /**
     * Interface de "callback": o Adapter não decide o que acontece ao clicar num livro;
     * ele apenas avisa a tela (ListaLivrosActivity), que implementa esta interface.
     */
    public interface OnLivroClickListener {
        /** Toque simples no livro → a tela abre a edição. */
        void onLivroClick(int position);

        /** Toque longo (pressionar e segurar) → a tela pergunta se quer excluir. */
        void onLivroLongClick(int position);
    }

    // Lista de livros exibida atualmente
    private final List<Livro> livros = new ArrayList<>();
    // Quem será avisado dos cliques (a tela de lista)
    private final OnLivroClickListener listener;

    public AdapterLivro(List<Livro> livrosIniciais, OnLivroClickListener listener) {
        this.listener = listener;
        if (livrosIniciais != null) {
            this.livros.addAll(livrosIniciais);
        }
    }

    /**
     * Troca todo o conteúdo da lista (ex.: depois de salvar ou excluir um livro)
     * e manda a RecyclerView se redesenhar com notifyDataSetChanged().
     */
    public void atualizarLista(List<Livro> lista) {
        livros.clear();
        if (lista != null) {
            livros.addAll(lista);
        }
        notifyDataSetChanged();
    }

    /** Devolve o livro de uma posição da lista (usado quando o usuário clica). */
    public Livro getLivro(int position) {
        return livros.get(position);
    }

    /**
     * Chamado quando a RecyclerView precisa de um cartão NOVO.
     * "Infla" (transforma o XML em objeto View) o layout item_livro.xml.
     */
    @NonNull
    @Override
    public LivroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_livro, parent, false);
        return new LivroViewHolder(view);
    }

    /**
     * Chamado para PREENCHER um cartão com os dados do livro da posição informada.
     * Acontece toda vez que um cartão aparece na tela (inclusive os reciclados).
     */
    @Override
    public void onBindViewHolder(@NonNull LivroViewHolder holder, int position) {
        Livro livro = livros.get(position);
        Context context = holder.itemView.getContext();

        // Textos formatados usando os modelos de strings.xml (ex.: "Autor: %1$s")
        holder.tvTitulo.setText(livro.getTitulo());
        holder.tvAutor.setText(context.getString(R.string.formato_autor, livro.getAutor()));
        holder.tvAno.setText(context.getString(R.string.formato_ano, livro.getAno()));

        // Nota é opcional: mostra "Nota: X/10" ou "Sem nota"
        if (livro.getNota() != null) {
            holder.tvNota.setText(context.getString(R.string.formato_nota, livro.getNota()));
        } else {
            holder.tvNota.setText(R.string.sem_nota);
        }

        // Status de leitura: texto e cor mudam (verde = Lido, terracota = Não lido).
        // A mesma cor pinta a faixa lateral do cartão (viewIndicador).
        int corStatus;
        if (livro.isLido()) {
            holder.tvStatus.setText(R.string.status_lido);
            corStatus = ContextCompat.getColor(context, R.color.status_lido);
        } else {
            holder.tvStatus.setText(R.string.status_nao_lido);
            corStatus = ContextCompat.getColor(context, R.color.status_nao_lido);
        }
        holder.tvStatus.setTextColor(corStatus);
        holder.viewIndicador.setBackgroundColor(corStatus);

        // Clique simples: avisa a tela. getBindingAdapterPosition() pega a posição ATUAL
        // do cartão (pode ter mudado desde que foi preenchido); NO_POSITION = item sendo removido.
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) {
                listener.onLivroClick(pos);
            }
        });

        // Clique longo: avisa a tela. Retornar true indica que o evento foi tratado.
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) {
                listener.onLivroLongClick(pos);
                return true;
            }
            return false;
        });
    }

    /** Informa à RecyclerView quantos itens existem na lista. */
    @Override
    public int getItemCount() {
        return livros.size();
    }

    /**
     * VIEWHOLDER — Guarda as referências dos componentes de UM cartão (item_livro.xml).
     * Assim o findViewById() é feito só uma vez por cartão, e não a cada rolagem,
     * o que deixa a lista mais rápida.
     */
    static class LivroViewHolder extends RecyclerView.ViewHolder {
        final View viewIndicador;  // Faixa colorida à esquerda (verde/vermelha)
        final TextView tvTitulo;
        final TextView tvAutor;
        final TextView tvAno;
        final TextView tvNota;
        final TextView tvStatus;   // "Lido" ou "Não lido"

        LivroViewHolder(@NonNull View itemView) {
            super(itemView);
            viewIndicador = itemView.findViewById(R.id.viewIndicador);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvAno = itemView.findViewById(R.id.tvAno);
            tvNota = itemView.findViewById(R.id.tvNota);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
