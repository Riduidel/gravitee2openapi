package org.ndx.gravitee.openapi.transform;

import static org.ndx.gravitee.openapi.transform.DotAwareMap.Builder.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.bazaarvoice.jolt.Transform;

public class PathTransformer implements Transform {
	private static final Logger logger = Logger.getLogger(PathTransformer.class.getName());
	public static interface ElementTransformer {
		void createPolicyConfigFor(String path, String key, Object value, DotAwareMap returned);
	}

	public static class NoopTransformer implements ElementTransformer {
		@Override
		public void createPolicyConfigFor(String path, String key, Object value, DotAwareMap returned) {
		}
	}

	public static abstract class DescriptionTransformer implements ElementTransformer {
		public void appendToDescription(String text, DotAwareMap returned) {
			returned.put("description", returned.getOrDefault("description", "") + "\n" + text);
		}
	}

	public static class GroovyScript extends DescriptionTransformer implements ElementTransformer {

		@Override
		public void createPolicyConfigFor(String path, String key, Object value, DotAwareMap returned) {
			Map<String, Object> groovyConfig = (Map<String, Object>) value;
			StringBuilder sOut = new StringBuilder("Run groovy scripts\n\n");
			for (Map.Entry<String, Object> entry : groovyConfig.entrySet()) {
				sOut.append(String.format(" * %s\n", entry.getKey()));
			}
			appendToDescription(sOut.toString(), returned);
		}

	}

	public static class TransformHeader extends DescriptionTransformer implements ElementTransformer {

		@Override
		public void createPolicyConfigFor(String path, String key, Object value, DotAwareMap returned) {
			Map<String, Object> headerConfig = (Map<String, Object>) value;
			StringBuilder sOut = new StringBuilder("Transform headers\n\n");
			for (Map.Entry<String, Object> entry : headerConfig.entrySet()) {
				String headerConfigKey = entry.getKey();
				if ("scope".equals(headerConfigKey)) {
					sOut.append(String.format("Process headers in scope %s\n\n", entry.getValue()));
				} else {
					List<Map<String, String>> manipulated = (List<Map<String, String>>) entry.getValue();
					if(!manipulated.isEmpty()) {
						StringBuilder local = new StringBuilder(String.format(" * %s\n", headerConfigKey));
						for (Map<String, String> headerTransform : manipulated) {
							local.append(String.format("   * %s\n", headerTransform.get("name")));
						}
						sOut.append(local);
					}
				}
			}
			appendToDescription(sOut.toString(), returned);
		}

	}

	public static class Mock extends DescriptionTransformer implements ElementTransformer {

		@Override
		public void createPolicyConfigFor(String path, String key, Object value, DotAwareMap returned) {
			Map<String, Object> mockConfig = (Map<String, Object>) value;
			StringBuilder sOut = new StringBuilder("Mock response\n\n");
			if (mockConfig.containsKey("status")) {
				sOut.append(String.format("With status code %s", mockConfig.get("status")));
				// TODO find a better way to document that !
				returned.in("responses").put(mockConfig.get("status"), map()
						.from("description").to("Response is mocked")
						.get());
			}
			appendToDescription(sOut.toString(), returned);
		}

	}

	public static Map<String, ElementTransformer> TRANSFORMERS = map().from("groovy").to(new GroovyScript())
			.from("transform-headers").to(new TransformHeader()).from("mock").to(new Mock()).get();
	
	public static final List<String> FORBIDDEN = Arrays.asList("trace", "connect");

	public Object transform(Object input) {
		if (input instanceof Map) {
			DotAwareMap improved = new DotAwareMap((Map) input);
			Map returned = new DotAwareMap();
			returned.putAll(createSwaggerHeader(input));
			returned.putAll(createInfoHeader(improved));
			returned.putAll(createPaths(improved));
			return returned;
		}
		return input;
	}

	/**
	 * Create all paths in OpenApi format
	 * 
	 * @param improved
	 * @return
	 */
	private Map createPaths(DotAwareMap improved) {
		DotAwareMap returned = map().get();
		improved.getFromPath("paths").map(allPaths -> {
			createAllPaths((Map<String, List<Map>>) allPaths, returned);
			return null;
		});
		return returned;
	}

	private void createAllPaths(Map<String, List<Map>> allPaths, DotAwareMap returned) {
		for (Map.Entry<String, List<Map>> pathConfig : allPaths.entrySet()) {
			String path = pathConfig.getKey();
			for (Map policyConfig : pathConfig.getValue()) {
				createPolicyConfig(path, policyConfig, returned);
			}
		}
	}

	private void createPolicyConfig(String path, Map config, DotAwareMap returned) {
		DotAwareMap local = returned.in("paths").in(path);
		List<String> methods = (List<String>) config.get("methods");
		for (String method : methods) {
			if(FORBIDDEN.contains(method.toLowerCase())) {
				logger.warning(String.format("Trying to use unsupported method %s on %s", method, path));
			} else {
				DotAwareMap methodLocal = local.in(method.toLowerCase()); 
				// Now augment the path description with infos obtained from the policy
				// This part uses a good kind of magic !
				for (Map.Entry<String, Object> element : ((Map<String, Object>) config).entrySet()) {
					getTransformerFor(element.getKey()).createPolicyConfigFor(path, element.getKey(), element.getValue(),
							methodLocal);
				}
				// Don't forget to add the mandatory responses, if not existing !
				if(!methodLocal.containsKey("responses")) {
					methodLocal.in("responses").put("200", map()
							.from("description").to("No manipulation is done here")
							.get());
				}
			}
		}
	}

	private ElementTransformer getTransformerFor(String key) {
		return TRANSFORMERS.getOrDefault(key, new NoopTransformer());
	}

	private Map createInfoHeader(DotAwareMap input) {
		DotAwareMap returned = map().get();
		input.getFromPath("name").ifPresent(value -> returned.setFromPath("info.title", String.format("\"%s\"", value)));
		input.getFromPath("description").ifPresent(value -> returned.setFromPath("info.description", String.format("\"%s\"", value)));
		input.getFromPath("version").ifPresent(value -> returned.setFromPath("info.version", String.format("\"%s\"", value)));
		return returned;
	}

	private Map createSwaggerHeader(Object input) {
		return map().from("swagger").to("2.0").get();
	}

}
