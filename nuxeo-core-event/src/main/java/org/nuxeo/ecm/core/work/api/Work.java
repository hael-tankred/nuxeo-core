/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;

/**
 * A {@link Work} instance gets scheduled and executed by a {@link WorkManager}
 * .
 * <p>
 * Its {@link #work} method runs when a slot in a queue becomes available, note
 * however that it can be suspended at any time (before initial execution, or
 * during execution).
 * <p>
 * A {@link Work} instance has an id that is used by the queuing mechanisms to
 * determine uniqueness. It also has a category that is used to choose which
 * queue should execute it. It can report its status and progress.
 * <p>
 * A {@link Work} instance is Serializable because it must be able to save its
 * computation state on interruption, and be reconstructed again with the saved
 * state to continue execution at a later time. Because of this, the instance
 * itself may change over time and be executed on a different JVM than the one
 * that constructed it initially.
 * <p>
 * A {@link Work} instance must have an id, which is used for equality
 * comparisons and as a key for persistent queues.
 * <p>
 * Implementors are strongly advised to inherit from {@link AbstractWork}.
 *
 * @see AbstractWork
 * @since 5.6
 */
public interface Work extends Serializable {

    /**
     * The running state of a {@link Work} instance.
     * <p>
     * The following transitions between states are possible:
     * <ul>
     * <li>SCHEDULED -> CANCELED (is never scheduled or run)
     * <li>SCHEDULED -> RUNNING
     * <li>RUNNING -> COMPLETED
     * <li>RUNNING -> FAILED
     * <li>RUNNING -> SCHEDULED (is suspended and persisted)
     * </ul>
     */
    enum State {
        /**
         * Work instance is scheduled to run later.
         */
        SCHEDULED,
        /**
         * Work instance was canceled.
         * <p>
         * This happens if:
         * <ul>
         * <li>it is never scheduled because another instance with the same id
         * was already scheduled or running (when scheduled with
         * {@link Scheduling#IF_NOT_SCHEDULED},
         * {@link Scheduling#IF_NOT_RUNNING}, or
         * {@link Scheduling#IF_NOT_RUNNING_OR_SCHEDULED}),
         * <li>it is never run because it was scheduled after commit and the
         * transaction rolled back,
         * <li>it is never run because it was scheduled, but another work with
         * the same id was scheduled with {@link Scheduling#CANCEL_SCHEDULED}.
         * </ul>
         */
        CANCELED,
        /**
         * Work instance is running.
         */
        RUNNING,
        /**
         * Work instance has completed normally.
         */
        COMPLETED,
        /**
         * Work instance has completed with an exception.
         */
        FAILED,
    }

    /**
     * A progress report about a work instance.
     * <p>
     * Progress can be expressed as a percentage, or with a current and total
     * count.
     * <ul>
     * <li>26.2% (percent not indeterminate)</li>
     * <li>12/345 (current not indeterminate)</li>
     * <li>?/345 (percent and current indeterminate but total non-zero)</li>
     * <li>? (percent and current indeterminate and total zero)</li>
     * </ul>
     *
     * @since 5.6
     */
    public static class Progress implements Serializable {

        private static final long serialVersionUID = 1L;

        public static long CURRENT_INDETERMINATE = -1;

        public static float PERCENT_INDETERMINATE = -1F;

        public static final Progress PROGRESS_INDETERMINATE = new Progress(
                PERCENT_INDETERMINATE);

        public static final Progress PROGRESS_0_PC = new Progress(0F);

        public static final Progress PROGRESS_100_PC = new Progress(100F);

        protected final float percent;

        protected final long current;

        protected final long total;

        /**
         * Constructs a {@link Progress} as a percentage.
         *
         * @param percent the percentage, a float between 0 and 100, or
         *            {@link #PERCENT_INDETERMINATE}
         */
        public Progress(float percent) {
            this.percent = percent > 100F ? 100F : percent;
            this.current = CURRENT_INDETERMINATE;
            this.total = 0;
        }

        /**
         * Constructs a {@link Progress} as a current and total count.
         *
         * @param current the current count or {@link #CURRENT_INDETERMINATE}
         * @param total the total count
         */
        public Progress(long current, long total) {
            this.percent = PERCENT_INDETERMINATE;
            this.current = current;
            this.total = total;
        }

        public float getPercent() {
            return percent;
        }

        public long getCurrent() {
            return current;
        }

        public long getTotal() {
            return total;
        }

        public boolean getIsWithPercent() {
            return percent != PERCENT_INDETERMINATE;
        }

        public boolean getIsWithCurrentAndTotal() {
            return current != CURRENT_INDETERMINATE;
        }

        public boolean getIsIndeterminate() {
            return percent == PERCENT_INDETERMINATE
                    && current == CURRENT_INDETERMINATE;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + "("
                    + (percent == PERCENT_INDETERMINATE ? "?"
                            : Float.valueOf(percent))
                    + "%, "
                    + (current == CURRENT_INDETERMINATE ? "?"
                            : Long.valueOf(current)) + "/" + total + ")";
        }
    }

    /**
     * This method should implement the actual work done by the {@link Work}
     * instance.
     * <p>
     * It should periodically update its progress through {@link #setProgress}.
     * <p>
     * To allow for suspension by the {@link WorkManager}, it should
     * periodically call {@link #isSuspending}, and if {@code true} call
     * {@link #suspended} return early with saved state data.
     * <p>
     * Clean up can by implemented by {@link #cleanUp()}.
     *
     * @see #isSuspending
     * @see #suspended
     * @see #cleanUp
     */
    void work() throws Exception;

    /**
     * The work id.
     * <p>
     * The id is used for equality comparisons, and as a key in persistent
     * queues.
     *
     * @return the work id, which must not be {@code null}
     *
     * @since 5.8
     */
    String getId();

    /**
     * This method is called after {@link #work} is done, in a finally block,
     * whether work completed normally or was in error or was interrupted.
     *
     * @param ok {@code true} if the work completed normally
     * @param e the exception, if available
     */
    void cleanUp(boolean ok, Exception e);

    /**
     * CALLED BY THE WORK MANAGER (not user code) when it requests that this
     * work instance be suspended.
     *
     * @since 5.8
     */
    void setWorkInstanceSuspending();

    /**
     * Checks if a suspend has been requested for this work instance by the work
     * manager.
     * <p>
     * If {@code true}, then state should be saved, {@link #suspended()} should
     * be called, and the {@link #work()} method should return.
     *
     * @since 5.8
     */
    boolean isSuspending();

    /**
     * Must be called by {@link Work} implementations to advertise that state
     * saving is done, when {@link #isSuspending()} returned {@code true}. After
     * this is called, the {@link #work()} method should return.
     *
     * @since 5.8
     */
    void suspended();

    /**
     * CALLED BY THE WORK MANAGER (not user code) to check if this work instance
     * really suspended.
     *
     * @since 5.8
     */
    boolean isWorkInstanceSuspended();

    /**
     * CALLED BY THE WORK MANAGER (not user code) to set this work instance's
     * state.
     *
     * @since 5.8
     */
    void setWorkInstanceState(State state);

    /**
     * CALLED BY THE WORK MANAGER (not user code) to get this work instance's
     * state.
     * <p>
     * Used only to get the final state of a completed instance (
     * {@link State#COMPLETED}, {@link State#FAILED} or {@link State#CANCELED}).
     *
     * @since 5.8
     */
    State getWorkInstanceState();

    /**
     * DO NOT USE THIS - gets the state of this work instance.
     * <p>
     * This should not be used because for non in-memory persistence, the work
     * instance gets serialized and deserialized for running and when retrieved
     * after completion, and therefore the original instance cannot get updated
     * after the original scheduling.
     *
     * @return the state
     * @deprecated since 5.8, use {@link WorkManager#getWorkState} instead
     */
    @Deprecated
    State getState();

    /**
     * Gets the category for this work.
     * <p>
     * Used to choose an execution queue.
     *
     * @return the category, or {@code null} for the default
     */
    String getCategory();

    /**
     * Gets a human-readable name for this work instance.
     *
     * @return a human-readable name
     */
    String getTitle();

    /**
     * Gets a human-readable status for this work instance.
     *
     * @return a human-readable status
     */
    String getStatus();

    /**
     * Gets the time at which this work instance was first scheduled.
     *
     * @return the scheduling time (milliseconds since epoch)
     */
    long getSchedulingTime();

    /**
     * Gets the time at which this work instance was first started.
     *
     * @return the start time (milliseconds since epoch), or {@code 0} if not
     *         stated
     */
    long getStartTime();

    /**
     * Gets the time at which this work instance was completed, suspended or
     * failed.
     *
     * @return the completion time (milliseconds since epoch), or {@code 0} if
     *         not completed
     */
    long getCompletionTime();

    /**
     * This method should be called periodically by the actual work method when
     * it knows of its progress.
     *
     * @param progress the progress
     * @see Progress#Progress(float)
     * @see Progress#Progress(long, long)
     */
    void setProgress(Progress progress);

    /**
     * Gets a progress report for this work instance.
     *
     * @return a progress report, not {@code null}
     */
    Progress getProgress();

    /**
     * Gets the user on behalf of which this work is done.
     * <p>
     * This is informative only.
     *
     * @return the user id, or {@code null}
     */
    String getUserId();

    /**
     * Gets the document impacted by the work.
     * <p>
     * Returns {@code null} if the work isn't about a single document.
     *
     * @return the document, or {@code null}. This is always a
     *         {@link DocumentLocation} with an {@link IdRef}
     * @since 5.8
     */
    DocumentLocation getDocument();

    /**
     * Gets the documents impacted by the work.
     * <p>
     * Returns {@code null} if the work isn't about documents.
     *
     * @return the documents, or an empty list. List elements are always a
     *         {@link DocumentLocation} with an {@link IdRef}
     * @since 5.8
     */
    List<DocumentLocation> getDocuments();

    /**
     * Returns {@code true} if {@link #getDocument} is only the root of a set of
     * documents on which this Work instance will act.
     *
     * @return {@code true} if a whole tree is impacted
     * @since 5.8
     */
    boolean isDocumentTree();

}
