package vip.mate.agent.graph.plan.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import vip.mate.agent.graph.NodeStreamingChatHelper;
import vip.mate.agent.graph.state.InvtStateKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanStateAccessorUsageTest {

    @Test
    void mergeUsageAlsoIncrementsSharedLlmCallCount() {
        Map<String, Object> state = new HashMap<>();
        state.put(InvtStateKeys.PROMPT_TOKENS, 10);
        state.put(InvtStateKeys.COMPLETION_TOKENS, 20);
        state.put(InvtStateKeys.LLM_CALL_COUNT, 2);
        NodeStreamingChatHelper.StreamResult result =
                new NodeStreamingChatHelper.StreamResult("ok", "", null, List.of(), false, 3, 4);

        Map<String, Object> output = PlanStateAccessor.output()
                .mergeUsage(new OverAllState(state), result)
                .build();

        assertEquals(13, output.get(InvtStateKeys.PROMPT_TOKENS));
        assertEquals(24, output.get(InvtStateKeys.COMPLETION_TOKENS));
        assertEquals(3, output.get(InvtStateKeys.LLM_CALL_COUNT));
    }
}
