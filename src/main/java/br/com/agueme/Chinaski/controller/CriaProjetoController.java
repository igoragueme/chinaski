package br.com.agueme.Chinaski.controller;

import br.com.agueme.Chinaski.domain.CriarProjetoUseCase;
import br.com.agueme.Chinaski.domain.DomainException;
import br.com.agueme.Chinaski.domain.ProjetoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/api/projeto")
public class CriaProjetoController {

    @Autowired
    private CriarProjetoUseCase criarProjetoUseCase;

    @PostMapping("/criar")
    public ResponseEntity<?> criar(@RequestBody ProjetoRequest projetoRequest){
        try {
            criarProjetoUseCase.execute(projetoRequest);
            return new ResponseEntity<>(new ResponseMessage("Automação executada com sucesso!"), HttpStatus.OK);
        }catch (DomainException e){
            return new ResponseEntity<>(new ResponseMessage(e.getMessage()), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(new ResponseMessage("Falha ao executar automação."), HttpStatus.OK);
        }
    }


}
