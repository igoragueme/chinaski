package br.com.agueme.Chinaski.domain;

import br.com.agueme.Chinaski.comum.K8sClient;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CriarProjetoUseCase {

    Logger logger = LoggerFactory.getLogger(CriarProjetoUseCase.class);

    public void execute(ProjetoRequest projetoRequest){
        try {
            KubernetesClient kubernetesClient = K8sClient.getInstance();
            final String nomeProjeto = projetoRequest.getNome();

            Namespace namespace = kubernetesClient.namespaces().withName(nomeProjeto).get();

            if (namespace != null) {
                throw new NamespaceJaExisteException("Já existe um ambiente montado com esse nome de projeto, considerar outro nome.");
            }

            Namespace projetoNamespace = kubernetesClient.namespaces().create(new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(nomeProjeto)
                    .addToLabels("istio-injection", "enabled")
                    .addToLabels("projeto", nomeProjeto)
                    .endMetadata()
                    .build());

            logger.info("Namespace " + nomeProjeto + " criado.");

            projetoRequest.getAplicacoes()
                .forEach(
                    aplicacao -> {
                        kubernetesClient.services()
                                .inNamespace(nomeProjeto)
                                .create(
                                    new ServiceBuilder()
                                        .withNewMetadata()
                                        .withName(aplicacao.getNome() + "-service")
                                        .addToLabels("sigla", aplicacao.getSigla())
                                        .endMetadata()
                                        .withNewSpec()
                                        .addNewPort()
                                        .withName("http")
                                        .withPort(8080)
                                        .withProtocol("TCP")
                                        .withTargetPort(new IntOrString(8080))
                                        .endPort()
                                        .addToSelector("app",aplicacao.getNome())
                                        .endSpec()
                                        .build()
                                );
                    });
            logger.info("Automação finalizada com sucesso.");
        }catch (KubernetesClientException e){
            /*Ver como tratar erro da API do k8s
            Sabemos que será necessário:
             - Deletar o namespace caso ele tenha sido criado
             - Deletar todos os services que possivelmente possa ter sido criado (talvez nao precise)
             - Verificar se ao deletar o namespace, o k8s realmente deleta todos os objetos atrelado a ele
             */

            e.printStackTrace();
        }catch (DomainException e){
            //Trocar por um logger
            logger.error(e.getMessage());
            throw e;
        }
    }

}
