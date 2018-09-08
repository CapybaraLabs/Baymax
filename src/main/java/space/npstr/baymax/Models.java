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

import org.springframework.stereotype.Component;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;

import java.io.File;
import java.io.FileInputStream;
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
        String filePath = "models/" + name + ".yaml";
        return Collections.unmodifiableMap(
                this.modelParser.parse(loadModelAsYamlString(filePath))
        );
    }

    private String loadModelAsYamlString(String fileName) {
        File modelFile = new File(fileName);
        if (!modelFile.exists() || !modelFile.canRead()) {
            throw new RuntimeException("Failed to find or read model file " + fileName);
        }
        try {
            InputStream fileStream = new FileInputStream(modelFile);
            return new String(fileStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model " + fileName, e);
        }
    }
}
