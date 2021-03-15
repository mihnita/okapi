package net.sf.okapi.connectors.google;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.query.QueryResult;

@RunWith(MockitoJUnitRunner.class)
public class TestGoogleMTv2Connector {

    @Mock
    private GoogleMTAPI api;

    private GoogleMTv2Connector connector;
    private int count = 0;

    @Before
    public void setup() {
        connector = new GoogleMTv2Connector(api);
        connector.getParameters().setRetryIntervalMs(0);
        connector.getParameters().setApiKey("1234");
    }

    @Test
    public void testBatchQueryRetry() throws Exception {
        when(api.translate(any(GoogleQueryBuilder.class))).thenAnswer(new MTAnswer());

        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            inputs.add(dummyString(i));
        }
        List<List<QueryResult>> results = connector.batchQueryText(inputs);
        assertEquals(10, results.size());
        for (int i = 0; i < results.size(); i++) {
            List<QueryResult> qrs = results.get(i);
            assertEquals(1, qrs.size());
            assertEquals(translation(inputs.get(i)), qrs.get(0).target.toString());
        }
    }

    @Test
    public void testQueryRetry() throws Exception {
        when(api.translate(any(GoogleQueryBuilder.class))).thenAnswer(new MTAnswer());

        String input = dummyString(1);
        int i = connector.query(input);
        assertEquals(1, i);
        assertEquals(translation(input), connector.next().target.toString());
    }

    @Test
    public void testQueryRetryWithAbort() throws Exception {
        try ( GoogleMTv2Connector conn = new GoogleMTv2Connector(api) ) {
            conn.getParameters().setRetryIntervalMs(10);
            conn.getParameters().setRetryCount(1);
            conn.getParameters().setFailuresBeforeAbort(1);
            conn.getParameters().setApiKey("1234");
            
            when(api.translate(any(GoogleQueryBuilder.class))).thenAnswer(new MTAnswer());

            List<String> inputs = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                inputs.add(dummyString(i));
            }
            String msg = null;
            try {
            	conn.batchQueryText(inputs);
            }
            catch ( OkapiException e ) {
            	msg = e.getMessage();
            }
            assertTrue(msg.startsWith("Too many retry failures"));
        }
    }
    
    class MTAnswer implements Answer<List> {
        @Override
        public List answer(InvocationOnMock invocation) throws Throwable {
            // This fails 50% of the time
            if (count++ % 2 == 0) {
                throw new GoogleMTErrorException(403, "User limit exceeded", "domain", "reason", "query");
            }
            else {
                GoogleQueryBuilder<GoogleQueryBuilder> qb = invocation.getArgument(0);
                List<TranslationResponse> responses = new ArrayList<>();
                for (String s : qb.getSourceTexts()) {
                    responses.add(new TranslationResponse(s, translation(s)));
                }
                return responses;
            }
        }
    }

    private String dummyString(int testNum) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 1800; j++) {
            sb.append('x');
        }
        String input = "Test " + testNum + ": " + sb.toString();
        return input;
    }

    private String translation(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
