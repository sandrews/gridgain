// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.hadoop.proto;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.hadoop.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Hadoop protocol task adapter.
 */
public abstract class GridHadoopProtocolTaskAdapter<R> implements GridComputeTask<GridHadoopProtocolTaskArguments, R> {
    @Nullable @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid,
        @Nullable GridHadoopProtocolTaskArguments arg) throws GridException {
        return Collections.singletonMap(new Job(arg), subgrid.get(0));
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd)
        throws GridException {
        return GridComputeJobResultPolicy.REDUCE;
    }

    /** {@inheritDoc} */
    @Nullable @Override public R reduce(List<GridComputeJobResult> results) throws GridException {
        if (!F.isEmpty(results)) {
            GridComputeJobResult res = results.get(0);

            return res.getData();
        }
        else
            return null;
    }

    /**
     *
     */
    private class Job implements GridComputeJob {
        @GridInstanceResource
        private Grid grid;

        /** Argument. */
        private final GridHadoopProtocolTaskArguments args;

        /**
         * Constructor.
         *
         * @param args Job argument.
         */
        private Job(GridHadoopProtocolTaskArguments args) {
            this.args = args;
        }

        /** {@inheritDoc} */
        @Override public void cancel() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Nullable @Override public Object execute() throws GridException {
            return run(((GridKernal)grid).context().hadoop(), args);
        }
    }

    /**
     * Run the task.
     *
     * @param proc Hadoop processor.
     * @param args Arguments.
     * @return Job result.
     * @throws GridException If failed.
     */
    public abstract R run(GridHadoopProcessor proc, GridHadoopProtocolTaskArguments args) throws GridException;
}
