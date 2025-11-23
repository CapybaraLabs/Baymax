/*
 * Copyright (C) 2018 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.baymax;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;
import space.npstr.baymax.helpdesk.exception.MalformedModelException;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class ModelLoader {

    private static final Logger log = LoggerFactory.getLogger(ModelLoader.class);

    private final Map<String, Map<String, Node>> models = new ConcurrentHashMap<>();

    private final ModelParser modelParser = new ModelParser();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Path tempDir;

    public ModelLoader() throws IOException {
        this.tempDir = Files.createTempDirectory("baymax_models");
        this.tempDir.toFile().deleteOnExit();
    }

    public Map<String, Node> getModel(String name, @Nullable URI uri) {
        return this.models.computeIfAbsent(name, __ -> loadModel(name, uri));
    }

    /**
     * @throws RuntimeException        if there is a general problem loading the model
     * @throws MalformedModelException if there is a problem parsing the model
     */
    public Map<String, Node> attemptReload(String name, @Nullable URI uri) {
        Map<String, Node> model = loadModel(name, uri);
        this.models.put(name, model);
        return model;
    }

    private Map<String, Node> loadModel(String name, @Nullable URI uri) {
        String rawModel;
        if (uri != null) {
            try {
                rawModel = loadModelFromUrl(name, uri);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load model" + name + " from url " + uri, e);
            }
        } else {
            String filePath = "models/" + name + ".yaml";
            rawModel = loadModelAsYamlString(filePath);
        }

        return Collections.unmodifiableMap(
                this.modelParser.parse(rawModel)
        );
    }

    public String loadModelFromUrl(String name, URI uri) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile(tempDir, name, ".yaml");
        tempFile.toFile().deleteOnExit();

        HttpResponse.BodyHandler<Path> bodyHandler = HttpResponse.BodyHandlers.ofFile(tempFile);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
        HttpResponse<Path> response = httpClient.send(request, bodyHandler);

        log.debug("Fetched model {} from {} with status {} and saved to {}", name, uri, response.statusCode(), tempFile);

        return loadModelAsYamlString(tempFile.toFile());
    }

    private String loadModelAsYamlString(String fileName) {
        return loadModelAsYamlString(new File(fileName));
    }

    private String loadModelAsYamlString(File modelFile) {
        if (!modelFile.exists() || !modelFile.canRead()) {
            throw new RuntimeException("Failed to find or read model file " + modelFile.getName());
        }
        try (InputStream fileStream = new FileInputStream(modelFile)) {
            return new String(fileStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model " + modelFile.getName(), e);
        }
    }
}
