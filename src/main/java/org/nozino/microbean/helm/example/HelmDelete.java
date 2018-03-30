package org.nozino.microbean.helm.example;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import hapi.release.ReleaseOuterClass.Release;

import hapi.services.tiller.Tiller.UninstallReleaseRequest;
import hapi.services.tiller.Tiller.UninstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelmDelete {
	private static final Logger logger = LoggerFactory.getLogger(HelmDelete.class);
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		if (args.length < 2) {
			System.out.println("Usage: cmd <kubernetes-config> <release-name>");
			return;
		}

		String fileName = args[0];
		File file = new File(fileName);
		if (!file.exists() || !file.isFile()) {
			System.err.println("File does not exist: " + fileName);
			return;
		}
		
		String releaseName = args[1];

		logger.debug("config file path: {}", fileName);
		logger.debug("releaseName: {}", releaseName);

		ConfigBuilder builder = new ConfigBuilder();
		if (args.length > 1) {
			builder.withOauthToken(fileName);
		}
		Config config = builder.build();

		final DefaultKubernetesClient client = new DefaultKubernetesClient(config);

		if (logger.isDebugEnabled()) {
			logger.debug("master url: {}", config.getMasterUrl());
			logger.debug("kubernetes api version: {}", client.getApiVersion());
		}

		final Tiller tiller = new Tiller(client);
		final ReleaseManager releaseManager = new ReleaseManager(tiller);

		final UninstallReleaseRequest.Builder uninstallRequestBuilder = UninstallReleaseRequest.newBuilder();
		assert uninstallRequestBuilder != null;
		
		uninstallRequestBuilder.setName(releaseName); // set releaseName
		uninstallRequestBuilder.setPurge(true); // --purge
		
		final Future<UninstallReleaseResponse> releaseFuture = releaseManager.uninstall(uninstallRequestBuilder.build());
		assert releaseFuture != null;
		final Release release = releaseFuture.get().getRelease();
		assert release != null;
		
		releaseManager.close();
	}
}
