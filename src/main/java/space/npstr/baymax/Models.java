package space.npstr.baymax;

import org.springframework.stereotype.Component;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class Models {

    private final Map<String, Map<String, Node>> models = new ConcurrentHashMap<>();

    private final ModelParser modelParser = new ModelParser();

    public Models() {}

    public Map<String, Node> getModelByName(String name) {
        return this.models.computeIfAbsent(name, this::loadModelByName);
    }

    public Map<String, Node> loadModelByName(String name) {
        String resourcePath = "models/" + name + ".yaml";
        return Collections.unmodifiableMap(
                this.modelParser.parse(loadModelAsYamlString(resourcePath))
        );
    }

    private String loadModelAsYamlString(String resourceName) {
        InputStream fileStream = Models.class.getClassLoader().getResourceAsStream(resourceName);
        try {
            return new String(fileStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model " + resourceName);
        }
    }
}
