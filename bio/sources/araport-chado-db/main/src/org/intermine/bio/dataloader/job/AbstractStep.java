package org.intermine.bio.dataloader.job;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.intermine.bio.dataloader.util.ClassUtils;

public abstract class AbstractStep implements Step {
	
	protected static final Logger log = Logger.getLogger(AbstractStep.class);

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}


	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property. Always overrides the default value if this object is a Spring bean.
	 *
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of the callbacks in a Spring container the
	 * name property will be set first if it is present. Care is needed with bean definition inheritance - if a parent
	 * bean has a name, then its children need an explicit name as well, otherwise they will not be unique.
	 *
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	
	@Override
	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 *
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit == 0 ? Integer.MAX_VALUE : startLimit;
	}
	
	public AbstractStep(String name) {
		this.name = name;
	}

	/**
	 * Extension point for subclasses to execute business logic. Subclasses should set the {@link ExitStatus} on the
	 * {@link StepExecution} before returning.
	 *
	 * @param stepExecution the current step context
	 * @throws Exception
	 */
	protected abstract void doExecute(StepExecution stepExecution) throws Exception;
	
	protected abstract void doPostProcess(StepExecution stepExecution) throws Exception;

	/**
	 * Extension point for subclasses to provide callbacks to their collaborators at the beginning of a step, to open or
	 * acquire resources. Does nothing by default.
	 *
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception
	 */
	protected void open(ExecutionContext ctx) throws Exception {
	}
	
	protected void open() throws Exception {
	}

	/**
	 * Extension point for subclasses to provide callbacks to their collaborators at the end of a step (right at the end
	 * of the finally block), to close or release resources. Does nothing by default.
	 *
	 * @param ctx the {@link ExecutionContext} to use
	 * @throws Exception
	 */
	protected void close(ExecutionContext ctx) throws Exception {
	}

	/**
	 * Template method for step execution logic - calls abstract methods for resource initialization (
	 * {@link #open(ExecutionContext)}), execution logic ({@link #doExecute(StepExecution)}) and resource closing (
	 * {@link #close(ExecutionContext)}).
	 */
	@Override
	public final void execute(StepExecution stepExecution)  {

		log.debug("Executing: id=" + stepExecution.getId());
		stepExecution.setStartTime(new Date());
		stepExecution.setStatus(BatchStatus.STARTED);
		
		// Start with a default value that will be trumped by anything
		ExitStatus exitStatus = ExitStatus.EXECUTING;

		try {
			
			log.info("Step has started " + getName() +".");
			
			log.info("Before Opening Reader");
			
			open();
			
			log.info("After Opening Reader");

			//execute
			try {
				doExecute(stepExecution);
			}
			catch (Exception e) {
				throw e.getCause();
			}
			
			//postprocess
			
			try {
				doPostProcess(stepExecution);
			}
			catch (Exception e) {
				throw e.getCause();
			}
			
			exitStatus = ExitStatus.COMPLETED.and(stepExecution.getExitStatus());

			// Need to upgrade here not set, in case the execution was stopped
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			
			log.info("Step has completed " + getName() +".");
			log.debug("Step execution success: id=" + stepExecution.getId());
		}
		catch (Throwable e) {
			stepExecution.upgradeStatus(determineBatchStatus(e));
			exitStatus = exitStatus.and(getDefaultExitStatusForFailure(e));
			stepExecution.addFailureException(e);
			if (stepExecution.getStatus() == BatchStatus.STOPPED) {
				log.info(String.format("Encountered interruption executing step %s in job %s : %s", name, stepExecution.getJobExecution().getJobInstance().getJobName(), e.getMessage()));
				if (log.isDebugEnabled()) {
					log.debug("Full exception", e);
				}
			}
			else {
				log.error(String.format("Encountered an error executing step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}
		}
		finally {

			try {
				// Update the step execution to the latest known value so the
				// listeners can act on it
				exitStatus = exitStatus.and(stepExecution.getExitStatus());
				stepExecution.setExitStatus(exitStatus);
				
			}
			catch (Exception e) {
				log.error(String.format("Exception in afterStep callback in step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
			}

			
			stepExecution.setEndTime(new Date());
			stepExecution.setExitStatus(exitStatus);

			try {
				close(stepExecution.getExecutionContext());
			}
			catch (Exception e) {
				log.error(String.format("Exception while closing step execution resources in step %s in job %s", name, stepExecution.getJobExecution().getJobInstance().getJobName()), e);
				stepExecution.addFailureException(e);
			}

			
			log.debug("Step execution complete: " + stepExecution.getSummary());
		}
	}

	
	/**
	 * Determine the step status based on the exception.
	 */
	private static BatchStatus determineBatchStatus(Throwable e) {
				return BatchStatus.STOPPED;
		
	}

	/**
	 * Register a step listener for callbacks at the appropriate stages in a step execution.
	 *
	 * @param listener a {@link StepExecutionListener}
	 */


	@Override
	public String toString() {
		return ClassUtils.getShortName(getClass()) + ": [name=" + name + "]";
	}

	/**
	 * Default mapping from throwable to {@link ExitStatus}. Clients can modify the exit code using a
	 * {@link StepExecutionListener}.
	 *
	 * @param ex the cause of the failure
	 * @return an {@link ExitStatus}
	 */
	private ExitStatus getDefaultExitStatusForFailure(Throwable ex) {
		ExitStatus exitStatus;
		
		exitStatus = ExitStatus.STOPPED.addExitDescription(JobInterruptedException.class.getName());
		

		return exitStatus;
	}

}

