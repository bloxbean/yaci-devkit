package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.service.RollbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/local-cluster/api/devnet/rollback")
@RequiredArgsConstructor
@Tag(name = "Rollback API", description = "API for simulating rollback in a single node devnet (Experimental)")
@Slf4j
public class RollbackController {

    @Autowired
    private RollbackService rollbackService;

    @PostMapping("/set-rollback-point")

    @Operation(summary = "Set Rollback Point",
            description = "Take a snapshot of current db folder of the devnet node. This will allow to" +
                    " simulate a rollback to this point later by using /perform-rollback endpoint.")
    public String setRollbackPoint() {
        StringBuilder sb = new StringBuilder();
        rollbackService.setRollbackPoint(msg -> sb.append(msg));

        return sb.toString();
    }

    @PostMapping("/perform-rollback")
    @Operation(summary = "Perform Rollback",
            description = "Rollback the devnet node to the last set rollback point. This will copy" +
                    " the db folder from the rollback point to the current db folder, effectively rolling back." +
                    "This is just a simulation of rollback in a single node setup where the blocks will be started" +
                    " from the last block in the rollback db."
    )
    public String rollback() {
        StringBuilder sb = new StringBuilder();
        rollbackService.rollback(msg -> sb.append(msg));

        return sb.toString();
    }
}
