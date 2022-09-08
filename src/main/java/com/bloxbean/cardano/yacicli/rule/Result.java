package com.bloxbean.cardano.yacicli.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    Map<String, Object> map = new HashMap<>();

    public void clear() {
        map.clear();
    }
}
