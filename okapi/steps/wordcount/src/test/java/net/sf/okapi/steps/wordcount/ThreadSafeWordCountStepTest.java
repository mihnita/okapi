package net.sf.okapi.steps.wordcount;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows some concurrency issues when using the WordCountStep. Uncommenting
 * submit.get() make the execution sequential.
 *  
 * @author aurambaj
 */
public class ThreadSafeWordCountStepTest {

    static AtomicInteger ai = new AtomicInteger();

    public static void main(String[] argv) throws InterruptedException, ExecutionException {

        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 100; i++) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    processDoc(ai.getAndIncrement());
                }
            };
            Future<?> submit = newFixedThreadPool.submit(runnable);
            // works if blocking 
            //  submit.get();

        }

        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(2, TimeUnit.MINUTES);
    }

    public static void processDoc(int i) {

        System.out.println("Start processDoc" + i);
        try {
            RawDocument rawDocument = new RawDocument("key1.1=The car is red in Doc:" + i + ".\nkey2.1=The car is blue in Doc:" + i + ".\nkey3.1=The car is red and blue in Doc:" + i + ".", LocaleId.ENGLISH);
            rawDocument.setTargetLocale(LocaleId.ENGLISH);

            PipelineDriver driver = new PipelineDriver();

            PropertiesFilter filter = new PropertiesFilter();
            driver.addStep(new RawDocumentToFilterEventsStep(filter));
            driver.addStep(new WordCountStep());

            driver.addBatchItem(rawDocument);
            driver.processBatch();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println("Exception in processDoc " + i + "\n" + sw.toString());
        }

        System.out.println("End processDoc " + i);
    }
}


