package org.nozino.microbean.helm.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import hapi.release.ReleaseOuterClass.Release;
import hapi.services.tiller.Tiller.ListReleasesRequest;
import hapi.services.tiller.Tiller.ListReleasesResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class HelmList {
	private static final Logger logger = LoggerFactory.getLogger(HelmList.class);

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		String idToken = null;
		InputStream input = new FileInputStream(new File("C:/Users/jinho/.bluemix/plugins/container-service/clusters/bs-cluster/kube-config-seo01-bs-cluster.yml"));
	    Yaml yaml = new Yaml();
	    Map<String, Object> iccsConfig = (Map<String, Object>)yaml.load(input);
	    if (iccsConfig != null) {
	    	ArrayList<Object> users = (ArrayList<Object>)iccsConfig.get("users");
	    	if (users != null) {
	    		Map<String, Object> user = (Map<String, Object>)users.get(0);
	    		if (user != null) {
	    			Map<String, Object> userInfo = (Map<String, Object>)user.get("user");
	    			if (userInfo != null) {
	    				Map<String, Object> authProvider = (Map<String, Object>)userInfo.get("auth-provider");
	    				if (authProvider != null) {
	    					Map<String, Object> authProviderConfig = (Map<String, Object>)authProvider.get("config");
	    					if (authProviderConfig != null) {
	    						idToken = (String)authProviderConfig.get("id-token");
	    						logger.debug("idToken: {}", idToken);
	    					}
	    				}
	    			}
	    		}
	    	}
	    }
	    
	    
		ConfigBuilder builder = new ConfigBuilder();
		
		builder.withMasterUrl("https://169.56.69.242:32254");
		builder.withOauthToken(idToken);
		builder.withTrustCerts(true);
		
		Config config = builder.build();

		final DefaultKubernetesClient client = new DefaultKubernetesClient(config);

		if (logger.isDebugEnabled()) {
			logger.debug("master url: {}", config.getMasterUrl());
			logger.debug("kubernetes api version: {}", client.getApiVersion());
		}

		final Tiller tiller = new Tiller(client);
		final ReleaseManager releaseManager = new ReleaseManager(tiller);

		final ListReleasesRequest.Builder requestBuilder = ListReleasesRequest.newBuilder();
		assert requestBuilder != null;

		final Iterator<ListReleasesResponse> releaseFuture = releaseManager.list(requestBuilder.build());
		assert releaseFuture != null;
			
		while (releaseFuture.hasNext()) {
			ListReleasesResponse ent = releaseFuture.next();
			System.out.println(ent.getReleasesCount());
			System.out.println(ent.getTotal());
			List<Release> releaseList = ent.getReleasesList();

			for(Release release : releaseList) {
				logger.info("======================");
				logger.info("{}", release.getName());
				logger.info(". CHART_NAME: {}", release.getChart().getMetadata().getName());
				logger.info(". DEP_CHART_VERSION: {}", release.getChart().getMetadata().getVersion());
				logger.info(". DEP_NAMESPACE: {}", release.getNamespace());
				logger.info(". DEP_STATUS: {}", release.getInfo().getStatus().getCode().toString());
				logger.info("======================");
			}
		}
		
		releaseManager.close();
		tiller.close();
	}
}
