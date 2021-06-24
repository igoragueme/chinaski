package br.com.agueme.Chinaski.comum;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public final class K8sClient {

    private static KubernetesClient kubernetesClient;

    private K8sClient(){
    }

    public static KubernetesClient getInstance(){
        if(kubernetesClient == null){
            kubernetesClient = new DefaultKubernetesClient();
        }
        return kubernetesClient;
    }

}
