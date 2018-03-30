/*
 * by nozino (nozino AT gmail.com)
 * chart name : stable/mariadb (by bitnami) 
 */

package org.nozino.microbean.helm.example;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.microbean.helm.chart.URLChartLoader;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import hapi.chart.ChartOuterClass.Chart;
import hapi.release.ReleaseOuterClass.Release;

import hapi.services.tiller.Tiller.UpdateReleaseRequest;
import hapi.services.tiller.Tiller.UpdateReleaseResponse;
import hapi.chart.ConfigOuterClass.Config.Builder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelmUpdate {
	private static final Logger logger = LoggerFactory.getLogger(HelmUpdate.class);

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		if (args.length < 3) {
			System.out.println("Usage: cmd <kubernetes-config> <release-name> <pvc>");
			return;
		}

		String fileName = args[0];
		File file = new File(fileName);
		if (!file.exists() || !file.isFile()) {
			System.err.println("File does not exist: " + fileName);
			return;
		}
		
		String releaseName = args[1];
		String pvcName = args[2];

		logger.debug("config file path: {}", fileName);

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

		final URI uri = URI.create("https://kubernetes-charts.storage.googleapis.com/mariadb-2.1.9.tgz");
		final URL url = uri.toURL();
		Chart.Builder chart = null;
		try (final URLChartLoader chartLoader = new URLChartLoader()) {
			chart = chartLoader.load(url);
		}
		assert chart != null;

		final Tiller tiller = new Tiller(client);
		final ReleaseManager releaseManager = new ReleaseManager(tiller);

		final UpdateReleaseRequest.Builder requestBuilder = UpdateReleaseRequest.newBuilder();
		assert requestBuilder != null;
		requestBuilder.setTimeout(300L);
		requestBuilder.setName(releaseName);
		requestBuilder.setWait(true);

		Builder valuesBuilder = requestBuilder.getValuesBuilder();
		valuesBuilder.setRaw(
				"persistence: \n" +
				"  existingClaim: " + pvcName + "\n" +
				"resources:\n" + 
				"  requests:\n" + 
				"    memory: 512Mi\n" + 
				"    cpu: 500m");
		
		final Future<UpdateReleaseResponse> releaseFuture = releaseManager.update(requestBuilder, chart);
		assert releaseFuture != null;
		final Release release = releaseFuture.get().getRelease();
		assert release != null;
		
		releaseManager.close();
		tiller.close();
	}
}
