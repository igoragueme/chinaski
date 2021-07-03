package br.com.agueme.Chinaski.domain;

import br.com.agueme.Chinaski.exception.DomainException;
import br.com.agueme.Chinaski.exception.NamespaceJaExisteException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import me.snowdrop.istio.api.networking.v1alpha3.*;
import me.snowdrop.istio.client.DefaultIstioClient;
import me.snowdrop.istio.client.IstioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CriarProjetoUseCase {

    Logger logger = LoggerFactory.getLogger(CriarProjetoUseCase.class);

    public void execute(ProjetoRequest projetoRequest){
        KubernetesClient kubernetesClient = new DefaultKubernetesClient();
        IstioClient istioClient = new DefaultIstioClient();

        try {
            final String nomeProjeto = projetoRequest.getNome();

            Namespace namespace = kubernetesClient.namespaces().withName(nomeProjeto).get();
            if (namespace != null) throw new NamespaceJaExisteException("Já existe um ambiente montado com esse nome de projeto, considerar outro nome.");

            criaNamespace(kubernetesClient, nomeProjeto);

            List<HTTPRoute> routes = new ArrayList<HTTPRoute>();
            projetoRequest.getAplicacoes()
                .forEach(
                    aplicacao -> {
                        Service service = criaService(kubernetesClient, nomeProjeto, aplicacao);

//                        new DeploymentBuilder()
//                                .withNewMetadata()
//                                .withName(aplicacao.getNome())
//                                .withLabels(Map.of("app",aplicacao.getNome()))
//                                .endMetadata()
//                                .withNewSpec()
//                                .withTemplate()
//
//                        kubernetesClient.apps()
//                                .deployments();

                        routes.add(criaRoute(nomeProjeto, service, aplicacao));
                    });

            criaVirtualService(istioClient, nomeProjeto, routes);
        }catch (KubernetesClientException e){
            e.printStackTrace();
        }catch (DomainException e){
            logger.error(e.getMessage());
            throw e;
        }
        logger.info("Automação finalizada com sucesso.");
    }

    private Namespace criaNamespace(KubernetesClient kubernetesClient, String nomeProjeto) {
        return kubernetesClient.namespaces()
                .create(new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(nomeProjeto)
                        .addToLabels("istio-injection", "enabled")
                        .addToLabels("projeto", nomeProjeto)
                        .endMetadata()
                        .build());
    }

    private VirtualService criaVirtualService(IstioClient istioClient, String nomeProjeto, List<HTTPRoute> routes) {
        return istioClient.v1alpha3VirtualService()
                .inNamespace(nomeProjeto)
                .create(new VirtualServiceBuilder()
                        .withNewMetadata()
                        .withName(nomeProjeto + "-virtualservice")
                        .withNamespace(nomeProjeto)
                        .withLabels(Map.of("projeto", nomeProjeto))
                        .endMetadata()
                        .withNewSpec()
                        .withHosts("projetos.xpto.hom.aws")
                        .withGateways("istio-system/projetos-gateway")
                        .withHttp(routes)
                        .endSpec()
                        .build()
                );
    }

    private HTTPRoute criaRoute(String nomeProjeto, Service service, AplicacaoRequest aplicacao) {
        HTTPMatchRequest matchRequest = new HTTPMatchRequestBuilder()
                .withNewUri()
                .withNewPrefixMatchType("/" + nomeProjeto + "/" + aplicacao.getNome() + "/")
                .endUri()
                .build();

        return new HTTPRouteBuilder()
                .withMatch(matchRequest)
                .withNewRewrite()
                .withNewUri("/")
                .endRewrite()
                .addNewRoute()
                .withNewDestination()
                .withHost(service.getMetadata().getName() + "." + nomeProjeto + ".svc.cluster.local")
                .endDestination()
                .endRoute()
                .build();
    }

    private Service criaService(KubernetesClient kubernetesClient, String nomeProjeto, AplicacaoRequest aplicacao) {
        Service service = kubernetesClient.services()
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
                                .addToSelector("app", aplicacao.getNome())
                                .endSpec()
                                .build()
                );
        return service;
    }

}