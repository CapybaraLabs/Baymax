package space.npstr.baymax;

import org.springframework.stereotype.Component;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class Models {

    private final Map<String, Node> akiModel;

    public Models() throws IOException {
        ModelParser modelParser = new ModelParser();
        this.akiModel = Collections.unmodifiableMap(
                modelParser.parse(loadModelAsYamlString("models/aki.yaml"))
        );
    }

    public Map<String, Node> getAkiModel() {
        return this.akiModel;
    }

    private String loadModelAsYamlString(String resourceName) throws IOException {
        InputStream fileStream = Models.class.getClassLoader().getResourceAsStream(resourceName);
        return new String(fileStream.readAllBytes());
    }
}
