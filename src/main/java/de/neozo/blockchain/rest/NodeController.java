package de.neozo.blockchain.rest;


import de.neozo.blockchain.domain.Node;
import de.neozo.blockchain.service.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;


@RestController
@RequestMapping("node")
public class NodeController {

    private final static Logger LOG = LoggerFactory.getLogger(NodeController.class);

    private final NodeService nodeService;

    @Autowired
    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @RequestMapping()
    Set<Node> getNodes() {
        return nodeService.getKnownNodes();
    }

    @RequestMapping(method = RequestMethod.PUT)
    void addNode(@RequestBody Node node) {
        LOG.info("Add node " + node.getAddress());
        nodeService.add(node);
    }

    @RequestMapping(path = "remove", method = RequestMethod.POST)
    void removeNode(@RequestBody Node node) {
        LOG.info("Remove node " + node.getAddress());
        nodeService.remove(node);
    }

}
