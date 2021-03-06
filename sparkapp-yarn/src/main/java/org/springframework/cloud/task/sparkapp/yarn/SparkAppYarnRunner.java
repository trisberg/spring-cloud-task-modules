/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.sparkapp.yarn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.deploy.yarn.Client;
import org.apache.spark.deploy.yarn.ClientArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;

/**
 * {@link CommandLineRunner} implementation that will run a Spark App in YARN mode using
 * configuration properties provided.
 *
 * @author Thomas Risberg
 */
public class SparkAppYarnRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SparkAppYarnRunner.class);

	@Autowired
	private Configuration hadoopConfiguration;

	@Autowired
	private SparkAppYarnTaskProperties config;

	@Override
	public void run(String... args) throws Exception {
		SparkConf sparkConf = new SparkConf();
		sparkConf.set("spark.yarn.jar", config.getSparkAssemblyJar());

		List<String> submitArgs = new ArrayList<String>();
		if (StringUtils.hasText(config.getAppName())) {
			submitArgs.add("--name");
			submitArgs.add(config.getAppName());
		}
		submitArgs.add("--jar");
		submitArgs.add(config.getAppJar());
		submitArgs.add("--class");
		submitArgs.add(config.getAppClass());
		if (StringUtils.hasText(config.getResourceFiles())) {
			submitArgs.add("--files");
			submitArgs.add(config.getResourceFiles());
		}
		if (StringUtils.hasText(config.getResourceArchives())) {
			submitArgs.add("--archives");
			submitArgs.add(config.getResourceArchives());
		}
		submitArgs.add("--executor-memory");
		submitArgs.add(config.getExecutorMemory());
		submitArgs.add("--num-executors");
		submitArgs.add("" + config.getNumExecutors());
		for (String arg : config.getAppArgs()) {
			submitArgs.add("--arg");
			submitArgs.add(arg);
		}
		logger.info("Submit App with args: " + Arrays.asList(submitArgs));
		ClientArguments clientArguments =
				new ClientArguments(submitArgs.toArray(new String[submitArgs.size()]), sparkConf);
		clientArguments.isClusterMode();
		Client client = new Client(clientArguments, hadoopConfiguration, sparkConf);
		System.setProperty("SPARK_YARN_MODE", "true");
		try {
			client.run();
		}
		catch (Throwable t) {
			logger.error("Spark Application failed: " + t.getMessage(), t);
			throw new RuntimeException("Spark Application failed", t);
		}
	}

}
