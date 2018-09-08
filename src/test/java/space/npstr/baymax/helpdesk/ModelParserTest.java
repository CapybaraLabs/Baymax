package space.npstr.baymax.helpdesk;

import org.junit.jupiter.api.Test;
import space.npstr.baymax.helpdesk.exception.MissingTargetNodeException;
import space.npstr.baymax.helpdesk.exception.NoRootNodeException;
import space.npstr.baymax.helpdesk.exception.UnreferencedNodesException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by napster on 05.09.18.
 */
public class ModelParserTest {

    private final ModelParser modelParser = new ModelParser();

    @Test
    void parse_SaneModel_Ok() throws IOException {
        Map<String, Node> model = this.modelParser.parse(loadModelAsYamlString("models/sane.yaml"));

        Node root = model.get("root");
        assertNotNull(root);
        assertEquals("How may I help you?", root.getTitle());
        assertEquals(2, root.getBranches().size());

        Node supportRole = model.get("support-role");
        assertNotNull(supportRole);
        Optional<Long> roleId = Optional.ofNullable(supportRole.getRoleId());
        assertTrue(roleId.isPresent());
        assertEquals(487925645989380108L, roleId.get().longValue());
    }

    @Test
    void parse_ModelWithoutRootNode_ExceptionThrown() {
        assertThrows(
                NoRootNodeException.class,
                () -> this.modelParser.parse(loadModelAsYamlString("models/no_root_node.yaml"))
        );
    }

    @Test
    void parse_ModelWithMissingNode_ExceptionThrown() {
        assertThrows(
                MissingTargetNodeException.class,
                () -> this.modelParser.parse(loadModelAsYamlString("models/missing_node.yaml"))
        );
    }

    @Test
    void parse_ModelWithUnreferencedNode_ExceptionThrown() {
        assertThrows(
                UnreferencedNodesException.class,
                () -> this.modelParser.parse(loadModelAsYamlString("models/unreferenced_node.yaml"))
        );
    }

    private String loadModelAsYamlString(String resourceName) throws IOException {
        InputStream fileStream = ModelParserTest.class.getClassLoader().getResourceAsStream(resourceName);
        return new String(fileStream.readAllBytes());
    }
}
