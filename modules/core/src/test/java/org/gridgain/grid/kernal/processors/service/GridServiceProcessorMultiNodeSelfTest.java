/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.service;

import org.gridgain.grid.*;
import org.gridgain.grid.service.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Single node services test.
 */
public class GridServiceProcessorMultiNodeSelfTest extends GridServiceProcessorAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int nodeCount() {
        return 4;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSingletonUpdateTopology() throws Exception {
        String name = "serviceSingletonUpdateTopology";

        Grid g = randomGrid();

        CountDownLatch latch = new CountDownLatch(1);

        DummyService.exeLatch(name, latch);

        GridFuture<?> fut = g.services().deployClusterSingleton(name, new DummyService());

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        latch.await();

        assertEquals(name, 1, DummyService.started(name));
        assertEquals(name, 0, DummyService.cancelled(name));

        int nodeCnt = 2;

        startExtraNodes(nodeCnt);

        try {
            assertEquals(name, 1, DummyService.started(name));
            assertEquals(name, 0, DummyService.cancelled(name));

            info(">>> Passed checks.");

            checkCount(name, g.services().deployedServices(), 1);
        }
        finally {
            stopExtraNodes(nodeCnt);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinityDeployUpdateTopology() throws Exception {
        Grid g = randomGrid();

        String name = "serviceAffinityUpdateTopology";

        final Integer affKey = 1;

        // Store a cache key.
        g.cache(CACHE_NAME).put(affKey, affKey.toString());

        CountDownLatch latch = new CountDownLatch(1);

        GridFuture<?> fut = g.services().deployKeyAffinitySingleton(name, new AffinityService(latch, affKey),
            CACHE_NAME, affKey);

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        latch.await();

        checkCount(name, g.services().deployedServices(), 1);

        int nodeCnt = 2;

        startExtraNodes(nodeCnt);

        try {
            checkCount(name, g.services().deployedServices(), 1);
        }
        finally {
            stopExtraNodes(nodeCnt);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testDeployOnEachNodeUpdateTopology() throws Exception {
        String name = "serviceOnEachNodeUpdateTopology";

        Grid g = randomGrid();

        CountDownLatch latch = new CountDownLatch(nodeCount());

        DummyService.exeLatch(name, latch);

        GridFuture<?> fut = g.services().deployNodeSingleton(name, new DummyService());

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        latch.await();

        assertEquals(name, nodeCount(), DummyService.started(name));
        assertEquals(name, 0, DummyService.cancelled(name));

        int newNodes = 2;

        latch = new CountDownLatch(newNodes);

        DummyService.exeLatch(name, latch);

        startExtraNodes(newNodes);

        try {
            latch.await();

            assertEquals(name, nodeCount() + newNodes, DummyService.started(name));
            assertEquals(name, 0, DummyService.cancelled(name));

            checkCount(name, g.services().deployedServices(), nodeCount() + newNodes);
        }
        finally {
            stopExtraNodes(newNodes);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testServiceProxy() throws Exception {
        String name = "dummyService";

        Grid g = randomGrid();

        final int[] cntr = new int[1];

        GridFuture<?> fut = g.services().deployNodeSingleton(name, new GridIntService(cntr) {
            @Override public void cancel(GridServiceContext ctx) {
                // No-op
            }

            @Override public void execute(GridServiceContext ctx) throws Exception {
                cntr[0] = 239;
            }
        });

        info("Deployed service: " + name);

        fut.get();

        info("Finished waiting for service future: " + name);

        GridService proxy = g.services().serviceProxy(name, GridService.class, true);

        proxy.execute(new GridServiceContextImpl(null, null, null, null, null, null));

        assertEquals("Proxy service was not executed", 239, cntr[0]);
    }

    /**
     * Class for testing purposes. Used for service proxy checking.
     */
    private static class GridIntService implements GridService, Serializable{
        /** Array testing purposes. */
        private final int[] a;

        /**
         * @param arr Array.
         */
        private GridIntService(int[] arr) {
            a = arr;
        }

        /** {@inheritDoc} */
        @Override public void cancel(GridServiceContext ctx) {
            // No-op
        }

        /** {@inheritDoc} */
        @Override public void execute(GridServiceContext ctx) throws Exception {
            a[0] = 239;
        }
    }
}
