package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.service.RollbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@RestController
@RequestMapping(path = "/local-cluster/api/devnet/rollback")
@RequiredArgsConstructor
@Tag(name = "Rollback API", description = "API for simulating rollback in a single node devnet (Experimental)")
@Slf4j
public class RollbackController {

    @Autowired
    private RollbackService rollbackService;

    @PostMapping("/take-db-snapshot")

    @Operation(summary = "Take node's db snapshot for rollback",
            description = "Take a snapshot of current db folder of the devnet node. This will allow to" +
                    " simulate a rollback to this point later by using /rollback-to-db-snapshot endpoint.")
    public String takeDBSnapshot() {
        StringBuilder sb = new StringBuilder();
        rollbackService.takeDBSnapshot(msg -> sb.append(msg));

        return sb.toString();
    }

    @PostMapping("/rollback-to-db-snapshot")
    @Operation(summary = "Perform Rollback by using the last db snapshot",
            description = "Rollback the devnet node to the last set rollback point. This will copy" +
                    " the db folder from the rollback point to the current db folder, effectively rolling back." +
                    "This is just a simulation of rollback in a single node setup where the blocks will be started" +
                    " from the last block in the last snapshot db."
    )
    public String rollbackToDBSnapshot() {
        StringBuilder sb = new StringBuilder();
        rollbackService.rollbackToLastDBSnapshot(msg -> sb.append(msg));

        return sb.toString();
    }

    @PostMapping("/create-forks")
    @Operation(summary = "Create forks for rollback",
            description = "Create forks for rollback. This will detach the main node from the peer nodes and create" +
                    " forks. The main node will be restarted with a specified delay if restartNode is selected")
    public ResponseEntity<String> createForks(@RequestBody CreateForksRequest request) {
        StringBuilder sb = new StringBuilder();
        boolean status = rollbackService.createForks(request.restartNode, request.waitTimeInSec, msg -> sb.append(msg));

        writeLn(sb.toString());
        if (status) {
            return ResponseEntity.ok(sb.toString());
        } else {
            return ResponseEntity.badRequest().body(sb.toString());
        }
    }

    @PostMapping("/join-forks")
    @Operation(summary = "Join forks for rollback",
            description = "Join the forks created by the create-forks endpoint. This will reattach the main node to the peer nodes.")
    public ResponseEntity<String> joinForks() {
        StringBuilder sb = new StringBuilder();
        boolean status = rollbackService.joinForks(msg -> sb.append(msg));

        writeLn(sb.toString());
        if (status) {
            return ResponseEntity.ok(sb.toString());
        } else {
            return ResponseEntity.badRequest().body(sb.toString());
        }
    }

    record CreateForksRequest(
        @Schema(description = "Whether to restart the main node", defaultValue = "false")
        boolean restartNode,
        @Schema(description = "Wait time before restarting the main node (in seconds)", defaultValue = "10")
        long waitTimeInSec
    ) {}
}
