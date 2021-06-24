package br.com.agueme.Chinaski.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjetoRequest {
    private String nome;
    private String responsavel;
    private List<AplicacaoRequest> aplicacoes;
}